package com.crazyclinic.core.content;

/** 所有中文 UI 文案（剧情文本在 Levels 中）。 */
public final class Texts {
    public static final String TITLE = "疯狂诊所";
    public static final String SUBTITLE = "逃出疯狂医院";
    public static final String VERSION = "重制版 v1.0";

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

    public static final String PAUSED = "已暂停";
    public static final String WIN = "逃脱成功";
    public static final String LOSE = "你被捕了";
    public static final String WIN_DESC = "电梯门缓缓关闭，你活着离开了这一层。";
    public static final String LOSE_DESC = "卡特倒在了血泊里……再来一次。";
    public static final String STAT_KILLS = "击倒敌人";
    public static final String STAT_TIME = "用时";
    public static final String STAT_DEATHS = "死亡次数";

    public static final String OBJECTIVE_KEY = "找到钥匙卡";
    public static final String OBJECTIVE_HACK = "黑入安保电脑";
    public static final String OBJECTIVE_ELEVATOR = "前往电梯";
    public static final String OBJECTIVE_BOSS = "击败赞恩";
    public static final String PROMPT_LOCKED = "门锁着，需要钥匙卡";
    public static final String PROMPT_HACK = "使用安保电脑";
    public static final String PROMPT_ELEVATOR = "启动电梯";
    public static final String PROMPT_LOCKER = "搜索储物柜";
    public static final String PROMPT_EMPTY = "空的";
    public static final String PROMPT_PICKUP = "拾取";

    public static final String HACK_TITLE = "黑入安保系统";
    public static final String HACK_DESC = "拖拽白色光点从左上走到右下，避开障碍。";
    public static final String HACK_FAIL = "黑入失败！警报已触发";
    public static final String HACK_SUCCESS = "安全系统已关闭";
    public static final String HACK_ALARM = "全图警报！";

    public static final String SET_SOUND = "音效";
    public static final String SET_MUSIC = "音乐";
    public static final String SET_VIBRATE = "震动";
    public static final String SET_RESET = "清除存档";
    public static final String SET_RESET_CONFIRM = "确定清除所有进度？";
    public static final String ON = "开";
    public static final String OFF = "关";

    public static final String ABOUT_BODY =
            "《疯狂诊所》非商业性同人重制版\n\n"
          + "原版：《疯狂医院 / Crazy Hospital》(2009)\n"
          + "开发：HandyGames · 中文化：net mobile AG\n"
          + "国内渠道：创艺和弦 / 中国移动百宝箱\n\n"
          + "本重制版所有代码与图形均为程序化原创，\n"
          + "不包含任何原版素材。仅用于学习与怀旧。\n\n"
          + "引擎：Android Canvas + 纯 Java 游戏核心\n"
          + "构建：Gradle · JDK 17 · 无第三方运行时依赖";

    public static final String[] TUTORIAL_PAGES = {
            "欢迎来到疯狂医院。\n\n你是卡特，一个失忆的病人。\n医院里的人并不想让你离开。\n\n" +
            "目标：找到钥匙卡，黑入安保电脑，坐电梯逃到下一层。",
            "左半屏拖动 = 移动。\n按住移动方向更久会开始冲刺（消耗体力）。\n\n" +
            "右半屏拖动 = 瞄准。\n按住后自动攻击，松开停火。\n\n" +
            "右下按钮：攻击 / 互动 / 闪避 / 换武器 / 用道具。",
            "红色血条 = 生命，金色体力 = 冲刺和闪避。\n\n靠近门、电脑、储物柜、电梯时，\n会出现互动按钮提示。\n\n" +
            "记住：枪声会引来敌人，能绕后就绕后。",
            "打死敌人可能掉落器官，\n拾取后永久强化你自己。\n\n" +
            "每一层都有不同的病区主题，\n最后一层在天台——那里有人在等你。\n\n祝你好运，卡特。"
    };

    private Texts() {}
}
