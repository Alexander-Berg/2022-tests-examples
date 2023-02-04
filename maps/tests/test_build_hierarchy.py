from datetime import datetime

from maps.garden.libs_server.build import build_defs, build_statistics


BUILD_TIME = datetime.fromisoformat("2020-07-07T19:09:43.379+00:00")

BUILDS_LIST = [
    build_statistics.BuildStatisticsRecord(
        id=1,
        name="src",
        contour_name="stable",
        properties={
            "autostarted": False,
            "region": "na",
            "shipping_date": "20200220_429984_1450_173448264",
            "shipping_id": "20200220_429984_1450_173448264",
            "vendor": "yandex",
        },
        build_status_logs=[
            build_statistics.BuildStatusLog(
                status=build_defs.BuildStatus(
                    string="completed",
                    finish_time=BUILD_TIME,
                    start_time=BUILD_TIME,
                    operation="remove",
                ),
                request_id=200653,
            )
        ],
        sources=[
            build_defs.Source(
                version=build_defs.SourceVersion(key="1908601caf53259397c36b98aa96654b"),
                name="src_url_na_yandex"
            )
        ],
        started_at=BUILD_TIME,
        completed_at=BUILD_TIME,
        finished_at=BUILD_TIME
    ),
    build_statistics.BuildStatisticsRecord(
        id=2,
        name="ymapsdf",
        contour_name="stable",
        properties={
            "autostarted": True,
            "region": "na",
            "shipping_date": "20200220_429984_1450_173448264",
            "vendor": "yandex",
        },
        build_status_logs=[
            build_statistics.BuildStatusLog(
                status=build_defs.BuildStatus(
                    string="completed",
                    finish_time=BUILD_TIME,
                    start_time=BUILD_TIME,
                    successful_tasks_number=442,
                    total_tasks_number=442,
                    operation="remove",
                ),
                request_id=200653,
                author="garden-autostarter",
            )
        ],
        sources=[
            build_defs.Source(
                version=build_defs.SourceVersion(build_id="1"),
                name="src"
            )
        ],
        started_at=BUILD_TIME,
        completed_at=BUILD_TIME,
        finished_at=BUILD_TIME
    ),
    build_statistics.BuildStatisticsRecord(
        id=3,
        name="renderer_denormalization",
        contour_name="stable",
        properties={
            "autostarted": True,
            "region": "na",
            "shipping_date": "20200220_429984_1450_173448264",
            "vendor": "yandex"
        },
        build_status_logs=[
            build_statistics.BuildStatusLog(
                status=build_defs.BuildStatus(
                    string="completed",
                    finish_time=BUILD_TIME,
                    start_time=BUILD_TIME,
                    successful_tasks_number=139,
                    total_tasks_number=139,
                    operation="remove",
                ),
                request_id=200692,
                author="garden-autostarter",
            )
        ],
        sources=[
            build_defs.Source(
                version=build_defs.SourceVersion(build_id="2"),
                name="ymapsdf"
            )
        ],
        started_at=BUILD_TIME,
        completed_at=BUILD_TIME,
        finished_at=BUILD_TIME,
        tracked_ancestor_builds=[build_defs.TrackedAncestorBuild(name="ymapsdf_src", build_id=0)],
    )
]


def test_build_hierarchy(garden_client, db):
    db.build_statistics.insert_many([r.dict() for r in BUILDS_LIST])
    response = garden_client.get(
        "/build_hierarchy/?module=ymapsdf&build_id=2",
    )
    return response.get_json()
