import gc
import pytest

from maps.garden.libs_server.graph.graph_python import Graph


def iterlen(it):
    return sum(1 for _ in it)


def _check_length(g, correct_length):
    assert len(g) == correct_length
    assert iterlen(g) == correct_length
    assert iterlen(g.vertices()) == correct_length


def test_graph():
    graph = Graph()

    graph.add_vertex(1)
    _check_length(graph, 1)

    graph.add_edge(2, 3)
    _check_length(graph, 3)

    assert graph.children(2) == set([3])
    assert graph.children(3) == set()

    with pytest.raises(Exception):
        graph.children(4)

    graph.add_vertex(4)
    assert sorted(list(graph.vertices())) == [1, 2, 3, 4]

    for v in range(1, 5):
        assert v in graph
    for v in range(5, 9):
        assert v not in graph

    graph.add_edge(2, 1)
    graph.add_edge(2, 1)
    assert graph.children(2) == set([1, 3])
    assert graph.children(1) == set()

    graph.remove_vertex(3)
    _check_length(graph, 3)

    assert graph.children(2) == set([1])

    graph.remove_edge(2, 1)
    assert graph.children(2) == set()

    del graph
    gc.collect()
