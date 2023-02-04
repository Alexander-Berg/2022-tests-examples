import uuid
from datetime import datetime
from decimal import Decimal

import pytest
import yarl
from pay.lib.entities.cart import Cart, CartTotal
from pay.lib.entities.order import Order

from sendr_utils import utcnow, without_none

from hamcrest import assert_that, equal_to, has_entries, has_items, has_properties

from billing.yandex_pay_admin.yandex_pay_admin.interactions.pay_backend import (
    MerchantKey,
    PayBackendClientResponseError,
    YandexPayPlusBackendClient,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.integration import Integration


@pytest.fixture
async def client(dummy_logger, request_id, setup_interactions_tvm):
    client = YandexPayPlusBackendClient(logger=dummy_logger, request_id=request_id)
    yield client
    await client.close()


@pytest.fixture
def merchant_id():
    return uuid.uuid4()


class TestMerchantKeys:
    @pytest.fixture
    def keys_endpoint(self, merchant_id, yandex_pay_admin_settings):
        url = yarl.URL(yandex_pay_admin_settings.YANDEX_PAY_PLUS_BACKEND_PRODUCTION_URL)
        return url / f'api/internal/v1/merchants/{merchant_id}/keys'

    @pytest.mark.asyncio
    async def test_get_merchant_keys(self, client, aioresponses_mocker, keys_endpoint, merchant_id):
        key_id = uuid.uuid4()
        aioresponses_mocker.get(
            keys_endpoint, payload={'data': {'keys': [{'key_id': str(key_id), 'created': '2022-02-22T20:22:00'}]}}
        )

        assert_that(
            await client.get_merchant_keys(merchant_id=merchant_id),
            has_items(
                MerchantKey(
                    key_id=key_id,
                    created=datetime(2022, 2, 22, 20, 22),
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_create_merchant_key(self, client, aioresponses_mocker, keys_endpoint, merchant_id):
        key_id = uuid.uuid4()
        aioresponses_mocker.post(
            keys_endpoint,
            payload={'data': {'key': {'key_id': str(key_id), 'created': '2022-02-22T20:22:00', 'value': '1'}}},
        )

        assert_that(
            await client.create_merchant_key(merchant_id=merchant_id),
            equal_to(
                MerchantKey(
                    key_id=key_id,
                    created=datetime(2022, 2, 22, 20, 22),
                    value='1',
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_create_merchant_key_4xx_error(self, client, aioresponses_mocker, keys_endpoint, merchant_id):
        aioresponses_mocker.post(
            keys_endpoint,
            status=400,
            payload={
                'code': 400,
                'data': {'message': 'TOO_MANY_API_KEYS'},
                'status': 'fail',
            },
        )

        with pytest.raises(PayBackendClientResponseError) as exc_info:
            await client.create_merchant_key(merchant_id=merchant_id)

        assert_that(
            exc_info.value,
            has_properties(
                message='TOO_MANY_API_KEYS',
            ),
        )

    @pytest.mark.asyncio
    async def test_delete_merchant_key(self, client, aioresponses_mocker, keys_endpoint, merchant_id):
        key_id = uuid.uuid4()
        aioresponses_mocker.delete(keys_endpoint, payload={})

        await client.delete_merchant_key(merchant_id=merchant_id, key_id=key_id),

        assert_that(
            aioresponses_mocker.requests[('DELETE', keys_endpoint)][0].kwargs,
            has_entries(
                {
                    'json': {'key_id': str(key_id)},
                }
            ),
        )


class TestIntegration:
    @pytest.fixture
    def integration_id(self):
        return uuid.uuid4()

    @pytest.fixture
    def psp_id(self):
        return uuid.uuid4()

    @pytest.fixture
    def now(self):
        return utcnow()

    @pytest.fixture
    def integration_endpoint(self, integration_id, yandex_pay_admin_settings):
        url = yarl.URL(yandex_pay_admin_settings.YANDEX_PAY_PLUS_BACKEND_PRODUCTION_URL)
        return url / f'api/internal/v1/integrations/{integration_id}'

    @pytest.fixture
    def integration_entity(self, integration_id, psp_id, merchant_id, now):
        return Integration(
            integration_id=integration_id,
            merchant_id=merchant_id,
            psp_id=psp_id,
            psp_external_id='payture',
            status=IntegrationStatus.READY,
        )

    @pytest.fixture
    def plus_backend_response(self, integration_entity, now):
        return {
            'data': {
                'integration': {
                    'integration_id': str(integration_entity.integration_id),
                    'merchant_id': str(integration_entity.merchant_id),
                    'psp_id': str(integration_entity.psp_id),
                    'status': integration_entity.status.value,
                    'enabled': True,
                    'created': now.isoformat(),
                    'updated': now.isoformat(),
                }
            }
        }

    @pytest.mark.asyncio
    @pytest.mark.parametrize('creds', [None, 'secret'])
    async def test_upsert_integration(
        self, client, aioresponses_mocker, integration_endpoint, integration_entity, plus_backend_response, creds, now
    ):
        mock = aioresponses_mocker.patch(
            integration_endpoint,
            payload=plus_backend_response,
        )

        await client.upsert_integration(integration=integration_entity, creds=creds)

        mock.assert_called_once()
        expected = without_none(
            {
                'merchant_id': str(integration_entity.merchant_id),
                'psp_id': str(integration_entity.psp_id),
                'status': integration_entity.status.value,
                'creds': creds,
            }
        )
        assert_that(
            mock.call_args.kwargs['json'],
            equal_to(expected),
        )

    @pytest.mark.asyncio
    async def test_upsert_integration_4xx_error(
        self, client, aioresponses_mocker, integration_entity, integration_endpoint
    ):
        aioresponses_mocker.patch(
            integration_endpoint,
            status=400,
            payload={
                'code': 400,
                'data': {'message': 'INTEGRATION_CREDENTIALS_REQUIRED'},
                'status': 'fail',
            },
        )

        with pytest.raises(PayBackendClientResponseError) as exc_info:
            await client.upsert_integration(integration=integration_entity)

        assert_that(
            exc_info.value,
            has_properties(
                message='INTEGRATION_CREDENTIALS_REQUIRED',
            ),
        )

    @pytest.mark.asyncio
    async def test_delete_integration(self, client, aioresponses_mocker, integration_endpoint, integration_id):
        mock = aioresponses_mocker.delete(integration_endpoint, payload={})

        await client.delete_integration(integration_id=integration_id),

        mock.assert_called_once()


class TestOrders:
    @pytest.fixture
    def orders_endpoint(self, merchant_id, yandex_pay_admin_settings):
        url = yarl.URL(yandex_pay_admin_settings.YANDEX_PAY_PLUS_BACKEND_PRODUCTION_URL)
        return url / f'api/internal/v1/merchants/{merchant_id}/orders'

    @pytest.mark.asyncio
    async def test_get_merchant_orders(self, client, aioresponses_mocker, orders_endpoint, merchant_id):
        order_id = uuid.uuid4()
        aioresponses_mocker.get(
            f'{orders_endpoint}?limit=10',
            payload={
                'data': {
                    'orders': [
                        {
                            'currency_code': 'GBP',
                            'checkout_order_id': str(order_id),
                            'cart': {
                                'items': [],
                                'total': {'amount': '441.00'},
                            },
                        }
                    ]
                }
            },
        )

        assert_that(
            await client.get_orders(merchant_id=merchant_id, limit=10),
            has_items(
                Order(
                    currency_code='GBP',
                    checkout_order_id=order_id,
                    cart=Cart(items=[], total=CartTotal(amount=Decimal('441.00'))),
                )
            ),
        )
