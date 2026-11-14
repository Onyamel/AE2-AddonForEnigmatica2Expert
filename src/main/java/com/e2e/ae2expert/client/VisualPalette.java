package com.e2e.ae2expert.client;

/** Приглушённые технологические акценты; серый корпус остаётся нейтральным. */
public final class VisualPalette {
    private VisualPalette() { }
    public static int cable(int channels) {
        switch(channels) { case 32:return 0x82929F;case 64:return 0x859B8D;case 128:return 0x7D9BAC;default:return 0xA18BAA; }
    }
    public static int color(String name, int tint) {
        if(name.equals("rf_eu_converter"))return tint==1?0x7D9BAC:0xB39A73;
        if(name.equals("fluix_chamber"))return 0xA18BAA;
        if(name.startsWith("cable_"))return cable(Integer.parseInt(name.substring(6)));
        if(name.equals("assembler_2")||name.equals("interface_18"))return 0x859B8D;
        if(name.equals("assembler_4")||name.equals("interface_27"))return 0x7D9BAC;
        if(name.equals("assembler_8")||name.equals("interface_36"))return 0x8992AA;
        if(name.equals("assembler_16")||name.equals("interface_48"))return 0xA18BAA;
        if(name.equals("assembler_32")||name.equals("interface_64"))return 0xB39A73;
        return 0xFFFFFF;
    }
    public static int shade(int rgb, double factor) {
        return (int)((rgb>>16&255)*factor)<<16|(int)((rgb>>8&255)*factor)<<8|(int)((rgb&255)*factor);
    }
}
