import pytest

from maps.analyzer.pylibs.schema import table, column, Optional, Any, Int64, String, derive_schema, ASCENDING, List, Tuple
from maps.analyzer.pylibs.schema.utils import (
    schema, schema_to_columns,
    nonstrict, get_schema,
    required,
    unsorted_column, unsorted_table,
)
from maps.analyzer.pylibs.schema.table import to_table, get_table, union_tables


def test_table():
    first = column('first', Optional(Int64), None)
    second = column('second', Optional(String), None)
    columns = [first, second]
    schema_attributes = {'key_columns': [first.name]}

    tbl = table(columns, None, schema_attributes)
    assert tbl.columns == columns
    assert tbl.schema == schema(columns, schema_attributes)

    good_row = {first.name: 5, second.name: b'good'}
    bad_row = {first.name: b'bad', 'extra_column': b'whatever'}
    assert (good_row, {}) == tbl.cast(good_row)
    bad_result, errors = tbl.cast(bad_row)
    assert bad_result == {'first': None}
    assert 'extra_column' in errors and 'Extra' in errors['extra_column']
    assert first.name in errors and 'Conversion' in errors[first.name]

    tbl2 = table([required(first), column('second', Optional(Int64), None)], None)
    raises_row = {first.name: 'lala', second.name: 22}
    noraises_row = {first.name: '243', second.name: 'fail'}

    with pytest.raises(ValueError):
        tbl2.cast(raises_row)
    noraises_result, noraises_errors = tbl2.cast(noraises_row)
    assert 'second' in noraises_errors


def test_get_table(ytc):
    no_schema = ytc.create_temp_table()
    no_schema_table = get_table(ytc, no_schema, strong_only=True)
    assert no_schema_table is None

    tbl = table([column('x', Optional(String), None)], None)
    with_schema = ytc.create_temp_table(attributes={'schema': tbl.schema})
    with_schema_table = get_table(ytc, with_schema, strong_only=True)
    assert with_schema_table == tbl


def test_to_table():
    schm = [{'name': 'string', 'type_v3': Optional(String).type_v3}]
    columns = schema_to_columns(schm)
    tbl = table(columns, None)
    tbl_schema = tbl.schema

    assert tbl is to_table(tbl)
    assert tbl_schema == to_table(columns).schema
    assert tbl_schema == to_table(schm).schema


def test_add_drop_columns():
    # add
    assert tbl(col('name')).add_columns([col('other_name')]) == tbl(col('name'), col('other_name'))
    assert nonstrict(tbl(col('name'))).add_columns([col('other_name')]) == nonstrict(tbl(col('name'), col('other_name')))
    assert tbl(col('name')).add_columns([col('name', ty=Optional(Int64))], overwrite=True) == tbl(col('name', ty=Optional(Int64)))
    assert tbl(col('name', sorted=True)).add_columns([col('other_name', sorted=True)]) == tbl(col('name', sorted=True), col('other_name', sorted=True))
    # sorted always goes first, but key columns order should be preserved
    assert tbl(col('name')).add_columns([col('other_name', sorted=True)]) == tbl(col('other_name', sorted=True), col('name'))
    assert tbl(
        col('key', sorted=True),
        col('name')
    ).add_columns([
        col('other_name', sorted=True)
    ]).columns == [
        col('key', sorted=True),
        col('other_name', sorted=True),
        col('name')
    ]  # compare with raw list to ensure correct order
    with pytest.raises(ValueError):
        tbl(col('name')).add_columns([col('name')])

    # drop
    assert tbl(col('foo'), col('bar')).drop_columns([col('foo')]) == tbl(col('bar'))
    assert tbl(col('foo'), col('bar'), col('baz')).drop_columns([col('baz'), col('bar')]) == tbl(col('foo'))
    assert tbl(col('foo', sorted=True), col('bar'), col('baz')).drop_columns([col('baz'), col('bar')]) == tbl(col('foo', sorted=True))
    assert tbl(col('foo'), col('bar')).drop_columns([col('baz'), col('bar')], strict=False) == tbl(col('foo'))
    with pytest.raises(ValueError):
        tbl(col('foo'), col('bar')).drop_columns([col('bar'), col('baz')])
    with pytest.raises(ValueError):
        tbl(col('foo', sorted=True), col('bar')).drop_columns([col('foo')])


