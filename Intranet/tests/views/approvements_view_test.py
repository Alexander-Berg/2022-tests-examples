import json
import pytest

from itertools import chain, count, product
from mock import patch, Mock, MagicMock

from django.core.urlresolvers import reverse

from staff.departments.models import DepartmentRoles
from staff.gap.workflows.utils import find_workflow
from staff.lib.auth.utils import get_or_create_test_user
from staff.lib.testing import (
    StaffFactory,
    DepartmentFactory,
    DepartmentStaffFactory,
)
from staff.lib.tests.blackbox import FakeResponse
from staff.person_profile.views.approvements_view import gap_newhire_counts
from staff.preprofile.models import PREPROFILE_STATUS, FORM_TYPE
from staff.preprofile.tests.utils import PreprofileFactory


def create_chief_department(chief_person):
    department = DepartmentFactory()
    chief_person.department = department
    chief_person.save()
    DepartmentStaffFactory(
        staff=chief_person,
        department=department,
        role_id=DepartmentRoles.CHIEF.value,
    )
    return department


def create_subordinate_gap(gap_test, company, department):
    subordinate_person = StaffFactory(department=department, office=company['offices']['KR'])

    VacationWorkflow = find_workflow('vacation')
    base_gap = gap_test.get_base_gap(VacationWorkflow)
    base_gap.update({
        'person_login': subordinate_person.login,
        'person_id': subordinate_person.id,
    })
    gap = VacationWorkflow.init_to_new(
        modifier_id=gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    )
    return gap.new_gap(base_gap)


def create_preprofiles(department):
    login_gen = ('login_{}'.format(i) for i in count())
    ruled_deps = [department]
    not_ruled_deps = [DepartmentFactory(parent=department), DepartmentFactory()]
    right_kwargs = [
        dict(status=PREPROFILE_STATUS.NEW, form_type=FORM_TYPE.EMPLOYEE),
        dict(status=PREPROFILE_STATUS.PREPARED, form_type=FORM_TYPE.EMPLOYEE),
        dict(status=PREPROFILE_STATUS.NEW, form_type=FORM_TYPE.MONEY),
    ]
    not_right_kwargs = [
        dict(status=PREPROFILE_STATUS.APPROVED, form_type=FORM_TYPE.EMPLOYEE),
    ]
    expected = product(ruled_deps, right_kwargs)
    not_expected = chain(
        product(ruled_deps, not_right_kwargs),
        product(not_ruled_deps, right_kwargs),
    )
    for (dep, kwargs), login in zip(not_expected, login_gen):
        PreprofileFactory(department_id=dep.id, login=login, last_name=login, **kwargs)

    return [
        PreprofileFactory(department_id=dep.id, login=login, last_name=login, **kwargs)
        for (dep, kwargs), login in zip(expected, login_gen)
    ]


@pytest.mark.parametrize('response_data, ok_count', (
    ({'current_count': 3}, 3),
    ({}, 0),
))
@patch('staff.person_profile.views.approvements_view.tvm2.get_tvm_ticket_by_deploy')
@pytest.mark.django_db
def test_approvements(mock, client, gap_test, company, response_data, ok_count):
    result = {
        'approvements': {
            'ok': ok_count,
            'gap': 1,
            'newhire': 3,
        },
    }

    chief_user = get_or_create_test_user()
    chief_person = chief_user.staff

    # gap
    department = create_chief_department(chief_person)
    create_subordinate_gap(gap_test, company, department)

    # newhire
    create_preprofiles(department)

    url = reverse('profile:approvements', kwargs={'login': chief_person.login})
    client.login(user=chief_user)

    with patch(
        'staff.person_profile.views.approvements_view.requests.get',
        return_value=FakeResponse(response_data),
    ):
        response = client.get(url)
    assert response.status_code == 200
    response_data = json.loads(response.content)
    assert response_data['target'] == result


@patch('staff.lib.decorators._check_service_id', lambda *a, **b: True)
@pytest.mark.django_db
def test_gap_newhire_counts(rf):
    newhire_count = 3
    gap_count = 2

    newhire_count_mock = Mock(return_value=newhire_count)
    gap_count_mock = Mock(return_value=gap_count)

    request = rf.get(reverse('profile:gap_newhire_counts'))
    user = get_or_create_test_user()
    person = user.get_profile()
    request.user = user
    request.yauser = MagicMock()
    request.yauser.raw_user_ticket = MagicMock(return_value='mock_user_ticket')

    with patch('staff.person_profile.views.approvements_view.get_newhire_approvements_count', newhire_count_mock):
        with patch('staff.person_profile.views.approvements_view.get_gap_approvements_count', gap_count_mock):
            response = gap_newhire_counts(request)

    newhire_count_mock.assert_called_once_with(person)
    gap_count_mock.assert_called_once_with(person.id)

    assert response.status_code == 200
    response_data = json.loads(response.content)
    assert response_data['newhire'] == newhire_count
    assert response_data['gap'] == gap_count
