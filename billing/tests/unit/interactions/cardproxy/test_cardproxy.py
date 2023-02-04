import json
from datetime import datetime, timezone
from urllib.parse import urljoin

import pytest
from aioresponses import CallbackResult
from marshmallow import ValidationError
from pay.lib.entities.payment_token import MITInfo

from sendr_interactions.base import LogFlag
from sendr_interactions.exceptions import InteractionResponseError

from hamcrest import assert_that, equal_to, has_entries, has_properties

from billing.yandex_pay.yandex_pay.core.entities.enums import MasterCardTokenStatus, VisaTokenStatus
from billing.yandex_pay.yandex_pay.interactions import CardProxyClient
from billing.yandex_pay.yandex_pay.interactions.cardproxy import (
    MastercardEnrollment, forward_request, map_mastercard_response_json_to_class, map_visa_response_json_to_class
)
from billing.yandex_pay.yandex_pay.interactions.duckgo.entities.thales import ThalesEncryptedCardResult
from billing.yandex_pay.yandex_pay.tests.matchers import convert_then_match


@pytest.fixture
async def cardproxy_client(create_client) -> CardProxyClient:
    CardProxyClient.REQUEST_RETRY_TIMEOUTS = (0.01,)
    client = create_client(CardProxyClient)
    yield client
    await client.close()


@pytest.fixture
def mastercard_response() -> dict:
    return {
        'srcCorrelationId': 'afd9061c-9cfc-4001-9e2b-b276e9782bdb',
        'maskedCard': {
            'srcDigitalCardId': '2523f68d-328b-414b-baa0-197411ee05ec',
            'panBin': '531222',
            'panLastFour': '3332',
            'panExpirationMonth': '05',
            'panExpirationYear': '2025',
            'paymentCardDescriptor': 'some_descriptor',
            'paymentCardType': 'some_type',
            'paymentAccountReference': '6421AKEROU2QIQWC03DPN3OMOLCM3',
            'dateOfCardCreated': '2020-11-20T12:05:28.051Z',
            'digitalCardData': {
                'status': 'ACTIVE',
                'descriptorName': 'Example Bank Product Configuration',
                'artUri': 'https://sbx.assets.mastercard.com/c911d734-2f4b-4326-8363-afb0ffe2bc2b.png',
            },
        },
        'maskedConsumer': {
            'srcConsumerId': '33c9a0b0-b7a3-4075-ad4e-47f82366dc29',
            'maskedEmailAddress': 'email@email.com',
            'maskedConsumerIdentity': {'identityType': 'EXTERNAL_ACCOUNT_ID', 'maskedIdentityValue': 'fake_account_id'},
            'dateConsumerAdded': '2020-11-20T12:05:28.051Z',
            'status': 'ACTIVE',
        },
    }


@pytest.fixture
def mastercard_response_headers():
    return {
        'X-Correlation-ID': 'fake_correlation_id_mc',
        'X-MC-Correlation-ID': 'fake_mc_correlation_id',
        'X-Src-Cx-Flow-Id': 'fake_flow_id',
        'X-Vcap-Request-Id': 'fake_vcap_request_id',
    }


@pytest.fixture
def expected_mastercard_result(mastercard_response) -> MastercardEnrollment:
    return map_mastercard_response_json_to_class(mastercard_response)


@pytest.mark.asyncio
async def test_should_pass_token_header_to_cardproxy(
    aioresponses_mocker, yandex_pay_settings, cardproxy_client, mastercard_response
):
    token = 'diehard_card_token'

    def callback(url, **kwargs):
        assert_that(kwargs['headers']['X-Diehard-Card-Token'], equal_to(token))

        return CallbackResult(payload=mastercard_response)

    aioresponses_mocker.post(
        urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request), callback=callback
    )

    await cardproxy_client.mastercard_enrollment(10, token, 1, 2020, 'Tester')


