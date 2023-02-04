# -*- coding: utf-8 -*


from textwrap import dedent

import pytest
import waffle.testutils
from django.core import mail
from django.utils import six

from idm.core.conflicts import find_conflicts
from idm.core.constants.action import ACTION
from idm.core.constants.system import SYSTEM_INCONSISTENCY_POLICY
from idm.core.workflow.exceptions import ConflictValidationError, WorkflowError
from idm.core.models import Role, RoleField, RoleNode, Action
from idm.core.workflow.common.subject import subjectify
from idm.framework.requester import requesterify
from idm.inconsistencies.models import Inconsistency
from idm.tests.templates.utils import user_mention
from idm.tests.utils import assert_contains, set_workflow, clear_mailbox, raw_make_role, make_inconsistency

# разрешаем использование базы в тестах
pytestmark = [pytest.mark.django_db]


def test_conflict(simple_system, arda_users):
    frodo = arda_users.frodo

    wf = dedent("""
        conflicts = (
            ('admin', 'manager', 'security@example.yandex.ru'),
            ('admin', 'po', 'another-security@example.yandex.ru'),  # подстрока, но не полный slug
        )
        approvers = []""")
    set_workflow(simple_system, wf)

    manager_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    poweruser_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'poweruser'}, None)

    assert frodo.roles.count() == 2
    assert len(mail.outbox) == 2
    for message in mail.outbox:
        assert message.subject == 'Simple система. Новая роль'
        assert message.to == ['frodo@example.yandex.ru']

    clear_mailbox()

    admin_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    # пришло два письма. то, что про конфликт – про точное совпадение
    assert len(mail.outbox) == 2
    conflict_message, new_role_message = mail.outbox
    assert conflict_message.subject == 'Обнаружены конфликты при запросе роли для %s' % user_mention(frodo)
    assert conflict_message.to == ['security@example.yandex.ru']

    assert_contains((
        'В системе "Simple система" обнаружен конфликт ролей.',
        'Запрошена роль "Роль: Админ" (https://example.com/system/simple/#role=%s) для %s' %
        (admin_role.id, user_mention(frodo)),
        'Конфликтует с ролями:\nРоль: Менеджер (https://example.com/system/simple/#role=%s)' % manager_role.id
    ), conflict_message.body)

    assert new_role_message.subject == 'Simple система. Новая роль'
    assert new_role_message.to == ['frodo@example.yandex.ru']


@waffle.testutils.override_switch('find_conflicts_in_workflow', active=True)
def test_conflict_conflics_format_validation(simple_system, arda_users):
    frodo = arda_users.frodo

    set_workflow(simple_system, dedent("""
        conflicts = {"a": [("a", "b"), ("c", "d")]}
        approvers = []
    """))
    with pytest.raises(WorkflowError):
        Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)

    set_workflow(simple_system, dedent("""
        conflicts = (
            ('admin', 'manager', 'security@example.yandex.ru'),
            ('admin', 'po', 'another-security@example.yandex.ru'))
        approvers = []
    """))
    with pytest.raises(WorkflowError):
        Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)


@waffle.testutils.override_switch('find_conflicts_in_workflow', active=True)
def test_conflict_calculated_in_wf(simple_system, arda_users):
    frodo = arda_users.frodo

    wf = dedent("""
        matrix = (
            ('admin', 'manager', 'security@example.yandex.ru'),
            ('admin', 'po', 'another-security@example.yandex.ru'),  # подстрока, но не полный slug
        )
        conflicts = find_conflicts(node, user, matrix)
        approvers = []""")
    set_workflow(simple_system, wf)

    manager_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    poweruser_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'poweruser'}, None)

    assert frodo.roles.count() == 2
    assert len(mail.outbox) == 2
    for message in mail.outbox:
        assert message.subject == 'Simple система. Новая роль'
        assert message.to == ['frodo@example.yandex.ru']

    clear_mailbox()

    admin_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    # пришло два письма. то, что про конфликт – про точное совпадение
    assert len(mail.outbox) == 2
    conflict_message, new_role_message = mail.outbox
    assert conflict_message.subject == 'Обнаружены конфликты при запросе роли для %s' % user_mention(frodo)
    assert conflict_message.to == ['security@example.yandex.ru']

    assert_contains((
        'В системе "Simple система" обнаружен конфликт ролей.',
        'Запрошена роль "Роль: Админ" (https://example.com/system/simple/#role=%s) для %s' %
        (admin_role.id, user_mention(frodo)),
        'Конфликтует с ролями:\nРоль: Менеджер (https://example.com/system/simple/#role=%s)' % manager_role.id
    ), conflict_message.body)

    assert new_role_message.subject == 'Simple система. Новая роль'
    assert new_role_message.to == ['frodo@example.yandex.ru']


