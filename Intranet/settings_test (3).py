from easymeeting.settings import *

import logging

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
    }
}
YAUTH_DEV_USER_LOGIN = 'robot-easymeeting'
YAUTH_DEV_USER_UID = '1120000000073516'
logging.disable(logging.ERROR)

CACHES = {
    'default': {
        'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
    }
}
