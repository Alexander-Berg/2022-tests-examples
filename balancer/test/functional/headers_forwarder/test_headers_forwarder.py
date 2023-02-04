# -*- coding: utf-8 -*-
import pytest

from configs import HeadersForwarderConfig, \
    HeadersForwarderRewriteConfig, HeadersForwarderMultipleConfig, Http2HeadersForwarderConfig

from balancer.test.util.predef import http, http2
from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.process import BalancerStartError
from balancer.test.util.stream.ssl.stream import SSLClientOptions


def gen_balancer(isHttp2, ctx, **kwargs):
    if isHttp2:
        kwargs["certs_dir"] = ctx.certs.root_dir
        return Http2HeadersForwarderConfig(**kwargs)
    else:
        return HeadersForwarderConfig(**kwargs)


def make_request(isHttp2, ctx, **kwargs):
    if isHttp2:
        conn = ctx.manager.connection.http2.create_ssl(
            port=ctx.balancer.config.port,
            ssl_options=SSLClientOptions(
                alpn='h2',
                key=ctx.certs.abs_path('client.key'),
                cert=ctx.certs.abs_path('client.crt'),
                ca_file=ctx.certs.root_ca,
                quiet=False
            )
        )
        conn.write_preface()
        res = conn.perform_request(http2.request.get(**kwargs))
    else:
        res = ctx.perform_request(http.request.get(**kwargs))

    return res


@pytest.mark.parametrize(
    'http2_enabled',
    [False, True],
    ids=['http', 'http2']
)
@pytest.mark.parametrize(
    'erase_from_request',
    [False, True],
    ids=['keep_in_request', 'erase_from_request']
)
@pytest.mark.parametrize(
    ['erase_from_response', 'weak'],
    [
        (False, False),
        (False, True),
        (True, False)
    ],
    ids=['keep-both', 'weak', 'erase_from_response']
)
def test_headers_forwarder(ctx, http2_enabled, erase_from_request, erase_from_response, weak):
    """
    BALANCER-987
    Форвардинг заголовков в ответ в обход бэкэнда
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={'x-zeppelin': 'new'})))
    config = gen_balancer(http2_enabled, ctx, erase_from_request=erase_from_request, erase_from_response=erase_from_response, weak=weak)

    ctx.start_balancer(config)

    res = make_request(http2_enabled, ctx, headers={'x-led': 'original'})
    req = ctx.backend.state.get_request()

    if erase_from_request:
        asserts.no_header(req, 'x-led')
    else:
        asserts.header_value(req, 'x-led', 'original')

    if erase_from_response:
        asserts.header_value(res, 'x-zeppelin', 'original')
    elif weak:
        asserts.header_value(res, 'x-zeppelin', 'new')
    else:
        asserts.header_values(res, 'x-zeppelin', ['new', 'original'])


def test_headers_forwarder_both_erase_from_response_and_weak_specified(ctx):
    """
    BALANCER-987
    Балансер не должен стартовать если указаны одновременно erase_from_response и weak
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={'x-zeppelin': 'new'})))
    config = HeadersForwarderConfig(erase_from_request=False, erase_from_response=True, weak=True)
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(config)


@pytest.mark.parametrize(
    ['specify_request_header', 'specify_response_header'],
    [
        (False, True),
        (True, False),
        (False, False)
    ],
    ids=[
        'response_header_specified',
        'request_header_specified',
        'no_headers_specified'
    ])
def test_headers_forwarder_headers_missing(ctx, specify_request_header, specify_response_header):
    """
    BALANCER-987
    request_header и response_header - обязательные параметры
    """
    request_header = 'x-led' if specify_request_header else None
    response_header = 'x-zeppelin' if specify_response_header else None
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={'x-zeppelin': 'new'})))
    config = HeadersForwarderConfig(request_header=request_header, response_header=response_header)
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(config)