@pytest.mark.asyncio
async def test_mastercard_enrollment_ok(
    aioresponses_mocker,
    yandex_pay_settings,
    cardproxy_client,
    mastercard_response,
    mastercard_response_headers,
    expected_mastercard_result,
):
    aioresponses_mocker.post(
        urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
        payload=mastercard_response,
        headers=mastercard_response_headers,
    )

    mastercard_result = await cardproxy_client.mastercard_enrollment(
        uid=1, pci_card_token='token', expiration_month=8, expiration_year=2021, holder_name='some_holder_name'
    )

    assert_that(mastercard_result, equal_to(expected_mastercard_result))

    ctx = cardproxy_client.logger.get_context()
    assert_that(ctx, has_entries(response_headers=has_entries(mastercard_response_headers)))


@pytest.mark.asyncio
async def test_should_send_mastercard_enrollment_request_with_body_from_func(
    aioresponses_mocker,
    yandex_pay_settings,
    mastercard_response,
    cardproxy_client,
    request_id,
    mocker,
):
    mocked_body = {'test': 'test'}
    build_mock = mocker.patch(
        'billing.yandex_pay.yandex_pay.interactions.cardproxy.'
        'forward_request.build_mastercard_enrollment_forward_request_body',
        mocker.Mock(return_value=mocked_body),
    )
    uid = 10
    token = 'diehard_card_token'
    expiration_month = 5
    expiration_year = 2025
    holder_name = 'Tester'

    def callback(url, **kwargs):
        assert_that(kwargs['json'], equal_to(mocked_body))

        return CallbackResult(payload=mastercard_response)

    aioresponses_mocker.post(
        urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request), callback=callback
    )

    await cardproxy_client.mastercard_enrollment(
        uid=uid,
        pci_card_token=token,
        expiration_month=expiration_month,
        expiration_year=expiration_year,
        holder_name=holder_name,
    )

    build_mock.assert_called_once_with(
        uid=uid,
        accounts_secret_base64=yandex_pay_settings.CARD_PROXY_ACCOUNTS_KEY_BASE64,
        expiration_month=expiration_month,
        expiration_year=expiration_year,
        holder_name=holder_name,
        request_id=request_id,
    )


def test_mastercard_response_mapping(mastercard_response):
    mapped = map_mastercard_response_json_to_class(mastercard_response)

    assert_that(
        mapped,
        has_properties(
            src_correlation_id='afd9061c-9cfc-4001-9e2b-b276e9782bdb',
            masked_card=has_properties(
                src_digital_card_id='2523f68d-328b-414b-baa0-197411ee05ec',
                pan_bin='531222',
                pan_last_four='3332',
                pan_expiration_month='05',
                pan_expiration_year='2025',
                payment_card_descriptor='some_descriptor',
                payment_card_type='some_type',
                payment_account_reference='6421AKEROU2QIQWC03DPN3OMOLCM3',
                date_of_card_created=datetime(2020, 11, 20, 12, 5, 28, 51000, tzinfo=timezone.utc),
                digital_card_data=has_properties(
                    status=MasterCardTokenStatus.ACTIVE,
                    descriptor_name='Example Bank Product Configuration',
                    art_uri='https://sbx.assets.mastercard.com/c911d734-2f4b-4326-8363-afb0ffe2bc2b.png',
                ),
            ),
            masked_consumer=has_properties(
                src_consumer_id='33c9a0b0-b7a3-4075-ad4e-47f82366dc29', masked_email_address='email@email.com'
            ),
        ),
    )


def test_mastercard_response_mapping_should_raise_validation_exception():
    totally_bad_response = {}

    with pytest.raises(ValidationError):
        map_mastercard_response_json_to_class(totally_bad_response)


