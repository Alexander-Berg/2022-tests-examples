import os
import pytest
import uuid

from functools import partial
from unittest.mock import patch, MagicMock, Mock

from django.conf import settings
from waffle.testutils import override_switch

from intranet.femida.src.actionlog.models import LogRecord
from intranet.femida.src.candidates.bulk_upload.choices import (
    CANDIDATE_UPLOAD_MODES,
    CANDIDATE_UPLOAD_RESOLUTIONS,
)
from intranet.femida.src.candidates.bulk_upload.controllers import (
    create_candidate,
    merge_into_candidate,
    beamery_merge_into_candidate,
)
from intranet.femida.src.candidates.bulk_upload.uploaders import (
    CandidateSheetUploader,
    CandidateStaffUploader,
    CandidateBeameryUploader,
)
from intranet.femida.src.candidates.choices import CONTACT_TYPES, CANDIDATE_STATUSES
from intranet.femida.src.candidates.models import CandidateProfession, Candidate, CandidateContact
from intranet.femida.src.communications.choices import MESSAGE_TYPES
from intranet.femida.src.utils.files import (
    iterate_csv,
    iterate_xls,
    SheetWriterCSVAdapter,
    SheetWriterXLSAdapter,
)

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import assert_not_raises, enable_actionlog, run_commit_hooks


class FakeStaffPerson:

    def __init__(self, login, first_name, last_name, is_external=False):
        self.login = login
        self.first_name = first_name
        self.last_name = last_name
        self.email = f'{login}@ya.ru'
        self.is_external = is_external

    @property
    def data(self):
        result = {
            'login': self.login,
            'name': {
                'first': {'ru': self.first_name},
                'last': {'ru': self.last_name},
            },
            'accounts': [{
                'type': 'gmail',
                'value': self.email,
            }],
            'phones': [],
            'yandex': {'login': self.login},
        }
        if self.is_external:
            result['official'] = {'organization': {'id': settings.EXTERNAL_ORGANIZATION_ID}}
        return result


def _create_candidate(first_name, last_name, email, login=''):
    return f.CandidateContactFactory(
        type=CONTACT_TYPES.email,
        account_id=email,
        candidate__login=login,
        candidate__first_name=first_name,
        candidate__last_name=last_name,
    ).candidate


@pytest.mark.parametrize('mode', CANDIDATE_UPLOAD_MODES._db_values)
@pytest.mark.parametrize('input_file_name, output_file_name, reader_function, writer_class', (
    ('i.csv', 'o.csv', iterate_csv, SheetWriterCSVAdapter),
    ('i.xls', 'o.xls', iterate_xls, SheetWriterXLSAdapter),
))
# Note: xlrd не считывает формулы по умолчанию,
# поэтому пишем ссылки, как текст (как в csv)
@patch(
    target=(
        'intranet.femida.src.candidates.bulk_upload.uploaders'
        '.CandidateSheetUploader._is_csv_output'
    ),
    new=True,
)
@enable_actionlog
def test_candidate_file_uploader(sheet_upload_data, mode, input_file_name, output_file_name,
                                 reader_function, writer_class):
    """
    Тест на базовые случаи загрузки списков кандидатов.
    В таблицах присутствуют поля:
    first_name, last_name, email, original, tags
    """
    # Готовим input-файл
    writer = writer_class(input_file_name)
    for row in sheet_upload_data['input']:
        writer.writerow(row)
    writer.close()

    try:
        # Запускаем upload
        uploader = CandidateSheetUploader(
            input_file_name=input_file_name,
            output_file_name=output_file_name,
            mode=mode,
        )
        uploader.upload()

        # Проверяем output-файл
        result = (tuple(row) for row in reader_function(output_file_name))
        assert sheet_upload_data['output'][mode] == tuple(result)
    finally:
        os.remove(input_file_name)
        os.remove(output_file_name)

    # Проверяем, что события пишутся в actionlog
    if mode == CANDIDATE_UPLOAD_MODES.auto:
        assert LogRecord.objects.filter(action_name='sheet_candidate_create').exists()
        assert LogRecord.objects.filter(action_name='sheet_candidate_merge').exists()
    elif mode != CANDIDATE_UPLOAD_MODES.check:
        assert LogRecord.objects.filter(action_name=f'sheet_candidate_{mode}').exists()


