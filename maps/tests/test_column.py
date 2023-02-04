import pytest

from maps.analyzer.pylibs.schema import Int64, Any, ASCENDING, column, Optional
from maps.analyzer.pylibs.schema.column import Column


def test_column():
    name = 'name'
    type = Optional(Int64)
    help = None
    sort_order = ASCENDING
    col_schema = {
        'name': 'name',
        'type_v3': Optional(Int64).type_v3,
        'sort_order': ASCENDING
    }

    with pytest.raises(ValueError):
        col = column(name, type, help, bad_optional_column_trait='anything')

    col = column(name, type, help, sort_order=sort_order)
    assert col == Column.from_schema(col_schema)

    col_as_dict = {
        'name': name,
        'type': Optional(Int64),
        'help': help,
        'sort_order': ASCENDING,
        'lock': None,
        'expression': None,
        'aggregate': None,
        'group': None,
    }
    assert col_as_dict == col.asdict()

    assert col_schema == col.schema

    new_col = col.replace(sort_order=None)
    col_as_dict['sort_order'] = None
    assert col_as_dict == new_col.asdict()

    col_schema.pop('sort_order')
    assert col_schema == new_col.schema


def test_is_subset():
    def col(ty, ordered=False):
        return column('foo', ty, None, sort_order=ASCENDING if ordered else None)

    assert col(Int64, ordered=True).is_subset(col(Int64, False))
    assert col(Int64, ordered=True).is_subset(col(Optional(Int64), True))
    assert col(Int64).is_subset(col(Optional(Int64)))
    assert col(Int64).is_subset(col(Any))