def test_conflict_for_rerequest(simple_system, arda_users):
    frodo = arda_users.frodo

    wf = dedent("""
        conflicts = (
            ('admin', 'manager', 'security@example.yandex.ru'),
        )
        approvers = []
    """)
    set_workflow(simple_system, wf)

    manager_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    admin_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    admin_role.set_raw_state('need_request')

    clear_mailbox()

    admin_role.rerequest(frodo)

    assert len(mail.outbox) == 1
    conflict_message = mail.outbox[0]
    assert conflict_message.subject == 'Обнаружены конфликты при запросе роли для %s' % user_mention(frodo)
    assert conflict_message.to == ['security@example.yandex.ru']

    assert_contains((
        'В системе "Simple система" обнаружен конфликт ролей.',
        'Запрошена роль "Роль: Админ" (https://example.com/system/simple/#role=%s) для %s' %
        (admin_role.id, user_mention(frodo)),
        'Конфликтует с ролями:\nРоль: Менеджер (https://example.com/system/simple/#role=%s)' % manager_role.id
    ), conflict_message.body)


def test_conflict_for_import(simple_system, arda_users, idm_robot):
    frodo = arda_users.frodo
    wf = dedent("""
            conflicts = (
                ('admin', 'manager', 'security@example.yandex.ru'),
            )
            approvers = []
        """)
    set_workflow(simple_system, wf)

    manager_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    clear_mailbox()
    assert Role.objects.count() == 1

    simple_system.inconsistency_policy = SYSTEM_INCONSISTENCY_POLICY.TRUST
    simple_system.save()
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=simple_system,
        path='/role/admin/',
    )
    inconsistency.resolve(force=True)

    assert Role.objects.count() == 2
    admin_role = Role.objects.order_by('pk').last()

    assert len(mail.outbox) == 1
    conflict_message = mail.outbox[0]
    assert conflict_message.subject == 'Обнаружены конфликты при запросе роли для Фродо Бэггинс (frodo)'
    assert conflict_message.to == ['security@example.yandex.ru']

    assert_contains((
        'В системе "Simple система" обнаружен конфликт ролей.',
        'Запрошена роль "Роль: Админ" (https://example.com/system/simple/#role=%s) для %s' %
        (admin_role.id, user_mention(frodo)),
        'Конфликтует с ролями:\nРоль: Менеджер (https://example.com/system/simple/#role=%s)' % manager_role.id
    ), conflict_message.body)


def test_no_conflict(simple_system, arda_users):
    frodo = arda_users.frodo

    wf = dedent("""
        conflicts = (
            ('/admin/', '/manager/', 'security@example.yandex.ru'),
        )
        approvers = []""")
    set_workflow(simple_system, wf)

    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)

    assert len(mail.outbox) == 2


def test_no_conflict_deprived_role(simple_system, arda_users):
    frodo = arda_users.frodo

    wf = dedent("""
        conflicts = (
            ('/admin/', '/manager/', 'security@example.yandex.ru'),
        )
        approvers = []""")
    set_workflow(simple_system, wf)

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role.set_raw_state('deprived')

    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert len(mail.outbox) == 2


