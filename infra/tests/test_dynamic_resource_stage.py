import logging

import pytest
import time
from infra.dctl.src.lib import dynamic_resource
from infra.dctl.src.lib import replica_set
from infra.dctl.src.lib import stage

import utils


@pytest.fixture(scope='function')
def stage_name(suffix_fixture):
    return 'it-dynamic_resource-' + suffix_fixture


@pytest.fixture(scope='function')
def stage_template(suffix_fixture):
    return '/stage-dr.yml'


def test_dynamic_resource_stage(stage_name, stage_fixture, stage_template, yp_client_fixture, yp_xdc_client_fixture, yp_cluster):
    logging.info('----Dynamic resource test start----')
    stage_data = stage.get(stage_name, yp_xdc_client_fixture)
    assert stage_data
    assert utils.wait_for_stage_readiness(stage_name, yp_xdc_client_fixture)
    time.sleep(60)

    rs = replica_set.get_rs(stage_name + ".DeployUnit1", yp_client_fixture)
    ps = rs.spec.pod_template_spec.spec.pod_agent_payload.spec

    dru_workload = utils.find_by_substr(ps.workloads, "dru")
    assert dru_workload
    assert dru_workload.box_ref == 'Box1'
    assert '/dru' in dru_workload.start.command_line

    dru_layer = utils.find_by_substr(ps.resources.layers, "dru-layer")
    assert dru_layer
    assert dru_layer.url

    box1 = utils.find_by_substr(ps.boxes, "Box1")
    assert box1
    assert box1.bind_skynet is True

    resource_id = stage_name + ".DeployUnit1.TestRes1"
    pod_set_id = stage_name + '.DeployUnit1'
    dr = dynamic_resource.get(resource_id, yp_client_fixture)
    assert dr

    assert dr.meta.pod_set_id == pod_set_id
    assert dr.spec.deploy_groups[0].storage_options.box_ref == box1.id
    assert 'rbtorrent:07199bbc031235a032d5ac2c4f22c2f5d461da6b' in dr.spec.deploy_groups[0].urls[0]

    logging.info("---Update dynamic resource----")
    stage_data.spec.dynamic_resources["TestRes1"].dynamic_resource.revision = 2
    try:
        stage.put(stage_data, yp_cluster, None, None, None, yp_xdc_client_fixture)
    except Exception as e:
        logging.info("Stage put retry due error: %s", e)
        stage.put(stage_data, yp_cluster, None, None, None, yp_xdc_client_fixture)

    assert utils.wait_for_stage_readiness(stage_name, yp_xdc_client_fixture, revision=2)
    time.sleep(60)
    attempts = 600
    version = 1
    while attempts >= 0 and version != 2:
        status = stage.get(stage_name, yp_xdc_client_fixture).status
        version = status.dynamic_resources['TestRes1'].current_target.dynamic_resource.revision
        attempts = attempts - 1
        time.sleep(1)
    assert version == 2
    logging.info('----Dynamic resource test end----')
