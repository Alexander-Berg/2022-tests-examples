import pytest

from django.urls.base import reverse

from intranet.femida.src.interviews.choices import INTERVIEW_STATES, INTERVIEW_ROUND_STATUSES
from intranet.femida.tests import factories as f

pytestmark = pytest.mark.django_db


def test_interview_action(su_client):
    interview_round = f.InterviewRoundFactory()
    f.InterviewFactory.create_batch(size=3, round=interview_round, state=INTERVIEW_STATES.draft)

    view_name = 'api:interview-rounds:cancel'
    url = reverse(view_name, kwargs={'pk': interview_round.id})
    response = su_client.post(url)

    interview_round.refresh_from_db()
    assert response.status_code == 200, response.content
    assert interview_round.status == INTERVIEW_ROUND_STATUSES.cancelled
    for interview in interview_round.interviews.all():
        assert interview.state == INTERVIEW_STATES.cancelled
