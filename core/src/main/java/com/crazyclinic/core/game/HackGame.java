package com.crazyclinic.core.game;

import com.crazyclinic.core.HudState;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Balance;
import com.crazyclinic.core.content.Texts;
import com.crazyclinic.core.world.Organ;
import com.crazyclinic.core.world.World;

import java.util.Random;

/**
 * 骇客小游戏状态机。
 *
 * 视口 720x960，12x16 网格（格 60px）；白色光标从左上 (0,0) 拖到右下 (11,15)。
 * 格子类型：0=可走 1=阻挡 2=退回起点 3=警报失败。
 * 布局用 (层号, 尝试次数) 作种子确定性生成，保证重试同一布局；生成后校验有纯 0 通路。
 */
final class HackGame {
    static final int STATE_PLAYING = 0;
    static final int STATE_SUCCESS = 1;
    static final int STATE_FAILED = 2;

    static final int TYPE_FREE = 0;
    static final int TYPE_WALL = 1;
    static final int TYPE_RESET = 2;
    static final int TYPE_ALARM = 3;

    private static final float STEP_INTERVAL = 0.042f;

    int cols, rows, cell;
    int[] cells = new int[0];
    int cursorX, cursorY;
    int targetX, targetY;
    int startX, startY, goalX, goalY;
    float timeLeft, timeTotal;
    int state = STATE_PLAYING;
    float resultTimer;
    float stepTimer;
    boolean active;

    private int attempt;

    /** 开始一小局（可被 debugStartHack 调用）。 */
    boolean start(GameScene scene, com.crazyclinic.core.Game game) {
        if (scene == null) return false;
        World w = scene.world;
        cols = Balance.HACK_COLS > 0 ? Balance.HACK_COLS : 12;
        rows = Balance.HACK_ROWS > 0 ? Balance.HACK_ROWS : 16;
        cell = Balance.HACK_CELL > 0 ? Balance.HACK_CELL : 60;
        // 兜底：网格必须能覆盖在 720x960 视口里
        if (cell * cols > 720) cols = Math.max(4, 720 / cell);
        if (cell * rows > World.VIEW_H) rows = Math.max(4, (int) (World.VIEW_H / cell));

        startX = 0;
        startY = 0;
        goalX = Math.max(1, cols - 1);
        goalY = Math.max(1, rows - 1);
        cursorX = startX;
        cursorY = startY;
        targetX = startX;
        targetY = startY;
        stepTimer = 0f;
        state = STATE_PLAYING;
        resultTimer = 0f;
        active = true;

        generate(w.floor, attempt++);

        float bonus = 0f;
        if (game != null && game.save != null && game.save.hasOrgan(Organ.BRAIN)) {
            bonus = Balance.ORGAN_BRAIN_HACK_BONUS;
        }
        timeTotal = Math.max(5f, Balance.HACK_TIME + bonus);
        timeLeft = timeTotal;

        // 让 HUD 立刻可用（预览工具可能在下一帧之前就截图）
        scene.hud.hacking = true;
        syncHud(scene.hud);
        if (game != null) game.audio.play(Sfx.HACK_START);
        return true;
    }

    // ------------------------------------------------------------------
    // 布局生成
    // ------------------------------------------------------------------

    private void generate(int floor, int attemptIndex) {
        int tries = 0;
        build(floor, attemptIndex);
        while (!pathExists() && tries < 24) {
            build(floor, attemptIndex + 1 + tries);
            tries++;
        }
        if (!pathExists()) carveFallback();
    }

