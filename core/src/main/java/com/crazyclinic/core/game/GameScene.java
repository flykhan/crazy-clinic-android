package com.crazyclinic.core.game;

import com.crazyclinic.core.Controls;
import com.crazyclinic.core.Game;
import com.crazyclinic.core.HudState;
import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Music;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.Scene;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Balance;
import com.crazyclinic.core.content.Levels;
import com.crazyclinic.core.content.Texts;
import com.crazyclinic.core.render.HudRenderer;
import com.crazyclinic.core.render.WorldRenderer;
import com.crazyclinic.core.world.Enemy;
import com.crazyclinic.core.world.LevelDef;
import com.crazyclinic.core.world.Organ;
import com.crazyclinic.core.world.Pickup;
import com.crazyclinic.core.world.Player;
import com.crazyclinic.core.world.Projectile;
import com.crazyclinic.core.world.ProjectileType;
import com.crazyclinic.core.world.TileMap;
import com.crazyclinic.core.world.TileType;
import com.crazyclinic.core.world.WeaponType;
import com.crazyclinic.core.world.World;

/**
 * 关卡主场景（占位实现：移动/瞄准/近战/拾取/交互/电梯过关，敌人暂不行动）。
 * 引擎代理在此基础上实现完整 AI、战斗、骇客小游戏、动画与关卡流程。
 */
public final class GameScene implements Scene {
    public final int floor;
    public final World world;
    public final HudState hud = new HudState();

    protected final WorldRenderer worldRenderer = new WorldRenderer();
    protected final HudRenderer hudRenderer = new HudRenderer();

    protected float elapsed;
    protected boolean finished;
    protected boolean paused;

    private int movePointer = -1;
    private int aimPointer = -1;

    public GameScene(int floor) {
        this.floor = floor;
        LevelDef def = Levels.get(floor);
        this.world = new World(def, null);
    }

    /** 供桌面预览/测试注入存档。 */
    public GameScene(int floor, com.crazyclinic.core.SaveData save) {
        this.floor = floor;
        LevelDef def = Levels.get(floor);
        this.world = new World(def, save);
    }

    @Override
    public void enter(Game game) {
        hud.reset();
        game.audio.music(Music.FLOOR);
        updateObjective();
        game.runKills = 0;
        game.runTime = 0f;
    }

    protected void updateObjective() {
        if (world.level.bossLevel) world.objective = Texts.OBJECTIVE_BOSS;
        else if (!world.camerasDisabled) world.objective = Texts.OBJECTIVE_HACK;
        else world.objective = Texts.OBJECTIVE_ELEVATOR;
    }

