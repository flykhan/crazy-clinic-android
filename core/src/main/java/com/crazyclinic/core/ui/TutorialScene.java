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
import com.crazyclinic.core.world.Organ;

/**
 * 操作教学：4 页图文（目标流程 / 双手操作 / HUD / 器官强化）。
 * 示意图全部程序化绘制（手机外形 + 拇指圆点 + 摇杆 + 按钮 + HUD 结构）。
 */
public final class TutorialScene implements Scene {
    /** 标题页进入时：教学看完直接开始第 1 层简报。 */
    public static boolean pendingStartGame;
    /** 简报页进入时：教学看完回到该层简报；0 = 回标题。 */
    public static int returnFloor;

    private static final String[] FALLBACK_BODIES = {
            "欢迎来到疯狂医院。\n\n你是卡特，一个失忆的病人。\n医院里的人并不想让你离开。\n\n"
                    + "目标：找到钥匙卡，黑入安保电脑，坐电梯逃到下一层。",
            "左半屏拖动 = 移动，推满 = 冲刺。\n右半屏拖动 = 瞄准，按住自动开火。\n\n"
                    + "右下按钮：攻击 / 互动 / 闪避 / 换武器 / 用道具。\n\n"
                    + "靠近门、电脑、储物柜、电梯时会出现互动提示。",
            "红色血条 = 生命，金色体力 = 冲刺和闪避。\n\n靠近门、电脑、储物柜、电梯时，会出现互动按钮提示。\n\n"
                    + "记住：枪声会引来敌人，能绕后就绕后。",
            "打死敌人可能掉落器官，拾取后永久强化自己。\n\n"
                    + "每一层都有不同的病区主题，最后一层在天台——那里有人在等你。\n\n祝你好运，卡特。"
    };

    private final Rect back = new Rect(34f, 72f, 76f, 76f);
    private final Rect diagram = new Rect(48f, 168f, 624f, 392f);
    private final Rect textPanel = new Rect(48f, 576f, 624f, 378f);
    private final UiButton prev = new UiButton(MenuTexts.PREV, UiKit.STYLE_SECONDARY);
    private final UiButton next = new UiButton(MenuTexts.NEXT, UiKit.STYLE_PRIMARY);
    private final UiButton skip = new UiButton(MenuTexts.SKIP, UiKit.STYLE_GHOST);

    private int page;
    private int pageCount;
    private float time;
    private float pageAnim;
    private int pointer = -1;
    private boolean backPressed;
    private String countText = "";

    public TutorialScene() {
        pageCount = Math.max(1, Math.min(8, Texts.TUTORIAL_PAGES.length > 0
                ? Texts.TUTORIAL_PAGES.length : FALLBACK_BODIES.length));
    }

    @Override
    public void enter(Game game) {
        game.audio.music(Music.TITLE);
        game.fade = 1f;
        time = 0f;
        page = 0;
        pointer = -1;
        backPressed = false;
        pageAnim = 0f;
        prev.set(48f, 1024f, 190f, 92f).resetAppear();
        next.set(258f, 1024f, 250f, 92f).resetAppear();
        skip.set(528f, 1024f, 144f, 92f).resetAppear();
        prev.appearDelay = 0.05f;
        next.appearDelay = 0.09f;
        skip.appearDelay = 0.13f;
        refreshNextLabel();
        countText = (page + 1) + " / " + pageCount;
    }

    private void refreshNextLabel() {
        boolean last = page >= pageCount - 1;
        next.label(last ? MenuTexts.FINISH : MenuTexts.NEXT).style(UiKit.STYLE_PRIMARY);
        prev.enabled = page > 0;
        countText = (page + 1) + " / " + pageCount;
    }

    private String body(int index) {
        if (Texts.TUTORIAL_PAGES.length > 0) {
            return Texts.TUTORIAL_PAGES[Math.min(index, Texts.TUTORIAL_PAGES.length - 1)];
        }
        return FALLBACK_BODIES[Math.min(index, FALLBACK_BODIES.length - 1)];
    }

