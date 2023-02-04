import asyncio

import pytest

from maps_adv.stat_controller.server.lib.domains import NormalizerStatus
from maps_adv.stat_controller.server.tests.tools import Any, dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_201(api):
    response = await api.post("/tasks/normalizer/", json={"executor_id": "executor0"})

    assert response.status == 201


@pytest.mark.parametrize(
    "now, expected",
    (
        [
            dt("2019-05-05 04:30:00"),
            ("2019-05-05T04:19:30+00:00", "2019-05-05T04:29:30+00:00"),
        ],
        # violate hour boundary
        [
            dt("2019-05-05 04:05:00"),
            ("2019-05-05T03:54:30+00:00", "2019-05-05T03:59:59+00:00"),
        ],
        # don't violate hour end buffer (MAX_TIME_RANGE_TO_SKIP)
        [
            dt("1999-05-05 07:59:29"),
            ("1999-05-05T07:48:59+00:00", "1999-05-05T07:58:59+00:00"),
        ],
    ),
)
async def test_will_create_new_task_if_no_other_exists(now, expected, api, freezer):
    freezer.move_to(now)

    response = await api.post("/tasks/normalizer/", json={"executor_id": "executor0"})
    json = await response.json()

    assert json["id"] == Any(int)
    assert json["timing_from"] == expected[0]
    assert json["timing_to"] == expected[1]


@pytest.mark.freeze_time(dt("2019-05-05 03:59:30"))
async def test_skips_first_creation_if_hour_violation(api):
    response = await api.post("/tasks/normalizer/", json={"executor_id": "executor0"})

    assert response.status == 200
    assert await response.json() == {}


@pytest.mark.parametrize(
    "now, timings, expected",
    (
        [
            dt("1999-05-05 07:50:00"),
            (dt("1999-05-05 07:20:00"), dt("1999-05-05 07:39:29")),
            ("1999-05-05T07:39:30+00:00", "1999-05-05T07:49:30+00:00"),
        ],
        [
            dt("1999-05-05 07:20:00"),
            (dt("1999-05-05 06:40:00"), dt("1999-05-05 06:59:59")),
            ("1999-05-05T07:00:00+00:00", "1999-05-05T07:19:30+00:00"),
        ],
        # don't exceed MAX_TIME_RANGE interval length
        [
            dt("1999-05-05 07:50:00"),
            (dt("1999-05-05 06:40:00"), dt("1999-05-05 06:59:59")),
            ("1999-05-05T07:00:00+00:00", "1999-05-05T07:19:59+00:00"),
        ],
        [
            dt("1999-05-05 08:05:00"),
            (dt("1999-05-05 06:40:00"), dt("1999-05-05 06:59:59")),
            ("1999-05-05T07:00:00+00:00", "1999-05-05T07:19:59+00:00"),
        ],
        # don't violate hour end buffer (MAX_TIME_RANGE_TO_SKIP)
        [
            dt("1999-05-05 07:19:29"),
            (dt("1999-05-05 06:40:00"), dt("1999-05-05 06:59:59")),
            ("1999-05-05T07:00:00+00:00", "1999-05-05T07:18:59+00:00"),
        ],
        # don't violate hour boundaries
        [
            dt("1999-05-05 08:05:00"),
            (dt("1999-05-05 07:30:00"), dt("1999-05-05 07:49:29")),
            ("1999-05-05T07:49:30+00:00", "1999-05-05T07:59:59+00:00"),
        ],
        [
            dt("1999-05-05 08:30:00"),
            (dt("1999-05-05 07:40:00"), dt("1999-05-05 07:54:59")),
            ("1999-05-05T07:55:00+00:00", "1999-05-05T07:59:59+00:00"),
        ],
        # ready for impossible: exists task which violate hour end buffer
        [
            dt("1999-05-05 08:30:00"),
            (dt("1999-05-05 07:40:00"), dt("1999-05-05 07:59:50")),
            ("1999-05-05T07:59:51+00:00", "1999-05-05T07:59:59+00:00"),
        ],
        [
            dt("1999-05-05 23:00:00"),
            (dt("1999-05-05 07:21:00"), dt("1999-05-05 07:40:29")),
            ("1999-05-05T07:40:30+00:00", "1999-05-05T07:59:59+00:00"),
        ],
    ),
)
async def test_task_created_with_timing_gt_completed_previous(
    now, timings, expected, api, factory, freezer
):
    freezer.move_to(now)

    await factory.normalizer("executor0", *timings, NormalizerStatus.completed)

    response = await api.post("/tasks/normalizer/", json={"executor_id": "executor1"})
    json = await response.json()

    assert json == {
        "id": Any(int),
        "timing_from": expected[0],
        "timing_to": expected[1],
    }


