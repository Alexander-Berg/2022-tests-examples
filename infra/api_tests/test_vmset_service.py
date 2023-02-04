import base64
import datetime
import freezegun

from dateutil import parser
import inject
import mock
import pytest
import yp.common as yp_common
import yp.data_model as data_model
from yp_proto.yp.client.api.proto import object_service_pb2

from yt import yson
from infra.swatlib.auth import staff, abc
from infra.swatlib.rpc import exceptions, authentication
from infra.swatlib.rpc.authentication import AuthSubject
from infra.qyp.proto_lib import vmset_api_pb2, vmagent_api_pb2, vmset_pb2, qdm_pb2
from infra.qyp.vmproxy.src import security_policy, pod_controller, vm_instance
from infra.qyp.vmproxy.src.web import vmset_service
from infra.qyp.vmproxy.src.lib.yp import yputil


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


def test_allocate_vm(ctx_mock, call):
    def configure(binder):
        binder.bind(staff.IStaffClient, mock.Mock())

    inject.clear_and_configure(configure)

    ctx_mock.sec_policy = security_policy.SecurityPolicy()
    # Case 1: owners not set
    req = vmset_api_pb2.AllocateVmRequest()
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.allocate_vm, req)

    # Case 2: ok
    ctx_mock.reset_mock()
    req = vmset_api_pb2.AllocateVmRequest()
    req.meta.auth.owners.logins.append('anonymous')
    allocate_run_mock = mock.Mock()
    with mock.patch('infra.qyp.vmproxy.src.action.create.run', allocate_run_mock):
        call(vmset_service.allocate_vm, req)
        assert_called_with(allocate_run_mock, req.meta, req.spec, ctx_mock, 'anonymous')


def test_deallocate_vm(ctx_mock, call):
    ctx_mock.sec_policy = security_policy.SecurityPolicy()
    ctx_mock.pod_ctl.check_write_permission.return_value = True

    # Case 1: deallocate successful
    req = vmset_api_pb2.DeallocateVmRequest()
    req.id = 'real-pod-id'
    call(vmset_service.deallocate_vm, req)
    assert_called_with(ctx_mock.pod_ctl.delete_pod_with_pod_set, req.id)

    # Case 2: deallocate failure
    ctx_mock.reset_mock()
    ctx_mock.pod_ctl.check_write_permission.return_value = False
    with pytest.raises(exceptions.ForbiddenError):
        req = vmset_api_pb2.DeallocateVmRequest()
        req.id = 'real-pod-id'
        call(vmset_service.deallocate_vm, req)


def test_container_access_ctrl(ctx_mock, call):
    def configure(binder):
        binder.bind(staff.IStaffClient, mock.Mock())

    inject.clear_and_configure(configure)

    # Check invalid requests
    bad_r = vmset_api_pb2.CheckVmAccessRequest()
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.check_vm_access, bad_r)
    bad_r.login = 'xakep'
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.check_vm_access, bad_r)
    pod_id = 'my_service_id'
    author = 'george_amberson'

    def get_pod(object_id, ts=None):
        if object_id != pod_id:
            raise yp_common.YpNoSuchObjectError(error={})

    def check_read_permission(pod_id, subject_id):
        return subject_id in [author]

    ctx_mock.pod_ctl.sec_policy = security_policy.SecurityPolicy(is_enabled=True, root_users=['george_amberson'])
    ctx_mock.pod_ctl.check_read_permission.side_effect = check_read_permission
    ctx_mock.pod_ctl.get_pod.side_effect = get_pod
    auth_subject = authentication.AuthSubject(login='nobody')
    keys = {
        'fingerprint_sha256': 'fingerprint_sha256',
        'fingerprint': 'key_fingerprint',
        'id': '1',
        'description': 'description',
        'key': 'key_content'
    }
    list_key = yson.YsonList()
    list_key.append(yson.YsonMap(keys))
    ret_value = [
        list_key
    ]
    ctx_mock.pod_ctl.get_keys_by_logins.return_value = ret_value

    # Test correct scenario
    request1 = vmset_api_pb2.CheckVmAccessRequest(login=author, service_id=pod_id)
    response1 = call(vmset_service.check_vm_access, request1, auth_subject)
    correct_response1 = vmset_api_pb2.CheckVmAccessResponse(
        access_granted=True,
        keys=[
            vmset_api_pb2.UserKey(
                key='key_content',
                fingerprint='key_fingerprint',
            )
        ]
    )

    assert response1 == correct_response1
    vmset_service.access_response_cache.clear()

    # Test invalid user scenario
    ctx_mock.reset_value()
    request2 = vmset_api_pb2.CheckVmAccessRequest(login='peeky_sib', service_id=pod_id)
    response2 = call(vmset_service.check_vm_access, request2, auth_subject)
    correct_response2 = vmset_api_pb2.CheckVmAccessResponse(
        access_granted=False,
        keys=[]
    )
    assert response2 == correct_response2
    vmset_service.access_response_cache.clear()

    # Test invalid staff response scenario
    ctx_mock.reset_value()
    keys = {
        'error_fingerprint_sha256': 'fingerprint_sha256',
        'error_fingerprint': 'key_fingerprint',
        'error_id': '1',
        'error_description': 'description',
        'error_key': 'key_content'
    }
    list_key = yson.YsonList()
    list_key.append(yson.YsonMap(keys))
    ret_value = [
        list_key
    ]
    ctx_mock.pod_ctl.get_keys_by_logins.return_value = ret_value

    request3 = vmset_api_pb2.CheckVmAccessRequest(login=author, service_id=pod_id)
    response3 = call(vmset_service.check_vm_access, request3, auth_subject)
    correct_response3 = vmset_api_pb2.CheckVmAccessResponse(
        access_granted=True,
        keys=[]
    )
    assert response3 == correct_response3
    vmset_service.access_response_cache.clear()


