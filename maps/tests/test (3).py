from maps.wikimap.feedback.pushes.addresses.prepare_uuids.lib.prepare_uuids import (
    prepare_uuids,
)
from maps.wikimap.feedback.pushes.helpers import helpers
from nile.api.v1 import (
    clusters,
    local,
    Record,
)
import pytest


@pytest.fixture
def job():
    cluster = clusters.MockYQLCluster()
    job = cluster.job()
    prepare_uuids(job, "2020-03-03", "2020-03-04", app_alias="maps")
    return job


def test_filter_metrika_log(job):
    filtered = []
    job.local_run(
        sources={
            "metrika_log_maps": local.StreamSource([
                Record(
                    APIKey=1,  # will be filtered out
                    AppPlatform=b"iOS",
                    AppVersionName=b"1",
                    DeviceID=b"1",
                    EventName=b"application.start-session",
                    Locale=b"ru_RU",
                    UUID=b"1",
                ),
                Record(
                    APIKey=4,
                    AppPlatform=b"winPhone",  # will be filtered out
                    AppVersionName=b"1",
                    DeviceID=b"1",
                    EventName=b"application.start-session",
                    Locale=b"ru_RU",
                    UUID=b"1",
                ),
                Record(
                    APIKey=4,
                    AppPlatform=b"iOS",
                    AppVersionName=b"1",
                    DeviceID=b"1",
                    EventName=b"application.start-session",
                    Locale=b"en_GB",  # will be filtered out
                    UUID=b"1",
                ),
                Record(
                    APIKey=4,
                    AppPlatform=b"iOS",
                    AppVersionName=b"1",
                    DeviceID=b"1",
                    EventName=b"event",  # will be filtered out
                    Locale=b"ru_RU",
                    UUID=b"1",
                ),
                Record(
                    APIKey=4,
                    AppPlatform=b"iOS",
                    AppVersionName=b"2",
                    DeviceID=b"2",
                    EventName=b"application.start-session",
                    Locale=b"ru_RU",
                    UUID=b"2",
                ),
                Record(
                    APIKey=4,
                    AppPlatform=b"iOS",
                    AppVersionName=b"3",
                    DeviceID=b"2",
                    EventName=b"application.start-session",
                    Locale=b"ru_RU",
                    UUID=b"2",
                ),
                Record(
                    APIKey=30488,
                    AppPlatform=b"android",
                    AppVersionName=b"3",
                    DeviceID=b"3",
                    EventName=b"application.start-session",
                    Locale=b"en_RU",
                    UUID=b"3",
                ),
            ]),
        },
        sinks={
            "filtered_maps": local.ListSink(filtered),
        }
    )

    helpers.compare_records_lists(
        sorted(filtered),
        sorted([
            Record(
                APIKey=4,
                AppPlatform=b"iOS",
                AppVersionName=b"2",
                DeviceID=b"2",
                EventName=b"application.start-session",
                Locale=b"ru_RU",
                UUID=b"2",
            ),
            Record(
                APIKey=4,
                AppPlatform=b"iOS",
                AppVersionName=b"3",
                DeviceID=b"2",
                EventName=b"application.start-session",
                Locale=b"ru_RU",
                UUID=b"2",
            ),
        ])
    )


def test_unique_uuids(job):
    unique = []
    job.local_run(
        sources={
            "filtered_maps": local.StreamSource([
                Record(
                    APIKey=4,
                    AppPlatform=b"iOS",
                    AppVersionName=b"2",
                    DeviceID=b"2",
                    EventName=b"application.start-session",
                    Locale=b"ru_RU",
                    UUID=b"2",
                ),
                Record(
                    APIKey=4,
                    AppPlatform=b"iOS",
                    AppVersionName=b"3",  # unique with previous record
                    DeviceID=b"2",
                    EventName=b"application.start-session",
                    Locale=b"ru_RU",
                    UUID=b"2",
                ),
                Record(
                    APIKey=30488,
                    AppPlatform=b"android",
                    AppVersionName=b"3",
                    DeviceID=b"3",
                    EventName=b"application.start-session",
                    Locale=b"en_RU",
                    UUID=b"3",
                ),
                Record(
                    APIKey=30488,
                    AppPlatform=b"iOS",
                    AppVersionName=b"4",
                    DeviceID=b"4",
                    EventName=b"application.start-session",
                    Locale=b"ru_RU",
                    UUID=b"3",
                ),
                Record(
                    APIKey=30488,
                    AppPlatform=b"iOS",
                    AppVersionName=b"4",
                    DeviceID=b"4",
                    EventName=b"application.start-session",
                    Locale=b"ru_BY",
                    UUID=b"4",
                ),
            ]),
        },
        sinks={
            "unique_maps": local.ListSink(unique),
        }
    )

    helpers.compare_records_lists(
        sorted(unique),
        sorted([
            Record(
                APIKey=4,
                AppPlatform=b"iOS",
                AppVersionName=b"2",
                DeviceID=b"2",
                EventName=b"application.start-session",
                Locale=b"ru_RU",
                UUID=b"2",
            ),
            Record(
                APIKey=30488,
                AppPlatform=b"android",
                AppVersionName=b"3",
                DeviceID=b"3",
                EventName=b"application.start-session",
                Locale=b"en_RU",
                UUID=b"3",
            ),
            Record(
                APIKey=30488,
                AppPlatform=b"iOS",
                AppVersionName=b"4",
                DeviceID=b"4",
                EventName=b"application.start-session",
                Locale=b"ru_RU",
                UUID=b"3",
            ),
        ])
    )


def test_project_uuids(job):
    result = []
    job.local_run(
        sources={
            "unique_maps": local.StreamSource([
                Record(
                    AppPlatform=b"android",
                    AppVersionName=b"2",
                    DeviceID=b"1",
                    EventName=b"application.start-session",
                    Locale=b"ru_RU",
                    UUID=b"1",
                ),
                Record(
                    AppPlatform=b"iOS",
                    AppVersionName=b"2",
                    DeviceID=b"2",
                    EventName=b"application.start-session",
                    Locale=b"ru_RU",
                    UUID=b"2",
                ),
                Record(
                    AppPlatform=b"android",
                    AppVersionName=b"3",
                    DeviceID=b"3",
                    EventName=b"application.start-session",
                    Locale=b"en_RU",
                    UUID=b"3",
                ),
            ]),
        },
        sinks={
            "output_maps": local.ListSink(result),
        }
    )

    helpers.compare_records_lists(
        sorted(result),
        sorted([
            Record(
                application=b"ru.yandex.yandexmaps",
                device_id=b"1",
                uuid=b"1",
            ),
            Record(
                application=b"ru.yandex.traffic",
                device_id=b"2",
                uuid=b"2",
            ),
            Record(
                application=b"ru.yandex.yandexmaps",
                device_id=b"3",
                uuid=b"3",
            ),
        ])
    )
