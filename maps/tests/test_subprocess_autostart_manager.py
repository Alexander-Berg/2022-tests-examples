import datetime as dt
import pytest
from unittest import mock
import pytz
import typing as tp

import yatest.common

from maps.garden.sdk.utils import contour
from maps.garden.libs_server.common.scheduler import SchedulerAdaptor

from maps.garden.sdk.module_traits import module_traits as mt
from maps.garden.sdk.module_rpc import common as rpc_common

from maps.garden.libs_server.autostart import subprocess_autostart_manager
from maps.garden.libs_server.log_storage.module_event_storage import ModuleEventStorage
from maps.garden.libs_server.log_storage.module_log_storage import ModuleLog
from maps.garden.libs_server.module import module_manager as mm
from maps.garden.libs_server.build import build_defs
from maps.garden.libs_server.build import build_utils
from maps.garden.libs_server.build import build_manager as bm
from maps.garden.libs_server.build import build_statistics


NOW = dt.datetime(2020, 12, 25, 17, 45, 00)

TEST_CONTOUR_NAME = contour.default_contour_name()

TEST_MODULE_NAME = "test_module"
TEST_SOURCE_MODULE_NAME = "test_src_module"
TEST_DEPLOYMENT_MODULE_NAME = "test_deploy_module"

SERVER_SETTINGS = {
    "autostart": {
        "contours": [TEST_CONTOUR_NAME]
    },
    "calendars": [],
    "tvm_client": {
        "client_id": 123456789,
        "token": "token",
    }
}


@pytest.fixture
def module_manager(db, migrations, module_helper):
    module_manager = mm.ModuleManager(
        config={
            "isolated_modules_cache_dir": ".",
            "yt": {
                "prefix": "//home/garden/prod",
                "config": {},
            },
            "sandbox": {
                "token": "token",
            },
        },
        db=db,
    )

    module_helper.set_module_manager(module_manager)

    module_helper.add_module_to_system_contour(
        mt.ModuleTraits(
            name=TEST_SOURCE_MODULE_NAME,
            type=mt.ModuleType.MAP,
            sort_options=[
                mt.SortOption(key_pattern="{0.properties[sort_key]}", reverse=True),
            ]
        )
    )
    module_helper.add_module_to_system_contour(
        mt.ModuleTraits(
            name=TEST_MODULE_NAME,
            type=mt.ModuleType.MAP,
            sources=[TEST_SOURCE_MODULE_NAME],
            autostarter=mt.ModuleAutostarter(trigger_by=[TEST_SOURCE_MODULE_NAME]),
            capabilities=[rpc_common.Capabilities.HANDLE_BUILD_STATUS],
        ),
        local_path=yatest.common.binary_path("maps/garden/libs_server/autostart/tests/test_module/test_module"),
    )
    module_helper.add_module_to_system_contour(
        mt.ModuleTraits(
            name=TEST_DEPLOYMENT_MODULE_NAME,
            type=mt.ModuleType.DEPLOYMENT,
            sources=[TEST_MODULE_NAME],
            autostarter=mt.ModuleAutostarter(trigger_by=[TEST_MODULE_NAME]),
            capabilities=[rpc_common.Capabilities.HANDLE_BUILD_STATUS],
        ),
        local_path=yatest.common.binary_path("maps/garden/libs_server/autostart/tests/test_deploy_module/test_deploy_module"),
    )

    return module_manager


@pytest.fixture
def module_builds_manager(mocker):
    return mocker.patch(
        "maps.garden.libs_server.build.module_builds_manager.ModuleBuildsManager",
        autospec=True
    )


@pytest.fixture
def autostart_manager(db, module_manager, module_builds_manager, scheduler_mock, thread_executor_mock):
    build_manager = bm.BuildManager(db, module_event_storage=ModuleEventStorage(db))

    autostart_manager = subprocess_autostart_manager.SubprocessAutostartManager(
        server_settings=SERVER_SETTINGS,
        db=db,
        module_manager=module_manager,
        build_manager=build_manager,
        module_builds_manager=module_builds_manager,
        delay_executor=SchedulerAdaptor(scheduler_mock),
    )

    with autostart_manager:
        yield autostart_manager


