package com.crazyclinic.preview;

import com.crazyclinic.core.Controls;
import com.crazyclinic.core.Game;
import com.crazyclinic.core.HudState;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.SaveData;
import com.crazyclinic.core.content.Texts;
import com.crazyclinic.core.game.GameScene;
import com.crazyclinic.core.world.Enemy;
import com.crazyclinic.core.world.ItemType;
import com.crazyclinic.core.world.Organ;
import com.crazyclinic.core.world.Player;
import com.crazyclinic.core.world.TileMap;
import com.crazyclinic.core.world.TileType;
import com.crazyclinic.core.world.WeaponType;
import com.crazyclinic.core.world.World;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 桌面预览入口：无模拟器/无显示环境下构造 Game，按场景渲染并输出 720x1280 PNG。
 *
 * <pre>
 *   java -Djava.awt.headless=true -cp ... com.crazyclinic.preview.PreviewMain [scene ...]
 *   选项: --out=DIR  输出目录（默认 docs/screenshots）
 *         --list     列出所有场景名
 *         --all      全部场景（默认）
 * </pre>
 *
 * 场景名：title level_select intro gameplay_floor1 gameplay_floor5 gameplay_floor10
 *        hacking pause result_win result_lose settings about tutorial
 */
public final class PreviewMain {

    private static final String[] ALL_SCENES = {
            "title", "level_select", "intro",
            "gameplay_floor1", "gameplay_floor5", "gameplay_floor10",
            "hacking", "pause", "result_win", "result_lose",
            "settings", "about", "tutorial",
    };

    private static final float DT = 1f / 60f;

    private static File outDir = new File("docs/screenshots");
    private static int written;
    private static int failed;

    public static void main(String[] args) {
        List<String> scenes = new ArrayList<>();
        boolean listOnly = false;
        for (String a : args) {
            if (a == null || a.isEmpty()) continue;
            if (a.equals("--list")) {
                listOnly = true;
            } else if (a.equals("--all")) {
                scenes.clear();
                scenes.addAll(Arrays.asList(ALL_SCENES));
            } else if (a.startsWith("--out=")) {
                outDir = new File(a.substring("--out=".length()));
            } else if (a.equals("--help") || a.equals("-h")) {
                printHelp();
                return;
            } else if (a.startsWith("-")) {
                System.err.println("[preview] unknown option: " + a);
                printHelp();
                System.exit(2);
            } else {
                scenes.add(a);
            }
        }
        if (listOnly) {
            for (String s : ALL_SCENES) System.out.println(s);
            return;
        }
        if (scenes.isEmpty()) scenes.addAll(Arrays.asList(ALL_SCENES));
        //noinspection ResultOfMethodCallIgnored
        outDir.mkdirs();
        System.out.println("[preview] out=" + outDir.getAbsolutePath() + " scenes=" + scenes.size());

        Java2DRenderer renderer = new Java2DRenderer();
        long t0 = System.currentTimeMillis();
        for (String scene : scenes) {
            if (!isKnown(scene)) {
                System.err.println("[preview] unknown scene: " + scene + " (use --list)");
                failed++;
                continue;
            }
            System.out.println("[preview] --- " + scene);
            try {
                renderScene(scene, renderer);
            } catch (Throwable t) {
                failed++;
                System.err.println("[preview] FAILED " + scene + ": " + t);
                t.printStackTrace();
            }
        }
        renderer.dispose();
        long ms = System.currentTimeMillis() - t0;
        System.out.println("[preview] done: " + written + " OK, " + failed + " failed, " + ms + " ms");
        if (failed > 0) System.exit(1);
    }

    private static void printHelp() {
        System.out.println("Usage: preview.sh [--out=DIR] [--list] [--all] [scene ...]");
        System.out.println("scenes: " + String.join(" ", ALL_SCENES));
    }

    private static boolean isKnown(String name) {
        for (String s : ALL_SCENES) if (s.equals(name)) return true;
        return false;
    }

    // ------------------------------------------------------------------
    // 场景分发
    // ------------------------------------------------------------------

