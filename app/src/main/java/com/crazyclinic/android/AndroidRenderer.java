package com.crazyclinic.android;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import com.crazyclinic.core.Renderer;

/**
 * Android Canvas 版 {@link Renderer}。
 * <p>
 * 逻辑坐标固定 720x1280，由 {@link GameView} 负责等比缩放 + 黑边 + 平移；
 * 本类只做「逻辑坐标 → 当前 Canvas」的绘制，因此所有尺寸/文字都直接用逻辑 px。
 * 语义与桌面 {@code Java2DRenderer} 完全一致（rotate 正=屏幕顺时针、drawText 的 y=垂直中心、
 * fillArc 0 度=3 点钟方向且正 sweep 顺时针、fillRadialGradient 填满全屏）。
 */
public final class AndroidRenderer implements Renderer {

    private Canvas canvas;

    /** 实心/描边共用一个 Paint，避免每帧分配。 */
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG
            | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF rect = new RectF();

    private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);

    private float alpha = 1f;
    private float textSize = 28f;
    private boolean textBold;
    private int textAlign;
    private int textColor = 0xFFEDF1F5;

    /** 与 Canvas.save/restore 成对使用的 alpha 栈（Canvas 自身不保存 Paint alpha）。 */
    private float[] alphaStack = new float[64];
    private int depth;

    public AndroidRenderer() {
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        textPaint.setTextSize(textSize);
        textPaint.setTypeface(regular);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(textColor);
    }

    /** 每帧由 GameView 在 lockCanvas 后调用。 */
    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
        // 重置到已保存的根状态（正常情况下 GameView 每帧都会 save/restore 平衡）
        depth = 0;
        alpha = 1f;
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
    }

    // ------------------------------------------------------------------
    // 状态
    // ------------------------------------------------------------------

    private int applyAlpha(int color) {
        int a = (color >>> 24) & 0xFF;
        int na = (int) (a * alpha + 0.5f);
        if (na < 0) na = 0;
        if (na > 255) na = 255;
        return (color & 0x00FFFFFF) | (na << 24);
    }

    private void fillColor(int color) {
        if (paint.getShader() != null) paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(applyAlpha(color));
    }

    private void strokeColor(int color, float strokeWidth) {
        if (paint.getShader() != null) paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth < 0.8f ? 0.8f : strokeWidth);
        paint.setColor(applyAlpha(color));
    }

    @Override
    public void clear(int color) {
        if (canvas == null) return;
        canvas.drawColor(color);
    }

    @Override
    public void save() {
        if (canvas == null) return;
        canvas.save();
        if (depth == alphaStack.length) {
            float[] bigger = new float[alphaStack.length * 2];
            System.arraycopy(alphaStack, 0, bigger, 0, alphaStack.length);
            alphaStack = bigger;
        }
        alphaStack[depth++] = alpha;
    }

    @Override
    public void restore() {
        if (canvas == null) return;
        if (depth == 0) return;
        depth--;
        alpha = alphaStack[depth];
        canvas.restore();
    }

    @Override
    public void translate(float dx, float dy) {
        if (canvas != null) canvas.translate(dx, dy);
    }

    @Override
    public void rotate(float degrees) {
        if (canvas != null) canvas.rotate(degrees);
    }

    @Override
    public void scale(float sx, float sy) {
        if (canvas == null) return;
        if (Math.abs(sx) < 1e-6f || Math.abs(sy) < 1e-6f) return;
        canvas.scale(sx, sy);
    }

    @Override
    public void clipRect(float x, float y, float w, float h) {
        if (canvas == null || w <= 0f || h <= 0f) return;
        canvas.clipRect(x, y, x + w, y + h);
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
    // 图元
    // ------------------------------------------------------------------

    @Override
    public void fillRect(float x, float y, float w, float h, int color) {
        if (canvas == null || w <= 0f || h <= 0f) return;
        fillColor(color);
        canvas.drawRect(x, y, x + w, y + h, paint);
    }

    @Override
    public void fillRoundRect(float x, float y, float w, float h, float radius, int color) {
        if (canvas == null || w <= 0f || h <= 0f) return;
        fillColor(color);
        float r = Math.min(radius, Math.min(w, h) * 0.5f);
        rect.set(x, y, x + w, y + h);
        canvas.drawRoundRect(rect, r, r, paint);
    }

    @Override
    public void strokeRoundRect(float x, float y, float w, float h, float radius, float strokeWidth, int color) {
        if (canvas == null || w <= 0f || h <= 0f) return;
        strokeColor(color, strokeWidth);
        float r = Math.min(radius, Math.min(w, h) * 0.5f);
        rect.set(x, y, x + w, y + h);
        canvas.drawRoundRect(rect, r, r, paint);
    }

    @Override
    public void strokeRect(float x, float y, float w, float h, float strokeWidth, int color) {
        if (canvas == null || w <= 0f || h <= 0f) return;
        strokeColor(color, strokeWidth);
        canvas.drawRect(x, y, x + w, y + h, paint);
    }

    @Override
    public void fillCircle(float cx, float cy, float radius, int color) {
        if (canvas == null || radius <= 0f) return;
        fillColor(color);
        canvas.drawCircle(cx, cy, radius, paint);
    }

    @Override
    public void strokeCircle(float cx, float cy, float radius, float strokeWidth, int color) {
        if (canvas == null || radius <= 0f) return;
        strokeColor(color, strokeWidth);
        canvas.drawCircle(cx, cy, radius, paint);
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float strokeWidth, int color) {
        if (canvas == null) return;
        strokeColor(color, strokeWidth);
        canvas.drawLine(x1, y1, x2, y2, paint);
    }

    @Override
    public void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        if (canvas == null) return;
        fillColor(color);
        if (path == null) path = new android.graphics.Path();
        path.reset();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.close();
        canvas.drawPath(path, paint);
    }

    private android.graphics.Path path;

    @Override
    public void fillPolygon(float[] points, int color) {
        if (canvas == null || points == null || points.length < 6) return;
        fillColor(color);
        if (path == null) path = new android.graphics.Path();
        path.reset();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i + 1 < points.length; i += 2) {
            path.lineTo(points[i], points[i + 1]);
        }
        path.close();
        canvas.drawPath(path, paint);
    }

    @Override
    public void fillArc(float cx, float cy, float radius, float startDegrees, float sweepDegrees, int color) {
        if (canvas == null || radius <= 0f || sweepDegrees == 0f) return;
        fillColor(color);
        rect.set(cx - radius, cy - radius, cx + radius, cy + radius);
        // Canvas.drawArc 正 sweep = 屏幕顺时针（y 向下），与桌面实现一致
        canvas.drawArc(rect, startDegrees, sweepDegrees, true, paint);
    }

    @Override
    public void fillVerticalGradient(float x, float y, float w, float h, int topColor, int bottomColor) {
        if (canvas == null || w <= 0f || h <= 0f) return;
        int top = applyAlpha(topColor);
        int bottom = applyAlpha(bottomColor);
        LinearGradient shader = new LinearGradient(x, y, x, y + h, top, bottom, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFFFFF);
        canvas.drawRect(x, y, x + w, y + h, paint);
        paint.setShader(null);
    }

    @Override
    public void fillRadialGradient(float cx, float cy, float radius, int innerColor, int outerColor) {
        if (canvas == null) return;
        float r = Math.max(0.5f, radius);
        RadialGradient shader = new RadialGradient(cx, cy, r,
                applyAlpha(innerColor), applyAlpha(outerColor), Shader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFFFFF);
        // 契约：该图元负责整屏氛围，填满 720x1280
        canvas.drawRect(0f, 0f, WIDTH, HEIGHT, paint);
        paint.setShader(null);
    }

    // ------------------------------------------------------------------
    // 文字
    // ------------------------------------------------------------------

    @Override
    public void setTextSize(float sizePx) {
        if (sizePx <= 0f || Float.isNaN(sizePx)) sizePx = 4f;
        textSize = sizePx;
        textPaint.setTextSize(sizePx);
    }

    @Override
    public void setTextBold(boolean bold) {
        if (bold == textBold) return;
        textBold = bold;
        textPaint.setTypeface(bold ? this.bold : regular);
    }

    @Override
    public void setTextAlign(int align) {
        if (align == textAlign) return;
        textAlign = align;
        textPaint.setTextAlign(align == 1 ? Paint.Align.CENTER : align == 2 ? Paint.Align.RIGHT : Paint.Align.LEFT);
    }

    @Override
    public void setTextColor(int color) {
        textColor = color;
        textPaint.setColor(applyAlpha(color));
    }

    @Override
    public void drawText(String text, float x, float y) {
        if (canvas == null || text == null || text.isEmpty()) return;
        // setTextColor 在 alpha 变化后需要重新应用（alpha 是全局乘数）
        textPaint.setColor(applyAlpha(textColor));
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = y - (fm.ascent + fm.descent) * 0.5f;
        canvas.drawText(text, x, baseline, textPaint);
    }

    @Override
    public float measureText(String text) {
        if (text == null || text.isEmpty()) return 0f;
        return textPaint.measureText(text);
    }
}
