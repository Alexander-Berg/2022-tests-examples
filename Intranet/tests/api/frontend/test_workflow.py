# coding: utf-8
from textwrap import dedent

import pytest
from django.core import mail

from idm.core.constants.role import ROLE_STATE
from idm.core.models import Workflow, System
from idm.tests.utils import create_user, add_perms_by_role, set_workflow, create_system, refresh, get_recievers, \
    DEFAULT_WORKFLOW
from idm.users.models import Group
from idm.utils import reverse
from idm.tests.api.frontend.test_role import get_role_url

pytestmark = pytest.mark.django_db


@pytest.fixture
def users(simple_system, pt1_system):
    """ Create editor and approver
    """
    terran = create_user('terran')

    editor = create_user('editor')
    add_perms_by_role('users_view', editor, simple_system)

    approver_superuser = create_user('approver_superuser')
    add_perms_by_role('superuser', approver_superuser)

    approver_security = create_user('approver_security')
    add_perms_by_role('security', approver_security)

    approver_responsible = create_user('approver_responsible')
    add_perms_by_role('responsible', approver_responsible, simple_system)

    approver_other = create_user('approver_other')
    add_perms_by_role('responsible', approver_other, pt1_system)

    approvers = [approver_superuser, approver_security, approver_responsible, approver_other]

    return terran, editor, approvers


def test_get_system_workflow(client, simple_system, users, idm_robot):
    """
    GET /systems/simple/workflow
    """
    terran, editor, _ = users

    client.login('editor')

    data = client.json.get(reverse(
        'api_get_system_workflow',
        api_name='frontend',
        resource_name='systems',
        slug='simple'
    )).json()

    expected_data = {
        'can_edit': True,
        'develop_workflow': [],
        'committed_workflow': [],
        'system_id': simple_system.pk,
        'workflow': DEFAULT_WORKFLOW,
        'group_workflow': DEFAULT_WORKFLOW,
        'updated': simple_system.get_workflow_changed_date().isoformat(),
    }
    simple_system.actual_workflow.fetch_parent()
    expected_history = [{
        'id': simple_system.actual_workflow.pk,
        'approver': {'login': idm_robot.username},
        'author': {'login': idm_robot.username},
        'diff': '@@ -0,0 +1 @@\n+approvers = []',
        'group_diff': '@@ -0,0 +1 @@\n+approvers = []',
        'updated': simple_system.actual_workflow.approved.isoformat(),
        'workflow': DEFAULT_WORKFLOW,
        'group_workflow': DEFAULT_WORKFLOW,
    }, {
        'id': simple_system.actual_workflow.parent.pk,
        'approver': {'login': idm_robot.username},
        'author': {'login': idm_robot.username},
        'diff': '',
        'group_diff': '',
        'updated': simple_system.actual_workflow.parent.approved.isoformat(),
        'workflow': '',
        'group_workflow': '',
    }]
    history = data.pop('history')
    assert data == expected_data
    assert history == expected_history


def test_system_workflow_forbidden(client, simple_system, users):
    """
    GET /systems/simple/workflow
    """
    client.login('terran')

    system_url = reverse('api_get_system_workflow', api_name='frontend', resource_name='systems', slug='simple')
    response = client.json.get(system_url)
    assert response.status_code == 403
    assert response.json() == {
        'error_code': 'FORBIDDEN',
        'message': 'You cannot view workflow'
    }


