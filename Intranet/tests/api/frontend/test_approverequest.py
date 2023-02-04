# coding: utf-8
import itertools
import operator
import random
import string
from textwrap import dedent
from typing import Optional, Dict

import pytest
from django.conf import settings
from ids.exceptions import BackendError


from idm.core.constants.approverequest import APPROVEREQUEST_DECISION
from idm.core.constants.role import ROLE_STATE
from idm.core.models import Role, ApproveRequest, Approve, RoleRequest
from idm.core.models.appmetrica import AppMetrica
from idm.core.models.metrikacounter import MetrikaCounter
from idm.inconsistencies.models import Inconsistency
from idm.tests.models.test_metrikacounter import generate_counter_record
from idm.tests.models.test_appmetrica import generate_app_record
from idm.tests.utils import (set_workflow, add_perms_by_role, refresh, mock_all_roles, assert_num_model_queries,
                             assert_approvers, mock_ids_repo, MockedTrackerIssue, create_user, create_system,
                             raw_make_role)
from idm.users.models import Group
from idm.utils import reverse
from idm.utils.curl import Response

pytestmark = pytest.mark.django_db


@pytest.fixture
def approve_requests_url():
    return reverse('api_dispatch_list', api_name='frontend', resource_name='approverequests')


@pytest.fixture
def approve_requests_bulk_url():
    return reverse('api_approve_bulk', api_name='frontend', resource_name='approverequests')


@pytest.fixture
def actions_url():
    return reverse('api_dispatch_list', api_name='frontend', resource_name='actions')


def get_approve_request_url(request_id):
    return reverse('api_dispatch_detail', api_name='frontend', resource_name='approverequests', pk=request_id)


def test_get_list(client, simple_system, more_users_for_test, approve_requests_url):
    """
    GET /frontend/approverequests/
    """
    (art, fantom, terran, zerg, protoss, admin) = more_users_for_test
    set_workflow(simple_system, 'approvers = [approver("terran") | approver("protoss"), approver("zerg")]')

    def get_approve_requests(params=None, expected_status=200):
        if params is None:
            params = {}
        response = client.json.get(approve_requests_url, params)
        assert response.status_code == expected_status, response.content
        return response.json()

    client.login('terran')
    get_approve_requests(expected_status=400)

    # запрашиваем роль для art
    superuser_role = Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)

    # у террана она в ожидающих
    data = get_approve_requests({'status': 'pending', 'approver': 'terran'})
    assert data['meta']['total_count'] == 1

    role_req = data['objects'][0]
    expected_keys = {
        'id',
        'role',
        'role_request',
        'approver',
        'requester',

        'approved',
        'decision',
        'is_done',

        'status',
        'human_status',
        'reason',
        'human_reason',
        'comment',
    }
    assert role_req.keys() == expected_keys
    assert role_req['is_done'] is False
    assert role_req['requester']['username'] == 'art'

    # у админа нету
    client.login('admin')
    data = get_approve_requests({'status': 'pending', 'approver': 'admin'})
    assert data['meta']['total_count'] == 0
    assert len(data['objects']) == 0

    # но есть при точном указании
    data = get_approve_requests({'status': 'pending', 'approver': 'terran'})
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['requester']['username'] == 'art'

    # или при выборе всех
    data = get_approve_requests({'status': 'pending'})
    assert data['meta']['total_count'] == 3
    assert data['objects'][0]['requester']['username'] == 'art'

    # эмулируем аппрув террана
    terran_approve_request = ApproveRequest.objects.select_related_for_set_decided().get(
        approve__role_request__role=superuser_role, approver__username='terran')
    terran_approve_request.set_approved(terran)

    # у террана она в завершенных
    client.login('terran')
    data = get_approve_requests({'status': 'pending', 'approver': 'terran'})
    assert data['meta']['total_count'] == 0

    data = get_approve_requests({'status': 'processed', 'approver': 'terran'})
    assert data['meta']['total_count'] == 1
    role_req = data['objects'][0]
    assert role_req['is_done'] is False

    # у зерга она в ожидающих
    client.login('zerg')
    data = get_approve_requests({'status': 'pending', 'approver': 'zerg'})
    assert data['meta']['total_count'] == 1

    role_req = data['objects'][0]
    assert role_req['is_done'] is False
    assert role_req['requester']['username'] == 'art'

    # эмулируем аппрув зерга
    zerg_approve_request = ApproveRequest.objects.select_related_for_set_decided().get(
        approve__role_request__role=superuser_role, approver__username='zerg')
    zerg_approve_request.set_approved(zerg)

    # у зерга она в завершенных
    client.login('zerg')
    data = get_approve_requests({'status': 'pending', 'approver': 'zerg'})
    assert data['meta']['total_count'] == 0

    data = get_approve_requests({'status': 'processed', 'approver': 'zerg'})
    assert data['meta']['total_count'] == 1
    role_req = data['objects'][0]
    assert role_req['is_done'] is True
    assert role_req['requester']['username'] == 'art'


def test_get_list__filters(client, simple_system, more_users_for_test, department_structure,
                                    approve_requests_url):
    """
    GET /frontend/approverequests/
    """
    (art, fantom, terran, zerg, protoss, admin) = more_users_for_test
    set_workflow(simple_system,
                 'approvers = [approver("terran") | approver("protoss"), approver("zerg")]',
                 'approvers = [approver("terran")]')

    def get_approve_requests(params=None, expected_status=200):
        if params is None:
            params = {}
        response = client.json.get(approve_requests_url, params)
        assert response.status_code == expected_status, response.content
        return response.json()

    Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)
    Role.objects.request_role(fantom, fantom, simple_system, None, {'role': 'manager'}, None)

    client.login('terran')
    data = get_approve_requests({'status': 'pending', 'approver': 'terran'})
    assert data['meta']['total_count'] == 2

    data = get_approve_requests({'status': 'pending', 'user': 'fantom', 'approver': 'terran'})
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['requester']['username'] == 'fantom'

    data = get_approve_requests({'status': 'pending', 'user': 'art,fantom', 'approver': 'terran'})
    assert data['meta']['total_count'] == 2

    # теперь если пользователь не найден, то мы будем просто отдавать пустой список, а не 400
    data = get_approve_requests({'status': 'pending', 'user': 'unknown', 'approver': 'terran'}, expected_status=200)
    assert data['meta']['total_count'] == 0

    data = get_approve_requests({'status': 'pending', 'user': 'protoss', 'approver': 'terran'})
    assert data['meta']['total_count'] == 0

    data = get_approve_requests({'status': 'pending', 'system': 'simple', 'path': '/manager/', 'approver': 'terran'})
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['requester']['username'] == 'fantom'

    client.login('terran')
    data = get_approve_requests({'status': 'pending', 'system': 'simple', 'approver': 'terran'})
    assert data['meta']['total_count'] == 2

    get_approve_requests({'status': 'pending', 'system': 'unknown', 'approver': 'terran'}, expected_status=400)

    add_perms_by_role('responsible', art, simple_system)
    middle_earth = Group.objects.get(slug='middle-earth')
    associations = Group.objects.get(slug='associations')
    Role.objects.request_role(art, middle_earth, simple_system, None, {'role': 'superuser'}, None)
    Role.objects.request_role(art, associations, simple_system, None, {'role': 'superuser'}, None)

    data = get_approve_requests({
        'status': 'pending',
        'system': 'simple',
        'approver': 'terran'
    })
    assert data['meta']['total_count'] == 4

    data = get_approve_requests({
        'status': 'pending',
        'group': middle_earth.external_id,
        'approver': 'terran'
    })
    assert data['meta']['total_count'] == 1

    # проверяем логику ИЛИ для user и group
    data = get_approve_requests({
        'status': 'pending',
        'group': middle_earth.external_id,
        'user': 'fantom',
        'approver': 'terran'
    })
    assert data['meta']['total_count'] == 2


