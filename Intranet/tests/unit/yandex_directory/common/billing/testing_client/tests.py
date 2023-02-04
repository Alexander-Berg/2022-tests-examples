# -*- coding: utf-8 -*-
from unittest.mock import patch, Mock

from testutils import (
    TestCase,
    override_settings,
)

from hamcrest import (
    assert_that,
    equal_to,
)

from intranet.yandex_directory.src.yandex_directory.common.billing.testing_client import BillingClientWithUidReplacement



class TestBillingClientWithUidReplacement(TestCase):
    def setUp(self, *args, **kwargs):
        with patch('intranet.yandex_directory.src.yandex_directory.common.billing.client.xmlrpc.client'), \
             patch('os.environ.get', return_value='testing'):
            self.client = BillingClientWithUidReplacement(endpoint='endpoint', token='token', manager_uid=1)
        super(TestBillingClientWithUidReplacement, self).setUp(*args, **kwargs)

    def test_assert_in_testing_environment(self):
        # проверим, что инициализация BillingClientWithUidReplacement не кидает ошибку если клиент запущен в тестинге
        with patch('os.environ.get', return_value='testing'), \
             patch('intranet.yandex_directory.src.yandex_directory.common.billing.client.xmlrpc.client'):
            BillingClientWithUidReplacement(endpoint='endpoint', token='token', manager_uid=1)

        # проверим, что инициализация BillingClientWithUidReplacement кидает ошибку если клиент запущен не в тестинге
        with patch('os.environ.get', return_value='not_testing'), \
             patch('intranet.yandex_directory.src.yandex_directory.common.billing.client.xmlrpc.client'), \
             self.assertRaises(RuntimeError):
                BillingClientWithUidReplacement(endpoint='endpoint', token='token', manager_uid=1)

    def test_prepare_uid_with_none_value(self):
        # проверим, что _prepare_uid вернет None если ему передали значение None
        uid = None
        result = self.client._prepare_uid(uid)
        assert_that(result, equal_to(None))

    def test_prepare_uid_with_int_value(self):
        # если проставлена настройка BILLING_CLIENT_UID_FOR_TESTING, нужно заменять uid
        uid = 123
        replace_to_uid = 456
        with override_settings(BILLING_CLIENT_UID_FOR_TESTING=replace_to_uid):
            result = self.client._prepare_uid(uid)
        assert_that(result, equal_to(str(replace_to_uid)))

    def test_prepare_uid_with_int_value(self):
        # если НЕ проставлена настройка BILLING_CLIENT_UID_FOR_TESTING,
        # заменять uid не нужно
        uid = 123
        with override_settings(BILLING_CLIENT_UID_FOR_TESTING=None):
            result = self.client._prepare_uid(uid)
        assert_that(result, equal_to(str(uid)))
