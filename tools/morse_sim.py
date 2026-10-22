#!/usr/bin/env python3
"""Morse group simulator: fake watches on your PC that a real emulator or watch can join.

Acts as the group host on TCP 8765 using the app's own protocol (newline-delimited JSON,
see app/src/main/kotlin/com/morse/wearos/transport/Protocol.kt). The host and extra members
are bots that answer every message, so one emulator gets a full group to talk to.

    python3 tools/morse_sim.py                 # host "Ana" + bot "Luis", random code
    python3 tools/morse_sim.py --code 1234 --bots Ana    # leaves 2 slots for emulators
    python3 tools/morse_sim.py --selftest

On the emulator: Settings > Connection > LAN / emulator, Host address 10.0.2.2 (default),
then Join group with the printed code. 10.0.2.2 is this PC, so no adb forward is needed.

Type at the prompt to make a bot talk:  "luis: on my way"  (or just "hola" for the host bot).
Commands: /members  /quit
Stdlib only.
"""
import argparse
import json
import random
import socket
import sys
import threading
import time
import uuid
from collections import deque

PORT = 8765
MAX_MEMBERS = 3  # GroupConfig.MAX_MEMBERS
MAX_NAME_LENGTH = 20

TABLE = {
    'A': '.-', 'B': '-...', 'C': '-.-.', 'D': '-..', 'E': '.', 'F': '..-.', 'G': '--.', 'H': '....',
    'I': '..', 'J': '.---', 'K': '-.-', 'L': '.-..', 'M': '--', 'N': '-.', 'O': '---', 'P': '.--.',
    'Q': '--.-', 'R': '.-.', 'S': '...', 'T': '-', 'U': '..-', 'V': '...-', 'W': '.--', 'X': '-..-',
    'Y': '-.--', 'Z': '--..',
    '0': '-----', '1': '.----', '2': '..---', '3': '...--', '4': '....-',
    '5': '.....', '6': '-....', '7': '--...', '8': '---..', '9': '----.',
    'Ñ': '--.--',
}
ACCENTS = {'Á': 'A', 'É': 'E', 'Í': 'I', 'Ó': 'O', 'Ú': 'U', 'Ü': 'U'}


def encode(text):
    """Same output as MorseCodec.encode."""
    words = text.strip().upper().split()
    return ' / '.join(
        ' '.join(TABLE[ACCENTS.get(c, c)] for c in w if ACCENTS.get(c, c) in TABLE) for w in words
    )


def bot_reply(word):
    """Keyword replies, mirroring MorseAssistant.offlineReplies."""
    w = ' ' + ''.join(c if c.isalnum() or c in '?¿' else ' ' for c in word.lower()) + ' '
    has = lambda *keys: any(f' {k} ' in w or f' {k}?' in w for k in keys)
    es = any(c in word.lower() for c in 'ñáéíóú¿') or has('hola', 'si', 'donde', 'espera', 'ayuda', 'vienes')
    if has('sos', 'help', 'ayuda', 'auxilio'):
        return 'VOY' if es else 'ON MY WAY'
    if has('where', 'donde', 'dónde'):
        return random.choice(['AQUI ESTOY', 'CASA']) if es else random.choice(["I'M HERE", 'HOME'])
    if '?' in w or '¿' in w:
        return random.choice(['SI', 'NO', 'QUIZAS'] if es else ['YES', 'NO', 'MAYBE'])
    return random.choice(['OK', 'GRACIAS', 'ESPERA'] if es else ['OK', 'THANKS', 'WAIT'])


def member(mid, name, is_host=False):
    return {'id': mid, 'name': name, 'isHost': is_host}


def message(sender, word):
    return {
        'id': str(uuid.uuid4()), 'senderId': sender['id'], 'senderName': sender['name'],
        'word': word, 'morse': encode(word), 'timestampMs': int(time.time() * 1000),
    }


