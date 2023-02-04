from datetime import datetime, timezone
from typing import Any, Dict

import pytest

from hamcrest import assert_that, equal_to, none

from billing.yandex_pay.yandex_pay.interactions.cardproxy import map_mastercard_response_json_to_class


@pytest.fixture
def mastercard_response() -> Dict[str, Any]:
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
            }
        },
        'maskedConsumer': {
            'srcConsumerId': '33c9a0b0-b7a3-4075-ad4e-47f82366dc29',
            'maskedEmailAddress': 'email@email.com',
            'maskedConsumerIdentity': {
                'identityType': 'EXTERNAL_ACCOUNT_ID',
                'maskedIdentityValue': 'fake_account_id'
            },
            'maskedMobileNumber': {},
            'dateConsumerAdded': '2020-11-20T12:05:28.051Z',
            'status': 'ACTIVE'
        }
    }


@pytest.fixture
def expected_enrollment_metadata() -> Dict[str, Any]:
    return {
        'masked_card': {
            'pan_bin': '531222',
            'pan_last_four': '3332',
            'pan_expiration_month': '05',
            'pan_expiration_year': '2025',
            'payment_card_descriptor': 'some_descriptor',
            'payment_card_type': 'some_type',
            'payment_account_reference': '6421AKEROU2QIQWC03DPN3OMOLCM3',
            'date_of_card_created': '2020-11-20 12:05:28.051000+00:00',
            'digital_card_data': {
                'descriptor_name': 'Example Bank Product Configuration',
                'art_uri': 'https://sbx.assets.mastercard.com/c911d734-2f4b-4326-8363-afb0ffe2bc2b.png',
            }
        },
        'masked_consumer': {
            'src_consumer_id': '33c9a0b0-b7a3-4075-ad4e-47f82366dc29',
            'masked_email_address': 'email@email.com',
            'masked_consumer_identity': {
                'identity_type': 'EXTERNAL_ACCOUNT_ID',
                'masked_identity_value': 'fake_account_id'
            },
            'date_consumer_added': '2020-11-20 12:05:28.051000+00:00',
            'status': 'ACTIVE',
        }
    }


def test_mastercard_enrollment_get_metadata(
    mastercard_response, expected_enrollment_metadata
):
    mastercard_enrollment_entity = map_mastercard_response_json_to_class(mastercard_response)

    assert_that(
        mastercard_enrollment_entity.get_metadata(),
        equal_to(expected_enrollment_metadata)
    )


def test_mastercard_enrollment_get_pan_expiration(mastercard_response):
    mastercard_enrollment_entity = map_mastercard_response_json_to_class(mastercard_response)
    expected = datetime(2025, 5, 31, tzinfo=timezone.utc)

    assert_that(
        mastercard_enrollment_entity.masked_card.get_pan_expiration_date(),
        equal_to(expected)
    )


@pytest.mark.parametrize('key_to_delete', ['panExpirationMonth', 'panExpirationYear'])
def test_mastercard_enrollment_pan_expiration_unknown(key_to_delete, mastercard_response):
    del mastercard_response['maskedCard'][key_to_delete]
    mastercard_enrollment_entity = map_mastercard_response_json_to_class(mastercard_response)

    assert_that(
        mastercard_enrollment_entity.masked_card.get_pan_expiration_date(),
        none()
    )
