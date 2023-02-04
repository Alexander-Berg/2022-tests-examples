# -*- coding: utf-8 -*-
import pytest
import os
import contextlib
from tempfile import NamedTemporaryFile

import yatest.common as yc
from library.python import resource
from mapreduce.yt.python.yt_stuff import YtConfig
from yt.wrapper import ypath_join
from yql.api.v1.client import YqlClient

from datacloud.dev_utils.data.data_utils import array_tostring
from datacloud.input_pipeline.input_pipeline import InputPipeLine


class Constants:
    TICKET_NAME = 'XPROD-000'
    PARTNER_ID = 'test_partner'
    CYPRESS_DIR = 'datacloud/input_pipeline/input_pipeline/tests/cypress_data'
    INPUT_RES_NAME = 'input.csv'
    NORM_RES_NAME = 'norm.tsv'
    RETRO_TAG = ''
    NORM_DELIMITER = '\t'

    SCORING_DIR = '//projects/scoring'
    PARTNER_DIR = ypath_join(SCORING_DIR, PARTNER_ID)
    TICKET_DIR = ypath_join(PARTNER_DIR, TICKET_NAME)

    RAW_DATA_DIR = ypath_join(TICKET_DIR, 'raw_data')
    RAW_TABLE = ypath_join(RAW_DATA_DIR, 'table')
    GLUED_TABLE = ypath_join(RAW_DATA_DIR, 'glued')

    GET_YUIDS_DIR = ypath_join(TICKET_DIR, 'get_yuids')
    INPUT_TABLE = ypath_join(GET_YUIDS_DIR, 'input')
    ALL_CID_TABLE = ypath_join(GET_YUIDS_DIR, 'all_cid')
    ALL_YUID_TABLE = ypath_join(GET_YUIDS_DIR, 'all_yuid')

    INPUT_YUID_TABLE = ypath_join(TICKET_DIR, 'input_yuid')
    DATACLOUD_TICKET_DIR = ypath_join(TICKET_DIR, 'datacloud')
    GREP_TICKET_DIR = ypath_join(DATACLOUD_TICKET_DIR, 'grep')
    AGG_TICKET_DIR = ypath_join(DATACLOUD_TICKET_DIR, 'aggregates')

    XPROD_DIR = '//home/x-products'
    AUDIENCE_DIR = ypath_join(XPROD_DIR, 'production/partners_data/audience')
    CRYPTA_DB_DIR = ypath_join(XPROD_DIR, 'production/crypta_v2/crypta_db_last')
    CID2ALL = ypath_join(CRYPTA_DB_DIR, 'cid_to_all')
    ID_VALUE2CID = ypath_join(CRYPTA_DB_DIR, 'id_value_to_cid')
    SITE2VEC = '//home/x-products/production/datacloud/bins/site2vec_04.04.2017'
    NORMED_S2V_COUNT = 6


@pytest.fixture(scope='module')
def constants():
    return Constants


@pytest.fixture(scope='module')
def yt_config():
    return YtConfig(local_cypress_dir=yc.source_path(Constants.CYPRESS_DIR))


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


@contextlib.contextmanager
def mk_file(resource_name, directory=None):
    directory = directory or yc.output_path()
    with NamedTemporaryFile(dir=directory) as tmp_file:
        tmp_file.write(resource.find(resource_name))
        tmp_file.seek(0)

        yield tmp_file.name


@pytest.yield_fixture(scope='module')
def input_file(constants):
    with mk_file(constants.INPUT_RES_NAME) as fn:
        yield fn


@pytest.yield_fixture(scope='module')
def norm_file(constants):
    with mk_file(constants.NORM_RES_NAME) as fn:
        yield fn


@pytest.yield_fixture(scope='module')
def logistic_model_file():
    with mk_file('logistic_model.pkl') as fn:
        yield fn


def binary_mapper(rec):
    for t in ('vector_b', 'vector_m'):
        rec[t] = array_tostring(map(float, rec[t].split(' ')))
    yield rec


@pytest.fixture(scope='module', autouse=True)
def input_pipeline(yt_client, yql_client, constants, input_file, norm_file, yql_http_file_server,
                   logistic_model_file):
    directory, input_file_name = os.path.split(input_file)
    norm_file_name = os.path.split(norm_file)[-1]

    yt_client.run_map(binary_mapper, constants.SITE2VEC, constants.SITE2VEC)
    yt_client.run_sort(constants.SITE2VEC, sort_by='host')

    urls = yql_http_file_server.register_files({'model.dssm': resource.find('model.dssm')}, {})

    settings = {
        'PARTNER_ID': constants.PARTNER_ID,
        'TICKET_NAME': constants.TICKET_NAME,
        'PATH_TO_CSV': directory,
        'INPUT_FILE': input_file_name,
        'NORMALIZED_FILE': norm_file_name,
        'RETRO_TAG': constants.RETRO_TAG,
        'SHUT_UP_ST_BOT': True,
        'DSSM_MODEL_URL': urls['model.dssm'],
        'PATH_TO_TAKE_MODEL_FROM': logistic_model_file,
        'NORMED_S2V_COUNT': constants.NORMED_S2V_COUNT,
        'APPLY_FOR_FEATURES': 'DSSM+NORMED_S2V',
        'IS_CREDIT_SCORING': True,
        'USE_CRYPTA_SNAPSHOT': False
    }
    _input_pipeline = InputPipeLine.from_dict(settings, yt_client=yt_client,
                                              yql_client=yql_client)

    return _input_pipeline
