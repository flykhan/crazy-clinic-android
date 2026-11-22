package com.crazyclinic.core.render;

import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.content.Balance;
import com.crazyclinic.core.world.Enemy;
import com.crazyclinic.core.world.EnemyState;
import com.crazyclinic.core.world.EnemyType;
import com.crazyclinic.core.world.Player;
import com.crazyclinic.core.world.WeaponType;
import com.crazyclinic.core.world.World;

/**
 * 角色程序化绘制：俯视 3/4 画法（脚底为锚点，身体直立、头朝面朝方向偏转）。
 * 统一 2~3px 深色描边 + 椭圆落地阴影，靠服装配色/配件区分 7 种敌人与 BOSS。
 */
final class CharacterArt {
    private CharacterArt() {}

    // ---- 敌人配色 ----
    private static final int GUARD_CLOTH = 0xFFB5342B, GUARD_DARK = 0xFF6E1A16;
    private static final int DOCTOR_CLOTH = 0xFF8E2F33, DOCTOR_DARK = 0xFF56191B;
    private static final int NURSE_CLOTH = 0xFFF0A8C0, NURSE_DARK = 0xFFC06A8A;
    private static final int FLU_CLOTH = 0xFF7C9A6A, FLU_DARK = 0xFF4E6644;
    private static final int TECH_CLOTH = 0xFF8B6BC4, TECH_DARK = 0xFF5A4190;
    private static final int SENTRY_METAL = 0xFF6E8390, SENTRY_DARK = 0xFF3A4854;
    private static final int BOSS_CLOTH = 0xFF7E4FA8, BOSS_DARK = 0xFF4A2A6A;
    private static final int BOSS_GOLD = 0xFFF2B33D;

    // ==================== 玩家 ====================

    static void drawPlayer(Renderer r, World w, Player p) {
        float t = w.time;
        boolean invis = p.invisTimer > 0f;
        float baseAlpha = invis ? 0.42f : 1f;
        float hurt = MathUtil.clamp(p.hurtFlash / 0.16f, 0f, 1f);

        // 闪避残影
        if (p.dodgeTimer > 0f) {
            for (int i = 3; i >= 1; i--) {
                float gx = p.x - (float) Math.cos(p.angle) * i * 14f;
                float gy = p.y - (float) Math.sin(p.angle) * i * 14f;
                r.setAlpha(0.16f * (4 - i) / 3f);
                playerFigure(r, gx, gy, p.angle, p.aimAngle, t, false, 0f, 0.6f, WeaponType.FIST, false);
            }
            r.setAlpha(baseAlpha);
        }

        if (p.dead) {
            Art.shadow(r, p.x, p.y + 4f, 30f, 11f, 115);
            drawFallenPlayer(r, p);
            r.setAlpha(1f);
            return;
        }
        Art.shadow(r, p.x, p.y + 4f, 28f, 10f, 115);
        playerFigure(r, p.x, p.y, p.angle, p.aimAngle, t, p.moving,
                hurt, baseAlpha, p.weapon, p.muzzleFlash > 0f || p.fireHold > 0f);

        // 攻击状态提示（近战挥砍弧光 / 枪械后坐）
        WeaponType wp = p.weapon;
        Balance.WeaponStats st = Balance.weapon(wp);
        if (st.melee && p.attackCd > st.cooldown * 0.45f) {
            float a0 = p.aimAngle - 0.75f;
            Art.fan(r, p.x, p.y - 44f, 74f, a0, 1.5f, Art.a(Palette.WHITE, 0.16f));
            Art.fan(r, p.x, p.y - 44f, 62f, a0 + 0.1f, 1.3f, Art.a(Palette.WHITE, 0.12f));
        }
        if (!st.melee && p.muzzleFlash > 0f) {
            float mi = MathUtil.clamp(p.muzzleFlash / 0.12f, 0f, 1f);
            float mx = p.x + (float) Math.cos(p.aimAngle) * 52f;
            float my = p.y - 44f + (float) Math.sin(p.aimAngle) * 34f;
            Art.glow(r, mx, my, 26f, Palette.GOLD, mi);
            // 枪口火星
            for (int i = 0; i < 4; i++) {
                float a = p.aimAngle + (i - 1.5f) * 0.22f;
                Art.ellipse(r, mx + (float) Math.cos(a) * 14f, my + (float) Math.sin(a) * 10f,
                        3f, 2f, Art.a(Palette.GOLD, 0.85f));
            }
        }

        // 加速药/EPO：身后拖影速度线
        if (p.speedBoostTimer > 0f) {
            float a = MathUtil.clamp(p.speedBoostTimer * 1.4f, 0f, 1f);
            for (int i = 0; i < 3; i++) {
                float off = 36f + i * 22f;
                float bx = p.x - (float) Math.cos(p.aimAngle) * off;
                float by = p.y - 40f - (float) Math.sin(p.aimAngle) * off;
                r.line(bx, by, bx - (float) Math.cos(p.aimAngle) * 22f, by - (float) Math.sin(p.aimAngle) * 22f,
                        2.4f - i * 0.4f, Art.a(0xFF9AD8F0, 0.22f * a));
            }
        }
        // 本生灯持续喷射：火焰扇
        if (p.fireHold > 0f) {
            float fi = MathUtil.clamp(p.fireHold / 0.14f, 0f, 1f);
            float ax = p.x + (float) Math.cos(p.aimAngle) * 30f;
            float ay = p.y - 46f + (float) Math.sin(p.aimAngle) * 30f;
            for (int i = 0; i < 3; i++) {
                float d = 14f + i * 14f;
                float fx = ax + (float) Math.cos(p.aimAngle) * d;
                float fy = ay + (float) Math.sin(p.aimAngle) * d;
                Art.ellipse(r, fx, fy, 13f - i * 2f, 9f - i * 1.5f,
                        Art.a(i == 0 ? Palette.ORANGE : (i == 1 ? Palette.GOLD : Palette.LIGHT), 0.35f * fi));
            }
        }
        r.setAlpha(1f);
    }

