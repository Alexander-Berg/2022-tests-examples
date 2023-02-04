import pytest
import datetime
import mock

from tests import object_builder as ob
from tests.balance_tests.contract.contract_common import create_jc

NOW = datetime.datetime.now()


@pytest.mark.parametrize('contract_params, is_signed', [
    ({'is_signed': None}, False),
    ({'is_signed': NOW}, True),
    ({'is_signed': None, 'is_faxed': NOW}, True)
])
def test_signed(session, contract_params, is_signed):
    contract = ob.ContractBuilder(**contract_params).build(session).obj
    jc = create_jc(contract)
    assert contract.signed is is_signed
    assert jc.signed is is_signed


@pytest.mark.parametrize('contract_params, expected', [
    ({'currency': 810}, {'currency_iso_code': 'RUB'}),
    ({'currency': 10398}, {'currency_iso_code': 'KZT'}),
])
def test_fields(session, contract_params, expected):
    contract = ob.ContractBuilder(**contract_params).build(session).obj
    jc = create_jc(contract)
    for key, value in expected.items():
        assert getattr(jc, key) == value


@pytest.mark.parametrize('contract_params, is_signed', [
    ({'is_signed': None, 'ctype': 'PARTNERS', 'collaterals': [{'collateral_type_id': 2090}]}, True),
    ({'is_signed': None, 'ctype': 'PARTNERS', 'collaterals': [{'collateral_type_id': 2100}]}, True)
])
def test_collateral_signed(session, contract_params, is_signed):
    contract = ob.ContractBuilder(**contract_params).build(session).obj
    jc = create_jc(contract)
    assert contract.signed is False
    assert jc.signed is False
    assert contract.collaterals[1].signed is is_signed
    assert jc.collaterals[1].signed is is_signed
    assert jc.person_type == contract.person.type


@pytest.mark.parametrize('contract_params, is_active', [
    ({'is_signed': None}, False),
    ({'is_signed': NOW}, True),
    ({'is_signed': None, 'is_faxed': NOW}, True),
    ({'is_signed': NOW, 'is_cancelled': NOW}, False),
    ({'is_signed': None, 'is_faxed': NOW, 'is_cancelled': NOW}, False),
])
def test_active(session, contract_params, is_active):
    contract = ob.ContractBuilder(**contract_params).build(session).obj
    jc = create_jc(contract)
    assert contract.active is is_active
    assert jc.active is is_active


@pytest.mark.parametrize('key, val, type_', [
    ('is_process_taxi_netting_in_oebs_', True, bool),
    ('is_process_taxi_netting_in_oebs_', False, bool),
    ('cpf_netting_last_dt', datetime.datetime.now(), datetime.datetime),
])
def test_extprops(session, key, val, type_):
    with mock.patch('balance.mapper.exportable_ng.ExportableNgMixin.enqueue_ng'):
        contract = ob.ContractBuilder(**{key: val}).build(session).obj
        jc = create_jc(contract)
        assert getattr(jc, key) == val
        assert isinstance(getattr(jc, key), type_)
