import json

import pytest

from django.core.urlresolvers import reverse

from staff.gap.workflows.choices import GAP_STATES
from staff.gap.workflows.utils import find_workflow


@pytest.mark.django_db
def test_all(gap_test, client):
    AbsenceWorkflow = find_workflow('absence')
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = reverse('gap:api-gap-find', kwargs={'gap_id': 1})
    response = client.get(url)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'id': 1,
        'workflow': AbsenceWorkflow.workflow,
        'person_login': gap_test.test_person.login,
        'work_in_absence': True,
        'date_from': '2015-01-01T10:20:00',
        'date_to': '2015-01-02T11:30:00',
        'full_day': True,
        'comment': '',
        'to_notify': [],
        'state': GAP_STATES.NEW,
    }

    IllnessWorkflow = find_workflow('illness')
    base_gap = gap_test.get_base_gap(IllnessWorkflow)
    base_gap['has_sicklist'] = True
    base_gap['is_covid'] = True
    IllnessWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = reverse('gap:api-gap-find', kwargs={'gap_id': 2})
    response = client.get(url)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'id': 2,
        'workflow': IllnessWorkflow.workflow,
        'person_login': gap_test.test_person.login,
        'work_in_absence': True,
        'has_sicklist': True,
        'is_covid': True,
        'date_from': '2015-01-01T10:20:00',
        'date_to': '2015-01-02T11:30:00',
        'full_day': True,
        'comment': '',
        'to_notify': [],
        'state': GAP_STATES.NEW,
    }

    LearningWorkflow = find_workflow('learning')
    base_gap = gap_test.get_base_gap(LearningWorkflow)
    LearningWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = reverse('gap:api-gap-find', kwargs={'gap_id': 3})
    response = client.get(url)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'id': 3,
        'workflow': LearningWorkflow.workflow,
        'person_login': gap_test.test_person.login,
        'work_in_absence': True,
        'date_from': '2015-01-01T10:20:00',
        'date_to': '2015-01-02T11:30:00',
        'full_day': True,
        'comment': '',
        'to_notify': [],
        'state': GAP_STATES.NEW,
    }

    MaternityWorkflow = find_workflow('maternity')
    base_gap = gap_test.get_base_gap(MaternityWorkflow)
    MaternityWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = reverse('gap:api-gap-find', kwargs={'gap_id': 4})
    response = client.get(url)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'id': 4,
        'workflow': MaternityWorkflow.workflow,
        'person_login': gap_test.test_person.login,
        'work_in_absence': True,
        'date_from': '2015-01-01T10:20:00',
        'date_to': '2015-01-02T11:30:00',
        'full_day': True,
        'comment': '',
        'to_notify': [],
        'state': GAP_STATES.NEW,
    }

    VacationWorkflow = find_workflow('vacation')
    base_gap = gap_test.get_base_gap(VacationWorkflow)
    base_gap['is_selfpaid'] = True
    VacationWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = reverse('gap:api-gap-find', kwargs={'gap_id': 5})
    response = client.get(url)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'id': 5,
        'workflow': VacationWorkflow.workflow,
        'person_login': gap_test.test_person.login,
        'work_in_absence': True,
        'is_selfpaid': True,
        'date_from': '2015-01-01T10:20:00',
        'date_to': '2015-01-02T11:30:00',
        'full_day': True,
        'comment': '',
        'to_notify': [],
        'state': GAP_STATES.NEW,
        'mandatory': False,
        'vacation_updated': False,
        'deadline': None,
        'geo_id': 0,
    }

    DutyWorkflow = find_workflow('duty')
    base_gap = gap_test.get_base_gap(DutyWorkflow)
    base_gap.update({
        'service_slug': 'staff',
        'service_name': 'staff',
        'shift_id': 42,
        'role_on_duty': 'duty',
    })
    DutyWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = reverse('gap:api-gap-find', kwargs={'gap_id': 6})
    response = client.get(url)
    assert response.status_code == 200
    assert json.loads(response.content) == {
        'id': 6,
        'workflow': DutyWorkflow.workflow,
        'person_login': gap_test.test_person.login,
        'work_in_absence': True,
        'date_from': '2015-01-01T10:20:00',
        'date_to': '2015-01-02T11:30:00',
        'full_day': True,
        'comment': '',
        'to_notify': [],
        'service_slug': 'staff',
        'service_name': 'staff',
        'shift_id': 42,
        'role_on_duty': 'duty',
        'state': GAP_STATES.NEW,
    }
