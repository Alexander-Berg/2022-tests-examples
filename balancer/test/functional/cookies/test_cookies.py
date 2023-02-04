# -*- coding: utf-8 -*-
"""
SEPE-3930
"""
import datetime

from configs import CookiesConfig, CookiesNamesakeConfig

from balancer.test.util import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http


def build_datetime(microseconds):
    return datetime.datetime.fromtimestamp(microseconds / 1000000.0)


def test_delete_cookies(ctx):
    """
    Проверка удаления кук
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(CookiesConfig(delete_regexp='for-delete.*'))
    ctx.perform_request(http.request.get(headers={
        'cookie': ";".join([
            'For-delete-1=1',
            'For-delete-2=2',
            'For-delete=3',
            'For-delete',
            'for-Delete=4',
            'Pink=Floyd',
            'Led=Zeppelin',
        ])
    }))

    req = ctx.backend.state.get_request()
    asserts.no_header(req, 'for-delete')
    asserts.no_header(req, 'for-delete-1')
    asserts.no_header(req, 'for-delete-2')


def test_create_cookies(ctx):
    """
    Проверка создания кук
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(CookiesConfig(enable_create=1))
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'cookie', 'Foo=bar')


def test_create_existing_cookies(ctx):
    """
    Если запрос содержит куки, указанные в create,
    то балансер должен удалить все эти куки и добавить куки с указанным значением
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(CookiesConfig(enable_create=1))
    ctx.perform_request(http.request.get(headers=[
        ('cookie', 'a;Foo=1;b'),
        ('cookie', 'Foo;Foo=baz;x;y'),
    ]))

    req = ctx.backend.state.get_request()
    asserts.single_header(req, 'cookie')
    asserts.header_value(req, 'cookie', 'a; b; x; y; Foo=bar')


def test_create_multiple_cookies(ctx):
    """
    Проверка добавления нескольких кук
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(CookiesConfig(enable_create_multiple=1))
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'cookie', 'boo=moo; Foo=bar')


def test_create_weak(ctx):
    """
    Если в запросе нет куки, указанной в create_weak, то балансер должен её добавить
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(CookiesConfig(enable_create_weak=1))
    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'cookie', 'Foo=bar')


def test_create_weak_existing_cookies(ctx):
    """
    Если запрос содержит куки, указанные в create_weak,
    то балансер должен оставить эти куки и не добавлять кук с указанным значением
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(CookiesConfig(enable_create_weak=1))
    ctx.perform_request(http.request.get(headers=[
        ('cookie', 'Foo=baz'),
        ('cookie', 'Foo=ban'),
    ]))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'cookie', 'Foo=baz; Foo=ban')
    asserts.no_header_value(req, 'host', 'yandex.ru')


def test_cookies_namesake(ctx):
    """
    Правильность логики для одноименных кук в модуле cookies
    """
    ctx.start_backend(SimpleConfig())

    ctx.start_balancer(CookiesNamesakeConfig())

    ctx.perform_request(http.request.get())

    req = ctx.backend.state.get_request()

    asserts.header_value(req, 'cookie', 'namesake=weak; namesake=strong')


def test_cookies_namesake_delete(ctx):
    """
    Правильность логики для одноименных кук в модуле cookies
    """
    ctx.start_backend(SimpleConfig())

    ctx.start_balancer(CookiesNamesakeConfig())

    ctx.perform_request(http.request.get(headers={'cookie': 'namesake=original'}))

    req = ctx.backend.state.get_request()

    asserts.single_header(req, 'cookie')
    asserts.header_value(req, 'cookie', 'namesake=strong')


def _build_datetime(microseconds):
    return datetime.datetime.fromtimestamp(microseconds / 1000000.0)


def test_create_func_starttime(ctx):
    """
    Проверка добавления куки со временем начала обработки запроса
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(CookiesConfig(create_func='starttime'))
    conn = ctx.create_http_connection()
    start_time = datetime.datetime.now()
    conn.perform_request(http.request.get())
    fin_time = datetime.datetime.now()

    req = ctx.backend.state.get_request()
    asserts.header(req, 'cookie')
    val = req.headers.get_one('cookie')
    assert val.startswith('func=')
    assert start_time <= _build_datetime(int(val[5:])) <= fin_time


def test_create_func_starttime_strong(ctx):
    """
    Проверка добавления куки со временем начала обработки запроса
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(CookiesConfig(create_func='starttime'))
    conn = ctx.create_http_connection()
    start_time = datetime.datetime.now()
    conn.perform_request(http.request.get(headers={'cookie': 'func=0'}))
    fin_time = datetime.datetime.now()

    req = ctx.backend.state.get_request()
    asserts.header(req, 'cookie')
    val = req.headers.get_one('cookie')
    val = val.split(';')
    for v in val:
        assert v.startswith('func=')
        assert start_time <= _build_datetime(int(v.strip()[5:])) <= fin_time


def test_create_func_starttime_weak(ctx):
    """
    Проверка добавления куки со временем начала обработки запроса
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(CookiesConfig(create_func_weak='starttime'))
    conn = ctx.create_http_connection()
    conn.perform_request(http.request.get(headers={'cookie': 'func=0'}))

    req = ctx.backend.state.get_request()
    asserts.header_value(req, 'cookie', 'func=0')


def test_create_func_starttime_weak_strong(ctx):
    """
    Проверка добавления куки со временем начала обработки запроса
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(CookiesConfig(create_func='starttime', create_func_weak='starttime'))
    conn = ctx.create_http_connection()
    start_time = datetime.datetime.now()
    conn.perform_request(http.request.get())
    fin_time = datetime.datetime.now()

    req = ctx.backend.state.get_request()
    asserts.header(req, 'cookie')
    val = req.headers.get_one('cookie')
    val = val.split(';')
    assert len(val) == 2
    for v in val:
        assert v.strip().startswith('func=')
        assert start_time <= _build_datetime(int(v.strip()[5:])) <= fin_time


def test_create_func_starttime_weak_strong_delete(ctx):
    """
    Проверка добавления куки со временем начала обработки запроса
    """
    ctx.start_backend(SimpleConfig())
    ctx.start_balancer(CookiesConfig(create_func='starttime', create_func_weak='starttime', delete_regexp='func'))
    conn = ctx.create_http_connection()
    start_time = datetime.datetime.now()
    conn.perform_request(http.request.get(headers={'cookie': 'func=0'}))
    fin_time = datetime.datetime.now()

    req = ctx.backend.state.get_request()
    asserts.header(req, 'cookie')
    val = req.headers.get_one('cookie')
    val = val.split(';')
    assert len(val) == 1
    for v in val:
        assert v.strip().startswith('func=')
        assert start_time <= _build_datetime(int(v.strip()[5:])) <= fin_time
