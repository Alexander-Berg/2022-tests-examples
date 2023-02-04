# -*- coding: utf-8 -*-
from .base import *

PORT = 1

SITE_BASE_URI = 'https://api.test.directory.qloud.yandex-team.ru/'


MAIN_DB_SHARDS_COUNT = 2

DATABASES_SSL_MODE = 'verify-full'

# 1 shard
MAIN_DB_SHARDS = [
    {
        'name': 'dirdb_test_01',
        'host': [
            'sas-w9fajgkcaktlqxmk.db.yandex.net',
            'vla-1d4s5zvq7vtbokbf.db.yandex.net',
            'man-6mgxoxl611t18kja.db.yandex.net',
        ],
    },
    {
        'name': 'dirdb_test_02',
        'host': [
            'sas-1hkzffy4czds7xn3.db.yandex.net',
            'vla-whfc9uhh63obucgn.db.yandex.net',
            'man-89ezxjfbo72r66ch.db.yandex.net',
        ],
    }
]
MAIN_DB_DEFAULTS = {
    'port': 6432,
    'user': 'ydir',
    'password': None,
}
META_DB_DEFAULTS = {
    'name': 'meta_dirdb_test',
    'host': [
        'sas-k3lpwznrunwkk6zb.db.yandex.net',
        'vla-ru6je7ha2n3ogp76.db.yandex.net',
        'man-9fgayy7odmzt92zb.db.yandex.net',
    ],
    'port': 6432,
    'user': 'ydir',
    'password': None,
}

MIGRATE_DATABASE_USER = 'ydir'

VAR_DATA_DIR_NAME = 'testing'

DB_LOGLEVEL = 'INFO' # может быть 'INFO или 'DEBUG', если надо выводить параметры
YANDEX_ALL_CA = '/usr/lib/yandex/yandex-directory/ssl/yandexAllCAs.pem'

BLACKBOX_URL = 'http://pass-test.yandex.ru/blackbox/'

PASSPORT_URL_LANDING = 'https://passport-test.yandex.ru/registration/connect'
PASSPORT_API = 'http://passport-test-internal.yandex.ru/'
ABOOK_TEST_HOST = 'https://collie-test.mail.yandex.net:443'
ABOOK_QA_HOST = 'https://collie-test.mail.yandex.net:443'

ABOOK_TEST_CALLBACK = {
    'callback': ABOOK_TEST_HOST + '/workspace/update',
    'headers': {}
}

ABOOK_QA_CALLBACK = {
    'callback': ABOOK_QA_HOST + '/workspace/update',
    'headers': {}
}

EDIT_PASSPORT_ACCOUNT = {
    'callback': 'intranet.yandex_directory.src.yandex_directory.passport.callbacks.edit_passport_account',
}

# Подписка на события пользователей (наименование событий - в прошедшем времени)
SUBSCRIPTIONS['user_added'].extend([
    ABOOK_TEST_CALLBACK,
])

SUBSCRIPTIONS['user_moved'].extend([
    ABOOK_TEST_CALLBACK,
])

SUBSCRIPTIONS['user_property_changed'].extend([
    ABOOK_TEST_CALLBACK,
    EDIT_PASSPORT_ACCOUNT,
])

SUBSCRIPTIONS['user_dismissed'].extend([
    ABOOK_TEST_CALLBACK,
])

# Подписка на события департаментов
SUBSCRIPTIONS['department_added'].extend([
    ABOOK_TEST_CALLBACK,
])

SUBSCRIPTIONS['department_moved'].extend([
    ABOOK_TEST_CALLBACK,
])

SUBSCRIPTIONS['department_deleted'].extend([
    ABOOK_TEST_CALLBACK,
])

SUBSCRIPTIONS['department_property_changed'].extend([
    ABOOK_TEST_CALLBACK,
])

# Подписка на события групп
SUBSCRIPTIONS['group_added'].extend([
    ABOOK_TEST_CALLBACK,
])

SUBSCRIPTIONS['group_deleted'].extend([
    ABOOK_TEST_CALLBACK,
])

