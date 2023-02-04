import datetime

import mock
from infra.qyp.proto_lib import vmset_pb2, accounts_api_pb2
from infra.qyp.vmproxy.src.action import create as create_action
from infra.qyp.vmproxy.src.action import helpers
from infra.qyp.vmproxy.src.lib.yp import yputil
from yp_proto.yp.client.api.proto import object_service_pb2
from yt import yson

import freezegun
import pytest
import yp.data_model as data_model
from infra.qyp.vmproxy.src import errors

LOGIN = 'author'
Gb = 1024 ** 3


def list_resources():
    default_node_id = 'sas1-0000.search.yandex.net'
    # CPU
    cpu_resp = object_service_pb2.TAttributeList()
    cpu_resp.values.append(yson.dumps(default_node_id))
    res_spec = data_model.TResourceSpec()
    res_spec.cpu.total_capacity = 1000 * 100
    cpu_resp.values.append(yputil.dumps_proto(res_spec))
    res_free = data_model.TResourceStatus.TAllocationStatistics()
    res_free.cpu.capacity = 1000 * 100
    cpu_resp.values.append(yputil.dumps_proto(res_free))
    # Memory
    mem_resp = object_service_pb2.TAttributeList()
    mem_resp.values.append(yson.dumps(default_node_id))
    res_spec = data_model.TResourceSpec()
    res_spec.memory.total_capacity = 100 * Gb
    mem_resp.values.append(yputil.dumps_proto(res_spec))
    res_free = data_model.TResourceStatus.TAllocationStatistics()
    res_free.memory.capacity = 100 * Gb
    mem_resp.values.append(yputil.dumps_proto(res_free))
    # SSD
    ssd_resp = object_service_pb2.TAttributeList()
    ssd_resp.values.append(yson.dumps(default_node_id))
    res_spec = data_model.TResourceSpec()
    res_spec.disk.storage_class = 'ssd'
    res_spec.disk.total_capacity = 100 * Gb
    ssd_resp.values.append(yputil.dumps_proto(res_spec))
    res_free = data_model.TResourceStatus.TAllocationStatistics()
    res_free.disk.capacity = 100 * Gb
    ssd_resp.values.append(yputil.dumps_proto(res_free))
    # HDD
    hdd_resp = object_service_pb2.TAttributeList()
    hdd_resp.values.append(yson.dumps(default_node_id))
    res_spec = data_model.TResourceSpec()
    res_spec.disk.storage_class = 'hdd'
    res_spec.disk.total_capacity = 100 * Gb
    hdd_resp.values.append(yputil.dumps_proto(res_spec))
    res_free = data_model.TResourceStatus.TAllocationStatistics()
    res_free.disk.capacity = 100 * Gb
    hdd_resp.values.append(yputil.dumps_proto(res_free))
    # GPU gpu_tesla_k40
    gpu_resp = object_service_pb2.TAttributeList()
    gpu_resp.values.append(yson.dumps(default_node_id))
    res_spec = data_model.TResourceSpec()
    res_spec.gpu.uuid = '1'
    res_spec.gpu.model = 'gpu_tesla_k40'
    res_spec.gpu.total_memory = 10000
    gpu_resp.values.append(yputil.dumps_proto(res_spec))
    res_free = data_model.TResourceStatus.TAllocationStatistics()
    res_free.gpu.capacity = 1
    gpu_resp.values.append(yputil.dumps_proto(res_free))

    list_resource_rsp = object_service_pb2.TRspSelectObjects()
    list_resource_rsp.results.extend([cpu_resp, mem_resp, gpu_resp, gpu_resp, ssd_resp, hdd_resp])
    return list_resource_rsp.results


