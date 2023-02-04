import datetime as dt

from maps.garden.libs_server.task import task_graph
from maps.garden.libs_server.task.task_graph import Task

NOW = dt.datetime(2020, 8, 25, 16, 20, 00)


def test_longest_path():
    tasks = [
        Task(
            key="1",
            started_at=(NOW + dt.timedelta(seconds=0)),
            finished_at=(NOW + dt.timedelta(seconds=10)),
            demands=["a"],
            creates=["b"],
        ),
        Task(
            key="2",
            started_at=(NOW + dt.timedelta(seconds=0)),
            finished_at=(NOW + dt.timedelta(seconds=11)),
            demands=["a"],
            creates=["c"],
        ),
        Task(
            key="3",
            started_at=(NOW + dt.timedelta(seconds=11)),
            finished_at=(NOW + dt.timedelta(seconds=15)),
            demands=["b", "c"],  # depends on tasks 1 and 2, but the 2nd task is longer
            creates=["d"],
        ),
        # Retry of task 3
        Task(
            key="4",
            started_at=(NOW + dt.timedelta(seconds=15)),
            finished_at=(NOW + dt.timedelta(seconds=20)),
            demands=["b", "c"],
            creates=["d"],
        ),
        Task(
            key="5",
            started_at=(NOW + dt.timedelta(seconds=20)),
            finished_at=(NOW + dt.timedelta(seconds=25)),
            demands=["b", "d"],
            creates=["e"],
        ),
    ]
    longest_path = task_graph.calculate_longest_path(tasks)

    assert longest_path.duration.total_seconds() == 21  # <total execution time> - <execution time of retried task 3>
    assert longest_path.task_keys == ["2", "4", "5"]
