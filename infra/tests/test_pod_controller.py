import inject
import mock
import pytest
import yp.common as yp_common
import yp.data_model as data_model
from infra.qyp.vmproxy.src.lib.yp import yputil
from yp.client import to_proto_enum
from yt import yson
from yp_proto.yp.client.api.proto import object_service_pb2

from infra.swatlib.auth import staff, abc
from infra.qyp.proto_lib import vmset_pb2
from infra.qyp.vmproxy.src import pod_controller, errors, security_policy
from infra.qyp.vmproxy.src.lib import abc_roles

POD_ID = 'real-pod-id'
ROBOT_LOGIN = 'robot-vmagent-rtc'
CONF_ID = 'conf-1234'
CREATION_TIME = 124567654321
LAST_MOD_TIME = 124568654321


def test_create_pod(config):
    vmagent_version = '1.12345'
    config.set_value('vmproxy.default_vmagent.version', vmagent_version)
    yp_client = mock.Mock()
    ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                       sec_policy=security_policy.SecurityPolicy())
    pod = data_model.TPod()
    pod_set = data_model.TPodSet()
    with mock.patch('uuid.uuid4', return_value='new_version'):
        ctl.create_pod_with_pod_set(pod, pod_set)
    yp_client.create_pod_with_pod_set.assert_called_with(pod_set, pod)


def test_delete_pod():
    yp_client = mock.Mock()

    def side_effect(id):
        if id != POD_ID:
            raise yp_common.YpNoSuchObjectError('')

    yp_client.remove_pod_set.side_effect = side_effect
    ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                       sec_policy=security_policy.SecurityPolicy())

    with pytest.raises(yp_common.YpNoSuchObjectError):
        ctl.delete_pod_with_pod_set('fake-pod-id')

    ctl.delete_pod_with_pod_set(POD_ID)
    yp_client.remove_pod_set.assert_called_with(POD_ID)


def test_get_active_pod():
    yp_client = mock.Mock()
    ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                       sec_policy=security_policy.SecurityPolicy())
    pod = data_model.TPod()
    pod.meta.id = POD_ID
    pod.meta.pod_set_id = POD_ID
    i = pod.spec.iss.instances.add()
    i.id.configuration.groupStateFingerprint = CONF_ID

    def get_object(object_id, object_type, ts):
        rsp = object_service_pb2.TRspGetObject()
        rsp.result.values.append(yputil.dumps_proto(pod))
        return rsp

    yp_client.get_object.side_effect = get_object
    # Case 1: not scheduled
    pod.status.Clear()
    pod.status.scheduling.state = to_proto_enum(data_model.ESchedulingState, 'none').number
    with pytest.raises(errors.WrongStateError):
        ctl.get_active_pod(POD_ID)

    # Case 2: error while scheduling
    pod.status.Clear()
    pod.status.scheduling.state = to_proto_enum(data_model.ESchedulingState, 'pending').number
    pod.status.scheduling.error.code = 1
    pod.status.scheduling.message = 'i cant do the thing you want'
    with pytest.raises(errors.WrongStateError):
        ctl.get_active_pod(POD_ID)

    # Case 3: iss-agent not active yet
    pod.status.Clear()
    pod.status.scheduling.state = to_proto_enum(data_model.ESchedulingState, 'assigned').number
    with pytest.raises(errors.WrongStateError):
        ctl.get_active_pod(POD_ID)

    # Case 4: still not active
    pod.status.Clear()
    pod.status.scheduling.state = to_proto_enum(data_model.ESchedulingState, 'assigned').number
    state = pod.status.agent.iss.currentStates.add()
    state.currentState = 'ENTITY_RESOURCES_NOT_READY_MY_GOSH'
    state.workloadId.configuration.groupStateFingerprint = CONF_ID
    with pytest.raises(errors.WrongStateError):
        ctl.get_active_pod(POD_ID)

    # Case 5: fail when allocating ip
    pod.status.Clear()
    pod.status.scheduling.state = to_proto_enum(data_model.ESchedulingState, 'assigned').number
    state = pod.status.agent.iss.currentStates.add()
    state.currentState = 'ACTIVE'
    state.workloadId.configuration.groupStateFingerprint = CONF_ID
    with pytest.raises(errors.WrongStateError):
        ctl.get_active_pod(POD_ID)

    # Case 6: OK
    pod.status.Clear()
    pod.status.scheduling.state = to_proto_enum(data_model.ESchedulingState, 'assigned').number
    state = pod.status.agent.iss.currentStates.add()
    state.currentState = 'ACTIVE'
    state.workloadId.configuration.groupStateFingerprint = CONF_ID
    alloc = pod.status.ip6_address_allocations.add()
    alloc.vlan_id = 'backbone'
    ctl.get_active_pod(POD_ID)

    # Case 7: this isn't the configuration you looking for
    pod.status.Clear()
    pod.status.scheduling.state = to_proto_enum(data_model.ESchedulingState, 'assigned').number
    state = pod.status.agent.iss.currentStates.add()
    state.currentState = 'ACTIVE'
    state.workloadId.configuration.groupStateFingerprint = 'other one'
    alloc = pod.status.ip6_address_allocations.add()
    alloc.vlan_id = 'backbone'
    with pytest.raises(errors.WrongStateError):
        ctl.get_active_pod(POD_ID)


