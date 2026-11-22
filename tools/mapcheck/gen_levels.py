#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""从手绘 ASCII 源图生成 core/content/Levels.java。

源图：tools/mapcheck/maps/floorNN.txt
  name: 病区名
  subtitle: 副标题
  organ: 器官顺序（逗号分隔，NONE 表示无）
  rooms: 房间数（说明用）
  boss: 0/1
  intro: 关卡简报，用 | 分段
  ---
  ASCII 地图行

用法：
  python3 tools/mapcheck/gen_levels.py            # 生成 Levels.java
  python3 tools/mapcheck/gen_levels.py --check    # 只校验源图，不写文件
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
MAPS = os.path.join(HERE, 'maps')
OUT = os.path.join(ROOT, 'core', 'src', 'main', 'java', 'com', 'crazyclinic', 'core', 'content', 'Levels.java')

LEGEND = set('.#+-ECLXBWSG DNFTHZ12345678mhMpseivok'.replace(' ', ''))
SOLID = set('#ECLXBW')


class Floor:
    def __init__(self, idx, meta, rows):
        self.idx = idx
        self.meta = meta
        self.rows = rows

    @property
    def name(self):
        return self.meta['name']

    @property
    def subtitle(self):
        return self.meta['subtitle']

    @property
    def intro_lines(self):
        return [s for s in self.meta.get('intro', '').split('|') if s]

    @property
    def organs(self):
        raw = self.meta.get('organ', 'NONE').strip()
        if not raw or raw.upper() == 'NONE':
            return []
        return [s.strip().upper() for s in raw.split(',') if s.strip()]

    @property
    def is_boss(self):
        return self.meta.get('boss', '0').strip() == '1'

    def count(self, chars):
        return sum(1 for r in self.rows for c in r if c in chars)


def parse_floor(path):
    meta, rows, in_rows = {}, [], False
    with open(path, encoding='utf-8') as fh:
        for line in fh.read().split('\n'):
            if not in_rows:
                if line.strip() == '---':
                    in_rows = True
                elif ':' in line:
                    k, v = line.split(':', 1)
                    meta[k.strip()] = v.strip()
            else:
                if line.strip() == '':
                    continue
                rows.append(line.rstrip('\n'))
    if not rows:
        raise SystemExit(f'{path}: 没有地图行')
    return meta, rows


def validate(fl):
    errs = []
    rows = fl.rows
    w = max(len(r) for r in rows)
    h = len(rows)
    if not (24 <= w <= 40):
        errs.append(f'{w} 列，超出 24~40')
    if not (22 <= h <= 34):
        errs.append(f'{h} 行，超出 22~34')
    for y, r in enumerate(rows):
        if len(r) != w:
            errs.append(f'第 {y} 行长度 {len(r)} != {w}')
        for x, c in enumerate(r):
            if c not in LEGEND:
                errs.append(f'非法字符 {c!r} at ({x},{y})')
    for x in range(w):
        for y in (0, h - 1):
            if rows[y][x] not in '#W':
                errs.append(f'外圈开口 ({x},{y}) = {rows[y][x]!r}')
    for y in range(h):
        for x in (0, w - 1):
            if rows[y][x] not in '#W':
                errs.append(f'外圈开口 ({x},{y}) = {rows[y][x]!r}')
    counts = {ch: fl.count(ch) for ch in 'SECk+Z'}
    if counts['S'] != 1:
        errs.append(f"S 数量 {counts['S']} != 1")
    if counts['E'] != 1:
        errs.append(f"E 数量 {counts['E']} != 1")
    if not 1 <= counts['C'] <= 3:
        errs.append(f"C 数量 {counts['C']} 不在 1~3")
    if not 1 <= counts['k'] <= 4:
        errs.append(f"k 数量 {counts['k']} 不在 1~4")
    if counts['+'] < 1:
        errs.append('没有锁门 +')
    if fl.is_boss and counts['Z'] < 1:
        errs.append('BOSS 层缺少 Z')
    if not fl.is_boss and counts['Z'] > 0:
        errs.append('非 BOSS 层出现 Z')
    return errs


