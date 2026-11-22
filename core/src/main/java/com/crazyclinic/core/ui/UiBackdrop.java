package com.crazyclinic.core.ui;

import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Renderer;

/**
 * 程序化菜单背景：医院走廊透视剪影、闪烁日光灯、漂浮尘埃、心电图、做旧网格。
 * 全部由 Renderer 图元绘制，不含位图。
 */
public final class UiBackdrop {
    private UiBackdrop() {}

    // ==================================================================
    // 子界面通用背景
    // ==================================================================

    /**
     * 常规模板：深色渐变 + 网格 + 尘埃 + 警示条 + 大水印 + 暗角。
     *
     * @param accent    主题强调色
     * @param watermark 右下角大字水印（null 则跳过）
     */
    public static void menu(Renderer r, float t, int accent, String watermark) {
        r.clear(Palette.BG_DARK);
        r.fillVerticalGradient(0f, 0f, UiKit.W, UiKit.H, 0xFF0D1117, 0xFF07090D);

        // 顶部主题辉光
        r.fillRadialGradient(UiKit.W * 0.5f, 150f, 620f,
                Palette.alpha(accent, 0.13f), 0x00000000);
        // 底部微弱薄荷反光
        r.fillRadialGradient(UiKit.W * 0.5f, UiKit.H + 60f, 520f,
                Palette.alpha(Palette.MINT, 0.06f), 0x00000000);

        grid(r, t);
        if (watermark != null) {
            r.setTextSize(220f);
            r.setTextBold(true);
            r.setTextAlign(2);
            r.setTextColor(Palette.alpha(Palette.WHITE, 0.028f));
            r.drawText(watermark, UiKit.W - 34f, UiKit.H * 0.62f);
        }
        dust(r, t, 22, 3, 0.5f);

        // 上下警戒条
        UiKit.hazard(r, 0f, UiKit.SAFE_TOP - 16f, UiKit.W, 7f, t * 9f,
                UiKit.HAZARD, UiKit.HAZARD_DARK, 0.55f);
        UiKit.hazard(r, 0f, UiKit.H - 26f, UiKit.W, 8f, -t * 7f,
                UiKit.HAZARD, UiKit.HAZARD_DARK, 0.40f);
        UiKit.vignette(r, 0.72f);
    }

    private static void grid(Renderer r, float t) {
        int color = Palette.alpha(Palette.WHITE, 0.022f);
        for (float x = 96f; x < UiKit.W; x += 96f) {
            r.line(x, 0f, x, UiKit.H, 1f, color);
        }
        for (float y = 96f; y < UiKit.H; y += 96f) {
            r.line(0f, y, UiKit.W, y, 1f, color);
        }
    }

    // ==================================================================
    // 标题背景：医院走廊
    // ==================================================================

