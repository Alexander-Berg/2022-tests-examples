import inject
import logging
import mock
import pytest
import six
import ujson

import nanny_rpc_client
from awacs.lib.nannyclient import INannyClient
from awacs.lib.nannyrpcclient import INannyRpcClient
from awacs.lib.order_processor.model import FeedbackMessage
from awacs.lib.staffclient import IStaffClient
from awacs.lib.ypliterpcclient import IYpLiteRpcClient
from awacs.model import alerting
from awacs.model.balancer.order import processors as p
from awacs.model.balancer.order.processors import BalancerOrder
from awacs.model.errors import NotFoundError
from infra.awacs.proto import model_pb2
from awtest.mocks.abc_client import AbcMockClient
from awtest.mocks.staff_client import StaffMockClient
from awtest.mocks.yp_lite_client import YpLiteMockClient
from awtest.mocks.nanny_client import NannyMockClient
from awtest.mocks.nanny_rpc_client import NannyRpcMockClient
from nanny_repo import repo_pb2
from nanny_rpc_client.exceptions import BadRequestError
from infra.swatlib.auth.abc import IAbcClient
from awtest import wait_until, wait_until_passes
from yp_lite_ui_repo import pod_sets_api_pb2, endpoint_sets_pb2


NS_ID = u'namespace-id'
BALANCER_ID = u'balancer-id_sas'


@pytest.fixture
def yp_lite_client():
    return YpLiteMockClient()


@pytest.fixture
def nanny_client():
    return NannyMockClient(url='https://nanny.yandex-team.ru/v2/', token='DUMMY')


@pytest.fixture
def nanny_rpc_mock_client():
    return NannyRpcMockClient()


@pytest.fixture(autouse=True)
def deps(binder, caplog, yp_lite_client, nanny_client, nanny_rpc_mock_client):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(IYpLiteRpcClient, yp_lite_client)
        b.bind(INannyRpcClient, nanny_rpc_mock_client)
        b.bind(IStaffClient, StaffMockClient())
        b.bind(IAbcClient, AbcMockClient())
        b.bind(INannyClient, nanny_client)
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def create_ns_pb(cache, zk_storage, order=None, spec=None):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    ns_pb.meta.abc_service_id = 999
    if order is not None:
        ns_pb.order.CopyFrom(order)
    if spec is not None:
        ns_pb.spec.CopyFrom(spec)
    zk_storage.create_namespace(namespace_id=NS_ID,
                                namespace_pb=ns_pb)
    assert wait_until(lambda: cache.get_namespace(NS_ID), timeout=1)
    return ns_pb


def create_balancer(cache, zk_storage, skip_pre_allocation_id=False, activate=True, use_existing_spec=False):
    b_pb = model_pb2.Balancer()
    b_pb.meta.id = BALANCER_ID
    b_pb.meta.namespace_id = NS_ID
    b_pb.meta.version = 'xxx'
    b_pb.order.content.allocation_request.location = 'SAS'
    b_pb.order.content.allocation_request.preset.type = \
        model_pb2.BalancerOrder.Content.LocationalYpLiteAllocationRequest.Preset.NANO
    b_pb.order.content.allocation_request.preset.instances_count = 1
    b_pb.order.content.abc_service_id = 999
    b_pb.order.content.activate_balancer = activate
    b_pb.spec.incomplete = True
    if not skip_pre_allocation_id:
        b_pb.order.progress.context['pre_allocation_id'] = ujson.dumps('random')
    if use_existing_spec:
        b_pb.order.content.copy_spec_from_balancer_id = 'existing_balancer'
    zk_storage.create_balancer(namespace_id=NS_ID,
                               balancer_id=BALANCER_ID,
                               balancer_pb=b_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, BALANCER_ID), timeout=1)
    return BalancerOrder(b_pb)


def create_existing_balancer(zk_storage):
    existing_b_pb = model_pb2.Balancer()
    existing_b_pb.meta.id = 'existing_balancer'
    existing_b_pb.meta.namespace_id = NS_ID
    existing_b_pb.spec.yandex_balancer.mode = existing_b_pb.spec.yandex_balancer.FULL_MODE
    existing_b_pb.spec.yandex_balancer.yaml = 'instance_macro: {include_upstreams: {}}'
    existing_b_pb.spec.config_transport.nanny_static_file.service_id = 'rtc_balancer_existing_balancer'
    zk_storage.create_balancer(namespace_id=NS_ID,
                               balancer_id='existing_balancer',
                               balancer_pb=existing_b_pb)
    return existing_b_pb


def test_start(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage, skip_pre_allocation_id=True)
    balancer.location = 'wrong'
    with pytest.raises(AssertionError, match='Unknown location "wrong"'):
        p.Start(balancer).process(ctx)

    balancer.location = 'sas'
    assert p.Start(balancer).process(ctx).name == 'ALLOCATING_YP_LITE_RESOURCES'
    assert balancer.context.get('pre_allocation_id')