def test_get_list__filter_nodeset(client, complex_system_with_nodesets, more_users_for_test,
                                            approve_requests_url):
    (art, fantom, terran, zerg, protoss, admin) = more_users_for_test
    set_workflow(complex_system_with_nodesets,
                 'approvers = [approver("terran") | approver("protoss"), approver("zerg")]',
                 'approvers = [approver("terran")]')

    def get_approve_requests(params=None, expected_status=200):
        if params is None:
            params = {}
        response = client.json.get(approve_requests_url, params)
        assert response.status_code == expected_status, response.content
        return response.json()

    Role.objects.request_role(art, art, complex_system_with_nodesets, None,
                              {'project': 'subs', 'role': 'developer'},
                              {'passport-login': 'art', 'field_1': '1'})

    client.login('terran')
    data = get_approve_requests({
        'status': 'pending',
        'system': 'complex',
        'nodeset': 'developer_id',
        'approver': 'terran'
    })
    assert data['meta']['total_count'] == 1

    data = get_approve_requests({
        'status': 'pending',
        'system': 'complex',
        'nodeset': 'developer_id,manager_id',
        'approver': 'terran'
    })
    assert data['meta']['total_count'] == 1

    data = get_approve_requests({
        'status': 'pending',
        'system': 'complex',
        'nodeset': 'somegroup',
        'approver': 'terran'
    }, expected_status=400)
    assert data == {
        'message': 'Invalid data sent',
        'error_code': 'BAD_REQUEST',
        'errors': {
            'nodeset': ['Набор ролей не найден']
        },
    }


@pytest.mark.parametrize('approver_name', ['varda', 'legolas'])
@pytest.mark.parametrize('approve_data', [{'approve': True}, {'decision': 'approve'}])
def test_post_detail__approve(client, simple_system, arda_users, approve_requests_url, actions_url,
                              approver_name, approve_data):
    """
    POST /frontend/approverequests/<request_id> - approve
    """

    set_workflow(simple_system, 'approvers = [approver("legolas") | approver("saruman"),'
                                'approver("varda") | approver("legolas")]')

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'superuser'}, None)

    client.login(approver_name)
    data = client.json.get(approve_requests_url, {'status': 'pending', 'approver': approver_name}).json()

    _approve_data = data['objects'][0]

    assert data['meta']['total_count'] == 1
    assert not _approve_data['is_done']

    request_id = _approve_data['id']
    request = ApproveRequest.objects.get(id=request_id)
    assert request.parent_id is None

    role_data = client.json.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=role.pk)
    ).json()
    assert role_data['approve_request']['id'] == request_id

    # BadRequest
    response = client.json.post(get_approve_request_url(request_id), {'someerrorkey': True})
    assert response.status_code == 400

    response = client.json.post(get_approve_request_url(request_id), approve_data)
    assert response.status_code == 204

    data = client.json.get(approve_requests_url, {'status': 'processed', 'approver': approver_name}).json()
    _approve_data = data['objects'][0]

    assert _approve_data['approved'] is True
    assert _approve_data['decision'] == 'approve'
    role.refresh_from_db()
    if approver_name == 'legolas':
        assert role.state == 'granted'
    else:
        assert role.state == 'requested'


def test_post_detail__approve_with_comment(client, simple_system, arda_users, approve_requests_url, actions_url):
    """
    POST /frontend/approverequests/<request_id> - approve with comment
    """
    comment = 'Добро пожаловать на Молдавскую Землю!'
    set_workflow(simple_system, 'approvers = [approver("legolas") | approver("saruman"), approver("varda")]')

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'superuser'}, None)
    client.login('saruman')
    data = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'saruman'}).json()
    assert len(data['objects']) == 1
    superuser_req_data = data['objects'][0]
    response = client.json.post(get_approve_request_url(superuser_req_data['id']), {
        'approve': True,
        'comment': comment
    })

    assert response.status_code == 204
    role = refresh(role)
    assert role.state == 'requested'
    action = role.actions.get(action='approve')
    assert action.comment == comment

    history = client.json.get(actions_url, {'role': role.pk}).json()
    assert history['objects'][0]['comment'] == comment
    assert history['objects'][0]['action'] == 'approve'


@pytest.mark.parametrize('approve_data', [{'approve': False}, {'decision': 'decline'}])
def test_post_detail__decline(client, simple_system, more_users_for_test, approve_requests_url, approve_data):
    """
    POST /frontend/approverequests/<request_id> - decline
    """
    (art, fantom, terran, zerg, protoss, admin) = more_users_for_test

    set_workflow(simple_system, 'approvers = [approver("terran") | approver("protoss"), approver("zerg")]')

    # role granted to art
    Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)

    client.login('terran')
    data = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'terran'}).json()
    superuser_req_data = data['objects'][0]

    assert data['meta']['total_count'] == 1
    assert superuser_req_data['is_done'] is False

    request_id = superuser_req_data['id']

    # BadRequest
    response = client.json.post(get_approve_request_url(request_id), {'someerrorkey': True})
    assert response.status_code == 400

    response = client.json.post(get_approve_request_url(request_id), approve_data)
    assert response.status_code == 204

    data = client.json.get(approve_requests_url, {'status': 'processed', 'approver': 'terran'}).json()
    superuser_req_data = data['objects'][0]
    assert superuser_req_data['is_done'] is True
    assert superuser_req_data['approved'] is False
    assert superuser_req_data['decision'] == 'decline'


