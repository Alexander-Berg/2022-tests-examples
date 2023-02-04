# coding: utf-8
import mock
import pytest

from awacs.wrappers.base import Holder
from awacs.wrappers.util import Value
from infra.awacs.proto import modules_pb2
from awacs.wrappers.errors import ValidationError
from awacs.wrappers.main import InstanceMacro, IpdispatchSection, Main, Ipdispatch, Admin, ErrorDocument

from awtest.wrappers import get_validation_exception


def test_instance_macro_primitive_fields_validation():
    pb = modules_pb2.InstanceMacro(maxconn=-10)
    pb.include_upstreams.SetInParent()

    m = InstanceMacro(pb)
    with pytest.raises(ValidationError) as e:
        m.validate()
    assert e.match('maxconn: must be non-negative')

    pb.maxconn = 10
    pb.buffer = -10
    with pytest.raises(ValidationError) as e:
        m.validate()
    assert e.match('buffer: must be non-negative')

    pb.buffer = 10
    pb.workers = -10
    with pytest.raises(ValidationError) as e:
        m.validate()
    assert e.match('workers: must be positive or zero')

    pb.workers = 0
    for addr in ('8.8.8.8', '2a02:6b8:0:3400::1075'):
        pb.private_address = addr
        with pytest.raises(ValidationError) as e:
            m.validate()
        assert e.match('private_address: must be local v4 address')

    pb.private_address = '127.0.0.1'
    pb.tcp_fastopen = -10
    with pytest.raises(ValidationError) as e:
        m.validate()
    assert e.match('tcp_fastopen: must be non-negative')

    pb.tcp_fastopen = 0
    with mock.patch.object(InstanceMacro, 'validate_composite_fields'):
        m.validate()

    pb.pinger_required = True
    with pytest.raises(ValidationError) as e:
        m.validate()
    assert e.match('state_directory: must be set if pinger_required is set')

    pb.state_directory = '/dev/shm/balancer-state'
    m.validate()


def test_instance_macro_composite_fields_validation():
    pb = modules_pb2.Holder()
    pb.instance_macro.SetInParent()

    h = Holder(pb)

    # test an empty macro without any ipdispatch sections
    with pytest.raises(ValidationError) as e:
        h.validate()
    assert e.match('instance_macro: at least one of the "include_upstreams", "sections" must be specified')

    # add first section
    section_1_pb = pb.instance_macro.sections.add()
    section_1_pb.key = 'slb_ping'
    section_1_pb.value.ips.add(value='127.0.0.1')
    section_1_pb.value.ports.add(value=80)
    h.update_pb(pb)

    with mock.patch.object(IpdispatchSection, 'validate'):
        # mock section's validate method as succeeding
        h.validate()

    with mock.patch.object(IpdispatchSection, 'validate', side_effect=ValidationError('BAD')):
        # mock section's validate method as failing
        with pytest.raises(ValidationError) as e:
            h.validate()
        assert e.match(r'instance_macro -> sections\[slb_ping\]: BAD')

    # add second ipdispatch section with the same id
    section_2_pb = pb.instance_macro.sections.add()
    section_2_pb.key = 'slb_ping'
    section_2_pb.value.SetInParent()
    h.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        h.validate()
    assert e.match('instance_macro -> sections: duplicate key "slb_ping"')

    pb.instance_macro.sections.pop()
    pb.instance_macro.sd.SetInParent()
    h.update_pb(pb)
    with mock.patch.object(IpdispatchSection, 'validate'):
        e = get_validation_exception(h.validate)
    e.match('instance_macro -> sd: can not be enabled without unistat')

    pb.instance_macro.unistat.addrs.add(ip='127.0.0.1', port=80)
    h.update_pb(pb)
    with mock.patch.object(IpdispatchSection, 'validate'):
        e = get_validation_exception(h.validate)
    e.match('instance_macro: unistat addrs and section "slb_ping" addrs intersect')

    pb.instance_macro.unistat.addrs[0].port = 888
    h.update_pb(pb)
    with mock.patch.object(IpdispatchSection, 'validate'):
        h.validate()


