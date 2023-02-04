import datetime as dt
import time

from bson.objectid import ObjectId
from datetime import datetime
import pymongo
from pymongo.database import Database
import pytest

from yt.common import date_string_to_datetime

from maps.garden.sdk.module_traits.module_traits import ModuleTraits, ModuleType
from maps.garden.libs_server.build.build_defs import BuildStatus, Source, SourceVersion
from maps.garden.libs_server.build.build_statistics import BuildStatisticsRecord, BuildStatusLog
from maps.garden.libs_server.migration import migrate_database
from maps.garden.libs_server.task import task_log_manager
from maps.garden.libs_server.task import operation_storage


@pytest.fixture
def config():
    return {
        'mongo': {
            'dbname': 'garden_server_test'
        }
    }


def _datetime_to_str(obj):
    if isinstance(obj, dt.datetime):
        return obj.isoformat()
    elif isinstance(obj, list):
        return [_datetime_to_str(e) for e in obj]
    elif isinstance(obj, dict):
        return {key: _datetime_to_str(value) for key, value in obj.items()}
    else:
        return obj


@pytest.mark.use_local_mongo
def test_migration(db, config):
    db.schema_version.remove({})
    db.schema_version.insert_one({"version": 0})

    migrate_database.run_migration(config)

    assert db.contours.count() == 1
    assert "module_traits" not in db.list_collection_names()
    assert len(db.build_statistics.index_information()) == 3
    assert len(db.builds.index_information()) == 2


@pytest.mark.use_local_mongo
def test_build_statistics_migration(db):
    db.builds.insert_one({
        "id": 1,
        "name": "altay",
        "module_version": "123456"
    })

    db.build_statistics.insert_many([
        {
            "_id": ObjectId("5dfb8483bbca76ec4f5dbef8"),
            "id": 1,
            "name": "altay",
            "autostarted": False,
            "release_name": "20191219",
            "requests_statuses": [
                {
                    "string": "completed",
                    "time_of_first_error": None,
                    "author": None,
                    "finish_time": None,
                    "start_time": None,
                    "request_id": 1,
                    "successful_tasks_number": 1,
                    "total_tasks_number": 1,
                    "operation": "remove",
                    "updated_at": dt.datetime.fromisoformat("2019-03-18T13:25:38.023+00:00"),
                }
            ],
            "shipping_date": "20191219",
            "sources": [
                {
                    "version": "key:1df1a90f626aa934f7dad30200666b10",
                    "name": "altay_companies_unknown"
                }
            ],
            "contour_name": "development"
        },
        {
            "_id": ObjectId("5c946ef2a922d268a8427809"),
            "id": 29,
            "name": "pedestrian_graph",
            "vendor": "yandex",
            "autostarted": True,
            "requests_statuses": [
                {
                    "string": None,
                    "time_of_first_error": None,
                    "author": "garden-autostarter",
                    "finish_time": None,
                    "start_time": None,
                    "request_id": 151903,
                    "successful_tasks_number": 0,
                    "total_tasks_number": 0,
                    "operation": "create",
                    "updated_at": dt.datetime.fromisoformat("2019-03-21T13:25:38.023+00:00"),
                },
                {
                    "string": "completed",
                    "time_of_first_error": None,
                    "author": "garden-autostarter",
                    "finish_time": dt.datetime.fromisoformat("2019-03-21T13:25:38.023+00:00"),
                    "start_time": dt.datetime.fromisoformat("2019-03-21T12:11:14.235+00:00"),
                    "request_id": 151971,
                    "successful_tasks_number": 7,
                    "total_tasks_number": 7,
                    "operation": "remove",
                    "updated_at": dt.datetime.fromisoformat("2019-03-22T12:11:14.235+00:00"),
                },
                {
                    "string": "failed",
                    "time_of_first_error": None,
                    "author": "garden-autostarter",
                    "finish_time": dt.datetime.fromisoformat("2019-03-22T13:25:38.023+00:00"),
                    "start_time": dt.datetime.fromisoformat("2019-03-22T12:11:14.235+00:00"),
                    "request_id": 151972,
                    "successful_tasks_number": 7,
                    "total_tasks_number": 7,
                    "operation": "remove",
                    "updated_at": dt.datetime.fromisoformat("2019-03-23T12:11:14.235+00:00"),
                }
            ],
            "shipping_date": "20190321_310762_1037_127648131",
            "sources": [
                {
                    "version": "build_id:4516",
                    "name": "ymapsdf"
                },
            ],
            "release": "19.03.22-1",
            "release_name": "19.03.22-1",
            "contour_name": "testing"
        }
    ])

    migrate_database._restructure_build_statistics(db)

    build_doc = db.build_statistics.find_one({"name": "altay"})
    assert BuildStatisticsRecord.parse_obj(build_doc) == BuildStatisticsRecord(
        mongo_id="5dfb8483bbca76ec4f5dbef8",
        id=1,
        name="altay",
        contour_name="development",
        module_version="123456",
        build_status_logs=[
            BuildStatusLog(
                request_id=1,
                module_version="123456",
                status=BuildStatus(
                    string="completed",
                    successful_tasks_number=1,
                    total_tasks_number=1,
                    operation="remove",
                    updated_at=dt.datetime.fromisoformat("2019-03-18T13:25:38.023+00:00"),
                ),
            )
        ],
        sources=[
            Source(
                version=SourceVersion(key="1df1a90f626aa934f7dad30200666b10"),
                name="altay_companies_unknown"
            )
        ],
        properties={
            "autostarted": False,
            "release_name": "20191219",
            "shipping_date": "20191219",
        },
        started_at=dt.datetime.fromisoformat("2019-12-19T14:09:07.000+00:00"),
    )

    build_doc = db.build_statistics.find_one({"name": "pedestrian_graph"})
    assert BuildStatisticsRecord.parse_obj(build_doc) == BuildStatisticsRecord(
        mongo_id="5c946ef2a922d268a8427809",
        id=29,
        name="pedestrian_graph",
        contour_name="testing",
        build_status_logs=[
            BuildStatusLog(
                author="garden-autostarter",
                request_id=151903,
                status=BuildStatus(
                    successful_tasks_number=0,
                    total_tasks_number=0,
                    operation="create",
                    updated_at=dt.datetime.fromisoformat("2019-03-21T13:25:38.023+00:00"),
                ),
            ),
            BuildStatusLog(
                request_id=151971,
                author="garden-autostarter",
                status=BuildStatus(
                    string="completed",
                    finish_time=dt.datetime.fromisoformat("2019-03-21T13:25:38.023+00:00"),
                    start_time=dt.datetime.fromisoformat("2019-03-21T12:11:14.235+00:00"),
                    successful_tasks_number=7,
                    total_tasks_number=7,
                    operation="remove",
                    updated_at=dt.datetime.fromisoformat("2019-03-22T12:11:14.235+00:00"),
                ),
            ),
            BuildStatusLog(
                request_id=151972,
                author="garden-autostarter",
                status=BuildStatus(
                    string="failed",
                    finish_time=dt.datetime.fromisoformat("2019-03-22T13:25:38.023+00:00"),
                    start_time=dt.datetime.fromisoformat("2019-03-22T12:11:14.235+00:00"),
                    successful_tasks_number=7,
                    total_tasks_number=7,
                    operation="remove",
                    updated_at=dt.datetime.fromisoformat("2019-03-23T12:11:14.235+00:00"),
                ),
            )
        ],
        sources=[
            Source(
                version=SourceVersion(build_id=4516),
                name="ymapsdf"
            ),
        ],
        properties={
            "release": "19.03.22-1",
            "release_name": "19.03.22-1",
            "shipping_date": "20190321_310762_1037_127648131",
            "vendor": "yandex",
            "autostarted": True,
        },
        started_at=dt.datetime.fromisoformat("2019-03-21T12:11:14.235+00:00"),
        finished_at=dt.datetime.fromisoformat("2019-03-22T13:25:38.023+00:00"),
        completed_at=dt.datetime.fromisoformat("2019-03-21T13:25:38.023+00:00"),
    )


