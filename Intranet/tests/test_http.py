# -*- coding: utf-8 -*-
import pytest
import httpx

from httpx import Response

from intranet.forms.micro.core.src.conf import AppTypeSettings
from intranet.forms.micro.core.src.http import HttpxTvmAuth
from intranet.forms.micro.core.src.types import AppTypes


@pytest.mark.asyncio
async def test_httpx_tvm_auth_async(respx_mock):
    url = 'http://yandex.ru/test'
    data = {'status': 'OK'}
    tvm2_client_id = 123

    mock_response = Response(200, json=data)
    route = respx_mock.get(url).mock(return_value=mock_response)
    settings = AppTypeSettings(app_type=AppTypes.forms_int)
    auth = HttpxTvmAuth(settings, tvm2_client_id)

    async with httpx.AsyncClient() as client:
        response = await client.get(url, auth=auth)

    assert response.status_code == 200
    assert response.json() == data

    assert route.called
    assert route.call_count == 1
    headers = route.calls.last.request.headers
    assert headers['x-ya-service-ticket'] == f'serv:{tvm2_client_id}'


def test_httpx_tvm_auth_sync(respx_mock):
    url = 'http://yandex.ru/test'
    data = {'status': 'OK'}
    tvm2_client_id = 123

    mock_response = Response(200, json=data)
    route = respx_mock.get(url).mock(return_value=mock_response)
    settings = AppTypeSettings(app_type=AppTypes.forms_int)
    auth = HttpxTvmAuth(settings, tvm2_client_id)

    response = httpx.get(url, auth=auth)

    assert response.status_code == 200
    assert response.json() == data

    assert route.called
    assert route.call_count == 1
    headers = route.calls.last.request.headers
    assert headers['x-ya-service-ticket'] == f'serv:{tvm2_client_id}'
