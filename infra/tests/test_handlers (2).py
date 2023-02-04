import pytest

from infra.reconf.util.handled import HandledDict, KeysHandler, HandlerConflictError


def test_conflicting_handlers():
    class H1(KeysHandler):
        @staticmethod
        def get_defaults():
            return {'foo': 1}

    class H2(KeysHandler):
        @staticmethod
        def get_defaults():
            return {'foo': 1, 'bar': 2}

    class D1(HandledDict):
        handlers = ('foo', 'bar')
        foo = H1
        bar = H2

    with pytest.raises(HandlerConflictError):
        D1()
