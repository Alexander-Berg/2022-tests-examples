# coding: utf-8


from textwrap import dedent
from unittest import mock

import pytest
from django.core import mail
from mock import patch

from idm.core.constants.action import ACTION
from idm.core.models import Role, RoleNode, ApproveRequest, Action
from idm.core.plugins.errors import PluginFatalError, PluginError
from idm.tests.utils import refresh, set_workflow, json, assert_contains, create_system, create_user, raw_make_role, \
    role_actions
from idm.users.models import Organization

pytestmark = pytest.mark.django_db


def test_simple_refs(arda_users, simple_system_w_refs, idm_robot):
    """Связанные роли выдаются, если аппруверы не заданы"""

    frodo = arda_users.get('frodo')
    organization = Organization.objects.create(org_id=100000)

    role = Role.objects.request_role(
        requester=frodo,
        subject=frodo,
        system=simple_system_w_refs,
        comment='',
        data={'role': 'admin'},
        fields_data=None,
        organization_id=organization.id,
    )
    assert frodo.roles.count() == 2
    assert frodo.roles.filter(state='granted').count() == 2
    assert role.organization_id == organization.id

    ref_role = Role.objects.select_related('node').exclude(pk=role.pk).get()
    assert ref_role.node.data == {'role': 'manager'}
    assert ref_role.parent_id == role.id
    assert ref_role.system_id == simple_system_w_refs.id
    assert ref_role.fields_data == {'login': '/'}
    assert ref_role.actions.count() == 5
    assert ref_role.organization_id == organization.id

    assert role_actions(ref_role) == ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant']

    request_action = ref_role.actions.select_related('requester').order_by('pk').first()
    assert request_action.requester == idm_robot
    assert request_action.comment == 'Reference role request'

    assert len(mail.outbox) == 2
    message1, message2 = mail.outbox
    assert message1.to == ['frodo@example.yandex.ru']
    assert message2.to == ['frodo@example.yandex.ru']
    assert message1.subject == 'Simple система. Новая роль'
    assert message2.subject == 'Simple система. Новая роль'
    assert_contains([
        'Вы получили новую роль в системе "Simple система":',
        'Роль: Менеджер',
        'поскольку у вас есть роль в системе "Simple система":',
        'Роль: Админ',
        'Роль была запрошена пользователем Фродо Бэггинс (frodo)'
    ], message1.body)
    assert_contains([
        'Вы получили новую роль в системе "Simple система":',
        'Роль: Админ',
        'Роль была запрошена пользователем Фродо Бэггинс (frodo)',
    ], message2.body)


def test_group_refs(arda_users, simple_system_w_refs, department_structure):
    """Связанная роль на группу"""
    frodo = arda_users.get('frodo')
    group = department_structure.fellowship

    role = Role.objects.request_role(frodo, group, simple_system_w_refs, '', {'role': 'admin'}, None)
    assert group.roles.filter(state='granted').count() == 2

    ref_role = Role.objects.exclude(pk=role.pk).select_related('node').get(group=group)
    assert ref_role.node.data == {'role': 'manager'}
    assert ref_role.parent_id == role.id

    assert Role.objects.filter(state='granted').count() == (group.members.count() + 1) * 2


def test_ref_role_can_be_requested_separately(arda_users, simple_system_w_refs):
    """Связанную роль можно запросить и не через связь"""

    frodo = arda_users.get('frodo')
    Role.objects.request_role(frodo, frodo, simple_system_w_refs, '', {'role': 'manager'}, None)
    assert frodo.roles.count() == 1
    assert frodo.roles.filter(state='granted').count() == 1


def test_ref_role_is_approved_when_parent_role_is_granted(arda_users, simple_system):
    """Связанные роли выдаются только тогда, когда родительская роль подтверждена"""

    frodo = arda_users.get('frodo')
    legolas = arda_users.get('legolas')
    set_workflow(simple_system, dedent('''
    approvers = []
    if role.get('role') == 'admin':
        ref_roles = [{
            'system': '%s',
            'role_data': {
                'role': 'manager'
            }
        }]
        approvers = ['legolas']
''' % simple_system.slug))
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == 1
    approve_request = ApproveRequest.objects.select_related_for_set_decided().get()
    approve_request.set_approved(legolas)
    assert Role.objects.count() == 2
    assert Role.objects.filter(state='granted').count() == 2


def test_ref_role_can_be_approvable(arda_users, simple_system):
    """Связанная роль может требовать подтверждения"""

    frodo = arda_users.get('frodo')
    set_workflow(simple_system, dedent('''
    if role.get('role') == 'admin':
        ref_roles = [{
            'system': '%s',
            'role_data': {
                'role': 'manager'
            }
        }]
        approvers = []
    else:
        approvers = ['legolas']
''') % simple_system.slug)
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == 2
    role = refresh(role)
    ref = role.refs.get()
    assert ref.state == 'requested'


