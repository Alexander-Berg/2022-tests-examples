# coding: utf-8
import copy

import inject
import mock
import pytest
import semantic_version
import six
import yaml
from awacs.wrappers import rps_limiter_settings
from datetime import datetime, timedelta
from sepelib.core import config as appconfig
from six.moves import range

import awtest
from awacs.lib.order_processor.model import OverallStatus
from awacs.lib.rpc import exceptions
from awacs.lib.yasm_client import IYasmClient
from awacs.lib.ypliterpcclient import IYpLiteRpcClient
from awacs.lib.strutils import flatten_full_id2
from awacs.model import validation
from awacs.model.errors import ValidationError
from awacs.model.balancer.ctl import BalancerCtl as BaseBalancerCtl
from awacs.model.balancer.generator import get_included_full_backend_ids_from_holder
from awacs.web import balancer_service, namespace_service, upstream_service, component_service
from awacs.web.validation import balancer
from awacs.web.validation.balancer import validate_balancer_container_spec, validate_location_balancer_id
from awacs.wrappers.l7macro import VALID_VERSIONS
from awacs.wrappers.base import Holder
from awtest.api import call, create_namespace_with_order_in_progress, NOT_ROOT_NS_OWNER_LOGIN, set_login_to_root_users, \
    fill_object_upper_limits, create_too_many_lines_yaml_config, create_too_many_chars_yaml_config, Api
from awtest.core import wait_until, wait_until_passes
from awtest.mocks.sandbox_client import MockSandboxClient
from awtest.mocks.yasm_client import MockYasmClient
from awtest.mocks.yp_lite_client import YpLiteMockClient
from infra.awacs.proto import api_pb2, model_pb2, modules_pb2
from infra.swatlib import sandbox
from infra.swatlib.rpc.exceptions import ForbiddenError


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client):
    def configure(b):
        b.bind(sandbox.ISandboxClient, MockSandboxClient())
        b.bind(IYpLiteRpcClient, YpLiteMockClient())
        b.bind(IYasmClient, MockYasmClient(''))
        binder_with_nanny_client(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


BACKEND_ID = 'backend'

_YAML_CONFIG_1 = {
    'main': {
        'addrs': [
            {'ip': '127.0.0.1', 'port': 80},
        ],
        'admin_addrs': [
            {'ip': '127.0.0.2', 'port': 80},
        ],
        'maxconn': 4000,
        'workers': 1,
        'buffer': 65536,
        'log': '/usr/local/www/logs/current-childs_log-balancer-16020',
        'events': {'stats': 'report'},
        'ipdispatch': {
            'include_upstreams': {
                'type': 'ALL',
            }
        },
    }
}
YAML_CONFIG_1 = yaml.dump(_YAML_CONFIG_1)

_YAML_CONFIG_2 = copy.deepcopy(_YAML_CONFIG_1)
_YAML_CONFIG_2['main']['maxconn'] = 5000
YAML_CONFIG_2 = yaml.dump(_YAML_CONFIG_2)

_YAML_CONFIG_3 = {
    'instance_macro': {
        'sections': {
            'main': {
                'ips': ['127.0.0.1',],
                'ports': [80],
                'balancer2': {
                    'attempts': 1,
                    'rr': {},
                    'generated_proxy_backends': {
                        'proxy_options': {},
                        'include_backends': {
                            'type': 'BY_ID',
                            'ids': [BACKEND_ID],
                        },
                    },
                },
            },
        }
    },
}
YAML_CONFIG_3 = yaml.dump(_YAML_CONFIG_3)

L7_MACRO = '''
---
l7_macro:
  version: 0.0.1
  http: {}
'''

L7_MACRO_WITH_UNICODE = '''
---
l7_macro:
  version: 0.0.1
  http: {}
  headers:
    - create: {
      target: привет, func: realip
    }
'''

L7_MACRO_WITH_UNICODE_COMMENT = '''
---
l7_macro:
  version: 0.0.1
  http: {}
  headers:
    - create: {
      target: hello, func: realip # коммент в юникоде
    }
'''

L7_MACRO_WITH_WEBAUTH = '''
---
l7_macro:
  version: 0.0.1
  http: {}
  webauth:
    mode: SIMPLE
    action: AUTHORIZE
'''

L7_MACRO_WITH_RPS_LIMITER = '''
---
l7_macro:
  version: 0.0.1
  health_check_reply: {{}}
  announce_check_reply:
    url_re: /ping
  rps_limiter:
    external:
      record_name: spec-promo-external
      installation: {}
  http: {{}}
'''

NS_ID = u'bbb'
LOGIN = u'login'
LOGIN2 = u'ferenets'
LOGIN3 = u'romanovich'
COMMENT = u'Creating very important balancer'
BALANCER_ID = u'balancer-1_sas'
BALANCER_ID_2 = u'balancer-2_sas'


class BalancerCtl(BaseBalancerCtl):
    TRANSPORT_PROCESSING_INTERVAL = 0.01
    TRANSPORT_POLLING_INTERVAL = 0.01
    TRANSPORT_MAIN_LOOP_FREQ = 0.01

    PROCESS_INTERVAL = 9999
    FORCE_PROCESS_INTERVAL = 9999
    EVENTS_QUEUE_GET_TIMEOUT = 9999
    SLEEP_AFTER_EXCEPTION_TIMEOUT = 0.01

    def _process_empty_queue(self, *_, **__):
        pass


def create_balancer_req(balancer_id=BALANCER_ID, nanny_service_id=u'prod_balancer',
                        mode=model_pb2.YandexBalancerSpec.FULL_MODE):
    req_pb = api_pb2.CreateBalancerRequest()
    req_pb.meta.id = balancer_id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.comment = COMMENT
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.location.type = req_pb.meta.location.YP_CLUSTER
    req_pb.meta.location.yp_cluster = u'SAS'
    req_pb.meta.category = u'users/romanovich'
    req_pb.spec.config_transport.nanny_static_file.service_id = nanny_service_id
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.mode = mode
    req_pb.spec.yandex_balancer.yaml = L7_MACRO if mode == model_pb2.YandexBalancerSpec.EASY_MODE else YAML_CONFIG_1
    return req_pb


def create_balancer_order_req():
    req_pb = create_balancer_req()
    req_pb.ClearField('spec')
    req_pb.meta.location.yp_cluster = u'SAS'
    req_pb.order.allocation_request.nanny_service_id_slug = u'xxx'
    req_pb.order.allocation_request.location = u'SAS'
    req_pb.order.allocation_request.network_macro = u'_SEARCHSAND_'
    req_pb.order.allocation_request.type = req_pb.order.allocation_request.PRESET
    req_pb.order.allocation_request.preset.type = req_pb.order.allocation_request.preset.MICRO
    req_pb.order.allocation_request.preset.instances_count = 1
    req_pb.order.abc_service_id = 999
    return req_pb


def make_header_pb(action, target=None, func=None, value=None, keep_existing=False,
                   target_re=None, service_name=None):
    if func and value:
        raise RuntimeError(
            'You can pass either `func` or `value` to `make_header`, but not both')

    header_action_pb = modules_pb2.L7Macro.HeaderAction()
    create_pb = getattr(header_action_pb, action)
    if target:
        create_pb.target = target
        create_pb.keep_existing = keep_existing
    if func:
        create_pb.func = func
    if value:
        create_pb.value = value
    if target_re:
        create_pb.target_re = target_re
    if service_name:
        create_pb.service_name = service_name

    if not (all([target, func, value])):
        create_pb.SetInParent()
    return header_action_pb


def create_update_l7_macro_to_03x_req(version, trust_x_forwarded_for_y=False):
    req_pb = api_pb2.UpdateBalancerL7MacroTo03xRequest()
    req_pb.namespace_id = NS_ID
    req_pb.id = BALANCER_ID
    req_pb.version = version
    req_pb.trust_x_forwarded_for_y = trust_x_forwarded_for_y
    return req_pb


def create_balancer_with_l7_macro(cache, namespace_id, b_id, version, header_pbs,
                                  use_upstream_handler=False):
    from awacs.model import util
    appconfig.set_value('run.root_users', [util.NANNY_ROBOT_LOGIN])
    req_pb = api_pb2.CreateBalancerRequest()

    req_pb.meta.id = b_id
    req_pb.meta.namespace_id = namespace_id
    req_pb.meta.category = u'users/romanovich'
    req_pb.meta.location.type = req_pb.meta.location.YP_CLUSTER
    req_pb.meta.location.yp_cluster = u'SAS'
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF

    req_pb.spec.config_transport.nanny_static_file.service_id = 'prod_balancer'
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.mode = model_pb2.YandexBalancerSpec.EASY_MODE
    l7_macro_pb = req_pb.spec.yandex_balancer.config.l7_macro
    l7_macro_pb.version = version
    l7_macro_pb.http.SetInParent()
    l7_macro_pb.include_domains.SetInParent()
    l7_macro_pb.health_check_reply.SetInParent()
    l7_macro_pb.announce_check_reply.url_re = u'/ping'
    if use_upstream_handler:
        l7_macro_pb.announce_check_reply.use_upstream_handler = use_upstream_handler
    l7_macro_pb.headers.extend(header_pbs)

    call(balancer_service.create_balancer, req_pb, util.NANNY_ROBOT_LOGIN)
    wait_until_passes(lambda: cache.must_get_balancer(namespace_id, b_id))


def get_balancer(namespace_id, balancer_id):
    balancer_req_pb = api_pb2.GetBalancerRequest()
    balancer_req_pb.id = balancer_id
    balancer_req_pb.namespace_id = namespace_id
    balancer_req_pb.consistency = api_pb2.STRONG
    return call(balancer_service.get_balancer, balancer_req_pb, LOGIN)


def get_balancer_version(namespace_id, balancer_id):
    resp_pb = get_balancer(namespace_id, balancer_id)
    return resp_pb.balancer.meta.version


def get_latest_l7_macro_03x_version():
    return max([x for x in VALID_VERSIONS if x < semantic_version.Version('0.4.0')])


def check_rev_indices(balancer_pb):
    assert len(balancer_pb.meta.indices) > 0
    assert balancer_pb.meta.indices[-1].id == balancer_pb.meta.version
    assert balancer_pb.meta.indices[-1].ctime == balancer_pb.meta.mtime
    holder = Holder(balancer_pb.spec.yandex_balancer.config)
    included_backend_ids = get_included_full_backend_ids_from_holder(balancer_pb.meta.namespace_id, holder)
    included_backend_ids = sorted(map(flatten_full_id2, included_backend_ids))
    assert sorted(balancer_pb.meta.indices[-1].included_backend_ids) == included_backend_ids


@pytest.mark.parametrize('init_version, header_pbs, expected_remaining_header_pbs, trust_xffy', [
    ('0.2.0', [make_header_pb('create', 'X-Forwarded-For-Y', func='realip')], [], False),
    (
        '0.2.0', [
            make_header_pb('laas'),
            make_header_pb('uaas', service_name='dude'),
            make_header_pb('delete', target_re='hello'),
            make_header_pb('log', target_re='hello_again'),
        ], [
            make_header_pb('laas'),
            make_header_pb('uaas', service_name='dude'),
            make_header_pb('delete', target_re='hello'),
            make_header_pb('log', target_re='hello_again'),
        ], False
    ),
    ('0.2.0', [make_header_pb('create', 'X-Forwarded-For-Y', func='realip', keep_existing=True)], [], True),
    ('0.2.0', [], [], False),
    ('0.2.0', [], [], True),
    (
        '0.2.0', [
            make_header_pb('create', 'X-Forwarded-For-Y', func='realip', keep_existing=True),
            make_header_pb('create', 'X-Hello', value='world'),
        ], [
            make_header_pb('create', 'X-Hello', value='world'),
        ], True
    ),
])
def test_update_l7_030_ui_call_positive(create_default_namespace, cache,
                                        init_version, header_pbs, expected_remaining_header_pbs, trust_xffy):
    create_default_namespace(NS_ID)
    create_balancer_with_l7_macro(cache, NS_ID, BALANCER_ID, init_version, header_pbs)
    version = get_balancer_version(NS_ID, BALANCER_ID)
    req_pb = create_update_l7_macro_to_03x_req(version, trust_xffy)
    call(balancer_service.update_l7macro_to_03x, req_pb, LOGIN)
    balancer_pb = get_balancer(NS_ID, BALANCER_ID)
    l7_macro_pb = balancer_pb.balancer.spec.yandex_balancer.config.l7_macro
    version_to_update_to = get_latest_l7_macro_03x_version()
    assert semantic_version.Version(l7_macro_pb.version) == version_to_update_to
    assert l7_macro_pb.core.trust_x_forwarded_for_y == trust_xffy
    assert list(l7_macro_pb.headers) == expected_remaining_header_pbs
    assert not l7_macro_pb.announce_check_reply.compat.disable_graceful_shutdown
    check_rev_indices(balancer_pb.balancer)


@pytest.mark.usefixtures('enable_auth')
@pytest.mark.parametrize('login, raises', [(LOGIN, False), (LOGIN2, True)])
def test_update_l7_030_ui_call_auth(create_default_namespace, zk_storage, cache, login, raises):
    create_default_namespace(NS_ID)
    create_balancer_with_l7_macro(cache, NS_ID, BALANCER_ID, '0.2.0', [])
    version = get_balancer_version(NS_ID, BALANCER_ID)
    req_pb = create_update_l7_macro_to_03x_req(version, True)
    if raises:
        with pytest.raises(ForbiddenError):
            call(balancer_service.update_l7macro_to_03x, req_pb, login)
    else:
        balancer_pb = call(balancer_service.update_l7macro_to_03x, req_pb, login)
        check_rev_indices(balancer_pb.balancer)


@pytest.mark.parametrize('init_version, header_pbs, trust_xffy', [
    ('0.3.0', [], True),
    (
        '0.2.0', [
            make_header_pb('create', 'X-Header', value='hello'),
            make_header_pb('create', 'X-Forwarded-For-Y', func='realip'),
        ],
        True
    ),
    (
        '0.2.0', [make_header_pb('create', 'X-Forwarded-For-Y', value='hello')], True
    ),
    (
        '0.2.0',
        [make_header_pb('create', 'X-Forwarded-For-Y', func='realip', keep_existing=True)],
        False
    ),
    (
        '0.2.0',
        [make_header_pb('create', 'X-Forwarded-For-Y', func='realip')],
        True
    ),
])
def test_update_l7_030_ui_call_negative(create_default_namespace, cache,
                                        init_version, header_pbs, trust_xffy):
    create_default_namespace(NS_ID)
    create_balancer_with_l7_macro(cache, NS_ID, BALANCER_ID, init_version, header_pbs)
    version = get_balancer_version(NS_ID, BALANCER_ID)
    req_pb = create_update_l7_macro_to_03x_req(version, trust_xffy)

    with pytest.raises(exceptions.BadRequestError):
        call(balancer_service.update_l7macro_to_03x, req_pb, LOGIN)

    updated_balancer_pb = get_balancer(NS_ID, BALANCER_ID)
    assert updated_balancer_pb.balancer.spec.yandex_balancer.config.l7_macro.version == init_version


@pytest.mark.parametrize('header_pbs, result', [
    (
        [
            make_header_pb('create', 'X-Forwarded-For-Y', func='realip'),
            make_header_pb('create', 'X-Mas', value='happy'),
        ],
        api_pb2.GetBalancerXFFYBehaviorResponse.OVERRIDE
    ),
    (
        [make_header_pb('create', 'X-Forwarded-For-Y', func='realip', keep_existing=True)],
        api_pb2.GetBalancerXFFYBehaviorResponse.TRUST
    ),
    (
        [
            make_header_pb('laas'),
            make_header_pb('uaas', service_name='dude'),
            make_header_pb('delete', target_re='X-Forwarded-For-Y'),
            make_header_pb('log', target_re='X-Forwarded-For-Y'),
        ],
        api_pb2.GetBalancerXFFYBehaviorResponse.NONE
    ),
    (
        [
            make_header_pb('laas'),
            make_header_pb('uaas', service_name='dude'),
            make_header_pb('create', target='X-Forwarded-For-Y', func='realip'),
            make_header_pb('log', target_re='X-Forwarded-For-Y'),
        ],
        api_pb2.GetBalancerXFFYBehaviorResponse.UNKNOWN
    ),
])
def test_get_balancer_xffy_behavior(create_default_namespace, cache,
                                    header_pbs, result):
    create_default_namespace(NS_ID)
    create_balancer_with_l7_macro(cache, NS_ID, BALANCER_ID, '0.2.0', header_pbs)

    req_pb = api_pb2.GetBalancerXFFYBehaviorRequest()
    req_pb.namespace_id = NS_ID
    req_pb.id = BALANCER_ID

    resp_pb = call(balancer_service.get_balancer_xffy_behavior, req_pb, LOGIN)

    assert resp_pb.behavior == result


def test_graceful_shutdown_disabling_upon_03x_updating(create_default_namespace, cache):
    create_default_namespace(NS_ID)
    create_balancer_with_l7_macro(cache, NS_ID, BALANCER_ID, u'0.2.5', [], use_upstream_handler=True)

    version = get_balancer_version(NS_ID, BALANCER_ID)
    req_pb = create_update_l7_macro_to_03x_req(version, trust_x_forwarded_for_y=False)
    balancer_pb = call(balancer_service.update_l7macro_to_03x, req_pb, LOGIN)

    l7_macro_pb = balancer_pb.balancer.spec.yandex_balancer.config.l7_macro
    version_to_update_to = get_latest_l7_macro_03x_version()
    assert semantic_version.Version(l7_macro_pb.version) == version_to_update_to
    assert l7_macro_pb.announce_check_reply.compat.disable_graceful_shutdown
    check_rev_indices(balancer_pb.balancer)


def test_forbidden_operations_during_namespace_order(zk_storage, cache, enable_auth):
    # forbid creation, removal, cancelling (including for operations), and transport pause/unpause
    create_namespace_with_order_in_progress(zk_storage, cache, NS_ID)
    req_pb = create_balancer_req()
    req_pb.spec.yandex_balancer.yaml = YAML_CONFIG_1
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(balancer_service.create_balancer, req_pb, LOGIN)

    b_pb = model_pb2.Balancer(meta=req_pb.meta, spec=req_pb.spec)
    b_pb.meta.version = 'xxx'
    b_pb.spec.config_transport.type = model_pb2.NANNY_STATIC_FILE
    b_pb.spec.config_transport.nanny_static_file.service_id = 's'
    zk_storage.create_balancer(NS_ID, BALANCER_ID, b_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, BALANCER_ID))

    req_pb = api_pb2.UpdateBalancerRequest(meta=b_pb.meta)
    req_pb.meta.transport_paused.value = True
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(balancer_service.update_balancer, req_pb, LOGIN)

    req_pb = api_pb2.UpdateBalancerRequest(meta=b_pb.meta, spec=b_pb.spec)
    req_pb.spec.yandex_balancer.yaml = L7_MACRO
    req_pb.spec.yandex_balancer.mode = req_pb.spec.yandex_balancer.EASY_MODE
    b_pb = call(balancer_service.update_balancer, req_pb, LOGIN).balancer

    req_pb = api_pb2.RemoveBalancerRequest(namespace_id=NS_ID, id=BALANCER_ID, version=b_pb.meta.version)
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(balancer_service.remove_balancer, req_pb, LOGIN)

    cert_id = 'anonymous.in.yandex-team.ru'
    cert_pb = model_pb2.Certificate()
    cert_pb.meta.id = cert_id
    cert_pb.meta.namespace_id = NS_ID
    cert_pb.spec.fields.subject_common_name = 'anonymous.in.yandex-team.ru'
    zk_storage.create_cert(namespace_id=NS_ID, cert_id=cert_id, cert_pb=cert_pb)
    wait_until_passes(lambda: cache.must_get_cert(NS_ID, cert_id))
    req_pb = api_pb2.CreateBalancerOperationRequest()
    req_pb.meta.id = BALANCER_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.order.create_system_backend.SetInParent()
    with pytest.raises(exceptions.ForbiddenError, match='Cannot do this while namespace order is in progress'):
        call(balancer_service.create_balancer_operation, req_pb, LOGIN)


