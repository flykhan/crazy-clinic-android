package com.crazyclinic.core.world;

public final class ItemSpawn {
    public final Pickup.Kind kind;
    public final WeaponType weapon;
    public final ItemType item;
    public final Organ organ;
    public final int amount;
    public final int tx, ty;

    private ItemSpawn(Pickup.Kind kind, WeaponType weapon, ItemType item, Organ organ, int amount, int tx, int ty) {
        this.kind = kind;
        this.weapon = weapon;
        this.item = item;
        this.organ = organ;
        this.amount = amount;
        this.tx = tx;
        this.ty = ty;
    }

    public static ItemSpawn weapon(WeaponType w, int tx, int ty) {
        return new ItemSpawn(Pickup.Kind.WEAPON, w, null, null, 1, tx, ty);
    }

    public static ItemSpawn item(ItemType it, int amount, int tx, int ty) {
        return new ItemSpawn(Pickup.Kind.ITEM, null, it, null, amount, tx, ty);
    }

    public static ItemSpawn organ(Organ organ, int tx, int ty) {
        return new ItemSpawn(Pickup.Kind.ORGAN, null, ItemType.ORGAN, organ, 1, tx, ty);
    }

    public static ItemSpawn keycard(int tx, int ty) {
        return new ItemSpawn(Pickup.Kind.KEYCARD, null, ItemType.KEYCARD, null, 1, tx, ty);
    }
}