    /**
     * 一点透视的医院走廊剪影：尽头的电梯、两侧病房门、闪烁日光灯、地面上光池、漂浮尘埃。
     */
    public static void corridor(Renderer r, float t) {
        r.clear(0xFF05070A);
        r.fillVerticalGradient(0f, 0f, UiKit.W, UiKit.H, 0xFF0A0D12, 0xFF040608);

        final float vx = 360f, vy = 505f;
        // 走廊尽头（后墙）
        final float bw = 196f, bh = 214f;
        float bx0 = vx - bw * 0.5f, bx1 = vx + bw * 0.5f;
        float by0 = vy - bh * 0.5f, by1 = vy + bh * 0.5f;

        // 天花板 / 地板 / 左右墙
        r.fillPolygon(new float[]{0f, -10f, bx0, by0, bx1, by0, UiKit.W, -10f}, 0xFF10141A);
        r.fillPolygon(new float[]{0f, UiKit.H + 10f, bx0, by1, bx1, by1, UiKit.W, UiKit.H + 10f}, 0xFF2B333D);
        r.fillPolygon(new float[]{-10f, -10f, bx0, by0, bx0, by1, -10f, UiKit.H + 10f}, 0xFF1D242C);
        r.fillPolygon(new float[]{UiKit.W + 10f, -10f, bx1, by0, bx1, by1, UiKit.W + 10f, UiKit.H + 10f}, 0xFF171D24);

        // 地板透视砖缝
        int grout = Palette.alpha(0xFF5A6675, 0.20f);
        for (int i = 0; i <= 8; i++) {
            float bx = -60f + i * (UiKit.W + 120f) / 8f;
            r.line(bx, UiKit.H + 10f, MathUtil.lerp(bx, vx, 0.74f), by1, 2f, grout);
        }
        for (int i = 1; i <= 5; i++) {
            float k = i / 6f;
            float yy = MathUtil.lerp(by1, UiKit.H + 10f, k * k);
            float lx = MathUtil.lerp(bx0, -10f, k * k);
            float rx = MathUtil.lerp(bx1, UiKit.W + 10f, k * k);
            r.line(lx, yy, rx, yy, 2f, grout);
        }

        // 两侧病房门（沿墙面按透视分布）
        door(r, 0.09f, 0.22f, false, false, 1);
        door(r, 0.31f, 0.42f, false, false, 2);
        door(r, 0.53f, 0.61f, false, true, 3);
        door(r, 0.12f, 0.24f, true, false, 4);
        door(r, 0.38f, 0.48f, true, false, 5);
        door(r, 0.60f, 0.67f, true, true, 6);

        // 墙裙 / 黄色警示条
        wallTrim(r, false);
        wallTrim(r, true);

        // 走廊尽头：电梯门 + 绿色 EXIT
        r.fillRect(bx0, by0, bw, bh, 0xFF232B35);
        r.strokeRect(bx0, by0, bw, bh, 3f, Palette.alpha(UiKit.STEEL, 0.25f));
        float dw = bw * 0.56f, dh = bh * 0.62f;
        float dx = vx - dw * 0.5f, dy = by1 - dh - 8f;
        int doorCol = Palette.alpha(0xFF3C4A4A, 0.95f);
        r.fillRect(dx, dy, dw, dh, doorCol);
        r.line(vx, dy, vx, dy + dh, 2.5f, Palette.alpha(Palette.BLACK, 0.6f));
        r.strokeRect(dx, dy, dw, dh, 2f, Palette.alpha(Palette.ELEVATOR, 0.55f));
        // 电梯状态灯
        float pulse = UiKit.blink(t, 2.4f, 0.25f);
        r.fillCircle(vx, dy - 14f, 5f, Palette.alpha(0xFF7FE6C0, pulse));
        r.fillRadialGradient(vx, dy - 14f, 90f, Palette.alpha(0xFF7FE6C0, 0.18f * pulse), 0x00000000);
        // 电梯两侧绿色状态灯（不用文字，避免与前景文字打架）
        r.fillRoundRect(vx - 40f, by0 + 18f, 80f, 4f, 2f, Palette.alpha(0xFF8CF0C6, 0.28f + 0.35f * pulse));

        // 天花板日光灯 + 光池
        for (int i = 0; i < 3; i++) {
            float p = 0.18f + i * 0.26f;
            lightFixture(r, t, p, i);
        }

        // 墙面血手印 / 污渍
        bloodStain(r, 150f, 690f, 46f, 1);
        bloodStain(r, 560f, 760f, 38f, 2);
        bloodStain(r, 262f, 470f, 22f, 3);

        // 尘埃
        dust(r, t, 44, 7, 0.95f);
        UiKit.vignette(r, 0.68f);
    }

