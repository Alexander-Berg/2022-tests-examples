# coding: utf-8
import pytest
import json

from mock import patch
from django.core.urlresolvers import reverse
from django.test.utils import override_settings

from intranet.dogma.dogma.core.models import Credential, Repo, OrganisationsToClone
from intranet.dogma.dogma.core.utils import get_node_queue

pytestmark = pytest.mark.django_db(transaction=True)


def test_create_credential_fail(client, ):
    client.login(username='vasya')
    url = reverse('api_v4:credential_viewset-list')
    data = {
        'name': 'test'
    }
    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    response_json = json.loads(response.content)
    assert response_json == {'message': {'auth_data': ['This field is required.'],
                                         'auth_type': ['This field is required.'],
                                         }
                             }
    assert response.status_code == 400


def test_create_credential_success(client, org):
    client.login(username='vasya')
    assert Credential.objects.count() == 0
    url = reverse('api_v4:credential_viewset-list')
    data = {
        'name': 'test',
        'auth_type': 'token',
        'auth_data': {'token': 'some token'},
    }
    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    response_json = json.loads(response.content)
    assert response.status_code == 201
    assert response_json['name'] == 'test'
    assert 'auth_data' not in response_json
    assert Credential.objects.count() == 1
    credential = Credential.objects.first()
    assert credential.connect_organization == org
    assert credential.auth_data == data['auth_data']


def test_change_credential_success(client, credential, org):

    client.login(username='vasya')
    url = reverse('api_v4:credential_viewset-detail', kwargs={'pk': credential.id})
    assert credential.name != 'new one'
    data = {
        'name': 'new one',
    }
    with override_settings(IS_BUSINESS=True):
        response = client.patch(url, json.dumps(data), content_type='application/json', )

    response_json = json.loads(response.content)
    assert response.status_code == 200
    assert response_json['name'] == data['name']
    credential = Credential.objects.get(pk=credential.id)
    assert credential.name == data['name']


def test_change_credential_wrong_org_fail(client, another_credential, ):
    client.login(username='vasya')
    url = reverse('api_v4:credential_viewset-detail', kwargs={'pk': another_credential.id})
    assert another_credential.name != 'new one'
    data = {
        'name': 'new one',
    }
    with override_settings(IS_BUSINESS=True):
        response = client.patch(url, json.dumps(data), content_type='application/json', )

    assert response.status_code == 404
    credential = Credential.objects.get(pk=another_credential.id)
    assert credential.name != data['name']


def test_list_credential_not_for_current_org_fail(client, another_credential):
    client.login(username='vasya')
    url = reverse('api_v4:credential_viewset-list')

    with override_settings(IS_BUSINESS=True):
        response = client.get(url)

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert len(response_json['results']) == 0


def test_list_credential_for_current_org_success(client, credential, ):
    client.login(username='vasya')
    with override_settings(IS_BUSINESS=True):
        url = reverse('api_v4:credential_viewset-list')
    response = client.get(url)

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert len(response_json['results']) == 1


def test_change_credential_start_tasks_success(transactional_db, client, credential, org,
                                               repo_github, organization, org_repo, clone_github,
                                               ):
    repo_github.credentials.add(credential)
    organization.credentials.add(credential)

    client.login(username='vasya')
    url = reverse('api_v4:credential_viewset-detail', kwargs={'pk': credential.id})
    data = {
        'name': 'new one',
    }

    with patch('intranet.dogma.dogma.core.tasks.clone_repo') as clone_mock:
        with patch('intranet.dogma.dogma.core.tasks.fetch_clone') as fetch_mock:
            with override_settings(IS_BUSINESS=True):
                response = client.patch(url, json.dumps(data), content_type='application/json', )
                clone_mock.apply_async.assert_called_with(args=[repo_github.id], queue=get_node_queue('clone'))
                fetch_mock.apply_async.assert_called_with(args=[clone_github.id], queue=get_node_queue('clone'))

    assert response.status_code == 200
