import base64
import json
import logging
import re
from typing import Any
from urllib.parse import quote_plus, urlparse

import pytest
from aioresponses import aioresponses

from sendr_interactions.exceptions import InteractionResponseError

from hamcrest import assert_that, equal_to, has_entries, has_entry, has_item, has_properties, instance_of, not_

from billing.yandex_pay.yandex_pay.interactions.mastercard import MasterCardClient
from billing.yandex_pay.yandex_pay.interactions.mastercard.exceptions import (
    ImageTooLargeError, TokenAlreadyDeletedError, UntrustedUrlError
)

FAKE_OAUTH_TOKEN = 'Fake oauth token'


class DummyMasterCardClient(MasterCardClient):
    url = 'dummy_url'

    async def dummy_delete(self, **kwargs: Any) -> None:
        await self.delete('dummy_delete', self.url, duckgo_sign=True, **kwargs)


@pytest.fixture
async def mastercard_client(create_client):
    client = create_client(MasterCardClient)
    yield client
    await client.close()


@pytest.fixture
async def dummy_mastercard_client(create_client):
    client = create_client(DummyMasterCardClient)
    yield client
    await client.close()


class TestMasterCardClientGetImage:
    MASTERCARD_URL = 'https://sbx.assets.mastercard.com/card-art/image.png'
    FAKE_IMAGE_DATA = b'fake_image_data'

    @pytest.fixture(autouse=True)
    def aioresponses_mocker(self, yandex_pay_settings):
        with aioresponses() as m:
            m.get(yandex_pay_settings.ZORA_URL, body=self.FAKE_IMAGE_DATA)
            yield m

    @pytest.mark.asyncio
    async def test_get_image(self, mastercard_client):
        image_data = await mastercard_client.get_image(self.MASTERCARD_URL)
        assert_that(image_data, equal_to(self.FAKE_IMAGE_DATA))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('scheme', ['', 'http', 'smpt', 'garbage'])
    async def test_untrusted_url_scheme(self, scheme, mastercard_client):
        url = urlparse(self.MASTERCARD_URL)._replace(scheme=scheme).geturl()
        pattern = f'URL scheme "{scheme}" is not trusted for image downloads.'
        with pytest.raises(UntrustedUrlError, match=re.escape(pattern)):
            await mastercard_client.get_image(url)

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'url,hostname',
        [
            ('https://localhost/image.png', 'localhost'),
            ('https://fake-mastercard.com', 'fake-mastercard.com'),
            ('https://fake-mastercard.com/image.png', 'fake-mastercard.com'),
            ('https://assets.mastercard.ru/image.png', 'assets.mastercard.ru'),
        ]
    )
    async def test_untrusted_url_hostname(self, url, hostname, mastercard_client):
        pattern = f'Hostname "{hostname}" is not trusted for image downloads.'
        with pytest.raises(UntrustedUrlError, match=re.escape(pattern)):
            await mastercard_client.get_image(url)

    @pytest.mark.asyncio
    async def test_image_too_large_exception(self, mocker, mastercard_client):
        mocker.patch.object(MasterCardClient, 'IMAGE_MAX_SIZE', 0)
        with pytest.raises(ImageTooLargeError):
            await mastercard_client.get_image(self.MASTERCARD_URL)


@pytest.fixture
def duckgo_sign_response_body():
    return {
        'status': 'success',
        'code': 200,
        'data': {'headers': {'Authorization': [FAKE_OAUTH_TOKEN]}}
    }


@pytest.fixture(autouse=True)
def mock_duckgo_sign_response(
    yandex_pay_settings, aioresponses_mocker, duckgo_sign_response_body
):
    return aioresponses_mocker.post(
        re.compile(f'^{yandex_pay_settings.DUCKGO_API_URL}.*'),
        status=200,
        payload=duckgo_sign_response_body,
    )


@pytest.fixture
def mock_mastercard_delete_response(yandex_pay_settings, aioresponses_mocker):
    return aioresponses_mocker.delete(
        re.compile(f'^{yandex_pay_settings.ZORA_URL}.*'),
        status=200,
        payload={'srcCorrelationId': 'fake_correlation_id'},
    )


