package com.crazyclinic.preview;

import com.crazyclinic.core.SaveStore;

/** 桌面预览用的内存存档：可选从文件预载/回写，默认纯内存。 */
public final class MemSaveStore implements SaveStore {

    private String data;

    public MemSaveStore() {
        this(null);
    }

    public MemSaveStore(String initial) {
        this.data = initial;
    }

    @Override
    public String load() {
        return data;
    }

    @Override
    public void save(String data) {
        this.data = data;
    }

    public String raw() {
        return data;
    }
}
