package com.crazyclinic.core;

/**
 * 平台无关 2D 绘制接口。
 * 逻辑坐标系固定为 720 x 1280（竖屏），Android 端按屏幕等比缩放并做黑边适配。
 * 实现类：AndroidRenderer（android.graphics.Canvas）、Java2DRenderer（桌面预览截图）。
 */
public interface Renderer {
    float WIDTH = 720f;
    float HEIGHT = 1280f;

    void clear(int color);

    void save();
    void restore();
    void translate(float dx, float dy);
    void rotate(float degrees);
    void scale(float sx, float sy);
    void clipRect(float x, float y, float w, float h);

    /** 全局透明度乘数，0..1，影响后续所有绘制，save/restore 会保存和恢复。 */
    void setAlpha(float alpha);
    float alpha();

    void fillRect(float x, float y, float w, float h, int color);
    void fillRoundRect(float x, float y, float w, float h, float radius, int color);
    void strokeRoundRect(float x, float y, float w, float h, float radius, float strokeWidth, int color);
    void strokeRect(float x, float y, float w, float h, float strokeWidth, int color);
    void fillCircle(float cx, float cy, float radius, int color);
    void strokeCircle(float cx, float cy, float radius, float strokeWidth, int color);
    void line(float x1, float y1, float x2, float y2, float strokeWidth, int color);
    void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int color);
    /** points = [x0,y0,x1,y1,...] 闭合多边形。 */
    void fillPolygon(float[] points, int color);
    void fillArc(float cx, float cy, float radius, float startDegrees, float sweepDegrees, int color);
    void fillVerticalGradient(float x, float y, float w, float h, int topColor, int bottomColor);
    void fillRadialGradient(float cx, float cy, float radius, int innerColor, int outerColor);

    void setTextSize(float sizePx);
    void setTextBold(boolean bold);
    /** 0 = 左对齐, 1 = 居中, 2 = 右对齐 */
    void setTextAlign(int align);
    void setTextColor(int color);
    /** y 为文字垂直中心 */
    void drawText(String text, float x, float y);
    float measureText(String text);
}
