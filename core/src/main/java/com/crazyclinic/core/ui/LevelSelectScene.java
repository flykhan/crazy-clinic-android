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
import com.crazyclinic.core.world.Organ;

/**
 * 关卡选择：10 层卡片（2 列可滚动）、解锁状态、已通关标记、顶部 7 器官收集展示。
 */
public final class LevelSelectScene implements Scene {
    private static final float VIEW_TOP = 300f;
    private static final float VIEW_BOTTOM = 1152f;
    private static final float CARD_W = 316f;
    private static final float CARD_H = 196f;
    private static final float GAP_X = 24f;
    private static final float GAP_Y = 20f;
    private static final float START_X = (UiKit.W - CARD_W * 2f - GAP_X) * 0.5f;
    private static final float CONTENT_TOP = 308f;

    private final Rect back = new Rect(34f, 72f, 76f, 76f);
    private final Rect organPanel = new Rect(40f, 176f, 640f, 104f);
    private final Rect[] cardRect = new Rect[Game.FLOOR_COUNT];
    private final Organ[] organs = Organ.values();

    private float time;
    private float scroll;
    private float velocity;
    private int pointer = -1;
    private boolean dragging;
    private float dragStartY;
    private float lastY;
    private float downY;
    private boolean backPressed;
    private int pressedCard = -1;
    private final StringBuilder sb = new StringBuilder(64);
    private final String[] floorNum = new String[Game.FLOOR_COUNT];
    private String statsLine = "";

    public LevelSelectScene() {
        for (int i = 0; i < cardRect.length; i++) {
            cardRect[i] = new Rect();
            floorNum[i] = String.format("%02d", i + 1);
        }
    }

    @Override
    public void enter(Game game) {
        game.audio.music(Music.TITLE);
        game.fade = 1f;
        time = 0f;
        scroll = 0f;
        velocity = 0f;
        pointer = -1;
        dragging = false;
        pressedCard = -1;
        backPressed = false;
        layoutCards();
        sb.setLength(0);
        sb.append("总击倒 ").append(game.save.totalKills)
                .append("  ·  死亡 ").append(game.save.deaths)
                .append("  ·  时长 ").append(UiKit.formatTimeShort(game.save.totalPlaySeconds));
        statsLine = sb.toString();
        sb.setLength(0);
    }

    private void layoutCards() {
        for (int i = 0; i < Game.FLOOR_COUNT; i++) {
            int col = i % 2;
            int row = i / 2;
            cardRect[i].set(START_X + col * (CARD_W + GAP_X),
                    CONTENT_TOP + row * (CARD_H + GAP_Y), CARD_W, CARD_H);
        }
    }

    private float contentBottom() {
        int rows = (Game.FLOOR_COUNT + 1) / 2;
        return CONTENT_TOP + rows * CARD_H + (rows - 1) * GAP_Y;
    }

    private float maxScroll() {
        return Math.max(0f, contentBottom() - VIEW_BOTTOM);
    }

    @Override
    public void update(Game game, float dt) {
        time += dt;
        UiKit.tickFade(game, dt);

        if (!dragging) {
            if (Math.abs(velocity) > 4f) {
                scroll += velocity * dt;
                velocity *= (float) Math.exp(-5.5f * dt);
                float max = maxScroll();
                if (scroll < 0f) {
                    scroll += (0f - scroll) * Math.min(1f, dt * 10f);
                    velocity = 0f;
                } else if (scroll > max) {
                    scroll += (max - scroll) * Math.min(1f, dt * 10f);
                    velocity = 0f;
                }
            } else {
                velocity = 0f;
                float max = maxScroll();
                if (scroll < 0f) scroll = MathUtil.approach(scroll, 0f, 14f, dt);
                if (scroll > max) scroll = MathUtil.approach(scroll, max, 14f, dt);
            }
        }
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @Override
    public void render(Game game, Renderer r) {
        UiBackdrop.menu(r, time, Palette.MINT, "LEVELS");

        // 顶部标题
        UiKit.header(r, Texts.BTN_LEVELS, null, time);
        // 已解锁数量
        r.setTextSize(22f);
        r.setTextBold(false);
        r.setTextAlign(2);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.95f));
        sb.setLength(0);
        sb.append("已解锁 ").append(game.save.unlockedFloor).append(" / ").append(Game.FLOOR_COUNT);
        r.drawText(sb.toString(), UiKit.W - UiKit.MARGIN, UiKit.SAFE_TOP + 58f);
        // 返回
        UiKit.backCircle(r, back, backPressed, true);

