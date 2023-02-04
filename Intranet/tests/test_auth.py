# -*- coding: utf-8 -*-
import pytest

from fastapi import Request
from unittest.mock import patch

from intranet.forms.micro.core.src.auth import SessionCookieAuth, TvmServiceAuth
from intranet.forms.micro.core.src.blackbox import InternalBlackbox, ExternalBlackbox
from intranet.forms.micro.core.src.conf import AppTypeSettings
from intranet.forms.micro.core.src.http import MockAsyncTVM2
from intranet.forms.micro.core.src.types import AppTypes
from intranet.forms.micro.core.src.user import YandexUser, ServiceUser


def test_base_auth_get_headers():
    auth_inst = SessionCookieAuth()

    scope = {
        'type': 'http',
        'headers': {
            b'x-real-ip': b'127.0.0.1',
            b'cookie': b'Session_id=1:234',
        },
    }
    headers = auth_inst.get_headers(Request(scope))
    assert headers == scope['headers']

    scope = {
        'type': 'http',
    }
    headers = auth_inst.get_headers(Request(scope))
    assert headers == {}


def test_base_auth_get_settings():
    auth_inst = SessionCookieAuth()

    scope = {
        'type': 'http',
        'settings': AppTypeSettings(
            app_type=AppTypes.forms_int,
            blackbox_name='ProdYateam',
        ),
    }
    settings = auth_inst.get_settings(Request(scope))
    assert settings == scope['settings']

    scope = {
        'type': 'http',
    }
    with pytest.raises(KeyError):
        auth_inst.get_settings(Request(scope))


def test_session_cookie_auth_get_cookies():
    auth_inst = SessionCookieAuth()

    headers = {
        b'x-real-ip': b'127.0.0.1',
        b'cookie': b'yandexuid=0987; Session_id=1:234; sessionid=4:321',
    }
    cookies = auth_inst.get_cookies(headers)
    assert cookies == {
        'yandexuid': '0987', 'Session_id': '1:234', 'sessionid': '4:321',
    }

    headers = {
        b'x-real-ip': b'127.0.0.1',
    }
    cookies = auth_inst.get_cookies(headers)
    assert cookies == {}


def test_session_cookie_auth_get_session_id():
    auth_inst = SessionCookieAuth()

    headers = {
        b'x-real-ip': b'127.0.0.1',
        b'cookie': b'yandexuid=0987; Session_id=1:234; sessionid=4:321',
    }
    session_id = auth_inst.get_session_id(headers)
    assert session_id == '1:234'

    headers = {
        b'x-real-ip': b'127.0.0.1',
    }
    session_id = auth_inst.get_session_id(headers)
    assert session_id is None


def test_session_cookie_auth_get_user_ip():
    auth_inst = SessionCookieAuth()

    headers = {
        b'x-real-ip': b'127.0.0.1',
        b'cookie': b'yandexuid=0987; Session_id=1:234; sessionid=4:321',
    }
    user_ip = auth_inst.get_user_ip(headers)
    assert user_ip == '127.0.0.1'

    headers = {
        b'cookie': b'yandexuid=0987; Session_id=1:234; sessionid=4:321',
    }
    with patch('intranet.forms.micro.core.src.auth.get_instance_ip') as mock_instance_ip:
        mock_instance_ip.return_value = '::1'
        user_ip = auth_inst.get_user_ip(headers)
    assert user_ip == '::1'
    mock_instance_ip.assert_called_once_with()


def test_session_cookie_auth_get_blackbox():
    auth_inst = SessionCookieAuth()

    settings = AppTypeSettings(
        app_type=AppTypes.forms_int,
        blackbox_name='ProdYateam',
    )
    blackbox = auth_inst.get_blackbox(settings)
    assert isinstance(blackbox, InternalBlackbox)

    settings = AppTypeSettings(
        app_type=AppTypes.forms_ext,
        blackbox_name='ProdYateam',
    )
    blackbox = auth_inst.get_blackbox(settings)
    assert isinstance(blackbox, InternalBlackbox)

    settings = AppTypeSettings(
        app_type=AppTypes.forms_biz,
        blackbox_name='Mimino',
    )
    blackbox = auth_inst.get_blackbox(settings)
    assert isinstance(blackbox, ExternalBlackbox)


