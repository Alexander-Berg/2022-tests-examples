# -*- coding: utf-8 -*-
import pytest

from configs import RewriteConfig, RewriteSchemeConfig, SeveralActionsConfig

from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.process import BalancerStartError
from balancer.test.util.predef import http
from urllib import urlencode


REWRITE_REQUEST = http.request.raw_get(path='/lalala?a=b&c=d', headers={'Bebebe': 'LAlala', 'Content-Length': 0})
REWRITE_HOST_REQUEST = http.request.raw_get(path='/lalala?a=b&c=d', headers={
    'Host': 'betatest.ru',
    'Bebebe': 'LAlala',
    'Content-Length': 0,
})


def base_test(ctx, request=None, **balancer_kwargs):
    if request is None:
        request = REWRITE_REQUEST
    ctx.start_backend(SimpleConfig())

    ctx.start_balancer(RewriteConfig(**balancer_kwargs))

    response = ctx.perform_request(request)
    asserts.status(response, 200)
    return ctx.backend.state.get_request()


def base_url_test(ctx, url, request=None, **balancer_kwargs):
    new_request = base_test(ctx, request=request, **balancer_kwargs)
    asserts.path(new_request, url)


def test_rewrite_url(ctx):
    """
    Общая работоспособность модуля rewrite

    необходимый конфиг
    default = {

        rewrite = {
            actions = {
                {
                    regexp = 'a';
                    rewrite = 'F';
                    literal = 1;
                    case_insensitive = 1;
                    global = 1;
                    split = 'url';
                };
            };

        proxy = {
            host = "127.0.0.1"; port = 8765;
            timeout = "0.3s"; backend_timeout = "1000s";
            fail_on_5xx = 0;
            };
        };
    };
    """
    base_url_test(ctx, url='/lFlFlF?F=b&c=d', regexp='a', rewrite='F', literal=1, case=1, glob=1)


def test_rewrite_query(ctx):
    """
    Общая работоспособность модуля rewrite

    необходимый конфиг
    default = {

        rewrite = {
            actions = {
                {
                    regexp = 'a';
                    rewrite = 'и';
                    split = 'query';
                };
            };

        proxy = {
            host = "127.0.0.1"; port = 8765;
            timeout = "0.3s"; backend_timeout = "1000s";
            fail_on_5xx = 0;
            };
        };
    };
    """
    base_url_test(ctx, url='/lalala?и=b&c=d', regexp='a', rewrite='и', split='query')


def test_rewrite_path(ctx):
    """
    Общая работоспособность модуля rewrite

    необходимый конфиг
    default = {

        rewrite = {
            actions = {
                {
                    regexp = 'a';
                    rewrite = 'd';
                    global = 1;
                    literal = 1;
                    split = 'path';
                };
            };

        proxy = {
            host = "127.0.0.1"; port = 8765;
            timeout = "0.3s"; backend_timeout = "1000s";
            fail_on_5xx = 0;
            };
        };
    };
    """
    base_url_test(ctx, url='/ldldld?a=b&c=d', regexp='a', rewrite='d', glob=1, literal=1, split='path')


def test_rewrite_normalized_path_relative(ctx):
    """
    BALANCER-3233
    """
    base_url_test(ctx, url='/ldldld?a=b&c=d', regexp='a', rewrite='d', glob=1, literal=1, split='normalized_path')


def test_rewrite_normalized_path_absolute(ctx):
    """
    BALANCER-3233
    """
    request=http.request.raw_get(path='http://yandex.ru/lalala?a=b&c=d')
    base_url_test(ctx, url='http://yandex.ru/ldldld?a=b&c=d', request=request, regexp='a', rewrite='d', glob=1, literal=1, split='normalized_path')


