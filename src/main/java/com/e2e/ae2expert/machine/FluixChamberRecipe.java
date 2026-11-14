package com.e2e.ae2expert.machine;

import java.util.Arrays;
import java.util.List;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/** Единое описание обработки для механизма, JEI/HEI и проверки рецепта. */
public final class FluixChamberRecipe {
    public static final FluixChamberRecipe INSTANCE=new FluixChamberRecipe();
    public static final String UID="ae2expert.fluix_chamber";
    public static final int DURATION=100;
    public static final int RF_PER_TICK=40;
    public static final int TOTAL_RF=DURATION*RF_PER_TICK;
    private FluixChamberRecipe() { }
    public List<ItemStack> inputs() {
        return Arrays.asList(ModMachineItems.ae2Material(1,1),new ItemStack(Items.REDSTONE),new ItemStack(Items.QUARTZ));
    }
    public ItemStack output() { return ModMachineItems.fluixCrystal(); }
    public static boolean accepts(int slot,ItemStack stack) {
        if(stack==null||stack.isEmpty()||stack.getItem().getRegistryName()==null)return false;
        String item=stack.getItem().getRegistryName().toString();
        return slot==0?item.equals("appliedenergistics2:material")&&stack.getMetadata()==1
                :slot==1?item.equals("minecraft:redstone")&&stack.getMetadata()==0
                :slot==2&&item.equals("minecraft:quartz")&&stack.getMetadata()==0;
    }
}
