import dataclasses
import datetime
import pytz
import pytest
from itertools import repeat

from maps.garden.sdk.core import AutotestsFailedError, DataValidationWarning

from maps.garden.sdk.module_traits.module_traits import (
    ModuleTraits,
    ModuleType,
)
from maps.garden.libs_server.graph.request_storage import FailedTask
from maps.garden.libs_server.infrastructure_clients import startrek as startrek_client
from maps.garden.libs_server.common.contour_manager import ContourManager
from maps.garden.libs_server.common.exceptions import ExceptionInfo
from maps.garden.libs_server.build.build_defs import Build, BuildStatus, BuildStatusString
from maps.garden.libs_server.build.build_manager import BuildManager
from maps.garden.libs_server.test_utils.module_mocks import ModuleManagerMock

from maps.garden.scheduler.lib.notification import startrek, utils


BASE_URL = "http://base_url/v2"
QUEUE = "MAPSGARDEN"
NOW = datetime.datetime(2020, 12, 25, 17, 45, 00)

ST_CONFIG = {
    "url": BASE_URL,
    "queue": QUEUE,
    "add_assignee": True,
    "add_followers": True,
    "token": "<wrong token>"
}

server_settings = {
    "startrek": ST_CONFIG,
    "garden": {
        "ui_hostname": "core-garden.dev.qloud.maps.yandex.net"
    }
}


class FakeABCClient:
    def __init__(self, server_settings) -> None:
        pass

    def get_module_followers(self, module_name) -> list[str]:
        return ['vasya']


@pytest.fixture
def patched_state(db, mocker):

    mocker.patch("maps.garden.scheduler.lib.notification.startrek.abc.Client", FakeABCClient)
    mocker.patch("maps.garden.libs_server.config.config.server_settings", return_value=server_settings)

    module_manager = ModuleManagerMock({
        "test_module_name": ModuleTraits(
            name="test_module_name",
            type=ModuleType.MAP,
            module_maintainer="test",
        )
    })
    mocker.patch("maps.garden.libs_server.application.state.module_manager", return_value=module_manager)

    contour_manager = ContourManager(db)
    contour_manager.create("stable", "admin", is_system=True)
    contour_manager.create("testing", "admin", is_system=True)
    mocker.patch("maps.garden.libs_server.application.state.contour_manager", return_value=contour_manager)

    build_manager = BuildManager(db, module_event_storage=None)
    mocker.patch("maps.garden.libs_server.application.state.build_manager", return_value=build_manager)


def test_format_args(patched_state):
    build = Build(
        contour_name="test_contour_name",
        name="test_module_name",
        id=1,
        author="test_author",
        module_version="123456",
    )

    args = utils.get_notification_template_kwargs(build)
    assert args["contour_name"] == build.contour_name
    assert args["module_name"] == build.name
    assert args["build_id"] == build.id
    assert args["author"] == build.author
    assert "failed_tasks" not in args


def test_render(patched_state, mocker):
    mocker.patch("maps.garden.scheduler.lib.notification.utils.MAX_MESSAGE_LINES", 10)

    st = startrek.StarTrek(server_settings)

    build = Build(
        contour_name="test_contour_name",
        name="test_module_name",
        id=1,
        author="igor",
        module_version="123456",
        extras={
            "shipping_date": "20201103",
            "region": "cis1",
        },
        status=BuildStatus(
            finish_time=datetime.datetime(2017, 4, 25, 9, 0, 0, tzinfo=pytz.utc),
            exception=ExceptionInfo(
                type="maps.garden.sdk.core.GardenError",
                message="Error",
                traceback="\n".join(repeat("Build traceback line", 10)),
            ),
            failed_tasks=[
                FailedTask(
                    task_name="MyTask",
                    task_info="MyTaskInfo",
                    insert_traceback="Insert traceback",
                    filename="filename",
                    line_number=999,
                    exception=ExceptionInfo(
                        type="maps.garden.sdk.core.GardenError",
                        message="Failed description: сдпвнцба šđčćǋǈ",
                        traceback="\n".join(repeat("Traceback line", 10)),
                    ),
                    failed_at=datetime.datetime(2017, 4, 25, 9, 0, 0, tzinfo=pytz.utc),
                    operation_id="yt_operation_id",
                    log_url="s3_log_url",
                ),
                FailedTask(
                    task_name="MyTask",
                    task_info="\n".join(repeat("Task info line", 10)),
                    insert_traceback="\n".join(repeat("Insert traceback line", 10)),
                    filename="filename",
                    line_number=999,
                    exception=ExceptionInfo(
                        type="maps.garden.sdk.core.GardenError",
                        message="\n".join(repeat("Very very long message", 100)),
                        traceback="\n".join(repeat("Traceback line", 100)),
                    ),
                    failed_at=datetime.datetime(2017, 4, 25, 9, 0, 0, tzinfo=pytz.utc),
                    operation_id="yt_operation_id",
                    log_url="s3_log_url",
                ),
            ],
        ),
    )

    args = utils.get_notification_template_kwargs(build)
    return st.build_failed_template.render(**args)


