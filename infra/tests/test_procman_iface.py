import os
import time
import signal
import unittest
import pytest

import gevent


from procman.server import Handler, watchdog
from procman.process import getenviron


def test_handler_interface(tmpdir):
    h = Handler(tmpdir)
    assert h.ping() is True
    assert h.stop() is None


def test_procs_interface_clean(tmpdir):
    h = Handler(tmpdir)
    with pytest.raises(Exception):
        h.findByPid(1)
    with pytest.raises(Exception):
        h.findByUUID(1)

    assert [i for i in h.findByTagsImpl(['test'])] == []
    assert [i for i in h.findByTagsEntryImpl(['test', 'other'])] == []
    assert [i for i in h.enumerateExecuting()] == []
    assert [i for i in h.enumerateRunning()] == []
    assert [i for i in h.enumerate()] == []
    assert [i for i in h.stats()] == []

    assert not h.is_valid('test_uuid')

    assert h.findByTags(['test']) == []
    assert h.findByTagsEntry(['test', 'other']) == []

    assert h.listTags() == []

    assert h.shutdown() is None


def test_proc_interface_nostart(tmpdir):
    h = Handler(tmpdir)

    uuid = h.create(
        None,  # fake pyro conn
        ['/bin/sleep', '1000'],
        env={'TEST': '1'},
        tags=['test', 'cat'],
        keeprunning=True,
        cwd='/',
        maxstreamlen=1000,
        afterlife=120
    )

    assert h.is_valid(uuid)

    stat = h.stat(None, uuid)
    good_stat = dict(
        uuid=uuid,
        stream=[],
        tags=['test', 'cat'],
        args=['/bin/sleep', '1000'],
        pid=None,
        cwd='/',
        start=time.time(),
        stoptime=None,
        retcodes=[])

    for k, v in good_stat.iteritems():
        if k == 'start':
            assert v > stat[k]
        else:
            assert stat[k] == v

    for key in getenviron():
        myvalue = os.getenv(key)
        if myvalue is not None:
            assert myvalue == stat['env'][key]

    assert stat['env']['TEST'] == '1'

    assert h.getTags(None, uuid) == frozenset(['test', 'cat'])
    h.addTags(None, uuid, ['one', 'two'])

    assert h.getTags(None, uuid) == frozenset(['test', 'cat', 'one', 'two'])
    assert h.listTags() == ['test', 'one', 'two', 'cat']

    assert h.findByTags(['one']) == [uuid]
    assert h.findByTags(['cat']) == [uuid]
    assert h.findByTags(['test', 'two']) == [uuid]
    assert h.findByTags(['one', 'two', 'three']) == [uuid]
    assert h.findByTags(['one'], excludeTags=['two']) == []
    assert h.findByTags(['super']) == []

    assert h.findByTagsEntry(['one', 'two']) == [uuid]
    assert h.findByTagsEntry(['one', 'two', 'three']) == []

    assert list(p.uuid for p in h.enumerateExecuting()) == []
    assert list(p.uuid for p in h.enumerateRunning()) == [uuid]

    assert h.running(None, uuid) is True  # I don't think it's good
    assert h.executing(None, uuid) is False

    assert h.signal(None, uuid, 'SIGTERM') is None
    assert h.signal(None, uuid, 'SIGTEM') is None

    h.kill(None, uuid)
    h.terminate(None, uuid)


def test_proc_interface_withstart(tmpdir):
    from gevent import sleep
    h = Handler(tmpdir)

    uuid = h.create(['/bin/sleep', '1000'])
    sleep(0.1)
    assert h.running(None, uuid) is True
    assert h.executing(None, uuid) is True

    assert h.find_by_pid(h.stat(None, uuid)['pid']) == uuid

    h.kill(None, uuid)


def test_unix_sock_watchdog(tmpdir):
    class Proc(object):
        def __init__(self):
            self.killed = False

        def stop(self):
            self.killed = True

    class ProcsMock(object):
        def __init__(self):
            self.procs = {}

        def enumerate_executing(self):  # noqa
            for i in self.procs.itervalues():
                yield i

    p = ProcsMock()
    for i in xrange(10):
        p.procs[i] = Proc()

    class Killer(object):
        def __init__(self):
            self.used_pid = None
            self.used_signum = None

        def kill(self, pid, signum):
            self.used_pid = pid
            self.used_signum = signum

    killer = Killer()
    sockfile = tmpdir.join('testfile')
    sockfile.ensure(file=1)

    try:
        ino = sockfile.stat().ino
        let = gevent.spawn(watchdog, ino, sockfile, p, timeout=0.1, kill=killer.kill)
        sockfile.remove()
        try:
            let.get(timeout=0.5)
        except gevent.Timeout:
            pass
        let.kill()
        # check
        assert killer.used_pid == os.getpid()
        assert killer.used_signum == signal.SIGKILL

        for proc in p.procs.itervalues():
            assert proc.killed is True
    finally:
        try:
            sockfile.remove()
        except EnvironmentError:
            pass


if __name__ == '__main__':
    unittest.main()
