#!/usr/bin/env python

from __future__ import print_function

import os
import re
import sys
import time
import socket
import platform
import subprocess as sp

try:
    __import__('pkg_resources').require('msgpack')
except:
    __import__('pkg_resources').require('msgpack-python')
import msgpack


LINER = os.path.abspath('./bin/liner.debug')
MAGIC = -0x6E
PERIOD = .01
SERVICE_BODY = '''
from __future__ import print_function

import sys
import time
import datetime as dt


for i in range(120):
    print('Date: %s' % dt.datetime.now(), file=sys.stdout if i % 2 else sys.stderr)
    time.sleep({period})
sys.exit({magic})
'''
DAEMON_SERVICE_BODY = '''
from __future__ import print_function

import os
import sys
import time
import socket


sockname = sys.argv[1]
sleep_amount = int(sys.argv[2])

sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
sock.bind(sockname)
sock.listen(1)

peer = sock.accept()[0]
data = peer.recv(0x100)
print('Peer says: %r. Closing standard streams.' % data)
sys.stdout.flush()  # Arcadia's `huge_python` doesn't support '-u' option

nullfd = os.open(os.devnull, os.O_RDWR)
for s in [sys.stdin, sys.stdout, sys.stderr]:
    os.dup2(nullfd, s.fileno())

time.sleep(sleep_amount)
peer.send(data)
peer.close()
sock.close()
'''
ZOMBIE_DAEMON_SERVICE_BODY = '''
from __future__ import print_function

import os
import sys
import time


pid = os.fork()
if not pid:  # Child process
    # Take some sleep..
    time.sleep(1 << 26)  # .. for almost forever :)

# Parent process here - print child's PID and just exit.
print(pid)
sys.stdout.flush()  # Arcadia's `huge_python` doesn't support '-u' option
'''

PYTHON = [sys.executable, "-u"]
try:
    import yatest.common
    LINER = yatest.common.binary_path('skynet/packages/libraries/liner/liner')
    PYTHON = [yatest.common.python_path()]
except ImportError:
    pass


def read(sock):
    unpacker = msgpack.Unpacker()
    while True:
        buf = sock.recv(0x100)
        if not buf:
            raise Exception('Liner unexpectedly dies.')
        unpacker.feed(buf)
        for o in unpacker:
            yield o


def getProcessTitle(pid):
    if os.uname()[0].lower() == 'darwin':
        import psutil
        try:
            return psutil.Process(pid).cmdline[0]
        except:
            return ''

    try:
        with open('/proc/%d/cmdline' % pid) as f:
            return f.read(0x10000).split('\x00', 1)[0]
    except:
        return ''


def checkProcessTitle(pid, check=None):
    check = check and platform.system().lower() == "linux" and check

    for i in range(10) if check is not None else []:
        state = getProcessTitle(pid)
        print("[Check #%d] Process %d title: %r" % (i, pid, state))

        try:
            if check:
                for st in check:
                    assert re.search(r'\W%s\W' % st, state)
            else:
                assert not re.search(r'[\[\]]', state)
            break
        except AssertionError:
            if i == 9:
                raise
            time.sleep(0.1)


# Debug wrapper for function above.
def processTitlePrinter(pid, *args):
    checkProcessTitle(pid)


def _startLiner(cmd, ORPHAN_TIME_TO_LIVE=10, MAX_OUT_BUF_SIZE=265):
    sockname = cmd[-1]
    try:
        # Remove any socket files first.
        os.unlink(sockname)
    except os.error:
        pass

    env = os.environ.copy()
    env.update({'ORPHAN_TIME_TO_LIVE': str(ORPHAN_TIME_TO_LIVE), 'MAX_OUT_BUF_SIZE': str(MAX_OUT_BUF_SIZE)})
    print('Own process ID: %d, executing %r' % (os.getpid(), cmd))
    p = sp.Popen(cmd, env=env)
    checkProcessTitle(p.pid)

    print('Waiting for socket %r to be created' % sockname)
    for i in range(100):
        if os.path.exists(sockname):
            break
        time.sleep(.1)

    return p