def test_post_detail__discuss(client, simple_system, more_users_for_test, approve_requests_url):
    """
    POST /frontend/approverequests/<request_id> - discuss
    """
    (art, fantom, terran, zerg, protoss, admin) = more_users_for_test

    set_workflow(simple_system, 'approvers = [approver("terran") | approver("protoss"), approver("zerg")]')

    # role granted to art
    Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)

    client.login('terran')
    data = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'terran'}).json()
    superuser_req_data = data['objects'][0]
    request_id = superuser_req_data['id']

    with mock_ids_repo('startrek2') as issues_repo:
        issues_repo.create.return_value = MockedTrackerIssue('abcd')
        approve_data = {'decision': 'discuss'}
        response = client.json.post(get_approve_request_url(request_id), approve_data)
        assert response.status_code == 201
        data = response.json()
        assert data.get('result') == settings.IDM_ST_BASE_URL + 'abcd'


def test_post_detail__discuss_linked_role(client, simple_system, more_users_for_test, approve_requests_url):
    """
    POST /frontend/approverequests/<request_id> - discuss linked role
    """
    (art, fantom, terran, zerg, protoss, admin) = more_users_for_test

    set_workflow(simple_system, 'approvers = [approver("terran") | approver("protoss"), approver("zerg")]')

    parent_role = Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)
    Role.objects.request_role(None, art, simple_system, None, {'role': 'superuser'}, None, parent=parent_role)

    client.login('terran')
    data = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'terran'}).json()
    superuser_req_data = data['objects'][0]
    request_id = superuser_req_data['id']

    with mock_ids_repo('startrek2') as issues_repo:
        issues_repo.create.return_value = MockedTrackerIssue('abcd')
        approve_data = {'decision': 'discuss'}
        response = client.json.post(get_approve_request_url(request_id), approve_data)
        assert response.status_code == 400
        data = response.json()
        assert data.get('message') == 'Связанные роли обсуждать нельзя'


def test_post_detail__discuss_with_tracker_error(client, simple_system, more_users_for_test, approve_requests_url):
    """
    POST /frontend/approverequests/<request_id> - discuss
    """
    (art, fantom, terran, zerg, protoss, admin) = more_users_for_test

    set_workflow(simple_system, 'approvers = [approver("terran") | approver("protoss"), approver("zerg")]')

    # role granted to art
    Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)

    client.login('terran')
    data = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'terran'}).json()
    superuser_req_data = data['objects'][0]
    request_id = superuser_req_data['id']

    with mock_ids_repo('startrek2') as issues_repo:
        issues_repo.create.side_effect = BackendError(response=Response(500, 'abcd', {}, ''))
        approve_data = {'decision': 'discuss'}
        response = client.json.post(get_approve_request_url(request_id), approve_data)
        issues_repo.create.side_effect = None
        assert response.status_code == 500
        assert b'abcd' in response.content


def test_post_detail__discuss_with_granted_role(client, simple_system, more_users_for_test, approve_requests_url):
    """
    POST /frontend/approverequests/<request_id> - discuss
    """
    (art, fantom, terran, zerg, protoss, admin) = more_users_for_test

    set_workflow(simple_system, 'approvers = [approver("terran") | approver("protoss"), approver("zerg")]')

    # role granted to art
    role = Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)
    role.state = ROLE_STATE.GRANTED
    role.save(update_fields=['state'])

    client.login('terran')
    data = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'terran'}).json()
    superuser_req_data = data['objects'][0]
    request_id = superuser_req_data['id']

    with mock_ids_repo('startrek2') as issues_repo:
        issues_repo.create.return_value = MockedTrackerIssue('abcd')
        approve_data = {'decision': 'discuss'}
        response = client.json.post(get_approve_request_url(request_id), approve_data)
        assert response.status_code == 400


def test_post_detail__decline_request_with_comment(client, simple_system, arda_users, approve_requests_url, actions_url):
    """
    POST /frontend/approverequests/<request_id> - decline with comment
    """
    comment = 'Thou shalt not pass!'
    set_workflow(simple_system, 'approvers = [approver("legolas") | approver("saruman"), approver("varda")]')

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'superuser'}, None)
    client.login('saruman')
    data = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'saruman'}).json()
    assert len(data['objects']) == 1
    superuser_req_data = data['objects'][0]
    response = client.json.post(get_approve_request_url(superuser_req_data['id']), {
        'approve': False,
        'comment': comment
    })

    assert response.status_code == 204
    role = refresh(role)
    assert role.state == 'declined'
    action = role.actions.get(action='decline')
    assert action.comment == comment
    history = client.json.get(actions_url, {'role': role.pk}).json()
    assert history['objects'][0]['comment'] == comment
    assert history['objects'][0]['action'] == 'decline'


def test_get_list__request_ref_role(client, simple_system, arda_users, approve_requests_url, idm_robot):
    """Проверяем, что запрос от IDM выглядит как запрос от робота"""

    workflow = dedent("""
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%s',
                'role_data': {
                    'role': 'manager'
                }
            }]
        elif role.get('role') == 'manager':
            approvers = ['sauron']
        """ % simple_system.slug)
    set_workflow(simple_system, workflow, workflow)

    frodo = arda_users.get('frodo')
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert frodo.roles.count() == 2
    assert frodo.roles.filter(state='granted').count() == 1

    client.login('sauron')
    response = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'sauron'})
    assert response.json()['objects'][0]['requester']['username'] == 'robot-idm'


def test_get_list__inconsistency_reason(client, simple_system, arda_users, approve_requests_url):
    """Проверим причины запроса роли. При запросе по заведению неконсистентности должно быть inconsistency"""

    set_workflow(simple_system, 'approvers = ["legolas"]')
    roles = [
        {
            'login': 'frodo',
            'roles': [{'role': 'manager'}]
        }
    ]
    with mock_all_roles(simple_system, roles):
        Inconsistency.objects.check_roles()
        Inconsistency.objects.resolve()

    client.login('legolas')
    response = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'legolas'})
    assert response.status_code == 200
    assert len(response.json()['objects']) == 1
    item = response.json()['objects'][0]
    assert item['reason'] == 'inconsistency'
    assert item['human_reason'] == 'Разрешение расхождения'


