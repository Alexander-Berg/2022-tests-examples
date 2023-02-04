# coding: utf-8
import copy
import random
from datetime import timedelta, datetime
from textwrap import dedent

import mock
import pytest
from django.conf import settings
from django.core import mail
from django.test.utils import override_settings
from django.utils import timezone
from django_yauth.authentication_mechanisms.tvm import TvmRequest

from idm.core.constants.role import ROLE_STATE
from idm.core.constants.rolefield import FIELD_TYPE
from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.core.models import ApproveRequest, Role, UserPassportLogin, NetworkMacro, RoleNode
from idm.tests.utils import (set_workflow, set_roles_tree, compare_time, disable_tasks,
                             add_perms_by_role, get_mocked_tvm_client, force_awaiting_memberships_role_grant,
                             create_user, create_system, random_slug)
from idm.users.constants.user import USER_TYPES
from idm.users.models import Group
from idm.utils import reverse

pytestmark = pytest.mark.django_db


@pytest.fixture
def requests_url():
    return reverse('api_dispatch_list', api_name='frontend', resource_name='rolerequests')


def get_request_url(request_id):
    return reverse('api_dispatch_detail', api_name='frontend', resource_name='rolerequests', pk=request_id)


def test_get_role_requests(client, simple_system, users_for_test, requests_url):
    """
    POST /frontend/rolerequests/
    """
    set_workflow(simple_system, 'approvers = ["terran"]')

    (art, fantom, terran, admin) = users_for_test
    role = Role.objects.request_role(art, art, simple_system, '', {'role': 'manager'}, fields_data={})
    assert role.state == 'requested'

    client.login('art')
    response = client.json.get(requests_url)
    assert response.status_code == 200
    data = response.json()['objects']
    assert len(data) == 1
    assert data[0]['is_done'] is False
    assert data[0]['role']['id'] == role.id


def test_post_role_requests_401(client, requests_url, simple_system):
    response = client.json.post(requests_url, {
        'system': 'simple',
        'role': '/manager/'
    })
    assert response.status_code == 401


def test_post_role_requests_400_no_subject(arda_users, client, requests_url, simple_system):
    client.login('frodo')
    response = client.json.post(requests_url, {
        'path': '/manager/',
        'system': 'simple',
        'simulate': True
    })
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Вы должны указать либо сотрудника, либо группу',
        'errors': {
            '__all__': ['Вы должны указать либо сотрудника, либо группу'],
        }
    }
    assert Role.objects.count() == 0


def test_post_role_requests_400_wrong_node(arda_users, client, requests_url, simple_system):
    client.login('frodo')
    response = client.json.post(requests_url, {
        'path': '/common/manager/',
        'system': 'simple',
        'simulate': True
    })
    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Invalid data sent',
        'errors': {
            'path': ['Узел в дереве ролей не найден']
        }
    }
    assert Role.objects.count() == 0


def test_post_role_requests_400_comment_required(arda_users, client, requests_url, simple_system):
    client.login('frodo')
    node_path = '/manager/'
    role_node: RoleNode = RoleNode.objects.get_node_by_value_path(simple_system, node_path)
    role_node.comment_required = True
    role_node.save(update_fields=('comment_required',))

    response = client.json.post(requests_url, {
        'path': node_path,
        'system': simple_system.slug,
        'user': 'frodo',
        'simulate': True
    })
    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Эта роль требует обязательного комментария',
    }
    assert Role.objects.count() == 0


def test_post_role_requests_no_meta(arda_users, client, requests_url, simple_system):
    client.login('frodo')
    response = client.json.post(requests_url, {
        'path': '/manager/',
        'user': 'frodo',
        'system': 'simple',
        'no_meta': True,
    })
    assert response.status_code == 201
    role = Role.objects.get()
    assert response.json() == {'id': role.id}


@pytest.mark.parametrize('param_type', (str, int))
def test_post_role_requests_group(arda_users, client, department_structure, param_type, requests_url, simple_system):
    """
    TestPalmID: 3456788-22
    """
    client.login('gandalf')
    set_workflow(simple_system, group_code='approvers = ["frodo"]')
    response = client.json.post(requests_url, {
        'path': '/manager/',
        'group': param_type(department_structure.fellowship.external_id),
        'system': 'simple',
    })
    assert response.status_code == 201

    role = Role.objects.get()
    assert role.state == ROLE_STATE.REQUESTED
    assert role.group_id == department_structure.fellowship.id


def test_post_role_requests(client, simple_system, users_for_test, requests_url):
    """
    POST /frontend/rolerequests/
    """
    set_workflow(simple_system, 'approvers = ["art"]')

    (art, fantom, terran, admin) = users_for_test

    # authorized actual request
    client.login('fantom')
    response = client.json.post(requests_url, {
        'path': '/manager/',
        'system': 'simple',
        'user': 'fantom',
    })

    assert response.status_code == 201
    assert Role.objects.count() == 1
    role = Role.objects.get()

    role.fetch_node()
    assert role.node.data == {'role': 'manager'}
    assert role.user_id == fantom.id
    assert role.system_id == simple_system.id
    assert role.with_inheritance
    assert role.with_robots
    assert role.with_external

    # request role for other user
    client.login('admin')
    response = client.json.post(requests_url, {
        'path': '/superuser/',
        'user': 'terran',
        'system': 'simple'
    })

    assert response.status_code == 201
    assert response.json()['data'] == {'role': 'superuser'}
    assert response.json()['user']['username'] == 'terran'
    assert response.json()['system']['slug'] == 'simple'

    assert Role.objects.count() == 2
    role = Role.objects.exclude(pk=role.pk).get()

    role.fetch_node()
    assert role.node.data == {'role': 'superuser'}
    assert role.user_id == terran.id
    assert role.actions.get(action='request').requester_id == admin.id
    assert role.system_id == simple_system.id

    # проверяем что роль корректно запрашивается при особой политике
    # меняем политику на разрешение запрашивать для всех
    simple_system.request_policy = 'anyone'
    simple_system.save()

    client.login('fantom')
    response = client.json.post(requests_url, {
        'path': '/poweruser/',
        'user': 'terran',
        'system': 'simple'
    })

    assert response.status_code == 201
    assert response.json()['data'] == {'role': 'poweruser'}
    assert response.json()['user']['username'] == 'terran'
    assert response.json()['system']['slug'] == 'simple'

    assert Role.objects.count() == 3


