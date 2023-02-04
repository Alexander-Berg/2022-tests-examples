import pytest

from django.urls.base import reverse

from intranet.femida.src.interviews.choices import INTERVIEW_ROUND_STATUSES
from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_consideration_detail(su_client):
    consideration = f.ConsiderationFactory()

    valid_statuses = {
        INTERVIEW_ROUND_STATUSES.new,
        INTERVIEW_ROUND_STATUSES.planning,
    }
    draft_interview_fields = {'id', 'section', 'type', 'aa_type', 'is_code', 'vacancy'}
    interviews_count = 2

    for status, _ in INTERVIEW_ROUND_STATUSES:
        interview_round = f.InterviewRoundFactory(
            consideration=consideration,
            status=status,
        )
        f.InterviewFactory.create_batch(size=interviews_count, round=interview_round)

    url = reverse('api:considerations:detail', kwargs={'pk': consideration.pk})
    response = su_client.get(url)
    assert response.status_code == 200, response.content

    response_data = response.json()
    assert len(response_data['interview_rounds']) == len(valid_statuses)
    assert len(response_data['interview_rounds'][0]['interviews']) == interviews_count
    interview_data = response_data['interview_rounds'][0]['interviews'][0]
    assert not draft_interview_fields.symmetric_difference(interview_data)
