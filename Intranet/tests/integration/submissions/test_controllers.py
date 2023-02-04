import pytest
import pytz

from datetime import datetime
from unittest.mock import patch

from intranet.femida.src.candidates.choices import (
    SUBMISSION_SOURCES,
    CANDIDATE_STATUSES,
    CONSIDERATION_STATUSES,
    CONSIDERATION_RESOLUTIONS,
)
from intranet.femida.src.candidates.submissions.controllers import SubmissionController
from intranet.femida.src.communications.choices import MESSAGE_STATUSES
from intranet.femida.src.interviews.choices import APPLICATION_STATUSES
from intranet.femida.src.offers.choices import SOURCES

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import run_commit_hooks


pytestmark = pytest.mark.django_db


_submission_candidate_data = {
    'cand_name': 'Ivan',
    'cand_surname': 'Ivanov',
    'cand_phone': '+77010010010',
    'cand_email': 'test@example.com',
}


def test_create_existing_attachment():
    candidate = f.create_heavy_candidate()
    submission = f.create_submission()

    candidate_attachment = candidate.candidate_attachments.first().attachment
    submission.attachment = f.AttachmentFactory(sha1=candidate_attachment.sha1)
    submission.save()

    ctl = SubmissionController(submission)
    ctl._close_submission(candidate)
    ctl._create_missing_attachment()

    assert not candidate.candidate_attachments.filter(attachment=submission.attachment).exists()


def test_create_missing_attachment():
    candidate = f.create_heavy_candidate()
    submission = f.create_submission()
    submission.attachment = f.AttachmentFactory(sha1='123')
    submission.save()

    ctl = SubmissionController(submission)
    ctl._close_submission(candidate)
    ctl._create_missing_attachment()

    assert candidate.candidate_attachments.filter(attachment=submission.attachment).exists()


# type, account_id, total_contacts_count
create_missing_contacts_test_cases = [
    ('phone', _submission_candidate_data['cand_phone'], 2),
    ('phone', '+77011231231', 3),
    ('email', _submission_candidate_data['cand_email'], 2),
    ('email', 'kdfk23@example.com', 3),
]


@pytest.mark.parametrize('test_case', create_missing_contacts_test_cases)
def test_create_missing_contacts(test_case):
    _type, account_id, total_contacts_count = test_case
    candidate = f.CandidateFactory()
    f.CandidateContactFactory(
        candidate=candidate,
        type=_type,
        account_id=account_id,
    )
    submission = f.create_submission(candidate_data=_submission_candidate_data)

    ctl = SubmissionController(submission)
    ctl._close_submission(candidate)
    ctl._create_missing_contacts()

    assert candidate.contacts.filter(type=_type, account_id=account_id).count() == 1
    assert candidate.contacts.count() == total_contacts_count


@pytest.mark.parametrize('submission_source, candidate_source', (
    (SUBMISSION_SOURCES.form, SOURCES.yandex_job_website),
    (SUBMISSION_SOURCES.reference, SOURCES.internal_reference),
    (SUBMISSION_SOURCES.rotation, SOURCES.rotation),
))
def test_create_candidate_with_correct_source(submission_source, candidate_source):
    submission = f.create_submission(source=submission_source)
    candidate = SubmissionController(submission).create_candidate()
    assert candidate.source == candidate_source
    assert not submission.is_fast_rejection


@pytest.mark.parametrize('submission_source, candidate_source', (
    (SUBMISSION_SOURCES.form, SOURCES.yandex_job_website),
    (SUBMISSION_SOURCES.reference, SOURCES.internal_reference),
    (SUBMISSION_SOURCES.rotation, SOURCES.rotation),
))
def test_merge_candidate_with_correct_source(submission_source, candidate_source):
    submission = f.create_submission(source=submission_source)
    old_candidate = f.create_candidate_with_consideration(status=CANDIDATE_STATUSES.closed)
    candidate = SubmissionController(submission).merge_into_candidate(old_candidate)
    assert candidate.source == candidate_source
    assert not submission.is_fast_rejection


