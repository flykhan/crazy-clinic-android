package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;

/**
 * 可复用菜单按钮：按压缩放 / 禁用态 / 入场动画 / 多指追踪。
 *
 * <p>使用方式：场景在 enter/构造里 {@link #set}，update 里 {@link #update(float)}，
 * render 里 {@link #draw}，触摸回调里 {@link #down}/{@link #up}。
 * 命中/点击音效由场景负责。</p>
 */
public final class UiButton {
    public final Rect rect = new Rect();
    public String label = "";
    public int style = UiKit.STYLE_SECONDARY;
    public boolean enabled = true;
    public boolean visible = true;

    /** 入场动画：0..1，场景里置 0 后由 update 自动缓入。 */
    public float appear = 1f;
    public float appearDelay = 0f;

    private boolean pressed;
    private int pointer = -1;
    private float pressAnim;
    private float age;

    public UiButton() {}

    public UiButton(String label, int style) {
        this.label = label;
        this.style = style;
    }

    public UiButton set(float x, float y, float w, float h) {
        rect.set(x, y, w, h);
        return this;
    }

    public UiButton label(String label) {
        this.label = label;
        return this;
    }

    public UiButton style(int style) {
        this.style = style;
        return this;
    }

    public UiButton enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public UiButton appearDelay(float delay) {
        this.appearDelay = delay;
        return this;
    }

    /** 重置入场动画（场景 enter 时调用）。 */
    public void resetAppear() {
        appear = 0f;
        age = 0f;
    }

    public boolean contains(float x, float y) {
        return rect.contains(x, y);
    }

    public boolean isPressed() {
        return pressed;
    }

    public int pointer() {
        return pointer;
    }

    public float pressAnim() {
        return pressAnim;
    }

    /** 尝试捕获按下；已禁用或不在矩形内返回 false。 */
    public boolean down(int id, float x, float y) {
        if (!enabled || !visible || pointer >= 0) return false;
        if (!contains(x, y)) return false;
        pointer = id;
        pressed = true;
        return true;
    }

    /** 松开；返回是否构成一次有效点击（同一指针且仍在按钮内）。 */
    public boolean up(int id, float x, float y) {
        if (id != pointer) return false;
        boolean click = enabled && contains(x, y);
        pointer = -1;
        pressed = false;
        return click;
    }

    public void cancel(int id) {
        if (id == pointer) {
            pointer = -1;
            pressed = false;
        }
    }

    public void cancelAll() {
        pointer = -1;
        pressed = false;
    }

    public void update(float dt) {
        age += dt;
        if (age >= appearDelay && appear < 1f) {
            appear = MathUtil.approach(appear, 1f, 9f, dt);
            if (appear > 0.995f) appear = 1f;
        }
        pressAnim = MathUtil.approach(pressAnim, pressed ? 1f : 0f, 20f, dt);
    }

    public void draw(Renderer r, float time) {
        if (!visible) return;
        UiKit.drawButton(r, rect, label, style, enabled, pressAnim, appear, time);
    }

    public void draw(Renderer r) {
        draw(r, 0f);
    }
}
