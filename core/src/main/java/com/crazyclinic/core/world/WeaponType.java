package com.crazyclinic.core.world;

/** 8 种武器 + 拳头。 */
public enum WeaponType {
    FIST("拳头"),
    AXE("斧头"),
    SCALPEL("手术刀"),
    PISTOL("手枪"),
    SHOTGUN("霰弹枪"),
    TASER("泰瑟枪"),
    DEFIBRILLATOR("除颤器"),
    BUNSEN("本生灯"),
    VIAL("爆炸试管");

    public final String label;

    WeaponType(String label) {
        this.label = label;
    }
}
