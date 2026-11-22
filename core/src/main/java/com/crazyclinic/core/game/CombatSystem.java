package com.crazyclinic.core.game;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Balance;
import com.crazyclinic.core.world.Enemy;
import com.crazyclinic.core.world.EnemyState;
import com.crazyclinic.core.world.EnemyType;
import com.crazyclinic.core.world.ItemType;
import com.crazyclinic.core.world.Organ;
import com.crazyclinic.core.world.Pickup;
import com.crazyclinic.core.world.Player;
import com.crazyclinic.core.world.Projectile;
import com.crazyclinic.core.world.ProjectileType;
import com.crazyclinic.core.world.WeaponType;
import com.crazyclinic.core.world.World;

import java.util.ArrayList;

/**
 * 战斗系统：9 种武器的差异、弹道/近战扇形/爆炸/毒云/燃烧、伤害与死亡掉落。
 *
 * <p>所有数值取自 {@link Balance}，不在这里硬编码平衡值。</p>
 */
final class CombatSystem {
    private static final float PLAYER_INVULN = 0.55f;
    private static final float MELEE_KNOCKBACK = 12f;
    private static final float MAX_FLAME_DPS = 30f;
    private static final float BUNSEN_FUEL_PER_AMMO = 0.55f;

    private final ArrayList<Burn> burns = new ArrayList<>();
    private final ArrayList<Cloud> clouds = new ArrayList<>();

    private float flameFuelTimer;
    private float poisonTick;

    static final class Burn {
        Enemy e;
        float time, dps;
    }

    static final class Cloud {
        float x, y, radius, time, dps;
        boolean poison;
        float particleTimer;
    }

    void reset() {
        burns.clear();
        clouds.clear();
        flameFuelTimer = 0f;
        poisonTick = 0f;
    }

    // ==================================================================
    // 每帧更新
    // ==================================================================

    void update(GameScene s, Game g, float dt) {
        updateBurns(s, g, dt);
        updateClouds(s, g, dt);
        updateProjectiles(s, g, dt);
    }

    private void updateBurns(GameScene s, Game g, float dt) {
        for (int i = burns.size() - 1; i >= 0; i--) {
            Burn b = burns.get(i);
            if (b.e == null || b.e.isDead()) {
                removeAt(burns, i);
                continue;
            }
            b.time -= dt;
            if (b.time <= 0f) {
                removeAt(burns, i);
                continue;
            }
            b.e.hp -= b.dps * dt;
            if (b.e.hp <= 0f) killEnemy(s, g, b.e);
        }
    }

    private void updateClouds(GameScene s, Game g, float dt) {
        World w = s.world;
        Player p = w.player;
        for (int i = clouds.size() - 1; i >= 0; i--) {
            Cloud c = clouds.get(i);
            c.time -= dt;
            if (c.time <= 0f) {
                removeAt(clouds, i);
                continue;
            }
            float r2 = c.radius * c.radius;
            for (int k = 0; k < w.enemies.size(); k++) {
                Enemy e = w.enemies.get(k);
                if (e.isDead()) continue;
                if (MathUtil.dist2(c.x, c.y, e.x, e.y) > r2) continue;
                e.hp -= c.dps * dt;
                if (MathUtil.chance(dt * 2.2f)) e.hurtFlash = 0.08f;
                if (e.hp <= 0f) killEnemy(s, g, e);
            }
            if (p != null && !p.dead && !p.isInvulnerable()
                    && MathUtil.dist2(c.x, c.y, p.x, p.y) <= r2) {
                float mult = poisonResist(g);
                p.hp -= c.dps * dt * mult;
                if (MathUtil.chance(dt * 3f)) {
                    s.hud.damageFlash = Math.max(s.hud.damageFlash, 0.35f);
                }
                if (p.hp <= 0f) s.beginDeath(g);
            }
            c.particleTimer -= dt;
            if (c.particleTimer <= 0f) {
                c.particleTimer = 0.17f;
                s.effects.poison(w, c.x, c.y, 2);
            }
        }
    }

