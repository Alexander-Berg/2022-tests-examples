from decimal import Decimal

import pytest
from aiohttp import TCPConnector

from hamcrest import assert_that, equal_to

from pay.lib.entities.card import Card, ExpirationDate
from pay.lib.entities.enums import AuthMethod, CardNetwork, IssuerBank
from pay.lib.interactions.yandex_pay.client import AbstractYandexPayClient
from pay.lib.interactions.yandex_pay.entities import CreatePaymentTokenResponse, PaymentMethodInfo
from pay.lib.interactions.yandex_pay.exceptions import CardNotFoundError

YANDEX_PAY_API_URL = 'https://yandex-pay.test'


class YandexPayTestClient(AbstractYandexPayClient):
    BASE_URL = YANDEX_PAY_API_URL
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    CONNECTOR = TCPConnector()


class TestCreatePaymentToken:
    @pytest.mark.asyncio
    async def test_call(self, yandex_pay_client, mock_create_payment_token, params):
        await yandex_pay_client.create_payment_token(**params)

        assert_that(
            mock_create_payment_token.call_args.kwargs['json'],
            equal_to(
                {
                    'uid': 777,
                    'gateway_merchant_id': 'gw-mid',
                    'psp_external_id': 'psp-gwid',
                    'currency': 'XTS',
                    'amount': '123.45',
                    'card_id': 'card-x1234',
                    'auth_methods': ['PAN_ONLY'],
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_result_success(self, yandex_pay_client, mock_create_payment_token, params):
        result = await yandex_pay_client.create_payment_token(**params)

        assert_that(
            result,
            equal_to(
                CreatePaymentTokenResponse(
                    payment_token='yp-token',
                    message_id='yp-msgid',
                    payment_method_info=PaymentMethodInfo(
                        card_last4='1234',
                        card_network=CardNetwork.MIR,
                    ),
                )
            ),
        )

    @pytest.fixture
    def params(self):
        return dict(
            uid=777,
            gateway_merchant_id='gw-mid',
            card_id='card-x1234',
            currency='XTS',
            amount=Decimal('123.45'),
            auth_methods=[AuthMethod.PAN_ONLY],
            psp_external_id='psp-gwid',
        )

    @pytest.fixture
    def mock_create_payment_token(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{YANDEX_PAY_API_URL}/api/internal/v1/checkout/payment_tokens',
            payload={
                'status': 'success',
                'code': 200,
                'data': {
                    'payment_token': 'yp-token',
                    'message_id': 'yp-msgid',
                    'payment_method_info': {
                        'card_last4': '1234',
                        'card_network': 'MIR',
                    },
                },
            },
        )


class TestGetUserCard:
    @pytest.mark.asyncio
    async def test_result_success(self, yandex_pay_client, aioresponses_mocker, url, params):
        card_response = {
            'card_id': params['card_id'],
            'last4': '1234',
            'card_network': 'MASTERCARD',
            'issuer_bank': 'ALFABANK',
            'expiration_date': {'month': 10, 'year': 2031},
            'trust_card_id': 'trust_card_id',
        }
        mock = aioresponses_mocker.get(url, payload={'status': 'success', 'code': 200, 'data': card_response})

        result = await yandex_pay_client.get_user_card(**params)

        assert_that(
            mock.call_args.kwargs['params'],
            equal_to({'uid': 777}),
        )
        assert_that(
            result,
            equal_to(
                Card(
                    card_id=params['card_id'],
                    last4='1234',
                    card_network=CardNetwork.MASTERCARD,
                    issuer_bank=IssuerBank.ALFABANK,
                    expiration_date=ExpirationDate(month=10, year=2031),
                    trust_card_id='trust_card_id',
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_not_found(self, yandex_pay_client, aioresponses_mocker, url, params):
        aioresponses_mocker.get(
            url,
            status=404,
            payload={'code': 404, 'data': {'message': 'CARD_NOT_FOUND'}, 'status': 'fail'},
        )

        with pytest.raises(CardNotFoundError):
            await yandex_pay_client.get_user_card(**params)

    @pytest.fixture
    def params(self):
        return dict(
            uid=777,
            card_id='card-x1234',
        )

    @pytest.fixture
    def url(self, params):
        return f"{YANDEX_PAY_API_URL}/api/internal/v1/user/cards/{params['card_id']}?uid={params['uid']}"


@pytest.fixture
async def yandex_pay_client(create_interaction_client) -> YandexPayTestClient:
    client = create_interaction_client(YandexPayTestClient)
    yield client
    await client.close()
