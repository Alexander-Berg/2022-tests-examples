from .settings_base import *  # noqa
from .settings_db import *  # noqa
from .settings_errbooster import *  # noqa


DEBUG = False

loggers = LOGGING['loggers']

loggers['']['level'] = 'DEBUG'
loggers['ift']['level'] = 'DEBUG'
loggers['django_replicated'] = {'level': 'INFO'}  # Слишком шумно.
