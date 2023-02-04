# coding: utf-8
import mock
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Main, Addr, ConnReject, CpuLimiter
from awacs.wrappers.util import validate_comma_separated_subnets
from awacs.wrappers.base import Holder
from awacs.wrappers.errors import ValidationError
from awtest.wrappers import get_validation_exception


def test_main():
    pb = modules_pb2.MainModule()

    addr_1 = pb.addrs.add()
    addr_1.ip = '8.8.8.8'
    addr_1.port = 80

    admin_addr_1 = pb.admin_addrs.add()
    admin_addr_1.ip = '127.0.0.1'
    admin_addr_1.port = 80

    main = Main(pb)
    assert main.addrs == [Addr(addr_1)]
    assert main.admin_addrs == [Addr(admin_addr_1)]

    pb.maxconn = -1
    e = get_validation_exception(main.validate, chained_modules=[])
    e.match('maxconn.*must be non-negative')

    pb.maxconn = 1000
    main.update_pb(pb)

    main.pb.workers = -10
    main.update_pb(pb)

    e = get_validation_exception(main.validate, chained_modules=[])
    e.match('workers.*must be positive or zero')

    main.pb.workers = 10
    main.update_pb(pb)

    e = get_validation_exception(main.validate, chained_modules=[])
    e.match('must have nested module')

    holder = modules_pb2.Holder()
    ipdispatch_pb = holder.ipdispatch

    entry = ipdispatch_pb.sections.add()
    entry.key = 'section_1'
    section_1 = entry.value
    section_1.ips.add(value='8.8.8.8')
    section_1.ports.add(value=80)

    chained_module = Holder(holder)
    with mock.patch.object(chained_module.module.sections['section_1'], 'is_admin', return_value=False):
        with pytest.raises(ValidationError) as e:
            main.validate(chained_modules=[chained_module])
    e.match('admin_addrs not used by admin ipdispatch sections: 127.0.0.1:80')

    entry = ipdispatch_pb.sections.add()
    entry.key = 'section_2'
    section_2 = entry.value
    section_2.ips.add(value='127.0.0.1')
    section_2.ports.add(value=80)

    chained_module = Holder(holder)
    with mock.patch.object(chained_module.module.sections['section_1'], 'is_admin', return_value=False):
        with mock.patch.object(chained_module.module.sections['section_2'], 'is_admin', return_value=True):
            main.validate(chained_modules=[chained_module])

    pb.unistat.addrs.add(ip='127.0.0.1', port=80)
    main.update_pb(pb)
    with mock.patch.object(chained_module.module.sections['section_1'], 'is_admin', return_value=False):
        with mock.patch.object(chained_module.module.sections['section_2'], 'is_admin', return_value=True):
            e = get_validation_exception(main.validate, chained_modules=[chained_module])
    e.match('admin_addrs and unistat addrs intersect: 127.0.0.1:80')

    pb.unistat.addrs[0].port = 888
    with mock.patch.object(chained_module.module.sections['section_1'], 'is_admin', return_value=False):
        with mock.patch.object(chained_module.module.sections['section_2'], 'is_admin', return_value=True):
            main.validate(chained_modules=[chained_module])

    pb.sd.SetInParent()
    pb.ClearField('unistat')
    main.update_pb(pb)
    with mock.patch.object(chained_module.module.sections['section_1'], 'is_admin', return_value=False):
        with mock.patch.object(chained_module.module.sections['section_2'], 'is_admin', return_value=True):
            e = get_validation_exception(main.validate, chained_modules=[chained_module])
    e.match('sd: can not be enabled without unistat')

    pb.unistat.SetInParent()
    main.update_pb(pb)
    with mock.patch.object(chained_module.module.sections['section_1'], 'is_admin', return_value=False):
        with mock.patch.object(chained_module.module.sections['section_2'], 'is_admin', return_value=True):
            main.validate(chained_modules=[chained_module])

    del pb.unistat.addrs[:]
    admin_addr_2 = pb.admin_addrs.add()
    admin_addr_2.ip = '127.0.0.1'
    admin_addr_2.port = 80
    main.update_pb(pb)
    e = get_validation_exception(main.validate, chained_modules=[chained_module])
    e.match('admin_addrs.*contain duplicate address: 127.0.0.1:80')

    pb.admin_addrs.pop()
    admin_addr_2 = pb.admin_addrs.add()
    admin_addr_2.ip = '8.8.8.8'
    admin_addr_2.port = 80
    main.update_pb(pb)
    e = get_validation_exception(main.validate, chained_modules=[chained_module])
    e.match('.*addrs and admin_addrs intersect: 8.8.8.8:80')