def esc(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')


def java_floor(fl):
    rows = fl.rows
    organ = ', '.join('Organ.' + o for o in fl.organs)
    lines = []
    lines.append(f'    /** 第 {fl.idx} 层：{fl.name}·{fl.subtitle}（{fl.meta.get("rooms", "?")} 个房间，'
                 f'{sum(fl.count(c) for c in "GDNFTHZ")} 名敌人，{fl.count("C")} 台电脑，{fl.count("k")} 把钥匙）。*/')
    lines.append(f'    private static LevelDef floor{fl.idx}() {{')
    lines.append(f'        return new LevelDef({fl.idx},')
    lines.append(f'                "{esc(fl.name)}", "{esc(fl.subtitle)}",')
    intro = fl.intro_lines
    for i, line in enumerate(intro):
        suffix = ',' if i == len(intro) - 1 else ' +'
        prefix = '                "' if i == 0 else '              "'
        lines.append(f'{prefix}{esc(line)}\\n"{suffix}')
    lines.append('                new String[]{')
    for r in rows:
        lines.append(f'                        "{r}",')
    lines.append('                },')
    lines.append(f'                new Organ[]{{{organ}}});')
    lines.append('    }')
    return '\n'.join(lines)


def main():
    check_only = '--check' in sys.argv[1:]
    floors = []
    bad = 0
    for i in range(1, 11):
        path = os.path.join(MAPS, f'floor{i:02d}.txt')
        meta, rows = parse_floor(path)
        for required in ('name', 'subtitle', 'intro'):
            if required not in meta:
                raise SystemExit(f'{path}: 缺少字段 {required}')
        fl = Floor(i, meta, rows)
        errs = validate(fl)
        if errs:
            bad += len(errs)
            print(f'  ✗ floor{i:02d}:')
            for e in errs:
                print('      -', e)
        floors.append(fl)
    if bad:
        raise SystemExit(f'源图校验失败：{bad} 个错误')
    print('源图校验通过：10 层，字符/尺寸/外圈/关键对象齐全。')
    if check_only:
        return
    body = '\n\n'.join(java_floor(fl) for fl in floors)
    java = f'''package com.crazyclinic.core.content;

import com.crazyclinic.core.world.LevelDef;
import com.crazyclinic.core.world.Organ;

/**
 * 10 层手绘 ASCII 关卡数据（由生成器维护，请勿手改）。
 *
 * 源图：tools/mapcheck/maps/floor01.txt ... floor10.txt
 * 生成：python3 tools/mapcheck/gen_levels.py
 * 校验：tools/mapcheck/ValidateLevels.java（见 tools/mapcheck/README.md）
 *
 * 图例见 docs/CONTRACT.md §4 与 LevelDef/TileType：
 *   # 墙  . 地板  + 锁门  - 开门  E 电梯  C 安保电脑  L 储物柜  X 箱子  B 病床  W 窗户  S 出生点
 *   G 看守人/守卫  D 医生  N 护士  F 感冒病人  T 中心助手  H 哨兵枪  Z BOSS
 *   1 手枪  2 霰弹枪  3 泰瑟枪  4 斧头  5 手术刀  6 除颤器  7 本生灯  8 爆炸试管
 *   m 小药包  h 药包  M 大药包  p 止痛药  s 类固醇  e 能量饮料  i 红细胞生成素  v 隐形药
 *   k 钥匙卡  o 器官（按 organOrder 顺序）
 */
public final class Levels {{
    public static final LevelDef[] FLOORS = build();

    private Levels() {{}}

    public static LevelDef get(int floor) {{
        int i = Math.max(1, Math.min(FLOORS.length, floor)) - 1;
        return FLOORS[i];
    }}

    private static LevelDef[] build() {{
        return new LevelDef[]{{
                floor1(), floor2(), floor3(), floor4(), floor5(),
                floor6(), floor7(), floor8(), floor9(), floor10()
        }};
    }}

{body}
}}
'''
    with open(OUT, 'w', encoding='utf-8') as fh:
        fh.write(java)
    print(f'已生成 {os.path.relpath(OUT, ROOT)}')


if __name__ == '__main__':
    main()
