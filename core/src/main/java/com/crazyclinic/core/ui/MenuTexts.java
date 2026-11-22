package com.crazyclinic.core.ui;

/**
 * 菜单界面专用补充文案（ui 包私有，不修改 level 代理维护的 Texts）。
 * 已有文案优先用 Texts，本类只放 Texts 里没有的。
 */
public final class MenuTexts {
    private MenuTexts() {}

    // ---------- 标题 ----------
    public static final String TITLE_EN = "C R A Z Y   H O S P I T A L";
    public static final String CONTINUE_FMT = "继续游戏 · 第 %d 层";
    public static final String CONTINUE_NONE = "继续游戏 · 无存档";
    public static final String NEW_GAME_DESC = "从头开始新的逃脱";
    public static final String LEVELS_DESC = "选择已解锁的病区";
    public static final String CREDIT_LINE = "程序化重绘 · 致敬 HandyGames《Crazy Hospital》";
    public static final String SAFE_AREA_NOTE = "重制版 v1.0";

    // ---------- 关卡选择 ----------
    public static final String ORGANS = "器官收集";
    public static final String ORGANS_FMT = "%d / 7";
    public static final String UNLOCKED_FMT = "已解锁 %d / %d";
    public static final String CLEARED = "已通关";
    public static final String CHALLENGE = "待挑战";
    public static final String LOCKED = "未解锁";
    public static final String BOSS_TAG = "BOSS";
    public static final String FLOOR_FMT = "第 %d 层";
    public static final String DRAG_HINT = "上下拖动浏览 · 点击卡片进入";
    public static final String LV_STATS_FMT = "总击倒 %d · 死亡 %d · 时长 %s";

    // ---------- 剧情简报 ----------
    public static final String BRIEFING = "病例档案 · BRIEFING";
    public static final String OBJECTIVE = "任务目标";
    public static final String OBJ_KEY = "钥匙卡";
    public static final String OBJ_HACK = "安保电脑";
    public static final String OBJ_ELEVATOR = "电梯";
    public static final String TUTORIAL_LINK = "操作说明";
    public static final String TAP_TO_SKIP = "点击文本立即显示全文";

    // ---------- 结算 ----------
    public static final String WIN_TITLE = "逃脱成功";
    public static final String LOSE_TITLE = "你被捕了";
    public static final String WIN_SUB_FMT = "第 %d 层 · %s";
    public static final String LOSE_SUB = "卡特倒在了血泊里……";
    public static final String HARVEST = "本层收获";
    public static final String NEXT_UNLOCK_FMT = "下一层已解锁：第 %d 层";
    public static final String ALL_CLEAR = "全部病区已通关 · 直升机在等你";
    public static final String NO_NEW_ORGAN = "本层没有发现器官";
    public static final String SAVED = "进度已保存";
    public static final String LOSE_TIP = "提示：贴着墙壁走、用闪避躲子弹，枪声会引来看守。";

    // ---------- 设置 ----------
    public static final String SET_SOUND_DESC = "界面点击 · 战斗音效";
    public static final String SET_MUSIC_DESC = "循环背景音乐";
    public static final String SET_VIBRATE_DESC = "受击 · 警报 · 器官";
    public static final String SET_RESET_DEFAULT = "恢复默认设置";
    public static final String SET_DATA = "游戏数据";
    public static final String SET_DATA_FMT = "击倒 %d · 死亡 %d · 游玩 %s";
    public static final String SET_STORAGE = "存档 v1 · 仅保存在本机 · 无云同步";
    public static final String CONFIRM_TITLE = "清除存档？";
    public static final String CONFIRM_BODY = "所有关卡解锁、器官强化与统计数据都将\n被永久删除，且无法恢复。";
    public static final String CONFIRM_OK = "确定清除";
    public static final String RESET_DONE = "已恢复默认设置";
    public static final String WIPED = "存档已清除";

    // ---------- 关于 ----------
    public static final String ABOUT_SUB = "非商业同人重制版";
    public static final String ABOUT_ORIGINAL_TITLE = "原版";
    public static final String ABOUT_ORIGINAL =
            "《疯狂医院 / Crazy Hospital》(2009)\n"
          + "开发 HandyGames · 中文化 net mobile AG\n"
          + "国内渠道 创艺和弦 / 中国移动百宝箱\n"
          + "原版版权归各自权利人所有";
    public static final String ABOUT_NOTICE_TITLE = "关于本重制";
    public static final String ABOUT_NOTICE =
            "本项目为学习与怀旧用途的同人重制，\n"
          + "不包含任何原版图片、音频或代码素材，\n"
          + "所有画面与音效均由程序实时生成。\n"
          + "与原版权方无隶属关系，请勿商用。";
    public static final String ABOUT_TECH_TITLE = "技术栈";
    public static final String ABOUT_TECH =
            "纯 Java 游戏核心 + Android Canvas 绘制\n"
          + "JDK 17 · Gradle 8.7 · minSdk 26\n"
          + "零第三方运行时依赖 · 无位图素材";
    public static final String ABOUT_CREDITS = "参与代号";
    public static final String[] CREDITS = {
            "Lead", "engine", "art", "ui", "level", "android"
    };
    public static final String[] CREDIT_ROLES = {
            "架构", "引擎", "美术", "菜单", "关卡", "安卓"
    };
    public static final String ABOUT_FOOTER = "MADE WITH CANVAS · 致敬 2009 年的诺基亚";

    // ---------- 教学 ----------
    public static final String[] TUTORIAL_TITLES = {
            "逃出这座医院", "双手操作", "看懂 HUD", "器官强化"
    };
    public static final String TUTORIAL_COUNT_FMT = "%d / %d";
    public static final String PREV = "上一页";
    public static final String NEXT = "下一页";
    public static final String FINISH = "开始逃命";
    public static final String SKIP = "跳过";
    public static final String TUTORIAL_TIP_TITLE = "小贴士";
    public static final String[] TUTORIAL_TIPS = {
            "先别急着开枪：巡逻的看守可以被绕开。",
            "推满摇杆会自动冲刺，冲刺和闪避共用体力。",
            "受伤后躲到掩体后等回血，别硬冲火力网。",
            "器官永久生效，死亡也不会丢失已获得的器官。"
    };
}
