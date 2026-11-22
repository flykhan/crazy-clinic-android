package com.crazyclinic.core.game;

import com.crazyclinic.core.world.TileMap;

/**
 * 网格 BFS 寻路（4 向），带 visited 时间戳 + 队列/前驱数组复用，
 * 单次搜索 O(tile 数)，不做任何分配，供敌人 AI 低频重算路径。
 *
 * <p>只处理"可走/不可走"，锁门在打开前是 solid，因此敌人不会穿锁门。</p>
 */
final class Pathfinder {
    /** 单条路径最大长度（格），超长截断。 */
    static final int MAX_PATH = 256;

    private final TileMap map;
    private final int w, h;
    private final int[] prev;
    private final int[] queue;
    private final int[] mark;
    private final int[] reverse;
    private int stamp;

    Pathfinder(TileMap map) {
        this.map = map;
        this.w = Math.max(1, map == null ? 1 : map.width);
        this.h = Math.max(1, map == null ? 1 : map.height);
        int n = w * h;
        prev = new int[n];
        queue = new int[n];
        mark = new int[n];
        reverse = new int[n];
    }

    boolean walkable(int tx, int ty) {
        return map != null && map.inBounds(tx, ty) && !map.solid(tx, ty);
    }

    /**
     * 从 (sx,sy) 寻路到 (gx,gy)。
     * 路径按顺序写入 outX/outY（瓦片中心，第一个点是起点的下一格），返回写入的格数。
     * 返回 0 表示不可达 / 已在目标 / 参数非法。outX/outY 长度需 >= MAX_PATH。
     */
    int find(int sx, int sy, int gx, int gy, float[] outX, float[] outY) {
        if (map == null || outX == null || outY == null) return 0;
        if (!map.inBounds(sx, sy) || !map.inBounds(gx, gy)) return 0;
        if (map.solid(gx, gy) || map.solid(sx, sy)) return 0;
        if (sx == gx && sy == gy) return 0;

        stamp++;
        int start = sy * w + sx;
        int goal = gy * w + gx;
        mark[start] = stamp;
        prev[start] = -1;
        int head = 0, tail = 0;
        queue[tail++] = start;
        boolean found = false;

        while (head < tail) {
            int cur = queue[head++];
            if (cur == goal) {
                found = true;
                break;
            }
            int cx = cur % w;
            int cy = cur / w;
            // 4 向；先横后纵的无序 BFS，路径会自然贴墙。
            if (cx + 1 < w) {
                int ni = cur + 1;
                if (mark[ni] != stamp && !map.solid(cx + 1, cy)) {
                    mark[ni] = stamp;
                    prev[ni] = cur;
                    queue[tail++] = ni;
                }
            }
            if (cx > 0) {
                int ni = cur - 1;
                if (mark[ni] != stamp && !map.solid(cx - 1, cy)) {
                    mark[ni] = stamp;
                    prev[ni] = cur;
                    queue[tail++] = ni;
                }
            }
            if (cy + 1 < h) {
                int ni = cur + w;
                if (mark[ni] != stamp && !map.solid(cx, cy + 1)) {
                    mark[ni] = stamp;
                    prev[ni] = cur;
                    queue[tail++] = ni;
                }
            }
            if (cy > 0) {
                int ni = cur - w;
                if (mark[ni] != stamp && !map.solid(cx, cy - 1)) {
                    mark[ni] = stamp;
                    prev[ni] = cur;
                    queue[tail++] = ni;
                }
            }
        }
        if (!found) return 0;

        int n = 0;
        int cur = goal;
        while (cur != start && n < reverse.length) {
            reverse[n++] = cur;
            cur = prev[cur];
        }
        int count = Math.min(n, Math.min(MAX_PATH, outX.length));
        for (int i = 0; i < count; i++) {
            int idx = reverse[n - 1 - i];
            int tx = idx % w;
            int ty = idx / w;
            outX[i] = map.cx(tx);
            outY[i] = map.cy(ty);
        }
        return count;
    }

    /** 玩家/实体所在格到目标格是否可达（不写路径，快速测试用）。 */
    boolean reachable(float fromX, float fromY, float toX, float toY) {
        if (map == null) return false;
        int sx = map.tx(fromX), sy = map.ty(fromY);
        int gx = map.tx(toX), gy = map.ty(toY);
        if (sx == gx && sy == gy) return true;
        return find(sx, sy, gx, gy, new float[1], new float[1]) > 0;
    }
}
