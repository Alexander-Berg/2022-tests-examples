from maps.garden.sdk.sandbox import batch_client

FAKE_TOKEN = ""
BATCH_SIZE = 10
SANDBOX_API = "https://sandbox.yandex-team.ru/api/v1.0/"


def test_delete_tasks(requests_mock):
    client = batch_client.SandboxBatchTask(FAKE_TOKEN)

    requests_mock.put(SANDBOX_API + "batch/tasks/delete",
                      request_headers={'Content-Type': 'application/json'},
                      additional_matcher=lambda data: data.text == '{"id": [1234]}',
                      json=[{"status": "SUCCESS", "task_id": 1234}])

    client.delete_tasks([1234], batch_size=BATCH_SIZE)
