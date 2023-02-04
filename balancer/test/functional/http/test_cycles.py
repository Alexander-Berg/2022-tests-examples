# -*- coding: utf-8 -*-

import pytest

from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig
from configs import CyclesConfig


HEADER = 'X-Yandex-Internal-Cycles'
ID_DELIMITER = ';'
COUNTER_DELIMITER = '-'
CYCLES_LUA_MD5 = '6f9376afbafeb8ba72f00e8ab6af6318'  # adjust, if config was changed
OTHER_MD5 = 'e5478797d797c4970b09720a70707999'
OTHER_MD5_2 = 'ffffffffffffffff0b09720a70707999'
OTHER_MD5_3 = 'ffeeeeeeeeeeffff0b09720a70707999'
OTHER_MD5_4 = 'fffffffffdddddd77777720a70707999'
MAX_LENGTH = '120'
MAX_CYCLES = '15'
COUNTER_ALLOWED = '14'
COUNTER_ALLOWED_INC = '15'
COUNTER_NOT_ALLOWED = '15'
COUNTER_NOT_ALLOWED2 = '55555'

CYCLES_LUA_ALLOWED = CYCLES_LUA_MD5+COUNTER_DELIMITER+COUNTER_ALLOWED
CYCLES_LUA_ALLOWED_RESULT = CYCLES_LUA_MD5+COUNTER_DELIMITER+COUNTER_ALLOWED_INC

CYCLES_LUA_NOT_ALLOWED = CYCLES_LUA_MD5+COUNTER_DELIMITER+COUNTER_NOT_ALLOWED
CYCLES_LUA_NOT_ALLOWED2 = CYCLES_LUA_MD5+COUNTER_DELIMITER+COUNTER_NOT_ALLOWED2

CYCLES_LUA_OTHER = OTHER_MD5+COUNTER_DELIMITER+COUNTER_NOT_ALLOWED
CYCLES_LUA_OTHER2 = OTHER_MD5_2+COUNTER_DELIMITER+COUNTER_ALLOWED
CYCLES_LUA_OTHER3 = OTHER_MD5_3+COUNTER_DELIMITER+COUNTER_NOT_ALLOWED
CYCLES_LUA_OTHER4 = OTHER_MD5_4+COUNTER_DELIMITER+COUNTER_ALLOWED

HEADER_ALLOWED = CYCLES_LUA_ALLOWED
HEADER_ALLOWED_RESULT = CYCLES_LUA_ALLOWED_RESULT

HEADER_ALLOWED2 = CYCLES_LUA_ALLOWED+ID_DELIMITER+CYCLES_LUA_OTHER+ID_DELIMITER+CYCLES_LUA_OTHER2
HEADER_ALLOWED2_RESULT = CYCLES_LUA_ALLOWED_RESULT+ID_DELIMITER+CYCLES_LUA_OTHER+ID_DELIMITER+CYCLES_LUA_OTHER2

HEADER_ALLOWED3 = CYCLES_LUA_OTHER+ID_DELIMITER+CYCLES_LUA_ALLOWED+ID_DELIMITER+CYCLES_LUA_OTHER2
HEADER_ALLOWED3_RESULT = CYCLES_LUA_OTHER+ID_DELIMITER+CYCLES_LUA_ALLOWED_RESULT+ID_DELIMITER+CYCLES_LUA_OTHER2

HEADER_ALLOWED4 = CYCLES_LUA_OTHER2+ID_DELIMITER+CYCLES_LUA_OTHER+ID_DELIMITER+CYCLES_LUA_ALLOWED
HEADER_ALLOWED4_RESULT = CYCLES_LUA_OTHER2+ID_DELIMITER+CYCLES_LUA_OTHER+ID_DELIMITER+CYCLES_LUA_ALLOWED_RESULT

HEADER_TOO_LONG = CYCLES_LUA_ALLOWED+ID_DELIMITER+CYCLES_LUA_OTHER3+ID_DELIMITER+CYCLES_LUA_OTHER4
HEADER_TOO_LONG_RESULT = CYCLES_LUA_ALLOWED_RESULT+ID_DELIMITER+CYCLES_LUA_OTHER3+ID_DELIMITER+CYCLES_LUA_OTHER4

HEADER_NOT_ALLOWED = {HEADER: CYCLES_LUA_NOT_ALLOWED}
HEADER_NOT_ALLOWED2 = {HEADER: CYCLES_LUA_NOT_ALLOWED2+ID_DELIMITER+CYCLES_LUA_OTHER+ID_DELIMITER+CYCLES_LUA_OTHER2}
HEADER_NOT_ALLOWED3 = {HEADER: CYCLES_LUA_OTHER+ID_DELIMITER+CYCLES_LUA_NOT_ALLOWED+ID_DELIMITER+CYCLES_LUA_OTHER2}
HEADER_NOT_ALLOWED4 = {HEADER: CYCLES_LUA_OTHER2+ID_DELIMITER+CYCLES_LUA_OTHER+ID_DELIMITER+CYCLES_LUA_NOT_ALLOWED}