def test_get_committed_workflows(client, simple_system, users, idm_robot):
    """
    GET /systems/simple/workflow
    """
    terran, editor, _ = users
    wf_url = reverse(
        'api_get_system_workflow',
        api_name='frontend',
        resource_name='systems',
        slug='simple'
    )

    initial = simple_system.workflows.get(parent=None)
    workflow = Workflow.objects.create(
        system=simple_system,
        user=editor,
        workflow='',
        comment='Hi',
        state='edit',
        parent=initial,
    )

    # У автора - в секции develop_workflow
    client.login('editor')

    data = client.json.get(wf_url).json()

    assert data == {
        'can_edit': True,
        'develop_workflow': [{
            'id': workflow.pk,
            'added': workflow.added.isoformat(),
            'updated': workflow.updated.isoformat(),
            'state': 'edit',
            'comment': 'Hi',
        }],
        'history': [{
            'id': simple_system.actual_workflow.pk,
            'approver': {'login': idm_robot.username},
            'author': {'login': idm_robot.username},
            'diff': '@@ -0,0 +1 @@\n+approvers = []',
            'group_diff': '@@ -0,0 +1 @@\n+approvers = []',
            'updated': simple_system.actual_workflow.approved.isoformat(),
            'workflow': DEFAULT_WORKFLOW,
            'group_workflow': DEFAULT_WORKFLOW,
        }, {
            'approver': {'login': idm_robot.username},
            'author': {'login': idm_robot.username},
            'diff': '',
            'group_diff': '',
            'id': initial.pk,
            'updated': initial.approved.isoformat(),
            'workflow': '',
            'group_workflow': '',
        }],
        'committed_workflow': [],
        'system_id': simple_system.pk,
        'workflow': DEFAULT_WORKFLOW,
        'group_workflow': DEFAULT_WORKFLOW,
        'updated': simple_system.get_workflow_changed_date().isoformat(),
    }

    # У аппрувера - нигде
    client.login('approver_superuser')

    data = client.json.get(wf_url).json()

    assert data == {
        'can_edit': True,
        'develop_workflow': [],
        'committed_workflow': [],
        'history': [{
            'id': simple_system.actual_workflow.pk,
            'approver': {'login': idm_robot.username},
            'author': {'login': idm_robot.username},
            'diff': '@@ -0,0 +1 @@\n+approvers = []',
            'group_diff': '@@ -0,0 +1 @@\n+approvers = []',
            'updated': simple_system.actual_workflow.approved.isoformat(),
            'workflow': DEFAULT_WORKFLOW,
            'group_workflow': DEFAULT_WORKFLOW
        }, {
            'approver': {'login': idm_robot.username},
            'author': {'login': idm_robot.username},
            'diff': '',
            'group_diff': '',
            'id': initial.pk,
            'updated': initial.approved.isoformat(),
            'workflow': '',
            'group_workflow': '',
        }],
        'system_id': simple_system.pk,
        'workflow': DEFAULT_WORKFLOW,
        'group_workflow': DEFAULT_WORKFLOW,
        'updated': simple_system.get_workflow_changed_date().isoformat(),
    }

    # после смены статуса оно появится в committed_workflow для аппрувера
    workflow.commit(editor)
    data = client.json.get(wf_url).json()

    assert data == {
        'can_edit': True,
        'develop_workflow': [],
        'committed_workflow': [{
            'id': workflow.pk,
            'added': workflow.added.isoformat(),
            'updated': workflow.updated.isoformat(),
            'author': {'login': 'editor'},
            'comment': 'Hi',
        }],
        'history': [{
            'id': simple_system.actual_workflow.pk,
            'approver': {'login': idm_robot.username},
            'author': {'login': idm_robot.username},
            'diff': '@@ -0,0 +1 @@\n+approvers = []',
            'group_diff': '@@ -0,0 +1 @@\n+approvers = []',
            'updated': simple_system.actual_workflow.approved.isoformat(),
            'workflow': DEFAULT_WORKFLOW,
            'group_workflow': DEFAULT_WORKFLOW
        }, {
            'approver': {'login': idm_robot.username},
            'author': {'login': idm_robot.username},
            'diff': '',
            'group_diff': '',
            'id': initial.pk,
            'updated': initial.approved.isoformat(),
            'workflow': '',
            'group_workflow': '',
        }],
        'system_id': simple_system.pk,
        'workflow': DEFAULT_WORKFLOW,
        'group_workflow': DEFAULT_WORKFLOW,
        'updated': simple_system.get_workflow_changed_date().isoformat(),
    }


def test_initial_workflow(client, arda_users, idm_robot):
    """Проверим, что будет, если сразу после создания системы поменять ей workflow"""

    frodo = arda_users.frodo

    simple_system = create_system(
        'simple',
        'idm.tests.base.SimplePlugin',
        name='Simple система',
        name_en='Simple system',
        emails='simple@yandex-team.ru, simplesystem@yandex-team.ru',
        workflow=None,
    )
    add_perms_by_role('responsible', frodo, simple_system)

    client.login('frodo')
    clone_url = reverse(
        'api_clone_system_workflow',
        api_name='frontend',
        resource_name='systems',
        slug='simple'
    )
    response = client.json.post(clone_url)
    pk = response.json()['id']
    edit_url = reverse('api_dispatch_detail', api_name='frontend', resource_name='workflow', id=pk)
    response = client.json.post(edit_url, {'workflow': 'approvers = ["varda"]'})
    assert response.status_code == 200

    commit_url = reverse('api_workflow_commit', api_name='frontend', resource_name='workflow', id=pk)
    response = client.json.post(commit_url)
    assert response.status_code == 200

    approve_url = reverse('api_workflow_approve', api_name='frontend', resource_name='workflow', id=pk)
    response = client.json.post(approve_url)
    assert response.status_code == 200


def test_clone_system_workflow(client, simple_system, users, idm_robot):
    """
    POST /systems/simple/clone_workflow
    """
    terran, editor, _ = users
    previous_workflow = simple_system.actual_workflow

    client.login('editor')

    assert simple_system.workflows.count() == 2
    initial = simple_system.workflows.get(parent=None)

    data = client.json.post(reverse(
        'api_clone_system_workflow',
        api_name='frontend',
        resource_name='systems',
        slug='simple'
    )).json()

    assert Workflow.objects.filter(user=editor, state='edit').count() == 1
    workflow = Workflow.objects.get(user=editor, state='edit')

    assert data == {
        'id': workflow.id,
        'workflow': DEFAULT_WORKFLOW,
        'group_workflow': DEFAULT_WORKFLOW,
        'added': workflow.added.isoformat(),
        'updated': workflow.updated.isoformat(),
    }

    data = client.json.get(reverse(
        'api_get_system_workflow',
        api_name='frontend',
        resource_name='systems',
        slug='simple'
    )).json()

    assert data == {
        'can_edit': True,
        'system_id': simple_system.pk,
        'workflow': DEFAULT_WORKFLOW,
        'group_workflow': DEFAULT_WORKFLOW,
        'updated': simple_system.get_workflow_changed_date().isoformat(),
        'develop_workflow': [{
            'added': workflow.added.isoformat(),
            'id': workflow.id,
            'state': 'edit',
            'updated': workflow.updated.isoformat(),
            'comment': '',
        }],
        'committed_workflow': [],
        'history': [{
            'id': simple_system.actual_workflow.pk,
            'approver': {'login': idm_robot.username},
            'author': {'login': idm_robot.username},
            'diff': '@@ -0,0 +1 @@\n+approvers = []',
            'group_diff': '@@ -0,0 +1 @@\n+approvers = []',
            'updated': previous_workflow.approved.isoformat(),
            'workflow': DEFAULT_WORKFLOW,
            'group_workflow': DEFAULT_WORKFLOW
        }, {
            'approver': {'login': idm_robot.username},
            'author': {'login': idm_robot.username},
            'diff': '',
            'group_diff': '',
            'id': initial.pk,
            'updated': initial.approved.isoformat(),
            'workflow': '',
            'group_workflow': '',
        }],
    }


