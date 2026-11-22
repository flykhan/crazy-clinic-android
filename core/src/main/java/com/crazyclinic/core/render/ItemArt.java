package com.crazyclinic.core.render;

import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.world.ItemType;
import com.crazyclinic.core.world.Organ;
import com.crazyclinic.core.world.WeaponType;

/**
 * 武器 / 药品 / 钥匙卡 / 器官图标（世界拾取物与 HUD 共用）。
 * 图标一律绘制在以 (cx,cy) 为中心的局部坐标；手持武器绘制在"手部原点 +x 朝前"的坐标系。
 */
final class ItemArt {
    private ItemArt() {}

    static final int METAL = 0xFFB8C4D0;
    static final int METAL_DARK = 0xFF5A6472;
    static final int STEEL = 0xFF8E9AA8;
    static final int WOOD = 0xFF8A6238;
    static final int WOOD_DARK = 0xFF5A3C1E;
    static final int RED_CROSS = 0xFFD8323C;
    static final int ORGAN_PURPLE = 0xFFB05AD8;
    static final int ORGAN_PINK = 0xFFE87CA8;

    // ==================== 武器 ====================

    /** 手持武器：原点=握把，+x=枪口/刃尖方向，s=缩放。 */
    static void drawWeaponHeld(Renderer r, WeaponType w, float s, boolean firing) {
        r.save();
        r.scale(s, s);
        weaponShape(r, w, firing);
        r.restore();
    }

    /** 世界/HUD 图标：以 (cx,cy) 为中心，size 为外接尺寸。 */
    static void drawWeaponIcon(Renderer r, float cx, float cy, float size, WeaponType w) {
        float s = size / 76f;
        r.save();
        r.translate(cx, cy);
        r.rotate(-32f);
        r.scale(s, s);
        r.translate(-14f, 0f);
        weaponShape(r, w, false);
        r.restore();
    }

