import pytest
from aiohttp import web
from aiohttp.test_utils import TestClient

from maps.infra.pycare.example import lib  # noqa - import endpoints
from maps.infra.pycare.test_utils import MockUserInfo, MockUserTicket, MockAuthClient


# Override fixture to initialize application
@pytest.fixture(scope='function')
async def test_application(test_application: web.Application) -> web.Application:
    test_application['requests'] = 0  # initialize application for /count endpoint
    return test_application


@pytest.mark.asyncio
async def test_ping(test_client: TestClient) -> None:
    response = await test_client.get('/ping')
    assert response.status == 200


@pytest.mark.asyncio
async def test_hello(test_client: TestClient) -> None:
    response = await test_client.get('/hello')
    assert response.status == 200


@pytest.mark.asyncio
async def test_user(test_client: TestClient, mock_user_info: MockUserInfo) -> None:
    with mock_user_info(uid=12345, login='mr-reese'):
        response = await test_client.get('/user')
    assert response.status == 200
    assert await response.read() == b'Hello, @mr-reese!'


@pytest.mark.asyncio
async def test_count(test_client: TestClient) -> None:
    for i in range(1, 11):
        response = await test_client.get('/count')
        assert response.status == 200
        assert await response.text() == f'Requests count: {i}'


@pytest.mark.asyncio
async def test_user_ticket(test_client: TestClient, mock_user_ticket: MockUserTicket) -> None:
    with mock_user_ticket('fake-ticket'):
        response = await test_client.get('/user_ticket')
    assert response.status == 200
    assert await response.read() == b'User ticket: fake-ticket'


@pytest.mark.asyncio
async def test_auth_client(test_client: TestClient, mock_auth_client: MockAuthClient) -> None:
    with mock_auth_client(user_ticket='fake-ticket'):
        response = await test_client.get('/auth_client')
    assert response.status == 200
    assert await response.read() == b'User ticket: fake-ticket'