    /** 走廊墙面上的病房门。left=false 表示右墙。 */
    private static void door(Renderer r, float t0, float t1, boolean right, boolean locked, int seed) {
        float vx = 360f, vy = 505f;
        float bx = vx + (right ? 98f : -98f);
        float by0 = vy - 107f, by1 = vy + 107f;
        float ox = right ? UiKit.W + 10f : -10f;
        // 墙内侧上/下沿插值
        float ax0 = MathUtil.lerp(ox, bx, t0), ay0 = MathUtil.lerp(-10f, by0, t0);
        float ax1 = MathUtil.lerp(ox, bx, t1), ay1 = MathUtil.lerp(-10f, by0, t1);
        float bx0 = MathUtil.lerp(ox, bx, t0), byy0 = MathUtil.lerp(UiKit.H + 10f, by1, t0);
        float bx1 = MathUtil.lerp(ox, bx, t1), byy1 = MathUtil.lerp(UiKit.H + 10f, by1, t1);

        float top = MathUtil.lerp(ay0, ay1, 0.18f);
        float bot = MathUtil.lerp(byy0, byy1, 0.72f);
        float xa = MathUtil.lerp(ax0, ax1, 0.12f);
        float xb = MathUtil.lerp(ax0, ax1, 0.88f);
        int fill = seed % 2 == 0 ? 0xFF2A231C : 0xFF26201A;
        r.fillPolygon(new float[]{
                xa, top, xb, MathUtil.lerp(ay0, ay1, 0.22f),
                xb, MathUtil.lerp(byy0, byy1, 0.78f), xa, bot
        }, fill);
        // 门缝高光
        r.line(xa, top, xa, bot, 2.2f, Palette.alpha(0xFF8A7448, 0.30f));
        r.line(xb, MathUtil.lerp(ay0, ay1, 0.22f), xb, MathUtil.lerp(byy0, byy1, 0.78f), 2.2f,
                Palette.alpha(0xFF8A7448, 0.20f));
        // 小观察窗
        float wx = MathUtil.lerp(xa, xb, 0.5f);
        float wy = MathUtil.lerp(top, bot, 0.30f);
        r.fillRect(wx - 8f, wy - 10f, 16f, 20f, Palette.alpha(0xFF0E1A22, 0.9f));
        // 门牌 / 锁灯
        if (locked) {
            r.fillCircle(xb - 6f, MathUtil.lerp(top, bot, 0.52f), 4.5f,
                    Palette.alpha(Palette.RED, 0.55f + 0.35f * UiKit.pulse(seed, 3f)));
        } else {
            r.fillCircle(xb - 6f, MathUtil.lerp(top, bot, 0.52f), 3.2f, Palette.alpha(UiKit.STEEL, 0.45f));
        }
    }

    private static void wallTrim(Renderer r, boolean right) {
        float vx = 360f, vy = 505f;
        float bx = vx + (right ? 98f : -98f);
        float by = vy + 107f;
        float ox = right ? UiKit.W + 10f : -10f;
        float yOuter = UiKit.H * 0.62f;
        int col = Palette.alpha(UiKit.HAZARD, 0.16f);
        r.line(ox, yOuter, bx, by - 8f, 5f, col);
        r.line(ox, yOuter + 14f, bx, by, 2f, Palette.alpha(UiKit.HAZARD_DARK, 0.4f));
    }

    private static void lightFixture(Renderer r, float t, float p, int index) {
        float vx = 360f, vy = 505f;
        float by0 = vy - 107f;
        float ox = -10f;
        float ax = MathUtil.lerp(ox, vx - 98f, p);
        float ay = MathUtil.lerp(-10f, by0, p);
        // 闪烁：基础正弦 + 硬切换抖动
        float base = 0.72f + 0.28f * (float) Math.sin(t * 2.1f + index * 2.3f);
        float flick = MathUtil.hash01((int) (t * 11f) + index * 37, 5) < 0.14f ? 0.18f : 1f;
        float lit = MathUtil.clamp(base * flick, 0.05f, 1f);
        float len = MathUtil.lerp(150f, 40f, p);
        float th = MathUtil.lerp(22f, 8f, p);
        int lamp = Palette.alpha(0xFFFFF2CC, 0.55f + 0.45f * lit);
        r.fillRoundRect(ax - len * 0.5f, ay - th * 0.5f, len, th, th * 0.4f,
                Palette.alpha(0xFF2A2E33, 0.9f));
        r.fillRoundRect(ax - len * 0.42f, ay - th * 0.18f, len * 0.84f, th * 0.36f, th * 0.18f, lamp);
        // 光晕
        r.fillRadialGradient(ax, ay + 36f, len * 0.95f,
                Palette.alpha(0xFFFFE9B8, 0.26f * lit), 0x00000000);
        // 地面光池
        float fx = MathUtil.lerp(vx, 360f, 0f);
        float fy = MathUtil.lerp(UiKit.H + 10f, vy + 107f, p * p);
        r.save();
        r.translate(ax * 0.35f + fx * 0.65f, fy);
        r.scale(1f, 0.30f);
        r.fillCircle(0f, 0f, len * 0.9f, Palette.alpha(0xFFFFE9B8, 0.115f * lit));
        r.restore();
    }