def test_get_status(ctx_mock, call):
    ctx_mock.sec_policy = security_policy.SecurityPolicy()
    ctx_mock.nanny_client.get_service_instances.return_value = [
        {
            'hostname': 'man1-0000',
            'port': 7255
        }
    ]
    owners = {'logins': ['volozh'], 'groups': []}
    ctx_mock.nanny_client.get_service_owners.return_value = owners
    ctx_mock.pod_ctl.get_pod_owners.return_value = owners
    vmagent_resp = vmagent_api_pb2.VMStatusResponse()
    vmagent_resp.config.id = '12345'
    content = base64.b64encode(vmagent_resp.SerializeToString())
    ctx_mock.vmagent_client.status.return_value = content

    # Case 1: no vm_id set
    req = vmset_api_pb2.GetStatusRequest()
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.get_status, req)

    # Case 2: nanny args, ok
    ctx_mock.reset_mock()
    req = vmset_api_pb2.GetStatusRequest()
    req.vm_id.nanny_args.host = 'man1-0000'
    req.vm_id.nanny_args.port = 7255
    req.vm_id.nanny_args.service = 'test-service'
    resp = call(vmset_service.get_status, req, auth_subject=AuthSubject('volozh'))
    assert resp == vmset_api_pb2.GetStatusResponse(
        state=vmagent_resp.state,
        config=vmagent_resp.config,
        data_transfer_state=vmagent_resp.data_transfer_state,
    )

    # Case 3: pod_id, ok
    ctx_mock.reset_mock()
    req = vmset_api_pb2.GetStatusRequest()
    req.vm_id.pod_id = 'real-pod-id'
    resp = call(vmset_service.get_status, req, auth_subject=AuthSubject('volozh'))
    assert resp == vmset_api_pb2.GetStatusResponse(
        state=vmagent_resp.state,
        config=vmagent_resp.config,
        data_transfer_state=vmagent_resp.data_transfer_state,
    )


def test_make_action(ctx_mock, call):
    ctx_mock.sec_policy = security_policy.SecurityPolicy()
    ctx_mock.nanny_client.get_service_instances.return_value = [
        {
            'hostname': 'man1-0000',
            'port': 7255
        }
    ]
    owners = {'logins': ['volozh'], 'groups': []}
    ctx_mock.nanny_client.get_service_owners.return_value = owners
    ctx_mock.pod_ctl.get_pod_owners.return_value = owners
    pod = data_model.TPod()
    ctx_mock.pod_ctl.get_active_pod.return_value = pod
    vmagent_resp = vmagent_api_pb2.VMActionResponse()
    content = base64.b64encode(vmagent_resp.SerializeToString())
    ctx_mock.vmagent_client.action.return_value = content

    # Case 1: no vm_id set
    req = vmset_api_pb2.MakeActionRequest()
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.get_status, req)

    # Case 2: nanny args, ok
    ctx_mock.reset_mock()
    req = vmset_api_pb2.MakeActionRequest()
    req.vm_id.nanny_args.host = 'man1-0000'
    req.vm_id.nanny_args.port = 7255
    req.vm_id.nanny_args.service = 'test-service'
    req.action = vmagent_api_pb2.VMActionRequest.START
    resp = call(vmset_service.make_action, req, auth_subject=AuthSubject('volozh'))
    assert resp == vmset_api_pb2.MakeActionResponse(state=vmagent_resp.state)

    # Case 3: pod_id, ok
    ctx_mock.reset_mock()
    req = vmset_api_pb2.MakeActionRequest()
    req.vm_id.pod_id = 'real-pod-id'
    req.action = vmagent_api_pb2.VMActionRequest.START
    resp = call(vmset_service.make_action, req, auth_subject=AuthSubject('volozh'))
    assert resp == vmset_api_pb2.MakeActionResponse(state=vmagent_resp.state)

    # Case 5: backup, yp, not vmagent_version
    ctx_mock.reset_mock()
    req = vmset_api_pb2.MakeActionRequest()
    req.action = vmagent_api_pb2.VMActionRequest.BACKUP
    req.vm_id.pod_id = 'real-pod-id'
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.make_action, req, auth_subject=AuthSubject('volozh'))


