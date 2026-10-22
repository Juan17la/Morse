#!/usr/bin/env python3
"""Morse control panel: the PC simulator (morse_sim.py) with a small web UI to drive the group.

    python3 tools/morse_panel.py                     # host "Ana" + bot "Luis", opens the browser
    python3 tools/morse_panel.py --code 1234 --bots Ana --port 8080
    python3 tools/morse_panel.py --selftest

Join from the emulator as usual (Settings > Connection > LAN, host 10.0.2.2, Join with the code).
From the panel you pick which bot talks, type a word or tap a preset (normal or silly), and it
lands on the watch's Incoming screen with its Morse vibration. Auto-reply can be switched off.
Stdlib only; the page is served on localhost only.
"""
import argparse
import json
import sys
import threading
import webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import morse_sim

MAX_WORD = 20  # WordsRepository.MAX_WORD_LENGTH

PRESETS = {
    'normal': ['OK', 'YES', 'NO', 'ON MY WAY', 'WHERE ARE YOU?', 'CALL ME', 'WAIT', 'THANKS',
               'HOME', 'SOS', 'HOLA', 'VOY', '¿DONDE ESTAS?', 'GRACIAS'],
    'silly': ['PIZZA TIME', 'BEEP BOOP', 'I AM A POTATO', 'MOO', 'TACO TUESDAY?', 'NAP TIME',
              'ALIENS ARE HERE', 'DANCE BREAK', 'BANANA', 'SEND SNACKS', 'QUACK', 'YEET'],
}


def state(host):
    return {
        'code': host.code,
        'bots': [b['name'] for b in host.bots],
        'members': host.members(),
        'maxMembers': morse_sim.MAX_MEMBERS,
        'autoReply': host.auto_reply,
        'log': list(host.log)[-60:],
        'presets': PRESETS,
    }


def make_handler(host):
    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *args):
            pass  # morse_sim already prints every message

        def reply(self, code, body, ctype='application/json'):
            data = body if isinstance(body, bytes) else json.dumps(body, ensure_ascii=False).encode()
            self.send_response(code)
            self.send_header('Content-Type', ctype + '; charset=utf-8')
            self.send_header('Cache-Control', 'no-store')
            self.end_headers()
            self.wfile.write(data)

        def do_GET(self):
            if self.path == '/':
                return self.reply(200, PAGE.encode(), 'text/html')
            if self.path == '/api/state':
                return self.reply(200, state(host))
            self.reply(404, {'error': 'not found'})

        def do_POST(self):
            try:
                body = json.loads(self.rfile.read(int(self.headers.get('Content-Length', 0))) or '{}')
            except ValueError:
                return self.reply(400, {'error': 'bad json'})
            if self.path == '/api/send':
                word = ' '.join(str(body.get('word', '')).split())[:MAX_WORD]
                if not morse_sim.encode(word):
                    return self.reply(400, {'error': 'nothing to send'})
                bot = next((b for b in host.bots if b['name'] == body.get('bot')), host.bots[0])
                host.bot_says(bot, word.upper())
                return self.reply(200, state(host))
            if self.path == '/api/auto':
                host.auto_reply = bool(body.get('on'))
                return self.reply(200, state(host))
            self.reply(404, {'error': 'not found'})

    return Handler


