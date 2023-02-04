import pytest

from intranet.dogma.dogma.core.dao.users import get_users_other_emails
from intranet.dogma.dogma.core.models import User
from intranet.dogma.dogma.core.logic.users import EmailGuesser
from ...utils import test_vcr


@pytest.fixture
def users(transactional_db,):
    users_list = list()
    users_list.append(
        User.objects.create(
            uid='12345',
            login='vsem_privet',
            email='test@ya.ru',
            name='Someone',
            other_emails='test@ya.ru,anotheremail@test.com'
    ))
    users_list.append(
        User.objects.create(
            uid='1120000000016772',
            login='smosker',
            email='smosker@ya.ru',
            name='Anotherone',
            other_emails='smosker@ya.ru,email@email.com'
        )
    )
    return users_list


def test_get_users_other_emails(users):
    result = list(get_users_other_emails([user.login for user in users]))
    vsem_privet_emails = ['test@ya.ru', 'anotheremail@test.com']
    smosker_emails = ['smosker@ya.ru', 'email@email.com']
    assert result == vsem_privet_emails + smosker_emails or result == smosker_emails + vsem_privet_emails


def test_user_from_staff_update(users):
    guesser = EmailGuesser()
    assert User.objects.count() == 2
    existed_user = User.objects.get(uid='1120000000016772')
    assert existed_user.email == 'smosker@ya.ru'
    with test_vcr.use_cassette('staff_success_response.yaml'):
        user, trusted = guesser.guess_user_by('smosker@gmail.com')

    assert User.objects.count() == 2
    assert user == existed_user
    assert trusted is True
    assert user.email == 'smosker@yandex-team.ru'


def test_user_from_staff_create():
    guesser = EmailGuesser()
    assert User.objects.count() == 0
    with test_vcr.use_cassette('staff_success_response.yaml'):
        user, trusted = guesser.guess_user_by('smosker@gmail.com')
    assert trusted is True
    assert User.objects.count() == 1
    user = User.objects.first()
    assert user.email == 'smosker@yandex-team.ru'
    assert user.uid == '1120000000016772'
    assert user.from_staff is True


def test_user_from_staff_fail_error():
    guesser = EmailGuesser()
    assert User.objects.count() == 0
    with test_vcr.use_cassette('staff_fail_response.yaml'):
        user, trusted = guesser.guess_user_by('smosker@gmail.com')
    assert trusted is False
    assert user is None
    assert User.objects.count() == 0


def test_user_from_staff_fail_no_match():
    guesser = EmailGuesser()
    assert User.objects.count() == 0
    with test_vcr.use_cassette('staff_blank_response.yaml'):
        user, trusted = guesser.guess_user_by('smosker@gmail.com')
    assert trusted is False
    assert user is None
    assert User.objects.count() == 0
