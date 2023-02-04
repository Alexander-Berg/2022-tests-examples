"""
Рецепт для поднятия pg-pinger перед тестами.
Предполагается, что перед данным рецептом был выполнен рецепт на поднятие базы,
чтобы были известны параметры подключения к базе.
"""
import json
import logging
import signal

import requests
import yatest.common
from library.python.testing.recipe import declare_recipe, set_env
from library.recipes.common import start_daemon, stop_daemon
from yatest.common import network

PG_PINGER_PID_FILE = "pg_pinger.pid"
PG_PINGER_PORT_FILE = "pg_pinger.port"
PG_PINGER_CONFIG_PATH = "pg_pinger.config"

logger = logging.getLogger('pg-pinger-recipe')


def start_pg_pinger(argv):
    logger.info('Start.')

    pm = network.PortManager()

    pg_pinger_port = pm.get_tcp_port()
    with open(PG_PINGER_PORT_FILE, 'w') as f:
        f.write(str(pg_pinger_port))

    with open('pg_recipe.json') as f:
        db_conn_params = json.loads(f.read())

    env = {
        'PINGER_HOSTS': db_conn_params['host'],
        'PINGER_PORT': str(db_conn_params['port']),
        'PINGER_USERNAME': db_conn_params['user'],
        'PINGER_DATABASE': db_conn_params['dbname'],
        'PINGER_PASSWORD': db_conn_params['password'],
        'PINGER_SSLMODE': 'disable',
        'PINGER_INTERVAL': '1000',
        'PINGER_TIMEOUT': '500',
        'PINGER_MAX_FAILS': '3',
        'PINGER_SYNC_MAX_LAG': '1000',
        'PINGER_HTTP_PORT': str(pg_pinger_port),
    }

    for k, v in env.items():
        set_env(k, v)

    def check():
        pg_pinger_ping_url = f'http://localhost:{pg_pinger_port}/ping'
        try:
            response = requests.get(pg_pinger_ping_url)
        except Exception:
            logger.exception('Failed to ping pg-pinger')
            return False

        if response.status_code >= 400:
            logger.error(
                'Failed to ping pg-pinger:\nResponse code %(code)s\n%(body)s',
                dict(code=response.status_code, body=response.content),
            )
            return False

        logger.info('pg-pinger answers on %(url)s', dict(url=pg_pinger_ping_url))
        return True

    pinger_bin = yatest.common.binary_path('toolbox/pg-pinger/cmd/pg-pinger')

    logger.info('Start pg-pinger at port %(port)s', dict(port=pg_pinger_port))
    logger.info('pg-pinger binary: %(path)s', dict(path=pinger_bin))
    logger.info('pg-pinger env:\n%(env)s', dict(env=json.dumps(env)))
    start_daemon(
        command=[pinger_bin],
        environment=env,
        is_alive_check=check,
        pid_file_name=PG_PINGER_PID_FILE,
        timeout=10,
    )


def stop_pg_pinger(argv):
    logger.info('Stop.')

    with open(PG_PINGER_PID_FILE) as f:
        pid = f.read().strip()
    if not stop_daemon(pid, signal=signal.SIGKILL):
        logger.error("%(pid)s is dead", dict(pid=pid))


def main():
    declare_recipe(start_pg_pinger, stop_pg_pinger)


if __name__ == '__main__':
    main()