    private static void playerFigure(Renderer r, float x, float y, float angle, float aimAngle,
                                     float t, boolean moving, float hurt, float alpha,
                                     WeaponType weapon, boolean firing) {
        float f = MathUtil.clamp(hurt, 0f, 1f);
        int skin = Art.flash(Art.SKIN, f);
        int skinD = Art.flash(Art.SKIN_DARK, f);
        int hair = Art.flash(Art.HAIR, f);
        int hairD = Art.flash(Art.HAIR_DARK, f);
        int gown = Art.flash(Art.GOWN, f);
        int gownD = Art.flash(Art.GOWN_DARK, f);
        int shorts = Art.flash(Art.SHORTS, f);
        int shoes = Art.flash(Art.SLIPPER, f);
        int s = 1;

        float bob = moving ? (float) Math.abs(Math.sin(t * 11.5f)) * 1.8f : (float) Math.sin(t * 2.3f) * 1.1f;
        float fy = y - bob;
        float stride = moving ? (float) Math.sin(t * 12.5f) : 0f;

        // 拖鞋
        Art.ellipse(r, x - 11f + stride * 2.4f, fy - 2f + stride * 0.8f, 9f, 6f, Art.OUTLINE_SOFT);
        Art.ellipse(r, x + 11f - stride * 2.4f, fy - 2f - stride * 0.8f, 9f, 6f, Art.OUTLINE_SOFT);
        Art.ellipse(r, x - 11f + stride * 2.4f, fy - 3f + stride * 0.8f, 8f, 5f, shoes);
        Art.ellipse(r, x + 11f - stride * 2.4f, fy - 3f - stride * 0.8f, 8f, 5f, shoes);
        Art.ellipse(r, x - 11f + stride * 2.4f, fy - 5f + stride * 0.8f, 5f, 2.4f, Art.a(0xFF6A9AE8, 0.8f));

        // 腿（肤色）+ 绿短裤
        r.fillRoundRect(x - 16f, fy - 28f, 10f, 24f, 5f, Art.OUTLINE);
        r.fillRoundRect(x + 6f, fy - 28f, 10f, 24f, 5f, Art.OUTLINE);
        r.fillRoundRect(x - 14.5f, fy - 27f, 7f, 21f, 3.5f, skin);
        r.fillRoundRect(x + 7.5f, fy - 27f, 7f, 21f, 3.5f, skinD);
        r.fillRoundRect(x - 18f, fy - 34f, 36f, 16f, 6f, Art.OUTLINE);
        r.fillRoundRect(x - 16f, fy - 33f, 32f, 14f, 5f, shorts);
        r.fillRect(x - 1f, fy - 33f, 2f, 14f, Art.darken(shorts, 0.3f));

        // 病号服身体（宽肩）
        r.fillRoundRect(x - 26f, fy - 71f, 52f, 46f, 14f, Art.OUTLINE);
        r.fillRoundRect(x - 23.5f, fy - 68.5f, 47f, 41f, 12f, gown);
        // 肩膀
        Art.discOutlined(r, x - 19f, fy - 62f, 9f, gown, Art.OUTLINE, 2.4f);
        Art.discOutlined(r, x + 19f, fy - 62f, 9f, gown, Art.OUTLINE, 2.4f);
        // 病号服细节：开襟、V 领、暗部、口袋
        r.fillRect(x - 1.5f, fy - 64f, 3f, 28f, gownD);
        r.fillTriangle(x - 7f, fy - 66f, x + 7f, fy - 66f, x, fy - 57f, Art.a(skin, 0.9f));
        r.fillTriangle(x - 5f, fy - 66f, x + 5f, fy - 66f, x, fy - 58f, Art.a(skinD, 0.7f));
        r.fillRect(x - 16f, fy - 44f, 10f, 2f, gownD);
        r.fillRect(x - 16f, fy - 30f, 9f, 6f, Art.a(gownD, 0.7f));
        r.fillRect(x - 22f, fy - 28f, 44f, 2.4f, Art.a(gownD, 0.85f));
        r.fillRoundRect(x - 20f, fy - 66f, 14f, 5f, 3f, Art.a(Palette.WHITE, 0.18f));
        // 袖口
        r.fillRect(x - 22f, fy - 34f, 5f, 10f, Art.a(gownD, 0.8f));

        // 后手（不持武器）
        r.fillRoundRect(x + 15f, fy - 62f, 10f, 24f, 5f, Art.OUTLINE);
        r.fillRoundRect(x + 16.5f, fy - 61f, 7f, 21f, 3.5f, skinD);

        // 头
        float hx = x + (float) Math.cos(angle) * 2.5f;
        float hy = fy - 84f;
        Art.discOutlined(r, hx, hy, 21f, skin, Art.OUTLINE, 3f);
        // 脸朝向（朝上时只看到后脑勺）
        float ex = (float) Math.cos(angle) * 8f;
        float ey = (float) Math.sin(angle) * 6f;
        if (Math.sin(angle) > -0.55f) {
            Art.discOutlined(r, hx - 7f + ex * 0.35f, hy - 1f + ey * 0.4f, 2.9f, 0xFF2A2222, 0xFF1A1414, 0.6f);
            Art.discOutlined(r, hx + 7f + ex * 0.35f, hy - 1f + ey * 0.4f, 2.9f, 0xFF2A2222, 0xFF1A1414, 0.6f);
            // 眉毛（惊恐）
            r.line(hx - 11f + ex * 0.3f, hy - 8f + ey * 0.3f, hx - 4f + ex * 0.3f, hy - 9.5f + ey * 0.3f, 1.6f, Art.a(hairD, 0.8f));
            r.line(hx + 4f + ex * 0.3f, hy - 9.5f + ey * 0.3f, hx + 11f + ex * 0.3f, hy - 8f + ey * 0.3f, 1.6f, Art.a(hairD, 0.8f));
            // 嘴
            r.line(hx - 4f + ex * 0.3f, hy + 10f + ey * 0.4f, hx + 4f + ex * 0.3f, hy + 10f + ey * 0.4f, 2f, 0xFF7A3A38);
        } else {
            // 后脑：发块
            Art.ellipse(r, hx, hy + 2f, 18f, 15f, hairD);
        }
        // 金发：顶部发盖（不遮眼睛）+ 锯齿 + 两侧鬓角
        Art.ellipse(r, hx - 1f, hy - 17f, 20f, 9f, Art.OUTLINE);
        Art.ellipse(r, hx - 1f, hy - 18f, 18f, 7.5f, hair);
        r.fillTriangle(hx - 15f, hy - 19f, hx - 10f, hy - 30f, hx - 5f, hy - 20f, hair);
        r.fillTriangle(hx - 7f, hy - 21f, hx - 1f, hy - 33f, hx + 4f, hy - 21f, hair);
        r.fillTriangle(hx + 3f, hy - 20f, hx + 10f, hy - 30f, hx + 14f, hy - 18f, hair);
        r.fillTriangle(hx - 9f, hy - 26f, hx - 6f, hy - 29f, hx - 5f, hy - 24f, Art.HAIR_HI);
        // 鬓角
        r.fillTriangle(hx - 19f, hy - 14f, hx - 14f, hy - 16f, hx - 15f, hy - 4f, hairD);
        r.fillTriangle(hx + 15f, hy - 14f, hx + 20f, hy - 16f, hx + 15f, hy - 4f, hairD);

        // 前手 + 武器（朝瞄准方向）
        r.save();
        r.translate(x + (float) Math.cos(aimAngle) * 6f, fy - 48f);
        r.rotate((float) Math.toDegrees(aimAngle));
        r.fillRoundRect(-4f, -6.5f, 24f, 13f, 6f, Art.OUTLINE);
        r.fillRoundRect(-3f, -5f, 21f, 10f, 5f, skin);
        r.fillRoundRect(10f, -4f, 8f, 8f, 4f, skinD);
        ItemArt.drawWeaponHeld(r, weapon, 0.82f, firing);
        r.restore();
    }

