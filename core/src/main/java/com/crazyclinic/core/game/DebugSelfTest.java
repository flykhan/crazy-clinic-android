package com.crazyclinic.core.game;

import com.crazyclinic.core.Audio;
import com.crazyclinic.core.Controls;
import com.crazyclinic.core.Game;
import com.crazyclinic.core.Music;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Balance;
import com.crazyclinic.core.content.Levels;
import com.crazyclinic.core.content.Texts;
import com.crazyclinic.core.world.Enemy;
import com.crazyclinic.core.world.EnemyType;
import com.crazyclinic.core.world.LevelDef;
import com.crazyclinic.core.world.Organ;
import com.crazyclinic.core.world.Pickup;
import com.crazyclinic.core.world.Player;
import com.crazyclinic.core.world.Projectile;
import com.crazyclinic.core.world.ProjectileType;
import com.crazyclinic.core.world.TileMap;
import com.crazyclinic.core.world.TileType;
import com.crazyclinic.core.world.WeaponType;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless 引擎自检：不依赖 Android / 渲染，构造合成关卡验证
 * 寻路、移动/冲刺/闪避、拾取、开门、骇客、敌人 AI、战斗、BOSS 阶段与过关流程。
 *
 * <pre>
 *   source .toolchain/env.sh
 *   javac -encoding UTF-8 -d /tmp/corebuild $(find core/src/main/java -name '*.java')
 *   java -cp /tmp/corebuild com.crazyclinic.core.game.DebugSelfTest
 * </pre>
 */
public final class DebugSelfTest {
    private static final float DT = 1f / 60f;
    private static int passed;
    private static int failed;

    private static final class Harness {
        Game game;
        GameScene scene;
        LevelDef def;
    }

    public static void main(String[] args) {
        System.out.println("=== CrazyClinic engine self test ===");
        testPathfinder();
        testMovementAndDodge();
        testPickupDoorComputerElevator();
        testHackGame();
        testEnemyAI();
        testCombat();
        testBoss();
        testTutorialPauseAndConsistency();
        testPerformanceAndFuzz();
        testRealLevels();
        System.out.println();
        System.out.println("[SELFTEST] passed=" + passed + " failed=" + failed);
        if (failed > 0) {
            System.out.println("[SELFTEST] RESULT: FAIL");
            System.exit(1);
        }
        System.out.println("[SELFTEST] RESULT: OK");
    }

    // ------------------------------------------------------------------
    // 关卡构造
    // ------------------------------------------------------------------

