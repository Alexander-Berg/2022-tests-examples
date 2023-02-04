import re

import pytest

from hamcrest import assert_that, equal_to, has_entries


@pytest.mark.asyncio
async def test_unauthorized(public_app, yandex_pay_plus_settings):
    r = await public_app.post(
        '/api/public/v1/orders/d25edd4c-444a-4727-a6fc-e28ed0803223/transactions',
    )
    data = await r.json()

    assert_that(r.status, equal_to(401))
    assert_that(data, equal_to({'code': 401, 'status': 'fail', 'data': {'message': 'MISSING_CREDENTIALS'}}))


@pytest.mark.asyncio
async def test_create_transaction(
    public_app, yandex_pay_plus_settings, authenticate_client, stored_merchant, create_order
):
    authenticate_client(public_app)
    order = await create_order()
    checkout_order_id = order['checkout_order_id']

    r = await public_app.post(
        f'/api/public/v1/orders/{checkout_order_id}/transactions',
        headers={'x-pay-session-id': 'sessid-123', 'x-forwarded-for-y': '192.0.2.1'},
        json={
            'card_id': 'card-x1234',
            'plan_id': '1',
            'browser_data': {
                'java_enabled': True,
                'language': 'ru',
                'screen_color_depth': 24,
                'screen_height': 1080,
                'screen_width': 1960,
                'timezone': -180,
                'window_height': 1080,
                'window_width': 1960
            },
            'challenge_return_path': 'https://challenge.test.ya.ru',
        },
    )
    data = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        data['data']['transaction'],
        has_entries(
            {
                'status': 'NEW',
                'version': 1,
            }
        ),
    )


@pytest.fixture(autouse=True)
async def integration(create_integration, stored_unittest_psp, stored_merchant):
    return await create_integration(
        merchant_id=stored_merchant.merchant_id,
        psp_id=stored_unittest_psp.psp_id,
    )


@pytest.fixture(autouse=True)
def mock_shipping_address(aioresponses_mocker, yandex_pay_plus_settings):
    return aioresponses_mocker.get(
        re.compile(rf'{yandex_pay_plus_settings.PASSPORT_ADDRESSES_URL}/address/get\?id=ship-a-id&.*'),
        payload={
            'id': 'ship-a-id',
            'country': 'passp-country',
            'locality': 'passp-locality',
            'building': 'passp-building',
        }
    )


@pytest.fixture(autouse=True)
def mock_shipping_contact(aioresponses_mocker, yandex_pay_plus_settings):
    return aioresponses_mocker.get(
        re.compile(rf'{yandex_pay_plus_settings.PASSPORT_ADDRESSES_URL}/contact/get\?id=ship-c-id&.*'),
        payload={
            'id': 'ship-c-id',
            'phone': '+70001112233',
            'email': 'address@email.test',
            'first_name': 'fname',
            'second_name': 'sname',
            'last_name': 'lname',
        }
    )


@pytest.fixture(autouse=True)
def mock_merchant_order(aioresponses_mocker, yandex_pay_plus_settings):
    return aioresponses_mocker.post(
        yandex_pay_plus_settings.ZORA_URL, payload={'status': 'success', 'data': {'orderId': 'real-order-id'}}
    )


@pytest.fixture(autouse=True)
def mock_merchant_webhook(mock_merchant_order, aioresponses_mocker, yandex_pay_plus_settings):
    return aioresponses_mocker.post(
        yandex_pay_plus_settings.ZORA_URL, payload={'status': 'success'}
    )
