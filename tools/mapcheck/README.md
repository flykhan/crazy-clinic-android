# tools/mapcheck —— 10 层关卡地图 / 连通性校验

> Owner：level 代理。作用域：`core/.../content/**` + `tools/mapcheck/**`。

`Levels.java` 里的 10 层地图是**手绘 ASCII 源图**生成的，源图在 `maps/floor01.txt … floor10.txt`。
改地图 → 重新生成 → 跑校验，三步都通过才算完成。

## 目录

| 文件 | 作用 |
|---|---|
| `maps/floorNN.txt` | 手绘 ASCII 源图（唯一事实来源，含关卡名/副标题/器官顺序/简报） |
| `gen_levels.py` | 源图校验 + 生成 `core/.../content/Levels.java` |
| `ValidateLevels.java` | 从 `Levels.FLOORS` 读取并做连通性 / 配比 / 边界校验 |
| `check.sh` | 一键：校验源图 → 生成 → 编译 core → 跑校验 |

## 一键校验

```bash
source /vol2/1000/dsh/workspace/.toolchain/env.sh
cd /vol2/1000/dsh/workspace/crazy-clinic
bash tools/mapcheck/check.sh
```

期望输出：

```text
[ 1] 精神病学       26x22  敌人  2  C1 k1 门+1 | 首武器 6格 首钥匙 13格  ✔
...
[10] 屋顶         32x24  敌人  5  C1 k1 门+1 | 首武器 16格 首钥匙 23格  ✔

ALL GREEN ✔  10/10 层通过（尺寸 / S·E·C·k / 外圈封闭 / S→k→+→C→E 可达 / 敌人与物品位置 / 敌人配比）
```

## 手动命令

```bash
source /vol2/1000/dsh/workspace/.toolchain/env.sh
cd /vol2/1000/dsh/workspace/crazy-clinic

# 1) 只校验源图（不写文件）
python3 tools/mapcheck/gen_levels.py --check

# 2) 由源图生成 Levels.java
python3 tools/mapcheck/gen_levels.py

# 3) 编译 core（契约要求）
javac -encoding UTF-8 -d /tmp/corebuild $(find core/src/main/java -name '*.java')

# 4) 编译并运行校验器
javac -encoding UTF-8 -cp /tmp/corebuild -d /tmp/mapcheck tools/mapcheck/ValidateLevels.java
java -cp /tmp/corebuild:/tmp/mapcheck ValidateLevels
```

> 其他代理改到一半导致 core 暂时编不过时，可以只编译校验所需的最小集合：
>
> ```bash
> mkdir -p /tmp/stub/com/crazyclinic/core
> printf 'package com.crazyclinic.core;\npublic class Game { public static final int FLOOR_COUNT = 10; }\n' \
>   > /tmp/stub/com/crazyclinic/core/Game.java
> rm -rf /tmp/cb2 && mkdir -p /tmp/cb2
> javac -encoding UTF-8 -d /tmp/cb2 /tmp/stub/com/crazyclinic/core/Game.java \
>   core/src/main/java/com/crazyclinic/core/{MathUtil,Palette,Renderer,SaveData}.java \
>   $(find core/src/main/java/com/crazyclinic/core/world -name '*.java') \
>   $(find core/src/main/java/com/crazyclinic/core/content -name '*.java') \
>   tools/mapcheck/ValidateLevels.java
> java -cp /tmp/cb2 ValidateLevels
> ```

## 地图源图格式

```text
name: 精神病学
subtitle: 教学病区
organ: HEART            # 器官顺序，逗号分隔；NONE = 本层无器官
rooms: 6                # 设计房间数（说明用）
boss: 0                 # 1 = BOSS 层
intro: 第一句|第二句|第三句    # | 分段，生成时转成 \n
---
###W#######W#########W####
...（ASCII 地图行）
```

图例（严格按 `docs/CONTRACT.md §4`，大小写敏感，**不得出现其它字符**）：

| 字符 | 含义 | 字符 | 含义 |
|---|---|---|---|
| `#` 墙 | `.` 地板 | `+` 锁门 | `-` 开门 |
| `E` 电梯 | `C` 安保电脑 | `L` 储物柜 | `X` 箱子（障碍） |
| `B` 病床 | `W` 窗户墙 | `S` 出生点 | `k` 钥匙卡 |
| `G` 看守人 / 守卫 | `D` 医生 | `N` 护士 | `F` 感冒病人 |
| `T` 中心助手 | `H` 哨兵枪 | `Z` BOSS 赞恩 | `o` 器官（按 organ 顺序） |
| `1` 手枪 | `2` 霰弹枪 | `3` 泰瑟枪 | `4` 斧头 |
| `5` 手术刀 | `6` 除颤器 | `7` 本生灯 | `8` 爆炸试管 |
| `m` 小药包 | `h` 药包 | `M` 大药包 | `p` 止痛药 |
| `s` 类固醇 | `e` 能量饮料 | `i` 红细胞生成素 | `v` 隐形药 |

