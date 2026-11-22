package com.crazyclinic.core.world;

public final class EnemySpawn {
    public final EnemyType type;
    public final int tx, ty;

    public EnemySpawn(EnemyType type, int tx, int ty) {
        this.type = type;
        this.tx = tx;
        this.ty = ty;
    }
}
