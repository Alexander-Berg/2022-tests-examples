import aiohttp
import aiohttp.client
import aiohttp.test_utils
import pytest

from maps_adv.common.helpers import coro_mock

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
async def ping_v1_mock(mocker, loop, aiohttp_client):
    ping = coro_mock()
    ping.coro.return_value = aiohttp.web.Response(status=200)

    api = aiohttp.web.Application()
    api.add_routes([aiohttp.web.get(r"/ping", ping)])

    server = aiohttp.test_utils.TestServer(api, loop=loop, port=33333)
    client = aiohttp.test_utils.TestClient(server, loop=loop)

    await client.start_server()
    yield ping
    await client.close()


@pytest.mark.usefixtures("ping_v1_mock")
async def test_returns_200(api):
    await api.get("/ping", expected_status=200)


@pytest.mark.real_db
@pytest.mark.usefixtures("ping_v1_mock")
async def test_returns_500_if_pool_is_disconnected(db, api):
    await db.close()

    await api.get("/ping", expected_status=500)


async def test_returns_500_if_no_valid_response_from_prev_version_of_backend(db, api):
    await api.get("/ping", expected_status=500)


@pytest.mark.parametrize("status_code", [200, 204])
async def test_returns_200_if_prev_version_of_backend_response_correct_status_code(
    status_code, db, api, ping_v1_mock
):
    ping_v1_mock.coro.return_value = aiohttp.web.Response(status=status_code)

    await api.get("/ping", expected_status=200)
