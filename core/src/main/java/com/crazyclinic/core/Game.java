package com.crazyclinic.core;

import com.crazyclinic.core.game.GameScene;
import com.crazyclinic.core.ui.AboutScene;
import com.crazyclinic.core.ui.IntroScene;
import com.crazyclinic.core.ui.LevelSelectScene;
import com.crazyclinic.core.ui.ResultScene;
import com.crazyclinic.core.ui.SettingsScene;
import com.crazyclinic.core.ui.TitleScene;
import com.crazyclinic.core.ui.TutorialScene;

/**
 * 全局游戏上下文：持有存档、音频、当前场景，负责场景切换与输入分发。
 * 逻辑分辨率 720x1280：上方 720x960 为世界视口，下方 320 为触屏控制区。
 */
public final class Game {
    public static final float WIDTH = 720f;
    public static final float HEIGHT = 1280f;
    public static final float VIEW_H = 960f;
    public static final float CONTROLS_Y = 960f;
    public static final int FLOOR_COUNT = 10;

    public final SaveData save;
    public final Audio audio;
    public final SaveStore store;
    public Renderer renderer;
    public Scene scene;
    public float time;
    public float fade;
    public int currentFloor;
    public int runKills;
    public float runTime;

    public Game(Audio audio, SaveStore store) {
        this.audio = audio;
        this.store = store;
        this.save = SaveData.parse(store == null ? null : store.load());
        audio.setSoundEnabled(save.soundOn);
        audio.setMusicEnabled(save.musicOn);
    }

    public void update(float dt) {
        if (dt > 0.1f) dt = 0.1f;
        time += dt;
        if (scene != null) scene.update(this, dt);
    }

    public void render(Renderer r) {
        this.renderer = r;
        if (scene != null) scene.render(this, r);
        if (fade > 0.003f) {
            r.fillRect(0f, 0f, WIDTH, HEIGHT, Palette.alpha(Palette.BLACK, Math.min(1f, fade)));
        }
    }

    public void setScene(Scene s) {
        scene = s;
        if (s != null) s.enter(this);
    }

    public void touchDown(int id, float x, float y) {
        if (scene != null) scene.touchDown(this, id, x, y);
    }

    public void touchMove(int id, float x, float y) {
        if (scene != null) scene.touchMove(this, id, x, y);
    }

    public void touchUp(int id, float x, float y) {
        if (scene != null) scene.touchUp(this, id, x, y);
    }

    public void backPressed() {
        if (scene != null) scene.backPressed(this);
    }

    public void pause() {
        if (scene != null) scene.pause(this);
    }

    public void resume() {
        if (scene != null) scene.resume(this);
    }

    // ---------- 场景导航 ----------
    public void gotoTitle() {
        currentFloor = 0;
        setScene(new TitleScene());
    }

    public void gotoLevelSelect() {
        setScene(new LevelSelectScene());
    }

    public void gotoIntro(int floor) {
        currentFloor = floor;
        setScene(new IntroScene(floor));
    }

    public void startFloor(int floor) {
        currentFloor = floor;
        runKills = 0;
        runTime = 0f;
        setScene(new GameScene(floor));
    }

    public void gotoResult(boolean win, int floor, int kills, float elapsed) {
        setScene(new ResultScene(win, floor, kills, elapsed));
    }

    public void gotoSettings() {
        setScene(new SettingsScene());
    }

    public void gotoAbout() {
        setScene(new AboutScene());
    }

    public void gotoTutorial() {
        setScene(new TutorialScene());
    }

    public boolean isFloorUnlocked(int floor) {
        return floor >= 1 && floor <= save.unlockedFloor && floor <= FLOOR_COUNT;
    }

    public void unlockFloor(int floor) {
        if (floor > save.unlockedFloor) save.unlockedFloor = Math.min(FLOOR_COUNT, floor);
    }

    public void saveNow() {
        if (store == null) return;
        try {
            store.save(save.serialize());
        } catch (Exception ignored) {
        }
    }
}
