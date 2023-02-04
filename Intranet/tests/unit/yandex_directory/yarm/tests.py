# coding: utf-8

from hamcrest import (
    assert_that,
    equal_to,
    not_,
    calling,
    raises,
)
from unittest.mock import patch


from testutils import TestCase


from intranet.yandex_directory.src.yandex_directory.core.yarm.exceptions import (
    YarmError,
    YarmLoginError,
)
from intranet.yandex_directory.src.yandex_directory.core.yarm import (
    raise_if_error,
    get_suid_or_raise,
    MAIL_SID,
)


class TestRaiseIfError(TestCase):

    def test_should_raise_if_error(self):
        # если есть ошибка кинем исключение
        err_resp = {'error': {'reason': 'some reason'}}
        assert_that(
            calling(raise_if_error).with_args(err_resp),
            raises(YarmError)
        )

    def test_should_raise_login_error(self):
        # если есть ошибка login error кинем соответствующее исключение
        err_resp = {'error': {'reason': 'login error'}}
        assert_that(
            calling(raise_if_error).with_args(err_resp),
            raises(YarmLoginError)
        )

    def test_should_not_raise_if_no_error(self):
        # если есть нет ошибки, то не кидаем исключение
        err_resp = {'body': 'succedd'}
        assert_that(
            calling(raise_if_error).with_args(err_resp),
            not_(
                raises(YarmError)
            )
        )


class TestGetSuidOrRaise(TestCase):

    def test_raise_error_if_no_suid(self):
        # если для uid нет почтового suid то кинем ошибку
        any_uid = 123
        with patch('intranet.yandex_directory.src.yandex_directory.core.yarm.get_suid_from_blackbox_for_uid', return_value=None):
            assert_that(
                calling(get_suid_or_raise).with_args(uid=any_uid),
                raises(RuntimeError)
            )

    def test_success_get_suid(self):
        # возвращаем почтовый suid если он есть
        any_uid = 123
        any_suid = 321
        with patch('intranet.yandex_directory.src.yandex_directory.core.yarm.get_suid_from_blackbox_for_uid', return_value=any_suid) as mocked_get:
            assert_that(
                get_suid_or_raise(uid=any_uid),
                equal_to(any_suid)
            )
            mocked_get.assert_called_once_with(any_uid, MAIL_SID)





