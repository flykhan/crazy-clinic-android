#!/usr/bin/env bash
# =============================================================================
# 疯狂诊所 · Android 构建脚本
#
#   bash scripts/build.sh            # 构建 debug APK（默认，用于安装测试）
#   bash scripts/build.sh debug      # 同上
#   bash scripts/build.sh release    # 构建 release APK（用 .keystore/crazyclinic.jks 签名）
#   bash scripts/build.sh clean      # 清理 app/core 构建产物
#
# 环境：source /vol2/1000/dsh/workspace/.toolchain/env.sh（脚本内自动 source）
# 产物：app/build/outputs/apk/{debug,release}/*.apk
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# shellcheck source=/dev/null
source /vol2/1000/dsh/workspace/.toolchain/env.sh >/dev/null 2>&1 || true

MODE="${1:-debug}"
GRADLE_OPTS=(-Dorg.gradle.jvmargs="-Xmx2g -Dfile.encoding=UTF-8")

# --- 选择 Gradle：优先 wrapper，其次工具链内置 ---------------------------------
if [[ -x "$ROOT/gradlew" ]]; then
    GRADLE=("$ROOT/gradlew")
elif [[ -n "${GRADLE_BIN:-}" && -x "$GRADLE_BIN" ]]; then
    GRADLE=("$GRADLE_BIN")
else
    GRADLE=(gradle)
fi

# --- 音频资源：缺失则先用程序化脚本生成 ---------------------------------------
RAW_DIR="$ROOT/app/src/main/res/raw"
if [[ ! -d "$RAW_DIR" ]] || [[ -z "$(ls -A "$RAW_DIR" 2>/dev/null || true)" ]]; then
    echo "[build] res/raw 为空，生成程序化音频 ..."
    python3 "$ROOT/tools/audio/gen_audio.py"
fi
SFX_COUNT=$(ls "$RAW_DIR"/sfx_*.wav 2>/dev/null | wc -l | tr -d ' ')
MUS_COUNT=$(ls "$RAW_DIR"/mus_*.wav 2>/dev/null | wc -l | tr -d ' ')
echo "[build] audio: ${SFX_COUNT} sfx + ${MUS_COUNT} music in res/raw"

print_apk_info() {
    local apk="$1"
    if [[ ! -f "$apk" ]]; then
        echo "[build] ERROR: APK not found: $apk" >&2
        return 1
    fi
    local size
    size=$(du -h "$apk" | cut -f1)
    echo ""
    echo "============================================================"
    echo " APK      : $apk"
    echo " SIZE     : $size ($(stat -c%s "$apk") bytes)"
    echo "------------------------------------------------------------"
    "$BUILD_TOOLS/aapt2" dump badging "$apk" 2>/dev/null | head -3 || true
    echo "------------------------------------------------------------"
    # 资源完整性：raw 中的 wav 数量
    # release 构建 AGP 会缩短资源路径（如 res/-w.wav），所以只按后缀统计
    local wavs wavbytes
    wavs=$(unzip -l "$apk" 2>/dev/null | grep -c '\.wav$' || true)
    wavbytes=$(unzip -l "$apk" 2>/dev/null | awk '/\.wav$/ {s+=$1} END {printf "%.1f", s/1048576}')
    echo " res/raw  : ${wavs:-0} wav packaged (${wavbytes:-0} MB)"
    # 签名信息
    if [[ -x "$BUILD_TOOLS/apksigner" ]]; then
        echo " signature: $("$BUILD_TOOLS/apksigner" verify --print-certs "$apk" 2>/dev/null | head -2 | tail -1 || echo '(verify failed)')"
    fi
    echo "============================================================"
    echo ""
}

case "$MODE" in
    debug)
        echo "[build] gradle: ${GRADLE[*]} :app:assembleDebug"
        "${GRADLE[@]}" :app:assembleDebug --no-daemon "${GRADLE_OPTS[@]}"
        print_apk_info "$ROOT/app/build/outputs/apk/debug/app-debug.apk"
        ;;
    release)
        echo "[build] gradle: ${GRADLE[*]} :app:assembleRelease"
        "${GRADLE[@]}" :app:assembleRelease --no-daemon "${GRADLE_OPTS[@]}"
        APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
        if [[ ! -f "$APK" ]]; then
            # 不同 AGP 版本可能产出未签名名称
            APK=$(ls "$ROOT"/app/build/outputs/apk/release/*.apk 2>/dev/null | head -1 || true)
        fi
        print_apk_info "$APK"
        if [[ -x "$BUILD_TOOLS/apksigner" ]]; then
            echo "[build] 签名校验："
            "$BUILD_TOOLS/apksigner" verify --verbose --print-certs "$APK" 2>&1 | head -12 || true
        fi
        ;;
    clean)
        echo "[build] gradle clean"
        "${GRADLE[@]}" clean --no-daemon "${GRADLE_OPTS[@]}"
        ;;
    *)
        echo "用法: bash scripts/build.sh [debug|release|clean]" >&2
        exit 2
        ;;
esac
