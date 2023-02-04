# -*- coding: utf-8 -*-

import pytest
import mock
from balance import mapper
from balance.completions import CompletionFilter
from decimal import Decimal
from datetime import datetime, date
from balance.completions_fetcher.configurable_partner_completion import RaiseOnEmptySetBehavior, \
    FieldTypecastBehavior, AddStaticFieldsBehavior, MoveFieldBehavior, CopyFieldBehavior, \
    SplitFieldBehavior, ConvertStringCaseBehavior, CastUUIDToIntBehavior, AppendDTBehavior, \
    FilterBehavior, ReplaceFieldValueBehavior, CalcBehavior, DInstallsBehavior, \
    Adapter2Behavior, Tags3Behavior, ConvertTimezoneBehavior, EvaluateTruthyBehavior, \
    ExtractFieldsBehavior, ExtractFieldsAsJsonBehavior, ApplyDatedNDSBehavior, \
    UniqueBehavior, GroupBehavior, CompletionFilterBehavior, RaiseIfClusterIsNotDay, \
    ValidateBehavior, AddValuesBehavior, ParseDateBehavior


class MockPlace(object):
    def __init__(self, place_id):
        self.place_id = place_id


# RAISE ON EMPTY SET
@pytest.mark.parametrize('data, expected', [
    [(i for i in [1, 2, 3]), [1, 2, 3]],
    [(i for i in [0]), [0]],
])
def test_raise_on_empty_set(data, expected):
    source_name = 'test_source'
    behavior = RaiseOnEmptySetBehavior(source_name=source_name)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


@pytest.mark.parametrize('data, error_class', [
    [(i for i in []), ValueError],
    [None, ValueError],
    [[], ValueError],
])
def test_raise_on_empty_set_raise(data, error_class):
    source_name = 'test_source'
    behavior = RaiseOnEmptySetBehavior(source_name=source_name)
    with pytest.raises(error_class):
        list(behavior.process(data))


# FIELD TYPECAST BEHAVIOR
@pytest.mark.parametrize('data, types, expected', [
    [
        [
            {'int_val': '1', 'decimal_val': '2.0000023', 'datetime_val': '2019-09-01 12:09:45',
             'string_val': 121, 'datetime_z_val': '2019-09-01T12:01:01.000000Z', 'raw_value': {'raw': 100}},
        ],
        [['int_val', 'int'], ['decimal_val', 'decimal'], ['datetime_val', 'datetime'], ['string_val', 'string'],
         ['datetime_z_val', 'datetime']],
        [
            {'int_val': 1, 'decimal_val': Decimal('2.0000023'), 'datetime_val': datetime(2019, 9, 1, 12, 9, 45),
             'string_val': '121', 'datetime_z_val': datetime(2019, 9, 1, 12, 1, 1), 'raw_value': {'raw': 100}}
        ]
    ],
    [
        [
            {'datetime_z_val': '2020-02-19T18:24:54Z'},
        ],
        [['datetime_z_val', 'datetime']],
        [
            {'datetime_z_val': datetime(2020, 2, 19, 18, 24, 54)}
        ]
    ],
    [
        [
            {'id': '1', 'price': '100.0000023', 'dt': '20190902130000'},
            {'id': '2', 'price': '200.0000034', 'dt': '20190902140000'},
            {'id': '3', 'price': '300.0000045', 'dt': '20190902150000'},
            {'id': '4', 'price': '400.0000056', 'dt': '20190902160000'},
        ],
        [['id', 'int'], ['price', 'decimal'], ['dt', 'datetime']],
        [
            {'id': 1, 'price': Decimal('100.0000023'), 'dt': datetime(2019, 9, 2, 13, 0, 0)},
            {'id': 2, 'price': Decimal('200.0000034'), 'dt': datetime(2019, 9, 2, 14, 0, 0)},
            {'id': 3, 'price': Decimal('300.0000045'), 'dt': datetime(2019, 9, 2, 15, 0, 0)},
            {'id': 4, 'price': Decimal('400.0000056'), 'dt': datetime(2019, 9, 2, 16, 0, 0)},
        ]
    ],
])
def test_field_typecast(data, types, expected):
    behavior = FieldTypecastBehavior(types=types)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# ADD VALUES BEHAVIOR
@pytest.mark.parametrize('data, values, overwrite_existing_fields, exception_message, expected', [
    [
        [{'exists': 1}],
        [['new_value', '2'], ['new_value_3', 3]],
        False,
        None,
        [{'exists': 1, 'new_value': '2', 'new_value_3': 3}],
    ],
    [
        [{'exists': 1}],
        [['exists', '2']],
        False,
        'Error on adding field exists: field is already in row',
        None,
    ],
    [
        [{'exists': 1}],
        [['exists', '2']],
        True,
        None,
        [{'exists': '2'}],
    ],
])
def test_add_values(data, values, overwrite_existing_fields, exception_message, expected):
    behavior = AddValuesBehavior(values=values, overwrite_existing_fields=overwrite_existing_fields)
    if exception_message:
        with pytest.raises(Exception) as _exc:
            list(behavior.process(data))
        assert exception_message in str(_exc), str(_exc)
    else:
        actual = list(behavior.process(data))
        assert actual == expected, 'Processed rows are different from expected'


@pytest.mark.parametrize('data, types, default, expected', [
    [
        [{'id': '1', 'some': 'a', 'dt': None}], [['id', 'int'], ['some', 'decimal'], ['dt', 'datetime']], None,
        [{'id': 1, 'some': None, 'dt': None}]  # mistype
    ],
    [
        [{'id': '2'}], [['id', 'int'], ['price', 'int']], 100, [{'id': 2, 'price': 100}]  # missing value
    ],
    [
        [
            {'id': '1', 'price': '10000.12', 'payload': '123', 'dt': None},
            {'id': '2', 'price': None, 'payload': '321', 'dt': '2019-08-21'},
            {'id': '3', 'price': None, 'payload': 'string', 'dt': None},
        ],
        [['id', 'int'], ['price', 'decimal'], ['payload', 'int'], ['dt', 'datetime']],
        {'price': '333.33', 'dt': '2019-09-01', 'payload': None},
        [
            {'id': 1, 'price': Decimal('10000.12'), 'payload': 123, 'dt': datetime(2019, 9, 1, 0, 0, 0)},
            {'id': 2, 'price': Decimal('333.33'), 'payload': 321, 'dt': datetime(2019, 8, 21, 0, 0, 0)},
            {'id': 3, 'price': Decimal('333.33'), 'payload': None, 'dt': datetime(2019, 9, 1, 0, 0, 0)},
        ]  # dict defaults
    ]
])
def test_field_typecast_mistype_default(data, types, default, expected):
    behavior = FieldTypecastBehavior(types=types, default_for_mistyped=default, raise_on_mistype=False)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


