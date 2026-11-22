package com.crazyclinic.core.render;

import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Renderer;

/**
 * 程序化美术公共工具：椭圆、描边圆、哈希噪声、颜色闪白、光晕、文字描边。
 * 全部为静态方法，不持有每帧状态（只复用一个多边形缓冲，绘制调用内同步消费）。
 */
final class Art {
    private Art() {}

    // ---- 关键描边/警示色（与原版调色板一致） ----
    static final int OUTLINE = 0xFF2A2222;
    static final int OUTLINE_SOFT = 0x88221C1E;
    static final int HAZARD = 0xFFE1C424;
    static final int HAZARD_DARK = 0xFF241C12;
    static final int TRIM = 0xFF6E7B8C;
    static final int WAINSCOT = 0xFF232A33;
    static final int WALL_TOP = 0xFF525D6D;
    static final int WALL_TOP_DARK = 0xFF3E4856;
    static final int WALL_SIDE = 0xFF2F3742;
    static final int GLASS = 0xFF2A3C52;

    static final int SKIN = 0xFFF0CBAE;
    static final int SKIN_DARK = 0xFFD9A883;
    static final int HAIR = 0xFFE1C424;
    static final int HAIR_HI = 0xFFFFE137;
    static final int HAIR_DARK = 0xFFA58C00;
    static final int GOWN = 0xFFDED6C3;
    static final int GOWN_DARK = 0xFFB3AA97;
    static final int SHORTS = 0xFF65D714;
    static final int SHORTS_DARK = 0xFF357507;
    static final int SLIPPER = 0xFF3570D7;
    static final int SLIPPER_DARK = 0xFF0040AF;

    // 复用缓冲：尺寸必须与 fillPolygon 期望的完整多边形点数完全一致，
    // 否则平台渲染器会把旧数据也当成顶点（不允许局部复用）。
    private static final float[] BUF_ELLIPSE = new float[32];      // 16 点
    private static final float[] BUF_ELLIPSE_ROT = new float[32];  // 16 点
    private static final float[] BUF_FAN = new float[24];          // 圆心 + 11 弧点 = 12 点

    // ---------- 基础形状 ----------

    /** 椭圆（16 边多边形，单次 fillPolygon，比 save/scale 更省）。 */
    static void ellipse(Renderer r, float cx, float cy, float rx, float ry, int color) {
        if (rx <= 0f || ry <= 0f) return;
        final int n = 16;
        float[] p = BUF_ELLIPSE;
        for (int i = 0; i < n; i++) {
            float a = i * (MathUtil.TAU / n);
            p[i * 2] = cx + (float) Math.cos(a) * rx;
            p[i * 2 + 1] = cy + (float) Math.sin(a) * ry;
        }
        r.fillPolygon(p, color);
    }

    /** 旋转椭圆（如锤头、涂抹血迹）。 */
    static void ellipseRot(Renderer r, float cx, float cy, float rx, float ry, float rotDeg, int color) {
        if (rx <= 0f || ry <= 0f) return;
        final int n = 16;
        float rad = (float) Math.toRadians(rotDeg);
        float c = (float) Math.cos(rad), sn = (float) Math.sin(rad);
        float[] p = BUF_ELLIPSE_ROT;
        for (int i = 0; i < n; i++) {
            float a = i * (MathUtil.TAU / n);
            float px = (float) Math.cos(a) * rx;
            float py = (float) Math.sin(a) * ry;
            p[i * 2] = cx + px * c - py * sn;
            p[i * 2 + 1] = cy + px * sn + py * c;
        }
        r.fillPolygon(p, color);
    }

    /** 空心椭圆（外圈深色，内圈填色）——比 strokeCircle 更有手绘描边感。 */
    static void discOutlined(Renderer r, float cx, float cy, float radius, int fill, int outline, float outlineW) {
        r.fillCircle(cx, cy, radius + outlineW, outline);
        r.fillCircle(cx, cy, radius, fill);
    }

