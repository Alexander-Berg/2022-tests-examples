from dataclasses import replace
from uuid import uuid4

import psycopg2.errors
import pytest

from hamcrest import assert_that, contains_inanyorder, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.storage import IntegrationMapper
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.integration import Integration
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import Merchant
from billing.yandex_pay_admin.yandex_pay_admin.storage.exceptions import (
    IntegrationDuplicateDeployedIntegrationError,
    IntegrationDuplicateMerchantPSPPerStage,
)
from billing.yandex_pay_admin.yandex_pay_admin.tests.unit.storage.mappers.base import BaseMapperTests


class TestIntegrationMapper(BaseMapperTests):
    @pytest.fixture
    def psp_id(self):
        return uuid4()

    @pytest.fixture
    def entity(self, psp_id, merchant: Merchant) -> Integration:
        assert merchant.merchant_id is not None

        return Integration(
            psp_id=psp_id,
            psp_external_id='payture',
            merchant_id=merchant.merchant_id,
            status=IntegrationStatus.DEPLOYED,
        )

    @pytest.fixture
    def another_entity(self, psp_id, merchant: Merchant) -> Integration:
        return Integration(
            psp_id=uuid4(),
            psp_external_id='uniteller',
            merchant_id=merchant.merchant_id,
            status=IntegrationStatus.READY,
        )

    @pytest.fixture
    def mapper(self, storage) -> IntegrationMapper:
        return storage.integration

    @pytest.mark.asyncio
    async def test_can_get_deployed_by_merchant_id(self, mapper, merchant, entity, another_entity):
        first_integration = await mapper.create(entity)
        await mapper.create(another_entity)

        found = await mapper.get_deployed_by_merchant_id(merchant.merchant_id, for_testing=True)

        assert_that(found, equal_to(first_integration))

    @pytest.mark.asyncio
    async def test_get_deployed_by_merchant_id_missing(self, mapper, entity, another_entity):
        integration = await mapper.create(entity)
        await mapper.create(another_entity)

        assert_that(integration.for_testing, equal_to(True))

        with pytest.raises(Integration.DoesNotExist):
            await mapper.get_deployed_by_merchant_id(integration.merchant_id, for_testing=False)

    @pytest.mark.asyncio
    async def test_list_by_merchant_id(self, mapper, merchant, entity, another_entity):
        first_integration = await mapper.create(entity)
        second_integration = await mapper.create(another_entity)

        found = await mapper.list_by_merchant_id(merchant.merchant_id)

        assert_that(found, contains_inanyorder(first_integration, second_integration))

    @pytest.mark.asyncio
    async def test_get_by_merchant_id_and_psp_id(self, mapper, merchant, entity, another_entity):
        first_integration = await mapper.create(entity)
        second_integration = await mapper.create(another_entity)

        loaded_first = await mapper.get_by_merchant_id_and_psp_id(
            merchant.merchant_id, first_integration.psp_id, for_testing=True
        )
        loaded_second = await mapper.get_by_merchant_id_and_psp_id(
            merchant.merchant_id, second_integration.psp_id, for_testing=True
        )

        assert_that(loaded_first, equal_to(first_integration))
        assert_that(loaded_second, equal_to(second_integration))

    @pytest.mark.asyncio
    async def test_get_by_merchant_id_and_psp_id__not_found(self, mapper, entity):
        integration = await mapper.create(entity)

        assert_that(integration.for_testing, equal_to(True))

        with pytest.raises(Integration.DoesNotExist):
            await mapper.get_by_merchant_id_and_psp_id(integration.merchant_id, integration.psp_id, for_testing=False)

    @pytest.mark.asyncio
    async def test_can_update(self, mapper, entity):
        created = await mapper.create(entity)
        created.status = IntegrationStatus.READY

        updated = await mapper.save(created)
        created.updated = updated.updated
        created.revision = updated.revision

        assert_that(
            updated,
            equal_to(created),
        )

    @pytest.mark.asyncio
    async def test_can_remove(self, mapper, entity, another_entity, merchant):
        created = await mapper.create(entity)
        second_integration = await mapper.create(another_entity)

        await mapper.delete(created)
        found = await mapper.list_by_merchant_id(merchant.merchant_id)

        assert_that(found, equal_to([second_integration]))

    @pytest.mark.asyncio
    async def test_update_bumps_revision(self, mapper, entity):
        created = await mapper.create(entity)
        assert_that(created.revision, equal_to(1))

        updated = await mapper.save(created)
        assert_that(updated.revision, equal_to(2))

        created.updated = updated.updated
        assert_that(updated, equal_to(created))

    @pytest.mark.asyncio
    async def test_has_foreign_key_on_merchant(self, mapper, entity):
        entity.merchant_id = uuid4()

        with pytest.raises(psycopg2.errors.ForeignKeyViolation):
            await mapper.create(entity)

    @pytest.mark.asyncio
    async def test_unique_merchant_psp_env_combination(self, rollback_after, mapper, entity, another_entity):
        first = await mapper.create(entity)
        second = await mapper.create(another_entity)

        async with rollback_after():
            with pytest.raises(IntegrationDuplicateMerchantPSPPerStage):
                await mapper.create(replace(entity, integration_id=uuid4()))

        second.psp_id = first.psp_id
        async with rollback_after():
            with pytest.raises(IntegrationDuplicateMerchantPSPPerStage):
                await mapper.save(second)

        second.for_testing = not second.for_testing
        await mapper.save(second)

    @pytest.mark.asyncio
    async def test_unique_deployed_integration_per_merchant(self, rollback_after, mapper, entity, another_entity):
        await mapper.create(entity)
        second = await mapper.create(another_entity)

        second.status = IntegrationStatus.DEPLOYED
        async with rollback_after():
            with pytest.raises(IntegrationDuplicateDeployedIntegrationError):
                await mapper.save(second)

        second.for_testing = not second.for_testing
        await mapper.save(second)

    @pytest.mark.parametrize('first_status', (IntegrationStatus.READY, IntegrationStatus.DEPLOYED))
    @pytest.mark.parametrize('second_status', (IntegrationStatus.READY, IntegrationStatus.DEPLOYED))
    @pytest.mark.parametrize('first_for_testing', (True, False))
    @pytest.mark.parametrize('second_for_testing', (True, False))
    @pytest.mark.asyncio
    async def test_list_by_partner_id(
        self,
        mapper,
        storage,
        entity,
        another_entity,
        partner,
        merchant,
        first_status,
        second_status,
        first_for_testing,
        second_for_testing,
    ):
        first = await mapper.create(replace(entity, for_testing=first_for_testing, status=first_status))
        second_merchant = await storage.merchant.create(replace(merchant, merchant_id=uuid4()))
        second = await mapper.create(
            replace(
                another_entity,
                for_testing=second_for_testing,
                status=second_status,
                merchant_id=second_merchant.merchant_id,
            )
        )
        # Extra integration
        outside_partner = await storage.partner.create(replace(partner, partner_id=uuid4()))
        outside_merchant = await storage.merchant.create(
            replace(merchant, partner_id=outside_partner.partner_id, merchant_id=uuid4())
        )
        await mapper.create(replace(entity, merchant_id=outside_merchant.merchant_id, for_testing=False))

        found = await mapper.list_by_partner_id(
            partner_id=merchant.partner_id, status=IntegrationStatus.DEPLOYED, for_testing=False
        )

        expected = []
        if first_status == IntegrationStatus.DEPLOYED and not first_for_testing:
            expected.append(first)
        if second_status == IntegrationStatus.DEPLOYED and not second_for_testing:
            expected.append(second)
        assert_that(found, contains_inanyorder(*expected))