def _waitForDeath(p):
    counter = 1000
    while True:
        corpse = counter < 0
        state = None if corpse else getProcessTitle(p.pid)
        if not counter % 10:
            state = state if state else getProcessTitle(p.pid)
            print(
                "[Death wait #%d] Process %d current title: %r" %
                (100 - abs(counter) / 10, p.pid, state)
            )

        if p.poll():
            break

        if not corpse and re.search(r'\Wcorpse\W', state):
            # Take additional time for the liner to kill sub-process
            print('Liner switched to "corpse" state.')
            counter = -1000

        counter -= 1 if not corpse else -1
        time.sleep(.01)
        if not counter:
            raise Exception('Liner%s did not terminated in reasonable time.' % (' [corpse]' if corpse else ''))
    p.wait()


def test_basic(tmpdir):
    os.chdir(str(tmpdir))
    SOCKET = 'liner_test_basic.sock'
    SERVICE = 'liner_test_basic.service'

    with open(SERVICE, 'w') as f:
        f.write(SERVICE_BODY.format(period=PERIOD, magic=MAGIC))

    cmd = [LINER, SOCKET]
    # cmd = ['valgrind', '--leak-check=full', '--show-reachable=yes'] + cmd
    # cmd = ['valgrind', '--leak-check=full', '--show-reachable=yes', '--db-attach=yes'] + cmd
    # cmd = ['gdb', '--args'] + cmd
    p = _startLiner(cmd)
    checkProcessTitle(p.pid, ['owum'])

    print('Connecting the socket.')
    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    sock.connect(SOCKET)

    print('Asking info.')
    sock.send(msgpack.dumps('info'))

    checkProcessTitle(p.pid, ['fetus'])
    print('Waiting for response.')
    reader = read(sock)
    data = reader.next()
    version = data['version']

    checkProcessTitle(p.pid)
    print('Data received: %r' % data)

    checkProcessTitle(p.pid)
    cmd = ['liner_test_service'] + PYTHON + [SERVICE]
    print('Executing service: %r' % cmd)
    sock.send(msgpack.dumps(cmd))

    checkProcessTitle(p.pid)  # processTitle(p.pid, ['infant'])  # The tool can quickly pass this state
    print('Waiting for PID')
    pid = reader.next()
    print('PID given: %d' % pid)

    # Check first output
    data = reader.next()
    print('Data received: %r' % repr(data))
    assert 'stderr' in data
    checkProcessTitle(p.pid, [])

    print('Setting the context and ask the state in one package ).')
    # The context data size should be greater then a socket read buffer in a liner.
    sock.send(
        msgpack.dumps(
            {'context': msgpack.dumps(
                {'MAGIC': MAGIC, 'FILLING': 'x' * 256}
            )}
        ) + msgpack.dumps('info')
    )
    for i in range(100):
        data = reader.next()
        print('[Check #%d] Data received: %r' % (i, repr(data)))
        if 'stdout' not in data and 'stderr' not in data:
            break
        if i == 99:
            raise Exception('Unable to fetch `info` response in 10 attempts.')

    assert data['pid'] == pid and data['name'] == cmd[0] and msgpack.loads(data['context'])['MAGIC'] == MAGIC

    print('Check `orphan` state.')
    sock.close()
    checkProcessTitle(p.pid, ['orphan'])

    sleep = 256 / 30 * PERIOD * 2
    print('Wait %.2g seconds for liner to go to `glutted` state' % sleep)
    time.sleep(sleep)
    checkProcessTitle(p.pid, ['glutted', 'orphan'])

    try:
        print('Reconnecting (1).')
        sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        sock.connect(SOCKET)
        checkProcessTitle(p.pid, ['glutted', 'adoptee'])

        print('Asking info (1).')
        sock.send(msgpack.dumps('info'))
        reader = read(sock)
        data = reader.next()
        print('Data received: %r' % repr(data))
        assert version == data['version']
        checkProcessTitle(p.pid, ['glutted', 'adoptee'])

        print('Disconnecting (2).')
        sock.close()
        checkProcessTitle(p.pid, ['glutted', 'orphan'])

        print('Reconnecting (2).')
        sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        sock.connect(SOCKET)
        checkProcessTitle(p.pid, ['glutted', 'adoptee'])

        print('Asking info (2).')
        sock.send(msgpack.dumps('info'))
        reader = read(sock)
        data = reader.next()
        print('Data received: %r' % repr(data))
        assert version == data['version'] and data['pid'] == pid
        assert data['name'] == cmd[0] and msgpack.loads(data['context'])['MAGIC'] == MAGIC
        time.sleep(0.1)

        print('Adopting.')
        sock.send(msgpack.dumps('adopt'))
    except:
        import traceback
        traceback.print_exc()
        p.wait()
        raise Exception('Liner unexpectedly died with code %r' % p.returncode)

    # Check next output
    data = reader.next()
    print('Data received: %r' % repr(data))
    assert 'stderr' in data
    checkProcessTitle(p.pid, [])

    for data in reader:
        print('Data received: %r' % repr(data))
        if isinstance(data, dict) and 'exited' in data:
            assert data['exited'] and not data['signaled'] and data['exitstatus'] == MAGIC
            break

    # The liner should still alive.
    os.kill(p.pid, 0)

    # Terminate the `liner` process gracefully.
    sock.send(msgpack.dumps('terminate'))