def test_list_pods():
    vmagent_version = '1.12345'
    yp_client = mock.Mock()
    ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                       sec_policy=security_policy.SecurityPolicy())
    object_defaults = data_model.TSchema()
    label = object_defaults.labels.attributes.add()
    label.key = 'version'
    label.value = yson.dumps('1')

    pod1 = data_model.TPod()
    pod1.spec.iss.instances.add()
    pod1.labels.CopyFrom(object_defaults.labels)
    pod1.meta.id = 'vm1'
    pod1.meta.pod_set_id = 'vm1'
    pod1.meta.creation_time = CREATION_TIME
    pod1.status.master_spec_timestamp = LAST_MOD_TIME
    attr = pod1.annotations.attributes.add()
    attr.key = 'owners'
    attr.value = yson.dumps({
        'logins': ['some_user'],
        'groups': []
    })
    pod_set1 = data_model.TPodSet()
    pod_set1.labels.CopyFrom(object_defaults.labels)
    pod_set1.meta.id = 'vm1'
    pod_set1.spec.node_segment_id = 'default'
    pod_set1.spec.account_id = 'abc:service:1234'

    pod2 = data_model.TPod()
    pod2.spec.iss.instances.add()
    pod2.labels.CopyFrom(object_defaults.labels)
    pod2.meta.id = 'vm2'
    pod2.meta.pod_set_id = 'vm2'
    pod2.meta.creation_time = CREATION_TIME
    pod2.status.master_spec_timestamp = LAST_MOD_TIME
    attr = pod2.annotations.attributes.add()
    attr.key = 'owners'
    attr.value = yson.dumps({
        'logins': ['admin'],
        'groups': []
    })
    pod_set2 = data_model.TPodSet()
    pod_set2.labels.CopyFrom(object_defaults.labels)
    pod_set2.meta.id = 'vm2'
    pod_set2.spec.node_segment_id = 'default'
    pod_set2.spec.account_id = 'abc:service:4321'

    pod3 = data_model.TPod()
    pod3.spec.iss.instances.add()
    pod3.labels.CopyFrom(object_defaults.labels)
    pod3.labels.attributes.add(key='vmagent_version', value=yson.dumps(vmagent_version))
    pod3.meta.id = 'vm3'
    pod3.meta.pod_set_id = 'vm3'
    pod3.meta.creation_time = CREATION_TIME
    pod3.status.master_spec_timestamp = LAST_MOD_TIME
    attr = pod3.annotations.attributes.add()
    attr.key = 'owners'
    attr.value = yson.dumps({
        'logins': ['some_user', 'admin'],
        'groups': []
    })
    pod_set3 = data_model.TPodSet()
    pod_set3.labels.CopyFrom(object_defaults.labels)
    pod_set3.labels.attributes.add(key='vmagent_version', value=yson.dumps(vmagent_version))
    pod_set3.meta.id = 'vm3'
    pod_set3.spec.node_segment_id = 'dev'
    pod_set3.spec.account_id = 'tmp'

    vm1 = vmset_pb2.VM()
    vm1.meta.id = 'vm1'
    vm1.meta.auth.owners.logins.extend(['some_user'])
    vm1.meta.version.pod = '1'
    vm1.meta.version.pod_set = '1'
    vm1.meta.creation_time.FromMicroseconds(CREATION_TIME)
    vm1.meta.last_modification_time.FromSeconds(LAST_MOD_TIME / 2 ** 30)
    vm1.spec.account_id = 'abc:service:1234'
    vm1.spec.vmagent_version = 'N/A'
    vm1.spec.qemu.node_segment = 'default'
    vm1.spec.qemu.resource_requests.vcpu_limit = 0
    vm1.spec.qemu.io_guarantees_per_storage['hdd'] = 0
    vm1.spec.qemu.io_guarantees_per_storage['ssd'] = 0
    vm2 = vmset_pb2.VM()
    vm2.meta.id = 'vm2'
    vm2.meta.auth.owners.logins.extend(['admin'])
    vm2.meta.version.pod = '1'
    vm2.meta.version.pod_set = '1'
    vm2.meta.creation_time.FromMicroseconds(CREATION_TIME)
    vm2.meta.last_modification_time.FromSeconds(LAST_MOD_TIME / 2 ** 30)
    vm2.spec.account_id = 'abc:service:4321'
    vm2.spec.vmagent_version = 'N/A'
    vm2.spec.qemu.node_segment = 'default'
    vm2.spec.qemu.resource_requests.vcpu_limit = 0
    vm2.spec.qemu.io_guarantees_per_storage['hdd'] = 0
    vm2.spec.qemu.io_guarantees_per_storage['ssd'] = 0
    vm3 = vmset_pb2.VM()
    vm3.meta.id = 'vm3'
    vm3.meta.auth.owners.logins.extend(['some_user', 'admin'])
    vm3.meta.version.pod = '1'
    vm3.meta.version.pod_set = '1'
    vm3.meta.creation_time.FromMicroseconds(CREATION_TIME)
    vm3.meta.last_modification_time.FromSeconds(LAST_MOD_TIME / 2 ** 30)
    vm3.spec.account_id = 'tmp'
    vm3.spec.vmagent_version = vmagent_version
    vm3.spec.qemu.node_segment = 'dev'
    vm3.spec.qemu.resource_requests.vcpu_limit = 0
    vm3.spec.qemu.io_guarantees_per_storage['hdd'] = 0
    vm3.spec.qemu.io_guarantees_per_storage['ssd'] = 0

    ret_value = object_service_pb2.TRspGetObjects()
    ret_value.subresponses.extend([
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            str(pod1.meta.id),
            yputil.dumps_proto(pod1.meta),
            yputil.dumps_proto(pod1.spec),
            yputil.dumps_proto(pod1.status),
            yputil.dumps_proto(pod1.labels),
            pod1.annotations.attributes[0].value,
            '#'
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            str(pod2.meta.id),
            yputil.dumps_proto(pod2.meta),
            yputil.dumps_proto(pod2.spec),
            yputil.dumps_proto(pod2.status),
            yputil.dumps_proto(pod2.labels),
            pod2.annotations.attributes[0].value,
            '#'
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            str(pod3.meta.id),
            yputil.dumps_proto(pod3.meta),
            yputil.dumps_proto(pod3.spec),
            yputil.dumps_proto(pod3.status),
            yputil.dumps_proto(pod3.labels),
            pod3.annotations.attributes[0].value,
            '#'
        ])),
    ])
    yp_client.get_objects.return_value = ret_value

    pod_set_list = [
        object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(pod_set1.meta),
            yputil.dumps_proto(pod_set1.spec),
            yputil.dumps_proto(pod_set1.labels),
        ]),
        object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(pod_set2.meta),
            yputil.dumps_proto(pod_set2.spec),
            yputil.dumps_proto(pod_set2.labels),
        ]),
        object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(pod_set3.meta),
            yputil.dumps_proto(pod_set3.spec),
            yputil.dumps_proto(pod_set3.labels),
        ]),
    ]
    ret_value = object_service_pb2.TRspSelectObjects()
    ret_value.results.extend(pod_set_list)
    yp_client.list_pod_sets.return_value = ret_value
    yp_client.get_user_access_allowed_to.return_value = ['vm1', 'vm2', 'vm3']

    sort = vmset_pb2.YpVmFindSort()
    sort.field.append('meta.id')

    # Case 1: no filter
    result = ctl.list_pods(sort=sort)
    assert result == [vm1, vm2, vm3]

    # Case 2: skip, limit
    result = ctl.list_pods(sort=sort, skip=1, limit=1)
    assert result == [vm2]

    # Case 3: login filter
    ret_value = object_service_pb2.TRspSelectObjects()
    ret_value.results.extend(pod_set_list[1:])
    yp_client.list_pod_sets.return_value = ret_value
    yp_client.get_user_access_allowed_to.return_value = ['vm2', 'vm3']
    query = vmset_pb2.YpVmFindQuery()
    query.login = 'admin'
    result = ctl.list_pods(sort=sort, query=query)
    assert result == [vm2, vm3]


