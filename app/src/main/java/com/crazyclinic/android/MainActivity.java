package com.crazyclinic.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * 临时占位 Activity：真正的游戏视图由 android 代理实现（GameView + AndroidRenderer）。
 * 保留此类名与包名，避免 AndroidManifest 变更。
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        TextView tv = new TextView(this);
        tv.setText("疯狂诊所\n构建骨架已就绪");
        tv.setTextSize(28f);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setBackgroundColor(0xFF0E1014);
        tv.setTextColor(0xFFF2F4F7);
        setContentView(tv);
        hideSystemBars();
        tv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { }
        });
    }

    private void hideSystemBars() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