class TestMasterCardDeleteCard:
    FAKE_TOKEN_ID = 'fake_token_id'

    @pytest.mark.asyncio
    async def test_delete_card(
        self,
        mock_mastercard_delete_response,
        mock_duckgo_sign_response,
        mastercard_client,
        yandex_pay_settings,
    ):
        response = await mastercard_client.delete_card(self.FAKE_TOKEN_ID)

        mock_mastercard_delete_response.assert_called_once()
        _, call_kwargs = mock_mastercard_delete_response.call_args
        url = (
            f'{yandex_pay_settings.MASTERCARD_API_URL}/src/api/digital/payments/cards/'
            f'{self.FAKE_TOKEN_ID}?srcClientId={yandex_pay_settings.MASTERCARD_SRC_CLIENT_ID}'
            f'&serviceId={quote_plus(yandex_pay_settings.MASTERCARD_SERVICE_ID)}'
        )
        assert_that(
            call_kwargs,
            has_entry(
                'headers',
                has_entries(
                    {
                        'Authorization': FAKE_OAUTH_TOKEN,
                        'X-Ya-Dest-Url': url,
                    }
                )
            )
        )

        mock_duckgo_sign_response.assert_called_once()
        _, call_kwargs = mock_duckgo_sign_response.call_args
        assert_that(
            call_kwargs,
            has_entry('json', {'url': url, 'method': 'DELETE', 'body': ''}),
        )

        assert_that(
            await response.json(),
            equal_to({'srcCorrelationId': 'fake_correlation_id'})
        )

    @pytest.mark.asyncio
    async def test_exception_if_card_already_removed_by_tsp(
        self, yandex_pay_settings, aioresponses_mocker, mastercard_client
    ):
        aioresponses_mocker.delete(
            re.compile(f'^{yandex_pay_settings.ZORA_URL}.*'),
            status=404,
            payload={'status': 404, 'reason': 'NOT_FOUND', 'message': 'Card not found'},
        )

        pattern = 'Token has already been deleted by TSP'
        with pytest.raises(TokenAlreadyDeletedError, match=pattern):
            await mastercard_client.delete_card(self.FAKE_TOKEN_ID)

    @pytest.mark.asyncio
    @pytest.mark.parametrize('body', ['', 'test', '{}', '{"test": true}', b'', b'\x00'], ids=str)
    async def test_generic_404_exception_is_not_affected(
        self, yandex_pay_settings, aioresponses_mocker, mastercard_client, body
    ):
        aioresponses_mocker.delete(
            re.compile(f'^{yandex_pay_settings.ZORA_URL}.*'),
            status=404,
            body=body,
        )
        with pytest.raises(InteractionResponseError) as excinfo:
            await mastercard_client.delete_card(self.FAKE_TOKEN_ID)

        assert_that(excinfo.value, not_(instance_of(TokenAlreadyDeletedError)))


@pytest.mark.asyncio
async def test_sign_request_with_json_payload(
    dummy_mastercard_client, mock_duckgo_sign_response, mock_mastercard_delete_response
):
    payload = {'fake': True}
    payload_b64 = base64.b64encode(json.dumps(payload).encode('utf-8')).decode('utf-8')
    await dummy_mastercard_client.dummy_delete(json=payload)

    mock_duckgo_sign_response.assert_called_once()
    _, call_kwargs = mock_duckgo_sign_response.call_args
    assert_that(
        call_kwargs,
        has_entry(
            'json',
            {'url': DummyMasterCardClient.url, 'method': 'DELETE', 'body': payload_b64}
        ),
    )

    mock_mastercard_delete_response.assert_called_once()
    _, call_kwargs = mock_mastercard_delete_response.call_args
    assert_that(
        call_kwargs,
        has_entries(
            headers=has_entry('Authorization', FAKE_OAUTH_TOKEN),
            data=json.dumps(payload).encode('utf-8'),
        )
    )


@pytest.mark.asyncio
async def test_sign_request_with_binary_payload(
    dummy_mastercard_client, mock_duckgo_sign_response, mock_mastercard_delete_response
):
    payload = b'fake_payload'
    payload_b64 = base64.b64encode(payload).decode('utf-8')
    await dummy_mastercard_client.dummy_delete(data=payload)

    mock_duckgo_sign_response.assert_called_once()
    _, call_kwargs = mock_duckgo_sign_response.call_args
    assert_that(
        call_kwargs,
        has_entry(
            'json',
            {'url': DummyMasterCardClient.url, 'method': 'DELETE', 'body': payload_b64}
        ),
    )

    mock_mastercard_delete_response.assert_called_once()
    _, call_kwargs = mock_mastercard_delete_response.call_args
    assert_that(
        call_kwargs,
        has_entries(
            headers=has_entry('Authorization', FAKE_OAUTH_TOKEN),
            data=payload,
        )
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('mock_mastercard_delete_response')
async def test_both_json_and_data_not_allowed(dummy_mastercard_client):
    pattern = 'data and json parameters can not be used at the same time'
    with pytest.raises(AssertionError, match=pattern):
        await dummy_mastercard_client.dummy_delete(data=b'', json={})


@pytest.fixture
def mastercard_response_headers():
    return {
        'X-Correlation-ID': 'fake_correlation_id_mc',
        'X-MC-Correlation-ID': 'fake_mc_correlation_id',
        'X-Src-Cx-Flow-Id': 'fake_flow_id',
        'X-Vcap-Request-Id': 'fake_vcap_request_id',
    }


@pytest.mark.asyncio
async def test_mastercard_logging_correlation_headers(
    aioresponses_mocker,
    mastercard_client,
    yandex_pay_settings,
    mastercard_response_headers,
    caplog,
):

    caplog.set_level(logging.INFO, logger="dummy_logger")
    mastercard_client.DEBUG = False  # DEBUG=True only in Development env

    aioresponses_mocker.post(
        re.compile(f'^{yandex_pay_settings.DUCKGO_API_URL}.*'),
        status=200,
        payload=duckgo_sign_response_body,
    )
    aioresponses_mocker.delete(
        re.compile(f'^{yandex_pay_settings.ZORA_URL}.*'),
        status=200,
        headers=mastercard_response_headers,
        payload={'srcCorrelationId': 'fake_correlation_id'},
    )

    await mastercard_client.delete_card("123")

    assert_that(
        caplog.records,
        has_item(
            has_properties(
                name='dummy_logger',
                message='Mastercard token deleted',
                _context=has_entries(
                    response_headers=has_entries(mastercard_response_headers),
                    src_correlation_id='fake_correlation_id',
                ),
            ),
        ),
    )
