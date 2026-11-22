#!/usr/bin/env bash
# 10 层关卡数据生成 + 连通性校验（level 代理维护）
# 用法：bash tools/mapcheck/check.sh
set -euo pipefail
cd "$(dirname "$0")/../.."

source /vol2/1000/dsh/workspace/.toolchain/env.sh

echo "== 1/3 校验源图（字符/尺寸/外圈/关键对象） =="
python3 tools/mapcheck/gen_levels.py --check

echo "== 2/3 生成 core/content/Levels.java =="
python3 tools/mapcheck/gen_levels.py

echo "== 3/3 编译 core + 运行连通性校验 =="
rm -rf /tmp/cc-corebuild /tmp/cc-mapcheck
mkdir -p /tmp/cc-corebuild /tmp/cc-mapcheck
javac -encoding UTF-8 -d /tmp/cc-corebuild $(find core/src/main/java -name '*.java')
javac -encoding UTF-8 -cp /tmp/cc-corebuild -d /tmp/cc-mapcheck tools/mapcheck/ValidateLevels.java
java -cp /tmp/cc-corebuild:/tmp/cc-mapcheck ValidateLevels
