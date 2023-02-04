import datetime as dt
import pytz
import pytest

from maps.garden.sdk.module_traits.module_traits import ModuleTraits, ModuleType, BuildsLimit, SortOption

from maps.garden.libs_server.common.scheduler import SchedulerAdaptor
from maps.garden.libs_server.log_storage.module_event_storage import ModuleEventStorage
from maps.garden.libs_server.log_storage.module_log_storage import ModuleLogStorage
from maps.garden.libs_server.build import build_defs
from maps.garden.libs_server.build import build_manager as bm
from maps.garden.libs_server.build import checks
from maps.garden.libs_server.build import module_builds_manager as mbm
from maps.garden.libs_server.test_utils.module_mocks import ModuleManagerMock

from maps.garden.scheduler.lib import module_builds_cleaner as mbc

import logging
logger = logging.getLogger("module_builds_cleaner")

NOW = dt.datetime(2020, 8, 19, 15, 25, 22, tzinfo=pytz.utc)


@pytest.fixture
def config_mock(db, mocker):
    mocker.patch("maps.garden.scheduler.lib.module_builds_cleaner.POLLING_INTERVAL_SEC", 1)

    module_log_storage = ModuleLogStorage(db)

    mocker.patch(
        "maps.garden.libs_server.application.state.module_log_storage",
        return_value=module_log_storage,
    )


class BuildsHelper:
    def __init__(self, db):
        self._db = db
        self._build_id = 0

    def create(
        self,
        module_name,
        status=build_defs.BuildStatusString.COMPLETED,
        finish_time=None,
        pinned=False,
        contour_name="contour_name",
        properties=None,
        sources=None,
    ):
        self._build_id += 1
        if sources:
            sources = [build_defs.Source.generate_from(source_build) for source_build in sources]
        build = build_defs.Build(
            id=self._build_id,
            name=module_name,
            contour_name=contour_name,
            extras=properties or {},
            pinned=pinned,
            created_at=(finish_time or NOW) - dt.timedelta(hours=1),
            status=build_defs.BuildStatus(
                string=status,
                finish_time=finish_time or NOW,
                updated_at=finish_time or NOW,
            ),
            sources=sources or [],
        )
        self._db.builds.insert_one(build.dict())
        return build

    def assert_removed(self, *builds: build_defs.Build):
        removed = {
            f"{row['module_name']}:{row['build_id']}"
            for row in self._db.build_actions.find({"operation": build_defs.BuildOperationString.REMOVE})
        }

        expected = {
            build.full_id
            for build in builds
        }

        assert removed == expected


def create_cleaner(db, scheduler_mock, module_traits):
    module_manager_mock = ModuleManagerMock(module_traits)

    build_manager = bm.BuildManager(db, ModuleEventStorage(db))

    module_builds_manager = mbm.ModuleBuildsManager(
        module_manager=module_manager_mock,
        build_manager=build_manager,
        resource_storage=None,
        contour_manager=None,
    )

    return mbc.ModuleBuildsCleaner(
        module_manager=module_manager_mock,
        build_manager=build_manager,
        module_builds_manager=module_builds_manager,
        delay_executor=SchedulerAdaptor(scheduler_mock),
    )


def test_no_limits(db, scheduler_mock, thread_executor_mock, config_mock):
    module_traits = {
        "test_module": ModuleTraits(
            name="test_module",
            type=ModuleType.SOURCE,
            build_limits=[],
            sort_options=[SortOption(key_pattern="test_pattern")])
    }

    builds_helper = BuildsHelper(db)
    builds_helper.create("test_module")
    builds_helper.create("test_module")

    module_builds_cleaner = create_cleaner(db, scheduler_mock, module_traits)
    with module_builds_cleaner:
        assert module_builds_cleaner._uncleaned_candidates == set()

        # activate background removal procedure
        scheduler_mock.execute_background_task()

        assert module_builds_cleaner._uncleaned_candidates == set()

    builds_helper.assert_removed()

    # No limits => no build removal attempts => nothing to write to log
    assert not db.module_logs.count({})


def test_simple_limit(db, scheduler_mock, thread_executor_mock, config_mock):
    build_limit = BuildsLimit(
        max=1,
        remove_on_exceed=True,
        ignore_warnings=True,
    )
    module_traits = {
        "test_module": ModuleTraits(
            name="test_module",
            type=ModuleType.SOURCE,
            build_limits=[build_limit],
            sort_options=[SortOption(key_pattern="test_pattern")])
    }

    builds_helper = BuildsHelper(db)
    build1 = builds_helper.create("test_module")
    builds_helper.create("test_module")

    module_builds_cleaner = create_cleaner(db, scheduler_mock, module_traits)
    with module_builds_cleaner:
        # activate background removal procedure
        scheduler_mock.execute_background_task()

        assert module_builds_cleaner._uncleaned_candidates == set()

    builds_helper.assert_removed(build1)

    # Build was successfully removed => nothing to write to log
    assert not db.module_logs.count({})


