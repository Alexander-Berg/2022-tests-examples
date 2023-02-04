import datetime as dt

from maps.garden.sdk.module_traits.module_traits import (
    ModuleTraits,
    ModuleType,
)
from maps.garden.libs_server.build.build_statistics import BuildStatisticsStorage
from maps.garden.libs_server.module.module_manager import ModuleManager

from maps.garden.tools.stat_updater.lib.build_logs_manager import BuildLogInfo, BuildLogsManager

CONTOUR_NAME = "unittest"

MODULE_TRAITS = {
    "ymapsdf": ModuleTraits(
        name="ymapsdf",
        type=ModuleType.MAP,
        sources=[]),
    "pedestrian_graph": ModuleTraits(
        name="pedestrian_graph",
        type=ModuleType.MAP,
        sources=["ymapsdf"]),
    "masstransit_static": ModuleTraits(
        name="masstransit_static",
        type=ModuleType.MAP,
        sources=[]),
    "masstransit": ModuleTraits(
        name="masstransit",
        type=ModuleType.MAP,
        sources=["pedestrian_graph", "masstransit_static"]),
    "masstransit_deployment": ModuleTraits(
        name="masstransit_deployment",
        type=ModuleType.MAP,
        sources=["masstransit"]),
}

BUILD_STATUS_LOG = [
    {
        "status": {
            "total_tasks_number": 1,
            "successful_tasks_number": 1,
            "finish_time": dt.datetime(2019, 12, 30)
        },
        "request_id": 1
    }
]

BUILD_STATUS_LOG_AUTOSTARTED = [
    {
        "status": {
            "total_tasks_number": 1,
            "successful_tasks_number": 1,
            "finish_time": dt.datetime(2019, 12, 30)
        },
        "author": "garden-autostarter",
        "request_id": 1
    }
]

STAT_DATA = [
    {
        "name": "masstransit_deployment",
        "id": 111,
        "properties": {
            "shipping_date": "20191126_402358_1374_160103108"
        },
        "sources": [
            {
                "version": "build_id:222",
                "name": "masstransit"
            }
        ],
        "build_status_logs": BUILD_STATUS_LOG + BUILD_STATUS_LOG_AUTOSTARTED,
        "tracked_ancestor_builds": [{
            "name": "ymapsdf_src",
            "build_id": 777
        }],
        "contour_name": CONTOUR_NAME,
        "started_at": dt.datetime(2019, 12, 30),
        "finished_at": dt.datetime(2019, 12, 30)
    },
    {
        "name": "masstransit",
        "id": 222,
        "properties": {
            "shipping_date": "20191126_402358_1374_160103108"
        },
        "sources": [
            {
                "version": "build_id:333",
                "name": "masstransit_static"
            },
            {
                "version": "build_id:444",
                "name": "pedestrian_graph"
            }
        ],
        "tracked_ancestor_builds": [{
            "name": "ymapsdf_src",
            "build_id": 777
        }],
        "build_status_logs": BUILD_STATUS_LOG,
        "contour_name": CONTOUR_NAME,
        "started_at": dt.datetime(2019, 12, 30),
        "finished_at": dt.datetime(2019, 12, 30)
    },
    {
        "name": "masstransit_static",
        "id": 333,
        "properties": {
            "shipping_date": "20191126_402358_1374_160103108"
        },
        "build_status_logs": BUILD_STATUS_LOG,
        "contour_name": CONTOUR_NAME,
        "started_at": dt.datetime(2019, 12, 30),
        "finished_at": dt.datetime(2019, 12, 30)
    },
    {
        "name": "pedestrian_graph",
        "id": 444,
        "sources": [
            {
                "version": "build_id:555",
                "name": "ymapsdf"
            }
        ],
        "tracked_ancestor_builds": [{
            "name": "ymapsdf_src",
            "build_id": 777
        }],
        "build_status_logs": BUILD_STATUS_LOG,
        "contour_name": CONTOUR_NAME,
        "started_at": dt.datetime(2019, 12, 30),
        "finished_at": dt.datetime(2019, 12, 30)
    },
    {
        "name": "ymapsdf",
        "id": 555,
        "properties": {
            "shipping_date": "20191126_402358_1234_160103108"
        },
        "tracked_ancestor_builds": [{
            "name": "ymapsdf_src",
            "build_id": 777
        }],
        "build_status_logs": BUILD_STATUS_LOG,
        "contour_name": CONTOUR_NAME,
        "started_at": dt.datetime(2019, 12, 30),
        "finished_at": dt.datetime(2019, 12, 30)
    },
    {
        "name": "ymapsdf_src",
        "id": 777,
        "properties": {
            "shipping_date": "20191126_402358_1234_160103108",
            "nmaps_branch_id": "1234"
        },
        "tracked_ancestor_builds": [{
            "name": "ymapsdf_src",
            "build_id": 777
        }],
        "build_status_logs": BUILD_STATUS_LOG,
        "contour_name": CONTOUR_NAME,
        "started_at": dt.datetime(2019, 12, 31),
        "finished_at": dt.datetime(2019, 12, 31)
    },
]


def test_build_log_manager(db, module_helper, migrations):
    module_manager = ModuleManager(
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

    for traits in MODULE_TRAITS.values():
        module_helper.add_module_to_system_contour(traits)

    db.build_statistics.insert_many(STAT_DATA)

    build_statistics_storage = BuildStatisticsStorage(db)

    logs_manager = BuildLogsManager(build_statistics_storage)
    result = logs_manager.completed(dt.datetime(2019, 12, 30), [CONTOUR_NAME])
    assert list(result) == [
        BuildLogInfo(id=111, name="masstransit_deployment", autocompleted=True, branch=1234, contour=CONTOUR_NAME),
        BuildLogInfo(id=222, name="masstransit", autocompleted=False, branch=1234, contour="unittest"),
        BuildLogInfo(id=333, name="masstransit_static", autocompleted=False, branch=-1, contour="unittest"),
        BuildLogInfo(id=444, name="pedestrian_graph", autocompleted=False, branch=1234, contour="unittest"),
        BuildLogInfo(id=555, name="ymapsdf", autocompleted=False, branch=1234, contour="unittest"),
    ]