@pytest.mark.asyncio
@pytest.mark.parametrize('code', (500, 504))
async def test_should_retry_5xx_response(
    aioresponses_mocker,
    cardproxy_client,
    mastercard_response,
    yandex_pay_settings,
    expected_mastercard_result,
    code,
):
    aioresponses_mocker.post(
        urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
        status=code,
    )
    aioresponses_mocker.post(
        urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
        payload=mastercard_response,
    )

    mastercard_result = await cardproxy_client.mastercard_enrollment(
        uid=1, pci_card_token='token', expiration_month=8, expiration_year=2021, holder_name='some_holder_name'
    )

    assert_that(mastercard_result, equal_to(expected_mastercard_result))


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'code, payload, tsp',
    [
        (
            403,
            {
                "code": 403,
                "data": {"errorResponse": {"message": "some msg", "reason": "cardVerificationFailed", "status": 403}},
                "status": "fail",
            },
            'visa',
        ),
        (
            400,
            {
                'code': 400,
                'data': {
                    'errordetail': [
                        {'message': 'some msg', 'reason': 'ISSUER_DECLINED', 'source': 'Card', 'sourceType': 'BODY'}
                    ],
                    'message': 'some msg',
                    'reason': 'INVALID_STATE',
                    'status': 400,
                },
                'status': 'fail',
            },
            'mastercard',
        ),
    ],
)
async def test_should_set_422_status(
    aioresponses_mocker,
    cardproxy_client,
    yandex_pay_settings,
    code,
    payload,
    tsp,
):
    aioresponses_mocker.post(
        urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
        status=code,
        payload=payload,
    )

    enrollment = getattr(cardproxy_client, f'{tsp}_enrollment')
    kwargs = {'holder_name': 'abc'} if tsp == 'mastercard' else {}

    with pytest.raises(InteractionResponseError) as e:
        await enrollment(
            uid=1,
            pci_card_token='token',
            expiration_month=8,
            expiration_year=2021,
            **kwargs,
        )

    assert e.value.status_code == 422


@pytest.mark.asyncio
@pytest.mark.parametrize('code', ('500', '504'))
async def test_should_retry_5xx_from_diehard(
    aioresponses_mocker,
    cardproxy_client,
    mastercard_response,
    yandex_pay_settings,
    expected_mastercard_result,
    code,
):
    aioresponses_mocker.post(
        urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
        payload={'some_error_response': True},
        headers={'X-DieHard-HTTP-Status': code},
    )
    aioresponses_mocker.post(
        urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
        payload=mastercard_response,
    )

    mastercard_result = await cardproxy_client.mastercard_enrollment(
        uid=1, pci_card_token='token', expiration_month=8, expiration_year=2021, holder_name='some_holder_name'
    )

    assert_that(mastercard_result, equal_to(expected_mastercard_result))


