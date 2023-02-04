# -*- coding: utf-8 -*


import pytest
from django.core import mail
from django.utils import timezone
from django.utils.encoding import force_text
from mock import patch

from idm.core.constants.role import ROLE_STATE
from idm.core.constants.rolefield import FIELD_TYPE
from idm.core.workflow.exceptions import (RoleAlreadyExistsError, Forbidden, AccessDenied, WorkflowError,
                                          DataValidationError,
                                          RoleNodeDoesNotExist)
from idm.core.exceptions import MultiplePassportLoginUsersError, RoleStateSwitchError
from idm.core.models import Role, RoleRequest, RoleField, ApproveRequest, Action, UserPassportLogin
from idm.core.plugins.errors import PluginFatalError
from idm.notification.models import Notice
from idm.tests.utils import (create_user, refresh, assert_contains, compare_time,
                             set_workflow, add_perms_by_role, assert_action_chain, add_members, raw_make_role)

pytestmark = pytest.mark.django_db


def test_simple_role_request(users_for_test, simple_system):
    """В простом случае всё должно работать"""
    art, fantom, terran, admin = users_for_test
    add_perms_by_role('responsible', art, simple_system)
    Role.objects.request_role(art, fantom, simple_system, '', {'role': 'manager'}, fields_data={})
    assert Role.objects.count() == 1


def test_request_workflow(users_for_test, simple_system):
    """записываем в реквест workflow"""
    art, fantom, terran, admin = users_for_test
    Role.objects.request_role(art, art, simple_system, '', {'role': 'manager'}, fields_data={})
    request = Role.objects.get().requests.get()
    assert request.workflow_id == simple_system.actual_workflow_id


def test_unknown_role_request(users_for_test, simple_system):
    """Если роли нет в дереве ролей, то роль не должна быть выдана"""
    art, fantom, terran, admin = users_for_test
    add_perms_by_role('responsible', art, simple_system)
    with pytest.raises(RoleNodeDoesNotExist):
        Role.objects.request_role(art, fantom, simple_system, '', {'role': 'manatee'}, {})


def test_request_role_for_nonleaf_role_node(arda_users, complex_system):
    """Если роль есть в дереве ролей, но не является листом, то запросить роль должно быть нельзя"""
    frodo = arda_users.frodo
    RoleField.objects.update(is_required=False)
    with pytest.raises(Forbidden) as excinfo:
        Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'rules'}, {})
    assert force_text(excinfo.value) == 'Роль не может быть запрошена на данный узел дерева ролей'


def test_request_role_for_system_without_workflow(users_for_test, simple_system):
    """Если нет workflow, то роль не должна быть выдана"""
    art, fantom, terran, admin = users_for_test
    set_workflow(simple_system, '\n\n\n\n\n')
    add_perms_by_role('responsible', art, simple_system)
    with pytest.raises(WorkflowError):
        Role.objects.request_role(art, fantom, simple_system, '', {'role': 'manager'}, None)


def test_request_role_for_system_not_providing_required_fields(users_for_test, simple_system):
    """Проверяем, что нельзя запросить роль, не предоставив поля, помеченные как required.
    В то же время поля, не помеченные required, должно быть можно не предоставлять.
    """
    art, fantom, terran, admin = users_for_test
    passport_login = simple_system.root_role_node.get_children().get().fields.get(slug='passport-login')
    passport_login.is_required = False
    passport_login.save()
    add_perms_by_role('responsible', art, simple_system)
    Role.objects.request_role(art, fantom, simple_system, comment='',
                              data={'role': 'manager'}, fields_data={})
    assert Role.objects.count() == 1
    # поменяем required на True
    passport_login.is_required = True
    passport_login.save()
    with pytest.raises(DataValidationError):
        Role.objects.request_role(art, fantom, simple_system, comment='', data={'role': 'superuser'},
                                  fields_data={'login': 'fantom'})


def test_request_role_providing_null_unrequired_fields(arda_users, simple_system):
    """Проверяем, что если значение необязательного поля равно None, то оно выпадает из fields_data"""

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, {'passport-login': None,
                                                                                            'login': 'something'})
    role = refresh(role)
    assert role.fields_data == {'login': 'something'}