def test_start_with_copy(cache, zk_storage, ctx):
    create_existing_balancer(zk_storage)
    balancer = create_balancer(cache, zk_storage)
    balancer.pb.order.content.copy_nanny_service.balancer_id = 'existing_balancer'
    assert p.Start(balancer).process(ctx).name == 'GETTING_VIRTUAL_SERVICE_IDS'
    assert balancer.context['copy_nanny_service_id'] == 'rtc_balancer_existing_balancer'


def test_getting_virtual_service_ids(cache, zk_storage, ctx, nanny_client, yp_lite_client):
    def get_service_runtime_attrs(*_, **__):
        return {
            'content': {
                'instances': {'yp_pod_ids': {'pods': [{'cluster': 'SAS', 'pod_id': 'p1'}]}}
            }
        }

    nanny_client.get_service_runtime_attrs = get_service_runtime_attrs
    create_existing_balancer(zk_storage)
    balancer = create_balancer(cache, zk_storage)
    balancer.context['copy_nanny_service_id'] = 'rtc_balancer_existing_balancer'
    assert p.GettingVirtualServiceIds(balancer).process(ctx).name == 'ALLOCATING_YP_LITE_RESOURCES'
    assert balancer.context['virtual_service_ids'] == ['v1', 'v2']


def test_allocating_yp_lite_resources(cache, zk_storage, ctx, yp_lite_client):
    balancer = create_balancer(cache, zk_storage)
    balancer.context['pod_set_id'] = '1'
    balancer.context['pod_ids'] = (1,)
    assert p.AllocatingYpLiteResources(balancer).process(ctx).name == 'GETTING_ABC_ROLE_STAFF_ID'
    assert not yp_lite_client.last_create_pod_set_request_args

    del balancer.context['pod_set_id']
    del balancer.context['pod_ids']
    assert p.AllocatingYpLiteResources(balancer).process(ctx).name == 'GETTING_ABC_ROLE_STAFF_ID'
    assert yp_lite_client.last_create_pod_set_request_args
    req = yp_lite_client.last_create_pod_set_request_args[0]  # type: pod_sets_api_pb2.CreatePodSetRequest
    assert req.allocation_mode == pod_sets_api_pb2.CreatePodSetRequest.PRE_ALLOCATION
    assert req.service_id == 'rtc_balancer_{}'.format(BALANCER_ID)
    assert req.cluster == 'SAS'
    assert req.pre_allocation_id == balancer.context['pre_allocation_id']
    assert req.allocation_request.replicas == 1
    assert req.allocation_request.snapshots_count == 5
    assert req.allocation_request.root_fs_quota_megabytes == 512
    assert req.allocation_request.work_dir_quota_megabytes == 512
    assert req.allocation_request.root_volume_storage_class == 'hdd'
    assert req.allocation_request.network_macro == balancer.allocation_request_pb.network_macro
    assert req.allocation_request.memory_guarantee_megabytes == 1536
    assert req.allocation_request.persistent_volumes[0].mount_point == '/logs'
    assert req.allocation_request.persistent_volumes[0].disk_quota_megabytes == 10240
    assert req.allocation_request.persistent_volumes[0].storage_class == 'hdd'
    assert req.allocation_request.vcpu_guarantee == 500
    assert req.allocation_request.vcpu_limit == 500
    assert req.allocation_request.sysctl_properties[0].name == 'net.ipv4.tcp_tw_reuse'
    assert req.allocation_request.sysctl_properties[0].value == '1'
    assert req.allocation_request.sysctl_properties[1].name == 'net.ipv4.tcp_retries2'
    assert req.allocation_request.sysctl_properties[1].value == '8'
    assert req.allocation_request.labels[0].key == 'awacs_namespace_id'
    assert req.allocation_request.labels[0].value == NS_ID
    assert req.antiaffinity_constraints.node_max_pods == 1
    assert req.antiaffinity_constraints.rack_max_pods == 1
    assert req.quota_settings.mode == req.quota_settings.ABC_SERVICE
    assert req.quota_settings.abc_service_id == 999


