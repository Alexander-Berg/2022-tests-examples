import base64
import hashlib

import pytest
import yarl
from pay.lib.entities.payment_token import MITInfo

from hamcrest import assert_that, equal_to, has_entries, has_properties

from billing.yandex_pay.yandex_pay.interactions import DuckGoClient
from billing.yandex_pay.yandex_pay.interactions.duckgo import (
    DuckGoInteractionError, InvalidPublicKeyError, create_mastercard_checkout_result, create_sign_result,
    create_visa_checkout_result, mastercard_sign_result_schema, visa_sign_result_schema
)


@pytest.fixture
async def duckgo_client(create_client):
    from billing.yandex_pay.yandex_pay.interactions import DuckGoClient
    client = create_client(DuckGoClient)
    yield client
    await client.close()


@pytest.fixture
def message_id():
    return '1:gAAAAABf_Z68apRys6H3llaGCwFF3lHTcEQLVwhz5yJ3sPSJUQaOgTdIOt'


@pytest.fixture
def message_expiration():
    return 123


@pytest.fixture
def mastercard_checkout_request_params(message_id, message_expiration):
    return {
        'card_id': '47aa9326-4b19-4eea-821b-ba989f2aadee',
        'recipient_id': 'yandex-trust',
        'recipient_pub_key': (
            'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAECLiIDfSrbiCTSBa3gKGiDEA4L9/'
            'c334/zxoXr8rJv1rOmkfFQoE7zDht8b2+Crv5AbcAiwQYnpfz9JKeXmIv/A=='
        ),
        'recipient_pub_key_signature': 'signature',
        'transaction_amount': '100',
        'transaction_currency': 'XTS',
        'mit_info': MITInfo(deferred=True),
        'gateway_merchant_id': 'afisha',
        'message_expiration': message_expiration,
        'message_id': message_id
    }


@pytest.fixture
def visa_checkout_request_params(message_id, message_expiration):
    return {
        'card_id': '47aa9326-4b19-4eea-821b-ba989f2aadee',
        'recipient_id': 'yandex-trust',
        'recipient_pub_key': (
            'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAECLiIDfSrbiCTSBa3gKGiDEA4L9/'
            'c334/zxoXr8rJv1rOmkfFQoE7zDht8b2+Crv5AbcAiwQYnpfz9JKeXmIv/A=='
        ),
        'recipient_pub_key_signature': 'signature',
        'transaction_amount': '100',
        'transaction_currency': 'RUB',
        'mit_info': MITInfo(deferred=True),
        'gateway_merchant_id': 'afisha',
        'message_expiration': message_expiration,
        'message_id': message_id,
        'relationship_id': 'relationship-id',
    }


@pytest.fixture
def visa_sign_request_params():
    return {
        'method': 'POST',
        'url': 'http://yandex.ru',
        'body': base64.b64encode(b'{"hello":"world"}').decode("utf-8")
    }


@pytest.fixture
def mastercard_sign_request_params():
    return {
        'method': 'DELETE',
        'url': 'http://yandex.ru',
        'body': base64.b64encode(b'{"hello":"world"}').decode("utf-8")
    }


@pytest.fixture
def visa_verify_request_params():
    return {
        'signature': 'my-signature',
        'url': 'http://yandex.ru?apiKey=111',
        'body': base64.b64encode(b'{"hello":"world"}').decode("utf-8")
    }


