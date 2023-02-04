import uuid

import psycopg2
import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import (
    DeliveryIntegrationParams,
    Merchant,
    YandexDeliveryParams,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import Partner
from billing.yandex_pay_admin.yandex_pay_admin.storage.mappers.merchant import MerchantMapper
from billing.yandex_pay_admin.yandex_pay_admin.tests.unit.storage.mappers.base import BaseMapperTests


class TestMerchantMapper(BaseMapperTests):
    @pytest.fixture()
    def delivery_integration_params(self):
        return DeliveryIntegrationParams(YandexDeliveryParams(oauth_token="token", autoaccept=False))

    @pytest.fixture
    def entity(self, partner: Partner, delivery_integration_params) -> Merchant:
        assert partner.partner_id is not None

        return Merchant(
            partner_id=partner.partner_id,
            name='merchant name',
            delivery_integration_params=delivery_integration_params,
        )

    @pytest.fixture
    def another_entity(self, partner: Partner, delivery_integration_params) -> Merchant:
        assert partner.partner_id is not None

        return Merchant(
            partner_id=partner.partner_id,
            name='another merchant name',
            delivery_integration_params=delivery_integration_params,
        )

    @pytest.fixture
    def mapper(self, storage) -> MerchantMapper:
        return storage.merchant

    @pytest.mark.asyncio
    async def test_can_find_by_partner_id(self, mapper, entity, another_entity):
        first_merchant = await mapper.create(entity)
        second_merchant = await mapper.create(another_entity)

        found = await mapper.find_by_partner_id(entity.partner_id)

        assert_that(found, equal_to([first_merchant, second_merchant]))

    @pytest.mark.asyncio
    async def test_can_update(self, mapper, entity):
        created = await mapper.create(entity)
        created.name = 'changed name'

        updated = await mapper.save(created)
        created.updated = updated.updated

        assert_that(
            updated,
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_can_remove(self, mapper, entity):
        created = await mapper.create(entity)

        await mapper.delete(created)
        found = await mapper.find_by_partner_id(entity.partner_id)

        assert_that(found, equal_to([]))

    @pytest.mark.asyncio
    async def test_has_foreign_key_on_partner(self, mapper, entity):
        entity.partner_id = uuid.uuid4()

        with pytest.raises(psycopg2.errors.ForeignKeyViolation):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_can_get_with_partner_id(self, mapper, entity):
        created = await mapper.create(entity)

        loaded = await mapper.get_with_partner_id(entity.merchant_id, partner_id=entity.partner_id)

        assert_that(loaded, equal_to(created))

    @pytest.mark.asyncio
    async def test_cannot_get_with_incorrect_partner_id(self, mapper, entity):
        await mapper.create(entity)

        with pytest.raises(Merchant.DoesNotExist):
            await mapper.get_with_partner_id(entity.merchant_id, partner_id=uuid.uuid4())

    @pytest.mark.asyncio
    async def test_update_bumps_revision(self, mapper, entity):
        created = await mapper.create(entity)
        assert_that(created.revision, equal_to(1))

        updated = await mapper.save(created)
        assert_that(updated.revision, equal_to(2))

        created.updated = updated.updated
        assert_that(updated, equal_to(created))
