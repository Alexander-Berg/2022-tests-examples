import asyncio
import datetime
from typing import Optional
from xmlrpc.client import Fault, ProtocolError, dumps, loads
from xmlrpc.server import SimpleXMLRPCDispatcher

import aiohttp
import pytest

from maps_adv.billing_proxy.lib.core.async_xmlrpc_client import XmlRpcClient

pytestmark = [pytest.mark.asyncio]


class HttpResponseMock:
    def __init__(
        self,
        status: int,
        body: bytes,
        headers: Optional[dict] = None,
        url: Optional[str] = None,
    ):
        self.status = status
        self._body = body
        self._headers = headers or {}
        self.url = url

    async def read(self) -> bytes:
        return self._body

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        pass

    @property
    def headers(self):
        return self._headers.copy()


@pytest.fixture
def xmlrpc_server():
    server = SimpleXMLRPCDispatcher()
    server.register_function(lambda *args: sum(args), "Sum")
    server.register_function(lambda *args: max(args), "Long.Named.Max")
    server.register_function(lambda value, times: [value] * times, "Repeat")

    def raises_fault(*_):
        raise Fault(12, "I raised!")

    server.register_function(raises_fault, "ReturnsFault")  # noqa

    return server


@pytest.fixture
def mock_aiohttp_client(mocker, xmlrpc_server):
    def post_method_mock(self, url, data, headers):
        input_args, methodname = loads(data)
        result = xmlrpc_server._dispatch(methodname, input_args)
        return HttpResponseMock(200, dumps((result,), methodname=methodname), url=url)

    mocker.patch("aiohttp.ClientSession.post", post_method_mock)


async def test_makes_request(mocker):
    mocker.patch(
        "aiohttp.ClientSession.post",
        return_value=HttpResponseMock(200, dumps((1,), methodname="Method")),
    )

    client = XmlRpcClient("localhost")
    await client.Method(2, 3)

    assert aiohttp.ClientSession.post.called

    call_args = aiohttp.ClientSession.post.call_args
    expected_data_arg = dumps((2, 3), methodname="Method").encode()
    assert call_args[1]["data"] == expected_data_arg


async def test_default_parameters(mocker):
    mocker.patch(
        "aiohttp.ClientSession.post",
        return_value=HttpResponseMock(200, dumps((1,), methodname="Method")),
    )

    client = XmlRpcClient("localhost")
    await client.Method(2, 3)

    assert aiohttp.ClientSession.post.called

    call_args = aiohttp.ClientSession.post.call_args
    expected_positional_arg = "http://localhost:80/"
    expected_headers_arg = {"Content-Type": "text/xml"}
    assert call_args[0] == (expected_positional_arg,)
    assert call_args[1]["headers"] == expected_headers_arg


@pytest.mark.parametrize(
    (
        "host",
        "port",
        "path",
        "use_https",
        "additional_headers",
        "expected_uri_arg",
        "expected_headers_arg",
    ),
    [
        (
            "localhost",
            80,
            "/",
            False,
            None,
            "http://localhost:80/",
            {"Content-Type": "text/xml"},
        ),
        (
            "localhost",
            80,
            "",
            False,
            None,
            "http://localhost:80",
            {"Content-Type": "text/xml"},
        ),
        (
            "localhost",
            2223,
            "/",
            False,
            None,
            "http://localhost:2223/",
            {"Content-Type": "text/xml"},
        ),
        (
            "localhost",
            80,
            "/",
            True,
            None,
            "https://localhost:80/",
            {"Content-Type": "text/xml"},
        ),
        (
            "localhost",
            333,
            "/xmlrpc/path",
            False,
            None,
            "http://localhost:333/xmlrpc/path",
            {"Content-Type": "text/xml"},
        ),
        (
            "localhost",
            80,
            "no/starting/slash",
            False,
            None,
            "http://localhost:80/no/starting/slash",
            {"Content-Type": "text/xml"},
        ),
        (
            "localhost",
            80,
            "/",
            False,
            {"X-Http-Custom-Header": "lol"},
            "http://localhost:80/",
            {"Content-Type": "text/xml", "X-Http-Custom-Header": "lol"},
        ),
    ],
)
async def test_respects_parameters(
    mocker,
    host,
    port,
    path,
    use_https,
    additional_headers,
    expected_uri_arg,
    expected_headers_arg,
):
    mocker.patch(
        "aiohttp.ClientSession.post",
        return_value=HttpResponseMock(200, dumps((1,), methodname="Method")),
    )

    client = XmlRpcClient(
        host, port, path, use_https=use_https, additional_headers=additional_headers
    )
    await client.Method(2, 3)

    assert aiohttp.ClientSession.post.called

    call_args = aiohttp.ClientSession.post.call_args
    assert call_args[0] == (expected_uri_arg,)
    assert call_args[1]["headers"] == expected_headers_arg


