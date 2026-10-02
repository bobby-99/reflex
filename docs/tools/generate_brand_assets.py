import os, math
from PIL import Image, ImageDraw, ImageFilter

def create_vector_drawables():
    # 1. ic_launcher_background.xml
    bg_xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="radial"
                android:centerX="54"
                android:centerY="54"
                android:gradientRadius="64"
                android:startColor="#23150D"
                android:endColor="#080503" />
        </aapt:attr>
    </path>
</vector>
"""
    with open("app/src/main/res/drawable/ic_launcher_background.xml", "w", encoding="utf-8") as f:
        f.write(bg_xml)

    # 2. ic_launcher_foreground.xml
    fg_xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="0.398"
        android:scaleY="0.398"
        android:translateX="14.2"
        android:translateY="14.2">
        <group>
            <clip-path android:pathData="M100,40 A60,60 0 1,0 100,160 A60,60 0 1,0 100,40 Z" />
            <path
                android:fillColor="#4A201C"
                android:pathData="M40,160 L40,105 C70,90 90,84 115,84 C138,84 150,90 160,96 L160,160 Z" />
            <path
                android:pathData="M40,160 L40,108 C65,88 85,92 100,102 C115,112 135,120 160,110 L160,160 Z">
                <aapt:attr name="android:fillColor">
                    <gradient
                        android:type="linear"
                        android:startX="100"
                        android:startY="90"
                        android:endX="100"
                        android:endY="160"
                        android:startColor="#EE9C69"
                        android:endColor="#A85E37" />
                </aapt:attr>
            </path>
        </group>
        <path
            android:pathData="M167.07,68.73 A74,74 0 1,1 137,35.92"
            android:strokeWidth="14"
            android:strokeLineCap="round"
            android:strokeLineJoin="round">
            <aapt:attr name="android:strokeColor">
                <gradient
                    android:type="linear"
                    android:startX="40"
                    android:startY="40"
                    android:endX="160"
                    android:endY="160"
                    android:startColor="#F5B084"
                    android:endColor="#B5714F" />
            </aapt:attr>
        </path>
        <path
            android:fillColor="#F2A16D"
            android:pathData="M154.1,40.5 A9,9 0 1,0 154.1,58.5 A9,9 0 1,0 154.1,40.5 Z" />
    </group>
</vector>
"""
    with open("app/src/main/res/drawable/ic_launcher_foreground.xml", "w", encoding="utf-8") as f:
        f.write(fg_xml)

    # 3. ic_launcher_monochrome.xml
    mono_xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="0.398"
        android:scaleY="0.398"
        android:translateX="14.2"
        android:translateY="14.2">
        <group>
            <clip-path android:pathData="M100,40 A60,60 0 1,0 100,160 A60,60 0 1,0 100,40 Z" />
            <path
                android:fillColor="#FFFFFF"
                android:pathData="M40,160 L40,108 C65,88 85,92 100,102 C115,112 135,120 160,110 L160,160 Z" />
        </group>
        <path
            android:pathData="M167.07,68.73 A74,74 0 1,1 137,35.92"
            android:strokeColor="#FFFFFF"
            android:strokeWidth="14"
            android:strokeLineCap="round"
            android:strokeLineJoin="round" />
        <path
            android:fillColor="#FFFFFF"
            android:pathData="M154.1,40.5 A9,9 0 1,0 154.1,58.5 A9,9 0 1,0 154.1,40.5 Z" />
    </group>
