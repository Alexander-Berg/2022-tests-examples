# coding: utf-8
import mock
import pytest

from awacs.lib.strutils import quote_join_sorted
from awacs.model.balancer.generator import get_would_be_injected_full_backend_ids
from awacs.wrappers import rps_limiter_settings
from awacs.wrappers.base import Holder, ValidationCtx
from awacs.wrappers.l7macro import L7MacroRpsLimiterExternal
from awacs.wrappers.main import RpsLimiter, RpsLimiterMacro, InstanceMacro
from awtest.wrappers import get_validation_exception
from infra.awacs.proto import modules_pb2


ALLOWED_BY_DEFAULT = list(rps_limiter_settings._public_installation_names) + [u'']


def test_rps_limiter_module():
    pb = modules_pb2.RpsLimiterModule()

    m = RpsLimiter(pb)

    e = get_validation_exception(m.validate)
    e.match('checker: is required')

    pb.checker.errordocument.status = 200
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('must have nested module')

    pb.skip_on_error = True
    m.validate(chained_modules=True)

    pb.on_error.errordocument.status = 502
    m.update_pb(pb)
    e = get_validation_exception(m.validate, chained_modules=True)
    e.match('on_error: must not be set together with skip_on_error')

    pb.skip_on_error = False
    m.update_pb(pb)

    with mock.patch.object(m.checker, 'validate') as stub_1, mock.patch.object(m.on_error, 'validate') as stub_2:
        m.validate(chained_modules=True)
    assert stub_1.called
    assert stub_2.called


@pytest.mark.parametrize(
    'installation,expected_full_backend_ids',
    [
        (
            'invalid',
            {}
        ),
        (
            '',
            {
                ('common-rpslimiter', 'rpslimiter-serval-sas-sd'),
                ('common-rpslimiter', 'rpslimiter-serval-man-sd'),
                ('common-rpslimiter', 'rpslimiter-serval-vla-sd')
            }
        ),
        (
            'COMMON',
            {
                ('common-rpslimiter', 'rpslimiter-serval-sas-sd'),
                ('common-rpslimiter', 'rpslimiter-serval-man-sd'),
                ('common-rpslimiter', 'rpslimiter-serval-vla-sd')
            }
        ),
        (
            'MAPS_FRONT',
            {
                ('common-rpslimiter-maps-front', 'rpslimiter-maps-front-sas-sd'),
                ('common-rpslimiter-maps-front', 'rpslimiter-maps-front-man-sd'),
                ('common-rpslimiter-maps-front', 'rpslimiter-maps-front-vla-sd')
            }
        ),
        (
            'EDUCATION',
            {
                ('common-rpslimiter-education', 'rpslimiter-education-iva-sd'),
                ('common-rpslimiter-education', 'rpslimiter-education-sas-sd'),
                ('common-rpslimiter-education', 'rpslimiter-education-vla-sd')
            }
        )
    ]
)
def test_rps_limiter_macro(installation, expected_full_backend_ids):
    pb = modules_pb2.Holder()
    pb.rps_limiter_macro.record_name = 'macro'
    pb.rps_limiter_macro.installation = installation

    holder = Holder(pb)

    if installation == 'invalid':
        with pytest.raises(KeyError):
            get_would_be_injected_full_backend_ids('n_id', holder)
    else:
        full_backend_ids = get_would_be_injected_full_backend_ids('n_id', holder)
        assert set(full_backend_ids) == expected_full_backend_ids


def test_rps_limiter_macro_validation():
    instance_macro_pb = modules_pb2.InstanceMacro()
    instance_macro_pb.sd.SetInParent()
    instance_macro = InstanceMacro(instance_macro_pb)

    rps_limiter_macro = modules_pb2.RpsLimiterMacro()
    rps_limiter_macro.record_name = u'macro'
    m = RpsLimiterMacro(rps_limiter_macro)

    for installation in ALLOWED_BY_DEFAULT:
        rps_limiter_macro.installation = installation
        m.validate(ctx=ValidationCtx(), chained_modules=True, preceding_modules=[instance_macro])
        m.validate(ctx=ValidationCtx(rps_limiter_allowed_installations=[]), chained_modules=True,
                   preceding_modules=[instance_macro])
        m.validate(ctx=ValidationCtx(rps_limiter_allowed_installations=[u'EDUCATION']), chained_modules=True,
                   preceding_modules=[instance_macro])

    for installation in rps_limiter_settings.get_non_public_installation_names():
        rps_limiter_macro.installation = installation
        for allowed_installation_names in (None, []):
            e = get_validation_exception(
                m.validate, ctx=ValidationCtx(rps_limiter_allowed_installations=allowed_installation_names),
                chained_modules=True)
            e.match((u'using installation "{}" is not allowed in this namespace. '
                     u'Please create a ticket in st/BALANCERSUPPORT if you need to use it.')
                    .format(rps_limiter_macro.installation))

    installation_names = quote_join_sorted(rps_limiter_settings.get_available_installation_names())
    rps_limiter_macro.installation = u'invalid'
    for allowed_installation_names in (None, [], [''], [rps_limiter_macro.installation]):
        e = get_validation_exception(
            m.validate,
            ctx=ValidationCtx(rps_limiter_allowed_installations=allowed_installation_names),
            chained_modules=True
        )
        e.match(u'installation "{}" doesn\'t exist. Available installations: {}'
                .format(rps_limiter_macro.installation, installation_names))


def test_l7_macro_rps_limiter_validation():
    pb = modules_pb2.L7Macro.RpsLimiterSettings.External()
    pb.record_name = u'f'
    m = L7MacroRpsLimiterExternal(pb)

    for installation in ALLOWED_BY_DEFAULT:
        pb.installation = installation
        m.validate(ctx=ValidationCtx())
        m.validate(ctx=ValidationCtx(rps_limiter_allowed_installations=[]))
        m.validate(ctx=ValidationCtx(rps_limiter_allowed_installations=[u'']))
        m.validate(ctx=ValidationCtx(rps_limiter_allowed_installations=[u'invalid']))

    for installation in rps_limiter_settings.get_non_public_installation_names():
        pb.installation = installation
        for allowed_installation_names in (None, []):
            e = get_validation_exception(
                m.validate, ctx=ValidationCtx(rps_limiter_allowed_installations=allowed_installation_names))
            e.match((u'using installation "{}" is not allowed in this namespace. '
                     u'Please create a ticket in st/BALANCERSUPPORT if you need to use it.')
                    .format(pb.installation))

    installation_names = quote_join_sorted(rps_limiter_settings.get_available_installation_names())
    pb.installation = u'invalid'
    for allowed_installation_names in (None, [], [u''], [pb.installation]):
        e = get_validation_exception(
            m.validate,
            ctx=ValidationCtx(rps_limiter_allowed_installations=allowed_installation_names)
        )
        e.match(u'installation "{}" doesn\'t exist. Available installations: {}'
                .format(pb.installation, installation_names))
