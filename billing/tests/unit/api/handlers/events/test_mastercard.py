import json
import logging
from copy import deepcopy

import pytest

from hamcrest import assert_that, ends_with, equal_to, has_entries, has_item, has_properties

from billing.yandex_pay.yandex_pay.api.schemas.events.mastercard import DigitalCardUpdateNotificationSchema
from billing.yandex_pay.yandex_pay.core.entities.enums import MasterCardTokenStatus, TSPType


@pytest.fixture
def mock_create_notification_task_action(mocker):
    mock_run = mocker.AsyncMock()
    mock_action_cls = mocker.patch(
        'billing.yandex_pay.yandex_pay.api.handlers.events.'
        'mastercard.CreateNotificationTaskAction'
    )
    mock_action_cls.return_value.run = mock_run
    return mock_action_cls


@pytest.fixture
def notification_status(request):
    status = getattr(request, 'param', MasterCardTokenStatus.ACTIVE)
    return getattr(status, 'value', status)


@pytest.fixture
def fake_notification(notification_status):
    return {
        "digitalCardUpdateNotifications": [
            {
                "eventTimeStamp": "1613662929009",
                "maskedCard": {
                    "srcDigitalCardId": "00000000-0000-0000-0000-000000000000",
                    "srcPaymentCardId": None,
                    "panBin": "545503",
                    "panLastFour": "6828",
                    "tokenBinRange": "123456",
                    "tokenLastFour": "0123",
                    "digitalCardData": {
                        "status": notification_status,
                        "descriptorName": "Example Bank Product Configuration",
                        "artUri": "https://example.test/fake.png",
                        "artHeight": None,
                        "artWidth": None,
                        "pendingEvents": None,
                    },
                    "panExpirationMonth": "04",
                    "panExpirationYear": "2023",
                    "paymentCardDescriptor": "MasterCard",
                    "paymentCardType": "CREDIT",
                    "digitalCardFeatures": None,
                    "countryCode": "933",
                    "maskedBillingAddress": None,
                    "dcf": None,
                    "serviceId": "COF_CP_GOO_1",
                    "paymentAccountReference": "500150F5DE22SND132Y6PR32AR5HB",
                    "dateOfCardCreated": "2020-02-24T11:32:32.060Z",
                    "dateOfCardLastUsed": None,
                }
            }
        ],
        "key_not_in_their_model": "does_not_break_things",
    }