def test_headers_forwarder_multiple_actions(ctx):
    """
    BALANCER-987
    Несколько header-ов в одном модуле headers_forwarder
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok()))
    ctx.start_balancer(HeadersForwarderMultipleConfig(
        first_req_header='x-led',
        first_resp_header='x-zeppelin',
        second_req_header='x-guns',
        second_resp_header='x-roses',
        third_req_header='x-nirvana',
        third_resp_header='x-nevermind'
    ))

    value1 = "And she's buying a stairway to heaven"
    value2 = "Knock-knock-knockin' on heaven's door"
    value3 = "Come as you are"

    res = ctx.perform_request(http.request.get(headers={'x-led': value1, 'x-guns': value2, 'x-nirvana': value3}))
    req = ctx.backend.state.get_request()

    asserts.header_value(req, 'x-led', value1)
    asserts.header_value(req, 'x-guns', value2)
    asserts.no_header(req, 'x-nirvana')  # erase_from_request = true

    asserts.header_value(res, 'x-zeppelin', value1)
    asserts.header_value(res, 'x-roses', value2)
    asserts.header_value(res, 'x-nevermind', value3)


def test_headers_forwarder_misses(ctx):
    """
    BALANCER-987
    В запросе нет заголовков, не должно их появляться и в ответе.
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={'x-guns': 'this_is_to_be_removed'})))
    ctx.start_balancer(HeadersForwarderMultipleConfig(
        first_req_header='x-led',
        first_resp_header='x-zeppelin',
        second_req_header='x-guns',
        second_resp_header='x-roses',
        third_req_header='x-nirvana',
        third_resp_header='x-nevermind'
    ))

    res = ctx.perform_request(http.request.get())
    req = ctx.backend.state.get_request()

    asserts.no_header(req, 'x-led')
    asserts.no_header(req, 'x-guns')
    asserts.no_header(req, 'x-nirvana')

    asserts.no_header(res, 'x-zeppelin')
    asserts.no_header(res, 'x-roses')
    asserts.no_header(res, 'x-nevermind')


def test_headers_forwarder_multiple_actions_duplicated_keys(ctx):
    """
    BALANCER-1707
    Несколько header-ов в одном модуле headers_forwarder c одинаковыми источниками
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok()))
    ctx.start_balancer(HeadersForwarderMultipleConfig(
        first_req_header='x-led',
        first_resp_header='x-zeppelin',
        second_req_header='x-led',
        second_resp_header='x-roses',
        third_req_header='x-led',
        third_resp_header='x-nevermind'
    ))

    value1 = "And she's buying a stairway to heaven"

    res = ctx.perform_request(http.request.get(headers={'x-led': value1}))
    req = ctx.backend.state.get_request()

    asserts.no_header(req, 'x-led')  # erase_from_request = true

    asserts.header_value(res, 'x-zeppelin', value1)
    asserts.header_value(res, 'x-roses', value1)
    asserts.header_value(res, 'x-nevermind', value1)


def test_header_appended(ctx):
    """
    BALANCER-2681
    Если в ответе есть заголовки, headers_forwarder без weak и erase_from_response
    добавляет новый заголовок, сохраняя старые.
    Также тестируется схема с генерацией заголовков ответа через комбинацию headers, rewrite и headers_forwarder
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={'Set-Cookie': 'yandexuid=123'})))
    ctx.start_balancer(HeadersForwarderRewriteConfig(
        request_header='X-Set-Cookie',
        response_header='Set-Cookie',
        header_value='321',
        regexp='.*',
        rewrite='starttime=%0',
        erase_from_request=True
    ))

    resp = ctx.perform_request(http.request.get(headers={'X-Set-Cookie': 'starttime=321'}))
    req = ctx.backend.state.get_request()
    asserts.no_header(req, 'X-Set-Cookie')
    asserts.header_values(resp, 'Set-Cookie', ['yandexuid=123', 'starttime=321'])
