from __future__ import print_function
import pytest
import pyjack
import sys

from skybone_coord.daemon import context


def test_simple():
    ctx = context.Context()
    ctx.initialize()
    with pytest.raises(AssertionError):
        ctx.initialize()
    assert repr(ctx) == '<Context>'


def test_print_hack():
    try:
        ctx = context.Context()
        ctx.initialize(print_hack=True)
        assert print == context.magicPrint

        ctx2 = context.Context()
        ctx2.initialize(print_hack=True)
        assert print == context.magicPrint
    finally:
        if 'oprint' in __builtins__:
            __builtins__['print'] = __builtins__['oprint']
            del __builtins__['oprint']


def test_logging_setup():
    import logging
    from skybone_coord.daemon import logger

    try:
        res = []

        def fake_setup(xself, progname, console_verbosity):
            res.append((progname, console_verbosity))

        pyjack.connect(logger.setup_logging, fake_setup)

        ctx = context.Context()
        assert ctx.log is None
        ctx.initialize(logging=True)
        assert ctx.log == logging.getLogger('')
        assert res[0][0] is None

        ctx = context.Context()
        ctx.initialize(progname='xyxy', logging=True, logging_verbosity=42)
        assert ctx.log == logging.getLogger('')
        assert res[1] == ('xyxy', 42)
    finally:
        logger.setup_logging.restore()


@pytest.fixture(scope='function')
def dtx(request):
    import daemon
    import gevent
    import faulthandler

    class GSig(dict):
        def signal(self, osignal, signum, handler, *args):
            self[signum] = (handler, args)

    class FH(object):
        def __init__(self):
            self._enabled = False

        def enable(self, orig):
            self._enabled = True

    class DTX(object):
        gsig = GSig()
        fh = FH()

        def __init__(self, oclass, **kwargs):
            self.__dict__.update(kwargs)
            self.opened = False

        def open(self):
            self._opened = True

        def close(self):
            self._opened = False

        def _make_signal_handler_map(self):
            return {-10: 1, -11: 'fake'}

    pyjack.connect(daemon.DaemonContext, DTX)
    pyjack.connect(gevent.signal, DTX.gsig.signal)
    pyjack.connect(faulthandler.enable, DTX.fh.enable)

    request.addfinalizer(lambda: (
        daemon.DaemonContext.restore(),
        gevent.signal.restore(),
        faulthandler.enable.restore(),
    ))


def test_open(dtx):
    import signal
    import daemon

    ctx = context.Context()
    ctx.initialize()

    with ctx:
        assert ctx._dctx.working_directory == '/'
        assert ctx._dctx.umask == 022
        assert ctx._dctx.stdout is sys.stdout
        assert ctx._dctx.stderr is sys.stderr
        assert ctx._dctx.stdin is sys.stdin
        assert ctx._dctx.prevent_core is False
        assert ctx._dctx.detach_process is False
        assert ctx._dctx.signal_map == {
            signal.SIGPIPE: None,
            signal.SIGTTOU: None,
            signal.SIGTTIN: None,
            signal.SIGTERM: ctx.terminate,
            signal.SIGALRM: ctx.terminate,
            signal.SIGHUP: ctx.terminate,
            signal.SIGTSTP: ctx.terminate
        }
        assert daemon.daemon.set_signal_handlers(42) is None
        assert ctx._dctx.gsig[-10][1] == (-10, )
        assert ctx._dctx.gsig[-10][0](42) is None
        assert ctx._dctx.gsig[-11] == ('fake', (-11, ))
        assert ctx._dctx._opened
        assert ctx._dctx.fh._enabled is True


def test_terminate():
    ctx = context.Context()
    with pytest.raises(SystemExit) as ex:
        ctx.terminate(9)
    assert str(ex.value) == 'Terminating on signal 9 (SIGKILL)'

    with pytest.raises(SystemExit) as ex:
        ctx.terminate(42)
    assert str(ex.value) == 'Terminating on signal 42 (unknown: not found)'


def test_setproctitle(dtx):
    try:
        orig = context.setproctitle

        context.setproctitle = None
        ctx = context.Context()
        ctx.initialize(progname='asd')

        with ctx:
            pass

        res = []

        def setproctitle(title):
            res.append(title)

        context.setproctitle = setproctitle
        ctx = context.Context()
        ctx.initialize(progname='dsa')

        with ctx:
            assert res == ['dsa']
    finally:
        context.setproctitle = orig


def test_os_not_support_some_signals():
    import signal
    sigpipe = signal.SIGPIPE
    sigterm = signal.SIGTERM

    import os
    print(os.environ.get('COVERAGE_PROCESS_START'))
    try:
        del signal.SIGPIPE
        del signal.SIGTERM
        ctx = context.Context()
        ctx.initialize()
        assert ctx._dctx.signal_map
        assert sigpipe not in ctx._dctx.signal_map
        assert sigterm not in ctx._dctx.signal_map
    finally:
        signal.SIGPIPE = sigpipe
        signal.SIGTERM = sigterm
