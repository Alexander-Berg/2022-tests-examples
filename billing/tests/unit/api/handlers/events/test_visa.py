import json
import logging

import pytest

from sendr_utils import alist, utcnow

from hamcrest import assert_that, contains_string, ends_with, equal_to, has_entries, has_item, has_properties

from billing.yandex_pay.yandex_pay.api.handlers.events.visa import (
    VisaCardMetadataUpdateNotificationHandler, VisaTokenStatusUpdateNotificationHandler
)
from billing.yandex_pay.yandex_pay.api.schemas.events.visa import VisaTokenStatusUpdateType
from billing.yandex_pay.yandex_pay.core.actions.events.visa import (
    VisaProcessUpdateCardMetaDataNotificationAction, VisaUpdateTokenStatusAction
)
from billing.yandex_pay.yandex_pay.core.entities.enums import TaskType, TSPType
from billing.yandex_pay.yandex_pay.interactions import DuckGoClient
from billing.yandex_pay.yandex_pay.interactions.duckgo import DuckGoInteractionError


@pytest.fixture
def provisioned_token_id():
    return 'fake-provisioned-token-id'


@pytest.fixture
def pan_enrollment_id():
    return 'fake-provisioned-token-id'


@pytest.fixture
def api_key():
    return 'api-key'


@pytest.fixture
def mock_duckgo_visa_verify_request(mocker):
    mock_run = mocker.AsyncMock()
    mock_action_cls = mocker.patch(
        'billing.yandex_pay.yandex_pay.api.handlers.'
        'mixins.visa_verify_request.VisaVerifyRequestAction'
    )
    mock_action_cls.return_value.run = mock_run
    return mock_action_cls


