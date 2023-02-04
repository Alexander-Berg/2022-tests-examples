import json
import typing as tp
from dataclasses import dataclass, asdict

import aiohttp
import pytest
from aiohttp.test_utils import make_mocked_request

from maps.infra.pycare.lib.auth import (
    add_auth_client_cleanup_hook,
    auth_client,
    get_client_ip_address,
    AuthClient,
    UserInfo,
    UserTicket,
)
from maps.infra.pycare.lib.handler import UserInfoParam, UserTicketParam


class MockResponse:
    def __init__(self,
                 status: int,
                 headers: tp.Optional[dict[str, str]] = None,
                 content: str = '') -> None:
        self.status = status
        self.headers = headers
        self.content = content

    async def json(self) -> tp.Any:
        return json.loads(self.content)

    @property
    def reason(self) -> str:
        return self.content

    async def __aenter__(self) -> 'MockResponse':
        return self

    async def __aexit__(self, *args, **kwargs) -> None:
        pass


@dataclass
class ClientIpTestCase:
    headers: dict[str, str]
    expected_addr: str


@pytest.mark.parametrize('case', [
    ClientIpTestCase(
        headers={'X-Remote-Addr': '192.168.0.7'},
        expected_addr='192.168.0.7',
    ),
    ClientIpTestCase(
        headers={
            'X-Remote-Addr': '192.168.0.7',
            'X-Forwarded-For': '192.168.0.8, 192.168.0.9',
        },
        expected_addr='192.168.0.8',
    ),
    ClientIpTestCase(
        headers={
            'X-Remote-Addr': '192.168.0.7',
            'X-Forwarded-For': '192.168.0.8, 192.168.0.9',
            'X-Real-IP': '192.168.0.10',
        },
        expected_addr='192.168.0.10',
    ),
    ClientIpTestCase(
        headers={
            'X-Remote-Addr': '192.168.0.7',
            'X-Forwarded-For': '192.168.0.8, 192.168.0.9',
            'X-Real-IP': '192.168.0.10',
            'X-Forwarded-For-Y': '192.168.0.11, 192.168.0.12',
        },
        expected_addr='192.168.0.11',
    )
])
def test_client_ip(case: ClientIpTestCase) -> None:
    request = make_mocked_request('GET', '/', headers=case.headers)
    assert get_client_ip_address(request) == case.expected_addr


@dataclass
class UserInfoTestCase:
    status_code: int
    user_info: tp.Optional[UserInfo]
    reason: tp.Optional[str]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', [
    UserInfoTestCase(
        status_code=200,
        user_info=UserInfo(uid=42, login='john-doe'),
        reason=None,
    ),
    UserInfoTestCase(
        status_code=401,
        user_info=None,
        reason='No authorization info present',
    ),
    UserInfoTestCase(
        status_code=500,
        user_info=None,
        reason='Internal error',
    ),
])
async def test_fetch_user_info(monkeypatch, case: UserInfoTestCase) -> None:
    def patched_get_method(self: aiohttp.ClientSession, url: str, headers: dict[str, str]) -> MockResponse:
        assert url == 'http://[::1]/user_info'
        assert headers == {
            'Host': 'auth-agent.maps.yandex.ru',
            'X-Real-IP': '127.0.0.1',
            'Authorization': 'OAuth AQAD-fake',
        }
        return MockResponse(
            status=case.status_code,
            content=json.dumps(asdict(case.user_info)) if case.user_info else case.reason,
        )

    monkeypatch.setattr(aiohttp.ClientSession, 'get', patched_get_method)

    fetch_coro = AuthClient().try_fetch_user_info(
        client_ip='127.0.0.1',
        oauth='AQAD-fake',
    )
    if case.status_code == 200:
        assert await fetch_coro == case.user_info
    else:
        with pytest.raises(Exception, match=case.reason):
            await fetch_coro


