package com.crazyclinic.core.render;

import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.world.TileMap;
import com.crazyclinic.core.world.TileType;
import com.crazyclinic.core.world.World;

/**
 * 程序化 tile 绘制：蓝灰方格地板 + 污渍/血迹细节、3/4 立体墙（顶面/侧面/墙裙/警示条）、
 * 窗户、门、电梯、安保电脑、储物柜、箱子、病床。全部用 MathUtil.hash01 做确定性细节。
 */
final class TileArt {
    private TileArt() {}

    static final int T = TileMap.TILE;

    // 每层病区主题：轻微色相偏移，保持医院蓝灰基调
    private static final int[] THEME_TINT = {
            0xFF4C6FA8, 0xFF4C8FA8, 0xFFA8544C, 0xFF5A6FA0, 0xFF9A6A88,
            0xFF2C3A58, 0xFF8A3A5A, 0xFF3A8A6A, 0xFF6A6A8A, 0xFF3A5A7A
    };

    static int themeTint(World w) {
        int idx = Math.max(0, Math.min(THEME_TINT.length - 1, w.floor - 1));
        return THEME_TINT[idx];
    }

    static boolean isWallLike(TileType t) {
        return t == TileType.WALL || t == TileType.WINDOW;
    }

    /** 是否遮光实体（用于决定墙正面朝向）。 */
    private static boolean solidLike(TileType t) {
        return t.solid;
    }

    // ==================== 地板 ====================

    static void drawFloor(Renderer r, TileMap map, int tx, int ty, int tint) {
        float x = tx * T, y = ty * T;
        boolean even = ((tx + ty) & 1) == 0;
        int base = Palette.mix(even ? Palette.FLOOR_A : Palette.FLOOR_B, tint, 0.06f);
        base = even ? Art.lighten(base, 0.035f) : Art.darken(base, 0.025f);
        r.fillRect(x, y, T, T, base);
        // 蓝灰方格：砖缝右侧/下侧 3px，避免重复画边
        r.fillRect(x + T - 3, y, 3, T, Palette.FLOOR_GROUT);
        r.fillRect(x, y + T - 3, T, 3, Palette.FLOOR_GROUT);
        r.fillRect(x + T - 3, y, 1, T, Art.darken(Palette.FLOOR_GROUT, 0.3f));
        r.fillRect(x, y + T - 3, T, 1, Art.darken(Palette.FLOOR_GROUT, 0.3f));
        // 上/左边微弱受光
        r.fillRect(x, y, T, 2, Art.lighten(base, 0.09f));
        r.fillRect(x, y, 2, T, Art.lighten(base, 0.05f));
        r.fillRect(x + T - 3, y, 3, 2, Art.a(Palette.FLOOR_GROUT, 0.6f));

        float h = Art.hash(tx, ty);
        if (h > 0.84f) {
            // 污渍：两小团暗斑 + 一点锈色（不规则，避免大圆斑）
            float ox = 10f + Art.hash(tx + 41, ty + 7) * 48f;
            float oy = 12f + Art.hash(tx + 17, ty + 53) * 40f;
            float rx = 3.5f + Art.hash(tx + 3, ty + 91) * 6f;
            Art.ellipse(r, x + ox, y + oy, rx, rx * 0.6f, Art.a(Palette.FLOOR_GROUT, 0.40f));
            if (h > 0.92f) {
                Art.ellipse(r, x + T - ox * 0.7f, y + T - oy * 0.55f,
                        rx * 0.7f, rx * 0.45f, Art.a(0xFF6A4A30, 0.22f));
            }
            if (h > 0.975f) {
                // 裂纹
                r.line(x + 14, y + 12, x + 30, y + 30, 1.4f, Art.a(Palette.FLOOR_GROUT, 0.8f));
                r.line(x + 30, y + 30, x + 26, y + 46, 1.3f, Art.a(Palette.FLOOR_GROUT, 0.7f));
            }
        } else if (h > 0.55f) {
            // 高光小点 / 磨损
            Art.ellipse(r, x + 20 + h * 30f, y + 24 + h * 18f, 2.6f, 1.5f,
                    Art.a(Palette.WHITE, 0.045f));
        }

        // 地面细节：地漏 / 废纸 / 陈旧血渍（确定性分布，增强医院病区氛围）
        float d = Art.hash(tx + 101, ty + 37);
        if (d > 0.955f) {
            // 圆形地漏
            float dx = x + 20f + Art.hash(tx + 3, ty + 77) * 32f;
            float dy = y + 20f + Art.hash(tx + 9, ty + 13) * 32f;
            Art.ellipse(r, dx, dy, 12f, 12f, Art.a(0xFF1A2028, 0.75f));
            r.strokeCircle(dx, dy, 11f, 1.6f, Art.a(0xFF5A6472, 0.55f));
            for (int i = 0; i < 3; i++) r.fillRect(dx - 9f, dy - 4f + i * 3.6f, 18f, 1.6f, Art.a(0xFF3E4652, 0.9f));
        } else if (d > 0.90f) {
            // 地上的废纸 / 药板
            float dx = x + 16f + Art.hash(tx + 51, ty + 5) * 38f;
            float dy = y + 18f + Art.hash(tx + 23, ty + 91) * 36f;
            r.fillRect(dx + 1.5f, dy + 1.5f, 13f, 9f, Art.a(Palette.BLACK, 0.25f));
            r.fillRect(dx, dy, 13f, 9f, Art.a(0xFFD8D4C8, 0.55f));
            r.fillRect(dx + 2f, dy + 3f, 9f, 1.2f, Art.a(0xFF8A8478, 0.7f));
            r.fillRect(dx + 2f, dy + 6f, 6f, 1.2f, Art.a(0xFF8A8478, 0.6f));
        } else if (d > 0.865f) {
            // 陈旧血渍（擦过的痕迹）
            float dx = x + 18f + Art.hash(tx + 31, ty + 61) * 36f;
            float dy = y + 22f + Art.hash(tx + 17, ty + 43) * 30f;
            Art.ellipseRot(r, dx, dy, 11f, 4f, h * 90f, Art.a(0xFF5A1418, 0.22f));
            Art.ellipse(r, dx - 8f, dy + 4f, 3f, 2f, Art.a(0xFF5A1418, 0.18f));
        }
    }

