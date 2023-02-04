from bcl.core.models import Role, Action


def test_user_has_permission(init_user):

    # Проверка сочетания прав.
    user = init_user('dummy', [Role.TREASURER, Role.SALARY_GROUP])

    assert user.has_permission(Action.STATEMENT_HANDLE_INTRADAY)
    assert not user.has_permission(Action.STATEMENT_HANDLE_FINAL)
