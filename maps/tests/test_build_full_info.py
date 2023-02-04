import datetime as dt

from bson.objectid import ObjectId
import pytest
import pytz

from maps.garden.sdk.module_traits.module_traits import ModuleType, ModuleTraits

MODULE_NAME = "test_module"
BUILD_ID = 15553
NOW = dt.datetime(2020, 12, 17, 15, 1, 22, tzinfo=pytz.utc)

FINISHED_TASK_STATISTICS = [
    {
        "_id": ObjectId("5fdb657505ff1f6bcd86baef"),
        "build_id": BUILD_ID,
        "contour_name": "unittest",
        "created_at": dt.datetime.fromisoformat("2020-12-17T14:04:40.994+00:00"),
        "creates": [
            "52d116ed501c7df734635db57509da70"
        ],
        "demands": [
            "fc8e8ed016ca589c8a78df7c4c54c373",
            "unexistent_resource_key",
        ],
        "error": None,
        "finished_at": dt.datetime.fromisoformat("2020-12-17T14:04:21.155+00:00"),
        "garden_task_id": "16838168248507266062",
        "insert_traceback": "traceback1",
        "log_url": "http://s3.mds.yandex.net/test.log",
        "main_operation_id": "59f72445-41d82fc3-3fe03e8-5075cef6",
        "module_name": MODULE_NAME,
        "module_version": "1",
        "name": "MergeMainDataWithExperiments",
        "predicted_consumption": {
            "cpu": 1.0,
            "operations": 1,
            "ram": 1073741824
        },
        "resource_durations": {
            "key_to_commit": {
                "52d116ed501c7df734635db57509da70": 5.0
            },
            "key_to_preparation": {
                "fc8e8ed016ca589c8a78df7c4c54c373": 10.0
            }
        },
        "retry_attempt": 0,
        "started_at": dt.datetime.fromisoformat("2020-12-17T13:58:29.376+00:00"),
        "task_durations": {
            "commit": 5.0,
            "execution": 333.779755,
            "preparation": 12.999245,
            "total": 351.779
        }
    },
    {
        "_id": ObjectId("5fdb644605ff1f6bcd86ba72"),
        "build_id": BUILD_ID,
        "contour_name": "unittest",
        "creates": [
            "09b3d331b47a64de63a2b2e22ee94101"
        ],
        "demands": [
            "fc8e8ed016ca589c8a78df7c4c54c373",
            "19b3d331b47a64de63a2b2e22ee94101"
        ],
        "error": "error text",
        "garden_task_id": "13175954018438364733",
        "insert_traceback": "traceback2",
        "log_url": "http://s3.mds.yandex.net/test1.log",
        "main_operation_id": "495b1c1b-3e4b061a-3fe03e8-c3410a6d",
        "module_name": MODULE_NAME,
        "module_version": "unknown",
        "name": "CopyExperimentsTable",
        "operations": [],
        "predicted_consumption": {
            "cpu": 1.0,
            "operations": 1,
            "ram": 1073741824
        },
        "resource_durations": {
            "key_to_commit": {},
            "key_to_preparation": {}
        },
        "retry_attempt": 0,
        "scheduler_events": {
            "ready_for_execution": dt.datetime.fromisoformat("2020-12-17T13:59:40.723+00:00"),
            "operation_created": dt.datetime.fromisoformat("2020-12-17T13:58:32.077+00:00"),
            "finished": dt.datetime.fromisoformat("2020-12-17T13:59:19.097+00:00"),
        },
        "module_events": {
            "started": dt.datetime.fromisoformat("2020-12-17T13:58:32.077+00:00"),
            "resource_preparation_started": dt.datetime.fromisoformat("2020-12-17T13:58:37.077+00:00"),
            "invocation_started": dt.datetime.fromisoformat("2020-12-17T13:58:39.077+00:00"),
            "resource_commitment_started": dt.datetime.fromisoformat("2020-12-17T13:59:05.097+00:00"),
            "finished": dt.datetime.fromisoformat("2020-12-17T13:59:10.097+00:00"),
        },
    },
    {
        "_id": ObjectId("5fdb657509ff1f6bcd86baef"),
        "build_id": BUILD_ID - 1,
        "contour_name": "unittest",
        "created_at": dt.datetime.fromisoformat("2020-12-16T13:59:40.723+00:00"),
        "creates": [
            "19b3d331b47a64de63a2b2e22ee94101"
        ],
        "demands": [
            "ac8e8ed016ca589c8a78df7c4c54c373"
        ],
        "error": None,
        "finished_at": dt.datetime.fromisoformat("2020-12-16T13:59:19.097+00:00"),
        "garden_task_id": "14175954018438364733",
        "insert_traceback": "traceback2",
        "log_url": "http://s3.mds.yandex.net/test1.log",
        "main_operation_id": "195b1c1b-3e4b061a-3fe03e8-c3410a6d",
        "module_name": MODULE_NAME,
        "module_version": "unknown",
        "name": "CopyExperimentsTable",
        "operations": [],
        "predicted_consumption": {
            "cpu": 1.0,
            "operations": 1,
            "ram": 1073741824
        },
        "resource_durations": {
            "key_to_commit": {},
            "key_to_preparation": {}
        },
        "retry_attempt": 0,
        "started_at": dt.datetime.fromisoformat("2020-12-16T13:58:32.077+00:00"),
        "task_durations": {
            "commit": 0.0,
            "execution": 39.609211,
            "preparation": 7.410789,
            "total": 47.02
        }
    }
]

