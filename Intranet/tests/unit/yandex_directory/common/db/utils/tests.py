# -*- coding: utf-8 -*-


import testutils

from sqlalchemy.exc import OperationalError

from hamcrest import(
    assert_that,
    calling,
    raises,
    has_entries,
)
import unittest.mock
from unittest.mock import (
    patch,
    Mock,
)

from intranet.yandex_directory.src.yandex_directory.common.exceptions import (
    ReadonlyConnectionError,
    UnknownShardError,
    ReadonlyModeError,
)
from intranet.yandex_directory.src.yandex_directory.common.db import (
    lock,
    get_meta_connection,
    get_main_connection,
    get_raw_main_connection,
    _select_engine,
)

from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
    OrganizationMetaModel,
)


class Test__get_lock(testutils.SimpleTestCase):
    def test_should_get_lock_with_hashed_lock_name(self):
        # проверяем, что контекстный менеджер вызовет
        # пару функций в постгресе для того, чтобы взять и отпустить лок

        mocked_execute_result = Mock()
        mocked_execute_result.fetchone = Mock(return_value=[Mock()])
        patched_connection = Mock()
        patched_connection.execute = Mock(return_value=mocked_execute_result)
        lock_name = 'test_lock'

        with lock(patched_connection, lock_name):
            pass

        patched_connection.execute.assert_has_calls([
            unittest.mock.call('select pg_try_advisory_xact_lock(%(key)s)', {'key': 5658839}),
        ])


# тут наследуемся от testutils.TestCase
# потому что этот тест работает с коннектами к базе
class Test__connections(testutils.TestCase):
    def test_connection_opened_for_read_cant_be_used_for_destructive_operations(self):
        # проверяем, что в юнит-тестах, несмотря на то, что все коннекты устанавливаются
        # к мастеру, если не указан for_write=True, то через коннект нельзя сделать
        # ничего кроме SELECT

        with get_meta_connection() as connection, \
                self.assertRaises(ReadonlyConnectionError):
            connection.execute('DELETE FROM organizations')

        with get_main_connection(shard=1) as connection, \
                self.assertRaises(ReadonlyConnectionError):
            connection.execute('DELETE FROM organizations')

    def test_trying_to_get_connection_to_unknown_shard_raises_exception(self):
        with self.assertRaises(UnknownShardError):
            _select_engine('main', shard=100500, for_write=False)

        with self.assertRaises(UnknownShardError):
            _select_engine('main', shard=None, for_write=False)

    @testutils.override_settings(READ_ONLY_MODE=True)
    def test_get_connection_in_ro_mode(self):
        # в read only режиме при попытке взять соединение на чтение бросаем исключение ReadonlyModeError

        assert_that(
            calling(get_meta_connection).with_args(for_write=True),
            raises(ReadonlyModeError)
        )

        assert_that(
            calling(get_main_connection).with_args(shard=1, for_write=True),
            raises(ReadonlyModeError)
        )

    def test_start_main_connection(self):
        # проверим, что при прерывании транзакции откатываемся назад
        user = self.create_user(nickname='not_changed')
        try:
            with get_main_connection(shard=1, for_write=True) as main_connection:
                UserModel(main_connection)\
                    .filter(id=user['id'])\
                    .update(nickname='changed')
                raise Exception
        except:
            pass
        new_user = UserModel(self.main_connection).get(user_id=user['id'])
        assert_that(
            new_user,
            has_entries(
                nickname='not_changed'
            )
        )

        # проверим, что если не прерываем транзакцию, то не откатываемся назад
        UserModel(self.main_connection)\
            .filter(id=user['id'])\
            .update(nickname='not_changed')
        try:
            with get_main_connection(shard=1, for_write=True) as main_connection:
                UserModel(main_connection)\
                    .filter(id=user['id'])\
                    .update(nickname='changed')
        except:
            pass
        new_user = UserModel(self.main_connection).get(user_id=user['id'])
        assert_that(
            new_user,
            has_entries(
                nickname='changed'
            )
        )

        # проверим запуск без начала транзакции
        UserModel(self.main_connection)\
            .filter(id=user['id'])\
            .update(nickname='not_changed')
        try:
            with get_main_connection(shard=1, for_write=True, no_transaction=True) as main_connection:
                UserModel(main_connection)\
                    .filter(id=user['id'])\
                    .update(nickname='changed')
                raise Exception
        except:
            pass
        new_user = UserModel(self.main_connection).get(user_id=user['id'])
        assert_that(
            new_user,
            has_entries(
                nickname='changed'
            )
        )

    def test_start_meta_connection(self):
        # проверим, что при прерывании транзакции откатываемся назад
        org = OrganizationMetaModel(self.meta_connection).create(label='not_changed', shard=1)
        try:
            with get_meta_connection(for_write=True) as meta_connection:
                OrganizationMetaModel(meta_connection)\
                    .filter(id=org['id'])\
                    .update(label='changed')
                raise Exception
        except:
            pass
        new_org = OrganizationMetaModel(self.meta_connection).get(id=org['id'])
        assert_that(
            new_org,
            has_entries(
                label='not_changed'
            )
        )

        # проверим, что если не прерываем транзакцию, то не откатываемся назад
        OrganizationMetaModel(self.meta_connection) \
            .filter(id=org['id']) \
            .update(label='not_changed')
        try:
            with get_meta_connection(for_write=True) as meta_connection:
                OrganizationMetaModel(meta_connection)\
                    .filter(id=org['id'])\
                    .update(label='changed')
        except:
            pass
        new_org = OrganizationMetaModel(self.meta_connection).get(id=org['id'])
        assert_that(
            new_org,
            has_entries(
                label='changed'
            )
        )

        # проверим запуск без начала транзакции
        OrganizationMetaModel(self.meta_connection) \
            .filter(id=org['id']) \
            .update(label='not_changed')
        try:
            with get_meta_connection(for_write=True, no_transaction=True) as meta_connection:
                OrganizationMetaModel(meta_connection)\
                    .filter(id=org['id'])\
                    .update(label='changed')
                raise Exception
        except:
            pass
        new_org = OrganizationMetaModel(self.meta_connection).get(id=org['id'])
        assert_that(
            new_org,
            has_entries(
                label='changed'
            )
        )


