package com.crazyclinic.core.world;

public final class Pickup {
    public enum Kind { WEAPON, ITEM, ORGAN, KEYCARD }

    public Kind kind;
    public WeaponType weapon;
    public ItemType item;
    public Organ organ;
    public int amount = 1;
    public float x, y;
    public float bob;
    public boolean taken;

    public static Pickup weapon(WeaponType w, float x, float y) {
        Pickup p = new Pickup();
        p.kind = Kind.WEAPON;
        p.weapon = w;
        p.x = x;
        p.y = y;
        return p;
    }

    public static Pickup item(ItemType it, int amount, float x, float y) {
        Pickup p = new Pickup();
        p.kind = Kind.ITEM;
        p.item = it;
        p.amount = amount;
        p.x = x;
        p.y = y;
        return p;
    }

    public static Pickup organ(Organ organ, float x, float y) {
        Pickup p = new Pickup();
        p.kind = Kind.ORGAN;
        p.organ = organ;
        p.item = ItemType.ORGAN;
        p.x = x;
        p.y = y;
        return p;
    }

    public static Pickup keycard(float x, float y) {
        Pickup p = new Pickup();
        p.kind = Kind.KEYCARD;
        p.item = ItemType.KEYCARD;
        p.x = x;
        p.y = y;
        return p;
    }

    public String label() {
        switch (kind) {
            case WEAPON: return weapon.label;
            case ORGAN: return organ.label + "（永久升级）";
            case KEYCARD: return "钥匙卡";
            default: return item == null ? "物品" : item.label;
        }
    }
}
