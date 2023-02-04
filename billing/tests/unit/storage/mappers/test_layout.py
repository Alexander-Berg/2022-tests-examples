from dataclasses import replace
from uuid import uuid4

import pytest

from hamcrest import assert_that, contains, equal_to, has_properties

from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import LayoutType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.layout import Layout
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin
from billing.yandex_pay_admin.yandex_pay_admin.tests.unit.storage.mappers.base import BaseMapperTests


class TestLayoutMapper(BaseMapperTests):
    @pytest.mark.asyncio
    async def test_get(self, mapper, entity, document):
        entity = await mapper.create(entity)

        assert_that(
            await mapper.get(entity.layout_id),
            equal_to(
                replace(
                    entity,
                    document=document,
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_get_not_found(self, mapper, entity):
        with pytest.raises(Layout.DoesNotExist):
            await mapper.get(entity.layout_id)

    @pytest.mark.asyncio
    async def test_save(self, mapper, entity, origin):
        created = await mapper.create(entity)

        saved = await mapper.save(
            replace(
                created,
                type=LayoutType.AFTER_CHECKOUT,
            )
        )
        assert_that(
            saved,
            equal_to(
                Layout(
                    layout_id=created.layout_id,
                    origin_id=created.origin_id,
                    type=LayoutType.AFTER_CHECKOUT,
                    document_id=created.document_id,
                    created=created.created,
                    updated=saved.updated,
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_delete(self, mapper, entity):
        entity = await mapper.create(entity)

        await mapper.delete(entity)

        with pytest.raises(Layout.DoesNotExist):
            await mapper.get(entity.layout_id)

    @pytest.mark.asyncio
    async def test_primary_key(self, mapper, entity):
        await mapper.create(entity)
        with pytest.raises(Layout.AlreadyExists):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_can_find_by_origin_id(self, mapper, storage, entity, document, merchant):
        other_origin = await storage.origin.create(
            Origin(merchant_id=merchant.merchant_id, origin_id=uuid4(), origin='other.test')
        )

        entity = await mapper.create(entity)
        await mapper.create(replace(entity, layout_id=uuid4(), origin_id=other_origin.origin_id))

        found = await mapper.find_by_origin_id(origin_id=entity.origin_id)
        assert_that(
            found,
            contains(
                has_properties(layout_id=entity.layout_id),
            ),
        )

    @pytest.fixture
    def mapper(self, storage):
        return storage.layout

    @pytest.fixture
    def entity(self, origin, document):
        return Layout(
            layout_id=uuid4(),
            origin_id=origin.origin_id,
            type=LayoutType.CHECKOUT,
            document_id=document.document_id,
        )

    @pytest.fixture
    async def document(self, storage, partner):
        return await storage.document.create(
            Document(
                partner_id=partner.partner_id,
                document_id=uuid4(),
                path='fo/bar',
                digest='',
            )
        )

    @pytest.fixture
    async def origin(self, storage, merchant):
        return await storage.origin.create(Origin(merchant_id=merchant.merchant_id, origin_id=uuid4(), origin='a.test'))