def assert_called_with(mock_func, *expected_args, **expected_kwargs):
    actual_args = mock_func.call_args.args
    actual_kwargs = mock_func.call_args.kwargs
    assert len(expected_args) == len(actual_args)
    assert len(expected_kwargs) == len(actual_kwargs)

    for i, actual_arg in enumerate(actual_args):
        expected_arg = expected_args[i]
        if actual_arg != expected_arg:
            if isinstance(actual_arg, data_model.TPod):
                actual_vm = vmset_pb2.VM()
                expected_vm = vmset_pb2.VM()
                for attr in actual_arg.annotations.attributes:
                    if attr.key == 'qyp_vm_spec':
                        actual_vm.ParseFromString(yson.loads(attr.value))
                for attr in expected_arg.annotations.attributes:
                    if attr.key == 'qyp_vm_spec':
                        expected_vm.ParseFromString(yson.loads(attr.value))
                assert actual_vm == expected_vm
            assert actual_arg == expected_arg


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_ok(config, vm, gen_expected_iss_payload, gen_expected_pod, gen_expected_pod_set, ctx_mock,
                   staff_client_mock):
    expected_pod = gen_expected_pod()
    expected_pod_set = gen_expected_pod_set()
    create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)
    assert_called_with(ctx_mock.pod_ctl.create_pod_with_pod_set, expected_pod, expected_pod_set)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_ok_w_ssh_keys_passed(config, vm, gen_expected_iss_payload, gen_expected_pod, gen_expected_pod_set,
                                     ctx_mock, staff_client_mock):
    config.set_value('vmproxy.pass_ssh_keys', True)
    keys1 = {
        'fingerprint_sha256': 'fingerprint_sha256',
        'fingerprint': 'fingerprint',
        'id': '1',
        'description': 'description',
        'key': '1'
    }
    keys2 = {
        'fingerprint_sha256': 'fingerprint_sha2562',
        'fingerprint': 'fingerprint',
        'id': '2',
        'description': 'description',
        'key': '2'
    }
    list_key1 = yson.YsonList()
    list_key2 = yson.YsonList()
    list_key1.append(yson.YsonMap(keys1))
    list_key2.append(yson.YsonMap(keys2))
    ret_value = [
        list_key1,
        list_key2
    ]
    ctx_mock.pod_ctl.get_keys_by_logins.return_value = ret_value

    expected_pod = gen_expected_pod()
    expected_pod_set = gen_expected_pod_set()

    create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)

    assert_called_with(ctx_mock.pod_ctl.create_pod_with_pod_set, expected_pod, expected_pod_set)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_w_check_account_use_permission_failed(config, vm, gen_expected_iss_payload, gen_expected_pod,
                                                      gen_expected_pod_set, ctx_mock):
    vm.spec.account_id = '123'
    ctx_mock.pod_ctl.check_use_account_permission.return_value = False
    with pytest.raises(errors.AuthorizationError):
        create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_w_staff_return_service(config, vm, gen_expected_iss_payload, gen_expected_pod, gen_expected_pod_set,
                                       staff_client_mock, ctx_mock):
    vm.meta.auth.owners.group_ids.append('1')
    staff_client_mock.list_groups.return_value = {
        'type': 'service',
        'service': {
            'id': '1'
        }
    }

    expected_pod = gen_expected_pod()
    expected_pod_set = gen_expected_pod_set(owners_ace_extend=['abc:service:1'])
    create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)
    assert_called_with(ctx_mock.pod_ctl.create_pod_with_pod_set, expected_pod, expected_pod_set)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_w_staff_return_departament(config, vm, gen_expected_iss_payload, gen_expected_pod, gen_expected_pod_set,
                                           staff_client_mock, ctx_mock):
    vm.meta.auth.owners.group_ids.append('1')

    staff_client_mock.list_groups.return_value = staff_client_mock.list_groups.return_value = {
        'type': 'department',
        'department': {
            'id': '1'
        }
    }
    expected_pod = gen_expected_pod()
    expected_pod_set = gen_expected_pod_set(owners_ace_extend=['staff:department:1'])

    create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)
    assert_called_with(ctx_mock.pod_ctl.create_pod_with_pod_set, expected_pod, expected_pod_set)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_go_into_owners_dict(config, vm, gen_expected_iss_payload, gen_expected_pod,
                                    gen_expected_pod_set, staff_client_mock, ctx_mock):
    min_rootfs_limit = yputil.MIN_ROOT_FS_LIMIT
    ssd_model_guarantee = vm.spec.qemu.io_guarantees_per_storage['ssd']
    vm.spec.account_id = 'abc:service:4172'
    vm.spec.qemu.node_segment = 'dev'
    personal_quotas = {
        'account_id': 'abc:service:4172',
        'segment': 'default',
        'cpu': 8000,
        'mem': 53687091200,
        'disk': {
            'storage': 'ssd',
            'capacity': 322122547200
        },
        'internet_address': 0
    }
    accounts_resp = accounts_api_pb2.ListUserAccountsResponse()
    acc_by_cluster = accounts_resp.accounts_by_cluster.add()
    account_resp = acc_by_cluster.accounts.add()
    limits = vmset_pb2.ResourceInfo()
    limits.cpu = 8000
    limits.mem = 53687091200
    limits.disk_per_storage['ssd'] = 322122547200
    account_resp.personal.limits.per_segment['dev'].CopyFrom(limits)
    account_resp.id = 'abc:service:4172'
    acc_manager_mock = mock.Mock()
    acc_manager_mock.request_account_data.return_value = accounts_resp
    ctx_mock.account_manager_client = acc_manager_mock
    config.set_value('personal_quotas', [personal_quotas])
    config.set_value('vmproxy.pass_ssh_keys', False)
    expected_pod = gen_expected_pod()
    expected_pod_set = gen_expected_pod_set()
    expected_pod.spec.disk_volume_requests[0].quota_policy.bandwidth_guarantee = min_rootfs_limit
    expected_pod.spec.disk_volume_requests[0].quota_policy.bandwidth_limit = min_rootfs_limit
    expected_pod.spec.disk_volume_requests[1].quota_policy.bandwidth_guarantee = ssd_model_guarantee - min_rootfs_limit
    expected_pod.spec.disk_volume_requests[1].quota_policy.bandwidth_limit = yputil.IO_BANDWIDTH_LIMIT_SSD - min_rootfs_limit
    ctx_mock.pod_ctl.check_use_account_permission.return_value = True
    scopes = {
        'slug': '234'
    }
    with mock.patch('infra.qyp.vmproxy.src.action.allocate.get_scopes_dict', return_value=scopes):
        create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)
    assert_called_with(ctx_mock.pod_ctl.create_pod_with_pod_set, expected_pod, expected_pod_set)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_w_abc_return_scopes_none(config, vm, gen_expected_iss_payload, gen_expected_pod,
                                         gen_expected_pod_set, ctx_mock, abc_client_mock):
    abc_client_mock._client.get.return_value = {'results': []}
    expected_pod = gen_expected_pod()
    expected_pod_set = gen_expected_pod_set()
    create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)
    assert_called_with(ctx_mock.pod_ctl.create_pod_with_pod_set, expected_pod, expected_pod_set)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_w_staff_return_service_role(config, vm, gen_expected_iss_payload, gen_expected_pod,
                                            gen_expected_pod_set, staff_client_mock, ctx_mock):
    vm.meta.auth.owners.group_ids.append('1')
    staff_client_mock.list_groups.return_value = {
        'type': 'servicerole',
        'role_scope': 'slug',
        'parent': {
            'service': {
                'id': '1'
            }
        }
    }
    expected_pod = gen_expected_pod()
    expected_pod_set = gen_expected_pod_set(owners_ace_extend=['abc:service-scope:1:234'])
    scopes = {
        'slug': '234'
    }
    with mock.patch('infra.qyp.vmproxy.src.action.allocate.get_scopes_dict', return_value=scopes):
        create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)

    assert_called_with(ctx_mock.pod_ctl.create_pod_with_pod_set, expected_pod, expected_pod_set)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_w_staff_return_trash(config, vm, ctx_mock, staff_client_mock):
    vm.meta.auth.owners.group_ids.append('1')
    staff_client_mock.list_groups.return_value = {
        'type': 'another-servicerole',
    }
    with pytest.raises(ValueError):
        create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_force_node_ok(config, vm, gen_expected_pod, gen_expected_pod_set, ctx_mock):
    forced_node_id = 'sas1-0000.search.yandex.net'
    vm.spec.account_id = 'any_available_account'
    vm.spec.qemu.forced_node_id = forced_node_id
    ctx_mock.pod_ctl.forced_node_free.return_value = True
    ctx_mock.pod_ctl.list_resources_by_nodes.return_value = list_resources()[:-1]
    expected_rr = data_model.TPodSpec.TResourceRequests()
    expected_rr.vcpu_guarantee = 1000 * 100
    expected_rr.vcpu_limit = 1000 * 100
    expected_rr.memory_guarantee = 1024 ** 3 * 100
    expected_rr.memory_limit = 1024 ** 3 * 100
    expected_rr.network_bandwidth_guarantee = 1024 ** 3 * 2
    # commented due to QEMUKVM-1679
    # expected_rr.network_bandwidth_limit = 1024 ** 3 * 2
    # TODO: what this
    expected_rr.anonymous_memory_limit = 3
    expected_rr.dirty_memory_limit = 1

    create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)

    result_pod = ctx_mock.pod_ctl.create_pod_with_pod_set.call_args[0][0]
    assert result_pod.spec.resource_requests == expected_rr
    assert yputil.cast_attr_dict_to_dict(result_pod.labels)['qyp_vm_forced_node_id'] == forced_node_id

    root_quota = config.get_value('vmproxy.default_porto_layer.root_quota')
    workdir_quota = config.get_value('vmproxy.default_porto_layer.workdir_quota')

    assert vm.spec.qemu.volumes[0].capacity == 69 * Gb - root_quota - workdir_quota
    assert len(result_pod.spec.scheduling.hints) == 1
    assert result_pod.spec.scheduling.hints[0].node_id == forced_node_id
    assert result_pod.spec.scheduling.hints[0].strong is True


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_force_node_w_node_has_allocations(config, vm, gen_expected_pod, gen_expected_pod_set, ctx_mock):
    vm.spec.qemu.forced_node_id = 'sas1-0000.search.yandex.net'
    vm.spec.account_id = 'any_available_account'
    ctx_mock.pod_ctl.forced_node_free.return_value = False
    with pytest.raises(ValueError):
        create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_max_vcpu_limit(config, vm, gen_expected_pod, gen_expected_pod_set, ctx_mock):
    vm.spec.account_id = "any_available_account"
    vm.spec.qemu.resource_requests.vcpu_limit = 256000
    vm.spec.qemu.resource_requests.vcpu_guarantee = 256000
    with pytest.raises(ValueError) as e:
        create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)
    assert 'Cpu limit 256000 exceeds maximum allowed value' in e.value.message


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_force_node_w_node_has_ssd_and_hdd_but_vm_not(config, vm, gen_expected_pod, gen_expected_pod_set,
                                                             ctx_mock):
    forced_node_id = 'sas1-0000.search.yandex.net'
    vm.spec.account_id = 'any_available_account'
    root_quota = config.get_value('vmproxy.default_porto_layer.root_quota')
    workdir_quota = config.get_value('vmproxy.default_porto_layer.workdir_quota')

    vm.spec.qemu.volumes[0].storage_class = 'ssd'
    vm.spec.qemu.volumes[0].capacity = 69 * Gb - root_quota - workdir_quota

    vm.spec.qemu.forced_node_id = forced_node_id
    ctx_mock.pod_ctl.forced_node_free.return_value = True
    ctx_mock.pod_ctl.list_resources_by_nodes.return_value = list_resources()

    with pytest.raises(ValueError) as error:
        create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)

    assert 'Qemu Volumes has wrong balance in node:' in error.value.message


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_force_node_w_node_and_vm_has_ssd_and_hdd(config, vm, gen_expected_pod, gen_expected_pod_set, ctx_mock):
    forced_node_id = 'sas1-0000.search.yandex.net'
    vm.spec.account_id = 'any_available_account'
    root_quota = config.get_value('vmproxy.default_porto_layer.root_quota')
    workdir_quota = config.get_value('vmproxy.default_porto_layer.workdir_quota')

    vm.spec.qemu.volumes[0].storage_class = 'ssd'
    vm.spec.qemu.volumes[0].capacity = 69 * Gb - root_quota - workdir_quota

    extra_volume = vm.spec.qemu.volumes.add()
    extra_volume.name = 'test'
    extra_volume.capacity = 69 * Gb
    extra_volume.storage_class = 'hdd'
    extra_volume.image_type = vmset_pb2.Volume.RAW

    vm.spec.qemu.forced_node_id = forced_node_id

    ctx_mock.pod_ctl.forced_node_free.return_value = True
    ctx_mock.pod_ctl.list_resources_by_nodes.return_value = list_resources()

    create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_without_gpu_in_gpu_segment(config, vm, ctx_mock, patch_uuid):
    vm.spec.qemu.node_segment = 'gpu-dev'
    with pytest.raises(ValueError):
        create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_with_gpu_request(config, vm, ctx_mock, gen_expected_pod, gen_expected_pod_set, patch_uuid):
    gpu_model = 'gpu_tesla_k40'
    vm.spec.qemu.node_segment = 'gpu-dev'
    vm.spec.qemu.gpu_request.capacity = 2
    vm.spec.qemu.gpu_request.model = gpu_model

    expected_pod = gen_expected_pod()
    for _ in range(vm.spec.qemu.gpu_request.capacity):
        gpu_req = expected_pod.spec.gpu_requests.add()
        gpu_req.id = 'version'
        gpu_req.model = gpu_model
    expected_pod.spec.iss.instances[0].properties['QYP_GPU'] = '{}-2-0-0'.format(gpu_model)
    expected_pod.spec.iss.instances[0].dynamicProperties.update({
        'QEMU_SYSTEM_CMD_BIN_PATH': yputil.QEMU_BIN_RESOURCE_PATH,
        'QEMU_IMG_CMD_BIN_PATH': yputil.QEMU_IMG_RESOURCE_PATH,
    })
    expected_pod_set = gen_expected_pod_set()

    create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)
    assert_called_with(ctx_mock.pod_ctl.create_pod_with_pod_set, expected_pod, expected_pod_set)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_windows_vm(config, vm, ctx_mock, gen_expected_pod, gen_expected_pod_set, patch_uuid):
    vm.spec.qemu.vm_type = vmset_pb2.VMType.WINDOWS
    expected_pod = gen_expected_pod()
    # QEMUKVM-902
    expected_pod.spec.ip6_address_requests[1].enable_dns = False
    expected_pod_set = gen_expected_pod_set()
    create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)
    assert_called_with(ctx_mock.pod_ctl.create_pod_with_pod_set, expected_pod, expected_pod_set)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_with_io_guarantees(config, vm, ctx_mock, gen_expected_pod, gen_expected_pod_set, patch_uuid):
    min_rootfs_limit = yputil.MIN_ROOT_FS_LIMIT
    # Case 1: default, SSD & HDD
    ssd_model_guarantee = 90 * 1024 ** 2
    hdd_model_guarantee = 15 * 1024 ** 2
    vm.spec.qemu.io_guarantees_per_storage['ssd'] = ssd_model_guarantee
    vm.spec.qemu.io_guarantees_per_storage['hdd'] = hdd_model_guarantee
    v = vm.spec.qemu.volumes.add()
    v.name = 'hdd_volume'
    v.storage_class = 'hdd'
    v.image_type = vmset_pb2.Volume.RAW
    v.capacity = 2147483648
    v.vm_mount_path = '/extra_hdd_volume'
    v.pod_mount_path = '/qemu-hdd_volume'
    expected_pod = gen_expected_pod()
    expected_pod.spec.disk_volume_requests[0].quota_policy.bandwidth_guarantee = min_rootfs_limit
    expected_pod.spec.disk_volume_requests[0].quota_policy.bandwidth_limit = min_rootfs_limit
    expected_pod.spec.disk_volume_requests[1].quota_policy.bandwidth_guarantee = ssd_model_guarantee - min_rootfs_limit
    expected_pod.spec.disk_volume_requests[1].quota_policy.bandwidth_limit = ssd_model_guarantee - min_rootfs_limit
    expected_pod.spec.disk_volume_requests[2].quota_policy.bandwidth_guarantee = hdd_model_guarantee
    expected_pod.spec.disk_volume_requests[2].quota_policy.bandwidth_limit = hdd_model_guarantee * 2
    expected_pod_set = gen_expected_pod_set()
    create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)
    assert_called_with(ctx_mock.pod_ctl.create_pod_with_pod_set, expected_pod, expected_pod_set)
    # Case 2: dev, SSD & HDD, small guarantees
    vm.spec.qemu.node_segment = 'dev'
    expected_pod = gen_expected_pod()
    expected_pod_set = gen_expected_pod_set()
    expected_pod.spec.disk_volume_requests[0].quota_policy.bandwidth_guarantee = min_rootfs_limit
    expected_pod.spec.disk_volume_requests[0].quota_policy.bandwidth_limit = min_rootfs_limit
    expected_pod.spec.disk_volume_requests[1].quota_policy.bandwidth_guarantee = ssd_model_guarantee - min_rootfs_limit
    expected_pod.spec.disk_volume_requests[1].quota_policy.bandwidth_limit = yputil.IO_BANDWIDTH_LIMIT_SSD - min_rootfs_limit
    expected_pod.spec.disk_volume_requests[2].quota_policy.bandwidth_guarantee = hdd_model_guarantee
    expected_pod.spec.disk_volume_requests[2].quota_policy.bandwidth_limit = yputil.IO_BANDWIDTH_LIMIT_HDD
    create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)
    assert_called_with(ctx_mock.pod_ctl.create_pod_with_pod_set, expected_pod, expected_pod_set)
    # Case 3: dev, SSD & HDD, large guarantees
    vm.spec.qemu.node_segment = 'dev'
    ssd_model_guarantee = 500 * 1024 ** 2
    hdd_model_guarantee = 100 * 1024 ** 2
    vm.spec.qemu.io_guarantees_per_storage['ssd'] = ssd_model_guarantee
    vm.spec.qemu.io_guarantees_per_storage['hdd'] = hdd_model_guarantee
    expected_pod = gen_expected_pod()
    expected_pod_set = gen_expected_pod_set()
    expected_pod.spec.disk_volume_requests[0].quota_policy.bandwidth_guarantee = min_rootfs_limit
    expected_pod.spec.disk_volume_requests[0].quota_policy.bandwidth_limit = min_rootfs_limit
    expected_pod.spec.disk_volume_requests[1].quota_policy.bandwidth_guarantee = ssd_model_guarantee - min_rootfs_limit
    expected_pod.spec.disk_volume_requests[1].quota_policy.bandwidth_limit = ssd_model_guarantee - min_rootfs_limit
    expected_pod.spec.disk_volume_requests[2].quota_policy.bandwidth_guarantee = hdd_model_guarantee
    expected_pod.spec.disk_volume_requests[2].quota_policy.bandwidth_limit = hdd_model_guarantee * 2
    create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)
    assert_called_with(ctx_mock.pod_ctl.create_pod_with_pod_set, expected_pod, expected_pod_set)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_with_admin_role(config, vm,
                                gen_expected_iss_payload,
                                gen_expected_pod,
                                gen_expected_pod_set,
                                staff_client_mock,
                                ctx_mock):

    service_id = '1'
    admin_group_id = '123'
    account_id = 'abc:service:{}'.format(service_id)
    scope_id = '234'
    scope_slug = 'slug'
    scopes_dict = {
        scope_slug: scope_id
    }

    helpers.get_staff_admin_group_ids.cache_clear()
    vm.spec.account_id = account_id
    staff_client_mock.list_groups.side_effect = [
        {
            'result': [
                {
                    'id': admin_group_id,
                    'role_scope': 'administration',
                    'parent': {
                        'id': service_id
                    },
                    'affiliation_counters': {
                        'yandex': 1,
                        'external': 0,
                    },
                }
            ]
        },
        {
            'type': 'servicerole',
            'role_scope': scope_slug,
            'parent': {
                'service': {
                    'id': service_id
                }
            }
        }
    ]
    old_vm = vmset_pb2.VM()
    old_vm.CopyFrom(vm)
    # Add vm with admin group id to pod
    vm.meta.auth.owners.group_ids.append(admin_group_id)
    expected_pod = gen_expected_pod()
    # Restore old vm
    vm.CopyFrom(old_vm)

    expected_pod_set = gen_expected_pod_set(
        owners_ace_extend=['abc:service-scope:{}:{}'.format(service_id, scope_id)]
    )
    with mock.patch('infra.qyp.vmproxy.src.action.allocate.get_scopes_dict', return_value=scopes_dict):
        create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)
        assert_called_with(ctx_mock.pod_ctl.create_pod_with_pod_set, expected_pod, expected_pod_set)


def test_create_with_scheduling_hints(config, vm, ctx_mock):
    config.set_value('vmproxy.root_users', [LOGIN])
    node_id = 'sas0-0000.search.yandex.net'
    h = vm.spec.scheduling.hints.add()
    h.node_id = node_id
    h.strong = True
    create_action.run(vm.meta, vm.spec, ctx_mock, LOGIN)
    result_pod = ctx_mock.pod_ctl.create_pod_with_pod_set.call_args[0][0]
    assert len(result_pod.spec.scheduling.hints) == 1
    assert result_pod.spec.scheduling.hints[0].node_id == node_id
    assert result_pod.spec.scheduling.hints[0].strong is True
