import base64
import datetime
import freezegun
import mock
import pytest
import semantic_version
from google.protobuf import json_format
from yt import yson

from infra.qyp.proto_lib import vmagent_api_pb2, vmagent_pb2, vmset_pb2
from infra.qyp.vmproxy.src import vm_instance
from infra.qyp.vmproxy.src.action import config as config_action
from infra.qyp.vmproxy.src.action import helpers as action_helpers
from infra.qyp.vmproxy.src.lib.yp import yputil


def test_validate_config_params():
    vmagent_version = semantic_version.Version('0.9', partial=True)
    vm_spec = vmset_pb2.VMSpec()
    vm_spec.qemu.resource_requests.vcpu_guarantee = 2000
    vm_spec.qemu.resource_requests.vcpu_limit = 3200
    vm_spec.qemu.resource_requests.memory_guarantee = 2 * 1024 ** 3
    v = vm_spec.qemu.volumes.add()
    v.name = yputil.MAIN_VOLUME_NAME
    v.capacity = 20 * 1024 ** 3
    config_pb_ = vmagent_pb2.VMConfig()
    config_pb_.vcpu = 2
    config_pb_.mem = 1 * 1024 ** 3
    config_pb_.disk.delta_size = 10 * 1024 ** 3
    config_pb_.disk.resource.rb_torrent = 'rbtorrent:1'
    # Case 1: ok
    config_pb = vmagent_pb2.VMConfig()
    config_pb.CopyFrom(config_pb_)
    with mock.patch('uuid.uuid4', return_value='new_version'):
        config_action.validate_config_params(config_pb, vm_spec, vmagent_version)
    assert config_pb.id == 'new_version'
    # Case 2: cpu unequal
    config_pb = vmagent_pb2.VMConfig()
    config_pb.CopyFrom(config_pb_)
    config_pb.vcpu = 33
    with pytest.raises(ValueError):
        config_action.validate_config_params(config_pb, vm_spec, vmagent_version)
    # Case 3: memory not set
    config_pb = vmagent_pb2.VMConfig()
    config_pb.CopyFrom(config_pb_)
    config_pb.mem = 0
    with pytest.raises(ValueError):
        config_action.validate_config_params(config_pb, vm_spec, vmagent_version)
    # Case 4: memory greater than allocation (mem + gap)
    config_pb = vmagent_pb2.VMConfig()
    config_pb.CopyFrom(config_pb_)
    config_pb.mem = 2 * 1024 ** 3
    with pytest.raises(ValueError):
        config_action.validate_config_params(config_pb, vm_spec, vmagent_version)
    # Case 5: disk size not set
    config_pb = vmagent_pb2.VMConfig()
    config_pb.CopyFrom(config_pb_)
    config_pb.disk.delta_size = 0
    with pytest.raises(ValueError):
        config_action.validate_config_params(config_pb, vm_spec, vmagent_version)
    # Case 6: disk size greater than allocation
    config_pb = vmagent_pb2.VMConfig()
    config_pb.CopyFrom(config_pb_)
    config_pb.disk.delta_size = 20 * 1024 ** 3 + 1
    with pytest.raises(ValueError):
        config_action.validate_config_params(config_pb, vm_spec, vmagent_version)
    # Case 7: delta size not set in new version
    config_pb = vmagent_pb2.VMConfig()
    config_pb.CopyFrom(config_pb_)
    config_pb.disk.delta_size = 0
    new_version = semantic_version.Version('0.10', partial=True)
    config_action.validate_config_params(config_pb, vm_spec, new_version)
    # Case 8: pass config id
    config_pb = vmagent_pb2.VMConfig()
    config_pb.CopyFrom(config_pb_)
    config_pb.id = 'some_real_id'
    config_action.validate_config_params(config_pb, vm_spec, vmagent_version)

    # Case 9: pass disc resource rb_torrent
    config_pb = vmagent_pb2.VMConfig()
    config_pb.CopyFrom(config_pb_)
    config_pb.id = 'some_real_id'
    config_pb.disk.resource.rb_torrent = ""
    with pytest.raises(ValueError):
        config_action.validate_config_params(config_pb, vm_spec, vmagent_version)

    config_pb.disk.resource.rb_torrent = "wrong-url"
    with pytest.raises(ValueError):
        config_action.validate_config_params(config_pb, vm_spec, vmagent_version)

    config_pb.disk.resource.rb_torrent = "http://anydomain.any/file"
    config_action.validate_config_params(config_pb, vm_spec, vmagent_version)

    config_pb.disk.resource.rb_torrent = "https://anydomain.any/file"
    config_action.validate_config_params(config_pb, vm_spec, vmagent_version)

    config_pb.disk.resource.rb_torrent = "rbtorrent:test-value"
    config_action.validate_config_params(config_pb, vm_spec, vmagent_version)

    assert config_pb.id == 'some_real_id'

    # Case 10: Empty qemu_volume
    del vm_spec.qemu.volumes[:]
    with pytest.raises(ValueError):
        config_action.validate_config_params(config_pb, vm_spec, vmagent_version)


