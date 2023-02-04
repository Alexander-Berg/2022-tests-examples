from .settings_auth import *  # noqa
from .settings_base import *  # noqa
from .settings_db import *  # noqa
from .settings_errbooster import *  # noqa


DEBUG = False

ALLOW_ROBOTS = True

loggers = LOGGING['loggers']  # noqa: F405

loggers['']['level'] = 'DEBUG'
loggers['dwh']['level'] = 'DEBUG'

if DEVELOPER_LOGIN:  # noqa: F405
    # Для случаев, когда запуск производится локально
    # (на машине разработчика), но использовать требуется
    # при этом конфигурацию "как на тестовом стенде",
    # например, в целях отладки луиджи-задач.
    # Предполагается, что YENV_TYPE=testing, заполнен DWH_USER_LOGIN,
    # а в директории conf/ лежат файлы конфигурации (возможно созданные
    # на основе данных из .env файлов).
    from .settings_env_development import *  # noqa
