from .settings_default import *

IMAP_FOLDER = 'TESTING'

BASE_URL = 'https://fb.test.yandex-team.ru'

SEND_REAL_MESSAGES = False
BANNED_EMAIL_RECIPIENTS = [
    'tigran@yandex-team.ru',
]

# CORS
CORS_ORIGIN_REGEX_WHITELIST = (
    r'^.*\.yandex-team\.ru$',
)
CORS_ALLOW_HEADERS = [
    'x-requested-with',
    'content-type',
    'accept',
    'origin',
    'authorization',
    'x-csrftoken',
    'user-agent',
    'accept-encoding',
    'debug-login',
]

CIA_BASE_URL = 'https://cia.test.yandex-team.ru/'

_auth_index = MIDDLEWARE_CLASSES.index('fb.cia.middleware.CustomYauthMiddleware')
MIDDLEWARE_CLASSES.insert(_auth_index + 1, 'fb.mock.middleware.MockAuthMiddleware')

CELERY_QUEUE = 'feedback-queue-testing'
CELERY_DEFAULT_QUEUE = CELERY_QUEUE
CELERY_DEFAULT_ROUTING_KEY = CELERY_QUEUE

try:
    from local_settings import *
except ImportError:
    pass


GOALS_BASE_URL = "https://goals.test.tools.yandex-team.ru/"
STAFF_API_BASE_URL = 'https://staff-api.test.yandex-team.ru/v3/'

CACHES = get_redis_cache_settings(cluster_name='hr-tech-test-redis')