def test_limit_with_grouping(db, scheduler_mock, thread_executor_mock, config_mock):
    build_limit = BuildsLimit(
        builds_grouping=["region"],
        max=1,
        remove_on_exceed=True,
        ignore_warnings=True,
    )
    module_traits = {
        "test_module": ModuleTraits(
            name="test_module",
            type=ModuleType.SOURCE,
            build_limits=[build_limit],
            sort_options=[SortOption(key_pattern="test_pattern")])
    }

    builds_helper = BuildsHelper(db)
    build1 = builds_helper.create("test_module", properties={"region": "cis1"})
    build2 = builds_helper.create("test_module", properties={"region": "cis2"})
    builds_helper.create("test_module", properties={"region": "cis1"})
    builds_helper.create("test_module", properties={"region": "cis2"})

    module_builds_cleaner = create_cleaner(db, scheduler_mock, module_traits)
    with module_builds_cleaner:
        # activate background removal procedure
        scheduler_mock.execute_background_task()

        assert module_builds_cleaner._uncleaned_candidates == set()

    builds_helper.assert_removed(build1, build2)

    # Builds were successfully removed => nothing to write to log
    assert not db.module_logs.count({})


def test_no_remove_on_exceed(db, scheduler_mock, thread_executor_mock, config_mock):
    build_limit = BuildsLimit(
        max=1,
        remove_on_exceed=False,
        ignore_warnings=True,
    )
    module_traits = {
        "test_module": ModuleTraits(
            name="test_module",
            type=ModuleType.SOURCE,
            build_limits=[build_limit],
            sort_options=[SortOption(key_pattern="test_pattern")])
    }

    builds_helper = BuildsHelper(db)
    builds_helper.create("test_module")
    builds_helper.create("test_module")

    module_builds_cleaner = create_cleaner(db, scheduler_mock, module_traits)
    with module_builds_cleaner:
        assert module_builds_cleaner._uncleaned_candidates == set()

        # activate background removal procedure
        scheduler_mock.execute_background_task()

        assert module_builds_cleaner._uncleaned_candidates == set()

    builds_helper.assert_removed()

    # FIXME: should we write to the log that a limit exceeded?
    assert not db.module_logs.count({})


