import pytest
import logging
import time

from infra.dctl.src.lib import stage
from infra.dctl.src.lib import replica_set
import utils


@pytest.fixture(scope='function')
def stage_name(suffix_fixture):
    return 'it-logrotate-' + suffix_fixture


def test_add_sidecar_logrotate(stage_name, stage_fixture, yp_client_fixture, yp_xdc_client_fixture):
    logging.info('----logrotate test start----')
    assert stage.get(stage_name, yp_xdc_client_fixture)
    assert utils.wait_for_stage_readiness(stage_name, yp_xdc_client_fixture)
    time.sleep(5)
    rs = replica_set.get_rs(stage_name + ".DeployUnit1", yp_client_fixture)
    logrotate_layer = None
    pod_agent_spec = rs.spec.pod_template_spec.spec.pod_agent_payload.spec

    logging.info('pod_agent_spec: %s', pod_agent_spec)

    for layer in pod_agent_spec.resources.layers:
        if "logrotate" in layer.id:
            logrotate_layer = layer
    assert logrotate_layer

    logrotate_ref = None
    for box in pod_agent_spec.boxes:
        if box.id == 'Box1':
            for ref in box.rootfs.layer_refs:
                if logrotate_layer.id == ref:
                    logrotate_ref = ref
    assert logrotate_ref

    logrotate_wl = None
    for mutable_workload in pod_agent_spec.mutable_workloads:
        if mutable_workload.workload_ref == "logrotate_Box1":
            logrotate_wl = mutable_workload
    assert logrotate_wl
    logging.info('----logrotate test end----')
