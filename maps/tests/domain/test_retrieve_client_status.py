import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize("is_client_exists", [True, False])
async def test_returns_delete_in_progress_status_for_not_processed_request(
    domain, dm, doorman, is_client_exists
):
    doorman.search_clients_for_gdpr.coro.return_value = is_client_exists
    dm.retrieve_last_request.coro.return_value = {"id": 1, "processed_at": None}

    got = await domain.retrieve_client_status(tvm_user_ticket="user-ticket")

    assert got[0] == "delete_in_progress"


@pytest.mark.parametrize(
    "last_delete_request", [None, {"id": 1, "processed_at": dt("2020-01-01 00:00:00")}]
)
async def test_goes_to_all_servers_if_no_request_in_processing(
    domain, dm, doorman, last_delete_request
):
    dm.retrieve_last_request.coro.return_value = last_delete_request

    await domain.retrieve_client_status(tvm_user_ticket="user-ticket")

    doorman.search_clients_for_gdpr.assert_called_once_with(passport_uid=123)


async def test_returns_empty_if_client_exists_nowhere(
    domain, dm, doorman
):
    dm.retrieve_last_request.coro.return_value = None
    doorman.search_clients_for_gdpr.coro.return_value = False

    got = await domain.retrieve_client_status(tvm_user_ticket="user-ticket")

    assert got[0] == "empty"


@pytest.mark.parametrize(
    "doorman_resp",
    [
        True,
        False,
        True,
    ],
)
async def test_returns_ready_to_delete_if_client_exists_somewhere(
    domain,
    dm,
    doorman,
    doorman_resp,
):
    dm.retrieve_last_request.coro.return_value = None
    doorman.search_clients_for_gdpr.coro.return_value = doorman_resp

    got = await domain.retrieve_client_status(tvm_user_ticket="user-ticket")

    assert got[0] == "ready_to_delete"


@pytest.mark.parametrize(
    "doorman_resp",
    [
        True,
        Exception,
    ],
)
async def test_ignores_fails_if_client_exists_somewhere(
    domain,
    dm,
    doorman,
    doorman_resp
):
    dm.retrieve_last_request.coro.return_value = None
    doorman.search_clients_for_gdpr.coro.side_effect = [doorman_resp]

    got = await domain.retrieve_client_status(tvm_user_ticket="user-ticket")

    assert got[0] == "ready_to_delete"


@pytest.mark.parametrize(
    "doorman_resp",
    [
        True,
        Exception,
    ],
)
async def test_logs_not_critical_fails(
    domain,
    dm,
    doorman,
    doorman_resp,
    caplog,
):
    dm.retrieve_last_request.coro.return_value = None
    doorman.search_clients_for_gdpr.coro.side_effect = [doorman_resp]

    await domain.retrieve_client_status(tvm_user_ticket="user-ticket")

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
async def test_raises_for_fails_if_client_exists_nowhere(
    domain,
    dm,
    doorman,
    doorman_resp
):
    dm.retrieve_last_request.coro.return_value = None
    doorman.search_clients_for_gdpr.coro.side_effect = [doorman_resp]

    with pytest.raises(Exception):
        await domain.retrieve_client_status(tvm_user_ticket="user-ticket")


@pytest.mark.parametrize(
    "dt_of_last_processed_request", [None, dt("2020-01-01 00:00:00")]
)
async def test_returns_dt_of_last_processed_request(
    domain, dm, doorman, dt_of_last_processed_request
):
    dm.retrieve_dt_of_last_processed_request.coro.return_value = (
        dt_of_last_processed_request
    )
    got = await domain.retrieve_client_status(tvm_user_ticket="user-ticket")

    assert got[1] == dt_of_last_processed_request
