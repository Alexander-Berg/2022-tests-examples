# coding: utf-8
import mock
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.base import ANY_MODULE
from awacs.wrappers.main import RegexpHost, RegexpHostSection
from awacs.wrappers.errors import ValidationError


def test_regexp_host():
    pb = modules_pb2.RegexpHostModule()
    reg_host = RegexpHost(pb)

    with pytest.raises(ValidationError) as e:
        reg_host.validate()
    e.match('at least one of the "include_upstreams", "sections" must be specified')

    led_entry_pb = pb.sections.add()
    led_entry_pb.key = 'led'
    led_entry_pb.value.SetInParent()
    reg_host.update_pb(pb)

    with mock.patch.object(reg_host.sections['led'], 'validate',
                           side_effect=ValidationError('BAD')):
        with pytest.raises(ValidationError) as e:
            reg_host.validate()
    e.match(r'sections\[led\].*BAD')

    with mock.patch.object(reg_host.sections['led'], 'validate') as led_validate:
        reg_host.validate()
    led_validate.assert_called_once()

    zeppelin_entry_pb = pb.sections.add()
    zeppelin_entry_pb.key = 'zeppelin'
    zeppelin_entry_pb.value.SetInParent()
    reg_host.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        reg_host.validate()
    e.match('too many sections with an empty pattern: "led", "zeppelin"')

    zeppelin_entry_pb.value.pattern = 'zeppelin\\.yandex\\.net'
    reg_host.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        reg_host.validate()
    e.match(r'sections\[led\]: section with an empty pattern must go last')

    led_entry_pb.value.pattern = 'led\\.yandex\\.net'
    reg_host.update_pb(pb)

    with mock.patch.object(reg_host.sections['led'], 'validate') as led_validate:
        with mock.patch.object(reg_host.sections['zeppelin'], 'validate') as zeppelin_validate:
            reg_host.validate()

    led_validate.assert_called_once()
    zeppelin_validate.assert_called_once()

    for call in led_validate.mock_calls:
        _, args, kwargs = call
        assert kwargs['key'] == 'led'

    for call in zeppelin_validate.mock_calls:
        _, args, kwargs = call
        assert kwargs['key'] == 'zeppelin'


def test_regexp_host_section():
    pb = modules_pb2.RegexpHostSection()
    pb.nested.errorlog.log = './errorlog'

    section = RegexpHostSection(pb)

    with mock.patch.object(section, 'require_nested'):
        with pytest.raises(ValidationError) as e:
            section.validate()
    e.match('must be a child of "regexp_host" module')

    pb.pattern = 'led\\.yandex\\.net'
    section.update_pb(pb)

    with mock.patch.object(section, 'require_nested'):
        with pytest.raises(ValidationError) as e:
            section.validate(preceding_modules=[ANY_MODULE], key=RegexpHostSection.DEFAULT_KEY)
    e.match('"default" section must have an empty pattern')
