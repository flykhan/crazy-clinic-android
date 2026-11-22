package com.crazyclinic.core.ui;

import com.crazyclinic.core.Game;
import com.crazyclinic.core.MathUtil;
import com.crazyclinic.core.Music;
import com.crazyclinic.core.Palette;
import com.crazyclinic.core.Rect;
import com.crazyclinic.core.Renderer;
import com.crazyclinic.core.Scene;
import com.crazyclinic.core.Sfx;
import com.crazyclinic.core.content.Levels;
import com.crazyclinic.core.content.Texts;
import com.crazyclinic.core.world.ItemSpawn;
import com.crazyclinic.core.world.LevelDef;
import com.crazyclinic.core.world.Organ;
import com.crazyclinic.core.world.Pickup;

/**
 * 结算界面：胜利（电梯光 / 薄荷绿）与失败（血红 / 心电归零）两套视觉，
 * 统计击倒/用时/死亡，展示本层收获的器官与下一层解锁状态。
 *
 * <p>总击倒 / 死亡 / 时长等存档统计由 GameScene 在 gotoResult 之前写入，
 * 这里只读展示，避免重复累加。</p>
 */
public final class ResultScene implements Scene {
    private final boolean win;
    private final int floor;
    private final int kills;
    private final float elapsed;
    private final LevelDef def;
    private final Rect statsPanel = new Rect(56f, 330f, 608f, 250f);
    private final Rect harvestPanel = new Rect(56f, 604f, 608f, 152f);
    private final UiButton primary = new UiButton();
    private final UiButton secondary = new UiButton();
    private final UiButton tertiary = new UiButton();
    private final Organ[] floorOrgans;
    private final Rect back = new Rect(34f, 72f, 76f, 76f);

    private float time;
    private int pointer = -1;
    private boolean backPressed;
    private String killsText;
    private String timeText;
    private String deathsText;
    private String titleSub;
    private String unlockNote;

    public ResultScene(boolean win, int floor, int kills, float elapsed) {
        this.win = win;
        this.floor = MathUtil.clamp(floor, 1, Game.FLOOR_COUNT);
        this.kills = Math.max(0, kills);
        this.elapsed = Math.max(0f, elapsed);
        this.def = Levels.get(this.floor);
        this.floorOrgans = collectOrgans(def);
    }

    private static Organ[] collectOrgans(LevelDef def) {
        if (def == null) return new Organ[0];
        int n = 0;
        for (ItemSpawn s : def.items) {
            if (s.kind == Pickup.Kind.ORGAN && s.organ != null) n++;
        }
        Organ[] arr = new Organ[n];
        int i = 0;
        for (ItemSpawn s : def.items) {
            if (s.kind == Pickup.Kind.ORGAN && s.organ != null) arr[i++] = s.organ;
        }
        return arr;
    }

    @Override
    public void enter(Game game) {
        game.fade = 1f;
        time = 0f;
        pointer = -1;
        backPressed = false;
        if (win) {
            game.audio.play(Sfx.FLOOR_WIN);
            game.audio.music(Music.WIN);
            // 幂等，无害；引擎通常已解锁
            game.unlockFloor(floor + 1);
            game.saveNow();
        } else {
            game.audio.play(Sfx.FLOOR_LOSE);
            game.audio.music(Music.LOSE);
        }
        killsText = Integer.toString(kills);
        timeText = UiKit.formatTime(elapsed);
        deathsText = Integer.toString(game.save.deaths);
        titleSub = win ? String.format(MenuTexts.WIN_SUB_FMT, floor, def.name) : MenuTexts.LOSE_SUB;
        if (win) {
            if (floor >= Game.FLOOR_COUNT) {
                unlockNote = MenuTexts.ALL_CLEAR;
            } else {
                unlockNote = String.format(MenuTexts.NEXT_UNLOCK_FMT, floor + 1);
            }
        } else {
            unlockNote = null;
        }
        // 布局
        primary.set(80f, 792f, 560f, 100f).resetAppear();
        secondary.set(80f, 912f, 262f, 88f).resetAppear();
        tertiary.set(378f, 912f, 262f, 88f).resetAppear();
        primary.appearDelay = 0.08f;
        secondary.appearDelay = 0.14f;
        tertiary.appearDelay = 0.20f;
        if (win) {
            primary.label(floor >= Game.FLOOR_COUNT ? Texts.BTN_MENU : Texts.BTN_NEXT)
                    .style(UiKit.STYLE_PRIMARY);
            secondary.label(Texts.BTN_LEVELS).style(UiKit.STYLE_SECONDARY);
            tertiary.label(Texts.BTN_MENU).style(UiKit.STYLE_SECONDARY);
        } else {
            primary.label(Texts.BTN_RETRY).style(UiKit.STYLE_PRIMARY);
            secondary.label(Texts.BTN_MENU).style(UiKit.STYLE_SECONDARY);
            tertiary.label(Texts.BTN_LEVELS).style(UiKit.STYLE_SECONDARY);
        }
    }