    private static void drawFallenPlayer(Renderer r, Player p) {
        int gown = Art.desaturate(Art.GOWN, 0.35f);
        Art.ellipse(r, p.x, p.y + 6f, 34f, 12f, Art.a(Palette.BLOOD, 0.55f));
        Art.ellipseRot(r, p.x, p.y + 2f, 28f, 14f, 24f, gown);
        Art.discOutlined(r, p.x - 16f, p.y - 8f, 13f, Art.SKIN, Art.OUTLINE, 2.4f);
        Art.ellipse(r, p.x - 18f, p.y - 14f, 12f, 7f, Art.HAIR_DARK);
        r.line(p.x - 21f, p.y - 10f, p.x - 18f, p.y - 8f, 2f, 0xFF2A2222);
        r.line(p.x - 12f, p.y - 9f, p.x - 9f, p.y - 7f, 2f, 0xFF2A2222);
        r.line(p.x - 26f, p.y + 2f, p.x - 42f, p.y + 8f, 6f, Art.a(Art.SKIN_DARK, 0.9f));
        r.line(p.x + 6f, p.y + 6f, p.x + 24f, p.y + 14f, 6f, Art.a(Art.GOWN_DARK, 0.9f));
    }

    // ==================== 敌人 ====================

    static void drawEnemy(Renderer r, World w, Enemy e) {
        float t = w.time;
        if (e.isDead()) {
            drawCorpse(r, w, e);
            return;
        }
        if (e.type == EnemyType.SENTRY) {
            drawSentry(r, w, e);
            return;
        }
        boolean moving = e.state == EnemyState.CHASE || e.state == EnemyState.PATROL;
        float f = MathUtil.clamp(e.hurtFlash / 0.16f, 0f, 1f);
        float bob = moving ? (float) Math.abs(Math.sin(t * 9f + e.homeX * 0.01f)) * 1.6f : 0f;
        float fy = e.y - bob;
        float angle = e.angle;
        float scale = e.type == EnemyType.BOSS ? 1.42f : 1f;
        int skin = Art.flash(e.type == EnemyType.FLU_PATIENT ? 0xFFB8C8A0 : Art.SKIN, f);
        int skinD = Art.flash(e.type == EnemyType.FLU_PATIENT ? 0xFF93A87C : Art.SKIN_DARK, f);

        // 阴影
        Art.shadow(r, e.x, e.y + 4f * scale, 26f * scale, 9.5f * scale, 120);

        float stride = (moving && e.state == EnemyState.CHASE) ? (float) Math.sin(t * 11f) : 0f;

        int cloth, clothD, legColor, shoe, hair;
        switch (e.type) {
            case GUARD: cloth = GUARD_CLOTH; clothD = GUARD_DARK; legColor = 0xFF3A4450; shoe = 0xFF20242A; hair = 0xFF2A2420; break;
            case DOCTOR: cloth = DOCTOR_CLOTH; clothD = DOCTOR_DARK; legColor = 0xFF3A4450; shoe = 0xFF20242A; hair = 0xFF2A2420; break;
            case NURSE: cloth = NURSE_CLOTH; clothD = NURSE_DARK; legColor = Art.SKIN_DARK; shoe = 0xFFF0F4F8; hair = 0xFF6A4A2A; break;
            case FLU_PATIENT: cloth = FLU_CLOTH; clothD = FLU_DARK; legColor = 0xFFB8C8A0; shoe = 0xFF5A6A4A; hair = 0xFF6A7050; break;
            case TECH: cloth = TECH_CLOTH; clothD = TECH_DARK; legColor = 0xFF3A4450; shoe = 0xFF20242A; hair = 0xFF2A2420; break;
            case BOSS: cloth = BOSS_CLOTH; clothD = BOSS_DARK; legColor = 0xFF6A4A32; shoe = 0xFF3A2A1E; hair = 0xFF8A8A8A; break;
            default: cloth = GUARD_CLOTH; clothD = GUARD_DARK; legColor = 0xFF3A4450; shoe = 0xFF20242A; hair = 0xFF2A2420; break;
        }
        cloth = Art.flash(cloth, f);
        clothD = Art.flash(clothD, f);

        // 脚/腿
        Art.ellipse(r, e.x - 10f * scale + stride * 2f, fy - 3f * scale, 8.5f * scale, 5.5f * scale, Art.OUTLINE_SOFT);
        Art.ellipse(r, e.x + 10f * scale - stride * 2f, fy - 3f * scale, 8.5f * scale, 5.5f * scale, Art.OUTLINE_SOFT);
        Art.ellipse(r, e.x - 10f * scale + stride * 2f, fy - 4f * scale, 7.5f * scale, 4.5f * scale, Art.flash(shoe, f));
        Art.ellipse(r, e.x + 10f * scale - stride * 2f, fy - 4f * scale, 7.5f * scale, 4.5f * scale, Art.flash(shoe, f));
        r.fillRoundRect(e.x - 16f * scale, fy - 30f * scale, 9f * scale, 26f * scale, 4f * scale, Art.OUTLINE);
        r.fillRoundRect(e.x + 7f * scale, fy - 30f * scale, 9f * scale, 26f * scale, 4f * scale, Art.OUTLINE);
        r.fillRoundRect(e.x - 14.5f * scale, fy - 29f * scale, 6f * scale, 23f * scale, 3f * scale, Art.flash(legColor, f));
        r.fillRoundRect(e.x + 8.5f * scale, fy - 29f * scale, 6f * scale, 23f * scale, 3f * scale, Art.flash(legColor, f));

        // 身体
        float bw = (e.type == EnemyType.BOSS ? 29f : 24f) * scale;
        float bh = (e.type == EnemyType.BOSS ? 50f : 44f) * scale;
        float topY = fy - bh - 26f * scale;
        r.fillRoundRect(e.x - bw - 2.4f * scale, topY - 2.4f * scale,
                (bw + 2.4f * scale) * 2, bh + 4.8f * scale, 11f * scale, Art.OUTLINE);
        r.fillRoundRect(e.x - bw, topY, bw * 2, bh, 9f * scale, cloth);
        // 肩部（让体型不那么像一根柱子）
        Art.discOutlined(r, e.x - bw + 6f * scale, topY + 7f * scale, 8.5f * scale,
                cloth, Art.OUTLINE, 2.4f * scale);
        Art.discOutlined(r, e.x + bw - 6f * scale, topY + 7f * scale, 8.5f * scale,
                cloth, Art.OUTLINE, 2.4f * scale);
        // 下摆阴影
        r.fillRect(e.x - bw + 2f * scale, topY + bh - 3f * scale, bw * 2 - 4f * scale, 2.4f * scale,
                Art.a(clothD, 0.75f));
        // 衣服细节
        r.fillRect(e.x - 8f * scale, topY + 8f * scale, 16f * scale, 2f * scale, Art.a(clothD, 0.9f));
        r.fillRect(e.x - 1.2f * scale, topY + 6f * scale, 2.4f * scale, bh - 12f * scale, Art.a(clothD, 0.8f));
        r.fillRoundRect(e.x - bw + 3f * scale, topY + 3f * scale, bw * 0.8f, 4f * scale, 2f * scale,
                Art.a(Palette.WHITE, 0.12f));
        switch (e.type) {
            case GUARD:
                // 工装背带
                r.fillRect(e.x - 14f * scale, topY + 4f * scale, 4f * scale, bh - 8f * scale, Art.a(clothD, 0.9f));
                r.fillRect(e.x + 10f * scale, topY + 4f * scale, 4f * scale, bh - 8f * scale, Art.a(clothD, 0.9f));
                r.fillRect(e.x - 8f * scale, topY + 2f * scale, 16f * scale, 3f * scale, 0xFFD8D2C4);
                break;
            case DOCTOR:
                // 白大褂边缘 + 听诊器
                r.fillRect(e.x - bw + 4f * scale, topY + 4f * scale, 5f * scale, bh - 6f * scale, Art.a(0xFFE8E4DC, 0.85f));
                r.fillRect(e.x + bw - 9f * scale, topY + 4f * scale, 5f * scale, bh - 6f * scale, Art.a(0xFFE8E4DC, 0.85f));
                r.strokeCircle(e.x - 8f * scale, topY + 18f * scale, 5f * scale, 2.2f * scale, 0xFFE8E4DC);
                break;
            case NURSE:
                // 围裙 + 红十字符
                r.fillRoundRect(e.x - 12f * scale, topY + 16f * scale, 24f * scale, bh - 18f * scale, 5f * scale, 0xFFF8F0F2);
                r.fillRect(e.x - 2.5f * scale, topY + 24f * scale, 5f * scale, 12f * scale, 0xFFD8323C);
                r.fillRect(e.x - 6f * scale, topY + 28f * scale, 12f * scale, 4f * scale, 0xFFD8323C);
                break;
            case FLU_PATIENT:
                // 病号服条纹
                r.fillRect(e.x - bw + 2f * scale, topY + 14f * scale, bw * 2 - 4f * scale, 2f * scale, Art.a(clothD, 0.8f));
                r.fillRect(e.x - bw + 2f * scale, topY + 24f * scale, bw * 2 - 4f * scale, 2f * scale, Art.a(clothD, 0.6f));
                break;
            case TECH:
                // 实验室围裙 + 胸牌
                r.fillRoundRect(e.x - 12f * scale, topY + 14f * scale, 24f * scale, bh - 16f * scale, 4f * scale, Art.a(0xFFE8E4DC, 0.55f));
                r.fillRect(e.x + 6f * scale, topY + 10f * scale, 8f * scale, 5f * scale, 0xFFE1C424);
                break;
            case BOSS:
                // 砖块肩甲 + 金边
                r.fillRect(e.x - bw - 4f * scale, topY + 2f * scale, 14f * scale, 12f * scale, 0xFF8B5A3C);
                r.fillRect(e.x + bw - 10f * scale, topY + 2f * scale, 14f * scale, 12f * scale, 0xFF8B5A3C);
                r.fillRect(e.x - bw - 4f * scale, topY + 6f * scale, 14f * scale, 1.6f * scale, Art.a(0xFF5A3520, 0.9f));
                r.fillRect(e.x - bw - 4f * scale, topY + 10f * scale, 14f * scale, 1.6f * scale, Art.a(0xFF5A3520, 0.9f));
                r.fillRect(e.x + bw - 10f * scale, topY + 6f * scale, 14f * scale, 1.6f * scale, Art.a(0xFF5A3520, 0.9f));
                r.fillRect(e.x + bw - 10f * scale, topY + 10f * scale, 14f * scale, 1.6f * scale, Art.a(0xFF5A3520, 0.9f));
                r.fillRect(e.x - 18f * scale, topY + bh - 5f * scale, 36f * scale, 3f * scale, BOSS_GOLD);
                r.fillRect(e.x - 3f * scale, topY + 12f * scale, 6f * scale, bh - 20f * scale, Art.a(BOSS_GOLD, 0.8f));
                break;
            default:
                break;
        }

        // 后手
        r.fillRoundRect(e.x + 14f * scale, fy - 60f * scale, 9f * scale, 22f * scale, 4f * scale, Art.OUTLINE);
        r.fillRoundRect(e.x + 15.5f * scale, fy - 59f * scale, 6f * scale, 19f * scale, 3f * scale, skinD);

        // 头
        float headR = (e.type == EnemyType.BOSS ? 24f : 20f) * scale;
        float hx = e.x + (float) Math.cos(angle) * 2.5f * scale;
        float hy = fy - (e.type == EnemyType.BOSS ? 86f : 78f) * scale;
        Art.discOutlined(r, hx, hy, headR, skin, Art.OUTLINE, 3f);

        float ex = (float) Math.cos(angle) * 7f * scale;
        float ey = (float) Math.sin(angle) * 5.5f * scale;
        boolean face = Math.sin(angle) > -0.55f;
        if (face) {
            int eyeCol = e.type == EnemyType.BOSS ? 0xFFFF3A2A
                    : (e.type == EnemyType.FLU_PATIENT ? 0xFF4A5A3A : 0xFF2A2222);
            if (e.type == EnemyType.BOSS && e.hp / Math.max(1f, e.maxHp) < 0.33f) {
                eyeCol = 0xFFFFE137;   // 三阶段：金红发光眼
            }
            Art.discOutlined(r, hx - 6.5f * scale + ex * 0.35f, hy - 1f * scale + ey * 0.4f,
                    2.8f * scale, eyeCol, 0xFF1A1414, 0.6f);
            Art.discOutlined(r, hx + 6.5f * scale + ex * 0.35f, hy - 1f * scale + ey * 0.4f,
                    2.8f * scale, eyeCol, 0xFF1A1414, 0.6f);
        }
        // 嘴（根据类型）
        switch (e.type) {
            case GUARD: case DOCTOR:
                // 白口罩
                r.fillRoundRect(hx - 13f * scale, hy + 1f * scale, 26f * scale, 15f * scale, 5f * scale, 0xFFE8EEF2);
                r.fillRect(hx - 13f * scale, hy + 10f * scale, 26f * scale, 2f * scale, Art.a(0xFFB8C4D0, 0.8f));
                break;
            case NURSE:
                r.fillRect(hx - 5f * scale, hy + 9f * scale, 10f * scale, 2.4f * scale, 0xFFB83A5A);
                break;
            case FLU_PATIENT:
                Art.ellipse(r, hx + 2f * scale, hy + 9f * scale, 5f * scale, 4f * scale, Art.a(0xFF4E6644, 0.8f));
                break;
            case BOSS:
                r.fillRect(hx - 7f * scale, hy + 8f * scale, 14f * scale, 3f * scale, 0xFF3A2020);
                r.fillRect(hx - 8f * scale, hy - 9f * scale, 6f * scale, 2f * scale, 0xFF3A2020);
                r.fillRect(hx + 2f * scale, hy - 9f * scale, 6f * scale, 2f * scale, 0xFF3A2020);
                break;
            default:
                r.fillRect(hx - 4f * scale, hy + 9f * scale, 8f * scale, 2f * scale, 0xFF7A5A50);
                break;
        }
        // 头发/头饰
        drawHeadgear(r, w, e, hx, hy, headR, angle, f, hair);

        // 特征配件 & 武器
        drawEnemyWeapon(r, w, e, fy, scale, angle, f);

        // 感冒病人的鼻涕泡
        if (e.type == EnemyType.FLU_PATIENT) {
            float wob = (float) Math.sin(t * 3.2f + e.homeY * 0.02f);
            float br = (7f + wob * 2.2f) * scale;
            float bx = hx + (float) Math.cos(angle) * 14f * scale;
            float by = hy + 10f * scale;
            r.fillCircle(bx, by + br * 0.7f, br, Art.a(0xFF8FE06A, 0.62f));
            r.fillCircle(bx - br * 0.3f, by + br * 0.6f, br * 0.35f, Art.a(0xFFFFFFFF, 0.5f));
            r.strokeCircle(bx, by + br * 0.7f, br, 1.6f, Art.a(0xFF4E8A3A, 0.7f));
        }

        // 眩晕星星
        if (e.stunTimer > 0f) {
            for (int i = 0; i < 3; i++) {
                float a = t * 6f + i * MathUtil.TAU / 3f;
                float sx = hx + (float) Math.cos(a) * 24f * scale;
                float sy = hy - 26f * scale + (float) Math.sin(a) * 8f * scale;
                r.fillTriangle(sx, sy - 6f, sx - 5f, sy + 4f, sx + 5f, sy + 4f, Art.HAZARD);
            }
        }

        // 警戒/追击标记（ALERT 随 alertTimer 0.8s 淡出+弹跳）
        if (e.state == EnemyState.ALERT || e.state == EnemyState.CHASE) {
            boolean chase = e.state == EnemyState.CHASE;
            float aa = chase ? 1f : MathUtil.clamp(e.alertTimer / 0.8f, 0f, 1f);
            float pop = chase ? 1f : (1.15f - aa * 0.15f);
            float by = hy - headR - 26f * scale + (float) Math.sin(t * 8f) * 2f;
            Art.discOutlined(r, hx, by, 13f * scale * pop,
                    Art.a(chase ? Palette.RED : Art.HAZARD, 0.35f + 0.65f * aa), 0xFF2A1414, 2.4f);
            r.setTextSize(20f * scale * pop);
            r.setTextBold(true);
            r.setTextAlign(1);
            r.setTextColor(Art.a(chase ? Palette.WHITE : 0xFF2A1A0A, 0.4f + 0.6f * aa));
            r.drawText("!", hx, by);
            r.setTextBold(false);
        }

        // 护士复活读条（reviveTimer 0→3s 递增，头顶进度条 + 注射器微光）
        if (e.type == EnemyType.NURSE && e.reviveTimer > 0f) {
            float w0 = 46f * scale;
            float pgr = MathUtil.clamp(e.reviveTimer / 3f, 0f, 1f);
            float py0 = hy - headR - 20f * scale;
            r.fillRoundRect(e.x - w0 / 2, py0, w0, 7f * scale, 3f, Art.a(Palette.BLACK, 0.75f));
            r.fillRoundRect(e.x - w0 / 2 + 1, py0 + 1, (w0 - 2) * pgr, 5f * scale, 2f, Palette.PURPLE);
            float pl = 0.5f + 0.5f * (float) Math.sin(t * 10f);
            Art.glow(r, e.x, py0 - 2f, 22f, Palette.PURPLE, 0.4f + 0.3f * pl);
            r.setTextSize(13f * scale);
            r.setTextAlign(1);
            r.setTextColor(Art.a(0xFFE8C4FF, 0.85f));
            r.drawText("复活中", e.x, py0 - 12f * scale);
        }

        // 血条
        if (e.hp < e.maxHp && e.type != EnemyType.BOSS) {
            float bw2 = 42f * scale;
            float by2 = hy - headR - 14f * scale;
            r.fillRoundRect(e.x - bw2 / 2 - 1, by2 - 1, bw2 + 2, 7f, 3f, Art.a(Palette.BLACK, 0.7f));
            float hp = MathUtil.clamp(e.hp / e.maxHp, 0f, 1f);
            r.fillRoundRect(e.x - bw2 / 2, by2, bw2 * hp, 5f, 2f,
                    hp > 0.5f ? Palette.GREEN : (hp > 0.25f ? Art.HAZARD : Palette.RED));
        }
    }

