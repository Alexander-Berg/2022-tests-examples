from datetime import datetime
import json

import pytest

from django.core.urlresolvers import reverse

from staff.lib.decorators import custom_dumps
from staff.lib.testing import StaffFactory

from staff.gap.controllers.gap import GapCtl
from staff.gap.controllers.utils import full_day_dates
from staff.gap.workflows.absence.workflow import AbsenceWorkflow
from staff.gap.workflows.base_workflow import BaseWorkflow
from staff.gap.workflows.choices import GAP_STATES as GS
from staff.gap.workflows.illness.workflow import IllnessWorkflow
from staff.gap.workflows.learning.workflow import LearningWorkflow
from staff.gap.workflows.maternity.workflow import MaternityWorkflow
from staff.gap.workflows.vacation.workflow import VacationWorkflow
from staff.gap.workflows.duty.workflow import DutyWorkflow
from staff.gap.workflows.utils import find_workflow


def assert_base_create(gap_test, gap, base_create_gap):
    assert gap['id'] == gap_test.DEFAULT_GAP_ID
    assert gap['workflow'] == base_create_gap['workflow']
    assert gap['state'] == GS.NEW
    assert gap['full_day'] == base_create_gap['full_day']
    if base_create_gap['full_day']:
        full_day_dates(base_create_gap, 1)
    assert gap['date_from'] == base_create_gap['date_from']
    assert gap['date_to'] == base_create_gap['date_to']
    assert gap['work_in_absence'] == base_create_gap['work_in_absence']
    assert gap['comment'] == base_create_gap['comment']
    assert gap['to_notify'] == base_create_gap['to_notify']


def assert_base_edit(gap_test, gap, base_create_gap, base_edit_gap, check_fields=None):
    assert gap['id'] == gap_test.DEFAULT_GAP_ID
    assert gap['workflow'] == base_create_gap['workflow']
    assert GS.NEW == gap['state']

    check_fields = check_fields or BaseWorkflow.editable_fields

    for field_name in check_fields:
        if field_name == 'to_notify':
            assert set(gap['to_notify']) == set(base_edit_gap['to_notify'])
            continue

        if field_name == 'full_day' and base_edit_gap['full_day']:
            full_day_dates(base_edit_gap, 1)

        assert gap[field_name] == base_edit_gap[field_name]


@pytest.mark.django_db
def test_change_absence(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('absence'))

    base_create_gap.update({
        'date_from': datetime(2015, 1, 1, 10, 20),
        'date_to': datetime(2015, 1, 1, 11, 30),
        'full_day': False,
    })

    url = reverse('gap:api-gap-create')
    response = client.post(url, custom_dumps(base_create_gap), content_type='application/json')

    assert response.status_code == 200
    assert json.loads(response.content) == {'id': gap_test.DEFAULT_GAP_ID}

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_create(gap_test, gap, base_create_gap)

    base_edit_gap = gap_test.get_api_base_edit_gap(gap_test.DEFAULT_GAP_ID)
    base_edit_gap.update({
        'date_from': datetime(2015, 1, 1, 10, 20),
        'date_to': datetime(2015, 1, 1, 12, 30),
        'full_day': False,
    })

    url = reverse('gap:api-gap-edit')
    response = client.post(url, custom_dumps(base_edit_gap), content_type='application/json')

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_edit(gap_test, gap, base_create_gap, base_edit_gap, check_fields=AbsenceWorkflow.editable_fields)


@pytest.mark.django_db
def test_change_illness(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('illness'))
    base_create_gap['has_sicklist'] = False
    base_create_gap['is_covid'] = False
    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )

    assert response.status_code == 200
    assert json.loads(response.content) == {'id': gap_test.DEFAULT_GAP_ID}

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_create(gap_test, gap, base_create_gap)

    base_edit_gap = gap_test.get_api_base_edit_gap(gap_test.DEFAULT_GAP_ID)
    base_edit_gap['has_sicklist'] = True
    base_edit_gap['is_covid'] = False
    url = reverse('gap:api-gap-edit')
    response = client.post(
        url,
        custom_dumps(base_edit_gap),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_edit(gap_test, gap, base_create_gap, base_edit_gap, check_fields=IllnessWorkflow.editable_fields)
    assert gap['has_sicklist'] == base_edit_gap['has_sicklist']


@pytest.mark.django_db
def test_change_illness_is_covid(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('illness'))
    base_create_gap['has_sicklist'] = False
    base_create_gap['is_covid'] = True
    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )
    assert response.status_code == 200
    assert json.loads(response.content) == {'id': gap_test.DEFAULT_GAP_ID}

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_create(gap_test, gap, base_create_gap)
    assert gap['has_sicklist'] is False
    assert gap['is_covid'] is True

    base_edit_gap = gap_test.get_api_base_edit_gap(gap_test.DEFAULT_GAP_ID)
    base_edit_gap['has_sicklist'] = True
    base_edit_gap['is_covid'] = True
    url = reverse('gap:api-gap-edit')
    response = client.post(
        url,
        custom_dumps(base_edit_gap),
        content_type='application/json',
    )
    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_edit(gap_test, gap, base_create_gap, base_edit_gap, check_fields=IllnessWorkflow.editable_fields)
    assert gap['has_sicklist'] is True
    assert gap['is_covid'] is True


