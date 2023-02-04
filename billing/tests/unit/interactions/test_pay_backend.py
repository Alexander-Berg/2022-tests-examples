import uuid

import pytest
import yarl
from pay.lib.entities.cart import Measurements
from pay.lib.entities.contact import Contact
from pay.lib.entities.shipping import Address, ShippingWarehouse

from sendr_interactions.exceptions import InteractionResponseError

from hamcrest import assert_that, has_entries, has_properties

from billing.yandex_pay_admin.yandex_pay_admin.interactions.pay_backend import (
    PayBackendClientResponseError,
    YandexPayBackendClient,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import (
    DeliveryIntegrationParams,
    YandexDeliveryParams,
)


@pytest.fixture
async def client(dummy_logger, request_id, setup_interactions_tvm):
    client = YandexPayBackendClient(logger=dummy_logger, request_id=request_id)
    yield client
    await client.close()


@pytest.mark.asyncio
async def test_put_psp_call(client, aioresponses_mocker, yandex_pay_admin_settings):
    psp_id = uuid.uuid4()
    url = yarl.URL(yandex_pay_admin_settings.YANDEX_PAY_BACKEND_PRODUCTION_URL)
    url /= f'api/internal/v1/psp/{psp_id}'
    aioresponses_mocker.put(
        url,
        payload={},
    )

    await client.put_psp(psp_id=psp_id, public_key='pubkey', public_key_signature='sig', psp_external_id='gw-id')

    assert_that(
        aioresponses_mocker.requests[('PUT', url)][0].kwargs,
        has_entries(
            {
                'json': {
                    'public_key': 'pubkey',
                    'public_key_signature': 'sig',
                    'psp_external_id': 'gw-id',
                    'psp_auth_keys': [],
                },
            }
        ),
    )


@pytest.mark.parametrize(
    'callback_url, delivery, expected_delivery',
    [
        (None, None, None),
        (None, dict(oauth_token='t', warehouses=[]), dict(encrypted_oauth_token='t', warehouses=[])),
        (
            'https://url.test',
            dict(oauth_token='t', autoaccept=False),
            dict(encrypted_oauth_token='t', autoaccept=False),
        ),
        (
            None,
            dict(
                oauth_token='t',
                warehouses=[
                    ShippingWarehouse(
                        address=Address(country='Russia', locality='Moscow', building='1'),
                        contact=Contact(email='email'),
                        emergency_contact=Contact(phone='phone'),
                    )
                ],
            ),
            dict(
                encrypted_oauth_token='t',
                warehouses=[
                    {
                        'address': {'country': 'Russia', 'locality': 'Moscow', 'building': '1'},
                        'contact': {'email': 'email'},
                        'emergency_contact': {'phone': 'phone'},
                    }
                ],
            ),
        ),
    ],
)
@pytest.mark.asyncio
async def test_put_merchant_call(
    client,
    aioresponses_mocker,
    yandex_pay_admin_settings,
    callback_url,
    delivery,
    expected_delivery,
):
    merchant_id = uuid.uuid4()
    partner_id = uuid.uuid4()
    url = yarl.URL(yandex_pay_admin_settings.YANDEX_PAY_BACKEND_PRODUCTION_URL)
    url /= f'api/internal/v1/merchants/{merchant_id}'
    aioresponses_mocker.put(
        url,
        payload={},
    )

    delivery_params = (
        DeliveryIntegrationParams(YandexDeliveryParams(**delivery), Measurements(1, 2, 3, 4)) if delivery else None
    )
    await client.put_merchant(
        merchant_id=merchant_id,
        name='test',
        origins=['a', 'b'],
        partner_id=partner_id,
        callback_url=callback_url,
        delivery_integration_params=delivery_params,
    )

    expected = {
        'name': 'test',
        'origins': [
            {'origin': 'a'},
            {'origin': 'b'},
        ],
        'partner_id': str(partner_id),
    }

    expected_measurements = {'length': 1.0, 'height': 2.0, 'width': 3.0, 'weight': 4.0} if delivery else None

    if callback_url:
        expected['callback_url'] = callback_url
    if expected_delivery:
        expected['delivery_integration_params'] = {
            'yandex_delivery': expected_delivery,
            'measurements': expected_measurements,
        }
    assert_that(
        aioresponses_mocker.requests[('PUT', url)][0].kwargs,
        has_entries({'json': expected}),
    )


@pytest.mark.asyncio
async def test_put_merchant_4xx_error(client, aioresponses_mocker, yandex_pay_admin_settings):
    merchant_id = uuid.uuid4()
    url = yarl.URL(yandex_pay_admin_settings.YANDEX_PAY_BACKEND_PRODUCTION_URL)
    url /= f'api/internal/v1/merchants/{merchant_id}'
    aioresponses_mocker.put(
        url,
        status=400,
        payload={
            "code": 400,
            "data": {
                "params": {
                    "description": "Insecure origin schema: HTTPS is expected.",
                    "origin": "http://test.fake",
                },
                "message": "INSECURE_MERCHANT_ORIGIN",
            },
            "status": "fail",
        },
    )

    with pytest.raises(PayBackendClientResponseError) as exc_info:
        await client.put_merchant(merchant_id=merchant_id, name='test', origins=['a', 'b'])

    assert_that(
        exc_info.value,
        has_properties(
            message='INSECURE_MERCHANT_ORIGIN',
            params={
                'description': 'Insecure origin schema: HTTPS is expected.',
                'origin': 'http://test.fake',
            },
        ),
    )


@pytest.mark.asyncio
async def test_put_merchant_5xx_error(client, aioresponses_mocker, yandex_pay_admin_settings, request_id):
    client.REQUEST_RETRY_TIMEOUTS = ()
    merchant_id = uuid.uuid4()
    url = yarl.URL(yandex_pay_admin_settings.YANDEX_PAY_BACKEND_PRODUCTION_URL)
    url /= f'api/internal/v1/merchants/{merchant_id}'
    aioresponses_mocker.put(url, status=500, body='Server error')

    with pytest.raises(InteractionResponseError) as exc_info:
        await client.put_merchant(merchant_id=merchant_id, name='test', origins=['a', 'b'])

    assert_that(
        exc_info.value,
        has_properties(
            params={'error': 'Invalid response Content-Type', 'request_id': request_id},
        ),
    )
