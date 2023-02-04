# coding: utf-8
import mock

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Ipdispatch, IpdispatchSection
from awacs.wrappers.errors import ValidationError
from awtest.wrappers import get_validation_exception


def test_ipdispatch():
    pb = modules_pb2.IpdispatchModule()

    section_1_entry = pb.sections.add()
    section_1_entry.key = 'section_1'
    section_1_entry.value.ips.add(value='8.8.8.8')
    section_1_entry.value.ips.add(value='9.9.9.9')
    section_1_entry.value.ports.add(value=80)
    section_1_entry.value.ports.add(value=81)

    section_2_entry = pb.sections.add()
    section_2_entry.key = 'section_2'
    section_2_entry.value.ips.add(value='127.0.0.1')
    section_2_entry.value.ips.add(value='127.0.0.2')
    section_2_entry.value.ports.add(value=80)
    section_2_entry.value.ports.add(value=81)

    ipdispatch = Ipdispatch(pb)
    assert ipdispatch.sections == {
        'section_1': IpdispatchSection(section_1_entry.value),
        'section_2': IpdispatchSection(section_2_entry.value),
    }

    with mock.patch.object(ipdispatch.sections['section_1'], 'validate'):
        with mock.patch.object(ipdispatch.sections['section_2'], 'validate'):
            ipdispatch.validate()

    with mock.patch.object(ipdispatch.sections['section_1'], 'validate'):
        with mock.patch.object(ipdispatch.sections['section_2'], 'validate',
                               side_effect=ValidationError('BAD')):
            e = get_validation_exception(ipdispatch.validate)
    e.match(r'sections\[section_2\]: BAD')

    section_3_entry = pb.sections.add()
    section_3_entry.key = 'section_3'
    section_3_entry.value.ports.add(value=80)
    section_3_entry.value.ports.add(value=81)
    ipdispatch.update_pb(pb)

    with mock.patch.object(ipdispatch.sections['section_1'], 'validate'):
        with mock.patch.object(ipdispatch.sections['section_2'], 'validate'):
            e = get_validation_exception(ipdispatch.validate)
    assert list(e.value.path) == ['sections[section_3]', 'ips']
    e.match('ips.*is required')

    section_3_entry.value.ips.add(value='1.1.1.1')
    del section_3_entry.value.ports[:]
    ipdispatch.update_pb(pb)

    with mock.patch.object(ipdispatch.sections['section_1'], 'validate'):
        with mock.patch.object(ipdispatch.sections['section_2'], 'validate'):
            e = get_validation_exception(ipdispatch.validate)
    e.match('ports.*is required')
    assert list(e.value.path) == ['sections[section_3]', 'ports']

    section_3_entry.value.ports.add(value=100000)
    ipdispatch.update_pb(pb)
    with mock.patch.object(ipdispatch.sections['section_1'], 'validate'):
        with mock.patch.object(ipdispatch.sections['section_2'], 'validate'):
            e = get_validation_exception(ipdispatch.validate)
    e.match(r'ports\[0\].*is not a valid port')
    assert list(e.value.path) == ['sections[section_3]', 'ports[0]']

    del pb.sections[-1]
    section_2_entry.value.ips.add(value='8.8.8.8')
    ipdispatch.update_pb(pb)
    with mock.patch.object(ipdispatch.sections['section_1'], 'validate'):
        with mock.patch.object(ipdispatch.sections['section_2'], 'validate'):
            e = get_validation_exception(ipdispatch.validate)
    e.match('section_1 and section_2 sections intersect by addresses: 8.8.8.8:80, 8.8.8.8:81')


def test_ipdispatch_section():
    pb = modules_pb2.IpdispatchSection()

    pb.ips.add(value='127.0.0.1')
    pb.ips.add(value='*')
    pb.ports.add(value=80)

    section = IpdispatchSection(pb)
    with mock.patch.object(section, 'is_admin', return_value=False):
        section.validate(chained_modules=True)

    pb.ips.add(value='127.0.0.1')
    section.update_pb(pb)
    e = get_validation_exception(section.validate)
    e.match('contains duplicate ip: 127.0.0.1')

    pb.ips.pop()
    pb.ips.add(value='8.8.8.8')
    section.update_pb(pb)
    e = get_validation_exception(section.validate)
    e.match('contains duplicate ip: 8.8.8.8')

    pb.ips.pop()
    pb.ports.add(value=80)
    section.update_pb(pb)
    e = get_validation_exception(section.validate)
    e.match('contains duplicate port: 80')


def test_ipdispatch_asterisk():
    pb = modules_pb2.IpdispatchModule()

    section_1_entry = pb.sections.add()
    section_1_entry.key = 'section_1'
    section_1_entry.value.ips.add(value='*')
    section_1_entry.value.ports.add(value=80)

    section_2_entry = pb.sections.add()
    section_2_entry.key = 'section_2'
    section_2_entry.value.ips.add(value='127.0.0.1')
    section_2_entry.value.ports.add(value=80)

    section_3_entry = pb.sections.add()
    section_3_entry.key = 'section_3'
    section_3_entry.value.ips.add(value='::1')
    section_3_entry.value.ports.add(value=80)

    ipdispatch = Ipdispatch(pb)

    with mock.patch.object(ipdispatch.sections['section_1'], 'validate'):
        with mock.patch.object(ipdispatch.sections['section_2'], 'validate'):
            with mock.patch.object(ipdispatch.sections['section_3'], 'validate'):
                ipdispatch.validate()

    external_section_entry = pb.sections.add()
    external_section_entry.key = 'external_section'
    external_section_entry.value.ips.add(value='8.8.8.8')
    external_section_entry.value.ports.add(value=80)

    ipdispatch.update_pb(pb)
    with mock.patch.object(ipdispatch.sections['section_1'], 'validate'):
        with mock.patch.object(ipdispatch.sections['section_2'], 'validate'):
            with mock.patch.object(ipdispatch.sections['section_3'], 'validate'):
                with mock.patch.object(ipdispatch.sections['external_section'], 'validate'):
                    e = get_validation_exception(ipdispatch.validate)
    e.match('section_1 and external_section sections intersect by addresses: 8.8.8.8:80')
