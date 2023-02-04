import os
import warnings

from requests.packages import urllib3

from intranet.yasanta.backend.settings import *  # noqa


urllib3.disable_warnings()

DEBUG = True
TEMPLATE_DEBUG = DEBUG
IN_TEST = True
IS_LOCAL = False

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql',
        'HOST': 'localhost' if os.getenv('IS_ARCADIA') else 'database',
        'NAME': os.getenv('PG_LOCAL_DATABASE', 'gifts'),
        'USER': os.getenv('PG_LOCAL_USER', 'gifts'),
        'PASSWORD': os.getenv('PG_LOCAL_PASSWORD', 'gifts'),
        'PORT': os.getenv('PG_LOCAL_PORT', '5432'),
    },
}


REPLICATED_DATABASE_SLAVES = []
DATABASE_ROUTERS = ['django_replicated.router.ReplicationRouter']


SOUTH_TESTS_MIGRATE = False


LOGGING['handlers']['console']['level'] = 'ERROR'  # noqa: F405


warnings.filterwarnings('ignore', category=UserWarning)


MIDDLEWARE = (
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'intranet.yasanta.backend.middleware.UwsgiLogMiddleware',
    'intranet.yasanta.backend.middleware.PingMiddleware',
    'intranet.yasanta.backend.middleware.AuthMiddleware',
    'django.middleware.security.SecurityMiddleware',
)
INSTALLED_APPS = list(INSTALLED_APPS)  # noqa: F405
INSTALLED_APPS += [
    'intranet.yasanta.backend.gifts.tests',
]

CACHES = {
    'default': {
        'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
    }
}

CELERY_ALWAYS_EAGER = False

DEFAULT_FILE_STORAGE = 'django.core.files.storage.FileSystemStorage'
MEDIA_ROOT = '/tmp/'

REPLICATED_DATABASE_DOWNTIME = 15
REPLICATED_CHECK_STATE_ON_WRITE = False