    /** 地板与墙相接的接触阴影（在墙下方、物体上方）。 */
    static void drawContactShadow(Renderer r, TileMap map, int tx, int ty) {
        float x = tx * T, y = ty * T;
        if (isWallLike(solidType(map, tx, ty - 1))) {
            r.fillVerticalGradient(x, y, T, 10f,
                    Art.a(Palette.BLACK, 0.28f), Art.a(Palette.BLACK, 0f));
        }
        if (isWallLike(solidType(map, tx - 1, ty))) {
            for (int i = 0; i < 3; i++) {
                r.fillRect(x + i * 2f, y, 2, T, Art.a(Palette.BLACK, 0.10f - i * 0.03f));
            }
        }
        if (isWallLike(solidType(map, tx + 1, ty))) {
            for (int i = 0; i < 3; i++) {
                r.fillRect(x + T - 2 - i * 2f, y, 2, T, Art.a(Palette.BLACK, 0.10f - i * 0.03f));
            }
        }
    }

    private static TileType solidType(TileMap map, int tx, int ty) {
        return map.type(tx, ty);
    }

    // ==================== 墙 ====================

    static void drawWall(Renderer r, TileMap map, int tx, int ty, int tint) {
        float x = tx * T, y = ty * T;
        boolean southOpen = !solidLike(map.type(tx, ty + 1));
        int cTop = Palette.mix(Art.lighten(Art.WALL_TOP, 0.10f), tint, 0.07f);
        int faceH = southOpen ? 30 : 0;
        int topH = T - (southOpen ? 26 : 0);   // 正面略微探出格子 4px，形成 3/4 立体感
        int faceY = (int) (y + topH);

        // 顶面
        r.fillRect(x, y, T, topH, cTop);
        // 侧面/顶面分块缝（对齐 72px 网格，不画太密）
        r.fillRect(x, y + 40, T, 2f, Art.a(Art.darken(cTop, 0.30f), 0.75f));
        if (((tx + ty) & 1) == 0) {
            r.fillRect(x + 34f, y, 2f, 40f, Art.a(Art.darken(cTop, 0.25f), 0.6f));
        }
        // 受光边 + 顶部轮廓（让墙从地板里“立”出来）
        r.fillRect(x, y, T, 3f, Art.lighten(cTop, 0.16f));
        r.fillRect(x, y, 3f, topH, Art.lighten(cTop, 0.08f));
        r.fillRect(x, y + topH - 3f, T, 3f, Art.darken(cTop, 0.35f));
        r.fillRect(x + T - 3f, y, 3f, topH, Art.darken(cTop, 0.22f));

        // 顶面随机痕迹
        float h = Art.hash(tx + 7, ty + 3);
        if (h > 0.88f) {
            r.line(x + 12f, y + 6f, x + 30f, y + 18f, 1.6f, Art.a(Art.darken(cTop, 0.45f), 0.8f));
            r.line(x + 30f, y + 18f, x + 38f, y + 34f, 1.4f, Art.a(Art.darken(cTop, 0.4f), 0.6f));
        } else if (h < 0.10f) {
            // 通风口
            r.fillRect(x + 22f, y + 10f, 28f, 12f, Art.a(Art.darken(cTop, 0.55f), 0.8f));
            for (int i = 0; i < 3; i++) r.fillRect(x + 24f, y + 12 + i * 3, 24, 1.4f, Art.a(0xFF7A8798, 0.6f));
        } else if (h > 0.80f) {
            // 墙面污渍
            Art.ellipse(r, x + 20f + h * 30f, y + 20f + h * 12f, 7f, 4.5f,
                    Art.a(Art.darken(cTop, 0.4f), 0.25f));
        }

        if (southOpen) {
            drawWallFace(r, x, faceY, tint);
        }
    }