def test_list_all_pods():
    vmagent_version = '1.12345'
    yp_client = mock.Mock()
    ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                       sec_policy=security_policy.SecurityPolicy())
    object_defaults = data_model.TSchema()
    label = object_defaults.labels.attributes.add()
    label.key = 'version'
    label.value = yson.dumps('1')

    pod1 = data_model.TPod()
    pod1.spec.iss.instances.add()
    pod1.labels.CopyFrom(object_defaults.labels)
    pod1.meta.id = 'vm1'
    pod1.meta.pod_set_id = 'vm1'
    pod1.meta.creation_time = CREATION_TIME
    pod1.status.master_spec_timestamp = LAST_MOD_TIME
    ip_addr = pod1.status.ip6_address_allocations.add()
    ip_addr.address = '::1'
    ip_addr.vlan_id = 'backbone'
    ip_addr.labels.attributes.add(key='owner', value=yson.dumps('vm'))
    attr = pod1.annotations.attributes.add()
    attr.key = 'owners'
    attr.value = yson.dumps({
        'logins': ['some_user'],
        'groups': []
    })
    pod_set1 = data_model.TPodSet()
    pod_set1.labels.CopyFrom(object_defaults.labels)
    pod_set1.meta.id = 'vm1'
    pod_set1.spec.node_segment_id = 'default'
    pod_set1.spec.account_id = 'abc:service:1234'

    pod2 = data_model.TPod()
    pod2.spec.iss.instances.add()
    pod2.labels.CopyFrom(object_defaults.labels)
    pod2.meta.id = 'vm2'
    pod2.meta.pod_set_id = 'vm2'
    pod2.meta.creation_time = CREATION_TIME
    pod2.status.master_spec_timestamp = LAST_MOD_TIME
    attr = pod2.annotations.attributes.add()
    attr.key = 'owners'
    attr.value = yson.dumps({
        'logins': ['admin'],
        'groups': []
    })
    pod_set2 = data_model.TPodSet()
    pod_set2.labels.CopyFrom(object_defaults.labels)
    pod_set2.meta.id = 'vm2'
    pod_set2.spec.node_segment_id = 'default'
    pod_set2.spec.account_id = 'abc:service:4321'

    pod3 = data_model.TPod()
    pod3.spec.iss.instances.add()
    pod3.labels.CopyFrom(object_defaults.labels)
    pod3.labels.attributes.add(key='vmagent_version', value=yson.dumps(vmagent_version))
    pod3.meta.id = 'vm3'
    pod3.meta.pod_set_id = 'vm3'
    pod3.meta.creation_time = CREATION_TIME
    pod3.status.master_spec_timestamp = LAST_MOD_TIME
    attr = pod3.annotations.attributes.add()
    attr.key = 'owners'
    attr.value = yson.dumps({
        'logins': ['some_user', 'admin'],
        'groups': []
    })
    pod_set3 = data_model.TPodSet()
    pod_set3.labels.CopyFrom(object_defaults.labels)
    pod_set3.labels.attributes.add(key='vmagent_version', value=yson.dumps(vmagent_version))
    pod_set3.meta.id = 'vm3'
    pod_set3.spec.node_segment_id = 'dev'
    pod_set3.spec.account_id = 'tmp'

    vm1 = vmset_pb2.VM()
    vm1.meta.id = 'vm1'
    vm1.meta.auth.owners.logins.extend(['some_user'])
    vm1.meta.version.pod = '1'
    vm1.meta.version.pod_set = '1'
    vm1.meta.creation_time.FromMicroseconds(CREATION_TIME)
    vm1.meta.last_modification_time.FromSeconds(LAST_MOD_TIME / 2 ** 30)
    vm1.spec.account_id = 'abc:service:1234'
    vm1.spec.vmagent_version = 'N/A'
    vm1.spec.qemu.node_segment = 'default'
    vm1.spec.qemu.resource_requests.vcpu_limit = 0
    vm1.spec.qemu.io_guarantees_per_storage['hdd'] = 0
    vm1.spec.qemu.io_guarantees_per_storage['ssd'] = 0
    ip_addr = vm1.status.ip_allocations.add()
    ip_addr.address = '::1'
    ip_addr.vlan_id = 'backbone'
    ip_addr.owner = 'vm'
    vm2 = vmset_pb2.VM()
    vm2.meta.id = 'vm2'
    vm2.meta.auth.owners.logins.extend(['admin'])
    vm2.meta.version.pod = '1'
    vm2.meta.version.pod_set = '1'
    vm2.meta.creation_time.FromMicroseconds(CREATION_TIME)
    vm2.meta.last_modification_time.FromSeconds(LAST_MOD_TIME / 2 ** 30)
    vm2.spec.account_id = 'abc:service:4321'
    vm2.spec.vmagent_version = 'N/A'
    vm2.spec.qemu.node_segment = 'default'
    vm2.spec.qemu.resource_requests.vcpu_limit = 0
    vm2.spec.qemu.io_guarantees_per_storage['hdd'] = 0
    vm2.spec.qemu.io_guarantees_per_storage['ssd'] = 0
    vm3 = vmset_pb2.VM()
    vm3.meta.id = 'vm3'
    vm3.meta.auth.owners.logins.extend(['some_user', 'admin'])
    vm3.meta.version.pod = '1'
    vm3.meta.version.pod_set = '1'
    vm3.meta.creation_time.FromMicroseconds(CREATION_TIME)
    vm3.meta.last_modification_time.FromSeconds(LAST_MOD_TIME / 2 ** 30)
    vm3.spec.account_id = 'tmp'
    vm3.spec.vmagent_version = vmagent_version
    vm3.spec.qemu.node_segment = 'dev'
    vm3.spec.qemu.resource_requests.vcpu_limit = 0
    vm3.spec.qemu.io_guarantees_per_storage['hdd'] = 0
    vm3.spec.qemu.io_guarantees_per_storage['ssd'] = 0

    ret_value = object_service_pb2.TRspGetObjects()
    ret_value.subresponses.extend([
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            str(pod1.meta.id),
            yputil.dumps_proto(pod1.meta),
            yputil.dumps_proto(pod1.spec),
            yputil.dumps_proto(pod1.status),
            yputil.dumps_proto(pod1.labels),
            pod1.annotations.attributes[0].value,
            '#'
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            str(pod2.meta.id),
            yputil.dumps_proto(pod2.meta),
            yputil.dumps_proto(pod2.spec),
            yputil.dumps_proto(pod2.status),
            yputil.dumps_proto(pod2.labels),
            pod2.annotations.attributes[0].value,
            '#'
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            str(pod3.meta.id),
            yputil.dumps_proto(pod3.meta),
            yputil.dumps_proto(pod3.spec),
            yputil.dumps_proto(pod3.status),
            yputil.dumps_proto(pod3.labels),
            pod3.annotations.attributes[0].value,
            '#'
        ])),
    ])
    yp_client.get_objects.return_value = ret_value

    ret_value = object_service_pb2.TRspSelectObjects()
    ret_value.results.extend([
        object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(pod_set1.meta),
            yputil.dumps_proto(pod_set1.spec),
            yputil.dumps_proto(pod_set1.labels),
        ]),
        object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(pod_set2.meta),
            yputil.dumps_proto(pod_set2.spec),
            yputil.dumps_proto(pod_set2.labels),
        ]),
        object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(pod_set3.meta),
            yputil.dumps_proto(pod_set3.spec),
            yputil.dumps_proto(pod_set3.labels),
        ]),
    ])
    yp_client.list_pod_sets.return_value = ret_value
    yp_client.get_user_access_allowed_to.return_value = []

    result = ctl.list_all_pods()
    assert result == [vm1, vm2, vm3]


