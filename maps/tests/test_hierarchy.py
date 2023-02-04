from datetime import datetime

import mongomock
import pytest

from maps.garden.libs_server.build import build_defs, build_statistics, hierarchy


# mongodb can't store microseconds
BUILD_TIME = datetime.fromisoformat("2020-07-07T19:09:43.379+00:00")


BUILDS_LIST = [
    build_statistics.BuildStatisticsRecord(
        id=17768,
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
                    updated_at=BUILD_TIME,
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
        finished_at=BUILD_TIME
    ),
    build_statistics.BuildStatisticsRecord(
        id=7438,
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
                    updated_at=BUILD_TIME,
                ),
                request_id=200653,
                author="garden-autostarter",
            )
        ],
        sources=[
            build_defs.Source(
                version=build_defs.SourceVersion(build_id="17768"),
                name="src"
            )
        ],
        started_at=BUILD_TIME,
        finished_at=BUILD_TIME
    ),
    build_statistics.BuildStatisticsRecord(
        id=6048,
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
                    updated_at=BUILD_TIME,
                ),
                request_id=200692,
                author="garden-autostarter",
            )
        ],
        sources=[
            build_defs.Source(
                version=build_defs.SourceVersion(build_id="7438"),
                name="ymapsdf"
            )
        ],
        started_at=BUILD_TIME,
        finished_at=BUILD_TIME
    ),
    build_statistics.BuildStatisticsRecord(
        id=1842,
        name="renderer_publication",
        contour_name="stable",
        properties={
            "autostarted": True,
            "release_name": "20.02.20-1",
        },
        build_status_logs=[
            build_statistics.BuildStatusLog(
                status=build_defs.BuildStatus(
                    string="completed",
                    finish_time=BUILD_TIME,
                    start_time=BUILD_TIME,
                    successful_tasks_number=220,
                    total_tasks_number=220,
                    operation="remove",
                    updated_at=BUILD_TIME,
                ),
                request_id=200766,
                author="garden-autostarter",
            )
        ],
        sources=[
            build_defs.Source(
                version=build_defs.SourceVersion(build_id="6048"),
                name="renderer_denormalization"
            ),
            build_defs.Source(
                version=build_defs.SourceVersion(build_id="29"),
                name="renderer_map_stable_bundle"
            ),
            build_defs.Source(
                version=build_defs.SourceVersion(key="2dcd6e5bf353f33c3a8fbf3fc356e3ab"),
                name="build_params"
            ),
        ],
        started_at=BUILD_TIME,
        finished_at=BUILD_TIME
    ),
]


OTHER_BUILDS = [
    build_statistics.BuildStatisticsRecord(
        id=1,
        name="src",
        contour_name="stable",
        properties={
            "autostarted": False,
            "region": "na",
            "shipping_date": "20200220_429984_1450_173448264",
            "shipping_id": "20200220_429984_1450_173448264",
            "vendor": "yandex"
        },
        build_status_logs=[
            build_statistics.BuildStatusLog(
                status=build_defs.BuildStatus(
                    string="completed",
                    finish_time=BUILD_TIME,
                    start_time=BUILD_TIME,
                    successful_tasks_number=0,
                    total_tasks_number=0,
                    operation="remove",
                    updated_at=BUILD_TIME,
                ),
                request_id=200766,
            )
        ],
        sources=[
            build_defs.Source(
                version=build_defs.SourceVersion(key="1908601caf53259397c36b98aa96654b"),
                name="src_url_na_yandex"
            ),
        ],
        started_at=BUILD_TIME,
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
            "vendor": "yandex"
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
                    updated_at=BUILD_TIME,
                ),
                request_id=200766,
                author="garden-autostarter",
            )
        ],
        sources=[
            build_defs.Source(
                version=build_defs.SourceVersion(build_id="1"),
                name="src"
            ),
        ],
        started_at=BUILD_TIME,
        finished_at=BUILD_TIME
    ),
]


def test_hierarchy_builder_absent_build():
    storage = build_statistics.BuildStatisticsStorage(
        mongomock.MongoClient(tz_aware=True).db,
    )
    builder = hierarchy.Builder(storage, "some_build", 1)
    result = builder.get_hierarchy()
    assert result == []


@pytest.mark.parametrize(
    ("direction", "modules"),
    [
        (
            hierarchy.Direction.UP,
            ["renderer_denormalization", "renderer_publication"],
        ),
        (
            hierarchy.Direction.DOWN,
            ["src", "ymapsdf"],
        ),
        (
            hierarchy.Direction.BOTH,
            ["src", "ymapsdf", "renderer_denormalization", "renderer_publication"],
        ),
    ],
)
@pytest.mark.freeze_time(BUILD_TIME)
def test_hierarchy_builder_all_builds(direction, modules):
    db = mongomock.MongoClient(tz_aware=True).db
    db.build_statistics.insert_many([x.dict() for x in BUILDS_LIST])
    db.build_statistics.insert_many([x.dict() for x in OTHER_BUILDS])
    storage = build_statistics.BuildStatisticsStorage(db)

    builder = hierarchy.Builder(storage, "ymapsdf", 7438)

    actual = sorted(
        (b.copy(exclude={"mongo_id"}) for b in builder.get_hierarchy(direction)),
        key=lambda build: build.id
    )
    expected = sorted(
        (build.copy(exclude={"mongo_id"}) for build in BUILDS_LIST if build.name in modules),
        key=lambda build: build.id
    )
    assert actual == expected
