# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal

import pytest

from balance import mapper
from billing.contract_iface.contract_meta import collateral_types, contract_attributes

from tests.balance_tests.contract.contract_common import (create_contract,
                                                          create_contract_type,
                                                          create_attr,
                                                          create_collateral_type)

NOW = datetime.datetime.now().replace(microsecond=0)


def get_attribute_values_from_db(session, attribute_batch_id, attr_name):
    return session.query(mapper.AttributeValue).\
        filter_by(code=attr_name.upper(), attribute_batch_id=attribute_batch_id).\
        order_by(mapper.AttributeValue.key_num).\
        all()


@pytest.mark.parametrize('pytype, test_attr_value, expected_db_attrs_list',
                         [
                             ('int', 12345, [{'value_num': 12345}]),
                             ('int', '12345', [{'value_num': 12345}]),
                             ('str', 12345, [{'value_str': '12345'}]),
                             ('date', NOW, [{'value_dt': NOW}]),
                             ('money', 666, [{'value_num': 666}]),
                             ('money', '666.66', [{'value_num': Decimal('666.66')}]),

                             ('jsondict', {666: {'rdr': 3}, 566: 'rrtr'},
                              [{'value_str': '\"rrtr\"', 'key_num': 566},
                               {'value_str': '{\"rdr\": 3}', 'key_num': 666}]),

                             ('intdict', {666: '9998', 778: 5556},
                              [{'value_num': 9998, 'key_num': 666},
                               {'value_num': 5556, 'key_num': 778}]),

                             ('intset', {4, 6},
                              [{'value_num': 1, 'key_num': 4},
                               {'value_num': 1, 'key_num': 6}]),

                             ('strset', {56: '3', 33: '5'},
                              [{'value_num': 1, 'key_num': 33, 'value_str': '5'},
                               {'value_num': 1, 'key_num': 56, 'value_str': '3'}]),

                             ('strdict', {67: '3', 45: 'aee'},
                              [{'value_num': 1, 'value_str': 'aee', 'key_num': 45},
                               {'value_num': 1, 'value_str': '3', 'key_num': 67}]),

                             ('ticket', 'rrr', [{'value_str': 'rrr'}]),

                             ('unheritableintdict', {666: '9998', 656: 454},
                              [{'value_num': 454, 'key_num': 656},
                               {'value_num': 9998, 'key_num': 666}]),

                             ('unheritableintset', {4, 6},
                              [{'value_num': 1, 'key_num': 4},
                               {'value_num': 1, 'key_num': 6}]),

                             ('unheritableintset', 5,
                              [{'value_num': 1, 'key_num': 5}]),

                             ('unheritablepromdict', {4: {'value_str': '3', 'value_num': '2'},
                                                      7: {'value_str': 'yy', 'value_num': ''}},
                              [{'key_num': 4, 'value_str': '3', 'value_num': 2},
                               {'key_num': 7, 'value_str': 'yy', 'value_num': None}]),

                             ('unheritablejsondict', {4: {'a': '3', 'b': 2}},
                              [{'key_num': 4, 'value_str': '{\"a\": \"3\", \"b\": 2}'}]),

                         ])
def test_set_attr(session, pytype, test_attr_value, expected_db_attrs_list):
    default_db_dict = {'value_num': None,
                       'value_str': None,
                       'value_dt': None,
                       'key_num': None}
    contract_type = create_contract_type(session)
    contract_attributes[contract_type] = create_attr(contract_type=contract_type, name='test_attr', pytype=pytype)
    contract = create_contract(session, ctype=contract_type, test_attr=test_attr_value)
    session.flush()
    session.expire(contract.col0)
    db_attrs_list = get_attribute_values_from_db(session, contract.col0.attribute_batch_id, 'test_attr')
    assert len(db_attrs_list) == len(expected_db_attrs_list)
    for row, expected_row in zip(db_attrs_list, expected_db_attrs_list):
        default_db_dict_copy = default_db_dict.copy()
        default_db_dict_copy.update(expected_row)
        for key, expected_value in default_db_dict_copy.items():
            assert getattr(row, key) == expected_value


@pytest.mark.parametrize('pytype, test_attr_value, expected_read_value',
                         [
                             ('int', 12345, 12345),
                             ('int', '12345', 12345),
                             ('str', 12345, '12345'),
                             ('date', NOW, NOW),
                             ('money', 666, 666),
                             ('money', '666.66', Decimal('666.66')),

                             ('jsondict', {666: {'rdr': 3}, 566: 'rrtr'}, {666: {'rdr': 3}, 566: 'rrtr'}),

                             ('intdict', {666: '9998', 778: 5556}, {666: 9998, 778: 5556}),

                             ('intset', {4, 6}, {4: 1, 6: 1}),

                             ('strset', {56: '3', 33: '5'}, {33: '5', 56: '3'}),

                             ('strdict', {67: '3', 45: 'aee'}, {45: 'aee', 67: '3'}),

                             ('ticket', 'rrr', 'rrr'),

                             ('unheritableintdict', {666: '9998', 656: 454}, {656: 454, 666: 9998}),

                             ('unheritableintset', {4, 6}, {4: 1, 6: 1}),

                             ('unheritablepromdict', {4: {'value_str': '3', 'value_num': '2'},
                                                      7: {'value_str': 'yy', 'value_num': ''}},
                              {4: {'value_str': '3', 'value_num': 2},
                               7: {'value_str': 'yy', 'value_num': None}}),

                             ('unheritablejsondict', {4: {'a': '3', 'b': 2}}, {4: {'a': '3', 'b': 2}}),

                         ])
