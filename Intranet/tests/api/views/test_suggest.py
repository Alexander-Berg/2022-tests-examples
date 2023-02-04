import pytest
from unittest import mock

from django.core.urlresolvers import reverse

from intranet.audit.src.core import models
from intranet.audit.src.users.models import StatedPerson


@pytest.fixture
def account(db, author):
    return models.Account.objects.create(
        name='some account',
        author=author,
    )


@pytest.fixture
def control_step(db, author, ):
    return models.ControlStep.objects.create(
        step='some description',
        author=author,
    )


@pytest.fixture
def statedperson_group(db, ):
    obj_id = '1_{}'.format(StatedPerson.GROUP_POSITION_SLUG)
    return StatedPerson.objects.create(
        id=obj_id,
        uid=obj_id,
        department='Очень важная группа',
        position=StatedPerson.GROUP_POSITION_SLUG,
    )


@pytest.mark.parametrize('is_root', (True, False))
def test_suggest_process_full_match_success(db, client, process, sub_process, is_root):
    process = process if is_root else sub_process
    channel = 'root_process' if is_root else 'process'
    url = reverse('ajax_lookup', kwargs={'channel': channel})
    response = client.get(url, {'term': process.name})
    response_json = response.json()
    assert len(response_json) == 1
    assert response_json[0]['pk'] == str(process.id)
    assert response.status_code == 200


@pytest.mark.parametrize('is_root', (True, False))
def test_suggest_process_no_match_success(db, client, process, sub_process, is_root):
    channel = 'root_process' if is_root else 'process'
    url = reverse('ajax_lookup', kwargs={'channel': channel})
    response = client.get(url, {'term': '9999999999999'})
    response_json = response.json()
    assert len(response_json) == 0


@pytest.mark.parametrize('is_root', (True, False))
def test_suggest_process_partial_match_success(db, client, process, process_two, sub_process,
                                               sub_process_two, is_root):
    channel = 'root_process' if is_root else 'process'
    expected = {process, process_two} if is_root else {sub_process, sub_process_two}
    url = reverse('ajax_lookup', kwargs={'channel': channel})
    response = client.get(url, {'term': 'proce'})
    response_json = response.json()
    assert len(response_json) == 2
    assert {obj['pk'] for obj in response_json} == {str(i.id) for i in expected}
    assert response.status_code == 200


def test_suggest_process_by_parent_success(db, client, sub_process, sub_process_two, process_three):
    url = reverse('ajax_lookup', kwargs={'channel': 'process'})
    response = client.get(url, {'term': process_three.name})
    response_json = response.json()
    expected = {str(sub_process.id), str(sub_process_two.id)}
    assert len(response_json) == 2
    assert {obj['pk'] for obj in response_json} == expected
    assert response.status_code == 200


@pytest.mark.parametrize('is_root', (True, False))
def test_suggest_process_wildcard_match_success(db, client, process, process_two, process_three,
                                                sub_process, sub_process_two, sub_process_three,
                                                is_root):
    channel = 'root_process' if is_root else 'process'
    expected = (
        {process, process_two, process_three}
        if is_root
        else {sub_process, sub_process_two, sub_process_three}
    )
    url = reverse('ajax_lookup', kwargs={'channel': channel})
    response = client.get(url, {'term': '*'})
    response_json = response.json()
    assert len(response_json) == 3
    assert {str(obj.id) for obj in expected} == {obj['pk'] for obj in response_json}
    assert response.status_code == 200


def test_suggest_account_success(db, client, account):
    url = reverse('ajax_lookup', kwargs={'channel': 'account'})
    response = client.get(url, {'term': account.name})
    response_json = response.json()
    assert len(response_json) == 1
    assert response_json[0]['value'] == account.name
    assert response_json[0]['pk'] == str(account.id)
    assert response.status_code == 200


def test_suggest_risk_by_number_success(db, client, risk):
    url = reverse('ajax_lookup', kwargs={'channel': 'risk'})
    response = client.get(url, {'term': risk.number})
    response_json = response.json()
    assert len(response_json) == 1
    assert risk.number in response_json[0]['value']
    assert response_json[0]['pk'] == str(risk.id)
    assert response.status_code == 200


def test_suggest_risk_by_name_success(db, client, risk):
    url = reverse('ajax_lookup', kwargs={'channel': 'risk'})
    response = client.get(url, {'term': risk.name})
    response_json = response.json()
    assert len(response_json) == 1
    assert risk.name in response_json[0]['value']
    assert response_json[0]['pk'] == str(risk.id)
    assert response.status_code == 200


def test_suggest_control_by_number_success(db, client, control):
    url = reverse('ajax_lookup', kwargs={'channel': 'control'})
    response = client.get(url, {'term': control.number})
    response_json = response.json()
    assert len(response_json) == 1
    assert control.number in response_json[0]['value']
    assert response_json[0]['pk'] == str(control.id)
    assert response.status_code == 200


def test_suggest_control_by_name_success(db, client, control):
    url = reverse('ajax_lookup', kwargs={'channel': 'control'})
    response = client.get(url, {'term': control.name})
    response_json = response.json()
    assert len(response_json) == 1
    assert control.name in response_json[0]['value']
    assert response_json[0]['pk'] == str(control.id)
    assert response.status_code == 200


def test_suggest_controlplan_success(db, client, control_plan):
    url = reverse('ajax_lookup', kwargs={'channel': 'control_plan'})
    response = client.get(url, {'term': control_plan.id})
    response_json = response.json()
    assert len(response_json) == 1
    assert response_json[0]['pk'] == str(control_plan.id)
    assert response.status_code == 200


def test_suggest_controlstep_by_step_success(db, client, control_step):
    url = reverse('ajax_lookup', kwargs={'channel': 'control_step'})
    response = client.get(url, {'term': control_step.step})
    response_json = response.json()
    assert len(response_json) == 1
    assert response_json[0]['pk'] == str(control_step.id)
    assert response.status_code == 200


def test_suggest_deficiency_by_description_success(db, client, deficiency):
    url = reverse('ajax_lookup', kwargs={'channel': 'deficiency'})
    response = client.get(url, {'term': deficiency.short_description})
    response_json = response.json()
    assert len(response_json) == 1
    assert deficiency.short_description in response_json[0]['value']
    assert response_json[0]['pk'] == str(deficiency.id)
    assert response.status_code == 200


def test_suggest_no_double(db, client, author,):
    account = models.Account.objects.create(
        name='some account',
        author=author,
    )
    account.name = '{} test'.format(account.id)
    account.save()
    url = reverse('ajax_lookup', kwargs={'channel': 'account'})
    response = client.get(url, {'term': account.id})
    response_json = response.json()
    assert len(response_json) == 1
    assert response_json[0]['pk'] == str(account.id)
    assert response.status_code == 200


def test_suggest_stated_person_group_success(db, client, statedperson_group):
    url = reverse('ajax_lookup', kwargs={'channel': 'stated_person'})
    with mock.patch('intranet.audit.src.users.lookups.suggest_persons',
                    lambda *args, **kwargs: list()):
        response = client.get(url, {'term': statedperson_group.department})
    response_json = response.json()
    assert len(response_json) == 1
    assert response_json[0]['pk'] == statedperson_group.id
    assert response.status_code == 200
