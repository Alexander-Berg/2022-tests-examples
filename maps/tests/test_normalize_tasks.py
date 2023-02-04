import datetime as dt

from bson.objectid import ObjectId

import yt.wrapper as yt

from maps.garden.libs_server.task import task_log_manager
from maps.garden.tools.perf_normalizer.lib.normalize_tasks import NormalizeTasks


def test_normalize_tasks(db):
    start_date = dt.datetime(year=2015, month=1, day=1)
    logs = [
        _generate_task_log_record(
            module_name="module_name1",
            build_id=1,
            started_at=start_date + dt.timedelta(days=i),
            finished_at=start_date + dt.timedelta(hours=2, days=i)
        )
        for i in range(5)
    ] + [
        _generate_task_log_record(
            module_name="module_name2",
            build_id=2,
            started_at=start_date + dt.timedelta(days=i),
            finished_at=start_date + dt.timedelta(hours=2, days=i)
        )
        for i in range(3)
    ] + [
        _generate_task_log_record(
            module_name="module_name3",
            build_id=3,
            started_at=start_date + dt.timedelta(days=0),
            finished_at=start_date + dt.timedelta(hours=2, days=0),
            creates=[
                task_log_manager.ResourceInfo(
                    name="tars_resource",
                    key="b182997a20ce978c586a86a6c93d00d0",
                    class_name="DirResource",
                    size={},
                    properties={
                        'vendor': 'navteq',
                        'region': 'australia',
                        'shipping_date': '20130701'
                    }
                )
            ],
            demands=[
                task_log_manager.ResourceInfo(
                    name="file_resource",
                    key="85826760d880f191454792c7cebc1a9d",
                    class_name="FileResource",
                    size={},
                    properties={
                        'shipping_date': '20130701',
                        'region': 'australia',
                        'vendor': 'navteq'
                    }
                )
            ]
        ),
        _generate_task_log_record(
            module_name="module_name4",
            build_id=4,
            started_at=start_date + dt.timedelta(days=0),
            finished_at=start_date + dt.timedelta(hours=2, days=0),
            creates=[
                task_log_manager.ResourceInfo(
                    name="face_beta_north_america_navteq",
                    key="c7904cea688680abcca566437ed9f9c2",
                    class_name="PostgresqlTableResource",
                    size={},
                    properties={
                        'vendor': 'navteq',
                        'region': 'north_america',
                        'shipping_date': '20130701',
                        'software': {
                            'yandex-maps-ymapsdf-schemas2': '2.21.2-0'
                        }
                    }
                )
            ],
            demands=[
                task_log_manager.ResourceInfo(
                    name="src_australia_navteq",
                    key="b182997a20ce978c586a86a6c93d00d0",
                    class_name="DirResource",
                    size={},
                    properties={
                        'vendor': 'navteq',
                        'region': 'australia',
                        'shipping_date': '20130701'
                    }
                )
            ]
        )
    ]

    db.task_log.insert_many([log.dict() for log in logs])

    assert db.task_log.count() == 10

    normalize_tasks = NormalizeTasks(db)
    normalize_tasks.run()

    assert db.task_statistics.count() == 10

    assert db.task_statistics.count({"module_name": "module_name1"}) == 5
    assert db.resource_statistics.count() == 3
    assert db.resource_statistics.count({"tags": "module_name1:1"}) == 0
    assert db.resource_statistics.count({"tags": "module_name2:2"}) == 0
    assert db.resource_statistics.count({"tags": "module_name3:3"}) == 2
    assert db.resource_statistics.count({"tags": "module_name4:4"}) == 2

    resource = db.resource_statistics.find_one(
        {"_id": "b182997a20ce978c586a86a6c93d00d0"}
    )
    assert len(resource["tags"]) == 2


def _generate_task_log_record(module_name, build_id, started_at, finished_at, creates=None, demands=None):
    return task_log_manager.TaskLogRecord(
        task_info=task_log_manager.TaskInfo(
            name="test_task_name",
            garden_task_id="100",
            predicted_consumption={'cpu': 2, 'ram': 16777216},
            module_name=module_name,
            build_id=build_id,
            contour_name="test_contour_name",
            creates=[r.key for r in creates or []],
            demands=[r.key for r in demands or []],
            insert_traceback="traceback",
            created_at=started_at,
            started_at=started_at,
            finished_at=finished_at
        ),
        task_key=ObjectId(),
        main_operation_id=yt.common.generate_uuid(),
        resources=(creates or []) + (demands or [])
    )
