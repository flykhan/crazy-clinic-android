package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.Music;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.Scene;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Texts;

/**
 * 关于：原版版权声明、同人重制声明、技术栈、参与代号、版本信息。
 */
public final class AboutScene implements Scene {
    private final Rect back = new Rect(34f, 72f, 76f, 76f);
    private static final Rect ORIGINAL = new Rect(48f, 368f, 624f, 190f);
    private static final Rect NOTICE = new Rect(48f, 578f, 624f, 190f);
    private static final Rect TECH = new Rect(48f, 788f, 624f, 152f);
    private static final Rect CREDITS = new Rect(48f, 960f, 624f, 122f);
    private static final int[] CREDIT_COLORS = {
            UiKit.GOLD_LIGHT, Palette.MINT, Palette.PURPLE, Palette.CYAN, Palette.GREEN, Palette.BLUE
    };

    private float time;
    private float reveal;
    private int pointer = -1;
    private boolean backPressed;

    @Override
    public void enter(Game game) {
        game.audio.music(Music.TITLE);
        game.fade = 1f;
        time = 0f;
        reveal = 0f;
        pointer = -1;
        backPressed = false;
    }

    @Override
    public void update(Game game, float dt) {
        time += dt;
        UiKit.tickFade(game, dt);
        reveal = Math.min(1f, reveal + dt * 2.2f);
    }

    @Override
    public void render(Game game, Renderer r) {
        UiBackdrop.menu(r, time, Palette.RED, "ABOUT");
        UiKit.header(r, Texts.BTN_ABOUT, null, time);
        UiKit.backCircle(r, back, backPressed, true);

        float alpha = 0.25f + 0.75f * reveal;
        r.save();
        r.setAlpha(alpha);

        // 徽章 + 标题
        UiKit.emblem(r, UiKit.W * 0.5f, 214f, 36f, time);
        r.setTextSize(46f);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(Palette.TEXT);
        r.drawText(Texts.TITLE, UiKit.W * 0.5f, 292f);
        r.setTextSize(22f);
        r.setTextBold(false);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.95f));
        r.drawText(MenuTexts.ABOUT_SUB + "  ·  " + Texts.VERSION, UiKit.W * 0.5f, 330f);

        drawSection(r, ORIGINAL, MenuTexts.ABOUT_ORIGINAL_TITLE, MenuTexts.ABOUT_ORIGINAL,
                Palette.GOLD, 22f);
        drawSection(r, NOTICE, MenuTexts.ABOUT_NOTICE_TITLE, MenuTexts.ABOUT_NOTICE,
                Palette.RED, 22f);
        drawSection(r, TECH, MenuTexts.ABOUT_TECH_TITLE, MenuTexts.ABOUT_TECH,
                Palette.CYAN, 22f);

        // 参与代号
        UiKit.panel(r, CREDITS, Palette.MINT, time);
        r.setTextSize(20f);
        r.setTextBold(true);
        r.setTextAlign(0);
        r.setTextColor(Palette.alpha(Palette.MINT, 0.9f));
        r.drawText(MenuTexts.ABOUT_CREDITS, CREDITS.x + 30f, CREDITS.y + 28f);
        float areaX = CREDITS.x + 28f;
        float areaW = CREDITS.w - 56f;
        float cell = areaW / MenuTexts.CREDITS.length;
        for (int i = 0; i < MenuTexts.CREDITS.length; i++) {
            float cx = areaX + cell * (i + 0.5f);
            int color = CREDIT_COLORS[i % CREDIT_COLORS.length];
            r.fillCircle(cx, CREDITS.y + 62f, 5f, Palette.alpha(color, 0.9f));
            r.setTextSize(20f);
            r.setTextBold(true);
            r.setTextAlign(1);
            r.setTextColor(color);
            r.drawText(MenuTexts.CREDITS[i], cx, CREDITS.y + 84f);
            r.setTextSize(15f);
            r.setTextBold(false);
            r.setTextColor(Palette.alpha(Palette.TEXT_DARK, 0.95f));
            r.drawText(MenuTexts.CREDIT_ROLES[i], cx, CREDITS.y + 106f);
        }

        // 底部
        r.setTextSize(19f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.9f));
        r.drawText("本重制版与 HandyGames 无隶属关系 · 请勿商用", UiKit.W * 0.5f, 1118f);
        r.setTextSize(17f);
        r.setTextColor(Palette.alpha(Palette.TEXT_DARK, 0.9f));
        r.drawText(MenuTexts.ABOUT_FOOTER, UiKit.W * 0.5f, 1152f);
        r.drawText("JAVA EDITION · v1.0.0 · 2026", UiKit.W * 0.5f, 1182f);
        r.restore();

        // 左上返回提示
        r.setTextSize(17f);
        r.setTextBold(false);
        r.setTextAlign(0);
        r.setTextColor(Palette.alpha(Palette.TEXT_DARK, 0.8f));
        r.drawText("返回", 120f, 110f);
        UiKit.hint(r, "点左上角箭头返回标题", 1230f, 0.55f);
    }

    private void drawSection(Renderer r, Rect panel, String title, String body, int accent, float size) {
        UiKit.panel(r, panel, accent, time);
        r.setTextSize(20f);
        r.setTextBold(true);
        r.setTextAlign(0);
        r.setTextColor(Palette.alpha(accent, 0.92f));
        r.drawText(title, panel.x + 30f, panel.y + 28f);
        UiKit.wrapped(r, body, panel.x + 30f, panel.y + 64f, panel.w - 60f, size,
                Palette.alpha(Palette.TEXT, 0.92f), 10f);
    }

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        if (pointer >= 0) return;
        pointer = id;
        backPressed = back.contains(x, y);
        if (backPressed) game.audio.play(Sfx.UI_CLICK);
    }

    @Override
    public void touchMove(Game game, int id, float x, float y) {
        if (id != pointer) return;
        if (backPressed && !back.contains(x, y)) backPressed = false;
    }

    @Override
    public void touchUp(Game game, int id, float x, float y) {
        if (id != pointer) return;
        pointer = -1;
        if (backPressed && back.contains(x, y)) {
            game.audio.play(Sfx.UI_BACK);
            game.gotoTitle();
        }
        backPressed = false;
    }

    @Override
    public void backPressed(Game game) {
        game.audio.play(Sfx.UI_BACK);
        game.gotoTitle();
    }
}
