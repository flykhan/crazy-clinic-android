package com.crazyclinic.android;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.crazyclinic.core.Audio;
import com.crazyclinic.core.Music;
import com.crazyclinic.core.Sfx;

import java.util.Locale;

/**
 * Android 音频实现：SoundPool 播 Sfx（res/raw/sfx_*.wav），MediaPlayer 循环 Music（res/raw/mus_*.wav）。
 * 资源由 tools/audio/gen_audio.py 程序化生成；缺失的音效静默忽略。
 */
public final class AndroidAudio implements Audio {

    private static final String TAG = "CrazyClinicAudio";
    private static final float MUSIC_VOLUME = 0.55f;
    private static final float SFX_VOLUME = 0.85f;

    private final Context context;
    private final SoundPool soundPool;
    private final int[] sfxIds;

    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    private boolean vibrateEnabled = true;

    private MediaPlayer musicPlayer;
    private Music currentTrack;
    private boolean paused;

    private final Vibrator vibrator;

    public AndroidAudio(Context context) {
        this.context = context.getApplicationContext();
        this.sfxIds = new int[Sfx.values().length];
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(8);
        builder.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        soundPool = builder.build();
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool sp, int sampleId, int status) {
                if (status != 0) Log.w(TAG, "sound load failed sampleId=" + sampleId + " status=" + status);
            }
        });
        for (Sfx sfx : Sfx.values()) {
            int res = resId("sfx_" + sfx.name().toLowerCase(Locale.ROOT));
            if (res != 0) {
                sfxIds[sfx.ordinal()] = soundPool.load(context, res, 1);
            }
        }
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private int resId(String name) {
        return context.getResources().getIdentifier(name, "raw", context.getPackageName());
    }

    @Override
    public void play(Sfx sfx) {
        if (!soundEnabled || sfx == null || soundPool == null || paused) return;
        int id = sfxIds[sfx.ordinal()];
        if (id == 0) return;
        try {
            soundPool.play(id, SFX_VOLUME, SFX_VOLUME, 1, 0, 1f);
        } catch (Exception e) {
            Log.w(TAG, "play failed: " + sfx, e);
        }
    }

    @Override
    public void music(Music track) {
        if (currentTrack == track && musicPlayer != null) {
            if (musicEnabled && !musicPlayer.isPlaying()) {
                try {
                    musicPlayer.start();
                } catch (Exception ignored) {
                }
            }
            return;
        }
        stopMusic();
        currentTrack = track;
        if (track == null || !musicEnabled) return;
        int res = resId("mus_" + track.name().toLowerCase(Locale.ROOT));
        if (res == 0) {
            Log.w(TAG, "missing music resource for " + track);
            return;
        }
        try {
            MediaPlayer mp = new MediaPlayer();
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(res);
            if (afd == null) {
                mp.release();
                return;
            }
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setLooping(true);
            mp.setVolume(MUSIC_VOLUME, MUSIC_VOLUME);
            mp.prepare();
            mp.start();
            musicPlayer = mp;
        } catch (Exception e) {
            Log.w(TAG, "music start failed: " + track, e);
            stopMusic();
        }
    }

    @Override
    public void stopMusic() {
        releasePlayer();
        currentTrack = null;
    }

    /** 只释放播放器，不清空“当前曲目”意图（用于静音开关恢复）。 */
    private void releasePlayer() {
        if (musicPlayer != null) {
            try {
                if (musicPlayer.isPlaying()) musicPlayer.stop();
            } catch (Exception ignored) {
            }
            try {
                musicPlayer.release();
            } catch (Exception ignored) {
            }
            musicPlayer = null;
        }
    }

    @Override
    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    @Override
    public void setMusicEnabled(boolean enabled) {
        if (enabled == musicEnabled) return;
        musicEnabled = enabled;
        if (!enabled) {
            // 记住当前曲目，重新打开音乐时恢复
            Music keep = currentTrack;
            releasePlayer();
            currentTrack = keep;
        } else if (currentTrack != null) {
            Music track = currentTrack;
            currentTrack = null;
            music(track);
        }
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setVibrateEnabled(boolean enabled) {
        vibrateEnabled = enabled;
    }

    @Override
    public void vibrate(int millis) {
        if (!vibrateEnabled || millis <= 0 || vibrator == null) return;
        if (!vibrator.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //noinspection deprecation
                vibrator.vibrate(millis);
            }
        } catch (Exception e) {
            Log.w(TAG, "vibrate failed", e);
        }
    }

    /** Activity.onPause：暂停 BGM / 停止播放短音。 */
    public void onPause() {
        paused = true;
        if (musicPlayer != null) {
            try {
                if (musicPlayer.isPlaying()) musicPlayer.pause();
            } catch (Exception ignored) {
            }
        }
        if (soundPool != null) {
            try {
                soundPool.autoPause();
            } catch (Exception ignored) {
            }
        }
    }

    /** Activity.onResume：恢复 BGM。 */
    public void onResume() {
        paused = false;
        if (soundPool != null) {
            try {
                soundPool.autoResume();
            } catch (Exception ignored) {
            }
        }
        if (musicEnabled && musicPlayer != null) {
            try {
                if (!musicPlayer.isPlaying()) musicPlayer.start();
            } catch (Exception ignored) {
            }
        }
    }

    public void release() {
        stopMusic();
        try {
            soundPool.release();
        } catch (Exception ignored) {
        }
    }
}
