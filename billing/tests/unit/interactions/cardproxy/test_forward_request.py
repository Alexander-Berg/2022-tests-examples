import base64
import hashlib
import hmac
import json

import pytest
from pay.lib.entities.payment_token import MITInfo

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.interactions.cardproxy import forward_request
from billing.yandex_pay.yandex_pay.interactions.cardproxy.forward_request import _build_uid_hash


@pytest.fixture
def uid() -> int:
    return 1


@pytest.fixture
def accounts_secret_base64() -> str:
    return base64.b64encode(b'some_hmac_secret').decode()


@pytest.fixture
def expiration_month() -> int:
    return 1


@pytest.fixture
def expiration_year() -> int:
    return 2020


@pytest.fixture
def holder_name() -> str:
    return 'Full Holder Name'


@pytest.fixture
def relationship_id(yandex_pay_settings) -> str:
    return yandex_pay_settings.VISA_COMMUNAL_TOKEN_RELATIONSHIP_ID


@pytest.fixture
def expected_visa_enrollment_duck_go_json_body_string(
    uid,
    accounts_secret_base64,
    expiration_month,
    expiration_year,
    relationship_id,
) -> str:
    account_id = base64.b32encode(
        hmac.new(
            key=base64.b64decode(accounts_secret_base64),
            msg=str(uid).encode(),
            digestmod=hashlib.sha256
        ).digest()
    ).decode('utf-8').rstrip('=')[:24]
    # эту функцию протестируем отдельно
    email_hash = _build_uid_hash(uid, accounts_secret_base64)
    body_dict = {
        'account_id': account_id,
        'card': {
            'primaryAccountNumber': '$$(frag:card_number)',
            'panExpirationMonth': str(expiration_month).zfill(2),
            'panExpirationYear': str(expiration_year),
        },
        'locale': 'ru_RU',
        'email_hash': email_hash,
        'pan_source': 'ONFILE',
        'relationship_id': relationship_id,
    }
    return json.dumps(body_dict)


@pytest.fixture
def expected_duck_go_json_body_string(
    uid,
    accounts_secret_base64,
    expiration_month,
    expiration_year,
    holder_name,
) -> str:
    account_id = hmac.new(
        key=base64.b64decode(accounts_secret_base64),
        msg=str(uid).encode(),
        digestmod=hashlib.sha256
    ).hexdigest()
    body_dict = {
        'card_source': 'WALLET',
        'account_id': account_id,
        'card': {
            'primaryAccountNumber': '$$(frag:card_number)',
            'panExpirationMonth': str(expiration_month).zfill(2),
            'panExpirationYear': str(expiration_year),
            'cardholderFullName': holder_name
        }
    }
    return json.dumps(body_dict)


@pytest.fixture
def expected_visa_enrollment_forward_request(
    expected_visa_enrollment_duck_go_json_body_string,
    request_id,
) -> dict:
    return {
        'fragments': [
            {'name': 'card_token',
             'value': '$$(header:X-Diehard-Card-Token)'},
            {'name': 'card_number',
             'value': '$$(func:detokenize:card_token)'},
            {'name': 'duckgo_key_id',
             'value': 'duckgo_key'},
            {'name': 'duckgo_key_value',
             'value': '$$(func:get_secret:duckgo_key_id)'},
            {'name': 'duckgo_key_hash_raw',
             'value': '$$(func:sha256raw:duckgo_key_value)'},
            {'name': 'duckgo_key_hash_hex',
             'value': '$$(func:hexlify:duckgo_key_hash_raw)'},
        ],
        'method': 'POST',
        'uri': 'duckgo',
        'uri-extra': 'VisaEnrollment',
        'body': expected_visa_enrollment_duck_go_json_body_string,
        'headers': {
            'Content-Type': 'application/json',
            'Authorization': 'SharedKeySHA256 $$(frag:duckgo_key_hash_hex)',
            'X-Request-Id': request_id,
        }
    }