    @Override
    public void update(Game game, float dt) {
        if (paused) return;
        elapsed += dt;
        game.runTime = elapsed;
        world.time += dt;
        Player p = world.player;

        // ---- 移动 ----
        float mx = hud.moveStickDx, my = hud.moveStickDy;
        float mag = (float) Math.sqrt(mx * mx + my * my);
        p.moving = mag > 0.15f;
        if (p.moving) {
            if (mag > 1f) {
                mx /= mag;
                my /= mag;
                mag = 1f;
            }
            boolean wantSprint = mag > 0.85f && p.stamina > 1f;
            p.sprinting = wantSprint;
            float sp = world.playerSpeedNow();
            if (wantSprint) {
                p.stamina = Math.max(0f, p.stamina - Balance.SPRINT_DRAIN * dt);
                p.staminaRegenDelay = Balance.STAMINA_REGEN_DELAY;
            }
            moveCircle(p, mx * sp * dt, my * sp * dt);
            p.angle = (float) Math.atan2(my, mx);
        } else {
            p.sprinting = false;
        }
        if (!p.sprinting) {
            if (p.staminaRegenDelay > 0f) p.staminaRegenDelay -= dt;
            else p.stamina = Math.min(p.maxStamina, p.stamina + Balance.STAMINA_REGEN * dt);
        }

        // ---- 瞄准 ----
        float amag = (float) Math.sqrt(hud.aimStickDx * hud.aimStickDx + hud.aimStickDy * hud.aimStickDy);
        if (hud.aimStickActive && amag > 0.2f) {
            p.aimAngle = (float) Math.atan2(hud.aimStickDy, hud.aimStickDx);
            p.angle = p.aimAngle;
            if (amag > 0.55f) tryAttack(game);
        }
        if (hud.attackHeld) tryAttack(game);

        // ---- 计时器 ----
        p.attackCd = Math.max(0f, p.attackCd - dt);
        p.invuln = Math.max(0f, p.invuln - dt);
        p.dodgeTimer = Math.max(0f, p.dodgeTimer - dt);
        p.dodgeCd = Math.max(0f, p.dodgeCd - dt);
        p.muzzleFlash = Math.max(0f, p.muzzleFlash - dt);
        p.hurtFlash = Math.max(0f, p.hurtFlash - dt * 2f);
        p.speedBoostTimer = Math.max(0f, p.speedBoostTimer - dt);
        p.epoSlowTimer = Math.max(0f, p.epoSlowTimer - dt);
        p.damageBoostTimer = Math.max(0f, p.damageBoostTimer - dt);
        p.noDamageTimer = Math.max(0f, p.noDamageTimer - dt);
        p.invisTimer = Math.max(0f, p.invisTimer - dt);
        p.fireHold = Math.max(0f, p.fireHold - dt);
        hud.damageFlash = Math.max(0f, hud.damageFlash - dt * 2f);
        hud.lowHealth = p.hp / p.maxHp;

        // ---- 拾取 ----
        Pickup pickup = world.pickupAt(p.x, p.y, Balance.PICKUP_RADIUS);
        if (pickup != null) applyPickup(game, pickup);

        // ---- 子弹 ----
        updateProjectiles(game, dt);

        // ---- 特效寿命 ----
        updateEffects(dt);

        // ---- 相机 ----
        float targetX = p.x - World.VIEW_W / 2f;
        float targetY = p.y - World.VIEW_H / 2f;
        world.cameraX = MathUtil.approach(world.cameraX, targetX, 12f, dt);
        world.cameraY = MathUtil.approach(world.cameraY, targetY, 12f, dt);
        world.clampCamera();
        world.shake = Math.max(0f, world.shake - dt * 3f);

        updateObjective();
        updateBossHud();

        if (p.hp <= 0f && !finished) {
            finished = true;
            game.audio.play(Sfx.PLAYER_DIE);
            game.gotoResult(false, floor, world.kills, elapsed);
        }
    }

    protected void updateBossHud() {
        Enemy boss = world.boss();
        if (boss != null && !boss.isDead()) {
            hud.bossHp = boss.hp;
            hud.bossMaxHp = boss.maxHp;
            hud.bossName = boss.type.label;
        } else {
            hud.bossHp = 0f;
        }
    }

    protected void moveCircle(Player p, float dx, float dy) {
        if (dx != 0f && !world.collidesCircle(p.x + dx, p.y, p.radius)) p.x += dx;
        if (dy != 0f && !world.collidesCircle(p.x, p.y + dy, p.radius)) p.y += dy;
    }