def test_clone_workflow(client, simple_system, users):
    assert Workflow.objects.filter(system=simple_system).count() == 2

    client.login('terran')
    res = client.json.post(reverse(
        'api_clone_system_workflow',
        api_name='frontend',
        resource_name='systems',
        slug='simple'
    ))

    assert res.status_code == 403
    assert Workflow.objects.filter(system=simple_system).count() == 2

    developer = create_user('developer')
    add_perms_by_role('developer', developer)

    client.login('developer')
    res = client.json.post(reverse(
        'api_clone_system_workflow',
        api_name='frontend',
        resource_name='systems',
        slug='simple'
    ))

    assert res.status_code == 200
    assert Workflow.objects.filter(system=simple_system).count() == 3
    assert Workflow.objects.filter(user=developer, state='edit').count() == 1


def test_clone_workflow_in_new_system(client, arda_users, idm_robot):
    """Проверим клонирование workflow у системы, у которой ещё нет actual_workflow"""

    system = create_system('simple', workflow=None)
    add_perms_by_role('responsible', arda_users.frodo, system=system)
    client.login('frodo')
    assert Workflow.objects.count() == 1
    response = client.json.post(reverse('api_clone_system_workflow', api_name='frontend', resource_name='systems',
                                        slug='simple'))
    assert response.status_code == 200
    assert Workflow.objects.count() == 2
    initial, workflow = Workflow.objects.order_by('pk')
    assert response.json() == {
        'id': workflow.id,
        'workflow': '',
        'group_workflow': '',
        'added': workflow.added.isoformat(),
        'updated': workflow.updated.isoformat(),
    }
    data = client.json.get(reverse('api_get_system_workflow', api_name='frontend', resource_name='systems',
                                   slug='simple')).json()
    expected_data = {
        'can_edit': True,
        'system_id': system.pk,
        'workflow': '',
        'group_workflow': '',
        'updated': initial.approved.isoformat(),
        'develop_workflow': [{
            'added': workflow.added.isoformat(),
            'id': workflow.id,
            'state': 'edit',
            'updated': workflow.updated.isoformat(),
            'comment': '',
        }],
        'committed_workflow': [],
        'history': [{
            'approver': {'login': idm_robot.username},
            'author': {'login': idm_robot.username},
            'diff': '',
            'group_diff': '',
            'id': initial.pk,
            'updated': initial.approved.isoformat(),
            'workflow': '',
            'group_workflow': '',
        }],
    }
    assert data == expected_data


def test_get_workflows(client, simple_system, users):
    """
    GET /workflow/
    """
    terran, editor, _ = users
    Workflow.objects.create(
        system=simple_system,
        user=editor,
        workflow='approvers = None',
        state='edit',
    )

    client.login('editor')
    response = client.get(reverse('api_dispatch_list', api_name='frontend', resource_name='workflow'))
    assert response.status_code == 405


def test_get_workflow(client, simple_system, users):
    """
    GET /workflow/<id>/
    """
    terran, editor, _ = users
    workflow = Workflow.objects.create(
        system=simple_system,
        user=editor,
        workflow='approvers = None',
        state='edit',
    )

    client.login('editor')

    data = client.json.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='workflow', id=workflow.id),
    ).json()

    assert data['id'] == workflow.id
    assert data['workflow'] == workflow.workflow
    assert data['system']['slug'] == workflow.system.slug


def test_edit_workflow(client, simple_system, users):
    """
    POST /workflow/<id>/
    """
    terran, editor, _ = users

    client.login('editor')

    # Создать рабочую копию
    data = client.json.post(reverse(
        'api_clone_system_workflow',
        api_name='frontend',
        resource_name='systems',
        slug='simple'
    )).json()

    workflow_id = data['id']

    # Отредактировать
    data = client.json.post(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='workflow', id=workflow_id),
        {'workflow': 'approvers = None', 'comment': 'Hi all!'},
    ).json()

    workflow = Workflow.objects.get(user=editor, state='edit')
    assert workflow.workflow == 'approvers = None'
    assert workflow.comment == 'Hi all!'

    assert data == {
        'id': workflow.id,
        'diff': '@@ -1 +1 @@\n-approvers = []\n+approvers = None',
        'group_diff': '@@ -1 +0,0 @@\n-approvers = []',
    }


def test_test_user_workflow(client, simple_system, users):
    """
    GET /workflow/<id>/test
    """

    terran, editor, _ = users
    workflow = Workflow.objects.create(
        system=simple_system,
        user=editor,
        workflow='approvers = [approver("terran")]',
        group_workflow='approvers = [approver("art")]',
        state='edit',
    )

    client.login('editor')

    data = client.json.post(
        reverse('api_workflow_test', api_name='frontend', resource_name='workflow', id=workflow.id),
        {
            'requester': editor.username,
            'user': editor.username,
            'path': '/superuser/',
        }
    ).json()
    expected = {
        'is_valid': True,
        'result': '[approver(terran, priority=1)]',
        'doctest': '''"""
>>> run(user("editor"), user("editor"), {u'role': u'superuser'})
[approver(terran, priority=1)]

"""''',
    }
    assert data == expected


