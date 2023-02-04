# -*- coding: utf-8 -*-

import pytest

from balance import exc
from balance.processors.oebs.contract import process_contract_impl
from tests.balance_tests.oebs.common import patch_oebs
from tests.balance_tests.oebs.conftest import (create_contract,
                                               create_firm,
                                               create_person)


def check_export(export_obj, state, error):
    assert export_obj.state == state
    assert export_obj.error == error


def test_w_person_wo_cached_firm(session, contract):
    """
    Выгружаем договор с фирмой, фирма договора не закеширована в плательщике.
    Проверяем, что фирма закешировалась.
    """
    session.config.__dict__['USE_FIRMS_FROM_PERSON_FIRM_CACHE'] = 1

    patch_contract_dao, patch_customer_dao, _, patch_out_cursor = patch_oebs()

    with patch_contract_dao, patch_customer_dao, pytest.raises(exc.DEFERRED_ERROR):
        process_contract_impl(contract, contract.firm, patch_out_cursor, 2)
    assert len(contract.person.person_firms) == 1
    assert contract.person.person_firms[0].firm == contract.firm
    assert contract.person.person_firms[0].oebs_export_dt is None


def test_delayed_contract_wo_cached_firm(session, country):
    """
    Выгружаем договор с фирмой, фирма договора не закеширована в плательщике.
    Проверяем, что при выгрузке договора:
    - фирма закешировалась
    - экcпорт договора упал с исключением DEFERRED_ERROR

    """
    session.config.__dict__['USE_FIRMS_FROM_PERSON_FIRM_CACHE'] = 1

    firm = create_firm(session, w_firm_export=True, country=country)
    person = create_person(session, country=country)
    contract = create_contract(session, person=person, firm_id=firm.id)

    patch_contract_dao, patch_customer_dao, _, patch_out_cursor = patch_oebs()
    contract.person.exports['OEBS'].state = 1

    with patch_contract_dao, patch_customer_dao, pytest.raises(exc.DEFERRED_ERROR) as exc_info:
        process_contract_impl(contract, contract.firm, patch_out_cursor, 2)
    error_msg = 'Export of {} has been deferred because its' \
                ' person (id = {}) is not in OEBS.'.format(contract,
                                                    contract.person.id)
    assert contract.person.person_firms[0].oebs_export_dt is None
    assert exc_info.value.msg == error_msg
    check_export(contract.exports['OEBS'], 0, None)


def test_contract_w_cached_firm_exported_person(session, country):
    """
    Выгружаем договор с фирмой, фирма закеширована в плательщике и плательщик уже выгружался в ОЕБС
     (заполнена oebs_export_dt)
    """
    session.config.__dict__['USE_FIRMS_FROM_PERSON_FIRM_CACHE'] = 1

    firm = create_firm(session, w_firm_export=True, country=country)
    person = create_person(session, country=country)
    contract = create_contract(session, person=person, firm_id=firm.id)
    person.firms = [contract.firm]
    person.person_firms[0].oebs_export_dt = session.now()
    person.exports['OEBS'].state = 1

    patch_contract_dao, patch_customer_dao, _, patch_out_cursor = patch_oebs()

    with patch_contract_dao, patch_customer_dao:
        process_contract_impl(contract, contract.firm, patch_out_cursor, 2)
    session.flush()
    check_export(contract.person.exports['OEBS'], 1, None)