class TestMasterCardCardNotificationHandler:
    INVALID_STATUS = 'invalid_status'

    def _get_error_payload(self, idx: int):
        message = (
            f"Unrecognized token status '{self.INVALID_STATUS}' in notification "
            f"at index: {idx}"
        )
        return {
            "status": 400,
            "errordetail": [
                {
                    "reason": "INVALID_VALUE",
                    "sourceType": "BODY",
                    "message": message,
                    "source": "maskedCard.digitalCardData.status",
                }
            ],
            "message": "Cannot process some notifications due to unrecognized token status",
            "reason": "INVALID_ARGUMENT",
        }

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'notification_status',
        MasterCardTokenStatus,
        indirect=True,
    )
    async def test_post_notification(
        self,
        app,
        mock_create_notification_task_action,
        fake_notification,
        product_logs,
    ):
        response = await app.post(
            'events/mastercard/notifications/card', json=fake_notification
        )
        json_body = await response.json()

        assert_that(response.status, equal_to(204))
        assert_that(json_body, equal_to(None))

        expected_notification = DigitalCardUpdateNotificationSchema().load(
            fake_notification['digitalCardUpdateNotifications'][0]
        ).data
        mock_create_notification_task_action.assert_called_once_with(
            notification=expected_notification
        )
        mock_create_notification_task_action.return_value.run.assert_awaited_once_with()

        [product_log] = product_logs()
        assert_that(
            product_log,
            has_properties(
                message='TSP status update notification',
                _context=has_entries(
                    tsp=TSPType.MASTERCARD,
                    body=fake_notification,
                )
            )
        )

    @pytest.mark.asyncio
    async def test_post_empty_notification_list(
        self, app, mock_create_notification_task_action
    ):
        response = await app.post(
            'events/mastercard/notifications/card',
            json={'digitalCardUpdateNotifications': []},
        )
        json_body = await response.json()

        assert_that(response.status, equal_to(204))
        assert_that(json_body, equal_to(None))

        mock_create_notification_task_action.assert_not_called()

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'notification_status', [INVALID_STATUS], indirect=True,
    )
    async def test_post_malformed_notification(
        self,
        app,
        mock_create_notification_task_action,
        fake_notification,
        notification_status,
    ):
        response = await app.post(
            'events/mastercard/notifications/card', json=fake_notification
        )
        json_body = await response.json()

        expected_error = {
            "status": 400,
            "errordetail": [
                {
                    "reason": "INVALID_VALUE",
                    "sourceType": "BODY",
                    "message": '{"0":{"maskedCard":{"digitalCardData":'
                               '{"status":["Invalid enum value invalid_status"]}}}}',
                    "source": "digitalCardUpdateNotifications",
                }
            ],
            "message": "Bad Request",
            "reason": "INVALID_ARGUMENT",
        }
        assert_that(response.status, equal_to(400))
        assert_that(json_body, equal_to(expected_error))

        mock_create_notification_task_action.assert_not_called()

    @pytest.mark.asyncio
    async def test_post_valid_and_invalid_notifications_in_single_batch(
        self,
        app,
        mock_create_notification_task_action,
        fake_notification,
    ):
        valid_notification = fake_notification['digitalCardUpdateNotifications'][0]
        malformed_notification = deepcopy(valid_notification)
        malformed_notification['maskedCard']['digitalCardData']['status'] = self.INVALID_STATUS
        fake_notification['digitalCardUpdateNotifications'].append(malformed_notification)

        response = await app.post(
            'events/mastercard/notifications/card', json=fake_notification
        )
        json_body = await response.json()

        expected_error = {
            "status": 400,
            "errordetail": [
                {
                    "reason": "INVALID_VALUE",
                    "sourceType": "BODY",
                    "message": '{"1":{"maskedCard":{"digitalCardData":'
                               '{"status":["Invalid enum value invalid_status"]}}}}',
                    "source": "digitalCardUpdateNotifications",
                }
            ],
            "message": "Bad Request",
            "reason": "INVALID_ARGUMENT",
        }
        assert_that(response.status, equal_to(400))
        assert_that(json_body, equal_to(expected_error))

        # valid notification was still processed
        mock_create_notification_task_action.assert_not_called()
        mock_create_notification_task_action.return_value.run.assert_not_awaited()

    @pytest.mark.asyncio
    async def test_invalid_payload_schema(
        self, app, mock_create_notification_task_action, fake_notification
    ):
        valid_notification = fake_notification['digitalCardUpdateNotifications'][0]
        malformed_notification = deepcopy(valid_notification)
        del malformed_notification['eventTimeStamp']
        fake_notification['digitalCardUpdateNotifications'].append(malformed_notification)

        response = await app.post(
            'events/mastercard/notifications/card', json=fake_notification
        )
        assert_that(response.status, equal_to(400))

        json_body = await response.json()
        for each in json_body['errordetail']:
            each['message'] = json.loads(each['message'])

        error_message = {"1": {"eventTimeStamp": ["Missing data for required field."]}}
        expected_error = {
            "status": 400,
            "errordetail": [
                {
                    "reason": "INVALID_VALUE",
                    "sourceType": "BODY",
                    "message": error_message,
                    "source": "digitalCardUpdateNotifications",
                }
            ],
            "message": "Bad Request",
            "reason": "INVALID_ARGUMENT",
        }
        assert_that(json_body, equal_to(expected_error))

        mock_create_notification_task_action.assert_not_called()

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'data,expected_log_call_kwargs',
        [
            (b'data', {'body': 'data'}),
            (b'\x80', {'body_base64': 'gA=='}),
        ]
    )
    async def test_invalid_request_logged(
        self, app, caplog, data, expected_log_call_kwargs
    ):
        request_logger_name = 'request_logger'
        caplog.set_level(logging.INFO, logger=request_logger_name)
        headers = {'X-Request-Id': 'fake_request_id', 'X-Secret': 'should_not_be_logged'}

        response = await app.post(
            'events/mastercard/notifications/card', data=data, headers=headers
        )

        assert_that(response.status, equal_to(400))

        request_logs = [r for r in caplog.records if r.name == request_logger_name]
        assert_that(
            request_logs,
            has_item(
                has_properties(
                    message='Failed to parse notification request',
                    levelno=logging.ERROR,
                    _context=has_entries(
                        tsp=TSPType.MASTERCARD,
                        method='POST',
                        url=ends_with('/events/mastercard/notifications/card'),
                        request_headers={'X-Request-Id': 'fake_request_id'},
                        **expected_log_call_kwargs
                    ),
                )
            )
        )