def test_get_attr(session, pytype, test_attr_value, expected_read_value):
    contract_type = create_contract_type(session)
    contract_attributes[contract_type] = create_attr(contract_type=contract_type, name='test_attr', pytype=pytype)
    contract = create_contract(session, ctype=contract_type, test_attr=test_attr_value)
    session.flush()
    session.expire(contract.col0)
    assert contract.col0.test_attr == expected_read_value


@pytest.mark.parametrize(
    'pytype, test_attr_value, test_attr_value_after_set, expected_db_attrs_list, test_attr_value_after_get',
    [
        ('unheritableintdict',

         {666: 9998, 656: 454},
         {666: '55445', 44: 00},

         [{'value_num': 00, 'key_num': 44},
          {'value_num': None, 'key_num': 656},
          {'value_num': 55445, 'key_num': 666}],

         {666: 55445, 44: 00}),

        ('unheritableintset',
         {4, 6},
         {3, 6},

         [{'value_num': 1, 'key_num': 3},
          {'value_num': None, 'key_num': 4},
          {'value_num': 1, 'key_num': 6}],

         {3, 6}),

        ('unheritablepromdict',

         {4: {'value_str': '3', 'value_num': 2},
          7: {'value_str': 'yy', 'value_num': 5}},

         {4: {'value_str': '3', 'value_num': 31},
          6: {'value_str': 'yrt', 'value_num': '99'}},

         [{'key_num': 4, 'value_str': '3', 'value_num': 31},
          {'key_num': 6, 'value_str': 'yrt', 'value_num': 99},
          {'key_num': 7, 'value_str': None, 'value_num': None}],

         {4: {'value_str': '3', 'value_num': 31},
          6: {'value_str': 'yrt', 'value_num': 99},
          7: {'value_str': None, 'value_num': None}}),

        ('unheritablejsondict',

         {4: {'a': '3', 'b': 2}, 5: {'44': 4}},

         {5: {'44': 4}, 6: {'er': '2323'}},

         [{'key_num': 4, 'value_str': 'null'},
          {'key_num': 5, 'value_str': '{\"44\": 4}'},
          {'key_num': 6, 'value_str': '{\"er\": \"2323\"}'}],

         {5: {'44': 4}, 6: {'er': '2323'}}),

        ('jsondict',
         {666: {'rdr': 3}, 566: {'ttg': 4}},

         {666: {'rdr': 4}, 778: {'vbn': 86}},

         [{'value_str': 'null', 'key_num': 566},
          {'value_str': '{\"rdr\": 4}', 'key_num': 666},
          {'value_str': '{\"vbn\": 86}', 'key_num': 778}],

         {666: {'rdr': 4}, 778: {'vbn': 86}}),

        ('jsondict',
         {666: {'rdr': 3}, 566: {'ttg': 4}},

         None,

         [{'value_str': 'null', 'key_num': 566},
          {'value_str': 'null', 'key_num': 666}],

         {}),

        ('intdict',
         {666: 9998, 656: 454},

         {666: '55445', 44: 0},
         [{'value_num': 0, 'key_num': 44},
          {'value_num': None, 'key_num': 656},
          {'value_num': 55445, 'key_num': 666}],

         {666: 55445, 44: 0}),

        ('intdict',
         {666: 9998, 656: 454},

         None,
         [{'value_num': None, 'key_num': 656},
          {'value_num': None, 'key_num': 666}],

         {}),

        ('intset',
         {4, 6},
         {3, 6},
         [{'value_num': 1, 'key_num': 3},
          {'value_num': None, 'key_num': 4},
          {'value_num': 1, 'key_num': 6}],

         {3, 6}),

        ('intset',
         {4, 6},
         None,
         [{'value_num': None, 'key_num': 4},
          {'value_num': None, 'key_num': 6}],

         set()),

        ('strset',
         {56: '3', 33: '5'},
         {33: '6', 18: '7'},
         [{'value_num': 1, 'key_num': 18, 'value_str': '7'},
          {'value_num': 1, 'key_num': 33, 'value_str': '6'},
          {'value_num': None, 'key_num': 56, 'value_str': None}],

         {33: '6', 18: '7'}),

        ('strset',
         {56: '3', 33: '5'},
         None,
         [{'value_num': None, 'key_num': 33, 'value_str': None},
          {'value_num': None, 'key_num': 56, 'value_str': None}],

         {}),

        ('strdict',
         {67: '3', 45: 'aee'},
         {67: '8', 44: 'eer'},

         [{'value_num': 1, 'value_str': 'eer', 'key_num': 44},
          {'value_num': None, 'value_str': None, 'key_num': 45},
          {'value_num': 1, 'value_str': '8', 'key_num': 67}],

         {67: '8', 44: 'eer'}),

        ('strdict',
         {67: '3', 45: 'aee'},
         None,

         [{'value_num': None, 'value_str': None, 'key_num': 45},
          {'value_num': None, 'value_str': None, 'key_num': 67}],

         {}),

        ('int',
         '5',
         '',
         [{'value_num': None, }],
         None),
    ])