def test_post_list__request_role_by_tvm_user(client, requests_url):
    """
    POST /frontend/rolerequests/
    """
    tvm_app = create_user(type=USER_TYPES.TVM_APP, department_group=None)
    user = create_user(uid=random.randint(1, 10 ** 8))
    system = create_system()
    middlewares = copy.copy(settings.MIDDLEWARE)
    middlewares[middlewares.index('idm.framework.middleware.IDMYandexAuthTestMiddleware')] = \
        'idm.framework.middleware.IDMYandexAuthMiddleware'

    with mock.patch(
            'idm.framework.authentication.get_tvm_client',
            return_value=get_mocked_tvm_client(uid=user.uid, src=tvm_app.username)
    ), override_settings(YAUTH_TEST_USER=False, MIDDLEWARE=middlewares):
        response = client.json.post(
            requests_url,
            {
                'path': system.nodes.last().value_path,
                'system': system.slug,
                'user': user.username,
            },
            HTTP_X_YA_SERVICE_TICKET=random_slug(),
            HTTP_X_YA_USER_TICKET=random_slug(),
        )

    assert response.status_code == 201, response.json()
    role = Role.objects.get()
    assert role.state == ROLE_STATE.GRANTED
    assert role.last_request.requester_id == user.id


def test_post_list__request_role_by_tvm_service(client, simple_system, arda_users, requests_url, monkeypatch):
    """
    POST /frontend/rolerequests/
    """
    tvm_app = create_user(type=USER_TYPES.TVM_APP, department_group=None)
    system = create_system(public=True)
    user = create_user()
    middlewares = copy.copy(settings.MIDDLEWARE)
    middlewares[middlewares.index('idm.framework.middleware.IDMYandexAuthTestMiddleware')] = \
        'idm.framework.middleware.IDMYandexAuthMiddleware'

    with mock.patch(
            'idm.framework.authentication.get_tvm_client',
            return_value=get_mocked_tvm_client(src=tvm_app.username),
    ), override_settings(YAUTH_TEST_USER=False, MIDDLEWARE=middlewares):
        response = client.json.post(
            requests_url,
            {
                'path': system.nodes.last().value_path,
                'system': system.slug,
                'user': user.username,
            },
            HTTP_X_YA_SERVICE_TICKET=random_slug(),
        )

    assert response.status_code == 201, response.json()
    role = Role.objects.get()
    assert role.user_id == user.id
    assert role.state == ROLE_STATE.GRANTED
    assert role.last_request.requester_id == tvm_app.id


def test_post_role_requests_with_flags(client, simple_system, users_for_test, requests_url):
    """
    POST /frontend/rolerequests/
    """
    set_workflow(
        simple_system,
        'approvers = ["art" '
        'if inheritance_settings["with_inheritance"] and '
        'not inheritance_settings["with_robots"] and '
        'inheritance_settings["with_external"] and '
        'inheritance_settings["without_hold"] '
        'else None]'
    )

    client.login('fantom')
    response = client.json.post(requests_url, {
        'path': '/manager/',
        'system': 'simple',
        'user': 'fantom',
        'with_robots': False,
        'without_hold': True,
        'request_fields': None,
    })

    assert response.status_code == 201
    role = Role.objects.get()

    assert role.with_inheritance
    assert not role.with_robots
    assert role.with_external


def test_request_role_big_deprive_after_days(client, simple_system, arda_users, requests_url):
    client.login('frodo')
    response = client.json.post(requests_url, {
        'path': '/poweruser/',
        'user': 'frodo',
        'system': 'simple',
        'deprive_after_days': 366,
    })
    assert response.status_code == 400
    assert response.json()['errors']['deprive_after_days'] == ['Убедитесь, что это значение меньше либо равно 365.']


def test_request_role_big_deprive_at(client, simple_system, arda_users, requests_url):
    client.login('frodo')
    response = client.json.post(requests_url, {
        'path': '/poweruser/',
        'user': 'frodo',
        'system': 'simple',
        'deprive_at': '2222-12-31',
    })
    assert response.status_code == 400
    assert response.json()['errors']['deprive_at'] == ['Нельзя запросить временную роль больше чем на год']


def test_request_role_answer_with_autoapprove(client, simple_system, arda_users, requests_url):
    """Проверим статус, которым мы отвечаем для случая автоподтверждения"""

    client.login('frodo')
    with disable_tasks():
        response = client.json.post(requests_url, {
            'path': '/poweruser/',
            'user': 'frodo',
            'system': 'simple'
        })
    assert response.status_code == 201
    data = response.json()
    assert data['state'] == 'approved'


def test_request_role_with_request_fields(client, simple_system, arda_users, requests_url):
    request_fields = {'approver': 'legolas'}
    set_workflow(simple_system, 'approvers = [request_fields["approver"]]')
    client.login('legolas')
    response = client.json.post(requests_url, {
        'path': '/manager/',
        'system': 'simple',
        'user': 'legolas',
        'request_fields': request_fields,
    })
    assert response.status_code == 201
    approve_request = ApproveRequest.objects.get()
    assert approve_request.approver_id == arda_users.legolas.id
    role = Role.objects.get()
    assert role.request_fields == request_fields


def test_request_approvers(client, simple_system, users_for_test, requests_url):
    """
    POST /frontend/rolerequests/
    """
    (art, fantom, terran, admin) = users_for_test

    set_workflow(simple_system, 'approvers = ["art"]')
    client.login('fantom')
    response = client.json.post(requests_url, {
        'path': '/manager/',
        'system': 'simple',
        'user': 'fantom',
        'simulate': True,
    })

    assert response.status_code == 200
    data = response.json()
    assert data['approvers'] == [[{'username': 'art', 'full_name': 'Центурион Марк'}]]

    set_workflow(simple_system, 'approvers = [approver("art") | "fantom"]')
    response = client.json.post(requests_url, {
        'path': '/manager/',
        'system': 'simple',
        'user': 'fantom',
        'simulate': True,
    })

    assert response.status_code == 200
    data = response.json()
    assert data['approvers'] == []


