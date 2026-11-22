package com.crazyclinic.core.game;

import com.crazyclinic.core.Controls;
import com.crazyclinic.core.Game;
import com.crazyclinic.core.HudState;
import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Balance;
import com.crazyclinic.core.world.Enemy;
import com.crazyclinic.core.world.Organ;
import com.crazyclinic.core.world.Player;
import com.crazyclinic.core.world.WeaponType;
import com.crazyclinic.core.world.World;

/**
 * 玩家控制：双摇杆移动/瞄准、推满冲刺、攻速与武器连发、闪避、体力恢复、
 * 自动瞄准辅助与各种持续效果计时。
 */
final class PlayerController {
    private static final float DODGE_SPEED = 640f;
    private static final float FOOTSTEP_INTERVAL = 0.3f;

    void update(GameScene s, Game g, float dt) {
        World w = s.world;
        Player p = w.player;
        if (p == null) return;
        HudState hud = s.hud;

        tickTimers(s, g, p, dt);
        if (p.dead) return;
        ensureValidWeapon(p);

        // ---- 闪避位移 ----
        if (p.dodgeTimer > 0f) {
            Physics.move(w, p, s.dodgeDirX * DODGE_SPEED * dt, s.dodgeDirY * DODGE_SPEED * dt);
            p.moving = true;
            p.angle = p.aimAngle;
            if (MathUtil.chance(dt * 24f)) s.effects.dust(w, p.x, p.y);
            sanitizeBounds(w, p);
            return;
        }

        // ---- 移动 ----
        float mx = hud.moveStickDx, my = hud.moveStickDy;
        float mag = (float) Math.sqrt(mx * mx + my * my);
        p.moving = mag > 0.16f;
        if (p.moving) {
            if (mag > 1f) {
                mx /= mag;
                my /= mag;
                mag = 1f;
            }
            boolean wantSprint = mag > 0.85f && p.stamina > 0.5f;
            p.sprinting = wantSprint;
            float sp = w.playerSpeedNow();
            if (wantSprint) {
                p.stamina = Math.max(0f, p.stamina - Balance.SPRINT_DRAIN * dt);
                p.staminaRegenDelay = Balance.STAMINA_REGEN_DELAY;
                if (p.stamina <= 0f) {
                    p.stamina = 0f;
                    p.sprinting = false;
                }
            }
            Physics.move(w, p, mx * sp * dt, my * sp * dt);
            if (!hud.aimStickActive) {
                p.aimAngle = MathUtil.approachAngle(p.aimAngle, (float) Math.atan2(my, mx), 10f * dt);
            }
            p.footstepTimer -= dt;
            if (p.footstepTimer <= 0f) {
                p.footstepTimer = FOOTSTEP_INTERVAL;
                if (MathUtil.chance(0.6f)) s.effects.dust(w, p.x, p.y + 8f);
            }
        } else {
            p.sprinting = false;
        }

        // ---- 瞄准 ----
        float ax = hud.aimStickDx, ay = hud.aimStickDy;
        float amag = (float) Math.sqrt(ax * ax + ay * ay);
        boolean manualAim = hud.aimStickActive && amag > 0.2f;
        if (manualAim) {
            p.aimAngle = (float) Math.atan2(ay, ax);
            p.angle = p.aimAngle;
            if (amag > 0.55f) s.combat.playerAttack(s, g);
        } else if (hud.attackHeld) {
            // 自动瞄准辅助：没推右摇杆时吸附最近的可见敌人
            Enemy t = findAssistTarget(s, p);
            if (t != null) {
                float want = MathUtil.angle(p.x, p.y, t.x, t.y);
                p.aimAngle = MathUtil.approachAngle(p.aimAngle, want, 14f * dt);
            }
            p.angle = p.aimAngle;
        }
        if (hud.attackHeld) s.combat.playerAttack(s, g);

        // ---- 朝向：没瞄准时朝移动方向 ----
        if (!manualAim && !hud.attackHeld && p.moving) {
            p.angle = p.aimAngle;
        }

        sanitizeBounds(w, p);
    }