def selftest():
    host = morse_sim.Host('1234', ['Ana', 'Luis'])
    server = ThreadingHTTPServer(('127.0.0.1', 0), make_handler(host))
    threading.Thread(target=server.serve_forever, daemon=True).start()
    import urllib.request
    base = f'http://127.0.0.1:{server.server_port}'

    def post(path, body):
        req = urllib.request.Request(base + path, json.dumps(body).encode(), {'Content-Type': 'application/json'})
        return json.loads(urllib.request.urlopen(req).read())

    assert b'Morse Control' in urllib.request.urlopen(base + '/').read()
    s = post('/api/send', {'bot': 'Luis', 'word': '  beep   boop  and a very long tail '})
    last = s['log'][-1]
    assert last['senderName'] == 'Luis' and last['word'] == 'BEEP BOOP AND A VERY' and last['morse']
    assert post('/api/auto', {'on': False})['autoReply'] is False
    try:
        post('/api/send', {'word': '!!!'})
        raise AssertionError('empty morse must be rejected')
    except urllib.error.HTTPError as e:
        assert e.code == 400
    assert all(len(w) <= MAX_WORD and morse_sim.encode(w) for ws in PRESETS.values() for w in ws)
    server.shutdown()
    print('panel selftest ok')


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--code', default=''.join(morse_sim.random.choice('0123456789') for _ in range(4)))
    p.add_argument('--bots', default='Ana,Luis', help='comma-separated; the first is the host')
    p.add_argument('--port', type=int, default=8080, help='panel port (the group itself uses 8765)')
    p.add_argument('--no-browser', action='store_true')
    p.add_argument('--selftest', action='store_true')
    args = p.parse_args()
    if args.selftest:
        return selftest()
    sys.stdout.reconfigure(line_buffering=True)

    names = [n.strip() for n in args.bots.split(',') if n.strip()][:morse_sim.MAX_MEMBERS - 1]
    if not names:
        p.error('need at least one bot: the first one is the group host')
    host = morse_sim.Host(args.code, names)
    threading.Thread(target=host.serve, daemon=True).start()
    url = f'http://localhost:{args.port}'
    print(f'Morse group on port {morse_sim.PORT}   ACCESS CODE: {args.code}')
    print(f'Control panel: {url}   (Ctrl+C to stop)')
    print('Emulator: Settings > Connection > LAN, host 10.0.2.2, then Join with the code above.\n')
    if not args.no_browser:
        webbrowser.open(url)
    try:
        ThreadingHTTPServer(('127.0.0.1', args.port), make_handler(host)).serve_forever()
    except KeyboardInterrupt:
        host.broadcast({'type': 'bye', 'memberId': host.bots[0]['id']})


