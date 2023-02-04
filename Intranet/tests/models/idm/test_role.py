# coding: utf-8
import datetime
import random
from unittest import mock

import pytest
import pytz

from django.core import mail
from django.core.management import call_command
from django.db.models import NOT_PROVIDED

from idm.core.workflow import exceptions
from idm.core.constants.role import ROLE_STATE
from idm.core.workflow.exceptions import InactiveSystemError
from idm.core.models import Role, RoleField, RoleNode, Transfer, UserPassportLogin
from idm.core.models.metrikacounter import MetrikaCounter
from idm.tests.models.test_metrikacounter import generate_counter_record
from idm.tests.utils import (refresh, assert_action_chain, set_workflow, clear_mailbox, assert_contains,
                             make_role, raw_make_role, change_department, move_group, accept, add_perms_by_role,
                             refresh_from_db, create_system, create_user, run_commit_hooks, create_group)
from idm.core.node_pipeline import NodePipeline
from idm.core.constants.node_relocation import RELOCATION_STATE
from idm.users.constants.user import USER_TYPES
from idm.users.models import Group, Organization, User
from idm.utils import events

pytestmark = pytest.mark.django_db


def test_role_rerequest(arda_users, simple_system):
    """Перезапросим роль тем же пользователем, которому она выдана"""

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    clear_mailbox()
    assert Role.objects.count() == 1
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.set_state('need_request')
    role.rerequest(frodo)
    role = refresh(role)
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'ask_rerequest', 'rerequest',
        'apply_workflow', 'approve', 'grant',
    ])
    assert len(mail.outbox) == 0


def test_rerun_workflow(arda_users, arda_users_with_roles, simple_system, idm_robot):
    workflow = '''
if role['role'] == 'superuser':
    ref_roles=[{
        'system': system.slug,
        'role_data': {
            'role': 'poweruser'
        },
    }]
approvers = []
    '''
    set_workflow(simple_system, workflow, group_code=workflow)
    fellowship_role = arda_users_with_roles['fellowship'][0]
    fellowship = fellowship_role.group
    sample_sam_role = Role.objects.get(parent=fellowship_role, user=arda_users.sam)
    group_role_a = Role.objects.request_role(idm_robot, fellowship, simple_system, '', {'role': 'superuser'}, None)
    group_role_b = Role.objects.get(parent=group_role_a, group=fellowship)
    for node in simple_system.nodes.all():
        node.relocation_state = RELOCATION_STATE.FIELDS_COMPUTED
        node.save(update_fields=['relocation_state'])
    sample_frodo_role = Role.objects.get(parent=group_role_a, user=arda_users.frodo)
    NodePipeline(simple_system).run()
    assert group_role_a.actions.filter(action='rerun_workflow').exists()
    assert group_role_b.actions.filter(action='rerun_workflow').exists()
    assert not sample_sam_role.actions.filter(action='rerun_workflow').exists()
    assert fellowship_role.actions.filter(action='rerun_workflow').exists()
    assert not sample_frodo_role.actions.filter(action='rerun_workflow').exists()


def test_send_reminder_about_role_requested_by_robot(simple_system, arda_users, robot_gollum):
    """Владелец робота не должен получать письма о ролях, запрошенных для робота"""

    frodo = arda_users.frodo
    robot_gollum.add_responsibles([frodo])
    robot_gollum.save()
    role = Role.objects.request_role(robot_gollum, robot_gollum, simple_system, '', {'role': 'manager'}, None)
    assert len(mail.outbox) == 1
    assert mail.outbox[0].to == ['gollum@example.yandex.ru']