def test_update_pod(config):
    def configure(binder):
        binder.bind(staff.IStaffClient, mock.Mock())

    inject.clear_and_configure(configure)

    pod = data_model.TPod()
    pod.meta.id = POD_ID
    pod.meta.pod_set_id = POD_ID
    pod.labels.attributes.add(key='version', value=yson.dumps('version'))

    def get_object(object_id, object_type, ts):
        rsp = object_service_pb2.TRspGetObject()
        rsp.result.values.append(yputil.dumps_proto(pod))
        return rsp

    yp_client = mock.Mock()
    yp_client.get_object.side_effect = get_object
    transaction_id = '12345'
    timestamp = 1
    yp_client.start_transaction.return_value = transaction_id, timestamp
    ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                       sec_policy=security_policy.SecurityPolicy())

    set_updates = {
        '/spec/important_spec_part': 'here be dragons'
    }
    updates = set_updates.copy()
    updates['/labels/version'] = yson.dumps('new_version')
    # Case 1: ok
    with mock.patch('uuid.uuid4', return_value='new_version'):
        ctl.update_pod(POD_ID, 'version', set_updates, transaction_id, timestamp)
    yp_client.update_object.assert_called_with(POD_ID, data_model.OT_POD, updates, transaction_id)

    # Case 2: Concurrent modification
    del pod.labels.attributes[:]
    pod.labels.attributes.add(key='version', value=yson.dumps('next_version'))
    with pytest.raises(errors.ConcurrentModificationError):
        ctl.update_pod(POD_ID, 'version', set_updates, transaction_id, timestamp)


