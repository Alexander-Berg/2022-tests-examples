from .settings_base import *
from .settings_errbooster import *  # noqa

LOGGING['loggers']['']['level'] = 'DEBUG'
LOGGING['loggers']['bcl']['level'] = 'DEBUG'
LOGGING['loggers']['django_replicated'] = {'level': 'INFO'}  # Слишком шумно.
LOGGING['loggers']['paramiko'] = {'level': 'INFO'}  # Слишком шумно.
LOGGING['loggers']['ydb.resolver.DiscoveryEndpointsResolver'] = {'level': 'INFO'}  # Слишком шумно.

SITE_URL = 'https://balalayka-test.paysys.yandex-team.ru'

TRUSTED_DOMAINS = (
    'balalayka-test.paysys.yandex-team.ru',
    'bcl-callback-test.paysys.yandex.net',

    # todo далее устаревшие. потребители не должны более использовать FINTOOLSDUTY-651
    'balalayka-test-l7.paysys.yandex-team.ru',
    'bcl-xmlrpc-test.paysys.yandex.net',
 )

ALLOWED_HOSTS.extend(TRUSTED_DOMAINS)

CSRF_TRUSTED_ORIGINS = [f'https://{domain}' for domain in TRUSTED_DOMAINS]