def test_rewrite_cgi(ctx):
    """
    MINOTAUR-3076
    Проверяем что rewrite корректно добавляет CGI параметры и поддерживает в значениях параметров любые символы
    """
    cgi = {
        'a': [
            r'~!@#$%^&*((((((()_+=/\\',
            '~1234=45',
        ],
        'b': 'a=1=2=3',
    }
    cgi = urlencode(cgi, True)
    encoded_cgi = cgi.replace('%', '%%')
    request = http.request.raw_get(path='http://ya.ru/')
    base_url_test(ctx, request=request, url='http://ya.ru/?{}'.format(cgi), regexp='^([^?]*)$', rewrite='%1?{}'.format(encoded_cgi), split='url')


def test_rewrite_http_1_0(ctx):
    """
    SEPE-4166
    Проверяем, что на запрос заданный по http/1.0, в ответ получили content-length

    необходимый конфиг
    default = {

        rewrite = {
            actions = {
                {
                    {
                        regexp = "/clck(/.*)";
                        rewrite = "%1";
                    };
                };
            };

        proxy = {
            host = "127.0.0.1"; port = 8765;
            timeout = "0.3s"; backend_timeout = "1000s";
            fail_on_5xx = 0;
            };
        };
    };
    """
    data = 'DATA'
    ctx.start_backend(SimpleConfig(response=http.response.ok(data=data)))

    ctx.start_balancer(RewriteConfig(regexp='/clck(/.*)', rewrite='%1'))

    response = ctx.perform_request(http.request.get(path='/clck/safeclick/data=hello', version='HTTP/1.0'))

    asserts.content(response, data)


def test_rewrite_whole_url(ctx):
    """
    Если указан параметр split = "url", то подстановка должна происходить только в URL строки запроса,
    а не во всей строке запроса
    """
    base_url_test(ctx, url='/abc', regexp='.*', rewrite='/abc', glob=1, split='url')


def test_rewrite_macroses(ctx):
    """
    Проверка работоспособности макросов %{url} и %{host}
    """
    base_url_test(ctx, url='/?url=/lalala?a=b&c=d+host=betatest.ru', request=REWRITE_HOST_REQUEST,
                  regexp='.*', rewrite='/?url=%{url}+host=%{host}', glob=1, split='url')


def test_rewrite_macroses_no_host(ctx):
    """
    Если в запросе отсутствует заголовок Host, то на месте макроса %{host} должна подставиться пустая строка
    """
    base_url_test(ctx, url='/?url=/lalala?a=b&c=d+host=',
                  regexp='.*', rewrite='/?url=%{url}+host=%{host}', glob=1, split='url')


@pytest.mark.parametrize('case_insensitive', [False, True], ids=['case_sensitive', 'case_insensitive'])
def test_header_name(ctx, case_insensitive):
    """
    Если указан параметр header_name, то балансер должен переписать значение указанного заголовка.
    Регистр заголовка значения не имеет.
    """
    header_name = 'Bebebe'
    header_value = 'LAlala'
    if case_insensitive:
        headers = {header_name.lower(): header_value}
    else:
        headers = {header_name: header_value}

    request = http.request.get(path='/lalala?a=b&c=d', headers=headers)

    new_request = base_test(ctx, request=request, header_name=header_name, glob=1, regexp='a', rewrite='b')
    asserts.header_value(new_request, 'Bebebe', 'LAlblb')


def test_header_name_no_header(ctx):
    """
    Если указан параметр header_name, но указанный заголовок в запросе отстутствует,
    то балансер должен передать запрос без изменений
    """
    new_request = base_test(ctx, header_name='Location', glob=1, regexp='.*', rewrite='yandex.ru')
    asserts.no_header(new_request, 'location')


def test_header_name_groups(ctx):
    """
    Проверка подстановки по номеру группы из регулярки при использовании header_name
    """
    new_request = base_test(ctx, header_name='Bebebe', glob=1, regexp='LA(.*)', rewrite='%1')
    asserts.header_value(new_request, 'Bebebe', 'lala')


def test_header_name_macroses(ctx):
    """
    Проверка подстановки макросов %{url} и %{host} в значение заголовка
    """
    new_request = base_test(ctx, request=REWRITE_HOST_REQUEST,
                            header_name='Bebebe', glob=1, regexp='.*', rewrite='%{host}%{url}')
    asserts.header_value(new_request, 'bebebe', 'betatest.ru/lalala?a=b&c=d')