def test_addrs_intersection():
    pb = modules_pb2.Holder()
    pb.instance_macro.SetInParent()

    h = Holder(pb)
    section_1_pb = pb.instance_macro.sections.add()
    section_1_pb.key = 'slb_ping'
    section_1_pb.value.SetInParent()

    section_2_pb = pb.instance_macro.sections.add()
    section_2_pb.key = 'default'
    section_2_pb.value.SetInParent()

    h.update_pb(pb)

    port_9000 = Value(Value.VALUE, 9000)

    section_1_addrs = [(Value(Value.VALUE, '127.0.0.1'), port_9000), (Value(Value.VALUE, '::1'), port_9000)]
    section_2_addrs = [(Value(Value.VALUE, '*'), 9000)]
    with mock.patch.object(IpdispatchSection, 'validate'):
        with mock.patch.object(h.module.sections['slb_ping'], 'list_addrs', return_value=section_1_addrs), \
             mock.patch.object(h.module.sections['default'], 'list_addrs', return_value=section_2_addrs):
            h.validate()

    section_1_addrs = [(Value(Value.VALUE, '127.0.0.1'), port_9000)]
    section_2_addrs = list(section_1_addrs)
    with pytest.raises(ValidationError) as e:
        with mock.patch.object(IpdispatchSection, 'validate'):
            with mock.patch.object(h.module.sections['slb_ping'], 'list_addrs', return_value=section_1_addrs), \
                 mock.patch.object(h.module.sections['default'], 'list_addrs', return_value=section_2_addrs):
                h.validate()
    assert e.match('instance_macro: slb_ping and default sections intersect by addresses: 127.0.0.1:9000')

    section_1_addrs = [(Value(Value.VALUE, '8.8.8.8'), port_9000)]
    section_2_addrs = [(Value(Value.VALUE, '*'), port_9000)]
    with pytest.raises(ValidationError) as e:
        with mock.patch.object(IpdispatchSection, 'validate'):
            with mock.patch.object(h.module.sections['slb_ping'], 'list_addrs', return_value=section_1_addrs), \
                 mock.patch.object(h.module.sections['default'], 'list_addrs', return_value=section_2_addrs):
                h.validate()
    assert e.match('instance_macro: slb_ping and default sections intersect by addresses: 8.8.8.8:9000')


def test_instance_macro_expand():
    pb = modules_pb2.InstanceMacro(
        log_dir='/tmp/',
        maxconn=10,
        workers=20,
        buffer=30,
        enable_reuse_port=True,
        private_address='127.0.0.99',
        tcp_fastopen=40,
        sections=[
            modules_pb2.IpdispatchSectionEntry(
                key='test',
                value=modules_pb2.IpdispatchSection(
                    ips=[modules_pb2.IpdispatchSection.Ip(value='8.8.8.8')],
                    ports=[modules_pb2.IpdispatchSection.Port(value=8000)],
                    local_ips=[modules_pb2.IpdispatchSection.Ip(value='127.0.0.1')],
                    local_ports=[modules_pb2.IpdispatchSection.Port(value=80)],
                    nested=modules_pb2.Holder(admin=modules_pb2.AdminModule())
                )
            ),
            modules_pb2.IpdispatchSectionEntry(
                key='test_2',
                value=modules_pb2.IpdispatchSection(
                    ips=[modules_pb2.IpdispatchSection.Ip(value='8.8.8.8')],
                    ports=[modules_pb2.IpdispatchSection.Port(value=9000)],
                    nested=modules_pb2.Holder(errordocument=modules_pb2.ErrorDocumentModule())
                )
            )
        ]
    )
    pb.default_tcp_rst_on_error.value = True
    m = InstanceMacro(pb)
    res = list(map(Holder, m.expand()))
    assert isinstance(res[0].module, Main)
    main = res[0].module
    assert set((a.pb.ip, a.pb.port) for a in main.admin_addrs) == {('127.0.0.1', 80), ('8.8.8.8', 8000)}
    assert set((a.pb.ip, a.pb.port) for a in main.addrs) == {('8.8.8.8', 9000)}
    assert main.pb.maxconn == 10
    assert main.pb.workers == 20
    assert main.pb.buffer == 30
    assert main.pb.tcp_fastopen == 40
    assert main.pb.private_address == '127.0.0.99'
    assert main.pb.default_tcp_rst_on_error.value
    log_call = main.get('log').value
    assert log_call.func_name == 'get_log_path'
    params = log_call.get_log_path_params
    assert params.pb.name == 'childs_log'
    assert params.pb.port == 8000
    assert params.pb.default_log_dir == '/tmp/'

    assert isinstance(main.nested.module, Ipdispatch)
    ipdisp = main.nested.module
    assert set(ipdisp.sections) == {'test', 'test_2'}
    assert len(list(ipdisp.sections['test'].list_addrs())) == 2
    assert len(list(ipdisp.sections['test_2'].list_addrs())) == 1

    assert isinstance(ipdisp.sections['test'].nested.module, Admin)
    assert isinstance(ipdisp.sections['test_2'].nested.module, ErrorDocument)


