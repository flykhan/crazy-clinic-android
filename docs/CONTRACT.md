# 开发契约（所有代理必读）

> 最后更新：Lead · 骨架 v1。接口变更必须先发消息给 Lead，不得单方面改别人的文件。

## 1. 项目定位

《疯狂诊所》= 2009 年 HandyGames《Crazy Hospital》中国移动渠道中文版（J2ME，240×320，俯视 3/4 视角动作 + 潜行逃脱）。
本仓库是**非商业同人重制**，Android 原生，**纯 Java + Android Canvas 程序化绘制**，不引入任何第三方库、位图素材、原版资源。
完整原版资料见 `docs/research.md`（598 行，必读）。

## 2. 技术基线

| 项 | 值 |
|---|---|
| 语言 | Java 17（core 纯 Java；app 用 Android framework） |
| 构建 | Gradle 8.7 + AGP 8.5.2（wrapper 由工具链生成） |
| SDK | minSdk 26 / compileSdk 34 / targetSdk 34 |
| 依赖 | 无（禁止 androidx、libGDX、第三方 jar） |
| 竖屏锁定 | `screenOrientation="portrait"`，全屏沉浸式 |
| 逻辑分辨率 | **720 × 1280**（竖屏） |
| 世界视口 | y = 0..960（720×960），下方 y = 960..1280 为控制区 |
| 世界瓦片 | **72 px**（原版 24px ×3） |
| 渲染 | Android Canvas（`AndroidRenderer`）+ 桌面 Java2D 预览（`Java2DRenderer`） |

### 构建

```bash
source /vol2/1000/dsh/workspace/.toolchain/env.sh
cd /vol2/1000/dsh/workspace/crazy-clinic
./gradlew :app:assembleDebug --no-daemon -Dorg.gradle.jvmargs="-Xmx2g -Dfile.encoding=UTF-8"
# 产物 app/build/outputs/apk/debug/app-debug.apk
```

快速只验证 core：`javac -encoding UTF-8 -d /tmp/corebuild $(find core/src/main/java -name '*.java')`。
桌面预览：`bash scripts/preview.sh`（由 android 代理实现，生成 720×1280 PNG 截图）。

## 3. 目录结构与文件所有权（并行写！不要越界）

| 目录/文件 | Owner | 内容 |
|---|---|---|
| `core/.../core/*.java`（根包：Renderer、Scene、Game、Audio、Sfx、Music、SaveData、SaveStore、MathUtil、Rect、Palette、HudState、Controls） | **Lead** | 冻结的公共契约；发现问题发消息给 Lead |
| `core/.../core/world/*.java` | **Lead** | 数据模型（World/Player/Enemy/Projectile/Pickup/Particle/FloatingText/Decal/TileMap/LevelDef/各枚举） |
| `core/.../core/content/Balance.java` | **level** | 数值表（可调，保持公开 API） |
| `core/.../core/content/Levels.java` | **level** | 10 层 ASCII 地图 + 剧情/简报文本 |
| `core/.../core/content/Texts.java` | **level**（Lead 初稿） | 全部中文 UI 文案 |
| `core/.../core/game/**` | **engine** | GameScene 完整实现 + 系统：移动/碰撞/AI/战斗/交互/骇客/流程/动画状态 |
| `core/.../core/render/**` | **art** | WorldRenderer / HudRenderer / Effects / 程序化角色绘制 |
| `core/.../core/ui/**` | **ui** | 标题/关卡选择/剧情/结算/设置/关于/教学 + UiKit |
| `app/**` | **android** | MainActivity、GameView、AndroidRenderer、AndroidAudio、AndroidSaveStore、图标、音效资源、res |
| `tools/preview/**`、`scripts/**`、`tools/audio/**` | **android** | 桌面预览截图工具、WAV 生成脚本、构建脚本 |
| `docs/**`、`README.md`、`.gitignore`、Gradle 根配置 | **Lead** | 文档与集成 |
| `research/**` | researcher | 已 gitignore，不提交 |

**规则**：只改自己所有权下的文件；新增文件只能放自己包内。接口/构造器/公开字段的任何变更 → 先发消息给 Lead + 受影响代理。

## 4. 核心 API（冻结）