class TestVisaEnroll:
    @pytest.fixture
    def visa_response(self, yandex_pay_settings, aioresponses_mocker) -> dict:
        return {
            "vProvisionedTokenID": "97e3f95cc4d0c8abd27b109ec71f5202",
            "vPanEnrollmentID": "c6fa06c6c7270f90275312cd7e5d7a02",
            "correlationID": "c6fa06c6c7270f90275312cd7e512345",
            "vtsCorrelationID": "c6fa06c6c7270f90275312cd7e56f7g1",
            "vtsResponseID": "c6fa06c6c7270f90275312cd7e513131",
            "paymentInstrument": {
                "last4": "0036",
                "expirationDate": {"year": "2022", "month": "12"},
                "expDatePrintedInd": "Y",
                "paymentAccountReference": "V0010013020293728316209189607",
            },
            "tokenInfo": {
                "encTokenInfo": "eyJhbGciOiJBMjU2R0NNS1ciLCJpdiI6IlRGVDlreTFFaExoemt3U",
                "tokenRequestorID": "40010082083",
                "tokenReferenceID": "DNITHE302029372832635410",
                "tokenStatus": "ACTIVE",
                "last4": "0622",
                "expirationDate": {"month": "12", "year": "2022"},
            },
        }

    @pytest.fixture
    def visa_response_headers(self):
        return {
            'X-Correlation-ID': 'fake_correlation_id',
            'X-VTS-Correlation-ID': 'fake_vts_correlation_id',
            'X-VTS-Response-ID': 'fake_vts_response_id',
        }

    @pytest.fixture
    def expected_visa_result(self, visa_response):
        return map_visa_response_json_to_class(visa_response)

    @pytest.mark.asyncio
    async def test_should_pass_token_header_to_cardproxy(
        self, aioresponses_mocker, yandex_pay_settings, cardproxy_client, visa_response
    ):
        token = 'diehard_card_token'

        def callback(url, **kwargs):
            assert_that(kwargs['headers']['X-Diehard-Card-Token'], equal_to(token))

            return CallbackResult(payload=visa_response)

        aioresponses_mocker.post(
            urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request), callback=callback
        )

        await cardproxy_client.visa_enrollment(10, token, 1, 2030)

    @pytest.mark.asyncio
    async def test_visa_enrollment_ok(
        self,
        aioresponses_mocker,
        yandex_pay_settings,
        cardproxy_client,
        visa_response,
        visa_response_headers,
        expected_visa_result,
    ):
        aioresponses_mocker.post(
            urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
            payload=visa_response,
            headers=visa_response_headers,
        )

        visa_result = await cardproxy_client.visa_enrollment(
            uid=1,
            pci_card_token='token',
            expiration_month=8,
            expiration_year=2021,
        )

        assert_that(visa_result, equal_to(expected_visa_result))

        ctx = cardproxy_client.logger.get_context()
        assert_that(ctx, has_entries(response_headers=has_entries(visa_response_headers)))

    @pytest.mark.asyncio
    async def test_should_send_visa_enrollment_request_with_body_from_func(
        self,
        aioresponses_mocker,
        yandex_pay_settings,
        visa_response,
        cardproxy_client,
        request_id,
        mocker,
    ):
        mocked_body = {'test': 'test'}
        build_mock = mocker.patch(
            'billing.yandex_pay.yandex_pay.interactions.cardproxy.'
            'forward_request.build_visa_enrollment_forward_request_body',
            mocker.Mock(return_value=mocked_body),
        )
        uid = 10
        token = 'diehard_card_token'
        expiration_month = 5
        expiration_year = 2025

        def callback(url, **kwargs):
            assert_that(kwargs['json'], equal_to(mocked_body))

            return CallbackResult(payload=visa_response)

        aioresponses_mocker.post(
            urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request), callback=callback
        )

        await cardproxy_client.visa_enrollment(
            uid=uid,
            pci_card_token=token,
            expiration_month=expiration_month,
            expiration_year=expiration_year,
        )

        build_mock.assert_called_once_with(
            uid=uid,
            expiration_month=expiration_month,
            expiration_year=expiration_year,
            request_id=request_id,
            secret_base64=yandex_pay_settings.VISA_ACCOUNT_SECRET_BASE64,
            relationship_id=yandex_pay_settings.VISA_COMMUNAL_TOKEN_RELATIONSHIP_ID,
        )

    @pytest.mark.asyncio
    async def test_should_send_visa_relationship_id_in_request_body(
        self,
        aioresponses_mocker,
        yandex_pay_settings,
        cardproxy_client,
        visa_response,
        visa_response_headers,
        expected_visa_result,
    ):
        relationship_id = yandex_pay_settings.VISA_COMMUNAL_TOKEN_RELATIONSHIP_ID

        def callback(url, **kwargs):
            assert_that(
                kwargs['json']['body'], convert_then_match(json.loads, has_entries(relationship_id=relationship_id))
            )

            return CallbackResult(payload=visa_response, headers=visa_response_headers)

        aioresponses_mocker.post(
            urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
            callback=callback,
        )

        visa_result = await cardproxy_client.visa_enrollment(
            uid=1,
            pci_card_token='token',
            expiration_month=8,
            expiration_year=2021,
        )

        assert_that(visa_result, equal_to(expected_visa_result))

    def test_visa_response_mapping(self, visa_response):
        mapped = map_visa_response_json_to_class(visa_response)

        assert_that(
            mapped,
            has_properties(
                provisioned_token_id='97e3f95cc4d0c8abd27b109ec71f5202',
                pan_enrollment_id='c6fa06c6c7270f90275312cd7e5d7a02',
                correlation_id='c6fa06c6c7270f90275312cd7e512345',
                vts_correlation_id='c6fa06c6c7270f90275312cd7e56f7g1',
                vts_response_id='c6fa06c6c7270f90275312cd7e513131',
                payment_instrument=has_properties(
                    last4='0036',
                    expiration_date=has_properties(year=2022, month=12),
                    payment_account_reference='V0010013020293728316209189607',
                ),
                token_info=has_properties(
                    last4='0622',
                    expiration_date=has_properties(year=2022, month=12),
                    status=VisaTokenStatus.ACTIVE,
                    token_requestor_id='40010082083',
                    token_reference_id='DNITHE302029372832635410',
                ),
            ),
        )

    def test_visa_response_mapping_should_raise_validation_exception(self):
        totally_bad_response = {}

        with pytest.raises(ValidationError):
            map_visa_response_json_to_class(totally_bad_response)

    @pytest.mark.asyncio
    @pytest.mark.parametrize('code', (500, 504))
    async def test_should_retry_5xx_response(
        self,
        aioresponses_mocker,
        cardproxy_client,
        visa_response,
        yandex_pay_settings,
        expected_visa_result,
        code,
    ):
        aioresponses_mocker.post(
            urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
            status=code,
        )
        aioresponses_mocker.post(
            urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
            payload=visa_response,
        )

        visa_result = await cardproxy_client.visa_enrollment(
            uid=1,
            pci_card_token='token',
            expiration_month=8,
            expiration_year=2021,
        )

        assert_that(visa_result, equal_to(expected_visa_result))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('code', ('500', '504'))
    async def test_should_retry_5xx_from_diehard(
        self,
        aioresponses_mocker,
        cardproxy_client,
        visa_response,
        yandex_pay_settings,
        expected_visa_result,
        code,
    ):
        aioresponses_mocker.post(
            urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
            payload={'some_error_response': True},
            headers={'X-DieHard-HTTP-Status': code},
        )
        aioresponses_mocker.post(
            urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
            payload=visa_response,
        )

        visa_result = await cardproxy_client.visa_enrollment(
            uid=1,
            pci_card_token='token',
            expiration_month=8,
            expiration_year=2021,
        )

        assert_that(visa_result, equal_to(expected_visa_result))


