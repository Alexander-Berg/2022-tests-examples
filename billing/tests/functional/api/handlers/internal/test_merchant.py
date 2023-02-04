from datetime import datetime, timedelta

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, contains_inanyorder, equal_to, has_entries, has_item

from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant
from billing.yandex_pay.yandex_pay.core.entities.merchant_origin import MerchantOrigin
from billing.yandex_pay.yandex_pay.tests.matchers import close_to_datetime, convert_then_match


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
    internal_app,
    merchant,
    merchant_origin,
    mock_internal_tvm,
):
    r = await internal_app.get(
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
    internal_app,
    merchant,
    merchant_origin,
    mock_internal_tvm,
    rands,
):
    new_name = rands()

    r = await internal_app.put(
        f'/api/internal/v1/merchants/{str(merchant.merchant_id)}',
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
        json={
            'name': new_name,
            'origins': [{'origin': merchant_origin.origin}, {'origin': 'https://new-origin.test'}],
        }
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
