import infra.callisto.controllers.build.planner as planner
import infra.callisto.controllers.utils.entities as entities
import infra.callisto.controllers.sdk.tier as tier


class TaskMock(object):
    def __init__(self, resource_name, prev_shard_name=None):
        self.resource_name = resource_name
        self.prev_shard_name = prev_shard_name

    @property
    def task_id(self):
        return 'id-' + self.resource_name

    def __eq__(self, other):
        return (
            self.resource_name == other.resource_name
        )


def test_not_enough_space():
    shard_size = 1234
    builders = [planner.Builder(entities.Agent('host', 0), building_shards=set(), prepared_shards=set(),
                                space=shard_size - 1, tier=_test_tier(shard_size))] * 1
    mapping = planner.assign_build_tasks_to_builders([TaskMock('shard')], builders, shard_size, shard_size, shard_size)
    assert not mapping, 'cannot schedule'


def test_not_enough_builders():
    shard_size = 1234
    builders = [planner.Builder(entities.Agent('host', i), building_shards=set(), prepared_shards=set(),
                                space=shard_size, tier=_test_tier(shard_size)) for i in range(9)]
    mapping = planner.assign_build_tasks_to_builders([TaskMock('shard')] * 10, builders,
                                                     shard_size, shard_size, shard_size)
    assert len(mapping) == 9, 'cannot schedule one task'


def test_inc_build():
    tasks_to_build = [TaskMock('shard-{}'.format(i), 'prev-shard-{}'.format(i)) for i in range(5)]
    builders_with_prev_shard = [
        planner.Builder(agent=entities.Agent('host', i), space=15, tier=_test_tier(5),
                        building_shards=set(), prepared_shards={'prev-shard-{}'.format(i)})
        for i in range(4)
    ]
    builder_without_prev_shard = planner.Builder(agent=entities.Agent('host', 4), space=15,
                                                 building_shards=set(), prepared_shards=set(), tier=_test_tier(5))
    builders = builders_with_prev_shard + [builder_without_prev_shard]

    mapping = planner.assign_build_tasks_to_builders(
        tasks_to_build, builders,
        space_needed_inc=10, space_needed_full=15, shard_size=5
    )
    for i, builder in enumerate(builders[:-1]):
        assert mapping[builder.agent] == tasks_to_build[i], 'enough freespace to build shard'
    assert mapping[builder_without_prev_shard.agent] == tasks_to_build[4], 'enough freespace to download&build shard'

    mapping = planner.assign_build_tasks_to_builders(
        tasks_to_build, builders,
        space_needed_inc=10, space_needed_full=15, shard_size=10
    )
    for i, builder in enumerate(builders[:-1]):
        assert mapping[builder.agent] == tasks_to_build[i], 'enough freespace to build shard'
    assert builder_without_prev_shard.agent not in mapping, 'not enough freespace to download and build shard'

    mapping = planner.assign_build_tasks_to_builders(
        tasks_to_build, builders,
        space_needed_inc=10, space_needed_full=15, shard_size=5000
    )
    for i, builder in enumerate(builders[:-1]):
        assert mapping[builder.agent] == tasks_to_build[i], 'shard_size does not affect build ' \
                                                            'if prev_shard is already prepared'

    mapping = planner.assign_build_tasks_to_builders(
        tasks_to_build, builders,
        space_needed_inc=1000, space_needed_full=1500, shard_size=5000
    )
    assert not mapping, 'not enough freespace to build shard, mapping should be empty'


def _test_tier(shard_size):
    if 'TestTier' in tier.TIERS:
        tier.TIERS.pop('TestTier')
    t = tier._JupiterTier('TestTier', 1, shard_size)
    t._shard_size = shard_size
    return t


def _all_tasks_in_mapping(tasks, mapping):
    return {task.task_id for task in tasks} == {task.task_id for task in mapping.values()}


def _all_builders_in_mapping(builders, mapping):
    return {builder.agent for builder in builders} == set(mapping)


def test_full_build():
    tasks_to_build = [TaskMock('shard-{}'.format(i)) for i in range(5)]
    builders = [
        planner.Builder(agent=entities.Agent('host', i), space=10, tier=_test_tier(5),
                        building_shards=set(), prepared_shards=set())
        for i in range(5)
    ]
    mapping = planner.assign_build_tasks_to_builders(
        tasks_to_build, builders,
        space_needed_inc=10, space_needed_full=10, shard_size=5
    )
    assert _all_tasks_in_mapping(tasks_to_build, mapping), 'builders have enough space'
    assert _all_builders_in_mapping(builders, mapping)

    mapping = planner.assign_build_tasks_to_builders(
        tasks_to_build, builders,
        space_needed_inc=10, space_needed_full=10, shard_size=5000
    )
    assert _all_tasks_in_mapping(tasks_to_build, mapping), 'shard_size does not affect full build'
    assert _all_builders_in_mapping(builders, mapping)

    mapping = planner.assign_build_tasks_to_builders(
        tasks_to_build, builders,
        space_needed_inc=5000, space_needed_full=5000, shard_size=5
    )
    assert not mapping, 'not enough freespace to build shard, mapping should be empty'


def test_find_tasks_to_remove():
    prev_tasks = [TaskMock('prev-shard-{}'.format(i)) for i in range(4)]
    tasks_to_build = [TaskMock('shard-{}'.format(i), 'prev-shard-{}'.format(i)) for i in range(5)]
    builders = [
        planner.Builder(
            agent=entities.Agent('host', i), space=10, tier=_test_tier(5),
            building_shards={tasks_to_build[i].resource_name}, prepared_shards={prev_tasks[i].resource_name}
        )
        for i in range(4)
    ]

    # in both states (building_shards and prepared_shards) is should work the same
    for state_of_shard in ['building_shards', 'prepared_shards']:
        builder_with_unneeded_shard = planner.Builder(
            agent=entities.Agent('host', 5), space=10, tier=_test_tier(5),
            building_shards=({prev_tasks[3].resource_name} if state_of_shard == 'building_shards' else set()),
            prepared_shards=({prev_tasks[3].resource_name} if state_of_shard == 'prepared_shards' else set()),
        )
        builders_for_test = builders + [builder_with_unneeded_shard]

        to_remove = planner.find_tasks_to_remove(tasks_to_build, prev_tasks, builders_for_test, keep_replicas_count=1)
        assert len(to_remove) == 1, 'builder with unneeded shard in {} should remove it'.format(state_of_shard)
        assert builder_with_unneeded_shard.agent in to_remove
        assert len(to_remove[builder_with_unneeded_shard.agent]) == 1
        assert to_remove[builder_with_unneeded_shard.agent][0] == prev_tasks[3]

        to_remove = planner.find_tasks_to_remove(tasks_to_build, prev_tasks, builders_for_test, keep_replicas_count=2)
        assert not to_remove, 'now this second copy is important'
