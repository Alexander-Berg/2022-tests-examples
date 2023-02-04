import datetime as dt
import pytest
import pytz
from itertools import chain, repeat

from maps.pylibs.yandex_environment import environment as yenv

from maps.garden.sdk.utils.contour import default_contour_name
from maps.garden.sdk.core import AutotestsFailedError

from maps.garden.sdk.module_traits.module_traits import (
    CheckConfig,
    StatusCheckConfig,
)
from maps.garden.libs_server.graph.request_storage import FailedTask
from maps.garden.libs_server.common.exceptions import ExceptionInfo
from maps.garden.libs_server.build.build_defs import BuildStatusString

from maps.garden.libs_server.monitorings import (
    juggler,
    latest_build_status_check,
)

NOW = dt.datetime(2020, 10, 9, 13, 40, 0, tzinfo=pytz.utc)


@pytest.mark.freeze_time(NOW)
def test_message_in_progress(status_checker):
    status_checker.add(BuildStatusString.IN_PROGRESS, hours_ago=17, extras={"region": "cis1"})
    code, msg = status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.CRIT, msg
    return msg


@pytest.mark.freeze_time(NOW)
def test_message_completed(status_checker):
    status_checker.add(BuildStatusString.COMPLETED, hours_ago=200, extras={"region": "cis1"})
    code, msg = status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.WARN, msg
    return msg