@pytest.mark.parametrize('approvers', ['[approver("art") | "terran"]', '["art", "terran"]'])
def test_request_with_too_many_approvers(client, simple_system, users_for_test, requests_url, approvers):
    """
    POST /frontend/rolerequests/
    """
    (art, fantom, terran, admin) = users_for_test
    simple_system.max_approvers = 1
    simple_system.save()

    set_workflow(simple_system, 'approvers = ["art"]')
    client.login('fantom')
    response = client.json.post(requests_url, {
        'path': '/manager/',
        'system': 'simple',
        'user': 'fantom',
        'simulate': True,
    })

    assert response.status_code == 200
    data = response.json()
    assert data['approvers'] == [[{'username': 'art', 'full_name': 'Центурион Марк'}]]

    set_workflow(simple_system, 'approvers = %s' % approvers)
    response = client.json.post(requests_url, {
        'path': '/manager/',
        'system': 'simple',
        'user': 'fantom',
        'simulate': True,
    })
    assert response.status_code == 409
    assert response.json()['message'] == (
        'Для роли определено слишком большое количество подтверждающих '
        '(сейчас: 2, разрешено: 1). Для решения проблемы напишите на tools@.'
    )


def test_request_hidden_role(client, pt1_system, users_for_test, requests_url):
    """
    POST /frontend/rolerequests/
    """
    set_workflow(pt1_system, 'approvers = ["art"]')

    client.login('fantom')
    response = client.json.post(requests_url, {
        'path': '/proj2/invisible_role/',
        'system': pt1_system.slug,
        'user': 'fantom',
        'simulate': True,
        'fields_data': {'passport-login': 'abc'},
    })
    assert response.status_code == 403
    assert response.json() == {
        'error_code': 'FORBIDDEN',
        'message': 'Вы не можете запрашивать скрытую роль',
    }
    assert len(mail.outbox) == 0
    assert Role.objects.count() == 0


@pytest.mark.parametrize('comment_required', (True, False))
def test_request_role_with_comment(client, simple_system, users_for_test, requests_url, comment_required):
    """
    POST /frontend/rolerequests/
    with comment
    """
    set_workflow(simple_system, 'approvers = ["art"]')

    client.login('terran')
    comment = 'Хочу быть менеджером'

    role_node: RoleNode = RoleNode.objects.get_node_by_value_path(simple_system, '/manager/')
    role_node.comment_required = comment_required
    role_node.save(update_fields=['comment_required'])

    response = client.json.post(requests_url, {
        'path': role_node.value_path,
        'system': simple_system.slug,
        'comment': comment,
        'user': 'terran',
    })

    assert response.status_code == 201
    data = response.json()
    expected = {
        # flat:
        # dates
        'added', 'review_at', 'expire_at', 'granted_at', 'updated',
        # params:
        'id', 'data', 'fields_data', 'is_active', 'is_public', 'state', 'system_specific',
        'ttl_date', 'ttl_days', 'review_date',
        # internal param
        'review_days',
        # human
        'human', 'human_short', 'human_state',
        # nested objects
        'group', 'node', 'parent', 'system', 'user', 'role_request',
        # additional data
        'permissions', 'ref_count', 'require_approve_from',
        'with_inheritance', 'with_external', 'with_robots', 'without_hold'
    }
    assert data.keys() == expected
    assert data['role_request']['requester']['username'] == 'terran'
    assert Role.objects.count() == 1
    role = Role.objects.get()
    assert role.actions.count() == 2
    action = role.actions.get(action='request')
    assert action.comment == comment
    assert len(mail.outbox) == 2
    assert mail.outbox[0].to == ['art@example.yandex.ru']
    assert comment in mail.outbox[0].body
    history = client.json.get(
        reverse('api_dispatch_list', api_name='frontend', resource_name='actions'),
        data={
            'role': role.pk,
        }
    ).json()
    assert history['objects'][1]['action'] == 'request'
    assert history['objects'][1]['comment'] == comment


def test_bad_post_role_requests(client, simple_system, users_for_test, requests_url):
    """
    POST /frontend/rolerequests/
    """
    set_workflow(simple_system, 'approvers = ["art"]')

    client.login('fantom')
    response = client.json.post(requests_url, {
        'path': '/manager/',
        'simulate': True
    })

    assert response.status_code == 400
    assert response.json() == {
        'message': 'Invalid data sent',
        'error_code': 'BAD_REQUEST',
        'errors': {
            'path': ['Обязательное поле.'],
            'system': ['Обязательное поле.']
        }
    }


def test_request_role_in_broken_system(client, simple_system, arda_users, requests_url):
    """Попробуем запросить роль в сломанной системе"""

    simple_system.is_broken = True
    simple_system.save()

    client.login('frodo')
    response = client.json.post(requests_url, {
        'user': 'frodo',
        'path': '/manager/',
        'system': 'simple',
    })

    assert response.status_code == 409
    assert response.json() == {
        'message': 'Система "Simple система" сломана. Роль не может быть запрошена.',
        'error_code': 'CONFLICT',
        'simulated': False,
    }