@pytest.mark.use_local_mongo
def test_remove_traits_fill_missing_policy_filters_migration(db):
    db.module_versions.insert_one({
        "traits": {
            "name": "test_module",
            "type": "map",
            "fill_missing_policy": {
                "filters": None,
                "fill_with_empty_resources": True,
            }
        }
    })

    migrate_database._remove_traits_fill_missing_policy_filters(db)

    expected_traits = ModuleTraits(
        name="test_module",
        type=ModuleType.MAP,
    )

    assert ModuleTraits.parse_obj(db.module_versions.find_one()["traits"]) == expected_traits


@pytest.mark.use_local_mongo
def test_remove_unused_traits_fields_migration(db):
    db.module_versions.insert_one({
        "traits": {
            "name": "test_module",
            "type": "map",
            "sources_displayed_name_pattern": "{0.sources[0].properties[release_name]}",
            "position": 2,
            "config": None,
            "testers": ["masstransit_tester"],
            "check_input_sources_completeness": True,
            "subsources_for_integrity_check": [],
            "allow_start_automatically": True,
        }
    })

    migrate_database._remove_unused_traits_fields(db)

    expected_traits = ModuleTraits(
        name="test_module",
        type=ModuleType.MAP,
    )

    assert ModuleTraits.parse_obj(db.module_versions.find_one()["traits"]) == expected_traits


