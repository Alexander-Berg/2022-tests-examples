from asyncio import coroutine
from typing import Tuple, Type
from unittest.mock import Mock

import aiohttp
import pytest
import tenacity

from maps_adv.config_loader import Config
from maps_adv.stat_controller.client.lib.base.exceptions import (
    BadGateway,
    GatewayTimeout,
    InternalServerError,
    UnknownResponse,
)
from maps_adv.stat_controller.client.lib.normalizer import (
    Client as NormalizerClient,
    TaskStatus as NormalizerTaskStatus,
)
from maps_adv.stat_controller.client.tests.utils import ContextManagerMock

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def config():
    _config = {
        "RETRY_MAX_ATTEMPTS": {"default": 3},
        "RETRY_WAIT_MULTIPLIER": {"default": 0.01},
    }

    config = Config(_config)
    config.init()
    return config


def valid_response_data():
    return {
        "timing_from": "2019-05-06T01:00:00+00:00",
        "timing_to": "2019-05-06T01:05:00+00:00",
        "id": 100,
    }


def mock_session_request(error_effects: Tuple[Exception], response_status: int):
    response_coro = coroutine(Mock(return_value=valid_response_data()))

    response = Mock()
    response.status = response_status
    response.json = response_coro
    response.content.read = response_coro

    request_side_effects = error_effects + (ContextManagerMock(response),)
    request = Mock(side_effect=request_side_effects)

    session = Mock()
    session.close = coroutine(Mock())
    session.request = request

    return session, request


def mock_session_content_read(error_effects: Tuple[Exception], response_status: int):
    response_side_effects = error_effects + (valid_response_data(),)
    response_coro = coroutine(Mock(side_effect=response_side_effects))

    response = Mock()
    response.status = response_status
    response.json = response_coro
    response.content.read = response_coro

    request = Mock(return_value=ContextManagerMock(response))

    session = Mock()
    session.close = coroutine(Mock())
    session.request = request

    return session, request


def patch_session(
    mocker, session_mock_func, error_effects: Tuple[Exception], response_status=200
):
    session_mock, request_mock = session_mock_func(
        error_effects=error_effects, response_status=response_status
    )

    mocker.patch(
        "maps_adv.stat_controller.client.lib.base.client.aiohttp.ClientSession",
        return_value=session_mock,
    )

    return request_mock


async def run_with_expected_raise(config, expected_exception_cls: Type[Exception]):
    async with NormalizerClient(
        "https://example.com/",
        retry_settings={
            "max_attempts": config.RETRY_MAX_ATTEMPTS,
            "wait_multiplier": config.RETRY_WAIT_MULTIPLIER,
        },
    ) as client:
        with pytest.raises(expected_exception_cls) as e:
            await client.update_task(
                task_id=10,
                executor_id="executor0",
                status=NormalizerTaskStatus.completed,
            )
    return e


async def run_without_config(max_attempts: int = 0, wait_multiplier: int = 0):
    async with NormalizerClient(
        "https://example.com/",
        retry_settings={
            "max_attempts": max_attempts,
            "wait_multiplier": wait_multiplier,
        },
    ) as client:
        await client.update_task(
            task_id=10, executor_id="executor0", status=NormalizerTaskStatus.completed
        )


async def run(config):
    await run_without_config(config.RETRY_MAX_ATTEMPTS, config.RETRY_WAIT_MULTIPLIER)


# ==================================================


@pytest.mark.parametrize(
    "session_mock_func", (mock_session_request, mock_session_content_read)
)
async def test_retries_for_exceptions_with_expected_type(
    mocker, config, session_mock_func
):
    error_effects = (
        InternalServerError,
        BadGateway,
        GatewayTimeout,
        aiohttp.ServerDisconnectedError(),
        aiohttp.ServerTimeoutError(),
    )
    request_mock = patch_session(
        mocker, session_mock_func=session_mock_func, error_effects=error_effects
    )

    await run_without_config(max_attempts=len(error_effects) + 1)

    assert request_mock.call_count == len(error_effects) + 1


@pytest.mark.parametrize(
    "session_mock_func", (mock_session_request, mock_session_content_read)
)
async def test_raises_for_exceptions_with_unexpected_type(
    mocker, config, session_mock_func
):
    request_mock = patch_session(
        mocker,
        session_mock_func=session_mock_func,
        error_effects=(aiohttp.ClientConnectionError("not-retry-exception"),),
    )

    e = await run_with_expected_raise(config, aiohttp.ClientConnectionError)

    assert "not-retry-exception" in e.value.args
    assert request_mock.call_count == 1


@pytest.mark.parametrize(
    "session_mock_func", (mock_session_request, mock_session_content_read)
)
async def test_raises_if_retry_attempts_exceeds_max(mocker, config, session_mock_func):
    error_effects = (
        aiohttp.ServerDisconnectedError(),
        aiohttp.ServerDisconnectedError(),
        aiohttp.ServerDisconnectedError(),
        aiohttp.ServerDisconnectedError(),
    )
    request_mock = patch_session(
        mocker, session_mock_func=session_mock_func, error_effects=error_effects
    )

    await run_with_expected_raise(config, tenacity.RetryError)

    assert request_mock.call_count == config.RETRY_MAX_ATTEMPTS


@pytest.mark.parametrize(
    "session_mock_func", (mock_session_request, mock_session_content_read)
)
async def test_not_retries_on_bad_status_response(mocker, config, session_mock_func):
    request_mock = patch_session(
        mocker,
        session_mock_func=session_mock_func,
        error_effects=tuple(),
        response_status=555,
    )

    e = await run_with_expected_raise(config, UnknownResponse)

    assert e.value.status_code == 555
    assert request_mock.call_count == 1