def test_message_failed(status_checker):
    status_checker.add(
        BuildStatusString.FAILED,
        failed_tasks=[
            FailedTask(
                task_name="MyTask",
                task_info="MyTaskInfo",
                insert_traceback="Insert traceback",
                exception=ExceptionInfo(
                    type="maps.garden.plugins.ecstatic.tasks.EcstaticError",
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
    code, msg = status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.CRIT, msg
    return msg


def test_message_failed_long_text(status_checker):
    long_text = "\n".join(
        letter * 10
        for letter in chain.from_iterable(repeat("abcdefghijklmnopqrstuvwxyz", 10))
    )

    status_checker.add(
        BuildStatusString.FAILED,
        failed_tasks=[
            FailedTask(
                task_name="MyTask",
                task_info="MyTaskInfo",
                insert_traceback="Insert traceback",
                exception=ExceptionInfo(
                    type="maps.garden.plugins.ecstatic.tasks.EcstaticError",
                    message=long_text,
                    traceback="Error traceback",
                ),
                failed_at=dt.datetime(2020, 9, 17, 10, 50, 53, tzinfo=pytz.utc),
            )
        ],
        extras={"region": "cis1"})
    code, msg = status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.CRIT, msg
    return msg


def test_latest_build_status_no_config(status_checker):

    status_checker.add(BuildStatusString.IN_PROGRESS, hours_ago=17)
    code, msg = status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.CRIT, msg

    status_checker.add(BuildStatusString.IN_PROGRESS, hours_ago=13)
    code, msg = status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.WARN, msg

    status_checker.add(BuildStatusString.IN_PROGRESS, hours_ago=9)
    code, msg = status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.OK, msg
    assert not msg

    status_checker.add(BuildStatusString.FAILED)
    code, msg = status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.CRIT, msg
    assert BuildStatusString.IN_PROGRESS not in msg.lower()


def test_latest_build_status_invalid_timedelta():
    with pytest.raises(RuntimeError):
        StatusCheckConfig(warn_age='1 hour')


@pytest.mark.parametrize(
    'single_status_checker',
    [{'in_progress': StatusCheckConfig(warn_age='1h', crit_age='3h')}],
    indirect=True,
)
def test_latest_build_status_no_builds_yet(single_status_checker):
    code, msg = single_status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.WARN
    assert 'no builds' in msg.lower()


@pytest.mark.parametrize(
    'single_status_checker',
    [{'in_progress': StatusCheckConfig(warn_age='1h', crit_age='3h')}],
    indirect=True,
)
def test_latest_build_status_no_status(single_status_checker):
    single_status_checker.add(BuildStatusString.REMOVING, hours_ago=8)
    code, msg = single_status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.OK, msg


@pytest.mark.parametrize(
    'single_status_checker',
    [{'completed': StatusCheckConfig(disabled=True)}],
    indirect=True,
)
def test_latest_build_status_disabled(single_status_checker):
    single_status_checker.add(
        BuildStatusString.COMPLETED,
        hours_ago=130 * 24,
    )
    code, msg = single_status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.OK, msg


@pytest.mark.parametrize(
    'single_status_checker',
    [{'completed': StatusCheckConfig(warn_age='1h', crit_age='3h')}],
    indirect=True,
)
def test_latest_build_status_cancelled(single_status_checker):
    single_status_checker.add(BuildStatusString.CANCELLED, hours_ago=4)
    code, msg = single_status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.WARN, msg

    single_status_checker.add(BuildStatusString.CANCELLED, hours_ago=2)
    code, msg = single_status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.WARN, msg

    single_status_checker.add(BuildStatusString.CANCELLED, hours_ago=0)
    code, msg = single_status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.OK, msg


@pytest.mark.parametrize(
    'single_status_checker',
    [{'waiting': StatusCheckConfig(crit_age='0h')}],
    indirect=True,
)
def test_latest_build_status_ignored(single_status_checker):
    single_status_checker.add(BuildStatusString.WAITING, hours_ago=4)
    code, msg = single_status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.OK, msg


@pytest.mark.parametrize(
    ('exception_name', 'status_string', 'status_code'),
    [
        (
            'random_module.name.' + AutotestsFailedError.__name__,
            BuildStatusString.FAILED,
            juggler.StatusCode.OK,
        ),
        (
            'AnotherExceptionName',
            BuildStatusString.FAILED,
            juggler.StatusCode.CRIT,
        ),
    ],
)
def test_latest_build_status_failed_task(
    status_checker,
    exception_name,
    status_string,
    status_code,
):
    failed_task = FailedTask(
        task_name="MyTask",
        task_info="MyTask",
        insert_traceback="Insert traceback",
        exception=ExceptionInfo(
            type=exception_name,
            message="Error message",
            traceback="Error traceback",
        ),
        failed_at=dt.datetime(2020, 9, 17, 10, 50, 53, tzinfo=pytz.utc),
    )
    status_checker.add(status_string, failed_tasks=[failed_task])
    code, msg = status_checker.check_generic_build_status()
    assert code == status_code, msg


def test_latest_build_status_ignore_contour(status_checker):
    status_checker.add(
        BuildStatusString.FAILED,
        contour_name=str(yenv.Environment.STABLE),
    )
    code, msg = status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.WARN, msg

    status_checker.add(
        BuildStatusString.FAILED,
        contour_name=default_contour_name(),
    )
    code, msg = status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.CRIT, msg


@pytest.mark.parametrize(
    'single_status_checker',
    [{
        'in_progress': StatusCheckConfig(warn_age='1h', crit_age='3h'),
        'failed': StatusCheckConfig(crit_age='5h'),
    }],
    indirect=True,
)
def test_latest_build_status_lifecycle(single_status_checker):
    def check_build_lifecycle(hours_ago, code_in_progress, code_failed):
        def check_result(code_expected, time):
            code, msg = single_status_checker.check_generic_build_status()
            assert code == code_expected, msg
            if code != juggler.StatusCode.OK:
                assert latest_build_status_check.format_datetime(time) in msg
            else:
                assert not msg

        time, build_id = single_status_checker.add(BuildStatusString.IN_PROGRESS, hours_ago)
        check_result(code_in_progress, time)
        single_status_checker.update_status(build_id, BuildStatusString.FAILED)
        check_result(code_failed, time)

    check_build_lifecycle(hours_ago=6, code_in_progress=juggler.StatusCode.CRIT, code_failed=juggler.StatusCode.CRIT)
    check_build_lifecycle(hours_ago=4, code_in_progress=juggler.StatusCode.CRIT, code_failed=juggler.StatusCode.WARN)
    check_build_lifecycle(hours_ago=2, code_in_progress=juggler.StatusCode.WARN, code_failed=juggler.StatusCode.WARN)
    check_build_lifecycle(hours_ago=0, code_in_progress=juggler.StatusCode.OK, code_failed=juggler.StatusCode.WARN)


@pytest.mark.parametrize(
    'status_checker',
    [[
        CheckConfig(
            statuses_config={
                'in_progress': StatusCheckConfig(warn_age='1h', crit_age='3h'),
            },
            group_by=['region'],
        ),
        CheckConfig(
            statuses_config={
                'in_progress': StatusCheckConfig(warn_age='5h', crit_age='5h'),
            },
            group_by=['very_special'],
        ),
    ]],
    indirect=True,
)
def test_latest_build_status_multiple_configs(status_checker):
    def check_build_grouping(hours_ago, code_expected):

        extras_generic = {'region': 'cis1'}
        time_generic, _ = status_checker.add(
            BuildStatusString.IN_PROGRESS,
            hours_ago,
            extras_generic,
        )

        extras_special = {'region': 'cis2', 'very_special': 'very'}
        time_special, _ = status_checker.add(
            BuildStatusString.IN_PROGRESS,
            hours_ago,
            extras_special,
        )

        code, msg = status_checker.check_generic_build_status()
        assert code == code_expected, msg
        if code > 0:
            assert latest_build_status_check.format_datetime(time_generic) in msg
            assert latest_build_status_check.format_datetime(time_special) in msg

    check_build_grouping(hours_ago=6, code_expected=juggler.StatusCode.CRIT)
    check_build_grouping(hours_ago=4, code_expected=juggler.StatusCode.CRIT)
    check_build_grouping(hours_ago=2, code_expected=juggler.StatusCode.WARN)
    check_build_grouping(hours_ago=0, code_expected=juggler.StatusCode.OK)


@pytest.mark.parametrize(
    'status_checker',
    [[
        CheckConfig(
            statuses_config={
                'in_progress': StatusCheckConfig(warn_age='5h', crit_age='5h'),
            },
            group_by=['very_special'],
        ),
    ]],
    indirect=True,
)
def test_latest_build_status_special_config(status_checker):
    extras_generic = {'region': 'cis1'}
    extras_special = {'region': 'cis2', 'very_special': 'very'}

    status_checker.add(
        BuildStatusString.IN_PROGRESS,
        hours_ago=6,
        extras=extras_generic,
    )

    code, msg = status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.WARN, msg
    assert 'no builds' in msg.lower()

    status_checker.add(
        BuildStatusString.IN_PROGRESS,
        hours_ago=6,
        extras=extras_special,
    )
    code, msg = status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.CRIT, msg

    status_checker.add(
        BuildStatusString.IN_PROGRESS,
        hours_ago=4,
        extras=extras_generic,
    )
    code, msg = status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.CRIT, msg

    status_checker.add(
        BuildStatusString.IN_PROGRESS,
        hours_ago=4,
        extras=extras_special,
    )
    code, msg = status_checker.check_generic_build_status()
    assert code == juggler.StatusCode.OK, msg


@pytest.mark.parametrize(
    ('status_string', 'failed_tasks', 'ignorable_tasks', 'status_code'),
    [
        (
            BuildStatusString.IN_PROGRESS,
            None,
            None,
            juggler.StatusCode.OK,
        ),
        (
            BuildStatusString.IN_PROGRESS,
            [FailedTask(
                task_name="MyTask",
                task_info="MyTask",
                insert_traceback="Insert traceback",
                exception=ExceptionInfo(
                    type="exception_name",
                    message="Error message",
                    traceback="Error traceback",
                ),
                failed_at=dt.datetime(2020, 9, 17, 10, 50, 53, tzinfo=pytz.utc),
                task_id="123456",
            )],
            None,
            juggler.StatusCode.CRIT,
        ),
        (
            BuildStatusString.IN_PROGRESS,
            [FailedTask(
                task_name="IgnoredTask",
                task_info="IgnoredTask",
                insert_traceback="Insert traceback",
                exception=ExceptionInfo(
                    type="exception_name",
                    message="Error message",
                    traceback="Error traceback",
                ),
                failed_at=dt.datetime(2020, 9, 17, 10, 50, 53, tzinfo=pytz.utc),
                task_id="123456"
            )],
            ["123456"],
            juggler.StatusCode.OK,
        ),
        (
            BuildStatusString.IN_PROGRESS,
            [FailedTask(
                task_name="IgnoredTask",
                task_info="IgnoredTask",
                insert_traceback="Insert traceback",
                exception=ExceptionInfo(
                    type="exception_name",
                    message="Error message",
                    traceback="Error traceback",
                ),
                failed_at=dt.datetime(2020, 9, 17, 10, 50, 53, tzinfo=pytz.utc),
                task_id="123456"
            ),
            FailedTask(
                task_name="NotIgnoredTask",
                task_info="NotIgnoredTask",
                insert_traceback="Insert traceback",
                exception=ExceptionInfo(
                    type="exception_name",
                    message="Error message",
                    traceback="Error traceback",
                ),
                failed_at=dt.datetime(2020, 9, 17, 10, 50, 53, tzinfo=pytz.utc),
                task_id="0000000"
            ),
            ],
            ["123456"],
            juggler.StatusCode.CRIT,
        ),
        (
            BuildStatusString.COMPLETED,
            [FailedTask(
                task_name="IgnoredTask",
                task_info="IgnoredTask",
                insert_traceback="Insert traceback",
                exception=ExceptionInfo(
                    type="exception_name",
                    message="Error message",
                    traceback="Error traceback",
                ),
                failed_at=dt.datetime(2020, 9, 17, 10, 50, 53, tzinfo=pytz.utc),
                task_id="123456"
            )],
            ["123456"],
            juggler.StatusCode.OK,
        ),
    ],
)
def test_latest_build_status_with_ignored_failed_task(
    status_checker,
    status_string,
    failed_tasks,
    ignorable_tasks,
    status_code,
):
    status_checker.add(status_string, failed_tasks=failed_tasks, ignorable_tasks=ignorable_tasks)
    code, msg = status_checker.check_generic_build_status()
    assert code == status_code, msg