def test_make_action_push_config(ctx_mock, call):
    ctx_mock.sec_policy = security_policy.SecurityPolicy()
    ctx_mock.nanny_client.get_service_instances.return_value = [
        {
            'hostname': 'man1-0000',
            'port': 7255
        }
    ]
    owners = {'logins': ['volozh'], 'groups': []}
    ctx_mock.nanny_client.get_service_owners.return_value = owners
    ctx_mock.pod_ctl.get_pod_owners.return_value = owners
    pod = data_model.TPod()
    pod.labels.attributes.add(key='vmagent_version', value=yson.dumps('0.27'))
    ctx_mock.pod_ctl.get_active_pod.return_value = pod
    vmagent_resp = vmagent_api_pb2.VMActionResponse()
    content = base64.b64encode(vmagent_resp.SerializeToString())
    ctx_mock.vmagent_client.action.return_value = content

    # Case 4: config action for vmagent ver < 0.28
    pod.labels.attributes.add(key='vmagent_version', value=yson.dumps('0.27'))
    ctx_mock.reset_mock()
    req = vmset_api_pb2.MakeActionRequest()
    req.vm_id.pod_id = 'real-pod-id'
    req.action = vmagent_api_pb2.VMActionRequest.PUSH_CONFIG
    req.config.mem = 1
    req.config.disk.delta_size = 1
    config_run_mock = mock.Mock()
    with mock.patch('infra.qyp.vmproxy.src.action.config.run', config_run_mock):
        resp = call(vmset_service.make_action, req, auth_subject=AuthSubject('volozh'))
        assert resp == vmset_api_pb2.MakeActionResponse(state=vmagent_resp.state)
        config_run_mock.assert_called_once_with(mock.ANY, req.config, ctx_mock)

    # Case 4: config action for vmagent ver >= 0.28
    pod.labels.attributes.add(key='vmagent_version', value=yson.dumps('0.28'))
    ctx_mock.reset_mock()
    req = vmset_api_pb2.MakeActionRequest()
    req.vm_id.pod_id = 'real-pod-id'
    req.action = vmagent_api_pb2.VMActionRequest.PUSH_CONFIG
    req.config.mem = 1
    req.config.disk.delta_size = 1
    update_run_mock = mock.Mock()
    yputil_cast_pod_to_vm = mock.Mock()
    vm = vmset_pb2.VM()
    yputil_cast_pod_to_vm.return_value = vm
    with mock.patch('infra.qyp.vmproxy.src.lib.yp.yputil.cast_pod_to_vm', yputil_cast_pod_to_vm):
        with mock.patch('infra.qyp.vmproxy.src.action.update.run', update_run_mock):
            resp = call(vmset_service.make_action, req, auth_subject=AuthSubject('volozh'))
            assert resp == vmset_api_pb2.MakeActionResponse(state=vmagent_resp.state)
            update_run_mock.assert_called_once_with(
                meta=vm.meta,
                spec=vm.spec,
                ctx=mock.ANY,
                login='volozh',
                config=req.config,
                update_vmagent=False,
                update_labels=False,
            )


def test_get_vm_stats(ctx_mock, call):
    req = vmset_api_pb2.GetVmStatsRequest()
    req.consistency = vmset_api_pb2.STRONG
    yp_client = mock.Mock()
    disk_request = [{
        'id': 'pod-id',
        'storage_class': 'hdd',
        'labels': {
            'mount_path': yputil.MAIN_VOLUME_POD_MOUNT_PATH
        },
        'quota_policy': {
            'capacity': 300 * 1024 ** 3
        }
    }]
    ret_value = object_service_pb2.TRspSelectObjects()
    ret_value.results.extend([
        object_service_pb2.TAttributeList(values=[
            str(8),
            str(50),
            yson.dumps(disk_request),
            yson.dumps('0.20')
        ]),
    ])
    yp_client.list_pods.return_value = ret_value
    ctx_mock.pod_ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                                    sec_policy=security_policy.SecurityPolicy())

    expected_result = vmset_api_pb2.GetVmStatsResponse()
    expected_result.usage.mem = 50
    expected_result.usage.cpu = 8
    expected_result.usage.disk_per_storage['hdd'] = 300 * 1024 ** 3
    expected_result.total = 1
    expected_result.vmagent_versions['0.20'] = 1
    result = call(vmset_service.get_vm_stats, req, auth_subject=AuthSubject('volozh'))

    assert result == expected_result