@pytest.mark.parametrize('mode', (
    CANDIDATE_UPLOAD_MODES.create,
    CANDIDATE_UPLOAD_MODES.merge,
))
def test_candidate_upload_professions(mode):
    """
    FEMIDA-5418: проверка на то, что ставится проф.сфера после заливки
    """
    original = f.CandidateFactory()
    profession = f.ProfessionFactory(
        id=100500,
        professional_sphere__id=500100,
        name='developer',
    )

    # Готовим csv-файл
    writer = SheetWriterCSVAdapter('i.csv')
    writer.writerow(('first_name', 'last_name', 'email', 'original', 'professions'))
    writer.writerow(('Ivan', 'Ivanov', 'ivan@ya.ru', original.id, profession.name))
    writer.close()

    try:
        # Запускаем upload
        uploader = CandidateSheetUploader('i.csv', 'o.csv', mode)
        uploader.upload()

        reader = iterate_csv('o.csv')
        headers = next(reader)
        row = next(reader)
        data = dict(zip(headers, row))
    finally:
        os.remove('i.csv')
        os.remove('o.csv')

    candidate_professions = list(
        CandidateProfession.objects
        .filter(candidate_id=data['candidate_id'])
    )
    assert len(candidate_professions) == 1
    assert candidate_professions[0].profession_id == 100500
    assert candidate_professions[0].professional_sphere_id == 500100


@pytest.mark.parametrize('answer, existing_agreement, mode, result', [
    ('', False, CANDIDATE_UPLOAD_MODES.create, None),
    ('да', False, CANDIDATE_UPLOAD_MODES.create, True),
    ('Да', False, CANDIDATE_UPLOAD_MODES.create, True),
    ('дА', False, CANDIDATE_UPLOAD_MODES.create, True),
    ('ДА', False, CANDIDATE_UPLOAD_MODES.create, True),
    ('нет', False, CANDIDATE_UPLOAD_MODES.create, False),
    ('Нет', False, CANDIDATE_UPLOAD_MODES.create, False),
    ('нЕт', False, CANDIDATE_UPLOAD_MODES.create, False),
    ('НЕт', False, CANDIDATE_UPLOAD_MODES.create, False),
    ('неТ', False, CANDIDATE_UPLOAD_MODES.create, False),
    ('НеТ', False, CANDIDATE_UPLOAD_MODES.create, False),
    ('нЕТ', False, CANDIDATE_UPLOAD_MODES.create, False),
    ('НЕТ', False, CANDIDATE_UPLOAD_MODES.create, False),
    ('yes', False, CANDIDATE_UPLOAD_MODES.create, True),
    ('', True, CANDIDATE_UPLOAD_MODES.merge, True),
    ('', False, CANDIDATE_UPLOAD_MODES.merge, False),
    (' ', False, CANDIDATE_UPLOAD_MODES.merge, False),
    (' ', True, CANDIDATE_UPLOAD_MODES.merge, True),
    ('asdfasdf', False, CANDIDATE_UPLOAD_MODES.merge, False),
    ('dsafasdf', True, CANDIDATE_UPLOAD_MODES.merge, True),
    ('да', False, CANDIDATE_UPLOAD_MODES.merge, True),
    ('no', True, CANDIDATE_UPLOAD_MODES.merge, False),
    (' да ', False, CANDIDATE_UPLOAD_MODES.merge, True),
])
def test_candidate_upload_mailing_agreement(answer, existing_agreement, mode, result):
    """
    FEMIDA-7317: Кандидату должен быть установлен флаг vacancies_mailing_agreement
    """
    original = f.CandidateFactory(vacancies_mailing_agreement=existing_agreement)

    # Готовим csv-файл
    writer = SheetWriterCSVAdapter('i.csv')
    writer.writerow(('first_name', 'last_name', 'email', 'original', 'vacancies_mailing_agreement'))
    writer.writerow(('Ivan', 'Ivanov', 'ivan@ya.ru', original.id, answer))
    writer.close()

    try:
        # Запускаем upload
        uploader = CandidateSheetUploader('i.csv', 'o.csv', mode)
        uploader.upload()

        reader = iterate_csv('o.csv')
        headers = next(reader)
        row = next(reader)
        data = dict(zip(headers, row))
    finally:
        os.remove('i.csv')
        os.remove('o.csv')

    candidate= list(
        Candidate.unsafe.filter(id=data['candidate_id'])
    )
    assert len(candidate) == 1
    assert candidate[0].vacancies_mailing_agreement == result


