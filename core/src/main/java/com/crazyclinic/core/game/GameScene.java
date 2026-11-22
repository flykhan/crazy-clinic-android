package com.crazyclinic.core.game;

import com.crazyclinic.core.Controls;
import com.crazyclinic.core.Game;
import com.crazyclinic.core.HudState;
import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Music;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.SaveData;
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
import com.crazyclinic.core.world.Player;
import com.crazyclinic.core.world.TileMap;
import com.crazyclinic.core.world.TileType;
import com.crazyclinic.core.world.WeaponType;
import com.crazyclinic.core.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * 关卡主场景：把玩家控制、AI、战斗、交互、骇客小游戏、相机/反馈与关卡流程串起来。
 *
 * <p>渲染完全委托给 {@link WorldRenderer} / {@link HudRenderer}（美术代理实现）；
 * 只有暂停面板由本类自己绘制（契约允许）。</p>
 */
public final class GameScene implements Scene {
    public final int floor;
    public final World world;
    public final HudState hud = new HudState();

    protected final WorldRenderer worldRenderer = new WorldRenderer();
    protected final HudRenderer hudRenderer = new HudRenderer();

    // 子系统
    final EffectsSystem effects = new EffectsSystem();
    final CombatSystem combat = new CombatSystem();
    final EnemyAI enemyAI;
    final Interactions interactions;
    final PlayerController playerController = new PlayerController();
    final HackGame hackGame = new HackGame();

    // 流程状态
    float elapsed;
    boolean finished;
    boolean paused;
    boolean dying;
    float dyingTimer;
    boolean autoNavigate = true;
    boolean won;
    boolean organsPreapplied;

    // 关卡进度
    int keycards;
    int lockedDoorsRemaining;
    boolean hasComputer;
    boolean hasElevator;
    String levelName = "";

    // 临时效果计时
    float energyTimer;
    float pendingEpSlow;
    float dodgeDirX, dodgeDirY;
    int tipIndex;
    float tipTimer;
    boolean tutorialMode;
    boolean tipKeyShown;
    boolean tipHackShown;
    boolean alarmLatched;
    Music currentMusic;

    private Game gameRef;

    // 触屏指针
    private int movePointer = -1, aimPointer = -1;
    private int attackPointer = -1, interactPointer = -1, dodgePointer = -1;
    private int weaponPointer = -1, itemPointer = -1;

    // 暂停面板按钮
    private static final float PANEL_X = 100f, PANEL_Y = 380f, PANEL_W = 520f, PANEL_H = 520f;
    private static final Rect CONTINUE_RECT = new Rect(160f, 600f, 400f, 92f);
    private static final Rect QUIT_RECT = new Rect(160f, 716f, 400f, 92f);

    /** 过关动画时长（hud.floorClearTimer 从 0 递增到该值，art 的电梯门/白场动画按此排布）。 */
    private static final float FLOOR_CLEAR_TIME = 1.8f;

    /** 每层原始地图快照：LevelDef 的 TileMap 是静态共享的，开门会污染下一局，这里做还原。 */
    private static final Map<LevelDef, byte[]> PRISTINE_MAPS = new HashMap<>();

    // ==================================================================
    // 构造
    // ==================================================================

    public GameScene(int floor) {
        this(floor, null, null);
    }

    public GameScene(int floor, SaveData save) {
        this(floor, null, save);
    }

    GameScene(int floor, LevelDef def, SaveData save) {
        this.floor = Math.max(1, floor);
        LevelDef d = def != null ? def : Levels.get(this.floor);
        if (d == null) {
            // 极端兜底：内容缺失时给一张最小地图，保证场景不崩
            d = new LevelDef(this.floor, "未知病区", "", "", fallbackRows(), new Organ[]{Organ.HEART});
        }
        this.levelName = d.name == null ? "" : d.name;
        this.world = new World(d, save);
        this.organsPreapplied = save != null;
        restorePristineMap(d);
        scanLevel();
        this.enemyAI = new EnemyAI(world.map);
        this.enemyAI.bind(world);
        this.interactions = new Interactions(world.map);
    }

