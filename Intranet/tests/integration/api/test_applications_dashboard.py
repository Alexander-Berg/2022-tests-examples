import pytest

from django.urls.base import reverse

from intranet.femida.src.interviews.choices import (
    APPLICATION_STATUSES as STATUSES,
    APPLICATION_RESOLUTIONS as RESOLUTIONS,
    APPLICATION_PROPOSAL_STATUSES as PROPOSAL_STATUSES,
    APPLICATION_DASHBOARD_STAGES as STAGES,
    CLOSED_APPLICATION_RESOLUTIONS,
    INTERVIEW_TYPES,
)

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


OPEN_STAGES = {s for s, _ in STAGES if s not in CLOSED_APPLICATION_RESOLUTIONS}


def _test_stage(client, stage, vacancy_id, expected_count=1):
    url = reverse('api:applications:dashboard')
    data = {
        'vacancies': vacancy_id,
        'stage': stage,
    }
    response = client.get(url, data)
    assert response.status_code == 200

    response_data = response.json()
    assert response_data['count'] == expected_count
    return response_data


def _test_empty_stages(client, stage, vacancy_id):
    """
    Проверяет, что стадии не пересекаются между собой.
    Актуально только для стадий активных прет-ов,
    т.к. стадии закрытых основаны на резолюциях и никогда не пересекутся
    """
    if stage not in OPEN_STAGES:
        return
    empty_stages = OPEN_STAGES - {stage}
    for s in empty_stages:
        _test_stage(client, s, vacancy_id, expected_count=0)


@pytest.mark.parametrize('stage,status,resolution,proposal_status', (
    (STAGES.draft, STATUSES.draft, '', ''),
    (STAGES.new, STATUSES.in_progress, '', PROPOSAL_STATUSES.undefined),
    (STAGES.team_is_interested, STATUSES.in_progress, '', PROPOSAL_STATUSES.accepted),
    (STAGES.offer_agreement, STATUSES.in_progress, RESOLUTIONS.offer_agreement, ''),
    (STAGES.closed, STATUSES.closed, '', ''),
    (STAGES.did_not_pass_assessments, STATUSES.closed, RESOLUTIONS.did_not_pass_assessments, ''),
    (STAGES.team_was_not_interested, STATUSES.closed, RESOLUTIONS.team_was_not_interested, ''),
    (STAGES.team_was_not_selected, STATUSES.closed, RESOLUTIONS.team_was_not_selected, ''),
    (STAGES.refused_us, STATUSES.closed, RESOLUTIONS.refused_us, ''),
    (STAGES.offer_rejected, STATUSES.closed, RESOLUTIONS.offer_rejected, ''),
    (STAGES.offer_accepted, STATUSES.closed, RESOLUTIONS.offer_accepted, ''),
    (STAGES.rotated, STATUSES.closed, RESOLUTIONS.rotated, ''),
    (STAGES.vacancy_closed, STATUSES.closed, RESOLUTIONS.vacancy_closed, ''),
    (STAGES.incorrect, STATUSES.closed, RESOLUTIONS.incorrect, ''),
    (STAGES.on_hold, STATUSES.closed, RESOLUTIONS.on_hold, ''),
    (STAGES.consideration_archived, STATUSES.closed, RESOLUTIONS.consideration_archived, ''),
))
def test_dashboard_by_status(su_client, stage, status, resolution, proposal_status):
    application = f.ApplicationFactory(
        status=status,
        resolution=resolution,
        proposal_status=proposal_status,
    )
    vacancy_id = application.vacancy.id
    _test_stage(su_client, stage, vacancy_id)
    _test_empty_stages(su_client, stage, vacancy_id)


@pytest.mark.parametrize('stage,interview_type', (
    (STAGES.invited_to_preliminary_interview, INTERVIEW_TYPES.screening),
    (STAGES.invited_to_onsite_interview, INTERVIEW_TYPES.regular),
    (STAGES.invited_to_final_interview, INTERVIEW_TYPES.final),
))
def test_dashboard_by_interview(su_client, stage, interview_type):
    application = f.ApplicationFactory(status=STATUSES.in_progress)
    f.InterviewFactory.create(
        application=application,
        consideration=application.consideration,
        type=interview_type,
    )
    vacancy_id = application.vacancy.id
    response_data = _test_stage(su_client, stage, vacancy_id)
    _test_empty_stages(su_client, stage, vacancy_id)

    interview = response_data['results'][0]['consideration']['interviews'][0]
    assert interview['application_id'] == application.id
    assert interview['vacancy_id'] == application.vacancy_id


def test_dashboard_filter_form(su_client):
    url = reverse('api:applications:dashboard-filter-form')
    response = su_client.get(url)
    assert response.status_code == 200
