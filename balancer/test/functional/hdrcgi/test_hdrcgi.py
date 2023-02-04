# -*- coding: utf-8 -*-
"""
SEPE-5562
Конфиг:
    cgi_from_hdr = {
        [cgi_1] = cgi_hdr_1;
        [cgi_2] = cgi_hdr_2;
    }; -- cgi_from_hdr
    hdr_from_cgi = {
        [hdr_1] = hdr_cgi_1;
        [hdr_2] = hdr_cgi_2;
    }; -- hdr_from_cgi
"""
import pytest

from configs import HdrcgiConfig

from balancer.test.util.process import BalancerStartError
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http
from balancer.test.util import asserts


def start_all(ctx, **balancer_kwargs):
    ctx.start_backend(SimpleConfig())
    return ctx.start_balancer(HdrcgiConfig(**balancer_kwargs))


def test_cgi_from_hdr_single(ctx):
    """
    Если заголовок запроса сматчился cgi_hdr_1,
    то должен добавиться параметр cgi_1 со значением этого заголовка
    """
    param_name = 'Yellow'
    header_value = 'Submarine'
    start_all(ctx, cgi_hdr_1='G.*n', cgi_1=param_name)
    response = ctx.perform_request(http.request.get(path="/?a=b&a=b&c", headers={'Green': header_value}))

    asserts.status(response, 200)
    req = ctx.backend.state.get_request()
    asserts.cgi_value(req, param_name, header_value)
    asserts.cgi_value(req, "a", ["b", "b"])
    asserts.cgi_value(req, "c", [""])


def test_cgi_from_hdr_none(ctx):
    """
    Если заголовок запроса не сматчился cgi_hdr_1,
    то cgi запроса должны остаться без изменений
    BALANCER-1609
    """
    param_name = 'Yellow'
    header_value = 'Submarine'
    start_all(ctx, cgi_hdr_1='B.*n', cgi_1=param_name)
    response = ctx.perform_request(http.request.get(path="/?a=b&a=b&c", headers={'Green': header_value}))

    asserts.status(response, 200)
    req = ctx.backend.state.get_request()
    asserts.no_cgi_value(req, param_name, header_value)
    asserts.cgi_value(req, "a", ["b", "b"])
    asserts.cgi_value(req, "c", [""])


def test_cgi_from_hdr_multiple(ctx):
    """
    Если один заголовок запроса сматчился cgi_hdr_1, а другой cgi_hdr_2,
    то должны соответственно добавиться параметры cgi_1 и cgi_2 со значениями этих заголовков
    """
    param1 = 'Yellow'
    param2 = 'Submarine'
    value1 = 'Hey'
    value2 = 'Jude'
    start_all(ctx, cgi_hdr_1='G.*n', cgi_1=param1, cgi_hdr_2='B.*e', cgi_2=param2)

    response = ctx.perform_request(http.request.get(headers={'Green': value1, 'Bye': value2}))
    asserts.status(response, 200)

    req = ctx.backend.state.get_request()
    asserts.cgi_value(req, param1, value1)
    asserts.cgi_value(req, param2, value2)


def test_hdr_from_cgi_single(ctx):
    """
    Если в запросе присутствует параметр hdr_cgi_1,
    то должен добавиться заголовок hdr_1 со значением этого параметра
    """
    param = 'Yellow'
    header_name = 'Green'
    value = 'Submarine'
    start_all(ctx, hdr_cgi_1=param, hdr_1=header_name)

    response = ctx.perform_request(http.request.get(path='/yandsearch?%s=%s' % (param, value)))
    asserts.status(response, 200)

    req = ctx.backend.state.get_request()
    asserts.header_value(req, header_name, value)


def test_hdr_from_cgi_multiple(ctx):
    """
    Если в запросе присутствуют параметры hdr_cgi_1 и hdr_cgi_2,
    то должны добавиться заголовки hdr_1 и hdr_2 со значениями соответствующих параметров
    """
    param1 = 'Yellow'
    param2 = 'Hey'
    header1 = 'Green'
    header2 = 'Bye'
    value1 = 'Submarine'
    value2 = 'Jude'
    start_all(ctx, hdr_cgi_1=param1, hdr_1=header1, hdr_cgi_2=param2, hdr_2=header2)

    response = ctx.perform_request(
        http.request.get(path='/yandsearch?%s=%s&%s=%s' % (param1, value1, param2, value2))
    )
    asserts.status(response, 200)

    req = ctx.backend.state.get_request()
    asserts.header_value(req, header1, value1)
    asserts.header_value(req, header2, value2)


