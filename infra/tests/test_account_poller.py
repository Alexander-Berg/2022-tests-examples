import yt_yson_bindings
import yp.data_model as data_model
import yt.yson as yson
import mock

from yp_proto.yp.client.api.proto import object_service_pb2
from infra.qyp.account_manager.src import account_poller, dismissed_vm_owner_worker
from infra.qyp.account_manager.src import constant
from collections import defaultdict
from infra.qyp.proto_lib import vmset_pb2


def make_response_staff_user(worker_id, worker_dismiss, worker_robot, head_id, head_dismiss):
    return {
        'login': worker_id,
        'official': {
            'is_dismissed': worker_dismiss,
            'is_robot': worker_robot
        },
        'department_group': {
            'department': {
                'heads': [
                    {
                        'person': {
                            'login': head_id,
                            'official': {
                                'is_dismissed': head_dismiss
                            }
                        }
                    }
                ]
            }
        }
    }


def make_spec_account(cpu, mem, hdd, ssd, ip, remove_ip=False, remove_hdd=False):
    data = {
        "internet_address": {
            "capacity": ip
        },
        "disk_per_storage_class": {
            "ssd": {
                "capacity": ssd
            },
            "hdd": {
                "capacity": hdd
            }
        },
        "cpu": {
            "capacity": cpu
        },
        "memory": {
            "capacity": mem
        }
    }
    if remove_ip:
        data.pop("internet_address")
    if remove_hdd:
        data["isk_per_storage_class"].pop("internet_address")

    return data


def make_yield(item):
    yield item


def make_yield_iterable_object(item):
    for it in item:
        yield it


def check_equals_two_dict(dct1, dct2):
    is_ok = True
    if isinstance(dct1, dict) and isinstance(dct2, dict):
        keys1 = sorted(dct1.keys())
        keys2 = sorted(dct2.keys())
        if keys1 == keys2:
            for key in keys1:
                is_ok &= check_equals_two_dict(dct1[key], dct2[key])
        else:
            is_ok = False
    elif isinstance(dct1, list) and isinstance(dct2, list):
        dct1 = sorted(dct1)
        dct2 = sorted(dct2)
        for i in range(len(dct1)):
            is_ok &= check_equals_two_dict(dct1[i], dct2[i])
    else:
        is_ok = dct1 == dct2
    return is_ok


def assert_zk_storage_push(call_args, expected_dict, zk_key):
    assert check_equals_two_dict(call_args[0], expected_dict)
    assert call_args[1] == zk_key


def test_quota_poller(config, ctx_mock):
    ZK_DATA_ID = 'vm_outside_sp'
    yp_client_mock = ctx_mock.yp_client_list
    # Case 1: Empty return TPodSet by absence author and absence access
    resp1 = object_service_pb2.TRspSelectObjects()
    resp2 = object_service_pb2.TRspSelectObjects()
    yp_client_mock.select_objects.side_effect = [resp1, resp2]
    yp_client_mock.cluster = {'cluster_name': 'SAS', 'url': 'sas.yp.yandex.net:8443', 'fqdn': 'sas.yp-c.yandex.net'}
    yp_client_mock.check_object_permissions_few.return_value = []

    poller = account_poller.AccountPoller(ctx_mock)
    poller.set_zk_storage(mock.Mock())
    poller.quota_poller()
    expected_result_case_1 = {}
    poller.zk_storage.set.assert_called_with(expected_result_case_1, ZK_DATA_ID)

    # Case 2: Non-empty return TPodSet, filter
    resp1 = object_service_pb2.TRspSelectObjects()
    r = resp1.results.add()
    r.values.extend([yson.dumps('id1')])
    r = resp1.results.add()
    r.values.extend([yson.dumps('id2')])
    r = resp1.results.add()
    r.values.extend([yson.dumps('id3')])

    p1 = data_model.TPod()
    p1.meta.id = 'pid1'
    p1.meta.pod_set_id = 'id2'
    p1.meta.creation_time = 1234567890

    p2 = data_model.TPod()
    p2.meta.id = 'pid12'
    p2.meta.pod_set_id = 'id3'
    p2.meta.creation_time = 1234567890

    p3 = data_model.TPod()
    p3.meta.id = 'pid123'
    p3.meta.pod_set_id = 'id3'
    p3.meta.creation_time = 1234567890

    resp2 = object_service_pb2.TRspSelectObjects()
    r = resp2.results.add()
    r.values.extend([yson.dumps('author1'), yt_yson_bindings.dumps_proto(p1)])
    r = resp2.results.add()
    r.values.extend([yson.dumps('#'), yt_yson_bindings.dumps_proto(p3)])
    r = resp2.results.add()
    r.values.extend([yson.dumps('author3'), yt_yson_bindings.dumps_proto(p3)])

    yp_client_mock.select_objects.side_effect = [resp1, resp2]
    yp_client_mock.cluster = {'cluster_name': 'SAS', 'url': 'sas.yp.yandex.net:8443', 'fqdn': 'sas.yp-c.yandex.net'}

    yp_client_mock.check_object_permissions_few.return_value = [
        ('author1', data_model.ACA_ALLOW),
        ('author3', data_model.ACA_DENY)
    ]
    poller = account_poller.AccountPoller(ctx_mock)
    poller.set_zk_storage(mock.Mock())
    poller.quota_poller()
    expected_result_case_2 = {yp_client_mock.cluster['cluster_name']: [
        {
            'meta': {
                'pod_set_id': 'id3',
                'creation_time': 1234567890,
                'id': 'pid123'
            },
            'author': 'author3'
        }
    ]}
    poller.zk_storage.set.assert_called_with(expected_result_case_2, ZK_DATA_ID)