    private void updateProjectiles(GameScene s, Game g, float dt) {
        World w = s.world;
        Player p = w.player;
        for (int i = w.projectiles.size() - 1; i >= 0; i--) {
            Projectile pr = w.projectiles.get(i);
            pr.age += dt;
            pr.x += pr.vx * dt;
            pr.y += pr.vy * dt;
            pr.life -= dt;

            boolean wall = w.map.solidAt(pr.x, pr.y);
            if (pr.life <= 0f || wall) {
                onProjectileEnd(s, g, pr, wall);
                removeAt(w.projectiles, i);
                continue;
            }

            if (pr.fromPlayer) {
                if (pr.explosive) {
                    // 爆炸试管：命中第一个敌人 / 墙立即爆炸
                    Enemy e = w.enemyAt(pr.x, pr.y, pr.radius + 26f);
                    if (e != null && !e.isDead()) {
                        explode(s, g, pr.x, pr.y, 132f, 90f, 40f, pr.poison, false);
                        removeAt(w.projectiles, i);
                    }
                    continue;
                }
                Enemy e = w.enemyAt(pr.x, pr.y, pr.radius + 26f);
                if (e != null && !e.isDead()) {
                    hitEnemy(s, g, e, pr);
                    if (pr.type == ProjectileType.SCALPEL) recoverScalpel(s, pr.x, pr.y);
                    removeAt(w.projectiles, i);
                }
            } else {
                // 敌方弹道 vs 玩家
                float rr = p.radius + pr.radius;
                if (!p.dead && MathUtil.dist2(pr.x, pr.y, p.x, p.y) <= rr * rr) {
                    if (pr.explosive) {
                        explode(s, g, pr.x, pr.y, 110f, 40f, 18f, pr.poison, true);
                    } else {
                        damagePlayer(s, g, pr.damage, pr.x, pr.y, false);
                        s.effects.impact(w, pr.x, pr.y, MathUtil.angle(pr.x, pr.y, pr.x - pr.vx, pr.y - pr.vy));
                    }
                    removeAt(w.projectiles, i);
                }
            }
        }
    }

    private void onProjectileEnd(GameScene s, Game g, Projectile pr, boolean wall) {
        World w = s.world;
        if (pr.explosive) {
            explode(s, g, pr.x, pr.y, 132f, 90f, 40f, pr.poison, !pr.fromPlayer);
            return;
        }
        if (pr.fromPlayer && pr.type == ProjectileType.SCALPEL) {
            recoverScalpel(s, pr.x, pr.y);
            s.effects.impact(w, pr.x, pr.y, pr.ownerAngle + MathUtil.PI);
            return;
        }
        if (wall) {
            s.effects.impact(w, pr.x, pr.y, pr.ownerAngle + MathUtil.PI);
            if (MathUtil.chance(0.5f)) s.effects.decal(w, pr.x, pr.y, 1, Palette.BLACK, 0.45f);
        }
    }

    private void hitEnemy(GameScene s, Game g, Enemy e, Projectile pr) {
        World w = s.world;
        float ang = MathUtil.angle(pr.x, pr.y, e.x, e.y);
        switch (pr.type) {
            case TASER_DART:
                s.effects.electric(w, e.x, e.y, 10);
                if (e.turret) {
                    // 电系克制：哨兵枪直接瘫痪
                    s.effects.text(w, "瘫痪！", e.x, e.y - 40, Palette.CYAN);
                    killEnemy(s, g, e);
                } else {
                    e.stunTimer = 2.5f;
                    e.state = EnemyState.HURT;
                    e.stateTimer = 2.5f;
                    s.effects.text(w, "麻痹！", e.x, e.y - 40, Palette.CYAN);
                    damageEnemy(s, g, e, pr.damage, ang, true, 0f, false);
                }
                if (g != null) g.audio.play(Sfx.TASER);
                break;
            case SCALPEL:
                damageEnemy(s, g, e, pr.damage, ang, true, 2f, true);
                s.effects.blood(w, e.x, e.y, ang, 3);
                break;
            case PELLET:
                damageEnemy(s, g, e, pr.damage, ang, true, 3f, false);
                s.effects.blood(w, e.x, e.y, ang, 3);
                break;
            default:
                damageEnemy(s, g, e, pr.damage, ang, true, 7f, true);
                s.effects.blood(w, e.x, e.y, ang, 5);
                s.effects.sparks(w, pr.x, pr.y, ang, 3);
                break;
        }
        if (g != null) g.audio.play(Sfx.HIT_FLESH);
    }