def test_fail_user_role(generic_system):
    """Проверяем отправку письма о том, что роль не удалось выдать, запросившему и пользователю"""
    art = create_user('art')
    admin = create_user('admin', superuser=True)

    assert art.roles.count() == 0
    assert list(admin.actions.all()) == []
    assert len(mail.outbox) == 0
    assert Notice.objects.count() == 0
    assert RoleRequest.objects.count() == 0

    with patch.object(generic_system.plugin.__class__, 'add_role') as add_role:
        add_role.side_effect = PluginFatalError(1, 'blah minor', {'a': 'b'})

        Role.objects.request_role(admin, art, generic_system, '', {'role': 'manager'}, None)

    art = refresh(art)
    assert art.roles.count() == 1
    role = art.roles.get()
    assert role.state == 'failed'
    # Проверим, что добавился role action
    last_action = role.actions.order_by('-id')[0]
    assert last_action.action == 'fail'
    assert 'PluginFatalError:: code=1, message=&quot;blah minor&quot;', last_action.error

    # пользователь и запросивший роль должны получить письма о неудаче
    assert len(mail.outbox) == 2
    requester_message, owner_message = mail.outbox
    assert requester_message.subject == (
        'Произошла ошибка при добавлении запрошенной вами в системе "Generic система" роли'
    )
    assert requester_message.to == ['admin@example.yandex.ru']
    assert_contains((
        'При добавлении роли для art в систему "Generic система" произошла ошибка:',
        'Роль: Менеджер',
        'Ошибка: PluginFatalError: code=1, message="blah minor", data={\'a\': \'b\'}, answer="None"',
    ), requester_message.body)
    assert owner_message.subject == 'Ошибка при добавлении роли в систему "Generic система"'
    assert owner_message.to == ['art@example.yandex.ru']
    assert_contains((
        'При добавлении роли для art в систему "Generic система" произошла ошибка:',
        'Роль: Менеджер',
        'Ошибка: PluginFatalError: code=1, message="blah minor", data={\'a\': \'b\'}, answer="None"'
    ), owner_message.body)
    # должны создаться уведомления
    assert Notice.objects.count() == 2
    notice_art = Notice.objects.get(recipient=art)
    assert notice_art.recipient == art
    assert notice_art.subject == 'Ошибка при добавлении роли в систему "Generic система"'
    assert not notice_art.is_seen
    assert_contains((
        'При добавлении роли для art в систему "Generic система" произошла ошибка',
        'Роль: Менеджер',
        'Ошибка: PluginFatalError: code=1, message="blah minor", data={\'a\': \'b\'}, answer="None"'
    ), notice_art.message)
    notice_2 = Notice.objects.get(recipient=admin)
    assert notice_2.recipient == admin
    assert notice_2.subject == 'Произошла ошибка при добавлении запрошенной вами в системе "Generic система" роли'
    assert not notice_2.is_seen
    assert_contains((
        'При добавлении роли для art в систему "Generic система" произошла ошибка:',
        'Роль: Менеджер',
        'Ошибка: PluginFatalError: code=1, message="blah minor", data={\'a\': \'b\'}, answer="None"'
    ), notice_2.message)
    assert role.requests.count() == 1
    assert role.requests.get().approves.count() == 0  # тк нет аппруверов при запросе роли


