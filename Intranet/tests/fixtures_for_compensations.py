import pytest
import datetime

from decimal import Decimal
from review.compensations import logic, models

from .compensations import factories


@pytest.fixture()
def regular_payments_2022_2023(db):
    regular_payments_list = []
    for year in (2022, 2023):
        for month in range(1, 13):
            regular_payments_list.append(
                factories.RegularPaymentDateFactory(
                    date=datetime.date(year, month, 5),
                    type=models.choices.REGULAR_PAYMENT_TYPES.salary,
                )
            )
            regular_payments_list.append(
                factories.RegularPaymentDateFactory(
                    date=datetime.date(year, month, 20),
                    type=models.choices.REGULAR_PAYMENT_TYPES.advance,
                )
            )
            regular_payments_list.append(
                factories.RegularPaymentDateFactory(
                    date=datetime.date(year, month, 18),
                    type=models.choices.REGULAR_PAYMENT_TYPES.preadvance,
                )
            )
    return regular_payments_list


@pytest.fixture()
def countries(db):
    return {
        'ua': factories.CountryFactory(code='ua', name_ru='Украина', name_en='Ukraine'),
        'ru': factories.CountryFactory(code='ru', name_ru='Россия', name_en='Russia'),
        'cz': factories.CountryFactory(code='cz', name_ru='Чехия', name_en='Czech Republic'),
    }


@pytest.fixture()
def payment_plans(db):
    return {
        'linear': factories.PaymentPlanFactory(slug='retention_bonus', periods=[3, 3, 3, 3], scheme=[25, 25, 25, 25]),
        'descending': factories.PaymentPlanFactory(slug='welcome_bonus', periods=[6, 3, 3], scheme=[50, 25, 25]),
        'one_time': factories.PaymentPlanFactory(slug='one_time', periods=[0], scheme=[100]),
    }


@pytest.fixture()
def payment_types(db, payment_plans):
    return {
        'welcome_bonus': factories.PaymentTypeFactory(name_ru='Вэлкам бонус', name_en='Welcome bonus', plan=payment_plans['descending']),
        'retention_bonus': factories.PaymentTypeFactory(name_ru='Ретеншн бонус', name_en='Retention bonus', plan=payment_plans['linear']),
        'one_time': factories.PaymentTypeFactory(name_ru='Одноразовая выплата', name_en='One time payment', plan=payment_plans['one_time']),
    }


@pytest.fixture()
def elements(db, payment_types, countries):
    salary = [models.choices.REGULAR_PAYMENT_TYPES.salary]
    preadvance = [models.choices.REGULAR_PAYMENT_TYPES.preadvance]
    salary_advance = [models.choices.REGULAR_PAYMENT_TYPES.salary, models.choices.REGULAR_PAYMENT_TYPES.advance]
    salary_advance_preadvance = [
        models.choices.REGULAR_PAYMENT_TYPES.salary,
        models.choices.REGULAR_PAYMENT_TYPES.advance,
        models.choices.REGULAR_PAYMENT_TYPES.preadvance,
    ]

    elements = [
        factories.ElementFactory(type=payment_types['welcome_bonus'], country=countries['ru'], regular_payments=salary_advance),
        factories.ElementFactory(type=payment_types['welcome_bonus'], country=countries['ua'], regular_payments=salary),
        factories.ElementFactory(type=payment_types['welcome_bonus'], country=countries['cz'], regular_payments=salary),

        factories.ElementFactory(type=payment_types['retention_bonus'], country=countries['ru'], regular_payments=salary_advance_preadvance),
        factories.ElementFactory(type=payment_types['retention_bonus'], country=countries['ua'], regular_payments=salary_advance_preadvance),
        factories.ElementFactory(type=payment_types['retention_bonus'], country=countries['cz'], regular_payments=salary_advance_preadvance),

        factories.ElementFactory(type=payment_types['one_time'], country=countries['ru'], regular_payments=preadvance),
        factories.ElementFactory(type=payment_types['one_time'], country=countries['ua'], regular_payments=preadvance),
        factories.ElementFactory(type=payment_types['one_time'], country=countries['cz'], regular_payments=salary_advance_preadvance),
    ]
    return {
        (element.type.plan.slug, element.country.code): element
        for element in elements
    }


@pytest.fixture()
def payment_schedules(db, payment_types):
    schedules_file = factories.PaymentSchedulesFileFactory()
    return [
        factories.PersonPaymentScheduleFactory(
            source=schedules_file,
            person_login='cheburashka',
            payment_type=payment_types['welcome_bonus'],
            payments_start_date=datetime.date(2022, 1, 15),
            bonus_absolute=Decimal('12500.00'),
        ),
        factories.PersonPaymentScheduleFactory(
            source=schedules_file,
            person_login='gena',
            payment_type=payment_types['retention_bonus'],
            payments_start_date=datetime.date(2022, 4, 25),
            bonus_absolute=Decimal('86900.00'),
        ),
        factories.PersonPaymentScheduleFactory(
            source=schedules_file,
            person_login='shapoklyak',
            payment_type=payment_types['one_time'],
            payments_start_date=datetime.date(2022, 5, 26),
            bonus_absolute=Decimal('12345.67'),
        ),
    ]


@pytest.fixture()
def person_payments(db, regular_payments_2022_2023, payment_schedules, payment_plans, countries, elements):
    for ps in payment_schedules:
        logic.generate_person_payments_from_schedule(ps)

    payments_qs = models.PersonPayment.objects.order_by('raw_date')
    logic.specify_payment_dates(payments_qs)
    payments = list(payments_qs)

    assert len(payments) == 8

    return payments
