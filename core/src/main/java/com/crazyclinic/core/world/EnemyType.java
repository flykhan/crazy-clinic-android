package com.crazyclinic.core.world;

public enum EnemyType {
    GUARD("看守人"),
    DOCTOR("医生"),
    FLU_PATIENT("感冒病人"),
    TECH("中心助手"),
    NURSE("护士"),
    SENTRY("哨兵枪"),
    BOSS("赞恩");

    public final String label;

    EnemyType(String label) {
        this.label = label;
    }
}
