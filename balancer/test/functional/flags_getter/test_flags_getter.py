# -*- coding: utf-8 -*-
import pytest
import time

from configs import FlagsGetterConfig

from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.balancer import asserts


def _start_all(ctx, flags_response, service_name=None, flags_path=None, flags_host=None, file_switch=None):
    backend = ctx.start_backend(
        SimpleConfig(http.response.ok()),
        name='backend'
    )
    flags_backend = ctx.start_backend(
        SimpleConfig(flags_response),
        name='flags_backend'
    )
    balancer = ctx.start_balancer(
        FlagsGetterConfig(
            service_name=service_name,
            flags_path=flags_path,
            flags_host=flags_host,
            file_switch=file_switch
        )
    )
    return backend, flags_backend, balancer


@pytest.mark.parametrize(
    'value',
    ['xxx', ''],
    ids=['xxx', 'empty']
)
def test_flags_request_custom(ctx, value):
    """
    Балансер должен отправить в flags идентификатор клиента и получить заголовок с флагами.
    Заголовки флагов должны быть удалены.
    """
    backend, flags_backend, balancer = _start_all(
        ctx,
        service_name='my_service_name',
        flags_host='flags-host.search.yandex.net',
        flags_path='/flags',
        flags_response=http.response.ok(headers={"x-yandex-exphandler-result": value})
    )
    resp = ctx.perform_request(http.request.get("/foobar?x=y", headers={
        'x-yandex-exphandler': 'aaa',
        'x-yandex-exphandler-result': 'bbb'
    }))
    asserts.status(resp, 200)

    flags_req = flags_backend.state.get_request()
    asserts.path(flags_req, "/flags/foobar")
    asserts.header_value(flags_req, "x-yandex-exphandler", "my_service_name")
    asserts.header_value(flags_req, "host", "flags-host.search.yandex.net")

    req = backend.state.get_request()
    asserts.no_header(req, "x-yandex-exphandler")
    asserts.header_value(req, "x-yandex-exphandler-result", value)

    unistat = ctx.get_unistat()
    assert unistat['flags_getter-my_service_name-no_header_summ'] == 0
    assert unistat['flags_getter-my_service_name-no_answer_summ'] == 0
    assert unistat['flags_getter-my_service_name-disabled_summ'] == 0
    assert unistat['flags_getter-my_service_name-success_summ'] == 1


def test_flags_request_defaults(ctx):
    """
    Балансер должен отправить в flags идентификатор клиента и получить заголовок с флагами.
    Заголовки флагов должны быть удалены.
    """
    backend, flags_backend, balancer = _start_all(
        ctx,
        flags_response=http.response.ok(headers={"X-Yandex-ExpHandler-Result": "xxx"})
    )
    resp = ctx.perform_request(http.request.get("/foobar", headers={
        'X-Yandex-ExpHandler': 'aaa',
        'x-yandex-exphandler-result': 'bbb'
    }))
    asserts.status(resp, 200)

    flags_req = flags_backend.state.get_request()
    asserts.path(flags_req, "/conflagexp/foobar")
    asserts.no_header(flags_req, "x-yandex-exphandler")
    asserts.header_value(flags_req, "host", "yandex.ru")

    req = backend.state.get_request()
    asserts.no_header(req, "x-yandex-exphandler")
    asserts.header_value(req, "x-yandex-exphandler-result", "xxx")

    unistat = ctx.get_unistat()
    assert unistat['flags_getter-no_header_summ'] == 0
    assert unistat['flags_getter-no_answer_summ'] == 0
    assert unistat['flags_getter-disabled_summ'] == 0
    assert unistat['flags_getter-success_summ'] == 1


def test_flags_request_no_header(ctx):
    """
    Балансер должен отправить в flags идентификатор клиента и получить заголовок с флагами.
    Заголовки флагов должны быть удалены.
    """
    backend, flags_backend, balancer = _start_all(
        ctx,
        flags_response=http.response.ok()
    )
    resp = ctx.perform_request(http.request.get("foobar", headers={
        'x-yandex-exphandler': 'aaa',
        'X-Yandex-ExpHandler-Result': 'bbb'
    }))
    asserts.status(resp, 200)

    req = backend.state.get_request()
    asserts.no_header(req, "x-yandex-exphandler")
    asserts.no_header(req, "x-yandex-exphandler-result")

    unistat = ctx.get_unistat()
    assert unistat['flags_getter-no_header_summ'] == 1
    assert unistat['flags_getter-no_answer_summ'] == 0
    assert unistat['flags_getter-disabled_summ'] == 0
    assert unistat['flags_getter-success_summ'] == 0


