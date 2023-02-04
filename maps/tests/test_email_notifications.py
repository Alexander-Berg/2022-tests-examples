import datetime
import pytest
import pytz

from maps.garden.sdk.module_traits.module_traits import ModuleTraits, ModuleType
from maps.garden.libs_server.common.exceptions import ExceptionInfo
from maps.garden.libs_server.build.build_defs import Build, BuildStatus
from maps.garden.libs_server.graph.request_storage import FailedTask
from maps.garden.libs_server.test_utils.module_mocks import ModuleManagerMock

from maps.garden.scheduler.lib.notification import email_notifier as email_notifications
from maps.garden.scheduler.lib.notification import utils as notification_utils


def failed_build():
    status = BuildStatus(
        finish_time=datetime.datetime(2017, 4, 25, 9, 0, 0, tzinfo=pytz.utc),
        exception=ExceptionInfo(
            type="maps.garden.sdk.core.GardenError",
            message="Error",
            traceback="Build error traceback",
        ),
        failed_tasks=[
            FailedTask(
                task_name="MyTask",
                task_info="MyTask",
                insert_traceback="Insert traceback",
                filename="Filename",
                line_number=999,
                exception=ExceptionInfo(
                    type="RuntimeError",
                    message="Error message",
                    traceback="Error traceback",
                ),
                failed_at=datetime.datetime(2017, 4, 25, 9, 0, 0, tzinfo=pytz.utc),
                operation_id="yt_operation_id",
                log_url="s3_log_url",
            )
        ]
    )
    return Build(
        contour_name="CONTOUR_NAME",
        id=1,
        name="BUILD_NAME",
        author="BUILD_AUTHOR",
        module_version="123456",
        status=status)


def completed_build():
    return Build(
        contour_name="CONTOUR_NAME",
        id=1,
        name="BUILD_NAME",
        author="BUILD_AUTHOR",
        module_version="123456",
    )


@pytest.fixture
def server_config(mocker):
    server_settings = {
        "garden": {
            "ui_hostname": "core-garden.dev.qloud.maps.yandex.net"
        },
        "smtp": {
            "host": "unittest.smtp.yandex.net",
            "port": 31337
        }
    }
    mocker.patch("maps.garden.libs_server.config.config.server_settings", return_value=server_settings)

    module_manager = ModuleManagerMock({
        "test_module_name": ModuleTraits(
            name="test_module_name",
            type=ModuleType.MAP,
            module_maintainer="test",
        )
    })
    mocker.patch("maps.garden.libs_server.application.state.module_manager", return_value=module_manager)

    return server_settings


def test_completed(server_config, mocker):
    smtp_mock = mocker.patch("smtplib.SMTP", autospec=True)

    email_notifier = email_notifications.EmailNotifier(server_config)
    email_notifier.notify(completed_build(), notification_utils.EventType.COMPLETED)

    smtp_mock.assert_called_once_with("unittest.smtp.yandex.net", 31337)
    smtp_mock.return_value.send_message.assert_called_once()
    smtp_mock.return_value.quit.assert_called_once()

    msg = smtp_mock.return_value.send_message.call_args.args[0]
    assert msg['Subject'] == "Garden: build #1 of BUILD_NAME was completed in CONTOUR_NAME"
    assert msg['From'] == email_notifications.SEND_NOTIFICATION_FROM
    assert msg['To'] == "BUILD_AUTHOR@yandex-team.ru"
    return msg.get_content()  # message body


def test_failed(server_config, mocker):
    smtp_mock = mocker.patch("smtplib.SMTP", autospec=True)

    email_notifier = email_notifications.EmailNotifier(server_config)
    email_notifier.notify(failed_build(), notification_utils.EventType.FAILED)

    smtp_mock.assert_called_once_with("unittest.smtp.yandex.net", 31337)
    smtp_mock.return_value.send_message.assert_called_once()
    smtp_mock.return_value.quit.assert_called_once()

    msg = smtp_mock.return_value.send_message.call_args.args[0]
    assert msg['Subject'] == "Garden: build #1 of BUILD_NAME has failed in CONTOUR_NAME"
    assert msg['From'] == email_notifications.SEND_NOTIFICATION_FROM
    assert msg['To'] == "BUILD_AUTHOR@yandex-team.ru"
    return msg.get_content()  # message body


def test_empty_email_config(server_config, mocker):
    del server_config["smtp"]

    smtp_mock = mocker.patch("smtplib.SMTP", autospec=True)

    email_notifier = email_notifications.EmailNotifier(server_config)
    email_notifier.notify(failed_build(), notification_utils.EventType.FAILED)
    email_notifier.notify(completed_build(), notification_utils.EventType.COMPLETED)

    smtp_mock.assert_not_called()
