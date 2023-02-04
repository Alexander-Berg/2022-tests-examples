import json
from datetime import datetime

import pytest

from django.core.urlresolvers import reverse

from staff.lib.decorators import custom_dumps
from staff.map.models import TableBook

from staff.gap.controllers.gap import GapCtl
from staff.gap.workflows.choices import GAP_STATES as GS
from staff.gap.workflows.utils import find_workflow


def change_state(gap_test, client, action_name, new_state):
    url = reverse('gap:api-gap-%s' % action_name,
                  kwargs={'gap_id': gap_test.DEFAULT_GAP_ID})
    response = client.post(url, content_type='application/json')

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert gap['state'] == new_state


'''
Absence
'''
@pytest.mark.django_db
def test_absence_new_canceled(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('absence'))

    base_create_gap.update({
        'date_from': datetime(2015, 1, 1, 10, 20),
        'date_to': datetime(2015, 1, 1, 11, 30),
        'full_day': False,
    })

    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert gap['state'] == GS.NEW

    change_state(gap_test, client, 'cancel', GS.CANCELED)


@pytest.mark.django_db
def test_illness_new_canceled(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('illness'))
    base_create_gap['has_sicklist'] = True

    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert gap['state'] == GS.NEW

    change_state(gap_test, client, 'cancel', GS.CANCELED)


@pytest.mark.django_db
def test_illness_new_signed_canceled(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('illness'))
    base_create_gap['has_sicklist'] = True

    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert gap['state'] == GS.NEW

    change_state(gap_test, client, 'sign', GS.SIGNED)
    change_state(gap_test, client, 'cancel', GS.CANCELED)


@pytest.mark.django_db
def test_learning_new_canceled(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('learning'))

    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert gap['state'] == GS.NEW

    change_state(gap_test, client, 'cancel', GS.CANCELED)


@pytest.mark.django_db
def test_maternity_new_canceled(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('maternity'))

    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert gap['state'] == GS.NEW

    change_state(gap_test, client, 'cancel', GS.CANCELED)


@pytest.mark.django_db
def test_vacation_new_canceled(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('vacation'))
    base_create_gap['is_selfpaid'] = True

    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert gap['state'] == GS.NEW

    change_state(gap_test, client, 'cancel', GS.CANCELED)


@pytest.mark.django_db
def test_vacation_new_confirmed_cancel(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('vacation'))
    base_create_gap['is_selfpaid'] = True

    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert gap['state'] == GS.NEW

    change_state(gap_test, client, 'confirm', GS.CONFIRMED)
    change_state(gap_test, client, 'cancel', GS.CANCELED)


@pytest.mark.django_db
def test_vacation_new_confirmed_signed_cancel(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('vacation'))
    base_create_gap['is_selfpaid'] = True

    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert gap['state'] == GS.NEW

    change_state(gap_test, client, 'confirm', GS.CONFIRMED)
    change_state(gap_test, client, 'sign', GS.SIGNED)
    change_state(gap_test, client, 'cancel', GS.CANCELED)


@pytest.mark.django_db
def test_duty_new_cancel(gap_test, client):
    base_create_gap = gap_test.get_api_base_create_gap(find_workflow('duty'))

    # create duty
    url = reverse('gap:api-gap-create')
    response = client.post(
        url,
        custom_dumps(base_create_gap),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert gap['state'] == GS.NEW

    # cancel duty
    url = reverse('gap:api-gap-cancel', kwargs={'gap_id': gap_test.DEFAULT_GAP_ID})
    response = client.post(
        url,
        b'',
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_extended(query={'id': gap_test.DEFAULT_GAP_ID})
    assert gap is None


@pytest.mark.django_db
@pytest.mark.parametrize('workflow', ('office_work', 'remote_work'))
def test_gaps_with_period_cancel(gap_test, client, workflow):
    base_gap = gap_test.get_base_periodic_gap(find_workflow(workflow))

    find_workflow(workflow)(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
        None,
    ).new_periodic_gap(base_gap)

    gap = GapCtl().find_gap_by_id(gap_test.DEFAULT_GAP_ID)
    assert gap['state'] == GS.NEW

    # cancel all gaps
    url = reverse('gap:api-gap-cancel', kwargs={'gap_id': gap_test.DEFAULT_GAP_ID})
    response = client.post(
        url,
        json.dumps({"periodic_gap_id": 1}),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_extended(query={'periodic_gap_id': 1, 'state': GS.NEW})
    assert gap is None


@pytest.mark.django_db
def test_office_work_with_period_cancel(gap_test, client):
    base_gap = gap_test.get_base_periodic_gap(find_workflow('office_work'))

    find_workflow('office_work')(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_test.test_person.id,
        None,
    ).new_periodic_gap(base_gap)

    gap_ids = [gap['id'] for gap in GapCtl().find_gaps(query={'periodic_gap_id': 1, 'state': GS.NEW})]
    assert gap_ids

    table_book = TableBook.objects.filter(gap_id__in=gap_ids)
    assert table_book

    # cancel all gaps
    url = reverse('gap:api-gap-cancel', kwargs={'gap_id': gap_test.DEFAULT_GAP_ID})
    response = client.post(
        url,
        json.dumps({"periodic_gap_id": 1}),
        content_type='application/json',
    )

    assert response.status_code == 200

    gap = GapCtl().find_gap_extended(query={'periodic_gap_id': 1, 'state': GS.NEW})
    assert gap is None

    table_book = TableBook.objects.filter(gap_id__in=gap_ids)
    assert not table_book