    private void tickTimers(GameScene s, Game g, Player p, float dt) {
        p.attackCd = Math.max(0f, p.attackCd - dt);
        p.invuln = Math.max(0f, p.invuln - dt);
        p.dodgeTimer = Math.max(0f, p.dodgeTimer - dt);
        p.dodgeCd = Math.max(0f, p.dodgeCd - dt);
        p.muzzleFlash = Math.max(0f, p.muzzleFlash - dt);
        p.hurtFlash = Math.max(0f, p.hurtFlash - dt * 2.2f);
        p.speedBoostTimer = Math.max(0f, p.speedBoostTimer - dt);
        p.epoSlowTimer = Math.max(0f, p.epoSlowTimer - dt);
        p.damageBoostTimer = Math.max(0f, p.damageBoostTimer - dt);
        p.noDamageTimer = Math.max(0f, p.noDamageTimer - dt);
        p.invisTimer = Math.max(0f, p.invisTimer - dt);
        p.fireHold = Math.max(0f, p.fireHold - dt);
        p.poisonTimer = Math.max(0f, p.poisonTimer - dt);

        // 体力：停止冲刺后延迟回复，能量饮料期间回复翻倍
        if (p.staminaRegenDelay > 0f) {
            p.staminaRegenDelay -= dt;
        } else if (!p.sprinting) {
            float mult = s.energyTimer > 0f ? Balance.ENERGY_STAMINA_MUL : 1f;
            p.stamina = Math.min(p.maxStamina, p.stamina + Balance.STAMINA_REGEN * mult * dt);
        }

        // EPO：药效结束后进入短时间减速
        if (s.pendingEpSlow > 0f) {
            s.pendingEpSlow -= dt;
            if (s.pendingEpSlow <= 0f) {
                p.epoSlowTimer = Math.max(p.epoSlowTimer, Balance.EPO_SLOW_TIME);
            }
        }
        s.energyTimer = Math.max(0f, s.energyTimer - dt);

        // 肝脏：缓慢回血
        if (g != null && g.save != null && g.save.hasOrgan(Organ.LIVER) && p.hp > 0f && p.hp < p.maxHp) {
            p.hp = Math.min(p.maxHp, p.hp + Balance.ORGAN_LIVER_REGEN * dt);
        }
    }

    /** 保证当前武器是已拥有且有弹药的（避免 HUD/手上武器不一致，或空枪卡住）。 */
    private void ensureValidWeapon(Player p) {
        if (p.weapon == null) {
            p.weapon = WeaponType.FIST;
            return;
        }
        if (!p.hasWeapon(p.weapon)) {
            p.weapon = WeaponType.FIST;
            return;
        }
        Balance.WeaponStats ws = Balance.weapon(p.weapon);
        if (ws.maxAmmo > 0 && p.ammo[p.weapon.ordinal()] <= 0) {
            // 自动切到还有弹药/近战武器
            for (WeaponType wt : WeaponType.values()) {
                if (!p.hasWeapon(wt)) continue;
                Balance.WeaponStats o = Balance.weapon(wt);
                if (o.maxAmmo > 0 && p.ammo[wt.ordinal()] <= 0) continue;
                p.weapon = wt;
                return;
            }
            p.weapon = WeaponType.FIST;
        }
    }