    protected void tryAttack(Game game) {
        Player p = world.player;
        if (p.attackCd > 0f) return;
        Balance.WeaponStats ws = Balance.weapon(p.weapon);
        if (ws.melee) {
            p.attackCd = ws.cooldown;
            p.muzzleFlash = 0.08f;
            float mul = (p.armsBonus ? Balance.ORGAN_ARMS_MELEE_MUL : 1f)
                    * (p.damageBoostTimer > 0f ? Balance.STEROID_MELEE_MUL : 1f);
            boolean hit = false;
            for (Enemy e : world.enemies) {
                if (e.isDead()) continue;
                float d = MathUtil.dist(p.x, p.y, e.x, e.y);
                if (d > ws.range + e.radius) continue;
                float a = MathUtil.angle(p.x, p.y, e.x, e.y);
                float diff = Math.abs(MathUtil.angleDelta(p.aimAngle, a));
                if (diff > Math.toRadians(ws.arcDeg * 0.5f)) continue;
                e.hp -= ws.damage * mul;
                e.hurtFlash = 0.2f;
                world.addBlood(e.x, e.y, a, 6);
                hit = true;
                if (e.hp <= 0f) killEnemy(e);
            }
            game.audio.play(Sfx.SWING);
            if (hit) game.audio.play(Sfx.HIT_FLESH);
            if (p.weapon == WeaponType.BUNSEN) {
                // 火焰持续判定：由冷却控制，简化为范围伤害
            }
        } else {
            int ammo = p.ammo[p.weapon.ordinal()];
            if (ammo == 0) {
                p.attackCd = 0.3f;
                return;
            }
            p.attackCd = ws.cooldown;
            if (ammo > 0) p.ammo[p.weapon.ordinal()] = ammo - 1;
            p.muzzleFlash = 0.1f;
            for (int i = 0; i < Math.max(1, ws.pellets); i++) {
                float spread = (float) Math.toRadians(ws.spreadDeg);
                float a = p.aimAngle + MathUtil.rand(-spread * 0.5f, spread * 0.5f);
                Projectile pr = new Projectile();
                pr.type = p.weapon == WeaponType.SHOTGUN ? ProjectileType.PELLET
                        : p.weapon == WeaponType.TASER ? ProjectileType.TASER_DART
                        : p.weapon == WeaponType.SCALPEL ? ProjectileType.SCALPEL
                        : p.weapon == WeaponType.VIAL ? ProjectileType.VIAL : ProjectileType.BULLET;
                pr.x = p.x + (float) Math.cos(a) * 26f;
                pr.y = p.y + (float) Math.sin(a) * 26f;
                pr.vx = (float) Math.cos(a) * ws.projectileSpeed;
                pr.vy = (float) Math.sin(a) * ws.projectileSpeed;
                pr.life = ws.range / Math.max(1f, ws.projectileSpeed);
                pr.damage = ws.damage;
                pr.fromPlayer = true;
                pr.explosive = p.weapon == WeaponType.VIAL;
                world.projectiles.add(pr);
            }
            game.audio.play(p.weapon == WeaponType.SHOTGUN ? Sfx.SHOTGUN
                    : p.weapon == WeaponType.TASER ? Sfx.TASER
                    : p.weapon == WeaponType.SCALPEL ? Sfx.THROW
                    : p.weapon == WeaponType.VIAL ? Sfx.THROW : Sfx.PISTOL);
            if (p.weapon != WeaponType.SCALPEL) world.shake = 0.22f;
        }
    }

    protected void updateProjectiles(Game game, float dt) {
        Player p = world.player;
        for (int i = world.projectiles.size() - 1; i >= 0; i--) {
            Projectile pr = world.projectiles.get(i);
            pr.x += pr.vx * dt;
            pr.y += pr.vy * dt;
            pr.life -= dt;
            if (pr.life <= 0f || world.map.solidAt(pr.x, pr.y)) {
                world.projectiles.remove(i);
                continue;
            }
            if (pr.fromPlayer) {
                Enemy e = world.enemyAt(pr.x, pr.y, 26f);
                if (e != null) {
                    e.hp -= pr.damage;
                    e.hurtFlash = 0.2f;
                    world.addBlood(e.x, e.y, MathUtil.angle(pr.x, pr.y, e.x, e.y), 5);
                    if (e.hp <= 0f) killEnemy(e);
                    world.projectiles.remove(i);
                }
            }
        }
    }

