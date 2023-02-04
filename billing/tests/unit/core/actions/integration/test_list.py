from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.integration.list import ListIntegrationsAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.get import GetMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import IntegrationStatus, RoleType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.integration import Integration
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import Merchant


@pytest.fixture
async def merchant(storage, partner, role):
    return await storage.merchant.create(
        Merchant(
            name='merchant name',
            partner_id=partner.partner_id,
        )
    )


@pytest.fixture
async def integrations(storage, merchant):
    return [
        await storage.integration.create(
            Integration(
                psp_id=uuid4(),
                psp_external_id='payture',
                merchant_id=merchant.merchant_id,
                status=IntegrationStatus.READY,
            )
        )
        for _ in range(3)
    ]


@pytest.mark.asyncio
async def test_result(user, merchant, integrations, mock_action):
    role_mock = mock_action(GetMerchantAction, merchant)

    result = await ListIntegrationsAction(merchant_id=merchant.merchant_id, user=user).run()

    assert_that(result, equal_to(result))
    role_mock.assert_run_once_with(
        user=user,
        merchant_id=merchant.merchant_id,
        minimum_role_required=RoleType.VIEWER,
    )