@pytest.mark.parametrize('max_count,custom_count', [
    (0, None),
    (1, None),
    (10, None),
    (5, 10),
    (10, 5),
])
def test_namespace_objects_total_limit(max_count, custom_count, create_default_namespace):
    create_default_namespace(NS_ID)
    set_login_to_root_users(LOGIN)
    appconfig.set_value('common_objects_limits.balancer', max_count)
    if custom_count is not None:
        fill_object_upper_limits(NS_ID, 'balancer', custom_count, LOGIN)
    count = custom_count or max_count

    req_pb = create_balancer_req()
    for _ in range(count):
        call(balancer_service.create_balancer, req_pb, LOGIN)
        req_pb.meta.id = 'a' + req_pb.meta.id
        service_id = 'a' + req_pb.spec.config_transport.nanny_static_file.service_id
        req_pb.spec.config_transport.nanny_static_file.service_id = service_id

    def check():
        list_req_pb = api_pb2.ListBalancersRequest(namespace_id=NS_ID)
        assert call(balancer_service.list_balancers, list_req_pb, LOGIN).total == count

    wait_until_passes(check)

    with pytest.raises(exceptions.BadRequestError,
                       match='Exceeded limit of balancers in the namespace: {}'.format(count)):
        call(balancer_service.create_balancer, req_pb, LOGIN)


def test_validate_balancer_container_spec():
    spec_pb = model_pb2.BalancerContainerSpec()
    tunnel_pb = spec_pb.outbound_tunnels.add()
    tunnel_pb.mode = tunnel_pb.IPIP6
    tunnel_pb.id = 'test'
    tunnel_pb.rules.add(from_ip='8.8.8.8')
    tunnel_pb.remote_ip = '1.1.1.1'
    with pytest.raises(exceptions.BadRequestError) as e:
        validate_balancer_container_spec(spec_pb, 'test')
    assert str(e.value) == '"test.outbound_tunnels[0].remote_ip": is not a valid IPv6 address'

    tunnel_pb.remote_ip = '2a02:6b8:a::a'
    with pytest.raises(exceptions.BadRequestError) as e:
        validate_balancer_container_spec(spec_pb, 'test')
    assert str(e.value) == '"test.outbound_tunnels[0].rules[0].from_ip": must be in 5.45.202.0/24'

    tunnel_pb.rules[0].from_ip = '5.45.202.8'
    validate_balancer_container_spec(spec_pb, 'test')

    vip_pb = spec_pb.virtual_ips.add()
    with pytest.raises(exceptions.BadRequestError) as e:
        validate_balancer_container_spec(spec_pb, 'test')
    assert str(e.value) == '"test.inbound_tunnels": must be configured to use virtual_ips'

    inbound_tunnel_pb = spec_pb.inbound_tunnels.add()
    with pytest.raises(exceptions.BadRequestError) as e:
        validate_balancer_container_spec(spec_pb, 'test')
    assert str(e.value) == '"test.inbound_tunnels[0]": must not be empty'

    inbound_tunnel_pb.fallback_ip6.SetInParent()
    with pytest.raises(exceptions.BadRequestError) as e:
        validate_balancer_container_spec(spec_pb, 'test')
    assert str(e.value) == '"test.virtual_ips[0].ip": must be present'

    vip_pb.ip = '0.0.0.abc'
    with pytest.raises(exceptions.BadRequestError) as e:
        validate_balancer_container_spec(spec_pb, 'test')
    assert str(e.value) == '"test.virtual_ips[0].description": must be present'

    vip_pb.description = 'ttt'
    with pytest.raises(exceptions.BadRequestError) as e:
        validate_balancer_container_spec(spec_pb, 'test')
    assert str(e.value) == '"test.virtual_ips[0].ip": "0.0.0.abc" is not a valid IP address'

    vip_pb.ip = '127.0.0.1'
    vip_pb_2 = spec_pb.virtual_ips.add()
    vip_pb_2.ip = '127.0.0.1'
    vip_pb_2.description = 'ttt'
    with pytest.raises(exceptions.BadRequestError) as e:
        validate_balancer_container_spec(spec_pb, 'test')
    assert str(e.value) == '"test.virtual_ips[1]": duplicate virtual IP: "127.0.0.1"'

    vip_pb_2.ip = '2a02:6b8:c08:a40c:0:696:7ef7'
    with pytest.raises(exceptions.BadRequestError) as e:
        validate_balancer_container_spec(spec_pb, 'test')
    assert str(e.value) == '"test.virtual_ips[1].ip": "2a02:6b8:c08:a40c:0:696:7ef7" is not a valid IP address'

    vip_pb_2.ip = '2a02:6b8:0:3400:0:71d:0:14c'
    validate_balancer_container_spec(spec_pb, 'test')

    inbound_tunnel_pb_2 = spec_pb.inbound_tunnels.add()
    inbound_tunnel_pb_2.fallback_ip6.SetInParent()
    with pytest.raises(exceptions.BadRequestError) as e:
        validate_balancer_container_spec(spec_pb, 'test')
    assert str(e.value) == '"test.inbound_tunnels[1]": duplicate tunnel type "fallback_ip6"'


def test_create_with_unicode(mongo_storage, create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    req_pb = api_pb2.CreateBalancerRequest()
    balancer_id = '{}_sas'.format(NS_ID)
    req_pb.meta.id = balancer_id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.location.type = req_pb.meta.location.YP_CLUSTER
    req_pb.meta.location.yp_cluster = 'SAS'
    req_pb.spec.config_transport.nanny_static_file.service_id = 'gencfg_balancer'
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.mode = model_pb2.YandexBalancerSpec.EASY_MODE
    req_pb.spec.yandex_balancer.yaml = L7_MACRO_WITH_UNICODE
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    assert str(e.value) == 'spec.yandex_balancer.yaml: line 8, column 15: Non-ascii characters present'

    req_pb.spec.yandex_balancer.yaml = L7_MACRO_WITH_UNICODE_COMMENT
    call(balancer_service.create_balancer, req_pb, LOGIN)


def test_create_and_get(mongo_storage, create_default_namespace):
    set_login_to_root_users(LOGIN)

    create_default_namespace(NS_ID)
    req_pb = create_balancer_req()
    req_pb.spec.yandex_balancer.yaml = yaml.dump({})
    with pytest.raises(exceptions.BadRequestError) as e:
        with mock.patch.object(balancer_service, 'validate_and_parse_yaml_balancer_config',
                               side_effect=ValidationError('BAD')):
            call(balancer_service.create_balancer, req_pb, LOGIN)
    assert str(e.value) == 'BAD'

    req_pb.spec.yandex_balancer.yaml = create_too_many_lines_yaml_config(larger_than=2000)
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    assert six.text_type(e.value) == (u'"spec.yandex_balancer.yaml" contains too many lines '
                                      u'(2002, allowed limit is 1000)')

    req_pb.spec.yandex_balancer.yaml = create_too_many_chars_yaml_config(larger_than=80000)
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    assert six.text_type(e.value) == (u'"spec.yandex_balancer.yaml" contains too many characters '
                                      u'(80075, allowed limit is 80000)')

    req_pb.spec.yandex_balancer.yaml = YAML_CONFIG_1
    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer
    assert b_pb.meta.id == BALANCER_ID
    assert b_pb.meta.namespace_id == NS_ID
    assert b_pb.meta.author == LOGIN
    assert b_pb.meta.comment == COMMENT
    assert b_pb.spec.config_transport.nanny_static_file.service_id == 'prod_balancer'
    assert b_pb.meta.auth.staff.owners.logins == [LOGIN]

    namespace_id = 'random'
    create_default_namespace(namespace_id)
    req_pb.meta.id = BALANCER_ID.replace('_', '-')
    req_pb.meta.namespace_id = namespace_id
    with pytest.raises(exceptions.ConflictError,
                       match='New balancer and balancer "{}" from namespace "{}" would have the same identifiers '
                             'after replacing "_" with "-", which is forbidden.'.format(BALANCER_ID, NS_ID)):
        call(balancer_service.create_balancer, req_pb, LOGIN)

    req_pb = api_pb2.GetBalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)
    resp_pb = call(balancer_service.get_balancer, req_pb, LOGIN)
    balancer_pb = resp_pb.balancer
    spec_pb = balancer_pb.spec
    assert spec_pb.HasField('yandex_balancer')
    assert spec_pb.yandex_balancer.HasField('config')
    assert spec_pb.yandex_balancer.config.HasField('main')
    main_pb = spec_pb.yandex_balancer.config.main
    assert len(main_pb.addrs) == 1
    assert len(main_pb.admin_addrs) == 1
    assert main_pb.nested.HasField('ipdispatch')
    assert main_pb.nested.ipdispatch.include_upstreams
    assert len(balancer_pb.status.revisions) == 1
    r_status = balancer_pb.status.revisions[0]
    assert r_status.id == balancer_pb.meta.version

    rev_pb = mongo_storage.get_balancer_rev(r_status.id)
    assert rev_pb.meta.id == r_status.id
    assert rev_pb.spec == b_pb.spec


