import pytest

from maps_adv.stat_controller.server.lib.domains import Normalizer, NormalizerStatus
from maps_adv.stat_controller.server.tests.tools import Any, dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def domain(normalizer_dm, config):
    return Normalizer(
        normalizer_dm,
        config["TIME_LAG"],
        config["MIN_TIME_RANGE"],
        config["MAX_TIME_RANGE"],
        config["MAX_TIME_RANGE_TO_SKIP"],
    )


@pytest.mark.parametrize(
    "now, ex_from, ex_to",
    (
        [
            dt("2019-05-05 04:30:00"),
            dt("2019-05-05 04:19:30"),
            dt("2019-05-05 04:29:30"),
        ],
        # violate hour boundary
        [
            dt("2019-05-05 04:05:00"),
            dt("2019-05-05 03:54:30"),
            dt("2019-05-05 03:59:59"),
        ],
        # don't violate hour end buffer (MAX_TIME_RANGE_TO_SKIP)
        [
            dt("1999-05-05 07:59:29"),
            dt("1999-05-05 07:48:59"),
            dt("1999-05-05 07:58:59"),
        ],
    ),
)
async def test_will_create_new_if_no_other_exists(now, ex_from, ex_to, freezer, domain):
    freezer.move_to(now)

    got = await domain.find_new("executor0")

    assert got == {"id": Any(int), "timing_from": ex_from, "timing_to": ex_to}


@pytest.mark.freeze_time(dt("2019-05-05 03:59:30"))
async def test_skips_first_creation_if_hour_violation(domain):
    got = await domain.find_new("executor0")

    assert got is None


@pytest.mark.parametrize(
    "now, timings, expected",
    (
        [
            dt("1999-05-05 07:50:00"),
            (dt("1999-05-05 07:20:00"), dt("1999-05-05 07:39:29")),
            (dt("1999-05-05 07:39:30"), dt("1999-05-05 07:49:30")),
        ],
        [
            dt("1999-05-05 07:20:00"),
            (dt("1999-05-05 06:40:00"), dt("1999-05-05 06:59:59")),
            (dt("1999-05-05 07:00:00"), dt("1999-05-05 07:19:30")),
        ],
        # don't exceed MAX_TIME_RANGE interval length
        [
            dt("1999-05-05 07:50:00"),
            (dt("1999-05-05 06:40:00"), dt("1999-05-05 06:59:59")),
            (dt("1999-05-05 07:00:00"), dt("1999-05-05 07:19:59")),
        ],
        [
            dt("1999-05-05 08:05:00"),
            (dt("1999-05-05 06:40:00"), dt("1999-05-05 06:59:59")),
            (dt("1999-05-05 07:00:00"), dt("1999-05-05 07:19:59")),
        ],
        # don't violate hour end buffer (MAX_TIME_RANGE_TO_SKIP)
        [
            dt("1999-05-05 07:19:29"),
            (dt("1999-05-05 06:40:00"), dt("1999-05-05 06:59:59")),
            (dt("1999-05-05 07:00:00"), dt("1999-05-05 07:18:59")),
        ],
        # don't violate hour boundaries
        [
            dt("1999-05-05 08:05:00"),
            (dt("1999-05-05 07:30:00"), dt("1999-05-05 07:49:29")),
            (dt("1999-05-05 07:49:30"), dt("1999-05-05 07:59:59")),
        ],
        [
            dt("1999-05-05 08:30:00"),
            (dt("1999-05-05 07:40:00"), dt("1999-05-05 07:54:59")),
            (dt("1999-05-05 07:55:00"), dt("1999-05-05 07:59:59")),
        ],
        # ready for impossible: exists task which violate hour end buffer
        [
            dt("1999-05-05 08:30:00"),
            (dt("1999-05-05 07:40:00"), dt("1999-05-05 07:59:50")),
            (dt("1999-05-05 07:59:51"), dt("1999-05-05 07:59:59")),
        ],
        [
            dt("1999-05-05 23:00:00"),
            (dt("1999-05-05 07:21:00"), dt("1999-05-05 07:40:29")),
            (dt("1999-05-05 07:40:30"), dt("1999-05-05 07:59:59")),
        ],
    ),
)
async def test_created_with_timing_gt_previous(
    now, timings, expected, freezer, domain, factory
):
    await factory.normalizer("executor0", *timings, NormalizerStatus.completed)
    freezer.move_to(now)

    got = await domain.find_new("executor1")

    assert got == {"id": Any(int), "timing_from": expected[0], "timing_to": expected[1]}


@pytest.mark.freeze_time(dt("2019-05-05 07:59:30"))
async def test_skips_creation_with_timing_gt_previous_if_hour_end_buffer_violation(
    domain, factory
):
    await factory.normalizer(
        "executor0", dt("2019-05-05 07:00:00"), dt("2019-05-05 07:19:59")
    )

    got = await domain.find_new("executor1")

    assert got is None


@pytest.mark.parametrize(
    "previous",
    (
        # gt than now - TIME_LAG
        (dt("1999-05-05 04:30:00"), dt("1999-05-05 04:49:31")),
        # gt than now - MIN_TIME_RANGE + TIME_LAG
        (dt("1999-05-05 14:20:01"), dt("1999-05-05 14:40:00")),
        # eq to now - MIN_TIME_RANGE + TIME_LAG
        (dt("1999-05-05 04:30:00"), dt("1999-05-05 04:39:30")),
    ),
)
@pytest.mark.freeze_time(dt("1999-05-05 04:50:00"))
async def test_is_not_created_if_may_intersects(previous, domain, factory):
    await factory.normalizer("executor0", *previous)

    got = await domain.find_new("executor0")

    assert got is None


@pytest.mark.freeze_time(dt("1999-05-05 07:30:00"))
async def test_returns_failed_instead_of_new(domain, factory):
    task_id = await factory.normalizer(
        "executor0", dt("1999-05-05 06:40:00"), dt("1999-05-05 06:59:59"), failed=True
    )

    got = await domain.find_new("sample_executor_id")

    assert got == {
        "id": task_id,
        "timing_from": dt("1999-05-05 06:40:00"),
        "timing_to": dt("1999-05-05 06:59:59"),
    }


@pytest.mark.freeze_time(dt("1999-05-05 07:30:00"))
async def test_does_not_give_tasks_if_normalizing_in_progress(domain, factory):
    await factory.normalizer(
        "executor0", dt("1999-05-05 06:40:00"), dt("1999-05-05 06:59:59")
    )

    got = await domain.find_new("sample_executor_id")

    assert got is None
