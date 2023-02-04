import uuid

import pytest
from pay.lib.entities.enums import PaymentMethodType
from pay.lib.entities.order import PaymentStatus

from sendr_pytest.mocks import mock_action  # noqa

from hamcrest import assert_that, equal_to, has_properties, match_equality

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.notify import NotifyMerchantTransactionAsyncAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.status import UpdateTransactionStatusAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    Transaction,
    TransactionData,
    TransactionStatus,
    TransactionThreeDSData,
)


@pytest.mark.asyncio
async def test_updates_transaction(storage, transaction):
    updated_transaction = await UpdateTransactionStatusAction(transaction, status=TransactionStatus.AUTHORIZED).run()

    stored = await storage.transaction.get(transaction.transaction_id)
    assert_that(stored, equal_to(updated_transaction))
    assert_that(stored.order, has_properties(
        payment_status=PaymentStatus.AUTHORIZED,
        payment_method_type=PaymentMethodType.SPLIT,
    ))
    assert_that(
        updated_transaction,
        has_properties({
            'status': TransactionStatus.AUTHORIZED,
            'version': transaction.version + 1,
        })
    )


@pytest.mark.parametrize(
    'save_param, expected_version',
    (
        (False, 1),
        (True, 2),
    ),
)
@pytest.mark.asyncio
async def test_saves(storage, transaction, save_param, expected_version):
    transaction.version = 1
    transaction = await storage.transaction.save(transaction)
    await UpdateTransactionStatusAction(transaction, status=TransactionStatus.AUTHORIZED, save=save_param).run()

    assert_that(
        await storage.transaction.get(transaction.transaction_id),
        has_properties({
            'version': expected_version,
        })
    )


@pytest.mark.parametrize(
    'status',
    set(TransactionStatus) - {
        TransactionStatus.NEW, TransactionStatus.FINGERPRINTING, TransactionStatus.THREEDS_CHALLENGE
    }
)
@pytest.mark.asyncio
async def test_notifies_merchant(transaction, status, mock_notify_merchant):
    transaction = await UpdateTransactionStatusAction(transaction, status=status).run()

    mock_notify_merchant.assert_run_once_with(match_equality(has_properties(transaction_id=transaction.transaction_id)))


@pytest.mark.parametrize(
    'status',
    set(TransactionStatus) - {
        TransactionStatus.AUTHORIZED,
        TransactionStatus.CHARGED,
        TransactionStatus.FAILED,
        TransactionStatus.VOIDED,
        TransactionStatus.REFUNDED,
        TransactionStatus.PARTIALLY_REFUNDED,
    },
)
@pytest.mark.asyncio
async def test_not_notifies_merchant(transaction, status, mock_notify_merchant):
    transaction = await UpdateTransactionStatusAction(transaction, status=status).run()

    mock_notify_merchant.assert_not_run()


@pytest.fixture(autouse=True)
def mock_notify_merchant(mock_action):  # noqa
    return mock_action(NotifyMerchantTransactionAsyncAction)


@pytest.fixture
async def transaction(storage, stored_checkout_order, entity_threeds_authentication_request, stored_integration):
    transaction = await storage.transaction.create(
        Transaction(
            transaction_id=uuid.UUID('ac3a67e1-9df1-4612-bfdc-c82a3c549c14'),
            checkout_order_id=stored_checkout_order.checkout_order_id,
            integration_id=stored_integration.integration_id,
            payment_method=PaymentMethodType.SPLIT,
            status=TransactionStatus.NEW,
            card_id='card-x1234',
            data=TransactionData(
                user_ip='192.0.2.1',
                threeds=TransactionThreeDSData(
                    authentication_request=entity_threeds_authentication_request
                ),
            ),
            version=1,
        )
    )
    transaction.order = stored_checkout_order
    return transaction
