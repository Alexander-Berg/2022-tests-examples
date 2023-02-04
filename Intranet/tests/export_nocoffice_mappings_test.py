import pytest
import json

from django.conf import settings
from django.core.urlresolvers import reverse
from django.contrib.auth.models import Permission

from staff.lib.testing import StaffFactory
from staff.whistlah.views import racktables_office_ids
from staff.whistlah.tests.factories import NOCOfficeFactory


@pytest.fixture
def tester():
    tester = StaffFactory(login='tester')
    return tester.user


@pytest.mark.django_db
def test_racktables_office_ids_with_permission(rf, tester):
    NOCOfficeFactory(office_id=1, noc_office_id=10),
    NOCOfficeFactory(office_id=2, noc_office_id=21),
    NOCOfficeFactory(office_id=2, noc_office_id=22),

    request = rf.get(reverse('whistlah:racktables_office_ids'))
    request.user = tester

    tester.user_permissions.add(
        Permission.objects.get(codename='can_export_nocoffice_mapping')
    )

    response = racktables_office_ids(request)
    assert response.status_code == 200

    response_data = json.loads(response.content)
    assert response_data == {'1': [10], '2': [21, 22]}


@pytest.mark.django_db
def test_racktables_office_ids_without_permission(rf, tester):
    request = rf.get(reverse('whistlah:racktables_office_ids'))
    request.user = tester

    response = racktables_office_ids(request)
    assert response.status_code == 302
    assert settings.LOGIN_URL in response._headers['location'][1]
