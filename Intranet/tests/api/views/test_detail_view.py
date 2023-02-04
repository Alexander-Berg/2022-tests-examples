import json
import pytest

from django.conf import settings
from django.core.urlresolvers import reverse
from django.test import override_settings
from unittest import mock

from intranet.audit.src.core import models
from intranet.audit.src.users.models import StatedPerson


@pytest.fixture
def control_two(db, author):
    return models.Control.objects.create(
        number='control number two',
        name='control two',
        author=author,
    )


def test_controlplan_detail_view_success(db, client, process, control_plan):
    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['process_data'][0]['name'] == process.name


def test_controlplan_detail_view_fail(db, client, control_plan):
    url = reverse("api_v1:controlplan_detail", kwargs={'pk': 9999999999})
    response = client.get(url)
    assert response.status_code == 404
    assert b'Request with invalid pk was made' in response.content


def test_controlplan_detail_patch_success(db, client, control_plan):
    assert control_plan.comment == 'comment for one'

    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    new_comment = 'test_new_comment'
    response = client.patch(
        path=url,
        data=json.dumps({"comment": new_comment}),
        content_type='application/json',
    )
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['comment'] == new_comment

    control_plan = models.ControlPlan.objects.get(pk=control_plan.id)
    assert response_json['id'] == control_plan.id
    assert control_plan.comment == new_comment


@pytest.mark.parametrize('status', (
    models.ControlPlan.STATUSES.archived,
    models.ControlPlan.STATUSES.deleted,
))
@override_settings(TEST_USER_DATA=settings.TEST_SIMPLE_USER_DATA)
def test_controlplan_detail_patch_fail(db, client, control_plan, status):
    control_plan.status = status
    control_plan.save()
    assert control_plan.comment == 'comment for one'

    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    new_comment = 'test_new_comment'
    response = client.patch(
        path=url,
        data=json.dumps({"comment": new_comment}),
        scontent_type='application/json',
    )
    assert response.status_code == 403


def test_controlplan_detail_patch_change_related_success(db, client, control,
                                                         control_two, control_plan):
    assert control_plan.control == control

    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    response = client.patch(
        path=url,
        data=json.dumps({"control": control_two.id}),
        content_type='application/json',
    )
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['control'] == control_two.id

    control_plan = models.ControlPlan.objects.get(pk=control_plan.id)
    assert response_json['id'] == control_plan.id
    assert control_plan.control == control_two


def test_controlplan_detail_patch_change_manytomany_success(db, client, process,
                                                            process_two, control_plan):
    assert control_plan.process.count() == 1
    assert control_plan.process.first() == process

    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    response = client.patch(
        path=url,
        data=json.dumps({"process": [process_two.id]}),
        content_type='application/json',
    )
    response_json = response.json()
    assert response.status_code == 200
    assert len(response_json['process']) == 1
    assert response_json['process'][0] == process_two.id

    control_plan = models.ControlPlan.objects.get(pk=control_plan.id)
    assert response_json['id'] == control_plan.id
    assert control_plan.process.count() == 1
    assert control_plan.process.first() == process_two


def test_controlplan_detail_patch_add_manytomany_success(db, client, process,
                                                         process_two, control_plan):
    assert control_plan.process.count() == 1
    assert control_plan.process.first() == process

    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    response = client.patch(
        path=url,
        data=json.dumps({"process": [process_two.id, process.id]}),
        content_type='application/json',
    )
    response_json = response.json()
    assert response.status_code == 200
    assert len(response_json['process']) == 2
    assert set(response_json['process']) == {process_two.id, process.id}

    control_plan = models.ControlPlan.objects.get(pk=control_plan.id)
    assert response_json['id'] == control_plan.id
    assert control_plan.process.count() == 2


def test_controlplan_detail_patch_add_manytomany_fail(db, client, process, control_plan):
    assert control_plan.process.count() == 1
    assert control_plan.process.first() == process

    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    response = client.patch(
        path=url,
        data=json.dumps({"process": [999999999]}),
        content_type='application/json',
    )
    response_json = response.json()
    expected_errors = {
        'process': [
            'Недопустимый первичный ключ "999999999" - объект не существует.'
        ]
    }
    assert response.status_code == 400
    assert response_json['errors'] == expected_errors
    assert control_plan.process.count() == 1
    assert control_plan.process.first() == process


