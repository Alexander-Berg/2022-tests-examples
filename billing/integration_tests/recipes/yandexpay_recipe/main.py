import io
import json
import logging
import os
import signal
from time import sleep

import requests
import yatest.common
from library.python.testing.recipe import declare_recipe
from yatest.common import network

logger = logging.getLogger('yandexpay-recipe')


class PersistentContext:
    YANDEXPAY_STDOUT_FILE = 'yandexpay.stdout'
    YANDEXPAY_STDERR_FILE = 'yandexpay.stdout'
    YANDEXPAY_LOG_FILE = 'yandexpay.log'
    YANDEXPAY_RESPONSE_LOG_FILE = 'yandexpay_response.log'

    YANDEXPAY_PID_FILE = 'yandexpay.pid'
    YANDEXPAY_PORT_FILE = 'yandexpay.port'

    @property
    def pid(self) -> int:
        with open(self.YANDEXPAY_PID_FILE) as f:
            return int(f.read().strip())

    @pid.setter
    def pid(self, value: int):
        with open(self.YANDEXPAY_PID_FILE, 'w') as f:
            f.write(str(value))

    @property
    def server_port(self) -> int:
        if not os.path.exists(self.YANDEXPAY_PORT_FILE):
            pm = network.PortManager()
            port = pm.get_tcp_port()
            with open(self.YANDEXPAY_PORT_FILE, 'w') as f:
                f.write(str(port))
            return port
        else:
            with open(self.YANDEXPAY_PORT_FILE) as f:
                return int(f.read().strip())

    @property
    def ping_url(self) -> str:
        return f'http://localhost:{self.server_port}/ping'

    @property
    def stdout_file_path(self) -> str:
        return yatest.common.output_path(self.YANDEXPAY_STDOUT_FILE)

    @property
    def stderr_file_path(self) -> str:
        return yatest.common.output_path(self.YANDEXPAY_STDERR_FILE)

    @property
    def log_file(self):
        return yatest.common.output_path(self.YANDEXPAY_LOG_FILE)

    @property
    def response_log_file(self):
        return yatest.common.output_path(self.YANDEXPAY_RESPONSE_LOG_FILE)

    @property
    def overwrite_settings_file_path(self):
        return yatest.common.data_path('overwrite-settings.py')

    @property
    def tvm_port(self):
        with open('tvmtool.port') as f:
            return int(f.read().strip())

    @property
    def tvm_tool_auth_token(self):
        with open('tvmtool.authtoken') as f:
            return f.read().strip()

    @property
    def interaction_mock_server_port(self):
        with open('yandexpay-interaction-mock.port') as f:
            return int(f.read().strip())


ctx = PersistentContext()


def ping_yandex_pay() -> bool:
    try:
        response = requests.get(ctx.ping_url)
    except Exception:
        logger.exception('Failed to ping YandexPay backend.')
        return False

    if response.status_code >= 400:
        logger.error(
            'Failed to ping YandexPay backend.\nStatus:\n%(status)s\nBody:\n%(body)s',
            dict(status=response.status_code, body=response.content)
        )
        return False

    logger.info('YandexPay answers on ping: %(body)s', dict(body=response.content))
    return True


