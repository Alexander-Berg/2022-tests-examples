import os
import sys
import time
import signal
import unittest
import subprocess
import pytest

from api.procman import RUNNING, FINISHED
from api.srvmngr import getServiceByName

PROCMAN_BIN = "snapshot/bin/procman"

if not os.path.exists(PROCMAN_BIN):
    raise RuntimeError("Can't find 'procman' executable")

scriptTemplate = r"""
import sys
import time

teststring = '{0}'
sys.stdout.write(teststring)
sys.stdout.flush()
time.sleep(0.1)
sys.stdout.write(teststring)
sys.stdout.flush()
"""


checkSigHup = r"""
import signal
import sys

ret = 1
if signal.getsignal(signal.SIGHUP) is signal.SIG_DFL:
    ret = 0

sys.exit(ret)
"""


@pytest.yield_fixture
def api(tmpdir, procman):
    cls = getServiceByName('procman').api()
    yield cls('PYRO:skynet.procman@./u:' + tmpdir.join('skynet.procman.sock').strpath)


@pytest.yield_fixture
def procman(tmpdir):
    from coverage.collector import Collector

    # ignore sighup to properly run test_sighup_handler
    signal.signal(signal.SIGHUP, signal.SIG_IGN)
    os.putenv('SKYNET_LOG_STDOUT', '1')
    args = [PROCMAN_BIN, '-l', '0', '--workdir', tmpdir.strpath, '--logdir', tmpdir.strpath]

    if Collector._collectors:
        args.append('--runcov')

    proc = subprocess.Popen(args)
    assert proc.pid is not None

    assert_in(lambda: tmpdir.join('skynet.procman.sock').check(exists=1), 1)

    yield proc

    pid = proc.pid
    if pid:
        try:
            os.kill(pid, 9)
        except OSError:
            pass
        proc.wait()


def assert_in(op, secs):
    deadline = time.time() + secs
    while time.time() < deadline:
        if op():
            return
        time.sleep(0.01)

    ret = op()
    assert ret


def test_client_connection(api):
    assert api.ping() is True


def test_process_creation(api):
    proxy = api.create(
        ['/bin/sleep', '10000'],
        keeprunning=False,
        afterlife=0
    )
    assert_in(lambda: proxy.executing() is True, 1)
    assert len(api.enumerate()) == 1
    proxy.kill()

    assert_in(lambda: len(api.enumerate()) == 0, 1)


def test_many_processes_creation(api):
    processes = []
    for i in xrange(50):
        proxy = api.create(
            ['/bin/sleep', '10000'],
            keeprunning=False,
            afterlife=0
        )
        assert_in(lambda: len(api.enumerate()) == i + 1, 0.1)
        processes.append(proxy)

    for proxy in processes:
        assert_in(lambda: proxy.executing() is True, 0.1)
        proxy.kill()

    for proxy in processes:
        assert_in(lambda: not api.is_valid(proxy.handle), 0.1)


def test_process_restart(api):
    proxy = api.create(['/bin/sleep', '100'], afterlife=1)
    assert_in(lambda: proxy.executing(), 0.5)

    proxy.kill()
    assert_in(lambda: not proxy.executing(), 0.5)
    assert not proxy.executing()
    assert proxy.status() is FINISHED

    assert_in(lambda: proxy.executing(), 12)
    assert proxy.executing()
    assert proxy.status() is RUNNING
    proxy.keepRunning(False)  # disable service

    proxy.kill()
    assert_in(lambda: not proxy.executing(), 0.5)
    assert proxy.status() is FINISHED


test_sighup_script = '''
import signal
import os
def _onsig(*args, **kwargs):
    open('killed_by_sighup', 'w')
    os._exit(1)
signal.signal(signal.SIGHUP, _onsig)
import time
open('ready', 'w')
time.sleep(100)
'''


def test_sighup_onstop(tmpdir, procman, api):
    testfile = tmpdir.join('test.py')
    testfile.write(test_sighup_script)

    proxy = api.create(
        [sys.executable, testfile.strpath],
        cwd=tmpdir.strpath,
        afterlife=1
    )
    assert_in(lambda: tmpdir.join('ready').check(exists=1, file=1), 1)

    childpid = proxy.stat()['pid']
    assert childpid is not None

    # stop procman
    api.stop()
    procman.wait()

    assert_in(lambda: tmpdir.join('killed_by_sighup').check(exists=1, file=1), 1)


def test_sighup_onsigkill(tmpdir, api, procman):
    testfile = tmpdir.join('test.py')
    testfile.write(test_sighup_script)

    proxy = api.create(
        [sys.executable, testfile.strpath],
        cwd=tmpdir.strpath,
        afterlife=1
    )
    assert_in(lambda: tmpdir.join('ready').check(exists=1, file=1), 1)

    childpid = proxy.stat()['pid']
    assert childpid is not None

    # stop procman
    os.kill(procman.pid, 9)
    procman.wait()

    assert_in(lambda: tmpdir.join('killed_by_sighup').check(exists=1, file=1), 1)


def test_child_stdout(tmpdir, api):
    teststring = 'a simple test string'

    proxy = api.create(
        [sys.executable, '-uc', "print('{}')".format(teststring)],
        keeprunning=False
    )

    assert_in(lambda: proxy.stat()['pid'] is None, 1)
    assert_in(lambda: proxy.stdout() == teststring + '\n', 0.5)

    # test proper ring buffer
    script = scriptTemplate.format(teststring)
    testfile = tmpdir.join('gen_child_stdout.py')
    testfile.write(script)

    proxy = api.create(
        [sys.executable, testfile.strpath],
        maxstreamlen=len(teststring),
        cwd=os.getcwd(),
        keeprunning=False
    )
    assert_in(lambda: proxy.stat()['pid'] is not None, 1)
    assert_in(lambda: proxy.stat()['stream'] == [(1, ('OUT', teststring))], 0.5)


def test_kill_not_existing_child(api):
    proxy = api.create([sys.executable, "-c", "import time; time.sleep(100)"])
    assert_in(lambda: proxy.stat()['pid'] is not None, 1)

    # a bit racy test: procman can call wait() between two calls to signal()
    proxy.signal(9)
    proxy.signal(9)


def test_sighup_handler(tmpdir, api):
    testfile = tmpdir.join('gen_child_sighup')
    testfile.write(checkSigHup)

    proxy = api.create(
        [sys.executable, testfile.strpath],
        cwd=os.getcwd(),
        keeprunning=False
    )
    assert_in(lambda: proxy.stat()['pid'] is not None, 1)
    assert_in(lambda: proxy.stat()['retcodes'] == ['exited with code: 0'], 1)


def test_process_tags(api):
    proxy = api.create(
        ['/bin/sleep', '10000'],
        tags=['sleep'],
        keeprunning=False,
        afterlife=0
    )
    assert_in(lambda: proxy.executing(), 1)

    assert proxy.getTags() == frozenset(['sleep'])
    proxy.addTags(['newTag'])
    assert proxy.getTags() == frozenset(['sleep', 'newTag'])
    assert len(api.findByTags(['sleep'])) == 1
    assert len(api.findByTags(['sleep'], excludeTags=['newTag'])) == 0

    proxy.kill()
    assert_in(lambda: len(api.enumerate()) == 0, 1)


if __name__ == '__main__':
    unittest.main()