    private static void weaponShape(Renderer r, WeaponType w, boolean firing) {
        switch (w) {
            case FIST: {
                r.fillCircle(10, 0, 11, Art.OUTLINE);
                r.fillCircle(9, -1, 9, Art.SKIN);
                r.fillRect(-6, -8, 12, 16, 0xFFDED6C3);
                r.fillRect(-6, -8, 12, 3, Art.a(Palette.WHITE, 0.25f));
                break;
            }
            case AXE: {
                r.fillRect(-18, -3.5f, 48, 7, WOOD_DARK);
                r.fillRect(-16, -2.5f, 44, 3, 0xFFA87A48);
                // 斧刃
                r.fillTriangle(22, -4, 44, -18, 44, 10, Art.OUTLINE);
                r.fillTriangle(24, -3, 41, -15, 41, 8, STEEL);
                r.fillTriangle(42, -14, 46, -2, 42, 10, 0xFFD8E0E8);
                r.fillRect(-4, -5, 4, 10, 0xFF5A6472);
                break;
            }
            case SCALPEL: {
                r.fillRect(-14, -3, 26, 6, METAL_DARK);
                r.fillTriangle(12, -2.4f, 42, -0.6f, 12, 2.4f, 0xFFE8EEF4);
                r.fillRect(-14, -3, 10, 6, 0xFF3A4048);
                break;
            }
            case PISTOL: {
                // 描边轮廓 + 滑套/套筒 + 扳机护圈
                r.fillRect(-8, -6, 34, 13, Art.OUTLINE);
                r.fillRect(16, -5, 20, 9, Art.OUTLINE);
                r.fillRect(-6, -4, 30, 9, 0xFF4A5460);
                r.fillRect(16, -3, 16, 5, 0xFF68747F);
                r.fillRect(20, -6, 5, 4, METAL);
                r.fillRect(-6, -4, 30, 2.4f, Art.a(Palette.WHITE, 0.14f));
                r.fillRect(2, -1, 22, 1.6f, Art.a(0xFF2A3038, 0.7f));
                r.fillRect(-4, 3, 9, 15, 0xFF3E4652);
                r.fillRect(-4, 3, 9, 3, 0xFF5A6472);
                r.strokeCircle(-2, 6, 4.4f, 1.8f, Art.OUTLINE);
                if (firing) muzzle(r, 33, 0);
                break;
            }
            case SHOTGUN: {
                r.fillRect(-22, -5, 38, 12, Art.OUTLINE);
                r.fillRect(8, -6, 34, 11, Art.OUTLINE);
                r.fillRect(-20, -3, 34, 8, WOOD_DARK);
                r.fillRect(-18, -2, 26, 4, 0xFFA87A48);
                r.fillRect(10, -4, 30, 7, 0xFF525C6A);
                r.fillRect(12, -6, 22, 3, 0xFF78848F);
                r.fillRect(14, 0, 24, 1.6f, Art.a(0xFF2A3038, 0.65f));
                r.fillRect(28, -2, 14, 3.4f, 0xFF343A42);
                if (firing) muzzle(r, 43, 0);
                break;
            }
            case TASER: {
                r.fillRect(-6, -5, 24, 11, 0xFF3E4A56);
                r.fillRect(-4, -4, 18, 8, Art.HAZARD);
                r.fillRect(16, -4, 12, 3, METAL);
                r.fillRect(16, 2, 12, 3, METAL);
                r.fillCircle(20, 0, 3, Palette.CYAN);
                if (firing) {
                    r.line(28, -2, 36, -6, 2, Palette.CYAN);
                    r.line(28, 2, 38, 5, 2, Palette.CYAN);
                }
                break;
            }
            case DEFIBRILLATOR: {
                r.fillRect(-8, -8, 20, 16, 0xFFB8342C);
                r.fillRect(-6, -6, 16, 5, 0xFFE8E8E8);
                r.fillRect(-6, 0, 16, 2, Art.a(Palette.BLACK, 0.3f));
                // 电缆 + 电极板
                r.line(10, 0, 24, -8, 2.4f, 0xFF2A2E34);
                r.line(24, -8, 30, -4, 2.4f, 0xFF2A2E34);
                r.fillRoundRect(28, -9, 12, 10, 3, METAL_DARK);
                r.fillRoundRect(29, -8, 10, 8, 2, METAL);
                if (firing) {
                    r.line(30, -6, 38, -12, 1.6f, Palette.CYAN);
                    r.line(32, -2, 40, 4, 1.6f, Palette.CYAN);
                }
                break;
            }
            case BUNSEN: {
                r.fillRect(-14, -5, 28, 10, 0xFF7A8A98);
                r.fillRect(-14, -5, 28, 3, 0xFFA8B8C4);
                r.fillRect(-18, -7, 8, 14, 0xFF4A545E);
                r.fillRect(12, -3, 8, 6, 0xFFA8B8C4);
                if (firing) {
                    Art.ellipseRot(r, 29, 0, 13, 7, 0, Art.a(Palette.ORANGE, 0.85f));
                    Art.ellipseRot(r, 26, 0, 9, 4.5f, 0, Palette.GOLD);
                    Art.ellipseRot(r, 23, 0, 5, 2.6f, 0, Palette.LIGHT);
                }
                break;
            }
            case VIAL: {
                r.fillRect(-2, -6, 10, 12, Art.a(0xFFD8E8E8, 0.75f));
                r.fillCircle(16, 0, 11, Art.a(0xFF8E4AB8, 0.85f));
                r.fillCircle(16, 0, 11, Art.a(0xFFD8E8E8, 0.35f));
                r.fillCircle(19, -4, 4, Art.a(0xFFFFFFFF, 0.4f));
                r.fillRect(-4, -4, 6, 8, 0xFF8A6A48);
                if (firing) Art.glow(r, 18, 0, 16f, Palette.PURPLE, 1f);
                break;
            }
            default:
                r.fillRect(-8, -4, 24, 8, METAL);
                break;
        }
    }

    private static void muzzle(Renderer r, float x, float y) {
        r.fillTriangle(x - 2, y - 7, x + 16, y, x - 2, y + 7, Art.a(Palette.ORANGE, 0.95f));
        r.fillTriangle(x - 2, y - 4, x + 10, y, x - 2, y + 4, Palette.GOLD);
        r.fillTriangle(x, y - 2.4f, x + 7, y, x, y + 2.4f, Palette.LIGHT);
    }