def test_request_role_with_passport_login(pt1_system):
    terran = create_user('terran')
    admin = create_user('admin', superuser=True)

    assert terran.roles.count() == 0
    assert terran.passport_logins.count() == 0
    assert len(mail.outbox) == 0

    Role.objects.request_role(admin, terran, pt1_system, '', {'project': 'proj1', 'role': 'admin'},
                              {'passport-login': 'yndx.terran.admin'})

    terran = refresh(terran)
    assert terran.roles.count() == 1
    assert terran.passport_logins.count() == 1
    assert terran.passport_logins.all()[0].login == 'yndx.terran.admin'

    role = terran.roles.select_related('node').get()
    assert not role.is_active
    assert role.state == 'awaiting'
    assert role.node.value_path == '/proj1/admin/'
    assert role.fields_data == {'passport-login': 'yndx.terran.admin'}
    assert len(mail.outbox) == 0

    terran.passport_logins.update(is_fully_registered=True)
    Role.objects.poke_awaiting_roles()
    role = refresh(role)

    assert role.is_active
    assert role.state == 'granted'
    assert compare_time(timezone.now(), role.granted_at, epsilon=2)
    assert len(mail.outbox) == 1

    assert_contains(
        (
            'Test1 система', 'Проект: Проект 1, Роль: Админ',
            'yndx.terran.admin',
        ),
        mail.outbox[0].body
    )

    # теперь снова добавим роль на тот же паспортный логин
    Role.objects.request_role(admin, terran, pt1_system, '', {'project': 'proj1', 'role': 'manager'},
                              {'passport-login': 'yndx.terran.admin'})

    terran = refresh(terran)
    assert terran.roles.count() == 2
    assert terran.passport_logins.count() == 1
    assert terran.passport_logins.get().login == 'yndx.terran.admin'
    new_role = terran.roles.exclude(pk=role.pk).get()
    assert new_role.is_active
    assert new_role.state == 'granted'
    assert new_role.fields_data == {'passport-login': 'yndx.terran.admin'}
    assert len(mail.outbox) == 2
    assert_contains(
        (
            'Test1 система',
            'Проект: Проект 1, Роль: Менеджер',
            'В Паспорте был заведен новый логин: yndx.terran.admin',
        ),
        mail.outbox[1].body
    )


def test_request_role_with_existed_passport_login(pt1_system, monkeypatch):
    monkeypatch.setattr('idm.sync.passport.exists', lambda *args, **kwargs: True)

    terran = create_user('terran')
    admin = create_user('admin', superuser=True)
    set_workflow(pt1_system, 'approvers = []')

    assert terran.passport_logins.count() == 0

    Role.objects.request_role(
        admin, terran, pt1_system, '',
        {'project': 'proj1', 'role': 'admin'},
        {'passport-login': 'yndx.terran.admin'}
    )

    terran = refresh(terran)
    assert terran.roles.count() == 1
    assert terran.passport_logins.count() == 0

    role = terran.roles.get()
    assert not role.is_active
    assert role.state == ROLE_STATE.IDM_ERROR

    action = Action.objects.first()
    assert 'Попытка использовать логин, неизвестный IDM. Выберите другой логин.' in action.error


def test_request_two_roles_with_same_passport_login(generic_system, arda_users):
    generic_system.passport_policy = 'unique_for_user'
    generic_system.save()

    frodo = arda_users['frodo']
    gandalf = arda_users['gandalf']
    set_workflow(generic_system, 'approvers = [approver("gandalf")]')

    role1 = Role.objects.request_role(
        frodo, frodo, generic_system, '',
        {'role': 'admin'},
        {'passport-login': 'yndx.frodo.admin'}
    )

    role2 = Role.objects.request_role(
        frodo, frodo, generic_system, '',
        {'role': 'manager'},
        {'passport-login': 'yndx.frodo.manager'}
    )

    assert frodo.passport_logins.count() == 0

    with patch.object(generic_system.plugin.__class__, '_send_data') as send_data:
        send_data.return_value = {
            'code': 1,
            'roles': {
                'slug': 'role',
                'name': 'Role',
                'values': {
                    'admin': 'Admin'
                }
            }
        }
        ApproveRequest.objects.select_related_for_set_decided().get(approve__role_request__role=role1).set_approved(gandalf)
        role1 = refresh(role1)
        assert role1.state == 'awaiting'
        frodo.passport_logins.update(is_fully_registered=True)
        Role.objects.poke_awaiting_roles()
        role1 = refresh(role1)
        assert role1.state == 'granted'

    with patch.object(generic_system.plugin.__class__, '_send_data') as send_data:
        send_data.return_value = {
            'code': 1,
            'roles': {
                'slug': 'role',
                'name': 'Role',
                'values': {
                    'manager': 'Manager'
                }
            }
        }
        ApproveRequest.objects.select_related_for_set_decided().get(approve__role_request__role=role2).set_approved(gandalf)

    role2 = refresh(role2)
    assert role2.state == ROLE_STATE.IDM_ERROR

    action = Action.objects.filter(role=role2).order_by('-id')[0]
    assert action.error == 'В связи с политикой системы "Generic система" в отношении паспортных логинов ' \
                           'у пользователя frodo не может быть второго паспортного логина'


