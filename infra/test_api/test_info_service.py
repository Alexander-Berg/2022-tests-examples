import pytest
import mock
import six

import inject
from infra.awacs.proto import api_pb2, model_pb2
from awtest.api import Api, call, make_balancer_spec_pb, set_login_to_root_users
from awacs.web import info_service
from awacs.web.validation import balancer as balancer_validation
from awtest.core import raises
from awacs.lib.rpc.exceptions import BadRequestError


LOGIN = 'admin'


@pytest.fixture(autouse=True)
def deps(binder):
    def configure(b):
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@mock.patch.object(balancer_validation, 'validate_create_balancer_request')
@mock.patch.object(balancer_validation, 'validate_balancer_service_and_set_location_if_needed')
@mock.patch.object(balancer_validation, 'validate_balancer_yaml_size')
def test_get_l7_macro_usage(_1, _2, _3):
    from awacs.wrappers.l7macro import VERSION_0_1_0, VERSION_0_2_0, VERSION_0_3_0
    ns_id = "namespace_id"
    Api.create_namespace(ns_id)

    # create 2 full mode balancers
    for i in range(2):
        nanny_service_id = "s-%s" % i
        spec_pb = make_balancer_spec_pb(nanny_service_id=nanny_service_id, mode=model_pb2.YandexBalancerSpec.FULL_MODE)
        balancer_id = "b-full-mode-%s" % i
        Api.create_balancer(ns_id, "b-full-mode-%s" % i, spec_pb)

    # create 2, 4, 6 balancers with corresponding versions of l7macro
    for i, version in enumerate([VERSION_0_1_0, VERSION_0_2_0, VERSION_0_3_0]):
        balancers_total = 2 * (i + 1)
        for j in range(balancers_total):
            nanny_service_id = "s-%s-%s" % (version, j)
            spec_pb = make_balancer_spec_pb(nanny_service_id=nanny_service_id,
                                            mode=model_pb2.YandexBalancerSpec.EASY_MODE,
                                            l7_macro_version=six.text_type(version))
            balancer_id = "b-%s-%s" % (six.text_type(version), j)
            Api.create_balancer(ns_id, balancer_id, spec_pb)

    req_pb = api_pb2.GetL7MacroUsageRequest()
    resp_pb = call(info_service.get_l7_macro_usage, req_pb, LOGIN)

    assert resp_pb.balancer_counts_by_version['none'] == 2
    assert resp_pb.balancer_counts_by_version[six.text_type(VERSION_0_1_0)] == 2
    assert resp_pb.balancer_counts_by_version[six.text_type(VERSION_0_2_0)] == 4
    assert resp_pb.balancer_counts_by_version[six.text_type(VERSION_0_3_0)] == 6


def test_get_normalized_config_from_upstream_yaml():
    req_pb = api_pb2.GetNormalizedConfigFromUpstreamYamlRequest()
    with raises(BadRequestError, '"yaml" or "config" must be set'):
        call(info_service.get_normalized_config_from_upstream_yaml, req_pb, LOGIN)

    easy_mode = '''
l7_upstream_macro:
  version: 0.0.1
  id: upstream_two
  matcher:
    any: true
  static_response:
    status: 421
    content: Bad Request
    '''

    full_mode = '''
regexp_section:
  matcher: {}
  report:
    uuid: upstream_two
    ranges: default
    errordocument:
      status: 421
      content: Bad Request
    '''
    full_mode = full_mode.strip()

    req_pb.yaml = easy_mode
    resp_pb = call(info_service.get_normalized_config_from_upstream_yaml, req_pb, LOGIN)
    assert resp_pb.yaml.strip() == full_mode

    regexp_section = req_pb.config.regexp.sections.add()
    regexp_section.key = 'default'
    regexp_section.value.CopyFrom(resp_pb.config.regexp_section)
    call(info_service.get_normalized_config_from_upstream_yaml, req_pb, LOGIN)

    req_pb.yaml = ''
    with raises(BadRequestError):
        call(info_service.get_normalized_config_from_upstream_yaml, req_pb, LOGIN)

    req_pb.yaml = 'l7_upstream_macro: {}'
    with raises(BadRequestError, 'l7_upstream_macro -> version: is required'):
        call(info_service.get_normalized_config_from_upstream_yaml, req_pb, LOGIN)

    req_pb.config.l7_upstream_macro.SetInParent()
    with raises(BadRequestError, 'l7_upstream_macro -> version: is required'):
        call(info_service.get_normalized_config_from_upstream_yaml, req_pb, LOGIN)

    req_pb.config.instance_macro.SetInParent()
    with raises(BadRequestError, 'instance_macro: at least one of the "include_upstreams", "sections" must be specified'):
        call(info_service.get_normalized_config_from_upstream_yaml, req_pb, LOGIN)