HEADER_INVALID = {HEADER: CYCLES_LUA_ALLOWED+'hoou'+COUNTER_DELIMITER+COUNTER_ALLOWED}
HEADER_INVALID2 = {HEADER: CYCLES_LUA_MD5+COUNTER_DELIMITER+COUNTER_ALLOWED+CYCLES_LUA_OTHER2}
HEADER_INVALID3 = {HEADER: CYCLES_LUA_OTHER2+ID_DELIMITER+CYCLES_LUA_MD5+COUNTER_DELIMITER+'qqq'}
HEADER_INVALID4 = {HEADER: CYCLES_LUA_MD5+COUNTER_DELIMITER+ID_DELIMITER}

HEADER_INVALID_RESULT = CYCLES_LUA_MD5+COUNTER_DELIMITER+'1'


def start_all(ctx, default_backend_confg, max_len_alert=MAX_LENGTH, **balancer_kwargs):
    ctx.start_backend(default_backend_confg, name='default_backend')
    ctx.start_balancer(CyclesConfig(enable_cycles_protection='true', max_cycles=MAX_CYCLES, cycles_header_len_alert=max_len_alert, **balancer_kwargs))


def test_no_counter_works(ctx):
    start_all(ctx, SimpleConfig())

    response = ctx.perform_request(http.request.get())
    assert response.status == 200
    assert ctx.default_backend.state.accepted.value == 1


@pytest.mark.parametrize("header, result", [(HEADER_ALLOWED, HEADER_ALLOWED_RESULT),
                                            (HEADER_ALLOWED2, HEADER_ALLOWED2_RESULT),
                                            (HEADER_ALLOWED3, HEADER_ALLOWED3_RESULT),
                                            (HEADER_ALLOWED4, HEADER_ALLOWED4_RESULT)])
def test_allowed_counter_works(ctx, header, result):
    start_all(ctx, SimpleConfig())

    response = ctx.perform_request(http.request.get(headers={HEADER: header}))
    assert response.status == 200
    assert ctx.default_backend.state.accepted.value == 1

    req = ctx.default_backend.state.get_request()
    asserts.header_value(req, HEADER, result)


@pytest.mark.parametrize("header, result", [(HEADER_TOO_LONG, HEADER_TOO_LONG_RESULT)])
def test_long_counter_works(ctx, header, result):
    start_all(ctx, SimpleConfig(), max_len_alert='20')

    response = ctx.perform_request(http.request.get(headers={HEADER: header}))
    assert response.status == 200
    assert ctx.default_backend.state.accepted.value == 1

    req = ctx.default_backend.state.get_request()
    asserts.header_value(req, HEADER, result)


@pytest.mark.parametrize('header', [HEADER_NOT_ALLOWED, HEADER_NOT_ALLOWED2, HEADER_NOT_ALLOWED3, HEADER_NOT_ALLOWED4])
def test_cycle_stopped(ctx, header):
    start_all(ctx, SimpleConfig())

    response = ctx.perform_request(http.request.get(headers=header))
    assert response.status == 429
    assert ctx.default_backend.state.accepted.value == 0

    # check that balancer works as usual
    response = ctx.perform_request(http.request.get())
    assert response.status == 200
    assert ctx.default_backend.state.accepted.value == 1


@pytest.mark.parametrize('header', [HEADER_NOT_ALLOWED, HEADER_NOT_ALLOWED2, HEADER_NOT_ALLOWED3, HEADER_NOT_ALLOWED4])
def test_disable_by_its(ctx, header):
    its_file = ctx.manager.fs.create_file('disable_cycles_protection')
    start_all(ctx, SimpleConfig(), disable_cycles_protection_file=its_file)

    response = ctx.perform_request(http.request.get(headers=header))
    assert response.status == 200
    assert ctx.default_backend.state.accepted.value == 1

    # check that common requests work as usual
    response = ctx.perform_request(http.request.get())
    assert response.status == 200
    assert ctx.default_backend.state.accepted.value == 2

    ctx.manager.fs.remove(its_file)


@pytest.mark.parametrize('header', [HEADER_INVALID, HEADER_INVALID2, HEADER_INVALID3, HEADER_INVALID4])
def test_invalid_counter_works(ctx, header):
    start_all(ctx, SimpleConfig())

    response = ctx.perform_request(http.request.get(headers=header))
    assert response.status == 200
    assert ctx.default_backend.state.accepted.value == 1

    req = ctx.default_backend.state.get_request()
    asserts.header_value(req, HEADER, HEADER_INVALID_RESULT)

    # check that balancer works as usual
    response = ctx.perform_request(http.request.get())
    assert response.status == 200
    assert ctx.default_backend.state.accepted.value == 2
