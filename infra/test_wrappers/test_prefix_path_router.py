# coding: utf-8
import mock
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.base import ANY_MODULE
from awacs.wrappers.main import PrefixPathRouter, PrefixPathRouterSection
from awacs.wrappers.errors import ValidationError


def test_prefix_path_router():
    pb = modules_pb2.PrefixPathRouterModule()
    prefix_path = PrefixPathRouter(pb)

    with pytest.raises(ValidationError) as e:
        prefix_path.validate()
    e.match('at least one of the "include_upstreams", "sections" must be specified')

    led_entry_pb = pb.sections.add()
    led_entry_pb.key = 'led'
    led_entry_pb.value.SetInParent()
    prefix_path.update_pb(pb)

    with mock.patch.object(prefix_path.sections['led'], 'validate',
                           side_effect=ValidationError('BAD')):
        with pytest.raises(ValidationError) as e:
            prefix_path.validate()
    e.match(r'sections\[led\].*BAD')

    with mock.patch.object(prefix_path.sections['led'], 'validate') as led_validate:
        prefix_path.validate()
    led_validate.assert_called_once()

    zeppelin_entry_pb = pb.sections.add()
    zeppelin_entry_pb.key = 'zeppelin'
    zeppelin_entry_pb.value.SetInParent()
    prefix_path.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        prefix_path.validate()
    e.match('too many sections with an empty route: "led", "zeppelin"')

    zeppelin_entry_pb.value.route = '/zeppelin/'
    prefix_path.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        prefix_path.validate()
    e.match(r'sections\[led\]: section with an empty route must go last')

    led_entry_pb.value.route = '/led'
    prefix_path.update_pb(pb)

    with mock.patch.object(prefix_path.sections['led'], 'validate') as led_validate:
        with mock.patch.object(prefix_path.sections['zeppelin'], 'validate') as zeppelin_validate:
            prefix_path.validate()

    led_validate.assert_called_once()
    zeppelin_validate.assert_called_once()

    for call in led_validate.mock_calls:
        _, args, kwargs = call
        assert kwargs['key'] == 'led'

    for call in zeppelin_validate.mock_calls:
        _, args, kwargs = call
        assert kwargs['key'] == 'zeppelin'


def test_prefix_path_router_section():
    pb = modules_pb2.PrefixPathRouterSection()
    pb.nested.errorlog.log = './errorlog'

    section = PrefixPathRouterSection(pb)

    with mock.patch.object(section, 'require_nested'):
        with pytest.raises(ValidationError) as e:
            section.validate()
    e.match('must be a child of "prefix_path_router" module')

    pb.route = '/led/'
    section.update_pb(pb)

    with mock.patch.object(section, 'require_nested'):
        with pytest.raises(ValidationError) as e:
            section.validate(preceding_modules=[ANY_MODULE], key=PrefixPathRouterSection.DEFAULT_KEY)
    e.match('"default" section must have an empty route')
