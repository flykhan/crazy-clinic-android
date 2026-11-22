package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Music;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.Scene;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Levels;
import com.crazyclinic.core.content.Texts;
import com.crazyclinic.core.world.LevelDef;

/**
 * 关卡前剧情简报：大号层名 + 副标题 + 打字机剧情文本 + “钥匙卡 → 电脑 → 电梯”目标示意。
 */
public final class IntroScene implements Scene {
    private final int floor;
    private final LevelDef def;
    private final Rect back = new Rect(34f, 72f, 76f, 76f);
    private final Rect panel = new Rect(48f, 330f, 624f, 380f);
    private final Rect objective = new Rect(48f, 734f, 624f, 176f);
    private final UiButton start = new UiButton(Texts.BTN_START, UiKit.STYLE_PRIMARY);
    private final UiButton backBtn = new UiButton(Texts.BTN_BACK, UiKit.STYLE_SECONDARY);
    private final UiButton tutorial = new UiButton(MenuTexts.TUTORIAL_LINK, UiKit.STYLE_GHOST);

    private float time;
    private int pointer = -1;
    private boolean backPressed;
    private String intro = "";
    private float introSize = -1f;

    public IntroScene(int floor) {
        this.floor = MathUtil.clamp(floor, 1, Game.FLOOR_COUNT);
        this.def = Levels.get(this.floor);
    }

    @Override
    public void enter(Game game) {
        game.audio.music(Music.TITLE);
        game.fade = 1f;
        time = 0f;
        pointer = -1;
        introSize = -1f;
        backPressed = false;
        intro = def.intro == null ? "" : def.intro;
        start.set(80f, 1014f, 560f, 100f).resetAppear();
        backBtn.set(80f, 1132f, 260f, 88f).resetAppear();
        tutorial.set(380f, 1132f, 260f, 88f).resetAppear();
        start.appearDelay = 0.10f;
        backBtn.appearDelay = 0.16f;
        tutorial.appearDelay = 0.20f;
    }

    @Override
    public void update(Game game, float dt) {
        time += dt;
        UiKit.tickFade(game, dt);
        start.update(dt);
        backBtn.update(dt);
        tutorial.update(dt);
    }

    @Override
    public void render(Game game, Renderer r) {
        int accent = UiKit.floorAccent(floor);
        UiBackdrop.menu(r, time, accent, String.format("%02d", floor));

        // 返回圆钮
        UiKit.backCircle(r, back, backPressed, true);

        // 楼层标题
        r.setTextSize(22f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.9f));
        r.drawText("第 " + floor + " 层  /  " + Game.FLOOR_COUNT, UiKit.W * 0.5f, 108f);

        float slide = (1f - MathUtil.easeOutCubic(MathUtil.clamp(time * 2.4f, 0f, 1f))) * 24f;
        r.save();
        r.setAlpha(MathUtil.clamp(time * 2.4f, 0f, 1f));
        UiKit.textFit(r, def.name, UiKit.W * 0.5f, 190f + slide, 640f, 86f, 46f,
                Palette.TEXT, 1, true);
        // 副标题（主题色，两侧短线）
        String sub = "· " + def.subtitle + " ·";
        r.setTextSize(30f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(accent, 0.95f));
        r.drawText(sub, UiKit.W * 0.5f, 264f + slide);
        float lineW = r.measureText(sub) * 0.5f + 20f;
        r.fillRect(UiKit.W * 0.5f - lineW - 60f, 264f + slide, 48f, 2f, Palette.alpha(accent, 0.5f));
        r.fillRect(UiKit.W * 0.5f + lineW + 12f, 264f + slide, 48f, 2f, Palette.alpha(accent, 0.5f));
        r.restore();