    // ==================================================================
    // 玩家攻击
    // ==================================================================

    void playerAttack(GameScene s, Game g) {
        Player p = s.world.player;
        if (p == null || p.dead || s.isFinished()) return;
        if (p.attackCd > 0f || p.dodgeTimer > 0f) return;
        WeaponType wt = p.weapon;
        if (wt == null) return;
        Balance.WeaponStats ws = Balance.weapon(wt);
        if (wt == WeaponType.BUNSEN) {
            flameAttack(s, g, p, ws);
            return;
        }
        int idx = wt.ordinal();
        int ammo = p.ammo[idx];
        if (ws.maxAmmo >= 0 && ammo <= 0) {
            p.attackCd = 0.28f;
            s.effects.text(s.world, "没弹药", p.x, p.y - 42, Palette.TEXT_DIM);
            if (g != null) g.audio.play(Sfx.UI_CLICK);
            return;
        }
        if (wt == WeaponType.DEFIBRILLATOR) {
            defibAttack(s, g, p, ws);
            return;
        }
        if (ws.melee) {
            meleeAttack(s, g, p, ws);
            return;
        }
        rangedAttack(s, g, p, ws);
    }

    /** 斧头 / 拳头：扇形近战。 */
    private void meleeAttack(GameScene s, Game g, Player p, Balance.WeaponStats ws) {
        World w = s.world;
        p.attackCd = Math.max(0.05f, ws.cooldown);
        p.muzzleFlash = 0.08f;
        float mul = meleeMul(p);
        float range = ws.range;
        float halfArc = (float) Math.toRadians(Math.max(30f, ws.arcDeg) * 0.5f);
        boolean hit = false;
        int hits = 0;
        for (int i = 0; i < w.enemies.size(); i++) {
            Enemy e = w.enemies.get(i);
            if (e.isDead()) continue;
            float d = MathUtil.dist(p.x, p.y, e.x, e.y);
            if (d > range + e.radius) continue;
            float a = MathUtil.angle(p.x, p.y, e.x, e.y);
            if (Math.abs(MathUtil.angleDelta(p.aimAngle, a)) > halfArc) continue;
            if (!w.lineOfSight(p.x, p.y, e.x, e.y)) continue;
            float dmg = ws.damage * mul;
            if (p.weapon == WeaponType.FIST) dmg *= 1f;
            damageEnemy(s, g, e, dmg, a, true, MELEE_KNOCKBACK, true);
            hit = true;
            hits++;
            s.effects.blood(w, e.x, e.y, a, 5);
            s.effects.sparks(w, e.x, e.y, a, 2);
            if (hits >= 4) break;
        }
        // 挥砍弧光
        for (int i = 0; i < 4 && w.particles.size() < EffectsSystem.MAX_PARTICLES; i++) {
            float a = p.aimAngle + MathUtil.rand(-halfArc, halfArc);
            float d = MathUtil.rand(range * 0.4f, range);
            w.addParticle(p.x + (float) Math.cos(a) * d, p.y + (float) Math.sin(a) * d,
                    (float) Math.cos(a) * 90f, (float) Math.sin(a) * 90f,
                    0.14f, 4f, Palette.WHITE, 0);
        }
        if (g != null) g.audio.play(Sfx.SWING);
        if (hit) {
            if (g != null) g.audio.play(Sfx.HIT_FLESH);
            w.shake = Math.max(w.shake, 0.12f);
        }
        noiseQuiet(s, p.x, p.y, 120f);
    }