    @Override
    public void update(Game game, float dt) {
        time += dt;
        UiKit.tickFade(game, dt);
        pageAnim = MathUtil.approach(pageAnim, 1f, 12f, dt);
        prev.update(dt);
        next.update(dt);
        skip.update(dt);
        refreshNextLabel();
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @Override
    public void render(Game game, Renderer r) {
        UiBackdrop.menu(r, time, Palette.MINT, "HOW TO PLAY");
        UiKit.header(r, "操作说明", countText, time);
        UiKit.backCircle(r, back, backPressed, true);

        // 页面内容淡入/位移
        float a = MathUtil.easeOutCubic(pageAnim);
        float dx = (1f - a) * 36f;
        r.save();
        r.setAlpha(0.25f + 0.75f * a);
        r.translate(dx, 0f);

        UiKit.panel(r, diagram, Palette.MINT, time);
        drawDiagram(r, page % 4);

        UiKit.panel(r, textPanel, Palette.CYAN, time);
        r.setTextSize(38f);
        r.setTextBold(true);
        r.setTextAlign(0);
        r.setTextColor(Palette.MINT);
        r.drawText(MenuTexts.TUTORIAL_TITLES[page % MenuTexts.TUTORIAL_TITLES.length],
                textPanel.x + 34f, textPanel.y + 50f);
        r.fillRect(textPanel.x + 34f, textPanel.y + 76f, textPanel.w - 68f, 1.5f,
                Palette.alpha(Palette.MINT, 0.25f));
        float bodyEnd = UiKit.wrapped(r, body(page), textPanel.x + 34f, textPanel.y + 108f,
                textPanel.w - 68f, 23f, Palette.alpha(Palette.TEXT, 0.94f), 9f);
        // 小贴士（正文放得下时才画）
        float tipY = textPanel.y + textPanel.h - 54f;
        if (bodyEnd < tipY - 26f) {
            String tip = MenuTexts.TUTORIAL_TIPS[page % MenuTexts.TUTORIAL_TIPS.length];
            r.fillRoundRect(textPanel.x + 24f, tipY - 26f, textPanel.w - 48f, 54f, 12f,
                    Palette.alpha(UiKit.HAZARD, 0.10f));
            r.fillRoundRect(textPanel.x + 24f, tipY - 26f, 5f, 54f, 2.5f,
                    Palette.alpha(UiKit.HAZARD, 0.8f));
            r.setTextSize(15f);
            r.setTextBold(true);
            r.setTextAlign(0);
            r.setTextColor(Palette.alpha(UiKit.HAZARD, 0.95f));
            r.drawText(MenuTexts.TUTORIAL_TIP_TITLE, textPanel.x + 44f, tipY - 8f);
            r.setTextSize(18f);
            r.setTextBold(false);
            r.setTextColor(Palette.alpha(Palette.TEXT, 0.88f));
            r.drawText(tip, textPanel.x + 44f, tipY + 16f);
        }
        r.restore();

        drawDots(r);
        prev.draw(r, time);
        next.draw(r, time);
        skip.draw(r, time);

        UiKit.hint(r, "提示：任何时候都可以跳过教学", 1164f, 0.75f);
    }

    private void drawDots(Renderer r) {
        float gap = 26f;
        float startX = UiKit.W * 0.5f - (pageCount - 1) * gap * 0.5f;
        for (int i = 0; i < pageCount; i++) {
            boolean active = i == page;
            float cx = startX + i * gap;
            if (active) {
                r.fillRoundRect(cx - 16f, 981f, 32f, 10f, 5f, Palette.MINT);
            } else {
                r.fillCircle(cx, 986f, 5f, Palette.alpha(Palette.TEXT_DARK, 0.85f));
            }
        }
    }

    // ------------------------------------------------------------------
    // 示意图
    // ------------------------------------------------------------------

    private void drawDiagram(Renderer r, int kind) {
        switch (kind) {
            case 0:
                diagramGoal(r);
                break;
            case 1:
                diagramHands(r);
                break;
            case 2:
                diagramHud(r);
                break;
            default:
                diagramOrgans(r);
                break;
        }
    }

    /** 第 1 页：钥匙卡 → 电脑 → 电梯 的流程小地图。 */
    private void diagramGoal(Renderer r) {
        float x = diagram.x, y = diagram.y, w = diagram.w, h = diagram.h;
        UiKit.tape(r, x + w * 0.5f, y + 40f, 400f, 34f, "只 有 电 梯 能 离 开", time * 10f, -1.2f);

        float rw = 150f, gap = 32f;
        float startX = x + (w - (rw * 3f + gap * 2f)) * 0.5f;
        float ry = y + 96f, rh = 188f;
        float r1 = startX, r2 = startX + rw + gap, r3 = startX + (rw + gap) * 2f;
        int[] tints = {Palette.MINT, Palette.CYAN, Palette.GOLD};

        // 走廊
        float corY = ry + rh * 0.5f - 30f;
        r.fillRect(r1 + rw, corY, gap, 60f, Palette.alpha(Palette.FLOOR_A, 0.9f));
        r.fillRect(r2 + rw, corY, gap, 60f, Palette.alpha(Palette.FLOOR_A, 0.9f));
        r.fillRect(r1 + rw, corY + 28f, gap, 1.5f, Palette.alpha(Palette.FLOOR_GROUT, 0.9f));
        r.fillRect(r2 + rw, corY + 28f, gap, 1.5f, Palette.alpha(Palette.FLOOR_GROUT, 0.9f));

        room(r, r1, ry, rw, rh, tints[0]);
        room(r, r2, ry, rw, rh, tints[1]);
        room(r, r3, ry, rw, rh, tints[2]);

        // 房间内道具
        UiKit.iconKey(r, r1 + rw * 0.5f, ry + 84f, 68f, Palette.GOLD);
        UiKit.iconComputer(r, r2 + rw * 0.5f, ry + 84f, 70f, Palette.CYAN);
        UiKit.iconElevator(r, r3 + rw * 0.5f, ry + 84f, 70f, Palette.MINT);
        label(r, "钥匙卡", r1 + rw * 0.5f, ry + rh - 28f, Palette.GOLD);
        label(r, "安保电脑", r2 + rw * 0.5f, ry + rh - 28f, Palette.CYAN);
        label(r, "电梯", r3 + rw * 0.5f, ry + rh - 28f, Palette.MINT);

        // 玩家（左房左上角）
        float px = r1 + 28f, py = ry + 40f;
        r.fillCircle(px, py, 15f, Palette.alpha(Palette.GREEN, 0.16f));
        r.fillCircle(px, py, 8f, Palette.alpha(Palette.GREEN, 0.95f));
        r.setTextSize(13f);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.GREEN, 0.9f));
        r.drawText("你", px, py + 24f);
        // 敌人（固定位置）
        enemyDot(r, r2 + 32f, ry + rh - 52f);
        enemyDot(r, r3 + rw - 30f, ry + rh - 58f);
        enemyDot(r, r2 + rw - 34f, ry + 34f);

