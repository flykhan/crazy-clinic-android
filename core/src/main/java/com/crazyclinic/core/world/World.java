package com.crazyclinic.core.world;

import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.SaveData;
import com.crazyclinic.core.content.Balance;

import java.util.ArrayList;
import java.util.List;

/** 一层关卡的运行时世界（数据容器；系统逻辑在 core.game 的 *System 类中）。 */
public final class World {
    public static final float VIEW_W = 720f;    // 世界视口宽
    public static final float VIEW_H = 960f;    // 世界视口高（下方 320px 为控制区）

    public final LevelDef level;
    public final TileMap map;
    public final Player player;
    public final List<Enemy> enemies = new ArrayList<>();
    public final List<Projectile> projectiles = new ArrayList<>();
    public final List<Pickup> pickups = new ArrayList<>();
    public final List<Particle> particles = new ArrayList<>();
    public final List<FloatingText> floatingTexts = new ArrayList<>();
    public final List<Decal> decals = new ArrayList<>();

    public float cameraX, cameraY;
    public float shake;
    public float time;
    public int kills;
    public boolean won;
    public boolean lost;
    public boolean elevatorUnlocked;
    public boolean camerasDisabled;
    public String objective = "";
    public float alert;                 // 0..1 警戒度
    public int floor;

    public World(LevelDef level, SaveData save) {
        this.level = level;
        this.map = level.map;
        this.floor = level.index;
        this.player = new Player();
        this.player.init(save);
        if (save != null && save.hasOrgan(Organ.ARMS)) this.player.armsBonus = true;
        this.player.x = level.spawnX;
        this.player.y = level.spawnY;
        this.player.angle = -MathUtil.PI * 0.5f;

        for (EnemySpawn s : level.enemies) {
            Enemy e = new Enemy(s.type);
            e.x = map.cx(s.tx);
            e.y = map.cy(s.ty);
            e.homeX = e.x;
            e.homeY = e.y;
            e.angle = MathUtil.rand(0f, MathUtil.TAU);
            if (e.turret) {
                // 哨兵枪朝最近的开放方向
                e.angle = nearestOpenAngle(e.x, e.y);
            }
            enemies.add(e);
        }
        for (ItemSpawn s : level.items) {
            Pickup p;
            if (s.kind == Pickup.Kind.WEAPON) {
                p = Pickup.weapon(s.weapon, map.cx(s.tx), map.cy(s.ty));
            } else if (s.kind == Pickup.Kind.ORGAN) {
                p = Pickup.organ(s.organ, map.cx(s.tx), map.cy(s.ty));
            } else if (s.kind == Pickup.Kind.KEYCARD) {
                p = Pickup.keycard(map.cx(s.tx), map.cy(s.ty));
            } else {
                p = Pickup.item(s.item, s.amount, map.cx(s.tx), map.cy(s.ty));
            }
            p.bob = MathUtil.rand(0f, MathUtil.TAU);
            pickups.add(p);
        }
        centerCameraOnPlayer();
    }

    public void centerCameraOnPlayer() {
        cameraX = player.x - VIEW_W * 0.5f;
        cameraY = player.y - VIEW_H * 0.5f;
        clampCamera();
    }

    public void clampCamera() {
        float maxX = Math.max(0f, map.pixelWidth() - VIEW_W);
        float maxY = Math.max(0f, map.pixelHeight() - VIEW_H);
        cameraX = MathUtil.clamp(cameraX, 0f, maxX);
        cameraY = MathUtil.clamp(cameraY, 0f, maxY);
    }

    public float viewCenterX() {
        return VIEW_W * 0.5f;
    }

    public float viewCenterY() {
        return VIEW_H * 0.5f;
    }

    public float screenToWorldX(float sx) {
        return cameraX + sx;
    }

    public float screenToWorldY(float sy) {
        return cameraY + sy;
    }

    public boolean isSolid(float wx, float wy) {
        return map.solidAt(wx, wy);
    }

