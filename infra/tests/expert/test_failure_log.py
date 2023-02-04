"""Tests host failure log."""

import pytest

from infra.walle.server.tests.lib.util import TestCase
from walle.expert import failure_log
from walle.expert.failure_log import FailureLog, register_failure
from walle.expert.types import Failure
from walle.models import timestamp, monkeypatch_timestamp
from walle.util.juggler import get_aggregate_name
from walle.util.limits import TimedLimit


@pytest.fixture
def test(request, mp_juggler_source):
    return TestCase.create(request)


def infinity_list():
    value = 0
    while True:
        yield value
        value += 1


@pytest.mark.parametrize("not_count", [True, False])
def test_register(mp, test, not_count):
    monkeypatch_timestamp(mp)
    mp.function(failure_log.uuid4, module=failure_log, side_effect=infinity_list())

    host = test.mock_host({"inv": 0})

    register_failure(host, ["first"], not_count=not_count)
    test.failure_log.mock(
        {
            "id": "0",
            "host_inv": host.inv,
            "project": host.project,
            "aggregate": host.get_aggregate(),
            "failures": ["first"],
            "action_time": timestamp(),
            "not_count": True if not_count else None,
        },
        save=False,
    )

    test.hosts.assert_equal()
    test.failure_log.assert_equal()

    host = test.mock_host({"inv": 1})
    register_failure(host, ["first"], not_count=not_count)
    test.failure_log.mock(
        {
            "id": "1",
            "host_inv": host.inv,
            "project": host.project,
            "aggregate": host.get_aggregate(),
            "failures": ["first"],
            "action_time": timestamp(),
            "not_count": True if not_count else None,
        },
        save=False,
    )

    test.hosts.assert_equal()
    test.failure_log.assert_equal()


@pytest.mark.parametrize('failure', Failure.ALL_RACK)
@pytest.mark.parametrize('fails_per_rack', [1, 10])
@pytest.mark.parametrize(
    'racks,limit,passed',
    [
        (1, 1, True),
        (2, 2, True),
        (2, 1, False),
        (3, 2, False),
    ],
)
def test_check_rack_limits(test, monkeypatch_timestamp, failure, racks, limit, fails_per_rack, passed):
    cur_time = timestamp()
    queue = 'mock-queue'
    project = 'mock-project'
    inv = 0
    for rack_id in range(racks):
        rack = "r{}".format(rack_id)
        aggregate = get_aggregate_name(queue, rack)
        delta = 0
        for _ in range(fails_per_rack):
            test.mock_host(
                {
                    "inv": inv,
                    "project": project,
                    "name": "mocked-{}".format(inv),
                    "location": {"rack": rack, "short_queue_name": queue},
                }
            )

            FailureLog(
                id="{}-{}-1".format(project, inv),
                host_inv=inv,
                project=project,
                aggregate=aggregate,
                failures=[failure],
                action_time=cur_time - 50 + delta,
            ).save(force_insert=True)
            inv += 1
            delta += 10

    if passed:
        assert failure_log.check_total_action_limits(failure, cur_time - 50, [TimedLimit(period=100, limit=limit)])
    else:
        assert not failure_log.check_total_action_limits(failure, cur_time - 50, [TimedLimit(period=100, limit=limit)])


def test_check_failure_limits(test, monkeypatch_timestamp):
    cur_time = timestamp()

    for inv in range(1, 4):
        test.mock_host({"inv": inv, "project": "p{}".format(inv), "name": "mocked-{}".format(inv)})

    FailureLog(
        id="1-1",
        host_inv=1,
        project="p1",
        aggregate='mock-aggregate',
        failures=["one", "two", "credited"],
        action_time=cur_time - 50,
    ).save(force_insert=True)
    FailureLog(
        id="1-2",
        host_inv=1,
        project="p1",
        aggregate='mock-aggregate',
        failures=["one", "credited"],
        credited_failures=["credited"],
        action_time=cur_time - 1,
    ).save(force_insert=True)

    FailureLog(
        id="2-1",
        host_inv=2,
        project="p2",
        aggregate='mock-aggregate',
        failures=["one", "three", "credited"],
        credited_failures=["credited"],
        action_time=cur_time - 1,
    ).save(force_insert=True)
    FailureLog(
        id="3-1",
        host_inv=3,
        project="p3",
        aggregate='mock-aggregate',
        failures=["three", "credited"],
        credited_failures=["credited"],
        action_time=cur_time - 50,
    ).save(force_insert=True)
    FailureLog(
        id="3-2",
        host_inv=3,
        project="p3",
        aggregate='mock-aggregate',
        failures=["three", "credited"],
        credited_failures=["credited"],
        action_time=cur_time - 1,
        not_count=True,
    ).save(force_insert=True)

    assert failure_log.check_total_action_limits("three", 0, [TimedLimit(period=10, limit=1)])
    assert not failure_log.check_total_action_limits("three", 0, [TimedLimit(period=10, limit=0)])
    assert failure_log.check_total_action_limits("three", cur_time, [TimedLimit(period=10, limit=0)])

    assert failure_log.check_total_action_limits(
        "three", 0, [TimedLimit(period=10, limit=1), TimedLimit(period=100, limit=2)]
    )
    assert not failure_log.check_total_action_limits(
        "three", 0, [TimedLimit(period=10, limit=1), TimedLimit(period=100, limit=1)]
    )
    assert failure_log.check_total_action_limits(
        "three", cur_time - 1, [TimedLimit(period=10, limit=1), TimedLimit(period=100, limit=1)]
    )

    assert not failure_log.check_total_action_limits("three", 0, [TimedLimit(period=100, limit=1)])
    assert failure_log.check_total_action_limits("three", 0, [TimedLimit(period=100, limit=1)], project_id="p2")

    assert not failure_log.check_total_action_limits("three", 0, [TimedLimit(period=100, limit=0)], project_id="p3")
    assert failure_log.check_total_action_limits("three", 0, [TimedLimit(period=10, limit=0)], project_id="p3")

    assert not failure_log.check_total_action_limits(
        "three", cur_time - 50, [TimedLimit(period=100, limit=0)], project_id="p3"
    )
    assert failure_log.check_total_action_limits(
        "three", cur_time - 49, [TimedLimit(period=100, limit=0)], project_id="p3"
    )

    assert failure_log.check_total_action_limits("credited", 0, [TimedLimit(period=100, limit=1)])
    assert not failure_log.check_total_action_limits("credited", 0, [TimedLimit(period=100, limit=0)])
