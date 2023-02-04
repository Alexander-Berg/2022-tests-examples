# encoding: utf-8
from __future__ import unicode_literals

import os
import shutil
import time

import freezegun
import mock
import pytest
import tornado.gen
import tornado.web
from tornado.httpclient import HTTPError

from intranet.webauth.lib import settings
from intranet.webauth.lib.monitorings.cache_monitorings import (
    CheckCacheIntegrityHandler,
    CheckCacheFreshnessHandler,
)
from intranet.webauth.lib.monitorings.qloud_sync_monitorings import CheckQloudSyncHandler


@pytest.fixture
def app():
    application = tornado.web.Application([
        (r'/check-cache-integrity', CheckCacheIntegrityHandler),
        (r'/check-cache-freshness', CheckCacheFreshnessHandler),
        (r'/check-qloud-sync', CheckQloudSyncHandler),
    ])
    return application


@tornado.gen.coroutine
def mocked_get_cache_version():
    raise tornado.gen.Return('1')


@tornado.gen.coroutine
def mocked_get_version_timestamp(version):
    assert version == '1'
    raise tornado.gen.Return(time.time() - 60)


@tornado.gen.coroutine
def mocked_get_version_old_timestamp(version):
    assert version == '1'
    raise tornado.gen.Return(time.time() - 100500)


@pytest.mark.parametrize('path', [r'/check-cache-integrity', r'/check-cache-freshness'])
@pytest.mark.gen_test
def test_cache_monitorings(http_client, base_url, path):
    with mock.patch('intranet.webauth.lib.monitorings.cache_monitorings.get_cache_version') as version_mock, \
         mock.patch('intranet.webauth.lib.monitorings.cache_monitorings.get_version_timestamp') as timestamp_mock, \
         mock.patch('intranet.webauth.lib.generate_idm_cache.get_webauth_idm_systems') as get_webauth_idm_systems:
        version_mock.side_effect = mocked_get_cache_version
        timestamp_mock.side_effect = mocked_get_version_timestamp
        get_webauth_idm_systems.return_value = ['system1', 'system2']
        # Очистим директорию, если прошлый тест упал
        try:
            shutil.rmtree(settings.DEV_PATH_PREFIX)
        except:
            pass
        # Проверим, что мониторинги вернут 500, если директория не существует
        with pytest.raises(HTTPError) as err:
            response = yield http_client.fetch(base_url + path)
        assert err.value.code == 500
        # Создадим директорию и проверим, что мониторинги вернут 500, т.к. отсутствуют файлы кеша
        os.makedirs(settings.WEBAUTH_IDM_CACHE_FOLDER, mode=0755)
        with pytest.raises(HTTPError) as err:
            response = yield http_client.fetch(base_url + path)
        assert err.value.code == 500
        # Создадим файлы кеша и проверим, что мониторинг на полноту кеша вернет 500, а на свежесть - 200
        for system in get_webauth_idm_systems():
            open(os.path.join(settings.WEBAUTH_IDM_CACHE_FOLDER, "%s.json" % system), 'a').close()
        with open(os.path.join(settings.WEBAUTH_IDM_CACHE_FOLDER, "webauth_idm_systems.json"),
                  'a') as webauth_idm_systems_cache_file:
            webauth_idm_systems_cache_file.write('["system1", "system2"]')
        if path == r'/check-cache-integrity':
            with pytest.raises(HTTPError) as err:
                response = yield http_client.fetch(base_url + path)
            assert err.value.code == 500
        elif path == r'/check-cache-freshness':
            response = yield http_client.fetch(base_url + path)
            assert response.code == 200

        # Запишем в файлы кеша данные, и проверим что оба мониторинга вернут 200
        for system in get_webauth_idm_systems():
            with open(os.path.join(settings.WEBAUTH_IDM_CACHE_FOLDER, "%s.json" % system), 'a') as output:
                output.write('test text')
        response = yield http_client.fetch(base_url + path)
        assert response.code == 200

        # Поменяем дату изменнения файлов кеша, и проверим, что мониторинг на полноту кеша вернет 200, а на свежесть - 500
        for system in get_webauth_idm_systems():
            os.utime(
                os.path.join(settings.WEBAUTH_IDM_CACHE_FOLDER, "%s.json" % system),
                (
                    time.time() - settings.WEBAUTH_IDM_CACHE_TTL_FOR_MONITORING * 2,
                    time.time() - settings.WEBAUTH_IDM_CACHE_TTL_FOR_MONITORING * 2,
                )
            )
        if path == r'/check-cache-integrity':
            response = yield http_client.fetch(base_url + path)
            assert response.code == 200
        elif path == r'/check-cache-freshness':
            with pytest.raises(HTTPError) as err:
                response = yield http_client.fetch(base_url + path)
            assert err.value.code == 500

        # Вернём свежие файлы, но сдвинем timestamp версии в кеше. Мониторинг на свежесть всё ещё 500
        for system in get_webauth_idm_systems():
            os.utime(
                os.path.join(settings.WEBAUTH_IDM_CACHE_FOLDER, "%s.json" % system),
                (
                    time.time(),
                    time.time(),
                )
            )
        if path == r'/check-cache-integrity':
            response = yield http_client.fetch(base_url + path)
            assert response.code == 200
        elif path == r'/check-cache-freshness':
            timestamp_mock.side_effect = mocked_get_version_old_timestamp
            with pytest.raises(HTTPError) as err:
                response = yield http_client.fetch(base_url + path)
            assert err.value.code == 500

        # Очистим директорию
        shutil.rmtree(settings.DEV_PATH_PREFIX)


