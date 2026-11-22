package com.crazyclinic.core.content;

import com.crazyclinic.core.world.EnemyType;
import com.crazyclinic.core.world.WeaponType;

/**
 * 全局数值表（复刻基准，来自 docs/research.md §5.7）。
 * 关卡代理可在本文件中调整平衡；改完记得同步 docs/design.md。
 */
public final class Balance {
    // ---- 玩家 ----
    public static final float BASE_HP = 100f;
    public static final float BASE_STAMINA = 100f;
    public static final float BASE_SPEED = 202f;     // px/s（2.8 tile/s @72px）
    public static final float SPRINT_SPEED = 331f;   // 4.6 tile/s
    public static final float SPRINT_DRAIN = 14f;    // 体力/秒
    public static final float STAMINA_REGEN = 9f;    // 体力/秒
    public static final float STAMINA_REGEN_DELAY = 0.8f;
    public static final float DODGE_STAMINA = 12f;
    public static final float DODGE_TIME = 0.35f;
    public static final float DODGE_CD = 0.7f;
    public static final float PLAYER_RADIUS = 22f;
    public static final float PICKUP_RADIUS = 46f;

    // ---- 器官 ----
    public static final float ORGAN_HEART_HP = 20f;
    public static final float ORGAN_KIDNEY_HP = 10f;
    public static final float ORGAN_LEGS_SPEED_MUL = 1.10f;
    public static final float ORGAN_LUNGS_STAMINA = 20f;
    public static final float ORGAN_ARMS_MELEE_MUL = 1.15f;
    public static final float ORGAN_LIVER_REGEN = 1f;
    public static final float ORGAN_KIDNEY_POISON_RESIST = 0.5f;
    public static final float ORGAN_BRAIN_HACK_BONUS = 2f;   // 骇客时间 +2s
    public static final float ORGAN_BRAIN_AUTO_HACK = 1f;    // 1=可自动破解一次

    // ---- 物品 ----
    public static final float HEAL_SMALL = 15f;
    public static final float HEAL_MEDIUM = 30f;
    public static final float HEAL_LARGE = 60f;
    public static final float PAINKILLER_TIME = 6f;
    public static final float STEROID_TIME = 8f;
    public static final float STEROID_MELEE_MUL = 2f;
    public static final float ENERGY_TIME = 10f;
    public static final float ENERGY_STAMINA_MUL = 2f;
    public static final float EPO_TIME = 8f;
    public static final float EPO_SPEED_MUL = 1.6f;
    public static final float EPO_SLOW_TIME = 5f;
    public static final float EPO_SLOW_MUL = 0.5f;
    public static final float INVIS_TIME = 6f;

    // ---- 骇客 ----
    public static final float HACK_TIME = 20f;
    public static final int HACK_COLS = 12;
    public static final int HACK_ROWS = 16;
    public static final int HACK_CELL = 60;

    // ---- 警报 ----
    public static final float ALERT_DECAY = 0.05f;
    public static final float ALERT_GAIN = 0.25f;

    private Balance() {}

    public static final class WeaponStats {
        public final float damage;
        public final float range;         // px
        public final float cooldown;      // 秒
        public final int maxAmmo;         // -1 = 无限
        public final boolean melee;
        public final boolean automatic;
        public final int pellets;
        public final float spreadDeg;
        public final float projectileSpeed;
        public final float arcDeg;

        WeaponStats(float damage, float range, float cooldown, int maxAmmo, boolean melee,
                    boolean automatic, int pellets, float spreadDeg, float projectileSpeed, float arcDeg) {
            this.damage = damage;
            this.range = range;
            this.cooldown = cooldown;
            this.maxAmmo = maxAmmo;
            this.melee = melee;
            this.automatic = automatic;
            this.pellets = pellets;
            this.spreadDeg = spreadDeg;
            this.projectileSpeed = projectileSpeed;
            this.arcDeg = arcDeg;
        }
    }

    private static final WeaponStats[] WEAPONS = new WeaponStats[WeaponType.values().length];