def test_get_list__reasons(client, simple_system, arda_users, approve_requests_url):
    """Причины запроса роли, отличные от inconsistency"""

    set_workflow(simple_system, 'approvers = ["legolas"]')
    frodo = arda_users.frodo

    client.login('legolas')
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    response = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'legolas'})
    assert response.status_code == 200
    assert len(response.json()['objects']) == 1
    item = response.json()['objects'][0]
    assert item['reason'] == 'request'
    assert item['human_reason'] == 'Запрос роли'
    approve_request = ApproveRequest.objects.select_related_for_set_decided().get()
    approve_request.set_approved(arda_users.legolas)
    role.refresh_from_db()
    assert role.state == 'granted'

    role.set_state('need_request')
    role.rerequest(arda_users.frodo)
    response = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'legolas'})
    assert response.status_code == 200
    assert len(response.json()['objects']) == 1
    item = response.json()['objects'][0]
    assert item['reason'] == 'rerequest'
    assert item['human_reason'] == 'Перезапрос роли'

    approve_request = ApproveRequest.objects.select_related_for_set_decided().get(approve__role_request__is_done=False)
    approve_request.set_approved(arda_users.legolas)
    role.refresh_from_db()
    role.review()
    response = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'legolas'})
    assert response.status_code == 200
    assert len(response.json()['objects']) == 1
    item = response.json()['objects'][0]
    assert item['reason'] == 'review_rerequest'
    assert item['human_reason'] == 'Регулярный пересмотр'


@pytest.mark.parametrize('comment', ['Хочу быть superuser', None])
def test_get_list__pass_comment(client, simple_system, arda_users, approve_requests_url, comment: str):
    set_workflow(simple_system, 'approvers = [approver("legolas")]')
    client.login('legolas')
    frodo = arda_users.frodo

    Role.objects.request_role(frodo, frodo, simple_system, comment, {'role': 'superuser'})
    data = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'legolas'}).json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['comment'] == (comment or '')


def test_get_list__approver_groups(client, simple_system, arda_users, approve_requests_url):
    set_workflow(simple_system, 'approvers = [approver("legolas") | approver("saruman"), approver("varda")]')
    client.login('legolas')
    frodo = arda_users.frodo
    Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'superuser'}, None)

    # 3 на статистику + 2 на получение списка (список id и получение всего треша по id)
    # + 1 на префетч других approvequest-ов
    with assert_num_model_queries(ApproveRequest, 3):
        data = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'legolas'}).json()
    assert data['meta']['total_count'] == 1
    assert_approvers(data['objects'][0]['role_request'], [['legolas', 'saruman'], ['varda']])


def test_post_detail__decline_on_broken_system(arda_users, simple_system, client, approve_requests_url):
    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = ["gandalf"]')

    Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'superuser'}, None)

    client.login('gandalf')
    data = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'gandalf'}).json()
    request_id = data['objects'][0]['id']

    simple_system.is_broken = True
    simple_system.save()

    response = client.json.post(get_approve_request_url(request_id), {'approve': False})
    assert response.status_code == 409
    data = response.json()
    assert data['message'] == 'Нельзя подтвердить/отклонить роль в сломанной системе'

    simple_system.is_broken = False
    simple_system.save()

    data = client.json.get(approve_requests_url, {'status': 'pending', 'approver': 'gandalf'}).json()
    assert data['objects'][0]['is_done'] is False


def test_get_list__filter_status(arda_users, simple_system, client, approve_requests_url):
    """
        Проверяет фильтрацию запросов в очереди по статусам pending, processed, all
    """
    set_workflow(simple_system, "approvers=['gandalf']")

    # неподтвержденные запросы
    Role.objects.request_role(arda_users.frodo, arda_users.frodo, simple_system, '', {'role': 'admin'})
    Role.objects.request_role(arda_users.meriadoc, arda_users.meriadoc, simple_system, '', {'role': 'manager'})
    # подтвержденные запросы
    poweruser_role = Role.objects.request_role(arda_users.bilbo, arda_users.bilbo, simple_system, '',
                                               {'role': 'poweruser'})
    superuser_role = Role.objects.request_role(arda_users.frodo, arda_users.frodo, simple_system, '',
                                               {'role': 'superuser'})

    poweruser_approve_request = ApproveRequest.objects.select_related_for_set_decided().get(
        approve__role_request__role=poweruser_role)
    poweruser_approve_request.set_approved(arda_users.gandalf)

    superuser_approve_request = ApproveRequest.objects.select_related_for_set_decided().get(
        approve__role_request__role=superuser_role)
    superuser_approve_request.set_approved(arda_users.gandalf)

    client.login('sauron')

    add_perms_by_role('superuser', arda_users.sauron, simple_system)

    # status: all
    response = client.json.get(approve_requests_url, {'status': 'all', 'priority_type': 'all'})
    assert response.status_code == 200
    data = response.json()

    assert len(data['objects']) == 4
    assert {ar['requester']['username'] for ar in data['objects']} == {'bilbo', 'meriadoc', 'frodo'}

    # status: pending
    response = client.json.get(approve_requests_url, {'status': 'pending', 'priority_type': 'all'})
    assert response.status_code == 200
    data = response.json()

    assert len(data['objects']) == 2
    assert {ar['requester']['username'] for ar in data['objects']} == {'meriadoc', 'frodo'}

    # status: processed
    response = client.json.get(approve_requests_url, {'status': 'processed', 'priority_type': 'all'})
    assert response.status_code == 200
    data = response.json()

    assert len(data['objects']) == 2
    assert {ar['requester']['username'] for ar in data['objects']} == {'frodo', 'bilbo'}