    private static String[] fallbackRows() {
        String[] rows = new String[8];
        for (int y = 0; y < rows.length; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < 8; x++) {
                boolean edge = x == 0 || y == 0 || x == 7 || y == 7;
                sb.append(edge ? '#' : '.');
            }
            rows[y] = sb.toString();
        }
        rows[1] = "#S.....#";
        rows[6] = "#.....E#";
        return rows;
    }

    private void scanLevel() {
        hasComputer = false;
        hasElevator = false;
        lockedDoorsRemaining = 0;
        TileMap m = world.map;
        for (int ty = 0; ty < m.height; ty++) {
            for (int tx = 0; tx < m.width; tx++) {
                TileType t = m.type(tx, ty);
                if (t == TileType.COMPUTER) hasComputer = true;
                else if (t == TileType.ELEVATOR) hasElevator = true;
                else if (t == TileType.DOOR_LOCKED) lockedDoorsRemaining++;
            }
        }
    }

    private void restorePristineMap(LevelDef def) {
        TileMap m = world.map;
        byte[] pristine = PRISTINE_MAPS.get(def);
        if (pristine == null || pristine.length != m.width * m.height) {
            byte[] snap = new byte[m.width * m.height];
            for (int ty = 0; ty < m.height; ty++) {
                for (int tx = 0; tx < m.width; tx++) {
                    snap[ty * m.width + tx] = m.raw(tx, ty);
                }
            }
            PRISTINE_MAPS.put(def, snap);
            return;
        }
        for (int ty = 0; ty < m.height; ty++) {
            for (int tx = 0; tx < m.width; tx++) {
                byte b = pristine[ty * m.width + tx];
                m.set(tx, ty, TileType.values()[b & 0xff]);
            }
        }
    }

    // ==================================================================
    // 生命周期
    // ==================================================================

    @Override
    public void enter(Game game) {
        gameRef = game;
        hud.reset();
        hud.toast = null;
        hud.toastTimer = 0f;
        hud.damageFlash = 0f;
        hud.healFlash = 0f;
        hud.hackCells = null;
        hud.hacking = false;
        hud.bossHp = 0f;
        hud.showingFloorClear = false;
        hud.floorClearTimer = 0f;
        hud.interactPrompt = null;
        hud.interactAvailable = false;
        hud.attackHeld = false;
        combat.reset();
        elapsed = 0f;
        finished = false;
        paused = false;
        dying = false;
        dyingTimer = 0f;
        won = false;
        alarmLatched = false;
        tipIndex = 0;
        tipTimer = 0f;
        tipKeyShown = false;
        tipHackShown = false;
        energyTimer = 0f;
        pendingEpSlow = 0f;
        dodgeDirX = dodgeDirY = 0f;
        keycards = 0;
        movePointer = aimPointer = attackPointer = interactPointer = dodgePointer = weaponPointer = itemPointer = -1;

        if (!organsPreapplied && game != null && game.save != null) {
            applySaveOrgans(game.save);
        }
        Player p = world.player;
        p.hp = p.maxHp;
        p.stamina = p.maxStamina;
        p.dead = false;
        p.dodgeTimer = 0f;
        p.invuln = 0f;
        p.attackCd = 0f;
        p.hurtFlash = 0f;
        p.item = null;
        p.itemCount = 0;
        p.hasKeycard = false;
        p.weapon = bestStartingWeapon(p);
        p.aimAngle = p.angle;
        world.centerCameraOnPlayer();
        world.time = 0f;
        world.kills = 0;
        world.won = false;
        world.lost = false;
        world.alert = 0f;
        hud.lowHealth = MathUtil.clamp(1f - p.hp / p.maxHp, 0f, 1f);

        if (game != null) {
            game.runKills = 0;
            game.runTime = 0f;
            currentMusic = world.level.bossLevel ? Music.BOSS : Music.FLOOR;
            game.audio.music(currentMusic);
        }
        // 第 1 层首次进入：分步教学（移动/瞄准 → 攻击/拾取 → 钥匙 → 骇客 → 电梯）
        tutorialMode = floor == 1 && (game == null || game.save == null || !game.save.tutorialSeen);
        updateObjective();
        updateBossHud();
        showFloorIntro(game);
    }

    private static WeaponType bestStartingWeapon(Player p) {
        if (p.hasWeapon(WeaponType.AXE)) return WeaponType.AXE;
        if (p.hasWeapon(WeaponType.FIST)) return WeaponType.FIST;
        return WeaponType.AXE;
    }

    private void applySaveOrgans(SaveData save) {
        Player p = world.player;
        for (Organ o : Organ.values()) {
            if (save.hasOrgan(o)) {
                p.applyOrganBonus(o);
                if (o == Organ.ARMS) p.armsBonus = true;
            }
        }
        organsPreapplied = true;
    }

    private void showFloorIntro(Game game) {
        if (floor == 1 && tutorialMode) {
            showToast("左摇杆移动 · 右摇杆瞄准/开火", 2.8f);
        } else if (floor == 1) {
            showToast("第 1 层 · " + levelName + "：找钥匙 → 黑电脑 → 坐电梯", 3.2f);
        } else if (floor == 10) {
            showToast("天台 · 击败赞恩后启动电梯", 3.2f);
        } else {
            showToast("第 " + floor + " 层 · " + levelName, 3f);
        }
    }

    // ==================================================================
    // 每帧更新
    // ==================================================================

    @Override
    public void update(Game game, float dt) {
        if (game == null) return;
        gameRef = game;
        if (dt <= 0f) return;
        if (dt > 0.05f) dt = 0.05f;   // 防御：大跳帧时限制单步

        tickHud(game, dt);
        if (paused) {
            hud.attackHeld = false;
            return;
        }
        if (finished) return;

        elapsed += dt;
        game.runTime = elapsed;
        game.runKills = world.kills;

        if (hud.showingFloorClear) {
            updateFloorClear(game, dt);
            return;
        }
        if (dying) {
            updateDying(game, dt);
            return;
        }
        if (hud.hacking) {
            hackGame.update(this, game, dt);
            world.time += dt;
            updateCamera(dt);
            updateMusic(game);
            return;
        }

        world.time += dt;
        playerController.update(this, game, dt);
        enemyAI.update(this, game, dt);
        combat.update(this, game, dt);
        interactions.updatePrompt(this, game);
        interactions.updatePickups(this, game, dt);
        effects.update(world, dt);
        updateAlert(game, dt);
        updateCamera(dt);
        updateObjective();
        updateBossHud();
        updateTips(game, dt);
        updateMusic(game);
        if (world.player.hp <= 0f && !dying) beginDeath(game);
    }

    private void tickHud(Game game, float dt) {
        hud.paused = paused;
        hud.toastTimer = Math.max(0f, hud.toastTimer - dt);
        if (hud.toastTimer <= 0f) hud.toast = null;
        hud.damageFlash = Math.max(0f, hud.damageFlash - dt * 2.6f);
        hud.healFlash = Math.max(0f, hud.healFlash - dt * 2.0f);
        hud.attackBtn = hud.attackHeld ? 1f : Math.max(0f, hud.attackBtn - dt * 5f);
        hud.interactBtn = Math.max(0f, hud.interactBtn - dt * 4f);
        hud.dodgeBtn = Math.max(0f, hud.dodgeBtn - dt * 4f);
        hud.weaponBtn = Math.max(0f, hud.weaponBtn - dt * 4f);
        hud.itemBtn = Math.max(0f, hud.itemBtn - dt * 4f);
        Player p = world.player;
        if (p != null && p.maxHp > 0f) {
            // 与 Lead 占位版/preview 约定一致：0..1 的血量比例，低于 0.3 表示危险
            hud.lowHealth = MathUtil.clamp(p.hp / p.maxHp, 0f, 1f);
        }
    }

    private void updateFloorClear(Game game, float dt) {
        hud.floorClearTimer = Math.min(FLOOR_CLEAR_TIME, hud.floorClearTimer + dt);
        hud.attackHeld = false;
        effects.update(world, dt);
        updateCamera(dt);
        if (hud.floorClearTimer >= FLOOR_CLEAR_TIME) {
            hud.showingFloorClear = false;
            completeWin(game);
        }
    }

    private void updateDying(Game game, float dt) {
        dyingTimer -= dt;
        hud.attackHeld = false;
        effects.update(world, dt);
        updateCamera(dt);
        if (dyingTimer <= 0f) completeLoss(game);
    }

    private void updateCamera(float dt) {
        Player p = world.player;
        float tx = p.x - World.VIEW_W * 0.5f;
        float ty = p.y - World.VIEW_H * 0.5f;
        world.cameraX = MathUtil.approach(world.cameraX, tx, 11f, dt);
        world.cameraY = MathUtil.approach(world.cameraY, ty, 11f, dt);
        world.clampCamera();
        world.shake = Math.max(0f, world.shake - dt * 2.4f);
        if (world.shake > 1f) world.shake = 1f;
    }

    private void updateAlert(Game game, float dt) {
        if (!enemyAI.anySees) {
            world.alert = Math.max(0f, world.alert - Balance.ALERT_DECAY * 8f * dt);
        }
        world.alert = MathUtil.clamp(world.alert, 0f, 1f);
        if (world.alert >= 0.999f && !alarmLatched) {
            alarmLatched = true;
            enemyAI.alarmAll(this);
            showToast("警报！所有敌人都被惊动了", 3f);
            hud.damageFlash = Math.max(hud.damageFlash, 0.4f);
            if (game != null) {
                game.audio.play(Sfx.ALARM);
                if (game.save != null && game.save.vibrateOn) game.audio.vibrate(70);
            }
        } else if (world.alert < 0.35f) {
            alarmLatched = false;
        }
    }

    private void updateObjective() {
        if (world.level.bossLevel) {
            Enemy boss = world.boss();
            if (boss != null && !boss.isDead()) {
                world.objective = Texts.OBJECTIVE_BOSS;
            } else {
                world.objective = Texts.OBJECTIVE_ELEVATOR;
            }
            return;
        }
        if (lockedDoorsRemaining > 0 && !hasKeycard()) {
            world.objective = Texts.OBJECTIVE_KEY;
            return;
        }
        if (hasComputer && !world.camerasDisabled) {
            world.objective = Texts.OBJECTIVE_HACK;
            return;
        }
        world.objective = Texts.OBJECTIVE_ELEVATOR;
    }

    private void updateBossHud() {
        Enemy boss = world.boss();
        if (boss != null && !boss.isDead()) {
            hud.bossHp = boss.hp;
            hud.bossMaxHp = Math.max(1f, boss.maxHp);
            hud.bossName = boss.type.label;
        } else {
            hud.bossHp = 0f;
        }
    }

    private void updateTips(Game game, float dt) {
        if (floor != 1 || !tutorialMode) return;
        tipTimer += dt;
        if (tipIndex == 0 && tipTimer > 4.0f) {
            tipIndex = 1;
            showToast("「攻击」按住连发 · 靠近物品自动拾取", 2.8f);
        } else if (tipIndex == 1 && tipTimer > 8.5f) {
            tipIndex = 2;
            showToast("找到钥匙卡(k)，靠近红色锁门按「互动」", 3f);
        } else if (tipIndex == 2 && tipTimer > 14.5f) {
            tipIndex = 3;
            showToast("黑入安保电脑(C) 关掉监控，再去电梯(E)", 3f);
        } else if (tipIndex == 3 && tipTimer > 21f) {
            tipIndex = 4;
            showToast("推满左摇杆冲刺 · 「闪避」可躲子弹", 2.8f);
        }
    }

    private void updateMusic(Game game) {
        if (game == null) return;
        Music want;
        if (world.level.bossLevel) {
            Enemy boss = world.boss();
            want = (boss != null && !boss.isDead()) ? Music.BOSS : Music.FLOOR;
        } else {
            want = world.alert > 0.55f ? Music.TENSE : Music.FLOOR;
        }
        if (want != currentMusic) {
            currentMusic = want;
            game.audio.music(want);
        }
    }

    // ==================================================================
    // 胜负流程
    // ==================================================================

    void beginDeath(Game game) {
        if (dying || finished) return;
        dying = true;
        dyingTimer = 1.4f;
        world.lost = true;
        Player p = world.player;
        p.hp = 0f;
        p.dead = true;
        hud.damageFlash = 1f;
        hud.attackHeld = false;
        resetSticks();
        if (game != null) {
            game.audio.play(Sfx.PLAYER_DIE);
            game.audio.music(Music.LOSE);
            if (game.save != null && game.save.vibrateOn) game.audio.vibrate(180);
        }
    }

    void completeLoss(Game game) {
        if (finished) return;
        finished = true;
        if (game != null && game.save != null) {
            game.save.deaths++;
            game.save.totalKills += world.kills;
            game.save.totalPlaySeconds += (int) elapsed;
            if (floor == 1) game.save.tutorialSeen = true;
            game.saveNow();
        }
        if (autoNavigate && game != null) {
            game.gotoResult(false, floor, world.kills, elapsed);
        }
    }

    void startFloorClear(Game game) {
        if (hud.showingFloorClear || finished || dying) return;
        hud.showingFloorClear = true;
        hud.floorClearTimer = 0f;
        world.won = true;
        hud.attackHeld = false;
        resetSticks();
        Player p = world.player;
        effects.text(world, "逃脱成功", p.x, p.y - 64, Palette.GOLD);
        if (game != null) {
            game.audio.play(Sfx.ELEVATOR);
            game.audio.play(Sfx.FLOOR_WIN);
            game.audio.music(Music.WIN);
        }
    }

    void completeWin(Game game) {
        if (finished) return;
        finished = true;
        won = true;
        if (game != null) {
            game.unlockFloor(floor + 1);
            if (game.save != null) {
                game.save.totalKills += world.kills;
                game.save.totalPlaySeconds += (int) elapsed;
                if (floor == 1) game.save.tutorialSeen = true;
                game.saveNow();
            }
            if (autoNavigate) game.gotoResult(true, floor, world.kills, elapsed);
        }
    }

    // ==================================================================
    // 骇客小游戏回调
    // ==================================================================

    void startHack(Game game, boolean debug) {
        if (hud.hacking || finished || dying || paused) return;
        if (!debug) resetSticks();
        hud.toast = null;
        hud.toastTimer = 0f;
        hackGame.start(this, game);
    }

    void applyHackSuccess(Game game) {
        world.camerasDisabled = true;
        world.alert = 0f;
        alarmLatched = false;
        updateObjective();
        if (tutorialMode && !tipHackShown) {
            tipHackShown = true;
            showToast("监控关闭！去电梯（E）按「互动」", 3f);
        } else {
            showToast(Texts.HACK_SUCCESS + "：监控与哨兵枪已瘫痪", 3f);
        }
        hud.healFlash = 0.5f;
        if (game != null) game.audio.play(Sfx.HACK_SUCCESS);
    }

    void applyHackFail(Game game) {
        world.alert = 1f;
        alarmLatched = true;
        enemyAI.alarmAll(this);
        showToast(Texts.HACK_FAIL, 3f);
        hud.damageFlash = 0.7f;
        world.shake = Math.max(world.shake, 0.45f);
        if (game != null) {
            game.audio.play(Sfx.ALARM);
            if (game.save != null && game.save.vibrateOn) game.audio.vibrate(80);
        }
    }

    void onHackClosed(Game game) {
        if (!world.camerasDisabled) {
            showToast("骇客失败。别灰心，回到电脑旁可以再试一次。", 3f);
        }
        updateObjective();
    }

    // ==================================================================
    // 交互回调（Interactions 调用）
    // ==================================================================

    int keycards() {
        return keycards;
    }

    boolean hasKeycard() {
        return keycards > 0 || world.player.hasKeycard;
    }

    void addKeycard() {
        keycards++;
        world.player.hasKeycard = true;
        if (tutorialMode && !tipKeyShown) {
            tipKeyShown = true;
            showToast("拿到钥匙卡！去开红色锁门（+）", 3f);
        }
    }

    boolean consumeKeycard() {
        if (keycards > 0) {
            keycards--;
            if (keycards <= 0) world.player.hasKeycard = false;
            return true;
        }
        if (world.player.hasKeycard) {
            world.player.hasKeycard = false;
            return true;
        }
        return false;
    }

    void onDoorOpened() {
        if (lockedDoorsRemaining > 0) lockedDoorsRemaining--;
        updateObjective();
    }

    void showToast(String text, float seconds) {
        if (text == null) return;
        hud.toast = text;
        hud.toastTimer = Math.max(0.4f, seconds);
    }

    void resetSticks() {
        movePointer = -1;
        aimPointer = -1;
        attackPointer = -1;
        interactPointer = -1;
        dodgePointer = -1;
        weaponPointer = -1;
        itemPointer = -1;
        hud.moveStickActive = false;
        hud.aimStickActive = false;
        hud.moveStickDx = hud.moveStickDy = 0f;
        hud.aimStickDx = hud.aimStickDy = 0f;
        hud.attackHeld = false;
    }

    // ==================================================================
    // 触屏
    // ==================================================================

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        gameRef = game;
        if (hud.hacking) {
            hackGame.setTargetFromScreen(x, y);
            return;
        }
        if (paused) {
            handlePauseTouch(game, x, y);
            return;
        }
        if (finished || dying || hud.showingFloorClear) return;

        if (Controls.inWorld(y)) {
            if (Controls.hit(x, y, Controls.PAUSE_X, Controls.PAUSE_Y, Controls.PAUSE_R)) {
                setPaused(game, true);
                if (game != null) game.audio.play(Sfx.UI_CLICK);
            }
            return;
        }

        // 右侧按钮
        if (Controls.hit(x, y, Controls.ATTACK_X, Controls.ATTACK_Y, Controls.ATTACK_R)) {
            attackPointer = id;
            hud.attackHeld = true;
            hud.attackBtn = 1f;
            return;
        }
        if (Controls.hit(x, y, Controls.INTERACT_X, Controls.INTERACT_Y, Controls.INTERACT_R)) {
            interactPointer = id;
            hud.interactBtn = 1f;
            interactions.interact(this, game);
            return;
        }
        if (Controls.hit(x, y, Controls.DODGE_X, Controls.DODGE_Y, Controls.DODGE_R)) {
            dodgePointer = id;
            hud.dodgeBtn = 1f;
            playerController.dodge(this, game);
            return;
        }
        if (inRect(x, y, Controls.WEAPON_X, Controls.WEAPON_Y, Controls.WEAPON_W, Controls.WEAPON_H)) {
            weaponPointer = id;
            hud.weaponBtn = 1f;
            playerController.cycleWeapon(this, game);
            return;
        }
        if (inRect(x, y, Controls.ITEM_X, Controls.ITEM_Y, Controls.ITEM_W, Controls.ITEM_H)) {
            itemPointer = id;
            hud.itemBtn = 1f;
            playerController.useItem(this, game);
            return;
        }

        // 摇杆：左半屏移动 / 右半屏瞄准
        if (x < Controls.MOVE_ZONE_MAX_X) {
            if (movePointer != -1) return;
            movePointer = id;
            hud.moveStickActive = true;
            hud.moveStickCx = x;
            hud.moveStickCy = y;
            hud.moveStickDx = 0f;
            hud.moveStickDy = 0f;
        } else {
            if (aimPointer != -1) return;
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
        if (hud.hacking) {
            hackGame.setTargetFromScreen(x, y);
            return;
        }
        if (paused) return;
        if (id == movePointer) {
            setStickDelta(x, y, true);
        } else if (id == aimPointer) {
            setStickDelta(x, y, false);
        }
    }

    private void setStickDelta(float x, float y, boolean move) {
        float cx = move ? hud.moveStickCx : hud.aimStickCx;
        float cy = move ? hud.moveStickCy : hud.aimStickCy;
        float dx = x - cx, dy = y - cy;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        float max = Math.max(1f, Controls.STICK_RADIUS);
        if (d > max) {
            dx = dx / d * max;
            dy = dy / d * max;
        }
        if (move) {
            hud.moveStickDx = dx / max;
            hud.moveStickDy = dy / max;
        } else {
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
        }
        if (id == aimPointer) {
            aimPointer = -1;
            hud.aimStickActive = false;
            hud.aimStickDx = hud.aimStickDy = 0f;
        }
        if (id == attackPointer) {
            attackPointer = -1;
            hud.attackHeld = false;
        }
        if (id == interactPointer) {
            interactPointer = -1;
            hud.interactBtn = 0f;
        }
        if (id == dodgePointer) {
            dodgePointer = -1;
            hud.dodgeBtn = 0f;
        }
        if (id == weaponPointer) {
            weaponPointer = -1;
            hud.weaponBtn = 0f;
        }
        if (id == itemPointer) {
            itemPointer = -1;
            hud.itemBtn = 0f;
        }
    }

    private static boolean inRect(float x, float y, float rx, float ry, float rw, float rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    // ==================================================================
    // 暂停
    // ==================================================================

    private void setPaused(Game game, boolean value) {
        paused = value;
        hud.paused = value;
        if (game != null) {
            // 暂停（含切后台）时停止 BGM，继续时按当前氛围恢复
            if (value) {
                game.audio.stopMusic();
            } else if (currentMusic != null) {
                game.audio.music(currentMusic);
            }
        }
        if (value) {
            hud.attackHeld = false;
            hud.moveStickActive = false;
            hud.aimStickActive = false;
            hud.moveStickDx = hud.moveStickDy = 0f;
            hud.aimStickDx = hud.aimStickDy = 0f;
            movePointer = aimPointer = attackPointer = -1;
        }
    }

    private void handlePauseTouch(Game game, float x, float y) {
        if (CONTINUE_RECT.contains(x, y)) {
            setPaused(game, false);
            if (game != null) game.audio.play(Sfx.UI_CLICK);
        } else if (QUIT_RECT.contains(x, y)) {
            if (game != null) {
                game.audio.play(Sfx.UI_BACK);
                game.gotoLevelSelect();
            }
        }
    }

    @Override
    public void backPressed(Game game) {
        if (finished || dying || hud.showingFloorClear) return;
        if (hud.hacking) {
            hackGame.cancel();
            hud.hacking = false;
            hud.hackCells = null;
            showToast("离开骇客界面", 1.2f);
            return;
        }
        setPaused(game, !paused);
        if (game != null) game.audio.play(Sfx.UI_CLICK);
    }

    @Override
    public void pause(Game game) {
        setPaused(game, true);
    }

    @Override
    public void resume(Game game) {
        setPaused(game, false);
    }

    // ==================================================================
    // 渲染
    // ==================================================================

    @Override
    public void render(Game game, Renderer r) {
        worldRenderer.render(game, r, world);
        hudRenderer.render(game, r, world, hud);
        if (paused) renderPausePanel(r);
    }

    private void renderPausePanel(Renderer r) {
        r.fillRect(0f, 0f, Game.WIDTH, Game.HEIGHT, Palette.alpha(Palette.BLACK, 0.66f));
        r.fillRoundRect(PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 28f, Palette.alpha(Palette.PANEL, 0.98f));
        r.strokeRoundRect(PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 28f, 3f, Palette.PANEL_STROKE);

        r.setTextAlign(1);
        r.setTextBold(true);
        r.setTextColor(Palette.TEXT);
        r.setTextSize(54f);
        r.drawText(Texts.PAUSED, Game.WIDTH / 2f, PANEL_Y + 72f);
        r.setTextBold(false);
        r.setTextSize(24f);
        r.setTextColor(Palette.TEXT_DIM);
        r.drawText("第 " + floor + " 层 · " + levelName, Game.WIDTH / 2f, PANEL_Y + 128f);
        if (world.objective != null && !world.objective.isEmpty()) {
            r.setTextSize(22f);
            r.setTextColor(Palette.MINT);
            r.drawText(world.objective, Game.WIDTH / 2f, PANEL_Y + 166f);
        }

        drawPauseButton(r, CONTINUE_RECT, Texts.BTN_RESUME, Palette.PANEL_ALT, Palette.MINT);
        drawPauseButton(r, QUIT_RECT, Texts.BTN_QUIT_FLOOR, Palette.alpha(Palette.RED_DARK, 0.85f), Palette.TEXT);

        r.setTextAlign(1);
        r.setTextSize(20f);
        r.setTextColor(Palette.TEXT_DARK);
        r.drawText("按返回键也可继续", Game.WIDTH / 2f, PANEL_Y + PANEL_H - 30f);
    }

    private void drawPauseButton(Renderer r, Rect rect, String label, int fill, int textColor) {
        r.fillRoundRect(rect.x, rect.y, rect.w, rect.h, 20f, fill);
        r.strokeRoundRect(rect.x, rect.y, rect.w, rect.h, 20f, 2f, Palette.PANEL_STROKE);
        r.setTextAlign(1);
        r.setTextBold(true);
        r.setTextSize(32f);
        r.setTextColor(textColor);
        r.drawText(label, rect.x + rect.w / 2f, rect.y + rect.h / 2f);
        r.setTextBold(false);
    }

    // ==================================================================
    // 调试 / QA 钩子（预览工具与自动化会用）
    // ==================================================================

    /**
     * 直接开始骇客小游戏（不需要站在电脑旁）。预览工具通过反射调用；返回是否成功进入。
     */
    public boolean debugStartHack() {
        if (finished || dying || hud.showingFloorClear) return false;
        if (hud.hacking) return true;
        startHack(gameRef, true);
        return hud.hacking;
    }

    /** 调试：立即判定当前骇客小游戏成败（成功会关闭监控/哨兵）。 */
    public void debugFinishHack(boolean success) {
        if (!hud.hacking) return;
        hackGame.debugFinish(this, gameRef, success);
    }

    /** 调试：把玩家传送到世界坐标，自动修正到最近的可站立点。 */
    public void debugTeleport(float wx, float wy) {
        Player p = world.player;
        if (p == null || world.map == null) return;
        float x = MathUtil.clamp(wx, p.radius, Math.max(p.radius, world.map.pixelWidth() - p.radius));
        float y = MathUtil.clamp(wy, p.radius, Math.max(p.radius, world.map.pixelHeight() - p.radius));
        if (world.collidesCircle(x, y, p.radius)) {
            boolean found = false;
            for (int ring = 1; ring <= 6 && !found; ring++) {
                for (int i = 0; i < 8 && !found; i++) {
                    float a = (float) (i * Math.PI / 4.0);
                    float nx = x + (float) Math.cos(a) * TileMap.TILE * ring;
                    float ny = y + (float) Math.sin(a) * TileMap.TILE * ring;
                    if (nx < p.radius || ny < p.radius
                            || nx > world.map.pixelWidth() - p.radius
                            || ny > world.map.pixelHeight() - p.radius) continue;
                    if (!world.collidesCircle(nx, ny, p.radius)) {
                        x = nx;
                        y = ny;
                        found = true;
                    }
                }
            }
            if (!found) {
                x = world.level.spawnX;
                y = world.level.spawnY;
            }
        }
        p.x = x;
        p.y = y;
        world.centerCameraOnPlayer();
    }

    /** 调试：击杀全部敌人（含 BOSS），用于 QA 快速推进。 */
    public void debugKillAllEnemies() {
        for (int i = 0; i < world.enemies.size(); i++) {
            Enemy e = world.enemies.get(i);
            if (e == null || e.isDead()) continue;
            combat.killEnemy(this, gameRef, e);
        }
        world.alert = 0f;
        alarmLatched = false;
        updateObjective();
    }

    /** 调试：立即触发过关动画（可用于截图）。 */
    public void debugWin() {
        startFloorClear(gameRef);
    }

    /** 调试：立刻完成本层（跳过过关动画）。 */
    public void debugCompleteWin() {
        completeWin(gameRef);
    }

    /** 调试：立即死亡（QA 验证失败流程）。 */
    public void debugKillPlayer() {
        Player p = world.player;
        p.invuln = 0f;
        p.dodgeTimer = 0f;
        p.noDamageTimer = 0f;
        combat.damagePlayer(this, gameRef, p.hp + 1f, p.x - 10f, p.y, false);
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isWon() {
        return won;
    }

    public boolean isHacking() {
        return hud.hacking;
    }

    public boolean isPaused() {
        return paused;
    }

    public int debugKeycardCount() {
        return keycards;
    }

    /** 调试：关闭自动跳转（headless 自检时防止切换到 ResultScene）。 */
    public void debugSetAutoNavigate(boolean value) {
        this.autoNavigate = value;
    }
}
