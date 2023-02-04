import pytest

import yt.wrapper as yt

from billing.library.python.logfeller_utils import log_interval
from billing.log_tariffication.py.lib import constants
from billing.log_tariffication.py.jobs.partner_acts import generate_act_meta


INITIAL_ACT_META = {
    constants.RUN_ID_KEY: '2020-06-30',
    constants.ACT_DT_KEY: '2020-06-30',
    constants.LOG_INTERVAL_KEY: log_interval.LogInterval([
        log_interval.Subinterval('c1', 'log', 1, 0, 1)
    ]).to_meta(),
}


def _create_table(yt_client, tariffed_dir_path, run_id, log_frame, ref_contract_frame):
    yt_client.create('table', yt.ypath_join(tariffed_dir_path, run_id), attributes={
        constants.LOG_TARIFF_META_ATTR: {
            constants.RUN_ID_KEY: run_id,
            constants.LOG_INTERVAL_KEY: log_interval.LogInterval([
                log_interval.Subinterval('c1', 'log', 1, log_frame[0], log_frame[1])
            ]).to_meta(),
            constants.REF_INTERVAL_KEY_FORMATTER('contracts'): log_interval.LogInterval([
                log_interval.Subinterval('c1', 'contr', 1, ref_contract_frame[0], ref_contract_frame[1])
            ]).to_meta(),
        },
    })


@pytest.fixture()
def tariffed_dir(yt_client, yt_root):
    tariffed_dir_path = yt.ypath_join(yt_root, 'tariffed_dir')
    yt_client.create('map_node', tariffed_dir_path)
    _create_table(yt_client, tariffed_dir_path, run_id='2020-08-03T11:00:00', log_frame=(1, 3), ref_contract_frame=(0, 2))
    _create_table(yt_client, tariffed_dir_path, run_id='2020-09-03T11:00:00', log_frame=(3, 5), ref_contract_frame=(0, 3))
    _create_table(yt_client, tariffed_dir_path, run_id='2020-09-11T11:00:00', log_frame=(5, 7), ref_contract_frame=(0, 4))
    yield tariffed_dir_path
    yt_client.remove(tariffed_dir_path, force=True, recursive=True)


def test_generate_act_meta(caplog, yt_client, tariffed_dir):
    def generate_act_meta_run_job(last_generated_meta, last_acted_meta, shift_days):
        return generate_act_meta.run_job(
            yt_client,
            last_generated_meta=last_generated_meta,
            last_acted_meta=last_acted_meta,
            tariffed_dir=tariffed_dir,
            shift_days=shift_days,
            extract_references_tariffed=['contracts'],
        )

    new_meta_07 = generate_act_meta_run_job(
        last_generated_meta=INITIAL_ACT_META,
        last_acted_meta=INITIAL_ACT_META,
        shift_days=2,
    )

    new_meta_08_shift_2 = generate_act_meta_run_job(
        last_generated_meta=new_meta_07,
        last_acted_meta=new_meta_07,
        shift_days=2,
    )

    new_meta_08_shift_10 = generate_act_meta_run_job(
        last_generated_meta=new_meta_07,
        last_acted_meta=new_meta_07,
        shift_days=10,
    )

    new_meta_09_error = 'Here should be an AssertionError'
    try:
        generate_act_meta_run_job(
            last_generated_meta=new_meta_08_shift_2,
            last_acted_meta=new_meta_08_shift_2,
            shift_days=2,
        )
    except AssertionError as e:
        new_meta_09_error = repr(e)

    caplog.clear()
    no_meta_when_not_acted = generate_act_meta_run_job(
        last_generated_meta=new_meta_07,
        last_acted_meta=INITIAL_ACT_META,
        shift_days=2,
    )
    log_no_meta_when_not_acted = list(filter(lambda l: l[0] == 'generate_act_meta', caplog.record_tuples))

    return {
        'new_meta_07': new_meta_07,
        'new_meta_08_shift_2': new_meta_08_shift_2,
        'new_meta_08_shift_10': new_meta_08_shift_10,
        'new_meta_09_error': new_meta_09_error,
        'no_meta_when_not_acted': no_meta_when_not_acted,
        'log_no_meta_when_not_acted': log_no_meta_when_not_acted,
    }


def _create_raw_table(yt_client, dir_path, run_id, log_frame):
    yt_client.create('table', yt.ypath_join(dir_path, run_id), attributes={
        log_interval.LB_META_ATTR: log_interval.LogInterval([
            log_interval.Subinterval('c1', 'log', 1, log_frame[0], log_frame[1])
        ]).to_meta(),
    })


@pytest.fixture()
def raw_log_dir(yt_client, yt_root):
    dir_path = yt.ypath_join(yt_root, 'raw_log_dir')
    yt_client.create('map_node', dir_path)
    _create_raw_table(yt_client, dir_path, run_id='2020-08-03T11:00:00', log_frame=(1, 3))
    _create_raw_table(yt_client, dir_path, run_id='2020-09-03T11:00:00', log_frame=(3, 5))
    _create_raw_table(yt_client, dir_path, run_id='2020-09-11T11:00:00', log_frame=(5, 7))
    yield dir_path
    yt_client.remove(dir_path, force=True, recursive=True)


def test_generate_act_meta_raw_log(yt_client, raw_log_dir):
    return generate_act_meta.run_job(
        yt_client,
        last_generated_meta=INITIAL_ACT_META,
        last_acted_meta=INITIAL_ACT_META,
        tariffed_dir=raw_log_dir,
    )
