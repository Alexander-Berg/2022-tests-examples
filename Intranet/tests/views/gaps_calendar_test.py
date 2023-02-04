import json
from datetime import timedelta

import mock
import pytest

from django.core.urlresolvers import reverse

from staff.gap.views.gaps_calendar_view import gaps_calendar
from staff.gap.workflows.utils import find_workflow


fake_calendar = mock.Mock(get=mock.Mock(side_effect=lambda *a, **b: []))


@mock.patch('staff.lib.calendar._calendar', fake_calendar)
@pytest.mark.django_db
def test_not_empty(gap_test, client):
    AbsenceWorkflow = find_workflow('absence')
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    date_from1_str = base_gap['date_from'].isoformat()

    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    base_gap['date_from'] = base_gap['date_from'] + timedelta(days=7)
    base_gap['date_to'] = base_gap['date_to'] + timedelta(days=7)
    date_to2_str = base_gap['date_to'].isoformat()
    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    base_gap['date_from'] = base_gap['date_from'] + timedelta(days=21)
    base_gap['date_to'] = base_gap['date_to'] + timedelta(days=21)
    date_from3_str = base_gap['date_from'].isoformat()
    date_to3_str = base_gap['date_to'].isoformat()
    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = '%s?l=%s&date_from=%s&date_to=%s' % (
        reverse('gap:gaps-calendar'),
        gap_test.test_person.login,
        date_from1_str,
        date_to2_str,
    )

    response = client.get(url)
    assert response.status_code == 200

    persons = json.loads(response.content)['persons']
    assert len(persons) == 1
    assert gap_test.test_person.login in persons
    gaps = persons[gap_test.test_person.login]['gaps']
    assert len(gaps) == 2

    url = '%s?l=%s&date_from=%s&date_to=%s' % (
        reverse('gap:gaps-calendar'),
        gap_test.test_person.login,
        date_from3_str,
        date_to3_str,
    )

    response = client.get(url)
    assert response.status_code == 200

    persons = json.loads(response.content)['persons']
    assert len(persons) == 1
    assert gap_test.test_person.login in persons
    gaps = persons[gap_test.test_person.login]['gaps']
    assert len(gaps) == 1


@pytest.mark.django_db
def test_not_empty0(gap_test, client):
    DutyWorkflow = find_workflow('duty')
    base_gap = gap_test.get_base_gap(DutyWorkflow)
    date_from1_str = base_gap['date_from'].isoformat()

    DutyWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    base_gap = gap_test.get_base_gap(DutyWorkflow)
    base_gap['date_from'] = base_gap['date_from'] + timedelta(days=7)
    base_gap['date_to'] = base_gap['date_to'] + timedelta(days=7)
    date_to2_str = base_gap['date_to'].isoformat()
    DutyWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    base_gap = gap_test.get_base_gap(DutyWorkflow)
    base_gap['date_from'] = base_gap['date_from'] + timedelta(days=21)
    base_gap['date_to'] = base_gap['date_to'] + timedelta(days=21)
    date_from3_str = base_gap['date_from'].isoformat()
    date_to3_str = base_gap['date_to'].isoformat()
    DutyWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = '%s?l=%s&date_from=%s&date_to=%s' % (
        reverse('gap:gaps-calendar'),
        gap_test.test_person.login,
        date_from1_str,
        date_to2_str,
    )

    response = client.get(url)
    assert response.status_code == 200

    persons = json.loads(response.content)['persons']
    assert len(persons) == 1
    assert gap_test.test_person.login in persons
    gaps = persons[gap_test.test_person.login]['gaps']
    assert len(gaps) == 2

    url = '%s?l=%s&date_from=%s&date_to=%s' % (
        reverse('gap:gaps-calendar'),
        gap_test.test_person.login,
        date_from3_str,
        date_to3_str,
    )

    response = client.get(url)
    assert response.status_code == 200

    persons = json.loads(response.content)['persons']
    assert len(persons) == 1
    assert gap_test.test_person.login in persons
    gaps = persons[gap_test.test_person.login]['gaps']
    assert len(gaps) == 1


def get_gap_calendar(rf, observer, login, gap):
    request = rf.get(
        reverse('gap:gaps-calendar'),
        {'l': [login], 'date_from': gap['date_from'], 'date_to':  gap['date_to']}
    )
    request.user = observer.user
    response = gaps_calendar(request)
    assert response.status_code == 200
    return json.loads(response.content)


@pytest.mark.django_db
def test_not_empty_external_self_without_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    gap = external_gap_case['external_gap']

    resp = get_gap_calendar(rf, observer, observer.login, gap)

    assert not resp


@pytest.mark.django_db
def test_not_empty_external_self_with_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    gap = external_gap_case['external_gap']
    external_gap_case['open_for_self']()

    resp = get_gap_calendar(rf, observer, observer.login, gap)

    assert len(resp['persons'][observer.login]['gaps']) == 1


@pytest.mark.django_db
def test_empty_external_without_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    gap = external_gap_case['inner_gap']
    login = external_gap_case['inner_person'].login

    resp = get_gap_calendar(rf, observer, login, gap)

    assert not resp


@pytest.mark.django_db
def test_not_empty_external_other_with_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    gap = external_gap_case['inner_gap']
    login = external_gap_case['inner_person'].login
    external_gap_case['create_permission']()

    resp = get_gap_calendar(rf, observer, login, gap)

    assert len(resp['persons'][login]['gaps']) == 1
