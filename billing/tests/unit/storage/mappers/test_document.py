import uuid

import psycopg2
import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.storage import DocumentMapper
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import Partner
from billing.yandex_pay_admin.yandex_pay_admin.tests.unit.storage.mappers.base import BaseMapperTests


class TestDocumentMapper(BaseMapperTests):
    @pytest.fixture
    def entity(self, partner: Partner) -> Document:
        assert partner.partner_id is not None

        return Document(
            document_id=uuid.uuid4(),
            partner_id=partner.partner_id,
            path='some_url',
            digest='digest',
        )

    @pytest.fixture
    def mapper(self, storage) -> DocumentMapper:
        return storage.document

    @pytest.mark.asyncio
    async def test_can_find_by_partner_id(self, mapper, entity):
        aoc_document = await mapper.create(entity)
        entity.path = 'offer'
        entity.document_id = uuid.uuid4()
        offer_document = await mapper.create(entity)

        found = await mapper.find_by_partner_id(entity.partner_id)

        assert_that(
            found,
            equal_to([aoc_document, offer_document]),
        )

    @pytest.mark.asyncio
    async def test_get(self, mapper, entity):
        created = await mapper.create(entity)

        assert_that(
            await mapper.get(created.document_id),
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_get_not_found(self, mapper):
        with pytest.raises(Document.DoesNotExist):
            await mapper.get(uuid.uuid4())

    @pytest.mark.asyncio
    async def test_can_update(self, mapper, entity):
        created = await mapper.create(entity)
        created.path = 'new_url'

        updated = await mapper.save(created)
        created.updated = updated.updated

        assert_that(
            updated,
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_unique_for_partner_document_id(self, mapper, entity):
        await mapper.create(entity)

        with pytest.raises(Document.AlreadyExists):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_has_foreign_key_on_partner(self, mapper, entity):
        entity.partner_id = uuid.uuid4()

        with pytest.raises(psycopg2.errors.ForeignKeyViolation):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_delete(self, mapper, entity):
        created = await mapper.create(entity)

        await mapper.delete(created)

        with pytest.raises(Document.DoesNotExist):
            await mapper.get(created.document_id)
