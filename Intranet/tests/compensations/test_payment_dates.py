import pytest
import datetime

from decimal import Decimal
from mock import mock

from review.compensations import models, logic, exceptions

from . import factories


@pytest.fixture(scope='module', autouse=True)
def mock_s3_storage():
    with mock.patch('review.lib.s3.S3Storage.save', return_value='mock_filename.xlsx'):
        yield


PERIODS = [(3, 3, 3, 3), (6, 3, 3), (0, )]


PAYMENT_PLAN_CASES = [
    (Decimal('100500'), PERIODS[0], [25, 25, 25, 25]),
    (Decimal('100500'), PERIODS[1], [50, 25, 25]),
    (Decimal('100500'), PERIODS[2], [100]),

    (Decimal('12345.67'), PERIODS[0], [25, 25, 25, 25]),
    (Decimal('12345.67'), PERIODS[1], [50, 25, 25]),
    (Decimal('12345.67'), PERIODS[2], [100]),
]


PAYMENT_DATES = [
    (
        datetime.date(2022, 2, 3),
        {
            PERIODS[0]: [(2022, 5, 3), (2022, 8, 3), (2022, 11, 3), (2023, 2, 3)],
            PERIODS[1]: [(2022, 8, 3), (2022, 11, 3), (2023, 2, 3)],
            PERIODS[2]: [(2022, 2, 3)],
        },
    ),
    (
        datetime.date(2022, 1, 30),
        {
            PERIODS[0]: [(2022, 4, 30), (2022, 7, 30), (2022, 10, 30), (2023, 1, 30)],
            PERIODS[1]: [(2022, 7, 30), (2022, 10, 30), (2023, 1, 30)],
            PERIODS[2]: [(2022, 1, 30)],
        },
    ),
    (
        datetime.date(2022, 8, 31),
        {
            PERIODS[0]: [(2022, 11, 30), (2023, 2, 28), (2023, 5, 31), (2023, 8, 31)],
            PERIODS[1]: [(2023, 2, 28), (2023, 5, 31), (2023, 8, 31)],
            PERIODS[2]: [(2022, 8, 31)],
        },
    ),
]


@pytest.mark.parametrize('summ, periods, scheme', PAYMENT_PLAN_CASES)
@pytest.mark.parametrize('start_date, expected_dates', PAYMENT_DATES)
def test_generate_payment_details(summ, periods, scheme, start_date, expected_dates):
    schedule = factories.PersonPaymentScheduleFactory(
        person_login='tester',
        bonus_absolute=summ,
        payment_type=factories.PaymentTypeFactory(
            plan=factories.PaymentPlanFactory(periods=periods, scheme=scheme),
        ),
        payments_start_date=start_date,
    )

    sums_and_dates = list(logic.generate_payment_details(schedule))
    assert len(sums_and_dates) == len(periods) == len(scheme)

    sums = [t[0] for t in sums_and_dates]
    dates = [t[1] for t in sums_and_dates]

    assert sum(sums) == Decimal(summ)
    assert dates == [datetime.date(*d) for d in expected_dates[periods]]


def test_specify_actual_payment_dates(regular_payments_2022_2023):
    payment_schedule = factories.PersonPaymentScheduleFactory()
    pp1 = factories.PersonPaymentFactory(schedule=payment_schedule, raw_date=datetime.date(2022, 6, 18))
    pp2 = factories.PersonPaymentFactory(schedule=payment_schedule, raw_date=datetime.date(2022, 10, 6))
    pp3 = factories.PersonPaymentFactory(schedule=payment_schedule, raw_date=datetime.date(2022, 12, 27))

    factories.ElementFactory(
        country=factories.CountryFactory(code='ru'),
        type=payment_schedule.payment_type,
    )

    pp_qs = models.PersonPayment.objects.filter(date=None)
    logic.specify_payment_dates(pp_qs)

    pp1.refresh_from_db()
    assert pp1.date == datetime.date(2022, 6, 5)

    pp2.refresh_from_db()
    assert pp2.date == datetime.date(2022, 10, 5)

    pp3.refresh_from_db()
    assert pp3.date == datetime.date(2022, 12, 20)


def test_specify_payment_dates_fails_without_regular_dates(regular_payments_2022_2023):

    # PersonPayment до начала регулярных выплат.
    pp = factories.PersonPaymentFactory(raw_date=datetime.date(2022, 1, 3))
    factories.ElementFactory(
        country=factories.CountryFactory(code='ru'),
        type=pp.schedule.payment_type,
    )

    with pytest.raises(exceptions.RegularPaymentDateNotFoundError) as ex:
        logic.specify_payment_dates(models.PersonPayment.objects.all())

    pp.refresh_from_db()
    assert pp.date is None