def test_add_user_role_view_access_denied(simple_system, arda_users):
    """Проверяем запрет добавления роли пользователю в workflow"""
    frodo = arda_users.frodo
    set_workflow(simple_system, 'raise AccessDenied("user has no permissions")')

    with pytest.raises(AccessDenied):
        Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    # роль пользователь запросить не смог
    assert frodo.roles.count() == 0


def test_request_role_for_subordinate(simple_system, arda_users, department_structure):
    """Тестируем добавление роли начальником подчиненному"""

    frodo = arda_users.frodo
    varda = arda_users.varda
    fellowship = department_structure.fellowship

    # сейчас начальник добавит роль не подчинённому
    with pytest.raises(Forbidden):
        Role.objects.request_role(frodo, varda, simple_system, '', {'role': 'admin'}, None)

    assert len(mail.outbox) == 0
    assert varda.roles.count() == 0

    # теперь сделаем varda подчинённым и повторим
    add_members(fellowship, [varda])
    varda = refresh(varda)

    role = Role.objects.request_role(frodo, varda, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)

    # пользователь должен получить от нас письмо что добавлена роль
    varda = refresh(varda)
    assert len(mail.outbox) == 1
    assert varda.roles.count() == 1
    assert role.state == 'granted'

    # руководитель должен увидеть роль подчинённого, которую он запросил
    roles = Role.objects.permitted_for(frodo)
    assert roles.count() == 1
    assert roles.get() == role

    # он также должен видеть и роли подчинённых, которые запросил не он
    role2 = Role.objects.request_role(varda, varda, simple_system, '', {'role': 'manager'}, None)
    role2 = refresh(role2)
    roles = Role.objects.permitted_for(frodo).order_by('pk')
    assert roles.count() == 2
    assert list(roles) == [role, role2]


def test_request_by_deputy(simple_system, arda_users, department_structure):
    """Тестируем запрос роли заместителем"""

    sam = arda_users.sam
    varda = arda_users.varda
    fellowship = department_structure.fellowship

    # заместитель не может запросить роль для не подчинённого
    with pytest.raises(Forbidden):
        Role.objects.request_role(sam, varda, simple_system, '', {'role': 'admin'}, None)

    assert len(mail.outbox) == 0
    assert varda.roles.count() == 0

    add_members(fellowship, [varda])
    varda = refresh(varda)

    role = Role.objects.request_role(sam, varda, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)

    # пользователь должен получить от нас письмо что добавлена роль
    varda = refresh(varda)
    assert len(mail.outbox) == 1
    assert varda.roles.count() == 1
    assert role.state == 'granted'

    # заместитель должен увидеть роль подчинённого, которую он запросил
    roles = Role.objects.permitted_for(sam)
    assert roles.count() == 1
    assert roles.get() == role

    # он также должен видеть и роли подчинённых, которые запросил не он
    role2 = Role.objects.request_role(varda, varda, simple_system, '', {'role': 'manager'}, None)
    role2 = refresh(role2)
    roles = Role.objects.permitted_for(sam).order_by('pk')
    assert roles.count() == 2
    assert list(roles) == [role, role2]


@pytest.mark.parametrize('subject_username', ['frodo', 'tvm_app'])
def test_request_role_with_limited_rights_in_system(complex_system, arda_users, department_structure, subject_username):
    """Проверим, что человек с ограниченными правами в системе может запрашивать роли для других, но только в рамках
    наложенных ограничений"""

    complex_system.use_tvm_role = True
    complex_system.save(update_fields=['use_tvm_role'])
    subject = arda_users[subject_username]
    legolas = arda_users.legolas
    RoleField.objects.all().delete()
    add_perms_by_role('roles_manage', legolas, complex_system, scope='/rules/')
    role = Role.objects.request_role(legolas, subject, complex_system, '', {'project': 'rules', 'role': 'invisic'}, None)
    role = refresh(role)
    assert role.state == 'granted'
    with pytest.raises(Forbidden):
        Role.objects.request_role(legolas, subject, complex_system, '', {'project': 'subs', 'role': 'manager'}, None)