def test_derive_schema(ytc):
    tbl = table([column('key', Optional(String), None)], None)
    col = column('some', Optional(String), None)

    tbl_without_schema = ytc.create_temp_table()

    derived_table = ytc.create_temp_table(schema=derive_schema(ytc, tbl_without_schema))
    derived_table_2 = ytc.create_temp_table(schema=derive_schema(
        ytc, tbl_without_schema,
        alter_table=lambda t: t.add_columns([col]),
    ))

    assert get_schema(ytc, derived_table) == get_schema(ytc, tbl_without_schema)
    assert get_schema(ytc, derived_table_2) == get_schema(ytc, tbl_without_schema)  # doesn't affect weak schema

    tbl_with_schema = ytc.create_temp_table(attributes={'schema': tbl.schema})

    derived_table = ytc.create_temp_table(schema=derive_schema(ytc, tbl_with_schema))
    derived_table_2 = ytc.create_temp_table(schema=derive_schema(
        ytc, tbl_with_schema,
        alter_table=lambda t: t.add_columns([col]),
    ))

    assert get_schema(ytc, derived_table) == get_schema(ytc, tbl_with_schema)
    sch = get_schema(ytc, tbl_with_schema)
    sch.append(col.schema)
    assert get_schema(ytc, derived_table_2) == sch


def test_table_sorted_by():
    sorted_columns = [col('first', sorted=True), col('second', sorted=True)]
    unsorted_columns = [col('third'), col('fourth')]
    t = table(sorted_columns + unsorted_columns, None)
    assert t.sorted_by == sorted_columns
    assert table(t.sorted_by, None).sorted_by == t.sorted_by
    assert not table(unsorted_columns, None).sorted_by


def test_is_subset():
    assert tbl(col('foo', Optional(Int64)), col('bar'), col('quux', Optional(Int64))).is_subset(tbl(col('bar'), col('foo', Optional(Int64))))
    assert tbl(col('foo', Int64), col('bar')).is_subset(tbl(col('foo', Optional(Int64))))
    assert not tbl(col('foo', Optional(Int64)), col('bar')).is_subset(tbl(col('foo', Int64)))
    assert tbl(col('foo', Optional(Int64)), col('bar'), col('quux', Optional(Int64))).is_subset(tbl(col('bar', Any), col('foo', Optional(Int64))))
    assert tbl(col('foo', Int64, sorted=True), col('bar', sorted=True), col('quux', Optional(Int64))).is_subset(
        tbl(col('foo', Optional(Int64), sorted=True), col('bar', sorted=True))
    )
    assert not tbl(col('foo', Int64, sorted=True), col('bar', sorted=True), col('quux', Optional(Int64))).is_subset(
        tbl(col('bar', sorted=True), col('foo', Optional(Int64), sorted=True))
    )
    assert tbl(col('foo', Optional(Int64)), col('bar')).is_subset(tbl(col('foo', Optional(Int64))))
    assert not tbl(col('foo', Optional(Int64)), col('bar')).is_subset(tbl(col('foo', Optional(Int64))), ignore_extra_columns=False)


def test_union_tables():
    assert union_tables([
        tbl(col('foo', sorted=True), col('bar', String, sorted=True), col('quux')),
        tbl(col('foo', sorted=True), col('quux', Optional(Int64), sorted=True), col('bar', String)),
    ]) == tbl(col('foo', sorted=True), col('bar', String), col('quux', Any))

    assert union_tables([
        tbl(col('foo', String)),
        tbl(col('bar', String)),
    ]) == tbl(col('foo'), col('bar'))

    COLS = [col('foo', sorted=True), col('bar', sorted=True), col('quux', String)]
    assert union_tables([
        tbl(*COLS),
        tbl(*(COLS + [col('smth')])),
    ]) == tbl(*(COLS + [col('smth')]))
    assert union_tables([
        tbl(*COLS),
        tbl(*([col('smth')] + list(map(unsorted_column, COLS)))),
    ]) == unsorted_table(tbl(*(COLS + [col('smth')])))  # order of columns doesn't matter

    # narrow any
    assert union_tables([
        tbl(col('foo', ty=Any), col('bar')),
        tbl(col('foo', ty=List(String)), col('bar')),
    ], narrow_any=True) == tbl(col('foo', ty=List(String)), col('bar'))
    # narrow any with different complex types
    assert union_tables([
        tbl(col('foo', ty=Any), col('bar')),
        tbl(col('foo', ty=List(String)), col('bar')),
        tbl(col('foo', ty=Tuple(String)), col('bar')),
    ], narrow_any=True) == tbl(col('foo', ty=Any), col('bar'))

    # narrow optional
    assert union_tables([
        tbl(col('foo'), col('bar', String)),
        tbl(col('foo', String), col('bar')),
    ], narrow_opt=True) == tbl(col('foo', String), col('bar', String))
    # missing column lead to optional
    assert union_tables([
        tbl(col('foo'), col('bar', String)),
        tbl(col('foo', String)),
    ], narrow_opt=True) == tbl(col('foo', String), col('bar'))


def col(name, ty=Optional(String), sorted=False):
    return column(name, ty, None, sort_order=ASCENDING if sorted else None)


def tbl(*cols):
    return table(list(cols), None)