RUNNING_TASK_STATISTICS = [
    {
        "_id": ObjectId("6fdb644605ff1f6bcd86ba72"),
        "build_id": BUILD_ID,
        "contour_name": "unittest",
        "created_at": dt.datetime.fromisoformat("2020-12-17T14:59:40.723+00:00"),
        "creates": [],
        "demands": [
            "09b3d331b47a64de63a2b2e22ee94101"
        ],
        "error": "error text",
        "finished_at": None,
        "garden_task_id": "13175954018438364734",
        "insert_traceback": "traceback2",
        "log_url": None,
        "main_operation_id": "495b1c1b-3e4b061a-3fe03e8-c3410a6d",
        "module_name": MODULE_NAME,
        "module_version": "unknown",
        "name": "TestTask",
        "operations": [],
        "predicted_consumption": {
            "cpu": 2.0,
            "operations": 1,
            "ram": 2073741824
        },
        "resource_durations": None,
        "retry_attempt": 0,
        "started_at": dt.datetime.fromisoformat("2020-12-17T14:58:32.077+00:00"),
        "task_durations": None
    }
]

OPERATIONS = [
    {
        "operation": {
            "id": "59f72445-41d82fc3-3fe03e8-5075cef6",
            "cluster": "hahn",
            "pool": "garden_development",
            "type": "vanilla",
            "started_at": dt.datetime.fromisoformat("2020-12-17T11:58:29.376+00:00"),
            "finished_at": dt.datetime.fromisoformat("2020-12-17T15:04:21.155+00:00"),
        },
        "task": {
            "build_id": BUILD_ID,
            "contour_name": "unittest",
            "module_name": MODULE_NAME,
            "garden_task_id": "16838168248507266062",
            "key": ObjectId("5fdb657505ff1f6bcd86baef"),
            "name": "MergeMainDataWithExperiments",
        },
        "result": {
            "started_at": dt.datetime.fromisoformat("2020-12-17T13:58:29.376+00:00"),
            "finished_at": dt.datetime.fromisoformat("2020-12-17T14:04:21.155+00:00"),
            "state": "completed",
            "stats": {
                "alerts": [
                    "alert1"
                ],
                "max_used_memory": 228073472,
                "used_core": 0.97
            }
        }
    },
    {
        "operation": {
            "id": "110be9f6-24a00362-3fe03e8-ae4717a2",
            "cluster": "hahn",
            "pool": "garden_development",
            "type": "reduce",
            "started_at": dt.datetime.fromisoformat("2020-12-17T13:58:44.070+00:00"),
            "finished_at": dt.datetime.fromisoformat("2020-12-17T14:00:09.621+00:00"),
        },
        "task": {
            "build_id": BUILD_ID,
            "contour_name": "unittest",
            "module_name": MODULE_NAME,
            "garden_task_id": "16838168248507266062",
            "key": ObjectId("5fdb657505ff1f6bcd86baef"),
            "name": "MergeMainDataWithExperiments",
        },
        "result": {
            "started_at": dt.datetime.fromisoformat("2020-12-17T13:58:44.070+00:00"),
            "finished_at": dt.datetime.fromisoformat("2020-12-17T14:00:09.621+00:00"),
            "state": "completed",
            "stats": {
                "alerts": [
                    "alert2"
                ],
                "max_used_memory": 376557568,
                "used_core": 11.66
            }
        }
    },
    {
        "operation": {
            "id": "b79184cd-b83ce08-3fe03e8-48a8ec0a",
            "cluster": "hahn",
            "pool": "garden_development",
            "type": "sort",
            "started_at": dt.datetime.fromisoformat("2020-12-17T14:00:14.968+00:00"),
            "finished_at": dt.datetime.fromisoformat("2020-12-17T14:03:35.787+00:00"),
        },
        "task": {
            "build_id": BUILD_ID,
            "contour_name": "unittest",
            "module_name": MODULE_NAME,
            "garden_task_id": "16838168248507266062",
            "key": ObjectId("5fdb657505ff1f6bcd86baef"),
            "name": "MergeMainDataWithExperiments",
        },
        "result": {
            "started_at": dt.datetime.fromisoformat("2020-12-17T14:00:14.968+00:00"),
            "finished_at": dt.datetime.fromisoformat("2020-12-17T14:03:35.787+00:00"),
            "state": "completed",
            "stats": {
                "alerts": [],
                "max_used_memory": 1927639040,
                "used_core": 0.95
            }
        }
    },
    {
        "operation": {
            "id": "b791841d-b83ce08-3fe03e8-48a8ec0a",
            "cluster": "hahn",
            "pool": "garden_development",
            "type": "sort",
            "started_at": dt.datetime.fromisoformat("2020-12-17T14:00:15.968+00:00"),
            "finished_at": dt.datetime.fromisoformat("2020-12-17T14:03:36.787+00:00"),
        },
        "task": {
            "build_id": BUILD_ID,
            "contour_name": "unittest",
            "module_name": MODULE_NAME,
            "garden_task_id": "16838168248507266062",
            "key": ObjectId("5fdb657505ff1f6bcd86baef"),
            "name": "MergeMainDataWithExperiments",
        },
        "yql": {
            "op_id": "yql_op_id",
            "share_id": "yql_share_id"
        },
        "result": {
            "started_at": dt.datetime.fromisoformat("2020-12-17T14:00:15.968+00:00"),
            "finished_at": dt.datetime.fromisoformat("2020-12-17T14:03:36.787+00:00"),
            "state": "completed",
            "stats": {
                "alerts": ["yql_alert1"],
                "max_used_memory": 1927639040,
                "used_core": 0.95
            }
        }
    },
    {
        "operation": {
            "id": "b791842d-b83ce08-3fe03e8-48a8ec0a",
            "cluster": "hahn",
            "pool": "garden_development",
            "started_at": dt.datetime.fromisoformat("2020-12-17T14:00:16.968+00:00"),
            "finished_at": dt.datetime.fromisoformat("2020-12-17T14:03:37.787+00:00"),
            "type": "sort",
        },
        "task": {
            "build_id": BUILD_ID,
            "contour_name": "unittest",
            "module_name": MODULE_NAME,
            "garden_task_id": "16838168248507266062",
            "key": ObjectId("5fdb657505ff1f6bcd86baef"),
            "name": "MergeMainDataWithExperiments",
        },
        "yql": {
            "op_id": "yql_op_id",
            "share_id": "yql_share_id",
            "stats": {
                "started_at": dt.datetime.fromisoformat("2020-12-17T14:00:14.968+00:00"),
                "finished_at": dt.datetime.fromisoformat("2020-12-17T14:03:38.787+00:00"),
            }
        },
        "result": {
            "started_at": dt.datetime.fromisoformat("2020-12-17T14:00:16.968+00:00"),
            "finished_at": dt.datetime.fromisoformat("2020-12-17T14:03:37.787+00:00"),
            "state": "completed",
            "stats": {
                "alerts": ["yql_alert2"],
                "max_used_memory": 1927639040,
                "used_core": 0.95
            },
        }
    },
]