def test_check_use_account_permission():
    abc_roles.service_roles_cache.clear()

    def check_obj_permission(object_id, object_type, subject_id, permission):
        return object_id != 'fake_acc'

    def list_members_iter(spec, **kwargs):
        yield {'results': [
            {'service': {'id': '1'}, 'person': {'login': 'qyp_user'}},
            {'service': {'id': '2'}, 'person': {'login': 'volozh'}},
        ]}
        return

    abc_client = mock.Mock()
    abc_client.list_members_iter.side_effect = list_members_iter

    def configure(binder):
        binder.bind(abc.IAbcClient, abc_client)

    inject.clear_and_configure(configure)

    yp_client = mock.Mock()
    yp_client.check_object_permissions.side_effect = check_obj_permission
    ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                       sec_policy=security_policy.SecurityPolicy())
    # Case 1: user has no access to account
    assert not ctl.check_use_account_permission('fake_acc', 'volozh', False)
    # Case 2: account has no qyp users
    assert ctl.check_use_account_permission('abc:service:0', 'volozh', True)
    # Case 3: account has qyp users and subject is not included
    assert not ctl.check_use_account_permission('abc:service:1', 'volozh', True)
    # Case 4: account has qyp users and subject is one of them
    assert ctl.check_use_account_permission('abc:service:2', 'volozh', True)
    # Case 5: robot has access to any account
    assert ctl.check_use_account_permission('literally_any_account', ROBOT_LOGIN, True)


