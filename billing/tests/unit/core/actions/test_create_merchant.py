from uuid import UUID

import pytest

from hamcrest import all_of, assert_that, equal_to, has_properties, instance_of

from billing.yandex_pay.yandex_pay.core.actions.create_merchant import CreateMerchantAction
from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant


@pytest.mark.asyncio
async def test_creates_merchant_with_generated_uuid():
    merchant = await CreateMerchantAction(name='some_name').run()

    assert_that(merchant, all_of(
        instance_of(Merchant),
        has_properties(dict(
            name=equal_to('some_name'),
            merchant_id=instance_of(UUID),
        ))
    ))


@pytest.mark.asyncio
async def test_creates_merchant_with_predefined_uuid():
    merchant = await CreateMerchantAction(
        name='some_name',
        merchant_id=UUID('9d57a8a8-4503-401d-99d6-fc3e2c54fa0f')
    ).run()

    assert_that(merchant, all_of(
        instance_of(Merchant),
        has_properties(dict(
            name=equal_to('some_name'),
            merchant_id=equal_to(UUID('9d57a8a8-4503-401d-99d6-fc3e2c54fa0f')),
        ))
    ))
