# coding: utf-8
import pytest
import json
from mock import patch

from django.core.urlresolvers import reverse
from django.test.utils import override_settings

from intranet.dogma.dogma.core.models import Repo
from intranet.dogma.dogma.core.utils import get_node_queue

pytestmark = pytest.mark.django_db(transaction=True)


def test_create_repo_fail_no_source(client, ):
    client.login(username='vasya')
    url = reverse('api_v4:repo_viewset-list')
    data = {
        'url': 'https://github.com/smosker/test'
    }
    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    response_json = json.loads(response.content)
    assert response_json == {'message': 'No source with such host found'}
    assert response.status_code == 400


def test_create_repo_success(client, github):
    client.login(username='vasya')
    url = reverse('api_v4:repo_viewset-list')
    data = {
        'url': 'https://github.com/smosker/test'
    }
    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    response_json = json.loads(response.content)
    assert response.status_code == 200
    repo = Repo.objects.get(pk=response_json['id'])
    assert repo.name == response_json['name']
    assert repo.name == 'test'
    assert repo.owner == 'smosker'
    assert response_json['last_sync_fail_error_code'] is None
    assert response_json['source']['last_sync_fail_error_code'] is None


def test_create_repo_with_login_success(client, github):
    client.login(username='vasya')
    url = reverse('api_v4:repo_viewset-list')
    data = {
        'url': 'https://smth@github.com/smosker/test'
    }
    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    response_json = json.loads(response.content)
    assert response.status_code == 200
    repo = Repo.objects.get(pk=response_json['id'])
    assert repo.name == response_json['name']
    assert repo.name == 'test'
    assert repo.owner == 'smosker'
    assert response_json['last_sync_fail_error_code'] is None
    assert response_json['source']['last_sync_fail_error_code'] is None


def test_create_repo_with_git_success(client, github):
    client.login(username='vasya')
    url = reverse('api_v4:repo_viewset-list')
    data = {
        'url': 'https://github.com/smosker/test.git'
    }
    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    response_json = json.loads(response.content)
    assert response.status_code == 200
    repo = Repo.objects.get(pk=response_json['id'])
    assert repo.name == response_json['name']
    assert repo.name == 'test'
    assert repo.owner == 'smosker'
    assert response_json['last_sync_fail_error_code'] is None
    assert response_json['source']['last_sync_fail_error_code'] is None


def test_create_repo_with_excess_path_success(client, github):
    client.login(username='vasya')
    url = reverse('api_v4:repo_viewset-list')
    data = {
        'url': 'https://github.com/smosker/test/src/master'
    }
    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    response_json = json.loads(response.content)
    assert response.status_code == 200
    repo = Repo.objects.get(pk=response_json['id'])
    assert repo.name == response_json['name']
    assert repo.name == 'test'
    assert repo.owner == 'smosker'
    assert repo.url == 'https://github.com/smosker/test'

    data = {
        'url': 'https://github.com/smosker/test/smth/other/path'
    }

    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    response_json = json.loads(response.content)
    assert response.status_code == 200
    assert repo.id == response_json['id']


def test_doesnt_trim_path_for_gitlab_success(client, gitlab):
    client.login(username='vasya')
    url = reverse('api_v4:repo_viewset-list')
    data = {
        'url': 'https://gitlab.com/smosker/some/path/to/repo/'
    }
    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    response_json = json.loads(response.content)
    assert response.status_code == 200
    repo = Repo.objects.get(pk=response_json['id'])
    assert repo.name == response_json['name']
    assert repo.name == 'some/path/to/repo'
    assert repo.owner == 'smosker'
    assert repo.url == 'https://gitlab.com/smosker/some/path/to/repo'


def test_create_repo_with_wrong_path_fail(client, github):
    client.login(username='vasya')
    url = reverse('api_v4:repo_viewset-list')
    data = {
        'url': 'https://github.com/smosker'
    }
    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    assert response.status_code == 400
    response_json = json.loads(response.content)
    assert response_json == {'message': 'Request with incorrect url was made'}


def test_list_repo_not_for_current_org_fail(client, repo_github):
    client.login(username='vasya')
    url = reverse('api_v4:repo_viewset-list')
    with override_settings(IS_BUSINESS=True):
        response = client.get(url)

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert len(response_json['results']) == 0


