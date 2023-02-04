# -*- coding: utf-8 -*-
import pytest
import urllib

from httpx import Response

from intranet.forms.micro.core.src.blackbox import InternalBlackbox, ExternalBlackbox
from intranet.forms.micro.core.src.conf import AppTypeSettings
from intranet.forms.micro.core.src.types import AppTypes


@pytest.mark.asyncio
async def test_internal_blackbox_sessionid(respx_mock):
    blackbox_response = Response(200, json={
        'error': 'OK',
        'status': {'value': 'VALID'},
        'uid': {'value': '1234'},
    })
    blackbox_route = (
        respx_mock.get('http://blackbox.yandex-team.ru/blackbox')
        .mock(return_value=blackbox_response)
    )
    settings = AppTypeSettings(
        app_type=AppTypes.forms_int,
        blackbox_name='ProdYateam',
    )
    bb_inst = InternalBlackbox(settings)
    uid = await bb_inst.sessionid('1:234', '127.0.0.1')
    assert uid == '1234'

    assert blackbox_route.called
    assert blackbox_route.call_count == 1

    query_params = dict(
        param.split('=', 1)
        for param in blackbox_route.calls.last.request.url.query.decode().split('&')
    )
    assert query_params['sessionid'] == urllib.parse.quote('1:234')
    assert query_params['method'] == 'sessionid'
    assert query_params['userip'] == '127.0.0.1'
    assert query_params['format'] == 'json'
    assert query_params['host'] == bb_inst.host

    headers = blackbox_route.calls.last.request.headers
    assert headers['x-ya-service-ticket'] == f'serv:{bb_inst.blackbox_client.value}'


@pytest.mark.asyncio
async def test_external_blackbox_sessionid(respx_mock):
    blackbox_response = Response(200, json={
        'error': 'OK',
        'status': {'value': 'VALID'},
        'uid': {'value': '1234'},
    })
    blackbox_route = (
        respx_mock.get('http://blackbox-mimino.yandex.net/blackbox')
        .mock(return_value=blackbox_response)
    )
    settings = AppTypeSettings(
        app_type=AppTypes.forms_biz,
        blackbox_name='Mimino',
    )
    bb_inst = ExternalBlackbox(settings, settings.blackbox_name)
    uid = await bb_inst.sessionid('1:234', '127.0.0.1')
    assert uid == '1234'

    assert blackbox_route.called
    assert blackbox_route.call_count == 1

    query_params = dict(
        param.split('=', 1)
        for param in blackbox_route.calls.last.request.url.query.decode().split('&')
    )
    assert query_params['sessionid'] == urllib.parse.quote('1:234')
    assert query_params['method'] == 'sessionid'
    assert query_params['userip'] == '127.0.0.1'
    assert query_params['format'] == 'json'
    assert query_params['host'] == bb_inst.host

    headers = blackbox_route.calls.last.request.headers
    assert headers['x-ya-service-ticket'] == f'serv:{bb_inst.blackbox_client.value}'


@pytest.mark.asyncio
async def test_internal_blackbox_error(respx_mock):
    blackbox_response = Response(200, json={
        'error': 'ERROR',
    })
    blackbox_route = (
        respx_mock.get('http://blackbox.yandex-team.ru/blackbox')
        .mock(return_value=blackbox_response)
    )
    settings = AppTypeSettings(
        app_type=AppTypes.forms_int,
        blackbox_name='ProdYateam',
    )
    bb_inst = InternalBlackbox(settings)
    uid = await bb_inst.sessionid('1:234', '127.0.0.1')
    assert uid is None

    assert blackbox_route.called
    assert blackbox_route.call_count == 1


@pytest.mark.asyncio
async def test_internal_blackbox_invalid(respx_mock):
    blackbox_response = Response(200, json={
        'error': 'OK',
        'status': {'value': 'INVALID'},
    })
    blackbox_route = (
        respx_mock.get('http://blackbox.yandex-team.ru/blackbox')
        .mock(return_value=blackbox_response)
    )
    settings = AppTypeSettings(
        app_type=AppTypes.forms_int,
        blackbox_name='ProdYateam',
    )
    bb_inst = InternalBlackbox(settings)
    uid = await bb_inst.sessionid('1:234', '127.0.0.1')
    assert uid is None

    assert blackbox_route.called
    assert blackbox_route.call_count == 1
