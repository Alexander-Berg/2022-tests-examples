# coding: utf-8

from at.settings import *

YAUTH_TEST_USER = {
    'login': 'robot-good-ezhik',
    'uid': '1120000000023943',
    'raw_user_ticket': 'user_ticket',
}

AT_TEST_CLUB = {
    'feed_id': 4611686018427387909,
    'name': 'yaru'
}

auth_mw_index = MIDDLEWARE_CLASSES.index('django_yauth.middleware.YandexAuthMiddleware')
MIDDLEWARE_CLASSES[auth_mw_index] = 'django_yauth.middleware.YandexAuthTestMiddleware'

_index = MIDDLEWARE_CLASSES.index('django_replicated.middleware.ReplicationMiddleware')
MIDDLEWARE_CLASSES.pop(_index)

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'TEST': {
                    'NAME': 'at.sqlite',
                },
    }
}

USE_SQLITE_IN_PY_TEST = True
IS_TESTRUN = True

import logging
logging.disable(logging.ERROR)