@dataclass
class UserTicketTestCase:
    status_code: int
    user_ticket: tp.Optional[str]
    reason: tp.Optional[str]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', [
    UserTicketTestCase(
        status_code=200,
        user_ticket='fake-ticket',
        reason=None,
    ),
    UserTicketTestCase(
        status_code=401,
        user_ticket=None,
        reason='No authorization info present',
    ),
    UserTicketTestCase(
        status_code=500,
        user_ticket=None,
        reason='Internal error',
    ),
])
async def test_fetch_user_ticket(monkeypatch, case: UserTicketTestCase) -> None:
    def patched_get_method(self: aiohttp.ClientSession, url: str, headers: dict[str, str]) -> MockResponse:
        assert url == 'http://[::1]/user_ticket'
        assert headers == {
            'Host': 'auth-agent.maps.yandex.ru',
            'X-Real-IP': '127.0.0.1',
            'Authorization': 'OAuth AQAD-fake',
        }
        return MockResponse(
            status=case.status_code,
            headers={'X-Ya-User-Ticket': case.user_ticket} if case.user_ticket else None,
            content=case.reason if case.reason else '',
        )

    monkeypatch.setattr(aiohttp.ClientSession, 'get', patched_get_method)

    fetch_coro = AuthClient().try_fetch_user_ticket(
        client_ip='127.0.0.1',
        oauth='AQAD-fake',
    )
    if case.status_code == 200:
        assert await fetch_coro == case.user_ticket
    else:
        with pytest.raises(Exception, match=case.reason):
            await fetch_coro


@dataclass
class UserInfoParamTestCase:
    auth_header: tp.Optional[str]
    expected_oauth: tp.Optional[str]


@pytest.mark.asyncio
@pytest.mark.parametrize('auth_required', [True, False])
@pytest.mark.parametrize('case', [
    UserInfoParamTestCase(
        auth_header='OAuth AQAD-fake',
        expected_oauth='AQAD-fake',
    ),
    UserInfoParamTestCase(
        auth_header='Bad header',
        expected_oauth=None,
    ),
    UserInfoParamTestCase(
        auth_header=None,
        expected_oauth=None,
    ),
])
async def test_user_info_param_parsing(case: UserInfoParamTestCase, auth_required: bool) -> None:
    class MockAuthClient:
        async def try_fetch_user_info(self, client_ip: str, oauth: str) -> UserInfo:
            assert client_ip == '127.0.0.1'
            assert oauth == case.expected_oauth
            return UserInfo(uid=42, login='john-doe')

    headers = {'X-Real-IP': '127.0.0.1'}
    if case.auth_header:
        headers['Authorization'] = case.auth_header
    request = make_mocked_request(
        'GET', '/',
        headers=headers,
        app={AuthClient.__name__: MockAuthClient()},
    )

    test_coro = UserInfoParam(
        name='user_info',
        required=auth_required,
    ).parse(request)

    if auth_required and not case.expected_oauth:
        with pytest.raises(Exception, match='No authorization info present'):
            await test_coro
    else:
        user_info = await test_coro
        if case.expected_oauth:
            assert user_info is not None
        else:
            assert user_info is None


@dataclass
class UserTicketParamTestCase:
    auth_header: tp.Optional[str]
    expected_oauth: tp.Optional[str]


@pytest.mark.asyncio
@pytest.mark.parametrize('auth_required', [True, False])
@pytest.mark.parametrize('case', [
    UserTicketParamTestCase(
        auth_header='OAuth AQAD-fake',
        expected_oauth='AQAD-fake',
    ),
    UserTicketParamTestCase(
        auth_header='Bad header',
        expected_oauth=None,
    ),
    UserTicketParamTestCase(
        auth_header=None,
        expected_oauth=None,
    ),
])
async def test_user_ticket_param_parsing(case: UserTicketParamTestCase, auth_required: bool) -> None:
    class MockAuthClient:
        async def try_fetch_user_ticket(self, client_ip: str, oauth: str) -> UserTicket:
            assert client_ip == '127.0.0.1'
            assert oauth == case.expected_oauth
            return UserTicket('fake-ticket')

    headers = {'X-Real-IP': '127.0.0.1'}
    if case.auth_header:
        headers['Authorization'] = case.auth_header
    request = make_mocked_request(
        'GET', '/',
        headers=headers,
        app={AuthClient.__name__: MockAuthClient()},
    )

    test_coro = UserTicketParam(
        name='user_ticket',
        required=auth_required,
    ).parse(request)

    if auth_required and not case.expected_oauth:
        with pytest.raises(Exception, match='No authorization info present'):
            await test_coro
    else:
        user_ticket = await test_coro
        if case.expected_oauth:
            assert user_ticket is not None
        else:
            assert user_ticket is None


@pytest.mark.asyncio
async def test_auth_client_creation_destruction() -> None:
    class AppMock(dict):
        def __init__(self) -> None:
            self.on_cleanup = []

    app = AppMock()
    add_auth_client_cleanup_hook(app)

    client = auth_client(app)
    assert isinstance(client, AuthClient)
    assert client == app[AuthClient.__name__]

    cleanup_hook = app.on_cleanup[0]
    await cleanup_hook(app)
    assert client._session.closed