class TestVisaTokenStatusUpdateNotificationHandler:
    INVALID_STATUS = 'invalid_status'

    @pytest.fixture
    def time_now(self):
        return utcnow().replace(microsecond=0)

    @pytest.fixture
    def visa_token_status_update_notification(self, time_now, provisioned_token_id):
        return {
            'date': int(time_now.timestamp()),
            'vProvisionedTokenID': provisioned_token_id,
        }

    @pytest.fixture
    def mock_visa_token_status_update_action(self, mocker):
        mock_run = mocker.AsyncMock()
        mock_action_cls = mocker.patch(
            'billing.yandex_pay.yandex_pay.api.handlers.events.'
            'visa.VisaUpdateTokenStatusAction'
        )
        mock_action_cls.return_value.run_async = mock_run
        return mock_action_cls

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'event_type',
        [VisaTokenStatusUpdateType.TOKEN_STATUS_UPDATED]
    )
    async def test_should_create_async_task(
        self,
        app,
        provisioned_token_id,
        api_key,
        event_type,
        visa_token_status_update_notification,
        mock_duckgo_visa_verify_request,
        time_now,
        mocked_logger,
        mocker,
        storage,
    ):
        mocker.patch.object(
            VisaTokenStatusUpdateNotificationHandler,
            'logger',
            mocker.PropertyMock(return_value=mocked_logger)
        )

        response = await app.post(
            'events/visa/v1/provisionedToken',
            params={
                'apiKey': api_key,
                'eventType': event_type.value
            },
            json=visa_token_status_update_notification
        )

        assert_that(response.status, equal_to(200))
        json_body = await response.json()
        assert_that(json_body, equal_to({}))

        created_tasks = await alist(storage.task.find())
        assert_that(len(created_tasks), equal_to(1))

        created_task = created_tasks[0]
        expected_params = {
            'max_retries': 20,
            'action_kwargs': {
                'event_timestamp': time_now.isoformat(sep=' '),
                'provisioned_token_id': visa_token_status_update_notification['vProvisionedTokenID'],
            }
        }

        assert_that(
            created_task,
            has_properties(
                params=expected_params,
                task_type=TaskType.RUN_ACTION,
                action_name=VisaUpdateTokenStatusAction.action_name
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'event_type',
        VisaTokenStatusUpdateType
    )
    async def test_update_token_status_notification(
        self,
        app,
        provisioned_token_id,
        api_key,
        event_type,
        mock_visa_token_status_update_action,
        visa_token_status_update_notification,
        mock_duckgo_visa_verify_request,
        time_now,
        product_logs,
    ):
        response = await app.post(
            'events/visa/v1/provisionedToken',
            params={
                'apiKey': api_key,
                'eventType': event_type.value
            },
            json=visa_token_status_update_notification
        )

        assert_that(response.status, equal_to(200))
        json_body = await response.json()
        assert_that(json_body, equal_to({}))

        mock_duckgo_visa_verify_request.assert_called_once()

        [log] = product_logs()
        assert_that(
            log,
            has_properties(
                message='TSP status update notification',
                _context=has_entries(
                    tsp=TSPType.VISA,
                    body=visa_token_status_update_notification,
                    event_type=event_type,
                )
            )
        )

        if event_type == VisaTokenStatusUpdateType.TOKEN_STATUS_UPDATED:
            mock_visa_token_status_update_action.assert_called_once_with(
                provisioned_token_id=provisioned_token_id, event_timestamp=time_now
            )
            mock_visa_token_status_update_action.return_value.run_async.assert_awaited_once_with()
        else:
            mock_visa_token_status_update_action.assert_not_called()
            mock_visa_token_status_update_action.return_value.run_async.assert_not_awaited()

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'event_type',
        [INVALID_STATUS]
    )
    async def test_update_token_status_notification_with_invalid_type(
        self,
        app,
        provisioned_token_id,
        api_key,
        event_type,
        mock_visa_token_status_update_action,
        visa_token_status_update_notification,
        mock_duckgo_visa_verify_request,
        caplog,
    ):
        request_logger_name = 'request_logger'
        caplog.set_level(logging.INFO, logger=request_logger_name)

        response = await app.post(
            f'events/visa/v1/provisionedToken?apiKey={api_key}&eventType={event_type}',
            json=visa_token_status_update_notification
        )

        json_body = await response.json()

        expected_error = {
            'status': 500,
            'reason': 'INVALID_ARGUMENT',
            'message': 'Bad Request',
            'errordetail': [
                {
                    'reason': 'INVALID_VALUE',
                    'sourceType': 'BODY',
                    'message': '["Invalid enum value invalid_status"]',
                    'source': 'eventType'
                }
            ]

        }
        assert_that(response.status, equal_to(500))
        assert_that(json_body, equal_to(expected_error))

        mock_visa_token_status_update_action.assert_not_called()

        assert_that(
            caplog.records,
            has_item(
                has_properties(
                    message='Failed to parse notification request',
                    name=request_logger_name,
                    _context=has_entries(
                        tsp=TSPType.VISA,
                        url=contains_string('events/visa/v1/provisionedToken'),
                        body=json.dumps(visa_token_status_update_notification),
                    ),
                )
            )
        )

    @pytest.mark.asyncio
    async def test_update_token_status_notification_with_mailformed_schema(
        self,
        app,
        api_key,
        mock_visa_token_status_update_action,
        visa_token_status_update_notification,
        mock_duckgo_visa_verify_request,
        mocked_logger,
    ):
        del visa_token_status_update_notification['vProvisionedTokenID']

        response = await app.post(
            f'events/visa/v1/provisionedToken?apiKey={api_key}'
            f'&eventType={VisaTokenStatusUpdateType.TOKEN_STATUS_UPDATED}',
            json=visa_token_status_update_notification
        )

        json_body = await response.json()

        expected_error = {
            'status': 500,
            'message': 'Bad Request',
            'errordetail': [
                {
                    'reason': 'INVALID_VALUE',
                    'sourceType': 'BODY',
                    'message': '["Missing data for required field."]',
                    'source': 'vProvisionedTokenID'
                }
            ],
            'reason': 'INVALID_ARGUMENT'
        }

        assert_that(response.status, equal_to(500))
        assert_that(json_body, equal_to(expected_error))

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'data,expected_log_call_kwargs',
        [
            (b'data', {'body': 'data'}),
            (b'\x80', {'body_base64': 'gA=='}),
        ]
    )
    async def test_update_token_status_notification_invalid_request_logged(
        self,
        app,
        data,
        expected_log_call_kwargs,
        mock_duckgo_visa_verify_request,
        caplog,
    ):
        request_logger_name = 'request_logger'
        caplog.set_level(logging.INFO, logger=request_logger_name)

        response = await app.post(
            'events/visa/v1/provisionedToken',
            data=data
        )

        assert_that(response.status, equal_to(500))

        assert_that(
            caplog.records,
            has_item(
                has_properties(
                    message='Failed to parse notification request',
                    name=request_logger_name,
                    _context=has_entries(
                        tsp=TSPType.VISA,
                        url=contains_string('events/visa/v1/provisionedToken'),
                        **expected_log_call_kwargs,
                    ),
                )
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'data,expected_log_call_kwargs',
        [
            (b'data', {'body': 'data'}),
            (b'\x80', {'body_base64': 'gA=='}),
        ]
    )
    async def test_update_token_status_notification_logged_without_auth(
        self,
        app,
        data,
        expected_log_call_kwargs,
        mocked_logger,
        mocker,
    ):
        mocker.patch.object(
            DuckGoClient,
            'visa_verify_request',
            side_effect=DuckGoInteractionError(
                status_code=403,
                method='POST',
                service='DUCKGO',
            )
        )
        mocker.patch.object(
            VisaTokenStatusUpdateNotificationHandler,
            'logger',
            mocker.PropertyMock(return_value=mocked_logger)
        )

        response = await app.post(
            'events/visa/v1/provisionedToken',
            data=data
        )

        assert_that(response.status, equal_to(500))

        _, call_kwargs = mocked_logger.context_push.call_args_list[1]
        assert_that(
            call_kwargs,
            has_entries(**expected_log_call_kwargs)
        )
        assert call_kwargs.get('url') is not None

        mocked_logger.exception.assert_called_once_with('Visa signature verification failed')


class TestVisaCardMetaDataUpdateNotificationHandler:

    @pytest.fixture
    def params(self, api_key):
        return {
            'apiKey': api_key
        }

    @pytest.fixture
    def visa_card_metadata_update_notification(self, pan_enrollment_id):
        return {
            'date': int(utcnow().timestamp()),
            'vPanEnrollmentID': pan_enrollment_id,
        }

    @pytest.fixture
    def mock_visa_process_update_card_metadata_action(self, mocker):
        mock_run = mocker.AsyncMock()
        mock_action_cls = mocker.patch(
            'billing.yandex_pay.yandex_pay.api.handlers.events.'
            'visa.VisaProcessUpdateCardMetaDataNotificationAction'
        )
        mock_action_cls.return_value.run_async = mock_run
        return mock_action_cls

    @pytest.mark.asyncio
    async def test_update_card_metadata_notification(
        self,
        app,
        pan_enrollment_id,
        params,
        visa_card_metadata_update_notification,
        mock_duckgo_visa_verify_request,
        mock_visa_process_update_card_metadata_action,
        product_logs,
    ):
        response = await app.post(
            'events/visa/v1/panMetadata',
            params=params,
            json=visa_card_metadata_update_notification,
        )

        assert_that(response.status, equal_to(200))
        json_body = await response.json()
        assert_that(json_body, equal_to({}))

        mock_duckgo_visa_verify_request.assert_called_once()

        [log] = product_logs()
        assert_that(
            log,
            has_properties(
                message='TSP card metadata update notification',
                _context=has_entries(
                    tsp=TSPType.VISA,
                    body=visa_card_metadata_update_notification,
                )
            )
        )

        mock_visa_process_update_card_metadata_action.assert_called_once_with(
            pan_enrollment_id=pan_enrollment_id
        )
        mock_visa_process_update_card_metadata_action.return_value.run_async.assert_awaited_once_with()
        mock_visa_process_update_card_metadata_action.return_value.run.assert_not_called()

    @pytest.mark.asyncio
    async def test_should_create_async_task(
        self,
        app,
        pan_enrollment_id,
        params,
        visa_card_metadata_update_notification,
        mock_duckgo_visa_verify_request,
        mocked_logger,
        mocker,
        storage,
    ):
        mocker.patch.object(
            VisaCardMetadataUpdateNotificationHandler,
            'logger',
            mocker.PropertyMock(return_value=mocked_logger)
        )

        response = await app.post(
            'events/visa/v1/panMetadata',
            params=params,
            json=visa_card_metadata_update_notification)

        assert_that(response.status, equal_to(200))
        json_body = await response.json()
        assert_that(json_body, equal_to({}))

        created_tasks = await alist(storage.task.find())
        assert_that(len(created_tasks), equal_to(1))

        created_task = created_tasks[0]
        expected_params = {
            'max_retries': 20,
            'action_kwargs': {
                'pan_enrollment_id': visa_card_metadata_update_notification['vPanEnrollmentID'],
            }
        }

        assert_that(
            created_task,
            has_properties(
                params=expected_params,
                task_type=TaskType.RUN_ACTION,
                action_name=VisaProcessUpdateCardMetaDataNotificationAction.action_name
            )
        )

    @pytest.mark.asyncio
    async def test_update_token_status_notification_with_mailformed_schema(
        self,
        app,
        pan_enrollment_id,
        params,
        visa_card_metadata_update_notification,
        mock_duckgo_visa_verify_request,
        mock_visa_process_update_card_metadata_action,
    ):
        del visa_card_metadata_update_notification['vPanEnrollmentID']

        response = await app.post(
            'events/visa/v1/panMetadata',
            params=params,
            json=visa_card_metadata_update_notification)

        json_body = await response.json()

        expected_error = {
            'status': 500,
            'message': 'Bad Request',
            'errordetail': [
                {
                    'reason': 'INVALID_VALUE',
                    'sourceType': 'BODY',
                    'message': '["Missing data for required field."]',
                    'source': 'vPanEnrollmentID'
                }
            ],
            'reason': 'INVALID_ARGUMENT'
        }

        assert_that(response.status, equal_to(500))
        assert_that(json_body, equal_to(expected_error))
        mock_visa_process_update_card_metadata_action.return_value.run.assert_not_called()
        mock_visa_process_update_card_metadata_action.return_value.run_async.assert_not_called()

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'data,expected_log_call_kwargs',
        [
            (b'data', {'body': 'data'}),
            (b'\x80', {'body_base64': 'gA=='}),
        ]
    )
    async def test_update_card_metadata_notification_invalid_request_logged(
        self,
        app,
        data,
        expected_log_call_kwargs,
        mock_duckgo_visa_verify_request,
        caplog,
    ):
        request_logger_name = 'request_logger'
        caplog.set_level(logging.INFO, logger=request_logger_name)

        response = await app.post(
            'events/visa/v1/panMetadata',
            data=data
        )

        assert_that(response.status, equal_to(500))

        assert_that(
            caplog.records,
            has_item(
                has_properties(
                    name=request_logger_name,
                    message='Failed to parse notification request',
                    _context=has_entries(
                        tsp=TSPType.VISA,
                        url=ends_with('events/visa/v1/panMetadata'),
                        **expected_log_call_kwargs,
                    ),
                )
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'data,expected_log_call_kwargs',
        [
            (b'data', {'body': 'data'}),
            (b'\x80', {'body_base64': 'gA=='}),
        ]
    )
    async def test_update_token_status_notification_logged_without_auth(
        self,
        app,
        data,
        expected_log_call_kwargs,
        mocked_logger,
        mocker,
    ):
        mocker.patch.object(
            DuckGoClient,
            'visa_verify_request',
            side_effect=DuckGoInteractionError(
                status_code=403,
                method='POST',
                service='DUCKGO',
            )
        )
        mocker.patch.object(
            VisaCardMetadataUpdateNotificationHandler,
            'logger',
            mocker.PropertyMock(return_value=mocked_logger)
        )

        response = await app.post(
            'events/visa/v1/panMetadata',
            data=data
        )

        assert_that(response.status, equal_to(500))

        _, call_kwargs = mocked_logger.context_push.call_args_list[1]
        assert_that(
            call_kwargs,
            has_entries(**expected_log_call_kwargs)
        )
        assert call_kwargs.get('url') is not None

        mocked_logger.exception.assert_called_once_with('Visa signature verification failed')
