package com.crazyclinic.core.render;

import com.crazyclinic.core.Controls;
import com.crazyclinic.core.Game;
import com.crazyclinic.core.HudState;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.world.Player;
import com.crazyclinic.core.world.World;

/** HUD + 触屏控件绘制（占位实现；美术代理负责重绘）。 */
public final class HudRenderer {
    public void render(Game game, Renderer r, World w, HudState hud) {
        drawTopBar(r, w);
        drawControls(r, w, hud);
        if (hud.toast != null && hud.toastTimer > 0) {
            r.fillRoundRect(60, 200, 600, 70, 16, Palette.alpha(Palette.PANEL, 0.9f));
            r.setTextSize(28);
            r.setTextAlign(1);
            r.setTextColor(Palette.TEXT);
            r.drawText(hud.toast, 360, 235);
        }
        if (hud.damageFlash > 0) {
            r.fillRect(0, 0, 720, World.VIEW_H, Palette.alpha(Palette.RED, 0.25f * hud.damageFlash));
        }
    }

    private void drawTopBar(Renderer r, World w) {
        Player p = w.player;
        r.fillRect(0, 0, 720, 150, Palette.alpha(Palette.BLACK, 0.55f));
        // 头像
        r.fillCircle(76, 76, 46, Palette.PANEL_ALT);
        r.fillCircle(76, 66, 24, 0xFFF0CBAE);
        r.fillRect(52, 92, 48, 30, Palette.MINT);
        // 血条
        r.fillRoundRect(140, 46, 320, 24, 8, Palette.alpha(Palette.BLACK, 0.7f));
        r.fillRoundRect(140, 46, 320 * Math.max(0, p.hp / p.maxHp), 24, 8, Palette.RED);
        r.setTextSize(20);
        r.setTextAlign(0);
        r.setTextColor(Palette.TEXT);
        r.drawText("生命", 140, 96);
        // 体力
        r.fillRoundRect(140, 100, 320, 12, 6, Palette.alpha(Palette.BLACK, 0.7f));
        r.fillRoundRect(140, 100, 320 * (p.stamina / p.maxStamina), 12, 6, Palette.GOLD);
        // 武器/弹药
        r.setTextAlign(2);
        r.setTextSize(24);
        r.setTextColor(Palette.TEXT);
        r.drawText(p.weapon.label + (p.ammo[p.weapon.ordinal()] >= 0 ? "  " + p.ammo[p.weapon.ordinal()] : "  ∞"), 700, 70);
        r.setTextSize(20);
        r.setTextColor(Palette.TEXT_DIM);
        r.drawText("第 " + w.floor + " 层", 700, 108);
        // 目标
        if (w.objective != null && !w.objective.isEmpty()) {
            r.setTextAlign(1);
            r.setTextSize(24);
            r.setTextColor(Palette.MINT);
            r.drawText(w.objective, 360, 170);
        }
        // 暂停
        r.fillCircle(Controls.PAUSE_X, Controls.PAUSE_Y, Controls.PAUSE_R, Palette.alpha(Palette.BLACK, 0.5f));
        r.fillRect(Controls.PAUSE_X - 14, Controls.PAUSE_Y - 18, 10, 36, Palette.TEXT);
        r.fillRect(Controls.PAUSE_X + 4, Controls.PAUSE_Y - 18, 10, 36, Palette.TEXT);
    }

    private void drawControls(Renderer r, World w, HudState hud) {
        // 控制区底板
        r.fillRect(0, World.VIEW_H, 720, 320, Palette.alpha(Palette.BG_DEEP, 0.92f));
        // 左摇杆
        float mx = hud.moveStickActive ? hud.moveStickCx : Controls.MOVE_DEFAULT_X;
        float my = hud.moveStickActive ? hud.moveStickCy : Controls.MOVE_DEFAULT_Y;
        r.fillCircle(mx, my, Controls.STICK_RADIUS * 0.6f, Palette.alpha(Palette.PANEL, 0.55f));
        r.strokeCircle(mx, my, Controls.STICK_RADIUS * 0.6f, 3f, Palette.alpha(Palette.PANEL_STROKE, 0.8f));
        float kx = mx + hud.moveStickDx * Controls.STICK_RADIUS * 0.6f;
        float ky = my + hud.moveStickDy * Controls.STICK_RADIUS * 0.6f;
        r.fillCircle(kx, ky, Controls.STICK_KNOB, Palette.alpha(Palette.TEXT, 0.75f));
        // 右摇杆
        float ax = hud.aimStickActive ? hud.aimStickCx : Controls.AIM_DEFAULT_X;
        float ay = hud.aimStickActive ? hud.aimStickCy : Controls.AIM_DEFAULT_Y;
        r.fillCircle(ax, ay, Controls.STICK_RADIUS * 0.5f, Palette.alpha(Palette.PANEL, 0.4f));
        r.fillCircle(ax + hud.aimStickDx * 70, ay + hud.aimStickDy * 70, Controls.STICK_KNOB * 0.8f,
                Palette.alpha(Palette.BLUE, 0.7f));
        // 攻击
        r.fillCircle(Controls.ATTACK_X, Controls.ATTACK_Y, Controls.ATTACK_R,
                Palette.alpha(Palette.RED, 0.7f + 0.2f * hud.attackBtn));
        r.setTextSize(34);
        r.setTextAlign(1);
        r.setTextColor(Palette.TEXT);
        r.drawText("攻击", Controls.ATTACK_X, Controls.ATTACK_Y);
        // 互动
        int ic = hud.interactAvailable ? Palette.MINT : Palette.PANEL_STROKE;
        r.fillCircle(Controls.INTERACT_X, Controls.INTERACT_Y, Controls.INTERACT_R, Palette.alpha(ic, 0.75f));
        r.setTextSize(22);
        r.drawText("互动", Controls.INTERACT_X, Controls.INTERACT_Y);
        // 闪避
        r.fillCircle(Controls.DODGE_X, Controls.DODGE_Y, Controls.DODGE_R, Palette.alpha(Palette.BLUE, 0.6f));
        r.setTextSize(22);
        r.drawText("闪避", Controls.DODGE_X, Controls.DODGE_Y);
        // 换武器 / 道具
        r.fillRoundRect(Controls.WEAPON_X, Controls.WEAPON_Y, Controls.WEAPON_W, Controls.WEAPON_H, 14,
                Palette.alpha(Palette.PANEL_ALT, 0.85f));
        r.setTextSize(22);
        r.setTextColor(Palette.TEXT);
        r.drawText("换武器", Controls.WEAPON_X + Controls.WEAPON_W / 2f, Controls.WEAPON_Y + Controls.WEAPON_H / 2f);
        r.fillRoundRect(Controls.ITEM_X, Controls.ITEM_Y, Controls.ITEM_W, Controls.ITEM_H, 14,
                Palette.alpha(Palette.PANEL_ALT, 0.85f));
        r.drawText("道具", Controls.ITEM_X + Controls.ITEM_W / 2f, Controls.ITEM_Y + Controls.ITEM_H / 2f);
        // 交互提示
        if (hud.interactPrompt != null) {
            r.fillRoundRect(230, 820, 260, 54, 14, Palette.alpha(Palette.PANEL, 0.9f));
            r.setTextSize(24);
            r.setTextAlign(1);
            r.setTextColor(Palette.MINT);
            r.drawText(hud.interactPrompt, 360, 847);
        }
    }
}
