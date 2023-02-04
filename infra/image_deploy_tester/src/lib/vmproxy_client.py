import getpass

from infra.qyp.proto_lib import vmset_api_pb2, vmset_pb2, vmset_api_stub, vmagent_pb2
from sepelib.core import config

import nanny_rpc_client


class VmproxyClient(object):
    MEMORY_GAP = 1024 ** 3
    DEFAULT_REQ_TIMEOUT = 300
    QEMU_PERSISTENT_VOLUME = '/qemu-persistent'

    def __init__(self, url, token, req_timeout=None):
        timeout = req_timeout or self.DEFAULT_REQ_TIMEOUT
        rpc = nanny_rpc_client.RetryingRpcClient(url + '/api', oauth_token=token, request_timeout=timeout)
        self.stub = vmset_api_stub.VmSetServiceStub(rpc)
        self.user = config.get_value("auth.username", None) or getpass.getuser()

    def create_vm(self, pod_id, image_url, vm_label, node_id=None):
        """
        :type pod_id: str
        :type image_url: str
        :type vm_label: str
        :type node_id: str
        """
        vm_spec = config.get_value('vm_spec')
        req = vmset_api_pb2.CreateVmRequest()
        req.meta.id = pod_id
        req.meta.auth.owners.logins.append(self.user)
        req.spec.account_id = vm_spec['account_id']
        req.spec.type = vmset_pb2.VMSpec.QEMU_VM
        req.spec.qemu.autorun = True
        req.spec.qemu.vm_type = vmagent_pb2.VMConfig.LINUX
        req.spec.qemu.network_id = vm_spec['network_id']
        req.spec.qemu.node_segment = vm_spec['node_segment']
        req.spec.qemu.resource_requests.dirty_memory_limit = 0
        req.spec.qemu.resource_requests.memory_limit = vm_spec['memory']
        req.spec.qemu.resource_requests.anonymous_memory_limit = 0
        req.spec.qemu.resource_requests.vcpu_guarantee = vm_spec['vcpu_guarantee'] * 1000
        req.spec.qemu.resource_requests.vcpu_limit = vm_spec['vcpu_limit'] * 1000
        req.spec.qemu.resource_requests.memory_guarantee = vm_spec['memory']
        req.spec.labels[vm_label] = '1'
        v = req.spec.qemu.volumes.add()
        v.name = self.QEMU_PERSISTENT_VOLUME
        v.capacity = vm_spec['volume_size']
        v.storage_class = vm_spec['storage_class']
        v.image_type = vmagent_pb2.VMDisk.RAW
        v.resource_url = image_url
        req.spec.qemu.io_guarantees_per_storage[v.storage_class] = vm_spec['io_guarantees_per_storage'][v.storage_class]
        if node_id:
            h = req.spec.scheduling.hints.add()
            h.node_id = node_id
            h.strong = True
        self.stub.create_vm(req)

    def get_vm(self, pod_id):
        """
        :type pod_id: str
        :rtype: vmset_pb2.VM
        """
        req = vmset_api_pb2.GetVmRequest()
        req.vm_id = pod_id
        return self.stub.get_vm(req)

    def remove_vm(self, pod_id):
        """
        :type pod_id: str
        """
        req = vmset_api_pb2.DeallocateVmRequest()
        req.id = pod_id
        self.stub.deallocate_vm(req)
