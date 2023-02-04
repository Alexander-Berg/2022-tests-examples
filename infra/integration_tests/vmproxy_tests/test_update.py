import requests
import uuid
import pytest
from infra.qyp.proto_lib import vmset_pb2


def test_update_vmagent(create_vm, pod_id, vmproxy_client, yp_cluster):
    create_vm(wait_status_change=True,
              raise_if_exists=True,
              vmagent_version='0.25',
              custom_pod_resources={
                  'vmagent': vmset_pb2.PodResource(
                      url='rbtorrent:27f459fb09649505a0e804bc2cfd316f06762b8a',
                      dynamic=True
                  )
              },
              image_type='RAW'
              )

    status = vmproxy_client.get_status(cluster=yp_cluster, pod_id=pod_id)
    assert status.vmagent_version == '0.25'
    vmproxy_client.update(
        pod_id=pod_id,
        abc=None,
        cluster=yp_cluster,
        update_vmagent=True,
        update_vmagent_version='0.26',
        custom_pod_resources={
            'vmagent': vmset_pb2.PodResource(
                url='rbtorrent:4c6e949e28fae0b13f170900a8da866fb59b3894',
                dynamic=True,
            ),
        },
    )
    vmproxy_client.wait_status_change(cluster=yp_cluster, pod_id=pod_id)
    status = vmproxy_client.get_status(cluster=yp_cluster, pod_id=pod_id)
    assert status.vmagent_version == '0.26'

    vmproxy_client.update(
        pod_id=pod_id,
        abc=None,
        cluster=yp_cluster,
        update_vmagent=True,
        update_vmagent_version='0.27',
        custom_pod_resources={
            'vmagent': vmset_pb2.PodResource(
                url='rbtorrent:73342b79bfa0b55fef7d530e4dfdbf9fe02158f4',
                dynamic=True,
            ),
        },
    )
    vmproxy_client.wait_status_change(cluster=yp_cluster, pod_id=pod_id)
    status = vmproxy_client.get_status(cluster=yp_cluster, pod_id=pod_id)
    assert status.vmagent_version == '0.27'