    /** 墙的竖直正面：踢脚墙裙 + 黄色警示条 + 顶部压条（略微探出格子形成立体感）。 */
    private static void drawWallFace(Renderer r, float x, float y, int tint) {
        int side = Palette.mix(Art.darken(Art.WALL_SIDE, 0.12f), tint, 0.06f);
        r.fillRect(x, y, T, 30, side);
        r.fillRect(x, y, T, 3f, Art.a(Palette.BLACK, 0.45f));
        // 顶部压条
        r.fillRect(x, y, T, 3, Art.TRIM);
        r.fillRect(x, y + 3, T, 1.5f, Art.a(Palette.BLACK, 0.35f));
        // 黄色警示条（窄一些、压低饱和度）
        float sy = y + 7;
        r.fillRect(x, sy, T, 5, Art.a(Art.HAZARD, 0.72f));
        for (int i = 0; i < 2; i++) {
            float bx = x + i * 36f;
            r.fillTriangle(bx + 2f, sy + 5, bx + 14f, sy, bx + 22f, sy, Art.a(Art.HAZARD_DARK, 0.7f));
            r.fillTriangle(bx + 14f, sy + 5, bx + 22f, sy, bx + 30f, sy + 5, Art.a(Art.HAZARD_DARK, 0.7f));
            r.fillTriangle(bx + 18f, sy + 5, bx + 30f, sy, bx + 36f, sy + 5, Art.a(Art.HAZARD_DARK, 0.7f));
        }
        // 深色墙裙
        r.fillRect(x, y + 14, T, 16, Art.WAINSCOT);
        r.fillRect(x, y + 14, T, 1.5f, Art.a(Palette.BLACK, 0.5f));
        // 墙面标识：十字牌 / 科室牌 / 管线（确定性）
        float hw = Art.hash((int) x / T + 211, (int) y / T + 97);
        if (hw > 0.72f) {
            float sx = x + 10f + Art.hash((int) x + 7, (int) y + 5) * 26f;
            r.fillRect(sx, y + 16f, 22f, 12f, Art.a(0xFFE8EEF2, 0.88f));
            r.strokeRect(sx, y + 16f, 22f, 12f, 1f, Art.a(0xFF5A6472, 0.8f));
            r.fillRect(sx + 9f, y + 18f, 4f, 8f, Art.a(0xFFD8323C, 0.9f));
            r.fillRect(sx + 6f, y + 20f, 10f, 4f, Art.a(0xFFD8323C, 0.9f));
        } else if (hw < 0.10f) {
            r.fillRect(x + 2f, y + 18f, T - 4f, 3f, Art.a(0xFF8A96A6, 0.55f));
            r.fillRect(x + 16f, y + 15f, 4f, 8f, Art.a(0xFF5A6472, 0.7f));
            r.fillRect(x + 50f, y + 15f, 4f, 8f, Art.a(0xFF5A6472, 0.7f));
        }
        // 墙脚阴影落到地面
        r.fillRect(x, y + 30, T, 3f, Art.a(Palette.BLACK, 0.55f));
        r.fillVerticalGradient(x, y + 30, T, 11f, Art.a(Palette.BLACK, 0.34f), Art.a(Palette.BLACK, 0f));
    }

