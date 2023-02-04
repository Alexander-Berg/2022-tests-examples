import json

import pytest
import requests
from django.core.urlresolvers import reverse
from mock import patch, MagicMock

from staff.lib.testing import StaffFactory

from staff.person_profile.views.goals_view import goals


@pytest.fixture
def tester():
    return StaffFactory.build()


@pytest.fixture
def yauser():
    user = MagicMock()
    user.raw_user_ticket = MagicMock(return_value='mock_user_ticket')
    return user


@pytest.fixture
def get_goals(rf, tester, yauser):
    def func():
        request = rf.get(reverse('profile:goals', kwargs={'login': tester.login}))
        request.user = tester.user
        request.yauser = yauser
        request.service_is_readonly = False
        response = goals(request, tester.login)
        return json.loads(response.content)
    return func


@pytest.mark.django_db
@patch('staff.person_profile.views.goals_view.get_person_goals', return_value=[])
def test_empty_get(rf, tester, get_goals):
    answer = get_goals()
    assert answer == {'target': {'goals': []}}


@pytest.mark.django_db
@patch('staff.person_profile.views.goals_view.get_person_goals', side_effect=requests.Timeout)
def test_timeout_get(rf, tester, get_goals):
    answer = get_goals()
    assert answer == {}


@pytest.mark.django_db
@patch('staff.person_profile.views.goals_view.get_person_goals', side_effect=requests.HTTPError)
def test_error_get(rf, tester, get_goals):
    answer = get_goals()
    assert answer == {}


goals_mock_data = [{
    'id': 32167,
    'key': 'GOALZ-32167',
    'url': 'https://goals.yandex-team.ru/compilations/?goal=32167',
    'title': 'Conquer the Galactic Empire',
    'QDeadline': '3634Q7',
}]


@pytest.mark.django_db
@patch('staff.person_profile.views.goals_view.get_person_goals', return_value=goals_mock_data)
def test_passports_get(rf, tester, get_goals):
    answer = get_goals()

    assert answer == {'target': {'goals': goals_mock_data}}
