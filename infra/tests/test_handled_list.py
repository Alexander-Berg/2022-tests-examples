import pytest

from infra.reconf.util.handled import Handler, HandledList


def test_init_wrong_args():
    with pytest.raises(TypeError, match='takes at most 1 argument'):
        HandledList(0, 1)


def test_init_using_args():
    l = HandledList((0, 1, 2))
    assert [0, 1, 2] == l


def test_init_using_defaults():
    class H(Handler):
        @staticmethod
        def get_defaults():
            return 0, 1

    class HL(HandledList):
        handler = H

    l = HL()
    assert [0, 1] == l


def test_default_append():
    l = HandledList()
    l.append(0)
    assert [0] == l


def test_default_extend():
    l = HandledList([0, 1])
    l.extend([2, 3])
    assert [0, 1, 2, 3] == l


def test_default_insert():
    l = HandledList([0])
    l.insert(0, 1)
    assert [1, 0] == l


def test_default_set():
    l = HandledList([4])
    l[0] = 1
    assert [1] == l


def test_handled_append():
    class H(Handler):
        def get_handled_value(self, key, val):
            return val + '!'

    class HL(HandledList):
        handler = H

    l = HL()
    l.append('foo')
    l.append('bar')
    assert ['foo!', 'bar!'] == l


def test_handled_extend():
    class H(Handler):
        def get_handled_value(self, key, val):
            return val - 1

    class HL(HandledList):
        handler = H

    l = HL([1])
    l.extend([2])
    assert [0, 1] == l


def test_handled_insert():
    class H(Handler):
        def get_handled_value(self, key, val):
            return val + 42

    class HL(HandledList):
        handler = H

    l = HL()
    l.insert(0, 1)
    l.insert(0, 0)
    assert [42, 43] == l


def test_handled_set():
    class H(Handler):
        def get_handled_value(self, key, val):
            return 'bar'

    class HL(HandledList):
        handler = H

    l = HL([None])
    l[0] = 'foo'
    assert ['bar'] == l


def test_repr():
    assert 'HandledList([0, 1, 2])' == repr(HandledList((0, 1, 2)))
