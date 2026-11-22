package com.crazyclinic.core.world;

/** 地面血迹/弹孔等贴花。 */
public final class Decal {
    public float x, y;
    public float angle;
    public float scale = 1f;
    public float life, maxLife;
    public int color;
    public int kind; // 0 血泊 1 弹孔 2 焦痕
}
