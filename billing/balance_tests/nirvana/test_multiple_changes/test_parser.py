# -*- coding: utf-8 -*-

import pytest

from balance.actions.nirvana.operations.multiple_changes.parser import *
from balance.actions.nirvana.operations.multiple_changes.exceptions import *
from decimal import Decimal


class Parser(ValueParser):
    _config = {
        'contract': {
            'external_id': {'data_type': 'str'},
            'client_id': {'data_type': 'int'},
            'services': {'data_type': 'list_int'},
            'projects': {'data_type': 'list_str'},
            'is_faxed': {'data_type': 'dt_iso'},
            'products_download': {'data_type': 'json'}
        },
        'collateral': {
            'num': {'data_type': 'str'},
            'is_signed': {'data_type': 'dt', 'nullable': True},
            'nds': {'data_type': 'num', 'allowed_values': {0, 18}},
            'add_services': {'data_type': 'list'},
        },
    }


@pytest.mark.parametrize(
    'parser_args, exc',
    [
        (['contractt', 'external_id', 'KEK/1'], KeyError),
        (['contract', 'type', 'IMARETARD'], AttributeNotAllowed),
    ]
)
def test_forbidden_attr(parser_args, exc):
    with pytest.raises(exc):
        Parser(*parser_args)


def test_no_data_type():
    vp = Parser('contract', 'external_id', 'KEK/1')
    vp.attr_desc = {'external_id': {}}
    with pytest.raises(NoDataTypeFound):
        vp.data_type


def test_value_not_allowed():
    vp = Parser('collateral', 'nds', 20)
    with pytest.raises(ValueNotAllowed):
        vp.val


def test_not_nullable():
    vp = Parser('contract', 'external_id', None)
    with pytest.raises(CanNotBeNull):
        vp.val


@pytest.mark.parametrize(
    'date',
    [
        dt.datetime(2019, 1, 1),
        '2019-01-01',
        '01.01.2019',
        '2019-01-01T01:00:00',
        '2019-01-01 01:00:00',
        '2019-01-01T01:00:00Z',
        '2019-01-01T01:00:00+0300',
        '2019-01-01T01:00:00+0300Z'
    ]
)
def test_valid_date(date):
    req_dt = dt.datetime(2019, 1, 1)

    vp = Parser('collateral', 'is_signed', date)
    assert vp.val == req_dt


@pytest.mark.parametrize(
    'date',
    [
        '2019-01-01T01:00:00',
        '2019-01-01T01:00:00Z',
        '2019-01-01T01:00:00+0300',
        '2019-01-01T01:00:00+0300Z'
    ]
)
def test_valid_date_iso(date):
    req_dt = '2019-01-01T00:00:00'

    vp = Parser('contract', 'is_faxed', date)
    assert vp.val == req_dt


def test_bad_date():
    vp = Parser('collateral', 'is_signed', '01-01-2019')
    with pytest.raises(AttributeParsingError):
        vp.val


def test_valid_int():
    req_num = 123

    vp = Parser('contract', 'client_id', '123')
    assert vp.val == req_num


@pytest.mark.parametrize(
    'num',
    [Decimal('18'), 18, '18', 18.0, '18, 00']
)
def test_valid_num(num):
    req_num = 18

    vp = Parser('collateral', 'nds', num)
    assert vp.val == req_num


def test_bad_num():
    vp = Parser('collateral', 'nds', '18.00.00')
    with pytest.raises(AttributeParsingError):
        vp.val


@pytest.mark.parametrize(
    'num_str, req_str',
    [
        (u'Ф-01', u'Ф-01'),
        (u'  Ф-01 ', u'Ф-01'),
        (1, u'1')
    ]
)
def test_str(num_str, req_str):
    vp = Parser('collateral', 'num', num_str)
    assert vp.val == req_str


def test_list():
    _list = [1, 2, 3]
    req_list = [1, 2, 3]

    vp = Parser('collateral', 'add_services', _list)
    assert vp.val == req_list


def test_list_int():
    _list = '1, 2, 3'
    req_list = [1, 2, 3]

    vp = Parser('contract', 'services', _list)
    assert vp.val == req_list


def test_list_str():
    _list = 'Ooh, ee, ooh, ah, ah'
    req_list = ['Ooh', 'ee', 'ooh', 'ah', 'ah']

    vp = Parser('contract', 'projects', _list)
    assert vp.val == req_list


def test_json():
    req_res = {u'product': 1, u'product2': u'2'}
    incom_val = '{"product": 1,"product2": "2"}'

    vp = Parser('contract', 'products_download', incom_val)
    assert vp.val == req_res