@pytest.fixture
def mastercard_checkout_response_body():
    return {
        'code': 200,
        'data': {
            'masked_card': {
                'dateOfCardCreated': '2020-12-05T23:53:59.579Z',
                'dateOfCardLastUsed': '2020-12-10T13:02:04.196Z',
                'digitalCardData': {
                    'artUri': (
                        'https://sbx.assets.mastercard.com/'
                        'card-art/combined-image-asset/c911d734-2f4b-4326-8363-afb0ffe2bc2b.png'
                    ),
                    'descriptorName': 'Example Bank Product Configuration',
                    'status': 'ACTIVE',
                },
                'maskedBillingAddress': None,
                'panBin': '520473',
                'panExpirationMonth': '01',
                'panExpirationYear': '2022',
                'panLastFour': '4784',
                'paymentAccountReference': '',
                'paymentCardDescriptor': '',
                'paymentCardType': 'CREDIT',
                'srcDigitalCardId': '198e7a1f-c253-46dc-92d9-520356ed6dee'
            },
            'masked_consumer': {
                'dateConsumerAdded': '2020-12-05T23:52:14.183Z',
                'maskedConsumerIdentity': {
                    'identityType': 'EXTERNAL_ACCOUNT_ID',
                    'maskedIdentityValue': '4053525715'
                },
                'maskedFullName': '',
                'maskedMobileNumber': {},
                'srcConsumerId': '11b91ca2-b98e-482f-891c-3c45167b9070',
                'status': 'ACTIVE'
            },
            'payment_token': 'eyJzaWduZWRNZXNzYWdlIjoie1wiZW5jcnlwdGVkTWVzc2FnZVwiOlwiV1RKcDJWYjJqNnRaOVJGbUFO',

        },
        'status': 'success',
    }


@pytest.fixture
def mastercard_checkout_response_headers():
    return {
        'X-Correlation-ID': 'fake_correlation_id_mc',
        'X-MC-Correlation-ID': 'fake_mc_correlation_id',
        'X-Src-Cx-Flow-Id': 'fake_flow_id',
        'X-Vcap-Request-Id': 'fake_vcap_request_id',
    }


@pytest.fixture
def visa_checkout_response_body():
    return {
        'code': 200,
        'data': {
            'payment_token': 'eyJzaWduZWRNZXNzYWdlIjoie1wiZW5jcnlwdGVkTWVzc2FnZVwiOlwiV1RKcDJWYjJqNnRaOVJGbUFO',
        },
        'status': 'success',
    }


@pytest.fixture
def visa_checkout_response_headers():
    return {
        'X-Correlation-ID': 'fake_correlation_id',
        'X-VTS-Correlation-ID': 'fake_vts_correlation_id',
        'X-VTS-Response-ID': 'fake_vts_response_id',
    }


@pytest.fixture
def visa_sign_response_body():
    return {
        'code': 200,
        'status': 'success',
        'data': {
            'headers': {
                'X-Header-Test': ['val1'],
                'X-Multi-Header': ['mult_val_1', 'mult_val_2'],
            },
            'url': 'http://yandex.ru?key=my_key'
        }
    }


@pytest.fixture
def mastercard_sign_response_body():
    return {
        'code': 200,
        'status': 'success',
        'data': {
            'headers': {
                'X-Header-Test': ['val1'],
                'X-Multi-Header': ['mult_val_1', 'mult_val_2'],
            },
        }
    }


@pytest.fixture
def visa_verify_request_response_body():
    return {
        'code': 200,
        'status': 'success',
        'data': {},
    }


@pytest.fixture
def verify_recipient_key_response_body():
    return {
        'code': 200,
        'status': 'success',
        'data': {},
    }


@pytest.fixture
def verify_recipient_key_failed_response_body():
    return {
        'code': 400,
        'status': 'fail',
        'data': {
            'message': 'INVALID_PUBLIC_KEY',
            'params': {
                'description': (
                    "paymenttoken: recipient key verifier: can't verify signature: "
                    "square/go-jose: error in cryptographic primitive"
                )
            },
        },
    }


def test_mastercard_checkout_response_entity_creation(mastercard_checkout_response_body):
    created = create_mastercard_checkout_result(mastercard_checkout_response_body['data'])

    assert_that(
        created,
        has_properties({
            'payment_token': 'eyJzaWduZWRNZXNzYWdlIjoie1wiZW5jcnlwdGVkTWVzc2FnZVwiOlwiV1RKcDJWYjJqNnRaOVJGbUFO',
        })
    )


