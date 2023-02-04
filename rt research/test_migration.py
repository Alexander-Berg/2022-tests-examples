# coding=utf-8
import frozendict
import pytest
import uuid

import ads.multik.pylib.key_value_store as kv


NS_NAMES, KEY_NAMES, DATA_NAMES = ('prod', 'user', 'branch'), ('bid', 'key'), ('title', 'body', 'href')
NS_TYPES, KEY_TYPES, DATA_TYPES = (bool, int, str), (int, str), (str, str, str)


def fill_data(store):
    store.upsert((True,), (1, 'super_uniq_hash'), {'title': 'title', 'body': 'body', 'href': 'href'})
    store.upsert((True, 1), (1, 'super_uniq_hash'), {'title': 'user_1_title'})
    store.upsert((True, 1, 'title'), (1, 'super_uniq_hash'), {'title': 'user_branch_title'})
    store.upsert((True, 1, 'body'), (1, 'super_uniq_hash'), {'body': 'user_branch_body'})
    store.upsert((True, 2), (1, 'super_uniq_hash'), {'title': 'user_2_title'})
    store.upsert((True, 3), (1, 'super_uniq_hash'), {'body': 'user_3_body', 'href': 'user_3_href'})


def expected_data():
    return {
        frozendict.frozendict({'title': 'title', 'body': 'body', 'href': 'href', 'bid': 1, 'key': 'super_uniq_hash', 'prod': True, 'user': None, 'branch': None}),
        frozendict.frozendict({'title': 'user_1_title', 'body': None, 'href': None, 'bid': 1, 'key': 'super_uniq_hash', 'prod': True, 'user': 1, 'branch': None}),
        frozendict.frozendict({'title': 'user_2_title', 'body': None, 'href': None, 'bid': 1, 'key': 'super_uniq_hash', 'prod': True, 'user': 2, 'branch': None}),
        frozendict.frozendict({'title': 'user_branch_title', 'body': None, 'href': None, 'bid': 1, 'key': 'super_uniq_hash', 'prod': True, 'user': 1, 'branch': 'title'}),
        frozendict.frozendict({'title': None, 'body': 'user_branch_body', 'href': None, 'bid': 1, 'key': 'super_uniq_hash', 'prod': True, 'user': 1, 'branch': 'body'}),
        frozendict.frozendict({'title': None, 'body': 'user_3_body', 'href': 'user_3_href', 'bid': 1, 'key': 'super_uniq_hash', 'prod': True, 'user': 3, 'branch': None}),
    }


def test_memory_to_memory():
    mem_store = kv.MemoryKeyValueStore(NS_NAMES, KEY_NAMES, DATA_NAMES)
    fill_data(mem_store)
    new_mem = kv.MemoryKeyValueStore.migrate(mem_store, NS_TYPES, KEY_TYPES, DATA_TYPES, (None, ))
    assert set(frozendict.frozendict(r) for r in new_mem.filter()) == expected_data()


@pytest.mark.linux
def test_yt_to_memory(local_yt_client):
    dyn_store = kv.DynTableKeyValueStore(NS_NAMES, KEY_NAMES, DATA_NAMES)
    tbl = '//home/' + str(uuid.uuid4())

    dyn_store.init_store((tbl, local_yt_client))
    dyn_store.create_store(NS_TYPES, KEY_TYPES, DATA_TYPES)
    dyn_store.connect_store()
    fill_data(dyn_store)

    mem_store = kv.MemoryKeyValueStore.migrate(dyn_store, NS_TYPES, KEY_TYPES, DATA_TYPES, (None, ))
    assert set(frozendict.frozendict(r) for r in mem_store.filter()) == expected_data()


@pytest.mark.linux
def test_yt_to_yt(local_yt_client):
    dyn_store = kv.DynTableKeyValueStore(NS_NAMES, KEY_NAMES, DATA_NAMES)
    tbl = '//home/' + str(uuid.uuid4())

    dyn_store.init_store((tbl, local_yt_client))
    dyn_store.create_store(NS_TYPES, KEY_TYPES, DATA_TYPES)
    dyn_store.connect_store()
    fill_data(dyn_store)

    tbl = '//home/' + str(uuid.uuid4())

    new_dyn_store = kv.DynTableKeyValueStore.migrate(dyn_store, NS_TYPES, KEY_TYPES, DATA_TYPES, ((tbl, local_yt_client), ))
    assert set(frozendict.frozendict(r) for r in new_dyn_store.filter()) == expected_data()


@pytest.mark.linux
def test_memory_to_yt(local_yt_client):
    mem_store = kv.MemoryKeyValueStore(NS_NAMES, KEY_NAMES, DATA_NAMES)
    fill_data(mem_store)

    tbl = '//home/' + str(uuid.uuid4())

    dyn_store = kv.DynTableKeyValueStore.migrate(mem_store, NS_TYPES, KEY_TYPES, DATA_TYPES, ((tbl, local_yt_client), ))
    assert set(frozendict.frozendict(r) for r in dyn_store.filter()) == expected_data()