@pytest.mark.gen_test
def test_qloud_sync_monitoring(http_client, base_url):
    # Очистим директорию, если прошлый тест упал
    try:
        os.remove(settings.DEV_PATH_PREFIX)
    except:
        pass
    try:
        os.makedirs(os.path.dirname(settings.WEBAUTH_QLOUD_SYNC_MARK_FILE))
    except:
        pass

    # Проверим, что мониторинг вернёт 500, если файл не существует
    with pytest.raises(HTTPError) as err:
        yield http_client.fetch(base_url + '/check-qloud-sync')
    assert err.value.code == 500

    # Создадим файл и проверим, что мониторинг вернёт 200
    open(settings.WEBAUTH_QLOUD_SYNC_MARK_FILE, 'w').close()
    response = yield http_client.fetch(base_url + '/check-qloud-sync')
    assert response.code == 200

    # Сделаем файл старым
    a_long_time_ago = time.time() - 100500 * 60 * 60
    os.utime(settings.WEBAUTH_QLOUD_SYNC_MARK_FILE, (a_long_time_ago, a_long_time_ago))

    with pytest.raises(HTTPError) as err:
        yield http_client.fetch(base_url + '/check-qloud-sync')
    assert err.value.code == 500

    # Очистим директорию
    shutil.rmtree(os.path.dirname(settings.DEV_PATH_PREFIX))


class MockedYqlClient(object):
    def __init__(self, return_table):
        self.return_table = return_table

    def get_results(self):
        table = mock.MagicMock()
        table.columns = map(list, zip(*self.return_table))
        table.rows = self.return_table[1:]
        return [table]

    def query(self, *args, **kwargs):
        request = mock.MagicMock()
        request.get_results = self.get_results
        return request


class MockedStatfaceClient(object):
    def upload_data(self, data=None, scale=None):
        self.uploaded_data = data

    def get_report(self, *args, **kwargs):
        report = mock.MagicMock()
        report.upload_data = self.upload_data
        return report


def test_balancer_request_time():
    yql_data = [
        ['component', 'average_time', 'median_time', 'quantile099', 'part_5xx', 'requests_number'],
        ['app-plus', 6.98, 4, 20, 1.96, 157006],
        ['check-oauth-token', 62.09, 16, 154.96, 4.18, 4399],
        ['oauth-callback', 233.72, 158, 556.65, 18.18, 11],
        ['save-oauth-token', 7.12, 7, 19, 12.5, 8],
    ]
    expected_result = {
        'app_plus_average_time': 6.98,
        'app_plus_median_time': 4,
        'app_plus_quantile099': 20,
        'app_plus_part_5xx': 1.96,
        'app_plus_requests_number': 157006,
        'app_plus_rps': 43.61277777777778,
        'check_oauth_token_average_time': 62.09,
        'check_oauth_token_median_time': 16,
        'check_oauth_token_quantile099': 154.96,
        'check_oauth_token_part_5xx': 4.18,
        'check_oauth_token_requests_number': 4399,
        'check_oauth_token_rps': 1.2219444444444445,
        'oauth_callback_average_time': 233.72,
        'oauth_callback_median_time': 158,
        'oauth_callback_quantile099': 556.65,
        'oauth_callback_part_5xx': 18.18,
        'oauth_callback_requests_number': 11,
        'oauth_callback_rps': 0.0030555555555555557,
        'save_oauth_token_average_time': 7.12,
        'save_oauth_token_median_time': 7,
        'save_oauth_token_quantile099': 19,
        'save_oauth_token_part_5xx': 12.5,
        'save_oauth_token_requests_number': 8,
        'save_oauth_token_rps': 0.0022222222222222222,
        'fielddate': '2015-10-21T11:35:00',
    }

    from intranet.webauth.lib.monitorings.balancer_request_time import BalancerRequestTimeMonitoring
    with mock.patch('intranet.webauth.lib.monitorings.balancer_request_time.YqlClient') as mocked_yql:
        mocked_yql.return_value = MockedYqlClient(yql_data)
        with mock.patch('intranet.webauth.lib.monitorings.balancer_request_time.StatfaceClient') as mocked_statface:
            mocked_client_instance = MockedStatfaceClient()
            mocked_statface.return_value = mocked_client_instance
            with freezegun.freeze_time('2015-10-21 09:03:31.000001'):
                BalancerRequestTimeMonitoring().run()
    uploaded_data = mocked_client_instance.uploaded_data
    assert uploaded_data == expected_result
