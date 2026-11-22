package com.crazyclinic.core.world;

/** 器官永久升级（对应原版打死敌人掉落的器官）。 */
public enum Organ {
    BRAIN(1 << 0, "脑", "骇客时间 +2 秒，电脑破解更容易"),
    HEART(1 << 1, "心", "生命上限 +20"),
    KIDNEY(1 << 2, "肾", "生命上限 +10，毒抗 50%"),
    LEGS(1 << 3, "腿", "移动速度 +10%"),
    LIVER(1 << 4, "肝", "每秒缓慢回血 1 点"),
    LUNGS(1 << 5, "肺", "体力上限 +20"),
    ARMS(1 << 6, "双臂", "斧头/手术刀伤害 +15%");

    public final int bit;
    public final String label;
    public final String desc;

    Organ(int bit, String label, String desc) {
        this.bit = bit;
        this.label = label;
        this.desc = desc;
    }
}
