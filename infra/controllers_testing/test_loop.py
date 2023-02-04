import pytest

import infra.callisto.controllers.utils.funcs as funcs


def test_topological_sort():
    good_tree = {
        1: [2, 3],
        2: [4],
        3: [],
        4: [],
    }
    order = funcs.topological_sort(1, good_tree)
    assert order[0] == 4, 'the deepest'
    assert order[3] == 1, 'root is the first'

    non_cycle_graph = {
        1: [2, 3],
        2: [3],
        3: [],
    }
    order = funcs.topological_sort(1, non_cycle_graph)
    assert order == [3, 2, 1]

    cycle_graph = {
        1: [2],
        2: [3],
        3: [1],
    }
    with pytest.raises(AssertionError):
        funcs.topological_sort(1, cycle_graph)