        // 简报面板
        UiKit.panel(r, panel, accent, time);
        r.setTextSize(20f);
        r.setTextBold(true);
        r.setTextAlign(0);
        r.setTextColor(Palette.alpha(accent, 0.9f));
        r.drawText(MenuTexts.BRIEFING, panel.x + 34f, panel.y + 34f);
        r.fillRect(panel.x + 34f, panel.y + 52f, panel.w - 68f, 1.5f, Palette.alpha(accent, 0.28f));
        // 病历纸质感：水印 + 极淡横线
        r.setTextSize(128f);
        r.setTextBold(true);
        r.setTextAlign(2);
        r.setTextColor(Palette.alpha(accent, 0.045f));
        r.drawText("FILE", panel.x + panel.w - 40f, panel.y + 250f);
        for (int i = 0; i < 4; i++) {
            r.fillRect(panel.x + 34f, panel.y + 176f + i * 16f, panel.w - 68f, 1f,
                    Palette.alpha(Palette.WHITE, 0.030f));
        }
        for (float sy = panel.y + 70f; sy < panel.y + panel.h - 70f; sy += 6f) {
            r.fillRect(panel.x + 2f, sy, panel.w - 4f, 1f, Palette.alpha(Palette.BLACK, 0.055f));
        }

        if (introSize < 0f) introSize = fitIntroSize(r);
        float textEnd = UiKit.wrapped(r, intro, panel.x + 34f, panel.y + 84f, panel.w - 68f,
                introSize, Palette.alpha(Palette.TEXT, 0.96f), 13f);
        // 档案页脚（正文过长时自动省略，避免压字）
        float footY = panel.y + panel.h - 96f;
        if (textEnd < footY - 6f) {
            for (float dx = panel.x + 34f; dx < panel.x + panel.w - 34f; dx += 18f) {
                r.fillRect(dx, footY, 10f, 1.5f, Palette.alpha(accent, 0.30f));
            }
            r.setTextSize(18f);
            r.setTextBold(false);
            r.setTextAlign(0);
            r.setTextColor(Palette.alpha(Palette.TEXT_DARK, 0.95f));
            r.drawText("病人编号 #C4RT3R  ·  病房 B-" + String.format("%02d", floor),
                    panel.x + 34f, footY + 28f);
            r.drawText("状态：在逃  ·  危险等级 " + dangerLevel(floor),
                    panel.x + 34f, footY + 54f);
            // 机密印章
            r.save();
            r.translate(panel.x + panel.w - 118f, footY + 42f);
            r.rotate(-11f);
            r.strokeRect(-74f, -24f, 148f, 48f, 2.5f, Palette.alpha(Palette.RED, 0.42f));
            r.setTextSize(24f);
            r.setTextBold(true);
            r.setTextAlign(1);
            r.setTextColor(Palette.alpha(Palette.RED, 0.42f));
            r.drawText("机 密", 0f, 0f);
            r.restore();
        }

