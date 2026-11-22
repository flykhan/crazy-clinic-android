package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.Music;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.Scene;
import com.crazyclinic.core.content.Texts;

/** 标题界面（占位实现，等 UI 代理美化）。 */
public final class TitleScene implements Scene {
    private final Rect[] buttons = new Rect[5];
    private float t;

    @Override
    public void enter(Game game) {
        game.audio.music(Music.TITLE);
    }

    @Override
    public void update(Game game, float dt) {
        t += dt;
        float w = 520, h = 96, x = (Game.WIDTH - w) * 0.5f, y = 560;
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new Rect(x, y + i * (h + 26), w, h);
        }
    }

    @Override
    public void render(Game game, Renderer r) {
        r.clear(Palette.BG_DARK);
        r.fillVerticalGradient(0, 0, Game.WIDTH, 520, Palette.BG_DEEP, Palette.BG_DARK);
        r.setTextAlign(1);
        r.setTextBold(true);
        r.setTextColor(Palette.MINT);
        r.setTextSize(112);
        r.drawText(Texts.TITLE, Game.WIDTH / 2f, 240);
        r.setTextSize(40);
        r.setTextColor(Palette.TEXT_DIM);
        r.drawText(Texts.SUBTITLE, Game.WIDTH / 2f, 330);
        r.setTextSize(26);
        r.drawText("J2ME 经典重制 · " + Texts.VERSION, Game.WIDTH / 2f, 386);

        r.save();
        r.translate(Game.WIDTH / 2f, 468);
        r.rotate((float) Math.sin(t * 1.7) * 3f);
        r.fillRoundRect(-150, -40, 300, 80, 18, Palette.alpha(Palette.RED_DARK, 0.85f));
        r.setTextSize(30);
        r.setTextBold(false);
        r.setTextColor(Palette.TEXT);
        r.setTextAlign(1);
        r.drawText("⚠ 逃出疯狂医院", 0, 0);
        r.restore();

        String[] labels = {Texts.BTN_NEW_GAME, Texts.BTN_CONTINUE, Texts.BTN_LEVELS, Texts.BTN_SETTINGS, Texts.BTN_ABOUT};
        int[] styles = {1, 0, 0, 0, 0};
        for (int i = 0; i < labels.length; i++) {
            UiKit.button(r, buttons[i], labels[i], styles[i]);
        }
        r.setTextSize(22);
        r.setTextColor(Palette.TEXT_DARK);
        r.setTextAlign(1);
        r.drawText("程序化重绘 · 致敬 HandyGames《Crazy Hospital》", Game.WIDTH / 2f, 1200);
    }

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        float w = 520, h = 96, bx = (Game.WIDTH - w) * 0.5f, by = 560;
        for (int i = 0; i < 5; i++) {
            if (new Rect(bx, by + i * (h + 26), w, h).contains(x, y)) {
                game.audio.play(com.crazyclinic.core.Sfx.UI_CONFIRM);
                switch (i) {
                    case 0: game.gotoIntro(1); break;
                    case 1: game.gotoIntro(Math.max(1, game.save.unlockedFloor)); break;
                    case 2: game.gotoLevelSelect(); break;
                    case 3: game.gotoSettings(); break;
                    default: game.gotoAbout(); break;
                }
                return;
            }
        }
    }
}
