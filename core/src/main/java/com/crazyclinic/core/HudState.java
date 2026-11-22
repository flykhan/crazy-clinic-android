package com.crazyclinic.core;

/**
 * GameScene 每帧写入、HudRenderer 读取的临时 UI 状态。
 * 引擎代理负责填充，美术代理负责绘制。
 */
public final class HudState {
    // 左摇杆（移动）
    public boolean moveStickActive;
    public float moveStickCx, moveStickCy, moveStickDx, moveStickDy;

    // 右摇杆（瞄准/开火）
    public boolean aimStickActive;
    public float aimStickCx, aimStickCy, aimStickDx, aimStickDy;

    // 右侧按钮状态（0..1 按压动画）
    public float attackBtn, interactBtn, dodgeBtn, weaponBtn, itemBtn;
    public boolean attackHeld;

    // 交互提示（如“开锁柜”“使用电脑”“进入电梯”）
    public String interactPrompt;
    public boolean interactAvailable;

    // 临时提示条
    public String toast;
    public float toastTimer;

    // 屏幕反馈
    public float damageFlash;   // 红屏 0..1
    public float healFlash;     // 绿屏 0..1
    public float lowHealth;     // 0..1

    // 暂停
    public boolean paused;

    // 骇客小游戏
    public boolean hacking;
    public int hackCols, hackRows;
    public int hackCursorX, hackCursorY;
    public int hackStartX, hackStartY;
    public int hackGoalX, hackGoalY;
    public float hackTimeLeft, hackTimeTotal;
    public boolean hackFailed, hackSuccess;
    public float hackResultTimer;
    /** 单元格类型：0 可走, 1 墙/阻挡, 2 弹回起点, 3 触发警报 */
    public int[] hackCells;

    // 电梯/过关动画
    public float floorClearTimer;
    public boolean showingFloorClear;

    // BOSS 血条（0 表示不显示）
    public float bossHp = 0f, bossMaxHp = 1f;
    public String bossName;

    public void reset() {
        moveStickActive = false;
        aimStickActive = false;
        attackBtn = interactBtn = dodgeBtn = weaponBtn = itemBtn = 0f;
        attackHeld = false;
        interactPrompt = null;
        interactAvailable = false;
        paused = false;
        hacking = false;
        bossHp = 0f;
        showingFloorClear = false;
    }
}
