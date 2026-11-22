package com.crazyclinic.core.content;

/**
 * 全部中文 UI 文案（剧情/关卡简报在 Levels 中）。
 *
 * 所有权：level 代理。ui/engine/art 通过常量名引用，请勿删改已有常量名；
 * 需要新文案时优先在这里增补，避免界面里写死中文。
 */
public final class Texts {
    // ================= 标题 / 通用 =================
    public static final String TITLE = "疯狂诊所";
    public static final String TITLE_EN = "CRAZY HOSPITAL";
    public static final String SUBTITLE = "逃出疯狂医院";
    public static final String VERSION = "重制版 v1.0";
    public static final String PRESS_ANY_KEY = "点击屏幕开始";
    public static final String LOADING = "正在载入……";

    // ================= 按钮 =================
    public static final String BTN_NEW_GAME = "新游戏";
    public static final String BTN_CONTINUE = "继续游戏";
    public static final String BTN_LEVELS = "关卡选择";
    public static final String BTN_SETTINGS = "设置";
    public static final String BTN_ABOUT = "关于";
    public static final String BTN_BACK = "返回";
    public static final String BTN_START = "开始行动";
    public static final String BTN_NEXT = "下一层";
    public static final String BTN_RETRY = "重试";
    public static final String BTN_MENU = "主菜单";
    public static final String BTN_RESUME = "继续游戏";
    public static final String BTN_QUIT_FLOOR = "放弃本层";
    public static final String BTN_CONFIRM = "确定";
    public static final String BTN_CANCEL = "取消";
    public static final String BTN_SKIP = "跳过";
    public static final String BTN_PREV = "上一页";
    public static final String BTN_FINISH = "完成";
    public static final String BTN_TUTORIAL = "操作说明";
    public static final String BTN_CLOSE = "关闭";

    // ================= 菜单 =================
    public static final String MENU_NEW_GAME_DESC = "从头开始新的逃脱";
    public static final String MENU_LEVELS_DESC = "选择已解锁的病区";
    public static final String MENU_CONTINUE_FMT = "继续游戏 · 第 %d 层";
    public static final String MENU_CONTINUE_NONE = "继续游戏 · 无存档";
    public static final String MENU_SELECT_FLOOR = "选择病区";
    public static final String MENU_LOCKED = "未解锁";
    public static final String MENU_CLEARED = "已通关";
    public static final String MENU_CHALLENGE = "待挑战";
    public static final String MENU_BOSS_TAG = "BOSS";
    public static final String MENU_FLOOR_FMT = "第 %d 层";
    public static final String MENU_DRAG_HINT = "上下拖动浏览 · 点击卡片进入";
    public static final String MENU_ORGANS = "器官收集";
    public static final String MENU_ORGANS_FMT = "%d / 7";
    public static final String MENU_UNLOCKED_FMT = "已解锁 %d / %d";
    public static final String MENU_TOTAL_STATS_FMT = "总击倒 %d · 死亡 %d · 时长 %s";
    public static final String MENU_CREDIT_LINE = "程序化重绘 · 致敬 HandyGames《Crazy Hospital》";

    // ================= 暂停 =================
    public static final String PAUSED = "已暂停";
    public static final String PAUSED_HINT = "点击屏幕继续";
    public static final String PAUSED_QUIT_HINT = "放弃本层会丢失本层进度，确定吗？";

