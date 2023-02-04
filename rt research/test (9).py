# coding=utf-8
import frozendict
import pytest
import uuid

import ads.multik.pylib.key_value_store as kv


def run_store_test(store, *init_args):
    store.init_store(*init_args)

    store.create_store((bool, int, str), (int, str), (str, str, str))
    store.connect_store()

    store.upsert((True,), (1, 'super_uniq_hash'), {'title': 'title', 'body': 'body', 'href': 'href'})
    store.upsert((True, 1), (1, 'super_uniq_hash'), {'title': 'user_1_title'})
    store.upsert((True, 1, 'title'), (1, 'super_uniq_hash'), {'title': 'user_branch_title'})
    store.upsert((True, 1, 'body'), (1, 'super_uniq_hash'), {'body': 'user_branch_body'})
    store.upsert((True, 2), (1, 'super_uniq_hash'), {'title': 'user_2_title'})
    store.upsert((True, 3), (1, 'super_uniq_hash'), {'body': 'user_3_body', 'href': 'user_3_href'})

    assert store.read((True,), (1, 'super_uniq_hash')) == {'prod': True, 'user': None, 'branch': None, 'bid': 1, 'key': 'super_uniq_hash',
                                                           'title': 'title', 'body': 'body', 'href': 'href'}
    assert store.read((True, 1), (1, 'super_uniq_hash')) == {'prod': True, 'user': 1, 'branch': None, 'bid': 1, 'key': 'super_uniq_hash',
                                                             'title': 'user_1_title', 'body': 'body', 'href': 'href'}
    assert store.read((True, 1, 'title'), (1, 'super_uniq_hash')) == {'prod': True, 'user': 1, 'branch': 'title', 'bid': 1, 'key': 'super_uniq_hash',
                                                                      'title': 'user_branch_title', 'body': 'body', 'href': 'href'}
    assert store.read((True, 1, 'body'), (1, 'super_uniq_hash')) == {'prod': True, 'user': 1, 'branch': 'body', 'bid': 1, 'key': 'super_uniq_hash',
                                                                     'title': 'user_1_title', 'body': 'user_branch_body', 'href': 'href'}
    assert store.read((True, 2), (1, 'super_uniq_hash')) == {'prod': True, 'user': 2, 'branch': None, 'bid': 1, 'key': 'super_uniq_hash',
                                                             'title': 'user_2_title', 'body': 'body', 'href': 'href'}
    assert store.read((True, 3), (1, 'super_uniq_hash')) == {'prod': True, 'user': 3, 'branch': None, 'bid': 1, 'key': 'super_uniq_hash',
                                                             'title': 'title', 'body': 'user_3_body', 'href': 'user_3_href'}

    store.upsert((True, 3), (1, 'super_uniq_hash'), {'body': 'create', 'href': 'user_3_href'})
    assert store.read((True, 3), (1, 'super_uniq_hash')) == {'prod': True, 'user': 3, 'branch': None, 'bid': 1, 'key': 'super_uniq_hash',
                                                             'title': 'title', 'body': 'create', 'href': 'user_3_href'}

    store.update((True, 3), (1, 'super_uniq_hash'), {'body': 'update'})
    assert store.read((True, 3), (1, 'super_uniq_hash')) == {'prod': True, 'user': 3, 'branch': None, 'bid': 1, 'key': 'super_uniq_hash',
                                                             'title': 'title', 'body': 'update', 'href': 'user_3_href'}

    assert store.read((True,), (1, 'super_uniq_hash')) == {'prod': True, 'user': None, 'branch': None, 'bid': 1, 'key': 'super_uniq_hash',
                                                           'title': 'title', 'body': 'body', 'href': 'href'}

    assert store.delete((True,), (1, 'super_uniq_hash')) == {'prod': True, 'user': None, 'branch': None, 'bid': 1, 'key': 'super_uniq_hash',
                                                             'title': 'title', 'body': 'body', 'href': 'href'}
    assert store.read((True, 1), (1, 'super_uniq_hash')) == {'prod': True, 'user': 1, 'branch': None, 'bid': 1, 'key': 'super_uniq_hash',
                                                             'title': 'user_1_title'}

    store.upsert((False, ), (100, 'uniq_hash'), {'body': 'body', 'href': 'href'})
    store.upsert((False, 1), (100, 'uniq_hash'), {'body': 'body1', 'href': 'href1'})
    store.upsert((False, 2), (100, 'uniq_hash'), {'body': 'body2', 'href': 'href2'})

    assert set(frozendict.frozendict(x) for x in store.filter()) == {
        frozendict.frozendict({'title': 'user_1_title', 'body': None, 'href': None, 'bid': 1, 'key': 'super_uniq_hash', 'prod': True, 'user': 1, 'branch': None}),
        frozendict.frozendict({'title': 'user_2_title', 'body': None, 'href': None, 'bid': 1, 'key': 'super_uniq_hash', 'prod': True, 'user': 2, 'branch': None}),
        frozendict.frozendict({'title': 'user_branch_title', 'body': None, 'href': None, 'bid': 1, 'key': 'super_uniq_hash', 'prod': True, 'user': 1, 'branch': 'title'}),
        frozendict.frozendict({'title': None, 'body': 'user_branch_body', 'href': None, 'bid': 1, 'key': 'super_uniq_hash', 'prod': True, 'user': 1, 'branch': 'body'}),
        frozendict.frozendict({'title': None, 'body': 'update', 'href': 'user_3_href', 'bid': 1, 'key': 'super_uniq_hash', 'prod': True, 'user': 3, 'branch': None}),

        frozendict.frozendict({'title': None, 'body': 'body2', 'href': 'href2', 'bid': 100, 'key': 'uniq_hash', 'prod': False, 'user': 2, 'branch': None}),
        frozendict.frozendict({'title': None, 'body': 'body1', 'href': 'href1', 'bid': 100, 'key': 'uniq_hash', 'prod': False, 'user': 1, 'branch': None}),
        frozendict.frozendict({'title': None, 'body': 'body', 'href': 'href', 'bid': 100, 'key': 'uniq_hash', 'prod': False, 'user': None, 'branch': None}),
    }
    assert set(frozendict.frozendict(x) for x in store.filter(store.Request([], []))) == set()
    assert set(frozendict.frozendict(x) for x in store.filter(store.Request([], [], ('title', 'body')))) == set()

    assert set(frozendict.frozendict(x) for x in store.filter(store.Request([
        (1, 'super_uniq_hash')
    ], [
        (True, 2)
    ]))) == {
        frozendict.frozendict({'title': 'user_2_title', 'body': None, 'href': None, 'bid': 1, 'key': 'super_uniq_hash', 'prod': True, 'user': 2, 'branch': None})
    }

    assert set(frozendict.frozendict(x) for x in store.filter(store.Request([
        (1, 'super_uniq_hash'), (1, 'super_uniq_hash'),
    ], [
        (True, 2), (True, 3)
    ]))) == {
        frozendict.frozendict({'title': 'user_2_title', 'body': None, 'href': None, 'bid': 1, 'key': 'super_uniq_hash', 'prod': True, 'user': 2, 'branch': None}),
        frozendict.frozendict({'title': None, 'body': 'update', 'href': 'user_3_href', 'bid': 1, 'key': 'super_uniq_hash', 'prod': True, 'user': 3, 'branch': None})
    }

    assert set(frozendict.frozendict(x) for x in store.filter(store.Request([
        (1, 'super_uniq_hash'), (1, 'super_uniq_hash'),
    ], [
        (True, 2), (True, 3)
    ], ('title', 'body')))) == {
        frozendict.frozendict({'title': 'user_2_title', 'body': None}),
        frozendict.frozendict({'title': None, 'body': 'update'})
    }

    assert list(store.filter(store.Request([
        (100, 'uniq_hash')
    ], [
        (False, )
    ]))) == [
        {'title': None, 'body': 'body', 'href': 'href', 'bid': 100, 'key': 'uniq_hash', 'prod': False, 'user': None, 'branch': None},
    ]

    assert list(store.filter(store.Request([
        (100, 'uniq_hash')
    ], [
        (False, 5)
    ]))) == [
        {'title': None, 'body': 'body', 'href': 'href', 'bid': 100, 'key': 'uniq_hash', 'prod': False, 'user': None, 'branch': None},
    ]

    assert list(store.filter(store.Request([
        (100, 'uniq_hash')
    ], [
        (False, 6)
    ]))) == [
        {'title': None, 'body': 'body', 'href': 'href', 'bid': 100, 'key': 'uniq_hash', 'prod': False, 'user': None, 'branch': None},
    ]

    assert list(store.filter(store.Request([
        (100, 'uniq_hash')
    ], [
        (False, 5),
        (False, 6)
    ]))) == [
        {'title': None, 'body': 'body', 'href': 'href', 'bid': 100, 'key': 'uniq_hash', 'prod': False, 'user': None, 'branch': None},
    ]

    assert list(store.filter(store.Request([
        (100, 'uniq_hash')
    ], [
        (False, 1)
    ]))) == [
        {'title': None, 'body': 'body1', 'href': 'href1', 'bid': 100, 'key': 'uniq_hash', 'prod': False, 'user': 1, 'branch': None},
    ]

    assert set(frozendict.frozendict(x) for x in store.filter(store.Request([
        (100, 'uniq_hash'), (100, 'uniq_hash'),
    ], [
        (False, 1), (False, 2)
    ]))) == {
        frozendict.frozendict({'title': None, 'body': 'body1', 'href': 'href1', 'bid': 100, 'key': 'uniq_hash', 'prod': False, 'user': 1, 'branch': None}),
        frozendict.frozendict({'title': None, 'body': 'body2', 'href': 'href2', 'bid': 100, 'key': 'uniq_hash', 'prod': False, 'user': 2, 'branch': None}),
    }

    store.drop_store()


