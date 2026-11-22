package com.crazyclinic.core;

import com.crazyclinic.core.world.World;

/**
 * 触屏控制布局（逻辑坐标 720x1280，控制区 y=960..1280）。
 * 引擎命中判定与美术绘制必须共用本类常量。
 */
public final class Controls {
    // 移动摇杆：左半屏动态摇杆
    public static final float MOVE_ZONE_MAX_X = 340f;
    public static final float STICK_RADIUS = 150f;
    public static final float STICK_KNOB = 56f;
    public static final float MOVE_DEFAULT_X = 196f;
    public static final float MOVE_DEFAULT_Y = 1120f;
    public static final float AIM_DEFAULT_X = 560f;
    public static final float AIM_DEFAULT_Y = 1090f;
    public static final float AIM_ZONE_MIN_X = 340f;

    // 右侧按钮
    public static final float ATTACK_X = 556f, ATTACK_Y = 1046f, ATTACK_R = 86f;
    public static final float INTERACT_X = 430f, INTERACT_Y = 1186f, INTERACT_R = 56f;
    public static final float DODGE_X = 668f, DODGE_Y = 1186f, DODGE_R = 56f;
    public static final float WEAPON_X = 78f, WEAPON_Y = 990f, WEAPON_W = 128f, WEAPON_H = 66f;
    public static final float ITEM_X = 222f, ITEM_Y = 990f, ITEM_W = 128f, ITEM_H = 66f;

    // 世界层按钮
    public static final float PAUSE_X = 660f, PAUSE_Y = 74f, PAUSE_R = 38f;

    public static final float CONTROLS_TOP = World.VIEW_H;

    private Controls() {}

    public static boolean inWorld(float y) {
        return y < World.VIEW_H;
    }

    public static boolean hit(float x, float y, float cx, float cy, float r) {
        return MathUtil.dist2(x, y, cx, cy) <= r * r;
    }
}
