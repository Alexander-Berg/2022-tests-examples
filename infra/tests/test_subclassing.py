from infra.reconf.util.handled import HandledDict, KeysHandler


class HandlerFoo(KeysHandler):
    @staticmethod
    def get_defaults():
        return {'foo': 'foo_val'}


class HandlerBar(KeysHandler):
    @staticmethod
    def get_defaults():
        return {'bar': 'bar_val'}


class FooBar(HandledDict):
    handlers = ('foo', 'bar')
    foo = HandlerFoo
    bar = HandlerBar


class HandlerBarBaz(HandlerBar):
    @staticmethod
    def get_defaults():
        defaults = HandlerBar.get_defaults()
        defaults.update({'baz': 'baz_val'})

        return defaults


class FooBarBaz(FooBar):
    handlers = ('foo', 'bar')
    foo = HandlerFoo
    bar = HandlerBarBaz


def test_init():
    data = FooBarBaz()
    assert 'foo_val' == data['foo']
    assert 'bar_val' == data['bar']
    assert 'baz_val' == data['baz']


def test_repr():
    assert "FooBarBaz({'foo': 'foo_val', 'bar': 'bar_val', 'baz': 'baz_val'})" == repr(FooBarBaz())
