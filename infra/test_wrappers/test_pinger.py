# coding: utf-8
import mock

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Pinger
from awacs.wrappers import main
from awtest.wrappers import get_validation_exception


def test_pinger():
    pb = modules_pb2.PingerModule()

    m = Pinger(pb)

    e = get_validation_exception(m.validate)
    e.match('ping_request_data: is required')

    pb.ping_request_data = r'GET /u?ver=0&show=0 HTTP/1.1\nHost: bar-navig.yandex.ru\n\n'
    m.update_pb(pb)
    e = get_validation_exception(m.validate)

    e.match('admin_request_uri: is required')
    pb.admin_request_uri = 'xxx'
    m.update_pb(pb)
    e = get_validation_exception(m.validate, chained_modules=True)

    e.match('admin_error_replier: is required')
    pb.admin_error_replier.SetInParent()
    m.update_pb(pb)

    pb.delay = '10s'
    pb.histtime = '20s'
    with mock.patch.object(main, 'validate_request_line') as stub_1, \
            mock.patch.object(main, 'validate_timedelta') as stub_2, \
            mock.patch.object(m.admin_error_replier, 'validate') as stub_3:
        m.validate(chained_modules=True)
    stub_1.assert_called_with(pb.ping_request_data)
    stub_2.assert_has_calls([
        mock.call('10s'), mock.call('20s')
    ], any_order=True)
    assert stub_3.called
