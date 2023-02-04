from mock import Mock
import pytest
from staff.person.passport.external import ExtPassport


class TestData(object):
    ext_login = None
    int_login = None
    ext_passport = None


@pytest.fixture
def test_data():
    result = TestData()
    result.ext_login = 'ext_login'
    result.int_login = 'int_login'

    ExtPassport.blackbox = Mock(**{'get_uid_by_login.return_value': 1})
    result.ext_passport = ExtPassport(
        ext_login=result.ext_login,
        int_login=result.int_login,
    )

    return result


def test_init(test_data):
    assert test_data.ext_passport.uid == 1
    assert test_data.ext_passport.ext_login == test_data.ext_login
    assert test_data.ext_passport.int_login, test_data.int_login
    test_data.ext_passport.blackbox.get_uid_by_login.assert_called_once_with(test_data.ext_login)


def test_upload_bb_data(test_data):
    test_data.ext_passport.push_to_passport = Mock()
    test_data.ext_passport.for_update = {'foo': {'f': 1}, 'bar': {}}

    test_data.ext_passport.upload_bb_data()
    test_data.ext_passport.push_to_passport.assert_any_call({'f': 1}, service_slug='foo')
    test_data.ext_passport.push_to_passport.assert_any_call({}, service_slug='bar')

    assert test_data.ext_passport.for_update == {}


def test_sid_668_true_in_bb(test_data):
    test_data.ext_passport._bb_data = {'betatest': 1}
    assert test_data.ext_passport.sid_668
    test_data.ext_passport.sid_668 = False
    assert 'betatest' in test_data.ext_passport.for_update
    assert test_data.ext_passport.for_update['betatest'] is None


def test_sid_668_false_in_bb(test_data):
    test_data.ext_passport._bb_data = {'betatest': 0}
    assert not test_data.ext_passport.sid_668
    test_data.ext_passport.sid_668 = True
    assert 'betatest' in test_data.ext_passport.for_update
    assert test_data.ext_passport.for_update['betatest'] == {}


def test_sid_669_true_in_bb(test_data):
    test_data.ext_passport._bb_data = {'yastaff': 1}
    assert test_data.ext_passport.sid_669
    test_data.ext_passport.sid_669 = False
    assert 'yastaff' in test_data.ext_passport.for_update
    assert test_data.ext_passport.for_update['yastaff'] is None


def test_sid_669_false_in_bb(test_data):
    test_data.ext_passport._bb_data = {'yastaff': 0}
    assert not test_data.ext_passport.sid_669
    test_data.ext_passport.sid_669 = True
    assert 'yastaff' in test_data.ext_passport.for_update
    assert test_data.ext_passport.for_update['yastaff'] == {'yastaff_login': test_data.int_login}
