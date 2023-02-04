import pytest

from bcl.banks.party_sber import Sber


@pytest.fixture
def contract(get_salary_contract):
    contract = get_salary_contract(Sber, number='4815162342')
    yield contract
