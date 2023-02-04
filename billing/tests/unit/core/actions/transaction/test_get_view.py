import logging
from uuid import UUID

import pytest
from pay.lib.entities.order import PaymentMethodType

from hamcrest import assert_that, contains, has_entries, has_properties, instance_of, none

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.get import GetTransactionForUserAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.get_view import GetTransactionViewAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TransactionAction, TransactionStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    SplitOrderMetaData,
    SplitTransactionData,
    Transaction,
    TransactionActionView,
    TransactionData,
    TransactionThreeDSData,
)

STATUS_WITH_ACTION = (TransactionStatus.FINGERPRINTING, TransactionStatus.THREEDS_CHALLENGE)


@pytest.fixture
def checkout_order(entity_checkout_order):
    return entity_checkout_order


@pytest.fixture
async def transaction(
    storage, checkout_order, entity_threeds_authentication_request, stored_integration,
):
    return Transaction(
        transaction_id=UUID('f59ac1dd-a1c7-49c4-82d4-da3a7dc47584'),
        checkout_order_id=checkout_order.checkout_order_id,
        integration_id=stored_integration.integration_id,
        status=TransactionStatus.NEW,
        card_id='card-x1234',
        data=TransactionData(
            user_ip='192.0.2.1',
            threeds=TransactionThreeDSData(
                authentication_request=entity_threeds_authentication_request
            ),
        ),
        version=1,
        reason='test',
        order=checkout_order,
    )


@pytest.fixture(autouse=True)
def mock_get_transaction(mock_action, transaction):
    mock_action(GetTransactionForUserAction, transaction)


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'transaction_status', list(TransactionStatus),
)
async def test_get_transaction_view(storage, entity_auth_user, transaction, transaction_status):
    transaction.status = transaction_status

    got = await GetTransactionViewAction(
        user=entity_auth_user,
        transaction_id=transaction.transaction_id,
    ).run()

    assert_that(got, instance_of(TransactionActionView))
    assert_that(
        got,
        has_properties(
            status=transaction_status,
            version=1,
            reason='test',
        )
    )


class TestPaymentMethodCard:
    @pytest.fixture
    async def checkout_order(self, storage, checkout_order):
        checkout_order.payment_method_type = PaymentMethodType.CARD
        return checkout_order

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'transaction_status, action, url',
        list(zip(
            STATUS_WITH_ACTION,
            (TransactionAction.HIDDEN_IFRAME, TransactionAction.IFRAME),
            (
                '/3ds/method/f59ac1dd-a1c7-49c4-82d4-da3a7dc47584',
                '/3ds/challenge/f59ac1dd-a1c7-49c4-82d4-da3a7dc47584/wrapper'
            ),
        )),
    )
    async def test_has_action(
        self, storage, entity_auth_user, transaction, transaction_status, action, yandex_pay_plus_settings, url
    ):
        transaction.status = transaction_status

        got = await GetTransactionViewAction(
            user=entity_auth_user,
            transaction_id=transaction.transaction_id,
        ).run()

        base_url = yandex_pay_plus_settings.THREEDS_NOTIFICATION_URL
        url = f'{base_url}{url}'
        assert_that(
            got,
            has_properties(
                action=action,
                action_url=url,
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'transaction_status', set(TransactionStatus) - set(STATUS_WITH_ACTION),
    )
    async def test_has_no_action(self, storage, entity_auth_user, transaction, transaction_status):
        transaction.status = transaction_status

        got = await GetTransactionViewAction(
            user=entity_auth_user,
            transaction_id=transaction.transaction_id,
        ).run()

        assert_that(
            got,
            has_properties(
                action=none(),
                action_url=none(),
            )
        )


class TestPaymentMethodSplit:
    @pytest.fixture
    async def checkout_order(self, storage, checkout_order):
        checkout_order.payment_method_type = PaymentMethodType.SPLIT
        return checkout_order

    @pytest.fixture
    async def transaction(self, storage, transaction):
        transaction.data.split = SplitTransactionData(
            checkout_url='https://split-checkout-url.test',
            order_meta=SplitOrderMetaData(
                order_id='split-order-id',
            ),
        )
        return transaction

    @pytest.mark.parametrize('transaction_status', [TransactionStatus.NEW])
    @pytest.mark.asyncio
    async def test_has_action(self, storage, entity_auth_user, transaction, transaction_status):
        transaction.status = transaction_status

        got = await GetTransactionViewAction(
            user=entity_auth_user,
            transaction_id=transaction.transaction_id,
        ).run()

        assert_that(
            got,
            has_properties(
                action=TransactionAction.SPLIT_IFRAME,
                action_url='https://split-checkout-url.test',
            )
        )

    @pytest.mark.parametrize('transaction_status', set(TransactionStatus) - {TransactionStatus.NEW})
    @pytest.mark.asyncio
    async def test_has_no_action(self, storage, entity_auth_user, transaction, transaction_status):
        transaction.status = transaction_status

        got = await GetTransactionViewAction(
            user=entity_auth_user,
            transaction_id=transaction.transaction_id,
        ).run()

        assert_that(
            got,
            has_properties(
                action=none(),
                action_url=none(),
            )
        )


class TestPaymentMethodCardOnDelivery:
    @pytest.fixture
    async def checkout_order(self, storage, checkout_order):
        checkout_order.payment_method_type = PaymentMethodType.CARD_ON_DELIVERY
        return checkout_order

    @pytest.mark.parametrize('transaction_status', list(TransactionStatus))
    @pytest.mark.asyncio
    async def test_has_no_action(self, storage, entity_auth_user, transaction, transaction_status):
        transaction.status = transaction_status

        got = await GetTransactionViewAction(
            user=entity_auth_user,
            transaction_id=transaction.transaction_id,
        ).run()

        assert_that(
            got,
            has_properties(
                action=none(),
                action_url=none(),
            )
        )


@pytest.mark.asyncio
async def test_action_call_logged(entity_auth_user, transaction, dummy_logs):
    view = await GetTransactionViewAction(
        user=entity_auth_user,
        transaction_id=transaction.transaction_id,
    ).run()

    logs = dummy_logs()
    assert_that(
        logs,
        contains(
            has_properties(
                message='TRANSACTION_ACTION_VIEW_REQUESTED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=entity_auth_user.uid,
                    transaction_id=transaction.transaction_id,
                )
            ),
            has_properties(
                message='TRANSACTION_ACTION_VIEW_RETURNED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=entity_auth_user.uid,
                    transaction_id=transaction.transaction_id,
                    transaction=view,
                )
            )
        ),
    )
