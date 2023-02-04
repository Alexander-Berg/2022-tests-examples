import requests
import uuid
import pytest
from infra.qyp.proto_lib import vmset_pb2


def test_create_vm(vmproxy_client, yp_cluster, auth_user, pod_id, create_vm, status_vm):
    create_vm(wait_status_change=True,
              raise_if_exists=False,
              vmagent_version='0.25',
              custom_pod_resources={
                    'vmagent': vmset_pb2.PodResource(
                        url='rbtorrent:ea3af1699ee6ebd0344b04b89b2d10dc29e57b97',
                        dynamic=True
                    )
              },
              image_type='RAW'
    )

    vm_status = status_vm()
    assert vm_status