    static {
        WEAPONS[WeaponType.FIST.ordinal()] = new WeaponStats(8f, 80f, 0.5f, -1, true, true, 1, 0f, 0f, 70f);
        WEAPONS[WeaponType.AXE.ordinal()] = new WeaponStats(34f, 79f, 0.75f, -1, true, false, 1, 0f, 0f, 90f);
        WEAPONS[WeaponType.SCALPEL.ordinal()] = new WeaponStats(14f, 504f, 0.35f, -1, false, false, 1, 1.5f, 900f, 0f);
        WEAPONS[WeaponType.PISTOL.ordinal()] = new WeaponStats(26f, 648f, 0.28f, 12, false, true, 1, 3f, 1300f, 0f);
        WEAPONS[WeaponType.SHOTGUN.ordinal()] = new WeaponStats(6f, 324f, 0.9f, 6, false, false, 10, 30f, 1000f, 0f);
        WEAPONS[WeaponType.TASER.ordinal()] = new WeaponStats(8f, 360f, 0.8f, 4, false, false, 1, 1f, 900f, 0f);
        WEAPONS[WeaponType.DEFIBRILLATOR.ordinal()] = new WeaponStats(10f, 72f, 1.2f, 3, true, false, 1, 0f, 0f, 60f);
        WEAPONS[WeaponType.BUNSEN.ordinal()] = new WeaponStats(30f, 158f, 0.08f, 8, false, true, 1, 0f, 0f, 26f);
        WEAPONS[WeaponType.VIAL.ordinal()] = new WeaponStats(90f, 130f, 1.0f, 1, false, false, 1, 0f, 700f, 0f);
    }

    public static WeaponStats weapon(WeaponType t) {
        return WEAPONS[t.ordinal()];
    }

    /** 近战伤害（含器官/类固醇加成由引擎计算，这里给基础值）。 */
    public static float meleeDamage(WeaponType t) {
        return weapon(t).damage;
    }

    public static final class EnemyStats {
        public final float hp;
        public final float speed;
        public final float damage;
        public final float sightRange;
        public final float attackRange;
        public final float attackCooldown;
        public final float radius;
        public final boolean ranged;
        public final boolean turret;
        public final float projectileSpeed;

        EnemyStats(float hp, float speed, float damage, float sightRange, float attackRange,
                   float attackCooldown, float radius, boolean ranged, boolean turret, float projectileSpeed) {
            this.hp = hp;
            this.speed = speed;
            this.damage = damage;
            this.sightRange = sightRange;
            this.attackRange = attackRange;
            this.attackCooldown = attackCooldown;
            this.radius = radius;
            this.ranged = ranged;
            this.turret = turret;
            this.projectileSpeed = projectileSpeed;
        }
    }

    private static final EnemyStats[] ENEMIES = new EnemyStats[EnemyType.values().length];

    static {
        ENEMIES[EnemyType.GUARD.ordinal()] = new EnemyStats(60f, 115f, 12f, 324f, 300f, 1.6f, 24f, true, false, 620f);
        ENEMIES[EnemyType.DOCTOR.ordinal()] = new EnemyStats(80f, 144f, 22f, 288f, 70f, 1.0f, 26f, false, false, 0f);
        ENEMIES[EnemyType.FLU_PATIENT.ordinal()] = new EnemyStats(40f, 72f, 6f, 0f, 108f, 2.2f, 24f, true, false, 380f);
        ENEMIES[EnemyType.TECH.ordinal()] = new EnemyStats(50f, 108f, 14f, 360f, 360f, 2.0f, 24f, true, false, 560f);
        ENEMIES[EnemyType.NURSE.ordinal()] = new EnemyStats(70f, 130f, 18f, 324f, 72f, 1.2f, 24f, false, false, 0f);
        ENEMIES[EnemyType.SENTRY.ordinal()] = new EnemyStats(40f, 0f, 8f, 432f, 432f, 0.5f, 26f, true, true, 900f);
        ENEMIES[EnemyType.BOSS.ordinal()] = new EnemyStats(400f, 158f, 24f, 360f, 600f, 1.2f, 34f, true, false, 700f);
    }

    public static EnemyStats enemy(EnemyType t) {
        return ENEMIES[t.ordinal()];
    }
}
