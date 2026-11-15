package com.e2e.ae2expert.encounter;

import net.minecraft.entity.EntityCreature;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public final class Visitor extends EntityCreature {
    public Visitor(World world) {
        super(world);
        setSize(.6f, 1.8f);
        setNoAI(true);
        setSilent(true);
        setCustomNameTag("Римуру Темпест");
        setAlwaysRenderNameTag(true);
    }

    @Override public boolean attackEntityFrom(DamageSource source, float damage) { return false; }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote && ticksExisted > 45 * 20) setDead();
    }

    @Override public boolean writeToNBTOptional(net.minecraft.nbt.NBTTagCompound tag) { return false; }
}
