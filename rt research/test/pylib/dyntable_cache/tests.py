# coding: utf-8

import logging

import pytest
from mapreduce.yt.python.yt_stuff import YtConfig

import yt.yson as yson

import bm.yt_tools


# Required to test DynTables
@pytest.fixture(scope='module')
def yt_config(request):
    return YtConfig(wait_tablet_cell_initialization=True)


def test_cache(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    logging.basicConfig(level=logging.INFO)

    # setup cache
    yt_root = '//home'
    cache_table = yt_root + '/cache'
    columns = [
        {'name': 'yt_hash',         'type': 'uint64',   'required': False, 'sort_order': 'ascending', 'expression': 'farm_hash(ArgID)'},
        {'name': 'ArgID',           'type': 'uint64',   'required': True, 'sort_order': 'ascending'},
        {'name': 'ResName',         'type': 'string',   'required': True},
        {'name': 'ResVal',          'type': 'int64',    'required': True},
        {'name': 'ResObj',          'type': 'any',      'required': False},
        {'name': 'update_time',     'type': 'uint64',   'required': True},
    ]
    schema = yson.YsonList(columns)
    schema.attributes["strict"] = True
    cache_table_attributes = {
        'dynamic': True,
        'schema': schema,
        'tablet_cell_bundle': 'default',
    }
    yt_client.create('table', cache_table, attributes=cache_table_attributes)
    yt_client.mount_table(cache_table, sync=True)

    logging.info('will create cache table: %s', cache_table)

    cache_object = bm.yt_tools.DynTableCache(
        table=cache_table,
        key_field='ArgID',
        value_fields=['ResName', 'ResVal', 'ResObj'],
        lookup_client=yt_client,
        map_reduce_client=yt_client,
    )

    def get_key(arg):
        return arg['ArgID']

    def fun1(args):
        res = {}
        for arg in args:
            key = get_key(arg)
            res[key] = {
                'ResName': 'name_' + str(arg['name']),
                'ResVal': -arg['val'],
                'ResObj': arg['obj'],
            }
        logging.info('fun1 res: %s', res)
        return res

    test_args1 = [
        {'ArgID': 10, 'name': 'XXX', 'val': 5, 'obj': ['u']},
        {'ArgID': 20, 'name': 'YYY', 'val': -4, 'obj': ['v']},
        {'ArgID': 30, 'name': 'ZZZ', 'val': -3, 'obj': ['e']},
        {'ArgID': 40, 'name': 'XYZ', 'val': 1, 'obj': ['z']},
    ]
    cache_object.call(fun1, test_args1, get_key=get_key)

    l1 = next(yt_client.lookup_rows(cache_table, [{'ArgID': 20}]))
    assert l1['ResName'] == 'name_YYY'
    assert l1['ResVal'] == 4

    # флашим кэш на в чанки
    yt_client.unmount_table(cache_table, sync=True)
    yt_client.mount_table(cache_table, sync=True)

    test_rows = [
        {'uid': 11111, 'Arg': {'ArgID': 10, 'name': 'XX', 'val': 55, 'obj': []}},
        {'uid': 11112, 'Arg': {'ArgID': 15, 'name': 'AZAZ', 'val': 66, 'obj': [0]}},
    ]
    table1 = yt_root + '/data1'
    yt_client.write_table(table1, test_rows)

    cache_object.call_for_table(
        fun1,
        table1,
        table1 + '.out',
        call_pack_size=1,
        get_key=get_key,
        get_arg=lambda row: row['Arg'],
    )

    l2 = list(yt_client.read_table(table1 + '.out'))
    logging.info('l2: %s', l2)
    assert len(l2) == 2
    l2.sort(key=lambda row: row['ArgID'])
    assert l2[0]['ResName'] == 'name_XXX'  # not XX, because XXX was cached
    assert l2[1]['ResVal'] == -66
    assert l2[1]['ResObj'] == [0]
