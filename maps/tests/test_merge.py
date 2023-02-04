import pytest

from maps.analyzer.pylibs.schema import merge_tables, merge_tables_schema, Optional, String, Double, Uint64, Any, optional, Dict, Uint16, Uint32
import maps.analyzer.pylibs.schema as s

from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables_contents


def col(nm, ty=Optional(String), sorted=False):
    return s.column(nm, ty, None, sort_order='ascending' if sorted else None)


def tbl(*cols):
    return s.table(list(cols), None)


CLID = col('clid', sorted=True)
UUID = col('uuid', sorted=True)
SPEED = col('speed', Double)
USPEED = col('speed', Uint64)
JAMS = col('jams', Double)
REASONS_V1 = col('reasons', Any)
REASONS_V3 = col('reasons', Optional(Dict(Uint16, Uint32)))
UUID_REQ = col('uuid', String, sorted=True)

USERS_TABLE = tbl(CLID, UUID)
SPEED_TABLE = tbl(CLID, UUID, SPEED)
USPEED_TABLE = tbl(CLID, UUID, USPEED)
JAMS_TABLE = tbl(CLID, UUID, JAMS)
JOINED_TABLE = tbl(CLID, UUID, optional(SPEED), optional(JAMS))

# for narrowing merging tests
SIGNALS_STATS_V1 = tbl(CLID, UUID, REASONS_V1)
SIGNALS_STATS_V3 = tbl(CLID, UUID, REASONS_V3)
SIGNALS_STATS_V3_REQ = tbl(CLID, UUID_REQ, REASONS_V3)


def test_merge_tables_schema():
    assert merge_tables_schema([SPEED_TABLE, JAMS_TABLE]) == JOINED_TABLE
    assert merge_tables_schema([SPEED_TABLE, JAMS_TABLE], shrink=True) == USERS_TABLE
    assert merge_tables_schema([USERS_TABLE, JAMS_TABLE], shrink=True) == USERS_TABLE
    with pytest.raises(ValueError):
        merge_tables_schema([USERS_TABLE, JAMS_TABLE], optionalize_types=False)  # noqa
    with pytest.raises(ValueError):
        merge_tables_schema([SPEED_TABLE, USPEED_TABLE], extend_types=False)  # noqa
    assert merge_tables_schema([USERS_TABLE, SPEED_TABLE], extend_types=False), "allow optionalize"

    # narrow any
    assert merge_tables_schema([SIGNALS_STATS_V1, SIGNALS_STATS_V3]) == SIGNALS_STATS_V1
    assert merge_tables_schema([SIGNALS_STATS_V1, SIGNALS_STATS_V3], narrow_any=True) == SIGNALS_STATS_V3
    # narrow opt
    assert merge_tables_schema([SIGNALS_STATS_V3, SIGNALS_STATS_V3_REQ]) == SIGNALS_STATS_V3
    assert merge_tables_schema([SIGNALS_STATS_V3, SIGNALS_STATS_V3_REQ], narrow_opt=True) == SIGNALS_STATS_V3_REQ
    # narrow any & opt
    assert merge_tables_schema([SIGNALS_STATS_V1, SIGNALS_STATS_V3, SIGNALS_STATS_V3_REQ]) == SIGNALS_STATS_V1
    assert merge_tables_schema([SIGNALS_STATS_V1, SIGNALS_STATS_V3, SIGNALS_STATS_V3_REQ], narrow_any=True) == SIGNALS_STATS_V3
    assert merge_tables_schema(
        [SIGNALS_STATS_V1, SIGNALS_STATS_V3, SIGNALS_STATS_V3_REQ], narrow_opt=True
    ) == tbl(CLID, UUID_REQ, REASONS_V1)  # only uuid made required
    assert merge_tables_schema(
        [SIGNALS_STATS_V1, SIGNALS_STATS_V3, SIGNALS_STATS_V3_REQ],
        narrow_any=True, narrow_opt=True,
    ) == SIGNALS_STATS_V3_REQ


def test_merge_tables(ytc):
    speeds = ytc.create_temp_table(schema=SPEED_TABLE.schema)
    ytc.write_table(speeds, [row(speed=20.0)])

    jams = ytc.create_temp_table(schema=JAMS_TABLE.schema)
    ytc.write_table(jams, [row(jams=10.0)])

    def check_result(result, rows):
        assert_equal_tables_contents(rows, list(ytc.read_table(result)), unordered=True)

    merged = merge_tables(ytc, [speeds, jams])
    check_result(merged, [
        row(speed=20.0, jams=None),
        row(speed=None, jams=10.0),
    ])

    merged = merge_tables(ytc, [speeds, jams], schema=USERS_TABLE)
    check_result(merged, [row(), row()])


def test_merge_from_output(ytc):
    LIST = s.column('list', s.List(s.String), None)
    src1 = ytc.create_temp_table(schema=s.table([LIST], None).schema)
    ytc.write_table(src1, [{'list': ['foo', 'bar']}])
    src2 = ytc.create_temp_table(schema=s.table([s.optional(LIST)], None).schema)
    ytc.write_table(src2, [{'list': None}])

    dst = merge_tables(ytc, [src1, src2])
    assert s.get_table(ytc, dst) == s.table([s.optional(LIST)], None)


def row(**fields):
    fields.update({'clid': 'navi', 'uuid': 'foobar'})
    return fields
