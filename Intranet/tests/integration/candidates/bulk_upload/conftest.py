import pytest

from collections import namedtuple
from datetime import date, datetime, timezone

from django.conf import settings

from intranet.femida.src.candidates.bulk_upload.choices import (
    CANDIDATE_UPLOAD_RESOLUTIONS as RESOLUTIONS,
    CANDIDATE_UPLOAD_MODES as MODES,
)
from intranet.femida.src.candidates.choices import CONTACT_TYPES

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import AnyFrom


LiteCandidate = namedtuple(
    typename='LiteCandidate',
    field_names=[
        'first_name',
        'last_name',
        'email',
        'original',
        'tags',
    ],
)


_empty_values = ['', None]
EMPTY = AnyFrom(values=_empty_values)
NOT_EMPTY = AnyFrom(exclude=_empty_values)


@pytest.fixture
def sheet_dataset():
    cities = f.CityFactory.create_batch(3)
    professions = f.ProfessionFactory.create_batch(3)
    skills = f.SkillFactory.create_batch(3)
    note_author = f.create_user()

    raw_data = {
        'first_name': 'FirstName',
        'middle_name': 'MiddleName',
        'last_name': 'MiddleName',
        'birthday': '1991-02-03',
        'gender': 'male',
        'country': 'Country',
        'city': 'City',
        'target_cities': ', '.join(i.name_ru for i in cities),
        'email': 'email@ya.ru',
        'phone': '+77001231231',
        'vacancies_mailing_agreement': 'Да',
        'contacts': 'skype, telegram',
        'contact_list': [
            {
                'type': CONTACT_TYPES.hh,
                'account_id': 'https://hh.ru/resume/123',
            },
            {
                'type': CONTACT_TYPES.vk,
                'account_id': 'https://vk.com/me',
            },
        ],
        'professions': ', '.join(i.name for i in professions),
        'skills': ', '.join(i.name for i in skills),
        'tags': 'tag1, tag2, tag3',
        'attachment_link': (
            'https://example.com/some_uuid.pdf,'
            'some.invalid.url'
        ),
        'educations': [{
            'institution': 'MGU',
            'faculty': 'Engineering',
            'degree': 'not_finished',
            'end_date': '2020-02-02',
        }],
        'institution': 'MIT',
        'degree': 'bachelor',
        'end_date': '2015-02',
        'jobs': [{
            'employer': 'Mail.ru',
            'start_date': '2019-01-01',
            'end_date': '2019-06-30',
        }],
        'employer': 'Rambler',
        'note_author': note_author.username,
        'q_Вопрос номер один': 'Ответ-1',
        'q_Вопрос номер два': 'Ответ-2',
        'ah_modified_at': '2021-01-01 00:00:01',
    }

    cleaned_data = {
        'first_name': 'FirstName',
        'middle_name': 'MiddleName',
        'last_name': 'MiddleName',
        'birthday': date(1991, 2, 3),
        'gender': 'male',
        'country': 'Country',
        'city': 'City',
        'target_cities': cities,
        'vacancies_mailing_agreement': True,
        'contacts': [
            {
                'type': 'other',
                'account_id': 'skype',
                'is_main': False,
            },
            {
                'type': 'other',
                'account_id': 'telegram',
                'is_main': False,
            },
            {
                'type': 'hh',
                'account_id': 'https://hh.ru/resume/123',
                'is_main': False,
            },
            {
                'type': 'vk',
                'account_id': 'https://vk.com/me',
                'is_main': False,
            },
            {
                'type': 'email',
                'account_id': 'email@ya.ru',
                'is_main': True,
            },
            {
                'type': 'phone',
                'account_id': '+77001231231',
                'is_main': True,
            },
        ],
        'candidate_professions': [
            {
                'profession': i,
                'professional_sphere_id': i.professional_sphere_id,
                'salary_expectation': '',
            }
            for i in professions
        ],
        'skills': skills,
        'attachment_link': ['https://example.com/some_uuid.pdf'],
        'tags': ['tag1', 'tag2', 'tag3'],
        'educations': [
            {
                'institution': 'MGU',
                'faculty': 'Engineering',
                'degree': 'not_finished',
                'end_date': date(2020, 2, 2),
            },
            {
                'institution': 'MIT',
                'degree': 'bachelor',
                'end_date': date(2015, 2, 1),
            },
        ],
        'jobs': [
            {
                'employer': 'Mail.ru',
                'start_date': date(2019, 1, 1),
                'end_date': date(2019, 6, 30),
                'position': '',
                'salary_evaluation': '',
            },
            {
                'employer': 'Rambler',
            },
        ],
        'answers': [
            {
                'question': 'Вопрос номер один',
                'answer': 'Ответ-1',
            },
            {
                'question': 'Вопрос номер два',
                'answer': 'Ответ-2',
            },
        ],
        'note': '',
        'note_author': note_author,
        'original': None,
        'ah_modified_at': datetime(2021, 1, 1, 0, 0, 1, tzinfo=timezone.utc),
    }

    return {
        'raw_data': raw_data,
        'cleaned_data': cleaned_data,
    }


