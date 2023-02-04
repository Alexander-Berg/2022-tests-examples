from copy import deepcopy

from yt.wrapper.yson import YsonList
from yt.yson import YsonBoolean

import maps.analyzer.pylibs.schema.table as table
import maps.analyzer.pylibs.schema.utils as utils
from maps.analyzer.pylibs.schema import column, Optional, Int64, String, ASCENDING


def test_get_schema(ytc):
    def mk_sorted_column(name):
        return {'name': name, 'type_v3': {'type_name': 'optional', 'item': 'string'}, 'sort_order': ASCENDING}

    def mk_column(name):
        return {'name': name, 'type_v3': {'type_name': 'optional', 'item': 'string'}}

    attrs = {'strict': YsonBoolean(True), 'unique_keys': YsonBoolean(False)}

    def mk_schema(cols):
        sch = YsonList(cols)
        sch.attributes.update(attrs)
        return sch

    key_column = mk_column('key')
    value_column = mk_column('value')

    tbl_without_schema = ytc.create_temp_table()
    assert not utils.get_schema(ytc, tbl_without_schema)
    assert not utils.get_schema(ytc, tbl_without_schema + '[#0]')  # should work ok for path with modifiers

    schm = mk_schema([key_column, value_column])

    tbl_with_schema = ytc.create_temp_table(attributes={'schema': schm})
    assert utils.get_schema(ytc, tbl_with_schema) == schm

    schm2 = mk_schema([key_column])
    assert utils.get_schema(ytc, tbl_with_schema + '{key}') == schm2
    assert utils.get_schema(ytc, tbl_with_schema + '{key}', ignore_column_selectors=True) == schm

    abc_schema = mk_schema(map(mk_sorted_column, ['a', 'b', 'c']))
    ab_schema = mk_schema(map(mk_sorted_column, ['a', 'b']))
    ac_schema = mk_schema([mk_sorted_column('a'), mk_column('c')])
    bc_schema = mk_schema(map(mk_column, ['b', 'c']))

    abc_table = ytc.create_temp_table(attributes={'schema': abc_schema})
    assert utils.get_schema(ytc, abc_table + '{a,b}') == ab_schema
    assert utils.get_schema(ytc, abc_table + '{a,c}') == ac_schema
    assert utils.get_schema(ytc, abc_table + '{b,c}') == bc_schema


def test_schema():
    first = column('first', Optional(Int64), None)
    second = column('second', Optional(String), None, sort_order=ASCENDING)
    columns = [first, second]
    default_attributes = {}
    utils.set_default_attributes(default_attributes)
    custom_attributes = {'key_columns': ['first'], 'strict': YsonBoolean(False)}
    schema_with_default_attrs = YsonList([
        {
            'name': first.name,
            'type_v3': first.type.type_v3,
        },
        {
            'name': second.name,
            'type_v3': second.type.type_v3,
            'sort_order': second.sort_order
        }
    ])
    schema_with_default_attrs.attributes = default_attributes
    schema_with_custom_attrs = deepcopy(schema_with_default_attrs)
    schema_with_custom_attrs.attributes = custom_attributes

    assert schema_with_default_attrs == utils.schema(columns)
    assert schema_with_custom_attrs == utils.schema(columns, custom_attributes)


def test_schema_to_columns():
    schm = [
        {'name': 'key', 'type_v3': Optional(String).type_v3, 'sort_order': ASCENDING},
        {'name': 'value', 'type_v3': Optional(Int64).type_v3}
    ]
    columns = [
        column('key', Optional(String), None, sort_order=ASCENDING),
        column('value', Optional(Int64), None),
    ]

    assert columns == utils.schema_to_columns(schm)


def test_errors_to_message():
    empty_errors = {}
    assert '' == utils.errors_to_message(empty_errors)

    errors = {'key': 'message', 'mey': 'kessage'}
    message = utils.errors_to_message(errors)
    assert ('[key]: message' in message and
            '[mey]: kessage' in message and
            message.count(utils.ERROR_DELIMITER) == 1)


def test_required():
    col = column('key', Optional(String), None)
    assert col == utils.optional(utils.required(col))


def test_strict():
    tbl = table([column('key', Optional(String), None)], None)
    assert tbl.strict
    assert not utils.nonstrict(tbl).strict
    assert utils.strict(utils.nonstrict(tbl)).strict


def test_sorted():
    col = column('key', Optional(String), None, sort_order=ASCENDING)
    tbl = table([col], None, attributes={utils.UNIQUE_KEYS: True})

    assert utils.unsorted_column(col).sort_order is None
    assert not utils.unsorted_table(tbl).attributes.get(utils.UNIQUE_KEYS, False)
    assert utils.unsorted_table(tbl).column(col.name).sort_order is None


def test_column_names():
    columns = [
        column('first', Optional(String), None),
        column('second', Optional(String), None),
    ]
    assert utils.column_names(columns) == ['first', 'second']


def test_is_sorted_by():
    tbl = table([
        column('first', Optional(String), None, sort_order=ASCENDING),
        column('second', Optional(String), None, sort_order=ASCENDING),
        column('third', Optional(String), None),
        column('fourth', Optional(String), None),
    ], None)
    assert utils.is_sorted_by(tbl, ['first', 'second'])
    assert utils.is_sorted_by(tbl, ['first'])
    assert not utils.is_sorted_by(tbl, ['second'])
    assert not utils.is_sorted_by(tbl, ['second', 'first'])
    assert not utils.is_sorted_by(tbl, ['first', 'second', 'third'])


def test_sorted_table():
    tbl = table([
        column('first', Optional(String), None),
        column('second', Optional(String), None, sort_order=ASCENDING),
        column('third', Optional(String), None),
        column('fourth', Optional(String), None),
    ], None)

    def check_sorted(sort_by):
        sorted_tbl = utils.sorted_table(tbl, sort_by, True)
        assert utils.column_names(sorted_tbl.sorted_by) == sort_by
        assert sorted_tbl.unique_keys

    check_sorted(['first', 'third'])
    check_sorted(['third', 'first'])