@pytest.mark.parametrize('data, types', [
    [
        [
            {'id': '1'}, {'id': 'aaa'},
        ],
        [['id', 'int']]
    ],
])
def test_field_typecast_raise_on_wrong_data(data, types):
    behavior = FieldTypecastBehavior(types=types)
    with pytest.raises(ValueError) as e:
        list(behavior.process(data))
    assert str(e.value).startswith('Error on typing field'), 'Raise other error instead "Unknown typename"'


@pytest.mark.parametrize('types, defaults', [
    [
        [['id', 'int'], ['dt', 'datetime']],
        {'transaction_id': 'int'}  # missed in types field
    ],
    [
        [['id', 'int'], ['dt', 'datetime']],
        {'id': 'AAA'}  # invalid value for type
    ]
])
def test_field_typecast_raise_on_wrong_defaults(types, defaults):
    with pytest.raises(ValueError):
        FieldTypecastBehavior(types=types, default_for_mistyped=defaults, raise_on_mistype=False)


@pytest.mark.parametrize('types', [
    [['id', 'ints'], ['value', 'string']],
])
def test_field_typecast_raise_on_wrong_types(types):
    with pytest.raises(ValueError) as e:
        FieldTypecastBehavior(types=types)
    assert str(e.value).startswith('Unknown typename'), 'Raise other error instead "Unknown typename"'


#  ADD STATIC FIELDS
@pytest.mark.parametrize('data, static_fields, from_config, expected', [
    [
        [{'id': 1}, {'id': 2, 'value': 2}],
        {'value': 100, 'dt': '2019-09-01'}, None,
        [{'id': 1, 'value': 100, 'dt': '2019-09-01'}, {'id': 2, 'value': 100, 'dt': '2019-09-01'}]  # add static field
    ],
    [
        [{'id': 1}, {'id': 2, 'value': 2}],
        {'value': 100}, ['page_id', 'source_id'],
        [{'id': 1, 'value': 100, 'page_id': 'page_id', 'source_id': 'source_id'},
         {'id': 2, 'value': 100, 'page_id': 'page_id', 'source_id': 'source_id'}]  # add fields from config
    ]
])
def test_add_static_fields(data, static_fields, from_config, expected):
    behavior = AddStaticFieldsBehavior(fields=static_fields, from_config=from_config,
                                       page_id='page_id', source_id='source_id')
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


#  MOVE FIELDS
@pytest.mark.parametrize('data, move_map, expected', [
    [
        [{'clid': 1, 'price': 100}, {'clid': 2, 'client_id': 3, 'price': 200}],
        [['clid', 'client_id']],
        [{'client_id': 1, 'price': 100}, {'client_id': 2, 'price': 200}]
    ],
    [
        [{'clid': 1, 'update_dt': '2019-09-01', 'price': 100}, {'clid': 2, 'update_dt': '2019-09-02', 'price': 200}],
        [['clid', 'client_id'], ['update_dt', 'dt']],
        [{'client_id': 1, 'dt': '2019-09-01', 'price': 100}, {'client_id': 2, 'dt': '2019-09-02', 'price': 200}]
    ],
    [
        [{'payload': 'string a', 'update_dt': '2019-09-01', 'price': 100},
         {'payload': 'string b', 'update_dt': '2019-09-02', 'price': 200}],
        [['payload', 't'], ['update_dt', 'payload'], ['t', 'update_dt']],
        [{'update_dt': 'string a', 'payload': '2019-09-01', 'price': 100},
         {'update_dt': 'string b', 'payload': '2019-09-02', 'price': 200}],
    ]
])
def test_move_fields(data, move_map, expected):
    behavior = MoveFieldBehavior(map=move_map)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# COPY FIELDS
@pytest.mark.parametrize('data, copy_map, expected', [
    [
        [{'clid': 1, 'price': 100}, {'clid': 2, 'client_id': 3, 'price': 200}],
        [['clid', 'client_id']],
        [{'clid': 1, 'client_id': 1, 'price': 100}, {'clid': 2, 'client_id': 2, 'price': 200}]
    ],
    [
        [{'clid': 1, 'update_dt': '2019-09-01', 'price': 100}, {'clid': 2, 'update_dt': '2019-09-02', 'price': 200}],
        [['clid', 'client_id'], ['update_dt', 'dt']],
        [{'clid': 1, 'client_id': 1, 'update_dt': '2019-09-01', 'dt': '2019-09-01', 'price': 100},
         {'clid': 2, 'client_id': 2, 'update_dt': '2019-09-02', 'dt': '2019-09-02', 'price': 200}]
    ]
])
def test_copy_fields(data, copy_map, expected):
    behavior = CopyFieldBehavior(map=copy_map)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# SPLIT FIELD
