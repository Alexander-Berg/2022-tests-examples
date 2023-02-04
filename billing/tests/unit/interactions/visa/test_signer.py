import base64
import json

import pytest
from aiohttp import ClientRequest
from aiohttp.payload import JsonPayload
from yarl import URL

from hamcrest import assert_that, equal_to, has_entries

from billing.yandex_pay.yandex_pay.interactions.duckgo import DuckGoClient, VisaSignResult
from billing.yandex_pay.yandex_pay.interactions.visa import DuckGoVisaSigner, signed_request


@pytest.fixture
def url():
    return "http://yandex.ru"


@pytest.fixture
def headers():
    return {'X-Some-Header': 'some-val'}


@pytest.fixture
def mocked_sign_response(url):
    return VisaSignResult({
        'signing-header1': ['iqqdq'],
        'signing-header2': ['panzer'],
    }, f"{url}/?key=empty&value=not_empty")


@pytest.fixture(autouse=True)
async def duckgo_client(create_client):
    client = create_client(DuckGoClient)
    yield client
    await client.close()


@pytest.fixture(autouse=True)
def mock_duckgo_visa_sign(mocker, mocked_sign_response):
    return mocker.patch.object(
        DuckGoClient,
        'sign_visa_request',
        return_value=mocked_sign_response,
    )


@pytest.fixture
def signer(duckgo_client):
    return DuckGoVisaSigner(duckgo_client)


@pytest.fixture
def signed_request_cls(signer):
    return signed_request(signer)


@pytest.mark.asyncio
async def test_should_sign_client_request(
    signed_request_cls,
    url,
    headers,
    mocked_sign_response,
    mock_duckgo_visa_sign,
    mocker
):
    expected_headers = {k: v for k, v in headers.items()}
    expected_headers.update({k: v[0] for k, v in mocked_sign_response.headers.items()})

    req = signed_request_cls("GET",
                             URL(url),
                             headers=headers,
                             data=b'')

    async def mock_send(inst, conn):
        assert_that(str(inst.url), equal_to(str(mocked_sign_response.url)))
        assert_that(inst.headers, has_entries(expected_headers))
        return None

    mocker.patch.object(ClientRequest, 'send', mock_send)
    await req.send(None)

    mock_duckgo_visa_sign.assert_called_once_with(
        method='GET',
        url=str(url),
        body_b64=''
    )


@pytest.mark.asyncio
async def test_should_sign_client_request_with_zora(
    signed_request_cls,
    url,
    headers,
    mocked_sign_response,
    mock_duckgo_visa_sign,
    mocker,
):
    actual_url = 'http://some.url.com?a=b'

    headers['X-Ya-Dest-Url'] = actual_url

    expected_headers = {k: v for k, v in headers.items()}
    expected_headers.update({k: v[0] for k, v in mocked_sign_response.headers.items()})
    # must be overwritten
    expected_headers['X-Ya-Dest-Url'] = mocked_sign_response.url

    req = signed_request_cls('GET',
                             URL(url),
                             headers=headers,
                             data=b'')

    async def mock_send(inst, conn):
        # not mocked_sign_response.url
        assert_that(str(inst.url), equal_to(str(url)))
        assert_that(inst.headers, has_entries(expected_headers))
        return None

    mocker.patch.object(ClientRequest, 'send', mock_send)
    await req.send(None)

    mock_duckgo_visa_sign.assert_called_once_with(
        method='GET',
        url=actual_url,
        body_b64=''
    )


@pytest.mark.asyncio
async def test_should_sign_client_request_with_json_body(
    signed_request_cls,
    url,
    headers,
    mocked_sign_response,
    mock_duckgo_visa_sign,
    mocker,
):
    expected_headers = {k: v for k, v in headers.items()}
    expected_headers.update({k: v[0] for k, v in mocked_sign_response.headers.items()})
    json_data = {
        'key': 'value',
        'number': 1.
    }
    payload = JsonPayload(json_data, dumps=json.dumps)
    expected_body = json.dumps(json_data).encode('utf-8')

    req = signed_request_cls("GET",
                             URL(url),
                             headers=headers,
                             data=payload)

    async def mock_send(inst, conn):
        assert_that(str(inst.url), equal_to(str(mocked_sign_response.url)))
        assert_that(inst.headers, has_entries(expected_headers))
        assert_that(inst.body._value, equal_to(expected_body))
        return None

    mocker.patch.object(ClientRequest, 'send', mock_send)
    await req.send(None)

    mock_duckgo_visa_sign.assert_called_once_with(
        method='GET',
        url=str(url),
        body_b64=base64.b64encode(expected_body).decode('utf-8'),
    )
