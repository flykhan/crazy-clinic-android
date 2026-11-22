package com.crazyclinic.core.game;

import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.world.Decal;
import com.crazyclinic.core.world.FloatingText;
import com.crazyclinic.core.world.Particle;
import com.crazyclinic.core.world.Player;
import com.crazyclinic.core.world.World;

/**
 * 粒子 / 浮字 / 贴花的生命周期更新与胶囊化生成。
 * 全部带数量上限，避免长时间战斗时对象无限增长。
 */
final class EffectsSystem {
    static final int MAX_PARTICLES = 300;
    static final int MAX_TEXTS = 48;
    static final int MAX_DECALS = 150;

    void update(World w, float dt) {
        if (w == null || dt <= 0f) return;
        updateParticles(w, dt);
        updateTexts(w, dt);
        updateDecals(w, dt);
    }

    private void updateParticles(World w, float dt) {
        for (int i = w.particles.size() - 1; i >= 0; i--) {
            Particle p = w.particles.get(i);
            p.life -= dt;
            if (p.life <= 0f) {
                removeAt(w.particles, i);
                continue;
            }
            switch (p.kind) {
                case 1: // 血滴
                    p.vy += 760f * dt;
                    break;
                case 2: // 火花
                    p.vy += 220f * dt;
                    p.vx *= 1f - 2.4f * dt;
                    p.vy *= 1f - 1.6f * dt;
                    break;
                case 3: // 烟雾
                    p.vy -= 26f * dt;
                    p.vx *= 1f - 0.9f * dt;
                    p.vy *= 1f - 0.9f * dt;
                    p.rotation += p.spin * dt * 0.3f;
                    break;
                case 4: // 弹壳
                    p.vy += 520f * dt;
                    break;
                case 5: // 碎屑
                    p.vy += 460f * dt;
                    p.rotation += p.spin * dt;
                    break;
                default: // 光点
                    p.vx *= 1f - 1.8f * dt;
                    p.vy *= 1f - 1.8f * dt;
                    break;
            }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
        }
    }

    private void updateTexts(World w, float dt) {
        for (int i = w.floatingTexts.size() - 1; i >= 0; i--) {
            FloatingText ft = w.floatingTexts.get(i);
            ft.life -= dt;
            if (ft.life <= 0f) {
                removeAt(w.floatingTexts, i);
                continue;
            }
            ft.y += ft.vy * dt;
            ft.vy *= 1f - 1.7f * dt;
        }
    }

    private void updateDecals(World w, float dt) {
        for (int i = w.decals.size() - 1; i >= 0; i--) {
            Decal d = w.decals.get(i);
            d.life -= dt;
            if (d.life <= 0f) removeAt(w.decals, i);
        }
    }

    private static <T> void removeAt(java.util.List<T> list, int index) {
        int last = list.size() - 1;
        if (index != last) list.set(index, list.get(last));
        list.remove(last);
    }

    // ------------------------------------------------------------------
    // 生成器
    // ------------------------------------------------------------------

    /** 命中血粒子：朝 dirAngle 方向的锥形喷溅 + 少量向后飞溅。 */
    void blood(World w, float x, float y, float dirAngle, int count) {
        if (w == null) return;
        count = Math.min(count, Math.max(0, MAX_PARTICLES - w.particles.size()));
        for (int i = 0; i < count; i++) {
            float a = dirAngle + MathUtil.rand(-0.85f, 0.85f);
            float sp = MathUtil.rand(70f, 300f);
            int color = (i & 1) == 0 ? Palette.BLOOD : Palette.RED_DARK;
            w.addParticle(x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp,
                    MathUtil.rand(0.22f, 0.55f), MathUtil.rand(3f, 7f), color, 1);
        }
    }

    /** 普通命中火花。 */
    void sparks(World w, float x, float y, float dirAngle, int count) {
        if (w == null) return;
        count = Math.min(count, Math.max(0, MAX_PARTICLES - w.particles.size()));
        for (int i = 0; i < count; i++) {
            float a = dirAngle + MathUtil.rand(-1.2f, 1.2f);
            float sp = MathUtil.rand(120f, 380f);
            w.addParticle(x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp,
                    MathUtil.rand(0.12f, 0.3f), MathUtil.rand(2f, 4f),
                    i % 2 == 0 ? Palette.LIGHT : Palette.GOLD, 2);
        }
    }

