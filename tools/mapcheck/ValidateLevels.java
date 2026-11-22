import com.crazyclinic.core.content.Levels;
import com.crazyclinic.core.world.EnemySpawn;
import com.crazyclinic.core.world.EnemyType;
import com.crazyclinic.core.world.ItemSpawn;
import com.crazyclinic.core.world.LevelDef;
import com.crazyclinic.core.world.Pickup;
import com.crazyclinic.core.world.TileMap;
import com.crazyclinic.core.world.TileType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * 10 层关卡连通性 / 数据完整性校验器（关卡代理自测用）。
 *
 * 校验项：
 *  1. 尺寸 24~40 列 × 22~34 行；
 *  2. 每层恰好 1 个 S、1 个 E，C 1~3 台，钥匙卡 1~4 把，至少 1 扇锁门；
 *  3. 外圈封闭（边界全部是实心墙/窗，无泄漏）；
 *  4. 不拿钥匙：S 能到至少 1 张钥匙卡；
 *     拿钥匙后：S 能到全部 C（相邻可交互）、E、全部物品；敌人不卡墙、不孤立；
 *  5. 每层至少有一处 C 或 E 被锁门真正挡住（钥匙/骇客门槛生效）；
 *  6. 敌人配比与 docs/research.md §5.6 一致（G 同时代表看守人与守卫）；
 *  7. BOSS 层含 Z，第 1 层首武器/首钥匙距离 ≤ 15 格（教学友好）。
 *
 * 用法见 tools/mapcheck/README.md。
 */
public final class ValidateLevels {
    private static final int WALL_MIN_W = 24, WALL_MAX_W = 40;
    private static final int WALL_MIN_H = 22, WALL_MAX_H = 34;
    private static final int TUTORIAL_MAX_DIST = 15;

    /** §5.6 敌人配置：GUARD(看守+守卫)/DOCTOR/FLU/TECH/NURSE/SENTRY/BOSS */
    private static final int[][] EXPECTED_ENEMIES = {
            {2, 0, 0, 0, 0, 0, 0},
            {3, 0, 2, 0, 0, 1, 0},
            {3, 1, 2, 0, 0, 1, 0},
            {4, 1, 2, 1, 0, 1, 0},
            {3, 2, 1, 1, 1, 1, 0},
            {4, 2, 1, 2, 1, 2, 0},
            {5, 2, 2, 2, 1, 2, 0},
            {5, 2, 2, 3, 2, 3, 0},
            {7, 2, 1, 3, 2, 3, 0},
            // 第 10 层按任务指定：Z + 2 G + 2 H（有掩体的 BOSS 竞技场）
            {2, 0, 0, 0, 0, 2, 1},
    };
    /** §5.6 骇客电脑数（C）/ 钥匙卡数（k） */
    private static final int[] EXPECTED_HACK = {1, 1, 2, 2, 2, 3, 3, 3, 3, 1};
    private static final int[] EXPECTED_KEYS = {1, 2, 2, 2, 3, 3, 3, 3, 4, 1};

    private static int errors;
    private static int warnings;

    public static void main(String[] args) {
        System.out.println("== 疯狂诊所 · 10 层关卡校验 ==");
        System.out.println();
        for (LevelDef def : Levels.FLOORS) {
            checkFloor(def);
        }
        System.out.println();
        if (errors == 0) {
            System.out.println("ALL GREEN ✔  " + Levels.FLOORS.length + "/" + Levels.FLOORS.length
                    + " 层通过（尺寸 / S·E·C·k / 外圈封闭 / S→k→+→C→E 可达 / 敌人与物品位置 / 敌人配比）"
                    + (warnings > 0 ? "；" + warnings + " 条提醒" : ""));
        } else {
            System.out.println("FAILED ✘  " + errors + " 个错误"
                    + (warnings > 0 ? "，" + warnings + " 条提醒" : ""));
            System.exit(1);
        }
    }

