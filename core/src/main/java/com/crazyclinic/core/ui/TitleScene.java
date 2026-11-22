package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Music;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.Scene;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Texts;

/**
 * 标题界面：走廊剪影 + 闪烁日光灯 + 漂浮尘埃 + 心电线的动态背景，
 * 血迹医疗十字徽章、《疯狂诊所》大标题（轻微抖动 + 滴血）、封条副标题与五个主菜单按钮。
 */
public final class TitleScene implements Scene {
    private static final int BTN_NEW = 0;
    private static final int BTN_CONTINUE = 1;
    private static final int BTN_LEVELS = 2;
    private static final int BTN_SETTINGS = 3;
    private static final int BTN_ABOUT = 4;

    private static final float TITLE_SIZE = 116f;
    private static final float TITLE_Y = 292f;

    private final UiButton[] buttons = new UiButton[5];
    private final float[] charLeft = new float[8];
    private final float[] charWidth = new float[8];
    private final float[] dripLen = new float[8];
    private int pointer = -1;
    private float time;
    private boolean titleMetrics;
    private boolean hasSave;
    private int lastUnlocked = -1;
    private int lastOrgans = -1;
    private String progressLine = "";
    private String organLine = "";

    public TitleScene() {
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new UiButton().appearDelay(0.05f + i * 0.05f);
        }
        for (int i = 0; i < dripLen.length; i++) {
            dripLen[i] = 7f + MathUtil.hash01(i * 17 + 5, 41) * 26f;
        }
    }

    @Override
    public void enter(Game game) {
        game.audio.music(Music.TITLE);
        game.fade = 1f;
        time = 0f;
        titleMetrics = false;
        pointer = -1;
        for (UiButton b : buttons) b.resetAppear();
        updateLayout(game);
    }

    private void updateLayout(Game game) {
        float w = 500f, h = 92f;
        float x = (UiKit.W - w) * 0.5f;
        float y = 556f;
        float gap = 18f;
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].set(x, y + i * (h + gap), w, h);
        }
        boolean newHasSave = game.save.unlockedFloor > 1 || game.save.tutorialSeen
                || game.save.organsMask != 0 || game.save.totalKills > 0;
        hasSave = newHasSave;
        buttons[BTN_NEW].label(Texts.BTN_NEW_GAME).style(UiKit.STYLE_PRIMARY).enabled(true);
        buttons[BTN_CONTINUE].style(UiKit.STYLE_SECONDARY).enabled(hasSave);
        if (game.save.unlockedFloor != lastUnlocked || organCount(game) != lastOrgans) {
            lastUnlocked = game.save.unlockedFloor;
            lastOrgans = organCount(game);
            buttons[BTN_CONTINUE].label(hasSave
                    ? String.format(MenuTexts.CONTINUE_FMT, Math.max(1, game.save.unlockedFloor))
                    : MenuTexts.CONTINUE_NONE);
            progressLine = "已解锁 " + game.save.unlockedFloor + " / " + Game.FLOOR_COUNT + " 层";
            organLine = "器官 " + lastOrgans + " / 7";
        }
        buttons[BTN_LEVELS].label(Texts.BTN_LEVELS).style(UiKit.STYLE_SECONDARY).enabled(true);
        buttons[BTN_SETTINGS].label(Texts.BTN_SETTINGS).style(UiKit.STYLE_SECONDARY).enabled(true);
        buttons[BTN_ABOUT].label(Texts.BTN_ABOUT).style(UiKit.STYLE_SECONDARY).enabled(true);
    }

    private static int organCount(Game game) {
        int n = 0;
        for (com.crazyclinic.core.world.Organ o : com.crazyclinic.core.world.Organ.values()) {
            if (game.save.hasOrgan(o)) n++;
        }
        return n;
    }

    @Override
    public void update(Game game, float dt) {
        time += dt;
        UiKit.tickFade(game, dt);
        updateLayout(game);
        for (UiButton b : buttons) b.update(dt);
    }

    @Override
    public void render(Game game, Renderer r) {
        UiBackdrop.corridor(r, time);

        // 左侧红色应急灯，缓慢呼吸
        float redPulse = 0.5f + 0.5f * (float) Math.sin(time * 1.1f);
        r.fillRadialGradient(64f, 700f, 460f, Palette.alpha(Palette.RED, 0.10f * redPulse), 0x00000000);

        // 顶部英文装饰行
        r.setTextSize(20f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(UiKit.STEEL, 0.55f));
        r.drawText(MenuTexts.TITLE_EN, UiKit.W * 0.5f, UiKit.SAFE_TOP + 22f);
        r.setTextSize(17f);
        r.setTextColor(Palette.alpha(Palette.TEXT_DARK, 0.95f));
        r.drawText("2009 J2ME 经典  ·  ANDROID 程序化重制", UiKit.W * 0.5f, UiKit.SAFE_TOP + 50f);

        // 医院十字徽章
        UiKit.emblem(r, UiKit.W * 0.5f, 178f, 42f, time);

        // 大标题：轻微抖动 + 偶发故障位移
        float shake = (float) Math.sin(time * 1.5f) * 2.2f + (float) Math.sin(time * 4.4f) * 0.8f;
        int glitchFrame = (int) (time * 9f);
        if (MathUtil.hash01(glitchFrame, 71) < 0.10f) {
            shake += (MathUtil.hash01(glitchFrame, 13) - 0.5f) * 16f;
        }
        if (!titleMetrics) {
            r.setTextSize(TITLE_SIZE);
            r.setTextBold(true);
            String title = Texts.TITLE;
            float total = r.measureText(title);
            float cursor = -total * 0.5f;
            int n = Math.min(title.length(), charLeft.length);
            for (int i = 0; i < n; i++) {
                charLeft[i] = cursor;
                charWidth[i] = r.measureText(title.substring(i, i + 1));
                cursor += charWidth[i];
            }
            titleMetrics = true;
        }
        r.fillRadialGradient(UiKit.W * 0.5f, TITLE_Y, 360f,
                Palette.alpha(Palette.RED, 0.10f + 0.05f * UiKit.pulse(time, 1.7f)), 0x00000000);
        r.save();
        r.translate(UiKit.W * 0.5f + shake, TITLE_Y);
        r.setTextSize(TITLE_SIZE);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.BLOOD, 0.90f));
        r.drawText(Texts.TITLE, 7f, 9f);
        r.setTextColor(Palette.alpha(Palette.BLACK, 0.75f));
        r.drawText(Texts.TITLE, 4f, 5f);
        r.setTextColor(UiKit.PAPER);
        r.drawText(Texts.TITLE, 0f, 0f);
        r.setTextColor(Palette.alpha(Palette.RED, 0.06f));
        r.drawText(Texts.TITLE, 0f, -1f);
        // 从字底滴下的血
        float baseY = TITLE_SIZE * 0.38f;
        int drips = Math.min(charLeft.length, Texts.TITLE.length());
        for (int i = 0; i < drips; i++) {
            float cx = charLeft[i] + charWidth[i] * 0.5f;
            float len = dripLen[i];
            float wob = (float) Math.sin(time * 0.8f + i) * 1.5f;
            r.fillRoundRect(cx - 2f, baseY, 4f, len, 2f, Palette.alpha(Palette.BLOOD, 0.85f));
            r.fillCircle(cx + wob, baseY + len, 3.6f, Palette.alpha(Palette.BLOOD, 0.9f));
        }
        r.restore();

        // 副标题封条（轻微摇摆）
        UiKit.tape(r, UiKit.W * 0.5f, 402f, 446f, 46f, Texts.SUBTITLE, time * 12f,
                (float) Math.sin(time * 0.6f) * 1.6f);

        // 版本信息
        r.setTextSize(21f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.95f));
        r.drawText(Texts.VERSION + "  ·  程序化重绘  ·  不含原版素材", UiKit.W * 0.5f, 452f);

        // 心电监护线
        UiBackdrop.ecg(r, 0f, 512f, UiKit.W, 42f, time, Palette.alpha(Palette.RED, 0.45f));
        r.fillRect(0f, 532f, UiKit.W, 1f, Palette.alpha(Palette.RED, 0.10f));

        // 按钮
        for (UiButton b : buttons) b.draw(r, time);

        // 底部进度信息（可选信息，不抢主视觉）
        r.setTextSize(17f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.85f));
        r.drawText(progressLine + "   ·   " + organLine, UiKit.W * 0.5f, 1158f);
        float barW = 300f, barX = (UiKit.W - barW) * 0.5f;
        r.fillRoundRect(barX, 1174f, barW, 4f, 2f, Palette.alpha(Palette.BLACK, 0.45f));
        float ratio = (game.save.unlockedFloor - 1) / (float) (Game.FLOOR_COUNT - 1);
        r.fillRoundRect(barX, 1174f, Math.max(4f, barW * MathUtil.clamp(ratio, 0f, 1f)), 4f, 2f,
                Palette.alpha(Palette.MINT, 0.75f));

        // 底部警戒条 + 版权行
        UiKit.hazard(r, 236f, 1136f, 248f, 8f, time * 8f,
                UiKit.HAZARD, UiKit.HAZARD_DARK, 0.35f);
        r.setTextSize(19f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT_DARK, 0.95f));
        r.drawText(MenuTexts.CREDIT_LINE, UiKit.W * 0.5f, 1206f);
        r.setTextSize(17f);
        r.setTextColor(Palette.alpha(Palette.TEXT_DARK, 0.7f));
        r.drawText("JAVA EDITION · v1.0.0 · 2026", UiKit.W * 0.5f, 1228f);
    }

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        if (pointer >= 0) return;
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].down(id, x, y)) {
                pointer = id;
                game.audio.play(Sfx.UI_CLICK);
                return;
            }
        }
    }

    @Override
    public void touchMove(Game game, int id, float x, float y) {
        if (id != pointer) return;
        for (UiButton b : buttons) {
            if (b.pointer() == id && !b.contains(x, y)) {
                b.cancel(id);
                pointer = -1;
                return;
            }
        }
    }

    @Override
    public void touchUp(Game game, int id, float x, float y) {
        if (id != pointer) return;
        pointer = -1;
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].up(id, x, y)) {
                activate(game, i);
                return;
            }
        }
        for (UiButton b : buttons) b.cancel(id);
    }

    private void activate(Game game, int index) {
        switch (index) {
            case BTN_NEW:
                game.audio.play(Sfx.UI_CONFIRM);
                if (!game.save.tutorialSeen) {
                    TutorialScene.pendingStartGame = true;
                    game.gotoTutorial();
                } else {
                    TutorialScene.pendingStartGame = false;
                    game.gotoIntro(1);
                }
                break;
            case BTN_CONTINUE:
                if (!hasSave) return;
                game.audio.play(Sfx.UI_CONFIRM);
                TutorialScene.pendingStartGame = false;
                game.gotoIntro(Math.max(1, game.save.unlockedFloor));
                break;
            case BTN_LEVELS:
                game.audio.play(Sfx.UI_CONFIRM);
                game.gotoLevelSelect();
                break;
            case BTN_SETTINGS:
                game.audio.play(Sfx.UI_CONFIRM);
                game.gotoSettings();
                break;
            default:
                game.audio.play(Sfx.UI_CONFIRM);
                game.gotoAbout();
                break;
        }
    }

    @Override
    public void backPressed(Game game) {
        // 标题页返回键交给平台（Android 退出应用）。
    }
}
