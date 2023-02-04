import pytest
from util import get_task_value, wait_task_completed, start_task, get_task_result


@pytest.mark.skip(reason="test usually fails, zone detection api is not used")
def test_simple(async_backend_url):
    task_value = get_task_value()
    task_id = start_task(async_backend_url, task_value)

    j = wait_task_completed(async_backend_url, task_id)
    assert "result" in j

    task_result = get_task_result()
    task_result["id"] = task_id
    assert j == task_result


@pytest.mark.skip(reason="zone detection api is not used")
def test_invalid_request(async_backend_url):
    task_value = get_task_value()
    del task_value["zones"][0]["geometry"]["type"]
    task_id = start_task(async_backend_url, task_value)

    j = wait_task_completed(async_backend_url, task_id)
    assert "error" in j
    assert j["error"] == {"message": "Value at `/zones/0/geometry/type' does not exist"}


@pytest.mark.skip(reason="zone detection api is not used")
def test_empty_request(async_backend_url):
    task_value = ""
    task_id = start_task(async_backend_url, task_value)

    j = wait_task_completed(async_backend_url, task_id)
    assert "error" in j
    assert j["error"] == {"message": "Expected object, got string at `'"}
