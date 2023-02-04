import json
import copy
from nirvana_api.parameter_classes import BlockPosition

from payplatform.tools.parse_nirvana_tasks_json import parse_nirvana_tasks

from graph_layout import get_graphiz_node_positions, graphiz_graph

FAKE_NIRVANA_OPERATION_NAME = 'fake_noop_operation'


def is_fake_task(task):
    return task.nirvana_operation == FAKE_NIRVANA_OPERATION_NAME


class TestTaskOverride:
    def __init__(self, config):
        self.prepare_operations = parse_nirvana_tasks(config.get('prepare_operations', []))
        self.test_operations = parse_nirvana_tasks(config.get('test_operations', []))


class TestGraphConfig:
    def __init__(self, config):
        self.whitelist = {}
        for task_id in config['whitelist']:
            self.whitelist[task_id] = TestTaskOverride(config['whitelist'][task_id])

        environment = config.get('environment', {})
        self.environment_type = environment.get('type', '')
        self.environment_operations = environment.get('operations', [])
        self.ticket = config.get('ticket', '')
        self.st_token = config.get('st_token', '')


def load_test_graph_config(test_graph_config_filename):
    with open(test_graph_config_filename, 'r') as fin:
        config = json.load(fin)
        return TestGraphConfig(config)


def turn_task_into_fake(task):
    task.nirvana_operation = FAKE_NIRVANA_OPERATION_NAME
    task.params = {}


def apply_test_operations(id_to_task, test_graph_config):
    for task_id, task_override in test_graph_config.whitelist.items():
        for _, test_operation in task_override.test_operations.items():
            test_operation.dependencies.add(task_id)

    for _, task in id_to_task.items():
        additional_dependencies = set()
        for dependency_id in task.dependencies:
            if dependency_id in test_graph_config.whitelist:
                additional_dependencies.update(test_graph_config.whitelist[dependency_id].test_operations)
        task.dependencies.update(additional_dependencies)


def apply_prepare_operations(id_to_task, test_graph_config):
    for task_id, task_override in test_graph_config.whitelist.items():
        additional_dependencies = set()

        for _, prepare_operation in task_override.prepare_operations.items():
            # Prepare operation is launched before actual task,
            # make it start after all dependencies of actual task but before actual task.
            prepare_operation.dependencies.update(id_to_task[task_id].dependencies)
            additional_dependencies.add(prepare_operation.name_id)

        id_to_task[task_id].dependencies.update(additional_dependencies)


def apply_test_blocks_params_override(id_to_task, test_graph_config, param, new_value):
    for task_id, task in test_graph_config.whitelist.items():
        for _, test_operation in task.test_operations.items():
            id_to_task[test_operation.name_id].params[param] = new_value
        for _, prepare_operation in task.prepare_operations.items():
            id_to_task[prepare_operation.name_id].params[param] = new_value


def apply_environment_override(id_to_task, test_graph_config):
    for task_id, task in id_to_task.items():
        if task.nirvana_operation in test_graph_config.environment_operations:
            task.params['environment'] = test_graph_config.environment_type


def apply_test_graph_overrides(id_to_task, test_graph_config):
    apply_test_operations(id_to_task, test_graph_config)
    apply_prepare_operations(id_to_task, test_graph_config)

    for task_id, task in id_to_task.items():
        if task_id not in test_graph_config.whitelist:
            turn_task_into_fake(task)

    for task_id, task_override in test_graph_config.whitelist.items():
        for _, test_operation in task_override.test_operations.items():
            id_to_task[test_operation.name_id] = test_operation
        for _, prepare_operation in task_override.prepare_operations.items():
            id_to_task[prepare_operation.name_id] = prepare_operation

    # if test_graph_config.environment_type:
    #     apply_environment_override(id_to_task, test_graph_config)
    #     apply_test_blocks_params_override(id_to_task, test_graph_config, 'environment',
    #                                       test_graph_config.environment_type)

    if test_graph_config.ticket:
        apply_test_blocks_params_override(id_to_task, test_graph_config, 'ticket', test_graph_config.ticket)

    if test_graph_config.st_token:
        apply_test_blocks_params_override(id_to_task, test_graph_config, 'st_token', test_graph_config.st_token)


def get_not_fake_dependencies(id_to_task, task_name_id):
    dependencies = set()
    task = id_to_task[task_name_id]

    for dependency_name_id in task.dependencies:
        if is_fake_task(id_to_task[dependency_name_id]):
            dependencies.update(get_not_fake_dependencies(id_to_task, dependency_name_id))
        else:
            dependencies.add(dependency_name_id)

    task.dependencies = dependencies
    return dependencies


def reduce_graph(id_to_task):
    for name_id, task in id_to_task.items():
        if not is_fake_task(task):
            task.dependencies = get_not_fake_dependencies(id_to_task, name_id)


TEST_LAYOUT_OFFSET = 10000


def layout_test_graph(blocks_info, id_to_task):
    graph = graphiz_graph()

    id_to_task = copy.deepcopy(id_to_task)

    reduce_graph(id_to_task)

    mnclose_task_to_block_guid = {}

    for block in blocks_info:
        mnclose_task = block.parameters.get('mnclose_task')
        if mnclose_task and not is_fake_task(id_to_task[mnclose_task]):
            graph.node(mnclose_task)
            for dependency in id_to_task[mnclose_task].dependencies:
                graph.edge(dependency, mnclose_task)
        mnclose_task_to_block_guid[mnclose_task] = block.block_guid

    node_positions = get_graphiz_node_positions(graph)

    positions = {}

    for mnclose_task, position in node_positions.items():
        block_guid = mnclose_task_to_block_guid[mnclose_task]
        positions[block_guid] = BlockPosition(int(position['x']) - TEST_LAYOUT_OFFSET,
                                              int(position['y']) - TEST_LAYOUT_OFFSET)

    return positions