def assert_module_log(
    db,
    submessage: str,
    exception_type: tp.Optional[str] = None,
    exception_message: tp.Optional[str] = None,
):
    records = [ModuleLog.parse_obj(r) for r in db.module_logs.find({})]
    assert len(records) == 1
    record = records[0]

    assert submessage in record.message

    if exception_type or exception_message:
        exception = record.exceptions[0]
        assert exception.type == exception_type
        assert exception.message == exception_message
    else:
        assert not record.exceptions


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_mongo
def test_create_build(db, autostart_manager, module_builds_manager, scheduler_mock):
    source_build1 = build_defs.Build(
        id=111,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        sources=[
            build_defs.Source(
                name="source_module",
                version=build_defs.SourceVersion(build_id=1),
            ),
            build_defs.Source(
                name="source_resource",
                version=build_defs.SourceVersion(key="1908601caf53259397c36b98aa96654b"),
            )
        ]
    )
    db.builds.insert_one(source_build1.dict())

    source_build2 = build_defs.Build(
        id=222,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        extras={
            "test_command": "create_build",
        }
    )
    db.builds.insert_one(source_build2.dict())

    existing_build = build_defs.Build(
        id=111,
        name=TEST_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
    )
    db.builds.insert_one(existing_build.dict())

    autostart_manager.handle_build_status_changed(source_build2)

    scheduler_mock.execute_background_task()

    expected_calls = [mock.call(
        TEST_MODULE_NAME,
        TEST_CONTOUR_NAME,
        build_utils.create_new_build(
            source_ids=[source_build1.full_id, source_build2.full_id],
            extras={
                "autostarted": True,
                "test_key": "test_value",
            }
        ),
        subprocess_autostart_manager.AUTO_STARTER_AUTHOR,
    )]

    assert module_builds_manager.create.call_args_list == expected_calls

    assert not db.autostart_requests.count_documents({})

    assert_module_log(db, submessage="Create new build")


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_mongo
def test_autostart_requests_in_queue(db, autostart_manager, module_builds_manager, scheduler_mock):
    source_build1 = build_defs.Build(
        id=111,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        extras={
            "test_command": "create_build",
        }
    )
    db.builds.insert_one(source_build1.dict())

    source_build2 = build_defs.Build(
        id=222,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        extras={
            "test_command": "create_build",
        }
    )
    db.builds.insert_one(source_build2.dict())

    autostart_manager.handle_build_status_changed(source_build1)
    autostart_manager.handle_build_status_changed(source_build2)

    assert db.autostart_requests.count_documents({}) == 2

    scheduler_mock.execute_background_task()

    expected_calls = [mock.call(
        TEST_MODULE_NAME,
        TEST_CONTOUR_NAME,
        build_utils.create_new_build(
            source_ids=[source_build1.full_id, source_build2.full_id],
            extras={
                "autostarted": True,
                "test_key": "test_value",
            }
        ),
        subprocess_autostart_manager.AUTO_STARTER_AUTHOR,
    )] * 2

    assert module_builds_manager.create.call_args_list == expected_calls

    assert not db.autostart_requests.count_documents({})


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_mongo
def test_existing_build_with_same_sources(db, autostart_manager, module_builds_manager, scheduler_mock):
    source_build = build_defs.Build(
        id=111,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        extras={
            "test_command": "create_build",
        }
    )
    db.builds.insert_one(source_build.dict())

    existing_build = build_defs.Build(
        id=222,
        name=TEST_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        sources=[
            build_defs.Source.generate_from(source_build),
        ]
    )
    db.builds.insert_one(existing_build.dict())

    autostart_manager.handle_build_status_changed(source_build)

    scheduler_mock.execute_background_task()

    module_builds_manager.create.assert_not_called()

    assert not db.autostart_requests.count_documents({})

    assert_module_log(db, submessage="New build is not created: There is another build with the same sources")


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_mongo
def test_existing_build_action_with_same_sources(db, autostart_manager, module_builds_manager, scheduler_mock):
    source_build = build_defs.Build(
        id=111,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        extras={
            "test_command": "create_build",
        }
    )
    db.builds.insert_one(source_build.dict())

    # Create build_action
    build_manager = bm.BuildManager(db, module_event_storage=ModuleEventStorage(db))
    build_manager.add(
        name=TEST_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        username="user",
        sources=[
            build_defs.Source.generate_from(source_build),
        ],
        extras={},
        module_version="123",
    )

    autostart_manager.handle_build_status_changed(source_build)

    scheduler_mock.execute_background_task()

    module_builds_manager.create.assert_not_called()

    assert not db.autostart_requests.count_documents({})

    assert_module_log(db, submessage="New build is not created: There is another build with the same sources")


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_mongo
def test_existing_build_with_same_sources_different_deploy_step(db, autostart_manager, module_builds_manager, scheduler_mock):
    source_build = build_defs.Build(
        id=111,
        name=TEST_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        extras={
            "test_command": "create_build",
        }
    )
    db.builds.insert_one(source_build.dict())

    existing_build = build_defs.Build(
        id=222,
        name=TEST_DEPLOYMENT_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        sources=[
            build_defs.Source.generate_from(source_build),
        ],
        extras={
            "deploy_step": "testing",
        }
    )
    db.builds.insert_one(existing_build.dict())

    autostart_manager.handle_build_status_changed(source_build)

    scheduler_mock.execute_background_task()

    expected_calls = [mock.call(
        TEST_DEPLOYMENT_MODULE_NAME,
        TEST_CONTOUR_NAME,
        build_utils.create_new_build(
            source_ids=[source_build.full_id],
            extras={
                "autostarted": True,
                "deploy_step": "stable",
            }
        ),
        subprocess_autostart_manager.AUTO_STARTER_AUTHOR,
    )]

    assert module_builds_manager.create.call_args_list == expected_calls

    assert not db.autostart_requests.count_documents({})

    assert_module_log(db, submessage="Create new build")


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_mongo
def test_no_build(db, autostart_manager, module_builds_manager, scheduler_mock):
    build = build_defs.Build(
        id=111,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        extras={
            "test_command": "no_build",
        }
    )
    db.builds.insert_one(build.dict())

    autostart_manager.handle_build_status_changed(build)

    scheduler_mock.execute_background_task()

    module_builds_manager.create.assert_not_called()

    assert not db.autostart_requests.count_documents({})

    assert_module_log(db, submessage="New build is not created: Module autostarter ignored the trigger")


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_mongo
def test_exception(db, autostart_manager, module_builds_manager, scheduler_mock):
    build = build_defs.Build(
        id=111,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        extras={
            "test_command": "raise_exception",
        }
    )
    db.builds.insert_one(build.dict())

    autostart_manager.handle_build_status_changed(build)

    scheduler_mock.execute_background_task()

    module_builds_manager.create.assert_not_called()

    assert not db.autostart_requests.count_documents({})

    assert_module_log(
        db,
        submessage="New build is not created: Exception occured",
        exception_type="main.CustomModuleError",
        exception_message="Something went wrong",
    )


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_mongo
def test_delayed_start(db, autostart_manager, module_builds_manager, scheduler_mock):
    source_build = build_defs.Build(
        id=111,
        name=TEST_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        extras={
            "test_command": "delay_start",
        }
    )
    db.builds.insert_one(source_build.dict())

    autostart_manager.handle_build_status_changed(source_build)

    scheduler_mock.execute_background_task()

    assert not module_builds_manager.create.called

    assert db.autostart_requests.count_documents({}) == 1

    request = subprocess_autostart_manager.AutostartRequest.parse_obj(
        db.autostart_requests.find_one()
    )

    assert request.trigger_module_name == TEST_MODULE_NAME
    assert request.trigger_build_id == 111
    assert request.target_module_name == TEST_DEPLOYMENT_MODULE_NAME
    assert request.target_contour_name == TEST_CONTOUR_NAME
    assert request.attempt == 2
    assert request.delayed_run_at == dt.datetime(2020, 12, 25, 18, 45, tzinfo=pytz.utc)

    assert_module_log(db, submessage="Module autostarter delayed start until")


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_mongo
def test_delayed_start_in_queue(db, autostart_manager, module_builds_manager, scheduler_mock):
    source_build = build_defs.Build(
        id=111,
        name=TEST_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        extras={
            "test_command": "delay_start",
        }
    )
    db.builds.insert_one(source_build.dict())

    autostart_manager.handle_build_status_changed(source_build)
    autostart_manager.handle_build_status_changed(source_build)

    scheduler_mock.execute_background_task()

    assert not module_builds_manager.create.called

    assert db.autostart_requests.count_documents({}) == 1

    request = subprocess_autostart_manager.AutostartRequest.parse_obj(
        db.autostart_requests.find_one()
    )

    assert request.trigger_module_name == TEST_MODULE_NAME
    assert request.trigger_build_id == 111
    assert request.target_module_name == TEST_DEPLOYMENT_MODULE_NAME
    assert request.target_contour_name == TEST_CONTOUR_NAME
    assert request.attempt == 2
    assert request.delayed_run_at == dt.datetime(2020, 12, 25, 18, 45, tzinfo=pytz.utc)


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_mongo
def test_build_sort(db, autostart_manager, module_builds_manager, scheduler_mock):
    source_build1 = build_defs.Build(
        id=111,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        extras={
            "test_command": "find_region",
            "region": "cis2",
            "sort_key": "100",
        }
    )
    db.builds.insert_one(source_build1.dict())

    source_build2 = build_defs.Build(
        id=222,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        extras={
            "test_command": "find_region",
            "region": "cis2",
            "sort_key": "300",  # upper on page
        }
    )
    db.builds.insert_one(source_build2.dict())

    # This build has higher id but lower sort_key
    source_build3 = build_defs.Build(
        id=333,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        extras={
            "test_command": "find_region",
            "region": "cis2",
            "sort_key": "200",  # lower on page
        }
    )
    db.builds.insert_one(source_build3.dict())

    existing_build = build_defs.Build(
        id=111,
        name=TEST_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        sources=[
            build_defs.Source(
                name=TEST_SOURCE_MODULE_NAME,
                version=build_defs.SourceVersion(build_id=111),
                properties={
                    "region": "cis2",
                }
            ),
        ]
    )
    db.builds.insert_one(existing_build.dict())

    autostart_manager.handle_build_status_changed(source_build1)

    scheduler_mock.execute_background_task()

    expected_calls = [mock.call(
        TEST_MODULE_NAME,
        TEST_CONTOUR_NAME,
        build_utils.create_new_build(
            source_ids=[source_build2.full_id],  # choose upper build on the page
            extras={
                "autostarted": True,
                "test_key": "test_value",
            }
        ),
        subprocess_autostart_manager.AUTO_STARTER_AUTHOR,
    )]

    assert module_builds_manager.create.call_args_list == expected_calls

    assert not db.autostart_requests.count_documents({})

    assert_module_log(db, submessage="Create new build")


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_mongo
def test_removed_sources(db, autostart_manager, module_builds_manager, scheduler_mock):
    source_build = build_defs.Build(
        id=222,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        extras={
            "test_command": "find_region",
            "region": "cis2",
        }
    )
    db.builds.insert_one(source_build.dict())

    existing_build = build_defs.Build(
        id=111,
        name=TEST_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
        sources=[
            # removed source
            build_defs.Source(
                name=TEST_SOURCE_MODULE_NAME,
                version=build_defs.SourceVersion(build_id=111),
                properties={
                    "region": "cis2",
                }
            ),
        ]
    )
    db.builds.insert_one(existing_build.dict())

    autostart_manager.handle_build_status_changed(source_build)

    scheduler_mock.execute_background_task()

    expected_calls = [mock.call(
        TEST_MODULE_NAME,
        TEST_CONTOUR_NAME,
        build_utils.create_new_build(
            source_ids=[source_build.full_id],
            extras={
                "autostarted": True,
                "test_key": "test_value",
            }
        ),
        subprocess_autostart_manager.AUTO_STARTER_AUTHOR,
    )]

    assert module_builds_manager.create.call_args_list == expected_calls

    assert not db.autostart_requests.count_documents({})

    assert_module_log(db, submessage="Create new build")


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_mongo
def test_removed_target_build(db, autostart_manager, module_builds_manager, scheduler_mock):
    """
    If autostarter was delayed then it may occur
        that trigger build has been removed by the time of the next autostarter call.
    """
    requests_storage = subprocess_autostart_manager.AutostartRequestStorage(db)
    requests_storage.add_request(
        subprocess_autostart_manager.AutostartRequest(
            trigger_module_name=TEST_SOURCE_MODULE_NAME,
            trigger_build_id=111,
            target_contour_name=TEST_CONTOUR_NAME,
            target_module_name=TEST_MODULE_NAME,
        )
    )

    source_build1_stat = build_statistics.BuildStatisticsRecord(
        id=111,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        properties={
            "test_command": "create_build",
        },
        started_at=NOW,
        build_status_logs=[
            build_statistics.BuildStatusLog(
                status=build_defs.BuildStatus.create_completed(),
                request_id=123,
            )
        ],
    )
    db.build_statistics.insert_one(source_build1_stat.dict())

    source_build2 = build_defs.Build(
        id=222,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
    )
    db.builds.insert_one(source_build2.dict())

    scheduler_mock.execute_background_task()

    expected_calls = [mock.call(
        TEST_MODULE_NAME,
        TEST_CONTOUR_NAME,
        build_utils.create_new_build(
            source_ids=[source_build2.full_id],
            extras={
                "autostarted": True,
                "test_key": "test_value",
            }
        ),
        subprocess_autostart_manager.AUTO_STARTER_AUTHOR,
    )]

    assert module_builds_manager.create.call_args_list == expected_calls

    assert not db.autostart_requests.count_documents({})

    assert_module_log(db, submessage="Create new build")


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_mongo
def test_module_trigger_itself_with_same_sources(db, autostart_manager, module_builds_manager, scheduler_mock):
    """
    test_module trigger itself.
    Build 'test_module:111' is removed, but it is a trigger.
    Do not create a new build if new build sources are the same as of trigger build.
    """
    requests_storage = subprocess_autostart_manager.AutostartRequestStorage(db)
    requests_storage.add_request(
        subprocess_autostart_manager.AutostartRequest(
            trigger_module_name=TEST_MODULE_NAME,
            trigger_build_id=111,
            target_contour_name=TEST_CONTOUR_NAME,
            target_module_name=TEST_MODULE_NAME,
        )
    )

    source_build = build_defs.Build(
        id=111,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
    )
    db.builds.insert_one(source_build.dict())

    source_build2 = build_defs.Build(
        id=222,
        name=TEST_SOURCE_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        status=build_defs.BuildStatus.create_completed(),
    )
    db.builds.insert_one(source_build2.dict())

    trigger_build_stat = build_statistics.BuildStatisticsRecord(
        id=111,
        name=TEST_MODULE_NAME,
        contour_name=TEST_CONTOUR_NAME,
        properties={
            "test_command": "create_build",
        },
        started_at=NOW,
        build_status_logs=[
            build_statistics.BuildStatusLog(
                status=build_defs.BuildStatus.create_completed(),
                request_id=123,
            )
        ],
        sources=[
            build_defs.Source.generate_from(source_build),
            build_defs.Source.generate_from(source_build2),
        ],
    )
    db.build_statistics.insert_one(trigger_build_stat.dict())

    scheduler_mock.execute_background_task()

    module_builds_manager.create.assert_not_called()

    assert not db.autostart_requests.count_documents({})

    assert_module_log(db, submessage="New build is not created: There is another build with the same sources")