def test_test_tvm_workflow_bad_request(client, simple_system, users):
    """
    GET /workflow/<id>/test
    """

    terran, editor, _ = users
    workflow = Workflow.objects.create(
        system=simple_system,
        user=editor,
        workflow='approvers = [approver("terran")]',
        group_workflow='approvers = [approver("art")]',
        state='edit',
    )

    assert not simple_system.use_tvm_role

    client.login('editor')

    response = client.json.post(
        reverse('api_workflow_test', api_name='frontend', resource_name='workflow', id=workflow.id),
        {
            'requester': editor.username,
            'user': editor.username,
            'user_type': 'tvm_app',
            'path': '/superuser/',
        }
    )
    assert response.status_code == 400
    assert response.json()['message'] == 'Система `simple` не поддерживает роли для tvm-приложений'


def test_test_tvm_workflow_ok(client, simple_system, users):
    """
    GET /workflow/<id>/test
    """

    terran, editor, _ = users
    workflow = Workflow.objects.create(
        system=simple_system,
        user=editor,
        workflow='approvers = [approver("terran")]',
        group_workflow='approvers = [approver("art")]',
        state='edit',
    )

    simple_system.use_tvm_role = True
    simple_system.save()
    editor.type = 'tvm_app'
    editor.save()

    client.login('editor')

    response = client.json.post(
        reverse('api_workflow_test', api_name='frontend', resource_name='workflow', id=workflow.id),
        {
            'requester': editor.username,
            'user': editor.username,
            'user_type': 'tvm_app',
            'path': '/superuser/',
        }
    )
    assert response.status_code == 200


def test_test_group_workflow(client, simple_system, arda_users, department_structure):
    """
    GET /workflow/<id>/test
    """

    frodo = arda_users.get('frodo')
    workflow = Workflow.objects.create(
        system=simple_system,
        user=frodo,
        workflow='approvers = ["gandalf"]',
        group_workflow='approvers = ["legolas"]',
        state='edit',
    )
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')

    client.login('frodo')

    response = client.json.post(
        reverse('api_workflow_test', api_name='frontend', resource_name='workflow', id=workflow.id),
        {
            'requester': frodo.username,
            'group': fellowship.external_id,
            'path': '/superuser/',
        }
    )

    expected = {
        'is_valid': True,
        'result': '[approver(legolas, priority=1)]',
        'doctest': '''"""
>>> run(user("frodo"), group(105), {u'role': u'superuser'})
[approver(legolas, priority=1)]

"""'''
    }
    assert response.json() == expected


def test_test_wrong_workflow(client, simple_system, arda_users, department_structure):
    """
    GET /workflow/<id>/test
    """

    frodo = arda_users.get('frodo')
    workflow = Workflow.objects.create(
        system=simple_system,
        user=frodo,
        workflow='upyachka',
        group_workflow='approvers = [approver("legolas")]',
        state='edit',
    )
    client.login('frodo')

    response = client.json.post(
        reverse('api_workflow_test', api_name='frontend', resource_name='workflow', id=workflow.id),
        {
            'requester': frodo.username,
            'path': '/superuser/',
            'user': 'gandalf'
        }
    )
    data = response.json()
    expected = {
        'is_valid': False,
        'result': '''Ошибка: NameError("name 'upyachka' is not defined")
Строка 1: upyachka
Переменные:
    False = False
    None = None
    True = True
    ad_groups = []
    email_cc = []
    fields_data = {}
    ignore_approvers = False
    no_email = False
    node = <NodeWrapper: Супер Пользователь>
    notify_everyone = False
    original_requester = None
    parent = None
    reason = 'online_test'
    ref_roles = []
    request_type = 'request'
    requester = <UserWrapper: frodo>
    review_days = None
    role = {'role': 'superuser'}
    scope = '/'
    send_sms = False
    system = <SystemWrapper: Simple система>
    system_specific = {}
    ttl_days = None
    user = <UserWrapper: gandalf>''',
    }
    assert data == expected


def test_test_bad_params(client, simple_system, users):
    """
    GET /workflow/<id>/test
    """
    terran, editor, _ = users
    workflow = Workflow.objects.create(
        system=simple_system,
        user=editor,
        workflow='approvers = [approver("terran")]',
        group_workflow='approvers = [approver("art")]',
        state='edit',
    )

    client.login('editor')

    response = client.json.post(
        reverse('api_workflow_test', api_name='frontend', resource_name='workflow', id=workflow.id),
        {}
    )

    assert response.status_code == 400
    assert response.json() == {
        'message': 'Invalid data sent',
        'error_code': 'BAD_REQUEST',
        'errors': {
            'path': ['Обязательное поле.'],
            'requester': ['Обязательное поле.'],
            'user': ['Обязательное поле.'],
        }
    }


def test_wrong_user(client, simple_system, arda_users):
    """
    GET /workflow/<id>/test
    """

    frodo = arda_users.get('frodo')
    workflow = Workflow.objects.create(system=simple_system, user=frodo, state='edit')
    client.login('frodo')
    wf_test_url = reverse('api_workflow_test', api_name='frontend', resource_name='workflow', id=workflow.id)
    response = client.json.post(wf_test_url, {
        'requester': frodo.username,
        'path': '/superuser/',
        'user': 'nonexistent',
    })
    assert response.status_code == 400
    expected = {
        'message': 'Invalid data sent',
        'error_code': 'BAD_REQUEST',
        'errors': {
            'user': ['Неизвестный пользователь.']
        }
    }
    assert response.json() == expected


