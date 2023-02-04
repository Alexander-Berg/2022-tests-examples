import pytest

import infra.callisto.controllers.build.source as sources
import infra.callisto.controllers.build.yt_observer as yt_observer
import infra.callisto.controllers.sdk.tier as tiers


def _make_target(time, state, generation, prev_state=None):
    return sources.Target(time, state, generation, 'junk', 'junk', 'junk', prev_state)


def test_source_get_targets():
    targets = [
        _make_target(1, 10, 1234),
        _make_target(2, 11, 1235),
        _make_target(3, 12, 1235),
    ]
    source = sources.ConstSource(targets)
    assert source.all_targets() == targets
    assert source.get_targets() == [
        _make_target(1, 10, 1234),
        _make_target(3, 12, 1235),
    ], 'should leave only one (newest) version of same generation'

    targets = [
        _make_target(3, 12, 1235),
        _make_target(2, 11, 1235),
        _make_target(1, 10, 1234),
    ]
    source = sources.ConstSource(targets)
    assert source.get_targets() == [
        _make_target(1, 10, 1234),
        _make_target(3, 12, 1235),
    ], 'should sort is according to `time`'


def test_source_simple():
    targets = [
        _make_target(1, 10, 1234),
        _make_target(2, 11, 1235),
    ]
    source = sources.ConstSource(targets)
    assert source.target_by_generation(1234) == targets[0]
    assert source.target_by_generation(1235) == targets[1]

    assert source.target_by_state(10) == targets[0]
    assert source.target_by_state(11) == targets[1]

    with pytest.raises(ValueError):
        assert source.target_by_generation(0)
    with pytest.raises(ValueError):
        assert source.target_by_state(0)


def test_source_multiple_targets():
    targets = [
        _make_target(1, 10, 1234),
        _make_target(2, 11, 1235),
        _make_target(3, 10, 1236),
        _make_target(4, 15, 1235),
    ]
    source = sources.ConstSource(targets)
    assert source.target_by_state(10) == targets[2]
    assert source.target_by_generation(1235) == targets[3]


def test_yt_observer():
    targets = [
        _make_target(1, 10, 1234, prev_state=None),
        _make_target(2, 11, 1235, prev_state=10),
        _make_target(3, 12, 1236, prev_state=None),
        _make_target(4, 13, 1237, prev_state=11),
    ]
    source = sources.ConstSource(targets)

    observer = yt_observer.Observer(
        tier=tiers.PlatinumTier0,
        name='test',
        source=source,
        build_task_type=None,
    )

    def _make_shard(gen):
        return tiers.PlatinumTier0.make_shard('0-0', gen)

    assert observer.get_last_generations(2) == [1237, 1236]
    assert observer._find_prev_shard_name(targets[0], _make_shard(targets[0].generation)) is None
    assert observer._find_prev_shard_name(targets[1], _make_shard(targets[1].generation)) == _make_shard(1234).fullname
    assert observer._find_prev_shard_name(targets[2], _make_shard(targets[2].generation)) is None
    assert observer._find_prev_shard_name(targets[3], _make_shard(targets[3].generation)) == _make_shard(1235).fullname
