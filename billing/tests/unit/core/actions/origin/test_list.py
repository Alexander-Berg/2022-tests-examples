from dataclasses import replace
from uuid import uuid4

import pytest

from hamcrest import assert_that, contains, has_properties

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.list import GetOriginsByMerchantIDAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.load import GetOriginWithLazyFieldsAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.role import AuthorizeRoleAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import MerchantNotFoundError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin


@pytest.mark.asyncio
async def test_returned(storage, merchant, mock_action, mocker, user):
    expected = (mocker.Mock(), mocker.Mock())
    mock_action(GetOriginWithLazyFieldsAction, side_effect=expected)
    await storage.origin.create(Origin(merchant_id=merchant.merchant_id, origin='https://a.test', origin_id=uuid4()))
    await storage.origin.create(Origin(merchant_id=merchant.merchant_id, origin='https://b.test', origin_id=uuid4()))

    returned = await GetOriginsByMerchantIDAction(
        partner_id=merchant.partner_id, merchant_id=merchant.merchant_id, user=user
    ).run()

    assert_that(returned, contains(*expected))


@pytest.mark.asyncio
async def test_does_not_return_other_origins(storage, partner, merchant, user):
    other_partner = await storage.partner.create(
        replace(
            partner,
            name='1',
            partner_id=None,
        )
    )
    other_merchant = await storage.merchant.create(
        replace(
            merchant,
            name='1',
            partner_id=other_partner.partner_id,
            merchant_id=None,
        )
    )
    origin = await storage.origin.create(
        Origin(merchant_id=merchant.merchant_id, origin='https://a.test', origin_id=uuid4())
    )
    await storage.origin.create(
        Origin(merchant_id=other_merchant.merchant_id, origin='https://a.test', origin_id=uuid4())
    )

    returned = await GetOriginsByMerchantIDAction(
        partner_id=merchant.partner_id, merchant_id=merchant.merchant_id, user=user
    ).run()

    assert_that(returned, contains(has_properties({'origin_id': origin.origin_id})))


@pytest.mark.asyncio
async def test_merchant_not_found(merchant, user):
    with pytest.raises(MerchantNotFoundError):
        await GetOriginsByMerchantIDAction(partner_id=merchant.partner_id, merchant_id=uuid4(), user=user).run()


@pytest.mark.asyncio
async def test_calls_authorize_role(user, mock_authorize_role, merchant):
    await GetOriginsByMerchantIDAction(
        partner_id=merchant.partner_id, merchant_id=merchant.merchant_id, user=user
    ).run()

    mock_authorize_role.assert_run_once_with(partner_id=merchant.partner_id, user=user)


@pytest.fixture(autouse=True)
def mock_authorize_role(mock_action):
    return mock_action(AuthorizeRoleAction)