def test_post_group_role_requests(client, simple_system, arda_users, department_structure, requests_url):
    """
    POST /frontend/rolerequests/
    """
    set_workflow(simple_system, group_code='approvers = ["legolas"]')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')

    # authorized simulation request
    client.login('frodo')
    response = client.json.post(requests_url, {
        'group': fellowship.external_id,
        'path': '/manager/',
        'system': 'simple',
        'simulate': True
    })
    data = response.json()
    assert 'simulated' in data
    assert data['simulated'] is True
    assert len(data['approvers']) == 1
    assert len(data['approvers'][0]) == 1
    assert data['approvers'][0][0]['username'] == 'legolas'
    assert Role.objects.count() == 0

    # authorized request for a nonexistent group
    response = client.json.post(requests_url, {
        'group': 100000000,
        'path': '/manager/',
        'system': 'simple'
    })
    assert response.status_code == 400
    assert response.json() == {
        'message': 'Invalid data sent',
        'error_code': 'BAD_REQUEST',
        'errors': {
            'group': ['Группа с id=100000000 не найдена']
        }
    }

    # authorized actual request
    response = client.json.post(requests_url, {
        'group': fellowship.external_id,
        'path': '/manager/',
        'system': 'simple'
    })

    assert response.status_code == 201
    data = response.json()
    assert data['data'] == {'role': 'manager'}
    assert data['user'] is None
    assert data['group']['id'] == fellowship.external_id
    assert data['group']['name'] == fellowship.name
    assert data['group']['slug'] == fellowship.slug
    assert data['system']['slug'] == 'simple'

    assert Role.objects.count() == 1
    role = Role.objects.get()

    role.fetch_node()
    assert role.node.data == {'role': 'manager'}
    assert role.user is None
    assert role.group_id == fellowship.id
    assert role.system_id == simple_system.id
    assert role.state == 'requested'


def test_request_role_for_system_not_providing_required_fields(client, simple_system, arda_users, requests_url):
    """Проверяем, что нельзя запросить роль, не предоставив поля, помеченные как required.
    В то же время поля, не помеченные required, должно быть можно не предоставлять.
    """
    passport_login = simple_system.root_role_node.get_children().get().fields.get(slug='passport-login')
    passport_login.is_required = False
    passport_login.save()

    client.login('frodo')
    response = client.json.post(requests_url, {
        'user': 'frodo',
        'path': '/manager/',
        'system': 'simple',
        'comment': '',
    })
    assert response.status_code == 201
    assert response.json()['human'] == 'Роль: Менеджер'  # сразу выводим информацию о роли

    assert Role.objects.count() == 1
    # поменяем required на True
    passport_login.is_required = True
    passport_login.save()
    response = client.json.post(requests_url, {
        'user': 'frodo',
        'path': '/manager/',
        'system': 'simple',
        'comment': '',
    })
    assert response.status_code == 409
    assert response.json() == {
        'error_code': 'CONFLICT',
        'simulated': False,
        'message': 'Некоторые поля заполнены некорректно',
        'errors': {
            'fields_data': {
                'passport-login': ['Обязательное поле.']
            }
        }
    }


def test_request_role_with_optional_login_for_groups(client, simple_system, arda_users, department_structure,
                                                     requests_url):
    """Даже если passport-login обязателен, при запросе роли на группу мы его не требуем"""
    set_workflow(simple_system, group_code='approvers = []')
    passport_login = simple_system.root_role_node.get_children().get().fields.get(slug='passport-login')
    passport_login.is_required = True
    passport_login.save()

    client.login('frodo')

    response = client.json.post(requests_url, {
        'path': '/manager/',
        'system': 'simple',
        'comment': '',
    })
    assert response.status_code == 400

    response = client.json.post(requests_url, {
        'user': 'frodo',
        'path': '/manager/',
        'system': 'simple',
        'comment': '',
    })
    assert response.status_code == 409
    assert response.json() == {
        'error_code': 'CONFLICT',
        'simulated': False,
        'message': 'Некоторые поля заполнены некорректно',
        'errors': {
            'fields_data': {
                'passport-login': ['Обязательное поле.']
            }
        }
    }

    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    response = client.json.post(requests_url, {
        'group': fellowship.external_id,
        'path': '/manager/',

        'system': 'simple',
        'comment': '',
    })
    assert response.status_code == 201
    assert response.json()['human'] == 'Роль: Менеджер'
    assert Role.objects.count() == fellowship.members.count() + 1


def test_request_role_invalid_data(client, simple_system, arda_users, requests_url):
    """Проверяем, что нельзя запросить роль, предоставив невалидные данные."""
    node = simple_system.root_role_node.get_children().get()
    node.fields.update(is_active=False)
    node.fields.create(
        slug='qty',
        name='Количество',
        name_en='Quantity',
        is_required=True,
        type=FIELD_TYPE.INTEGER
    )

    client.login('frodo')
    # предоставим валидные данные
    response = client.json.post(requests_url, {
        'user': 'frodo',
        'path': '/manager/',
        'system': 'simple',
        'comment': '',
        'fields_data': {
            'qty': 9000
        }
    })
    assert response.status_code == 201
    # сразу выводим информацию о роли
    assert response.json()['fields_data'] == {'qty': 9000}
    assert Role.objects.count() == 1
    # предоставим невалидные данные
    response = client.json.post(requests_url, {
        'user': 'frodo',
        'path': '/manager/',
        'system': 'simple',
        'comment': '',
        'fields_data': {
            'qty': 'сто пятьдесять два с осьмушкой, пожалуйста'
        }
    })
    assert response.status_code == 409
    assert response.json() == {
        'error_code': 'CONFLICT',
        'simulated': False,
        'message': 'Некоторые поля заполнены некорректно',
        'errors': {
            'fields_data': {
                'qty': ['Введите целое число.']
            }
        }
    }


def test_request_role_invalid_suggest_data(client, simple_system, arda_users, requests_url):
    """Проверяем, что нельзя запросить роль, предоставив невалидные данные."""
    node = simple_system.root_role_node.get_children().get()
    node.fields.update(is_active=False)
    node.fields.create(
        slug='macro',
        name='Сетевой макрос',
        name_en='Network macro',
        is_required=True,
        type=FIELD_TYPE.SUGGEST,
        options={'suggest': 'macros'}
    )

    macro = NetworkMacro.objects.create(slug='_ARDA_NETS_')

    client.login('frodo')
    # предоставим валидные данные
    response = client.json.post(requests_url, {
        'user': 'frodo',
        'path': '/manager/',
        'system': 'simple',
        'comment': '',
        'fields_data': {
            'macro': macro.slug,
        }
    })
    assert response.status_code == 201
    # сразу выводим информацию о роли
    assert response.json()['fields_data'] == {'macro': macro.slug}
    assert Role.objects.count() == 1
    # предоставим невалидные данные
    response = client.json.post(requests_url, {
        'user': 'frodo',
        'path': '/manager/',
        'system': 'simple',
        'comment': '',
        'fields_data': {
            'macro': '_NUMENOR_NETS_'
        }
    })
    assert response.status_code == 409
    assert response.json() == {
        'error_code': 'CONFLICT',
        'simulated': False,
        'message': 'Некоторые поля заполнены некорректно',
        'errors': {
            'fields_data': {
                'macro': ['Object with id=_NUMENOR_NETS_ is not a valid object from suggest "macros"']
            }
        }
    }


