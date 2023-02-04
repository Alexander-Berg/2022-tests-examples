import json
from datetime import timedelta

import pytest

from django.core.urlresolvers import reverse

from staff.lib.mongodb import mongo
from staff.lib.testing import StaffFactory, OfficeFactory

from staff.gap.tests.constants import GAPS_MONGO_COLLECTION
from staff.gap.views.gaps_by_day_views import gaps_by_id
from staff.gap.workflows.utils import find_workflow


@pytest.mark.django_db
def test_not_empty(gap_test, client):
    AbsenceWorkflow = find_workflow('absence')
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    base_gap = gap_test.get_base_gap(AbsenceWorkflow)
    base_gap['date_from'] = base_gap['date_from'] + timedelta(days=7)
    base_gap['date_to'] = base_gap['date_to'] + timedelta(days=7)

    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    mongo.db[GAPS_MONGO_COLLECTION].update(
        {'id': gap_test.DEFAULT_GAP_ID},
        {'$set': {'master_issue': 'VACANCY-123456'}},
    )

    url = reverse('gap:gaps-by-id', kwargs={'gap_id': gap_test.DEFAULT_GAP_ID})
    response = client.get(url)

    assert response.status_code == 200
    gaps = json.loads(response.content)['gaps']
    assert len(gaps) == 2
    assert gaps[0]['master_issue'] == 'VACANCY-123456'

    gap = gaps[0]
    assert 'gap_edit_url' in gap
    assert 'service_slug' not in gap
    assert 'service_name' not in gap
    assert 'shift_id' not in gap
    assert 'role_on_duty' not in gap


@pytest.mark.django_db
def test_duty_by_id(gap_test, client):
    DutyWorkflow = find_workflow('duty')
    base_gap = gap_test.get_base_gap(DutyWorkflow)
    DutyWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    base_gap = gap_test.get_base_gap(DutyWorkflow)
    base_gap['date_from'] = base_gap['date_from'] + timedelta(days=7)
    base_gap['date_to'] = base_gap['date_to'] + timedelta(days=7)

    DutyWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = reverse('gap:gaps-by-id', kwargs={'gap_id': gap_test.DEFAULT_GAP_ID})
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
def test_covid_illness_by_id(gap_test, client):
    workflow = find_workflow('illness')
    base_gap = gap_test.get_base_gap(workflow)
    gap_not_covid = workflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    base_gap = gap_test.get_base_gap(workflow)
    base_gap['date_from'] = base_gap['date_from'] + timedelta(days=7)
    base_gap['date_to'] = base_gap['date_to'] + timedelta(days=7)
    base_gap['is_covid'] = True
    gap_covid = workflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
    ).new_gap(base_gap)

    url = reverse('gap:gaps-by-id', kwargs={'gap_id': gap_not_covid['id']})
    response = client.get(url)
    assert response.status_code == 200
    gaps = json.loads(response.content)['gaps']
    assert len(gaps) == 1

    gap = gaps[0]
    assert 'is_covid' in gap
    assert not gap['is_covid']

    url = reverse('gap:gaps-by-id', kwargs={'gap_id': gap_covid['id']})
    response = client.get(url)
    assert response.status_code == 200
    gaps = json.loads(response.content)['gaps']
    assert len(gaps) == 1

    gap = gaps[0]
    assert 'is_covid' in gap
    assert gap['is_covid']


def get_gap_by_id(rf, observer, gap_id):
    request = rf.get(reverse('gap:gaps-by-id', kwargs={'gap_id': gap_id}))
    request.user = observer.user
    response = gaps_by_id(request, gap_id=gap_id)
    try:
        js = json.loads(response.content)
    except Exception:
        js = None
    return response.status_code, js


@pytest.mark.django_db
def test_not_empty_external_self_without_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']

    code, resp = get_gap_by_id(rf, observer, external_gap_case['external_gap']['id'])

    assert code == 403


@pytest.mark.django_db
def test_not_empty_external_self_with_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    external_gap_case['open_for_self']()

    code, resp = get_gap_by_id(rf, observer, external_gap_case['external_gap']['id'])

    assert code == 200
    assert len(resp['gaps']) == 1


@pytest.mark.django_db
def test_403_external_without_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']

    code, resp = get_gap_by_id(rf, observer, external_gap_case['inner_gap']['id'])

    assert code == 403


@pytest.mark.django_db
def test_not_empty_external_other_with_perm(external_gap_case, rf):
    observer = external_gap_case['external_person']
    external_gap_case['create_permission']()

    code, resp = get_gap_by_id(rf, observer, external_gap_case['inner_gap']['id'])

    assert code == 200
    assert len(resp['gaps']) == 1


@pytest.mark.django_db
def test_create_periodic_gap(gap_test, rf, client):
    observer = StaffFactory()
    office = OfficeFactory()
    url = reverse('gap:edit-gap', kwargs={'login': observer.login})
    data = {
        'workflow': 'office_work',
        'date_from': '2020-02-28T00:00:00.000Z',
        'date_to': '2020-02-28T00:00:00.000Z',
        'comment': '',
        'office': office.id,
        'full_day': True,
        'work_in_absence': True,
        'is_periodic_gap': True,
        'periodic_date_to': '2020-03-29T00:00:00.000Z',
        'periodic_type': 'week',
        'period': 1,
        'periodic_map_weekdays': ['3'],
    }

    response = client.post(
        url,
        json.dumps(data),
        content_type='application/json'
    )
    gap_id = json.loads(response.content)['id']

    code, resp = get_gap_by_id(rf, observer, gap_id)

    assert code == 200
    gaps = resp['gaps']
    assert len(gaps) == 1
    gap = gaps[0]
    assert gap['periodic_gap_id']
    assert gap['periodic_type'] == data['periodic_type']
    assert gap['period'] == data['period']
    assert gap['periodic_map_weekdays'] == data['periodic_map_weekdays']
