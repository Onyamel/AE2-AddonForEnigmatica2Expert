package com.e2e.ae2expert;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.*;
import java.util.*;
import appeng.api.config.*;
import appeng.api.networking.*;
import appeng.api.storage.*;
import appeng.api.storage.data.*;
import com.e2e.ae2expert.api.IChannelCapacity;
import com.e2e.ae2expert.core.ExpertTransformer;
import com.e2e.ae2expert.machine.MachineEnergy;
import com.e2e.ae2expert.machine.TileRfEuConverter;
import com.e2e.ae2expert.storage.LongCellInventory;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** Проверки границ, сохранения и настоящего преобразованного байткода AE2. */
public final class RegressionChecks {
    private static int checks;
    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }
    public static void main(String[] args) throws Exception {
        energy();
        storage();
        channels();
        checks += VisualRegressionChecks.run();
        System.out.println("Пройдено регрессионных проверок: " + checks);
    }

    private static void energy() throws Exception {
        int[] saves = {0};
        MachineEnergy e = new MachineEnergy(20000, 1000, 0, () -> saves[0]++);
        e.load(19000);
        check(e.getEnergyStored() == 19000, "Загрузка энергии не ограничена скоростью приёма");
        check(e.extractEnergy(40, false) == 0, "Внешний выход камеры закрыт");
        check(e.consume(40) == 40 && e.getEnergyStored() == 18960, "Внутренний расход работает");
        check(e.receiveEnergy(1000, true) == 1000 && e.getEnergyStored() == 18960, "Симуляция приёма");
        check(e.receiveEnergy(2000, false) == 1000, "Ограничение внешнего входа");
        check(e.receiveEnergy(-100, false) == 0 && e.extractEnergy(-100, false) == 0, "Отрицательные запросы");
        e.load(999999);
        check(e.getEnergyStored() == 20000, "Верхняя граница NBT");
        e.load(-1);
        check(e.getEnergyStored() == 0 && saves[0] == 2, "Нижняя граница и отметки сохранения");
        TileEntity.register("ae2expert:regression_converter", TileRfEuConverter.class);
        TileRfEuConverter tile = new TileRfEuConverter();
        Field field = TileRfEuConverter.class.getDeclaredField("rf");
        field.setAccessible(true);
        MachineEnergy rf = (MachineEnergy) field.get(tile);
        check(rf.canReceive() && !rf.canExtract(), "Направление RF -> EU");
        rf.load(40000);
        Method toEu = TileRfEuConverter.class.getDeclaredMethod("convertRfToEu");
        toEu.setAccessible(true);
        toEu.invoke(tile);
        check(rf.getEnergyStored() == 38976 && Math.abs(tile.getEuBuffer() - 230.4) < 1e-8, "Расход и потери RF -> EU");
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeToNBT(tag);
        TileRfEuConverter restored = new TileRfEuConverter();
        restored.readFromNBT(tag);
        restored.readFromNBT(tag);
        check(restored.getEnergyStored() == 38976 && Math.abs(restored.getEuBuffer() - 230.4) < 1e-8, "Повторное чтение NBT");
        tile.toggleMode();
        check(!rf.canReceive() && rf.canExtract() && rf.receiveEnergy(10, false) == 0, "Направление EU -> RF");
        Method toRf = TileRfEuConverter.class.getDeclaredMethod("convertEuToRf");
        toRf.setAccessible(true);
        double before = tile.getEuBuffer();
        int oldRf = rf.getEnergyStored();
        toRf.invoke(tile);
        check(rf.getEnergyStored() - oldRf == (int) Math.floor(before * 3.6), "Выход EU -> RF");
        check(rf.getEnergyStored() - oldRf <= (before - tile.getEuBuffer()) * 3.6 + 1e-8, "Энергия не создаётся");
        tag.setInteger("rf", 40000);
        tag.setDouble("eu", Double.NaN);
        restored.readFromNBT(tag);
        check(restored.getEnergyStored() == 40000 && restored.getEuBuffer() == 0, "Полный буфер и защита NaN");
    }

    private static void storage() {
        // Канал проверяет арифметику независимо от клиентской инициализации AE2.
        TestChannel channel = new TestChannel(8);
        LongCellInventory<TestStack> cell = new LongCellInventory<>(ItemStack.EMPTY, inv -> { }, channel, 1L << 29, 63, 8, 1);
        long big = 3_000_000_000L;
        check(cell.injectItems(new TestStack("A", big), Actionable.SIMULATE, null) == null && cell.getStoredItemCount() == 0, "Симуляция большого ввода");
        cell.injectItems(new TestStack("A", big), Actionable.MODULATE, null);
        check(cell.getStoredItemCount() == big, "Количество больше Integer.MAX_VALUE");
        TestStack extracted = cell.extractItems(new TestStack("A", big), Actionable.MODULATE, null);
        check(extracted.getStackSize() == big && cell.getStoredItemTypes() == 0, "Извлечение освобождает тип");
        for (int i = 0; i < 63; i++) cell.injectItems(new TestStack("type" + i, 1), Actionable.MODULATE, null);
        check(cell.getStoredItemTypes() == 63 && cell.injectItems(new TestStack("extra", 1), Actionable.MODULATE, null) != null, "Лимит 63 типа");
        cell.extractItems(new TestStack("type0", 1), Actionable.MODULATE, null);
        check(cell.injectItems(new TestStack("extra", 1), Actionable.MODULATE, null) == null, "Повторное использование освободившегося типа");
        LongCellInventory<TestStack> small = new LongCellInventory<>(ItemStack.EMPTY, inv -> { }, channel, 10, 3, 2, 1);
        TestStack rest = small.injectItems(new TestStack("A", 100), Actionable.SIMULATE, null);
        check(rest != null && rest.count == 36 && small.getStoredItemCount() == 0, "Частичный ввод учитывает стоимость типа");
        rest = small.injectItems(new TestStack("A", 100), Actionable.MODULATE, null);
        check(rest.count == 36 && small.getStoredItemCount() == 64 && small.getFreeBytes() == 0, "Фактический частичный ввод");
        small.extractItems(new TestStack("A", 1), Actionable.MODULATE, null);
        check(small.getRemainingItemCount() == 1 && small.injectItems(new TestStack("A", 1), Actionable.MODULATE, null) == null, "Использование неполного байта");
        LongCellInventory<TestStack> fluid = new LongCellInventory<>(ItemStack.EMPTY, inv -> { }, new TestChannel(8000), 1L << 29, 63, 8, 1);
        fluid.injectItems(new TestStack("water", 4_000_000_000_000L), Actionable.MODULATE, null);
        check(fluid.getStoredItemCount() == 4_000_000_000_000L, "Жидкости за пределами int");
        LongCellInventory<TestStack> infinite = new LongCellInventory<>(ItemStack.EMPTY, inv -> { }, channel, 1L << 50, 1024, 1024, 8);
        for (int i = 0; i < 1024; i++) infinite.injectItems(new TestStack("i" + i, 1), Actionable.MODULATE, null);
        check(infinite.getStoredItemTypes() == 1024 && !infinite.canHoldNewItem(), "Лимит 1024 типа");
        ItemStack persisted = new ItemStack((Item) null);
        LongCellInventory<TestStack> saved = new LongCellInventory<>(persisted, null, channel, 1L << 50, 1024, 1024, 8);
        saved.injectItems(new TestStack("long", 5_000_000_000L), Actionable.MODULATE, null);
        LongCellInventory<TestStack> loaded = new LongCellInventory<>(persisted, null, channel, 1L << 50, 1024, 1024, 8);
        check(loaded.getStoredItemCount() == 5_000_000_000L, "Сохранение long в NBT и загрузка");
    }

    private static byte[] bytes(String name) throws Exception {
        try (InputStream input = RegressionChecks.class.getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (input == null) throw new IllegalStateException(name);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int count;
            while ((count = input.read(chunk)) != -1) out.write(chunk, 0, count);
            return out.toByteArray();
        }
    }

    private static void channels() throws Exception {
        ExpertTransformer transformer = new ExpertTransformer();
        Map<String, byte[]> classes = new HashMap<>();
        for (String name : Arrays.asList("appeng.me.GridNode", "appeng.me.GridConnection", "appeng.parts.networking.PartCable")) {
            byte[] original = bytes(name), changed = transformer.transform(name, name, original);
            check(!Arrays.equals(original, changed), "Патч применён: " + name);
            classes.put(name, changed);
        }
        // Убираем только создание полной сети из тестового узла: проверяем счётчики
        // соединения, не запускаем клиентские реестры AE2 в отдельной JVM.
        org.objectweb.asm.tree.ClassNode testNode = new org.objectweb.asm.tree.ClassNode();
        new org.objectweb.asm.ClassReader(classes.get("appeng.me.GridNode")).accept(testNode, 0);
        for (org.objectweb.asm.tree.MethodNode method : testNode.methods) if (method.name.equals("getInternalGrid")) {
            method.instructions = new org.objectweb.asm.tree.InsnList();
            method.instructions.add(new org.objectweb.asm.tree.InsnNode(org.objectweb.asm.Opcodes.ACONST_NULL));
            method.instructions.add(new org.objectweb.asm.tree.InsnNode(org.objectweb.asm.Opcodes.ARETURN));
            method.tryCatchBlocks.clear();
            method.localVariables = null;
        }
        org.objectweb.asm.ClassWriter testWriter = new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
        testNode.accept(testWriter);
        classes.put("appeng.me.GridNode", testWriter.toByteArray());
        classes.put("com.e2e.ae2expert.core.ChannelHook", bytes("com.e2e.ae2expert.core.ChannelHook"));
        ClassLoader isolated = new ClassLoader(RegressionChecks.class.getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!classes.containsKey(name)) return super.loadClass(name, resolve);
                Class<?> type = findLoadedClass(name);
                if (type == null) { byte[] data = classes.get(name); type = defineClass(name, data, 0, data.length); }
                if (resolve) resolveClass(type);
                return type;
            }
        };
        Class<?> nodeType = isolated.loadClass("appeng.me.GridNode");
        Class<?> connectionType = isolated.loadClass("appeng.me.GridConnection");
        // Загрузка и разрешение методов выполняют проверку bytecode verifier JVM.
        isolated.loadClass("appeng.parts.networking.PartCable").getDeclaredMethods();
        Method max = nodeType.getDeclaredMethod("getMaxChannels");
        max.setAccessible(true);
        Method canCarry = connectionType.getMethod("canSupportMoreChannels");
        Constructor<?> constructor = connectionType.getDeclaredConstructor(nodeType, nodeType, appeng.api.util.AEPartLocation.class);
        constructor.setAccessible(true);
        Field data = connectionType.getDeclaredField("channelData");
        data.setAccessible(true);
        for (int capacity : new int[] {32, 64, 128, 256}) {
            Object a = nodeType.getConstructor(IGridBlock.class).newInstance(gridBlock(capacity));
            Object b = nodeType.getConstructor(IGridBlock.class).newInstance(gridBlock(capacity));
            check((int) max.invoke(a) == capacity, "Ёмкость берётся из части, а не AENetworkProxy");
            Object connection = constructor.newInstance(a, b, appeng.api.util.AEPartLocation.EAST);
            data.setInt(connection, capacity - 1);
            check((boolean) canCarry.invoke(connection), "Предпоследний канал доступен: " + capacity);
            connectionType.getMethod("incrementChannelCount", int.class).invoke(connection, 1);
            check(!(boolean) canCarry.invoke(connection), "Предел канала: " + capacity);
            connectionType.getMethod("finalizeChannels").invoke(connection);
            check((int) connectionType.getMethod("getUsedChannels").invoke(connection) == capacity, "Счётчик не оборачивается: " + capacity);
            connectionType.getMethod("setControllerRoute", appeng.me.pathfinding.IPathItem.class, boolean.class).invoke(connection, null, true);
            check((int) connectionType.getMethod("getUsedChannels").invoke(connection) == capacity && (data.getInt(connection) & 0xffff) == 0, "Сброс сохраняет опубликованный счётчик");
        }
        Object dense = nodeType.getConstructor(IGridBlock.class).newInstance(gridBlock(0));
        Field compressed = nodeType.getDeclaredField("compressedData");
        compressed.setAccessible(true);
        compressed.setInt(dense, 2);
        check((int) max.invoke(dense) == 32, "Штатный плотный кабель сохраняет 32 канала");
    }

    private static IGridBlock gridBlock(int capacity) {
        IGridHost host = (IGridHost) Proxy.newProxyInstance(RegressionChecks.class.getClassLoader(),
                capacity > 0 ? new Class<?>[] {IGridHost.class, IChannelCapacity.class} : new Class<?>[] {IGridHost.class},
                (proxy, method, args) -> method.getName().equals("getChannelCapacity") ? capacity : null);
        return (IGridBlock) Proxy.newProxyInstance(RegressionChecks.class.getClassLoader(), new Class<?>[] {IGridBlock.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getMachine")) return host;
                    if (method.getName().equals("getFlags")) return EnumSet.of(GridFlags.DENSE_CAPACITY);
                    return null;
                });
    }

    private static final class TestChannel implements IStorageChannel<TestStack> {
        private final int units;
        TestChannel(int units) { this.units = units; }
        @Override public int getUnitsPerByte() { return units; }
        @Override public IItemList<TestStack> createList() { return new TestList(); }
        @Override public TestStack createStack(Object object) { return (TestStack) object; }
        @Override public TestStack readFromPacket(ByteBuf buf) { return null; }
        @Override public TestStack createFromNBT(NBTTagCompound tag) { return new TestStack(tag.getString("name"), tag.getLong("count")); }
    }
    private static final class TestList implements IItemList<TestStack> {
        private final Map<String, TestStack> values = new LinkedHashMap<>();
        @Override public void add(TestStack stack) { addStorage(stack); }
        @Override public void addStorage(TestStack stack) { values.computeIfAbsent(stack.name, key -> new TestStack(key, 0)).count += stack.count; }
        @Override public TestStack findPrecise(TestStack key) { return values.get(key.name); }
        @Override public Collection<TestStack> findFuzzy(TestStack key, FuzzyMode mode) { return values.values(); }
        @Override public boolean isEmpty() { return values.isEmpty(); }
        @Override public void addCrafting(TestStack stack) { addStorage(stack); }
        @Override public void addRequestable(TestStack stack) { addStorage(stack); }
        @Override public TestStack getFirstItem() { return iterator().hasNext() ? iterator().next() : null; }
        @Override public int size() { return values.size(); }
        @Override public Iterator<TestStack> iterator() { return values.values().iterator(); }
        @Override public void resetStatus() { values.clear(); }
    }
    private static final class TestStack implements IAEStack<TestStack> {
        private final String name;
        private long count;
        TestStack(String name, long count) { this.name = name; this.count = count; }
        @Override public void add(TestStack other) { count += other.count; }
        @Override public long getStackSize() { return count; }
        @Override public TestStack setStackSize(long count) { this.count = count; return this; }
        @Override public long getCountRequestable() { return 0; }
        @Override public TestStack setCountRequestable(long value) { return this; }
        @Override public boolean isCraftable() { return false; }
        @Override public TestStack setCraftable(boolean value) { return this; }
        @Override public TestStack reset() { count = 0; return this; }
        @Override public boolean isMeaningful() { return count > 0; }
        @Override public void incStackSize(long value) { count += value; }
        @Override public void decStackSize(long value) { count -= value; }
        @Override public void incCountRequestable(long value) { }
        @Override public void decCountRequestable(long value) { }
        @Override public void writeToNBT(NBTTagCompound tag) { tag.setString("name", name); tag.setLong("count", count); }
        @Override public boolean fuzzyComparison(TestStack other, FuzzyMode mode) { return name.equals(other.name); }
        @Override public void writeToPacket(ByteBuf buf) { }
        @Override public TestStack copy() { return new TestStack(name, count); }
        @Override public TestStack empty() { return new TestStack(name, 0); }
        @Override public boolean isItem() { return false; }
        @Override public boolean isFluid() { return false; }
        @Override public IStorageChannel<TestStack> getChannel() { return null; }
        @Override public ItemStack asItemStackRepresentation() { return ItemStack.EMPTY; }
    }
}
