import logging

import pytest
from infra.dctl.src.lib import stage
from collections import namedtuple

import time
import utils


StageTimestamps = namedtuple('StageTimestamps', ['spec', 'target_spec', 'ready'])


@pytest.fixture(scope='function')
def stage_name(suffix_fixture):
    return 'redeploy-' + suffix_fixture


def test_redeploy_restart(yp_cluster, stage_name, stage_spec, stage_fixture, yp_client_fixture, yp_xdc_client_fixture):
    logging.info('---Redeploy test start----')

    assert stage.get(stage_name, yp_xdc_client_fixture)
    assert utils.wait_for_stage_readiness(stage_name, yp_xdc_client_fixture)
    assert utils.wait_for_pod_readiness(stage_name + '.DeployUnit1', {yp_cluster: yp_client_fixture}, 1)

    stage_data = stage.get(stage_name, yp_xdc_client_fixture)
    assert stage_data
    stage_data
    logging.info('stage data: %s ', stage_data.status.deploy_units['DeployUnit1'].ready)

    origin_ts = _get_timestamps(stage_data)

    # update spec timestamp
    stage.put(stage_data, yp_cluster, None, None, None, yp_xdc_client_fixture)
    assert utils.wait_for_stage_readiness(stage_name, yp_xdc_client_fixture, revision=2)

    updated_stage_data = stage.get(stage_name, yp_xdc_client_fixture)

    while _get_timestamps(updated_stage_data).spec == origin_ts.spec:
        updated_stage_data = stage.get(stage_name, yp_xdc_client_fixture)
        time.sleep(0.5)

    logging.info('updated stage data: %s ', updated_stage_data.status.deploy_units['DeployUnit1'].ready)
    updated_ts = _get_timestamps(updated_stage_data)
    assert updated_ts.spec != origin_ts.spec
    assert updated_ts.ready.seconds == origin_ts.ready.seconds
    logging.info('----Redeploy test end----')


def _get_timestamps(stage_data, du_name='DeployUnit1'):
    ready_timestamp = stage_data.status.deploy_units[du_name].ready.last_transition_time
    target_spec_timestamp = stage_data.status.deploy_units[du_name].target_spec_timestamp
    spec_timestamp = stage_data.status.spec_timestamp
    return StageTimestamps(spec_timestamp, target_spec_timestamp, ready_timestamp)
