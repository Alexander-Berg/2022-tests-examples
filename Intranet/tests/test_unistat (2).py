# encoding: utf-8
from __future__ import unicode_literals

import json
import shutil

import freezegun
import mock
import pytest
import tornado.web

from intranet.webauth.lib import settings
from intranet.webauth.lib.monitorings.unistat import Unistat


@pytest.fixture
def app():
    application = tornado.web.Application([
        (r'/unistat', Unistat),
    ])
    return application


@pytest.mark.xfail  # FIXME VIEWER-943
@pytest.mark.gen_test
def test_unistat(http_client, base_url):
    # Очистим директорию, если прошлый тест упал
    try:
        shutil.rmtree(settings.DEV_PATH_PREFIX)
    except:
        pass
    # Нужно для импорта generate_idm_cache
    with mock.patch('intranet.webauth.lib.utils.setup_logging') as mocked:
        mocked.return_value = None
        from intranet.webauth.lib.generate_idm_cache import main
        with mock.patch('intranet.webauth.lib.monitorings.unistat.get_cache_version', return_value=None), \
             mock.patch('intranet.webauth.lib.generate_idm_cache.update_system'),\
             mock.patch('intranet.webauth.lib.generate_idm_cache.upload_to_redis'),\
             mock.patch('intranet.webauth.lib.generate_idm_cache.check_cache_existence'), \
             mock.patch('intranet.webauth.lib.generate_idm_cache.get_roles_batch', return_value={'objects': [], 'meta': {'total_count': 0}}), \
             mock.patch('intranet.webauth.lib.generate_idm_cache.upload_to_redis'), \
             mock.patch('intranet.webauth.lib.generate_idm_cache.get_webauth_idm_systems') as get_webauth_idm_systems:
            get_webauth_idm_systems.return_value = ['system1', 'system2']

            with freezegun.freeze_time('2001-01-01 12:00:00.000001'):
                main()
            response = yield http_client.fetch(base_url + '/unistat')

            # Если какого-то из файлов со статистикой еще не существует, то вернем пустой список
            assert json.loads(response.body) == [
                ['cache_integrity_max', 0.0],
                ['cache_freshness_max', 0.0],
                ['redis_integrity_max', 0.0],
                ['check_qloud_sync_max', 1.0],
            ]
            with freezegun.freeze_time('2001-01-01 12:10:00.000001'):
                main()
            response = yield http_client.fetch(base_url + '/unistat')
            assert json.loads(response.body) == [
                ['time_of_last_two_cache_generations_ahhh', 10.0],
                ['time_of_cache_generation_ahhh', 0.0],
                ['cache_integrity_max', 0.0],
                ['cache_freshness_max', 0.0],
                ['redis_integrity_max', 0.0],
                ['check_qloud_sync_max', 1.0],
            ]
            # Очистим директорию
            shutil.rmtree(settings.DEV_PATH_PREFIX)