def test_conflict_group(simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship

    wf = dedent("""
        conflicts = (
            ('/admin/', '/manager/', 'security@example.yandex.ru'),
        )
        approvers = []""")
    set_workflow(simple_system, wf, wf)

    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)

    assert frodo.roles.count() == 1
    assert len(mail.outbox) == 1
    assert mail.outbox[0].subject == 'Simple система. Новая роль'
    assert mail.outbox[0].to == ['frodo@example.yandex.ru']

    admin_role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)

    assert len(mail.outbox) == 3
    assert mail.outbox[1].subject == (
        'Обнаружены конфликты при запросе роли для Братство кольца (fellowship-of-the-ring)'
    )
    assert mail.outbox[1].to == ['security@example.yandex.ru']

    assert_contains((
        'В системе "Simple система" обнаружен конфликт ролей.',
        'Запрошена роль "Роль: Админ" (https://example.com/system/simple/#role=%s) '
        'для Братство кольца' % admin_role.id,
        'Конфликтует с ролями:\nРоль: Менеджер'
    ), mail.outbox[1].body)

    assert mail.outbox[2].subject == 'Simple система. Новая роль'
    assert mail.outbox[2].to == ['frodo@example.yandex.ru']


def test_conflict_group_hierarchy(simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    associations = department_structure.associations
    fellowship = department_structure.fellowship

    wf = dedent("""
        conflicts = (
            ('/admin/', '/manager/', 'security@example.yandex.ru'),
        )
        approvers = []""")
    set_workflow(simple_system, wf, wf)

    Role.objects.request_role(frodo, associations, simple_system, '', {'role': 'manager'}, None)

    assert frodo.roles.count() == 1
    assert len(mail.outbox) == 1
    assert mail.outbox[0].subject == 'Simple система. Новая роль'
    assert mail.outbox[0].to == ['varda@example.yandex.ru']

    admin_role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)

    assert len(mail.outbox) == 3
    assert mail.outbox[1].subject == (
        'Обнаружены конфликты при запросе роли для Братство кольца (fellowship-of-the-ring)'
    )
    assert mail.outbox[1].to == ['security@example.yandex.ru']

    assert_contains((
        'В системе "Simple система" обнаружен конфликт ролей.',
        'Запрошена роль "Роль: Админ" (https://example.com/system/simple/#role=%s) '
        'для Братство кольца' % admin_role.id,
        'Конфликтует с ролями:\nРоль: Менеджер',
    ), mail.outbox[1].body)

    assert mail.outbox[2].subject == 'Simple система. Новая роль'
    assert mail.outbox[2].to == ['frodo@example.yandex.ru']


def test_conflict_group_reverse_hierarchy(simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    associations = department_structure.associations
    fellowship = department_structure.fellowship

    wf = dedent("""
        conflicts = (
            ('/admin/', '/manager/', 'security@example.yandex.ru'),
        )
        approvers = []""")
    set_workflow(simple_system, wf, wf)

    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)

    assert frodo.roles.count() == 1
    assert len(mail.outbox) == 1
    assert mail.outbox[0].subject == 'Simple система. Новая роль'
    assert mail.outbox[0].to == ['frodo@example.yandex.ru']

    Role.objects.request_role(frodo, associations, simple_system, '', {'role': 'admin'}, None)

    assert len(mail.outbox) == 2
    assert mail.outbox[1].subject == 'Simple система. Новая роль'
    assert mail.outbox[1].to == ['varda@example.yandex.ru']


def test_conflict_group_vs_personal(simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship

    wf = dedent("""
        conflicts = (
            ('/admin/', '/manager/', 'security@example.yandex.ru'),
        )
        approvers = []""")
    set_workflow(simple_system, wf, wf)

    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)

    assert frodo.roles.count() == 1
    assert len(mail.outbox) == 1
    assert mail.outbox[0].subject == 'Simple система. Новая роль'
    assert mail.outbox[0].to == ['frodo@example.yandex.ru']

    admin_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert len(mail.outbox) == 3
    assert mail.outbox[1].subject == 'Обнаружены конфликты при запросе роли для Фродо Бэггинс (frodo)'
    assert mail.outbox[1].to == ['security@example.yandex.ru']

    manager_role = Role.objects.get(user=frodo, node__value_path='/manager/')
    assert_contains((
        'В системе "Simple система" обнаружен конфликт ролей.',
        'Запрошена роль "Роль: Админ" (https://example.com/system/simple/#role=%s) для %s' %
        (admin_role.id, user_mention(frodo)),
        'Конфликтует с ролями:\nРоль: Менеджер (https://example.com/system/simple/#role=%s)' % manager_role.id
    ), mail.outbox[1].body)

    assert mail.outbox[2].subject == 'Simple система. Новая роль'
    assert mail.outbox[2].to == ['frodo@example.yandex.ru']