@pytest.mark.use_local_mongo
def test_task_log_migration(db):
    db.task_log.insert_many([
        # finished task
        {
            "_id": ObjectId("602117bb41d6ff528429d96d"),
            "tag": "renderer_denormalization:10165",
            "kind": "task_execution",
            "request_ids": [],
            "task_id": "12192431195919684147",
            "task_name": "ToolYtTask",
            "name": "ToolYtTask",
            "task_module_name": "renderer_denormalization",
            "module_name": "renderer_denormalization",
            "contour_name": "testing",
            "module_version": "testing_f0f7e770e8e7e8db6826f6caf59350bc",
            "task_python_module": "renderer_denormalization",
            "python_module": "renderer_denormalization",
            "insert_traceback": "  File \"maps/garden/modules/renderer_denormalization/bin/main.py\",\n",
            "predict_consumption": {
                "cpu": 1,
                "ram": 4294967296,
                "operations": 2
            },
            "consumption": {
                "cpu": 1,
                "ram": 4294967296,
                "operations": 2
            },
            "additional_data": {
                "ensure_available_duration": {
                    "total": 15850.188,
                    "resources": [
                        {
                            "name": "ymapsdf_ft_aao_yandex",
                            "delta_ms": 31
                        }
                    ]
                },
                "commit_durations": [
                    {
                        "name": "renderer_denormalization_rd_el_shields_data_tmp_aao_yandex",
                        "delta_ms": 1
                    }
                ],
                "yt_operation_id": "2c25104d-db0accd2-3fe03e8-41621233",
                "yt_cluster": "hahn",
                "log_url": "http://s3.mds.yandex.net/maps-garden-logs-stable/580800.log",
                "retry_attempt": 0,
                "yt_operations": [
                    {
                        "id": "2c25104d-db0accd2-3fe03e8-41621233",
                        "type": "vanilla",
                        "start_time": dt.datetime.fromisoformat("2021-02-08T10:49:10.023+00:00"),
                        "finish_time": dt.datetime.fromisoformat("2021-02-08T10:50:27.023+00:00"),
                        "stats": {
                            "operation_count": 1,
                            "events": {
                                "starting_duration": 0.18,
                                "waiting_for_agent_duration": 0,
                                "initializing_duration": 0.45,
                                "preparing_duration": 0.08,
                                "pending_duration": 0,
                                "materializing_duration": 0.51,
                                "running_duration": 74.51,
                                "completing_duration": 1.08
                            },
                            "start_time": dt.datetime.fromisoformat("2021-02-08T10:49:10.023+00:00"),
                            "finish_time": dt.datetime.fromisoformat("2021-02-08T10:50:27.023+00:00"),
                            "duration": 76.81,
                            "cpu": {
                                "core_predicted": 1,
                                "core_used_total": 0.91,
                                "core_used_in_user_and_proxy": 0.07,
                                "time_total": 70.58,
                                "time_in_user_and_proxy": 5.58
                            },
                            "ram": {
                                "predicted_memory_limit": 4294967296,
                                "predicted_tmpfs_size": 0,
                                "memory_limit": 4294967296,
                                "max_memory": 253157376,
                                "max_tmpfs_size": 0
                            }
                        }
                    }
                ]
            },
            "hostname": "slot_46.sas0-2849-node-hahn.sas.yp-c.yandex.net",
            "pid": -1,
            "pgid": -1,
            "ppid": -1,
            "thread_id": -1,
            "created_at": dt.datetime.fromisoformat("2021-02-08T10:49:10.023+00:00"),
            "start": dt.datetime.fromisoformat("2021-02-08T10:49:10.023+00:00"),
            "end": dt.datetime.fromisoformat("2021-02-08T10:50:27.023+00:00"),
            "error": None,
            "demands": [
                [
                    {
                        "name": "ymapsdf_ft_aao_yandex",
                        "key": "3e9c6cffb1d2efbcea6d6372f40044ba",
                        "class_name": "YtTableResource",
                        "uri": "https://yt.yandex-team.ru/hahn/navigation?path=//home/garden/final/ft",
                        "size": {
                            "bytes": 12491631
                        },
                        "properties": {
                            "region": "aao",
                            "shipping_date": "20210207_549230_2031_209695532",
                            "vendor": "yandex"
                        },
                        "task_key": None
                    }
                ]
            ],
            "creates": [
                [
                    {
                        "name": "renderer_denormalization_rd_el_shields_data_tmp_aao_yandex",
                        "key": "3493f2f1854a5acec6febcde370c67b5",
                        "class_name": "YtTableResource",
                        "uri": "https://yt.yandex-team.ru/hahn/navigation?path=//home/garden/data_tmp",
                        "size": {
                            "bytes": 1341
                        },
                        "properties": {
                            "region": "aao",
                            "shipping_date": "20210207_549230_2031_209695532",
                            "vendor": "yandex"
                        },
                        "task_key": None
                    }
                ]
            ]
        },
        # in progress task
        {
            "_id": ObjectId("60269cf2bea820c39d708ab6"),
            "tag": "ymapsdf:11317",
            "kind": "task_execution",
            "request_ids": [],
            "task_id": "9811681233477716522",
            "task_name": "ComputeAdCenterTask",
            "name": "ComputeAdCenterTask",
            "task_module_name": "ymapsdf",
            "module_name": "ymapsdf",
            "contour_name": "datatesting",
            "module_version": "7844688",
            "task_python_module": "ymapsdf",
            "python_module": "ymapsdf",
            "insert_traceback": "  File ",
            "predict_consumption": {
                "cpu": 1,
                "ram": 1073741824,
                "operations": 2
            },
            "consumption": {
                "cpu": 1,
                "ram": 1073741824,
                "operations": 2
            },
            "additional_data": {
                "ensure_available_duration": None,
                "commit_durations": [],
                "yt_operation_id": "8567d369-214ef504-3fe03e8-5e469359",
                "yt_cluster": "hahn",
                "log_url": None,
                "retry_attempt": 0,
                "yt_operations": []
            },
            "hostname": "unknown",
            "pid": -1,
            "pgid": -1,
            "ppid": -1,
            "thread_id": -1,
            "created_at": dt.datetime.fromisoformat("2021-02-12T15:21:08.023+00:00"),
            "start": dt.datetime.fromisoformat("2021-02-12T15:21:08.023+00:00"),
            "end": None,
            "error": None,
            "demands": [
                [
                    {
                        "name": "ymapsdf_beta_ad_center_geom_data_eu4_yandex",
                        "key": "8eedfe5a9d07782299c0e8c542af869e",
                        "class_name": "YtTableResource",
                        "uri": "https://yt.yandex.net/hahn//ad_center_geom_data",
                        "size": {
                            "bytes": 769418
                        },
                        "properties": {
                            "region": "eu4",
                            "shipping_date": "20210212_551133_2036_210135915",
                            "vendor": "yandex"
                        },
                        "task_key": None
                    }
                ]
            ],
            "creates": []
        }
    ])

    migrate_database._restructure_task_log(db)

    docs = []
    for doc in db.task_log.find():
        parsed = task_log_manager.TaskLogRecord.parse_obj(doc).dict()
        parsed["_id"] = str(parsed["_id"])
        if parsed.get("task_key"):
            parsed["task_key"] = str(parsed["task_key"])
        for resource in parsed["resources"]:
            # ya tools have special validation rules for `uri` fields
            assert resource.pop("uri", None)
        docs.append(parsed)
    return _datetime_to_str(docs)