def test_get_list__filter_status_with_multiplte_groups(arda_users, simple_system, client, approve_requests_url):
    """
        Проверяет фильтрацию запросов в очереди по статусам pending, processed, all
    """
    client.login('sauron')
    add_perms_by_role('superuser', arda_users.sauron, simple_system)
    workflow = dedent("""
        approvers = [
            any_from(['sam', 'bilbo'], priority=1) | any_from(['gimli', 'varda'], priority=2),
            any_from(['sauron', 'saruman'], priority=1) | approver('bilbo', priority=2),
            any_from(['galadriel', 'aragorn'])
        ]
    """)

    set_workflow(simple_system, workflow)

    admin_role = Role.objects.request_role(arda_users.frodo, arda_users.frodo, simple_system, '', {'role': 'admin'})
    bilbo_approve_request = ApproveRequest.objects.select_related_for_set_decided().filter(
        approve__role_request__role=admin_role, approver__username='bilbo').first()
    bilbo_approve_request.set_approved(arda_users.bilbo)

    assert Approve.objects.count() == 3
    assert Approve.objects.filter(approved=True).count() == 2

    # status: all
    response = client.json.get(approve_requests_url, {'status': 'all', 'priority_type': 'all'})
    assert response.status_code == 200
    data = response.json()

    expected_approvers = {'sam', 'bilbo', 'gimli', 'varda', 'sauron', 'saruman', 'galadriel', 'aragorn'}

    assert data['meta']['total_count'] == len(expected_approvers)
    assert {ar['approver']['username'] for ar in data['objects']} == expected_approvers

    # status: processed
    response = client.json.get(approve_requests_url, {'status': 'processed', 'priority_type': 'all'})
    assert response.status_code == 200
    data = response.json()

    expected_approvers = {'sam', 'bilbo', 'gimli', 'varda', 'sauron', 'saruman'}

    assert data['meta']['total_count'] == len(expected_approvers)
    assert {ar['approver']['username'] for ar in data['objects']} == expected_approvers

    # status: pending
    response = client.json.get(approve_requests_url, {'status': 'pending', 'priority_type': 'all'})
    assert response.status_code == 200
    data = response.json()

    expected_approvers = {'galadriel', 'aragorn'}

    assert data['meta']['total_count'] == len(expected_approvers)
    assert {ar['approver']['username'] for ar in data['objects']} == expected_approvers


def test_get_list__filter_status_with_autoapproves(arda_users, simple_system, client, approve_requests_url):
    client.login('sauron')
    add_perms_by_role('superuser', arda_users.sauron, simple_system)
    workflow = dedent("""
            approvers = [
                any_from(['sam', 'bilbo'], priority=1) | any_from(['gimli', 'varda'], priority=2),
                any_from(['sauron', 'saruman'], priority=1) | approver('varda', priority=2),
            ]
        """)

    set_workflow(simple_system, workflow)

    admin_role = Role.objects.request_role(arda_users.varda, arda_users.varda, simple_system, '', {'role': 'admin'})
    bilbo_approve_request = ApproveRequest.objects.select_related_for_set_decided().filter(
        approve__role_request__role=admin_role, approver__username='varda').first()
    bilbo_approve_request.set_approved(arda_users.varda)

    assert Approve.objects.count() == 2
    assert Approve.objects.filter(approved=True).count() == 2

    # status: processed
    response = client.json.get(approve_requests_url, {'status': 'processed', 'priority_type': 'all'})
    assert response.status_code == 200
    data = response.json()

    expected_approvers = {'sam', 'bilbo', 'gimli', 'varda', 'sauron', 'saruman'}

    assert data['meta']['total_count'] == len(expected_approvers)
    assert {ar['approver']['username'] for ar in data['objects']} == expected_approvers

    # status: pending
    response = client.json.get(approve_requests_url, {'status': 'pending', 'priority_type': 'all'})
    assert response.status_code == 200
    data = response.json()

    assert data['meta']['total_count'] == 0


def test_get_list__filter_priority_type(arda_users, simple_system, pt1_system, client, approve_requests_url):
    """
           Проверяет фильтрацию запросов в очереди по типу приоритета: основной, второстепенный, любой
    """
    client.login('sauron')
    add_perms_by_role('superuser', arda_users.sauron, simple_system)

    simple_system_workflow = dedent("""
        approvers = [
            approver('sam', priority=1) | approver('manve', priority=2),
            approver('gandalf', priority=1) | approver('meriadoc', priority=1),
            approver('gimli', priority=1) | approver('sauron', priority=2, notify=True) | approver('varda', priority=1, notify=False)
        ]
    """)

    pt1_system_workflow = dedent("""
        approvers = [
            approver('manve', priority=1) | approver('sam', priority=2) | approver('bilbo', priority=3),
            approver('sauron', priority=1) | approver('gimli', priority=2, notify=True) | approver('varda', priority=1, notify=False)
        ]
    """)

    set_workflow(simple_system, simple_system_workflow)
    set_workflow(pt1_system, pt1_system_workflow)

    simple_system_admin = Role.objects.request_role(arda_users.peregrin, arda_users.peregrin,
                                                    simple_system, '', {'role': 'admin'})
    simple_system_manager = Role.objects.request_role(arda_users.aragorn, arda_users.aragorn,
                                                      simple_system, '', {'role': 'manager'})

    pt1_admin = Role.objects.request_role(arda_users.gandalf, arda_users.gandalf, pt1_system, '',
                                          {'project': 'proj1', 'role': 'admin'}, {'passport-login': 'gandalf'})
    pt1_manager = Role.objects.request_role(arda_users.meriadoc, arda_users.meriadoc, pt1_system, '',
                                            {'project': 'proj1', 'role': 'manager'}, {'passport-login': 'meriadoc'})

    all_requests_role_ids = {simple_system_admin.id, simple_system_manager.id, pt1_admin.id, pt1_manager.id}
    simple_system_role_ids = {simple_system_admin.id, simple_system_manager.id}
    pt1_system_role_ids = {pt1_admin.id, pt1_manager.id}

    # основной приоритет
    for username in ('sam', 'gandalf', 'gimli', 'meriadoc', 'varda'):
        response = client.json.get(approve_requests_url, {
            'status': 'pending',
            'priority_type': 'primary',
            'system': simple_system.slug,
            'approver': username
        })
        assert response.status_code == 200
        data = response.json()
        assert len(data['objects']) == 2
        assert {ar['role']['id'] for ar in data['objects']} == simple_system_role_ids

    for username in ('manve', 'sauron', 'varda'):
        response = client.json.get(approve_requests_url, {
            'status': 'pending',
            'priority_type': 'primary',
            'approver': username,
            'system': pt1_system.slug
        })
        assert response.status_code == 200
        data = response.json()
        assert len(data['objects']) == 2
        assert {ar['role']['id'] for ar in data['objects']} == pt1_system_role_ids

    # второстепенный приоритет
    for username in ('manve', 'sauron'):
        response = client.json.get(approve_requests_url, {
            'status': 'pending',
            'priority_type': 'secondary',
            'system': simple_system.slug,
            'approver': username
        })
        assert response.status_code == 200
        data = response.json()
        assert len(data['objects']) == 2
        assert {ar['role']['id'] for ar in data['objects']} == simple_system_role_ids

    for username in ('sam', 'bilbo', 'gimli'):
        response = client.json.get(approve_requests_url, {
            'status': 'pending',
            'priority_type': 'secondary',
            'approver': username,
            'system': pt1_system.slug
        })
        assert response.status_code == 200
        data = response.json()
        assert len(data['objects']) == 2
        assert {ar['role']['id'] for ar in data['objects']} == pt1_system_role_ids

    # любой приоритет
    expected_result = {
        'sam': all_requests_role_ids,
        'manve': all_requests_role_ids,
        'sauron': all_requests_role_ids,
        'gimli': all_requests_role_ids,
        'gandalf': simple_system_role_ids,
        'meriadoc': simple_system_role_ids,
        'bilbo': pt1_system_role_ids,
    }

    for username in expected_result.keys():
        response = client.json.get(approve_requests_url, {
            'status': 'pending',
            'priority_type': 'all',
            'approver': username
        })
        assert response.status_code == 200, response.content
        data = response.json()
        assert len(data['objects']) == len(expected_result[username])

        assert {ar['role']['id'] for ar in data['objects']} == expected_result[username]


