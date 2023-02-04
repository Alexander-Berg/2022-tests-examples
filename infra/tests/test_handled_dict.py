import pytest

from infra.reconf.util.handled import HandledDict, KeysHandler, HandlerAbsentError


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


def test_init_no_args():
    data = FooBar()
    assert 'foo_val' == data['foo'] and 'bar_val' == data['bar']


def test_init_by_args():
    data = FooBar({'foo': 'redefined_foo_val'})
    assert 'redefined_foo_val' == data['foo']


def test_init_by_unregistered_args():
    with pytest.raises(HandlerAbsentError):
        FooBar({'unregistered': 'some_val'})


def test_init_via_default_handler():
    class DefaultHandler(KeysHandler):
        def get_handled_value(self, key, val):
            return val

    class GenericDict(HandledDict):
        default_handler = DefaultHandler

    data = GenericDict({'unregistered': 'some_val'})

    assert {'unregistered': 'some_val'} == data


def test_init_with_disabled_default_handler():
    class GenericDict(HandledDict):
        default_handler = None
        handlers = ('foo',)
        foo = HandlerFoo

    data = GenericDict({'foo': 'here', 'unregistered': 'some_val'})

    assert {'foo': 'here'} == data


def test_assign_value_to_registered_key():
    data = FooBar()
    data['foo'] = 'assigned_foo_val'
    assert {'bar': 'bar_val', 'foo': 'assigned_foo_val'} == data


def test_assign_value_to_unregistered_key():
    data = FooBar()
    with pytest.raises(HandlerAbsentError):
        data['unregistered'] = 'some_val'


def test_assign_value_via_default_handler():
    class DefaultHandler(KeysHandler):
        def get_handled_value(self, key, val):
            return val + '_modified_by_default_handler'

    class UnregFooBar(FooBar):
        default_handler = DefaultHandler

    data = UnregFooBar()

    data['unregistered'] = 'some_val'
    expected = {'bar': 'bar_val',
                'foo': 'foo_val',
                'unregistered': 'some_val_modified_by_default_handler'}
    assert expected == data


def test_clear():
    data = FooBar()
    data.clear()
    assert {} == data


def test_copy():
    data = FooBar()
    assert data == data.copy()


def test_get():
    data = FooBar()
    assert 'bar_val' == data.get('bar')


def test_del():
    data = FooBar()
    del data['bar']
    assert {'foo': 'foo_val'} == data


def test_pop():
    data = FooBar()
    assert 'foo_val' == data.pop('foo')
    assert {'bar': 'bar_val'} == data


def test_pop_absent():
    data = FooBar()
    with pytest.raises(KeyError):
        data.pop('not_exist')
    assert {'bar': 'bar_val', 'foo': 'foo_val'} == data


def test_popitem():
    data = FooBar()
    del data['foo']  # to leave just one item
    assert ('bar', 'bar_val') == data.popitem()
    assert {} == data


def test_fromkeys():
    data = FooBar.fromkeys(('foo', 'bar'), 'value')
    assert {'bar': 'value', 'foo': 'value'} == data


def test_fromkeys_unregistered():
    with pytest.raises(HandlerAbsentError):
        # baz key is not handled and should raise error
        FooBar.fromkeys(('foo', 'bar', 'baz'), 'value')


def test_items():
    assert [('bar', 'bar_val'), ('foo', 'foo_val')] == sorted(list(FooBar().items()))


def test_keys():
    assert ['bar', 'foo'] == sorted(list(FooBar().keys()))


def test_iter():
    assert ['bar', 'foo'] == sorted(list(iter(FooBar())))


def test_repr():
    assert "FooBar({'foo': 'foo_val', 'bar': 'bar_val'})" == repr(FooBar())


def test_setdefault_absent():
    data = FooBar()
    del data['foo']
    val = data.setdefault('foo', 'default_foo_val')
    assert val == 'default_foo_val'
    assert {'bar': 'bar_val', 'foo': 'default_foo_val'} == data


def test_setdefault_exist():
    data = FooBar()
    val = data.setdefault('foo', 'default_foo_val')
    assert val == 'foo_val'
    assert {'bar': 'bar_val', 'foo': 'foo_val'} == data


def test_update_from_dict():
    data = FooBar()
    data.update({'foo': 'updated_foo_val'})
    assert {'bar': 'bar_val', 'foo': 'updated_foo_val'} == data


def test_update_from_list():
    data = FooBar()
    data.update([['foo', 'updated_foo_val']])
    assert {'bar': 'bar_val', 'foo': 'updated_foo_val'} == data


def test_update_from_kwargs():
    data = FooBar()
    data.update(foo='updated_foo_val')
    assert {'bar': 'bar_val', 'foo': 'updated_foo_val'} == data


def test_update_unregistered():
    data = FooBar()
    with pytest.raises(HandlerAbsentError):
        data.update({'baz': 'baz_val'})


def test_values():
    assert ['bar_val', 'foo_val'] == sorted(list(FooBar().values()))
