import mock

from awacs.wrappers.l7upstreammacro import (
    L7UpstreamMacroDcBalancerSettings,
    L7UpstreamMacroByDcScheme,
    L7UpstreamMacroBalancerSettings,
    L7UpstreamMacroOnError,
    L7UpstreamMacroDc,
    L7UpstreamMacroFlatScheme
)
from awtest.wrappers import get_exception_msg, FixtureStorage
from infra.awacs.proto import modules_pb2


fixture_storage = FixtureStorage(u'test_l7_upstream_macro')


def test_by_dc_scheme():
    pb = modules_pb2.L7UpstreamMacro.ByDcScheme()
    m = L7UpstreamMacroByDcScheme(pb)
    assert get_exception_msg(m.validate) == u'dc_balancer: is required'

    pb.dc_balancer.SetInParent()
    m = L7UpstreamMacroByDcScheme(pb)
    assert get_exception_msg(m.validate) == u'balancer: is required'

    pb.balancer.SetInParent()
    m = L7UpstreamMacroByDcScheme(pb)
    assert get_exception_msg(m.validate) == u'dcs: is required'

    pb.dcs.add()
    m = L7UpstreamMacroByDcScheme(pb)
    assert get_exception_msg(m.validate) == u'on_error: is required'

    pb.on_error.SetInParent()
    m = L7UpstreamMacroByDcScheme(pb)

    with mock.patch.object(L7UpstreamMacroDcBalancerSettings, u'validate') as dc_balancer_validate:
        with mock.patch.object(L7UpstreamMacroBalancerSettings, u'validate') as balancer_validate:
            with mock.patch.object(L7UpstreamMacroOnError, u'validate') as on_error_validate:
                with mock.patch.object(L7UpstreamMacroDc, u'validate') as dc_validate:
                    assert get_exception_msg(m.validate) is None

    assert dc_balancer_validate.called
    assert balancer_validate.called
    assert on_error_validate.called
    assert dc_validate.called


def test_flat_scheme():
    pb = modules_pb2.L7UpstreamMacro.FlatScheme()
    m = L7UpstreamMacroFlatScheme(pb)
    assert get_exception_msg(m.validate) == u'balancer: is required'

    pb.balancer.SetInParent()
    m = L7UpstreamMacroFlatScheme(pb)

    with mock.patch.object(L7UpstreamMacroBalancerSettings, u'validate'):
        assert get_exception_msg(m.validate) == u'backend_ids: is required'

    pb.backend_ids.append(u'a')
    with mock.patch.object(L7UpstreamMacroBalancerSettings, u'validate'):
        assert get_exception_msg(m.validate) == u'on_error: is required'

    pb.on_error.SetInParent()
    m = L7UpstreamMacroFlatScheme(pb)

    with mock.patch.object(L7UpstreamMacroOnError, u'validate'), \
         mock.patch.object(L7UpstreamMacroBalancerSettings, u'validate'):
        assert get_exception_msg(m.validate) is None


def test_flat_scheme_on_fast_error():
    holder_pb = modules_pb2.Holder()
    upstream_macro_pb = holder_pb.l7_upstream_macro
    upstream_macro_pb.version = '0.0.1'
    upstream_macro_pb.headers.add().uaas.service_name = 'test_uaas'

    pb = upstream_macro_pb.flat_scheme
    pb.balancer.attempts = 1
    pb.balancer.max_reattempts_share = 1
    pb.balancer.max_pessimized_endpoints_share = 1
    pb.balancer.do_not_retry_http_responses = True

    pb.balancer.SetInParent()
    pb.backend_ids.append(u'a')

    pb.on_error.SetInParent()
    pb.on_error.static.status = 522
    pb.on_error.static.content = 'Something wicked'
    pb.on_fast_error.SetInParent()
    pb.on_fast_error.static.status = 892
    pb.on_fast_error.static.content = 'Even worse'

    with mock.patch.object(L7UpstreamMacroOnError, u'validate'), \
         mock.patch.object(L7UpstreamMacroBalancerSettings, u'validate'):
        fixture_storage.assert_pb_is_equal_to_file(holder_pb, 'flat_scheme_on_fast_error.pb.txt')


def test_by_dc_scheme_on_fast_error():
    holder_pb = modules_pb2.Holder()
    upstream_macro_pb = holder_pb.l7_upstream_macro
    upstream_macro_pb.version = '0.0.1'
    upstream_macro_pb.headers.add().uaas.service_name = 'test_uaas'
    pb = upstream_macro_pb.by_dc_scheme

    pb.dc_balancer.SetInParent()
    pb.dc_balancer.attempts = 1
    pb.dc_balancer.method = pb.dc_balancer.BY_DC_WEIGHT

    pb.balancer.SetInParent()
    pb.balancer.attempts = 1
    pb.balancer.max_reattempts_share = 1
    pb.balancer.max_pessimized_endpoints_share = 1
    pb.balancer.do_not_retry_http_responses = True

    pb.dcs.add()
    pb.on_error.SetInParent()
    pb.on_error.static.status = 522
    pb.on_error.static.content = 'Something wicked'

    pb.on_fast_error.SetInParent()
    pb.on_fast_error.static.status = 892
    pb.on_fast_error.static.content = 'Even worse'

    with mock.patch.object(L7UpstreamMacroDcBalancerSettings, u'validate'):
        with mock.patch.object(L7UpstreamMacroBalancerSettings, u'validate'):
            with mock.patch.object(L7UpstreamMacroOnError, u'validate'):
                with mock.patch.object(L7UpstreamMacroDc, u'validate'):
                    fixture_storage.assert_pb_is_equal_to_file(holder_pb, 'by_dc_scheme_on_fast_error.pb.txt')