def test_request_role_sudo_validator(client, simple_system, arda_users, requests_url):
    """Проверка правила sudoers"""
    node = simple_system.root_role_node.get_children().get()
    node.fields.update(is_active=False)
    node.fields.create(
        slug='sudo',
        name='Sudo',
        name_en='Sudo',
        is_required=True,
        type=FIELD_TYPE.CHARFIELD,
        options={'validators': ['sudoers_entry']},
    )

    client.login('frodo')
    # предоставим валидные данные
    response = client.json.post(requests_url, {
        'user': 'frodo',
        'path': '/manager/',
        'system': 'simple',
        'comment': '',
        'fields_data': {
            'sudo': 'ALL=(ALL) ALL'
        }
    })
    assert response.status_code == 201
    assert response.json()['fields_data'] == {'sudo': 'ALL=(ALL) ALL'}
    assert Role.objects.count() == 1

    # предоставим невалидные данные
    response = client.json.post(requests_url, {
        'user': 'frodo',
        'path': '/manager/',
        'system': 'simple',
        'comment': '',
        'fields_data': {
            'sudo': 'XXX'
        }
    })
    assert response.status_code == 409
    data = response.json()
    assert data['message'] == 'Некоторые поля заполнены некорректно'
    assert 'Ошибка в правиле sudoers' in data['errors']['fields_data']['sudo'][0]


def test_post_role_requests_with_passport_login(client, simple_system, users_for_test, requests_url):
    """
    POST /frontend/rolerequests/
    """
    set_roles_tree(simple_system, {
        'fields': [
            {
                'name': 'Паспортный логин',
                'required': True,
                'slug': 'passport-login'
            }
        ],
        'roles': {
            'values': {
                'superuser': 'суперпользователь',
                'manager': 'менеджер'
            },
            'name': 'роль',
            'slug': 'role'
        }
    })
    set_workflow(simple_system, 'approvers = ["art"]')

    (art, fantom, terran, admin) = users_for_test

    client.login('fantom')

    # Запрашиваем роль

    data = {
        'user': 'fantom',
        'path': '/manager/',
        'system': 'simple',
        'fields_data': {'passport-login': 'abc'},
    }

    client.json.post(requests_url, data=data)

    assert Role.objects.count() == 1

    # Запрашиваем точно такую же роль

    response = client.json.post(requests_url, data=data)
    assert response.status_code == 409
    response_data = response.json()
    expected_data = {
        'error_code': 'CONFLICT',
        'message': ('У пользователя "Легионер Тит" уже есть такая роль (Роль: менеджер) '
                    'в системе "Simple система" в состоянии "Запрошена"'),
        'simulated': False,
    }
    assert response_data == expected_data

    assert Role.objects.count() == 1

    # Cимулируем запрос точно такой же роли

    data['simulate'] = True

    response = client.json.post(requests_url, data)

    assert Role.objects.count() == 1
    assert response.status_code == 409
    resp_data = response.json()
    expected_message = ('У пользователя "Легионер Тит" уже есть такая роль (Роль: менеджер) '
                        'в системе "Simple система" в состоянии "Запрошена"')
    assert resp_data == {
        'error_code': 'CONFLICT',
        'simulated': True,
        'message': expected_message
    }

    # Меняем паспортный логин и симулирем запрос

    data['fields_data']['passport-login'] = 'def'

    response = client.json.post(requests_url, data=data)
    assert response.status_code == 200

    resp_data = response.json()
    assert resp_data['simulated'] is True
    assert 'approvers' in resp_data
    assert Role.objects.count() == 1

    # Запрашиваем такую роль

    del data['simulate']

    response = client.json.post(requests_url, data)

    assert response.status_code == 201
    assert Role.objects.count() == 2

    # Симулируем другую роль с этим же паспортным логином

    data['simulate'] = True
    data['path'] = '/superuser/'

    response = client.json.post(requests_url, data)

    resp_data = response.json()
    assert resp_data['simulated'] is True
    assert 'approvers' in resp_data
    assert Role.objects.count() == 2

    # Запрашиваем эту роль

    del data['simulate']

    response = client.json.post(requests_url, data)

    assert response.status_code == 201
    assert Role.objects.count() == 3


