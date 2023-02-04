import json

import pytest

from django.core.urlresolvers import reverse

from staff.gap.workflows.utils import find_workflow


@pytest.mark.django_db
def test_absence_response_contains_required_fields(gap_test, client):
    AbsenceWorkflow = find_workflow('absence')

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    date_from = base_gap['date_from']

    url = '%s?login=%s&limit=10&page=1&workflow=absence&date_from=%s' % (
        reverse('gap:gaps-history'),
        gap_test.test_person.login,
        date_from
    )

    gap = json.loads(client.get(url).content)['gaps'][0]

    assert 'id' in gap
    assert 'workflow' in gap
    assert 'date_from' in gap
    assert 'date_to' in gap
    assert 'comment' in gap
    assert 'state' in gap
    assert 'full_day' in gap
    assert 'work_in_absence' in gap
    assert 'created_at' in gap

    assert 'service_slug' not in gap
    assert 'service_name' not in gap
    assert 'shift_id' not in gap
    assert 'role_on_duty' not in gap

    assert 'is_covid' not in gap


@pytest.mark.django_db
def test_duty_response_contains_required_fields(gap_test, client):
    DutyWorkflow = find_workflow('duty')

    base_gap = gap_test.get_base_gap(DutyWorkflow)
    DutyWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    date_from = base_gap['date_from']

    url = '%s?login=%s&limit=10&page=1&workflow=duty&date_from=%s' % (
        reverse('gap:gaps-history'),
        gap_test.test_person.login,
        date_from
    )

    gap = json.loads(client.get(url).content)['gaps'][0]

    assert 'id' in gap
    assert 'workflow' in gap
    assert 'date_from' in gap
    assert 'date_to' in gap
    assert 'comment' in gap
    assert 'state' in gap
    assert 'full_day' in gap
    assert 'work_in_absence' in gap
    assert 'created_at' in gap

    assert 'service_slug' in gap
    assert 'service_name' in gap
    assert 'shift_id' in gap
    assert 'role_on_duty' in gap

    assert 'is_covid' not in gap


@pytest.mark.django_db
def test_covid_illness_response_contains_required_fields(gap_test, client):
    IllnessWorkflow = find_workflow('illness')

    base_gap = gap_test.get_base_gap(IllnessWorkflow)
    IllnessWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    date_from = base_gap['date_from']

    url = '%s?login=%s&limit=10&page=1&workflow=illness&date_from=%s' % (
        reverse('gap:gaps-history'),
        gap_test.test_person.login,
        date_from
    )

    gap = json.loads(client.get(url).content)['gaps'][0]

    assert 'id' in gap
    assert 'workflow' in gap
    assert 'date_from' in gap
    assert 'date_to' in gap
    assert 'comment' in gap
    assert 'state' in gap
    assert 'full_day' in gap
    assert 'work_in_absence' in gap
    assert 'created_at' in gap

    assert 'service_slug' not in gap
    assert 'service_name' not in gap
    assert 'shift_id' not in gap
    assert 'role_on_duty' not in gap

    assert 'is_covid' in gap
