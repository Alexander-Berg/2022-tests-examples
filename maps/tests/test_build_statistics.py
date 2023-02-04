import datetime as dt
import pytz
import pytest

from maps.garden.libs_server.build import build_defs, build_statistics

NOW = dt.datetime(2016, 11, 23, 15, 25, 22, tzinfo=pytz.utc)


@pytest.mark.freeze_time(NOW)
def test_build_statistics_record():
    build = build_defs.Build(
        id=123,
        name="test_module",
        contour_name="test_contour",
        author="apollo",
        sources=[
            build_defs.Source(
                name="test_source",
                contour_name="test_contour",
                version=build_defs.SourceVersion(build_id=111),
            ),
        ],
        extras={
            "release_name": "20201011",
        },
        status=build_defs.BuildStatus(
            string=build_defs.BuildStatusString.SCHEDULING,
            updated_at=NOW,
        ),
        module_version="version123",
        request_id=123,
    )

    statistics_record = build_statistics.BuildStatisticsRecord.from_build(
        build, prev_build_statistics=None)

    assert statistics_record.id == build.id
    assert statistics_record.name == build.name
    assert statistics_record.contour_name == build.contour_name
    assert statistics_record.sources == build.sources
    assert statistics_record.properties == build.extras
    assert statistics_record.module_version == build.module_version
    assert statistics_record.build_status_logs == [
        build_statistics.BuildStatusLog(
            status=build.status,
            request_id=build.request_id,
            author=build.author,
            module_version=build.module_version
        )
    ]
    assert statistics_record.started_at == NOW
    assert statistics_record.finished_at is None

    restored_build = statistics_record.to_build()
    assert restored_build.id == build.id
    assert restored_build.name == build.name
    assert restored_build.contour_name == build.contour_name
    assert restored_build.sources == build.sources
    assert restored_build.extras == build.extras
    assert restored_build.status.string is None

    # Update build

    start_time = dt.datetime(2021, 10, 10, 12, 34, tzinfo=pytz.utc)

    build.status = build_defs.BuildStatus(
        string=build_defs.BuildStatusString.SCHEDULING,
        start_time=start_time,
        updated_at=start_time,
    )
    statistics_record = build_statistics.BuildStatisticsRecord.from_build(
        build, prev_build_statistics=statistics_record)

    assert statistics_record.build_status_logs == [
        build_statistics.BuildStatusLog(
            status=build.status,
            request_id=build.request_id,
            author=build.author,
            module_version=build.module_version
        )
    ]
    assert statistics_record.started_at == NOW
    assert statistics_record.finished_at is None

    # Restart build

    finish_time = dt.datetime(2021, 11, 10, 12, 34, tzinfo=pytz.utc)

    build.request_id = 456
    build.status = build_defs.BuildStatus(
        string=build_defs.BuildStatusString.COMPLETED,
        finish_time=finish_time,
        updated_at=finish_time,
    )
    statistics_record = build_statistics.BuildStatisticsRecord.from_build(
        build, prev_build_statistics=statistics_record)

    assert statistics_record.build_status_logs == [
        build_statistics.BuildStatusLog(
            status=build_defs.BuildStatus(
                string=build_defs.BuildStatusString.SCHEDULING,
                start_time=start_time,
                updated_at=start_time,
            ),
            request_id=123,
            author=build.author,
            module_version=build.module_version
        ),
        build_statistics.BuildStatusLog(
            status=build_defs.BuildStatus(
                string=build_defs.BuildStatusString.COMPLETED,
                finish_time=finish_time,
                updated_at=finish_time,
            ),
            request_id=456,
            author=build.author,
            module_version=build.module_version
        ),
    ]
    assert statistics_record.started_at == NOW
    assert statistics_record.finished_at == finish_time


def test_failed_build_on_scheduling():
    build = build_defs.Build(
        id=123,
        name="test_module",
        contour_name="test_contour",
        author="apollo",
        sources=[
            build_defs.Source(
                name="test_source",
                contour_name="test_contour",
                version=build_defs.SourceVersion(build_id=111),
            ),
        ],
        extras={
            "release_name": "20201011",
        },
        status=build_defs.BuildStatus.create_failed("propagate properties failed"),
        module_version="version123",
        startrek_issue_key="TEST-123",
        request_id=None,  # build failed before getting a request id
    )

    statistics_record = build_statistics.BuildStatisticsRecord.from_build(
        build, prev_build_statistics=None)

    restored_build = statistics_record.to_build()
    assert restored_build.id == build.id
    assert restored_build.name == build.name
    assert restored_build.contour_name == build.contour_name
    assert restored_build.sources == build.sources
    assert restored_build.extras == build.extras
    assert restored_build.status.string is None
    assert restored_build.startrek_issue_key == build.startrek_issue_key
