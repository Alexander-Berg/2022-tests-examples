# -*- coding: utf-8 -*-
import pytest

from balance.utils.passport_sms_api import PassportSmsApi
from balance.utils import get_nested, path as path_util
from balance.utils import string as string_utils

from tests.base import BalanceTest


class TestPassportSmsApi(BalanceTest):
    def setUp(self):
        super(TestPassportSmsApi, self).setUp()
        self.api = PassportSmsApi(self.session)

    def test_send_from_test_env(self):
        """
        Проверяет, что сообщение не будет отправлено, если uid не присутствует
        в списке uid'ов пользователей, которым можно посылать сообщения
        из тестового окружения.
        """
        with self.assertRaises(AssertionError):
            self.api.send_sms("Test message.", uid=-1)


class TestPath(object):
    @pytest.mark.parametrize(
        "url, code, ans",
        [
            (
                "https://balance.yandex.ru/xxx?yy",
                "by",
                "https://balance.yandex.by/xxx?yy",
            ),
            (
                "https://balance.yandex.ru/xxx?yy",
                "tr",
                "https://balance.yandex.com.tr/xxx?yy",
            ),
            (
                "https://balance.yandex.ru/xxx?yy",
                "com.tr",
                "https://balance.yandex.com.tr/xxx?yy",
            ),
            (
                "https://balance.yandex.ru/xxx?yy",
                None,
                "https://balance.yandex.ru/xxx?yy",
            ),
        ],
    )
    def test_fix_domain(self, url, code, ans):
        assert path_util.fix_domain(url, code) == ans


class TestStringSyncType(object):
    @pytest.mark.parametrize("ref_string", [u"Пока", u""])
    @pytest.mark.parametrize("unicode_string", [u"Привет", u""])
    @pytest.mark.parametrize("encoding", ["utf8", "cp1251"])
    def test_str_to_unicode(self, encoding, unicode_string, ref_string):
        string = unicode_string.encode(encoding)
        result = string_utils.sync_type(string, ref_string, encoding)
        assert isinstance(result, unicode)
        assert result == unicode_string

    @pytest.mark.parametrize("ref_string", [u"Пока", u""])
    @pytest.mark.parametrize("string", [u"Привет", u""])
    @pytest.mark.parametrize("encoding", ["utf8", "cp1251"])
    def test_unicode_to_str(self, encoding, string, ref_string):
        string = u"Привет"
        encoded_string = string.encode(encoding)
        ref_string = ref_string.encode(encoding)
        result = string_utils.sync_type(string, ref_string, encoding)
        assert isinstance(result, str)
        assert result == encoded_string

    @pytest.mark.parametrize("string", ["Привет", u"Привет"])
    @pytest.mark.parametrize("ref_string", [None, object()])
    def test_invalid_ref_string(self, ref_string, string):
        assert string_utils.sync_type(string, ref_string) is string

    @pytest.mark.parametrize("string", [None, object()])
    @pytest.mark.parametrize("ref_string", ["Привет", u"Привет"])
    def test_invalid_string(self, string, ref_string):
        assert string_utils.sync_type(string, ref_string) is string

    @pytest.mark.parametrize(
        ["ref_string", "string"], [(u"Привет", u"Пока"), ("Привет", "Пока")]
    )
    def test_same_type_unchanged(self, ref_string, string):
        assert string_utils.sync_type(string, ref_string) is string


@pytest.mark.parametrize(
    "obj,path,result",
    [
        (None, ("a", 1, "z"), None),
        (1, ("a", 1, "z"), None),
        ("a", ("a", 1, "z"), None),
        ("a", (0,), "a"),
        ([], ("a", 1, "z"), None),
        ([], (0,), None),
        ([1, 2, 3], (0,), 1),
        ({}, ("a", 1, "z"), None),
        ({"a": 1}, ("a", 1, "z"), None),
        ({"a": 1}, ("a",), 1),
        ({"a": {"b": {"c": 1}}}, ("a", "b", "c"), 1),
        ({"a": {"b": {"c": 1}}}, ("a", "b"), {"c": 1}),
        ({"a": {"b": {"c": [1, 2, 3]}}}, ("a", "b", "c", 1), 2),
        ({"a": {"b": {"c": [1, {"d": 2}, 3]}}}, ("a", "b", "c", 1, "d"), 2),
    ],
)
def test_get_nested(obj, path, result):
    assert get_nested(obj, *path) == result
