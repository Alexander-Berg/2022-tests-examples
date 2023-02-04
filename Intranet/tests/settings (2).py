import os
import warnings

from requests.packages import urllib3

from intranet.hrdb_ext.src.settings import *  # noqa


urllib3.disable_warnings()

warnings.filterwarnings("ignore", category=UserWarning)


DEBUG = True
IN_TEST = True
IS_LOCAL = False

DATABASES = {
    'default': {
        'ENGINE': 'django_pgaas.backend',
        'HOST': 'localhost' if os.getenv('IS_ARCADIA') else 'postgres',
        'NAME': os.getenv('PG_LOCAL_DATABASE', 'hrdb_ext'),
        'USER': os.getenv('PG_LOCAL_USER', 'hrdb_ext'),
        'PASSWORD': os.getenv('PG_LOCAL_PASSWORD', 'hrdb_ext'),
        'PORT': os.getenv('PG_LOCAL_PORT', '5432'),
    },
}

CACHES = {
    'default': {
        'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
    }
}

DEFAULT_FILE_STORAGE = 'django.core.files.storage.FileSystemStorage'
MEDIA_ROOT = '/tmp/'