def test_negative(tmpdir):
    os.chdir(str(tmpdir))
    SOCKET = 'liner_test_negative.sock'
    SERVICE = 'liner_test_negative.service'

    with open(SERVICE, 'w') as f:
        f.write(SERVICE_BODY.format(period=PERIOD, magic=MAGIC))

    cmd = [LINER, SOCKET]
    p = _startLiner(cmd, ORPHAN_TIME_TO_LIVE=2)
    checkProcessTitle(p.pid, ['owum'])

    print('Connecting the socket.')
    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    sock.connect(SOCKET)

    print('Asking info.')
    sock.send(msgpack.dumps('info'))

    print('Emulating wrong version - disconnect it.')
    sock.close()
    _waitForDeath(p)

    p = _startLiner(cmd, ORPHAN_TIME_TO_LIVE=1)
    checkProcessTitle(p.pid, ['owum'])

    print('Connecting the socket.')
    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    sock.connect(SOCKET)

    print('Asking info.')
    sock.send(msgpack.dumps('info'))

    print('Waiting for response.')
    reader = read(sock)
    data = reader.next()

    checkProcessTitle(p.pid)
    print('Data received: %r' % data)

    checkProcessTitle(p.pid)
    subcmd = ['liner_test_service'] + PYTHON + [SERVICE]
    print('Asking to execute service: %r' % subcmd)
    sock.send(msgpack.dumps(subcmd))

    checkProcessTitle(p.pid)  # processTitle(p.pid, ['infant'])  # The tool can quickly pass this state
    print('Waiting for PID')
    pid = reader.next()
    print('PID given: %d' % pid)

    # Check first output
    data = reader.next()
    print('Data received: %r' % repr(data))
    assert 'stderr' in data
    checkProcessTitle(p.pid, [])

    print('Sending invalid command.')
    sock.send("InVaLiD cOmMaNd")
    # Now liner should close the connection ..
    for i in range(3):
        data = sock.recv(0x10000)
        if not data:
            break
    assert not data
    # .. and kill itself and the sub-process.
    _waitForDeath(p)

    p = _startLiner(cmd, ORPHAN_TIME_TO_LIVE=1)
    checkProcessTitle(p.pid, ['owum'])

    print('Connecting the socket.')
    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    sock.connect(SOCKET)

    print('Asking info.')
    sock.send(msgpack.dumps('info'))
    checkProcessTitle(p.pid, ['fetus'])

    print('Waiting for response.')
    reader = read(sock)
    data = reader.next()

    checkProcessTitle(p.pid)
    print('Data received: %r' % data)

    checkProcessTitle(p.pid)
    cmd = ['liner_test_service', '/nosuchrootdir/nosuchsubdir/nosuchfile']
    print('Asking to execute not existing service: %r' % cmd)
    sock.send(msgpack.dumps(cmd))

    checkProcessTitle(p.pid)  # processTitle(p.pid, ['infant'])  # The tool can quickly pass this state
    print('Waiting for PID')
    pid = reader.next()
    print('PID given: %d' % pid)

    # Check first output, it should contains an error message actually.
    data = reader.next()
    print('Data received: %r' % repr(data))
    assert 'stderr' in data

    # Now liner should sent an exit code.
    data = reader.next()
    print('Data received: %r' % repr(data))
    assert 'exited' in data

    # Terminate the `liner` process gracefully.
    sock.send(msgpack.dumps('terminate'))


