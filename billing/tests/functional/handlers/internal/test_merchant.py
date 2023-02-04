from datetime import datetime, timedelta

import pytest

from sendr_pytest.matchers import close_to_datetime, convert_then_match
from sendr_utils import utcnow

from hamcrest import assert_that, contains_inanyorder, equal_to, has_entries, has_item

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_origin import MerchantOrigin


@pytest.fixture
async def merchant(storage, rands):
    return await storage.merchant.create(Merchant(name=rands()))


@pytest.fixture
async def merchant_origin(storage, merchant):
    return await storage.merchant_origin.create(
        MerchantOrigin(merchant_id=merchant.merchant_id, origin='https://ya.test:443')
    )


@pytest.mark.asyncio
async def test_merchant_get(
    app,
    merchant,
    merchant_origin,
    mock_internal_tvm,
):
    r = await app.get(
        f'/api/internal/v1/merchants/{str(merchant.merchant_id)}',
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        json_body,
        has_entries({
            'code': 200,
            'status': 'success',
            'data': has_entries({
                'merchant': has_entries({
                    'merchant_id': str(merchant.merchant_id),
                    'name': merchant.name,
                    'created': convert_then_match(
                        datetime.fromisoformat,
                        equal_to(merchant.created),
                    ),
                    'origins': has_item(has_entries({
                        'origin': merchant_origin.origin,
                        'created': convert_then_match(
                            datetime.fromisoformat,
                            equal_to(merchant_origin.created),
                        ),
                    })),
                })
            })
        })
    )


@pytest.mark.asyncio
async def test_merchant_put(
    app,
    merchant,
    merchant_origin,
    mock_internal_tvm,
    rands,
):
    new_name = rands()
    request_body = {
        'name': new_name,
        'origins': [{'origin': merchant_origin.origin}, {'origin': 'https://new-origin.test'}],
    }
    r = await app.put(
        f'/api/internal/v1/merchants/{str(merchant.merchant_id)}',
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
        json=request_body,
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        json_body,
        has_entries({
            'code': 200,
            'status': 'success',
            'data': has_entries({
                'merchant': has_entries({
                    'merchant_id': str(merchant.merchant_id),
                    'name': new_name,
                    'created': convert_then_match(
                        datetime.fromisoformat,
                        equal_to(merchant.created),
                    ),
                    'origins': contains_inanyorder(
                        has_entries({
                            'origin': merchant_origin.origin,
                            'created': convert_then_match(
                                datetime.fromisoformat,
                                equal_to(merchant_origin.created),
                            ),
                        }),
                        has_entries({
                            'origin': 'https://new-origin.test:443',
                            'created': convert_then_match(
                                lambda dt: datetime.fromisoformat(dt),
                                close_to_datetime(utcnow(), timedelta(seconds=5))
                            )
                        })
                    )
                })
            })
        })
    )
