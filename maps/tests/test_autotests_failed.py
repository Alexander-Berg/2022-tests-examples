import datetime as dt
import pytest
import pytz

from maps.garden.sdk.core import AutotestsFailedError

from maps.garden.sdk.module_traits.module_traits import (
    CheckConfig,
    StatusCheckConfig,
)
from maps.garden.libs_server.graph.request_storage import FailedTask
from maps.garden.libs_server.common.exceptions import ExceptionInfo
from maps.garden.libs_server.build.build_defs import BuildStatusString

from maps.garden.libs_server.monitorings import juggler


def test_no_builds_yet(status_checker):
    code, msg = status_checker.check_autotests_failed()
    assert code == juggler.StatusCode.OK, msg


def test_no_failed_task(status_checker):
    status_checker.add(BuildStatusString.FAILED)
    code, msg = status_checker.check_autotests_failed()
    assert code == juggler.StatusCode.OK, msg


def test_autotests_message(status_checker):
    status_checker.add(
        BuildStatusString.FAILED,
        failed_tasks=[
            FailedTask(
                task_name="MyTask",
                task_info="MyTaskInfo",
                insert_traceback="Insert traceback",
                exception=ExceptionInfo(
                    type='random_module.name.' + AutotestsFailedError.__name__,
                    message="""\
Hello, world!
Привет, мир!
https://npro.maps.yandex.ru/#!/objects/53068948
DROP TABLE students;
</body>""",
                    traceback="Error traceback",
                ),
                failed_at=dt.datetime(2020, 9, 17, 10, 50, 53, tzinfo=pytz.utc),
            )
        ],
        extras={"region": "cis1"})
    code, msg = status_checker.check_autotests_failed()
    assert code == juggler.StatusCode.CRIT, msg
    return msg


@pytest.mark.parametrize(
    ('exception_name', 'status_string', 'status_code'),
    [
        (
            'random_module.name.' + AutotestsFailedError.__name__,
            BuildStatusString.FAILED,
            juggler.StatusCode.CRIT,
        ),
        (
            'AnotherExceptionName',
            BuildStatusString.FAILED,
            juggler.StatusCode.OK,
        ),
        (
            'another_module.name.' + AutotestsFailedError.__name__,
            BuildStatusString.IN_PROGRESS,
            juggler.StatusCode.CRIT,
        ),
    ],
)
def test_failed_task(
    status_checker,
    exception_name,
    status_string,
    status_code,
):
    failed_task = FailedTask(
        exception=ExceptionInfo(
            type=exception_name,
            message="Error message",
            traceback="Error traceback",
        ),
        failed_at=dt.datetime(2020, 9, 17, 10, 50, 53, tzinfo=pytz.utc),
    )
    status_checker.add(status_string, failed_tasks=[failed_task])
    code, msg = status_checker.check_autotests_failed()
    assert code == status_code, msg


def test_failed_before_but_now_ok(status_checker):
    failed_task = FailedTask(
        exception=ExceptionInfo(
            type='another_module.name.' + AutotestsFailedError.__name__,
            message="Error message",
            traceback="Error traceback",
        ),
        failed_at=dt.datetime(2020, 9, 17, 10, 50, 53, tzinfo=pytz.utc),
    )
    status_checker.add(
        BuildStatusString.FAILED,
        failed_tasks=[failed_task],
        hours_ago=10,
    )
    code, msg = status_checker.check_autotests_failed()
    assert code == juggler.StatusCode.CRIT, msg

    status_checker.add(BuildStatusString.COMPLETED)
    code, msg = status_checker.check_autotests_failed()
    assert code == juggler.StatusCode.OK, msg


@pytest.mark.parametrize(
    'status_checker',
    [[
        CheckConfig(
            statuses_config={'failed': StatusCheckConfig(crit_age='100000d')},
            group_by=['region'],
        ),
        CheckConfig(
            statuses_config={'failed': StatusCheckConfig(disabled=True)},
        ),
    ]],
    indirect=True,
)
def test_latest_build_status_multiple_configs(status_checker):
    failed_task = FailedTask(
        exception=ExceptionInfo(
            type='another_module.name.' + AutotestsFailedError.__name__,
            message="Error message",
            traceback="Error traceback",
        ),
        failed_at=dt.datetime(2020, 9, 17, 10, 50, 53, tzinfo=pytz.utc),
    )

    status_checker.add(
        BuildStatusString.IN_PROGRESS,
        failed_tasks=[failed_task],
    )
    code, msg = status_checker.check_autotests_failed()
    assert code == juggler.StatusCode.OK, msg

    extras_generic = {'region': 'cis1'}
    status_checker.add(
        BuildStatusString.IN_PROGRESS,
        extras=extras_generic,
        failed_tasks=[failed_task],
    )
    code, msg = status_checker.check_autotests_failed()
    assert code == juggler.StatusCode.WARN, msg
