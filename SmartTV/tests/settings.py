import os  # noqa

from smarttv.droideka.settings import *  # noqa

os.environ['APP_DIR'] = os.path.join(os.path.dirname(__file__), '..')
os.environ['MODE'] = 'TEST'

DB_MASTER = 'default'
DB_REPLICA = DB_MASTER
DB_MASTER_SERIALIZED = DB_MASTER

DATABASES = {
    DB_MASTER: {
        'ENGINE': 'django.db.backends.postgresql',
        'HOST': 'localhost',
        'PORT': os.environ['PG_LOCAL_PORT'],
        'NAME': os.environ['PG_LOCAL_DATABASE'],
        'USER': os.environ['PG_LOCAL_USER'],
        'PASSWORD': os.environ['PG_LOCAL_PASSWORD'],
    },
}

ANDROID_HOSTS = ['testserver']

CACHE_BACKEND_CLASS = 'django.core.cache.backends.dummy.DummyCache'

CACHES = {
    'default': {
        'BACKEND': CACHE_BACKEND_CLASS,
        'LOCATION': 'in-mem-test',
    },
    'unversioned': {
        'BACKEND': CACHE_BACKEND_CLASS,
    },
    'local': {
        'BACKEND': CACHE_BACKEND_CLASS,
    }
}

ETALON_PROMOCODE = 'ETANOL'

PLATFORM_INDICATOR = 'android'

MUSIC_DEFAULT_COVER_PATTERN = 'http://ya.ru/bucket/%%'
