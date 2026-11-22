package com.crazyclinic.core.game;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Balance;
import com.crazyclinic.core.content.Texts;
import com.crazyclinic.core.world.Enemy;
import com.crazyclinic.core.world.EnemyState;
import com.crazyclinic.core.world.EnemyType;
import com.crazyclinic.core.world.Player;
import com.crazyclinic.core.world.ProjectileType;
import com.crazyclinic.core.world.TileMap;
import com.crazyclinic.core.world.World;

import java.util.ArrayList;

/**
 * 敌人 AI：巡逻 → 视野锥 + LOS 发现 → 警觉 → BFS 寻路追击 → 攻击 → 硬直 / 死亡。
 *
 * 性能：视野检测与重寻路都跑在 0.15~0.5s 的错峰计时器上（不每帧全图），
 * BFS 用 {@link Pathfinder} 的复用数组，单帧不会分配路径容器。
 */
final class EnemyAI {
    private static final float PI = MathUtil.PI;
    private static final float VISION_CONE = PI * 55f / 180f;   // 巡逻视野半角 55°
    private static final float SENTRY_CONE = PI / 3f;           // 哨兵扫描半角 60°
    private static final float LOST_CHASE_TIME = 3.0f;
    private static final float CORPSE_TIME = 30f;

    private final Pathfinder path;
    private final ArrayList<Agent> agents = new ArrayList<>();
    private final ArrayList<Enemy> summonQueue = new ArrayList<>();

    /** 本帧是否有敌人看见玩家（GameScene 用来加减警戒度）。 */
    boolean anySees;

    EnemyAI(TileMap map) {
        this.path = new Pathfinder(map);
    }

    /** 每个敌人的 AI 私有状态（不污染 Lead 的 Enemy 数据模型）。 */
    static final class Agent {
        final Enemy e;
        final float[] px = new float[Pathfinder.MAX_PATH];
        final float[] py = new float[Pathfinder.MAX_PATH];
        int pathLen, pathIdx;
        float repathTimer;
        float thinkTimer;
        float lostTimer;
        float lastSeenX, lastSeenY;
        float windup;
        int attackKind;
        float specialCd;
        float dashTime, dashDx, dashDy;
        boolean dashHit;
        float reviveTimer;
        float scanPhase;
        float homeAngle;
        float stuckTimer;
        float contactCd;
        int bossPhase = 1;
        float chargeWindup, chargeTime, chargeDx, chargeDy;
        float chargeCd;
        float repathJitter;
        boolean seesPlayer;

        Agent(Enemy e) {
            this.e = e;
            this.homeAngle = e.angle;
            this.repathJitter = MathUtil.rand(0f, 0.18f);
            this.thinkTimer = MathUtil.rand(0f, 0.16f);
            this.wanderTimer = MathUtil.rand(0.2f, 1.6f);
        }

        float wanderTimer;
        float wanderAngle = MathUtil.rand(0f, MathUtil.TAU);
    }

    // ------------------------------------------------------------------
    // 生命周期
    // ------------------------------------------------------------------

    void bind(World w) {
        agents.clear();
        if (w == null) return;
        for (Enemy e : w.enemies) agents.add(new Agent(e));
    }

    Agent agentOf(Enemy e) {
        for (int i = 0; i < agents.size(); i++) {
            if (agents.get(i).e == e) return agents.get(i);
        }
        return null;
    }

    private void ensureAgents(World w) {
        while (agents.size() < w.enemies.size()) {
            agents.add(new Agent(w.enemies.get(agents.size())));
        }
    }

