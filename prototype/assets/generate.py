import math, random
from pathlib import Path

W, H = 1920, 1080
OUT = Path(__file__).parent

def stars(n, seed, ymax=740, big=False):
    random.seed(seed)
    parts = []
    for _ in range(n):
        x = random.uniform(0, W)
        y = random.uniform(0, ymax)
        r = random.choice([0.55, 0.7, 0.9, 1.15, 1.5, 2.0])
        o = random.uniform(0.18, 0.9)
        cls = ' class="tw"' if big and r > 1.4 else ''
        parts.append(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{r}" fill="#eaf4ff" opacity="{o:.2f}"{cls}/>')
    return "\n".join(parts)

def ridge_points(seed, base, amp, step_min, step_max, ymax=H):
    random.seed(seed)
    pts = [(0, base + random.uniform(-amp, amp))]
    x = 0
    while x < W:
        x += random.randint(step_min, step_max)
        pts.append((min(x, W), base + random.uniform(-amp, amp)))
    return pts

def ridge_path(pts):
    return "M " + " L ".join(f"{x:.0f},{y:.0f}" for x, y in pts) + f" L {W},{H} L 0,{H} Z"

def ridge_line(pts):
    return "M " + " L ".join(f"{x:.0f},{y:.0f}" for x, y in pts)

# ---------------------------------------------------------------- aurora
def aurora():
    random.seed(11)
    curtains = []
    for layer, (c1, c2, c3, op) in enumerate([
        ("#2bf0c0", "#4f8dff", "#9b6bff", 0.34),
        ("#6bffb2", "#2bd8d0", "#5b8cff", 0.22),
    ]):
        paths = []
        for x in range(-40, W + 40, 17):
            base = 315 + 66 * math.sin(x / 300 + layer * 0.9) + 30 * math.sin(x / 105 + layer * 1.8) + layer * 38
            h = 135 + 145 * random.random()
            w = 4.5 + 5 * random.random()
            paths.append(
                f'<path d="M{x - w:.1f} {base:.1f} L{x + w:.1f} {base:.1f} '
                f'L{x + w * .22:.1f} {base - h:.1f} L{x - w * .22:.1f} {base - h:.1f} Z" '
                f'fill="url(#cur{layer})"/>'
            )
        curtains.append(
            f'<g filter="url(#soft)" opacity="{op}" style="mix-blend-mode:screen">\n'
            + "\n".join(paths) + "\n</g>"
        )
    stars_svg = stars(130, 3, 700, big=True)
    far = ridge_points(21, 700, 55, 70, 150)
    mid = ridge_points(22, 775, 65, 55, 120)
    near = ridge_points(23, 850, 55, 45, 100)
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1920 1080" preserveAspectRatio="xMidYMid slice">
<defs>
  <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="#02040a"/>
    <stop offset=".42" stop-color="#061321"/>
    <stop offset=".72" stop-color="#0a2230"/>
    <stop offset="1" stop-color="#040b12"/>
  </linearGradient>
  <radialGradient id="horizon" cx=".52" cy=".82" r=".62">
    <stop offset="0" stop-color="#1d7b78" stop-opacity=".30"/>
    <stop offset=".55" stop-color="#123d4c" stop-opacity=".14"/>
    <stop offset="1" stop-color="#000000" stop-opacity="0"/>
  </radialGradient>
  <linearGradient id="band" x1="0" y1="0" x2="1" y2="0">
    <stop offset="0" stop-color="#2bf0c0" stop-opacity="0"/>
    <stop offset=".28" stop-color="#2bf0c0" stop-opacity=".55"/>
    <stop offset=".55" stop-color="#4f8dff" stop-opacity=".45"/>
    <stop offset=".8" stop-color="#9b6bff" stop-opacity=".38"/>
    <stop offset="1" stop-color="#9b6bff" stop-opacity="0"/>
  </linearGradient>
  <linearGradient id="cur0" x1="0" y1="1" x2="0" y2="0">
    <stop offset="0" stop-color="#2bf0c0" stop-opacity=".95"/>
    <stop offset=".5" stop-color="#4f8dff" stop-opacity=".45"/>
    <stop offset="1" stop-color="#9b6bff" stop-opacity="0"/>
  </linearGradient>
  <linearGradient id="cur1" x1="0" y1="1" x2="0" y2="0">
    <stop offset="0" stop-color="#6bffb2" stop-opacity=".9"/>
    <stop offset=".5" stop-color="#2bd8d0" stop-opacity=".4"/>
    <stop offset="1" stop-color="#5b8cff" stop-opacity="0"/>
  </linearGradient>
  <linearGradient id="mist" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="#0b2530" stop-opacity="0"/>
    <stop offset=".55" stop-color="#0e2b36" stop-opacity=".55"/>
    <stop offset="1" stop-color="#040b12" stop-opacity=".9"/>
  </linearGradient>
  <linearGradient id="ridgeLight" x1="0" y1="0" x2="1" y2="0">
    <stop offset="0" stop-color="#2bf0c0" stop-opacity="0"/>
    <stop offset=".5" stop-color="#8ef5e0" stop-opacity=".55"/>
    <stop offset="1" stop-color="#9b6bff" stop-opacity="0"/>
  </linearGradient>
  <filter id="soft" x="-15%" y="-15%" width="130%" height="130%"><feGaussianBlur stdDeviation="7"/></filter>
  <filter id="blur40" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="38"/></filter>
  <filter id="glow" x="-200%" y="-200%" width="500%" height="500%"><feGaussianBlur stdDeviation="3"/></filter>
</defs>
<rect width="1920" height="1080" fill="url(#sky)"/>
<rect width="1920" height="1080" fill="url(#horizon)"/>
{stars_svg}
<path d="M-80 360 C 260 140, 520 520, 780 300 S 1220 90, 1480 330 S 1760 500, 2020 260 L 2020 520 C 1740 700, 1460 470, 1200 590 S 620 860, 320 640 S 40 560, -80 690 Z" fill="url(#band)" filter="url(#blur40)" opacity=".55"/>
{curtains[0]}
{curtains[1]}
<rect y="620" width="1920" height="460" fill="url(#mist)"/>
<path d="{ridge_path(far)}" fill="#081722"/>
<path d="{ridge_line(far)}" fill="none" stroke="url(#ridgeLight)" stroke-width="2.2" opacity=".28"/>
<path d="{ridge_path(mid)}" fill="#050d15"/>
<path d="{ridge_line(mid)}" fill="none" stroke="url(#ridgeLight)" stroke-width="1.8" opacity=".20"/>
<path d="{ridge_path(near)}" fill="#020508"/>
<path d="{ridge_line(near)}" fill="none" stroke="url(#ridgeLight)" stroke-width="1.4" opacity=".12"/>
<rect y="930" width="1920" height="150" fill="#020508"/>
<g opacity=".20" stroke="#4be3c8" stroke-width="1.1">
  <path d="M120 1010 H 540"/><path d="M760 1035 H 1220"/><path d="M1360 1005 H 1740"/>
  <path d="M260 1057 H 620"/><path d="M980 1064 H 1420"/>
</g>
</svg>'''

# ---------------------------------------------------------------- dusk
def dusk():
    random.seed(31)
    stars_svg = stars(55, 8, 380)
    far = ridge_points(41, 690, 45, 80, 170)
    mid = ridge_points(42, 770, 70, 55, 130)
    near = ridge_points(43, 860, 60, 45, 110)
    clouds = []
    for i in range(8):
        x = random.uniform(100, 1820)
        y = random.uniform(260, 650)
        rx = random.uniform(220, 520)
        ry = random.uniform(12, 34)
        o = random.uniform(.10, .26)
        clouds.append(f'<ellipse cx="{x:.0f}" cy="{y:.0f}" rx="{rx:.0f}" ry="{ry:.0f}" fill="#ffc39a" opacity="{o:.2f}"/>')
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1920 1080" preserveAspectRatio="xMidYMid slice">
<defs>
  <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="#100c24"/>
    <stop offset=".34" stop-color="#2c1b40"/>
    <stop offset=".58" stop-color="#633450"/>
    <stop offset=".74" stop-color="#b45c59"/>
    <stop offset=".84" stop-color="#e89467"/>
    <stop offset="1" stop-color="#f6bd88"/>
  </linearGradient>
  <radialGradient id="sun" cx=".5" cy=".5" r=".5">
    <stop offset="0" stop-color="#fff3cf" stop-opacity="1"/>
    <stop offset=".16" stop-color="#ffe1a8" stop-opacity=".95"/>
    <stop offset=".45" stop-color="#ffb57a" stop-opacity=".35"/>
    <stop offset="1" stop-color="#ff9d72" stop-opacity="0"/>
  </radialGradient>
  <linearGradient id="mist" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="#f7b98e" stop-opacity="0"/>
    <stop offset=".5" stop-color="#e79a76" stop-opacity=".18"/>
    <stop offset="1" stop-color="#2a1524" stop-opacity=".55"/>
  </linearGradient>
  <filter id="blur12" x="-100%" y="-100%" width="300%" height="300%"><feGaussianBlur stdDeviation="12"/></filter>
  <filter id="blur22" x="-100%" y="-100%" width="300%" height="300%"><feGaussianBlur stdDeviation="22"/></filter>
</defs>
<rect width="1920" height="1080" fill="url(#sky)"/>
{stars_svg}
<circle cx="1370" cy="742" r="430" fill="url(#sun)" opacity=".85"/>
<circle cx="1370" cy="742" r="48" fill="#fff0c8" opacity=".95" filter="url(#blur12)"/>
<g filter="url(#blur22)" style="mix-blend-mode:screen">
{''.join(clouds)}
</g>
<rect y="600" width="1920" height="480" fill="url(#mist)"/>
<path d="{ridge_path(far)}" fill="#3a2238" opacity=".95"/>
<path d="{ridge_path(mid)}" fill="#211426"/>
<path d="{ridge_path(near)}" fill="#0c0810"/>
<g fill="none" stroke="#1a0f18" stroke-width="2" opacity=".55" stroke-linecap="round">
  <path d="M470 420 q 12 -10 24 0 q 12 -10 24 0"/>
  <path d="M540 390 q 9 -8 18 0 q 9 -8 18 0"/>
  <path d="M420 455 q 8 -7 16 0 q 8 -7 16 0"/>
</g>
</svg>'''

# ---------------------------------------------------------------- nebula
def nebula():
    random.seed(51)
    stars_svg = stars(165, 12, 1080, big=True)
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1920 1080" preserveAspectRatio="xMidYMid slice">
<defs>
  <radialGradient id="base" cx=".5" cy=".4" r=".8">
    <stop offset="0" stop-color="#0a1324"/>
    <stop offset=".55" stop-color="#050810"/>
    <stop offset="1" stop-color="#02030a"/>
  </radialGradient>
  <radialGradient id="planet" cx=".3" cy=".26" r=".82">
    <stop offset="0" stop-color="#4a7fb4"/>
    <stop offset=".3" stop-color="#244566"/>
    <stop offset=".66" stop-color="#0c1826"/>
    <stop offset="1" stop-color="#03060b"/>
  </radialGradient>
  <radialGradient id="pglow" cx=".5" cy=".5" r=".5">
    <stop offset="0" stop-color="#5bc8ff" stop-opacity=".30"/>
    <stop offset=".55" stop-color="#3a6bff" stop-opacity=".10"/>
    <stop offset="1" stop-color="#000000" stop-opacity="0"/>
  </radialGradient>
  <linearGradient id="rim" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0" stop-color="#b8f1ff" stop-opacity=".95"/>
    <stop offset=".38" stop-color="#6aa8ff" stop-opacity=".40"/>
    <stop offset=".72" stop-color="#9b6bff" stop-opacity=".12"/>
    <stop offset="1" stop-color="#9b6bff" stop-opacity="0"/>
  </linearGradient>
  <radialGradient id="n1" cx=".5" cy=".5" r=".5">
    <stop offset="0" stop-color="#ff4fa3" stop-opacity=".30"/>
    <stop offset=".55" stop-color="#7b2bff" stop-opacity=".10"/>
    <stop offset="1" stop-color="#000000" stop-opacity="0"/>
  </radialGradient>
  <radialGradient id="n2" cx=".5" cy=".5" r=".5">
    <stop offset="0" stop-color="#3f7dff" stop-opacity=".26"/>
    <stop offset=".55" stop-color="#1d3a8f" stop-opacity=".08"/>
    <stop offset="1" stop-color="#000000" stop-opacity="0"/>
  </radialGradient>
  <filter id="blur90" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="90"/></filter>
  <filter id="glow" x="-200%" y="-200%" width="500%" height="500%"><feGaussianBlur stdDeviation="2.5"/></filter>
</defs>
<rect width="1920" height="1080" fill="url(#base)"/>
<g filter="url(#blur90)" opacity=".65">
  <ellipse cx="250" cy="900" rx="560" ry="320" fill="url(#n1)"/>
  <ellipse cx="700" cy="1010" rx="640" ry="280" fill="url(#n2)"/>
  <ellipse cx="1440" cy="980" rx="540" ry="260" fill="url(#n1)" opacity=".45"/>
</g>
{stars_svg}
<circle cx="1770" cy="135" r="330" fill="url(#pglow)"/>
<circle cx="1770" cy="135" r="246" fill="url(#planet)"/>
<circle cx="1770" cy="135" r="246" fill="none" stroke="url(#rim)" stroke-width="2.2" opacity=".7"/>
<circle cx="1770" cy="135" r="262" fill="none" stroke="#7fd8ff" stroke-width="1" opacity=".12"/>
<path d="M1560 80 A 250 250 0 0 1 1860 -20" fill="none" stroke="#d8f4ff" stroke-width="1.4" opacity=".18"/>
</svg>'''

# ---------------------------------------------------------------- forest
def forest():
    import random as rnd
    rnd.seed(71)
    # stars
    parts=[]
    for _ in range(80):
        x=rnd.uniform(0,W); y=rnd.uniform(0,520)
        r=rnd.choice([0.5,0.7,0.9,1.2,1.6]); o=rnd.uniform(.15,.8)
        parts.append(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{r}" fill="#e8f6ff" opacity="{o:.2f}"/>')
    stars="\n".join(parts)
    def trees(count, base, hmin, hmax, wmin, wmax, color, op, seed):
        rnd.seed(seed); out=[]
        for _ in range(count):
            x=rnd.uniform(-30, W+30)
            h=rnd.uniform(hmin,hmax); w=rnd.uniform(wmin,wmax)
            top=base-h
            tiers=4
            for i in range(tiers):
                ttop=top + i*h*0.20
                tbot=top + (i+1)*h*0.30
                half=w*(0.28 + i*0.22)
                out.append(f'<polygon points="{x:.0f},{ttop:.0f} {x-half:.0f},{tbot:.0f} {x+half:.0f},{tbot:.0f}" fill="{color}" opacity="{op}"/>')
            out.append(f'<rect x="{x-2:.0f}" y="{base-8:.0f}" width="4" height="10" fill="{color}" opacity="{op}"/>')
        return "\n".join(out)
    far=trees(70, 790, 100, 190, 26, 44, "#0a2027", .95, 72)
    mid=trees(42, 900, 190, 300, 42, 70, "#06151c", .98, 73)
    near=trees(20, 1040, 340, 520, 80, 130, "#02080c", 1, 74)
    fire=[]
    rnd.seed(75)
    for _ in range(16):
        x=rnd.uniform(100,1820); y=rnd.uniform(800,1010); o=rnd.uniform(.25,.7); r=rnd.uniform(1.2,2.2)
        fire.append(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{r}" fill="#ffd98a" opacity="{o:.2f}"/>')
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1920 1080" preserveAspectRatio="xMidYMid slice">
<defs>
  <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="#050b14"/>
    <stop offset=".45" stop-color="#0a1a26"/>
    <stop offset=".72" stop-color="#10333a"/>
    <stop offset="1" stop-color="#07161c"/>
  </linearGradient>
  <radialGradient id="moon" cx=".5" cy=".5" r=".5">
    <stop offset="0" stop-color="#eaf9ff" stop-opacity=".95"/>
    <stop offset=".45" stop-color="#bfe6ee" stop-opacity=".35"/>
    <stop offset="1" stop-color="#8fd0dc" stop-opacity="0"/>
  </radialGradient>
  <linearGradient id="fog" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="#8fd6cf" stop-opacity="0"/>
    <stop offset=".5" stop-color="#8fd6cf" stop-opacity=".16"/>
    <stop offset="1" stop-color="#8fd6cf" stop-opacity="0"/>
  </linearGradient>
  <filter id="blur30" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="30"/></filter>
</defs>
<rect width="1920" height="1080" fill="url(#sky)"/>
{stars}
<circle cx="1470" cy="250" r="260" fill="url(#moon)" opacity=".55"/>
<circle cx="1470" cy="250" r="64" fill="#eafaff" opacity=".9"/>
<circle cx="1470" cy="250" r="64" fill="none" stroke="#ffffff" stroke-width="1" opacity=".35"/>
<path d="M0 700 Q 480 620 960 690 T 1920 660 L 1920 1080 L 0 1080 Z" fill="#08181f" opacity=".85"/>
<g filter="url(#blur30)" opacity=".7">
  <rect y="700" width="1920" height="130" fill="url(#fog)"/>
  <rect y="790" width="1920" height="140" fill="url(#fog)" opacity=".8"/>
</g>
{far}
<g filter="url(#blur30)" opacity=".45">
  <rect y="820" width="1920" height="150" fill="url(#fog)"/>
</g>
{mid}
<path d="M0 930 Q 420 880 900 930 T 1920 910 L 1920 1080 L 0 1080 Z" fill="#030b0f"/>
{near}
<g filter="url(#blur30)" opacity=".35">
  <rect y="930" width="1920" height="120" fill="url(#fog)"/>
</g>
{''.join(fire)}
</svg>'''

(OUT / "aurora.svg").write_text(aurora(), encoding="utf-8")
(OUT / "dusk.svg").write_text(dusk(), encoding="utf-8")
(OUT / "nebula.svg").write_text(nebula(), encoding="utf-8")
(OUT / "forest.svg").write_text(forest(), encoding="utf-8")
print("written", *(f.name for f in OUT.glob("*.svg")))
