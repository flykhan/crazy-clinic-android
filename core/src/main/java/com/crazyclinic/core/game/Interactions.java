package com.crazyclinic.core.game;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Balance;
import com.crazyclinic.core.content.Texts;
import com.crazyclinic.core.world.ItemType;
import com.crazyclinic.core.world.Organ;
import com.crazyclinic.core.world.Pickup;
import com.crazyclinic.core.world.Player;
import com.crazyclinic.core.world.TileMap;
import com.crazyclinic.core.world.TileType;
import com.crazyclinic.core.world.WeaponType;
import com.crazyclinic.core.world.World;

/**
 * 交互与拾取：电脑 / 锁门 / 储物柜 / 电梯的提示与动作、物品效果、
 * 拾取物自动吸附。
 */
final class Interactions {
    private static final float ATTRACT_RADIUS = 150f;
    private static final float ATTRACT_SPEED = 340f;

    private final boolean[] searched;   // 储物柜是否搜过（按瓦片索引）
    private final int width;
    private float compactTimer;

    // 当前帧找到的交互点
    private int targetTx = -1, targetTy = -1;
    private TileType targetType;

    Interactions(TileMap map) {
        this.width = Math.max(1, map == null ? 1 : map.width);
        int n = width * Math.max(1, map == null ? 1 : map.height);
        this.searched = new boolean[n];
    }

    // ==================================================================
    // 每帧提示
    // ==================================================================