def test_controlplan_detail_put_success(db, client, control, risk, control_two, control_plan,
                                        account, process):
    data = {
        'evidence': None,
        'regulation': None,
        'antifraud': False,
        'id': control_plan.id,
        'service': [],
        'business_unit': [],
        'system_data': [],
        'system': [],
        'process': [process.id],
        'control_type': 'warning',
        'risk_data': [
            {
                'number': 'risk number',
                'id': risk.id,
                'name': 'risk',
                'actions': {'change': True, 'delete': True},
            },
        ], 'account_data': [],
        'assertion': [],
        'description': None,
        'method': 'manual',
        'comment': 'comment for one',
        'legal': [],
        'owner': [],
        'test_period_finished': None,
        'business_unit_data': [],
        'account': [],
        'risk': [risk.id],
        'service_data': [],
        'key_control': True,
        'control': control.id,
        'test_period_started': None,
        'legal_data': [],
        'owner_data': [],
        'frequency': 'adhoc',
        'assertion_data': [],
        'control_data': {
            'number': 'control number',
            'id': control.id,
            'name': 'control',
            'actions': {'change': True, 'delete': True},
        },
        'process_data': [
            {
                'id': process.id,
                'name': 'process_one',
                'parent': None,
                'parent_data': None,
                'process_type': 'root',
                'actions': {'change': True, 'delete': True},
            },
        ]
    }
    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    response = client.get(url)
    response_json = response.json()
    assert response_json['id'] == control_plan.id
    assert response_json['process_data'] == data['process_data']

    assert control_plan.control == control
    assert control_plan.account.count() == 0

    data['comment'] = 'some new totally comment'
    data['account'] = [account.id]
    data['control'] = control_two.id
    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    response = client.put(url, json.dumps(data), content_type='application/json')
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['comment'] == data['comment']

    data['control_data'] = {
        'id': control_two.id,
        'name': control_two.name,
        'number': control_two.number,
        'actions': {'change': True, 'delete': True},
    }
    data['account_data'] = [{
        'id': account.id,
        'name': account.name,
        'parent': account.parent,
        'parent_data': None,
        'actions': {'change': True, 'delete': True},
    }]
    assert response_json['account_data'] == data['account_data']
    assert response_json['control_data'] == data['control_data']

    control_plan = models.ControlPlan.objects.get(pk=control_plan.id)
    assert control_plan.control == control_two
    assert control_plan.account.count() == 1
    assert control_plan.account.first() == account


def test_controlplan_detail_add_owner_success(db, client, control_plan, test_vcr):
    assert control_plan.owner.count() == 0
    assert StatedPerson.objects.count() == 0

    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    owner_id = "1120000000016772"
    with test_vcr.use_cassette('controlplan_add_owner_success'):
        with mock.patch(
                'intranet.audit.src.users.logic.staff_person.get_service_ticket',
                lambda *args, **kwargs: 'test token',
        ):
            response = client.patch(
                path=url,
                data=json.dumps({"owner": [owner_id]}),
                content_type='application/json',
            )
    response_json = response.json()
    assert response.status_code == 200
    assert len(response_json['owner_data']) == 1
    assert response_json['owner_data'][0]['uid'] == owner_id
    assert response_json['owner_data'][0]['login'] == 'smosker'
    assert len(response_json['owner']) == 1
    assert "{}_".format(owner_id) in response_json['owner'][0]

    stated_person = StatedPerson.objects.first()
    assert StatedPerson.objects.count() == 1
    assert stated_person.uid == owner_id

    control_plan = models.ControlPlan.objects.get(pk=control_plan.id)
    assert control_plan.owner.count() == 1
    assert control_plan.owner.first() == stated_person


def test_controlplan_detail_view_with_fields_success(db, client, control_plan,
                                                     django_assert_num_queries,
                                                     default_queries_count):
    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    with django_assert_num_queries(default_queries_count + 1):
        response = client.get(
            path=url,
            data={'fields': 'id,key_control,comment,method,control_type'},
        )
    response_json = response.json()
    expected = {
        'id': control_plan.id, 'comment': 'comment for one',
        'control_type': 'warning',
        'method': 'manual',
        'key_control': True,
    }
    assert response.status_code == 200
    assert response_json == expected