@pytest.fixture
def expected_mastercard_enrollment_forward_request(
    expected_duck_go_json_body_string,
    request_id,
) -> dict:
    return {
        'fragments': [
            {'name': 'card_token',
             'value': '$$(header:X-Diehard-Card-Token)'},
            {'name': 'card_number',
             'value': '$$(func:detokenize:card_token)'},
            {'name': 'duckgo_key_id',
             'value': 'duckgo_key'},
            {'name': 'duckgo_key_value',
             'value': '$$(func:get_secret:duckgo_key_id)'},
            {'name': 'duckgo_key_hash_raw',
             'value': '$$(func:sha256raw:duckgo_key_value)'},
            {'name': 'duckgo_key_hash_hex',
             'value': '$$(func:hexlify:duckgo_key_hash_raw)'},
        ],
        'method': 'POST',
        'uri': 'duckgo',
        'uri-extra': 'MastercardEnrollment',
        'body': expected_duck_go_json_body_string,
        'headers': {
            'Content-Type': 'application/json',
            'Authorization': 'SharedKeySHA256 $$(frag:duckgo_key_hash_hex)',
            'X-Request-Id': request_id,
        }
    }


def test_can_build_mastercard_enrollment_request_body(
    uid,
    accounts_secret_base64,
    expiration_month,
    expiration_year,
    holder_name,
    request_id,
    expected_mastercard_enrollment_forward_request,
):
    request_body = forward_request.build_mastercard_enrollment_forward_request_body(
        uid=uid,
        accounts_secret_base64=accounts_secret_base64,
        expiration_month=expiration_month,
        expiration_year=expiration_year,
        holder_name=holder_name,
        request_id=request_id,
    )

    assert_that(request_body, equal_to(expected_mastercard_enrollment_forward_request))


def test_can_build_pan_checkout_request_body():
    """
    Функция построения тела запроса в CardProxy ForwardRequest
    строит корректное тело на основе тестовых данных.
    """
    body = forward_request.build_pan_checkout_forward_request_body(
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
        request_id='request_id_123',
    )

    expected_body = {
        'fragments': [
            {'name': 'card_token', 'value': '$$(header:X-Diehard-Card-Token)'},
            {'name': 'card_number', 'value': '$$(func:detokenize:card_token)'},
            {'name': 'duckgo_key_id', 'value': 'duckgo_key'},
            {'name': 'duckgo_key_value', 'value': '$$(func:get_secret:duckgo_key_id)'},
            {'name': 'duckgo_key_hash_raw', 'value': '$$(func:sha256raw:duckgo_key_value)'},
            {'name': 'duckgo_key_hash_hex', 'value': '$$(func:hexlify:duckgo_key_hash_raw)'},
        ],
        'method': 'POST',
        'uri': 'duckgo',
        'uri-extra': 'PANCheckout',
        'headers': {
            'Content-Type': 'application/json',
            'Authorization': 'SharedKeySHA256 $$(frag:duckgo_key_hash_hex)',
            'X-Request-Id': 'request_id_123',
        },
        'body': {
            'card': {
                'primary_account_number': '$$(frag:card_number)',
                'pan_expiration_month': 1,
                'pan_expiration_year': 2022
            },
            'recipient_id': 'payture',
            'recipient_pub_key': 'PUBLIC_KEY',
            'recipient_pub_key_signature': 'KEY_SIGNATURE',
            'gateway_merchant_id': 'payture-merchant',
            'message_expiration': 2000000000,
            'message_id': '1234567',
            'transaction_info': {
                'currency': 'RUB',
                'amount': 100,
            },
            'mit_info': {
                'deferred': True,
                'recurring': False,
            },
        }
    }

    body['body'] = json.loads(body['body'])

    assert body == expected_body


def test_can_build_visa_enrollment_forward_request_body(
    uid,
    accounts_secret_base64,
    expiration_month,
    expiration_year,
    holder_name,
    request_id,
    expected_visa_enrollment_forward_request,
    relationship_id,
):
    request_body = forward_request.build_visa_enrollment_forward_request_body(
        uid=uid,
        expiration_month=expiration_month,
        expiration_year=expiration_year,
        request_id=request_id,
        secret_base64=accounts_secret_base64,
        relationship_id=relationship_id,
    )

    assert_that(request_body, equal_to(expected_visa_enrollment_forward_request))


def test_buid_uid_hash_should_work():
    h = _build_uid_hash(12345, 'c2VjcmV0')
    assert_that(h, equal_to('RvM1WRFpsY_6Zj6X5aTvG24fEdCrx_GmtH6HmtWy41Y'))