</vector>
"""
    with open("app/src/main/res/drawable/ic_launcher_monochrome.xml", "w", encoding="utf-8") as f:
        f.write(mono_xml)

    # 4. ic_splash.xml
    splash_xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="288dp"
    android:height="288dp"
    android:viewportWidth="288"
    android:viewportHeight="288">
    <group
        android:scaleX="1.157"
        android:scaleY="1.157"
        android:translateX="28.3"
        android:translateY="28.3">
        <group>
            <clip-path android:pathData="M100,40 A60,60 0 1,0 100,160 A60,60 0 1,0 100,40 Z" />
            <path
                android:fillColor="#4A201C"
                android:pathData="M40,160 L40,105 C70,90 90,84 115,84 C138,84 150,90 160,96 L160,160 Z" />
            <path
                android:pathData="M40,160 L40,108 C65,88 85,92 100,102 C115,112 135,120 160,110 L160,160 Z">
                <aapt:attr name="android:fillColor">
                    <gradient
                        android:type="linear"
                        android:startX="100"
                        android:startY="90"
                        android:endX="100"
                        android:endY="160"
                        android:startColor="#EE9C69"
                        android:endColor="#A85E37" />
                </aapt:attr>
            </path>
        </group>
        <path
            android:pathData="M167.07,68.73 A74,74 0 1,1 137,35.92"
            android:strokeWidth="14"
            android:strokeLineCap="round"
            android:strokeLineJoin="round">
            <aapt:attr name="android:strokeColor">
                <gradient
                    android:type="linear"
                    android:startX="40"
                    android:startY="40"
                    android:endX="160"
                    android:endY="160"
                    android:startColor="#F5B084"
                    android:endColor="#B5714F" />
            </aapt:attr>
        </path>
        <path
            android:fillColor="#F2A16D"
            android:pathData="M154.1,40.5 A9,9 0 1,0 154.1,58.5 A9,9 0 1,0 154.1,40.5 Z" />
    </group>
</vector>
"""
    with open("app/src/main/res/drawable/ic_splash.xml", "w", encoding="utf-8") as f:
        f.write(splash_xml)

    # 5. ic_stat_reflex.xml
    stat_xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <group
        android:scaleX="0.1205"
        android:scaleY="0.1205"
        android:translateX="-0.05"
        android:translateY="-0.05">
        <group>
            <clip-path android:pathData="M100,41.5 A58.5,58.5 0 1,0 100,158.5 A58.5,58.5 0 1,0 100,41.5 Z" />
            <path
                android:fillColor="#FFFFFF"
                android:pathData="M40,160 L40,108 C65,88 85,92 100,102 C115,112 135,120 160,110 L160,160 Z" />
        </group>
        <path
            android:pathData="M167.07,68.73 A74,74 0 1,1 137,35.92"
            android:strokeColor="#FFFFFF"
            android:strokeWidth="14"
            android:strokeLineCap="round"
            android:strokeLineJoin="round" />
        <path
            android:fillColor="#FFFFFF"
            android:pathData="M154.1,40.5 A9,9 0 1,0 154.1,58.5 A9,9 0 1,0 154.1,40.5 Z" />
    </group>
