package com.crazyclinic.core;

/** 存档读写接口（Android: SharedPreferences；桌面: 内存或文件）。 */
public interface SaveStore {
    String load();

    void save(String data);
}
