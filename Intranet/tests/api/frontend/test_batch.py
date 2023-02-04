# coding: utf-8

import pytest

from copy import deepcopy

from constance.test import override_config
from django.db import transaction

from idm.core.models import Role, RoleRequest
from idm.tests.utils import raw_make_role, refresh
from idm.utils import reverse


pytestmark = pytest.mark.django_db(transaction=True)


@pytest.fixture
def batch_url():
    return reverse('api_dispatch_list', api_name='v1', resource_name='batch')


ROLE_REQUEST_DATA = {
    'method': 'post',
    'path': 'rolerequests',
    'body': {
        'system': 'test1',
        'user': 'fantom',
        'path': '/proj1/manager/',
        'fields_data': {
            'passport-login': 'abc',
        },
    },
}


@pytest.mark.parametrize('data', [list(), dict()])
def test_empty_batch(client, users_for_test, data, batch_url):
    client.login('admin')
    response = client.json.post(batch_url, data=data)

    assert response.status_code == 400
    assert response.json()['message'] == 'Non-empty list of request objects must be supplied'


@pytest.mark.parametrize('IDM_OLD_BATCH_USERS', ['', 'admin'])
def test_recursive_batch(client, users_for_test, IDM_OLD_BATCH_USERS, batch_url):
    client.login('admin')
    with override_config(IDM_OLD_BATCH_USERS=IDM_OLD_BATCH_USERS):
        response = client.json.post(batch_url, data=[
            {
                'path': 'batch',
                'method': 'get',
            }
        ])

    assert response.status_code == 400 if 'admin' not in IDM_OLD_BATCH_USERS else 500
    response_data = response.json()

    assert len(response_data['responses']) == 1

    response1 = response_data['responses'][0]
    assert response1['status_code'] == 403


@pytest.mark.parametrize('IDM_OLD_BATCH_USERS', ['', 'admin'])
def test_nonexistent_resource(client, users_for_test, IDM_OLD_BATCH_USERS, batch_url):
    client.login('admin')

    with override_config(IDM_OLD_BATCH_USERS=IDM_OLD_BATCH_USERS):
        response = client.json.post(batch_url, data=[
            {
                'path': 'hamburgers',
                'method': 'get',
            }
        ])

    assert response.status_code == 400 if 'admin' not in IDM_OLD_BATCH_USERS else 500

    response_data = response.json()
    assert len(response_data['responses']) == 1
    assert response_data['responses'][0]['status_code'] == 404


def test_multiple_posts(client, users_with_roles, batch_url):
    client.login('admin')

    request1 = deepcopy(ROLE_REQUEST_DATA)
    request2 = deepcopy(ROLE_REQUEST_DATA)
    request2['body']['user'] = 'terran'
    request2['body']['fields_data']['passport-login'] = 'cba'
    response = client.json.post(batch_url, data=[request1, request2])

    assert response.status_code == 200
    assert RoleRequest.objects.count() == 2


@pytest.mark.parametrize('IDM_OLD_BATCH_USERS', ['', 'admin'])
def test_multiple_posts_with_error(client, users_with_roles, IDM_OLD_BATCH_USERS, batch_url):
    client.login('admin')

    request1 = deepcopy(ROLE_REQUEST_DATA)
    request2 = deepcopy(ROLE_REQUEST_DATA)
    request2['body']['user'] = 'terran'
    del request2['body']['fields_data']

    with override_config(IDM_OLD_BATCH_USERS=IDM_OLD_BATCH_USERS):
        response = client.json.post(batch_url, data=[request1, request2])

    assert response.status_code == 400 if 'admin' not in IDM_OLD_BATCH_USERS else 500
    assert RoleRequest.objects.count() == 0

    response_data = response.json()
    assert len(response_data['responses']) == 2
    response1, response2 = response_data['responses']

    assert response1['status_code'] == 201
    assert response2['status_code'] == 409


