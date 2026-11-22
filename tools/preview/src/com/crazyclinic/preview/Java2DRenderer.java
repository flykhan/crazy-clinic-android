package com.crazyclinic.preview;

import com.crazyclinic.core.Renderer;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 桌面预览用的 {@link Renderer} 实现：BufferedImage(720x1280) + Graphics2D。
 * <p>
 * 与 Android 端 {@code AndroidRenderer} 保持一致的语义：
 * <ul>
 *   <li>坐标 720x1280，y 向下；rotate 正方向为屏幕顺时针；</li>
 *   <li>文字 y 为垂直中心；对齐 0/1/2 = 左/中/右；</li>
 *   <li>{@code fillArc} 角度 0 度 = 3 点钟方向，正 sweep 顺时针（与 Android Canvas.drawArc 相同）；</li>
 *   <li>{@code fillRadialGradient} 填满整个屏幕（以给定圆心/半径做径向渐变）；</li>
 *   <li>{@code setAlpha} 为全局乘数，save/restore 会保存和恢复；</li>
 *   <li>{@code clear} 忽略当前变换，整屏覆盖。</li>
 * </ul>
 * 字体优先使用 DroidSansFallbackFull（中文必需），找不到时回退 SansSerif 并打印警告。
 */
public final class Java2DRenderer implements Renderer {

    private static final int IMG_W = (int) WIDTH;
    private static final int IMG_H = (int) HEIGHT;

    /** 优先尝试的 CJK 字体（含 Droid 兜底）。 */
    private static final String[] FONT_CANDIDATES = {
            "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf",
            "/usr/share/fonts/truetype/droid/DroidSansFallback.ttf",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.otf",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/truetype/arphic/uming.ttc",
    };

    /**
     * 拉丁/符号字体：DroidSansFallbackFull 只包含 CJK 与符号、**没有 ASCII**，
     * 因此纯用它渲染 "J2ME v1.0" 之类会得到方块；这里按字符逐个回退到 DejaVu Sans。
     */
    private static final String[] LATIN_CANDIDATES = {
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
    };

    private static final String[] LATIN_BOLD_CANDIDATES = {
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
    };

    private static Font baseFont;
    private static Font latinBase;
    private static Font latinBoldBase;
    private static boolean fontWarned;

    private final BufferedImage image;
    private final Graphics2D g;

    private float alpha = 1f;

    private float textSize = 28f;
    private boolean textBold;
    private int textAlign;
    private int textColor = 0xFFEDF1F5;
    private Font cjkFont;
    private Font latinFont;
    private FontMetrics cjkMetrics;
    private FontMetrics latinMetrics;

    private State[] stack = new State[64];
    private int depth;

    private static final class State {
        AffineTransform transform;
        Shape clip;
        float alpha;
    }

    public Java2DRenderer() {
        image = new BufferedImage(IMG_W, IMG_H, BufferedImage.TYPE_INT_ARGB);
        g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        rebuildFont();
    }

    // ------------------------------------------------------------------
    // 字体
    // ------------------------------------------------------------------

    private static synchronized Font loadFont(String[] candidates, String fallbackFamily, boolean bold, String what) {
        for (String path : candidates) {
            try {
                File f = new File(path);
                if (!f.isFile()) continue;
                Font fnt = Font.createFont(Font.TRUETYPE_FONT, f);
                System.out.println("[preview] " + what + ": " + path + " (" + fnt.getFontName() + ")");
                return fnt;
            } catch (Exception ignored) {
                // try next
            }
        }
        System.out.println("[preview] WARNING: no " + what + " found, fallback to " + fallbackFamily);
        return new Font(fallbackFamily, bold ? Font.BOLD : Font.PLAIN, 16);
    }

    private static synchronized Font baseFont() {
        if (baseFont == null) {
            baseFont = loadFont(FONT_CANDIDATES, Font.SANS_SERIF, false, "CJK font");
        }
        return baseFont;
    }

    private static synchronized Font latinBase() {
        if (latinBase == null) {
            latinBase = loadFont(LATIN_CANDIDATES, Font.SANS_SERIF, false, "latin font");
        }
        return latinBase;
    }

