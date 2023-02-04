from .settings_auth import *  # noqa
from .settings_base import *  # noqa
from .settings_db import *  # noqa
from .settings_errbooster import *  # noqa


DEBUG = False

ALLOW_ROBOTS = True

loggers = LOGGING['loggers']

loggers['']['level'] = 'DEBUG'
loggers['mdh']['level'] = 'DEBUG'
loggers['django_replicated'] = {'level': 'INFO'}  # Слишком шумно.
loggers['kikimr'] = {'level': 'WARNING'}  # Слишком шумно.

# Включаем browserable api для упрощения жизни разработчиков фронта.
REST_FRAMEWORK['DEFAULT_RENDERER_CLASSES'] = (
    'rest_framework.renderers.JSONRenderer',
    'rest_framework.renderers.BrowsableAPIRenderer',
)

DOMAIN_FRONT_LOCAL = f'local.{DOMAIN_FRONT}'  # для подключения фронта с локальной машины
DOMAIN_FRONT_LOCAL_3000 = f'{DOMAIN_FRONT_LOCAL}:3000'

CSRF_TRUSTED_ORIGINS.extend([
    DOMAIN_FRONT_LOCAL,
    DOMAIN_FRONT_LOCAL_3000,
])

CORS_ORIGIN_WHITELIST += [
    f'https://{DOMAIN_FRONT_LOCAL}',
    f'https://{DOMAIN_FRONT_LOCAL_3000}',
]