def test_deprive_or_decline(arda_users, simple_system):
    frodo = arda_users.frodo

    deprive_info = [
        ('rerequested', 'deprived', ['deprive', 'first_remove_role_push', 'remove']),
        ('imported', 'deprived', ['deprive', 'first_remove_role_push', 'remove']),
        ('need_request', 'deprived', ['deprive', 'first_remove_role_push', 'remove']),
        ('granted', 'deprived', ['deprive', 'first_remove_role_push', 'remove']),
        ('depriving', 'deprived', ['redeprive', 'remove']),
        ('review_request', 'deprived', ['deprive', 'first_remove_role_push', 'remove']),
        ('onhold', 'deprived', ['deprive', 'first_remove_role_push', 'remove']),
        ('created', 'created', []),
        ('requested', 'declined', ['decline']),
        ('approved', 'approved', []),
        ('sent', 'deprived', ['deprive', 'first_remove_role_push', 'remove']),
        ('deprived', 'deprived', []),
        ('failed', 'failed', []),
        ('expired', 'expired', []),
        ('declined', 'declined', [])
    ]

    for initial, expected, actions in deprive_info:
        role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state=initial)
        role.deprive_or_decline(None, bypass_checks=True, comment='Тестовый комментарий')
        assert refresh(role).state == expected
        assert_action_chain(role, actions)
        if {'deprive', 'decline'} & set(actions):
            deprive_or_decline = role.actions.get(action__in=('deprive', 'decline'))
            assert 'Тестовый комментарий' in deprive_or_decline.comment
        role.delete()

    # тестируем deprive_all
    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='approved')
    role.deprive_or_decline(None, bypass_checks=True, comment='Тестовый комментарий', deprive_all=True)
    assert refresh(role).state == 'deprived'
    assert_action_chain(role, ['remove'])
    remove = role.actions.get(action='remove')
    assert 'Тестовый комментарий' in remove.comment
    role.delete()

    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='created')
    role.deprive_or_decline(None, bypass_checks=True, comment='Тестовый комментарий', deprive_all=True)
    assert refresh(role).state == 'deprived'
    assert_action_chain(role, ['remove'])
    remove = role.actions.get(action='remove')
    assert 'Тестовый комментарий' in remove.comment
    role.delete()


def test_rerequest_role_requiring_approval(arda_users, simple_system):
    """Перезапросим роль для случая, когда требуется подтверждение"""

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    clear_mailbox()
    set_workflow(simple_system, '''approvers = ['legolas']''')
    assert Role.objects.count() == 1
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.set_state('need_request')
    role.rerequest(frodo)
    role = refresh(role)
    assert role.state == 'rerequested'
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'ask_rerequest',
        'rerequest', 'apply_workflow',
    ])
    assert len(mail.outbox) == 0
    call_command('idm_send_notifications')
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['legolas@example.yandex.ru']
    assert message.subject == 'Подтверждение ролей.'
    assert_contains([
        'Вашего решения ожидает 1 запрос роли:',
        'Повторных запросов: 1',
        'Пройдите по ссылке https://example.com/queue/, чтобы подтвердить или отклонить запросы на выдачу ролей.',
    ], message.body)


def test_role_rerequest_with_comment(arda_users, simple_system):
    """Перезапросим роль с комментарием. Комментарий не отображается в письме-дайджесте, но тем не менее сохраняется
    для отображения на фронтэнде"""

    frodo = arda_users.frodo
    comment = 'Перезапрашиваю нужную роль'
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    clear_mailbox()
    set_workflow(simple_system, '''approvers = ['legolas']''')
    assert Role.objects.count() == 1
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.set_state('need_request')
    role.rerequest(frodo, comment=comment)
    role = refresh(role)
    assert role.state == 'rerequested'
    assert_action_chain(
        role,
        [
            'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
            'ask_rerequest', 'rerequest', 'apply_workflow',
        ]
    )
    assert len(mail.outbox) == 0
    call_command('idm_send_notifications')
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['legolas@example.yandex.ru']
    assert message.subject == 'Подтверждение ролей.'
    assert_contains([
        'Вашего решения ожидает 1 запрос роли:',
        'Повторных запросов: 1',
        'Пройдите по ссылке https://example.com/queue/, чтобы подтвердить или отклонить запросы на выдачу ролей.',
    ], message.body)
    rerequest_action = role.actions.get(action='rerequest')
    assert rerequest_action.comment == comment


def test_is_unique_and_get_alike(arda_users, simple_system):
    frodo = arda_users.frodo

    pairs_of_different = [
        # Теперь для нас роль с fields_data={}, НЕ эквивалентна роли с fields_data=None
        (None, []),
        ([], None),
        ({}, None),
        (None, {}),
        ([], {}),
        ({}, []),
        ('', {}),
        ('', []),
        ('', None),
        (None, {'a': 'b'}),
        ({}, {'': 0}),
        ({'a': 'b'}, {}),
        ({'a': 'b'}, [])
    ]
    for fields_data1, fields_data2 in pairs_of_different:
        Role.objects.all().delete()
        role1 = make_role(frodo, simple_system, {'role': 'manager'}, fields_data1)
        role2 = make_role(frodo, simple_system, {'role': 'manager'}, fields_data2)
        assert list(role1.get_alike()) == []
        assert list(role2.get_alike()) == []
        assert role1.is_unique()
        assert role2.is_unique()


