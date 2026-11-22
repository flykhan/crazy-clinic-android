package com.crazyclinic.core.render;

import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.world.Decal;
import com.crazyclinic.core.world.Particle;
import com.crazyclinic.core.world.Pickup;
import com.crazyclinic.core.world.Projectile;
import com.crazyclinic.core.world.ProjectileType;
import com.crazyclinic.core.world.World;

/** 拾取物、子弹拖尾、粒子、地面贴花。 */
final class FxArt {
    private FxArt() {}

    // ==================== 拾取物 ====================

    static void drawPickup(Renderer r, World w, Pickup p) {
        float bob = (float) Math.sin(w.time * 2.6f + p.bob) * 4.5f;
        float x = p.x;
        float y = p.y + bob;

        int glowColor;
        float glowSize;
        switch (p.kind) {
            case WEAPON: glowColor = Palette.GOLD; glowSize = 30f; break;
            case ORGAN: glowColor = ItemArt.ORGAN_PURPLE; glowSize = 40f; break;
            case KEYCARD: glowColor = Palette.CYAN; glowSize = 30f; break;
            default: glowColor = Palette.GREEN; glowSize = 26f; break;
        }

        Art.shadow(r, p.x, p.y + 6f, 17f, 6.5f, 95);

        float pulse = 0.75f + 0.25f * (float) Math.sin(w.time * 3.4f + p.bob);
        if (p.kind == Pickup.Kind.ORGAN) {
            // 器官：紫色脉冲光环，最醒目
            Art.ellipse(r, x, y, glowSize * 1.5f, glowSize * 1.5f, Art.a(ItemArt.ORGAN_PURPLE, 0.14f * pulse));
            Art.ellipse(r, x, y, glowSize * 1.05f, glowSize * 1.05f, Art.a(ItemArt.ORGAN_PURPLE, 0.16f * pulse));
            Art.ellipse(r, x, y, 30f, 30f, Art.a(ItemArt.ORGAN_PURPLE, 0.26f * pulse));
            for (int i = 0; i < 4; i++) {
                float a = w.time * 2.2f + i * MathUtil.TAU / 4f;
                float sx = x + (float) Math.cos(a) * 30f;
                float sy = y + (float) Math.sin(a) * 20f;
                r.fillCircle(sx, sy, 2.4f, Art.a(0xFFFFFFFF, 0.5f * pulse));
            }
            ItemArt.drawOrganIcon(r, x, y - 2, 52, p.organ);
        } else {
            Art.ellipse(r, x, y, glowSize, glowSize, Art.a(glowColor, 0.10f * pulse));
            Art.ellipse(r, x, y, glowSize * 0.6f, glowSize * 0.6f, Art.a(glowColor, 0.12f * pulse));
            // 地面标记圈
            r.strokeCircle(p.x, p.y + 6f, 20f, 1.6f, Art.a(glowColor, 0.35f));
            switch (p.kind) {
                case WEAPON:
                    ItemArt.drawWeaponIcon(r, x, y - 2, 52, p.weapon);
                    // 金色星芒
                    r.fillTriangle(x - 24, y - 14, x - 14, y - 6, x - 16, y - 18, Art.a(Palette.GOLD, 0.6f));
                    break;
                case KEYCARD:
                    ItemArt.drawItemIcon(r, x, y - 2, 44, com.crazyclinic.core.world.ItemType.KEYCARD);
                    r.fillCircle(x + 18f, y - 16f, 2.4f, Art.a(Palette.WHITE, 0.7f * pulse));
                    break;
                default:
                    ItemArt.drawItemIcon(r, x, y - 2, 44, p.item);
                    break;
            }
        }
        // 浮动名称（小字，靠近可读）
        if (p.kind == Pickup.Kind.ORGAN) {
            r.setTextSize(16);
            r.setTextBold(true);
            r.setTextAlign(1);
            r.setTextColor(Art.a(0xFFE8C4FF, 0.9f));
            r.drawText("器官 · " + p.organ.label, p.x, p.y + 42f + bob);
            r.setTextBold(false);
        }
    }

    // ==================== 子弹 ====================