    /** 除颤器：近战击晕 + EMP 秒哨兵。 */
    private void defibAttack(GameScene s, Game g, Player p, Balance.WeaponStats ws) {
        World w = s.world;
        p.attackCd = Math.max(0.2f, ws.cooldown);
        p.ammo[p.weapon.ordinal()] = Math.max(0, p.ammo[p.weapon.ordinal()] - 1);
        p.muzzleFlash = 0.12f;
        s.effects.electric(w, p.x, p.y, 14);
        w.shake = Math.max(w.shake, 0.2f);
        float halfArc = (float) Math.toRadians(Math.max(30f, ws.arcDeg) * 0.5f);
        for (int i = 0; i < w.enemies.size(); i++) {
            Enemy e = w.enemies.get(i);
            if (e.isDead()) continue;
            float d = MathUtil.dist(p.x, p.y, e.x, e.y);
            if (d > ws.range + e.radius) continue;
            float a = MathUtil.angle(p.x, p.y, e.x, e.y);
            if (Math.abs(MathUtil.angleDelta(p.aimAngle, a)) > halfArc) continue;
            if (!w.lineOfSight(p.x, p.y, e.x, e.y)) continue;
            s.effects.electric(w, e.x, e.y, 8);
            if (e.turret) {
                s.effects.text(w, "EMP 瘫痪！", e.x, e.y - 40, Palette.CYAN);
                killEnemy(s, g, e);
            } else if (e.type == EnemyType.BOSS) {
                damageEnemy(s, g, e, ws.damage, a, true, 0f, false);
            } else {
                e.stunTimer = Math.max(e.stunTimer, 1.5f);
                e.state = EnemyState.HURT;
                e.stateTimer = 1.5f;
                damageEnemy(s, g, e, ws.damage, a, true, 6f, false);
            }
        }
        // EMP 范围：附近的哨兵枪也瘫痪
        for (int i = 0; i < w.enemies.size(); i++) {
            Enemy e = w.enemies.get(i);
            if (e.isDead() || !e.turret) continue;
            if (MathUtil.dist(p.x, p.y, e.x, e.y) < 190f) {
                s.effects.electric(w, e.x, e.y, 6);
                killEnemy(s, g, e);
            }
        }
        if (g != null) g.audio.play(Sfx.DEFIB);
        noise(s, p.x, p.y, 260f);
    }

    /** 本生灯：持续喷火，点燃目标。 */
    private void flameAttack(GameScene s, Game g, Player p, Balance.WeaponStats ws) {
        World w = s.world;
        int idx = p.weapon.ordinal();
        if (p.ammo[idx] <= 0) {
            p.attackCd = 0.3f;
            s.effects.text(w, "没燃料", p.x, p.y - 42, Palette.TEXT_DIM);
            if (g != null) g.audio.play(Sfx.UI_CLICK);
            return;
        }
        p.attackCd = Math.max(0.05f, ws.cooldown);
        p.muzzleFlash = 0.05f;
        // 燃料：每约 0.55s 消耗 1 点
        flameFuelTimer += ws.cooldown;
        if (flameFuelTimer >= BUNSEN_FUEL_PER_AMMO) {
            flameFuelTimer -= BUNSEN_FUEL_PER_AMMO;
            p.ammo[idx] = Math.max(0, p.ammo[idx] - 1);
        }
        float range = ws.range;
        float halfArc = (float) Math.toRadians(46f) * 0.5f;
        float tickDamage = Math.min(MAX_FLAME_DPS, ws.damage) * ws.cooldown;
        for (int i = 0; i < w.enemies.size(); i++) {
            Enemy e = w.enemies.get(i);
            if (e.isDead()) continue;
            float d = MathUtil.dist(p.x, p.y, e.x, e.y);
            if (d > range + e.radius) continue;
            float a = MathUtil.angle(p.x, p.y, e.x, e.y);
            if (Math.abs(MathUtil.angleDelta(p.aimAngle, a)) > halfArc) continue;
            if (!w.lineOfSight(p.x, p.y, e.x, e.y)) continue;
            damageEnemy(s, g, e, tickDamage, a, false, 0f, false);
            addBurn(e, 1.2f, 11f);
        }
        s.effects.flame(w, p, p.aimAngle, range);
        if (MathUtil.chance(0.25f) && g != null) g.audio.play(Sfx.FLAME);
        noiseQuiet(s, p.x, p.y, 150f);
    }

