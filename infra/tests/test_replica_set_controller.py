import pytest
import logging

import yp.data_model as data_model
from infra.dctl.src.lib import stage
from infra.dctl.src.lib import replica_set

import utils


@pytest.fixture(scope='function')
def stage_name(suffix_fixture):
    return 'it-replica-set-' + suffix_fixture


def test_replica_set(stage_name, stage_fixture, yp_client_fixture, yp_xdc_client_fixture):
    logging.info('----replica set test start----')
    assert stage.get(stage_name, yp_xdc_client_fixture)
    assert utils.wait_for_stage_readiness(stage_name, yp_xdc_client_fixture)
    rs = replica_set.get_rs(stage_name + ".DeployUnit1", yp_client_fixture)
    assert rs.status.ready_condition.status == data_model.CS_TRUE
    logging.info('----replica set end----')
