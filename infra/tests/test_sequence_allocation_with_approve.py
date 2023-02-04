import logging
import pytest
import time
from infra.dctl.src.lib import pod
from infra.dctl.src.lib import yp_client
from yp.client import find_token

import utils

SAS_TEST_CLUSTER = "sas-test"
MAN_TEST_CLUSTER = "man-pre"
CLUSTER_SEQUENCE = [MAN_TEST_CLUSTER, SAS_TEST_CLUSTER]


@pytest.fixture(scope='function')
def stage_name(suffix_fixture):
    return 'it-sequence_allocation-' + suffix_fixture


@pytest.fixture(scope='function')
def stage_template():
    return '/stage-multi-new.yml'


def test_sequence_allocation_with_approve(stage_name, yp_xdc_client_fixture, stage_spec, stage_template,
                                          yp_client_fixture, stage_fixture,
                                          yp_user, yp_cluster):
    logging.info('----sequence allocation with approve test start----')
    if yp_cluster not in CLUSTER_SEQUENCE:
        logging.info('----Test has skipped, cluster not support----')
        return

    du_name = "DeployUnit1"
    rs1_id = stage_name + "." + du_name

    client_sas = yp_client.YpClient(SAS_TEST_CLUSTER, find_token(), yp_user)
    client_man = yp_client.YpClient(MAN_TEST_CLUSTER, find_token(), yp_user)

    clients = {SAS_TEST_CLUSTER: client_sas, MAN_TEST_CLUSTER: client_man}

    revision = 1

    step_in_active = 0
    first_was_checked = False
    second_was_checked = False
    approve(yp_client_fixture, du_name, CLUSTER_SEQUENCE[0], revision, stage_name)
    for _ in range(200):
        pods = list(pod.list_objects(rs1_id, clients, 10))
        if len(pods) == 1 and not first_was_checked:
            logging.info("Current cluster: " + utils.get_row_value(pods[0], "Cluster"))
            if utils.get_row_value(pods[0], "Cluster") == CLUSTER_SEQUENCE[0]:
                step_in_active += 1
                first_was_checked = True
                assert wait_for_pod_readiness(rs1_id, CLUSTER_SEQUENCE[0], clients, 1)
                approve(yp_client_fixture, du_name, CLUSTER_SEQUENCE[1], revision, stage_name)
        if len(pods) == 2 and not second_was_checked:
            for pod_elem in pods:
                logging.info("Current cluster: " + utils.get_row_value(pod_elem, "Cluster"))
                if utils.get_row_value(pod_elem, "Cluster") == CLUSTER_SEQUENCE[1]:
                    return
        time.sleep(2)
    assert step_in_active != 2
    logging.info('----sequence allocation test end----')


def approve(yp_client_local, du_name, cluster_name, revision, stage_id):
    yp_client_local.client.update_object("stage", stage_id, set_updates=[
        {
            "path": "/control/approve",
            "value": {
                "options": {
                    "revision": revision,
                    "cluster": cluster_name,
                    "deploy_unit": du_name
                }
            },
        }
    ])


def wait_for_pod_readiness(replica_set_name, cluster, clients, revision, limit=10):
    for _ in range(600):
        for row in pod.list_objects(replica_set_name, clients, limit):
            logging.info('----row---- \n %s', row)
            if utils.get_row_value(row, "Pod") == utils.Status.READY.value and \
                    str(revision) == utils.get_row_value(row, "Target") and \
                    utils.get_row_value(row, "Target") == utils.get_row_value(row, "Current") and \
                    utils.get_row_value(row, "Cluster") == cluster:
                return True
        time.sleep(4)
    return False
