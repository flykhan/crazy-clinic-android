package com.crazyclinic.core;

/** 音频接口：Android 用 SoundPool/MediaPlayer 实现，桌面预览用静音实现。 */
public interface Audio {
    void play(Sfx sfx);

    void music(Music track);

    void stopMusic();

    void setSoundEnabled(boolean enabled);

    void setMusicEnabled(boolean enabled);

    /** 短震动，毫秒；不支持的平台忽略。 */
    void vibrate(int millis);
}