@pytest.mark.parametrize('data, field, to, delimiter, default, delete_from_field, expected', [
    # split field and delete field
    [
        [{'id': 1, 'payload': '123-string-321'}, {'id': 2, 'payload': '456-asd-789'}],
        'payload', ['user_id', 'msg', 'product_id'], '-', None, True,
        [{'id': 1, 'user_id': '123', 'msg': 'string', 'product_id': '321'},
         {'id': 2, 'user_id': '456', 'msg': 'asd', 'product_id': '789'}]
    ],
    # split field and delete field with skipped field
    [
        [{'id': 1, 'payload': '123-string-321'}, {'id': 2, 'payload': '456-asd-789'}],
        'payload', ['user_id', '_', 'product_id'], '-', None, True,
        [{'id': 1, 'user_id': '123', 'product_id': '321'},
         {'id': 2, 'user_id': '456', 'product_id': '789'}]
    ],
    # split field and don't delete field
    [
        [{'id': 1, 'payload': '123-string-321'}, {'id': 2, 'payload': '456-asd-789'}],
        'payload', ['user_id', 'msg', 'product_id'], '-', None, False,
        [{'id': 1, 'payload': '123-string-321', 'user_id': '123', 'msg': 'string', 'product_id': '321'},
         {'id': 2, 'payload': '456-asd-789', 'user_id': '456', 'msg': 'asd', 'product_id': '789'}]
    ],
    # split field with default value
    [
        [{'id': 1, 'payload': '123.string'}, {'id': 2, 'payload': '456.asd.789'},
         {'id': 3, 'payload': '987'}, {'id': 4, 'payload': ''}, {'id': 5, 'payload': None}],
        'payload', ['user_id', 'msg', 'product_id'], '.', 'SSS', True,
        [{'id': 1, 'user_id': '123', 'msg': 'string', 'product_id': 'SSS'},
         {'id': 2, 'user_id': '456', 'msg': 'asd', 'product_id': '789'},
         {'id': 3, 'user_id': '987', 'msg': 'SSS', 'product_id': 'SSS'},
         {'id': 4, 'user_id': 'SSS', 'msg': 'SSS', 'product_id': 'SSS'},
         {'id': 5, 'user_id': 'SSS', 'msg': 'SSS', 'product_id': 'SSS'}]
    ],
    # split field with default values (as dict)
    [
        [{'id': 1, 'payload': '123.string'}, {'id': 2, 'payload': '456.asd.789'},
         {'id': 3, 'payload': '987'}, {'id': 4, 'payload': ''}, {'id': 5, 'payload': None}],
        'payload', ['user_id', 'payload', 'product_id'], '.',
        {'user_id': 'AAA', 'payload': 'MMM', 'product_id': 'PPP'}, False,
        [{'id': 1, 'user_id': '123', 'payload': 'string', 'product_id': 'PPP'},
         {'id': 2, 'user_id': '456', 'payload': 'asd', 'product_id': '789'},
         {'id': 3, 'user_id': '987', 'payload': 'MMM', 'product_id': 'PPP'},
         {'id': 4, 'user_id': 'AAA', 'payload': 'MMM', 'product_id': 'PPP'},
         {'id': 5, 'user_id': 'AAA', 'payload': 'MMM', 'product_id': 'PPP'}]
    ]
])
def test_split(data, field, to, delimiter, default, delete_from_field, expected):
    behavior = SplitFieldBehavior(field=field, to=to, delimiter=delimiter,
                                  default=default, delete_from_field=delete_from_field, raise_on_different_length=False)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


@pytest.mark.parametrize('data, field, to, delimiter', [
    [
        [{'id': 1, 'payload': '123-string-321'}, {'id': 2, 'payload': 'asd-789'}],
        'payload', ['user_id', 'msg', 'product_id'], '-'
    ],
    # with skipped fields
    [
        [{'id': 1, 'payload': '123-string-321'}, {'id': 2, 'payload': 'asd-789'}],
        'payload', ['user_id', '_', 'product_id'], '-'
    ],
])
def test_split_raise_on_different_length_fields_and_values(data, field, to, delimiter):
    behavior = SplitFieldBehavior(field=field, to=to, delimiter=delimiter)
    with pytest.raises(ValueError) as e:
        list(behavior.process(data))
    assert str(e.value).endswith('has incorrect count of values to unpack')


@pytest.mark.parametrize('data, field, to, delimiter, default', [
    [
        [{'id': 1, 'payload': '123-string-321'}, {'id': 2, 'payload': 'asd-789'}],
        'payload', ['user_id', 'msg', 'product_id'], '-',
        {'user_id': 1, 'msg': 2}  # has'nt default for product_id
    ],
])
def test_split_raise_on_missed_default(data, field, to, delimiter, default):
    behavior = SplitFieldBehavior(field=field, to=to, delimiter=delimiter, default=default,
                                  raise_on_different_length=False)
    with pytest.raises(ValueError) as e:
        list(behavior.process(data))
    msg = str(e.value)
    assert msg.startswith('Default value for') and msg.endswith('is not set')


# CAST UUID TO INT
@pytest.mark.parametrize('data, fields, expected', [
    [
        [{'id': '1', 'uuid': '00000000-0000-0000-0000-0000000003e8'},
         {'id': '2', 'uuid': '00000000-0000-0000-0000-000000522f10'}],
        ['uuid'],
        [{'id': '1', 'uuid': 1000},
         {'id': '2', 'uuid': 5386000}]
    ],
    [
        [
            {'id': '1', 'uuid_1': '00000000-0000-0000-0023-86f26fc10000',
             'uuid_2': '00000000-0000-0000-0027-147114878000'},
            {'id': '2', 'uuid_1': '00000000-0000-0000-0047-0de4df820000',
             'uuid_2': '00000000-0000-0000-004e-28e2290f0000'}
        ],
        ['uuid_1', 'uuid_2'],
        [{'id': '1', 'uuid_1': 10000000000000000, 'uuid_2': 11000000000000000},
         {'id': '2', 'uuid_1': 20000000000000000, 'uuid_2': 22000000000000000}]
    ],
])
def test_cast_uuid_to_int(data, fields, expected):
    behavior = CastUUIDToIntBehavior(fields=fields)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# CONVERT STRING CASE
@pytest.mark.parametrize('data, fields, to_case, expected', [
    [
        [{'name': 'aaa', 'payload': 'bbb'}, {'name': 'bBb', 'payload': 'CcC'}],
        ['name'], 'upper',
        [{'name': 'AAA', 'payload': 'bbb'}, {'name': 'BBB', 'payload': 'CcC'}]
    ],
    [
        [{'name': 'aaa', 'payload': 'bBb'}, {'name': 'bBb', 'payload': 'CcC'}],
        ['name', 'payload'], 'lower',
        [{'name': 'aaa', 'payload': 'bbb'}, {'name': 'bbb', 'payload': 'ccc'}]
    ]
])
def test_convert_string_case(data, fields, to_case, expected):
    behavior = ConvertStringCaseBehavior(fields=fields, to=to_case)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# APPEND DT