    private static void renderScene(String scene, Java2DRenderer r) throws Exception {
        Game game = newGame();
        game.renderer = r;
        if (scene.equals("title")) {
            game.gotoTitle();
            step(game, 40);
        } else if (scene.equals("level_select")) {
            game.gotoLevelSelect();
            step(game, 40);
        } else if (scene.equals("intro")) {
            game.gotoIntro(5);
            step(game, 40);
        } else if (scene.startsWith("gameplay_floor")) {
            int floor = Integer.parseInt(scene.substring("gameplay_floor".length()));
            prepareGameplay(game, floor);
        } else if (scene.equals("hacking")) {
            prepareHacking(game);
        } else if (scene.equals("pause")) {
            preparePause(game);
        } else if (scene.equals("result_win")) {
            game.gotoResult(true, 4, 23, 355.4f);
            step(game, 40);
        } else if (scene.equals("result_lose")) {
            game.gotoResult(false, 4, 11, 168.2f);
            step(game, 40);
        } else if (scene.equals("settings")) {
            game.gotoSettings();
            step(game, 40);
        } else if (scene.equals("about")) {
            game.gotoAbout();
            step(game, 40);
        } else if (scene.equals("tutorial")) {
            game.gotoTutorial();
            step(game, 40);
        }
        // 场景切换/淡入不应污染截图
        game.fade = 0f;
        render(game, r, scene);
    }

    /** 每次预览用独立存档：解锁 6 层、两个器官、有统计，标题页会显示“继续”。 */
    private static Game newGame() {
        SaveData sd = new SaveData();
        sd.unlockedFloor = 6;
        sd.organsMask = Organ.HEART.bit | Organ.BRAIN.bit;
        sd.totalKills = 137;
        sd.deaths = 4;
        sd.totalPlaySeconds = 5382;
        sd.tutorialSeen = true;
        MemSaveStore store = new MemSaveStore(sd.serialize());
        return new Game(new SilentAudio(), store);
    }

    private static void step(Game game, int frames) {
        for (int i = 0; i < frames; i++) game.update(DT);
    }

    private static void render(Game game, Java2DRenderer r, String name) throws Exception {
        r.clear(Palette.BLACK);
        game.render(r);
        File file = new File(outDir, name + ".png");
        r.writePng(file);
        // 尺寸校验，避免产出被裁剪/缩放错误的截图
        BufferedImage check = ImageIO.read(file);
        int w = check.getWidth();
        int h = check.getHeight();
        if (w != 720 || h != 1280) {
            throw new IllegalStateException("bad png size " + w + "x" + h + " for " + file);
        }
        long bytes = file.length();
        written++;
        System.out.println("[preview] wrote " + file.getPath() + "  " + w + "x" + h
                + "  " + (bytes / 1024) + " KB");
    }

    // ------------------------------------------------------------------
    // 游戏内截图
    // ------------------------------------------------------------------

    private static void prepareGameplay(Game game, int floor) {
        game.startFloor(floor);
        GameScene gs = game.scene instanceof GameScene ? (GameScene) game.scene : null;
        if (gs == null) {
            step(game, 60);
            return;
        }
        World w = gs.world;
        Player p = w.player;

        // 装备与状态：让 HUD 有武器/弹药/钥匙可画（截图用，不影响核心逻辑）
        WeaponType weapon = floor >= 5 ? WeaponType.SHOTGUN : WeaponType.PISTOL;
        p.giveWeapon(WeaponType.PISTOL);
        p.giveWeapon(weapon);
        p.hasKeycard = floor != 10;
        p.item = ItemType.MEDKIT;
        p.itemCount = 2;

        Enemy target = chooseTarget(w, floor);
        if (target != null) {
            placePlayerNear(w, target);
        }
        w.centerCameraOnPlayer();

        if (target != null) {
            float dx = target.x - p.x;
            float dy = target.y - p.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < 1f) d = 1f;
            float nx = dx / d, ny = dy / d;

            // 左摇杆：朝目标走
            game.touchDown(0, Controls.MOVE_DEFAULT_X, Controls.MOVE_DEFAULT_Y);
            game.touchMove(0, Controls.MOVE_DEFAULT_X + nx * Controls.STICK_RADIUS,
                    Controls.MOVE_DEFAULT_Y + ny * Controls.STICK_RADIUS);
            // 右摇杆：瞄准目标（推过 55% 自动开火）
            game.touchDown(1, Controls.AIM_DEFAULT_X, Controls.AIM_DEFAULT_Y);
            game.touchMove(1, Controls.AIM_DEFAULT_X + nx * Controls.STICK_RADIUS,
                    Controls.AIM_DEFAULT_Y + ny * Controls.STICK_RADIUS);
            // 攻击键：按住
            game.touchDown(2, Controls.ATTACK_X, Controls.ATTACK_Y);

            // BOSS 层保持 2~3 格距离（否则和 BOSS 立绘重叠），普通层稍微靠近一点
            step(game, floor == 10 ? 10 : 30);
            game.touchUp(0, Controls.MOVE_DEFAULT_X, Controls.MOVE_DEFAULT_Y);
            stepToMuzzleFlash(game, gs, 120);   // 站定开火，捕到枪口焰/子弹在飞的一帧
        } else {
            step(game, 120);
        }