def test_post_role_requests_with_passport_login_for_system_with_passport_policy(client, simple_system, users_for_test,
                                                                                requests_url):
    """
    POST /frontend/rolerequests/
    Тестируем проверку работы политики паспортных логинов "один логин на все роли пользователя системы"
    """
    set_roles_tree(simple_system, {
        'fields': [
            {
                'name': 'Паспортный логин',
                'required': True,
                'slug': 'passport-login'
            }
        ],
        'roles': {
            'values': {
                'superuser': 'суперпользователь',
                'manager': 'менеджер'
            },
            'name': 'роль',
            'slug': 'role'
        }
    })
    simple_system.passport_policy = 'unique_for_user'
    simple_system.save()
    set_workflow(simple_system, 'approvers = ["art"]')

    (art, fantom, terran, admin) = users_for_test

    client.login('fantom')

    # Запрашиваем роль

    data = {
        'user': 'fantom',
        'path': '/manager/',
        'system': 'simple',
        'fields_data': {'passport-login': 'abc'},
    }

    client.json.post(requests_url, data)

    assert Role.objects.count() == 1
    base_role = Role.objects.get()

    # Запрашиваем точно такую же роль

    response = client.json.post(requests_url, data)
    assert response.status_code == 409
    response_data = response.json()
    expected_data = {
        'error_code': 'CONFLICT',
        'message': ('У пользователя "Легионер Тит" уже есть такая роль (Роль: менеджер) '
                    'в системе "Simple система" в состоянии "Запрошена"'
                    ),
        'simulated': False,
    }
    assert response_data == expected_data

    assert Role.objects.count() == 1

    # Пытаемся запросить для того же пользователя другую роль на другой паспортный логин
    base_role.fetch_user()
    UserPassportLogin.objects.add('abc', base_role)
    data['path'] = '/superuser/'
    data['fields_data']['passport-login'] = 'def'

    response = client.json.post(requests_url, data)
    assert response.status_code == 409
    response_data = response.json()
    expected_data = {
        'error_code': 'CONFLICT',
        'message': 'В связи с политикой системы "Simple система" в отношении паспортных логинов у пользователя '
                   'fantom не может быть второго паспортного логина',
        'simulated': False
    }
    assert response_data == expected_data

    assert Role.objects.count() == 1

    # Симулируем запрос роли для того же пользователя на запрос другой роли на другой паспортный логин
    data['simulate'] = True

    response = client.json.post(requests_url, data)

    assert Role.objects.count() == 1
    assert response.status_code == 409
    assert response.json() == {
        'error_code': 'CONFLICT',
        'simulated': True,
        'message': 'В связи с политикой системы "Simple система" в отношении паспортных логинов '
                   'у пользователя fantom не может быть второго паспортного логина'
    }


@pytest.mark.parametrize('date_field', ['deprive_at', 'review_at'])
def test_post_role_requests_w_expire_or_review_date(client, simple_system, users_for_test, requests_url, date_field):
    """
    POST /frontend/rolerequests/
    with expire_date/review_date for role
    """
    set_workflow(simple_system, 'approvers = ["art"]')

    (art, fantom, terran, admin) = users_for_test

    client.login('admin')

    # bad date
    data = {
        'path': '/superuser/',
        'user': 'terran',
        'system': 'simple',
        date_field: 'not a date!'
    }
    response = client.json.post(requests_url, data)

    assert response.status_code == 400

    # date in the past
    dt = timezone.localtime(timezone.now() - timedelta(days=2))
    data = {
        'path': '/superuser/',
        'user': 'terran',
        'system': 'simple',
        date_field: dt.strftime('%Y-%m-%d')
    }
    response = client.json.post(requests_url, data)
    assert response.status_code == 400
    if date_field == 'deprive_at':
        errors = {
            'deprive_at': ['Нельзя запросить временную роль на дату в прошлом: "%s".' % dt.date().isoformat()]
        }
    elif date_field == 'review_at':
        errors = {
            'review_at': ['Нельзя назначить пересмотр на дату в прошлом: "%s".' % dt.date().isoformat()]
        }
    else:
        assert False
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Invalid data sent',
        'errors': errors
    }

    # valid expire date
    dt = timezone.localtime(timezone.now() + timedelta(days=5)).date()
    data = {
        'path': '/superuser/',
        'user': 'terran',
        'system': 'simple',
        date_field: dt.strftime('%Y-%m-%d')
    }
    response = client.json.post(requests_url, data)

    assert response.status_code == 201
    resp_data = response.json()
    assert resp_data['data'] == {'role': 'superuser'}
    assert resp_data['user']['username'] == 'terran'
    assert resp_data['system']['slug'] == 'simple'

    assert Role.objects.count() == 1
    role = Role.objects.get()

    role.fetch_node()
    assert role.node.data == {'role': 'superuser'}
    assert role.user_id == terran.id
    assert role.actions.get(action='request').requester_id == admin.id
    assert role.system_id == simple_system.id
    assert role.ttl_days is None
    if date_field == 'deprive_at':
        assert timezone.localtime(role.ttl_date).date() == dt + timedelta(days=1)
    elif date_field == 'review_at':
        assert timezone.localtime(role.review_date).date() == dt + timedelta(days=1)
    else:
        assert False


def test_post_role_requests_w_ttl(client, simple_system, users_for_test, requests_url):
    data = {
        'path': '/superuser/',
        'user': 'terran',
        'system': 'simple',
        'deprive_after_days': 5,
    }
    client.login('admin')
    response = client.json.post(requests_url, data)

    assert response.status_code == 201

    assert Role.objects.count() == 1
    role = Role.objects.all()[0]

    role.fetch_node()
    assert role.node.data == {'role': 'superuser'}
    assert role.ttl_days == 5
    assert role.ttl_date is None


def test_post_role_requests_w_both_ttl_fields(client, simple_system, users_for_test, requests_url):
    dt = timezone.localtime(timezone.now() + timedelta(days=5)).date()
    data = {
        'path': '/superuser/',
        'user': 'terran',
        'system': 'simple',
        'deprive_at': dt.strftime('%Y-%m-%d'),
        'deprive_after_days': 5,

    }
    client.login('admin')
    response = client.json.post(requests_url, data)

    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Нельзя передавать одновременно время жизни роли в днях и дату истечения',
        'errors': {
            '__all__': ['Нельзя передавать одновременно время жизни роли в днях и дату истечения']
        }
    }


def test_post_role_request_expiration_answer(client, simple_system, users_for_test, requests_url):
    """
        если в воркфлоу нет ничего про срок, и запрошена бессрочная роль, то выводим сообщение "бессрочная роль"
        если в воркфлоу нет ничего про срок, и запрошена временная роль, то выводим сообщение, дублирующее срок - "на N дней" или "до X числа".
    """
    data = {
        'path': '/superuser/',
        'user': 'terran',
        'system': 'simple',
        'simulate': True
    }
    client.login('admin')
    response = client.json.post(requests_url, data)
    assert response.json()['expire_date'] is None

    data['deprive_after_days'] = 6
    result = client.json.post(requests_url, data).json()
    assert result['ttl_days'] == 6
    assert result['expire_date'] is None

    del data['deprive_after_days']
    dt = timezone.now() + timedelta(days=5)
    data['deprive_at'] = dt.strftime('%Y-%m-%d')
    result = client.json.post(requests_url, data).json()
    expire = datetime.strptime(result['expire_date'], '%Y-%m-%dT%H:%M:%S')
    assert compare_time(expire, (dt + timedelta(1)).replace(hour=0, minute=0, second=0, tzinfo=None))
    assert result['ttl_days'] is None


