import pytest

from intranet.femida.src.candidates.helpers import close_candidate
from intranet.femida.src.candidates.considerations.helpers import archive_consideration
from intranet.femida.src.candidates.choices import (
    SUBMISSION_SOURCES,
    SUBMISSION_STATUSES,
    CONSIDERATION_RESOLUTIONS,
    CONSIDERATION_STATUSES,
    CANDIDATE_STATUSES,
)
from intranet.femida.src.candidates.models import CandidateSubmission
from intranet.femida.src.candidates.submissions.helpers import filter_submissions_by_recruiter
from intranet.femida.src.offers.choices import SOURCES

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def _filter_submissions_by_recruiter(recruiter):
    return filter_submissions_by_recruiter(CandidateSubmission.unsafe.all(), recruiter)


def test_filter_submissions_by_recruiter_new_form():
    """
    Новые отклики из КФ на вакансии данного рекрутера
    """
    recruiter = f.create_recruiter()
    vacancy = f.VacancyFactory()
    vacancy.set_main_recruiter(recruiter)
    submission_form = f.SubmissionFormFactory()
    submission_form.vacancies.add(vacancy)
    f.SubmissionFactory.create_batch(3)  # пустышки
    expected = f.SubmissionFactory(
        source=SUBMISSION_SOURCES.form,
        status=SUBMISSION_STATUSES.new,
        form=submission_form,
    )
    result = _filter_submissions_by_recruiter(recruiter)
    assert [expected] == list(result)


def test_filter_submissions_by_recruiter_new_reference():
    """
    Новые отклики-рекомендации на вакансии данного рекрутера
    """
    recruiter = f.create_recruiter()
    vacancy = f.VacancyFactory()
    vacancy.set_main_recruiter(recruiter)
    reference = f.ReferenceFactory()
    reference.vacancies.add(vacancy)
    f.SubmissionFactory.create_batch(3)  # пустышки
    expected = f.SubmissionFactory(
        source=SUBMISSION_SOURCES.reference,
        status=SUBMISSION_STATUSES.new,
        reference=reference,
    )
    result = _filter_submissions_by_recruiter(recruiter)
    assert [expected] == list(result)


def test_filter_submissions_by_recruiter_closed():
    """
    Отклики, обработанные данным рекрутером
    """
    recruiter = f.create_recruiter()
    f.SubmissionFactory.create_batch(3)  # пустышки
    expected = f.SubmissionFactory(
        status=SUBMISSION_STATUSES.closed,
        responsible=recruiter,
    )
    result = _filter_submissions_by_recruiter(recruiter)
    assert [expected] == list(result)


def test_filter_submissions_by_recruiter_processed_reference():
    """
    Отклики, где рекрутер обработал рекомендацию
    """
    recruiter = f.create_recruiter()
    reference = f.ReferenceFactory(processed_by=recruiter)
    f.SubmissionFactory.create_batch(3)  # пустышки
    expected = f.SubmissionFactory(reference=reference)
    result = _filter_submissions_by_recruiter(recruiter)
    assert [expected] == list(result)


def test_archive_consideration():
    candidate = f.create_candidate_with_consideration()
    consideration = candidate.considerations.last()
    assert consideration.state == CONSIDERATION_STATUSES.in_progress
    assert consideration.extended_status == CONSIDERATION_STATUSES.in_progress
    assert consideration.resolution == ''
    assert list(consideration.responsibles.all()) == []

    archive_consideration(
        consideration=consideration,
        resolution=CONSIDERATION_RESOLUTIONS.rejected_by_resume,
    )
    assert consideration.state == CONSIDERATION_STATUSES.archived
    assert consideration.extended_status == CONSIDERATION_STATUSES.archived
    assert consideration.resolution == CONSIDERATION_RESOLUTIONS.rejected_by_resume
    assert set(consideration.responsibles.all()) == set(candidate.responsibles.all())
    candidate_responsibles = dict(candidate.candidate_responsibles.values_list('user', 'role'))
    consideration_responsibles = dict(
        consideration.consideration_responsibles.values_list('user', 'role'),
    )
    assert candidate_responsibles == consideration_responsibles


def test_close_candidate():
    candidate = f.create_candidate_with_consideration(source_description='test')
    assert candidate.status == CANDIDATE_STATUSES.in_progress
    assert candidate.source == SOURCES.other
    assert candidate.source_description == 'test'
    assert candidate.responsibles.count() == 2

    close_candidate(candidate)
    assert candidate.status == CANDIDATE_STATUSES.closed
    assert candidate.source == ''
    assert candidate.source_description == ''
    assert candidate.responsibles.count() == 0
