# scripts/ 使用说明

> 所有脚本自带 `source /vol2/1000/dsh/workspace/.toolchain/env.sh`，在任何目录执行都可以。
> 不要 `sudo`，不要改 `.toolchain/`。

## 1. 构建 APK

```bash
# debug APK（安装测试用，applicationId 带 .debug 后缀，debug 签名）
bash scripts/build.sh
# 等价：bash scripts/build.sh debug

# release APK（自动使用 .keystore/crazyclinic.jks 签名；没有 keystore 则回退 debug 签名）
bash scripts/build.sh release

# 清理
bash scripts/build.sh clean
```

产物：

| 模式 | 路径 |
|---|---|
| debug | `app/build/outputs/apk/debug/app-debug.apk` |
| release | `app/build/outputs/apk/release/app-release.apk` |

脚本会自动：
1. 若 `app/src/main/res/raw/` 为空，先运行 `python3 tools/audio/gen_audio.py` 生成 38 个程序化 WAV；
2. 调用 Gradle wrapper（`./gradlew`，缺失时用 `$GRADLE_BIN`）执行 `assembleXxx --no-daemon -Xmx2g`；
3. 用 `aapt2 dump badging` 打印包名/版本/SDK，统计打包的 `res/raw/*.wav`；
4. 用 `apksigner verify --print-certs` 打印签名信息。

也可以直接跑 Gradle：

```bash
source /vol2/1000/dsh/workspace/.toolchain/env.sh
./gradlew :app:assembleDebug --no-daemon \
  -Dorg.gradle.jvmargs="-Xmx2g -Dfile.encoding=UTF-8"
```

## 2. 桌面预览截图（无模拟器 / 无显示环境）

```bash
bash scripts/preview.sh                     # 全部 13 个场景 → docs/screenshots/*.png
bash scripts/preview.sh title settings      # 只出指定场景（可多个）
bash scripts/preview.sh --list              # 列出所有场景名
bash scripts/preview.sh --out=/tmp/shots    # 指定输出目录
```

场景名：`title` `level_select` `intro` `gameplay_floor1` `gameplay_floor5`
`gameplay_floor10` `hacking` `pause` `result_win` `result_lose` `settings` `about` `tutorial`

- 输出固定 720×1280 PNG（脚本用 Python 解析 PNG IHDR 强制校验尺寸，不合格会报错退出）。
- 中文用 `/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf`；该字体**不含 ASCII**，
  渲染器会逐字回退到 DejaVu Sans，因此 `/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf` 必须存在。
- 实现：`tools/preview/src/com/crazyclinic/preview/`
  - `Java2DRenderer`：Java2D 版 `Renderer`（BufferedImage 720×1280 + Graphics2D 抗锯齿）
  - `PreviewMain`：构造 `Game`，遍历场景输出 PNG；游戏内场景会用 `touchDown/Move/Up`
    模拟双摇杆+开火，并自动把主角传送到敌人附近
  - `SilentAudio` / `MemSaveStore`：预览用静音音频与内存存档
- 预览编译产物在 `build/preview/`（已 gitignore）。

## 3. 程序化音频

```bash
python3 tools/audio/gen_audio.py                 # 生成全部 32 sfx + 6 mus 到 app/src/main/res/raw
python3 tools/audio/gen_audio.py --list          # 列出文件名
python3 tools/audio/gen_audio.py --only sfx_pistol mus_title   # 只生成指定文件
python3 tools/audio/gen_audio.py --verify        # 校验已有 WAV：时长/峰值/RMS/削波
```

- 纯 Python 标准库合成（振荡器 + 噪声 + 包络 + 滤波），22050Hz / 16bit / 单声道，不引入外部素材。
- 文件名与 `core/Sfx`、`core/Music` 枚举一一对应（小写，前缀 `sfx_` / `mus_`）。
- 全部 38 个文件约 5.0MB，APK 内压缩后满足体积限制。

## 4. 常见问题

- **Gradle 报 `Unable to initialize metrics ... /home/flykhan/.android`**：无害警告，工具链把
  `ANDROID_USER_HOME` 指到了工作区内的 `.home/.android`，不影响构建。
- **javac 中文乱码**：所有 Gradle 编译任务已带 `-Dfile.encoding=UTF-8`；手工 javac 记得
  `-encoding UTF-8`。
- **首次 Gradle 构建很慢**：依赖已缓存在 `/vol2/1000/dsh/workspace/.gradle-home`，增量构建约 10~15s。