    /** 子弹打墙 / 打金属。 */
    void impact(World w, float x, float y, float dirAngle) {
        sparks(w, x, y, dirAngle, 5);
        smoke(w, x, y, 2, 0.5f);
    }

    void smoke(World w, float x, float y, int count, float scale) {
        if (w == null) return;
        count = Math.min(count, Math.max(0, MAX_PARTICLES - w.particles.size()));
        for (int i = 0; i < count; i++) {
            w.addParticle(x + MathUtil.rand(-8f, 8f), y + MathUtil.rand(-8f, 8f),
                    MathUtil.rand(-26f, 26f), MathUtil.rand(-50f, -10f),
                    MathUtil.rand(0.45f, 1.1f), MathUtil.rand(10f, 22f),
                    i % 3 == 0 ? Palette.WALL_TRIM : Palette.WALL_SIDE, 3);
        }
        // 用 scale 调整一下不额外分配：传给粒子大小
        int from = Math.max(0, w.particles.size() - count);
        for (int i = from; i < w.particles.size(); i++) {
            w.particles.get(i).size *= Math.max(0.2f, scale);
        }
    }

    /** 枪口焰 + 硝烟。 */
    void muzzle(World w, Player p, float angle, boolean big) {
        if (w == null || p == null) return;
        float mx = p.x + (float) Math.cos(angle) * (p.radius + 18f);
        float my = p.y + (float) Math.sin(angle) * (p.radius + 18f);
        if (w.particles.size() < MAX_PARTICLES) {
            w.addParticle(mx, my, (float) Math.cos(angle) * 160f, (float) Math.sin(angle) * 160f,
                    0.08f, big ? 22f : 14f, Palette.LIGHT, 2);
        }
        smoke(w, mx, my, big ? 3 : 1, big ? 1.4f : 0.8f);
        if (w.particles.size() < MAX_PARTICLES) {
            w.addParticle(mx, my, MathUtil.rand(-40f, 40f), MathUtil.rand(-40f, 40f),
                    0.35f, big ? 9f : 6f, Palette.LIGHT, 0);
        }
    }

    /** 抛壳。 */
    void shell(World w, Player p, float angle) {
        if (w == null || p == null || w.particles.size() >= MAX_PARTICLES) return;
        float side = angle + MathUtil.PI * 0.5f + MathUtil.rand(-0.4f, 0.4f);
        float sp = MathUtil.rand(70f, 150f);
        w.addParticle(p.x, p.y, (float) Math.cos(side) * sp, (float) Math.sin(side) * sp,
                0.7f, 3.4f, Palette.GOLD, 4);
    }

    /** 爆炸：火球 + 烟 + 碎屑 + 地面焦痕 + 震屏。 */
    void explosion(World w, float x, float y) {
        if (w == null) return;
        int room = Math.max(0, MAX_PARTICLES - w.particles.size());
        int fire = Math.min(10, room);
        for (int i = 0; i < fire; i++) {
            float a = MathUtil.rand(0f, MathUtil.TAU);
            float sp = MathUtil.rand(60f, 300f);
            int c = i < 3 ? Palette.LIGHT : (i < 7 ? Palette.ORANGE : Palette.RED_DARK);
            w.addParticle(x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp,
                    MathUtil.rand(0.2f, 0.5f), MathUtil.rand(10f, 26f), c, 0);
        }
        smoke(w, x, y, 6, 1.8f);
        if (w.particles.size() < MAX_PARTICLES) {
            w.addParticle(x, y, 0f, 0f, 0.16f, 54f, Palette.alpha(Palette.LIGHT, 0.8f), 0);
        }
        decal(w, x, y, 2, Palette.BLACK, 1.4f);
        w.shake = Math.max(w.shake, 0.6f);
    }

