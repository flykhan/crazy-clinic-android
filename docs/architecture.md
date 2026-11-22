# 架构说明

## 分层

```
app (Android)
  MainActivity → GameView(SurfaceView, 60fps 线程) → Game.update/render
  AndroidRenderer : Renderer   （Canvas，720×1280 等比缩放 + 黑边）
  AndroidAudio    : Audio      （SoundPool + 合成 WAV；BGM 循环）
  AndroidSaveStore: SaveStore  （SharedPreferences）

core (纯 Java 17，无 Android import)
  core/            公共契约：Game、Scene、Renderer、Audio、SaveData、Palette、HudState、Controls、MathUtil
  core/world/      数据模型：World、Player、Enemy、TileMap、LevelDef、Pickup、Projectile、Particle、枚举
  core/content/    内容：Balance 数值、Levels 10 层地图/剧情、Texts 文案
  core/game/       系统：GameScene + 移动/碰撞/AI/战斗/交互/骇客/流程
  core/render/     渲染：WorldRenderer、HudRenderer、特效/角色绘制
  core/ui/         菜单：标题/选关/剧情/结算/设置/关于/教学

tools/preview (桌面 Java2D)
  Java2DRenderer : Renderer（BufferedImage + Graphics2D，字体 DroidSansFallbackFull）
  PreviewMain    自动遍历所有场景并输出 720×1280 PNG，用于无模拟器验收
```

## 关键设计

- **平台无关核心**：core 不依赖 Android，可 `javac` 直接编译、桌面 Java2D 渲染、单元测试。
- **无资源位图**：所有美术由 `Renderer` 图元绘制；字体使用系统字体（Android sans-serif / 桌面 CJK 字体）。
- **数据驱动关卡**：ASCII 地图 + 字符图例，便于关卡代理快速迭代。
- **输入**：Android 多点触控 → `Game.touch*` → 当前 `Scene`；坐标已换算到 720×1280 逻辑坐标。
- **存档**：SaveStore 抽象；Android 用 SharedPreferences，预览用内存。
- **音效**：`Audio` 抽象；Android 用 SoundPool + `res/raw/*.wav`（脚本合成）。
- **渲染性能**：表面只重绘一次/帧；避免 Bitmap；粒子与文字池化；暗角用径向渐变一次。

## 线程模型

GameView 独立渲染线程：固定步长 1/60s 累计器更新 + 渲染；触摸事件投递到 `Game`；Activity 生命周期 onPause 暂停音频。
