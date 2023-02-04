from uuid import uuid4

import psycopg2
import pytest

from hamcrest import assert_that, contains_inanyorder, equal_to

from pay.bill_payments.bill_payments.interactions.kazna.entities import DocumentCode
from pay.bill_payments.bill_payments.storage import Storage
from pay.bill_payments.bill_payments.storage.entities.document import Document
from pay.bill_payments.bill_payments.storage.entities.user import User
from pay.bill_payments.bill_payments.storage.mappers.document import DocumentMapper
from pay.bill_payments.bill_payments.tests.unit.storage.mappers.base import BaseMapperTests


class TestDocumentMapper(BaseMapperTests):
    @pytest.fixture
    def entity(self, user: User) -> Document:
        return Document(
            document_id=uuid4(),
            uid=user.uid,
            code=DocumentCode.DRIVER_LICENSE,
            title='The title',
            value='document value',
        )

    @pytest.fixture
    def mapper(self, storage: Storage) -> DocumentMapper:
        return storage.document

    @pytest.mark.asyncio
    async def test_can_find_by_uid(self, mapper, entity):
        document = await mapper.create(entity)
        entity.document_id = uuid4()
        another_document = await mapper.create(entity)

        found = await mapper.find_by_uid(entity.uid)

        assert_that(
            found,
            contains_inanyorder(
                document,
                another_document,
            ),
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
            await mapper.get(uuid4())

    @pytest.mark.asyncio
    async def test_get_by_uid_and_document_id(self, mapper, entity):
        created = await mapper.create(entity)

        found = await mapper.get_by_uid_and_document_id(uid=entity.uid, document_id=entity.document_id)
        assert_that(found, equal_to(created))

    @pytest.mark.asyncio
    async def test_get_by_uid_and_document_id__not_found(self, mapper, entity):
        await mapper.create(entity)

        with pytest.raises(Document.DoesNotExist):
            await mapper.get_by_uid_and_document_id(uid=entity.uid + 1, document_id=entity.document_id)

    @pytest.mark.asyncio
    async def test_can_update(self, mapper, entity):
        created = await mapper.create(entity)
        created.code = DocumentCode.VEHICLE_REGISTRATION_CERTIFICATE

        updated = await mapper.save(created)
        created.updated = updated.updated

        assert_that(
            updated,
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_unique_for_document_id(self, mapper, entity, storage):
        await mapper.create(entity)
        await storage.user.create(User(uid=122234))
        entity.uid = 122234

        with pytest.raises(Document.AlreadyExists):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_has_foreign_key_on_user(self, mapper, entity):
        entity.uid = 11111

        with pytest.raises(psycopg2.errors.ForeignKeyViolation):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_delete(self, mapper, entity):
        created = await mapper.create(entity)

        await mapper.delete(created)

        found = await mapper.find_by_uid(entity.uid)

        assert not found
