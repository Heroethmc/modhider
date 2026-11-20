# Pixel-art icon for Mod Hider: an eye with a slash on a dark tile. Drawn on a 32x32 grid.
import math, sys
from PIL import Image

N = 32
C = {
    'bg': (24, 34, 43), 'bg2': (31, 45, 56), 'bevel': (52, 74, 88), 'shadow': (15, 22, 28),
    'line': (12, 20, 16), 'white': (232, 241, 236), 'white2': (186, 204, 195),
    'iris': (63, 191, 127), 'iris2': (38, 138, 90), 'pupil': (12, 26, 20), 'glint': (255, 255, 255),
    'slash': (255, 107, 87), 'slash2': (204, 70, 56),
}
px = [[None] * N for _ in range(N)]

def put(x, y, c):
    if 0 <= x < N and 0 <= y < N:
        px[y][x] = C[c]

# Rounded tile with a light bevel top-left and a shadow bottom-right.
R = 4
for y in range(N):
    for x in range(N):
        cx = min(max(x, R), N - 1 - R); cy = min(max(y, R), N - 1 - R)
        if (x - cx) ** 2 + (y - cy) ** 2 > R * R:
            continue
        put(x, y, 'bg2' if (x + y) < N else 'bg')
for y in range(N):
    for x in range(N):
        if px[y][x] is None:
            continue
        inside = lambda a, b: 0 <= a < N and 0 <= b < N and px[b][a] is not None
        if not inside(x - 1, y) or not inside(x, y - 1):
            put(x, y, 'bevel')
        elif not inside(x + 1, y) or not inside(x, y + 1):
            put(x, y, 'shadow')

# Almond-shaped eye.
cx, cy, half_w, half_h = 15.5, 15.5, 12.5, 8.5
def in_eye(x, y):
    dx = (x - cx) / half_w
    if abs(dx) >= 1:
        return False
    return abs(y - cy) <= half_h * (1 - dx * dx)
for y in range(N):
    for x in range(N):
        if in_eye(x, y):
            put(x, y, 'white2' if y > cy + 2 else 'white')
for y in range(N):
    for x in range(N):
        if in_eye(x, y) and not all(in_eye(x + a, y + b) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            put(x, y, 'line')

# Iris, pupil and glint.
for y in range(N):
    for x in range(N):
        d = math.hypot(x - cx, y - cy)
        if d <= 5.2 and in_eye(x, y):
            put(x, y, 'iris2' if d > 4.2 or y > cy + 2 else 'iris')
        if d <= 2.3:
            put(x, y, 'pupil')
put(13, 13, 'glint'); put(14, 13, 'glint'); put(13, 14, 'glint')

# Slash from bottom-left to top-right, outlined so it reads at small sizes.
def on_slash(x, y, w):
    return abs((x - 15.5) + (y - 15.5)) <= w and 6 <= x <= 25
for y in range(N):
    for x in range(N):
        if px[y][x] is not None and on_slash(x, y, 2.6):
            put(x, y, 'line')
for y in range(N):
    for x in range(N):
        if px[y][x] is not None and on_slash(x, y, 1.6) and 7 <= x <= 24:
            put(x, y, 'slash2' if (x + y) % 2 == 0 and x > 15 else 'slash')

img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
for y in range(N):
    for x in range(N):
        if px[y][x] is not None:
            img.putpixel((x, y), px[y][x] + (255,))
for size, path in ((512, sys.argv[1]), (128, sys.argv[2])):
    img.resize((size, size), Image.NEAREST).save(path)