def test_instance_macro_expand_2():
    pb = modules_pb2.InstanceMacro(
        maxconn=10,
        workers=20,
        buffer=30,
        sections=[
            modules_pb2.IpdispatchSectionEntry(
                key='admin',
                value=modules_pb2.IpdispatchSection(
                    ips=[
                        modules_pb2.IpdispatchSection.Ip(value='127.0.0.1'),
                        modules_pb2.IpdispatchSection.Ip(value='::1')
                    ],
                    ports=[modules_pb2.IpdispatchSection.Port(value=8891)],
                    nested=modules_pb2.Holder(http=modules_pb2.HttpModule(nested=modules_pb2.Holder(admin=modules_pb2.AdminModule())))
                )
            ),
            modules_pb2.IpdispatchSectionEntry(
                key='http_section',
                value=modules_pb2.IpdispatchSection(
                    ips=[
                        modules_pb2.IpdispatchSection.Ip(value='2a02:6b8:0:3400::109'),
                        modules_pb2.IpdispatchSection.Ip(value='93.158.157.109')
                    ],
                    ports=[modules_pb2.IpdispatchSection.Port(value=8891)],
                    local_ips=[
                        modules_pb2.IpdispatchSection.Ip(value='local_v4_addr'),
                        modules_pb2.IpdispatchSection.Ip(value='local_v6_addr'),
                    ],
                    local_ports=[modules_pb2.IpdispatchSection.Port(value=8891)],
                    nested=modules_pb2.Holder(extended_http_macro=modules_pb2.ExtendedHttpMacro(
                        port=8891,
                        maxlen=131072,
                        maxreq=131072,
                        nested=modules_pb2.Holder(errordocument=modules_pb2.ErrorDocumentModule(status=200, content='It works!'))
                    ))
                )
            )
        ]
    )
    m = InstanceMacro(pb)
    holder = Holder(m.expand()[0])
    assert holder.module_name == 'main'
    assert holder.module.nested.module_name == 'ipdispatch'
    ipdispatch = holder.module.nested.module
    assert set(ipdispatch.sections) == {'admin', 'http_section'}

    admin_section = ipdispatch.sections['admin']
    assert {ip.pb.value for ip in admin_section.ips} == {'::1', '127.0.0.1'}
    assert {port.pb.value for port in admin_section.ports} == {8891}

    http_section = ipdispatch.sections['http_section']
    assert {ip.pb.value for ip in http_section.ips} == {'93.158.157.109', '2a02:6b8:0:3400::109'}
    assert {port.pb.value for port in http_section.ports} == {8891}

    assert {ip.pb.value for ip in http_section.local_ips} == {'local_v4_addr', 'local_v6_addr'}
    assert {port.pb.value for port in http_section.local_ports} == {8891}

    assert (
        set(ipdispatch.to_config(preceding_modules=[Main(modules_pb2.MainModule())]).table) ==
        {'admin', 'http_section_8891_local', 'http_section_8891_remote'}
    )
