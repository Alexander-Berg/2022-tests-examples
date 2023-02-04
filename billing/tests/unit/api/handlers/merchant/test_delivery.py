from datetime import datetime, timezone
from decimal import Decimal
from uuid import UUID

import pytest
from pay.lib.entities.shipping import Address, Contact, DeliveryStatus

from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.mocks import mock_action

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.accept_claim import (
    AcceptDeliveryClaimByOrderIdAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.cancel_claim import (
    CancelDeliveryClaimByOrderIdAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.cancel_info import GetDeliveryCancelInfoAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.create_claim import (
    CreateDeliveryClaimByOrderIdAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.authenticate import AuthenticateMerchantAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.enums import (
    DeliveryCancelState,
    SatisfactoryDeliveryCancelState,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.delivery import Delivery, StorageWarehouse

dummy_use_fixture = [mock_action]


@pytest.fixture
def delivery():
    return ensure_all_fields(Delivery)(
        checkout_order_id=UUID('3ead36ed-5999-4e7b-810e-590ca13bea22'),
        merchant_id=UUID('1a1d848a-aa27-4241-ab02-ad1f5306c79d'),
        delivery_id=UUID('4158d702-009c-403c-88a4-249ba802b7db'),
        external_id='external_id',
        price=Decimal('123.45'),
        warehouse=StorageWarehouse(
            address=Address(
                country='',
                locality='',
                building='',
            ),
            emergency_contact=Contact(),
            contact=Contact(),
        ),
        status=DeliveryStatus.NEW,
        raw_status='',
        actual_price=Decimal('12.34'),
        version=1,
        revision=1,
        created=datetime(2020, 12, 1, 23, 59, 59, tzinfo=timezone.utc),
        updated=datetime(2020, 12, 1, 23, 59, 59, tzinfo=timezone.utc),
    )


@pytest.fixture(autouse=True)
def mock_merchant_authorization(mock_action):  # noqa
    return mock_action(AuthenticateMerchantAction, UUID('1a1d848a-aa27-4241-ab02-ad1f5306c79d'))


@pytest.mark.asyncio
async def test_create_delivery_handler(public_app, delivery, mock_action):
    mock_create = mock_action(CreateDeliveryClaimByOrderIdAction, delivery)

    response = await public_app.post('/api/merchant/v1/orders/some-order-id/delivery/create')
    data = await response.json()

    assert data == {
        'status': 'success',
        'code': 200,
        'data': {
            'delivery': {
                'price': str(delivery.price),
                'actualPrice': str(delivery.actual_price),
                'created': str(delivery.created.isoformat()),
                'updated': str(delivery.updated.isoformat()),
                'status': str(delivery.status.name),
            }
        },
    }
    assert response.status == 200

    mock_create.assert_called_once_with(
        merchant_id=delivery.merchant_id,
        order_id='some-order-id',
    )


@pytest.mark.asyncio
async def test_accept_delivery_handler(public_app, delivery, mock_action):
    mock_accept = mock_action(AcceptDeliveryClaimByOrderIdAction, delivery)

    response = await public_app.post('/api/merchant/v1/orders/some-order-id/delivery/accept')
    data = await response.json()

    assert data == {
        'status': 'success',
        'code': 200,
        'data': {
            'delivery': {
                'price': str(delivery.price),
                'actualPrice': str(delivery.actual_price),
                'created': str(delivery.created.isoformat()),
                'updated': str(delivery.updated.isoformat()),
                'status': str(delivery.status.name),
            },
        },
    }
    assert response.status == 200

    mock_accept.assert_called_once_with(
        merchant_id=delivery.merchant_id,
        order_id='some-order-id',
    )


class TestCancelInfoHandler:
    @pytest.mark.asyncio
    async def test_response(self, public_app, mock_get_cancel_state):
        response = await public_app.get('/api/merchant/v1/orders/some-order-id/delivery/cancel-info')
        data = await response.json()

        assert_that(response.status, equal_to(200))
        assert_that(
            data,
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'cancelState': 'PAID',
                    },
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_calls_action(self, public_app, mock_get_cancel_state):
        await public_app.get('/api/merchant/v1/orders/some-order-id/delivery/cancel-info')

        mock_get_cancel_state.assert_run_once_with(
            merchant_id=UUID('1a1d848a-aa27-4241-ab02-ad1f5306c79d'), order_id='some-order-id'
        )

    @pytest.fixture
    def mock_get_cancel_state(self, mock_action):
        return mock_action(GetDeliveryCancelInfoAction, DeliveryCancelState.PAID)


class TestCancelDeliveryHandler:
    @pytest.mark.asyncio
    async def test_response(self, public_app, delivery, mock_cancel_delivery):
        response = await public_app.post(
            '/api/merchant/v1/orders/some-order-id/delivery/cancel', json={'cancelState': 'PAID'}
        )
        data = await response.json()

        assert_that(response.status, equal_to(200))
        assert_that(
            data,
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'delivery': {
                            'price': str(delivery.price),
                            'actualPrice': str(delivery.actual_price),
                            'created': str(delivery.created.isoformat()),
                            'updated': str(delivery.updated.isoformat()),
                            'status': str(delivery.status.name),
                        },
                    },
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_calls_action(self, public_app, mock_cancel_delivery):
        await public_app.post('/api/merchant/v1/orders/some-order-id/delivery/cancel', json={'cancelState': 'PAID'})

        mock_cancel_delivery.assert_run_once_with(
            merchant_id=UUID('1a1d848a-aa27-4241-ab02-ad1f5306c79d'),
            order_id='some-order-id',
            cancel_state=SatisfactoryDeliveryCancelState.PAID,
        )

    @pytest.fixture
    def mock_cancel_delivery(self, mock_action, delivery):
        return mock_action(CancelDeliveryClaimByOrderIdAction, delivery)
