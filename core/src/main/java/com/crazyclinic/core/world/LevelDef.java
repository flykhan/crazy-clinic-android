package com.crazyclinic.core.world;

import java.util.ArrayList;
import java.util.List;

/**
 * 一层（关卡）定义。ASCII 图例见 TileMap 与 docs/CONTRACT.md：
 * 敌人：G 看守人  D 医生  N 护士  F 感冒病人  T 中心助手  H 哨兵枪  Z BOSS
 * 武器：1 手枪  2 霰弹枪  3 泰瑟枪  4 斧头  5 手术刀  6 除颤器  7 本生灯  8 试管
 * 物品：m 小药包  h 药包  M 大药包  p 止痛药  s 类固醇  e 能量饮料  i 红细胞生成素
 *       v 隐形药  k 钥匙卡  o 器官（按 organOrder 顺序）
 */
public final class LevelDef {
    public final int index;          // 1-based
    public final String name;
    public final String subtitle;
    public final String intro;       // 进入关卡前的剧情/简报文本
    public final TileMap map;
    public final float spawnX, spawnY;
    public final List<EnemySpawn> enemies = new ArrayList<>();
    public final List<ItemSpawn> items = new ArrayList<>();
    public final boolean bossLevel;

    public LevelDef(int index, String name, String subtitle, String intro, String[] rows, Organ[] organOrder) {
        this.index = index;
        this.name = name;
        this.subtitle = subtitle;
        this.intro = intro;
        this.map = TileMap.parse(rows);
        float sx = -1, sy = -1;
        int organCursor = 0;
        boolean boss = false;
        for (int y = 0; y < map.height; y++) {
            for (int x = 0; x < map.width; x++) {
                char c = rawChar(rows, x, y);
                TileType t = TileType.fromChar(c);
                if (t == TileType.SPAWN) {
                    if (sx < 0) {
                        sx = map.cx(x);
                        sy = map.cy(y);
                    }
                    continue;
                }
                EnemyType et = enemyType(c);
                if (et != null) {
                    enemies.add(new EnemySpawn(et, x, y));
                    if (et == EnemyType.BOSS) boss = true;
                    continue;
                }
                WeaponType wt = weaponType(c);
                if (wt != null) {
                    items.add(ItemSpawn.weapon(wt, x, y));
                    continue;
                }
                ItemType it = itemType(c);
                if (it == ItemType.KEYCARD) {
                    items.add(ItemSpawn.keycard(x, y));
                    continue;
                }
                if (it == ItemType.ORGAN) {
                    Organ organ = organOrder != null && organOrder.length > 0
                            ? organOrder[Math.min(organCursor++, organOrder.length - 1)]
                            : Organ.BRAIN;
                    items.add(ItemSpawn.organ(organ, x, y));
                    continue;
                }
                if (it != null) {
                    items.add(ItemSpawn.item(it, itemAmount(it), x, y));
                }
            }
        }
        if (sx < 0) {
            // 容错：地图里没有 S 时找第一个可走瓦片
            outer:
            for (int y = 0; y < map.height; y++) {
                for (int x = 0; x < map.width; x++) {
                    if (!map.solid(x, y)) {
                        sx = map.cx(x);
                        sy = map.cy(y);
                        break outer;
                    }
                }
            }
        }
        this.spawnX = sx < 0 ? TileMap.TILE * 1.5f : sx;
        this.spawnY = sy < 0 ? TileMap.TILE * 1.5f : sy;
        this.bossLevel = boss;
    }

    private static char rawChar(String[] rows, int x, int y) {
        if (y < 0 || y >= rows.length) return '#';
        String row = rows[y];
        return x < row.length() ? row.charAt(x) : '#';
    }

    private static int itemAmount(ItemType it) {
        switch (it) {
            case MEDKIT_SMALL: return 15;
            case MEDKIT: return 30;
            case MEDKIT_LARGE: return 60;
            default: return 1;
        }
    }

    public static EnemyType enemyType(char c) {
        switch (c) {
            case 'G': return EnemyType.GUARD;
            case 'D': return EnemyType.DOCTOR;
            case 'N': return EnemyType.NURSE;
            case 'F': return EnemyType.FLU_PATIENT;
            case 'T': return EnemyType.TECH;
            case 'H': return EnemyType.SENTRY;
            case 'Z': return EnemyType.BOSS;
            default: return null;
        }
    }

    public static WeaponType weaponType(char c) {
        switch (c) {
            case '1': return WeaponType.PISTOL;
            case '2': return WeaponType.SHOTGUN;
            case '3': return WeaponType.TASER;
            case '4': return WeaponType.AXE;
            case '5': return WeaponType.SCALPEL;
            case '6': return WeaponType.DEFIBRILLATOR;
            case '7': return WeaponType.BUNSEN;
            case '8': return WeaponType.VIAL;
            default: return null;
        }
    }

    public static ItemType itemType(char c) {
        switch (c) {
            case 'm': return ItemType.MEDKIT_SMALL;
            case 'h': return ItemType.MEDKIT;
            case 'M': return ItemType.MEDKIT_LARGE;
            case 'p': return ItemType.PAINKILLER;
            case 's': return ItemType.STEROID;
            case 'e': return ItemType.ENERGY_DRINK;
            case 'i': return ItemType.EPO;
            case 'v': return ItemType.INVISIBILITY;
            case 'k': return ItemType.KEYCARD;
            case 'o': return ItemType.ORGAN;
            default: return null;
        }
    }
}
