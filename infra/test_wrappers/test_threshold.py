# coding: utf-8
import mock

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Threshold
from awacs.wrappers.errors import ValidationError
from awtest.wrappers import get_validation_exception


def test_threshold():
    pb = modules_pb2.ThresholdModule()

    threshold = Threshold(pb)

    e = get_validation_exception(threshold.validate, chained_modules=True)
    e.match('hi_bytes.*is required')

    pb.hi_bytes = 100
    threshold.update_pb(pb)

    e = get_validation_exception(threshold.validate, chained_modules=True)
    e.match('lo_bytes.*is required')

    pb.lo_bytes = 200
    threshold.update_pb(pb)

    e = get_validation_exception(threshold.validate, chained_modules=True)
    e.match('pass_timeout.*is required')

    pb.pass_timeout = '20s'
    threshold.update_pb(pb)

    e = get_validation_exception(threshold.validate, chained_modules=True)
    e.match('recv_timeout.*is required')

    pb.recv_timeout = '20s'
    threshold.update_pb(pb)

    e = get_validation_exception(threshold.validate, chained_modules=True)
    e.match(r'lo_bytes > hi_bytes \(200 > 100\), which is meaningless')

    pb.lo_bytes = 10
    threshold.update_pb(pb)

    threshold.validate(chained_modules=True)

    pb.on_pass_timeout_failure.SetInParent()
    threshold.update_pb(pb)

    with mock.patch.object(threshold.on_pass_timeout_failure, 'validate') as validate:
        threshold.validate(chained_modules=True)
    validate.assert_called_once()

    with mock.patch.object(threshold.on_pass_timeout_failure, 'validate',
                           side_effect=ValidationError('BAD')) as validate:
        e = get_validation_exception(threshold.validate, chained_modules=True)
    validate.assert_called_once()
    e.match('on_pass_timeout_failure: BAD')