@pytest.mark.django_db
def test_change_illness_with_sicklist(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('illness'))
    base_create_gap['has_sicklist'] = True
    url = reverse('gap:api-gap-create')
    response = client.post(url, custom_dumps(base_create_gap), content_type='application/json')

    assert response.status_code == 200
    assert json.loads(response.content) == {'id': gap_test.DEFAULT_GAP_ID}

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_create(gap_test, gap, base_create_gap)

    base_edit_gap = gap_test.get_api_base_edit_gap(gap_test.DEFAULT_GAP_ID)
    base_edit_gap['has_sicklist'] = False
    url = reverse('gap:api-gap-edit')
    response = client.post(url, custom_dumps(base_edit_gap), content_type='application/json')

    assert response.status_code == 400  # Запрещено снимать галочку has_sicklist


@pytest.mark.django_db
def test_change_learning(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('learning'))
    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )

    assert response.status_code == 200
    assert json.loads(response.content) == {'id': gap_test.DEFAULT_GAP_ID}

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_create(gap_test, gap, base_create_gap)

    base_edit_gap = gap_test.get_api_base_edit_gap(gap_test.DEFAULT_GAP_ID)
    url = reverse('gap:api-gap-edit')
    response = client.post(
        url,
        custom_dumps(base_edit_gap),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_edit(gap_test, gap, base_create_gap, base_edit_gap, check_fields=LearningWorkflow.editable_fields)


@pytest.mark.django_db
def test_change_maternity(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('maternity'))
    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )

    assert response.status_code == 200
    assert json.loads(response.content) == {'id': gap_test.DEFAULT_GAP_ID}

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_create(gap_test, gap, base_create_gap)

    base_edit_gap = gap_test.get_api_base_edit_gap(gap_test.DEFAULT_GAP_ID)
    url = reverse('gap:api-gap-edit')
    response = client.post(
        url,
        custom_dumps(base_edit_gap),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_edit(gap_test, gap, base_create_gap, base_edit_gap, check_fields=MaternityWorkflow.editable_fields)


@pytest.mark.django_db
def test_change_vacation(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('vacation'))
    base_create_gap['is_selfpaid'] = False
    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )

    assert response.status_code == 200
    assert json.loads(response.content) == {'id': gap_test.DEFAULT_GAP_ID}

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_create(gap_test, gap, base_create_gap)

    base_edit_gap = gap_test.get_api_base_edit_gap(gap_test.DEFAULT_GAP_ID)
    base_edit_gap['is_selfpaid'] = True
    base_edit_gap['countries_to_visit'] = []
    url = reverse('gap:api-gap-edit')
    response = client.post(url, custom_dumps(base_edit_gap), content_type='application/json')

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_edit(gap_test, gap, base_create_gap, base_edit_gap, check_fields=VacationWorkflow.editable_fields)
    assert gap['is_selfpaid'] == base_edit_gap['is_selfpaid']


@pytest.mark.django_db
def test_change_duty(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('duty'))
    base_create_gap.update({
        'service_slug': 'femida',
        'service_name': 'femida',
        'shift_id': 41,
        'role_on_duty': 'duty',
    })
    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )

    assert response.status_code == 200
    assert json.loads(response.content) == {'id': gap_test.DEFAULT_GAP_ID}

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_create(gap_test, gap, base_create_gap)

    other_person = StaffFactory(login='other')

    base_edit_gap = gap_test.get_api_base_edit_gap(gap_test.DEFAULT_GAP_ID)
    base_edit_gap.update({
        'service_slug': 'staff',
        'service_name': 'staff',
        'shift_id': 42,
        'role_on_duty': 'god',
        'person_login': other_person.login,
        'person_id': other_person.id,
    })
    url = reverse('gap:api-gap-edit')
    response = client.post(
        url,
        custom_dumps(base_edit_gap),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert_base_edit(gap_test, gap, base_create_gap, base_edit_gap, check_fields=DutyWorkflow.editable_fields)