def test_zombie_recall(tmpdir):  # O_o #
    os.chdir(str(tmpdir))
    SOCKET = 'liner_test_zombie_recall.sock'
    SERVICE = 'liner_test_zombie_recall.service'

    with open(SERVICE, 'w') as f:
        f.write(SERVICE_BODY.format(period=PERIOD, magic=MAGIC))

    cmd = [LINER, SOCKET]
    p = _startLiner(cmd, ORPHAN_TIME_TO_LIVE=4)
    checkProcessTitle(p.pid, ['owum'])

    print('Connecting the socket.')
    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    sock.connect(SOCKET)

    print('Asking info.')
    sock.send(msgpack.dumps('info'))

    print('Waiting for response.')
    reader = read(sock)
    data = reader.next()

    checkProcessTitle(p.pid)
    print('Data received: %r' % data)

    checkProcessTitle(p.pid)
    cmd = ['liner_test_service', '/bin/sleep', '1']
    print('Asking to execute service: %r' % cmd)
    sock.send(msgpack.dumps(cmd))

    checkProcessTitle(p.pid)  # processTitle(p.pid, ['infant'])  # The tool can quickly pass this state
    print('Waiting for PID')
    pid = reader.next()
    print('PID given: %d' % pid)

    print('Left the liner alone and wait for sub-process termination.')
    sock.close()
    time.sleep(1.5)
    checkProcessTitle(p.pid, ['orphan', 'zombie'])

    print('Reconnecting (1).')
    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    sock.connect(SOCKET)
    checkProcessTitle(p.pid, ['adoptee', 'zombie'])

    print('Asking info (1).')
    sock.send(msgpack.dumps('info'))
    reader = read(sock)
    data = reader.next()
    print('Data received: %r' % repr(data))
    checkProcessTitle(p.pid, ['adoptee', 'zombie'])

    print('Disconnecting (2).')
    sock.close()
    checkProcessTitle(p.pid, ['orphan', 'zombie'])

    print('Reconnecting (2).')
    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    sock.connect(SOCKET)
    checkProcessTitle(p.pid, ['adoptee', 'zombie'])

    print('Asking info (2).')
    sock.send(msgpack.dumps('info'))
    reader = read(sock)
    data = reader.next()
    print('Data received: %r' % repr(data))
    assert data['pid'] == pid and data['name'] == cmd[0]
    time.sleep(0.1)

    print('Adopting.')
    sock.send(msgpack.dumps('adopt'))

    # Now liner should sent an exit code.
    data = reader.next()
    print('Data received: %r' % repr(data))
    assert 'exited' in data

    # Terminate the `liner` process gracefully.
    sock.send(msgpack.dumps('terminate'))