@pytest.mark.use_local_mongo
def test_delete_request_by_contour_and_module(db):
    requests_storage = subprocess_autostart_manager.AutostartRequestStorage(db)
    assert requests_storage.delete_requests(TEST_CONTOUR_NAME, TEST_MODULE_NAME, True) == 0
    test_request = subprocess_autostart_manager.AutostartRequest(
        trigger_module_name=TEST_SOURCE_MODULE_NAME,
        trigger_build_id=111,
        target_contour_name=TEST_CONTOUR_NAME,
        target_module_name=TEST_MODULE_NAME,
        delayed_run_at=dt.datetime.now()
    )
    db.autostart_requests.insert_one(test_request.dict())
    assert requests_storage.delete_requests(TEST_CONTOUR_NAME, TEST_MODULE_NAME, True) == 1
    db.autostart_requests.insert_one(test_request.dict())
    assert requests_storage.delete_requests(TEST_CONTOUR_NAME, TEST_MODULE_NAME) == 1


@pytest.mark.use_local_mongo
def test_find_requests(db):
    requests_storage = subprocess_autostart_manager.AutostartRequestStorage(db)
    find_result = requests_storage.find_requests(
        target_contour_name=TEST_CONTOUR_NAME,
        target_module_name=TEST_MODULE_NAME,
        is_delayed=True
    )
    assert not find_result
    test_request = subprocess_autostart_manager.AutostartRequest(
        trigger_module_name=TEST_SOURCE_MODULE_NAME,
        trigger_build_id=111,
        target_contour_name=TEST_CONTOUR_NAME,
        target_module_name=TEST_MODULE_NAME,
        delayed_run_at=dt.datetime.now()
    )
    db.autostart_requests.insert_one(test_request.dict())
    find_result = requests_storage.find_requests(
        target_contour_name=TEST_CONTOUR_NAME,
        target_module_name=TEST_MODULE_NAME,
        is_delayed=True
    )
    assert find_result
    assert find_result[0].trigger_build_id == 111
    find_result = requests_storage.find_requests(
        target_contour_name=TEST_CONTOUR_NAME,
        target_module_name=TEST_MODULE_NAME,
    )
    assert find_result
    assert find_result[0].trigger_build_id == 111