class Host:
    def __init__(self, code, bot_names):
        self.code = code
        self.bots = [member(f'pc-bot-{i}', n[:MAX_NAME_LENGTH], i == 0) for i, n in enumerate(bot_names)]
        self.clients = {}  # member id -> (member, socket)
        self.lock = threading.Lock()
        self.auto_reply = True
        self.log = deque(maxlen=200)  # every message seen, newest last; read by morse_panel.py

    def members(self):
        with self.lock:
            return self.bots + [m for m, _ in self.clients.values()]

    def send(self, sock, envelope):
        try:
            sock.sendall((json.dumps(envelope, ensure_ascii=False) + '\n').encode())
        except OSError:
            pass

    def broadcast(self, envelope, except_id=None):
        with self.lock:
            targets = [s for mid, (_, s) in self.clients.items() if mid != except_id]
        for s in targets:
            self.send(s, envelope)

    def bot_says(self, bot, word):
        msg = message(bot, word)
        print(f'  [{bot["name"]}] {word}   {msg["morse"]}')
        self.log.append(msg)
        self.broadcast({'type': 'message', 'message': msg})

    def serve(self):
        server = socket.socket()
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind(('0.0.0.0', PORT))
        server.listen()
        while True:
            conn, addr = server.accept()
            threading.Thread(target=self.handle, args=(conn, addr), daemon=True).start()

    def handle(self, conn, addr):
        reader = conn.makefile('r', encoding='utf-8')
        hello = json.loads(reader.readline() or 'null')
        if not hello or hello.get('type') != 'hello':
            conn.close()
            return
        if hello['code'] != self.code:
            print(f'  ! {addr[0]} tried wrong code {hello["code"]}')
            self.send(conn, {'type': 'rejected', 'reason': 'WRONG_CODE'})
            conn.close()
            return
        m = hello['member']
        joiner = member(m['id'], ' '.join(m['name'].split())[:MAX_NAME_LENGTH] or '?')
        with self.lock:
            self.clients.pop(joiner['id'], None)
            full = len(self.bots) + len(self.clients) >= MAX_MEMBERS
            if not full:
                self.clients[joiner['id']] = (joiner, conn)
        if full:
            print(f'  ! {joiner["name"]} rejected: group full')
            self.send(conn, {'type': 'rejected', 'reason': 'GROUP_FULL'})
            conn.close()
            return
        current = self.members()
        self.send(conn, {'type': 'welcome', 'code': self.code, 'members': current})
        self.broadcast({'type': 'members', 'members': current}, except_id=joiner['id'])
        print(f'+ {joiner["name"]} joined from {addr[0]}')
        try:
            for line in reader:
                env = json.loads(line)
                if env['type'] == 'bye':
                    break
                if env['type'] == 'message':
                    msg = env['message']
                    print(f'  [{msg["senderName"]}] {msg["word"]}   {msg["morse"]}')
                    self.log.append(msg)
                    self.broadcast(env, except_id=joiner['id'])
                    if self.bots and self.auto_reply:
                        threading.Timer(random.uniform(2, 4), self.bot_says,
                                        args=(random.choice(self.bots), bot_reply(msg['word']))).start()
        except (OSError, ValueError):
            pass
        finally:
            with self.lock:
                self.clients.pop(joiner['id'], None)
            conn.close()
            print(f'- {joiner["name"]} left')
            self.broadcast({'type': 'members', 'members': self.members()})


def selftest():
    assert encode('SOS') == '... --- ...'
    assert encode("  I'm   here ") == '.. -- / .... . .-. .'
    assert encode('Sí') == encode('SI')
    assert encode('año') == '.- --.-- ---'
    assert encode('12') == '.---- ..---'
    assert bot_reply('Where are you') in ("I'M HERE", 'HOME')
    assert bot_reply('SOS') == 'ON MY WAY'
    assert bot_reply('¿Vienes?') in ('SI', 'NO', 'QUIZAS')
    env = {'type': 'message', 'message': message(member('x', 'Ana'), 'yes')}
    line = json.dumps(env, ensure_ascii=False)
    assert '\n' not in line and json.loads(line) == env and env['message']['morse'] == '-.-- . ...'
    print('selftest ok')


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--code', default=''.join(random.choice('0123456789') for _ in range(4)))
    p.add_argument('--bots', default='Ana,Luis', help='comma-separated; the first is the host')
    p.add_argument('--selftest', action='store_true')
    args = p.parse_args()
    if args.selftest:
        return selftest()
    sys.stdout.reconfigure(line_buffering=True)  # keep the log live when piped to a file

    names = [n.strip() for n in args.bots.split(',') if n.strip()][:MAX_MEMBERS - 1]
    if not names:
        p.error('need at least one bot: the first one is the group host')
    host = Host(args.code, names)
    threading.Thread(target=host.serve, daemon=True).start()
    print(f'Morse simulator on port {PORT}   ACCESS CODE: {args.code}')
    print(f'Bots: {", ".join(b["name"] for b in host.bots)} ({MAX_MEMBERS - len(host.bots)} free slot(s))')
    print('Emulator: Settings > Connection > LAN, host 10.0.2.2, then Join with the code above.\n')

    by_name = {b['name'].lower(): b for b in host.bots}
    try:
        while True:
            line = input().strip()
            if line == '/quit':
                break
            if line == '/members':
                for m in host.members():
                    print(f'  {"*" if m["isHost"] else " "} {m["name"]}')
                continue
            if not line:
                continue
            name, sep, word = line.partition(':')
            bot = by_name.get(name.strip().lower()) if sep else None
            host.bot_says(bot or host.bots[0], (word if bot else line).strip()[:20])
    except (EOFError, KeyboardInterrupt):
        pass
    host.broadcast({'type': 'bye', 'memberId': host.bots[0]['id']})


if __name__ == '__main__':
    main()
