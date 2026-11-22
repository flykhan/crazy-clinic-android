# 疯狂诊所 · Crazy Clinic — v1.0.0

**J2ME 时代经典手机游戏《疯狂诊所》(HandyGames《Crazy Hospital》中国移动渠道中文版) 的 Android 同人重制版。**

> 项目发起人 / 出品：[flykhan](https://github.com/flykhan)
> 开发：DeepSeek Harness 多智能体协作（Lead / engine / art / ui / level / android / researcher）

## 本版内容

### 游戏
- **10 层手绘病区关卡**：精神病学 → 门诊·保健站 → 急症室 → 内科 → 外科 → 放射学 → 心脏病·ICU → 实验室 → 办事处·总部 → 屋顶（BOSS 赞恩）
- **俯视角动作 + 潜行逃脱**：找钥匙卡、黑入安保电脑（12×16 限时迷宫小游戏）、躲避/击杀敌人、坐单向电梯逃往下一层
- **7 种敌人 + BOSS**：看守人、医生（电锯）、护士（复活尸体）、感冒病人、中心助手（毒试管）、哨兵枪（固定炮台/怕电）、守卫；BOSS 赞恩三阶段
- **9 种武器**：拳头、斧头、手术刀（可回收投掷）、手枪、霰弹枪、泰瑟枪、除颤器（EMP）、本生灯（火焰喷射）、爆炸试管（毒云）
- **11 种物品 + 7 种器官永久强化**：医药包/止痛药/类固醇/能量饮料/EPO/隐形药；脑心肾腿肝肺双臂
- **完整系统**：警报/视野锥/BFS 追兵、体力冲刺/闪避、死亡结算、关卡解锁存档、4 页图文教学、设置（音效/音乐/震动/清档）、关于（版权声明）

### 技术
- 纯 **Java 17 + Android Canvas**，**零第三方运行时依赖**，**零位图素材**（全部图形程序化绘制）
- 竖屏 720×1280 逻辑分辨率，SurfaceView 渲染线程 60Hz，多点触控双摇杆
- 26 个合成音效 + 6 段循环 BGM 由脚本程序化生成
- 全部关卡为手绘 ASCII 地图并经连通性校验器验证（S→钥匙→锁门→电脑→电梯 必可达）
- 核心可在桌面 JVM 用 Java2D 直接渲染出图（`scripts/preview.sh`），无需模拟器

## 安装
- Android **8.0+**（minSdk 26），APK 约 5.4 MB
- 下载 `app-release.apk`，允许「安装未知来源应用」后安装

## 说明
- 本项目为**非商业同人重制**，不包含任何原版 JAR / 图片 / 音频 / 文本素材；原版版权归 HandyGames 及其权利人所有，与本项目无隶属关系。
- 代码以 MIT 协议开源。

## 致谢
- 原版：HandyGames《Crazy Hospital》(2009)，中文化 net mobile AG，国内渠道 创艺和弦 / 中国移动百宝箱
- 发起与出品：[flykhan](https://github.com/flykhan)
- Powered by [DeepSeek Harness](https://github.com/deepseek-ai/deepseek-harness)