    // ==================== 窗户墙 ====================

    static void drawWindowWall(Renderer r, TileMap map, int tx, int ty, int tint) {
        float x = tx * T, y = ty * T;
        boolean southOpen = !solidLike(map.type(tx, ty + 1));
        int topH = T - (southOpen ? 26 : 0);
        int cTop = Palette.mix(Art.lighten(Art.WALL_TOP, 0.10f), tint, 0.07f);

        r.fillRect(x, y, T, topH, cTop);
        r.fillRect(x, y, T, 3f, Art.lighten(cTop, 0.18f));
        r.fillRect(x, y, 3f, topH, Art.lighten(cTop, 0.08f));
        r.fillRect(x + T - 3f, y, 3f, topH, Art.darken(cTop, 0.22f));

        // 窗框凹槽
        r.fillRect(x + 6, y + 5, 60, 40, 0xFF20262E);
        r.fillRect(x + 9, y + 8, 54, 34, Art.GLASS);
        // 夜空玻璃渐变 + 反光
        r.fillVerticalGradient(x + 9, y + 8, 54, 34, 0xFF344C66, 0xFF1B2A3C);
        r.fillTriangle(x + 9, y + 42, x + 30, y + 8, x + 44, y + 8, Art.a(0xFFBFD8F0, 0.16f));
        r.line(x + 12, y + 38, x + 34, y + 12, 2.4f, Art.a(0xFFBFD8F0, 0.30f));
        // 窗棂
        r.fillRect(x + 34, y + 8, 4, 34, 0xFF20262E);
        r.fillRect(x + 9, y + 23, 54, 4, 0xFF20262E);
        // 窗台
        r.fillRect(x + 4, y + 43, 64, 4, Art.TRIM);
        r.fillRect(x + 4, y + 45, 64, 2, Art.a(Palette.BLACK, 0.4f));

        if (southOpen) {
            // 窗光洒在地面
            Art.ellipse(r, x + T * 0.5f, y + topH + 4, T * 0.62f, 12f, Art.a(0xFF9CC6E8, 0.12f));
            drawWallFace(r, x, y + topH, tint);
        }
    }

    // ==================== 门 ====================

