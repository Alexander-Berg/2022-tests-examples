from asyncio import coroutine
from typing import Iterator, Tuple, Union
from unittest.mock import Mock

import aiohttp
import pytest
import tenacity

from maps_adv.warden.client.lib.client import Client as WardenClient
from maps_adv.warden.client.lib.exceptions import Conflict
from maps_adv.warden.proto.errors_pb2 import Error
from maps_adv.warden.proto.tasks_pb2 import TaskDetails, UpdateTaskOutput

pytestmark = [pytest.mark.asyncio]


class ContextManagerMock:
    def __init__(self, return_value):
        self._return_value = return_value

    async def __aenter__(self):
        return self._return_value

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        pass


class ResponseMocker:
    valid_response_data = {"id": 1, "status": "accepted", "time_limit": 300}

    @classmethod
    def response_mock_gen(
        cls,
        exceptions: Tuple = tuple(),
        with_statuses: Tuple = tuple(),
        valid_response_proto=None,
    ) -> Iterator[Union[Exception, Mock]]:
        for exc in exceptions:
            yield exc
        for status in with_statuses:
            yield cls._mock_response(status, valid_response_proto)

    @classmethod
    def _mock_response(cls, status: int, valid_response_proto) -> ContextManagerMock:
        response_coro = coroutine(
            Mock(return_value=valid_response_proto.SerializeToString())
        )

        response = Mock()
        response.status = status
        response.read = response_coro
        response.content.read = response_coro
        response.headers = {"Content-Type": "application/octet-stream"}

        return ContextManagerMock(response)


class SessionMocker:
    @classmethod
    def mock(cls, responses: Iterator) -> Tuple[Mock, Mock]:
        request = cls._mock_request(responses)
        return cls._mock_session(request), request

    @classmethod
    def _mock_request(cls, responses: Iterator) -> Mock:
        return Mock(side_effect=responses)

    @staticmethod
    def _mock_session(request: Mock) -> ContextManagerMock:
        session = Mock()
        session.close = coroutine(Mock())
        session.request = request

        return ContextManagerMock(session)


def patch_session(mocker, responses: Iterator) -> Mock:
    session_mock, request_mock = SessionMocker.mock(responses)

    mocker.patch(
        "maps_adv.warden.client.lib.client.aiohttp.ClientSession",
        return_value=session_mock,
    )

    return request_mock


async def run_create_task(client):
    await client.create_task()


async def run_update_task(client):
    await client.update_task(1, "completed")


# ==================================================


@pytest.fixture()
async def client(request, mocker):
    attempts = request.node.get_closest_marker("attempts")
    if attempts:
        for method in (WardenClient.create_task, WardenClient.update_task):
            method.retry.stop = tenacity.stop_after_attempt(attempts.args[0])

    for method in (WardenClient.create_task, WardenClient.update_task):
        method.retry.wait = tenacity.wait_exponential(multiplier=0.001)

    yield WardenClient(
        "https://example.com/", executor_id="executor0", task_type="test-task"
    )


@pytest.mark.parametrize(
    "run_func, run_params, valid_response_status, valid_response_proto",
    [
        (
            "create_task",
            {},
            201,
            TaskDetails(task_id=1, status="accepted", time_limit=300),
        ),
        ("update_task", dict(task_id=1, status="completed"), 204, UpdateTaskOutput()),
    ],
)
async def test_retries_for_expected_responses2(
    mocker, client, run_func, run_params, valid_response_status, valid_response_proto
):
    responses = ResponseMocker.response_mock_gen(
        exceptions=(aiohttp.ServerDisconnectedError(), aiohttp.ServerTimeoutError()),
        with_statuses=(500, 502, 504, valid_response_status),
        valid_response_proto=valid_response_proto,
    )

    request_mock = patch_session(mocker, responses)

    await getattr(client, run_func)(**run_params)

    assert request_mock.call_count == 6


@pytest.mark.parametrize(
    "run_func, run_params, valid_response_status, valid_response_proto",
    [
        ("create_task", {}, 201, Error(code=Error.ERROR_CODE.CONFLICT)),
    ],
)
async def test_not_retries_for_unexpected_response_status(
    mocker, client, run_func, run_params, valid_response_status, valid_response_proto
):
    responses = ResponseMocker.response_mock_gen(
        with_statuses=(409, valid_response_status),
        valid_response_proto=valid_response_proto,
    )

    request_mock = patch_session(mocker, responses)

    with pytest.raises(Conflict):
        await getattr(client, run_func)(**run_params)

    assert request_mock.call_count == 1


@pytest.mark.parametrize(
    "run_func, run_params",
    [("create_task", {}), ("update_task", dict(task_id=1, status="completed"))],
)
async def test_not_retries_for_unexpected_exception(
    mocker, client, run_func, run_params
):
    responses = ResponseMocker.response_mock_gen(
        exceptions=(aiohttp.ClientConnectionError("not-retry-exception"),)
    )

    request_mock = patch_session(mocker, responses)

    with pytest.raises(aiohttp.ClientConnectionError) as e:
        await getattr(client, run_func)(**run_params)

    assert "not-retry-exception" in e.value.args
    assert request_mock.call_count == 1


@pytest.mark.parametrize(
    "run_func, run_params, valid_response_status, valid_response_proto",
    [
        (
            "create_task",
            {},
            201,
            TaskDetails(task_id=1, status="accepted", time_limit=300),
        ),
        ("update_task", dict(task_id=1, status="completed"), 204, UpdateTaskOutput()),
    ],
)
@pytest.mark.attempts(2)
async def test_raises_if_retry_attempts_exceeds_max(
    mocker, client, run_func, run_params, valid_response_status, valid_response_proto
):
    responses = ResponseMocker.response_mock_gen(
        with_statuses=(502, 502, valid_response_status),
        valid_response_proto=valid_response_proto,
    )
    request_mock = patch_session(mocker, responses)

    with pytest.raises(tenacity.RetryError):
        await getattr(client, run_func)(**run_params)

    assert request_mock.call_count == 2