任何非图例字符都会被 `TileMap.parse` 当成**地板**（不是墙），会悄悄打出穿墙洞——所以 `gen_levels.py`
在生成前会硬校验字符集。

## 校验器检查项（`ValidateLevels.java`）

1. 尺寸 24~40 列 × 22~34 行；
2. 恰好 1 个 `S`、1 个 `E`，`C` 1~3 台、钥匙卡 1~4 把、至少 1 扇 `+`；
3. 外圈全部封闭（边界都是实心 `#`/`W`，无泄漏）；
4. 不拿钥匙：`S` 能到至少 1 张钥匙卡；拿钥匙后：`S` 能到全部电脑（相邻可交互）、电梯、全部物品；
5. 每层至少有一处 `C` 或 `E` 被锁门真正挡住（钥匙门槛生效），且**锁门数 ≤ 钥匙卡数**（引擎按门消耗钥匙卡，防软锁）；
6. 敌人不卡墙、不孤立；敌人/电脑/钥匙数量与 `research.md §5.6` 一致；
7. 第 10 层必须含 `Z`、其它层不得有 `Z`；第 1 层首武器/首钥匙 BFS 距离 ≤ 15 格（1.5 屏）。

## 10 层设计总览

| 层 | 病区 | 尺寸 | 房间 | 敌人（G/D/F/T/N/H/Z） | C | k | `+` | 新解锁 | 目标时长 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 精神病学（教学） | 26×22 | 6 | 2/0/0/0/0/0/0 | 1 | 1 | 1 | 斧头·手枪·手术刀 | 4~6 min |
| 2 | 门诊·保健站 | 28×24 | 8 | 3/0/2/0/0/1/0 | 1 | 2 | 2 | 除颤器·本生灯 | 6~8 |
| 3 | 急症室 | 30×26 | 9 | 3/1/2/0/0/1/0 | 2 | 2 | 2 | 泰瑟枪 | 7~9 |
| 4 | 内科 | 30×28 | 10 | 4/1/2/1/0/1/0 | 2 | 2 | 2 | 霰弹枪 | 8~10 |
| 5 | 外科 | 32×26 | 10 | 3/2/1/1/1/1/0 | 2 | 3 | 3 | 爆炸试管 | 8~10 |
| 6 | 放射学 | 32×30 | 11 | 4/2/1/2/1/2/0 | 3 | 3 | 2 | 隐形药 | 9~11 |
| 7 | 心脏病·ICU | 34×28 | 11 | 5/2/2/2/1/2/0 | 3 | 3 | 3 | 红细胞生成素·大药包 | 9~12 |
| 8 | 实验室 | 34×30 | 12 | 5/2/2/3/2/3/0 | 3 | 3 | 3 | 器官 ×2 | 10~13 |
| 9 | 办事处·总部 | 36×32 | 12 | 7/2/1/3/2/3/0 | 3 | 4 | 2 | 全武器补给 | 10~13 |
| 10 | 屋顶（BOSS） | 32×24 | 4 | 2/0/0/0/0/2/1 | 1 | 1 | 1 | 结局 | 5~7 |

说明：

- `G` 同时代表看守人与守卫（图例没有独立字符）；表中 G 数 = §5.6 的“看守 + 守卫”之和。
- 第 10 层按任务指定收敛为 **Z + 2G + 2H** 的掩体竞技场（8 个箱子 + 2 段矮墙做掩体，
  中央开阔，边缘有掩体；研究表的 4G+1N 版本对单人过难，遂删去护士、守卫减半）。
- 房间数按“有独立出入口的封闭区域”计，走廊不计；与 §5.6 表的房间数一致。
- 器官顺序：L1 心 → L2 腿 → L3 肺 → L4 肾 → L5 肝 → L6 脑 → L7 双臂 → L8 心+肺（器官 ×2）；
  L9/L10 不再放器官，保证永久强化节奏。
- 教学层：出生点 6 格内就是斧头、12 格内拿到手枪、13 格内拿到钥匙卡（都 ≤ 1.5 屏）；
  钥匙 → 安保电脑（锁门在护士站与安保室之间）→ 电梯的动线一眼可见。