@pytest.mark.parametrize('IDM_OLD_BATCH_USERS', ['', 'frodo'])
def test_tolerate_4xx(client, arda_users, pt1_system, IDM_OLD_BATCH_USERS, batch_url):
    client.login('frodo')
    gandalf_role = raw_make_role(arda_users['gandalf'], pt1_system, {'project': 'proj1', 'role': 'manager'})
    old_time = gandalf_role.updated
    request_data = {
        'requests': [
            # good:
            {
                'method': 'post',
                'path': 'rolerequests',
                'body': {
                    'system': 'test1',
                    'user': 'frodo',
                    'path': '/proj1/manager/',
                    'fields_data': {
                        'passport-login': 'abc',
                    },
                },
            },
            # bad:
            {
                'method': 'get',
                'path': 'status/400/',
                'body': {
                    'touch_role': gandalf_role.pk,
                }
            },
            # bad:
            {
                'method': 'get',
                'path': 'somewhere_else',
            },
            # good:
            {
                'method': 'post',
                'path': 'rolerequests',
                'body': {
                    'system': 'test1',
                    'user': 'sam',
                    'path': '/proj1/manager/',
                    'fields_data': {
                        'passport-login': 'cba',
                    },
                },
            },
        ]
    }

    with override_config(IDM_OLD_BATCH_USERS=IDM_OLD_BATCH_USERS):
        tolerate_4xx = 'frodo' not in IDM_OLD_BATCH_USERS
        response = client.json.post(batch_url, data=request_data)
    assert response.status_code == (400 if tolerate_4xx else 500)
    assert len(response.json()['responses']) == 2
    assert RoleRequest.objects.count() == 0
    assert refresh(gandalf_role).updated == old_time

    request_data['requests'][1]['rollback_on_4xx'] = False
    tolerate_4xx = ('frodo' not in IDM_OLD_BATCH_USERS or
                    request_data['requests'][1].get('rollback_on_4xx', True)
                    )
    with override_config(IDM_OLD_BATCH_USERS=IDM_OLD_BATCH_USERS):
        response = client.json.post(batch_url, data=request_data)
    assert response.status_code == 400 if tolerate_4xx else 500
    assert len(response.json()['responses']) == (3 if tolerate_4xx else 2)
    assert RoleRequest.objects.count() == 0
    assert refresh(gandalf_role).updated == old_time

    request_data['requests'][2]['rollback_on_4xx'] = False
    tolerate_4xx = (
        'frodo' not in IDM_OLD_BATCH_USERS or
        request_data['requests'][2].get('rollback_on_4xx', True)
    )
    with override_config(IDM_OLD_BATCH_USERS=IDM_OLD_BATCH_USERS):
        response = client.json.post(batch_url, data=request_data)
    assert response.status_code == 400 if tolerate_4xx else 500
    assert len(response.json()['responses']) == (
        4 if tolerate_4xx else 2
    )
    # при rollback_on_4xx=False и кода 4xx остальные подзапросы записываются в базу
    assert RoleRequest.objects.count() == (
        2 if tolerate_4xx else 0
    )
    # а сам упавший подзапрос откатывается
    assert refresh(gandalf_role).updated == old_time


@pytest.mark.parametrize('IDM_OLD_BATCH_USERS', ['', 'frodo'])
@pytest.mark.parametrize('rollback_1', [False, True])
@pytest.mark.parametrize('rollback_2', [False, True])
def test_tolerate_4xx_with_400(client, arda_users, pt1_system, rollback_1, rollback_2, IDM_OLD_BATCH_USERS, batch_url):
    client.login('frodo')
    gandalf_role = raw_make_role(arda_users['gandalf'], pt1_system, {'project': 'proj1', 'role': 'manager'})
    old_time = gandalf_role.updated
    request_data = {
        'requests': [
            # 400:
            {
                'method': 'get',
                'path': 'status/400/',
                'rollback_on_4xx': rollback_1,
                'body': {
                    'touch_role': gandalf_role.pk,
                }
            },
            # good:
            {
                'method': 'post',
                'path': 'rolerequests',
                'body': {
                    'system': 'test1',
                    'user': 'frodo',
                    'path': '/proj1/manager/',
                    'fields_data': {
                        'passport-login': 'abc',
                    },
                },
                'rollback_on_4xx': rollback_2,
            }

        ]
    }

    with override_config(IDM_OLD_BATCH_USERS=IDM_OLD_BATCH_USERS):
        tolerate_4xx = 'frodo' not in IDM_OLD_BATCH_USERS and not rollback_1
        response = client.json.post(batch_url, data=request_data)
    assert response.status_code == 400 if tolerate_4xx else 500
    assert RoleRequest.objects.count() == 0 if rollback_1 else 1
    assert refresh(gandalf_role).updated == old_time


