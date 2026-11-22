package com.crazyclinic.core.content;

import com.crazyclinic.core.world.LevelDef;
import com.crazyclinic.core.world.Organ;

/**
 * 10 层关卡数据（关卡代理负责替换本文件的手写地图）。
 * 图例见 docs/CONTRACT.md 与 LevelDef 注释。
 */
public final class Levels {
    public static final LevelDef[] FLOORS = buildPlaceholders();

    private Levels() {}

    public static LevelDef get(int floor) {
        int i = Math.max(1, Math.min(FLOORS.length, floor)) - 1;
        return FLOORS[i];
    }

    private static LevelDef[] buildPlaceholders() {
        String[][] names = {
                {"精神病学", "教学病区"},
                {"门诊·保健站", "人满为患"},
                {"急症室", "红色警报"},
                {"内科", "慢性死亡"},
                {"外科", "手术刀之夜"},
                {"放射学", "看不见的敌人"},
                {"心脏病·ICU", "心跳停止"},
                {"实验室", "造物主"},
                {"办事处·总部", "最后一扇门"},
                {"屋顶", "赞恩"}
        };
        String[] intros = {
                "你在精神病学的病床上醒来。\n休克疗法出了问题——现在，逃。",
                "门诊大厅挤满了人。\n他们看起来都不太对劲。",
                "急症室的灯忽明忽暗。\n你能听见远处电锯的声音。",
                "内科走廊弥漫着消毒水味。\n监控摄像头缓缓转了过来。",
                "外科层正在进行一场手术。\n病人不应该是你。",
                "放射学层没有开灯。\n黑暗中有人拖着铁链走过。",
                "重症监护室的心电监护仪突然全部归零。\n心跳声却还在靠近。",
                "实验室里摆满了培养槽。\n其中一个写着你的名字。",
                "总部办公楼。\n赞恩就在档案室等你。",
                "天台风雨交加。\n直升机在等你——前提是你能活着上去。"
        };
        LevelDef[] arr = new LevelDef[names.length];
        for (int i = 0; i < names.length; i++) {
            arr[i] = new LevelDef(i + 1, names[i][0], names[i][1], intros[i],
                    placeholderRows(i + 1, i == 9), defaultOrgans(i + 1));
        }
        return arr;
    }

    private static Organ[] defaultOrgans(int floor) {
        switch (floor) {
            case 2: return new Organ[]{Organ.HEART};
            case 3: return new Organ[]{Organ.LEGS};
            case 4: return new Organ[]{Organ.LUNGS};
            case 5: return new Organ[]{Organ.BRAIN};
            case 6: return new Organ[]{Organ.KIDNEY};
            case 7: return new Organ[]{Organ.LIVER};
            case 8: return new Organ[]{Organ.ARMS};
            default: return new Organ[]{Organ.BRAIN};
        }
    }

    /** 简易占位地图：保证出生点/钥匙/电脑/门/电梯/敌人/物资齐全，可被引擎正常加载。 */
    private static String[] placeholderRows(int floor, boolean boss) {
        String[] rows = new String[23];
        StringBuilder sb;
        for (int y = 0; y < rows.length; y++) {
            sb = new StringBuilder();
            for (int x = 0; x < 29; x++) {
                boolean border = x == 0 || y == 0 || x == 28 || y == 22;
                if (border) {
                    sb.append('#');
                } else if (x == 14 && y > 2 && y < 20 && y != 11) {
                    sb.append('#');
                } else {
                    sb.append('.');
                }
            }
            rows[y] = sb.toString();
        }
        char[] top = rows[3].toCharArray();
        for (int x = 2; x <= 8; x++) top[x] = '.';
        top[2] = 'S';
        rows[3] = new String(top);
        rows[4] = replace(rows[4], 6, 'L');
        rows[5] = replace(replace(rows[5], 10, 'k'), 2, 'm');
        rows[8] = replace(replace(rows[8], 6, 'C'), 18, '1');
        rows[10] = replace(rows[10], 22, 'g');
        rows[13] = replace(replace(rows[13], 4, '2'), 24, 'n');
        rows[16] = replace(rows[16], 20, 'E');
        rows[18] = replace(rows[18], 2, '+');
        if (boss) rows[19] = replace(rows[19], 24, 'Z');
        else rows[19] = replace(rows[19], 20, 'D');
        return rows;
    }

    private static String replace(String row, int x, char c) {
        if (x < 0 || x >= row.length()) return row;
        char[] cs = row.toCharArray();
        cs[x] = c;
        return new String(cs);
    }

    private static String[] emptyRowsCompatibility() {
        return new String[0];
    }
}