    static void drawDoor(Renderer r, int tx, int ty, boolean locked, boolean open) {
        float x = tx * T, y = ty * T;
        // 门框（在地面上占满一格，门体立起来）
        r.fillRect(x, y, T, T, 0xFF1E242C);
        r.fillRect(x + 3, y + 3, T - 6, T - 6, 0xFF303842);
        // 门槽
        r.fillRect(x + 7, y + 9, T - 14, T - 16, 0xFF151A20);

        if (open) {
            // 敞开的门：内部幽深 + 门扇贴在左侧
            r.fillRect(x + 8, y + 10, T - 16, T - 18, 0xFF0A0D11);
            r.fillVerticalGradient(x + 8, y + 10, T - 16, T - 18, 0xFF16202A, 0xFF07090C);
            r.fillRect(x + 8, y + 10, 10, T - 18, 0xFF4A5260);
            r.fillRect(x + 8, y + 10, 2, T - 18, 0xFF6B7686);
            // 门洞暖光
            Art.ellipse(r, x + T * 0.5f, y + T - 4, T * 0.55f, 14f, Art.a(Art.HAZARD, 0.12f));
            // 门槛
            r.fillRect(x + 6, y + T - 9, T - 12, 4, 0xFF5A6472);
        } else {
            // 双开金属门
            float dw = (T - 16) * 0.5f;
            for (int side = 0; side < 2; side++) {
                float dx = x + 8 + side * dw;
                r.fillRect(dx, y + 10, dw, T - 18, 0xFF4A5260);
                r.fillRect(dx + 1, y + 11, dw - 2, T - 20, 0xFF566170);
                // 竖向拉丝
                for (int i = 0; i < 3; i++) {
                    r.fillRect(dx + 5 + i * 6, y + 14, 1.4f, T - 26, Art.a(0xFF2A323C, 0.55f));
                }
                // 观察窗
                r.fillRoundRect(dx + 4, y + 16, dw - 8, 16, 3, 0xFF151A20);
                r.fillRoundRect(dx + 5, y + 17, dw - 10, 14, 2.5f, 0xFF2A3C52);
                r.fillVerticalGradient(dx + 5, y + 17, dw - 10, 14, 0xFF3E5670, 0xFF1A2634);
                r.line(dx + 6, y + 29, dx + 15, y + 19, 1.8f, Art.a(0xFFBFD8F0, 0.45f));
                // 把手
                float hx = side == 0 ? dx + dw - 8 : dx + 4;
                r.fillRect(hx, y + 42, 3.2f, 12, 0xFFB8C4D0);
            }
            // 中缝
            r.fillRect(x + T * 0.5f - 1, y + 10, 2, T - 18, 0xFF252C34);
            if (locked) {
                // 红色钥匙孔 + 电子锁指示灯
                Art.glow(r, x + T - 20, y + 46, 16f, Palette.RED, 1.2f);
                r.fillRoundRect(x + T - 32, y + 38, 24, 18, 4, 0xFF3A1416);
                r.strokeRoundRect(x + T - 32, y + 38, 24, 18, 4, 1.5f, 0xFF8E2A2A);
                Art.discOutlined(r, x + T - 20, y + 46, 4.5f, Palette.RED, 0xFF6E1414, 1.5f);
                r.fillRect(x + T - 21.5f, y + 49, 3, 5, 0xFF6E1414);
                // 斜拉封条
                r.line(x + 10, y + 12, x + T - 10, y + T - 10, 3.5f, Art.a(Palette.RED, 0.22f));
            } else {
                Art.discOutlined(r, x + T - 20, y + 46, 4.5f, Palette.MINT, 0xFF1B3A2A, 1.5f);
            }
            r.fillRect(x + 6, y + T - 9, T - 12, 4, 0xFF5A6472);
        }
    }

    // ==================== 电梯 ====================

    static void drawElevator(Renderer r, int tx, int ty, boolean unlocked) {
        float x = tx * T, y = ty * T;
        r.fillRect(x, y, T, T, 0xFF1A2028);
        r.fillRect(x + 3, y + 3, T - 6, T - 6, 0xFF4A5462);
        // 不锈钢门
        r.fillRect(x + 7, y + 14, T - 14, T - 20, 0xFF6A7888);
        for (int i = 0; i < 2; i++) {
            float dx = x + 7 + i * (T - 14) * 0.5f;
            r.fillRect(dx, y + 14, (T - 14) * 0.5f, T - 20, 0xFF7A8898);
            r.fillVerticalGradient(dx, y + 14, (T - 14) * 0.5f, T - 20, 0xFF98A8B8, 0xFF5E6A78);
            for (int s = 0; s < 3; s++) {
                r.fillRect(dx + 6 + s * 8, y + 18, 1.4f, T - 28, Art.a(0xFF2A323C, 0.5f));
            }
        }
        r.fillRect(x + T * 0.5f - 1, y + 14, 2, T - 20, 0xFF20262E);
        // 顶部指示灯板
        r.fillRoundRect(x + 14, y + 4, T - 28, 12, 4, 0xFF141A21);
        int lamp = unlocked ? Palette.MINT : Palette.ALERT;
        r.setAlpha(unlocked ? 1f : 0.55f + 0.45f * (float) Math.abs(Math.sin(System.nanoTime() * 1e-9 * 3f)));
        Art.discOutlined(r, x + T * 0.5f, y + 10, 2.6f, lamp, 0xFF0B0E12, 1f);
        r.setAlpha(1f);
        // 上行箭头
        r.fillTriangle(x + T * 0.5f, y + 7, x + T * 0.5f - 6, y + 15, x + T * 0.5f + 6, y + 15,
                unlocked ? Palette.MINT : Art.a(Palette.TEXT_DIM, 0.6f));
        // 梯门下方光带
        r.fillRect(x + 10, y + T - 7, T - 20, 3, Art.a(unlocked ? Palette.MINT : Palette.RED_DARK, 0.5f));
        Art.ellipse(r, x + T * 0.5f, y + T + 2, T * 0.55f, 10f,
                Art.a(unlocked ? Palette.MINT : Palette.RED, 0.10f));
    }

    // ==================== 安保电脑 ====================