def test_memory_store():
    mem_store = kv.MemoryKeyValueStore(('prod', 'user', 'branch'), ('bid', 'key'), ('title', 'body', 'href'))
    run_store_test(mem_store, None)


@pytest.mark.linux
def test_dyn_table_store(local_yt_client):
    dyn_store = kv.DynTableKeyValueStore(('prod', 'user', 'branch'), ('bid', 'key'), ('title', 'body', 'href'))
    tbl = '//home/' + str(uuid.uuid4())

    run_store_test(dyn_store, (tbl, local_yt_client))  # Use local YT

    tbl = '//home/' + str(uuid.uuid4())
    dyn_store = kv.DynTableKeyValueStore(('prod', 'user', 'branch'), ('bid', 'key'), ('title', 'body', 'href'))
    dyn_store.init_store((tbl, local_yt_client))
    dyn_store.create_store((bool, int, str), (int, str), (str, str, str))
    dyn_store.connect_store()

    with pytest.raises(RuntimeError):
        with dyn_store.transaction():
            dyn_store.upsert((True,), (1, 'super_uniq_hash'), {'title': 'title', 'body': 'body', 'href': 'href'})
            raise RuntimeError('Fail transaction')
    with pytest.raises(kv.error.NoSuchNamespaceException):
        dyn_store.read((True, ), (1, 'super_uniq_hash'))

    dyn_store.upsert((True,), (1, 'super_uniq_hash'), {'title': 'title', 'body': 'body', 'href': 'href'})

    dict_representation = dyn_store.dict()
    assert dict_representation == {
        'class': 'DynTableKeyValueStore',
        'config_diff': {
            'local_temp_directory': local_yt_client.config['local_temp_directory'],
            'pickling': {'python_binary': local_yt_client.config['pickling']['python_binary']},
            'prefix': '//',
            'proxy': {'url': local_yt_client.config['proxy']['url']}
        },
        'data_cols': ['title', 'body', 'href'],
        'keys_cols': ['bid', 'key'],
        'module': 'ads.multik.pylib.key_value_store.dyn_table',
        'namespaces': ['prod', 'user', 'branch'],
        'proxy': local_yt_client.config['proxy']['url'],
        'table_path': tbl,
    }
    restored = kv.DynTableKeyValueStore.from_dict(dict_representation)

    assert restored.read((True,), (1, 'super_uniq_hash')) == {'prod': True, 'user': None, 'branch': None, 'bid': 1, 'key': 'super_uniq_hash',
                                                              'title': 'title', 'body': 'body', 'href': 'href'}
    restored.drop_store()
