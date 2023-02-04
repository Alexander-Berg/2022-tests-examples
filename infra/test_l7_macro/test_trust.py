import pytest

from awacs.wrappers.base import wrap
from awtest.wrappers import get_default_l7_macro_pb, get_wrapped_validation_exception_msg
from infra.awacs.proto import modules_pb2


@pytest.mark.parametrize('version, trust, contains_header', [
    ('0.3.0', False, 'create_func'),
    ('0.3.0', True, 'create_func_weak')
])
def test_030_x_forwarded_for_y_autoadd_header(version, trust, contains_header):
    holder_pb = get_default_l7_macro_pb()
    pb = holder_pb.l7_macro
    pb.version = version

    pb.core.trust_x_forwarded_for_y = trust

    m = wrap(holder_pb)
    m.module.validate()
    m.expand_immediate_contained_macro()
    headers_pb = holder_pb.instance_macro.sections[1].value.nested.extended_http_macro.nested.regexp.sections[
        0].value.nested.modules[0].headers

    action_pb = getattr(headers_pb, contains_header)
    assert ['X-Forwarded-For-Y'] == list(action_pb.keys())
    assert ['realip'] == list(action_pb.values())


@pytest.mark.parametrize('version, header_in_config, error', [
    ('0.3.0', False, None),
    (
        '0.3.0', True,
        "headers[1]: You can't modify the `X-Forwarded-For-Y` header manually "
        "in l7_macro version 0.3.0 and above. The only available action is `log`"
    ),
    ('0.2.7', True, None),
    ('0.2.7', False, None),
])
def test_x_forwarded_for_y_validate(version, header_in_config, error):
    holder_pb = get_default_l7_macro_pb()
    pb = holder_pb.l7_macro
    pb.version = version

    action_pb = pb.headers.add()
    action_pb.create.target = 'Z-Forwarded-For-Y'
    action_pb.create.func = 'realip'
    action_pb.create.keep_existing = True

    if header_in_config:
        action_pb2 = pb.headers.add()
        action_pb2.create.target = 'X-Forwarded-For-Y'
        action_pb2.create.func = 'realip'
        action_pb2.create.keep_existing = True

    assert get_wrapped_validation_exception_msg(pb) == error


@pytest.mark.parametrize('version, error', [
    ('0.3.0', None),
    ('0.2.0', 'core: trust_x_forwarded_for_y is not available in versions prior to 0.3.0'),
])
def test_trust_xffy_validate(version, error):
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.version = version
    pb.http.ports.append(80)
    pb.https.ports.append(443)
    pb.https.certs.add(id='test-cert')
    pb.core.trust_x_forwarded_for_y = True

    assert get_wrapped_validation_exception_msg(pb) == error


def test_trust_x_yandex_ja_x_validate():
    blank_l7_macro = get_default_l7_macro_pb()
    pb = blank_l7_macro.l7_macro
    pb.version = '0.3.2'

    # 1) set core.trust_x_yandex_ja_x in an early version
    pb.core.trust_x_yandex_ja_x = True
    assert get_wrapped_validation_exception_msg(blank_l7_macro) == (
        'l7_macro -> core -> trust_x_yandex_ja_x: is not available in versions prior to 0.3.3')

    # 2) update to the supporting version
    pb.version = '0.3.3'
    assert get_wrapped_validation_exception_msg(blank_l7_macro) == (
        'l7_macro -> core -> trust_x_yandex_ja_x: cannot be set without configuring antirobot')

    # 3) add antirobot
    pb.antirobot.SetInParent()
    pb.announce_check_reply.SetInParent()
    pb.announce_check_reply.url_re = '.*'
    pb.health_check_reply.SetInParent()
    assert get_wrapped_validation_exception_msg(blank_l7_macro) is None
