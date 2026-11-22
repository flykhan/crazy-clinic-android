package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Music;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.Scene;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Texts;

/**
 * 设置：音效/音乐/震动三行大号滑动开关（按压缩放）、恢复默认、清除存档二次确认。
 * 所有改动立即写入存档。
 */
public final class SettingsScene implements Scene {
    private final Rect back = new Rect(34f, 72f, 76f, 76f);
    private final Rect[] rows = {new Rect(), new Rect(), new Rect()};
    private final Rect statsPanel = new Rect(60f, 572f, 600f, 138f);
    private final Rect resetDefault = new Rect(60f, 738f, 600f, 90f);
    private final Rect wipe = new Rect(60f, 848f, 600f, 90f);
    private final Rect tutorialRow = new Rect(60f, 958f, 600f, 90f);
    private final UiButton backBtn = new UiButton(Texts.BTN_BACK, UiKit.STYLE_SECONDARY);
    private final Rect dialog = new Rect(70f, 424f, 580f, 424f);
    private final UiButton dlgCancel = new UiButton(Texts.BTN_CANCEL, UiKit.STYLE_SECONDARY);
    private final UiButton dlgOk = new UiButton(MenuTexts.CONFIRM_OK, UiKit.STYLE_DANGER);

    private final float[] toggleAnim = new float[3];
    private float time;
    private int pointer = -1;
    private boolean backPressed;
    private int rowPressed = -1;
    private boolean confirmWipe;
    private String toast;
    private float toastTimer;
    private String statsText;

    @Override
    public void enter(Game game) {
        game.audio.music(Music.TITLE);
        game.fade = 1f;
        time = 0f;
        pointer = -1;
        backPressed = false;
        rowPressed = -1;
        confirmWipe = false;
        toast = null;
        toastTimer = 0f;
        toggleAnim[0] = game.save.soundOn ? 1f : 0f;
        toggleAnim[1] = game.save.musicOn ? 1f : 0f;
        toggleAnim[2] = game.save.vibrateOn ? 1f : 0f;
        for (int i = 0; i < rows.length; i++) {
            rows[i].set(60f, 196f + i * 120f, 600f, 104f);
        }
        backBtn.set(80f, 1112f, 560f, 92f).resetAppear();
        dlgCancel.set(104f, 690f, 238f, 92f);
        dlgOk.set(378f, 690f, 238f, 92f);
        statsText = String.format(MenuTexts.SET_DATA_FMT, game.save.totalKills, game.save.deaths,
                UiKit.formatTimeShort(game.save.totalPlaySeconds));
    }

    @Override
    public void update(Game game, float dt) {
        time += dt;
        UiKit.tickFade(game, dt);
        float[] target = {
                game.save.soundOn ? 1f : 0f,
                game.save.musicOn ? 1f : 0f,
                game.save.vibrateOn ? 1f : 0f
        };
        for (int i = 0; i < toggleAnim.length; i++) {
            toggleAnim[i] = MathUtil.approach(toggleAnim[i], target[i], 14f, dt);
        }
        if (toastTimer > 0f) toastTimer -= dt;
        backBtn.update(dt);
        dlgCancel.update(dt);
        dlgOk.update(dt);
    }