@pytest.mark.parametrize('is_external', (True, False))
@pytest.mark.parametrize('has_similar', (True, False))
@patch('intranet.femida.src.utils.files.yt', MagicMock())
@patch('intranet.femida.src.candidates.bulk_upload.uploaders.SheetWriterYTAdapter.close', Mock())
def test_staff_upload_create(is_external, has_similar):
    f.create_waffle_switch('enable_staff_candidates_auto_sync')
    f.create_waffle_switch('enable_staff_candidates_possible_duplicates_creation')
    person = FakeStaffPerson('new', 'Ivan', 'Ivanov', is_external)
    if has_similar:
        similar_candidate_id = _create_candidate('Denis', person.last_name, person.email).id
        resolution = CANDIDATE_UPLOAD_RESOLUTIONS.created_duplicate
    else:
        similar_candidate_id = None
        resolution = CANDIDATE_UPLOAD_RESOLUTIONS.created

    uploader = CandidateStaffUploader([person.data])
    uploader.upload()

    result = uploader.writer._data[0]
    candidate_id = result['candidate_id']
    candidate = Candidate.unsafe.get(id=candidate_id)

    assert result['result'] == resolution
    assert candidate.id != similar_candidate_id
    assert candidate.login == person.login
    assert candidate.first_name == person.first_name
    assert candidate.last_name == person.last_name
    assert candidate.contacts.filter(type=CONTACT_TYPES.email, account_id=person.email).exists()
    assert candidate.messages.filter(type=MESSAGE_TYPES.note).exists() == is_external


@pytest.mark.parametrize('is_external', (True, False))
@pytest.mark.parametrize('cand_login', ('existing', ''))
@patch('intranet.femida.src.utils.files.yt', MagicMock())
@patch('intranet.femida.src.candidates.bulk_upload.uploaders.SheetWriterYTAdapter.close', Mock())
def test_staff_upload_merge(is_external, cand_login):
    f.create_waffle_switch('enable_staff_candidates_auto_sync')
    person = FakeStaffPerson('existing', 'Алёша', 'Миранчук', is_external)
    candidate = _create_candidate('алЕша', person.last_name, person.email, cand_login)
    uploader = CandidateStaffUploader([person.data])
    uploader.upload()

    result = uploader.writer._data[0]
    candidate_id = result['candidate_id']
    candidate.refresh_from_db()

    assert result['result'] == CANDIDATE_UPLOAD_RESOLUTIONS.merged
    assert candidate_id == candidate.id
    assert candidate.login == 'existing'
    assert candidate.first_name == person.first_name
    assert not candidate.messages.filter(type=MESSAGE_TYPES.note).exists()