    /**
     * 扇形（角度用屏幕坐标弧度：0=右，正方向=屏幕下方 / 顺时针）。
     * 用多边形实现，避免不同平台 fillArc 角度方向差异。
     */
    static void fan(Renderer r, float cx, float cy, float radius, float startRad, float sweepRad, int color) {
        final int n = 10;
        float[] p = BUF_FAN;
        p[0] = cx;
        p[1] = cy;
        for (int i = 0; i <= n; i++) {
            float a = startRad + sweepRad * i / (float) n;
            p[2 + i * 2] = cx + (float) Math.cos(a) * radius;
            p[3 + i * 2] = cy + (float) Math.sin(a) * radius;
        }
        r.fillPolygon(p, color);
    }

    /** 地面椭圆投影。 */
    static void shadow(Renderer r, float cx, float cy, float rx, float ry, int alpha) {
        ellipse(r, cx, cy, rx, ry, Palette.withAlpha(0xFF000000, MathUtil.clamp(alpha, 0, 255)));
    }

    /** 2~3 圈同心圆构成的柔光（廉价替代径向渐变）。 */
    static void glow(Renderer r, float cx, float cy, float radius, int color, float strength) {
        ellipse(r, cx, cy, radius, radius, Palette.alpha(color, 0.10f * strength));
        ellipse(r, cx, cy, radius * 0.66f, radius * 0.66f, Palette.alpha(color, 0.14f * strength));
        ellipse(r, cx, cy, radius * 0.34f, radius * 0.34f, Palette.alpha(color, 0.20f * strength));
    }

    /** 竖直方向柔光带（灯光/门缝）。 */
    static void softRect(Renderer r, float x, float y, float w, float h, int color, float a) {
        r.fillRect(x, y, w, h, Palette.alpha(color, a));
    }

    // ---------- 颜色 ----------

    static int tint(int color, int with, float f) {
        return Palette.mix(color, with, f);
    }

    static int lighten(int c, float f) {
        return Palette.lighten(c, f);
    }

    static int darken(int c, float f) {
        return Palette.darken(c, f);
    }

    static int a(int c, float alpha) {
        return Palette.alpha(c, alpha);
    }

    /** 受击白闪。 */
    static int flash(int color, float f) {
        return f <= 0.01f ? color : Palette.mix(color, Palette.WHITE, MathUtil.clamp(f, 0f, 1f) * 0.82f);
    }

    /** 死亡/隐形灰化。 */
    static int desaturate(int color, float f) {
        int grey = Palette.mix(color, 0xFF808080, 0.6f);
        return Palette.mix(color, grey, MathUtil.clamp(f, 0f, 1f));
    }

    // ---------- 噪声 ----------

    static float hash(int x, int y) {
        return MathUtil.hash01(x, y);
    }

    static float hash3(int x, int y, int z) {
        return MathUtil.hash01(x * 31 + z * 17, y * 53 - z * 7);
    }

    static float wave(float t, float freq) {
        return (float) Math.sin(t * freq);
    }

    static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    // ---------- 文字 ----------

    /** 带深色投影的居中文字（世界内浮动文字/提示用，保证在任何地板上可读）。 */
    static void textOutlined(Renderer r, String text, float x, float y, float size, int color) {
        r.setTextSize(size);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(Palette.withAlpha(0xFF000000, 170));
        r.drawText(text, x + 2f, y + 2f);
        r.setTextColor(color);
        r.drawText(text, x, y);
        r.setTextBold(false);
    }

    static void textShadow(Renderer r, String text, float x, float y, float size, int color, int align) {
        r.setTextSize(size);
        r.setTextAlign(align);
        r.setTextColor(Palette.withAlpha(0xFF000000, 150));
        r.drawText(text, x + 1.5f, y + 1.5f);
        r.setTextColor(color);
        r.drawText(text, x, y);
    }
}