### Renderer（平台无关绘制）
`clear / save / restore / translate / rotate(deg) / scale / clipRect / setAlpha / alpha / fillRect / fillRoundRect / strokeRoundRect / strokeRect / fillCircle / strokeCircle / line / fillTriangle / fillPolygon(float[]) / fillArc / fillVerticalGradient / fillRadialGradient(填满全屏) / setTextSize / setTextBold / setTextAlign(0左1中2右) / setTextColor / drawText(text,x,y=垂直中心) / measureText`。
坐标系 720×1280；无位图 API —— 所有美术必须用图元绘制。

### Scene
`enter(Game) / update(Game,dt) / render(Game,Renderer) / touchDown/touchMove/touchUp(Game,id,x,y) / backPressed(Game) / pause/resume(Game)`。

### Game（全局上下文，只读使用）
字段：`save, audio, store, renderer, scene, time, fade, currentFloor, runKills, runTime`。
导航：`gotoTitle/gotoLevelSelect/gotoIntro(floor)/startFloor(floor)/gotoResult(win,floor,kills,elapsed)/gotoSettings/gotoAbout/gotoTutorial/backPressed/pause/resume`。
存档：`isFloorUnlocked(f) / unlockFloor(f) / saveNow()`。

### 菜单场景构造器（ui 代理实现）
`new TitleScene()`、`new LevelSelectScene()`、`new IntroScene(int floor)`、`new ResultScene(boolean win, int floor, int kills, float elapsed)`、`new SettingsScene()`、`new AboutScene()`、`new TutorialScene()`。
`new GameScene(int floor)`：关卡主场景（engine 代理实现），必须暴露：
```java
public final int floor;
public final World world;
public final HudState hud;
public GameScene(int floor);
```

### World 数据模型（Lead 已实现，可读可写字段）
- `World`：`level, map, player, enemies, projectiles, pickups, particles, floatingTexts, decals, cameraX/Y, shake, time, kills, won, lost, elevatorUnlocked, camerasDisabled, objective, alert, floor`；
  方法：`centerCameraOnPlayer/clampCamera/collidesCircle/enemyAt/pickupAt/boss/addParticle/addBlood/addFloatingText/addDecal/lineOfSight/playerSpeedNow`。
  常量：`VIEW_W=720, VIEW_H=960`。
- `Player`：`x,y,angle,aimAngle,radius,hp,maxHp,stamina,maxStamina,speed,sprintSpeed,weapon,owned[],ammo[],item,itemCount,hasKeycard,dead,moving,sprinting,invuln,hurtFlash,dodgeTimer,dodgeCd,attackCd,muzzleFlash,speedBoostTimer,epoSlowTimer,damageBoostTimer,noDamageTimer,invisTimer,staminaRegenDelay,fireHold,armsBonus`；
  方法：`init(SaveData) / applyOrganBonus(Organ) / meleeMultiplier() / hasWeapon/giveWeapon/giveAmmo/isInvisible/isInvulnerable`。
- `Enemy`：`type,turret,x,y,angle,radius,hp,maxHp,speed,sightRange,attackRange,attackCooldown,damage,state,stateTimer,alertTimer,attackTimer,wanderTimer,wanderAngle,homeX,homeY,hurtFlash,deathTimer,muzzleFlash,stunTimer,reviveTimer,bodyTimer,dead`；`isDead()`。
- `Projectile / Pickup / Particle / FloatingText / Decal`：见源码。
- `TileMap`：`TILE=72, width,height,type/solid/opaque/solidAt/opaqueAt/tx/ty/cx/cy/pixelWidth/pixelHeight/parse(String[])`。
- `LevelDef`：`index,name,subtitle,intro,map,spawnX/Y,enemies,items,bossLevel`。
- 枚举：`WeaponType(9)`、`EnemyType(7)`、`ItemType(11)`、`Organ(7, bit/label/desc)`、`ProjectileType`、`EnemyState`、`TileType`。

