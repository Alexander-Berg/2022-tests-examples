import base64

import datetime

import freezegun
import mock
import pytest
import json
from yt import yson
import yp.data_model as data_model
from yp_proto.yp.client.api.proto import cluster_api_pb2, object_service_pb2

from infra.qyp.proto_lib import vmset_pb2, vmagent_pb2, vmagent_api_pb2, qdm_pb2, accounts_api_pb2
from infra.qyp.vmproxy.src import errors, security_policy
from infra.qyp.vmproxy.src.action import allocate as allocate_action, helpers
from infra.qyp.vmproxy.src.action import update as update_action
from infra.qyp.vmproxy.src.action import config as config_action
from infra.qyp.vmproxy.src.lib.yp import yputil

LOGIN = 'author'


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
    res_spec.memory.total_capacity = 1024 ** 3 * 100
    mem_resp.values.append(yputil.dumps_proto(res_spec))
    res_free = data_model.TResourceStatus.TAllocationStatistics()
    res_free.memory.capacity = 1024 ** 3 * 100
    mem_resp.values.append(yputil.dumps_proto(res_free))
    # SSD
    ssd_resp = object_service_pb2.TAttributeList()
    ssd_resp.values.append(yson.dumps(default_node_id))
    res_spec = data_model.TResourceSpec()
    res_spec.disk.storage_class = 'ssd'
    res_spec.disk.total_capacity = 1024 ** 3 * 100
    ssd_resp.values.append(yputil.dumps_proto(res_spec))
    res_free = data_model.TResourceStatus.TAllocationStatistics()
    res_free.disk.capacity = 1024 ** 3 * 100
    ssd_resp.values.append(yputil.dumps_proto(res_free))

    list_resource_rsp = object_service_pb2.TRspSelectObjects()
    list_resource_rsp.results.extend([cpu_resp, mem_resp, ssd_resp])
    return list_resource_rsp.results


@pytest.fixture(autouse=True)
def update_vm(vm, patch_uuid):
    for v in vm.spec.qemu.volumes:
        v.req_id = vm.meta.id + '-' + patch_uuid()


def assert_called_with(mock_func, *expected_args, **expected_kwargs):
    actual_args = mock_func.call_args.args
    actual_kwargs = mock_func.call_args.kwargs
    assert len(expected_args) == len(actual_args)
    assert len(expected_kwargs) == len(actual_kwargs)

    def match_args(actual_arg, expected_arg):
        if actual_arg != expected_arg:
            if isinstance(actual_arg, dict):
                if '/annotations/qyp_vm_spec' in actual_arg:
                    actual_vm = vmset_pb2.VM()
                    expected_vm = vmset_pb2.VM()
                    actual_vm.ParseFromString(yson.loads(actual_arg.pop('/annotations/qyp_vm_spec')))
                    expected_vm.ParseFromString(yson.loads(expected_arg.pop('/annotations/qyp_vm_spec')))
                    assert actual_vm == expected_vm
                if '/spec/iss' in actual_arg:
                    actual_iss = yputil.loads_proto(actual_arg.pop('/spec/iss'), cluster_api_pb2.HostConfiguration)
                    expected_iss = yputil.loads_proto(expected_arg.pop('/spec/iss'), cluster_api_pb2.HostConfiguration)
                    assert actual_iss == expected_iss

                if '/spec/resource_requests' in actual_arg:
                    actual_payload = yputil.loads_proto(actual_arg.pop('/spec/resource_requests'),
                                                        data_model.TPodSpec.TResourceRequests)
                    expected_payload = yputil.loads_proto(expected_arg.pop('/spec/resource_requests'),
                                                          data_model.TPodSpec.TResourceRequests)
                    assert actual_payload == expected_payload

                if '/spec/disk_volume_requests' in actual_arg:
                    actual_payload = yson.loads(actual_arg.pop('/spec/disk_volume_requests'))

                    expected_payload = yson.loads(expected_arg.pop('/spec/disk_volume_requests'))
                    v1 = json.dumps(actual_payload, indent=4, sort_keys=True)
                    v2 = json.dumps(expected_payload, indent=4, sort_keys=True)
                    assert v1 == v2
                v1 = json.dumps(actual_arg, indent=4, sort_keys=True)
                v2 = json.dumps(expected_arg, indent=4, sort_keys=True)
                assert v1 == v2
            else:
                assert actual_arg == expected_arg

    for i, actual_arg_value in enumerate(actual_args):
        expected_arg_value = expected_args[i]
        match_args(actual_arg_value, expected_arg_value)

    for arg_key, actual_arg_value in actual_kwargs.items():
        expected_arg_value = expected_kwargs[arg_key]
        match_args(actual_arg_value, expected_arg_value)


