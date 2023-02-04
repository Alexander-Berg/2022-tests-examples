# coding=utf-8

import pytest

from yt.wrapper import ypath_join

from billing.log_tariffication.py.jobs.common import move2unprocessed
from billing.log_tariffication.py.lib.constants import (
    LOG_INTERVAL_KEY,
    LOG_TARIFF_META_ATTR,
    RUN_ID_KEY,
)
from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)
from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    CURR_RUN_ID,
    NEXT_RUN_ID
)


def test_no_path_or_dir():
    with pytest.raises(AssertionError, match='Either target path or target dir must be specified'):
        move2unprocessed.run_job(
            None,
            {LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 5, 10)]).to_meta()},
            None,
            None,
            'unprocessed',
        )


def test_empty_interval():
    with pytest.raises(AssertionError, match='Empty alignment interval'):
        move2unprocessed.run_job(
            None,
            {LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 10, 10)]).to_meta()},
            'path',
            None,
            'unprocessed',
        )


@pytest.mark.usefixtures('yt_transaction')
def test_interval_mismatch(yt_client, stripped_log_path, untariffed_dir):
    meta = {
        LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 5, 10)]).to_meta(),
        RUN_ID_KEY: CURR_RUN_ID
    }
    yt_client.create(
        'table',
        stripped_log_path,
    )
    yt_client.create(
        'table',
        ypath_join(untariffed_dir, PREV_RUN_ID),
        attributes={
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 5, 10)]).to_meta(),
            }
        }
    )

    with pytest.raises(AssertionError, match=f'interval mismatch for {untariffed_dir}'):
        move2unprocessed.run_job(
            yt_client,
            meta,
            stripped_log_path,
            None,
            untariffed_dir,
        )


@pytest.mark.parametrize(
    'unprocessed_run_id, error',
    [
        (NEXT_RUN_ID, r'Next table exists for *'),
        (CURR_RUN_ID, r'Bad meta in current table for *')
    ]
)
@pytest.mark.usefixtures('yt_transaction')
def test_bad_run_id(yt_client, stripped_log_path, untariffed_dir, unprocessed_run_id, error):
    meta = {
        LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 5, 10)]).to_meta(),
        RUN_ID_KEY: CURR_RUN_ID
    }
    yt_client.create(
        'table',
        stripped_log_path,
    )
    yt_client.create(
        'table',
        ypath_join(untariffed_dir, unprocessed_run_id),
        attributes={
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 0, 5)]).to_meta(),
            }
        }
    )

    with pytest.raises(AssertionError, match=error):
        move2unprocessed.run_job(
            yt_client,
            meta,
            stripped_log_path,
            None,
            untariffed_dir,
        )


@pytest.mark.usefixtures('yt_transaction')
def test_has_path(yt_client, stripped_log_path, untariffed_dir):
    meta = {
        LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 5, 10)]).to_meta(),
        RUN_ID_KEY: CURR_RUN_ID
    }
    yt_client.create(
        'table',
        stripped_log_path,
    )
    yt_client.create(
        'table',
        ypath_join(untariffed_dir, PREV_RUN_ID),
        attributes={
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 0, 5)]).to_meta(),
            },
            '_yql_row_spec': {'mock': 'spec'},
        }
    )

    res_unprocessed_path = move2unprocessed.run_job(
        yt_client,
        meta,
        stripped_log_path,
        None,
        untariffed_dir,
    )

    assert res_unprocessed_path == ypath_join(untariffed_dir, CURR_RUN_ID)
    assert yt_client.get(ypath_join(res_unprocessed_path, f'@{LOG_TARIFF_META_ATTR}')) == meta
    assert yt_client.get(ypath_join(res_unprocessed_path, '@_yql_row_spec')) == {'mock': 'spec'}


@pytest.mark.usefixtures('yt_transaction')
def test_has_dir(yt_client, tariffed_dir, untariffed_dir):
    meta = {
        LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 5, 10)]).to_meta(),
        RUN_ID_KEY: CURR_RUN_ID
    }
    yt_client.create(
        'table',
        ypath_join(tariffed_dir, PREV_RUN_ID),
        attributes={
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 5, 10)]).to_meta(),
            },
        }
    )
    yt_client.create(
        'table',
        ypath_join(untariffed_dir, PREV_RUN_ID),
        attributes={
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: LogInterval([Subinterval('a', 'a', 0, 0, 5)]).to_meta(),
            },
            '_yql_row_spec': {'mock': 'spec'},
        }
    )

    res_unprocessed_path = move2unprocessed.run_job(
        yt_client,
        meta,
        None,
        tariffed_dir,
        untariffed_dir,
    )

    assert res_unprocessed_path == ypath_join(untariffed_dir, CURR_RUN_ID)
    assert yt_client.get(ypath_join(res_unprocessed_path, f'@{LOG_TARIFF_META_ATTR}')) == meta
    assert yt_client.get(ypath_join(res_unprocessed_path, '@_yql_row_spec')) == {'mock': 'spec'}
