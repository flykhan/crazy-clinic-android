package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.Scene;
import com.crazyclinic.core.content.Levels;
import com.crazyclinic.core.content.Texts;
import com.crazyclinic.core.world.LevelDef;

/** 关卡选择（占位版）。 */
public final class LevelSelectScene implements Scene {
    private final Rect[] cards = new Rect[Game.FLOOR_COUNT];
    private final Rect back = new Rect();

    @Override
    public void enter(Game game) {
        game.audio.music(com.crazyclinic.core.Music.TITLE);
    }

    @Override
    public void update(Game game, float dt) {
        float cw = 300, ch = 180, gapX = 30, gapY = 26;
        float startX = (Game.WIDTH - cw * 2 - gapX) * 0.5f;
        for (int i = 0; i < cards.length; i++) {
            int col = i % 2, row = i / 2;
            cards[i] = new Rect(startX + col * (cw + gapX), 260 + row * (ch + gapY), cw, ch);
        }
        back.set(60, 1130, 240, 90);
    }

    @Override
    public void render(Game game, Renderer r) {
        r.clear(Palette.BG_DARK);
        r.setTextAlign(1);
        r.setTextBold(true);
        r.setTextColor(Palette.TEXT);
        r.setTextSize(64);
        r.drawText(Texts.BTN_LEVELS, Game.WIDTH / 2f, 130);
        r.setTextSize(26);
        r.setTextColor(Palette.TEXT_DIM);
        r.drawText("已解锁 " + game.save.unlockedFloor + " / " + Game.FLOOR_COUNT + " 层", Game.WIDTH / 2f, 190);
        for (int i = 0; i < cards.length; i++) {
            Rect c = cards[i];
            boolean unlocked = game.isFloorUnlocked(i + 1);
            LevelDef def = Levels.FLOORS[i];
            int fill = unlocked ? Palette.PANEL_ALT : Palette.PANEL;
            r.fillRoundRect(c.x, c.y, c.w, c.h, 20, Palette.alpha(fill, 0.96f));
            r.strokeRoundRect(c.x, c.y, c.w, c.h, 20, 3f, unlocked ? Palette.MINT : Palette.PANEL_STROKE);
            r.setTextAlign(0);
            r.setTextBold(true);
            r.setTextSize(34);
            r.setTextColor(unlocked ? Palette.TEXT : Palette.TEXT_DARK);
            r.drawText((i + 1) + " · " + def.name, c.x + 22, c.y + 52);
            r.setTextSize(24);
            r.setTextBold(false);
            r.setTextColor(unlocked ? Palette.TEXT_DIM : Palette.TEXT_DARK);
            r.drawText(def.subtitle, c.x + 22, c.y + 100);
            r.setTextSize(22);
            r.drawText(unlocked ? "点击进入" : "未解锁", c.x + 22, c.y + 148);
        }
        UiKit.button(r, back, Texts.BTN_BACK, 0);
    }

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        if (back.contains(x, y)) {
            game.audio.play(com.crazyclinic.core.Sfx.UI_BACK);
            game.gotoTitle();
            return;
        }
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null && cards[i].contains(x, y) && game.isFloorUnlocked(i + 1)) {
                game.audio.play(com.crazyclinic.core.Sfx.UI_CONFIRM);
                game.gotoIntro(i + 1);
                return;
            }
        }
    }

    @Override
    public void backPressed(Game game) {
        game.gotoTitle();
    }
}