def test_expand_l7_macro_to_instance_macro():
    set_login_to_root_users(LOGIN)
    req_pb = api_pb2.GetNormalizedInstanceMacroFromBalancerYamlRequest()
    with raises(BadRequestError, '"yaml" must be set'):
        call(info_service.expand_l7_macro_to_instance_macro, req_pb, LOGIN)

    easy_mode = '''
l7_macro:
  version: 0.2.8
  http: {}
  health_check_reply: {}
  include_domains: {}
    '''

    with raises(BadRequestError, '"l7_macro.include_domains" is not supported.'):
        req_pb.yaml = easy_mode
        call(info_service.expand_l7_macro_to_instance_macro, req_pb, LOGIN)

    easy_mode = easy_mode.replace('include_domains: {}', '')

    full_mode = r'''
instance_macro:
  version: 0.0.2
  maxconn: 5000
  workers: !f 'get_workers()'
  sections:
    admin:
      ips:
      - 127.0.0.1
      - ::1
      ports:
      - !f 'get_port_var("port")'
      http:
        admin: {}
    http_section:
      ips:
      - '*'
      ports:
      - 80
      extended_http_macro:
        maxlen: 65536
        maxreq: 65536
        report_input_size_ranges: 32,64,128,256,512,1024,4096,8192,16384,131072,524288,1048576,2097152
        report_output_size_ranges: 512,1024,4096,8192,16384,32768,65536,131072,262144,524288,1048576,2097152,4194304,8388608
        yandex_cookie_policy: YCP_STABLE
        regexp:
          sections:
            awacs-balancer-health-check:
              matcher:
                match_fsm:
                  uri: /awacs-balancer-health-check
              errordocument:
                status: 200
            ? ''
            : matcher: {}
              regexp:
                include_upstreams:
                  filter:
                    any: true
                  order:
                    label:
                      name: order
  tcp_listen_queue: 128
  unistat:
    hide_legacy_signals: true
  cpu_limiter:
    active_check_subnet_file: ./controls/active_check_subnets_list
    disable_file: ./controls/cpu_limiter_disabled
  sd: {}
  state_directory: /dev/shm/balancer-state
  dynamic_balancing_log: !f 'get_log_path("dynamic_balancing_log", get_port_var("port"),
    "/place/db/www/logs/")'
  pinger_required: true
  pinger_log: !f 'get_log_path("pinger_log", get_port_var("port"), "/place/db/www/logs/")'
  config_check: {}
    '''
    full_mode = full_mode.strip()

    req_pb.yaml = easy_mode
    resp_pb = call(info_service.expand_l7_macro_to_instance_macro, req_pb, LOGIN)
    assert resp_pb.yaml.strip() == full_mode

    req_pb.yaml = full_mode
    resp_pb = call(info_service.expand_l7_macro_to_instance_macro, req_pb, LOGIN)
    assert resp_pb.yaml.strip() == full_mode

    req_pb.yaml = full_mode.replace('''
      http:
        admin: {}
    '''.strip(), '''
      nested:
        http:
          admin: {}
    '''.strip())
    resp_pb = call(info_service.expand_l7_macro_to_instance_macro, req_pb, LOGIN)
    assert resp_pb.yaml.strip() == full_mode