RESOURCE_STATISTICS = [
    {
        "_id": "09b3d331b47a64de63a2b2e22ee94101",
        "class_name": "YtTableResource",
        "created_at": dt.datetime.fromisoformat("2020-12-17T13:59:40.439+00:00"),
        "name": "resource1",
        "properties": {
            "release_name": "2020-12-17"
        },
        "size": {
            "bytes": 5935456662
        },
        "tags": [
            f"{MODULE_NAME}:{BUILD_ID}"
        ],
        "uri": "http://resource1",
        "create_task_keys": [
            ObjectId("5fdb644605ff1f6bcd86ba12"),
            ObjectId("5fdb644605ff1f6bcd86ba72")
        ]
    },
    {
        "_id": "fc8e8ed016ca589c8a78df7c4c54c373",
        "class_name": "YtSourcePathResource",
        "created_at": dt.datetime.fromisoformat("2020-12-17T14:04:40.849+00:00"),
        "name": "resource2",
        "properties": {
            "release_name": "2020-12-17",
            "yt_path": "//home/maps/poi/test_module/stable_v2/2020-12-17T07:01:14.510379+00:00"
        },
        "size": {},
        "tags": [
            f"{MODULE_NAME}:{BUILD_ID}"
        ],
        "uri": "http://resource2"
    },
    {
        "_id": "52d116ed501c7df734635db57509da70",
        "class_name": "YtTableResource",
        "created_at": dt.datetime.fromisoformat("2020-12-17T14:04:40.709+00:00"),
        "name": "resource3",
        "properties": {
            "release_name": "2020-12-17"
        },
        "size": {
            "bytes": 327950469
        },
        "tags": [
            f"{MODULE_NAME}:{BUILD_ID}"
        ],
        "uri": "http://resource3",
        "create_task_keys": [
            ObjectId("5fdb657505ff1f6bcd86baef")
        ]
    },
    {
        "_id": "19b3d331b47a64de63a2b2e22ee94101",
        "class_name": "YtTableResource",
        "created_at": dt.datetime.fromisoformat("2020-12-16T14:04:40.709+00:00"),
        "name": "resource4",
        "properties": {
            "release_name": "2020-12-16"
        },
        "size": {
            "bytes": 127950469
        },
        "tags": [
            f"{MODULE_NAME}:{BUILD_ID}",
            f"{MODULE_NAME}:{BUILD_ID - 1}"
        ],
        "uri": "http://resource4",
        "create_task_keys": [
            ObjectId("5fdb657509ff1f6bcd86baef")
        ]
    }
]

