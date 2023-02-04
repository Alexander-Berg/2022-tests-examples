import pytest
import datetime

from tests.balance_tests.contract.contract_common import create_contract, create_jc

NOW = datetime.datetime.now()


def check_current_state(cs, contract, check_signed, dt, col):
    assert cs._contract == contract
    assert cs._check_signed == check_signed
    assert cs._dt == dt
    assert cs._col == col


@pytest.mark.parametrize('signed', [True, False])
def test_current_state_non_signed_contract(session, signed):
    contract = create_contract(session, is_signed=None, is_faxed=None)
    jc = create_jc(contract)
    if signed:
        assert contract.current_state(signed=signed) is None
        assert jc.current_state(signed=signed) is None
    else:
        cs = contract.current_state(signed=signed)
        jcs = jc.current_state(signed=signed)
        check_current_state(cs, contract=contract, check_signed=False, dt=None, col=None)
        check_current_state(jcs, contract=jc, check_signed=False, dt=None, col=None)
    check_current_state(contract.current, contract=contract, check_signed=False, dt=None, col=None)
    check_current_state(jc.current, contract=jc, check_signed=False, dt=None, col=None)


@pytest.mark.parametrize('extra_contract_params', [{},
                                                   {'is_cancelled': NOW}])
@pytest.mark.parametrize('contract_params', [{'is_signed': NOW},
                                             {'is_faxed': NOW}])
@pytest.mark.parametrize('signed', [True, False])
def test_current_state_signed_contract_wo_dt(session, signed, contract_params, extra_contract_params):
    contract_params.update(extra_contract_params)
    contract = create_contract(session, **contract_params)
    jc = create_jc(contract)
    cs = contract.current_state(signed=signed)
    jcs = jc.current_state(signed=signed)
    check_current_state(cs, contract=contract, check_signed=signed, dt=None, col=None)
    check_current_state(jcs, contract=jc, check_signed=signed, dt=None, col=None)
    check_current_state(contract.current, contract=contract, check_signed=False, dt=None, col=None)
    check_current_state(jc.current, contract=jc, check_signed=False, dt=None, col=None)


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
@pytest.mark.parametrize('signed', [True, False])
def test_current_state_w_active(session, contract_params, is_active_on, signed):
    contract = create_contract(session, **contract_params)
    jc = create_jc(contract)
    cs = contract.current_state(signed=signed, dt=NOW, active=True)
    jcs = jc.current_state(signed=signed, dt=NOW, active=True)
    if is_active_on:
        check_current_state(cs, contract=contract, check_signed=signed, dt=NOW, col=None)
        check_current_state(jcs, contract=jc, check_signed=signed, dt=NOW, col=None)
    else:
        assert cs is None
        assert jcs is None
    check_current_state(contract.current, contract=contract, check_signed=False, dt=None, col=None)
    check_current_state(jc.current, contract=jc, check_signed=False, dt=None, col=None)
