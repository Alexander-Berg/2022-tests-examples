# -*- coding: utf-8 -*-
"""Вспомогательные функции для тестирования ручек CreateCommonContract/CreateOffer.

Примеры использования есть в test_xmlrpc_create_common_contract_for_adfox.py
"""

from balance import mapper
from billing.contract_iface import contract_meta


__author__ = 'quark'


def get_contract_attr(obj, attr):
    for o in (obj.current_state(), obj, obj.col0):
        try:
            return getattr(o, attr.lower())
        except AttributeError:
            continue
    return None


def contract_contains(contract, params=None):
    if params is None:
        return

    for param_key in params:
        try:
            val = get_contract_attr(contract, param_key)
        except AttributeError:
            assert False, 'Ошибка в договоре, не найден атрибут: {}'.format(param_key)
        if val != params[param_key]:
            if isinstance(val, list) and set(val) == set(params[param_key]):
                continue
            if isinstance(val, contract_meta.ContractTypes) and val.type == params[param_key].type:
                continue
            assert val == params[param_key], u'Ошибка в договоре:  {}'.format(param_key)


def check_create_contract_res_params(session, res, manager, expected_params):
    assert isinstance(res, dict)
    assert set(res.keys()) == {'ID', 'EXTERNAL_ID'}

    contract_obj = session.query(mapper.Contract).get(res['ID'])
    assert contract_obj.external_id == res['EXTERNAL_ID']
    contract_contains(contract_obj, expected_params)