    /** 圆形实体是否与墙体碰撞。 */
    public boolean collidesCircle(float x, float y, float r) {
        int minTx = map.tx(x - r), maxTx = map.tx(x + r);
        int minTy = map.ty(y - r), maxTy = map.ty(y + r);
        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                if (!map.solid(tx, ty)) continue;
                float nx = MathUtil.clamp(x, tx * TileMap.TILE, (tx + 1) * TileMap.TILE);
                float ny = MathUtil.clamp(y, ty * TileMap.TILE, (ty + 1) * TileMap.TILE);
                if (MathUtil.dist2(x, y, nx, ny) < r * r) return true;
            }
        }
        return false;
    }

    public Enemy enemyAt(float x, float y, float r) {
        float rr = r * r;
        for (Enemy e : enemies) {
            if (e.isDead()) continue;
            if (MathUtil.dist2(x, y, e.x, e.y) <= rr) return e;
        }
        return null;
    }

    public Pickup pickupAt(float x, float y, float r) {
        float rr = r * r;
        for (Pickup p : pickups) {
            if (p.taken) continue;
            if (MathUtil.dist2(x, y, p.x, p.y) <= rr) return p;
        }
        return null;
    }

    public Enemy boss() {
        for (Enemy e : enemies) {
            if (e.type == EnemyType.BOSS) return e;
        }
        return null;
    }

    public void addParticle(float x, float y, float vx, float vy, float life, float size, int color, int kind) {
        Particle p = new Particle();
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.life = life;
        p.maxLife = life;
        p.size = size;
        p.color = color;
        p.kind = kind;
        p.rotation = MathUtil.rand(0f, MathUtil.TAU);
        p.spin = MathUtil.rand(-8f, 8f);
        particles.add(p);
    }

    public void addBlood(float x, float y, float dirAngle, int count) {
        for (int i = 0; i < count; i++) {
            float a = dirAngle + MathUtil.rand(-0.9f, 0.9f);
            float sp = MathUtil.rand(60f, 260f);
            addParticle(x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp,
                    MathUtil.rand(0.25f, 0.6f), MathUtil.rand(3f, 7f),
                    i % 3 == 0 ? Palette.BLOOD : Palette.RED_DARK, 1);
        }
    }

    public void addFloatingText(String text, float x, float y, int color) {
        FloatingText ft = new FloatingText();
        ft.text = text;
        ft.x = x;
        ft.y = y;
        ft.vy = -56f;
        ft.life = 1.0f;
        ft.maxLife = 1.0f;
        ft.color = color;
        floatingTexts.add(ft);
    }

    public void addDecal(float x, float y, int kind, int color, float scale) {
        Decal d = new Decal();
        d.x = x;
        d.y = y;
        d.kind = kind;
        d.color = color;
        d.scale = scale;
        d.angle = MathUtil.rand(0f, MathUtil.TAU);
        d.life = 30f;
        d.maxLife = 30f;
        decals.add(d);
    }

    /** 敌方/玩家子弹的简易视线（瓦片 DDA 近似）。 */
    public boolean lineOfSight(float x1, float y1, float x2, float y2) {
        float d = MathUtil.dist(x1, y1, x2, y2);
        int steps = Math.max(2, (int) (d / (TileMap.TILE * 0.25f)));
        for (int i = 1; i < steps; i++) {
            float t = i / (float) steps;
            float x = x1 + (x2 - x1) * t;
            float y = y1 + (y2 - y1) * t;
            if (map.opaqueAt(x, y)) return false;
        }
        return true;
    }

    private float nearestOpenAngle(float x, float y) {
        float best = 0f;
        for (int i = 0; i < 8; i++) {
            float a = i * MathUtil.TAU / 8f;
            float nx = x + (float) Math.cos(a) * TileMap.TILE * 2f;
            float ny = y + (float) Math.sin(a) * TileMap.TILE * 2f;
            if (!map.solidAt(nx, ny)) return a;
        }
        return best;
    }

    public float playerSpeedNow() {
        float s = player.sprinting ? player.sprintSpeed : player.speed;
        if (player.speedBoostTimer > 0f) s *= Balance.EPO_SPEED_MUL;
        if (player.epoSlowTimer > 0f) s *= Balance.EPO_SLOW_MUL;
        return s;
    }
}
