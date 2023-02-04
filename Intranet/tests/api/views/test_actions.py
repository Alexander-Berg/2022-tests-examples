import json
import pytest

from django.conf import settings
from django.core.urlresolvers import reverse
from django.test import override_settings
from freezegun import freeze_time
from unittest import mock


def test_controlplan_action_draft_success(db, client, control_plan):
    assert control_plan.status == 'draft'

    url = reverse("api_v1:action", kwargs={'pk': control_plan.id, 'obj_class': 'controlplan'})
    response = client.get(url)
    response_json = response.json()

    expected = {'edit', 'make_report', 'clone', 'to_review', 'to_deleted'}
    actual = {item for item, available in response_json.items() if available}
    assert expected == actual


def test_controlplan_action_deleted_success(db, client, control_plan):
    control_plan.status = 'deleted'
    control_plan.save()

    url = reverse("api_v1:action", kwargs={'pk': control_plan.id, 'obj_class': 'controlplan'})
    response = client.get(url)
    response_json = response.json()

    expected = {'make_report', 'clone'}
    actual = {item for item, available in response_json.items() if available}
    assert expected == actual


def test_controlplan_action_archived_success(db, client, control_plan):
    control_plan.status = 'archived'
    control_plan.save()

    url = reverse("api_v1:action", kwargs={'pk': control_plan.id, 'obj_class': 'controlplan'})
    response = client.get(url)
    response_json = response.json()

    expected = {'make_report', 'clone'}
    actual = {item for item, available in response_json.items() if available}
    assert expected == actual


def test_controlplan_action_review_success(db, client, control_plan):
    control_plan.status = 'review'
    control_plan.save()

    url = reverse("api_v1:action", kwargs={'pk': control_plan.id, 'obj_class': 'controlplan'})
    response = client.get(url)
    response_json = response.json()

    expected = {'edit', 'make_report', 'clone', 'to_draft', 'to_deleted', 'to_active'}
    actual = {item for item, available in response_json.items() if available}
    assert expected == actual


def test_controlplan_action_active_success(db, client, control_plan):
    control_plan.status = 'active'
    control_plan.save()

    url = reverse("api_v1:action", kwargs={'pk': control_plan.id, 'obj_class': 'controlplan'})
    response = client.get(url)
    response_json = response.json()

    expected = {
        'edit',
        'make_report',
        'clone',
        'add_version',
        'add_test',
        'to_archived',
        'to_deleted',
        'to_draft',
    }
    actual = {item for item, available in response_json.items() if available}
    assert expected == actual


def test_controlplan_action_active_with_controltest_success(db, client, control_test,
                                                            control_plan, stated_person):
    control_plan.status = 'active'
    control_plan.reviewer.add(stated_person)
    control_plan.save()

    url = reverse("api_v1:action", kwargs={'pk': control_plan.id, 'obj_class': 'controlplan'})
    response = client.get(url)
    response_json = response.json()

    expected = {'make_report', 'clone', 'add_version', 'add_test', 'to_archived', 'edit'}
    actual = {item for item, available in response_json.items() if available}
    assert expected == actual

    control_test.status = 'archived'
    control_test.save()

    response = client.get(url)
    response_json = response.json()

    expected = {'make_report', 'clone', 'add_version', 'add_test', 'to_archived'}
    actual = {item for item, available in response_json.items() if available}
    assert expected == actual


@override_settings(TEST_USER_DATA=settings.TEST_NO_ACCESS_USER_DATA)
def test_controlplan_action_no_superuser_controltest_success(db, client, control_plan):
    url = reverse("api_v1:action", kwargs={'pk': control_plan.id, 'obj_class': 'controlplan'})
    response = client.get(url)
    response_json = response.json()

    expected = {'to_review', 'to_deleted'}
    actual = {item for item, available in response_json.items() if available}
    assert expected == actual


def test_change_status_success(db, client, control_plan):
    assert control_plan.status == 'draft'

    url = reverse("api_v1:action", kwargs={'pk': control_plan.id, 'obj_class': 'controlplan'})
    data = {'action': 'to_deleted'}
    response = client.post(url, data)
    assert response.status_code == 200

    control_plan.refresh_from_db()
    assert control_plan.status == 'deleted'


def test_change_status_fail(db, client, control_plan):
    assert control_plan.status == 'draft'

    url = reverse("api_v1:action", kwargs={'pk': control_plan.id, 'obj_class': 'controlplan'})
    data = {'action': 'to_active'}
    response = client.post(url, data)
    assert response.status_code == 409

    response_json = response.json()
    assert response_json['message'] == ['You can not set this status for current object']

    control_plan.refresh_from_db()
    assert control_plan.status == 'draft'


