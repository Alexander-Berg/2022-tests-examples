from awacs.wrappers.base import wrap
from awacs.wrappers.l7macro import to_header_module_pbs, L7MacroHeaderAction, Mode
from awtest.wrappers import get_exception_msg
from infra.awacs.proto import modules_pb2


def test_l7_macro_to_headers_pbs():
    a_0_pb = modules_pb2.L7Macro.HeaderAction()
    a_0_pb.log.target_re = u'X-.*'
    a_0_pb.log.cookie_fields.extend([u'A', u'B'])

    a_1_pb = modules_pb2.L7Macro.HeaderAction()
    a_1_pb.create.target = u'Req-Id'
    a_1_pb.create.value = u'123'
    a_1_pb.create.keep_existing = True

    a_2_pb = modules_pb2.L7Macro.HeaderAction()
    a_2_pb.create.target = u'Req-Id'
    a_2_pb.create.func = u'reqid'

    a_3_pb = modules_pb2.L7Macro.HeaderAction()
    a_3_pb.create.target = u'Client-IP'
    a_3_pb.create.func = u'realip'
    a_3_pb.create.keep_existing = True

    a_4_pb = modules_pb2.L7Macro.HeaderAction()
    a_4_pb.delete.target_re = u'.*'

    a_5_pb = modules_pb2.L7Macro.HeaderAction()
    a_5_pb.copy.target = u'A'
    a_5_pb.copy.source = u'B'
    a_5_pb.copy.keep_existing = True

    a_6_pb = modules_pb2.L7Macro.HeaderAction()
    a_6_pb.copy.target = u'C'
    a_6_pb.copy.source = u'D'

    a_7_pb = modules_pb2.L7Macro.HeaderAction()
    a_7_pb.append.target = u'Req-Id'
    a_7_pb.append.func = u'reqid'

    a_8_pb = modules_pb2.L7Macro.HeaderAction()
    a_8_pb.append.target = u'Req-Id-2'
    a_8_pb.append.value = u'123'
    a_8_pb.append.do_not_create_if_missing = True

    a_9_pb = modules_pb2.L7Macro.HeaderAction()
    a_9_pb.rewrite.target = u'X-Location'
    a_9_pb.rewrite.pattern.re = u'.*'
    a_9_pb.rewrite.pattern.case_sensitive.value = True
    a_9_pb.rewrite.replacement = u'https://xxx.yandex-team.ru%{url}'

    a_10_pb = modules_pb2.L7Macro.HeaderAction()
    a_10_pb.rewrite.target = u'X-Location'
    a_10_pb.rewrite.pattern.re = u'xxx'
    getattr(a_10_pb.rewrite.pattern, u'global').value = True
    a_10_pb.rewrite.pattern.literal.value = True
    a_10_pb.rewrite.replacement = u'yyy'

    a_11_pb = modules_pb2.L7Macro.HeaderAction()
    a_11_pb.uaas.service_name = 'uaas_test'

    a_12_pb = modules_pb2.L7Macro.HeaderAction()
    a_12_pb.laas.SetInParent()

    a_13_pb = modules_pb2.L7Macro.HeaderAction()
    a_13_pb.create.target = u'Req-Id-2'
    a_13_pb.create.func = u'reqid'

    a_14_pb = modules_pb2.L7Macro.HeaderAction()
    a_14_pb.create.target = u'Client-IP-2'
    a_14_pb.create.func = u'realip'
    a_14_pb.create.keep_existing = True

    headers_pbs = to_header_module_pbs(
        map(L7MacroHeaderAction, [a_0_pb, a_1_pb, a_2_pb, a_3_pb, a_4_pb, a_5_pb,
                                  a_6_pb, a_7_pb, a_8_pb, a_9_pb, a_10_pb, a_11_pb, a_12_pb, a_13_pb, a_14_pb]),
        mode=Mode.HEADERS)

    h_0_pb = modules_pb2.LogHeadersModule(name_re=u'X-.*', cookie_fields=[u'A', u'B'])

    h_1_pb = modules_pb2.HeadersModule()
    h_1_pb.create_weak.add(key=u'Req-Id', value=u'123')

    h_2_pb = modules_pb2.HeadersModule()
    h_2_pb.create_func[u'Req-Id'] = u'reqid'
    h_2_pb.create_func_weak[u'Client-IP'] = u'realip'

    h_3_pb = modules_pb2.HeadersModule()
    h_3_pb.delete = u'.*'

    h_4_pb = modules_pb2.HeadersModule()
    h_4_pb.copy_weak.add(key=u'B', value=u'A')

    h_5_pb = modules_pb2.HeadersModule()
    h_5_pb.copy.add(key=u'D', value=u'C')

    h_6_pb = modules_pb2.HeadersModule()
    h_6_pb.append_func[u'Req-Id'] = u'reqid'
    h_6_pb.append_weak.add(key=u'Req-Id-2', value=u'123')

    h_7_pb = modules_pb2.RewriteModule()
    a_pb = h_7_pb.actions.add(
        header_name=u'X-Location',
        regexp=u'.*',
        rewrite=u'https://xxx.yandex-team.ru%{url}'
    )
    a_pb.case_insensitive.value = False

    a_pb = h_7_pb.actions.add(
        header_name=u'X-Location',
        regexp=u'xxx',
        rewrite=u'yyy',
        literal=True
    )
    a_pb.case_insensitive.value = True
    setattr(a_pb, u'global', True)

    h_8_pb = modules_pb2.ExpGetterMacro()
    h_8_pb.service_name = 'uaas_test'

    h_9_pb = modules_pb2.GeobaseMacro(version='0.0.1')

    h_10_pb = modules_pb2.HeadersModule()
    h_10_pb.create_func[u'Req-Id-2'] = u'reqid'
    h_10_pb.create_func_weak[u'Client-IP-2'] = u'realip'

    expected_headers_pbs = [h_0_pb, h_1_pb, h_2_pb, h_3_pb, h_4_pb, h_5_pb, h_6_pb, h_7_pb, h_8_pb, h_9_pb, h_10_pb]

    assert headers_pbs == expected_headers_pbs


