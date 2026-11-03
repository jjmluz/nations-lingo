#!/usr/bin/env python3
"""Synthesizes the game's sound effects into res/raw as 16-bit mono WAV files."""
import math
import struct
import wave
from pathlib import Path

RATE = 22050
OUT_DIR = Path(__file__).resolve().parent.parent / "app/src/main/res/raw"


def note(frequency, seconds, volume=0.6, square=False):
    count = int(RATE * seconds)
    samples = []
    for i in range(count):
        t = i / RATE
        wave_value = math.sin(2 * math.pi * frequency * t)
        if square:
            wave_value = 0.5 * (1 if wave_value >= 0 else -1)
        else:
            wave_value = 0.8 * wave_value + 0.2 * math.sin(4 * math.pi * frequency * t)
        # Short attack, then an exponential decay, ending at zero so nothing clicks.
        envelope = min(1, i / (RATE * 0.004)) * math.exp(-3 * i / count) * (1 - i / count)
        samples.append(volume * wave_value * envelope)
    return samples


def melody(*notes):
    return [sample for part in notes for sample in part]


SOUNDS = {
    "move": note(520, 0.05, 0.4),
    "start": melody(note(523, 0.08), note(659, 0.08), note(784, 0.16)),
    "correct": melody(note(660, 0.08), note(990, 0.16)),
    "wrong": melody(note(196, 0.11, 0.45, square=True), note(147, 0.2, 0.45, square=True)),
    "tick": note(1000, 0.04, 0.35),
    "game_over": melody(note(523, 0.17), note(440, 0.17), note(349, 0.17), note(262, 0.4)),
    "victory": melody(note(523, 0.1), note(659, 0.1), note(784, 0.1), note(1047, 0.45)),
}

OUT_DIR.mkdir(exist_ok=True)
for name, samples in SOUNDS.items():
    with wave.open(str(OUT_DIR / f"{name}.wav"), "wb") as file:
        file.setnchannels(1)
        file.setsampwidth(2)
        file.setframerate(RATE)
        file.writeframes(b"".join(struct.pack("<h", int(s * 32767)) for s in samples))
    print(name, f"{len(samples) / RATE:.2f}s")