SUBSCRIPTIONS['group_property_changed'].extend([
    ABOOK_TEST_CALLBACK,
])

# Подписка на события организации
SUBSCRIPTIONS['organization_migrated'].extend([
    ABOOK_TEST_CALLBACK,
])

SUBSCRIPTIONS['organization_added'].extend([
    ABOOK_TEST_CALLBACK,
])

SUBSCRIPTIONS['organization_deleted'].extend([
    ABOOK_TEST_CALLBACK,
])

DOMAIN_PART = '.yaconnect.com'

# Email
MAIL_SEND_FUNCTION = 'intranet.yandex_directory.src.yandex_directory.core.mailer.mailer.send'

GOLOVAN_STATS_AGGREGATOR_CLASS = 'golovan_stats_aggregator.uwsgi.UwsgiStatsAggregator'

# для тестах используем ru и en
ORGANIZATION_LANGUAGES = "ru,en"

PORTAL_URL = 'https://connect-test.ws.yandex.ru/'

# Данные для аватарницы, в которой лежат лого организаций.
MDS_READ_API = 'https://avatars.mdst.yandex.net'
MDS_WRITE_API = 'http://avatars-int.mdst.yandex.net:13000'
MDS_NAMESPACE = 'connect'

BILLING_YT_TABLES_PATH = '//home/yandex-connect-billing-test/data'
BILLING_API_URL = 'https://balance-xmlrpc-tvm-ts.paysys.yandex.net:8004/xmlrpctvm'
BILLING_YT_CLUSTERS = 'freud,hume'
BILLING_CLIENT_UID_FOR_TESTING = 1130000024872018  # billing_test@cronies.ru
BILLING_CLIENT_CLASS = 'intranet.yandex_directory.src.yandex_directory.common.billing.testing_client.BillingClientWithUidReplacement'

STAFF_URL = 'https://team.test.yandex.{tld}/'

PROMOCODE_FOR_EDUCATION_ORG = 'edu_org_promocode'
PROMOCODE_FOR_YANDEX_PROJECT_ORG = 'yndx_org_promocode'

STEP_API = 'https://step-sandbox1.n.yandex-team.ru'


BIND_ACCESS_SKIP_CHECK = {
    'direct',
    'tools-integration-test',
    'yandexsprav',
    'alice_b2b',
    'metrika',  # так как иначе в тестинге нельзя привязать счетчик из-за использования разных ЧЯ нами и метрикой
}

CRON_CHECK_BALANCE = False

BILLING_PARTNER_DISK_PRODUCT_ID = 510830

TRACKER_YT_ORGANIZATIONS_TABLE = '//home/startrek/tables/test/b2b/all/organizations'
CLOUD_TS_HOST = 'ts.private-api.cloud-testing.yandex.net:4282'
CLOUD_NOTIFY_URL = 'https://console-preprod.cloud.yandex.net/notify/v1/send'

DIRECTORY_TVM_CLIENT_ID = 2000204

LOGBROKER_ENDPOINT = 'logbroker.yandex.net'
LOGBROKER_PORT = '2135'
LOGBROKER_TOPIC = 'domenator_logbroker/test/domenator-topic'
LOGBROKER_CONSUMER_NAME = 'domenator_logbroker/test/yandex_connect_consumer_testing'
LOGBROKER_TVM_ID = 2001059
LOGBROKER_READ_MAX_LAG = 10

PASSPORT_LOGBROKER_TOPIC = "/passport/passport-account-modification-log-fast-testing"
PASSPORT_LOGBROKER_CONSUMER_NAME = "/directory/passport/passport-account-modification-testing"

TRIAL_SHUTDOWN_DATE_FOR_CLOUD_ORGANIZATIONS = '2021-03-22'

REDIS_HOSTS = ['sas-35wh3vvznwktolx2.db.yandex.net']
REDIS_DB_NAME = 'redis_test'

EVENT_NOTIFICATION_ENQUEUE_ON_END = False