def test_vmagent_resp(ctx_mock):
    vm_config_ = vmagent_pb2.VMConfig()
    vm_config_.vcpu = 2
    vm_config_.mem = 1024 ** 3
    vm_config_.disk.resource.rb_torrent = 'rbtorrent:fake'
    vmagent_resp = vmagent_api_pb2.VMStatusResponse(config=vm_config_)
    ctx_mock.vmagent_client.status.return_value = base64.b64encode(vmagent_resp.SerializeToString())


def gen_pod_updates(vm, kwargs=None):
    """

    :type vm: vmset_pb2.VM
    :return:
        """
    defaults = {
        '/labels/deploy_engine': yson.dumps('QYP'),
        '/labels/version': yson.dumps('version'),
        "/labels/qyp_vm_type": yson.dumps(vm.spec.qemu.vm_type),
        "/labels/qyp_vm_autorun": yson.dumps(vm.spec.qemu.autorun),
        '/labels/vmagent_version': yson.dumps(vm.spec.vmagent_version),
        '/labels/qyp_vm_mark': yson.dumps({}),
    }
    if kwargs:
        defaults.update(kwargs)
    return defaults


def gen_pod_set_updates(vm, kwargs=None):
    """

    :type vm: vmset_pb2.VM
    :return:
    """
    defaults = {
        '/labels/deploy_engine': yson.dumps('QYP'),
        '/labels/version': yson.dumps('version'),
        '/labels/vmagent_version': yson.dumps(vm.spec.vmagent_version),
        '/labels/qyp_vm_mark': yson.dumps({}),
        "/labels/qyp_vm_type": yson.dumps(vm.spec.qemu.vm_type),
        "/labels/qyp_vm_autorun": yson.dumps(vm.spec.qemu.autorun),
    }
    if kwargs:
        defaults.update(kwargs)
    return defaults


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_update_owners(vm, config, ctx_mock, staff_client_mock, sec_policy_mock, abc_client_mock,
                       pod, pod_set, gen_expected_pod, gen_expected_pod_set):
    staff_client_mock.list_groupmembership.return_value = {'result': []}
    staff_client_mock.list_groups.return_value = {'type': 'department', 'department': {'id': 'test'}}
    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)

    current_pod = gen_expected_pod()
    current_pod_set = gen_expected_pod_set()

    pod.CopyFrom(current_pod)
    pod_set.CopyFrom(current_pod_set)

    abc_client_mock._client.get.return_value = {'results': []}

    vm.meta.auth.owners.logins.extend(['test3', 'test4'])
    vm.meta.auth.owners.group_ids.extend(['3', '4'])

    pod_updates = gen_pod_updates(vm, {
        '/annotations/owners/logins': yson.dumps(['author', 'test3', 'test4']),
        '/annotations/owners/groups': yson.dumps([3, 4]),
        '/annotations/qyp_vm_spec': helpers.yson_dumps_vm(vmset_pb2.VM(spec=vm.spec, meta=vm.meta)),
    })
    acl = allocate_action.make_pod_acl(vm.meta)
    pod_set_updates = gen_pod_set_updates(vm, {
        '/meta/acl': helpers.yson_dumps_list_of_proto(acl),
    })

    update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)
    assert_called_with(ctx_mock.pod_ctl.update_pod, vm.meta.id, vm.meta.version.pod, pod_updates, t_id, 0)
    assert_called_with(ctx_mock.pod_ctl.update_pod_set, vm.meta.id, vm.meta.version.pod_set, pod_set_updates, t_id, 0)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_update_abc_owning_fail(vm, ctx_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set, pod_ctl_mock):
    def check_use_account(acc_id, login, use_cache):
        return acc_id in ('abc:service:1', 'abc:service:2', 'abc:service:4172')

    def check_use_macro(obj_id, login):
        return obj_id in (vm.spec.qemu.network_id, '_GOODNETS_')

    pod_ctl_mock.check_use_account_permission.side_effect = check_use_account
    pod_ctl_mock.check_use_macro_permission.side_effect = check_use_macro
    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())

    vm.spec.account_id = 'abc:service:666'
    with pytest.raises(errors.AuthorizationError) as e:
        update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)
    assert e.value.message == 'User {} has no access to account {}'.format(LOGIN, vm.spec.account_id)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_update_abc_owning_ok(vm, config, ctx_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set):
    def check_use_account(acc_id, login, use_cache):
        return acc_id in ('abc:service:1', 'abc:service:2', 'abc:service:4172')

    ctx_mock.pod_ctl.check_use_account_permission.side_effect = check_use_account
    ctx_mock.personal_quotas_dict = {'abc:service:4172': {}}
    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)

    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())

    vm.spec.account_id = 'abc:service:2'

    update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)

    pod_updates = gen_pod_updates(vm, {
        '/annotations/qyp_vm_spec': helpers.yson_dumps_vm(vmset_pb2.VM(spec=vm.spec, meta=vm.meta)),
    })
    pod_set_updates = gen_pod_set_updates(vm, {
        '/spec/account_id': yson.dumps('abc:service:2'),
    })
    assert_called_with(ctx_mock.pod_ctl.update_pod, vm.meta.id, vm.meta.version.pod, pod_updates, t_id, 0)
    assert_called_with(ctx_mock.pod_ctl.update_pod_set, vm.meta.id, vm.meta.version.pod_set, pod_set_updates, t_id, 0)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_update_abc_admin_owner(vm, config, ctx_mock,
                                staff_client_mock,
                                pod, pod_set,
                                gen_expected_pod, gen_expected_pod_set):

    service_id = '1'
    admin_group_id = '123'
    account_id = 'abc:service:{}'.format(service_id)
    scope_id = '234'
    scope_slug = 'slug'
    scopes_dict = {
        scope_slug: scope_id
    }
    ts = 10
    transaction_id = 'transaction'

    helpers.get_staff_admin_group_ids.cache_clear()
    ctx_mock.pod_ctl.check_use_account_permission.return_value = True
    ctx_mock.personal_quotas_dict = {}
    ctx_mock.pod_ctl.start_transaction.return_value = (transaction_id, ts)

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
    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())

    vm.spec.account_id = account_id

    new_vm = vmset_pb2.VM()
    new_vm.CopyFrom(vm)
    new_vm.meta.auth.owners.group_ids.append(admin_group_id)
    group_ids = map(int, new_vm.meta.auth.owners.group_ids)

    pod_updates = gen_pod_updates(vm, {
        '/annotations/owners/logins': yson.dumps(vm.meta.auth.owners.logins),
        '/annotations/owners/groups': yson.dumps(group_ids),
        '/annotations/qyp_vm_spec': helpers.yson_dumps_vm(vmset_pb2.VM(spec=new_vm.spec, meta=new_vm.meta)),
    })

    pod_set_with_acl = gen_expected_pod_set(
        owners_ace_extend=['abc:service-scope:{}:{}'.format(service_id, scope_id)]
    )
    pod_set_updates = gen_pod_set_updates(vm, {
        '/spec/account_id': yson.dumps(account_id),
        '/meta/acl': helpers.yson_dumps_list_of_proto(pod_set_with_acl.meta.acl),
    })

    with mock.patch('infra.qyp.vmproxy.src.action.allocate.get_scopes_dict', return_value=scopes_dict):
        update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)

        assert_called_with(ctx_mock.pod_ctl.update_pod,
                           vm.meta.id,
                           vm.meta.version.pod,
                           pod_updates,
                           transaction_id, ts)
        assert_called_with(ctx_mock.pod_ctl.update_pod_set,
                           vm.meta.id,
                           vm.meta.version.pod_set,
                           pod_set_updates,
                           transaction_id, ts)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_update_abc_owning_personal_ok(vm, config, ctx_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set):
    def check_use_account(acc_id, login, use_cache):
        return acc_id in ('abc:service:1', 'abc:service:2', 'abc:service:4172')

    ctx_mock.pod_ctl.check_use_account_permission.side_effect = check_use_account
    ctx_mock.personal_quotas_dict = {'abc:service:4172': {}}
    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)

    accounts_resp = accounts_api_pb2.ListUserAccountsResponse()
    acc_by_cluster = accounts_resp.accounts_by_cluster.add()
    account_resp = acc_by_cluster.accounts.add()
    limits = vmset_pb2.ResourceInfo()
    limits.cpu = 8000
    limits.mem = 3221225472
    limits.disk_per_storage['ssd'] = 20737418240
    account_resp.id = 'abc:service:4172'
    account_resp.personal.limits.per_segment['default'].CopyFrom(limits)
    acc_manager_mock = mock.Mock()
    acc_manager_mock.request_account_data.return_value = accounts_resp
    ctx_mock.account_manager_client = acc_manager_mock

    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())

    vm.spec.account_id = 'abc:service:4172'
    update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)

    pod_updates = gen_pod_updates(vm, {
        '/annotations/owners/author': yson.dumps('author'),
        "/annotations/qyp_vm_spec": helpers.yson_dumps_vm(vmset_pb2.VM(spec=vm.spec, meta=vm.meta)),
    })

    pod_set_updates = gen_pod_set_updates(vm, {
        '/spec/account_id': yson.dumps('abc:service:4172'),
    })

    assert_called_with(ctx_mock.pod_ctl.update_pod, vm.meta.id, vm.meta.version.pod, pod_updates, t_id, 0)
    assert_called_with(ctx_mock.pod_ctl.update_pod_set, vm.meta.id, vm.meta.version.pod_set, pod_set_updates, t_id, 0)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_update_network_id_fail(vm, config, pod_ctl_mock, ctx_mock, staff_client_mock, sec_policy_mock,
                                abc_client_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set):
    copy_of_current_network_id = vm.spec.qemu.network_id

    def check_use_macro(obj_id, login):
        return obj_id in (copy_of_current_network_id, '_GOODNETS_')

    ctx_mock.pod_ctl.check_use_macro_permission.side_effect = check_use_macro

    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)

    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())

    vm.spec.qemu.network_id = '_OTHERNETS_'
    with pytest.raises(errors.AuthorizationError):
        update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_update_network_id_ok(vm, config, pod_ctl_mock, ctx_mock, staff_client_mock, sec_policy_mock,
                              abc_client_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set):
    copy_of_current_network_id = vm.spec.qemu.network_id

    def check_use_macro(obj_id, login):
        return obj_id in (copy_of_current_network_id, '_GOODNETS_')

    ctx_mock.pod_ctl.check_use_macro_permission.side_effect = check_use_macro

    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)

    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())

    # check macro permission is not checked if network is not updated
    vm.spec.qemu.network_id = copy_of_current_network_id
    update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)
    assert not ctx_mock.pod_ctl.check_use_macro_permission.called

    vm.spec.qemu.network_id = '_GOODNETS_'
    update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)

    pod_updates = gen_pod_updates(vm, {
        '/spec/ip6_address_requests/0/network_id': yson.dumps('_GOODNETS_'),
        '/spec/ip6_address_requests/1/network_id': yson.dumps('_GOODNETS_'),
    })
    pod_set_updates = {}

    assert_called_with(ctx_mock.pod_ctl.update_pod, vm.meta.id, vm.meta.version.pod, pod_updates, t_id, 0)
    assert_called_with(ctx_mock.pod_ctl.update_pod_set, vm.meta.id, vm.meta.version.pod_set, pod_set_updates, t_id, 0)
    assert ctx_mock.pod_ctl.check_use_macro_permission.called


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
@pytest.mark.parametrize('resource_names', [['vcpu_limit', 'vcpu_guarantee'], ['memory_limit', 'memory_guarantee']])
def test_update_resource_requests_ok(vm, config, pod_ctl_mock, ctx_mock, staff_client_mock, sec_policy_mock,
                                     abc_client_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set,
                                     gen_expected_iss_payload, resource_names):
    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())

    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)
    for resource_name in resource_names:
        setattr(vm.spec.qemu.resource_requests, resource_name,
                getattr(vm.spec.qemu.resource_requests, resource_name) + 1000)
    with mock.patch('uuid.uuid4', return_value='new_version'):
        update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)
        new_pod = gen_expected_pod()
        new_iss_proto = gen_expected_iss_payload(new_pod)

    pod_res = data_model.TPodSpec.TResourceRequests()
    pod_res.dirty_memory_limit = vm.spec.qemu.resource_requests.dirty_memory_limit
    pod_res.memory_limit = vm.spec.qemu.resource_requests.memory_limit
    pod_res.anonymous_memory_limit = vm.spec.qemu.resource_requests.anonymous_memory_limit
    pod_res.vcpu_guarantee = vm.spec.qemu.resource_requests.vcpu_guarantee
    pod_res.vcpu_limit = vm.spec.qemu.resource_requests.vcpu_limit
    pod_res.memory_guarantee = vm.spec.qemu.resource_requests.memory_guarantee
    pod_res.network_bandwidth_guarantee = vm.spec.qemu.resource_requests.network_bandwidth_guarantee
    # commented due to QEMUKVM-1679
    # pod_res.network_bandwidth_limit = vm.spec.qemu.resource_requests.network_bandwidth_guarantee
    pod_updates = gen_pod_updates(vm, {
        '/spec/resource_requests': yputil.dumps_proto(pod_res),
        '/spec/iss': yputil.dumps_proto(new_iss_proto),
        '/labels/version': yson.dumps('new_version'),
        "/annotations/qyp_vm_spec": helpers.yson_dumps_vm(vmset_pb2.VM(spec=vm.spec, meta=vm.meta)),
    })
    pod_set_updates = {}

    assert_called_with(ctx_mock.pod_ctl.update_pod, vm.meta.id, vm.meta.version.pod, pod_updates, t_id, 0)
    assert_called_with(ctx_mock.pod_ctl.update_pod_set, vm.meta.id, vm.meta.version.pod_set, pod_set_updates, t_id, 0)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_update_use_nat64(vm, config, pod_ctl_mock, ctx_mock, staff_client_mock, sec_policy_mock,
                          abc_client_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set,
                          gen_expected_iss_payload):
    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())

    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)
    vm.spec.qemu.use_nat64 = True
    new_pod = gen_expected_pod()
    new_iss_proto = gen_expected_iss_payload(new_pod)

    update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)

    pod_updates = gen_pod_updates(vm, {
        '/spec/iss': yputil.dumps_proto(new_iss_proto),
    })
    pod_set_updates = {}

    assert_called_with(ctx_mock.pod_ctl.update_pod, vm.meta.id, vm.meta.version.pod, pod_updates, t_id, 0)
    assert_called_with(ctx_mock.pod_ctl.update_pod_set, vm.meta.id, vm.meta.version.pod_set, pod_set_updates, t_id, 0)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_update_forced_node_id_failed(vm, config, pod_ctl_mock, ctx_mock, staff_client_mock, sec_policy_mock,
                                      abc_client_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set,
                                      gen_expected_iss_payload):
    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())

    forced_node_id = 'sas1-0000.search.yandex.net'
    vm.spec.qemu.forced_node_id = forced_node_id
    pod_ctl_mock.forced_node_free.return_value = False
    with pytest.raises(ValueError) as error:
        update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)

    assert error.value.message == 'Node has allocations'


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_update_forced_node_id_with_one_volume_ok(vm, config, pod_ctl_mock, ctx_mock, staff_client_mock,
                                                  sec_policy_mock,
                                                  abc_client_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set,
                                                  gen_expected_iss_payload):
    # Case 12: update forced_node_id successful
    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())

    forced_node_id = 'sas1-0000.search.yandex.net'
    vm.spec.account_id = 'not_tmp_account'
    vm.spec.qemu.forced_node_id = forced_node_id
    pod_ctl_mock.forced_node_free.return_value = True

    ctx_mock.personal_quotas_dict = {'abc:service:4172': {}}

    pod_ctl_mock.list_resources_by_nodes.return_value = list_resources()
    update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)

    pod_pb = gen_expected_pod()
    hint = pod_pb.spec.scheduling.hints.add()
    hint.node_id = forced_node_id
    hint.strong = True
    pod_set_updates = gen_pod_set_updates(vm, {
        '/spec/account_id': yson.dumps('not_tmp_account'),
        '/labels/qyp_vm_forced_node_id': yson.dumps(forced_node_id),
    })

    assert pod_ctl_mock.update_pod_with_move.call_args[1]['pod'].labels == pod_pb.labels
    assert_called_with(pod_ctl_mock.update_pod_with_move,
                       pod_id=vm.meta.id,
                       pod_version=vm.meta.version.pod,
                       pod_set_version=vm.meta.version.pod_set,
                       pod=pod_pb,
                       pod_set_updates=pod_set_updates)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
