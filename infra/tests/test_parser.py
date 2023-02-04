# coding: utf-8
import pytest
import mock

from infra.awacs.proto import modules_pb2 as proto, modules_pb2
from awacs import yamlparser
from awacs.lib.yamlparser.errors import SchemaError, YamlSyntaxError, NonAsciiCharsPresent
from awacs.wrappers.base import Holder, ANY_MODULE
from awacs.wrappers.errors import ValidationError
from awacs.wrappers.main import Ip, Call, Main
from awtest import t


def parse(yaml, cls=proto.Holder):
    return yamlparser.parse(cls, yaml, ensure_ascii=True)


def parse2(yaml):
    pb = parse(yaml)
    parsed_unparsed_pb = parse(yamlparser.dump(pb))
    assert pb == parsed_unparsed_pb
    return pb


def test_parser_1():
    with open(t('fixtures/config_1.yml')) as f:
        document = f.read()

    pb = parse2(document)
    m = Holder(pb)
    m.validate()
    assert m.module_name == 'main'
    m.expand_macroses()
    m.module.to_config().to_lua()


def test_parser_2():
    with open(t('fixtures/config_2.yml')) as f:
        document = f.read()

    pb = parse2(document)
    m = Holder(pb)
    m.validate()
    assert m.module_name == 'main'
    m.expand_macroses()
    m.module.to_config().to_top_level_lua()


def test_parser_3():
    with open(t('fixtures/config_3.yml')) as f:
        document = f.read()

    pb = parse2(document)
    m = Holder(pb)
    m.validate()
    m.expand_macroses()
    m.to_config(preceding_modules=[Main(modules_pb2.MainModule())]).to_lua()


def test_parser_4():
    with open(t('fixtures/config_4.yml')) as f:
        document = f.read()

    pb = parse2(document)
    m = Holder(pb)
    m.validate()
    m.expand_macroses()

    _, config = m.chain.to_config()
    config.to_top_level_lua()


def test_parser_5():
    with open(t('fixtures/config_5.yml')) as f:
        document = f.read()

    pb = parse2(document)
    m = Holder(pb)
    m.validate(preceding_modules=[ANY_MODULE])
    m.expand_macroses()
    m.module.to_config(priority=1).to_top_level_lua()


def test_parser_6():
    with open(t('fixtures/config_6.yml')) as f:
        document = f.read()

    pb = parse2(document)
    m = Holder(pb)
    m.validate()
    m.expand_macroses()
    m.to_config().to_top_level_lua()


def test_parser_awacs_659():
    pb = modules_pb2.Holder()
    pb.l7_upstream_macro.flat_scheme.balancer.do_not_retry_http_responses = False
    parsed_unparsed_pb = parse(yamlparser.dump(pb))
    """
    print pb

    l7_upstream_macro {
      flat_scheme {
        balancer {
          do_not_retry_http_responses: false
        }
      }
    }

    print parsed_unparsed_pb

    l7_upstream_macro {
      flat_scheme {
        balancer {
        }
      }
    }
    """
    assert pb != parsed_unparsed_pb  # XXX


def test_knobs_parsing():
    document = """
balancer2:
    rr: {
      weights_file: !k test
    }
    attempts: 3
    backends:
      - weight: 100
        errordocument: {}
    """

    pb = parse2(document)
    m = Holder(pb)
    with pytest.raises(ValidationError) as e:
        m.validate()
    e.match('balancer2 -> rr -> weights_file: knobs are not allowed for this balancer')


def test_enum_parsing():
    document = """
type: BY_ID
ids:
  - a
  - b
    """

    pb = parse(document, proto.IncludeUpstreams)
    assert pb.type == proto.BY_ID
    assert pb.ids == ['a', 'b']

    document = """type: PUMPURUM"""
    with pytest.raises(SchemaError) as e:
        parse(document, proto.IncludeUpstreams)
    e.match('line 1, column 7: failed to parse "type": '
            'enum type "awacs.modules.IncludeUpstreamsType" has no value named PUMPURUM')


def test_oneofs_1():
    document = """
modules:
  - rewrite:
      actions: []
    headers:
      create_func:
        X-Real-IP: realip
    """
    with pytest.raises(SchemaError) as e:
        parse(document)
    e.match('"headers" and "rewrite" cannot both be present in Holder')


