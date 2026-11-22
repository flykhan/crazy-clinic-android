package com.crazyclinic.core.render;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.world.Decal;
import com.crazyclinic.core.world.Enemy;
import com.crazyclinic.core.world.FloatingText;
import com.crazyclinic.core.world.Particle;
import com.crazyclinic.core.world.Pickup;
import com.crazyclinic.core.world.Player;
import com.crazyclinic.core.world.Projectile;
import com.crazyclinic.core.world.TileMap;
import com.crazyclinic.core.world.TileType;
import com.crazyclinic.core.world.World;

import java.util.WeakHashMap;

/**
 * 720x960 世界视口渲染：医院瓦片/物件 → 贴花 → 拾取物 → 角色（y 排序）→ 子弹/粒子/文字
 * → 灯光/暗角/警报/受伤红闪。所有图元程序化绘制，无位图。
 */
public final class WorldRenderer {
    private static final int MAX_PARTICLES = 300;

    // 灯具位置缓存（每张地图只算一次，确定性哈希；WeakHashMap 避免持有旧地图）
    private static final WeakHashMap<TileMap, float[]> LIGHTS = new WeakHashMap<>();

    // 实体 y 排序 scratch（单渲染线程，复用，避免每帧分配）
    private static Enemy[] enemyScratch = new Enemy[0];
    private static float[] keyScratch = new float[0];

