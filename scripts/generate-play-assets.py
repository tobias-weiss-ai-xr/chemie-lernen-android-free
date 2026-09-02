#!/usr/bin/env python3
"""Generate Google-Play brand assets for Chemie Lernen (Free).

Outputs:
  play-store/icons/icon-512-square.png   (512x512, full-bleed app icon)
  play-store/icons/icon-512.png          (512x512, rounded copy for docs)
  play-store/feature-graphic.png         (1024x500, Play feature graphic)

The artwork reproduces the app's launcher vector (ic_launcher_foreground):
white Erlenmeyer flask with green liquid on green background.
"""
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ICONS = os.path.join(ROOT, "play-store", "icons")
FEATURE = os.path.join(ROOT, "play-store", "feature-graphic.png")

GREEN = (46, 125, 50)        # #2E7D32 (chemie_green)
GREEN_DARK = (27, 94, 32)    # #1B5E20 (chemie_green_dark)
GREEN_LIGHT = (76, 175, 80)  # #4CAF50 (chemie_green_light)
WHITE = (255, 255, 255)

FONT_BOLD = "C:/Windows/Fonts/arialbd.ttf"
FONT_REG = "C:/Windows/Fonts/arial.ttf"


def flask_polygons(scale, cx, cy):
    """Return (outline_pts, outline_corner_circles, liquid_pts) in scaled coords.

    Geometry from app/src/main/res/drawable/ic_launcher_foreground.xml
    (108x108 viewport): flask spans x=29..79, y=20..95.
    Rounded bottom corners are drawn as small circles (r=6 in viewport units)
    centered at the corner points.
    """
    def P(px, py):
        return (cx + (px - 54.0) * scale, cy + (py - 57.5) * scale)

    # white flask outline (straight corners)
    outline = [
        P(47, 20), P(61, 20), P(61, 48), P(79, 86),
        P(79, 95), P(29, 95), P(29, 86), P(47, 48),
    ]
    # rounding circles at the two bottom corners
    corner_circles = [P(79, 95), P(29, 95)]
    corner_r = 6.0 * scale
    # green liquid (trapezoid inside the flask)
    liquid = [
        P(36.6, 70), P(71.4, 70), P(79, 86),
        P(79, 95), P(29, 95), P(29, 86),
    ]
    return outline, corner_circles, corner_r, liquid


def draw_flask(draw, scale, cx, cy):
    outline, corners, r, liquid = flask_polygons(scale, cx, cy)
    draw.polygon(outline, fill=WHITE)
    for C in corners:
        draw.ellipse([C[0] - r, C[1] - r, C[0] + r, C[1] + r], fill=WHITE)
    draw.polygon(liquid, fill=GREEN_LIGHT)


def make_icon():
    os.makedirs(ICONS, exist_ok=True)
    size = 512
    # Flask (75 units tall in 108 viewport) scaled to ~62% of the square.
    scale = size * 0.62 / 75.0          # ~4.23
    cx = size / 2
    cy = size / 2 - scale * 4           # tiny optical lift
    im = Image.new("RGB", (size, size), GREEN)
    draw = ImageDraw.Draw(im)
    draw_flask(draw, scale, cx, cy)
    im.save(os.path.join(ICONS, "icon-512-square.png"))
    # rounded copy (for docs / illustration)
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle([0, 0, size - 1, size - 1], radius=96, fill=255)
    rounded = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    rounded.paste(im, (0, 0), mask)
    rounded.save(os.path.join(ICONS, "icon-512.png"))
    print("Icon 512x512:", os.path.join(ICONS, "icon-512-square.png"))


def make_feature_graphic():
    W, H = 1024, 500
    im = Image.new("RGB", (W, H))
    # vertical gradient dark->green
    top = Image.new("RGB", (1, H))
    for y in range(H):
        t = y / (H - 1)
        r = int(GREEN_DARK[0] + (GREEN[0] - GREEN_DARK[0]) * t)
        g = int(GREEN_DARK[1] + (GREEN[1] - GREEN_DARK[1]) * t)
        b = int(GREEN_DARK[2] + (GREEN[2] - GREEN_DARK[2]) * t)
        top.putpixel((0, y), (r, g, b))
    im = top.resize((W, H))
    draw = ImageDraw.Draw(im)

    # flask, left half
    scale = H * 0.72 / 75.0
    draw_flask(draw, scale, W * 0.30, H / 2 + scale * 2)

    # text right side (inside Play safe-area: centered rect ~x 256..768, y ~62..437)
    title_f = ImageFont.truetype(FONT_BOLD, 60)
    tag_f = ImageFont.truetype(FONT_REG, 36)
    url_f = ImageFont.truetype(FONT_BOLD, 34)
    tx = 270
    draw.text((tx, 140), "Chemie Lernen", font=title_f, fill=WHITE)
    draw.text((tx, 258), "Interaktive Chemie lernen", font=tag_f, fill=(220, 235, 215))
    draw.text((tx, 324), "kostenlos · ohne Tracking", font=tag_f, fill=(220, 235, 215))
    draw.text((tx, 402), "chemie-lernen.org", font=url_f, fill=(255, 255, 255))
    im.save(FEATURE)
    print("Feature graphic 1024x500:", FEATURE)


if __name__ == "__main__":
    make_icon()
    make_feature_graphic()
