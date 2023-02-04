import copy

import pytest

from django.urls import reverse
from django.contrib.auth.models import User, Group
from django.conf import settings
from startrek_client.exceptions import NotFound
from unittest.mock import Mock, patch

from ok.approvements.choices import APPROVEMENT_STAGE_APPROVEMENT_SOURCES, APPROVEMENT_STATUSES
from ok.approvements.models import Approvement
from ok.tracker.queues import get_queue_name
from ok.approvements import controllers
from ok.flows import executor as flow_executor
from tests.integration.api.mocks import _mock_get_validated_map_staff_users, _mock_table_flow


pytestmark = pytest.mark.django_db


stages = [
    {'approver': 'agrml'},
    {'stages': [{'approver': 'qazaq'}, {'approver': 'tmalikova'}]},
    {'approver': 'terrmit'},
]


@pytest.fixture
def flow_client(client):
    flow_group = Group.objects.create(name=settings.FLOW_USER_GROUP)
    user = User.objects.create(username='somelogin')
    user.groups.add(flow_group)
    client.force_authenticate(user.username)
    return client


def test_flow_test_fails(flow_client):
    url = reverse('private_api:flows:test')
    data = dict(flow_code="add_approvers_group(['volozh', 'tavria'], need_all=true)", flow_context={})
    response = flow_client.post(url, data=data)
    assert response.status_code == 200, response.content
    assert 'NameError' in response.json()['traceback']


@patch('ok.approvements.controllers.get_validated_map_staff_users', _mock_get_validated_map_staff_users)
def test_flow_test_ok(flow_client):
    url = reverse('private_api:flows:test')
    data = dict(flow_code='add_approver("tester")', flow_context={'asd': 'asd'})
    response = flow_client.post(url, data=data)
    assert response.status_code == 200, response.content
    login = response.json()['stages'][0]['approver']['login']
    assert login == 'tester'


@patch('ok.approvements.controllers.get_validated_map_staff_users', _mock_get_validated_map_staff_users)
@pytest.mark.parametrize('code', (
    'add_approver(flow_api.Person("tester"))',
    'flow_api.stages.append_persons("tester")',
    'flow_api.stages.append_persons(flow_api.Person("tester"))',
))
def test_flow_test_ok_objects(flow_client, code):
    url = reverse('private_api:flows:test')
    data = dict(flow_code=code, flow_context={'asd': 'asd'})
    response = flow_client.post(url, data=data)
    assert response.status_code == 200, response.content
    login = response.json()['stages'][0]['approver']['login']
    assert login == 'tester'


@patch('ok.approvements.controllers.get_validated_map_staff_users', _mock_get_validated_map_staff_users)
@pytest.mark.parametrize('code', (
    'add_approvers_group([flow_api.Person("tester")])',
    'flow_api.stages.append_group(["tester"], False)',
    'flow_api.stages.append_group([flow_api.Person("tester")], False)',
))
def test_flow_test_ok_objects_groups(flow_client, code):
    url = reverse('private_api:flows:test')
    data = dict(flow_code=code, flow_context={'asd': 'asd'})
    response = flow_client.post(url, data=data)
    assert response.status_code == 200, response.content
    login = response.json()['stages'][0]['stages'][0]['approver']['login']
    assert login == 'tester'


@patch('ok.approvements.controllers.get_validated_map_staff_users', _mock_get_validated_map_staff_users)
def test_flow_test_drop(flow_client):
    url = reverse('private_api:flows:test')
    data = dict(flow_code='add_approver("tester")\ndrop_approvement()\nadd_approver("tester")',
                flow_context={'asd': 'asd'})
    response = flow_client.post(url, data=data)
    assert response.status_code == 200, response.content
    assert response.json()['detail']['error'] == 'Flow creation dropped'


@patch('ok.flows.flow_functions._call_table_flow', _mock_table_flow)
@patch('ok.approvements.controllers.get_validated_map_staff_users', _mock_get_validated_map_staff_users)
def test_flow_call_table_flow(flow_client):
    url = reverse('private_api:flows:test')
    data = dict(flow_code='a = call_table_flow(1, a=2)\nadd_approver(a[\'login\'])',
                flow_context={'asd': 'asd'})
    response = flow_client.post(url, data=data)
    assert response.status_code == 200, response.content
    login = response.json()['stages'][0]['approver']['login']
    assert login == 'tester'


def test_flow_not_in_group(client):
    url = reverse('private_api:flows:test')
    data = dict(flow_code='print(1)', flow_context={})
    response = client.post(url, data=data)
    assert response.status_code == 403, response.content
