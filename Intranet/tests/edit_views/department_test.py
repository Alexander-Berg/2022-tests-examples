import pytest
import json
import mock

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.person.models import Staff

from staff.lib.testing import (
    StaffFactory,
    DepartmentFactory,
    UserFactory,
    GroupFactory,
)


@pytest.mark.django_db()
def test_edit_department(client):
    ayd = mock.Mock(return_value=None)

    with mock.patch('staff.person.controllers.PersonCtl._perform_delayed_effects', new=ayd):
        test_person = StaffFactory(
            user=UserFactory(is_superuser=True),
            login=settings.AUTH_TEST_USER,
        )
        active_dep = DepartmentFactory(intranet_status=1)
        GroupFactory(url=active_dep.url, department=active_dep, intranet_status=1)
        disabled_dep = DepartmentFactory(intranet_status=0)

        def check_person():
            test_person = Staff.objects.get(login=settings.AUTH_TEST_USER)
            assert test_person.department_id == active_dep.id

        data = {'department': [
            {
                'department_id': str(active_dep.id),
            },
        ]}

        response = client.post(
            reverse('profile:edit-department', kwargs={'login': test_person.login}),
            json.dumps(data),
            content_type='application/json'
        )

        assert response.status_code == 200, response.content

        answer = json.loads(response.content)

        assert answer == {'target': {}}

        check_person()

        data = {'department': [
            {
                'department_id': str(disabled_dep.id),
            },
        ]}

        response = client.post(
            reverse('profile:edit-department', kwargs={'login': test_person.login}),
            json.dumps(data),
            content_type='application/json'
        )

        assert response.status_code == 200, response.content

        answer = json.loads(response.content)

        assert answer == {'errors': {'department': {
            '0': {'department_id': [{'error_key': 'staff-invalid_department_id'}]},
        }}}

        check_person()
