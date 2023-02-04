from review.settings import *

import logging

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql_psycopg2',
        'NAME': 'review',
        'USER': 'review',
        'PASSWORD': 'review',
        'HOST': 'postgres',
        'PORT': 5432,
        'CONN_MAX_AGE': None,
        'OPTIONS': {
            'connect_timeout': 5,
        },
    },
}

YAUTH_DEV_USER_LOGIN = 'test_user'
logging.disable(logging.ERROR)


_index = MIDDLEWARE_CLASSES.index('review.lib.auth.MoonNightMiddleware')
MIDDLEWARE_CLASSES.pop(_index)

_index = MIDDLEWARE_CLASSES.index('review.lib.replicated.ReplicationMiddleware')
MIDDLEWARE_CLASSES.pop(_index)
ENABLE_ERRORBOOSTER = False