def test_list_user_accounts(ctx_mock, call):
    ctx_mock.personal_quotas_dict = {
        'acc3': {
            'segment': 'dev',
            'cpu': 2000,
            'mem': 8589934592,
            'disk':
                [
                    {
                        'storage': 'hdd',
                        'capacity': 21474836480,
                    }
                ],
            'internet_address': 0
        }
    }
    yp_client = mock.Mock()
    ctx_mock.pod_ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                                    sec_policy=security_policy.SecurityPolicy())
    req = vmset_api_pb2.ListUserAccountsRequest()

    # Case 1: login not exits
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.list_user_accounts, req, auth_subject=AuthSubject('volozh'))

    # Case 2: ok
    def list_members_iter(spec, **kwargs):
        yield {'results': [
            {'service': {'id': 'acc1'}, 'person': {'login': 'volozh'}},
        ]}
        return

    abc_client = mock.Mock()
    abc_client.list_members_iter.side_effect = list_members_iter

    def configure(binder):
        binder.bind(abc.IAbcClient, abc_client)

    inject.clear_and_configure(configure)
    ret_value = object_service_pb2.TRspGetObjects()
    ret_value.subresponses.extend([
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            str('acc1'),
            '[{}]'.format(' ;'.join(['user', 'robot', 'volozh'])),
        ]))
    ])
    yp_client.get_groups.return_value = ret_value

    req.consistency = vmset_api_pb2.STRONG
    req.login = 'volozh'
    req.segment = 'dev'
    req.account_id = 'acc1'
    acc = data_model.TAccount()
    acc.meta.id = 'acc1'
    acc.spec.resource_limits.per_segment['dev'].cpu.capacity = 1000
    acc.spec.resource_limits.per_segment['default'].cpu.capacity = 1000
    acc.spec.resource_limits.per_segment['dev'].internet_address.capacity = 1
    acc.spec.resource_limits.per_segment['dev'].disk_per_storage_class['hdd'].capacity = 21474836480
    acc.spec.resource_limits.per_segment['dev'].disk_per_storage_class['hdd'].bandwidth = 30 * 1024 ** 2
    res_acc = vmset_pb2.Account()
    res_acc.id = 'acc1'
    res_acc.limits.per_segment['dev'].cpu = 1000
    res_acc.limits.per_segment['dev'].disk_per_storage['hdd'] = 21474836480
    res_acc.limits.per_segment['dev'].internet_address = 1
    res_acc.limits.per_segment['dev'].io_guarantees_per_storage['hdd'] = 30 * 1024 ** 2
    res_acc.members_count = 1
    ret_value = object_service_pb2.TRspGetObjects()
    ret_value.subresponses.extend([
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(acc),
        ])),
    ])
    yp_client.get_accounts.return_value = ret_value
    result = call(vmset_service.list_user_accounts, req, auth_subject=('volozh'))
    assert result.accounts[0] == res_acc


def test_backup_vm(ctx_mock, call):
    CONF_ID = 'conf-1234'
    ctx_mock.sec_policy = security_policy.SecurityPolicy()
    yp_client = mock.Mock()
    ctx_mock.pod_ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                                    sec_policy=ctx_mock.sec_policy)
    ctx_mock.vmagent_client = mock.Mock()
    req = vmset_api_pb2.BackupVmRequest()
    req.vm_id.pod_id = 'pod-id'
    pod = data_model.TPod()
    ip_addr = pod.status.ip6_address_allocations.add()
    ip_addr.address = '::1'
    ip_addr.vlan_id = 'backbone'
    state = pod.status.agent.iss.currentStates.add()
    state.currentState = 'ACTIVE'
    state.workloadId.configuration.groupStateFingerprint = CONF_ID
    pod.labels.attributes.add(key='vmagent_version', value=yson.dumps('0.16'))
    pod.status.scheduling.state = 300
    i = pod.spec.iss.instances.add()
    i.id.configuration.groupStateFingerprint = CONF_ID
    state = pod.status.agent.iss.currentStates.add()
    state.currentState = CONF_ID
    state.workloadId.configuration.groupStateFingerprint = CONF_ID

    ret_value = object_service_pb2.TRspGetObject()
    ret_value.result.values.extend([yputil.dumps_proto(pod)])
    yp_client.get_object.return_value = ret_value
    instance = vm_instance.PodVMInstance.from_pod_id(
        pod_id=req.vm_id.pod_id,
        ctx=ctx_mock
    )
    vmagent_req = vmagent_api_pb2.VMActionRequest()
    vmagent_req.action = vmagent_api_pb2.VMActionRequest.BACKUP
    encoded_data = base64.b64encode(vmagent_req.SerializeToString())
    call(vmset_service.backup_vm, req, auth_subject=AuthSubject('volozh'))
    assert_called_with(ctx_mock.vmagent_client.action, url=instance.get_agent_url(), data=encoded_data)


