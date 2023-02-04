# -*- coding: utf-8 -*-
import pytest

from fastapi import Request
from unittest.mock import patch

from intranet.forms.micro.core.src.auth import SessionCookieAuth, TvmServiceAuth
from intranet.forms.micro.core.src.conf import AppTypeSettings
from intranet.forms.micro.core.src.types import AppTypes
from intranet.forms.micro.core.src.user import AnonymousUser, YandexUser, ServiceUser, get_yauser


@pytest.fixture
def api_request():
    scope = {
        'type': 'http',
        'settings': AppTypeSettings(app_type=AppTypes.forms_int),
    }
    return Request(scope)


@pytest.mark.asyncio
async def test_get_yauser_should_return_service_user(api_request):
    with (
        patch.object(TvmServiceAuth, 'auth') as mock_tvm,
        patch.object(SessionCookieAuth, 'auth') as mock_cookie,
    ):
        mock_tvm.return_value = ServiceUser(src=123, uid=1234)
        mock_cookie.return_value = None
        yauser = await get_yauser(api_request)

    assert isinstance(yauser, ServiceUser)
    assert yauser.authenticated
    assert yauser.src == 123
    assert yauser.uid == 1234

    mock_tvm.assert_called_once_with(api_request)
    mock_cookie.assert_not_called()


@pytest.mark.asyncio
async def test_get_yauser_should_return_yandex_user(api_request):
    with (
        patch.object(TvmServiceAuth, 'auth') as mock_tvm,
        patch.object(SessionCookieAuth, 'auth') as mock_cookie,
    ):
        mock_tvm.return_value = None
        mock_cookie.return_value = YandexUser(uid=1234)
        yauser = await get_yauser(api_request)

    assert isinstance(yauser, YandexUser)
    assert yauser.authenticated
    assert yauser.uid == 1234

    mock_tvm.assert_called_once_with(api_request)
    mock_cookie.assert_called_once_with(api_request)


@pytest.mark.asyncio
async def test_get_yauser_should_return_anonymous_user(api_request):
    with (
        patch.object(TvmServiceAuth, 'auth') as mock_tvm,
        patch.object(SessionCookieAuth, 'auth') as mock_cookie,
    ):
        mock_tvm.return_value = None
        mock_cookie.return_value = None
        yauser = await get_yauser(api_request)

    assert isinstance(yauser, AnonymousUser)
    assert not yauser.authenticated

    mock_tvm.assert_called_once_with(api_request)
    mock_cookie.assert_called_once_with(api_request)
