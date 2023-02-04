import uuid

import psycopg2
import pytest

from hamcrest import assert_that, contains_inanyorder

from billing.yandex_pay_admin.yandex_pay_admin.storage import ContractDocumentMapper
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.contract_document import ContractDocument
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import ContractDocumentType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import Partner
from billing.yandex_pay_admin.yandex_pay_admin.tests.unit.storage.mappers.base import BaseMapperTests


class TestContractDocumentMapper(BaseMapperTests):
    @pytest.fixture
    def entity(self, partner: Partner, document: Document) -> ContractDocument:
        assert partner.partner_id is not None

        return ContractDocument(
            contract_document_id=uuid.uuid4(),
            partner_id=partner.partner_id,
            type=ContractDocumentType.CONTRACT,
            document_id=document.document_id,
        )

    @pytest.fixture
    def another_entity(self, partner: Partner, document: Document) -> ContractDocument:
        assert partner.partner_id is not None

        return ContractDocument(
            contract_document_id=uuid.uuid4(),
            partner_id=partner.partner_id,
            type=ContractDocumentType.PASSPORT,
            document_id=document.document_id,
        )

    @pytest.fixture
    async def document(self, storage, partner: Partner) -> Document:
        assert partner.partner_id is not None

        return await storage.document.create(
            Document(
                document_id=uuid.uuid4(),
                partner_id=partner.partner_id,
                path='pa/th',
                digest='',
            )
        )

    @pytest.fixture
    def mapper(self, storage) -> ContractDocumentMapper:
        return storage.contract_document

    @pytest.mark.asyncio
    async def test_can_find_by_partner_id(self, mapper, entity, another_entity, document):
        first_contract_document = await mapper.create(entity)
        second_contract_document = await mapper.create(another_entity)

        found = await mapper.find_by_partner_id(entity.partner_id)

        first_contract_document.document = document
        second_contract_document.document = document
        assert_that(
            found,
            contains_inanyorder(first_contract_document, second_contract_document),
        )

    @pytest.mark.asyncio
    async def test_can_remove(self, mapper, entity):
        created = await mapper.create(entity)

        await mapper.delete(created)
        found = await mapper.find_by_partner_id(entity.partner_id)

        assert len(found) == 0

    @pytest.mark.asyncio
    async def test_has_foreign_key_on_partner(self, mapper, entity):
        entity.partner_id = uuid.uuid4()

        with pytest.raises(psycopg2.errors.ForeignKeyViolation):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_has_foreign_key_on_document(self, mapper, entity):
        entity.document_id = uuid.uuid4()

        with pytest.raises(psycopg2.errors.ForeignKeyViolation):
            await mapper.create(entity)
