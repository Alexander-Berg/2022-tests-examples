from maps.garden.sdk.core import Task, TaskGraphBuilder
from maps.garden.modules.ymapsdf_osm.lib.utils import YmapsdfOsmGraphBuild


class MyTask(Task):
    pass


def test_builder():
    task = MyTask()

    graph_builder = YmapsdfOsmGraphBuild(TaskGraphBuilder())
    graph_builder.add_task_more(
        create_final_tables=["ad"],
        task=task,
    )

    assert "test_builder.py" in task.insert_traceback_raw[-1].filename
