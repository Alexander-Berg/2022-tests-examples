import http.client
import unittest

from .common import (
    API_ENDPOINT,
    SOLVER_TEST_APIKEY,
    CLOUD_TYPE,
    get_task_result,
    create_task,
    start_task,
    requests_get_with_retry,
    requests_post_with_retry,
)

TEST_BUCKET = "b2bgeo-test" if CLOUD_TYPE == 'rtc' else 'yandex-asyncsolver-testing-test'  # else is aws
# you can check AWS bucket here: https://s3.console.aws.amazon.com/s3/buckets/yandex-asyncsolver-testing?region=us-east-2&prefix=YC/&showversions=false
INVALID_BUCKET = "invalid-bucket-name"


def check_request_with_buckets(request_url, invalid_bucket_status_code=None):
    request_url_with_bucket = f"{request_url}"
    resp = requests_get_with_retry(request_url_with_bucket, params={"bucket": TEST_BUCKET})
    assert resp.ok, f"GET request {request_url_with_bucket} failed with code {resp.status_code} and text:\n{resp.text}"

    resp = requests_get_with_retry(request_url)
    assert (
        resp.status_code == http.client.GONE if invalid_bucket_status_code is None else invalid_bucket_status_code
    ), f"GET request {request_url} returned unexpected status code: {resp.status_code}, text:\n{resp.text}"

    request_url_invalid_bucket = f"{request_url}"
    resp = requests_get_with_retry(request_url_invalid_bucket, params={"bucket": INVALID_BUCKET})
    assert (
        resp.status_code == http.client.BAD_REQUEST if invalid_bucket_status_code is None else invalid_bucket_status_code
    ), f"GET request {request_url_invalid_bucket} returned unexpected status code: {resp.status_code}, text:\n{resp.text}"


class S3BucketsTest(unittest.TestCase):
    def test_custom_bucket(self):
        task = create_task()

        url_post_task = f"{API_ENDPOINT}/add/mvrp"
        task_id = start_task(url_post_task, task, {"apikey": SOLVER_TEST_APIKEY, "bucket": TEST_BUCKET})

        url_get_result = f"{API_ENDPOINT}/result/{task_id}"
        j = get_task_result(url_get_result, task_id, params={"bucket": TEST_BUCKET})
        assert j["message"] == "Task successfully completed"

        url_get_result = f"{API_ENDPOINT}/result/{task_id}"
        check_request_with_buckets(url_get_result, invalid_bucket_status_code=http.client.FORBIDDEN)

        url_get_log_request = f"{API_ENDPOINT}/log/request/{task_id}"
        check_request_with_buckets(url_get_log_request)

        url_get_log_response = f"{API_ENDPOINT}/log/response/{task_id}"
        check_request_with_buckets(url_get_log_response, invalid_bucket_status_code=http.client.FORBIDDEN)

        url_get_log_stderr= f"{API_ENDPOINT}/log/stderr/{task_id}"
        check_request_with_buckets(url_get_log_stderr, invalid_bucket_status_code=http.client.FORBIDDEN)

    def test_post_task_with_invalid_custom_bucket(self):
        task = create_task()

        url_post_task = f"{API_ENDPOINT}/add/mvrp"
        resp = requests_post_with_retry(url_post_task, json=task, params={"apikey": SOLVER_TEST_APIKEY, "bucket": INVALID_BUCKET})

        assert (
            resp.status_code == http.client.BAD_REQUEST
        ), f"POST request {url_post_task} returned unexpected status code {resp.status_code}, text:\n{resp.text}"
        assert resp.json()["error"]["message"] == "Invalid request parameters."
