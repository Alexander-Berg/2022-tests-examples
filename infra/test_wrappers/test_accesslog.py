# coding: utf-8
from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Accesslog
from awtest.wrappers import get_validation_exception


def test_accesslog():
    pb = modules_pb2.AccesslogModule()

    accesslog = Accesslog(pb)

    e = get_validation_exception(accesslog.validate)
    e.match('log.*is required')

    pb.log = '/tmp/log'
    accesslog.update_pb(pb)

    accesslog.validate(chained_modules=True)