def test_controlplan_detail_view_with_fields_related_success(db, client, risk, control_plan,
                                                             django_assert_num_queries,
                                                             default_queries_count):
    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    with django_assert_num_queries(default_queries_count + 2):
        response = client.get(url, {'fields': 'id,key_control,risk', })
    response_json = response.json()
    expected = {
        'id': control_plan.id,
        'risk_data': [
            {
                'id': risk.id,
                'name': 'risk',
                'number': 'risk number',
                'actions': {'change': True, 'delete': True},
            }
        ],
        'key_control': True,
        'risk': [risk.id],
    }
    assert response.status_code == 200
    assert response_json == expected


def test_controltest_detail_view_success(db, client, control_test, control_step):
    url = reverse("api_v1:controltest_detail", kwargs={'pk': control_test.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == control_test.id
    assert response_json['controlstep_set_data'][0]['id'] == control_step.id


def test_controltest_detail_patch_success(db, client, control_test):
    assert control_test.comment == 'some comment'

    url = reverse("api_v1:controltest_detail", kwargs={'pk': control_test.id})
    new_comment = 'test_new_comment'
    response = client.patch(
        path=url,
        data=json.dumps({"comment": new_comment}),
        content_type='application/json',
    )
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['comment'] == new_comment

    control_test = models.ControlTest.objects.get(pk=control_test.id)
    assert response_json['id'] == control_test.id
    assert control_test.comment == new_comment


@pytest.mark.parametrize('status', (
    models.ControlTest.STATUSES.archived,
    models.ControlTest.STATUSES.deleted,
))
@override_settings(TEST_USER_DATA=settings.TEST_SIMPLE_USER_DATA)
def test_controltest_detail_patch_fail(db, client, control_test, status):
    control_test.status = status
    control_test.save()
    assert control_test.comment == 'some comment'

    url = reverse("api_v1:controltest_detail", kwargs={'pk': control_test.id})
    new_comment = 'test_new_comment'
    response = client.patch(
        path=url,
        data=json.dumps({"comment": new_comment}),
        content_type='application/json',
    )
    assert response.status_code == 403


def test_account_detail_view_success(db, client, account):
    url = reverse("api_v1:account_detail", kwargs={'pk': account.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == account.id


def test_account_detail_patch_success(db, client, account):
    assert account.name == 'account'

    url = reverse("api_v1:account_detail", kwargs={'pk': account.id})
    new_name = 'new name'
    response = client.patch(url, json.dumps({"name": new_name}), content_type='application/json')
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['name'] == new_name

    account = models.Account.objects.get(pk=account.id)
    assert response_json['id'] == account.id
    assert account.name == new_name


def test_account_detail_view_queries_num_success(db, client, account, default_queries_count,
                                                 django_assert_num_queries):
    url = reverse("api_v1:account_detail", kwargs={'pk': account.id})
    with django_assert_num_queries(default_queries_count + 1):
        response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == account.id


def test_assertion_detail_view_success(db, client, assertion):
    url = reverse("api_v1:assertion_detail", kwargs={'pk': assertion.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == assertion.id


def test_assertion_delete_view_success(db, client, assertion):
    assert models.Assertion.objects.count() == 1

    url = reverse("api_v1:assertion_detail", kwargs={'pk': assertion.id})
    response = client.delete(url)
    assert response.status_code == 204

    assert models.Assertion.objects.count() == 0


def test_assertion_delete_view_fail(db, client, assertion, control_plan):
    """
    Попытка удалить используемый Assertion
    """
    control_plan.assertion.add(assertion)

    url = reverse('api_v1:assertion_detail', kwargs={'pk': assertion.id})
    response = client.delete(url)
    assertion.refresh_from_db()
    assert response.status_code == 409

    assert not assertion.is_removed


def test_assertion_detail_patch_success(db, client, assertion):
    assert assertion.name == 'assertion name'

    url = reverse("api_v1:assertion_detail", kwargs={'pk': assertion.id})
    new_name = 'new name'
    response = client.patch(url, json.dumps({"name": new_name}), content_type='application/json')
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['name'] == new_name

    assertion = models.Assertion.objects.get(pk=assertion.id)
    assert response_json['id'] == assertion.id
    assert assertion.name == new_name


def test_assertion_detail_view_queries_num_success(db, client, assertion, default_queries_count,
                                                   django_assert_num_queries):
    url = reverse("api_v1:assertion_detail", kwargs={'pk': assertion.id})
    with django_assert_num_queries(default_queries_count + 1):
        response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == assertion.id


def test_business_unit_detail_view_success(db, client, business_unit):
    url = reverse("api_v1:business_unit_detail", kwargs={'pk': business_unit.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == business_unit.id


def test_business_unit_detail_patch_success(db, client, business_unit):
    assert business_unit.name == 'business unit'

    url = reverse("api_v1:business_unit_detail", kwargs={'pk': business_unit.id})
    new_name = 'new name'
    response = client.patch(
        path=url,
        data=json.dumps({"name": new_name}), content_type='application/json')
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['name'] == new_name

    business_unit = models.BusinessUnit.objects.get(pk=business_unit.id)
    assert response_json['id'] == business_unit.id
    assert business_unit.name == new_name


def test_control_detail_view_success(db, client, control):
    url = reverse("api_v1:control_detail", kwargs={'pk': control.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == control.id


def test_control_detail_patch_success(db, client, control):
    assert control.name == 'control'

    url = reverse("api_v1:control_detail", kwargs={'pk': control.id})
    new_name = 'new name'
    response = client.patch(
        path=url,
        data=json.dumps({"name": new_name}), content_type='application/json')
    response_json = response.json()
    assert response_json['name'] == new_name
    assert response.status_code == 200

    control = models.Control.objects.get(pk=control.id)
    assert response_json['id'] == control.id
    assert control.name == new_name


def test_control_step_detail_view_success(db, client, control_step):
    url = reverse("api_v1:controlstep_detail", kwargs={'pk': control_step.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == control_step.id


def test_control_step_detail_patch_success(db, client, control_step):
    assert control_step.step == 'some step'

    url = reverse("api_v1:controlstep_detail", kwargs={'pk': control_step.id})
    new_step = 'new step'
    response = client.patch(
        path=url,
        data=json.dumps({"step": new_step}), content_type='application/json')
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['step'] == new_step

    control_step = models.ControlStep.objects.get(pk=control_step.id)
    assert response_json['id'] == control_step.id
    assert control_step.step == new_step


def test_deficiency_detail_view_success(db, client, deficiency):
    url = reverse("api_v1:deficiency_detail", kwargs={'pk': deficiency.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == deficiency.id


def test_deficiency_detail_patch_success(db, client, deficiency):
    assert deficiency.short_description == 'some description'

    url = reverse("api_v1:deficiency_detail", kwargs={'pk': deficiency.id})
    new_short_description = 'new description'
    response = client.patch(
        path=url,
        data=json.dumps({"short_description": new_short_description}),
        content_type='application/json',
    )
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['short_description'] == new_short_description

    deficiency = models.Deficiency.objects.get(pk=deficiency.id)
    assert response_json['id'] == deficiency.id
    assert deficiency.short_description == new_short_description


def test_deficiency_group_detail_view_success(db, client, deficiency_group):
    url = reverse('api_v1:deficiencygroup_detail', kwargs={'pk': deficiency_group.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == deficiency_group.id


def test_deficiency_group_detail_patch_success(db, client, deficiency_group):
    assert deficiency_group.state == deficiency_group.STATES.open

    url = reverse('api_v1:deficiencygroup_detail', kwargs={'pk': deficiency_group.id})
    data = {'state': deficiency_group.STATES.fixed}
    response = client.patch(
        path=url,
        data=json.dumps(data), content_type='application/json')
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['state'] == data['state']

    deficiency_group.refresh_from_db()
    assert response_json['id'] == deficiency_group.id
    assert deficiency_group.state == data['state']


def test_ipe_view_success(db, client, ipe):
    url = reverse("api_v1:ipe_detail", kwargs={'pk': ipe.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == ipe.id


def test_ipe_patch_success(db, client, ipe):
    assert ipe.name == 'name'

    url = reverse("api_v1:ipe_detail", kwargs={'pk': ipe.id})
    new_name = 'new name'
    response = client.patch(url, json.dumps({"name": new_name}), content_type='application/json')
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['name'] == new_name

    ipe = models.IPE.objects.get(pk=ipe.id)
    assert response_json['id'] == ipe.id
    assert ipe.name == new_name


def test_legal_view_success(db, client, legal):
    url = reverse("api_v1:legal_detail", kwargs={'pk': legal.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == legal.id


def test_legal_patch_success(db, client, legal):
    assert legal.name == 'legal'

    url = reverse("api_v1:legal_detail", kwargs={'pk': legal.id})
    new_name = 'new name'
    response = client.patch(url, json.dumps({"name": new_name}), content_type='application/json')
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['name'] == new_name

    legal = models.Legal.objects.get(pk=legal.id)
    assert response_json['id'] == legal.id
    assert legal.name == new_name


def test_risk_view_success(db, client, risk):
    url = reverse("api_v1:risk_detail", kwargs={'pk': risk.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == risk.id


def test_risk_patch_success(db, client, risk):
    assert risk.name == 'risk'

    url = reverse("api_v1:risk_detail", kwargs={'pk': risk.id})
    new_name = 'new name'
    response = client.patch(url, json.dumps({"name": new_name}), content_type='application/json')
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['name'] == new_name

    risk = models.Risk.objects.get(pk=risk.id)
    assert response_json['id'] == risk.id
    assert risk.name == new_name


def test_system_view_success(db, client, system):
    url = reverse("api_v1:system_detail", kwargs={'pk': system.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == system.id


def test_system_patch_success(db, client, system):
    assert system.name == 'system'

    url = reverse("api_v1:system_detail", kwargs={'pk': system.id})
    new_name = 'new name'
    response = client.patch(url, json.dumps({"name": new_name}), content_type='application/json')
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['name'] == new_name

    system = models.System.objects.get(pk=system.id)
    assert response_json['id'] == system.id
    assert system.name == new_name


def test_service_view_success(db, client, service):
    url = reverse("api_v1:service_detail", kwargs={'pk': service.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == service.id


def test_service_patch_success(db, client, service):
    assert service.name == 'service'

    url = reverse("api_v1:service_detail", kwargs={'pk': service.id})
    new_name = 'new name'
    response = client.patch(url, json.dumps({"name": new_name}), content_type='application/json')
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['name'] == new_name

    service = models.Service.objects.get(pk=service.id)
    assert response_json['id'] == service.id
    assert service.name == new_name


def test_process_view_success(db, client, process):
    url = reverse("api_v1:process_detail", kwargs={'pk': process.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == process.id


def test_process_patch_success(db, client, process):
    assert process.name == 'process_one'

    url = reverse("api_v1:process_detail", kwargs={'pk': process.id})
    new_name = 'new name'
    response = client.patch(url, json.dumps({"name": new_name}), content_type='application/json')
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['name'] == new_name

    process = models.Process.objects.get(pk=process.id)
    assert response_json['id'] == process.id
    assert process.name == new_name


def test_file_view_success(db, client, file):
    url = reverse("api_v1:file_detail", kwargs={'pk': file.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == file.id


def test_controltestipe_view_success(db, client, controltestipe):
    url = reverse("api_v1:controltestipe_detail", kwargs={'pk': controltestipe.id})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['id'] == controltestipe.id


def test_controlplan_status_change_success(db, client, control_plan):
    control_plan.status = 'active'
    control_plan.save()
    assert control_plan.comment == 'comment for one'

    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    new_comment = 'test_new_comment'
    response = client.patch(
        path=url,
        data=json.dumps({"comment": new_comment}),
        content_type='application/json')
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['comment'] == new_comment

    control_plan = models.ControlPlan.objects.get(pk=control_plan.id)
    assert response_json['id'] == control_plan.id
    assert control_plan.comment == new_comment
    assert control_plan.status == 'draft'


def test_controlplan_status_dont_change_success(db, client, control_plan, stated_person):
    control_plan.reviewer.add(stated_person)
    control_plan.status = 'active'
    control_plan.save()
    assert control_plan.comment == 'comment for one'

    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    new_comment = 'test_new_comment'
    response = client.patch(
        path=url,
        data=json.dumps({"comment": new_comment}),
        content_type='application/json',
    )
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['comment'] == new_comment

    control_plan = models.ControlPlan.objects.get(pk=control_plan.id)
    assert response_json['id'] == control_plan.id
    assert control_plan.comment == new_comment
    assert control_plan.status == 'active'