def test_post_role_request_expiration_answer_w_workflow(client, simple_system, users_for_test, requests_url):
    """
        если в воркфлоу прописано число дней жизни роли, и запрошена бессрочная роль, то выводим сообщение "роль будет запрошена на N дней".
        если в воркфлоу прописано число X дней жизни роли, и запрошена роль на Y дней, то выводим "роль будет запрошена на min(X,Y) дней"
        если в воркфлоу прописано число X дней жизни роли, и запрошена роль до числа Z, то:
            если Z <= today+X, то выводим сообщение "роль будет запрошена до числа Z"
            если Z > today+X, то выводим сообщение "роль будет запрошена на X дней" (игнорируя желание пользователя).
    """
    set_workflow(simple_system, 'ttl_days = 5; approvers = []')
    data = {
        'path': '/superuser/',
        'user': 'terran',
        'system': 'simple',
        'simulate': True
    }
    client.login('admin')
    result = client.json.post(requests_url, data).json()
    assert result['ttl_days'] == 5
    assert result['expire_date'] is None

    data['deprive_after_days'] = 6
    result = client.json.post(requests_url, data).json()
    assert result['ttl_days'] == 5
    assert result['expire_date'] is None

    data['deprive_after_days'] = 1
    result = client.json.post(requests_url, data).json()
    assert result['ttl_days'] == 1
    assert result['expire_date'] is None

    del data['deprive_after_days']
    data['deprive_at'] = (timezone.localtime(timezone.now()).date() + timedelta(days=7)).strftime('%Y-%m-%d')
    result = client.json.post(requests_url, data).json()
    assert result['ttl_days'] == 5
    assert result['expire_date'] is None

    dt = timezone.now() + timedelta(days=3)
    data['deprive_at'] = dt.strftime('%Y-%m-%d')
    result = client.json.post(requests_url, data).json()
    expire = datetime.strptime(result['expire_date'], '%Y-%m-%dT%H:%M:%S')
    assert compare_time(expire, (dt + timedelta(1)).replace(hour=0, minute=0, second=0, tzinfo=None))
    assert result['ttl_days'] is None


def test_post_role_request_review_date_answer_w_workflow(client, simple_system, users_for_test, requests_url):
    """
        если в воркфлоу прописано число дней до пересмотра роли, и запрошена роль без указания даты пересмотра, то выводим сообщение "роль будет перезапрошена через N дней".
        если в воркфлоу прописано число X дней до пересмотра роли, и запрошена роль с пересмотром до числа Z, то:
            если Z <= today+X, то выводим сообщение "роль будет перезапрошена в день Z"
            если Z > today+X, то выводим сообщение "роль будет перезапрошена через X дней" (игнорируя желание пользователя).
    """
    set_workflow(simple_system, 'review_days = 5; approvers = []')
    data = {
        'path': '/superuser/',
        'user': 'terran',
        'system': 'simple',
        'simulate': True
    }
    client.login('admin')
    result = client.json.post(requests_url, data).json()
    assert result['review_days'] == 5
    assert result['review_date'] is None

    data['review_at'] = (timezone.localtime(timezone.now()).date() + timedelta(days=7)).strftime('%Y-%m-%d')
    result = client.json.post(requests_url, data).json()
    assert result['review_days'] == 5
    assert result['review_date'] is None

    dt = timezone.now() + timedelta(days=3)
    data['review_at'] = dt.strftime('%Y-%m-%d')
    result = client.json.post(requests_url, data).json()
    expire = datetime.strptime(result['review_date'], '%Y-%m-%dT%H:%M:%S')
    assert compare_time(expire, (dt + timedelta(1)).replace(hour=0, minute=0, second=0, tzinfo=None))
    assert result['review_days'] is None


def test_post_role_workflow_access_denied_nomessage(client, simple_system, arda_users, requests_url):
    """
        Проверим, что если в workflow AccessDenied без текста, то message не пустой
    """

    data = {
        'path': '/superuser/',
        'user': 'frodo',
        'system': 'simple',
    }
    client.login('frodo')

    # пустое сообщение об ошибке
    set_workflow(simple_system, 'raise AccessDenied()')
    response = client.json.post(requests_url, data)
    assert response.json()['message'] == 'Запрос роли запрещён согласно правилам workflow'

    # непустое сообщение об ошибке
    set_workflow(simple_system, 'raise AccessDenied(\'Какая-то ошибка\')')
    response = client.json.post(requests_url, data)
    assert response.json()['message'] == 'Какая-то ошибка'


