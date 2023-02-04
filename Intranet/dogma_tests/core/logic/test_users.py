# coding: utf-8


import contextlib

import pytest
from pretend import stub
from mock import patch

from intranet.dogma.dogma.core.logic.users import EmailGuesser


pytestmark = pytest.mark.django_db(transaction=True)


@contextlib.contextmanager
def with_guesser(stub):
    guesser = EmailGuesser()
    setattr(guesser, 'all_users', stub)
    yield guesser


def test_email_guesser_guesses():

    with patch('intranet.dogma.dogma.core.logic.users.get_user_data_by_email_from_staff') as get_user_mock:
        get_user_mock.return_value = None

        all_users = [stub(login='', email='chapson@yandex-team.ru')]
        with with_guesser(all_users) as guesser:
            user, is_trusted_result = guesser._guess_user_by('chapson@yandex-team.ru')
            assert user.email == 'chapson@yandex-team.ru'
            assert is_trusted_result

        all_users = [stub(login='', email='something', other_emails_parsed={'chapson@yandex-team.ru'})]
        with with_guesser(all_users) as guesser:
            user, is_trusted_result = guesser._guess_user_by('chapson@yandex-team.ru')
            assert user.email == 'something'
            assert is_trusted_result

        all_users = [stub(login='chapson', email='something', other_emails_parsed=[])]
        with with_guesser(all_users) as guesser:
            user, is_trusted_result = guesser._guess_user_by('chapson@yandex-team.ru')
            assert user is None

        all_users = [stub(other_emails_usernames=['chapson'], login='', email='something', other_emails_parsed=[])]
        with with_guesser(all_users) as guesser:
            user, is_trusted_result = guesser._guess_user_by('chapson@yandex-team.ru')
            assert user is None

        all_users = [stub(login='', email='chapson@yandex-team.ru')]
        with with_guesser(all_users) as guesser:
            user, is_trusted_result = guesser.guess_user_by('CHAPSON@yandex-team.ru')
            assert user.email == 'chapson@yandex-team.ru'
            assert is_trusted_result

        all_users = [stub(login='', email='something', other_emails_parsed={'chapson@yandex-team.ru'})]
        with with_guesser(all_users) as guesser:
            user, is_trusted_result = guesser.guess_user_by('CHAPSON@yandex-team.ru')
            assert user.email == 'something'
            assert is_trusted_result

        all_users = [stub(login='chapson', email='something', other_emails_parsed=[])]
        with with_guesser(all_users) as guesser:
            user, is_trusted_result = guesser.guess_user_by('CHAPSON@yandex-team.ru')
            assert user is None

        all_users = [stub(other_emails_usernames=['chapson'], login='', email='something', other_emails_parsed=[])]
        with with_guesser(all_users) as guesser:
            user, is_trusted_result = guesser.guess_user_by('CHAPSON@yandex-team.ru')
            assert user is None
