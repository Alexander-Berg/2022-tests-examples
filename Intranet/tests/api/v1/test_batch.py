# coding: utf-8

import pytest

from idm.utils import reverse
from idm.core.models import Role

pytestmark = pytest.mark.django_db


def test_readonly_batch_v1(client, users_with_roles):
    client.login('admin')

    role1, role2 = Role.objects.all()[:2]
    batch_url = reverse('api_dispatch_list', api_name='v1', resource_name='batch')

    response = client.json.post(
        path=batch_url,
        data=[
            {
                'method': 'get',
                'path': 'roles/{}/'.format(role1.id),
            },
            {
                'id': '123',
                'method': 'get',
                'path': 'roles/{}/'.format(role2.id),
            },
        ],
    )

    assert response.status_code == 200
    response_data = response.json()

    assert isinstance(response_data['responses'], list)
    assert len(response_data['responses']) == 2

    batch_response1, batch_response2 = response_data['responses']

    assert 'id' not in batch_response1
    assert 'id' in batch_response2
    assert batch_response2['id'] == '123'
    assert batch_response1['status_code'] == batch_response2['status_code'] == 200

    response1 = client.json.get(
        path=reverse('api_dispatch_detail', api_name='v1', resource_name='roles', pk=role1.id),
    )
    response2 = client.json.get(
        path=reverse('api_dispatch_detail', api_name='v1', resource_name='roles', pk=role2.id),
    )

    assert batch_response1['body'] == response1.json()
    assert batch_response2['body'] == response2.json()


def test_batch_different_requesters(client, arda_users):
    client.login('gandalf')

    batch_url = reverse('api_dispatch_list', api_name='v1', resource_name='batch')

    response = client.json.post(
        path=batch_url,
        data=[
            {
                'method': 'get',
                'path': 'approverequests',
                'body': {'status': 'pending', '_requester': 'frodo'}
            }
        ],
    )

    assert response.status_code == 200
    data = response.json()
    assert data['responses'][0]['status_code'] == 200