def test_adopted_orphan_death(tmpdir):  # X_x #
    os.chdir(str(tmpdir))
    SOCKET = 'liner_test_adopted_orphan_death.sock'
    SERVICE = 'liner_test_adopted_orphan_death.service'

    with open(SERVICE, 'w') as f:
        f.write(SERVICE_BODY.format(period=PERIOD, magic=MAGIC))

    cmd = [LINER, SOCKET]
    p = _startLiner(cmd, ORPHAN_TIME_TO_LIVE=1)
    checkProcessTitle(p.pid, ['owum'])

    print('Check not fertilized egg with dead in a second.')
    time.sleep(1)
    _waitForDeath(p)

    p = _startLiner(cmd, ORPHAN_TIME_TO_LIVE=3)
    checkProcessTitle(p.pid, ['owum'])

    print('Connecting the socket.')
    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    sock.connect(SOCKET)

    print('Asking info.')
    sock.send(msgpack.dumps('info'))

    print('Waiting for response.')
    reader = read(sock)
    data = reader.next()

    checkProcessTitle(p.pid)
    print('Data received: %r' % data)

    checkProcessTitle(p.pid)
    cmd = ['liner_test_service'] + PYTHON + [SERVICE]
    print('Asking to execute service: %r' % cmd)
    sock.send(msgpack.dumps(cmd))

    checkProcessTitle(p.pid)  # processTitle(p.pid, ['infant'])  # The tool can quickly pass this state
    print('Waiting for PID')
    pid = reader.next()
    print('PID given: %d' % pid)

    # Check first output
    data = reader.next()
    print('Data received: %r' % repr(data))
    assert 'stderr' in data
    checkProcessTitle(p.pid, [])

    print('Check `orphan` state.')
    sock.close()
    checkProcessTitle(p.pid, ['orphan'])

    sleep = 256 / 30 * PERIOD * 2
    print('Wait %.2g seconds for liner to go to into `glutted` state.' % sleep)
    time.sleep(sleep)
    checkProcessTitle(p.pid, ['glutted', 'orphan'])

    print('Reconnecting (1).')
    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    sock.connect(SOCKET)
    checkProcessTitle(p.pid, ['glutted', 'adoptee'])

    print('Asking info (1).')
    sock.send(msgpack.dumps('info'))
    reader = read(sock)
    data = reader.next()
    print('Data received: %r' % repr(data))
    checkProcessTitle(p.pid, ['glutted', 'adoptee'])

    print('Disconnect it again and wait for orphan death by TTL.')
    sock.close()
    checkProcessTitle(p.pid, ['orphan'])
    time.sleep(2)
    checkProcessTitle(p.pid, ['glutted', 'orphan', 'zombie'])
    _waitForDeath(p)


def test_daemon_handle(tmpdir):
    os.chdir(str(tmpdir))
    SOCKET = 'liner_test_daemon_handle.sock'
    SERVICE = 'liner_test_daemon_handle.service'
    DAEMON_SOCKET = 'liner_test_daemon_handle.daemon_service.sock'

    with open(SERVICE, 'w') as f:
        f.write(DAEMON_SERVICE_BODY.format(period=PERIOD, magic=MAGIC))

    cmd = [LINER, SOCKET]
    # cmd = ['valgrind', '--leak-check=full', '--show-reachable=yes'] + cmd
    # cmd = ['valgrind', '--leak-check=full', '--show-reachable=yes', '--db-attach=yes'] + cmd
    # cmd = ['gdb', '--args'] + cmd
    # checkProcessTitle = processTitlePrinter  # DEBUG: Disable process title check.
    p = _startLiner(cmd, ORPHAN_TIME_TO_LIVE=1)
    checkProcessTitle(p.pid, ['owum'])

    print('Connecting the socket.')
    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    sock.connect(SOCKET)

    print('Asking info.')
    sock.send(msgpack.dumps('info'))

    checkProcessTitle(p.pid, ['fetus'])
    print('Waiting for response.')
    reader = read(sock)
    data = reader.next()

    checkProcessTitle(p.pid)
    print('Data received: %r' % data)

    checkProcessTitle(p.pid)
    try:
        os.unlink(DAEMON_SOCKET)
    except OSError:
        pass
    cmd = ['liner_test_service'] + PYTHON + [SERVICE, DAEMON_SOCKET, '1']
    print('Executing service: %r' % cmd)
    sock.send(msgpack.dumps(cmd))

    checkProcessTitle(p.pid)  # processTitle(p.pid, ['infant'])  # The tool can quickly pass this state
    print('Waiting for PID')
    pid = reader.next()
    print('PID given: %d' % pid)

    print('Connecting daemon socket.')
    for i in range(100):
        if os.path.exists(DAEMON_SOCKET):
            break
        if i == 99:
            data = reader.next()
            print('Data received: %r' % repr(data))
            raise Exception('Daemon did not created a socket in a reasonable time.')
        time.sleep(.01)

    daemon_sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    daemon_sock.connect(DAEMON_SOCKET)

    print('Sending a MAGIC to the daemon (it should close its standard stream as response).')
    daemon_sock.send(msgpack.dumps(MAGIC))

    # Check first output
    data = reader.next()
    print('Data received: %r' % repr(data))
    assert 'stdout' in data

    print('Now it should sleep for a second. Read MAGIC after it.')
    time.sleep(1)

    assert MAGIC == msgpack.loads(daemon_sock.recv(0x100))

    # Now liner should sent an exit code in a reasonable time.
    daemon_sock.settimeout(1)
    while True:
        data = reader.next()
        print('Data received: %r' % repr(data))
        if 'stdout' not in data:
            break
    assert data['exited'] == 1 and data['exitstatus'] == 0

    # Terminate the `liner` process gracefully.
    sock.send(msgpack.dumps('terminate'))