    private static synchronized Font latinBoldBase() {
        if (latinBoldBase == null) {
            latinBoldBase = loadFont(LATIN_BOLD_CANDIDATES, Font.SANS_SERIF, true, "latin bold font");
        }
        return latinBoldBase;
    }

    private void rebuildFont() {
        float size = Math.max(4f, textSize);
        cjkFont = baseFont().deriveFont(size);
        latinFont = (textBold ? latinBoldBase() : latinBase()).deriveFont(size);
        if (textBold) {
            // DroidSansFallback 只有 Regular；drawText 会再叠一次偏移描边模拟粗体。
            cjkFont = cjkFont.deriveFont(Font.BOLD);
        }
        cjkMetrics = g.getFontMetrics(cjkFont);
        latinMetrics = g.getFontMetrics(latinFont);
    }

    /** 拉丁字体是否可显示该字符（CJK 主字体缺 ASCII，需要逐字回退）。 */
    private boolean preferLatin(char c) {
        if (c == ' ') return false;                  // 空格用哪个都一样，跟主字体走
        if (cjkFont.canDisplay(c)) return false;
        return latinFont.canDisplay(c);
    }

    private Color col(int argb) {
        int a = (argb >>> 24) & 0xFF;
        float ef = alpha * (a / 255f);
        if (ef <= 0f) return null;
        int ai = (int) (ef * 255f + 0.5f);
        if (ai > 255) ai = 255;
        return new Color((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, ai);
    }

    private static float safe(float v) {
        if (Float.isNaN(v) || Float.isInfinite(v)) return 0f;
        return v;
    }

    private static float norm(float w) {
        return (w <= 0f) ? 1f : w;
    }

    // ------------------------------------------------------------------
    // 画布 / 变换 / 状态
    // ------------------------------------------------------------------

    @Override
    public void clear(int color) {
        g.setComposite(AlphaComposite.Src);
        g.setColor(new Color(color, true));
        g.fillRect(0, 0, IMG_W, IMG_H);
        g.setComposite(AlphaComposite.SrcOver);
    }

    @Override
    public void save() {
        if (depth == stack.length) {
            State[] bigger = new State[stack.length * 2];
            System.arraycopy(stack, 0, bigger, 0, stack.length);
            stack = bigger;
        }
        State s = stack[depth];
        if (s == null) {
            s = new State();
            stack[depth] = s;
        }
        s.transform = g.getTransform();
        s.clip = g.getClip();
        s.alpha = alpha;
        depth++;
    }

    @Override
    public void restore() {
        if (depth == 0) return;
        depth--;
        State s = stack[depth];
        g.setTransform(s.transform);
        g.setClip(s.clip);
        alpha = s.alpha;
    }

    @Override
    public void translate(float dx, float dy) {
        g.translate(safe(dx), safe(dy));
    }

    @Override
    public void rotate(float degrees) {
        g.rotate(Math.toRadians(safe(degrees)));
    }

    @Override
    public void scale(float sx, float sy) {
        if (Math.abs(sx) < 1e-6f || Math.abs(sy) < 1e-6f) return;
        g.scale(safe(sx), safe(sy));
    }

    @Override
    public void clipRect(float x, float y, float w, float h) {
        if (w <= 0f || h <= 0f) return;
        g.clip(new Rectangle2D.Float(safe(x), safe(y), safe(w), safe(h)));
    }

    @Override
    public void setAlpha(float a) {
        if (Float.isNaN(a)) a = 1f;
        alpha = a < 0f ? 0f : (a > 1f ? 1f : a);
    }

    @Override
    public float alpha() {
        return alpha;
    }

    // ------------------------------------------------------------------
    // 填充图元
    // ------------------------------------------------------------------

    @Override
    public void fillRect(float x, float y, float w, float h, int color) {
        if (w <= 0f || h <= 0f) return;
        Color c = col(color);
        if (c == null) return;
        g.setColor(c);
        g.fill(new Rectangle2D.Float(safe(x), safe(y), safe(w), safe(h)));
    }

    @Override
    public void fillRoundRect(float x, float y, float w, float h, float radius, int color) {
        if (w <= 0f || h <= 0f) return;
        Color c = col(color);
        if (c == null) return;
        float r = Math.max(0f, Math.min(radius, Math.min(w, h) * 0.5f));
        g.setColor(c);
        g.fill(new RoundRectangle2D.Float(safe(x), safe(y), safe(w), safe(h), r * 2f, r * 2f));
    }

    @Override
    public void strokeRoundRect(float x, float y, float w, float h, float radius, float strokeWidth, int color) {
        if (w <= 0f || h <= 0f) return;
        Color c = col(color);
        if (c == null) return;
        float r = Math.max(0f, Math.min(radius, Math.min(w, h) * 0.5f));
        g.setColor(c);
        g.setStroke(stroke(strokeWidth));
        g.draw(new RoundRectangle2D.Float(safe(x), safe(y), safe(w), safe(h), r * 2f, r * 2f));
    }

    @Override
    public void strokeRect(float x, float y, float w, float h, float strokeWidth, int color) {
        if (w <= 0f || h <= 0f) return;
        Color c = col(color);
        if (c == null) return;
        g.setColor(c);
        g.setStroke(stroke(strokeWidth));
        g.draw(new Rectangle2D.Float(safe(x), safe(y), safe(w), safe(h)));
    }

    @Override
    public void fillCircle(float cx, float cy, float radius, int color) {
        if (radius <= 0f) return;
        Color c = col(color);
        if (c == null) return;
        g.setColor(c);
        g.fill(new Ellipse2D.Float(safe(cx - radius), safe(cy - radius), radius * 2f, radius * 2f));
    }

    @Override
    public void strokeCircle(float cx, float cy, float radius, float strokeWidth, int color) {
        if (radius <= 0f) return;
        Color c = col(color);
        if (c == null) return;
        g.setColor(c);
        g.setStroke(stroke(strokeWidth));
        g.draw(new Ellipse2D.Float(safe(cx - radius), safe(cy - radius), radius * 2f, radius * 2f));
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float strokeWidth, int color) {
        Color c = col(color);
        if (c == null) return;
        g.setColor(c);
        g.setStroke(stroke(strokeWidth));
        g.draw(new Line2D.Float(safe(x1), safe(y1), safe(x2), safe(y2)));
    }

    @Override
    public void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        Color c = col(color);
        if (c == null) return;
        Path2D.Float p = new Path2D.Float();
        p.moveTo(safe(x1), safe(y1));
        p.lineTo(safe(x2), safe(y2));
        p.lineTo(safe(x3), safe(y3));
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }

    @Override
    public void fillPolygon(float[] points, int color) {
        if (points == null || points.length < 6) return;
        Color c = col(color);
        if (c == null) return;
        Path2D.Float p = new Path2D.Float();
        p.moveTo(safe(points[0]), safe(points[1]));
        for (int i = 2; i + 1 < points.length; i += 2) {
            p.lineTo(safe(points[i]), safe(points[i + 1]));
        }
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }

    @Override
    public void fillArc(float cx, float cy, float radius, float startDegrees, float sweepDegrees, int color) {
        if (radius <= 0f || sweepDegrees == 0f) return;
        Color c = col(color);
        if (c == null) return;
        // Java2D 的角度在 y 向上的数学坐标里是逆时针；屏幕 y 向下，取负号即与
        // Android Canvas.drawArc（正 sweep = 屏幕顺时针）一致。
        Arc2D.Float arc = new Arc2D.Float(
                safe(cx - radius), safe(cy - radius), radius * 2f, radius * 2f,
                -safe(startDegrees), -safe(sweepDegrees), Arc2D.PIE);
        g.setColor(c);
        g.fill(arc);
    }

    @Override
    public void fillVerticalGradient(float x, float y, float w, float h, int topColor, int bottomColor) {
        if (w <= 0f || h <= 0f) return;
        Color top = col(topColor);
        Color bottom = col(bottomColor);
        if (top == null && bottom == null) return;
        if (top == null) top = bottom;
        if (bottom == null) bottom = top;
        g.setPaint(new GradientPaint(safe(x), safe(y), top, safe(x), safe(y + h), bottom, false));
        g.fill(new Rectangle2D.Float(safe(x), safe(y), safe(w), safe(h)));
        g.setPaint(null);
    }

