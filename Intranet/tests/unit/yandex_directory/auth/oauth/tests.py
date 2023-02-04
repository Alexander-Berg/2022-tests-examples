# -*- coding: utf-8 -*-
import tempfile
from unittest.mock import patch

from testutils import (
    TestCase,
    override_settings,
)
from hamcrest import (
    assert_that,
    equal_to,
)

from intranet.yandex_directory.src.yandex_directory.auth.scopes import (
    scope,
    get_oauth_service_data,
    _oauth_service_data,
    check_scopes, ScopeCheckMethod,
)


class Test_check_permissions(TestCase):

    def test_any_scope(self):
        # должно быть хоть одно из запрашиваемых прав
        result = check_scopes(
            [scope.read_users, scope.write_users, scope.read_actions],  # имеющиеся права
            [scope.read_users, scope.read_actions]  # запрашиваемые права
        )
        assert_that(result, equal_to(True))

    def test_no_any_scope(self):
        # нет ни одного доступного права из необходимых
        assert_that(
            check_scopes(
                [scope.read_users, scope.write_users, scope.read_actions],   # имеющиеся права
                [scope.read_departments]  # запрашиваемые права
            ),
            equal_to(False)
        )

    def test_not_all_scopes(self):
        # есть не все доступные права из необходимых
        assert_that(
            check_scopes(
                [scope.read_users, scope.write_users, scope.read_actions],  # имеющиеся права
                [scope.read_users, scope.work_with_any_organization],  # запрашиваемые права
                method=ScopeCheckMethod.AND
            ),
            equal_to(False)
        )

    def test_all_scopes(self):
        # есть все доступные права из необходимых
        assert_that(
            check_scopes(
                [scope.read_users, scope.work_with_any_organization, scope.write_users, scope.read_actions],  # имеющиеся права
                [scope.read_users, scope.work_with_any_organization],  # запрашиваемые права
                method=ScopeCheckMethod.AND
            ),
            equal_to(True)
        )


class Test_get_oauth_service_data(TestCase):

    def test_no_env_service_oauth_data_file(self):
        # не задана переменная окружения SERVICE_OAUTH_DATA_FILE

        some_client_id = 'dkjfhjk3asdfjhjhfghdjgahjsdgf hagh '
        with patch.dict('intranet.yandex_directory.src.yandex_directory.auth.scopes._oauth_service_data', {}):
            assert_that(
                get_oauth_service_data(some_client_id),
                equal_to((None, None, ))  # нет данных для client_id
            )

    def test_already_cached_data(self):
        # есть закэшированные данные

        has_info_client_id = 'have_cached_data'
        hasnot_info_client_id = 'not_have_cached_data'
        with patch.dict('intranet.yandex_directory.src.yandex_directory.auth.scopes._oauth_service_data', {has_info_client_id: (1, 2)}):
            assert_that(
                get_oauth_service_data(has_info_client_id),
                equal_to((1, 2,))  # есть данные для client_id
            )

            assert_that(
                get_oauth_service_data(hasnot_info_client_id),
                equal_to((None, None,))  # нет данных для client_id
            )

    def test_read_from_file(self):
        # кэшируем даные из файла

        client_id = 'client_id'
        internal_client_id = 'internal_client_id'
        secret = 'secret'

        client_id2 = 'client_id2'
        internal_client_id2 = 'internal_client_id2'
        secret2 = 'secret2'

        # содержимое файла соответствия client_id -> (internal_client_id, secret)
        s = '{} {} {}\nsome text\n'.format(client_id, internal_client_id, secret)
        s += '{} {} {}\nsome text some text'.format(client_id2, internal_client_id2, secret2)

        with tempfile.NamedTemporaryFile() as fp:
            fp.write(s.encode('utf-8'))
            fp.seek(0)
            with override_settings(SERVICE_OAUTH_DATA_FILE=fp.name):
                # получили ожидаемые данные
                assert_that(
                    get_oauth_service_data(client_id),
                    equal_to((internal_client_id, secret,))
                )
                # данные закэшировались
                assert_that(
                    _oauth_service_data,
                    equal_to({
                        client_id: (internal_client_id, secret,),
                        client_id2: (internal_client_id2, secret2,),
                    })
                )