def test_wrong_group(client, simple_system, arda_users):
    """
    GET /workflow/<id>/test
    """

    frodo = arda_users.get('frodo')
    workflow = Workflow.objects.create(system=simple_system, user=frodo, state='edit')

    client.login('frodo')
    group_id = 3141592653589793
    response = client.json.post(
        reverse('api_workflow_test', api_name='frontend', resource_name='workflow', id=workflow.id),
        {
            'requester': frodo.username,
            'path': '/superuser/',
            'group': group_id
        }
    )
    assert response.status_code == 400
    expected = {
        'error_code': 'BAD_REQUEST',
        'message': 'Invalid data sent',
        'errors': {
            'group': ['Группа с id=%s не найдена' % group_id]
        }
    }
    assert response.json() == expected


@pytest.mark.parametrize('silent', [True, False])
def test_commit_workflow(client, simple_system, users, silent, mailoutbox, idm_robot):
    """
    GET /workflow/<id>/commit
    """
    terran, editor, _ = users
    previous_workflow = simple_system.actual_workflow

    client.login('editor')

    initial = simple_system.workflows.get(parent=None)
    # Создать рабочую копию
    workflow = Workflow.objects.create(
        system=simple_system,
        user=editor,
        workflow='',
        state='edit',
        parent=initial,
    )

    # Черновик висит в develop
    wf_url = reverse('api_get_system_workflow', api_name='frontend', resource_name='systems', slug='simple')
    data = client.json.get(wf_url).json()

    assert data['develop_workflow'] == [{
        'id': workflow.id,
        'state': 'edit',
        'added': workflow.added.isoformat(),
        'updated': workflow.updated.isoformat(),
        'comment': '',
    }]
    assert data['committed_workflow'] == []

    # Отправить на подтверждение
    wf_commit_url = reverse('api_workflow_commit', api_name='frontend', resource_name='workflow', id=workflow.id)
    response = client.json.post(wf_commit_url, data={'silent': silent})

    assert response.status_code == 200
    data = response.json()
    assert data == {
        'id': workflow.id,
        'message': 'Workflow отправлен на согласование',
    }

    workflow = refresh(workflow)
    assert workflow.state == 'commited'

    # В ручке workflow меняется статус, запись пропадает из черновиков и появляется в запросах
    data = client.json.get(wf_url).json()

    expected_data = {
        'can_edit': True,
        'system_id': simple_system.id,
        'workflow': DEFAULT_WORKFLOW,
        'group_workflow': DEFAULT_WORKFLOW,
        'updated': simple_system.get_workflow_changed_date().isoformat(),
        'develop_workflow': [],
        'committed_workflow': [{
            'id': workflow.id,
            'added': workflow.added.isoformat(),
            'updated': workflow.updated.isoformat(),
            'comment': '',
            'author': {'login': 'editor'}
        }],
        'history': [{
            'id': simple_system.actual_workflow.pk,
            'approver': {'login': idm_robot.username},
            'author': {'login': idm_robot.username},
            'diff': '@@ -0,0 +1 @@\n+approvers = []',
            'group_diff': '@@ -0,0 +1 @@\n+approvers = []',
            'updated': previous_workflow.approved.isoformat(),
            'workflow': DEFAULT_WORKFLOW,
            'group_workflow': DEFAULT_WORKFLOW
        }, {
            'approver': {'login': idm_robot.username},
            'author': {'login': idm_robot.username},
            'diff': '',
            'group_diff': '',
            'id': initial.pk,
            'updated': initial.approved.isoformat(),
            'workflow': '',
            'group_workflow': '',
        }],
    }
    assert data == expected_data

    if silent:
        assert len(mailoutbox) == 0
    else:
        assert len(mailoutbox) > 0


def test_commit_same_workflow(client, simple_system, users):
    """
    POST /workflow/<id>/commit
    """
    terran, editor, _ = users

    client.login('editor')

    workflow = Workflow.objects.create(
        system=simple_system,
        user=editor,
        workflow=DEFAULT_WORKFLOW,
        group_workflow=DEFAULT_WORKFLOW,
        state='edit',
    )

    # Отправить на подтверждение
    wf_url = reverse('api_workflow_commit', api_name='frontend', resource_name='workflow', id=workflow.id)
    response = client.json.post(wf_url)

    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Код workflow не отличается от текущего.'
    }
    assert refresh(workflow).state == 'edit'


def test_approve_possibility(client, simple_system, users):
    """
    GET /workflow/<id>
    """
    terran, editor, approvers = users
    super_, security, responsible_our, responsible_other = approvers

    workflow = Workflow.objects.create(
        system=simple_system,
        user=editor,
        workflow='',
        state='edit',
    )

    # В статусе "редактирование" не может подтвердить никто
    client.login('editor')
    data = client.json.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='workflow', id=workflow.id),
    ).json()

    assert data['can_approve'] is False

    for approver in approvers:
        client.login(approver.username)
        response = client.json.get(
            reverse('api_dispatch_detail', api_name='frontend', resource_name='workflow', id=workflow.id),
        )

        assert response.status_code == 403

    # Может подтвердить только аппрувер
    workflow.state = 'commited'
    workflow.save()

    client.login('editor')
    data = client.json.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='workflow', id=workflow.id),
    ).json()

    assert data['can_approve'] is False

    approver_can_approve = [
        (super_, True),
        (security, False),
        (responsible_our, True),
        (responsible_other, False)
    ]
    for approver, can_approve in approver_can_approve:
        client.login(approver.username)
        wf_url = reverse('api_dispatch_detail', api_name='frontend', resource_name='workflow', id=workflow.id)
        response = client.json.get(wf_url)
        data = response.json()
        if can_approve:
            assert response.status_code == 200
            assert data['can_approve'] is True
        else:
            assert response.status_code == 403
            assert response.json() == {
                'error_code': 'FORBIDDEN',
                'message': 'You have no permission to view this workflow'
            }


