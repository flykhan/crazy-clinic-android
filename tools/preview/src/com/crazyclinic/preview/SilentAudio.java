package com.crazyclinic.preview;

import com.crazyclinic.core.Audio;
import com.crazyclinic.core.Music;
import com.crazyclinic.core.Sfx;

/** 桌面预览用的静音 Audio：只记录最近事件，方便调试。 */
public final class SilentAudio implements Audio {

    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    private Music current;
    private Sfx lastSfx;
    private final boolean verbose;

    public SilentAudio() {
        this(false);
    }

    public SilentAudio(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void play(Sfx sfx) {
        lastSfx = sfx;
        if (verbose && sfx != null) System.out.println("[preview] sfx " + sfx);
    }

    @Override
    public void music(Music track) {
        current = track;
        if (verbose && track != null) System.out.println("[preview] music " + track);
    }

    @Override
    public void stopMusic() {
        current = null;
    }

    @Override
    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    @Override
    public void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
    }

    @Override
    public void vibrate(int millis) {
        if (verbose && millis > 0) System.out.println("[preview] vibrate " + millis + "ms");
    }

    public boolean soundEnabled() {
        return soundEnabled;
    }

    public boolean musicEnabled() {
        return musicEnabled;
    }

    public Music currentMusic() {
        return current;
    }

    public Sfx lastSfx() {
        return lastSfx;
    }
}