def test_visa_checkout_response_entity_creation(visa_checkout_response_body):
    created = create_visa_checkout_result(visa_checkout_response_body['data'])

    assert_that(
        created,
        has_properties({
            'payment_token': 'eyJzaWduZWRNZXNzYWdlIjoie1wiZW5jcnlwdGVkTWVzc2FnZVwiOlwiV1RKcDJWYjJqNnRaOVJGbUFO',
        })
    )


def test_visa_sign_response_entity_creation(visa_sign_response_body):
    created = create_sign_result(visa_sign_result_schema, visa_sign_response_body['data'])

    assert_that(
        created,
        has_properties({
            'headers': visa_sign_response_body['data']['headers'],
            'url': 'http://yandex.ru?key=my_key'
        })
    )


class TestMasterCardCheckoutInteractionMethodWorks:
    @pytest.fixture(autouse=True)
    def mock_mastercard_checkout_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        mastercard_checkout_response_body,
        mastercard_checkout_response_headers,
    ):
        return aioresponses_mocker.post(
            f'{yandex_pay_settings.DUCKGO_API_URL}/v1/mastercard/checkout',
            payload=mastercard_checkout_response_body,
            headers=mastercard_checkout_response_headers,
        )

    @pytest.fixture
    def expected_mastercard_checkout_result(self, mastercard_checkout_response_body):
        return create_mastercard_checkout_result(mastercard_checkout_response_body['data'])

    @pytest.mark.asyncio
    async def test_mastercard_checkout_normal_response(
        self,
        duckgo_client: DuckGoClient,
        mock_mastercard_checkout_response,
        mastercard_checkout_request_params,
        expected_mastercard_checkout_result,
    ):
        mastercard_checkout_result = await duckgo_client.mastercard_checkout(**mastercard_checkout_request_params)

        assert mastercard_checkout_result == expected_mastercard_checkout_result

    @pytest.mark.asyncio
    async def test_mastercard_checkout_response_headers(
        self,
        duckgo_client: DuckGoClient,
        mock_mastercard_checkout_response,
        mastercard_checkout_request_params,
        expected_mastercard_checkout_result,
        mastercard_checkout_response_headers,
    ):
        await duckgo_client.mastercard_checkout(**mastercard_checkout_request_params)

        ctx = duckgo_client.logger.get_context()
        assert_that(
            ctx,
            has_entries(response_headers=has_entries(mastercard_checkout_response_headers))
        )


class TestMasterCardCheckoutRequestHasCorrectJSONBody:
    @pytest.fixture(autouse=True)
    def mock_mastercard_checkout_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        mastercard_checkout_response_body,
    ):
        return aioresponses_mocker.post(
            f'{yandex_pay_settings.DUCKGO_API_URL}/v1/mastercard/checkout',
            payload=mastercard_checkout_response_body,
        )

    @pytest.fixture
    def expected_json_argument(self, mastercard_checkout_request_params):
        return {
            'card_id': mastercard_checkout_request_params['card_id'],
            'recipient_id': mastercard_checkout_request_params['recipient_id'],
            'recipient_pub_key': mastercard_checkout_request_params['recipient_pub_key'],
            'recipient_pub_key_signature': mastercard_checkout_request_params['recipient_pub_key_signature'],
            'transaction_info': {
                'amount': mastercard_checkout_request_params['transaction_amount'],
                'currency': mastercard_checkout_request_params['transaction_currency'],
            },
            'mit_info': {
                'recurring': mastercard_checkout_request_params['mit_info'].recurring,
                'deferred': mastercard_checkout_request_params['mit_info'].deferred,
            },
            'gateway_merchant_id': mastercard_checkout_request_params['gateway_merchant_id'],
            'message_expiration': mastercard_checkout_request_params['message_expiration'],
            'message_id': mastercard_checkout_request_params['message_id'],
        }

    @pytest.mark.asyncio
    async def test_mastercard_checkout_request_body(
        self,
        duckgo_client: DuckGoClient,
        expected_json_argument,
        mock_mastercard_checkout_response,
        mastercard_checkout_request_params,
    ):
        """Сформировали корректное тело запроса."""
        await duckgo_client.mastercard_checkout(**mastercard_checkout_request_params)

        _, call_kwargs = mock_mastercard_checkout_response.call_args
        assert call_kwargs['json'] == expected_json_argument