@pytest.mark.parametrize('silent', [True, False])
def test_approve_workflow(client, simple_system, users, silent, mailoutbox):
    """
    POST /workflow/<id>/approve
    """
    terran, editor, approvers = users

    for approver in approvers:
        workflow = Workflow.objects.create(
            system=simple_system,
            user=editor,
            workflow=approver.username,
            state='commited',
        )

        # Самому нельзя подтвердить свои правки
        client.login('editor')
        wf_url = reverse('api_workflow_approve', api_name='frontend', resource_name='workflow', id=workflow.id)
        response = client.json.post(wf_url)
        assert response.status_code == 403
        assert response.json() == {
            'error_code': 'FORBIDDEN',
            'message': 'У вас нет прав на принятие этого запроса на изменение workflow.'
        }

        # Аппрувер может подтвердить
        can_approve = approver.username not in ('approver_other', 'approver_security')
        client.login(approver.username)

        approve_url = reverse('api_workflow_approve', api_name='frontend', resource_name='workflow', id=workflow.id)
        if can_approve:
            response = client.json.post(approve_url, data={'silent': silent})
            data = response.json()
            assert data == {
                'id': workflow.id,
                'message': 'Изменение worklow подтверждено и сохранено в системе.',
            }
            workflow = refresh(workflow)
            assert workflow.state == 'approved'

            simple_system = refresh(simple_system)
            simple_system.fetch_actual_workflow()
            assert simple_system.get_user_workflow_code() == approver.username

            if silent:
                assert len(mailoutbox) == 0
            else:
                assert len(mailoutbox) > 0

            set_workflow(simple_system, DEFAULT_WORKFLOW)
        else:
            client.post(approve_url)

            workflow = refresh(workflow)
            assert workflow.state == 'commited'

            simple_system = refresh(simple_system)
            simple_system.fetch_actual_workflow()
            assert simple_system.get_user_workflow_code() == DEFAULT_WORKFLOW


def test_approve_same_workflow(client, simple_system, users):
    """
    POST /workflow/<id>/approve
    """
    terran, editor, approvers = users
    approver_superuser = approvers[0]

    client.login('approver_superuser')

    workflow = Workflow.objects.create(
        system=simple_system,
        user=approver_superuser,
        workflow=DEFAULT_WORKFLOW,
        group_workflow=DEFAULT_WORKFLOW,
        state='edit',
    )

    # Отправить на подтверждение
    wf_url = reverse('api_workflow_approve', api_name='frontend', resource_name='workflow', id=workflow.id)
    response = client.json.post(wf_url)

    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Код workflow не отличается от текущего.'
    }
    assert refresh(workflow).state == 'edit'


def test_approve_bad_workflow(client, simple_system, users):
    """
    GET /workflow/<id>/approve
    """
    terran, editor, approvers = users
    admin = approvers[0]

    workflow = Workflow.objects.create(
        system=simple_system,
        user=admin,
        workflow=':)',
        state='edit',
    )

    client.login(admin.username)
    commit_wf_url = reverse('api_workflow_commit', api_name='frontend', resource_name='workflow', id=workflow.id)
    response = client.json.post(commit_wf_url)
    assert response.status_code == 400
    assert response.json()['message'].startswith('Невозможно сохранить workflow с ошибками:')

    approve_wf_url = reverse('api_workflow_approve', api_name='frontend', resource_name='workflow', id=workflow.id)
    response = client.json.post(approve_wf_url)
    assert response.status_code == 400
    assert response.json()['message'].startswith('Невозможно сохранить workflow с ошибками:')


def test_decline_workflow(client, simple_system, users):
    """
    GET /workflow/<id>/decline
    """
    terran, editor, approvers = users

    # Аппрувер может откатить
    for approver in approvers:
        workflow = Workflow.objects.create(
            system=simple_system,
            user=editor,
            workflow='',
            state='commited',
        )

        # Самому нельзя откатить свои правки
        client.login('editor')
        approve_url = reverse('api_workflow_approve', api_name='frontend', resource_name='workflow', id=workflow.id)
        response = client.json.post(approve_url)
        assert response.json() == {
            'error_code': 'FORBIDDEN',
            'message': 'У вас нет прав на принятие этого запроса на изменение workflow.'
        }

        can_approve = approver.username not in ('approver_other', 'approver_security')
        client.login(approver.username)
        decline_url = reverse('api_workflow_decline', api_name='frontend', resource_name='workflow', id=workflow.id)

        if can_approve:
            response = client.json.post(decline_url)
            data = response.json()
            assert data == {
                'id': workflow.id,
                'message': 'Изменение worklow отменено.',
            }

            workflow = refresh(workflow)
            assert workflow.state == 'edit'

            system = refresh(simple_system)
            system.fetch_actual_workflow()
            assert system.get_user_workflow_code() == DEFAULT_WORKFLOW
        else:
            client.post(decline_url)

            workflow = Workflow.objects.get(id=workflow.id)
            assert workflow.state == 'commited'
            system = refresh(simple_system)
            system.fetch_actual_workflow()

        workflow.delete()


