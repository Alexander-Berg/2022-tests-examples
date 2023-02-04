import pytest
from smb.common.testing_utils import dt

from smb.common.aiotvm.lib import BaseTvmException
from maps_adv.geosmb.cleaner.server.lib.exceptions import NoPassportUidException

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time(dt("2020-01-01 00:00:00"))]

url = "/1/takeout/status/"


async def test_returns_data_in_expected_format(api, factory):
    await factory.create_request(
        passport_uid=123, processed_at=dt("2020-01-01 00:00:00")
    )

    res = await api.get(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        expected_status=200,
    )

    assert res == {
        "status": "ok",
        "data": [
            {
                "id": "1",
                "slug": "all",
                "state": "ready_to_delete",
                "update_date": "2020-01-01T00:00:00+00:00",
            }
        ],
    }


@pytest.mark.parametrize("is_client_exists", [True, False])
async def test_returns_delete_in_progress_state_for_not_processed_request(
    api, factory, doorman, is_client_exists
):
    await factory.create_request(passport_uid=123, processed_at=None)
    doorman.search_clients_for_gdpr.coro.return_value = is_client_exists

    res = await api.get(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        expected_status=200,
    )

    assert res["data"][0]["state"] == "delete_in_progress"


async def test_goes_to_all_servers_if_no_request_in_processing(
    api, factory, doorman,
):
    await factory.create_request(processed_at=dt("2020-01-01 00:00:00"))

    await api.get(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        expected_status=200,
    )

    doorman.search_clients_for_gdpr.assert_called_once_with(passport_uid=123)


async def test_returns_empty_if_client_exists_nowhere(
    api, doorman
):
    doorman.search_clients_for_gdpr.coro.return_value = False

    res = await api.get(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        expected_status=200,
    )

    assert res["data"][0]["state"] == "empty"


@pytest.mark.parametrize(
    "doorman_resp",
    [
        True,
        False,
        True,
    ],
)
async def test_returns_ready_to_delete_if_client_exists_somewhere(
    factory,
    api,
    doorman,
    doorman_resp,
):
    doorman.search_clients_for_gdpr.coro.return_value = doorman_resp

    res = await api.get(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        expected_status=200,
    )

    assert res["data"][0]["state"] == "ready_to_delete"


@pytest.mark.parametrize(
    "doorman_resp",
    [
        True,
        Exception,
    ],
)
async def test_ignores_fails_if_client_exists_somewhere(
    api, doorman, doorman_resp,
):
    doorman.search_clients_for_gdpr.coro.side_effect = [doorman_resp]

    res = await api.get(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        expected_status=200,
    )

    assert res["data"][0]["state"] == "ready_to_delete"


@pytest.mark.parametrize(
    "doorman_resp",
    [
        True,
        Exception,
    ],
)
async def test_logs_not_critical_fails(
    api,
    doorman,
    doorman_resp,
    caplog,
):
    doorman.search_clients_for_gdpr.coro.side_effect = [doorman_resp]

    await api.get(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        expected_status=200,
    )

    warns = [r for r in caplog.records if r.levelname == "WARNING"]
    assert {warn.message for warn in warns} == {
        "Request to service failed, but we keep moving."
    }


@pytest.mark.parametrize(
    "doorman_resp",
    [
        False,
        Exception,
    ],
)
async def test_returns_500_for_fails_if_client_exists_nowhere(
    api,
    doorman,
    doorman_resp,
):
    doorman.search_clients_for_gdpr.coro.side_effect = [doorman_resp]

    await api.get(url, headers={"X-Ya-User-Ticket": "u-ticket"}, expected_status=500)


async def test_returns_error_if_user_ticket_is_missed(api):
    res = await api.get(url, expected_status=200)

    assert res == {
        "status": "error",
        "errors": [
            {"code": "missed_user_ticket", "message": "TVM user ticket is missed"}
        ],
    }


async def test_returns_error_if_user_ticket_is_bad(aiotvm, api):
    aiotvm.fetch_user_uid.side_effect = BaseTvmException(
        {
            "error": "invalid ticket format",
            "debug_string": "debug",
            "logging_string": "bad-ticket",
        }
    )

    res = await api.get(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        expected_status=200,
    )

    assert res == {
        "status": "error",
        "errors": [
            {
                "code": "bad_user_ticket",
                "message": "{'error': 'invalid ticket format', "
                "'debug_string': 'debug', 'logging_string': 'bad-ticket'}",
            }
        ],
    }


async def test_returns_error_if_user_ticket_without_passport_uid(aiotvm, api):
    aiotvm.fetch_user_uid.side_effect = NoPassportUidException

    res = await api.get(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        expected_status=200,
    )

    assert res == {
        "status": "error",
        "errors": [
            {
                "code": "no_passport_uid",
                "message": "No passport uid for TVM user ticket",
            }
        ],
    }


async def test_returns_updated_date_as_dt_of_last_processed_request(api, factory):
    await factory.create_request(
        passport_uid=123, processed_at=dt("2020-01-01 00:00:00")
    )

    res = await api.get(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        expected_status=200,
    )

    assert res["data"][0]["update_date"] == "2020-01-01T00:00:00+00:00"


async def test_returns_empty_updated_date_if_no_processed_requests(api, factory):
    await factory.create_request(passport_uid=123, processed_at=None)

    res = await api.get(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        expected_status=200,
    )

    assert res["data"][0]["update_date"] is None
