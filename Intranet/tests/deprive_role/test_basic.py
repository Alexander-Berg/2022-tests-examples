import freezegun
import pytest
from django.core import mail
from waffle.testutils import override_switch

from idm.core.workflow import exceptions
from idm.core.constants.role import ROLE_STATE
from idm.core.models import Role, RoleField
from idm.tests.utils import (refresh, clear_mailbox, assert_contains, set_workflow, assert_action_chain,
                             add_perms_by_role, raw_make_role, capture_http, Response, assert_http)
from idm.users.models import Group, Department
from idm.utils import json

pytestmark = pytest.mark.django_db


@override_switch('stop_depriving', active=True)
def test_waffle_stop_depriving_switch(simple_system, arda_users):
    frodo = arda_users.frodo
    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted')
    role.deprive_or_decline(frodo, 'xxx')
    role.refresh_from_db()
    assert role.state == ROLE_STATE.DEPRIVING


def test_system_stop_depriving_per_system(simple_system, arda_users):
    frodo = arda_users.frodo
    simple_system.stop_depriving = True
    simple_system.save()
    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted')
    role.deprive_or_decline(frodo, 'xxx')
    role.refresh_from_db()
    assert role.state == ROLE_STATE.DEPRIVING


def test_basic_role_deprive(simple_system, arda_users, depriver_users):
    """Проверка базового сценария отзыва роли"""
    frodo = arda_users.frodo

    for depriver, can_deprive in depriver_users:
        role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
        role = Role.objects.select_related('node', 'user', 'user__department_group').get(pk=role.pk)
        assert role.state == 'granted'
        clear_mailbox()

        if can_deprive:
            role.deprive_or_decline(depriver)
            assert len(mail.outbox) == 1
            message = mail.outbox[0]
            assert message.to == ['frodo@example.yandex.ru']
            assert_contains([
                depriver.username,
                'отозвал вашу роль в системе "Simple система":',
                'Роль: Менеджер',
            ], message.body)
        else:
            with pytest.raises(exceptions.Forbidden):
                role.deprive_or_decline(depriver)
            assert len(mail.outbox) == 0
        role.delete()


def test_requester_can_deprive(simple_system, arda_users):
    simple_system.request_policy = 'anyone'
    simple_system.save()
    frodo = arda_users.frodo
    legolas = arda_users.legolas

    role = Role.objects.request_role(legolas, frodo, simple_system, '', {'role': 'manager'}, None)
    role = refresh(role)
    assert role.state == 'granted'

    role.deprive_or_decline(legolas)
    role = refresh(role)
    assert role.state == 'deprived'


def test_basic_role_deprive_with_comment(simple_system, arda_users, depriver_users):
    """Проверка базового сценария отзыва роли с комментарием"""
    set_workflow(simple_system, 'approvers=[]\nemail_cc=[recipient("legolas@example.yandex.ru", deprived=True)]')
    frodo = arda_users.frodo
    comment = 'Отзывая ненужную более роль, передайте женщине соль!'

    for depriver, can_deprive in depriver_users:
        role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
        role = Role.objects.select_related('node', 'user', 'user__department_group').get(pk=role.pk)
        assert role.state == 'granted'
        clear_mailbox()

        if can_deprive:
            role.deprive_or_decline(depriver, comment=comment)
            assert len(mail.outbox) == 2
            message = mail.outbox[0]
            assert message.to == ['frodo@example.yandex.ru']
            assert_contains([
                depriver.username,
                comment,
                'отозвал вашу роль в системе "Simple система":',
                'Роль: Менеджер',
            ], message.body)
            cc_message = mail.outbox[1]

            assert cc_message.to == ['legolas@example.yandex.ru']
            assert_contains([
                depriver.username,
                comment,
                'отозвал роль сотрудника Фродо Бэггинс (frodo) в системе "Simple система":',
                'Роль: Менеджер',
            ], cc_message.body)
        else:
            with pytest.raises(exceptions.Forbidden):
                role.deprive_or_decline(depriver)
            assert len(mail.outbox) == 0
        role.delete()


