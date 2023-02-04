from datetime import datetime

import hamcrest as hm
import pytest

from billing.library.python.calculator import exceptions as exc
from billing.library.python.calculator.services.contract import ContractManagerService, Eq, Contains
from billing.library.python.calculator.test_utils import builder

CONTRACTS = [
    builder.gen_general_contract(contract_id=1, client_id=1, person_id=1, services=[121], firm=1),
    builder.gen_general_contract(contract_id=2, client_id=1, person_id=2, services=[121, 638], firm=1, signed=False),
    builder.gen_general_contract(contract_id=3, client_id=1, person_id=1, services=[131], firm=121),
]


class TestContractManagerService:
    @pytest.mark.parametrize('filters, expected_contract_ids', [
        pytest.param(None, [1, 3], id='without filters'),
        pytest.param([Eq('contract.client_id', 1)], [1, 3], id='with client filter'),
        pytest.param([Eq('contract.client_id', 1), Contains('services', 121)], [1], id='with client/service filters'),
    ])
    def test_get_all(self, filters, expected_contract_ids):
        manager = ContractManagerService(CONTRACTS)
        states = manager.get_all_current_signed(datetime.now(), filters)
        hm.assert_that(states, hm.has_length(len(expected_contract_ids)))
        hm.assert_that([cs.contract.id for cs in states], hm.has_items(*expected_contract_ids))

    def test_get_one(self):
        manager = ContractManagerService(CONTRACTS)
        cs = manager.get_current_signed(datetime.now(), [Eq('contract.id', 3)])
        hm.assert_that(cs.contract.id, hm.equal_to(3))

    @pytest.mark.parametrize('filters, expected_error', [
        pytest.param(None, exc.AmbiguousContractStateError, id='ambiguous states'),
        pytest.param([Eq('contract.id', 2)], exc.NoActiveContractsError, id='no active contracts'),
    ])
    def test_get_one_error(self, filters, expected_error):
        manager = ContractManagerService(CONTRACTS)
        with pytest.raises(expected_error):
            manager.get_current_signed(datetime.now(), filters)


class TestFilters:
    def test_eq(self):
        states = ContractManagerService(CONTRACTS).get_all_current_signed(datetime.now())
        eq = Eq('contract.id', 1)
        states = eq.filter(states)
        hm.assert_that(states, hm.has_length(1))
        hm.assert_that(states[0].contract.id, hm.equal_to(1))

    def test_contains(self):
        states = ContractManagerService(CONTRACTS).get_all_current_signed(datetime.now())
        contains = Contains('services', 131)
        states = contains.filter(states)
        hm.assert_that(states, hm.has_length(1))
        hm.assert_that(states[0].contract.id, hm.equal_to(3))
