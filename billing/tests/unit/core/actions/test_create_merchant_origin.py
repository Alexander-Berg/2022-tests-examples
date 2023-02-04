import pytest

from hamcrest import assert_that, has_properties

from billing.yandex_pay.yandex_pay.core.actions.create_merchant import CreateMerchantAction
from billing.yandex_pay.yandex_pay.core.actions.create_merchant_origin import CreateMerchantOriginAction
from billing.yandex_pay.yandex_pay.core.exceptions import (
    CoreInsecureMerchantOriginSchemaError, CoreInvalidMerchantOriginError
)
from billing.yandex_pay.yandex_pay.utils.domain import get_canonical_origin


@pytest.fixture
async def merchant():
    return await CreateMerchantAction(
        name='some_name',
    ).run()


@pytest.mark.asyncio
async def test_creates_merchant_origin(merchant):
    canonical_origin = get_canonical_origin('https://best-shop.ru:443')

    merchant_origin = await CreateMerchantOriginAction(
        merchant_id=merchant.merchant_id,
        origin=canonical_origin,
    ).run()

    assert_that(
        merchant_origin,
        has_properties(dict(
            merchant_id=merchant.merchant_id,
            origin=canonical_origin,
        ))
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('origin, exc_cls', (
    ('invalid/origin', CoreInvalidMerchantOriginError),
    ('http://test.test:80', CoreInsecureMerchantOriginSchemaError),
))
async def test_invalid_origin(merchant, storage, origin, exc_cls):
    with pytest.raises(exc_cls):
        await CreateMerchantOriginAction(
            merchant_id=merchant.merchant_id,
            origin=origin,
        ).run()