@pytest.mark.asyncio
@pytest.mark.freeze_time(dt("2019-05-05 07:59:30"))
async def test_skips_creation_with_timing_gt_previous_if_hour_end_buffer_violation(
    factory, api
):
    await factory.normalizer(
        "executor0",
        dt("2019-05-05 07:00:00"),
        dt("2019-05-05 07:19:59"),
        NormalizerStatus.completed,
    )
    response = await api.post("/tasks/normalizer/", json={"executor_id": "executor1"})

    assert response.status == 200
    assert await response.json() == {}


@pytest.mark.parametrize(
    "previous",
    (
        # gt than now - TIME_LAG
        (dt("1999-05-05 04:40:00"), dt("1999-05-05 04:49:31")),
        # gt than now - MIN_TIME_RANGE + TIME_LAG
        (dt("1999-05-05 14:20:01"), dt("1999-05-05 14:40:00")),
        # eq to now - MIN_TIME_RANGE + TIME_LAG
        (dt("1999-05-05 04:30:00"), dt("1999-05-05 04:39:30")),
    ),
)
@pytest.mark.freeze_time(dt("1999-05-05 04:50:00"))
async def test_is_not_created_if_may_intersects(previous, api, factory):
    await factory.normalizer("executor0", *previous, NormalizerStatus.completed)

    response = await api.post("/tasks/normalizer/", json={"executor_id": "executor1"})

    assert response.status == 200
    assert await response.json() == {}


@pytest.mark.freeze_time(dt("2019-05-06 00:25:00"))
async def test_returns_reanimated_if_exists(factory, api):
    task_id = await factory.normalizer(
        "executor0", dt("2019-05-06 00:00:00"), dt("2019-05-06 00:10:00"), failed=True
    )

    response = await api.post("/tasks/normalizer/", json={"executor_id": "executor1"})
    json = await response.json()

    assert json["id"] == task_id


@pytest.mark.freeze_time(dt("2019-05-06 00:25:00"))
async def test_returns_nothing_if_normalizing_in_progress(factory, api):
    await factory.normalizer(
        "executor0", dt("2019-05-06 00:00:00"), dt("2019-05-06 00:10:00")
    )

    response = await api.post("/tasks/normalizer/", json={"executor_id": "executor1"})
    json = await response.json()

    assert json == {}


async def test_errored_for_empty_executor_id(api):
    response = await api.post("/tasks/normalizer/", json={"executor_id": ""})
    json = await response.json()

    assert response.status == 400
    assert json == {"executor_id": ["Value should not be empty."]}


async def test_errored_for_empty_data(api):
    response = await api.post("/tasks/normalizer/", json={})
    json = await response.json()

    assert response.status == 400
    assert json == {"executor_id": ["Missing data for required field."]}


@pytest.mark.freeze_time(dt("2019-05-02 01:30:00"))
async def test_returns_task_data(api):
    response = await api.post("/tasks/normalizer/", json={"executor_id": "executor0"})
    json = await response.json()

    assert json == {
        "timing_from": "2019-05-02T01:19:30+00:00",
        "timing_to": "2019-05-02T01:29:30+00:00",
        "id": Any(int),
    }


@pytest.mark.real_db
async def test_cant_request_two_tasks_in_one_moment(api):
    async def _request(executor_id, sleep):
        await asyncio.sleep(sleep)
        return await api.post("/tasks/normalizer/", json={"executor_id": executor_id})

    responses = await asyncio.gather(
        _request("executor2", 0), _request("executor3", 0.001)
    )

    assert responses[0].status == 201
    assert responses[1].status == 409