class TestRaisesBaseDuckGoInteractionErrorIfResponseIsNotOK:
    BAD_RESPONSES = [
        {
            'status': 'fail',
            'code': 400,
            'data': {
                'message': 'INVALID_PUBLIC_KEY',
                'params': {
                    'description': "can't parse public key: illegal base64 data at input byte 124"
                }
            }
        },
        {
            'status': 'fail',
            'code': 400,
            'data': {
                'message': 'INVALID_REQUEST',
                'params': {
                    'description': "can't parse payload: invalid character 'a' looking for beginning of value"
                }
            }
        }
    ]

    @pytest.fixture(params=BAD_RESPONSES)
    def mastercard_checkout_error_body(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    def mock_mastercard_checkout_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        mastercard_checkout_error_body,
    ):
        aioresponses_mocker.post(
            f'{yandex_pay_settings.DUCKGO_API_URL}/v1/mastercard/checkout',
            status=400,
            payload=mastercard_checkout_error_body,
        )

    @pytest.mark.asyncio
    async def test_raises_base_duckgo_interaction_error(
        self,
        duckgo_client: DuckGoClient,
        mastercard_checkout_request_params,
    ):
        with pytest.raises(DuckGoInteractionError):
            await duckgo_client.mastercard_checkout(**mastercard_checkout_request_params)


class TestVisaCheckoutInteractionMethodWorks:
    @pytest.fixture(autouse=True)
    def mock_visa_checkout_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        visa_checkout_response_body,
    ):
        return aioresponses_mocker.post(
            f'{yandex_pay_settings.DUCKGO_API_URL}/v1/visa/checkout',
            payload=visa_checkout_response_body,
        )

    @pytest.fixture
    def expected_visa_checkout_result(self, visa_checkout_response_body):
        return create_visa_checkout_result(visa_checkout_response_body['data'])

    @pytest.mark.asyncio
    async def test_visa_checkout_normal_response(
        self,
        duckgo_client: DuckGoClient,
        mock_visa_checkout_response,
        visa_checkout_request_params,
        expected_visa_checkout_result,
    ):
        visa_checkout_result = await duckgo_client.visa_checkout(**visa_checkout_request_params)

        assert visa_checkout_result == expected_visa_checkout_result


class TestVisaCheckoutRequestReturnsCorrectData:
    @pytest.fixture(autouse=True)
    def mock_visa_checkout_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        visa_checkout_response_body,
        visa_checkout_response_headers,
    ):
        return aioresponses_mocker.post(
            f'{yandex_pay_settings.DUCKGO_API_URL}/v1/visa/checkout',
            payload=visa_checkout_response_body,
            headers=visa_checkout_response_headers,
        )

    @pytest.fixture
    def expected_json_argument(self, visa_checkout_request_params, message_id):
        return {
            'provisioned_token_id': visa_checkout_request_params['card_id'],
            'recipient_id': visa_checkout_request_params['recipient_id'],
            'recipient_pub_key': visa_checkout_request_params['recipient_pub_key'],
            'client_payment_data_id': base64.b32encode(
                hashlib.sha256(message_id.encode('utf-8')).digest()
            ).decode('utf-8').rstrip('=')[:36],
            'recipient_pub_key_signature': visa_checkout_request_params['recipient_pub_key_signature'],
            'transaction_info': {
                'amount': visa_checkout_request_params['transaction_amount'],
                'currency': visa_checkout_request_params['transaction_currency'],
            },
            'mit_info': {
                'recurring': visa_checkout_request_params['mit_info'].recurring,
                'deferred': visa_checkout_request_params['mit_info'].deferred,
            },
            'gateway_merchant_id': visa_checkout_request_params['gateway_merchant_id'],
            'message_expiration': visa_checkout_request_params['message_expiration'],
            'message_id': message_id,
            'relationship_id': 'relationship-id',
        }

    @pytest.mark.asyncio
    async def test_visa_checkout_request_body(
        self,
        duckgo_client: DuckGoClient,
        expected_json_argument,
        mock_visa_checkout_response,
        visa_checkout_request_params,
    ):
        """Сформировали корректное тело запроса."""
        await duckgo_client.visa_checkout(**visa_checkout_request_params)

        _, call_kwargs = mock_visa_checkout_response.call_args
        assert call_kwargs['json'] == expected_json_argument

    @pytest.mark.asyncio
    async def test_visa_checkout_request_headers(
        self,
        duckgo_client: DuckGoClient,
        expected_json_argument,
        mock_visa_checkout_response,
        visa_checkout_request_params,
        visa_checkout_response_headers,
    ):
        await duckgo_client.visa_checkout(**visa_checkout_request_params)

        ctx = duckgo_client.logger.get_context()
        assert_that(
            ctx,
            has_entries(response_headers=has_entries(visa_checkout_response_headers))
        )