    static void drawProjectile(Renderer r, World w, Projectile p) {
        float sp = (float) Math.sqrt(p.vx * p.vx + p.vy * p.vy);
        float dirX = sp > 1f ? p.vx / sp : (float) Math.cos(p.ownerAngle);
        float dirY = sp > 1f ? p.vy / sp : (float) Math.sin(p.ownerAngle);
        switch (p.type) {
            case BULLET: {
                r.line(p.x - dirX * 16f, p.y - dirY * 16f, p.x, p.y, 3.4f, Art.a(Palette.GOLD, 0.45f));
                r.line(p.x - dirX * 8f, p.y - dirY * 8f, p.x, p.y, 2.2f, Art.a(0xFFFFE9B8, 0.9f));
                r.fillCircle(p.x, p.y, 3.4f, Palette.WHITE);
                break;
            }
            case PELLET: {
                r.fillCircle(p.x, p.y, 2.6f, Art.a(Palette.GOLD, 0.85f));
                r.line(p.x - dirX * 8f, p.y - dirY * 8f, p.x, p.y, 2f, Art.a(Palette.ORANGE, 0.4f));
                break;
            }
            case TASER_DART: {
                r.line(p.x - dirX * 20f, p.y - dirY * 20f, p.x, p.y, 2.4f, Art.a(Palette.CYAN, 0.5f));
                for (int i = 0; i < 2; i++) {
                    float bx = p.x - dirX * (10f + i * 8f);
                    float by = p.y - dirY * (10f + i * 8f);
                    r.line(bx, by, bx + dirY * 7f - dirX * 4f, by - dirX * 7f - dirY * 4f, 1.8f, Art.a(Palette.CYAN, 0.9f));
                }
                r.fillCircle(p.x, p.y, 3.2f, Palette.CYAN);
                r.fillCircle(p.x + dirX * 3f, p.y + dirY * 3f, 3.2f, Art.a(Palette.WHITE, 0.75f));
                break;
            }
            case VIAL: {
                r.save();
                r.translate(p.x, p.y);
                r.rotate(p.spin);
                r.fillCircle(0, 0, 8f, Art.OUTLINE);
                r.fillCircle(0, 0, 6.4f, Art.a(0xFF9BE06A, 0.9f));
                r.fillCircle(-2f, -2f, 2.6f, Art.a(0xFFFFFFFF, 0.5f));
                r.fillRect(-3f, -9f, 6f, 5f, 0xFF8A6A48);
                r.restore();
                r.line(p.x - dirX * 10f, p.y - dirY * 10f, p.x, p.y, 2f, Art.a(Palette.GREEN, 0.35f));
                break;
            }
            case WRENCH: {
                r.save();
                r.translate(p.x, p.y);
                r.rotate(p.spin * 57.3f);
                r.fillRect(-14f, -2.4f, 28f, 4.8f, Art.OUTLINE);
                r.fillRect(-13f, -1.6f, 26f, 3.2f, ItemArt.METAL_DARK);
                r.fillRoundRect(-20f, -7f, 9f, 14f, 4f, Art.OUTLINE);
                r.fillRoundRect(-19f, -6f, 7f, 12f, 3f, ItemArt.METAL);
                r.fillRoundRect(11f, -7f, 9f, 14f, 4f, Art.OUTLINE);
                r.fillRoundRect(12f, -6f, 7f, 12f, 3f, ItemArt.METAL);
                r.restore();
                break;
            }
            case SNEEZE: {
                Art.ellipse(r, p.x, p.y, 10f, 7f, Art.a(0xFF8FE06A, 0.55f));
                r.fillCircle(p.x - 4f, p.y - 2f, 4f, Art.a(0xFFB8F08A, 0.7f));
                r.fillCircle(p.x + 5f, p.y + 2f, 3f, Art.a(0xFF8FE06A, 0.6f));
                break;
            }
            case SCALPEL: {
                r.line(p.x - dirX * 14f, p.y - dirY * 14f, p.x, p.y, 2.6f, Art.a(0xFFE8EEF4, 0.9f));
                r.fillTriangle(p.x, p.y - 3f, p.x + dirX * 9f, p.y + dirY * 9f, p.x, p.y + 3f, ItemArt.METAL);
                break;
            }
            case THROWN_ORGAN: {
                Art.ellipse(r, p.x, p.y, 12f, 9f, Art.a(ItemArt.ORGAN_PURPLE, 0.85f));
                Art.ellipse(r, p.x - 3f, p.y - 3f, 5f, 4f, Art.a(ItemArt.ORGAN_PINK, 0.8f));
                Art.glow(r, p.x, p.y, 20f, ItemArt.ORGAN_PURPLE, 0.6f);
                break;
            }
            default:
                r.fillCircle(p.x, p.y, 3f, Palette.GOLD);
                break;
        }
    }

    // ==================== 粒子 ====================