def test_rewrite_scheme(ctx):
    """
    BALANCER-250
    Проверка специального правила %{scheme}, которое регистрирует наличие ssl соединения
    """
    ctx.start_balancer(RewriteSchemeConfig(cert_dir=ctx.certs.root_dir))

    # http
    resp = ctx.perform_request(http.request.get())

    asserts.status(resp, 200)
    asserts.header_value(resp, 'cardigans', 'http')

    resp = None

    # https
    with ctx.manager.connection.http.create_ssl(port=ctx.balancer.config.ssl_port) as conn:
        resp = conn.perform_request(http.request.get())

    asserts.status(resp, 200)
    asserts.header_value(resp, 'cardigans', 'https')

    resp = None


def test_several_headers(ctx):
    """
        Checks that all actions are evalueated with header
    """

    ctx.start_backend(SimpleConfig())

    regexp1, regexp2, regexp3 = ".*", ".*", ".*"
    rewrite1, rewrite2, rewrite3 = "1", "2", "3"
    header1, header2, header3 = "X-ya", "X-yb", "X-Ya"

    ctx.start_balancer(SeveralActionsConfig(
        regexp1=regexp1, rewrite1=rewrite1, header_name1=header1,
        regexp2=regexp2, rewrite2=rewrite2, header_name2=header2,
        regexp3=regexp3, rewrite3=rewrite3, header_name3=header3
        )
    )

    response = ctx.perform_request(http.request.get(headers={'X-ya': '0'}))
    asserts.status(response, 200)
    backend_request = ctx.backend.state.get_request()
    asserts.header_value(backend_request, 'x-ya', '3')


@pytest.mark.parametrize(
    "host",
    [("aaa", "xxx"), ("bbb", "yyy"), ("ccc", "zzz")]
)
def test_several_headers_2(ctx, host):
    """
        Checks that all actions are evalueated with header
    """

    ctx.start_backend(SimpleConfig())

    regexp1, regexp2, regexp3 = "aaa", "bbb", "ccc"
    rewrite1, rewrite2, rewrite3 = "xxx", "yyy", "zzz"

    ctx.start_balancer(SeveralActionsConfig(
        regexp1=regexp1, rewrite1=rewrite1, header_name1="host",
        regexp2=regexp2, rewrite2=rewrite2, header_name2="host",
        regexp3=regexp3, rewrite3=rewrite3, header_name3="host",
    ))

    response = ctx.perform_request(http.request.get(headers={'host': host[0]}))
    asserts.status(response, 200)
    backend_request = ctx.backend.state.get_request()
    asserts.header_value(backend_request, 'host', host[1])


def test_merge_slashes(ctx):
    """
    BALANCER-2614
    """
    base_url_test(
        ctx, request=http.request.get('///a///b///c///?x///y'),
        url='/a/b/c/?x///y', regexp='/+', rewrite='/', glob=1, split='path')


def test_cut(ctx):
    """
    BALANCERSUPPORT-539
    """
    base_url_test(
        ctx, request=http.request.get('/0123456789'),
        url='/01234', regexp='(/.{0,5}).*', rewrite='%1', split='path')


@pytest.mark.parametrize('header_name', [
    'content-length',
    'transfer-encoding',
    'Content-Length',
    'Transfer-Encoding',
], ids=[
    'content-length',
    'transfer-encoding',
    'Content-Length',
    'Transfer-Encoding',
])
def test_restricted_headers_modification_does_not_start(ctx, header_name):
    """
    BALANCER-3209: Изменение заголовков Content-Length и Transfer-Encoding запрещено
    """
    request = http.request.get(path='/lalala?a=b&c=d')

    with pytest.raises(BalancerStartError):
        base_test(ctx, request=request, header_name=header_name, glob=1, regexp='a', rewrite='b')