    protected void killEnemy(Enemy e) {
        e.dead = true;
        e.state = com.crazyclinic.core.world.EnemyState.DEAD;
        e.deathTimer = 0.6f;
        world.kills++;
        world.addBlood(e.x, e.y, MathUtil.rand(0f, MathUtil.TAU), 14);
        world.addDecal(e.x, e.y, 0, Palette.BLOOD, 1.4f);
        if (com.crazyclinic.core.MathUtil.chance(0.12f)) {
            world.pickups.add(Pickup.item(com.crazyclinic.core.world.ItemType.MEDKIT_SMALL, 15, e.x, e.y));
        }
    }

    protected void applyPickup(Game game, Pickup pk) {
        if (pk.taken) return;
        pk.taken = true;
        Player p = world.player;
        switch (pk.kind) {
            case WEAPON:
                p.giveWeapon(pk.weapon);
                world.addFloatingText("获得 " + pk.weapon.label, pk.x, pk.y - 30, Palette.GOLD);
                game.audio.play(Sfx.PICKUP_WEAPON);
                break;
            case KEYCARD:
                p.hasKeycard = true;
                world.addFloatingText("获得钥匙卡", pk.x, pk.y - 30, Palette.MINT);
                game.audio.play(Sfx.KEYCARD);
                break;
            case ORGAN:
                p.applyOrganBonus(pk.organ);
                game.save.addOrgan(pk.organ);
                game.saveNow();
                world.addFloatingText("永久强化：" + pk.organ.label, pk.x, pk.y - 30, Palette.PURPLE);
                game.audio.play(Sfx.ORGAN_UPGRADE);
                break;
            default:
                useItemPickup(game, pk);
                break;
        }
    }

    protected void useItemPickup(Game game, Pickup pk) {
        Player p = world.player;
        switch (pk.item) {
            case MEDKIT_SMALL:
            case MEDKIT:
            case MEDKIT_LARGE: {
                float heal = pk.item == com.crazyclinic.core.world.ItemType.MEDKIT_SMALL ? Balance.HEAL_SMALL
                        : pk.item == com.crazyclinic.core.world.ItemType.MEDKIT ? Balance.HEAL_MEDIUM : Balance.HEAL_LARGE;
                p.hp = Math.min(p.maxHp, p.hp + heal);
                world.addFloatingText("+" + (int) heal, pk.x, pk.y - 30, Palette.GREEN);
                game.audio.play(Sfx.HEAL);
                break;
            }
            default:
                p.item = pk.item;
                p.itemCount = pk.amount;
                world.addFloatingText("拾取 " + pk.item.label, pk.x, pk.y - 30, Palette.TEXT);
                game.audio.play(Sfx.PICKUP);
                break;
        }
    }

    protected void updateEffects(float dt) {
        for (int i = world.particles.size() - 1; i >= 0; i--) {
            com.crazyclinic.core.world.Particle p = world.particles.get(i);
            p.life -= dt;
            if (p.life <= 0f) {
                world.particles.remove(i);
                continue;
            }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vx *= 0.92f;
            p.vy *= 0.92f;
        }
        for (int i = world.floatingTexts.size() - 1; i >= 0; i--) {
            com.crazyclinic.core.world.FloatingText ft = world.floatingTexts.get(i);
            ft.life -= dt;
            if (ft.life <= 0f) {
                world.floatingTexts.remove(i);
                continue;
            }
            ft.y += ft.vy * dt;
            ft.vy *= 0.96f;
        }
        for (int i = world.decals.size() - 1; i >= 0; i--) {
            com.crazyclinic.core.world.Decal d = world.decals.get(i);
            d.life -= dt;
            if (d.life <= 0f) world.decals.remove(i);
        }
    }