def test_conflict_personal_vs_group(simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship

    wf = dedent("""
        conflicts = (
            ('/admin/', '/manager/', 'security@example.yandex.ru'),
        )
        approvers = []""")
    set_workflow(simple_system, wf, wf)

    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)

    assert frodo.roles.count() == 1
    assert len(mail.outbox) == 1
    assert mail.outbox[0].subject == 'Simple система. Новая роль'
    assert mail.outbox[0].to == ['frodo@example.yandex.ru']

    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)

    assert len(mail.outbox) == 2
    assert mail.outbox[1].subject == 'Simple система. Новая роль'
    assert mail.outbox[1].to == ['frodo@example.yandex.ru']


def test_conflict_wildcards(simple_system, arda_users):
    frodo = arda_users.frodo

    wf = dedent("""
        conflicts = (
            ('/a%in', '/man%/', 'security@example.yandex.ru'),
            ('/a%i', '/man%/', 'failed@example.yandex.ru'),  # только полные совпадения
            ('%b%', '%', 'failed2@example.yandex.ru'),
        )
        approvers = []""")
    set_workflow(simple_system, wf)

    manager_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)

    assert frodo.roles.count() == 1
    assert len(mail.outbox) == 1
    assert mail.outbox[0].subject == 'Simple система. Новая роль'
    assert mail.outbox[0].to == ['frodo@example.yandex.ru']

    admin_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert len(mail.outbox) == 3
    assert mail.outbox[1].subject == 'Обнаружены конфликты при запросе роли для Фродо Бэггинс (frodo)'
    assert mail.outbox[1].to == ['security@example.yandex.ru']

    assert_contains((
        'В системе "Simple система" обнаружен конфликт ролей.',
        'Запрошена роль "Роль: Админ" (https://example.com/system/simple/#role=%s) для %s' %
        (admin_role.id, user_mention(frodo)),
        'Конфликтует с ролями:\nРоль: Менеджер (https://example.com/system/simple/#role=%s)' % manager_role.id,
        'Правило конфликта:',
        '/a%in конфликтует с /man%/',
    ), mail.outbox[1].body)

    assert mail.outbox[2].subject == 'Simple система. Новая роль'
    assert mail.outbox[2].to == ['frodo@example.yandex.ru']


def test_intersystem_conflict(simple_system, complex_system, pt1_system, arda_users):
    """Проверим работу межсистемных конфликтов (IDM-5021)"""

    RoleField.objects.filter(node__system=complex_system).delete()

    frodo = arda_users.frodo
    wf = dedent("""
        conflicts = (
            ('/a%in', 'complex', '/man%/', 'security@example.yandex.ru'),
            ('/a%i', 'complex', '/man%/', 'failed@example.yandex.ru'),  # только полные совпадения внутри slug-ов
            ('%b%', '%', 'failed2@example.yandex.ru'),
        )
        approvers = []""")
    set_workflow(simple_system, wf)

    manager_role1 = raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'manager'})
    manager_role2 = raw_make_role(frodo, pt1_system, {'project': 'proj1', 'role': 'manager'})

    assert frodo.roles.count() == 2
    clear_mailbox()
    admin_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    assert len(mail.outbox) == 2
    conflict_message, new_role_message = mail.outbox
    assert conflict_message.subject == 'Обнаружены конфликты при запросе роли для Фродо Бэггинс (frodo)'
    assert conflict_message.to == ['security@example.yandex.ru']

    assert_contains((
        'В системе "Simple система" обнаружен конфликт ролей.',
        'Запрошена роль "Роль: Админ" (https://example.com/system/simple/#role=%s) для %s' %
        (admin_role.id, user_mention(frodo)),
        'Конфликтует с ролями:\n',
        'Проект: Подписки, Роль: Менеджер (https://example.com/system/complex/#role=%s)' % manager_role1.id,
        'Правило конфликта:',
        '/a%in конфликтует с /man%/ в системе "Complex система"',
    ), conflict_message.body)

    # про вторую систему в письме ничего нет
    assert pt1_system.slug not in conflict_message.body
    assert six.text_type(manager_role2.pk) not in conflict_message.body

    assert new_role_message.subject == 'Simple система. Новая роль'
    assert new_role_message.to == ['frodo@example.yandex.ru']


