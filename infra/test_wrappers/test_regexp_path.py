# coding: utf-8
import mock
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.base import ANY_MODULE
from awacs.wrappers.main import RegexpPath, RegexpPathSection
from awacs.wrappers.errors import ValidationError


def test_regexp_path():
    pb = modules_pb2.RegexpPathModule()
    reg_path = RegexpPath(pb)

    with pytest.raises(ValidationError) as e:
        reg_path.validate()
    e.match('at least one of the "include_upstreams", "sections" must be specified')

    led_entry_pb = pb.sections.add()
    led_entry_pb.key = 'led'
    led_entry_pb.value.SetInParent()
    reg_path.update_pb(pb)

    with mock.patch.object(reg_path.sections['led'], 'validate',
                           side_effect=ValidationError('BAD')):
        with pytest.raises(ValidationError) as e:
            reg_path.validate()
    e.match(r'sections\[led\].*BAD')

    with mock.patch.object(reg_path.sections['led'], 'validate') as led_validate:
        reg_path.validate()
    led_validate.assert_called_once()

    zeppelin_entry_pb = pb.sections.add()
    zeppelin_entry_pb.key = 'zeppelin'
    zeppelin_entry_pb.value.SetInParent()
    reg_path.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        reg_path.validate()
    e.match('too many sections with an empty pattern: "led", "zeppelin"')

    zeppelin_entry_pb.value.pattern = '/zeppelin/.*'
    reg_path.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        reg_path.validate()
    e.match(r'sections\[led\]: section with an empty pattern must go last')

    led_entry_pb.value.pattern = '/led/.*'
    reg_path.update_pb(pb)

    with mock.patch.object(reg_path.sections['led'], 'validate') as led_validate:
        with mock.patch.object(reg_path.sections['zeppelin'], 'validate') as zeppelin_validate:
            reg_path.validate()

    led_validate.assert_called_once()
    zeppelin_validate.assert_called_once()

    for call in led_validate.mock_calls:
        _, args, kwargs = call
        assert kwargs['key'] == 'led'

    for call in zeppelin_validate.mock_calls:
        _, args, kwargs = call
        assert kwargs['key'] == 'zeppelin'


def test_regexp_path_section():
    pb = modules_pb2.RegexpPathSection()
    pb.nested.errorlog.log = './errorlog'

    section = RegexpPathSection(pb)

    with mock.patch.object(section, 'require_nested'):
        with pytest.raises(ValidationError) as e:
            section.validate()
    e.match('must be a child of "regexp_path" module')

    pb.pattern = '/led/.*'
    section.update_pb(pb)

    with mock.patch.object(section, 'require_nested'):
        with pytest.raises(ValidationError) as e:
            section.validate(preceding_modules=[ANY_MODULE], key=RegexpPathSection.DEFAULT_KEY)
    e.match('"default" section must have an empty pattern')