def test_delete_workflow(client, simple_system, users):
    """
    GET /workflow/<id>/delete
    """
    terran, editor, _ = users

    workflow = Workflow.objects.create(
        system=simple_system,
        user=editor,
        workflow='',
        state='edit',
    )
    assert simple_system.workflows.count() == 3

    # Только автор может удалить свои правки
    client.login('approver_superuser')
    wf_deletion_url = reverse('api_workflow_delete', api_name='frontend', resource_name='workflow', id=workflow.id)
    response = client.json.post(wf_deletion_url)
    assert response.status_code == 404

    client.login('editor')
    response = client.json.post(wf_deletion_url)

    assert response.status_code == 200
    data = response.json()
    assert data == {
        'id': None,
        'message': 'Workflow удален',
    }

    assert simple_system.workflows.count() == 2
    assert System.objects.filter(id=workflow.system_id).exists() is True

    # Workflow на согласовании
    workflow = Workflow.objects.create(
        system=simple_system,
        user=editor,
        workflow='',
        state='commited',
    )

    # Удалить
    wf_deletion_url = reverse('api_workflow_delete', api_name='frontend', resource_name='workflow', id=workflow.id)
    response = client.json.post(wf_deletion_url)
    assert response.status_code == 400

    data = response.json()
    assert data['message'] == 'Вы можете удалять workflow только из режима редактирования.'
    assert Workflow.objects.filter(system=simple_system).count() == 3


def test_delete_parent_workflow(client, simple_system, arda_users):
    """
    Проверяем, что удаление parent workflow черновика не позволено
    """

    frodo = arda_users.frodo

    # Очищаем workflow, созданные фикстурой
    for workflow in simple_system.workflows.order_by('-pk'):
        workflow.delete()

    workflow = Workflow.objects.create(
        system=simple_system,
        user=frodo,
        workflow='',
        state='commited',
    )
    workflow_next = Workflow.objects.create(
        system=simple_system,
        user=frodo,
        workflow='',
        parent=workflow,
        state='edit',
    )
    workflow_next_one = Workflow.objects.create(
        system=simple_system,
        user=frodo,
        workflow='',
        parent=workflow_next,
        state='edit',
    )

    client.login('frodo')
    wf_deletion_url = reverse('api_workflow_delete', api_name='frontend', resource_name='workflow', id=workflow_next.id)
    response = client.json.post(wf_deletion_url)
    assert response.status_code == 500


def test_workflow_history(client, simple_system, users, idm_robot):
    """
    GET /systems/simple/workflow
    """
    terran, editor, _ = users

    previous_workflow = simple_system.actual_workflow
    workflow = Workflow.objects.create(
        system=simple_system,
        user=editor,
        workflow='approvers = ["terran"]',
        state='commited',
    )

    workflow2 = Workflow.objects.create(
        system=simple_system,
        user=editor,
        workflow='approvers = ["art"]',
        state='commited',
    )

    client.login('approver_superuser')
    client.json.post(reverse('api_workflow_approve', api_name='frontend', resource_name='workflow', id=workflow.id))
    client.json.post(reverse('api_workflow_approve', api_name='frontend', resource_name='workflow', id=workflow2.id))

    wf_url = reverse('api_get_system_workflow', api_name='frontend', resource_name='systems', slug='simple')
    response = client.json.get(wf_url)

    workflow = refresh(workflow)
    workflow2 = refresh(workflow2)

    expected_data = {
        'can_edit': True,
        'develop_workflow': [],
        'committed_workflow': [],
        'system_id': simple_system.pk,
        'workflow': 'approvers = ["art"]',
        'group_workflow': '',
        'updated': workflow2.approved.isoformat(),
    }
    data = response.json()
    history = data.pop('history')
    assert data == expected_data
    previous_workflow.fetch_parent()
    expected_history = [
        {
            'id': workflow2.pk,
            'updated': workflow2.approved.isoformat(),
            'author': {'login': 'editor'},
            'approver': {'login': 'approver_superuser'},
            'workflow': 'approvers = ["art"]',
            'diff': '@@ -1 +1 @@\n-approvers = ["terran"]\n+approvers = ["art"]',
            'group_workflow': '',
            'group_diff': ''
        }, {
            'id': workflow.pk,
            'updated': workflow.approved.isoformat(),
            'author': {'login': 'editor'},
            'approver': {'login': 'approver_superuser'},
            'workflow': 'approvers = ["terran"]',
            'diff': '@@ -1 +1 @@\n-approvers = []\n+approvers = ["terran"]',
            'group_workflow': '',
            'group_diff': '@@ -1 +0,0 @@\n-approvers = []'
        }, {
            'id': previous_workflow.pk,
            'updated': previous_workflow.approved.isoformat(),
            'author': {'login': idm_robot.username},
            'approver': {'login': idm_robot.username},
            'workflow': DEFAULT_WORKFLOW,
            'diff': '@@ -0,0 +1 @@\n+approvers = []',
            'group_workflow': DEFAULT_WORKFLOW,
            'group_diff': '@@ -0,0 +1 @@\n+approvers = []',
        }, {
            'id': previous_workflow.parent.pk,
            'updated': previous_workflow.parent.approved.isoformat(),
            'author': {'login': idm_robot.username},
            'approver': {'login': idm_robot.username},
            'workflow': '',
            'diff': '',
            'group_workflow': '',
            'group_diff': '',
        }
    ]
    assert history == expected_history