BUILD_STATISTICS = [
    {
        "_id": ObjectId("5fdb63fde350eef521c6d8c8"),
        "id": BUILD_ID,
        "name": MODULE_NAME,
        "properties": {
            "release_name": "2020-12-17",
        },
        "contour_name": "unittest",
        "build_status_logs": [
            {
                "status": {
                    "string": "completed",
                    "total_tasks_number": 2,
                    "successful_tasks_number": 2,
                    "start_time": dt.datetime.fromisoformat("2020-12-17T13:58:22.989+00:00"),
                    "finish_time": dt.datetime.fromisoformat("2020-12-17T14:04:37.404+00:00"),
                },
                "request_id": 262133,
                "author": "resource_scanner",
            }
        ],
        "sources": [
            {
                "name": MODULE_NAME,
                "version": "key:fc8e8ed016ca589c8a78df7c4c54c373"
            }
        ],
        "started_at": dt.datetime.fromisoformat("2020-12-17T13:58:22.989+00:00"),
        "finished_at": dt.datetime.fromisoformat("2020-12-17T14:04:37.404+00:00"),
    }
]


@pytest.mark.freeze_time(NOW)
@pytest.mark.parametrize(
    "tasks",
    [
        FINISHED_TASK_STATISTICS,
        FINISHED_TASK_STATISTICS + RUNNING_TASK_STATISTICS
    ]
)
def test_build_full_info(garden_client, module_helper, db, tasks):
    db.build_statistics.insert(BUILD_STATISTICS)
    db.resource_statistics.insert(RESOURCE_STATISTICS)
    db.task_statistics.insert(tasks)
    db.task_operations.insert(OPERATIONS)

    traits = ModuleTraits(
        name=MODULE_NAME,
        displayed_name=MODULE_NAME,
        type=ModuleType.MAP
    )
    module_helper.add_module_to_system_contour(traits)

    response = garden_client.get(f"/modules/{MODULE_NAME}/builds/{BUILD_ID}/full_info/")
    response = response.get_json()
    response["moduleVersions"] = sorted(response.get("moduleVersions", []), key=lambda x: x["version"])
    return response
