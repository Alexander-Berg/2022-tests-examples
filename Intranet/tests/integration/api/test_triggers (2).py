import pytest
from collections import namedtuple
from unittest.mock import patch

from django.urls import reverse
from tests.factories import FlowFactory

pytestmark = pytest.mark.django_db

TRIGGER_URL = 'some/url'
TRIGGER_ID = 1
# impossible to use unittest.mock.Mock due to "self" parameter
TriggerMockCls = namedtuple('TriggerMockCls', 'self,id')
TRIGGER_MOCK = TriggerMockCls(TRIGGER_URL, TRIGGER_ID)


@pytest.mark.parametrize('params', [
    {
        'trigger_create_type': 'complete',
        'queue': 'SOMEQUEUE',
        'name': 'trigger_name',
        'groups': ['yandex', 'yandex_dep'],
        'is_parallel': True,
        'oauth_token': 'SOMETOKEN',
        'stages': [
            {'approver': 'shigarus'},
            {
                'stages': [{'approver': 'zivot'}],
                'need_approvals': 1,
            }
        ],
    },
    {
        'trigger_create_type': 'draft',
        'queue': 'SOMEQUEUE',
        'name': 'trigger_name',
        'groups': ['yandex', 'yandex_dep'],
        'stages': [
            {'approver': 'shigarus'},
            {
                'stages': [{'approver': 'zivot'}],
                'need_approvals': 1,
            },
        ],
    },
    {
        'trigger_create_type': 'complete',
        'queue': 'SOMEQUEUE',
        'name': 'trigger_name',
        'groups': ['yandex', 'yandex_dep'],
        'is_parallel': True,
        'oauth_token': 'SOMETOKEN',
        'flow_name': 'flow_name',
        'flow_context': {'asd': '1'},
    },
    {
        'trigger_create_type': 'draft',
        'queue': 'SOMEQUEUE',
        'name': 'trigger_name',
        'groups': ['yandex', 'yandex_dep'],
        'flow_name': 'flow_name',
        'flow_context': {'asd': '1'},
    },
])
@patch('ok.tracker.controllers.st_connection.request', lambda *a, **kw: TRIGGER_MOCK)
def test_trigger_create(client, params):
    FlowFactory(name='flow_name', code='')
    url = reverse('api:triggers:create_trigger')
    response = client.post(url, data=params)
    assert response.status_code == 200, response.content
    assert response.json()['trigger_url'] == TRIGGER_URL


@pytest.mark.parametrize('params', [
    {
        'trigger_create_type': 'complete',
        'queue': 'SOMEQUEUE',
        'name': 'trigger_name',
        'groups': ['yandex', 'yandex_dep'],
    },
])
def test_trigger_invalid_form(client, params):
    url = reverse('api:triggers:create_trigger')
    response = client.post(url, data=params)
    assert response.status_code == 400, response.content
    assert response.json()['errors'][''][0]['code'] == 'oauth_token_required'
