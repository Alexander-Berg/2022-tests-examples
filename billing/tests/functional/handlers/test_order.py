import uuid
from datetime import datetime, timedelta
from decimal import Decimal

import pytest

from sendr_pytest.matchers import close_to_datetime, convert_then_match
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, greater_than, has_entry, has_properties, match_equality

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_budget import CashbackBudget
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import PaymentMethodType


@pytest.mark.asyncio
@pytest.mark.parametrize('payment_method_type', list(PaymentMethodType))
async def test_create_order(
    app, rands, yandex_pay_plus_settings, storage, trust_card_id, randn, payment_method_type
):
    yandex_pay_plus_settings.CASHBACK_USER_SHEET_SPENDING_LIMIT = {'XTS': '1000'}
    yandex_pay_plus_settings.CASHBACK_CARD_SHEET_SPENDING_LIMIT = {'XTS': '3000'}
    yandex_pay_plus_settings.CASHBACK_CATEGORIES = ['0.01', '0.1']
    now = utcnow()
    uid = randn()
    merchant = {
        'id': str(uuid.uuid4()),
        'name': 'name',
        'url': 'https://url.test',
    }
    order_data = {
        'uid': uid,
        'message_id': rands(),
        'currency': 'XTS',
        'psp_id': str(uuid.uuid4()),
        'trust_card_id': trust_card_id,
        'card_id': str(uuid.uuid4()),
    }
    extra_kwargs = {
        'amount': '100.0',
        'merchant': merchant,
        'cashback_category_id': '0.01',
        'payment_method_type': payment_method_type.value,
    }
    payload = order_data | extra_kwargs

    r = await app.post('/api/v1/orders', json=payload)

    data = await r.json()

    assert_that(r.status, equal_to(200), data)
    assert_that(
        data,
        has_entry(
            'data',
            equal_to({
                'order_id': match_equality(greater_than(0)),
                'merchant_id': merchant['id'],
                'cashback': match_equality(convert_then_match(Decimal, Decimal('1'))),
                'cashback_category': match_equality(convert_then_match(Decimal, Decimal('0.01'))),
                'status': 'new',
                'created': match_equality(
                    convert_then_match(
                        datetime.fromisoformat,
                        close_to_datetime(now, timedelta(seconds=30)),
                    )
                ),
                'updated': match_equality(
                    convert_then_match(
                        datetime.fromisoformat,
                        close_to_datetime(now, timedelta(seconds=30)),
                    )
                ),
                **order_data,
                'amount': match_equality(convert_then_match(Decimal, Decimal('100.0'))),
                'payment_method_type': payment_method_type.value,
            })
        )
    )

    order = await storage.order.get_by_message_id(order_data['message_id'])
    assert_that(
        order,
        has_properties(
            uid=uid,
            cashback=greater_than(0),
        )
    )

    user_sheet = await storage.cashback_user_sheet.get_for_datetime(
        order.uid,
        order.currency,
        utcnow(),
    )
    assert_that(user_sheet.spent, equal_to(0))

    card_sheet = await storage.cashback_card_sheet.get_for_datetime(
        order.trust_card_id,
        order.currency,
        utcnow(),
    )
    assert_that(card_sheet.spent, equal_to(0))


@pytest.fixture
def tvm_user_id(randn):
    return randn() * 2 + 1


@pytest.fixture
async def trust_card_id(rands):
    return f'card-x{rands()}'


@pytest.fixture(autouse=True)
async def budget(storage):
    return await storage.cashback_budget.create(
        CashbackBudget(
            budget_id=uuid.uuid4(),
            currency='XTS',
            spent=Decimal('0'),
            spending_limit=Decimal('1000000'),
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=10),
        )
    )