    // ---------------- 触摸 ----------------
    @Override
    public void touchDown(Game game, int id, float x, float y) {
        if (Controls.inWorld(y)) {
            if (Controls.hit(x, y, Controls.PAUSE_X, Controls.PAUSE_Y, Controls.PAUSE_R)) {
                paused = !paused;
                hud.paused = paused;
                game.audio.play(Sfx.UI_CLICK);
                return;
            }
            return;
        }
        if (paused) {
            paused = false;
            hud.paused = false;
            return;
        }
        if (Controls.hit(x, y, Controls.ATTACK_X, Controls.ATTACK_Y, Controls.ATTACK_R)) {
            hud.attackHeld = true;
            hud.attackBtn = 1f;
            return;
        }
        if (Controls.hit(x, y, Controls.INTERACT_X, Controls.INTERACT_Y, Controls.INTERACT_R)) {
            hud.interactBtn = 1f;
            doInteract(game);
            return;
        }
        if (Controls.hit(x, y, Controls.DODGE_X, Controls.DODGE_Y, Controls.DODGE_R)) {
            doDodge(game);
            return;
        }
        if (x >= Controls.WEAPON_X && x <= Controls.WEAPON_X + Controls.WEAPON_W
                && y >= Controls.WEAPON_Y && y <= Controls.WEAPON_Y + Controls.WEAPON_H) {
            cycleWeapon(game);
            return;
        }
        if (x >= Controls.ITEM_X && x <= Controls.ITEM_X + Controls.ITEM_W
                && y >= Controls.ITEM_Y && y <= Controls.ITEM_Y + Controls.ITEM_H) {
            useItem(game);
            return;
        }
        if (x < Controls.MOVE_ZONE_MAX_X) {
            movePointer = id;
            hud.moveStickActive = true;
            hud.moveStickCx = x;
            hud.moveStickCy = y;
            hud.moveStickDx = 0f;
            hud.moveStickDy = 0f;
        } else {
            aimPointer = id;
            hud.aimStickActive = true;
            hud.aimStickCx = x;
            hud.aimStickCy = y;
            hud.aimStickDx = 0f;
            hud.aimStickDy = 0f;
        }
    }

    @Override
    public void touchMove(Game game, int id, float x, float y) {
        if (id == movePointer) {
            float dx = x - hud.moveStickCx, dy = y - hud.moveStickCy;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            float max = Controls.STICK_RADIUS;
            if (d > max) {
                dx = dx / d * max;
                dy = dy / d * max;
                d = max;
            }
            hud.moveStickDx = dx / max;
            hud.moveStickDy = dy / max;
        } else if (id == aimPointer) {
            float dx = x - hud.aimStickCx, dy = y - hud.aimStickCy;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            float max = Controls.STICK_RADIUS;
            if (d > max) {
                dx = dx / d * max;
                dy = dy / d * max;
                d = max;
            }
            hud.aimStickDx = dx / max;
            hud.aimStickDy = dy / max;
        }
    }

    @Override
    public void touchUp(Game game, int id, float x, float y) {
        if (id == movePointer) {
            movePointer = -1;
            hud.moveStickActive = false;
            hud.moveStickDx = hud.moveStickDy = 0f;
        } else if (id == aimPointer) {
            aimPointer = -1;
            hud.aimStickActive = false;
            hud.aimStickDx = hud.aimStickDy = 0f;
        }
        hud.attackHeld = false;
        hud.attackBtn = 0f;
        hud.interactBtn = 0f;
        hud.dodgeBtn = 0f;
    }

    protected void cycleWeapon(Game game) {
        WeaponType[] all = WeaponType.values();
        int idx = world.player.weapon.ordinal();
        for (int i = 1; i <= all.length; i++) {
            WeaponType w = all[(idx + i) % all.length];
            if (world.player.hasWeapon(w)) {
                world.player.weapon = w;
                game.audio.play(Sfx.UI_CLICK);
                return;
            }
        }
    }

