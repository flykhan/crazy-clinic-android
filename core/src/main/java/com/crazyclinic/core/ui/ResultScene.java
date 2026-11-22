package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.Scene;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Texts;

/** 过关/失败结算（占位版）。 */
public final class ResultScene implements Scene {
    private final boolean win;
    private final int floor;
    private final int kills;
    private final float elapsed;
    private final Rect primary = new Rect();
    private final Rect secondary = new Rect();

    public ResultScene(boolean win, int floor, int kills, float elapsed) {
        this.win = win;
        this.floor = floor;
        this.kills = kills;
        this.elapsed = elapsed;
    }

    @Override
    public void enter(Game game) {
        if (win) {
            game.audio.play(Sfx.FLOOR_WIN);
            game.audio.music(com.crazyclinic.core.Music.WIN);
            game.unlockFloor(floor + 1);
            game.save.totalKills += kills;
            game.saveNow();
        } else {
            game.audio.play(Sfx.FLOOR_LOSE);
            game.audio.music(com.crazyclinic.core.Music.LOSE);
            game.save.deaths++;
            game.saveNow();
        }
    }

    @Override
    public void update(Game game, float dt) {
        float w = 420, h = 100;
        primary.set((Game.WIDTH - w) * 0.5f, 860, w, h);
        secondary.set((Game.WIDTH - w) * 0.5f, 986, w, h);
    }

    @Override
    public void render(Game game, Renderer r) {
        r.clear(win ? Palette.BG_DEEP : Palette.BG_DARK);
        r.setTextAlign(1);
        r.setTextBold(true);
        r.setTextColor(win ? Palette.MINT : Palette.RED);
        r.setTextSize(96);
        r.drawText(win ? Texts.WIN : Texts.LOSE, Game.WIDTH / 2f, 280);
        r.setTextSize(32);
        r.setTextBold(false);
        r.setTextColor(Palette.TEXT_DIM);
        UiKit.wrapped(r, win ? Texts.WIN_DESC : Texts.LOSE_DESC, 90, 400, Game.WIDTH - 180, 32, Palette.TEXT_DIM, 12);
        r.setTextSize(34);
        r.setTextColor(Palette.TEXT);
        r.setTextAlign(1);
        r.drawText(Texts.STAT_KILLS + "  " + kills, Game.WIDTH / 2f, 580);
        r.drawText(Texts.STAT_TIME + "  " + UiKit.formatTime(elapsed), Game.WIDTH / 2f, 640);
        UiKit.button(r, primary, win ? (floor >= Game.FLOOR_COUNT ? Texts.BTN_MENU : Texts.BTN_NEXT) : Texts.BTN_RETRY, 1);
        UiKit.button(r, secondary, Texts.BTN_MENU, 0);
    }

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        if (primary.contains(x, y)) {
            game.audio.play(Sfx.UI_CONFIRM);
            if (win) {
                if (floor >= Game.FLOOR_COUNT) game.gotoTitle();
                else game.gotoIntro(floor + 1);
            } else {
                game.startFloor(floor);
            }
        } else if (secondary.contains(x, y)) {
            game.audio.play(Sfx.UI_BACK);
            game.gotoLevelSelect();
        }
    }
}
