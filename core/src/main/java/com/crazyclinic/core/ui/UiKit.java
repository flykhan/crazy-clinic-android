package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.content.Texts;
import com.crazyclinic.core.world.Organ;

/**
 * 菜单通用绘制工具与图元库。
 *
 * <p>视觉语言：深色医院 + 薄荷绿 / 血红点缀 + 黄色警示条 + 粗糙做旧感。
 * 全部使用 Renderer 图元绘制，不依赖任何位图素材。</p>
 *
 * <p>逻辑坐标 720x1280；安全区 {@link #SAFE_TOP} / {@link #SAFE_BOTTOM}。</p>
 */
public final class UiKit {
    private UiKit() {}

    public static final float W = Game.WIDTH;
    public static final float H = Game.HEIGHT;
    /** 全面屏顶部安全区（状态栏 / 水滴屏）。 */
    public static final float SAFE_TOP = 64f;
    /** 全面屏底部安全区（手势条）。 */
    public static final float SAFE_BOTTOM = 48f;
    public static final float MARGIN = 40f;

    // ---- 扩展语义色 ----
    public static final int INK = 0xFF0A0C10;
    public static final int PANEL_INK = 0xFF12161D;
    public static final int PANEL_LIGHT = 0xFF1B222C;
    public static final int HAZARD = 0xFFE7C63C;
    public static final int HAZARD_DARK = 0xFF16191F;
    public static final int MINT_GLOW = 0xFFA8EBCB;
    public static final int MINT_DARK = 0xFF2E6B55;
    public static final int BLOOD_LIGHT = 0xFFD2423A;
    public static final int RED_FLASH = 0xFFFF3B30;
    public static final int RUST = 0xFF9A6A3E;
    public static final int STEEL = 0xFF8E9BAA;
    public static final int PAPER = 0xFFE9ECEF;
    public static final int GOLD_LIGHT = 0xFFFFD46A;

    // ==================================================================
    // 基础工具
    // ==================================================================

    public static int a(int color, float alpha) {
        return Palette.alpha(color, alpha);
    }

    public static int mix(int c1, int c2, float t) {
        return Palette.mix(c1, c2, t);
    }

    /** 场景进入时把 game.fade 拉黑，之后淡入。场景在 update 里每帧调用。 */
    public static void tickFade(Game game, float dt) {
        if (game.fade > 0f) {
            game.fade -= dt * 4.2f;
            if (game.fade < 0f) game.fade = 0f;
        }
    }

    public static float pulse(float time, float speed) {
        return 0.5f + 0.5f * (float) Math.sin(time * speed);
    }

    /** 每层病区的主题强调色（用于简报/结算/水印）。 */
    public static int floorAccent(int floor) {
        switch (floor) {
            case 1: return Palette.MINT;
            case 2: return Palette.CYAN;
            case 3: return Palette.ALERT;
            case 4: return Palette.GREEN;
            case 5: return Palette.PURPLE;
            case 6: return Palette.BLUE;
            case 7: return Palette.RED;
            case 8: return Palette.PURPLE;
            case 9: return Palette.GOLD;
            case 10: return UiKit.GOLD_LIGHT;
            default: return Palette.MINT;
        }
    }

    public static float blink(float time, float period, float min) {
        float p = (time % period) / period;
        float v = p < 0.5f ? p * 2f : (1f - p) * 2f;
        return min + (1f - min) * v;
    }

    // ==================================================================
    // 文字
    // ==================================================================

    public static void text(Renderer r, String s, float x, float y, float size, int color, int align) {
        r.setTextSize(size);
        r.setTextBold(false);
        r.setTextAlign(align);
        r.setTextColor(color);
        r.drawText(s, x, y);
    }

    public static void textBold(Renderer r, String s, float x, float y, float size, int color, int align) {
        r.setTextSize(size);
        r.setTextBold(true);
        r.setTextAlign(align);
        r.setTextColor(color);
        r.drawText(s, x, y);
    }

    /** 带描边阴影的标题文字（粗糙印刷感）。 */
    public static void textShadow(Renderer r, String s, float x, float y, float size, int color, int align,
                                  int shadowColor, float ox, float oy) {
        r.setTextSize(size);
        r.setTextBold(true);
        r.setTextAlign(align);
        r.setTextColor(shadowColor);
        r.drawText(s, x + ox, y + oy);
        r.setTextColor(color);
        r.drawText(s, x, y);
    }

    /** 自动缩小字号以适配宽度，返回实际字号。 */
    public static float textFit(Renderer r, String s, float x, float y, float maxWidth, float size,
                                float minSize, int color, int align, boolean bold) {
        r.setTextSize(size);
        r.setTextBold(bold);
        float w = r.measureText(s);
        if (w > maxWidth && w > 0f) {
            size = Math.max(minSize, size * maxWidth / w);
            r.setTextSize(size);
        }
        r.setTextAlign(align);
        r.setTextColor(color);
        r.drawText(s, x, y);
        return size;
    }

