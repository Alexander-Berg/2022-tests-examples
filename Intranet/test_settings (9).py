# flake8: noqa
"""
Тут импортируются все настройки из settings.py (в режимах development и local), и изменяется только то, что нужно.
"""

from wiki.env_wrapper import env
from wiki.settings import *  # noqa

##### Настройки стораджей #####
from wiki.utils.features.models import WikiFeatures

if 'slave' in DATABASES:
    del DATABASES['slave']


##### Переопределения и хаки для тестов #####


def replace_item_in_list(src, old, new):
    src[src.index(old)] = new


USE_S3_MOCK = True
CELERY_TASK_ALWAYS_EAGER = False
CELERY_ALWAYS_EAGER = False
CELERY_BROKER_URL = 'memory://localhost/'  # какие-то тесты наверняка потребуют CELERY_ALWAYS_EAGER=False

TEMPLATE_LOADERS = (
    'django.template.loaders.filesystem.Loader',
    'django.template.loaders.app_directories.Loader',
)

for cache_key in CACHES.keys():
    if not USE_REDIS_FROM_RECIPE or CACHES[cache_key]['BACKEND'] != 'django_redis.cache.RedisCache':
        CACHES[cache_key]['BACKEND'] = 'django.core.cache.backends.locmem.LocMemCache'

# Проверка service_is_readonly явно спрашивает консистентный ответ,
# поэтому она знает, что перед ней перегруженный метод TwoLevelCache.get, который
# принимает параметр second_level_only
CACHES['wiki_state']['BACKEND'] = 'wiki.utils.cache.TwoLevelCache'

# Заменить оригинальную авторизационную мидлварину тестовой.
MIDDLEWARE = list(MIDDLEWARE)
replace_item_in_list(
    MIDDLEWARE, 'wiki.middleware.passport_auth.PassportAuthMiddleware', 'wiki.middleware.test_auth.TestAuthMiddleware'
)

if 'wiki.middleware.org_detector.OrgDetectorMiddleware' in MIDDLEWARE:
    MIDDLEWARE.remove('wiki.middleware.org_detector.OrgDetectorMiddleware')

STARTREK_CONFIG['OAUTH2_TOKEN'] = ''

PASSWORD_HASHERS = ('django.contrib.auth.hashers.UnsaltedMD5PasswordHasher',)

LOGGING['loggers'] = {  # только WARNING и выше
    'django.request': {
        'handlers': ['null'],
        'level': 'WARNING',
        'propagate': False,
    },
    'wiki.users.logic.robots': {
        'handlers': ['null'],  # мы никуда в тестах не печатаем это сообщение.
        'level': 'DEBUG',
        'propagate': False,
    },
    'django_replicated.router': {
        'handlers': ['null'],  # мы никуда в тестах не печатаем это сообщение.
        'level': 'DEBUG',
        'propagate': False,
    },
}

LOGGING['root'] = {'level': 'ERROR', 'handlers': ['default']}

YLOCK['backend'] = 'thread'

ROBOT_VIKA_TOKEN = '849f671d9cc44cbdb9f236c214e42bf6'

# Тесты не успевают дойти до селери брокера за установленные
# в настройках таймауты, поэтому будем использовать дефолтные значения.
BROKER_TRANSPORT_OPTIONS = {}


class DisableMigrations(object):
    def __contains__(self, item):
        return True

    def __getitem__(self, item):
        return None


FEATURES = WikiFeatures(
    avatars_enabled=False,
    ms365=not IS_BUSINESS,
    cloud_uid_supported=IS_BUSINESS,
    mock_attach_download=True,
    mock_tracker=True,
)

if not env.bool('PREPARING_MIGRATIONS', default=False):
    MIGRATION_MODULES = DisableMigrations()
    STORAGE_CLASS = 'wiki.utils.mock_storage.InMemoryStorage'
    FEATURES.inmemory_s3storage = True
