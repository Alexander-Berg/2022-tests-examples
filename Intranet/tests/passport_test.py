import pytest
from mock import Mock
from staff.person.passport.base import Passport, BBField


class TestData(object):
    bb_answer = None
    foo_field = None
    bar_field = None
    test_passport = None


@pytest.fixture
def test_data():
    result = TestData()

    result.bb_answer = {
        'dbfields': {
            'bar.uid': 100,
            'foo.uid': 200,
        },
        'attributes': {
            '27': 'Ivan',
        },
        'uid': '123',
    }
    result.extracted_bb_answer = {
        'f': 200,
        'b': 100,
        'firstname': 'Ivan',
    }

    result.foo_field = BBField(
        passport_name='f',
        passport_field='foo.uid',
    )

    result.bar_field = BBField(
        passport_name='b',
        passport_field='bar.uid',
    )

    result.firstname = BBField(
        passport_name='firstname',
    )

    class TestPassport(Passport):
        foo = result.foo_field
        bar = result.bar_field
        firstname = result.firstname

    result.test_passport = TestPassport(uid=1)
    return result


def test_init(test_data):
    assert test_data.foo_field.name == 'foo'
    assert test_data.test_passport.uid == 1
    assert test_data.test_passport._bb_data is None
    assert test_data.test_passport.for_update == {}
    assert test_data.test_passport.passport_fields == {'bar.uid': 'b', 'foo.uid': 'f'}
    assert test_data.test_passport.passport_attributes == {27: 'firstname'}


def test_bb_data(test_data):
    test_data.test_passport.load_bb_data = Mock(return_value=test_data.bb_answer)
    assert test_data.test_passport.foo == 200
    assert test_data.test_passport.bar == 100
    assert test_data.test_passport.firstname == 'Ivan'
    test_data.test_passport.load_bb_data.assert_called_once_with()


def test_save(test_data):
    test_data.test_passport.upload_bb_data = Mock()
    test_data.test_passport.save()
    test_data.test_passport.for_update = test_data.extracted_bb_answer
    test_data.test_passport.save()
    test_data.test_passport.upload_bb_data.assert_called_once_with()


def test_load_bb_data(test_data):
    blackbox = Mock(**{'get_user_info.return_value': test_data.bb_answer})
    test_data.test_passport.blackbox = blackbox
    assert test_data.test_passport.load_bb_data() == test_data.bb_answer
    test_data.test_passport.blackbox.get_user_info.assert_called_once_with(
        uid=test_data.test_passport.uid,
        passport_fields=test_data.test_passport.passport_fields,
        attributes=test_data.test_passport.passport_attributes,
    )


def test_extract_bb_data(test_data):
    assert (test_data.test_passport.extract_bb_data(test_data.bb_answer) == test_data.extracted_bb_answer)