@pytest.mark.parametrize('data, field, to_string_format, dt, expected', [
    [
        [{'id': 100, 'payload': 'p01'}, {'id': 200, 'payload': 'p02'}, {'id': 300, 'payload': 'p03'}],
        'dt', None, datetime(2019, 9, 10, 1, 0, 0),
        [{'id': 100, 'payload': 'p01', 'dt': datetime(2019, 9, 10, 1, 0, 0)},
         {'id': 200, 'payload': 'p02', 'dt': datetime(2019, 9, 10, 1, 0, 0)},
         {'id': 300, 'payload': 'p03', 'dt': datetime(2019, 9, 10, 1, 0, 0)}],
    ],
    [
        [{'id': 100, 'payload': 'p01'}, {'id': 300, 'payload': 'p03'}],
        'dt', '%Y-%m-%d', datetime(2019, 11, 12, 11, 22, 33),
        [{'id': 100, 'payload': 'p01', 'dt': '2019-11-12'},
         {'id': 300, 'payload': 'p03', 'dt': '2019-11-12'}],
    ]
])
def test_append_dt(data, field, to_string_format, dt, expected):
    behavior = AppendDTBehavior(start_dt=dt, field=field, to_string_format=to_string_format)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# FILTER
@pytest.mark.parametrize('data, conditions, expected', [
    # one field compare_value
    [
        [{'id': id_} for id_ in range(0, 20)],
        [
            {'field': 'id', 'operator': '<', 'compare_value': 10},
            {'field': 'id', 'operator': '>=', 'compare_value': 3},
        ],
        [{'id': id_} for id_ in range(3, 10)],
    ],
    [
        [{'id': id_} for id_ in range(0, 20)],
        [
            {'field': 'id', 'operator': 'in', 'compare_value': [1, 2, 3]},
        ],
        [{'id': id_} for id_ in range(1, 4)],
    ],
    [
        [{'id': id_} for id_ in range(0, 20)],
        [
            {'field': 'id', 'operator': 'in', 'compare_table_field': 't_distribution_pages.place_id'},
            {'field': 'id', 'operator': '<', 'compare_value': 4},
        ],
        [{'id': id_} for id_ in range(2, 4)],
    ],
    # with None in condition
    [
        [{'id': 1, 'p': None}, {'id': 2, 'p': 'AAA'}],
        [{'field': 'p', 'operator': '!=', 'compare_value': None}],
        [{'id': 2, 'p': 'AAA'}],
    ],
    # two field compare value and another field
    [
        [{'client_id': id_, 'person_id': id_ if id_ & 1 else None} for id_ in range(0, 10)],
        [
            {'field': 'client_id', 'operator': '<', 'compare_value': 10},
            {'field': 'person_id', 'operator': '==', 'compare_field': 'client_id'},
        ],
        [{'client_id': id_, 'person_id': id_} for id_ in range(1, 10, 2)]
    ],
    # always false conditions
    [
        [{'client_id': id_, 'person_id': id_} for id_ in range(0, 10)],
        [
            {'field': 'client_id', 'operator': '<', 'compare_value': 5},
            {'field': 'person_id', 'operator': '>', 'compare_field': 'client_id'},
        ],
        []
    ],
    # empty conditions always true
    [
        [{'client_id': id_, 'person_id': id_} for id_ in range(0, 10)],
        [],
        [{'client_id': id_, 'person_id': id_} for id_ in range(0, 10)],
    ],
    # many conditions
    [
        [{'client_id': cl_id, 'person_id': p_id, 'dt': '2019-01-' + str(day), 'id': 1 if cl_id & 1 else None}
         for cl_id, p_id, day in zip(range(0, 10), range(10, 0, -1), range(10, 20))],
        [
            {'field': 'id', 'operator': '!=', 'compare_value': 'HIDE_IT', 'default': 'HIDE_IT'},
            {'field': 'client_id', 'operator': '>', 'compare_field': 'id'},
            {'field': 'dt', 'operator': '<=', 'compare_value': '2019-01-15'},
            {'field': 'dt', 'operator': '>=', 'compare_value': '2019-01-10'},
            {'field': 'person_id', 'operator': '<=', 'compare_field': 'client_id'},
        ],
        [{'client_id': 5, 'person_id': 5, 'dt': '2019-01-15', 'id': 1}]
    ],
])
def test_filter(data, conditions, expected):
    session = mock.MagicMock()
    session.query().all = lambda **kw: [MockPlace(i) for i in range(2, 5)]
    behavior = FilterBehavior(conditions=conditions, session=session)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


@pytest.mark.parametrize('conditions', [
    [{'field': 'a', 'operator': '//'}],  # wrong operator
    [{'field': 'a', 'compare_field': 'b', 'compare_value': 100}],  # usage compare field and value
    [{'field': 'a', 'compare_field': 'b', 'compare_value': 100}],  # usage compare field and value
    [{'field': 'a', 'compare_table_field': 'b.a', 'compare_value': 100}],  # usage compare table_field and value
    [{'field': 'a', 'compare_table_field': 'b'}],  # usage invalid compare_table_field (not table.field)
    [{'field': 'a', 'compare_table_field': 'b_c.a.a'}],  # usage invalid compare_table_field (not table.field)
])
def test_filter_wrong_condition(conditions):
    with pytest.raises(ValueError):
        FilterBehavior(conditions=conditions)


