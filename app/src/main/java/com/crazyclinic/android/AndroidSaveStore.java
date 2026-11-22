package com.crazyclinic.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.crazyclinic.core.SaveStore;

/** SharedPreferences 单行字符串存档；key 固定，跨版本由 SaveData.serialize 兼容。 */
public final class AndroidSaveStore implements SaveStore {

    private static final String PREFS = "crazyclinic";
    private static final String KEY_SAVE = "save_v1";

    private final SharedPreferences prefs;

    public AndroidSaveStore(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public String load() {
        try {
            return prefs.getString(KEY_SAVE, null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void save(String data) {
        try {
            prefs.edit().putString(KEY_SAVE, data).apply();
        } catch (Exception ignored) {
        }
    }

    /** 清空存档（设置里的“清除进度”）。 */
    public void clear() {
        try {
            prefs.edit().remove(KEY_SAVE).apply();
        } catch (Exception ignored) {
        }
    }
}
