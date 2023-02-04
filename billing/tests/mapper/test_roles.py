from .utils import user_add_role_with_permission_constraint


def test_has_access_with_empty_constrain(mongomock, user):
    user_add_role_with_permission_constraint(user, 'perm', 'write', None)
    assert not user.has_access('perm', 'write', service=1)
    assert not user.has_access('perm', 'write', service=4)
    assert not user.has_access('perm', 'write', service=4, user=user.uid)
    assert not user.has_access('perm', 'write', user=user.uid)
    assert not user.has_access('perm', 'write')


def test_has_access_with_null_constrain(mongomock, user):
    user_add_role_with_permission_constraint(user, 'perm', 'write', {'service': None, 'user': None})
    assert not user.has_access('perm', 'write', service=1)
    assert not user.has_access('perm', 'write', service=4)
    assert not user.has_access('perm', 'write', service=4, user=user.uid)
    assert not user.has_access('perm', 'write', user=user.uid)
    assert not user.has_access('perm', 'write')


def test_has_access_with_list_constrain(mongomock, user):
    user_add_role_with_permission_constraint(user, 'perm', 'write', {'service': [1, 2, 3]})
    assert user.has_access('perm', 'write', service=1)
    assert not user.has_access('perm', 'write', service=4)
    assert not user.has_access('perm', 'write')


def test_has_access_with_mask_constrain(mongomock, user):
    user_add_role_with_permission_constraint(user, 'perm', 'write', {'service': '*'})
    assert user.has_access('perm', 'write', service=1)
    assert user.has_access('perm', 'write', service=4)
    assert not user.has_access('perm', 'write')


def test_has_access_with_broadcast_constrain(mongomock, user, user_manager):
    user_add_role_with_permission_constraint(user, 'perm', 'write', '*')
    assert user.has_access('perm', 'write', service=1)
    assert user.has_access('perm', 'write', service=4)
    assert user.has_access('perm', 'write', service=4, user=user.uid)
    assert user.has_access('perm', 'write', service=4, user=user_manager.uid)
    assert user.has_access('perm', 'write', user=user_manager.uid)
    assert user.has_access('perm', 'write')


def test_has_access_with_user_self_constrain(mongomock, user, user_manager):
    user_add_role_with_permission_constraint(user, 'perm', 'write', {'service': '*', 'user': 'self'})
    assert not user.has_access('perm', 'write', service=1)
    assert user.has_access('perm', 'write', service=4, user=user.uid)
    assert not user.has_access('perm', 'write', service=4, user=user_manager.uid)
    assert not user.has_access('perm', 'write')


def test_has_access_with_user_broadcast_constrain(mongomock, user, user_manager):
    user_add_role_with_permission_constraint(user, 'perm', 'write', {'user': '*'})
    assert user.has_access('perm', 'write', user=user.uid)
    assert user.has_access('perm', 'write', user=user_manager.uid)
    assert not user.has_access('perm', 'write')
