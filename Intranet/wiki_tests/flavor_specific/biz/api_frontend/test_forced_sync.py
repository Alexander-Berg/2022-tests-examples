import pytest
import json
import mock
from django.core.cache import caches
from wiki.api_frontend.logic.forced_sync import check_if_synced

from wiki.sync.connect.tasks.forced_sync import get_cache_key
from wiki.sync.connect.tasks.consts import CACHE_FORCED_SYNC_TIMEOUT, ForcedSyncStatus, ForcedSyncResult
from wiki.sync.connect.tasks.helpers import ForcedSyncData

pytestmark = [pytest.mark.django_db]

CACHE_FORCED_SYNC_BACKEND = caches['forced_sync']


@pytest.fixture(scope='function', autouse=True)
def clear_storage():
    CACHE_FORCED_SYNC_BACKEND.clear()


class TestForcedSync:
    def _post_request(self, client, api_url, mock_func, data, expected_resp_status, expected_resp_data=None):
        with mock.patch('wiki.sync.connect.tasks.forced_sync.ForcedSyncTask.delay', mock_func):
            response = client.post(
                f'{api_url}/.forced_sync',
                data=data,
            )
            assert response.status_code == expected_resp_status
            content = json.loads(response.content)
            if expected_resp_data:
                actual_data = content['data']
                assert actual_data == expected_resp_data
            return content

    @staticmethod
    def _check_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, expected_result):
        cache_key = get_cache_key(dir_org_id, cloud_org_id, user_cloud_uid, user_uid)
        task_result_dict = CACHE_FORCED_SYNC_BACKEND.get(cache_key)
        if expected_result is None:
            print(task_result_dict)
            assert task_result_dict == expected_result
            return
        task_result = ForcedSyncResult.parse_obj(task_result_dict)
        assert task_result.result == expected_result

    @staticmethod
    def _set_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, result: ForcedSyncResult):
        cache_key = get_cache_key(dir_org_id, cloud_org_id, user_cloud_uid, user_uid)
        CACHE_FORCED_SYNC_BACKEND.set(cache_key, result.dict(), CACHE_FORCED_SYNC_TIMEOUT)

    def mock_celery_task_failed_result(
        self, dir_org_id: str, cloud_org_id, user_cloud_uid: str, user_uid: str, user_iam_token: str, *args, **kwargs
    ):
        result = ForcedSyncResult(result=ForcedSyncStatus.FAILED)
        self._set_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, result)

    def mock_celery_task_ok_result(
        self, dir_org_id: str, cloud_org_id, user_cloud_uid: str, user_uid: str, user_iam_token: str, *args, **kwargs
    ):
        result = ForcedSyncResult(result=ForcedSyncStatus.OK, org_id=dir_org_id)
        self._set_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, result)

    def mock_celery_task_in_progress_result(
        self, dir_org_id: str, cloud_org_id, user_cloud_uid: str, user_uid: str, user_iam_token: str, *args, **kwargs
    ):
        result = ForcedSyncResult(result=ForcedSyncStatus.IN_PROGRESS)
        self._set_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, result)

    def test_invalid_params(self, client, api_url, wiki_users, organizations):
        client.login('thasonic')

        self._post_request(
            client=client,
            api_url=api_url,
            mock_func=self.mock_celery_task_failed_result,
            data={
                'cloud_uid': 'dfasdfasdfa',
                'uid': '121212121',
            },
            expected_resp_status=409,
        )

        self._post_request(
            client=client,
            api_url=api_url,
            mock_func=self.mock_celery_task_failed_result,
            data={
                'org_id': organizations.org_21.dir_id,
            },
            expected_resp_status=409,
        )

        self._post_request(
            client=client,
            api_url=api_url,
            mock_func=self.mock_celery_task_failed_result,
            data={
                'org_id': organizations.org_21.dir_id,
                'cloud_uid': '',
            },
            expected_resp_status=409,
        )

        self._post_request(
            client=client,
            api_url=api_url,
            mock_func=self.mock_celery_task_failed_result,
            data={
                'org_id': organizations.org_21.dir_id,
                'cloud_uid': '',
                'uid': '123123123',
            },
            expected_resp_status=409,
        )

        self._post_request(
            client=client,
            api_url=api_url,
            mock_func=self.mock_celery_task_failed_result,
            data={
                'org_id': organizations.org_21.dir_id,
                'cloud_uid': 'wtwertwert',
                'uid': '',
            },
            expected_resp_status=409,
        )

    def test_existing_user(self, client, api_url, wiki_users, organizations):
        """
        Проверить, что при вызове принудительного синка для существующего пользователя с реальной организацией
        таска синка не вызывается, а в ответе на запрос возвращается результат OK.
        """
        client.login('thasonic')
        chapson = wiki_users.chapson
        org = chapson.orgs.all()[0]

        self._post_request(
            client=client,
            api_url=api_url,
            mock_func=self.mock_celery_task_failed_result,
            data={
                'org_id': org.dir_id,
                'uid': chapson.staff.uid,
            },
            expected_resp_status=200,
            expected_resp_data={'error_code': None, 'result': 'ok', 'org_id': org.dir_id},
        )

    def test_task_failed_result(self, client, api_url, wiki_users, organizations):
        """
        Приверить, что таска, падающая с ошибкой, записывает в кеш результат FAILED, а повторный запрос возващает
        результат из кеша без повторного перевызова таски.
        """
        client.login('thasonic')

        dir_org_id = organizations.org_21.dir_id
        cloud_org_id = ''
        user_cloud_uid = ''
        user_uid = 'xxx'

        self._check_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, None)

        self._post_request(
            client=client,
            api_url=api_url,
            mock_func=self.mock_celery_task_failed_result,
            data={
                'org_id': dir_org_id,
                'uid': user_uid,
            },
            expected_resp_status=200,
            expected_resp_data={'error_code': None, 'result': 'in_progress', 'org_id': None},
        )

        self._check_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, ForcedSyncStatus.FAILED)

        self._post_request(
            client=client,
            api_url=api_url,
            mock_func=self.mock_celery_task_ok_result,
            data={
                'org_id': dir_org_id,
                'uid': user_uid,
            },
            expected_resp_status=200,
            expected_resp_data={'error_code': None, 'result': 'failed', 'org_id': None},
        )

        # в кеше должен остаться результат FAILED, потому что таска mock_celery_task_ok_result повторно не вызывалась
        self._check_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, ForcedSyncStatus.FAILED)

    def test_task_ok_result(self, client, api_url, wiki_users, organizations):
        """
        Приверить, что таска, выполненная успешно, записывает в кеш результат OK, а повторный запрос возващает
        результат из кеша без повторного перевызова таски.
        """
        client.login('thasonic')

        dir_org_id = organizations.org_21.dir_id
        cloud_org_id = ''
        user_cloud_uid = '222'
        user_uid = '111'

        self._check_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, None)

        self._post_request(
            client=client,
            api_url=api_url,
            mock_func=self.mock_celery_task_ok_result,
            data={
                'org_id': dir_org_id,
                'cloud_uid': user_cloud_uid,
                'uid': user_uid,
            },
            expected_resp_status=200,
            expected_resp_data={'error_code': None, 'result': 'in_progress', 'org_id': None},
        )

        self._check_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, ForcedSyncStatus.OK)

        self._post_request(
            client=client,
            api_url=api_url,
            mock_func=self.mock_celery_task_failed_result,
            data={
                'org_id': dir_org_id,
                'cloud_uid': user_cloud_uid,
                'uid': user_uid,
            },
            expected_resp_status=200,
            expected_resp_data={'error_code': None, 'result': 'in_progress', 'org_id': None},
        )

        # в кеше должен остаться результат OK, но так как пользователь незасинкан, будет IN_PROGRESS
        self._check_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, ForcedSyncStatus.FAILED)

    def test_task_in_progress_result(self, client, api_url, wiki_users, organizations):
        """
        Приверить, что таска, выполняющая sync, записывает в кеш результат in_progress, а повторный запрос возващает
        результат из кеша без повторного перевызова таски.
        """
        client.login('thasonic')

        dir_org_id = organizations.org_21.dir_id
        cloud_org_id = ''
        user_cloud_uid = '333'
        user_uid = '1000'

        self._check_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, None)

        self._post_request(
            client=client,
            api_url=api_url,
            mock_func=self.mock_celery_task_in_progress_result,
            data={
                'org_id': dir_org_id,
                'cloud_uid': user_cloud_uid,
                'uid': user_uid,
            },
            expected_resp_status=200,
            expected_resp_data={'error_code': None, 'result': 'in_progress', 'org_id': None},
        )

        self._check_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, ForcedSyncStatus.IN_PROGRESS)

        self._post_request(
            client=client,
            api_url=api_url,
            mock_func=self.mock_celery_task_failed_result,
            data={
                'org_id': dir_org_id,
                'cloud_uid': user_cloud_uid,
                'uid': user_uid,
            },
            expected_resp_status=200,
            expected_resp_data={'error_code': None, 'result': 'in_progress', 'org_id': None},
        )

        # в кеше должен остаться результат in_progress,
        # потому что таска mock_celery_task_failed_result повторно не вызывалась
        self._check_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, ForcedSyncStatus.IN_PROGRESS)

        # таска как бы завершила свою работу успешно.
        result = ForcedSyncResult(result=ForcedSyncStatus.OK)
        self._set_cache_result(dir_org_id, cloud_org_id, user_cloud_uid, user_uid, result)

        # замокаем так что все проверки пользователя зеленые -
        with mock.patch('wiki.api_frontend.logic.forced_sync.check_if_synced', return_value=True):
            # проверим, что вернет ручка на этот раз
            self._post_request(
                client=client,
                api_url=api_url,
                mock_func=self.mock_celery_task_failed_result,
                data={
                    'org_id': dir_org_id,
                    'cloud_uid': user_cloud_uid,
                    'uid': user_uid,
                },
                expected_resp_status=200,
                expected_resp_data={'error_code': None, 'result': 'ok', 'org_id': dir_org_id},
            )

    def test_check_if_synced(self, client, api_url, wiki_users, organizations):
        def _check_if_synced(dir_org_id: str, cloud_org_id: str, user_cloud_uid: str, user_uid: str):
            data = ForcedSyncData(
                dir_org_id=dir_org_id,
                cloud_org_id=cloud_org_id,
                user_cloud_uid=user_cloud_uid,
                user_uid=user_uid,
                user_iam_token='',
            )
            return check_if_synced(data)

        chapson_uid, chapson_cloud_uid = wiki_users.chapson.get_uid(), wiki_users.chapson.get_cloud_uid()
        connect_org = {'id': organizations.org_42.dir_id}
        with mock.patch('wiki.api_frontend.logic.forced_sync.get_user_orgs_from_connect', return_value=[connect_org]):
            assert _check_if_synced(organizations.org_42.dir_id, '', chapson_cloud_uid, chapson_uid)
            assert _check_if_synced(organizations.org_42.dir_id, '', chapson_cloud_uid, '')
            assert _check_if_synced(organizations.org_42.dir_id, '', '', chapson_uid)
            assert _check_if_synced('', '', chapson_cloud_uid, chapson_uid)
            assert _check_if_synced('', '', chapson_cloud_uid, '')
            assert _check_if_synced('', '', '', chapson_uid)

            assert not _check_if_synced(organizations.org_21.dir_id, '', '', chapson_uid)
            assert not _check_if_synced(organizations.org_42.dir_id, '', '', 'invalid')

        with mock.patch('wiki.api_frontend.logic.forced_sync.get_user_orgs_from_connect', return_value=[]):
            assert not _check_if_synced('', '', '', chapson_uid)

        organizations.org_42.cloud_id = 'cloud_id'
        organizations.org_42.save()

        with mock.patch('wiki.api_frontend.logic.forced_sync.get_user_orgs_from_connect', return_value=[]):
            assert _check_if_synced('', organizations.org_42.cloud_id, chapson_cloud_uid, chapson_uid)