def test_use_macro_permission(config):
    user = 'user'
    config.set_value('vmproxy.network_whitelist', ['_WHITELISTNETS_'])

    def check_obj_permission(object_id, object_type, subject_id, permission):
        return object_id == '_GOODNETS_'

    yp_client = mock.Mock()
    yp_client.check_object_permissions.side_effect = check_obj_permission
    ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                       sec_policy=security_policy.SecurityPolicy())

    # Case 1: user has no access to macro
    assert not ctl.check_use_macro_permission('_OTHERNETS_', user)
    # Case 2: user has access to macro
    assert ctl.check_use_macro_permission('_GOODNETS_', user)
    # Case 3: whitelist macro
    assert ctl.check_use_macro_permission('_WHITELISTNETS_', user)


def test_check_read_permission():
    def check_read_permission(pod_id, subject_id):
        return subject_id != 'fake_acc'
    yp_client = mock.Mock()
    yp_client.check_read_permission.side_effect = check_read_permission
    ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                       sec_policy=security_policy.SecurityPolicy())

    # Case 1: user has no access to subject
    assert not ctl.check_read_permission('subject', 'fake_acc')

    # Case 2: user has access to subject
    assert ctl.check_read_permission('subject', 'real_acc')

    # Case 3: disabled check permission
    ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                       sec_policy=security_policy.SecurityPolicy(is_enabled=False))
    assert ctl.check_read_permission('subject', 'fake_acc')
    assert ctl.check_read_permission('subject', 'real_acc')


def test_check_write_permission():
    def check_read_permission(pod_id, subject_id):
        return subject_id != 'fake_acc'
    yp_client = mock.Mock()
    yp_client.check_write_permission.side_effect = check_read_permission
    ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                       sec_policy=security_policy.SecurityPolicy())

    # Case 1: user has no access to subject
    assert not ctl.check_write_permission('subject', 'fake_acc')

    # Case 2: user has access to subject
    assert ctl.check_write_permission('subject', 'real_acc')

    # Case 3: disabled check permission
    ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                       sec_policy=security_policy.SecurityPolicy(is_enabled=False))
    assert ctl.check_write_permission('subject', 'fake_acc')
    assert ctl.check_write_permission('subject', 'real_acc')


def test_get_pod_stats():
    # Case 1: get stats without filters
    yp_client = mock.Mock()
    disk_request = [{
        'id': 'pod-id-version',
        'storage_class': 'hdd',
        'labels': {
            'mount_path': yputil.MAIN_VOLUME_POD_MOUNT_PATH
        },
        'quota_policy': {
            'capacity': 300 * 1024 ** 3
        }
    }]
    ret_value = object_service_pb2.TRspGetObjects()
    ret_value.subresponses.extend([
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            str(8),
            str(50),
            yson.dumps(disk_request),
            yson.dumps('0.20')
        ])),
    ])
    yp_client.get_objects.return_value = ret_value

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

    ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                       sec_policy=security_policy.SecurityPolicy())
    size, usage, vmagent_version = ctl.get_pod_stats()
    expected_size = 1
    expected_usage = vmset_pb2.ResourceInfo()
    expected_usage.cpu = 8
    expected_usage.mem = 50
    expected_usage.disk_per_storage['hdd'] = 300 * 1024 ** 3
    assert size == expected_size
    assert usage == expected_usage

    # Case 2: get stats with filter
    ret_value = object_service_pb2.TRspSelectObjects()
    ret_value.results.extend([
        object_service_pb2.TAttributeList(values=[
            str('pod-id')
        ]),
    ])
    yp_client.list_pod_sets.return_value = ret_value
    size, usage, vmagent_version = ctl.get_pod_stats(segment='dev')
    assert size == expected_size
    assert usage == expected_usage

    # Case 3: get stats with filter, empty
    del ret_value.results[0]
    yp_client.list_pod_sets.return_value = ret_value
    size, usage, vmagent_version = ctl.get_pod_stats(segment='dev')
    expected_size = 0
    expected_usage = vmset_pb2.ResourceInfo()
    assert size == expected_size
    assert usage == expected_usage


