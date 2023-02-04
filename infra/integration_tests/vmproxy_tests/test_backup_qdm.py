import requests
import uuid
import pytest
from infra.qyp.proto_lib import vmset_pb2, vmset_api_pb2


def test_backup_vm_raw(vmproxy_client, yp_cluster, auth_user, pod_id, create_vm, status_vm):
    create_vm(wait_status_change=True,
              raise_if_exists=True,
              vmagent_version='0.25',
              custom_pod_resources={
                    'vmagent': vmset_pb2.PodResource(
                        url='rbtorrent:c7e00acc507e9acd8f69a3998f441d2cdb22c5c2',
                        dynamic=True
                    )
              },
              image_type='RAW'
              )

    vmproxy_client.backup(
        cluster=yp_cluster,
        pod_id=pod_id,
        backup_storage=vmset_api_pb2.QDM
    )


def test_backup_vm_delta(vmproxy_client, yp_cluster, auth_user, pod_id, create_vm, status_vm):
    create_vm(wait_status_change=True,
              raise_if_exists=True,
              vmagent_version='0.25',
              custom_pod_resources={
                  'vmagent': vmset_pb2.PodResource(
                      url='rbtorrent:c7e00acc507e9acd8f69a3998f441d2cdb22c5c2',
                      dynamic=True
                  )
              },
              image_type='DELTA',
              )

    vmproxy_client.backup(
        cluster=yp_cluster,
        pod_id=pod_id,
        backup_storage=vmset_api_pb2.QDM
    )
