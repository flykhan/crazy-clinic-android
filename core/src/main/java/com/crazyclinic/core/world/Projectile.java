package com.crazyclinic.core.world;

public final class Projectile {
    public ProjectileType type;
    public float x, y, vx, vy;
    public float radius = 5f;
    public float life = 2f;
    public float damage;
    public boolean fromPlayer;
    public boolean explosive;
    public boolean poison;
    public boolean hitWall;
    public float spin;
    public float gravity;
    public float age;
    public float ownerAngle;
}