def test_l7_macro_to_headers_pbs_2():
    a_0_pb = modules_pb2.L7Macro.HeaderAction()
    a_0_pb.log.target_re = u'X-.*'
    a_0_pb.log.cookie_fields.extend([u'A', u'B'])

    a_1_pb = modules_pb2.L7Macro.HeaderAction()
    a_1_pb.log.target_re = u'X-.*'
    a_1_pb.log.cookie_fields.extend([u'A', u'B'])

    headers_pbs = to_header_module_pbs(
        list(map(L7MacroHeaderAction, [a_0_pb, a_1_pb])),
        mode=Mode.HEADERS)
    response_headers_pbs = to_header_module_pbs(
        list(map(L7MacroHeaderAction, [a_0_pb, a_1_pb])),
        mode=Mode.RESPONSE_HEADERS)

    assert headers_pbs[0] == modules_pb2.LogHeadersModule(name_re=u'X-.*', cookie_fields=[u'A', u'B'])
    assert response_headers_pbs[0] == modules_pb2.LogHeadersModule(response_name_re=u'X-.*', cookie_fields=[u'A', u'B'])


def test_l7_macro_to_headers_pbs_3():
    a_0_pb = modules_pb2.L7Macro.HeaderAction()
    a_0_pb.append.target = u'Y-Req-Id'
    a_0_pb.append.value = u'123'

    a_1_pb = modules_pb2.L7Macro.HeaderAction()
    a_1_pb.create.target = u'Req-Id'
    a_1_pb.create.func = u'reqid'

    a_2_pb = modules_pb2.L7Macro.HeaderAction()
    a_2_pb.create.target = u'X-Req-Id'
    a_2_pb.create.func = u'x-reqid'

    a_3_pb = modules_pb2.L7Macro.HeaderAction()
    a_3_pb.create.target = u'Req-Id'
    a_3_pb.create.func = u'reqid-2'

    a_4_pb = modules_pb2.L7Macro.HeaderAction()
    a_4_pb.append.target = u'Y-Req-Id'
    a_4_pb.append.value = u'12345'

    a_5_pb = modules_pb2.L7Macro.HeaderAction()
    a_5_pb.append.target = u'Y-Req-Id'
    a_5_pb.append.value = u'123456'

    headers_pbs = to_header_module_pbs(
        list(map(L7MacroHeaderAction, [a_0_pb, a_1_pb, a_2_pb, a_3_pb, a_4_pb, a_5_pb])),
        mode=Mode.HEADERS)

    cls = modules_pb2.HeadersModule
    h_1_pb = cls()
    h_1_pb.create_func[u'Req-Id'] = u'reqid-2'
    h_1_pb.create_func[u'X-Req-Id'] = u'x-reqid'
    h_1_pb.append.add(key=u'Y-Req-Id', value=u'123')
    h_2_pb = cls()
    h_2_pb.append.add(key=u'Y-Req-Id', value=u'12345')
    h_3_pb = cls()
    h_3_pb.append.add(key=u'Y-Req-Id', value=u'123456')

    expected_headers_pbs = [h_1_pb, h_2_pb, h_3_pb]

    assert headers_pbs == expected_headers_pbs