PAGE = r'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Morse Control</title>
<style>
:root {
  --bg: #f2f2f7; --card: #ffffff; --card2: #f2f2f7; --text: #000; --text2: #6e6e73; --sep: rgba(60,60,67,.14);
  --orange: #ff9500; --orange2: #ff6a00; --blue: #007aff; --green: #34c759; --purple: #af52de; --pink: #ff2d55; --red: #ff3b30;
  --shadow: 0 1px 2px rgba(0,0,0,.04), 0 8px 24px rgba(0,0,0,.06);
  --bubble-in: #e9e9eb;
}
@media (prefers-color-scheme: dark) {
  :root {
    --bg: #000; --card: #1c1c1e; --card2: #2c2c2e; --text: #fff; --text2: #8e8e93; --sep: rgba(84,84,88,.6);
    --orange: #ff9f0a; --blue: #0a84ff; --green: #30d158; --purple: #bf5af2; --pink: #ff375f; --red: #ff453a;
    --shadow: none; --bubble-in: #26262a;
  }
}
* { box-sizing: border-box; margin: 0; }
html { -webkit-font-smoothing: antialiased; }
body {
  font: 15px/1.4 -apple-system, BlinkMacSystemFont, "SF Pro Text", "Inter", "Segoe UI", system-ui, sans-serif;
  background: var(--bg); color: var(--text); min-height: 100vh;
}
.wrap { max-width: 1040px; margin: 0 auto; padding: 28px 16px 48px; }
header { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; flex-wrap: wrap; margin-bottom: 22px; }
h1 { font-size: 34px; font-weight: 700; letter-spacing: -.02em; }
h1 span { background: linear-gradient(135deg, #ffc145, var(--orange), var(--orange2)); -webkit-background-clip: text; background-clip: text; color: transparent; }
.sub { color: var(--text2); margin-top: 2px; }
.code { display: flex; gap: 6px; }
.code b {
  width: 44px; height: 56px; display: grid; place-items: center; border-radius: 12px; background: var(--card);
  box-shadow: var(--shadow); font: 700 30px ui-monospace, "SF Mono", Menlo, monospace;
  background-image: linear-gradient(180deg, rgba(255,255,255,.06), transparent);
}
.code b i { font-style: normal; background: linear-gradient(180deg, #ffc145, var(--orange2)); -webkit-background-clip: text; background-clip: text; color: transparent; }
.grid { display: grid; grid-template-columns: 1.15fr .85fr; gap: 20px; }
@media (max-width: 820px) { .grid { grid-template-columns: 1fr; } }
.label { font-size: 13px; text-transform: uppercase; letter-spacing: .04em; color: var(--text2); margin: 0 0 6px 16px; }
.card { background: var(--card); border-radius: 16px; box-shadow: var(--shadow); overflow: hidden; margin-bottom: 22px; }
.pad { padding: 16px; }
.row { display: flex; align-items: center; gap: 12px; padding: 11px 16px; min-height: 52px; }
.row + .row { border-top: .5px solid var(--sep); }
.row .grow { flex: 1; min-width: 0; }
.muted { color: var(--text2); font-size: 13px; }
.ellipsis { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.avatar { width: 34px; height: 34px; border-radius: 50%; display: grid; place-items: center; color: #fff; font-weight: 600; flex: none;
  box-shadow: inset 0 1px 0 rgba(255,255,255,.35); }
.tag { font-size: 12px; padding: 2px 8px; border-radius: 99px; background: var(--card2); color: var(--text2); }
.tag.host { background: color-mix(in srgb, var(--orange) 18%, transparent); color: var(--orange); }
.tag.bot { background: color-mix(in srgb, var(--purple) 16%, transparent); color: var(--purple); }
.tag.watch { background: color-mix(in srgb, var(--green) 16%, transparent); color: var(--green); }
.empty-slot { color: var(--text2); }
.empty-slot .avatar { background: transparent; border: 1.5px dashed var(--sep); box-shadow: none; }

.seg { display: flex; background: var(--card2); border-radius: 9px; padding: 2px; position: relative; }
.seg button { flex: 1; border: 0; background: none; font: inherit; font-weight: 500; color: var(--text); padding: 7px 10px; border-radius: 7px; cursor: pointer; position: relative; z-index: 1; transition: color .2s; }
.seg .thumb { position: absolute; top: 2px; bottom: 2px; border-radius: 7px; background: var(--card); box-shadow: 0 1px 3px rgba(0,0,0,.12), 0 3px 8px rgba(0,0,0,.08); transition: transform .35s cubic-bezier(.3,1.3,.5,1), width .35s; }
@media (prefers-color-scheme: dark) { .seg .thumb { background: #636366; } }

.compose { display: flex; gap: 10px; align-items: center; margin-top: 14px; }
.field { flex: 1; position: relative; }
.field input {
  width: 100%; font: inherit; font-size: 17px; padding: 12px 56px 12px 16px; border-radius: 12px; border: 0; outline: 0;
  background: var(--card2); color: var(--text); text-transform: uppercase; transition: box-shadow .2s;
}
.field input::placeholder { text-transform: none; }
.field input:focus { box-shadow: 0 0 0 3px color-mix(in srgb, var(--blue) 35%, transparent); }
.field .count { position: absolute; right: 14px; top: 50%; transform: translateY(-50%); font-size: 12px; color: var(--text2); font-variant-numeric: tabular-nums; }
.send {
  width: 46px; height: 46px; border-radius: 50%; border: 0; cursor: pointer; flex: none; color: #fff; font-size: 20px;
  background: linear-gradient(180deg, #4da3ff, var(--blue)); box-shadow: 0 4px 14px color-mix(in srgb, var(--blue) 40%, transparent);
  transition: transform .25s cubic-bezier(.3,1.5,.5,1), opacity .2s;
}
.send:active { transform: scale(.88); }
.send:disabled { opacity: .35; box-shadow: none; cursor: default; }
.morse { min-height: 26px; margin-top: 12px; display: flex; flex-wrap: wrap; gap: 6px 10px; align-items: center; }
.letter { display: flex; gap: 4px; }
.dot, .dash { height: 8px; border-radius: 99px; background: linear-gradient(90deg, #ffc145, var(--orange2)); animation: pop .35s cubic-bezier(.3,1.6,.5,1) both; }
.dot { width: 8px; } .dash { width: 24px; }
.wordgap { width: 1.5px; height: 14px; background: var(--sep); }
@keyframes pop { from { transform: scale(.2); opacity: 0; } }

.chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 14px; }
.chip {
  border: 0; font: inherit; font-weight: 500; font-size: 14px; padding: 8px 14px; border-radius: 99px; cursor: pointer;
  background: var(--card2); color: var(--text); transition: transform .25s cubic-bezier(.3,1.5,.5,1), background .2s;
  animation: rise .4s cubic-bezier(.3,1.3,.5,1) both;
}
.chip:hover { background: color-mix(in srgb, var(--accent, var(--blue)) 16%, var(--card2)); }
.chip:active { transform: scale(.92); }
.silly .chip { --accent: var(--pink); }
.chip.add { color: var(--blue); }
@keyframes rise { from { transform: translateY(8px); opacity: 0; } }

.toggle { position: relative; width: 51px; height: 31px; flex: none; }
.toggle input { opacity: 0; width: 0; height: 0; }
.toggle span { position: absolute; inset: 0; border-radius: 99px; background: var(--card2); transition: background .25s; cursor: pointer; box-shadow: inset 0 0 0 1px var(--sep); }
.toggle span::after { content: ""; position: absolute; top: 2px; left: 2px; width: 27px; height: 27px; border-radius: 50%; background: #fff; box-shadow: 0 3px 8px rgba(0,0,0,.15), 0 1px 1px rgba(0,0,0,.16); transition: transform .3s cubic-bezier(.3,1.4,.5,1); }
.toggle input:checked + span { background: var(--green); box-shadow: none; }
.toggle input:checked + span::after { transform: translateX(20px); }

.feed { height: 460px; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 4px; scroll-behavior: smooth; }
.msg { max-width: 78%; display: flex; flex-direction: column; animation: rise .35s cubic-bezier(.3,1.3,.5,1) both; }
.msg.out { align-self: flex-end; align-items: flex-end; }
.msg .who { font-size: 11px; color: var(--text2); margin: 8px 12px 2px; }
.bubble { padding: 8px 14px; border-radius: 19px; background: var(--bubble-in); }
.out .bubble { background: linear-gradient(180deg, #4da3ff, var(--blue)); color: #fff; }
.bubble .m { font: 11px ui-monospace, "SF Mono", Menlo, monospace; opacity: .65; letter-spacing: .08em; margin-top: 2px; word-break: break-all; }
.feed-empty { margin: auto; text-align: center; color: var(--text2); }
.feed-empty .big { font-size: 40px; margin-bottom: 6px; }

.status { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; color: var(--text2); }
.status::before { content: ""; width: 8px; height: 8px; border-radius: 50%; background: var(--green); box-shadow: 0 0 0 0 var(--green); animation: breathe 2s infinite; }
.status.off::before { background: var(--red); animation: none; }
@keyframes breathe { 50% { box-shadow: 0 0 0 5px transparent; } 0% { box-shadow: 0 0 0 0 color-mix(in srgb, var(--green) 60%, transparent); } }

.toast { position: fixed; left: 50%; bottom: 28px; transform: translate(-50%, 30px); opacity: 0; padding: 10px 18px; border-radius: 99px;
  background: rgba(30,30,30,.85); color: #fff; backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px); font-weight: 500;
  transition: transform .4s cubic-bezier(.3,1.4,.5,1), opacity .25s; pointer-events: none; }
.toast.show { transform: translate(-50%, 0); opacity: 1; }
.del { border: 0; background: none; color: var(--red); cursor: pointer; font-size: 13px; margin-left: 2px; }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { animation: none !important; transition: none !important; } }
</style>
</head>
<body>
<div class="wrap">
  <header>
    <div>
      <h1><span>Morse</span> Control</h1>
      <div class="sub"><span id="status" class="status">Waiting for a watch…</span></div>
    </div>
    <div>
      <div class="label" style="margin-left:2px">Access code</div>
      <div class="code" id="code"></div>
    </div>
  </header>

  <div class="grid">
    <div>
      <div class="label">Send as</div>
      <div class="card pad">
        <div class="seg" id="senders"><div class="thumb"></div></div>
        <div class="compose">
          <div class="field">
            <input id="word" maxlength="20" placeholder="Type a word" autocomplete="off" aria-label="Message">
            <span class="count" id="count">0/20</span>
          </div>
          <button class="send" id="send" disabled aria-label="Send">&#x2191;</button>
        </div>
        <div class="morse" id="preview" aria-hidden="true"></div>
      </div>

      <div class="label">Presets</div>
      <div class="card pad">
        <div class="seg" id="kinds"><div class="thumb"></div>
          <button data-kind="normal">Normal</button><button data-kind="silly">Silly</button><button data-kind="mine">Mine</button>
        </div>
        <div class="chips" id="chips"></div>
      </div>

      <div class="label">Bots</div>
      <div class="card">
        <div class="row">
          <div class="grow"><div>Auto-reply</div><div class="muted">Bots answer every watch message after 2–4 s</div></div>
          <label class="toggle"><input type="checkbox" id="auto"><span></span></label>
        </div>
      </div>
    </div>

    <div>
      <div class="label" id="membersLabel">Members</div>
      <div class="card" id="members"></div>
      <div class="label">Conversation</div>
      <div class="card"><div class="feed" id="feed"></div></div>
    </div>
  </div>
</div>
<div class="toast" id="toast"></div>

<script>
const TABLE = {A:'.-',B:'-...',C:'-.-.',D:'-..',E:'.',F:'..-.',G:'--.',H:'....',I:'..',J:'.---',K:'-.-',L:'.-..',M:'--',N:'-.',O:'---',P:'.--.',Q:'--.-',R:'.-.',S:'...',T:'-',U:'..-',V:'...-',W:'.--',X:'-..-',Y:'-.--',Z:'--..','0':'-----','1':'.----','2':'..---','3':'...--','4':'....-','5':'.....','6':'-....','7':'--...','8':'---..','9':'----.','Ñ':'--.--'};
const ACC = {'Á':'A','É':'E','Í':'I','Ó':'O','Ú':'U','Ü':'U'};
const encode = t => t.trim().toUpperCase().split(/\s+/).filter(Boolean)
  .map(w => [...w].map(c => TABLE[ACC[c] || c]).filter(Boolean).join(' ')).join(' / ').trim();
const COLORS = ['#0a84ff','#30d158','#bf5af2','#ff375f','#64d2ff','#5e5ce6','#ff9f0a'];
// Same pick as avatarColor() on the watch (Java String.hashCode, floorMod).
const color = n => { let h = 0; for (let i = 0; i < n.length; i++) h = (Math.imul(31, h) + n.charCodeAt(i)) | 0; return COLORS[((h % 7) + 7) % 7]; };
const short = (n, max = 14) => { const f = n.trim().split(' ')[0] || '?'; return f.length <= max ? f : f.slice(0, max - 1) + '…'; };
const $ = id => document.getElementById(id);
const esc = s => s.replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const store = { get(k, d) { try { return JSON.parse(localStorage.getItem(k)) ?? d; } catch { return d; } },
                set(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch {} } };

let S = null, sender = store.get('sender', null), kind = store.get('kind', 'normal'), mine = store.get('mine', []), lastLogId = null;

function moveThumb(seg) {
  const on = seg.querySelector('button.on'), th = seg.querySelector('.thumb');
  if (!on) return;
  th.style.width = on.offsetWidth + 'px';
  th.style.transform = `translateX(${on.offsetLeft - 2}px)`;
  th.style.left = '2px';
}
function select(seg, match) {
  seg.querySelectorAll('button').forEach(b => b.classList.toggle('on', match(b)));
  moveThumb(seg);
}
function toast(t) { const el = $('toast'); el.textContent = t; el.classList.add('show'); clearTimeout(toast.h); toast.h = setTimeout(() => el.classList.remove('show'), 1800); }

function morseHTML(m) {
  return m.split(' ').filter(Boolean).map(l => l === '/' ? '<span class="wordgap"></span>'
    : `<span class="letter">${[...l].map(s => `<span class="${s === '.' ? 'dot' : 'dash'}"></span>`).join('')}</span>`).join('');
}

async function api(path, body) {
  const r = await fetch(path, body ? { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body) } : {});
  if (!r.ok) throw new Error((await r.json()).error);
  return r.json();
}

async function send(word) {
  if (!encode(word)) return;
  try { render(await api('/api/send', { bot: sender, word })); toast(`${short(sender)} sent “${word.toUpperCase()}”`); }
  catch (e) { toast(e.message); }
}

function renderChips() {
  const words = kind === 'mine' ? mine : S.presets[kind];
  $('chips').className = 'chips ' + kind;
  $('chips').innerHTML = words.map((w, i) =>
    `<button class="chip" style="animation-delay:${i * 18}ms" data-w="${esc(w)}">${esc(w)}${kind === 'mine' ? `<span class="del" data-del="${i}" aria-label="Remove">✕</span>` : ''}</button>`).join('')
    + (kind === 'mine' ? '<button class="chip add" id="addMine">＋ Save current word</button>' : '');
}

function render(s) {
  const first = !S; S = s;
  $('code').innerHTML = [...s.code].map(d => `<b><i>${d}</i></b>`).join('');
  if (first) {
    if (!s.bots.includes(sender)) sender = s.bots[0];
    $('senders').insertAdjacentHTML('beforeend', s.bots.map(b => `<button data-bot="${esc(b)}">${esc(short(b))}</button>`).join(''));
    select($('senders'), b => b.dataset.bot === sender);
    select($('kinds'), b => b.dataset.kind === kind);
    renderChips();
  }
  $('auto').checked = s.autoReply;

  const watches = s.members.filter(m => !m.id.startsWith('pc-bot-'));
  $('status').textContent = watches.length ? `${watches.length} watch${watches.length > 1 ? 'es' : ''} connected` : 'Waiting for a watch…';
  $('status').classList.toggle('off', !watches.length);
  $('membersLabel').textContent = `Members ${s.members.length} / ${s.maxMembers}`;
  const rows = s.members.map(m => {
    const bot = m.id.startsWith('pc-bot-');
    return `<div class="row"><div class="avatar" style="background:linear-gradient(180deg,color-mix(in srgb,${color(m.name)} 70%,#fff),${color(m.name)})">${esc((m.name[0] || '?').toUpperCase())}</div>
      <div class="grow ellipsis" title="${esc(m.name)}">${esc(short(m.name))}</div>
      ${m.isHost ? '<span class="tag host">Host</span>' : ''}<span class="tag ${bot ? 'bot' : 'watch'}">${bot ? 'Bot' : 'Watch'}</span></div>`;
  });
  for (let i = s.members.length; i < s.maxMembers; i++) rows.push('<div class="row empty-slot"><div class="avatar"></div><div class="grow">Open slot</div></div>');
  $('members').innerHTML = rows.join('');

  const newest = s.log.length ? s.log[s.log.length - 1].id : null;
  if (newest !== lastLogId) {
    lastLogId = newest;
    $('feed').innerHTML = s.log.length ? s.log.map(m => {
      const out = m.senderId.startsWith('pc-bot-');
      return `<div class="msg ${out ? 'out' : ''}"><div class="who">${esc(short(m.senderName))} · ${new Date(m.timestampMs).toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})}</div>
        <div class="bubble">${esc(m.word)}<div class="m">${esc(m.morse)}</div></div></div>`;
    }).join('') : '<div class="feed-empty"><div class="big">⌚︎</div>No messages yet.<br>Pick a preset to say hi.</div>';
    $('feed').scrollTop = $('feed').scrollHeight;
  }
}

$('word').addEventListener('input', e => {
  const v = e.target.value, m = encode(v);
  $('count').textContent = `${v.length}/20`;
  $('send').disabled = !m;
  $('preview').innerHTML = morseHTML(m);
});
$('word').addEventListener('keydown', e => { if (e.key === 'Enter') $('send').click(); });
$('send').addEventListener('click', () => { const v = $('word').value; send(v); $('word').value = ''; $('word').dispatchEvent(new Event('input')); });
$('senders').addEventListener('click', e => { const b = e.target.closest('button'); if (!b) return; sender = b.dataset.bot; store.set('sender', sender); select($('senders'), x => x === b); });
$('kinds').addEventListener('click', e => { const b = e.target.closest('button'); if (!b) return; kind = b.dataset.kind; store.set('kind', kind); select($('kinds'), x => x === b); renderChips(); });
$('chips').addEventListener('click', e => {
  const del = e.target.closest('[data-del]');
  if (del) { mine.splice(+del.dataset.del, 1); store.set('mine', mine); return renderChips(); }
  if (e.target.closest('#addMine')) {
    const w = $('word').value.trim().toUpperCase();
    if (!encode(w)) return toast('Type a word first');
    if (!mine.includes(w)) { mine.push(w); store.set('mine', mine); }
    return renderChips();
  }
  const c = e.target.closest('.chip'); if (c) send(c.dataset.w);
});
$('auto').addEventListener('change', async e => { render(await api('/api/auto', { on: e.target.checked })); toast(e.target.checked ? 'Auto-reply on' : 'Auto-reply off'); });
addEventListener('resize', () => { moveThumb($('senders')); moveThumb($('kinds')); });

// ponytail: 1 s polling; switch to SSE if the log ever needs to be instant.
async function poll() {
  try { render(await api('/api/state')); } catch { $('status').textContent = 'Panel stopped'; $('status').classList.add('off'); }
  setTimeout(poll, 1000);
}
poll();
</script>
</body>
</html>
'''

if __name__ == '__main__':
    main()