@pytest.mark.parametrize('IDM_OLD_BATCH_USERS', ['', 'frodo'])
@pytest.mark.parametrize('rollback_1', [False, True])
@pytest.mark.parametrize('rollback_2', [False, True])
@pytest.mark.parametrize('rollback_3', [False, True])
def test_tolerate_4xx_with_500(client, arda_users, pt1_system,
                               IDM_OLD_BATCH_USERS, rollback_1, rollback_2, rollback_3, batch_url):
    client.login('frodo')
    gandalf_role = raw_make_role(arda_users['gandalf'], pt1_system, {'project': 'proj1', 'role': 'manager'})
    old_time = gandalf_role.updated
    request_data = {
        'requests': [
            # good:
            {
                'method': 'post',
                'path': 'rolerequests',
                'body': {
                    'system': 'test1',
                    'user': 'frodo',
                    'path': '/proj1/manager/',
                    'fields_data': {
                        'passport-login': 'abc',
                    },
                },
                'rollback_on_4xx': rollback_1,
            },
            # 500:
            {
                'method': 'get',
                'path': 'status/500/',
                'rollback_on_4xx': rollback_2,
                'body': {
                    'touch_role': gandalf_role.pk,
                }
            },
            # good:
            {
                'method': 'post',
                'path': 'rolerequests',
                'body': {
                    'system': 'test1',
                    'user': 'sam',
                    'path': '/proj1/manager/',
                    'fields_data': {
                        'passport-login': 'cba',
                    },
                },
                'rollback_on_4xx': rollback_3,
            },
        ]
    }

    with override_config(IDM_OLD_BATCH_USERS=IDM_OLD_BATCH_USERS):
        response = client.json.post(batch_url, data=request_data)
    assert response.status_code == 500
    assert len(response.json()['responses']) == 2
    assert RoleRequest.objects.count() == 0
    assert refresh(gandalf_role).updated == old_time


@pytest.mark.parametrize('IDM_OLD_BATCH_USERS', ['', 'admin'])
def test_multiple_posts_with_leading_error(client, users_with_roles, IDM_OLD_BATCH_USERS, batch_url):
    client.login('admin')

    request1 = deepcopy(ROLE_REQUEST_DATA)
    del request1['body']['fields_data']
    request2 = deepcopy(ROLE_REQUEST_DATA)
    request2['body']['user'] = 'terran'

    with override_config(IDM_OLD_BATCH_USERS=IDM_OLD_BATCH_USERS):
        response = client.json.post(batch_url, data=[request1, request2])

    assert response.status_code == 400 if 'admin' not in IDM_OLD_BATCH_USERS else 500
    assert RoleRequest.objects.count() == 0

    response_data = response.json()
    assert len(response_data['responses']) == 1

    response1 = response_data['responses'][0]
    assert response1['status_code'] == 409


@pytest.mark.parametrize('IDM_OLD_BATCH_USERS', ['', 'admin'])
def test_invalid_request_params(client, users_with_roles, IDM_OLD_BATCH_USERS, batch_url):
    client.login('admin')

    request = deepcopy(ROLE_REQUEST_DATA)
    del request['method']

    with override_config(IDM_OLD_BATCH_USERS=IDM_OLD_BATCH_USERS):
        response = client.json.post(batch_url, data=[request])

    assert response.status_code == 400 if 'admin' not in IDM_OLD_BATCH_USERS else 500


def test_get_with_params(client, users_with_roles, batch_url):
    client.login('admin')

    request = {
        'method': 'get',
        'path': 'roles',
        'body': {
            'user': 'fantom',
        },
    }

    response_data = client.json.post(batch_url, data=[request]).json()
    assert len(response_data['responses']) == 1

    role_data = response_data['responses'][0]['body']['objects'][0]
    assert role_data['user']['username'] == 'fantom'


def test_savepoint_rollback(arda_users, pt1_system, batch_url):
    assert Role.objects.count() == 0
    with transaction.atomic():
        raw_make_role(arda_users['gandalf'], pt1_system, {'project': 'proj1', 'role': 'manager'})
        assert Role.objects.count() == 1
        with transaction.atomic():
            raw_make_role(arda_users['frodo'], pt1_system, {'project': 'proj1', 'role': 'manager'})
            assert Role.objects.count() == 2
            transaction.set_rollback(True)
    assert Role.objects.count() == 1
