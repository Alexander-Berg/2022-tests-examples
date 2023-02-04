import pytest
from unittest.mock import patch

from django.urls import reverse

from ok.approvements.choices import APPROVEMENT_STATUSES
from ok.utils.strings import str_to_md5
from tests.factories import ApprovementFactory
from tests.integration.api.mocks import _mock_get_staff_iter_500, _mock_group_members

pytestmark = pytest.mark.django_db


check_url = reverse('api:approvements:check')
check_data = (
    {
        'author': 'author',
        'groups': [
            'group_url1',
            'group_url2',
        ],
        'uid': 'FEB-07',
        'object_id': 'JOB-123',
    },
)


def test_invalid_data(client):
    response = client.get(check_url)
    assert response.status_code == 400
    errors = response.json()['errors']
    assert sorted(errors.keys()) == ['author', 'object_id', 'uid']


@patch('ok.api.approvements.views.get_staff_group_member_logins', _mock_group_members)
@pytest.mark.parametrize('data', check_data)
@pytest.mark.parametrize('current_user,expected_state', (
    ('author', 'can_create'),
    ('group_url1_member', 'can_create'),
    ('no_author', 'error_approvement_creating'),
))
def test_new_approvement(client, current_user, expected_state, data):
    client.force_authenticate(current_user)
    response = client.get(check_url, data)

    assert response.status_code == 200
    data = response.json()
    assert data['id'] is None
    assert data['state'] == expected_state


@patch('ok.api.approvements.views.get_staff_group_member_logins', _mock_group_members)
@pytest.mark.parametrize('access_data,status_code', (
    ({'author': 'author'}, 200),
    ({'groups': ['group_url']}, 200),
    ({}, 400),
))
def test_access_data_required(client, access_data, status_code):
    data = {
        'uid': 'FEB-07',
        'object_id': 'JOB-123',
        **access_data
    }
    response = client.get(check_url, data)
    assert response.status_code == status_code


@patch('ok.utils.staff.get_staff_iter', _mock_get_staff_iter_500)
@pytest.mark.parametrize('data', check_data)
def test_check_200_on_staff_500(client, data):
    """
    Ошибка получения дополнительных ответственных по группам должна обрабатываться тихо.
    Должен браться единственный ответственный: автор.
    """
    client.force_authenticate('author')
    response = client.get(check_url, data)

    assert response.status_code == 200
    data = response.json()
    assert data['state'] == 'can_create'


@pytest.mark.parametrize('approvement_status,expected_state', (
    (APPROVEMENT_STATUSES.in_progress, 'error_active_approvement_exists'),
    (APPROVEMENT_STATUSES.suspended, 'error_active_approvement_exists'),
    (APPROVEMENT_STATUSES.rejected, 'error_active_approvement_exists'),
    (APPROVEMENT_STATUSES.closed, 'can_create'),
))
def test_create_if_another_approvemet_exists(client, approvement_status, expected_state):
    approvement = ApprovementFactory(status=approvement_status)

    author_login = 'author'
    params = {
        'author': author_login,
        'object_id': approvement.object_id,
        'uid': 'new_uid'
    }
    client.force_authenticate(author_login)
    response = client.get(check_url, params)

    assert response.status_code == 200
    data = response.json()
    assert data['id'] is None
    assert data['state'] == expected_state


@pytest.mark.parametrize('approvement_status', (
    APPROVEMENT_STATUSES.in_progress,
    APPROVEMENT_STATUSES.closed,
))
@pytest.mark.parametrize('current_user', ('author', 'no_author'))
def test_show_existing_approvement(client, approvement_status, current_user):
    uid = 'uid1'
    approvement = ApprovementFactory(
        status=approvement_status,
        author='author',
        uid=str_to_md5(uid)
    )

    params = {
        'author': approvement.author,
        'object_id': approvement.object_id,
        'uid': uid
    }
    client.force_authenticate(current_user)
    response = client.get(check_url, params)

    assert response.status_code == 200
    data = response.json()
    assert data['id'] is approvement.id
    assert data['uuid'] == str(approvement.uuid)
    assert data['state'] == 'exists'
