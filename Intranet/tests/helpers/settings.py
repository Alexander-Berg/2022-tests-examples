import os  # noqa: UnusedImport

from intranet.search.api.settings import *  # noqa
from django_alive.settings import *  # noqa: ImportStarUsed
from django_idm_api.settings import *  # noqa: ImportStarUsed

try:
    _index = MIDDLEWARE.index('django_yauth.middleware.YandexAuthMiddleware')  # noqa: F405
except ValueError:
    pass
else:
    MIDDLEWARE[_index] = 'django_yauth.middleware.YandexAuthTestMiddleware'  # noqa: F405

DATABASES = {'default': {
    'ENGINE': 'django_pgaas.backend',
    'HOST': 'localhost',
    'NAME': os.environ.get('PG_LOCAL_DATABASE', ''),
    'USER': os.environ.get('PG_LOCAL_USER', ''),
    'PASSWORD': os.environ.get('PG_LOCAL_PASSWORD', ''),
    'PORT': os.environ.get('PG_LOCAL_PORT', ''),
}}
REPLICATED_DATABASE_SLAVES = []

DEBUG = False

from django_yauth.settings import *  # noqa: ImportStarUsed

YAUTH_TYPE = 'intranet'
YAUTH_USE_SITES = False
YAUTH_TEST_USER = {'uid': 0, 'login': 'test', 'email': 'test@yandex-team.ru', 'is_superuser': True}

YAUTH_TVM2_CLIENT_ID = TVM2_CLIENT_ID  # noqa: ImportStarUsage
YAUTH_TVM2_SECRET = TVM2_SECRET  # noqa: ImportStarUsage
YAUTH_USE_TVM2_FOR_BLACKBOX = True
YAUTH_MECHANISMS = ['django_yauth.authentication_mechanisms.cookie']
