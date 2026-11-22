package com.crazyclinic.core;

/** 简单矩形（左上角 + 宽高）。 */
public final class Rect {
    public float x, y, w, h;

    public Rect() {}

    public Rect(float x, float y, float w, float h) {
        set(x, y, w, h);
    }

    public Rect set(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        return this;
    }

    public Rect set(Rect o) {
        return set(o.x, o.y, o.w, o.h);
    }

    public float right() {
        return x + w;
    }

    public float bottom() {
        return y + h;
    }

    public float cx() {
        return x + w * 0.5f;
    }

    public float cy() {
        return y + h * 0.5f;
    }

    public boolean contains(float px, float py) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    public Rect inflate(float d) {
        return new Rect(x - d, y - d, w + d * 2, h + d * 2);
    }

    public boolean intersects(Rect o) {
        return x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y;
    }
}
