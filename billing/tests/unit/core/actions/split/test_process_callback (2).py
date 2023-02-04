import uuid
from dataclasses import replace
from datetime import timedelta
from uuid import UUID

import pytest
from pay.lib.interactions.merchant.entities import TransactionStatus
from pay.lib.interactions.split.entities import YandexSplitOrderStatus

from sendr_pytest.matchers import close_to_datetime
from sendr_utils import utcnow

from hamcrest import match_equality

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.update_order import UpdateMerchantOrderAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.status_event.create import CreateOrderStatusEventAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.split.process_callback import ProcessSplitCallbackAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.status import UpdateTransactionStatusAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.enums import SplitCallbackEventType
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import OrderEventSource
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    TransactionStatus as CheckoutTransactionStatus,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import ClassicOrderStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    SplitOrderMetaData,
    SplitTransactionData,
)


@pytest.fixture(autouse=True)
async def stored_merchant(storage, entity_merchant):
    return await storage.merchant.create(
        replace(entity_merchant, split_merchant_id='fake_split_merchant_id', merchant_id=uuid.uuid4())
    )


@pytest.fixture
def split_merchant_id():
    return 'fake_split_merchant_id'


@pytest.fixture
def merchant_id(stored_merchant):
    return stored_merchant.merchant_id


@pytest.fixture(autouse=True)
def mock_merchant_order_update(mock_action):
    return mock_action(UpdateMerchantOrderAction)


@pytest.fixture(autouse=True)
def mock_plus_order_update(mock_action):
    return mock_action(CreateOrderStatusEventAction)


@pytest.fixture
def event_time_matcher():
    return match_equality(close_to_datetime(utcnow(), timedelta(seconds=10)))


@pytest.mark.parametrize(
    'split_status, merchant_status, plus_order_status',
    [
        (YandexSplitOrderStatus.NEW, None, None),
        (YandexSplitOrderStatus.PROCESSING, None, None),
        (YandexSplitOrderStatus.APPROVED, TransactionStatus.HOLD, ClassicOrderStatus.HOLD),
        (YandexSplitOrderStatus.COMMITED, TransactionStatus.SUCCESS, None),
        (YandexSplitOrderStatus.REFUNDED, TransactionStatus.REVERSE, ClassicOrderStatus.REVERSE),
        (
            YandexSplitOrderStatus.PARTIALLY_REFUNDED,
            TransactionStatus.PARTIAL_REFUND,
            ClassicOrderStatus.HOLD,
        ),
        (YandexSplitOrderStatus.FAILED, TransactionStatus.FAIL, None),
    ],
)
@pytest.mark.asyncio
async def test_update_merchant_order(
    mock_merchant_order_update,
    mock_plus_order_update,
    split_status,
    merchant_status,
    plus_order_status,
    event_time_matcher,
    merchant_id,
    split_merchant_id,
):
    await ProcessSplitCallbackAction(
        order_id='123',
        external_id='fake_external_id',
        status=split_status,
        split_merchant_id=split_merchant_id,
        event_type=SplitCallbackEventType.ORDER_UPDATE,
    ).run()

    if merchant_status is None:
        mock_merchant_order_update.assert_not_called()
    else:
        mock_merchant_order_update.assert_called_once_with(
            merchant_id=merchant_id,
            external_id='fake_external_id',
            status=merchant_status,
            event_time=event_time_matcher,
        )

    if plus_order_status is None:
        mock_plus_order_update.assert_not_called()
    else:
        mock_plus_order_update.assert_called_once_with(
            message_id=f'2:{merchant_id}_fake_external_id',
            status=plus_order_status,
            event_time=event_time_matcher,
            source=OrderEventSource.SPLIT,
        )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'split_status, plus_order_status',
    [
        (YandexSplitOrderStatus.NEW, None),
        (YandexSplitOrderStatus.PROCESSING, None),
        (YandexSplitOrderStatus.APPROVED, ClassicOrderStatus.SUCCESS),
        (YandexSplitOrderStatus.COMMITED, ClassicOrderStatus.SUCCESS),
        (YandexSplitOrderStatus.REFUNDED, None),
        (YandexSplitOrderStatus.PARTIALLY_REFUNDED, ClassicOrderStatus.SUCCESS),
        (YandexSplitOrderStatus.FAILED, None),
    ],
)
async def test_update_merchant_order_bnpl_finished(
    mock_merchant_order_update,
    mock_plus_order_update,
    split_status,
    plus_order_status,
    event_time_matcher,
    merchant_id,
    split_merchant_id,
):
    await ProcessSplitCallbackAction(
        order_id='123',
        external_id='fake_external_id',
        status=split_status,
        split_merchant_id=split_merchant_id,
        event_type=SplitCallbackEventType.BNPL_FINISHED,
    ).run()

    mock_merchant_order_update.assert_not_called()

    if plus_order_status is None:
        mock_plus_order_update.assert_not_called()
    else:
        mock_plus_order_update.assert_called_once_with(
            message_id=f'2:{merchant_id}_fake_external_id',
            status=plus_order_status,
            event_time=event_time_matcher,
            source=OrderEventSource.SPLIT,
        )