def test_workflow_policy(client, simple_system, users):
    terran, editor, approvers = users
    approver_superuser, approver_security, approver_responsible, approver_other = approvers
    wf_url = reverse('api_get_system_workflow', api_name='frontend', resource_name='systems', slug='simple')

    workflow = Workflow.objects.create(
        system=simple_system,
        user=approver_responsible,
        workflow='',
        comment='Hi',
        state='commited',
    )

    client.login('approver_responsible')
    data = client.json.get(wf_url).json()
    assert len(data['committed_workflow']) == 1

    simple_system.workflow_approve_policy = 'another'
    simple_system.save()

    data = client.json.get(wf_url).json()
    # свои запросы видны даже при включенной sox-политике
    assert len(data['committed_workflow']) == 1

    response = client.json.post(reverse(
        'api_workflow_approve', api_name='frontend', resource_name='workflow', id=workflow.id
    ))
    assert response.status_code == 403
    assert response.json()['message'] == 'В этой системе запрещено подтверждать свои изменения.'

    approver_responsible2 = create_user('approver_responsible2')
    add_perms_by_role('responsible', approver_responsible2, simple_system)
    client.login('approver_responsible2')
    response = client.json.post(reverse(
        'api_workflow_approve', api_name='frontend', resource_name='workflow', id=workflow.id
    ))
    assert response.status_code == 200


def test_workflow_policy_for_superuser(client, simple_system, users):
    terran, editor, approvers = users
    approver_superuser, approver_security, approver_responsible, approver_other = approvers

    simple_system.workflow_approve_policy = 'another'
    simple_system.save()

    workflow = Workflow.objects.create(
        system=simple_system,
        user=approver_superuser,
        workflow='',
        comment='Hi',
        state='commited',
    )

    client.login('approver_superuser')
    response = client.json.post(reverse(
        'api_workflow_approve', api_name='frontend', resource_name='workflow', id=workflow.id
    ))
    assert response.status_code == 200


def test_workflow_policy_notification(client, simple_system, users):
    terran, editor, approvers = users
    approver = approvers[0]

    simple_system.workflow_approve_policy = 'another'
    simple_system.save()

    workflow = Workflow.objects.create(
        system=simple_system,
        user=approver,
        workflow='',
        comment='Hi',
        state='edit',
    )
    workflow.commit(approver)

    assert len(mail.outbox) == 3
    assert get_recievers(mail.outbox) == {
        'approver_responsible@example.yandex.ru',
        'idm-notification@yandex-team.ru',
        'security-alerts@yandex-team.ru',
    }


def test_read_workflow_through_set(client, simple_system, arda_users):
    """Проверим, что нельзя обойти проверку прав через сабсет. IDM-4903"""

    client.login('frodo')
    workflow_pk = simple_system.actual_workflow.pk
    # урлы захардкожены, потому что, возможно, мы совсем удалим /set/ узлы
    response = client.json.get('/api/frontend/workflow/%s/' % workflow_pk)
    assert response.status_code == 403
    response = client.json.get('/api/frontend/workflow/set/%s/' % workflow_pk)
    assert response.status_code > 400


def _set_deprive_workflow(system, code):
    system.use_workflow_for_deprive = True
    system.save(update_fields=('use_workflow_for_deprive',))
    set_workflow(system, code)


@pytest.mark.django_db
def test_workflow_for_deprive_api_should_always_be_able_to_deprive(
        client, simple_system, arda_users, arda_users_with_roles):
    """Кнопку отозвать хотим показывать всем, уже после нажатия может быть сообщено что прав нет"""
    user = arda_users.frodo
    role = arda_users_with_roles.frodo[0]
    _set_deprive_workflow(simple_system, dedent("""
        raise_syntax_error()
    """))
    client.login(user)
    deprive_url = get_role_url(role.pk)
    response = client.get(deprive_url)
    assert response.status_code == 200
    assert response.json()['permissions']['can_be_deprived']


@pytest.mark.django_db
def test_workflow_for_deprive_api_error(client, simple_system, arda_users, arda_users_with_roles):
    user = arda_users.frodo
    role = arda_users_with_roles.frodo[0]
    _set_deprive_workflow(simple_system, dedent("""
        raise_syntax_error()
    """))
    client.login(user)
    deprive_url = get_role_url(role.pk)
    response = client.delete(deprive_url)
    assert response.status_code == 409


@pytest.mark.django_db
def test_workflow_for_deprive_api_forbidden(client, simple_system, arda_users, arda_users_with_roles):
    user = arda_users.legolas
    role = arda_users_with_roles.legolas[0]
    _set_deprive_workflow(simple_system, dedent("""
        approvers = [any_from(['gandalf', 'frodo']), 'bilbo']
    """))
    client.login(user)
    deprive_url = get_role_url(role.pk)
    response = client.delete(deprive_url)
    assert response.status_code == 403
    assert response.json() == {
        'error_code': 'FORBIDDEN',
        'message': 'Недостаточно прав для отзыва роли',
        'data': {'approvers': [
            {'full_name': 'gandalf', 'is_active': True, 'username': 'gandalf'},
            {'full_name': 'Фродо Бэггинс', 'is_active': True, 'username': 'frodo'},
            {'full_name': 'bilbo', 'is_active': True, 'username': 'bilbo'}]}}


@pytest.mark.django_db
def test_workflow_for_deprive_api_ok(client, simple_system, arda_users, arda_users_with_roles):
    user = arda_users.frodo
    role = arda_users_with_roles.frodo[0]
    _set_deprive_workflow(simple_system, dedent("""
        approvers = ['frodo']
    """))
    client.login(user)
    deprive_url = get_role_url(role.pk)
    response = client.delete(deprive_url)
    assert response.status_code == 204
    role.refresh_from_db()
    assert role.state == ROLE_STATE.DEPRIVED