def test_l7_macro_to_headers_pbs_4():
    # test duplicates
    a_0_pb = modules_pb2.L7Macro.HeaderAction()
    a_0_pb.append.target = u'X-Smth'
    a_0_pb.append.func = u'realip'

    a_1_pb = modules_pb2.L7Macro.HeaderAction()
    a_1_pb.append.target = u'X-Smth'
    a_1_pb.append.func = u'whatever'

    a_2_pb = modules_pb2.L7Macro.HeaderAction()
    a_2_pb.append.target = u'X-Smth'
    a_2_pb.append.func = u'whatever'

    a_3_pb = modules_pb2.L7Macro.HeaderAction()
    a_3_pb.create.target = u'X-Smth'
    a_3_pb.create.func = u'whatever'

    a_4_pb = modules_pb2.L7Macro.HeaderAction()
    a_4_pb.create.target = u'X-Smth'
    a_4_pb.create.func = u'whatever'

    a_5_pb = modules_pb2.L7Macro.HeaderAction()
    a_5_pb.create.target = u'Y-Smth'
    a_5_pb.create.value = u'whatever'

    a_6_pb = modules_pb2.L7Macro.HeaderAction()
    a_6_pb.create.target = u'Y-Smth'
    a_6_pb.create.value = u'whatever'

    a_7_pb = modules_pb2.L7Macro.HeaderAction()
    a_7_pb.delete.target_re = u'.-Smth'

    a_8_pb = modules_pb2.L7Macro.HeaderAction()
    a_8_pb.delete.target_re = u'.-Smth'

    headers_pbs = to_header_module_pbs(
        list(map(L7MacroHeaderAction, [a_0_pb, a_1_pb, a_2_pb, a_3_pb, a_4_pb, a_5_pb, a_6_pb, a_7_pb, a_8_pb])),
        mode=Mode.HEADERS)

    cls = modules_pb2.HeadersModule
    h_1_pb = cls()
    h_1_pb.append_func[u'X-Smth'] = u'realip'
    h_2_pb = cls()
    h_2_pb.append_func[u'X-Smth'] = u'whatever'
    h_3_pb = cls()
    h_3_pb.append_func[u'X-Smth'] = u'whatever'
    h_4_pb = cls()
    h_4_pb.create.add(key=u'Y-Smth', value=u'whatever')
    h_4_pb.create_func[u'X-Smth'] = u'whatever'
    h_5_pb = cls()
    h_5_pb.delete = u'.-Smth'
    expected_headers_pbs = [h_1_pb, h_2_pb, h_3_pb, h_4_pb, h_5_pb]

    assert headers_pbs == expected_headers_pbs


def test_l7_macro_header_action_validate():
    pb = modules_pb2.L7Macro.HeaderAction()
    m = wrap(pb)
    e = get_exception_msg(m.validate, mode=Mode.HEADERS)
    assert e == (
        u'at least one of the "append", "copy", "copy_from_request", "create", "decrypt_icookie", "delete", '
        u'"laas", "log", "rewrite", "uaas" must be specified')

    pb.log.SetInParent()
    m = wrap(pb)
    e = get_exception_msg(m.validate, mode=Mode.HEADERS)
    assert e == u'log -> target_re: is required'

    pb.log.target_re = u'('
    e = get_exception_msg(m.validate, mode=Mode.HEADERS)
    assert u'log -> target_re: is not a valid regexp' in e

    pb.log.target_re = u'X-.*'
    e = get_exception_msg(m.validate, mode=Mode.HEADERS)
    assert not e

    pb.rewrite.target = u'X-Location'
    pb.rewrite.pattern.re = u'.*'
    pb.rewrite.replacement = u'xxx'
    m = wrap(pb)
    e = get_exception_msg(m.validate, mode=Mode.RESPONSE_HEADERS)
    assert e == u'rewrite: is not supported for response headers'

    pb.uaas.service_name = '123'
    m = wrap(pb)
    e = get_exception_msg(m.validate, mode=Mode.RESPONSE_HEADERS)
    assert e == u'uaas: is not supported for response headers'

    e = get_exception_msg(m.validate, mode=Mode.HEADERS)
    assert not e

    pb.create.target = u'X-Number'
    pb.create.value = u'123'
    m = wrap(pb)
    e = get_exception_msg(m.validate, mode=Mode.HEADERS)
    assert not e
