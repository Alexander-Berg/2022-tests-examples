# -*- coding: utf-8 -*-

import pytest

from yt.wrapper import ypath_join

from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
    LOG_INTERVAL_KEY,
    RUN_ID_KEY,
)
from billing.log_tariffication.py.jobs.common.meta_checker import MetaChecker

from billing.library.python.logfeller_utils.tests.utils import mk_interval
from billing.library.python.yt_utils.test_utils.utils import create_subdirectory
from billing.log_tariffication.py.tests.constants import CURR_RUN_ID, PREV_RUN_ID


@pytest.fixture(name='res_dir')
def res_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res')


@pytest.fixture
def meta_checker(yt_client):
    return MetaChecker(yt_client)


def get_meta(log_interval, run_id=None, log_interval_key=LOG_INTERVAL_KEY):
    meta = {
        log_interval_key: log_interval.to_meta(),
    }
    if run_id is not None:
        meta[RUN_ID_KEY] = run_id
    return meta


def test_dir_intervals(yt_client, res_dir, meta_checker: MetaChecker):
    prev_path = ypath_join(res_dir, PREV_RUN_ID)
    yt_client.create(
        'table',
        prev_path,
        attributes={
            LOG_TARIFF_META_ATTR: get_meta(mk_interval(10, 20), PREV_RUN_ID),
            'schema': [{'name': 'name', 'type': 'string'}]
        }
    )
    yt_client.write_table(prev_path, [{'name': 'hello world'}])

    curr_path = ypath_join(res_dir, CURR_RUN_ID)
    yt_client.create(
        'table',
        curr_path,
        attributes={
            LOG_TARIFF_META_ATTR: get_meta(mk_interval(20, 30), CURR_RUN_ID),
            'schema': [{'name': 'name', 'type': 'string'}]
        }
    )
    yt_client.write_table(prev_path, [{'name': 'hello world'}])

    config = {'dir_intervals': [res_dir]}
    meta_checker.check(get_meta(mk_interval(30, 40), CURR_RUN_ID), config)

    try:
        meta_checker.check(get_meta(mk_interval(15, 25), CURR_RUN_ID), config)
    except AssertionError as e:
        assert str(e) == f'interval mismatch for {res_dir}'
