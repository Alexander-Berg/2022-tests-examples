import json
from datetime import timezone
from decimal import Decimal
from uuid import uuid4

import pytest

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.send_to_payments_history import (
    SendToPaymentsHistoryAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import OrderSchemaValidationError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import PaymentMethodType
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import ClassicOrderStatus, Order, OrderData

ORDER_AMOUNT = Decimal('1000')
CASHBACK_AMOUNT = Decimal('30')
CASHBACK_CATEGORY = Decimal('0.03')
ORDER_STATUS_SEQ = (ClassicOrderStatus.HOLD, ClassicOrderStatus.SUCCESS, ClassicOrderStatus.REFUND)


@pytest.fixture
async def customer():
    return await CreateCustomerAction(uid=456).run()


@pytest.fixture
async def trust_card_id(rands):
    return f'card-x{rands()}'


@pytest.fixture(autouse=True)
def mock_xts_default_cashback(yandex_pay_plus_settings):
    yandex_pay_plus_settings.CASHBACK_USER_SHEET_SPENDING_LIMIT['XTS'] = 1000
    yandex_pay_plus_settings.CASHBACK_CARD_SHEET_SPENDING_LIMIT['XTS'] = 3000


@pytest.fixture
async def order(storage, customer, rands, trust_card_id):
    async def _order(data=None, payment_id=''):
        return await storage.order.create(
            Order(
                uid=customer.uid,
                message_id=rands(),
                currency='XTS',
                amount=ORDER_AMOUNT,
                cashback=CASHBACK_AMOUNT,
                cashback_category=CASHBACK_CATEGORY,
                psp_id=uuid4(),
                merchant_id=uuid4(),
                status=ClassicOrderStatus.NEW,
                trust_card_id=trust_card_id,
                data=data or OrderData(),
                payment_method_type=PaymentMethodType.CARD,
                payment_id=payment_id,
            )
        )
    return _order


class ProducerMock:
    def __init__(self, *args, **kwargs) -> None:
        pass

    async def __aenter__(self, *args, **kwargs):
        return self

    async def __aexit__(self, *args, **kwargs):
        pass

    async def write(self, data):
        pass


@pytest.fixture
def logbroker_mock(mocker):
    write_mock = mocker.AsyncMock()

    mocker.patch.object(ProducerMock, 'write', write_mock)
    mocker.patch.object(SendToPaymentsHistoryAction, 'producer_cls', ProducerMock)
    mocker.patch.object(SendToPaymentsHistoryAction, 'logbroker_cls', mocker.MagicMock())

    return write_mock


@pytest.mark.asyncio
async def test_send_success_basic(logbroker_mock, order):
    order: Order = await order()

    await SendToPaymentsHistoryAction(uid=order.uid, order_id=order.order_id).run()

    args = json.loads(logbroker_mock.call_args.args[0])

    logbroker_mock.assert_called_once()

    assert args['status'] == 'created'
    assert args['items'] == []
    assert args['amount'] == order.amount
    assert args['updated'] == order.updated.astimezone(timezone.utc).isoformat()
    assert args['created'] == order.created.astimezone(timezone.utc).isoformat()
    assert args['merchant']['id'] == str(order.merchant_id)
    assert args['message_id'] == order.message_id
    assert args['uid'] == order.uid
    assert args['currency'] == order.currency


@pytest.mark.asyncio
async def test_send_success_with_basket(logbroker_mock, order):
    data = OrderData(
        merchant_name='some_name',
        merchant_url='some_url',
        order_basket={
            "id": "123",
            "items": [{"type": "ITEM", "label": "Сумма чаевых", "amount": "1.00", "quantity": None}],
            "total": {"label": None, "amount": "1.00"}
        }
    )
    order = await order(data=data, payment_id='123')

    await SendToPaymentsHistoryAction(uid=order.uid, order_id=order.order_id).run()

    args = json.loads(logbroker_mock.call_args.args[0])

    logbroker_mock.assert_called_once()
    assert args['message_id'] == f'{order.message_id}:123'
    assert args['status'] == 'created'
    assert args['items'] == [{
        'amount': '1.00',
        'type': 'ITEM',
        'label': 'Сумма чаевых',
    }]
    assert args['merchant']['id'] == str(order.merchant_id)
    assert args['merchant']['gateway_name'] == 'some_name'
    assert args['merchant']['gateway_url'] == 'some_url'


@pytest.mark.asyncio
async def test_send_bad_data(logbroker_mock, order, storage):
    order: Order = await order()
    order.data = OrderData(
        order_basket={
            'items': [
                {'quantity': {'label': 'hello'}}  # count - required
            ]
        }
    )
    await storage.order.save(order)

    with pytest.raises(OrderSchemaValidationError):
        await SendToPaymentsHistoryAction(uid=order.uid, order_id=order.order_id).run()


@pytest.mark.asyncio
async def test_when_history_disabled__does_not_send(logbroker_mock, order, storage, yandex_pay_plus_settings):
    yandex_pay_plus_settings.PAYMENT_HISTORY_ENABLED = False
    order = await order()

    await SendToPaymentsHistoryAction(uid=order.uid, order_id=order.order_id).run()

    logbroker_mock.assert_not_called()
