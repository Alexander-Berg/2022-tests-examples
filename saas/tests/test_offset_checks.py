import functools
from typing import Callable

from saas.library.python.logbroker.internal_api.client import LogbrokerPartitionInfo
from saas.tools.devops.lb_dc_checker.dc_checker.checkers.common import OffsetCheckResult


def _override_partitions(curr_check: OffsetCheckResult, last_check: OffsetCheckResult, factory_fn: Callable) -> None:
    for shard, check in curr_check.shard_to_check_result.items():
        for idx, partition in list(enumerate(check.mirror_topic_info.partitions)):
            origin_partition = last_check.shard_to_check_result[shard].origin_topic_info.partitions[idx]
            new_mirror_partition = factory_fn(origin_partition, partition)
            check.mirror_topic_info.partitions[idx] = new_mirror_partition


def _create_mirror_partition(
    last_origin_partition: LogbrokerPartitionInfo,
    curr_mirror_partition: LogbrokerPartitionInfo,
    offset_delta: int
) -> LogbrokerPartitionInfo:
    mirror_read_offset = last_origin_partition.read_offset + offset_delta
    mirror_end_offset = max(curr_mirror_partition.end_offset, mirror_read_offset)

    return LogbrokerPartitionInfo(
        mirror_end_offset,
        mirror_read_offset,

        curr_mirror_partition.commit_time_lag_ms,
        curr_mirror_partition.read_time_lag_ms,
        curr_mirror_partition.read_session_id
    )


def test_offset_check_success(fake, last_check, curr_check):
    factory_fn = functools.partial(_create_mirror_partition, offset_delta=fake.random_int(10, 100))
    _override_partitions(curr_check, last_check, factory_fn)

    assert curr_check.is_passed(last_check=last_check) is True


def test_offset_check_success_same(last_check, curr_check):
    factory_fn = functools.partial(_create_mirror_partition, offset_delta=0)
    _override_partitions(curr_check, last_check, factory_fn)

    assert curr_check.is_passed(last_check=last_check) is True


def test_offset_check_error(fake, last_check, curr_check):
    factory_fn = functools.partial(_create_mirror_partition, offset_delta=-fake.random_int(10, 100))
    _override_partitions(curr_check, last_check, factory_fn)

    assert curr_check.is_passed(last_check=last_check) is False


def test_offset_checks(fake, last_check, curr_check):
    for _ in range(10):
        offset_delta = fake.random_int(-100, 100)
        factory_fn = functools.partial(_create_mirror_partition, offset_delta=offset_delta)
        _override_partitions(curr_check, last_check, factory_fn)

        assert curr_check.is_passed(last_check=last_check) == (offset_delta >= 0)