@pytest.mark.use_local_mongo
def test_task_statistics_migration(db):
    db.task_statistics.insert_many([
        # finished task
        {
            "_id": ObjectId("5fb6839a075706e7c857daed"),
            "tag": "altay:731",
            "kind": "task_execution",
            "request_ids": [],
            "task_id": "16898755930817458103",
            "name": "RubricTask",
            "module_name": "altay",
            "python_module": "altay",
            "insert_traceback": "  File",
            "predict_consumption": {
                "cpu": 1,
                "ram": 1073741824,
                "operations": 1
            },
            "additional_data": {
                "ensure_available_duration": {
                    "total": 12299.784000000001,
                    "resources": []
                },
                "yt_operation_id": "90b3b2fd-a733702f-3fe03e8-57d1acc7",
                "yt_cluster": "hahn",
                "log_url": "http://s3.mds.yandex.net/.log",
                "retry_attempt": 0,
                "yt_operations": [
                    {
                        "id": "90b3b2fd-a733702f-3fe03e8-57d1acc7",
                        "type": "vanilla",
                        "start_time": date_string_to_datetime("2020-11-19T14:36:30.485Z"),
                        "finish_time": date_string_to_datetime("2020-11-19T14:38:13.517Z"),
                        "stats": {
                            "operation_count": 1,
                            "events": {
                                "starting_duration": 0.51,
                                "waiting_for_agent_duration": 0,
                                "initializing_duration": 0.91,
                                "preparing_duration": 0.03,
                                "pending_duration": 0,
                                "materializing_duration": 0.33,
                                "running_duration": 99.64,
                                "completing_duration": 1.61
                            },
                            "start_time": date_string_to_datetime("2020-11-19T14:36:30.485Z"),
                            "finish_time": date_string_to_datetime("2020-11-19T14:38:13.517Z"),
                            "duration": 103.03,
                            "cpu": {
                                "core_predicted": 1,
                                "core_used_total": 0.94,
                                "core_used_in_user_and_proxy": 0.06,
                                "time_total": 97.92,
                                "time_in_user_and_proxy": 6.68
                            },
                            "ram": {
                                "predicted_memory_limit": 1073741824,
                                "predicted_tmpfs_size": 0,
                                "memory_limit": 1073741824,
                                "max_memory": 205516800,
                                "max_tmpfs_size": 0
                            }
                        }
                    },
                    {
                        "id": "a554afa3-201dc115-3fe03e8-6472b6ec",
                        "type": "merge",
                        "start_time": date_string_to_datetime("2020-11-19T14:37:05.064Z"),
                        "finish_time": date_string_to_datetime("2020-11-19T14:37:38.726Z"),
                        "stats": {
                            "operation_count": 1,
                            "events": {
                                "starting_duration": 0.14,
                                "waiting_for_agent_duration": 0,
                                "initializing_duration": 0.41,
                                "preparing_duration": 0.4,
                                "pending_duration": 0,
                                "materializing_duration": 0.44,
                                "running_duration": 30.12,
                                "completing_duration": 2.16
                            },
                            "start_time": date_string_to_datetime("2020-11-19T14:37:05.064Z"),
                            "finish_time": date_string_to_datetime("2020-11-19T14:37:38.726Z"),
                            "duration": 33.66,
                            "cpu": {
                                "core_used_total": 0.72,
                                "core_used_in_user_and_proxy": 0.05,
                                "time_total": 24.91,
                                "time_in_user_and_proxy": 1.57
                            },
                            "ram": {
                                "max_memory": 180015104
                            }
                        }
                    }
                ]
            },
            "hostname": "slot_35.sas0-1091-node-hahn.sas.yp-c.yandex.net",
            "pid": -1,
            "pgid": -1,
            "ppid": -1,
            "thread_id": -1,
            "created_at": date_string_to_datetime("2020-11-19T14:39:45.411Z"),
            "start": date_string_to_datetime("2020-11-19T14:36:30.485Z"),
            "end": date_string_to_datetime("2020-11-19T14:38:13.517Z"),
            "contour_name": "stable",
            "creates": [
                "altay_rubrics:c591b53a00618cb3f212a9f2fe45d27b"
            ],
            "demands": [
                "src_altay:f79173380a8f313ad85a29b896dfc9c2"
            ]
        },
        # in progress task
        {
            "_id": ObjectId("6018b370448a2ff40b0abcf6"),
            "additional_data": {
                "ensure_available_duration": None,
                "commit_durations": [],
                "yt_operation_id": "94d65bd9-3ca4defe-3fe03e8-b3a4681f",
                "yt_cluster": "hahn",
                "log_url": None,
                "retry_attempt": 0,
                "yt_operations": []
            },
            "task_id": "11516591829644964809",
            "consumption": {
                "cpu": 1,
                "ram": 2147483648,
                "operations": 1
            },
            "contour_name": "stable",
            "created_at": date_string_to_datetime("2021-02-02T02:05:23.845Z"),
            "creates": [],
            "demands": [
                "matrix_router_data_build_ammo:ffb66ec7650a7053f1d5743673ce0069",
                "matrix_router_data_build_dataset_active_in_testing:4faf4f9f032a72ae8c132450fed4f40c"
            ],
            "end": None,
            "error": None,
            "hostname": "unknown",
            "insert_traceback": "  File \"maps",
            "kind": "task_execution",
            "module_name": "matrix_router_data_validator",
            "module_version": "7651164",
            "name": "_CompareMatrixRoutersTask",
            "pgid": -1,
            "pid": -1,
            "ppid": -1,
            "predict_consumption": {
                "cpu": 1,
                "ram": 2147483648,
                "operations": 1
            },
            "python_module": "matrix_router_data_validator",
            "request_ids": [],
            "start": date_string_to_datetime("2021-02-02T02:05:23.845Z"),
            "tag": "matrix_router_data_validator:200",
            "task_module_name": "matrix_router_data_validator",
            "task_name": "_CompareMatrixRoutersTask",
            "task_python_module": "matrix_router_data_validator",
            "thread_id": -1
        }
    ])

    migrate_database._restructure_task_statistics(db)

    docs = []
    for doc in db.task_statistics.find():
        parsed = task_log_manager.TaskStatisticsRecord.parse_obj(doc).dict()
        parsed["_id"] = str(parsed["_id"])
        docs.append(parsed)
    return _datetime_to_str(docs)


