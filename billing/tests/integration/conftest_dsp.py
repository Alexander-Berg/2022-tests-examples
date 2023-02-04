import os
import copy
import signal
from datetime import datetime

import arrow
import pytest
import flask

import yatest.common
import yql_ports

from billing.log_tariffication.py.lib import constants


REWARD_GROSS = 1
REWARD_NET = 2


def create_table(yt_client, table_path, info):
    yt_client.create('table', table_path, recursive=True, attributes=info['attributes'])
    yt_client.write_table(
        table_path,
        list(map(lambda r: dict(copy.deepcopy(info.get('common_data_part', dict())), **r), info.get('data', list())))
    )


@pytest.fixture(scope='session')
def udf_server_file_url():
    """
    Required in ya.make

    DEPENDS(
        billing/udf_python
    )
    """
    path = yatest.common.binary_path('billing/udf_python/libpython3_udf.so')
    port = yql_ports.get_yql_port('udf_server')
    pid = os.fork()
    if pid == 0:
        app = flask.Flask(__name__)
        @app.route('/')
        def get_file():
            return flask.send_file(path)
        app.run("127.0.0.1", port)

    yield f'http://localhost:{port}'
    os.kill(pid, signal.SIGTERM)


def to_ts(dt: datetime) -> int:
    return arrow.get(dt).replace(tzinfo=constants.MSK_TZ).int_timestamp