    /** 自动换行绘制（按字符，适合中文）。返回绘制后的下一行 y。 */
    public static float wrapped(Renderer r, String text, float x, float y, float maxWidth,
                                float size, int color, float lineGap) {
        r.setTextSize(size);
        r.setTextAlign(0);
        r.setTextColor(color);
        StringBuilder sb = new StringBuilder(48);
        String[] lines = text.split("\n", -1);
        for (String line : lines) {
            sb.setLength(0);
            if (line.isEmpty()) {
                y += size + lineGap;
                continue;
            }
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                sb.append(c);
                if (sb.length() > 1 && r.measureText(sb.toString()) > maxWidth) {
                    sb.setLength(sb.length() - 1);
                    r.drawText(sb.toString(), x, y);
                    y += size + lineGap;
                    sb.setLength(0);
                    sb.append(c);
                }
            }
            if (sb.length() > 0) {
                r.drawText(sb.toString(), x, y);
                y += size + lineGap;
            }
        }
        return y;
    }

    public static String formatTime(float seconds) {
        int s = Math.max(0, (int) seconds);
        int h = s / 3600;
        int m = (s % 3600) / 60;
        int ss = s % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, ss);
        return String.format("%d:%02d", m, ss);
    }

    public static String formatTimeShort(int seconds) {
        int m = seconds / 60;
        if (m >= 60) return (m / 60) + "小时" + (m % 60) + "分";
        if (m > 0) return m + " 分钟";
        return seconds + " 秒";
    }

    // ==================================================================
    // 按钮
    // ==================================================================

    public static final int STYLE_SECONDARY = 0;
    public static final int STYLE_PRIMARY = 1;
    public static final int STYLE_DANGER = 2;
    public static final int STYLE_GHOST = 3;

    /** 兼容旧签名：无动画按钮。 */
    public static void button(Renderer r, Rect b, String label, int style) {
        drawButton(r, b, label, style, true, 0f, 1f, 0f);
    }

    public static void drawButton(Renderer r, Rect b, String label, int style,
                                  boolean enabled, float press, float appear, float time) {
        if (appear <= 0.01f) return;
        float s = 1f - 0.05f * press;
        float dy = (1f - appear) * 18f;
        float alpha = MathUtil.clamp(appear, 0f, 1f);
        r.save();
        r.setAlpha(r.alpha() * alpha);
        r.translate(b.cx(), b.cy() + dy);
        r.scale(s, s);
        float x = -b.w * 0.5f, y = -b.h * 0.5f, w = b.w, h = b.h;
        float rad = Math.min(20f, h * 0.28f);

        int fill;
        int stroke;
        int textColor;
        int accent;
        switch (style) {
            case STYLE_PRIMARY:
                fill = enabled ? Palette.MINT : mix(Palette.MINT, Palette.PANEL, 0.66f);
                stroke = enabled ? mix(Palette.MINT, Palette.WHITE, 0.35f) : Palette.PANEL_STROKE;
                textColor = enabled ? Palette.BG_DARK : mix(Palette.TEXT_DARK, Palette.BG_DARK, 0.35f);
                accent = MINT_DARK;
                break;
            case STYLE_DANGER:
                fill = enabled ? Palette.RED_DARK : mix(Palette.RED_DARK, Palette.PANEL, 0.62f);
                stroke = enabled ? Palette.RED : Palette.PANEL_STROKE;
                textColor = enabled ? 0xFFFFE1DC : Palette.TEXT_DARK;
                accent = Palette.RED;
                break;
            case STYLE_GHOST:
                fill = Palette.alpha(Palette.PANEL, 0.30f);
                stroke = Palette.alpha(Palette.MINT, enabled ? 0.75f : 0.25f);
                textColor = enabled ? Palette.MINT : Palette.TEXT_DARK;
                accent = Palette.MINT;
                break;
            default:
                fill = enabled ? Palette.PANEL_ALT : mix(Palette.PANEL_ALT, Palette.BG_DARK, 0.5f);
                stroke = enabled ? Palette.PANEL_STROKE : mix(Palette.PANEL_STROKE, Palette.BG_DARK, 0.5f);
                textColor = enabled ? Palette.TEXT : Palette.TEXT_DARK;
                accent = enabled ? Palette.MINT : Palette.TEXT_DARK;
                break;
        }

        // 投影 + 底板
        r.fillRoundRect(x + 3f, y + 7f, w, h, rad, Palette.alpha(Palette.BLACK, 0.55f));
        r.fillRoundRect(x, y, w, h, rad, fill);
        // 顶部高光
        r.fillRoundRect(x + 2f, y + 2f, w - 4f, h * 0.44f, rad, Palette.alpha(Palette.WHITE,
                style == STYLE_PRIMARY ? 0.20f : 0.055f));
        // 按压变暗
        if (press > 0.01f) {
            r.fillRoundRect(x, y, w, h, rad, Palette.alpha(Palette.BLACK, 0.14f * press));
        }
        // 左侧色条
        r.fillRoundRect(x + 9f, y + h * 0.28f, 5f, h * 0.44f, 2.5f,
                Palette.alpha(accent, enabled ? 0.95f : 0.35f));
        // 描边
        r.strokeRoundRect(x, y, w, h, rad, 2.5f, stroke);
        r.strokeRoundRect(x + 3f, y + 3f, w - 6f, h - 6f, Math.max(4f, rad - 1.5f), 1f,
                Palette.alpha(Palette.WHITE, style == STYLE_PRIMARY ? 0.16f : 0.05f));

        // 文字
        float size = Math.min(42f, h * 0.36f);
        r.setTextSize(size);
        r.setTextBold(true);
        r.setTextAlign(1);
        float tw = r.measureText(label);
        float maxW = w - 70f;
        if (tw > maxW && tw > 0f) {
            size = Math.max(22f, size * maxW / tw);
            r.setTextSize(size);
        }
        float ty = -1f + press * 1.5f;
        r.setTextColor(Palette.alpha(Palette.BLACK, style == STYLE_PRIMARY ? 0.28f : 0.55f));
        r.drawText(label, 0f, ty + 3f);
        r.setTextColor(textColor);
        r.drawText(label, 0f, ty);
        r.restore();
    }

    // ==================================================================
    // 面板 / 容器
    // ==================================================================

    /** 兼容旧签名。 */
    public static void panel(Renderer r, Rect b) {
        panel(r, b, 0, 0f);
    }

    public static void panel(Renderer r, Rect b, int accent, float time) {
        r.fillRoundRect(b.x + 4f, b.y + 8f, b.w, b.h, 26f, Palette.alpha(Palette.BLACK, 0.45f));
        r.fillRoundRect(b.x, b.y, b.w, b.h, 26f, Palette.alpha(PANEL_INK, 0.97f));
        r.fillRoundRect(b.x + 2f, b.y + 2f, b.w - 4f, b.h * 0.42f, 24f, Palette.alpha(Palette.WHITE, 0.035f));
        r.strokeRoundRect(b.x, b.y, b.w, b.h, 26f, 2.5f, Palette.alpha(Palette.PANEL_STROKE, 0.95f));
        cornerBrackets(r, b, 20f, accent != 0 ? Palette.alpha(accent, 0.85f) : Palette.alpha(STEEL, 0.6f));
        grunge(r, b.x, b.y, b.w, b.h, 18, 11);
        if (accent != 0) {
            r.fillRoundRect(b.x, b.y + 22f, 6f, b.h - 44f, 3f, Palette.alpha(accent, 0.9f));
            r.fillRoundRect(b.x, b.y + 22f, 6f, 46f, 3f, Palette.alpha(Palette.WHITE, 0.25f));
        }
    }

    /** 面板四角的工业风括角。 */
    public static void cornerBrackets(Renderer r, Rect b, float len, int color) {
        float x0 = b.x, y0 = b.y, x1 = b.x + b.w, y1 = b.y + b.h;
        float l = len;
        r.line(x0 + 2f, y0 + l, x0 + 2f, y0 + 2f, 2.5f, color);
        r.line(x0 + 2f, y0 + 2f, x0 + l, y0 + 2f, 2.5f, color);
        r.line(x1 - 2f, y0 + l, x1 - 2f, y0 + 2f, 2.5f, color);
        r.line(x1 - 2f, y0 + 2f, x1 - l, y0 + 2f, 2.5f, color);
        r.line(x0 + 2f, y1 - l, x0 + 2f, y1 - 2f, 2.5f, color);
        r.line(x0 + 2f, y1 - 2f, x0 + l, y1 - 2f, 2.5f, color);
        r.line(x1 - 2f, y1 - l, x1 - 2f, y1 - 2f, 2.5f, color);
        r.line(x1 - 2f, y1 - 2f, x1 - l, y1 - 2f, 2.5f, color);
    }

    /** 确定性做旧污点（不随帧变化，避免闪烁）。 */
    public static void grunge(Renderer r, float x, float y, float w, float h, int count, int seed) {
        for (int i = 0; i < count; i++) {
            float hx = MathUtil.hash01(seed + i * 7, 13);
            float hy = MathUtil.hash01(seed + i * 13, 29);
            float hr = MathUtil.hash01(seed + i * 3, 41);
            float px = x + 6f + hx * (w - 12f);
            float py = y + 6f + hy * (h - 12f);
            r.fillCircle(px, py, 1.2f + hr * 3.4f, Palette.alpha(Palette.BLACK, 0.10f + hr * 0.14f));
        }
    }

    public static void scrim(Renderer r, float alpha) {
        r.fillRect(0f, 0f, W, H, Palette.alpha(Palette.BLACK, alpha));
    }

    public static void vignette(Renderer r, float strength) {
        r.fillRadialGradient(W * 0.5f, H * 0.44f, 820f, 0x00000000,
                Palette.alpha(Palette.BLACK, MathUtil.clamp(strength, 0f, 0.92f)));
    }

    // ==================================================================
    // 警示条 / 进度条 / 开关
    // ==================================================================

    /** 斜纹警示条（相位可滚动）。 */
    public static void hazard(Renderer r, float x, float y, float w, float h, float phase,
                              int colA, int colB, float alpha) {
        if (w <= 0f || h <= 0f) return;
        r.save();
        r.setAlpha(r.alpha() * alpha);
        r.clipRect(x, y, w, h);
        r.fillRect(x, y, w, h, colA);
        r.translate(x + w * 0.5f, y + h * 0.5f);
        r.rotate(-32f);
        float span = w + h + 40f;
        float step = 46f;
        float off = (phase % step + step) % step;
        for (float sx = -span; sx < span; sx += step) {
            r.fillRect(sx + off, -span, step * 0.5f, span * 2f, colB);
        }
        r.restore();
    }

    /** 中段留白、两端斜纹的“封条 / 胶带”，文字居中。 */
    public static void tape(Renderer r, float cx, float cy, float w, float h, String label,
                            float phase, float tilt) {
        r.save();
        r.translate(cx, cy);
        r.rotate(tilt);
        float x = -w * 0.5f, y = -h * 0.5f;
        r.fillRoundRect(x + 2f, y + 4f, w, h, 3f, Palette.alpha(Palette.BLACK, 0.45f));
        r.fillRect(x, y, w, h, Palette.alpha(HAZARD, 0.95f));
        float cap = h * 1.7f;
        r.save();
        r.clipRect(x, y, cap, h);
        hazard(r, x, y, cap, h, phase, HAZARD, HAZARD_DARK, 1f);
        r.restore();
        r.save();
        r.clipRect(x + w - cap, y, cap, h);
        hazard(r, x + w - cap, y, cap, h, phase, HAZARD, HAZARD_DARK, 1f);
        r.restore();
        r.strokeRect(x, y, w, h, 1.5f, Palette.alpha(HAZARD_DARK, 0.6f));
        float size = h * 0.56f;
        r.setTextSize(size);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(HAZARD_DARK);
        r.drawText(label, 0f, 0f);
        r.restore();
    }

    public static void progressBar(Renderer r, float x, float y, float w, float h, float value,
                                   int fill, int bg, int border) {
        float v = MathUtil.clamp(value, 0f, 1f);
        r.fillRoundRect(x, y, w, h, h * 0.5f, bg);
        if (v > 0.001f) {
            r.fillRoundRect(x + 2f, y + 2f, Math.max(h - 4f, (w - 4f) * v), h - 4f,
                    (h - 4f) * 0.5f, fill);
        }
        r.strokeRoundRect(x, y, w, h, h * 0.5f, 2f, border);
        int seg = 10;
        for (int i = 1; i < seg; i++) {
            float sx = x + w * i / seg;
            r.fillRect(sx - 1f, y + 3f, 2f, h - 6f, Palette.alpha(Palette.BLACK, 0.35f));
        }
    }

    /** 大号滑动开关。on: 0..1 已缓动的值。 */
    public static void toggle(Renderer r, float x, float y, float w, float h, float on,
                              boolean pressed, float time) {
        float k = MathUtil.easeInOutQuad(MathUtil.clamp(on, 0f, 1f));
        int track = mix(0xFF28303C, Palette.MINT, k * 0.62f);
        r.fillRoundRect(x + 2f, y + 4f, w, h, h * 0.5f, Palette.alpha(Palette.BLACK, 0.45f));
        r.fillRoundRect(x, y, w, h, h * 0.5f, track);
        r.strokeRoundRect(x, y, w, h, h * 0.5f, 2.5f,
                Palette.alpha(k > 0.5f ? Palette.MINT : Palette.PANEL_STROKE, 0.9f));
        float kr = h * 0.40f;
        float kx = x + kr + 6f + k * (w - 2f * kr - 12f);
        float ky = y + h * 0.5f;
        r.fillCircle(kx + 1f, ky + 3f, kr, Palette.alpha(Palette.BLACK, 0.35f));
        r.fillCircle(kx, ky - (pressed ? 1.5f : 0f), kr, k > 0.5f ? Palette.WHITE : Palette.TEXT_DIM);
        r.fillCircle(kx - kr * 0.25f, ky - kr * 0.3f, kr * 0.42f, Palette.alpha(Palette.WHITE, 0.35f));
        r.strokeCircle(kx, ky, kr, 2f, Palette.alpha(Palette.BLACK, 0.35f));
    }

    // ==================================================================
    // 图标（全部图元绘制）
    // ==================================================================

    public static void iconBack(Renderer r, float cx, float cy, float s, int color) {
        r.line(cx + s * 0.18f, cy - s * 0.36f, cx - s * 0.22f, cy, s * 0.16f, color);
        r.line(cx - s * 0.22f, cy, cx + s * 0.18f, cy + s * 0.36f, s * 0.16f, color);
    }

    public static void iconArrow(Renderer r, float cx, float cy, float len, int color) {
        float h = len * 0.42f;
        r.line(cx - len * 0.5f, cy, cx + len * 0.5f - h * 0.9f, cy, h * 0.34f, color);
        r.fillTriangle(cx + len * 0.5f, cy,
                cx + len * 0.5f - h, cy - h * 0.62f,
                cx + len * 0.5f - h, cy + h * 0.62f, color);
    }

    public static void iconChevron(Renderer r, float cx, float cy, float s, int color, boolean right) {
        float d = right ? 1f : -1f;
        r.line(cx - s * 0.18f * d, cy - s * 0.32f, cx + s * 0.16f * d, cy, s * 0.14f, color);
        r.line(cx + s * 0.16f * d, cy, cx - s * 0.18f * d, cy + s * 0.32f, s * 0.14f, color);
    }

    public static void iconLock(Renderer r, float cx, float cy, float s, int color) {
        r.strokeCircle(cx, cy - s * 0.13f, s * 0.23f, s * 0.10f, color);
        r.fillRoundRect(cx - s * 0.34f, cy - s * 0.03f, s * 0.68f, s * 0.52f, s * 0.10f, color);
        r.fillCircle(cx, cy + s * 0.17f, s * 0.07f, Palette.alpha(INK, 0.85f));
        r.fillRect(cx - s * 0.025f, cy + s * 0.17f, s * 0.05f, s * 0.14f, Palette.alpha(INK, 0.85f));
    }

    public static void iconKey(Renderer r, float cx, float cy, float s, int color) {
        r.strokeCircle(cx - s * 0.24f, cy, s * 0.15f, s * 0.09f, color);
        r.line(cx - s * 0.09f, cy, cx + s * 0.40f, cy, s * 0.09f, color);
        r.line(cx + s * 0.24f, cy, cx + s * 0.24f, cy + s * 0.15f, s * 0.08f, color);
        r.line(cx + s * 0.36f, cy, cx + s * 0.36f, cy + s * 0.11f, s * 0.08f, color);
    }

    public static void iconHeart(Renderer r, float cx, float cy, float s, int color) {
        r.fillCircle(cx - s * 0.16f, cy - s * 0.12f, s * 0.19f, color);
        r.fillCircle(cx + s * 0.16f, cy - s * 0.12f, s * 0.19f, color);
        r.fillTriangle(cx - s * 0.34f, cy - s * 0.05f,
                cx + s * 0.34f, cy - s * 0.05f,
                cx, cy + s * 0.36f, color);
    }

    public static void iconCross(Renderer r, float cx, float cy, float s, int color) {
        r.fillRoundRect(cx - s * 0.13f, cy - s * 0.42f, s * 0.26f, s * 0.84f, s * 0.05f, color);
        r.fillRoundRect(cx - s * 0.42f, cy - s * 0.13f, s * 0.84f, s * 0.26f, s * 0.05f, color);
    }

    public static void iconSkull(Renderer r, float cx, float cy, float s, int color) {
        r.fillCircle(cx, cy - s * 0.08f, s * 0.34f, color);
        r.fillRoundRect(cx - s * 0.22f, cy + s * 0.12f, s * 0.44f, s * 0.28f, s * 0.08f, color);
        r.fillCircle(cx - s * 0.14f, cy - s * 0.12f, s * 0.09f, Palette.alpha(INK, 0.9f));
        r.fillCircle(cx + s * 0.14f, cy - s * 0.12f, s * 0.09f, Palette.alpha(INK, 0.9f));
        r.fillTriangle(cx, cy + s * 0.02f, cx - s * 0.06f, cy + s * 0.13f, cx + s * 0.06f, cy + s * 0.13f,
                Palette.alpha(INK, 0.9f));
    }

    public static void iconCrosshair(Renderer r, float cx, float cy, float s, int color) {
        r.strokeCircle(cx, cy, s * 0.30f, s * 0.09f, color);
        r.fillCircle(cx, cy, s * 0.06f, color);
        r.line(cx, cy - s * 0.46f, cx, cy - s * 0.26f, s * 0.08f, color);
        r.line(cx, cy + s * 0.26f, cx, cy + s * 0.46f, s * 0.08f, color);
        r.line(cx - s * 0.46f, cy, cx - s * 0.26f, cy, s * 0.08f, color);
        r.line(cx + s * 0.26f, cy, cx + s * 0.46f, cy, s * 0.08f, color);
    }

    public static void iconClock(Renderer r, float cx, float cy, float s, int color) {
        r.strokeCircle(cx, cy, s * 0.38f, s * 0.09f, color);
        r.line(cx, cy - s * 0.22f, cx, cy, s * 0.08f, color);
        r.line(cx, cy, cx + s * 0.19f, cy + s * 0.10f, s * 0.08f, color);
    }

    public static void iconWarning(Renderer r, float cx, float cy, float s, int color) {
        r.fillTriangle(cx, cy - s * 0.46f, cx + s * 0.48f, cy + s * 0.38f, cx - s * 0.48f, cy + s * 0.38f, color);
        r.fillRoundRect(cx - s * 0.045f, cy - s * 0.20f, s * 0.09f, s * 0.30f, s * 0.04f,
                Palette.alpha(INK, 0.9f));
        r.fillCircle(cx, cy + s * 0.22f, s * 0.055f, Palette.alpha(INK, 0.9f));
    }

    public static void iconComputer(Renderer r, float cx, float cy, float s, int color) {
        r.strokeRoundRect(cx - s * 0.40f, cy - s * 0.40f, s * 0.80f, s * 0.58f, s * 0.06f, s * 0.08f, color);
        r.fillRoundRect(cx - s * 0.32f, cy - s * 0.32f, s * 0.64f, s * 0.42f, s * 0.03f,
                Palette.alpha(color, 0.35f));
        r.fillRect(cx - s * 0.05f, cy + s * 0.19f, s * 0.10f, s * 0.14f, color);
        r.fillRoundRect(cx - s * 0.28f, cy + s * 0.32f, s * 0.56f, s * 0.09f, s * 0.03f, color);
    }

    public static void iconElevator(Renderer r, float cx, float cy, float s, int color) {
        r.strokeRoundRect(cx - s * 0.36f, cy - s * 0.44f, s * 0.72f, s * 0.88f, s * 0.06f, s * 0.08f, color);
        r.line(cx, cy - s * 0.40f, cx, cy + s * 0.40f, s * 0.07f, color);
        r.line(cx - s * 0.16f, cy + s * 0.06f, cx - s * 0.16f, cy - s * 0.06f, s * 0.06f, Palette.alpha(color, 0.7f));
        r.line(cx + s * 0.16f, cy - s * 0.06f, cx + s * 0.16f, cy + s * 0.06f, s * 0.06f, Palette.alpha(color, 0.7f));
        r.fillTriangle(cx, cy - s * 0.55f, cx - s * 0.10f, cy - s * 0.40f, cx + s * 0.10f, cy - s * 0.40f, color);
    }

    public static void iconSpeaker(Renderer r, float cx, float cy, float s, int color) {
        r.fillPolygon(new float[]{
                cx - s * 0.38f, cy - s * 0.15f,
                cx - s * 0.16f, cy - s * 0.15f,
                cx + s * 0.08f, cy - s * 0.40f,
                cx + s * 0.08f, cy + s * 0.40f,
                cx - s * 0.16f, cy + s * 0.15f,
                cx - s * 0.38f, cy + s * 0.15f
        }, color);
        r.strokeCircle(cx + s * 0.10f, cy, s * 0.28f, s * 0.07f, color);
        r.strokeCircle(cx + s * 0.10f, cy, s * 0.44f, s * 0.07f, Palette.alpha(color, 0.55f));
    }

    public static void iconMusic(Renderer r, float cx, float cy, float s, int color) {
        r.fillCircle(cx - s * 0.16f, cy + s * 0.26f, s * 0.14f, color);
        r.fillCircle(cx + s * 0.26f, cy + s * 0.14f, s * 0.14f, color);
        r.line(cx - s * 0.03f, cy + s * 0.26f, cx - s * 0.03f, cy - s * 0.34f, s * 0.09f, color);
        r.line(cx + s * 0.39f, cy + s * 0.14f, cx + s * 0.39f, cy - s * 0.44f, s * 0.09f, color);
        r.line(cx - s * 0.03f, cy - s * 0.34f, cx + s * 0.39f, cy - s * 0.44f, s * 0.12f, color);
    }

    public static void iconVibrate(Renderer r, float cx, float cy, float s, int color) {
        r.strokeRoundRect(cx - s * 0.18f, cy - s * 0.40f, s * 0.36f, s * 0.80f, s * 0.09f, s * 0.08f, color);
        r.fillCircle(cx, cy + s * 0.26f, s * 0.05f, color);
        r.line(cx - s * 0.34f, cy - s * 0.22f, cx - s * 0.42f, cy - s * 0.10f, s * 0.07f, Palette.alpha(color, 0.8f));
        r.line(cx - s * 0.34f, cy + s * 0.22f, cx - s * 0.42f, cy + s * 0.10f, s * 0.07f, Palette.alpha(color, 0.8f));
        r.line(cx + s * 0.34f, cy - s * 0.22f, cx + s * 0.42f, cy - s * 0.10f, s * 0.07f, Palette.alpha(color, 0.8f));
        r.line(cx + s * 0.34f, cy + s * 0.22f, cx + s * 0.42f, cy + s * 0.10f, s * 0.07f, Palette.alpha(color, 0.8f));
    }

    public static void iconStar(Renderer r, float cx, float cy, float radius, int color) {
        float[] pts = new float[20];
        for (int i = 0; i < 10; i++) {
            double ang = -Math.PI / 2.0 + i * Math.PI / 5.0;
            float rr = (i % 2 == 0) ? radius : radius * 0.44f;
            pts[i * 2] = cx + (float) Math.cos(ang) * rr;
            pts[i * 2 + 1] = cy + (float) Math.sin(ang) * rr;
        }
        r.fillPolygon(pts, color);
    }

    /** 帮助 / 说明图标。 */
    public static void iconHelp(Renderer r, float cx, float cy, float s, int color) {
        r.strokeCircle(cx, cy, s * 0.40f, s * 0.09f, color);
        int seg = 5;
        float radius = s * 0.15f;
        float ccx = cx;
        float ccy = cy - s * 0.10f;
        float px = 0f, py = 0f;
        for (int i = 0; i <= seg; i++) {
            float a = (float) Math.toRadians(150f + 250f * i / (float) seg);
            float x1 = ccx + (float) Math.cos(a) * radius;
            float y1 = ccy + (float) Math.sin(a) * radius;
            if (i > 0) r.line(px, py, x1, y1, s * 0.09f, color);
            px = x1;
            py = y1;
        }
        r.line(cx, cy + s * 0.02f, cx, cy + s * 0.15f, s * 0.09f, color);
        r.fillCircle(cx, cy + s * 0.27f, s * 0.055f, color);
    }

    /** 重置/恢复默认图标（环形箭头）。 */
    public static void iconReset(Renderer r, float cx, float cy, float s, int color) {
        int seg = 9;
        float radius = s * 0.32f;
        float start = (float) Math.toRadians(-60);
        float sweep = (float) Math.toRadians(285);
        float px = 0f, py = 0f;
        for (int i = 0; i <= seg; i++) {
            float a = start + sweep * i / (float) seg;
            float x1 = cx + (float) Math.cos(a) * radius;
            float y1 = cy + (float) Math.sin(a) * radius;
            if (i > 0) r.line(px, py, x1, y1, s * 0.10f, color);
            px = x1;
            py = y1;
        }
        // 箭头
        float ea = start;
        float ex = cx + (float) Math.cos(ea) * radius;
        float ey = cy + (float) Math.sin(ea) * radius;
        r.fillTriangle(ex + s * 0.04f, ey - s * 0.20f,
                ex + s * 0.24f, ey + s * 0.02f,
                ex - s * 0.06f, ey + s * 0.08f, color);
        r.fillCircle(cx, cy, s * 0.055f, Palette.alpha(color, 0.8f));
    }

    public static void iconPhone(Renderer r, float x, float y, float w, float h, int color) {
        r.fillRoundRect(x + 3f, y + 6f, w, h, 26f, Palette.alpha(Palette.BLACK, 0.5f));
        r.fillRoundRect(x, y, w, h, 26f, Palette.alpha(INK, 0.85f));
        r.strokeRoundRect(x, y, w, h, 26f, 3f, color);
        r.fillRoundRect(x + 10f, y + 26f, w - 20f, h - 64f, 12f, Palette.alpha(Palette.FLOOR_A, 0.30f));
        r.strokeRoundRect(x + 10f, y + 26f, w - 20f, h - 64f, 12f, 1.5f, Palette.alpha(color, 0.4f));
        r.fillRoundRect(x + w * 0.5f - 22f, y + 9f, 44f, 7f, 3.5f, Palette.alpha(color, 0.75f));
        r.fillRoundRect(x + w * 0.5f - 34f, y + h - 24f, 68f, 7f, 3.5f, Palette.alpha(color, 0.55f));
    }

    /** 十字徽章：医院 logo。 */
    public static void emblem(Renderer r, float cx, float cy, float radius, float time) {
        float glow = 0.55f + 0.45f * pulse(time, 2.1f);
        r.fillCircle(cx, cy, radius * 1.55f, Palette.alpha(Palette.RED, 0.06f * glow));
        r.fillCircle(cx, cy + radius * 0.10f, radius * 1.02f, Palette.alpha(Palette.BLACK, 0.45f));
        r.fillCircle(cx, cy, radius, Palette.alpha(0xFF1A1013, 0.96f));
        r.strokeCircle(cx, cy, radius, 3.5f, Palette.alpha(Palette.RED, 0.85f));
        r.strokeCircle(cx, cy, radius * 0.84f, 1.5f, Palette.alpha(Palette.MINT, 0.22f));
        iconCross(r, cx, cy, radius * 0.95f, Palette.alpha(Palette.BLOOD, 0.92f));
        iconCross(r, cx - 1.5f, cy - 1.5f, radius * 0.92f, Palette.alpha(Palette.RED, 0.85f));
        r.fillCircle(cx + radius * 0.30f, cy + radius * 0.44f, radius * 0.075f, Palette.alpha(Palette.BLOOD, 0.9f));
    }

    // ==================================================================
    // 器官图标
    // ==================================================================

    public static int organColor(Organ organ) {
        switch (organ) {
            case BRAIN: return 0xFFE0A6F0;
            case HEART: return 0xFFF26D7D;
            case KIDNEY: return 0xFFC4CE6B;
            case LEGS: return 0xFFF2B366;
            case LIVER: return 0xFFC97A5A;
            case LUNGS: return 0xFF8FC6E8;
            case ARMS: return 0xFFF2D66E;
            default: return Palette.TEXT_DIM;
        }
    }

    /** 器官圆盘（亮/暗两种状态）。 */
    public static void organPlate(Renderer r, float cx, float cy, float radius, boolean lit) {
        if (lit) {
            r.fillCircle(cx, cy, radius + 4f, Palette.alpha(Palette.MINT, 0.10f));
            r.fillCircle(cx, cy, radius, 0xFF232B36);
            r.strokeCircle(cx, cy, radius, 2.5f, Palette.alpha(Palette.MINT, 0.75f));
        } else {
            r.fillCircle(cx, cy, radius, 0xFF171C24);
            r.strokeCircle(cx, cy, radius, 2f, Palette.alpha(Palette.PANEL_STROKE, 0.8f));
        }
    }

    public static void organ(Renderer r, Organ organ, float cx, float cy, float s, boolean lit) {
        int color = lit ? organColor(organ) : Palette.alpha(Palette.TEXT_DARK, 0.55f);
        if (lit) {
            r.fillCircle(cx, cy, s * 0.52f, Palette.alpha(organColor(organ), 0.14f));
        }
        switch (organ) {
            case BRAIN: {
                r.fillCircle(cx, cy, s * 0.42f, color);
                int dark = Palette.alpha(Palette.BLACK, 0.30f);
                r.line(cx, cy - s * 0.38f, cx, cy + s * 0.34f, s * 0.055f, dark);
                r.line(cx - s * 0.30f, cy - s * 0.12f, cx - s * 0.02f, cy - s * 0.20f, s * 0.05f, dark);
                r.line(cx - s * 0.32f, cy + s * 0.12f, cx - s * 0.02f, cy + s * 0.06f, s * 0.05f, dark);
                r.line(cx + s * 0.30f, cy - s * 0.10f, cx + s * 0.02f, cy - s * 0.02f, s * 0.05f, dark);
                r.line(cx + s * 0.28f, cy + s * 0.16f, cx + s * 0.02f, cy + s * 0.22f, s * 0.05f, dark);
                break;
            }
            case HEART:
                iconHeart(r, cx, cy, s * 0.92f, color);
                break;
            case KIDNEY: {
                r.save();
                r.translate(cx - s * 0.10f, cy);
                r.rotate(-18f);
                r.fillRoundRect(-s * 0.16f, -s * 0.38f, s * 0.32f, s * 0.76f, s * 0.16f, color);
                r.restore();
                r.save();
                r.translate(cx + s * 0.22f, cy + s * 0.04f);
                r.rotate(-18f);
                r.fillRoundRect(-s * 0.14f, -s * 0.32f, s * 0.28f, s * 0.64f, s * 0.14f,
                        Palette.alpha(color, 0.85f));
                r.restore();
                r.line(cx - s * 0.02f, cy + s * 0.10f, cx - s * 0.02f, cy - s * 0.22f, s * 0.05f,
                        Palette.alpha(Palette.BLACK, 0.35f));
                break;
            }
            case LEGS: {
                r.fillRoundRect(cx - s * 0.27f, cy - s * 0.46f, s * 0.21f, s * 0.72f, s * 0.10f, color);
                r.fillRoundRect(cx + s * 0.06f, cy - s * 0.46f, s * 0.21f, s * 0.72f, s * 0.10f, color);
                r.fillCircle(cx - s * 0.16f, cy - s * 0.44f, s * 0.11f, Palette.alpha(color, 0.8f));
                r.fillCircle(cx + s * 0.16f, cy - s * 0.44f, s * 0.11f, Palette.alpha(color, 0.8f));
                // 膝盖 + 脚掌
                r.line(cx - s * 0.27f, cy + s * 0.02f, cx - s * 0.06f, cy + s * 0.02f, s * 0.045f,
                        Palette.alpha(Palette.BLACK, 0.25f));
                r.line(cx + s * 0.06f, cy + s * 0.02f, cx + s * 0.27f, cy + s * 0.02f, s * 0.045f,
                        Palette.alpha(Palette.BLACK, 0.25f));
                r.fillRoundRect(cx - s * 0.30f, cy + s * 0.24f, s * 0.26f, s * 0.14f, s * 0.06f,
                        Palette.alpha(color, 0.95f));
                r.fillRoundRect(cx + s * 0.04f, cy + s * 0.24f, s * 0.26f, s * 0.14f, s * 0.06f,
                        Palette.alpha(color, 0.95f));
                break;
            }
            case LIVER: {
                r.fillPolygon(new float[]{
                        cx - s * 0.42f, cy - s * 0.26f,
                        cx + s * 0.06f, cy - s * 0.40f,
                        cx + s * 0.42f, cy - s * 0.18f,
                        cx + s * 0.30f, cy + s * 0.30f,
                        cx - s * 0.16f, cy + s * 0.38f,
                        cx - s * 0.40f, cy + s * 0.10f
                }, color);
                r.line(cx - s * 0.10f, cy - s * 0.20f, cx + s * 0.04f, cy + s * 0.14f, s * 0.05f,
                        Palette.alpha(Palette.BLACK, 0.30f));
                break;
            }
            case LUNGS: {
                r.fillRoundRect(cx - s * 0.40f, cy - s * 0.30f, s * 0.30f, s * 0.70f, s * 0.13f,
                        Palette.alpha(color, 0.92f));
                r.fillRoundRect(cx + s * 0.10f, cy - s * 0.30f, s * 0.30f, s * 0.70f, s * 0.13f,
                        Palette.alpha(color, 0.92f));
                r.line(cx, cy - s * 0.46f, cx, cy - s * 0.14f, s * 0.07f, color);
                r.line(cx, cy - s * 0.14f, cx - s * 0.20f, cy - s * 0.02f, s * 0.06f, color);
                r.line(cx, cy - s * 0.14f, cx + s * 0.20f, cy - s * 0.02f, s * 0.06f, color);
                break;
            }
            case ARMS: {
                // 肩线 + 双臂 + 拳头
                r.fillRoundRect(cx - s * 0.26f, cy - s * 0.42f, s * 0.52f, s * 0.13f, s * 0.06f,
                        Palette.alpha(color, 0.85f));
                r.line(cx - s * 0.24f, cy - s * 0.34f, cx - s * 0.36f, cy + s * 0.20f, s * 0.13f, color);
                r.line(cx + s * 0.24f, cy - s * 0.34f, cx + s * 0.36f, cy + s * 0.20f, s * 0.13f, color);
                r.fillCircle(cx - s * 0.37f, cy + s * 0.26f, s * 0.12f, Palette.alpha(color, 0.95f));
                r.fillCircle(cx + s * 0.37f, cy + s * 0.26f, s * 0.12f, Palette.alpha(color, 0.95f));
                break;
            }
            default:
                r.fillCircle(cx, cy, s * 0.34f, color);
                break;
        }
    }

    // ==================================================================
    // 通用头部（子界面）
    // ==================================================================

    /** 子界面统一标题区。返回标题下方的 y。 */
    public static float header(Renderer r, String title, String rightNote, float time) {
        r.setTextSize(58f);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.BLACK, 0.55f));
        r.drawText(title, W * 0.5f + 2f, SAFE_TOP + 60f + 3f);
        r.setTextColor(Palette.TEXT);
        r.drawText(title, W * 0.5f, SAFE_TOP + 60f);
        r.fillRoundRect(W * 0.5f - 40f, SAFE_TOP + 92f, 80f, 5f, 2.5f, Palette.alpha(Palette.MINT, 0.85f));
        r.fillCircle(W * 0.5f - 52f, SAFE_TOP + 94.5f, 3f, Palette.alpha(Palette.MINT, 0.6f));
        r.fillCircle(W * 0.5f + 52f, SAFE_TOP + 94.5f, 3f, Palette.alpha(Palette.MINT, 0.6f));
        if (rightNote != null) {
            r.setTextSize(22f);
            r.setTextBold(false);
            r.setTextAlign(2);
            r.setTextColor(Palette.TEXT_DIM);
            r.drawText(rightNote, W - MARGIN, SAFE_TOP + 58f);
        }
        return SAFE_TOP + 108f;
    }

    /** 小标签徽章。 */
    public static void badge(Renderer r, float cx, float cy, String label, int color, float size) {
        r.setTextSize(size);
        r.setTextBold(true);
        float w = r.measureText(label) + size * 1.2f;
        float h = size * 1.6f;
        r.fillRoundRect(cx - w * 0.5f, cy - h * 0.5f, w, h, h * 0.4f, Palette.alpha(color, 0.18f));
        r.strokeRoundRect(cx - w * 0.5f, cy - h * 0.5f, w, h, h * 0.4f, 1.5f, Palette.alpha(color, 0.8f));
        r.setTextAlign(1);
        r.setTextColor(color);
        r.drawText(label, cx, cy);
    }

    /** 圆角返回按钮（左上角）。 */
    public static void backCircle(Renderer r, Rect b, boolean pressed, boolean enabled) {
        r.fillRoundRect(b.x + 2f, b.y + 5f, b.w, b.h, b.h * 0.5f, Palette.alpha(Palette.BLACK, 0.5f));
        r.fillRoundRect(b.x, b.y, b.w, b.h, b.h * 0.5f,
                Palette.alpha(Palette.PANEL_ALT, enabled ? 0.95f : 0.5f));
        r.strokeRoundRect(b.x, b.y, b.w, b.h, b.h * 0.5f, 2.5f,
                Palette.alpha(enabled ? Palette.PANEL_STROKE : Palette.TEXT_DARK, 0.9f));
        if (pressed) {
            r.fillRoundRect(b.x, b.y, b.w, b.h, b.h * 0.5f, Palette.alpha(Palette.BLACK, 0.18f));
        }
        iconBack(r, b.cx(), b.cy(), b.h * 0.52f, enabled ? Palette.MINT : Palette.TEXT_DARK);
    }

    /** 底部小提示（自动淡出）。 */
    public static void hint(Renderer r, String text, float y, float alpha) {
        r.setTextSize(22f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT_DARK, alpha));
        r.drawText(text, W * 0.5f, y);
    }
}
