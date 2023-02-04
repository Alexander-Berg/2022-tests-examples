import pytest

from hamcrest import assert_that, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.account.ensure import EnsureCashbackAccountAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments import TrustPaymentsClient
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments.entities import YandexPlusAccount
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_account import CashbackAccount


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


@pytest.fixture(autouse=True)
def mock_create_plus_account(mocker, trust_account):
    return mocker.patch.object(
        TrustPaymentsClient, 'create_plus_account', mocker.AsyncMock(return_value=trust_account)
    )


@pytest.mark.asyncio
async def test_calls_create_account_if_not_exists(mock_create_plus_account, customer):
    await EnsureCashbackAccountAction(customer.uid, 'XTS').run()

    mock_create_plus_account.assert_awaited_once_with(
        uid=customer.uid,
        currency='XTS',
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('account')
async def test_reuses_account_if_exists(mock_create_plus_account, customer):
    await EnsureCashbackAccountAction(customer.uid, 'XTS').run()

    mock_create_plus_account.assert_not_called()


@pytest.mark.asyncio
async def test_creates_account_if_not_exists(storage, trust_account, customer):
    await EnsureCashbackAccountAction(customer.uid, 'XTS').run()

    account = await storage.cashback_account.get_by_uid_and_currency(uid=customer.uid, currency='XTS')
    assert_that(
        account,
        has_properties({
            'trust_account_id': trust_account.account_id,
        }),
    )
