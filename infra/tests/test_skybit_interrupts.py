import gevent.event

from skybone.rbtorrent.skbn.interrupts import Interrupts


class ExampleException(Exception):
    pass


def test_interrupts():
    ev = Interrupts()
    interrupt_ev = gevent.event.Event()
    finish_ev = gevent.event.Event()

    def _test():
        try:
            with ev.interruptable([ExampleException]):
                interrupt_ev.set()
                finish_ev.wait()
        except ExampleException:
            pass
        else:
            assert 0, 'Not raised'

    #grns = [gevent.spawn(_test) for i in range(1000)]
    grn = gevent.spawn(_test)
    interrupt_ev.wait()
    ev.interrupt(ExampleException)
    finish_ev.set()
    #gevent.joinall(grns)
    grn.get()


def test_interrupts_before():
    ev = Interrupts()
    interrupt_ev = gevent.event.Event()
    finish_ev = gevent.event.Event()

    def _test():
        interrupt_ev.set()
        gevent.sleep()

        try:
            with ev.interruptable([ExampleException]):
                finish_ev.wait()
        except ExampleException:
            raise
            assert 0, 'Raised, but should not'

    #grn = gevent.spawn(_test)
    grns = [gevent.spawn(_test) for i in range(1000)]
    interrupt_ev.wait()
    ev.interrupt(ExampleException)
    finish_ev.set()
    gevent.joinall(grns)
    #grn.get()


def test_interrupts_multiple():
    ev = Interrupts()
    interrupt_ev = gevent.event.Event()
    finish_ev = gevent.event.Event()

    def _test():
        try:
            with ev.interruptable([ExampleException]):
                interrupt_ev.set()
                finish_ev.wait()
        except ExampleException:
            pass
        else:
            assert 0, 'Not raised'

    grn = gevent.spawn(_test)
    interrupt_ev.wait()

    for i in range(10):
        ev.interrupt(ExampleException)

    finish_ev.set()
    grn.get()


def test_interrupts_died():
    ev = Interrupts()

    interrupt_ev = gevent.event.Event()
    finish_ev = gevent.event.Event()

    def _test():
        try:
            with ev.interruptable([ExampleException]):
                interrupt_ev.set()
                finish_ev.wait()
                raise Exception('different')
        except ExampleException:
            raise
        except Exception as ex:
            assert str(ex) == 'different'
        else:
            assert 0, 'Not raised'

    grn = gevent.spawn(_test)
    interrupt_ev.wait()
    finish_ev.set()
    ev.interrupt(ExampleException)
    grn.get()


def test_interrupts_suspend():
    ev = Interrupts()

    interrupt_ev = gevent.event.Event()
    interrupt_ev2 = gevent.event.Event()
    finish_ev = gevent.event.Event()

    not_interrupted = [False]

    def _test():
        try:
            with ev.interruptable([ExampleException]) as interrupts:
                with interrupts.suspend():
                    interrupt_ev.set()
                    finish_ev.wait()
                    not_interrupted[0] = True
        except ExampleException:
            pass
        else:
            assert 0, 'Not interrupted'
        finally:
            interrupt_ev2.set()

    grn = gevent.spawn(_test)
    interrupt_ev.wait()
    ev.interrupt(ExampleException)
    finish_ev.set()
    interrupt_ev2.wait()
    grn.get()

    assert not_interrupted[0], 'Suspend not working'