def test_change_department(simple_system, arda_users, department_structure):
    """Тестирование смены подразделения пользователем"""

    gandalf = arda_users.gandalf
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    set_workflow(simple_system, 'approvers = [approver("legolas")]')
    gandalf_role = raw_make_role(gandalf, simple_system, {'role': 'admin'}, state='granted')

    # при переходе сотрудника в другой отдел роли должны переходить в need_request
    change_department(gandalf, fellowship, valinor)
    assert refresh(gandalf_role).state == 'granted'
    assert Transfer.objects.count() == 1
    transfer = Transfer.objects.select_related('user').get()
    assert transfer.user_id == gandalf.id
    assert transfer.type == 'user'
    assert transfer.source_path == '/middle-earth/associations/fellowship-of-the-ring/'
    assert transfer.target_path == '/valinor/'

    transfer.accept(bypass_checks=True)
    assert refresh(gandalf_role).state == 'need_request'

    assert_action_chain(gandalf_role, ['ask_rerequest'])
    ask_rerequest = gandalf_role.actions.get()
    expected_comment = (
        'Необходимо перезапросить роль в связи со сменой подразделения '
        'с "/Средиземье/Объединения/Братство кольца/" на "/Валинор/"'
    )
    assert ask_rerequest.comment == expected_comment
    assert ask_rerequest.transfer_id == transfer.id
    assert len(mail.outbox) == 0


def test_change_department_hidden_role(pt1_system, arda_users, department_structure):
    """Скрытая роль не перезапрашивается при переходе"""
    gandalf = arda_users.gandalf
    set_workflow(pt1_system, 'approvers = [approver("legolas")]')
    gandalf_role = raw_make_role(gandalf, pt1_system, {'project': 'proj2', 'role': 'invisible_role'}, state='granted')

    change_department(gandalf, department_structure.fellowship, department_structure.valinor)
    gandalf_role = refresh(gandalf_role)
    assert gandalf_role.state == 'granted'
    assert_action_chain(gandalf_role, [])
    assert len(mail.outbox) == 0


def test_change_department_ignore_policy(simple_system, arda_users, department_structure):
    """Персональная роль не перезапрашивается при переходе, если так настроена система"""
    simple_system.review_on_relocate_policy = 'ignore'
    simple_system.save()
    gandalf = arda_users.gandalf
    set_workflow(simple_system, 'approvers = [approver("legolas")]')
    gandalf_role = raw_make_role(gandalf, simple_system, {'role': 'admin'}, state='granted')

    change_department(gandalf, department_structure.fellowship, department_structure.valinor)
    gandalf_role = refresh(gandalf_role)
    assert gandalf_role.state == 'granted'
    assert_action_chain(gandalf_role, [])
    assert len(mail.outbox) == 0


def test_move_group_ignore_policy(simple_system, arda_users, department_structure):
    """Групповая роль не перезапрашивается при переходе, если так настроена система"""

    simple_system.review_on_relocate_policy = 'ignore'
    simple_system.save()
    set_workflow(simple_system, 'approvers = [approver("legolas")]', 'approvers = [approver("legolas")]')
    fellowship_role = raw_make_role(department_structure.fellowship, simple_system, {'role': 'admin'}, state='granted')

    move_group(department_structure.fellowship, department_structure.valinor)
    fellowship_role = refresh(fellowship_role)
    assert fellowship_role.state == 'granted'
    assert_action_chain(fellowship_role, [])
    assert len(mail.outbox) == 0


def test_roles_are_not_asked_to_rerequest_with_other_group_types(simple_system, arda_users, department_structure):
    """Если пользователь вышел из группы другого типа, например, вики-группы, то его роли не нужно пересматривать"""

    gandalf = arda_users.gandalf
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    set_workflow(simple_system, 'approvers = [approver("legolas")]')
    gandalf_role = raw_make_role(gandalf, simple_system, {'role': 'admin'}, state='granted')
    Group.objects.update(type='wiki')
    change_department(gandalf, fellowship, valinor)
    # и ничего не случилось
    assert refresh(gandalf_role).state == 'granted'
    assert_action_chain(gandalf_role, [])


def test_even_autoapproved_roles_are_asked_to_rerequest(simple_system, arda_users, department_structure):
    """Даже если роль может быть выдана автоматически, нужно перевести её в need_request –
    вдруг она не нужна пользователю, пусть он тогда её самостоятельно отзовёт"""

    gandalf = arda_users.gandalf
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    set_workflow(simple_system, 'approvers = []')
    gandalf_role = Role.objects.request_role(gandalf, gandalf, simple_system, '', {'role': 'admin'}, None)
    change_department(gandalf, fellowship, valinor)
    assert refresh(gandalf_role).state == 'granted'
    assert_action_chain(gandalf_role, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])
    assert gandalf.transfers.count() == 1

    gandalf.transfers.get().accept(bypass_checks=True)
    assert refresh(gandalf_role).state == 'need_request'
    assert_action_chain(gandalf_role,
                        ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'ask_rerequest'])