    private static void drawHeadgear(Renderer r, World w, Enemy e, float hx, float hy, float headR,
                                     float angle, float f, int hair) {
        switch (e.type) {
            case GUARD:
                // 制服帽
                r.fillRoundRect(hx - headR + 2f, hy - headR - 2f, headR * 2 - 4f, headR * 0.72f, 6f, Art.OUTLINE);
                r.fillRoundRect(hx - headR + 3.5f, hy - headR - 0.5f, headR * 2 - 7f, headR * 0.6f, 5f, Art.flash(0xFF2A3440, f));
                r.fillRect(hx - headR + 3.5f, hy - headR + headR * 0.42f, headR * 2 - 7f, 3f, Art.flash(0xFF141A22, f));
                Art.discOutlined(r, hx, hy - headR + 2f, 4f, Art.HAZARD, 0xFF2A1A0A, 1.2f);
                break;
            case DOCTOR:
                // 手术帽 + 额镜
                r.fillRoundRect(hx - headR + 3f, hy - headR - 1f, headR * 2 - 6f, headR * 0.55f, 5f, Art.flash(0xFF3A6A8A, f));
                Art.discOutlined(r, hx, hy - headR + 4f, 4.5f, Art.flash(0xFFB8C4D0, f), Art.OUTLINE, 1.2f);
                break;
            case NURSE:
                // 护士帽 + 红十字
                r.fillRoundRect(hx - headR + 4f, hy - headR - 3f, headR * 2 - 8f, headR * 0.5f, 4f, 0xFFF8F8FA);
                r.fillRect(hx - 1.6f, hy - headR + 1f, 3.2f, 8f, 0xFFD8323C);
                r.fillRect(hx - 4.5f, hy - headR + 3.2f, 9f, 3.2f, 0xFFD8323C);
                // 长发（两侧披下，不遮脸）
                Art.ellipse(r, hx - headR * 0.82f, hy + headR * 0.30f, headR * 0.42f, headR * 0.62f,
                        Art.flash(0xFF8A6A3A, f));
                Art.ellipse(r, hx + headR * 0.82f, hy + headR * 0.30f, headR * 0.42f, headR * 0.62f,
                        Art.flash(0xFF8A6A3A, f));
                break;
            case FLU_PATIENT:
                // 稀疏乱发
                r.fillTriangle(hx - 12f, hy - headR + 3f, hx - 8f, hy - headR - 8f, hx - 3f, hy - headR + 2f, hair);
                r.fillTriangle(hx + 2f, hy - headR + 2f, hx + 8f, hy - headR - 7f, hx + 12f, hy - headR + 3f, hair);
                break;
            case TECH:
                // 护目镜
                r.fillRoundRect(hx - headR + 1f, hy - 9f, headR * 2 - 2f, 13f, 5f, Art.OUTLINE);
                Art.discOutlined(r, hx - 7f, hy - 3f, 7f, Art.flash(0xFF57D3E0, f), 0xFF2A3A40, 2f);
                Art.discOutlined(r, hx + 7f, hy - 3f, 7f, Art.flash(0xFF57D3E0, f), 0xFF2A3A40, 2f);
                r.fillRect(hx - 12f, hy - 12f, 24f, 3f, Art.a(Palette.BLACK, 0.6f));
                r.fillCircle(hx - 9f, hy - 5f, 2f, Art.a(Palette.WHITE, 0.7f));
                r.fillCircle(hx + 5f, hy - 5f, 2f, Art.a(Palette.WHITE, 0.7f));
                // 耳朵上的铅笔
                r.fillRect(hx + headR - 2f, hy - 6f, 3f, 14f, Art.HAZARD);
                break;
            case BOSS:
                // 光头 + 眉骨阴影 + 金属头盔边框
                Art.ellipse(r, hx, hy - headR + 4f, headR * 0.9f, headR * 0.4f, Art.flash(0xFF6A6A72, f));
                r.fillRect(hx - headR * 0.9f, hy - headR + 3f, headR * 1.8f, 3f, Art.a(0xFF3A3A42, 0.8f));
                break;
            default:
                break;
        }
    }