class TestCheckoutOrder:
    @pytest.fixture(autouse=True)
    async def transaction(self, storage, stored_transaction):
        stored_transaction.data.split = SplitTransactionData(
            order_meta=SplitOrderMetaData(
                order_id='split-order-id',
            ),
            checkout_url='https://split-checkout-url.test'
        )
        return await storage.transaction.save(stored_transaction)

    @pytest.fixture
    def merchant_id(self, stored_checkout_order):
        return UUID(str(stored_checkout_order.merchant_id))

    @pytest.fixture
    def params(self, stored_checkout_order, split_merchant_id):
        return dict(
            order_id='split-order-id',
            external_id=stored_checkout_order.order_id,
            status=YandexSplitOrderStatus.APPROVED,
            split_merchant_id=split_merchant_id,
            event_type=SplitCallbackEventType.ORDER_UPDATE,
        )

    @pytest.fixture(autouse=True)
    def mock_update_checkout_transaction_status(self, mock_action):
        return mock_action(UpdateTransactionStatusAction)

    @pytest.mark.asyncio
    async def test_does_not_call_merchant_update(self, params, mock_merchant_order_update):
        await ProcessSplitCallbackAction(**params).run()

        mock_merchant_order_update.assert_not_called()

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'split_status, classic_status',
        [
            (YandexSplitOrderStatus.NEW, None),
            (YandexSplitOrderStatus.PROCESSING, None),
            (YandexSplitOrderStatus.APPROVED, ClassicOrderStatus.HOLD),
            (YandexSplitOrderStatus.COMMITED, None),
            (YandexSplitOrderStatus.REFUNDED, ClassicOrderStatus.REVERSE),
            (
                YandexSplitOrderStatus.PARTIALLY_REFUNDED,
                ClassicOrderStatus.HOLD,
            ),
            (YandexSplitOrderStatus.FAILED, None),
        ],
    )
    async def test_classic_order_status_update(
        self, transaction, mock_plus_order_update, event_time_matcher, params, split_status, classic_status
    ):
        params['status'] = split_status

        await ProcessSplitCallbackAction(**params).run()

        if classic_status is None:
            mock_plus_order_update.assert_not_called()
        else:
            mock_plus_order_update.assert_called_once_with(
                message_id=transaction.message_id,
                status=classic_status,
                event_time=event_time_matcher,
                source=OrderEventSource.SPLIT,
            )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'split_status, checkout_transaction_status',
        [
            (YandexSplitOrderStatus.NEW, None),
            (YandexSplitOrderStatus.PROCESSING, None),
            (YandexSplitOrderStatus.APPROVED, CheckoutTransactionStatus.AUTHORIZED),
            (YandexSplitOrderStatus.COMMITED, CheckoutTransactionStatus.CHARGED),
            (YandexSplitOrderStatus.REFUNDED, CheckoutTransactionStatus.REFUNDED),
            (
                YandexSplitOrderStatus.PARTIALLY_REFUNDED,
                CheckoutTransactionStatus.PARTIALLY_REFUNDED
            ),
            (YandexSplitOrderStatus.FAILED, CheckoutTransactionStatus.FAILED),
        ],
    )
    async def test_updates_checkout_status(
        self,
        params,
        split_status,
        checkout_transaction_status,
        storage,
        transaction,
        stored_checkout_order,
        mock_update_checkout_transaction_status,
    ):
        params['status'] = split_status

        await ProcessSplitCallbackAction(**params).run()

        if checkout_transaction_status is None:
            mock_update_checkout_transaction_status.assert_not_called()
        else:
            transaction.order = stored_checkout_order
            mock_update_checkout_transaction_status.assert_called_once_with(
                transaction=transaction,
                status=checkout_transaction_status,
            )

    @pytest.mark.asyncio
    async def test_does_not_update_checkout_status_when_order_id_mismatch(
        self,
        params,
        mock_update_checkout_transaction_status,
    ):
        params['status'] = YandexSplitOrderStatus.APPROVED
        params['order_id'] = 'alien-split-order-id'

        await ProcessSplitCallbackAction(**params).run()

        mock_update_checkout_transaction_status.assert_not_called()

    @pytest.mark.asyncio
    async def test_does_not_update_checkout_status_when_transaction_has_no_split_data(
        self,
        params,
        mock_update_checkout_transaction_status,
        storage,
        transaction,
    ):
        params['status'] = YandexSplitOrderStatus.APPROVED
        transaction.data.split = None
        await storage.transaction.save(transaction)

        await ProcessSplitCallbackAction(**params).run()

        mock_update_checkout_transaction_status.assert_not_called()