    /** 远程武器：手枪/霰弹/泰瑟/手术刀/试管。 */
    private void rangedAttack(GameScene s, Game g, Player p, Balance.WeaponStats ws) {
        World w = s.world;
        int idx = p.weapon.ordinal();
        p.attackCd = Math.max(0.05f, ws.cooldown);
        boolean infinite = ws.maxAmmo < 0;
        if (!infinite) p.ammo[idx] = Math.max(0, p.ammo[idx] - 1);
        int pellets = Math.max(1, ws.pellets);
        float spread = (float) Math.toRadians(Math.max(0f, ws.spreadDeg));
        float speed = Math.max(120f, ws.projectileSpeed);
        ProjectileType pt = projectileTypeOf(p.weapon);
        boolean vial = p.weapon == WeaponType.VIAL;
        boolean poison = vial && MathUtil.chance(0.5f);
        for (int i = 0; i < pellets; i++) {
            if (w.projectiles.size() >= 220) break;
            float a = p.aimAngle + (pellets > 1 ? MathUtil.rand(-spread * 0.5f, spread * 0.5f) : MathUtil.rand(-spread, spread) * 0.5f);
            Projectile pr = new Projectile();
            pr.type = pt;
            pr.x = p.x + (float) Math.cos(a) * (p.radius + 8f);
            pr.y = p.y + (float) Math.sin(a) * (p.radius + 8f);
            pr.vx = (float) Math.cos(a) * speed;
            pr.vy = (float) Math.sin(a) * speed;
            pr.life = Math.max(0.12f, ws.range / speed);
            pr.damage = ws.damage;
            pr.fromPlayer = true;
            pr.ownerAngle = a;
            pr.explosive = vial;
            pr.poison = poison;
            pr.radius = vial ? 8f : (pt == ProjectileType.SCALPEL ? 6f : (pt == ProjectileType.PELLET ? 4f : 5f));
            pr.spin = MathUtil.rand(-8f, 8f);
            w.projectiles.add(pr);
        }
        p.muzzleFlash = 0.1f;
        switch (p.weapon) {
            case SHOTGUN:
                s.effects.muzzle(w, p, p.aimAngle, true);
                s.effects.shell(w, p, p.aimAngle);
                w.shake = Math.max(w.shake, 0.4f);
                if (g != null) g.audio.play(Sfx.SHOTGUN);
                noise(s, p.x, p.y, 780f);
                break;
            case PISTOL:
                s.effects.muzzle(w, p, p.aimAngle, false);
                s.effects.shell(w, p, p.aimAngle);
                w.shake = Math.max(w.shake, 0.22f);
                if (g != null) g.audio.play(Sfx.PISTOL);
                noise(s, p.x, p.y, 640f);
                break;
            case TASER:
                s.effects.electric(w, p.x, p.y, 5);
                w.shake = Math.max(w.shake, 0.12f);
                if (g != null) g.audio.play(Sfx.TASER);
                noise(s, p.x, p.y, 320f);
                break;
            case SCALPEL:
                s.effects.muzzle(w, p, p.aimAngle, false);
                if (g != null) g.audio.play(Sfx.THROW);
                noiseQuiet(s, p.x, p.y, 200f);
                break;
            case VIAL:
                if (g != null) g.audio.play(Sfx.THROW);
                noiseQuiet(s, p.x, p.y, 180f);
                break;
            default:
                if (g != null) g.audio.play(Sfx.PISTOL);
                noise(s, p.x, p.y, 500f);
                break;
        }
    }