@freezegun.freeze_time(datetime.datetime.utcfromtimestamp(0))
def test_create_vm(vm, ctx_mock, config, call, abc_client_mock, staff_client_mock, gen_expected_pod,
                   gen_expected_pod_set):
    config.set_value('vmproxy.node_segment', ['test', 'default'])
    config.set_value('vmproxy.pass_ssh_keys', True)

    abc_client_mock._client.get.return_value = {'results': [{'id': 'id', 'slug': 'slug'}]}
    staff_client_mock.list_groups.return_value = {'type': 'department', 'department': {'id': '1'}}

    vm.meta.auth.owners.logins.extend(['volozh', 'other-user'])
    vm.meta.auth.owners.group_ids.extend(['1234'])
    vm.meta.author = 'volozh'
    vm.spec.qemu.node_segment = 'default'

    req = vmset_api_pb2.CreateVmRequest()
    req.spec.CopyFrom(vm.spec)
    req.meta.CopyFrom(vm.meta)

    ctx_mock.sec_policy = security_policy.SecurityPolicy()
    ctx_mock.pod_ctl.get_keys_by_logins.return_value = [[{'key': 'key'}]]
    expected_pod = gen_expected_pod()
    expected_pod_set = gen_expected_pod_set(owners_ace_extend=['volozh', 'other-user', 'staff:department:1'])

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
                id='pod-id',
                auth=vmset_pb2.Auth(
                    owners=vmset_pb2.StaffUsersGroup(
                        logins=['volozh']
                    )
                )
            )
        )
    )

    ctx_mock.qdm_client.get_revision_info.return_value = qdmspec

    # Case 1: ok
    call(vmset_service.create_vm, req, auth_subject=AuthSubject('volozh'))
    assert_called_with(ctx_mock.pod_ctl.create_pod_with_pod_set, expected_pod, expected_pod_set)

    # Case 2: auth error
    del req.meta.auth.owners.group_ids[:]
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.create_vm, req, auth_subject=AuthSubject('fake-volozh'))


