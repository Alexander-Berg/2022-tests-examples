import pytest

from intranet.audit.src.users.logic.user import already_created, _get_check_key, _prepare_stated_persons
from intranet.audit.src.users.models import StatedPerson


@pytest.fixture
def stated_person():
    return StatedPerson(
        id='123',
        uid='123',
        position='test',
        department='dep',
        department_slug='dep_slug',
        last_name='last name'
    )


def test_already_created_true():
    assert already_created('12312_GGHGHG') is True


def test_already_created_false():
    assert already_created('12312') is False


def test__get_check_key(stated_person):
    attrs_to_check = ('uid', 'position', 'department_slug',
                      'department', 'last_name',
                      )
    assert _get_check_key(stated_person, attrs_to_check) == '123_test_dep_slug_dep_last name'


def test__prepare_stated_persons(stated_person):
    _prepare_stated_persons(stated_person)
    assert stated_person.id.startswith(stated_person.uid)
    assert '_' in stated_person.id
