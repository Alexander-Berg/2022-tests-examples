import contextlib
import gevent
import logging
import os
import py
import pyjack
import pytest
import random

from skybone_coord.main import SkyboneCoordDaemon, main


@pytest.fixture(scope='function')
def ctx(cov, cfg, tmpdir):
    class Ctx(object):
        pass

    ctx = Ctx()
    ctx.log = logging.getLogger('')
    ctx.cfg = cfg
    ctx.appdir = py.path.local(__file__).dirpath().dirpath()
    ctx.workdir = tmpdir

    return ctx


@pytest.fixture(scope='function')
def cfg():
    class cfg:
        class tracker:
            port = random.randint(1024, 65536)

            class announce:
                interval = (60, 120)
                interval_leech = (60, 120)
                return_peers = 25
                return_seeders_ratio = 0.12

        class web:
            port = random.randint(1024, 65536)

    return cfg


def test_coord_daemon(ctx):
    scd = SkyboneCoordDaemon(cfg=ctx.cfg, path=ctx.appdir, wpath=ctx.workdir, log=logging.getLogger(''))

    with pytest.raises(AssertionError):
        scd.stop()

    with gevent.Timeout(1):
        scd.join()

    scd.start()

    with pytest.raises(AssertionError):
        scd.start()

    with gevent.Timeout(0.01) as timeout:
        try:
            scd.join()
        except gevent.Timeout as ex:
            if ex is not timeout:
                raise
            pass
        else:
            assert 0, 'not raised'

    with gevent.Timeout(0.01):
        try:
            scd.stop()
        except gevent.Timeout as ex:
            assert 0

    with gevent.Timeout(0.01):
        scd.join()


class Exited(gevent.GreenletExit):
    def __init__(self, code):
        self.code = code


@pytest.fixture(scope='function')
def mockexit(request):
    def _exit(osexit, code):
        raise Exited(code)
    pyjack.connect(os._exit, _exit)
    request.addfinalizer(os._exit.restore)


class RunningApps(object):
    def __init__(self):
        self.active = set()

    def __call__(self, request):
        pyjack.connect(SkyboneCoordDaemon.start, self._start)
        pyjack.connect(SkyboneCoordDaemon.stop, self._stop)

        request.addfinalizer(self.finish)

    def _start(self, orig, app, *args, **kwargs):
        self.active.add(app)
        return orig(app, *args, **kwargs)

    def _stop(self, orig, app, *args, **kwargs):
        self.active.discard(app)
        return orig(app, *args, **kwargs)

    def finish(self):
        assert not self.active
        SkyboneCoordDaemon.start.restore
        SkyboneCoordDaemon.stop.restore


@pytest.fixture(scope='session')
def runapps(request):
    obj = RunningApps()
    obj(request)
    return obj


@pytest.fixture(scope='function')
def curgrn():
    return gevent.getcurrent()


@contextlib.contextmanager
def rungrn(meth, *args, **kwargs):
    grn = gevent.spawn(meth, *args, **kwargs)
    yield grn
    grn.kill()
    assert grn.ready()


def grn_killer(grn, ex):
    def _killer():
        gevent.sleep(0.01)
        grn.throw(ex)
    return _killer


def test_main_kill_by_ctrl_c(ctx, mockexit, runapps, curgrn):
    with rungrn(grn_killer(curgrn, KeyboardInterrupt)):
        with pytest.raises(Exited) as ex:
            main(ctx)
        assert ex.value.code == 0

    assert not runapps.active


#def test_main_kill_by_system_exit(ctx, mockexit, runapps, curgrn):
#    with rungrn(grn_killer(curgrn, SystemExit)):
#        with pytest.raises(SystemExit):
#            main(ctx)
#        assert len(runapps.active) == 1
#        runapps.active.pop().stop()


#def test_main_kill_by_system_exit_with_msg(ctx, mockexit, runapps, curgrn):
#    with rungrn(grn_killer(curgrn, SystemExit('ouch%s', '!'))):
#        with pytest.raises(SystemExit) as ex:
#            main(ctx)
#        assert ex.value.args == ('ouch%s', '!')
#        assert len(runapps.active) == 1
#        runapps.active.pop().stop()


#def test_main_kill_by_base_exception(ctx, mockexit, runapps, curgrn):
#    with rungrn(grn_killer(curgrn, BaseException('wtf'))):
#        with pytest.raises(Exited) as ex:
#            main(ctx)
#        assert ex.value.code == 1
#        assert len(runapps.active) == 1
#        runapps.active.pop().stop()


def nth_raise(n, ex):
    class Bomb(object):
        def __init__(self, n, ex):
            self.c = 0
            self.n = n
            self.ex = ex

        def __call__(self, orig, *args, **kwargs):
            self.c += 1
            if self.c == self.n:
                if isinstance(self.ex, BaseException):
                    raise self.ex
                elif isinstance(self.ex, (int, float)):
                    gevent.sleep(self.ex)
            else:
                return orig(*args, **kwargs)
    return Bomb(n, ex)


@pytest.fixture(scope='function')
def jack(request):
    class Jacky(object):
        def __init__(self):
            self.mocked = set()

        def connect(self, a, b):
            pyjack.connect(a, b)
            self.mocked.add(a)

        def noop(self, a):
            self.connect(a, lambda *a, **kw: None)

        def finish(self):
            for a in self.mocked:
                a.restore()

    jacky = Jacky()
    request.addfinalizer(jacky.finish)
    return jacky


#def test_main_app_final_join_fail(ctx, mockexit, runapps, jack):
#    jack.connect(SkyboneCoordDaemon.join, nth_raise(2, BaseException('oopsy')))
#    jack.noop(SkyboneCoordDaemon.start)
#    jack.noop(SkyboneCoordDaemon.stop)
#
#    with pytest.raises(Exited) as ex:
#        main(ctx)
#
#    assert ex.value.code == 1


def test_main_app_final_join_timeout(ctx, mockexit, runapps, jack):
    jack.connect(SkyboneCoordDaemon.join, nth_raise(2, 0.1))
    jack.noop(SkyboneCoordDaemon.start)
    jack.noop(SkyboneCoordDaemon.stop)

    with pytest.raises(Exited) as ex:
        main(ctx, fastexit=0.01)

    assert ex.value.code == 1