def test_main_asterisk():
    pb = modules_pb2.MainModule()

    addr_all = pb.addrs.add()
    addr_all.ip = '*'
    addr_all.port = 80

    admin_addr_1 = pb.admin_addrs.add()
    admin_addr_1.ip = '127.0.0.1'
    admin_addr_1.port = 8080

    main = Main(pb)

    holder = modules_pb2.Holder()
    ipdispatch_pb = holder.ipdispatch

    entry = ipdispatch_pb.sections.add()
    entry.key = 'section'
    section = entry.value
    section.ips.add(value='*')
    section.ports.add(value=80)

    entry = ipdispatch_pb.sections.add()
    entry.key = 'admin_section'
    admin_section = entry.value
    admin_section.ips.add(value='127.0.0.1')
    admin_section.ports.add(value=8080)

    chained_module = Holder(holder)
    with mock.patch.object(chained_module.module.sections['section'], 'is_admin', return_value=False):
        with mock.patch.object(chained_module.module.sections['admin_section'], 'is_admin', return_value=True):
            main.validate(chained_modules=[chained_module])

    addr_local_v4 = pb.addrs.add()
    addr_local_v4.ip = '127.0.0.1'
    addr_local_v4.port = 80

    addr_local_v6 = pb.addrs.add()
    addr_local_v6.ip = '::1'
    addr_local_v6.port = 80

    main.update_pb(pb)

    entry = ipdispatch_pb.sections.add()
    entry.key = 'section_local_v4'
    section_local_v4 = entry.value
    section_local_v4.ips.add(value='127.0.0.1')
    section_local_v4.ports.add(value=80)

    entry = ipdispatch_pb.sections.add()
    entry.key = 'section_local_v6'
    section_local_v6 = entry.value
    section_local_v6.ips.add(value='::1')
    section_local_v6.ports.add(value=80)

    chained_module = Holder(holder)
    with mock.patch.object(chained_module.module.sections['section'], 'is_admin', return_value=False):
        with mock.patch.object(chained_module.module.sections['section_local_v4'], 'is_admin', return_value=False):
            with mock.patch.object(chained_module.module.sections['section_local_v6'], 'is_admin', return_value=False):
                with mock.patch.object(chained_module.module.sections['admin_section'], 'is_admin', return_value=True):
                    main.validate(chained_modules=[chained_module])

    addr_external = pb.addrs.add()
    addr_external.ip = '8.8.8.8'
    addr_external.port = 80

    main.update_pb(pb)
    e = get_validation_exception(main.validate, chained_modules=True)
    e.match('addrs.*contain duplicate address: 8.8.8.8:80')

    pb.addrs.pop()
    admin_addr_2 = pb.admin_addrs.add()
    admin_addr_2.ip = '8.8.8.8'
    admin_addr_2.port = 80
    main.update_pb(pb)
    e = get_validation_exception(main.validate, chained_modules=True)
    e.match('.*addrs and admin_addrs intersect: 8.8.8.8:80')


def test_addr():
    pb = modules_pb2.Addr()

    addr = Addr(pb)

    e = get_validation_exception(addr.validate)
    e.match('ip: is required')

    pb.ip = 'xxx'
    addr.update_pb(pb)

    e = get_validation_exception(addr.validate)
    e.match('port: is required')

    pb.port = -1
    addr.update_pb(pb)

    e = get_validation_exception(addr.validate)
    e.match('ip: is not a valid IP address')

    pb.ip = '8.8.8.8'
    addr.update_pb(pb)

    e = get_validation_exception(addr.validate)
    e.match('port: is not a valid port')

    pb.port = 80
    addr.update_pb(pb)

    addr.validate()


def test_cpu_limiter():
    pb = modules_pb2.MainModule.CpuLimiter.ConnReject()
    l = ConnReject(pb)

    e = get_validation_exception(l.validate)
    e.match('lo: is required')
    pb.lo.value = 0

    e = get_validation_exception(l.validate)
    e.match('hi: is required')
    pb.hi.value = 1

    pb.lo.value = -1
    e = get_validation_exception(l.validate)
    e.match('lo: must be greater or equal to 0')

    pb.lo.value = 1.00001
    e = get_validation_exception(l.validate)
    e.match('lo: must be less or equal to 1')

    pb.lo.value = 0.5
    pb.hi.value = -1
    e = get_validation_exception(l.validate)
    e.match('hi: must be greater or equal to 0')

    pb.hi.value = 2
    e = get_validation_exception(l.validate)
    e.match('hi: must be less or equal to 1')

    pb.hi.value = 0.2
    e = get_validation_exception(l.validate)
    e.match('lo: must be less or equal to "hi"')

    pb.lo.value = .1
    pb.hi.value = .9

    pb.conn_hold_count.value = -1
    e = get_validation_exception(l.validate)
    e.match('conn_hold_count: must be non-negative')

    pb.conn_hold_count.value = 1000
    pb.conn_hold_duration = 'xxx'
    e = get_validation_exception(l.validate)
    e.match('conn_hold_duration: "xxx" is not a valid timedelta string')

    pb = modules_pb2.MainModule.CpuLimiter()
    pb.conn_reject.lo.value = .1
    pb.conn_reject.hi.value = .9
    pb.conn_reject.conn_hold_duration = '200s'

    c = CpuLimiter(pb)
    e = get_validation_exception(c.validate)
    e.match('conn_reject -> conn_hold_duration: must not be set if "keepalive_close" is not enabled')

    pb.conn_reject.conn_hold_duration = ''
    pb.conn_reject.conn_hold_count.value = 100
    e = get_validation_exception(c.validate)
    e.match('conn_reject -> conn_hold_count: must not be set if "keepalive_close" is not enabled')


def test_validate_comma_separated_subnets():
    e = get_validation_exception(validate_comma_separated_subnets, '123', 'x')
    e.match('x: "123" is not a valid subnet')
    e = get_validation_exception(validate_comma_separated_subnets, '123.0.0.1/2,124.0.0.1', 'x')
    e.match('x: "124.0.0.1" is not a valid subnet')
    e = get_validation_exception(validate_comma_separated_subnets, '123.0.0.1/2,124.0.0.1/-1', 'x')
    e.match('x: "124.0.0.1/-1" is not a valid subnet')
    e = get_validation_exception(validate_comma_separated_subnets, '123.0.0.1/2,124.0.0.1/50', 'x')
    e.match('x: "50" is not a valid prefix for "124.0.0.1"')
    e = get_validation_exception(validate_comma_separated_subnets, '123.0.0.1/2,::1/199', 'x')
    e.match('x: "199" is not a valid prefix')
    validate_comma_separated_subnets('123.0.0.1/2,::1/50', 'x')
    validate_comma_separated_subnets('2001:678:384:100::/64', 'x')