def test_get_list__filter_role_ids(arda_users, simple_system, pt1_system, client, approve_requests_url):
    """
        Проверяет фильтрацию по role_id
    """
    client.login('sauron')
    add_perms_by_role('superuser', arda_users.sauron, simple_system)

    simple_system_workflow = dedent("""
            approvers = [
                approver('sam', priority=1) | approver('manve', priority=2),
                approver('gandalf', priority=1) | approver('meriadoc', priority=1, notify=False)
            ]
        """)

    pt1_system_workflow = dedent("""
            approvers = [
                approver('sam', priority=2) | approver('varda', priority=3),
                approver('sauron', priority=1) | approver('gimli', priority=2, notify=True)
            ]
        """)

    set_workflow(simple_system, simple_system_workflow)
    set_workflow(pt1_system, pt1_system_workflow)

    simple_system_admin = Role.objects.request_role(arda_users.peregrin, arda_users.peregrin,
                                                    simple_system, '', {'role': 'admin'})
    simple_system_manager = Role.objects.request_role(arda_users.aragorn, arda_users.aragorn,
                                                      simple_system, '', {'role': 'manager'})

    pt1_admin = Role.objects.request_role(arda_users.gandalf, arda_users.gandalf, pt1_system, '',
                                          {'project': 'proj1', 'role': 'admin'}, {'passport-login': 'gandalf'})
    pt1_manager = Role.objects.request_role(arda_users.meriadoc, arda_users.meriadoc, pt1_system, '',
                                            {'project': 'proj1', 'role': 'manager'}, {'passport-login': 'meriadoc'})

    # simple_system
    response = client.json.get(approve_requests_url, {
        'status': 'pending',
        'priority_type': 'all',
        'role_ids': ','.join(str(role_id) for role_id in [simple_system_manager.id, simple_system_admin.id])
    })

    assert response.status_code == 200, response.content
    data = response.json()

    assert data['meta']['total_count'] == 8
    assert {ar['approver']['username'] for ar in data['objects']} == {'sam', 'manve', 'meriadoc', 'gandalf'}

    # pt1_system
    response = client.json.get(approve_requests_url, {
        'status': 'pending',
        'priority_type': 'all',
        'role_ids': ','.join(str(role_id) for role_id in [pt1_admin.id, pt1_manager.id])
    })

    assert response.status_code == 200, response.content
    data = response.json()

    assert data['meta']['total_count'] == 8
    assert {ar['approver']['username'] for ar in data['objects']} == {'sam', 'varda', 'sauron', 'gimli'}

    # both systems
    response = client.json.get(approve_requests_url, {
        'status': 'pending',
        'priority_type': 'all',
        'role_ids': ','.join(str(role_id) for role_id in [simple_system_admin.id, pt1_manager.id])
    })

    assert response.status_code == 200, response.content
    data = response.json()

    assert data['meta']['total_count'] == 8
    assert {ar['approver']['username'] for ar in data['objects']} == \
           {'sam', 'varda', 'sauron', 'gimli', 'manve', 'meriadoc', 'gandalf'}


@pytest.mark.parametrize('reason', ['request', 'rerequest', 'review_rerequest', 'import'])
def test_get_list__filter_by_reason(client, approve_requests_url: str, reason: str):
    client.login(create_user(superuser=True))
    approvers = [create_user(), create_user()]
    system = create_system(
        workflow='approvers = [{}]'.format(', '.join(f'"{user.username}"' for user in approvers)),
        public=True,
    )
    node = system.nodes.last()

    role_requests_by_reason: Dict[str, RoleRequest] = {}
    requested_role = Role.objects.request_role(create_user(), create_user(), system, '', node.data)
    role_requests_by_reason['request'] = requested_role.requests.get()
    rerequested_role = raw_make_role(create_user(), system, node.data, state='deprived')
    rerequested_role.rerequest(create_user())
    role_requests_by_reason['rerequest'] = rerequested_role.requests.get()
    review_role = raw_make_role(create_user(), system, node.data)
    review_role.review()
    role_requests_by_reason['review_rerequest'] = review_role.requests.get()
    imported_role = Role.objects.create_role(create_user(), system, node, node.data, save=True)
    imported_role.set_state('imported')
    role_requests_by_reason['import'] = imported_role.requests.get()

    response = client.get(approve_requests_url, {
        'status': 'pending',
        'priority_type': 'all',
        'reason': reason
    })
    assert response.status_code == 200, response.json()
    result = response.json()
    assert len(result['objects']) == len(approvers)
    for approve_request_data, approve_request in zip(
            sorted(result['objects'], key=operator.itemgetter('id')),
            sorted(
                itertools.chain(*(
                    approve.requests.select_related(
                        'approver',
                        'approve',
                        'approve__role_request',
                        'approve__role_request__role',
                    ).all()
                    for approve in role_requests_by_reason[reason].approves.all())
                ),
                key=operator.attrgetter('id'),
            ),
        ):
        assert approve_request_data['id'] == approve_request.id
        assert approve_request_data['approver']['username'] == approve_request.approver.username
        assert approve_request_data['approver']['username'] in {user.username for user in approvers}
        assert approve_request_data['role_request']['id'] == approve_request.approve.role_request.id
        assert approve_request_data['role_request']['id'] == role_requests_by_reason[reason].id
        assert approve_request_data['role']['id'] == approve_request.approve.role_request.role.id
        assert approve_request_data['role']['id'] == role_requests_by_reason[reason].role.id