def test_only_granted_roles_are_asked_to_be_rerequested(simple_system, arda_users, department_structure):
    """Из всех доступных статусов в need_request будут переведены только granted роли"""

    gandalf = arda_users.gandalf
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    set_workflow(simple_system, 'approvers = []')
    roles = {}
    for state in ROLE_STATE.ALL_STATES:
        role = raw_make_role(gandalf, simple_system, {'role': 'admin'}, fields_data={'login': state}, state=state)
        roles[state] = role
    change_department(gandalf, fellowship, valinor)
    for state in ROLE_STATE.ALL_STATES:
        role = refresh(roles[state])
        assert role.state == state
        assert_action_chain(role, [])

    accept(Transfer.objects.all())

    for state in ROLE_STATE.ALL_STATES:
        role = refresh(roles[state])
        if state == 'granted':
            assert role.state == 'need_request'
            assert_action_chain(role, ['ask_rerequest'])
        else:
            assert role.state == state
            assert_action_chain(role, [])


def test_reminders_are_not_sent_if_there_are_roles_that_need_rerequest(simple_system, arda_users, department_structure):
    """Если нет ролей, которые требуют перезапроса, то переход в другой департамент не отсылает писем"""

    gandalf = arda_users.gandalf
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    set_workflow(simple_system, 'approvers = [approver("legolas")]')
    change_department(gandalf, fellowship, valinor)

    assert len(mail.outbox) == 0


def test_roles_are_asked_to_be_rerequested_on_broken_system_too(simple_system, arda_users, department_structure):
    """Если система сломана, то роли всё равно переходят в need_request"""

    simple_system.is_broken = True
    simple_system.save()

    gandalf = arda_users.gandalf
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    set_workflow(simple_system, 'approvers = []')
    roles = {}
    for state in ROLE_STATE.ALL_STATES:
        role = raw_make_role(gandalf, simple_system, {'role': 'admin'}, fields_data={'login': state}, state=state)
        roles[state] = role
    change_department(gandalf, fellowship, valinor)
    accept(Transfer.objects.all())

    for state in ROLE_STATE.ALL_STATES:
        role = refresh(roles[state])
        if state == 'granted':
            assert role.state == 'need_request'
            assert_action_chain(role, ['ask_rerequest'])
        else:
            assert role.state == state
            assert_action_chain(role, [])


def test_transfer_is_rejected(simple_system, arda_users, department_structure):
    """Если система сломана, то роли всё равно переходят в need_request"""

    gandalf = arda_users.gandalf
    frodo = arda_users.frodo
    add_perms_by_role('superuser', frodo)
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    set_workflow(simple_system, 'approvers = []')

    role = raw_make_role(gandalf, simple_system, {'role': 'admin'}, state='granted')
    change_department(gandalf, fellowship, valinor)

    role = refresh(role)
    assert_action_chain(role, [])
    assert role.state == 'granted'

    transfer = Transfer.objects.select_related('user').get()
    transfer.reject(requester=frodo)
    role = refresh(role)
    assert role.state == 'granted'
    assert_action_chain(role, ['keep_granted'])
    action = role.actions.get()
    assert action.comment is None


def test_group_roles_are_not_asked_to_be_rerequested_if_system_is_broken(simple_system, arda_users,
                                                                         department_structure):
    """При перемещении группы её групповые роли не будут переведены в need_request, если система сломана"""
    set_workflow(simple_system, 'approvers = [approver("legolas")]')

    simple_system.is_broken = True
    simple_system.save()

    middle_earth = department_structure.earth
    associations = department_structure.associations
    fellowship = department_structure.fellowship

    # создадим новую группу, дочернюю для middle_earth
    fellowships = Group(
        parent=middle_earth,
        slug='fellowships',
        name='Братства',
        type=middle_earth.type
    )
    fellowships.save()

    roles = []
    for state in ROLE_STATE.ALL_STATES:
        for group, role_slug in [(middle_earth, 'manager'), (associations, 'poweruser'), (fellowship, 'superuser'),
                                 (fellowships, 'admin')]:
            role = raw_make_role(group, simple_system, {'role': role_slug}, fields_data={'login': state}, state=state)
            roles.append((role, state))

    move_group(fellowship, fellowships)

    for role, state in roles:
        role = refresh(role)
        assert role.state == state
        assert_action_chain(role, [])


