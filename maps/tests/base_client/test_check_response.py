from asyncio import coroutine
from unittest.mock import Mock

import pytest

from maps_adv.stat_controller.client.lib.base.client import BaseClient
from maps_adv.stat_controller.client.lib.base.exceptions import (
    BadGateway,
    Conflict,
    GatewayTimeout,
    InternalServerError,
    InvalidContentType,
    UnknownResponse,
    WrongPayload,
)

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("status_code", [200, 500, 502, 504, 555])
async def test_does_not_raise_exception_for_expected_status_response(status_code):
    response = Mock()
    response.status = status_code

    await BaseClient._check_response(response, status_code)


@pytest.mark.parametrize(
    ["status_code", "expected_exception", "expected_exception_args"],
    [
        (400, WrongPayload, ("error message",)),
        (409, Conflict, ("error message",)),
        (415, InvalidContentType, ("error message",)),
        (500, InternalServerError, tuple()),
        (502, BadGateway, tuple()),
        (504, GatewayTimeout, tuple()),
    ],
)
async def test_raises_exception_for_known_error_status_codes(
    status_code, expected_exception, expected_exception_args
):
    response = Mock()
    response.status = status_code
    response.json = coroutine(lambda: "error message")

    with pytest.raises(expected_exception) as exc_info:
        await BaseClient._check_response(response, 200)

    assert exc_info.value.args == expected_exception_args


async def test_raises_exception_for_unknown_error_status_codes():
    response = Mock()
    response.status = 555
    response.content.read = coroutine(lambda: "error message")

    with pytest.raises(UnknownResponse) as exc_info:
        await BaseClient._check_response(response, 200)

    assert exc_info.value.args == ("Status=555, payload=error message",)