def test_role_owner_can_deprive_it_before_approval(simple_system, arda_users):
    """Проверка, что человек, для которого была запрошена роль, может отозвать эту роль до того,
    как по ней будет принято решение подтверждающими"""

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    add_perms_by_role('responsible', gandalf, simple_system)
    set_workflow(simple_system, '''approvers = ['legolas']''')
    role = Role.objects.request_role(gandalf, frodo, simple_system, '', {'role': 'manager'}, None)
    role.deprive_or_decline(frodo)
    role = refresh(role)
    assert role.state == 'declined'
    assert_action_chain(role, ['request', 'apply_workflow', 'decline'])


def test_group_responsible_can_deprive_group_role_before_approval(simple_system, arda_users, department_structure):
    """Проверка, что ответственный группы, для которой была запрошена роль, может отозвать эту роль до того,
    как по ней было принято решение подтверждающими"""

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    add_perms_by_role('responsible', gandalf, simple_system)
    set_workflow(simple_system, group_code='''approvers = ['legolas']''')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    role = Role.objects.request_role(gandalf, fellowship, simple_system, '', {'role': 'manager'}, None)
    role.deprive_or_decline(frodo)
    role = refresh(role)
    assert role.state == 'declined'
    assert_action_chain(role, ['request', 'apply_workflow', 'decline'])


def test_deprive_role_with_limited_rights_in_system(complex_system, arda_users):
    """Проверим, что человек с ограниченными правами в системе может отзывать чужие роли, но только в рамках
    наложенных ограничений"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    RoleField.objects.all().delete()
    # TODO: перейти на department_structure при переводе get_all_heads на работу с моделью Group, а не Department
    department = Department.objects.create(name='Братство кольца', slug='fellowship', chief=frodo)
    legolas.department = department
    legolas.save()
    add_perms_by_role('roles_manage', legolas, complex_system, scope='/rules/')
    role1 = raw_make_role(frodo, complex_system, {'project': 'rules', 'role': 'invisic'}, state='granted')
    role2 = raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'manager'}, state='granted')
    role1.deprive_or_decline(legolas)
    role1 = refresh(role1)
    assert role1.state == 'deprived'
    with pytest.raises(exceptions.Forbidden):
        role2.deprive_or_decline(legolas)


def test_can_deprive_subordinates_role_irregardless_of_limited_rights(complex_system, arda_users):
    """Проверим, что если у сотрудника имеются ограниченные права в системе, то у подчинённого всё равно можно
    отозвать любую роль"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    RoleField.objects.all().delete()
    # TODO: перейти на department_structure при переводе get_all_heads на работу с моделью Group, а не Department
    department = Department.objects.create(name='Братство кольца', slug='fellowship', chief=frodo)
    legolas.department = department
    legolas.save()
    add_perms_by_role('roles_manage', frodo, complex_system, scope='/rules/')
    role1 = raw_make_role(legolas, complex_system, {'project': 'rules', 'role': 'invisic'}, state='granted')
    role2 = raw_make_role(legolas, complex_system, {'project': 'subs', 'role': 'manager'}, state='granted')
    role1.deprive_or_decline(frodo)
    role1 = refresh(role1)
    assert role1.state == 'deprived'
    role2.deprive_or_decline(frodo)
    role1 = refresh(role2)
    assert role1.state == 'deprived'


def test_deprive_role_from_need_request_with_comment(simple_system, arda_users):
    """Проверим отзыв роли с комментарием из состояния need_request [RULES-2188]"""

    frodo = arda_users.frodo
    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='need_request')
    role.deprive_or_decline(frodo, 'Эта роль мне не нужна')
    role = refresh(role)
    assert role.state == 'deprived'
    assert_action_chain(role, ['deprive', 'first_remove_role_push', 'remove'])
    deprive_action = role.actions.get(action='deprive')
    expected_comment = 'Неперезапрошенная роль отозвана и будет удалена из системы: Эта роль мне не нужна'
    assert deprive_action.comment == expected_comment


