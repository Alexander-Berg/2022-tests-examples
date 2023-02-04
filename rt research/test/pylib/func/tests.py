# coding: utf-8

import yt.yson as yson

from bm.yt_tools import join_schemas_from_list


def test_join_schemas():
    def assert_equal_schemas(s1, s2):
        assert s1.attributes['strict'] == s2.attributes['strict']
        cols1 = [col.copy() for col in s1]
        cols2 = [col.copy() for col in s2]
        for col in cols1 + cols2:
            col.setdefault('required', False)
        assert cols1 == cols2

    # простая проверка
    assert_equal_schemas(
        join_schemas_from_list([
            yson.to_yson_type([{'name': 'ID', 'type': 'uint64'}, {'name': 'Name', 'type': 'string'}], attributes={'strict': False}),
            yson.to_yson_type([{'name': 'ID', 'type': 'uint64'}, {'name': 'Age', 'type': 'uint64'}], attributes={'strict': False}),
        ]),
        yson.to_yson_type([
            {'name': 'ID', 'type': 'uint64'},
            {'name': 'Name', 'type': 'string'},
            {'name': 'Age', 'type': 'uint64'}
        ], attributes={'strict': False}),
    )

    # проверим strict
    assert_equal_schemas(
        join_schemas_from_list([
            yson.to_yson_type([{'name': 'ID', 'type': 'uint64'}], attributes={'strict': False}),
            yson.to_yson_type([{'name': 'ID', 'type': 'uint64'}], attributes={'strict': True}),
        ]),
        yson.to_yson_type([{'name': 'ID', 'type': 'uint64'}], attributes={'strict': False}),
    )
    assert_equal_schemas(
        join_schemas_from_list([
            yson.to_yson_type([{'name': 'ID', 'type': 'uint64'}], attributes={'strict': True}),
            yson.to_yson_type([{'name': 'ID', 'type': 'uint64'}], attributes={'strict': True}),
        ]),
        yson.to_yson_type([{'name': 'ID', 'type': 'uint64'}], attributes={'strict': True}),
    )

    # проверим типизацию
    try:
        join_schemas_from_list([
            yson.to_yson_type([{'name': 'ID', 'type': 'int64'}], attributes={'strict': True}),
            yson.to_yson_type([{'name': 'ID', 'type': 'uint64'}], attributes={'strict': True}),
        ])
        raise Exception("We should fail on incompatible types")
    except:
        pass  # this is ok
