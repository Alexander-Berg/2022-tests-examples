"""Tests host operation log."""

import pytest

from infra.walle.server.tests.lib.util import TestCase
from sepelib.mongo.mock import ObjectMocker
from walle.hosts import TaskType
from walle.models import monkeypatch_timestamp
from walle.operations_log.constants import Operation
from walle.operations_log.operations import OperationLog, check_limits, get_last_n_operations
from walle.util.limits import TimedLimit


@pytest.fixture
def test(request):
    return TestCase.create(request)


def test_check_limits(test, monkeypatch):
    host1 = test.mock_host(dict(inv=1, name="one"))
    host2 = test.mock_host(dict(inv=2, name="two"))

    main_operation = Operation.REBOOT
    other_operation = Operation.REDEPLOY

    log = ObjectMocker(OperationLog)
    log.mock(
        dict(
            id="1.1",
            audit_log_id="x",
            host_inv=host1.inv,
            host_name=host1.name,
            type=main_operation.type,
            time=1,
            task_type=TaskType.AUTOMATED_HEALING,
        )
    )
    log.mock(
        dict(
            id="1.2",
            audit_log_id="x",
            host_inv=host1.inv,
            host_name=host1.name,
            type=other_operation.type,
            time=2,
            task_type=TaskType.AUTOMATED_HEALING,
        )
    )
    log.mock(
        dict(
            id="1.3",
            audit_log_id="x",
            host_inv=host1.inv,
            host_name=host1.name,
            type=main_operation.type,
            time=3,
            task_type=TaskType.AUTOMATED_HEALING,
        )
    )
    log.mock(
        dict(
            id="1.4",
            audit_log_id="x",
            host_inv=host1.inv,
            host_name="some-other-name",
            type=main_operation.type,
            time=4,
            task_type=TaskType.AUTOMATED_HEALING,
        )
    )
    log.mock(
        dict(
            id="2.1",
            audit_log_id="x",
            host_inv=host2.inv,
            host_name=host2.name,
            type=main_operation.type,
            time=4,
            task_type=TaskType.AUTOMATED_HEALING,
        )
    )

    monkeypatch_timestamp(monkeypatch, 4)

    assert check_limits(host1, main_operation, [TimedLimit(period=10, limit=3)])
    assert not check_limits(host1, main_operation, [TimedLimit(period=10, limit=2)])

    assert check_limits(host1, other_operation, [TimedLimit(period=10, limit=2)])
    assert not check_limits(host1, other_operation, [TimedLimit(period=10, limit=1)])

    assert check_limits(host1, main_operation, [TimedLimit(period=2, limit=2)])
    assert not check_limits(host1, main_operation, [TimedLimit(period=2, limit=1)])

    # Mock host configuration changing
    host1.name = "some-other-name"

    assert check_limits(host1, main_operation, [TimedLimit(period=10, limit=2)])
    assert not check_limits(host1, main_operation, [TimedLimit(period=10, limit=1)])

    # Mock a free host
    del host1.name

    assert check_limits(host1, main_operation, [TimedLimit(period=10, limit=4)])
    assert not check_limits(host1, main_operation, [TimedLimit(period=10, limit=3)])


def test_check_limits_with_params(test, monkeypatch):
    host, operation = test.mock_host(dict(inv=1, name="one")), Operation.CHANGE_DISK

    log = ObjectMocker(OperationLog)
    common = {
        'audit_log_id': "x",
        'host_inv': host.inv,
        'host_name': host.name,
        "task_type": TaskType.AUTOMATED_HEALING,
    }
    log.mock(dict(id="1.1", type=operation.type, time=1, params={"slot": 1, "serial": "x"}, **common))
    log.mock(dict(id="1.2", type=operation.type, time=2, params={"slot": 2}, **common))
    log.mock(dict(id="1.3", type=operation.type, time=3, params={"slot": 3}, **common))
    log.mock(dict(id="1.4", type=operation.type, time=4, params={"slot": 1, "serial": "y"}, **common))

    monkeypatch_timestamp(monkeypatch, 5)

    assert check_limits(host, operation, [TimedLimit(period=10, limit=5)])
    assert not check_limits(host, operation, [TimedLimit(period=10, limit=4)])

    assert check_limits(host, operation, [TimedLimit(period=10, limit=3)], params={"slot": 1})
    assert not check_limits(host, operation, [TimedLimit(period=10, limit=2)], params={"slot": 1})

    # Missing param must be treated as matched param
    assert check_limits(host, operation, [TimedLimit(period=10, limit=4)], params={"serial": "x"})
    assert not check_limits(host, operation, [TimedLimit(period=10, limit=3)], params={"serial": "x"})