def test_ref_role_can_be_autoapproved(arda_users, simple_system):
    """Связанная роль может быть автоматически подтверждена на основании того, что она связанная"""

    frodo = arda_users.get('frodo')
    legolas = arda_users.get('legolas')
    set_workflow(simple_system, dedent('''
    if role.get('role') == 'admin':
        ref_roles = [{
            'system': '%s',
            'role_data': {
                'role': 'manager'
            }
        }]
    if parent is not None:
        approvers = []
    else:
        approvers = ['legolas']

''') % simple_system.slug)
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert role.state == 'requested'
    approve_request = ApproveRequest.objects.select_related_for_set_decided().get()
    approve_request.set_approved(legolas)
    role = refresh(role)
    assert role.state == 'granted'
    assert role.refs.get().state == 'granted'


def test_ref_role_may_fail_to_grant(arda_users, simple_system, generic_system):
    """Связанная роль может и не выдаться, и перейти из approved в failed"""

    frodo = arda_users.get('frodo')
    set_workflow(simple_system, dedent('''
    approvers = []
    if role.get('role') == 'admin':
        ref_roles = [{
            'system': '%s',
            'role_data': {
                'role': 'manager'
            }
        }]
''') % generic_system.slug)
    set_workflow(generic_system, 'approvers = []')
    with patch.object(generic_system.plugin.__class__, 'add_role') as add_role:
        add_role.side_effect = PluginFatalError(1, 'weird error', {'hello': 'world'})
        role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert Role.objects.count() == 2
    role = refresh(role)
    assert role.state == 'granted'
    ref = role.refs.get()
    assert ref.state == 'failed'


def test_cross_system_refs(arda_users, simple_system, generic_system):
    """Связанных ролей может быть несколько, они могут быть выданы в разных системах"""

    frodo = arda_users.get('frodo')
    set_workflow(simple_system, '''
approvers=[]
if role.get('role') == 'admin':
    ref_roles = [{
        'system': '%(simple)s',
        'role_data': {
            'role': 'manager'
        }
    }, {
        'system': '%(generic)s',
        'role_data': {
            'role': 'poweruser'
        },
        'role_fields': {
            'login': 'blogin',
        }
    }]''' % {
        'simple': simple_system.slug,
        'generic': generic_system.slug,
    })
    set_workflow(generic_system)
    with patch.object(generic_system.plugin.__class__, 'add_role') as add_role:
        add_role.return_value = {
            'code': 0,
            'data': {
                'baz': 'foobar'
            }
        }
        role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert frodo.roles.count() == 3
    assert frodo.roles.filter(state='granted').count() == 3
    role = refresh(role)
    assert role.refs.count() == 2
    generic_role = frodo.roles.select_related('node').get(system=generic_system)
    assert generic_role.node.data == {'role': 'poweruser'}
    assert generic_role.fields_data == {'login': 'blogin'}
    assert generic_role.system_specific == {'baz': 'foobar'}


def test_ref_role_is_not_requested_if_system_has_bad_workflow(arda_users, simple_system, generic_system):
    """Связанные роли могут располагаться и в системах со сломанным workflow,
    в этом случае основная роль будет запрошена, а связанные - нет"""

    frodo = arda_users.get('frodo')
    set_workflow(simple_system, dedent('''
    approvers=[]
    if role.get('role') == 'admin':
        ref_roles = [{
            'system': '%s',
            'role_data': {
                'role': 'manager'
            }
        }]''') % generic_system.slug)
    set_workflow(generic_system, 'explicit syntax error', bypass_checks=True)
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == 1
    role = refresh(role)
    assert role.state == 'granted'


def test_ref_role_is_not_requested_if_workflow_exception(arda_users, simple_system, generic_system):
    """При запросе связанной роли может случиться ошибка в workflow,
    в этом случае основная роль будет запрошена, а связанная - нет"""

    frodo = arda_users.get('frodo')
    set_workflow(simple_system, dedent('''
    approvers=[]
    if role.get('role') == 'admin':
        ref_roles = [{
            'system': '%s',
            'role_data': {
                'role': 'manager'
            }
        }]''') % generic_system.slug)
    set_workflow(generic_system, 'raise AccessDenied("Thou shalt not pass")')
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == 1
    role = refresh(role)
    assert role.state == 'granted'


def test_ref_role_is_not_requested_if_nonexistent_fields(arda_users, simple_system):
    """В случае если в role_fields в связанной роли указаны невалидные поля,
    основная роль должна выдаться, а связанная - нет"""

    frodo = arda_users.get('frodo')
    set_workflow(simple_system, dedent('''
    approvers=[]
    if role.get('role') == 'admin':
        ref_roles = [{
            'system': '%s',
            'role_data': {
                'role': 'manager'
            },
            'role_fields': {
                'scope': '/qwerty/'
            }
        }]''') % simple_system.slug)
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == 1
    role = refresh(role)
    assert role.state == 'granted'

    assert len(mail.outbox) == 2
    # Сообщение про выданную роль не проверяем, но проверяем сообщение про невалидную ref роль
    expected = (
        "В системе Simple система была попытка выдать на роль Админ невалидную связанную роль "
        "{\'system\': \'simple\', \'role_data\': {\'role\': \'manager\'}, \'role_fields\': {\'scope\': \'/qwerty/\'}}.\n"
        'Ошибка: "Указаны несуществующие fields"'
    )
    assert expected in mail.outbox[0].body