    private static void bloodStain(Renderer r, float x, float y, float size, int seed) {
        r.fillCircle(x, y, size * 0.42f, Palette.alpha(Palette.BLOOD, 0.34f));
        r.fillCircle(x - size * 0.30f, y + size * 0.18f, size * 0.22f, Palette.alpha(Palette.BLOOD, 0.26f));
        r.fillCircle(x + size * 0.27f, y + size * 0.26f, size * 0.16f, Palette.alpha(Palette.BLOOD, 0.22f));
        r.fillCircle(x + size * 0.10f, y - size * 0.30f, size * 0.12f, Palette.alpha(Palette.BLOOD, 0.20f));
        for (int i = 0; i < 4; i++) {
            float dx = (MathUtil.hash01(seed * 31 + i, 9) - 0.5f) * size * 1.5f;
            float dy = MathUtil.hash01(seed * 53 + i, 17) * size * 0.7f;
            r.fillCircle(x + dx, y + size * 0.2f + dy, 1.8f + MathUtil.hash01(seed + i, 3) * 2.6f,
                    Palette.alpha(Palette.BLOOD, 0.22f));
        }
    }

    // ==================================================================
    // 通用粒子 / 心电
    // ==================================================================

    /** 漂浮尘埃。位置由时间驱动，无状态。 */
    public static void dust(Renderer r, float t, int count, int seed, float brightness) {
        for (int i = 0; i < count; i++) {
            float h1 = MathUtil.hash01(seed + i * 3, 7);
            float h2 = MathUtil.hash01(seed + i * 5, 11);
            float h3 = MathUtil.hash01(seed + i * 7, 13);
            float speed = 6f + h3 * 18f;
            float x = (h1 * UiKit.W + t * speed) % UiKit.W;
            float y = (h2 * UiKit.H - t * (4f + h1 * 10f)) % UiKit.H;
            if (y < 0f) y += UiKit.H;
            float a = (0.10f + 0.26f * h2) * brightness * (0.6f + 0.4f * (float) Math.sin(t * 1.7f + i));
            r.fillCircle(x, y, 0.8f + h3 * 1.8f, Palette.alpha(0xFFE9F2E4, Math.max(0.02f, a)));
        }
    }

    /** 心电折线（横向滚动）。 */
    public static void ecg(Renderer r, float x, float y, float w, float h, float t, int color) {
        int n = 76;
        float amp = h * 0.42f;
        float scroll = (t * 0.30f) % 1f;
        float prevX = x, prevY = y;
        int glowColor = Palette.alpha(color, 0.16f);
        for (int i = 0; i <= n; i++) {
            float u = i / (float) n;
            float p = (u + scroll) % 1f;
            float v = ecgShape(p);
            float px = x + u * w;
            float py = y - v * amp;
            if (i > 0) {
                r.line(prevX, prevY, px, py, 4.5f, glowColor);
                r.line(prevX, prevY, px, py, 2f, color);
            }
            prevX = px;
            prevY = py;
        }
    }

    private static float ecgShape(float p) {
        return 0.10f * gauss(p, 0.16f, 0.020f)
                - 0.26f * gauss(p, 0.245f, 0.011f)
                + 1.00f * gauss(p, 0.300f, 0.013f)
                - 0.40f * gauss(p, 0.365f, 0.018f)
                + 0.26f * gauss(p, 0.600f, 0.035f);
    }

    private static float gauss(float p, float center, float width) {
        float d = p - center;
        return (float) Math.exp(-(d * d) / (2f * width * width));
    }

    /** 缓慢呼吸的血色暗角（失败/危险界面）。 */
    public static void dangerPulse(Renderer r, float t, float strength) {
        float k = 0.55f + 0.45f * (float) Math.sin(t * 2.2f);
        r.fillRadialGradient(UiKit.W * 0.5f, UiKit.H * 0.5f, 760f, 0x00000000,
                Palette.alpha(Palette.RED_DARK, MathUtil.clamp(strength * k, 0f, 0.7f)));
    }
}
