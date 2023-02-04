import logging
import time
import yp.data_model as data_model

from enum import Enum
from infra.dctl.src.lib import stage, pod, project
from yp.common import wait
from yt.wrapper import ypath_join


class Status(Enum):
    READY = 'Ready'
    PROGRESS = 'InProgress'
    NONE = '-'


def get_stage_status(stage_name, yp_client, revision=None):
    status = stage.get_status(stage_name, yp_client)
    for row in status:
        row.border = False
        row.header = False
        if revision is not None:
            spec_revision = row.get_string(fields=["SpecRev"]).strip()
            if int(spec_revision) != revision:
                return Status.NONE

        unit_status = row.get_string(fields=["DeployStatus"]).strip()
        if unit_status == Status.NONE.value:
            return Status.NONE
        if unit_status == Status.PROGRESS.value:
            return Status.PROGRESS
    return Status.READY


def wait_for_pod_readiness(replica_set_name, clients, revision, limit=10,
                           retries=None, sleep_backoff=None):
    retries = retries or _get_param('pod', 'max_retries', 1800, int)
    sleep_backoff = sleep_backoff or _get_param('pod', 'sleep_backoff', 1, float)
    for _ in range(retries):
        status = True
        for row in pod.list_objects(replica_set_name, clients, limit):
            logging.info(row)
            if get_row_value(row, "Pod") != Status.READY.value or \
                    str(revision) != get_row_value(row, "Target") or \
                    get_row_value(row, "Target") != get_row_value(row, "Current"):
                status = False
        logging.info('%s status: %s', replica_set_name, status)
        if status:
            return True
        time.sleep(sleep_backoff)
    return False


def wait_for_stage_status(stage_name, expected_status, yp_client, revision=None,
                          retries=None, sleep_backoff=None):
    retries = retries or _get_param('stage', 'max_retries', 1800, int)
    sleep_backoff = sleep_backoff or _get_param('stage', 'sleep_backoff', 1, float)
    for _ in range(retries):
        status = get_stage_status(stage_name, yp_client, revision)
        logging.info('%s status: %s', stage_name, status.value)
        if status == expected_status:
            return True
        time.sleep(sleep_backoff)
    get_stage_info(stage_name, yp_client)
    return False


def get_stage_info(stage_name, yp_client):
    stage_status = stage.get_status(stage_name, yp_client)
    logging.info("----getting info about deploy---")
    logging.info(stage_status)


def wait_for_stage_readiness(stage_name, yp_client, **kwargs):
    return wait_for_stage_status(stage_name, Status.READY, yp_client, **kwargs)


def wait_for_stage_in_progress(stage_name, yp_client, **kwargs):
    return wait_for_stage_status(stage_name, Status.PROGRESS, yp_client, **kwargs)


def get_row_value(row, *fields):
    return row.get_string(header=False, border=False, fields=fields).strip()


def find_by_substr(container, key):
    for val in container:
        if key in val.id:
            return val
    return None


def _get_param(obj_type, param, default, param_type):
    import yatest.common
    return param_type(yatest.common.get_param(f'wait_{obj_type}_{param}', default))


def await_project_ownership(project_name, user, client, orchid_client,
                            retries=None, sleep_backoff=None):
    retries = retries or _get_param('project', 'max_retries', 600, int)
    sleep_backoff = sleep_backoff or _get_param('project', 'sleep_backoff', 1, float)

    attempt = 0
    ownership_postfix = f'{user}.OWNER'

    group_id = None
    while attempt < retries:
        prj = project.get(project_name, client)
        if prj:
            acls = prj.meta.acl
            # ownership_postfix += ":" + prj.meta.uuid # TODO:fixme
            logging.info('project %s acl: %s', project_name, acls)
            logging.info('ownership_postfix is : %s', ownership_postfix)

            for acl in acls:
                for subject in acl.subjects:
                    if ownership_postfix in subject:
                        group_id = subject
                        break
        if group_id:
            break
        time.sleep(sleep_backoff)
        attempt += 1
    if attempt >= retries or group_id is None:
        raise Exception('Unable to find owners for %s' % project_name)

    found_group = None
    while attempt < retries:
        group = client.get(object_type=data_model.OT_GROUP, object_id=group_id)
        if group:
            group_members = group.spec.members
            logging.info('project: %s, group: %s, members: %s', project_name, group_id, group.spec.members)
            if user in group_members:
                found_group = group
                break
        time.sleep(sleep_backoff)
        attempt += 1

    if attempt >= retries or found_group is None:
        raise Exception('Unable to find user membership for %s' % project_name)

    if orchid_client is not None:
        _sync_access_control(client, orchid_client)
    else:
        # DEPLOY-3779: well... we do not have read permissions for YP production
        # so we have to do it in unreliable manner. Default ClusterStateUpdatePeriod is
        # 1 second, but it's better to wait longer
        sync_await = _get_param('project', 'access_sync', 5, int)
        logging.warning(f'Unreliable access control sync requested. Sleeping for {sync_await}s to ensure acl '
                        f'rules was applied on YP side. In case of stage creation failure consider enlarging '
                        f'wait_project_access_sync test parameter')
        time.sleep(sync_await)


def _sync_access_control(yp_xdc_client_fixture, orchid_client,
                         retries=None, sleep_backoff=None):
    retries = retries or _get_param('cluster_state', 'max_retries', 60, int)
    sleep_backoff = sleep_backoff or _get_param('cluster_state', 'sleep_backoff', 1, float)

    master_addresses = orchid_client.get_instances()
    expected_timestamp = yp_xdc_client_fixture.client.generate_timestamp()
    synced_master_addresses = set()

    def is_state_updated():
        for master_address in master_addresses:
            if master_address in synced_master_addresses:
                continue

            current_timestamp = orchid_client.get(master_address, "/access_control/cluster_state_timestamp")
            if current_timestamp > expected_timestamp:
                synced_master_addresses.add(master_address)
            else:
                return False
        return True

    wait(is_state_updated, iter=retries, sleep_backoff=sleep_backoff)


class YpOrchidClient(object):
    def __init__(self, yt_client):
        self._yt_client = yt_client
        self._instances_path = "//yp/master/instances"
        self._instance_addresses = self._yt_client.list(self._instances_path)
        assert len(self._instance_addresses) > 0

    def get(self, instance_address, path, *args, **kwargs):
        absolute_path = ypath_join(
            self._instances_path,
            instance_address,
            "/orchid",
            path,
        )
        return self._yt_client.get(absolute_path, *args, **kwargs)

    def get_instances(self):
        return list(self._instance_addresses)