def test_list_vm(ctx_mock, call):
    yp_client = mock.Mock()
    yp_client.generate_timestamp.return_value = 0

    pod_set_1 = data_model.TPodSet()
    pod_set_1.meta.id = 'pod-id1'
    pod_set_1.labels.attributes.add(key='version', value=yson.dumps('0.16'))

    pod_set_2 = data_model.TPodSet()
    pod_set_2.meta.id = 'pod-id2'
    pod_set_2.labels.attributes.add(key='version', value=yson.dumps('0.16'))

    pod_set_list = [
        object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(pod_set_1.meta),
            yputil.dumps_proto(pod_set_1.spec),
            yputil.dumps_proto(pod_set_1.labels),
        ]),
        object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(pod_set_2.meta),
            yputil.dumps_proto(pod_set_2.spec),
            yputil.dumps_proto(pod_set_2.labels),
        ]),
    ]
    ret_value = object_service_pb2.TRspSelectObjects()
    ret_value.results.extend(pod_set_list)
    yp_client.list_pod_sets.return_value = ret_value

    pod_1 = data_model.TPod()
    pod_1.meta.id = 'pod-id1'
    pod_1.meta.pod_set_id = 'pod-id1'
    pod_1.labels.attributes.add(key='version', value=yson.dumps('0.16'))

    pod_2 = data_model.TPod()
    pod_2.meta.id = 'pod-id2'
    pod_2.meta.pod_set_id = 'pod-id2'
    ret_value = object_service_pb2.TRspGetObjects()
    pod_2.labels.attributes.add(key='version', value=yson.dumps('0.16'))

    ret_value.subresponses.extend([
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            str(pod_1.meta.id),
            yputil.dumps_proto(pod_1.meta),
            yputil.dumps_proto(pod_1.spec),
            yputil.dumps_proto(pod_1.status),
            yputil.dumps_proto(pod_1.labels),
            yson.dumps({'author': 'volozh', 'groups': [], 'logins': ['volozh']}),
            '#',
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            str(pod_2.meta.id),
            yputil.dumps_proto(pod_2.meta),
            yputil.dumps_proto(pod_2.spec),
            yputil.dumps_proto(pod_2.status),
            yputil.dumps_proto(pod_2.labels),
            yson.dumps({'author': 'other', 'groups': [], 'logins': ['other']}),
            '#',
        ])),
    ])
    yp_client.get_objects.return_value = ret_value
    yp_client.get_user_access_allowed_to.return_value = ['pod-id1', 'pod-id2']

    ctx_mock.sec_policy = security_policy.SecurityPolicy()
    ctx_mock.pod_ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                                    sec_policy=ctx_mock.sec_policy)
    # Case 1: ok, login exist
    ret_value = object_service_pb2.TRspSelectObjects()
    ret_value.results.extend(pod_set_list[:1])
    yp_client.list_pod_sets.return_value = ret_value
    yp_client.get_user_access_allowed_to.return_value = ['pod-id1']
    req = vmset_api_pb2.ListYpVmRequest()
    req.query.login = 'volozh'
    result = call(vmset_service.list_vm, req)

    vm1 = vmset_pb2.VM()
    vm1.meta.id = 'pod-id1'
    vm1.meta.auth.owners.logins.extend(['volozh'])
    vm1.meta.version.pod = '0.16'
    vm1.meta.version.pod_set = '0.16'
    vm1.meta.author = 'volozh'
    vm1.spec.vmagent_version = 'N/A'
    vm1.meta.creation_time.FromMicroseconds(0)
    vm1.meta.last_modification_time.FromMicroseconds(0)
    vm1.spec.qemu.resource_requests.memory_limit = 0
    vm1.spec.qemu.io_guarantees_per_storage['hdd'] = 0
    vm1.spec.qemu.io_guarantees_per_storage['ssd'] = 0
    vm2 = vmset_pb2.VM()
    vm2.meta.id = 'pod-id2'
    vm2.meta.auth.owners.logins.extend(['other'])
    vm2.meta.version.pod = '0.16'
    vm2.meta.version.pod_set = '0.16'
    vm2.meta.author = 'other'
    vm2.spec.vmagent_version = 'N/A'
    vm2.meta.creation_time.FromMicroseconds(0)
    vm2.meta.last_modification_time.FromMicroseconds(0)
    vm2.spec.qemu.resource_requests.memory_limit = 0
    vm2.spec.qemu.io_guarantees_per_storage['hdd'] = 0
    vm2.spec.qemu.io_guarantees_per_storage['ssd'] = 0
    expected = vmset_api_pb2.ListYpVmResponse(vms=[vm1])
    assert result == expected

    # Case 2: ok, login not exist
    ret_value = object_service_pb2.TRspSelectObjects()
    ret_value.results.extend(pod_set_list)
    yp_client.list_pod_sets.return_value = ret_value
    yp_client.get_user_access_allowed_to.return_value = ['pod-id1', 'pod-id2']
    ctx_mock.reset_mock()
    req = vmset_api_pb2.ListYpVmRequest()
    result = call(vmset_service.list_vm, req)
    expected = vmset_api_pb2.ListYpVmResponse(vms=[vm1, vm2])
    assert result == expected

    # Case 3: request over limits
    ctx_mock.reset_mock()
    req = vmset_api_pb2.ListYpVmRequest()
    req.limit = 1024 ** 3
    with pytest.raises(exceptions.BadRequestError):
        call(vmset_service.list_vm, req)


def test_get_vm(ctx_mock, call):
    CONF_ID = 'conf-1234'
    yp_client = mock.Mock()

    ctx_mock.sec_policy = security_policy.SecurityPolicy()
    ctx_mock.pod_ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                                    sec_policy=ctx_mock.sec_policy)
    ctx_mock.vmagent_client.status.return_value = ''

    req = vmset_api_pb2.GetVmRequest()
    req.vm_id = 'pod-id'

    pod = data_model.TPod()
    pod.meta.id = 'pod-id'
    pod.meta.pod_set_id = 'pod-set-id'
    ip_addr = pod.status.ip6_address_allocations.add()
    ip_addr.address = '::1'
    ip_addr.vlan_id = 'backbone'
    i = pod.spec.iss.instances.add()
    i.id.configuration.groupStateFingerprint = CONF_ID
    state = pod.status.agent.iss.currentStates.add()
    state.currentState = 'ACTIVE'
    state.workloadId.configuration.groupStateFingerprint = CONF_ID
    pod.labels.attributes.add(key='vmagent_version', value=yson.dumps('0.16'))
    pod.status.scheduling.state = 300
    owners_dict = dict()
    owners_dict['logins'] = ['volozh', 'other-user']
    owners_dict['groups'] = [1234]
    pod.annotations.attributes.add(key='owners', value=yson.dumps(owners_dict))
    pod.labels.attributes.add(key='version', value=yson.dumps('0.16'))
    pod_set = data_model.TPodSet()
    pod_set.meta.id = 'pod-set-id'
    pod_set.labels.attributes.add(key='version', value=yson.dumps('0.16'))

    ret_value_1 = object_service_pb2.TRspGetObject()
    ret_value_1.result.values.extend([yputil.dumps_proto(pod)])
    ret_value_2 = object_service_pb2.TRspGetObject()
    ret_value_2.result.values.extend([yputil.dumps_proto(pod_set)])
    yp_client.get_object.side_effect = [ret_value_1, ret_value_1, ret_value_2]

    result = call(vmset_service.get_vm, req, auth_subject=AuthSubject('volozh'))

    vm = vmset_pb2.VM()
    vm.meta.id = 'pod-id'
    vm.meta.auth.owners.logins.extend(['volozh', 'other-user'])
    vm.meta.auth.owners.group_ids.extend(['1234'])
    vm.meta.version.pod = '0.16'
    vm.meta.version.pod_set = '0.16'
    vm.spec.vmagent_version = '0.16'
    vm.spec.qemu.io_guarantees_per_storage['hdd'] = 0
    vm.spec.qemu.io_guarantees_per_storage['ssd'] = 0
    ip_addr = vm.status.ip_allocations.add()
    ip_addr.address = '::1'
    ip_addr.vlan_id = 'backbone'
    vm.meta.creation_time.FromMicroseconds(0)
    vm.meta.last_modification_time.FromMicroseconds(0)
    vm.spec.qemu.resource_requests.memory_limit = 0
    vm.status.state.Clear()
    vm.config.mem = 0
    expected = vmset_api_pb2.GetVmResponse(vm=vm)
    assert result == expected


