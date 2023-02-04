import pytest

from unittest.mock import patch

from django.urls.base import reverse

from intranet.femida.src.candidates.choices import (
    SUBMISSION_SOURCES,
    SUBMISSION_STATUSES,
    REFERENCE_STATUSES,
)
from intranet.femida.src.candidates.models import CandidateSubmission
from intranet.femida.src.core.switches import TemporarySwitch
from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_submission_list(su_client):
    f.create_submission()
    url = reverse('api:submissions:list')
    response = su_client.get(url)
    assert response.status_code == 200


def test_submission_filter_form(su_client):
    url = reverse('api:submissions:filter-form')
    response = su_client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize('source', SUBMISSION_SOURCES._db_values)
def test_submission_detail(su_client, source):
    submission = f.create_submission(source=source)
    url = reverse('api:submissions:detail', kwargs={'pk': submission.id})
    response = su_client.get(url)
    assert response.status_code == 200


def test_submission_detail_publication_source(su_client):
    submission = f.create_submission(source=SUBMISSION_SOURCES.publication)
    publication = submission.publication
    url = reverse('api:submissions:detail', kwargs={'pk': submission.id})

    response = su_client.get(url)
    assert response.status_code == 200, response.content

    response_data = response.json()
    assert publication.title == response_data['form']['title']
    assert publication.title == response_data['source_details']['title']


def test_submission_handle_form(su_client, dd_dataset):
    submission = dd_dataset['ivan_submission']
    url = reverse('api:submissions:handle-form', kwargs={'pk': submission.id})
    response = su_client.get(url)
    assert response.status_code == 200

    duplicate_choices = response.data['structure']['duplicate']['choices']
    duplicate_ids_received = {choice['value'] for choice in duplicate_choices}
    duplicate_ids = {dd_dataset['ivan'].id, dd_dataset['ivan_too'].id}
    assert len(duplicate_choices) == 2
    assert duplicate_ids_received == duplicate_ids


@pytest.mark.parametrize('request_data, enable_candidate_main_recruiter', (
    (
        {
            'is_rejection': False,
        },
        True,
    ),
    (
        {
            'is_rejection': True,
            'text': 'sample rejection text',
        },
        False,
    ),
    (
        {
            'is_rejection': True,
            'text': 'sample rejection text',
            'schedule_time': '2222-01-01 15:35:20',
        },
        False,
    ),
))
def test_submission_handle(su_client, enable_candidate_main_recruiter, dd_dataset, request_data):
    f.create_waffle_switch('enable_candidate_main_recruiter', enable_candidate_main_recruiter)
    submission = dd_dataset['ivan_submission']
    url = reverse('api:submissions:handle', kwargs={'pk': submission.id})
    response = su_client.post(url, data=request_data)
    assert response.status_code == 200, response.content
    submission.refresh_from_db()
    if enable_candidate_main_recruiter:
        assert submission.candidate.main_recruiter == f.get_superuser()
    else:
        assert not submission.candidate.main_recruiter


def test_submission_reject(su_client):
    submission = f.create_submission()
    url = reverse('api:submissions:reject', kwargs={'pk': submission.id})
    response = su_client.post(url)
    assert response.status_code == 200


reference_action_names = (
    'approve-reference',
    'approve-reference-without-benefits',
    'reject-reference',
)


@patch(
    target='intranet.femida.src.candidates.submissions.workflow.IssueTransitionOperation.__call__',
    new=lambda *x, **y: None,
)
@pytest.mark.parametrize('action', reference_action_names)
def test_reference_action(su_client, action):
    reference = f.ReferenceFactory.create()
    submission = f.SubmissionFactory(
        reference=reference,
        source=SUBMISSION_SOURCES.reference,
        status=SUBMISSION_STATUSES.draft,
    )
    url = reverse('api:submissions:{}'.format(action), kwargs={'pk': submission.id})
    data = {
        'comment': 'text',
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


@pytest.mark.parametrize('action', reference_action_names)
def test_reference_action_form(su_client, action):
    reference = f.ReferenceFactory.create()
    submission = f.SubmissionFactory(reference=reference, status=SUBMISSION_STATUSES.draft)
    url = reverse('api:submissions:{}-form'.format(action), kwargs={'pk': submission.id})
    response = su_client.get(url)
    assert response.status_code == 200


@patch(
    target='intranet.femida.src.staff.tasks.give_approved_reference_counter_achievement.delay',
    return_value=None,
)
@patch(
    target='intranet.femida.src.candidates.submissions.workflow.IssueTransitionOperation.__call__',
    new=lambda *x, **y: None,
)
@pytest.mark.parametrize('action, result', zip(reference_action_names, [True, True, False]))
def test_reference_action_gives_green_achievement(task_mock, su_client, action, result):
    f.create_waffle_switch(TemporarySwitch.GREEN_REFERENCE_ACHIEVEMENTS, True)
    user = f.UserFactory()
    f.create_n_references_with_m_closed_offers(user, (1, 1), REFERENCE_STATUSES.new)
    submission = CandidateSubmission.unsafe.first()
    url = reverse('api:submissions:{}'.format(action), kwargs={'pk': submission.id})
    data = {
        'comment': 'text',
    }
    response = su_client.post(url, data)
    assert response.status_code == 200
    assert task_mock.called == result
    if result:
        assert task_mock.call_args.args == ([user.username],)