def test_create_conflict(create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    req_pb = create_balancer_req()

    call(balancer_service.create_balancer, req_pb, LOGIN)
    with pytest.raises(exceptions.ConflictError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    assert e.match(u'Balancer "{}" already exists in namespace "{}"'.format(BALANCER_ID, NS_ID))

    req_pb.meta.namespace_id = NS_ID + '1'
    create_default_namespace(req_pb.meta.namespace_id)
    with pytest.raises(exceptions.ConflictError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    assert e.match(u'Balancer "{}" already exists in different namespace "{}"'.format(BALANCER_ID, NS_ID))

    req_pb.meta.id = BALANCER_ID + '1_sas'
    req_pb.meta.namespace_id = NS_ID + '1'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    assert e.match(
        u'Nanny service "prod_balancer" is already used in balancer "{}:{}"'.format(NS_ID, BALANCER_ID))


def test_create_order(create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    req_pb = create_balancer_order_req()
    req_pb.order.ClearField('abc_service_id')
    with pytest.raises(exceptions.BadRequestError, match='"order.abc_service_id" must be set'):
        call(balancer_service.create_balancer, req_pb, LOGIN)
    req_pb.order.abc_service_id = 999
    with mock.patch.object(balancer_service, 'validate_nanny_service_nonexistence'):
        b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer
    assert b_pb.meta.id == BALANCER_ID
    assert b_pb.meta.namespace_id == NS_ID
    assert b_pb.meta.author == LOGIN
    assert b_pb.meta.comment == COMMENT
    assert b_pb.meta.auth.staff.owners.logins == [LOGIN]
    assert b_pb.order.content.allocation_request.nanny_service_id_slug == 'xxx'
    assert b_pb.order.content.allocation_request.location == 'SAS'
    assert b_pb.order.content.allocation_request.network_macro == '_SEARCHSAND_'
    assert b_pb.order.content.allocation_request.type == req_pb.order.allocation_request.PRESET
    assert b_pb.order.content.allocation_request.preset.type == req_pb.order.allocation_request.preset.MICRO
    assert b_pb.order.content.allocation_request.preset.instances_count == 1
    assert b_pb.order.content.activate_balancer
    assert b_pb.spec.incomplete


def test_create_order_with_copy_spec(zk_storage, cache, create_default_namespace):
    create_default_namespace(NS_ID)
    req_pb = create_balancer_order_req()
    req_pb.order.copy_spec_from_balancer_id = 'xxx'
    with pytest.raises(exceptions.NotFoundError, match='Balancer "bbb:xxx" not found'):
        call(balancer_service.create_balancer, req_pb, LOGIN)

    b_pb = model_pb2.Balancer()
    b_pb.meta.id = 'xxx'
    b_pb.meta.namespace_id = NS_ID
    b_pb.spec.incomplete = True
    zk_storage.create_balancer(NS_ID, 'xxx', b_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, 'xxx'))

    with pytest.raises(exceptions.BadRequestError, match='"order.copy_spec_from_balancer_id": '
                                                         'cannot copy an incomplete or removed balancer "xxx"'):
        call(balancer_service.create_balancer, req_pb, LOGIN)

    for b_pb in zk_storage.update_balancer(NS_ID, 'xxx'):
        b_pb.spec.incomplete = False
        b_pb.spec.deleted = True
    assert wait_until(lambda: not cache.must_get_balancer(NS_ID, 'xxx').spec.incomplete)

    with pytest.raises(exceptions.BadRequestError, match='"order.copy_spec_from_balancer_id": '
                                                         'cannot copy an incomplete or removed balancer "xxx"'):
        call(balancer_service.create_balancer, req_pb, LOGIN)

    for b_pb in zk_storage.update_balancer(NS_ID, 'xxx'):
        b_pb.spec.deleted = False
    assert wait_until(lambda: not cache.must_get_balancer(NS_ID, 'xxx').spec.deleted)

    with mock.patch.object(balancer_service, 'validate_nanny_service_nonexistence'):
        call(balancer_service.create_balancer, req_pb, LOGIN)


def test_create_order_with_copy_service(zk_storage, cache, create_default_namespace):
    create_default_namespace(NS_ID)
    req_pb = create_balancer_order_req()
    req_pb.order.copy_nanny_service.SetInParent()
    with pytest.raises(exceptions.BadRequestError, match='"order.copy_nanny_service.balancer_id" must be set'):
        call(balancer_service.create_balancer, req_pb, LOGIN)
    req_pb.order.copy_nanny_service.balancer_id = 'yyy'
    with pytest.raises(exceptions.NotFoundError, match='Balancer "bbb:yyy" not found'):
        call(balancer_service.create_balancer, req_pb, LOGIN)

    b_pb = model_pb2.Balancer()
    b_pb.meta.id = 'yyy'
    b_pb.meta.location.type = b_pb.meta.location.YP_CLUSTER
    b_pb.meta.location.yp_cluster = 'SAS'
    b_pb.meta.namespace_id = NS_ID
    b_pb.spec.incomplete = True
    zk_storage.create_balancer(NS_ID, 'yyy', b_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, 'yyy'))

    with pytest.raises(exceptions.BadRequestError, match='"order.copy_nanny_service.balancer_id": '
                                                         'cannot copy an incomplete or removed balancer "yyy"'):
        call(balancer_service.create_balancer, req_pb, LOGIN)

    for b_pb in zk_storage.update_balancer(NS_ID, 'yyy'):
        b_pb.spec.incomplete = False
        b_pb.spec.deleted = True
    assert wait_until(lambda: not cache.must_get_balancer(NS_ID, 'yyy').spec.incomplete)

    with pytest.raises(exceptions.BadRequestError, match='"order.copy_nanny_service.balancer_id": '
                                                         'cannot copy an incomplete or removed balancer "yyy"'):
        call(balancer_service.create_balancer, req_pb, LOGIN)

    for b_pb in zk_storage.update_balancer(NS_ID, 'yyy'):
        b_pb.spec.deleted = False
    assert wait_until(lambda: not cache.must_get_balancer(NS_ID, 'yyy').spec.deleted)

    with mock.patch.object(balancer_service, 'validate_nanny_service_nonexistence'):
        with mock.patch.object(balancer, 'validate_nanny_service_for_copy'):
            with pytest.raises(exceptions.BadRequestError, match='Cannot create copied balancer in the same location '
                                                                 'as another balancer: "yyy"'):
                call(balancer_service.create_balancer, req_pb, LOGIN)

            for b_pb in zk_storage.update_balancer(NS_ID, 'yyy'):
                b_pb.meta.location.yp_cluster = 'MAN'
            assert wait_until(lambda: cache.must_get_balancer(NS_ID, 'yyy').meta.location.yp_cluster == 'MAN')

            call(balancer_service.create_balancer, req_pb, LOGIN)


def test_cancel_order(zk_storage, cache):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    zk_storage.create_namespace(NS_ID, ns_pb)
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.id = BALANCER_ID
    balancer_pb.meta.namespace_id = NS_ID
    balancer_pb.meta.auth.type = balancer_pb.meta.auth.STAFF
    balancer_pb.order.content.SetInParent()
    balancer_pb.order.progress.state.id = 'FINISHED'
    balancer_pb.order.status.status = 'FINISHED'
    zk_storage.create_balancer(NS_ID, BALANCER_ID, balancer_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, BALANCER_ID))

    req_pb = api_pb2.CancelBalancerOrderRequest(id=BALANCER_ID, namespace_id=NS_ID)
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel order that is not in progress'):
        call(balancer_service.cancel_balancer_order, req_pb, LOGIN)

    for balancer_pb in zk_storage.update_balancer(NS_ID, BALANCER_ID):
        balancer_pb.order.status.status = 'IN_PROGRESS'
    assert wait_until(lambda: cache.must_get_balancer(NS_ID, BALANCER_ID).order.status.status == 'IN_PROGRESS',
                      timeout=1)

    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel balancer order at this stage'):
        call(balancer_service.cancel_balancer_order, req_pb, LOGIN)

    for balancer_pb in zk_storage.update_balancer(NS_ID, BALANCER_ID):
        balancer_pb.order.progress.state.id = 'ALLOCATING_YP_LITE_RESOURCES'
    assert wait_until(lambda: (cache.must_get_balancer(NS_ID, BALANCER_ID).order.progress.state.id ==
                               'ALLOCATING_YP_LITE_RESOURCES'),
                      timeout=1)

    call(balancer_service.cancel_balancer_order, req_pb, LOGIN)
    assert zk_storage.must_get_balancer(NS_ID, BALANCER_ID).order.cancelled.value


def test_force_cancel_order(zk_storage, cache):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    zk_storage.create_namespace(NS_ID, ns_pb)
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.id = BALANCER_ID
    balancer_pb.meta.namespace_id = NS_ID
    balancer_pb.meta.auth.type = balancer_pb.meta.auth.STAFF
    balancer_pb.order.content.SetInParent()
    balancer_pb.order.progress.state.id = 'CREATING_NANNY_SERVICE'
    balancer_pb.order.status.status = 'IN_PROGRESS'
    zk_storage.create_balancer(NS_ID, BALANCER_ID, balancer_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, BALANCER_ID))

    req_pb = api_pb2.CancelBalancerOrderRequest(id=BALANCER_ID, namespace_id=NS_ID)
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel balancer order at this stage'):
        call(balancer_service.cancel_balancer_order, req_pb, LOGIN)

    req_pb.force = True
    with pytest.raises(exceptions.ForbiddenError, match='Only root users can force cancel balancer order'):
        call(balancer_service.cancel_balancer_order, req_pb, LOGIN)

    set_login_to_root_users(LOGIN)
    call(balancer_service.cancel_balancer_order, req_pb, LOGIN)
    assert wait_until(lambda: cache.must_get_balancer(NS_ID, BALANCER_ID).order.cancelled.value)


def test_force_cancel_operation(zk_storage, cache):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    ns_pb.meta.auth.type = ns_pb.meta.auth.STAFF
    zk_storage.create_namespace(NS_ID, ns_pb)
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.id = BALANCER_ID
    balancer_pb.meta.namespace_id = NS_ID
    balancer_pb.meta.auth.type = balancer_pb.meta.auth.STAFF
    zk_storage.create_balancer(NS_ID, BALANCER_ID, balancer_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, BALANCER_ID))
    balancer_op_pb = model_pb2.BalancerOperation()
    balancer_op_pb.meta.id = BALANCER_ID
    balancer_op_pb.meta.namespace_id = NS_ID
    balancer_op_pb.order.content.create_system_backend.SetInParent()
    balancer_op_pb.order.progress.state.id = 'CREATING_BALANCER'
    balancer_op_pb.order.status.status = 'IN_PROGRESS'
    zk_storage.create_balancer_operation(NS_ID, BALANCER_ID, balancer_op_pb)
    wait_until_passes(lambda: cache.must_get_balancer_operation(NS_ID, BALANCER_ID))

    req_pb = api_pb2.CancelBalancerOperationRequest(id=BALANCER_ID, namespace_id=NS_ID)
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel balancer operation at this stage'):
        call(balancer_service.cancel_balancer_operation, req_pb, LOGIN)

    req_pb.force = True
    with pytest.raises(exceptions.ForbiddenError, match='Only root users can force cancel balancer operation'):
        call(balancer_service.cancel_balancer_operation, req_pb, LOGIN)

    set_login_to_root_users(LOGIN)
    call(balancer_service.cancel_balancer_operation, req_pb, LOGIN)
    assert wait_until(lambda: cache.must_get_balancer_operation(NS_ID, BALANCER_ID).order.cancelled.value)


def test_change_abc_service_id(zk_storage, cache):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    zk_storage.create_namespace(NS_ID, ns_pb)
    balancer_pb = model_pb2.Balancer()
    balancer_pb.meta.id = BALANCER_ID
    balancer_pb.meta.namespace_id = NS_ID
    balancer_pb.meta.auth.type = balancer_pb.meta.auth.STAFF
    balancer_pb.order.content.SetInParent()
    balancer_pb.order.progress.state.id = 'FINISHED'
    balancer_pb.order.status.status = 'FINISHED'
    zk_storage.create_balancer(NS_ID, BALANCER_ID, balancer_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, BALANCER_ID))

    req_pb = api_pb2.ChangeBalancerOrderRequest(id=BALANCER_ID, namespace_id=NS_ID)
    req_pb.change_abc_service_id.abc_service_id = 888
    with pytest.raises(exceptions.BadRequestError, match='Cannot change balancer ABC service at this time'):
        call(balancer_service.change_balancer_order, req_pb, LOGIN)

    for balancer_pb in zk_storage.update_balancer(NS_ID, BALANCER_ID):
        balancer_pb.order.progress.state.id = 'ALLOCATING_YP_LITE_RESOURCES'
    assert wait_until(
        lambda: cache.must_get_balancer(NS_ID, BALANCER_ID).order.progress.state.id == 'ALLOCATING_YP_LITE_RESOURCES',
        timeout=1)

    req_pb = api_pb2.ChangeBalancerOrderRequest(id=BALANCER_ID, namespace_id=NS_ID)
    with pytest.raises(exceptions.BadRequestError, match='Supported order change operations: "change_abc_service_id"'):
        call(balancer_service.change_balancer_order, req_pb, LOGIN)

    req_pb.change_abc_service_id.SetInParent()
    with pytest.raises(exceptions.BadRequestError, match='"change_abc_service_id.abc_service_id" must be set'):
        call(balancer_service.change_balancer_order, req_pb, LOGIN)

    req_pb.change_abc_service_id.abc_service_id = 888
    call(balancer_service.change_balancer_order, req_pb, LOGIN)
    assert zk_storage.must_get_balancer(NS_ID, BALANCER_ID).order.content.abc_service_id == 888


