import re
from typing import Optional

import pytest

from hamcrest import all_of, assert_that, equal_to, has_entries, has_item, has_properties, has_property

from billing.yandex_pay.yandex_pay.core.entities.enums import VisaTokenStatus, VisaTokenStatusUpdateReason
from billing.yandex_pay.yandex_pay.interactions import VisaClient
from billing.yandex_pay.yandex_pay.interactions.visa import (
    create_visa_card_metadata_result, create_visa_content_result, create_visa_token_status_result
)
from billing.yandex_pay.yandex_pay.interactions.visa.exceptions import BaseVisaInteractionError


@pytest.fixture
async def visa_client(create_client):
    client = create_client(VisaClient)
    yield client
    await client.close()


@pytest.fixture
def provisioned_token_id():
    return "token-id"


@pytest.fixture
def enrollment_id():
    return 'some-enrollment-id'


@pytest.fixture
def guid():
    return 'some-guid'


@pytest.fixture
def visa_card_metadata_response_body():
    return {
        "paymentInstrument": {
            "last4": "0507",
            "expirationDate": {
                "ignore02field": "FA238F20",
                "month": "12",
                "year": "2022"
            },
            "cvv2PrintedInd": "Y",
            "ignore01field": "E100786F",
            "expDatePrintedInd": "Y",
            "paymentAccountReference": "V0010013020294400836524608503",
            "enabledServices": {
                "merchantPresentedQR": "N"
            }
        },
        "vPanEnrollmentID": "7d6a589ca81a1bf7b15d1307413daa01",
        "tokens": [
            {
                "vProvisionedTokenID": "2af631b105f5aa692c8a13f635173401",
                "tokenStatus": "ACTIVE"
            }
        ],
        "cardMetaData": {
            "backgroundColor": "0x000000",
            "foregroundColor": "0x7dffff",
            "labelColor": "0x000000",
            "contactWebsite": "https://www.visatest.com",
            "contactEmail": "test@visa.com",
            "contactNumber": "1800768999",
            "contactName": "visaTest",
            "privacyPolicyURL": "https://prefix.html",
            "termsAndConditionsURL": "https://terms.html",
            "shortDescription": "FNDB Visa Classic Card",
            "longDescription": "Visa Test Only Card",
            "cardData": [
                {
                    "guid": "a0b72f0e60284f4f9809c9375557a77c",
                    "contentType": "cardSymbol",
                    "content": [
                        {
                            "mimeType": "image/png",
                            "width": "100",
                            "height": "100"
                        }
                    ]
                },
                {
                    "guid": "3883d6a112284123b8b23ec595670eb7",
                    "contentType": "digitalCardArt",
                    "content": [
                        {
                            "mimeType": "image/png",
                            "width": "1536",
                            "height": "969"
                        }
                    ]
                }
            ],
        }
    }


@pytest.fixture
def visa_content_response_body():
    return {
        "altText": "cardSymbol",
        "contentType": "cardSymbol",
        "content": [
            {
                "mimeType": "image/png",
                "width": "100",
                "height": "100",
                "encodedData": "iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAIAAAD/..."
            }
        ]
    }


@pytest.fixture
def visa_token_status_response_body():
    return {
        "deviceBindingInfoList":
            [{
                "clientDeviceID": "9C4...72",
                "deviceName": "...",
                "status": "CHALLENGED"
            }],
        "tokenInfo": {
            "tokenStatus": "ACTIVE",
            "ignore01field": "D2553045",
            "expirationDate": {
                "year": "2022",
                "month": "12"
            }}
    }


@pytest.fixture
def visa_token_status_response_headers():
    return {
        'X-Correlation-ID': 'fake_correlation_id',
        'X-VTS-Correlation-ID': 'fake_vts_correlation_id',
        'X-VTS-Response-ID': 'fake_vts_response_id',
    }


@pytest.fixture
def zora_url_pattern(yandex_pay_settings):
    return re.compile(f'^{yandex_pay_settings.ZORA_URL}.*')


