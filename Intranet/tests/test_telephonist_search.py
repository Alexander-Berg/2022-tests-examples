import pytest
import json
from mock import patch
from django.core.urlresolvers import reverse


@pytest.fixture()
def callcenter_mode(settings):
    settings.ROOT_URLCONF = 'staff.callcenter_urls'


@pytest.mark.django_db
def test_telephonist_search_with_empty_query(company, superuser_client, callcenter_mode):
    url = reverse('telephonist-search')
    response = superuser_client.get(url)

    assert response.status_code == 400
    assert response.content == b'Empty search query'


FAKED_AVAILABILITIES_DATA = {
    'dep11-chief': {'is_available': True, 'seconds_before': 0, 'on_maternity': False},
    'dep11-person': {'is_available': True, 'seconds_before': 0, 'on_maternity': False},
}


class MockedTVM:
    def get_user_ticket_for_robot_staff(self):
        return 'some_user_ticket'


@pytest.mark.django_db
def test_telephonist_search(company, superuser_client, callcenter_mode):
    url = reverse('telephonist-search') + '?search_query=dep11-'
    with patch('staff.api.search_views.tvm2', MockedTVM()):
        with patch('staff.api.search_views.get_persons_availabilities', return_value=FAKED_AVAILABILITIES_DATA):
            response = superuser_client.get(url)

    assert response.status_code == 200
    result = json.loads(response.content)
    assert len(result) == 2
    for person_data in result:
        assert set(person_data.keys()) == {
            'first_name', 'last_name', 'middle_name', 'login', 'position',
            'office__name', 'join_at', 'co-workers', 'mobile_phone', 'telegram',
            'chief', 'availability', 'department'
        }