class TestVisaRaisesBaseDuckGoInteractionErrorIfResponseIsNotOK:
    BAD_RESPONSES = [
        {
            'status': 'fail',
            'code': 400,
            'data': {
                'message': 'INVALID_PUBLIC_KEY',
                'params': {
                    'description': "can't parse public key: illegal base64 data at input byte 124"
                }
            }
        },
        {
            'status': 'fail',
            'code': 400,
            'data': {
                'message': 'INVALID_REQUEST',
                'params': {
                    'description': "can't parse payload: invalid character 'a' looking for beginning of value"
                }
            }
        }
    ]

    @pytest.fixture(params=BAD_RESPONSES)
    def visa_checkout_error_body(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    def mock_visa_checkout_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        visa_checkout_error_body,
    ):
        aioresponses_mocker.post(
            f'{yandex_pay_settings.DUCKGO_API_URL}/v1/visa/checkout',
            status=400,
            payload=visa_checkout_error_body,
        )

    @pytest.mark.asyncio
    async def test_raises_base_duckgo_interaction_error(
        self,
        duckgo_client: DuckGoClient,
        visa_checkout_request_params,
    ):
        with pytest.raises(DuckGoInteractionError):
            await duckgo_client.visa_checkout(**visa_checkout_request_params)


class TestVisaSignInteractionMethodWorks:
    @pytest.fixture(autouse=True)
    def mock_visa_sign_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        visa_sign_response_body,
    ):
        return aioresponses_mocker.post(
            f'{yandex_pay_settings.DUCKGO_API_URL}/v1/visa/sign_request',
            payload=visa_sign_response_body,
        )

    @pytest.fixture
    def expected_visa_sign_result(self, visa_sign_response_body):
        return create_sign_result(visa_sign_result_schema, visa_sign_response_body['data'])

    @pytest.mark.asyncio
    async def test_visa_sign_normal_response(
        self,
        duckgo_client: DuckGoClient,
        mock_visa_sign_response,
        visa_sign_request_params,
        expected_visa_sign_result,
    ):
        visa_sign_result = await duckgo_client.sign_visa_request(
            method=visa_sign_request_params['method'],
            url=visa_sign_request_params['url'],
            body_b64=visa_sign_request_params['body']
        )

        assert visa_sign_result == expected_visa_sign_result


class TestVisaSignRequestHasCorrectJSONBody:
    @pytest.fixture(autouse=True)
    def mock_visa_sign_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        visa_sign_response_body,
    ):
        return aioresponses_mocker.post(
            f'{yandex_pay_settings.DUCKGO_API_URL}/v1/visa/sign_request',
            payload=visa_sign_response_body,
        )

    @pytest.fixture
    def expected_json_argument(self, visa_sign_request_params):
        return {
            'method': visa_sign_request_params['method'],
            'url': visa_sign_request_params['url'],
            'body': visa_sign_request_params['body']
        }

    @pytest.mark.asyncio
    async def test_visa_sign_request_body(
        self,
        duckgo_client: DuckGoClient,
        expected_json_argument,
        mock_visa_sign_response,
        visa_sign_request_params
    ):
        await duckgo_client.sign_visa_request(
            method=visa_sign_request_params['method'],
            url=visa_sign_request_params['url'],
            body_b64=visa_sign_request_params['body']
        )

        _, call_kwargs = mock_visa_sign_response.call_args
        assert call_kwargs['json'] == expected_json_argument