def test_warnings_in_workflow(client, simple_system, arda_users, requests_url):
    client.login('frodo')
    params = {
        'path': '/superuser/',
        'user': 'frodo',
        'system': 'simple',
        'simulate': True
    }

    # есть warnings
    set_workflow(simple_system, "warnings=['Ой']; approvers = []")
    data = client.json.post(requests_url, params).json()
    assert data['warnings'] == ['Ой']

    # есть warning
    set_workflow(simple_system, "warning='Ой'; approvers = []")
    data = client.json.post(requests_url, params).json()
    assert data['warnings'] == ['Ой']

    # warning - словарь
    set_workflow(simple_system, "warning={'ru': 'Ой'}; approvers = []")
    data = client.json.post(requests_url, params).json()
    assert data['warnings'] == ['Ой']

    # есть и warning и warnings
    set_workflow(simple_system, "warning='Ой'; warnings=['Ай']; approvers = []")
    data = client.json.post(requests_url, params).json()
    assert data['warnings'] == ['Ай', 'Ой']

    # нет ни warning ни warnings
    set_workflow(simple_system, "approvers = []")
    data = client.json.post(requests_url, params).json()
    assert data['warnings'] == []

    # нет нужных ключей в словаре
    set_workflow(simple_system, "warning={'jp': 'Ой'}; approvers = []")
    data = client.json.post(requests_url, params).json()
    assert data['warnings'] == ['']

    # не строка и не словарь в warning
    set_workflow(simple_system, "warning=1; approvers = []")
    response = client.json.post(requests_url, params)
    assert response.status_code == 409
    assert response.json()['message'] == 'warning: Неверный формат поля'

    # не строки в словаре
    set_workflow(simple_system, "warning={'ru': [], 'en': 555}; approvers = []")
    response = client.json.post(requests_url, params)
    assert response.status_code == 409
    assert response.json()['message'] == 'warning: Неверный формат поля'

    # не строки в warnings
    set_workflow(simple_system, "warnings=[1,2,3]; approvers = []")
    response = client.json.post(requests_url, params)
    assert response.status_code == 409
    assert response.json()['message'] == (
        'warnings: "1" - Неверный формат поля, '
        '"2" - Неверный формат поля, '
        '"3" - Неверный формат поля'
    )


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AVAILABLE)
@pytest.mark.parametrize('language', ['en', 'ru'])
@pytest.mark.parametrize('fields_data', (None, {'login': 'yndx.frodo'}))
def test_user_role_when_group_present(client, simple_system, arda_users, requests_url, department_structure,
                                      language, group_policy, fields_data):
    add_perms_by_role('responsible', arda_users.sauron, simple_system)
    simple_system.group_policy = group_policy
    simple_system.save()
    client.login('sauron', lang=language)
    set_workflow(simple_system, group_code='approvers = []')
    params = {
        'path': '/superuser/',
        'user': 'frodo',
        'system': 'simple',
        'simulate': True
    }
    if fields_data:
        wrong_data = None
        params['fields_data'] = fields_data
    else:
        wrong_data = {'login': 'yndx.frodo'}

    data = client.json.post(requests_url, params).json()
    assert data['warnings'] == []

    group_role = Role.objects.request_role(
        arda_users.sauron,
        department_structure.fellowship,
        simple_system,
        '',
        {'role': 'superuser'},
        fields_data=wrong_data
    )
    if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS:
        force_awaiting_memberships_role_grant(group_role)

    data = client.json.post(requests_url, params).json()
    assert data['warnings'] == []

    role = Role.objects.request_role(
        arda_users.sauron,
        department_structure.fellowship,
        simple_system,
        '',
        {'role': 'superuser'},
        fields_data=fields_data
    )

    data = client.json.post(requests_url, params).json()
    if language == 'ru':
        expected = 'Вы запрашиваете роль, которая уже выдана Фродо Бэггинс на основе следующих групп: Братство кольца'
    else:
        expected = ('You are requesting a role that has already been granted for the following groups '
                    'of Frodo Baggins: Fellowship of the Ring')
    assert data['warnings'] == [expected]


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AVAILABLE)
@pytest.mark.parametrize('language', ['en', 'ru'])
def test_group_role_when_parent_present(client, simple_system, arda_users, requests_url, department_structure,
                                        language, group_policy):
    add_perms_by_role('responsible', arda_users.sauron, simple_system)
    simple_system.group_policy = group_policy
    simple_system.save()
    client.login('sauron', lang=language)
    set_workflow(simple_system, group_code='approvers = []')
    params = {
        'path': '/superuser/',
        'group': department_structure.shire.external_id,
        'system': 'simple',
        'simulate': True
    }

    data = client.json.post(requests_url, params).json()
    assert data['warnings'] == []

    role = Role.objects.request_role(
        arda_users.sauron,
        department_structure.earth,
        simple_system,
        '',
        {'role': 'superuser'},
    )

    if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS:
        force_awaiting_memberships_role_grant(role)

    data = client.json.post(requests_url, params).json()
    if language == 'ru':
        expected = 'Вы запрашиваете роль, которая уже выдана следующим родителям группы Шир: Средиземье'
    else:
        expected = ('You are requesting a role that has already been granted to the following parents '
                    'of The Shire: Middle Earth')
    assert data['warnings'] == [expected]


@pytest.mark.parametrize('language', ['en', 'ru'])
def test_ref_roles_with_nonexistent_fields(client, simple_system, arda_users, requests_url, department_structure,
                                           language):
    add_perms_by_role('responsible', arda_users.sauron, simple_system)
    client.login('sauron', lang=language)

    set_workflow(simple_system, dedent('''
        approvers = []
        ref_roles = [
            {
                'system': 'simple',
                'role_data': {'role': 'manager'},
                'role_fields': {'scope': 'qwerty'}
            }
        ]
    '''))

    params = {
        'path': '/superuser/',
        'user': 'frodo',
        'system': 'simple',
        'simulate': True
    }

    data = client.json.post(requests_url, params).json()
    if language == 'ru':
        expected = (
            "В связи с тем, что workflow системы Simple система содержит ошибки, связанная роль "
            "\"{'system': 'simple', 'role_data': {'role': 'manager'}, 'role_fields': {'scope': 'qwerty'}}\" "
            "не будет выдана. После исправления workflow потребуется перезапрос роли."
        )
    else:
        expected = (
            "Reference role \"{'system': 'simple', 'role_data': {'role': 'manager'}, 'role_fields': "
            "{'scope': 'qwerty'}}\" could not be requested due to errors in workflow of system "
            "Simple system. Role will require a rerequest when the workflow is fixed."
        )

    assert data['warnings'] == [expected]


def test_post_tvm_role_requests(client, simple_system, arda_users, requests_url):
    """
    POST /frontend/rolerequests/
    """
    set_workflow(simple_system, 'approvers = []')

    client.login(arda_users['frodo'])

    # Нельзя запрашивать роли в системах, которые не поддерживают роли для tvm-приложений
    response = client.json.post(requests_url, {
        'path': '/manager/',
        'system': 'simple',
        'user': arda_users.tvm_app.username,
    })

    assert response.status_code == 409
    assert response.json()['message'] == "Система `%s` не поддерживает роли для tvm-приложений" % simple_system.slug
    assert Role.objects.count() == 0
