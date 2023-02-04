import mock
import yt.yson as yson
from collections import defaultdict
from infra.qyp.proto_lib import accounts_pb2
from yp_proto.yp.client.api.proto import object_service_pb2

import yp.data_model as data_model
from infra.qyp.account_manager.src import constant
from infra.qyp.account_manager.src import helpers
from infra.qyp.account_manager.src.model.actions.get_account_summary import get_account_summary
from sepelib.core import config as sconfig


def make_spec_pod(cpu, mem, ssd, ip=False):
    return {
        'resource_requests': {
            'vcpu_guarantee': cpu * 1000,
            'memory_guarantee': mem * 1024 ** 3
        },
        'disk_volume_requests': [
            {},
            {
                'quota_policy': {
                    'capacity': ssd * 1024 ** 3
                },
                'storage_class': 'ssd'
            }
        ],
        'ip6_address_requests': [
            {
                'enable_internet': ip
            }
        ]
    }


def make_yield(item):
    yield item


def test_quota_summary(config):
    FAKE_ACCOUND_ID = 1234
    FAKE_QYP_USERS = ['login1', 'login2']
    FAKE_SP_USERS = ['login3']
    FAKE_ALL_USERS = FAKE_QYP_USERS + FAKE_SP_USERS
    FAKE_GROUP_USERS = [{
        'results': [
            {
                'service': {
                    'id': 11111
                },
                'person': {
                    'login': 'login1'
                }
            },
            {
                'service': {
                    'id': 11111
                },
                'person': {
                    'login': 'login2'
                }
            },
            {
                'service': {
                    'id': 4172
                },
                'person': {
                    'login': 'login1'
                }
            },
            {
                'service': {
                    'id': 4172
                },
                'person': {
                    'login': 'login2'
                }
            },
            {
                'service': {
                    'id': 11111
                },
                'person': {
                    'login': 'login3'
                }
            },
            {
                'service': {
                    'id': 4172
                },
                'person': {
                    'login': 'login3'
                }
            }
        ]
    }]
    FAKE_VM_ACCESS_ALLOW = ['vm_id1', 'vm_id2', 'vm_id3']
    FAKE_LIST_ACCOUNTS = ['abc:service:11111', constant.QYP_PERSONAL_ID]
    PERS_QUOTA = {
        constant.QYP_PERSONAL_ID: {
            'segment': 'dev',
            'cpu': 8000,
            'mem': 53687091200,
            'disk': [
                {
                    'storage': 'ssd',
                    'capacity': 322122547200,
                    'bandwidth_guarantee': 0,
                },
                {
                    'storage': 'hdd',
                    'capacity': 0,
                    'bandwidth_guarantee': 0,
                }
            ],
            'internet_address': 0
        }
    }
    sconfig.set_value('personal_limit', PERS_QUOTA)

    FAKE_SCOPES_SERVICE = {
        'results': [
            {
                'role': {
                    'code': 'admin'
                }
            },
            {
                'role': {
                    'code': 'duty'
                }
            }
        ]
    }
    ctx_mock = mock.Mock()
    yp_client_mock = mock.Mock()
    pod_ctl_mock = mock.Mock()
    pod_ctl_mock.yp_client = yp_client_mock
    abc_client_mock = ctx_mock.abc_client
    ctx_mock.pod_ctl_map = {'TEST': pod_ctl_mock}
    ctx_mock.yp_client_list = [yp_client_mock]

    def get_group(acc_id, **kwargs):
        get_group_rsp = object_service_pb2.TRspGetObject()
        if acc_id == 'superusers':
            get_group_rsp.result.values.append(yson.dumps(['root']))
        else:
            get_group_rsp.result.values.append(yson.dumps(FAKE_ALL_USERS))
        return get_group_rsp

    yp_client_mock.get_group.side_effect = get_group

    # Case 1: Zero people in group
    abc_client_mock.list_service_members.side_effect = [[], []]
    pod_ctl_mock.get_users_access_allowed_to.return_value = []
    resp1 = object_service_pb2.TRspSelectObjects()
    pod_ctl_mock.list_pods.return_value = resp1
    pod_ctl_mock.list_account_ids_by_logins.return_value = {}
    abc_client_mock.list_members_iter.side_effect = [defaultdict(list), frozenset()]
    pod_ctl_mock.list_accounts.return_value = []
    resp2 = object_service_pb2.TRspSelectObjects()
    pod_ctl_mock.list_groups.return_value = resp2
    pod_ctl_mock.check_object_permissions_few.return_value = frozenset()
    yp_client_mock.check_object_permissions_few.return_value = []

    return_data = get_account_summary(ctx_mock, FAKE_ACCOUND_ID, [], [], [])
    expected_result_case_1 = accounts_pb2.PotentialResourceInfo()
    helpers.convert_dict_resources_to_protobuf(expected_result_case_1.group_quota, helpers.make_dict_resources(0, 0, 0, 0, 0))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_1.used_quota, helpers.make_dict_resources(0, 0, 0, 0, 0))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_1.diff_group_with_used, helpers.make_dict_resources(0, 0, 0, 0, 0))
    assert return_data == expected_result_case_1

    # Case 2: Zero people with role QYP User and few people in SP
    abc_client_mock.list_service_members.side_effect = [[], FAKE_SP_USERS]
    # pod_ctl_mock.get_users_access_allowed_to.return_value = FAKE_VM_ACCESS_ALLOW[-1]
    resp1 = object_service_pb2.TRspSelectObjects()
    r = resp1.results.add()
    r.values.extend([yson.dumps('vm_id3'), yson.dumps(make_spec_pod(2, 8, 200)), yson.dumps('login3')])
    pod_ctl_mock.list_pods.return_value = resp1

    def list_account_ids_by_logins(logins):
        return {login: FAKE_LIST_ACCOUNTS for login in logins}

    pod_ctl_mock.list_account_ids_by_logins.side_effect = list_account_ids_by_logins
    result_filter = defaultdict(list)
    result_filter['login1'].extend(FAKE_LIST_ACCOUNTS)
    result_filter['login2'].extend(FAKE_LIST_ACCOUNTS)
    result_filter['login3'].extend(FAKE_LIST_ACCOUNTS)
    abc_client_mock.list_members_iter.side_effect = [make_yield(FAKE_SCOPES_SERVICE), FAKE_GROUP_USERS, result_filter]
    acc1 = data_model.TAccount()
    acc1.meta.id = 'abc:service:11111'
    acc1.spec.resource_limits.per_segment['dev'].cpu.capacity = 20 * 1000
    acc1.spec.resource_limits.per_segment['dev'].memory.capacity = 40 * 1024 ** 3
    acc1.spec.resource_limits.per_segment['dev'].disk_per_storage_class['hdd'].capacity = 0
    acc1.spec.resource_limits.per_segment['dev'].disk_per_storage_class['ssd'].capacity = 200 * 1024 ** 3
    acc1.spec.resource_limits.per_segment['default'].cpu.capacity = 40 * 1000
    acc1.spec.resource_limits.per_segment['default'].memory.capacity = 200 * 1024 ** 3
    acc1.spec.resource_limits.per_segment['default'].disk_per_storage_class['hdd'].capacity = 0
    acc1.spec.resource_limits.per_segment['default'].disk_per_storage_class['ssd'].capacity = 2000 * 1024 ** 3
    acc1.status.resource_usage.per_segment['dev'].cpu.capacity = 10 * 1000
    acc1.status.resource_usage.per_segment['dev'].memory.capacity = 20 * 1024 ** 3
    acc1.status.resource_usage.per_segment['dev'].disk_per_storage_class['hdd'].capacity = 0
    acc1.status.resource_usage.per_segment['dev'].disk_per_storage_class['ssd'].capacity = 100 * 1024 ** 3
    acc1.status.resource_usage.per_segment['default'].cpu.capacity = 20 * 1000
    acc1.status.resource_usage.per_segment['default'].memory.capacity = 100 * 1024 ** 3
    acc1.status.resource_usage.per_segment['default'].disk_per_storage_class['hdd'].capacity = 0
    acc1.status.resource_usage.per_segment['default'].disk_per_storage_class['ssd'].capacity = 1000 * 1024 ** 3

    acc2 = data_model.TAccount()
    acc2.meta.id = constant.QYP_PERSONAL_ID
    acc2.spec.resource_limits.per_segment['dev'].cpu.capacity = 20 * 1000
    acc2.spec.resource_limits.per_segment['dev'].memory.capacity = 40 * 1024 ** 3
    acc2.spec.resource_limits.per_segment['dev'].disk_per_storage_class['hdd'].capacity = 0
    acc2.spec.resource_limits.per_segment['dev'].disk_per_storage_class['ssd'].capacity = 200 * 1024 ** 3
    acc2.status.resource_usage.per_segment['dev'].cpu.capacity = 10 * 1000
    acc2.status.resource_usage.per_segment['dev'].memory.capacity = 20 * 1024 ** 3
    acc2.status.resource_usage.per_segment['dev'].disk_per_storage_class['hdd'].capacity = 0
    acc2.status.resource_usage.per_segment['dev'].disk_per_storage_class['ssd'].capacity = 100 * 1024 ** 3

    pod_ctl_mock.list_accounts.return_value = [acc1, acc2]

    resp2 = object_service_pb2.TRspSelectObjects()
    r = resp2.results.add()
    r.values.extend([yson.dumps('abc:service:11111'), yson.dumps(FAKE_ALL_USERS)])
    r = resp2.results.add()
    r.values.extend([yson.dumps(constant.QYP_PERSONAL_ID), yson.dumps(FAKE_ALL_USERS)])
    pod_ctl_mock.list_groups.return_value = resp2
    pod_ctl_mock.check_object_permissions_few.return_value = [['login3', data_model.ACA_ALLOW],
                                                                 ['login2', data_model.ACA_ALLOW],
                                                                 ['login1', data_model.ACA_ALLOW]]
    yp_client_mock.check_object_permissions_few.return_value = [['login3', data_model.ACA_ALLOW],
                                                                 ['login2', data_model.ACA_DENY],
                                                                 ['login1', data_model.ACA_DENY]]

    return_data = get_account_summary(ctx_mock, FAKE_ACCOUND_ID, [], [], [])
    len_all_users = len(FAKE_ALL_USERS)
    expected_result_case_2 = accounts_pb2.PotentialResourceInfo()
    expected_result_case_2.count_users = 1
    helpers.convert_dict_resources_to_protobuf(expected_result_case_2.group_quota,
                                               helpers.make_dict_resources(8, 50, 0, 300, 0, False))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_2.used_quota,
                                               helpers.make_dict_resources(9, 22, 0, 267 + 6, 0, False))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_2.diff_group_with_used,
                                               helpers.make_dict_resources(-1, 28, 0, 33 - 6, 0, False))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_2.users['login3'].group_cons['abc:service:11111'],
                                               helpers.make_dict_resources(20.0 / len_all_users, 40.0 / len_all_users, 0.0, 200.0 / len_all_users, 0.0, False))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_2.users['login3'].group_cons['summary'],
                                               helpers.make_dict_resources(7, 14, 0, 67, 0, False))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_2.users['login3'].diff_pers_with_used,
                                               helpers.make_dict_resources(-1, 28, 0, 33 - 6, 0, False))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_2.users['login3'].personal_vms['vm_id3'],
                                               helpers.make_dict_resources(2, 8, 0, 200 + 6, 0, False))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_2.users['login3'].personal_vms['summary'],
                                               helpers.make_dict_resources(2, 8, 0, 200 + 6, 0, False))
    assert return_data == expected_result_case_2

    # Case 3: Few people with role QYP with exclude login
    abc_client_mock.list_service_members.side_effect = [FAKE_QYP_USERS]
    pod_ctl_mock.get_users_access_allowed_to.return_value = FAKE_VM_ACCESS_ALLOW
    resp1 = object_service_pb2.TRspSelectObjects()
    r = resp1.results.add()
    r.values.extend([yson.dumps('vm_id1'), yson.dumps(make_spec_pod(2, 8, 200)), yson.dumps('login1')])
    r = resp1.results.add()
    r.values.extend([yson.dumps('vm_id2'), yson.dumps(make_spec_pod(4, 8, 200)), yson.dumps('login2')])
    r = resp1.results.add()
    r.values.extend([yson.dumps('vm_id3'), yson.dumps(make_spec_pod(2, 8, 100)), yson.dumps('login3')])
    pod_ctl_mock.list_pods.return_value = resp1
    abc_client_mock.list_members_iter.side_effect = [make_yield(FAKE_SCOPES_SERVICE), FAKE_GROUP_USERS, result_filter]
    pod_ctl_mock.list_accounts.return_value = [acc1, acc2]
    pod_ctl_mock.list_groups.return_value = resp2
    pod_ctl_mock.check_object_permissions_few.return_value = [['login3', data_model.ACA_ALLOW],
                                                                 ['login2', data_model.ACA_ALLOW],
                                                                 ['login1', data_model.ACA_ALLOW]]
    yp_client_mock.check_object_permissions_few.return_value = [['login3', data_model.ACA_DENY],
                                                                 ['login2', data_model.ACA_ALLOW],
                                                                 ['login1', data_model.ACA_DENY]]

    return_data = get_account_summary(ctx_mock, FAKE_ACCOUND_ID, ['login1'], [], [])
    expected_result_case_3 = accounts_pb2.PotentialResourceInfo()
    expected_result_case_3.count_users = 1
    helpers.convert_dict_resources_to_protobuf(expected_result_case_3.group_quota,
                                               helpers.make_dict_resources(8, 50, 0, 300, 0, False))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_3.used_quota,
                                               helpers.make_dict_resources(11, 22, 0, 267 + 1 * 6, 0, False))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_3.diff_group_with_used,
                                               helpers.make_dict_resources(-3, 28, 0, 33 - 1 * 6, 0, False))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_3.users['login2'].group_cons['abc:service:11111'],
                                               helpers.make_dict_resources(20.0 / len_all_users, 40.0 / len_all_users, 0.0, 200.0 / len_all_users, 0.0, False))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_3.users['login2'].group_cons['summary'],
                                               helpers.make_dict_resources(7, 14, 0, 67, 0, False))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_3.users['login2'].diff_pers_with_used,
                                               helpers.make_dict_resources(-3, 28, 0, 33 - 6, 0, False))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_3.users['login2'].personal_vms['vm_id2'],
                                               helpers.make_dict_resources(4, 8, 0, 200 + 6, 0, False))
    helpers.convert_dict_resources_to_protobuf(expected_result_case_3.users['login2'].personal_vms['summary'],
                                               helpers.make_dict_resources(4, 8, 0, 200 + 6, 0, False))
    assert return_data == expected_result_case_3