@pytest.mark.usefixtures("mock_aiohttp_client")
@pytest.mark.parametrize(
    ("methodname", "method_params", "expected_results"),
    [
        ("Sum", (1, 2, 3), (6,)),
        ("Long.Named.Max", (1, 2, 3), (3,)),
        ("Repeat", (3, 4), ([3, 3, 3, 3],)),
    ],
)
async def test_parses_data(methodname, method_params, expected_results):
    client = XmlRpcClient("localhost")
    result = await getattr(client, methodname)(*method_params)

    assert result == expected_results


@pytest.mark.parametrize(
    ("http_code", "http_body"),
    [
        (500, b"Internal Server Error"),
        (502, b"Bad Gateway"),
        (503, b"Service Unavailable"),
        (504, b"Gateway Timeout"),
    ],
)
async def test_raises_on_non_200_status_code(mocker, http_code, http_body):
    mocker.patch(
        "aiohttp.ClientSession.post",
        return_value=HttpResponseMock(
            http_code, http_body, {"X-Http-Fail": "I failed"}, "localhost:80/path"
        ),
    )

    client = XmlRpcClient("localhost")
    with pytest.raises(ProtocolError) as http_exc:
        await client.Method(2, 3)

    http_exc.value.url = "localhost:80/path"
    http_exc.value.errcode = http_code
    http_exc.value.errmsg = http_body
    http_exc.value.headers = {"X-Http-Fail": "I failed"}


@pytest.mark.usefixtures("mock_aiohttp_client")
async def test_raises_on_fault_response():
    client = XmlRpcClient("localhost")
    with pytest.raises(Fault):
        await client.ReturnsFault(2, 3, 4)


@pytest.mark.parametrize(
    ("session", "expected_close_called"),
    [(None, True), (aiohttp.ClientSession(), False)],
)
async def test_session_closed_on_close_if_owned(mocker, session, expected_close_called):
    future = asyncio.Future()
    future.set_result(None)
    mocker.patch("aiohttp.ClientSession.close", return_value=future)

    client = XmlRpcClient("localhost", session=session)
    await client.close()

    assert aiohttp.ClientSession.close.called == expected_close_called


async def test_forwards_exception_on_aiohttp_timeout(mocker):
    mocker.patch("aiohttp.ClientSession.post", side_effect=asyncio.TimeoutError())

    client = XmlRpcClient("localhost")
    with pytest.raises(asyncio.TimeoutError):
        await client.Sum()


@pytest.mark.parametrize("value", [datetime.datetime(2000, 1, 1, 2, 3, 4), b"qwerty"])
async def test_uses_native_datatypes(mocker, value):
    mocker.patch(
        "aiohttp.ClientSession.post",
        return_value=HttpResponseMock(200, dumps((value,), methodname="Method")),
    )

    client = XmlRpcClient("localhost")
    result = await client.Sum()

    assert type(result[0]) is type(value)
