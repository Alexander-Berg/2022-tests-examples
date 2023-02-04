#!/usr/bin/python
# -*- coding: utf-8 -*-

import pytest
import datetime

from tests.tutils import shift_date
from balance.constants import FirmId, PREPAY_PAYMENT_TYPE, DIRECT_SERVICE_ID
from billing.contract_iface import ContractTypeId
from tests.balance_tests.contract.contract_common import create_contract, set_unique_external_id, set_same_external_id
from balance import exc
from balance import mapper

ACTIVE_CONTRACT = {}
NOT_ACTIVE_CONTRACT = {'is_suspended': shift_date(days=-3)}

base_params = dict(
    commission=ContractTypeId.NON_AGENCY,
    firm=FirmId.YANDEX_OOO,
    payment_type=PREPAY_PAYMENT_TYPE,
    services={DIRECT_SERVICE_ID},
    is_signed=datetime.datetime.now(),
)


@pytest.mark.parametrize('params', [
    dict(contracts_params=[ACTIVE_CONTRACT, ACTIVE_CONTRACT], expected_exc=exc.NOT_UNIQUE),
    dict(contracts_params=[NOT_ACTIVE_CONTRACT, NOT_ACTIVE_CONTRACT], expected_exc=exc.ACTIVE_CONTRACT_NOT_FOUND),
    dict(contracts_params=[ACTIVE_CONTRACT, NOT_ACTIVE_CONTRACT]),
    dict(contracts_params=[ACTIVE_CONTRACT, NOT_ACTIVE_CONTRACT], dt=False, expected_exc=exc.NOT_UNIQUE),
])
def test_not_unique_external_id(session, params):
    contracts = []
    for param in params['contracts_params']:
        contracts_params = base_params.copy()
        if param:
            contracts_params.update(param)
        contracts.append(create_contract(session, **contracts_params))
    set_same_external_id(contracts)
    session.flush()

    expected_contract = contracts[0]
    dt = session.now() if params.get('dt', True) else None
    expected_exc = params.get('expected_exc')

    if expected_exc:
        with pytest.raises(expected_exc):
            mapper.Contract.get_contract(session, contract_eid=expected_contract.external_id, active_filter_dt=dt)
    else:
        actual_contract = mapper.Contract.get_contract(session, contract_eid=expected_contract.external_id,
                                                       active_filter_dt=dt)
        assert expected_contract == actual_contract


def test_no_id(session):
    assert mapper.Contract.get_contract(session) is None


def test_get_by_id(session):
    contract = create_contract(session, **base_params)
    assert contract == mapper.Contract.get_contract(session, contract_id=contract.id)


def test_get_by_unique_external_id(session):
    contract = create_contract(session, **base_params)
    set_unique_external_id(contract)
    session.flush()
    expected_contract = contract
    assert expected_contract == mapper.Contract.get_contract(session, contract_eid=expected_contract.external_id)