@patch('intranet.femida.src.utils.files.yt', MagicMock())
@patch('intranet.femida.src.candidates.bulk_upload.uploaders.SheetWriterYTAdapter.close', Mock())
@pytest.mark.parametrize('cand_first_name, cand_login, resolution', (
    ('Denis', '', CANDIDATE_UPLOAD_RESOLUTIONS.created_duplicate),
    ('Maxim', 'another', CANDIDATE_UPLOAD_RESOLUTIONS.error),
))
def test_staff_upload_conflict(cand_first_name, cand_login, resolution):
    f.create_waffle_switch('enable_staff_candidates_auto_sync')
    person = FakeStaffPerson('conflict', 'Maxim', 'Kotov')
    candidate = _create_candidate(cand_first_name, person.last_name, person.email, cand_login)
    uploader = CandidateStaffUploader([person.data])
    uploader.upload()

    result = uploader.writer._data[0]
    candidate.refresh_from_db()

    assert result['result'] == resolution
    assert result['candidate_id'] is None
    assert list(Candidate.unsafe.all()) == [candidate]
    assert candidate.login == cand_login


@pytest.mark.parametrize('emails, expected_result, expected_contacts', (
    ([], [], set()),
    (['ya@ya@ru'], ['error'], set()),
    (['ya@ya@ru', 'ya@ya.ru'], ['created'], {'ya@ya.ru'}),
))
@patch('intranet.femida.src.utils.files.yt', MagicMock())
@patch('intranet.femida.src.candidates.bulk_upload.uploaders.SheetWriterYTAdapter.close', Mock())
def test_staff_upload_invalid_contacts(emails, expected_result, expected_contacts):
    f.create_waffle_switch('enable_staff_candidates_auto_sync')
    person = FakeStaffPerson('orlov', 'Sergey', 'Orlov')
    data = person.data
    data['yandex'] = {'login': None}
    data['accounts'] = [{'type': 'personal_email', 'value': e} for e in emails]

    uploader = CandidateStaffUploader([data])
    uploader.upload()

    result = [r['result'] for r in uploader.writer._data]
    assert result == expected_result

    contacts = set(
        CandidateContact.objects
        .filter(candidate__login=person.login)
        .values_list('account_id', flat=True)
    )
    assert contacts == expected_contacts


@pytest.mark.parametrize('keep_modification_time', (True, False))
def test_merge_candidate_keep_modification_time(sheet_dataset, keep_modification_time):
    """
    Проверяет возможность не изменять modified при изменении кандидата
    """
    data = sheet_dataset['cleaned_data']
    data['keep_modification_time'] = keep_modification_time
    candidate = f.create_heavy_candidate()
    Candidate.objects.filter(id=candidate.id).update(modified='2000-02-01')
    candidate.refresh_from_db()

    merge_into_candidate(candidate, data)
    candidate.refresh_from_db()
    assert (candidate.modified.year == 2000) is keep_modification_time


@patch('intranet.femida.src.actionlog.models.touch_candidates_task.delay')
@pytest.mark.parametrize('status, is_touched', (
    pytest.param(CANDIDATE_STATUSES.in_progress, True, id='changed'),
    pytest.param(CANDIDATE_STATUSES.closed, False, id='unchanged'),
    pytest.param('', False, id='empty'),
))
@override_switch('enable_candidate_beamery_uploader', active=True)
@enable_actionlog
def test_candidate_beamery_touch_on_status_change(mocked_touch, status, is_touched):
    candidate = f.CandidateFactory(beamery_id=uuid.uuid4(), status=CANDIDATE_STATUSES.closed)
    contact_data = {
        'type': CONTACT_TYPES.beamery,
        'account_id': str(candidate.beamery_id),
    }
    f.CandidateContactFactory(candidate=candidate, **contact_data)
    data = {
        'original': candidate.id,
        'beamery_id': candidate.beamery_id,
        'contacts': [contact_data],
        'status': status,
    }

    uploader = CandidateBeameryUploader(CANDIDATE_UPLOAD_MODES.merge, rows=[data])
    with run_commit_hooks():
        uploader.upload()

    assert mocked_touch.called is is_touched
    if is_touched:
        mocked_touch.assert_called_once_with([candidate.id])


