from __future__ import print_function

from ya.skynet.services.heartbeatserver.utils.triggers import Trigger


def test_basic():
    def parse(s):
        t = Trigger.parse(s)
        print('%r -> %r' % (s, t[0]))
        return t

    print('')
    for s in ['warn(1 < x > 3)', 'warn(3 > x < 1)', ' warn (3>x<1) ', 'warn(1<>3)', 'warn (  1   <  >  3  )  ']:
        for f in [parse, Trigger.fromString]:
            t = f(s)[0]
            assert t.name == 'warn' and t.min == 1 and t.max == 3
            assert not t.check(2) and not t.check(1) and not t.check(3) and t.check(0) and t.check(4)

    for s in ['warn(1<)', 'warn(1<x)', 'warn(>1)', 'warn(x>1)', 'warn(1<x)', ' warn (1  <  ) ', ' warn ( 1  <  x  ) ']:
        for f in [parse, Trigger.fromString]:
            t = f(s)[0]
            assert t.max == 1 and t.min is None
            assert not t.check(0) and not t.check(1) and t.check(2)

    for s in ['error(1>)', 'error(1>x)', 'error(<1)', 'error(x<1)', 'error(1>x)', ' error(1 > ) ', ' error( 1 > x )']:
        for f in [parse, Trigger.fromString]:
            t = f(s)[0]
            assert t.name == 'error' and t.min == 1 and t.max is None

    for s in ['warn(<1), error(>2)', ' warn(<1) error(>2) ', 'warn(<1), error(>2)', 'warn(<1)error(>2)']:
        t1, t2 = Trigger.fromString(s)
        print('%r -> %r, %r' % (s, t1, t2))
        assert t1.name == 'warn' and t1.min == 1 and t1.max is None
        assert t2.name == 'error' and t2.min is None and t2.max == 2


def test_negative():
    for s, chk in [
        ['foo(1 < x > 2)', 'Invalid trigger name'],
        ['warn(<>)', 'without any limits'],
        ['warn(>)', 'without any limits'],
        ['warn(<)', 'without any limits'],
        ['warn(1)', 'without any limits'],
        ['warn(xxx)', 'without any limits'],
        ['warn(1 < x > 3', 'Unterminated'],
        ['warn(1 < x < 3)', 'limit specified twice'],
        ['warn(1 > x > 3)', 'limit specified twice'],
        ['warn(1 < x > 1)', 'is greater or equal'],
        ['warn(2 < x > 1)', 'is greater or equal'],
    ]:
        try:
            Trigger.parse(s)
            assert False, 'This point should not be reached'
        except Exception as ex:
            print('%r parse - exception caught: %s' % (s, str(ex)))
            assert chk in str(ex)

    for s, chk in [
        ['error(<1), error(>2)', 'two triggers of the same name'],
        ['warn(<1), warn(>2)', 'two triggers of the same name'],
        ['warn(<1), warn(>2), error(>3)', 'More than two triggers'],
    ]:
        try:
            Trigger.fromString(s)
            assert False, 'This point should not be reached'
        except Exception as ex:
            print('%r parse - exception caught: %s' % (s, str(ex)))
            assert chk in str(ex)
