package com.crazyclinic.core.game;

import com.crazyclinic.core.world.Enemy;
import com.crazyclinic.core.world.Player;
import com.crazyclinic.core.world.World;

/** 圆形实体与瓦片墙的轴分离碰撞/滑墙。 */
final class Physics {
    private Physics() {}

    /** 玩家移动：先 X 后 Y，撞墙的轴不动。 */
    static void move(World w, Player p, float dx, float dy) {
        if (w == null || p == null) return;
        if (dx != 0f) {
            float nx = p.x + dx;
            if (!w.collidesCircle(nx, p.y, p.radius)) p.x = nx;
        }
        if (dy != 0f) {
            float ny = p.y + dy;
            if (!w.collidesCircle(p.x, ny, p.radius)) p.y = ny;
        }
    }

    /** 敌人移动（同样轴分离滑墙），并避免叠进另一个敌人太少。 */
    static void move(World w, Enemy e, float dx, float dy) {
        if (w == null || e == null) return;
        if (dx != 0f) {
            float nx = e.x + dx;
            if (!w.collidesCircle(nx, e.y, e.radius)) e.x = nx;
        }
        if (dy != 0f) {
            float ny = e.y + dy;
            if (!w.collidesCircle(e.x, ny, e.radius)) e.y = ny;
        }
    }

    /** 施加减速后的位移：直接尝试位移并返回是否成功（用于击退）。 */
    static boolean push(World w, Enemy e, float dx, float dy) {
        if (w == null || e == null) return false;
        boolean moved = false;
        float nx = e.x + dx;
        if (!w.collidesCircle(nx, e.y, e.radius)) {
            e.x = nx;
            moved = true;
        }
        float ny = e.y + dy;
        if (!w.collidesCircle(e.x, ny, e.radius)) {
            e.y = ny;
            moved = true;
        }
        return moved;
    }
}