def test_deprive_sent_role(simple_system, arda_users):
    frodo = arda_users.frodo
    role = raw_make_role(frodo, simple_system, {'role': 'manager'}, state='sent')

    role.deprive_or_decline(frodo, 'Эта роль мне не нужна')

    role = refresh(role)
    assert role.state == 'deprived'
    assert role.actions.filter(action='deprive').exists() is True


def test_depriving_any_role_pushes_to_system(generic_system, arda_users):
    """Проверим, что отзыв любой активной роли приводит к пушу в систему, если роль активна"""

    frodo = arda_users.frodo

    def return_data(url, **kwargs):
        if 'remove-role' in url:
            return Response(200, {'code': 0})
        return Response(500, {})

    with capture_http(generic_system, side_effect=return_data) as send_data:
        for state in ROLE_STATE.ALL_STATES:
            send_data.http_post.reset_mock()
            state_is_deprivable = state in ROLE_STATE.DEPRIVABLE_STATES
            role = raw_make_role(frodo, generic_system, {'role': 'admin'}, state=state,
                                 fields_data={'login': state},
                                 system_specific={'login': state})

            if state in ('created', 'approved', 'declined', 'deprived', 'expired', 'failed', 'idm_error'):
                role.deprive_or_decline(None, bypass_checks=True)
                role = refresh(role)
                assert role.state == state
                continue

            role.deprive_or_decline(None, bypass_checks=True)
            role = refresh(role)
            if state_is_deprivable:
                if state == ROLE_STATE.DEPRIVING:
                    expected_chain = ['redeprive', 'remove']
                else:
                    expected_chain = ['deprive', 'first_remove_role_push', 'remove']
                assert role.state == 'deprived'
                assert_action_chain(role, expected_chain)
            else:
                assert role.state == 'declined'
                assert_action_chain(role, ['decline'])

            if state_is_deprivable:
                assert_http(send_data.http_post, url='http://example.com/remove-role/', data={
                    'fields': json.dumps(role.fields_data),
                    'login': 'frodo',
                    'role': json.dumps(role.node.data),
                    'path': '/role/admin/',
                })
            else:
                assert len(send_data.http_post.call_args_list) == 0


@freezegun.freeze_time("2019-01-01 12:00:00")
@override_switch('idm.deprive_not_immediately', active=True)
def test_redeprive_role_mail(simple_system, arda_users, idm_robot):
    """Проверка, что в письме на отзыв роли указываем правильного инициатора отзыва"""
    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    add_perms_by_role('responsible', gandalf, simple_system)
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role.refresh_from_db()
    assert role.state == ROLE_STATE.GRANTED
    clear_mailbox()
    deprive_comment = 'Роль больше не нужна'
    role.deprive_or_decline(gandalf, comment=deprive_comment)
    role = refresh(role)
    assert role.state == ROLE_STATE.DEPRIVING_VALIDATION
    assert len(mail.outbox) == 0
    # Запустим повторный отзыв роли от имени робота
    role.deprive_or_decline(depriver=idm_robot, deprive_all=False, force_deprive=True)
    role.refresh_from_db()
    assert role.state == ROLE_STATE.DEPRIVED
    assert role.depriver_id == gandalf.id
    assert role.deprive_comment == deprive_comment
    # Рассылка писем включена только в интранете
    assert len(mail.outbox)
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Simple система. Роль отозвана'
    assert_contains([
        f'{gandalf.get_full_name()} отозвал вашу роль в системе "Simple система":',
        'Роль: Менеджер',
        'Комментарий: Роль больше не нужна',
    ], message.body)
