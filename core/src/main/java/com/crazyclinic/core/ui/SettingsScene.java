package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.Scene;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Texts;

/** 设置（占位版）。 */
public final class SettingsScene implements Scene {
    private final Rect sound = new Rect();
    private final Rect music = new Rect();
    private final Rect vibrate = new Rect();
    private final Rect reset = new Rect();
    private final Rect back = new Rect();
    private boolean confirmReset;

    @Override
    public void enter(Game game) {
        game.audio.music(com.crazyclinic.core.Music.TITLE);
    }

    @Override
    public void update(Game game, float dt) {
        float x = 90, w = Game.WIDTH - 180, h = 96;
        sound.set(x, 330, w, h);
        music.set(x, 456, w, h);
        vibrate.set(x, 582, w, h);
        reset.set(x, 728, w, h);
        back.set((Game.WIDTH - 420) * 0.5f, 1050, 420, 96);
    }

    @Override
    public void render(Game game, Renderer r) {
        r.clear(Palette.BG_DARK);
        r.setTextAlign(1);
        r.setTextBold(true);
        r.setTextColor(Palette.TEXT);
        r.setTextSize(64);
        r.drawText(Texts.BTN_SETTINGS, Game.WIDTH / 2f, 160);
        row(r, sound, Texts.SET_SOUND, game.save.soundOn);
        row(r, music, Texts.SET_MUSIC, game.save.musicOn);
        row(r, vibrate, Texts.SET_VIBRATE, game.save.vibrateOn);
        r.fillRoundRect(reset.x, reset.y, reset.w, reset.h, 18, Palette.alpha(Palette.RED_DARK, 0.9f));
        r.strokeRoundRect(reset.x, reset.y, reset.w, reset.h, 18, 3f, Palette.PANEL_STROKE);
        r.setTextAlign(1);
        r.setTextBold(true);
        r.setTextSize(36);
        r.setTextColor(Palette.TEXT);
        r.drawText(confirmReset ? Texts.SET_RESET_CONFIRM : Texts.SET_RESET, reset.cx(), reset.cy());
        UiKit.button(r, back, Texts.BTN_BACK, 0);
    }

    private void row(Renderer r, Rect b, String label, boolean on) {
        r.fillRoundRect(b.x, b.y, b.w, b.h, 18, Palette.alpha(Palette.PANEL_ALT, 0.95f));
        r.strokeRoundRect(b.x, b.y, b.w, b.h, 18, 3f, Palette.PANEL_STROKE);
        r.setTextAlign(0);
        r.setTextBold(true);
        r.setTextSize(36);
        r.setTextColor(Palette.TEXT);
        r.drawText(label, b.x + 32, b.cy());
        r.setTextAlign(2);
        r.setTextColor(on ? Palette.MINT : Palette.TEXT_DARK);
        r.drawText(on ? Texts.ON : Texts.OFF, b.right() - 32, b.cy());
    }

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        if (sound.contains(x, y)) {
            game.save.soundOn = !game.save.soundOn;
            game.audio.setSoundEnabled(game.save.soundOn);
            game.saveNow();
            game.audio.play(Sfx.UI_CLICK);
        } else if (music.contains(x, y)) {
            game.save.musicOn = !game.save.musicOn;
            game.audio.setMusicEnabled(game.save.musicOn);
            game.saveNow();
        } else if (vibrate.contains(x, y)) {
            game.save.vibrateOn = !game.save.vibrateOn;
            game.saveNow();
            game.audio.vibrate(30);
        } else if (reset.contains(x, y)) {
            if (!confirmReset) {
                confirmReset = true;
                game.audio.play(Sfx.UI_CLICK);
            } else {
                game.save.unlockedFloor = 1;
                game.save.organsMask = 0;
                game.save.totalKills = 0;
                game.save.deaths = 0;
                game.saveNow();
                confirmReset = false;
                game.audio.play(Sfx.HACK_FAIL);
            }
        } else if (back.contains(x, y)) {
            game.audio.play(Sfx.UI_BACK);
            game.gotoTitle();
        }
    }

    @Override
    public void backPressed(Game game) {
        game.gotoTitle();
    }
}
