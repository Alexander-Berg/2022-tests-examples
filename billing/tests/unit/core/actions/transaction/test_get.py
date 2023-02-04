import logging
from dataclasses import replace
from uuid import uuid4

import pytest

from sendr_aiopg.engine.lazy import Preset

from hamcrest import assert_that, equal_to, has_entries, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.get import GetTransactionForUserAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.user import User
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import TransactionNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TransactionStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    Transaction,
    TransactionData,
    TransactionThreeDSData,
)


@pytest.fixture
async def transaction(
    storage, stored_checkout_order, entity_threeds_authentication_request, stored_integration
):
    transaction = Transaction(
        transaction_id=uuid4(),
        checkout_order_id=stored_checkout_order.checkout_order_id,
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
    )
    return await storage.transaction.create(transaction)


@pytest.mark.asyncio
async def test_get_transaction(entity_auth_user, transaction, stored_checkout_order):
    got = await GetTransactionForUserAction(
        transaction_id=transaction.transaction_id,
        user=entity_auth_user,
    ).run()

    transaction = replace(
        transaction,
        order=stored_checkout_order,
    )
    assert_that(got, equal_to(transaction))


@pytest.mark.asyncio
async def test_get_transaction_missing(entity_auth_user):
    with pytest.raises(TransactionNotFoundError):
        await GetTransactionForUserAction(
            transaction_id=uuid4(),
            user=entity_auth_user,
        ).run()


@pytest.mark.asyncio
async def test_get_transaction_wrong_user(entity_auth_user, transaction, dummy_logs):
    with pytest.raises(TransactionNotFoundError):
        await GetTransactionForUserAction(
            transaction_id=transaction.transaction_id,
            user=User(entity_auth_user.uid + 1),
        ).run()

    [log] = dummy_logs()
    assert_that(
        log,
        has_properties(
            message='TRANSACTION_UID_MISMATCH',
            levelno=logging.WARNING,
            _context=has_entries(
                transaction_uid=entity_auth_user.uid,
                uid=entity_auth_user.uid + 1,
            )
        )
    )


@pytest.mark.asyncio
async def test_get_transaction_replica_read(mocker, entity_auth_user, transaction):
    action = GetTransactionForUserAction(
        transaction_id=transaction.transaction_id,
        user=entity_auth_user,
        allow_replica_read=True,
    )
    action.allow_connection_reuse = False
    spy = mocker.spy(action.db_engine, 'using')

    await action.run()

    spy.assert_called_once_with(Preset.ACTUAL_LOCAL.value)
