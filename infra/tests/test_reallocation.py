import pytest
import logging

from infra.dctl.src.lib import stage, pod
from yp.common import YpNoSuchObjectError
import utils


@pytest.fixture(scope='function')
def stage_template(suffix_fixture):
    return '/stage-reallocation.yml'


@pytest.fixture(scope='function')
def stage_name(suffix_fixture):
    return 'reallocation-' + suffix_fixture


def test_reallocation(stage_name, stage_fixture, stage_template, stage_spec, yp_client_fixture, yp_xdc_client_fixture, yp_cluster):
    logging.info('----Reallocation test start----')
    rs1_id = stage_name + ".DeployUnit1"
    rs2_id = stage_name + ".DeployUnit2"
    stage_data = stage.get(stage_name, yp_xdc_client_fixture)
    assert stage_data
    assert utils.wait_for_stage_readiness(stage_name, yp_xdc_client_fixture)
    clients = {yp_cluster: yp_client_fixture}
    pod_table_1 = to_dict(pod.list_objects(rs1_id, clients, 10))
    pod_table_2 = to_dict(pod.list_objects(rs2_id, clients, 10))
    stage_data.spec.deploy_units["DeployUnit2"] \
        .replica_set.replica_set_template \
        .pod_template_spec.spec.pod_agent_payload.spec \
        .workloads[0].start.command_line = "/simple_http_server 81 'Bye @aidenne!'"
    stage.put(stage_data, yp_cluster, None, None, None, yp_xdc_client_fixture)
    assert utils.wait_for_pod_readiness(rs1_id, clients, 1)
    assert utils.wait_for_pod_readiness(rs2_id, clients, 2)

    pod_table_1_v2 = to_dict(pod.list_objects(rs1_id, clients, 10))
    pod_table_2_v2 = to_dict(pod.list_objects(rs2_id, clients, 10))

    assert pod_table_1 == pod_table_1_v2
    assert pod_table_2 != pod_table_2_v2
    stage.remove(stage_name, yp_xdc_client_fixture)
    with pytest.raises(YpNoSuchObjectError):
        stage.get(stage_name, yp_xdc_client_fixture)
    logging.info('----Reallocation test end----')


def to_dict(table):
    result = dict()
    for row in table:
        result[row.get_string(header=False, border=False, fields=["PodID"]).strip()] = \
            row.get_string(border=False, header=False, fields=["Time"]).strip()
    return result
