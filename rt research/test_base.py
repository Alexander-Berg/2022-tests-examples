# coding=utf-8
import pytest

import ads.multik.pylib.key_value_store as kv


class SimpleKV(kv.KeyValueStore):
    def __init__(self, namespaces, keys_cols, data_cols):
        super(SimpleKV, self).__init__(namespaces, keys_cols, data_cols)

    def upsert(self, namespace, key, data):
        super(SimpleKV, self).upsert(namespace, key, data)

    def read(self, namespace, key, column_list=None):
        return super(SimpleKV, self).read(namespace, key, column_list)

    def update(self, namespace, key, data):
        super(SimpleKV, self).update(namespace, key, data)

    def delete(self, namespace, key):
        super(SimpleKV, self).delete(namespace, key)

    def init_store(self, *args):
        super(SimpleKV, self).init_store(*args)

    def connect_store(self):
        super(SimpleKV, self).connect_store()

    def create_store(self, namespace_types, key_types, data_types):
        super(SimpleKV, self).create_store(namespace_types, key_types, data_types)

    def drop_store(self):
        super(SimpleKV, self).drop_store()

    def files(self):
        return super(SimpleKV, self).files()

    def transaction(self):
        return super(SimpleKV, self).transaction()

    def filter(self, request=None):
        return
        yield


def test_base_kv_do_nothing():
    with pytest.raises(TypeError):
        SimpleKV([], [], [])
    with pytest.raises(TypeError):
        SimpleKV((None, ), [], [])

    with pytest.raises(TypeError):
        SimpleKV((), [], [])
    with pytest.raises(TypeError):
        SimpleKV((), (None,), [])

    with pytest.raises(TypeError):
        SimpleKV((), ('1', ), [])
    with pytest.raises(TypeError):
        SimpleKV((), ('1', ), (None,))

    with pytest.raises(ValueError):
        SimpleKV(('', ), (), ())
    with pytest.raises(ValueError):
        SimpleKV((), ('1', ), ())
    with pytest.raises(ValueError):
        SimpleKV((), (), ('1', ))
    with pytest.raises(ValueError):
        SimpleKV(('1', ), ('1', ), ())
    with pytest.raises(ValueError):
        SimpleKV(('1', ), (), ('1', ))
    with pytest.raises(ValueError):
        SimpleKV((), ('1', ), ('1', ))

    store = SimpleKV((), ('1', ), ('2', ))

    with pytest.raises(TypeError):
        store.validate_key([])
    with pytest.raises(ValueError):
        store.validate_key(('1', '2', ))

    with pytest.raises(TypeError):
        store.validate_namespace([])
    with pytest.raises(ValueError):
        store.validate_namespace(('1', '2', ))

    store.init_store()
    store.connect_store()

    with pytest.raises(ValueError):
        store.create_store((int, ), (), ())
    with pytest.raises(ValueError):
        store.create_store((), (), ())
    with pytest.raises(ValueError):
        store.create_store((), (int,), ())

    store.create_store((), (int, ), (int, ))
    store.drop_store()

    assert store.files() == []
    assert store.read((), (2, )) == {}

    assert store.dict() == {
        'class': 'SimpleKV',
        'module': '__tests__.test_base',
        'data_cols': ['2'],
        'keys_cols': ['1'],
        'namespaces': [],
    }