def test_restore_backup(ctx_mock, qdm_client_mock, call):
    CONF_ID = 'conf-1234'
    yp_client = mock.Mock()

    ctx_mock.sec_policy = security_policy.SecurityPolicy()
    ctx_mock.pod_ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                                    sec_policy=ctx_mock.sec_policy)
    ctx_mock.vmagent_client.status.return_value = ''

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
                id='pod-id',
                auth=vmset_pb2.Auth(
                    owners=vmset_pb2.StaffUsersGroup(
                        logins=['volozh']
                    )
                )
            )
        )
    )

    ctx_mock.qdm_client.get_revision_info.return_value = qdmspec

    req = vmset_api_pb2.RestoreBackupRequest()
    req.vm_id = 'pod-id'
    req.resource_url = 'qdm:testing'

    pod = data_model.TPod()
    pod.meta.id = 'pod-id'
    pod.meta.pod_set_id = 'pod-set-id'
    ip_addr = pod.status.ip6_address_allocations.add()
    ip_addr.address = '::1'
    ip_addr.vlan_id = 'backbone'
    i = pod.spec.iss.instances.add()
    i.id.configuration.groupStateFingerprint = CONF_ID
    state = pod.status.agent.iss.currentStates.add()
    state.currentState = 'ACTIVE'
    state.workloadId.configuration.groupStateFingerprint = CONF_ID
    pod.labels.attributes.add(key='vmagent_version', value=yson.dumps('0.16'))
    pod.status.scheduling.state = 300
    owners_dict = dict()
    owners_dict['logins'] = ['volozh', 'other-user']
    owners_dict['groups'] = [1234]
    pod.annotations.attributes.add(key='owners', value=yson.dumps(owners_dict))
    pod.labels.attributes.add(key='version', value=yson.dumps('0.16'))
    pod_set = data_model.TPodSet()
    pod_set.meta.id = 'pod-set-id'
    pod_set.labels.attributes.add(key='version', value=yson.dumps('0.16'))

    # Case 1: user in spec
    obj = object_service_pb2.TRspGetObject()
    obj.result.values.extend([yputil.dumps_proto(pod)])
    yp_client.get_object.return_value = obj
    yp_client.start_transaction.return_value = ('sas', 1)

    ret_value_1 = vmset_api_pb2.RestoreBackupResponse()
    result = call(vmset_service.restore_backup, req, auth_subject=AuthSubject('volozh'))
    assert ret_value_1 == result

    # Case 2: user not in spec
    obj = object_service_pb2.TRspGetObject()
    obj.result.values.extend([yputil.dumps_proto(pod)])
    yp_client.get_object.return_value = obj
    yp_client.start_transaction.return_value = ('sas', 1)

    with pytest.raises(exceptions.ForbiddenError) as error:
        call(vmset_service.restore_backup, req, auth_subject=AuthSubject('definetly-not-volozh'))

    assert 'Attempt to restore VM backup by person-non-owner' == error.value.message

    # Case 3: no spec
    qdmspec = qdm_pb2.QDMBackupSpec(
        qdm_spec_version=1,
        rev_id='testing',
        filemap=[qdm_pb2.QDMBackupFileSpec(
            path='layer.img',
            size=1289479127,
            meta=qdm_pb2.QDMBackupFileMeta()
        )],
    )
    ctx_mock.qdm_client.get_revision_info.return_value = qdmspec
    obj = object_service_pb2.TRspGetObject()
    obj.result.values.extend([yputil.dumps_proto(pod)])
    yp_client.get_object.return_value = obj
    yp_client.start_transaction.return_value = ('sas', 1)

    result = call(vmset_service.restore_backup, req, auth_subject=AuthSubject('volozh'))
    assert ret_value_1 == result


