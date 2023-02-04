from datetime import date
import pytest
from mock import Mock
from staff.person.passport.internal import IntPassport
from staff.lib.testing import StaffFactory


class TestData(object):
    int_passport = None
    bb_answer = None


@pytest.fixture
def test_data():
    result = TestData()
    result.int_passport = IntPassport(uid=1)
    result.bb_answer = {
        'attributes': {
            '34': 'ru',
            '27': 'Иван',
            '29': '1',
            '28': 'Иванов',
            '30': '1984-12-15',
            '33': 'Europe/Moscow',
            '212': 'Ivan',
            '213': 'Ivanov',
        }
    }

    return result


def test_upload_bb_data(test_data):
    test_data.int_passport.for_update = {'foo': 'b'}
    test_data.int_passport.push_to_passport = Mock()

    test_data.int_passport.upload_bb_data()
    test_data.int_passport.push_to_passport.assert_called_once_with({'foo': 'b', 'uid': 1})

    assert test_data.int_passport.for_update == {}


@pytest.mark.django_db
def test_fields_all_equal(test_data):
    test_data.int_passport.load_bb_data = Mock(return_value=test_data.bb_answer)

    fields = {
        'tz': 'Europe/Moscow',
        'birthday': date(1984, 12, 15),
        'first_name': 'Иван',
        'last_name': 'Иванов',
        'first_name_en': 'Ivan',
        'last_name_en': 'Ivanov',
        'lang_ui': 'ru',
        'gender': 'M',
    }

    person = StaffFactory(**fields)

    for field in fields:
        setattr(test_data.int_passport, field, getattr(person, field))

    assert test_data.int_passport.for_update == {}


@pytest.mark.django_db
def test_fields_all_not_equal(test_data):
    test_data.int_passport.load_bb_data = Mock(return_value=test_data.bb_answer)

    fields = {
        'tz': 'Europe/Kiev',
        'birthday': date(2984, 12, 16),
        'first_name': 'Петр',
        'last_name': 'Петров',
        'first_name_en': 'Petr',
        'last_name_en': 'Petrov',
        'lang_ui': 'en',
        'gender': 'F',
    }

    person = StaffFactory(**fields)

    for field in fields:
        setattr(test_data.int_passport, field, getattr(person, field))

    model = {
        'timezone': 'Europe/Kiev',
        'birthday': '2984-12-16',
        'firstname': 'Петр'.encode('utf-8'),
        'lastname': 'Петров'.encode('utf-8'),
        'firstname_global': 'Petr'.encode('utf-8'),
        'lastname_global': 'Petrov'.encode('utf-8'),
        'language': 'en',
        'gender': 'f',
    }

    assert test_data.int_passport.for_update == model