    void update(GameScene s, Game game, float dt) {
        World w = s.world;
        Player p = w.player;
        if (w == null || p == null || dt <= 0f) return;
        ensureAgents(w);
        anySees = false;

        int n = agents.size();
        for (int i = 0; i < n; i++) {
            Agent a = agents.get(i);
            Enemy e = a.e;
            if (e == null) continue;
            if (e.hurtFlash > 0f) e.hurtFlash = Math.max(0f, e.hurtFlash - dt);
            if (e.muzzleFlash > 0f) e.muzzleFlash = Math.max(0f, e.muzzleFlash - dt);

            if (e.isDead()) {
                if (e.deathTimer > 0f) e.deathTimer = Math.max(0f, e.deathTimer - dt);
                if (e.bodyTimer > 0f) e.bodyTimer = Math.max(0f, e.bodyTimer - dt);
                continue;
            }
            if (e.stunTimer > 0f) {
                e.stunTimer = Math.max(0f, e.stunTimer - dt);
                a.windup = 0f;
                if (e.stunTimer <= 0f) {
                    e.state = EnemyState.ALERT;
                    e.stateTimer = 0.4f;
                }
                continue;
            }
            if (e.state == EnemyState.HURT) {
                e.stateTimer -= dt;
                if (e.stateTimer <= 0f) {
                    e.state = EnemyState.CHASE;
                    a.lostTimer = 0f;
                }
                if (a.seesPlayer) anySees = true;
                continue;
            }

            if (e.turret) {
                updateSentry(s, game, a, p, dt);
            } else if (e.type == EnemyType.BOSS) {
                updateBoss(s, game, a, p, dt);
            } else {
                updateNormal(s, game, a, p, dt);
            }
            if (a.seesPlayer) {
                anySees = true;
                // 被看见会累积警戒
                w.alert = Math.min(1f, w.alert + Balance.ALERT_GAIN * 1.7f * dt);
            }
        }

        // BOSS 召唤的守卫在帧末加入
        for (int i = 0; i < summonQueue.size(); i++) {
            Enemy e = summonQueue.get(i);
            w.enemies.add(e);
            agents.add(new Agent(e));
        }
        summonQueue.clear();
    }

    // ------------------------------------------------------------------
    // 普通敌人
    // ------------------------------------------------------------------

    private void updateNormal(GameScene s, Game game, Agent a, Player p, float dt) {
        Enemy e = a.e;
        updateDetection(s, a, p, dt);
        if (e.attackTimer > 0f) e.attackTimer = Math.max(0f, e.attackTimer - dt);
        if (a.specialCd > 0f) a.specialCd = Math.max(0f, a.specialCd - dt);

        // 攻击前摇：站定读条，结束时结算
        if (a.windup > 0f) {
            e.state = EnemyState.ATTACK;
            face(e, p.x, p.y, 14f, dt);
            a.windup -= dt;
            if (a.windup <= 0f) {
                a.windup = 0f;
                executeNormalAttack(s, game, a, p);
            }
            return;
        }

        // 医生电锯冲刺
        if (a.dashTime > 0f) {
            a.dashTime -= dt;
            e.state = EnemyState.CHASE;
            e.angle = (float) Math.atan2(a.dashDy, a.dashDx);
            Physics.move(s.world, e, a.dashDx * e.speed * 2.3f * dt, a.dashDy * e.speed * 2.3f * dt);
            if (!a.dashHit && MathUtil.dist(e.x, e.y, p.x, p.y) < e.radius + p.radius + 10f
                    && s.world.lineOfSight(e.x, e.y, p.x, p.y)) {
                a.dashHit = true;
                s.combat.damagePlayer(s, game, e.damage, e.x, e.y, false);
                s.world.shake = Math.max(s.world.shake, 0.28f);
            }
            if (a.dashTime <= 0f) a.dashHit = false;
            return;
        }

        float d = MathUtil.dist(e.x, e.y, p.x, p.y);

        // 感冒病人：只游荡 + 近身喷嚏
        if (e.type == EnemyType.FLU_PATIENT) {
            updateFlu(s, game, a, p, d, dt);
            return;
        }

        // 护士：看不见玩家时优先读条复活尸体
        if (!a.seesPlayer && e.type == EnemyType.NURSE && tryNurseRevive(s, game, a, dt)) {
            return;
        }

        if (a.seesPlayer) {
            a.lostTimer = 0f;
            a.lastSeenX = p.x;
            a.lastSeenY = p.y;
            if (e.state == EnemyState.PATROL) {
                e.state = EnemyState.ALERT;
                e.stateTimer = 0.8f;
                e.alertTimer = 1f;
                face(e, p.x, p.y, 10f, dt);
                return;
            }
            if (e.state == EnemyState.ALERT) {
                e.stateTimer -= dt;
                face(e, p.x, p.y, 6f, dt);
                if (e.stateTimer > 0f) return;
                e.state = EnemyState.CHASE;
            }
            // 医生电锯猛冲
            if (e.type == EnemyType.DOCTOR && d > 110f && d < 430f && a.specialCd <= 0f) {
                a.specialCd = 4.5f;
                a.dashTime = 0.6f;
                a.dashHit = false;
                a.dashDx = (p.x - e.x) / Math.max(1f, d);
                a.dashDy = (p.y - e.y) / Math.max(1f, d);
                e.state = EnemyState.ATTACK;
                if (game != null) game.audio.play(Sfx.SWING);
                return;
            }
            if (tryStartAttack(s, game, a, p, d)) return;
            chase(s, a, p, dt, e.type == EnemyType.DOCTOR ? 1.3f : 1f);
            return;
        }

        // 看不见玩家：警觉 → 搜索最后已知位置 → 回巡逻
        if (e.state == EnemyState.ALERT) {
            e.stateTimer -= dt;
            face(e, a.lastSeenX, a.lastSeenY, 5f, dt);
            if (e.stateTimer <= 0f) e.state = EnemyState.CHASE;
            return;
        }
        if (e.state == EnemyState.CHASE || e.state == EnemyState.ATTACK) {
            if (a.lostTimer > LOST_CHASE_TIME) {
                e.state = EnemyState.PATROL;
                a.pathLen = 0;
                return;
            }
            if (MathUtil.dist(e.x, e.y, a.lastSeenX, a.lastSeenY) < 72f) {
                e.state = EnemyState.PATROL;
                a.pathLen = 0;
                return;
            }
            chase(s, a, p, dt, e.type == EnemyType.DOCTOR ? 1.15f : 0.95f);
            return;
        }
        patrol(s, a, dt);
    }