def test_update_with_unicode(mongo_storage, cache, create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    req_pb = create_balancer_req()
    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer
    initial_version = b_pb.meta.version
    assert mongo_storage.list_balancer_revs(namespace_id=NS_ID, balancer_id=BALANCER_ID).total == 1

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.id = BALANCER_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = initial_version
    req_pb.meta.comment = COMMENT
    req_pb.spec.yandex_balancer.yaml = L7_MACRO_WITH_UNICODE
    req_pb.spec.config_transport.nanny_static_file.service_id = u'prod_balancer'
    req_pb.spec.yandex_balancer.mode = model_pb2.YandexBalancerSpec.EASY_MODE

    with pytest.raises(exceptions.BadRequestError) as exc:
        call(balancer_service.update_balancer, req_pb, LOGIN)
    assert 'Non-ascii characters present' in str(exc.value)

    req_pb.spec.yandex_balancer.yaml = L7_MACRO_WITH_UNICODE_COMMENT
    call(balancer_service.update_balancer, req_pb, LOGIN)


def test_update(mongo_storage, cache, create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    req_pb = create_balancer_req()
    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer
    initial_version = b_pb.meta.version
    assert mongo_storage.list_balancer_revs(namespace_id=NS_ID, balancer_id=BALANCER_ID).total == 1

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.id = BALANCER_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = 'xxx'
    req_pb.meta.comment = COMMENT

    def check_error():
        with pytest.raises(exceptions.BadRequestError) as exc:
            call(balancer_service.update_balancer, req_pb, LOGIN)
        assert str(exc.value) == ('at least one of the "spec", "meta.auth", "meta.location", "meta.flags" or '
                                  '"meta.transport_paused" fields must be present')

    wait_until_passes(check_error)

    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.yandex_balancer.yaml = YAML_CONFIG_2
    req_pb.spec.yandex_balancer.ClearField('config')
    with pytest.raises(exceptions.ConflictError) as e:
        call(balancer_service.update_balancer, req_pb, LOGIN)
    e.match('Balancer modification conflict')
    e.match('assumed version="xxx"')
    e.match('current="{}"'.format(b_pb.meta.version))
    assert mongo_storage.list_balancer_revs(namespace_id=NS_ID, balancer_id=BALANCER_ID).total == 1

    req_pb.meta.version = b_pb.meta.version

    req_pb.spec.yandex_balancer.yaml = create_too_many_lines_yaml_config(larger_than=2000)
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.update_balancer, req_pb, LOGIN)
    assert six.text_type(e.value) == (u'"spec.yandex_balancer.yaml" contains too many lines '
                                      u'(2002, allowed limit is 1000)')

    req_pb.spec.yandex_balancer.yaml = create_too_many_chars_yaml_config(larger_than=80000)
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.update_balancer, req_pb, LOGIN)
    assert six.text_type(e.value) == (u'"spec.yandex_balancer.yaml" contains too many characters '
                                      u'(80075, allowed limit is 80000)')

    req_pb.spec.yandex_balancer.yaml = YAML_CONFIG_2
    call(balancer_service.update_balancer, req_pb, LOGIN)

    def check_balancer():
        pb = api_pb2.GetBalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)
        balancer_pb = call(balancer_service.get_balancer, pb, LOGIN).balancer
        assert balancer_pb.spec.yandex_balancer.config.main.maxconn == 5000
        assert balancer_pb.meta.comment == COMMENT
        assert balancer_pb.meta.version != initial_version
        assert mongo_storage.list_balancer_revs(namespace_id=NS_ID, balancer_id=BALANCER_ID).total == 2
        # initial revision has not been seen by validator, so we look only for current version
        # assert len(b.status.revisions) == 2
        # assert [r.id for r in b.status.revisions] == [initial_version, b.meta.version]
        assert len(balancer_pb.status.revisions) == 1
        assert [r.id for r in balancer_pb.status.revisions] == [balancer_pb.meta.version]
        r_status_pb = balancer_pb.status.revisions[0]
        assert r_status_pb.id == balancer_pb.meta.version
        assert r_status_pb.validated.status == 'Unknown'
        assert r_status_pb.in_progress.status == 'False'
        assert r_status_pb.active.status == 'False'
        return balancer_pb, r_status_pb

    b_pb, r_status = wait_until(check_balancer, timeout=1)

    rev_pb = mongo_storage.get_balancer_rev(r_status.id)
    assert rev_pb.meta.id == r_status.id
    assert rev_pb.meta.balancer_id == BALANCER_ID
    assert rev_pb.meta.namespace_id == NS_ID
    assert rev_pb.meta.comment == COMMENT
    assert rev_pb.spec == b_pb.spec

    req_pb = api_pb2.GetBalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)
    resp_pb = call(balancer_service.get_balancer, req_pb, LOGIN)
    b_pb = resp_pb.balancer  # type: model_pb2.Balancer
    tunnel_pb = b_pb.spec.container_spec.outbound_tunnels.add(
        id='test',
        remote_ip='::1',
        mode=model_pb2.BalancerContainerSpec.OutboundTunnel.IPIP6,
    )
    tunnel_pb.rules.add(from_ip='5.45.202.111', to_ip='::3')

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.CopyFrom(b_pb.meta)
    req_pb.spec.CopyFrom(b_pb.spec)
    resp_pb = call(balancer_service.update_balancer, req_pb, LOGIN)

    # see https://st.yandex-team.ru/SWAT-6307 for details
    b_pb = resp_pb.balancer  # type: model_pb2.Balancer
    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.CopyFrom(b_pb.meta)
    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.ClearField('container_spec')
    req_pb.meta.comment = u'changed using awacsctl'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.update_balancer, req_pb, LOGIN)
    assert str(e.value) == u'"spec.container_spec" can not be removed using awacsctl (see SWAT-6307 for details)'


def test_update_balancer_indices(zk_storage, mongo_storage, cache, create_default_namespace, ctx, ctlrunner):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)

    spec_pb = model_pb2.BackendSpec()
    spec_pb.selector.type = model_pb2.BackendSelector.MANUAL
    Api.create_backend(namespace_id=NS_ID, backend_id=BACKEND_ID, spec_pb=spec_pb)

    req_pb = create_balancer_req()
    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer
    assert len(b_pb.meta.indices) == 1
    assert len(b_pb.meta.indices[0].included_backend_ids) == 0

    initial_version = b_pb.meta.version
    assert mongo_storage.list_balancer_revs(namespace_id=NS_ID, balancer_id=BALANCER_ID).total == 1

    req_pb = api_pb2.UpdateBalancerIndicesRequest()
    with awtest.raises(exceptions.BadRequestError, text_contains=u'No "id" specified.'):
        call(balancer_service.update_balancer_indices, req_pb, LOGIN)

    req_pb.id = BALANCER_ID
    with awtest.raises(exceptions.BadRequestError, text_contains=u'No "namespace_id" specified.'):
        call(balancer_service.update_balancer_indices, req_pb, LOGIN)

    with pytest.raises(exceptions.InternalError, match=u'No balancer revisions present in state'):
        req_pb.namespace_id = NS_ID
        call(balancer_service.update_balancer_indices, req_pb, LOGIN)

    balancer_ctl = BalancerCtl(NS_ID, BALANCER_ID)
    ctlrunner.run_ctl(balancer_ctl)
    balancer_ctl._force_process(ctx)

    update_indices_req = api_pb2.UpdateBalancerIndicesRequest(namespace_id=NS_ID, id=BALANCER_ID)
    call(balancer_service.update_balancer_indices, update_indices_req, LOGIN)

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.id = BALANCER_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = initial_version
    req_pb.meta.comment = COMMENT
    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.yandex_balancer.yaml = YAML_CONFIG_3
    req_pb.spec.yandex_balancer.ClearField('config')

    b_pb = call(balancer_service.update_balancer, req_pb, LOGIN).balancer
    last_rev_ind = None
    for ind_pb in b_pb.meta.indices:
        if ind_pb.id == b_pb.meta.version:
            last_rev_ind = ind_pb
            break
    assert last_rev_ind is not None
    assert list(last_rev_ind.included_backend_ids) == [flatten_full_id2((NS_ID, BACKEND_ID))]

    expected_indices = sorted(b_pb.meta.indices, key=str)
    for b_pb in zk_storage.update_balancer(NS_ID, BALANCER_ID):
        del b_pb.meta.indices[:]

    def check():
        b_pb = call(balancer_service.get_balancer,
                    api_pb2.GetBalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)).balancer
        assert len(b_pb.meta.indices) == 0
    wait_until_passes(check)

    ctlrunner.run_ctl(balancer_ctl)
    balancer_ctl._force_process(ctx)

    call(balancer_service.update_balancer_indices, update_indices_req, LOGIN)

    def check():
        req_pb = api_pb2.GetBalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)
        b_pb = call(balancer_service.get_balancer, req_pb).balancer
        assert sorted(b_pb.meta.indices, key=str) == expected_indices

    wait_until_passes(check)


@mock.patch.object(validation, 'validate_balancer_config')
def test_list_balancers_and_revisions(_1, create_default_namespace):
    set_login_to_root_users(LOGIN)
    ns_ids = ['aaa', 'bbb', 'ccc', 'ddd']
    balancers = {}
    for ns_id in ns_ids:
        create_default_namespace(ns_id)
        b_id = ns_id + '_sas'
        req_pb = create_balancer_req()
        req_pb.meta.id = b_id
        req_pb.meta.namespace_id = ns_id
        req_pb.spec.config_transport.nanny_static_file.service_id = 'prod_balancer_{}'.format(b_id)

        balancers[b_id] = call(balancer_service.create_balancer, req_pb, LOGIN).balancer

    def check_namespace():
        pb = api_pb2.ListNamespacesRequest()
        r_pb = call(namespace_service.list_namespaces, pb, LOGIN)
        assert len(r_pb.namespaces) == 4
        assert [ns.meta.id for ns in r_pb.namespaces] == ns_ids

    wait_until_passes(check_namespace)

    req_pb = api_pb2.ListBalancersRequest(namespace_id='aaa')
    resp_pb = call(balancer_service.list_balancers, req_pb, LOGIN)
    assert len(resp_pb.balancers) == 1
    assert [b.meta.id for b in resp_pb.balancers] == ['aaa_sas']
    assert all(resp_pb.balancers[0].HasField(f) for f in ('meta', 'spec', 'status'))

    req_pb.field_mask.paths.append('meta')
    resp_pb = call(balancer_service.list_balancers, req_pb, LOGIN)
    assert len(resp_pb.balancers) == 1

    assert [b.meta.id for b in resp_pb.balancers] == ['aaa_sas']
    assert resp_pb.balancers[0].HasField('meta')
    assert all(not resp_pb.balancers[0].HasField(f) for f in ('spec', 'status'))
    req_pb = api_pb2.ListBalancersRequest(namespace_id='aaa')
    resp_pb = call(balancer_service.list_balancers, req_pb, LOGIN)
    assert resp_pb.total == 1
    assert len(resp_pb.balancers) == 1

    req_pb = api_pb2.ListBalancersRequest(namespace_id='bbb', skip=1)
    resp_pb = call(balancer_service.list_balancers, req_pb, LOGIN)
    assert resp_pb.total == 1
    assert len(resp_pb.balancers) == 0

    req_pb = api_pb2.CreateBalancerRequest()
    req_pb.meta.id = 'xxx_sas'
    req_pb.meta.namespace_id = 'xxx'
    create_default_namespace(req_pb.meta.namespace_id)
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.location.type = req_pb.meta.location.YP_CLUSTER
    req_pb.meta.location.yp_cluster = 'SAS'
    req_pb.spec.config_transport.nanny_static_file.service_id = 'es_balancer'
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.yaml = L7_MACRO

    balancers[req_pb.meta.id] = call(balancer_service.create_balancer, req_pb, LOGIN).balancer

    req_pb = api_pb2.ListBalancersRequest()
    resp_pb = call(balancer_service.list_balancers, req_pb, LOGIN)
    assert resp_pb.total == len(balancers)

    # add yet another balancer
    req_pb = api_pb2.CreateBalancerRequest()
    req_pb.meta.id = 'eee_sas'
    req_pb.meta.namespace_id = 'eee'
    create_default_namespace(req_pb.meta.namespace_id)
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.location.type = req_pb.meta.location.YP_CLUSTER
    req_pb.meta.location.yp_cluster = 'SAS'
    req_pb.meta.flags.skip_rps_check_during_removal.value = True
    req_pb.spec.config_transport.nanny_static_file.service_id = 'prod_balancer'
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.yaml = '{}'

    balancers[req_pb.meta.id] = call(balancer_service.create_balancer, req_pb, LOGIN).balancer

    def check_list():
        r_pb = call(balancer_service.list_balancers, api_pb2.ListBalancersRequest(namespace_id='eee'), LOGIN)
        assert len(r_pb.balancers) == 1

    wait_until_passes(check_list)

    # remove added balancer
    req_pb = api_pb2.RemoveBalancerRequest(namespace_id='eee', id='eee_sas')
    call(balancer_service.remove_balancer, req_pb, LOGIN)

    def check_list_2():
        r_pb = call(balancer_service.list_balancers, api_pb2.ListBalancersRequest(namespace_id='eee'), LOGIN)
        assert r_pb.balancers[0].spec.deleted

    wait_until_passes(check_list_2)

    for _ in range(3):
        req_pb = api_pb2.UpdateBalancerRequest()
        req_pb.meta.id = 'aaa_sas'
        req_pb.meta.namespace_id = 'aaa'
        req_pb.meta.version = balancers['aaa_sas'].meta.version
        req_pb.spec.CopyFrom(balancers['aaa_sas'].spec)
        req_pb.spec.yandex_balancer.yaml += ' '
        balancers['aaa_sas'] = call(balancer_service.update_balancer, req_pb, LOGIN).balancer

    req_pb = api_pb2.ListBalancerRevisionsRequest(namespace_id='aaa', id='aaa_sas')
    resp_pb = call(balancer_service.list_balancer_revisions, req_pb, LOGIN)
    assert resp_pb.total == 4
    assert set(rev.meta.balancer_id for rev in resp_pb.revisions) == {'aaa_sas'}
    assert len(resp_pb.revisions) == 4
    assert resp_pb.revisions[0].meta.id == balancers['aaa_sas'].meta.version

    req_pb = api_pb2.ListBalancerRevisionsRequest(namespace_id='aaa', id='aaa_sas', skip=2)
    resp_pb = call(balancer_service.list_balancer_revisions, req_pb, LOGIN)
    assert resp_pb.total == 4
    assert len(resp_pb.revisions) == 2

    req_pb = api_pb2.ListBalancerRevisionsRequest(namespace_id='aaa', id='aaa_sas', skip=2, limit=1)
    resp_pb = call(balancer_service.list_balancer_revisions, req_pb, LOGIN)
    assert resp_pb.total == 4
    assert len(resp_pb.revisions) == 1


@mock.patch.object(validation, 'validate_upstream_config')
@mock.patch.object(validation, 'validate_balancer_config')
def test_remove_balancer(_1, _2, cache, mongo_storage, zk_storage, create_default_namespace, checker):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    upstream_1_id = 'upstream_1'

    req_pb = create_balancer_req()
    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer

    req_pb = api_pb2.CreateUpstreamRequest()
    req_pb.meta.id = upstream_1_id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.yaml = '{}'

    call(upstream_service.create_upstream, req_pb, LOGIN)

    def check_balancer():
        balancer_state_pb = zk_storage.get_balancer_state(NS_ID, BALANCER_ID)
        assert balancer_state_pb.namespace_id == NS_ID
        assert balancer_state_pb.balancer_id == BALANCER_ID
        assert mongo_storage.list_balancer_revs(namespace_id=NS_ID, balancer_id=BALANCER_ID).total == 1
        assert mongo_storage.list_upstream_revs(NS_ID, upstream_1_id).total == 1

    wait_until_passes(check_balancer)

    req_pb = api_pb2.RemoveBalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)
    with pytest.raises(exceptions.ForbiddenError, match='Could not check if balancer has any traffic, please check it m'
                                                        'anually and contact support for approval of balancer removal'):
        call(balancer_service.remove_balancer, req_pb, LOGIN)

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.id = BALANCER_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = b_pb.meta.version
    req_pb.meta.flags.skip_rps_check_during_removal.value = True
    b_pb = call(balancer_service.update_balancer, req_pb, LOGIN).balancer

    balancer_op_pb = model_pb2.BalancerOperation()
    balancer_op_pb.meta.id = BALANCER_ID
    balancer_op_pb.meta.namespace_id = NS_ID
    zk_storage.create_balancer_operation(NS_ID, BALANCER_ID, balancer_op_pb)
    wait_until_passes(lambda: cache.must_get_balancer_operation(NS_ID, BALANCER_ID))

    for check in checker:
        with check:
            balancer_pb = zk_storage.get_balancer(NS_ID, BALANCER_ID)
            assert balancer_pb.meta.flags.skip_rps_check_during_removal.value

    req_pb = api_pb2.RemoveBalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)
    with pytest.raises(exceptions.ForbiddenError, match='Cannot remove balancer with an active operation'):
        call(balancer_service.remove_balancer, req_pb, LOGIN)

    zk_storage.remove_balancer_operation(NS_ID, BALANCER_ID)
    assert wait_until(lambda: cache.get_balancer_operation(NS_ID, BALANCER_ID) is None, timeout=1)
    call(balancer_service.remove_balancer, req_pb, LOGIN)

    for check in checker:
        with check:
            balancer_pb = zk_storage.get_balancer(NS_ID, BALANCER_ID)
            assert balancer_pb.spec.deleted