    // ================= 关卡目标 / HUD =================
    public static final String OBJECTIVE = "任务目标";
    public static final String OBJECTIVE_KEY = "找到钥匙卡";
    public static final String OBJECTIVE_HACK = "黑入安保电脑";
    public static final String OBJECTIVE_ELEVATOR = "前往电梯";
    public static final String OBJECTIVE_BOSS = "击败赞恩";
    public static final String OBJ_KEY = "钥匙卡";
    public static final String OBJ_HACK = "安保电脑";
    public static final String OBJ_ELEVATOR = "电梯";
    public static final String OBJ_BOSS = "赞恩";
    public static final String HUD_FLOOR_FMT = "第 %d 层 · %s";
    public static final String HUD_KEYCARD_FMT = "钥匙卡 ×%d";
    public static final String HUD_BOSS_PHASE_FMT = "赞恩 · 第 %d 阶段";
    public static final String BOSS_TAG = "BOSS";
    public static final String BOSS_NAME = "赞恩";
    public static final String ALERT_TEXT = "警报！";
    public static final String ALERT_CALM = "警报解除";

    // ================= 交互提示 =================
    public static final String PROMPT_LOCKED = "门锁着，需要钥匙卡";
    public static final String PROMPT_HACK = "使用安保电脑";
    public static final String PROMPT_HACKED = "安全系统已关闭";
    public static final String PROMPT_ELEVATOR = "启动电梯";
    public static final String PROMPT_ELEVATOR_LOCKED = "电梯被封锁了，先解决赞恩";
    public static final String PROMPT_LOCKER = "搜索储物柜";
    public static final String PROMPT_EMPTY = "空的";
    public static final String PROMPT_PICKUP = "拾取";
    public static final String PROMPT_OPEN_DOOR = "开门";
    public static final String PROMPT_NEED_KEY = "需要钥匙卡";
    public static final String PROMPT_DOOR_OPENED = "门开了";
    public static final String PROMPT_NOTHING = "这里没什么可操作的";
    public static final String PROMPT_HACKING = "骇入中……";
    public static final String PROMPT_ORGAN = "取走器官";

    // ================= 拾取 / 强化提示 =================
    public static final String PICKUP_WEAPON_FMT = "获得武器：%s";
    public static final String PICKUP_AMMO_FMT = "%s 弹药 +%d";
    public static final String PICKUP_KEYCARD = "拿到钥匙卡！去找红色锁门（+）";
    public static final String PICKUP_KEYCARD_FMT = "钥匙卡 ×%d";
    public static final String PICKUP_HEAL_FMT = "生命 +%d";
    public static final String PICKUP_ITEM_FMT = "拾取 %s";
    public static final String PICKUP_ORGAN_FMT = "永久强化：%s";
    public static final String ORGAN_ALREADY = "这个器官已经强化过了";
    public static final String NO_AMMO = "没弹药";
    public static final String NO_FUEL = "没燃料";
    public static final String WEAPON_BROKEN = "武器坏了";
    public static final String TOO_FAR = "够不着";
    public static final String DOOR_LOCKED_HINT = "锁着的门需要钥匙卡";

    // ================= 骇客小游戏 =================
    public static final String HACK_TITLE = "黑入安保系统";
    public static final String HACK_DESC = "拖拽白色光点从左上走到右下，避开障碍。";
    public static final String HACK_RULES = "黄色 = 无法通过；橙色 = 退回起点；红黄 = 失败并触发警报。";
    public static final String HACK_CONTROLS = "拖动摇杆或直接拖动光点移动";
    public static final String HACK_FAIL = "黑入失败！警报已触发";
    public static final String HACK_SUCCESS = "安全系统已关闭";
    public static final String HACK_ALARM = "全图警报！";
    public static final String HACK_TIME_FMT = "剩余 %d 秒";
    public static final String HACK_BONUS = "脑升级：骇客时限 +2 秒";
    public static final String HACK_AUTO = "一键破解 ×1";
    public static final String HACK_RETRY = "重新骇入";