    private void updateFlu(GameScene s, Game game, Agent a, Player p, float d, float dt) {
        Enemy e = a.e;
        if (e.attackTimer > 0f) e.attackTimer = Math.max(0f, e.attackTimer - dt);
        if (d < e.attackRange + 40f) {
            e.state = EnemyState.ATTACK;
            face(e, p.x, p.y, 3f, dt);
            if (e.attackTimer <= 0f) {
                e.attackTimer = e.attackCooldown;
                // 喷嚏 AoE：伤害 + 减速，正对病人时触发
                float ang = MathUtil.angle(e.x, e.y, p.x, p.y);
                if (Math.abs(MathUtil.angleDelta(e.angle, ang)) < PI * 0.5f && d < 165f
                        && s.world.lineOfSight(e.x, e.y, p.x, p.y)) {
                    s.combat.damagePlayer(s, game, e.damage, e.x, e.y, false);
                    p.epoSlowTimer = Math.max(p.epoSlowTimer, 2f);
                    s.effects.poison(s.world, p.x, p.y, 4);
                    s.effects.smoke(s.world, e.x, e.y, 2, 0.8f);
                }
            }
            return;
        }
        // 不主动追击，只游荡
        e.state = EnemyState.PATROL;
        patrol(s, a, dt);
    }

    private void updateDetection(GameScene s, Agent a, Player p, float dt) {
        a.thinkTimer -= dt;
        if (a.thinkTimer > 0f) return;
        a.thinkTimer = 0.15f + a.repathJitter;
        boolean see = canSee(s, a, p);
        if (see != a.seesPlayer) {
            if (see) {
                a.lostTimer = 0f;
                a.lastSeenX = p.x;
                a.lastSeenY = p.y;
            }
        }
        a.seesPlayer = see;
        if (!see) a.lostTimer += 0.15f;
    }

    /** 视野锥 + LOS；隐形的玩家不可见；全图警报时视野放宽。 */
    private boolean canSee(GameScene s, Agent a, Player p) {
        Enemy e = a.e;
        if (p == null || p.isInvisible()) return false;
        if (e.sightRange <= 0.1f) return false;
        World w = s.world;
        float d = MathUtil.dist(e.x, e.y, p.x, p.y);
        float range = e.sightRange * (w.alert >= 0.99f ? 1.35f : 1f);
        if (d > range) return false;
        if (d > 120f) {
            float want = MathUtil.angle(e.x, e.y, p.x, p.y);
            float diff = Math.abs(MathUtil.angleDelta(e.angle, want));
            float cone = e.turret ? SENTRY_CONE : VISION_CONE;
            if (diff > cone) return false;
        }
        return w.lineOfSight(e.x, e.y, p.x, p.y);
    }

