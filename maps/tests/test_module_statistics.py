from datetime import datetime, timedelta
import urllib


# mongodb can't store microseconds
BUILD_TIME = datetime.fromisoformat("2020-07-07T19:09:43.379+00:00")
FROM_TIME = BUILD_TIME - timedelta(days=10)


MODULE_STATISTICS_BUILDS = [
    {
        "name": "ymapsdf",
        "id": 1,
        "contour_name": "unittest",
        "build_status_logs": [
            {
                "status": {
                    "string": "completed",
                    "finish_time": FROM_TIME - timedelta(days=1),
                    "start_time": FROM_TIME - timedelta(days=1),
                },
                "request_id": 0
            }
        ],
        "finished_at": FROM_TIME - timedelta(days=1),
        "started_at": FROM_TIME - timedelta(days=1),
        "completed_at": FROM_TIME - timedelta(days=1),
        "properties": {
            "release_name": "1-2-3",
            "shipping_date": "12345",
            "vendor": "yandex",
            "region": "cis1",
            "some_other_property": "data"
        },
        "tracked_ancestor_builds": []
    },
    {
        "name": "ymapsdf",
        "id": 2,
        "contour_name": "unittest",
        "build_status_logs": [
            {
                "status": {
                    "string": "completed",
                    "finish_time": FROM_TIME - timedelta(days=1),
                    "start_time": FROM_TIME - timedelta(days=1),
                },
                "request_id": 1
            },
            {
                "status": {
                    "string": "not_completed",
                    "finish_time": BUILD_TIME,
                    "start_time": FROM_TIME - timedelta(days=1),
                },
                "request_id": 2
            }
        ],
        "finished_at": BUILD_TIME,
        "completed_at": FROM_TIME - timedelta(days=1),
        "started_at": FROM_TIME - timedelta(days=1),
        "tracked_ancestor_builds": [{"name": "ymapsdf_src", "build_id": 0}]
    },
    # should not be included in the statistics
    {
        "name": "ymapsdf",
        "id": 3,
        "contour_name": "testing",
        "build_status_logs": [
            {
                "status": {
                    "string": "completed",
                    "finish_time": BUILD_TIME,
                    "start_time": BUILD_TIME,
                },
                "request_id": 3
            }
        ],
        "finished_at": BUILD_TIME,
        "completed_at": BUILD_TIME,
        "started_at": BUILD_TIME,
        "tracked_ancestor_builds": [{"name": "ymapsdf_src", "build_id": 0}]
    },
    # should not be included in the statistics
    {
        "name": "src",
        "id": 3,
        "contour_name": "unittest",
        "build_status_logs": [
            {
                "status": {
                    "string": "completed",
                    "finish_time": BUILD_TIME,
                    "start_time": BUILD_TIME,
                },
                "request_id": 4
            }
        ],
        "finished_at": BUILD_TIME,
        "completed_at": BUILD_TIME,
        "started_at": BUILD_TIME,
        "tracked_ancestor_builds": [],
    },
]


def test_module_statistics(garden_client, db):
    db.build_statistics.insert_many(MODULE_STATISTICS_BUILDS)
    response = garden_client.get(
        f"/module_statistics/?module=ymapsdf&contour=unittest&from={urllib.parse.quote(FROM_TIME.isoformat())}",
    )
    return response.get_json()


def test_module_build_statistics(garden_client, db):
    db.build_statistics.insert_many(MODULE_STATISTICS_BUILDS)
    response = garden_client.get(
        "/modules/ymapsdf/build_statistics/3/",
    )
    return response.get_json()