    private static ProjectileType projectileTypeOf(WeaponType wt) {
        switch (wt) {
            case SHOTGUN: return ProjectileType.PELLET;
            case TASER: return ProjectileType.TASER_DART;
            case SCALPEL: return ProjectileType.SCALPEL;
            case VIAL: return ProjectileType.VIAL;
            default: return ProjectileType.BULLET;
        }
    }

    static float meleeMul(Player p) {
        if (p == null) return 1f;
        float m = p.meleeMultiplier();
        if (p.damageBoostTimer > 0f) m *= Balance.STEROID_MELEE_MUL;
        return m;
    }

    // ==================================================================
    // 敌方弹道 & 爆炸
    // ==================================================================

    Projectile enemyShot(GameScene s, Enemy e, ProjectileType type, float angle, float speed, float damage,
                         boolean explosive, boolean poison) {
        if (s.world.projectiles.size() >= 220) return null;
        Projectile pr = new Projectile();
        pr.type = type;
        pr.x = e.x + (float) Math.cos(angle) * (e.radius + 12f);
        pr.y = e.y + (float) Math.sin(angle) * (e.radius + 12f);
        pr.vx = (float) Math.cos(angle) * speed;
        pr.vy = (float) Math.sin(angle) * speed;
        float rangeGuess = e.attackRange > 0f ? e.attackRange : 400f;
        pr.life = Math.max(0.35f, rangeGuess / speed * 1.15f);
        pr.damage = damage;
        pr.fromPlayer = false;
        pr.ownerAngle = angle;
        pr.explosive = explosive;
        pr.poison = poison;
        pr.radius = type == ProjectileType.VIAL ? 8f : (type == ProjectileType.WRENCH ? 9f : 5f);
        pr.spin = MathUtil.rand(-10f, 10f);
        s.world.projectiles.add(pr);
        return pr;
    }

    void enemySpread(GameScene s, Enemy e, float baseAngle, int pellets, float spreadDeg, float speed, float damage) {
        float spread = (float) Math.toRadians(Math.max(0f, spreadDeg));
        for (int i = 0; i < Math.max(1, pellets); i++) {
            float a = baseAngle + MathUtil.rand(-spread * 0.5f, spread * 0.5f);
            enemyShot(s, e, ProjectileType.PELLET, a, speed, damage, false, false);
        }
    }

    /** 爆炸：范围伤害 + 击退 + 毒云（50% 由调用者决定）。 */
    void explode(GameScene s, Game g, float x, float y, float radius, float centerDamage,
                 float edgeDamage, boolean poison, boolean hurtPlayer) {
        World w = s.world;
        for (int i = 0; i < w.enemies.size(); i++) {
            Enemy e = w.enemies.get(i);
            if (e.isDead()) continue;
            float d = MathUtil.dist(x, y, e.x, e.y);
            if (d > radius + e.radius) continue;
            float t = MathUtil.clamp(d / radius, 0f, 1f);
            float dmg = centerDamage * (1f - t) + edgeDamage * t;
            float ang = MathUtil.angle(x, y, e.x, e.y);
            damageEnemy(s, g, e, dmg, ang, true, 26f * (1f - t), true);
        }
        Player p = w.player;
        if (hurtPlayer && p != null && !p.dead) {
            float d = MathUtil.dist(x, y, p.x, p.y);
            if (d < radius) {
                float t = MathUtil.clamp(d / radius, 0f, 1f);
                damagePlayer(s, g, edgeDamage * (1f - t) + 4f, x, y, false);
                Physics.move(w, p, (p.x - x) * 0.25f, (p.y - y) * 0.25f);
            }
        }
        s.effects.explosion(w, x, y);
        if (poison) {
            addCloud(x, y, radius * 0.85f, 4.5f, 25f, true);
            s.effects.poison(w, x, y, 10);
        }
        w.shake = Math.max(w.shake, 0.55f);
        noise(s, x, y, 900f);
        if (g != null) {
            g.audio.play(Sfx.IMPACT);
            if (g.save != null && g.save.vibrateOn) g.audio.vibrate(35);
        }
    }

