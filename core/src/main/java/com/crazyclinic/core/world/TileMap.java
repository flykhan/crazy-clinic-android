package com.crazyclinic.core.world;

/** 由 ASCII 行构造的瓦片地图。 */
public final class TileMap {
    public static final int TILE = 72;

    public final int width;
    public final int height;
    private final byte[] tiles;

    public TileMap(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.tiles = new byte[this.width * this.height];
        for (int i = 0; i < tiles.length; i++) tiles[i] = (byte) TileType.FLOOR.ordinal();
    }

    public byte raw(int tx, int ty) {
        if (!inBounds(tx, ty)) return (byte) TileType.WALL.ordinal();
        return tiles[ty * width + tx];
    }

    public TileType type(int tx, int ty) {
        return TileType.values()[raw(tx, ty) & 0xff];
    }

    public void set(int tx, int ty, TileType t) {
        if (inBounds(tx, ty)) tiles[ty * width + tx] = (byte) t.ordinal();
    }

    public boolean inBounds(int tx, int ty) {
        return tx >= 0 && ty >= 0 && tx < width && ty < height;
    }

    public boolean solid(int tx, int ty) {
        return type(tx, ty).solid;
    }

    public boolean opaque(int tx, int ty) {
        return type(tx, ty).opaque;
    }

    public boolean solidAt(float wx, float wy) {
        return solid((int) Math.floor(wx / TILE), (int) Math.floor(wy / TILE));
    }

    public boolean opaqueAt(float wx, float wy) {
        return opaque((int) Math.floor(wx / TILE), (int) Math.floor(wy / TILE));
    }

    public int tx(float wx) {
        return (int) Math.floor(wx / TILE);
    }

    public int ty(float wy) {
        return (int) Math.floor(wy / TILE);
    }

    public float cx(int tx) {
        return tx * TILE + TILE * 0.5f;
    }

    public float cy(int ty) {
        return ty * TILE + TILE * 0.5f;
    }

    public float pixelWidth() {
        return width * (float) TILE;
    }

    public float pixelHeight() {
        return height * (float) TILE;
    }

    /** 静态构造：按最大行宽补齐，未知字符按地板处理。 */
    public static TileMap parse(String[] rows) {
        int w = 1;
        for (String row : rows) w = Math.max(w, row.length());
        TileMap m = new TileMap(w, rows.length);
        for (int y = 0; y < rows.length; y++) {
            String row = rows[y];
            for (int x = 0; x < w; x++) {
                char c = x < row.length() ? row.charAt(x) : '#';
                TileType t = TileType.fromChar(c);
                m.set(x, y, t == null ? TileType.FLOOR : t);
            }
        }
        return m;
    }
}