@patch('intranet.femida.src.actionlog.models.touch_candidates_task.delay')
@pytest.mark.parametrize('phones, is_touched', (
    pytest.param(['+7 700 123 4567'], False, id='the-same'),
    pytest.param(['+7 (700) 123-45-67'], False, id='identical'),
    pytest.param(['+7 700 123 4567', '+7 (700) 123 45 67'], False, id='the-same-and-identical'),
    pytest.param(['+7 968 123 4567'], True, id='new'),
    pytest.param(['+7 700 123 4567', '+7 968 123 4567'], True, id='the-same-and-new'),
))
@override_switch('enable_candidate_beamery_uploader', active=True)
@enable_actionlog
def test_candidate_beamery_touch_on_phones_change(mocked_touch, phones, is_touched):
    candidate = f.CandidateFactory(beamery_id=uuid.uuid4(), status=CANDIDATE_STATUSES.closed)
    contact_data = {
        'type': CONTACT_TYPES.phone,
        'account_id': '+7 700 123 4567',
        'normalized_account_id': '+7 700 123 4567',
    }
    f.CandidateContactFactory(candidate=candidate, **contact_data)
    data = {
        'original': candidate.id,
        'beamery_id': candidate.beamery_id,
        'contacts': [{'type': CONTACT_TYPES.phone, 'account_id': i} for i in phones],
    }

    uploader = CandidateBeameryUploader(CANDIDATE_UPLOAD_MODES.merge, rows=[data])
    with run_commit_hooks():
        uploader.upload()

    assert mocked_touch.called is is_touched
    if is_touched:
        mocked_touch.assert_called_once_with([candidate.id])


@patch(
    target='intranet.femida.src.candidates.bulk_upload.controllers.actionlog.is_initialized',
    new=Mock(return_value=True),
)
@override_switch('enable_candidate_beamery_uploader', active=True)
def test_candidate_beamery_respect_inactive_contacts():
    candidate = f.CandidateFactory(beamery_id=uuid.uuid4())
    create_email = partial(f.CandidateContactFactory, candidate=candidate, type=CONTACT_TYPES.email)
    existing_emails = (
        create_email(account_id='active@ya.ru', is_active=True),
        create_email(account_id='inactive@ya.ru', is_active=False),
    )
    new_email_value = 'new@ya.ru'
    contacts = [{'type': p.type, 'account_id': p.account_id} for p in existing_emails]
    contacts.append({'type': CONTACT_TYPES.email, 'account_id': new_email_value})

    beamery_merge_into_candidate(candidate, {'contacts': contacts})

    result_contacts = candidate.contacts.order_by('id')
    assert len(result_contacts) == 3
    active, inactive, new = result_contacts
    assert (active, inactive) == existing_emails
    assert active.is_active
    assert not inactive.is_active
    assert new.is_active
    assert new.account_id == new_email_value


@patch(
    target='intranet.femida.src.candidates.bulk_upload.controllers.actionlog.is_initialized',
    new=Mock(return_value=True),
)
@override_switch('enable_candidate_beamery_uploader', active=True)
def test_candidate_beamery_respect_inactive_tags():
    candidate = f.CandidateFactory(beamery_id=uuid.uuid4())
    create_candidate_tag = partial(f.CandidateTagFactory, candidate=candidate)
    existing_tags = (
        create_candidate_tag(tag__name='active', is_active=True),
        create_candidate_tag(tag__name='inactive', is_active=False),
    )

    beamery_merge_into_candidate(candidate, {'tags': ['active', 'inactive', 'new']})

    result_candidate_tags = candidate.candidate_tags.order_by('id')
    assert len(result_candidate_tags) == 3
    active, inactive, new = result_candidate_tags
    assert (active, inactive) == existing_tags
    assert active.is_active
    assert not inactive.is_active
    assert new.is_active
    assert new.tag.name == 'new'


# Смоук-тесты на заведомо успешные случаи


def test_candidate_create(sheet_dataset):
    with assert_not_raises():
        create_candidate(sheet_dataset['cleaned_data'])


def test_merge_into_candidate(sheet_dataset):
    candidate = f.create_heavy_candidate()
    with assert_not_raises():
        merge_into_candidate(candidate, sheet_dataset['cleaned_data'])
