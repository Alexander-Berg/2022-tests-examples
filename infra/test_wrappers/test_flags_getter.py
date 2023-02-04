# coding: utf-8
import mock

from infra.awacs.proto import modules_pb2
from awacs.wrappers.errors import ValidationError
from awacs.wrappers.main import FlagsGetter
from awtest.wrappers import get_validation_exception


def test_flags_getter():
    pb = modules_pb2.FlagsGetterModule()

    m = FlagsGetter(pb)
    e = get_validation_exception(m.validate)
    e.match('service_name: is required')

    pb.service_name = 'test'
    m.update_pb(pb)

    e = get_validation_exception(m.validate)
    e.match('flags: is required')

    pb.flags.SetInParent()
    m.update_pb(pb)

    e = get_validation_exception(m.validate)
    e.match('must have nested module')

    with mock.patch.object(m.flags, 'validate',
                           side_effect=ValidationError('smth went wrong')):
        e = get_validation_exception(m.validate, chained_modules=[mock.Mock()])
    e.match('flags: smth went wrong')

    with mock.patch.object(m.flags, 'validate'):
        m.validate(chained_modules=[mock.Mock()])

    assert len(list(m.get_branches())) == 1
    assert m.get_named_branches() == {'flags': m.flags}
