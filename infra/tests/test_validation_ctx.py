import pytest
import six

from infra.awacs.proto import model_pb2
from awacs.wrappers.errors import ValidationError
from awacs.wrappers.base import ValidationCtx


def _get_components_spec(version):
    component_spec_pb = model_pb2.BalancerSpec().components
    component_spec_pb.pginx_binary.state = component_spec_pb.pginx_binary.SET
    component_spec_pb.pginx_binary.version = version
    return component_spec_pb


@pytest.mark.parametrize('min, max, error_class, msg', [
    ('201-4', None, ValidationError, 'requires component pginx_binary of version >= 201-4, not 201-3'),
    ('201-4', '301-9', ValidationError, 'requires component pginx_binary of version >= 201-4 and <= 301-9, not 201-3'),
    ('1-4', '201-2', ValidationError, 'requires component pginx_binary of version >= 1-4 and <= 201-2, not 201-3'),
    (None, '100-2', ValidationError, 'requires component pginx_binary of version <= 100-2, not 201-3'),
    (None, None, RuntimeError, 'At least one of `min` and `max` should be specified'),
    ('201-4', '100-2', RuntimeError, 'Minimum version must be less than or equal to max version'),
])
def test_ensure_component_version__negative(min, max, error_class, msg):
    ctx = ValidationCtx(components_pb=_get_components_spec('201-3'))
    with pytest.raises(error_class) as e:
        ctx.ensure_component_version(model_pb2.ComponentMeta.PGINX_BINARY, min, max)
    assert six.text_type(e.value) == msg


@pytest.mark.parametrize('min, max', [
    ('201-4', None),
    ('1-5', None),
    (None, '201-5'),
    (None, '301-5'),
    ('1-5', '301-5'),
])
def test_ensure_component_version__positive(min, max):
    ctx = ValidationCtx(components_pb=_get_components_spec('201-4'))
    ctx.ensure_component_version(model_pb2.ComponentMeta.PGINX_BINARY, min, max)


def test_ensure_component_version__bad_component():
    ctx = ValidationCtx(components_pb=_get_components_spec('201-3'))
    with pytest.raises(AssertionError) as e:
        ctx.ensure_component_version(-1, '201-4')
    assert six.text_type(e.value) == 'Unknown component_type: -1'