    private void build(int floor, int attemptIndex) {
        int n = cols * rows;
        if (cells.length != n) cells = new int[n];
        for (int i = 0; i < n; i++) cells[i] = TYPE_FREE;
        Random rng = new Random(0x5EEDC1A1L + floor * 7919L + attemptIndex * 104729L);
        for (int i = 0; i < n; i++) {
            if (i == 0 || i == n - 1) continue;
            if (rng.nextFloat() < 0.21f) cells[i] = TYPE_WALL;
        }
        cells[0] = TYPE_FREE;
        cells[n - 1] = TYPE_FREE;
        // 起点右侧 / 终点左侧保底一格，避免一开局就被封死
        if (cols > 1) cells[1] = TYPE_FREE;
        if (n > 2) cells[n - 2] = TYPE_FREE;

        int placed = 0;
        int guard = 0;
        while (placed < 4 && guard++ < 300) {
            int i = rng.nextInt(n);
            if (cells[i] == TYPE_FREE && i != 0 && i != n - 1) {
                cells[i] = TYPE_RESET;
                placed++;
            }
        }
        placed = 0;
        guard = 0;
        while (placed < 3 && guard++ < 300) {
            int i = rng.nextInt(n);
            if (cells[i] == TYPE_FREE && i != 0 && i != n - 1) {
                cells[i] = TYPE_ALARM;
                placed++;
            }
        }
    }

    /** 纯可走格（0）从起点到终点是否连通。 */
    private boolean pathExists() {
        int n = cols * rows;
        int[] queue = new int[n];
        boolean[] seen = new boolean[n];
        int head = 0, tail = 0;
        queue[tail++] = 0;
        seen[0] = true;
        while (head < tail) {
            int cur = queue[head++];
            if (cur == n - 1) return true;
            int cx = cur % cols, cy = cur / cols;
            if (cx + 1 < cols) tail = visit(cur + 1, queue, tail, seen);
            if (cx > 0) tail = visit(cur - 1, queue, tail, seen);
            if (cy + 1 < rows) tail = visit(cur + cols, queue, tail, seen);
            if (cy > 0) tail = visit(cur - cols, queue, tail, seen);
        }
        return false;
    }

    private int visit(int idx, int[] queue, int tail, boolean[] seen) {
        if (seen[idx] || cells[idx] != TYPE_FREE) return tail;
        seen[idx] = true;
        queue[tail++] = idx;
        return tail;
    }

    /** 极端情况：挖一条 Z 字纯通路过关，保证一定能玩。 */
    private void carveFallback() {
        int n = cols * rows;
        for (int i = 0; i < n; i++) {
            if (cells[i] == TYPE_WALL || cells[i] == TYPE_ALARM || cells[i] == TYPE_RESET) {
                int x = i % cols, y = i / cols;
                // 只清掉靠近主通道的两列/两行
                if (y < 2 || y >= rows - 2 || x < 2 || x >= cols - 2) cells[i] = TYPE_FREE;
            }
        }
        cells[0] = TYPE_FREE;
        cells[n - 1] = TYPE_FREE;
        // 中间那条 L 型通道
        for (int x = 0; x < cols; x++) cells[(rows / 2) * cols + x] = TYPE_FREE;
        for (int y = 0; y < rows; y++) cells[y * cols + (cols / 2)] = TYPE_FREE;
    }

    // ------------------------------------------------------------------
    // 更新
    // ------------------------------------------------------------------

    void update(GameScene scene, com.crazyclinic.core.Game game, float dt) {
        if (!active || scene == null) return;
        if (state == STATE_PLAYING) {
            timeLeft -= dt;
            if (timeLeft <= 0f) {
                timeLeft = 0f;
                fail(scene, game);
            } else {
                int dx = targetX - cursorX;
                int dy = targetY - cursorY;
                if (dx != 0 || dy != 0) {
                    stepTimer -= dt;
                    if (stepTimer <= 0f) {
                        stepTimer = STEP_INTERVAL;
                        step(scene, game);
                    }
                } else {
                    stepTimer = 0f;
                }
            }
        } else {
            resultTimer -= dt;
            if (resultTimer <= 0f) close(scene, game);
        }
        syncHud(scene.hud);
    }