def test_change_status_to_review_success(db, client, control_plan, test_vcr):
    assert control_plan.status == 'draft'
    assert control_plan.reviewer.count() == 0

    url = reverse("api_v1:action", kwargs={'pk': control_plan.id, 'obj_class': 'controlplan'})
    data = {'action': 'to_review', 'data': {'reviewer_id': '1120000000016772'}}
    with test_vcr.use_cassette('change_status_to_review_success.yaml'):
        with mock.patch(
                'intranet.audit.src.users.logic.staff_person.get_service_ticket',
                lambda *args, **kwargs: 'test token'
        ):
            response = client.post(url, json.dumps(data), content_type='application/json')
    assert response.status_code == 200

    response_json = response.json()
    assert response_json['controlplan']['status'] == 'review'
    assert response_json['actions'] == {
        'clone': True,
        'to_active': True,
        'edit': True,
        'add_version': False,
        'to_draft': True,
        'to_review': False,
        'make_report': True,
        'to_archived': False,
        'add_test': False,
        'to_deleted': True,
    }

    control_plan.refresh_from_db()
    assert control_plan.status == 'review'
    assert control_plan.reviewer.count() == 1

    reviewer = control_plan.reviewer.first()
    assert reviewer.uid == '1120000000016772'
    assert reviewer.login == 'smosker'


@freeze_time('2017-07-21')
@pytest.mark.parametrize('action', ('add_version', 'to_archived'))
@pytest.mark.parametrize('action_data, test_period_finished', (
    ({'test_period_finished': '2015-01-01'}, '2015-01-01'),
    (None, '2017-07-21'),
))
def test_controlplan_archive(db, client, control_plan, action, action_data, test_period_finished):
    control_plan.status = 'active'
    control_plan.save()

    url = reverse("api_v1:action", kwargs={'pk': control_plan.id, 'obj_class': 'controlplan'})
    data = {'action': action}
    if action_data:
        data['data'] = action_data
    response = client.post(url, json.dumps(data), content_type='application/json')
    assert response.status_code == 200, response.content

    control_plan.refresh_from_db()
    assert control_plan.status == 'archived'
    assert control_plan.test_period_finished.isoformat() == test_period_finished


def test_add_new_version_fail(db, client, control_plan):
    assert control_plan.status == 'draft'

    url = reverse("api_v1:action", kwargs={'pk': control_plan.id, 'obj_class': 'controlplan'})
    data = {'action': 'add_version'}
    response = client.post(url, data)
    assert response.status_code == 409

    control_plan.refresh_from_db()
    assert control_plan.status == 'draft'


def test_controltest_action_draft_success(db, client, control_test):
    control_test.status = 'draft'
    control_test.save()

    url = reverse("api_v1:action", kwargs={'pk': control_test.id, 'obj_class': 'controltest'})
    response = client.get(url)
    response_json = response.json()

    expected = {
        'edit',
        'make_report',
        'change_controltestipe',
        'delete_controltestipe',
        'create_controltestipe',
        'reorder_steps',
        'create_deficiency',
        'download_attachments',
        'to_review',
        'create_step',
        'delete_step',
        'change_step',
        'to_deleted',
    }
    actual = {item for item, available in response_json.items() if available}
    assert expected == actual


def test_controltest_action_active_success(db, client, control_test):
    control_test.status = 'active'
    control_test.save()

    url = reverse("api_v1:action", kwargs={'pk': control_test.id, 'obj_class': 'controltest'})
    response = client.get(url)
    response_json = response.json()

    expected = {
        'edit',
        'make_report',
        'download_attachments',
        'to_deleted',
        'to_archived',
        'to_draft',
    }
    actual = {item for item, available in response_json.items() if available}
    assert expected == actual


def test_controltest_action_review_success(db, client, control_test, stated_person):
    control_test.status = 'review'
    control_test.reviewer.add(stated_person)
    control_test.save()

    url = reverse("api_v1:action", kwargs={'pk': control_test.id, 'obj_class': 'controltest'})
    response = client.get(url)
    response_json = response.json()

    expected = {
        'edit',
        'make_report',
        'change_controltestipe',
        'delete_controltestipe',
        'create_controltestipe',
        'reorder_steps',
        'to_active',
        'download_attachments',
        'to_draft',
        'create_step',
        'delete_step',
        'change_step',
        'to_deleted',
    }
    actual = {item for item, available in response_json.items() if available}
    assert expected == actual


def test_controltest_action_archived_success(db, client, control_test):
    control_test.status = 'archived'
    control_test.save()

    url = reverse("api_v1:action", kwargs={'pk': control_test.id, 'obj_class': 'controltest'})
    response = client.get(url)
    response_json = response.json()

    expected = {'make_report', 'download_attachments'}
    actual = {item for item, available in response_json.items() if available}
    assert expected == actual


def test_change_controltest_status_success(db, client, control_test, controltestipe, ipe):
    control_test.status = 'active'
    control_test.save()
    assert ipe.status == 'in_progress'

    url = reverse("api_v1:action", kwargs={'pk': control_test.id, 'obj_class': 'controltest'})
    data = {'action': 'to_archived'}
    response = client.post(url, data)
    assert response.status_code == 200

    control_test.refresh_from_db()
    ipe.refresh_from_db()
    assert control_test.status == 'archived'
    assert ipe.status == 'in_progress'


def test_change_controltest_status_fail(db, client, control_test, controltestipe, ipe):
    control_test.status = 'review'
    control_test.save()
    assert ipe.status == 'in_progress'

    url = reverse("api_v1:action", kwargs={'pk': control_test.id, 'obj_class': 'controltest'})
    data = {'action': 'to_archived'}
    response = client.post(url, data)
    assert response.status_code == 409

    control_test.refresh_from_db()
    ipe.refresh_from_db()
    assert control_test.status == 'review'
    assert ipe.status == 'in_progress'
