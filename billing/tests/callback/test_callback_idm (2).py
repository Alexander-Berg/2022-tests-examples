import pytest
from django_idm_api.exceptions import UserNotFound

from mdh.callback.idm import Hooks
from mdh.core.models import ReaderRole, Audit


def test_basic(init_user, client, django_assert_num_queries):

    user = init_user(robot=True)
    user1 = init_user(is_active=False)
    user2 = init_user('dummy')

    hooks = Hooks()

    role_spec = {'role': ReaderRole.alias}

    # Добавление роли.
    assert not user1.roles.count()

    with django_assert_num_queries(10) as _:
        # Тут же происходит активация пользователя, если ещё не активен.
        result = hooks.add_role(user1.username, role_spec, {}, 'user')

    assert result == {'code': 0, 'data': {}}
    assert user1.roles.count() == 1
    assert user1.roles.first().active
    assert user1.check_roles([ReaderRole])

    # Получение ролей пользователя.
    with django_assert_num_queries(2) as _:
        result = hooks.get_user_roles(user1.username)

    assert result == {'code': 0, 'roles': [{'role': ReaderRole.alias}]}

    # Получение ролей всех пользователей.
    with django_assert_num_queries(2) as _:
        result = hooks.get_all_roles()

    assert result == {
        'code': 0,
        'users': [
            {'login': 'robot-mdh-test', 'roles': []},
            {'login': 'tester', 'roles': [{'role': ReaderRole.alias}]},
            {'login': 'dummy', 'roles': []}
        ]}

    # Удаление (сокрытие) роли.
    with django_assert_num_queries(9) as _:
        result = hooks.remove_role(user1.username, role_spec, {}, False, '')

    assert result == {'code': 0}

    user1.refresh_from_db()
    assert not user1.is_active
    assert not user1.roles.first().active
    assert not user1.check_roles([ReaderRole])

    # Проверка, что сокрытая роль не фигурирует в списке ролей.
    assert hooks.get_user_roles(user1.username) == {'code': 0, 'roles': []}
    assert hooks.get_all_roles() == {
        'code': 0,
        'users': [
            {'login': 'robot-mdh-test', 'roles': []},
            {'login': 'tester', 'roles': []},
            {'login': 'dummy', 'roles': []}
        ]}

    # Восстановление ранее удалённой роли.
    result = hooks.add_role(user1.username, role_spec, {}, 'user')
    assert result == {'code': 0, 'data': {}}
    assert user1.roles.count() == 1
    assert user1.roles.first().active

    assert Audit.objects.count() == 3  # Создание + удаление + восставновление

    # Добавляем роль и создаём пользователя.
    result = hooks.add_role('newuser', role_spec, {}, 'user')
    assert result == {'code': 0, 'data': {}}

    # Запрашиваем роль неизвестного пользователя.
    with pytest.raises(UserNotFound):
        hooks.get_user_roles('bogus')


def test_info():

    hooks = Hooks()
    result = hooks.info()

    assert result['code'] == 0
    assert result['roles']['values'] == {
        'batcher': {'name': 'Batch contributor', 'unique_id': 'batcher'},
        'contributor': {'name': 'Contributor', 'unique_id': 'contributor'},
        'governing': {'name': 'Governing', 'unique_id': 'governing'},
        'reader': {'name': 'Reader', 'unique_id': 'reader'},
        'support': {'name': 'Support', 'unique_id': 'support'}
    }
