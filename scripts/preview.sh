#!/usr/bin/env bash
# =============================================================================
# 疯狂诊所 · 桌面预览截图
#
#   bash scripts/preview.sh                    # 生成全部 13 张 720x1280 PNG
#   bash scripts/preview.sh title pause        # 只生成指定场景
#   bash scripts/preview.sh --list             # 列出所有场景名
#   bash scripts/preview.sh --out=/tmp/shots   # 指定输出目录
#
# 输出默认写入 docs/screenshots/ 。无需模拟器、无需显示环境（headless Java2D）。
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# shellcheck source=/dev/null
source /vol2/1000/dsh/workspace/.toolchain/env.sh >/dev/null 2>&1 || true

BUILD_DIR="$ROOT/build/preview"
CLASSES="$BUILD_DIR/classes"
OUT_DIR_DEFAULT="$ROOT/docs/screenshots"

# 多个代理可能同时跑预览：加文件锁串行化编译/输出，避免互相删 classes 导致
# NoClassDefFoundError 或 PNG 写坏。
mkdir -p "$BUILD_DIR"
if command -v flock >/dev/null 2>&1; then
    exec 9>"$BUILD_DIR/.preview.lock"
    flock 9
fi

# 允许 CC_PREVIEW_OUT 覆盖输出目录
if [[ -n "${CC_PREVIEW_OUT:-}" ]]; then
    OUT_DIR_DEFAULT="$CC_PREVIEW_OUT"
fi

# --- 收集参数：--out= 透传给 Main，同时用于尺寸校验 ---------------------------
ARGS=()
OUT_DIR="$OUT_DIR_DEFAULT"
for arg in "$@"; do
    case "$arg" in
        --out=*) OUT_DIR="${arg#--out=}" ;;
    esac
    ARGS+=("$arg")
done

mkdir -p "$OUT_DIR"
rm -rf "$CLASSES"
mkdir -p "$CLASSES"

echo "[preview] javac core + tools/preview ..."
CORE_SRC=$(find core/src/main/java -name '*.java' | sort)
PREVIEW_SRC=$(find tools/preview/src -name '*.java' | sort)
# shellcheck disable=SC2086
javac -encoding UTF-8 -source 17 -target 17 -nowarn \
    -d "$CLASSES" \
    $CORE_SRC $PREVIEW_SRC

echo "[preview] running headless renderer ..."
java -Djava.awt.headless=true -Dfile.encoding=UTF-8 \
    -cp "$CLASSES" com.crazyclinic.preview.PreviewMain "${ARGS[@]}"

# --- 校验 PNG 尺寸（必须 720x1280） ------------------------------------------
echo "[preview] verifying PNG dimensions in $OUT_DIR ..."
python3 - "$OUT_DIR" <<'PY'
import os, struct, sys

out = sys.argv[1]
# 只校验本次出现的文件（全部也很快），按名字排序
names = sorted(f for f in os.listdir(out) if f.endswith('.png'))
if not names:
    print("[preview] ERROR: no PNG produced in", out)
    sys.exit(1)
bad = 0
total = 0
for name in names:
    path = os.path.join(out, name)
    with open(path, 'rb') as fh:
        head = fh.read(33)
    if len(head) < 33 or head[:8] != b'\x89PNG\r\n\x1a\n' or head[12:16] != b'IHDR':
        print("[preview] ERROR: not a valid PNG:", name)
        bad += 1
        continue
    w, h = struct.unpack('>II', head[16:24])
    total += 1
    if (w, h) != (720, 1280):
        print("[preview] ERROR: %s is %dx%d (expected 720x1280)" % (name, w, h))
        bad += 1
    else:
        print("  OK  %-24s %4dx%-4d  %6d bytes" % (name, w, h, os.path.getsize(path)))
print("[preview] %d PNG checked, %d bad" % (total, bad))
sys.exit(1 if bad else 0)
PY

echo "[preview] done -> $OUT_DIR"
