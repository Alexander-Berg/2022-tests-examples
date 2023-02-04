# coding: utf-8
import mock

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Cryprox
from awtest.wrappers import get_validation_exception


def test_cryprox():
    pb = modules_pb2.CryproxModule()
    c = Cryprox(pb)

    e = get_validation_exception(c.validate)
    e.match('partner_token.*is required')

    pb.partner_token = 'XXX'

    e = get_validation_exception(c.validate)
    e.match('use_cryprox_matcher.*is required')

    pb.use_cryprox_matcher.SetInParent()
    c.update_pb()
    e = get_validation_exception(c.validate)
    e.match('secrets_file.*is required')

    pb.secrets_file = 'yyy'
    e = get_validation_exception(c.validate)
    e.match('disable_file.*is required')

    pb.disable_file = 'xxx'
    e = get_validation_exception(c.validate)
    e.match('cryprox_backend.*is required')

    pb.cryprox_backend.SetInParent()
    c.update_pb()
    e = get_validation_exception(c.validate)
    e.match('must have nested module')

    with mock.patch.object(c.use_cryprox_matcher, 'validate') as stub_1, \
            mock.patch.object(c.cryprox_backend, 'validate') as stub_2:
        c.validate(chained_modules=[mock.Mock()])

        assert stub_1.called
        assert stub_2.called

        c.validate(chained_modules=[mock.Mock()])