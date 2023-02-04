from unittest import mock

import pytest
from freezegun import freeze_time
from smb.common.http_client import BaseHttpClientException
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize(
    "processed_services, expected_doorman_called, ",
    [
        (set(), True),
        ({"unknown"}, True),
        ({"doorman"}, False),
    ],
)
async def test_calls_service_if_no_completed_ops_for_it(
    domain,
    dm,
    doorman,
    processed_services,
    expected_doorman_called,
):
    dm.list_processed_services.coro.return_value = processed_services

    await domain.delete_clients()

    assert doorman.clear_clients_for_gdpr.called == expected_doorman_called


@freeze_time("2020-01-01 00:00:00")
async def test_marks_request_as_processed_if_all_services_become_processed(domain, dm):
    dm.list_not_processed_requests.coro.return_value = [{"id": 11, "passport_uid": 111}]
    dm.list_processed_services.coro.return_value = set()

    await domain.delete_clients()

    dm.mark_request_as_processed.assert_called_once_with(
        record_id=11, processed_at=dt("2020-01-01 00:00:00")
    )


async def test_does_not_marks_request_as_processed_if_doorman_call_failed(
    domain, dm, doorman
):
    dm.list_processed_services.coro.return_value = set()
    doorman.clear_clients_for_gdpr.side_effect = BaseHttpClientException

    await domain.delete_clients()

    dm.mark_request_as_processed.assert_not_called()


async def test_logs_error_if_no_services_to_call(domain, dm, caplog):
    dm.list_not_processed_requests.coro.return_value = [{"id": 11, "passport_uid": 111}]
    dm.list_processed_services.coro.return_value = {
        "doorman",
    }

    await domain.delete_clients()

    errors = [r for r in caplog.records if r.levelname == "ERROR"]
    assert [error.message for error in errors] == [
        "Delete request is not marked as processed, "
        "but operations for all services are completed. "
        "request_id=11, processed_services=['doorman']"
    ]


async def test_logs_error_for_each_failed_service_call(
    domain, doorman, caplog
):
    doorman.clear_clients_for_gdpr.side_effect = Exception

    await domain.delete_clients()

    errors = [r for r in caplog.records if r.levelname == "ERROR"]
    assert [error.message for error in errors] == [
        "Failed to clear client's data in service. request_id=11, "
        "passport_uid=111, service_name=doorman.",
    ]


async def test_creates_op_for_each_successful_service_call(
    domain, dm, doorman
):
    dm.list_not_processed_requests.coro.return_value = [
        {"id": 1, "passport_uid": 111000}
    ]
    doorman.clear_clients_for_gdpr.coro.return_value = [
        {"client_id": 111, "biz_id": 222}
    ]

    await domain.delete_clients()

    dm.create_operation.assert_has_calls(
        [
            mock.call(
                request_id=1,
                service_name="doorman",
                metadata=[{"client_id": 111, "biz_id": 222}],
                is_success=True,
            ),
        ]
    )


async def test_creates_op_for_each_failed_service_call(
    domain, dm, doorman
):
    dm.list_not_processed_requests.coro.return_value = [{"id": 11, "passport_uid": 111}]
    doorman.clear_clients_for_gdpr.side_effect = BaseHttpClientException("boom one!")

    await domain.delete_clients()

    dm.create_operation.assert_has_calls(
        [
            mock.call(
                request_id=11,
                service_name="doorman",
                metadata="boom one!",
                is_success=False,
            ),
        ]
    )
