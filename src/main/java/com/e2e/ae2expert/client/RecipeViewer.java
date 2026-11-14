package com.e2e.ae2expert.client;

/** Мост не содержит типов JEI, поэтому окно машины работает и без этого мода. */
public final class RecipeViewer {
    private static Runnable opener;
    private RecipeViewer() { }
    public static void register(Runnable action) { opener=action; }
    public static boolean available() { return opener!=null; }
    public static void open() { if(opener!=null)opener.run(); }
}
