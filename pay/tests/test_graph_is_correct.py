import json
from collections import defaultdict

import pytest
from library.python import resource

ENVIRONMENTS = ['/production.json', '/load.json']


@pytest.mark.parametrize('env', ENVIRONMENTS)
def test_graph_structure_is_loadable(env):
    graph_file = resource.find(env)

    graph = json.loads(graph_file)

    assert graph is not None


@pytest.mark.parametrize('env', ENVIRONMENTS)
def test_dependencies_have_only_existing_nodes(env):
    graph_file = resource.find(env)

    nodes = json.loads(graph_file)
    existing_nodes = set([x['name_id'] for x in nodes])

    not_existing_dependencies = set()

    for node in nodes:
        for dependency in node['dependencies']:
            if dependency not in existing_nodes:
                not_existing_dependencies.add(dependency)

    assert not not_existing_dependencies, 'Found not existing dependencies: {}'.format(not_existing_dependencies)


def find_duplicates(elements, key):
    keys = set()
    duplicated_keys = []

    for element in elements:
        element_key = key(element)
        if element_key in keys:
            duplicated_keys.append(element_key)
        keys.add(element_key)

    return duplicated_keys


@pytest.mark.parametrize('env', ENVIRONMENTS)
def test_have_no_duplicate_node_name_ids(env):
    graph_file = resource.find(env)

    nodes = json.loads(graph_file)

    duplicated_name_ids = find_duplicates(nodes, key=lambda x: x['name_id'])

    assert not duplicated_name_ids, 'Found duplicated name ids: {}'.format(duplicated_name_ids)


def create_adjacency_dict(nodes):
    graph = defaultdict(list)
    for node in nodes:
        target_id = node['name_id']
        for source_name in node['dependencies']:
            graph[source_name].append(target_id)
    return graph


def dfs(graph, start, end):
    stack = [(start, [])]
    paths = []
    while stack:
        state, path = stack.pop()
        if path and state == end:
            paths.append(path)
            continue
        for next_state in graph[state]:
            if next_state in path:
                continue
            stack.append((next_state, path + [next_state]))
    return paths


def filter_unique_lists(list_of_lists):
    unique_lists = list()
    for entity_list in list_of_lists:
        add = True
        for unique_list in unique_lists:
            # check cyclic permutation
            sublists = [(unique_list + unique_list)[i: i + len(unique_list)] for i in range(len(unique_list))]
            if entity_list in sublists and len(entity_list) == len(unique_list):
                add = False
                break
        if add:
            unique_lists.append(entity_list)
    return unique_lists


@pytest.mark.parametrize('env', ENVIRONMENTS)
def test_have_no_cycles(env):
    graph_file = resource.find(env)
    nodes = json.loads(graph_file)
    graph = create_adjacency_dict(nodes)
    cycles = list()
    for node in nodes:
        name_id = node["name_id"]
        for path in dfs(graph, name_id, name_id):
            cycles.append([name_id] + path)

    unique_cycles = filter_unique_lists(cycles)
    assert len(unique_cycles) == 0, f"Found cycles: {unique_cycles}"


@pytest.mark.parametrize('env', ENVIRONMENTS)
def test_execution_months_inheritance(env):
    graph_file = resource.find(env)

    bad_nodes = []
    nodes = json.loads(graph_file)
    nodes = {node['name_id']: node for node in nodes}
    default_range = range(1, 13)
    for node in nodes.values():
        previous_execution_months = set(default_range)
        for name_id in node['dependencies']:
            previous_execution_months = previous_execution_months.intersection(
                nodes[name_id].get('execution_months', []) or default_range)
        if not (previous_execution_months
                and set(node.get('execution_months', []) or default_range).issubset(previous_execution_months)):
            bad_nodes.append(node['name_id'])
    assert not bad_nodes, 'Found nodes with incorrectly inherited execution_months: {}'.format(bad_nodes)


@pytest.mark.parametrize('env', ['/production.json'])
def test_have_offset_or_dependencies(env):
    graph_file = resource.find(env)
    nodes = json.loads(graph_file)
    bad_nodes = []
    for node in nodes:
        if len(node['dependencies']) == 0 and node.get('offset') is None:
            bad_nodes.append(node['name_id'])
    assert not bad_nodes, 'Found nodes without dependencies and offset: {}'.format(bad_nodes)
