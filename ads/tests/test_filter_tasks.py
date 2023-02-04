import pytest
from yabs.conf.utils import AttrDict
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.pipeline import PipelineMessageBus
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.task import Task, GraphState, TasksFlow
from ads.nirvana.online_learning.run_flowkeeper.online_flowkeeper.pipeline.action import \
    remove_similar_tasks_from_task_flow


@pytest.fixture()
def resource_factory():
    return None


@pytest.fixture()
def resource_config():
    return AttrDict({"nirvana": {}})


@pytest.fixture()
def mbus():
    return PipelineMessageBus()


@pytest.fixture()
def task1():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": "2016090112",
        "nirvana_flow_id": "task_flowid1",
        "predecessor_task_id": "task,2016090111",
        "graph_state": GraphState.finished_succeeded,
        "is_leader": False,
        "start_time": "201707191623"
    })
    return task


@pytest.fixture()
def task2():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": "2016090113",
        "nirvana_flow_id": "task_flowid2",
        "predecessor_task_id": "task,2016090112",
        "graph_state": GraphState.finished_succeeded,
        "is_leader": True,
        "start_time": "201707191623"
    })
    return task


@pytest.fixture()
def task3():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": "2016090114",
        "nirvana_flow_id": "task_flowid3",
        "predecessor_task_id": "task,2016090113",
        "graph_state": None,
        "start_time": "201707191622"
    })
    return task


@pytest.fixture()
def task4():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": "2016090113",
        "nirvana_flow_id": "3333",
        "predecessor_task_id": "task,2016090112",
        "graph_state": GraphState.finished_succeeded,
        "is_leader": False,
        "start_time": "201707191623"
    })
    return task


@pytest.fixture()
def task5():
    task = Task()
    task.load_from_dict({
        "ml_task_id": "task",
        "last_log_date": "2016090114",
        "nirvana_flow_id": "task_flowid3",
        "predecessor_task_id": "task,2016090113",
        "graph_state": None,
        "start_time": "201707191621"
    })
    return task


@pytest.fixture()
def tasks_flow(task1, task2, task3):
    return TasksFlow([task1, task2, task3])


def test__remove_unique_last_log_dates(task1, task2, task3, mbus, resource_factory, resource_config):
    tasks_flow = TasksFlow([task1, task2, task3])
    remove_similar_tasks_from_task_flow(task3, tasks_flow, mbus, resource_factory, resource_config)
    assert sorted(tasks_flow._tasks, key=lambda x: x.last_log_date) == \
           sorted([task1, task2, task3], key=lambda x: x.last_log_date)


def test__remove_with_leader(task1, task2, task3, task4, mbus, resource_factory, resource_config):
    tasks_flow = TasksFlow([task1, task2, task3, task4])
    remove_similar_tasks_from_task_flow(task3, tasks_flow, mbus, resource_factory, resource_config)
    assert sorted(tasks_flow._tasks, key=lambda x: x.last_log_date) == \
           sorted([task1, task2, task3], key=lambda x: x.last_log_date)


def test__remove_by_start_time(task1, task2, task3, task5, mbus, resource_factory, resource_config):
    tasks_flow = TasksFlow([task1, task2, task3, task5])
    remove_similar_tasks_from_task_flow(task3, tasks_flow, mbus, resource_factory, resource_config)
    assert sorted(tasks_flow._tasks, key=lambda x: x.last_log_date) == \
           sorted([task1, task2, task3], key=lambda x: x.last_log_date)


def test__remove_with_empty_tasks_list(task5, mbus, resource_factory, resource_config):
    tasks_flow = TasksFlow([])
    remove_similar_tasks_from_task_flow(task5, tasks_flow, mbus, resource_factory, resource_config)
    assert tasks_flow._tasks == []