    private static void drawEnemyWeapon(Renderer r, World w, Enemy e, float fy, float scale, float angle, float f) {
        float t = w.time;
        r.save();
        r.translate(e.x + (float) Math.cos(angle) * 7f * scale, fy - 46f * scale);
        r.rotate((float) Math.toDegrees(angle));
        float s = scale * 0.9f;
        r.scale(s, s);
        switch (e.type) {
            case GUARD: {
                // 扳钳
                r.fillRoundRect(-4f, -6f, 24f, 12f, 5f, Art.OUTLINE);
                r.fillRoundRect(-2f, -4.5f, 21f, 9f, 4f, Art.SKIN);
                r.fillRect(14f, -3f, 30f, 6f, Art.OUTLINE);
                r.fillRect(15f, -2f, 28f, 4f, ItemArt.METAL);
                r.fillRoundRect(40f, -11f, 14f, 22f, 5f, Art.OUTLINE);
                r.fillRoundRect(41.5f, -9.5f, 11f, 19f, 4f, ItemArt.METAL_DARK);
                r.fillRect(46f, -9f, 6f, 18f, ItemArt.METAL);
                if (e.muzzleFlash > 0f) {
                    r.fillTriangle(52f, -2f, 64f, 0f, 52f, 6f, Art.a(Palette.ORANGE, 0.8f));
                }
                break;
            }
            case DOCTOR: {
                // 电锯
                r.fillRoundRect(-4f, -6f, 22f, 12f, 5f, Art.OUTLINE);
                r.fillRoundRect(-2f, -4.5f, 19f, 9f, 4f, 0xFF3E7A4E);
                r.fillRect(14f, -8f, 12f, 16f, 0xFF3A4048);
                r.fillRect(20f, -9f, 10f, 18f, Art.OUTLINE);
                r.fillRect(21.5f, -7.5f, 8f, 15f, 0xFF7A8694);
                // 锯条
                r.fillRect(28f, -5f, 46f, 10f, Art.OUTLINE);
                r.fillRect(29f, -3.5f, 44f, 7f, ItemArt.STEEL);
                for (int i = 0; i < 7; i++) {
                    r.fillTriangle(32f + i * 6f, -3.5f, 35f + i * 6f, -9f, 38f + i * 6f, -3.5f, ItemArt.METAL);
                    r.fillTriangle(32f + i * 6f, 3.5f, 35f + i * 6f, 9f, 38f + i * 6f, 3.5f, ItemArt.METAL);
                }
                if (e.state == EnemyState.ATTACK || e.muzzleFlash > 0f) {
                    r.fillCircle(70f, 0, 6f, Art.a(Palette.ORANGE, 0.6f));
                    r.line(66f, -4f, 74f, 4f, 2f, Palette.GOLD);
                }
                break;
            }
            case NURSE: {
                // 超大注射器
                r.fillRoundRect(-4f, -5f, 20f, 10f, 5f, Art.OUTLINE);
                r.fillRoundRect(-2f, -3.5f, 17f, 7f, 3.5f, Art.SKIN);
                r.fillRect(12f, -9f, 34f, 18f, Art.OUTLINE);
                r.fillRect(14f, -7f, 30f, 14f, Art.a(0xFFE8F0F4, 0.9f));
                r.fillRect(18f, -7f, 20f, 14f, Art.a(0xFF9BE06A, 0.85f));
                r.fillRect(14f, -2f, 30f, 2f, Art.a(0xFF8A9AA8, 0.8f));
                r.fillRect(46f, -6f, 4f, 12f, Art.OUTLINE);
                r.fillRect(50f, -1.2f, 22f, 2.4f, ItemArt.METAL);
                break;
            }
            case FLU_PATIENT: {
                // 无力垂下的双臂
                r.fillRoundRect(-2f, -4f, 18f, 8f, 4f, Art.OUTLINE);
                r.fillRoundRect(-1f, -2.5f, 16f, 5f, 2.5f, Art.flash(0xFF93A87C, f));
                break;
            }
            case TECH: {
                // 试管
                r.fillRoundRect(-4f, -5f, 18f, 10f, 4f, Art.OUTLINE);
                r.fillRoundRect(-3f, -3.5f, 16f, 7f, 3f, Art.flash(0xFFF0D860, f));
                r.fillCircle(18f, 0, 9f, Art.a(0xFF65D714, 0.85f));
                r.fillCircle(18f, 0, 9f, Art.a(0xFFE8F0F4, 0.35f));
                r.fillCircle(21f, -3f, 3f, Art.a(0xFFFFFFFF, 0.5f));
                r.fillRect(14f, -4f, 5f, 8f, 0xFF8A6A48);
                Art.glow(r, 18f, 0, 22f, Palette.GREEN, 0.7f);
                break;
            }
            case BOSS: {
                // 巨锤
                r.fillRoundRect(-4f, -7f, 24f, 14f, 6f, Art.OUTLINE);
                r.fillRoundRect(-2f, -5.5f, 21f, 11f, 5f, Art.flash(0xFF8A5A3C, f));
                r.fillRect(-2f, -3f, 52f, 6f, Art.OUTLINE);
                r.fillRect(0f, -2f, 50f, 4f, 0xFF6A4A32);
                // 锤头
                r.fillRoundRect(40f, -22f, 26f, 44f, 7f, Art.OUTLINE);
                r.fillRoundRect(42f, -20f, 22f, 40f, 6f, Art.flash(0xFF8A8A92, f));
                r.fillRect(42f, -8f, 22f, 3f, Art.a(0xFF5A5A62, 0.8f));
                r.fillRect(42f, 4f, 22f, 3f, Art.a(0xFF5A5A62, 0.8f));
                r.fillRect(44f, -18f, 18f, 3f, Art.a(Palette.WHITE, 0.18f));
                r.fillCircle(53f, -14f, 3f, Art.a(BOSS_GOLD, 0.9f));
                r.fillCircle(53f, 14f, 3f, Art.a(BOSS_GOLD, 0.9f));
                if (e.state == EnemyState.ATTACK) {
                    // 锤击冲击环
                    r.setAlpha(0.5f);
                    r.strokeCircle(60f, 0f, 18f, 3f, Palette.GOLD);
                    r.setAlpha(1f);
                }
                break;
            }
            default:
                break;
        }
        r.restore();
    }

