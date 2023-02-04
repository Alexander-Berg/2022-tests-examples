import json

import yatest.common

from maps.garden.libs_server.task.yt_stats import FullYtStats, BriefYtStats


def test_full_stats():
    operation = _load_json_from_file_resource("data/map_operation.json")
    return FullYtStats.from_yt_operation(operation).dict()


def test_brief_stats():
    operation = _load_json_from_file_resource("data/map_operation.json")
    return BriefYtStats.from_yt_operation(operation).dict()


def test_default_stats():
    operation = _load_json_from_file_resource("data/running_vanilla_operation.json")
    task_id = "17876499775571658829"
    return FullYtStats.from_yt_operation(operation, task_id).dict()


def test_total_stats_for_vanilla_with_multiple_jobs():
    operation = _load_json_from_file_resource("data/finished_vanilla_multiple_tasks.json")
    return BriefYtStats.from_yt_operation(operation).dict()


def test_task_stats_for_vanilla_with_multiple_jobs():
    operation = _load_json_from_file_resource("data/finished_vanilla_multiple_tasks.json")
    job = _load_json_from_file_resource("data/task1_job.json")
    return BriefYtStats.from_yt_operation(operation, job).dict()


def _load_json_from_file_resource(file_path):
    file_path = yatest.common.source_path("maps/garden/libs_server/task/tests/{}".format(file_path))
    with open(file_path, "r") as f:
        return json.load(f)
