# -*- coding: utf-8 -*-

import pytest

from maps.garden.sdk.core import GardenError
from maps.garden.libs_server.graph.task_queue import TaskQueue
from maps.garden.libs_server.graph.graph_python import Graph


class TaskStub:
    def __init__(self, read_locks, write_locks):
        self.read_locks = read_locks
        self.write_locks = write_locks

    def task_id(self):
        return int(hash(self))


@pytest.fixture
def first_task():
    return TaskStub(set("1"), set("2"))


@pytest.fixture
def second_task():
    return TaskStub(set("2"), set("3"))


def create_task_queue(first_task, second_task):
    task_queue = TaskQueue()
    task_queue.add(second_task, [first_task])
    return task_queue


def test_restore(first_task, second_task):
    task_queue = create_task_queue(first_task, second_task)
    task_queue.recompute_ready()
    assert task_queue.has_ready()
    assert task_queue.peek() == first_task

    another_task_queue = create_task_queue(first_task, second_task)
    another_task_queue.restore(first_task)
    assert not another_task_queue.has_ready()

    another_task_queue.collect(first_task)
    another_task_queue.recompute_ready()
    assert another_task_queue.has_ready()
    assert another_task_queue.peek() == second_task


def test_raises(first_task, second_task):
    task_queue = create_task_queue(first_task, second_task)
    task_queue.restore(first_task)
    with pytest.raises(GardenError):
        task_queue.restore(first_task)


def test_unicode():
    # Two tasks that are using the same write lock, so they should be scheduled after each other
    task1 = TaskStub(set("读锁1"), set("эксклюзивная"))
    task2 = TaskStub(set("读锁2"), set("эксклюзивная"))

    task_queue = TaskQueue()
    task_queue.add(task1, [])
    task_queue.add(task2, [])

    task_queue.recompute_ready()

    assert task_queue.has_ready()
    task = task_queue.pop()
    assert task == task1 or task == task2

    task_queue.recompute_ready()
    assert not task_queue.has_ready()

    task_queue.collect(task)
    task_queue.recompute_ready()
    assert task_queue.has_ready()


def test_merge(first_task, second_task):
    task_queue = TaskQueue()
    assert not task_queue.get_tasks_cache()

    graph = Graph()

    graph.add_vertex(first_task)
    graph.add_edge(second_task, first_task)

    task_queue.merge(graph)
    assert len(task_queue.get_tasks_cache()) == 2

    task_queue.recompute_ready()
    assert task_queue.contains(first_task)
    assert task_queue.contains(second_task)
    assert task_queue.has_ready()
    assert task_queue.peek() == first_task
    assert task_queue.pop_task_ready_timestamp_ms(first_task) != 0
    assert task_queue.pop() == first_task

    assert len(task_queue.get_tasks_cache()) == 2

    task_queue.collect(first_task)
    task_queue.recompute_ready()
    assert task_queue.has_ready()
    assert task_queue.peek() == second_task
    assert task_queue.pop() == second_task
    assert len(task_queue.get_tasks_cache()) == 2


def test_remove(first_task, second_task):
    task_queue = create_task_queue(first_task, second_task)
    assert len(task_queue.get_tasks_cache()) == 2
    assert task_queue.remove_with_dependencies(second_task) == [second_task]
    assert task_queue.remove_with_dependencies(second_task) == []
    assert len(task_queue.get_tasks_cache()) == 1

    task_queue.recompute_ready()
    assert task_queue.peek() == first_task

    task_queue.add(second_task, [first_task])
    assert len(task_queue.get_tasks_cache()) == 2

    removed_tasks = task_queue.remove_with_dependencies(first_task)
    assert len(removed_tasks) == 2
    assert not task_queue.get_tasks_cache()
    assert all(t in [first_task, second_task] for t in removed_tasks)

    task_queue.recompute_ready()
    assert not task_queue.has_ready()


def test_remove2(first_task, second_task):
    task_queue = TaskQueue()
    assert not task_queue.get_tasks_cache()

    graph = Graph()

    graph.add_vertex(first_task)
    graph.add_edge(second_task, first_task)

    task_queue.merge(graph)
    task_queue.recompute_ready()
    assert task_queue.pop() == first_task
    assert task_queue.contains(first_task)

    assert len(task_queue.get_tasks_cache()) == 2

    removed_tasks = task_queue.remove_with_dependencies(first_task)
    assert not task_queue.contains(first_task)
    assert not task_queue.contains(second_task)
    assert len(removed_tasks) == 2
    assert all(t in [first_task, second_task] for t in removed_tasks)

    task_queue.recompute_ready()
    assert not task_queue.has_ready()
    assert not task_queue.get_tasks_cache()
