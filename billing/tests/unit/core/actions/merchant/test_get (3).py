from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.create_or_update import CreateOrUpdateMerchantAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.get import GetMerchantAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import CoreMerchantNotFoundError


@pytest.mark.asyncio
async def test_get_merchant():
    merchant = await CreateOrUpdateMerchantAction(
        merchant_id=uuid4(), name='some_name',
        origins=[{'origin': 'https://foo.test'}],
    ).run()

    assert_that(
        await GetMerchantAction(merchant.merchant_id).run(),
        equal_to(merchant),
    )


@pytest.mark.asyncio
async def test_get_merchant__when_not_found():
    with pytest.raises(CoreMerchantNotFoundError):
        await GetMerchantAction(uuid4()).run()