    // ================= 胜败 / 结算 =================
    public static final String WIN = "逃脱成功";
    public static final String LOSE = "你被捕了";
    public static final String WIN_TITLE = "逃脱成功";
    public static final String LOSE_TITLE = "你被捕了";
    public static final String WIN_DESC = "电梯门缓缓关闭，你活着离开了这一层。";
    public static final String LOSE_DESC = "卡特倒在了血泊里……再来一次。";
    public static final String WIN_SUB_FMT = "第 %d 层 · %s";
    public static final String LOSE_SUB = "医院又把你拖回了病床";
    public static final String LOSE_TIP = "提示：贴墙走、用闪避躲子弹、先解决护士和哨兵枪。";
    public static final String ALL_CLEAR = "全部病区已通关，你逃出了疯狂医院。";
    public static final String NEXT_UNLOCK_FMT = "第 %d 层已解锁";
    public static final String STAT_KILLS = "击倒敌人";
    public static final String STAT_TIME = "用时";
    public static final String STAT_DEATHS = "死亡次数";
    public static final String STAT_ORGANS = "器官收集";
    public static final String HARVEST = "本层收获";
    public static final String ORGANS = "器官";
    public static final String NO_NEW_ORGAN = "本层没有新的器官";
    public static final String SAVED = "进度已自动保存";
    public static final String CLEARED = "已通关";
    public static final String CHALLENGE = "待挑战";
    public static final String LOCKED = "未解锁";
    public static final String RESULT_DEATH_REASON_FMT = "死亡原因：%s";

    // ================= 设置 =================
    public static final String SET_SOUND = "音效";
    public static final String SET_MUSIC = "音乐";
    public static final String SET_VIBRATE = "震动";
    public static final String SET_SOUND_DESC = "按钮、枪声、爆炸与拾取反馈";
    public static final String SET_MUSIC_DESC = "病区氛围与 BOSS 战音乐";
    public static final String SET_VIBRATE_DESC = "受伤、开枪与爆炸时震动";
    public static final String SET_RESET = "清除存档";
    public static final String SET_RESET_DEFAULT = "恢复默认设置";
    public static final String SET_RESET_CONFIRM = "确定清除所有进度？";
    public static final String SET_DATA = "本机记录";
    public static final String SET_DATA_FMT = "总击倒 %d · 死亡 %d · 游戏时长 %s";
    public static final String SET_STORAGE = "存档保存在本机，不会上传网络";
    public static final String SET_RESET_DEFAULT_DONE = "已恢复默认设置";
    public static final String RESET_DONE = "存档已清除";
    public static final String WIPED = "进度已清空，一切从头开始";
    public static final String CONFIRM_TITLE = "确认操作";
    public static final String CONFIRM_OK = "确定";
    public static final String CONFIRM_BODY = "这会清除全部关卡进度、器官强化与记录。\n操作不可撤销，确定继续吗？";
    public static final String ON = "开";
    public static final String OFF = "关";
    public static final String VOLUME_FMT = "音量 %d%%";

    // ================= 教学 =================
    public static final String[] TUTORIAL_TITLES = {
            "你是谁", "怎么走", "怎么打", "怎么活下来"
    };
    public static final String[] TUTORIAL_PAGES = {
            "欢迎来到疯狂医院。\n\n"
          + "你是卡特，一个在休克疗法后失忆的病人。\n"
          + "医院里的人都疯了，而且他们不想让你离开。\n\n"
          + "目标：找到钥匙卡，黑入安保电脑，坐电梯逃到下一层。",
            "左半屏拖动 = 移动。\n"
          + "把摇杆推到底会开始冲刺，冲刺消耗体力。\n\n"
          + "右半屏拖动 = 瞄准，按住即自动攻击。\n"
          + "右下角按钮：攻击 / 互动 / 闪避 / 换武器 / 用道具。",
            "红色血条 = 生命，金色体力条 = 冲刺与闪避。\n\n"
          + "靠近门、电脑、储物柜、电梯时会出现互动按钮。\n"
          + "锁门需要钥匙卡，电脑可以关掉这一层的安保系统。\n\n"
          + "记住：枪声会引来敌人，能绕后就绕后。",
            "打死敌人可能掉落器官，拾取后永久强化你自己。\n"
          + "每层结算后自动存档；电梯只能向上，不能回头。\n\n"
          + "最后一层在天台——那里有人在等你。\n\n"
          + "祝你好运，卡特。"
    };
    public static final String[] TUTORIAL_TIPS = {
            "提示：每层都要重复“钥匙 → 电脑 → 电梯”的流程。",
            "提示：闪避可以躲子弹，也会消耗体力。",
            "提示：护士会复活倒地的敌人，优先解决她。",
            "提示：器官强化会保留到下一周目。"
    };
    public static final String TUTORIAL_COUNT_FMT = "第 %d / %d 页";
    public static final String TUTORIAL_LINK = "操作说明";
    public static final String TUTORIAL_SKIP = "跳过教学";

