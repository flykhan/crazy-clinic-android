package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.Scene;
import com.crazyclinic.core.content.Texts;

/** 关于/版权声明（占位版）。 */
public final class AboutScene implements Scene {
    private final Rect back = new Rect();

    @Override
    public void update(Game game, float dt) {
        back.set((Game.WIDTH - 420) * 0.5f, 1120, 420, 92);
    }

    @Override
    public void render(Game game, Renderer r) {
        r.clear(Palette.BG_DARK);
        r.setTextAlign(1);
        r.setTextBold(true);
        r.setTextColor(Palette.MINT);
        r.setTextSize(60);
        r.drawText(Texts.BTN_ABOUT, Game.WIDTH / 2f, 130);
        UiKit.wrapped(r, Texts.ABOUT_BODY, 80, 210, Game.WIDTH - 160, 30, Palette.TEXT_DIM, 10);
        UiKit.button(r, back, Texts.BTN_BACK, 0);
    }

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        if (back.contains(x, y)) game.gotoTitle();
    }

    @Override
    public void backPressed(Game game) {
        game.gotoTitle();
    }
}
