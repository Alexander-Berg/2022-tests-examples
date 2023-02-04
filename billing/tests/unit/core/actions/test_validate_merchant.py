import logging
from uuid import uuid4

import pytest

from hamcrest import assert_that, has_entries, has_item, has_properties

from billing.yandex_pay.yandex_pay.core.actions.validate_merchant import ValidateMerchantAction
from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant
from billing.yandex_pay.yandex_pay.core.entities.merchant_origin import MerchantOrigin
from billing.yandex_pay.yandex_pay.core.exceptions import CoreMerchantAccountError


@pytest.fixture
async def merchant(storage):
    return await storage.merchant.create(
        Merchant(
            merchant_id=uuid4(),
            name='fake',
        )
    )


@pytest.fixture
async def merchant_origin(storage, merchant: Merchant):
    return await storage.merchant_origin.create(
        MerchantOrigin(
            merchant_id=merchant.merchant_id,
            origin='https://example.com:443',
        )
    )


@pytest.fixture(autouse=True)
def origin_validation_mandatory(yandex_pay_settings):
    yandex_pay_settings.MERCHANT_ORIGIN_VALIDATION_IS_MANDATORY = True


@pytest.mark.asyncio
async def test_validate_merchant(merchant, merchant_origin):
    await ValidateMerchantAction(
        merchant_id=merchant.merchant_id,
        validate_origin=True,
        merchant_origin=merchant_origin.origin,
    ).run()


@pytest.mark.asyncio
async def test_merchant_is_blocked(
    storage, merchant, merchant_origin, dummy_logs
):
    merchant.is_blocked = True
    await storage.merchant.save(merchant)

    with pytest.raises(CoreMerchantAccountError):
        await ValidateMerchantAction(
            merchant_id=merchant.merchant_id,
            validate_origin=True,
            merchant_origin=merchant_origin.origin,
        ).run()

    assert_that(
        dummy_logs(),
        has_item(
            has_properties(
                message='Merchant is blocked',
                levelno=logging.WARNING,
                _context=has_entries(
                    merchant_id=merchant.merchant_id,
                    validate_origin=True,
                    merchant_origin=merchant_origin.origin,
                    check_merchant_origin_mandatory=True,
                ),
            )
        )
    )


@pytest.mark.asyncio
async def test_merchant_origin_is_blocked(
    storage, merchant, merchant_origin, dummy_logs
):
    merchant_origin.is_blocked = True
    await storage.merchant_origin.save(merchant_origin)

    with pytest.raises(CoreMerchantAccountError):
        await ValidateMerchantAction(
            merchant_id=merchant.merchant_id,
            validate_origin=True,
            merchant_origin=merchant_origin.origin,
        ).run()

    assert_that(
        dummy_logs(),
        has_item(
            has_properties(
                message='Merchant origin is blocked',
                levelno=logging.WARNING,
                _context=has_entries(
                    merchant_id=merchant.merchant_id,
                    validate_origin=True,
                    merchant_origin=merchant_origin.origin,
                    check_merchant_origin_mandatory=True,
                ),
            )
        )
    )


@pytest.mark.asyncio
async def test_merchant_origin_validation_skipped(
    merchant, merchant_origin, yandex_pay_settings, dummy_logs
):
    yandex_pay_settings.MERCHANT_ORIGIN_VALIDATION_IS_MANDATORY = False

    await ValidateMerchantAction(
        merchant_id=merchant.merchant_id,
        validate_origin=True,
        merchant_origin=merchant_origin.origin,
    ).run()

    assert_that(
        dummy_logs(),
        has_item(
            has_properties(
                message='Origin validation skipped',
                levelno=logging.INFO,
                _context=has_entries(
                    merchant_id=merchant.merchant_id,
                    validate_origin=True,
                    merchant_origin=merchant_origin.origin,
                    check_merchant_origin_mandatory=False,
                ),
            )
        )
    )
