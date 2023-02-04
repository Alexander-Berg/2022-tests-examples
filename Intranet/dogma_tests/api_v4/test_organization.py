# coding: utf-8
import pytest
import json
from mock import patch

from django.core.urlresolvers import reverse
from django.test.utils import override_settings

from intranet.dogma.dogma.core.models import OrganisationsToClone, Repo
from intranet.dogma.dogma.core.utils import get_node_queue

pytestmark = pytest.mark.django_db(transaction=True)


def test_create_organization_fail_no_source(client, ):
    client.login(username='vasya')
    url = reverse('api_v4:organization_viewset-list')
    data = {
        'url': 'https://github.com/smosker'
    }
    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    response_json = json.loads(response.content)
    assert response_json == {'message': 'No source with such host found'}
    assert response.status_code == 400


def test_create_organization_success(client, github):
    client.login(username='vasya')
    url = reverse('api_v4:organization_viewset-list')
    data = {
        'url': 'https://github.com/smosker'
    }
    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    response_json = json.loads(response.content)
    assert response.status_code == 200
    organization = OrganisationsToClone.objects.get(pk=response_json['id'])
    assert organization.name == response_json['name']
    assert organization.name == 'smosker'


def test_create_bitbucket_organization_success(client, bitbucket):
    client.login(username='vasya')
    url = reverse('api_v4:organization_viewset-list')
    data = {
        'url': 'https://bitbucket.org/account/user/dogmatestb2b/projects/NEWTEST'
    }
    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    response_json = json.loads(response.content)
    assert response.status_code == 200
    organization = OrganisationsToClone.objects.get(pk=response_json['id'])
    assert organization.name == response_json['name']
    assert organization.name == 'dogmatestb2b-NEWTEST'


def test_list_organization_not_for_current_org_fail(client, organization):
    client.login(username='vasya')
    url = reverse('api_v4:organization_viewset-list')
    with override_settings(IS_BUSINESS=True):
        response = client.get(url)

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert len(response_json['results']) == 0


def test_list_organization_for_current_org_success(client, organization, org):
    organization.connect_organization.add(org)

    client.login(username='vasya')
    with override_settings(IS_BUSINESS=True):
        url = reverse('api_v4:organization_viewset-list')
    response = client.get(url)

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert len(response_json['results']) == 1


def test_get_organization_for_current_org_success(client, organization, org):
    organization.connect_organization.add(org)
    client.login(username='vasya')
    url = reverse('api_v4:organization_viewset-detail', kwargs={'pk': organization.id})
    with override_settings(IS_BUSINESS=True):
        response = client.get(url)

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert response.status_code == 200
    organization = OrganisationsToClone.objects.get(pk=response_json['id'])
    assert organization.name == response_json['name']


def test_get_organization_not_for_current_org_success(client, organization):

    client.login(username='vasya')
    url = reverse('api_v4:organization_viewset-detail', kwargs={'pk': organization.id})
    with override_settings(IS_BUSINESS=True):
        response = client.get(url)

    assert response.status_code == 404


def test_disable_organization_success(client, organization, org):
    organization.connect_organization.add(org)
    client.login(username='vasya')

    url = reverse('api_v4:organization_viewset-list')
    with override_settings(IS_BUSINESS=True):
        response = client.get(url)

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert len(response_json['results']) == 1

    url = reverse('api_v4:organization_viewset-disable', kwargs={'pk': organization.id})
    with override_settings(IS_BUSINESS=True):
        response = client.post(url)

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert response_json == {'status': 'ok'}

    organization = OrganisationsToClone.objects.get(pk=organization.id)
    assert organization.is_active is False
    assert organization.connect_organization.count() == 0

    url = reverse('api_v4:organization_viewset-list')
    with override_settings(IS_BUSINESS=True):
        response = client.get(url)
    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert len(response_json['results']) == 0


def disable_related_repo_success(client, organization, org, repo_github):
    repo_github.organisation = organization
    repo_github.save()
    organization.connect_organization.add(org)
    repo_github.connect_organization.add(org)
    assert repo_github.is_active() is True

    url = reverse('api_v4:organization_viewset-disable', kwargs={'pk': organization.id})
    with override_settings(IS_BUSINESS=True):
        response = client.post(url)

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert response_json == {'status': 'ok'}

    repo = Repo.objects.get(pk=repo_github.id)
    assert repo.connect_organization.exists() is False
    assert repo.is_active is False

    organisation = OrganisationsToClone.objects.get(pk=organization.id)
    assert organisation.connect_organization.exists() is False
    assert organisation.is_active is False


def test_change_credentials_organization_fail(client, organization):
    client.login(username='vasya')
    url = reverse('api_v4:organization_viewset-change-credentials', kwargs={'pk': organization.id})
    response = client.post(url, json.dumps({}), content_type='application/json', )

    assert response.status_code == 400
    response_json = json.loads(response.content)
    assert response_json == {'message': 'You have to provide credentials'}


def test_change_credentials_wrong_id_organization_fail(client, organization):
    client.login(username='vasya')
    url = reverse('api_v4:organization_viewset-change-credentials', kwargs={'pk': organization.id})
    data = {'credentials': [5555]}
    response = client.post(url, json.dumps(data), content_type='application/json', )

    assert response.status_code == 400
    response_json = json.loads(response.content)
    assert response_json == {'message': 'No credentials with such ids'}


def test_change_credentials_organization_success(client, organization, credential, org_repo, repo_github, ):

    assert organization.credentials.exists() is False

    client.login(username='vasya')
    url = reverse('api_v4:organization_viewset-change-credentials', kwargs={'pk': organization.id})
    data = {'credentials': [credential.id]}
    with patch('intranet.dogma.dogma.core.tasks.clone_repo') as clone_mock:
        response = client.post(url, json.dumps(data), content_type='application/json', )
        clone_mock.apply_async.assert_called_with(args=[org_repo.id], queue=get_node_queue('clone'))

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert len(response_json['credentials']) == 1
    assert response_json['credentials'][0]['name'] == credential.name
    organization = OrganisationsToClone.objects.get(pk=organization.id)
    assert organization.credentials.count() == 1
    assert organization.credentials.first() == credential


def test_change_credentials_organization_for_not_current_org_fail(client, organization, credential):
    assert organization.credentials.exists() is False

    client.login(username='vasya')
    url = reverse('api_v4:organization_viewset-change-credentials', kwargs={'pk': organization.id})
    data = {'credentials': [credential.id]}
    with override_settings(IS_BUSINESS=True):
        response = client.post(url, json.dumps(data), content_type='application/json', )

    assert response.status_code == 404
    assert organization.credentials.exists() is False