def test_get_list__filter_by_requesters(client, approve_requests_url: str):
    client.login(create_user(superuser=True))
    approvers = [create_user(), create_user()]
    system = create_system(
        workflow='approvers = [{}]'.format(', '.join(f'"{user.username}"' for user in approvers)),
        public=True,
    )
    node = system.nodes.last()

    role_by_requesters = {
        requester: Role.objects.request_role(requester, create_user(), system, '', node.data)
        for requester in (create_user(), create_user(), create_user())
    }
    for requester in role_by_requesters:
        response = client.get(approve_requests_url, {
            'status': 'pending',
            'priority_type': 'all',
            'requester': requester.username,
        })

        assert response.status_code == 200, response.json()
        result = response.json()
        assert len(result['objects']) == len(approvers)

        role: Role = role_by_requesters[requester]
        role_request: RoleRequest = role.requests.get()
        for approve_request in result['objects']:
            assert approve_request['requester']['username'] == requester.username
            assert approve_request['approver']['username'] in {user.username for user in approvers}
            assert approve_request['role_request']['id'] == role_request.id
            assert approve_request['role']['id'] == role.id

    response = client.get(approve_requests_url, {
        'status': 'pending',
        'priority_type': 'all',
        'requester': ','.join(requester.username for requester in role_by_requesters),
    })
    assert response.status_code == 200, response.json()
    result = response.json()
    assert len(result['objects']) == len(approvers) * len(role_by_requesters)
    requester_slots = {
        requester.username: {approver.username for approver in approvers}
        for requester in role_by_requesters
    }

    for approve_request in result['objects']:
        requester_slots[approve_request['requester']['username']].remove(approve_request['approver']['username'])
    assert not any(requester_slots.values())


@pytest.mark.parametrize('priority_type', ('primary', 'secondary', 'all'))
@pytest.mark.parametrize('filter_option', ('role_ids', 'system', 'user', 'path', None))
@pytest.mark.parametrize(('decision', 'decided'), (
        (APPROVEREQUEST_DECISION.APPROVE, True),
        (APPROVEREQUEST_DECISION.DECLINE, False),
        (APPROVEREQUEST_DECISION.IGNORE, None),
))
def test_post_bulk(arda_users, simple_system, other_system, client, approve_requests_bulk_url, filter_option,
                              decision: str, decided: Optional[bool], priority_type: str):
    """
        Проверяет фильтрацию по role_id
    """
    client.login('sauron')
    add_perms_by_role('superuser', arda_users.sauron, simple_system)
    add_perms_by_role('superuser', arda_users.sauron, other_system)

    set_workflow(simple_system, dedent("""approvers = [approver('sauron', priority=1)]"""))
    set_workflow(other_system, dedent("""approvers = [approver('sauron', priority=1)]"""))

    approved_role = Role.objects.request_role(
        arda_users.sauron, arda_users.sauron, simple_system, '', {'role': 'manager'}
    )
    assert approved_role.requests.get().approves.get().approved is True

    admin_role = Role.objects.request_role(
        arda_users.gandalf, arda_users.gandalf, simple_system, '', {'role': 'admin'}
    )
    manager_role = Role.objects.request_role(
        arda_users.gandalf, arda_users.aragorn, simple_system, '', {'role': 'manager'}
    )
    developer_role = Role.objects.request_role(
        arda_users.gandalf, arda_users.sam, other_system, '', {'role': 'admin'}
    )
    roles = {admin_role, manager_role, developer_role}
    for role in roles:
        assert role.requests.count() == 1
        role_request: RoleRequest = role.requests.get()
        assert not role_request.is_done
        assert role_request.approves.count() == 1
        approve: Approve = role_request.approves.get()
        assert approve.approved is None
        assert approve.requests.count() == 1
        assert approve.requests.get().decision == APPROVEREQUEST_DECISION.NOT_DECIDED
        assert role.state == ROLE_STATE.REQUESTED

    expect_approved = roles
    params = {}
    if filter_option == 'role_ids':
        expect_approved = {manager_role, developer_role}
        params[filter_option] = ','.join(str(role.id) for role in expect_approved)
    elif filter_option == 'system':
        params[filter_option] = simple_system.slug
        expect_approved = {admin_role, manager_role}
    elif filter_option == 'user':
        params[filter_option] = 'sam'
        expect_approved = {developer_role}
    elif filter_option == 'path':
        params['system'] = other_system.slug
        params[filter_option] = '/'
        expect_approved = {developer_role}

    if priority_type == 'secondary':
        expect_approved = set()

    response = client.json.post(
        approve_requests_bulk_url,
        {
            'priority_type': priority_type,
            'comment': ''.join(random.choices(string.ascii_lowercase, k=20)),
            'decision': decision,
            **params
        }
    )

    assert response.status_code == 204, response.json()

    assert approved_role.requests.get().approves.get().approved is True

    for role in expect_approved:
        role.refresh_from_db()
        role_request: RoleRequest = role.requests.get()
        assert role_request.is_done
        approve: Approve = role_request.approves.get()
        assert approve.approved is (decided or False)
        assert approve.requests.get().decision == decision

    for role in (roles - expect_approved):
        role.refresh_from_db()
        role_request: RoleRequest = role.requests.get()
        assert not role_request.is_done
        approve: Approve = role_request.approves.get()
        assert approve.approved is None
        assert approve.requests.get().decision == APPROVEREQUEST_DECISION.NOT_DECIDED


def test_post_list__ignore(arda_users, simple_system, client, approve_requests_url):
    set_workflow(simple_system, 'approvers = [approver("legolas") | approver("saruman"),'
                                'approver("varda") | approver("legolas")]')

    frodo = arda_users.frodo
    Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'superuser'}, None)

    client.login('saruman')
    request = ApproveRequest.objects.get(approver=arda_users.saruman)
    response = client.json.post(get_approve_request_url(request.id), {'decision': 'ignore'})
    assert response.status_code == 204

    response = client.json.get(approve_requests_url, {
        'status': 'processed',
        'priority_type': 'all',
    }).json()
    assert len(response['objects']) == 1
    data = response['objects'][0]
    assert data['id'] == request.id

    response = client.json.get(approve_requests_url, {
        'status': 'pending',
        'priority_type': 'all'
    }).json()
    approve_reqeuests_ids = [x['id'] for x in response['objects']]
    assert request.id not in approve_reqeuests_ids

    request.refresh_from_db()
    assert request.decision == 'ignore'