    /** 朝触点贪心走一格；被墙挡住就停下等玩家重新拖。 */
    private void step(GameScene scene, com.crazyclinic.core.Game game) {
        int dx = Integer.compare(targetX, cursorX);
        int dy = Integer.compare(targetY, cursorY);
        int nx = cursorX, ny = cursorY;
        int adx = Math.abs(targetX - cursorX);
        int ady = Math.abs(targetY - cursorY);
        if (adx >= ady && dx != 0) nx += dx;
        else if (dy != 0) ny += dy;
        else if (dx != 0) nx += dx;
        if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) {
            targetX = cursorX;
            targetY = cursorY;
            return;
        }
        int t = cells[ny * cols + nx];
        if (t == TYPE_WALL) {
            // 撞墙：停下，让玩家换方向
            targetX = cursorX;
            targetY = cursorY;
            return;
        }
        cursorX = nx;
        cursorY = ny;
        if (game != null && ((cursorX + cursorY) & 1) == 0) game.audio.play(Sfx.HACK_STEP);

        if (t == TYPE_RESET) {
            cursorX = startX;
            cursorY = startY;
            targetX = startX;
            targetY = startY;
            scene.showToast("被弹回起点！", 1.6f);
            if (game != null) game.audio.play(Sfx.HACK_FAIL);
        } else if (t == TYPE_ALARM) {
            fail(scene, game);
        } else if (cursorX == goalX && cursorY == goalY) {
            success(scene, game);
        }
    }

    private void success(GameScene scene, com.crazyclinic.core.Game game) {
        if (state != STATE_PLAYING) return;
        state = STATE_SUCCESS;
        resultTimer = 1.25f;
        targetX = cursorX;
        targetY = cursorY;
        scene.applyHackSuccess(game);
        if (game != null) game.audio.play(Sfx.HACK_SUCCESS);
    }

    private void fail(GameScene scene, com.crazyclinic.core.Game game) {
        if (state != STATE_PLAYING) return;
        state = STATE_FAILED;
        resultTimer = 1.35f;
        targetX = cursorX;
        targetY = cursorY;
        scene.applyHackFail(game);
        if (game != null) game.audio.play(Sfx.HACK_FAIL);
    }

    private void close(GameScene scene, com.crazyclinic.core.Game game) {
        active = false;
        scene.hud.hacking = false;
        scene.hud.hackSuccess = false;
        scene.hud.hackFailed = false;
        scene.hud.hackResultTimer = 0f;
        scene.onHackClosed(game);
    }

    void syncHud(HudState hud) {
        if (hud == null) return;
        hud.hacking = active;
        hud.hackCols = cols;
        hud.hackRows = rows;
        hud.hackCursorX = cursorX;
        hud.hackCursorY = cursorY;
        hud.hackStartX = startX;
        hud.hackStartY = startY;
        hud.hackGoalX = goalX;
        hud.hackGoalY = goalY;
        hud.hackTimeLeft = Math.max(0f, timeLeft);
        hud.hackTimeTotal = timeTotal;
        hud.hackFailed = state == STATE_FAILED;
        hud.hackSuccess = state == STATE_SUCCESS;
        hud.hackResultTimer = Math.max(0f, resultTimer);
        hud.hackCells = cells;
    }

    // ------------------------------------------------------------------
    // 输入 / 调试
    // ------------------------------------------------------------------

    /** 屏幕坐标（720x1280）→ 网格目标格；暂停/结算输入不会走到这里。 */
    void setTargetFromScreen(float x, float y) {
        if (!active || state != STATE_PLAYING) return;
        int gx = (int) (x / (float) cell);
        int gy = (int) (y / (float) cell);
        gx = Math.max(0, Math.min(cols - 1, gx));
        gy = Math.max(0, Math.min(rows - 1, gy));
        targetX = gx;
        targetY = gy;
    }

    void cancel() {
        active = false;
        state = STATE_PLAYING;
    }

    /** 调试/QA：直接判定本局成败。 */
    void debugFinish(GameScene scene, com.crazyclinic.core.Game game, boolean win) {
        if (!active) return;
        if (win) {
            cursorX = goalX;
            cursorY = goalY;
            success(scene, game);
            close(scene, game);
        } else {
            fail(scene, game);
            close(scene, game);
        }
    }
}
