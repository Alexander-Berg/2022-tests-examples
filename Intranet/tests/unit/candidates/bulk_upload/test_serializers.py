import pytest
import uuid

from datetime import date

from waffle.testutils import override_switch

from intranet.femida.src.candidates.bulk_upload.choices import CANDIDATE_UPLOAD_MODES
from intranet.femida.src.candidates.bulk_upload.forms import CandidateBulkUploadBeameryForm
from intranet.femida.src.candidates.bulk_upload.serializers import BeameryCandidateSerializer
from intranet.femida.src.candidates.choices import CONTACT_TYPES, CANDIDATE_STATUSES

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@pytest.fixture
def beamery_raw_data():
    return {
        'id': 'fffff82d-7f98-498e-bbee-e441b127f291',
        'confidential': False,
        'status': {
            'id': 'in_progress',
        },
        'customFields': [
            {
                'Name': 'Gender',
                'value': 'not_specified',
            },
            {
                'Name': 'Birthdate',
                'value': '1992-10-17',
            },
            {
                'Name': 'Femida Status',
                'value': 'in_progress',
            },
            {
                'Name': 'Femida Id',
                'value': 1234567,
            },
            {
                'Name': 'Original Id',
                'value': '86272214',
            },
            {
                'Name': 'Yandex Owner',
                'value': 'no owner',
            },
            {
                'Name': 'Yandex Owner Id',
                'value': 'no owner',
            },
        ],
        'education': [{
            'degree': 'unknown',
            'endDate': '2014-05-31T00:00:00.000Z',
            'organisationName': 'Казанский институт',
            'program': 'Инженерный факультет',
            'startDate': '2009-05-31T00:00:00.000Z',
        }],
        'emails': [
            'lliushin@gmail.com',
            'lliushin2@gmail.com',
        ],
        'experience': [
            {
                'current': False,
                'endDate': '2015-09-29T20:00:00.000Z',
                'organisationName': 'VK',
                'startDate': '2014-10-30T20:00:00.000Z',
            },
            {
                'current': False,
                'endDate': '2015-09-30T00:00:00.000Z',
                'organisationName': 'VK',
                'role': 'empty',
                'startDate': '2014-10-31T00:00:00.000Z',
            },
        ],
        'firstName': 'Владимир',
        'middleNames': [
            'Иванович',
            'Сергеевич',
            '',
        ],
        'lastName': 'Заварзин',
        'fullName': 'Владимир Иванович Заварзин',
        'links': [
            'http://mabutorov.by',
        ],
        'location': {
            'address': 'Москва и, Российская Федерация',
            'city': 'Москва',
            'country': 'Российская Федерация',
        },
        'integrations': {
            'brassring': {
                'id': '1234567',
            },
        },
        'phoneNumbers': [
            '+7-926-765-75-56',
        ],
        'tags': [
            'Supernatural',
            'Rock star',
            'Jekyll and Hyde',
        ],
        'skills': [
            'Oрека',
            'SQL',
        ],
        'updatedTime': '2021-10-07T12:13:26.715Z',
    }


@pytest.fixture
def beamery_serialized_data():
    return {
        'original': '1234567',
        'beamery_id': 'fffff82d-7f98-498e-bbee-e441b127f291',
        'status': CANDIDATE_STATUSES.in_progress,
        'first_name': 'Владимир',
        'middle_name': 'Иванович Сергеевич',
        'last_name': 'Заварзин',
        'contacts': [
            {
                'type': CONTACT_TYPES.beamery,
                'account_id': (
                    'https://app.beamery.ru/#/crm/profile/'
                    'fffff82d-7f98-498e-bbee-e441b127f291'
                ),
            },
            {'type': CONTACT_TYPES.email, 'account_id': 'lliushin@gmail.com'},
            {'type': CONTACT_TYPES.email, 'account_id': 'lliushin2@gmail.com'},
            {'type': CONTACT_TYPES.phone, 'account_id': '+7-926-765-75-56'},
        ],
        'birthday': '1992-10-17',
        'city': 'Москва',
        'country': 'Российская Федерация',
        'educations': [{
            'degree': 'unknown',
            'end_date': '2014-05-31',
            'institution': 'Казанский институт',
            'faculty': 'Инженерный факультет',
        }],
        'jobs': [
            {
                'employer': 'VK',
                'start_date': '2014-10-30',
                'end_date': '2015-09-29',
            },
            {
                'employer': 'VK',
                'position': 'empty',
                'start_date': '2014-10-31',
                'end_date': '2015-09-30',
            },
        ],
        'tags': [
            'Supernatural',
            'Rock star',
            'Jekyll and Hyde',
        ],
    }


@pytest.fixture
def beamery_cleaned_data():
    return {
        'beamery_id': uuid.UUID('fffff82d-7f98-498e-bbee-e441b127f291'),
        'birthday': date(1992, 10, 17),
        'city': 'Москва',
        'contacts': [
            {
                'account_id': (
                    'https://app.beamery.ru/#/crm/profile/'
                    'fffff82d-7f98-498e-bbee-e441b127f291'
                ),
                'is_main': False,
                'type': 'beamery',
            },
            {
                'account_id': 'lliushin@gmail.com',
                'is_main': False,
                'type': 'email',
            },
            {
                'account_id': 'lliushin2@gmail.com',
                'is_main': False,
                'type': 'email',
            },
            {
                'account_id': '+7-926-765-75-56',
                'is_main': False,
                'type': 'phone',
            },
        ],
        'country': 'Российская Федерация',
        'educations': [{
            'degree': 'unknown',
            'end_date': date(2014, 5, 31),
            'faculty': 'Инженерный факультет',
            'institution': 'Казанский институт',
        }],
        'first_name': 'Владимир',
        'gender': '',
        'jobs': [
            {
                'employer': 'VK',
                'end_date': date(2015, 9, 29),
                'position': '',
                'salary_evaluation': '',
                'start_date': date(2014, 10, 30),
            },
            {
                'employer': 'VK',
                'end_date': date(2015, 9, 30),
                'position': 'empty',
                'salary_evaluation': '',
                'start_date': date(2014, 10, 31),
            },
        ],
        'last_name': 'Заварзин',
        'middle_name': 'Иванович Сергеевич',
        'status': CANDIDATE_STATUSES.in_progress,
        'tags': ['Supernatural', 'Rock star', 'Jekyll and Hyde'],
    }


@override_switch('enable_upload_jobs_from_beamery', active=True)
@override_switch('enable_upload_educations_from_beamery', active=True)
def test_beamery_candidate_serializer(beamery_raw_data, beamery_serialized_data,
                                      beamery_cleaned_data):
    beamery_cleaned_data['original'] = f.CandidateFactory(
        id=int(beamery_serialized_data['original']),
    )

    serializer = BeameryCandidateSerializer(beamery_raw_data)
    serialized_data = serializer.data
    form = CandidateBulkUploadBeameryForm(
        data=serialized_data,
        initial={'_mode': CANDIDATE_UPLOAD_MODES.merge},
    )

    assert dict(serialized_data) == beamery_serialized_data
    assert form.is_valid(), form.errors
    assert form.cleaned_data == beamery_cleaned_data