    protected void useItem(Game game) {
        Player p = world.player;
        if (p.item == null || p.itemCount <= 0) return;
        switch (p.item) {
            case PAINKILLER: p.noDamageTimer = Balance.PAINKILLER_TIME; break;
            case STEROID: p.damageBoostTimer = Balance.STEROID_TIME; break;
            case ENERGY_DRINK: p.stamina = p.maxStamina; break;
            case EPO: p.speedBoostTimer = Balance.EPO_TIME; p.epoSlowTimer = Balance.EPO_TIME + Balance.EPO_SLOW_TIME; break;
            case INVISIBILITY: p.invisTimer = Balance.INVIS_TIME; break;
            default: break;
        }
        p.itemCount--;
        if (p.itemCount <= 0) p.item = null;
        game.audio.play(Sfx.PICKUP);
    }

    protected void doDodge(Game game) {
        Player p = world.player;
        if (p.dodgeCd > 0f || p.stamina < Balance.DODGE_STAMINA) return;
        p.stamina -= Balance.DODGE_STAMINA;
        p.dodgeCd = Balance.DODGE_CD;
        p.dodgeTimer = Balance.DODGE_TIME;
        p.invuln = Math.max(p.invuln, Balance.DODGE_TIME);
        hud.dodgeBtn = 1f;
        game.audio.play(Sfx.SWING);
    }

    protected void doInteract(Game game) {
        Player p = world.player;
        int ptx = world.map.tx(p.x), pty = world.map.ty(p.y);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int tx = ptx + dx, ty = pty + dy;
                TileType t = world.map.type(tx, ty);
                if (t == TileType.ELEVATOR) {
                    if (world.level.bossLevel && world.boss() != null && !world.boss().isDead()) {
                        world.addFloatingText("先解决赞恩", p.x, p.y - 40, Palette.RED);
                        return;
                    }
                    win(game);
                    return;
                }
                if (t == TileType.COMPUTER) {
                    world.camerasDisabled = true;
                    world.objective = Texts.OBJECTIVE_ELEVATOR;
                    world.addFloatingText("安全系统已关闭", p.x, p.y - 40, Palette.MINT);
                    game.audio.play(Sfx.HACK_SUCCESS);
                    return;
                }
                if (t == TileType.LOCKER) {
                    world.addFloatingText("空的", p.x, p.y - 40, Palette.TEXT_DIM);
                    return;
                }
                if (t == TileType.DOOR_LOCKED) {
                    if (p.hasKeycard) {
                        world.map.set(tx, ty, TileType.DOOR_OPEN);
                        world.addFloatingText("门开了", p.x, p.y - 40, Palette.MINT);
                        game.audio.play(Sfx.DOOR);
                    } else {
                        world.addFloatingText("需要钥匙卡", p.x, p.y - 40, Palette.RED);
                    }
                    return;
                }
            }
        }
    }

    protected void win(Game game) {
        if (finished) return;
        finished = true;
        world.won = true;
        game.audio.play(Sfx.ELEVATOR);
        game.unlockFloor(floor + 1);
        game.save.totalPlaySeconds += (int) elapsed;
        game.saveNow();
        game.gotoResult(true, floor, world.kills, elapsed);
    }

    @Override
    public void render(Game game, Renderer r) {
        worldRenderer.render(game, r, world);
        hudRenderer.render(game, r, world, hud);
        if (paused) {
            r.fillRect(0, 0, Game.WIDTH, Game.HEIGHT, Palette.alpha(Palette.BLACK, 0.6f));
            r.setTextAlign(1);
            r.setTextBold(true);
            r.setTextColor(Palette.TEXT);
            r.setTextSize(64);
            r.drawText(Texts.PAUSED, Game.WIDTH / 2f, 560);
            r.setTextSize(28);
            r.setTextColor(Palette.TEXT_DIM);
            r.drawText("点击屏幕继续", Game.WIDTH / 2f, 640);
        }
    }

    @Override
    public void backPressed(Game game) {
        paused = !paused;
        hud.paused = paused;
    }

    @Override
    public void pause(Game game) {
        paused = true;
        hud.paused = true;
    }

    @Override
    public void resume(Game game) {
        paused = false;
        hud.paused = false;
    }
}
