package com.crazyclinic.core.render;

import com.crazyclinic.core.Controls;
import com.crazyclinic.core.Game;
import com.crazyclinic.core.HudState;
import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.content.Balance;
import com.crazyclinic.core.content.Texts;
import com.crazyclinic.core.world.Enemy;
import com.crazyclinic.core.world.EnemyType;
import com.crazyclinic.core.world.ItemType;
import com.crazyclinic.core.world.Pickup;
import com.crazyclinic.core.world.Player;
import com.crazyclinic.core.world.TileMap;
import com.crazyclinic.core.world.TileType;
import com.crazyclinic.core.world.WeaponType;
import com.crazyclinic.core.world.World;

import java.util.WeakHashMap;

/**
 * 720x1280 全屏 HUD + 触屏控件 + 骇客 overlay + 过关/暂停/BOSS 表现。
 * 控件位置全部来自 {@link Controls}，与引擎命中判定共用。
 */
public final class HudRenderer {
    private static final float TOP_H = 168f;
    private static final int GOLD = 0xFFF2B33D;

    // 小地图缓存（每张地图只扫描一次）
    private static final WeakHashMap<TileMap, Mini> MINI = new WeakHashMap<>();

    private static final class Mini {
        float[] walls;     // [tx,ty,...] 仅墙边界
        float[] elevator;  // [tx,ty]
        float[] computer;  // [tx,ty,...]
        int w, h;
    }

    private final StringBuilder sb = new StringBuilder(16);

    public void render(Game game, Renderer r, World w, HudState hud) {
        if (hud.hacking) {
            drawHacking(r, w, hud, game);
            drawScreenFx(r, w, hud);
            return;
        }
        drawTopBar(r, w, hud, game);
        drawMinimap(r, w, hud);
        drawWorldOverlays(r, w, hud);
        drawControls(r, w, hud);
        drawBossBar(r, w, hud);
        drawScreenFx(r, w, hud);
        if (hud.showingFloorClear) drawFloorClear(r, w, hud);
    }

    // ==================== 顶部 HUD ====================

    private void drawTopBar(Renderer r, World w, HudState hud, Game game) {
        float t = w.time;
        // 底板 + 底部渐隐
        r.fillRect(0f, 0f, 720f, TOP_H, Art.a(0xFF0B0F14, 0.84f));
        r.fillVerticalGradient(0f, TOP_H - 22f, 720f, 22f, Art.a(0xFF0B0F14, 0.84f), Art.a(0xFF0B0F14, 0f));
        r.fillRect(0f, 0f, 720f, 2f, Art.a(0xFF39434F, 0.9f));

        Player p = w.player;
        drawAvatar(r, p, hud, t);
        drawBars(r, w, hud, t);
        drawWeaponBlock(r, w, hud, t);

        // 目标条
        float oy = 132f;
        r.fillRoundRect(24f, oy, 672f, 32f, 10f, Art.a(0xFF121822, 0.88f));
        r.strokeRoundRect(24f, oy, 672f, 32f, 10f, 1.6f, Art.a(0xFF39434F, 0.9f));
        boolean alarm = w.alert > 0.55f;
        int accent = alarm ? Palette.RED : (w.level.bossLevel ? GOLD : Palette.MINT);
        r.fillRoundRect(24f, oy, 6f, 32f, 3f, accent);
        // 小菱形
        r.fillTriangle(44f, oy + 16f, 51f, oy + 9f, 58f, oy + 16f, accent);
        r.fillTriangle(44f, oy + 16f, 51f, oy + 23f, 58f, oy + 16f, accent);
        String obj = w.objective == null || w.objective.isEmpty() ? "探索这一层" : w.objective;
        r.setTextSize(21f);
        r.setTextAlign(0);
        r.setTextColor(alarm ? Art.a(0xFFFFD0C8, 1f) : Art.a(0xFFDDE8EE, 0.96f));
        r.drawText("目标 · " + obj, 66f, oy + 16f);
        if (alarm) {
            float bl = 0.5f + 0.5f * (float) Math.sin(t * 10f);
            r.setTextAlign(2);
            r.setTextBold(true);
            r.setTextColor(Art.a(Palette.ALERT, 0.55f + 0.45f * bl));
            r.drawText("警报", 682f, oy + 16f);
            r.setTextBold(false);
        }

        // 暂停键（世界层右上，Controls 常量）
        r.fillCircle(Controls.PAUSE_X, Controls.PAUSE_Y, Controls.PAUSE_R,
                Art.a(0xFF0B0F14, 0.66f));
        r.strokeCircle(Controls.PAUSE_X, Controls.PAUSE_Y, Controls.PAUSE_R, 2.4f,
                Art.a(0xFF8A96A6, 0.85f));
        r.fillRoundRect(Controls.PAUSE_X - 13f, Controls.PAUSE_Y - 16f, 8f, 32f, 3f, Palette.TEXT);
        r.fillRoundRect(Controls.PAUSE_X + 5f, Controls.PAUSE_Y - 16f, 8f, 32f, 3f, Palette.TEXT);
    }

