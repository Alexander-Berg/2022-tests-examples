# coding: utf-8
import json
from unittest.mock import patch

import pytest
import responses
from django.test import override_settings
from pkg_resources import resource_string

from procu.api import models
from procu.api.utils import get_front_version
from procu.utils.test import assert_status, mock_phone, prepare_user, mock_wf

pytestmark = [pytest.mark.django_db]


# ------------------------------------------------------------------------------


STORAGE_REFERENCE = {
    "key": "5af517d6-c104-4ca8-9427-35e7be849445",
    "data": {
        "info": {"url": "123"},
        "tags": [19, 99],
        "title": "политической культуры",
        "agents": [],
        "vat_id": "Коммунизм иссушает",
        "comment": "Потенциал почвенной влаги однозначно интегрирует",
        "legal_name": "",
        "has_contract": False,
        "payment_terms": "парадоксальным, косвенно",
    },
}


def test_storage_retrieve(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['employee'])

    resp = client.get(f'/api/misc/storage/{STORAGE_REFERENCE["key"]}')
    assert_status(resp, 200)

    assert resp.json() == STORAGE_REFERENCE


def test_storage_create(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['employee'])

    form = STORAGE_REFERENCE['data']

    resp = client.post(f'/api/misc/storage', data={'data': form})
    assert_status(resp, 201)

    result = resp.json()

    assert result['data'] == form

    try:
        model_data = models.FormData.objects.values_list('data', flat=True).get(
            key=result['key']
        )
        assert model_data == form

    except models.FormData.DoesNotExist:
        pytest.fail('Storage item has not been created')


@override_settings(FORM_STORAGE_MAX_SIZE=5)
def test_storage_size_limit(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['employee'])

    resp = client.post('/api/misc/storage', data={'data': '123456789'})
    assert_status(resp, 400)


@override_settings(FORM_STORAGE_KEYS_PER_DAY=2)
def test_storage_rate_limit(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['employee'])

    resp = None

    for _ in range(3):
        resp = client.post('/api/misc/storage', data={'data': '123456789'})

    assert_status(resp, 400)


@pytest.mark.parametrize('role', ('employee', 'manager', 'admin'))
@pytest.mark.parametrize('endpoint', ('sections', 'forms'))
def test_sections_and_forms(clients, endpoint, role):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=[role])

    reference = json.loads(
        resource_string('procu', f'fixtures/{endpoint}/{role}.json')
    )

    resp = client.get(f'/api/misc/{endpoint}')
    assert_status(resp, 200)

    assert resp.json() == reference


def test_wf_preview_unavailable(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['employee'])

    with responses.RequestsMock():
        resp = client.post('/api/misc/wf', data={'message': 'foo'})
        assert_status(resp, 200)
        assert resp.json() == {'html': ''}


def test_wf_preview_ok(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['employee'])

    html = resource_string('procu', f'fixtures/wf/response.html').decode()

    with mock_wf(html):
        resp = client.post('/api/misc/wf', data={'message': 'foo'})
        assert_status(resp, 200)
        assert resp.json() == {'html': html}


def test_reset_version(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['employee'])

    v1 = get_front_version()

    resp = client.post('/api/misc/reset_static_version')
    assert_status(resp, 200)

    v2 = get_front_version()

    assert v1 and v2
    assert v1 != v2


@patch('procu.api.utils.common.get_service_ticket', lambda x: '***')
def test_get_phone(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['employee'])

    with mock_phone('robot-procu', number='+71234567891'):
        resp = client.get('/api/misc/phone?username=robot-procu')

    assert_status(resp, 200)
    assert resp.json() == {'phone': '+71234567891'}
