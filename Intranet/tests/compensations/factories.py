import factory
from factory import fuzzy

from datetime import date, timedelta
from decimal import Decimal

from review.staff.models import Person
from review.compensations import models


class PersonFactory(factory.django.DjangoModelFactory):

    uid = factory.Sequence(lambda n: 11300000000 + n)
    login = factory.Sequence(lambda n: f'login{n}')
    department = None
    first_name_ru = factory.Sequence(lambda n: f'Первый{n}')
    last_name_ru = factory.Sequence(lambda n: f'Последний{n}')
    first_name_en = factory.Sequence(lambda n: f'First{n}')
    last_name_en = factory.Sequence(lambda n: f'Last{n}')

    class Meta:
        model = Person


class PaymentPlanFactory(factory.django.DjangoModelFactory):

    slug = factory.Sequence(lambda n: f'plan_slug_{n}')
    periods = [25, 25, 25, 25]
    scheme = [3, 3, 3, 3]

    class Meta:
        model = models.PaymentPlan


class PaymentTypeFactory(factory.django.DjangoModelFactory):

    name_ru = factory.Sequence(lambda n: f'Тип оплаты {n}')
    name_en = factory.Sequence(lambda n: f'Payment type {n}')
    plan = factory.SubFactory(PaymentPlanFactory)

    class Meta:
        model = models.PaymentType


class PaymentSchedulesFileFactory(factory.django.DjangoModelFactory):

    name = factory.Sequence(lambda n: f'uploadedfile_{n}')
    file = factory.django.FileField(filename='file.xlsx', data=b'filecontentabracadabra')
    payment_type = factory.SubFactory(PaymentTypeFactory)
    payments_start_date = fuzzy.FuzzyDate(
        start_date=date.today() + timedelta(days=20),
        end_date=date.today() + timedelta(days=200),
    )
    status = models.choices.SCHEDULES_FILE_STATUS.pending
    processed_at = None
    errors = ''
    uploader = factory.SubFactory(PersonFactory)

    class Meta:
        model = models.PaymentSchedulesFile


class PersonPaymentScheduleFactory(factory.django.DjangoModelFactory):

    source = factory.SubFactory(PaymentSchedulesFileFactory)
    person_login = factory.Sequence(lambda n: f'person-login{n}')
    full_name = factory.Sequence(lambda n: f'Full Name {n}')
    payment_type = factory.SubFactory(PaymentTypeFactory)
    assignment = factory.Sequence(lambda n: f'12345-00{n}')
    bonus = factory.Sequence(lambda n: n**2 % 100)
    bonus_absolute = factory.Sequence(lambda n: Decimal(n * 100))
    salary = factory.Sequence(lambda n: Decimal((n + 100)**2))
    currency = factory.Iterator(models.choices.CURRENCIES._db_values)
    payments_start_date = factory.SelfAttribute('.source.payments_start_date')

    class Meta:
        model = models.PersonPaymentSchedule


class PersonPaymentFactory(factory.django.DjangoModelFactory):

    schedule = factory.SubFactory(PersonPaymentScheduleFactory)
    amount = factory.SelfAttribute('.schedule.bonus')
    currency = factory.SelfAttribute('.schedule.currency')
    raw_date = fuzzy.FuzzyDate(
        start_date=date.today() + timedelta(days=20),
        end_date=date.today() + timedelta(days=200),
    )
    date = None
    status = models.choices.PAYMENT_STATUSES.scheduled

    class Meta:
        model = models.PersonPayment


class CountryFactory(factory.django.DjangoModelFactory):

    code = 'ru'
    name_ru = 'РФ'
    name_en = 'Russia'

    class Meta:
        model = models.Country
        django_get_or_create = ('code', )


class RegularPaymentDateFactory(factory.django.DjangoModelFactory):

    date = fuzzy.FuzzyDate(
        start_date=date.today() + timedelta(days=20),
        end_date=date.today() + timedelta(days=200),
    )
    type = models.choices.REGULAR_PAYMENT_TYPES.salary
    country = factory.SubFactory(CountryFactory)

    class Meta:
        model = models.RegularPaymentDate


class ElementFactory(factory.django.DjangoModelFactory):

    name_ru = factory.Sequence(lambda n: f'Элемент {n}')
    name_en = factory.Sequence(lambda n: f'Element {n}')
    type = factory.SubFactory(PaymentTypeFactory)
    country = factory.SubFactory(CountryFactory)

    regular_payments = [
        models.choices.REGULAR_PAYMENT_TYPES.salary,
        models.choices.REGULAR_PAYMENT_TYPES.advance,
    ]

    class Meta:
        model = models.Element


class ExportPaymentsFactory(factory.django.DjangoModelFactory):

    class Meta:
        model = models.ExportPayments
