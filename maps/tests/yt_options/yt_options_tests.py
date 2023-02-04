import pytest
from maps.b2bgeo.mvrp_solver.backend.tests_lib.yt_common import run_yt_task


def test_run_small_task_parallel(async_backend_url_task_sizes):
    backend_url = async_backend_url_task_sizes

    # small task - should not run on YT
    result = run_yt_task(backend_url, 1,
                         save_as="task_small_local")
    assert len(result["response"]["yt_operations"]) == 0

    # small task with quality 'low' - should not run on YT
    result = run_yt_task(backend_url, 1, override_test_options={"quality": "low"},
                         save_as="task_small_local_low")
    assert len(result["response"]["yt_operations"]) == 0

    # small task with thread_count > 1 - should run on YT
    result = run_yt_task(backend_url, 1, override_test_options={"quality": "low", "thread_count": 2},
                         save_as="task_small_local_low_thread2")
    assert len(result["response"]["yt_operations"]) == 2

    # small task with task_count > 1 - should run on YT
    result = run_yt_task(backend_url, 1, override_test_options={"quality": "low", "task_count": 2},
                         save_as="task_small_local_low_task2")
    assert len(result["response"]["yt_operations"]) == 2

    # small task with quality 'normal' - should not run on YT
    result = run_yt_task(backend_url, 1, override_test_options={"quality": "normal"},
                         save_as="task_small_local_normal")
    assert len(result["response"]["yt_operations"]) == 0

    # small task with quality 'high' - should not run on YT
    result = run_yt_task(backend_url, 1, override_test_options={"quality": "high"},
                         save_as="task_small_local_high")
    assert len(result["response"]["yt_operations"]) == 0


def test_restart_on_drop_task_parallel(async_backend_url):
    backend_url = async_backend_url

    # restart_on_drop, explicit task_count => restart_on_drop tasks on/off: 2/2
    override_test_options = {
        "restart_on_drop": True,
        "task_count": 4
    }
    result = run_yt_task(backend_url, 2, override_test_options=override_test_options,
                         save_as="task_restart_on_drop")
    assert len(result["response"]["yt_operations"]) == 2
    tasks_summary = result["response"]["result"]["metrics"]["_tasks_summary"]
    assert len(tasks_summary) == 4
    assert sum([1 if task["options"]["restart_on_drop"] else 0 for task in tasks_summary]) == 2
    assert sum([1 if task["options"]["solver_time_limit_s"] == 1 else 0 for task in tasks_summary]) == 2

    # restart_on_drop, quality=normal, default task_count, small size => 1 task with restart_on_drop
    override_test_options = {
        "restart_on_drop": True,
        "quality": "normal"
    }
    result = run_yt_task(backend_url, 2, override_test_options=override_test_options,
                         save_as="task_restart_on_drop_small_normal")
    assert len(result["response"]["yt_operations"]) == 0
    tasks_summary = result["response"]["result"]["metrics"]["_tasks_summary"]
    assert len(tasks_summary) == 1
    assert sum([1 if task["options"]["restart_on_drop"] else 0 for task in tasks_summary]) == 1
    assert sum([1 if task["options"]["solver_time_limit_s"] == 1 else 0 for task in tasks_summary]) == 1

    # We use thread_count == 5 because the default value 30 is too big for a local YT node
    thread_count = 5

    # restart_on_drop, quality=normal, default task_count, medium size => 4 tasks, restart_on_drop tasks on/off: 2/2
    override_test_options = {
        "restart_on_drop": True,
        "quality": "normal",
        "thread_count": thread_count
    }
    expected_task_count = 4
    expected_restart_on_drop = int(0.5 + expected_task_count / 2)  # half of the tasks with restart_on_drop=true
    result = run_yt_task(backend_url, 2, override_test_options=override_test_options,
                         expected_task_count=expected_task_count, location_count=200,
                         save_as="task_restart_on_drop_medium_normal")
    assert len(result["response"]["yt_operations"]) == 2
    tasks_summary = result["response"]["result"]["metrics"]["_tasks_summary"]
    assert len(tasks_summary) == expected_task_count
    assert sum([1 if task["options"]["restart_on_drop"] else 0 for task in tasks_summary]) == expected_restart_on_drop
    assert sum([1 if task["options"]["solver_time_limit_s"] == 1 else 0 for task in tasks_summary]) == expected_restart_on_drop


def test_run_low_temperature_task_parallel(async_backend_url):
    backend_url = async_backend_url

    result = run_yt_task(backend_url, 1, override_test_options={"task_count": 2},
                         save_as="task_low_temperature_0")
    assert len(result["response"]["yt_operations"]) == 2
    assert sum([1 if task["options"]["temperature"] == 500 else 0
                for task in result["response"]["result"]["metrics"]["_tasks_summary"]]) == 0

    result = run_yt_task(backend_url, 1, override_test_options={"task_count": 2, "temperature": 10000},
                         save_as="task_low_temperature_T")
    assert len(result["response"]["yt_operations"]) == 2
    assert sum([1 if task["options"]["temperature"] == 500 else 0
                for task in result["response"]["result"]["metrics"]["_tasks_summary"]]) == 0

    result = run_yt_task(backend_url, 1, override_test_options={"task_count": 2, "quality": "low"},
                         save_as="task_low_temperature_low")
    assert len(result["response"]["yt_operations"]) == 2
    assert sum([1 if task["options"]["temperature"] == 500 else 0
                for task in result["response"]["result"]["metrics"]["_tasks_summary"]]) == 0

    result = run_yt_task(backend_url, 1, override_test_options={"task_count": 2, "quality": "normal"},
                         save_as="task_low_temperature_normal")
    assert len(result["response"]["yt_operations"]) == 2
    assert sum([1 if task["options"]["temperature"] == 500 else 0
                for task in result["response"]["result"]["metrics"]["_tasks_summary"]]) == 1

    result = run_yt_task(backend_url, 1, override_test_options={"task_count": 2, "quality": "high"},
                         save_as="task_low_temperature_high")
    assert len(result["response"]["yt_operations"]) == 2
    assert sum([1 if task["options"]["temperature"] == 500 else 0
                for task in result["response"]["result"]["metrics"]["_tasks_summary"]]) == 1