    static void drawComputer(Renderer r, int tx, int ty, boolean disabled, float time) {
        float x = tx * T, y = ty * T;
        // 桌子阴影
        Art.ellipse(r, x + T * 0.5f, y + T - 10, T * 0.44f, 9f, Art.a(Palette.BLACK, 0.35f));
        // 桌子
        r.fillRect(x + 6, y + 42, T - 12, 20, 0xFF5A4632);
        r.fillRect(x + 6, y + 42, T - 12, 4, 0xFF7A6248);
        r.fillRect(x + 8, y + 62, T - 16, 5, 0xFF3A2C1E);
        // CRT 显示器
        r.fillRoundRect(x + 17, y + 6, 38, 32, 4, 0xFF23282F);
        r.fillRoundRect(x + 20, y + 9, 32, 25, 3, disabled ? 0xFF1A2A22 : 0xFF0C1A2A);
        // 屏幕内容
        if (disabled) {
            r.fillRect(x + 22, y + 11, 28, 21, Art.a(0xFF2E5A42, 0.55f));
            r.line(x + 27, y + 24, x + 33, y + 29, 2f, Art.a(Palette.MINT, 0.8f));
            r.line(x + 33, y + 29, x + 41, y + 18, 2f, Art.a(Palette.MINT, 0.8f));
        } else {
            r.fillVerticalGradient(x + 22, y + 11, 28, 21, 0xFF1E4A72, 0xFF0C2236);
            for (int i = 0; i < 4; i++) {
                float lw = 8f + Art.hash(tx + i, ty + 9) * 16f;
                r.fillRect(x + 24, y + 14 + i * 4.4f, lw, 2f, Art.a(Palette.CYAN, 0.65f));
            }
            r.fillRect(x + 24, y + 12, 6, 2, Art.a(Palette.WHITE, 0.6f));
        }
        // 屏幕蓝光
        Art.glow(r, x + T * 0.5f, y + 22, disabled ? 26f : 34f,
                disabled ? Palette.MINT : Palette.CYAN, 0.85f);
        // 键盘
        r.fillRoundRect(x + 16, y + 62, 38, 9, 2, 0xFF2A3038);
        for (int i = 0; i < 5; i++) r.fillRect(x + 19 + i * 6.4f, y + 64, 5, 5, 0xFF3E4652);
        // 主机
        r.fillRect(x + 54, y + 48, 13, 17, 0xFF3A4048);
        r.fillRect(x + 56, y + 50, 3, 3, disabled ? Palette.MINT : Palette.CYAN);
        // 桌面纸张
        r.fillRect(x + 10, y + 50, 9, 7, Art.a(Palette.WHITE, 0.75f));
    }

    // ==================== 储物柜 ====================

    static void drawLocker(Renderer r, int tx, int ty) {
        float x = tx * T, y = ty * T;
        Art.shadow(r, x + T * 0.5f, y + T - 6, T * 0.42f, 8f, 90);
        // 柜体
        r.fillRoundRect(x + 8, y + 6, T - 16, T - 12, 4, 0xFF3A434F);
        r.fillRect(x + 10, y + 8, T - 20, T - 16, 0xFF4C5866);
        // 左右柜门
        float dw = (T - 22) * 0.5f;
        for (int s = 0; s < 2; s++) {
            float dx = x + 11 + s * dw;
            r.fillRect(dx, y + 9, dw - 1, T - 18, 0xFF586473);
            r.fillRect(dx + 1, y + 10, 2, T - 20, Art.a(Palette.WHITE, 0.10f));
            // 通风百叶
            for (int i = 0; i < 3; i++) {
                r.fillRect(dx + 5, y + 15 + i * 4, dw - 12, 2, Art.a(0xFF2A323C, 0.8f));
            }
            // 把手
            float hx = s == 0 ? dx + dw - 6 : dx + 4;
            r.fillRect(hx, y + 34, 3, 14, 0xFFB8C4D0);
            r.fillRect(hx, y + 34, 3, 3, Art.a(Palette.WHITE, 0.6f));
        }
        r.fillRect(x + T * 0.5f - 1, y + 9, 2, T - 18, 0xFF2A323C);
        // 顶部受光
        r.fillRect(x + 10, y + 8, T - 20, 3, Art.a(Palette.WHITE, 0.14f));
        r.fillRect(x + 8, y + 6, T - 16, 2, 0xFF8A96A6);
        // 锁孔
        Art.discOutlined(r, x + T - 17, y + T * 0.5f, 3f, 0xFF1A1E24, 0xFF9AA6B4, 1f);
    }