# REPLACE FIELD VALUE BEHAVIOR
@pytest.mark.parametrize('data, conditions, field, with_value, with_field, expected', [
    [
        # change to value
        [{'id': i, 'price': Decimal(i * 10) if i & 1 else Decimal(-1)} for i in range(1, 6)],
        [{'field': 'price', 'operator': '<', 'compare_value': 0}],
        'price', Decimal(100), None,
        [{'id': 1, 'price': Decimal(10)}, {'id': 2, 'price': Decimal(100)}, {'id': 3, 'price': Decimal(30)},
         {'id': 4, 'price': Decimal(100)},  {'id': 5, 'price': Decimal(50)}]
    ],
    [
        # change to value
        [{'id': i, 'price': Decimal(i * 10)} for i in range(1, 6)],
        [{'field': 'id', 'operator': 'in', 'compare_value': [1, 2, 3]},
         {'field': 'price', 'operator': '<', 'compare_value': 30}],
        'price', Decimal(100), None,
        [{'id': 1, 'price': Decimal(100)}, {'id': 2, 'price': Decimal(100)}, {'id': 3, 'price': Decimal(30)},
         {'id': 4, 'price': Decimal(40)},  {'id': 5, 'price': Decimal(50)}]
    ],
    [
        # change to value
        [{'id': i, 'price': Decimal(i * 10)} for i in range(1, 6)],
        [{'field': 'id', 'operator': 'nin', 'compare_value': [1, 2, 3]},
         {'field': 'price', 'operator': '<', 'compare_value': 50}],
        'price', Decimal(100), None,
        [{'id': 4, 'price': Decimal(40)}, ]
    ],
    [
        # change to field
        [{'id': i, 'price': Decimal(i * 10) if i & 1 else None} for i in range(1, 6)],
        [{'field': 'id', 'operator': '>=', 'compare_value': 3},
         {'field': 'price', 'operator': '==', 'compare_value': None}],
        'price', None, 'id',
        [{'id': 1, 'price': Decimal(10)}, {'id': 2, 'price': None}, {'id': 3, 'price': Decimal(30)},
         {'id': 4, 'price': 4},  {'id': 5, 'price': Decimal(50)}]
    ],
    [
        [
            {"clicks": 18, "shows": 17, "clid": None, "national_version": "com",
             "bucks": "63.864835", "client_id": 34879676},
            {"clicks": 7, "shows": 7, "clid": "j:null", "national_version": "kz",
             "bucks": "10.005718", "client_id": 13361436},
            {"clicks": 120, "shows": 11, "clid": "2328170", "national_version": "kz",  # <--
             "bucks": "2.429388", "client_id": 1612908},
            {"clicks": 1, "shows": 1, "clid": "2328169-637", "national_version": "kz",
             "bucks": "1.429388", "client_id": 1612907},
            {"clicks": 1, "shows": 1, "clid": "2323977-673", "national_version": "kz",
             "bucks": "1.429388", "client_id": 13361436}
        ],
        [{'field': 'clicks', 'operator': '>', 'compare_field': 'shows'},
         {'field': 'national_version', 'operator': '==', 'compare_value': 'kz'}],
        'bucks', None, 'clid',
        [
            {"clicks": 18, "shows": 17, "clid": None, "national_version": "com",
             "bucks": "63.864835", "client_id": 34879676},
            {"clicks": 7, "shows": 7, "clid": "j:null", "national_version": "kz",
             "bucks": "10.005718", "client_id": 13361436},
            {"clicks": 120, "shows": 11, "clid": "2328170", "national_version": "kz",
             "bucks": "2328170", "client_id": 1612908},
            {"clicks": 1, "shows": 1, "clid": "2328169-637", "national_version": "kz",
             "bucks": "1.429388", "client_id": 1612907},
            {"clicks": 1, "shows": 1, "clid": "2323977-673", "national_version": "kz",
             "bucks": "1.429388", "client_id": 13361436}
        ],
    ]
])
def test_replace_field_value(data, conditions, field, with_value, with_field, expected):
    behavior = ReplaceFieldValueBehavior(conditions=conditions, field=field,
                                         with_value=with_value, with_field=with_field, session=None)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# VALIDATE BEHAVIOR
@pytest.mark.parametrize('data, conditions, with_error', [
    [
        [{'service_id': 10}],
        [{'field': 'service_id', 'operator': 'in', 'compare_value': [10, 11]}],
        False
    ],
    [
        [{'service_id': 10}],
        [{'field': 'service_id', 'operator': 'in', 'compare_value': [11, 12]}],
        True
    ],
])
def test_replace_field_value(data, conditions, with_error):
    behavior = ValidateBehavior(conditions=conditions, session=None)
    if with_error:
        with pytest.raises(ValueError):
            list(behavior.process(data))
    else:
        list(behavior.process(data))


# CALC BEHAVIOR
@pytest.mark.parametrize('data, fields, in_decimal, ignore_null, operations, expected', [
    [
        #  unary -
        [{'amount': i} for i in range(-1, -10, -1)],
        ['amount'], False, True, [['-']],
        [{'amount': i} for i in range(1, 10)],
    ],
    [
        # operation sequence
        [{'id': 'aaa', 'amount': Decimal('100.350')}, {'id': 'bbb', 'amount': Decimal('350.105')},
         {'id': 'ccc', 'amount': Decimal('0')}],
        ['amount'], False, True,
        [['+', 20], ['*', 4], ['/', 2], ['-', 10]],
        [{'id': 'aaa', 'amount': Decimal('230.700')}, {'id': 'bbb', 'amount': Decimal('730.21')},
         {'id': 'ccc', 'amount': Decimal('30')}],
    ],
    [
        # using decimal
        [{'id': 'aaa', 'amount': Decimal('1.3000000000011'), 'qty': Decimal('5')},
         {'id': 'bbb', 'amount': Decimal('2.4000000000000007'), 'qty': Decimal('17')},
         {'id': 'ccc', 'amount': Decimal('0'), 'qty': Decimal('-119')}],
        ['amount', 'qty'], True, True,
        [['+', '0.000000333'], ['*', '10.1'], ['-', '0.000012100000000000009']],
        [{'id': 'aaa', 'amount': Decimal('13.129991263311109999991'), 'qty': Decimal('50.499991263299999999991')},
         {'id': 'bbb', 'amount': Decimal('24.239991263300007069991'), 'qty': Decimal('171.699991263299999999991')},
         {'id': 'ccc', 'amount': Decimal('-0.000008736700000000009'), 'qty': Decimal('-1201.900008736700000000009')}],
    ],
    [
        #  string concat
        [{'a': 'bb'}, {'a': 'cc'}],
        ['a'], False, True, [['+', ' a']],
        [{'a': 'bb a'}, {'a': 'cc a'}],
    ],
    [
        #  string concat with None
        [{'a': None}, {'a': 'cc'}],
        ['a'], False, True, [['+', ' a']],
        [{'a': None}, {'a': 'cc a'}],
    ],
    [
        #  Decimal sum with None
        [{'a': None}, {'a': 200}],
        ['a'], True, True, [['+', 300]],
        [{'a': None}, {'a': Decimal('500')}],
    ],
])
def test_calc(data, fields, in_decimal, ignore_null, operations, expected):
    behavior = CalcBehavior(fields=fields, operations=operations, in_decimal=in_decimal, ignore_null=ignore_null)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


@pytest.mark.parametrize('operations, in_decimal, expected_error', [
    [[[]], False, 'Empty operation'],
    [[['+', 10], ['*']], False, 'Operator "*" is not unary'],
    [[['*', 3], ['//', 10]], False, 'Unknown operator "//"'],
    [[['-', 'aa']], False, 'Value is not numeric at "[\'-\', \'aa\']"'],
    [[['+', 'aa']], True, 'Value is not numeric at "[\'+\', \'aa\']"'],
])
def test_calc_raise_on_wrong_operations(operations, in_decimal, expected_error):
    with pytest.raises(ValueError) as e:
        CalcBehavior(fields=['test'], operations=operations, in_decimal=in_decimal)
    assert str(e.value) == expected_error


