from datetime import timedelta
import json

import pytest

from django.core.urlresolvers import reverse

from staff.departments.models import DepartmentRoles
from staff.lib.testing import DepartmentStaffFactory, StaffFactory
from staff.gap.views.gaps_by_day_views import gaps_by_day
from staff.gap.workflows.utils import find_workflow


@pytest.mark.django_db
def test_not_empty(gap_test, client):
    AbsenceWorkflow = find_workflow('absence')
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    df = base_gap['date_from']
    day_date = '%s-%s-%s' % (df.year, df.month, df.day)

    url = '%s?login=%s&day_date=%s' % (
        reverse('gap:gaps-by-day'),
        gap_test.test_person.login,
        day_date,
    )

    response = client.get(url)

    assert response.status_code == 200

    gaps = json.loads(response.content)['gaps']
    assert len(gaps) == 1


def get_gap_by_date(rf, observer, person, day_date):
    request = rf.get(reverse('gap:gaps-by-day'))
    request.user = observer.user
    response = gaps_by_day(request, login=person.login, day_date=day_date)
    try:
        js = json.loads(response.content)
    except Exception:
        js = None
    return response.status_code, js


@pytest.mark.django_db
def test_not_empty_external_self_without_perm(external_gap_case, rf):
    person = external_gap_case['external_person']

    code, resp = get_gap_by_date(rf, person, person, external_gap_case['external_gap']['date_from'])

    assert code == 403


@pytest.mark.django_db
def test_not_empty_external_self_with_perm(external_gap_case, rf):
    person = external_gap_case['external_person']
    external_gap_case['open_for_self']()

    code, resp = get_gap_by_date(rf, person, person, external_gap_case['external_gap']['date_from'])

    assert code == 200
    assert len(resp['gaps']) == 1


@pytest.mark.django_db
def test_403_external_without_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    person = external_gap_case['inner_person']

    code, resp = get_gap_by_date(rf, observer, person, external_gap_case['inner_gap']['date_from'])

    assert code == 403


@pytest.mark.django_db
def test_not_empty_external_other_with_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    person = external_gap_case['inner_person']
    external_gap_case['create_permission']()

    code, resp = get_gap_by_date(rf, observer, person, external_gap_case['inner_gap']['date_from'])

    assert code == 200
    assert len(resp['gaps']) == 1


@pytest.mark.django_db
def test_empty(gap_test, client):
    AbsenceWorkflow = find_workflow('absence')
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    df = base_gap['date_from']
    day_date = '%s-%s-%s' % (df.year + 1, df.month, df.day)

    url = '%s?login=%s&day_date=%s' % (
        reverse('gap:gaps-by-day'),
        gap_test.test_person.login,
        day_date,
    )

    response = client.get(url)

    assert response.status_code == 200

    gaps = json.loads(response.content)['gaps']
    assert len(gaps) == 0


@pytest.mark.django_db
def test_vacation_workflow_should_contains_show_confirm_button_for_chief(gap_test, rf):
    chief_person = StaffFactory(
        department=gap_test.test_person.department,
        login='test_chief',
        uid='1120000000018265',
        tz='UTC',
    )

    DepartmentStaffFactory(
        staff=chief_person,
        department=gap_test.test_person.department,
        role_id=DepartmentRoles.CHIEF.value
    )

    VacationWorkflow = find_workflow('vacation')
    base_gap = gap_test.get_base_gap(VacationWorkflow)

    VacationWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    request = rf.get(reverse('gap:gaps-by-day'))
    request.user = chief_person.user

    response = gaps_by_day(request, login=gap_test.test_person.login, day_date=base_gap['date_from'])

    assert response.status_code == 200

    gaps = json.loads(response.content)['gaps']
    assert len(gaps) == 1
    assert gaps[0]['show_confirm_button']