def test_allocating_yp_lite_resources_with_quota_error(cache, zk_storage, ctx, yp_lite_client):
    balancer = create_balancer(cache, zk_storage)
    msg = six.text_type(r"""
    Computing resource quota exceeded. Please request more resources for your ABC-service or use temporary junk account. Details: Method /create_objects replied with error
    Account "\xd0\xa1\xd0\xb5\xd1\x80\xd0\xb2\xd0\xb8\xd1\x81\xd1\x8b \xd0\xa1\xd0\xbb\xd1\x83\xd0\xb6\xd0\xb1\xd1\x8b DCA" (key abc:service:3207) is over CPU limit in segment "default"

***** Details:
Method /create_objects replied with error
    origin          sas1-3208.search.yandex.net in 2020-04-21T06:29:25.853637Z
    request_id      bcfdb16c-e588cd9e-9c8c44f3-29a01977
    method          create_objects
Account "\xd0\xa1\xd0\xb5\xd1\x80\xd0\xb2\xd0\xb8\xd1\x81\xd1\x8b \xd0\xa1\xd0\xbb\xd1\x83\xd0\xb6\xd0\xb1\xd1\x8b DCA" (key abc:service:3207) is over CPU limit in segment "default"
    code            100012
    origin          vla1-6057-e66.vla.yp.gencfg-c.yandex.net in 2020-04-21T06:29:25.851517Z (pid 59, tid 7192c3ef71562df8, fid ffdf05c44d0611f2)
    trace_id        955d1dcc-462f6c99-66555143-ccca94c1
    usage           1000
    limit           0
    span_id         15935344412541379813""")
    with mock.patch.object(yp_lite_client, 'create_pod_set', side_effect=BadRequestError(message=msg)):
        rv = p.AllocatingYpLiteResources(balancer).process(ctx)
        assert isinstance(rv, FeedbackMessage)
        assert rv.message == balancer.context['quota_error_message']
        assert rv.pb_error_type == model_pb2.BalancerOrder.OrderFeedback.QUOTA_ERROR
        assert rv.content['quota_error'].abc_service_id == balancer.pb.order.content.abc_service_id
        assert rv.content['quota_error'].details == u' is over CPU limit'
        assert balancer.context[
            'quota_error_message'] == u'ABC service "\u0421\u0435\u0440\u0432\u0438\u0441\u044b \u0421\u043b\u0443\u0436\u0431\u044b DCA" (id: 3207) is over CPU limit in this location.'


def test_allocating_yp_lite_resources_with_approval_after(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage)
    balancer.pb.order.content.wait_for_approval_after_allocation = True
    assert p.AllocatingYpLiteResources(balancer).process(ctx).name == 'WAITING_FOR_APPROVAL_AFTER_ALLOCATION'


def test_waiting_for_approval_after_allocation(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage)
    balancer.pb.order.content.wait_for_approval_after_allocation = True
    assert p.WaitingForApprovalAfterAllocation(balancer).process(ctx).name == 'WAITING_FOR_APPROVAL_AFTER_ALLOCATION'

    balancer.pb.order.approval.after_allocation = True
    assert p.WaitingForApprovalAfterAllocation(balancer).process(ctx).name == 'GETTING_ABC_ROLE_STAFF_ID'


def test_getting_abc_role_staff_id_noop(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage)
    balancer.context['abc_role_staff_id'] = 'test'
    assert p.GettingAbcRoleStaffId(balancer).process(ctx).name == 'CREATING_NANNY_SERVICE'
    assert balancer.context['abc_role_staff_id'] == 'test'


ADMINISTRATION_ROLE_ID = 1
DEVELOPMENT_ROLE_ID = 2
CERT_ROLE_ID = 3
RESOURCES_RESPONSIBLE_ROLE = 4
ABC_SERVICE_ID = 99


@pytest.mark.parametrize('abc_roles, abc_service_id, expected', [
    ([{"role_scope": "administration", "id": ADMINISTRATION_ROLE_ID}, {"role_scope": "development",
     "id": DEVELOPMENT_ROLE_ID}, {"role_scope": "cert", "id": CERT_ROLE_ID}], ABC_SERVICE_ID, ADMINISTRATION_ROLE_ID),
    ([{"role_scope": "administration", "id": ADMINISTRATION_ROLE_ID}, {"role_scope": "cert", "id": CERT_ROLE_ID}], ABC_SERVICE_ID, ADMINISTRATION_ROLE_ID),
    ([{"role_scope": "development", "id": DEVELOPMENT_ROLE_ID}, {"role_scope": "cert", "id": CERT_ROLE_ID}], ABC_SERVICE_ID, DEVELOPMENT_ROLE_ID),
    ([{"role_scope": "resources_responsible", "id": RESOURCES_RESPONSIBLE_ROLE}, {
     "role_scope": "cert", "id": CERT_ROLE_ID}], ABC_SERVICE_ID, ABC_SERVICE_ID),
])
def test_choosing_abc_id(abc_roles, abc_service_id, expected):
    assert expected == p.GettingAbcRoleStaffId._choose_abc_group(abc_roles, abc_service_id)


def test_getting_abc_role_staff_id(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage)
    assert p.GettingAbcRoleStaffId(balancer).process(ctx).name == 'CREATING_NANNY_SERVICE'
    assert balancer.context['abc_role_staff_id'] == '268088'