@pytest.mark.parametrize('is_internship, responsibles_count', ((True, 1), (False, 2)))
def test_create_candidate_with_correct_responsibles(is_internship, responsibles_count):
    initiator = None if is_internship else f.create_recruiter()
    vacancy = f.create_vacancy()

    submission = f.create_submission()
    form = submission.form
    form.is_internship = is_internship
    form.vacancies.set([vacancy])

    candidate = SubmissionController(submission=submission, initiator=initiator).create_candidate()
    assert candidate.responsibles.count() == responsibles_count


def test_create_candidate_with_fast_reject():
    submission = f.create_submission()
    candidate = SubmissionController(submission).create_candidate(
        is_rejection=True,
    )
    assert submission.is_fast_rejection
    assert candidate.status == CANDIDATE_STATUSES.closed

    considerations = list(candidate.considerations.all())
    assert len(considerations) == 1
    consideration = considerations[0]
    assert consideration.state == CONSIDERATION_STATUSES.archived
    assert consideration.resolution == CONSIDERATION_RESOLUTIONS.rejected_by_resume

    applications_statuses = set(candidate.applications.values_list('status', flat=True))
    assert applications_statuses == {APPLICATION_STATUSES.closed}


@pytest.mark.parametrize('data', (
    {
        'candidate_status': CANDIDATE_STATUSES.in_progress,
        'considerations_count': 1,
        'consideration_status': CONSIDERATION_STATUSES.in_progress,
        'resolution': '',
    },
    {
        'candidate_status': CANDIDATE_STATUSES.closed,
        'considerations_count': 2,
        'consideration_status': CONSIDERATION_STATUSES.archived,
        'resolution': CONSIDERATION_RESOLUTIONS.rejected_by_resume,
    },
))
def test_merge_into_candidate_with_fast_reject(data):
    submission = f.create_submission()
    old_candidate = f.create_candidate_with_consideration(status=data['candidate_status'])
    candidate = SubmissionController(submission).merge_into_candidate(
        candidate=old_candidate,
        is_rejection=True,
    )
    assert submission.is_fast_rejection
    assert candidate.status == data['candidate_status']

    considerations = candidate.considerations.all()
    assert considerations.count() == data['considerations_count']
    consideration = considerations.last()
    assert consideration.state == data['consideration_status']
    assert consideration.resolution == data['resolution']

    applications_statuses = set(candidate.applications.values_list('status', flat=True))
    assert applications_statuses == {APPLICATION_STATUSES.closed}


@patch(
    'intranet.femida.src.candidates.considerations.controllers'
    '.send_candidate_reference_event_task.delay'
)
def test_no_reference_issue_transitions_on_fast_reject_create(mocked_delay):
    submission = f.create_submission()
    with run_commit_hooks():
        SubmissionController(submission).create_candidate(is_rejection=True)
    assert not mocked_delay.called


@pytest.mark.parametrize('candidate_status', CANDIDATE_STATUSES._db_values)
@patch(
    'intranet.femida.src.candidates.considerations.controllers'
    '.send_candidate_reference_event_task.delay'
)
def test_no_reference_issue_transitions_on_fast_reject_merge(mocked_delay, candidate_status):
    submission = f.create_submission()
    old_candidate = f.create_candidate_with_consideration(status=candidate_status)
    with run_commit_hooks():
        SubmissionController(submission).merge_into_candidate(
            candidate=old_candidate,
            is_rejection=True,
        )
    assert not mocked_delay.called


@pytest.mark.parametrize('schedule_time, message_status', (
    (datetime(2020, 1, 1, 15, 35, 20, tzinfo=pytz.utc), MESSAGE_STATUSES.scheduled),
    (None, MESSAGE_STATUSES.sending),
))
def test_create_rejection_message(schedule_time, message_status):
    submission = f.create_submission()
    controller = SubmissionController(submission)
    candidate = controller.create_candidate(is_rejection=True)
    assert submission.is_fast_rejection

    controller.create_rejection_message(text='sample rejection text', schedule_time=schedule_time)
    assert candidate.messages.count() == 2
    messages_data = list(
        candidate.messages
        .values('status', 'subject', 'type', 'text')
        .order_by('id')
    )
    assert messages_data == [
        {
            'status': u'sent',
            'text': u'something1',
            'type': u'brief',
            'subject': u'',
        },
        {
            'status': message_status,
            'text': u'sample rejection text',
            'type': u'outcoming',
            'subject': u'От компании Яндекс',
        },
    ]
