import json

from infra.reconf.util.handled import HandledDict, KeysHandler


class HandlerBaz(KeysHandler):
    @staticmethod
    def get_defaults():
        return {'baz': 'baz_val'}


class Baz(HandledDict):
    handlers = ('baz',)
    baz = HandlerBaz


class HandlerFoo(KeysHandler):
    @staticmethod
    def get_defaults():
        return {'foo': 'foo_val'}


class HandlerBar(KeysHandler):
    @staticmethod
    def get_defaults():
        return {'bar': Baz()}


class NestedFooBar(HandledDict):
    handlers = ('foo', 'bar')
    foo = HandlerFoo
    bar = HandlerBar


def test_json():
    assert '{"bar": {"baz": "baz_val"}, "foo": "foo_val"}' == \
        json.dumps(NestedFooBar(), sort_keys=True)


def test_repr():
    assert "NestedFooBar({'foo': 'foo_val', 'bar': Baz({'baz': 'baz_val'})})" == repr(NestedFooBar())
