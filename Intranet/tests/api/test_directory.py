from unittest import mock
import pytest

from django.conf import settings
from django.urls import reverse
from intranet.search.core.models import Organization, PushRecord

from intranet.search.core.sources.directory import client as dir_client
from intranet.search.core.storages.organization import org_model_to_dict
from intranet.search.tests.helpers import models_helpers as mh


pytestmark = [
    pytest.mark.django_db(transaction=False),
    pytest.mark.usefixtures('bisearch_app'),
]


api_url = reverse('directory-event-add')
sync_api_url = reverse('directory-sync')


def assert_response_ok(response, push):
    assert response.status_code == 200
    assert response.json() == {'status': 'ok', 'push_id': push.id}


def assert_push_record_created(data, status=PushRecord.STATUS_NEW):
    push_filter = PushRecord.objects.filter(organization__directory_id=data['org_id'],
                                            type=data['event'], status=status)
    assert push_filter.exists()
    push = push_filter.get()
    assert push.meta == data['object']


@pytest.mark.parametrize('event',
                         list(dir_client.ORGANIZATION_EVENTS) +
                         list(dir_client.DOMAIN_EVENTS) +
                         list(dir_client.USER_EVENTS) +
                         list(dir_client.DEPARTMENT_EVENTS) +
                         list(dir_client.GROUP_EVENTS))
@mock.patch('intranet.search.core.tasks.directory.sync_directory_organization.delay')
def test_unknown_organization(sync_mock, event, api_client):
    """ Любое событие о неизвестной организации запускает её полную синхронизацию """
    data = {'org_id': 1, 'event': event, 'object': {}}
    r = api_client.post(api_url, json=data)

    assert Organization.objects.filter(directory_id=data['org_id']).exists()
    assert_push_record_created(data)

    org = Organization.objects.get(directory_id=data['org_id'])
    push = PushRecord.objects.get(organization=org)
    sync_mock.assert_called_once_with(org_model_to_dict(org), push_id=push.id,
                                      force_reindex=True, event=event)
    assert_response_ok(r, push)


@pytest.mark.parametrize('event', dir_client.USER_EVENTS)
@mock.patch('intranet.search.core.tasks.directory.handle_single_push')
def test_user_event(push_mock, event, api_client):
    org = mh.Organization()
    uid = 'some_uid'
    data = {'org_id': org.directory_id, 'event': event, 'object': {'id': uid}}

    r = api_client.post(api_url, json=data)

    assert_push_record_created(data)
    push = PushRecord.objects.get(organization=org)
    assert_response_ok(r, push)
    push_mock.assert_called_once_with(push.id, search='directory', index='', data=data['object'],
                                      action='create', organization_id=org.id)


@mock.patch('intranet.search.core.tasks.directory.handle_single_push')
def test_user_deleted_event(push_mock, api_client):
    org = mh.Organization()
    data = {'org_id': org.directory_id, 'event': dir_client.USER_DELETED_EVENT, 'object': {'id': 'some_id'}}

    r = api_client.post(api_url, json=data)

    assert_push_record_created(data)
    push = PushRecord.objects.get(organization=org)
    assert_response_ok(r, push)
    push_mock.assert_called_once_with(push.id, search='directory', index='', data=data['object'],
                                      action='delete', organization_id=org.id)


@pytest.mark.parametrize('event', dir_client.GROUP_EVENTS)
@mock.patch('intranet.search.core.tasks.directory.handle_single_push')
@mock.patch('intranet.search.core.tasks.directory.handle_indexation_push')
def test_group_event(indexation_mock, push_mock, event, api_client):
    org = mh.Organization()
    data = {'org_id': org.directory_id, 'event': event, 'object': {'id': 'some_id'}}

    r = api_client.post(api_url, json=data)

    assert_push_record_created(data)
    push = PushRecord.objects.get(organization=org)
    assert_response_ok(r, push)
    push_mock.assert_called_once_with(push.id, search='directory', index='groups',
                                      data=data['object'], action='create', organization_id=org.id)


@pytest.mark.parametrize('event', dir_client.DEPARTMENT_EVENTS)
@mock.patch('intranet.search.core.tasks.directory.handle_single_push')
@mock.patch('intranet.search.core.tasks.directory.handle_indexation_push')
def test_department_event(indexation_mock, push_mock, event, api_client):
    org = mh.Organization()
    data = {'org_id': org.directory_id, 'event': event, 'object': {'id': 'some_id'}}

    r = api_client.post(api_url, json=data)

    assert_push_record_created(data)
    push = PushRecord.objects.get(organization=org)
    assert_response_ok(r, push)
    push_mock.assert_called_once_with(push.id, search='directory', index='departments',
                                      data=data['object'], action='create', organization_id=org.id)