def fill_pod_spec(pod_, pod_set_):
    yputil.cast_dict_to_attr_dict({"owners": {'groups': ['1', '2'], 'logins': ['test1', 'test2']}},
                                  pod_.annotations)
    yputil.cast_dict_to_attr_dict({"version": "1"}, pod_.labels)
    yputil.cast_dict_to_attr_dict({"version": "1"}, pod_set_.labels)
    i = pod_.spec.iss.instances.add()
    v = i.entity.instance.volumes.add()
    v.mountPoint = yputil.MAIN_VOLUME_POD_MOUNT_PATH
    v.storage = '/place'
    v.quotaBytes = 10 * 1024 ** 3


def test_run_vmagent_config(pod, pod_set, pod_ctl_mock, ctx_mock):
    fill_pod_spec(pod, pod_set)
    data_pb = vmagent_api_pb2.VMActionRequest()
    data_pb.action = vmagent_api_pb2.VMActionRequest.PUSH_CONFIG
    data_pb.config.id = '1'
    data_pb.config.vcpu = 2
    data_pb.config.mem = 2 * 1024 ** 3
    data_pb.config.disk.delta_size = 10 * 1024 ** 3
    data_pb.config.disk.resource.rb_torrent = 'rbtorrent:1'
    encoded = base64.b64encode(data_pb.SerializeToString())
    # ctx = mock.Mock()

    # Case 1: gencfg/nanny instance
    host = 'sas1-1234'
    port = '1234'
    instance = vm_instance.HostVMInstance(host, port, 'service')
    config_action.run(instance, data_pb.config, ctx_mock)
    ctx_mock.vmagent_client.action.assert_called_with(url='http://{}:{}'.format(host, port), data=encoded)
    # Case 2: yp pod, no vmagent version
    ctx_mock.reset_mock()
    pod_id = 'pod_id'
    # pod = data_model.TPod()
    pod.spec.resource_requests.vcpu_guarantee = 2000
    pod.spec.resource_requests.vcpu_limit = 32000
    pod.spec.resource_requests.memory_guarantee = data_pb.config.mem + config_action.MEMORY_GAP
    v = pod.spec.disk_volume_requests.add()
    v.quota_policy.capacity = data_pb.config.disk.delta_size
    v.labels.attributes.add(key='mount_path', value=yson.dumps(yputil.MAIN_VOLUME_POD_MOUNT_PATH))
    container_ip = '::1'
    port = '1234'
    instance = vm_instance.PodVMInstance(pod_id, pod, container_ip, port)
    config_action.run(instance, data_pb.config, ctx_mock)
    ctx_mock.vmagent_client.action.assert_called_with(url='http://[{}]:{}'.format(container_ip, port), data=encoded)
    # Case 3: old vmagent version
    ctx_mock.reset_mock()
    pod.labels.attributes.add(key='vmagent_version', value=yson.dumps('0.1'))
    instance = vm_instance.PodVMInstance(pod_id, pod, container_ip, port)
    config_action.run(instance, data_pb.config, ctx_mock)
    ctx_mock.vmagent_client.action.assert_called_with(url='http://[{}]:{}'.format(container_ip, port), data=encoded)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_put_config_as_resource(pod, pod_set, pod_ctl_mock, ctx_mock):
    fill_pod_spec(pod, pod_set)
    data_pb = vmagent_api_pb2.VMActionRequest()
    data_pb.action = vmagent_api_pb2.VMActionRequest.PUSH_CONFIG
    data_pb.config.id = '1'
    data_pb.config.vcpu = 2
    data_pb.config.mem = 2 * 1024 ** 3
    data_pb.config.disk.delta_size = 10 * 1024 ** 3
    data_pb.config.disk.resource.rb_torrent = 'rbtorrent:1'
    # ctx = mock.Mock()
    t_id, ts = '1', 1
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, ts)

    pod_id = 'pod_id'
    container_ip = '::1'
    port = '1234'
    version = '000'
    # pod = data_model.TPod()
    pod.meta.id = pod_id
    pod.spec.iss.instances.add()
    pod.spec.resource_requests.vcpu_guarantee = 2000
    pod.spec.resource_requests.vcpu_limit = 32000
    pod.spec.resource_requests.memory_guarantee = data_pb.config.mem + config_action.MEMORY_GAP
    v = pod.spec.disk_volume_requests.add()
    v.quota_policy.capacity = data_pb.config.disk.delta_size
    v.labels.attributes.add(key='mount_path', value=yson.dumps(yputil.MAIN_VOLUME_POD_MOUNT_PATH))
    conf = '{}-0'.format(pod_id)
    i = pod.spec.iss.instances[0]
    i.id.configuration.groupStateFingerprint = conf
    new_resource = i.entity.instance.resources[config_action.VM_CONFIG_RESOURCE_NAME].resource
    url = action_helpers.make_encoded_resource(json_format.MessageToJson(data_pb.config))
    new_resource.uuid = yputil.make_resource_uuid(url)
    new_resource.urls.append(url)
    new_resource.verification.checkPeriod = '0d0h0m'
    new_resource.verification.checksum = 'EMPTY:'
    pod.labels.attributes.add(key='version', value=yson.dumps(version))
    pod.labels.attributes.add(key='vmagent_version', value=yson.dumps('0.3'))

    instance = vm_instance.PodVMInstance(pod_id, pod, container_ip, port)
    set_updates = {'/spec/iss': yputil.dumps_proto(pod.spec.iss)}
    config_action.run(instance, data_pb.config, ctx_mock)
    ctx_mock.pod_ctl.update_pod.assert_called_with(pod_id, version, set_updates, t_id, ts)