    private void patrol(GameScene s, Agent a, float dt) {
        Enemy e = a.e;
        e.state = EnemyState.PATROL;
        a.wanderTimer -= dt;
        if (a.wanderTimer <= 0f) {
            a.wanderTimer = MathUtil.rand(1.3f, 3.2f);
            float fromHome = MathUtil.dist(e.x, e.y, e.homeX, e.homeY);
            if (fromHome > TileMap.TILE * 4f) {
                a.wanderAngle = MathUtil.angle(e.x, e.y, e.homeX, e.homeY) + MathUtil.rand(-0.45f, 0.45f);
            } else {
                a.wanderAngle = MathUtil.rand(0f, MathUtil.TAU);
            }
        }
        float speed = e.speed * 0.55f;
        float nx = e.x + (float) Math.cos(a.wanderAngle) * TileMap.TILE * 0.8f;
        float ny = e.y + (float) Math.sin(a.wanderAngle) * TileMap.TILE * 0.8f;
        if (s.world.map.solidAt(nx, ny)) {
            a.wanderAngle += MathUtil.rand(0.9f, 2.4f);
            a.wanderTimer = MathUtil.rand(0.2f, 0.5f);
            return;
        }
        e.angle = MathUtil.approachAngle(e.angle, a.wanderAngle, 2.6f * dt);
        move(s.world, e, (float) Math.cos(a.wanderAngle) * speed * dt,
                (float) Math.sin(a.wanderAngle) * speed * dt);
    }

    private void chase(GameScene s, Agent a, Player p, float dt, float speedMul) {
        Enemy e = a.e;
        e.state = EnemyState.CHASE;
        World w = s.world;
        float speed = e.speed * speedMul;
        boolean los = w.lineOfSight(e.x, e.y, p.x, p.y);
        if (los && a.seesPlayer) {
            a.pathLen = 0;
            face(e, p.x, p.y, 10f, dt);
            move(w, e, (p.x - e.x), (p.y - e.y), speed * dt);
        } else {
            a.repathTimer -= dt;
            if (a.repathTimer <= 0f || a.pathLen == 0) {
                a.repathTimer = 0.34f + a.repathJitter;
                a.pathLen = path.find(w.map.tx(e.x), w.map.ty(e.y),
                        w.map.tx(a.lastSeenX), w.map.ty(a.lastSeenY), a.px, a.py);
                a.pathIdx = 0;
            }
            if (a.pathLen > 0 && a.pathIdx < a.pathLen) {
                float wx = a.px[a.pathIdx];
                float wy = a.py[a.pathIdx];
                if (MathUtil.dist(e.x, e.y, wx, wy) < 22f) {
                    a.pathIdx++;
                    if (a.pathIdx >= a.pathLen) a.pathLen = 0;
                }
                if (a.pathLen > 0 && a.pathIdx < a.pathLen) {
                    wx = a.px[a.pathIdx];
                    wy = a.py[a.pathIdx];
                    face(e, wx, wy, 8f, dt);
                    move(w, e, wx - e.x, wy - e.y, speed * dt);
                    return;
                }
            }
            // 无路可走时朝最后已知位置直线逼近（会滑墙，不会卡死）
            face(e, a.lastSeenX, a.lastSeenY, 6f, dt);
            move(w, e, a.lastSeenX - e.x, a.lastSeenY - e.y, speed * 0.85f * dt);
        }
    }