    private void addCloud(float x, float y, float radius, float time, float dps, boolean poison) {
        Cloud c = new Cloud();
        c.x = x;
        c.y = y;
        c.radius = radius;
        c.time = time;
        c.dps = dps;
        c.poison = poison;
        clouds.add(c);
    }

    private void addBurn(Enemy e, float time, float dps) {
        for (int i = 0; i < burns.size(); i++) {
            Burn b = burns.get(i);
            if (b.e == e) {
                b.time = Math.max(b.time, time);
                b.dps = Math.max(b.dps, dps);
                return;
            }
        }
        Burn b = new Burn();
        b.e = e;
        b.time = time;
        b.dps = dps;
        burns.add(b);
    }

    private void recoverScalpel(GameScene s, float x, float y) {
        if (s.world.pickups.size() > 120) return;
        Pickup pk = Pickup.weapon(WeaponType.SCALPEL, x, y);
        pk.bob = MathUtil.rand(0f, MathUtil.TAU);
        s.world.pickups.add(pk);
    }

    // ==================================================================
    // 伤害 / 死亡
    // ==================================================================

    void damageEnemy(GameScene s, Game g, Enemy e, float dmg, float dirAngle,
                     boolean blood, float knockback, boolean stagger) {
        if (e == null || e.isDead() || dmg <= 0f) return;
        e.hp -= dmg;
        e.hurtFlash = Math.max(e.hurtFlash, 0.16f);
        if (knockback > 0f) {
            float ang = dirAngle;
            Physics.push(s.world, e, (float) Math.cos(ang) * knockback, (float) Math.sin(ang) * knockback);
        }
        if (stagger && e.type != EnemyType.BOSS && e.stunTimer <= 0f) {
            e.state = EnemyState.HURT;
            e.stateTimer = 0.16f;
        }
        if (blood) s.effects.blood(s.world, e.x, e.y, dirAngle, 4);
        if (MathUtil.chance(0.5f) && dmg >= 8f) {
            s.effects.text(s.world, String.valueOf((int) dmg), e.x + MathUtil.rand(-8f, 8f),
                    e.y - e.radius - 14f, Palette.TEXT);
        }
        s.enemyAI.onEnemyHurt(s, e);
        if (e.hp <= 0f) killEnemy(s, g, e);
    }

    void damagePlayer(GameScene s, Game g, float dmg, float srcX, float srcY, boolean poison) {
        Player p = s.world.player;
        if (p == null || p.dead || p.hp <= 0f || s.isFinished()) return;
        World w = s.world;
        float realDmg = dmg;
        if (!poison) {
            if (p.isInvulnerable()) return;
            p.invuln = Math.max(p.invuln, PLAYER_INVULN);
        } else {
            realDmg *= poisonResist(g);
        }
        p.hp -= realDmg;
        p.hurtFlash = 1f;
        s.hud.damageFlash = Math.max(s.hud.damageFlash, poison ? 0.45f : 0.85f);
        float ang = MathUtil.angle(srcX, srcY, p.x, p.y);
        s.effects.blood(w, p.x, p.y, ang, 6);
        s.effects.text(w, "-" + (int) Math.max(1f, realDmg), p.x, p.y - 46, Palette.RED);
        w.shake = Math.max(w.shake, 0.32f);
        if (g != null) {
            g.audio.play(Sfx.PLAYER_HURT);
            if (g.save != null && g.save.vibrateOn) g.audio.vibrate(45);
        }
        if (p.hp <= 0f) {
            p.hp = 0f;
            s.beginDeath(g);
        }
    }