    // ---------- 哨兵炮台 ----------

    private static void drawSentry(Renderer r, World w, Enemy e) {
        float t = w.time;
        float f = MathUtil.clamp(e.hurtFlash, 0f, 1f);
        int metal = Art.flash(SENTRY_METAL, f);
        int dark = Art.flash(SENTRY_DARK, f);
        boolean alarmed = e.state == EnemyState.CHASE || e.state == EnemyState.ATTACK || e.alertTimer > 0f;
        boolean offline = w.camerasDisabled;   // 骇客成功后安全系统瘫痪，炮台断电
        if (offline) {
            metal = Art.desaturate(metal, 0.55f);
            dark = Art.desaturate(dark, 0.45f);
            alarmed = false;
        }

        // 扫描扇区（断电时不画）
        if (!offline) {
            float sweep = 0.24f;
            float alpha = alarmed ? 0.11f : 0.045f;
            float mx = e.x + (float) Math.cos(e.angle) * 26f;
            float my = e.y - 18f + (float) Math.sin(e.angle) * 26f;
            Art.fan(r, mx, my, 230f, e.angle - sweep, sweep * 2f,
                    Art.a(alarmed ? Palette.RED : Palette.CYAN, alpha));
            Art.fan(r, mx, my, 120f, e.angle - sweep * 0.7f, sweep * 1.4f,
                    Art.a(alarmed ? Palette.ALERT : Palette.CYAN, alpha * 1.7f));
        }

        Art.shadow(r, e.x, e.y + 8f, 33f, 12f, 135);
        // 固定底座
        Art.discOutlined(r, e.x, e.y + 5f, 30f, dark, 0xFF0E1216, 3.2f);
        Art.discOutlined(r, e.x, e.y + 5f, 24f, metal, 0xFF0E1216, 2.2f);
        Art.discOutlined(r, e.x, e.y + 5f, 12f, dark, 0xFF0E1216, 2f);
        // 底座警示灯 + 铆钉
        for (int i = 0; i < 6; i++) {
            float a = i * MathUtil.TAU / 6f + 0.35f;
            float bx = e.x + (float) Math.cos(a) * 21f;
            float by = e.y + 5f + (float) Math.sin(a) * 21f;
            r.fillCircle(bx, by, 2.2f, Art.a(0xFFDDE8F0, 0.7f));
        }
        // 支撑脚
        r.line(e.x - 18f, e.y + 24f, e.x - 26f, e.y + 34f, 5f, 0xFF141A20);
        r.line(e.x + 18f, e.y + 24f, e.x + 26f, e.y + 34f, 5f, 0xFF141A20);
        // 立柱
        r.fillRect(e.x - 10f, e.y - 14f, 20f, 22f, 0xFF141A20);
        r.fillRect(e.x - 8f, e.y - 13f, 16f, 20f, dark);
        r.fillRect(e.x - 8f, e.y - 13f, 5f, 20f, Art.a(Palette.WHITE, 0.10f));

        // 旋转炮头
        r.save();
        r.translate(e.x, e.y - 18f);
        r.rotate((float) Math.toDegrees(e.angle));
        // 双枪管
        r.fillRect(4f, -8f, 38f, 6f, 0xFF0E1216);
        r.fillRect(6f, -6.5f, 34f, 3.2f, metal);
        r.fillRect(4f, 3f, 38f, 6f, 0xFF0E1216);
        r.fillRect(6f, 4.5f, 34f, 3.2f, dark);
        // 枪口制退器
        r.fillRect(40f, -9f, 7f, 19f, 0xFF0E1216);
        r.fillRect(41f, -7f, 4f, 15f, metal);
        // 机匣
        r.fillRoundRect(-17f, -15f, 34f, 30f, 7f, 0xFF0E1216);
        r.fillRoundRect(-15f, -13f, 30f, 26f, 6f, metal);
        r.fillRect(-15f, -13f, 30f, 7f, Art.a(Palette.WHITE, 0.12f));
        r.fillRect(-15f, -1f, 30f, 2f, Art.a(dark, 0.8f));
        // 弹鼓
        Art.discOutlined(r, -9f, 9f, 7f, dark, 0xFF0E1216, 1.6f);
        // 传感器：在线=闪烁红眼，断电=暗绿灯
        float blink = 0.5f + 0.5f * (float) Math.sin(t * (alarmed ? 14f : 4f));
        if (offline) {
            Art.discOutlined(r, 2f, -8f, 4.2f, Art.a(0xFF3E6A50, 0.85f), 0xFF0E1216, 1.4f);
        } else {
            Art.discOutlined(r, 2f, -8f, 4.2f, Art.a(Palette.ALERT, 0.55f + 0.45f * blink), 0xFF0E1216, 1.4f);
            if (alarmed) Art.glow(r, 46f, 0f, 16f, Palette.RED, 0.9f);
        }
        // 开火枪口焰
        if (!offline && e.muzzleFlash > 0f) {
            float mf = MathUtil.clamp(e.muzzleFlash / 0.12f, 0f, 1f);
            r.fillTriangle(44f, -8f, 62f, 0f, 44f, 8f, Art.a(Palette.ORANGE, 0.9f * mf));
            r.fillTriangle(44f, -4f, 56f, 0f, 44f, 4f, Art.a(Palette.GOLD, 0.95f * mf));
        }
        r.restore();
        if (offline) {
            // 断电：歪垂的天线 + 一点电火花
            r.line(e.x + 15f, e.y - 30f, e.x + 26f, e.y - 40f, 2.4f, Art.a(0xFF2A3038, 0.9f));
            float sp = (t * 3f) % 1f;
            if (sp < 0.18f) {
                r.line(e.x + 10f, e.y - 26f, e.x + 18f, e.y - 32f, 1.6f, Art.a(Palette.CYAN, 0.6f));
                r.fillCircle(e.x + 18f, e.y - 32f, 2f, Art.a(Palette.CYAN, 0.6f));
            }
        }

        // 天线 + 顶部警示灯（断电则不亮）
        if (!offline) {
            r.line(e.x + 15f, e.y - 30f, e.x + 22f, e.y - 52f, 2.4f, 0xFF0E1216);
            float blink2 = 0.5f + 0.5f * (float) Math.sin(t * 7f);
            Art.discOutlined(r, e.x + 22f, e.y - 54f, 3.8f, Art.a(Palette.ALERT, 0.35f + 0.65f * blink2), 0xFF0E1216, 1.2f);
        }

        // 电击眩晕
        if (e.stunTimer > 0f) {
            for (int i = 0; i < 3; i++) {
                float a = t * 9f + i * 2.1f;
                float bx = e.x + (float) Math.cos(a) * 30f;
                float by = e.y - 10f + (float) Math.sin(a) * 18f;
                r.line(e.x - 6f, e.y - 10f, bx, by, 2f, Art.a(Palette.CYAN, 0.8f));
                r.line(bx, by, bx + 6f, by - 6f, 2f, Art.a(Palette.CYAN, 0.8f));
                r.fillCircle(bx + 6f, by - 6f, 2.4f, Palette.CYAN);
            }
        }
        // 血条
        if (e.hp < e.maxHp) {
            float bw = 48f;
            r.fillRoundRect(e.x - bw / 2 - 1, e.y - 62f, bw + 2, 8f, 3f, Art.a(Palette.BLACK, 0.65f));
            float hp = MathUtil.clamp(e.hp / e.maxHp, 0f, 1f);
            r.fillRoundRect(e.x - bw / 2, e.y - 61f, bw * hp, 6f, 2f, Palette.GREEN);
        }
        if (!offline && (e.state == EnemyState.ALERT || e.state == EnemyState.CHASE)) {
            float by = e.y - 70f + (float) Math.sin(t * 8f) * 2f;
            Art.discOutlined(r, e.x, by, 12f, Palette.RED, 0xFF2A1414, 2.4f);
            r.setTextSize(19);
            r.setTextBold(true);
            r.setTextAlign(1);
            r.setTextColor(Palette.WHITE);
            r.drawText("!", e.x, by);
            r.setTextBold(false);
        }
    }