    @Override
    public void update(Game game, float dt) {
        time += dt;
        UiKit.tickFade(game, dt);
        primary.update(dt);
        secondary.update(dt);
        tertiary.update(dt);
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @Override
    public void render(Game game, Renderer r) {
        int accent = win ? Palette.MINT : Palette.RED;
        UiBackdrop.menu(r, time, accent, win ? "CLEAR" : "FAILED");

        if (win) {
            renderWinGlow(r);
        } else {
            renderLoseAtmosphere(r);
        }

        // 返回圆钮
        UiKit.backCircle(r, back, backPressed, true);

        // 状态标题
        float pop = MathUtil.easeOutBack(MathUtil.clamp(time * 3f, 0f, 1f));
        r.save();
        r.translate(UiKit.W * 0.5f, 214f);
        r.scale(pop, pop);
        r.setTextSize(88f);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.BLACK, 0.6f));
        r.drawText(win ? MenuTexts.WIN_TITLE : MenuTexts.LOSE_TITLE, 4f, 7f);
        if (win) {
            r.setTextColor(accent);
            r.drawText(MenuTexts.WIN_TITLE, 0f, 0f);
            r.setTextColor(Palette.alpha(UiKit.MINT_GLOW, 0.35f));
            r.drawText(MenuTexts.WIN_TITLE, 0f, -3f);
        } else {
            r.setTextColor(accent);
            r.drawText(MenuTexts.LOSE_TITLE, 0f, 0f);
            r.setTextColor(Palette.alpha(UiKit.BLOOD_LIGHT, 0.45f));
            r.drawText(MenuTexts.LOSE_TITLE, 0f, -3f);
        }
        r.restore();
        // 副标题
        r.setTextSize(26f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.95f));
        UiKit.textFit(r, titleSub, UiKit.W * 0.5f, 282f, 640f, 26f, 18f,
                Palette.alpha(Palette.TEXT_DIM, 0.95f), 1, false);

        // 统计面板
        UiKit.panel(r, statsPanel, accent, time);
        statRow(r, statsPanel.y + 56f, 0, Texts.STAT_KILLS, killsText,
                win ? Palette.MINT : Palette.TEXT);
        statRow(r, statsPanel.y + 126f, 1, Texts.STAT_TIME, timeText,
                win ? Palette.CYAN : Palette.TEXT);
        statRow(r, statsPanel.y + 196f, 2, Texts.STAT_DEATHS, deathsText,
                win ? Palette.GOLD : Palette.ALERT);

        // 收获 / 提示面板
        if (win) {
            renderHarvest(r, game, accent);
        } else {
            renderTip(r);
        }


        // 按钮
        primary.draw(r, time);
        secondary.draw(r, time);
        tertiary.draw(r, time);

        // 底部印章 + 提示
        renderStamp(r);
        r.setTextSize(19f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT_DARK, 0.85f));
        r.drawText(win ? "每一层都在变深 · 下一层会更难" : MenuTexts.SAVED,
                UiKit.W * 0.5f, 1236f);
    }

    private void renderWinGlow(Renderer r) {
        // 电梯顶光：从上方射下的绿色光束
        r.fillRadialGradient(UiKit.W * 0.5f, 200f, 520f, Palette.alpha(Palette.MINT, 0.20f), 0x00000000);
        r.save();
        r.setAlpha(0.10f);
        float[] beam = new float[8];
        for (int i = 0; i < 4; i++) {
            float x0 = 150f + i * 140f;
            float x1 = x0 + 46f;
            beam[0] = UiKit.W * 0.5f - 30f;
            beam[1] = 0f;
            beam[2] = UiKit.W * 0.5f + 30f;
            beam[3] = 0f;
            beam[4] = x1 + 130f;
            beam[5] = UiKit.H;
            beam[6] = x0 - 70f;
            beam[7] = UiKit.H;
            r.fillPolygon(beam, UiKit.MINT_GLOW);
        }
        r.restore();
        // 上升的尘埃
        for (int i = 0; i < 22; i++) {
            float h1 = MathUtil.hash01(i * 13 + 3, 21);
            float h2 = MathUtil.hash01(i * 7 + 5, 31);
            float y = UiKit.H - ((time * (24f + h2 * 46f) + h1 * UiKit.H) % UiKit.H);
            float x = 120f + h1 * 480f + (float) Math.sin(time * 0.9f + i) * 10f;
            r.fillCircle(x, y, 1.2f + h2 * 2.4f, Palette.alpha(0xFFD8FFE9, 0.20f + 0.25f * h2));
        }
        // 底部电梯门缝光
        r.fillVerticalGradient(UiKit.W * 0.5f - 70f, 560f, 140f, 720f,
                Palette.alpha(Palette.MINT, 0.10f), Palette.alpha(Palette.MINT, 0.0f));
    }

    private void renderLoseAtmosphere(Renderer r) {
        UiBackdrop.dangerPulse(r, time, 0.42f);
        // 归零的心电线
        float y = 306f;
        r.line(0f, y, UiKit.W * 0.34f, y, 2f, Palette.alpha(Palette.RED, 0.45f));
        r.line(UiKit.W * 0.34f, y, UiKit.W * 0.42f, y - 10f, 2f, Palette.alpha(Palette.RED, 0.45f));
        r.line(UiKit.W * 0.42f, y - 10f, UiKit.W * 0.50f, y + 26f, 2f, Palette.alpha(Palette.RED, 0.45f));
        r.line(UiKit.W * 0.50f, y + 26f, UiKit.W * 0.58f, y - 16f, 2f, Palette.alpha(Palette.RED, 0.45f));
        r.line(UiKit.W * 0.58f, y - 16f, UiKit.W * 0.66f, y, 2f, Palette.alpha(Palette.RED, 0.45f));
        r.line(UiKit.W * 0.66f, y, UiKit.W, y, 2f, Palette.alpha(Palette.RED, 0.45f));
        // 顶部血迹
        UiKit.grunge(r, 0f, 0f, UiKit.W, 260f, 26, 77);
        r.fillCircle(92f, 150f, 26f, Palette.alpha(Palette.BLOOD, 0.22f));
        r.fillRoundRect(90f, 150f, 5f, 78f, 2.5f, Palette.alpha(Palette.BLOOD, 0.20f));
    }

    private void statRow(Renderer r, float y, int kind, String label, String value, int valueColor) {
        float iconX = statsPanel.x + 52f;
        switch (kind) {
            case 0:
                UiKit.iconCrosshair(r, iconX, y, 34f, Palette.alpha(valueColor, 0.85f));
                break;
            case 1:
                UiKit.iconClock(r, iconX, y, 34f, Palette.alpha(valueColor, 0.85f));
                break;
            default:
                UiKit.iconSkull(r, iconX, y, 34f, Palette.alpha(valueColor, 0.85f));
                break;
        }
        r.setTextSize(26f);
        r.setTextBold(false);
        r.setTextAlign(0);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.95f));
        r.drawText(label, statsPanel.x + 84f, y);
        r.setTextSize(38f);
        r.setTextBold(true);
        r.setTextAlign(2);
        r.setTextColor(valueColor);
        r.drawText(value, statsPanel.x + statsPanel.w - 36f, y);
        if (kind < 2) {
            r.fillRect(statsPanel.x + 34f, y + 36f, statsPanel.w - 68f, 1.2f,
                    Palette.alpha(Palette.PANEL_STROKE, 0.5f));
        }
    }

    private void renderHarvest(Renderer r, Game game, int accent) {
        UiKit.panel(r, harvestPanel, accent, time);
        r.setTextSize(20f);
        r.setTextBold(true);
        r.setTextAlign(0);
        r.setTextColor(Palette.alpha(accent, 0.95f));
        r.drawText(MenuTexts.HARVEST, harvestPanel.x + 34f, harvestPanel.y + 32f);
        r.setTextSize(19f);
        r.setTextBold(false);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.95f));
        r.drawText(floorOrgans.length > 1 ? "永久强化 ×" + floorOrgans.length : "永久强化",
                harvestPanel.x + 34f, harvestPanel.y + 66f);

        // 右侧：下一层 / 全部通关
        float chipX = harvestPanel.x + harvestPanel.w - 176f;
        r.fillRect(chipX - 22f, harvestPanel.y + 20f, 1.5f, harvestPanel.h - 40f,
                Palette.alpha(Palette.PANEL_STROKE, 0.6f));
        r.setTextSize(18f);
        r.setTextBold(false);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.95f));
        if (floor >= Game.FLOOR_COUNT) {
            r.drawText("全部病区", chipX + 72f, harvestPanel.y + 54f);
            r.setTextSize(24f);
            r.setTextBold(true);
            r.setTextColor(Palette.GOLD);
            r.drawText("已通关", chipX + 72f, harvestPanel.y + 90f);
        } else {
            r.drawText("下一层", chipX + 72f, harvestPanel.y + 54f);
            r.setTextSize(34f);
            r.setTextBold(true);
            r.setTextColor(Palette.GOLD);
            r.drawText("第 " + (floor + 1) + " 层", chipX + 72f, harvestPanel.y + 94f);
        }

        if (floorOrgans.length == 0) {
            r.setTextSize(22f);
            r.setTextBold(false);
            r.setTextAlign(1);
            r.setTextColor(Palette.alpha(Palette.TEXT_DIM, 0.9f));
            r.drawText(MenuTexts.NO_NEW_ORGAN, harvestPanel.x + 220f, harvestPanel.y + 104f);
            return;
        }
        int n = Math.min(floorOrgans.length, 4);
        float iconY = harvestPanel.y + 106f;
        float labelY = harvestPanel.y + 140f;
        float areaX = harvestPanel.x + 34f;
        float areaW = 376f;
        float cell = areaW / n;
        for (int i = 0; i < n; i++) {
            Organ organ = floorOrgans[i];
            boolean lit = game.save.hasOrgan(organ);
            float cx = areaX + cell * (i + 0.5f);
            UiKit.organPlate(r, cx, iconY, 24f, lit);
            UiKit.organ(r, organ, cx, iconY, 40f, lit);
            r.setTextSize(17f);
            r.setTextBold(lit);
            r.setTextAlign(1);
            r.setTextColor(lit ? Palette.alpha(UiKit.organColor(organ), 0.95f)
                    : Palette.alpha(Palette.TEXT_DARK, 0.9f));
            r.drawText(organ.label, cx, labelY);
        }
    }

    private void renderTip(Renderer r) {
        UiKit.panel(r, harvestPanel, Palette.ALERT, time);
        UiKit.iconSkull(r, harvestPanel.x + 62f, harvestPanel.y + 76f, 52f,
                Palette.alpha(Palette.ALERT, 0.8f));
        r.setTextSize(22f);
        r.setTextBold(false);
        r.setTextAlign(0);
        UiKit.wrapped(r, MenuTexts.LOSE_TIP, harvestPanel.x + 100f, harvestPanel.y + 56f,
                harvestPanel.w - 140f, 22f, Palette.alpha(Palette.TEXT, 0.9f), 10f);
    }

    private void renderStamp(Renderer r) {
        r.save();
        r.translate(UiKit.W * 0.5f, 1090f);
        r.rotate(win ? -7f : 5f);
        r.setTextSize(46f);
        r.setTextBold(true);
        r.setTextAlign(1);
        r.setTextColor(Palette.alpha(win ? Palette.MINT : Palette.RED, 0.16f));
        r.drawText(win ? "C L E A R E D" : "D E C E A S E D", 0f, 0f);
        r.strokeRect(-176f, -34f, 352f, 68f, 3f, Palette.alpha(win ? Palette.MINT : Palette.RED, 0.10f));
        r.restore();
    }

    // ------------------------------------------------------------------
    // 触摸
    // ------------------------------------------------------------------

    @Override
    public void touchDown(Game game, int id, float x, float y) {
        if (pointer >= 0) return;
        pointer = id;
        backPressed = back.contains(x, y);
        if (primary.down(id, x, y) || secondary.down(id, x, y) || tertiary.down(id, x, y)) {
            game.audio.play(Sfx.UI_CLICK);
        } else if (backPressed) {
            game.audio.play(Sfx.UI_CLICK);
        }
    }

    @Override
    public void touchMove(Game game, int id, float x, float y) {
        if (id != pointer) return;
        if (backPressed && !back.contains(x, y)) backPressed = false;
        if (primary.pointer() == id && !primary.contains(x, y)) primary.cancel(id);
        if (secondary.pointer() == id && !secondary.contains(x, y)) secondary.cancel(id);
        if (tertiary.pointer() == id && !tertiary.contains(x, y)) tertiary.cancel(id);
    }

    @Override
    public void touchUp(Game game, int id, float x, float y) {
        if (id != pointer) return;
        pointer = -1;
        if (backPressed && back.contains(x, y)) {
            backPressed = false;
            game.audio.play(Sfx.UI_BACK);
            game.gotoTitle();
            return;
        }
        backPressed = false;
        if (primary.up(id, x, y)) {
            game.audio.play(Sfx.UI_CONFIRM);
            if (win) {
                if (floor >= Game.FLOOR_COUNT) game.gotoTitle();
                else game.gotoIntro(floor + 1);
            } else {
                game.startFloor(floor);
            }
        } else if (secondary.up(id, x, y)) {
            game.audio.play(Sfx.UI_BACK);
            if (win) game.gotoLevelSelect();
            else game.gotoTitle();
        } else if (tertiary.up(id, x, y)) {
            game.audio.play(Sfx.UI_BACK);
            if (win) game.gotoTitle();
            else game.gotoLevelSelect();
        } else {
            primary.cancel(id);
            secondary.cancel(id);
            tertiary.cancel(id);
        }
    }

    @Override
    public void backPressed(Game game) {
        game.audio.play(Sfx.UI_BACK);
        game.gotoTitle();
    }
}
