import pytest
from django_idm_api.exceptions import UserNotFound

from dwh.callback.idm import Hooks
from dwh.core.models.user import ROLE_SUPPORT


def test_basic(init_user, client, django_assert_num_queries):

    init_user(robot=True)
    user1 = init_user(is_active=False)
    init_user('dummy')

    hooks = Hooks()

    role_spec = {'role': ROLE_SUPPORT}

    # Добавление роли.
    assert not len(user1.roles)

    with django_assert_num_queries(3) as _:
        # Тут же происходит активация пользователя, если ещё не активен.
        result = hooks.add_role(user1.username, role_spec, {}, 'user')

    user1.refresh_from_db()
    assert result == {'code': 0, 'data': {}}
    assert len(user1.roles) == 1
    assert ROLE_SUPPORT in user1.roles

    # Получение ролей пользователя.
    with django_assert_num_queries(1) as _:
        result = hooks.get_user_roles(user1.username)

    assert result == {'code': 0, 'roles': [{'role': ROLE_SUPPORT}]}

    # Получение ролей всех пользователей.
    with django_assert_num_queries(1) as _:
        result = hooks.get_all_roles()

    assert result == {
        'code': 0,
        'users': [
            {'login': 'robot-dwh-test', 'roles': []},
            {'login': 'tester', 'roles': [{'role': 'support'}]},
            {'login': 'dummy', 'roles': []}
        ]}

    # Удаление роли.
    with django_assert_num_queries(3) as _:
        result = hooks.remove_role(user1.username, role_spec, {}, False, '')

    assert result == {'code': 0}

    user1.refresh_from_db()
    assert not user1.is_active
    assert ROLE_SUPPORT not in user1.roles

    # Проверка, что сокрытая роль не фигурирует в списке ролей.
    assert hooks.get_user_roles(user1.username) == {'code': 0, 'roles': []}
    assert hooks.get_all_roles() == {
        'code': 0,
        'users': [
            {'login': 'robot-dwh-test', 'roles': []},
            {'login': 'tester', 'roles': []},
            {'login': 'dummy', 'roles': []}
        ]}

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
    assert result['roles']['values'] == {'support': {'name': 'Поддержка', 'unique_id': 'support'}}
