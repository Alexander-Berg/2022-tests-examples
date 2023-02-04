import logging
import uuid
from datetime import datetime, timezone

import pytest
import yenv

from hamcrest import assert_that, ends_with, equal_to, has_entries, has_item, has_properties

from billing.yandex_pay.yandex_pay.core.actions.payment_notification import HandlePaymentNotificationAction
from billing.yandex_pay.yandex_pay.core.actions.psp.auth import AuthPSPRequestAction
from billing.yandex_pay.yandex_pay.core.entities.enums import CardNetwork, PaymentNotificationStatus
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.psp_key import PSPKey


@pytest.fixture
def psp():
    return PSP(
        psp_id=uuid.uuid4(),
        psp_external_id='psp_external_id',
        public_key='public-key',
        public_key_signature='public-key-signature',
    )


@pytest.fixture
def psp_key(psp):
    return PSPKey(
        psp_id=psp.psp_id,
        psp_key_id=2,
        key='foobar',
        alg='ES256',
    )


@pytest.fixture(autouse=True)
def mock_auth_psp(mock_action, psp, psp_key):
    return mock_action(AuthPSPRequestAction, (psp, psp_key))


@pytest.fixture(autouse=True)
def mock_handle_payment_notification(mock_action, psp, psp_key):
    return mock_action(HandlePaymentNotificationAction)


@pytest.fixture
def yenv_type_sandbox():
    _type = yenv.type
    yenv.type = 'sandbox'
    yield
    yenv.type = _type


@pytest.mark.asyncio
@pytest.mark.parametrize('card_network', list(CardNetwork))
async def test_returns_200(app, card_network):
    r = await app.post(
        'api/psp/v1/payment_notification',
        json={
            'messageId': '123',
            'status': 'SUCCESS',
            'eventTime': '2020-01-01T00:00:00+00:00',
            'rrn': 'rrn',
            'approvalCode': 'approvalCode',
            'eci': 'eci',
            'cardNetwork': card_network.value,
            'amount': 100,
            'currency': 'RUB',
        }
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        json_body,
        equal_to({
            'code': 200,
            'status': 'success',
            'data': {}
        })
    )


@pytest.mark.asyncio
async def test_calls_action(app, mock_handle_payment_notification, psp):
    await app.post(
        'api/psp/v1/payment_notification',
        json={
            'messageId': '123',
            'status': 'SUCCESS',
            'eventTime': '2020-01-01T00:00:00+00:00',
            'rrn': 'rrn',
            'approvalCode': 'approvalCode',
            'eci': 'eci',
            'amount': 100,
            'currency': 'XTS',
            'reason': 'the reason',
            'reasonCode': 'THE_REASON',
            'paymentId': 'payment-id',
            'recurring': True,
        },
    )

    mock_handle_payment_notification.assert_called_once_with(
        message_id='123',
        status=PaymentNotificationStatus.SUCCESS,
        event_time=datetime(2020, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
        rrn='rrn',
        approval_code='approvalCode',
        eci='eci',
        amount=100,
        currency='XTS',
        reason='the reason',
        reason_code='THE_REASON',
        psp_external_id=psp.psp_external_id,
        payment_id='payment-id',
        recurring=True,
    )


@pytest.mark.asyncio
async def test_null_card_network_allowed(app):
    r = await app.post(
        'api/psp/v1/payment_notification',
        json={
            'messageId': '123',
            'status': 'SUCCESS',
            'eventTime': '2020-01-01T00:00:00+00:00',
            'rrn': 'rrn',
            'approvalCode': 'approvalCode',
            'eci': 'eci',
            'cardNetwork': None,
            'amount': 100,
            'currency': 'XTS',
            'reason': 'the reason',
            'reasonCode': 'THE_REASON',
        }
    )

    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        json_body,
        equal_to({
            'code': 200,
            'status': 'success',
            'data': {},
        })
    )


@pytest.mark.parametrize(
    'body, errors',
    [
        (
            {
                'messageId': '123',
                'status': 'HOLD',
                'eventTime': '2020-01-01T00:00:00+00:00',
                'paymentId': 'xxx',
            },
            {'amount': ['Can not be empty.']},
        ),
        (
            {
                'messageId': '123',
                'status': 'SUCCESS',
                'eventTime': '2020-01-01T00:00:00+00:00',
                'recurring': True,
            },
            {'paymentId': ['Can not be empty.']},
        ),
        (
            {
                'messageId': '123',
                'status': 'SUCCESS',
                'eventTime': '2020-01-01T00:00:00+00:00',
                'amount': -1,
            },
            {'amount': ['Must be at least 0.']},
        ),
    ],
)
@pytest.mark.asyncio
async def test_validation(app, body, errors):
    r = await app.post('api/psp/v1/payment_notification', json=body)

    json_body = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json_body,
        equal_to(
            {
                'code': 400,
                'status': 'fail',
                'data': {'message': 'BAD_REQUEST', 'params': errors},
            }
        ),
    )


@pytest.mark.parametrize(
    'body, errors',
    [
        (
            {
                'messageId': '123',
                'status': 'HOLD',
                'eventTime': '2020-01-01T00:00:00+00:00',
            },
            {'amount': ['Can not be empty.']},
        ),
        (
            {
                'messageId': '123',
                'status': 'SUCCESS',
                'eventTime': '2020-01-01T00:00:00+00:00',
            },
            {'amount': ['Can not be empty.']},
        ),
        (
            {
                'messageId': '123',
                'status': 'FAIL',
                'eventTime': '2020-01-01T00:00:00+00:00',
                'amount': 100,
            },
            {'currency': ['Can not be empty.'], 'reasonCode': ['Can not be empty.']},
        ),
    ],
)
@pytest.mark.asyncio
async def test_sandbox_validation(app, yenv_type_sandbox, body, errors):
    r = await app.post('api/psp/v1/payment_notification', json=body)

    json_body = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json_body,
        equal_to(
            {
                'code': 400,
                'status': 'fail',
                'data': {'message': 'BAD_REQUEST', 'params': errors},
            }
        ),
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'data,expected_log_call_kwargs',
    [
        (b'data', {'body': 'data'}),
        (b'\x80', {'body_base64': 'gA=='}),
    ],
)
async def test_request_parsing_failure_logged(app, caplog, data, expected_log_call_kwargs):
    request_logger_name = 'request_logger'
    caplog.set_level(logging.INFO, logger=request_logger_name)

    r = await app.post(
        'api/psp/v1/payment_notification',
        data=data,
    )

    assert_that(r.status, equal_to(400))
    assert_that(
        await r.json(),
        equal_to(
            {
                'data': {
                    'params': {
                        'messageId': ['Missing data for required field.'],
                        'eventTime': ['Missing data for required field.'],
                        'status': ['Missing data for required field.'],
                    },
                    'message': 'BAD_REQUEST',
                },
                'code': 400,
                'status': 'fail',
            }
        ),
    )

    request_logs = [r for r in caplog.records if r.name == request_logger_name]
    assert_that(
        request_logs,
        has_item(
            has_properties(
                message='Failed to parse PSP request',
                levelno=logging.ERROR,
                _context=has_entries(
                    method='POST',
                    url=ends_with('/api/psp/v1/payment_notification'),
                    **expected_log_call_kwargs,
                ),
            )
        ),
    )
