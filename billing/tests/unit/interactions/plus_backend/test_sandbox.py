from decimal import Decimal
from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.interactions.plus_backend.entities import (
    OrderStatus, PlusOrder, YandexPayPlusMerchant
)
from billing.yandex_pay.yandex_pay.interactions.plus_backend.sandbox import SandboxYandexPayPlusClient


@pytest.fixture
async def sandbox_plus_client(create_client) -> SandboxYandexPayPlusClient:
    client = create_client(SandboxYandexPayPlusClient)
    yield client
    await client.close()


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def user(uid):
    return User(uid)


@pytest.fixture
def order_id(randn):
    return randn()


@pytest.fixture
def message_id(rands):
    return rands()


@pytest.fixture
def merchant():
    return YandexPayPlusMerchant(
        id=uuid4(),
        name='name',
        url='url',
    )


@pytest.fixture
def psp_id():
    return uuid4()


@pytest.mark.asyncio
async def test_create_order(
    user, message_id, merchant, psp_id, sandbox_plus_client
):
    expected = PlusOrder(
        order_id=-1,
        uid=user.uid,
        message_id=message_id,
        currency='XTS',
        amount=Decimal('1.00'),
        cashback=Decimal('0.00'),
        cashback_category=Decimal('0.00'),
        status=OrderStatus.NEW,
        psp_id=psp_id,
        merchant_id=merchant.id,
        payment_method_type=PaymentMethodType.CARD,
    )
    response = await sandbox_plus_client.create_order(
        uid=user.uid,
        message_id=message_id,
        merchant=merchant,
        psp_id=psp_id,
        currency='XTS',
        amount=Decimal('1.00'),
        trust_card_id='card-x123abc',
        last4='1234',
        country_code='RUS',
        order_basket={},
        payment_method_type=PaymentMethodType.CARD,
    )

    assert_that(response, equal_to(expected))


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'amount,expected_response',
    [
        (Decimal('50.00'), {'category': Decimal('0.05'), 'amount': Decimal('2.0')}),
        (Decimal('1.00'), {'category': Decimal('0.0'), 'amount': Decimal('0.0')}),
        (Decimal('0.0'), {'category': Decimal('0.0'), 'amount': Decimal('0.0')}),
        (None, {'category': Decimal('0.05'), 'amount': Decimal('100.0')}),
    ]
)
async def test_get_user_cashback_amount(
    merchant, psp_id, sandbox_plus_client, amount, expected_response
):
    response = await sandbox_plus_client.get_user_cashback_amount(
        user_ticket=None,
        merchant=merchant,
        psp_id=psp_id,
        currency='XTS',
        amount=amount,
    )

    assert_that(response, equal_to(expected_response))