def test_balancers_config_to_yaml(create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    req_pb = create_balancer_req()
    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.id = BALANCER_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = b_pb.meta.version
    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.yandex_balancer.yaml = ''
    req_pb.spec.yandex_balancer.config.main.addrs[0].port = 0
    req_pb.spec.yandex_balancer.config.main.addrs[0].f_port.type = modules_pb2.Call.GET_PORT_VAR
    req_pb.spec.yandex_balancer.config.main.addrs[0].f_port.get_port_var_params.var = 'name'

    def check_yaml():
        r_pb = call(balancer_service.update_balancer, req_pb, LOGIN)
        assert r_pb.balancer.spec.yandex_balancer.yaml == '''main:
  addrs:
  - ip: 127.0.0.1
    port: !f 'get_port_var("name")'
  admin_addrs:
  - ip: 127.0.0.2
    port: 80
  maxconn: 4000
  workers: 1
  buffer: 65536
  log: /usr/local/www/logs/current-childs_log-balancer-16020
  events:
    stats: report
  ipdispatch:
    include_upstreams:
      type: ALL
'''

    wait_until_passes(check_yaml)


def test_balancers_location(create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    req_pb = create_balancer_req()
    req_pb.meta.location.yp_cluster = 'SAS'
    req_pb.meta.location.type = model_pb2.BalancerMeta.Location.YP_CLUSTER
    req_pb.meta.id = 'balancer'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    assert str(e.value) == ('"meta.id": balancer in location SAS must start with "sas.", "yp.sas." or "sas-yp."'
                            ' or end with "-sas", "_sas", "-sas-yp" or "_sas_yp"')
    req_pb.meta.id = 'balancer_sas'
    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer
    assert b_pb.meta.location.type == model_pb2.BalancerMeta.Location.YP_CLUSTER
    assert b_pb.meta.location.yp_cluster == 'SAS'

    req_pb = create_balancer_req()
    req_pb.meta.id = 'vla.balancer'
    req_pb.meta.location.yp_cluster = 'VLA'
    req_pb.spec.config_transport.nanny_static_file.service_id = 'prod_balancer_2'
    call(balancer_service.create_balancer, req_pb, LOGIN)

    req_pb = create_balancer_req()
    req_pb.meta.location.type = 0
    req_pb.meta.location.yp_cluster = ''
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    assert str(e.value) == '"meta.location" must be specified'

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.id = 'vla.balancer'
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = 'xxx'
    req_pb.meta.location.type = model_pb2.BalancerMeta.Location.YP_CLUSTER
    req_pb.meta.location.yp_cluster = 'VLA'
    b_pb = call(balancer_service.update_balancer, req_pb, LOGIN).balancer
    assert b_pb.meta.location.type == model_pb2.BalancerMeta.Location.YP_CLUSTER
    assert b_pb.meta.location.yp_cluster == 'VLA'

    req_pb = create_balancer_order_req()
    req_pb.meta.location.yp_cluster = 'VLA'
    req_pb.meta.id = 'vla.another.balancer'
    req_pb.order.copy_nanny_service.balancer_id = 'vla.balancer'
    req_pb.order.abc_service_id = 999
    with mock.patch.object(balancer_service, 'validate_nanny_service_nonexistence'), \
        mock.patch.object(balancer, 'validate_nanny_service_for_copy'):  # noqa
        with pytest.raises(exceptions.BadRequestError,
                           match='"order.allocation_request.location" must match "meta.location"'):
            call(balancer_service.create_balancer, req_pb, LOGIN)

        req_pb.order.allocation_request.location = 'VLA'
        with pytest.raises(exceptions.BadRequestError,
                           match='Cannot create copied balancer in the same location as another balancer: '
                                 '"vla.balancer"'):  # noqa
            call(balancer_service.create_balancer, req_pb, LOGIN)

        req_pb.order.allocation_request.location = 'MAN'
        req_pb.meta.location.yp_cluster = 'MAN'
        req_pb.meta.id = 'man.balancer'
        call(balancer_service.create_balancer, req_pb, LOGIN)


def test_validate_location_balancer_id():
    oks = [
        ('VLA', 'vla.balancer.1'),
        ('VLA', '2-balancer-vla'),
        ('VLA', '3-vla-yp'),
        ('VLA', 'bb_vla'),
        ('VLA', 'pickup-points-manager.taxi.yandex.net_vla'),
        ('VLA', 'pickup-points-manager.taxi.yandex.net_vla_yp'),
        ('VLA', 'yp.vla.pickup-points-manager.taxi.yandex.net'),
        ('VLA', 'vla-yp.pickup-points-manager.taxi.yandex.net'),
        ('SAS', 'qloud_migrated_metrics_test_sas'),
        ('TEST_SAS', 'romanovich31337_test_sas'),
        ('MAN_PRE', 'man_pre.bb'),
    ]
    for cluster, _id in oks:
        validate_location_balancer_id(yp_cluster=cluster, _id=_id)

    yp_cluster = 'VLA'
    duplicates = [
        '2-balancer_vla-vla-yp',
        'b_vla_vla',
        'vla.b_vla',
        'vla.b-vla-yp',
        'vla-yp.b_vla_yp',
    ]
    for _id in duplicates:
        with pytest.raises(ValidationError,
                           match='"meta.id": duplicate location affix "vla"'):
            validate_location_balancer_id(yp_cluster=yp_cluster, _id=_id)

    wrong_affix = [
        ('VLA', 'vla.balancer.1_sas', 'sas'),
        ('VLA', '2-balancer-sas-vla', 'sas'),
        ('VLA', '3-sas-yp-vla-yp', 'sas'),
        ('VLA', 'sas.bb_vla', 'sas'),
        ('VLA', 'yp.sas.bb_vla_yp', 'sas'),
        ('TEST_SAS', 'yp.test_sas.bb_sas_yp', 'sas'),
        ('MAN_PRE', 'yp.man.bb_man_pre_yp', 'man'),
        ('MAN_PRE', 'yp.sas.bb_man_pre_yp', 'sas'),
    ]
    for cluster, _id, wrong in wrong_affix:
        with pytest.raises(ValidationError,
                           match='"meta.id": wrong location affix "{}" for balancer in location "{}"'.format(
                               wrong, cluster)):
            validate_location_balancer_id(yp_cluster=cluster, _id=_id)

    fails = [
        '',
        '.',
        'vla1',
        'vla-yp',
        '-vla.asd-yp',
        'balancer',
        'bb_Vla',
        'bal_vla-yp',
        'balancer-VLA',
        'vla',
        '.vla.bbb-yp',
        'yp-vla.bbb'
    ]
    for _id in fails:
        with pytest.raises(ValidationError,
                           match='"meta.id": balancer in location VLA must start with "vla.", "yp.vla."'
                                 ' or "vla-yp." or end with "-vla", "_vla", "-vla-yp" or "_vla_yp"'):
            validate_location_balancer_id(yp_cluster=yp_cluster, _id=_id)


def test_balancer_from_nanny_service(create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    req_pb = create_balancer_req()
    req_pb.meta.id = 'balancer_from_nanny_service_sas'

    # Simple gencfg nanny service
    req_pb.spec.config_transport.nanny_static_file.service_id = 'from_nanny_service_simple'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    msg = ('Nanny service must have type AWACS_BALANCER. See '
           'https://wiki.yandex-team.ru/cplb/awacs/awacs-balancers-from-existing-nanny-service/ for details')
    assert str(e.value) == msg

    # Gencfg nanny service with type==AWACS_BALANCER
    req_pb.spec.config_transport.nanny_static_file.service_id = 'from_nanny_service_awacs'
    call(balancer_service.create_balancer, req_pb, LOGIN)

    req_pb.meta.id = 'balancer_from_nanny_service2_sas'

    # YP nanny service with type==AWACS_BALANCER
    req_pb.spec.config_transport.nanny_static_file.service_id = 'from_nanny_service_awacs_yp'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    msg = ('Balancer can be created only from single location Nanny service. See '
           'https://wiki.yandex-team.ru/cplb/awacs/awacs-balancers-from-existing-nanny-service/ for details')
    assert str(e.value) == msg

    # YP nanny service with type==AWACS_BALANCER and yp_cluster==SAS
    req_pb.meta.id = 'balancer_from_nanny_service2'
    req_pb.spec.config_transport.nanny_static_file.service_id = 'from_nanny_service_awacs_yp_sas'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    assert str(e.value) == ('"meta.id": balancer in location SAS must start with "sas.", "yp.sas." or "sas-yp." '
                            'or end with "-sas", "_sas", "-sas-yp" or "_sas_yp"')

    # Fix balancer name
    req_pb.meta.id = 'balancer_from_nanny_service2_sas'
    resp_pb = call(balancer_service.create_balancer, req_pb, LOGIN)
    assert resp_pb.balancer.meta.location.yp_cluster == 'SAS'
    assert resp_pb.balancer.meta.location.type == model_pb2.BalancerMeta.Location.YP_CLUSTER

    # Trying to create MAN balancer from SAS Nanny service
    req_pb.meta.id = 'balancer_from_nanny_service2_man'
    req_pb.meta.location.type = model_pb2.BalancerMeta.Location.YP_CLUSTER
    req_pb.meta.location.yp_cluster = 'MAN'
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    msg = ('Balancer location must be the same as Nanny service location. See '
           'https://wiki.yandex-team.ru/cplb/awacs/awacs-balancers-from-existing-nanny-service/ for details')
    assert str(e.value) == msg


@mock.patch.object(validation, 'validate_balancer_config')
def test_create_gencfg_migration(_1, create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    op_req_pb = api_pb2.CreateBalancerOperationRequest()
    op_req_pb.meta.id = 'xxx_sas'
    op_req_pb.meta.namespace_id = 'xxx'
    op_req_pb.order.migrate_from_gencfg_to_yp_lite.SetInParent()
    with pytest.raises(exceptions.NotFoundError, match='Balancer "xxx:xxx_sas" does not exist'):
        call(balancer_service.create_balancer_operation, op_req_pb, LOGIN)

    req_pb = api_pb2.CreateBalancerRequest()
    req_pb.meta.id = 'xxx_sas'
    req_pb.meta.namespace_id = 'xxx'
    create_default_namespace(req_pb.meta.namespace_id)
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.location.type = req_pb.meta.location.GENCFG_DC
    req_pb.meta.location.gencfg_dc = 'SAS'
    req_pb.spec.config_transport.nanny_static_file.service_id = 'gencfg_balancer'
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.mode = model_pb2.YandexBalancerSpec.EASY_MODE
    req_pb.spec.yandex_balancer.yaml = L7_MACRO
    call(balancer_service.create_balancer, req_pb, LOGIN)

    with pytest.raises(exceptions.BadRequestError,
                       match=r'"order.content.migrate_from_gencfg_to_yp_lite.new_balancer_id" must be set'):
        call(balancer_service.create_balancer_operation, op_req_pb, LOGIN)

    op_req_pb.order.migrate_from_gencfg_to_yp_lite.new_balancer_id = 'sas.new_balancer_sas'
    with pytest.raises(exceptions.BadRequestError,
                       match=r'"order.content.migrate_from_gencfg_to_yp_lite.abc_service_id" must be specified'):
        call(balancer_service.create_balancer_operation, op_req_pb, LOGIN)

    op_req_order_content_pb = op_req_pb.order.migrate_from_gencfg_to_yp_lite
    op_req_order_content_pb.abc_service_id = 1821
    with pytest.raises(exceptions.BadRequestError,
                       match=r'"order.content.migrate_from_gencfg_to_yp_lite.allocation_request" must be specified'):
        call(balancer_service.create_balancer_operation, op_req_pb, LOGIN)

    op_req_pb.order.migrate_from_gencfg_to_yp_lite.allocation_request.nanny_service_id_slug = 'not-used-yet'
    with pytest.raises(exceptions.BadRequestError,
                       match=r'"order.content.migrate_from_gencfg_to_yp_lite.allocation_request.location"'
                             r' must be set'):
        call(balancer_service.create_balancer_operation, op_req_pb, LOGIN)

    op_req_order_content_pb.allocation_request.location = 'SAS'
    op_req_order_content_pb.allocation_request.network_macro = '_SEARCHSAND_'
    op_req_order_content_pb.allocation_request.preset.instances_count = 1
    op_req_order_content_pb.allocation_request.preset.type = op_req_order_content_pb.allocation_request.preset.MICRO
    with pytest.raises(exceptions.BadRequestError,
                       match=r'"meta.id": duplicate location affix "sas"'):
        call(balancer_service.create_balancer_operation, op_req_pb, LOGIN)

    op_req_pb.order.migrate_from_gencfg_to_yp_lite.new_balancer_id = 'new_balancer_sas'
    call(balancer_service.create_balancer_operation, op_req_pb, LOGIN)


def test_list_balancer_operations(zk_storage, cache, create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    cert_id = 'anonymous.in.yandex-team.ru'
    cert_pb = model_pb2.Certificate()
    cert_pb.meta.id = cert_id
    cert_pb.meta.namespace_id = 'xxx'
    create_default_namespace(cert_pb.meta.namespace_id)
    cert_pb.spec.fields.subject_common_name = 'anonymous.in.yandex-team.ru'
    zk_storage.create_cert(namespace_id='xxx', cert_id=cert_id, cert_pb=cert_pb)
    wait_until_passes(lambda: cache.must_get_cert('xxx', cert_id))
    ids = ['aaa_sas', 'bbb_sas', 'ccc_sas', 'ddd_sas']
    balancer_op_pbs = {}
    for _id in ids:
        req_pb = api_pb2.CreateBalancerRequest()
        req_pb.meta.id = _id
        req_pb.meta.namespace_id = 'xxx'
        req_pb.meta.auth.type = req_pb.meta.auth.STAFF
        req_pb.meta.location.type = req_pb.meta.location.YP_CLUSTER
        req_pb.meta.location.yp_cluster = 'SAS'
        req_pb.spec.config_transport.nanny_static_file.service_id = 'es_balancer' + _id
        req_pb.spec.type = model_pb2.YANDEX_BALANCER
        req_pb.spec.yandex_balancer.mode = model_pb2.YandexBalancerSpec.EASY_MODE
        req_pb.spec.yandex_balancer.yaml = L7_MACRO
        call(balancer_service.create_balancer, req_pb, LOGIN)

        req_pb = api_pb2.CreateBalancerOperationRequest()
        req_pb.meta.id = _id
        req_pb.meta.namespace_id = 'xxx'
        req_pb.meta.comment = COMMENT
        req_pb.order.create_system_backend.SetInParent()

        balancer_op_pbs[_id] = call(balancer_service.create_balancer_operation, req_pb, LOGIN).operation

    def check_list():
        list_pb = api_pb2.ListBalancerOperationsRequest(namespace_id='xxx')
        pb = call(balancer_service.list_balancer_operations, list_pb, LOGIN)
        assert pb.total == 4
        assert len(pb.operations) == 4

    wait_until_passes(check_list)

    req_pb = api_pb2.ListBalancerOperationsRequest(namespace_id='xxx', skip=4)
    resp_pb = call(balancer_service.list_balancer_operations, req_pb, LOGIN)
    assert resp_pb.total == 4
    assert len(resp_pb.operations) == 0


def test_update_balancer_transport_paused(zk_storage, cache, checker, create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    req_pb = create_balancer_req()
    call(balancer_service.create_balancer, req_pb, LOGIN)

    req_pb = api_pb2.UpdateBalancerTransportPausedRequest(namespace_id=NS_ID, id=BALANCER_ID)
    with pytest.raises(exceptions.BadRequestError, match='"transport_paused" must be set'):
        call(balancer_service.update_balancer_transport_paused, req_pb, LOGIN)

    req_pb.transport_paused.value = True
    req_pb.transport_paused.comment = 'paused'
    resp_pb = call(balancer_service.update_balancer_transport_paused, req_pb, LOGIN)
    assert resp_pb.transport_paused.value
    assert resp_pb.transport_paused.comment == 'paused'
    assert resp_pb.transport_paused.author == LOGIN

    get_req_pb = api_pb2.GetBalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)
    for a in checker:
        with a:
            transport_paused_pb = call(balancer_service.get_balancer, get_req_pb, LOGIN).balancer.meta.transport_paused
            assert transport_paused_pb.value
            assert transport_paused_pb.comment == 'paused'
            assert transport_paused_pb.author == LOGIN

    req_pb.transport_paused.value = False
    req_pb.transport_paused.comment = 'unpaused'
    req_pb.transport_paused.author = 'someone_else'
    resp_pb = call(balancer_service.update_balancer_transport_paused, req_pb, LOGIN)
    assert not resp_pb.transport_paused.value
    assert resp_pb.transport_paused.comment == 'unpaused'
    assert resp_pb.transport_paused.author == LOGIN

    for a in checker:
        with a:
            transport_paused_pb = call(balancer_service.get_balancer, get_req_pb, LOGIN).balancer.meta.transport_paused
            assert not transport_paused_pb.value
            assert transport_paused_pb.comment == 'unpaused'
            assert transport_paused_pb.author == LOGIN


def test_update_balancer_flags(checker, create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    req_pb = create_balancer_req()
    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.id = BALANCER_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = b_pb.meta.version
    req_pb.meta.flags.skip_rps_check_during_removal.value = True
    resp_pb = call(balancer_service.update_balancer, req_pb, LOGIN)
    assert resp_pb.balancer.meta.flags.skip_rps_check_during_removal.value

    get_req_pb = api_pb2.GetBalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)
    for check in checker:
        with check:
            flags_pb = call(balancer_service.get_balancer, get_req_pb, LOGIN).balancer.meta.flags
            assert flags_pb.skip_rps_check_during_removal.value
            assert flags_pb.skip_rps_check_during_removal.author == LOGIN


@mock.patch.object(balancer_service, 'validate_nanny_service_nonexistence', lambda *args, **kwargs: None)
def test_create_balancers_in_testing_namespace(zk_storage, cache):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    ns_pb.meta.auth.type = ns_pb.meta.auth.STAFF
    ns_pb.meta.auth.staff.owners.logins.extend([LOGIN])
    ns_pb.order.status.status = 'FINISHED'
    ns_pb.spec.env_type = model_pb2.NamespaceSpec.NS_ENV_TESTING
    zk_storage.create_namespace(NS_ID, ns_pb)
    wait_until_passes(lambda: cache.must_get_namespace(NS_ID))

    bal1_id = 'nstest1_sas'
    req_pb = api_pb2.CreateBalancerRequest()
    req_pb.meta.id = bal1_id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.location.type = req_pb.meta.location.YP_CLUSTER
    req_pb.meta.location.yp_cluster = 'SAS'
    req_pb.meta.flags.skip_rps_check_during_removal.value = True
    req_pb.order.allocation_request.nanny_service_id_slug = u'xxx'
    req_pb.order.allocation_request.location = u'SAS'
    req_pb.order.allocation_request.network_macro = u'_SEARCHSAND_'
    req_pb.order.allocation_request.type = req_pb.order.allocation_request.PRESET
    req_pb.order.allocation_request.preset.type = req_pb.order.allocation_request.preset.MICRO
    req_pb.order.allocation_request.preset.instances_count = 1
    req_pb.order.abc_service_id = 999
    req_pb.order.env_type = model_pb2.BalancerSpec.L7_ENV_PRESTABLE

    with pytest.raises(exceptions.BadRequestError,
                       match='PRESTABLE and PRODUCTION mode balancers cannot be created in TESTING namespace'):
        call(balancer_service.create_balancer, req_pb, LOGIN)

    req_pb.order.env_type = model_pb2.BalancerSpec.L7_ENV_TESTING
    call(balancer_service.create_balancer, req_pb, LOGIN)
    expected_total = 1

    def check_list():
        r_pb = call(balancer_service.list_balancers, api_pb2.ListBalancersRequest(namespace_id=NS_ID), LOGIN)
        assert len(r_pb.balancers) == expected_total

    wait_until_passes(check_list)

    bal2_id = 'nstest2_sas'
    req_pb.meta.id = bal2_id
    req_pb.order.env_type = model_pb2.BalancerSpec.L7_ENV_UNKNOWN

    call(balancer_service.create_balancer, req_pb, LOGIN)
    expected_total = 2
    wait_until_passes(check_list)


@mock.patch.object(balancer_service, 'validate_nanny_service_nonexistence', lambda *args, **kwargs: None)
def test_normalized_prj_validation(zk_storage, cache):
    prj1 = 'some.prj'
    prj2 = 'some_prj'
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    ns_pb.meta.auth.type = ns_pb.meta.auth.STAFF
    ns_pb.meta.auth.staff.owners.logins.extend([LOGIN])
    ns_pb.order.status.status = 'FINISHED'
    ns_pb.spec.env_type = model_pb2.NamespaceSpec.NS_ENV_TESTING
    ns_pb.spec.balancer_constraints.instance_tags.prj = prj1
    zk_storage.create_namespace(NS_ID, ns_pb)
    wait_until_passes(lambda: cache.must_get_namespace(NS_ID))

    ns2_id = NS_ID + '_2'
    ns_pb.meta.id = ns2_id
    ns_pb.spec.balancer_constraints.instance_tags.ClearField('prj')
    zk_storage.create_namespace(ns2_id, ns_pb)
    wait_until_passes(lambda: cache.must_get_namespace(ns2_id))

    bal1_id = 'nstest1_sas'
    bal2_id = 'nstest2_sas'

    bal_pb = model_pb2.Balancer()
    bal_pb.meta.id = bal1_id
    bal_pb.meta.namespace_id = NS_ID
    bal_pb.spec.config_transport.nanny_static_file.instance_tags.prj = prj1
    zk_storage.create_balancer(NS_ID, bal1_id, bal_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, bal1_id))

    req_pb = api_pb2.CreateBalancerRequest()
    req_pb.meta.id = bal2_id
    req_pb.meta.namespace_id = ns2_id
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.location.type = req_pb.meta.location.YP_CLUSTER
    req_pb.meta.location.yp_cluster = 'SAS'
    req_pb.order.allocation_request.nanny_service_id_slug = u'xxx'
    req_pb.order.allocation_request.location = u'SAS'
    req_pb.order.allocation_request.network_macro = u'_SEARCHSAND_'
    req_pb.order.allocation_request.type = req_pb.order.allocation_request.PRESET
    req_pb.order.allocation_request.preset.type = req_pb.order.allocation_request.preset.MICRO
    req_pb.order.allocation_request.preset.instances_count = 1
    req_pb.order.abc_service_id = 999
    req_pb.order.instance_tags.prj = prj1

    # Try the same prj in another namespace
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    assert str(e.value) == ('"order.instance_tags.prj": normalized prj tag "some_prj" is already used by another '
                            'namespace: {}'.format(NS_ID))

    # Try the same normalised prj in another namespace
    req_pb.order.instance_tags.prj = prj2
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    assert str(e.value) == ('"order.instance_tags.prj": normalized prj tag "some_prj" is already used by another '
                            'namespace: {}'.format(NS_ID))

    # Try the same normalised prj in same namespace
    req_pb.meta.namespace_id = NS_ID
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    assert str(e.value) == '"order.instance_tags.prj": must be equal to "some.prj"'

    # Try the same prj in same namespace
    req_pb.order.instance_tags.prj = prj1
    call(balancer_service.create_balancer, req_pb, LOGIN)

    # Try the same prj in same namespace
    req_pb = api_pb2.CreateNamespaceRequest()
    req_pb.meta.id = NS_ID + '_123'
    req_pb.meta.category = 'test'
    req_pb.meta.abc_service_id = 123
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.auth.staff.owners.logins.extend([LOGIN])
    req_pb.order.flow_type = req_pb.order.QUICK_START
    req_pb.order.yp_lite_allocation_request.nanny_service_id_slug = 'slug'
    req_pb.order.yp_lite_allocation_request.locations.append('sas')
    req_pb.order.yp_lite_allocation_request.network_macro = '_FERENETS_'
    req_pb.order.yp_lite_allocation_request.preset.type = req_pb.order.yp_lite_allocation_request.preset.MICRO
    req_pb.order.yp_lite_allocation_request.preset.instances_count = 2
    req_pb.order.certificate_order_content.ca_name = 'InternalCA'
    req_pb.order.certificate_order_content.abc_service_id = 1821
    req_pb.order.certificate_order_content.common_name = 'yandex.yandex.ru'
    es = req_pb.order.endpoint_sets.add()
    es.cluster = 'sas'
    es.id = '1'
    req_pb.order.instance_tags.prj = prj2

    with pytest.raises(exceptions.BadRequestError) as e:
        call(namespace_service.create_namespace, req_pb, LOGIN)
    assert str(e.value) == ('"order.instance_tags.prj": normalized prj tag "some_prj" is already used by another '
                            'namespace: {}'.format(NS_ID))


def test_update_balancers_in_testing_namespace(zk_storage, cache):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    ns_pb.meta.auth.type = ns_pb.meta.auth.STAFF
    ns_pb.meta.auth.staff.owners.logins.extend([LOGIN])
    ns_pb.order.status.status = 'FINISHED'
    ns_pb.spec.env_type = model_pb2.NamespaceSpec.NS_ENV_TESTING
    zk_storage.create_namespace(NS_ID, ns_pb)
    wait_until_passes(lambda: cache.must_get_namespace(NS_ID))

    appconfig.set_value('run.root_users', [LOGIN])
    req_pb = create_balancer_req()
    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer

    # env_type is set by order processor so at the beginning it's always UNKNOWN regardless of env_type in order
    assert b_pb.spec.env_type == model_pb2.BalancerSpec.L7_ENV_UNKNOWN
    appconfig.set_value('run.root_users', [])

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.id = b_pb.meta.id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = b_pb.meta.version

    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.env_type = model_pb2.BalancerSpec.L7_ENV_PRODUCTION
    with pytest.raises(exceptions.BadRequestError, match='can be set only to TESTING if namespace is in TESTING'):
        call(balancer_service.update_balancer, req_pb, LOGIN)

    req_pb.spec.env_type = model_pb2.BalancerSpec.L7_ENV_TESTING

    b_pb = call(balancer_service.update_balancer, req_pb, LOGIN).balancer
    assert b_pb.spec.env_type == model_pb2.BalancerSpec.L7_ENV_TESTING

    req_pb.meta.version = b_pb.meta.version
    req_pb.spec.env_type = model_pb2.BalancerSpec.L7_ENV_PRODUCTION

    with pytest.raises(exceptions.BadRequestError, match='can only be changed by root'):
        call(balancer_service.update_balancer, req_pb, LOGIN)

    req_pb.spec.env_type = model_pb2.BalancerSpec.L7_ENV_UNKNOWN

    with pytest.raises(exceptions.BadRequestError, match='can only be changed by root'):
        call(balancer_service.update_balancer, req_pb, LOGIN)


@mock.patch.object(balancer_service, 'validate_nanny_service_nonexistence', lambda *args, **kwargs: None)
def test_create_balancers_in_production_namespace(zk_storage, cache):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    ns_pb.meta.auth.type = ns_pb.meta.auth.STAFF
    ns_pb.meta.auth.staff.owners.logins.extend([LOGIN])
    ns_pb.order.status.status = 'FINISHED'
    ns_pb.spec.env_type = model_pb2.NamespaceSpec.NS_ENV_PRODUCTION
    zk_storage.create_namespace(NS_ID, ns_pb)
    wait_until_passes(lambda: cache.must_get_namespace(NS_ID))

    bal1_id = 'nstest1_sas'
    req_pb = api_pb2.CreateBalancerRequest()
    req_pb.meta.id = bal1_id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.location.type = req_pb.meta.location.YP_CLUSTER
    req_pb.meta.location.yp_cluster = 'SAS'
    req_pb.meta.flags.skip_rps_check_during_removal.value = True
    req_pb.order.allocation_request.nanny_service_id_slug = u'xxx'
    req_pb.order.allocation_request.location = u'SAS'
    req_pb.order.allocation_request.network_macro = u'_SEARCHSAND_'
    req_pb.order.allocation_request.type = req_pb.order.allocation_request.PRESET
    req_pb.order.allocation_request.preset.type = req_pb.order.allocation_request.preset.MICRO
    req_pb.order.allocation_request.preset.instances_count = 1
    req_pb.order.abc_service_id = 999
    req_pb.order.env_type = model_pb2.BalancerSpec.L7_ENV_TESTING

    with pytest.raises(exceptions.BadRequestError,
                       match='Balancer cannot be created in TESTING mode in PRODUCTION namespace'):
        call(balancer_service.create_balancer, req_pb, LOGIN)

    req_pb.order.env_type = model_pb2.BalancerSpec.L7_ENV_PRODUCTION
    req_pb.order.instance_tags.ctype = 'prod'

    call(balancer_service.create_balancer, req_pb, LOGIN)
    expected_total = 1

    def check_list():
        r_pb = call(balancer_service.list_balancers, api_pb2.ListBalancersRequest(namespace_id=NS_ID), LOGIN)
        assert len(r_pb.balancers) == expected_total

    wait_until_passes(check_list)

    bal2_id = 'nstest2_sas'
    req_pb.meta.id = bal2_id
    req_pb.order.env_type = model_pb2.BalancerSpec.L7_ENV_UNKNOWN

    call(balancer_service.create_balancer, req_pb, LOGIN)
    expected_total = 2
    wait_until_passes(check_list)


def test_update_balancers_in_production_namespace(zk_storage, cache):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    ns_pb.meta.auth.type = ns_pb.meta.auth.STAFF
    ns_pb.meta.auth.staff.owners.logins.extend([LOGIN])
    ns_pb.order.status.status = 'FINISHED'
    ns_pb.spec.env_type = model_pb2.NamespaceSpec.NS_ENV_PRODUCTION
    zk_storage.create_namespace(NS_ID, ns_pb)
    wait_until_passes(lambda: cache.must_get_namespace(NS_ID))

    appconfig.set_value('run.root_users', [LOGIN])
    req_pb = create_balancer_req()
    req_pb.spec.config_transport.nanny_static_file.instance_tags.ctype = 'prod'
    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer

    # env_type is set by order processor so at the beginning it's always UNKNOWN regardless of env_type in order
    assert b_pb.spec.env_type == model_pb2.BalancerSpec.L7_ENV_UNKNOWN
    appconfig.set_value('run.root_users', [])

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.id = b_pb.meta.id
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = b_pb.meta.version

    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.env_type = model_pb2.BalancerSpec.L7_ENV_TESTING
    with pytest.raises(exceptions.BadRequestError,
                       match='"spec.env_type" cannot be set to TESTING when namespace is not TESTING'):
        call(balancer_service.update_balancer, req_pb, LOGIN)

    req_pb.spec.env_type = model_pb2.BalancerSpec.L7_ENV_PRODUCTION

    b_pb = call(balancer_service.update_balancer, req_pb, LOGIN).balancer
    assert b_pb.spec.env_type == model_pb2.BalancerSpec.L7_ENV_PRODUCTION

    req_pb.meta.version = b_pb.meta.version
    req_pb.spec.env_type = model_pb2.BalancerSpec.L7_ENV_TESTING

    with pytest.raises(exceptions.BadRequestError, match='can only be changed by root'):
        call(balancer_service.update_balancer, req_pb, LOGIN)

    req_pb.spec.env_type = model_pb2.BalancerSpec.L7_ENV_UNKNOWN

    with pytest.raises(exceptions.BadRequestError, match='can only be changed by root'):
        call(balancer_service.update_balancer, req_pb, LOGIN)

    req_pb.spec.env_type = model_pb2.BalancerSpec.L7_ENV_PRESTABLE

    with pytest.raises(exceptions.BadRequestError, match='can only be changed by root'):
        call(balancer_service.update_balancer, req_pb, LOGIN)


def test_cancel_balancer_operation(zk_storage, cache, create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    cert_id = 'anonymous.in.yandex-team.ru'
    cert_pb = model_pb2.Certificate()
    cert_pb.meta.id = cert_id
    cert_pb.meta.namespace_id = 'xxx'
    create_default_namespace(cert_pb.meta.namespace_id)
    cert_pb.spec.fields.subject_common_name = 'anonymous.in.yandex-team.ru'
    zk_storage.create_cert(namespace_id='xxx', cert_id=cert_id, cert_pb=cert_pb)
    wait_until_passes(lambda: cache.must_get_cert('xxx', cert_id))

    req_pb = api_pb2.CreateBalancerRequest()
    req_pb.meta.id = 'xxx_sas'
    req_pb.meta.namespace_id = 'xxx'
    req_pb.meta.auth.type = req_pb.meta.auth.STAFF
    req_pb.meta.location.type = req_pb.meta.location.YP_CLUSTER
    req_pb.meta.location.yp_cluster = 'SAS'
    req_pb.spec.config_transport.nanny_static_file.service_id = 'es_balancer'
    req_pb.spec.type = model_pb2.YANDEX_BALANCER
    req_pb.spec.yandex_balancer.mode = model_pb2.YandexBalancerSpec.EASY_MODE
    req_pb.spec.yandex_balancer.yaml = L7_MACRO
    call(balancer_service.create_balancer, req_pb, LOGIN)

    op_req_pb = api_pb2.CreateBalancerOperationRequest()
    op_req_pb.meta.id = 'xxx_sas'
    op_req_pb.meta.namespace_id = 'xxx'
    op_req_pb.order.create_system_backend.SetInParent()
    call(balancer_service.create_balancer_operation, op_req_pb, LOGIN)

    for op_pb in zk_storage.update_balancer_operation('xxx', 'xxx_sas'):
        op_pb.order.status.status = OverallStatus.FINISHED.name
    assert wait_until(lambda: (cache.must_get_balancer_operation('xxx', 'xxx_sas').order.status.status ==
                               OverallStatus.FINISHED.name),
                      timeout=1)
    req_pb = api_pb2.CancelBalancerOperationRequest()
    req_pb.id = 'xxx_sas'
    req_pb.namespace_id = 'xxx'
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel operation that is not in progress'):
        call(balancer_service.cancel_balancer_operation, req_pb, LOGIN)

    for op_pb in zk_storage.update_balancer_operation('xxx', 'xxx_sas'):
        op_pb.order.status.status = OverallStatus.IN_PROGRESS.name
    assert wait_until(lambda: (cache.must_get_balancer_operation('xxx', 'xxx_sas').order.status.status ==
                               OverallStatus.IN_PROGRESS.name))
    with pytest.raises(exceptions.BadRequestError, match='Cannot cancel balancer operation at this stage'):
        call(balancer_service.cancel_balancer_operation, req_pb, LOGIN)

    for op_pb in zk_storage.update_balancer_operation('xxx', 'xxx_sas'):
        op_pb.order.progress.state.id = 'STARTED'
    assert wait_until(lambda: (
        cache.must_get_balancer_operation('xxx', 'xxx_sas').order.progress.state.id == 'STARTED'))
    call(balancer_service.cancel_balancer_operation, req_pb, LOGIN)

    def check():
        get_req_pb = api_pb2.GetBalancerOperationRequest(namespace_id='xxx', id='xxx_sas')
        pb = call(balancer_service.get_balancer_operation, get_req_pb, LOGIN)
        assert pb.operation.order.cancelled.value

    wait_until_passes(check)


def draft_component(cache, type, version, sb_task_type, sb_task_id, sb_resource_type, sb_resource_id, login):
    draft_req_pb = api_pb2.DraftComponentRequest()
    draft_req_pb.type = type
    draft_req_pb.version = version
    draft_req_pb.startrek_issue_key = 'SWATOPS-112'
    draft_req_pb.spec.source.sandbox_resource.task_id = sb_task_id
    draft_req_pb.spec.source.sandbox_resource.task_type = sb_task_type
    draft_req_pb.spec.source.sandbox_resource.resource_id = sb_resource_id
    draft_req_pb.spec.source.sandbox_resource.resource_type = sb_resource_type
    call(component_service.draft_component, draft_req_pb, login)
    assert wait_until(lambda: (cache.must_get_component(type, version).status.status ==
                               model_pb2.ComponentStatus.DRAFTED))


def publish_component(cache, type, version, login):
    publ_req_pb = api_pb2.PublishComponentRequest()
    publ_req_pb.type = type
    publ_req_pb.version = version
    publ_req_pb.message = 'Publish {}'.format(version)
    call(component_service.publish_component, publ_req_pb, login)
    assert wait_until(lambda: (cache.must_get_component(type, version).status.status ==
                               model_pb2.ComponentStatus.PUBLISHED))


def test_balancer_components(cache, checker, create_default_namespace):
    set_login_to_root_users(LOGIN)
    set_login_to_root_users(LOGIN2)
    create_default_namespace(NS_ID)
    PGINX_VERSIONS = ('178-1', '185-4', '185-5')
    INSTANCECTL_VERSIONS = ('2.0', '2.1')

    req_pb = create_balancer_req()
    req_pb.spec.components.pginx_binary.version = PGINX_VERSIONS[0]

    with pytest.raises(exceptions.BadRequestError, match='"spec.components" must not be set'):
        call(balancer_service.create_balancer, req_pb, LOGIN)

    req_pb.spec.ClearField('components')
    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.id = BALANCER_ID
    req_pb.meta.namespace_id = NS_ID
    req_pb.meta.version = b_pb.meta.version

    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.components.pginx_binary.version = PGINX_VERSIONS[0]
    with pytest.raises(exceptions.BadRequestError,
                       match='"spec.components.pginx_binary.version" must not be set if state is UNKNOWN'):
        call(balancer_service.update_balancer, req_pb, LOGIN)

    req_pb.spec.components.pginx_binary.state = req_pb.spec.components.pginx_binary.REMOVED
    with pytest.raises(exceptions.BadRequestError,
                       match='"spec.components.pginx_binary.version" must not be set if state is REMOVED'):
        call(balancer_service.update_balancer, req_pb, LOGIN)

    req_pb.spec.components.pginx_binary.state = req_pb.spec.components.pginx_binary.SET
    with pytest.raises(exceptions.NotFoundError, match='"spec.components.pginx_binary": component PGINX_BINARY '
                                                       'with version {} does not exist'.format(PGINX_VERSIONS[0])):
        call(balancer_service.update_balancer, req_pb, LOGIN)

    for version in PGINX_VERSIONS:
        draft_component(cache, model_pb2.ComponentMeta.PGINX_BINARY, version,
                        'BUILD_BALANCER_BUNDLE', '637214020', 'BALANCER_EXECUTABLE', '1416163086', LOGIN)
    for version in INSTANCECTL_VERSIONS:
        draft_component(cache, model_pb2.ComponentMeta.INSTANCECTL, version,
                        'BUILD_INSTANCE_CTL', '639956062', 'INSTANCECTL', '1423168226', LOGIN)

    for check in checker:
        with check:
            with pytest.raises(exceptions.BadRequestError, match='"spec.components.pginx_binary": only published '
                                                                 'components can be used in balancers'):
                call(balancer_service.update_balancer, req_pb, LOGIN)

    for version in PGINX_VERSIONS:
        publish_component(cache, model_pb2.ComponentMeta.PGINX_BINARY, version, LOGIN2)
    for version in INSTANCECTL_VERSIONS:
        publish_component(cache, model_pb2.ComponentMeta.INSTANCECTL, version, LOGIN2)

    for check in checker:
        with check:
            b_pb = call(balancer_service.update_balancer, req_pb, LOGIN).balancer

    assert b_pb.spec.components.pginx_binary.version == PGINX_VERSIONS[0]

    req_pb.meta.version = b_pb.meta.version
    req_pb.spec.components.instancectl.version = ''
    req_pb.spec.components.instancectl.state = req_pb.spec.components.instancectl.SET

    for check in checker:
        with check:
            with pytest.raises(exceptions.BadRequestError, match='"spec.components.instancectl.version": is required'):
                call(balancer_service.update_balancer, req_pb, LOGIN)

    retire_req_pb = api_pb2.RetireComponentRequest()
    retire_req_pb.type = model_pb2.ComponentMeta.PGINX_BINARY
    retire_req_pb.version = PGINX_VERSIONS[0]
    retire_req_pb.superseded_by = PGINX_VERSIONS[1]
    for check in checker:
        with check:
            with pytest.raises(exceptions.BadRequestError, match='Component PGINX_BINARY of version {} is used in '
                                                                 'balancer "{}:{}"'.format(PGINX_VERSIONS[0], NS_ID,
                                                                                           BALANCER_ID)):
                call(component_service.retire_component, retire_req_pb, LOGIN)


def set_comp(component_pb, version):
    component_pb.state = component_pb.SET
    component_pb.version = version


def remove_comp(component_pb):
    component_pb.state = component_pb.REMOVED
    component_pb.version = u''


def test_balancer_awacslet_related_components(cache, create_default_namespace):
    set_login_to_root_users(LOGIN)
    set_login_to_root_users(LOGIN2)
    create_default_namespace(NS_ID)
    with mock.patch.object(component_service, u'check_and_complete_sandbox_resource'):
        draft_component(cache, model_pb2.ComponentMeta.AWACSLET, u'0.0.1',
                        u'YA_MAKE', u'123', u'AWACSLET_BINARY', u'456', LOGIN)
        draft_component(cache, model_pb2.ComponentMeta.AWACSLET_GET_WORKERS_PROVIDER, u'0.0.1',
                        u'NANNY_REMOTE_COPY_RESOURCE', u'123', u'AWACSLET_GET_WORKERS_PROVIDER', u'456', LOGIN)
        for version in (u'2.1', u'2.8'):
            draft_component(cache, model_pb2.ComponentMeta.INSTANCECTL, version,
                            u'BUILD_INSTANCE_CTL', u'123', u'INSTANCECTL', u'456', LOGIN)

    publish_component(cache, model_pb2.ComponentMeta.AWACSLET, u'0.0.1', LOGIN2)
    publish_component(cache, model_pb2.ComponentMeta.AWACSLET_GET_WORKERS_PROVIDER, u'0.0.1', LOGIN2)
    publish_component(cache, model_pb2.ComponentMeta.INSTANCECTL, u'2.1', LOGIN2)
    publish_component(cache, model_pb2.ComponentMeta.INSTANCECTL, u'2.8', LOGIN2)

    req_pb = create_balancer_req()
    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.CopyFrom(b_pb.meta)
    req_pb.spec.CopyFrom(b_pb.spec)
    set_comp(req_pb.spec.components.awacslet, u'0.0.1')
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.update_balancer, req_pb, LOGIN)
    assert six.text_type(e.value) == u'"spec.components": AWACSLET_GET_WORKERS_PROVIDER must be set if AWACSLET is set'

    set_comp(req_pb.spec.components.awacslet_get_workers_provider, u'0.0.1')
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.update_balancer, req_pb, LOGIN)
    assert six.text_type(e.value) == u'"spec.components": INSTANCECTL must be set if AWACSLET is set'

    set_comp(req_pb.spec.components.instancectl, u'2.1')
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.update_balancer, req_pb, LOGIN)
    assert six.text_type(e.value) == u'"spec.components": INSTANCECTL_CONF must be removed if AWACSLET is set'

    remove_comp(req_pb.spec.components.instancectl_conf)
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.update_balancer, req_pb, LOGIN)
    assert six.text_type(e.value) == u'"spec.components": GET_WORKERS_PROVIDER must be removed if AWACSLET is set'

    remove_comp(req_pb.spec.components.get_workers_provider)
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.update_balancer, req_pb, LOGIN)
    assert six.text_type(e.value) == u'"spec.components": INSTANCECTL must be >= 2.8 if AWACSLET is set'

    set_comp(req_pb.spec.components.instancectl, u'2.8')
    call(balancer_service.update_balancer, req_pb, LOGIN)

    # change balancer location to GENCFG_DC
    req_pb = create_balancer_req(balancer_id=BALANCER_ID_2, nanny_service_id=u'prod_balancer_2')
    req_pb.meta.location.type = req_pb.meta.location.GENCFG_DC
    req_pb.meta.location.gencfg_dc = u'SAS'
    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.CopyFrom(b_pb.meta)
    req_pb.spec.CopyFrom(b_pb.spec)
    set_comp(req_pb.spec.components.awacslet, u'0.0.1')
    set_comp(req_pb.spec.components.awacslet_get_workers_provider, u'0.0.1')
    set_comp(req_pb.spec.components.instancectl, u'2.8')
    remove_comp(req_pb.spec.components.instancectl_conf)
    remove_comp(req_pb.spec.components.get_workers_provider)
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.update_balancer, req_pb, LOGIN)
    assert six.text_type(e.value) == u'"spec.components.awacslet" can only be set for YP.lite-powered balancers'


