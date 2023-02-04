import pytest
from mock import patch, Mock

from pretend import stub

from ..permissions.base import BlockBase
from ..constants import TARGET_TYPES


DUMMY_LOGIN = 'dummy'


@pytest.mark.parametrize('target_type', TARGET_TYPES._choices)
def test_block_for_superuser(target_type):
    block = BlockBase(
        show_on=TARGET_TYPES._choices,
        chief=True,
        owner=True,
    )
    superuser_property = stub(
        is_superuser=True,
        get_target_type=lambda *a, **kw: target_type,
        readonly=False,
    )
    assert block.is_available(
        properties=superuser_property,
        target_login=DUMMY_LOGIN,
    )


@pytest.mark.parametrize('target_type', TARGET_TYPES._choices)
@pytest.mark.parametrize('anketa_type', TARGET_TYPES._choices)
def test_block_show_on(target_type, anketa_type):
    block = BlockBase(
        show_on=(target_type,),
        chief=True,
        owner=True
    )

    property = stub(
        is_superuser=True,
        get_target_type=lambda *a, **kw: anketa_type,
        readonly=False,
    )
    is_available = block.is_available(
        properties=property,
        target_login=DUMMY_LOGIN,
    )
    assert is_available == (anketa_type == target_type)


def test_checkers():
    block = BlockBase(
        show_on=TARGET_TYPES._choices,
        chief=True,
        owner=True
    )
    assert block.check_owner in block.checkers
    assert block.check_permission not in block.checkers
    assert block._check_chief in block.checkers
    assert block._check_custom_rule not in block.checkers

    block = BlockBase(
        show_on=TARGET_TYPES._choices,
        owner=True,
        permission='block_permission_string',
    )
    assert block._check_chief not in block.checkers
    assert block.check_owner in block.checkers
    assert block.check_permission in block.checkers
    assert block._check_custom_rule not in block.checkers

    block = BlockBase(
        show_on=TARGET_TYPES._choices,
        waffle='waffle_key',
        permission='block_permission_string',
        custom_rule=lambda a, b: False
    )
    assert block._check_chief not in block.checkers
    assert block.check_owner not in block.checkers
    assert block.check_permission in block.checkers
    assert block._check_custom_rule in block.checkers


@patch('staff.person_profile.permissions.utils.view403', new=Mock())
@pytest.mark.parametrize('is_available', [True, False])
def test_observer_has_perm(is_available):
    from staff.person_profile.permissions.utils import observer_has_perm, view403

    block_reg = Mock(is_available=Mock(return_value=is_available))
    perm_ctl = Mock(return_value=block_reg)
    request = Mock()
    observer_user = object()
    request.user = Mock(get_profile=Mock(return_value=observer_user))
    request.service_is_readonly = False
    login = 'login'
    perm = 'perm'

    with patch('staff.person_profile.permissions.utils.get_block_registry', new=perm_ctl):
        @observer_has_perm(perm)
        def view(request, login):
            return 'result'

        result = view(request, login)

        if is_available:
            assert result == 'result'
        else:
            view403.assert_called_once_with(request, login)

    perm_ctl.assert_called_once_with(
        observer=observer_user,
        login_or_logins=login,
        service_readonly=False
    )
    block_reg.is_available.assert_called_once_with(perm, login)
