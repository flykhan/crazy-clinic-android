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

/**
 * 世界渲染（占位实现：几何色块；美术代理负责程序化重绘为医院场景）。
 * 只负责 720x960 世界视口，不画 HUD。
 */
public final class WorldRenderer {
    public void render(Game game, Renderer r, World w) {
        r.clear(Palette.BG_DEEP);
        r.save();
        r.clipRect(0, 0, World.VIEW_W, World.VIEW_H);
        r.translate(-w.cameraX, -w.cameraY);

        int minTx = Math.max(0, w.map.tx(w.cameraX) - 1);
        int maxTx = Math.min(w.map.width - 1, w.map.tx(w.cameraX + World.VIEW_W) + 1);
        int minTy = Math.max(0, w.map.ty(w.cameraY) - 1);
        int maxTy = Math.min(w.map.height - 1, w.map.ty(w.cameraY + World.VIEW_H) + 1);
        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                drawTile(r, w.map, tx, ty);
            }
        }
        for (Decal d : w.decals) {
            r.fillCircle(d.x, d.y, 22f * d.scale, Palette.alpha(d.color, 0.35f * (d.life / d.maxLife)));
        }
        for (Pickup p : w.pickups) {
            if (p.taken) continue;
            float bob = (float) Math.sin(w.time * 3f + p.bob) * 4f;
            r.fillCircle(p.x, p.y + bob, 18f, Palette.GOLD);
            r.strokeCircle(p.x, p.y + bob, 22f, 3f, Palette.TEXT);
        }
        for (Enemy e : w.enemies) {
            if (e.isDead()) continue;
            int c = e.type == com.crazyclinic.core.world.EnemyType.SENTRY ? Palette.CYAN
                    : e.type == com.crazyclinic.core.world.EnemyType.BOSS ? Palette.PURPLE
                    : e.type == com.crazyclinic.core.world.EnemyType.NURSE ? Palette.MINT : Palette.RED;
            r.fillCircle(e.x, e.y, e.radius, c);
            r.fillCircle(e.x, e.y, e.radius * 0.5f, Palette.BLACK);
            r.save();
            r.translate(e.x, e.y);
            r.rotate((float) Math.toDegrees(e.angle));
            r.fillRect(0, -3, e.radius + 14, 6, Palette.TEXT);
            r.restore();
            if (e.hp < e.maxHp) {
                r.fillRect(e.x - 24, e.y - 34, 48, 6, Palette.alpha(Palette.BLACK, 0.6f));
                r.fillRect(e.x - 24, e.y - 34, 48 * (e.hp / e.maxHp), 6, Palette.RED);
            }
        }
        for (Projectile p : w.projectiles) {
            r.fillCircle(p.x, p.y, 4f, p.fromPlayer ? Palette.GOLD : Palette.ALERT);
        }
        drawPlayer(r, w.player, w.time);
        for (Particle p : w.particles) {
            float a = p.life / p.maxLife;
            r.fillCircle(p.x, p.y, p.size * a, Palette.alpha(p.color, a));
        }
        for (FloatingText ft : w.floatingTexts) {
            r.setTextSize(ft.size);
            r.setTextBold(true);
            r.setTextAlign(1);
            r.setTextColor(Palette.alpha(ft.color, ft.life / ft.maxLife));
            r.drawText(ft.text, ft.x, ft.y);
        }
        r.restore();

        // 暗角 + 玩家光圈
        float px = w.player.x - w.cameraX;
        float py = w.player.y - w.cameraY;
        r.fillRadialGradient(px, py, 640f, Palette.alpha(Palette.BLACK, 0f), Palette.alpha(Palette.BLACK, 0.55f));
        if (w.player.hurtFlash > 0f) {
            r.fillRect(0, 0, World.VIEW_W, World.VIEW_H, Palette.alpha(Palette.RED, 0.28f * w.player.hurtFlash));
        }
    }

    private void drawTile(Renderer r, TileMap map, int tx, int ty) {
        float x = tx * TileMap.TILE;
        float y = ty * TileMap.TILE;
        TileType t = map.type(tx, ty);
        switch (t) {
            case WALL:
            case WINDOW:
                r.fillRect(x, y, TileMap.TILE, TileMap.TILE, Palette.WALL_TOP);
                r.fillRect(x, y + TileMap.TILE * 0.72f, TileMap.TILE, TileMap.TILE * 0.28f, Palette.WALL_SIDE);
                break;
            case DOOR_LOCKED:
                r.fillRect(x, y, TileMap.TILE, TileMap.TILE, Palette.DOOR);
                r.fillRect(x + 10, y + 10, TileMap.TILE - 20, TileMap.TILE - 20, Palette.DOOR_LOCK);
                break;
            case DOOR_OPEN:
                r.fillRect(x, y, TileMap.TILE, TileMap.TILE, Palette.DOOR);
                break;
            case ELEVATOR:
                r.fillRect(x, y, TileMap.TILE, TileMap.TILE, Palette.ELEVATOR);
                r.setTextSize(24);
                r.setTextAlign(1);
                r.setTextColor(Palette.TEXT);
                r.drawText("↑", x + 36, y + 36);
                break;
            case COMPUTER:
                r.fillRect(x, y, TileMap.TILE, TileMap.TILE, Palette.COMPUTER);
                r.fillRect(x + 8, y + 8, TileMap.TILE - 16, TileMap.TILE - 24, Palette.BLUE);
                break;
            case LOCKER:
                r.fillRect(x, y, TileMap.TILE, TileMap.TILE, Palette.LOCKER);
                r.fillRect(x + 6, y + 6, TileMap.TILE - 12, TileMap.TILE - 12, Palette.PANEL_STROKE);
                break;
            case CRATE:
                r.fillRect(x, y, TileMap.TILE, TileMap.TILE, Palette.CRATE);
                break;
            case BED:
                r.fillRect(x, y, TileMap.TILE, TileMap.TILE, Palette.BED);
                r.fillRect(x + 8, y + 6, TileMap.TILE - 16, TileMap.TILE - 30, Palette.WHITE);
                break;
            default:
                boolean even = ((tx + ty) & 1) == 0;
                r.fillRect(x, y, TileMap.TILE, TileMap.TILE, even ? Palette.FLOOR_A : Palette.FLOOR_B);
                r.strokeRect(x, y, TileMap.TILE, TileMap.TILE, 1.5f, Palette.FLOOR_GROUT);
                break;
        }
    }

    private void drawPlayer(Renderer r, Player p, float time) {
        r.fillCircle(p.x, p.y + 6, p.radius * 1.1f, Palette.SHADOW);
        float bob = p.moving ? (float) Math.sin(time * 16f) * 2f : 0f;
        r.save();
        r.translate(p.x, p.y + bob);
        r.rotate((float) Math.toDegrees(p.angle));
        r.fillCircle(0, 0, p.radius, Palette.alpha(Palette.TEXT, p.invisTimer > 0 ? 0.5f : 1f));
        r.fillCircle(p.radius * 0.4f, 0, p.radius * 0.55f, Palette.MINT);
        r.fillRect(p.radius * 0.5f, -3, p.radius + 10, 6, Palette.GOLD);
        r.restore();
    }
}