def test_auto_added_shawshank_layer(cache, create_default_namespace):
    set_login_to_root_users(LOGIN)
    set_login_to_root_users(LOGIN2)

    create_default_namespace(NS_ID)
    req_pb = create_balancer_req()
    req_pb.spec.container_spec.inbound_tunnels.add().fallback_ip6.SetInParent()
    with pytest.raises(RuntimeError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    assert six.text_type(e.value) == u'shawshank component not found'

    for version in ('0.0.3', '0.0.1'):
        with mock.patch.object(component_service, u'check_and_complete_sandbox_resource'):
            draft_component(cache, model_pb2.ComponentMeta.SHAWSHANK_LAYER, version,
                            u'YA_PACKAGE', u'123', u'SHAWSHANK_LAYER', u'456', LOGIN)
        publish_component(cache, model_pb2.ComponentMeta.SHAWSHANK_LAYER, version, LOGIN2)

    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer
    assert b_pb.spec.components.shawshank_layer.state == b_pb.spec.components.shawshank_layer.SET
    assert b_pb.spec.components.shawshank_layer.version == '0.0.3'

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.CopyFrom(b_pb.meta)
    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.container_spec.ClearField('inbound_tunnels')
    req_pb.spec.components.ClearField('shawshank_layer')
    b_pb = call(balancer_service.update_balancer, req_pb, LOGIN).balancer
    assert b_pb.spec.components.shawshank_layer.state == b_pb.spec.components.shawshank_layer.UNKNOWN

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.CopyFrom(b_pb.meta)
    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.container_spec.inbound_tunnels.add().fallback_ip6.SetInParent()
    b_pb = call(balancer_service.update_balancer, req_pb, NOT_ROOT_NS_OWNER_LOGIN).balancer
    assert b_pb.spec.components.shawshank_layer.state == b_pb.spec.components.shawshank_layer.SET


def test_l3_decap_tunnel(cache, create_default_namespace):
    set_login_to_root_users(LOGIN)
    set_login_to_root_users(LOGIN2)
    create_default_namespace(NS_ID)

    with mock.patch.object(component_service, u'check_and_complete_sandbox_resource'):
        draft_component(cache, model_pb2.ComponentMeta.SHAWSHANK_LAYER, '0.0.0',
                        u'YA_PACKAGE', u'123', u'SHAWSHANK_LAYER', u'456', LOGIN)
    publish_component(cache, model_pb2.ComponentMeta.SHAWSHANK_LAYER, '0.0.0', LOGIN2)

    req_pb = create_balancer_req()
    req_pb.spec.container_spec.inbound_tunnels.add().fallback_ip6.SetInParent()
    b_pb = call(balancer_service.create_balancer, req_pb, LOGIN).balancer

    assert not b_pb.spec.container_spec.outbound_tunnels

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.CopyFrom(b_pb.meta)
    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.container_spec.virtual_ips.add(description='a', ip='127.0.0.1')
    outer_pb = req_pb.spec.container_spec.outbound_tunnels.add(
        id='my-outer',
        mode='IPIP6',
        remote_ip='2a02:6b8:0:3400:0:71d:0:141')
    outer_pb.rules.add(from_ip='5.45.202.162')
    b_pb = call(balancer_service.update_balancer, req_pb, LOGIN).balancer

    assert len(b_pb.spec.container_spec.outbound_tunnels) == 2
    assert b_pb.spec.components.shawshank_layer.state == b_pb.spec.components.shawshank_layer.SET
    assert b_pb.spec.container_spec.outbound_tunnels[0] == outer_pb
    l3_decap = b_pb.spec.container_spec.outbound_tunnels[1]
    assert l3_decap.remote_ip == '2a02:6b8:0:3400::aaaa'
    assert l3_decap.id == 'awacs-l3-decap'
    assert len(l3_decap.rules) == 1
    assert l3_decap.rules[0].from_ip == '127.0.0.1'
    assert wait_until(lambda: cache.get_balancer(NS_ID, BALANCER_ID) == b_pb)

    req_pb.meta.CopyFrom(b_pb.meta)
    for ip in ('2a02:6b8:0:3400:0:71d:0:14c', '127.0.0.2', '5.45.202.162'):
        req_pb.spec.container_spec.virtual_ips.add(description='a', ip=ip)
    b_pb = call(balancer_service.update_balancer, req_pb, LOGIN).balancer

    assert len(b_pb.spec.container_spec.outbound_tunnels) == 2
    assert b_pb.spec.components.shawshank_layer.state == b_pb.spec.components.shawshank_layer.SET
    assert b_pb.spec.container_spec.outbound_tunnels[0] == outer_pb
    l3_decap = b_pb.spec.container_spec.outbound_tunnels[1]
    assert l3_decap.remote_ip == '2a02:6b8:0:3400::aaaa'
    assert l3_decap.id == 'awacs-l3-decap'
    assert len(l3_decap.rules) == 2
    assert l3_decap.rules[0].from_ip == '127.0.0.1'
    assert l3_decap.rules[1].from_ip == '127.0.0.2'
    assert wait_until(lambda: cache.get_balancer(NS_ID, BALANCER_ID) == b_pb)

    req_pb.meta.CopyFrom(b_pb.meta)
    req_pb.spec.container_spec.ClearField('virtual_ips')
    req_pb.spec.container_spec.virtual_ips.add(description='a', ip='2a02:6b8:0:3400:0:71d:0:28c')
    req_pb.spec.container_spec.virtual_ips.add(description='a', ip='127.0.0.3')
    b_pb = call(balancer_service.update_balancer, req_pb, LOGIN).balancer

    assert len(b_pb.spec.container_spec.outbound_tunnels) == 2
    assert b_pb.spec.components.shawshank_layer.state == b_pb.spec.components.shawshank_layer.SET
    assert b_pb.spec.container_spec.outbound_tunnels[0] == outer_pb
    l3_decap = b_pb.spec.container_spec.outbound_tunnels[1]
    assert l3_decap.remote_ip == '2a02:6b8:0:3400::aaaa'
    assert l3_decap.id == 'awacs-l3-decap'
    assert len(l3_decap.rules) == 1
    assert l3_decap.rules[0].from_ip == '127.0.0.3'
    assert wait_until(lambda: cache.get_balancer(NS_ID, BALANCER_ID) == b_pb)

    req_pb.meta.CopyFrom(b_pb.meta)
    req_pb.spec.container_spec.ClearField('virtual_ips')
    b_pb = call(balancer_service.update_balancer, req_pb, LOGIN).balancer

    assert b_pb.spec.components.shawshank_layer.state == b_pb.spec.components.shawshank_layer.SET
    assert len(b_pb.spec.container_spec.outbound_tunnels) == 1
    assert b_pb.spec.container_spec.outbound_tunnels[0] == outer_pb
    assert wait_until(lambda: cache.get_balancer(NS_ID, BALANCER_ID) == b_pb)


def test_create_wo_namespace():
    set_login_to_root_users(LOGIN)
    req_pb = create_balancer_req()
    with pytest.raises(exceptions.NotFoundError) as e:
        call(balancer_service.create_balancer, req_pb, LOGIN)
    assert e.match(u'Namespace "{}" does not exist'.format(NS_ID))


def test_removal_checks(create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    req_pb = create_balancer_req()
    call(balancer_service.create_balancer, req_pb, LOGIN)
    req_pb = api_pb2.GetBalancerRemovalChecksRequest(namespace_id=NS_ID, id=BALANCER_ID)
    resp_pb = call(balancer_service.get_balancer_removal_checks, req_pb, LOGIN)
    (rps_check, endpoint_sets_usage_check, nanny_service_usage_check, gencfg_groups_usage_check, other_balancers_check,
     no_active_operation_check) = resp_pb.checks
    assert rps_check.state == rps_check.UNKNOWN
    assert rps_check.message == ('Could not check if balancer has any traffic, please check it manually and '
                                 'contact support for approval of balancer removal')
    assert endpoint_sets_usage_check.state == endpoint_sets_usage_check.PASSED
    assert endpoint_sets_usage_check.message == 'Balancer endpoint sets are not used in backends'
    assert nanny_service_usage_check.state == nanny_service_usage_check.PASSED
    assert nanny_service_usage_check.message == 'Nanny service is not used in backends'
    assert gencfg_groups_usage_check.state == gencfg_groups_usage_check.PASSED
    assert gencfg_groups_usage_check.message == 'Balancer is Yp.Lite-powered'
    assert other_balancers_check.state == other_balancers_check.PASSED
    assert other_balancers_check.message == 'No other balancers in the namespace are being deleted'
    assert no_active_operation_check.state == no_active_operation_check.PASSED
    assert no_active_operation_check.message == 'No active operation for balancer'


def test_webauth(create_default_namespace):
    set_login_to_root_users(LOGIN)
    create_default_namespace(NS_ID)
    req_pb = create_balancer_req(mode=model_pb2.YandexBalancerSpec.EASY_MODE)
    call(balancer_service.create_balancer, req_pb, LOGIN)

    req_pb = api_pb2.GetBalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)
    resp_pb = call(balancer_service.get_balancer, req_pb, NOT_ROOT_NS_OWNER_LOGIN)
    b_pb = resp_pb.balancer  # type: model_pb2.Balancer

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.CopyFrom(b_pb.meta)
    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.yandex_balancer.yaml = L7_MACRO_WITH_WEBAUTH
    resp_pb = call(balancer_service.update_balancer, req_pb, NOT_ROOT_NS_OWNER_LOGIN)
    b_pb = resp_pb.balancer  # type: model_pb2.Balancer

    assert b_pb.meta.flags.forbid_disabling_webauth.value
    assert b_pb.meta.flags.forbid_disabling_webauth.author == NOT_ROOT_NS_OWNER_LOGIN
    td = b_pb.meta.flags.forbid_disabling_webauth.not_before.ToDatetime() - datetime.utcnow()
    assert timedelta(hours=23) < td <= timedelta(hours=24)

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.CopyFrom(b_pb.meta)
    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.meta.flags.forbid_disabling_webauth.not_before.seconds = 0

    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.update_balancer, req_pb, NOT_ROOT_NS_OWNER_LOGIN)
    assert six.text_type(e.value) == u'"meta.flags" can only be changed by someone with root privileges.'

    resp_pb = call(balancer_service.update_balancer, req_pb, LOGIN)
    b_pb = resp_pb.balancer  # type: model_pb2.Balancer

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.CopyFrom(b_pb.meta)
    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.yandex_balancer.yaml = L7_MACRO
    with pytest.raises(exceptions.BadRequestError) as e:
        call(balancer_service.update_balancer, req_pb, NOT_ROOT_NS_OWNER_LOGIN)
    assert six.text_type(e.value) == u'"webauth": can only be disabled by roots. ' \
                                     u'Please contact support if you absolutely must do it'

    call(balancer_service.update_balancer, req_pb, LOGIN)


