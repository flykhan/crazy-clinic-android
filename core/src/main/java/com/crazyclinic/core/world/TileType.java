package com.crazyclinic.core.world;

/**
 * 地图瓦片。ASCII 图例：
 * '#' 墙  '.' 地板  '+' 锁着的门  '-' 敞开的门  'E' 电梯  'C' 安保电脑
 * 'L' 储物柜  'X' 箱子  'B' 病床  'W' 窗户墙  'S' 玩家出生点
 */
public enum TileType {
    FLOOR('.', false, false),
    WALL('#', true, true),
    DOOR_LOCKED('+', true, true),
    DOOR_OPEN('-', false, false),
    ELEVATOR('E', false, false),
    COMPUTER('C', true, true),
    LOCKER('L', true, true),
    CRATE('X', true, true),
    BED('B', true, false),
    WINDOW('W', true, true),
    SPAWN('S', false, false),
    UNKNOWN('?', false, false);

    public final char ch;
    public final boolean solid;
    public final boolean opaque;

    TileType(char ch, boolean solid, boolean opaque) {
        this.ch = ch;
        this.solid = solid;
        this.opaque = opaque;
    }

    public static TileType fromChar(char c) {
        switch (c) {
            case '#': return WALL;
            case '+': return DOOR_LOCKED;
            case '-': return DOOR_OPEN;
            case 'E': return ELEVATOR;
            case 'C': return COMPUTER;
            case 'L': return LOCKER;
            case 'X': return CRATE;
            case 'B': return BED;
            case 'W': return WINDOW;
            case 'S': return SPAWN;
            case '.': return FLOOR;
            default: return null;
        }
    }
}