</vector>
"""
    with open("app/src/main/res/drawable/ic_stat_reflex.xml", "w", encoding="utf-8") as f:
        f.write(stat_xml)

    print("Vector drawables written successfully.")

def render_high_res_mark(target_px, scale_factor, bg_color=None, is_round_mask=False):
    ss = 4
    canvas_size = target_px * ss
    im = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    if bg_color is not None:
        draw.rectangle([0, 0, canvas_size, canvas_size], fill=bg_color + (255,))
        cx, cy = canvas_size / 2, canvas_size / 2
        max_r = canvas_size * 0.55
        for r_step in range(int(max_r), 0, -4):
            t = 1.0 - (r_step / max_r)
            glow_r = int(bg_color[0] + (35 - bg_color[0]) * (t**1.8))
            glow_g = int(bg_color[1] + (21 - bg_color[1]) * (t**1.8))
            glow_b = int(bg_color[2] + (13 - bg_color[2]) * (t**1.8))
            draw.ellipse([cx - r_step, cy - r_step, cx + r_step, cy + r_step], fill=(glow_r, glow_g, glow_b, 255))

    cx, cy = canvas_size / 2, canvas_size / 2
    u = (target_px * ss * scale_factor)

    def tx(x, y):
        return (cx + (x - 100.0) * u, cy + (y - 100.0) * u)

    clip_mask = Image.new("L", (canvas_size, canvas_size), 0)
    clip_draw = ImageDraw.Draw(clip_mask)
    r60 = 60.0 * u
    clip_draw.ellipse([cx - r60, cy - r60, cx + r60, cy + r60], fill=255)

    wave_layer = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    wave_draw = ImageDraw.Draw(wave_layer)

    back_poly = [
        tx(40, 160),
        tx(40, 105),
        tx(70, 90),
        tx(90, 84),
        tx(115, 84),
        tx(138, 84),
        tx(150, 90),
        tx(160, 96),
        tx(160, 160)
    ]
    wave_draw.polygon(back_poly, fill=(74, 32, 28, 255))

    front_poly = [
        tx(40, 160),
        tx(40, 108),
        tx(65, 88),
        tx(85, 92),
        tx(100, 102),
        tx(115, 112),
        tx(135, 120),
        tx(160, 110),
        tx(160, 160)
    ]
    f_mask = Image.new("L", (canvas_size, canvas_size), 0)
    f_draw = ImageDraw.Draw(f_mask)
    f_draw.polygon(front_poly, fill=255)

    top_y = cy - 10.0 * u
    bot_y = cy + 60.0 * u
    grad_layer = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    grad_draw = ImageDraw.Draw(grad_layer)
    for y_line in range(int(max(0, top_y)), int(min(canvas_size, bot_y + 1))):
        prog = (y_line - top_y) / max(1.0, (bot_y - top_y))
        prog = max(0.0, min(1.0, prog))
        c_r = int(238 + (168 - 238) * prog)
        c_g = int(156 + (94 - 156) * prog)
        c_b = int(105 + (55 - 105) * prog)
        grad_draw.line([(0, y_line), (canvas_size, y_line)], fill=(c_r, c_g, c_b, 255))

    wave_layer.paste(grad_layer, (0, 0), f_mask)
    im.paste(wave_layer, (0, 0), clip_mask)

    r74 = 74.0 * u
    w14 = max(1, int(14.0 * u))
    ring_layer = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    ring_draw = ImageDraw.Draw(ring_layer)
    ring_draw.arc([cx - r74, cy - r74, cx + r74, cy + r74], start=-24.3, end=284.3, fill=(242, 161, 109, 255), width=w14)

    cap_r = w14 / 2.0
    p1 = tx(167.07, 68.73)
    p2 = tx(137.0, 35.92)
    ring_draw.ellipse([p1[0] - cap_r, p1[1] - cap_r, p1[0] + cap_r, p1[1] + cap_r], fill=(242, 161, 109, 255))
    ring_draw.ellipse([p2[0] - cap_r, p2[1] - cap_r, p2[0] + cap_r, p2[1] + cap_r], fill=(242, 161, 109, 255))

    im.paste(ring_layer, (0, 0), ring_layer)

    dot_center = tx(154.1, 49.5)
    r9 = 9.0 * u
    draw.ellipse([dot_center[0] - r9, dot_center[1] - r9, dot_center[0] + r9, dot_center[1] + r9], fill=(242, 161, 109, 255))

    final_im = im.resize((target_px, target_px), Image.Resampling.LANCZOS)

    if is_round_mask:
        round_mask = Image.new("L", (target_px, target_px), 0)
        rm_draw = ImageDraw.Draw(round_mask)
        rm_draw.ellipse([0, 0, target_px, target_px], fill=255)
        out = Image.new("RGBA", (target_px, target_px), (0, 0, 0, 0))
        out.paste(final_im, (0, 0), round_mask)
        return out

    return final_im

if __name__ == "__main__":
    create_vector_drawables()

    scale_512 = (313.0 / 166.0) / 512.0
    ps_icon = render_high_res_mark(512, scale_512, bg_color=(8, 5, 3), is_round_mask=False)
    ps_icon.convert("RGB").save("store/ic_launcher_512.png", "PNG")
    print("store/ic_launcher_512.png saved.")

    densities = {
        "mipmap-mdpi": (48, 108),
        "mipmap-hdpi": (72, 162),
        "mipmap-xhdpi": (96, 216),
        "mipmap-xxhdpi": (144, 324),
        "mipmap-xxxhdpi": (192, 432)
    }

    scale_launcher = (66.0 / 166.0) / 108.0

    for folder, (icon_px, fg_px) in densities.items():
        dir_path = os.path.join("app/src/main/res", folder)
        os.makedirs(dir_path, exist_ok=True)

        sq = render_high_res_mark(icon_px, scale_launcher, bg_color=(8, 5, 3), is_round_mask=False)
        sq.save(os.path.join(dir_path, "ic_launcher.png"), "PNG")

        rd = render_high_res_mark(icon_px, scale_launcher, bg_color=(8, 5, 3), is_round_mask=True)
        rd.save(os.path.join(dir_path, "ic_launcher_round.png"), "PNG")

        fg = render_high_res_mark(fg_px, scale_launcher, bg_color=None, is_round_mask=False)
        fg.save(os.path.join(dir_path, "ic_launcher_foreground.png"), "PNG")
        print(f"Generated icons for {folder}")