class Test__retry_on_connect_to_server(testutils.SimpleTestCase):
    # ретраим попытку соединения при этой ошибке от pgbouncer
    retry_err_msg = 'pgbouncer cannot connect to server'

    def setUp(self):
        super(Test__retry_on_connect_to_server, self).setUp()

        # эмитируем ошибку от pgbouncer
        class RetryException(OperationalError):
            def __init__(self, *args, **kwargs):
                orig = Exception(Test__retry_on_connect_to_server.retry_err_msg)
                super(RetryException, self).__init__('', '', orig)

        self.RetryException = RetryException

    def test_retry_for_meta(self):
        # ретраим попытку соеденения при соединении к meta базе
        with patch('intranet.yandex_directory.src.yandex_directory.common.db._select_engine', side_effect=self.RetryException) as mc:
            try:
                with get_meta_connection():
                    # первый вызов + три ретрая
                    self.assertEqual(mc.call_count, 4)
            except self.RetryException:
                pass

    def test_retry_for_main(self):
        # ретраим попытку соеденения при соединении к main базе
        with patch('intranet.yandex_directory.src.yandex_directory.common.db._select_engine', side_effect=self.RetryException) as mc:
            try:
                with get_main_connection(shard=1):
                    # первый вызов + три ретрая
                    self.assertEqual(mc.call_count, 4)
            except self.RetryException:
                pass

    def test_retry_for_raw(self):
        # ретраим попытку соеденения при соединении к main в raw режиме
        with patch('intranet.yandex_directory.src.yandex_directory.common.db._select_engine', side_effect=self.RetryException) as mc:
            try:
                get_raw_main_connection(shard=1)
                # первый вызов + три ретрая
                self.assertEqual(mc.call_count, 4)
            except self.RetryException:
                pass