    // ==================== 物品 ====================

    static void drawItemIcon(Renderer r, float cx, float cy, float size, ItemType t) {
        float s = size / 72f;
        r.save();
        r.translate(cx, cy);
        r.scale(s, s);
        switch (t) {
            case MEDKIT_SMALL:
            case MEDKIT:
            case MEDKIT_LARGE: {
                float w = t == ItemType.MEDKIT_SMALL ? 38 : (t == ItemType.MEDKIT ? 48 : 56);
                r.fillRoundRect(-w / 2 - 2, -w / 2 + 4, w + 4, w - 4, 8, Art.OUTLINE);
                r.fillRoundRect(-w / 2, -w / 2 + 6, w, w - 8, 7, 0xFFE8EEF2);
                r.fillRoundRect(-w / 2, -w / 2 + 6, w, 8, 6, 0xFFF8FBFF);
                int rc = t == ItemType.MEDKIT_LARGE ? 0xFFD8323C : RED_CROSS;
                r.fillRect(-4, -w / 2 + 16, 8, w - 26, rc);
                r.fillRect(-w / 2 + 10, -8, w - 20, 8, rc);
                // 提手
                r.fillRect(-9, -w / 2 + 1, 18, 4, 0xFF4A525E);
                r.fillRect(-9, -w / 2 + 1, 18, 2, METAL);
                break;
            }
            case PAINKILLER: {
                r.fillRoundRect(-16, -10, 32, 32, 5, Art.OUTLINE);
                r.fillRoundRect(-14, -8, 28, 28, 4, 0xFFE0E4E8);
                r.fillRect(-11, -18, 22, 12, 0xFFE8F0F8);
                r.fillRect(-11, -18, 22, 3, 0xFFFFFFFF);
                r.fillRect(-14, 2, 28, 10, 0xFFF08A3C);
                r.setTextSize(11);
                r.setTextBold(true);
                r.setTextAlign(1);
                r.setTextColor(0xFF3A2A20);
                r.drawText("药", 0, 7);
                break;
            }
            case STEROID:
            case EPO: {
                r.save();
                r.rotate(-38f);
                int fluid = t == ItemType.STEROID ? 0xFFE1C424 : 0xFFD8323C;
                r.fillRoundRect(-4, -4, 34, 8, 3, Art.OUTLINE);
                r.fillRoundRect(-2, -2, 30, 4, 2, 0xFFDDE6EE);
                r.fillRoundRect(4, -2, 20, 4, 2, fluid);
                r.fillRect(-8, -5, 6, 10, METAL_DARK);
                r.fillRect(20, -5, 4, 10, METAL_DARK);
                r.fillRect(32, -1, 14, 2, METAL);
                r.fillTriangle(46, 0, 40, -4, 40, 4, METAL);
                r.restore();
                break;
            }
            case ENERGY_DRINK: {
                r.fillRoundRect(-13, -22, 26, 44, 5, Art.OUTLINE);
                r.fillRoundRect(-11, -20, 22, 40, 4, 0xFF2A6AB8);
                r.fillRect(-11, -20, 22, 8, 0xFF4A9BE8);
                r.fillTriangle(-3, -14, 5, -4, -1, -4, Palette.GOLD);
                r.fillTriangle(-1, -4, 5, 6, -3, 6, Palette.GOLD);
                r.fillRect(-11, 12, 22, 3, Art.a(0xFFFFFFFF, 0.35f));
                break;
            }
            case INVISIBILITY: {
                r.fillRoundRect(-12, -16, 24, 30, 8, Art.a(0xFF9AD8E8, 0.55f));
                r.fillRoundRect(-10, -10, 20, 22, 7, Art.a(0xFF57D3E0, 0.75f));
                r.fillRect(-5, -24, 10, 9, 0xFF8A6A48);
                r.fillCircle(-4, -2, 3, Art.a(0xFFFFFFFF, 0.7f));
                r.fillCircle(4, 4, 2, Art.a(0xFFFFFFFF, 0.5f));
                r.line(-8, 8, -2, 2, 1.4f, Art.a(0xFFFFFFFF, 0.6f));
                break;
            }
            case KEYCARD: {
                r.fillRoundRect(-22, -14, 44, 28, 4, Art.OUTLINE);
                r.fillRoundRect(-20, -12, 40, 24, 3, 0xFF2E6AB0);
                r.fillVerticalGradient(-20, -12, 40, 24, 0xFF3E7ED0, 0xFF24508A);
                r.fillRect(-14, -6, 14, 10, Palette.GOLD);
                r.fillRect(-14, -6, 14, 2, Art.a(0xFFFFFFFF, 0.4f));
                r.fillRect(-14, 6, 24, 3, Art.a(0xFFFFFFFF, 0.25f));
                r.fillRect(-14, 11, 16, 2, Art.a(0xFFFFFFFF, 0.18f));
                Art.discOutlined(r, 15, 0, 3.4f, 0xFF16304E, 0xFF8AB4E0, 1f);
                break;
            }
            case AMMO: {
                r.fillRoundRect(-20, -14, 40, 28, 4, Art.OUTLINE);
                r.fillRoundRect(-18, -12, 36, 24, 3, 0xFF5A6A3A);
                r.fillRect(-16, -4, 32, 6, 0xFF3E4A2A);
                for (int i = 0; i < 4; i++) {
                    r.fillRect(-15 + i * 8, -11, 5, 12, Art.HAZARD);
                    r.fillTriangle(-15 + i * 8, -11, -12.5f + i * 8, -16, -10 + i * 8, -11, METAL);
                }
                break;
            }
            case ORGAN:
            default: {
                drawOrganIcon(r, 0, 0, 64, Organ.BRAIN);
                break;
            }
        }
        r.restore();
    }

