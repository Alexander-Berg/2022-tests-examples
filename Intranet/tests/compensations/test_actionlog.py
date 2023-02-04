import pytest

from mock import mock
from datetime import datetime
from django.urls import reverse

from tests import helpers
from tests.compensations import factories

from review.shortcuts import const
from review.compensations.models import choices, LogRecord, Snapshot, PersonPaymentSchedule
from review.compensations.lib import encryption


EXCEL_ROWS_MOCK = [
    {
        'assignment': '228908-54804',
        'bonus': None,
        'bonus_absolute': 100500,
        'currency': 'RUB',
        'date_begin': datetime(2022, 3, 1, 0, 0),
        'date_end': datetime(2022, 3, 31, 0, 0),
        'financial_reporting_center': 'DSMS',
        'full_name': 'Коренблюм Герман Владимирович',
        'gl_service': None,
        'legal_entity': 'ООО Яндекс',
        'message': None,
        'payments_start_date': datetime(2022, 3, 1, 0, 0),
        'person_login': 'KORENBLYUM',
        'product': None,
        'salary': None,
        'status': 'Проверена',
    },
    {
        'assignment': '33835-11342',
        'bonus': 140,
        'bonus_absolute': 55230.34,
        'currency': 'RUB',
        'date_begin': datetime(2022, 3, 1, 0, 0),
        'date_end': datetime(2022, 3, 31, 0, 0),
        'financial_reporting_center': 'MSMS35',
        'full_name': 'Сидоров Никита Владимирович',
        'gl_service': None,
        'legal_entity': 'ООО Яндекс.Технологии',
        'message': None,
        'payments_start_date': datetime(2022, 4, 1, 0, 0),
        'person_login': 'NICKSHEVR',
        'product': None,
        'salary': 99900,
        'status': 'Проверена'
    },
]


@pytest.fixture(scope='module', autouse=True)
def mock_s3_storage():
    with mock.patch('review.lib.s3.S3Storage.save', return_value='mock_filename.xlsx'):
        yield


@pytest.fixture
def superuser(test_person, global_role_builder, settings):
    global_role_builder(person=test_person, type=const.ROLE.GLOBAL.SUPPORT)
    settings.YAUTH_TEST_USER['login'] = settings.DEFAULT_TEST_USER
    return test_person


def test_action_file_parsing(superuser, client, settings):
    file_instance = factories.PaymentSchedulesFileFactory()
    assert file_instance.status == choices.SCHEDULES_FILE_STATUS.pending

    url = reverse('admin:compensations_paymentschedulesfile_changelist')
    request_data = {
        'action': 'parse_files_action',
        '_selected_action': [file_instance.id],
    }

    with mock.patch('review.compensations.logic.parse_excel', return_value=EXCEL_ROWS_MOCK):
        response = client.post(url, request_data)

    file_instance.refresh_from_db()
    assert file_instance.status == choices.SCHEDULES_FILE_STATUS.successed

    assert PersonPaymentSchedule.objects.count() == 2
    schedule = PersonPaymentSchedule.objects.first()

    assert LogRecord.objects.count() == 1
    log = LogRecord.objects.get()
    assert log.action_name == 'paymentschedulesfile_parse'
    assert log.user == superuser

    assert Snapshot.objects.filter(obj_str='paymentschedulesfile').count() == 1
    snapshot = Snapshot.objects.filter(obj_str='paymentschedulesfile').get()
    assert snapshot.reason == 'change'
    assert snapshot.log_record_id == log.id
    assert snapshot.obj_id == file_instance.id

    assert Snapshot.objects.filter(obj_str='personpaymentschedule').count() == 2
    snapshot = Snapshot.objects.filter(obj_str='personpaymentschedule').first()
    assert snapshot.reason == 'addition'
    assert snapshot.log_record_id == log.id
    assert snapshot.obj_id == schedule.id

    regular_fields = (
        'id', 'assignment', 'currency', 'financial_reporting_center', 'full_name', 'gl_service',
        'legal_entity', 'message', 'payment_type_id', 'person_login', 'processed_at',
        'product', 'source_id', 'status',
    )
    datetime_fields = ('created', 'date_begin', 'date_end', 'modified', 'payments_start_date')
    encrypted_fields = ('bonus', 'bonus_absolute', 'salary')

    for field in regular_fields:
        assert snapshot.data[field] == getattr(schedule, field)

    for field in datetime_fields:
        assert snapshot.data[field] == getattr(schedule, field).isoformat()

    for field in encrypted_fields:
        assert encryption.decrypt(snapshot.data[field]) == str(getattr(schedule, field))