class TestGetLastNOperations:
    def test_get_last_n_operations_simple(self, test):
        host = test.mock_host(dict(inv=1, name="one"))
        operation = Operation.REBOOT
        log = ObjectMocker(OperationLog)
        for idx in range(20):
            log.mock(
                dict(
                    id="{}".format(idx),
                    audit_log_id="x",
                    host_inv=host.inv,
                    host_name=host.name,
                    type=operation.type,
                    time=idx,
                    task_type=TaskType.AUTOMATED_HEALING,
                )
            )

        ops = get_last_n_operations(host, operation, n=10)
        assert ops[0]["time"] == 19
        assert ops[-1]["time"] == 10
        assert len(ops) == 10

        ops = get_last_n_operations(host, operation, n=1)
        assert ops[0]["time"] == 19
        assert len(ops) == 1

        ops = get_last_n_operations(host, operation, n=5)
        assert ops[0]["time"] == 19
        assert ops[-1]["time"] == 15
        assert len(ops) == 5

    def test_get_last_n_operations_with_diff_op_types(self, test):
        host = test.mock_host(dict(inv=1, name="one"))
        main_operation = Operation.REBOOT
        other_operation = Operation.REDEPLOY
        log = ObjectMocker(OperationLog)
        for idx in range(0, 20, 2):
            log.mock(
                dict(
                    id="{}".format(idx),
                    audit_log_id="x",
                    host_inv=host.inv,
                    host_name=host.name,
                    type=main_operation.type,
                    time=idx,
                    task_type=TaskType.AUTOMATED_HEALING,
                )
            )
            log.mock(
                dict(
                    id="{}".format(idx + 1),
                    audit_log_id="x",
                    host_inv=host.inv,
                    host_name=host.name,
                    type=other_operation.type,
                    time=idx,
                    task_type=TaskType.AUTOMATED_HEALING,
                )
            )

        ops = get_last_n_operations(host, main_operation, n=10)
        assert ops[0]["time"] == 18
        assert ops[-1]["time"] == 0
        assert len(ops) == 10
        for op in ops:
            assert op["type"] == main_operation.type

        ops = get_last_n_operations(host, main_operation, n=5)
        assert ops[0]["time"] == 18
        assert ops[-1]["time"] == 10
        assert len(ops) == 5
        for op in ops:
            assert op["type"] == main_operation.type

        ops = get_last_n_operations(host, main_operation, n=1)
        assert ops[0]["time"] == 18
        assert ops[0]["type"] == main_operation.type

    def test_get_last_n_operations_with_params(self, test):
        host = test.mock_host(dict(inv=1, name="one"))
        operation = Operation.REBOOT
        log = ObjectMocker(OperationLog)
        for idx in range(0, 18, 3):
            log.mock(
                dict(
                    id="{}".format(idx),
                    audit_log_id="x",
                    host_inv=host.inv,
                    host_name=host.name,
                    type=operation.type,
                    time=idx,
                    task_type=TaskType.AUTOMATED_HEALING,
                )
            )
            log.mock(
                dict(
                    id="{}".format(idx + 1),
                    audit_log_id="x",
                    host_inv=host.inv,
                    host_name=host.name,
                    type=operation.type,
                    time=idx,
                    task_type=TaskType.AUTOMATED_HEALING,
                    params={"slot": 10},
                )
            )
            log.mock(
                dict(
                    id="{}".format(idx + 2),
                    audit_log_id="x",
                    host_inv=host.inv,
                    host_name=host.name,
                    type=operation.type,
                    time=idx,
                    task_type=TaskType.AUTOMATED_HEALING,
                    params={"slots": [1, 2]},
                )
            )

        ops = get_last_n_operations(host, operation, n=3)
        for op in ops:
            assert op["time"] == 15
        assert ops[1]["params"] == {"slot": 10}
        assert ops[2]["params"] == {"slots": [1, 2]}
        assert "params" not in ops[0]
        assert len(ops) == 3

        ops = get_last_n_operations(host, operation, n=3, params={"slot": 10})
        for op, idx in zip(ops, [15, 12, 9]):
            assert op["time"] == idx
            assert op["params"] == {"slot": 10}
            assert op["_id"] == str(idx + 1)

        ops = get_last_n_operations(host, operation, n=3, params={"slots": [1, 2]})
        for op, idx in zip(ops, [15, 12, 9]):
            assert op["time"] == idx
            assert op["params"] == {"slots": [1, 2]}
            assert op["_id"] == str(idx + 2)

        ops = get_last_n_operations(host, operation, n=3, params={"slots": 1})
        for op, idx in zip(ops, [15, 12, 9]):
            assert op["time"] == idx
            assert op["params"] == {"slots": [1, 2]}
            assert op["_id"] == str(idx + 2)

        ops = get_last_n_operations(host, operation, n=3, params={"slots": [1]})
        for op, idx in zip(ops, [15, 12, 9]):
            assert op["time"] == idx
            assert op["params"] == {"slots": [1, 2]}
            assert op["_id"] == str(idx + 2)

    def test_get_last_n_operations_for_empty_op_log(self, test):
        host = test.mock_host(dict(inv=1, name="one"))
        operation = Operation.REBOOT

        ops = get_last_n_operations(host, operation, n=3)
        assert len(ops) == 0
