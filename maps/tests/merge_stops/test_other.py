import yatest.common

from maps.garden.modules.ymapsdf.lib.merge_stops import compute_ft_types


def test_compute_ft_types(monkeypatch):
    monkeypatch.setenv("GEOBASE_FORCE_CCTZ", "1")

    merged_masstransit_data_path = yatest.common.source_path(
        "maps/garden/modules/ymapsdf/tests/merge_stops/data/merged_masstransit_data_0.0.0-0")

    stop_id_to_ft_type = compute_ft_types.compute_stop_ft_types(merged_masstransit_data_path)

    assert stop_id_to_ft_type == {
        "s1": "transport-bus-stop",
        "s2": "transport-bus-stop",
        "s3": "transport-bus-stop",
        "s4": "transport-bus-stop",
        "s5": "transport-bus-stop",
        "s6": "transport-bus-stop",
        "s8": "transport-bus-stop",
        "s9": "transport-bus-stop",
        "s11": "transport-bus-stop",
        "station__MCC1": "transport-railway-station",
        "station__MCC2": "transport-railway-station",
    }