def test_get_pod_container_ip():
    pod = data_model.TPod()
    pod.meta.id = 'pod-id'
    # Case 1: ip not exist
    with pytest.raises(ValueError):
        pod_controller.PodController.get_pod_container_ip(pod)
    # Case 2: ip exist
    ip_addr = pod.status.ip6_address_allocations.add()
    ip_addr.address = '::1'
    ip_addr.vlan_id = 'backbone'
    assert pod_controller.PodController.get_pod_container_ip(pod) == '::1'


def test_get_pod_owners():
    pod = data_model.TPod()
    pod.meta.id = 'pod-id'
    # Case 1: owners not exist
    with pytest.raises(ValueError):
        pod_controller.PodController.get_pod_owners(pod)
    # Case 2: owners exist
    owners = {
        'logins': ['some_user'],
        'groups': []
    }
    attr = pod.annotations.attributes.add()
    attr.key = 'owners'
    attr.value = yson.dumps(owners)
    assert pod_controller.PodController.get_pod_owners(pod) == owners


def test_get_backup_list():
    pod = data_model.TPod()
    pod.meta.id = 'pod-id'
    # Case 1: backup not exist
    assert not pod_controller.PodController.get_backup_list(pod)
    # Case 2: backup exist
    attr = pod.annotations.attributes.add()
    attr.key = 'backup_list'
    list_backup = [
        'first_backup',
        'second_backup'
    ]
    attr.value = yson.dumps(list_backup)
    assert pod_controller.PodController.get_backup_list(pod) == list_backup


def test_update_backup_list():
    yp_client = mock.Mock()
    pod_ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                           sec_policy=security_policy.SecurityPolicy())
    pod = data_model.TPod()
    pod.meta.id = 'pod-id'
    pod.meta.pod_set_id = 'pod-id'
    pod.labels.attributes.add(key='version', value='1234')
    backup_list = ['backup1', 'backup2']
    ret_value = object_service_pb2.TRspGetObject()
    ret_value.result.values.append(yputil.dumps_proto(pod))
    yp_client.get_object.return_value = ret_value

    with mock.patch('uuid.uuid4', return_value='uuid_version'):
        pod_ctl.update_backup_list(pod, backup_list)
        set_updates = {
            '/annotations/backup_list': yson.dumps(backup_list),
            '/labels/version': yson.dumps('uuid_version')
        }
        yp_client.update_object.assert_called_with(pod.meta.id, data_model.OT_POD, set_updates, None)


def test_transaction():
    yp_client = mock.Mock()
    pod_ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                           sec_policy=security_policy.SecurityPolicy())

    yp_client.start_transaction.return_value = 1111, 2222
    assert pod_ctl.start_transaction() == (1111, 2222)

    val = '1234'
    pod_ctl.commit_transaction(val)
    yp_client.commit_transaction.assert_called_with(val)


def test_check_use_macro_permission():
    yp_client = mock.Mock()
    pod_ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                           sec_policy=security_policy.SecurityPolicy())
    macro_name, subject_id = 'macro', 'subject'
    pod_ctl.check_use_macro_permission(macro_name, subject_id)
    yp_client.check_object_permissions.assert_called_with(object_id=macro_name,
                                                          object_type=data_model.OT_NETWORK_PROJECT,
                                                          subject_id=subject_id, permission=[data_model.ACA_USE])


def test_get_keys_by_logins():
    yp_client = mock.Mock()
    pod_ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                           sec_policy=security_policy.SecurityPolicy())
    logins = ['login1', 'login2']
    ret_value = object_service_pb2.TRspSelectObjects()
    ret_value.results.extend([
        object_service_pb2.TAttributeList(values=[
            yson.dumps({'key': 'key1', 'fingerprint': 'fingerprint1'})
        ]),
        object_service_pb2.TAttributeList(values=[
            yson.dumps({'key': 'key2', 'fingerprint': 'fingerprint2'})
        ])
    ])
    yp_client.select_objects.return_value = ret_value
    result = pod_ctl.get_keys_by_logins(logins)
    expected = [
        {'key': 'key1', 'fingerprint': 'fingerprint1'},
        {'key': 'key2', 'fingerprint': 'fingerprint2'}
    ]
    assert result == expected


