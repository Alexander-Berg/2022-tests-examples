# -*- coding: utf-8 -*-
import pytest
import datetime

from tests.balance_tests.contract.contract_common import create_contract, create_jc

NOW = datetime.datetime.now()


@pytest.mark.parametrize('contract_params, is_active_on',
                         [({}, False),
                          ({'is_signed': NOW, 'is_cancelled': NOW}, False),
                          ({'is_faxed': NOW, 'is_cancelled': NOW}, False),
                          ({'is_signed': NOW}, True),
                          ({'is_faxed': NOW}, True),
                          ({'is_signed': NOW, 'finish_dt': NOW}, False),
                          ({'is_signed': NOW, 'finish_dt': NOW + datetime.timedelta(seconds=1), 'end_dt': NOW}, True),
                          ({'is_signed': NOW, 'dt': NOW + datetime.timedelta(seconds=1)}, False),

                          ])
def test_active_on(session, contract_params, is_active_on):
    contract = create_contract(session, **contract_params)
    jc = create_jc(contract)
    assert contract.active_on(dt=NOW) is is_active_on
    assert jc.active_on(dt=NOW) is is_active_on


def test_active_on_empty_date(session):
    contract = create_contract(session, **{'is_signed': NOW})
    jc = create_jc(contract)
    assert contract.active_on(dt=None) is True
    assert jc.active_on(dt=None) is True
