import inject
import pytest

import mock
import yp.data_model as data_model
from infra.swatlib.auth import abc
from yt import yson
from yp_proto.yp.client.api.proto import object_service_pb2

from infra.qyp.proto_lib import vmset_pb2
from infra.qyp.vmproxy.src import pod_controller, security_policy, errors
from infra.qyp.vmproxy.src.action import list_user_accounts as list_accounts_action
from infra.qyp.vmproxy.src.lib import abc_roles
from infra.qyp.vmproxy.src.lib.yp import yputil

PERSONAL_ACCOUNT_ID = 'abc:service:4172'


def test_list_user_accounts(config):
    abc_roles.service_roles_cache.clear()
    search_users = ['imperator', 'volozh', 'some_guy']

    def list_members_iter(spec, **kwargs):
        yield {'results': [
            {'service': {'id': '1'}, 'person': {'login': 'qyp_user'}},
            {'service': {'id': '1'}, 'person': {'login': 'volozh'}},
            {'service': {'id': '2'}, 'person': {'login': 'volozh'}},
        ]}
        return

    abc_client = mock.Mock()
    abc_client.list_members_iter.side_effect = list_members_iter

    def configure(binder):
        binder.bind(abc.IAbcClient, abc_client)

    inject.clear_and_configure(configure)
    ctx = mock.Mock()
    vmagent_version = '1.12345'
    yp_client = mock.Mock()
    ctx.pod_ctl = pod_controller.PodController(yp_cluster='SAS', yp_client=yp_client,
                                               sec_policy=security_policy.SecurityPolicy())
    ctx.personal_quotas_dict = {
        'abc:service:3': {
            'segment': 'dev',
            'cpu': 2000,
            'mem': 8589934592,
            'disk':
                [
                    {
                        'storage': 'hdd',
                        'capacity': 21474836480,
                        'bandwidth_guarantee': 314572800,
                    }
                ],
            'internet_address': 0
        }
    }
    object_defaults = data_model.TSchema()
    label = object_defaults.labels.attributes.add()
    label.key = 'version'
    label.value = yson.dumps('1')

    pod1 = data_model.TPod()
    pod1.labels.CopyFrom(object_defaults.labels)
    pod1.meta.id = 'vm1'
    pod1.meta.pod_set_id = 'vm1'
    attr = pod1.annotations.attributes.add()
    attr.key = 'owners'
    attr.value = yson.dumps({
        'logins': ['volozh', 'imperator'],
        'groups': []
    })
    pod_set1 = data_model.TPodSet()
    pod_set1.labels.CopyFrom(object_defaults.labels)
    pod_set1.meta.id = 'vm1'
    pod_set1.spec.node_segment_id = 'default'
    pod_set1.spec.account_id = 'abc:service:1'

    pod2 = data_model.TPod()
    pod2.labels.CopyFrom(object_defaults.labels)
    pod2.meta.id = 'vm2'
    pod2.meta.pod_set_id = 'vm2'
    attr = pod2.annotations.attributes.add()
    attr.key = 'owners'
    attr.value = yson.dumps({
        'logins': ['volozh', 'imperator'],
        'groups': []
    })
    pod_set2 = data_model.TPodSet()
    pod_set2.labels.CopyFrom(object_defaults.labels)
    pod_set2.meta.id = 'vm2'
    pod_set2.spec.node_segment_id = 'default'
    pod_set2.spec.account_id = 'abc:service:2'

    pod3 = data_model.TPod()
    pod3.labels.CopyFrom(object_defaults.labels)
    pod3.labels.attributes.add(key='vmagent_version', value=yson.dumps(vmagent_version))
    pod3.meta.id = 'vm3'
    pod3.meta.pod_set_id = 'vm3'
    v = pod3.spec.disk_volume_requests.add()
    v.id = 'version'
    v.storage_class = 'hdd'
    v.quota_policy.capacity = 1024
    v.quota_policy.bandwidth_guarantee = 10 * 1024 ** 2
    v.quota_policy.bandwidth_limit = 10 * 1024 ** 2
    v.labels.attributes.add(key='mount_path', value=yson.dumps(yputil.MAIN_VOLUME_POD_MOUNT_PATH))
    attr = pod3.annotations.attributes.add()
    attr.key = 'owners'
    attr.value = yson.dumps({
        'logins': ['volozh', 'imperator'],
        'groups': [],
        'author': 'imperator',
    })
    pod_set3 = data_model.TPodSet()
    pod_set3.labels.CopyFrom(object_defaults.labels)
    pod_set3.labels.attributes.add(key='vmagent_version', value=yson.dumps(vmagent_version))
    pod_set3.meta.id = 'vm3'
    pod_set3.spec.node_segment_id = 'dev'
    pod_set3.spec.account_id = 'abc:service:3'

    ret_value = object_service_pb2.TRspSelectObjects()
    ret_value.results.extend([
        object_service_pb2.TAttributeList(values=[
            str(pod1.meta.id),
            yputil.dumps_proto(pod1.meta),
            yputil.dumps_proto(pod1.spec),
            yputil.dumps_proto(pod1.status),
            yputil.dumps_proto(pod1.labels),
            pod1.annotations.attributes[0].value,
            '#'
        ]),
        object_service_pb2.TAttributeList(values=[
            str(pod2.meta.id),
            yputil.dumps_proto(pod2.meta),
            yputil.dumps_proto(pod2.spec),
            yputil.dumps_proto(pod2.status),
            yputil.dumps_proto(pod2.labels),
            pod2.annotations.attributes[0].value,
            '#',
        ]),
        object_service_pb2.TAttributeList(values=[
            str(pod3.meta.id),
            yputil.dumps_proto(pod3.meta),
            yputil.dumps_proto(pod3.spec),
            yputil.dumps_proto(pod3.status),
            yputil.dumps_proto(pod3.labels),
            pod3.annotations.attributes[0].value,
            '#'
        ]),
    ])
    yp_client.list_pods.return_value = ret_value

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

    group = data_model.TGroup()
    group.meta.id = 'abc:service:1'
    group_members = ['user', 'test', 'imperator', 'robot', 'volozh', 'qyp_user']
    qyp_users = ['volozh', 'qyp_user']
    group.spec.members.extend(group_members)
    expected_members_count = len(set(group.spec.members)
                                 .intersection(set(qyp_users))
                                 .intersection(set(search_users)))

    def get_groups(ids, selectors=None, timestamp=None):
        if ids == [PERSONAL_ACCOUNT_ID]:
            users = search_users
        else:
            users = group_members
        ret_value = object_service_pb2.TRspGetObjects()
        ret_value.subresponses.extend([
            object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
                yson.dumps(group.meta.id),
                '[{}]'.format(' ;'.join(users)),
            ])),
        ])
        return ret_value

    yp_client.get_groups.side_effect = get_groups

    yp_superusers_group = data_model.TGroup()
    yp_superusers_group.spec.members.extend(['slonnn'])
    get_group_resp = object_service_pb2.TRspGetObject()
    get_group_resp.result.values.append(yputil.dumps_proto(yp_superusers_group))
    yp_client.get_group.return_value = get_group_resp

    acc1 = data_model.TAccount()
    acc1.meta.id = 'abc:service:1'
    acc1.spec.resource_limits.per_segment['dev'].cpu.capacity = 1000
    acc1.spec.resource_limits.per_segment['default'].cpu.capacity = 1000
    acc2 = data_model.TAccount()
    acc2.meta.id = 'abc:service:2'
    acc2.spec.resource_limits.per_segment['dev'].cpu.capacity = 2000
    acc3 = data_model.TAccount()
    acc3.meta.id = 'abc:service:3'
    acc3.spec.resource_limits.per_segment['dev'].cpu.capacity = 3000
    acc3.status.resource_usage.per_segment['dev'].cpu.capacity = 3000

    # Case 1: account set, segment set
    ret_value = object_service_pb2.TRspGetObjects()
    ret_value.subresponses.extend([
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(acc1),
        ])),
    ])
    yp_client.get_accounts.return_value = ret_value

    expected = vmset_pb2.Account()
    expected.type = vmset_pb2.Account.SERVICE
    expected.id = 'abc:service:1'
    expected.limits.per_segment['dev'].cpu = 1000
    expected.members_count = expected_members_count
    result = list_accounts_action.run(ctx, 'volozh', 'abc:service:1', 'dev', True)
    yp_client.get_accounts.assert_called_with(['abc:service:1'], selectors=None)
    assert result == [expected]
    # Case 2: account set, segment not set
    ret_value = object_service_pb2.TRspGetObjects()
    ret_value.subresponses.extend([
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(acc1),
        ])),
    ])
    yp_client.get_accounts.return_value = ret_value

    expected = vmset_pb2.Account()
    expected.type = vmset_pb2.Account.SERVICE
    expected.id = 'abc:service:1'
    expected.limits.per_segment['dev'].cpu = 1000
    expected.limits.per_segment['default'].cpu = 1000
    expected.members_count = expected_members_count
    result = list_accounts_action.run(ctx, 'volozh', 'abc:service:1', '', True)
    assert result == [expected]
    # Case 3: account not set, segment not set
    ret_value = object_service_pb2.TRspGetObjects()
    ret_value.subresponses.extend([
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(acc1),
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(acc2),
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(acc3),
        ])),
    ])
    yp_client.get_accounts.return_value = ret_value
    yp_client.get_user_access_allowed_to.return_value = ['abc:service:1', 'abc:service:2', 'abc:service:3']

    res_acc1 = vmset_pb2.Account()
    res_acc1.type = vmset_pb2.Account.SERVICE
    res_acc1.id = 'abc:service:1'
    res_acc1.limits.per_segment['dev'].cpu = 1000
    res_acc1.limits.per_segment['default'].cpu = 1000
    res_acc1.members_count = expected_members_count
    res_acc2 = vmset_pb2.Account()
    res_acc2.type = vmset_pb2.Account.SERVICE
    res_acc2.id = 'abc:service:2'
    res_acc2.limits.per_segment['dev'].cpu = 2000
    res_acc3 = vmset_pb2.Account()
    res_acc3.type = vmset_pb2.Account.PERSONAL
    res_acc3.id = 'abc:service:3'
    res_acc3.limits.per_segment['dev'].cpu = 3000
    res_acc3.usage.per_segment['dev'].cpu = 3000
    res_acc3.personal.limits.per_segment['dev'].cpu = 2000
    res_acc3.personal.limits.per_segment['dev'].mem = 8589934592
    res_acc3.personal.limits.per_segment['dev'].disk_per_storage['hdd'] = 21474836480
    res_acc3.personal.limits.per_segment['dev'].internet_address = 0
    res_acc3.personal.limits.per_segment['dev'].io_guarantees_per_storage['hdd'] = 314572800

    ret_value = object_service_pb2.TRspGetObjects()
    yp_client.get_objects.return_value = ret_value

    result = list_accounts_action.run(ctx, 'volozh', '', '', True)
    yp_client.get_accounts.assert_called_with(['abc:service:1', 'abc:service:2', 'abc:service:3'], selectors=None)
    assert result == [res_acc1, res_acc2, res_acc3]
    # # # Case 4: personal quota usage
    # res_acc3.personal.usage.per_segment['dev'].disk_per_storage['hdd'] = 1024
    # actual_result = list_accounts_action.run(ctx, 'imperator', '', '', True)
    # expected_result = [res_acc1, res_acc2, res_acc3]
    # assert actual_result == expected_result
    # Case 5: qyp_user role
    list_accounts_action.run(ctx, 'qyp_user', '', '', True)
    yp_client.get_accounts.assert_called_with(['abc:service:1', 'abc:service:3'], selectors=None)
    # Case 6: segment exist with ip and hdd
    acc3.spec.resource_limits.per_segment['dev'].internet_address.capacity = 1
    acc3.spec.resource_limits.per_segment['dev'].disk_per_storage_class['hdd'].capacity = 300 * 1024 ** 3
    acc3.spec.resource_limits.per_segment['dev'].disk_per_storage_class['hdd'].bandwidth = 300 * 1024 ** 2
    del res_acc1.limits.per_segment['default']
    res_acc3.limits.per_segment['dev'].internet_address = 1
    res_acc3.limits.per_segment['dev'].disk_per_storage['hdd'] = 300 * 1024 ** 3
    res_acc3.limits.per_segment['dev'].io_guarantees_per_storage['hdd'] = 300 * 1024 ** 2
    res_acc3.personal.usage.per_segment['dev'].internet_address = 1
    res_acc3.personal.limits.per_segment['dev'].disk_per_storage['hdd'] = 20 * 1024 ** 3
    res_acc3.personal.usage.per_segment['dev'].disk_per_storage['hdd'] = 1024
    res_acc3.personal.usage.per_segment['dev'].io_guarantees_per_storage['hdd'] = 10 * 1024 ** 2
    res_acc3.personal.usage.per_segment['dev'].io_guarantees_per_storage['ssd'] = 0
    ret_value = object_service_pb2.TRspGetObjects()
    ret_value.subresponses.extend([
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(acc1),
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(acc2),
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yputil.dumps_proto(acc3),
        ])),
    ])
    yp_client.get_accounts.return_value = ret_value
    ip6 = data_model.TPodSpec().TIP6AddressRequest()
    ip6.enable_internet = True
    ip6.network_id = 'net_id'
    ip6.vlan_id = 'vlan_id'
    pod3.spec.ip6_address_requests.extend([ip6])
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

    result = list_accounts_action.run(ctx, 'imperator', '', 'dev', True)
    assert result == [res_acc1, res_acc2, res_acc3]

    # Case 7: raise Auth
    ctx.pod_ctl.yp_client.check_object_permissions.return_value = False
    with pytest.raises(errors.AuthorizationError):
        list_accounts_action.run(ctx, '', '123', '', True)

    # Case 8: empty account_id
    ctx.pod_ctl.yp_client.get_user_access_allowed_to.return_value = []
    result = list_accounts_action.run(ctx, '', '', '', True)
    assert result == []