    private void move(World w, Enemy e, float dx, float dy) {
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < 0.0001f) return;
        move(w, e, dx, dy, d);
    }

    /** 位移辅助：按方向归一化后移动，记录卡墙时间。 */
    private void move(World w, Enemy e, float dx, float dy, float maxStep) {
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < 0.0001f) return;
        float step = Math.min(maxStep, d);
        Physics.move(w, e, dx / d * step, dy / d * step);
    }

    private void face(Enemy e, float tx, float ty, float rate, float dt) {
        float want = MathUtil.angle(e.x, e.y, tx, ty);
        e.angle = MathUtil.approachAngle(e.angle, want, rate * dt);
    }

    // ------------------------------------------------------------------
    // 攻击
    // ------------------------------------------------------------------

    private boolean tryStartAttack(GameScene s, Game game, Agent a, Player p, float d) {
        Enemy e = a.e;
        World w = s.world;
        boolean los = w.lineOfSight(e.x, e.y, p.x, p.y);
        switch (e.type) {
            case GUARD:
                if (d <= 92f) {
                    a.attackKind = 0;
                    a.windup = 0.3f;
                    e.state = EnemyState.ATTACK;
                    return true;
                }
                if (d <= e.attackRange && los && e.attackTimer <= 0f) {
                    a.attackKind = 1;
                    a.windup = 0.4f;
                    e.state = EnemyState.ATTACK;
                    return true;
                }
                return false;
            case DOCTOR:
                if (d <= 100f) {
                    a.attackKind = 0;
                    a.windup = 0.35f;
                    e.state = EnemyState.ATTACK;
                    return true;
                }
                return false;
            case NURSE:
                if (d <= 96f) {
                    a.attackKind = 4;
                    a.windup = 0.35f;
                    e.state = EnemyState.ATTACK;
                    return true;
                }
                return false;
            case TECH:
                if (d <= e.attackRange && los && e.attackTimer <= 0f) {
                    a.attackKind = 2;
                    a.windup = 0.55f;
                    e.state = EnemyState.ATTACK;
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private void executeNormalAttack(GameScene s, Game game, Agent a, Player p) {
        Enemy e = a.e;
        e.attackTimer = e.attackCooldown;
        float d = MathUtil.dist(e.x, e.y, p.x, p.y);
        switch (a.attackKind) {
            case 0: // 近战
                if (d <= 118f && s.world.lineOfSight(e.x, e.y, p.x, p.y)) {
                    s.combat.damagePlayer(s, game, e.damage, e.x, e.y, false);
                    s.world.shake = Math.max(s.world.shake, 0.2f);
                }
                if (game != null) game.audio.play(Sfx.SWING);
                break;
            case 1: // 看守人扔扳钳
                if (d <= e.attackRange + 40f) {
                    s.combat.enemyShot(s, e, ProjectileType.WRENCH,
                            MathUtil.angle(e.x, e.y, p.x, p.y), 620f, e.damage, false, false);
                    e.muzzleFlash = 0.1f;
                    if (game != null) game.audio.play(Sfx.THROW);
                    noise(s.world, e.x, e.y, 260f);
                }
                break;
            case 2: // 中心助手扔试管（爆炸/毒气）
                if (d <= e.attackRange + 40f) {
                    boolean poison = MathUtil.chance(0.5f);
                    s.combat.enemyShot(s, e, ProjectileType.VIAL,
                            MathUtil.angle(e.x, e.y, p.x, p.y), 560f, e.damage, true, poison);
                    e.muzzleFlash = 0.1f;
                    if (game != null) game.audio.play(Sfx.THROW);
                    noise(s.world, e.x, e.y, 300f);
                }
                break;
            case 3: // 喷嚏
                if (d <= 165f && s.world.lineOfSight(e.x, e.y, p.x, p.y)) {
                    s.combat.damagePlayer(s, game, e.damage, e.x, e.y, false);
                    p.epoSlowTimer = Math.max(p.epoSlowTimer, 2f);
                }
                break;
            case 4: // 护士注射
                if (d <= 110f && s.world.lineOfSight(e.x, e.y, p.x, p.y)) {
                    s.combat.damagePlayer(s, game, e.damage, e.x, e.y, false);
                    s.effects.blood(s.world, p.x, p.y, MathUtil.angle(e.x, e.y, p.x, p.y), 4);
                }
                break;
            default:
                break;
        }
        e.state = EnemyState.CHASE;
    }

    /** 护士读条复活尸体（3s），玩家靠近/被看见时优先战斗。 */
    private boolean tryNurseRevive(GameScene s, Game game, Agent a, float dt) {
        Enemy e = a.e;
        Enemy corpse = null;
        float best = 280f;
        for (Enemy c : s.world.enemies) {
            if (c == e || !c.isDead() || c.type == EnemyType.BOSS) continue;
            if (c.bodyTimer <= 0f) continue;
            float d = MathUtil.dist(e.x, e.y, c.x, c.y);
            if (d < best) {
                best = d;
                corpse = c;
            }
        }
        if (corpse == null) {
            e.reviveTimer = 0f;
            return false;
        }
        if (best > 78f) {
            a.repathTimer -= dt;
            if (a.repathTimer <= 0f || a.pathLen == 0) {
                a.repathTimer = 0.4f;
                a.pathLen = path.find(s.world.map.tx(e.x), s.world.map.ty(e.y),
                        s.world.map.tx(corpse.x), s.world.map.ty(corpse.y), a.px, a.py);
                a.pathIdx = 0;
                e.reviveTimer = 0f;
            }
            e.state = EnemyState.CHASE;
            if (a.pathLen > 0 && a.pathIdx < a.pathLen) {
                float wx = a.px[a.pathIdx];
                float wy = a.py[a.pathIdx];
                if (MathUtil.dist(e.x, e.y, wx, wy) < 22f) {
                    a.pathIdx++;
                } else {
                    face(e, wx, wy, 8f, dt);
                    move(s.world, e, wx - e.x, wy - e.y, e.speed * dt);
                    return true;
                }
            }
            face(e, corpse.x, corpse.y, 6f, dt);
            move(s.world, e, corpse.x - e.x, corpse.y - e.y, e.speed * 0.9f * dt);
            return true;
        }
        // 读条
        e.state = EnemyState.ATTACK;
        face(e, corpse.x, corpse.y, 8f, dt);
        e.reviveTimer += dt;
        if (MathUtil.chance(0.22f)) {
            s.effects.electric(s.world, corpse.x, corpse.y, 1);
        }
        if (e.reviveTimer >= 3f) {
            e.reviveTimer = 0f;
            corpse.dead = false;
            corpse.state = EnemyState.ALERT;
            corpse.stateTimer = 0.8f;
            corpse.hp = Math.max(8f, corpse.maxHp * 0.5f);
            corpse.deathTimer = 0f;
            corpse.bodyTimer = 0f;
            corpse.hurtFlash = 0.2f;
            s.effects.text(s.world, "复活！", corpse.x, corpse.y - 44, Palette.MINT);
            s.effects.electric(s.world, corpse.x, corpse.y, 12);
            if (game != null) game.audio.play(Sfx.ORGAN_UPGRADE);
            if (!a.seesPlayer) a.lostTimer = 0f;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // 哨兵枪
    // ------------------------------------------------------------------

    private void updateSentry(GameScene s, Game game, Agent a, Player p, float dt) {
        Enemy e = a.e;
        if (e.attackTimer > 0f) e.attackTimer = Math.max(0f, e.attackTimer - dt);
        if (s.world.camerasDisabled) {
            e.state = EnemyState.PATROL;
            a.seesPlayer = false;
            return;
        }
        a.scanPhase += dt * 0.95f;
        e.angle = a.homeAngle + (float) Math.sin(a.scanPhase) * 1.05f;
        boolean see = canSee(s, a, p);
        a.seesPlayer = see;
        if (see && e.attackTimer <= 0f) {
            e.attackTimer = e.attackCooldown;
            e.muzzleFlash = 0.1f;
            e.state = EnemyState.ATTACK;
            s.combat.enemyShot(s, e, ProjectileType.BULLET,
                    MathUtil.angle(e.x, e.y, p.x, p.y), Balance.enemy(EnemyType.SENTRY).projectileSpeed,
                    e.damage, false, false);
            if (game != null) game.audio.play(Sfx.PISTOL);
            noise(s.world, e.x, e.y, 480f);
        } else if (!see) {
            e.state = EnemyState.PATROL;
        }
    }

    // ------------------------------------------------------------------
    // BOSS 赞恩
    // ------------------------------------------------------------------

    private void updateBoss(GameScene s, Game game, Agent a, Player p, float dt) {
        Enemy e = a.e;
        if (e.attackTimer > 0f) e.attackTimer = Math.max(0f, e.attackTimer - dt);
        if (a.specialCd > 0f) a.specialCd = Math.max(0f, a.specialCd - dt);
        if (a.contactCd > 0f) a.contactCd = Math.max(0f, a.contactCd - dt);
        if (a.chargeCd > 0f) a.chargeCd = Math.max(0f, a.chargeCd - dt);
        updateDetection(s, a, p, dt);
        float d = MathUtil.dist(e.x, e.y, p.x, p.y);
        if (a.seesPlayer) {
            a.lostTimer = 0f;
            a.lastSeenX = p.x;
            a.lastSeenY = p.y;
        } else {
            a.lostTimer += dt;
        }

        int phase = e.hp / e.maxHp > 0.66f ? 1 : (e.hp / e.maxHp > 0.33f ? 2 : 3);
        if (phase != a.bossPhase) {
            a.bossPhase = phase;
            s.world.shake = Math.max(s.world.shake, 0.5f);
            s.effects.text(s.world, "赞恩 · 第 " + phase + " 阶段", e.x, e.y - 70, Palette.PURPLE);
            if (game != null) game.audio.play(Sfx.BOSS_ROAR);
            if (phase == 2) {
                summonMinions(s, e, 2);
                a.specialCd = 9.5f;
            }
            if (phase == 3) {
                summonMinions(s, e, 1);
                a.specialCd = 7f;
            }
        }

        // 冲撞
        if (a.chargeWindup > 0f) {
            a.chargeWindup -= dt;
            e.state = EnemyState.ATTACK;
            face(e, p.x, p.y, 10f, dt);
            s.world.shake = Math.max(s.world.shake, 0.25f);
            if (a.chargeWindup <= 0f) {
                a.chargeTime = 0.9f;
                a.contactCd = 0f;
                float ang = MathUtil.angle(e.x, e.y, p.x, p.y);
                a.chargeDx = (float) Math.cos(ang);
                a.chargeDy = (float) Math.sin(ang);
                e.angle = ang;
                if (game != null) game.audio.play(Sfx.BOSS_ROAR);
            }
            return;
        }
        if (a.chargeTime > 0f) {
            a.chargeTime -= dt;
            if (a.chargeTime <= 0f) a.chargeCd = 6f;
            e.state = EnemyState.ATTACK;
            float sx = e.x, sy = e.y;
            Physics.move(s.world, e, a.chargeDx * 520f * dt, a.chargeDy * 520f * dt);
            // 撞墙或撞到玩家即结束
            if (Math.abs(e.x - sx) + Math.abs(e.y - sy) < 0.2f) {
                a.chargeTime = 0f;
                a.chargeCd = 6f;
                s.world.shake = Math.max(s.world.shake, 0.5f);
            }
            if (a.contactCd <= 0f && MathUtil.dist(e.x, e.y, p.x, p.y) < e.radius + p.radius + 12f
                    && s.world.lineOfSight(e.x, e.y, p.x, p.y)) {
                a.contactCd = 0.6f;
                s.combat.damagePlayer(s, game, e.damage, e.x, e.y, false);
                s.world.shake = Math.max(s.world.shake, 0.5f);
            }
            return;
        }

        // 攻击前摇
        if (a.windup > 0f) {
            e.state = EnemyState.ATTACK;
            face(e, p.x, p.y, 10f, dt);
            a.windup -= dt;
            if (a.windup <= 0f) {
                a.windup = 0f;
                executeBossAttack(s, game, a, p, d);
            }
            return;
        }

        // 召唤
        if (a.bossPhase >= 2 && a.specialCd <= 0f) {
            a.specialCd = 9.5f;
            summonMinions(s, e, a.bossPhase >= 3 ? 1 : 2);
            s.effects.text(s.world, "守卫，上！", e.x, e.y - 60, Palette.ALERT);
            if (game != null) game.audio.play(Sfx.BOSS_ROAR);
        }
        // 三阶段蓄力冲撞
        if (a.bossPhase >= 3 && a.chargeCd <= 0f && a.chargeTime <= 0f && a.chargeWindup <= 0f
                && MathUtil.dist(e.x, e.y, p.x, p.y) > 140f) {
            a.chargeWindup = 0.62f;
            e.state = EnemyState.ATTACK;
            return;
        }

        if (a.seesPlayer) {
            e.state = EnemyState.CHASE;
            if (d <= 125f && e.attackTimer <= 0f) {
                a.attackKind = 0;
                a.windup = 0.45f;
                e.state = EnemyState.ATTACK;
                return;
            }
            if (d <= 620f && e.attackTimer <= 0f && s.world.lineOfSight(e.x, e.y, p.x, p.y)) {
                a.attackKind = 1;
                a.windup = 0.4f;
                e.state = EnemyState.ATTACK;
                return;
            }
            chase(s, a, p, dt, 1f);
        } else if (a.lostTimer > 4f) {
            patrol(s, a, dt);
        } else {
            chase(s, a, p, dt, 0.85f);
        }
    }

    private void executeBossAttack(GameScene s, Game game, Agent a, Player p, float d) {
        Enemy e = a.e;
        e.attackTimer = e.attackCooldown;
        if (a.attackKind == 0) {
            // 大锤横扫
            if (d <= 160f && s.world.lineOfSight(e.x, e.y, p.x, p.y)) {
                s.combat.damagePlayer(s, game, e.damage, e.x, e.y, false);
            }
            s.world.shake = Math.max(s.world.shake, 0.6f);
            s.effects.explosion(s.world, e.x + (float) Math.cos(e.angle) * 60f,
                    e.y + (float) Math.sin(e.angle) * 60f);
            if (game != null) game.audio.play(Sfx.SHOTGUN);
        } else {
            // 霰弹
            s.combat.enemySpread(s, e, MathUtil.angle(e.x, e.y, p.x, p.y), 5, 26f, 700f, 9f);
            e.muzzleFlash = 0.12f;
            s.world.shake = Math.max(s.world.shake, 0.35f);
            noise(s.world, e.x, e.y, 700f);
            if (game != null) game.audio.play(Sfx.SHOTGUN);
        }
        e.state = EnemyState.CHASE;
    }

    private void summonMinions(GameScene s, Enemy boss, int count) {
        for (int i = 0; i < count; i++) {
            float a = MathUtil.rand(0f, MathUtil.TAU);
            float dist = MathUtil.rand(TileMap.TILE * 1.4f, TileMap.TILE * 2.6f);
            float x = boss.x + (float) Math.cos(a) * dist;
            float y = boss.y + (float) Math.sin(a) * dist;
            if (s.world.collidesCircle(x, y, 24f)) {
                // 换方向再试
                for (int k = 0; k < 8; k++) {
                    a = (float) (k * Math.PI / 4.0);
                    x = boss.x + (float) Math.cos(a) * dist;
                    y = boss.y + (float) Math.sin(a) * dist;
                    if (!s.world.collidesCircle(x, y, 24f)) break;
                }
            }
            Enemy e = new Enemy(EnemyType.GUARD);
            e.x = x;
            e.y = y;
            e.homeX = x;
            e.homeY = y;
            e.angle = MathUtil.rand(0f, MathUtil.TAU);
            e.state = EnemyState.ALERT;
            e.stateTimer = 0.8f;
            e.alertTimer = 1f;
            summonQueue.add(e);
            s.effects.poison(s.world, x, y, 5);
        }
    }

    // ------------------------------------------------------------------
    // 对外：噪音 / 警报 / 受击
    // ------------------------------------------------------------------

    /** 枪声/爆炸惊动附近敌人。 */
    void noise(World w, float x, float y, float radius) {
        if (w == null) return;
        float r2 = radius * radius;
        for (int i = 0; i < agents.size(); i++) {
            Agent a = agents.get(i);
            Enemy e = a.e;
            if (e == null || e.isDead() || e.stunTimer > 0f) continue;
            if (e.type == EnemyType.FLU_PATIENT || e.turret) continue;
            if (MathUtil.dist2(x, y, e.x, e.y) > r2) continue;
            a.lastSeenX = x;
            a.lastSeenY = y;
            a.lostTimer = 0f;
            if (e.state == EnemyState.PATROL || e.state == EnemyState.ALERT) {
                e.state = EnemyState.ALERT;
                e.stateTimer = 0.55f;
                e.alertTimer = 1f;
            }
        }
        w.alert = Math.max(w.alert, 0.3f);
    }

    /** 全图警报：所有敌人向玩家位置警觉扑来。 */
    void alarmAll(GameScene s) {
        World w = s.world;
        Player p = w.player;
        w.alert = 1f;
        for (int i = 0; i < agents.size(); i++) {
            Agent a = agents.get(i);
            Enemy e = a.e;
            if (e == null || e.isDead() || e.turret || e.stunTimer > 0f) continue;
            a.lastSeenX = p.x;
            a.lastSeenY = p.y;
            a.lostTimer = 0f;
            if (e.type != EnemyType.BOSS) {
                e.state = EnemyState.ALERT;
                e.stateTimer = 0.8f;
                e.alertTimer = 1f;
            }
        }
    }

    /** 敌人挨打：打断前摇、进入硬直并知道玩家位置。 */
    void onEnemyHurt(GameScene s, Enemy e) {
        Agent a = agentOf(e);
        Player p = s.world.player;
        if (a != null) {
            a.windup = 0f;
            a.lastSeenX = p.x;
            a.lastSeenY = p.y;
            a.lostTimer = 0f;
            a.dashTime = 0f;
        }
        s.world.alert = Math.max(s.world.alert, 0.28f);
    }

    /** 调试/QA：让所有敌人立即进入警觉（可被 GameScene.debug 调用）。 */
    void wakeAll(GameScene s) {
        alarmAll(s);
    }

    /** 查询某个敌人是否正看着玩家（供 UI/HUD 用）。 */
    boolean isSeeing(Enemy e) {
        Agent a = agentOf(e);
        return a != null && a.seesPlayer;
    }
}