@pytest.mark.freeze_time(NOW)
def test_no_ignore_warnings(db, scheduler_mock, thread_executor_mock, config_mock):
    build_limit = BuildsLimit(
        max=1,
        remove_on_exceed=True,
        ignore_warnings=False,
    )
    module_traits = {
        "source_module": ModuleTraits(
            name="source_module",
            type=ModuleType.SOURCE,
            build_limits=[build_limit],
            sort_options=[SortOption(key_pattern="test_pattern")]),
        "map_module": ModuleTraits(
            name="map_module",
            type=ModuleType.MAP,
            build_limits=[]),
        "another_map_module": ModuleTraits(
            name="another_map_module",
            type=ModuleType.MAP,
            build_limits=[]),
        "deployment_module": ModuleTraits(
            name="deployment_module",
            type=ModuleType.DEPLOYMENT,
            build_limits=[])
    }

    builds_helper = BuildsHelper(db)

    # remove
    build1 = builds_helper.create("source_module")

    # the only build in the contour => no need to remove
    builds_helper.create("source_module", contour_name="other_contour")

    # pinned block removal
    builds_helper.create("source_module", pinned=True)

    # offspring block removal
    build = builds_helper.create("source_module")
    builds_helper.create("map_module", sources=[build])

    # deployment offpring block removal
    build = builds_helper.create("source_module")
    builds_helper.create("deployment_module", sources=[build])

    # offspring in another contour does not block removal
    build10 = builds_helper.create("source_module")
    builds_helper.create("map_module", sources=[build], contour_name="other_contour")

    # old failed offspring block removal
    build = builds_helper.create("source_module")
    builds_helper.create(
        "map_module",
        sources=[build],
        status=build_defs.BuildStatusString.FAILED,
        finish_time=NOW - checks.BLOCKING_BUILD_TTL - dt.timedelta(seconds=100),
    )

    # failed offspring block removal
    build = builds_helper.create("source_module")
    builds_helper.create(
        "another_map_module",
        sources=[build],
        status=build_defs.BuildStatusString.FAILED,
    )

    # failed offspring from another contour does not block removal
    build16 = builds_helper.create("source_module")
    builds_helper.create(
        "map_module",
        sources=[build16],
        status=build_defs.BuildStatusString.FAILED,
        contour_name="other_contour",
    )

    # cancelled build block removal
    build = builds_helper.create("source_module")
    builds_helper.create(
        "map_module",
        sources=[build],
        status=build_defs.BuildStatusString.CANCELLED,
    )

    # old cancelled build block removal
    build = builds_helper.create("source_module")
    builds_helper.create(
        "map_module",
        sources=[build],
        status=build_defs.BuildStatusString.CANCELLED,
        finish_time=NOW - checks.BLOCKING_BUILD_TTL - dt.timedelta(seconds=100),
    )

    # very old cancelled build should be removed
    build20 = builds_helper.create("source_module")
    build21 = builds_helper.create(
        "map_module",
        sources=[build],
        status=build_defs.BuildStatusString.CANCELLED,
        finish_time=NOW - dt.timedelta(days=4),
    )

    # in progress offspring block removal
    build = builds_helper.create("source_module")
    builds_helper.create(
        "map_module",
        sources=[build],
        status=build_defs.BuildStatusString.IN_PROGRESS,
    )

    # in progress offspring block removal
    build = builds_helper.create("source_module")
    builds_helper.create(
        "map_module",
        sources=[build],
        status=build_defs.BuildStatusString.IN_PROGRESS,
        contour_name="other_contour",
    )

    # removing offspring block removal
    build = builds_helper.create("source_module")
    builds_helper.create(
        "map_module",
        sources=[build],
        status=build_defs.BuildStatusString.REMOVING,
    )

    # The last completed build must not be removed
    builds_helper.create("source_module")

    # Old failed build is removed
    build29 = builds_helper.create("source_module", status=build_defs.BuildStatusString.FAILED)

    # The last failed build must not be removed if it is not very old
    builds_helper.create("source_module", status=build_defs.BuildStatusString.FAILED)

    module_builds_cleaner = create_cleaner(db, scheduler_mock, module_traits)
    with module_builds_cleaner:
        # activate background removal procedure
        scheduler_mock.execute_background_task()
        scheduler_mock.execute_background_task()

        assert module_builds_cleaner._uncleaned_candidates == {
            mbc.CleanupCandidate("contour_name", "source_module", build_defs.BuildStatusString.COMPLETED),
        }

    builds_helper.assert_removed(build1, build10, build16, build20, build21, build29)

    return list(db.module_logs.find({}, {"_id": False, "message": True, "exceptions": True}))


