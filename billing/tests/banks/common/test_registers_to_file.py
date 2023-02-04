from typing import Any, Tuple

from bcl.banks.base import Associate
from bcl.banks.common.registry_operator import RegistryOperator
from bcl.core.models import SalaryRegistry


class NonAbstractRegistry(SalaryRegistry):
    class Meta:
        proxy = True
        app_label = 'core'

    def incoming_save(self, data):
        pass

    def outgoing_compile(self) -> Tuple[str, bytes, str]:
        return 'test_filename.xml', bytes(), 'xml'

    @classmethod
    def incoming_parse(cls, data: bytes) -> dict:
        pass

    def process_connector_result(self, result: Any) -> None:
        pass


class ZippedRegistry(NonAbstractRegistry):
    class Meta:
        proxy = True
        app_label = 'core'

    force_zip = True


class NonAbstractRegistryOperator(RegistryOperator):
    class Meta:
        proxy = True
        app_label = 'core'

    @classmethod
    def process_card_registry(cls, registry: SalaryRegistry):
        pass

    @classmethod
    def process_salary_registry(cls, registry: SalaryRegistry):
        pass

    @classmethod
    def process_dismissal_registry(cls, registry: SalaryRegistry):
        pass

    @classmethod
    def process_connector_result(cls, registry: SalaryRegistry, result: Any) -> None:
        pass


class _RegistryOperator(NonAbstractRegistryOperator):
    class Meta:
        proxy = True
        app_label = 'core'

    type_cards = NonAbstractRegistry
    type_salary = ZippedRegistry


class _Associate(Associate):
    registry_operator = _RegistryOperator
    id = 123
    title = 'Test Associate'

    @classmethod
    def get_bank_code(cls, swift: str) -> str:
        return 'yYHP6'


def test_registers_to_file_1_registry(contract):
    operator = _RegistryOperator(contract, SalaryRegistry.TYPES_CARDS)
    registry = (RegistryOperator
                .spawn_for_contract(contract.number, SalaryRegistry.TYPES_CARDS)
                .registry_create('c61ef018-fdc0-47a1-be0c-429d2b8cb279', '324', '11-12-2016', []))

    filename, contents, content_type = operator.registers_to_file([registry.id], {})
    assert filename.endswith('.xml')
    assert content_type is 'xml'


def test_registers_to_file_1_registry_zip_single(contract):
    operator = _RegistryOperator(contract, SalaryRegistry.TYPES_SALARY)
    registry = (RegistryOperator
                .spawn_for_contract(contract.number, SalaryRegistry.TYPES_SALARY)
                .registry_create('9e232957-1758-4810-9ef1-c2f62f678cfa', '638', '11-12-2016', []))

    filename, contents, content_type = operator.registers_to_file([registry.id], {})
    assert filename.endswith('.zip')
    assert content_type is None


def test_registers_to_file_several_registries(contract):
    operator = _RegistryOperator(contract, SalaryRegistry.TYPES_CARDS)

    registry1 = (RegistryOperator
                 .spawn_for_contract(contract.number, SalaryRegistry.TYPES_CARDS)
                 .registry_create('e29cddc7-ef4a-49bd-a936-63851cf7f78c', '401', '11-12-2016', []))

    registry2 = (RegistryOperator
                 .spawn_for_contract(contract.number, SalaryRegistry.TYPES_CARDS)
                 .registry_create('8b955fe5-5ab3-4649-b584-b0a2c40fe7c3', '884', '11-12-2016', []))

    filename, contents, content_type = operator.registers_to_file([registry1.id, registry2.id], {})
    assert filename.endswith('.zip')
    assert content_type is None