def test_roles_granted_after_transfer_are_not_rerequested(simple_system, arda_users, department_structure):
    """Проверим, что роли, запрошенные после перемещения, не пересматриваются при применении перемещения.
    Но при этом роли, запрошенные до, а выданные после перемещения, всё же пересматриваются при применении
    перемещения."""

    gandalf = arda_users.gandalf
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor

    # эта роль должна быть пересмотрена
    role1 = Role.objects.request_role(gandalf, gandalf, simple_system, '', {'role': 'admin'}, None)

    change_department(gandalf, fellowship, valinor)

    # эта роль не должна быть пересмотрена
    role2 = Role.objects.request_role(gandalf, gandalf, simple_system, '', {'role': 'manager'}, None)
    set_workflow(simple_system, 'approvers = ["frodo"]')

    assert Transfer.objects.count() == 1
    accept(Transfer.objects.all())

    role1 = refresh(role1)
    role2 = refresh(role2)
    assert role1.state == 'need_request'
    assert role2.state == 'granted'
    assert_action_chain(role1,
                        ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'ask_rerequest'])
    assert_action_chain(role2, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])


def test_inner_transitions_in_broken_system(simple_system, arda_users, department_structure):
    gandalf = arda_users.gandalf
    fellowship = department_structure.fellowship
    set_workflow(simple_system, group_code='approvers = []')

    role = Role.objects.request_role(gandalf, fellowship, simple_system, '', {'role': 'admin'}, None)
    assert role.state == 'granted'

    simple_system.is_broken = True
    simple_system.save()

    # отозвать не можем
    with pytest.raises(exceptions.BrokenSystemError):
        role.set_state('depriving')
    assert role.state == 'granted'

    # но можем отправить в холд и вернуть назад
    role.set_state('onhold')
    assert role.state == 'onhold'
    role.set_state('granted')
    assert role.state == 'granted'


def test_request_role_in_inactive_system(simple_system, arda_users):
    """
        Проверка сообщения исключения InactiveSystemError при запросе роли в неактивной системе
    """
    set_workflow(simple_system, 'approvers = ["legolas"]')

    simple_system.is_active = False
    simple_system.save()

    with pytest.raises(InactiveSystemError) as exc:
        Role.objects.request_role(arda_users.gandalf, arda_users.gandalf, simple_system, '', {'role': 'admin'}, None)

    assert str(exc.value) == "Система \"{}\" неактивна. Роль не может быть запрошена.".format(
        simple_system.get_name())


def test_subject_responsibles_for_user(simple_system, arda_users):
    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = []')
    role = Role.objects.request_role(
        requester=frodo,
        subject=frodo,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
    )
    assert len(frodo.all_heads) > 0
    assert set(role.get_subject().get_responsibles(with_heads=True)) == {frodo} | set(frodo.all_heads)


