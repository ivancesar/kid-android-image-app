#!/usr/bin/env python3
"""Convert the Illustrator-exported category SVGs to Android VectorDrawables.

Handles exactly what this icon set uses: a CSS <style> block mapping .stN
classes to fill/stroke props, <path>/<circle>/<ellipse>/<rect> elements, plain
<g> wrappers, and translate+rotate transforms. Primitives and transformed
elements are emitted as cubic Beziers, which are affine-invariant, so a baked
transform stays exact instead of fighting SVG-vs-VectorDrawable arc semantics.
"""
import math, os, re, sys
import xml.etree.ElementTree as ET

SVG_NS = 'http://www.w3.org/2000/svg'
K = 0.5522847498307936  # circle -> 4-cubic approximation constant

def tag(e):
    return e.tag.split('}')[-1]

def parse_css(text):
    """.st0 { fill: #fff; }  ->  {'st0': {'fill': '#fff'}}; multi-selector aware."""
    rules = {}
    for sel, body in re.findall(r'([^{}]+)\{([^}]*)\}', text):
        props = {}
        for decl in body.split(';'):
            if ':' in decl:
                k, v = decl.split(':', 1)
                props[k.strip()] = v.strip()
        for name in sel.split(','):
            name = name.strip().lstrip('.')
            if name:
                rules.setdefault(name, {}).update(props)
    return rules