    // ==================== 器官 ====================

    static void drawOrganIcon(Renderer r, float cx, float cy, float size, Organ o) {
        float s = size / 64f;
        r.save();
        r.translate(cx, cy);
        r.scale(s, s);
        int glow = Art.a(ORGAN_PURPLE, 0.32f);
        Art.glow(r, 0, 0, 34f, ORGAN_PURPLE, 1f);
        r.fillCircle(0, 0, 25, glow);
        int outline = 0xFF3A1C44;
        switch (o) {
            case BRAIN: {
                // 脑叶：多个凸起组成轮廓
                Art.discOutlined(r, -8, -9, 10, ORGAN_PINK, outline, 2f);
                Art.discOutlined(r, 8, -9, 10, ORGAN_PINK, outline, 2f);
                Art.discOutlined(r, -11, 2, 9, ORGAN_PINK, outline, 2f);
                Art.discOutlined(r, 11, 2, 9, ORGAN_PINK, outline, 2f);
                Art.discOutlined(r, -5, 12, 8, ORGAN_PINK, outline, 2f);
                Art.discOutlined(r, 6, 12, 8, ORGAN_PINK, outline, 2f);
                Art.discOutlined(r, 0, 1, 13, 0xFFE093BC, outline, 2f);
                // 脑沟
                r.line(-14, -4, -4, 0, 2.2f, Art.a(0xFF8A3A5A, 0.85f));
                r.line(-4, 0, -9, 8, 2.2f, Art.a(0xFF8A3A5A, 0.8f));
                r.line(14, -4, 4, 1, 2.2f, Art.a(0xFF8A3A5A, 0.85f));
                r.line(4, 1, 9, 9, 2.2f, Art.a(0xFF8A3A5A, 0.8f));
                r.line(-3, 13, 3, 13, 2f, Art.a(0xFF8A3A5A, 0.6f));
                break;
            }
            case HEART: {
                Art.discOutlined(r, -8, -6, 12, ORGAN_PINK, outline, 2.4f);
                Art.discOutlined(r, 8, -6, 12, ORGAN_PINK, outline, 2.4f);
                r.fillTriangle(-19, -2, 19, -2, 0, 22, ORGAN_PINK);
                r.fillTriangle(-17, 0, 17, 0, 0, 19, ORGAN_PINK);
                // 血管
                r.line(0, -16, 0, -22, 3, 0xFFB83A5A);
                r.line(-6, -20, -10, -14, 2.4f, 0xFFB83A5A);
                r.line(6, -20, 10, -14, 2.4f, 0xFFB83A5A);
                r.fillCircle(-6, -2, 4, Art.a(0xFFFFFFFF, 0.22f));
                break;
            }
            case KIDNEY: {
                Art.ellipseRot(r, 0, 0, 17, 22, 20, outline);
                Art.ellipseRot(r, 0, 0, 14, 19, 20, 0xFFC06A8A);
                r.fillCircle(7, 0, 6, Art.a(outline, 0.55f));
                r.line(-6, -12, -2, -4, 2, 0xFF8A3A5A);
                r.line(-2, 4, 4, 10, 2, 0xFF8A3A5A);
                break;
            }
            case LEGS: {
                // 两条腿 + 脚掌
                r.fillRoundRect(-16, -20, 13, 30, 6, outline);
                r.fillRoundRect(3, -20, 13, 30, 6, outline);
                r.fillRoundRect(-14, -18, 9, 26, 4, 0xFFD8A07A);
                r.fillRoundRect(5, -18, 9, 26, 4, 0xFFD8A07A);
                Art.ellipse(r, -8, 12, 9, 5.5f, outline);
                Art.ellipse(r, 8, 12, 9, 5.5f, outline);
                Art.ellipse(r, -8, 11, 7.5f, 4.2f, 0xFFE8C0A0);
                Art.ellipse(r, 8, 11, 7.5f, 4.2f, 0xFFE8C0A0);
                r.fillRect(-16, -14, 13, 5, 0xFF9AA6B4);
                r.fillRect(3, -14, 13, 5, 0xFF9AA6B4);
                break;
            }
            case LIVER: {
                // 双叶肝
                Art.ellipseRot(r, -8, -2, 14, 12, -18, outline);
                Art.ellipseRot(r, 8, 2, 12, 10, 12, outline);
                Art.ellipseRot(r, -8, -2, 12, 10, -18, 0xFF9A4A5A);
                Art.ellipseRot(r, 8, 2, 10, 8, 12, 0xFF8A3A4A);
                r.fillTriangle(-2, -10, 4, -8, 0, 10, Art.a(0xFF6A2A3A, 0.7f));
                r.line(-6, -2, 4, 2, 2, Art.a(outline, 0.6f));
                break;
            }
            case LUNGS: {
                Art.ellipseRot(r, -9, 2, 10, 20, -8, outline);
                Art.ellipseRot(r, 9, 2, 10, 20, 8, outline);
                Art.ellipseRot(r, -9, 2, 8, 17, -8, 0xFFE8A0B0);
                Art.ellipseRot(r, 9, 2, 8, 17, 8, 0xFFE8A0B0);
                r.fillRect(-3, -20, 6, 16, 0xFFB87A8A);
                r.line(0, -20, -12, -10, 3, 0xFFB87A8A);
                r.line(0, -20, 12, -10, 3, 0xFFB87A8A);
                break;
            }
            case ARMS: {
                // 二头肌 + 前臂 + 拳头
                r.fillRoundRect(-22, -12, 26, 18, 8, outline);
                r.fillRoundRect(-20, -10, 22, 14, 6, 0xFFD8A07A);
                r.fillRoundRect(-2, -4, 22, 12, 6, outline);
                r.fillRoundRect(0, -2, 18, 8, 4, 0xFFE0B08A);
                Art.discOutlined(r, 20, 2, 8, 0xFFE8B890, outline, 2f);
                r.fillCircle(-12, -4, 6, Art.a(0xFFF0C8A8, 0.9f));
                r.fillRect(-22, -10, 9, 5, Art.a(0xFF9AA6B4, 0.85f));
                break;
            }
            default:
                Art.discOutlined(r, 0, 0, 18, ORGAN_PINK, outline, 2.4f);
                break;
        }
        // 高光 + 外圈脉冲
        r.fillCircle(-6, -8, 4, Art.a(0xFFFFFFFF, 0.25f));
        r.strokeCircle(0, 0, 27, 1.6f, Art.a(ORGAN_PURPLE, 0.5f));
        r.restore();
    }
}
