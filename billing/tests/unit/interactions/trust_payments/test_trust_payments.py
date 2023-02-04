from dataclasses import dataclass
from datetime import datetime, timezone

import pytest

from hamcrest import assert_that, has_properties

from billing.yandex_pay.yandex_pay.interactions import TrustPaymentsClient
from billing.yandex_pay.yandex_pay.interactions.trust_payments.entities import PartnerInfo
from billing.yandex_pay.yandex_pay.interactions.trust_payments.exceptions import TrustPaymentsResponseError


@pytest.fixture
async def trust_payments_client(create_client):
    client = create_client(TrustPaymentsClient)
    client.REQUEST_RETRY_TIMEOUTS = ()
    yield client
    await client.close()


@pytest.fixture
def uid():
    return 123456789


class TestPaymentMethodsNormalResponse:
    @pytest.fixture
    def payment_method_data(self, uid):
        return {
            "region_id": 225,
            "payment_method": "card",
            "system": "MasterCard",
            "expiration_month": "01",
            "card_country": "RUS",
            "binding_ts": "1586458392.247",
            "ebin_tags_version": 0,
            "card_level": "STANDARD",
            "holder": "Card Holder",
            "id": "card-a1a1234567a12abcd12345a1a",
            "payment_system": "MasterCard",
            "last_paid_ts": "1586458393.248",
            "account": "123456****7890",
            "ebin_tags": [],
            "expiration_year": "2030",
            "aliases": [
                "card-a1a1234567a12abcd12345a1a"
            ],
            "expired": False,
            "card_bank": "SBERBANK OF RUSSIA",
            "card_id": "card-a1a1234567a12abcd12345a1a",
            "recommended_verification_type": "standard2",
            "orig_uid": str(uid),
            "partner_info": {
                "is_yabank_card": True,
                "is_fake_yabank_card": True,
                "is_yabank_card_owner": True
            },
            "binding_systems": [
                "trust"
            ]
        }

    @pytest.fixture
    def payment_methods_response_data(self, payment_method_data):
        response_data = {
            "status": "success",
            "bound_payment_methods": [
                payment_method_data,
            ],
        }
        return response_data

    @pytest.fixture(autouse=True)
    async def mock_response(
        self,
        aioresponses_mocker,
        yandex_pay_settings,
        uid,
        payment_methods_response_data,
    ):
        return aioresponses_mocker.get(
            f'{yandex_pay_settings.TRUST_PAYMENTS_API_URL}/trust-payments/v2/payment-methods?payment-method=card',
            payload=payment_methods_response_data
        )

    @pytest.mark.asyncio
    async def test_ok_response(
        self,
        uid,
        trust_payments_client: TrustPaymentsClient,
        payment_method_data,
    ):
        payment_methods = await trust_payments_client.get_payment_methods(uid)
        assert len(payment_methods) == 1
        assert_that(
            payment_methods[0],
            has_properties(dict(
                id=payment_method_data['id'],
                card_id=payment_method_data['card_id'],
                binding_systems=payment_method_data['binding_systems'],
                orig_uid=payment_method_data['orig_uid'],
                payment_method=payment_method_data['payment_method'],
                system=payment_method_data['system'],
                payment_system=payment_method_data['payment_system'],
                expiration_month=payment_method_data['expiration_month'],
                expiration_year=payment_method_data['expiration_year'],
                card_bank=payment_method_data['card_bank'],
                expired=payment_method_data['expired'],
                account=payment_method_data['account'],
                binding_ts=datetime.fromtimestamp(float(payment_method_data['binding_ts']), tz=timezone.utc),
                last_paid_ts=datetime.fromtimestamp(float(payment_method_data['last_paid_ts']), tz=timezone.utc),
                partner_info=PartnerInfo(is_yabank_card=True, is_fake_yabank_card=True, is_yabank_card_owner=True),
            ))
        )

    @pytest.mark.asyncio
    async def test_ok_response__missing_last_paid_ts_and_partner_info(
        self,
        uid,
        trust_payments_client: TrustPaymentsClient,
        payment_method_data,
    ):
        payment_method_data.pop('last_paid_ts')
        payment_method_data.pop('partner_info')

        payment_methods = await trust_payments_client.get_payment_methods(uid)
        assert len(payment_methods) == 1
        assert_that(
            payment_methods[0],
            has_properties(dict(
                id=payment_method_data['id'],
                card_id=payment_method_data['card_id'],
                binding_systems=payment_method_data['binding_systems'],
                orig_uid=payment_method_data['orig_uid'],
                payment_method=payment_method_data['payment_method'],
                system=payment_method_data['system'],
                payment_system=payment_method_data['payment_system'],
                expiration_month=payment_method_data['expiration_month'],
                expiration_year=payment_method_data['expiration_year'],
                card_bank=payment_method_data['card_bank'],
                expired=payment_method_data['expired'],
                account=payment_method_data['account'],
                binding_ts=datetime.fromtimestamp(float(payment_method_data['binding_ts']), tz=timezone.utc),
                last_paid_ts=datetime.fromtimestamp(float(payment_method_data['binding_ts']), tz=timezone.utc),
                partner_info=None,
            ))
        )


class TestPaymentMethodsErrorResponse:
    @dataclass
    class ExpectedResponse:
        payload: dict
        http_status: int

    # для "документирования" что приходит
    RESPONSES = [
        # non existing UID
        ExpectedResponse(
            http_status=401,
            payload={
                "status": "no_auth",
                "status_desc": "passport not found",
                "method": "trust_payments.get_payment_methods",
            },
        ),
        # bad tvm service ticket
        ExpectedResponse(
            http_status=400,
            payload={
                "status": "error",
                "status_code": "invalid_service",
                "status_desc": "invalid_service",
            },
        ),
        # bad service token
        ExpectedResponse(
            http_status=500,
            payload={
                "status": "error",
                "status_code": "technical_error",
                "status_desc": "Server internal error",
                "method": "trust_payments.get_payment_methods",
            }
        )
    ]

    @pytest.fixture(params=RESPONSES)
    def expected_response(self, request) -> ExpectedResponse:
        return request.param

    @pytest.fixture(autouse=True)
    def mock_response(self, yandex_pay_settings, aioresponses_mocker, expected_response: ExpectedResponse):
        return aioresponses_mocker.get(
            f'{yandex_pay_settings.TRUST_PAYMENTS_API_URL}/trust-payments/v2/payment-methods?payment-method=card',
            payload=expected_response.payload,
            status=expected_response.http_status,
            repeat=True,  # fix retries on 5xx codes
        )

    @pytest.mark.asyncio
    async def test_error_response(
        self,
        expected_response: ExpectedResponse,
        trust_payments_client: TrustPaymentsClient,
        uid,
    ):
        with pytest.raises(TrustPaymentsResponseError):
            await trust_payments_client.get_payment_methods(uid)
