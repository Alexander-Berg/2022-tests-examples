# coding: utf-8
from __future__ import unicode_literals

import os
import pytest


_MOCKED_USER_TICKET = None
_MOCKED_ROBOT_ALMIGHTY_USER_TICKET = None


def vcr_proxy_matcher(r1, r2):
    """
    Костыль для трендбокса + vcr.
    В трендбоксе все https запросы идут через прокси 172.17.0.1:8888.
    Vcr не понимает, на какой хост изначально шёл запрос,
    и не может сматчиться с касетами.
    Поэтому, при запуске через трендбокс,
    вместо стандартной проверки хоста и порта, проверяем,
    что это просто попытка сходить в прокси
    """
    r2_host = r2.host
    r2_port = r2.port

    is_trendbox = os.getenv('TRENDBOX', False)
    if is_trendbox and r1.scheme == 'https' and r1.scheme == r2.scheme:
        r2_host = '172.17.0.1'
        r2_port = 8888

    assert r1.host == r2_host, '{} != {}'.format(r1.host, r2_host)
    assert r1.port == r2_port, '{} != {}'.format(r1.port, r2_port)


@pytest.fixture
def vcr_config():
    return {
        'filter_headers': [
            ('Authorization', '****'),
            ('X-Ya-Service-Ticket', '****'),
            ('X-Ya-User-Ticket', '****'),
        ],
        'decode_compressed_response': True,
        'match_on': (
            'method',
            'scheme',
            'proxy',
            'path',
            'query',
        ),
        # Игнорим localhost, чтобы VCR не создавал кассеты
        # при походах в tvmtool
        'ignore_localhost': True,
    }


@pytest.fixture
def vcr(vcr, vcr_config):
    vcr.register_matcher('proxy', vcr_proxy_matcher)
    return vcr


@pytest.fixture(autouse=True)
def no_get_user_tickets(monkeypatch):
    monkeypatch.setattr(
        'easymeeting.lib.tvm2_client.get_user_ticket',
        lambda *x, **y: _MOCKED_USER_TICKET or 'user_ticket1',
    )
    monkeypatch.setattr(
        'easymeeting.lib.tvm2_client.get_robot_almighty_user_ticket',
        lambda *x, **y: _MOCKED_ROBOT_ALMIGHTY_USER_TICKET or 'user_ticket2',
    )


@pytest.fixture(autouse=True)
def no_get_service_tickets(monkeypatch):
    if os.getenv('ENABLE_PYTEST_TVM'):
        return
    monkeypatch.setattr(
        'easymeeting.lib.tvm2_client.get_service_ticket',
        lambda *x, **y: 'service_ticket1',
    )
    monkeypatch.setattr(
        'easymeeting.lib.tvm2_client.get_service_tickets',
        lambda *x, **y: {'key': 'service_ticket1'},
    )


def pytest_runtest_setup():
    if os.getenv('ENABLE_PYTEST_TVM'):
        from easymeeting.lib.tvm2_client import (
            get_robot_user_ticket,
            get_robot_almighty_user_ticket,
        )
        # Явно мокаем user-тикеты до запуска тестов, чтобы внутри тестов
        # не делать запросов в blackbox и не создавать на них vcr-кассеты
        global _MOCKED_USER_TICKET, _MOCKED_ROBOT_ALMIGHTY_USER_TICKET
        _MOCKED_USER_TICKET = get_robot_user_ticket()
        _MOCKED_ROBOT_ALMIGHTY_USER_TICKET = get_robot_almighty_user_ticket()