def get_overwrite_settings():
    csrf_path = yatest.common.source_path(
        'billing/yandex_pay/tools/load/integration_tests/recipes/yandexpay_recipe/settings/csrf.json'
    )
    geobase_path = yatest.common.source_path(
        'billing/yandex_pay/tools/load/integration_tests/recipes/yandexpay_recipe/settings/geobase.json'
    )
    root_public_key_path = yatest.common.source_path(
        'billing/yandex_pay/tools/load/integration_tests/recipes/yandexpay_recipe/settings/root_public_key.json'
    )

    with open('pg_pinger.port') as f:
        pg_pinger_port = f.read().strip()

    with open('pg_recipe.json') as f:
        db_conn_params = json.loads(f.read())

    f = io.StringIO()

    f.write(f"""
LOGGING = {{
    'version': 1,
    'disable_existing_loggers': False,
    'formatters': {{
        'uni': {{
            '()': 'sendr_qlog.UniFormatter',
        }},
    }},
    'handlers': {{
        'file': {{
            'level': 'DEBUG',
            'formatter': 'uni',
            'class': 'logging.FileHandler',
            'filename': '{ctx.log_file}',
        }}
    }},
    'root': {{
        'handlers': ['file'],
        'level': 'DEBUG',
    }}
}}
""")

    f.write(f"""
FILESTORAGE_CSRF_ANTI_FORGERY_KEYS_PATH="{csrf_path}"
""")

    f.write(f"""
FILESTORAGE_GEOBASE_PATH="{geobase_path}"
""")

    f.write(f"""
FILESTORAGE_YANDEX_PAY_ROOT_PUBLIC_KEYS_PATH="{root_public_key_path}"
""")

    f.write(f"""
RESPONSE_FILE_PATH='{ctx.response_log_file}'
""")

    f.write(f"""
PG_PINGER_URL='http://localhost:{pg_pinger_port}'
""")

    f.write("""
DATABASE = {{
    'NAME': '{dbname}',
    'USER': '{user}',
    'PASSWORD': '{password}',
    'HOST': '{host}',
    'PORT': {port},
    'USE_SSL': None,
}}
""".format(**db_conn_params))

    f.write(f"""
TVM_PORT={ctx.tvm_port}
TVM_HOST='localhost'
""")

    return f.getvalue()


def start(argv):
    logger.info('Start YandexPay backend.')

    with open(ctx.overwrite_settings_file_path, 'w') as f:
        settings_content = get_overwrite_settings()
        f.write(settings_content)
        logger.info("Using overwrite settings:\n%(settings)s", dict(settings=settings_content))

    yandexpay_bin = yatest.common.binary_path('billing/yandex_pay/bin/yandex_pay')
    logger.info('Binary file %(path)s', dict(path=yandexpay_bin))
    logger.info('stdout path %(path)s', dict(path=ctx.stdout_file_path))
    logger.info('stderr path %(path)s', dict(path=ctx.stderr_file_path))

    env = {
        'YENV_TYPE': 'load',
        'YANDEX_PAY_EXTRA_CONFIG_FILE': ctx.overwrite_settings_file_path,
        'TVMTOOL_LOCAL_AUTHTOKEN': ctx.tvm_tool_auth_token,
        'MOCKS_PORT': str(ctx.interaction_mock_server_port),
    }
    yandexpay_cmd = [yandexpay_bin, 'runserver', '--host', 'localhost', '--port', str(ctx.server_port)]

    # SIC: сохраняем контекст запуска бинаря, чтобы в тестах использовать shell команды
    with open('yandexpay.run', 'w') as f:
        f.write(json.dumps({
            'env': env,
            'cmd': [yandexpay_bin],
        }))

    execution = yatest.common.execute(
        yandexpay_cmd,
        wait=False,
        env=env,
        stdout=ctx.stdout_file_path,
        stderr=ctx.stderr_file_path,
    )
    ctx.pid = execution.process.pid

    for _ in range(10):
        if ping_yandex_pay():
            return
        sleep(1)

    raise RuntimeError('Unable to ping YandexPay backend.')


def stop(argv):
    logger.info('Stop YandexPay backend.')

    if not ping_yandex_pay():
        logger.error('YandexPay does not answer on ping, is it running?')

    try:
        os.kill(ctx.pid, signal.SIGKILL)
        logger.info('Sent termination signal to YandexPay backend.')
    except (ProcessLookupError, FileNotFoundError):
        logger.error('Unable to terminate YandexPay process.')

    if os.environ.get('YANDEXPAY_RECIPE_PRINT_LOGS', ''):
        try:
            with open(ctx.stdout_file_path) as f:
                logger.info('YandexPay backend stdout:\n%(stdout)s', dict(stdout=f.read()))
        except FileNotFoundError:
            pass

        try:
            with open(ctx.stderr_file_path) as f:
                logger.info('YandexPay backend stderr:\n%(stderr)s', dict(stderr=f.read()))
        except FileNotFoundError:
            pass

        try:
            with open(ctx.log_file) as f:
                logger.info('YandexPay backend log:\n%(log)s', dict(log=f.read()))
        except FileNotFoundError:
            pass
    else:
        logger.info('Find YandexPay backend logs in:\n%(dir)s', dict(dir=os.path.dirname(ctx.log_file)))


def main():
    declare_recipe(start, stop)