    static void drawParticle(Renderer r, Particle p) {
        float t = p.life / p.maxLife;
        float a = MathUtil.clamp(t, 0f, 1f);
        switch (p.kind) {
            case 1: // 血滴
                Art.ellipse(r, p.x, p.y, p.size * (0.5f + a * 0.5f), p.size * (0.35f + a * 0.4f),
                        Art.a(p.color, 0.85f * a));
                if (a > 0.7f) {
                    r.fillCircle(p.x - p.vx * 0.005f, p.y - p.vy * 0.005f, p.size * 0.35f, Art.a(p.color, 0.5f));
                }
                break;
            case 2: // 火花
                r.fillCircle(p.x, p.y, p.size * a, Art.a(p.color, 0.9f * a));
                r.fillCircle(p.x, p.y, p.size * a * 0.5f, Art.a(Palette.WHITE, 0.7f * a));
                break;
            case 3: // 烟雾
                Art.ellipse(r, p.x, p.y, p.size * (1.6f - a * 0.6f), p.size * (1.4f - a * 0.5f),
                        Art.a(p.color, 0.22f * a));
                break;
            case 4: // 弹壳
                r.save();
                r.translate(p.x, p.y);
                r.rotate(p.rotation * 57.3f);
                r.fillRect(-3f, -1.6f, 6f, 3.2f, Art.a(p.color, 0.95f * a));
                r.fillRect(-3f, -1.6f, 2f, 3.2f, Art.a(0xFF7A5A20, 0.9f * a));
                r.restore();
                break;
            case 5: // 碎屑
                r.save();
                r.translate(p.x, p.y);
                r.rotate(p.rotation * 57.3f);
                r.fillTriangle(-p.size, p.size, p.size, p.size * 0.4f, -p.size * 0.2f, -p.size,
                        Art.a(p.color, 0.85f * a));
                r.restore();
                break;
            default: // 光点
                r.fillCircle(p.x, p.y, p.size * a, Art.a(p.color, 0.85f * a));
                break;
        }
    }

    // ==================== 贴花 ====================

    static void drawDecal(Renderer r, World w, Decal d) {
        float t = MathUtil.clamp(d.life / d.maxLife, 0f, 1f);
        float fade = t > 0.85f ? 1f : t / 0.85f;   // 最后 15% 生命淡出
        fade = MathUtil.clamp(fade, 0f, 1f);
        float s = d.scale;
        switch (d.kind) {
            case 1: { // 弹孔
                Art.ellipse(r, d.x, d.y, 8f * s, 7f * s, Art.a(Palette.BLACK, 0.55f * fade));
                r.strokeCircle(d.x, d.y, 9f * s, 1.6f, Art.a(0xFF5A5A5A, 0.35f * fade));
                Art.ellipse(r, d.x - 2f, d.y - 2f, 3f * s, 2.4f * s, Art.a(Palette.BLACK, 0.7f * fade));
                break;
            }
            case 2: { // 焦痕
                Art.ellipseRot(r, d.x, d.y, 26f * s, 19f * s, (float) Math.toDegrees(d.angle),
                        Art.a(Palette.BLACK, 0.30f * fade));
                Art.ellipseRot(r, d.x, d.y, 18f * s, 12f * s, (float) Math.toDegrees(d.angle),
                        Art.a(0xFF2A1A10, 0.35f * fade));
                Art.ellipseRot(r, d.x, d.y, 8f * s, 5f * s, (float) Math.toDegrees(d.angle),
                        Art.a(Palette.BLACK, 0.45f * fade));
                break;
            }
            default: { // 血泊
                int col = d.color == 0 ? Palette.BLOOD : d.color;
                Art.ellipseRot(r, d.x, d.y, 27f * s, 17f * s, (float) Math.toDegrees(d.angle),
                        Art.a(col, 0.42f * fade));
                Art.ellipseRot(r, d.x + 6f * s, d.y + 3f * s, 14f * s, 9f * s, (float) Math.toDegrees(d.angle + 0.5f),
                        Art.a(Palette.RED_DARK, 0.45f * fade));
                // 溅射小点（确定性）
                for (int i = 0; i < 3; i++) {
                    float hx = Art.hash3((int) d.x, (int) d.y, i);
                    float hy = Art.hash3((int) d.y, (int) d.x, i + 7);
                    float ang = hx * MathUtil.TAU;
                    float dist = 18f * s + hy * 16f * s;
                    Art.ellipse(r, d.x + (float) Math.cos(ang) * dist, d.y + (float) Math.sin(ang) * dist * 0.6f,
                            3.4f * s, 2.2f * s, Art.a(col, (0.28f + hy * 0.2f) * fade));
                }
                break;
            }
        }
    }
}
