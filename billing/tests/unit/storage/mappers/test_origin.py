import uuid

import psycopg2
import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.storage import OriginMapper
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import Merchant
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin
from billing.yandex_pay_admin.yandex_pay_admin.tests.unit.storage.mappers.base import BaseMapperTests


class TestOriginMapper(BaseMapperTests):
    @pytest.fixture
    def entity(self, merchant: Merchant) -> Origin:
        assert merchant.merchant_id is not None

        return Origin(
            origin_id=uuid.uuid4(),
            merchant_id=merchant.merchant_id,
            origin='origin',
        )

    @pytest.fixture
    def another_entity(self, merchant: Merchant) -> Origin:
        assert merchant.merchant_id is not None

        return Origin(
            origin_id=uuid.uuid4(),
            merchant_id=merchant.merchant_id,
            origin='another_origin',
        )

    @pytest.fixture
    def mapper(self, storage) -> OriginMapper:
        return storage.origin

    @pytest.mark.asyncio
    async def test_can_find_by_merchant_id(self, mapper, entity, another_entity):
        first_origin = await mapper.create(entity)
        second_origin = await mapper.create(another_entity)

        found = await mapper.find_by_merchant_id(entity.merchant_id)

        assert_that(
            found,
            equal_to([first_origin, second_origin]),
        )

    @pytest.mark.asyncio
    async def test_can_remove(self, mapper, entity):
        created = await mapper.create(entity)

        await mapper.delete(created)
        found = await mapper.find_by_merchant_id(entity.merchant_id)

        assert len(found) == 0

    @pytest.mark.asyncio
    async def test_unique_for_partner_and_origin(self, mapper, entity):
        await mapper.create(entity)

        entity.origin_id = uuid.uuid4()
        entity.origin = 'second origin'
        await mapper.create(entity)

        entity.origin_id = uuid.uuid4()
        with pytest.raises(Origin.AlreadyExists):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_has_foreign_key_on_merchant(self, mapper, entity):
        entity.merchant_id = uuid.uuid4()

        with pytest.raises(psycopg2.errors.ForeignKeyViolation):
            await mapper.create(entity)