# D_INSTALLS - skip rows with empty fielddate, install_new or place_id in nested csv
@pytest.mark.parametrize('data, expected', [
    [
        [
            {'id': 1, 'fielddate': 'a', 'install_new': 1,
                'path': '\tR\tWebsites\tpunto.browser.yandex.ru\tInstallation\t100\t200'},
            {'id': 2, 'fielddate': 'a', 'install_new': None,  # <-- no install_new
                'path': '\tR\tWebsites\tpunto.browser.yandex.ru\tInstallation\t100\t200'},
            {'id': 3, 'fielddate': None, 'install_new': 1,  # <-- no fielddate
                'path': '\tR\tWebsites\tpunto.browser.yandex.ru\tInstallation\t100\t200'},
            {'id': 4, 'fielddate': 'a', 'install_new': 1,  # <-- no place_id
                'path': '\tR\tWebsites\tpunto.browser.yandex.ru\tInstallation\t\t200'},
            {'id': 5, 'fielddate': 'b', 'install_new': 2,  # <-- full row
                'path': '\tR\tWebsites\tpunto.browser.yandex.ru\tInstallation\t200\t300'},
            {'id': 6, 'fielddate': 'c', 'install_new': 3,  # <-- without vid
                'path': '\tR\tWebsites\tpunto.browser.yandex.ru\tInstallation\t400\t'},
        ],
        [
            {'id': 1, 'fielddate': 'a', 'install_new': 1,
                'path': '\tR\tWebsites\tpunto.browser.yandex.ru\tInstallation\t100\t200',
                'vid': 200, 'place_id': 100},
            {'id': 5, 'fielddate': 'b', 'install_new': 2,  # <-- full row
                'path': '\tR\tWebsites\tpunto.browser.yandex.ru\tInstallation\t200\t300',
                'vid': 300, 'place_id': 200},
            {'id': 6, 'fielddate': 'c', 'install_new': 3,  # <-- without vid
                'path': '\tR\tWebsites\tpunto.browser.yandex.ru\tInstallation\t400\t',
                'vid': None, 'place_id': 400},
        ]
    ]
])
def test_d_installs(data, expected):
    behavior = DInstallsBehavior()
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# ADDAPPTER2 - raise error if type not install_count or dt != start_dt
@pytest.mark.parametrize('data, start_dt, expected_error', [
    [
        [{'id': 1, 'dt': datetime(2019, 9, 1, 0, 0, 0), '_type': 'install_count'},
         {'id': 2, 'dt': datetime(2019, 9, 1, 0, 0, 0), '_type': 'install_count'}],
        datetime(2019, 9, 1, 0, 0, 0), ''
    ],
    [
        [{'id': 1, 'dt': datetime(2019, 9, 2, 0, 0, 0), '_type': 'install_count'},
         {'id': 2, 'dt': datetime(2019, 9, 1, 0, 0, 0), '_type': 'install_count'}],
        datetime(2019, 9, 2, 0, 0, 0), 'Date should be equal start_dt. Got'
    ],
    [
        [{'id': 1, 'dt': datetime(2019, 9, 1, 0, 0, 0), '_type': 'install_count'},
         {'id': 2, 'dt': datetime(2019, 9, 1, 0, 0, 0), '_type': 'wrong'}],
        datetime(2019, 9, 1, 0, 0, 0), 'Type should be install_count. Got'
    ]
])
def test_adapter2(data, start_dt, expected_error):
    behavior = Adapter2Behavior(start_dt=start_dt)
    if expected_error:
        with pytest.raises(ValueError) as e:
            list(behavior.process(data))
        assert str(e.value).startswith(expected_error)
    else:
        actual = list(behavior.process(data))
        assert actual == data, 'Processed rows are different from expected'


# TAGS3 - skip all if start_dt in [2015, 6, 1, 2015, 6, 10)
@pytest.mark.parametrize('data, start_dt, expected', [
    [
        [{'id': 'a'}], datetime(2019, 9, 1), [{'id': 'a'}],
    ],
    [
        [{'id': 'a'}], date(2019, 9, 1), [{'id': 'a'}],
    ],
    [
        [{'id': 'a'}], datetime(2019, 9, 10), [{'id': 'a'}],
    ],
    [
        [{'id': 'a'}], date(2019, 9, 10), [{'id': 'a'}],
    ],
    [
        [{'id': 'a'}], datetime(2015, 6, 1), [],
    ],
    [
        [{'id': 'a'}], date(2015, 6, 1), [],
    ],
    [
        [{'id': 'a'}], datetime(2015, 6, 5), [],
    ],
    [
        [{'id': 'a'}], date(2015, 6, 5), [],
    ],
])
def test_tags3(data, start_dt, expected):
    behavior = Tags3Behavior(start_dt=start_dt)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# CONVERT TIMEZONE
@pytest.mark.parametrize('data, fields, timezone, expected', [
    [
        [{'dt': '2019-03-04T23:17:33.804045+00:00'}],
        ['dt'], 'Europe/Moscow',
        [{'dt': datetime(2019, 3, 5, 2, 17, 33)}],
    ],
    [
        [{'dt': '2019-09-01T00:00:00.804045+03:00'}],
        ['dt'], 'Europe/Moscow',
        [{'dt': datetime(2019, 9, 1)}]
    ],
    [
        [
            {'dt': '2019-09-01T01:00:00.804045+00:00', 'update_dt': '2019-10-01T02:00:00.804045+00:00'},
            {'dt': '2019-10-01T02:12:34.804045+00:00', 'update_dt': '2019-11-01T02:34:15.804045+00:00'},
        ],
        ['dt', 'update_dt'], 'Europe/Moscow',
        [
            {'dt': datetime(2019, 9, 1, 4), 'update_dt': datetime(2019, 10, 1, 5)},
            {'dt': datetime(2019, 10, 1, 5, 12, 34), 'update_dt': datetime(2019, 11, 1, 5, 34, 15)},
        ]
    ]
])
def test_convert_timezone(data, fields, timezone, expected):
    behavior = ConvertTimezoneBehavior(fields=fields, timezone=timezone)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# DATEUTIL PARSE
