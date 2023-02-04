import datetime as dt
import typing as tp
from freezegun import freeze_time
import pytz

from concurrent.futures import ThreadPoolExecutor
from unittest import mock

from maps.pylibs.pydantic_mongo.object_id import ObjectId

from maps.garden.libs_server.task.yt_task import YtTask
from maps.garden.libs_server.task.task_storage import TaskStorage, TaskRecord
from maps.garden.libs_server.yt_task_handler.pymod.constants import MAX_WAIT_TIME_OF_STARTING_YT_JOB
from maps.garden.libs_server.yt_task_handler.pymod.yt_task_tracker import YtTaskTracker, JobFailedError

FREEZE_TIME = dt.datetime(2021, 12, 17, 0, 0, 0, tzinfo=pytz.utc)


def create_yt_task_tracker() -> YtTaskTracker:
    return YtTaskTracker(
        yt_client_config={},
        thread_executor=ThreadPoolExecutor(max_workers=2)
    )


def create_yt_task(
    task_id: int,
    operation_id: tp.Optional[str] = None,
    job_id: tp.Optional[str] = None,
    task_key: tp.Optional[str] = None
) -> YtTask:
    return YtTask(
        task_id=task_id,
        contour_name="unittest",
        module_name="module_name",
        build_id=10,
        consumption={},
        operation_id=operation_id,
        job_id=job_id,
        task_key=task_key,
    )


class YtClientMock:
    class OperationState:
        def __init__(self):
            self.operation_id = None
            self.operation_id_to_state = {}

        def __str__(self):
            return f"operation_id={self.operation_id}"

        def set_id_to_state(self, operation_id: str, state: str):
            self.operation_id_to_state[operation_id] = state

        def is_starting(self):
            if self.operation_id_to_state[self.operation_id] == "starting":
                return True
            else:
                return False

        def is_running(self):
            if self.operation_id_to_state[self.operation_id] == "running":
                return True
            else:
                return False

    def __init__(self):
        self.operation_id = "operation_id"
        self.operation_state = self.OperationState()
        self.jobs_info = {
            "jobs": [
                {
                    "id": "job_id_1",
                    "task_name": "1234",
                    "state": "running"
                },
                {
                    "id": "job_id_2",
                    "task_name": "4321",
                    "state": "completed"
                }
            ]
        }

    def abort_operation(self, operation_id: str):
        assert type(operation_id) is str
        for i, job in enumerate(self.jobs_info["jobs"]):
            if job["state"] == "running":
                self.jobs_info["jobs"][i]["state"] = "aborted"

    def abort_job(self, job_id: str):
        assert type(job_id) is str
        for i, job in enumerate(self.jobs_info["jobs"]):
            if job["id"] == job_id:
                self.jobs_info["jobs"][i]["state"] = "aborted"

    def list_jobs(self, operation_id: str) -> dict[str, any]:
        assert type(operation_id) is str
        return self.jobs_info

    def get_operation_state(self, operation_id: str):
        self.operation_state.operation_id = operation_id
        return self.operation_state


@mock.patch("maps.garden.libs_server.yt_task_handler.pymod.yt_task_tracker.YtTaskTracker._get_thread_local_yt_client")
def test_abort_operation(get_yt_client_mock: mock.MagicMock):
    yt_task_tracker = create_yt_task_tracker()
    yt_client_mock = YtClientMock()
    get_yt_client_mock.return_value = yt_client_mock

    # Check successful abortion
    yt_task_tracker.abort_operation(yt_client_mock.operation_id)
    assert yt_client_mock.jobs_info["jobs"][0]["state"] == "aborted"

    # Check that abortion of the same operation will not happened
    yt_client_mock.jobs_info["jobs"][0]["state"] = "running"
    yt_task_tracker.abort_operation(yt_client_mock.operation_id)
    assert yt_client_mock.jobs_info["jobs"][0]["state"] == "running"


@freeze_time(FREEZE_TIME + 2 * MAX_WAIT_TIME_OF_STARTING_YT_JOB)
@mock.patch("maps.garden.libs_server.yt_task_handler.pymod.yt_task_tracker.YtTaskTracker._get_thread_local_yt_client")
def test_check_task(get_yt_client_mock: mock.MagicMock, db: mock.MagicMock):
    yt_task_tracker = create_yt_task_tracker()
    yt_client_mock = YtClientMock()
    get_yt_client_mock.return_value = yt_client_mock

    task_key_3 = "61bc6b301e04a842e12e9c53"
    task_key_4 = "61bc6b301e04a842e12e9c54"

    task_1 = create_yt_task(task_id=1234, operation_id="1234")
    task_2 = create_yt_task(task_id=4321, operation_id="4321")
    task_3 = create_yt_task(task_id=1111, operation_id="1111", task_key=task_key_3)
    task_4 = create_yt_task(task_id=1112, operation_id="1112", task_key=task_key_4)

    task_storage = TaskStorage(db)

    # expect task_3 will produce an exception
    task_record_3 = _generate_task_record(
        yt_task=task_3,
        created_at=FREEZE_TIME,  # created more than MAX_WAIT_TIME_OF_STARTING_YT_JOB ago
        task_key=task_key_3
    )
    yt_client_mock.get_operation_state(task_3.operation_id).set_id_to_state(task_3.operation_id, "running")
    task_storage.insert(task_record_3)

    # expect task_4 will update task_record and not raise an exception
    task_record_4 = _generate_task_record(
        yt_task=task_4,
        created_at=FREEZE_TIME + 1.5 * MAX_WAIT_TIME_OF_STARTING_YT_JOB,
        task_key=task_key_4
    )
    yt_client_mock.get_operation_state(task_4.operation_id).set_id_to_state(task_4.operation_id, "running")
    task_storage.insert(task_record_4)

    # Check running task
    task_error_1 = yt_task_tracker.check_task_on_yt(task_1, task_storage)
    assert yt_client_mock.jobs_info["jobs"][0]["state"] == "aborted"
    assert isinstance(task_error_1.exception, JobFailedError)

    # Check completed task
    task_error_2 = yt_task_tracker.check_task_on_yt(task_2, task_storage)
    assert yt_client_mock.jobs_info["jobs"][1]["state"] == "completed"
    assert isinstance(task_error_2.exception, JobFailedError)

    # Check not started tasks
    task_error_3 = yt_task_tracker.check_task_on_yt(task_3, task_storage)
    assert isinstance(task_error_3.exception, JobFailedError)
    assert task_storage.find_by_key(task_key_3).updated_at == FREEZE_TIME

    task_error_4 = yt_task_tracker.check_task_on_yt(task_4, task_storage)
    assert task_error_4 is None
    assert task_storage.find_by_key(task_key_4).updated_at == dt.datetime.now(pytz.utc)


def _generate_task_record(
    yt_task: YtTask,
    created_at: dt.datetime,
    task_key: str = None,
) -> TaskRecord:
    return TaskRecord(
        mongo_id=ObjectId(task_key),
        name=str(),
        garden_task_id=yt_task.task_id,
        module_name=str(),
        build_id=int(),
        contour_name=str(),
        input_proto=bytes(),
        created_at=created_at,
        updated_at=FREEZE_TIME
    )