@mock.patch('intranet.search.core.tasks.directory.handle_single_push')
@mock.patch('intranet.search.core.tasks.directory.handle_indexation_push')
def test_department_property_changed_event(indexation_mock, push_mock, api_client):
    """ При изменении названия департамента переиндексируем ещё и всех людей в нем
    """
    org = mh.Organization()
    data = {'org_id': org.directory_id, 'event': dir_client.DEPARTMENT_PROPERTY_CHANGED_EVENT,
            'object': {'id': 'some_id', 'title': 'new_title'}}

    r = api_client.post(api_url, json=data)

    assert_push_record_created(data)
    push = PushRecord.objects.get(organization=org)
    assert_response_ok(r, push)
    push_mock.assert_called_once_with(push.id, search='directory', index='departments',
                                      data=data['object'], action='create', organization_id=org.id)

    comment = 'event={}, department={}, org_directory_id={}'.format(
        data['event'], data['object']['id'], org.directory_id)
    indexation_mock.assert_called_once_with(push.id, search='directory', index='', comment=comment,
                                            keys=['department:{}'.format(data['object']['id'])],
                                            organization_id=org.id)


@mock.patch('intranet.search.core.tasks.directory.handle_single_push')
@mock.patch('intranet.search.core.tasks.directory.handle_indexation_push')
def test_group_property_changed_event(indexation_mock, push_mock, api_client):
    """ При изменении названия гуппы переиндексируем ещё и всех людей в нем
    """
    org = mh.Organization()
    data = {'org_id': org.directory_id, 'event': dir_client.GROUP_PROPERTY_CHANGED_EVENT,
            'object': {'id': 'some_id', 'title': 'new_title'}}

    r = api_client.post(api_url, json=data)

    assert_push_record_created(data)
    push = PushRecord.objects.get(organization=org)
    assert_response_ok(r, push)
    push_mock.assert_called_once_with(push.id, search='directory', index='groups',
                                      data=data['object'], action='create', organization_id=org.id)

    comment = 'event={}, group={}, org_directory_id={}'.format(
        data['event'], data['object']['id'], org.directory_id)
    indexation_mock.assert_called_once_with(push.id, search='directory', index='', comment=comment,
                                            keys=['group:{}'.format(data['object']['id'])],
                                            organization_id=org.id)


@pytest.mark.parametrize('event', dir_client.SERVICE_EVENTS)
@pytest.mark.parametrize('service', settings.ISEARCH['searches']['base'].keys())
@mock.patch('intranet.search.core.tasks.directory.sync_directory_organization.delay')
def test_known_service_event(sync_mock, service, event, api_client):
    org = mh.Organization()
    data = {'org_id': org.directory_id, 'event': event, 'object': {'slug': service}}

    r = api_client.post(api_url, json=data)

    assert_push_record_created(data)
    push = PushRecord.objects.get(organization=org)
    assert_response_ok(r, push)
    sync_mock.assert_called_once_with(org_model_to_dict(org), push_id=push.id, revision=0, **data)


@pytest.mark.parametrize('event', dir_client.SERVICE_EVENTS)
@mock.patch('intranet.search.core.tasks.directory.sync_directory_organization.delay')
def test_unknown_service_event(sync_mock, event, api_client):
    org = mh.Organization()
    data = {'org_id': org.directory_id, 'event': event, 'object': {'slug': 'unknown_service'}}

    response = api_client.post(api_url, json=data)
    assert response.status_code == 200
    assert response.json() == {'status': 'ok', 'push_id': None, 'details': 'Unknown service "unknown_service"'}

    pushes = PushRecord.objects.filter(organization=org)
    assert not pushes.exists()
    assert sync_mock.call_count == 0


@mock.patch('intranet.search.core.tasks.directory.sync_directory_organization.delay')
def test_sync_existing_org(sync_mock, api_client):
    org = mh.Organization()
    r = api_client.post(sync_api_url, json={'org_id': org.directory_id})

    assert r.status_code == 200
    assert_push_record_created(
        {'org_id': org.directory_id, 'event': 'force_sync', 'object': {}},
        status=PushRecord.STATUS_NEW
    )
    push = PushRecord.objects.get(organization=org)
    sync_mock.assert_called_once_with(
        org_model_to_dict(org), push_id=push.id, force_reindex=True, event='force_sync'
    )


@mock.patch('intranet.search.core.tasks.directory.sync_directory_organization.delay')
def test_sync_new_org(sync_mock, api_client):
    org_id = 1
    r = api_client.post(sync_api_url, json={'org_id': org_id})

    assert r.status_code == 200
    assert_push_record_created(
        {'org_id': org_id, 'event': 'force_sync', 'object': {}},
        status=PushRecord.STATUS_NEW
    )
    org = Organization.objects.filter(directory_id=org_id).first()
    assert org is not None
    push = PushRecord.objects.get(organization=org)
    sync_mock.assert_called_once_with(
        org_model_to_dict(org), push_id=push.id, force_reindex=True, event='force_sync'
    )


@mock.patch('intranet.search.core.tasks.directory.sync_directory_organization.delay')
def test_sync_no_org(sync_mock, api_client):
    r = api_client.post(sync_api_url, json={})
    assert r.status_code == 400
