package com.crazyclinic.core.world;

import com.crazyclinic.core.content.Balance;

public final class Enemy {
    public final EnemyType type;
    public final boolean turret;
    public float x, y, angle, radius;
    public float hp, maxHp;
    public float speed;
    public float sightRange;
    public float attackRange;
    public float attackCooldown;
    public float damage;

    public EnemyState state = EnemyState.PATROL;
    public float stateTimer;
    public float alertTimer;
    public float attackTimer;
    public float wanderTimer;
    public float wanderAngle;
    public float homeX, homeY;
    public float hurtFlash;
    public float deathTimer;
    public float muzzleFlash;
    public float stunTimer;
    public float reviveTimer;    // 护士复活读条
    public float bodyTimer;      // 尸体保留时间
    public boolean dead;

    public Enemy(EnemyType type) {
        this.type = type;
        Balance.EnemyStats s = Balance.enemy(type);
        this.turret = (type == EnemyType.SENTRY);
        this.maxHp = s.hp;
        this.hp = s.hp;
        this.speed = s.speed;
        this.sightRange = s.sightRange;
        this.attackRange = s.attackRange;
        this.attackCooldown = s.attackCooldown;
        this.damage = s.damage;
        this.radius = s.radius;
    }

    public boolean isDead() {
        return dead || state == EnemyState.DEAD;
    }
}