def test_subject_responsibles_for_group(simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    set_workflow(simple_system, 'approvers = []', group_code='approvers = []')
    role = Role.objects.request_role(
        requester=frodo,
        subject=fellowship,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
    )
    assert len(fellowship.responsibles) > 0
    assert set(role.get_subject().get_responsibles(with_heads=True)) == set(fellowship.get_responsibles())


def test_subject_responsibles_for_robot(simple_system, arda_users, robot_gollum):
    frodo = arda_users.frodo
    gollum = robot_gollum
    gollum.add_responsibles([frodo])
    set_workflow(simple_system, 'approvers = []')
    role = Role.objects.request_role(
        requester=frodo,
        subject=gollum,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
    )
    gollum_responsibles = list(gollum.responsibles.all())
    assert len(gollum_responsibles) > 0
    assert set(gollum_responsibles) | {gollum} == set(role.get_subject().get_responsibles(with_heads=True))
    assert set(role.get_subject().get_responsibles()) == {gollum}
    gollum.notify_responsibles = True
    gollum.save()
    assert set(role.get_subject().get_responsibles()) == {gollum, frodo}


def test_check_awaiting_conditions(simple_system, arda_users):
    node = RoleNode.objects.get_node_by_data(simple_system, {'role': 'admin'})
    RoleField.objects.create(node=node, type='charfield', name='Resource Id', slug='resource_id', is_required=True)
    RoleField.objects.filter(slug='passport-login').update(is_required=True)
    frodo = arda_users.frodo
    organization = Organization.objects.create(org_id=1000000)
    org_user = User.objects.create(username='org_%s' % organization.pk, type=USER_TYPES.ORGANIZATION)
    login = UserPassportLogin.objects.create(login='yndx-frodo', user=frodo)
    set_workflow(simple_system, 'approvers=[]', 'approvers=[]')

    frodo_role = raw_make_role(
        subject=frodo,
        system=simple_system,
        data={'role': 'admin'},
        organization_id=organization.id,
        fields_data={'resource_id': 'some_resource_id'},
    )

    state, comments = frodo_role.check_awaiting_conditions(passport_login_id=NOT_PROVIDED)
    assert state == ROLE_STATE.AWAITING
    assert comments == [
        'Роль требует указания паспортного логина',
        'Ресурс, на который запрашивается роль, ещё не привязан к организации',
    ]

    frodo_role.fields_data['passport-login'] = login.login
    frodo_role.passport_logins.add(login)
    frodo_role.save(update_fields=['fields_data'])
    state, comments = frodo_role.check_awaiting_conditions(passport_login_id=NOT_PROVIDED)
    assert state == ROLE_STATE.AWAITING
    assert comments == [
        'Требуется дорегистрировать паспортный логин',
        'Ресурс, на который запрашивается роль, ещё не привязан к организации',
    ]

    login.is_fully_registered = True
    login.save()

    state, comments = frodo_role.check_awaiting_conditions(passport_login_id=NOT_PROVIDED)
    assert state == ROLE_STATE.AWAITING
    assert comments == [
        'Ресурс, на который запрашивается роль, ещё не привязан к организации',
    ]

    org_role = raw_make_role(
        subject=org_user,
        system=simple_system,
        data={'role': 'admin'},
        organization_id=organization.id,
        fields_data={'resource_id': 'some_resource_id'},
    )
    state, comments = frodo_role.check_awaiting_conditions(passport_login_id=NOT_PROVIDED)
    assert state == ROLE_STATE.GRANTED
    assert not comments


def test_role_in_awaiting_state_if_conditions_not_met(simple_system, arda_users, department_structure):
    """
    Проверим, что если не выполнены все условия из списка CHECK_AWAITING_CONDITION_VALIDATORS,
    роль будет находиться в состоянии `awaiting`
    """
    # Подготовим данные для теста
    node = RoleNode.objects.get_node_by_data(simple_system, {'role': 'admin'})
    RoleField.objects.create(node=node, type='charfield', name='Resource Id', slug='resource_id', is_required=True)
    RoleField.objects.filter(slug='passport-login').update(is_required=True)
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    organization = Organization.objects.create(org_id=1000000)
    org_user = User.objects.create(username='org_%s' % organization.pk, type=USER_TYPES.ORGANIZATION)
    set_workflow(simple_system, 'approvers=[]', 'approvers=[]')

    # Создадим два паспортных логина, чтоб автоматически не привязать ничего к роли frodo
    login1 = UserPassportLogin.objects.create(login='yndx-frodo-1', user=frodo)
    login2 = UserPassportLogin.objects.create(login='yndx-frodo-2', user=frodo)

    group_role = Role.objects.request_role(
        requester=frodo,
        subject=fellowship,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
        organization_id=organization.id,
        fields_data={'resource_id': 'some_resource_id'},
    )

    frodo_role = group_role.refs.get(user=frodo)

    # Пока в Коннекте нет групповых ролей - организация не пробрасывается в персональные роли, выдаваемые по групповым
    group_role.refs.update(organization_id=group_role.organization_id)
    assert frodo_role.state == ROLE_STATE.AWAITING
    assert frodo_role.passport_logins.count() == 0
    assert frodo_role.fields_data == {'resource_id': 'some_resource_id'}

    # Добавим паспортный логин к членству в группе, проверим что он подтянется в персональную роль
    fellowship.memberships.filter(user=frodo).update(passport_login=login1)
    Role.objects.poke_awaiting_roles()
    frodo_role.refresh_from_db()

    assert frodo_role.state == ROLE_STATE.AWAITING
    assert frodo_role.passport_logins.count() == 1
    assert frodo_role.passport_logins.get() == login1
    assert frodo_role.fields_data == {'resource_id': 'some_resource_id', 'passport-login': 'yndx-frodo-1'}

    # Дорегистрируем паспортный логин
    frodo.passport_logins.update(is_fully_registered=True)
    Role.objects.poke_awaiting_roles()
    frodo_role.refresh_from_db()

    # Роль всё ещё в статусе `awaiting`, т.к. не выполнено условие `resource_associate_condition`
    # Ресурс не привязан к организации
    assert frodo_role.state == ROLE_STATE.AWAITING

    # Привяжем ресурс к организации, проверим, что роль frodo перейдет в статус `granted`
    org_role = raw_make_role(
        subject=org_user,
        system=simple_system,
        data={'role': 'admin'},
        organization_id=organization.id,
        fields_data={'resource_id': 'some_resource_id'},
    )
    Role.objects.poke_awaiting_roles()
    frodo_role.refresh_from_db()

    assert frodo_role.state == ROLE_STATE.GRANTED


def test_poke_personal_roles_when_resource_associated(simple_system, arda_users):
    """
    Проверим, что после выдачи роли на организацию будут довыданы персональные роли на этот же ресурс
    Другие роли в статусе `awaiting` довыдаваться не будут
    """
    node = RoleNode.objects.get_node_by_data(simple_system, {'role': 'admin'})
    RoleField.objects.create(node=node, type='charfield', name='Resource Id', slug='resource_id', is_required=False)
    frodo = arda_users.frodo
    sam = arda_users.sam
    organization = Organization.objects.create(org_id=1000000)
    org_user = User.objects.create(username='org_%s' % organization.pk, type=USER_TYPES.ORGANIZATION)

    login = UserPassportLogin.objects.create(login='yndx-frodo', user=frodo)

    frodo_role = Role.objects.request_role(
        requester=frodo,
        subject=frodo,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
        organization_id=organization.id,
        fields_data={'resource_id': 'some_resource_id'},
    )

    frodo_role_with_passport_login = Role.objects.request_role(
        requester=frodo,
        subject=frodo,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
        organization_id=organization.id,
        fields_data={'passport-login': 'yndx-frodo'},
    )

    sam_role = Role.objects.request_role(
        requester=sam,
        subject=sam,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
        organization_id=organization.id,
        fields_data={'resource_id': 'other_resource_id'},
    )
    refresh_from_db(frodo_role, frodo_role_with_passport_login, sam_role)

    assert frodo_role.state == ROLE_STATE.AWAITING
    assert frodo_role.fields_data == {'resource_id': 'some_resource_id'}

    assert frodo_role_with_passport_login.state == ROLE_STATE.AWAITING
    assert frodo_role_with_passport_login.fields_data == {'passport-login': 'yndx-frodo'}

    assert sam_role.state == ROLE_STATE.AWAITING
    assert sam_role.fields_data == {'resource_id': 'other_resource_id'}

    login.is_fully_registered = True
    login.save()

    # Привяжем ресурс к организации, проверим, что роль frodo перейдет в статус `granted`
    org_role = Role.objects.request_role(
        requester=org_user,
        subject=org_user,
        system=simple_system,
        comment='',
        data={'role': 'admin'},
        organization_id=organization.id,
        fields_data={'resource_id': 'some_resource_id'},
    )
    refresh_from_db(frodo_role, frodo_role_with_passport_login, sam_role, org_role)

    assert org_role.state == ROLE_STATE.GRANTED
    assert frodo_role.state == ROLE_STATE.GRANTED
    assert sam_role.state == ROLE_STATE.AWAITING
    assert frodo_role_with_passport_login.state == ROLE_STATE.AWAITING


def test_restore_deprived_role(arda_users, simple_system):
    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state=ROLE_STATE.DEPRIVED)
    role.restore(comment='comment')

    role.refresh_from_db()
    assert role.state == ROLE_STATE.APPROVED
    assert role.actions.get().action == 'restore'