def test_hdr_from_cgi_multiple_post(ctx):
    """
    Если в запросе присутствуют параметры hdr_cgi_1 и hdr_cgi_2,
    то должны добавиться заголовки hdr_1 и hdr_2 со значениями соответствующих параметров
    """
    param1 = 'Yellow'
    param2 = 'Hey'
    header1 = 'Green'
    header2 = 'Bye'
    value1 = 'Submarine'
    value2 = 'Jude'
    start_all(ctx, hdr_cgi_1=param1, hdr_1=header1, hdr_cgi_2=param2, hdr_2=header2, body_scan_limit=32)

    data = "%s=%s" % (param2, value2)
    response = ctx.perform_request(
        http.request.post(
            path='/yandsearch?%s=%s' % (param1, value1),
            headers={'content-type': 'application/x-www-form-urlencoded; charset=UTF-8'},
            data=data
        )
    )
    asserts.status(response, 200)
    req = ctx.backend.state.get_request()

    asserts.content(req, data)
    asserts.header_value(req, header1, value1)
    asserts.header_value(req, header2, value2)


def test_hdr_from_cgi_multiple_post_content_type_mismatch(ctx):
    """
    Если в запросе присутствуют параметры hdr_cgi_1 и hdr_cgi_2, но content-type не application/x-www-form-urlencoded
    то должен добавиться только заголовок hdr_1
    """
    param1 = 'Yellow'
    param2 = 'Hey'
    header1 = 'Green'
    header2 = 'Bye'
    value1 = 'Submarine'
    value2 = 'Jude'
    start_all(ctx, hdr_cgi_1=param1, hdr_1=header1, hdr_cgi_2=param2, hdr_2=header2, body_scan_limit=32)

    data = "%s=%s" % (param2, value2)
    response = ctx.perform_request(
        http.request.post(
            path='/yandsearch?%s=%s' % (param1, value1),
            headers={'content-type': 'text/plain'},
            data=data
        )
    )
    asserts.status(response, 200)
    req = ctx.backend.state.get_request()

    asserts.content(req, data)
    asserts.header_value(req, header1, value1)
    asserts.no_header(req, header2)


def test_hdr_from_cgi_multiple_post_scan_past_limit(ctx):
    """
    Если в запросе присутствуют параметры hdr_cgi_1 и hdr_cgi_2, но hdr_cgi_2 находится за лимитом длины скана,
    то должен добавиться только заголовок hdr_1
    """
    param1 = 'Yellow'
    param2 = 'Hey'
    header1 = 'Green'
    header2 = 'Bye'
    value1 = 'Submarine'
    value2 = 'Jude'
    start_all(ctx, hdr_cgi_1=param1, hdr_1=header1, hdr_cgi_2=param2, hdr_2=header2, body_scan_limit=32)

    data = "foo=bar&" * 4 + "%s=%s" % (param2, value2)
    response = ctx.perform_request(
        http.request.post(
            path='/yandsearch?%s=%s' % (param1, value1),
            headers={'content-type': 'application/x-www-form-urlencoded'},
            data=data
        )
    )
    asserts.status(response, 200)
    req = ctx.backend.state.get_request()

    asserts.content(req, data)
    asserts.header_value(req, header1, value1)
    asserts.no_header(req, header2)


def test_hdr_from_cgi_multiple_post_scan_near_limit(ctx):
    """
    Если в запросе присутствуют параметры hdr_cgi_1 и hdr_cgi_2, но hdr_cgi_2 находится до лимита длины скана,
    то должны добавиться заголовки hdr_1 и hdr_2 со значениями соответствующих параметров.
    Возможно, обрезанными по body_scan_limit.
    """
    param1 = 'Yellow'
    param2 = 'Hey'
    header1 = 'Green'
    header2 = 'Bye'
    value1 = 'Submarine'
    value2 = 'Jude'
    start_all(ctx, hdr_cgi_1=param1, hdr_1=header1, hdr_cgi_2=param2, hdr_2=header2, body_scan_limit=32)

    data = "foo=bar&" * 3 + "x&%s=%s" % (param2, value2)
    assert len(data) == 34

    response = ctx.perform_request(
        http.request.post(
            path='/yandsearch?%s=%s' % (param1, value1),
            headers={'content-type': 'application/x-www-form-urlencoded'},
            data=data
        )
    )
    asserts.status(response, 200)
    req = ctx.backend.state.get_request()

    asserts.content(req, data)
    asserts.header_value(req, header1, value1)
    asserts.header_value(req, header2, value2[0:2])  # value2 is truncated by the scan limit