def test_vm_owners_poller(config, ctx_mock):
    yp_client_mock = ctx_mock.yp_client_list
    pod_set_data = [
        [
            'vm1',
            [{}, {"subjects": ["user1", "user2", "abc:service:1", "abc:service:2", "staff:department:3"]}],
            'test_account1'
        ],
        [
            'vm2',
            [{}, {"subjects": ["user3", "user4", "abc:service:4", "abc:service:5", "staff:department:6"]}],
            'test_account2'
        ],
        [
            'vm3',
            [{}, {"subjects": ["user5", "user6", "abc:service:7", "abc:service:8", "staff:department:9"]}],
            'test_account3'
        ],
        [
            'vm4',
            [{}, {"subjects": ["user5", "user6", "abc:service:7", "abc:service:8", "staff:department:9"]}],
            'test_account3'
        ]
    ]
    resp1 = object_service_pb2.TRspSelectObjects()
    resp1.results.extend([
        object_service_pb2.TAttributeList(values=[
            yson.dumps(pod_set_data[0][0]),
            yson.dumps(pod_set_data[0][1]),
            yson.dumps(pod_set_data[0][2]),
        ]),
        object_service_pb2.TAttributeList(values=[
            yson.dumps(pod_set_data[1][0]),
            yson.dumps(pod_set_data[1][1]),
            yson.dumps(pod_set_data[1][2]),
        ]),
        object_service_pb2.TAttributeList(values=[
            yson.dumps(pod_set_data[2][0]),
            yson.dumps(pod_set_data[2][1]),
            yson.dumps(pod_set_data[2][2]),
        ]),
    ])
    resp2 = object_service_pb2.TRspGetObjects()
    resp2.subresponses.extend([
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yson.dumps('abc:service:1')
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yson.dumps('abc:service:2')
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yson.dumps('staff:department:3')
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yson.dumps('abc:service:4')
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yson.dumps('abc:service:5')
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yson.dumps('abc:service:9')
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yson.dumps('user1')
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yson.dumps('user2')
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yson.dumps('user3')
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yson.dumps('user5')
        ])),
        object_service_pb2.TRspGetObjects.TSubresponse(result=object_service_pb2.TAttributeList(values=[
            yson.dumps('user6')
        ])),
    ])

    yp_client_mock.select_objects.return_value = resp1
    yp_client_mock.cluster = {'cluster_name': 'SAS'}
    resp_get_objects = object_service_pb2.TRspGetObjects()  # mock response for checking existing groups and users
    resp_get_objects.CopyFrom(resp2)

    resp_account_users = {
        'test_account1': {"user1", "user2", "abc:service:1", "abc:service:2", "staff:department:3"},
        'test_account2': {"user2", "user3", "user4", "abc:service:4", "abc:service:5", "staff:department:6"},
        'test_account3': {"user5", "abc:service:7", "abc:service:8", "staff:department:9"},
    }

    yp_client_mock.get_objects.return_value = resp_get_objects
    yp_client_mock.get_object_access_allowed_for.return_value = resp_account_users

    # Case 1: workers not dismissed, groups not deactivated
    poller = account_poller.AccountPoller(ctx_mock)
    zk = mock.Mock()
    zk.get = mock.Mock(return_value={'vm4.SAS': 1})
    poller.set_zk_storage(zk)

    config.set_value('jobs.dismissed_owners.enable', True)
    with mock.patch(
        'infra.qyp.account_manager.src.dismissed_vm_owner_worker.DismissedVmOwnerWorker._fill_account_admin_members',
            return_value=None):
        poller.vm_owners_poller()
    expected_result_1 = {
        'test_account2': {
            'vms': {
                'vm2.SAS': {
                    'account_id': 'test_account2', 'groups': ['staff:department:6'],
                    'non_account_users': [], 'dismissed_users': ['user4'], 'author': None
                }
            },
            'admin_members': [],
            'name': ''
        },
        'test_account3': {
            'vms': {
                'vm3.SAS': {
                    'account_id': 'test_account3',
                    'groups': ['abc:service:7', 'abc:service:8', 'staff:department:9'],
                    'non_account_users': ['user6'],
                    'dismissed_users': [],
                    'author': None,
                }
            },
            'admin_members': [],
            'name': ''
        }
    }
    assert_zk_storage_push(poller.zk_storage.set.call_args[0], expected_result_1, 'dismissed_owners_vms_by_abc')

    # Case 2: set blacklist account
    config.set_value('jobs.dismissed_owners.blacklist_accounts_for_owning_check', ['test_account3'])
    with mock.patch(
        'infra.qyp.account_manager.src.dismissed_vm_owner_worker.DismissedVmOwnerWorker._fill_account_admin_members',
            return_value=None):
        poller.vm_owners_poller()
    expected_result_1['test_account3']['vms']['vm3.SAS']['non_account_users'] = []
    assert_zk_storage_push(poller.zk_storage.set.call_args[0], expected_result_1, 'dismissed_owners_vms_by_abc')

    # Case 3: set blacklist user
    config.set_value('jobs.dismissed_owners.blacklist_accounts_for_owning_check', [])
    config.set_value('jobs.dismissed_owners.blacklist_users', ['user6'])
    with mock.patch(
        'infra.qyp.account_manager.src.dismissed_vm_owner_worker.DismissedVmOwnerWorker._fill_account_admin_members',
            return_value=None):
        poller.vm_owners_poller()
    assert_zk_storage_push(poller.zk_storage.set.call_args[0], expected_result_1, 'dismissed_owners_vms_by_abc')


