import requests
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    wait_task, API_KEY, LIMITED_API_KEY)
from maps.b2bgeo.test_lib.sanitizers_utils import sanitizer_aware_timeout

from util import run_task


def _test_completed(async_backend_url, api_key, location_count, uniq_location_count=None, error=None):
    response = run_task(async_backend_url, api_key, location_count, uniq_location_count=uniq_location_count,
                        quality='low')
    assert response.ok
    task_id = response.json()['id']
    response = requests.get(async_backend_url + "/result/" + task_id)
    assert response.ok

    j = wait_task(async_backend_url, task_id, timeout=sanitizer_aware_timeout(30, 120))

    assert "status" in j
    if error:
        assert "error" in j
        assert j["error"]["message"] == error
    else:
        assert "calculated" in j["status"]
        assert j["message"] == 'Task successfully completed', j["message"]


def _test_error(async_backend_url, api_key, location_count, uniq_location_count=None, error=None):
    response = run_task(async_backend_url, api_key, location_count, uniq_location_count=uniq_location_count,
                        quality='low')
    assert response.status_code == 400
    j = response.json()
    assert "error" in j
    if error:
        assert j["error"]["message"] == error


def test_limited_api_key(async_backend_url):
    _test_completed(async_backend_url, LIMITED_API_KEY, 1000)
    _test_error(async_backend_url, LIMITED_API_KEY, 1001)


def test_unlimited_api_key(async_backend_url_location_count_limit):
    async_backend_url = async_backend_url_location_count_limit
    _test_completed(async_backend_url, API_KEY, 100,
                    error="Number of unique locations 101 is greater than limit: 100")  # 101 = 100 locations + 1 depot
    _test_completed(async_backend_url, API_KEY, 100, uniq_location_count=99)
    _test_error(async_backend_url, API_KEY, 201,
                error="Number of orders in planning task should not exceed 200 order(s)")


def test_default_concurrent_tasks_limit(async_backend_small_default_concurrent_task_limit):
    for _ in range(2):
        response = run_task(async_backend_small_default_concurrent_task_limit, API_KEY, 10, 60, quality='low')
        assert response.ok
    response = run_task(async_backend_small_default_concurrent_task_limit, API_KEY, 10, 60, quality='low')
    assert response.status_code == requests.codes.too_many_requests
    assert 'Your limit is 2 concurrent tasks' in response.json()['error']['message']


def test_default_xxhuge_concurrent_tasks_limit_two_huge(async_backend_url_task_sizes):
    response = run_task(async_backend_url_task_sizes, API_KEY, 90, uniq_location_count=5)
    assert response.ok
    task_id = response.json()['id']

    response = run_task(async_backend_url_task_sizes, API_KEY, 90, uniq_location_count=5)
    assert response.status_code == requests.codes.too_many_requests
    assert 'Limit for concurrent xxhuge task number exceeded.' in response.json()['error']['message']

    wait_task(async_backend_url_task_sizes, task_id, timeout=sanitizer_aware_timeout(30, 120))


def test_default_xxhuge_concurrent_tasks_limit_one_huge_one_small(async_backend_url_task_sizes):
    response = run_task(async_backend_url_task_sizes, API_KEY, 90, uniq_location_count=5)
    assert response.ok
    task_id = response.json()['id']

    response = run_task(async_backend_url_task_sizes, API_KEY, 10)
    assert response.ok

    wait_task(async_backend_url_task_sizes, task_id, timeout=sanitizer_aware_timeout(30, 120))


def test_individual_concurrent_tasks_limit(async_backend_small_individual_concurrent_task_limit):
    for _ in range(3):
        response = run_task(async_backend_small_individual_concurrent_task_limit, API_KEY, 10, 60, quality='low')
        assert response.ok
    response = run_task(async_backend_small_individual_concurrent_task_limit, API_KEY, 10, 60, quality='low')
    assert response.status_code == requests.codes.too_many_requests
    assert 'Your limit is 3 concurrent tasks' in response.json()['error']['message']