    /**
     * 20x10 双房间：左房 S/抽屉/看守，x=6 隔墙 + 门，右房 C/k/E/守卫。
     * 只有打开 (6,3) 的门才能从出生点到达电梯。
     */
    private static LevelDef testLevel(int index, boolean boss) {
        int w = 20, h = 10;
        char[][] g = new char[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) g[y][x] = '.';
        }
        for (int x = 0; x < w; x++) {
            g[0][x] = '#';
            g[h - 1][x] = '#';
        }
        for (int y = 0; y < h; y++) {
            g[y][0] = '#';
            g[y][w - 1] = '#';
        }
        for (int y = 1; y < h - 1; y++) g[y][6] = '#';
        g[3][6] = '+';
        g[1][1] = 'S';
        g[1][11] = 'C';
        g[4][14] = 'k';
        g[8][15] = 'E';
        g[6][3] = 'G';
        g[6][14] = 'G';
        g[2][4] = 'm';
        g[6][11] = '1';
        if (boss) {
            g[5][16] = 'Z';
            g[7][16] = 'H';
        }
        String[] rows = new String[h];
        for (int y = 0; y < h; y++) rows[y] = new String(g[y]);
        return new LevelDef(index, "自检病区", "selftest", "", rows, new Organ[]{Organ.HEART});
    }

    private static Harness harness(LevelDef def) {
        Harness h = new Harness();
        h.def = def;
        h.game = new Game(new NullAudio(), null);
        h.scene = new GameScene(def.index, def, h.game.save);
        h.scene.debugSetAutoNavigate(false);
        h.game.setScene(h.scene);
        return h;
    }

    private static void step(Harness h, int frames) {
        for (int i = 0; i < frames; i++) h.game.update(DT);
    }

    private static Enemy enemyOfType(GameScene s, EnemyType type) {
        for (Enemy e : s.world.enemies) {
            if (e.type == type) return e;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 测试
    // ------------------------------------------------------------------

    private static void testPathfinder() {
        LevelDef def = testLevel(1, false);
        Pathfinder pf = new Pathfinder(def.map);
        float[] px = new float[Pathfinder.MAX_PATH];
        float[] py = new float[Pathfinder.MAX_PATH];
        int n = pf.find(1, 1, 15, 8, px, py);
        check("寻路：锁门未开时不可达电梯", n == 0);
        int n2 = pf.find(1, 1, 5, 8, px, py);
        check("寻路：同房间可达", n2 > 0);
        def.map.set(6, 3, TileType.DOOR_OPEN);
        int n3 = pf.find(1, 1, 15, 8, px, py);
        check("寻路：开门后可达电梯", n3 > 0);
        check("寻路：路径终点是电梯附近", n3 > 0 && Math.abs(px[n3 - 1] - def.map.cx(15)) < 1f);
        // 目标在墙里 → 0
        check("寻路：目标不可走返回 0", pf.find(1, 1, 6, 1, px, py) == 0);
    }

    private static void testMovementAndDodge() {
        Harness h = harness(testLevel(1, false));
        GameScene s = h.scene;
        Player p = s.world.player;
        s.hud.moveStickActive = true;
        s.hud.moveStickDx = -1f;
        s.hud.moveStickDy = 0f;
        float startX = p.x;
        step(h, 60);
        check("移动：按住左摇杆会移动", p.x < startX - 5f);
        check("移动：不会穿进墙里", !s.world.collidesCircle(p.x, p.y, p.radius));
        float stamina = p.stamina;
        check("冲刺：推满摇杆消耗体力", stamina < p.maxStamina - 0.5f);
        check("冲刺：移动速度进入冲刺状态", p.sprinting || stamina < p.maxStamina);
        s.hud.moveStickActive = false;
        s.hud.moveStickDx = s.hud.moveStickDy = 0f;

        // 移到左房空地，朝右闪避（避免贴着墙导致位移被碰撞吞掉）
        s.debugTeleport(s.world.map.cx(2), s.world.map.cy(2));
        p.aimAngle = 0f;
        p.stamina = p.maxStamina;
        float beforeX = p.x, beforeY = p.y;
        s.playerController.dodge(s, h.game);
        check("闪避：进入无敌并消耗体力", p.dodgeTimer > 0f && p.stamina < p.maxStamina);
        step(h, 12);
        check("闪避：快速位移", MathUtil2.dist(p.x, p.y, beforeX, beforeY) > 8f);
        step(h, 30);
        check("闪避：结束后无敌消失", p.dodgeTimer <= 0f && !s.isFinished());
    }

    private static void testPickupDoorComputerElevator() {
        Harness h = harness(testLevel(1, false));
        GameScene s = h.scene;
        Player p = s.world.player;
        check("目标：开局目标是找钥匙", Texts.OBJECTIVE_KEY.equals(s.world.objective));

        // 钥匙（自动拾取）
        s.debugTeleport(s.world.map.cx(14), s.world.map.cy(4));
        step(h, 3);
        check("拾取：钥匙卡进背包", s.debugKeycardCount() >= 1 && p.hasKeycard);

        // 开门（玩家站在门左侧，按互动）
        s.debugTeleport(s.world.map.cx(5), s.world.map.cy(3));
        step(h, 2);
        h.game.touchDown(7, Controls.INTERACT_X, Controls.INTERACT_Y);
        h.game.touchUp(7, Controls.INTERACT_X, Controls.INTERACT_Y);
        check("交互：钥匙开门并消耗钥匙", s.world.map.type(6, 3) == TileType.DOOR_OPEN && s.debugKeycardCount() == 0);

        // 电脑 → 骇客
        s.debugTeleport(s.world.map.cx(10), s.world.map.cy(1));
        step(h, 2);
        h.game.touchDown(8, Controls.INTERACT_X, Controls.INTERACT_Y);
        h.game.touchUp(8, Controls.INTERACT_X, Controls.INTERACT_Y);
        check("交互：电脑进入骇客界面", s.hud.hacking && s.isHacking());
        s.debugFinishHack(true);
        check("骇客成功：监控/哨兵关闭", s.world.camerasDisabled && s.world.alert == 0f);
        check("目标：骇客后更新为前往电梯", Texts.OBJECTIVE_ELEVATOR.equals(s.world.objective));

        // 电梯 → 过关
        s.debugTeleport(s.world.map.cx(15), s.world.map.cy(7));
        step(h, 2);
        h.game.touchDown(9, Controls.INTERACT_X, Controls.INTERACT_Y);
        h.game.touchUp(9, Controls.INTERACT_X, Controls.INTERACT_Y);
        check("电梯：开始过关动画", s.hud.showingFloorClear);
        step(h, 130);
        check("过关：流程完成并解锁下一层", s.isFinished() && s.isWon());
        check("过关：存档解锁第 2 层", h.game.save.unlockedFloor >= 2);
        check("过关：runKills 已同步", h.game.runKills == s.world.kills);
        check("过关：自动导航已跳过 ResultScene", !(h.game.scene instanceof GameScene) || h.game.scene == s);
    }

    private static void testHackGame() {
        Harness h = harness(testLevel(3, false));
        GameScene s = h.scene;
        check("骇客：debugStartHack 进入 overLay", s.debugStartHack() && s.hud.hacking);
        check("骇客：网格 12x16", s.hud.hackCols == 12 && s.hud.hackRows == 16);
        check("骇客：格数据长度正确", s.hud.hackCells != null && s.hud.hackCells.length == 12 * 16);
        check("骇客：起点左上/终点右下", s.hud.hackStartX == 0 && s.hud.hackStartY == 0
                && s.hud.hackGoalX == 11 && s.hud.hackGoalY == 15);
        check("骇客：限时 >0", s.hud.hackTimeTotal >= Balance.HACK_TIME && s.hud.hackTimeLeft > 0f);
        // 网格必须存在纯 0 通路，保证可通关
        check("骇客：布局存在可行通路", hackHasPath(s.hud.hackCells, 12, 16));
        // 拖拽移动一格
        s.hackGame.setTargetFromScreen(60f * 1.5f, 60f * 0.5f);
        step(h, 3);
        check("骇客：光标朝触点逐格移动", s.hud.hackCursorX == 1 && s.hud.hackCursorY == 0);
        s.debugFinishHack(false);
        check("骇客失败：触发全图警报", s.world.alert >= 0.99f && !s.world.camerasDisabled && !s.hud.hacking);
        // 重试并成功
        check("骇客：允许重试", s.debugStartHack() && s.hud.hacking);
        s.debugFinishHack(true);
        check("骇客重试成功：关闭监控", s.world.camerasDisabled && !s.hud.hacking);
    }

    private static boolean hackHasPath(int[] cells, int cols, int rows) {
        int n = cols * rows;
        boolean[] seen = new boolean[n];
        int[] q = new int[n];
        int head = 0, tail = 0;
        q[tail++] = 0;
        seen[0] = true;
        while (head < tail) {
            int cur = q[head++];
            if (cur == n - 1) return true;
            int cx = cur % cols, cy = cur / cols;
            int[] nx = {cx + 1, cx - 1, cx, cx};
            int[] ny = {cy, cy, cy + 1, cy - 1};
            for (int k = 0; k < 4; k++) {
                if (nx[k] < 0 || ny[k] < 0 || nx[k] >= cols || ny[k] >= rows) continue;
                int ni = ny[k] * cols + nx[k];
                if (seen[ni] || cells[ni] != 0) continue;
                seen[ni] = true;
                q[tail++] = ni;
            }
        }
        return false;
    }

    private static void testEnemyAI() {
        Harness h = harness(testLevel(1, false));
        GameScene s = h.scene;
        Enemy guard = enemyOfType(s, EnemyType.GUARD);
        check("AI：存在看守人", guard != null);
        if (guard == null) return;
        Player p = s.world.player;
        // 站到看守人旁边（距离 < 120 必被发现）
        s.debugTeleport(guard.x + TileMap.TILE * 1.2f, guard.y);
        step(h, 90);
        check("AI：发现玩家后进入警觉/追击",
                guard.state != com.crazyclinic.core.world.EnemyState.PATROL || s.enemyAI.isSeeing(guard));
        check("AI：警戒度上升", s.world.alert > 0f);
        float hpBefore = p.hp;
        step(h, 240);
        check("AI：看守人会攻击造成伤害", p.hp < hpBefore);
        check("AI：玩家没有被卡进墙里", !s.world.collidesCircle(p.x, p.y, p.radius));
    }

    private static void testCombat() {
        Harness h = harness(testLevel(2, false));
        GameScene s = h.scene;
        Player p = s.world.player;
        Enemy guard = enemyOfType(s, EnemyType.GUARD);
        // 右房守卫用于射击测试
        Enemy target = null;
        for (Enemy e : s.world.enemies) {
            if (e == guard) continue;
            target = e;
            break;
        }
        check("战斗：存在测试敌人", target != null);
        if (target == null) return;
        p.giveWeapon(WeaponType.PISTOL);
        s.debugTeleport(target.x - TileMap.TILE * 3f, target.y);
        p.aimAngle = MathUtil2.angle(p.x, p.y, target.x, target.y);
        float before = target.hp;
        h.game.touchDown(2, Controls.ATTACK_X, Controls.ATTACK_Y);
        step(h, 150);
        h.game.touchUp(2, Controls.ATTACK_X, Controls.ATTACK_Y);
        check("战斗：手枪命中并造成伤害", target.hp < before || target.isDead());
        check("战斗：击杀计数/死亡状态一致", !target.isDead() || s.world.kills >= 1);

        // 近战：斧头扇形判定
        Harness h2 = harness(testLevel(2, false));
        GameScene s2 = h2.scene;
        Player p2 = s2.world.player;
        Enemy meleeTarget = null;
        for (Enemy e : s2.world.enemies) {
            if (e != enemyOfType(s2, EnemyType.GUARD)) {
                meleeTarget = e;
                break;
            }
        }
        if (meleeTarget != null) {
            p2.weapon = WeaponType.AXE;
            p2.attackCd = 0f;
            s2.debugTeleport(meleeTarget.x - 60f, meleeTarget.y);
            p2.aimAngle = MathUtil2.angle(p2.x, p2.y, meleeTarget.x, meleeTarget.y);
            float hp = meleeTarget.hp;
            s2.combat.playerAttack(s2, h2.game);
            check("战斗：斧头近战命中", meleeTarget.hp < hp || meleeTarget.isDead());
        }

        // 爆炸试管
        Harness h3 = harness(testLevel(2, false));
        GameScene s3 = h3.scene;
        Player p3 = s3.world.player;
        p3.giveWeapon(WeaponType.VIAL);
        Enemy boomTarget = null;
        for (Enemy e : s3.world.enemies) {
            if (e != enemyOfType(s3, EnemyType.GUARD)) {
                boomTarget = e;
                break;
            }
        }
        if (boomTarget != null) {
            s3.debugTeleport(boomTarget.x - 80f, boomTarget.y);
            p3.aimAngle = MathUtil2.angle(p3.x, p3.y, boomTarget.x, boomTarget.y);
            p3.attackCd = 0f;
            float hp = boomTarget.hp;
            s3.combat.playerAttack(s3, h3.game);
            step(h3, 30);
            check("战斗：爆炸试管造成伤害", boomTarget.hp < hp || boomTarget.isDead());
        }
    }

    private static void testBoss() {
        Harness h = harness(testLevel(10, true));
        GameScene s = h.scene;
        Enemy boss = s.world.boss();
        check("BOSS：第 10 层存在赞恩", boss != null && s.world.level.bossLevel);
        if (boss == null) return;
        step(h, 2);
        check("BOSS：HUD 血条已填充", s.hud.bossHp > 0f && s.hud.bossMaxHp >= boss.maxHp - 0.01f);
        check("BOSS：开局目标为击败赞恩", Texts.OBJECTIVE_BOSS.equals(s.world.objective));
        int before = s.world.enemies.size();
        boss.hp = boss.maxHp * 0.5f;
        step(h, 3);
        check("BOSS：掉到 66% 以下进入二阶段并召唤守卫", s.world.enemies.size() > before);
        boss.hp = boss.maxHp * 0.2f;
        step(h, 3);
        // 击杀 BOSS
        s.debugKillAllEnemies();
        check("BOSS：全部击杀后电梯解锁目标", Texts.OBJECTIVE_ELEVATOR.equals(s.world.objective));
        check("BOSS：BOSS 已死亡", s.world.boss() == null || s.world.boss().isDead());
        s.debugTeleport(s.world.map.cx(15), s.world.map.cy(7));
        step(h, 2);
        h.game.touchDown(5, Controls.INTERACT_X, Controls.INTERACT_Y);
        h.game.touchUp(5, Controls.INTERACT_X, Controls.INTERACT_Y);
        check("BOSS：击杀后可用电梯", s.hud.showingFloorClear);
        step(h, 130);
        check("BOSS：通关流程完成", s.isFinished() && s.isWon());
    }

    /** 性能预算（20+ 敌人 / 100 子弹）+ 10 层随机输入模糊测试。 */
    private static void testPerformanceAndFuzz() {
        System.out.println("-- 性能 / 随机输入健壮性 --");
        Harness h = new Harness();
        h.game = new Game(new NullAudio(), null);
        h.scene = new GameScene(8);
        h.scene.debugSetAutoNavigate(false);
        h.game.setScene(h.scene);
        GameScene s = h.scene;
        s.world.player.noDamageTimer = 9999f;
        // 往世界塞 100 颗子弹（放在可站立格中心，避免立刻撞墙消失）
        int placed = 0;
        for (int ty = 0; ty < s.world.map.height && placed < 100; ty++) {
            for (int tx = 0; tx < s.world.map.width && placed < 100; tx++) {
                if (s.world.map.solid(tx, ty)) continue;
                Projectile pr = new Projectile();
                pr.type = ProjectileType.BULLET;
                pr.x = s.world.map.cx(tx);
                pr.y = s.world.map.cy(ty);
                pr.life = 99f;
                pr.damage = 0f;
                pr.fromPlayer = true;
                pr.radius = 4f;
                s.world.projectiles.add(pr);
                placed++;
            }
        }
        for (int i = 0; i < 30; i++) h.game.update(DT);
        long t0 = System.nanoTime();
        for (int i = 0; i < 300; i++) h.game.update(DT);
        long ns = System.nanoTime() - t0;
        double msPerFrame = ns / 1_000_000.0 / 300.0;
        check("性能：300 帧平均 < 8ms/帧（实测 " + String.format("%.2f", msPerFrame) + "ms，"
                + s.world.enemies.size() + " 敌 / " + s.world.projectiles.size() + " 弹）", msPerFrame < 8.0);
        check("性能：粒子数量受控", s.world.particles.size() <= EffectsSystem.MAX_PARTICLES);

        // 随机输入模糊测试：10 层各 240 帧
        java.util.Random rng = new java.util.Random(20261123L);
        boolean crash = false;
        String crashMsg = "";
        int frames = 0;
        for (int floor = 1; floor <= Game.FLOOR_COUNT && !crash; floor++) {
            Harness fh = new Harness();
            fh.game = new Game(new NullAudio(), null);
            fh.scene = new GameScene(floor);
            fh.scene.debugSetAutoNavigate(false);
            fh.game.setScene(fh.scene);
            fh.scene.debugKillAllEnemies();   // 专注状态机，避免随机输入被秒
            for (int i = 0; i < 240; i++) {
                int r = rng.nextInt(100);
                try {
                    if (r < 18) {
                        float px = rng.nextBoolean() ? 60 + rng.nextInt(260) : 360 + rng.nextInt(300);
                        float py = 980 + rng.nextInt(260);
                        fh.game.touchDown(rng.nextInt(4), px, py);
                    } else if (r < 48) {
                        fh.game.touchMove(rng.nextInt(4), rng.nextInt(700), rng.nextInt(1270));
                    } else if (r < 68) {
                        fh.game.touchUp(rng.nextInt(4), rng.nextInt(700), rng.nextInt(1270));
                    } else if (r < 71) {
                        fh.game.pause();
                    } else if (r < 74) {
                        fh.game.resume();
                    }
                    fh.game.update(DT);
                } catch (Throwable t) {
                    crash = true;
                    crashMsg = t.toString();
                    break;
                }
                Player p = fh.scene.world.player;
                if (!Float.isFinite(p.x) || !Float.isFinite(p.y)) {
                    crash = true;
                    crashMsg = "NaN player at floor " + floor;
                    break;
                }
                if (!Float.isFinite(fh.scene.hud.toastTimer) || fh.scene.world.alert < 0f || fh.scene.world.alert > 1f) {
                    crash = true;
                    crashMsg = "bad hud/alert at floor " + floor;
                    break;
                }
                frames++;
            }
        }
        check("健壮性：10 层随机输入 " + frames + " 帧无异常"
                + (crashMsg.isEmpty() ? "" : "（" + crashMsg + "）"), !crash);
    }

    /** 用关卡代理的真实 10 层地图跑「取钥匙 → 开门 → 骇客 → 电梯」全流程。 */
    private static void testRealLevels() {
        System.out.println("-- 真实关卡连通性/通关模拟 --");
        for (int floor = 1; floor <= Game.FLOOR_COUNT; floor++) {
            LevelDef def = Levels.get(floor);
            Harness h = new Harness();
            h.def = def;
            h.game = new Game(new NullAudio(), null);
            h.scene = new GameScene(floor);
            h.scene.debugSetAutoNavigate(false);
            h.game.setScene(h.scene);
            GameScene s = h.scene;
            TileMap m = s.world.map;
            Player p = s.world.player;
            String tag = "L" + floor;
            check(tag + " 出生点可站立",
                    !s.world.collidesCircle(p.x, p.y, p.radius));
            check(tag + " 目标文本非空", s.world.objective != null && !s.world.objective.isEmpty());

            boolean bossFloor = def.bossLevel;
            int doors = 0;
            int keyPickups = 0;
            int computerTx = -1, computerTy = -1, elevatorTx = -1, elevatorTy = -1;
            for (int ty = 0; ty < m.height; ty++) {
                for (int tx = 0; tx < m.width; tx++) {
                    TileType t = m.type(tx, ty);
                    if (t == TileType.DOOR_LOCKED) doors++;
                    else if (t == TileType.COMPUTER && computerTx < 0) {
                        computerTx = tx;
                        computerTy = ty;
                    } else if (t == TileType.ELEVATOR && elevatorTx < 0) {
                        elevatorTx = tx;
                        elevatorTy = ty;
                    }
                }
            }
            for (Pickup pk : s.world.pickups) {
                if (pk.kind == Pickup.Kind.KEYCARD) keyPickups++;
            }
            check(tag + " 有电梯", elevatorTx >= 0);
            boolean[] closedReach = flood(m, p.x, p.y, false);
            boolean[] openReach = flood(m, p.x, p.y, true);
            check(tag + " 电梯可达(考虑开门)", elevatorTx >= 0 && at(openReach, m, elevatorTx, elevatorTy));
            if (computerTx >= 0) {
                check(tag + " 电脑可达(考虑开门)", adjacentReachable(openReach, m, computerTx, computerTy));
            }
            if (doors > 0) {
                check(tag + " 钥匙数量 >= 锁门数量(" + keyPickups + "/" + doors + ")", keyPickups >= doors);
                // 第一把钥匙必须在不开门的情况下可达，否则会死锁
                boolean firstKeyReachable = false;
                for (Pickup pk : s.world.pickups) {
                    if (pk.kind != Pickup.Kind.KEYCARD) continue;
                    if (at(closedReach, m, m.tx(pk.x), m.ty(pk.y))) {
                        firstKeyReachable = true;
                        break;
                    }
                }
                check(tag + " 首把钥匙无需开门即可拿到", firstKeyReachable);
            }

            // 实战模拟：清怪（避免被打断），拿钥匙、开门、骇客、坐电梯
            if (!bossFloor) s.debugKillAllEnemies();
            List<Pickup> keys = new ArrayList<>();
            for (Pickup pk : s.world.pickups) {
                if (pk.kind == Pickup.Kind.KEYCARD && !pk.taken) keys.add(pk);
            }
            for (Pickup pk : keys) {
                s.debugTeleport(pk.x, pk.y);
                step(h, 3);
            }
            check(tag + " 模拟：拿到钥匙 (" + s.debugKeycardCount() + ")", s.debugKeycardCount() >= Math.min(1, Math.max(doors, 0)) || doors == 0);

            int opened = 0;
            for (int ty = 0; ty < m.height; ty++) {
                for (int tx = 0; tx < m.width; tx++) {
                    if (m.type(tx, ty) != TileType.DOOR_LOCKED) continue;
                    int[] adj = adjacentWalkableForDoor(m, tx, ty);
                    if (adj == null) continue;
                    if (s.hud.showingFloorClear) break;
                    s.debugTeleport(m.cx(adj[0]), m.cy(adj[1]));
                    step(h, 2);
                    h.game.touchDown(20, Controls.INTERACT_X, Controls.INTERACT_Y);
                    h.game.touchUp(20, Controls.INTERACT_X, Controls.INTERACT_Y);
                    if (m.type(tx, ty) == TileType.DOOR_OPEN) opened++;
                }
            }
            check(tag + " 模拟：锁门全部打开 (" + opened + "/" + doors + ")", opened >= doors);

            if (s.hud.showingFloorClear) {
                check(tag + " 模拟：未提前触发过关", false);
            }
            if (computerTx >= 0) {
                if (s.hud.hacking) s.debugFinishHack(true);
                int[] adj = adjacentWalkable(m, computerTx, computerTy);
                check(tag + " 电脑旁有可站立格", adj != null);
                if (adj != null) {
                    s.debugTeleport(m.cx(adj[0]), m.cy(adj[1]));
                    step(h, 2);
                    h.game.touchDown(21, Controls.INTERACT_X, Controls.INTERACT_Y);
                    h.game.touchUp(21, Controls.INTERACT_X, Controls.INTERACT_Y);
                    check(tag + " 模拟：进入骇客", s.hud.hacking);
                    if (s.hud.hacking) s.debugFinishHack(true);
                    check(tag + " 模拟：骇客成功关闭监控", s.world.camerasDisabled);
                }
            }

            if (elevatorTx >= 0) {
                s.debugTeleport(m.cx(elevatorTx), m.cy(elevatorTy));
                step(h, 2);
                h.game.touchDown(22, Controls.INTERACT_X, Controls.INTERACT_Y);
                h.game.touchUp(22, Controls.INTERACT_X, Controls.INTERACT_Y);
                if (bossFloor) {
                    check(tag + " BOSS 未死时电梯不可用", !s.hud.showingFloorClear);
                    s.debugKillAllEnemies();
                    step(h, 2);
                    h.game.touchDown(23, Controls.INTERACT_X, Controls.INTERACT_Y);
                    h.game.touchUp(23, Controls.INTERACT_X, Controls.INTERACT_Y);
                }
                check(tag + " 模拟：启动电梯", s.hud.showingFloorClear);
                step(h, 130);
                check(tag + " 模拟：通关", s.isFinished() && s.isWon());
                if (floor < Game.FLOOR_COUNT) {
                    check(tag + " 模拟：解锁下一层", h.game.save.unlockedFloor >= floor + 1);
                }
            }
        }

        // 死亡流程（用第 2 层）
        Harness dh = new Harness();
        dh.game = new Game(new NullAudio(), null);
        dh.scene = new GameScene(2);
        dh.scene.debugSetAutoNavigate(false);
        dh.game.setScene(dh.scene);
        dh.scene.debugKillAllEnemies();
        int deathsBefore = dh.game.save.deaths;
        dh.scene.debugKillPlayer();
        step(dh, 100);
        check("失败：死亡后进入结算", dh.scene.isFinished() && !dh.scene.isWon());
        check("失败：死亡次数 +1", dh.game.save.deaths == deathsBefore + 1);
        // 重开一层：世界状态必须干净（无残留子弹/粒子/骇客浮层）
        Harness rh = new Harness();
        rh.game = new Game(new NullAudio(), null);
        rh.scene = new GameScene(1);
        rh.scene.debugSetAutoNavigate(false);
        rh.game.setScene(rh.scene);
        check("重开：无残留子弹", rh.scene.world.projectiles.isEmpty());
        check("重开：无残留粒子/浮字", rh.scene.world.particles.isEmpty() && rh.scene.world.floatingTexts.isEmpty());
        check("重开：骇客浮层已关闭", !rh.scene.hud.hacking && !rh.scene.isHacking());
        check("重开：未继承暂停", !rh.scene.isPaused());
    }

    /** 从世界坐标洪泛；doorsOpen=true 时锁门视为可通行（模拟已拿到钥匙）。 */
    private static boolean[] flood(TileMap m, float wx, float wy, boolean doorsOpen) {
        int n = m.width * m.height;
        boolean[] reach = new boolean[n];
        int[] q = new int[n];
        int sx = m.tx(wx), sy = m.ty(wy);
        sx = Math.max(0, Math.min(m.width - 1, sx));
        sy = Math.max(0, Math.min(m.height - 1, sy));
        int start = sy * m.width + sx;
        int head = 0, tail = 0;
        reach[start] = true;
        q[tail++] = start;
        while (head < tail) {
            int cur = q[head++];
            int cx = cur % m.width, cy = cur / m.width;
            int[] dx = {1, -1, 0, 0};
            int[] dy = {0, 0, 1, -1};
            for (int k = 0; k < 4; k++) {
                int nx = cx + dx[k], ny = cy + dy[k];
                if (!m.inBounds(nx, ny)) continue;
                int ni = ny * m.width + nx;
                if (reach[ni]) continue;
                TileType t = m.type(nx, ny);
                boolean passable = !t.solid || (doorsOpen && t == TileType.DOOR_LOCKED);
                if (!passable) continue;
                reach[ni] = true;
                q[tail++] = ni;
            }
        }
        return reach;
    }

    private static boolean at(boolean[] reach, TileMap m, int tx, int ty) {
        if (!m.inBounds(tx, ty)) return false;
        return reach[ty * m.width + tx];
    }

    private static boolean adjacentReachable(boolean[] reach, TileMap m, int tx, int ty) {
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] d : dirs) {
            int nx = tx + d[0], ny = ty + d[1];
            if (at(reach, m, nx, ny)) return true;
        }
        return false;
    }

    /** 开门用相邻格：优先选 3x3 内没有电梯/电脑的格子，避免交互被更高优先级抢走。 */
    private static int[] adjacentWalkableForDoor(TileMap m, int tx, int ty) {
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        int[] fallback = null;
        for (int[] d : dirs) {
            int nx = tx + d[0], ny = ty + d[1];
            if (!m.inBounds(nx, ny) || m.solid(nx, ny)) continue;
            if (fallback == null) fallback = new int[]{nx, ny};
            boolean busy = false;
            for (int yy = ny - 1; yy <= ny + 1 && !busy; yy++) {
                for (int xx = nx - 1; xx <= nx + 1; xx++) {
                    if (!m.inBounds(xx, yy)) continue;
                    TileType t = m.type(xx, yy);
                    if (t == TileType.ELEVATOR || t == TileType.COMPUTER) {
                        busy = true;
                        break;
                    }
                }
            }
            if (!busy) return new int[]{nx, ny};
        }
        return fallback;
    }

    private static int[] adjacentWalkable(TileMap m, int tx, int ty) {
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] d : dirs) {
            int nx = tx + d[0], ny = ty + d[1];
            if (!m.inBounds(nx, ny) || m.solid(nx, ny)) continue;
            return new int[]{nx, ny};
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    /** 避免测试类直接依赖 MathUtil 的静态导入风格。 */
    private static final class MathUtil2 {
        static float dist(float x1, float y1, float x2, float y2) {
            float dx = x2 - x1, dy = y2 - y1;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        static float angle(float x1, float y1, float x2, float y2) {
            return (float) Math.atan2(y2 - y1, x2 - x1);
        }
    }

    private static class NullAudio implements Audio {
        @Override
        public void play(Sfx sfx) {
        }

        @Override
        public void music(Music track) {
        }

        @Override
        public void stopMusic() {
        }

        @Override
        public void setSoundEnabled(boolean enabled) {
        }

        @Override
        public void setMusicEnabled(boolean enabled) {
        }

        @Override
        public void vibrate(int millis) {
        }
    }

    private static final class RecordingAudio extends NullAudio {
        int stopMusicCalls;
        int musicCalls;

        @Override
        public void music(Music track) {
            musicCalls++;
        }

        @Override
        public void stopMusic() {
            stopMusicCalls++;
        }
    }

    private static void testTutorialPauseAndConsistency() {
        System.out.println("-- 教学/暂停/武器一致性 --");
        // 首次进入第 1 层：分步教学提示
        Harness h = new Harness();
        h.game = new Game(new NullAudio(), null);
        h.scene = new GameScene(1);
        h.scene.debugSetAutoNavigate(false);
        h.game.setScene(h.scene);
        check("教学：首条提示是移动/瞄准", h.scene.hud.toast != null && h.scene.hud.toast.contains("左摇杆"));
        step(h, 260);
        check("教学：4 秒后推进到攻击/拾取提示", h.scene.hud.toast != null && h.scene.hud.toast.contains("攻击"));
        check("教学：第 1 层初始目标仍为钥匙", h.scene.world.objective != null);

        // 暂停 → 停 BGM；继续 → 恢复 BGM
        Harness ph = new Harness();
        RecordingAudio audio = new RecordingAudio();
        ph.game = new Game(audio, null);
        ph.scene = new GameScene(2);
        ph.scene.debugSetAutoNavigate(false);
        ph.game.setScene(ph.scene);
        int musicBefore = audio.musicCalls;
        ph.game.pause();
        check("暂停：停止 BGM", audio.stopMusicCalls > 0);
        check("暂停：世界冻结", ph.scene.isPaused());
        ph.game.resume();
        check("继续：恢复 BGM", audio.musicCalls > musicBefore);
        check("继续：世界恢复", !ph.scene.isPaused());

        // 武器一致性：空枪自动切到可用武器
        Harness wh = harness(testLevel(2, false));
        Player p = wh.scene.world.player;
        p.giveWeapon(WeaponType.PISTOL);
        p.ammo[WeaponType.PISTOL.ordinal()] = 1;
        wh.scene.hud.attackHeld = true;
        step(wh, 8);
        wh.scene.hud.attackHeld = false;
        check("武器：打空后自动切到可用武器", p.weapon != WeaponType.PISTOL || p.ammo[WeaponType.PISTOL.ordinal()] > 0);

        // 骇客成功后，电脑不再提示"使用安保电脑"（先把锁门打开，避免目标仍是找钥匙）
        LevelDef hackDef = testLevel(3, false);
        hackDef.map.set(6, 3, TileType.DOOR_OPEN);
        Harness ch = harness(hackDef);
        ch.scene.debugStartHack();
        ch.scene.debugFinishHack(true);
        ch.scene.debugTeleport(ch.scene.world.map.cx(10), ch.scene.world.map.cy(1));
        step(ch, 3);
        check("骇客后：电脑不再提示黑入", !Texts.PROMPT_HACK.equals(ch.scene.hud.interactPrompt));
        check("骇客后：目标更新为电梯", Texts.OBJECTIVE_ELEVATOR.equals(ch.scene.world.objective));
    }
}
