import pytest

from hamcrest import assert_that, equal_to, has_property

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_account import CashbackAccount
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer import Customer
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer_serial import CustomerSerial


@pytest.mark.asyncio
async def test_create(storage, customer, customer_serial, make_cashback_account):
    cashback_account = make_cashback_account()

    created = await storage.cashback_account.create(cashback_account)

    cashback_account.account_id = customer_serial.account_id
    cashback_account.created = created.created
    cashback_account.updated = created.updated
    assert_that(
        created,
        equal_to(cashback_account),
    )


@pytest.mark.asyncio
async def test_get(storage, customer, make_cashback_account):
    cashback_account = make_cashback_account()

    created = await storage.cashback_account.create(cashback_account)

    got = await storage.cashback_account.get(created.uid, created.account_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(CashbackAccount.DoesNotExist):
        await storage.cashback_account.get(1, 2)


@pytest.mark.asyncio
async def test_save(storage, customer):
    cashback_account = CashbackAccount(
        uid=customer.uid,
        currency='XTS',
        trust_account_id='ididid',
    )
    created = await storage.cashback_account.create(cashback_account)
    created.currency = 'USD'
    created.trust_account_id = '222222'

    saved = await storage.cashback_account.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_cashback_account_uid_currency_pair_is_unique(storage, make_cashback_account):
    await storage.cashback_account.create(make_cashback_account(currency='XTS', trust_account_id='123'))

    with pytest.raises(CashbackAccount.DuplicateCurrency):
        await storage.cashback_account.create(make_cashback_account(currency='XTS', trust_account_id='456'))


@pytest.mark.asyncio
async def test_get_by_uid_and_currency(storage, make_cashback_account, customer):
    await storage.cashback_account.create(make_cashback_account(currency='XTS', trust_account_id='123'))
    await storage.cashback_account.create(make_cashback_account(currency='RUB', trust_account_id='456'))
    await storage.cashback_account.create(make_cashback_account(currency='BYN', trust_account_id='789'))

    assert_that(
        await storage.cashback_account.get_by_uid_and_currency(uid=customer.uid, currency='XTS'),
        has_property('trust_account_id', '123'),
    )


@pytest.fixture
async def customer(storage):
    return await storage.customer.create(Customer(uid=1400))


@pytest.fixture(autouse=True)
async def customer_serial(storage, customer):
    return await storage.customer_serial.create(CustomerSerial(uid=customer.uid, account_id=10))


@pytest.fixture
def make_cashback_account(storage, customer):
    def _make_cashback_account(**kwargs):
        cashback_account = CashbackAccount(
            uid=customer.uid,
            currency='XTS',
            trust_account_id='111222',
        )
        for key in kwargs:
            setattr(cashback_account, key, kwargs[key])
        return cashback_account
    return _make_cashback_account
