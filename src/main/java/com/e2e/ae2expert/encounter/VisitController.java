package com.e2e.ae2expert.encounter;

import com.e2e.ae2expert.AE2ExpertAddon;
import java.util.UUID;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public final class VisitController {
    private static final long DELAY = 5 * 60 * 20;
    private static final long RECENT_INPUT = 60;
    private UUID tracked;
    private float yaw, pitch;
    private double x, y, z;
    private int dimension;
    private long lastInput = Long.MIN_VALUE;
    private long lastTick = Long.MIN_VALUE;
    private Visitor visitor;
    private UUID guest;
    private int scene;
    private static final String KEY = "ae2expert_visit";

    public static void register() {
        EntityRegistry.registerModEntity(new ResourceLocation("ae2expert", "visitor"), Visitor.class,
                "visitor", 0, AE2ExpertAddon.instance, 48, 1, true);
    }

    public static VisitController start(FMLServerStartingEvent event) {
        VisitController controller = new VisitController();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(controller);
        event.registerServerCommand(new CommandBase() {
            @Override public String getName() { return "danzalexforyou"; }
            @Override public String getUsage(ICommandSender sender) { return "/danzalexforyou"; }
            @Override public int getRequiredPermissionLevel() { return 0; }
            @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
                EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                if (!singlePlayer(server)) throw new CommandException("commands.ae2expert.visit.single");
                if (controller.visitor != null) throw new CommandException("commands.ae2expert.visit.running");
                if (!controller.spawn(player)) throw new CommandException("commands.ae2expert.visit.location");
                controller.data(server).complete();
            }
        });
        return controller;
    }

    public static void stop(VisitController controller) {
        if (controller != null) net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(controller);
    }

    private static boolean singlePlayer(MinecraftServer server) {
        return !server.isDedicatedServer() && server.getPlayerList().getPlayers().size() == 1;
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || !singlePlayer(server)) {
            tracked = null;
            if (visitor != null) finish();
            return;
        }
        EntityPlayerMP player = server.getPlayerList().getPlayers().get(0);
        long now = server.getWorld(0).getTotalWorldTime();
        double dx = player.posX - x, dy = player.posY - y, dz = player.posZ - z;
        double distance = dx * dx + dy * dy + dz * dz;
        if (!player.getUniqueID().equals(tracked) || now - lastTick > 2 || player.dimension != dimension || distance > 64) {
            tracked = player.getUniqueID();
            dimension = player.dimension;
            yaw = player.rotationYaw;
            pitch = player.rotationPitch;
            lastInput = Long.MIN_VALUE;
        } else if (moved(player, dx * dx + dz * dz, dy) || Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(player.rotationYaw - yaw)) > .2f
                || Math.abs(player.rotationPitch - pitch) > .2f) {
            lastInput = now;
            yaw = player.rotationYaw;
            pitch = player.rotationPitch;
        }
        x = player.posX;
        y = player.posY;
        z = player.posZ;
        lastTick = now;
        if (visitor != null) {
            if (visitor.isDead || visitor.world != player.world || !player.isEntityAlive() || !player.getUniqueID().equals(guest)) {
                finish();
                return;
            }
            if (++scene == 15 * 20) say(player, "Манас Сиэль..... ... Я ПОГЛОЩАЮ ТЕБЯ!");
            if (scene == 23 * 20) firework(player);
            if (scene == 32 * 20)
                say(player, "Не бойся, я не серьёзно, ты хороший человек, я тебя не трону. Проживи свою лучшую жизнь, и радуйся каждому мгновенью! Удачи тебе на твоём пути, и будь счастлив!");
            if (scene == 38 * 20) finish();
            return;
        }
        VisitData saved = data(server);
        if (saved.done) return;
        saved.schedule(now);
        if (now < saved.deadline || lastInput == Long.MIN_VALUE || now - lastInput > RECENT_INPUT || now % 10 != 0) return;
        if (spawn(player)) saved.complete();
    }

    private static boolean moved(EntityPlayerMP player, double horizontalDistanceSquared, double verticalDistance) {
        if (player.isRiding() || player.isInWater() || player.isInLava()) return false;
        return horizontalDistanceSquared >= .0025
                || player.capabilities.isFlying && Math.abs(verticalDistance) >= .05;
    }

    private boolean spawn(EntityPlayerMP player) {
        if (visitor != null || !player.isEntityAlive() || player.isSpectator() || player.isRiding()
                || player.isPlayerSleeping()) return false;
        WorldServer world = player.getServerWorld();
        BlockPos origin = new BlockPos(player);
        boolean airborne = player.capabilities.isFlying;
        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            BlockPos landing = origin.offset(side, 2);
            BlockPos spot = origin.offset(side, 4);
            if (!airborne) {
                boolean found = false;
                for (int offset = 0; offset <= 64; offset++) {
                    if (safe(world, landing.down(offset), true) && safe(world, spot.down(offset), true)) {
                        landing = landing.down(offset);
                        spot = spot.down(offset);
                        found = true;
                        break;
                    }
                    if (player.onGround && offset >= 2) break;
                }
                if (!found) continue;
            } else if (!safe(world, landing, false) || !safe(world, spot, false)) continue;
            float look = side.getHorizontalAngle();
            Visitor candidate = new Visitor(world);
            candidate.setNoGravity(airborne);
            candidate.setLocationAndAngles(spot.getX() + .5, spot.getY(), spot.getZ() + .5, look + 180, 0);
            candidate.rotationYawHead = look + 180;
            if (!world.spawnEntity(candidate)) continue;
            visitor = candidate;
            guest = player.getUniqueID();
            scene = 0;
            player.connection.setPlayerLocation(landing.getX() + .5, landing.getY(), landing.getZ() + .5, look, 0);
            player.fallDistance = 0;
            return true;
        }
        return false;
    }

    private static boolean safe(WorldServer world, BlockPos pos, boolean ground) {
        if (pos.getY() < 1 || pos.getY() >= world.getHeight() - 1 || !world.isBlockLoaded(pos)
                || !world.getWorldBorder().contains(pos) || !world.isAirBlock(pos) || !world.isAirBlock(pos.up())) return false;
        if (ground) {
            net.minecraft.block.state.IBlockState floor = world.getBlockState(pos.down());
            if (!floor.isFullBlock() || floor.getBlock() == Blocks.MAGMA || floor.getBlock() == Blocks.CACTUS) return false;
        }
        AxisAlignedBB box = new AxisAlignedBB(pos.getX() + .2, pos.getY(), pos.getZ() + .2,
                pos.getX() + .8, pos.getY() + 1.8, pos.getZ() + .8);
        return world.getCollisionBoxes(null, box).isEmpty() && world.checkNoEntityCollision(box);
    }

    private static void say(EntityPlayerMP player, String text) {
        player.sendMessage(new TextComponentString("<Римуру Темпест> " + text));
    }

    private static void firework(EntityPlayerMP player) {
        player.connection.sendPacket(new net.minecraft.network.play.server.SPacketParticles(
                net.minecraft.util.EnumParticleTypes.FIREWORKS_SPARK, false, (float) player.posX,
                (float) player.posY + 2, (float) player.posZ, .6f, .7f, .6f, .15f, 85));
        player.world.playSound(null, player.posX, player.posY, player.posZ, net.minecraft.init.SoundEvents.ENTITY_FIREWORK_BLAST,
                net.minecraft.util.SoundCategory.PLAYERS, .65f, 1.2f);
    }

    private void finish() {
        visitor.setDead();
        visitor = null;
        guest = null;
    }

    private VisitData data(MinecraftServer server) {
        net.minecraft.world.storage.MapStorage storage = server.getWorld(0).getMapStorage();
        VisitData data = (VisitData) storage.getOrLoadData(VisitData.class, KEY);
        if (data == null) {
            data = new VisitData(KEY);
            storage.setData(KEY, data);
        }
        return data;
    }

    public static final class VisitData extends WorldSavedData {
        long deadline;
        boolean done;
        private boolean fixedDelay;

        public VisitData(String name) { super(name); }

        void schedule(long now) {
            if (fixedDelay) return;
            long limit = now + DELAY;
            deadline = deadline == 0 ? limit : Math.min(deadline, limit);
            fixedDelay = true;
            markDirty();
        }

        void complete() { done = true; markDirty(); }

        @Override public void readFromNBT(NBTTagCompound nbt) {
            deadline = nbt.getLong("deadline");
            done = nbt.getBoolean("done");
            fixedDelay = nbt.getBoolean("fixedDelay");
        }

        @Override public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            nbt.setLong("deadline", deadline);
            nbt.setBoolean("done", done);
            nbt.setBoolean("fixedDelay", fixedDelay);
            return nbt;
        }
    }
}