def test_creating_nanny_service_wo_alerting(cache, zk_storage, ctx, nanny_client):
    create_ns_pb(cache, zk_storage)

    def get_service_info_attrs(*_, **__):
        return {
            'content': {
                'tickets_integration': {
                    'service_release_rules': []
                },
                'monitoring_settings': {
                    'deploy_monitoring': {
                        'content': {
                            'responsible': {}
                        }
                    },
                    'juggler_settings': {
                        'content': {
                            'juggler_tags': [],
                            'juggler_hosts': [{}],
                            'active_checks': [{
                                'checks': [],
                                'passive_checks': [{
                                    'notifications': [{
                                        'on_status_change': {
                                            'method': ['email'],
                                            'users': {}
                                        }
                                    }]
                                }]
                            }]
                        }
                    }
                }
            }
        }

    def get_service_runtime_attrs(*_, **__):
        return {
            'content': {
                'instances': {}
            }
        }

    def get_service_auth_attrs(*_, **__):
        return {
            'content': {
                'conf_managers': {},
                'ops_managers': {},
                'observers': {},
            }
        }

    nanny_client.get_service_info_attrs = get_service_info_attrs
    nanny_client.get_service_runtime_attrs = get_service_runtime_attrs
    nanny_client.get_service_auth_attrs = get_service_auth_attrs
    nanny_client.create_service = mock.Mock()
    balancer = create_balancer(cache, zk_storage)
    balancer.context['abc_role_staff_id'] = 'svc_999_slug_id'
    balancer.context['pod_ids'] = ['pod1', 'pod2']
    assert p.CreatingNannyService(balancer).process(ctx).name == 'SETTING_UP_CLEANUP_POLICY'
    assert balancer.context['created_nanny_service_id'] == 'rtc_balancer_balancer-id_sas'
    nanny_client.create_service.assert_called_once_with(
        auth_attrs={
            'observers':
                {'groups': ['75815']},
            'owners': {'logins': [], 'groups': ['75815', 'svc_999_slug_id']},
            'conf_managers': {'groups': ['33712', 'svc_999_slug_id']},
            'ops_managers': {'groups': ['33712', 'svc_999_slug_id']}},
        comment='Created from L7 balancer template service "rtc-yp-lite-balancer-template"',
        info_attrs={
            'category': '/balancer/namespace-id',
            'labels': [{'value': u'sas', 'key': 'geo'}, {'value': u'prod', 'key': 'ctype'},
                       {'value': u'balancer', 'key': 'itype'},
                       {'value': u'namespace-id', 'key': 'prj'},
                       {'value': u'sas', 'key': 'dc'},
                       {'value': u'random', 'key': 'pre_allocation_id'}],
            'yp_cluster': u'SAS',
            'tickets_integration': {
                'service_release_rules': []
            },
            'abc_group': 999,
            'monitoring_settings': {
                'deploy_monitoring': {
                    'content': {
                        'responsible': {'groups': ['svc_999_slug_id']},
                        'alert_methods': ['telegram', 'email']
                    }
                },
                'juggler_settings': {
                    'content': {
                        'juggler_tags': [u'namespace-id'],
                        'juggler_hosts': [{'name': 'rtc_balancer_balancer-id_sas'}],
                        'active_checks': [{
                            'passive_checks': [{
                                'notifications': [{
                                    'on_status_change': {
                                        'method': ['email'],
                                        'users': {'logins': [], 'groups': ['svc_999_slug_id']}
                                    }
                                }],
                                'juggler_host_name': 'rtc_balancer_balancer-id_sas'
                            }],
                            'checks': []
                        }]
                    }
                }
            },
            'desc': 'awacs-powered balancer for balancer-id_sas in SAS'
        },
        runtime_attrs={
            'instances': {
                'yp_pod_ids': {
                    'orthogonal_tags': {
                        'metaprj': 'unknown',
                        'itype': u'balancer',
                        'ctype': u'prod',
                        'prj': u'namespace-id'
                    },
                    'pods': [{'cluster': u'SAS', 'pod_id': 'pod1'}, {'cluster': u'SAS', 'pod_id': 'pod2'}]
                },
                'chosen_type': 'YP_POD_IDS'}
        },
        service_id='rtc_balancer_balancer-id_sas'
    )


