import os  # noqa

from smarttv.alice.tv_proxy.settings import *  # noqa

os.environ['APP_DIR'] = os.path.join(os.path.dirname(__file__), '..')
os.environ['MODE'] = 'TEST'

CACHES = {
    'default': {
        'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
        'LOCATION': 'in-mem-test',
    }
}