@pytest.mark.freeze_time(NOW)
@pytest.mark.parametrize(
    (
        "exception_name",
        "contour_name",
    ),
    [
        (
            "OtherException",
            "stable",
        ),
        (
            AutotestsFailedError.__name__,
            "testing",
        ),
        (
            DataValidationWarning.__name__,
            "testing",
        ),
    ]
)
def test_notify_failed_ok(
    db,
    patched_state,
    mocker,
    exception_name,
    contour_name,
):
    """
    Test description creation and theirs content
    """
    module_name = "test_module_name"
    components = {
        "modules": 1,
        f"modules-{module_name}": 2,
        "autotests": 3,
    }
    mocker.patch.object(
        startrek_client.Client,
        "upsert_component",
        side_effect=lambda _, name: components[name],
    )

    st_notifier = startrek.StarTrek(server_settings)

    mocked_issue = mocker.patch.object(
        startrek_client.Client,
        "create_issue",
        return_value={"key": "MAPSGARDENBUILD-123"}
    )

    build = Build(
        id=1,
        name=module_name,
        contour_name=contour_name,
        module_version="123456",
        author="igor",
        extras={
            "shipping_date": "20201103",
            "region": "cis1",
        },
        status=BuildStatus(
            string=BuildStatusString.FAILED,
            failed_tasks=[
                FailedTask(
                    task_name="MyTask",
                    task_info="MyTaskInfo",
                    insert_traceback="insert traceback",
                    filename="filename",
                    line_number=999,
                    failed_at=datetime.datetime(2017, 4, 25, 9, 0, 0, tzinfo=pytz.utc),
                    exception=ExceptionInfo(
                        type=exception_name,
                        message="Message",
                        traceback="Traceback",
                    ),
                    operation_id="yt_operation_id",
                    log_url="s3_log_url",
                ),
            ],
        ),
    )
    db.builds.insert_one(build.dict())

    st_notifier.notify(build, utils.EventType.FAILED)

    mocked_issue.assert_called_once()
    assert build.startrek_issue_key == "MAPSGARDENBUILD-123"
    assert db.builds.find_one({})["startrek_issue_key"] == "MAPSGARDENBUILD-123"

    return dataclasses.asdict(mocked_issue.call_args.args[0])


def test_components_access_failed(patched_state, mocker):
    """
    The notifier must raise an exception in case of access failure
    """
    error_message = "Error message for test"
    mocker.patch.object(
        startrek_client.Client,
        "upsert_component",
        side_effect=RuntimeError(error_message),
    )

    st_notifier = startrek.StarTrek(server_settings)

    build = Build(
        id=1,
        name="test_module_name",
        contour_name="stable",
        module_version="123456",
        status=BuildStatus(
            string=BuildStatusString.FAILED,
            failed_tasks=[
                FailedTask(
                    filename="Filename",
                    line_number=999,
                    failed_at=datetime.datetime(2017, 4, 25, 9, 0, 0, tzinfo=pytz.utc),
                    exception=ExceptionInfo(
                        type=AutotestsFailedError.__name__,
                        message="",
                        traceback="",
                    ),
                    task_name="MyTask",
                    task_info="MyTask",
                ),
            ],
        ),
    )

    with pytest.raises(RuntimeError) as exception:
        st_notifier.notify(build, utils.EventType.FAILED)
    assert str(exception.value) == error_message
