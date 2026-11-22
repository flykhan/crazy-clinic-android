package com.crazyclinic.core.world;

public final class Particle {
    public float x, y, vx, vy;
    public float life, maxLife;
    public float size;
    public float gravity;
    public float rotation;
    public float spin;
    public int color;
    public int kind; // 0 光点 1 血滴 2 火花 3 烟雾 4 弹壳 5 碎屑
}
