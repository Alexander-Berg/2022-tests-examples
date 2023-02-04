import time
import pytest

from freezegun import freeze_time

from infra.rtc_sla_tentacles.backend.lib.tentacle_agent.tentacle_state import TentacleState, TimestampFileReadResult


def remove_empty_buckets_from_histograms(histogram):
    histogram_with_full_buckets_only = list()
    for bucket in histogram:
        if bucket[1] == 1:
            histogram_with_full_buckets_only.append(bucket)
    return histogram_with_full_buckets_only


@freeze_time("2020-01-01 00:00:00")
def offset_seconds(offset):
    return int(time.time()) - offset


@pytest.mark.parametrize(
    "settings,assertions",
    [
        (
            {
                "juggler_crit_age": 600,
                "timestamp_files_settings": {
                    "resource_file_path": TimestampFileReadResult(ts=offset_seconds(500)),
                    "prepare_finish_time_file_path": TimestampFileReadResult(ts=offset_seconds(200)),
                    "self_shutdown_time_file_path": TimestampFileReadResult(ts=offset_seconds(5))
                }
            },
            {
                "juggler_timestamp_age_monitoring_response": ("Ok", 200),
                "resource_delivery_duration_histogram_full_buckets": [[300, 1]],
                "configuration_switch_duration_histogram_full_buckets": [[480, 1]],
                "activation_duration_histogram_full_buckets": [[5, 1]]
            }
        ),

        (
            {
                "juggler_crit_age": 600,
                "timestamp_files_settings": {
                    "resource_file_path": TimestampFileReadResult(ts=offset_seconds(700)),
                    "prepare_finish_time_file_path": TimestampFileReadResult(ts=offset_seconds(180)),
                    "self_shutdown_time_file_path": TimestampFileReadResult(ts=offset_seconds(100))
                }
            },
            {
                "juggler_timestamp_age_monitoring_response": ("Timestamp too old", 418),
                "resource_delivery_duration_histogram_full_buckets": [[510, 1]],
                "configuration_switch_duration_histogram_full_buckets": [[690, 1]],
                "activation_duration_histogram_full_buckets": [[90, 1]]
            }
        ),

        (
            {
                "juggler_crit_age": 600,
                "timestamp_files_settings": {
                    "resource_file_path": TimestampFileReadResult(errmsg="ERROR: Timestamp file read error"),
                    "prepare_finish_time_file_path": TimestampFileReadResult(
                        errmsg="ERROR: Timestamp file read error"),
                    "self_shutdown_time_file_path": TimestampFileReadResult(
                        errmsg="ERROR: Timestamp file read error")
                }
            },
            {
                "juggler_timestamp_age_monitoring_response": ("ERROR: Timestamp file read error", 418),
                "resource_delivery_duration_histogram_full_buckets": [],
                "configuration_switch_duration_histogram_full_buckets": [],
                "activation_duration_histogram_full_buckets": []
            }
        )
    ]
)
def test_tentacle_state(monkeypatch, settings, assertions):
    freezer = freeze_time("2020-01-01 00:00:00")
    freezer.start()

    juggler_settings = {
        "crit_age": settings["juggler_crit_age"],
        "ok_code": 200,
        "crit_code": 418
    }

    def mock_read_ts_file(_, read_result: TimestampFileReadResult) -> TimestampFileReadResult:
        return read_result
    monkeypatch.setattr(TentacleState, "_read_ts_file", mock_read_ts_file)

    tentacle_state = TentacleState(binary_start_ts=int(time.time()),
                                   timestamp_files_settings=settings["timestamp_files_settings"],
                                   juggler_settings=juggler_settings)

    assert tentacle_state.get_juggler_timestamp_age_monitoring() == \
        assertions["juggler_timestamp_age_monitoring_response"]

    assert remove_empty_buckets_from_histograms(tentacle_state.resource_delivery_duration_histogram) == \
        assertions["resource_delivery_duration_histogram_full_buckets"]

    assert remove_empty_buckets_from_histograms(tentacle_state.configuration_switch_duration_histogram) == \
        assertions["configuration_switch_duration_histogram_full_buckets"]

    assert remove_empty_buckets_from_histograms(tentacle_state.activation_duration_histogram) == \
        assertions["activation_duration_histogram_full_buckets"]

    freezer.stop()
