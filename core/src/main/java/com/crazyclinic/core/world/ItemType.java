package com.crazyclinic.core.world;

public enum ItemType {
    MEDKIT_SMALL("小医药包"),
    MEDKIT("医药包"),
    MEDKIT_LARGE("大医药包"),
    PAINKILLER("止痛药"),
    STEROID("类固醇"),
    ENERGY_DRINK("能量饮料"),
    EPO("红细胞生成素"),
    INVISIBILITY("隐形药"),
    KEYCARD("钥匙卡"),
    ORGAN("器官"),
    AMMO("弹药");

    public final String label;

    ItemType(String label) {
        this.label = label;
    }
}