@pytest.mark.parametrize('ns_pb,expected_notification_group', [
    (model_pb2.Namespace(
        spec=model_pb2.NamespaceSpec(
            alerting=model_pb2.NamespaceSpec.AlertingSettings(
                version=six.text_type(alerting.CURRENT_VERSION),
                juggler_raw_downtimers=model_pb2.NamespaceSpec.AlertingSettings.JugglerRawDowntimers(
                    staff_group_ids=[1111])
            )
        ),
        order=model_pb2.NamespaceOrder(
            content=model_pb2.NamespaceOrder.Content(
                alerting_simple_settings=model_pb2.NamespaceOrder.Content.AlertingSimpleSettings(
                    notify_staff_group_id=2222)))
    ), '1111'),
    (model_pb2.Namespace(
        spec=model_pb2.NamespaceSpec(
            alerting=model_pb2.NamespaceSpec.AlertingSettings(
                version=six.text_type(alerting.CURRENT_VERSION)
            )
        ),
        order=model_pb2.NamespaceOrder(
            content=model_pb2.NamespaceOrder.Content(
                alerting_simple_settings=model_pb2.NamespaceOrder.Content.AlertingSimpleSettings(
                    notify_staff_group_id=2222)))
    ), '2222'),
    (model_pb2.Namespace(
        spec=model_pb2.NamespaceSpec(
            alerting=model_pb2.NamespaceSpec.AlertingSettings(
                version=six.text_type(alerting.CURRENT_VERSION))
        )
    ), 'svc_999_slug_id')
])
def test_creating_nanny_service_w_alerting(cache, zk_storage, ctx, nanny_client, ns_pb, expected_notification_group):
    create_ns_pb(cache, zk_storage, spec=ns_pb.spec, order=ns_pb.order)

    def get_service_info_attrs(*_, **__):
        return {
            'content': {
                'tickets_integration': {
                    'service_release_rules': [],
                },
                'monitoring_settings': {
                    'deploy_monitoring': {
                        'content': {
                            'responsible': {}
                        }
                    },
                    'juggler_settings': {
                        'content': {
                            'juggler_tags': [],
                            'juggler_hosts': [{}],
                            'active_checks': [{
                                'checks': [],
                                'passive_checks': [{
                                    'notifications': [{
                                        'on_status_change': {
                                            'method': ['email'],
                                            'users': {}
                                        }
                                    }]
                                }]
                            }]
                        }
                    }
                }
            }
        }

    def get_service_runtime_attrs(*_, **__):
        return {
            'content': {
                'instances': {}
            }
        }

    def get_service_auth_attrs(*_, **__):
        return {
            'content': {
                'conf_managers': {},
                'ops_managers': {},
                'observers': {},
            }
        }

    nanny_client.get_service_info_attrs = get_service_info_attrs
    nanny_client.get_service_runtime_attrs = get_service_runtime_attrs
    nanny_client.get_service_auth_attrs = get_service_auth_attrs
    nanny_client.create_service = mock.Mock()
    balancer = create_balancer(cache, zk_storage)
    balancer.context['abc_role_staff_id'] = 'svc_999_slug_id'
    balancer.context['pod_ids'] = ['pod1', 'pod2']
    assert p.CreatingNannyService(balancer).process(ctx).name == 'SETTING_UP_CLEANUP_POLICY'
    assert balancer.context['created_nanny_service_id'] == 'rtc_balancer_balancer-id_sas'
    nanny_client.create_service.assert_called_once_with(
        auth_attrs={
            'observers':
                {'groups': ['75815']},
            'owners': {'logins': [], 'groups': ['75815', 'svc_999_slug_id']},
            'conf_managers': {'groups': ['33712', 'svc_999_slug_id']},
            'ops_managers': {'groups': ['33712', 'svc_999_slug_id']}},
        comment='Created from L7 balancer template service "rtc-yp-lite-balancer-template"',
        info_attrs={
            'category': '/balancer/namespace-id',
            'labels': [{'value': u'sas', 'key': 'geo'}, {'value': u'prod', 'key': 'ctype'},
                       {'value': u'balancer', 'key': 'itype'},
                       {'value': u'namespace-id', 'key': 'prj'},
                       {'value': u'sas', 'key': 'dc'},
                       {'value': u'random', 'key': 'pre_allocation_id'}],
            'yp_cluster': u'SAS',
            'tickets_integration': {
                'service_release_rules': []
            },
            'abc_group': 999,
            'monitoring_settings': {
                'deploy_monitoring': {
                    'is_enabled': False,
                },
                'juggler_settings': {
                    'content': {
                        'juggler_tags': [u'namespace-id'],
                        'juggler_hosts': [{'name': 'rtc_balancer_balancer-id_sas'}],
                        'active_checks': [{
                            'passive_checks': [{
                                'notifications': [{
                                    'on_status_change': {
                                        'method': ['email'],
                                        'users': {'logins': [], 'groups': [expected_notification_group]}
                                    }
                                }],
                                'juggler_host_name': 'rtc_balancer_balancer-id_sas'
                            }],
                            'checks': []
                        }]
                    }
                }
            },
            'desc': 'awacs-powered balancer for balancer-id_sas in SAS'
        },
        runtime_attrs={
            'instances': {
                'yp_pod_ids': {
                    'orthogonal_tags': {
                        'metaprj': 'unknown',
                        'itype': u'balancer',
                        'ctype': u'prod',
                        'prj': u'namespace-id'
                    },
                    'pods': [{'cluster': u'SAS', 'pod_id': 'pod1'}, {'cluster': u'SAS', 'pod_id': 'pod2'}]
                },
                'chosen_type': 'YP_POD_IDS'}
        },
        service_id='rtc_balancer_balancer-id_sas'
    )