        // 器官收集面板
        UiKit.panel(r, organPanel, Palette.PURPLE, time);
        drawOrganStrip(r, game);

        // 卡片列表（裁剪滚动区）
        r.save();
        r.clipRect(0f, VIEW_TOP, UiKit.W, VIEW_BOTTOM - VIEW_TOP);
        for (int i = 0; i < Game.FLOOR_COUNT; i++) {
            drawCard(r, game, i);
        }
        r.restore();
        // 顶部渐隐，暗示可滚动
        r.fillVerticalGradient(0f, VIEW_TOP, UiKit.W, 26f, Palette.alpha(0xFF07090D, 0f),
                Palette.alpha(0xFF07090D, 0.75f));
        // 轻量底部渐隐，柔化滚动裁切
        r.fillVerticalGradient(0f, VIEW_BOTTOM - 16f, UiKit.W, 16f,
                Palette.alpha(0xFF07090D, 0f), Palette.alpha(0xFF07090D, 0.55f));
        drawScrollbar(r);
        drawPageDots(r);

        // 底部提示 + 总统计
        float hintY = 1204f;
        UiKit.hint(r, MenuTexts.DRAG_HINT, hintY, 0.9f);
        r.setTextSize(19f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT_DARK, 0.9f));
        r.drawText(statsLine, UiKit.W * 0.5f, 1232f);
    }

    private void drawOrganStrip(Renderer r, Game game) {
        int count = 0;
        for (Organ o : organs) {
            if (game.save.hasOrgan(o)) count++;
        }
        float iconY = organPanel.y + 40f;
        float labelY = organPanel.y + 80f;
        float areaX = organPanel.x + 20f;
        float areaW = organPanel.w - 150f;
        float cell = areaW / organs.length;
        for (int i = 0; i < organs.length; i++) {
            Organ o = organs[i];
            boolean lit = game.save.hasOrgan(o);
            float cx = areaX + cell * (i + 0.5f);
            UiKit.organPlate(r, cx, iconY, 24f, lit);
            UiKit.organ(r, o, cx, iconY, 40f, lit);
            r.setTextSize(18f);
            r.setTextBold(lit);
            r.setTextAlign(1);
            r.setTextColor(lit ? Palette.alpha(UiKit.organColor(o), 0.95f)
                    : Palette.alpha(Palette.TEXT_DARK, 0.9f));
            r.drawText(o.label, cx, labelY);
        }
        // 计数
        float cx = organPanel.x + organPanel.w - 74f;
        r.setTextSize(20f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.95f));
        r.drawText(MenuTexts.ORGANS, cx, organPanel.y + 34f);
        r.setTextSize(40f);
        r.setTextBold(true);
        r.setTextColor(count == organs.length ? Palette.GOLD : Palette.MINT);
        r.drawText(count + " / " + organs.length, cx, organPanel.y + 72f);
    }

    private void drawCard(Renderer r, Game game, int index) {
        int floor = index + 1;
        Rect c = cardRect[index];
        float y = c.y - scroll;
        if (y > VIEW_BOTTOM || y + c.h < VIEW_TOP) return;
        boolean unlocked = game.isFloorUnlocked(floor);
        boolean cleared = floor < game.save.unlockedFloor;
        boolean current = floor == game.save.unlockedFloor;
        LevelDef def = Levels.get(floor);
        boolean boss = floor == Game.FLOOR_COUNT;
        boolean pressed = pressedCard == index;
        float scale = pressed ? 0.965f : 1f;

        r.save();
        r.translate(c.x + c.w * 0.5f, y + c.h * 0.5f);
        r.scale(scale, scale);
        float x = -c.w * 0.5f, yy = -c.h * 0.5f;

        int accent = boss ? Palette.GOLD : (unlocked ? Palette.MINT : Palette.TEXT_DARK);
        int fill = unlocked ? UiKit.PANEL_LIGHT : 0xFF11151B;
        // 投影
        r.fillRoundRect(x + 3f, yy + 7f, c.w, c.h, 22f, Palette.alpha(Palette.BLACK, 0.55f));
        r.fillRoundRect(x, yy, c.w, c.h, 22f, Palette.alpha(fill, 0.97f));
        // 顶部渐变高光
        r.fillRoundRect(x + 2f, yy + 2f, c.w - 4f, c.h * 0.40f, 20f, Palette.alpha(Palette.WHITE, 0.035f));
        r.strokeRoundRect(x, yy, c.w, c.h, 22f, 2.5f,
                Palette.alpha(unlocked ? Palette.PANEL_STROKE : 0xFF252C35, 0.95f));
        // 左侧主题色条
        r.fillRoundRect(x, yy + 18f, 6f, c.h - 36f, 3f, Palette.alpha(accent, unlocked ? 0.9f : 0.35f));
        // 角标
        UiKit.cornerBrackets(r, new Rect(x + 6f, yy + 6f, c.w - 12f, c.h - 12f), 16f,
                Palette.alpha(accent, unlocked ? 0.5f : 0.22f));

        // 右下角大楼层号水印
        r.setTextSize(96f);
        r.setTextBold(true);
        r.setTextAlign(2);
        r.setTextColor(Palette.alpha(accent, unlocked ? 0.13f : 0.07f));
        r.drawText(floorNum[index], c.w * 0.5f - 16f, c.h * 0.5f - 2f);

        // 楼层 & 状态标签
        r.setTextSize(19f);
        r.setTextBold(false);
        r.setTextAlign(0);
        r.setTextColor(Palette.alpha(unlocked ? Palette.TEXT_DIM : Palette.TEXT_DARK, 0.95f));
        sb.setLength(0);
        sb.append("第 ").append(floor).append(" 层");
        r.drawText(sb.toString(), x + 22f, yy + 26f);
        if (boss) {
            UiKit.badge(r, x + c.w - 56f, yy + 26f, MenuTexts.BOSS_TAG, Palette.GOLD, 17f);
        } else if (cleared) {
            UiKit.iconStar(r, x + c.w - 44f, yy + 26f, 10f, Palette.alpha(Palette.GOLD, 0.9f));
        }

        // 名称 / 副标题
        UiKit.textFit(r, def.name, x + 22f, yy + 66f, c.w - 48f,
                boss ? 34f : 33f, 22f,
                unlocked ? Palette.TEXT : Palette.alpha(Palette.TEXT_DARK, 0.85f), 0, true);
        UiKit.textFit(r, def.subtitle, x + 22f, yy + 103f, c.w - 48f, 21f, 17f,
                Palette.alpha(unlocked ? Palette.TEXT_DIM : Palette.TEXT_DARK, 0.95f), 0, false);

        // 分隔线
        r.fillRect(x + 22f, yy + 126f, c.w - 44f, 1.5f,
                Palette.alpha(unlocked ? Palette.PANEL_STROKE : 0xFF252C35, 0.8f));

        // 状态行
        float statusY = yy + 152f;
        if (!unlocked) {
            UiKit.iconLock(r, x + 36f, statusY, 26f, Palette.alpha(Palette.TEXT_DARK, 0.95f));
            r.setTextSize(21f);
            r.setTextBold(false);
            r.setTextAlign(0);
            r.setTextColor(Palette.alpha(Palette.TEXT_DARK, 0.95f));
            r.drawText(MenuTexts.LOCKED, x + 58f, statusY);
        } else if (cleared) {
            UiKit.iconStar(r, x + 34f, statusY, 13f, Palette.GOLD);
            r.setTextSize(21f);
            r.setTextBold(true);
            r.setTextAlign(0);
            r.setTextColor(Palette.alpha(Palette.GOLD, 0.95f));
            r.drawText(MenuTexts.CLEARED, x + 54f, statusY);
        } else if (current) {
            UiKit.iconArrow(r, x + 40f, statusY, 30f, Palette.MINT);
            r.setTextSize(21f);
            r.setTextBold(true);
            r.setTextAlign(0);
            r.setTextColor(Palette.MINT);
            r.drawText(MenuTexts.CHALLENGE, x + 60f, statusY);
            float blink = 0.35f + 0.35f * UiKit.blink(time, 1.9f, 0f);
            r.fillCircle(x + c.w - 34f, statusY, 5f, Palette.alpha(Palette.MINT, blink));
        } else {
            r.setTextSize(21f);
            r.setTextBold(false);
            r.setTextAlign(0);
            r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.9f));
            r.drawText(unlocked ? "点击进入" : MenuTexts.LOCKED, x + 22f, statusY);
        }

        if (!unlocked) {
            // 未解锁整卡压暗
            r.fillRoundRect(x, yy, c.w, c.h, 22f, Palette.alpha(Palette.BG_DARK, 0.35f));
        }
        if (pressed) {
            r.fillRoundRect(x, yy, c.w, c.h, 22f, Palette.alpha(Palette.WHITE, 0.06f));
        }
        r.restore();
    }

    private void drawScrollbar(Renderer r) {
        float max = maxScroll();
        if (max <= 1f) return;
        float trackX = UiKit.W - 14f;
        float trackTop = VIEW_TOP + 6f;
        float trackH = VIEW_BOTTOM - VIEW_TOP - 12f;
        r.fillRoundRect(trackX - 3f, trackTop, 6f, trackH, 3f, Palette.alpha(Palette.BLACK, 0.35f));
        float visible = VIEW_BOTTOM - VIEW_TOP;
        float contentH = contentBottom() - CONTENT_TOP;
        float ratio = MathUtil.clamp(visible / contentH, 0.16f, 1f);
        float thumbH = trackH * ratio;
        float t = max <= 0f ? 0f : MathUtil.clamp(scroll / max, 0f, 1f);
        float thumbY = trackTop + (trackH - thumbH) * t;
        r.fillRoundRect(trackX - 3f, thumbY, 6f, thumbH, 3f, Palette.alpha(Palette.MINT, 0.55f));
    }

    /** 滚动页指示点：两页（1~6 层 / 7~10 层）。 */
    private void drawPageDots(Renderer r) {
        float max = maxScroll();
        int page = max <= 1f ? 0 : (scroll > max * 0.5f ? 1 : 0);
        int pages = max <= 1f ? 1 : 2;
        float gap = 22f;
        float startX = UiKit.W * 0.5f - (pages - 1) * gap * 0.5f;
        for (int i = 0; i < pages; i++) {
            if (i == page) {
                r.fillRoundRect(startX + i * gap - 12f, 1174f, 24f, 8f, 4f, Palette.MINT);
            } else {
                r.fillCircle(startX + i * gap, 1178f, 4f, Palette.alpha(Palette.TEXT_DARK, 0.9f));
            }
        }
    }

    // ------------------------------------------------------------------
    // 触摸
    // ------------------------------------------------------------------

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        if (pointer >= 0) return;
        pointer = id;
        downY = y;
        lastY = y;
        dragStartY = y;
        dragging = false;
        velocity = 0f;
        backPressed = back.contains(x, y);
        pressedCard = hitCard(x, y);
        if (backPressed) game.audio.play(Sfx.UI_CLICK);
    }

    @Override
    public void touchMove(Game game, int id, float x, float y) {
        if (id != pointer) return;
        float dy = y - lastY;
        lastY = y;
        if (!dragging) {
            if (Math.abs(y - downY) > 14f) {
                dragging = true;
                pressedCard = -1;
                backPressed = false;
            } else {
                return;
            }
        }
        float max = maxScroll();
        float next = scroll - dy;
        // 越界阻尼
        if (next < 0f) next = next * 0.35f;
        else if (next > max) next = max + (next - max) * 0.35f;
        velocity = -dy / Math.max(0.001f, 0.0166f);
        scroll = next;
        if (Math.abs(y - downY) > 14f) {
            // 拖动后清除按压态
            pressedCard = -1;
        }
    }

    @Override
    public void touchUp(Game game, int id, float x, float y) {
        if (id != pointer) return;
        pointer = -1;
        if (backPressed && back.contains(x, y)) {
            backPressed = false;
            game.audio.play(Sfx.UI_BACK);
            game.gotoTitle();
            return;
        }
        backPressed = false;
        if (!dragging) {
            int card = hitCard(x, y);
            if (card >= 0) {
                pressedCard = -1;
                if (game.isFloorUnlocked(card + 1)) {
                    game.audio.play(Sfx.UI_CONFIRM);
                    game.gotoIntro(card + 1);
                } else {
                    game.audio.play(Sfx.UI_BACK);
                }
                return;
            }
        }
        pressedCard = -1;
        // 速度过大时钳制，避免甩飞
        float maxV = 2600f;
        velocity = MathUtil.clamp(velocity, -maxV, maxV);
        if (Math.abs(velocity) < 220f) velocity = 0f;
        dragging = false;
    }

    /** 命中卡片（考虑滚动偏移），返回 0-based 索引或 -1。 */
    private int hitCard(float x, float y) {
        if (y < VIEW_TOP || y > VIEW_BOTTOM) return -1;
        for (int i = 0; i < Game.FLOOR_COUNT; i++) {
            Rect c = cardRect[i];
            float cy = c.y - scroll;
            if (x >= c.x && x <= c.x + c.w && y >= cy && y <= cy + c.h) return i;
        }
        return -1;
    }

    @Override
    public void backPressed(Game game) {
        game.audio.play(Sfx.UI_BACK);
        game.gotoTitle();
    }
}
