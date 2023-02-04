from mock import Mock, patch

from staff.preprofile.utils import get_uid_from_passport


@patch('staff.preprofile.utils.blackbox.userinfo', Mock(return_value={'uid': 1234, 'name': 'uhura'}))
def test_get_uid_from_passport_not_none():
    assert get_uid_from_passport('uhura') == 1234


@patch('staff.preprofile.utils.blackbox.userinfo', Mock(return_value={'name': 'spock'}))
def test_get_uid_from_passport_none():
    assert get_uid_from_passport('spock') is None
