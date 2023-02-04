import pytest
from infra.qyp.proto_lib import vmset_api_pb2


@pytest.mark.parametrize("yp_cluster,pod_id", [('VLA', 'idyachkov-test-vm')])
def test_update_vm_labels(vmproxy_client, yp_cluster, pod_id):
    new_labels = {
        'test_label': 'any_value'
    }
    vmproxy_client.update_labels(yp_cluster, pod_id, labels=new_labels)

    vm = vmproxy_client.get_vm(yp_cluster, pod_id)

    assert 'test_label' in vm.spec.labels

    # test clear labels
    vmproxy_client.update_labels(yp_cluster, pod_id, labels={})

    vm = vmproxy_client.get_vm(yp_cluster, pod_id)

    assert not vm.spec.labels['qyp_test_label']