class TestVisaSignRaisesBaseDuckGoInteractionErrorIfResponseIsNotOK:
    BAD_RESPONSES = [
        {
            'status': 'fail',
            'code': 400,
            'data': {
                'message': 'INTERNAL_ERROR',
                'params': {
                    'description': "wrong base64 fortmat"
                }
            }
        },
    ]

    @pytest.fixture(params=BAD_RESPONSES)
    def visa_sign_error_body(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    def mock_visa_sign_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        visa_sign_error_body,
    ):
        return aioresponses_mocker.post(
            f'{yandex_pay_settings.DUCKGO_API_URL}/v1/visa/sign_request',
            status=400,
            payload=visa_sign_error_body,
        )

    @pytest.mark.asyncio
    async def test_raises_base_duckgo_interaction_error(
        self,
        duckgo_client: DuckGoClient,
        visa_sign_request_params,
    ):
        with pytest.raises(DuckGoInteractionError):
            await duckgo_client.sign_visa_request(
                method=visa_sign_request_params['method'],
                url=visa_sign_request_params['url'],
                body_b64=visa_sign_request_params['body']
            )


class TestVisaVerifyRequestInteractionMethodWorks:
    @pytest.fixture(autouse=True)
    def mock_visa_verify_request_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        visa_verify_request_response_body,
    ):
        return aioresponses_mocker.post(
            f'{yandex_pay_settings.DUCKGO_API_URL}/v1/visa/verify_request',
            payload=visa_verify_request_response_body,
        )

    @pytest.mark.asyncio
    async def test_visa_verify_request_normal_response(
        self,
        duckgo_client: DuckGoClient,
        mock_visa_verify_request_response,
        visa_verify_request_params,
    ):
        # expect no error
        await duckgo_client.visa_verify_request(
            signature=visa_verify_request_params['signature'],
            url=visa_verify_request_params['url'],
            body_b64=visa_verify_request_params['body']
        )


class TestVisaCheckSignatureRequestHasCorrectJSONBody:
    @pytest.fixture(autouse=True)
    def mock_visa_verify_request_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        visa_verify_request_response_body,
    ):
        return aioresponses_mocker.post(
            f'{yandex_pay_settings.DUCKGO_API_URL}/v1/visa/verify_request',
            payload=visa_verify_request_response_body,
        )

    @pytest.fixture
    def expected_json_argument(self, visa_verify_request_params):
        return {
            'signature': visa_verify_request_params['signature'],
            'url': visa_verify_request_params['url'],
            'body': visa_verify_request_params['body']
        }

    @pytest.mark.asyncio
    async def test_visa_verify_request_body(
        self,
        duckgo_client: DuckGoClient,
        expected_json_argument,
        mock_visa_verify_request_response,
        visa_verify_request_params
    ):
        await duckgo_client.visa_verify_request(
            signature=visa_verify_request_params['signature'],
            url=visa_verify_request_params['url'],
            body_b64=visa_verify_request_params['body']
        )

        _, call_kwargs = mock_visa_verify_request_response.call_args
        assert call_kwargs['json'] == expected_json_argument


