from maps.pylibs.yandex_environment import environment as yenv

from maps.garden.libs.auth.auth_client import HttpAuth
from maps.garden.libs.jns_client import client

from maps.garden.sdk.core import Task
from maps.garden.sdk.core.task import get_caller_task


class MockAuth(HttpAuth):
    def __init__(self, tvm_secret: str, source_tvm_id: int, destination_tvm_id: int) -> None:
        self._secret = f"{tvm_secret}:{source_tvm_id}:{destination_tvm_id}"

    def get_garden_server_auth_headers(self):
        return {"X-Ya-Service-Ticket": self._secret}


class MyTask(Task):
    def __call__(self):
        return get_caller_task()


def test_get_caller_task():
    # There is no task in the stack
    assert get_caller_task() is None

    # There is a task in the stack
    task = MyTask()
    assert task() is task


def test_task_notify(requests_mock, mocker):
    request_data = {
        "project": client.JNS_GARDEN_PROJECT,
        "template": client.JNS_TASK_TEMPLATE,
        "target_project": client.JNS_GARDEN_PROJECT,
        "channel": client.JNS_GARDEN_DEVELOPMENT_CHANNEL,
        "params": {
            "message": "message",
            "task_name": "MyTask",
            "module_name": "module_name",
            "contour_name": str(yenv.Environment.STABLE),
        }
    }

    def matcher(data):
        return data.json() == request_data

    requests_mock.post(
        client.JNS_CHANNEL_URL,
        status_code=200,
        request_headers={
            "Content-Type": "application/json",
            "X-Ya-Service-Ticket": f"secret:123:{client.JNS_TVM_ID}",
        },
        additional_matcher=matcher,
    )

    task = MyTask()
    task.load_environment_settings({
        "garden": {
            "tvm_client": {
                "token": "secret",
                "source_client_id": 123,
            },
        }
    })

    task.module_name = "module_name"
    mocker.patch("maps.garden.sdk.core.task.yenv.get_yandex_environment", return_value=yenv.Environment.STABLE)

    jns_config = client.JnsConfig(
        stable=[
            client.JnsTarget(
                project=client.JNS_GARDEN_PROJECT,
                channel=client.JNS_GARDEN_DEVELOPMENT_CHANNEL,
            )
        ]
    )

    mocker.patch("maps.garden.sdk.core.task.TVMServiceAuth", MockAuth)
    task.notify(jns_config, "message")

    assert requests_mock.called