@pytest.mark.use_local_mongo
def test_drop_traits_acl_migration(db):
    db.module_versions.insert_one({
        "traits": {
            "name": "test_module",
            "type": "map",
            "acl": ["someuser", "otheruser"]
        }
    })

    migrate_database._drop_acl_from_traits(db)

    expected_traits = ModuleTraits(
        name="test_module",
        type=ModuleType.MAP,
    )

    assert ModuleTraits.parse_obj(db.module_versions.find_one()["traits"]) == expected_traits


@pytest.mark.use_local_mongo
def test_resource_statistics_migration(db):
    db.resource_statistics.insert_many([
        {
            "_id": "ymapsdf_cond_vehicle_restriction_tr_yandex:7efc3f2a18255831bde9387799c8ea6d",
            "class_name": "YtTableResource",
            "key": "7efc3f2a18255831bde9387799c8ea6d",
            "name": "ymapsdf_cond_vehicle_restriction_tr_yandex",
            "properties": {
                "region": "tr",
                "shipping_date": "20210118_540773_2011_207940906",
                "vendor": "yandex"
            },
            "size": {
                "bytes": 0
            },
            "tags": [
                "ymapsdf:10858",
                "ymapsdf_release_yt:625",
                "pedestrian_graph:774",
                "road_graph_build:536",
                "bicycle_graph:1252"
            ],
            "task_key": None,
            "uri": "https://yt.yandex.net/cond_vehicle_restriction"
        },
        {
            "_id": "data_rasp_on_yt_release:ddd8fbbac6daf0ef0f47a1acdd8be619",
            "class_name": "PythonResource",
            "key": "ddd8fbbac6daf0ef0f47a1acdd8be619",
            "name": "data_rasp_on_yt_release",
            "properties": {
                "release": "21.01.25-1"
            },
            "size": {

            },
            "tags": [
                "masstransit_deployment:1955"
            ],
            "task_key": ObjectId("600ea11526ce8757aed1c578"),
            "uri": None
        }
    ])

    migrate_database._restructure_resource_statistics(db)

    docs = []
    for doc in db.resource_statistics.find():
        parsed = task_log_manager.ResourceInfoRecord.parse_obj(doc).dict()
        parsed.pop("uri", None)
        parsed["create_task_keys"] = sorted(
            str(task_key)
            for task_key in parsed.get("create_task_keys", [])
        )
        parsed["tags"] = sorted(parsed["tags"])
        docs.append(parsed)
    return docs


@pytest.mark.use_local_mongo
def test_autostart_requests_unique_index(db):
    migrate_database._create_autostart_requests_unique_index(db)

    record = {
        "target_contour_name": "stable",
        "target_module_name": "ymapsdf",
        "delayed_run_at": None
    }

    db.autostart_requests.insert_one(record.copy())
    db.autostart_requests.insert_one(record.copy())

    record["delayed_run_at"] = dt.datetime(2021, 12, 30)

    db.autostart_requests.insert_one(record.copy())
    with pytest.raises(pymongo.errors.DuplicateKeyError):
        db.autostart_requests.insert_one(record.copy())


@pytest.mark.use_local_mongo
def test_remove_old_requests(db):
    db.builds.insert_one({
        "id": 1,
        "name": "altay",
        "module_version": "123456",
        "request_id": 111,
    })

    db.requests.insert_one({
        "_id": 111,
    })

    db.requests.insert_one({
        "_id": 222,
    })

    migrate_database._remove_old_requests(db)

    records = list(db.requests.find({}))
    assert records == [{"_id": 111}]