class TestVisaCheckSignatureRaisesBaseDuckGoInteractionErrorIfResponseIsNotOK:
    BAD_RESPONSES = [
        {
            'status': 'fail',
            'code': 400,
            'data': {
                'message': 'INTERNAL_ERROR',
                'params': {
                    'description': "wrong base64 format"
                }
            }
        },
    ]

    @pytest.fixture(params=BAD_RESPONSES)
    def visa_verify_request_error_body(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    def mock_visa_verify_request_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        visa_verify_request_error_body,
    ):
        return aioresponses_mocker.post(
            f'{yandex_pay_settings.DUCKGO_API_URL}/v1/visa/verify_request',
            status=400,
            payload=visa_verify_request_error_body,
        )

    @pytest.mark.asyncio
    async def test_raises_base_duckgo_interaction_error(
        self,
        duckgo_client: DuckGoClient,
        visa_verify_request_params,
    ):
        with pytest.raises(DuckGoInteractionError):
            await duckgo_client.visa_verify_request(
                signature=visa_verify_request_params['signature'],
                url=visa_verify_request_params['url'],
                body_b64=visa_verify_request_params['body']
            )


class TestMasterCardSignInteractionMethod:
    @pytest.fixture(autouse=True)
    def mock_mastercard_sign_response(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        mastercard_sign_response_body,
    ):
        return aioresponses_mocker.post(
            f'{yandex_pay_settings.DUCKGO_API_URL}/v1/mastercard/sign_request',
            payload=mastercard_sign_response_body,
        )

    @pytest.fixture
    def expected_mastercard_sign_result(self, mastercard_sign_response_body):
        return create_sign_result(
            mastercard_sign_result_schema, mastercard_sign_response_body['data']
        )

    @pytest.mark.asyncio
    async def test_mastercard_sign_normal_response(
        self,
        duckgo_client: DuckGoClient,
        mock_mastercard_sign_response,
        mastercard_sign_request_params,
        expected_mastercard_sign_result,
    ):
        sign_result = await duckgo_client.sign_mastercard_request(
            method=mastercard_sign_request_params['method'],
            url=mastercard_sign_request_params['url'],
            body_b64=mastercard_sign_request_params['body'],
        )

        assert sign_result == expected_mastercard_sign_result

        mock_mastercard_sign_response.assert_called_once()
        _, call_kwargs = mock_mastercard_sign_response.call_args
        assert call_kwargs['json'] == mastercard_sign_request_params


@pytest.mark.asyncio
async def test_verify_recipient_key_successful_response(
    aioresponses_mocker,
    yandex_pay_settings,
    duckgo_client,
    verify_recipient_key_response_body,
):
    aioresponses_mocker.post(
        f'{yandex_pay_settings.DUCKGO_API_URL}/v1/payment_token/verify_recipient_key',
        payload=verify_recipient_key_response_body,
    )

    response = await duckgo_client.verify_recipient_key('fake_key', 'fake_key_signature')

    assert_that(response, equal_to(verify_recipient_key_response_body))


@pytest.mark.asyncio
async def test_verify_recipient_key_failed_response(
    aioresponses_mocker,
    yandex_pay_settings,
    duckgo_client,
    verify_recipient_key_failed_response_body,
):
    aioresponses_mocker.post(
        f'{yandex_pay_settings.DUCKGO_API_URL}/v1/payment_token/verify_recipient_key',
        status=400,
        payload=verify_recipient_key_failed_response_body,
    )

    with pytest.raises(InvalidPublicKeyError) as exc_info:
        await duckgo_client.verify_recipient_key('fake_key', 'fake_key_signature')

    assert_that(
        exc_info.value,
        has_properties(
            status_code=400,
            response_status='fail',
            params={
                'headers': {},
                **verify_recipient_key_failed_response_body['data'],
            }
        )
    )


class TestAuthorization:
    @pytest.mark.asyncio
    async def test_shared_key(self, duckgo_client, aioresponses_mocker, yandex_pay_settings):
        url = yarl.URL(f'{yandex_pay_settings.DUCKGO_API_URL}/v1/whatever')
        aioresponses_mocker.post(
            url,
            status=200,
            payload={},
        )
        duckgo_client.SHARED_KEY_TYPE = 'SharedKey'
        duckgo_client.SHARED_KEY = 'helloworld'

        await duckgo_client.post('my_method', url)

        assert_that(
            aioresponses_mocker.requests[('POST', url)][0].kwargs,
            has_entries({
                'headers': has_entries({
                    'Authorization': 'SharedKey helloworld',
                })
            })
        )
