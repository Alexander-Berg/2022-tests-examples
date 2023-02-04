import datetime as dt

import mongomock
import pytest

from maps.garden.tools.module_monitoring.lib.hanging_builds import generate_hanging_builds_event

from maps.garden.libs_server.build.build_defs import Build, BuildStatus, BuildStatusString
from maps.garden.libs_server.build.builds_storage import BuildsStorage


@pytest.mark.parametrize(
    "builds",
    [
        [],
        [
            {"status": BuildStatusString.COMPLETED, "time_diff": dt.timedelta()},
            {"status": BuildStatusString.COMPLETED, "time_diff": dt.timedelta(minutes=1)},
            {"status": BuildStatusString.COMPLETED, "time_diff": dt.timedelta(days=1)},
        ],
        [
            {"status": BuildStatusString.WAITING, "time_diff": dt.timedelta()},
            {"status": BuildStatusString.SCHEDULING, "time_diff": dt.timedelta(minutes=1)},
            {"status": BuildStatusString.WAITING, "time_diff": dt.timedelta(days=1)},
        ],
        [
            {"status": BuildStatusString.SCHEDULING, "time_diff": dt.timedelta(hours=1)},
            {"status": BuildStatusString.WAITING, "time_diff": dt.timedelta(days=1)},
        ],
    ],
)
def test_hanging_builds(supervisor_pidfile_ctime, builds, freezer):
    mocked_db = mongomock.MongoClient(tz_aware=True).db
    builds_storage = BuildsStorage(mocked_db)
    now = supervisor_pidfile_ctime + dt.timedelta(hours=1)
    freezer.move_to(now)

    for i, build in enumerate(builds):
        builds_storage.save(Build(
            id=i,
            name="test_name",
            contour_name="test_contour",
            created_at=now - build["time_diff"],
            status=BuildStatus(string=build["status"]),
            extras={},
        ))

    return list(generate_hanging_builds_event(builds_storage))


def test_supervisor_just_started(supervisor_pidfile_ctime, freezer):
    freezer.move_to(supervisor_pidfile_ctime)
    return list(generate_hanging_builds_event(object()))
