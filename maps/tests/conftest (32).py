import pytest

from maps_adv.geosmb.clients.geosearch import GeoSearchClient

pytest_plugins = ["aiohttp.pytest_plugin", "smb.common.aiotvm.pytest.plugin"]


@pytest.fixture
def mock_resolve_org(aresponses):
    def _mocker(resp_pb):
        aresponses.add(
            "geosearch.test",
            "/",
            "GET",
            aresponses.Response(status=200, body=resp_pb.SerializeToString()),
        )

    return _mocker


@pytest.fixture
def mock_resolve_orgs(aresponses):
    def _mocker(resp_pb):
        aresponses.add(
            "geosearch.test",
            "/",
            "GET",
            aresponses.Response(status=200, body=resp_pb.SerializeToString()),
        )

    return _mocker


@pytest.fixture
async def client(aiotvm):
    async with GeoSearchClient(
        url="https://geosearch.test", tvm=aiotvm, tvm_destination="geosearch"
    ) as _client:
        yield _client