@pytest.mark.freeze_time(NOW)
def test_ignore_warnings(db, scheduler_mock, thread_executor_mock, config_mock):
    build_limit = BuildsLimit(
        max=1,
        remove_on_exceed=True,
        ignore_warnings=True,
    )
    module_traits = {
        "source_module": ModuleTraits(
            name="source_module",
            type=ModuleType.SOURCE,
            build_limits=[build_limit],
            sort_options=[SortOption(key_pattern="test_pattern")]),
        "map_module": ModuleTraits(
            name="map_module",
            type=ModuleType.MAP,
            build_limits=[]),
        "another_map_module": ModuleTraits(
            name="another_map_module",
            type=ModuleType.MAP,
            build_limits=[]),
        "deployment_module": ModuleTraits(
            name="deployment_module",
            type=ModuleType.DEPLOYMENT,
            build_limits=[])
    }

    builds_helper = BuildsHelper(db)

    # remove
    build1 = builds_helper.create("source_module")

    # the only build in the contour => no need to remove
    builds_helper.create("source_module", contour_name="other_contour")

    # pinned block removal
    builds_helper.create("source_module", pinned=True)

    # completed offspring doesn not block removal
    build4 = builds_helper.create("source_module")
    builds_helper.create("map_module", sources=[build4])

    # deployment offpring block removal
    build = builds_helper.create("source_module")
    builds_helper.create("deployment_module", sources=[build])

    # offspring in another contour does not block removal
    build10 = builds_helper.create("source_module")
    builds_helper.create("map_module", sources=[build], contour_name="other_contour")

    # old failed offspring does not block removal
    build12 = builds_helper.create("source_module")
    builds_helper.create(
        "map_module",
        sources=[build12],
        status=build_defs.BuildStatusString.FAILED,
        finish_time=NOW - checks.BLOCKING_BUILD_TTL - dt.timedelta(seconds=100),
    )

    # failed offspring block removal
    build = builds_helper.create("source_module")
    builds_helper.create(
        "another_map_module",
        sources=[build],
        status=build_defs.BuildStatusString.FAILED,
    )

    # failed offspring from another contour does not block removal
    build16 = builds_helper.create("source_module")
    builds_helper.create(
        "map_module",
        sources=[build16],
        status=build_defs.BuildStatusString.FAILED,
        contour_name="other_contour",
    )

    # cancelled build block removal
    build = builds_helper.create("source_module")
    builds_helper.create(
        "map_module",
        sources=[build],
        status=build_defs.BuildStatusString.CANCELLED,
    )

    # old cancelled build does not block removal
    build19 = builds_helper.create("source_module")
    builds_helper.create(
        "map_module",
        sources=[build19],
        status=build_defs.BuildStatusString.CANCELLED,
        finish_time=NOW - checks.BLOCKING_BUILD_TTL - dt.timedelta(seconds=100),
    )

    # very old cancelled build should be removed
    build20 = builds_helper.create("source_module")
    build21 = builds_helper.create(
        "map_module",
        sources=[build],
        status=build_defs.BuildStatusString.CANCELLED,
        finish_time=NOW - dt.timedelta(days=4),
    )

    # in progress offspring block removal
    build = builds_helper.create("source_module")
    builds_helper.create(
        "map_module",
        sources=[build],
        status=build_defs.BuildStatusString.IN_PROGRESS,
    )

    # in progress offspring block removal
    build = builds_helper.create("source_module")
    builds_helper.create(
        "map_module",
        sources=[build],
        status=build_defs.BuildStatusString.IN_PROGRESS,
        contour_name="other_contour",
    )

    # removing offspring does not block removal
    build26 = builds_helper.create("source_module")
    builds_helper.create(
        "map_module",
        sources=[build26],
        status=build_defs.BuildStatusString.REMOVING,
    )

    # The last completed build must not be removed
    builds_helper.create("source_module")

    # Old failed build is removed
    build29 = builds_helper.create("source_module", status=build_defs.BuildStatusString.FAILED)

    # The last failed build must not be removed if it is not very old
    builds_helper.create("source_module", status=build_defs.BuildStatusString.FAILED)

    module_builds_cleaner = create_cleaner(db, scheduler_mock, module_traits)
    with module_builds_cleaner:
        # activate background removal procedure
        scheduler_mock.execute_background_task()
        scheduler_mock.execute_background_task()

        assert module_builds_cleaner._uncleaned_candidates == {
            mbc.CleanupCandidate("contour_name", "source_module", build_defs.BuildStatusString.COMPLETED),
        }

    builds_helper.assert_removed(
        build1, build4, build10, build12, build16, build19, build20, build21, build26, build29)

    return list(db.module_logs.find({}, {"_id": False, "message": True, "exceptions": True}))


def test_add_new_candidate(db, scheduler_mock, thread_executor_mock, config_mock):
    build_limit = BuildsLimit(
        max=1,
        remove_on_exceed=True,
        ignore_warnings=True,
    )
    module_traits = {
        "test_module": ModuleTraits(
            name="test_module",
            type=ModuleType.SOURCE,
            build_limits=[build_limit],
            sort_options=[SortOption(key_pattern="test_pattern")]),
        "deployment_module": ModuleTraits(
            name="deployment_module",
            type=ModuleType.DEPLOYMENT,
            build_limits=[build_limit]),
    }

    builds_helper = BuildsHelper(db)

    module_builds_cleaner = create_cleaner(db, scheduler_mock, module_traits)
    with module_builds_cleaner:
        assert module_builds_cleaner._uncleaned_candidates == set()

        # The first and the only build is not removed
        build1 = builds_helper.create("test_module")
        module_builds_cleaner.on_build_status_changed(build1)
        builds_helper.assert_removed()
        assert module_builds_cleaner._uncleaned_candidates == set()

        # 2nd build is ready => remove the previous build
        build2 = builds_helper.create("test_module")
        module_builds_cleaner.on_build_status_changed(build2)
        builds_helper.assert_removed(build1)
        assert module_builds_cleaner._uncleaned_candidates == set()

        # deployment offpring block removal
        builds_helper.create("deployment_module", sources=[build2])

        # 3rd build is ready => we can't remove the previous build because it is blocked by deployment
        build3 = builds_helper.create("test_module")
        module_builds_cleaner.on_build_status_changed(build3)
        builds_helper.assert_removed(build1)
        assert module_builds_cleaner._uncleaned_candidates == {
            mbc.CleanupCandidate("contour_name", "test_module", build_defs.BuildStatusString.COMPLETED),
        }
