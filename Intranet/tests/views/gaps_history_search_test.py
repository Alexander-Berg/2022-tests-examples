from datetime import timedelta
import json

import pytest

from django.core.urlresolvers import reverse

from staff.gap.workflows.utils import find_workflow


@pytest.mark.django_db
def test_history_can_find_by_workflow(gap_test, client):
    AbsenceWorkflow = find_workflow('absence')
    IllnessWorkflow = find_workflow('illness')
    DutyWorkflow = find_workflow('duty')

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    date_from = base_gap['date_from']
    base_gap['date_from'] = base_gap['date_from'] + timedelta(days=7)
    base_gap['date_to'] = base_gap['date_to'] + timedelta(days=7)
    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    base_gap = gap_test.get_base_gap(IllnessWorkflow)
    base_gap['date_from'] = base_gap['date_from'] + timedelta(days=10)
    base_gap['date_to'] = base_gap['date_to'] + timedelta(days=10)
    base_gap['has_sicklist'] = True
    IllnessWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    base_gap = gap_test.get_base_gap(DutyWorkflow)
    base_gap['date_from'] = base_gap['date_from'] + timedelta(days=14)
    base_gap['date_to'] = base_gap['date_to'] + timedelta(days=14)
    DutyWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = '%s?login=%s&limit=10&page=1&workflow=absence&workflow=duty&date_from=%s' % (
        reverse('gap:gaps-history'),
        gap_test.test_person.login,
        date_from
    )

    response = client.get(url)
    assert response.status_code == 200

    data = json.loads(response.content)
    gaps = data['gaps']
    assert len(gaps) == 3


@pytest.mark.django_db
def test_history_will_not_find_anything_on_workflow_absence(gap_test, client):
    IllnessWorkflow = find_workflow('illness')

    base_gap = gap_test.get_base_gap(IllnessWorkflow)
    base_gap['date_from'] = base_gap['date_from'] + timedelta(days=10)
    base_gap['date_to'] = base_gap['date_to'] + timedelta(days=10)
    base_gap['has_sicklist'] = True
    IllnessWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = '%s?login=%s&limit=10&page=1&workflow=absence' % (
        reverse('gap:gaps-history'),
        gap_test.test_person.login,
    )

    response = client.get(url)
    assert response.status_code == 200

    data = json.loads(response.content)
    gaps = data['gaps']
    assert len(gaps) == 0