    /** 火焰喷射粒子。 */
    void flame(World w, Player p, float angle, float range) {
        if (w == null || p == null || w.particles.size() >= MAX_PARTICLES) return;
        float a = angle + MathUtil.rand(-0.18f, 0.18f);
        float sp = MathUtil.rand(320f, 520f);
        float dist = MathUtil.rand(20f, Math.max(40f, range));
        float x = p.x + (float) Math.cos(a) * dist;
        float y = p.y + (float) Math.sin(a) * dist;
        int c = MathUtil.chance(0.45f) ? Palette.LIGHT : (MathUtil.chance(0.5f) ? Palette.ORANGE : Palette.RED);
        w.addParticle(x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp,
                MathUtil.rand(0.12f, 0.3f), MathUtil.rand(8f, 18f), c, 0);
        if (MathUtil.chance(0.35f)) smoke(w, x, y, 1, 0.7f);
    }

    /** 玩家残影/闪避尘土。 */
    void dust(World w, float x, float y) {
        if (w == null || w.particles.size() >= MAX_PARTICLES) return;
        w.addParticle(x + MathUtil.rand(-10f, 10f), y + MathUtil.rand(-10f, 10f),
                MathUtil.rand(-30f, 30f), MathUtil.rand(-30f, 30f),
                0.35f, MathUtil.rand(8f, 16f), Palette.WALL_TRIM, 3);
    }

    /** 有毒气体。 */
    void poison(World w, float x, float y, int count) {
        if (w == null) return;
        count = Math.min(count, Math.max(0, MAX_PARTICLES - w.particles.size()));
        for (int i = 0; i < count; i++) {
            w.addParticle(x + MathUtil.rand(-26f, 26f), y + MathUtil.rand(-26f, 26f),
                    MathUtil.rand(-20f, 20f), MathUtil.rand(-34f, -6f),
                    MathUtil.rand(0.6f, 1.4f), MathUtil.rand(9f, 20f),
                    i % 2 == 0 ? Palette.GREEN : Palette.MINT, 3);
        }
    }

    /** 拾取闪光。 */
    void pickupFlash(World w, float x, float y, int color) {
        if (w == null || w.particles.size() >= MAX_PARTICLES) return;
        for (int i = 0; i < 5; i++) {
            float a = MathUtil.rand(0f, MathUtil.TAU);
            float sp = MathUtil.rand(40f, 120f);
            w.addParticle(x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp,
                    0.35f, MathUtil.rand(3f, 6f), color, 0);
        }
    }

    /** 电击火花（泰瑟/除颤）。 */
    void electric(World w, float x, float y, int count) {
        if (w == null) return;
        count = Math.min(count, Math.max(0, MAX_PARTICLES - w.particles.size()));
        for (int i = 0; i < count; i++) {
            float a = MathUtil.rand(0f, MathUtil.TAU);
            float sp = MathUtil.rand(90f, 260f);
            w.addParticle(x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp,
                    MathUtil.rand(0.12f, 0.32f), MathUtil.rand(2f, 5f),
                    i % 2 == 0 ? Palette.CYAN : Palette.WHITE, 2);
        }
    }

    /** 带数量上限的贴花。 */
    void decal(World w, float x, float y, int kind, int color, float scale) {
        if (w == null) return;
        if (w.decals.size() >= MAX_DECALS) removeAt(w.decals, 0);
        w.addDecal(x, y, kind, color, scale);
    }

    /** 死亡血泊贴花。 */
    void bloodPool(World w, float x, float y) {
        if (w == null) return;
        if (w.decals.size() >= MAX_DECALS) removeAt(w.decals, 0);
        w.addDecal(x, y, 0, Palette.BLOOD, MathUtil.rand(1.1f, 1.9f));
    }

    /** 浮字（带上限）。 */
    void text(World w, String text, float x, float y, int color) {
        if (w == null || text == null) return;
        if (w.floatingTexts.size() >= MAX_TEXTS) removeAt(w.floatingTexts, 0);
        w.addFloatingText(text, x, y, color);
    }
}