def test_flags_request_no_answer(ctx):
    """
    Балансер должен отправить в flags идентификатор клиента и получить заголовок с флагами.
    Заголовки флагов должны быть удалены.
    """
    backend, flags_backend, balancer = _start_all(
        ctx,
        flags_response=http.response.service_unavailable()
    )
    resp = ctx.perform_request(http.request.get("foobar", headers={
        'x-yandex-exphandler': 'aaa',
        'x-yandex-exphandler-result': 'bbb'
    }))
    asserts.status(resp, 200)

    req = backend.state.get_request()
    asserts.no_header(req, "x-yandex-exphandler")
    asserts.no_header(req, "x-yandex-exphandler-result")

    unistat = ctx.get_unistat()
    assert unistat['flags_getter-no_header_summ'] == 0
    assert unistat['flags_getter-no_answer_summ'] == 1
    assert unistat['flags_getter-disabled_summ'] == 0
    assert unistat['flags_getter-success_summ'] == 0


def test_file_switch(ctx):
    """
    При появлении файла file_switch балансер должен перестать ходить в flags.
    """
    file_switch = ctx.manager.fs.create_file('file_switch')
    backend, flags_backend, balancer = _start_all(
        ctx,
        file_switch=file_switch,
        flags_response=http.response.ok(headers={"X-Yandex-ExpHandler-Result": "xxx"})
    )
    resp = ctx.perform_request(http.request.get("/qwer", headers={
        'X-Yandex-ExpHandler': 'aaa',
        'x-yandex-exphandler-result': 'bbb'
    }))
    asserts.status(resp, 200)

    assert flags_backend.state.requests.empty()

    req = backend.state.get_request()
    asserts.no_header(req, "x-yandex-exphandler")
    asserts.no_header(req, "x-yandex-exphandler-result")

    unistat = ctx.get_unistat()
    assert unistat['flags_getter-no_header_summ'] == 0
    assert unistat['flags_getter-no_answer_summ'] == 0
    assert unistat['flags_getter-disabled_summ'] == 1
    assert unistat['flags_getter-success_summ'] == 0

    ctx.manager.fs.remove(file_switch)
    time.sleep(3)

    resp = ctx.perform_request(http.request.get("/asdf", headers={
        'X-Yandex-ExpHandler': 'aaa',
        'x-yandex-exphandler-result': 'bbb'
    }))
    asserts.status(resp, 200)

    flags_req = flags_backend.state.get_request()
    asserts.path(flags_req, "/conflagexp/asdf")
    asserts.no_header(flags_req, "x-yandex-exphandler")
    asserts.header_value(flags_req, "host", "yandex.ru")

    req = backend.state.get_request()
    asserts.no_header(req, "x-yandex-exphandler")
    asserts.header_value(req, "x-yandex-exphandler-result", "xxx")

    unistat = ctx.get_unistat()
    assert unistat['flags_getter-no_header_summ'] == 0
    assert unistat['flags_getter-no_answer_summ'] == 0
    assert unistat['flags_getter-disabled_summ'] == 1
    assert unistat['flags_getter-success_summ'] == 1

    ctx.manager.fs.create_file(file_switch)
    time.sleep(3)

    resp = ctx.perform_request(http.request.get("/zxcv", headers={
        'X-Yandex-ExpHandler': 'aaa',
        'x-yandex-exphandler-result': 'bbb'
    }))
    asserts.status(resp, 200)

    assert flags_backend.state.requests.empty(), flags_backend.state.get_request()
    req = backend.state.get_request()
    asserts.no_header(req, "x-yandex-exphandler")
    asserts.no_header(req, "x-yandex-exphandler-result")

    unistat = ctx.get_unistat()
    assert unistat['flags_getter-no_header_summ'] == 0
    assert unistat['flags_getter-no_answer_summ'] == 0
    assert unistat['flags_getter-disabled_summ'] == 2
    assert unistat['flags_getter-success_summ'] == 1
