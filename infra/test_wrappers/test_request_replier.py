# coding: utf-8
import mock

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import RequestReplier
from awtest.wrappers import get_validation_exception


def test_request_replier():
    pb = modules_pb2.RequestReplierModule()

    m = RequestReplier(pb)

    e = get_validation_exception(m.validate, chained_modules=True)
    e.match('sink: is required')

    pb.sink.SetInParent()
    m.update_pb(pb)
    with mock.patch.object(m.sink, 'validate') as stub:
        m.validate(chained_modules=True)
    assert stub.called