def test_ticket_handling(config, ctx_mock):
    config.set_value('jobs.dismissed_owners.dry_run_tickets', False)
    yp_client_mock = mock.Mock()
    group_members = object_service_pb2.TRspGetObject()
    group_members.result.values.extend(['owner1', 'owner2'])
    yp_client_mock.get_group = mock.Mock(return_value=group_members)
    zk = mock.Mock()
    startrek_cli = mock.Mock()
    calendar_cli = mock.Mock()
    calendar_cli.is_holiday = mock.Mock(return_value=True)
    worker = dismissed_vm_owner_worker.DismissedVmOwnerWorker(zk, [yp_client_mock])
    worker.startrek_cli = startrek_cli
    worker.calendar_cli = calendar_cli
    vms_by_abc = {
        'abc:service:1': {
            'vms': {
                'vm3.SAS': {
                    'account_id': 'test_account3', 'groups': ['abc:service:7', 'abc:service:8', 'staff:department:9'],
                    'non_account_users': ['user6'], 'dismissed_users': [], 'author': None,
                }
            },
            'admin_members': [],
            'name': '',

        },
        'abc:service:2': {
            'vms': {
                'vm2.SAS': {
                    'account_id': 'test_account2', 'groups': ['staff:department:6'], 'non_account_users': [],
                    'dismissed_users': ['user4'], 'author': None
                },
            },
            'admin_members': [],
            'name': 'account_name',
        }
    }

    # Case 1: open tickets
    zk.get = mock.Mock(return_value={})
    worker._handle_tickets(vms_by_abc)
    assert len(worker.zk_storage.set.call_args[0][0]) == 2
    assert worker.zk_storage.set.call_args[0][1] == worker.zk_open_tickets_by_abc_key

    # Case 2. close tickets
    open_tickets = {
        'abc:service:1': {
            'ticket': {
                'key': 'ticket1', 'queue': 'test', 'summary': '', 'description': '',
                'tags': [], 'assignee': '', 'followers': []
            },
            'vm_data': {'account_id': 'acc', 'groups': [''], 'non_account_users': [], 'dismissed_users': ['user4']},
            'last_warn_time': 0
        }
    }
    zk.get = mock.Mock(return_value=open_tickets)
    worker._handle_tickets(vms_by_abc={})
    assert worker.startrek_cli.close_ticket.call_args[0] == ('ticket1', 'fixed')
    assert len(worker.zk_storage.set.call_args[0][0]) == 0
    assert worker.zk_storage.set.call_args[0][1] == worker.zk_open_tickets_by_abc_key


