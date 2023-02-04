from datetime import datetime

import pytest
import pytz
from aiohttp.web import json_response
from smb.common.testing_utils import dt

from maps_adv.geosmb.booking_yang.server.lib.clients import YangClient
from maps_adv.geosmb.booking_yang.server.lib.clients.yang_client import NaiveDateTime

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
async def client():
    async with YangClient(
        url="http://yang.server", token="keken", pool_id="123"
    ) as _client:
        yield _client


@pytest.fixture
def mock_list_assignments(aresponses):
    return lambda *a: aresponses.add("yang.server", "/api/v1/assignments", "GET", *a)


async def test_sends_correct_request(client, mock_list_assignments):
    request_path = None
    request_body = None
    request_headers = None
    request_params = None

    async def _handler(request):
        nonlocal request_path, request_body, request_headers, request_params
        request_path = request.path
        request_body = await request.read()
        request_headers = request.headers
        request_params = dict(request.query)
        return json_response(status=200, data=RESPONSE_ITEMS[1])

    mock_list_assignments(_handler)

    async for _ in client.list_accepted_assignments(dt("2020-08-20 18:00:00")):
        pass

    assert request_path == "/api/v1/assignments"
    assert request_headers["Authorization"] == "OAuth keken"
    assert request_body == b""
    assert request_params == {
        "created_gte": "2020-08-20T18:00:00",
        "limit": "500",
        "pool_id": "123",
        "sort": "id",
        "status": "ACCEPTED",
    }


@pytest.mark.parametrize(
    "min_created_param, expected_result",
    [
        (datetime(2020, 8, 20, 18, 30, 00, tzinfo=pytz.utc), "2020-08-20T18:30:00"),
        (
            pytz.timezone("Europe/Moscow").localize(datetime(2020, 8, 20, 18, 30, 00)),
            "2020-08-20T15:30:00",
        ),
    ],
)
async def test_converts_min_created_to_utc(
    client, mock_list_assignments, min_created_param, expected_result
):
    created_gte_param = None

    async def _handler(request):
        nonlocal created_gte_param
        created_gte_param = request.query["created_gte"]
        return json_response(status=200, data=RESPONSE_ITEMS[1])

    mock_list_assignments(_handler)

    async for _ in client.list_accepted_assignments(min_created_param):
        pass

    assert created_gte_param == expected_result


async def test_adds_id_gt_param_if_more_than_one_chunk(client, mock_list_assignments):
    request_params = []

    async def _handler_01(request):
        nonlocal request_params
        request_params.append(dict(request.query))
        return json_response(status=200, data=RESPONSE_ITEMS[0])

    async def _handler_02(request):
        nonlocal request_params
        request_params.append(dict(request.query))
        return json_response(status=200, data=RESPONSE_ITEMS[1])

    mock_list_assignments(_handler_01)
    mock_list_assignments(_handler_02)

    async for _ in client.list_accepted_assignments(dt("2020-08-20 18:00:00")):
        pass

    assert request_params[0].get("id_gt") is None
    assert request_params[1].get("id_gt") == "id_2"


async def test_returns_expected_assignment_data(client, mock_list_assignments):
    for resp_page in RESPONSE_ITEMS:
        mock_list_assignments(json_response(status=200, data=resp_page))

    assignments = []

    async for assignment in client.list_accepted_assignments(dt("2020-08-20 18:00:00")):
        assignments.append(assignment)

    assert assignments[0] == dict(
        id="id_1",
        task_suite_id="task_suite_id_1",
        task={"id": "task_id_1", "tasks_params": "task_params_values_1"},
        solution={"solution": "value_1"},
        accepted_at=dt("2015-12-15 20:00:00"),
    )
    assert assignments[1] == dict(
        id="id_2",
        task_suite_id="task_suite_id_2",
        task={"id": "task_id_2", "tasks_params": "task_params_values_2"},
        solution={"solution": "value_3"},
        accepted_at=dt("2016-12-15 20:00:00"),
    )
    assert assignments[2] == dict(
        id="id_3",
        task_suite_id="task_suite_id_3",
        task={"id": "task_id_3", "tasks_params": "task_params_values_3"},
        solution={"solution": "value_5"},
        accepted_at=dt("2017-12-15 20:00:00"),
    )


async def test_returns_nothing_if_nothing_found(client, mock_list_assignments):
    mock_list_assignments(
        json_response(status=200, data={"items": [], "has_more": False})
    )

    assignments = []

    async for assignment in client.list_accepted_assignments(dt("2020-08-20 18:00:00")):
        assignments.append(assignment)

    assert assignments == []


async def test_raises_for_naive_min_created(client):
    with pytest.raises(
        NaiveDateTime,
        match="min_created must be aware with timezone: 2020-08-20 18:00:00.",
    ):
        async for _ in client.list_accepted_assignments(
            datetime(2020, 8, 20, 18, 0, 0)
        ):
            pass


RESPONSE_ITEMS = [
    {
        "items": [
            {
                "id": "id_1",
                "task_suite_id": "task_suite_id_1",
                "tasks": [
                    {"id": "task_id_1", "tasks_params": "task_params_values_1"},
                    {"id": "another_task_id", "tasks_params": "task_params_values"},
                ],
                "solutions": [
                    {"output_values": {"solution": "value_1"}},
                    {"output_values": {"solution": "value_2"}},
                ],
                "accepted": "2015-12-15T20:00:00",
                "any_other": "param_values",
            },
            {
                "id": "id_2",
                "task_suite_id": "task_suite_id_2",
                "tasks": [
                    {"id": "task_id_2", "tasks_params": "task_params_values_2"},
                    {"id": "another_task_id", "tasks_params": "task_params_values"},
                ],
                "solutions": [
                    {"output_values": {"solution": "value_3"}},
                    {"output_values": {"solution": "value_4"}},
                ],
                "accepted": "2016-12-15T20:00:00",
                "any_other": "param_values",
            },
        ],
        "has_more": True,
    },
    {
        "items": [
            {
                "id": "id_3",
                "task_suite_id": "task_suite_id_3",
                "tasks": [
                    {"id": "task_id_3", "tasks_params": "task_params_values_3"},
                    {"id": "another_task_id", "tasks_params": "task_params_values"},
                ],
                "solutions": [
                    {"output_values": {"solution": "value_5"}},
                    {"output_values": {"solution": "value_6"}},
                ],
                "accepted": "2017-12-15T20:00:00",
                "any_other": "param_values",
            }
        ],
        "has_more": False,
    },
]