def test_request_by_team_member(complex_system, arda_users, department_structure):
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    RoleField.objects.all().delete()
    with pytest.raises(Forbidden):
        Role.objects.request_role(legolas, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, None)

    add_perms_by_role('users_view', legolas, system=complex_system)

    role = Role.objects.request_role(legolas, frodo, complex_system, '', {'project': 'subs', 'role': 'manager'}, None)
    role = refresh(role)
    assert role.state == 'granted'


def test_can_request_role_for_subordinate_irregardless_of_limited_rights(complex_system, arda_users,
                                                                         department_structure):
    """Проверим, что если у сотрудника имеются ограниченные права в системе, то запросить для подчинённого всё равно
    можно любую роль"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    RoleField.objects.all().delete()
    add_perms_by_role('roles_manage', frodo, complex_system, scope='/rules/')
    role1 = Role.objects.request_role(frodo, legolas, complex_system, '', {'project': 'rules', 'role': 'invisic'}, None)
    role1 = refresh(role1)
    assert role1.state == 'granted'
    role2 = Role.objects.request_role(frodo, legolas, complex_system, '', {'project': 'subs', 'role': 'manager'}, None)
    role2 = refresh(role2)
    assert role2.state == 'granted'


def test_request_role_with_request_policy(simple_system, arda_users, department_structure):
    """Тестируем добавление роли с учетом request_policy"""

    # сейчас запрашиваем роль не подчиненному
    with pytest.raises(Forbidden):
        Role.objects.request_role(arda_users.frodo, arda_users.varda, simple_system, '', {'role': 'admin'}, None)

    assert len(mail.outbox) == 0
    assert arda_users.sauron.roles.count() == 0

    # меняем политику на разрешение запрашивать для всех
    simple_system.request_policy = 'anyone'
    simple_system.save()

    role = Role.objects.request_role(arda_users.frodo, arda_users.varda, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)

    # пользователь должен получить от нас письмо что добавлена роль
    assert len(mail.outbox) == 1
    assert arda_users.varda.roles.count() == 1
    assert role.state == 'granted'


def test_add_user_role_view_with_fields(simple_system, users_for_test):
    (art, fantom, terran, admin) = users_for_test

    assert art.roles.count() == 0
    assert list(admin.actions.all()) == []
    assert len(mail.outbox) == 0

    # проверим передачу дополнителных полей
    role = Role.objects.request_role(admin, art, simple_system, '', {'role': 'admin'}, {'login': 'terran'})
    role = refresh(role)

    # пользователь должен получить от нас письмо что добавлена роль
    assert len(mail.outbox) == 1
    assert admin.actions.count() == 0
    assert admin.requested.count() == 1
    assert admin.requested.get().action == 'request'
    assert art.roles.count() == 1
    assert role.state == 'granted'
    assert role.node.data == {'role': 'admin'}
    assert role.fields_data == {'login': 'terran'}

    # дополнителное поле необязательное, попробуем без него
    role = Role.objects.request_role(admin, art, simple_system, '', {'role': 'manager'}, None)
    role = refresh(role)

    assert len(mail.outbox) == 2
    assert admin.requested.count() == 2
    assert admin.requested.order_by('-added', '-pk').first().action == 'request'
    assert art.roles.count() == 2
    assert role.state == 'granted'
    assert role.node.data == {'role': 'manager'}
    assert role.fields_data is None


def test_switch_state_need_request_to_depriving_through_expire(simple_system, users_for_test):
    """Проверяем переключение состояния роли из need_request в depriving в случае истечения даты протухания"""
    (art, fantom, terran, admin) = users_for_test

    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, None)

    role = refresh(role)
    assert role.state == 'granted'

    role.set_state('need_request')
    role = refresh(role)
    assert role.state == 'need_request'
    assert role.expire_at is not None

    role.set_state('depriving', transition='expire')
    role = refresh(role)
    assert role.state == 'deprived'
    assert role.expire_at is None
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'ask_rerequest', 'expire', 'first_remove_role_push', 'remove',
    ])


def test_switch_state_need_request_to_depriving_through_deprive(simple_system, arda_users):
    """Проверяем переключение состояния роли из need_request в depriving в случае явного отзыва роли"""

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    role = refresh(role)
    assert role.state == 'granted'

    role.set_state('need_request')
    role = refresh(role)
    assert role.state == 'need_request'
    assert role.expire_at is not None

    role.set_state('depriving', transition='deprive')
    role = refresh(role)
    assert role.state == 'deprived'
    assert role.expire_at is None
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'ask_rerequest', 'deprive', 'first_remove_role_push', 'remove',
    ])


def test_add_user_role_twice_with_different_fields_data(simple_system, arda_users):
    """Сейчас мы позволяем добавлять две одинаковые роли, c разными fields_data"""

    frodo = arda_users.frodo
    legolas = arda_users.frodo
    role1 = Role.objects.request_role(frodo, legolas, simple_system, '', {'role': 'admin'}, {'login': 'foo'})
    role2 = Role.objects.request_role(frodo, legolas, simple_system, '', {'role': 'admin'}, {'login': 'bar'})

    actions = frodo.requested.order_by('id')
    assert actions.count() == 2
    assert legolas.actions.count() == 8
    assert list(actions.values_list('action', flat=True)) == ['request', 'request']

    assert_action_chain(role1, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])
    assert_action_chain(role2, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])

    role1 = refresh(role1)
    role2 = refresh(role2)

    assert role1.state == 'granted'
    assert role1.fields_data == {'login': 'foo'}

    assert role2.state == 'granted'
    assert role2.fields_data == {'login': 'bar'}


def test_request_role_twice_with_different_null_fields_data(simple_system, users_for_test):
    """Если fields_data не задается, то вторая роль не может быть добавлена
    """
    (art, fantom, terran, admin) = users_for_test

    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, None)

    with pytest.raises(RoleAlreadyExistsError):
        Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, None)
    # попробуем эквивалентный, но тоже пустой fields_data
    with pytest.raises(RoleAlreadyExistsError) as excinfo:
        Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, {})
    # важно. проверяем, что роль в состоянии "выдана""
    expected = (
        'У пользователя "Легат Аврелий" уже есть такая роль (Роль: Админ) '
        'в системе "Simple система" в состоянии "Выдана"'
    )
    assert str(excinfo.value) == expected

    actions = admin.requested.all()
    assert actions.count() == 1
    assert actions.get().action == 'request'

    assert Role.objects.count() == 1
    assert RoleRequest.objects.count() == 1  # и запрос роли только один
    role = refresh(role)
    assert role.state == 'granted'
    assert role.system_specific is None


def test_non_unicode_in_task_exception(simple_system, users_for_test):
    """Проверяем обработку не unicode строк в сообщениях об ошибках (например от системы)"""
    (art, fantom, terran, admin) = users_for_test
    terran.passport_logins.create(login='yndx.terran.admin', state='created', is_fully_registered=True)

    assert terran.roles.count() == 0

    with patch.object(simple_system.plugin.__class__, 'add_role', autospec=True) as add_role:
        add_role.side_effect = RuntimeError(force_text('WROMG SEND DATA: грусть-печаль'))

        Role.objects.request_role(admin, terran, simple_system, None, {'role': 'admin'},
                                  {'passport-login': 'yndx.terran.admin'})

    terran = refresh(terran)
    assert terran.roles.count() == 1
    role = terran.roles.get()
    assert role.state == ROLE_STATE.IDM_ERROR  # state will approved if encode error
    assert not role.is_active


def test_empty_passport_login(simple_system, arda_users):
    """Если паспортный логин не передан, то в fields_data не должно оставаться его следов"""

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, {'passport-login': ''})
    role.refresh_from_db()
    assert role.fields_data is None


def test_no_blackbox_call_if_no_passport_login_field(generic_system, arda_users):
    """Проверим, что если у системы нет поля "паспортный логин" или оно не обязательное и не передано,
    или оно не обязательное и передано пустое, то обращения в blackbox не происходит"""

    frodo = arda_users.frodo

    def assert_bb_is_not_touched(fields_data):
        Role.objects.all().delete()
        with patch('idm.sync.passport.exists') as exists:
            exists.side_effect = Exception('Could not touch BB!')
            with patch.object(generic_system.plugin.__class__, '_post_data') as post_data:
                post_data.return_value = {
                    'code': 0
                }
                role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'admin'}, fields_data)
        role = refresh(role)
        assert role.state == 'granted'

    passport_login_field = RoleField.objects.get(type=FIELD_TYPE.PASSPORT_LOGIN,
                                                 node__system=generic_system)
    passport_login_field.is_required = False
    passport_login_field.save()

    assert_bb_is_not_touched(None)
    assert_bb_is_not_touched({})
    assert_bb_is_not_touched({'passport-login': ''})

    passport_login_field.delete()

    assert_bb_is_not_touched(None)
    assert_bb_is_not_touched({})
    assert_bb_is_not_touched({'passport-login': ''})


def test_do_not_save_passwords(arda_users, generic_system):
    """Не храним отданные системой пароли, а также меняем точки на тире в паспортных логинах"""

    frodo = arda_users.frodo
    with patch.object(generic_system.plugin.__class__, '_send_data') as send_data:
        send_data.return_value = {
            'code': 0,
            'data': {
                'passport-login': 'yndx.frodo',
                'password': 'wow',
                'unrelated': 'foo',
            }
        }
        role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.system_specific == {'passport-login': 'yndx-frodo', 'unrelated': 'foo'}


def test_wrong_role_state_switch(simple_system, arda_users):
    """Проверка обработки запрещённого перехода"""

    frodo = arda_users.frodo
    role = raw_make_role(frodo, simple_system, {'role': 'manager'}, state='requested')

    with pytest.raises(RoleStateSwitchError):
        role.set_state('depriving')

    role = refresh(role)

    # Статус роли все — "requested", т.к. прямого перехода "requested" -> "deprived",
    # а флаг from_any_state не передан
    assert role.state == 'requested'
    # Проверим, что добавился role action
    last_action = role.actions.order_by('-id').first()
    assert last_action.action == 'fail'


def test_request_role_with_passport_login_occupied_by_another_user(pt1_system, arda_users):
    frodo = arda_users.frodo
    sam = arda_users.sam
    UserPassportLogin.objects.create(login='yndx-frodo', user=frodo)

    Role.objects.request_role(
        frodo, frodo, pt1_system, '',
        {'project': 'proj1', 'role': 'admin'},
        {'passport-login': 'yndx-frodo'}
    )

    with pytest.raises(MultiplePassportLoginUsersError) as err:
        Role.objects.request_role(
            sam, sam, pt1_system, '',
            {'project': 'proj1', 'role': 'admin'},
            {'passport-login': 'yndx-frodo'}
        )

    expected_message = (
        'Role /proj1/admin/ for user sam on passport_login yndx-frodo can not be issued, '
        'passport_login belongs to user frodo'
    )

    assert str(err.value) == expected_message
    assert Role.objects.count() == 1
    role = Role.objects.get()
    assert role.user_id == frodo.id
    assert role.state == 'awaiting'


def test_role_request_with_additional_fields(users_for_test, simple_system):
    art, fantom, terran, admin = users_for_test
    add_perms_by_role('responsible', art, simple_system)
    Role.objects.request_role(art, fantom, simple_system, '', {'role': 'manager'},
                              with_inheritance=False,
                              with_external=False,
                              with_robots=False,
                              without_hold=True,)
    role = Role.objects.get()
    assert role.with_inheritance is False
    assert role.with_external is False
    assert role.with_robots is False
    assert role.without_hold is True


def test_apply_workflow_action(arda_users, simple_system):
    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = ["frodo"]; email_cc = ["x@yandex-team.ru"]')
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'})
    apply_workflow_action = role.actions.get(action='apply_workflow')
    assert apply_workflow_action.comment == (
        'Подтверждающие: [approver(frodo, priority=1)]\n\nРассылки:\ngranted: x@yandex-team.ru'
    )


def test_workflow_comment(arda_users, simple_system):
    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = []; workflow_comment="comment"')
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'})
    workflow_comment_action = role.actions.get(action='workflow_comment')
    assert workflow_comment_action.comment == 'comment'
