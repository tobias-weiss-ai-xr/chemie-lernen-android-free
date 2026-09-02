#!/usr/bin/env python3
"""Capture Google-Play-Screenshots for Chemie Lernen (Free) on a connected device.

Usage:  python scripts/capture-screenshots.py [device-serial]
Resolves tap targets via uiautomator text lookup -> resolution independent.
Output: play-store/screenshots/phone/screenshot_XX_*.png (native 1008x2244)
"""
import os
import re
import subprocess
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "play-store", "screenshots", "phone")
PKG = "org.chemie_lernen_org.free"
ACTIVITY = "org.chemie_lernen_org.app.MainActivity"

devices = subprocess.run(
    ["adb", "devices"], capture_output=True, text=True
).stdout.strip().splitlines()
serial = sys.argv[1] if len(sys.argv) > 1 else (devices[1].split("\t")[0] if len(devices) > 1 else None)
if not serial:
    print("Kein Gerät gefunden")
    sys.exit(1)
print(f"Gerät: {serial}")
ADB = ["adb", "-s", serial]


def sh(*args, raw=False):
    r = subprocess.run(ADB + list(args), capture_output=True)
    if r.returncode != 0:
        raise SystemExit(f"adb {' '.join(args)} failed: {r.stderr}")
    return r.stdout


def dump_ui():
    sh("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    return sh("shell", "cat", "/sdcard/ui.xml").decode("utf-8", "replace")


def find_center(xml, text):
    esc = text.replace("&", "&amp;").replace('"', "&quot;").replace("<", "&lt;").replace(">", "&gt;")
    pat = re.compile(
        r'text="' + re.escape(esc) + r'"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    )
    m = pat.search(xml)
    if not m:
        return None
    x1, y1, x2, y2 = (int(g) for g in m.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2


def tap_text(text, timeout=15, wait_after=2.0):
    """Tap the center of the first node whose text == text. Retries until found."""
    for _ in range(timeout):
        c = find_center(dump_ui(), text)
        if c:
            sh("shell", "input", "tap", str(c[0]), str(c[1]))
            time.sleep(wait_after)
            return True
        time.sleep(1)
    print(f"  ⚠️  Text nicht gefunden: {text!r}")
    return False


def back(wait=2.0):
    sh("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(wait)


def shot(name):
    data = sh("exec-out", "screencap", "-p", raw=True)
    with open(os.path.join(OUT, name), "wb") as f:
        f.write(data)
    print(f"  📸 {name}")


def launch():
    sh("shell", "am", "force-stop", PKG)
    time.sleep(1)
    sh("shell", "am", "start", "-n", f"{PKG}/{ACTIVITY}")
    time.sleep(6)


def main():
    os.makedirs(OUT, exist_ok=True)
    print("== Starte App ==")
    launch()

    print("[1/8] Home")
    shot("screenshot_01_home.png")

    print("[2/8] Themenbereiche")
    tap_text("Themen")
    shot("screenshot_02_themen.png")

    print("[3/8] Rechner")
    tap_text("Rechner")
    shot("screenshot_03_rechner.png")

    print("[4/8] Lernvideos")
    tap_text("Videos")
    shot("screenshot_04_videos.png")

    print("[5/8] Mehr (Settings)")
    tap_text("Mehr")
    shot("screenshot_05_mehr.png")

    print("[6/8] Wissensnetz (WebView)")
    tap_text("Home", wait_after=1.0)
    time.sleep(1)
    tap_text("Wissensnetz")
    time.sleep(8)  # WebView load
    shot("screenshot_06_wissensnetz.png")

    print("[7/8] Molare-Masse-Rechner (WebView)")
    back()
    tap_text("Rechner", wait_after=1.0)
    time.sleep(1)
    tap_text("Molare Masse")
    time.sleep(8)
    shot("screenshot_07_molare_masse.png")

    print("[8/8] Themenbereich: Einführung in die Chemie (WebView)")
    back()
    tap_text("Home", wait_after=1.0)
    time.sleep(1)
    tap_text("Themenbereiche", wait_after=1.5)
    time.sleep(1)
    tap_text("Einführung in die Chemie")
    time.sleep(8)
    shot("screenshot_08_einfuehrung_chemie.png")

    print()
    print(f"Fertig: {OUT}")


if __name__ == "__main__":
    main()
