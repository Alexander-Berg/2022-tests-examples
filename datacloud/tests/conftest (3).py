# -*- coding: utf-8 -*-
import pytest

import yatest.common as yc
from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig
from library.python import resource

from yql.api.v1.client import YqlClient
from datacloud.features.dssm import dssm_main

CYPRESS_DIR = 'datacloud/features/dssm/tests/cypress_data'


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

@pytest.fixture(scope='module')
def dssm_main_retro_instance(yt_client, yql_client, yql_http_file_server):
    urls = yql_http_file_server.register_files({'model.dssm': resource.find('model.dssm')}, {})
    _dssm_main_instance = dssm_main.DSSMTables(date_str='1', is_retro=True,
                                               yt_client=yt_client, yql_client=yql_client,
                                               model_url=urls['model.dssm'])

    return _dssm_main_instance
