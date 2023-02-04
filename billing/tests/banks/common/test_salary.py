import pytest

from bcl.banks.common.registry_operator import RegistryOperator
from bcl.core.models import SalaryRegistry
from bcl.exceptions import SalaryError


def test_registry_by_registry_number(contract):
    registry_number = '759'
    registry_id = '41D067A9-EF4F-39E7-E055-000000000074'

    def test_registry(reg_type):
        registry = RegistryOperator.spawn_for_contract(contract.number, reg_type).registry_create(
            registry_id, registry_number, '11-12-2016', [])

        found_registry = RegistryOperator.spawn_for_contract(contract.number, reg_type).registry_get(
            registry_id, registry.registry_number)

        assert registry == found_registry

    test_registry(SalaryRegistry.TYPES_SALARY)
    test_registry(SalaryRegistry.TYPES_CARDS)


def test_card_registry_factory_duplicate_check(contract):
    registry_number = '759'
    registry1_id = 'f2bf04bd-41f7-4323-ae42-4da7c8a65291'
    registry2_id = '9163c524-778c-4abb-a572-e1d9af4f9e95'

    def test_duplicates(reg_type):
        RegistryOperator.spawn_for_contract(contract.number, reg_type).registry_create(
            registry1_id, registry_number, '11-12-2016', [])

        RegistryOperator.spawn_for_contract(contract.number, reg_type).registry_create(
            registry2_id, registry_number, '11-12-2016', [])

        with pytest.raises(SalaryError):
            RegistryOperator.spawn_for_contract(contract.number, reg_type).registry_create(
                registry2_id, registry_number, '11-12-2016', [])

    test_duplicates(SalaryRegistry.TYPES_SALARY)
    test_duplicates(SalaryRegistry.TYPES_CARDS)