    // ---------- 尸体 ----------

    private static void drawCorpse(Renderer r, World w, Enemy e) {
        float scale = e.type == EnemyType.BOSS ? 1.5f : 1f;
        // deathTimer 30→0 表示尸体存在时长，最后 5s 淡出
        float fade = MathUtil.clamp(e.deathTimer / 5f, 0f, 1f);
        if (fade <= 0.01f && e.deathTimer <= 0.01f) fade = 0.2f;
        int cloth;
        switch (e.type) {
            case GUARD: cloth = GUARD_CLOTH; break;
            case DOCTOR: cloth = DOCTOR_CLOTH; break;
            case NURSE: cloth = NURSE_CLOTH; break;
            case FLU_PATIENT: cloth = FLU_CLOTH; break;
            case TECH: cloth = TECH_CLOTH; break;
            case SENTRY: cloth = SENTRY_METAL; break;
            case BOSS: cloth = BOSS_CLOTH; break;
            default: cloth = GUARD_CLOTH; break;
        }
        cloth = Art.desaturate(cloth, 0.4f);
        int a = (int) (255 * fade);
        Art.shadow(r, e.x, e.y + 6f, 32f * scale, 11f * scale, (int) (110 * fade));
        // 血泊
        Art.ellipseRot(r, e.x, e.y + 4f, 30f * scale, 15f * scale, (float) Math.toDegrees(e.angle),
                Palette.withAlpha(Palette.BLOOD, (int) (140 * fade)));
        Art.ellipseRot(r, e.x - 12f * scale, e.y + 8f, 8f * scale, 4f * scale, 20f,
                Palette.withAlpha(Palette.RED_DARK, (int) (120 * fade)));
        // 瘫倒的身体
        Art.ellipseRot(r, e.x, e.y, 24f * scale, 12f * scale, (float) Math.toDegrees(e.angle),
                Palette.withAlpha(Art.darken(cloth, 0.25f), a));
        Art.ellipseRot(r, e.x, e.y - 2f * scale, 18f * scale, 8f * scale, (float) Math.toDegrees(e.angle),
                Palette.withAlpha(cloth, a));
        // 头
        float hx = e.x - (float) Math.cos(e.angle) * 22f * scale;
        float hy = e.y - (float) Math.sin(e.angle) * 12f * scale;
        Art.discOutlined(r, hx, hy, 12f * scale,
                Palette.withAlpha(Art.SKIN_DARK, a), Palette.withAlpha(Art.OUTLINE, a), 2f);
        // 四肢
        r.line(e.x + 6f * scale, e.y, e.x + 30f * scale, e.y + 10f * scale, 7f * scale,
                Palette.withAlpha(Art.SKIN_DARK, a));
        r.line(e.x - 4f * scale, e.y + 2f * scale, e.x - 26f * scale, e.y + 14f * scale, 7f * scale,
                Palette.withAlpha(Art.SKIN_DARK, a));
        // 眼睛 X
        r.line(hx - 5f, hy - 3f, hx - 1f, hy + 1f, 1.8f, Palette.withAlpha(0xFF2A2222, a));
        r.line(hx - 1f, hy - 3f, hx - 5f, hy + 1f, 1.8f, Palette.withAlpha(0xFF2A2222, a));
        // 可被护士复活的淡标记（bodyTimer>0 期间）
        if (e.bodyTimer > 0f) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(w.time * 4f + e.x * 0.01f);
            int pc = Art.a(0xFFB06AE8, (0.14f + 0.14f * pulse) * fade);
            r.fillRect(e.x - 6f, e.y - 34f * scale, 12f, 3f, pc);
            r.fillRect(e.x - 1.5f, e.y - 38.5f * scale, 3f, 12f, pc);
        }
    }
}