    private static float poisonResist(Game g) {
        if (g == null || g.save == null) return 1f;
        return g.save.hasOrgan(Organ.KIDNEY) ? (1f - Balance.ORGAN_KIDNEY_POISON_RESIST) : 1f;
    }

    void killEnemy(GameScene s, Game g, Enemy e) {
        if (e == null || e.isDead()) return;
        World w = s.world;
        e.dead = true;
        e.state = EnemyState.DEAD;
        e.hp = 0f;
        e.stunTimer = 0f;
        e.reviveTimer = 0f;
        e.deathTimer = 30f;
        e.bodyTimer = 25f;
        w.kills++;
        s.effects.blood(w, e.x, e.y, MathUtil.rand(0f, MathUtil.TAU), 14);
        s.effects.bloodPool(w, e.x, e.y);
        s.effects.smoke(w, e.x, e.y, 2, 0.6f);
        w.shake = Math.max(w.shake, 0.14f);
        if (g != null) g.audio.play(Sfx.ENEMY_DIE);
        if (e.type == EnemyType.BOSS) {
            onBossKilled(s, g, e);
            return;
        }
        // 10% 掉落药/弹药
        if (MathUtil.chance(0.10f)) {
            if (MathUtil.chance(0.5f)) {
                Pickup pk = Pickup.item(ItemType.MEDKIT_SMALL, 15, e.x, e.y);
                pk.bob = MathUtil.rand(0f, MathUtil.TAU);
                w.pickups.add(pk);
            } else {
                int amount = MathUtil.randInt(4, 8);
                Pickup pk = Pickup.item(ItemType.AMMO, amount, e.x, e.y);
                pk.bob = MathUtil.rand(0f, MathUtil.TAU);
                w.pickups.add(pk);
            }
        }
    }

    private void onBossKilled(GameScene s, Game g, Enemy boss) {
        World w = s.world;
        s.effects.explosion(w, boss.x, boss.y);
        s.effects.text(w, "赞恩倒下了……", boss.x, boss.y - 80, Palette.GOLD);
        w.objective = com.crazyclinic.core.content.Texts.OBJECTIVE_ELEVATOR;
        w.shake = Math.max(w.shake, 0.9f);
        s.showToast("赞恩倒下了！电梯已可启动", 4f);
        // BOSS 掉落：大药包 + 弹药 + 钥匙卡
        Pickup med = Pickup.item(ItemType.MEDKIT_LARGE, 60, boss.x + 40f, boss.y);
        med.bob = MathUtil.rand(0f, MathUtil.TAU);
        w.pickups.add(med);
        Pickup ammo = Pickup.item(ItemType.AMMO, 12, boss.x - 40f, boss.y);
        ammo.bob = MathUtil.rand(0f, MathUtil.TAU);
        w.pickups.add(ammo);
        if (g != null) {
            g.audio.play(Sfx.ORGAN_UPGRADE);
            if (g.save != null && g.save.vibrateOn) g.audio.vibrate(120);
        }
    }

    // ==================================================================
    // 噪音
    // ==================================================================

    void noise(GameScene s, float x, float y, float radius) {
        if (s == null || s.enemyAI == null) return;
        s.enemyAI.noise(s.world, x, y, radius);
    }

    /** 近战/火焰这类只有极小范围的声音：不惊动远处，但会小幅度提高警戒。 */
    private void noiseQuiet(GameScene s, float x, float y, float radius) {
        if (s == null || s.enemyAI == null) return;
        s.enemyAI.noise(s.world, x, y, radius);
    }

    private static <T> void removeAt(java.util.List<T> list, int index) {
        int last = list.size() - 1;
        if (index != last) list.set(index, list.get(last));
        list.remove(last);
    }

    // 供 GameScene HUD 使用
    int activeCloudCount() {
        return clouds.size();
    }

    int activeBurnCount() {
        return burns.size();
    }
}
