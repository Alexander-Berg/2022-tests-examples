from .settings import *
from copy import deepcopy
import os
import warnings

DEBUG = True
TEMPLATE_DEBUG = DEBUG

# workaround для мока celery.current_app.control.inspect()
CELERY_MODULE = 'staff/celery_tools/tests'

DATABASES = {
    'default': {
        'ENGINE': 'staff.lib.db_engine',
        'NAME': 'staff',
        'USER': 'staff',
        'PASSWORD': 'staff',
        'HOST': 'postgres',
        'PORT': 5432,
        'CONN_MAX_AGE': None,
        'OPTIONS': {
            'connect_timeout': 5,
        },
    },
}

REPLICATED_CHECK_STATE_ON_WRITE = False
REPLICATED_DATABASE_SLAVES = []

SOUTH_TESTS_MIGRATE = False

if 'LOGGING' in locals():
    # LOGGING = {'version': 1}  # disable logging
    LOGGING['handlers']['stream']['formatter'] = 'file'


warnings.filterwarnings("ignore", category=UserWarning)


# TEST_RUNNER = 'django_nose.NoseTestSuiteRunner'


CELERY_ALWAYS_EAGER = True


MIDDLEWARE_CLASSES = [
    'corsheaders.middleware.CorsMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django_replicated.middleware.ReplicationMiddleware',
    'staff.lib.middleware.StaffReadOnlyMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',

    'staff.lib.middleware.TestAuthMiddleware',

    'staff.lib.middleware.LanguageMiddleware',
    'staff.lib.middleware.AccessMiddleware',
]

AUTH_TEST_USER = 'tester'
AUTH_TEST_PERSON_UID = '1120000000018264'

YAUTH_MECHANISMS = [
    'staff.lib.auth.test_auth_mechanism',
]

PASSWORD_HASHERS = (
    'django.contrib.auth.hashers.UnsaltedMD5PasswordHasher',
)

DISABLE_BABYLON_LOG = True

PROD_EMISSION_MASTER_REPLICATED_MODELS = EMISSION_MASTER_REPLICATED_MODELS
EMISSION_MASTER_REPLICATED_MODELS = []

IS_TESTS = True
IS_QYP = os.getenv('IS_QYP') == '1'

YLOCK = {
    'backend': 'thread',  # чтобы не ходить в YT в тестах
}