def test_zombie_daemon(tmpdir):
    os.chdir(str(tmpdir))
    SOCKET = 'liner_test_zombie_daemon.sock'
    SERVICE = 'liner_test_zombie_daemon.service'

    with open(SERVICE, 'w') as f:
        f.write(ZOMBIE_DAEMON_SERVICE_BODY.format(period=PERIOD, magic=MAGIC))

    cmd = [LINER, SOCKET]
    p = _startLiner(cmd, ORPHAN_TIME_TO_LIVE=1)
    checkProcessTitle(p.pid, ['owum'])

    print('Connecting the socket.')
    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    sock.connect(SOCKET)

    print('Asking info.')
    sock.send(msgpack.dumps('info'))

    checkProcessTitle(p.pid, ['fetus'])
    print('Waiting for response.')
    reader = read(sock)
    data = reader.next()

    checkProcessTitle(p.pid)
    print('Data received: %r' % data)

    cmd = ['liner_test_service'] + PYTHON + [SERVICE]
    print('Executing service: %r' % cmd)
    sock.send(msgpack.dumps(cmd))

    checkProcessTitle(p.pid)  # processTitle(p.pid, ['infant'])  # The tool can quickly pass this state
    print('Waiting for PID')
    pid = reader.next()
    print('PID given: %d' % pid)

    # Check first output
    data = reader.next()
    print('Data received: %r' % repr(data))
    assert 'stdout' in data

    # It should be child's PID
    cpid = int(data['stdout'].strip())

    print(
        'Now the daemon should terminate, while '
        'liner should send its exit code in a reasonable time.'
    )
    while True:
        data = reader.next()
        print('Data received: %r' % repr(data))
        if 'stdout' not in data:
            break
    assert data['exited'] == 1 and data['exitstatus'] == 0 and data['zombie'] == 1

    print('The liner should still alive.')
    os.kill(p.pid, 0)

    print('While the daemon - not.')
    try:
        os.kill(pid, 0)
        assert False, 'Daemon (PID %r) still ALIVE!' % pid
    except OSError:
        pass

    print("And the daemon's child should still alive too.")
    os.kill(cpid, 0)

    print("Now terminate the `liner`'s process group completely.")
    sock.send(msgpack.dumps('terminatepg'))

    _waitForDeath(p)
    time.sleep(.1)

    print('Check for corpses amount.')

    def _still_running(p):
        try:
            return bool(os.kill((p[1] if isinstance(p, tuple) else p), 0) or 1)
        except OSError:
            return False

    tick = .01
    total_wait = 0
    while total_wait < 3:
        if not all(map(_still_running, (p.pid, pid, cpid))):
            print("All processes are dead.")
            break
        print("Some process(es) are running - sleeping %.2f and checking again." % (tick,))
        time.sleep(tick)
        total_wait += tick
        tick = min(tick * 2, 1)
    else:
        name, p = next(iter(map(_still_running, (("Liner", p.pid), ("Daemon", pid), ("Child", cpid)))), (None, None))
        if name:
            assert False, "%s (PID %r) still ALIVE!" % (name, p)
