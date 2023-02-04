import pytest

from django.test import TestCase
from django.http import HttpRequest

from intranet.audit.src.users.models import User, StatedPerson
from intranet.audit.src.users.dao.user import get_user_by_request, check_stated_persons_existence


@pytest.fixture
def stated_person_one(db):
    return StatedPerson.objects.create(
        id='123_GHFTHFJGJ',
        uid='123',
        login='test',
        first_name='test name',
        last_name='test last name',
        position='slave',
        department='cleaning service',
        department_slug='clean_service',
    )


@pytest.fixture
def stated_person_two(db):
    return StatedPerson.objects.create(
        id='124_GHFTHFJGJFD',
        uid='124',
        login='boss login',
        first_name='test boss name',
        last_name='test boss last name',
        position='big boss',
        department='cleaning service',
        department_slug='clean_service',
    )


@pytest.mark.usefixtures("dummy_yauser")
class GetUserTestCase(TestCase):
    def setUp(self):
        self.request = HttpRequest()
        self.request.yauser = self.dummy_yauser

    def test_get_user_exists(self):
        User.objects.create(uid=self.dummy_yauser.uid, login=self.dummy_yauser.login,
                            last_name=self.dummy_yauser.last_name,
                            first_name=self.dummy_yauser.first_name)
        user = get_user_by_request(self.request)
        self.assertEqual(User.objects.count(), 1)
        self.assertEqual(user.uid, self.dummy_yauser.uid)
        self.assertEqual(user.login, self.dummy_yauser.login)

    def test_get_user_not_exists_not_save(self):
        user = get_user_by_request(self.request)
        self.assertEqual(User.objects.count(), 0)
        self.assertEqual(user.uid, self.dummy_yauser.uid)
        self.assertEqual(user.login, self.dummy_yauser.login)


def test_check_stated_persons_existence_not_exists(stated_person_one, stated_person_two):
    person = StatedPerson(
        uid='123',
        login='test',
        first_name='test name',
        last_name='test last name',
        position='rebel',
        department='cleaning service',
        department_slug='clean_service',
    )
    stated_persons, to_create = check_stated_persons_existence([person])
    assert len(stated_persons) == 0
    assert len(to_create) == 1
    assert to_create[0].position == person.position


def test_check_stated_persons_existence_exists_two(stated_person_one, stated_person_two):
    person = stated_person_one
    person1 = stated_person_two
    stated_persons, to_create = check_stated_persons_existence([person, person1])
    assert len(stated_persons) == 2
    assert stated_persons[person.uid].position == stated_person_one.position
    assert stated_persons[person1.uid].position == stated_person_two.position
    assert len(to_create) == 0


def test_check_stated_persons_existence_exists_one(stated_person_one, stated_person_two):
    person = stated_person_two
    person1 = StatedPerson(
        uid='123',
        login='test',
        first_name='test name',
        last_name='test last name',
        position='rebel',
        department='cleaning service',
        department_slug='clean_service',
    )
    stated_persons, to_create = check_stated_persons_existence([person, person1])
    assert len(stated_persons) == 1
    assert stated_persons[person.uid].position == stated_person_two.position
    assert stated_persons[person.uid].last_name == stated_person_two.last_name
    assert len(to_create) == 1
    assert to_create[0].position == person1.position
    assert to_create[0].last_name == person1.last_name
