# coding: utf-8
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Errorlog
from awacs.wrappers.errors import ValidationError


def test_errorlog():
    pb = modules_pb2.ErrorlogModule()
    errorlog = Errorlog(pb)

    with pytest.raises(ValidationError) as e:
        errorlog.validate(chained_modules=True)
    e.match('log: is required')

    pb.log = '/tmp/log'
    errorlog.update_pb(pb)

    errorlog.validate(chained_modules=True)

    pb.log_level = 'EXCEPTION'
    errorlog.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        errorlog.validate(chained_modules=True)
    e.match('log_level.*must be one of the')

    pb.log_level = 'ERROR'
    errorlog.update_pb(pb)
    errorlog.validate(chained_modules=True)