def test_find_conflicts(simple_system, users_for_test):
    user = users_for_test[0]
    subj = subjectify(user)

    role = Role.objects.request_role(user, user, simple_system, None, {'role': 'manager'})
    node1 = RoleNode.objects.get(system=simple_system, slug='admin')
    node2 = RoleNode.objects.get(system=simple_system, slug='poweruser')
    node3 = RoleNode.objects.get(system=simple_system, slug='superuser')
    matrix = [
        ('admin', 'manager', 'security@example.yandex.ru'),
        ('po%ser', '%ger', 'another-security@example.yandex.ru'),
    ]
    conflicts = find_conflicts(subj, node1, matrix)
    assert conflicts
    assert list(list(conflicts.values())[0])[0][0] == role

    conflicts = find_conflicts(subj, node2, matrix)
    assert conflicts
    assert list(list(conflicts.values())[0])[0][0] == role

    assert not find_conflicts(subj, node3, matrix)


def test_invalid_conflict_syntax(simple_system, users_for_test):
    user = users_for_test[0]
    Role.objects.request_role(user, user, simple_system, None, {'role': 'manager'})
    node = RoleNode.objects.get(system=simple_system, slug='admin')
    with pytest.raises(ConflictValidationError):
        find_conflicts(subjectify(user), node, [
            ('admin', 'manager', 'security@example.yandex.ru'),
            ('po%ser', '%ger', 'another-security@example.yandex.ru', 'extra_field')])
    with pytest.raises(ConflictValidationError):
        find_conflicts(subjectify(user), node, [
            ('admin', 'manager', 'security@example.yandex.ru'),
            ('po%ser', '%ger')])


@waffle.testutils.override_switch('find_conflicts_in_workflow', active=True)
def test_find_conflicts_in_wf(simple_system, users_for_test):
    user = users_for_test[0]

    role1 = Role.objects.request_role(user, user, simple_system, None, {'role': 'manager'})
    set_workflow(simple_system, dedent('''
        approvers = []
        matrix = [
            ('admin', 'manager', 'security@example.yandex.ru'),
            ('po%ser', '%ger', 'another-security@example.yandex.ru'),
        ]
        conflicts = find_conflicts(node, user, matrix)
    '''))

    role2, context = Role.objects.request_or_simulate(
        requesterify(user), user, simple_system, {'role': 'poweruser'},
        fields_data=None, ttl_days=None, ttl_date=None, review_date=None, parent=None)
    role, conflict = context['conflicts']['another-security@example.yandex.ru'][0]
    assert role == role1
    assert conflict.conflicting_path == '%ger'


@waffle.testutils.override_switch('find_conflicts_in_workflow', active=True)
@pytest.mark.django_db
def test_conflict_comment(simple_system, users_for_test):
    user = users_for_test[0]

    set_workflow(simple_system, dedent('''
        approvers = []
        conflict_comment = 'here is conflict comment'
    '''))
    Role.objects.request_role(user, user, simple_system, None, {'role': 'manager'})
    action = Action.objects.get(action=ACTION.CONFLICT_COMMENT)
    assert action.data['comment'] == 'here is conflict comment'
