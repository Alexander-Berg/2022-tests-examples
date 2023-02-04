import pytest
import time
from sepelib.gevent import GreenThread


class GreenTestThread(GreenThread):
    def __init__(self, f, args):
        super(GreenTestThread, self).__init__()
        self.run_func = f
        self.args = args

    def run(self):
        self.run_func(*self.args)


def wait_and_append(lst, el, t):
    time.sleep(t)
    lst.append(el)


def test_thread_wait():
    t = GreenThread()
    t.start()
    t.stop()
    with pytest.raises(RuntimeError):
        t.wait()


def test_ready():
    a = [0, ]
    t = GreenTestThread(wait_and_append, [a, 1, 0])
    t.start()
    t.wait()
    assert t.ready()
    assert len(a) == 2
    assert a[1] == 1


def test_kill():
    a = []
    t = GreenTestThread(wait_and_append, [a, 1, 100])
    t.kill()
    assert len(a) == 0
    assert t.ready()


def test_stop():
    a = []
    t = GreenTestThread(wait_and_append, [a, 1, 100])
    t.stop()
    assert len(a) == 0
    assert t.ready()


def test_not_implemented_run():
    t = GreenThread()
    with pytest.raises(NotImplementedError):
        t.run()
