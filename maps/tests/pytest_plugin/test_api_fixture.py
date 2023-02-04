from unittest.mock import Mock

import pytest
from aiohttp import web
from aiohttp.client import ClientResponse

from maps_adv.common.lasagna import Lasagna

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def resource(request):
    mock = Mock()

    async def _res(req):
        mock(req, await req.read())

        return (
            web.json_response({"lol": "kek"})
            if request.node.get_closest_marker("json_response")
            else web.Response(body=b"kek")
        )

    return mock, _res


@pytest.fixture
def app(resource):
    _, resource = resource

    class ExampleApp(Lasagna):
        FILESYSTEM_PATH = "/tmp/"

        async def _setup_layers(self, db):
            api = web.Application()
            api.add_routes(
                [
                    getattr(web, method)("/resource/", resource)
                    for method in ("get", "post", "put", "patch", "delete")
                ]
            )
            return api

    return ExampleApp({})


@pytest.fixture
def db():
    return Mock()


@pytest.mark.parametrize("method", ("get", "post", "put", "patch", "delete"))
async def test_calls_with_expected_method(method, api, resource):
    mock, _ = resource

    await getattr(api, method)("/resource/")

    req = mock.call_args[0][0]
    assert req.method == method.upper()


async def test_encodes_proto(api, resource):
    mock, _ = resource
    proto = Mock()
    proto.SerializeToString.return_value = b"azaza"

    await api.post("/resource/", proto=proto)

    assert mock.call_args[0][1] == b"azaza"


async def test_returns_proto_if_specified(api):
    res = Mock()
    proto = Mock()
    proto.FromString.return_value = res

    got = await api.get("/resource/", decode_as=proto)

    assert got is res
    proto.FromString.assert_called_with(b"kek")


@pytest.mark.json_response
async def test_returns_json_by_default_if_server_returns_json(api):
    got = await api.put("/resource/")

    assert got == {"lol": "kek"}


async def test_returns_bytes_by_default(api):
    got = await api.put("/resource/")

    assert got == b"kek"


async def test_returns_respornse_if_specified(api):
    got = await api.delete("/resource/", as_response=True)

    assert isinstance(got, ClientResponse)


async def test_asserts_for_status_if_specified(api):
    with pytest.raises(AssertionError, match="200 == 201"):
        await api.patch("/resource/", expected_status=201)
