# coding: utf-8
import json
import textwrap

import pytest

from awacs.lib.yamlparser.errors import SchemaError
from awacs.lib.yamlparser.wrappers_util import dump_tlem_pb, dump_uem_pb
from infra.awacs.proto import modules_pb2
from awacs.yamlparser.core import parse, dump
from awacs.yamlparser.schemautil import JsonSchemaBuilder
from awacs.wrappers.base import Holder


UPSTREAM_2_YAML = '''main:
  enable_reuse_port: true
  addrs:
    - ip: 127.0.0.4
      port: 16020
    - ip: 127.0.0.1
      port: 16020
    - ip: ::1
      port: 16020
  admin_addrs:
    - ip: 127.0.0.1
      port: 8000
  maxconn: 4000
  buffer: 65536
  log: !f get_log_path('test', 80)
  events:
    Stats: report
  ipdispatch:
    sections:
      xx:
        ips:
          - 127.0.0.4
          - 127.0.0.1
          - ::1
        ports:
          - !f get_port_var("test")
        stats_attr: http
        modules:
          - errorlog:
              log: /usr/local/www/logs/current-error_log-balancer-16021
              log_level: ERROR
          - http:
              stats_attr: http
              keepalive: true
              # no_keepalive_file: ./controls/keepalive_disabled
              no_keepalive_file: !k keepalive_disabled
          - accesslog:
              log: /usr/local/www/logs/current-access_log-balancer-16021
          - report:
              uuid: http
              refers: service_total
              ranges: 'default'
          - hasher:
              mode: 'subnet'
              subnet_v4_mask: 32
              subnet_v6_mask: 128
          - regexp:
              sections:
                main:
                  matcher:
                    match_fsm:
                      uri: /gobabygo(/.*)?
                      case_insensitive: false
                  modules:
                    - headers:
                        create_func:
                          X-Real-IP: realip
                        create_func_weak:
                          X-Forwarded-For: realip
                          X-Req-Id: reqid
                          X-Scheme: scheme
                          X-Source-Port: realport
                    - errordocument:
                        status: 500
      yy:
        ips:
          - 127.0.0.1
        ports: [8000]
        stats_attr: http
        modules:
          - errorlog:
              log: /usr/local/www/logs/current-error_log-balancer-16021
              log_level: ERROR
          - http: {}
          - admin: {}'''

UPSTREAM_4_YAML = '''regexp_section:
  matcher:
    match_fsm:
      path: '/batteries'
  extended_http_macro:
        port: 16102
        report_uuid: https
        enable_ssl: true
        ssl_sni_contexts:
          exp.yandex-team.ru:
            cert: !c exp.yandex-team.ru
            servername_regexp: '(exp|exp-beta|ab)\\\\.test\\\\.yandex-team\\\\.ru'
          ab.yandex-team.ru:
            cert: !c ab.yandex-team.ru
            servername_regexp: default
            secondary_cert_postfix: secondary
  modules:
    - ssl_sni:
        force_ssl: true
        contexts:
          default:
            cert: !c cert'''

UPSTREAM_5_YAML = '''regexp_section:
  matcher:
    match_fsm:
      path: '/batteries'
  extended_http_macro:
        port: 16102
        report_uuid: https
        enable_ssl: true
        ssl_sni_contexts:
          exp.yandex-team.ru:
            cert: !c exp.yandex-team.ru
            servername_regexp: '(exp|exp-beta|ab)\\\\.test\\\\.yandex-team\\\\.ru'
          ab.yandex-team.ru:
            cert: !c ab.yandex-team.ru
            servername_regexp: default
            secondary_cert: !c ab1.yandex-team.ru
  modules:
    - ssl_sni:
        force_ssl: true
        contexts:
          default:
            cert: !c cert'''


def p(yml):
    return parse(modules_pb2.Holder, yml)


def test_dump():
    for yml in (UPSTREAM_2_YAML, UPSTREAM_4_YAML, UPSTREAM_5_YAML):
        h_pb = p(yml)
        assert h_pb == p(dump(h_pb))


def test_regression_swat_4487():
    # Beware of this protobuf magic:
    # For details please see
    # https://github.com/google/protobuf/blob/0400cca3236de1ca303af38bf81eab332d042b7c/python/google/protobuf/internal/message_test.py#L662-L676
    # https://github.com/google/protobuf/issues/491
    pb = modules_pb2.ExtendedHttpMacro()
    field_desc = pb.DESCRIPTOR.fields_by_name['port']
    assert field_desc.type == field_desc.TYPE_INT32
    assert not pb.HasField('port')
    assert pb.port == 0
    assert not pb.HasField('port')
    pb.port = 0
    assert pb.HasField('port')
    # /magic

    yml1 = 'extended_http_macro: {}'
    yml2 = 'extended_http_macro: {port: 0}'
    pb1 = p(yml1)  # type: modules_pb2.Holder
    pb2 = p(yml2)  # type: modules_pb2.Holder
    assert pb1 == pb2

    pb1.extended_http_macro.port = 0
    assert pb1.extended_http_macro.HasField('port')
    assert 'port' not in dump(pb1)


SWAT_4667_YAML = '''headers:
  create_func:
    Authorization: !f get_str_env_var("IDM_TOKEN")'''


def test_regression_swat_4667():
    with pytest.raises(SchemaError) as e:
        p(SWAT_4667_YAML)
    assert e.match('map value does not accept a function call')


SWAT_4678_YAML = '''errordocument: {status: 200}
---
errordocument: {status: 200}'''


def test_regression_swat_4678():
    with pytest.raises(SchemaError) as e:
        p(SWAT_4678_YAML)
    assert e.match('line 2, column 1: document is not accepted here, no further data expected')


