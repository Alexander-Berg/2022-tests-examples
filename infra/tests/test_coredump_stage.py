import logging
import time

import pytest
from infra.dctl.src.lib import replica_set
from infra.dctl.src.lib import stage

import utils

EContainerULimit_CORE = 1


@pytest.fixture(scope='function')
def stage_name(suffix_fixture):
    return 'it-coredump-' + suffix_fixture


@pytest.fixture(scope='function')
def stage_template(suffix_fixture):
    return '/stage-coredump.yml'


def test_coredump_stage(stage_name, stage_template, stage_fixture, yp_client_fixture, yp_xdc_client_fixture):
    logging.info('----Coredump test start----')
    assert stage.get(stage_name, yp_xdc_client_fixture)
    assert utils.wait_for_stage_readiness(stage_name, yp_xdc_client_fixture)
    time.sleep(5)

    rs = replica_set.get_rs(stage_name + ".DeployUnit1", yp_client_fixture)
    ps = rs.spec.pod_template_spec.spec.pod_agent_payload.spec

    coredump_wl = utils.find_by_substr(ps.workloads, "Box1-Workload1")
    assert coredump_wl
    assert coredump_wl.start.core_command
    assert coredump_wl.ulimit_soft
    assert coredump_wl.ulimit_soft[0].name == EContainerULimit_CORE and coredump_wl.ulimit_soft[0].value == 104857600

    gdb_layer = utils.find_by_substr(ps.resources.layers, "gdb-layer")
    assert gdb_layer
    assert gdb_layer.url

    coredump_box = utils.find_by_substr(ps.boxes, "Box1")
    coredump_volume = utils.find_by_substr(ps.volumes, "COREDUMP_VOLUME")
    assert coredump_box
    assert coredump_volume
    volume_ref = None
    for volume in coredump_box.volumes:
        if volume.volume_ref == coredump_volume.id:
            volume_ref = volume
    assert volume_ref

    instancectl_binary = utils.find_by_substr(ps.resources.static_resources, "instancectl-binary")
    assert instancectl_binary
    assert instancectl_binary.url

    logging.info('----Coredump test end----')