def test_rps_limiter(create_default_namespace):
    set_login_to_root_users(LOGIN)
    ns_pb = create_default_namespace(NS_ID)
    req_pb = create_balancer_req(mode=model_pb2.YandexBalancerSpec.EASY_MODE)
    call(balancer_service.create_balancer, req_pb, LOGIN)

    req_pb = api_pb2.GetBalancerRequest(namespace_id=NS_ID, id=BALANCER_ID)
    b_pb = call(balancer_service.get_balancer, req_pb, LOGIN).balancer  # type: model_pb2.Balancer

    for public_installation_name in rps_limiter_settings._public_installation_names:
        req_pb = api_pb2.UpdateBalancerRequest()
        req_pb.meta.CopyFrom(b_pb.meta)
        req_pb.spec.CopyFrom(b_pb.spec)
        req_pb.spec.yandex_balancer.yaml = L7_MACRO_WITH_RPS_LIMITER.format(public_installation_name)
        b_pb = call(balancer_service.update_balancer, req_pb, LOGIN).balancer  # type: model_pb2.Balancer

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.CopyFrom(b_pb.meta)
    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.yandex_balancer.yaml = L7_MACRO_WITH_RPS_LIMITER.format(u'PDB')
    with awtest.raises(exceptions.BadRequestError,
                       text_contains=u'using installation "PDB" is not allowed in this namespace'):
        call(balancer_service.update_balancer, req_pb, LOGIN)

    update_ns_req_pb = api_pb2.UpdateNamespaceRequest()
    update_ns_req_pb.meta.CopyFrom(ns_pb.meta)
    update_ns_req_pb.spec.CopyFrom(ns_pb.spec)
    update_ns_req_pb.spec.rps_limiter_allowed_installations.installations.append(u'PDB')
    call(namespace_service.update_namespace, update_ns_req_pb, LOGIN, enable_auth=True)
    b_pb = call(balancer_service.update_balancer, req_pb, LOGIN).balancer

    req_pb = api_pb2.UpdateBalancerRequest()
    req_pb.meta.CopyFrom(b_pb.meta)
    req_pb.spec.CopyFrom(b_pb.spec)
    req_pb.spec.yandex_balancer.yaml = L7_MACRO_WITH_RPS_LIMITER.format(u'NONEXISTENT')
    with awtest.raises(exceptions.BadRequestError,
                       text_contains=u'installation "NONEXISTENT" doesn\'t exist'):
        call(balancer_service.update_balancer, req_pb, LOGIN)