class TestPANCheckoutInteractionMethod:
    @pytest.fixture(autouse=True)
    def mock_cardproxy_response(self, yandex_pay_settings, aioresponses_mocker):
        return aioresponses_mocker.post(
            urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
            payload={"status": "success", "code": 200, "data": {"payment_token": "ABC"}},
            headers={'X-DieHard-HTTP-Status': '200'},
        )

    @pytest.mark.asyncio
    async def test_can_make_normal_request(self, cardproxy_client: CardProxyClient):
        """
        Метод отрабатывает нормально при нормальном ответе.
        """
        result = await cardproxy_client.pan_checkout(
            pci_card_token='abcd1234',
            recipient_id='payture',
            recipient_pub_key='PUBLIC_KEY',
            recipient_pub_key_signature='KEY_SIGNATURE',
            gateway_merchant_id='payture-merchant',
            pan_expiration_month=1,
            pan_expiration_year=2022,
            transaction_amount=100,
            transaction_currency="RUB",
            mit_info=MITInfo(deferred=True),
            message_id="1234567",
            message_expiration=2000000000,
        )
        assert result.payment_token == 'ABC'

    class TestInteractionMethodSessionCalls:
        @pytest.fixture(autouse=True)
        def spy_on_session_request_calls(self, cardproxy_client, mocker):
            return mocker.spy(cardproxy_client.session, 'request')

        @pytest.fixture(autouse=True)
        async def call_pan_checkout_interaction_method(
            self,
            cardproxy_client,
            spy_on_session_request_calls,
        ):
            return await cardproxy_client.pan_checkout(
                pci_card_token='abcd1234',
                recipient_id='payture',
                recipient_pub_key='PUBLIC_KEY',
                recipient_pub_key_signature='KEY_SIGNATURE',
                gateway_merchant_id='payture-merchant',
                pan_expiration_month=1,
                pan_expiration_year=2022,
                transaction_amount=100,
                transaction_currency="RUB",
                mit_info=MITInfo(deferred=True),
                message_id="1234567",
                message_expiration=2000000000,
            )

        @pytest.mark.asyncio
        async def test_json_argument_built_with_builder(self, cardproxy_client, spy_on_session_request_calls):
            """
            Вызываем объект сессии с json-телом, соответствующим результату функции построения тела,
            при условии одинаковости соответствующих тестовых данных.
            """
            _, call_kwargs = spy_on_session_request_calls.call_args_list[0]
            assert call_kwargs['json'] == forward_request.build_pan_checkout_forward_request_body(
                recipient_id='payture',
                recipient_pub_key='PUBLIC_KEY',
                recipient_pub_key_signature='KEY_SIGNATURE',
                gateway_merchant_id='payture-merchant',
                pan_expiration_month=1,
                pan_expiration_year=2022,
                transaction_amount=100,
                transaction_currency="RUB",
                mit_info=MITInfo(deferred=True),
                message_id="1234567",
                message_expiration=2000000000,
                request_id=cardproxy_client.request_id,
            )

        @pytest.mark.asyncio
        async def test_card_token_is_passed_in_header(self, spy_on_session_request_calls):
            _, call_kwargs = spy_on_session_request_calls.call_args_list[0]
            assert call_kwargs['headers']['X-Diehard-Card-Token'] == 'abcd1234'


