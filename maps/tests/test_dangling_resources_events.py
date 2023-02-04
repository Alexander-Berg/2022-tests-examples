import pytest
import datetime as dt
import pytz
from maps.garden.sdk.resources import PythonResource
from maps.garden.libs_server.common.exceptions import ExceptionInfo
from maps.garden.libs_server.resource_storage.dangling_resource_manager import DanglingRecord
from maps.garden.tools.module_monitoring.lib.dangling_resources import generate_dangling_resources_events

NOW = dt.datetime(2020, 12, 25, 17, 45, 00, tzinfo=pytz.utc)


class _FakeDanglingResourcesManager:
    def get_all(self):
        resources = [
            DanglingRecord(
                resource=PythonResource(name="OK"),
                inserted_at=NOW,
                size=1
            ),
            DanglingRecord(
                resource=PythonResource(name="Warn delete try count"),
                inserted_at=NOW,
                size=1,
                delete_try_count=40,
                last_exception=ExceptionInfo(
                    type="CantRemoveException",
                    message="some message",
                    traceback="some stack trace"
                )
            ),
            DanglingRecord(
                resource=PythonResource(name="Warn time"),
                inserted_at=(NOW - dt.timedelta(days=4)),
                size=1
            ),
            DanglingRecord(
                resource=PythonResource(name="Warn time, warn retry"),
                inserted_at=(NOW - dt.timedelta(days=14)),
                size=1,
                delete_try_count=40
            ),
        ]
        yield from resources


@pytest.mark.freeze_time(NOW)
def test_dangling_resources_events(mocker):
    mocker.patch.object(PythonResource, "key", return_value="123", new_callable=mocker.PropertyMock)
    dangling_resource_manager = _FakeDanglingResourcesManager()
    events = generate_dangling_resources_events(dangling_resource_manager, source_config={"message_limit": 100})
    return list(events)