def test_contract_attrs_updated(session, pytype, test_attr_value, test_attr_value_after_set,
                                expected_db_attrs_list, test_attr_value_after_get):
    """
    Задаем атрибут в нулевом допнике, а потом апдейтим в нем же. Проверяем, что атрибут правильно разложился в базу,
     правильно читается с базы
    :param session:
    :param pytype: тип атрибута договора
    :param test_attr_value: первоначальное значение атрибута
    :param test_attr_value_after_set: обновленное значение атрибута
    :param expected_db_attrs_list: как обновленный атрибут раскладывается в базе
    :param test_attr_value_after_get: как выглядит getattr обновленного атрибута
    """
    default_db_dict = {'value_num': None,
                       'value_str': None,
                       'value_dt': None,
                       'key_num': None}
    contract_type = create_contract_type(session)
    contract_attributes[contract_type] = create_attr(contract_type=contract_type, name='test_attr', pytype=pytype)
    contract = create_contract(session, ctype=contract_type, test_attr=test_attr_value)
    session.flush()
    session.expire(contract.col0)
    contract.col0.test_attr = test_attr_value_after_set
    session.flush()
    session.expire(contract.col0)
    db_attrs_list = get_attribute_values_from_db(session, contract.col0.attribute_batch_id, 'test_attr')
    assert len(db_attrs_list) == len(expected_db_attrs_list)
    for row, expected_row in zip(db_attrs_list, expected_db_attrs_list):
        default_db_dict_copy = default_db_dict.copy()
        default_db_dict_copy.update(expected_row)
        for key, expected_value in default_db_dict_copy.items():
            assert getattr(row, key) == expected_value
    assert contract.current_state().test_attr == test_attr_value_after_get


@pytest.mark.parametrize(
    'pytype, zero_collateral_value, second_collateral_value_after, expected_result_value',
    [
        ('unheritableintdict',
         {666: 9998, 656: '454'},
         {666: 55445, 44: 00},
         {666: 55445, 44: 00, 656: 454}),

        ('intdict',
         {666: 9998, 656: 454},
         {666: '55445', 44: 0},
         {44: 0, 666: 55445}),

        ('unheritableintset',
         {4, 6},
         {3, 6},
         {3, 4, 6}),

        ('intset',
         {4, 6},
         {3, 6},
         {3, 6}),

        ('unheritablejsondict',
         {4: {'a': '3', 'b': 2}, 5: {'44': 4}},
         {5: {'44': 4}, 6: {'er': '2323'}},
         {5: {'44': 4}, 6: {'er': '2323'}, 4: {'a': '3', 'b': 2}}),

        ('jsondict',
         {666: {'rdr': 3}, 566: {'ttg': 4}},
         {666: {'rd': 46}, 778: {'vbn': 86}},
         {666: {u'rd': 46}, 778: {u'vbn': 86}}),

        ('unheritablepromdict',
         {4: {'value_str': '3', 'value_num': 2},
          7: {'value_str': 'yy', 'value_num': 5}},

         {4: {'value_str': '3', 'value_num': '31'},
          6: {'value_str': 'yrt', 'value_num': ''}},

         {4: {'value_str': '3', 'value_num': 31},
          6: {'value_str': 'yrt', 'value_num': None},
          7: {'value_str': 'yy', 'value_num': 5}}),

        ('strset',
         {56: '3', 33: '5'},
         {33: '6', 18: '7'},
         {18: '7', 33: '6'}
         ),

        ('strdict',
         {67: '3', 45: 'aee'},
         {67: '8', 44: 'eer'},
         {44: 'eer', 67: '8'}),
    ])
def test_contract_attrs_updated_by_collateral(session, pytype, zero_collateral_value, second_collateral_value_after,
                                              expected_result_value):
    """
    Задаем атрибут в нулевом допнике, а потом апдейтим в другом допнике.
    Проверяем, как атрибут выглядит в договоре
    :param session:
    :param pytype: тип атрибута договора
    :param zero_collateral_value: первоначальное значение атрибута
    :param second_collateral_value_after: значение атрибута в допнике
    :param expected_result_value: итоговое значение атрибуты в договоре
    """
    contract_type = create_contract_type(session)
    create_collateral_type(contract_type)
    contract_attributes[contract_type] = create_attr(contract_type=contract_type, name='test_attr', pytype=pytype)
    contract = create_contract(session, ctype=contract_type, test_attr=zero_collateral_value)
    contract.append_collateral(dt=NOW, test_attr=second_collateral_value_after,
                               collateral_type=collateral_types[contract_type][1])
    session.flush()
    session.expire(contract)
    assert contract.current_state().test_attr == expected_result_value