    public void render(Game game, Renderer r, World w) {
        r.clear(Palette.BG_DEEP);
        TileMap map = w.map;
        Player p = w.player;

        // 屏幕震动
        float shakeX = 0f, shakeY = 0f;
        if (w.shake > 0.01f) {
            shakeX = (float) Math.sin(w.time * 67.3f) * w.shake * 7f;
            shakeY = (float) Math.cos(w.time * 51.7f) * w.shake * 5f;
        }

        int minTx = Math.max(0, map.tx(w.cameraX) - 1);
        int maxTx = Math.min(map.width - 1, map.tx(w.cameraX + World.VIEW_W) + 1);
        int minTy = Math.max(0, map.ty(w.cameraY) - 1);
        int maxTy = Math.min(map.height - 1, map.ty(w.cameraY + World.VIEW_H) + 1);
        int tint = TileArt.themeTint(w);

        r.save();
        r.clipRect(0f, 0f, World.VIEW_W, World.VIEW_H);
        r.translate(-w.cameraX + shakeX, -w.cameraY + shakeY);

        // ---- 1. 地面 ----
        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                if (!TileArt.isWallLike(map.type(tx, ty))) {
                    TileArt.drawFloor(r, map, tx, ty, tint);
                }
            }
        }
        // 墙脚接触阴影
        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                if (!TileArt.isWallLike(map.type(tx, ty))) {
                    TileArt.drawContactShadow(r, map, tx, ty);
                }
            }
        }

        // ---- 2. 地面贴花 ----
        for (Decal d : w.decals) {
            if (inView(d.x, d.y, w, 80f)) FxArt.drawDecal(r, w, d);
        }

        // ---- 3. 墙 / 门 / 物件（按行序绘制，保证墙正面覆盖下方地面） ----
        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                TileType t = map.type(tx, ty);
                switch (t) {
                    case WALL: TileArt.drawWall(r, map, tx, ty, tint); break;
                    case WINDOW: TileArt.drawWindowWall(r, map, tx, ty, tint); break;
                    case DOOR_LOCKED: TileArt.drawDoor(r, tx, ty, true, false); break;
                    case DOOR_OPEN: TileArt.drawDoor(r, tx, ty, false, true); break;
                    case ELEVATOR: TileArt.drawElevator(r, tx, ty, w.elevatorUnlocked); break;
                    case COMPUTER: TileArt.drawComputer(r, tx, ty, w.camerasDisabled, w.time); break;
                    case LOCKER: TileArt.drawLocker(r, tx, ty); break;
                    case CRATE: TileArt.drawCrate(r, tx, ty); break;
                    case BED: TileArt.drawBed(r, tx, ty); break;
                    default: break;
                }
            }
        }

        // ---- 4. 拾取物 ----
        for (Pickup pk : w.pickups) {
            if (pk.taken) continue;
            if (!inView(pk.x, pk.y, w, 60f)) continue;
            FxArt.drawPickup(r, w, pk);
        }

        // ---- 5. 角色（按 y 排序；玩家插入其间） ----
        drawSortedActors(r, w);

        // ---- 6. 子弹 ----
        for (Projectile pr : w.projectiles) {
            if (!inView(pr.x, pr.y, w, 40f)) continue;
            FxArt.drawProjectile(r, w, pr);
        }

        // ---- 7. 粒子 ----
        int drawn = 0;
        for (int i = 0; i < w.particles.size() && drawn < MAX_PARTICLES; i++) {
            Particle pt = w.particles.get(i);
            if (pt.life <= 0f) continue;
            if (!inView(pt.x, pt.y, w, 40f)) continue;
            FxArt.drawParticle(r, pt);
            drawn++;
        }

        // ---- 8. 浮动文字 ----
        for (FloatingText ft : w.floatingTexts) {
            if (ft.life <= 0f) continue;
            float a = MathUtil.clamp(ft.life / ft.maxLife, 0f, 1f);
            float rise = (1f - a) * 26f;
            Art.textOutlined(r, ft.text, ft.x, ft.y - rise, ft.size,
                    Palette.alpha(ft.color, Math.min(1f, a * 1.6f)));
        }

        r.restore();

        // ---- 9. 光照 / 暗角 / 屏幕反馈（视口坐标） ----
        drawAtmosphere(r, w, shakeX, shakeY, tint);
    }

    private static boolean inView(float x, float y, World w, float margin) {
        return x >= w.cameraX - margin && x <= w.cameraX + World.VIEW_W + margin
                && y >= w.cameraY - margin && y <= w.cameraY + World.VIEW_H + margin;
    }

    private void drawSortedActors(Renderer r, World w) {
        int n = 0;
        for (Enemy e : w.enemies) {
            if (!inView(e.x, e.y, w, 120f)) continue;
            if (n >= enemyScratch.length) grow(n + 8);
            enemyScratch[n] = e;
            keyScratch[n] = e.y;
            n++;
        }
        // 插入排序（n 很小）
        for (int i = 1; i < n; i++) {
            Enemy e = enemyScratch[i];
            float key = keyScratch[i];
            int j = i - 1;
            while (j >= 0 && keyScratch[j] > key) {
                enemyScratch[j + 1] = enemyScratch[j];
                keyScratch[j + 1] = keyScratch[j];
                j--;
            }
            enemyScratch[j + 1] = e;
            keyScratch[j + 1] = key;
        }
        Player p = w.player;
        boolean playerDrawn = false;
        for (int i = 0; i < n; i++) {
            Enemy e = enemyScratch[i];
            if (!playerDrawn && !p.dead && e.y > p.y) {
                CharacterArt.drawPlayer(r, w, p);
                playerDrawn = true;
            }
            CharacterArt.drawEnemy(r, w, e);
        }
        if (!playerDrawn) CharacterArt.drawPlayer(r, w, p);
        // 清理引用，避免 static 数组持有已移除敌人
        for (int i = 0; i < n; i++) enemyScratch[i] = null;
    }

    private static void grow(int need) {
        int cap = Math.max(need, Math.max(8, enemyScratch.length * 2));
        Enemy[] ne = new Enemy[cap];
        float[] nk = new float[cap];
        System.arraycopy(enemyScratch, 0, ne, 0, enemyScratch.length);
        System.arraycopy(keyScratch, 0, nk, 0, keyScratch.length);
        enemyScratch = ne;
        keyScratch = nk;
    }

    // ==================== 氛围 ====================

    private void drawAtmosphere(Renderer r, World w, float shakeX, float shakeY, int tint) {
        float px = w.player.x - w.cameraX + shakeX;
        float py = w.player.y - w.cameraY + shakeY;

        r.save();
        r.clipRect(0f, 0f, World.VIEW_W, World.VIEW_H);

        // 每层轻微色温（区分病区）
        r.fillRect(0f, 0f, World.VIEW_W, World.VIEW_H, Palette.alpha(tint, 0.045f));

        // 日光灯光池
        float[] lights = lights(w.map);
        for (int i = 0; i < lights.length; i += 2) {
            float lx = lights[i] - w.cameraX + shakeX;
            float ly = lights[i + 1] - w.cameraY + shakeY;
            if (lx < -160f || lx > World.VIEW_W + 160f || ly < -160f || ly > World.VIEW_H + 160f) continue;
            float flicker = 1f;
            int id = (int) (lights[i] + lights[i + 1] * 3.1f);
            float h = Art.hash3(id, id + 7, 3);
            if (h > 0.72f) {
                flicker = 0.82f + 0.18f * (float) Math.sin(w.time * (7f + h * 9f) + id);
            }
            // 6 层同心椭圆近似柔光，避免硬边圆环
            Art.ellipse(r, lx, ly, 158f, 132f, Art.a(Palette.LIGHT, 0.014f * flicker));
            Art.ellipse(r, lx, ly, 128f, 106f, Art.a(Palette.LIGHT, 0.018f * flicker));
            Art.ellipse(r, lx, ly, 100f, 82f, Art.a(Palette.LIGHT, 0.022f * flicker));
            Art.ellipse(r, lx, ly, 74f, 60f, Art.a(Palette.LIGHT, 0.027f * flicker));
            Art.ellipse(r, lx, ly, 50f, 40f, Art.a(0xFFFFF6DE, 0.032f * flicker));
            Art.ellipse(r, lx, ly, 26f, 20f, Art.a(0xFFFFF6DE, 0.042f * flicker));
        }

        // 玩家为中心的光圈（暖白，多层柔光）
        Art.ellipse(r, px, py, 280f, 246f, Art.a(Palette.LIGHT, 0.013f));
        Art.ellipse(r, px, py, 210f, 184f, Art.a(Palette.LIGHT, 0.016f));
        Art.ellipse(r, px, py, 150f, 130f, Art.a(Palette.LIGHT, 0.020f));
        Art.ellipse(r, px, py, 100f, 86f, Art.a(0xFFFFF6DE, 0.024f));
        Art.ellipse(r, px, py, 58f, 50f, Art.a(0xFFFFF6DE, 0.030f));

        // 暗角：中心透明 → 边缘深黑
        float vg = w.player.hp / Math.max(1f, w.player.maxHp) < 0.3f ? 0.80f : 0.70f;
        r.fillRadialGradient(px, py, 660f, Art.a(Palette.BLACK, 0f), Art.a(Palette.BLACK, vg));

        // 警戒：屏幕边缘红色呼吸
        if (w.alert > 0.02f) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(w.time * (3.5f + w.alert * 4f));
            float a = MathUtil.clamp(w.alert, 0f, 1f) * (0.07f + 0.07f * pulse);
            r.fillRadialGradient(World.VIEW_W * 0.5f, World.VIEW_H * 0.5f, 700f,
                    Art.a(Palette.RED, 0f), Art.a(Palette.ALERT, a));
            // 顶部警报灯条
            r.fillVerticalGradient(0f, 0f, World.VIEW_W, 22f,
                    Art.a(Palette.ALERT, a * 1.2f), Art.a(Palette.ALERT, 0f));
            r.fillVerticalGradient(0f, World.VIEW_H - 22f, World.VIEW_W, 22f,
                    Art.a(Palette.ALERT, 0f), Art.a(Palette.ALERT, a * 1.2f));
        }

        // 受伤红闪
        if (w.player.hurtFlash > 0.01f) {
            r.fillRect(0f, 0f, World.VIEW_W, World.VIEW_H,
                    Palette.alpha(Palette.RED, 0.30f * MathUtil.clamp(w.player.hurtFlash, 0f, 1f)));
        }
        // 隐身时轻微蓝色氛围
        if (w.player.invisTimer > 0f) {
            r.fillRadialGradient(px, py, 420f, Art.a(Palette.CYAN, 0f),
                    Art.a(Palette.CYAN, 0.10f));
        }
        r.restore();
    }

    private static float[] lights(TileMap map) {
        float[] cached = LIGHTS.get(map);
        if (cached != null) return cached;
        int count = 0;
        for (int ty = 0; ty < map.height; ty++) {
            for (int tx = 0; tx < map.width; tx++) {
                if (!map.solid(tx, ty) && Art.hash(tx * 13 + 5, ty * 7 + 3) > 0.968f) count++;
            }
        }
        float[] out = new float[count * 2];
        int k = 0;
        for (int ty = 0; ty < map.height && k < out.length; ty++) {
            for (int tx = 0; tx < map.width && k < out.length; tx++) {
                if (!map.solid(tx, ty) && Art.hash(tx * 13 + 5, ty * 7 + 3) > 0.968f) {
                    out[k++] = map.cx(tx);
                    out[k++] = map.cy(ty);
                }
            }
        }
        LIGHTS.put(map, out);
        return out;
    }
}
