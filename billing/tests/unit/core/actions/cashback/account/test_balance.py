import logging
from decimal import Decimal

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, has_entries, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.account.balance import (
    LogCashbackAccountBalanceAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments import TrustPaymentsClient
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments.entities import (
    CardTrustPaymentMethod,
    YandexAccountTrustPaymentMethod,
    YandexPlusAccount,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_account import CashbackAccount


@pytest.fixture
def params(account):
    return {
        'cashback_account': account,
        'message_id': '0:msgid',
        'cashback_transaction_purchase_token': 'topup-purchase-token',
    }


@pytest.fixture
async def customer():
    return await CreateCustomerAction(uid=1551).run()


@pytest.fixture
def trust_account(customer):
    return YandexPlusAccount(
        account_id='acc_id',
        uid=customer.uid,
        currency='XTS',
    )


@pytest.fixture
async def account(storage, trust_account, customer):
    return await storage.cashback_account.create(
        CashbackAccount(
            uid=customer.uid,
            currency='XTS',
            trust_account_id=trust_account.account_id,
        ),
    )


@pytest.fixture
def mock_trust_get_payment_methods(mocker, trust_account):
    return mocker.patch.object(
        TrustPaymentsClient, 'get_payment_methods', mocker.AsyncMock(
            return_value=[
                CardTrustPaymentMethod(
                    id='card-x1234',
                    card_id='card-x3a6979846d97cedc82925d9c',
                    binding_systems=['trust'],
                    orig_uid=trust_account.uid,
                    system='MasterCard',
                    payment_system='MasterCard',
                    expiration_month='09',
                    expiration_year='2099',
                    card_bank='SBERBANK OF RUSSIA',
                    expired=False,
                    account='123456****7890',
                    last_paid_ts=utcnow(),
                    binding_ts=utcnow(),
                ),
                YandexAccountTrustPaymentMethod(
                    id='yandex-account-1',
                    balance=Decimal('10.00'),
                    currency='XTS',
                    account='not' + trust_account.account_id,
                ),
                YandexAccountTrustPaymentMethod(
                    id='yandex-account-2',
                    balance=Decimal('20.00'),
                    currency='XTS',
                    account=trust_account.account_id,
                ),
            ],
        )
    )


@pytest.fixture
def mock_trust_get_payment_methods_empty(mocker, trust_account):
    return mocker.patch.object(TrustPaymentsClient, 'get_payment_methods', mocker.AsyncMock(return_value=[]))


@pytest.mark.asyncio
async def test_calls_trust_get_payment_methods(mock_trust_get_payment_methods, params, account):
    await LogCashbackAccountBalanceAction(**params).run()

    mock_trust_get_payment_methods.assert_awaited_once_with(uid=account.uid)


@pytest.mark.asyncio
async def test_logs_balance(mock_trust_get_payment_methods, account, dummy_logs, params):
    await LogCashbackAccountBalanceAction(**params).run()

    [*_, log] = dummy_logs()
    assert_that(
        log,
        has_properties(
            message='CASHBACK_REFUND_WALLET_BALANCE',
            levelno=logging.INFO,
            _context=has_entries(
                uid=account.uid,
                yandex_account_balance=Decimal('20.00'),
                message_id='0:msgid',
                cashback_transaction_purchase_token='topup-purchase-token',
            ),
        )
    )


@pytest.mark.asyncio
async def test_logs_balance__when_account_not_found(mock_trust_get_payment_methods_empty, account, dummy_logs, params):
    await LogCashbackAccountBalanceAction(**params).run()

    [*_, log] = dummy_logs()
    assert_that(
        log,
        has_properties(
            message='CASHBACK_REFUND_WALLET_BALANCE_NOT_FOUND',
            levelno=logging.ERROR,
            _context=has_entries(
                uid=account.uid,
                yandex_account_balance=None,
                message_id='0:msgid',
                cashback_transaction_purchase_token='topup-purchase-token',
            ),
        )
    )