def test_ref_role_is_requested_even_if_another_could_not(arda_users, simple_system, generic_system):
    """Если при запросе одной связанной роли возникло исключение,
    то другие связанные роли всё равно должны запрашиваться"""

    frodo = arda_users.get('frodo')
    set_workflow(generic_system, 'explicit syntax error', bypass_checks=True)
    set_workflow(simple_system, dedent('''
    approvers=[]
    if role.get('role') == 'admin':
        ref_roles = [{
            'system': '%(generic)s',
            'role_data': {
                'role': 'manager'
            }
        }, {
            'system': '%(simple)s',
            'role_data': {
                'role': 'manager'
            }
        }]''') % {
        'generic': generic_system.slug,
        'simple': simple_system.slug
    })
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == 2
    role = refresh(role)
    assert role.state == 'granted'
    assert role.refs.get().state == 'granted'


def test_post_data_about_same_role_to_system_just_once(generic_system, arda_users):
    """Если у пользователя уже есть роль в системе, а мы выдаём ему ещё одну автоматически – то информацию
    в систему мы должны отправлять только один раз"""

    def forbid_manager(self, method, data, **_):
        data = json.loads(data['role'])
        if data['role'] == 'manager':
            raise PluginError(1, 'Cannot add second role', {})
        return {
            'data': {
                'token': 'another'
            }
        }

    frodo = arda_users.frodo
    set_workflow(generic_system, dedent('''
    approvers=[]
    if role.get('role') == 'admin':
        ref_roles = [{
            'system': '%(generic)s',
            'role_data': {
                'role': 'manager'
            }
        }]''') % {
        'generic': generic_system.slug,
    })
    with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
        post_data.return_value = {
            'data': {
                'token': 'whoa!'
            }
        }
        Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'}, None)
    with patch.object(generic_system.plugin.__class__, '_post_data', forbid_manager):
        Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'admin'}, None)
    frodo_roles = Role.objects.filter(user=frodo)
    assert frodo_roles.count() == 3
    assert frodo_roles.filter(state='granted').count() == 3
    role1, role2 = frodo_roles.filter(node__data={'role': 'manager'})
    assert role1.system_specific == role2.system_specific
    assert role1.system_specific == {'token': 'whoa!'}
    role3 = frodo_roles.exclude(pk__in=[role1.pk, role2.pk]).get()
    assert role3.system_specific == {'token': 'another'}


def test_hidden_ref(arda_users, simple_system_w_refs):
    """Скрытая связанная роль"""
    workflow = dedent("""
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%s',
                'role_data': {
                    'role': 'manager'
                },
                'visibility': False,
            }]
        """ % simple_system_w_refs.slug)
    set_workflow(simple_system_w_refs, workflow, workflow)

    frodo = arda_users.get('frodo')

    role = Role.objects.request_role(frodo, frodo, simple_system_w_refs, '', {'role': 'admin'}, None)
    assert frodo.roles.count() == 2
    assert frodo.roles.filter(state='granted').count() == 2

    ref_role = Role.objects.exclude(pk=role.pk).select_related('node').get()
    assert ref_role.node.data == {'role': 'manager'}
    assert ref_role.is_public is False

    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert_contains(['Роль: Админ'], message.body)


def test_public_ref_of_hidden_node(arda_users, simple_system_w_refs):
    """Скрытая связанная роль"""
    workflow = dedent("""
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%s',
                'role_data': {
                    'role': 'manager'
                },
                'visibility': True,
            }]
        """ % simple_system_w_refs.slug)
    set_workflow(simple_system_w_refs, workflow, workflow)
    RoleNode.objects.filter(value_path='/manager/').update(is_public=False)

    frodo = arda_users.get('frodo')

    Role.objects.request_role(frodo, frodo, simple_system_w_refs, '', {'role': 'admin'}, None)

    assert frodo.roles.count() == 1
    assert frodo.roles.select_related('node').get().node.value_path == '/admin/'

    assert len(mail.outbox) == 1


def test_ref_role_request_failed():
    ref_system = create_system()
    ref_node = ref_system.nodes.last()
    ref_role_definition = {'system': ref_system.slug, 'role_data': ref_node.data}
    system = create_system()
    user = create_user()
    parent_role = raw_make_role(
        subject=user,
        system=system,
        data=system.nodes.last().data,
        ref_roles=[ref_role_definition]
    )

    class RefRoleError(Exception):
        pass

    with mock.patch('idm.core.querysets.role.RoleManager.request_role', side_effect=RefRoleError):
        parent_role.request_ref_roles()

    assert not parent_role.refs.exists()
    ref_error_action: Action = parent_role.actions.first()
    assert ref_error_action.action == ACTION.REF_ROLE_ERROR
    assert ref_error_action.comment == 'Не удалось запросить связанные роли'