def test_run_medium_task_parallel(async_backend_url):
    backend_url = async_backend_url
    task_count_normal_high = 4
    location_count = 200

    # medium task
    result = run_yt_task(backend_url, 2,
                         expected_task_count=1, location_count=location_count, save_as="task_medium")
    assert len(result["response"]["yt_operations"]) == 0

    # medium task, quality low
    result = run_yt_task(backend_url, 2, override_test_options={"quality": "low"},
                         expected_task_count=1, location_count=location_count, save_as="task_medium_low")
    assert len(result["response"]["yt_operations"]) == 0

    # We use thread_count == 5 because the default value 30 is too big for a local YT node
    thread_count = 5

    # medium task, quality normal
    result = run_yt_task(backend_url, 2, override_test_options={"quality": "normal", "thread_count": thread_count},
                         expected_task_count=task_count_normal_high, location_count=location_count,
                         save_as="task_medium_normal")
    assert len(result["response"]["yt_operations"]) == 2

    # medium task, quality high
    result = run_yt_task(backend_url, 2, override_test_options={"quality": "high", "thread_count": thread_count},
                         expected_task_count=task_count_normal_high, location_count=location_count,
                         save_as="task_medium_high")
    assert len(result["response"]["yt_operations"]) == 2


def test_run_medium_task_parallel_proto(async_backend_url_proto):
    backend_url = async_backend_url_proto
    task_count_normal_high = 4
    location_count = 200

    # We use thread_count == 5 because the default value 30 is too big for a local YT node
    thread_count = 5

    # medium task, quality normal
    result = run_yt_task(backend_url, 2, override_test_options={"quality": "normal", "thread_count": thread_count},
                         expected_task_count=task_count_normal_high, location_count=location_count,
                         save_as="task_medium_normal")
    assert len(result["response"]["yt_operations"]) == 2


def test_run_large_task_parallel(async_backend_url_task_sizes):
    backend_url = async_backend_url_task_sizes
    task_count_normal_high = 4
    cluster_count = 2
    location_count = 50

    # large task
    result = run_yt_task(backend_url, 10,
                         expected_task_count=1, location_count=location_count, save_as="task_large")
    assert len(result["response"]["yt_operations"]) == 0

    # large task, quality low
    result = run_yt_task(backend_url, 10, override_test_options={"quality": "low"},
                         expected_task_count=1, location_count=location_count, save_as="task_large_low")
    assert len(result["response"]["yt_operations"]) == 0

    # large task, quality normal
    result = run_yt_task(backend_url, 20, override_test_options={"quality": "normal"},
                         expected_task_count=task_count_normal_high, location_count=location_count,
                         save_as="task_large_normal")
    assert len(result["response"]["yt_operations"]) == cluster_count

    # large task, quality high
    result = run_yt_task(backend_url, 20, override_test_options={"quality": "high"},
                         expected_task_count=task_count_normal_high, location_count=location_count,
                         save_as="task_large_high")
    assert len(result["response"]["yt_operations"]) == cluster_count


@pytest.mark.parametrize("task_size, location_count", [("huge", 60), ("xhuge", 70), ("xxhuge", 80)])
def test_run_huge_task_parallel(async_backend_url_task_sizes, task_size, location_count):
    backend_url = async_backend_url_task_sizes
    task_count_huge_low = 2
    task_count_normal_high = 4
    cluster_count = 2

    # We use thread_count == 5 because the default value 30 (for 'huge') is too big for a local YT node
    thread_count = 5

    # huge task, quality low
    result = run_yt_task(backend_url, 20, override_test_options={"quality": "low", "thread_count": thread_count},
                         expected_task_count=task_count_huge_low, location_count=location_count,
                         save_as=f"task_{task_size}_low")
    assert len(result["response"]["yt_operations"]) == cluster_count

    # huge task, quality normal
    result = run_yt_task(backend_url, 20, override_test_options={"quality": "normal", "thread_count": thread_count},
                         expected_task_count=task_count_normal_high, location_count=location_count,
                         save_as=f"task_{task_size}_normal")
    assert len(result["response"]["yt_operations"]) == cluster_count

    # huge task, quality high
    result = run_yt_task(backend_url, 20, override_test_options={"quality": "high", "thread_count": thread_count},
                         expected_task_count=task_count_normal_high, location_count=location_count,
                         save_as=f"task_{task_size}_high")
    assert len(result["response"]["yt_operations"]) == cluster_count
