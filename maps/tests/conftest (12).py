import aiohttp
import pytest

from tenacity import RetryError

from maps_adv.common.client import Client as BaseClient

pytest_plugins = ["aiohttp.pytest_plugin"]


class TestClient(BaseClient):
    async def request(self):
        try:
            response_body = await self._retryer.call(
                self._request,
                "GET",
                "/path",
                expected_status=200,
            )
        except RetryError as exc:
            exc.reraise()

        return response_body

    @staticmethod
    async def _check_response(response: aiohttp.ClientResponse):
        pass


@pytest.fixture
def rmock(aresponses):
    return lambda *a: aresponses.add("example.com", *a)


@pytest.fixture
def mock_request(rmock):
    return lambda h: rmock("/path", "GET", h)


@pytest.fixture
def client():
    return TestClient("http://example.com")
