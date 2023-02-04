import pytest
import datetime

from balance.mapper import Contract, ContractCollateral
from tests.balance_tests.contract.contract_common import create_contract, create_jc
from billing.contract_iface.contract_meta import collateral_types

NOW = datetime.datetime.now()
YESTERDAY = NOW - datetime.timedelta(days=1)
DAY_BEFORE_YESTERDAY = YESTERDAY - datetime.timedelta(days=1)


def check_current_state(cs, contract, check_signed, dt, col):
    assert cs._contract == contract
    assert cs._check_signed == check_signed
    assert cs._dt == dt
    assert cs._col == col


def test_current_signed_non_signed_contract(session):
    contract = create_contract(session, is_signed=None, is_faxed=None)
    jc = create_jc(contract)
    assert contract.current_signed() is None
    assert jc.current_signed() is None


@pytest.mark.parametrize('extra_contract_params', [{},
                                                   {'is_cancelled': NOW}])
@pytest.mark.parametrize('contract_params', [{'is_signed': NOW},
                                             {'is_faxed': NOW}])
def test_current_signed_signed_contract_wo_dt(session, contract_params, extra_contract_params):
    contract_params.update(extra_contract_params)
    contract = create_contract(session, **contract_params)
    cs = contract.current_signed()
    check_current_state(cs, contract=contract, check_signed=True, dt=None, col=None)
    jc = create_jc(contract)
    jcs = jc.current_signed()
    check_current_state(jcs, contract=jc, check_signed=True, dt=None, col=None)


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
def test_current_signed_active_on(session, contract_params, is_active_on):
    contract = create_contract(session, **contract_params)
    cs = contract.current_signed(dt=NOW, active=True)
    if is_active_on:
        check_current_state(cs, contract=contract, check_signed=True, dt=NOW, col=None)
    else:
        assert cs is None

    jc = create_jc(contract)
    jcs = jc.current_signed(dt=NOW, active=True)
    if is_active_on:
        check_current_state(jcs, contract=jc, check_signed=True, dt=NOW, col=None)
    else:
        assert jcs is None


def test_complex_attribute_propagation(session):
    CHANGE_SERVICES_COL_ID = 1001
    MEMO_COL_ID = 1003
    contract = create_contract(session)

    contract.col0.dt = DAY_BEFORE_YESTERDAY

    contract.col0.services = {10: 1, 11: 1, 12: 1}
    contract.col0.is_signed = DAY_BEFORE_YESTERDAY
    assert contract.current.services == {10, 11, 12}

    # intermediate collateral to check services attribute to bypass the
    # state through collateral not changing the target attr by itself
    contract.append_collateral(collateral_type=collateral_types['GENERAL'][MEMO_COL_ID],
                               dt=YESTERDAY,
                               memo='some text',
                               is_signed=YESTERDAY
                               )
    contract.append_collateral(collateral_type=collateral_types['GENERAL'][CHANGE_SERVICES_COL_ID],
                               dt=NOW,
                               services ={12},
                               is_signed=NOW
                               )
    session.flush()
    session.expunge_all()

    contract = session.query(Contract).get(contract.id)
    js_contract = create_jc(contract)

    for c in (contract, js_contract):
        assert c.current.services == {12}
        assert c.current_signed(DAY_BEFORE_YESTERDAY).services == {10, 11, 12}
        assert c.current_signed(YESTERDAY).services == {10, 11, 12}
        assert c.current_signed(NOW).services == {12}