    private static void checkFloor(LevelDef def) {
        List<String> errs = new ArrayList<>();
        List<String> warns = new ArrayList<>();
        TileMap map = def.map;
        int w = map.width, h = map.height;

        // ---- 1. 尺寸 ----
        if (w < WALL_MIN_W || w > WALL_MAX_W) errs.add("宽度 " + w + " 超出 " + WALL_MIN_W + "~" + WALL_MAX_W);
        if (h < WALL_MIN_H || h > WALL_MAX_H) errs.add("高度 " + h + " 超出 " + WALL_MIN_H + "~" + WALL_MAX_H);

        // ---- 2. 关键对象计数 ----
        int spawnCount = 0, elevatorCount = 0, computerCount = 0, lockedDoors = 0;
        int sx = -1, sy = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                TileType t = map.type(x, y);
                if (t == TileType.SPAWN) {
                    spawnCount++;
                    if (sx < 0) { sx = x; sy = y; }
                } else if (t == TileType.ELEVATOR) elevatorCount++;
                else if (t == TileType.COMPUTER) computerCount++;
                else if (t == TileType.DOOR_LOCKED) lockedDoors++;
            }
        }
        int keyCount = 0;
        List<int[]> keys = new ArrayList<>();
        List<int[]> computers = new ArrayList<>();
        List<int[]> elevators = new ArrayList<>();
        for (ItemSpawn s : def.items) {
            if (s.kind == Pickup.Kind.KEYCARD) {
                keyCount++;
                keys.add(new int[]{s.tx, s.ty});
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                TileType t = map.type(x, y);
                if (t == TileType.COMPUTER) computers.add(new int[]{x, y});
                else if (t == TileType.ELEVATOR) elevators.add(new int[]{x, y});
            }
        }
        if (spawnCount != 1) errs.add("S 数量 " + spawnCount + " != 1");
        if (elevatorCount != 1) errs.add("E 数量 " + elevatorCount + " != 1");
        if (computerCount < 1 || computerCount > 3) errs.add("C 数量 " + computerCount + " 不在 1~3");
        if (keyCount < 1 || keyCount > 4) errs.add("钥匙卡数量 " + keyCount + " 不在 1~4");
        if (lockedDoors < 1) errs.add("没有锁门 +");

        // ---- 3. 外圈封闭 ----
        for (int x = 0; x < w; x++) {
            if (!map.solid(x, 0)) errs.add("外圈开口 (" + x + ",0)");
            if (!map.solid(x, h - 1)) errs.add("外圈开口 (" + x + "," + (h - 1) + ")");
        }
        for (int y = 0; y < h; y++) {
            if (!map.solid(0, y)) errs.add("外圈开口 (0," + y + ")");
            if (!map.solid(w - 1, y)) errs.add("外圈开口 (" + (w - 1) + "," + y + ")");
        }

        // ---- 4/5. 连通性：先找钥匙，再开门 ----
        boolean[] noKey = bfs(map, sx, sy, false);
        boolean[] withKey = bfs(map, sx, sy, true);
        if (sx < 0) {
            errs.add("找不到出生点 S");
        } else {
            // 出生点距地图上边界至少 4 个 tile，否则会被顶部 HUD（0~240px）遮住
            if (def.spawnY < TileMap.TILE * 4f) {
                errs.add("出生点距上边界 < 4 tile（y=" + (int) def.spawnY
                        + "px，会被顶部 HUD 遮挡）");
            }
            if (!map.solidAt(def.spawnX, def.spawnY)) {
                int ptx = map.tx(def.spawnX), pty = map.ty(def.spawnY);
                if (!noKey[pty * w + ptx]) errs.add("出生点不在可通行区域");
                if (!hasOpenNeighbor(map, ptx, pty)) errs.add("出生点四周全被堵死");
            } else {
                errs.add("出生点落在墙里");
            }
        }

        boolean keyReachable = false;
        for (int[] k : keys) {
            if (inReach(noKey, w, k[0], k[1])) keyReachable = true;
            if (!inReach(withKey, w, k[0], k[1])) errs.add("钥匙卡不可达 (" + k[0] + "," + k[1] + ")");
        }
        if (!keyReachable) errs.add("不拿钥匙时没有任何钥匙卡可达");

        boolean hasGatedTarget = false;
        for (int[] c : computers) {
            boolean gated = !adjacentReach(noKey, map, c[0], c[1]) && adjacentReach(withKey, map, c[0], c[1]);
            if (gated) hasGatedTarget = true;
            if (!adjacentReach(withKey, map, c[0], c[1])) {
                errs.add("安保电脑不可达/不可交互 (" + c[0] + "," + c[1] + ")");
            }
        }
        for (int[] e : elevators) {
            boolean gated = !inReach(noKey, w, e[0], e[1]) && inReach(withKey, w, e[0], e[1]);
            if (gated) hasGatedTarget = true;
            if (!inReach(withKey, w, e[0], e[1])) errs.add("电梯不可达 (" + e[0] + "," + e[1] + ")");
        }
        if (!hasGatedTarget) errs.add("没有任何 C/E 被锁门挡住（钥匙门槛失效）");
        // 引擎按门消耗钥匙卡（GameScene.consumeKeycard）；锁门数 > 钥匙数会软锁
        if (lockedDoors > keyCount) {
            errs.add("锁门 " + lockedDoors + " 扇 > 钥匙卡 " + keyCount + " 把（按门消耗会软锁）");
        }

        // 物品全部可达
        for (ItemSpawn s : def.items) {
            if (!inReach(withKey, w, s.tx, s.ty)) {
                errs.add("物品不可达 " + itemLabel(s) + " (" + s.tx + "," + s.ty + ")");
            }
            if (map.solid(s.tx, s.ty)) errs.add("物品在墙里 " + itemLabel(s) + " (" + s.tx + "," + s.ty + ")");
        }

        // 敌人：不卡墙、不孤立、不被完全封死
        for (EnemySpawn s : def.enemies) {
            if (map.solid(s.tx, s.ty)) errs.add("敌人卡墙 " + s.type.label + " (" + s.tx + "," + s.ty + ")");
            if (!hasOpenNeighbor(map, s.tx, s.ty)) {
                errs.add("敌人四周全被堵死 " + s.type.label + " (" + s.tx + "," + s.ty + ")");
            } else if (!inReach(withKey, w, s.tx, s.ty)) {
                warns.add("敌人出生点不可达 " + s.type.label + " (" + s.tx + "," + s.ty + ")");
            }
        }

        // 锁门两侧必须可通行
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (map.type(x, y) == TileType.DOOR_LOCKED && !doorConnects(map, x, y)) {
                    errs.add("锁门两侧被堵 (" + x + "," + y + ")");
                }
            }
        }

        // ---- 6. 敌人/电脑/钥匙配比 ----
        int idx = def.index - 1;
        int[] counts = new int[EnemyType.values().length];
        for (EnemySpawn s : def.enemies) counts[s.type.ordinal()]++;
        if (idx >= 0 && idx < EXPECTED_ENEMIES.length) {
            int[] exp = EXPECTED_ENEMIES[idx];
            for (int i = 0; i < exp.length; i++) {
                if (counts[i] != exp[i]) {
                    errs.add("敌人数量不符 " + EnemyType.values()[i].label + " "
                            + counts[i] + " != §5.6 的 " + exp[i]);
                }
            }
            if (computerCount != EXPECTED_HACK[idx]) {
                errs.add("电脑数量 " + computerCount + " != §5.6 骇客数 " + EXPECTED_HACK[idx]);
            }
            if (keyCount != EXPECTED_KEYS[idx]) {
                errs.add("钥匙卡数量 " + keyCount + " != §5.6 钥匙数 " + EXPECTED_KEYS[idx]);
            }
        }

        // ---- 7. BOSS / 教学距离 ----
        boolean hasBoss = counts[EnemyType.BOSS.ordinal()] > 0;
        if (def.index == 10) {
            if (!def.bossLevel || !hasBoss) errs.add("第 10 层必须是含 Z 的 BOSS 层");
        } else if (def.bossLevel || hasBoss) {
            errs.add("只有第 10 层允许 BOSS/Z");
        }

        int distWeapon = nearestDistance(map, def.items, true);
        int distKey = nearestDistance(map, def.items, false);
        if (def.index == 1) {
            if (distWeapon < 0 || distWeapon > TUTORIAL_MAX_DIST) {
                errs.add("第 1 层第一把武器距离 " + distWeapon + " 格 > " + TUTORIAL_MAX_DIST);
            }
            if (distKey < 0 || distKey > TUTORIAL_MAX_DIST) {
                errs.add("第 1 层第一张钥匙卡距离 " + distKey + " 格 > " + TUTORIAL_MAX_DIST);
            }
        }

        // ---- 输出 ----
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%2d] %-10s %2dx%-2d  敌人 %2d  C%d k%d 门+%d | 首武器 %s 首钥匙 %s",
                def.index, def.name, w, h, def.enemies.size(), computerCount, keyCount, lockedDoors,
                distWeapon < 0 ? "-" : distWeapon + "格", distKey < 0 ? "-" : distKey + "格"));
        boolean ok = errs.isEmpty();
        sb.append(ok ? "  ✔" : "  ✘");
        System.out.println(sb);
        for (String e : errs) System.out.println("        ✘ " + e);
        for (String wmsg : warns) System.out.println("        ! " + wmsg);
        errors += errs.size();
        warnings += warns.size();
    }

    // ------------------------------------------------------------------
    private static boolean[] bfs(TileMap map, int sx, int sy, boolean hasKey) {
        int w = map.width, h = map.height;
        boolean[] seen = new boolean[w * h];
        if (sx < 0 || !walkable(map, sx, sy, hasKey)) return seen;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        seen[sy * w + sx] = true;
        q.add(sy * w + sx);
        while (!q.isEmpty()) {
            int cur = q.poll();
            int x = cur % w, y = cur / w;
            int[] dx = {1, -1, 0, 0}, dy = {0, 0, 1, -1};
            for (int i = 0; i < 4; i++) {
                int nx = x + dx[i], ny = y + dy[i];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (seen[ny * w + nx] || !walkable(map, nx, ny, hasKey)) continue;
                seen[ny * w + nx] = true;
                q.add(ny * w + nx);
            }
        }
        return seen;
    }

    private static boolean walkable(TileMap map, int x, int y, boolean hasKey) {
        TileType t = map.type(x, y);
        switch (t) {
            case FLOOR:
            case SPAWN:
            case DOOR_OPEN:
            case ELEVATOR:
                return true;
            case DOOR_LOCKED:
                return hasKey;
            default:
                return false;
        }
    }

    private static boolean inReach(boolean[] reach, int w, int x, int y) {
        return x >= 0 && y >= 0 && y * w + x < reach.length && reach[y * w + x];
    }

    /** 电脑是实心 tile，玩家站相邻格交互（3×3 范围）。 */
    private static boolean adjacentReach(boolean[] reach, TileMap map, int x, int y) {
        int w = map.width, h = map.height;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (inReach(reach, w, nx, ny)) return true;
            }
        }
        return false;
    }

    private static boolean hasOpenNeighbor(TileMap map, int x, int y) {
        int[] dx = {1, -1, 0, 0}, dy = {0, 0, 1, -1};
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i], ny = y + dy[i];
            if (!map.inBounds(nx, ny)) continue;
            if (!map.solid(nx, ny)) return true;
        }
        return false;
    }

    private static boolean doorConnects(TileMap map, int x, int y) {
        boolean l = !map.solid(x - 1, y), r = !map.solid(x + 1, y);
        boolean u = !map.solid(x, y - 1), d = !map.solid(x, y + 1);
        return (l && r) || (u && d);
    }

    /** weapons=true 找最近的武器拾取点；false 只找钥匙卡（不含药包等普通物品）。 */
    private static int nearestDistance(TileMap map, List<ItemSpawn> items, boolean weapons) {
        int best = -1;
        int w = map.width;
        int[] dist = new int[w * map.height];
        java.util.Arrays.fill(dist, -1);
        // 找到出生点：reach 对应的 BFS 起点就是 S tile
        int sx = -1, sy = -1;
        for (int y = 0; y < map.height && sx < 0; y++) {
            for (int x = 0; x < w; x++) {
                if (map.type(x, y) == TileType.SPAWN) { sx = x; sy = y; break; }
            }
        }
        if (sx < 0) return -1;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        dist[sy * w + sx] = 0;
        q.add(sy * w + sx);
        while (!q.isEmpty()) {
            int cur = q.poll();
            int x = cur % w, y = cur / w;
            int[] dx = {1, -1, 0, 0}, dy = {0, 0, 1, -1};
            for (int i = 0; i < 4; i++) {
                int nx = x + dx[i], ny = y + dy[i];
                if (nx < 0 || ny < 0 || nx >= w || ny >= map.height) continue;
                if (dist[ny * w + nx] >= 0 || !walkable(map, nx, ny, false)) continue;
                dist[ny * w + nx] = dist[y * w + x] + 1;
                q.add(ny * w + nx);
            }
        }
        for (ItemSpawn s : items) {
            boolean isWeapon = s.kind == Pickup.Kind.WEAPON;
            boolean isKey = s.kind == Pickup.Kind.KEYCARD;
            if (weapons ? !isWeapon : !isKey) continue;
            int d = dist[s.ty * w + s.tx];
            if (d >= 0 && (best < 0 || d < best)) best = d;
        }
        return best;
    }

    private static String itemLabel(ItemSpawn s) {
        switch (s.kind) {
            case WEAPON: return s.weapon.label;
            case KEYCARD: return "钥匙卡";
            case ORGAN: return "器官";
            default: return s.item == null ? "物品" : s.item.label;
        }
    }

    private ValidateLevels() {}
}