@pytest.mark.parametrize('data, field, format, expected', [
    [
        [{'dt': '2019-03-04'}],
        'dt', '%Y-%m-%d',
        [{'dt': datetime(2019, 3, 4)}],
    ],
    [
        [
            {'dt': '2019-09'},
            {'dt': '2020-01'},
        ],
        'dt', '%Y-%m',
        [
            {'dt': datetime(2019, 9, 1)},
            {'dt': datetime(2020, 1, 1)},
        ]
    ]
])
def test_parse_date_by_format(data, field, format, expected):
    behavior = ParseDateBehavior(field=field, format=format)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'



# EVALUATE TRUTHY
@pytest.mark.parametrize('data, fields, default, expected', [
    [
        [{'val': True}, {'val': 'true'}, {'val': 'Y'}, {'val': '1'}, {},
         {'val': False}, {'val': 'false'}, {'val': 'n'}, {'val': '0'}],
        ['val'],
        False,
        [{'val': i < 4} for i in range(0, 9)]
    ],
    [
        [{'f': 1, 'val': False}, {'f': 'off', }, {}],
        ['f', 'val'], True,
        [{'f': True, 'val': False}, {'f': False, 'val': True}, {'f': True, 'val': True}],
    ]
])
def test_evaluate_truthy(data, fields, default, expected):
    behavior = EvaluateTruthyBehavior(fields=fields, default=default)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# EXTRACTS FIELD BEHAVIOR
@pytest.mark.parametrize('data, field, map_, to, expected', [
    [
        [
            {'id': 1, 'payload': {'alias_id': 100, 'value': 'aaa'}},
            {'id': 2, 'payload': {'alias_id': 200, 'value': 'ccc'}},
            {'id': 3, 'payload': {'alias_id': 200, 'value': 'ddd'}},
            {'id': 4, 'payload': {'alias_id': 200}},
            {'id': 5, 'payload': None},
        ],
        'payload', [['alias_id', 'alias_id'], ['value', 'description']], None,
        [
            {'id': 1, 'alias_id': 100, 'description': 'aaa', 'payload': {'alias_id': 100, 'value': 'aaa'}},
            {'id': 2, 'alias_id': 200, 'description': 'ccc', 'payload': {'alias_id': 200, 'value': 'ccc'}},
            {'id': 3, 'alias_id': 200, 'description': 'ddd', 'payload': {'alias_id': 200, 'value': 'ddd'}},
            {'id': 4, 'alias_id': 200, 'description': None, 'payload': {'alias_id': 200}},
            {'id': 5, 'alias_id': None, 'description': None, 'payload': None},
        ],
    ],
    [
        [
            {'id': 1, 'payload': {'alias_id': 100, 'value': 'aaa'}},
            {'id': 2, 'payload': {'alias_id': 200, 'value': 'ccc'}},
        ],
        'payload', [['value', 'payload'], ['alias_id', 'alias_id']], None,
        [
            {'id': 1, 'alias_id': 100, 'payload': 'aaa'},
            {'id': 2, 'alias_id': 200, 'payload': 'ccc'},
        ],
    ],
    [
        [
            {'id': 1, 'payload': {'alias_id': 100, 'value': 'aaa'}},
            {'id': 2, 'payload': {'alias_id': 200, 'value': 'ccc'}},
        ],
        'payload', [['value', 'payload'], ['alias_id', 'alias_id']], 'inner',
        [
            {'id': 1, 'payload': {'alias_id': 100, 'value': 'aaa'}, 'inner': {'alias_id': 100, 'payload': 'aaa'}},
            {'id': 2, 'payload': {'alias_id': 200, 'value': 'ccc'}, 'inner': {'alias_id': 200, 'payload': 'ccc'}},
        ],
    ],
])
def test_extract_fields(data, field, map_, to, expected):
    behavior = ExtractFieldsBehavior(field=field, map=map_, to=to)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# EXTRACT FIELDS AS JSON
@pytest.mark.parametrize('data, field, to, fields, expected', [
    [
        [
            {'id': 1, 'payload': {'name': 'a', 'dt': '2019-09-01'}},
            {'id': 2, 'payload': {'name': 'b', 'dt': '2019-09-02'}},
            {'id': 3, 'payload': {'name': 'c', 'dt': '2019-09-03'}},
            {'id': 4, 'payload': {'name': 'd', 'dt': '2019-09-04'}},
        ],
        'payload', 'json', ['name'],
        [
            {'id': 1, 'json': '{"name": "a"}', 'payload': {'name': 'a', 'dt': '2019-09-01'}},
            {'id': 2, 'json': '{"name": "b"}', 'payload': {'name': 'b', 'dt': '2019-09-02'}},
            {'id': 3, 'json': '{"name": "c"}', 'payload': {'name': 'c', 'dt': '2019-09-03'}},
            {'id': 4, 'json': '{"name": "d"}', 'payload': {'name': 'd', 'dt': '2019-09-04'}},
        ]
    ],
    [
        [
            {'id': 1, 'payload': {'name': 'a', 'dt': '2019-09-01'}},
            {'id': 2, 'payload': {'name': 'b', 'dt': '2019-09-02'}},
            {'id': 3, 'payload': {'name': 'c', 'dt': '2019-09-03'}},
            {'id': 4, 'payload': {'name': 'd', 'dt': '2019-09-04'}},
        ],
        'payload', 'payload', ['name', 'dt', 'miss'],
        [
            {'id': 1, 'payload': '{"dt": "2019-09-01", "name": "a"}'},
            {'id': 2, 'payload': '{"dt": "2019-09-02", "name": "b"}'},
            {'id': 3, 'payload': '{"dt": "2019-09-03", "name": "c"}'},
            {'id': 4, 'payload': '{"dt": "2019-09-04", "name": "d"}'},
        ]
    ],
])
def test_extract_fields_as_json(data, field, to, fields, expected):
    behavior = ExtractFieldsAsJsonBehavior(field=field, to=to, fields=fields)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# APPLY DATED NDS
@pytest.mark.parametrize('data, fields, nds, expected', [
    [
        [
            {'id': 1, 'qty': Decimal('2.22')},
            {'id': 2, 'qty': Decimal('0.00')},
            {'id': 3, 'qty': Decimal('-1.11')},
        ], ['qty'], Decimal('1.18'),
        [
            {'id': 1, 'qty': Decimal('2.6196')},
            {'id': 2, 'qty': Decimal('0.00')},
            {'id': 3, 'qty': Decimal('-1.3098')},
        ]
    ],
])
def test_apply_dated_nds(data, fields, nds, expected):
    mapper.Nds.get_nds_koef_on_dt = mock.MagicMock(
        name='nds_koef', side_effect=lambda *args, **kwargs: nds)
    behavior = ApplyDatedNDSBehavior(fields=fields, session=None, start_dt=None)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# raise on none nds