def test_accounts_overquoting(config, ctx_mock):
    ZK_DATA_ID = 'overquoting_accounts'
    yp_client_mock = ctx_mock.yp_client_list
    config.set_value('overquoting_segments', ['dev', 'default'])
    # Case 1: Empty overquoting
    resp = object_service_pb2.TRspSelectObjects()
    r = resp.results.add()
    limits = {
        'per_segment': {
            'default': make_spec_account(20 * 1000, 100 * 1024 ** 3, 0, 1000 * 1024 ** 3, 5),
        }
    }
    usage = {
        'per_segment': {
            'default': make_spec_account(10 * 1000, 50 * 1024 ** 3, 0, 999 * 1024 ** 3, 0),
        }
    }
    r.values.extend([yson.dumps('abc:service:1111'), yson.dumps(usage), yson.dumps(limits)])
    yp_client_mock.list_accounts.return_value = resp
    poller = account_poller.AccountPoller(ctx_mock)
    poller.set_zk_storage(mock.Mock())
    poller.overquoting_account_poller()
    expected_result_case_1 = defaultdict(list)
    poller.zk_storage.set.assert_called_with(expected_result_case_1, ZK_DATA_ID)

    # Case 2: Empty per_usage in limits
    resp = object_service_pb2.TRspSelectObjects()
    r = resp.results.add()
    limits = {
        'per_segment': {}
    }
    usage = {
        'per_segment': {
            'default': make_spec_account(0, 0, 0, 0, 0),
            'dev': make_spec_account(0, 0, 0, 0, 0)
        }
    }
    r.values.extend([yson.dumps('abc:service:1111'), yson.dumps(usage), yson.dumps(limits)])
    yp_client_mock.list_accounts.return_value = resp
    poller = account_poller.AccountPoller(ctx_mock)
    poller.set_zk_storage(mock.Mock())
    poller.overquoting_account_poller()
    expected_result_case_2 = defaultdict(list)
    poller.zk_storage.set.assert_called_with(expected_result_case_2, ZK_DATA_ID)

    # Case 3: One overquting dev
    resp = object_service_pb2.TRspSelectObjects()
    r = resp.results.add()
    limits = {
        'per_segment': {
            'default': make_spec_account(20 * 1000, 100 * 1024 ** 3, 0, 1000 * 1024 ** 3, 5),
        }
    }
    usage = {
        'per_segment': {
            'default': make_spec_account(22 * 1000, 50 * 1024 ** 3, 0, 999 * 1024 ** 3, 0),
        }
    }
    r.values.extend([yson.dumps('abc:service:1111'), yson.dumps(usage), yson.dumps(limits)])
    yp_client_mock.list_accounts.return_value = resp
    yp_client_mock.cluster = {'cluster_name': 'SAS', 'url': 'sas.yp.yandex.net:8443', 'fqdn': 'sas.yp-c.yandex.net'}
    poller = account_poller.AccountPoller(ctx_mock)
    poller.set_zk_storage(mock.Mock())
    poller.overquoting_account_poller()
    expected_result_case_3 = {
        'SAS': [
            {
                'usage': usage['per_segment']['default'],
                'limits': limits['per_segment']['default'],
                'id': 'abc:service:1111',
                'segment': 'default',
                'overquoting': {
                    'cpu': 2000
                }
            }
        ]
    }
    poller.zk_storage.set.assert_called_with(expected_result_case_3, ZK_DATA_ID)

    # Case 4: Few overquoting
    resp = object_service_pb2.TRspSelectObjects()
    r = resp.results.add()
    limits1 = {
        'per_segment': {
            'default': make_spec_account(20 * 1000, 100 * 1024 ** 3, 0, 1000 * 1024 ** 3, 5),
        }
    }
    limits2 = {
        'per_segment': {
            'dev': make_spec_account(10 * 1000, 25 * 1024 ** 3, 0, 200 * 1024 ** 3, 0),
        }
    }
    usage1 = {
        'per_segment': {
            'default': make_spec_account(22 * 1000, 50 * 1024 ** 3, 0, 999 * 1024 ** 3, 0),
        }
    }
    usage2 = {
        'per_segment': {
            'dev': make_spec_account(22 * 1000, 50 * 1024 ** 3, 0, 300 * 1024 ** 3, 0),
        }
    }
    r.values.extend([yson.dumps('abc:service:1111'), yson.dumps(usage1), yson.dumps(limits1)])
    r = resp.results.add()
    r.values.extend([yson.dumps('abc:service:2222'), yson.dumps(usage2), yson.dumps(limits2)])
    yp_client_mock.list_accounts.return_value = resp
    yp_client_mock.cluster = {'cluster_name': 'SAS', 'url': 'sas.yp.yandex.net:8443', 'fqdn': 'sas.yp-c.yandex.net'}
    poller = account_poller.AccountPoller(ctx_mock)
    poller.set_zk_storage(mock.Mock())
    poller.overquoting_account_poller()
    expected_result_case_4 = {
        'SAS': [
            {
                'usage': usage1['per_segment']['default'],
                'limits': limits1['per_segment']['default'],
                'id': 'abc:service:1111',
                'segment': 'default',
                'overquoting': {
                    'cpu': 2000
                }
            },
            {
                'usage': usage2['per_segment']['dev'],
                'limits': limits2['per_segment']['dev'],
                'id': 'abc:service:2222',
                'segment': 'dev',
                'overquoting': {
                    'cpu': 12000,
                    'memory': 25 * 1024 ** 3,
                    'ssd': 100 * 1024 ** 3
                }
            }
        ]
    }
    poller.zk_storage.set.assert_called_with(expected_result_case_4, ZK_DATA_ID)

    # Case 5: Overquoting not dev and default
    resp = object_service_pb2.TRspSelectObjects()
    r = resp.results.add()
    limits1 = {
        'per_segment': {
            'default_fake': make_spec_account(20 * 1000, 100 * 1024 ** 3, 0, 1000 * 1024 ** 3, 5),
        }
    }
    limits2 = {
        'per_segment': {
            'dev_fake': make_spec_account(10 * 1000, 25 * 1024 ** 3, 0, 200 * 1024 ** 3, 0),
        }
    }
    usage1 = {
        'per_segment': {
            'default_fake': make_spec_account(22 * 1000, 50 * 1024 ** 3, 0, 999 * 1024 ** 3, 0),
        }
    }
    usage2 = {
        'per_segment': {
            'dev_fake': make_spec_account(22 * 1000, 50 * 1024 ** 3, 0, 300 * 1024 ** 3, 0),
        }
    }
    r.values.extend([yson.dumps('abc:service:1111'), yson.dumps(usage1), yson.dumps(limits1)])
    r = resp.results.add()
    r.values.extend([yson.dumps('abc:service:2222'), yson.dumps(usage2), yson.dumps(limits2)])
    yp_client_mock.list_accounts.return_value = resp
    yp_client_mock.cluster = {'cluster_name': 'SAS', 'url': 'sas.yp.yandex.net:8443', 'fqdn': 'sas.yp-c.yandex.net'}
    poller = account_poller.AccountPoller(ctx_mock)
    poller.set_zk_storage(mock.Mock())
    poller.overquoting_account_poller()
    expected_result_case_5 = defaultdict(list)
    poller.zk_storage.set.assert_called_with(expected_result_case_5, ZK_DATA_ID)