def test_update_pod_set():
    yp_client = mock.Mock()
    pod_ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                           sec_policy=security_policy.SecurityPolicy())

    # Case 1: empty updates
    pod_set_id = 'pod-id'
    version = 1234
    set_updates = {}
    pod_ctl.update_pod_set(pod_set_id, version, set_updates)
    # Case 2: ok
    set_updates = {
        'first': 'update1',
        'second': 'update2'
    }
    pod = data_model.TPod()
    pod.meta.id = 'pod-id'
    # pod.meta.pod_set_id = 'pod-id'
    pod.labels.attributes.add(key='version', value='1234')
    ret_value = object_service_pb2.TRspGetObject()
    ret_value.result.values.append(yputil.dumps_proto(pod))
    yp_client.get_object.return_value = ret_value
    with mock.patch('uuid.uuid4', return_value='uuid_version'):
        pod_ctl.update_pod_set(pod_set_id, version, set_updates)
        set_updates['/labels/version'] = '"uuid_version"'
        yp_client.update_object.assert_called_with(pod.meta.id, data_model.OT_POD_SET, set_updates, None)


def test_make_yp_query():
    query = vmset_pb2.YpVmFindQuery()
    query.name = 'name'
    query.account.extend(['account1', 'account2'])
    query.segment.extend(['segment1', 'segment2'])

    result = pod_controller.make_yp_query(query)
    expected = '[/labels/deploy_engine] = "QYP" AND is_substr("name", [/meta/id]) AND [/spec/account_id] in ' \
               '("account1","account2") AND [/spec/node_segment_id] in ("segment1","segment2")'
    assert result == expected


def test_multikeysort():
    class Fake:
        def __init__(self, id):
            self.id = id
            self.item1 = 'item1'
            self.item2 = 'item2'
            self.item3 = 'item3'
    fake1, fake2, fake3 = Fake(1), Fake(2), Fake(3)
    items = [fake3, fake1, fake2]
    fields = ['+id', '-id', '+id']

    result = pod_controller.multikeysort(items, fields)
    expected = [fake1, fake2, fake3]
    assert result == expected


def test_pod_controller_factory(config):
    clusters = [{
        'cluster': 'SAS',
        'url': 'sas.yp.yandex.net',
        'enable_https': True}]
    config.set_value('yp.robot_token', 'robot_token')
    config.set_value('yp.clusters', clusters)
    sec_policy = security_policy.SecurityPolicy()
    pod_factory = pod_controller.PodControllerFactory('SAS', sec_policy)
    pod_ctl = pod_factory.get_object()
    yp_client = mock.Mock()
    expected = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                            sec_policy=sec_policy)
    assert pod_ctl.yp_cluster == expected.yp_cluster


def test_update_pod_with_move(config):
    yp_client = mock.Mock()
    t_id = 't-id'
    ts = 12345
    yp_client.start_transaction.return_value = t_id, ts
    old_pod = data_model.TPod()
    pod_labels_dict = {
        "version": "1",
        "vmagent_version": config.get_value('vmproxy.default_vmagent.version')
    }
    yputil.cast_dict_to_attr_dict(pod_labels_dict, old_pod.labels)

    def get_object(object_id, object_type, ts):
        ret_value = object_service_pb2.TRspGetObject()
        ret_value.result.values.append(yputil.dumps_proto(old_pod))
        return ret_value

    yp_client.get_object.side_effect = get_object
    pod_ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                           sec_policy=security_policy.SecurityPolicy())

    pod_id = 'pod-id'
    pod_version = '1'
    pod_set_version = '1'
    pod = data_model.TPod()
    pod.CopyFrom(old_pod)
    pod_set_updates = {
        '/labels/vmagent_version': yson.dumps('new'),
    }
    pod_set_actual_updates = {
        '/labels/vmagent_version': yson.dumps('new'),
        '/labels/version': yson.dumps('new_version'),
    }
    # Case 1: pod version does not match
    yp_client.reset_mock()
    with pytest.raises(errors.ConcurrentModificationError):
        pod_ctl.update_pod_with_move(pod_id, '0', pod_set_version, pod, pod_set_updates)
    yp_client.commit_transaction.assert_not_called()
    # Case 2: pod set version does not match
    yp_client.reset_mock()
    with pytest.raises(errors.ConcurrentModificationError):
        pod_ctl.update_pod_with_move(pod_id, pod_version, '0', pod, pod_set_updates)
    yp_client.commit_transaction.assert_not_called()
    # Case 3: successful update
    yp_client.reset_mock()
    with mock.patch('uuid.uuid4', return_value='new_version'):
        pod_ctl.update_pod_with_move(pod_id, pod_version, pod_set_version, pod, pod_set_updates)
    yp_client.remove_object.assert_called_once_with(pod_id, data_model.OT_POD, t_id)
    yp_client.create_pod.assert_called_once_with(pod, t_id)
    yp_client.update_object.assert_called_once_with(pod_id, data_model.OT_POD_SET, pod_set_actual_updates, t_id)
    yp_client.commit_transaction.assert_called_with(t_id)