def test_cardproxy_body_logging_on_success_not_allowed():
    assert_that(CardProxyClient.LOG_RESPONSE_BODY.value & LogFlag.ON_SUCCESS.value, equal_to(0))


@pytest.mark.asyncio
async def test_thales_encrypt_card_ok(aioresponses_mocker, yandex_pay_settings, cardproxy_client):
    fake_encrypted_card = 'fake_encrypted_card'
    payload = {'data': {'encrypted_card': fake_encrypted_card}}
    aioresponses_mocker.post(
        urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
        payload=payload,
    )

    result = await cardproxy_client.thales_encrypt_card(
        pci_card_token='token',
        cvn_token='123',
        expiration_month=8,
        expiration_year=2021,
    )

    assert_that(result, equal_to(ThalesEncryptedCardResult(fake_encrypted_card)))


@pytest.mark.asyncio
async def test_thales_encrypt_card_request_content(aioresponses_mocker, yandex_pay_settings, cardproxy_client):
    mock_cardproxy = aioresponses_mocker.post(
        urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
        payload={'data': {'encrypted_card': 'fake_encrypted_card'}},
    )

    await cardproxy_client.thales_encrypt_card(
        pci_card_token='token',
        expiration_month=8,
        expiration_year=2021,
        cvn_token='123',
    )

    mock_cardproxy.assert_called_once()
    _, call_kwargs = mock_cardproxy.call_args_list[0]
    assert_that(
        call_kwargs,
        has_entries(
            json=has_entries(
                {
                    'method': 'POST',
                    'uri': 'duckgo',
                    'uri-extra': 'ThalesEncryptedCard',
                    'body': convert_then_match(
                        json.loads,
                        equal_to(
                            {
                                'card': {
                                    'primary_account_number': '$$(frag:card_number)',
                                    'pan_expiration_month': 8,
                                    'pan_expiration_year': 2021,
                                    'cvv': '$$(frag:cvn)',
                                },
                            }
                        ),
                    ),
                }
            ),
            headers=has_entries({'X-Diehard-Card-Token': 'token', 'X-Diehard-Cvn-Token': '123'}),
        ),
    )


@pytest.mark.asyncio
async def test_thales_encrypt_card_response_validation_error(
    aioresponses_mocker, yandex_pay_settings, cardproxy_client
):
    aioresponses_mocker.post(
        urljoin(yandex_pay_settings.CARD_PROXY_API_URL, CardProxyClient.Paths.forward_request),
        payload={'data': {'bad': True}},
    )

    with pytest.raises(ValidationError):
        await cardproxy_client.thales_encrypt_card(
            pci_card_token='token',
            cvn_token='cvn_token',
            expiration_month=8,
            expiration_year=2021,
        )