@pytest.fixture
def sheet_upload_data():
    original = f.CandidateFactory()
    orig_id = str(original.id)

    new = LiteCandidate('Ivan', 'New', 'new@ya.ru', orig_id, '')
    dup = LiteCandidate('Alexandr', 'Duplicate', 'duplicate@ya.ru', orig_id, 'tag')
    mbdup = LiteCandidate('Irina', 'Duplicate', 'duplicate@ya.ru', orig_id, '')
    nddup = LiteCandidate('', '', '', orig_id, '')  # no data duplicate
    inv = LiteCandidate('Invalid', 'Invalid', 'invalid_email', orig_id, '')

    # Создаём кандидата, который будет очевидным дублем для `duplicate`
    # и вероятным дублем для `possible_duplicate`
    db_dup = f.CandidateFactory(
        first_name=dup.first_name,
        last_name=dup.last_name,
    )
    f.CandidateContactFactory(
        candidate=db_dup,
        type=CONTACT_TYPES.email,
        is_main=True,
        account_id=dup.email,
        normalized_account_id=dup.email,
    )

    dup_id = str(db_dup.id)
    orig_url = f'{settings.FEMIDA_URL}candidates/{orig_id}'
    dup_url = f'{settings.FEMIDA_URL}candidates/{dup_id}'

    input_headers = ('first_name', 'last_name', 'email', 'original', 'tags')
    input_rows = (input_headers, new, dup, mbdup, nddup, inv)
    output_headers = input_headers + (
        'result',
        'errors',
        'candidate_id',
        'candidate_url',
        'conflict_id',
        'conflict_url',
    )

    # Ожидаемый output в режиме check
    check_output_rows = (
        # ... | result | errors | candidate_id | candidate_url | conflict_id | conflict_url
        output_headers,
        new + (RESOLUTIONS.created, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY),
        dup + (RESOLUTIONS.merged, EMPTY, dup_id, dup_url, EMPTY, EMPTY),
        mbdup + (RESOLUTIONS.created_duplicate, EMPTY, EMPTY, EMPTY, dup_id, dup_url),
        nddup + (RESOLUTIONS.error, NOT_EMPTY, EMPTY, EMPTY, EMPTY, EMPTY),
        inv + (RESOLUTIONS.error, NOT_EMPTY, EMPTY, EMPTY, EMPTY, EMPTY),
    )

    # Ожидаемый output в режиме auto
    auto_output_rows = (
        # ... | result | errors | candidate_id | candidate_url | conflict_id | conflict_url
        output_headers,
        new + (RESOLUTIONS.created, EMPTY, NOT_EMPTY, NOT_EMPTY, EMPTY, EMPTY),
        dup + (RESOLUTIONS.merged, EMPTY, dup_id, dup_url, EMPTY, EMPTY),
        mbdup + (RESOLUTIONS.created_duplicate, EMPTY, NOT_EMPTY, NOT_EMPTY, dup_id, dup_url),
        nddup + (RESOLUTIONS.error, NOT_EMPTY, EMPTY, EMPTY, EMPTY, EMPTY),
        inv + (RESOLUTIONS.error, NOT_EMPTY, EMPTY, EMPTY, EMPTY, EMPTY),
    )

    # Ожидаемый output в режиме create
    create_output_rows = (
        # ... | result | errors | candidate_id | candidate_url | conflict_id | conflict_url
        output_headers,
        new + (RESOLUTIONS.created, EMPTY, NOT_EMPTY, NOT_EMPTY, EMPTY, EMPTY),
        dup + (RESOLUTIONS.created, EMPTY, NOT_EMPTY, NOT_EMPTY, EMPTY, EMPTY),
        mbdup + (RESOLUTIONS.created, EMPTY, NOT_EMPTY, NOT_EMPTY, EMPTY, EMPTY),
        nddup + (RESOLUTIONS.error, NOT_EMPTY, EMPTY, EMPTY, EMPTY, EMPTY),
        inv + (RESOLUTIONS.error, NOT_EMPTY, EMPTY, EMPTY, EMPTY, EMPTY),
    )

    # Ожидаемый output в режиме merge
    merge_output_rows = (
        # ... | result | errors | candidate_id | candidate_url | conflict_id | conflict_url
        output_headers,
        new + (RESOLUTIONS.merged, EMPTY, orig_id, orig_url, EMPTY, EMPTY),
        dup + (RESOLUTIONS.merged, EMPTY, orig_id, orig_url, EMPTY, EMPTY),
        mbdup + (RESOLUTIONS.merged, EMPTY, orig_id, orig_url, EMPTY, EMPTY),
        nddup + (RESOLUTIONS.merged, EMPTY, orig_id, orig_url, EMPTY, EMPTY),
        inv + (RESOLUTIONS.error, NOT_EMPTY, EMPTY, EMPTY, EMPTY, EMPTY),
    )

    return {
        'input': input_rows,
        'output': {
            MODES.check: check_output_rows,
            MODES.auto: auto_output_rows,
            MODES.create: create_output_rows,
            MODES.merge: merge_output_rows,
        },
    }
