from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.get import GetMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.role import AuthorizeRoleAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import MerchantNotFoundError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import Merchant


@pytest.fixture
async def merchant(storage, partner, role):
    return await storage.merchant.create(
        Merchant(
            name='merchant name',
            partner_id=partner.partner_id,
        )
    )


@pytest.mark.asyncio
async def test_get_merchant(merchant, user):
    loaded = await GetMerchantAction(user=user, merchant_id=merchant.merchant_id).run()

    assert_that(loaded, equal_to(merchant))


@pytest.mark.asyncio
async def test_calls_authorize_action__viewer(merchant, partner, user, mock_action):
    mock = mock_action(AuthorizeRoleAction)

    await GetMerchantAction(user=user, merchant_id=merchant.merchant_id).run()

    mock.assert_run_once_with(partner_id=partner.partner_id, user=user, minimum_role_required=RoleType.VIEWER)


@pytest.mark.asyncio
async def test_calls_authorize_action__owner(merchant, partner, user, mock_action):
    mock = mock_action(AuthorizeRoleAction)

    await GetMerchantAction(user=user, merchant_id=merchant.merchant_id, minimum_role_required=RoleType.OWNER).run()

    mock.assert_run_once_with(partner_id=partner.partner_id, user=user, minimum_role_required=RoleType.OWNER)


@pytest.mark.asyncio
async def test_cannot_get_missing_merchant(merchant, user):
    with pytest.raises(MerchantNotFoundError):
        await GetMerchantAction(user=user, merchant_id=uuid4()).run()


@pytest.mark.asyncio
async def test_get_with_partner_id(merchant, partner, user):
    loaded = await GetMerchantAction(
        user=user,
        merchant_id=merchant.merchant_id,
        partner_id=partner.partner_id,
    ).run()

    assert_that(loaded, equal_to(merchant))


@pytest.mark.asyncio
async def test_cannot_get_missing_merchant_with_partner_id(partner, merchant, user):
    with pytest.raises(MerchantNotFoundError):
        await GetMerchantAction(
            user=user,
            merchant_id=uuid4(),
            partner_id=partner.partner_id,
        ).run()


@pytest.mark.asyncio
async def test_cannot_get_merchant_belonging_to_different_partner(merchant, user):
    with pytest.raises(MerchantNotFoundError):
        await GetMerchantAction(
            user=user,
            merchant_id=merchant.merchant_id,
            partner_id=uuid4(),
        ).run()