@pytest.mark.parametrize('original_location,new_location', [
    ('sas', 'man'),
    ('vla', 'sas')
])
def test_updating_copied_nanny_service(cache, zk_storage, ctx, nanny_client, original_location, new_location):
    create_ns_pb(cache, zk_storage)

    def get_service_info_attrs(*_, **__):
        rv = {
            'content': {
                'labels': [
                    {'key': 'other_label', 'value': 'other_value'},
                    {'key': 'dc', 'value': 'moon'},
                    {'key': 'geo', 'value': 'moon'},
                ],
                'tickets_integration': {
                    'service_release_rules': [],
                },
                'monitoring_settings': {
                    'deploy_monitoring': {
                        'is_enabled': False,
                    },
                    'juggler_settings': {
                        'content': {
                            'juggler_tags': [u'namespace-id'],
                            'juggler_hosts': [{'name': 'rtc_balancer_balancer-id_{}'.format(original_location)}],
                            'active_checks': [{
                                'passive_checks': [{
                                    'juggler_host_name': 'rtc_balancer_balancer-id_{}'.format(original_location)
                                }],
                                'checks': []
                            }]
                        }
                    }
                }
            }
        }
        return rv

    def get_service_runtime_attrs(*_, **__):
        return {
            'content': {
                'instances': {
                    'chosen_type': 'something',
                    'yp_pod_ids': {
                        'pods': [{'cluster': 'moon', 'pod_id': 'andy'}]
                    }
                }
            }
        }

    nanny_client.get_service_info_attrs = get_service_info_attrs
    nanny_client.get_service_runtime_attrs = get_service_runtime_attrs
    nanny_client.update_service = mock.Mock()
    balancer = create_balancer(cache, zk_storage)
    balancer.nanny_service_id = 'rtc_balancer_balancer-id_{}'.format(new_location)
    balancer.location = new_location
    balancer.cluster = new_location.upper()
    balancer.context['pod_ids'] = ['pod1', 'pod2']
    assert p.UpdatingCopiedNannyService(balancer).process(ctx).name == 'CREATING_ENDPOINT_SET'

    nanny_client.update_service.assert_called_once_with(
        comment='Updated info labels and runtime instances',
        info_attrs={
            'content': {
                'labels': [
                    {'key': 'other_label', 'value': 'other_value'},
                    {'key': 'dc', 'value': new_location},
                    {'key': 'geo', 'value': new_location},
                ],
                'tickets_integration': {
                    'service_release_rules': [],
                },
                'monitoring_settings': {
                    'deploy_monitoring': {
                        'is_enabled': False,
                    },
                    'juggler_settings': {
                        'content': {
                            'juggler_tags': [u'namespace-id'],
                            'juggler_hosts': [{'name': 'rtc_balancer_balancer-id_{}'.format(new_location)}],
                            'active_checks': [{
                                'passive_checks': [{
                                    'juggler_host_name': 'rtc_balancer_balancer-id_{}'.format(new_location)
                                }],
                                'checks': []
                            }]
                        }
                    }
                }
            }
        },
        runtime_attrs={
            'content': {
                'instances': {
                    'yp_pod_ids': {
                        'pods': [{'cluster': new_location.upper(), 'pod_id': 'pod1'},
                                 {'cluster': new_location.upper(), 'pod_id': 'pod2'}]
                    },
                    'chosen_type': 'YP_POD_IDS'}
            }
        },
        service_id='rtc_balancer_balancer-id_{}'.format(new_location)
    )


def test_setting_up_cleanup_policy(cache, zk_storage, ctx, nanny_rpc_mock_client):
    nanny_rpc_mock_client.update_cleanup_policy = mock.Mock()
    balancer = create_balancer(cache, zk_storage)
    assert p.SettingUpCleanupPolicy(balancer).process(ctx).name == 'SETTING_UP_REPLICATION_POLICY'
    nanny_rpc_mock_client.update_cleanup_policy.assert_called_once()
    (meta_pb, spec_pb), _ = nanny_rpc_mock_client.update_cleanup_policy.call_args
    assert meta_pb.id == 'rtc_balancer_balancer-id_sas'
    assert spec_pb.type == repo_pb2.CleanupPolicySpec.SIMPLE_COUNT_LIMIT
    assert spec_pb.simple_count_limit.snapshots_count == 1
    assert spec_pb.simple_count_limit.stalled_ttl == 'P7D'


def test_setting_up_replication_policy(cache, zk_storage, ctx, nanny_rpc_mock_client):
    nanny_rpc_mock_client.update_replication_policy = mock.Mock()
    balancer = create_balancer(cache, zk_storage)
    assert p.SettingUpReplicationPolicy(balancer).process(ctx).name == 'CREATING_ENDPOINT_SET'
    nanny_rpc_mock_client.update_replication_policy.assert_called_once()
    (meta_pb, spec_pb), _ = nanny_rpc_mock_client.update_replication_policy.call_args
    assert meta_pb.id == 'rtc_balancer_balancer-id_sas'
    assert spec_pb.replication_method == repo_pb2.ReplicationPolicySpec.MOVE
    assert spec_pb.involuntary_replication_choice == repo_pb2.ReplicationPolicySpec.ENABLED
    assert spec_pb.max_unavailable == 1
    assert spec_pb.rate_limit.delay_seconds == 43200
    assert spec_pb.maintenance_kind == repo_pb2.ReplicationPolicySpec.MK_ALL
    assert spec_pb.max_tolerable_downtime_seconds == 3600
    assert spec_pb.disruption_budget_kind == repo_pb2.ReplicationPolicySpec.MIXED


