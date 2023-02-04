import mock
import pytest

from balance.person import set_hidden
from tests.balance_tests.person.person_common import create_person, create_client


def test_set_to_true(person):
    person.hidden = False
    set_hidden(person, True)
    assert person.hidden


def test_set_to_false(person):
    person.hidden = True
    set_hidden(person, False)
    assert not person.hidden


@pytest.mark.single_account
@mock.patch('balance.person.single_account.availability.check_person_attributes')
class TestCheckPersonAttributesCalls(object):
    def test_not_called_without_single_account(self, check_person_attributes_mock, person):
        person.hidden = True
        set_hidden(person, False)
        check_person_attributes_mock.assert_not_called()

    def test_not_called_on_hide(self, check_person_attributes_mock, session):
        person = create_person(session, client=create_client(session, with_single_account=True))
        person.hidden = False
        set_hidden(person, True)
        check_person_attributes_mock.assert_not_called()

    def test_not_called_if_unchanged(self, check_person_attributes_mock, session):
        person = create_person(session, client=create_client(session, with_single_account=True))
        person.hidden = False
        set_hidden(person, False)
        check_person_attributes_mock.assert_not_called()

    def test_called(self, check_person_attributes_mock, session):
        person = create_person(session, client=create_client(session, with_single_account=True))
        person.hidden = True
        set_hidden(person, False)
        check_person_attributes_mock.assert_called_once_with(person)
