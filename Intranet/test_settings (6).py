import os
import time
import warnings

from plan.settings import *  # noqa
from django_idm_api.settings import *  # noqa

IN_TEST = True

AUTH_TEST_USER = 'kukuxumusu'

user = os.environ.get('PG_LOCAL_USER', 'postgres')
password = os.environ.get('PG_LOCAL_PASSWORD', 'postgres')
db_name = os.environ.get('PG_LOCAL_DATABASE', 'abc')
port = os.environ.get('PG_LOCAL_PORT', '5432')

if os.environ.get('USE_SQLITE'):
    DATABASE_ENGINE = 'django.db.backends.sqlite3'
else:
    DATABASE_ENGINE = 'django_pgaas.backend'

DATABASES = {
    'default': {
        'ENGINE': DATABASE_ENGINE,
        'NAME': db_name,
        'USER': user,
        'PASSWORD': password,
        'HOST': 'localhost',
        'PORT': port,
        'OPTIONS': {
            'sslmode': 'disable',
        },
    },
    'slave1': {
        'ENGINE': DATABASE_ENGINE,
        'NAME': db_name,
        'USER': user,
        'PASSWORD': password,
        'HOST': 'localhost',
        'PORT': port,
        'OPTIONS': {
            'sslmode': 'disable',
        },
    },
}

DATABASE_SLAVES = []
DATABASE_ROUTERS = []

DATABASES['default'].setdefault('TEST', {}).update({
    'NAME': 'test_%s_%s_%s' % (DATABASES['default']['NAME'], 'abc', time.time()),
    'CHARSET': 'utf8',
})

DATABASES.pop('slave1', None)

warnings.filterwarnings("ignore", category=UserWarning)

MIDDLEWARE_CLASSES = [
    'plan.common.middleware.CORSMiddleware',
    'plan.common.middleware.AbcReadOnlyMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django_yauth.middleware.YandexAuthTestMiddleware',
    'plan.common.middleware.UserMiddleware',
    'plan.common.middleware.TvmAccessMiddleware',
    'waffle.middleware.WaffleMiddleware',
    'plan.common.utils.messages.AjaxMessaging',
    'plan.common.middleware.I18NMiddleware',
    'plan.common.middleware.CanEditPermissionMiddleware',
    'plan.common.middleware.AcceptWithPermissionsOnlyMiddleWare',
    'plan.common.middleware.DisablingUrlMiddleWare',
]

YAUTH_TEST_USER = 'enot'
CREATE_USER_ON_ACCESS = True

IDM_URL_PREFIX = 'api/idm/'

_EXCLUDED_APPS = ['plan.api']
INSTALLED_APPS = [app for app in INSTALLED_APPS if app not in _EXCLUDED_APPS]  # noqa

CACHES = {
    'default': {
        'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
    }
}

# пока так сделаю, потому что тороплюсь, но потом нужно получше что-то
# придумать, чтобы отключать сигналы в тестах.
DENORMALIZATION_MODELS = ()

task_always_eager = CELERY_ALWAYS_EAGER = CELERY_TASK_ALWAYS_EAGER = True
LOCKS_ENABLED = False

LOGGING = {
    'version': 1,
    'disable_existing_loggers': True,
    'handlers': {
        'console': {
            'class': 'logging.StreamHandler',
        },
    },
    # 'loggers': {
    #     'django': {
    #         'handlers': ['console'],
    #         'level': 'DEBUG',
    #     },
    # },
    'root': {
        'level': 'DEBUG',
        'handlers': ['console']
    }
}

YAUTH_MECHANISMS = [
    'django_yauth.authentication_mechanisms.cookie',
    'django_yauth.authentication_mechanisms.oauth',
    'django_yauth.authentication_mechanisms.tvm',
]
YAUTH_TVM2_CLIENT_ID = 123
YAUTH_TVM2_SECRET = 'tvm_secret'

SERVICE_TVM_ID_ALLOWED_FOR_USERS = {222, }

AUTHENTICATION_BACKENDS = [
    'plan.api.auth.AuthBackend',
]

RESTRICT_OEBS_SUBTREE = False