    // ================= 关于 / 版权 =================
    public static final String ABOUT_SUB = "非商业同人重制版";
    public static final String ABOUT_ORIGINAL_TITLE = "原版";
    public static final String ABOUT_ORIGINAL =
            "《疯狂医院 / Crazy Hospital》(2009)\n"
          + "开发：HandyGames · 中文化：net mobile AG\n"
          + "国内渠道：创艺和弦 / 中国移动百宝箱\n"
          + "原版 © HandyGames，版权归原作者所有。";
    public static final String ABOUT_NOTICE_TITLE = "版权声明";
    public static final String ABOUT_NOTICE =
            "本项目是非商业同人重制，仅用于学习与怀旧。\n"
          + "代码、美术与音效全部为程序化原创，\n"
          + "不包含任何原版素材（图片 / 音频 / 数据）。\n"
          + "原版著作权、商标权归 HandyGames 所有。";
    public static final String ABOUT_TECH_TITLE = "技术";
    public static final String ABOUT_TECH =
            "纯 Java 游戏核心 + Android Canvas 程序化绘制\n"
          + "逻辑分辨率 720 × 1280 · 瓦片 72 px\n"
          + "无第三方运行时依赖 · 无位图资源";
    public static final String ABOUT_CREDITS = "制作";
    public static final String[] CREDITS = {"关卡 / 数值 / 文案", "引擎 / 玩法系统", "程序化美术 / HUD", "菜单 / UI", "安卓壳 / 音频"};
    public static final String[] CREDIT_ROLES = {"level", "engine", "art", "ui", "android"};
    public static final String CREDIT_LINE = "程序化重绘 · 致敬 HandyGames《Crazy Hospital》";
    public static final String ABOUT_FOOTER = "感谢每一位记得这款 J2ME 老游戏的人。";
    public static final String ABOUT_BODY =
            "《疯狂诊所》非商业性同人重制版\n\n"
          + "原版：《疯狂医院 / Crazy Hospital》(2009)\n"
          + "开发：HandyGames · 中文化：net mobile AG\n"
          + "国内渠道：创艺和弦 / 中国移动百宝箱\n\n"
          + "本重制版所有代码与图形均为程序化原创，\n"
          + "不包含任何原版素材。仅用于学习与怀旧。\n"
          + "原版著作权归 HandyGames 所有。\n\n"
          + "引擎：Android Canvas + 纯 Java 游戏核心\n"
          + "构建：Gradle · JDK 17 · 无第三方运行时依赖";

    // ================= 死亡原因（结算/教学用） =================
    public static final String DEATH_UNKNOWN = "失血过多";
    public static final String DEATH_GUARD = "被看守人的扳钳击中";
    public static final String DEATH_DOCTOR = "死于电锯";
    public static final String DEATH_FLU = "被病人的喷嚏传染";
    public static final String DEATH_TECH = "被爆炸试管波及";
    public static final String DEATH_NURSE = "被超大注射器刺中";
    public static final String DEATH_SENTRY = "被哨兵枪火力覆盖";
    public static final String DEATH_BOSS = "被赞恩的铁锤砸中";

    private Texts() {}
}