@pytest.mark.parametrize('from_role_state', [ROLE_STATE.DEPRIVING_VALIDATION, ROLE_STATE.ONHOLD])
def test_restore_personal_roles_by_restoring_groupmembership(
        arda_users, department_structure, simple_system, from_role_state
):
    """
    Тест что персональная роль, выданная по групповой может быть восстановлена
    из ONHOLD, DEPRIVING_VALIDATION
    Для этого надо заново попытаться её выдать.
    """
    # arrange
    group_role = raw_make_role(
        department_structure.fellowship,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.GRANTED,
    )
    personal_role = raw_make_role(
        arda_users.frodo,
        simple_system,
        {'role': 'admin'},
        state=from_role_state,
        parent=group_role,
    )

    # act
    new_role = group_role.create_group_member_role(arda_users.frodo, replace_login=True)

    # assert
    personal_role.refresh_from_db()
    assert new_role == personal_role
    assert personal_role.state == ROLE_STATE.GRANTED


def test_restore_deprived_requested_role(arda_users, simple_system):
    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state=ROLE_STATE.REQUESTED)
    role.restore(comment='comment')

    role.refresh_from_db()
    assert role.state == ROLE_STATE.APPROVED
    assert list(role.actions.order_by('added').values_list('action', flat=True)) == ['decline', 'restore']


def test_restore_deprived_role_with_alike_requested(arda_users, simple_system):
    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state=ROLE_STATE.DEPRIVED)
    requested_role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state=ROLE_STATE.REQUESTED)
    role.restore(comment='comment')

    role.refresh_from_db()
    assert role.state == ROLE_STATE.APPROVED
    assert role.actions.get().action == 'restore'

    expected_comment = (
            'Отклонение роли в связи с восстановлением https://example.com/system/%s/#role=%s' % (
        simple_system.slug, role.id)
    )
    requested_role.refresh_from_db()
    assert requested_role.state == ROLE_STATE.DECLINED
    assert requested_role.actions.get().data['comment'] == expected_comment


