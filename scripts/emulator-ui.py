"""Small ADB accessibility/screenshot helper for the temporary test emulator."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / '.local' / 'emulator-qa'
OUT.mkdir(parents=True, exist_ok=True)
ADB = r'C:\Android\Sdk\platform-tools\adb.exe'
SERIAL = 'emulator-5556'

def adb(*args, binary=False):
    result = subprocess.run([ADB, '-s', SERIAL, *args], capture_output=True, timeout=40)
    if result.returncode:
        raise RuntimeError(result.stderr.decode(errors='replace'))
    return result.stdout if binary else result.stdout.decode('utf-8', errors='replace').strip()

def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/personas-window.xml')
    xml = adb('exec-out', 'cat', '/sdcard/personas-window.xml')
    (OUT / 'latest.xml').write_text(xml, encoding='utf-8')
    return ET.fromstring(xml)

def snapshot():
    root = tree()
    nodes = []
    for node in root.iter('node'):
        a = node.attrib
        if a.get('text') or a.get('content-desc') or a.get('class') == 'android.widget.EditText' or a.get('scrollable') == 'true':
            nodes.append({k: a.get(k) for k in ('text', 'content-desc', 'class', 'resource-id', 'bounds', 'enabled', 'scrollable')})
    print(json.dumps({'rotation': root.get('rotation'), 'nodes': nodes}, ensure_ascii=True, indent=2))

def find(selector):
    root = tree()
    for a in ('text', 'content-desc', 'resource-id'):
        matches = [n for n in root.iter('node') if n.get(a) == selector]
        if matches:
            return matches[-1]
    raise RuntimeError('UI element not found: ' + selector)

def tap_node(node):
    coords = list(map(int, re.findall(r'\d+', node.get('bounds'))))
    assert len(coords) == 4 and coords[2] > coords[0] and coords[3] > coords[1]
    adb('shell', 'input', 'tap', str((coords[0] + coords[2]) // 2), str((coords[1] + coords[3]) // 2))

def tap(selector):
    tap_node(find(selector))

def fill(selector, value):
    def matched(root):
        return [n for n in root.iter('node') if n.get('class') == 'android.widget.EditText' and selector in
            [child.get(attribute, '') for child in n.iter('node') for attribute in ('text', 'content-desc', 'hint')]]
    for attempt in range(6):
        root = tree()
        matches = matched(root)
        scrolls = [n for n in root.iter('node') if n.get('scrollable') == 'true' and n.get('class') == 'android.widget.ScrollView']
        if matches:
            target = matches[-1]
            left, top, right, bottom = map(int, re.findall(r'\d+', target.get('bounds')))
            parents = {child: parent for parent in root.iter() for child in parent}
            ancestor = parents.get(target)
            while ancestor is not None:
                if ancestor.get('scrollable') == 'true':
                    x1, y1, x2, y2 = map(int, re.findall(r'\d+', ancestor.get('bounds')))
                    left, top, right, bottom = max(left, x1), max(top, y1), min(right, x2), min(bottom, y2)
                ancestor = parents.get(ancestor)
            if right > left and bottom - top >= 24:
                adb('shell', 'input', 'tap', str((left + right)//2), str((top + bottom)//2))
                break
        if not scrolls: raise RuntimeError('Editable field not found: ' + selector)
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', scrolls[-1].get('bounds')))
        adb('shell', 'input', 'swipe', str((x1+x2)//2), str(y2-30), str((x1+x2)//2), str(y1+30), '350')
    else: raise RuntimeError('Editable field not visible: ' + selector)
    time.sleep(.4)
    focused = matched(tree())
    if not focused or focused[-1].get('focused') != 'true':
        raise AssertionError('Field did not receive focus: ' + selector)
    previous = focused[-1].get('text', '')
    if previous:
        adb('shell', 'input', 'keyevent', '123')
        adb('shell', 'input', 'keyevent', *(['67'] * len(previous)))
        cleared = matched(tree())
        if not cleared or cleared[-1].get('text', ''):
            raise AssertionError('Field was not cleared before typing: ' + selector)
    if value:
        encoded = value.replace(' ', '%s').replace("'", '')
        adb('shell', 'input', 'text', "'" + encoded + "'")
    time.sleep(.2)
    edited = matched(tree())
    if not edited or edited[-1].get('text', '') != value:
        raise AssertionError('Field did not preserve the complete input: ' + selector + ' expected=' + value + ' actual=' + repr([n.get('text') for n in edited]))

def hide_keyboard():
    if 'mInputShown=true' in adb('shell', 'dumpsys', 'input_method'):
        adb('shell', 'input', 'keyevent', '4')
        time.sleep(.3)
def screenshot(name):
    path = OUT / (name + '.png')
    path.write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
    print(str(path))

def perform(steps):
    for step in steps:
        action, *values = step
        print('STEP ' + json.dumps(step, ensure_ascii=True), flush=True)
        if action == 'tap': tap(*values)
        elif action == 'fill': fill(*values)
        elif action == 'key': adb('shell', 'input', 'keyevent', *values)
        elif action == 'swipe': adb('shell', 'input', 'swipe', *values)
        elif action == 'screenshot': screenshot(*values)
        elif action == 'snapshot': snapshot()
        else: raise ValueError('Unknown UI action: ' + action)
        time.sleep(.5)
    print('READY', flush=True)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['snapshot', 'tap', 'fill', 'key', 'swipe', 'screenshot', 'plan', 'interactive'])
    parser.add_argument('args', nargs='*')
    args = parser.parse_args()
    if args.action == 'snapshot': snapshot()
    elif args.action == 'tap': tap(args.args[0])
    elif args.action == 'fill': fill(*args.args)
    elif args.action == 'key': adb('shell', 'input', 'keyevent', args.args[0])
    elif args.action == 'swipe': adb('shell', 'input', 'swipe', *args.args)
    elif args.action == 'screenshot': screenshot(args.args[0])
    elif args.action == 'plan':
        plan = Path(args.args[0]).resolve()
        if not plan.is_relative_to(OUT.resolve()):
            raise ValueError('Plans must be inside .local/emulator-qa')
        perform(json.loads(plan.read_text(encoding='utf-8-sig')))
    elif args.action == 'interactive':
        print('READY', flush=True)
        for line in sys.stdin:
            if line.strip() == 'quit': break
            try: perform(json.loads(line))
            except Exception as error: print('ERROR ' + str(error), flush=True)
