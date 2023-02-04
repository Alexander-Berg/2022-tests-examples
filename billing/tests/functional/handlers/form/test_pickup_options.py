import json

import pytest
from jose import jws

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant

MERCHANT_BASE_URL = 'https://merchant.test/yandexpay'


@pytest.fixture(autouse=True)
async def merchant(storage, rands):
    return await storage.merchant.create(Merchant(name=rands(), callback_url=MERCHANT_BASE_URL))


@pytest.fixture
def pickup_option():
    return {
        'pickupPointId': 'option-1',
        'provider': 'IN_STORE',
        'address': 'address',
        'location': {'latitude': 12.34, 'longitude': 43.21},
        'title': 'title',
        'fromDate': '2022-03-11',
        'toDate': '2022-04-11',
        'amount': '42.00',
        'description': 'description',
        'phones': ['+79876543210'],
        'schedule': [{'label': 'weekdays', 'fromTime': '08:00', 'toTime': '20:00'}],
        'storagePeriod': 7,
        'allowedPaymentMethods': ['CARD'],
    }


class TestPickupOptions:
    @pytest.fixture
    def params(self, merchant):
        return {
            'merchant_id': str(merchant.merchant_id),
            'currency_code': 'XTS',
            'cart': {
                'items': [],
            },
            'bounding_box': {
                'ne': {'latitude': 12.34, 'longitude': 43.21},
                'sw': {'latitude': 23.45, 'longitude': 32.10},
            },
            'metadata': 'metadata',
        }

    @pytest.fixture
    def mock_merchant(self, aioresponses_mocker, yandex_pay_plus_settings, pickup_option):
        return aioresponses_mocker.post(
            yandex_pay_plus_settings.ZORA_URL, payload={
                'status': 'success',
                'data': {
                    'pickupOptions': [pickup_option],
                }
            }
        )

    @pytest.mark.asyncio
    async def test_unauthorized(self, public_app, params):
        r = await public_app.post(
            '/api/public/v1/pickup-options',
            json=params,
            headers={'x-pay-session-id': 'sessid-123'},
        )
        data = await r.json()

        assert_that(r.status, equal_to(401))
        assert_that(data, equal_to({'code': 401, 'status': 'fail', 'data': {'message': 'MISSING_CREDENTIALS'}}))

    @pytest.mark.asyncio
    async def test_get_pickup_options(self, public_app, authenticate_client, params, mock_merchant):
        authenticate_client(public_app)

        r = await public_app.post(
            '/api/public/v1/pickup-options',
            json=params,
            headers={'x-pay-session-id': 'sessid-123'},
        )
        data = await r.json()

        assert_that(r.status, equal_to(200))
        assert_that(
            data['data']['pickup_options'],
            equal_to([{
                'pickup_point_id': 'option-1',
                'provider': 'IN_STORE',
                'address': 'address',
                'location': {'latitude': 12.34, 'longitude': 43.21},
                'title': 'title',
                'from_date': '2022-03-11',
                'to_date': '2022-04-11',
                'amount': '42.00',
                'description': 'description',
                'phones': ['+79876543210'],
                'schedule': [{'label': 'weekdays', 'from_time': '08:00:00', 'to_time': '20:00:00'}],
                'storage_period': 7,
                'allowed_payment_methods': ['CARD'],
            }]),
        )
        _, call_kwargs = mock_merchant.call_args_list[0]
        assert_that(
            json.loads(jws.get_unverified_claims(call_kwargs['data'])),
            equal_to({
                'cart': {
                    'cartId': '31454b66e98cf80d557294b53394f0255dc1403e9ca0334b38dab5033c9bb372',
                    'items': [],
                },
                'boundingBox': params['bounding_box'],
                'metadata': params['metadata'],
                'merchantId': params['merchant_id'],
                'currencyCode': 'XTS',
            }),
        )


class TestPickupOptionDetails:
    @pytest.fixture
    def params(self, merchant):
        return {
            'merchant_id': str(merchant.merchant_id),
            'currency_code': 'XTS',
            'cart': {
                'items': [],
            },
            'pickup_point_id': 'option-1',
            'metadata': 'metadata',
        }

    @pytest.fixture
    def mock_merchant(self, aioresponses_mocker, yandex_pay_plus_settings, pickup_option):
        return aioresponses_mocker.post(
            yandex_pay_plus_settings.ZORA_URL, payload={
                'status': 'success',
                'data': {'pickupOption': pickup_option}
            }
        )

    @pytest.mark.asyncio
    async def test_unauthorized(self, public_app, params):
        r = await public_app.post(
            '/api/public/v1/pickup-option-details',
            json=params,
            headers={'x-pay-session-id': 'sessid-123'},
        )
        data = await r.json()

        assert_that(r.status, equal_to(401))
        assert_that(data, equal_to({'code': 401, 'status': 'fail', 'data': {'message': 'MISSING_CREDENTIALS'}}))

    @pytest.mark.asyncio
    async def test_get_pickup_option_details(self, public_app, authenticate_client, params, mock_merchant):
        authenticate_client(public_app)

        r = await public_app.post(
            '/api/public/v1/pickup-option-details',
            json=params,
            headers={'x-pay-session-id': 'sessid-123'},
        )
        data = await r.json()

        assert_that(r.status, equal_to(200))
        assert_that(
            data['data']['pickup_option'],
            equal_to({
                'pickup_point_id': 'option-1',
                'provider': 'IN_STORE',
                'address': 'address',
                'location': {'latitude': 12.34, 'longitude': 43.21},
                'title': 'title',
                'from_date': '2022-03-11',
                'to_date': '2022-04-11',
                'amount': '42.00',
                'description': 'description',
                'phones': ['+79876543210'],
                'schedule': [{'label': 'weekdays', 'from_time': '08:00:00', 'to_time': '20:00:00'}],
                'storage_period': 7,
                'allowed_payment_methods': ['CARD'],
            }),
        )
        _, call_kwargs = mock_merchant.call_args_list[0]
        assert_that(
            json.loads(jws.get_unverified_claims(call_kwargs['data'])),
            equal_to({
                'cart': {
                    'cartId': '31454b66e98cf80d557294b53394f0255dc1403e9ca0334b38dab5033c9bb372',
                    'items': [],
                },
                'pickupPointId': params['pickup_point_id'],
                'metadata': params['metadata'],
                'merchantId': params['merchant_id'],
                'currencyCode': 'XTS',
            }),
        )
