from sys import version_info

from yt.wrapper import ypath_join

from maps.analyzer.pylibs.schema import Uint64, column, table, String, Utf8, Optional, Dict, Text, Bytes
from maps.analyzer.pylibs.schema.operations import (
    schematize, default_errors_filter, read_table, write_table,
)
from maps.analyzer.pylibs.schema.column import COLUMN_SCHEMA_TRAITS
from maps.analyzer.pylibs.schema.table import get_table
from maps.analyzer.pylibs.schema.utils import ERROR, ERROR_DELIMITER

from mapreduce.yt.python.table_schema import extract_column_attributes

GOOD = {'key': 'Ivan', 'value': 42}
BAD = {'key': 'Bolvan', 'value': -42, 'pomidor': 'ogurets'}
DATA = [GOOD, BAD]


# `YtContext.read_table` will convert anything to `str`, which have different meaning in py2 and py3
# If we use String, py3 will fail on comparing to `str` (need encoding to convert to bytes)
# If we use Utf8, py2 will return False on comparing YsonUnicode with `str`
STR_TYPE = Utf8 if version_info.major >= 3 else String


def run_schematize_check(ytc, src, tbl, error_filter, log_errors):
    result = schematize(ytc, src, tbl, False, error_filter, log_errors)
    good_tbl = result[0] if log_errors else result

    correct_schema = tbl.schema
    actual_schema = ytc.get(ypath_join(good_tbl, '@schema'))

    for attr in correct_schema.attributes:
        assert correct_schema.attributes[attr] == actual_schema.attributes[attr]

    assert list(correct_schema) == extract_column_attributes(list(actual_schema), attributes=COLUMN_SCHEMA_TRAITS)

    good_data = [tbl.cast(x)[0] for x in DATA if error_filter(tbl.cast(x)[1])]
    actual_data = list(ytc.read_table(good_tbl))

    for x in good_data:
        assert x in actual_data
    if not log_errors:
        return
    bad_tbl = result[1]
    error = next(ytc.read_table(bad_tbl))[ERROR.name]
    assert 'Row: ' in error
    assert error.count(ERROR_DELIMITER) == 2


def test_schematize(ytc):
    tbl = table([column('key', Optional(STR_TYPE), None), column('value', Optional(Uint64), None)], None)

    src = ytc.create_temp_table()
    ytc.write_table(src, DATA)
    args = zip([default_errors_filter, lambda x: True], (False, True))
    for error_filter, log_errors in args:
        run_schematize_check(ytc, src, tbl, error_filter, log_errors)


def test_preserve_schema(ytc):
    tbl = table([column('key', Optional(STR_TYPE), None), column('value', Optional(Uint64), None)], None)

    src = ytc.create_temp_table(attributes={'schema': tbl.schema})
    yt_schema = get_table(ytc, src)
    assert yt_schema == tbl


def test_read_write_table(ytc):
    tbl = table([column('dict', Dict(Text, Optional(Bytes)), None), column('comment', Text, None), column('data', Bytes, None)], None)
    t = ytc.create_temp_table(attributes={'schema': tbl.schema})

    ROWS = [
        {'dict': {'foo': b'haha', 'bar': None}, 'comment': 'some comment', 'data': b'bytes'},
        {'dict': {'foo': None, 'bar': b'test'}, 'comment': 'some other comment', 'data': b'binary data'},
    ]

    write_table(ytc, t, ROWS)
    rows = list(read_table(ytc, t))

    assert rows == ROWS