def test_restore_deprived_role_with_alike_granted(arda_users, simple_system):
    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state=ROLE_STATE.DEPRIVED)
    granted_role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state=ROLE_STATE.GRANTED)
    role.restore(comment='comment')

    role.refresh_from_db()
    assert role.state == ROLE_STATE.DEPRIVED
    assert not role.actions.exists()


def test_restore_depriving_role(arda_users, simple_system):
    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state=ROLE_STATE.DEPRIVING)
    role.restore(comment='comment')

    role.refresh_from_db()
    assert role.state == ROLE_STATE.GRANTED
    assert role.actions.get().action == 'restore'


@pytest.mark.parametrize('state',
                         ROLE_STATE.ALL_STATES - {ROLE_STATE.REQUESTED, ROLE_STATE.DEPRIVING, ROLE_STATE.DEPRIVED})
def test_restore_wrong_role(arda_users, simple_system, state):
    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state=state)
    with pytest.raises(ValueError):
        role.restore(comment='comment')

    role.refresh_from_db()
    assert role.state == state
    assert not role.actions.exists()


def test_rerequest_broken_role(simple_system, arda_users):
    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role.refresh_from_db()

    role.set_raw_state(ROLE_STATE.IDM_ERROR)
    role.rerequest(frodo)
    role.refresh_from_db()
    assert role.state == ROLE_STATE.GRANTED


def test_email_humanize__metrika_counter(metrika_system):
    rolenode = metrika_system.nodes.last()

    counter = MetrikaCounter.objects.create(**generate_counter_record().as_dict)
    fields_data = {'counter_id': counter.counter_id, 'counter_name': counter.name}
    user = create_user()
    role = raw_make_role(
        subject=user,
        system=metrika_system,
        data=rolenode.data,
        fields_data=fields_data,
    )

    expected_fields_data = {**fields_data, 'counter_name': counter.name}
    assert_contains([
        f'Роль: {rolenode.get_name()} ({counter.name})',
        f'Данные полей: {expected_fields_data}'
    ], role.email_humanize())


@pytest.mark.parametrize('create_with_empty_name', [True, False])
def test_email_humanize__metrika_counter_not_found(create_with_empty_name: bool, metrika_system):
    rolenode = metrika_system.nodes.last()

    counter_id = str(random.randint(1, 10*8))
    if create_with_empty_name:
        MetrikaCounter.objects.create(
            counter_id=counter_id,
            update_time=datetime.datetime.now(tz=pytz.UTC).strftime(MetrikaCounter.UPDATE_TIME_FORMAT),
        )
    fields_data = {'counter_id': counter_id}
    user = create_user()
    role = raw_make_role(
        subject=user,
        system=metrika_system,
        data=rolenode.data,
        fields_data=fields_data,
    )

    assert_contains([
        f'Роль: {rolenode.get_name()}',
        f'Данные полей: {fields_data}'
    ], role.email_humanize())


@pytest.mark.parametrize('is_group_role', [True, False])
@pytest.mark.parametrize('system_export_to_tirole', [True, False])
@pytest.mark.parametrize(('from_state', 'to_state', 'expect_call'), [
    (ROLE_STATE.REQUESTED, ROLE_STATE.DECLINED, False),
    (ROLE_STATE.APPROVED, ROLE_STATE.GRANTED, True),
    (ROLE_STATE.GRANTED, ROLE_STATE.REVIEW_REQUEST, False),
    (ROLE_STATE.DEPRIVING, ROLE_STATE.DEPRIVED, True),
])
def test_export_role_to_tirole(
        system_export_to_tirole: bool,
        is_group_role: bool,
        from_state: str,
        to_state: str,
        expect_call: bool,
        mongo_mock,
):
    system = create_system(
        export_to_tirole=system_export_to_tirole,
        workflow=f'approvers = ["{create_user().username}"]',
    )
    subject = is_group_role and create_group() or create_user()
    role = raw_make_role(subject, system, system.nodes.last().data)
    role.set_raw_state(from_state)
    with run_commit_hooks(), mock.patch('idm.utils.events.add_event') as add_event_mock:
        role.set_state(to_state)

    if system_export_to_tirole and not is_group_role and expect_call:
        add_event_mock.assert_called_once_with(
            event_type=events.EventType.YT_EXPORT_REQUIRED,
            system_id=system.id,
            role_id=role.id,
        )
    else:
        add_event_mock.assert_not_called()
