package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.Scene;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Levels;
import com.crazyclinic.core.content.Texts;
import com.crazyclinic.core.world.LevelDef;

/** 关卡前剧情/简报（占位版）。 */
public final class IntroScene implements Scene {
    private final int floor;
    private final Rect start = new Rect();
    private final Rect back = new Rect();

    public IntroScene(int floor) {
        this.floor = floor;
    }

    @Override
    public void enter(Game game) {
        game.audio.music(com.crazyclinic.core.Music.TITLE);
    }

    @Override
    public void update(Game game, float dt) {
        start.set((Game.WIDTH - 420) * 0.5f, 1010, 420, 100);
        back.set(60, 1140, 240, 82);
    }

    @Override
    public void render(Game game, Renderer r) {
        r.clear(Palette.BG_DARK);
        LevelDef def = Levels.get(floor);
        r.fillVerticalGradient(0, 0, Game.WIDTH, Game.HEIGHT, Palette.BG_DEEP, Palette.BG_DARK);
        r.setTextAlign(1);
        r.setTextBold(true);
        r.setTextColor(Palette.MINT);
        r.setTextSize(30);
        r.drawText("第 " + floor + " 层 / " + Game.FLOOR_COUNT, Game.WIDTH / 2f, 150);
        r.setTextSize(86);
        r.setTextColor(Palette.TEXT);
        r.drawText(def.name, Game.WIDTH / 2f, 262);
        r.setTextSize(32);
        r.setTextColor(Palette.TEXT_DIM);
        r.drawText(def.subtitle, Game.WIDTH / 2f, 340);

        r.fillRoundRect(70, 420, Game.WIDTH - 140, 480, 28, Palette.alpha(Palette.PANEL, 0.95f));
        r.strokeRoundRect(70, 420, Game.WIDTH - 140, 480, 28, 3f, Palette.PANEL_STROKE);
        UiKit.wrapped(r, def.intro, 110, 480, Game.WIDTH - 220, 34, Palette.TEXT, 16);
        UiKit.button(r, start, Texts.BTN_START, 1);
        UiKit.button(r, back, Texts.BTN_BACK, 0);
    }

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        if (start.contains(x, y)) {
            game.audio.play(Sfx.UI_CONFIRM);
            game.startFloor(floor);
        } else if (back.contains(x, y)) {
            game.audio.play(Sfx.UI_BACK);
            game.gotoLevelSelect();
        }
    }

    @Override
    public void backPressed(Game game) {
        game.gotoLevelSelect();
    }
}
