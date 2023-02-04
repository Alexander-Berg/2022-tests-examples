import datetime as dt
import http.client
import pytest
import pytz

from maps.garden.libs_server.graph import request_storage
from maps.garden.libs_server.common import exceptions
from maps.garden.libs_server.build import build_defs

NOW = dt.datetime(2020, 12, 17, 15, 1, 22, tzinfo=pytz.utc)

MODULE_NAME = "ymapsdf"
BUILD_ID_1 = 123
BUILD_ID_2 = 456
SVN_REVISION = "12345"
ARC_HASH = "5c793b2c8f899191b2b946e56382f28d31af3487"


def test_error_log_no_error(garden_client, db):
    build = build_defs.Build(
        id=BUILD_ID_1,
        name=MODULE_NAME,
        contour_name="test_contour",
        status=build_defs.BuildStatus(
            string=build_defs.BuildStatusString.COMPLETED,
        ),
    )
    db.builds.insert_one(build.dict())

    response = garden_client.get(f"/modules/{MODULE_NAME}/builds/{BUILD_ID_1}/errors/")
    assert response.status_code == http.client.OK
    return response.get_json()


@pytest.mark.parametrize(
    "build_id, revision",
    [
        (BUILD_ID_1, SVN_REVISION),
        (BUILD_ID_2, ARC_HASH)
    ]
)
def test_error_log(garden_client, db, build_id, revision):
    build = build_defs.Build(
        id=build_id,
        name=MODULE_NAME,
        module_version=revision,
        contour_name="test_contour",
        status=build_defs.BuildStatus(
            string=build_defs.BuildStatusString.FAILED,
            failed_tasks=[
                request_storage.FailedTask(
                    task_name="MyTask",
                    task_info="MyTask",
                    insert_traceback="Insert traceback",
                    filename="Filename",
                    line_number=999,
                    exception=exceptions.ExceptionInfo(
                        type="RuntimeError",
                        message="Error message https://npro.maps.yandex.ru/#!/objects/53000001 <main>\n  два пробела",
                        traceback="Error traceback",
                    ),
                    failed_at=dt.datetime(2017, 4, 25, 9, 0, 0, tzinfo=pytz.utc),
                    operation_id="yt_operation_id",
                    log_url="s3_log_url",
                    task_id="1234567890",
                )
            ],
        ),
    )
    db.builds.insert_one(build.dict())

    response = garden_client.get(f"/modules/{MODULE_NAME}/builds/{build_id}/errors/")
    assert response.status_code == http.client.OK

    return response.get_json()


@pytest.mark.freeze_time(NOW)
def test_error_log_from_build_manipulator(garden_client, db):
    build = build_defs.Build(
        id=BUILD_ID_1,
        name=MODULE_NAME,
        contour_name="test_contour",
        status=build_defs.BuildStatus.create_failed("Error message"),
    )
    db.builds.insert_one(build.dict())

    response = garden_client.get(f"/modules/{MODULE_NAME}/builds/{BUILD_ID_1}/errors/")
    assert response.status_code == http.client.OK
    return response.get_json()


@pytest.mark.parametrize(
    (
        "exception",
        "is_ignored",
        "is_sink",
    ),
    [
        ("DataValidationWarning", False, False),
        ("DataValidationWarning", False, True),
        ("DataValidationWarning", True, True),
        ("GardenError", False, False)
    ]
)
@pytest.mark.freeze_time(NOW)
def test_error_log_exceptions(garden_client, db, exception, is_ignored, is_sink):
    build = build_defs.Build(
        id=BUILD_ID_1,
        name=MODULE_NAME,
        contour_name="test_contour",
        module_version=ARC_HASH,
        status=build_defs.BuildStatus(
            string=build_defs.BuildStatusString.FAILED,
            failed_tasks=[
                request_storage.FailedTask(
                    task_name="MyTask",
                    task_info="MyTask",
                    insert_traceback="Insert traceback",
                    filename="Filename",
                    line_number=999,
                    task_id="123",
                    exception=exceptions.ExceptionInfo(
                        type=exception,
                        message="Error message https://npro.maps.yandex.ru/#!/objects/53000001 <main>\n  два пробела",
                        traceback="Error traceback",
                    ),
                    failed_at=dt.datetime(2017, 4, 25, 9, 0, 0, tzinfo=pytz.utc),
                    operation_id="yt_operation_id",
                    log_url="s3_log_url",
                    is_sink=is_sink,
                )
            ],
        ),
    )

    if is_ignored:
        build.ignorable_tasks = ["123"]

    db.builds.insert_one(build.dict())

    response = garden_client.get(f"/modules/{MODULE_NAME}/builds/{BUILD_ID_1}/errors/")
    assert response.status_code == http.client.OK
    return response.get_json()
