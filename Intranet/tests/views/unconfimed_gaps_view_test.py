from datetime import timedelta
import json

import pytest

from django.core.urlresolvers import reverse

from staff.departments.models import DepartmentRoles
from staff.lib.testing import StaffFactory, DepartmentStaffFactory

from staff.gap.workflows.absence.workflow import AbsenceWorkflow
from staff.gap.workflows.duty.workflow import DutyWorkflow
from staff.gap.views.unconfirmed_gaps_view import unconfirmed_gaps


@pytest.fixture
def chief_person(gap_test, kinds):
    chief_person = StaffFactory(
        department=gap_test.dep_yandex,
        login='test_chief',
        uid='1120000000018265',
    )

    DepartmentStaffFactory(
        staff=chief_person,
        department=gap_test.dep_yandex,
        role_id=DepartmentRoles.CHIEF.value,
    )

    return chief_person


@pytest.mark.django_db
def test_long_absence_should_be_in_unconfirmed_list(gap_test, rf, chief_person, company):
    chief_person = company.persons['yandex-chief']
    chief_person.tz = 'UTC'
    chief_person.save()

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['dep12-person'].login,
        'person_id': company.persons['dep12-person'].id,
        'full_day': False,
    })

    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    ).new_gap(base_gap)

    request = rf.get(reverse('gap:unconfirmed-gaps'), {'all': 1})
    request.user = chief_person.user

    result = unconfirmed_gaps(request)

    gaps = json.loads(result.content)['gaps']
    assert len(gaps) == 1


@pytest.mark.django_db
def test_full_day_absence_should_be_in_unconfirmed_list(gap_test, rf, chief_person, company):
    chief_person = company.persons['yandex-chief']
    chief_person.tz = 'UTC'
    chief_person.save()

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap.update({
        'person_login': company.persons['dep12-person'].login,
        'person_id': company.persons['dep12-person'].id,
    })

    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        person_id=base_gap['person_id'],
    ).new_gap(base_gap)

    request = rf.get(reverse('gap:unconfirmed-gaps'), {'all': 1})
    request.user = chief_person.user

    result = unconfirmed_gaps(request)

    gaps = json.loads(result.content)['gaps']
    assert len(gaps) == 1


@pytest.mark.django_db
def test_short_absence_should_not_be_in_unconfirmed_list(gap_test, rf, chief_person):
    chief_person.tz = 'UTC'
    chief_person.save()

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    base_gap['full_day'] = False
    base_gap['date_to'] = base_gap['date_from'] + timedelta(hours=3)

    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    request = rf.get(reverse('gap:unconfirmed-gaps'), {'all': 1})
    request.user = chief_person.user

    result = unconfirmed_gaps(request)

    gaps = json.loads(result.content)['gaps']
    assert len(gaps) == 0


@pytest.mark.django_db
def test_duty_should_not_be_in_unconfirmed_list(gap_test, rf, chief_person):
    chief_person.tz = 'UTC'
    chief_person.save()

    base_gap = gap_test.get_base_gap(DutyWorkflow)
    base_gap['date_to'] = base_gap['date_from'] + timedelta(days=42)

    DutyWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    request = rf.get(reverse('gap:unconfirmed-gaps'), {'all': 1})
    request.user = chief_person.user

    result = unconfirmed_gaps(request)

    gaps = json.loads(result.content)['gaps']
    assert len(gaps) == 0
