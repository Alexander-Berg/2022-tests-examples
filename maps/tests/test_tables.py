import maps.bizdir.sps.db.tables as db

from yandex.maps.proto.bizdir.sps.hypothesis_pb2 import HypothesisStatus


def test_hypothesis_status_has__for_pb_enum_value_except_unknown__returns_true() -> None:
    for status in HypothesisStatus.DESCRIPTOR.values_by_name:
        if status == "UNKNOWN":
            continue
        assert db.HypothesisStatus.has(status.lower())