@pytest.mark.use_local_mongo
def test_operations_migration(db):
    db.operations.insert_many([
        {
            "_id": "f2f582f9-4dad72c7-3fe03e8-eea0ae54",
            "creation_data": {
                "type": "vanilla",
                "cluster": "hahn",
                "pool": "garden",
                "started_at": date_string_to_datetime("2021-02-10T15:03:39.233Z"),
                "task_id": "14692680696009645623",
                "task_name": "SplitTarToFilesTask",
                "task_key": ObjectId("6023f5c245f178baf1f3a8d5"),
                "module_name": "ymapsdf",
                "build_id": 9775,
                "contour_name": "testing",
                "yql_op_id": None,
                "yql_share_id": None
            },
            "result": {
                "state": "completed",
                "finished_at": date_string_to_datetime("2021-02-10T15:05:41.495Z"),
                "stats": {
                    "alerts": [],
                    "used_core": 0.95,
                    "max_used_memory": 259166208
                }
            }
        },
        {
            "_id": "6101a794-cbf01609-3fe03e8-246b391c",
            "creation_data": {
                "type": "map",
                "cluster": "hahn",
                "pool": "garden",
                "started_at": date_string_to_datetime("2021-03-02T13:38:13.538Z"),
                "task_id": "13859227424156289025",
                "task_name": "MakeFtPermalinkNmapsTask",
                "task_key": ObjectId("603e3e959402b540155b9f8e"),
                "module_name": "ymapsdf",
                "build_id": 9778,
                "contour_name": "study",
                "yql_op_id": "603e3ee503d34ef49e3d27a5",
                "yql_share_id": "YD4-5QPTTvSePSeliS9a6PinpDweUNf-3NwOqspp-Dw="
            },
            "result": {
                "state": "completed",
                "finished_at": date_string_to_datetime("2021-03-02T13:40:09.439Z"),
                "stats": {
                    "alerts": [],
                    "used_core": 0.84,
                    "max_used_memory": 375500800
                },
                "yql_stats": {
                    "started_at": date_string_to_datetime("2021-03-02T13:34:29.680Z"),
                    "finished_at": date_string_to_datetime("2021-03-02T13:47:58.787Z")
                }
            }
        },
        {
            "_id": "f222fdd1-ce464b3d-3fe03e8-87474246",
            "creation_data": {
                "type": "sort",
                "cluster": "hahn",
                "pool": "garden",
                "started_at": date_string_to_datetime("2021-03-03T07:23:18.697Z"),
                "task_id": "17726369013626649846",
                "task_name": "UploadToYtTableTask",
                "task_key": ObjectId("603f39189402b540155ba73f"),
                "module_name": "ymapsdf",
                "build_id": 9779,
                "contour_name": "testing",
                "yql_op_id": None,
                "yql_share_id": None
            },
            "result": None
        }
    ])

    migrate_database._restructure_operations(db, since_datetime=dt.datetime.fromtimestamp(0))

    docs = []
    for doc in db.task_operations.find():
        doc = operation_storage.TaskOperation.parse_obj(doc).dict()
        doc.pop("_id")
        doc["task"]["key"] = str(doc["task"]["key"])
        docs.append(doc)
    return _datetime_to_str(docs)


@pytest.mark.use_local_mongo
def test_remove_deprecated_build_fields(db):
    db.builds.insert_one({
        "id": 1,
        "name": "altay",
        "module_version": "123456",
        "status": {
            "string": "failed",
            "operation": "restart",
            "error": "Build failed",
        }
    })

    migrate_database._remove_build_status_deprecated_fields(db)

    records = list(db.builds.find({}, {"_id": False}))
    assert records == [{
        "id": 1,
        "name": "altay",
        "module_version": "123456",
        "status": {
            "string": "failed",
        }
    }]


@pytest.mark.use_local_mongo
def test_remove_deleted_contours(db):
    for i in range(5):
        db.contours.insert_one({
            "_id": i + 1,
            "owner": "tester",
            "is_system": False,
            "status": "deleted"
        })

    migrate_database._remove_deleted_contours(db)

    assert db.contours.count() == 0


@pytest.mark.freeze_time(datetime(2020, 10, 9, 13, 40, 00))
@pytest.mark.use_local_mongo
def test_refactor_dangling_resources(db):
    now = int(time.time())
    now_datetime = datetime.fromtimestamp(now)
    db.dangling_resources.insert_one({
        "insertion_time": now,
        "last_exception_value": "test",
        "doc_path": "test",
    })
    db.dangling_resources.insert_one({
        "insertion_time": now,
        "last_exception_value": None,
        "doc_path": None,
    })

    migrate_database._refactor_dangling_resources(db)

    for record in db.dangling_resources.find():
        assert "insertion_time" not in record
        assert "last_exception_value" not in record
        assert "doc_path" not in record
        # there are some mistakes with timezones, but it is not important here
        assert record["inserted_at"].day == now_datetime.day
        assert record["inserted_at"].year == now_datetime.year
        assert record["inserted_at"].month == now_datetime.month


@pytest.mark.use_local_mongo
def test_remove_id_being_tracked_flag(db):
    db.tasks.insert_one({
        "id": "id_1"
    })
    db.tasks.insert_one({
        "id": "id_2",
        "id_being_tracked": False
    })
    db.tasks.insert_one({
        "id": "id_3",
        "invocation_status": "unknown"
    })
    db.tasks.insert_one({
        "id": "id_4",
        "id_being_tracked": False,
        "invocation_status": "finished"
    })
    db.tasks.insert_one({
        "id": "id_5",
        "id_being_tracked": False,
        "invocation_status": "final"
    })
    db.tasks.insert_one({
        "id": "id_6",
        "id_being_tracked": False,
        "invocation_status": "finished"
    })
    db.tasks.insert_one({
        "id": "id_7",
        "id_being_tracked": True,
        "invocation_status": "running"
    })

    migrate_database._remove_id_being_tracked_flag(db)

    records = db.tasks.find()
    for record in records:
        assert record.get("id_being_tracked") is None

        if record["id"] != "id_7":
            assert record["invocation_status"] == "final"
        else:
            assert record["invocation_status"] == "running"


@pytest.mark.use_local_mongo
def test_rename_time_fields(db):
    db.contours.insert_one({
        "creation_time": "123",
    })

    migrate_database._rename_time_fields(db)

    records = list(db.contours.find({}, {"_id": False}))
    assert records == [{"created_at": "123"}]


@pytest.mark.use_local_mongo
def test_add_build_creation_data_required_fields(db: Database):
    db.build_actions.insert_many([
        {
            "build_id": 1,
        },
        {
            "build_id": 2,
            "build_creation_data": None,
        },
        {
            # the build_creation_data.external_resources field should appear here
            # the remaining fields should remain unchanged
            "build_id": 3,
            "build_creation_data": {},
        },
        {
            "build_id": 4,
            "build_creation_data": {
                "external_resources": [],
            },
        },
        {
            "build_id": 5,
            "build_creation_data": {
                "external_resources": [
                    {"key_properties": {"sandbox_resource_id": "31"}}
                ],
            },
        },
    ])

    migrate_database._add_build_creation_data_required_fields(db)

    return list(db.build_actions.find({}, {"_id": False}))


