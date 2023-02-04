from awtest.wrappers import get_default_l7_macro_pb, get_wrapped_validation_exception_msg, FixtureStorage


def test_decrypt_icookie_validate():
    blank_l7_macro = get_default_l7_macro_pb()
    pb = blank_l7_macro.l7_macro
    pb.version = '0.3.2'

    # 1) set core.trust_icookie but not the header or antirobot
    pb.core.trust_icookie = True
    assert get_wrapped_validation_exception_msg(blank_l7_macro) == (
        "l7_macro -> core -> trust_icookie: cannot be set without decrypt_icookie"
        " action or configured antirobot")

    # 2) add decrypt_icookie header
    pb.headers.add().decrypt_icookie.SetInParent()
    assert get_wrapped_validation_exception_msg(blank_l7_macro) is None

    # 3) add antirobot
    pb.antirobot.SetInParent()
    pb.announce_check_reply.SetInParent()
    pb.announce_check_reply.url_re = '.*'
    pb.health_check_reply.SetInParent()
    assert get_wrapped_validation_exception_msg(blank_l7_macro) is None

    # 4) blank l7macro with core.trust_icookie and antirobot, no header
    blank_l7_macro = get_default_l7_macro_pb()
    pb = blank_l7_macro.l7_macro
    pb.version = '0.3.2'
    pb.core.trust_icookie = True
    pb.antirobot.SetInParent()
    pb.announce_check_reply.SetInParent()
    pb.announce_check_reply.url_re = '.*'
    pb.health_check_reply.SetInParent()
    assert get_wrapped_validation_exception_msg(blank_l7_macro) is None

    # 5) header with early version
    blank_l7_macro = get_default_l7_macro_pb()
    pb = blank_l7_macro.l7_macro
    pb.version = '0.3.1'
    pb.headers.add().decrypt_icookie.SetInParent()
    assert get_wrapped_validation_exception_msg(blank_l7_macro) == (
        "l7_macro -> headers[0]: the `decrypt_icookie` header "
        "is not available in versions prior to 0.3.2")

    # 6) antirobot with early version
    blank_l7_macro = get_default_l7_macro_pb()
    pb = blank_l7_macro.l7_macro
    pb.version = '0.2.0'
    pb.antirobot.SetInParent()
    pb.announce_check_reply.SetInParent()
    pb.announce_check_reply.url_re = '.*'
    pb.health_check_reply.SetInParent()
    assert get_wrapped_validation_exception_msg(blank_l7_macro) is None

    # 7) core.trust_icookie in early version
    pb.core.trust_icookie = True
    assert get_wrapped_validation_exception_msg(blank_l7_macro) == (
        'l7_macro -> core -> trust_icookie: is not available in versions prior to 0.3.2')


def test_decrypt_icookie_expand():
    fixture_storage = FixtureStorage(u'test_l7_macro', u'icookie')
    blank_l7_macro_pb = get_default_l7_macro_pb()
    pb = blank_l7_macro_pb.l7_macro
    pb.version = '0.3.2'
    pb.headers.add().decrypt_icookie.SetInParent()
    fixture_storage.assert_pb_is_equal_to_file(blank_l7_macro_pb, 'decrypt_icookie_header.txt')

    blank_l7_macro_pb = get_default_l7_macro_pb()
    pb = blank_l7_macro_pb.l7_macro
    pb.version = '0.3.2'
    pb.headers.add().decrypt_icookie.SetInParent()
    pb.core.trust_icookie = True
    fixture_storage.assert_pb_is_equal_to_file(blank_l7_macro_pb, 'decrypt_icookie_header_trust_icookie.txt')

    blank_l7_macro_pb = get_default_l7_macro_pb()
    pb = blank_l7_macro_pb.l7_macro
    pb.version = '0.3.2'
    pb.antirobot.SetInParent()
    pb.announce_check_reply.SetInParent()
    pb.announce_check_reply.url_re = '.*'
    pb.health_check_reply.SetInParent()
    fixture_storage.assert_pb_is_equal_to_file(blank_l7_macro_pb, 'antirorobot.txt')

    blank_l7_macro_pb = get_default_l7_macro_pb()
    pb = blank_l7_macro_pb.l7_macro
    pb.version = '0.3.2'
    pb.antirobot.SetInParent()
    pb.announce_check_reply.SetInParent()
    pb.announce_check_reply.url_re = '.*'
    pb.health_check_reply.SetInParent()
    pb.core.trust_icookie = True
    fixture_storage.assert_pb_is_equal_to_file(blank_l7_macro_pb, 'antirorobot_trust_icookie.txt')

    blank_l7_macro_pb = get_default_l7_macro_pb()
    pb = blank_l7_macro_pb.l7_macro
    pb.version = '0.3.2'
    pb.antirobot.SetInParent()
    pb.announce_check_reply.SetInParent()
    pb.announce_check_reply.url_re = '.*'
    pb.health_check_reply.SetInParent()
    pb.core.trust_icookie = True
    pb.headers.add().decrypt_icookie.SetInParent()
    fixture_storage.assert_pb_is_equal_to_file(blank_l7_macro_pb, 'antirorobot_and_header_trust_icookie.txt')

    blank_l7_macro_pb = get_default_l7_macro_pb()
    pb = blank_l7_macro_pb.l7_macro
    pb.version = '0.2.12'
    pb.antirobot.SetInParent()
    pb.announce_check_reply.SetInParent()
    pb.announce_check_reply.url_re = '.*'
    pb.health_check_reply.SetInParent()
    fixture_storage.assert_pb_is_equal_to_file(blank_l7_macro_pb, 'antirorobot_in_l7_v0_2_11.txt')
