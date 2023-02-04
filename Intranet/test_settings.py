# -*- coding: utf-8 -*-
import logging
import os
import socket

from blackbox import odict
from events.settings import *  # noqa
from events.arc_compat import is_arc


DEBUG = False
IS_TEST = True

MEDIA_ROOT = '/tmp/tech_disk'

FAKE_YANDEX_UID = '1234567890987654321'

ROBOT_FORMS_TEST_YANDEX_UID = '1120000000047641'

TEST_INTERNAL_YANDEX_UID = '1120000000009214'
TEST_INTERNAL_LOGIN = 'zomb-prj-124'
TEST_INTERNAL_PASSWORD = 'j04_f8fd61'

TEST_EXTERNAL_YANDEX_UID = '136920086'
TEST_EXTERNAL_LOGIN = 'test-events'
TEST_EXTERNAL_PASSWORD = 'j03_f8fd61'

TEST_BUSINESS_YANDEX_UID = '1130000000473378'
TEST_BUSINESS_LOGIN = 'wikiboy@wikitest.yaconnect.com'
TEST_BUSINESS_PASSWORD = 'yandexBusiness'
TEST_BUSINESS_ORG_ID = 732

TEST_WIKIGLEB1_LOGIN = 'admin@wikigleb1.yaconnect.com'
TEST_WIKIGLEB1_PASSWORD = 'yandexBusiness'
TEST_WIKIGLEB1_ORG_ID = 957

TEST_ROBOT_LOGIN = 'robot-forms-test'
TEST_ROBOT_PASSWORD = 'kiss4blE'

TESTS_RUN_IN_DOCKER = os.environ.get('TESTS_RUN_IN_DOCKER')

FRONTEND_DOMAIN = 'forms.yandex-team{tld}'
DEFAULT_FRONTEND_DOMAIN = 'forms.yandex-team.ru'


def get_fixtures_dir():
    fixtures_dir ='tests/fixtures'
    if TESTS_RUN_IN_DOCKER:
        fixtures_dir = os.path.join('src', fixtures_dir)
    if is_arc():
        fixtures_dir = os.path.join('intranet/forms', fixtures_dir)
    return fixtures_dir


FIXTURES_DIR = get_fixtures_dir()

DATABASES = {
    DATABASE_DEFAULT: {  # noqa
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': 'file::memory:?cache=shared',
        'TEST_CHARSET': 'utf8',
    },
}
DATABASE_ROLOCAL = DATABASE_DEFAULT  # noqa
REPLICATED_DATABASE_SLAVES = []
REPLICATED_CHECK_STATE_ON_WRITE = False
DATABASES_UPDATE = {}

CELERY_RESULT_BACKEND = None
CELERY_TASK_ALWAYS_EAGER = True

for cache_key in CACHES.keys():  # noqa
    CACHES[cache_key]['BACKEND'] = 'django.core.cache.backends.locmem.LocMemCache'  # noqa
    CACHES[cache_key]['LOCATION'] = 'localhost'  # noqa

DISK_ADMIN_API_CLIENT_CLASS = 'events.common_app.disk_admin.mock_client.DiskAdminMockClient'

KINOPOISK_API_CLIENT_CLASS = 'events.common_app.kinopoisk.mock_client.KinopoiskMockAPIClient'

MUSIC_STATUS_CLIENT_CLASS = 'events.common_app.music.mock_client.MusicStatusMockClient'

CAPTCHA_HOSTNAME = 'api.captcha.yandex.net'
IS_ENABLE_CAPTCHA = False

logging.disable(logging.CRITICAL)

SOUTH_TESTS_MIGRATE = False
SKIP_SOUTH_TESTS = True

TEST_HOST = socket.gethostname()

TEST_RUNNER = 'events.common_app.test_runners.NoseTestRunnerWithSavingCeleryTasksResults'
# TEST_RUNNER = 'events.common_app.test_runners.PytestTestRunner'

BLACKBOX_SESSIONID_MOCK = odict({
    'status': 'VALID',
    'karma_status': '0',
    'domain': None,
    'bruteforce_policy': {
        'captcha': False,
        'login_rule': 0,
        'password_expired': False,
        'level': None,
    },
    'login_status': None,
    'uid': '136920086',
    'login_rule': 0,
    'connection_id': 's:1523536031561:tKryqXYGAAATAQQMuAYCKg:53',
    'oauth': None,
    'ticket': None,
    'default_email': 'test-events@yandex.com',
    'emails': [{
        'default': True,
        'login_rule': 0,
        'validated': True,
        'address': 'test-events@yandex.com',
        'native': True,
    }],
    'secure': True,
    'redirect': False,
    'password_verification_age': 0,
    'fields': {
        'lang': 'ru',
        'city': None,
        'social_aliases': None,
        'display_name': 'test-events',
        'login_rule': 0,
        'sex': '1',
        'fio': 'Pupkin Vasily',
        'default_avatar_id': '0/0-0',
        'social': None,
        'birth_date': None,
        'login': 'test-events',
        'nickname': None,
        'email': None,
        'aliases': [('1', 'test-events')],
    },
    'age': 0,
    'password_status': None,
    'new_sslsession': None,
    'lite_uid': None,
    'new_session': None,
    'karma': '0',
    'error': 'OK',
    'valid': True,
})

BLACKBOX_USERINFO_MOCK = odict({
    'status': None,
    'karma_status': '0',
    'domain': None,
    'bruteforce_policy': {
        'captcha': False,
        'level': None,
        'login_rule': 0,
        'password_expired': False,
    },
    'login_status': None,
    'uid': '136920086',
    'login_rule': 0,
    'connection_id': None,
    'oauth': None,
    'ticket': None,
    'default_email': 'test-events@yandex.com',
    'emails': [{
        'address': 'test-events@yandex.com',
        'default': True,
        'login_rule': 0,
        'native': True,
        'validated': True,
    }],
    'fields': {
        'aliases': [('1', 'test-events')],
        'birth_date': None,
        'city': None,
        'default_avatar_id': '0/0-0',
        'display_name': 'test-events',
        'email': None,
        'fio': 'Pupkin Vasily',
        'lang': 'ru',
        'login': 'test-events',
        'login_rule': 0,
        'nickname': None,
        'sex': '1',
        'social': None,
        'social_aliases': None,
    },
    'password_status': None,
    'lite_uid': None,
    'karma': '0',
    'error': None
})

SOME_SERVICE_TICKET = (
    "3:serv:CJ4oELvO8tsFIggI65J6EMyKeg:EVMQoB-0"
    "AKBaQZ7IMVrF1Tr5hydx4vu8jtfLZKb1GN2uNu3ZRxY"
    "6BRZoxAmtj6WHmmExkGIHTGvvnnxAaWs688MrhaLnyev"
    "74b2cLCTtnQOg0BYO6q3-kMg9fLHCv86kLKKcFeJAZlCz"
    "B3E60Qq_yerO9NiGfq7LWsQFRbAEB5Y"
)

YLOCK = {
    'backend': 'thread',
}

AUTHENTICATION_BACKENDS = [
    'guardian.backends.ObjectPermissionBackend',
    'events.yauth_contrib.helpers.MockCookieMechanism',
]

CLICKHOUSE_CLIENT_CLASS = 'events.common_app.clickhouse.mock_client.MockClickHouseClient'
VERSION_GENERATOR_CLASS = 'events.surveyme.versions.RandomVersionGenerator'

STARTREK_UNIQUE_IN_TESTS = True

# vim: ft=python :
