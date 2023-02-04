import pytest
from mock import patch

from django.core.urlresolvers import reverse
from plan.idm.manager import Manager
from common import factories


def test_get_department(client):
    department = factories.DepartmentFactory()
    response = client.json.get(
        reverse(
            'api-frontend:staff-department-detail',
            args=(department.id,)
        ),
    )
    assert response.status_code == 200
    response = response.json()
    assert response['id'] == department.id
    assert response['name'] == department.name
    assert response['name_en'] == department.name_en


def test_get_staff(client):
    staff = factories.StaffFactory(is_frozen=True)
    response = client.json.get(
        reverse('api-v4:staff-person-list'),
        {'is_frozen': True}
    )
    assert response.status_code == 200
    response = response.json()
    assert len(response['results']) == 1
    data = response['results'][0]
    assert data['login'] == staff.login
    assert data['is_frozen'] is True


@pytest.mark.parametrize('has_perm', (True, False))
@pytest.mark.parametrize('by_login', (True, False))
def test_patch_staff(client, has_perm, by_login):
    staff = factories.StaffFactory(is_frozen=True)
    if has_perm:
        staff.user.is_superuser = True
        staff.user.save()
    client.login(staff.login)
    with patch('plan.staff.api.views.notify_staff_about_person') as mock_notify:
        with patch.object(Manager, 'set_is_frozen') as mock_set:
            arg = staff.login if by_login else staff.id
            response = client.json.patch(
                reverse('api-v4:staff-person-detail', args=(arg,)),
                {'is_frozen': False},
            )
    if not has_perm:
        assert response.status_code == 403
        mock_notify.delay.assert_not_called()
        mock_set.assert_not_called()
    else:
        assert response.status_code == 200
        mock_notify.delay.assert_called_once_with(staff_id=staff.id)
        mock_set.assert_called_once_with(login=staff.login, value=False)
    staff.refresh_from_db()
    assert staff.is_frozen is not has_perm


def test_get_department_403(only_view_client):
    department = factories.DepartmentFactory()
    response = only_view_client.json.get(
        reverse(
            'api-frontend:staff-department-detail',
            args=(department.id,)
        ),
    )
    assert response.status_code == 403