    // ==================== 箱子 ====================

    static void drawCrate(Renderer r, int tx, int ty) {
        float x = tx * T, y = ty * T;
        Art.shadow(r, x + T * 0.5f, y + T - 6, T * 0.42f, 9f, 100);
        r.fillRect(x + 8, y + 8, T - 16, T - 18, 0xFF5A4528);
        r.fillRect(x + 10, y + 10, T - 20, T - 22, 0xFF725536);
        // 木纹
        for (int i = 0; i < 3; i++) {
            r.fillRect(x + 10, y + 18 + i * 15, T - 20, 2.4f, Art.a(0xFF3A2C1A, 0.75f));
        }
        // 对角撑板
        r.line(x + 12, y + 44, x + T - 14, y + 14, 5, 0xFF8A6A44);
        r.line(x + 12, y + 44, x + T - 14, y + 14, 1.6f, Art.a(0xFF3A2C1A, 0.8f));
        // 金属包角
        for (int cx = 0; cx < 2; cx++) {
            for (int cy = 0; cy < 2; cy++) {
                r.fillRect(x + 8 + cx * (T - 22), y + 8 + cy * (T - 32), 14, 5, 0xFF4A4A48);
                r.fillRect(x + 8 + cx * (T - 22), y + 8 + cy * (T - 32), 5, 14, 0xFF4A4A48);
            }
        }
        // 顶部高光
        r.fillRect(x + 10, y + 10, T - 20, 3, Art.a(Palette.WHITE, 0.10f));
        // 标签
        r.fillRect(x + 26, y + 34, 20, 14, Art.a(0xFFE8E2D0, 0.85f));
        r.fillRect(x + 28, y + 37, 16, 2, Art.a(0xFF8A2A2A, 0.8f));
        r.fillRect(x + 28, y + 41, 12, 1.6f, Art.a(0xFF4A4A4A, 0.6f));
    }

    // ==================== 病床 ====================

    static void drawBed(Renderer r, int tx, int ty) {
        float x = tx * T, y = ty * T;
        Art.shadow(r, x + T * 0.5f, y + T * 0.62f, T * 0.46f, 14f, 110);
        // 床架
        r.fillRoundRect(x + 4, y + 4, T - 8, T - 8, 6, 0xFF6B7888);
        r.fillRoundRect(x + 6, y + 6, T - 12, T - 12, 5, 0xFF7E8C9E);
        // 床垫
        r.fillRoundRect(x + 8, y + 8, T - 16, T - 16, 5, 0xFFDCE6F0);
        // 枕头
        r.fillRoundRect(x + 12, y + 11, T - 24, 15, 5, 0xFFF4F8FC);
        r.fillRect(x + 14, y + 20, T - 28, 2, Art.a(0xFFB8C4D0, 0.6f));
        // 被子
        r.fillRoundRect(x + 9, y + 30, T - 18, T - 40, 4, 0xFF7C9AB8);
        r.fillRoundRect(x + 9, y + 30, T - 18, 4, 2, 0xFF9CB8D2);
        r.line(x + 14, y + 34, x + 22, y + 60, 2, Art.a(0xFF5A7896, 0.7f));
        r.line(x + 26, y + 34, x + 34, y + 60, 2, Art.a(0xFF5A7896, 0.5f));
        // 床头/床尾金属杆
        r.fillRect(x + 6, y + 6, T - 12, 2.4f, 0xFFB0BCC8);
        r.fillRect(x + 6, y + T - 9, T - 12, 2.4f, 0xFFB0BCC8);
        // 侧栏
        r.fillRect(x + 4, y + 18, 3, T - 36, 0xFFA8B4C0);
        r.fillRect(x + T - 7, y + 18, 3, T - 36, 0xFFA8B4C0);
        // 床单血点（确定性）
        if (Art.hash(tx + 5, ty + 11) > 0.72f) {
            Art.ellipse(r, x + 30, y + 46, 6f, 3.4f, Art.a(Palette.BLOOD, 0.55f));
            Art.ellipse(r, x + 40, y + 50, 3f, 2f, Art.a(Palette.BLOOD, 0.4f));
        }
    }
}
