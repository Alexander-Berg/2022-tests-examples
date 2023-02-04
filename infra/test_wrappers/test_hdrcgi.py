# coding: utf-8
import mock

from infra.awacs.proto import modules_pb2
from awacs.wrappers.base import wrap
from awacs.wrappers import main
from awtest.wrappers import get_validation_exception


def test_hdrcgi():
    pb = modules_pb2.HdrcgiModule()

    m = wrap(pb)

    e = get_validation_exception(m.validate)
    e.match('at least one of the "cgi_from_hdr", "hdr_from_cgi" must be specified')

    pb.cgi_from_hdr['value-1'] = 'header-name-1'
    m.update_pb(pb)

    with mock.patch.object(main, 'validate_header_name') as stub:
        m.validate(chained_modules=True)
    stub.assert_has_calls([mock.call('header-name-1')])

    pb.hdr_from_cgi['header-name-2'] = 'value-2'
    m.update_pb(pb)

    with mock.patch.object(main, 'validate_header_name') as stub:
        m.validate(chained_modules=True)
    stub.assert_has_calls([mock.call('header-name-1'), mock.call('header-name-2')], any_order=True)