def test_cgi_from_hdr_add_existing_param(ctx):
    """
    Если заголовок запроса сматчился cgi_hdr_1, и в запросе уже есть параметр cgi_1,
    то должен добавиться еще один параметр cgi_1 со значением заголовка
    """
    param = 'Hey'
    value1 = 'Jude'
    value2 = 'You'

    start_all(ctx, cgi_hdr_1='B.*e', cgi_1=param)

    response = ctx.perform_request(http.request.get(path='/yandsearch?%s=%s' % (param, value1),
                                                    headers={'Bye': value2}))
    asserts.status(response, 200)

    req = ctx.backend.state.get_request()
    asserts.cgi_value(req, param, value1)
    asserts.cgi_value(req, param, value2)


def test_hdr_from_cgi_add_existing_header(ctx):
    """
    Если в запросе присутствует параметр hdr_cgi_1,
    то при добавлении заголовка hdr_1 все одноименные заголовки удаляются
    """
    param = 'Green'
    header = 'Yellow'
    value1 = 'Submarine'
    value2 = 'Lamborghini'

    start_all(ctx, hdr_cgi_1=param, hdr_1=header)

    response = ctx.perform_request(http.request.get(path='/yandsearch?%s=%s' % (param, value1),
                                                    headers={header: value2}))
    asserts.status(response, 200)

    req = ctx.backend.state.get_request()
    asserts.single_header(req, header)
    asserts.header_value(req, header, value1)


def test_cgi_from_hdr_percent_encoding(ctx):
    """
    Если заголовок запроса сматчился cgi_hdr_1, и значение заголовка содержит символы >0x80,
    то должен добавиться параметр cgi_1 со значением этого заголовка, в котором символы заменены на %XX
    """
    param = 'help'
    header = 'Help'

    start_all(ctx, cgi_hdr_1=header, cgi_1=param)

    response = ctx.perform_request(http.request.get(headers={header: '\x81'}))
    asserts.status(response, 200)

    req = ctx.backend.state.get_request()
    path = req.request_line.path
    assert '%s=%%81' % param in path, 'Invalid percent-encoded value: %s' % path


@pytest.mark.parametrize(
    ['param', 'value'],
    [
        ("help", "%20%me%20"),
        ("help", "%")
    ],
    ids=["invalid_1", "invalid_2"]
)
def test_hdr_from_cgi_percent_encoding(ctx, param, value):
    """
    Если в запросе присутствует параметр hdr_cgi_1, значение которого содержит percent-encoding символы,
    то должен добавиться заголовок hdr_1 со значением этого параметра без изменений
    """
    header_name = 'Help'

    start_all(ctx, hdr_cgi_1=param, hdr_1=header_name)

    response = ctx.perform_request(http.request.get(path='/yandsearch?%s=%s' % (param, value)))
    asserts.status(response, 200)

    req = ctx.backend.state.get_request()
    asserts.header_value(req, header_name, value)


def test_hdr_from_cgi_duplicate_params(ctx):
    """
    Если в запросе присутствует несколько параметров hdr_cgi_1,
    то должен добавиться заголовок hdr_1 со значением первого из параметров
    """
    header_name = 'Green'
    param = 'Yellow'
    value1 = 'Submarine'
    value2 = 'Lamborghini'

    start_all(ctx, hdr_cgi_1=param, hdr_1=header_name)

    response = ctx.perform_request(http.request.get(
        path='/yandsearch?%s=%s&%s=%s' % (param, value1, param, value2)))
    asserts.status(response, 200)

    req = ctx.backend.state.get_request()
    asserts.header_value(req, header_name, value1)


