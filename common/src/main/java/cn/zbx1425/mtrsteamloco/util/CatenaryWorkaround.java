package cn.zbx1425.mtrsteamloco.util;

public class CatenaryWorkaround {
    private static boolean IS_RENDER = false;

    private CatenaryWorkaround() {}

    public static boolean isRender() {
        return IS_RENDER;
    }

    public static void beginRender() {
        IS_RENDER = true;
    }

    public static void endRender() {
        IS_RENDER = false;
    }
}
