package com.crazyclinic.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.crazyclinic.core.Game;

import java.util.ArrayDeque;
import java.util.Locale;

/**
 * SurfaceView + 独立渲染线程：固定步长 1/60s 更新（最多补 3 帧），每帧 lockCanvas 绘制。
 * <p>
 * 画布把 720x1280 逻辑分辨率等比缩放并居中（黑边），触摸坐标反变换回逻辑坐标；
 * 支持多点触控（DOWN/MOVE/UP/POINTER_DOWN/POINTER_UP/CANCEL），pointerId 透传给 Game。
 */
public final class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final float STEP = 1f / 60f;
    private static final long FRAME_NANOS = 16_666_667L;
    private static final int MAX_CATCHUP_STEPS = 3;

    private final Game game;
    private final AndroidAudio audio;
    private final AndroidRenderer renderer = new AndroidRenderer();

    private Thread renderThread;
    private volatile boolean running;
    private volatile boolean surfaceReady;
    private volatile boolean showFps;

    // 逻辑 -> 屏幕变换（物理像素）
    private volatile float viewScale = 1f;
    private volatile float viewOffX;
    private volatile float viewOffY;

    // 刘海/手势安全区（物理像素）
    private volatile int insetLeft;
    private volatile int insetTop;
    private volatile int insetRight;
    private volatile int insetBottom;

    private final Paint fpsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private volatile float lastFps;

    private static final int EV_DOWN = 0;
    private static final int EV_MOVE = 1;
    private static final int EV_UP = 2;

    private static final class TouchEvent {
        final int type;
        final int id;
        final float rawX;
        final float rawY;

        TouchEvent(int type, int id, float rawX, float rawY) {
            this.type = type;
            this.id = id;
            this.rawX = rawX;
            this.rawY = rawY;
        }
    }

    private final ArrayDeque<TouchEvent> touchQueue = new ArrayDeque<>();
    private final Object touchLock = new Object();

    public GameView(Context context, Game game, AndroidAudio audio) {
        super(context);
        this.game = game;
        this.audio = audio;
        getHolder().addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setKeepScreenOn(true);
        fpsPaint.setColor(0xFFFFFF66);
        fpsPaint.setTextSize(30f);
        fpsPaint.setTypeface(Typeface.create("monospace", Typeface.BOLD));
    }

    public void setShowFps(boolean show) {
        showFps = show;
    }

    /** MainActivity 传入刘海/系统栏安全区（物理像素），用于把 720x1280 内容居中在安全区内。 */
    public void setSafeInsets(int left, int top, int right, int bottom) {
        this.insetLeft = left;
        this.insetTop = top;
        this.insetRight = right;
        this.insetBottom = bottom;
    }

    // ------------------------------------------------------------------
    // Activity 生命周期
    // ------------------------------------------------------------------

    public void onHostPause() {
        stopLoop();
        game.pause();
    }

    public void onHostResume() {
        if (surfaceReady && renderThread == null) {
            startLoop();
        }
    }

    public void onHostDestroy() {
        stopLoop();
    }

    // ------------------------------------------------------------------
    // SurfaceHolder.Callback
    // ------------------------------------------------------------------

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        startLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        stopLoop();
    }

    private synchronized void startLoop() {
        if (renderThread != null) return;
        running = true;
        renderThread = new Thread(this, "CrazyClinic-Render");
        renderThread.start();
    }

    private synchronized void stopLoop() {
        running = false;
        Thread t = renderThread;
        renderThread = null;
        if (t != null && t != Thread.currentThread()) {
            try {
                t.join(800);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ------------------------------------------------------------------
    // 渲染循环
    // ------------------------------------------------------------------

    @Override
    public void run() {
        long last = System.nanoTime();
        long fpsWindow = last;
        int fpsFrames = 0;
        double acc = 0;
        while (running) {
            long now = System.nanoTime();
            double elapsed = (now - last) / 1e9;
            last = now;
            if (elapsed > 0.25) elapsed = 0.25;   // 后台恢复后不要一次补太多
            acc += elapsed;

            int steps = 0;
            while (acc >= STEP && steps < MAX_CATCHUP_STEPS) {
                drainTouchEvents();
                game.update(STEP);
                acc -= STEP;
                steps++;
            }
            if (acc > STEP * MAX_CATCHUP_STEPS) acc = 0;   // 落后太多直接丢弃，避免越补越慢

            renderFrame();

            fpsFrames++;
            long t = System.nanoTime();
            if (t - fpsWindow >= 500_000_000L) {
                lastFps = fpsFrames * 1e9f / (t - fpsWindow);
                fpsFrames = 0;
                fpsWindow = t;
            }

            long frameNanos = System.nanoTime() - now;
            long sleepNanos = FRAME_NANOS - frameNanos;
            if (sleepNanos > 1_000_000L) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void renderFrame() {
        if (!surfaceReady) return;
        SurfaceHolder holder = getHolder();
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas();
        } catch (Exception ignored) {
            return;
        }
        if (canvas == null) return;
        try {
            int w = canvas.getWidth();
            int h = canvas.getHeight();
            computeTransform(w, h);
            canvas.drawColor(0xFF000000);
            canvas.save();
            canvas.translate(viewOffX, viewOffY);
            canvas.scale(viewScale, viewScale);
            renderer.setCanvas(canvas);
            game.render(renderer);
            canvas.restore();
            if (showFps) {
                canvas.drawText(String.format(Locale.ROOT, "%.0f FPS", lastFps), 16f, 34f, fpsPaint);
            }
        } catch (Exception e) {
            // 单帧绘制异常不应杀死渲染线程，但必须能诊断
            android.util.Log.e("CCGameView", "render exception", e);
        } finally {
            try {
                holder.unlockCanvasAndPost(canvas);
            } catch (Exception ignored) {
            }
        }
    }

    private void computeTransform(int w, int h) {
        int availW = Math.max(1, w - insetLeft - insetRight);
        int availH = Math.max(1, h - insetTop - insetBottom);
        float sx = availW / com.crazyclinic.core.Renderer.WIDTH;
        float sy = availH / com.crazyclinic.core.Renderer.HEIGHT;
        float scale = Math.min(sx, sy);
        if (scale <= 0f || Float.isNaN(scale)) scale = 1f;
        viewScale = scale;
        viewOffX = insetLeft + (availW - com.crazyclinic.core.Renderer.WIDTH * scale) * 0.5f;
        viewOffY = insetTop + (availH - com.crazyclinic.core.Renderer.HEIGHT * scale) * 0.5f;
    }

    // ------------------------------------------------------------------
    // 触摸
    // ------------------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                enqueue(EV_DOWN, event.getPointerId(0), event.getX(0), event.getY(0));
                return true;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                int i = event.getActionIndex();
                enqueue(EV_DOWN, event.getPointerId(i), event.getX(i), event.getY(i));
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                int count = event.getPointerCount();
                for (int i = 0; i < count; i++) {
                    enqueue(EV_MOVE, event.getPointerId(i), event.getX(i), event.getY(i));
                }
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                int i = event.getActionIndex();
                enqueue(EV_UP, event.getPointerId(i), event.getX(i), event.getY(i));
                return true;
            }
            case MotionEvent.ACTION_UP: {
                enqueue(EV_UP, event.getPointerId(0), event.getX(0), event.getY(0));
                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                int count = event.getPointerCount();
                for (int i = 0; i < count; i++) {
                    enqueue(EV_UP, event.getPointerId(i), event.getX(i), event.getY(i));
                }
                return true;
            }
            default:
                return true;
        }
    }

    private void enqueue(int type, int id, float rawX, float rawY) {
        synchronized (touchLock) {
            touchQueue.add(new TouchEvent(type, id, rawX, rawY));
        }
    }

    /** 在渲染线程内消费触摸队列，保证 update 与输入之间没有数据竞争。 */
    private void drainTouchEvents() {
        TouchEvent ev;
        while (true) {
            synchronized (touchLock) {
                ev = touchQueue.poll();
            }
            if (ev == null) return;
            float scale = viewScale;
            float lx = (ev.rawX - viewOffX) / scale;
            float ly = (ev.rawY - viewOffY) / scale;
            if (lx < 0f) lx = 0f;
            else if (lx > com.crazyclinic.core.Renderer.WIDTH) lx = com.crazyclinic.core.Renderer.WIDTH;
            if (ly < 0f) ly = 0f;
            else if (ly > com.crazyclinic.core.Renderer.HEIGHT) ly = com.crazyclinic.core.Renderer.HEIGHT;
            switch (ev.type) {
                case EV_DOWN:
                    game.touchDown(ev.id, lx, ly);
                    break;
                case EV_MOVE:
                    game.touchMove(ev.id, lx, ly);
                    break;
                case EV_UP:
                    game.touchUp(ev.id, lx, ly);
                    break;
                default:
                    break;
            }
        }
    }
}