@pytest.mark.asyncio
async def test_session_cookie_auth_should_return_yandex_user():
    auth_inst = SessionCookieAuth()

    settings = AppTypeSettings(
        app_type=AppTypes.forms_int,
        blackbox_name='ProdYateam',
    )
    scope = {
        'type': 'http',
        'headers': {
            b'x-real-ip': b'127.0.0.1',
            b'cookie': b'Session_id=1:234',
        },
        'settings': settings,
    }
    request = Request(scope)

    with patch.object(InternalBlackbox, 'sessionid') as mock_sessionid:
        mock_sessionid.return_value = '1234'
        yauser = await auth_inst.auth(request)

    assert isinstance(yauser, YandexUser)
    assert yauser.uid == '1234'
    mock_sessionid.assert_called_once_with('1:234', '127.0.0.1')


@pytest.mark.asyncio
async def test_session_cookie_auth_should_return_none_1():
    auth_inst = SessionCookieAuth()

    settings = AppTypeSettings(
        app_type=AppTypes.forms_int,
        blackbox_name='ProdYateam',
    )
    scope = {
        'type': 'http',
        'headers': {
            b'x-real-ip': b'127.0.0.1',
            b'cookie': b'Session_id=1:234',
        },
        'settings': settings,
    }
    request = Request(scope)

    with patch.object(InternalBlackbox, 'sessionid') as mock_sessionid:
        mock_sessionid.return_value = None
        yauser = await auth_inst.auth(request)

    assert yauser is None
    mock_sessionid.assert_called_once_with('1:234', '127.0.0.1')


@pytest.mark.asyncio
async def test_session_cookie_auth_should_return_none_2():
    auth_inst = SessionCookieAuth()

    settings = AppTypeSettings(
        app_type=AppTypes.forms_int,
        blackbox_name='ProdYateam',
    )
    scope = {
        'type': 'http',
        'headers': {
            b'x-real-ip': b'127.0.0.1',
        },
        'settings': settings,
    }
    request = Request(scope)

    with patch.object(InternalBlackbox, 'sessionid') as mock_sessionid:
        mock_sessionid.return_value = None
        yauser = await auth_inst.auth(request)

    assert yauser is None
    mock_sessionid.assert_not_called()


@pytest.mark.asyncio
async def test_tvm_service_auth_should_return_service_user():
    auth_inst = TvmServiceAuth()

    settings = AppTypeSettings(
        app_type=AppTypes.forms_int,
        blackbox_name='ProdYateam',
    )
    scope = {
        'type': 'http',
        'headers': {
            b'x-real-ip': b'127.0.0.1',
            b'x-ya-service-ticket': b'serv:123',
            b'x-uid': b'1234',
        },
        'settings': settings,
    }
    request = Request(scope)

    yauser = await auth_inst.auth(request)

    assert isinstance(yauser, ServiceUser)
    assert yauser.src == '123'
    assert yauser.uid == '1234'


@pytest.mark.asyncio
async def test_tvm_service_auth_should_return_none_1():
    auth_inst = TvmServiceAuth()

    settings = AppTypeSettings(
        app_type=AppTypes.forms_int,
        blackbox_name='ProdYateam',
    )
    scope = {
        'type': 'http',
        'headers': {
            b'x-real-ip': b'127.0.0.1',
            b'x-ya-service-ticket': b'serv:123',
        },
        'settings': settings,
    }
    request = Request(scope)

    yauser = await auth_inst.auth(request)

    assert yauser is None


@pytest.mark.asyncio
async def test_tvm_service_auth_should_return_none_2():
    auth_inst = TvmServiceAuth()

    settings = AppTypeSettings(
        app_type=AppTypes.forms_int,
        blackbox_name='ProdYateam',
    )
    scope = {
        'type': 'http',
        'headers': {
            b'x-real-ip': b'127.0.0.1',
            b'x-uid': b'1234',
        },
        'settings': settings,
    }
    request = Request(scope)

    yauser = await auth_inst.auth(request)

    assert yauser is None


@pytest.mark.asyncio
async def test_tvm_service_auth_should_return_none_3():
    auth_inst = TvmServiceAuth()

    settings = AppTypeSettings(
        app_type=AppTypes.forms_int,
        blackbox_name='ProdYateam',
    )
    scope = {
        'type': 'http',
        'headers': {
            b'x-real-ip': b'127.0.0.1',
            b'x-ya-service-ticket': b'serv:123',
            b'x-uid': b'1234',
        },
        'settings': settings,
    }
    request = Request(scope)

    with patch.object(MockAsyncTVM2, 'parse_service_ticket') as mock_parse:
        mock_parse.return_value = None
        yauser = await auth_inst.auth(request)

    assert yauser is None
    mock_parse.assert_called_once_with('serv:123')