def test_get_list__move_parent_request(arda_users, simple_system, client, approve_requests_url):
    set_workflow(simple_system, "approvers = [any_from(['frodo', 'bilbo']), any_from(['bilbo', 'sam', 'meriadoc'])]")
    client.login(arda_users.bilbo)

    Role.objects.request_role(arda_users.sauron, arda_users.sauron, simple_system, None, {'role': 'superuser'}, None)

    request1, request2 = ApproveRequest.objects.filter(approver=arda_users.bilbo).order_by('pk')
    assert request2.parent_id == request1.id

    response = client.json.get(approve_requests_url, {
        'status': 'pending',
        'priority_type': 'all',
        'approver': 'bilbo',
    })
    assert response.status_code == 200
    response = response.json()
    assert response['meta']['total_count'] == 1
    assert response['objects'][0]['id'] == request1.id

    frodo_request = ApproveRequest.objects.select_related_for_set_decided().get(approver=arda_users.frodo)
    frodo_request.set_approved(arda_users.frodo)

    request2.refresh_from_db()
    assert request2.parent_id is None

    response = client.json.get(approve_requests_url, {
        'status': 'pending',
        'priority_type': 'all',
        'approver': 'bilbo',
    })
    assert response.status_code == 200
    response = response.json()
    assert response['meta']['total_count'] == 1
    assert response['objects'][0]['id'] == request2.id


def test_get__list__only_meta(arda_users, simple_system, client, approve_requests_url):
    client.login(arda_users.frodo)
    response = client.json.get(approve_requests_url, {'only_meta': True, 'status': 'pending', 'type': 'all'})
    assert response.status_code == 200
    assert response.json() == {
        'meta': {
            'total_count': 0,
            'count_is_estimated': False,
            'limit': 20,
            'next': None,
            'previous': None,
            'offset': 0,
        }
    }


def test_get_list__service_group_level3_500(
        client,
        arda_users,
        simple_system,
        simple_service_groups,
        approve_requests_url,
):
    """
    Сервисная группа уровня 3 в статусе 'deprived', на нее есть 'deprived' роль.

    IDM-10314: запрос должен возвращать 200
    https://idm-api.test.yandex-team.ru
    /api/frontend/approverequests/
    ?status=all&priority_type=all&system=testsystem&approver=pixel&no_meta=true
    """
    simple_system.request_policy = 'anyone'
    simple_system.save(update_fields=['request_policy'])
    group = Group.objects.create(
        type='service',
        slug='svc_group_scope_child',
        parent=simple_service_groups['svc_group_scope'],
        name='svc_group_scope_child',
        external_id=999,
        description='bad group level3')

    assert group.parent.level == 2
    assert group.level == 3

    sam = arda_users.sam
    frodo = arda_users.frodo
    set_workflow(simple_system, group_code="approvers = ['frodo']")
    role = Role.objects.request_role(sam, group, simple_system, '', {'role': 'admin'})
    role.refresh_from_db()
    assert role.state == 'requested'
    ApproveRequest.objects.select_related(
        'approve__role_request__role__system',
        'approve__role_request__role__node',
        'approve__role_request__requester',
        'approver'
    ).get().set_approved(frodo, 'ok!')
    role.refresh_from_db()
    assert role.state == 'granted'
    role.deprive_or_decline(sam)
    role.refresh_from_db()
    assert role.state == 'deprived'
    assert group.state == 'active'
    group.state = 'deprived'
    group.save(update_fields=['state'])
    assert group.state == 'deprived'


    client.login('frodo')
    response = client.json.get(approve_requests_url, {
        'no_meta': True,
        'system': 'simple',
        'priority_type': 'all',
        'status': 'all',
        'approver': 'frodo',
    })
    assert response.status_code == 200


def test_get_list__metrika_counter_fields_data(client, metrika_system, approve_requests_url):
    subject = create_user()
    client.login(subject)
    set_workflow(metrika_system, f'approvers = ["{create_user().username}"]')

    counter = MetrikaCounter.objects.create(**generate_counter_record().as_dict)
    rolenode = metrika_system.nodes.last()
    role = Role.objects.request_role(
        subject,
        subject,
        metrika_system,
        '',
        rolenode.data,
        fields_data={'counter_id': counter.counter_id},
    )

    response = client.get(approve_requests_url, {'status': 'pending'})
    assert response.status_code == 200, response.json()
    result = response.json()
    assert len(result['objects']) == 1
    assert result['objects'][0]['role']['id'] == role.id
    assert result['objects'][0]['role']['fields_data'] == \
           {'counter_id': counter.counter_id, 'counter_name': counter.name}, \
        result['objects'][0]['role']


def test_get_list__app_metrica_fields_data(client, app_metrica_system, approve_requests_url):
    subject = create_user()
    client.login(subject)
    set_workflow(app_metrica_system, f'approvers = ["{create_user().username}"]')

    app = AppMetrica.objects.create(**generate_app_record().as_dict)
    rolenode = app_metrica_system.nodes.last()
    role = Role.objects.request_role(
        subject,
        subject,
        app_metrica_system,
        '',
        rolenode.data,
        fields_data={'application_id': app.application_id},
    )

    response = client.get(approve_requests_url, {'status': 'pending'})
    assert response.status_code == 200, response.json()
    result = response.json()
    assert len(result['objects']) == 1
    assert result['objects'][0]['role']['id'] == role.id
    assert result['objects'][0]['role']['fields_data'] == \
           {'application_id': app.application_id, 'application_name': app.name}, result['objects'][0]['role']


def test_as_approver(simple_system, arda_users, approve_requests_url):
    set_workflow(simple_system, "approvers = [any_from(['frodo'])]")

    Role.objects.request_role(arda_users.sauron, arda_users.sauron, simple_system, None, {'role': 'superuser'}, None)

    frodo_request = ApproveRequest.objects.select_related(
        'approver'
    ).get(approver=arda_users.frodo)
    approver = frodo_request.as_approver()

    assert approver.username == arda_users.frodo.username
    assert approver._user == arda_users.frodo
