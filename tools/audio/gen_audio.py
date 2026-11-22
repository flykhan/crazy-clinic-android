#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
疯狂诊所 · 程序化音频生成器（纯 Python 标准库，无第三方依赖）。

生成 22050Hz / 16bit / 单声道 WAV：
  app/src/main/res/raw/sfx_*.wav   32 个音效（与 core/Sfx 枚举一一对应，小写）
  app/src/main/res/raw/mus_*.wav   6 首循环 BGM（12~25s，8-bit/医院氛围合成）

用法：
  python3 tools/audio/gen_audio.py                # 生成全部
  python3 tools/audio/gen_audio.py --list         # 列出文件名
  python3 tools/audio/gen_audio.py --only sfx_ui_click mus_title
  python3 tools/audio/gen_audio.py --out /tmp/raw

设计原则：不引入任何外部音频素材，全部用振荡器/噪声/包络/滤波器合成。
"""

import argparse
import math
import os
import random
import struct
import sys
import time
import wave

SR = 22050
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DEFAULT_OUT = os.path.join(ROOT, "app", "src", "main", "res", "raw")

TAU = 2.0 * math.pi


# ---------------------------------------------------------------------------
# 基础波形
# ---------------------------------------------------------------------------

def _wave(kind, phase):
    if kind == "sine":
        return math.sin(phase)
    if kind == "square":
        return 1.0 if math.sin(phase) >= 0.0 else -1.0
    if kind == "tri":
        x = (phase / TAU) % 1.0
        return 4.0 * abs(x - 0.5) - 1.0
    if kind == "saw":
        x = (phase / TAU) % 1.0
        return 2.0 * x - 1.0
    return math.sin(phase)


def _env(t, dur, attack, decay, sustain):
    """简单 ADSR（无 release，decay 到 sustain 后保持）。"""
    if t < 0:
        return 0.0
    if t < attack:
        return t / max(attack, 1e-6)
    d = decay if decay > 0 else max(dur - attack, 1e-6)
    if t < attack + d:
        k = (t - attack) / d
        return 1.0 + (sustain - 1.0) * k
    return sustain


def tone(dur, f0, f1=None, kind="sine", vol=0.5, attack=0.004,
         decay=None, sustain=0.0, detune=0.0, vibrato=0.0, seed=1):
    """单个振荡器音符（频率可线性扫频）。"""
    n = int(max(0.0, dur) * SR)
    out = [0.0] * n
    if n == 0:
        return out
    rng = random.Random(seed)
    phase = 0.0
    for i in range(n):
        t = i / SR
        f = f0 if f1 is None else f0 + (f1 - f0) * (i / max(1, n - 1))
        if vibrato:
            f *= 1.0 + vibrato * math.sin(TAU * 5.5 * t)
        if detune:
            f *= 1.0 + detune * math.sin(TAU * 0.27 * t + rng.random())
        phase += TAU * f / SR
        s = _wave(kind, phase)
        out[i] = s * _env(t, dur, attack, decay if decay is not None else dur * 0.6, sustain) * vol
    return out


def noise(dur, vol=0.3, attack=0.002, decay=None, sustain=0.0, lp=0.0, hp=0.0, seed=1):
    """噪声（可选一阶低通/高通）。lp/hp 为 0..1 系数。"""
    n = int(max(0.0, dur) * SR)
    out = [0.0] * n
    if n == 0:
        return out
    rng = random.Random(seed)
    prev = 0.0
    prev_out = 0.0
    for i in range(n):
        t = i / SR
        x = rng.uniform(-1.0, 1.0)
        if lp > 0.0:
            prev = prev + lp * (x - prev)
            x = prev
        if hp > 0.0:
            # 一阶高通 = 原始 - 低通
            prev_out = prev_out + hp * (x - prev_out)
            x = x - prev_out
        out[i] = x * _env(t, dur, attack, decay if decay is not None else dur * 0.5, sustain) * vol
    return out


def mix(*bufs):
    n = max((len(b) for b in bufs), default=0)
    out = [0.0] * n
    for b in bufs:
        for i, v in enumerate(b):
            out[i] += v
    return out


def add_at(buf, src, start_sec, gain=1.0):
    """把 src 叠加到 buf 的 start_sec 处（原地）。"""
    i0 = int(start_sec * SR)
    for i, v in enumerate(src):
        idx = i0 + i
        if 0 <= idx < len(buf):
            buf[idx] += v * gain
    return buf


def lowpass(buf, cutoff_hz):
    if cutoff_hz <= 0:
        return buf
    k = 1.0 - math.exp(-TAU * cutoff_hz / SR)
    prev = 0.0
    for i, v in enumerate(buf):
        prev += k * (v - prev)
        buf[i] = prev
    return buf


def fade_edges(buf, fade_in=0.004, fade_out=0.02):
    n = len(buf)
    fi = int(fade_in * SR)
    fo = int(fade_out * SR)
    for i in range(min(fi, n)):
        buf[i] *= i / max(1, fi)
    for i in range(min(fo, n)):
        buf[n - 1 - i] *= i / max(1, fo)
    return buf


def normalize(buf, peak=0.88):
    m = max((abs(v) for v in buf), default=0.0)
    if m < 1e-9:
        return buf
    g = peak / m
    for i in range(len(buf)):
        buf[i] *= g
    return buf


def write_wav(path, buf, tail_silence=0.0):
    buf = list(buf)
    if tail_silence > 0:
        buf.extend([0.0] * int(tail_silence * SR))
    frames = bytearray()
    for v in buf:
        if v > 1.0:
            v = 1.0
        elif v < -1.0:
            v = -1.0
        frames += struct.pack("<h", int(v * 32767.0))
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(bytes(frames))
    return len(buf)


# ---------------------------------------------------------------------------
# 32 个音效
# ---------------------------------------------------------------------------

def sfx_ui_click():
    b = mix(tone(0.045, 1250, 900, "sine", 0.5, attack=0.001, decay=0.03),
            noise(0.02, 0.12, attack=0.001, decay=0.015, hp=0.6, seed=2))
    return normalize(fade_edges(b, 0.001, 0.01), 0.7)


def sfx_ui_confirm():
    b = [0.0] * int(0.25 * SR)
    add_at(b, tone(0.07, 660, 660, "tri", 0.35, attack=0.002, decay=0.05), 0.0)
    add_at(b, tone(0.09, 990, 990, "tri", 0.35, attack=0.002, decay=0.07), 0.075)
    return normalize(fade_edges(b, 0.002, 0.02), 0.7)


def sfx_ui_back():
    b = tone(0.13, 780, 420, "tri", 0.4, attack=0.002, decay=0.1)
    return normalize(fade_edges(b, 0.002, 0.02), 0.7)


def sfx_pistol():
    b = mix(noise(0.16, 0.8, attack=0.0005, decay=0.09, lp=0.55, seed=3),
            tone(0.12, 190, 60, "sine", 0.7, attack=0.001, decay=0.08),
            tone(0.08, 900, 300, "square", 0.10, attack=0.001, decay=0.05))
    return normalize(fade_edges(b, 0.001, 0.03), 0.95)


def sfx_shotgun():
    b = mix(noise(0.34, 0.9, attack=0.0005, decay=0.18, lp=0.35, seed=4),
            tone(0.28, 130, 45, "sine", 0.8, attack=0.001, decay=0.2),
            noise(0.08, 0.5, attack=0.001, decay=0.06, hp=0.7, seed=5))
    return normalize(fade_edges(b, 0.001, 0.05), 0.98)


def sfx_taser():
    # 电流：方波 + 快速振幅调制 + 噼啪噪声
    n = int(0.55 * SR)
    out = [0.0] * n
    phase = 0.0
    rng = random.Random(6)
    for i in range(n):
        t = i / SR
        f = 1900 - 700 * (i / n)
        phase += TAU * f / SR
        buzz = 1.0 if math.sin(phase) >= 0 else -1.0
        am = 0.5 + 0.5 * math.sin(TAU * 47 * t)
        crackle = rng.uniform(-1, 1) * (0.6 if (i % 900) < 60 else 0.08)
        env = math.exp(-t * 3.0) * min(1.0, t / 0.01)
        out[i] = (buzz * 0.28 * am + crackle * 0.4) * env
    return normalize(fade_edges(out, 0.002, 0.05), 0.9)


def sfx_throw():
    b = mix(noise(0.26, 0.5, attack=0.01, decay=0.2, lp=0.2, hp=0.35, seed=7),
            tone(0.26, 700, 180, "tri", 0.12, attack=0.01, decay=0.2))
    return normalize(fade_edges(b, 0.005, 0.05), 0.8)


def sfx_swing():
    b = mix(noise(0.20, 0.55, attack=0.01, decay=0.14, lp=0.25, hp=0.5, seed=8),
            tone(0.20, 420, 160, "tri", 0.18, attack=0.01, decay=0.15))
    return normalize(fade_edges(b, 0.005, 0.05), 0.8)


def sfx_defib():
    b = [0.0] * int(0.62 * SR)
    add_at(b, mix(tone(0.10, 520, 520, "square", 0.25, attack=0.001, decay=0.08),
                  noise(0.10, 0.35, attack=0.001, decay=0.07, hp=0.4, seed=9)), 0.0)
    add_at(b, mix(tone(0.12, 380, 300, "square", 0.3, attack=0.001, decay=0.1),
                  noise(0.12, 0.4, attack=0.001, decay=0.09, hp=0.4, seed=10)), 0.16)
    add_at(b, mix(tone(0.28, 220, 110, "saw", 0.18, attack=0.01, decay=0.25),
                  noise(0.28, 0.15, attack=0.01, decay=0.2, hp=0.3, seed=11)), 0.03)
    return normalize(fade_edges(b, 0.002, 0.05), 0.9)


def sfx_flame():
    b = noise(0.7, 0.5, attack=0.05, decay=0.6, lp=0.16, sustain=0.35, seed=12)
    # 低频轰鸣 + 随机爆裂
    b = mix(b, tone(0.7, 70, 90, "sine", 0.15, attack=0.05, decay=0.6, sustain=0.4))
    return normalize(fade_edges(b, 0.02, 0.1), 0.85)


def sfx_impact():
    b = mix(noise(0.09, 0.6, attack=0.0005, decay=0.05, lp=0.5, seed=13),
            tone(0.09, 260, 90, "sine", 0.5, attack=0.001, decay=0.06))
    return normalize(fade_edges(b, 0.001, 0.02), 0.85)


def sfx_hit_flesh():
    b = mix(noise(0.16, 0.55, attack=0.001, decay=0.1, lp=0.3, seed=14),
            tone(0.16, 140, 70, "sine", 0.6, attack=0.001, decay=0.12))
    return normalize(fade_edges(b, 0.001, 0.03), 0.85)


def sfx_enemy_die():
    b = mix(tone(0.6, 220, 70, "saw", 0.4, attack=0.005, decay=0.5, sustain=0.1),
            noise(0.6, 0.35, attack=0.005, decay=0.45, lp=0.25, seed=15))
    return normalize(fade_edges(b, 0.005, 0.08), 0.9)


def sfx_player_hurt():
    b = mix(tone(0.28, 300, 120, "saw", 0.5, attack=0.003, decay=0.2, sustain=0.1),
            noise(0.28, 0.25, attack=0.003, decay=0.2, lp=0.4, seed=16))
    return normalize(fade_edges(b, 0.003, 0.05), 0.9)


def sfx_player_die():
    b = mix(tone(1.4, 240, 45, "saw", 0.45, attack=0.01, decay=1.2, sustain=0.05),
            tone(1.4, 180, 35, "sine", 0.4, attack=0.01, decay=1.2, sustain=0.05),
            noise(0.9, 0.2, attack=0.02, decay=0.7, lp=0.2, seed=17))
    return normalize(fade_edges(b, 0.01, 0.15), 0.92)


def sfx_pickup():
    b = mix(tone(0.06, 880, 880, "tri", 0.4, attack=0.001, decay=0.05),
            add_at([0.0] * int(0.15 * SR),
                   tone(0.08, 1320, 1320, "tri", 0.4, attack=0.001, decay=0.06), 0.06))
    return normalize(fade_edges(b, 0.002, 0.02), 0.75)


def sfx_pickup_weapon():
    b = mix(noise(0.18, 0.4, attack=0.001, decay=0.12, hp=0.55, seed=18),
            tone(0.16, 700, 500, "square", 0.2, attack=0.001, decay=0.12),
            add_at([0.0] * int(0.25 * SR),
                   noise(0.1, 0.3, attack=0.001, decay=0.07, hp=0.6, seed=19), 0.10))
    return normalize(fade_edges(b, 0.002, 0.04), 0.85)


def sfx_heal():
    b = mix(tone(0.5, 523.25, 523.25, "sine", 0.3, attack=0.02, decay=0.4),
            tone(0.5, 659.25, 659.25, "sine", 0.25, attack=0.04, decay=0.4),
            tone(0.5, 783.99, 783.99, "sine", 0.2, attack=0.06, decay=0.4))
    return normalize(fade_edges(b, 0.01, 0.1), 0.75)


def sfx_keycard():
    b = mix(tone(0.08, 1046, 1046, "square", 0.22, attack=0.001, decay=0.06),
            add_at([0.0] * int(0.3 * SR),
                   tone(0.08, 1568, 1568, "square", 0.22, attack=0.001, decay=0.06), 0.1))
    return normalize(fade_edges(b, 0.002, 0.03), 0.7)


def sfx_door():
    b = mix(noise(0.75, 0.35, attack=0.02, decay=0.6, lp=0.12, sustain=0.2, seed=20),
            tone(0.7, 180, 120, "saw", 0.15, attack=0.05, decay=0.5, sustain=0.2))
    b = mix(b, add_at([0.0] * int(0.8 * SR),
                      mix(noise(0.1, 0.4, attack=0.001, decay=0.07, lp=0.4, seed=21),
                          tone(0.1, 120, 60, "sine", 0.4, attack=0.001, decay=0.07)), 0.65))
    return normalize(fade_edges(b, 0.01, 0.08), 0.85)


def sfx_locker():
    b = mix(noise(0.16, 0.5, attack=0.001, decay=0.1, hp=0.5, seed=22),
            tone(0.2, 320, 180, "square", 0.2, attack=0.001, decay=0.15))
    return normalize(fade_edges(b, 0.001, 0.04), 0.8)


def sfx_hack_start():
    b = mix(tone(0.1, 660, 880, "square", 0.2, attack=0.002, decay=0.08),
            add_at([0.0] * int(0.4 * SR),
                   tone(0.1, 990, 1320, "square", 0.2, attack=0.002, decay=0.08), 0.12))
    return normalize(fade_edges(b, 0.002, 0.03), 0.7)


def sfx_hack_step():
    b = mix(tone(0.035, 1600, 1600, "square", 0.18, attack=0.001, decay=0.025),
            noise(0.02, 0.15, attack=0.001, decay=0.015, hp=0.7, seed=23))
    return normalize(fade_edges(b, 0.001, 0.01), 0.65)


def sfx_hack_success():
    notes = [523.25, 659.25, 783.99, 1046.5]
    b = [0.0] * int(0.9 * SR)
    for i, f in enumerate(notes):
        add_at(b, tone(0.24, f, f, "tri", 0.32, attack=0.002, decay=0.2), i * 0.12)
    return normalize(fade_edges(b, 0.003, 0.1), 0.8)


def sfx_hack_fail():
    b = mix(tone(0.55, 160, 90, "square", 0.3, attack=0.002, decay=0.45, sustain=0.2),
            tone(0.55, 168, 95, "square", 0.25, attack=0.002, decay=0.45, sustain=0.2),
            noise(0.3, 0.15, attack=0.002, decay=0.25, lp=0.3, seed=24))
    return normalize(fade_edges(b, 0.003, 0.06), 0.85)


def sfx_alarm():
    # 两音警笛循环（2s）
    n = int(2.0 * SR)
    out = [0.0] * n
    for i in range(n):
        t = i / SR
        f = 880 if (int(t / 0.25) % 2 == 0) else 660
        phase = TAU * f * t
        s = 1.0 if math.sin(phase) >= 0 else -1.0
        # 每个 0.25s 重新起音，模拟 klaxon
        sub = (t % 0.25) / 0.25
        env = 0.55 + 0.45 * math.sin(math.pi * sub)
        out[i] = s * 0.18 * env
    out = lowpass(out, 2400)
    return normalize(fade_edges(out, 0.01, 0.05), 0.8)


def sfx_sentry_alert():
    b = [0.0] * int(0.6 * SR)
    for k in range(2):
        add_at(b, mix(tone(0.12, 1200, 1200, "square", 0.25, attack=0.002, decay=0.1),
                      noise(0.12, 0.2, attack=0.002, decay=0.1, hp=0.5, seed=25 + k)), k * 0.2)
    return normalize(fade_edges(b, 0.002, 0.05), 0.8)


def sfx_elevator():
    b = mix(tone(0.5, 880, 880, "sine", 0.3, attack=0.005, decay=0.4),
            tone(0.5, 1320, 1320, "sine", 0.2, attack=0.01, decay=0.4),
            noise(0.5, 0.25, attack=0.02, decay=0.35, lp=0.12, sustain=0.3, seed=26))
    return normalize(fade_edges(b, 0.005, 0.1), 0.8)


def sfx_organ_upgrade():
    notes = [392.0, 523.25, 659.25, 783.99, 1046.5]
    b = [0.0] * int(1.2 * SR)
    for i, f in enumerate(notes):
        add_at(b, tone(0.5, f, f, "sine", 0.28, attack=0.01, decay=0.4), i * 0.11)
        add_at(b, tone(0.5, f * 1.005, f * 1.005, "sine", 0.15, attack=0.01, decay=0.4), i * 0.11)
    return normalize(fade_edges(b, 0.005, 0.15), 0.85)


def sfx_floor_win():
    # 上行大三和弦号角
    seq = [(392.0, 0.0), (523.25, 0.16), (659.25, 0.32), (783.99, 0.48), (783.99, 0.72)]
    b = [0.0] * int(2.2 * SR)
    for f, t in seq:
        add_at(b, mix(tone(0.5, f, f, "tri", 0.3, attack=0.005, decay=0.4),
                      tone(f / 2, 0.5, f / 2, "sine", 0.2, attack=0.005, decay=0.4)), t)
    add_at(b, mix(tone(0.9, 1046.5, 1046.5, "tri", 0.25, attack=0.005, decay=0.8),
                  tone(0.9, 1318.5, 1318.5, "sine", 0.15, attack=0.01, decay=0.8)), 0.75)
    return normalize(fade_edges(b, 0.005, 0.2), 0.9)


def sfx_floor_lose():
    seq = [392.0, 349.23, 293.66, 220.0]
    b = [0.0] * int(2.0 * SR)
    for i, f in enumerate(seq):
        add_at(b, mix(tone(0.6, f, f * 0.98, "tri", 0.3, attack=0.01, decay=0.5),
                      tone(0.6, f * 0.5, f * 0.5, "sine", 0.25, attack=0.02, decay=0.5)), i * 0.38)
    return normalize(fade_edges(b, 0.01, 0.2), 0.9)


def sfx_boss_roar():
    # 低频咆哮：低音锯 + 噪声 + 颤音
    n = int(1.6 * SR)
    out = [0.0] * n
    phase = 0.0
    phase2 = 0.0
    rng = random.Random(27)
    for i in range(n):
        t = i / SR
        f = 70 + 25 * math.sin(TAU * 1.3 * t) - 30 * (i / n)
        phase += TAU * f / SR
        phase2 += TAU * (f * 1.5) / SR
        growl = (1.0 if math.sin(phase) >= 0 else -1.0) * 0.45
        growl += math.sin(phase2) * 0.25
        growl += rng.uniform(-1, 1) * 0.18
        env = math.exp(-t * 1.2) * min(1.0, t / 0.05)
        out[i] = growl * env
    out = lowpass(out, 900)
    return normalize(fade_edges(out, 0.01, 0.15), 0.95)


SFX_BUILDERS = {
    "sfx_ui_click": sfx_ui_click,
    "sfx_ui_confirm": sfx_ui_confirm,
    "sfx_ui_back": sfx_ui_back,
    "sfx_pistol": sfx_pistol,
    "sfx_shotgun": sfx_shotgun,
    "sfx_taser": sfx_taser,
    "sfx_throw": sfx_throw,
    "sfx_swing": sfx_swing,
    "sfx_defib": sfx_defib,
    "sfx_flame": sfx_flame,
    "sfx_impact": sfx_impact,
    "sfx_hit_flesh": sfx_hit_flesh,
    "sfx_enemy_die": sfx_enemy_die,
    "sfx_player_hurt": sfx_player_hurt,
    "sfx_player_die": sfx_player_die,
    "sfx_pickup": sfx_pickup,
    "sfx_pickup_weapon": sfx_pickup_weapon,
    "sfx_heal": sfx_heal,
    "sfx_keycard": sfx_keycard,
    "sfx_door": sfx_door,
    "sfx_locker": sfx_locker,
    "sfx_hack_start": sfx_hack_start,
    "sfx_hack_step": sfx_hack_step,
    "sfx_hack_success": sfx_hack_success,
    "sfx_hack_fail": sfx_hack_fail,
    "sfx_alarm": sfx_alarm,
    "sfx_sentry_alert": sfx_sentry_alert,
    "sfx_elevator": sfx_elevator,
    "sfx_organ_upgrade": sfx_organ_upgrade,
    "sfx_floor_win": sfx_floor_win,
    "sfx_floor_lose": sfx_floor_lose,
    "sfx_boss_roar": sfx_boss_roar,
}


# ---------------------------------------------------------------------------
# BGM：6 首简单可听的循环曲
# ---------------------------------------------------------------------------

CHORDS = {
    "Am": [57, 60, 64, 67],
    "F": [53, 57, 60, 65],
    "C": [48, 52, 55, 59],
    "G": [55, 59, 62, 65],
    "Em": [52, 55, 59, 62],
    "E": [52, 56, 59, 64],
    "Dm": [50, 53, 57, 60],
    "Bb": [46, 50, 53, 58],
    "Gm": [43, 46, 50, 55],
    "A": [45, 49, 52, 57],
}

TRACKS = {
    # name: bpm, bars, chords, style
    "mus_title": dict(bpm=76, bars=6, chords=["Am", "F", "C", "G", "Am", "F"], style="ambient"),
    "mus_floor": dict(bpm=100, bars=8, chords=["Am", "Am", "F", "G", "Em", "Em", "F", "E"], style="floor"),
    "mus_tense": dict(bpm=132, bars=8, chords=["Dm", "Dm", "Bb", "A", "Dm", "Dm", "Bb", "A"], style="tense"),
    "mus_boss": dict(bpm=150, bars=8, chords=["Dm", "Bb", "Gm", "A", "Dm", "Bb", "Gm", "A"], style="boss"),
    "mus_win": dict(bpm=118, bars=6, chords=["C", "F", "G", "C", "Am", "F"], style="win"),
    "mus_lose": dict(bpm=68, bars=6, chords=["Am", "F", "Dm", "E", "Am", "Am"], style="lose"),
}


def midi_freq(m):
    return 440.0 * (2.0 ** ((m - 69) / 12.0))


def _add_tone(buf, start, dur, freq, kind, vol, attack, decay, sustain, seed=1):
    i0 = int(start * SR)
    n = int(dur * SR)
    if n <= 0:
        return
    phase = 0.0
    for i in range(n):
        t = i / SR
        phase += TAU * freq / SR
        s = _wave(kind, phase)
        idx = i0 + i
        if 0 <= idx < len(buf):
            buf[idx] += s * _env(t, dur, attack, decay, sustain) * vol


def _add_kick(buf, start, vol=0.5):
    i0 = int(start * SR)
    n = int(0.18 * SR)
    phase = 0.0
    for i in range(n):
        t = i / SR
        f = 120 * math.exp(-t * 16) + 42
        phase += TAU * f / SR
        env = math.exp(-t * 15)
        idx = i0 + i
        if 0 <= idx < len(buf):
            buf[idx] += math.sin(phase) * env * vol


def _add_hat(buf, start, vol=0.10, decay=0.05):
    i0 = int(start * SR)
    n = int(0.07 * SR)
    rng = random.Random(int(start * 1000) + 7)
    for i in range(n):
        t = i / SR
        env = math.exp(-t / max(decay, 1e-4))
        idx = i0 + i
        if 0 <= idx < len(buf):
            buf[idx] += rng.uniform(-1, 1) * env * vol


def build_music(name, bpm, bars, chords, style):
    spb = 60.0 / bpm
    bar = 4.0 * spb
    total = bars * bar
    n = int(total * SR)
    buf = [0.0] * n

    # 风格参数：pad / arp / bass / drums
    if style == "ambient":
        arp_div, arp_kind, arp_vol = 2, "sine", 0.085
        bass_div, bass_kind, bass_vol = 2, "sine", 0.14
        drums, hat_div = "soft", 0
        pad_vol = 0.055
    elif style == "floor":
        arp_div, arp_kind, arp_vol = 2, "tri", 0.09
        bass_div, bass_kind, bass_vol = 2, "tri", 0.16
        drums, hat_div = "normal", 2
        pad_vol = 0.05
    elif style == "tense":
        arp_div, arp_kind, arp_vol = 4, "square", 0.05
        bass_div, bass_kind, bass_vol = 4, "saw", 0.13
        drums, hat_div = "tense", 2
        pad_vol = 0.042
    elif style == "boss":
        arp_div, arp_kind, arp_vol = 4, "saw", 0.05
        bass_div, bass_kind, bass_vol = 4, "saw", 0.17
        drums, hat_div = "boss", 2
        pad_vol = 0.05
    elif style == "win":
        arp_div, arp_kind, arp_vol = 2, "tri", 0.10
        bass_div, bass_kind, bass_vol = 1, "tri", 0.15
        drums, hat_div = "normal", 2
        pad_vol = 0.045
    else:  # lose
        arp_div, arp_kind, arp_vol = 1, "sine", 0.09
        bass_div, bass_kind, bass_vol = 1, "sine", 0.13
        drums, hat_div = "none", 0
        pad_vol = 0.06

    for b in range(bars):
        tones = CHORDS[chords[b % len(chords)]]
        t0 = b * bar
        root = tones[0]
        # --- pad：和弦长音 ---
        for j, m in enumerate(tones[:3]):
            _add_tone(buf, t0, bar * 0.98, midi_freq(m), "sine", pad_vol * (1.0 - 0.15 * j),
                      0.4, 0.0, 1.0, seed=100 + j)
        # --- arp：8/16 分音符琶音 ---
        arp_notes = [tones[1], tones[2], tones[3], tones[1] + 12,
                     tones[2] + 12, tones[3], tones[2], tones[1]]
        steps = int(4 * arp_div)
        step_dur = bar / steps
        for s in range(steps):
            m = arp_notes[s % len(arp_notes)]
            if style in ("tense", "boss") and s % 3 == 2:
                m += 1  # 半音紧张感
            _add_tone(buf, t0 + s * step_dur, step_dur * 0.92, midi_freq(m), arp_kind,
                      arp_vol, 0.004, step_dur * 0.8, 0.0, seed=200 + s)
        # --- bass ---
        bass_steps = int(4 * bass_div / 2)
        bass_step_dur = bar / max(1, bass_steps)
        for s in range(bass_steps):
            m = root - 12 + (7 if (s % 2 == 1 and style in ("floor", "win")) else 0)
            _add_tone(buf, t0 + s * bass_step_dur, bass_step_dur * 0.9, midi_freq(m),
                      bass_kind, bass_vol, 0.005, bass_step_dur * 0.7, 0.0, seed=300 + s)
        # --- drums ---
        if drums == "soft":
            for beat in (0, 2):
                _add_kick(buf, t0 + beat * spb, 0.16)
        elif drums == "normal":
            for beat in range(4):
                _add_kick(buf, t0 + beat * spb, 0.30 if beat % 2 == 0 else 0.22)
        elif drums == "tense":
            for beat in range(4):
                _add_kick(buf, t0 + beat * spb, 0.34)
        elif drums == "boss":
            for beat in range(4):
                _add_kick(buf, t0 + beat * spb, 0.42)
            # 军鼓：2、4 拍
            for beat in (1, 3):
                _add_hat(buf, t0 + beat * spb, 0.16, 0.09)
        if hat_div:
            hat_steps = int(4 * hat_div)
            for s in range(hat_steps):
                _add_hat(buf, t0 + s * (bar / hat_steps), 0.07, 0.04)

    # 轻微低通塑造“医院走廊”音色
    lowpass(buf, 5200 if style in ("tense", "boss") else 4200)
    fade_edges(buf, fade_in=0.03, fade_out=0.05)
    normalize(buf, 0.72)
    return buf


MUSIC_BUILDERS = {name: (lambda cfg: (lambda: build_music(name, **cfg)))(cfg)
                  for name, cfg in TRACKS.items()}


# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------

def generate(only=None, out_dir=DEFAULT_OUT, verbose=True):
    os.makedirs(out_dir, exist_ok=True)
    names = list(SFX_BUILDERS.keys()) + list(MUSIC_BUILDERS.keys())
    if only:
        wanted = set(only)
        names = [n for n in names if n in wanted or n.replace("_", "") in wanted]
    total_bytes = 0
    t_start = time.time()
    for name in names:
        if name in SFX_BUILDERS:
            buf = SFX_BUILDERS[name]()
            dur = len(buf) / SR
        else:
            buf = MUSIC_BUILDERS[name]()
            dur = len(buf) / SR
        path = os.path.join(out_dir, name + ".wav")
        samples = write_wav(path, buf)
        size = os.path.getsize(path)
        total_bytes += size
        if verbose:
            print("  %-22s %6.2fs  %8.1f KB" % (name + ".wav", dur, size / 1024.0))
    if verbose:
        print("[audio] %d files, %.2f MB, %.1fs -> %s"
              % (len(names), total_bytes / 1024.0 / 1024.0, time.time() - t_start, out_dir))
    return names, total_bytes


def verify(out_dir=DEFAULT_OUT):
    """检查 WAV：时长 / 峰值 / RMS，过短/静音/削波都算失败。"""
    import glob
    problems = 0
    total = 0
    total_bytes = 0
    for path in sorted(glob.glob(os.path.join(out_dir, "*.wav"))):
        total += 1
        total_bytes += os.path.getsize(path)
        with wave.open(path, "rb") as w:
            ch = w.getnchannels()
            sw = w.getsampwidth()
            rate = w.getframerate()
            n = w.getnframes()
            raw = w.readframes(n)
        if ch != 1 or sw != 2 or rate != SR:
            print("  BAD   %-26s ch=%d sw=%d rate=%d" % (os.path.basename(path), ch, sw, rate))
            problems += 1
            continue
        peak = 0
        acc = 0
        for i in range(0, len(raw) - 1, 2):
            v = struct.unpack_from("<h", raw, i)[0]
            av = abs(v)
            if av > peak:
                peak = av
            acc += v * v
        rms = math.sqrt(acc / max(1, n)) / 32768.0
        peakf = peak / 32768.0
        dur = n / rate
        db_peak = 20 * math.log10(max(peakf, 1e-9))
        db_rms = 20 * math.log10(max(rms, 1e-9))
        status = "OK  "
        if dur < 0.02 or db_peak < -45:
            status = "SILENT"
            problems += 1
        elif peakf >= 0.999:
            status = "CLIP"
            problems += 1
        print("  %s  %-26s %6.2fs  peak %5.1f dB  rms %5.1f dB" %
              (status, os.path.basename(path), dur, db_peak, db_rms))
    print("[audio] verify: %d files, %.2f MB, %d problem(s)"
          % (total, total_bytes / 1024.0 / 1024.0, problems))
    return problems


def main(argv):
    ap = argparse.ArgumentParser(description="疯狂诊所程序化 WAV 生成器")
    ap.add_argument("--out", default=DEFAULT_OUT, help="输出目录（默认 app/src/main/res/raw）")
    ap.add_argument("--only", nargs="*", default=None, help="只生成指定文件（不含 .wav）")
    ap.add_argument("--list", action="store_true", help="列出所有文件名后退出")
    ap.add_argument("--verify", action="store_true", help="只校验已有 WAV 后退出")
    args = ap.parse_args(argv)
    all_names = list(SFX_BUILDERS.keys()) + list(MUSIC_BUILDERS.keys())
    if args.list:
        for n in all_names:
            print(n)
        return 0
    if args.verify:
        return 1 if verify(args.out) > 0 else 0
    if args.only:
        unknown = [n for n in args.only if n not in all_names]
        if unknown:
            print("[audio] unknown: %s" % ", ".join(unknown), file=sys.stderr)
            print("[audio] available: %s" % ", ".join(all_names), file=sys.stderr)
            return 2
    _, total = generate(args.only, args.out)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