def test_visa_card_metadata_response_entity_creation(visa_card_metadata_response_body):
    created = create_visa_card_metadata_result(visa_card_metadata_response_body)

    assert_that(
        created,
        all_of(
            has_property("enrollment_id", "7d6a589ca81a1bf7b15d1307413daa01"),
            has_property(
                'payment_instrument',
                all_of(
                    has_property('last4', '0507'),
                    has_property(
                        'expiration_date',
                        all_of(
                            has_property('month', 12),
                            has_property('year', 2022),
                        )
                    ),
                    has_property('payment_account_reference', 'V0010013020294400836524608503'),
                )
            ),
            has_property(
                'tokens',
                has_item(
                    has_properties({
                        'provisioned_id': '2af631b105f5aa692c8a13f635173401',
                        'status': VisaTokenStatus.ACTIVE,
                    })
                )
            ),
            has_property(
                'card_metadata',
                all_of(
                    has_property("background_color", "0x000000"),
                    has_property("foreground_color", "0x7dffff"),
                    has_property("label_color", "0x000000"),
                    has_property("contact_website", "https://www.visatest.com"),
                    has_property("contact_email", "test@visa.com"),
                    has_property("contact_number", "1800768999"),
                    has_property("contact_name", "visaTest"),
                    has_property("privacy_policy_url", "https://prefix.html"),
                    has_property("terms_and_conditions_url", "https://terms.html"),
                    has_property("short_description", "FNDB Visa Classic Card"),
                    has_property("long_description", "Visa Test Only Card"),
                    has_property(
                        'card_data',
                        all_of(
                            has_item(
                                all_of(
                                    has_property('guid', 'a0b72f0e60284f4f9809c9375557a77c'),
                                    has_property('content_type', 'cardSymbol'),
                                    has_property(
                                        'content',
                                        has_item(
                                            has_properties({
                                                "mime_type": "image/png",
                                                "width": 100,
                                                "height": 100,
                                            })
                                        )
                                    )
                                )
                            ),
                            has_item(
                                all_of(
                                    has_property('guid', '3883d6a112284123b8b23ec595670eb7'),
                                    has_property('content_type', 'digitalCardArt'),
                                    has_property(
                                        'content',
                                        has_item(
                                            has_properties({
                                                "mime_type": "image/png",
                                                "width": 1536,
                                                "height": 969
                                            })
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )


def test_visa_content_response_entity_creation(visa_content_response_body):
    created = create_visa_content_result(visa_content_response_body)

    assert_that(
        created,
        all_of(
            has_property("alt_text", "cardSymbol"),
            has_property("content_type", "cardSymbol"),
            has_property(
                "content",
                has_item(
                    all_of(
                        has_property("mime_type", "image/png"),
                        has_property("width", 100),
                        has_property("height", 100),
                        has_property("encoded_data", 'iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAIAAAD/...'),
                    )
                )
            )
        )
    )


def test_visa_token_status_response_entity_creation(visa_token_status_response_body):
    created = create_visa_token_status_result(visa_token_status_response_body)
    assert_that(
        created,
        all_of(
            has_property(
                "token_info",
                all_of(
                    has_property("status", VisaTokenStatus.ACTIVE),
                    has_property(
                        'expiration_date',
                        all_of(
                            has_property('month', 12),
                            has_property('year', 2022),
                        )
                    ),
                )
            )
        )
    )


class TestVisaCardMetadataInteractionMethodWorks:
    @pytest.fixture(autouse=True)
    def mock_visa_card_metadata_response(
        self,
        zora_url_pattern,
        aioresponses_mocker,
        visa_card_metadata_response_body,
        enrollment_id
    ):
        return aioresponses_mocker.get(
            zora_url_pattern,
            status=200,
            payload=visa_card_metadata_response_body,
        )

    @pytest.fixture
    def expected_visa_card_metadata_result(self, visa_card_metadata_response_body):
        return create_visa_card_metadata_result(visa_card_metadata_response_body)

    @pytest.mark.asyncio
    async def test_visa_card_metadata_normal_response(
        self,
        visa_client: VisaClient,
        enrollment_id,
        expected_visa_card_metadata_result
    ):
        visa_card_metadata_reult = await visa_client.card_metadata(enrollment_id)
        assert visa_card_metadata_reult == expected_visa_card_metadata_result


class TestVisaContentInteractionMethosWorks:
    @pytest.fixture(autouse=True)
    def mock_visa_content_response(
        self,
        zora_url_pattern,
        aioresponses_mocker,
        visa_content_response_body,
        guid
    ):
        return aioresponses_mocker.get(
            zora_url_pattern,
            status=200,
            payload=visa_content_response_body,
        )

    @pytest.fixture
    def expected_visa_content_result(self, visa_content_response_body):
        return create_visa_content_result(visa_content_response_body)

    @pytest.mark.asyncio
    async def test_visa_card_get_content_normal_response(
        self,
        visa_client: VisaClient,
        guid,
        expected_visa_content_result
    ):
        visa_card_metadata_reult = await visa_client.get_content(guid)
        assert visa_card_metadata_reult == expected_visa_content_result


class TestVisaChangeTokenStatusInteractionMethodsWorks:
    @pytest.mark.parametrize(
        'reason', [r for r in VisaTokenStatusUpdateReason]
    )
    @pytest.mark.parametrize(
        'description', [None, 'some description']
    )
    @pytest.mark.parametrize(
        'status,action',
        [
            (VisaTokenStatus.ACTIVE, 'resume'),
            (VisaTokenStatus.SUSPENDED, 'suspend'),
            (VisaTokenStatus.DELETED, 'delete'),
        ]
    )
    @pytest.mark.asyncio
    async def test_visa_card_get_content_normal_response(
        self,
        visa_client: VisaClient,
        provisioned_token_id: str,
        status: VisaTokenStatus,
        reason: VisaTokenStatusUpdateReason,
        description: Optional[str],
        action: str,
        aioresponses_mocker,
        zora_url_pattern,
    ):
        mock_visa_change_token_status_response = aioresponses_mocker.put(
            re.compile(zora_url_pattern),
            status=200,
            payload={},
        )

        await visa_client.change_token_status(
            provisioned_token_id=provisioned_token_id,
            status=status,
            update_reason=reason,
            description=description
        )

        mock_visa_change_token_status_response.assert_called_once()
        a, call_kwargs = mock_visa_change_token_status_response.call_args
        expected_json_args = {
            'updateReason': {
                'reasonCode': reason.value,
                'reasonDesc': description
            }
        }

        assert_that(call_kwargs['json'], equal_to(expected_json_args))

    @pytest.mark.asyncio
    async def test_raises_on_unexpected_status(
        self,
        visa_client: VisaClient,
        provisioned_token_id: str
    ):
        with pytest.raises(ValueError):
            await visa_client.change_token_status(
                provisioned_token_id=provisioned_token_id,
                status=VisaTokenStatus.INACTIVE,
                update_reason=VisaTokenStatusUpdateReason.CUSTOMER_CONFIRMED,
                description='desc'
            )


class TestVisaTokenStatusInteractionMethodsWorks:
    @pytest.fixture(autouse=True)
    def mock_visa_content_response(
        self,
        zora_url_pattern,
        aioresponses_mocker,
        visa_token_status_response_body,
        visa_token_status_response_headers,
        provisioned_token_id,
    ):
        return aioresponses_mocker.get(
            re.compile(zora_url_pattern),
            status=200,
            payload=visa_token_status_response_body,
            headers=visa_token_status_response_headers,
        )

    @pytest.fixture
    def expected_visa_token_status_result(self, visa_token_status_response_body):
        return create_visa_token_status_result(visa_token_status_response_body)

    @pytest.mark.asyncio
    async def test_visa_card_get_content_normal_response(
        self,
        visa_client: VisaClient,
        provisioned_token_id,
        expected_visa_token_status_result,
        visa_token_status_response_headers,
    ):
        visa_token_status_result = await visa_client.get_token_status(provisioned_token_id)
        assert visa_token_status_result == expected_visa_token_status_result

        ctx = visa_client.logger.get_context()
        assert_that(
            ctx,
            has_entries(response_headers=has_entries(visa_token_status_response_headers))
        )


class TestRaisesBaseVisaInteractionErrorIfResponseIsNotOK:
    BAD_RESPONSES = [
        {
            'errorResponse': {
                'message': 'Missing or invalid value for Pan ReferenceID.',
                'reason': 'cardVerificationFailed',
                'details': [
                    {
                        'location': 'panReferenceId',
                        'message': 'Should be a numeric value'
                    }
                ]
            }
        },
        {
            'errorResponse': {
                'message': 'This reason is not listed',
                'reason': 'unknown_reason',
                'details': [
                    {
                        'some': 'error',
                        'message': 'some message'
                    }
                ]
            }
        },
    ]

    @pytest.fixture(params=BAD_RESPONSES)
    def visa_card_metadata_error_body(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    def mock_visa_card_metadata_response(
        self,
        zora_url_pattern,
        aioresponses_mocker,
        visa_card_metadata_error_body,
    ):
        aioresponses_mocker.get(
            re.compile(zora_url_pattern),
            status=400,
            payload=visa_card_metadata_error_body,
        )

    @pytest.mark.asyncio
    async def test_raises_base_visa_interaction_error(
        self,
        visa_client: VisaClient,
        enrollment_id: str
    ):
        with pytest.raises(BaseVisaInteractionError):
            await visa_client.card_metadata(enrollment_id)