@pytest.mark.parametrize('default_vmagent_version', ['0.26'])
def test_update_vmagent(vm, config, pod_ctl_mock, ctx_mock, staff_client_mock,
                        abc_client_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set,
                        gen_expected_iss_payload, default_vmagent_version):
    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())

    # test copy exists iss hooks time limits
    pod.spec.iss.instances[0].entity.instance.timeLimits['iss_hook_stop'].maxExecutionTimeMs = 100000000

    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)
    new_vmagent_version = '0.28'

    config.set_value('vmproxy.default_vmagent.version', new_vmagent_version)
    config.set_value('vmproxy.default_pod_resources.vmagent.url', 'rbtorrent:new_url')

    update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, update_vmagent=True)

    expected_pod = gen_expected_pod()
    expected_iss_proto = expected_pod.spec.iss

    # test copy exists iss hooks time limits
    expected_iss_proto.instances[0].entity.instance.timeLimits['iss_hook_stop'].maxExecutionTimeMs = 100000000

    vm.spec.vmagent_version = new_vmagent_version

    root_fs_disc_volume_req = data_model.TPodSpec.TDiskVolumeRequest()
    root_fs_disc_volume_req.CopyFrom(pod.spec.disk_volume_requests[0])
    del expected_pod.spec.disk_volume_requests[:]

    yputil.cast_qemu_volumes_to_pod_disk_volume_requests(
        pod_id=vm.meta.id,
        vm_spec=vm.spec,
        disk_volume_requests=expected_pod.spec.disk_volume_requests,
        root_fs_req=root_fs_disc_volume_req)

    pod_updates = gen_pod_updates(vm, {
        '/spec/iss': yputil.dumps_proto(expected_iss_proto),
        '/labels/vmagent_version': yson.dumps(new_vmagent_version),
        '/labels/qyp_vm_mark': yson.dumps({update_action.UPDATE_VMAGENT_VM_MARK_LABEL: 'in_progress'}),
        '/spec/disk_volume_requests': helpers.yson_dumps_list_of_proto(pod.spec.disk_volume_requests),
        '/spec/dynamic_attributes/annotations': yson.dumps(["qyp_vm_spec", "qyp_ssh_authorized_keys"]),
        "/annotations/qyp_vm_spec": helpers.yson_dumps_vm(vmset_pb2.VM(spec=vm.spec, meta=vm.meta)),
    })

    pod_set_updates = gen_pod_set_updates(vm, {
        '/labels/vmagent_version': yson.dumps(new_vmagent_version),
        '/labels/qyp_vm_mark': yson.dumps({update_action.UPDATE_VMAGENT_VM_MARK_LABEL: 'in_progress'}),
    })

    prev_vmagent_resource_url = pod.spec.iss.instances[0].entity.instance.resources[
        yputil.VMAGENT_RESOURCE_NAME].dynamicResource
    current_vmagent_resource_url = expected_iss_proto.instances[0].entity.instance.resources[
        yputil.VMAGENT_RESOURCE_NAME].dynamicResource
    assert prev_vmagent_resource_url != current_vmagent_resource_url
    assert_called_with(ctx_mock.pod_ctl.update_pod, vm.meta.id, vm.meta.version.pod, pod_updates, t_id, 0)
    assert_called_with(ctx_mock.pod_ctl.update_pod_set, vm.meta.id, vm.meta.version.pod_set, pod_set_updates, t_id, 0)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