def parse_transform(s):
    """Compose a transform list into a single affine (a,b,c,d,e,f)."""
    m = (1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
    for fn, args in re.findall(r'(\w+)\s*\(([^)]*)\)', s or ''):
        n = [float(x) for x in re.split(r'[\s,]+', args.strip()) if x]
        if fn == 'translate':
            t = (1, 0, 0, 1, n[0], n[1] if len(n) > 1 else 0)
        elif fn == 'rotate':
            r = math.radians(n[0]); cos, sin = math.cos(r), math.sin(r)
            t = (cos, sin, -sin, cos, 0, 0)
            if len(n) == 3:  # rotate about a point
                m = mul(m, (1, 0, 0, 1, n[1], n[2]))
                m = mul(m, t)
                m = mul(m, (1, 0, 0, 1, -n[1], -n[2]))
                continue
        elif fn == 'scale':
            t = (n[0], 0, 0, n[1] if len(n) > 1 else n[0], 0, 0)
        elif fn == 'matrix':
            t = tuple(n)
        else:
            raise SystemExit('unsupported transform: %s' % fn)
        m = mul(m, t)
    return m

def mul(m, n):
    a1, b1, c1, d1, e1, f1 = m; a2, b2, c2, d2, e2, f2 = n
    return (a1*a2 + c1*b2, b1*a2 + d1*b2,
            a1*c2 + c1*d2, b1*c2 + d1*d2,
            a1*e2 + c1*f2 + e1, b1*e2 + d1*f2 + f1)

def apply(m, x, y):
    a, b, c, d, e, f = m
    return (a*x + c*y + e, b*x + d*y + f)

def num(v):
    """Trim floats so path data stays compact and readable."""
    s = '%.3f' % v
    s = s.rstrip('0').rstrip('.')
    return '0' if s in ('', '-0') else s

def pts(m, *coords):
    out = []
    for i in range(0, len(coords), 2):
        out.append('%s,%s' % tuple(num(v) for v in apply(m, coords[i], coords[i+1])))
    return ' '.join(out)

def ellipse_path(cx, cy, rx, ry, m):
    ox, oy = rx * K, ry * K
    return ('M%s C%s C%s C%s C%s Z' % (
        pts(m, cx + rx, cy),
        pts(m, cx + rx, cy + oy, cx + ox, cy + ry, cx, cy + ry),
        pts(m, cx - ox, cy + ry, cx - rx, cy + oy, cx - rx, cy),
        pts(m, cx - rx, cy - oy, cx - ox, cy - ry, cx, cy - ry),
        pts(m, cx + ox, cy - ry, cx + rx, cy - oy, cx + rx, cy)))

def rect_path(x, y, w, h, rx, ry, m):
    rx, ry = min(rx, w / 2.0), min(ry, h / 2.0)
    if rx <= 0 and ry <= 0:
        return 'M%s L%s L%s L%s Z' % (pts(m, x, y), pts(m, x + w, y),
                                      pts(m, x + w, y + h), pts(m, x, y + h))
    ox, oy = rx * K, ry * K
    return ('M%s L%s C%s L%s C%s L%s C%s L%s C%s Z' % (
        pts(m, x + rx, y), pts(m, x + w - rx, y),
        pts(m, x + w - rx + ox, y, x + w, y + ry - oy, x + w, y + ry),
        pts(m, x + w, y + h - ry),
        pts(m, x + w, y + h - ry + oy, x + w - rx + ox, y + h, x + w - rx, y + h),
        pts(m, x + rx, y + h),
        pts(m, x + rx - ox, y + h, x, y + h - ry + oy, x, y + h - ry),
        pts(m, x, y + ry),
        pts(m, x, y + ry - oy, x + rx - ox, y, x + rx, y)))

NAMED = {'black': '#000000', 'white': '#ffffff'}

def color(c):
    c = NAMED.get(c.strip().lower(), c).strip().lower()
    if not c.startswith('#'):
        raise SystemExit('unsupported color: %s' % c)
    h = c[1:]
    if len(h) == 3:
        h = ''.join(ch * 2 for ch in h)
    return '#FF' + h.upper()

def props_for(el, css):
    p = {}
    for cls in (el.get('class') or '').split():
        p.update(css.get(cls, {}))
    for k in ('fill', 'stroke', 'stroke-width', 'stroke-linecap', 'stroke-linejoin'):
        if el.get(k):
            p[k] = el.get(k)
    return p

def walk(el, css, inherited, out):
    m = mul(inherited, parse_transform(el.get('transform')))
    t = tag(el)
    if t == 'g':
        for child in el:
            walk(child, css, m, out)
        return
    if t in ('defs', 'style', 'title', 'desc'):
        return
    p = props_for(el, css)
    if t == 'path':
        d = el.get('d')
        if m != (1.0, 0.0, 0.0, 1.0, 0.0, 0.0):
            raise SystemExit('transform on <path> needs path-level baking')
    elif t == 'circle':
        r = float(el.get('r'))
        d = ellipse_path(float(el.get('cx')), float(el.get('cy')), r, r, m)
    elif t == 'ellipse':
        d = ellipse_path(float(el.get('cx')), float(el.get('cy')),
                         float(el.get('rx')), float(el.get('ry')), m)
    elif t == 'rect':
        d = rect_path(float(el.get('x')), float(el.get('y')),
                      float(el.get('width')), float(el.get('height')),
                      float(el.get('rx') or 0), float(el.get('ry') or 0), m)
    else:
        raise SystemExit('unsupported element: <%s>' % t)
    out.append((d, p))

def convert(src, dst, name):
    ET.register_namespace('', SVG_NS)
    root = ET.parse(src).getroot()
    css = {}
    for st in root.iter('{%s}style' % SVG_NS):
        css.update(parse_css(st.text or ''))

    vb = [float(x) for x in re.split(r'[\s,]+', root.get('viewBox').strip())]
    shapes = []
    for child in root:
        walk(child, css, (1.0, 0.0, 0.0, 1.0, 0.0, 0.0), shapes)

    lines = ['<?xml version="1.0" encoding="utf-8"?>',
             '<!-- Generated from icons-src/%s by tools/svg2vd.py. Do not edit by hand. -->' % name,
             '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
             '    android:width="24dp"',
             '    android:height="24dp"',
             '    android:viewportWidth="%s"' % num(vb[2]),
             '    android:viewportHeight="%s">' % num(vb[3])]
    for d, p in shapes:
        lines.append('    <path')
        attrs = []
        # SVG's initial fill is black, so an unclassed shape with no fill
        # property is a black shape -- not an unfilled one.
        fill = p.get('fill', 'black')
        if fill == 'none':
            attrs.append('android:fillColor="@android:color/transparent"')
        else:
            attrs.append('android:fillColor="%s"' % color(fill))
        if p.get('stroke') and p['stroke'] != 'none':
            attrs.append('android:strokeColor="%s"' % color(p['stroke']))
            attrs.append('android:strokeWidth="%s"' % num(float(
                re.sub(r'[a-z]+$', '', p.get('stroke-width', '1')))))
            if p.get('stroke-linecap'):
                attrs.append('android:strokeLineCap="%s"' % p['stroke-linecap'])
            if p.get('stroke-linejoin'):
                attrs.append('android:strokeLineJoin="%s"' % p['stroke-linejoin'])
        for a in attrs:
            lines.append('        %s' % a)
        lines.append('        android:pathData="%s" />' % d.replace('"', ''))
    lines.append('</vector>')
    with open(dst, 'w') as fh:
        fh.write('\n'.join(lines) + '\n')
    return len(shapes)

if __name__ == '__main__':
    src_dir, out_dir = sys.argv[1], sys.argv[2]
    for f in sorted(os.listdir(src_dir)):
        if not f.endswith('.svg'):
            continue
        stem = f[:-4]
        dst = os.path.join(out_dir, 'ic_theme_%s.xml' % stem)
        n = convert(os.path.join(src_dir, f), dst, f)
        print('%-18s -> %-28s %2d paths' % (f, os.path.basename(dst), n))