        // 走廊内的流程箭头
        arrowBetween(r, r1 + rw + 6f, corY + 30f, r2 - 6f, corY + 30f,
                Palette.alpha(Palette.MINT, 0.95f));
        arrowBetween(r, r2 + rw + 6f, corY + 30f, r3 - 6f, corY + 30f,
                Palette.alpha(Palette.MINT, 0.95f));

        r.setTextSize(18f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.9f));
        r.drawText("先拿钥匙卡，黑入电脑关监控，最后坐电梯离开", x + w * 0.5f, y + h - 24f);
    }

    private void enemyDot(Renderer r, float x, float y) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 3.0 + x);
        r.fillCircle(x, y, 12f, Palette.alpha(Palette.RED, 0.10f + 0.10f * pulse));
        r.fillCircle(x, y, 6f, Palette.alpha(Palette.RED, 0.9f));
    }

    private void room(Renderer r, float x, float y, float w, float h, int tint) {
        r.fillRoundRect(x, y, w, h, 12f, Palette.alpha(0xFF1E252E, 0.95f));
        r.strokeRoundRect(x, y, w, h, 12f, 2f, Palette.alpha(tint, 0.5f));
        r.fillRect(x + 12f, y + 12f, w - 24f, 4f, Palette.alpha(tint, 0.22f));
        r.fillRect(x + w * 0.5f, y + 16f, 1f, h - 32f, Palette.alpha(Palette.FLOOR_GROUT, 0.5f));
    }

    private void label(Renderer r, String s, float cx, float cy, int color) {
        r.setTextSize(19f);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(color, 0.95f));
        r.drawText(s, cx, cy);
    }

    private void arrowBetween(Renderer r, float x1, float y1, float x2, float y2, int color) {
        float len = MathUtil.dist(x1, y1, x2, y2);
        float ang = (float) Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
        r.save();
        r.translate(x1, y1);
        r.rotate(ang);
        UiKit.iconArrow(r, len * 0.5f, 0f, len, color);
        r.restore();
    }

    /** 第 2 页：手机 + 双摇杆 + 拇指圆点 + 按钮图例。 */
    private void diagramHands(Renderer r) {
        float x = diagram.x, y = diagram.y, w = diagram.w, h = diagram.h;
        float pw = 280f, ph = 300f;
        float px = x + (w - pw) * 0.5f;
        float py = y + 12f;
        UiKit.iconPhone(r, px, py, pw, ph, Palette.alpha(UiKit.STEEL, 0.9f));

        float sx = px + 12f, sy = py + 28f, sw = pw - 24f, sh = ph - 66f;
        r.save();
        r.clipRect(sx, sy, sw, sh);
        r.fillVerticalGradient(sx, sy, sw, sh, 0xFF2F3742, 0xFF1B2129);
        for (int i = 0; i < 4; i++) {
            r.fillRect(sx, sy + sh * (i + 1) / 5f, sw, 1.5f, Palette.alpha(Palette.FLOOR_A, 0.55f));
            r.fillRect(sx + sw * (i + 1) / 5f, sy, 1.5f, sh, Palette.alpha(Palette.FLOOR_A, 0.55f));
        }
        // 左侧移动摇杆
        float lx = sx + 62f, ly = sy + sh - 58f;
        r.fillCircle(lx, ly, 38f, Palette.alpha(Palette.BLACK, 0.30f));
        r.strokeCircle(lx, ly, 38f, 2f, Palette.alpha(UiKit.STEEL, 0.6f));
        float tx = lx + (float) Math.cos(time * 1.3f) * 22f;
        float ty = ly + (float) Math.sin(time * 1.3f) * 22f;
        r.fillCircle(tx, ty, 15f, Palette.alpha(Palette.MINT, 0.95f));
        r.strokeCircle(tx, ty, 15f, 1.5f, Palette.alpha(UiKit.MINT_GLOW, 0.8f));

        // 右侧瞄准：拇指 + 方向虚线 + 准星
        float rx = sx + sw - 52f, ry = sy + sh - 96f;
        r.fillCircle(rx, ry, 15f, Palette.alpha(Palette.RED, 0.95f));
        float aim = time * 1.0f - 1.9f;
        float adx = (float) Math.cos(aim), ady = (float) Math.sin(aim);
        for (int i = 1; i <= 3; i++) {
            float t0 = 26f + i * 22f;
            r.line(rx + adx * t0, ry + ady * t0,
                    rx + adx * (t0 + 12f), ry + ady * (t0 + 12f), 2.5f,
                    Palette.alpha(Palette.RED, 0.55f));
        }
        float crx = rx + adx * 106f, cry = ry + ady * 106f;
        r.strokeCircle(crx, cry, 12f, 2f, Palette.alpha(Palette.RED, 0.8f));
        r.fillCircle(crx, cry, 3f, Palette.alpha(Palette.RED, 0.9f));

        // 右下按钮簇
        r.fillCircle(sx + sw - 34f, sy + sh - 30f, 19f, Palette.alpha(Palette.RED, 0.9f));
        r.strokeCircle(sx + sw - 34f, sy + sh - 30f, 19f, 1.5f, Palette.alpha(Palette.WHITE, 0.3f));
        r.fillCircle(sx + sw - 88f, sy + sh - 24f, 12f, Palette.alpha(Palette.CYAN, 0.9f));
        r.fillCircle(sx + sw - 34f, sy + sh - 74f, 12f, Palette.alpha(Palette.BLUE, 0.9f));
        r.restore();

        // 手机外侧说明
        r.setTextSize(20f);
        r.setTextBold(true);
        r.setTextAlign(2);
        r.setTextColor(Palette.MINT);
        r.drawText("左半屏", x + 156f, y + 116f);
        r.setTextSize(17f);
        r.setTextBold(false);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.95f));
        r.drawText("拖动移动", x + 156f, y + 144f);
        r.drawText("推满冲刺", x + 156f, y + 168f);

        r.setTextSize(20f);
        r.setTextBold(true);
        r.setTextAlign(0);
        r.setTextColor(Palette.ALERT);
        r.drawText("右半屏", x + w - 156f, y + 116f);
        r.setTextSize(17f);
        r.setTextBold(false);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.95f));
        r.drawText("拖动瞄准", x + w - 156f, y + 144f);
        r.drawText("按住开火", x + w - 156f, y + 168f);

        // 图例
        float legendY = y + h - 34f;
        String[] names = {"攻击", "互动", "闪避", "武器", "道具"};
        int[] colors = {Palette.RED, Palette.CYAN, Palette.BLUE, Palette.MINT, Palette.GOLD};
        float cell = (w - 80f) / names.length;
        for (int i = 0; i < names.length; i++) {
            float cx = x + 40f + cell * (i + 0.5f);
            r.fillCircle(cx - 28f, legendY, 8f, Palette.alpha(colors[i], 0.9f));
            r.setTextSize(18f);
            r.setTextBold(true);
            r.setTextAlign(0);
            r.setTextColor(Palette.alpha(colors[i], 0.95f));
            r.drawText(names[i], cx - 14f, legendY);
        }
    }

    /** 第 3 页：HUD 结构示意。 */
    private void diagramHud(Renderer r) {
        float x = diagram.x, y = diagram.y, w = diagram.w, h = diagram.h;
        float mx = x + 36f, my = y + 16f, mw = w - 72f, mh = h - 66f;
        r.fillRoundRect(mx, my, mw, mh, 14f, Palette.alpha(0xFF151A21, 0.96f));
        r.strokeRoundRect(mx, my, mw, mh, 14f, 2f, Palette.alpha(UiKit.STEEL, 0.35f));

        float topH = 100f;
        float bottomH = 118f;
        float worldY = my + topH, worldH = mh - topH - bottomH;

        // 世界区：简易病房
        r.fillVerticalGradient(mx + 6f, worldY, mw - 12f, worldH, 0xFF343C48, 0xFF232A33);
        r.fillRect(mx + 6f, worldY, mw - 12f, 16f, Palette.alpha(0xFF525D6D, 0.85f));
        r.fillRect(mx + 6f, worldY + 16f, mw - 12f, 5f, Palette.alpha(UiKit.HAZARD, 0.20f));
        // 病床 + 柜子 + 门
        r.fillRoundRect(mx + 34f, worldY + 34f, 86f, 40f, 6f, Palette.alpha(Palette.BED, 0.30f));
        r.fillRoundRect(mx + 40f, worldY + 40f, 74f, 14f, 4f, Palette.alpha(Palette.WHITE, 0.10f));
        r.fillRoundRect(mx + mw - 130f, worldY + 30f, 40f, 56f, 4f,
                Palette.alpha(Palette.LOCKER, 0.55f));
        r.fillRoundRect(mx + mw - 210f, worldY + 26f, 46f, 70f, 4f,
                Palette.alpha(Palette.DOOR, 0.45f));
        r.fillCircle(mx + mw - 168f, worldY + 60f, 2.5f, Palette.alpha(Palette.GOLD, 0.8f));
        r.save();
        r.translate(mx + 116f, worldY + worldH - 36f);
        r.scale(1f, 0.38f);
        r.fillCircle(0f, 0f, 54f, Palette.alpha(Palette.MINT, 0.10f));
        r.restore();
        r.fillCircle(mx + 116f, worldY + worldH - 36f, 12f, Palette.alpha(Palette.GREEN, 0.7f));

        // 顶部 HUD 板
        r.fillRect(mx + 6f, my + 6f, mw - 12f, topH - 8f, Palette.alpha(0xFF0B0F14, 0.82f));
        r.fillCircle(mx + 46f, my + 40f, 24f, 0xFF2A323D);
        r.strokeCircle(mx + 46f, my + 40f, 24f, 2f, Palette.alpha(UiKit.STEEL, 0.55f));
        r.fillCircle(mx + 46f, my + 42f, 14f, 0xFFF0CBAE);
        r.fillArc(mx + 46f, my + 35f, 15f, 180f, 180f, 0xFFE1C424);
        UiKit.progressBar(r, mx + 84f, my + 24f, 190f, 16f, 0.72f, Palette.RED, 0xFF2A0E0E,
                Palette.alpha(UiKit.STEEL, 0.5f));
        r.setTextSize(13f);
        r.setTextBold(true);
        r.setTextAlign(0);
        r.setTextColor(Palette.alpha(Palette.TEXT, 0.85f));
        r.drawText("生命", mx + 84f, my + 56f);
        r.setTextColor(Palette.alpha(Palette.GOLD, 0.9f));
        r.drawText("体力", mx + 132f, my + 56f);
        for (int i = 0; i < 6; i++) {
            r.fillCircle(mx + 180f + i * 16f, my + 56f, 5f, Palette.alpha(Palette.GOLD, 0.85f));
        }
        r.fillRoundRect(mx + mw - 164f, my + 20f, 54f, 34f, 8f, Palette.alpha(Palette.PANEL_ALT, 0.92f));
        UiKit.iconCrosshair(r, mx + mw - 137f, my + 37f, 28f, Palette.alpha(Palette.TEXT, 0.9f));
        r.setTextSize(16f);
        r.setTextBold(true);
        r.setTextAlign(2);
        r.setTextColor(Palette.TEXT);
        r.drawText("12 / 24", mx + mw - 20f, my + 37f);
        r.fillRoundRect(mx + 18f, my + 70f, mw - 36f, 22f, 5f, Palette.alpha(Palette.BLACK, 0.45f));
        r.setTextSize(14f);
        r.setTextBold(false);
        r.setTextAlign(0);
        r.setTextColor(Palette.alpha(Palette.MINT, 0.95f));
        r.drawText("目标：找到钥匙卡 → 黑入电脑 → 电梯", mx + 30f, my + 81f);

        // 底部控制区
        float by = my + topH + worldH;
        r.fillRect(mx + 6f, by, mw - 12f, mh - topH - worldH - 6f, Palette.alpha(Palette.BLACK, 0.42f));
        float cy = by + (mh - topH - worldH) * 0.52f;
        r.strokeCircle(mx + 64f, cy, 30f, 2f, Palette.alpha(UiKit.STEEL, 0.55f));
        r.fillCircle(mx + 64f, cy, 13f, Palette.alpha(Palette.MINT, 0.85f));
        r.fillRoundRect(mx + 116f, cy - 40f, 78f, 27f, 6f, Palette.alpha(Palette.PANEL_ALT, 0.95f));
        r.fillRoundRect(mx + 116f, cy + 9f, 78f, 27f, 6f, Palette.alpha(Palette.PANEL_ALT, 0.95f));
        r.setTextSize(14f);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(Palette.MINT);
        r.drawText("换武器", mx + 155f, cy - 26f);
        r.setTextColor(Palette.GOLD);
        r.drawText("道具", mx + 155f, cy + 23f);

        // 右下按钮（互不重叠）
        float ax = mx + mw - 86f, ay = cy - 10f;
        float ix = mx + mw - 154f, iy = cy + 30f;
        float dx = mx + mw - 36f, dy = cy + 28f;
        r.fillCircle(ax, ay, 34f, Palette.alpha(Palette.RED, 0.88f));
        r.fillCircle(ix, iy, 21f, Palette.alpha(Palette.CYAN, 0.88f));
        r.fillCircle(dx, dy, 18f, Palette.alpha(Palette.BLUE, 0.88f));
        r.setTextSize(15f);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(Palette.WHITE);
        r.drawText("攻击", ax, ay);
        r.setTextSize(13f);
        r.setTextColor(Palette.BG_DARK);
        r.drawText("互动", ix, iy);
        r.setTextColor(Palette.WHITE);
        r.drawText("闪避", dx, dy);

        r.setTextSize(17f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.9f));
        r.drawText("红色=生命 · 金色=体力 · 右下=攻击/互动/闪避", x + w * 0.5f, y + h - 22f);
    }

    /** 第 4 页：器官掉落与永久强化。 */
    private void diagramOrgans(Renderer r) {
        float x = diagram.x, y = diagram.y, w = diagram.w, h = diagram.h;
        UiKit.tape(r, x + w * 0.5f, y + 42f, 420f, 34f, "器 官 = 永 久 强 化", time * 9f, 1.0f);

        float ry = y + 116f;
        UiKit.iconSkull(r, x + 96f, ry, 76f, Palette.alpha(Palette.TEXT_DIM, 0.9f));
        arrowBetween(r, x + 152f, ry, x + 216f, ry, Palette.alpha(Palette.MINT, 0.7f));
        r.fillCircle(x + 274f, ry, 44f, Palette.alpha(Palette.PURPLE, 0.16f));
        r.fillCircle(x + 274f, ry, 36f, 0xFF232B36);
        r.strokeCircle(x + 274f, ry, 36f, 2.5f, Palette.alpha(Palette.MINT, 0.75f));
        UiKit.organ(r, Organ.HEART, x + 274f, ry, 58f, true);
        arrowBetween(r, x + 326f, ry, x + 390f, ry, Palette.alpha(Palette.MINT, 0.7f));

        float px = x + 460f;
        r.fillCircle(px, ry - 10f, 17f, 0xFFF0CBAE);
        r.fillArc(px, ry - 20f, 17f, 180f, 180f, 0xFFE1C424);
        r.fillRoundRect(px - 20f, ry + 8f, 40f, 40f, 10f, 0xFFDED6C3);
        r.fillCircle(px, ry + 6f, 4f, Palette.alpha(Palette.MINT, 0.9f));
        float glow = 0.5f + 0.5f * UiKit.pulse(time, 2.4f);
        r.fillCircle(px, ry + 10f, 55f, Palette.alpha(Palette.MINT, 0.05f + 0.06f * glow));
        r.setTextSize(18f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.9f));
        r.drawText("击倒掉落", x + 96f, ry + 92f);
        r.drawText("拾取器官", x + 274f, ry + 92f);
        r.drawText("永久强化", px, ry + 92f);

        Organ[] all = Organ.values();
        float areaW = w - 80f;
        float cell = areaW / all.length;
        float iy = y + h - 92f;
        for (int i = 0; i < all.length; i++) {
            float cx = x + 40f + cell * (i + 0.5f);
            UiKit.organPlate(r, cx, iy, 28f, true);
            UiKit.organ(r, all[i], cx, iy, 44f, true);
            r.setTextSize(15f);
            r.setTextBold(true);
            r.setTextAlign(1);
            r.setTextColor(Palette.alpha(UiKit.organColor(all[i]), 0.9f));
            r.drawText(all[i].label, cx, iy + 46f);
        }
    }

    // ------------------------------------------------------------------
    // 触摸
    // ------------------------------------------------------------------

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        if (pointer >= 0) return;
        pointer = id;
        backPressed = back.contains(x, y);
        if (prev.down(id, x, y) || next.down(id, x, y) || skip.down(id, x, y)) {
            game.audio.play(Sfx.UI_CLICK);
        } else if (backPressed) {
            game.audio.play(Sfx.UI_CLICK);
        }
    }

    @Override
    public void touchMove(Game game, int id, float x, float y) {
        if (id != pointer) return;
        if (backPressed && !back.contains(x, y)) backPressed = false;
        if (prev.pointer() == id && !prev.contains(x, y)) prev.cancel(id);
        if (next.pointer() == id && !next.contains(x, y)) next.cancel(id);
        if (skip.pointer() == id && !skip.contains(x, y)) skip.cancel(id);
    }

    @Override
    public void touchUp(Game game, int id, float x, float y) {
        if (id != pointer) return;
        pointer = -1;
        if (backPressed && back.contains(x, y)) {
            backPressed = false;
            finish(game);
            return;
        }
        backPressed = false;
        if (prev.up(id, x, y)) {
            if (page > 0) {
                page--;
                pageAnim = 0f;
                game.audio.play(Sfx.UI_CLICK);
            }
        } else if (next.up(id, x, y)) {
            if (page < pageCount - 1) {
                page++;
                pageAnim = 0f;
                game.audio.play(Sfx.UI_CLICK);
            } else {
                finish(game);
            }
        } else if (skip.up(id, x, y)) {
            finish(game);
        } else {
            prev.cancel(id);
            next.cancel(id);
            skip.cancel(id);
        }
    }

    private void finish(Game game) {
        game.save.tutorialSeen = true;
        game.saveNow();
        game.audio.play(Sfx.UI_CONFIRM);
        boolean startGame = pendingStartGame;
        int targetFloor = returnFloor;
        pendingStartGame = false;
        returnFloor = 0;
        if (startGame) {
            game.gotoIntro(1);
        } else if (targetFloor > 0) {
            game.gotoIntro(Math.min(Game.FLOOR_COUNT, targetFloor));
        } else {
            game.gotoTitle();
        }
    }

    @Override
    public void backPressed(Game game) {
        finish(game);
    }
}
