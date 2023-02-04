# encoding: utf-8

from cppzoom import ZSomethingFormat, ZRecord


def test_something_format(tmpdir):
    data_file = str(tmpdir.join("test.data"))

    writer = ZSomethingFormat(None, data_file, "ab")
    writer.write_record(5, "itype=test||ctype,geo,prj,tier", ZRecord.from_dict({"test_signal": 1}))
    buf = writer.dump()

    reader = ZSomethingFormat(buf, data_file, "rb")
    result = reader.read_records([5], [("itype=test||ctype,geo,prj,tier", ["test_signal"])])
    assert len(result) == 1

    timestamp, requested_index, record = result[0]
    assert timestamp == 5
    assert requested_index == 0
    assert record.get_values() == {'test_signal': 1}

    assert [
        (ts, key, records.get_values())
        for ts, key, records in reader.iterate_records([5])
    ] == [(5, "itype=test||ctype,geo,prj,tier", {"test_signal": 1})]
    assert list(reader.iterate_keys()) == [("itype=test||ctype,geo,prj,tier", ["test_signal"])]

    assert reader.has_records([0, 5], ["itype=test||ctype,geo,prj,tier"]) == [False, True]

    assert reader.first_record_time() == 5
    assert reader.last_record_time() == 5
