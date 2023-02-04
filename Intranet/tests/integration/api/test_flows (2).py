import pytest

from unittest.mock import patch

from django.conf import settings
from django.contrib.auth.models import Group
from django.urls import reverse

from tests import factories as f
from tests.integration.api.mocks import _mock_table_flow


pytestmark = pytest.mark.django_db


stages = [
    {'approver': 'agrml'},
    {'stages': [{'approver': 'qazaq'}, {'approver': 'tmalikova'}]},
    {'approver': 'terrmit'},
]


@pytest.fixture
def flow_client(client):
    flow_group = Group.objects.create(name=settings.FLOW_USER_GROUP)
    user = f.UserFactory()
    approver = f.UserFactory(username='tester')  # noqa
    user.groups.add(flow_group)
    client.force_authenticate(user.username)
    return client


def test_flow_test_fails(flow_client):
    url = reverse('private_api:flows:test')
    data = dict(flow_code="add_approvers_group(['volozh', 'tavria'], need_all=true)", flow_context={})
    response = flow_client.post(url, data=data)
    assert response.status_code == 200, response.content
    assert 'NameError' in response.json()['traceback']


def test_flow_test_ok(flow_client):
    url = reverse('private_api:flows:test')
    data = dict(flow_code='add_approver("tester")', flow_context={'asd': 'asd'})
    response = flow_client.post(url, data=data)
    assert response.status_code == 200, response.content
    login = response.json()['stages'][0]['approver']['login']
    assert login == 'tester'


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


@pytest.mark.parametrize('func, need_all, is_with_deputies, expected', (
    ('add_approvers_group', None, True, True),
    ('add_approvers_group', False, True, True),
    ('flow_api.stages.append_group', True, True, Exception),
    ('flow_api.stages.append_group', False, False, False),
))
def test_flow_test_group_with_deputies(flow_client, func, need_all, is_with_deputies, expected):
    if need_all is None:
        code = f'{func}(["tester"], is_with_deputies={is_with_deputies})'
    else:
        code = f'{func}(["tester"], {need_all}, is_with_deputies={is_with_deputies})'

    url = reverse('private_api:flows:test')
    response = flow_client.post(url, data={'flow_code': code, 'flow_context': {}})

    assert response.status_code == 200, response.content
    response_data = response.json()
    if expected is Exception:
        assert 'cannot use deputies with need_all=True' in response_data['traceback']
    else:
        group_stage = response_data['stages'][0]
        assert group_stage['is_with_deputies'] == expected
        assert group_stage['stages'][0]['approver']['login'] == 'tester'


def test_flow_test_drop(flow_client):
    url = reverse('private_api:flows:test')
    data = dict(flow_code='add_approver("tester")\ndrop_approvement()\nadd_approver("tester")',
                flow_context={'asd': 'asd'})
    response = flow_client.post(url, data=data)
    assert response.status_code == 200, response.content
    assert response.json()['detail']['error'] == 'Flow creation dropped'


@patch('ok.flows.flow_functions._call_table_flow', _mock_table_flow)
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