    /** 找自动瞄准目标：射程内、有视线、夹角最小的存活敌人。 */
    private Enemy findAssistTarget(GameScene s, Player p) {
        float bestScore = Float.MAX_VALUE;
        Enemy best = null;
        float maxDist = 560f;
        for (int i = 0; i < s.world.enemies.size(); i++) {
            Enemy e = s.world.enemies.get(i);
            if (e.isDead()) continue;
            float d = MathUtil.dist(p.x, p.y, e.x, e.y);
            if (d > maxDist) continue;
            if (!s.world.lineOfSight(p.x, p.y, e.x, e.y)) continue;
            float a = MathUtil.angle(p.x, p.y, e.x, e.y);
            float diff = Math.abs(MathUtil.angleDelta(p.aimAngle, a));
            float score = d + diff * 20f;
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    /** 兜底：把玩家限制在地图内，避免 NaN/越界导致渲染或碰撞异常。 */
    private void sanitizeBounds(World w, Player p) {
        if (!Float.isFinite(p.x) || !Float.isFinite(p.y)) {
            p.x = w.level.spawnX;
            p.y = w.level.spawnY;
            return;
        }
        float r = Math.max(1f, p.radius);
        float maxX = Math.max(r, w.map.pixelWidth() - r);
        float maxY = Math.max(r, w.map.pixelHeight() - r);
        p.x = MathUtil.clamp(p.x, r, maxX);
        p.y = MathUtil.clamp(p.y, r, maxY);
    }

    // ------------------------------------------------------------------
    // 动作
    // ------------------------------------------------------------------

    void dodge(GameScene s, Game g) {
        Player p = s.world.player;
        if (p == null || p.dead) return;
        if (p.dodgeCd > 0f || p.stamina < Balance.DODGE_STAMINA) return;
        p.stamina = Math.max(0f, p.stamina - Balance.DODGE_STAMINA);
        p.staminaRegenDelay = Balance.STAMINA_REGEN_DELAY;
        p.dodgeCd = Balance.DODGE_CD;
        p.dodgeTimer = Balance.DODGE_TIME;
        p.invuln = Math.max(p.invuln, Balance.DODGE_TIME);
        float dx = s.hud.moveStickDx, dy = s.hud.moveStickDy;
        float mag = (float) Math.sqrt(dx * dx + dy * dy);
        if (mag > 0.2f) {
            s.dodgeDirX = dx / mag;
            s.dodgeDirY = dy / mag;
        } else {
            s.dodgeDirX = (float) Math.cos(p.aimAngle);
            s.dodgeDirY = (float) Math.sin(p.aimAngle);
        }
        s.effects.dust(s.world, p.x, p.y);
        if (g != null) g.audio.play(Sfx.SWING);
    }

    /** 循环切换到已有武器（跳过空弹药的枪械）。 */
    void cycleWeapon(GameScene s, Game g) {
        Player p = s.world.player;
        WeaponType[] all = WeaponType.values();
        int idx = p.weapon == null ? 0 : p.weapon.ordinal();
        for (int i = 1; i <= all.length; i++) {
            WeaponType wt = all[(idx + i) % all.length];
            if (!p.hasWeapon(wt)) continue;
            Balance.WeaponStats ws = Balance.weapon(wt);
            if (ws.maxAmmo > 0 && p.ammo[wt.ordinal()] <= 0) continue;
            p.weapon = wt;
            p.attackCd = Math.max(p.attackCd, 0.12f);
            s.effects.text(s.world, wt.label, p.x, p.y - 46, Palette.GOLD);
            if (g != null) g.audio.play(Sfx.UI_CLICK);
            return;
        }
        // 全都空？切回拳头
        p.weapon = WeaponType.FIST;
        if (g != null) g.audio.play(Sfx.UI_BACK);
    }

    /** 使用当前道具。 */
    void useItem(GameScene s, Game g) {
        Player p = s.world.player;
        if (p == null || p.item == null || p.itemCount <= 0) {
            s.showToast("没有可用道具", 1.2f);
            return;
        }
        switch (p.item) {
            case PAINKILLER:
                p.noDamageTimer = Balance.PAINKILLER_TIME;
                s.showToast("止痛药：" + (int) Balance.PAINKILLER_TIME + " 秒内免疫伤害", 2.2f);
                break;
            case STEROID:
                p.damageBoostTimer = Balance.STEROID_TIME;
                s.showToast("类固醇：近战伤害 ×" + (int) Balance.STEROID_MELEE_MUL, 2.2f);
                break;
            case ENERGY_DRINK:
                s.energyTimer = Balance.ENERGY_TIME;
                p.stamina = p.maxStamina;
                s.showToast("能量饮料：体力恢复加速", 2.2f);
                break;
            case EPO:
                p.speedBoostTimer = Balance.EPO_TIME;
                s.pendingEpSlow = Balance.EPO_TIME;
                s.showToast("红细胞生成素：移动加速！", 2.2f);
                break;
            case INVISIBILITY:
                p.invisTimer = Balance.INVIS_TIME;
                s.showToast("隐形药：敌人暂时看不到你", 2.2f);
                break;
            case MEDKIT_SMALL:
            case MEDKIT:
            case MEDKIT_LARGE: {
                float heal = p.item == com.crazyclinic.core.world.ItemType.MEDKIT_SMALL ? Balance.HEAL_SMALL
                        : (p.item == com.crazyclinic.core.world.ItemType.MEDKIT ? Balance.HEAL_MEDIUM : Balance.HEAL_LARGE);
                float before = p.hp;
                p.hp = Math.min(p.maxHp, p.hp + heal);
                s.hud.healFlash = Math.max(s.hud.healFlash, 0.8f);
                s.effects.text(s.world, "+" + (int) (p.hp - before), p.x, p.y - 40, Palette.GREEN);
                break;
            }
            default:
                break;
        }
        if (g != null) g.audio.play(Sfx.PICKUP);
        s.effects.pickupFlash(s.world, p.x, p.y, Palette.GOLD);
        p.itemCount--;
        if (p.itemCount <= 0) {
            p.item = null;
            p.itemCount = 0;
        }
    }

    /** 拾取时同步 HUD 上的摇杆状态（供内部用，避免未使用警告）。 */
    static boolean stickActive(HudState hud) {
        return hud != null && hud.moveStickActive;
    }

    static float stickRadius() {
        return Controls.STICK_RADIUS;
    }
}