    @Override
    public void fillRadialGradient(float cx, float cy, float radius, int innerColor, int outerColor) {
        Color inner = col(innerColor);
        Color outer = col(outerColor);
        if (inner == null && outer == null) return;
        if (inner == null) inner = new Color(outer.getRed(), outer.getGreen(), outer.getBlue(), 0);
        if (outer == null) outer = new Color(inner.getRed(), inner.getGreen(), inner.getBlue(), 0);
        float r = Math.max(0.5f, radius);
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(safe(cx), safe(cy)), r,
                new float[]{0f, 1f},
                new Color[]{inner, outer}));
        // 契约规定该图元负责整屏氛围，填满整个逻辑屏幕。
        g.fill(new Rectangle2D.Float(0f, 0f, WIDTH, HEIGHT));
        g.setPaint(null);
    }

    private static Stroke stroke(float w) {
        float width = norm(w);
        if (width < 0.8f) width = 0.8f;
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    // ------------------------------------------------------------------
    // 文字
    // ------------------------------------------------------------------

    @Override
    public void setTextSize(float sizePx) {
        if (sizePx <= 0f || Float.isNaN(sizePx)) sizePx = 4f;
        if (Math.abs(sizePx - textSize) < 0.01f) return;
        textSize = sizePx;
        rebuildFont();
    }

    @Override
    public void setTextBold(boolean bold) {
        if (bold == textBold) return;
        textBold = bold;
        rebuildFont();
    }

    @Override
    public void setTextAlign(int align) {
        textAlign = align;
    }

    @Override
    public void setTextColor(int color) {
        textColor = color;
    }

    @Override
    public void drawText(String text, float x, float y) {
        if (text == null || text.isEmpty()) return;
        Color c = col(textColor);
        if (c == null) return;
        float w = measureText(text);
        float dx = safe(x);
        if (textAlign == 1) dx -= w * 0.5f;
        else if (textAlign == 2) dx -= w;
        // y = 视觉垂直中心 → 换算基线（以 CJK 主字体为准，混排时基线统一）
        float baseline = safe(y) + (cjkMetrics.getAscent() - cjkMetrics.getDescent()) * 0.5f;
        g.setColor(c);
        int n = text.length();
        int i = 0;
        float cx = dx;
        while (i < n) {
            boolean latin = preferLatin(text.charAt(i));
            int j = i + 1;
            while (j < n && preferLatin(text.charAt(j)) == latin) j++;
            String run = text.substring(i, j);
            Font f = latin ? latinFont : cjkFont;
            FontMetrics fm = latin ? latinMetrics : cjkMetrics;
            g.setFont(f);
            g.drawString(run, cx, baseline);
            if (textBold && !latin) {
                // 合成粗体：再画一遍轻微右移，弥补 DroidSansFallback 无粗体字面
                float off = Math.max(0.7f, textSize * 0.035f);
                g.drawString(run, cx + off, baseline);
            }
            cx += fm.stringWidth(run);
            i = j;
        }
    }

    @Override
    public float measureText(String text) {
        if (text == null || text.isEmpty()) return 0f;
        float w = 0f;
        int n = text.length();
        int i = 0;
        while (i < n) {
            boolean latin = preferLatin(text.charAt(i));
            int j = i + 1;
            while (j < n && preferLatin(text.charAt(j)) == latin) j++;
            FontMetrics fm = latin ? latinMetrics : cjkMetrics;
            w += fm.stringWidth(text.substring(i, j));
            i = j;
        }
        return w;
    }

    // ------------------------------------------------------------------
    // 输出
    // ------------------------------------------------------------------

    public BufferedImage image() {
        return image;
    }

    public void writePng(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        if (!ImageIO.write(image, "png", file)) {
            throw new IOException("no PNG writer available for " + file);
        }
    }

    public void dispose() {
        g.dispose();
    }
}