        polishHud(game, gs, floor);
    }

    /** 逐帧推进，直到主角刚开枪（枪口焰 > 0）或超时。 */
    private static void stepToMuzzleFlash(Game game, GameScene gs, int maxFrames) {
        for (int i = 0; i < maxFrames; i++) {
            game.update(DT);
            if (gs.world.player.muzzleFlash > 0f) {
                // 再补一帧，让子弹离膛、特效出现，枪口焰仍在（约 6 帧寿命）
                game.update(DT);
                return;
            }
        }
    }

    /** 选一个“周围敌人最多”的目标（BOSS 层选 BOSS），保证画面里有战斗对象。 */
    private static Enemy chooseTarget(World w, int floor) {
        if (floor == 10) {
            Enemy boss = w.boss();
            if (boss != null && !boss.isDead()) return boss;
        }
        Enemy best = null;
        int bestScore = -1;
        for (Enemy e : w.enemies) {
            if (e.isDead()) continue;
            int score = 0;
            for (Enemy o : w.enemies) {
                if (o == e || o.isDead()) continue;
                float d = dist(e.x, e.y, o.x, o.y);
                if (d < TileMap.TILE * 6f) score++;
            }
            // 离玩家出生点太近的敌人不选（否则没有“潜入”构图）
            if (dist(e.x, e.y, w.player.x, w.player.y) < TileMap.TILE * 2f) score -= 3;
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    private static void placePlayerNear(World w, Enemy target) {
        Player p = w.player;
        float[][] dirs = {
                {-1f, 0f}, {1f, 0f}, {0f, -1f}, {0f, 1f},
                {-0.7f, -0.7f}, {0.7f, -0.7f}, {-0.7f, 0.7f}, {0.7f, 0.7f},
        };
        for (float distTiles : new float[]{3.2f, 2.8f, 3.6f, 2.4f}) {
            for (float[] d : dirs) {
                float px = target.x + d[0] * distTiles * TileMap.TILE;
                float py = target.y + d[1] * distTiles * TileMap.TILE;
                if (px < 2 * TileMap.TILE || py < 2 * TileMap.TILE) continue;
                if (px > (w.map.width - 2) * TileMap.TILE || py > (w.map.height - 2) * TileMap.TILE) continue;
                if (w.collidesCircle(px, py, p.radius + 6f)) continue;
                p.x = px;
                p.y = py;
                p.angle = (float) Math.atan2(target.y - py, target.x - px);
                p.aimAngle = p.angle;
                return;
            }
        }
        // 兜底：站在目标旁两格
        p.x = target.x;
        p.y = target.y + TileMap.TILE * 2f;
        if (w.collidesCircle(p.x, p.y, p.radius)) {
            p.x = target.x - TileMap.TILE * 2f;
            p.y = target.y;
        }
    }

    private static void polishHud(Game game, GameScene gs, int floor) {
        HudState hud = gs.hud;
        World w = gs.world;
        Player p = w.player;
        // 血量/体力不设成满条，截图更像真实战斗中的一瞬
        if (p.hp > p.maxHp * 0.62f) p.hp = p.maxHp * 0.62f;
        if (p.stamina > p.maxStamina * 0.72f) p.stamina = p.maxStamina * 0.72f;
        hud.lowHealth = p.hp / p.maxHp;
        hud.attackBtn = Math.max(hud.attackBtn, 0.65f);
        hud.attackHeld = true;
        if (hud.moveStickActive) {
            hud.moveStickDx *= 0.75f;
            hud.moveStickDy *= 0.75f;
        }
        if (hud.aimStickActive) {
            float mag = (float) Math.sqrt(hud.aimStickDx * hud.aimStickDx + hud.aimStickDy * hud.aimStickDy);
            if (mag < 0.55f) {
                Enemy target = chooseTarget(w, floor);
                if (target != null) {
                    float ang = (float) Math.atan2(target.y - p.y, target.x - p.x);
                    hud.aimStickDx = (float) Math.cos(ang) * 0.8f;
                    hud.aimStickDy = (float) Math.sin(ang) * 0.8f;
                }
            }
        }
        if (floor == 10) {
            Enemy boss = w.boss();
            if (boss != null && !boss.isDead()) {
                hud.bossHp = boss.hp;
                hud.bossMaxHp = boss.maxHp;
                hud.bossName = boss.type.label;
            }
        }
        if (hud.toast == null) {
            hud.toast = floor == 10 ? Texts.OBJECTIVE_BOSS : Texts.OBJECTIVE_HACK;
            hud.toastTimer = 2.2f;
        }
        hud.interactAvailable = true;
        hud.interactPrompt = floor == 10 ? Texts.PROMPT_ELEVATOR : Texts.PROMPT_HACK;
    }

    private static void prepareHacking(Game game) {
        game.startFloor(3);
        GameScene gs = game.scene instanceof GameScene ? (GameScene) game.scene : null;
        if (gs == null) {
            step(game, 60);
            return;
        }
        step(game, 30);
        boolean started = invokeNoArgHook(gs, "debugStartHack");
        if (!started) {
            started = tryStartHackByInteracting(game, gs);
        }
        if (started) {
            // 让计时器走一点，光标与倒计时更自然
            int guard = 0;
            while (!gs.hud.hacking && guard++ < 30) game.update(DT);
            step(game, 45);
            if (gs.hud.hacking) {
                // 光标朝终点方向挪两格，避免停在起点
                HudState hud = gs.hud;
                hud.hackCursorX = Math.max(hud.hackStartX, hud.hackGoalX / 2);
                hud.hackCursorY = Math.max(hud.hackStartY, hud.hackGoalY / 2);
                return;
            }
        }
        // 兜底：直接填 HudState，只渲染不 update（等引擎提供 debug 钩子/交互入口）
        System.out.println("[preview] WARN: hacking debug hook not available, using synthetic HUD state");
        fillSyntheticHack(gs.hud);
    }

    private static boolean tryStartHackByInteracting(Game game, GameScene gs) {
        World w = gs.world;
        Player p = w.player;
        for (int ty = 0; ty < w.map.height; ty++) {
            for (int tx = 0; tx < w.map.width; tx++) {
                if (w.map.type(tx, ty) != TileType.COMPUTER) continue;
                int[][] adj = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
                for (int[] a : adj) {
                    int ax = tx + a[0], ay = ty + a[1];
                    if (w.map.solid(ax, ay)) continue;
                    p.x = w.map.cx(ax);
                    p.y = w.map.cy(ay);
                    w.centerCameraOnPlayer();
                    game.touchDown(3, Controls.INTERACT_X, Controls.INTERACT_Y);
                    step(game, 4);
                    game.touchUp(3, Controls.INTERACT_X, Controls.INTERACT_Y);
                    if (gs.hud.hacking) return true;
                }
            }
        }
        return false;
    }

    private static boolean invokeNoArgHook(Object target, String name) {
        try {
            Method m = target.getClass().getMethod(name);
            m.setAccessible(true);
            Object res = m.invoke(target);
            boolean ok = !(res instanceof Boolean) || (Boolean) res;
            if (ok) System.out.println("[preview] called " + name + "()");
            return ok;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (Throwable t) {
            System.err.println("[preview] hook " + name + " failed: " + t);
            return false;
        }
    }

    private static void fillSyntheticHack(HudState hud) {
        int cols = 12, rows = 16;
        hud.hacking = true;
        hud.hackCols = cols;
        hud.hackRows = rows;
        hud.hackStartX = 0;
        hud.hackStartY = 0;
        hud.hackGoalX = cols - 1;
        hud.hackGoalY = rows - 1;
        hud.hackCursorX = 5;
        hud.hackCursorY = 6;
        hud.hackTimeTotal = 20f;
        hud.hackTimeLeft = 12.6f;
        hud.hackFailed = false;
        hud.hackSuccess = false;
        hud.hackResultTimer = 0f;
        int[] cells = new int[cols * rows];
        // 简单迷宫：竖向黄色墙 + 少量橙色回退格 / 红黄失败格
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int idx = y * cols + x;
                boolean wall = (x == 3 && y >= 2 && y <= 9)
                        || (x == 6 && y >= 5 && y <= 13)
                        || (x == 9 && y >= 2 && y <= 7);
                if (wall) cells[idx] = 1;
            }
        }
        cells[2 * cols + 2] = 2;
        cells[11 * cols + 8] = 2;
        cells[6 * cols + 4] = 3;
        cells[13 * cols + 10] = 3;
        hud.hackCells = cells;
    }

    private static void preparePause(Game game) {
        game.startFloor(2);
        GameScene gs = game.scene instanceof GameScene ? (GameScene) game.scene : null;
        if (gs != null) {
            step(game, 90);
            game.pause();
            step(game, 20);
            polishHud(game, gs, 2);
        } else {
            step(game, 60);
        }
        // 暂停时真实游戏会把 hud.paused 置位；兜底保证覆盖层出现
        if (gs != null && !gs.hud.paused) gs.hud.paused = true;
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