    void updatePrompt(GameScene s, Game g) {
        World w = s.world;
        Player p = w.player;
        targetTx = -1;
        targetTy = -1;
        targetType = null;
        s.hud.interactPrompt = null;
        s.hud.interactAvailable = false;
        if (p == null || p.dead) return;

        int ptx = w.map.tx(p.x);
        int pty = w.map.ty(p.y);
        int bestScore = -1;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int tx = ptx + dx, ty = pty + dy;
                if (!w.map.inBounds(tx, ty)) continue;
                TileType t = w.map.type(tx, ty);
                int score = switch (t) {
                    case ELEVATOR -> 40;
                    case COMPUTER -> w.camerasDisabled ? -1 : 30;
                    case DOOR_LOCKED -> 25;
                    case LOCKER -> 20;
                    default -> -1;
                };
                if (score < 0) continue;
                // 离玩家更近的优先
                float d = MathUtil.dist(p.x, p.y, w.map.cx(tx), w.map.cy(ty));
                score -= (int) (d / TileMap.TILE);
                if (score <= bestScore) continue;
                bestScore = score;
                targetTx = tx;
                targetTy = ty;
                targetType = t;
            }
        }
        if (targetType == null) return;

        switch (targetType) {
            case ELEVATOR: {
                boolean bossAlive = w.level.bossLevel && w.boss() != null && !w.boss().isDead();
                s.hud.interactAvailable = !bossAlive;
                s.hud.interactPrompt = bossAlive ? "先解决赞恩" : Texts.PROMPT_ELEVATOR;
                break;
            }
            case COMPUTER:
                if (w.camerasDisabled) {
                    s.hud.interactAvailable = false;
                    s.hud.interactPrompt = Texts.HACK_SUCCESS;
                } else {
                    s.hud.interactAvailable = true;
                    s.hud.interactPrompt = Texts.PROMPT_HACK;
                }
                break;
            case LOCKER: {
                boolean done = isSearched(targetTx, targetTy);
                s.hud.interactAvailable = !done;
                s.hud.interactPrompt = done ? Texts.PROMPT_EMPTY : Texts.PROMPT_LOCKER;
                break;
            }
            case DOOR_LOCKED:
                s.hud.interactAvailable = true;
                s.hud.interactPrompt = s.hasKeycard() ? "用钥匙卡开门" : Texts.PROMPT_LOCKED;
                break;
            default:
                break;
        }
    }

    private boolean isSearched(int tx, int ty) {
        int idx = ty * width + tx;
        return idx >= 0 && idx < searched.length && searched[idx];
    }

    private void markSearched(int tx, int ty) {
        int idx = ty * width + tx;
        if (idx >= 0 && idx < searched.length) searched[idx] = true;
    }

    // ==================================================================
    // 互动
    // ==================================================================

    void interact(GameScene s, Game g) {
        if (targetType == null) {
            s.showToast("这里没什么可操作的", 1.2f);
            if (g != null) g.audio.play(Sfx.UI_BACK);
            return;
        }
        World w = s.world;
        Player p = w.player;
        switch (targetType) {
            case ELEVATOR:
                if (w.level.bossLevel && w.boss() != null && !w.boss().isDead()) {
                    s.showToast("先把赞恩解决掉！", 1.8f);
                    if (g != null) g.audio.play(Sfx.UI_BACK);
                    return;
                }
                s.startFloorClear(g);
                break;
            case COMPUTER:
                if (w.camerasDisabled) {
                    s.showToast(Texts.HACK_SUCCESS, 1.6f);
                    return;
                }
                s.startHack(g, false);
                break;
            case LOCKER:
                if (isSearched(targetTx, targetTy)) {
                    s.showToast(Texts.PROMPT_EMPTY, 1.2f);
                    if (g != null) g.audio.play(Sfx.UI_BACK);
                } else {
                    markSearched(targetTx, targetTy);
                    searchLocker(s, g, targetTx, targetTy);
                }
                break;
            case DOOR_LOCKED:
                if (s.consumeKeycard()) {
                    w.map.set(targetTx, targetTy, TileType.DOOR_OPEN);
                    s.onDoorOpened();
                    s.showToast("门开了", 1.2f);
                    s.effects.text(w, "咔嗒", w.map.cx(targetTx), w.map.cy(targetTy) - 30, Palette.MINT);
                    if (g != null) g.audio.play(Sfx.DOOR);
                    s.combat.noise(s, w.map.cx(targetTx), w.map.cy(targetTy), 260f);
                } else {
                    s.showToast(Texts.PROMPT_LOCKED, 1.6f);
                    s.effects.text(w, "需要钥匙卡", p.x, p.y - 42, Palette.RED);
                    if (g != null) g.audio.play(Sfx.UI_BACK);
                }
                break;
            default:
                break;
        }
    }

    /** 储物柜随机掉落（武器/药品/弹药/钥匙卡/道具）。直接生效并给反馈。 */
    private void searchLocker(GameScene s, Game g, int tx, int ty) {
        World w = s.world;
        Player p = w.player;
        float x = w.map.cx(tx), y = w.map.cy(ty);
        int roll = MathUtil.randInt(0, 11);
        if (roll <= 2) {
            spawnAt(s, g, Pickup.item(ItemType.MEDKIT_SMALL, 15, x, y));
        } else if (roll == 3) {
            spawnAt(s, g, Pickup.item(ItemType.MEDKIT, 30, x, y));
        } else if (roll == 4 || roll == 5) {
            spawnAt(s, g, Pickup.item(ItemType.AMMO, MathUtil.randInt(4, 9), x, y));
        } else if (roll == 6) {
            spawnAt(s, g, Pickup.item(ItemType.PAINKILLER, 1, x, y));
        } else if (roll == 7) {
            spawnAt(s, g, Pickup.item(ItemType.ENERGY_DRINK, 1, x, y));
        } else if (roll == 8) {
            spawnAt(s, g, Pickup.item(ItemType.STEROID, 1, x, y));
        } else if (roll == 9) {
            spawnAt(s, g, Pickup.item(ItemType.INVISIBILITY, 1, x, y));
        } else if (roll == 10) {
            // 随机武器（可能已拥有 → 补弹药）
            WeaponType[] pool = {WeaponType.PISTOL, WeaponType.SHOTGUN, WeaponType.TASER,
                    WeaponType.SCALPEL, WeaponType.DEFIBRILLATOR, WeaponType.BUNSEN, WeaponType.VIAL};
            WeaponType wt = pool[MathUtil.randInt(0, pool.length - 1)];
            spawnAt(s, g, Pickup.weapon(wt, x, y));
        } else {
            spawnAt(s, g, Pickup.keycard(x, y));
        }
        if (g != null) g.audio.play(Sfx.LOCKER);
        s.effects.pickupFlash(w, x, y, Palette.GOLD);
        s.effects.smoke(w, x, y, 2, 0.6f);
    }

    private void spawnAt(GameScene s, Game g, Pickup pk) {
        pk.bob = MathUtil.rand(0f, MathUtil.TAU);
        // 直接给到玩家（从柜子里"掏出来"的感觉），不经地面吸附
        applyPickup(s, g, pk);
    }

    // ==================================================================
    // 地面拾取物
    // ==================================================================

    void updatePickups(GameScene s, Game g, float dt) {
        World w = s.world;
        Player p = w.player;
        if (p == null || p.dead) return;
        float pr2 = Balance.PICKUP_RADIUS * Balance.PICKUP_RADIUS;
        float attract2 = ATTRACT_RADIUS * ATTRACT_RADIUS;
        for (int i = 0; i < w.pickups.size(); i++) {
            Pickup pk = w.pickups.get(i);
            if (pk == null || pk.taken) continue;
            float d2 = MathUtil.dist2(p.x, p.y, pk.x, pk.y);
            if (d2 <= pr2) {
                applyPickup(s, g, pk);
                continue;
            }
            if (d2 <= attract2) {
                float d = (float) Math.sqrt(d2);
                if (d > 0.01f) {
                    float step = Math.min(ATTRACT_SPEED * dt, d - Balance.PICKUP_RADIUS * 0.5f);
                    if (step > 0f) {
                        pk.x += (p.x - pk.x) / d * step;
                        pk.y += (p.y - pk.y) / d * step;
                    }
                }
            }
        }
        compactTimer -= dt;
        if (compactTimer <= 0f) {
            compactTimer = 2.5f;
            compactTaken(w);
        }
    }

    private void compactTaken(World w) {
        for (int i = w.pickups.size() - 1; i >= 0; i--) {
            if (w.pickups.get(i).taken) w.pickups.remove(i);
        }
    }

    // ==================================================================
    // 拾取生效
    // ==================================================================

    void applyPickup(GameScene s, Game g, Pickup pk) {
        if (pk == null || pk.taken) return;
        World w = s.world;
        Player p = w.player;
        pk.taken = true;
        switch (pk.kind) {
            case WEAPON: {
                WeaponType wt = pk.weapon;
                if (wt == null) return;
                boolean had = p.hasWeapon(wt);
                Balance.WeaponStats ws = Balance.weapon(wt);
                if (!had) {
                    p.giveWeapon(wt);
                    s.effects.text(w, "获得 " + wt.label, pk.x, pk.y - 34, Palette.GOLD);
                    s.showToast("获得武器：" + wt.label, 2.2f);
                    if (g != null) g.audio.play(Sfx.PICKUP_WEAPON);
                } else if (ws.maxAmmo > 0) {
                    int before = p.ammo[wt.ordinal()];
                    p.giveAmmo(wt, ws.maxAmmo);
                    int got = Math.max(0, p.ammo[wt.ordinal()] - before);
                    s.effects.text(w, wt.label + " 弹药 +" + got, pk.x, pk.y - 34, Palette.GOLD);
                    if (g != null) g.audio.play(Sfx.PICKUP);
                } else {
                    // 无限弹药武器重复拾取：回收手术刀等
                    s.effects.text(w, wt.label, pk.x, pk.y - 34, Palette.TEXT_DIM);
                    if (g != null) g.audio.play(Sfx.PICKUP);
                }
                s.effects.pickupFlash(w, pk.x, pk.y, Palette.GOLD);
                break;
            }
            case KEYCARD:
                s.addKeycard();
                s.effects.text(w, "钥匙卡 ×" + s.keycards(), pk.x, pk.y - 34, Palette.MINT);
                s.showToast("拿到钥匙卡！去找红色锁门（+）", 2.6f);
                s.effects.pickupFlash(w, pk.x, pk.y, Palette.MINT);
                if (g != null) g.audio.play(Sfx.KEYCARD);
                break;
            case ORGAN: {
                Organ organ = pk.organ;
                if (organ == null) return;
                boolean already = g != null && g.save != null && g.save.hasOrgan(organ);
                if (already) {
                    float heal = Math.min(20f, p.maxHp - p.hp);
                    p.hp = Math.min(p.maxHp, p.hp + 20f);
                    s.effects.text(w, organ.label + " 已强化 +" + (int) heal, pk.x, pk.y - 34, Palette.PURPLE);
                } else {
                    p.applyOrganBonus(organ);
                    if (organ == Organ.ARMS) p.armsBonus = true;
                    if (g != null) {
                        g.save.addOrgan(organ);
                        g.saveNow();
                    }
                    s.effects.text(w, "永久强化：" + organ.label, pk.x, pk.y - 34, Palette.PURPLE);
                    s.showToast(organ.label + " —— " + organ.desc, 3.6f);
                    s.hud.healFlash = Math.max(s.hud.healFlash, 0.8f);
                    if (g != null) g.audio.play(Sfx.ORGAN_UPGRADE);
                }
                s.effects.pickupFlash(w, pk.x, pk.y, Palette.PURPLE);
                break;
            }
            case ITEM:
            default:
                applyItem(s, g, pk);
                break;
        }
    }

    private void applyItem(GameScene s, Game g, Pickup pk) {
        World w = s.world;
        Player p = w.player;
        if (pk.item == null) return;
        switch (pk.item) {
            case MEDKIT_SMALL:
            case MEDKIT:
            case MEDKIT_LARGE: {
                float heal = pk.amount > 0 ? pk.amount : Balance.HEAL_SMALL;
                float before = p.hp;
                p.hp = Math.min(p.maxHp, p.hp + heal);
                float got = p.hp - before;
                s.effects.text(w, "+" + (int) got, pk.x, pk.y - 34, Palette.GREEN);
                s.effects.pickupFlash(w, pk.x, pk.y, Palette.GREEN);
                s.hud.healFlash = Math.max(s.hud.healFlash, 0.8f);
                if (g != null) g.audio.play(Sfx.HEAL);
                break;
            }
            case AMMO: {
                WeaponType target = pickAmmoTarget(p);
                if (target != null) {
                    int before = p.ammo[target.ordinal()];
                    p.giveAmmo(target, Math.max(1, pk.amount));
                    int got = Math.max(0, p.ammo[target.ordinal()] - before);
                    s.effects.text(w, target.label + " +" + got, pk.x, pk.y - 34, Palette.GOLD);
                } else {
                    s.effects.text(w, "弹药 +" + Math.max(1, pk.amount), pk.x, pk.y - 34, Palette.TEXT_DIM);
                }
                s.effects.pickupFlash(w, pk.x, pk.y, Palette.GOLD);
                if (g != null) g.audio.play(Sfx.PICKUP);
                break;
            }
            case PAINKILLER:
            case STEROID:
            case ENERGY_DRINK:
            case EPO:
            case INVISIBILITY: {
                if (p.item == pk.item) {
                    p.itemCount += Math.max(1, pk.amount);
                } else {
                    p.item = pk.item;
                    p.itemCount = Math.max(1, pk.amount);
                }
                s.effects.text(w, "拾取 " + pk.item.label + " ×" + p.itemCount, pk.x, pk.y - 34, Palette.TEXT);
                s.effects.pickupFlash(w, pk.x, pk.y, Palette.GOLD);
                if (g != null) g.audio.play(Sfx.PICKUP);
                break;
            }
            default:
                break;
        }
    }

    private static WeaponType pickAmmoTarget(Player p) {
        // 优先当前武器；否则挑一个非满弹的有限弹药武器
        int idx = p.weapon.ordinal();
        Balance.WeaponStats cur = Balance.weapon(p.weapon);
        if (cur.maxAmmo > 0 && p.ammo[idx] < cur.maxAmmo) return p.weapon;
        for (WeaponType wt : WeaponType.values()) {
            Balance.WeaponStats ws = Balance.weapon(wt);
            if (ws.maxAmmo <= 0) continue;
            if (!p.hasWeapon(wt)) continue;
            if (p.ammo[wt.ordinal()] < ws.maxAmmo) return wt;
        }
        return null;
    }
}