def test_find_vm_unused(config, ctx_mock):
    ZK_DATA_ID = 'table_unused_vms'
    FIXED_TIME_TEST = 1567900800
    yp_client_mock = ctx_mock.yp_client_list
    vmproxy_client_mock = ctx_mock.vmproxy_client
    abc_client_mock = ctx_mock.abc_client

    config.set_value('jobs.unused_vms', {
        'enable': True, 'enable_update': True,
        'accounts_to_mark': ['tmp', 'abc:service:4172'], 'accounts_to_delete': ['tmp', 'abc:service:4172']
    })

    yp_client_mock.cluster = {'cluster_name': 'SAS'}

    vm_pb_linux = vmset_pb2.VM()
    vm_pb_linux.spec.qemu.node_segment = 'dev'
    vm_pb_linux.meta.auth.owners.logins.append('user1')
    vm_pb_linux.spec.account_id = constant.QYP_PERSONAL_ID
    yson_vm_linux = yson.dumps(vm_pb_linux.SerializeToString(deterministic=True))
    vm_pb_win = vmset_pb2.VM()
    vm_pb_win.spec.qemu.node_segment = 'dev'
    vm_pb_win.meta.auth.owners.logins.append('user1')
    vm_pb_win.spec.qemu.vm_type = vmset_pb2.VMType.WINDOWS
    yson_vm_win = yson.dumps(vm_pb_win.SerializeToString(deterministic=True))
    vm_pb_non_personal = vmset_pb2.VM()
    vm_pb_non_personal.CopyFrom(vm_pb_linux)
    vm_pb_non_personal.spec.account_id = 'abc:abc:abc'
    yson_vm_non_personal = yson.dumps(vm_pb_non_personal.SerializeToString(deterministic=True))

    resp = object_service_pb2.TRspSelectObjects()
    r = resp.results.add()
    r.values.extend([yson.dumps('pod1'), yson.dumps(10**6), yson_vm_win])
    r = resp.results.add()
    r.values.extend([yson.dumps('pod2'), yson.dumps(10**6), yson_vm_linux])
    r = resp.results.add()
    # unused VM but recently created 10 days ago
    r.values.extend(
        [yson.dumps('pod3'), yson.dumps((FIXED_TIME_TEST - constant.PERIOD * 10)*10**6), yson_vm_linux]
    )
    r = resp.results.add()
    r.values.extend([yson.dumps('pod4'), yson.dumps(10**6), yson_vm_linux])
    r = resp.results.add()
    r.values.extend([yson.dumps('pod5'), yson.dumps(10**6), yson_vm_linux])
    r = resp.results.add()
    r.values.extend([yson.dumps('pod6'), yson.dumps(10**6), yson_vm_linux])
    r = resp.results.add()
    r.values.extend([yson.dumps('pod7'), yson.dumps(10**6), yson_vm_linux])
    r = resp.results.add()
    r.values.extend([yson.dumps('pod8'), yson.dumps(10**6), yson_vm_linux])
    r = resp.results.add()
    r.values.extend([yson.dumps('pod9'), yson.dumps(10**6), yson_vm_linux])
    r = resp.results.add()
    r.values.extend([yson.dumps('pod10'), yson.dumps(10**6), yson_vm_linux])
    r = resp.results.add()
    r.values.extend([yson.dumps('pod11'), yson.dumps(10**6), yson_vm_linux])
    r = resp.results.add()
    r.values.extend([yson.dumps('pod12'), yson.dumps(10**6), yson_vm_linux])
    r = resp.results.add()
    r.values.extend([yson.dumps('pod13'), yson.dumps(10**6), yson_vm_linux])
    r = resp.results.add()
    r.values.extend([yson.dumps('pod14'), yson.dumps(10**6), yson_vm_linux])
    r = resp.results.add()
    r.values.extend([yson.dumps('pod15'), yson.dumps(10**6), yson_vm_linux])
    r = resp.results.add()
    r.values.extend([yson.dumps('pod16'), yson.dumps(10**6), yson_vm_linux])
    r = resp.results.add()
    r.values.extend([yson.dumps('pod17'), yson.dumps(10**6), yson_vm_non_personal])
    yp_client_mock.list_pods.return_value = resp

    prep_golovan = {
        'pod2': [2] * (constant.DEFAULT_DAYS - 5) + [0] * 5,
        'pod3': [0] * constant.DEFAULT_DAYS,
        'pod4': [0] * constant.DEFAULT_DAYS,
        'pod5': [0] * constant.DEFAULT_DAYS,
        'pod6': [0] * constant.DEFAULT_DAYS,
        'pod7': [0] * (constant.DEFAULT_DAYS - 5) + [1] * 5,
        'pod8': [0] * constant.DEFAULT_DAYS,
        'pod9': [0] * constant.DEFAULT_DAYS,
        'pod10': [0] * (constant.DEFAULT_DAYS - 5) + [1] * 5,
        'pod11': [0] * constant.DEFAULT_DAYS,
        'pod12': [0] * constant.DEFAULT_DAYS,
        'pod13': [0] * (constant.DEFAULT_DAYS - 5) + [1] * 5,
        'pod14': [0] * constant.DEFAULT_DAYS,
        'pod15': [0] * constant.DEFAULT_DAYS,
        'pod16': [0] * constant.DEFAULT_DAYS,
    }
    resp_golovan = []
    for i in range(constant.DEFAULT_DAYS):
        cur_timestamp = {}
        for pod in range(1, 16):
            pod_id = 'pod{}'.format(pod + 1)
            # recent vms filtered without sending signals
            if pod_id == 'pod3':
                continue
            key = 'itype=qemuvm;geo=sas;prj={};ctype=prod:unistat-ssh_syn_count_dmmm'.format(pod_id)
            val = prep_golovan[pod_id][i]
            cur_timestamp[key] = val
        resp_golovan.append((i, cur_timestamp))

    def list_backups(_, pod_id):
        backup = vmset_pb2.Backup()
        if pod_id == 'pod15':
            backup.status.state = vmset_pb2.BackupStatus.COMPLETED
            backup.status.url = 'qdm:123'
            return [backup]
        if pod_id == 'pod16':
            backup.status.state = vmset_pb2.BackupStatus.REMOVED
            return [backup]

    def list_abc_services(spec):
        if spec.get('parent__with_descendants') == '4172':
            return [{'id': '1'}, {'id': '2'}]
        return []

    abc_client_mock.list_services.side_effect = list_abc_services
    vmproxy_client_mock.list_backups.side_effect = list_backups

    poller = account_poller.AccountPoller(ctx_mock)
    resp_zk = [
        {'pod_id': 'pod5', 'cluster': 'sas', 'state': 1, 'last_timestamp': FIXED_TIME_TEST - 2 * constant.PERIOD, 'unused_days': 35},
        {'pod_id': 'pod6', 'cluster': 'sas', 'state': 1, 'last_timestamp': FIXED_TIME_TEST - constant.COUNT_DAYS_MOVE_STATE_2 * constant.PERIOD, 'unused_days': 35},
        {'pod_id': 'pod7', 'cluster': 'sas', 'state': 1, 'last_timestamp': FIXED_TIME_TEST - constant.COUNT_DAYS_MOVE_STATE_2 * constant.PERIOD, 'unused_days': 35},
        {'pod_id': 'pod8', 'cluster': 'sas', 'state': 2, 'last_timestamp': FIXED_TIME_TEST - 2 * constant.PERIOD, 'unused_days': 35},
        {'pod_id': 'pod9', 'cluster': 'sas', 'state': 2, 'last_timestamp': FIXED_TIME_TEST - constant.COUNT_DAYS_MOVE_STATE_3 * constant.PERIOD, 'unused_days': 35},
        {'pod_id': 'pod10', 'cluster': 'sas', 'state': 2, 'last_timestamp': FIXED_TIME_TEST - constant.COUNT_DAYS_MOVE_STATE_3 * constant.PERIOD, 'unused_days': 35},
        {'pod_id': 'pod11', 'cluster': 'sas', 'state': 3, 'last_timestamp': FIXED_TIME_TEST - constant.PERIOD / 2, 'unused_days': 35},
        {'pod_id': 'pod12', 'cluster': 'sas', 'state': 3, 'last_timestamp': FIXED_TIME_TEST - constant.COUNT_DAYS_MOVE_STATE_4 * constant.PERIOD, 'unused_days': 35},
        {'pod_id': 'pod13', 'cluster': 'sas', 'state': 3, 'last_timestamp': FIXED_TIME_TEST - constant.COUNT_DAYS_MOVE_STATE_4 * constant.PERIOD, 'unused_days': 35},
        {'pod_id': 'pod14', 'cluster': 'sas', 'state': 3, 'last_timestamp': FIXED_TIME_TEST - constant.COUNT_DAYS_MOVE_STATE_4 * constant.PERIOD, 'unused_days': 35},
        {'pod_id': 'pod15', 'cluster': 'sas', 'state': 4, 'last_timestamp': FIXED_TIME_TEST - 31 * constant.PERIOD, 'unused_days': 35},
        {'pod_id': 'pod16', 'cluster': 'sas', 'state': 4, 'last_timestamp': FIXED_TIME_TEST - 31 * constant.PERIOD, 'unused_days': 35},
    ]
    poller.set_zk_storage(mock.Mock())
    poller.zk_storage.get.return_value = resp_zk

    with mock.patch('time.time', return_value=FIXED_TIME_TEST):
        with mock.patch('infra.qyp.account_manager.src.account_poller.AccountPoller._send_golovan_request', return_value=resp_golovan):
            poller.find_unused_vms()

    vmproxy_client_mock.deallocate.assert_called_with('sas', 'pod15')
    vmproxy_client_mock.backup.assert_called_with('sas', 'pod16')

    expected_result_1 = [
        {'pod_id': 'pod4', 'cluster': 'sas', 'state': 1, 'last_timestamp': FIXED_TIME_TEST, 'unused_days': 31},
        {'pod_id': 'pod5', 'cluster': 'sas', 'state': 1, 'last_timestamp': FIXED_TIME_TEST - 2 * constant.PERIOD, 'unused_days': 33},
        {'pod_id': 'pod6', 'cluster': 'sas', 'state': 2, 'last_timestamp': FIXED_TIME_TEST, 'unused_days': 45},
        {'pod_id': 'pod8', 'cluster': 'sas', 'state': 2, 'last_timestamp': FIXED_TIME_TEST - 2 * constant.PERIOD, 'unused_days': 47},
        {'pod_id': 'pod9', 'cluster': 'sas', 'state': 3, 'last_timestamp': FIXED_TIME_TEST, 'unused_days': 58},
        {'pod_id': 'pod11', 'cluster': 'sas', 'state': 3, 'last_timestamp': FIXED_TIME_TEST - constant.PERIOD / 2, 'unused_days': 58},
        {'pod_id': 'pod12', 'cluster': 'sas', 'state': 4, 'last_timestamp': FIXED_TIME_TEST, 'unused_days': 59},
        {'pod_id': 'pod14', 'cluster': 'sas', 'state': 4, 'last_timestamp': FIXED_TIME_TEST, 'unused_days': 59},
        {'pod_id': 'pod16', 'cluster': 'sas', 'state': 4, 'last_timestamp': FIXED_TIME_TEST - 31 * constant.PERIOD, 'unused_days': 59},
    ]
    assert_zk_storage_push(poller.zk_storage.set.call_args[0], expected_result_1, ZK_DATA_ID)

    mark_call, unmark_call = yp_client_mock.update_objects.call_args_list
    for label in mark_call.kwargs['set_updates']:
        assert label['/labels/qyp_unused_for_30_days'] == 'true'
    assert mark_call.kwargs['object_type'] == data_model.OT_POD_SET
    assert not set(mark_call.kwargs['object_ids']).difference(['pod4'])

    for label in unmark_call.kwargs['set_updates']:
        assert label['/labels/qyp_unused_for_30_days'] == 'false'
    assert unmark_call.kwargs['object_type'] == data_model.OT_POD_SET
    assert not set(unmark_call.kwargs['object_ids']).difference(['pod7', 'pod10', 'pod13', 'pod15'])
