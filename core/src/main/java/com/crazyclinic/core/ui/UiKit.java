package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.content.Texts;

/** 菜单通用绘制工具（UI 代理可扩展/替换，保持静态方法签名可用即可）。 */
public final class UiKit {
    private UiKit() {}

    /** style: 0 次级按钮, 1 主按钮, 2 危险按钮 */
    public static void button(Renderer r, Rect b, String label, int style) {
        int fill = style == 1 ? Palette.MINT : (style == 2 ? Palette.RED_DARK : Palette.PANEL_ALT);
        int textColor = style == 1 ? Palette.BG_DARK : Palette.TEXT;
        r.fillRoundRect(b.x, b.y, b.w, b.h, 18f, Palette.alpha(fill, 0.95f));
        r.strokeRoundRect(b.x, b.y, b.w, b.h, 18f, 3f, Palette.PANEL_STROKE);
        r.setTextSize(b.h * 0.36f);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(textColor);
        r.drawText(label, b.cx(), b.cy());
    }

    public static boolean hit(Rect b, float x, float y) {
        return b.contains(x, y);
    }

    public static void scrim(Renderer r, float alpha) {
        r.fillRect(0f, 0f, Game.WIDTH, Game.HEIGHT, Palette.alpha(Palette.BLACK, alpha));
    }

    public static void panel(Renderer r, Rect b) {
        r.fillRoundRect(b.x, b.y, b.w, b.h, 24f, Palette.alpha(Palette.PANEL, 0.97f));
        r.strokeRoundRect(b.x, b.y, b.w, b.h, 24f, 3f, Palette.PANEL_STROKE);
    }

    /** 自动换行绘制（按字符，适合中文）。返回绘制后的 y。 */
    public static float wrapped(Renderer r, String text, float x, float y, float maxWidth,
                                float size, int color, float lineGap) {
        r.setTextSize(size);
        r.setTextAlign(0);
        r.setTextColor(color);
        String[] lines = text.split("\n", -1);
        for (String line : lines) {
            if (line.isEmpty()) {
                y += size + lineGap;
                continue;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                sb.append(c);
                if (r.measureText(sb.toString()) > maxWidth) {
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
        int s = (int) seconds;
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
