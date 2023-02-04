from mock import MagicMock

from staff.person_profile.permissions.check_permissions import can_view_digital_sign


def test_can_view_digital_sign_is_superuser():
    perm = MagicMock()
    perm.is_superuser = True
    target_login = 'test_login'

    assert can_view_digital_sign(None, perm, target_login)


def test_can_view_digital_sign_is_owner():
    perm = MagicMock()
    perm.is_superuser = False
    perm.is_owner = MagicMock(return_value=True)
    target_login = 'test_login'

    assert can_view_digital_sign(None, perm, target_login)
    perm.is_owner.assert_called_once_with(target_login)


def test_can_view_digital_sign_can_view_digital_sign():
    perm = MagicMock()
    perm.is_superuser = False
    perm.is_owner = MagicMock(return_value=False)
    perm.permissions = ['test1', 'django_intranet_stuff.can_view_digital_sign', 'test2']
    target_login = 'test_login'

    assert can_view_digital_sign(None, perm, target_login)
    perm.is_owner.assert_called_once_with(target_login)


def test_can_view_digital_sign_no_permissions():
    perm = MagicMock()
    perm.is_superuser = False
    perm.is_owner = MagicMock(return_value=False)
    perm.permissions = ['test1', 'django_intranet_stuff.can_view_digital_sign2', 'test2']
    target_login = 'test_login'

    assert not can_view_digital_sign(None, perm, target_login)
    perm.is_owner.assert_called_once_with(target_login)
