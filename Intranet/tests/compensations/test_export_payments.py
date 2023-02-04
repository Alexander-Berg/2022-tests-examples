import pytest
import datetime

from mock import mock

from review.compensations import models, logic, exceptions

from . import factories


@pytest.fixture(scope='module', autouse=True)
def mock_s3_storage():
    with mock.patch('review.lib.s3.S3Storage.save', return_value='mock_filename.xlsx'):
        yield


def test_process_xls_export_skips_done_payments(person_payments, payment_types):
    pt = payment_types['retention_bonus']
    export_instance = factories.ExportPaymentsFactory(
        date=person_payments[-1].raw_date + datetime.timedelta(days=10),
        payment_type=pt,
        author=factories.PersonFactory(),
        status=models.choices.EXPORT_FILE_STATUSES.draft,
    )

    payments_qs = models.PersonPayment.objects.filter(schedule__payment_type=pt)

    assert payments_qs.count() == 4

    done_payment = payments_qs.first()
    done_payment.status = models.choices.PAYMENT_STATUSES.done
    done_payment.save()

    logic.process_xls_export(export_instance)
    export_instance.refresh_from_db()

    payments = models.PersonPayment.objects.filter(export=export_instance)
    assert payments.count() == 3
    assert done_payment not in payments


def test_export_fails_on_outdated_payments(person_payments, payment_types):
    pt = payment_types['retention_bonus']
    export_instance = factories.ExportPaymentsFactory(
        date=person_payments[-1].raw_date + datetime.timedelta(days=10),
        payment_type=pt,
        author=factories.PersonFactory(),
        status=models.choices.EXPORT_FILE_STATUSES.draft,
    )

    second_payment = list(
        models.PersonPayment.objects
        .filter(schedule__payment_type=pt)
        .order_by('raw_date')
    )[1]
    second_payment.date = None
    second_payment.save()

    with pytest.raises(exceptions.OutdatedPaymentsError):
        logic.process_xls_export(export_instance)


def test_mark_payments_as_done(person_payments, payment_types):
    pt = payment_types['retention_bonus']
    export_instance = factories.ExportPaymentsFactory(
        date=person_payments[-1].raw_date + datetime.timedelta(days=10),
        payment_type=pt,
        author=factories.PersonFactory(),
        status=models.choices.EXPORT_FILE_STATUSES.ready,
    )
    export_instance.person_payments = models.PersonPayment.objects.filter(
        schedule__payment_type=pt,
        status=models.choices.PAYMENT_STATUSES.scheduled,
    )

    logic.mark_payments_as_done(export_instance)

    assert (
        export_instance.person_payments
        .filter(status=models.choices.PAYMENT_STATUSES.done)
        .count()
    ) == 4


EXPORT_SCENARIOS = [
    ((2022, 5, 15), 'welcome_bonus', 0),
    ((2022, 5, 15), 'retention_bonus', 0),
    ((2022, 5, 15), 'one_time', 0),

    ((2022, 10, 15), 'welcome_bonus', 2),
    ((2022, 10, 15), 'retention_bonus', 1),
    ((2022, 10, 15), 'one_time', 1),

    ((2023, 4, 22), 'welcome_bonus', 3),
    ((2023, 4, 22), 'retention_bonus', 4),
    ((2023, 4, 22), 'one_time', 1),
]


@pytest.mark.parametrize('export_date, export_plan, result_count', EXPORT_SCENARIOS)
def test_get_payments_for_export(export_date, export_plan, result_count, person_payments, payment_types):
    export_instance = models.ExportPayments(
        date=datetime.date(*export_date),
        payment_type=payment_types[export_plan],
        author=factories.PersonFactory(),
    )
    payments_for_export = logic.get_payments_for_export(export_instance)

    assert payments_for_export.count() == result_count
    for pp in payments_for_export:
        assert pp.schedule.payment_type == payment_types[export_plan]


PAYMENT_LOGIN_MAPPING = [
    ('welcome_bonus', 'cheburashka'),
    ('retention_bonus', 'gena'),
    ('one_time', 'shapoklyak'),
]


@pytest.mark.parametrize('plan_name, login', PAYMENT_LOGIN_MAPPING)
def test_process_xls_export(plan_name, login, person_payments, payment_types):
    # arrange
    export_instance = factories.ExportPaymentsFactory(
        date=person_payments[-1].raw_date + datetime.timedelta(days=10),
        payment_type=payment_types[plan_name],
        author=factories.PersonFactory(),
        status=models.choices.EXPORT_FILE_STATUSES.draft,
    )
    logic.process_xls_export(export_instance)

    # act
    export_instance.refresh_from_db()

    # assert
    assert export_instance.file is not None
    assert export_instance.status == models.choices.EXPORT_FILE_STATUSES.ready

    assert export_instance.person_payments.count() == len(payment_types[plan_name].plan.periods)
    payments = models.PersonPayment.objects.filter(schedule__person_login=login)

    payment_ids = set(
        models.PersonPayment.objects
        .filter(
            status=models.choices.PAYMENT_STATUSES.scheduled,
            schedule__person_login=login
        )
        .values_list('id', flat=True)
    )
    assert set(export_instance.person_payments.values_list('id', flat=True)) == payment_ids
