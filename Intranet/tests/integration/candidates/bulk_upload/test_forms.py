import pytest
import uuid

from datetime import date

from intranet.femida.src.candidates.bulk_upload.forms import (
    CandidateBulkUploadSheetForm,
    CandidateBulkUploadBeameryForm,
)
from intranet.femida.src.candidates.bulk_upload.cache import CandidateDictionariesCache
from intranet.femida.src.candidates.bulk_upload.choices import CANDIDATE_UPLOAD_MODES
from intranet.femida.src.candidates.choices import CONTACT_TYPES, CANDIDATE_DEGREES

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_candidate_bulk_upload_sheet_form(sheet_dataset):
    data = sheet_dataset['raw_data']
    expected_cleaned_data = sheet_dataset['cleaned_data']

    mapping = CandidateDictionariesCache()
    form = CandidateBulkUploadSheetForm(
        data=data,
        initial={
            '_mapping': {
                'cities': mapping.cities_by_name_map,
                'professions': mapping.professions_by_name_map,
                'skills': mapping.skills_by_name_map,
            },
        },
    )

    assert form.is_valid(), form.errors
    assert form.cleaned_data == expected_cleaned_data


@pytest.mark.parametrize('data', (
    {
        'email': 'test@ya.ru',
        'phone': '+77001231238',
        'contacts': 'facebook,telegram',
        'contact_list': [{'type': 'hh', 'account_id': 'hh.ru/resume/123'}],
        'is_valid': True,
        'expected': {
            'test@ya.ru',
            '+77001231238',
            'facebook',
            'telegram',
            'hh.ru/resume/123',
        },
    },
    {
        'email': 'test@ya.ru',
        'is_valid': True,
        'expected': {'test@ya.ru'},
    },
    {
        'contact_list': [{'type': 'hh', 'account_id': 'hh.ru/resume/123'}],
        'is_valid': True,
        'expected': {'hh.ru/resume/123'},
    },
    {
        'is_valid': False,
        'expected': set(),
    },
))
def test_candidate_bulk_upload_sheet_form_contacts(data):
    is_valid = data.pop('is_valid')
    expected = data.pop('expected')
    data['first_name'] = 'Ivan'
    data['last_name'] = 'Ivanov'
    form = CandidateBulkUploadSheetForm(data=data)
    assert form.is_valid() is is_valid, form.errors
    result = {c['account_id'] for c in form.cleaned_data['contacts']}
    assert result == expected


@pytest.mark.parametrize('data, options', (
    pytest.param(
        {
            'last_name': 'Martynov',
        },
        {
            'mode': CANDIDATE_UPLOAD_MODES.create,
            'is_valid': True,
            'error_fields': set(),
        },
        id='min-required-fields-for-create',
    ),
    pytest.param(
        {
            'last_name': 'Martynov',
            'first_name': 'I' * 256,  # > 255 символов
            'birthday': '2000-20-50',  # невалидная дата рождения
            'gender': 'male',
        },
        {
            'mode': CANDIDATE_UPLOAD_MODES.create,
            'is_valid': True,
            'error_fields': {'birthday', 'first_name'},
        },
        id='skip-invalid-fields-for-create',
    ),
    pytest.param(
        {
            'last_name': 'I' * 256,  # > 255 символов
        },
        {
            'mode': CANDIDATE_UPLOAD_MODES.create,
            'is_valid': False,
            'error_fields': {'last_name'},
        },
        id='fail-on-invalid-required-fields-for-create',
    ),
    pytest.param(
        {
            'original': 100500,
        },
        {
            'mode': CANDIDATE_UPLOAD_MODES.merge,
            'is_valid': True,
            'error_fields': set(),
        },
        id='min-required-fields-for-merge',
    ),
    pytest.param(
        {
            'original': 100500,
            'gender': 'undefined',  # невалидный пол
        },
        {
            'mode': CANDIDATE_UPLOAD_MODES.merge,
            'is_valid': True,
            'error_fields': {'gender'},
        },
        id='skip-invalid-fields-for-merge',
    ),
    pytest.param(
        {
            'original': 500100,  # неизвестный кандидат
        },
        {
            'mode': CANDIDATE_UPLOAD_MODES.merge,
            'is_valid': False,
            'error_fields': {'original'},
        },
        id='fail-on-invalid-required-fields-for-merge',
    ),
))
def test_candidate_bulk_upload_beamery_form(data, options):
    f.CandidateFactory(id=100500)
    beamery_id = str(uuid.uuid4())
    always_required_data = {
        'beamery_id': beamery_id,
        'contacts': [{'type': CONTACT_TYPES.beamery, 'account_id': beamery_id}],
    }
    form = CandidateBulkUploadBeameryForm(
        data=always_required_data | data,
        initial={'_mode': options['mode']},
    )

    assert form.is_valid() == options['is_valid']
    assert set(form.errors_as_dict()['errors']) == options['error_fields']


# Тест-кейсы на игнорирование невалидных значений в списковых полях
# в форме загрузки кандидатов из Бимери в Фемиду
@pytest.mark.parametrize('field, field_data, expected', (
    pytest.param(
        'contacts',
        [
            {'type': 'email', 'account_id': 'good@mail.com'},
            {'type': 'email', 'account_id': 'bad-mail-com'},
        ],
        [{'type': 'email', 'account_id': 'good@mail.com', 'is_main': False}],
        id='contacts',
    ),
    pytest.param(
        'educations',
        [
            {'institution': 'MSU', 'degree': 'aspirant'},
            {'institution': 'HSE', 'degree': CANDIDATE_DEGREES.bachelor},
        ],
        [{
            'institution': 'HSE',
            'degree': CANDIDATE_DEGREES.bachelor,
            'faculty': '',
            'end_date': None,
        }],
        id='educations',
    ),
    pytest.param(
        'jobs',
        [
            {'employer': 'Yandex', 'start_date': '2017-02-08'},
            {'employer': 'P&G'},
        ],
        [{
            'employer': 'Yandex',
            'start_date': date(2017, 2, 8),
            'end_date': None,
            'position': '',
            'salary_evaluation': '',
        }],
        id='jobs',
    ),
    pytest.param(
        'tags',
        ['simple-tag', 'i' * 256, 'another-tag'],
        ['simple-tag', 'another-tag'],
        id='tags',
    ),
))
def test_candidate_bulk_upload_beamery_form_skip_invalid_values(field, field_data, expected):
    candidate = f.CandidateFactory()
    beamery_id = str(uuid.uuid4())
    data = {
        'original': candidate.id,
        'beamery_id': beamery_id,
        'contacts': [{'type': CONTACT_TYPES.beamery, 'account_id': beamery_id}],
        field: field_data,
    }
    form = CandidateBulkUploadBeameryForm(data, initial={'_mode': CANDIDATE_UPLOAD_MODES.merge})

    assert form.is_valid()
    assert form.cleaned_data[field] == expected
