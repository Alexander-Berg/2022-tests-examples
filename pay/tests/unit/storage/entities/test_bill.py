from datetime import date, datetime, timedelta, timezone
from uuid import UUID

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.interactions.kazna.entities import DepartmentType
from pay.bill_payments.bill_payments.storage.entities.bill import Bill
from pay.bill_payments.bill_payments.storage.entities.enums import BillStatus

FUTURE = utcnow() + timedelta(days=7)
PAST = utcnow() - timedelta(days=7)


@pytest.fixture
def bill():
    return Bill(
        bill_id=UUID('cb3b1b72-0f36-4c70-a573-0d9144dcb461'),
        uid=228,
        supplier_bill_id='supplier_bill_id',
        document_id=UUID('1c2490f9-24a1-4f7e-a2a7-48968c05de08'),
        status=BillStatus.NEW,
        amount=55,
        amount_to_pay=25,
        bill_date=datetime(year=2021, month=10, day=10, tzinfo=timezone.utc),
        purpose='purpose',
        kbk='kbk',
        oktmo='oktmo',
        inn='inn',
        kpp='kpp',
        bik='bik',
        account_number='account_number',
        payee_name='payee_name',
        payer_name='payer_name',
        div_id=33,
        treasure_branch='treasure_branch',
        dep_type=DepartmentType.GIBDD,
        legal_act='legal_act',
        offense_name='offense_name',
        offense_place='offense_place',
        offense_date=datetime(year=2020, month=10, day=10, tzinfo=timezone.utc),
        discount_size='50',
        discount_date=FUTURE.date(),
        paid_amount=30,
        paid_date=datetime(year=2006, month=10, day=10, tzinfo=timezone.utc),
    )


@pytest.mark.parametrize(
    'amount_to_pay, discount_size, expected_discounted_amount',
    (
        pytest.param(100, None, 100, id='empty-discount'),
        pytest.param(100, '30', 70, id='has-discount'),
        pytest.param(101, '50', 51, id='check-rounding'),
    ),
)
def test_discounted_amount_by_money(bill, amount_to_pay, discount_size, expected_discounted_amount):
    bill.discount_date = FUTURE.date()

    bill.amount_to_pay = amount_to_pay
    bill.discount_size = discount_size

    assert_that(bill.discounted_amount, equal_to(expected_discounted_amount))


@pytest.mark.parametrize(
    'discount_date, expected_discounted_amount',
    (
        pytest.param(FUTURE.date(), 50, id='future'),
        pytest.param(PAST.date(), 100, id='past'),
        pytest.param(None, 100, id='none'),
    ),
)
def test_discounted_amount_by_discount_date(bill, discount_date, expected_discounted_amount):
    bill.amount_to_pay = 100
    bill.discount_size = '50'

    bill.discount_date = discount_date

    assert_that(bill.discounted_amount, equal_to(expected_discounted_amount))


def test_payment_deadline(bill):
    bill.bill_date = date(2020, 12, 30)

    assert_that(bill.payment_deadline, equal_to(date(2021, 3, 10)))


@pytest.mark.parametrize(
    'amount, expected_amount',
    (
        pytest.param(100000, 2300 + 1000, id='common'),
        pytest.param(104400, 2401 + 1000, id='round-down'),  # 1044*0.023 = 24.012 => 24.01
        pytest.param(104500, 2404 + 1000, id='round-up'),  # 1045*0.023 = 24.035 => 24.04
        pytest.param(4000, 2000, id='min'),  # 40*0.023 = 0.92 ; max(20, 0.92) => 20
    ),
)
@pytest.mark.asyncio
async def test_fee_amount(bill, amount, expected_amount):
    bill.discount_date = PAST.date()
    bill.amount_to_pay = amount

    assert_that(bill.fee_amount, equal_to(expected_amount))


@pytest.mark.asyncio
async def test_fee_amount_calculates_against_discount_size(bill):
    bill.discount_date = FUTURE.date()
    bill.discount_size = '80'
    bill.amount_to_pay = 500000

    assert_that(bill.fee_amount, equal_to(2300 + 1000))  # 500.00 * 0.2 * 0.023 = 23.00
