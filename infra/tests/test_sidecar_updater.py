import logging

import pytest
from infra.dctl.src.lib import replica_set
from infra.dctl.src.lib import stage
from infra.dctl.src.lib import yputil

import utils

POD_AGENT_OLD_REVISION = 1718693439
POD_AGENT_NEW_REVISION = 1741144396
POD_AGENT_OLD_URL = "rbtorrent:c943a62cdc72d8e1fab2548d2e6379cb5fd18704"
POD_AGENT_NEW_URL = "rbtorrent:3a3553de04437448c23aa807a3b09463a49da359"


@pytest.fixture(scope='function')
def stage_name(suffix_fixture):
    return 'it-sidecar-updater-' + suffix_fixture


@pytest.fixture(scope='function')
def stage_template(suffix_fixture):
    return '/stage-sidecars.yml'


def test_sidecar_updater(stage_name, stage_fixture, yp_client_fixture, yp_xdc_client_fixture, yp_cluster):
    logging.info('---Sidecar updater test start----')

    stage_data = stage.get(stage_name, yp_xdc_client_fixture)
    assert stage_data
    assert stage_data.spec.deploy_units["DeployUnit1"].pod_agent_sandbox_info.revision == POD_AGENT_OLD_REVISION

    assert utils.wait_for_stage_readiness(stage_name, yp_xdc_client_fixture)

    rs1 = replica_set.get_rs(stage_name + ".DeployUnit1", yp_client_fixture)
    ps1 = rs1.spec.pod_template_spec.spec.pod_agent_payload.meta
    assert ps1.url == POD_AGENT_OLD_URL

    rs2 = replica_set.get_rs(stage_name + ".DeployUnit2", yp_client_fixture)
    ps2 = rs2.spec.pod_template_spec.spec.pod_agent_payload.meta
    assert ps2.url == POD_AGENT_OLD_URL

    yputil.set_label(stage_data.labels, 'du_sidecar_autoupdate_revision', {
        'DeployUnit1': {'podBin': POD_AGENT_NEW_REVISION}
    })
    stage_data.spec.deploy_units["DeployUnit1"].revision = 2
    stage.put(stage_data, yp_cluster, None, None, None, yp_xdc_client_fixture)

    logging.info('----After update----')

    stage_data = stage.get(stage_name, yp_xdc_client_fixture)
    assert stage_data
    assert stage_data.spec.deploy_units["DeployUnit1"].pod_agent_sandbox_info.revision == POD_AGENT_NEW_REVISION

    assert utils.wait_for_stage_in_progress(stage_name, yp_xdc_client_fixture)
    assert utils.wait_for_stage_readiness(stage_name, yp_xdc_client_fixture)

    rs1 = replica_set.get_rs(stage_name + ".DeployUnit1", yp_client_fixture)
    ps1 = rs1.spec.pod_template_spec.spec.pod_agent_payload.meta
    assert ps1.url == POD_AGENT_NEW_URL

    rs2 = replica_set.get_rs(stage_name + ".DeployUnit2", yp_client_fixture)
    ps2 = rs2.spec.pod_template_spec.spec.pod_agent_payload.meta
    assert ps2.url == POD_AGENT_OLD_URL

    logging.info('----Sidecar updater test end----')