@pytest.mark.skip
def test_generate_jsonschema():
    b = JsonSchemaBuilder()
    schema = b.build_schema(modules_pb2.Holder.DESCRIPTOR)

    with open('./tests/fixtures/schema.json') as f:
        expected_schema = json.load(f)

    if schema != expected_schema:
        with open('./tests/fixtures/schema.json.new', 'w') as f:
            json.dump(schema, f, indent=4, sort_keys=True)

    assert schema == expected_schema


def test_tlem_dump():
    holder_pb = modules_pb2.Holder()
    l7_macro_pb = holder_pb.l7_macro
    l7_macro_pb.version = '0.0.5'
    l7_macro_pb.include_domains.SetInParent()
    l7_macro_pb.health_check_reply.SetInParent()
    l7_macro_pb.announce_check_reply.url_re = '/ping'
    l7_macro_pb.http.SetInParent()
    l7_macro_pb.https.SetInParent()
    header = l7_macro_pb.headers.add()
    header.create.target = 'X-Forwarded-For-Y'
    header.create.func = 'realip'
    header.create.keep_existing = True
    response_header = l7_macro_pb.response_headers.add()
    response_header.append.target = 'X-Forwarded-For-Y'
    response_header.append.value = '127.0.0.1'
    response_header.append.do_not_create_if_missing = True
    assert dump_tlem_pb(holder_pb) == textwrap.dedent('''l7_macro:
  version: 0.0.5
  http: {}
  https: {}
  announce_check_reply:
    url_re: /ping
  health_check_reply: {}
  headers:
    - create: {target: X-Forwarded-For-Y, keep_existing: true, func: realip}
  response_headers:
    - append: {target: X-Forwarded-For-Y, do_not_create_if_missing: true, value: 127.0.0.1}
  include_domains: {}
  ''')


def test_uem_dump():
    holder_pb = modules_pb2.Holder()
    l7_uem = holder_pb.l7_upstream_macro
    l7_uem.id = 'fancy_id'
    l7_uem.version = '0.0.4'
    l7_uem.monitoring.uuid = 'd-u'
    l7_uem.matcher.path_re = '/123/ads/'
    dc_balancer = l7_uem.by_dc_scheme.dc_balancer
    dc_balancer.weights_section_id = 'bygeo'
    dc_balancer.attempts = 8
    dc_balancer.method = modules_pb2.L7UpstreamMacro.DcBalancerSettings.LOCAL_THEN_BY_DC_WEIGHT
    balancer = l7_uem.by_dc_scheme.balancer
    balancer.compat.method = modules_pb2.L7UpstreamMacro.BalancerSettings.Compat.RR
    balancer.do_not_retry_http_responses = True
    balancer.retry_non_idempotent.value = False
    balancer.max_reattempts_share = 0.15
    balancer.fast_attempts = 2
    balancer.backend_read_timeout = '500ms'
    balancer.backend_write_timeout = '999m'
    balancer.allow_connection_upgrade = True
    balancer.connect_timeout = balancer.connect_timeout or '100ms'
    balancer.backend_timeout = balancer.backend_timeout or '5s'
    balancer.attempts = balancer.attempts or 1
    for i in range(3):
        dc = l7_uem.by_dc_scheme.dcs.add()
        backend_id = 'es{}_{}'.format(i, i + 1)
        dc.name = str(i)
        dc.backend_ids.extend([backend_id])
    dc_balancer.attempts = 3
    static_on_error = l7_uem.by_dc_scheme.on_error.static
    static_on_error.status = 504
    static_on_error.content = 'Service unavailable'
    assert dump_uem_pb(holder_pb) == textwrap.dedent('''l7_upstream_macro:
  version: 0.0.4
  id: fancy_id
  matcher:
    path_re: /123/ads/
  monitoring:
    uuid: d-u
  by_dc_scheme:
    dc_balancer:
      weights_section_id: bygeo
      method: LOCAL_THEN_BY_DC_WEIGHT
      attempts: 3
    balancer:
      compat:
        method: RR
      attempts: 1
      max_reattempts_share: 0.15
      fast_attempts: 2
      do_not_retry_http_responses: true
      retry_non_idempotent: false
      connect_timeout: 100ms
      backend_timeout: 5s
      backend_read_timeout: 500ms
      backend_write_timeout: 999m
      allow_connection_upgrade: true
    dcs:
      - name: '0'
        backend_ids:
          - es0_1
      - name: '1'
        backend_ids:
          - es1_2
      - name: '2'
        backend_ids:
          - es2_3
    on_error:
      static:
        status: 504
        content: 'Service unavailable'
  ''')


def test_to_normal_form_XXX():
    pb = p('''
modules:
- http: {}
- ssl_sni: {}
- accesslog: {}
- admin: {}
''')
    h = Holder(pb)
    h.to_normal_form_XXX()
    assert dump(h.pb) == '''http:
  ssl_sni:
    accesslog:
      admin: {}
'''

    pb = p('''
modules:
- admin: {}
''')
    h = Holder(pb)
    h.to_normal_form_XXX()
    assert dump(h.pb) == '''admin: {}
'''
    pb = p('''
modules:
- geobase:
    geo:
      modules:
      - report: {}
      - errordocument: {}
- http: {}
- ssl_sni: {}
- accesslog: {}
- admin: {}
''')
    h = Holder(pb)
    h.to_normal_form_XXX()
    assert dump(h.pb) == '''geobase:
  geo:
    report:
      errordocument: {}
  http:
    ssl_sni:
      accesslog:
        admin: {}
'''