### HudState（engine 每帧写，art 读）
`moveStickActive/Cx/Cy/Dx/Dy`、`aimStickActive/Cx/Cy/Dx/Dy`、`attackBtn/interactBtn/dodgeBtn/weaponBtn/itemBtn`、`attackHeld`、`interactPrompt/interactAvailable`、`toast/toastTimer`、`damageFlash/healFlash/lowHealth`、`paused`、骇客字段（`hacking,hackCols/Rows/CursorX/Y/StartX/Y/GoalX/Y,TimeLeft/TimeTotal,Failed/Success/ResultTimer,hackCells`）、`floorClearTimer/showingFloorClear`、`bossHp/bossMaxHp/bossName`。

### Controls（触屏布局常量，engine 与 art 必须共用）
移动摇杆区 `x<340`、`STICK_RADIUS=150`；攻击 `(556,1046,r86)`、互动 `(430,1186,r56)`、闪避 `(668,1186,r56)`、换武器 `(78,990,128x66)`、道具 `(222,990,128x66)`、暂停 `(660,74,r38)`。

### Balance（level 代理维护，engine/art 只读）
`BASE_HP/BASE_STAMINA/BASE_SPEED/SPRINT_SPEED/...`、`weapon(WeaponType)→WeaponStats{damage,range,cooldown,maxAmmo,melee,automatic,pellets,spreadDeg,projectileSpeed,arcDeg}`、`enemy(EnemyType)→EnemyStats{hp,speed,damage,sightRange,attackRange,attackCooldown,radius,ranged,turret,projectileSpeed}`、物品常量 `HEAL_*/PAINKILLER_TIME/...`、骇客 `HACK_TIME/HACK_COLS/HACK_ROWS/HACK_CELL`。

### Levels / ASCII 图例（level 代理写地图）
`.java` 中 10 层，每层 `new LevelDef(index, name, subtitle, intro, rows, organOrder)`。
图例（大小写敏感）：

| 字符 | 含义 | 字符 | 含义 |
|---|---|---|---|
| `#` | 墙 | `G` | 看守人 |
| `.` | 地板 | `D` | 医生 |
| `+` | 锁着的门（需钥匙卡） | `N` | 护士 |
| `-` | 敞开的门 | `F` | 感冒病人 |
| `E` | 电梯（过关点） | `T` | 中心助手 |
| `C` | 安保电脑（骇客点） | `H` | 哨兵枪（固定炮台） |
| `L` | 储物柜（可搜） | `Z` | BOSS 赞恩 |
| `X` | 箱子（障碍） | `1` | 手枪 |
| `B` | 病床（障碍） | `2` | 霰弹枪 |
| `W` | 窗户墙 | `3` | 泰瑟枪 |
| `S` | 玩家出生点 | `4` | 斧头 |
| `m` | 小药包 +15 | `5` | 手术刀 |
| `h` | 药包 +30 | `6` | 除颤器 |
| `M` | 大药包 +60 | `7` | 本生灯 |
| `p` | 止痛药 | `8` | 爆炸试管 |
| `s` | 类固醇 | `k` | 钥匙卡 |
| `e` | 能量饮料 | `o` | 器官（按 organOrder 顺序分配） |
| `i` | 红细胞生成素 | `v` | 隐形药 |

地图建议：每层 24~40 列 × 22~34 行；至少 1 个 `S`、1 个 `E`、1 个 `C`、1 把 `k`、1 扇 `+`；房间+走廊结构；0.9~1.2 屏宽的首屏教学引导。

## 5. 质量与验收
- **必须**用 `javac` 编译 core 通过；Android 端 `./gradlew :app:assembleDebug` 通过。
- 桌面截图必须能跑通以下场景并出图（720×1280）：标题、关卡选择、剧情、游戏中（HUD+控件）、暂停、骇客、结算、设置、关于。
- 所有中文文案来自 `Texts`/`Levels`；不要写死重复文案（可临时，最终统一）。
- 帧率目标：中端机 ≥ 50fps；单帧绘制图元建议 < 4000 次；避免每帧 new 大对象（粒子/文字池由 art/engine 自行控制）。
- 不提交：`research/`、`.keystore/`、`build/`、`local.properties`、任何原版素材。
- 不要执行 `git` 命令（Lead 统一提交）；不要动别人的文件；改完自测后 send_message 向 Lead 汇报：改了什么、怎么验证、遗留问题、需要谁配合。
