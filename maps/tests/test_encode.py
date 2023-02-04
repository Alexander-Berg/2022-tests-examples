import maps.analyzer.modules.matcher.matcher.tools.debug_matcher.lib.encode_signal_sequence as encode
import maps.doc.proto.analyzer.gpssignal_pb2 as gps


SIGNALS = (
    (0, 1, 1),
    (2, 3, 7),
    (11, 23, 43),
    (45, 21, 58)
)


def test_encode_from_string():
    signals_string = ""
    for s in SIGNALS:
        signals_string += f'{s[0]}, {s[1]}, {s[2]}\n'
    signal_sequence = encode.from_string(signals_string)
    assert len(signal_sequence.signals) == len(SIGNALS)
    for actual, expected in zip(signal_sequence.signals, SIGNALS):
        assert actual.lon == expected[0]
        assert actual.lat == expected[1]
        assert actual.timestamp == expected[2]


def test_encode_from_gps_signal_collection_proto():
    signals_collection = gps.GpsSignalCollectionData()
    for s in SIGNALS:
        new_signal = signals_collection.signals.add()
        new_signal.lon = s[0]
        new_signal.lat = s[1]
        new_signal.time = s[2]
    signal_sequence = encode.from_gps_signal_collection_proto(
        signals_collection.SerializeToString()
    )
    assert len(signal_sequence.signals) == len(SIGNALS)
    for actual, expected in zip(signal_sequence.signals, SIGNALS):
        assert actual.lon == expected[0]
        assert actual.lat == expected[1]
        assert actual.timestamp == expected[2]


test_encode_from_string()
test_encode_from_gps_signal_collection_proto()
