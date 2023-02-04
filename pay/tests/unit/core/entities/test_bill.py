from datetime import date, datetime
from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.core.entities.bill import Bill, BillStatus, charge_to_bill
from pay.bill_payments.bill_payments.interactions.kazna.entities import (
    AdditionalData,
    Charge,
    ChargeData,
    DepartmentType,
    PaymentData,
)


@pytest.fixture
def document_id():
    return uuid4()


@pytest.fixture
def charge(mocker):
    return Charge(
        document=mocker.Mock(),
        supplier_bill_id='1',
        department=DepartmentType.GIBDD,
        charge_data=ChargeData(
            bill_date=datetime(2021, 12, 20, 15, 0, 0, 0),
            amount_to_pay=15000,
            amount=25000,
            purpose='purpose',
            kbk='kbk',
            oktmo='oktmo',
            inn='inn',
            kpp='kpp',
            bik='bik',
            account_number='account_number',
            payee_name='payee_name',
            payer_name='payer_name',
            div_id=123,
            treasure_branch='treasure_branch',
            additional_data=AdditionalData(
                discount_date=date(2021, 12, 22),
                discount_size='discount_size',
                legal_act='legal_act',
                offense_name='offense_name',
                offense_place='offense_place',
                offense_date=datetime(2021, 12, 3, 4, 5, 6),
            ),
            payment_data=PaymentData(
                sum=100,
                date=datetime(2021, 12, 1, 12, 30),
            ),
        ),
    )


@pytest.fixture
def bill(document_id):
    return Bill(
        uid=13,
        supplier_bill_id='1',
        document_id=document_id,
        status=BillStatus.NEW,
        amount_to_pay=15000,
        amount=25000,
        dep_type=DepartmentType.GIBDD,
        bill_date=datetime(2021, 12, 20, 15, 0, 0, 0),
        purpose='purpose',
        kbk='kbk',
        oktmo='oktmo',
        inn='inn',
        kpp='kpp',
        bik='bik',
        account_number='account_number',
        payee_name='payee_name',
        payer_name='payer_name',
        div_id=123,
        treasure_branch='treasure_branch',
        discount_date=date(2021, 12, 22),
        discount_size='discount_size',
        legal_act='legal_act',
        offense_name='offense_name',
        offense_place='offense_place',
        offense_date=datetime(2021, 12, 3, 4, 5, 6),
        paid_amount=100,
        paid_date=datetime(2021, 12, 1, 12, 30),
    )


def test_charge_to_bill_conversion(charge, bill, document_id):
    converted = charge_to_bill(13, charge, document_id)
    assert_that(converted, equal_to(bill))


def test_no_additional_data(charge, bill, document_id):
    charge.charge_data.additional_data = None
    charge.charge_data.payment_data = None

    for field in (
        'discount_date',
        'discount_size',
        'legal_act',
        'offense_name',
        'offense_place',
        'offense_date',
        'paid_amount',
        'paid_date',
    ):
        setattr(bill, field, None)

    converted = charge_to_bill(13, charge, document_id)
    assert_that(converted, equal_to(bill))
