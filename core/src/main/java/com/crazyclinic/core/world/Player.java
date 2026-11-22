package com.crazyclinic.core.world;

import com.crazyclinic.core.SaveData;
import com.crazyclinic.core.content.Balance;

/** 主角卡特。 */
public final class Player {
    public float x, y;
    public float angle;          // 面朝方向（弧度）
    public float aimAngle;       // 瞄准方向（弧度）
    public float radius = 22f;

    public float hp, maxHp;
    public float stamina, maxStamina;
    public float speed;          // 基础移动速度 px/s
    public float sprintSpeed;

    public WeaponType weapon = WeaponType.AXE;
    public final boolean[] owned = new boolean[WeaponType.values().length];
    public final int[] ammo = new int[WeaponType.values().length];

    public ItemType item;
    public int itemCount;

    public boolean hasKeycard;
    public boolean dead;
    public boolean moving;
    public boolean sprinting;

    public float invuln;         // 无敌剩余秒数
    public float hurtFlash;
    public float dodgeTimer;     // 闪避无敌剩余
    public float dodgeCd;
    public float attackCd;
    public float muzzleFlash;
    public float boardTimer;
    public float speedBoostTimer;
    public float epoSlowTimer;
    public float damageBoostTimer;
    public float noDamageTimer;
    public float invisTimer;
    public float staminaRegenDelay;
    public float footstepTimer;
    public float fireHold;       // 本生灯持续喷射
    public float poisonTimer;
    public float poisonTick;

    public void init(SaveData save) {
        maxHp = Balance.BASE_HP;
        speed = Balance.BASE_SPEED;
        sprintSpeed = Balance.SPRINT_SPEED;
        maxStamina = Balance.BASE_STAMINA;
        if (save != null) {
            for (Organ o : Organ.values()) {
                if (save.hasOrgan(o)) applyOrganBonus(o);
            }
        }
        hp = maxHp;
        stamina = maxStamina;
        for (int i = 0; i < owned.length; i++) {
            owned[i] = false;
            ammo[i] = 0;
        }
        owned[WeaponType.FIST.ordinal()] = true;
        owned[WeaponType.AXE.ordinal()] = true;
        ammo[WeaponType.FIST.ordinal()] = -1;
        ammo[WeaponType.AXE.ordinal()] = -1;
        weapon = WeaponType.AXE;
        item = null;
        itemCount = 0;
    }

    /** 运行时拾取器官时调用（同时引擎会写 SaveData）。 */
    public void applyOrganBonus(Organ organ) {
        switch (organ) {
            case HEART: maxHp += Balance.ORGAN_HEART_HP; break;
            case KIDNEY: maxHp += Balance.ORGAN_KIDNEY_HP; break;
            case LEGS: speed *= Balance.ORGAN_LEGS_SPEED_MUL; sprintSpeed *= Balance.ORGAN_LEGS_SPEED_MUL; break;
            case LUNGS: maxStamina += Balance.ORGAN_LUNGS_STAMINA; break;
            case ARMS: /* 伤害倍率在 meleeMultiplier() 中计算 */ break;
            default: break;
        }
        hp = Math.min(hp, maxHp);
        stamina = Math.min(stamina, maxStamina);
    }

    public float meleeMultiplier() {
        // 引擎在创建世界时根据存档设置；运行时拾取用字段记录
        return armsBonus ? Balance.ORGAN_ARMS_MELEE_MUL : 1f;
    }

    public boolean armsBonus;

    public boolean hasWeapon(WeaponType w) {
        return owned[w.ordinal()];
    }

    public void giveWeapon(WeaponType w) {
        owned[w.ordinal()] = true;
        int max = Balance.weapon(w).maxAmmo;
        ammo[w.ordinal()] = max;
        weapon = w;
    }

    public void giveAmmo(WeaponType w, int n) {
        if (!owned[w.ordinal()]) return;
        int max = Balance.weapon(w).maxAmmo;
        if (max < 0) return;
        ammo[w.ordinal()] = Math.min(max, ammo[w.ordinal()] + n);
    }

    public boolean isInvisible() {
        return invisTimer > 0f;
    }

    public boolean isInvulnerable() {
        return invuln > 0f || dodgeTimer > 0f || noDamageTimer > 0f;
    }
}
