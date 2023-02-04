# coding: utf-8

from mock import patch
import pytest

from django.db.models import NOT_PROVIDED

from idm.core.constants.role import ROLE_STATE
from idm.core.models import Role, RoleField, RoleNode, UserPassportLogin
from idm.core.role_validators.passport_login_validator import PassportLoginValidator
from idm.core.role_validators.resource_associate_validator import ResourceAssociateValidator
from idm.tests.utils import set_workflow
from idm.users.constants.user import USER_TYPES
from idm.users.models import Organization, User

pytestmark = pytest.mark.django_db


def test_get_passport_login_to_check_awaiting_condition(pt1_system, arda_users, department_structure):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    set_workflow(pt1_system, 'approvers=[]', 'approvers=[]')

    # passport_login_id == NOT_PROVIDED, is_personal_role == True
    # получим паспортный логин, привязанный к членству в группе
    mb_passport_login = UserPassportLogin.objects.create(login='yndx-frodo-fellowship', user=frodo)
    fellowship.memberships.filter(user=frodo).update(passport_login=mb_passport_login)
    group_role = Role.objects.request_role(
        frodo,
        fellowship,
        pt1_system,
        '',
        {'project': 'proj1', 'role': 'admin'},
    )
    role = group_role.refs.get(user=frodo)

    validator = PassportLoginValidator(role)
    login = validator._get_passport_login_to_check_awaiting_condition(passport_login_id=NOT_PROVIDED)
    assert login == mb_passport_login

    # passport_login_id == NOT_PROVIDED, is_personal_role == False
    # получим паспортный логин, привязанный к роли
    role_passport_login = UserPassportLogin.objects.create(login='yndx-frodo', user=frodo)

    role = Role.objects.request_role(
        frodo,
        frodo,
        pt1_system,
        '',
        {'project': 'proj1', 'role': 'admin'},
        {'passport-login': 'yndx-frodo'}
    )

    validator = PassportLoginValidator(role)
    login = validator._get_passport_login_to_check_awaiting_condition(passport_login_id=NOT_PROVIDED)
    assert login == role_passport_login

    # passport_login_id == None
    # получим None
    login = validator._get_passport_login_to_check_awaiting_condition(passport_login_id=None)
    assert login is None

    # passport_login == валидное значение id паспортного логина
    # получим корректный логин
    id = role_passport_login.id
    login = validator._get_passport_login_to_check_awaiting_condition(passport_login_id=id)
    assert login == role_passport_login


def test_try_add_passport_login_to_role(simple_system, arda_users):
    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'})
    passport_login = UserPassportLogin.objects.create(login='yndx-frodo', user=frodo)
    validator = PassportLoginValidator(role)

    # Проверим удачное добавление паспортного логина
    result, comment = validator._try_add_passport_login_to_role(passport_login)
    assert result
    assert comment is None

    # Проверим, что при политике `unique_for_user` второй паспортный логин добавить не получится
    simple_system.passport_policy = 'unique_for_user'
    simple_system.save()
    passport_login = UserPassportLogin.objects.create(login='yndx-frodo-2', user=frodo)
    result, comment = validator._try_add_passport_login_to_role(passport_login)
    assert not result
    assert comment == 'Нарушение политики системы в отношении паспортных логинов'

    # Проверим, что ошибка при выполнении обрабатывается корректно
    with patch.object(simple_system, 'check_passport_policy') as check_passport_policy:
        check_passport_policy.side_effect = Exception()
        result, comment = validator._try_add_passport_login_to_role(passport_login)
    assert not result
    assert comment == 'Возникла ошибка при добавлении паспортного логина к роли'


def test_passport_login_condition(pt1_system, arda_users):
    frodo = arda_users.frodo
    role = Role.objects.request_role(
        frodo,
        frodo,
        pt1_system,
        '',
        {'project': 'proj1', 'role': 'admin'},
        {'passport-login': 'yndx-frodo'}
    )
    validator = PassportLoginValidator(role)

    # Проверим работу функция, если передать passport_login_id=NOT_PROVIDED
    # Все условия выполнены
    role.passport_logins.update(is_fully_registered=True)
    result, comment = validator.check(passport_login_id=NOT_PROVIDED)
    assert result
    assert comment is None

    # Паспортный логин, привязанный к роли недорегестрирован
    role.passport_logins.update(is_fully_registered=False)
    result, comment = validator.check(passport_login_id=NOT_PROVIDED)
    assert not result
    assert comment == 'Требуется дорегистрировать паспортный логин'

    # В fields_data роли отсутсвует поле passport-login, но для роли оно обязательно
    role.fields_data = {}
    role.system_specific = {}
    role.save(update_fields=['fields_data', 'fields_data'])
    role.refresh_from_db()

    result, comment = validator.check(passport_login_id=NOT_PROVIDED)
    assert not result
    assert comment == 'Роль требует указания паспортного логина'

    # Проверим работу функция, если передать passport_login_id=None для роли, которая требует паспортный логин
    result, comment = validator.check(passport_login_id=None)
    assert not result
    assert comment == 'Роль требует указания паспортного логина'

    # Проверим работу функция, если передать в passport_login_id id существующего логина
    passport_login = role.passport_logins.get()
    # Паспортный логин недорегестрирован
    result, comment = validator.check(passport_login_id=passport_login.id)
    assert not result
    assert comment == 'Требуется дорегистрировать паспортный логин'

    # Паспортный логин дорегестрирован
    role.passport_logins.update(is_fully_registered=True)
    result, comment = validator.check(passport_login_id=passport_login.id)
    assert result
    assert comment is None


def test_resource_associate_condition(simple_system, arda_users):
    node = RoleNode.objects.get_node_by_data(simple_system, {'role': 'admin'})
    RoleField.objects.create(node=node, type='charfield', name='Resource Id', slug='resource_id', is_required=True)
    frodo = arda_users.frodo
    organization = Organization.objects.create(org_id=1000000)
    org_user = User.objects.create(username='org_%s' % organization.pk, type=USER_TYPES.ORGANIZATION)
    role = Role.objects.request_role(
        frodo,
        frodo,
        simple_system,
        '',
        {'role': 'admin'},
        {'resource_id': 'some_resource_id'},
        organization_id=organization.id,
    )
    validator = ResourceAssociateValidator(role)

    # Ресур к организации не привязан,
    result, comment = validator.check()
    assert not result
    assert comment == 'Ресурс, на который запрашивается роль, ещё не привязан к организации'

    # Привяжем ресурс к организации, выдав на него любую роль
    org_role = Role.objects.request_role(
        org_user,
        org_user,
        simple_system,
        '',
        {'role': 'admin'},
        {'resource_id': 'some_resource_id'},
        organization_id=organization.id,
    )

    result, comment = validator.check()
    assert result
    assert comment is None

    # Отвяжем ресурс от организации, отозвав роль
    org_role.set_raw_state(ROLE_STATE.DEPRIVED)
    result, comment = validator.check()
    assert not result
    assert comment == 'Ресурс, на который запрашивается роль, ещё не привязан к организации'