@pytest.mark.django_db
def test_short_absence_workflow_should_not_contains_show_confirm_button_for_chief(gap_test, rf):
    chief_person = StaffFactory(
        department=gap_test.test_person.department,
        login='test_chief',
        uid='1120000000018265',
        tz='UTC',
    )
    DepartmentStaffFactory(
        staff=chief_person,
        department=gap_test.test_person.department,
        role_id=DepartmentRoles.CHIEF.value,
    )

    AbsenceWorkflow = find_workflow('absence')
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap['full_day'] = False
    base_gap['date_to'] = base_gap['date_from'] + timedelta(hours=3)

    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    request = rf.get(reverse('gap:gaps-by-day'))
    request.user = chief_person.user

    response = gaps_by_day(request, login=gap_test.test_person.login, day_date=base_gap['date_from'])

    assert response.status_code == 200

    gaps = json.loads(response.content)['gaps']
    assert len(gaps) == 1
    assert not gaps[0]['show_confirm_button']


@pytest.mark.django_db
def test_long_absence_should_contains_show_confirm_button_for_chief(gap_test, rf):
    chief_person = StaffFactory(
        department=gap_test.test_person.department,
        login='test_chief',
        uid='1120000000018265',
        tz='UTC',
    )

    chief_person = gap_test.test_person.department.departmentstaff_set.get(role_id=DepartmentRoles.CHIEF.value).staff

    AbsenceWorkflow = find_workflow('absence')
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    workflow = AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    )
    workflow.LONG_ABSENCES_WO_APPROVAL = 0
    workflow.new_gap(base_gap)

    request = rf.get(reverse('gap:gaps-by-day'))
    request.user = chief_person.user

    response = gaps_by_day(request, login=gap_test.test_person.login, day_date=base_gap['date_from'])

    assert response.status_code == 200

    gaps = json.loads(response.content)['gaps']
    assert len(gaps) == 1
    assert gaps[0]['show_confirm_button']


@pytest.mark.django_db
def test_illness_workflow_should_contains_show_confirm_button_for_chief(gap_test, rf):
    chief_person = StaffFactory(
        department=gap_test.test_person.department,
        login='test_chief',
        uid='1120000000018265',
        tz='UTC',
    )

    DepartmentStaffFactory(
        staff=chief_person,
        department=gap_test.test_person.department,
        role_id=DepartmentRoles.CHIEF.value,
    )

    IllnessWorkflow = find_workflow('illness')
    base_gap = gap_test.get_base_gap(IllnessWorkflow)

    IllnessWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    request = rf.get(reverse('gap:gaps-by-day'))
    request.user = chief_person.user

    response = gaps_by_day(request, login=gap_test.test_person.login, day_date=base_gap['date_from'])

    assert response.status_code == 200

    gaps = json.loads(response.content)['gaps']
    assert len(gaps) == 1
    assert not gaps[0]['show_confirm_button']


@pytest.mark.django_db
def test_duty_workflow_should_contains_special_fields(gap_test, client):
    DutyWorkflow = find_workflow('duty')
    base_gap = gap_test.get_base_gap(DutyWorkflow)

    DutyWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    df = base_gap['date_from']
    day_date = '%s-%s-%s' % (df.year, df.month, df.day)

    url = '%s?login=%s&day_date=%s' % (
        reverse('gap:gaps-by-day'),
        gap_test.test_person.login,
        day_date,
    )

    response = client.get(url)

    assert response.status_code == 200

    gaps = json.loads(response.content)['gaps']
    assert len(gaps) == 1

    gap = gaps[0]

    assert 'gap_edit_url' not in gap
    assert 'service_slug' in gap
    assert 'service_name' in gap
    assert 'shift_id' in gap
    assert 'role_on_duty' in gap


@pytest.mark.django_db
def test_covid_illness_workflow_should_contains_special_fields(gap_test, client):
    workflow = find_workflow('illness')
    base_gap = gap_test.get_base_gap(workflow)
    workflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    base_gap_2 = gap_test.get_base_gap(workflow)
    base_gap_2['is_covid'] = True
    workflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap_2)

    df = base_gap['date_from']
    day_date = '%s-%s-%s' % (df.year, df.month, df.day)

    url = '%s?login=%s&day_date=%s' % (
        reverse('gap:gaps-by-day'),
        gap_test.test_person.login,
        day_date,
    )

    response = client.get(url)
    assert response.status_code == 200

    gaps = json.loads(response.content)['gaps']
    assert len(gaps) == 2

    assert all('is_covid' in gap for gap in gaps)
    gaps.sort(key=lambda x: x['is_covid'])
    assert not gaps[0]['is_covid']
    assert gaps[1]['is_covid']
