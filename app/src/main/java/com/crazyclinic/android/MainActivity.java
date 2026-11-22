package com.crazyclinic.android;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import com.crazyclinic.core.Game;

/**
 * 游戏入口：沉浸式全屏（隐藏状态栏/导航栏）+ 竖屏 + 防熄屏，
 * 创建 Game（SharedPreferences 存档 + 合成音频）与 GameView（SurfaceView 渲染循环）。
 */
public class MainActivity extends Activity {

    private AndroidAudio audio;
    private AndroidSaveStore store;
    private Game game;
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Android 10+：当系统栏透明时不要叠加对比度遮罩（否则手势导航条会有一条亮边）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
            window.setStatusBarContrastEnforced(false);
        }
        // 刘海屏：允许内容延伸到短边（竖屏时即顶部刘海区域）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(lp);
        }
        // API 30+：内容延伸到系统栏区域；低版本用 layout flags 兜底
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        store = new AndroidSaveStore(this);
        audio = new AndroidAudio(this);
        game = new Game(audio, store);
        gameView = new GameView(this, game, audio);
        setContentView(gameView);

        // 刘海/安全区：把 720x1280 内容居中在安全区内（物理像素）
        final View decor = window.getDecorView();
        decor.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                applySafeInsets(insets);
                return insets;
            }
        });
        decor.post(new Runnable() {
            @Override
            public void run() {
                WindowInsets insets = decor.getRootWindowInsets();
                if (insets != null) applySafeInsets(insets);
            }
        });

        game.gotoTitle();
    }

    private void applySafeInsets(WindowInsets insets) {
        int left = 0, top = 0, right = 0, bottom = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.graphics.Insets sb = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            left = sb.left;
            top = sb.top;
            right = sb.right;
            bottom = sb.bottom;
        } else {
            left = insets.getSystemWindowInsetLeft();
            top = insets.getSystemWindowInsetTop();
            right = insets.getSystemWindowInsetRight();
            bottom = insets.getSystemWindowInsetBottom();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                android.view.DisplayCutout cutout = insets.getDisplayCutout();
                if (cutout != null) {
                    left = Math.max(left, cutout.getSafeInsetLeft());
                    top = Math.max(top, cutout.getSafeInsetTop());
                    right = Math.max(right, cutout.getSafeInsetRight());
                    bottom = Math.max(bottom, cutout.getSafeInsetBottom());
                }
            }
        }
        // 安全区过大时（某些 ROM 全屏仍报状态栏），限制在屏幕短边 12% 内，避免画面被压得太小
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int maxSide = (int) (Math.min(dm.widthPixels, dm.heightPixels) * 0.12f);
        left = Math.min(left, maxSide);
        right = Math.min(right, maxSide);
        top = Math.min(top, maxSide);
        bottom = Math.min(bottom, maxSide);
        gameView.setSafeInsets(left, top, right, bottom);
    }

    private void hideSystemBars() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        audio.onResume();
        audio.setVibrateEnabled(game.save.vibrateOn);
        game.resume();
        if (gameView != null) gameView.onHostResume();
    }

    @Override
    protected void onPause() {
        if (gameView != null) gameView.onHostPause();
        game.pause();
        game.saveNow();
        audio.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) gameView.onHostDestroy();
        game.saveNow();
        if (audio != null) audio.release();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // 标题页再按返回 = 退出应用；其余场景交给 Scene（暂停/返回上级菜单）
        if (game.scene instanceof com.crazyclinic.core.ui.TitleScene) {
            finish();
            return;
        }
        game.backPressed();
    }
}
