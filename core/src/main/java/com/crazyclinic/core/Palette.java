package com.crazyclinic.core;

/** 全局配色（ARGB）。美术/UI 可在此语义色基础上扩展。 */
public final class Palette {
    // 基础
    public static final int BLACK = 0xFF000000;
    public static final int WHITE = 0xFFF2F4F7;
    public static final int BG_DARK = 0xFF0B0D12;
    public static final int BG_DEEP = 0xFF101319;
    public static final int PANEL = 0xFF161B23;
    public static final int PANEL_ALT = 0xFF202732;
    public static final int PANEL_STROKE = 0xFF39434F;
    public static final int TEXT = 0xFFEDF1F5;
    public static final int TEXT_DIM = 0xFF98A3B2;
    public static final int TEXT_DARK = 0xFF5A6675;

    // 强调色
    public static final int RED = 0xFFE04B3A;
    public static final int RED_DARK = 0xFF7E1420;
    public static final int BLOOD = 0xFFA3151F;
    public static final int GREEN = 0xFF57C785;
    public static final int MINT = 0xFF7FD1AE;
    public static final int BLUE = 0xFF4A9BE8;
    public static final int CYAN = 0xFF57D3E0;
    public static final int PURPLE = 0xFFA879E8;
    public static final int GOLD = 0xFFF2B33D;
    public static final int ORANGE = 0xFFF08A3C;
    public static final int ALERT = 0xFFFF5A3C;

    // 医院场景
    public static final int FLOOR_A = 0xFF3E4756;
    public static final int FLOOR_B = 0xFF37404E;
    public static final int FLOOR_GROUT = 0xFF2B323D;
    public static final int WALL_TOP = 0xFF525D6D;
    public static final int WALL_SIDE = 0xFF313943;
    public static final int WALL_TRIM = 0xFF6E7B8C;
    public static final int DOOR = 0xFF9A6A38;
    public static final int DOOR_LOCK = 0xFFC24B3A;
    public static final int ELEVATOR = 0xFF2E7D6B;
    public static final int COMPUTER = 0xFF2C4B6E;
    public static final int LOCKER = 0xFF4C5866;
    public static final int CRATE = 0xFF6B5638;
    public static final int BED = 0xFF8A9AAE;
    public static final int WINDOW = 0xFF2A3C52;
    public static final int LIGHT = 0xFFFFE9B8;
    public static final int SHADOW = 0x66000000;

    private Palette() {}

    public static int argb(int a, int r, int g, int b) {
        return (a << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
    }

    public static int alpha(int color, float a) {
        int ai = (int) (MathUtil.clamp(a, 0f, 1f) * 255f + 0.5f);
        return (color & 0x00FFFFFF) | (ai << 24);
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathUtil.clamp(alpha, 0, 255) << 24);
    }

    public static int mix(int c1, int c2, float t) {
        t = MathUtil.clamp(t, 0f, 1f);
        int a1 = (c1 >>> 24) & 0xff, r1 = (c1 >> 16) & 0xff, g1 = (c1 >> 8) & 0xff, b1 = c1 & 0xff;
        int a2 = (c2 >>> 24) & 0xff, r2 = (c2 >> 16) & 0xff, g2 = (c2 >> 8) & 0xff, b2 = c2 & 0xff;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return argb(a, r, g, b);
    }

    public static int darken(int color, float amount) {
        return mix(color, BLACK, amount);
    }

    public static int lighten(int color, float amount) {
        return mix(color, WHITE, amount);
    }
}
