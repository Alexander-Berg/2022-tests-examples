# -*- coding: utf-8 -*-
import pytest
import yatest.common as yc
from mapreduce.yt.python.yt_stuff import YtConfig
from yql.api.v1.client import YqlClient

CYPRESS_DIR = 'datacloud/features/contact_actions/tests/cypress_data'


@pytest.fixture(scope='module')
def yt_config():
    return YtConfig(local_cypress_dir=yc.source_path(CYPRESS_DIR))


@pytest.fixture(scope='module')
def yt_client(yt):
    return yt.get_yt_client()


@pytest.fixture(scope='module')
def yql_client(yql_api):
    return YqlClient(
        server='localhost',
        port=yql_api.port,
        db='plato'
    )