def test_creating_system_endpoint_set(cache, zk_storage, ctx, yp_lite_client):
    yp_lite_client.create_endpoint_set = mock.Mock()
    yp_lite_client.get_endpoint_set = mock.Mock(side_effect=nanny_rpc_client.exceptions.NotFoundError('test'))
    balancer = create_balancer(cache, zk_storage)
    assert p.CreatingEndpointSet(balancer).process(ctx).name == 'CREATING_USER_ENDPOINT_SET'
    yp_lite_client.create_endpoint_set.assert_called_once()
    req_pb, _ = yp_lite_client.create_endpoint_set.call_args
    req_pb = req_pb[0]
    assert req_pb.cluster == 'SAS'
    assert req_pb.meta.id == 'awacs-rtc_balancer_balancer-id_sas'
    assert req_pb.meta.service_id == 'rtc_balancer_balancer-id_sas'
    assert req_pb.meta.ownership == endpoint_sets_pb2.EndpointSetMeta.SYSTEM
    assert req_pb.spec.protocol == 'TCP'
    assert req_pb.spec.port == 80
    assert req_pb.spec.description == 'Created by awacs'


def test_creating_system_endpoint_set_noop(cache, zk_storage, ctx, yp_lite_client):
    yp_lite_client.create_endpoint_set = mock.Mock()
    balancer = create_balancer(cache, zk_storage)
    assert p.CreatingEndpointSet(balancer).process(ctx).name == 'CREATING_USER_ENDPOINT_SET'
    yp_lite_client.create_endpoint_set.assert_not_called()


def test_creating_user_endpoint_set(cache, zk_storage, ctx, yp_lite_client):
    yp_lite_client.create_endpoint_set = mock.Mock()
    yp_lite_client.get_endpoint_set = mock.Mock(side_effect=nanny_rpc_client.exceptions.NotFoundError('test'))
    balancer = create_balancer(cache, zk_storage)
    assert p.CreatingUserEndpointSet(balancer).process(ctx).name == 'CREATING_AWACS_BALANCER'
    yp_lite_client.create_endpoint_set.assert_called_once()
    req_pb, _ = yp_lite_client.create_endpoint_set.call_args
    req_pb = req_pb[0]
    assert req_pb.cluster == 'SAS'
    assert req_pb.meta.id == 'rtc_balancer_balancer-id_sas'
    assert req_pb.meta.service_id == 'rtc_balancer_balancer-id_sas'
    assert req_pb.meta.ownership == endpoint_sets_pb2.EndpointSetMeta.USER
    assert req_pb.spec.protocol == 'TCP'
    assert req_pb.spec.port == 80
    assert req_pb.spec.description == 'Created by awacs'


def test_creating_user_endpoint_set_noop(cache, zk_storage, ctx, yp_lite_client):
    yp_lite_client.create_endpoint_set = mock.Mock()
    balancer = create_balancer(cache, zk_storage)
    assert p.CreatingUserEndpointSet(balancer).process(ctx).name == 'CREATING_AWACS_BALANCER'
    yp_lite_client.create_endpoint_set.assert_not_called()


def test_creating_awacs_balancer(cache, zk_storage, ctx):
    create_ns_pb(cache, zk_storage)
    balancer = create_balancer(cache, zk_storage)
    assert p.CreatingAwacsBalancer(balancer).process(ctx).name == 'CREATING_BALANCER_BACKEND'
    balancer_pb = zk_storage.get_balancer(NS_ID, BALANCER_ID)
    assert balancer_pb.spec.yandex_balancer.mode == balancer_pb.spec.yandex_balancer.EASY_MODE
    assert len(balancer_pb.meta.indices) == 1
    index_pb = balancer_pb.meta.indices[0]
    assert index_pb.ctime == balancer_pb.meta.mtime
    assert index_pb.id == balancer_pb.meta.version


def test_creating_awacs_balancer_from_existing_spec(cache, zk_storage, ctx):
    create_ns_pb(cache, zk_storage)
    balancer = create_balancer(cache, zk_storage, use_existing_spec=True)
    existing_b_pb = create_existing_balancer(zk_storage)
    assert p.CreatingAwacsBalancer(balancer).process(ctx).name == 'CREATING_BALANCER_BACKEND'
    balancer_pb = zk_storage.get_balancer(NS_ID, BALANCER_ID)
    assert balancer_pb.spec.yandex_balancer.mode == existing_b_pb.spec.yandex_balancer.mode
    assert balancer_pb.spec.yandex_balancer.yaml == existing_b_pb.spec.yandex_balancer.yaml
    assert balancer_pb.spec.config_transport.nanny_static_file.service_id == 'rtc_balancer_balancer-id_sas'
    assert len(balancer_pb.meta.indices) == 1
    index_pb = balancer_pb.meta.indices[0]
    assert index_pb.ctime == balancer_pb.meta.mtime
    assert index_pb.id == balancer_pb.meta.version


