package com.crazyclinic.core;

import com.crazyclinic.core.world.Organ;

/** 永久存档（关卡解锁 / 器官永久升级 / 设置 / 统计）。 */
public final class SaveData {
    public int unlockedFloor = 1;      // 已解锁的最高层，1..Game.FLOOR_COUNT
    public int organsMask = 0;         // 已获得器官位掩码
    public int totalKills = 0;
    public int deaths = 0;
    public int totalPlaySeconds = 0;
    public boolean soundOn = true;
    public boolean musicOn = true;
    public boolean vibrateOn = true;
    public boolean tutorialSeen = false;

    public boolean hasOrgan(Organ organ) {
        return (organsMask & organ.bit) != 0;
    }

    public void addOrgan(Organ organ) {
        organsMask |= organ.bit;
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder("v1");
        sb.append('|').append(unlockedFloor);
        sb.append('|').append(organsMask);
        sb.append('|').append(totalKills);
        sb.append('|').append(deaths);
        sb.append('|').append(totalPlaySeconds);
        sb.append('|').append(soundOn ? 1 : 0);
        sb.append('|').append(musicOn ? 1 : 0);
        sb.append('|').append(vibrateOn ? 1 : 0);
        sb.append('|').append(tutorialSeen ? 1 : 0);
        return sb.toString();
    }

    public static SaveData parse(String data) {
        SaveData s = new SaveData();
        if (data == null) return s;
        try {
            String[] p = data.trim().split("\\|");
            if (p.length < 2 || !"v1".equals(p[0])) return s;
            s.unlockedFloor = clampInt(parseInt(p, 1, 1), 1, Game.FLOOR_COUNT);
            s.organsMask = parseInt(p, 2, 0);
            s.totalKills = parseInt(p, 3, 0);
            s.deaths = parseInt(p, 4, 0);
            s.totalPlaySeconds = parseInt(p, 5, 0);
            s.soundOn = parseInt(p, 6, 1) != 0;
            s.musicOn = parseInt(p, 7, 1) != 0;
            s.vibrateOn = parseInt(p, 8, 1) != 0;
            s.tutorialSeen = parseInt(p, 9, 0) != 0;
        } catch (Exception ignored) {
        }
        return s;
    }

    private static int parseInt(String[] p, int i, int def) {
        if (i >= p.length) return def;
        try {
            return Integer.parseInt(p[i].trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static int clampInt(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
