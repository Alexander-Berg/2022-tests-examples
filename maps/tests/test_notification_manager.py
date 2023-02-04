import pytest

from maps.pylibs.utils.lib.common import wait_until
from maps.pylibs.yandex_environment import environment as yenv

from maps.garden.libs_server.build.build_defs import Build

from maps.garden.scheduler.lib.notification.manager import NotificationManager
import maps.garden.scheduler.lib.notification.email_notifier as email
import maps.garden.scheduler.lib.notification.infra as infra
import maps.garden.scheduler.lib.notification.startrek as startrek
import maps.garden.scheduler.lib.notification.utils as utils


_NOTIFICATION_CLASSES = [
    infra.InfraNotifier,
    email.EmailNotifier,
    startrek.StarTrek,
]


@pytest.mark.parametrize(
    ("event_type", "classes"),
    [
        (
            utils.EventType.STARTED,
            [
                infra.InfraNotifier,
            ],
        ),
        (
            utils.EventType.COMPLETED,
            [
                infra.InfraNotifier,
                email.EmailNotifier,
            ],
        ),
        (
            utils.EventType.FAILED,
            _NOTIFICATION_CLASSES,
        ),
    ],
)
def test_exception_safe(mocker, monkeypatch, event_type, classes):
    """
    Notification manager must process all events even if they throw exceptions

    Use `on_build_failed` because it calls all the notifiers.
    Also use `object()` because all the notifiers are mocked.
    """
    exception = RuntimeError("Must not interrupt notifications")

    for cls in _NOTIFICATION_CLASSES:
        mocker.patch.object(cls, "__init__", return_value=None)

    mocked_objects = [
        mocker.patch.object(cls, "notify", side_effect=exception)
        for cls in classes
    ]

    monkeypatch.setattr(yenv, "get_yandex_environment", lambda: yenv.Environment.TESTING)
    config = {"infra": {"test": "test"}, "startrek": {"test": "test"}}

    with NotificationManager(object(), config) as manager:
        build = Build(name="test", id=1, contour_name="name")
        manager._notify(build, event_type)
        for mocked in mocked_objects:
            assert wait_until(lambda m=mocked: m.called), mocked
            assert mocked.call_args.args == (build, event_type)