def test_list_repo_for_current_org_success(client, repo_github, org):
    repo_github.connect_organization.add(org)

    client.login(username='vasya')
    with override_settings(IS_BUSINESS=True):
        url = reverse('api_v4:repo_viewset-list')
    response = client.get(url)

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert len(response_json['results']) == 1


def test_get_repo_for_current_org_success(client, repo_github, org):
    repo_github.connect_organization.add(org)
    client.login(username='vasya')
    url = reverse('api_v4:repo_viewset-detail', kwargs={'pk': repo_github.id})
    with override_settings(IS_BUSINESS=True):
        response = client.get(url)

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert response.status_code == 200
    repo = Repo.objects.get(pk=response_json['id'])
    assert repo.name == response_json['name']


def test_get_repo_not_for_current_org_success(client, repo_github):

    client.login(username='vasya')
    url = reverse('api_v4:repo_viewset-detail', kwargs={'pk': repo_github.id})
    with override_settings(IS_BUSINESS=True):
        response = client.get(url)

    assert response.status_code == 404


def test_disable_repo_success(client, repo_github, org):
    repo_github.connect_organization.add(org)
    client.login(username='vasya')

    url = reverse('api_v4:repo_viewset-list')
    with override_settings(IS_BUSINESS=True):
        response = client.get(url)

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert len(response_json['results']) == 1

    url = reverse('api_v4:repo_viewset-disable', kwargs={'pk': repo_github.id})
    with override_settings(IS_BUSINESS=True):
        response = client.post(url)

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert response_json == {'status': 'ok'}

    repo = Repo.objects.get(pk=repo_github.id)
    assert repo.is_active is False
    assert repo.connect_organization.count() == 0

    url = reverse('api_v4:repo_viewset-list')
    with override_settings(IS_BUSINESS=True):
        response = client.get(url)
    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert len(response_json['results']) == 0


def test_change_credentials_repo_fail(client, repo_github):
    client.login(username='vasya')
    url = reverse('api_v4:repo_viewset-change-credentials', kwargs={'pk': repo_github.id})
    response = client.post(url, json.dumps({}), content_type='application/json', )

    assert response.status_code == 400
    response_json = json.loads(response.content)
    assert response_json == {'message': 'You have to provide credentials'}


def test_change_credentials_wrong_id_repo_fail(client, repo_github):
    client.login(username='vasya')
    url = reverse('api_v4:repo_viewset-change-credentials', kwargs={'pk': repo_github.id})
    data = {'credentials': [5555]}
    response = client.post(url, json.dumps(data), content_type='application/json', )

    assert response.status_code == 400
    response_json = json.loads(response.content)
    assert response_json == {'message': 'No credentials with such ids'}


def test_change_credentials_repo_success(client, repo_github, credential, ):
    repo_github.clone_attempt = 20
    repo_github.save()
    assert repo_github.credentials.exists() is False

    client.login(username='vasya')
    url = reverse('api_v4:repo_viewset-change-credentials', kwargs={'pk': repo_github.id})
    data = {'credentials': [credential.id]}
    with patch('intranet.dogma.dogma.core.tasks.clone_repo') as clone_mock:
        response = client.post(url, json.dumps(data), content_type='application/json',)
        clone_mock.apply_async.assert_called_with(args=[repo_github.id], queue=get_node_queue('clone'))

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert len(response_json['credentials']) == 1
    assert response_json['credentials'][0]['name'] == credential.name
    changed_repo = Repo.objects.get(pk=repo_github.id)
    assert changed_repo.credentials.count() == 1
    assert changed_repo.credentials.first() == credential
    assert changed_repo.clone_attempt == 0


def test_change_credentials_repo_for_not_current_org_fail(client, repo_github, credential):
    assert repo_github.credentials.exists() is False

    client.login(username='vasya')
    url = reverse('api_v4:repo_viewset-change-credentials', kwargs={'pk': repo_github.id})
    data = {'credentials': [credential.id]}
    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    assert response.status_code == 404
    assert repo_github.credentials.exists() is False