@pytest.mark.use_local_mongo
def test_add_foreign_key_to_source_builds(db: Database):
    db.build_actions.insert_many([
        # without build_creation_data
        {
            "build_id": 1,
        },
        # with empty external_resources
        {
            "build_creation_data": {
                "external_resources": [],
                "contour_name": "test",
            },
            "build_id": 2,
            "module_name": "ymapsdf"
        },
        # with one element in external_resources
        {
            "build_creation_data": {
                "external_resources": [
                    {"key_properties": {"sandbox_resource_id": "3"}}
                ],
                "contour_name": "test",
            },
            "build_id": 3,
            "module_name": "ymapsdf"
        },
        {
            "build_creation_data": {
                "external_resources": [
                    {"key_properties": {"sandbox_resource_id": "31"}}
                ],
                "contour_name": "test",
            },
            "build_id": 31,
            "module_name": "ymapsdf"
        },
        # with multiple elements in external_resources
        {
            "build_creation_data": {
                "external_resources": [
                    # this is the situation when key_properties of the external_resources are different
                    {"key_properties": {"sandbox_resource_id": "4"}},
                    {"key_properties": {"sandbox_resource_id": "41"}},
                ],
                "contour_name": "test",
            },
            "build_id": 4,
            "module_name": "ymapsdf"
        },
        # with null build_creation_data
        {
            "build_creation_data": None,
            "build_id": 5,
            "module_name": "ymapsdf"
        },
    ])

    db.builds.insert_many([
        {
            "id": 1,
            "contour_name": "test",
            "name": "ymapsdf"
        },
        {
            "id": 2,
            "contour_name": "test",
            "name": "ymapsdf"
        },
        {
            "id": 3,
            "contour_name": "test",
            "name": "ymapsdf"
        },
        {
            "id": 31,
            # this is the wrong situation when the keys of the build and the build_action are different
            "foreign_key": {
                "some_another_key_31": "value"
            },
            "contour_name": "test",
            "name": "ymapsdf"
        },
        {
            "id": 4,
            "contour_name": "test",
            "name": "ymapsdf"
        },
        {
            "id": 5,
            "contour_name": "test",
            "name": "ymapsdf"
        }
    ])

    migrate_database._add_foreign_key_to_source_builds(db)

    result = {
        "builds": list(db.builds.find({}, {"_id": False})),
        "build_actions": list(db.build_actions.find({}, {"_id": False})),
    }

    return result


@pytest.mark.use_local_mongo
def test_remove_builds_limit_field_from_traits(db):
    db.module_versions.insert_many(
        [
            {
                "traits": {
                    "name": "test_module",
                    "type": "map",
                    "builds_limit": "some_limit",
                }
            },
            {
                "traits": {
                    "name": "test_module",
                    "type": "map"
                }
            },
            {

            },
            {
                "traits": None
            },
        ]
    )

    migrate_database._remove_builds_limit_field_from_traits(db)

    expected_traits = ModuleTraits(
        name="test_module",
        type=ModuleType.MAP,
    )

    assert all(ModuleTraits.parse_obj(record["traits"]) == expected_traits for record in db.module_versions.find({"traits.name": "test_module"}))


@pytest.mark.use_local_mongo
def test_copy_traits_search_properties_to_builds_grouping(db):
    db.module_versions.insert_many(
        [
            {
                "traits": {
                    "name": "source",
                    "type": "source",
                    "search_properties": ["test"],
                    "builds_grouping": [],  # here should be ["test"]
                }
            },
            {
                "traits": {
                    "name": "map",
                    "type": "map",
                    "search_properties": ["test"],
                    "builds_grouping": [],  # here should be []
                }
            }
        ]
    )

    migrate_database._copy_traits_search_properties_to_builds_grouping(db)

    return list(db.module_versions.find({}, {"_id": False}))


@pytest.mark.use_local_mongo
def test_add_created_at_field_to_builds(db):
    db.builds.insert_many([
        {
            "id": 2,
            "name": "altay",
            "time": dt.datetime.now(),
            "status": {
                "string": "completed",
                "start_time": dt.datetime.now(),
            },
        },
        {
            "id": 2,
            "name": "altay",
            "time": dt.datetime.now(),
            "status": {
                "string": "completed",
            },
        },
        {
            "id": 2,
            "name": "altay",
            "time": dt.datetime.now(),
            "status": {
                "string": "completed",
                "finish_time": dt.datetime.now(),
            },
        },
    ])

    migrate_database._add_created_at_field_to_builds(db)
    for build in db.builds.find():
        assert "time" not in build
        assert build["created_at"]
        assert build["status"]["updated_at"]
        if "finish_time" in build["status"]:
            assert build["status"]["updated_at"] == build["status"]["finish_time"]
        elif "start_time" in build["status"]:
            assert build["status"]["updated_at"] == build["status"]["start_time"]
        else:
            assert build["status"]["updated_at"] == build["created_at"]