def test_apply_dated_nds_not_found_dated_nds():
    mapper.Nds.get_nds_koef_on_dt = mock.MagicMock(
        name='nds_koef', side_effect=lambda *args, **kwargs: None)
    with pytest.raises(ValueError) as e:
        ApplyDatedNDSBehavior(fields=[], session=None, start_dt='FAKE')
    assert str(e.value) == 'NDS coefficient on "FAKE" not found'


# UNIQUE
@pytest.mark.parametrize('data, fields, exists, expected', [
    [
        [{'id': i} for i in range(0, 3)],
        ['id'], False,
        [{'id': i} for i in range(0, 3)],
    ],
    [
        [{'id': i} for i in range(0, 3)],
        ['id'], True, [],
    ],
])
def test_unique(data, fields, exists, expected):
    session = mock.MagicMock()
    session.query().filter_by().exists = lambda **kw: exists
    behavior = UniqueBehavior(fields=fields, session=session, table='t_completion_source')
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# GROUP
@pytest.mark.parametrize('data, by, fields, expected', [
    #  first
    [
        [
            {'id': 1, 'dt': '2019-09-01'},
            {'id': 2, 'dt': '2019-09-02'},
            {'id': 2, 'dt': '2019-09-03'},
        ], ['id'],
        [{'field': 'id'}, {'field': 'dt', 'operator': 'first'}],
        [
            {'id': 1, 'dt': '2019-09-01'},
            {'id': 2, 'dt': '2019-09-02'},
        ]
    ],
    #  sum
    [
        [
            {'id': 1, 'amount': '1.22'},
            {'id': 2, 'amount': '2.33'},
            {'id': 2, 'amount': '3.44'},
        ], ['id'],
        [{'field': 'id'}, {'field': 'amount', 'operator': 'sum'}],
        [
            {'id': 1, 'amount': Decimal('1.22')},
            {'id': 2, 'amount': Decimal('5.77')},
        ]
    ],
    # multiple columns in by expr and multiple operators
    [
        [
            {'id': 1, 'type': 'aaa', 'amount': '1.11', 'dt': '2019-09-01'},
            {'id': 1, 'type': 'bbb', 'amount': '2.22', 'dt': '2019-09-02'},
            {'id': 1, 'type': 'bbb', 'amount': '3.33', 'dt': '2019-09-03'},
            {'id': 2, 'type': 'ccc', 'amount': '4.44', 'dt': '2019-09-04'},
            {'id': 2, 'type': 'ccc', 'amount': '5.55', 'dt': '2019-09-05'},
            {'id': 3, 'type': 'ddd', 'amount': '6.66', 'dt': '2019-09-06'},
        ], ['id', 'type'],
        [{'field': 'id'}, {'field': 'type'},
         {'field': 'amount', 'operator': 'sum'}, {'field': 'dt', 'operator': 'first'}],
        [
            {'id': 1, 'type': 'aaa', 'amount': Decimal('1.11'), 'dt': '2019-09-01'},
            {'id': 1, 'type': 'bbb', 'amount': Decimal('5.55'), 'dt': '2019-09-02'},
            {'id': 2, 'type': 'ccc', 'amount': Decimal('9.99'), 'dt': '2019-09-04'},
            {'id': 3, 'type': 'ddd', 'amount': Decimal('6.66'), 'dt': '2019-09-06'},
        ]
    ],
])
def test_group(data, by, fields, expected):
    behavior = GroupBehavior(by=by, fields=fields)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


@pytest.mark.parametrize('by, fields, expected_error', [
    # empty by expr
    [
        [], [{'field': 'a'}], 'Columns "by" unspecified',
    ],
    # field not specified
    [
        ['id'], [{}], 'Field required at',
    ],
    # unknown operator
    [
        ['id'], [{'field': 'a', 'operator': '+'}], 'Unknown operator',
    ],
    # use operator with index field
    [
        ['id', 'name'], [{'field': 'some', 'operator': 'sum'}, {'field': 'id', 'operator': 'sum'}],
        'For field "id" do not use an aggregate operation',
    ],
    [
        ['id', 'name'], [{'field': 'id'}, {'field': 'wrong'}],
        'For field "wrong" use an aggregate operation or include field in "by" list',
    ],
])
def test_group_raise(by, fields, expected_error):
    with pytest.raises(ValueError) as e:
        GroupBehavior(by=by, fields=fields)
    assert str(e.value).startswith(expected_error)


# COMPLETION FILTER
@pytest.mark.parametrize('data, field, values, expected', [
    [
        [{'product_id': i, 'price': Decimal(i * 10)} for i in range(7, 19)],
        'product_id', [1, 8, 16],
        [{'product_id': i, 'price': Decimal(i * 10)} for i in [8, 16]],
    ]
])
def test_completion_filter(data, field, values, expected):
    filter_ = CompletionFilter({field: values})
    behavior = CompletionFilterBehavior(filter_)
    actual = list(behavior.process(data))
    assert actual == expected, 'Processed rows are different from expected'


# RAISE IF CLUSTER IS NOT DAY
@pytest.mark.parametrize('data', [
    [{'id': 1, 'amount': Decimal('1.11')}, {'id': 2, 'amount': Decimal('5.55')}, {'id': 3, 'amount': Decimal('6.66')}],
])
def test_raise_if_cluster_is_not_day(data):
    start_dt = datetime(2019, 9, 1)
    end_dt = start_dt
    behavior = RaiseIfClusterIsNotDay(start_dt=start_dt, end_dt=end_dt)
    actual = list(behavior.process(data))
    assert actual == data, 'Processed rows are different from expected'


@pytest.mark.parametrize('start_dt, end_dt', [
    [datetime(2019, 9, 1), datetime(2019, 9, 2)]
])
def test_raise_if_cluster_is_not_day_raise(start_dt, end_dt):
    with pytest.raises(ValueError) as e:
        RaiseIfClusterIsNotDay(start_dt=start_dt, end_dt=end_dt)
    assert str(e.value) == 'Invalid start_dt={} and end_dt={}, set date_cluster=1 in config'.format(start_dt, end_dt)