def test_cgi_and_hdr(ctx):
    """
    Одновременное преобразование заголовка в cgi-параметр и cgi-параметра в заголовок
    """
    from_param = 'in_my'
    from_param_value = 'life'
    from_header = 'Yellow'
    from_header_value = 'Submarine'
    to_param = 'Green'
    to_header = 'it_s_my'

    start_all(ctx, cgi_hdr_1=from_header, cgi_1=to_param, hdr_cgi_1=from_param, hdr_1=to_header)

    response = ctx.perform_request(http.request.get(path='/yandsearch?%s=%s' % (from_param, from_param_value),
                                                    headers={from_header: from_header_value}))
    asserts.status(response, 200)

    req = ctx.backend.state.get_request()
    asserts.header_value(req, to_header, from_param_value)
    asserts.cgi_value(req, to_param, from_header_value)


def test_cgi_from_hdr_duplicate_headers(ctx):
    """
    Если несколько заголовков соответстуют регулярному выражению cgi_hdr_1,
    то должен добавиться параметр cgi_1 со значением первого заголовка
    """
    param = 'Green'
    value1 = 'Submarine'
    value2 = 'Park'

    start_all(ctx, cgi_hdr_1='Yell.*', cgi_1=param)

    response = ctx.perform_request(http.request.raw_get(headers=[('Yellow', value1), ('Yellowstone', value2)]))
    asserts.status(response, 200)

    req = ctx.backend.state.get_request()
    asserts.cgi_value(req, param, value1)
    asserts.no_cgi_value(req, param, value2)


def test_add_hdr_from_cgi_param(ctx):
    """
    Если в cgi_from_hdr добавился параметр, соответсвующий hdr_cgi_1,
    то заголовок hdr_1 добавляться не должен
    """
    param = 'Yellow'
    from_header = 'Green'
    to_header = 'Nuclear'
    value = 'Submarine'

    start_all(ctx, cgi_hdr_1=from_header, cgi_1=param, hdr_cgi_1=param, hdr_1=to_header)

    response = ctx.perform_request(http.request.get(headers={from_header: value}))
    asserts.status(response, 200)

    req = ctx.backend.state.get_request()
    asserts.no_header_value(req, to_header, value)


def test_add_cgi_from_hdr_header(ctx):
    """
    Если в hdr_from_cgi добавился заголовок, соответсвующий cgi_hdr_1,
    то параметр cgi_1 добавляться не должен
    """
    header = 'Yellow'
    from_param = 'Green'
    to_param = 'Nuclear'
    value = 'Submarine'

    start_all(ctx, cgi_hdr_1=header, cgi_1=to_param, hdr_cgi_1=from_param, hdr_1=header)

    response = ctx.perform_request(http.request.get(path='/yandsearch?%s=%s' % (from_param, value)))
    asserts.status(response, 200)

    req = ctx.backend.state.get_request()
    asserts.no_cgi_value(req, to_param, value)


def test_complex_hdr_regexp(ctx):
    """
    Тест на более сложную регулярку на заголовок
    """
    param = 'header'

    start_all(ctx, cgi_hdr_1=r'X-(\w+)-(\d+)-(a|b)-[^z]-[1-3]-Z{3}', cgi_1=param)

    response = ctx.perform_request(http.request.get(headers={
        'Y-abc-987-a-x-2-ZZZ': '7',
        'X-ab0-987-b-x-2-ZZZ': '6',
        'X-abc-98a-a-x-2-ZZZ': '5',
        'X-abc-987-c-x-2-ZZZ': '4',
        'X-abc-987-b-z-2-ZZZ': '3',
        'X-abc-987-a-x-4-ZZZ': '2',
        'X-abc-987-b-x-2-ZZ': '1',
        'X-abc-987-b-x-2-ZZZ': '0',
    }))
    asserts.status(response, 200)

    req = ctx.backend.state.get_request()
    asserts.cgi_value(req, param, '0')


def test_invalid_cgi(ctx):
    """
    Если в конфиге указан неверный cgi-параметр,
    то балансер должен завершиться с кодом возврата 1
    """
    ctx.start_fake_backend()

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(HdrcgiConfig(cgi_1='param*'))  # TODO check return code


def test_invalid_header(ctx):
    """
    Если в конфиге указан неверная регулярка для заголовков,
    то балансер должен завершиться с кодом возврата 1
    """
    ctx.start_fake_backend()

    with pytest.raises(BalancerStartError):
        ctx.start_balancer(HdrcgiConfig(hdr_1='X-Header**'))  # TODO check return code
