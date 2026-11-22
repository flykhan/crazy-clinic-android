package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.Scene;
import com.crazyclinic.core.content.Texts;

/** 操作教学（占位版）。 */
public final class TutorialScene implements Scene {
    private int page;
    private final Rect next = new Rect();
    private final Rect back = new Rect();

    @Override
    public void update(Game game, float dt) {
        next.set((Game.WIDTH - 420) * 0.5f, 1000, 420, 100);
        back.set(60, 1130, 240, 82);
    }

    @Override
    public void render(Game game, Renderer r) {
        r.clear(Palette.BG_DARK);
        r.setTextAlign(1);
        r.setTextBold(true);
        r.setTextColor(Palette.MINT);
        r.setTextSize(56);
        r.drawText("操作说明", Game.WIDTH / 2f, 150);
        r.setTextSize(26);
        r.setTextColor(Palette.TEXT_DIM);
        r.drawText((page + 1) + " / " + Texts.TUTORIAL_PAGES.length, Game.WIDTH / 2f, 205);
        r.setTextAlign(0);
        UiKit.wrapped(r, Texts.TUTORIAL_PAGES[page], 70, 300, Game.WIDTH - 140, 34, Palette.TEXT, 18);
        UiKit.button(r, next, page < Texts.TUTORIAL_PAGES.length - 1 ? "下一页" : "知道了", 1);
        UiKit.button(r, back, "跳过", 0);
    }

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        if (next.contains(x, y)) {
            page++;
            if (page >= Texts.TUTORIAL_PAGES.length) {
                game.save.tutorialSeen = true;
                game.saveNow();
                game.gotoTitle();
            }
        } else if (back.contains(x, y)) {
            game.save.tutorialSeen = true;
            game.saveNow();
            game.gotoTitle();
        }
    }
}