def test_oneofs_2():
    document = """
modules:
  - balancer2:
      attempts: 3
      by_name_policy:
        name: !f get_geo("informers_", "random")
        unique_policy: {}
      rr: {weights_file: ./controls/traffic_control.weights}
      timeout_policy:
        timeout: 100ms
        unique_policy: {}
    """
    with pytest.raises(SchemaError) as e:
        parse(document)
    e.match('"timeout_policy" and "by_name_policy" cannot both be present in Balancer2Module')


def test_oneofs_3():
    document = """
modules:
  - balancer2:
      attempts: 3
      rr: {weights_file: ./controls/traffic_control.weights}
      weighted2: {}
    """
    with pytest.raises(SchemaError) as e:
        parse(document)
    e.match('"weighted2" and "rr" cannot both be present in Balancer2Module')


def test_func_params():
    document = """
main:
    workers: 10
    maxconn: 20
    log: !f get_log_path("childs_log", 13251)
    addrs:
        - ip: !f get_ip_by_iproute("v4")
          port: 8081
    ipdispatch:
        sections:
            test:
                ips: [!f get_ip_by_iproute("v4")]
                ports:
                    - 8081
                shared: {uuid: "nevermind"}
    """.strip()
    pb = parse2(document)
    h = Holder(pb)

    with mock.patch.object(h.module.nested.module.sections['test'].nested, 'validate'):
        h.validate()
    from awacs.wrappers.config import Config

    with mock.patch.object(h.module.nested.module.sections['test'], 'to_config', return_value=Config()):
        h.to_config().to_top_level_lua()


def test_call_param():
    document = """
value: 127.0.0.1
f_value: !f g("ip")
    """
    with pytest.raises(SchemaError) as e:
        parse(document, proto.IpdispatchSection.Ip)
    e.match('"f_value" and "value" cannot both be present in Ip')
    assert e.value.get_snippet(max_length=1000) == '\n'.join([
        'value: 127.0.0.1',
        'f_value: !f g("ip")',
        '^',
    ])

    document = """
value: 127.0.0.1
    """
    ip_pb = parse(document, proto.IpdispatchSection.Ip)
    ip = Ip(ip_pb)
    ip.validate()
    value = ip.get('value')
    assert not value.is_func()
    assert value.value == '127.0.0.1'

    document = """
value: !f unknown('xxx')
    """
    with pytest.raises(SchemaError) as e:
        parse(document, proto.IpdispatchSection.Ip)
    e.match('unknown function "unknown"')
    assert e.value.get_snippet(max_length=1000) == '\n'.join([
        "value: !f unknown('xxx')",
        '       ^',
    ])

    document = """
    value: !f get_log_path("xxx")
    """
    ip_pb = parse(document, proto.IpdispatchSection.Ip)

    ip = Ip(ip_pb)
    with pytest.raises(ValidationError) as e:
        ip.validate()
    e.match('only the following functions allowed here: "get_ip_by_iproute"')

    document = """
    value: !f get_ip_by_iproute('xxx')
    """
    with pytest.raises(SchemaError) as e:
        parse(document, proto.IpdispatchSection.Ip)
    e.match(r"get_ip_by_iproute's 1st argument \(family\) must be one of the following: 'v4', 'v6'")

    document = """
    value: !f get_ip_by_iproute('v4')
    """
    ip_pb = parse(document, proto.IpdispatchSection.Ip)
    ip = Ip(ip_pb)
    ip.validate()

    value = ip.get('value')
    assert value.is_func()
    assert isinstance(value.value, Call)
    assert str(value.value) == 'get_ip_by_iproute("v4")'

    document = """
    value: 127.0.0.1
    """
    ip_pb = parse(document, proto.IpdispatchSection.Ip)
    ip = Ip(ip_pb)
    ip.validate()

    value = ip.get('value')
    assert not value.is_func()
    assert value.value == '127.0.0.1'


