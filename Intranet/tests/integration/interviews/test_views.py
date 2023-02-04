import pytest

from unittest.mock import patch

from django.conf import settings
from django.urls.base import reverse
from django.utils import timezone

from intranet.femida.src.interviews.choices import (
    INTERVIEW_TYPES,
    AA_TYPES,
)

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import get_mocked_event, dt_to_str, shifted_now_str


pytestmark = pytest.mark.django_db


def test_interview_form_aa_robots(su_client):
    """
    Проверяем, что форма возвращает типы аа вместе с роботами
    """

    f.create_waffle_switch('disable_aa_architecture_type')

    interview = f.create_interview()
    candidate = interview.candidate
    url = reverse('api:interviews:create-form')
    params = {
        'candidate': candidate.id,
    }
    response = su_client.get(url, params)

    choices = response.json()['structure']['aa_type']['choices']

    assert len(choices) == len(settings.AA_TYPES) - 1  # -disable_aa_architecture_type
    assert 'robot' in choices[0]
    assert settings.AA_TYPES_ROBOTS.get(choices[0]['value'], None) == choices[0]['robot']


@patch('intranet.femida.src.interviews.controllers.update_event', lambda *args, **kwargs: None)
@patch('intranet.femida.src.api.interviews.forms.get_event', get_mocked_event)
def test_interview_aa_section_create_view(su_client):
    """
    Проверяем создание встречи с новыми полями для календаря
    """
    application = f.ApplicationFactory.create()
    interviewer = f.create_aa_interviewer(AA_TYPES.to)
    optional_participant = f.create_user()

    aa_calendar_data = {
        'optional_participant': optional_participant.username,
        'event_id': 2,
        'event_start_ts': dt_to_str(timezone.now()),
        'event_end_ts': shifted_now_str(hours=1),
        'event_resources': [],
        'event_others_can_view': 'true',
        'event_conference_url': 'https://zoom.com/qweasd',
    }
    data = {
        'section': 'Interview',
        'interviewer': interviewer.username,
        'type': INTERVIEW_TYPES.aa,
        "aa_type": AA_TYPES.to,
        "is_code": "true",
        'candidate': application.candidate_id,
        **aa_calendar_data
    }

    url = reverse('api:interviews:list')
    response = su_client.post(url, data)
    print(response.data)
    assert response.status_code == 201


@patch('intranet.femida.src.interviews.controllers.update_event', lambda *args, **kwargs: None)
@patch('intranet.femida.src.api.interviews.forms.get_event', get_mocked_event)
def test_interview_aa_section_update_view(su_client):
    """
    Проверяем обновление встречи с новыми полями для календаря
    """
    interview = f.create_interview(
        type=INTERVIEW_TYPES.aa,
        aa_type=AA_TYPES.to,
    )
    interviewer = f.create_aa_interviewer(AA_TYPES.to)
    optional_participant = f.create_user()
    params = {
        'section': 'Updated interview',
        'event_id': 47,
        'event_resources': [],
        'event_others_can_view': 'true',
        'event_conference_url': 'https://zoom.com/qweasd',
        'interviewer': interviewer.username,
        'optional_participant': optional_participant.username,
    }

    url = reverse('api:interviews:detail', kwargs={'pk': interview.id})
    response = su_client.put(url, params)

    assert response.status_code == 200
