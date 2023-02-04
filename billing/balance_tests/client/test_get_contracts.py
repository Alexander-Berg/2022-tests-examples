import pytest

# from tests import object_builder as ob

from tests.balance_tests.client.client_common import create_client, create_contract, PARTNERS, GENERAL, SPENDABLE


def test_client_wo_contract(session, client):
    assert client.get_contracts() == []


@pytest.mark.parametrize('contract_type', [PARTNERS, GENERAL])
def test_client_w_contract(session, client, contract_type):
    contract = create_contract(session, client=client, contract_type=contract_type)
    if contract_type == GENERAL:
        assert client.get_contracts() == [contract]
    else:
        assert client.get_contracts() == []


@pytest.mark.parametrize('contract_type', [PARTNERS, SPENDABLE])
def test_client_force_contract_type(session, client, contract_type):
    contract = create_contract(session, client=client, contract_type=contract_type)
    assert client.get_contracts(contract_type=contract_type) == [contract]