    @Override
    public void render(Game game, Renderer r) {
        UiBackdrop.menu(r, time, Palette.CYAN, "SETTINGS");
        UiKit.header(r, Texts.BTN_SETTINGS, null, time);
        UiKit.backCircle(r, back, backPressed, true);

        toggleRow(r, 0, MenuTexts.SET_SOUND_DESC, game.save.soundOn, toggleAnim[0],
                UiKit::iconSpeaker, Palette.MINT);
        toggleRow(r, 1, MenuTexts.SET_MUSIC_DESC, game.save.musicOn, toggleAnim[1],
                UiKit::iconMusic, Palette.CYAN);
        toggleRow(r, 2, MenuTexts.SET_VIBRATE_DESC, game.save.vibrateOn, toggleAnim[2],
                UiKit::iconVibrate, Palette.PURPLE);

        // 统计面板
        UiKit.panel(r, statsPanel, Palette.GOLD, time);
        r.setTextSize(20f);
        r.setTextBold(true);
        r.setTextAlign(0);
        r.setTextColor(Palette.alpha(Palette.GOLD, 0.9f));
        r.drawText(MenuTexts.SET_DATA, statsPanel.x + 30f, statsPanel.y + 30f);
        r.setTextSize(24f);
        r.setTextBold(false);
        r.setTextColor(Palette.alpha(Palette.TEXT, 0.92f));
        r.drawText(statsText, statsPanel.x + 30f, statsPanel.y + 76f);
        r.setTextSize(19f);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.9f));
        r.drawText(MenuTexts.SET_STORAGE, statsPanel.x + 30f, statsPanel.y + 112f);

        // 恢复默认
        drawActionRow(r, resetDefault, MenuTexts.SET_RESET_DEFAULT, Palette.MINT, false);
        // 清除存档
        drawActionRow(r, wipe, Texts.SET_RESET, Palette.RED, true);
        // 操作说明
        drawActionRow(r, tutorialRow, MenuTexts.TUTORIAL_LINK, Palette.CYAN, false, 3);

        // 返回
        backBtn.draw(r, time);

        // toast
        if (toastTimer > 0f && toast != null) {
            float a = MathUtil.clamp(toastTimer * 2f, 0f, 1f);
            r.save();
            r.setAlpha(a);
            float tw = 380f;
            r.fillRoundRect((UiKit.W - tw) * 0.5f, 1050f, tw, 56f, 28f,
                    Palette.alpha(UiKit.PANEL_INK, 0.96f));
            r.strokeRoundRect((UiKit.W - tw) * 0.5f, 1050f, tw, 56f, 28f, 2f,
                    Palette.alpha(Palette.MINT, 0.6f));
            r.setTextSize(24f);
            r.setTextBold(true);
            r.setTextAlign(1);
            r.setTextColor(Palette.MINT);
            r.drawText(toast, UiKit.W * 0.5f, 1078f);
            r.restore();
        }

        if (confirmWipe) drawConfirm(r);
        UiKit.hint(r, "所有设置立即保存", 1226f, 0.7f);
    }

    private void toggleRow(Renderer r, int index, String desc, boolean on, float anim,
                           IconDrawer icon, int accent) {
        Rect row = rows[index];
        boolean pressed = rowPressed == index;
        float scale = pressed ? 0.985f : 1f;
        r.save();
        r.translate(row.cx(), row.cy());
        r.scale(scale, scale);
        float x = -row.w * 0.5f, y = -row.h * 0.5f;
        r.fillRoundRect(x + 3f, y + 6f, row.w, row.h, 20f, Palette.alpha(Palette.BLACK, 0.5f));
        r.fillRoundRect(x, y, row.w, row.h, 20f,
                Palette.alpha(on ? UiKit.PANEL_LIGHT : UiKit.PANEL_INK, 0.97f));
        r.strokeRoundRect(x, y, row.w, row.h, 20f, 2.5f,
                Palette.alpha(on ? Palette.PANEL_STROKE : 0xFF29313B, 0.95f));
        if (pressed) {
            r.fillRoundRect(x, y, row.w, row.h, 20f, Palette.alpha(Palette.BLACK, 0.16f));
        }
        // 图标盘
        r.fillCircle(x + 62f, 0f, 34f, Palette.alpha(Palette.BG_DARK, 0.7f));
        r.strokeCircle(x + 62f, 0f, 34f, 2f, Palette.alpha(accent, on ? 0.7f : 0.28f));
        icon.draw(r, x + 62f, 0f, 44f, on ? accent : Palette.TEXT_DARK);
        // 文字
        r.setTextSize(32f);
        r.setTextBold(true);
        r.setTextAlign(0);
        r.setTextColor(on ? Palette.TEXT : Palette.alpha(Palette.TEXT_DIM, 0.9f));
        r.drawText(labelFor(index), x + 116f, -16f);
        r.setTextSize(19f);
        r.setTextBold(false);
        r.setTextColor(Palette.alpha(Palette.TEXT_DARK, 0.95f));
        r.drawText(desc, x + 116f, 22f);
        // 开关状态文字 + 开关
        r.setTextSize(24f);
        r.setTextBold(true);
        r.setTextAlign(2);
        r.setTextColor(on ? Palette.MINT : Palette.alpha(Palette.TEXT_DARK, 0.95f));
        r.drawText(on ? Texts.ON : Texts.OFF, x + row.w - 206f, 0f);
        UiKit.toggle(r, x + row.w - 186f, -32f, 160f, 64f, anim, pressed, time);
        r.restore();
    }

    private String labelFor(int index) {
        switch (index) {
            case 0: return Texts.SET_SOUND;
            case 1: return Texts.SET_MUSIC;
            default: return Texts.SET_VIBRATE;
        }
    }

    private void drawActionRow(Renderer r, Rect b, String label, int accent, boolean danger) {
        drawActionRow(r, b, label, accent, danger, danger ? 1 : 0);
    }

    /** iconKind: 0 重置 1 警告 2 帮助 3 说明 4 箭头。 */
    private void drawActionRow(Renderer r, Rect b, String label, int accent, boolean danger, int iconKind) {
        r.fillRoundRect(b.x + 3f, b.y + 6f, b.w, b.h, 20f, Palette.alpha(Palette.BLACK, 0.45f));
        r.fillRoundRect(b.x, b.y, b.w, b.h, 20f,
                danger ? Palette.alpha(Palette.RED_DARK, 0.55f) : Palette.alpha(UiKit.PANEL_INK, 0.95f));
        r.strokeRoundRect(b.x, b.y, b.w, b.h, 20f, 2.5f,
                Palette.alpha(danger ? Palette.RED : Palette.PANEL_STROKE, 0.8f));
        r.fillRoundRect(b.x, b.y + 18f, 6f, b.h - 36f, 3f, Palette.alpha(accent, 0.85f));
        switch (iconKind) {
            case 1:
                UiKit.iconWarning(r, b.x + 58f, b.cy(), 40f, Palette.alpha(Palette.RED, 0.9f));
                break;
            case 3:
                UiKit.iconHelp(r, b.x + 58f, b.cy(), 40f, Palette.alpha(Palette.CYAN, 0.9f));
                break;
            default:
                UiKit.iconReset(r, b.x + 58f, b.cy(), 40f, Palette.alpha(Palette.MINT, 0.9f));
                break;
        }
        r.setTextSize(30f);
        r.setTextBold(true);
        r.setTextAlign(0);
        r.setTextColor(danger ? 0xFFFFD9D4 : Palette.TEXT);
        r.drawText(label, b.x + 96f, b.cy());
        UiKit.iconChevron(r, b.x + b.w - 34f, b.cy(), 36f,
                Palette.alpha(danger ? Palette.RED : Palette.TEXT_DIM, 0.8f), true);
    }

    private void drawConfirm(Renderer r) {
        UiKit.scrim(r, 0.72f);
        UiKit.panel(r, dialog, Palette.RED, time);
        r.setTextSize(40f);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.RED, 0.95f));
        r.drawText(MenuTexts.CONFIRM_TITLE, dialog.cx(), dialog.y + 64f);
        UiKit.iconWarning(r, dialog.cx(), dialog.y + 128f, 56f, Palette.ALERT);
        r.setTextSize(23f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT, 0.9f));
        String[] lines = MenuTexts.CONFIRM_BODY.split("\n");
        for (int i = 0; i < lines.length; i++) {
            r.drawText(lines[i], dialog.cx(), dialog.y + 192f + i * 34f);
        }
        r.fillRect(dialog.x + 40f, dialog.y + 248f, dialog.w - 80f, 1.5f,
                Palette.alpha(Palette.PANEL_STROKE, 0.7f));
        dlgCancel.draw(r, time);
        dlgOk.draw(r, time);
    }

    // ------------------------------------------------------------------
    // 触摸
    // ------------------------------------------------------------------

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        if (pointer >= 0) return;
        pointer = id;
        if (confirmWipe) {
            if (dlgCancel.down(id, x, y) || dlgOk.down(id, x, y)) {
                game.audio.play(Sfx.UI_CLICK);
            }
            return;
        }
        backPressed = back.contains(x, y);
        if (backPressed) {
            game.audio.play(Sfx.UI_CLICK);
            return;
        }
        for (int i = 0; i < rows.length; i++) {
            if (rows[i].contains(x, y)) {
                rowPressed = i;
                game.audio.play(Sfx.UI_CLICK);
                return;
            }
        }
        if (resetDefault.contains(x, y)) {
            rowPressed = -2;
            game.audio.play(Sfx.UI_CLICK);
            return;
        }
        if (wipe.contains(x, y)) {
            rowPressed = -3;
            game.audio.play(Sfx.UI_CLICK);
            return;
        }
        if (tutorialRow.contains(x, y)) {
            rowPressed = -4;
            game.audio.play(Sfx.UI_CLICK);
            return;
        }
        if (backBtn.down(id, x, y)) {
            game.audio.play(Sfx.UI_CLICK);
        }
    }

    @Override
    public void touchMove(Game game, int id, float x, float y) {
        if (id != pointer) return;
        if (backPressed && !back.contains(x, y)) backPressed = false;
        if (rowPressed >= 0 && !rows[rowPressed].contains(x, y)) rowPressed = -1;
        if (rowPressed == -2 && !resetDefault.contains(x, y)) rowPressed = -1;
        if (rowPressed == -3 && !wipe.contains(x, y)) rowPressed = -1;
        if (rowPressed == -4 && !tutorialRow.contains(x, y)) rowPressed = -1;
        if (backBtn.pointer() == id && !backBtn.contains(x, y)) backBtn.cancel(id);
    }

    @Override
    public void touchUp(Game game, int id, float x, float y) {
        if (id != pointer) return;
        pointer = -1;
        if (confirmWipe) {
            if (dlgCancel.up(id, x, y)) {
                confirmWipe = false;
                game.audio.play(Sfx.UI_BACK);
            } else if (dlgOk.up(id, x, y)) {
                wipeSave(game);
            } else {
                dlgCancel.cancel(id);
                dlgOk.cancel(id);
            }
            return;
        }
        if (backPressed && back.contains(x, y)) {
            backPressed = false;
            game.audio.play(Sfx.UI_BACK);
            game.gotoTitle();
            return;
        }
        backPressed = false;
        if (backBtn.up(id, x, y)) {
            game.audio.play(Sfx.UI_BACK);
            game.gotoTitle();
            return;
        }
        if (rowPressed >= 0 && rows[rowPressed].contains(x, y)) {
            toggleSetting(game, rowPressed);
            rowPressed = -1;
            return;
        }
        if (rowPressed == -2 && resetDefault.contains(x, y)) {
            resetDefaults(game);
            rowPressed = -1;
            return;
        }
        if (rowPressed == -4 && tutorialRow.contains(x, y)) {
            rowPressed = -1;
            TutorialScene.pendingStartGame = false;
            TutorialScene.returnFloor = 0;
            game.gotoTutorial();
            return;
        }
        if (rowPressed == -3 && wipe.contains(x, y)) {
            confirmWipe = true;
            dlgCancel.resetAppear();
            dlgOk.resetAppear();
            dlgCancel.appearDelay = 0f;
            dlgOk.appearDelay = 0f;
            game.audio.play(Sfx.UI_CLICK);
            rowPressed = -1;
            return;
        }
        rowPressed = -1;
        backBtn.cancel(id);
    }

    private void toggleSetting(Game game, int index) {
        if (index == 0) {
            game.save.soundOn = !game.save.soundOn;
            game.audio.setSoundEnabled(game.save.soundOn);
            if (game.save.soundOn) game.audio.play(Sfx.UI_CONFIRM);
        } else if (index == 1) {
            game.save.musicOn = !game.save.musicOn;
            game.audio.setMusicEnabled(game.save.musicOn);
            if (game.save.musicOn) game.audio.music(Music.TITLE);
            else game.audio.stopMusic();
            game.audio.play(Sfx.UI_CLICK);
        } else {
            game.save.vibrateOn = !game.save.vibrateOn;
            if (game.save.vibrateOn) game.audio.vibrate(40);
            game.audio.play(Sfx.UI_CLICK);
        }
        game.saveNow();
        toast = index == 0 ? Texts.SET_SOUND + (game.save.soundOn ? " 开" : " 关")
                : index == 1 ? Texts.SET_MUSIC + (game.save.musicOn ? " 开" : " 关")
                : Texts.SET_VIBRATE + (game.save.vibrateOn ? " 开" : " 关");
        toastTimer = 1.4f;
    }

    private void resetDefaults(Game game) {
        game.save.soundOn = true;
        game.save.musicOn = true;
        game.save.vibrateOn = true;
        game.audio.setSoundEnabled(true);
        game.audio.setMusicEnabled(true);
        game.audio.music(Music.TITLE);
        game.saveNow();
        toast = MenuTexts.RESET_DONE;
        toastTimer = 1.6f;
        game.audio.play(Sfx.UI_CONFIRM);
    }

    private void wipeSave(Game game) {
        game.save.unlockedFloor = 1;
        game.save.organsMask = 0;
        game.save.totalKills = 0;
        game.save.deaths = 0;
        game.save.totalPlaySeconds = 0;
        game.save.tutorialSeen = false;
        game.saveNow();
        confirmWipe = false;
        showToast(MenuTexts.WIPED);
        game.audio.play(Sfx.HACK_FAIL);
    }

    private void showToast(String text) {
        toast = text;
        toastTimer = 1.8f;
    }

    @Override
    public void backPressed(Game game) {
        if (confirmWipe) {
            confirmWipe = false;
            game.audio.play(Sfx.UI_BACK);
            return;
        }
        game.audio.play(Sfx.UI_BACK);
        game.gotoTitle();
    }

    /** 行内图标的函数式封装，避免枚举/字符串分派。 */
    private interface IconDrawer {
        void draw(Renderer r, float cx, float cy, float size, int color);
    }
}
