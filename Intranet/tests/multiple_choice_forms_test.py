from datetime import date
import json

import pytest
from django.contrib.auth.models import Permission

from staff.preprofile.tests.utils import PreprofileFactory
from staff.preprofile.views import person_forms
from staff.preprofile.models.preprofile import PREPROFILE_STATUS as STATUS


def create_preprofiles(company, kinopoisk_org, yndx_org, edadeal_org,
                       red_rose_office, tel_aviv_office, sochi_office):
    # index < 2 - correct
    statuses = [STATUS.NEW, STATUS.READY, STATUS.PREPARED, STATUS.APPROVED]
    orgs = [kinopoisk_org, yndx_org, edadeal_org]
    offices = [red_rose_office, tel_aviv_office, sochi_office]
    for param in [
        (1, 1, 1),
        (0, 0, 0),
        (2, 0, 2),
        (3, 1, 1),
        (2, 2, 0),
        (0, 0, 2),
        (0, 2, 0),
        (1, 2, 2),
        (3, 1, 2),
        (1, 1, 2),
    ]:
        yield PreprofileFactory(
            department=company.yandex,
            first_name=('%d_%d_%d' % param),
            last_name='Test',
            status=statuses[param[0]],
            organization=orgs[param[1]],
            office=offices[param[2]],
            join_at=date.today(),
        )


@pytest.mark.django_db()
def test_multiple_choice(company, rf, tester, base_of_organizations_and_offices):
    fixture = base_of_organizations_and_offices
    profiles = list(create_preprofiles(company, **fixture.as_dict))

    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    part_url_with_status = '&status=%s&status=%s' % (STATUS.READY, 'neW')
    request = rf.get(fixture.as_url + part_url_with_status)
    request.user = tester

    result = person_forms(request)
    assert result.status_code == 200
    result = json.loads(result.content)

    ids = {form['id'] for form in result['result']}
    names = {form['first_name'] for form in result['result']}

    assert len(ids) == 2

    assert '0_0_0' in names
    assert profiles[0].id in ids

    assert '1_1_1' in names
    assert profiles[1].id in ids

    for preprofile in result['result']:
        assert 'first_name' in preprofile
        assert 'last_name' in preprofile
        assert 'middle_name' in preprofile
        assert 'created_at' in preprofile
        assert 'modified_at' in preprofile
        assert 'id' in preprofile
        assert 'login' in preprofile
        assert 'department_name' in preprofile
        assert 'department_url' in preprofile
        assert 'office_name' in preprofile
        assert 'organization_name' in preprofile
        assert 'position' in preprofile
        assert 'status' in preprofile
        assert 'city_name' in preprofile
        assert 'join_at' in preprofile