def test_error_messages():
    document = """{"""
    with pytest.raises(YamlSyntaxError) as e:
        parse(document, proto.Holder)
    e.match("expected the node content, but found '<stream end>'")
    assert e.value.get_snippet() == '\n'.join([
        '{',
        ' ^'
    ])

    document = """
    regexp:
      sections:
        section_id: []
    """.strip()
    with pytest.raises(SchemaError) as e:
        parse(document, proto.Holder)
    e.match('sequence is not accepted here, RegexpSection expected')

    assert e.value.get_snippet(max_length=256, max_lines=2) == '\n'.join([
        '      sections:',
        '        section_id: []',
        '                    ^',
    ])
    assert e.value.get_snippet(max_length=20) == '\n'.join([
        'ction_id: []',
        '          ^'
    ])

    document = """
    regexp:
      sections:
        section_id: {}
        section_id: {}
    """.strip()
    with pytest.raises(SchemaError) as e:
        parse(document, proto.Holder)
    e.match('duplicate key "section_id"')
    assert e.value.get_snippet(max_length=1000) == '\n'.join([
        'regexp:',
        '      sections:',
        '        section_id: {}',
        '        section_id: {}',
        '        ^'
    ])

    document = "{log: 123, log: '456'}"
    with pytest.raises(SchemaError) as e:
        parse(document, proto.MainModule)
    e.match('duplicate field "log"')
    assert e.value.get_snippet(max_length=1000) == '\n'.join([
        "{log: 123, log: '456'}",
        '           ^'
    ])

    document = """
    regexp:
      sections:
        section_id:
    """.strip()
    with pytest.raises(SchemaError) as e:
        parse(document, proto.Holder)
    e.match('scalar is not accepted here, RegexpSection expected')
    assert e.value.get_snippet(max_length=1000, max_lines=2) == '\n'.join([
        '      sections:',
        '        section_id:',
        '                   ^',
    ])
    assert e.value.get_snippet(max_length=50) == '\n'.join([
        'ions:',  # TODO this is quite ugly
        '        section_id:',
        '                   ^',
    ])

    document = """
    include_upstreams:
    """.strip()
    with pytest.raises(SchemaError) as e:
        parse(document, proto.InstanceMacro)
    e.match('scalar is not accepted here, IncludeUpstreams expected')

    document = """
type: BY_ID
ids:
""".strip()
    with pytest.raises(SchemaError) as e:
        parse(document, proto.IncludeUpstreams)
    e.match('scalar is not accepted here, sequence of string expected')

    document = "modules: {}"
    with pytest.raises(SchemaError) as e:
        parse(document)
    e.match('mapping is not accepted here, sequence of Holder expected')

    document = "workers: {}"
    with pytest.raises(SchemaError) as e:
        parse(document, proto.MainModule)
    e.match('mapping is not accepted here, int32 expected')

    document = "log: {}"
    with pytest.raises(SchemaError) as e:
        parse(document, proto.MainModule)
    e.match('mapping is not accepted here, string expected')

    document = "ports: {}"
    with pytest.raises(SchemaError) as e:
        parse(document, proto.IpdispatchSection)
    e.match('mapping is not accepted here, sequence of Port expected')

    document = "[]"
    with pytest.raises(SchemaError) as e:
        parse(document, proto.MainModule)
    e.match('sequence is not accepted here, MainModule expected')

    document = "events: []"
    with pytest.raises(SchemaError) as e:
        parse(document, proto.MainModule)
    e.match('sequence is not accepted here, mapping expected')

    document = "http: []"
    with pytest.raises(SchemaError) as e:
        parse(document, proto.MainModule)
    e.match('sequence is not accepted here, HttpModule expected')

    document = "workers: []"
    with pytest.raises(SchemaError) as e:
        parse(document, proto.MainModule)
    e.match('sequence is not accepted here, int32 expected')

    document = "log: []"
    with pytest.raises(SchemaError) as e:
        parse(document, proto.MainModule)
    e.match('sequence is not accepted here, string expected')

    document = "events: {test: []}"
    with pytest.raises(SchemaError) as e:
        parse(document, proto.MainModule)
    e.match('sequence is not accepted here, string expected')

    document = "events: {test: []}"
    with pytest.raises(SchemaError) as e:
        parse(document, proto.MainModule)
    e.match('sequence is not accepted here, string expected')

    document = "include_backends: {type: [ugc_db_backend_test_sas]}"
    with pytest.raises(SchemaError) as e:
        parse(document, proto.GeneratedProxyBackends)
    e.match('sequence is not accepted here, one of the following values: "NONE", "BY_ID" expected')


def test_parser_with_unicode():
    document = u'''
#comment
l7_macro:
  version: 0.2.0
  http: {}
  announce_check_reply:
    url_re: /ping
  headers:
    - create: {
      target: Х-Переслано-Для-Я, func: realip
    }
'''
    with pytest.raises(NonAsciiCharsPresent) as e:
        yamlparser.parse(proto.MainModule, document, ensure_ascii=True)
    mark = e.value.mark
    assert mark.line == 9
    assert mark.column == 14
