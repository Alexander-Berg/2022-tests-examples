import pytest

from unittest.mock import patch

from intranet.femida.src.contest.api import ContestConflictError, ContestError
from intranet.femida.src.candidates.submissions.internships import handle_internship_submission
from intranet.femida.src.candidates.models import DuplicationCase, Challenge, CandidateSubmission
from intranet.femida.src.candidates.choices import SUBMISSION_STATUSES

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


register_to_contest_path = (
    'intranet.femida.src.candidates.submissions.internships.register_to_contest'
)

notify_about_failed_contest_registration_path = (
    'intranet.femida.src.candidates.submissions.internships.'
    'notify_about_failed_registration_to_contest'
)


def faked_register_to_contest(contest_id, login):
    return 1


def faked_register_to_contest_with_conflict_error(contest_id, login):
    raise ContestConflictError


def faked_register_to_contest_with_error(contest_id, login):
    raise ContestError


@patch(register_to_contest_path, faked_register_to_contest)
def test_submission_not_duplicate(dd_dataset):
    submission = f.create_submission(
        candidate_data={
            'cand_name': 'Name',
            'cand_surname': 'Surname',
            'cand_phone': '12345',
            'cand_email': 'email@yandex.ru',
            'contest_id': 1,
            'login': 'login',
        }
    )
    candidate = handle_internship_submission(submission)
    assert not DuplicationCase.unsafe.exists()
    assert candidate.first_name == 'Name'
    assert submission.status == SUBMISSION_STATUSES.closed


@patch(register_to_contest_path, faked_register_to_contest)
def test_submission_maybe_duplicate(dd_dataset):
    submission = f.create_submission(
        candidate_data={
            'cand_name': 'Petr',
            'cand_surname': 'Ivanov',
            'cand_phone': '71234567899',
            'cand_email': 'ivanov@yandex.ru',
            'contest_id': 1,
            'login': 'login',
        }
    )
    candidate = handle_internship_submission(submission)
    assert DuplicationCase.unsafe.exists()
    assert candidate.first_name == 'Petr'
    assert candidate != dd_dataset['ivan']
    assert submission.status == SUBMISSION_STATUSES.closed


def _is_challenge_created():
    return (
        Challenge.objects
        .filter(answers__contains={
            'contest': {'id': 1},
            'participation': {'id': 1},
        })
        .exists()
    )


@patch(register_to_contest_path, faked_register_to_contest)
def test_submission_definitely_duplicate(dd_dataset):
    submission = dd_dataset['ivan_submission']
    candidate = handle_internship_submission(submission)
    assert not DuplicationCase.unsafe.exists()
    assert candidate == dd_dataset['ivan']
    assert _is_challenge_created()
    assert submission.status == SUBMISSION_STATUSES.closed


@pytest.mark.parametrize('notify', (True, False))
@patch(register_to_contest_path, faked_register_to_contest_with_conflict_error)
@patch(notify_about_failed_contest_registration_path)
def test_contest_conflict_error(notify_mock, dd_dataset, notify):
    """
    Проверяем, что ContestConflictError не приводит к откату транзакции
    """
    submission = dd_dataset['ivan_submission']
    submission.candidate_data['notify'] = str(notify)
    candidate = handle_internship_submission(submission)
    assert not DuplicationCase.unsafe.exists()
    assert candidate == dd_dataset['ivan']
    assert not _is_challenge_created()
    submission = CandidateSubmission.unsafe.get(id=submission.id)
    assert submission.status == SUBMISSION_STATUSES.closed
    assert notify_mock.called == notify


@patch(register_to_contest_path, faked_register_to_contest_with_error)
def test_contest_error(dd_dataset):
    """
    Проверяем, что обычный ContestError приводит к откату транзакции
    """
    submission = dd_dataset['ivan_submission']
    with pytest.raises(ContestError):
        handle_internship_submission(submission)
    submission = CandidateSubmission.unsafe.get(id=submission.id)
    assert submission.status != SUBMISSION_STATUSES.closed
