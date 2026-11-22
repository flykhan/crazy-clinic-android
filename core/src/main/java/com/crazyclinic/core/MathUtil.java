package com.crazyclinic.core;

import java.util.Random;

/** 通用数学/缓动工具。 */
public final class MathUtil {
    public static final float PI = (float) Math.PI;
    public static final float TAU = (float) (Math.PI * 2);
    private static final Random RNG = new Random();

    private MathUtil() {}

    public static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** 与帧率无关的指数趋近。 */
    public static float approach(float current, float target, float ratePerSecond, float dt) {
        float k = 1f - (float) Math.exp(-ratePerSecond * dt);
        return current + (target - current) * k;
    }

    public static float easeOutCubic(float t) {
        t = clamp(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    public static float easeInOutQuad(float t) {
        t = clamp(t, 0f, 1f);
        return t < 0.5f ? 2f * t * t : 1f - 2f * (1f - t) * (1f - t);
    }

    public static float easeOutBack(float t) {
        t = clamp(t, 0f, 1f);
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    public static float dist(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public static float dist2(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    public static float angle(float x1, float y1, float x2, float y2) {
        return (float) Math.atan2(y2 - y1, x2 - x1);
    }

    public static float angleDelta(float a, float b) {
        float d = (b - a) % TAU;
        if (d > PI) d -= TAU;
        if (d < -PI) d += TAU;
        return d;
    }

    public static float approachAngle(float current, float target, float maxDelta) {
        float d = angleDelta(current, target);
        if (d > maxDelta) d = maxDelta;
        if (d < -maxDelta) d = -maxDelta;
        return current + d;
    }

    public static float rand(float min, float max) {
        return min + RNG.nextFloat() * (max - min);
    }

    public static int randInt(int min, int maxInclusive) {
        return min + RNG.nextInt(maxInclusive - min + 1);
    }

    public static boolean chance(float p) {
        return RNG.nextFloat() < p;
    }

    public static float sign(float v) {
        return v < 0 ? -1f : (v > 0 ? 1f : 0f);
    }

    /** 简单的 0..1 哈希噪声，用于绘制污渍等细节。 */
    public static float hash01(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        h = h ^ (h >> 16);
        return (h & 0x7fffffff) / (float) 0x7fffffff;
    }

    public static float smoothstep(float t) {
        t = clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }
}