        // 目标面板
        UiKit.panel(r, objective, 0, time);
        r.setTextSize(20f);
        r.setTextBold(true);
        r.setTextAlign(0);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.95f));
        r.drawText(MenuTexts.OBJECTIVE, objective.x + 34f, objective.y + 32f);

        float iconY = objective.y + 92f;
        float labelY = objective.y + 140f;
        float[] cx = {objective.x + 116f, objective.x + 312f, objective.x + 508f};
        step(r, cx[0], iconY, labelY, MenuTexts.OBJ_KEY, accent, 0);
        step(r, cx[1], iconY, labelY, MenuTexts.OBJ_HACK, accent, 1);
        step(r, cx[2], iconY, labelY, MenuTexts.OBJ_ELEVATOR, accent, 2);
        for (int i = 0; i < 2; i++) {
            float ax = (cx[i] + cx[i + 1]) * 0.5f;
            UiKit.iconArrow(r, ax, iconY, 44f, Palette.alpha(Palette.TEXT_DIM, 0.75f));
        }

        // 按钮
        start.draw(r, time);
        backBtn.draw(r, time);
        tutorial.draw(r, time);

        // 底部提示（第 1 层强调教学）
        if (floor == 1) {
            r.setTextSize(19f);
            r.setTextBold(false);
            r.setTextAlign(1);
            r.setTextColor(Palette.alpha(UiKit.HAZARD, 0.75f));
            r.drawText("教学关：跟着左上角的目标提示走，别惊动看守。", UiKit.W * 0.5f, 954f);
        } else {
            r.setTextSize(19f);
            r.setTextBold(false);
            r.setTextAlign(1);
            r.setTextColor(Palette.alpha(Palette.TEXT_DARK, 0.85f));
            r.drawText("按任意目标推进 · 电梯是唯一出口", UiKit.W * 0.5f, 954f);
        }
    }

    /** 根据正文长度自适应字号，保证整段剧情在一个面板内完整显示。 */
    private float fitIntroSize(Renderer r) {
        float maxW = panel.w - 68f;
        float maxH = panel.h - 84f - 24f;
        for (float size = 29f; size >= 21f; size -= 2f) {
            int lines = countLines(r, intro, maxW, size);
            if (lines * (size + 13f) <= maxH) return size;
        }
        return 21f;
    }

    private static int countLines(Renderer r, String text, float maxWidth, float size) {
        r.setTextSize(size);
        int lines = 0;
        StringBuilder sb = new StringBuilder(64);
        String[] rows = text.split("\n", -1);
        for (String row : rows) {
            sb.setLength(0);
            if (row.isEmpty()) {
                lines++;
                continue;
            }
            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                sb.append(c);
                if (sb.length() > 1 && r.measureText(sb.toString()) > maxWidth) {
                    sb.setLength(sb.length() - 1);
                    lines++;
                    sb.setLength(0);
                    sb.append(c);
                }
            }
            if (sb.length() > 0) lines++;
        }
        return lines;
    }

    private static String dangerLevel(int floor) {
        if (floor >= 9) return "极高 ■■■■■";
        if (floor >= 6) return "高 ■■■■□";
        if (floor >= 3) return "中 ■■■□□";
        return "低 ■■□□□";
    }

    private void step(Renderer r, float cx, float iconY, float labelY, String label, int accent, int kind) {
        r.fillCircle(cx, iconY, 34f, Palette.alpha(Palette.BG_DARK, 0.65f));
        r.strokeCircle(cx, iconY, 34f, 2f, Palette.alpha(accent, 0.35f));
        switch (kind) {
            case 0:
                UiKit.iconKey(r, cx, iconY, 50f, Palette.GOLD);
                break;
            case 1:
                UiKit.iconComputer(r, cx, iconY, 52f, Palette.CYAN);
                break;
            default:
                UiKit.iconElevator(r, cx, iconY, 52f, Palette.MINT);
                break;
        }
        r.setTextSize(21f);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT, 0.92f));
        r.drawText(label, cx, labelY);
    }

    // ------------------------------------------------------------------
    // 触摸
    // ------------------------------------------------------------------

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        if (pointer >= 0) return;
        pointer = id;
        backPressed = back.contains(x, y);
        if (backPressed) game.audio.play(Sfx.UI_CLICK);
        if (start.down(id, x, y) || backBtn.down(id, x, y) || tutorial.down(id, x, y)) {
            game.audio.play(Sfx.UI_CLICK);
            return;
        }
    }

    @Override
    public void touchMove(Game game, int id, float x, float y) {
        if (id != pointer) return;
        if (backPressed && !back.contains(x, y)) backPressed = false;
        if (start.pointer() == id && !start.contains(x, y)) start.cancel(id);
        if (backBtn.pointer() == id && !backBtn.contains(x, y)) backBtn.cancel(id);
        if (tutorial.pointer() == id && !tutorial.contains(x, y)) tutorial.cancel(id);
    }

    @Override
    public void touchUp(Game game, int id, float x, float y) {
        if (id != pointer) return;
        pointer = -1;
        if (backPressed && back.contains(x, y)) {
            backPressed = false;
            game.audio.play(Sfx.UI_BACK);
            game.gotoLevelSelect();
            return;
        }
        backPressed = false;
        if (start.up(id, x, y)) {
            game.audio.play(Sfx.UI_CONFIRM);
            game.startFloor(floor);
        } else if (backBtn.up(id, x, y)) {
            game.audio.play(Sfx.UI_BACK);
            game.gotoLevelSelect();
        } else if (tutorial.up(id, x, y)) {
            game.audio.play(Sfx.UI_CONFIRM);
            TutorialScene.pendingStartGame = false;
            TutorialScene.returnFloor = floor;
            game.gotoTutorial();
        } else {
            start.cancel(id);
            backBtn.cancel(id);
            tutorial.cancel(id);
        }
    }

    @Override
    public void backPressed(Game game) {
        game.audio.play(Sfx.UI_BACK);
        game.gotoLevelSelect();
    }
}
