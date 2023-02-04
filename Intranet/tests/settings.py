import os
import warnings

from requests.packages import urllib3

from intranet.femida.src.settings import *  # noqa


urllib3.disable_warnings()

DEBUG = True
TEMPLATE_DEBUG = DEBUG
IN_TEST = True
IS_LOCAL = False

DATABASES = {
    'default': {
        'ENGINE': DATABASE_ENGINE,  # noqa: F405
        'HOST': 'localhost' if os.getenv('IS_ARCADIA') else 'postgres',
        'NAME': os.getenv('PG_LOCAL_DATABASE', 'femida'),
        'USER': os.getenv('PG_LOCAL_USER', 'femida'),
        'PASSWORD': os.getenv('PG_LOCAL_PASSWORD', 'femida'),
        'PORT': os.getenv('PG_LOCAL_PORT', '5432'),
    },
}


REPLICATED_DATABASE_SLAVES = []
DATABASE_ROUTERS = ['django_replicated.router.ReplicationRouter']


SOUTH_TESTS_MIGRATE = False


LOGGING['handlers']['console']['level'] = 'ERROR'  # noqa: F405


warnings.filterwarnings("ignore", category=UserWarning)


MIDDLEWARE = (
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'intranet.femida.src.core.middleware.XFrameOptionsMiddleware',
    'intranet.femida.src.core.middleware.TestYauthMiddleware',
    'django.middleware.security.SecurityMiddleware',
    'intranet.femida.src.core.middleware.TestAuthMiddleware',
    'intranet.femida.src.permissions.middleware.PermissionsContextMiddleware',
)
INSTALLED_APPS = list(INSTALLED_APPS)  # noqa: F405
# https://st.yandex-team.ru/WIKI-11094
INSTALLED_APPS.remove('django_tools_log_context')
INSTALLED_APPS += [
    'intranet.femida.tests',
]

CACHES = {
    'default': {
        'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
    }
}

CELERY_ALWAYS_EAGER = False

DEFAULT_FILE_STORAGE = 'django.core.files.storage.FileSystemStorage'
MEDIA_ROOT = '/tmp/'