def test_creating_balancer_backend(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage)
    assert p.CreatingBalancerBackend(balancer).process(ctx).name == 'VALIDATING_AWACS_BALANCER'
    backend_pb = zk_storage.get_backend(NS_ID, BALANCER_ID)
    assert backend_pb.meta.is_system.value
    assert backend_pb.spec.selector.type == model_pb2.BackendSelector.BALANCERS
    assert len(backend_pb.spec.selector.balancers) == 1
    assert backend_pb.spec.selector.balancers[0].id == BALANCER_ID


def test_creating_balancer_backend_without_activate(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage, activate=False)
    assert p.CreatingBalancerBackend(balancer).process(ctx).name == 'FINISH'
    backend_pb = zk_storage.get_backend(NS_ID, BALANCER_ID)
    assert backend_pb.meta.is_system.value
    assert backend_pb.spec.selector.type == model_pb2.BackendSelector.BALANCERS
    assert len(backend_pb.spec.selector.balancers) == 1
    assert backend_pb.spec.selector.balancers[0].id == BALANCER_ID


def test_validating_awacs_balancer(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage)
    with pytest.raises(NotFoundError):
        p.ValidatingAwacsBalancer(balancer).process(ctx)

    state_pb = model_pb2.BalancerState()
    state_pb.namespace_id = NS_ID
    state_pb.balancer_id = BALANCER_ID
    zk_storage.create_balancer_state(NS_ID, BALANCER_ID, state_pb)
    wait_until_passes(lambda: cache.must_get_balancer_state(NS_ID, BALANCER_ID))
    assert p.ValidatingAwacsBalancer(balancer).process(ctx).name == 'VALIDATING_AWACS_BALANCER'

    for state_pb in zk_storage.update_balancer_state(NS_ID, BALANCER_ID):
        status = state_pb.balancer.statuses.add()
        status.validated.status = 'True'
        status.revision_id = balancer.pb.meta.version
    wait_until(lambda: cache.must_get_balancer_state(NS_ID, BALANCER_ID).balancer.statuses[0].validated.status)
    assert p.ValidatingAwacsBalancer(balancer).process(ctx).name == 'ACTIVATING_AWACS_BALANCER'


def test_activating_awacs_balancer(cache, zk_storage, ctx, nanny_client):
    nanny_client.set_snapshot_state = mock.Mock()

    balancer = create_balancer(cache, zk_storage)
    with pytest.raises(NotFoundError):
        p.ActivatingAwacsBalancer(balancer).process(ctx)

    state_pb = model_pb2.BalancerState()
    state_pb.namespace_id = NS_ID
    state_pb.balancer_id = BALANCER_ID
    zk_storage.create_balancer_state(NS_ID, BALANCER_ID, state_pb)
    wait_until_passes(lambda: cache.must_get_balancer_state(NS_ID, BALANCER_ID))
    assert p.ActivatingAwacsBalancer(balancer).process(ctx).name == 'ACTIVATING_AWACS_BALANCER'

    for state_pb in zk_storage.update_balancer_state(NS_ID, BALANCER_ID):
        status = state_pb.balancer.statuses.add()
        status.in_progress.meta.nanny_static_file.snapshots.add(service_id='rtc_balancer_balancer-id_sas',
                                                                snapshot_id='abc')
    assert p.ActivatingAwacsBalancer(balancer).process(ctx).name == 'FINISH'
    nanny_client.set_snapshot_state.assert_called_once_with(
        comment='Activating L7 balancer',
        recipe='common',
        service_id=u'rtc_balancer_balancer-id_sas',
        snapshot_id=u'abc',
        state='ACTIVE')


def test_deallocating_yp_lite_resources_noop(cache, zk_storage, ctx, yp_lite_client):
    yp_lite_client.get_pod_set = mock.Mock(side_effect=nanny_rpc_client.exceptions.NotFoundError('test'))
    yp_lite_client.remove_pod_set = mock.Mock()
    balancer = create_balancer(cache, zk_storage)
    assert p.DeallocatingYpLiteResources(balancer).process(ctx).name == 'CANCELLED'
    yp_lite_client.remove_pod_set.assert_not_called()


def test_deallocating_yp_lite_resources(cache, zk_storage, ctx, yp_lite_client):
    yp_lite_client.remove_pod_set = mock.Mock()
    balancer = create_balancer(cache, zk_storage, skip_pre_allocation_id=True)
    assert p.DeallocatingYpLiteResources(balancer).process(ctx).name == 'CANCELLED'
    yp_lite_client.remove_pod_set.assert_not_called()

    balancer.context['pre_allocation_id'] = 'test'
    assert p.DeallocatingYpLiteResources(balancer).process(ctx).name == 'CANCELLED'
    yp_lite_client.remove_pod_set.assert_called_once()
    req_pb, _ = yp_lite_client.remove_pod_set.call_args
    req_pb = req_pb[0]
    assert req_pb.service_id == 'rtc_balancer_balancer-id_sas'
    assert req_pb.cluster == 'SAS'
    assert req_pb.pre_allocation_id == 'test'
