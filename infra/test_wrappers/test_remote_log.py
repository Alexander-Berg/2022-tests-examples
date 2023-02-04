# coding: utf-8
import mock

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import RemoteLog
from awacs.wrappers.errors import ValidationError
from awtest.wrappers import get_validation_exception


def test_remote_log():
    pb = modules_pb2.RemoteLogModule()
    remote_log = RemoteLog(pb)

    e = get_validation_exception(remote_log.validate, chained_modules=True)
    e.match('remote_log_storage.*is required')

    pb.remote_log_storage.SetInParent()
    remote_log.update_pb(pb)

    with mock.patch.object(remote_log.remote_log_storage, 'validate') as validate:
        remote_log.validate(chained_modules=True)
    validate.assert_called_once()

    with mock.patch.object(remote_log.remote_log_storage, 'validate', side_effect=ValidationError('BAD')) as validate:
        e = get_validation_exception(remote_log.validate, chained_modules=True)
    validate.assert_called_once()
    e.match('remote_log_storage: BAD')