@pytest.mark.parametrize('default_vmagent_version', ['0.27'])
def test_update_vmagent_with_prev_qdm_eviction(vm, config, pod_ctl_mock, ctx_mock, staff_client_mock,
                                               abc_client_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set,
                                               gen_expected_iss_payload, default_vmagent_version):
    vm.spec.qemu.volumes[0].image_type = vmset_pb2.Volume.RAW
    vm.spec.qemu.volumes[0].resource_url = 'qdm:'
    pod.CopyFrom(gen_expected_pod())
    current_iss_proto = pod.spec.iss
    pod_set.CopyFrom(gen_expected_pod_set())

    config_from_resources = vmagent_pb2.VMConfig()
    config_from_resources.disk.type = vmagent_pb2.VMDisk.RAW
    config_from_resources.disk.resource.rb_torrent = 'qdm:'
    config_action.put_config_as_resource(current_iss_proto, config_from_resources)

    with mock.patch('infra.qyp.vmproxy.src.action.update.get_vm_status') as get_vm_status:
        get_vm_status_resp = vmagent_api_pb2.VMStatusResponse()
        get_vm_status_resp.config.vcpu = vm.spec.qemu.resource_requests.vcpu_limit / 1000
        get_vm_status_resp.config.mem = vm.spec.qemu.resource_requests.memory_limit - 1024 ** 3
        get_vm_status_resp.config.disk.type = vmagent_pb2.VMDisk.DELTA
        get_vm_status_resp.config.disk.resource.rb_torrent = 'qdm:'
        get_vm_status_resp.state.type = vmagent_pb2.VMState.RUNNING
        get_vm_status.return_value = get_vm_status_resp
        t_id = 'transaction'
        ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)
        new_vmagent_version = '0.28'

        config.set_value('vmproxy.default_vmagent.version', new_vmagent_version)
        config.set_value('vmproxy.default_pod_resources.vmagent.url', 'rbtorrent:new_url')

        update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, update_vmagent=True)

        get_vm_status.assert_called_with(ctx_mock, pod)
        vm.spec.qemu.volumes[0].image_type = vmset_pb2.Volume.DELTA

        new_pod = gen_expected_pod()
        new_iss_proto = new_pod.spec.iss
        config_action.put_config_as_resource(new_iss_proto, config_from_resources)

        vm.spec.vmagent_version = new_vmagent_version

        root_fs_disc_volume_req = data_model.TPodSpec.TDiskVolumeRequest()
        root_fs_disc_volume_req.CopyFrom(pod.spec.disk_volume_requests[0])
        del pod.spec.disk_volume_requests[:]

        yputil.cast_qemu_volumes_to_pod_disk_volume_requests(
            pod_id=vm.meta.id,
            vm_spec=vm.spec,
            disk_volume_requests=pod.spec.disk_volume_requests,
            root_fs_req=root_fs_disc_volume_req)

        pod_updates = gen_pod_updates(vm, {
            '/spec/iss': yputil.dumps_proto(new_iss_proto),
            '/labels/vmagent_version': yson.dumps(new_vmagent_version),
            '/labels/qyp_vm_mark': yson.dumps({update_action.UPDATE_VMAGENT_VM_MARK_LABEL: 'in_progress'}),
            '/spec/disk_volume_requests': helpers.yson_dumps_list_of_proto(pod.spec.disk_volume_requests),
            '/spec/dynamic_attributes/annotations': yson.dumps(["qyp_vm_spec", "qyp_ssh_authorized_keys"]),
            "/annotations/qyp_vm_spec": helpers.yson_dumps_vm(vmset_pb2.VM(spec=vm.spec, meta=vm.meta)),
        })

        pod_set_updates = gen_pod_set_updates(vm, {
            '/labels/vmagent_version': yson.dumps(new_vmagent_version),
            '/labels/qyp_vm_mark': yson.dumps({update_action.UPDATE_VMAGENT_VM_MARK_LABEL: 'in_progress'}),
        })

        prev_vmagent_resource_url = pod.spec.iss.instances[0].entity.instance.resources[
            yputil.VMAGENT_RESOURCE_NAME].dynamicResource
        current_vmagent_resource_url = new_iss_proto.instances[0].entity.instance.resources[
            yputil.VMAGENT_RESOURCE_NAME].dynamicResource
        assert prev_vmagent_resource_url != current_vmagent_resource_url
        assert_called_with(ctx_mock.pod_ctl.update_pod, vm.meta.id, vm.meta.version.pod, pod_updates, t_id, 0)
        assert_called_with(ctx_mock.pod_ctl.update_pod_set, vm.meta.id, vm.meta.version.pod_set, pod_set_updates, t_id,
                           0)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_update_labels(vm, config, pod_ctl_mock, ctx_mock, staff_client_mock, sec_policy_mock,
                       abc_client_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set,
                       gen_expected_iss_payload):
    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())

    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)
    # case: skip update labels for non root users
    vm.spec.labels['test'] = 'test'
    ctx_mock.sec_policy.is_root.return_value = False
    update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False, update_labels=True)

    ctx_mock.pod_ctl.update_pod.assert_not_called()
    ctx_mock.pod_ctl.update_pod_set.assert_not_called()

    # case: update labels for root users
    vm.spec.labels['test'] = 'test'
    ctx_mock.sec_policy.is_root.return_value = True
    update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False, update_labels=True)

    pod_updates = gen_pod_updates(vm, {
        '/labels/qyp_vm_mark': yson.dumps({'test': 'test'}),
    })
    pod_set_updates = gen_pod_set_updates(vm, {
        '/labels/qyp_vm_mark': yson.dumps({'test': 'test'}),
    })

    assert_called_with(ctx_mock.pod_ctl.update_pod, vm.meta.id, vm.meta.version.pod, pod_updates, t_id, 0)
    assert_called_with(ctx_mock.pod_ctl.update_pod_set, vm.meta.id, vm.meta.version.pod_set, pod_set_updates, t_id, 0)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_update_volumes_1(vm_qdm, config, pod_ctl_mock, ctx_mock, staff_client_mock, sec_policy_mock,
                          abc_client_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set,
                          gen_expected_iss_payload):
    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())
    ctx_mock.sec_policy = security_policy.SecurityPolicy()

    # case: update qdm for not user nor root, but there is no changes in disk
    qdmspec = qdm_pb2.QDMBackupSpec(
        qdm_spec_version=1,
        filemap=[qdm_pb2.QDMBackupFileSpec()],
        vmspec=vmset_pb2.VM(
            spec=vmset_pb2.VMSpec(
                qemu=vmset_pb2.QemuVMSpec(
                    volumes=[vmset_pb2.Volume(
                        storage_class='hdd'
                    )]
                )
            ),
            meta=vmset_pb2.VMMeta(
                id='pod-id-2',
                auth=vmset_pb2.Auth(
                    owners=vmset_pb2.StaffUsersGroup(
                        logins=['not_author']
                    )
                )
            )
        )
    )

    ctx_mock.qdm_client.get_revision_info.return_value = qdmspec

    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)

    vm_qdm.spec.qemu.volumes[0].resource_url = 'qdm:testing'
    update_action.run(vm_qdm.meta, vm_qdm.spec, ctx_mock, LOGIN, False)
    ctx_mock.qdm_client.get_revision_info.assert_not_called()


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_update_volumes_2(vm, config, pod_ctl_mock, ctx_mock, staff_client_mock, sec_policy_mock,
                          abc_client_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set,
                          gen_expected_iss_payload):
    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())
    ctx_mock.sec_policy = security_policy.SecurityPolicy()

    # case: qdmspec is None
    qdmspec = None

    ctx_mock.qdm_client.get_revision_info.return_value = qdmspec

    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)

    vm.spec.qemu.volumes[0].resource_url = 'qdm:testing'

    with pytest.raises(ValueError) as error:
        update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)

    assert 'QDM not found resource with id: qdm:testing' == error.value.message

    # case: update qdm for not user nor root

    qdmspec = qdm_pb2.QDMBackupSpec(
        qdm_spec_version=1,
        filemap=[qdm_pb2.QDMBackupFileSpec()],
        vmspec=vmset_pb2.VM(
            spec=vmset_pb2.VMSpec(
                qemu=vmset_pb2.QemuVMSpec(
                    volumes=[vmset_pb2.Volume(
                        storage_class='hdd'
                    )]
                )
            ),
            meta=vmset_pb2.VMMeta(
                id='pod-id-2',
                auth=vmset_pb2.Auth(
                    owners=vmset_pb2.StaffUsersGroup(
                        logins=['not_author']
                    )
                )
            )
        )
    )

    ctx_mock.qdm_client.get_revision_info.return_value = qdmspec

    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)

    vm.spec.qemu.volumes[0].resource_url = 'qdm:testing'

    with pytest.raises(errors.AuthorizationError) as error:
        update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)

    assert 'Attempt to restore VM backup by person-non-owner' == error.value.message

    # case: change qdm by not root but owner
    qdmspec = qdm_pb2.QDMBackupSpec(
        qdm_spec_version=1,
        filemap=[qdm_pb2.QDMBackupFileSpec()],
        vmspec=vmset_pb2.VM(
            spec=vmset_pb2.VMSpec(
                qemu=vmset_pb2.QemuVMSpec(
                    volumes=[vmset_pb2.Volume(
                        storage_class='hdd'
                    )]
                )
            ),
            meta=vmset_pb2.VMMeta(
                id='pod-id-2',
                auth=vmset_pb2.Auth(
                    owners=vmset_pb2.StaffUsersGroup(
                        logins=['author']
                    )
                )
            )
        )
    )

    ctx_mock.qdm_client.get_revision_info.return_value = qdmspec

    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)

    vm.spec.qemu.volumes[0].resource_url = 'qdm:testing'
    update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)

    root_fs_disc_volume_req = data_model.TPodSpec.TDiskVolumeRequest()
    root_fs_disc_volume_req.CopyFrom(pod.spec.disk_volume_requests[0])
    del pod.spec.disk_volume_requests[:]

    yputil.cast_qemu_volumes_to_pod_disk_volume_requests(
        pod_id=vm.meta.id,
        vm_spec=vm.spec,
        disk_volume_requests=pod.spec.disk_volume_requests,
        root_fs_req=root_fs_disc_volume_req)

    pod_updates = gen_pod_updates(vm, {
        '/spec/disk_volume_requests': helpers.yson_dumps_list_of_proto(pod.spec.disk_volume_requests),
        "/annotations/qyp_vm_spec": helpers.yson_dumps_vm(vmset_pb2.VM(spec=vm.spec, meta=vm.meta)),
    })

    assert_called_with(ctx_mock.pod_ctl.update_pod, vm.meta.id, vm.meta.version.pod, pod_updates, t_id, 0)
    assert_called_with(ctx_mock.qdm_client.get_revision_info, 'qdm:testing')

    # case: add 2nd qdm by not root and not owner
    qdmspec = qdm_pb2.QDMBackupSpec(
        qdm_spec_version=1,
        filemap=[qdm_pb2.QDMBackupFileSpec()],
        vmspec=vmset_pb2.VM(
            spec=vmset_pb2.VMSpec(
                qemu=vmset_pb2.QemuVMSpec(
                    volumes=[vmset_pb2.Volume(
                        storage_class='sdd'
                    )]
                )
            ),
            meta=vmset_pb2.VMMeta(
                id='pod-id-3',
                auth=vmset_pb2.Auth(
                    owners=vmset_pb2.StaffUsersGroup(
                        logins=['not_author']
                    )
                )
            )
        )
    )

    ctx_mock.qdm_client.get_revision_info.return_value = qdmspec

    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)

    vm.spec.qemu.volumes[0].resource_url = 'qdm:testing'
    v = vm.spec.qemu.volumes.add()
    v.capacity = 10 * 1024 ** 3
    v.storage_class = 'ssd'
    v.resource_url = 'qdm:sas'
    v.name = 'sas'
    v.pod_mount_path = '/sas'
    v.vm_mount_path = '/sas'

    with pytest.raises(errors.AuthorizationError) as error:
        update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)

    assert 'Attempt to restore VM backup by person-non-owner' == error.value.message


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_update_gpu_request(vm, config, ctx_mock, pod, pod_set, gen_expected_pod, gen_expected_pod_set,
                            gen_expected_iss_payload, patch_uuid):
    gpu_model = 'gpu_tesla_k40'
    pod.CopyFrom(gen_expected_pod())
    pod_set.CopyFrom(gen_expected_pod_set())
    gpu_req = pod.spec.gpu_requests.add()
    gpu_req.model = gpu_model

    t_id = 'transaction'
    ctx_mock.pod_ctl.start_transaction.return_value = (t_id, 0)
    vm.spec.qemu.gpu_request.capacity = 2
    vm.spec.qemu.gpu_request.model = gpu_model
    vm.spec.qemu.qemu_options.audio = vm.spec.qemu.qemu_options.HDA

    new_pod = gen_expected_pod()
    new_iss_proto = gen_expected_iss_payload(new_pod)
    new_iss_proto.instances[0].properties['QYP_GPU'] = '{}-2-0-0'.format(gpu_model)

    pod_updates = gen_pod_updates(vm, {
        '/spec/iss': yputil.dumps_proto(new_iss_proto),
        '/annotations/qyp_vm_spec': helpers.yson_dumps_vm(vmset_pb2.VM(spec=vm.spec, meta=vm.meta)),
        '/spec/gpu_requests': helpers.yson_dumps_list_of_proto([
            data_model.TPodSpec.TGpuRequest(id='version', model=gpu_model),
            data_model.TPodSpec.TGpuRequest(id='version', model=gpu_model),
        ]),
        '/labels/version': yson.dumps('version'),
    })
    pod_set_updates = {}

    update_action.run(vm.meta, vm.spec, ctx_mock, LOGIN, False)
    assert_called_with(ctx_mock.pod_ctl.update_pod, vm.meta.id, vm.meta.version.pod, pod_updates, t_id, 0)
    assert_called_with(ctx_mock.pod_ctl.update_pod_set, vm.meta.id, vm.meta.version.pod_set, pod_set_updates, t_id, 0)