def test_get_vm_usage(ctx_mock, call):
    hb_client_return_value = {
        "dutop": {
            "du_top_data": "{\"user1\": {\"total_size\": 100, \"top_dir_sizes\": {\".a\": 30, \"b\": 70}}}",
            "last_update": "2022-01-16T11:54:29Z"
        },
        "last": {
            "user_sessions": {
                "user1": "2022-04-13T18:23:17Z",
            },
            "user_sessions_seconds_length": {"user1": 129590},
            "wtmp_begin_dt": "2022-03-16T11:54:29Z",
            "last_update": "2022-02-17T11:54:29Z"
        },
        "who": {
            "user_actions": {
                "user1": ["action1_command"],
                "user2": ["action2"]
            },
            "user_sessions": {"user1": "2022-04-13T15:22:58.609875Z", "user2": "2022-03-25T16:16:28.118850Z"},
            "last_update": "2022-02-16T11:54:29Z",
            "initialised": "2022-02-16T11:54:29Z"
        }
    }
    ctx_mock.heartbeat_client.request_usage.return_value = hb_client_return_value
    pod_ctl_mock = mock.Mock()
    pod_ctl_mock.check_write_permission.return_value = True
    ctx_mock.pod_ctl = pod_ctl_mock
    req = vmset_api_pb2.GetVmUsageRequest()
    req.vm_id = 'pod-id'

    expected_res = vmset_api_pb2.GetVmUsageResponse()

    dt = parser.parse('2022-01-16T11:54:29Z').replace(tzinfo=None)
    expected_res.dutop.last_update_time.FromDatetime(dt)
    dutop_entry = expected_res.dutop.entries.add()
    dutop_entry.user_name = 'user1'
    dutop_entry.size = 100
    dir1 = dutop_entry.top_dirs.add()
    dir1.dir_name = 'b'
    dir1.size = 70
    dir2 = dutop_entry.top_dirs.add()
    dir2.dir_name = '.a'
    dir2.size = 30

    last_entry = expected_res.last.entries.add()
    dt = parser.parse('2022-02-17T11:54:29Z').replace(tzinfo=None)
    expected_res.last.last_update_time.FromDatetime(dt)
    dt = parser.parse('2022-03-16T11:54:29Z').replace(tzinfo=None)
    expected_res.last.from_time.FromDatetime(dt)
    last_entry.user_name = 'user1'
    last_entry.sessions_seconds_length = 129590
    dt = parser.parse('2022-04-13 18:23:17').replace(tzinfo=None)
    last_entry.session_end.FromDatetime(dt)

    dt = parser.parse('2022-02-16T11:54:29Z').replace(tzinfo=None)
    expected_res.who.last_update_time.FromDatetime(dt)
    dt = parser.parse('2022-02-16T11:54:29Z').replace(tzinfo=None)
    expected_res.who.from_time.FromDatetime(dt)
    huan = expected_res.who.entries.add()
    huan.user_name = 'user1'
    dt = parser.parse('2022-04-13 15:22:58.609875').replace(tzinfo=None)
    huan.session_end.FromDatetime(dt)
    huan.actions.extend(["action1_command"])
    who_two = expected_res.who.entries.add()
    who_two.user_name = 'user2'
    dt = parser.parse('2022-03-25 16:16:28.118850').replace(tzinfo=None)
    who_two.session_end.FromDatetime(dt)
    who_two.actions.extend(["action2"])

    result = call(vmset_service.get_vm_usage, req)
    assert result == expected_res

    with pytest.raises(exceptions.ForbiddenError):
        pod_ctl_mock.check_write_permission.return_value = False
        call(vmset_service.get_vm_usage, req)


def test_get_vm_usage_empty_response(ctx_mock, call):
    hb_client_return_value = {
        "dutop": {},
        "last": {},
        "who": {}
    }
    ctx_mock.heartbeat_client.request_usage.return_value = hb_client_return_value
    pod_ctl_mock = mock.Mock()
    pod_ctl_mock.check_write_permission.return_value = True
    ctx_mock.pod_ctl = pod_ctl_mock
    req = vmset_api_pb2.GetVmUsageRequest()
    req.vm_id = 'pod-id'

    expected_res = vmset_api_pb2.GetVmUsageResponse()
    result = call(vmset_service.get_vm_usage, req)
    assert result == expected_res