@pytest.mark.use_local_mongo
def test_fill_updated_at_field(db):
    builds = [
        {
            "id": 1,
            "name": "altay",
            "created_at": dt.datetime.fromisoformat("2019-03-18T13:25:38.023+00:00"),
            "status": {
                "string": "completed",
                "start_time": dt.datetime.fromisoformat("2019-03-19T13:25:38.023+00:00"),
                "updated_at": None,
            },
        },
        {
            "id": 2,
            "name": "altay",
            "created_at": dt.datetime.fromisoformat("2019-03-18T13:25:38.023+00:00"),
            "status": {
                "string": "completed",
                "updated_at": None,
            },
        },
        {
            "id": 3,
            "name": "altay",
            "created_at": dt.datetime.fromisoformat("2019-03-18T13:25:38.023+00:00"),
            "status": {
                "string": "completed",
                "start_time": dt.datetime.fromisoformat("2019-03-19T13:25:38.023+00:00"),
                "finish_time": dt.datetime.fromisoformat("2019-03-20T13:25:38.023+00:00"),
                "updated_at": None,
            },
        },
        {
            "id": 4,
            "name": "altay",
            "created_at": dt.datetime.fromisoformat("2019-03-18T13:25:38.023+00:00"),
            "status": {
                "string": "completed",
                "finish_time": dt.datetime.fromisoformat("2019-03-19T13:25:38.023+00:00"),
                "updated_at": dt.datetime.fromisoformat("2019-03-20T13:25:38.023+00:00"),
            },
        },
    ]

    db.builds.insert_many(builds)

    migrate_database._fill_updated_at_field(db)
    for build in db.builds.find():
        if build["id"] == 1:
            assert build["status"]["updated_at"] == build["status"]["start_time"]

        if build["id"] == 2:
            assert build["status"]["updated_at"] == build["created_at"]

        if build["id"] == 3:
            assert build["status"]["updated_at"] == build["status"]["finish_time"]

        if build["id"] == 4:
            assert build["status"]["updated_at"] == builds[3]["status"]["updated_at"]


@pytest.mark.use_local_mongo
def test_remove_registered_sources_collection(db):
    known_sources = [
        {"id": 1},
        {"id": 2},
        {"id": 3},
        {"id": 4},
    ]

    db.registered_sources.insert_many(known_sources)

    migrate_database._remove_registered_sources_collection(db)
    assert not list(db.registered_sources.find())


def test_add_updated_at_to_statistics(db):
    builds = [
        {
            "id": 1,
            "completed_at": dt.datetime.fromisoformat("2019-03-20T13:25:38.023+00:00"),
            "build_status_logs": [
                {
                    "status": {
                        "start_time": dt.datetime.fromisoformat("2019-03-18T13:25:38.023+00:00"),
                        "finish_time": dt.datetime.fromisoformat("2019-03-19T13:25:38.023+00:00"),
                    },
                },
                {
                    "status": {
                        "start_time": dt.datetime.fromisoformat("2019-03-21T13:25:38.023+00:00"),
                        "finish_time": dt.datetime.fromisoformat("2019-03-22T13:25:38.023+00:00"),
                    },
                },
            ]
        },
        {
            "id": 2,
            "completed_at": dt.datetime.fromisoformat("2019-03-20T13:25:38.023+00:00"),
            "build_status_logs": [
                {
                    "status": {
                        "start_time": dt.datetime.fromisoformat("2019-03-18T13:25:38.023+00:00"),
                    },

                },
                {
                    "status": {
                        "start_time": dt.datetime.fromisoformat("2019-03-21T13:25:38.023+00:00"),
                    },

                },
            ]
        },
        {
            "id": 3,
            "started_at": dt.datetime.fromisoformat("2019-03-20T13:25:38.023+00:00"),
            "build_status_logs": [
                {
                    "status": {},
                },
                {
                    "status": {},
                },
            ]
        },
        {
            "id": 4,
            "completed_at": dt.datetime.fromisoformat("2019-03-20T13:25:38.023+00:00"),
            "build_status_logs": [
                {
                    "status": {
                        "start_time": dt.datetime.fromisoformat("2019-03-18T13:25:38.023+00:00"),
                        "finish_time": dt.datetime.fromisoformat("2019-03-19T13:25:38.023+00:00"),
                        "updated_at": dt.datetime.fromisoformat("2019-03-24T13:25:38.023+00:00"),
                    }
                },
                {
                    "status": {
                        "start_time": dt.datetime.fromisoformat("2019-03-21T13:25:38.023+00:00"),
                        "finish_time": dt.datetime.fromisoformat("2019-03-22T13:25:38.023+00:00"),
                        "updated_at": dt.datetime.fromisoformat("2019-03-24T13:25:38.023+00:00"),
                    },
                },
            ]
        },
        {
            "id": 5,
            "started_at": dt.datetime.fromisoformat("2019-03-24T13:25:38.023+00:00"),
            "build_status_logs": [
                {
                    "status": {
                        "updated_at": None,
                    },
                },
                {
                    "status": {
                        "updated_at": None,
                    },
                },
            ]
        },
    ]

    db.build_statistics.insert_many(builds)

    migrate_database._add_updated_at_to_statistics(db)

    for record in db.build_statistics.find():
        if record["id"] == 1:
            for status in record["build_status_logs"]:
                assert "updated_at" in status["status"]
                assert status["status"]["updated_at"] == status["status"]["finish_time"]
        if record["id"] == 2:
            for status in record["build_status_logs"]:
                assert "updated_at" in status["status"]
                assert status["status"]["updated_at"] == status["status"]["start_time"]
        if record["id"] == 3:
            for status in record["build_status_logs"]:
                assert "updated_at" in status["status"]
                assert status["status"]["updated_at"] == record["started_at"]
        if record["id"] == 4:
            for status in record["build_status_logs"]:
                assert "updated_at" in status["status"]
                assert status["status"]["updated_at"] == dt.datetime.fromisoformat("2019-03-24T13:25:38.023+00:00")
        if record["id"] == 5:
            for status in record["build_status_logs"]:
                assert "updated_at" in status["status"]
                assert status["status"]["updated_at"] == record["started_at"]