    private void drawAvatar(Renderer r, Player p, HudState hud, float t) {
        float cx = 76f, cy = 76f;
        float ratio = MathUtil.clamp(p.hp / Math.max(1f, p.maxHp), 0f, 1f);
        boolean low = ratio < 0.3f;
        // 外框
        r.fillCircle(cx, cy, 63f, Art.a(0xFF0B0F14, 0.9f));
        r.strokeCircle(cx, cy, 62f, 3.4f, Art.a(0xFF55606E, 0.95f));
        r.strokeCircle(cx, cy, 56f, 2f, Art.a(0xFF2A323C, 0.9f));
        // 背景
        r.fillCircle(cx, cy, 56f, Art.a(low ? 0xFF2A1216 : 0xFF1A2028, 1f));
        r.fillCircle(cx, cy - 10f, 46f, Art.a(0xFF232C38, 1f));
        // 病号服领口
        r.fillRoundRect(cx - 36f, cy + 20f, 72f, 44f, 10f, Art.GOWN);
        r.fillTriangle(cx - 14f, cy + 20f, cx + 14f, cy + 20f, cx, cy + 40f, Art.a(Art.SKIN, 1f));
        r.fillRect(cx - 2f, cy + 26f, 4f, 26f, Art.GOWN_DARK);
        // 脖子/脸
        r.fillRoundRect(cx - 15f, cy + 8f, 30f, 22f, 6f, Art.SKIN_DARK);
        // 头发底（先画后脑，再画脸）
        r.fillCircle(cx, cy - 14f, 38f, Art.OUTLINE);
        r.fillCircle(cx, cy - 22f, 37f, Art.HAIR_DARK);
        r.fillCircle(cx, cy - 12f, 35f, Art.SKIN);
        // 表情（低血量惊恐/流汗）
        int eye = low ? 0xFF3A1414 : 0xFF2A2222;
        Art.discOutlined(r, cx - 13f, cy - 12f, 5.4f, eye, 0xFF1A1414, 1f);
        Art.discOutlined(r, cx + 13f, cy - 12f, 5.4f, eye, 0xFF1A1414, 1f);
        r.fillCircle(cx - 14.5f, cy - 14f, 1.8f, Art.a(0xFFFFFFFF, 0.75f));
        r.fillCircle(cx + 11.5f, cy - 14f, 1.8f, Art.a(0xFFFFFFFF, 0.75f));
        if (low) {
            r.line(cx - 20f, cy - 24f, cx - 7f, cy - 20f, 2.4f, 0xFF3A1414);
            r.line(cx + 7f, cy - 20f, cx + 20f, cy - 24f, 2.4f, 0xFF3A1414);
            // 出汗
            float drip = (t * 0.8f) % 1f;
            Art.ellipse(r, cx + 30f, cy - 22f + drip * 22f, 4f, 6f, Art.a(0xFF9AD8F0, 0.9f - drip * 0.6f));
        } else {
            r.line(cx - 19f, cy - 22f, cx - 7f, cy - 24f, 2.2f, Art.HAIR_DARK);
            r.line(cx + 7f, cy - 24f, cx + 19f, cy - 22f, 2.2f, Art.HAIR_DARK);
        }
        r.line(cx - 8f, cy + 4f, cx + 8f, cy + 4f, 2.6f, low ? 0xFF8A2A28 : 0xFF8A5A50);
        // 金色刘海（只盖额头，露出眼睛）
        Art.ellipse(r, cx, cy - 35f, 34f, 15f, Art.HAIR);
        r.fillTriangle(cx - 34f, cy - 38f, cx - 26f, cy - 58f, cx - 16f, cy - 40f, Art.HAIR);
        r.fillTriangle(cx - 16f, cy - 42f, cx - 4f, cy - 62f, cx + 6f, cy - 42f, Art.HAIR_HI);
        r.fillTriangle(cx + 6f, cy - 40f, cx + 18f, cy - 58f, cx + 30f, cy - 36f, Art.HAIR);
        // 刘海尖
        r.fillTriangle(cx - 12f, cy - 42f, cx - 5f, cy - 24f, cx + 1f, cy - 42f, Art.HAIR);
        r.fillTriangle(cx + 2f, cy - 42f, cx + 9f, cy - 26f, cx + 17f, cy - 42f, Art.HAIR_HI);
        // 鬓角
        r.fillTriangle(cx - 35f, cy - 30f, cx - 24f, cy - 30f, cx - 30f, cy - 2f, Art.HAIR_DARK);
        r.fillTriangle(cx + 35f, cy - 30f, cx + 24f, cy - 30f, cx + 30f, cy - 2f, Art.HAIR_DARK);
        // 头像外框（在头发之后重描，压住越界的发梢）
        r.strokeCircle(cx, cy, 62f, 3.4f, Art.a(0xFF55606E, 0.95f));
        r.strokeCircle(cx, cy, 58.5f, 1.6f, Art.a(0xFF2A323C, 0.9f));
        // 低血红圈脉冲
        if (low) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(t * 6f);
            r.strokeCircle(cx, cy, 62f + pulse * 3f, 3f, Art.a(Palette.RED, 0.35f + 0.4f * pulse));
        }
        // 器官加成小徽章（右上角）
        if (p.armsBonus) {
            Art.discOutlined(r, cx + 44f, cy - 40f, 10f, Art.a(0xFF5A2A6A, 0.95f), 0xFF2A1414, 2f);
            r.setTextSize(12f);
            r.setTextBold(true);
            r.setTextAlign(1);
            r.setTextColor(0xFFE8C4FF);
            r.drawText("臂", cx + 44f, cy - 40f);
            r.setTextBold(false);
        }
        // 头盔/病号标志
        Art.discOutlined(r, cx - 44f, cy + 40f, 10f, 0xFFEEF2F6, 0xFF2A2222, 1.6f);
        r.fillRect(cx - 45.5f, cy + 36f, 3f, 8f, 0xFFD8323C);
        r.fillRect(cx - 48f, cy + 38.5f, 8f, 3f, 0xFFD8323C);
    }

    private void drawBars(Renderer r, World w, HudState hud, float t) {
        Player p = w.player;
        float hpRatio = MathUtil.clamp(p.hp / Math.max(1f, p.maxHp), 0f, 1f);
        float stRatio = MathUtil.clamp(p.stamina / Math.max(1f, p.maxStamina), 0f, 1f);
        boolean low = hpRatio < 0.3f;

        // ---- 生命条 ----
        float bx = 152f, by = 28f, bw = 308f, bh = 22f;
        r.fillRoundRect(bx - 2f, by - 2f, bw + 4f, bh + 4f, 9f, Art.a(0xFF060A0E, 0.85f));
        r.fillRoundRect(bx, by, bw, bh, 8f, 0xFF2A0E0E);
        float fw = bw * hpRatio;
        if (fw > 2f) {
            r.fillRoundRect(bx, by, fw, bh, 8f, 0xFFB71C1C);
            r.fillRoundRect(bx, by + 2f, fw, bh - 8f, 7f, 0xFFE0453A);
            r.fillRoundRect(bx, by + 2f, fw, 5f, 4f, Art.a(0xFFFF8A70, 0.45f));
        }
        // 分节
        for (int i = 1; i < 10; i++) {
            r.fillRect(bx + bw * i / 10f, by + 3f, 1.4f, bh - 6f, Art.a(0xFF1A0606, 0.55f));
        }
        // 低血脉冲
        if (low) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(t * 7f);
            r.fillRoundRect(bx, by, bw, bh, 8f, Art.a(0xFFE0453A, 0.10f + 0.22f * pulse));
            r.strokeRoundRect(bx, by, bw, bh, 8f, 2.6f, Art.a(Palette.RED, 0.4f + 0.5f * pulse));
            Art.discOutlined(r, bx - 14f, by + bh / 2f, 7f + pulse * 1.6f, Palette.RED, 0xFF2A0E0E, 2f);
        } else {
            r.strokeRoundRect(bx, by, bw, bh, 8f, 2f, Art.a(0xFFC9D2DC, 0.75f));
        }
        // 数值
        r.setTextSize(15f);
        r.setTextAlign(2);
        r.setTextColor(Art.a(0xFFFFFFFF, 0.85f));
        r.drawText((int) Math.ceil(p.hp) + " / " + (int) p.maxHp, bx + bw - 10f, by + bh / 2f);

        // ---- 体力条 ----
        float sy = 60f;
        r.fillRoundRect(bx - 2f, sy - 2f, bw + 4f, 14f, 7f, Art.a(0xFF060A0E, 0.8f));
        r.fillRoundRect(bx, sy, bw, 10f, 5f, 0xFF3A2A10);
        if (stRatio > 0.01f) {
            r.fillRoundRect(bx, sy, bw * stRatio, 10f, 5f, 0xFFB8871E);
            r.fillRoundRect(bx, sy, bw * stRatio, 4f, 3f, GOLD);
            r.fillRoundRect(bx, sy + 4f, bw * stRatio, 5f, 3f, 0xFFD89A2A);
        }
        for (int i = 1; i < 10; i++) {
            r.fillRect(bx + bw * i / 10f, sy + 2f, 1.2f, 6f, Art.a(0xFF1A1206, 0.5f));
        }
        if (p.sprinting) {
            r.strokeRoundRect(bx, sy, bw, 10f, 5f, 2f, Art.a(0xFFFFE9B8, 0.5f + 0.3f * (float) Math.sin(t * 20f)));
        }
        // 体力图标
        r.fillTriangle(bx - 16f, sy + 2f, bx - 6f, sy - 2f, bx - 6f, sy + 6f, Art.a(GOLD, 0.95f));
        r.fillTriangle(bx - 16f, sy + 10f, bx - 6f, sy + 6f, bx - 6f, sy + 12f, Art.a(GOLD, 0.7f));

        // ---- 警戒条 ----
        float ay = 82f, aw = 180f;
        r.setTextSize(14f);
        r.setTextAlign(0);
        r.setTextColor(Art.a(0xFF98A3B2, 0.95f));
        r.drawText("警戒", bx, ay + 5f);
        float ax = bx + 44f;
        r.fillRoundRect(ax, ay - 1f, aw, 10f, 5f, Art.a(0xFF0A0E14, 0.85f));
        float al = MathUtil.clamp(w.alert, 0f, 1f);
        int alCol = al < 0.35f ? Palette.MINT : (al < 0.7f ? GOLD : Palette.ALERT);
        if (al > 0.01f) r.fillRoundRect(ax, ay - 1f, aw * al, 10f, 5f, alCol);
        for (int i = 1; i < 5; i++) r.fillRect(ax + aw * i / 5f, ay + 1f, 1.2f, 6f, Art.a(0xFF060A0E, 0.5f));
        r.setTextAlign(0);
        r.setTextSize(14f);
        r.setTextColor(Art.a(alCol, 0.95f));
        String alertLabel = al < 0.35f ? "安全" : (al < 0.7f ? "可疑" : "警报");
        r.drawText(alertLabel, ax + aw + 10f, ay + 5f);
    }

    private void drawWeaponBlock(Renderer r, World w, HudState hud, float t) {
        Player p = w.player;
        // 武器卡片
        r.fillRoundRect(460f, 10f, 80f, 80f, 14f, Art.a(0xFF1C2634, 0.92f));
        r.fillRoundRect(463f, 13f, 74f, 74f, 11f, Art.a(0xFF2A3648, 0.9f));
        r.strokeRoundRect(460f, 10f, 80f, 80f, 14f, 2f, Art.a(0xFF5A6A7C, 0.95f));
        Art.glow(r, 500f, 50f, 38f, Art.HAIR, 0.55f);
        ItemArt.drawWeaponIcon(r, 500f, 48f, 62f, p.weapon);

        int ammo = p.ammo[p.weapon.ordinal()];
        r.setTextAlign(2);
        r.setTextSize(22f);
        r.setTextBold(true);
        String ammoText;
        if (ammo < 0) {
            ammoText = "∞";
            r.setTextColor(Art.a(0xFFE8EEF4, 0.95f));
        } else {
            int max = Balance.weapon(p.weapon).maxAmmo;
            ammoText = ammo + " / " + max;
            r.setTextColor(ammo == 0 ? Palette.RED : (ammo <= max / 4 ? GOLD : Art.a(0xFFE8EEF4, 0.95f)));
        }
        r.drawText(ammoText, 616f, 36f);
        r.setTextBold(false);
        r.setTextSize(17f);
        r.setTextColor(Art.a(0xFFB8C2CE, 0.92f));
        r.drawText(p.weapon.label, 616f, 72f);
        // 楼层
        r.setTextSize(18f);
        r.setTextColor(Art.a(0xFFDDE8EE, 0.92f));
        r.drawText("第 " + w.floor + " 层", 616f, 100f);
        // 钥匙卡
        if (p.hasKeycard) {
            ItemArt.drawItemIcon(r, 430f, 100f, 30f, ItemType.KEYCARD);
        }
    }

    // ==================== 小地图 ====================

    private void drawMinimap(Renderer r, World w, HudState hud) {
        if (w.level.bossLevel) return;   // BOSS 层不显示小地图，保持压迫感
        float mx = 540f, my = 180f, mw = 164f, mh = 142f;
        r.fillRoundRect(mx, my, mw, mh, 12f, Art.a(0xFF0A0F16, 0.58f));
        r.strokeRoundRect(mx, my, mw, mh, 12f, 1.6f, Art.a(0xFF7A8798, 0.5f));
        r.fillRoundRect(mx + 4f, my + 4f, mw - 8f, 20f, 8f, Art.a(0xFF141C26, 0.75f));
        r.setTextSize(12f);
        r.setTextAlign(0);
        r.setTextColor(Art.a(0xFF8A96A6, 0.85f));
        r.drawText("雷达 / " + w.level.name, mx + 10f, my + 14f);

        Mini mini = mini(w.map);
        float pad = 9f;
        float availW = mw - pad * 2f, availH = mh - 34f;
        float cell = Math.min(availW / mini.w, availH / mini.h);
        float ox = mx + (mw - mini.w * cell) * 0.5f;
        float oy = my + 28f + (availH - mini.h * cell) * 0.5f;
        // 房间底
        r.fillRoundRect(ox, oy, mini.w * cell, mini.h * cell, 3f, Art.a(0xFF243244, 0.85f));
        // 墙边界
        int wallColor = Art.a(0xFF9AAFC8, 0.68f);
        for (int i = 0; i < mini.walls.length; i += 2) {
            float tx = mini.walls[i], ty = mini.walls[i + 1];
            r.fillRect(ox + tx * cell, oy + ty * cell, cell + 0.6f, cell + 0.6f, wallColor);
        }
        // 电梯 / 电脑
        if (mini.elevator != null) {
            r.fillRect(ox + mini.elevator[0] * cell, oy + mini.elevator[1] * cell, cell + 0.8f, cell + 0.8f, Palette.MINT);
        }
        for (int i = 0; i < mini.computer.length; i += 2) {
            r.fillCircle(ox + (mini.computer[i] + 0.5f) * cell, oy + (mini.computer[i + 1] + 0.5f) * cell,
                    Math.max(1.6f, cell * 0.42f), w.camerasDisabled ? Art.a(Palette.MINT, 0.9f) : Palette.CYAN);
        }
        // 拾取物标记（武器/钥匙/器官）
        for (Pickup pk : w.pickups) {
            if (pk.taken) continue;
            if (pk.kind != Pickup.Kind.KEYCARD && pk.kind != Pickup.Kind.ORGAN && pk.kind != Pickup.Kind.WEAPON) continue;
            int col = pk.kind == Pickup.Kind.KEYCARD ? GOLD : (pk.kind == Pickup.Kind.ORGAN ? ItemArt.ORGAN_PURPLE : Art.a(0xFFE8EEF4, 0.8f));
            float dx = ox + (pk.x / TileMap.TILE) * cell;
            float dy = oy + (pk.y / TileMap.TILE) * cell;
            r.fillRect(dx - 1.2f, dy - 1.2f, 2.4f, 2.4f, col);
        }
        // 敌人点
        for (Enemy e : w.enemies) {
            if (e.isDead()) continue;
            float dx = ox + (e.x / TileMap.TILE) * cell;
            float dy = oy + (e.y / TileMap.TILE) * cell;
            int col = e.type == EnemyType.BOSS ? GOLD
                    : (e.state == com.crazyclinic.core.world.EnemyState.CHASE ? Palette.ALERT : Palette.RED);
            r.fillCircle(dx, dy, e.type == EnemyType.BOSS ? 3.4f : 2.2f, col);
        }
        // 玩家
        Player p = w.player;
        float px = ox + (p.x / TileMap.TILE) * cell;
        float py = oy + (p.y / TileMap.TILE) * cell;
        r.save();
        r.translate(px, py);
        r.rotate((float) Math.toDegrees(p.angle));
        r.fillTriangle(6.5f, 0f, -4f, -4.2f, -4f, 4.2f, Art.a(0xFF9AD8F0, 0.95f));
        r.restore();
        r.fillCircle(px, py, 1.6f, Palette.WHITE);
        // 扫描线
        float scan = (w.time * 40f) % mh;
        r.fillRect(mx + 4f, my + scan, mw - 8f, 1.2f, Art.a(Palette.CYAN, 0.10f));
    }

    private static Mini mini(TileMap map) {
        Mini m = MINI.get(map);
        if (m != null) return m;
        m = new Mini();
        m.w = map.width;
        m.h = map.height;
        int count = 0;
        for (int ty = 0; ty < map.height; ty++) {
            for (int tx = 0; tx < map.width; tx++) {
                if (isMinimapWall(map, tx, ty)) count++;
            }
        }
        m.walls = new float[count * 2];
        int k = 0;
        int compCount = 0;
        float[] elev = null;
        for (int ty = 0; ty < map.height; ty++) {
            for (int tx = 0; tx < map.width; tx++) {
                TileType t = map.type(tx, ty);
                if (isMinimapWall(map, tx, ty)) {
                    m.walls[k++] = tx;
                    m.walls[k++] = ty;
                }
                if (t == TileType.ELEVATOR) elev = new float[]{tx, ty};
                if (t == TileType.COMPUTER) compCount++;
            }
        }
        m.elevator = elev;
        m.computer = new float[compCount * 2];
        k = 0;
        for (int ty = 0; ty < map.height; ty++) {
            for (int tx = 0; tx < map.width; tx++) {
                if (map.type(tx, ty) == TileType.COMPUTER) {
                    m.computer[k++] = tx;
                    m.computer[k++] = ty;
                }
            }
        }
        MINI.put(map, m);
        return m;
    }

    /** 只画与可走区域相邻的墙，小地图才干净。 */
    private static boolean isMinimapWall(TileMap map, int tx, int ty) {
        if (!map.solid(tx, ty)) return false;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                if (!map.solid(tx + dx, ty + dy)) return true;
            }
        }
        return false;
    }

    // ==================== 世界层覆盖 ====================

    private void drawWorldOverlays(Renderer r, World w, HudState hud) {
        Player p = w.player;
        float px = p.x - w.cameraX;
        float py = p.y - w.cameraY;
        // 瞄准辅助虚线
        float aa = p.aimAngle;
        for (int i = 1; i <= 3; i++) {
            float d = 62f + i * 46f;
            float dx = px + (float) Math.cos(aa) * d;
            float dy = py - 30f + (float) Math.sin(aa) * d;
            if (dx < 0 || dx > 720 || dy < 0 || dy > World.VIEW_H) break;
            r.fillCircle(dx, dy, 2.6f - i * 0.4f, Art.a(Palette.WHITE, 0.28f - i * 0.06f));
        }

        // 交互提示气泡
        if (hud.interactPrompt != null && !hud.interactPrompt.isEmpty()) {
            float bx = MathUtil.clamp(px, 150f, 570f);
            float by = MathUtil.clamp(py - 118f, 190f, World.VIEW_H - 120f);
            float ps = 20f;
            float pw = textW(r, hud.interactPrompt, ps);
            if (pw > 300f) { ps = Math.max(14f, ps * 300f / pw); pw = textW(r, hud.interactPrompt, ps); }
            float w0 = MathUtil.clamp(pw + 46f, 150f, 360f);
            float pulse = 0.5f + 0.5f * (float) Math.sin(w.time * 5f);
            r.fillRoundRect(bx - w0 / 2f, by - 22f, w0, 44f, 12f, Art.a(0xFF0C1218, 0.88f));
            r.strokeRoundRect(bx - w0 / 2f, by - 22f, w0, 44f, 12f, 2f,
                    Art.a(hud.interactAvailable ? Palette.MINT : Palette.PANEL_STROKE, 0.75f + 0.25f * pulse));
            // 小尖角
            r.fillTriangle(bx - 9f, by + 21f, bx + 9f, by + 21f, bx, by + 32f, Art.a(0xFF0C1218, 0.9f));
            // B 键徽标
            Art.discOutlined(r, bx - w0 / 2f + 18f, by, 13f, Art.a(0xFF2E6A72, 1f), 0xFF8A96A6, 1.6f);
            r.setTextSize(15f);
            r.setTextBold(true);
            r.setTextAlign(1);
            r.setTextColor(Art.a(0xFFDDF4F2, 1f));
            r.drawText("B", bx - w0 / 2f + 18f, by);
            r.setTextBold(false);
            r.setTextSize(ps);
            r.setTextAlign(0);
            r.setTextColor(Art.a(0xFFE8F4F2, 0.98f));
            r.drawText(hud.interactPrompt, bx - w0 / 2f + 36f, by);
        }

        // Toast（世界视口底部；超长自动缩字号 + 最多两行居中）
        if (hud.toast != null && hud.toastTimer > 0f) {
            float a = MathUtil.clamp(hud.toastTimer * 2.2f, 0f, 1f);
            String text = hud.toast;
            float ts = 23f;
            String line1 = text;
            String line2 = null;
            float txtW = textW(r, text, ts);
            if (txtW > 560f) {
                // 按显示宽度折成两行（在中间附近找断点，优先标点/空格）
                int best = -1;
                float bestScore = Float.MAX_VALUE;
                float half = txtW * 0.5f;
                float acc = 0f;
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (i > 0) {
                        float score = Math.abs(acc - half);
                        if ((c == '，' || c == '。' || c == '、' || c == '；' || c == '：' || c == ' '
                                || c == '！' || c == '？' || c == '/') && score < bestScore) {
                            bestScore = score;
                            best = i + 1;
                        }
                    }
                    acc = textW(r, text.substring(0, i + 1), ts);
                }
                if (best < 0) {
                    for (int i = 1; i < text.length(); i++) {
                        float score = Math.abs(textW(r, text.substring(0, i), ts) - half);
                        if (score < bestScore) { bestScore = score; best = i; }
                    }
                }
                if (best > 0 && best < text.length()) {
                    line1 = text.substring(0, best);
                    line2 = text.substring(best);
                }
                // 仍然过宽则缩字号
                float wMax = Math.max(textW(r, line1, ts), line2 == null ? 0f : textW(r, line2, ts));
                if (wMax > 560f) {
                    ts = Math.max(16f, ts * 560f / wMax);
                    wMax = Math.max(textW(r, line1, ts), line2 == null ? 0f : textW(r, line2, ts));
                }
                txtW = wMax;
            }
            float lines = line2 == null ? 1f : 2f;
            float th = lines == 1f ? 50f : 78f;
            float baseY = hud.bossHp > 0f ? 742f : 832f;   // 有 BOSS 血条时上移，避免重叠
            float ty = baseY - (lines - 1f) * 14f;
            float tw = MathUtil.clamp(txtW + 84f, 260f, 640f);
            r.fillRoundRect(360f - tw / 2f, ty, tw, th, 14f, Art.a(0xFF10161E, 0.9f * a));
            r.strokeRoundRect(360f - tw / 2f, ty, tw, th, 14f, 1.6f, Art.a(0xFF5A6472, 0.7f * a));
            r.fillRoundRect(360f - tw / 2f, ty, 5f, th, 2.5f, Art.a(GOLD, 0.9f * a));
            r.fillTriangle(360f - tw / 2f + 22f, ty + th * 0.5f - 6f, 360f - tw / 2f + 32f, ty + th * 0.5f,
                    360f - tw / 2f + 22f, ty + th * 0.5f + 6f, Art.a(GOLD, 0.95f * a));
            r.setTextSize(ts);
            r.setTextAlign(1);
            r.setTextColor(Art.a(0xFFF0F4F8, a));
            if (line2 == null) {
                r.drawText(line1, 360f + 8f, ty + th * 0.5f);
            } else {
                r.drawText(line1, 360f + 8f, ty + 24f);
                r.drawText(line2, 360f + 8f, ty + 54f);
            }
        }
    }

    // ==================== BOSS 血条 ====================

    private void drawBossBar(Renderer r, World w, HudState hud) {
        if (hud.bossHp <= 0f) return;
        float ratio = MathUtil.clamp(hud.bossHp / Math.max(1f, hud.bossMaxHp), 0f, 1f);
        int phase = ratio > 0.66f ? 1 : (ratio > 0.33f ? 2 : 3);
        float bx = 96f, by = 902f, bw = 528f, bh = 22f;
        float t = w.time;
        int mainCol = phase == 1 ? 0xFFB84AC0 : (phase == 2 ? 0xFFD8644A : 0xFFE0392A);
        int darkCol = phase == 1 ? 0xFF7E2A8A : (phase == 2 ? 0xFF8A3418 : 0xFF8A1414);
        // 名牌
        String name = hud.bossName == null ? "赞恩" : hud.bossName;
        String plate = "BOSS · " + name + "  " + romanPhase(phase);
        r.fillRoundRect(360f - 132f, 868f, 264f, 28f, 9f, Art.a(0xFF1A1020, 0.88f));
        r.strokeRoundRect(360f - 132f, 868f, 264f, 28f, 9f, 2f, Art.a(GOLD, 0.8f));
        r.setTextSize(19f);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(GOLD);
        r.drawText(plate, 360f, 882f);
        r.setTextBold(false);
        // 头骨徽标
        Art.discOutlined(r, 70f, by + bh / 2f, 20f, Art.a(0xFF241A2E, 0.95f), Art.a(GOLD, 0.7f), 2f);
        Art.discOutlined(r, 70f, by + bh / 2f - 2f, 12f, 0xFFE8E4DC, 0xFF2A2222, 2f);
        r.fillCircle(66f, by + bh / 2f - 4f, 3.2f, 0xFF2A2222);
        r.fillCircle(74f, by + bh / 2f - 4f, 3.2f, 0xFF2A2222);
        r.fillTriangle(67f, by + bh / 2f + 4f, 73f, by + bh / 2f + 4f, 70f, by + bh / 2f + 8f, 0xFF2A2222);
        // 血条
        r.fillRoundRect(bx - 2f, by - 2f, bw + 4f, bh + 4f, 10f, Art.a(0xFF06080C, 0.9f));
        r.fillRoundRect(bx, by, bw, bh, 9f, Art.a(darkCol, 0.6f));
        if (ratio > 0.005f) {
            r.fillRoundRect(bx, by, bw * ratio, bh, 9f, darkCol);
            r.fillRoundRect(bx, by + 2f, bw * ratio, bh - 8f, 7f, mainCol);
            r.fillRoundRect(bx, by + 2f, bw * ratio, 4f, 3f, Art.a(0xFFFFD0F8, 0.5f));
        }
        // 分节 + 边框
        for (int i = 1; i < 8; i++) r.fillRect(bx + bw * i / 8f, by + 3f, 1.6f, bh - 6f, Art.a(0xFF1A0A1E, 0.6f));
        r.strokeRoundRect(bx, by, bw, bh, 9f, 2.2f, Art.a(GOLD, 0.85f));
        // 三阶段狂暴闪光
        if (phase == 3) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(t * 8f);
            r.strokeRoundRect(bx - 2f, by - 2f, bw + 4f, bh + 4f, 10f, 2.4f, Art.a(Palette.RED, 0.3f + 0.5f * pulse));
            r.fillRect(bx, by + bh + 3f, bw * pulse, 3f, Art.a(Palette.RED, 0.5f));
        }
        r.setTextSize(14f);
        r.setTextAlign(2);
        r.setTextColor(Art.a(0xFFF0E0F4, 0.9f));
        r.drawText((int) Math.ceil(hud.bossHp) + " / " + (int) hud.bossMaxHp, bx + bw - 8f, by + bh / 2f);
    }

    private static String romanPhase(int phase) {
        return phase == 1 ? "· 一阶段" : (phase == 2 ? "· 二阶段" : "· 最终阶段");
    }

    // ==================== 控制区 ====================

    private void drawControls(Renderer r, World w, HudState hud) {
        float top = World.VIEW_H;
        r.fillVerticalGradient(0f, top, 720f, 320f, Art.a(0xFF0A0E14, 0.93f), Art.a(0xFF05080C, 0.97f));
        r.fillRect(0f, top, 720f, 3f, Art.a(0xFF39434F, 0.85f));
        // 顶部警示色带
        for (int i = 0; i < 24; i++) {
            r.fillRect(i * 30f, top + 3f, 15f, 3f, Art.a(Art.HAZARD, 0.12f));
        }

        Player p = w.player;

        // 左摇杆（动态位置）
        float mcx = hud.moveStickActive ? hud.moveStickCx : Controls.MOVE_DEFAULT_X;
        float mcy = hud.moveStickActive ? hud.moveStickCy : Controls.MOVE_DEFAULT_Y;
        drawMoveStick(r, mcx, mcy, hud, w.time);

        // 瞄准摇杆（仅拖动时显示，压在攻击按钮下方层级）
        if (hud.aimStickActive) {
            drawAimStick(r, hud.aimStickCx, hud.aimStickCy, hud);
        } else {
            // 未拖动时，在攻击键左侧留一个很淡的准星提示
            r.strokeCircle(Controls.AIM_DEFAULT_X - 116f, Controls.AIM_DEFAULT_Y + 40f, 22f, 1.4f,
                    Art.a(0xFF8A96A6, 0.16f));
            r.line(Controls.AIM_DEFAULT_X - 126f, Controls.AIM_DEFAULT_Y + 40f,
                    Controls.AIM_DEFAULT_X - 106f, Controls.AIM_DEFAULT_Y + 40f, 1.4f, Art.a(0xFF8A96A6, 0.16f));
            r.line(Controls.AIM_DEFAULT_X - 116f, Controls.AIM_DEFAULT_Y + 30f,
                    Controls.AIM_DEFAULT_X - 116f, Controls.AIM_DEFAULT_Y + 50f, 1.4f, Art.a(0xFF8A96A6, 0.16f));
            r.setTextSize(13f);
            r.setTextAlign(1);
            r.setTextColor(Art.a(0xFF8A96A6, 0.4f));
            r.drawText("右半屏拖动瞄准", Controls.AIM_DEFAULT_X - 116f, Controls.AIM_DEFAULT_Y + 74f);
        }

        // 换武器 / 道具胶囊
        drawCapsule(r, Controls.WEAPON_X, Controls.WEAPON_Y, Controls.WEAPON_W, Controls.WEAPON_H,
                hud.weaponBtn, "换武器", true, p.weapon, null, 0);
        drawCapsule(r, Controls.ITEM_X, Controls.ITEM_Y, Controls.ITEM_W, Controls.ITEM_H,
                hud.itemBtn, "道具", false, null, p.item, p.itemCount);

        // 攻击按钮
        drawAttackButton(r, hud, p, w.time);

        // 互动 / 闪避
        drawInteractButton(r, hud, w.time);
        drawDodgeButton(r, hud, p, w.time);

        // 底部缓慢滚动的装饰线
        r.fillRect(0f, 1274f, 720f, 2f, Art.a(0xFF39434F, 0.4f));
    }

    private void drawMoveStick(Renderer r, float cx, float cy, HudState hud, float t) {
        boolean active = hud.moveStickActive;
        int base = Art.a(active ? 0xFF141C26 : 0xFF101620, active ? 0.62f : 0.42f);
        r.fillCircle(cx, cy, Controls.STICK_RADIUS * 0.92f, base);
        r.strokeCircle(cx, cy, Controls.STICK_RADIUS * 0.92f, 2.6f,
                Art.a(0xFF9AAFC8, active ? 0.55f : 0.28f));
        r.strokeCircle(cx, cy, Controls.STICK_RADIUS * 0.60f, 1.6f,
                Art.a(0xFF9AAFC8, active ? 0.30f : 0.14f));
        // 8 向刻度
        for (int i = 0; i < 8; i++) {
            float a = i * MathUtil.TAU / 8f;
            float r0 = Controls.STICK_RADIUS * 0.78f;
            float r1 = Controls.STICK_RADIUS * 0.88f;
            r.line(cx + (float) Math.cos(a) * r0, cy + (float) Math.sin(a) * r0,
                    cx + (float) Math.cos(a) * r1, cy + (float) Math.sin(a) * r1,
                    2.4f, Art.a(0xFF9AAFC8, active ? 0.42f : 0.20f));
        }
        float dx = hud.moveStickDx, dy = hud.moveStickDy;
        float mag = Math.min(1f, (float) Math.sqrt(dx * dx + dy * dy));
        float travel = Controls.STICK_RADIUS * 0.58f * mag;
        float kx = cx + dx * travel;
        float ky = cy + dy * travel;
        // 摇杆头
        float kr = Controls.STICK_KNOB;
        Art.discOutlined(r, kx, ky, kr, Art.a(active ? 0xFF2E3A48 : 0xFF202A36, 0.95f), 0xFF0A0E14, 3f);
        r.fillCircle(kx, ky, kr - 6f, Art.a(active ? 0xFFDDE8F0 : 0xFFA8B8C8, active ? 0.95f : 0.8f));
        r.fillCircle(kx - kr * 0.28f, ky - kr * 0.30f, kr * 0.42f, Art.a(0xFFFFFFFF, active ? 0.35f : 0.22f));
        // 摇杆头四向箭头
        int arrowCol = Art.a(active ? 0xFF2E3A48 : 0xFF8A96A6, active ? 0.75f : 0.55f);
        r.fillTriangle(kx - 7f, ky - kr * 0.42f, kx + 7f, ky - kr * 0.42f, kx, ky - kr * 0.62f, arrowCol);
        r.fillTriangle(kx - 7f, ky + kr * 0.42f, kx + 7f, ky + kr * 0.42f, kx, ky + kr * 0.62f, arrowCol);
        r.fillTriangle(kx - kr * 0.42f, ky - 7f, kx - kr * 0.42f, ky + 7f, kx - kr * 0.62f, ky, arrowCol);
        r.fillTriangle(kx + kr * 0.42f, ky - 7f, kx + kr * 0.42f, ky + 7f, kx + kr * 0.62f, ky, arrowCol);
        // 摇杆头方向指示
        if (mag > 0.15f) {
            r.line(kx, ky, kx + dx * kr * 0.55f, ky + dy * kr * 0.55f, 3f, Art.a(0xFF2E3A48, 0.6f));
        }
        // 冲刺反馈
        if (mag > 0.85f) {
            r.strokeCircle(cx, cy, Controls.STICK_RADIUS * 0.92f, 3f,
                    Art.a(GOLD, 0.35f + 0.25f * (float) Math.sin(t * 16f)));
        }
        // 冲刺提示（只在推满时出现）
        if (mag > 0.85f) {
            r.setTextSize(15f);
            r.setTextAlign(1);
            r.setTextBold(true);
            r.setTextColor(Art.a(GOLD, 0.9f));
            r.drawText("冲刺", cx, cy - Controls.STICK_RADIUS * 0.92f - 10f);
            r.setTextBold(false);
        }
    }

    private void drawAimStick(Renderer r, float cx, float cy, HudState hud) {
        r.fillCircle(cx, cy, 92f, Art.a(0xFF101A24, 0.42f));
        r.strokeCircle(cx, cy, 92f, 2f, Art.a(Palette.CYAN, 0.45f));
        float dx = hud.aimStickDx, dy = hud.aimStickDy;
        float mag = Math.min(1f, (float) Math.sqrt(dx * dx + dy * dy));
        r.fillCircle(cx + dx * 50f, cy + dy * 50f, 34f, Art.a(0xFF2E5A70, 0.85f));
        r.fillCircle(cx + dx * 50f, cy + dy * 50f, 28f, Art.a(Palette.CYAN, 0.9f));
        r.fillCircle(cx + dx * 50f - 8f, cy + dy * 50f - 9f, 10f, Art.a(0xFFFFFFFF, 0.35f));
        if (mag > 0.55f) {
            r.strokeCircle(cx, cy, 92f + 4f * mag, 3f, Art.a(Palette.GOLD, 0.5f));
        }
    }

    private void drawAttackButton(Renderer r, HudState hud, Player p, float t) {
        float press = MathUtil.clamp(hud.attackBtn, 0f, 1f);
        float r0 = Controls.ATTACK_R * (1f - 0.06f * press);
        float cx = Controls.ATTACK_X, cy = Controls.ATTACK_Y;
        // 外光晕
        Art.glow(r, cx, cy, r0 + 16f, Palette.RED, hud.attackHeld ? 1.2f : 0.5f);
        r.fillCircle(cx, cy, r0 + 4f, Art.a(0xFF2A0A08, 0.85f));
        r.fillCircle(cx, cy, r0, Art.a(0xFF7E1414, 1f));
        r.fillCircle(cx, cy, r0 - 10f, Art.a(0xFFC22A20, 1f));
        r.fillCircle(cx, cy, r0 - 22f, Art.a(0xFFE04B3A, 1f));
        Art.ellipse(r, cx - r0 * 0.22f, cy - r0 * 0.42f, r0 * 0.56f, r0 * 0.30f, Art.a(0xFFFF9A82, 0.30f));
        Art.ellipse(r, cx, cy + r0 * 0.40f, r0 * 0.62f, r0 * 0.26f, Art.a(0xFF8E1410, 0.35f));
        if (press > 0.05f) {
            r.fillCircle(cx, cy, r0 - 6f, Art.a(0xFFFFFFFF, 0.10f * press));
        }
        r.strokeCircle(cx, cy, r0 + 2f, 3f, Art.a(0xFFFFB4A0, 0.55f));
        // 武器剪影（白色）
        r.save();
        r.translate(cx - 4f, cy - 8f);
        r.rotate(-24f);
        r.scale(1.05f, 1.05f);
        r.setAlpha(0.95f);
        ItemArt.drawWeaponHeld(r, p.weapon, 1.15f, false);
        r.restore();
        r.setAlpha(1f);
        // 标签
        r.setTextSize(21f);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(Art.a(0xFFFFFFFF, 0.92f));
        r.drawText("攻击", cx, cy + r0 * 0.48f);
        r.setTextBold(false);
        if (hud.attackHeld) {
            r.strokeCircle(cx, cy, r0 + 6f + 4f * (float) Math.sin(t * 18f), 2f, Art.a(0xFFFFE9B8, 0.55f));
        }
    }

    private void drawInteractButton(Renderer r, HudState hud, float t) {
        boolean avail = hud.interactAvailable;
        float press = MathUtil.clamp(hud.interactBtn, 0f, 1f);
        float r0 = Controls.INTERACT_R * (1f - 0.06f * press);
        float cx = Controls.INTERACT_X, cy = Controls.INTERACT_Y;
        int fill = avail ? 0xFF2E7A80 : 0xFF2A3440;
        if (avail) {
            Art.glow(r, cx, cy, r0 + 12f, Palette.CYAN, 0.7f + 0.4f * (float) Math.sin(t * 5f));
            r.strokeCircle(cx, cy, r0 + 6f + 2.5f * (float) Math.sin(t * 5f), 2f, Art.a(Palette.CYAN, 0.5f));
        }
        r.fillCircle(cx, cy, r0 + 3f, Art.a(0xFF061014, 0.85f));
        r.fillCircle(cx, cy, r0, Art.a(fill, avail ? 1f : 0.65f));
        r.fillCircle(cx, cy, r0 - 9f, Art.a(avail ? 0xFF4FB3BF : 0xFF39434F, 1f));
        r.fillCircle(cx, cy, r0 - 18f, Art.a(avail ? 0xFF6FD3DC : 0xFF4A5563, 1f));
        // 张开的手
        r.fillRoundRect(cx - 13f, cy - 8f, 26f, 21f, 9f, Art.a(0xFFEAF6F8, 0.95f));
        for (int i = 0; i < 4; i++) {
            float fx = cx - 13f + i * 6.6f;
            float fh = i == 1 ? 20f : (i == 2 ? 18f : 15f);
            r.fillRoundRect(fx, cy - 8f - fh, 5.2f, fh + 6f, 2.6f, Art.a(0xFFEAF6F8, 0.92f));
        }
        r.fillRoundRect(cx - 20f, cy - 6f, 7f, 15f, 3.5f, Art.a(0xFFEAF6F8, 0.85f));
        r.fillRect(cx - 13f, cy + 3f, 26f, 2.5f, Art.a(0xFFB8D8DC, 0.6f));
        r.setTextSize(17f);
        r.setTextAlign(1);
        r.setTextColor(Art.a(0xFFFFFFFF, avail ? 0.95f : 0.5f));
        r.drawText("互动", cx, cy + r0 - 13f);
        if (avail) {
            // 上方提示
            float by = cy - r0 - 18f;
            r.fillTriangle(cx - 7f, by + 8f, cx + 7f, by + 8f, cx, by, Art.a(Palette.CYAN, 0.8f));
        }
    }

    private void drawDodgeButton(Renderer r, HudState hud, Player p, float t) {
        float press = MathUtil.clamp(hud.dodgeBtn, 0f, 1f);
        float r0 = Controls.DODGE_R * (1f - 0.06f * press);
        float cx = Controls.DODGE_X, cy = Controls.DODGE_Y;
        float cd = MathUtil.clamp(p.dodgeCd / Balance.DODGE_CD, 0f, 1f);
        boolean ready = cd <= 0.01f && p.stamina >= Balance.DODGE_STAMINA;
        int fill = ready ? 0xFF2E5A9A : 0xFF2A3440;
        r.fillCircle(cx, cy, r0 + 3f, Art.a(0xFF061014, 0.85f));
        r.fillCircle(cx, cy, r0, Art.a(fill, ready ? 1f : 0.7f));
        r.fillCircle(cx, cy, r0 - 9f, Art.a(ready ? 0xFF3A70C0 : 0xFF39434F, 1f));
        r.fillCircle(cx, cy, r0 - 18f, Art.a(ready ? 0xFF4A8AE0 : 0xFF4A5563, 1f));
        // 冷却扇形
        if (cd > 0.01f) {
            Art.fan(r, cx, cy, r0 - 4f, -MathUtil.PI / 2f, MathUtil.TAU * cd, Art.a(0xFF05080C, 0.62f));
        }
        // 闪避图标：双向箭头
        r.fillTriangle(cx - 2f, cy - 16f, cx + 16f, cy - 8f, cx - 2f, cy, Art.a(0xFFEAF6F8, 0.95f));
        r.fillTriangle(cx + 2f, cy + 2f, cx - 16f, cy + 10f, cx + 2f, cy + 16f, Art.a(0xFFEAF6F8, 0.85f));
        r.setTextSize(17f);
        r.setTextAlign(1);
        r.setTextColor(Art.a(0xFFFFFFFF, ready ? 0.95f : 0.5f));
        r.drawText("闪避", cx, cy + r0 - 13f);
        if (ready) {
            r.strokeCircle(cx, cy, r0 + 5f, 1.8f, Art.a(0xFF9AC6F0, 0.35f + 0.15f * (float) Math.sin(t * 4f)));
        }
    }

    private void drawCapsule(Renderer r, float x, float y, float w, float h, float press,
                             String label, boolean weapon, WeaponType wt, ItemType it, int count) {
        float p = MathUtil.clamp(press, 0f, 1f);
        float dy = 3f * p;
        int fill = 0xFF1A2230;
        r.fillRoundRect(x, y + dy, w, h, 13f, Art.a(0xFF06090E, 0.8f));
        r.fillRoundRect(x, y + dy, w, h - 2f, 13f, Art.a(fill, 0.95f));
        r.strokeRoundRect(x, y + dy, w, h - 2f, 13f, 2f, Art.a(0xFF39434F, 0.95f));
        // 图标区
        if (weapon) {
            ItemArt.drawWeaponIcon(r, x + 32f, y + h / 2f + dy, 38f, wt);
        } else if (it != null) {
            ItemArt.drawItemIcon(r, x + 32f, y + h / 2f + dy, 34f, it);
        } else {
            r.strokeCircle(x + 32f, y + h / 2f + dy, 15f, 1.6f, Art.a(0xFF5A6472, 0.8f));
            r.line(x + 24f, y + h / 2f + dy, x + 40f, y + h / 2f + dy, 1.6f, Art.a(0xFF5A6472, 0.8f));
        }
        // 文本
        r.setTextSize(17f);
        r.setTextAlign(0);
        r.setTextColor(Art.a(0xFFDDE8EE, 0.92f));
        r.drawText(label, x + 58f, y + h / 2f + dy - 6f);
        r.setTextSize(12f);
        r.setTextColor(Art.a(0xFF8A96A6, 0.9f));
        if (weapon) {
            r.drawText("< >", x + 58f, y + h / 2f + dy + 13f);
        } else if (it != null) {
            r.drawText("x" + count, x + 58f, y + h / 2f + dy + 13f);
        } else {
            r.drawText("空", x + 58f, y + h / 2f + dy + 13f);
        }
        // 数量徽标
        if (!weapon && it != null && count > 1) {
            Art.discOutlined(r, x + w - 16f, y + 14f + dy, 10f, 0xFFE1C424, 0xFF2A1A0A, 1.6f);
            r.setTextSize(12f);
            r.setTextBold(true);
            r.setTextAlign(1);
            r.setTextColor(0xFF2A1A0A);
            r.drawText(String.valueOf(Math.min(99, count)), x + w - 16f, y + 14f + dy);
            r.setTextBold(false);
        }
    }

    // ==================== 屏幕反馈 ====================

    private void drawScreenFx(Renderer r, World w, HudState hud) {
        Player p = w.player;
        float low = MathUtil.clamp(hud.lowHealth, 0f, 1f);
        if (low > 0f && low < 0.30f) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(w.time * 6.5f);
            float a = (1f - low / 0.30f) * (0.10f + 0.10f * pulse);
            r.fillRadialGradient(360f, 480f, 700f, Art.a(Palette.RED, 0f), Art.a(Palette.RED, a));
        }
        if (hud.damageFlash > 0.01f) {
            float a = MathUtil.clamp(hud.damageFlash, 0f, 1f);
            r.fillRect(0f, 0f, 720f, 1280f, Art.a(Palette.RED, 0.32f * a));
            r.fillRadialGradient(360f, 480f, 700f, Art.a(Palette.RED_DARK, 0f), Art.a(Palette.RED_DARK, 0.52f * a));
            // 受伤边缘红框
            r.fillVerticalGradient(0f, 0f, 720f, 40f, Art.a(Palette.RED_DARK, 0.5f * a), Art.a(Palette.RED_DARK, 0f));
            r.fillVerticalGradient(0f, 1240f, 720f, 40f, Art.a(Palette.RED_DARK, 0f), Art.a(Palette.RED_DARK, 0.5f * a));
        }
        if (hud.healFlash > 0.01f) {
            float a = MathUtil.clamp(hud.healFlash, 0f, 1f);
            r.fillRect(0f, 0f, 720f, 1280f, Art.a(Palette.GREEN, 0.12f * a));
        }
    }

    // ==================== 过关动画 ====================

    private void drawFloorClear(Renderer r, World w, HudState hud) {
        float t = MathUtil.clamp(hud.floorClearTimer, 0f, 1.8f);
        float phase = MathUtil.easeInOutQuad(t / 1.35f);
        float doorW = 380f * phase;
        // 电梯门（金属双开）
        drawDoorPanel(r, 0f, doorW, false);
        drawDoorPanel(r, 720f - doorW, doorW, true);
        // 门缝白光
        if (doorW > 40f) {
            float gapA = 0.5f + 0.5f * phase;
            r.fillVerticalGradient(360f - 12f, 0f, 24f, 1280f,
                    Art.a(0xFFFFFFFF, 0.85f * gapA), Art.a(0xFFBFE0FF, 0.55f * gapA));
            Art.glow(r, 360f, 480f, 220f, 0xFFBFE0FF, 1.4f * gapA);
        }
        // 文案
        float ta = MathUtil.clamp(1f - Math.abs(phase - 0.55f) * 2.5f, 0f, 1f);
        if (ta > 0.01f) {
            r.setTextSize(58f);
            r.setTextBold(true);
            r.setTextAlign(1);
            r.setTextColor(Art.a(0xFF0A1220, 0.55f * ta));
            r.drawText("本层通过", 362f, 474f);
            r.setTextColor(Art.a(0xFFFFFFFF, ta));
            r.drawText("本层通过", 360f, 470f);
            r.setTextBold(false);
            r.setTextSize(24f);
            r.setTextColor(Art.a(0xFFE8F4FF, 0.9f * ta));
            r.drawText("第 " + w.floor + " 层 · " + w.level.name, 360f, 528f);
            r.setTextSize(18f);
            r.setTextColor(Art.a(0xFFBFD0E0, 0.75f * ta));
            r.drawText("电梯门正在关闭……", 360f, 566f);
        }
        // 结束白场
        float wh = MathUtil.clamp((t - 1.25f) / 0.55f, 0f, 1f);
        if (wh > 0f) {
            r.fillRect(0f, 0f, 720f, 1280f, Art.a(0xFFFFFFFF, wh));
        }
    }

    private void drawDoorPanel(Renderer r, float x, float w, boolean right) {
        if (w <= 0f) return;
        r.fillVerticalGradient(x, 0f, w, 1280f, 0xFF67748A, 0xFF39424E);
        // 外框
        r.fillRect(x, 0f, w, 18f, Art.a(0xFF20262E, 0.85f));
        r.fillRect(x, 1262f, w, 18f, Art.a(0xFF20262E, 0.85f));
        // 拉丝纹理（少量）
        for (int i = 0; i < 7; i++) {
            float lx = x + 16f + i * (w / 7f);
            r.fillRect(lx, 20f, 1.4f, 1240f, Art.a(0xFF2A323C, 0.28f));
        }
        // 内嵌面板
        float inset = right ? 18f : 14f;
        r.fillRoundRect(x + inset, 110f, Math.max(0f, w - 34f), 508f, 6f, Art.a(0xFF4E5A6A, 0.55f));
        r.strokeRoundRect(x + inset, 110f, Math.max(0f, w - 34f), 508f, 6f, 2f, Art.a(0xFF2A323C, 0.7f));
        r.fillRoundRect(x + inset, 680f, Math.max(0f, w - 34f), 440f, 6f, Art.a(0xFF4E5A6A, 0.55f));
        r.strokeRoundRect(x + inset, 680f, Math.max(0f, w - 34f), 440f, 6f, 2f, Art.a(0xFF2A323C, 0.7f));
        // 边缘高光/阴影
        if (right) {
            r.fillRect(x + w - 6f, 0f, 6f, 1280f, Art.a(0xFFDDE8F0, 0.35f));
            r.fillRect(x, 0f, 3f, 1280f, Art.a(0xFF1A2028, 0.8f));
        } else {
            r.fillRect(x, 0f, 6f, 1280f, Art.a(0xFFDDE8F0, 0.35f));
            r.fillRect(x + w - 3f, 0f, 3f, 1280f, Art.a(0xFF1A2028, 0.8f));
        }
        // 电梯门警示条 + 门缝读数
        r.fillRect(x, 632f, w, 8f, Art.a(Art.HAZARD, 0.45f));
        for (int i = 0; i < 8; i++) r.fillRect(x + 14f + i * 40f, 632f, 14f, 8f, Art.a(0xFF241C12, 0.5f));
        // 门栓
        Art.discOutlined(r, right ? x + 26f : x + w - 26f, 620f, 5f, 0xFF8A96A6, 0xFF1A2028, 1.6f);
    }

    // ==================== 骇客 overlay ====================

    private void drawHacking(Renderer r, World w, HudState hud, Game game) {
        // 全屏底 + 中央冷光
        r.fillRect(0f, 0f, 720f, 1280f, 0xFF05080C);
        r.fillRadialGradient(360f, 480f, 640f, Art.a(0xFF102A3A, 0.50f), Art.a(0xFF04070A, 0.95f));

        int cols = hud.hackCols > 0 ? hud.hackCols : Balance.HACK_COLS;
        int rows = hud.hackRows > 0 ? hud.hackRows : Balance.HACK_ROWS;
        float cell = Balance.HACK_CELL;
        // 网格底
        r.fillRect(0f, 0f, cols * cell, rows * cell, Art.a(0xFF0A1218, 0.9f));
        int[] cells = hud.hackCells;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int type = 0;
                if (cells != null && y * cols + x < cells.length) type = cells[y * cols + x];
                float cx = x * cell, cy = y * cell;
                r.fillRect(cx + 2f, cy + 2f, cell - 4f, cell - 4f,
                        type == 0 ? Art.a(0xFF14212C, 0.85f) : Art.a(0xFF1A1010, 0.85f));
                switch (type) {
                    case 1: { // 黄：阻挡
                        r.fillRect(cx + 5f, cy + 5f, cell - 10f, cell - 10f, Art.a(0xFFA8941E, 0.92f));
                        r.fillRect(cx + 5f, cy + 5f, cell - 10f, 4f, Art.a(0xFFD8C248, 0.9f));
                        for (int i = 0; i < 2; i++) {
                            r.line(cx + 9f + i * 24f, cy + cell - 11f, cx + 22f + i * 24f, cy + 11f,
                                    4f, Art.a(0xFF4A3A08, 0.8f));
                        }
                        // 锁形图标：锁梁 + 锁体
                        r.strokeCircle(cx + cell * 0.5f, cy + cell * 0.5f - 4f, 6f, 3f, Art.a(0xFF3A2E08, 0.8f));
                        r.fillRoundRect(cx + cell * 0.5f - 8f, cy + cell * 0.5f - 3f, 16f, 13f, 3f,
                                Art.a(0xFF3A2E08, 0.85f));
                        r.fillRect(cx + cell * 0.5f - 2f, cy + cell * 0.5f + 2f, 4f, 5f, Art.a(0xFFD8C248, 0.6f));
                        break;
                    }
                    case 2: { // 橙：退回起点
                        r.fillRect(cx + 5f, cy + 5f, cell - 10f, cell - 10f, Art.a(0xFFD0702A, 0.92f));
                        r.fillRect(cx + 5f, cy + 5f, cell - 10f, 4f, Art.a(0xFFFFB878, 0.9f));
                        // 回退箭头
                        r.fillTriangle(cx + 16f, cy + 30f, cx + 40f, cy + 18f, cx + 40f, cy + 42f, Art.a(0xFF3A1805, 0.9f));
                        r.fillRect(cx + 24f, cy + 27f, 14f, 6f, Art.a(0xFF3A1805, 0.9f));
                        r.fillRect(cx + 34f, cy + 21f, 6f, 18f, Art.a(0xFF3A1805, 0.9f));
                        break;
                    }
                    case 3: { // 红黄：立即失败
                        r.fillRect(cx + 5f, cy + 5f, cell - 10f, cell - 10f, Art.a(0xFFA3141C, 0.94f));
                        for (int i = 0; i < 3; i++) {
                            r.fillTriangle(cx + 5f + i * 17f, cy + cell - 5f, cx + 17f + i * 17f, cy + 5f,
                                    cx + 25f + i * 17f, cy + 5f, Art.a(0xFFE1C424, 0.9f));
                        }
                        // 感叹号
                        r.fillRect(cx + cell * 0.5f - 2.5f, cy + 14f, 5f, 18f, Art.a(0xFF3A0808, 0.85f));
                        r.fillRect(cx + cell * 0.5f - 2.5f, cy + 36f, 5f, 6f, Art.a(0xFF3A0808, 0.85f));
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        // 网格线
        for (int x = 0; x <= cols; x++) {
            r.fillRect(x * cell - 1f, 0f, 1.6f, rows * cell, Art.a(0xFF2A4A5A, 0.32f));
        }
        for (int y = 0; y <= rows; y++) {
            r.fillRect(0f, y * cell - 1f, cols * cell, 1.6f, Art.a(0xFF2A4A5A, 0.32f));
        }

        drawHackCellMarker(r, hud.hackStartX, hud.hackStartY, cols, rows, false, w.time);
        drawHackCellMarker(r, hud.hackGoalX, hud.hackGoalY, cols, rows, true, w.time);

        // 光标
        float ccx = (hud.hackCursorX + 0.5f) * cell;
        float ccy = (hud.hackCursorY + 0.5f) * cell;
        float pulse = 0.5f + 0.5f * (float) Math.sin(w.time * 6f);
        Art.glow(r, ccx, ccy, 44f, Palette.CYAN, 0.9f);
        r.fillRoundRect(ccx - 23f, ccy - 23f, 46f, 46f, 8f, Art.a(0xFFFFFFFF, 0.94f));
        r.fillRoundRect(ccx - 16f, ccy - 16f, 32f, 32f, 6f, Art.a(0xFF2E6A80, 1f));
        r.fillCircle(ccx, ccy, 6f, Art.a(Palette.WHITE, 0.9f));
        r.strokeRoundRect(ccx - 27f, ccy - 27f, 54f, 54f, 10f, 2.4f,
                Art.a(Palette.CYAN, 0.45f + 0.35f * pulse));

        // 顶部细时间条
        float timeFrac = hud.hackTimeTotal > 0f
                ? MathUtil.clamp(hud.hackTimeLeft / hud.hackTimeTotal, 0f, 1f) : 0f;
        int timeCol = timeFrac > 0.5f ? Palette.CYAN : (timeFrac > 0.25f ? GOLD : Palette.RED);
        r.fillRect(0f, 0f, 720f, 7f, Art.a(0xFF0A1218, 0.9f));
        r.fillRect(0f, 0f, 720f * timeFrac, 7f, timeCol);
        r.fillRect(0f, 0f, 720f * timeFrac, 2.5f, Art.a(0xFFFFFFFF, 0.3f));

        // ---- 控制区（960..1280）：标题 / 倒计时 / 图例 ----
        r.fillVerticalGradient(0f, 960f, 720f, 320f, 0xFF0C141C, 0xFF06090E);
        r.fillRect(0f, 960f, 720f, 3f, Art.a(0xFF39434F, 0.9f));
        r.fillRect(0f, 963f, 720f, 2f, Art.a(0xFF2A4A5A, 0.35f));
        r.setTextSize(30f);
        r.setTextBold(true);
        r.setTextAlign(0);
        r.setTextColor(Art.a(0xFFDDE8EE, 0.96f));
        r.drawText(Texts.HACK_TITLE, 28f, 1002f);
        r.setTextBold(false);
        r.setTextSize(16f);
        r.setTextColor(Art.a(0xFF7FA8BE, 0.9f));
        r.drawText("拖拽白色光标，从左上角走到右下角", 28f, 1032f);
        // 倒计时
        r.setTextSize(52f);
        r.setTextBold(true);
        r.setTextAlign(2);
        r.setTextColor(timeCol);
        sb.setLength(0);
        sb.append(Math.max(0, (int) Math.ceil(hud.hackTimeLeft)));
        r.drawText(sb.toString(), 672f, 1010f);
        r.setTextSize(20f);
        r.setTextBold(false);
        r.setTextColor(Art.a(0xFF98A3B2, 0.9f));
        r.drawText("秒", 694f, 1028f);
        // 时间条
        r.fillRoundRect(26f, 1054f, 668f, 16f, 8f, Art.a(0xFF06090E, 0.95f));
        r.fillRoundRect(28f, 1056f, 664f, 12f, 6f, Art.a(0xFF1A2A34, 1f));
        if (timeFrac > 0.005f) {
            r.fillRoundRect(28f, 1056f, 664f * timeFrac, 12f, 6f, timeCol);
            r.fillRoundRect(28f, 1056f, 664f * timeFrac, 4f, 3f, Art.a(0xFFFFFFFF, 0.3f));
        }
        r.strokeRoundRect(26f, 1054f, 668f, 16f, 8f, 2f, Art.a(0xFF39434F, 0.9f));
        // 图例
        drawHackLegend(r, 28f, 1094f, 0xFFB8A422, "黄：不可通过");
        drawHackLegend(r, 28f, 1138f, 0xFFD0702A, "橙：退回起点");
        drawHackLegend(r, 380f, 1094f, 0xFFA3141C, "红黄：立即警报");
        drawHackLegend(r, 380f, 1138f, 0xFFEAF2F8, "白：你的位置");
        // 底部提示
        r.setTextSize(15f);
        r.setTextAlign(1);
        r.setTextColor(Art.a(0xFF5A6675, 0.95f));
        r.drawText("限时黑入 · 到达终点即可关闭安保系统", 360f, 1206f);

        // ---- 结果 ----
        if (hud.hackSuccess || hud.hackFailed) {
            boolean ok = hud.hackSuccess;
            float ra = MathUtil.clamp(hud.hackResultTimer > 0f ? hud.hackResultTimer * 1.6f : 1f, 0f, 1f);
            r.fillRect(0f, 0f, 720f, 1280f, Art.a(ok ? 0xFF0A2A1A : 0xFF2A0A0A, 0.72f * ra));
            float py = 360f;
            r.fillRoundRect(120f, py, 480f, 200f, 20f, Art.a(0xFF0C1620, 0.96f * ra));
            r.strokeRoundRect(120f, py, 480f, 200f, 20f, 3f, Art.a(ok ? Palette.MINT : Palette.RED, 0.9f * ra));
            r.setTextSize(42f);
            r.setTextBold(true);
            r.setTextAlign(1);
            r.setTextColor(Art.a(ok ? Palette.MINT : Palette.ALERT, ra));
            r.drawText(ok ? "骇入成功" : "骇入失败", 360f, py + 68f);
            r.setTextSize(22f);
            r.setTextColor(Art.a(0xFFDDE8EE, 0.95f * ra));
            r.drawText(ok ? Texts.HACK_SUCCESS : Texts.HACK_FAIL, 360f, py + 124f);
            if (!ok) {
                r.setTextSize(18f);
                r.setTextColor(Art.a(Palette.RED, 0.9f * ra));
                r.drawText(Texts.HACK_ALARM, 360f, py + 160f);
            }
            r.setTextBold(false);
        }
    }

    private void drawHackLegend(Renderer r, float x, float y, int color, String label) {
        r.fillRoundRect(x, y - 12f, 24f, 24f, 5f, color);
        if (color == 0xFFA3141C) {
            // 红黄警戒格：加黄色斜纹，与游戏内一致
            for (int i = 0; i < 2; i++) {
                float bx = x + 2f + i * 11f;
                r.fillTriangle(bx, y + 11f, bx + 5f, y - 11f, bx + 10f, y - 11f, Art.a(0xFFE1C424, 0.9f));
            }
        }
        r.strokeRoundRect(x, y - 12f, 24f, 24f, 5f, 1.4f, Art.a(0xFF39434F, 0.9f));
        r.setTextSize(17f);
        r.setTextAlign(0);
        r.setTextColor(Art.a(0xFFB8C4D0, 0.95f));
        r.drawText(label, x + 34f, y);
    }

    private void drawHackCellMarker(Renderer r, int gx, int gy, int cols, int rows, boolean goal, float t) {
        if (gx < 0 || gy < 0 || gx >= cols || gy >= rows) return;
        float cell = Balance.HACK_CELL;
        float cx = (gx + 0.5f) * cell;
        float cy = (gy + 0.5f) * cell;
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * (goal ? 5f : 3f));
        if (goal) {
            Art.glow(r, cx, cy, 44f, GOLD, 0.8f + 0.4f * pulse);
            r.fillRoundRect(cx - 24f, cy - 24f, 48f, 48f, 8f, Art.a(0xFF3A2A08, 0.95f));
            r.fillRoundRect(cx - 20f, cy - 20f, 40f, 40f, 6f, Art.a(GOLD, 0.9f));
            // 旗帜
            r.fillRect(cx - 8f, cy - 14f, 3f, 26f, Art.a(0xFF2A1A0A, 0.9f));
            r.fillTriangle(cx - 6f, cy - 14f, cx + 12f, cy - 8f, cx - 6f, cy - 2f, Art.a(0xFF2A1A0A, 0.9f));
            r.strokeRoundRect(cx - 24f, cy - 24f, 48f, 48f, 8f, 2f, Art.a(GOLD, 0.5f + 0.4f * pulse));
        } else {
            r.fillRoundRect(cx - 24f, cy - 24f, 48f, 48f, 8f, Art.a(0xFF0A2A20, 0.95f));
            r.fillRoundRect(cx - 20f, cy - 20f, 40f, 40f, 6f, Art.a(0xFF1A5A40, 0.9f));
            r.setTextSize(20f);
            r.setTextBold(true);
            r.setTextAlign(1);
            r.setTextColor(Art.a(Palette.MINT, 0.95f));
            r.drawText("起", cx, cy);
            r.setTextBold(false);
            r.strokeRoundRect(cx - 24f, cy - 24f, 48f, 48f, 8f, 2f, Art.a(Palette.MINT, 0.4f + 0.3f * pulse));
        }
    }

    private static float textW(Renderer r, String s, float size) {
        r.setTextSize(size);
        return r.measureText(s);
    }
}
