
import json

import arrow
import pytest

from yt.wrapper import (
    ypath_join,
    common as yt_common,
)

from billing.log_tariffication.py.jobs.core_acts.process_correction_commands import Job
from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
    RUN_ID_KEY,
    LOG_INTERVAL_KEY,
    ACTED_SLICE_KEY,
    DAILY_ACTED_SLICE_KEY,
    ACT_DT_KEY,
    PREV_COMMANDS_TABLE,
    LAST_COMMANDS_TABLE,
)
from billing.log_tariffication.py.lib import utils
from billing.library.python.logmeta_utils.meta import generate_run_id
from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)

from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
    create_yt_client,
)


"""
SOURCE:
tariffed:
1  4  5  8  9 11 12   17 18 21 22  26 27 30
|--|  |--|  |-|  |----|  |--|  |---|  |--|
interim:
1        8  9         17 18        26
|---  ---|  |--  -----|  |---  ----|
acts:
1        8
|---  ---|
daily_acts:
1        8  9         17
|---  ---|  |--  -----|
monthly_acted_events:
1        8
|---  ---|
daily_acted_events:
1        8  9         17
|---  ---|  |--  -----|
commands:
                                      27 30
                                      |==|

DESTINATION:
tariffed:
1  4  5  8  9 11 12   17 18 21 22  26 27 30
|--|  |--|  |-|  |----|  |--|  |---|  |--|
interim:
1        8  9         17 18        26 27 30
|---  ---|  |--  -----|  |---  ----|  |==|
acts:
1        8  9                            30
|---  ---|  |============================|
daily_acts:
1        8  9         17 18              30
|---  ---|  |--  -----|  |===============|
monthly_acted_events (external job):
1        8  9                            30
|---  ---|  |============================|
daily_acted_events (external job):
1        8  9         17 18              30
|---  ---|  |--  -----|  |===============|
commands:
                                      27 30
                                      |==|
"""

INTERVAL1 = LogInterval([
    Subinterval('c0', 't0', 0, 1, 5),
])
INTERVAL2 = LogInterval([
    Subinterval('c0', 't0', 0, 5, 9),
])
INTERVAL3 = LogInterval([
    Subinterval('c0', 't0', 0, 9, 12),
])
INTERVAL4 = LogInterval([
    Subinterval('c0', 't0', 0, 12, 18),
])
INTERVAL5 = LogInterval([
    Subinterval('c0', 't0', 0, 18, 22),
])
INTERVAL6 = LogInterval([
    Subinterval('c0', 't0', 0, 22, 27),
])
INTERVAL7 = LogInterval([
    Subinterval('c0', 't0', 0, 27, 31),
])

ACT_DT = arrow.Arrow(2021, 12, 31)
DAILY_ACT_DT = arrow.Arrow(2022, 1, 1)
COMMANDS_DT = arrow.Arrow(2022, 1, 2)

TARIFFED_META1 = {
    RUN_ID_KEY: generate_run_id(ACT_DT.shift(hours=0)),
    LOG_INTERVAL_KEY: INTERVAL1.to_meta(),
}

TARIFFED_META2 = {
    RUN_ID_KEY: generate_run_id(ACT_DT.shift(hours=3)),
    LOG_INTERVAL_KEY: INTERVAL2.to_meta(),
}

TARIFFED_META3 = {
    RUN_ID_KEY: generate_run_id(ACT_DT.shift(hours=6)),
    LOG_INTERVAL_KEY: INTERVAL3.to_meta(),
}

TARIFFED_META4 = {
    RUN_ID_KEY: generate_run_id(ACT_DT.shift(hours=9)),
    LOG_INTERVAL_KEY: INTERVAL4.to_meta(),
}

TARIFFED_META5 = {
    RUN_ID_KEY: generate_run_id(ACT_DT.shift(hours=12)),
    LOG_INTERVAL_KEY: INTERVAL5.to_meta(),
}

TARIFFED_META6 = {
    RUN_ID_KEY: generate_run_id(ACT_DT.shift(hours=15)),
    LOG_INTERVAL_KEY: INTERVAL6.to_meta(),
}

TARIFFED_META7 = {
    RUN_ID_KEY: generate_run_id(ACT_DT.shift(hours=18)),
    LOG_INTERVAL_KEY: INTERVAL7.to_meta(),
}

ACTS_META1 = {
    RUN_ID_KEY: generate_run_id(ACT_DT),
    LOG_INTERVAL_KEY: LogInterval.from_slices(INTERVAL1.beginning, INTERVAL2.end).to_meta(),
    ACT_DT_KEY: ACT_DT.strftime('%Y-%m-%d'),
}

DAILY_ACTS_META1 = {
    RUN_ID_KEY: generate_run_id(ACT_DT),
    LOG_INTERVAL_KEY: LogInterval.from_slices(INTERVAL1.beginning, INTERVAL2.end).to_meta(),
    ACT_DT_KEY: ACT_DT.strftime('%Y-%m-%d'),
}

DAILY_ACTS_META2 = {
    RUN_ID_KEY: generate_run_id(DAILY_ACT_DT),
    LOG_INTERVAL_KEY: LogInterval.from_slices(INTERVAL3.beginning, INTERVAL4.end).to_meta(),
    ACT_DT_KEY: DAILY_ACT_DT.strftime('%Y-%m-%d'),
}

INTERIM_META1 = {
    RUN_ID_KEY: generate_run_id(ACT_DT.shift(hours=0)),
    LOG_INTERVAL_KEY: LogInterval.from_slices(INTERVAL1.beginning, INTERVAL2.end).to_meta(),
}

INTERIM_META2 = {
    RUN_ID_KEY: generate_run_id(ACT_DT.shift(hours=3)),
    LOG_INTERVAL_KEY: LogInterval.from_slices(INTERVAL3.beginning, INTERVAL4.end).to_meta(),
}

INTERIM_META3 = {
    RUN_ID_KEY: generate_run_id(ACT_DT.shift(hours=6)),
    LOG_INTERVAL_KEY: LogInterval.from_slices(INTERVAL5.beginning, INTERVAL6.end).to_meta(),
}

COMMANDS_META1 = {
    RUN_ID_KEY: generate_run_id(COMMANDS_DT),
    LOG_INTERVAL_KEY: INTERVAL7.to_meta(),
    ACTED_SLICE_KEY: LogInterval.from_meta(ACTS_META1[LOG_INTERVAL_KEY]).end.to_meta(),
    DAILY_ACTED_SLICE_KEY: LogInterval.from_meta(DAILY_ACTS_META2[LOG_INTERVAL_KEY]).end.to_meta(),
}

COMMANDS_META1_WIDE = {
    RUN_ID_KEY: generate_run_id(COMMANDS_DT),
    LOG_INTERVAL_KEY: LogInterval.from_slices(INTERVAL3.beginning, INTERVAL7.end).to_meta(),
    ACTED_SLICE_KEY: LogInterval.from_meta(ACTS_META1[LOG_INTERVAL_KEY]).end.to_meta(),
    DAILY_ACTED_SLICE_KEY: LogInterval.from_meta(DAILY_ACTS_META2[LOG_INTERVAL_KEY]).end.to_meta(),
}

COMMANDS_TABLE_SCHEMA = [
    {'name': 'id', 'type': 'uint64'},
    {'name': 'command', 'type': 'string'},
    {'name': 'params', 'type': 'string'},
]
TARIFFED_EVENTS_TABLE_SCHEMA = [
    {'name': 'UID', 'type': 'string'},
]
ACT_HEADERS_TABLE_SCHEMA = [
    {'name': 'act_id', 'type': 'string', 'required': True},
    {'name': 'act_dt', 'type': 'uint32'},
    {'name': 'act_sum', 'type': 'double'},
    {'name': 'hidden', 'type': 'boolean'},
    {'name': 'ticket_id', 'type': 'string'},
]
DAILY_ACT_HEADERS_TABLE_SCHEMA = ACT_HEADERS_TABLE_SCHEMA
ACT_ROWS_TABLE_SCHEMA = [
    {'name': 'UID', 'type': 'string'},
    {'name': 'act_id', 'type': 'string', 'required': True},
    {'name': 'acted_qty', 'type': 'double'},
    {'name': 'acted_sum', 'type': 'double'},
]
DAILY_ACT_ROWS_TABLE_SCHEMA = ACT_ROWS_TABLE_SCHEMA
EXTERNAL_ACT_ROWS_TABLE_SCHEMA = ACT_ROWS_TABLE_SCHEMA
INTERIM_ACT_ROWS_TABLE_SCHEMA = [
    {'name': 'UID', 'type': 'string'},
    {'name': 'acted_qty', 'type': 'double'},
    {'name': 'acted_sum', 'type': 'double'},
]
EXTERNAL_INTERIM_ACT_ROWS_TABLE_SCHEMA = INTERIM_ACT_ROWS_TABLE_SCHEMA
ACTED_EVENTS_TABLE_SCHEMA = [
    {'name': 'UID', 'type': 'string'},
    {'name': 'row_UID', 'type': 'string'},
    {'name': 'acted_qty', 'type': 'double'},
    {'name': 'acted_sum', 'type': 'double'},
]
EXTERNAL_ACTED_EVENTS_TABLE_SCHEMA = ACTED_EVENTS_TABLE_SCHEMA
MONTHLY_ACTED_EVENTS_TABLE_SCHEMA = ACTED_EVENTS_TABLE_SCHEMA + [{'name': 'act_id', 'type': 'string', 'required': True}]
DAILY_ACTED_EVENTS_TABLE_SCHEMA = MONTHLY_ACTED_EVENTS_TABLE_SCHEMA
UNPROCESSED_EVENTS_STRIPPED_TABLE_SCHEMA = [
    {'name': 'UID', 'type': 'string'},
]
UNPROCESSED_EVENTS_FULL_TABLE_SCHEMA = UNPROCESSED_EVENTS_STRIPPED_TABLE_SCHEMA
UNPROCESSED_INTERIM_ROWS_TABLE_SCHEMA = INTERIM_ACT_ROWS_TABLE_SCHEMA
EXTERNAL_UNPROCESSED_EVENTS_FULL_TABLE_SCHEMA = UNPROCESSED_EVENTS_FULL_TABLE_SCHEMA + [{'name': 'command_id', 'type': 'int32', 'required': True}]


@pytest.fixture(name='tariffed_events_stripped_dir')
def tariffed_events_stripped_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'tariffed_events_stripped')


@pytest.fixture(name='commands_dir')
def commands_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'commands')


@pytest.fixture(name='tariffed_events_dir')
def tariffed_events_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'tariffed_events')


@pytest.fixture(name='external_act_headers_dir')
def external_act_headers_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'external_act_headers')


@pytest.fixture(name='external_act_rows_dir')
def external_act_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'external_act_rows')


@pytest.fixture(name='external_interim_act_rows_dir')
def external_interim_act_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'external_interim_act_rows')


@pytest.fixture(name='external_acted_events_dir')
def external_acted_events_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'external_acted_events')


@pytest.fixture(name='external_unprocessed_events_full_dir')
def external_unprocessed_events_full_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'external_unprocessed_events_full')


@pytest.fixture(name='act_headers_dir')
def act_headers_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'act_headers')


@pytest.fixture(name='daily_act_headers_dir')
def daily_act_headers_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'daily_act_headers')


@pytest.fixture(name='act_rows_dir')
def act_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'act_rows')


@pytest.fixture(name='daily_act_rows_dir')
def daily_act_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'daily_act_rows')


@pytest.fixture(name='interim_act_rows_dir')
def interim_act_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'interim_act_rows')


@pytest.fixture(name='acted_events_dir')
def acted_events_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'acted_events')


@pytest.fixture(name='monthly_acted_events_dir')
def monthly_acted_events_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'monthly_acted_events')


@pytest.fixture(name='daily_acted_events_dir')
def daily_acted_events_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'daily_acted_events')


@pytest.fixture(name='unprocessed_events_full_dir')
def unprocessed_events_full_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'unprocessed_events_full')


@pytest.fixture(name='unprocessed_events_stripped_dir')
def unprocessed_events_stripped_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'unprocessed_events_stripped')


@pytest.fixture(name='unprocessed_interim_rows_dir')
def unprocessed_interim_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'unprocessed_interim_rows')


@pytest.fixture(name='res_act_headers_dir')
def res_act_headers_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_act_headers')


@pytest.fixture(name='res_daily_act_headers_dir')
def res_daily_act_headers_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_daily_act_headers')


@pytest.fixture(name='res_act_rows_dir')
def res_act_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_act_rows')


@pytest.fixture(name='res_daily_act_rows_dir')
def res_daily_act_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_daily_act_rows')


@pytest.fixture(name='res_interim_act_rows_dir')
def res_interim_act_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_interim_act_rows')


@pytest.fixture(name='res_acted_events_dir')
def res_acted_events_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_acted_events')


@pytest.fixture(name='res_unprocessed_events_full_dir')
def res_unprocessed_events_full_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_unprocessed_events_full')


@pytest.fixture(name='res_unprocessed_events_stripped_dir')
def res_unprocessed_events_stripped_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_unprocessed_events_stripped')


@pytest.fixture(name='res_unprocessed_interim_rows_dir')
def res_unprocessed_interim_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_unprocessed_interim_rows')


def create_table(
    yt_client, folder, table, attributes=None, schema=None, rows=None, meta=None
):
    path = ypath_join(folder, table)
    attributes = attributes or {}
    if schema:
        attributes['schema'] = schema
    yt_client.create('table', path, attributes=attributes)

    if rows:
        yt_client.write_table(path, rows or [])

    if meta:
        utils.meta.set_log_tariff_meta(yt_client, path, meta)

    return path


def prepare_internal_tables(
    yt_client=None,
    tariffed_events_dir=None,
    act_headers_dir=None,
    daily_act_headers_dir=None,
    act_rows_dir=None,
    daily_act_rows_dir=None,
    interim_act_rows_dir=None,
    acted_events_dir=None,
    monthly_acted_events_dir=None,
    daily_acted_events_dir=None,
    unprocessed_events_full_dir=None,
    unprocessed_events_stripped_dir=None,
    unprocessed_interim_rows_dir=None,
    tariffed_events1=None,
    tariffed_events2=None,
    tariffed_events3=None,
    tariffed_events4=None,
    tariffed_events5=None,
    tariffed_events6=None,
    tariffed_events7=None,
    act_headers1=None,
    daily_act_headers1=None,
    daily_act_headers2=None,
    act_rows1=None,
    daily_act_rows1=None,
    daily_act_rows2=None,
    interim_act_rows1=None,
    interim_act_rows2=None,
    interim_act_rows3=None,
    acted_events1=None,
    acted_events2=None,
    acted_events3=None,
    monthly_acted_events1=None,
    daily_acted_events1=None,
    daily_acted_events2=None,
    unprocessed_events_full=None,
    unprocessed_events_stripped=None,
    unprocessed_interim_rows=None,
    **kwargs,
):
    create_table(
        yt_client, tariffed_events_dir, TARIFFED_META1[RUN_ID_KEY],
        schema=TARIFFED_EVENTS_TABLE_SCHEMA, meta=TARIFFED_META1, rows=tariffed_events1,
    )
    create_table(
        yt_client, tariffed_events_dir, TARIFFED_META2[RUN_ID_KEY],
        schema=TARIFFED_EVENTS_TABLE_SCHEMA, meta=TARIFFED_META2, rows=tariffed_events2,
    )
    create_table(
        yt_client, tariffed_events_dir, TARIFFED_META3[RUN_ID_KEY],
        schema=TARIFFED_EVENTS_TABLE_SCHEMA, meta=TARIFFED_META3, rows=tariffed_events3,
    )
    create_table(
        yt_client, tariffed_events_dir, TARIFFED_META4[RUN_ID_KEY],
        schema=TARIFFED_EVENTS_TABLE_SCHEMA, meta=TARIFFED_META4, rows=tariffed_events4,
    )
    create_table(
        yt_client, tariffed_events_dir, TARIFFED_META5[RUN_ID_KEY],
        schema=TARIFFED_EVENTS_TABLE_SCHEMA, meta=TARIFFED_META5, rows=tariffed_events5,
    )
    create_table(
        yt_client, tariffed_events_dir, TARIFFED_META6[RUN_ID_KEY],
        schema=TARIFFED_EVENTS_TABLE_SCHEMA, meta=TARIFFED_META6, rows=tariffed_events6,
    )
    create_table(
        yt_client, tariffed_events_dir, TARIFFED_META7[RUN_ID_KEY],
        schema=TARIFFED_EVENTS_TABLE_SCHEMA, meta=TARIFFED_META7, rows=tariffed_events7,
    )
    create_table(
        yt_client, act_headers_dir, ACTS_META1[RUN_ID_KEY],
        schema=ACT_HEADERS_TABLE_SCHEMA, meta=ACTS_META1, rows=act_headers1,
    )
    create_table(
        yt_client, daily_act_headers_dir, DAILY_ACTS_META1[RUN_ID_KEY],
        schema=ACT_HEADERS_TABLE_SCHEMA, meta=DAILY_ACTS_META1, rows=daily_act_headers1,
    )
    create_table(
        yt_client, daily_act_headers_dir, DAILY_ACTS_META2[RUN_ID_KEY],
        schema=ACT_HEADERS_TABLE_SCHEMA, meta=DAILY_ACTS_META2, rows=daily_act_headers2,
    )
    create_table(
        yt_client, act_rows_dir, ACTS_META1[RUN_ID_KEY],
        schema=ACT_ROWS_TABLE_SCHEMA, meta=ACTS_META1, rows=act_rows1,
    )
    create_table(
        yt_client, daily_act_rows_dir, DAILY_ACTS_META1[RUN_ID_KEY],
        schema=ACT_ROWS_TABLE_SCHEMA, meta=DAILY_ACTS_META1, rows=daily_act_rows1,
    )
    create_table(
        yt_client, daily_act_rows_dir, DAILY_ACTS_META2[RUN_ID_KEY],
        schema=ACT_ROWS_TABLE_SCHEMA, meta=DAILY_ACTS_META2, rows=daily_act_rows2,
    )
    create_table(
        yt_client, interim_act_rows_dir, INTERIM_META1[RUN_ID_KEY],
        schema=INTERIM_ACT_ROWS_TABLE_SCHEMA, meta=INTERIM_META1, rows=interim_act_rows1,
    )
    create_table(
        yt_client, interim_act_rows_dir, INTERIM_META2[RUN_ID_KEY],
        schema=INTERIM_ACT_ROWS_TABLE_SCHEMA, meta=INTERIM_META2, rows=interim_act_rows2,
    )
    create_table(
        yt_client, interim_act_rows_dir, INTERIM_META3[RUN_ID_KEY],
        schema=INTERIM_ACT_ROWS_TABLE_SCHEMA, meta=INTERIM_META3, rows=interim_act_rows3,
    )
    create_table(
        yt_client, acted_events_dir, INTERIM_META1[RUN_ID_KEY],
        schema=ACTED_EVENTS_TABLE_SCHEMA, meta=INTERIM_META1, rows=acted_events1,
    )
    create_table(
        yt_client, acted_events_dir, INTERIM_META2[RUN_ID_KEY],
        schema=ACTED_EVENTS_TABLE_SCHEMA, meta=INTERIM_META2, rows=acted_events2,
    )
    create_table(
        yt_client, acted_events_dir, INTERIM_META3[RUN_ID_KEY],
        schema=ACTED_EVENTS_TABLE_SCHEMA, meta=INTERIM_META3, rows=acted_events3,
    )
    monthly_acted_events_table = create_table(
        yt_client, monthly_acted_events_dir, ACTS_META1[RUN_ID_KEY],
        schema=MONTHLY_ACTED_EVENTS_TABLE_SCHEMA, meta=ACTS_META1, rows=monthly_acted_events1,
    )
    yt_client.run_sort(
        source_table=monthly_acted_events_table,
        destination_table=monthly_acted_events_table,
        sort_by=['act_id'],
    )
    daily_acted_events_table = create_table(
        yt_client, daily_acted_events_dir, DAILY_ACTS_META1[RUN_ID_KEY],
        schema=DAILY_ACTED_EVENTS_TABLE_SCHEMA, meta=DAILY_ACTS_META1, rows=daily_acted_events1,
    )
    yt_client.run_sort(
        source_table=daily_acted_events_table,
        destination_table=daily_acted_events_table,
        sort_by=['act_id'],
    )
    daily_acted_events_table = create_table(
        yt_client, daily_acted_events_dir, DAILY_ACTS_META2[RUN_ID_KEY],
        schema=DAILY_ACTED_EVENTS_TABLE_SCHEMA, meta=DAILY_ACTS_META2, rows=daily_acted_events2,
    )
    yt_client.run_sort(
        source_table=daily_acted_events_table,
        destination_table=daily_acted_events_table,
        sort_by=['act_id'],
    )
    create_table(
        yt_client, unprocessed_events_full_dir, INTERIM_META3[RUN_ID_KEY],
        schema=UNPROCESSED_EVENTS_FULL_TABLE_SCHEMA, meta=INTERIM_META3, rows=unprocessed_events_full,
    )
    create_table(
        yt_client, unprocessed_events_stripped_dir, INTERIM_META3[RUN_ID_KEY],
        schema=UNPROCESSED_EVENTS_STRIPPED_TABLE_SCHEMA, meta=INTERIM_META3, rows=unprocessed_events_stripped,
    )
    create_table(
        yt_client, unprocessed_interim_rows_dir, ACTS_META1[RUN_ID_KEY],
        schema=UNPROCESSED_INTERIM_ROWS_TABLE_SCHEMA, meta=ACTS_META1, rows=unprocessed_interim_rows,
    )


def prepare_external_tables(
    run_id,
    yt_client=None,
    external_act_headers_dir=None,
    external_act_rows_dir=None,
    external_interim_act_rows_dir=None,
    external_acted_events_dir=None,
    external_unprocessed_events_full_dir=None,
    external_act_headers=None,
    external_act_rows=None,
    external_interim_act_rows=None,
    external_acted_events=None,
    external_unprocessed_events_full=None,
    **kwargs,
):
    create_table(
        yt_client, external_act_headers_dir, run_id,
        schema=ACT_HEADERS_TABLE_SCHEMA, rows=external_act_headers,
    )
    create_table(
        yt_client, external_act_rows_dir, run_id,
        schema=ACT_ROWS_TABLE_SCHEMA, rows=external_act_rows,
    )
    create_table(
        yt_client, external_interim_act_rows_dir, run_id,
        schema=INTERIM_ACT_ROWS_TABLE_SCHEMA, rows=external_interim_act_rows,
    )
    create_table(
        yt_client, external_acted_events_dir, run_id,
        schema=ACTED_EVENTS_TABLE_SCHEMA, rows=external_acted_events,
    )
    create_table(
        yt_client, external_unprocessed_events_full_dir, run_id,
        schema=EXTERNAL_UNPROCESSED_EVENTS_FULL_TABLE_SCHEMA, rows=external_unprocessed_events_full,
    )


@pytest.fixture(name='job')
def job_fixture(
    yt_client,
    yql_client,
    # commands_meta,
    # acts_meta,
    # daily_acts_meta,
    # tariffed_events_stripped_table,
    # enable_consistency_checks,
    commands_dir,
    tariffed_events_dir,
    external_act_headers_dir,
    external_act_rows_dir,
    external_interim_act_rows_dir,
    external_acted_events_dir,
    external_unprocessed_events_full_dir,
    act_headers_dir,
    daily_act_headers_dir,
    act_rows_dir,
    daily_act_rows_dir,
    interim_act_rows_dir,
    acted_events_dir,
    monthly_acted_events_dir,
    daily_acted_events_dir,
    unprocessed_events_full_dir,
    unprocessed_events_stripped_dir,
    unprocessed_interim_rows_dir,
    res_act_headers_dir,
    res_daily_act_headers_dir,
    res_act_rows_dir,
    res_daily_act_rows_dir,
    res_interim_act_rows_dir,
    res_acted_events_dir,
    res_unprocessed_events_full_dir,
    res_unprocessed_events_stripped_dir,
    res_unprocessed_interim_rows_dir,
):
    default_args = (
        'yt_client',
        'yql_client',
        'commands_meta',
        'acts_meta',
        'daily_acts_meta',
        'tariffed_events_stripped_table',
        'enable_consistency_checks',
        'commands_dir',
        'tariffed_events_dir',
        'external_act_headers_dir',
        'external_act_rows_dir',
        'external_interim_act_rows_dir',
        'external_acted_events_dir',
        'external_unprocessed_events_full_dir',
        'act_headers_dir',
        'daily_act_headers_dir',
        'act_rows_dir',
        'daily_act_rows_dir',
        'interim_act_rows_dir',
        'acted_events_dir',
        'monthly_acted_events_dir',
        'daily_acted_events_dir',
        'unprocessed_events_full_dir',
        'unprocessed_events_stripped_dir',
        'unprocessed_interim_rows_dir',
        'res_act_headers_dir',
        'res_daily_act_headers_dir',
        'res_act_rows_dir',
        'res_daily_act_rows_dir',
        'res_interim_act_rows_dir',
        'res_acted_events_dir',
        'res_unprocessed_events_full_dir',
        'res_unprocessed_events_stripped_dir',
        'res_unprocessed_interim_rows_dir',
    )
    fixture_kwargs = locals()

    def _wrapped(**kwargs):
        args = []
        for arg_name in default_args:
            args.append(kwargs.get(arg_name, fixture_kwargs.get(arg_name, None)))
            kwargs.pop(arg_name, None)

        return Job(*args, **kwargs)

    return _wrapped


def test_locked(
    yt_client,
    job,
    tariffed_events_stripped_dir,
    commands_dir,
):
    commands_meta = COMMANDS_META1
    acts_meta = ACTS_META1
    daily_acts_meta = DAILY_ACTS_META2

    tariffed_events_stripped_table = create_table(
        yt_client,
        tariffed_events_stripped_dir,
        commands_meta[RUN_ID_KEY],
        meta=commands_meta,
    )

    alt_yt_client = create_yt_client()
    with alt_yt_client.Transaction():
        alt_yt_client.lock(commands_dir)

        with pytest.raises(yt_common.YtError, match='Timed out while waiting'):
            with yt_client.Transaction() as transaction:
                job(
                    yt_client=yt_client,
                    commands_meta=commands_meta,
                    acts_meta=acts_meta,
                    daily_acts_meta=daily_acts_meta,
                    tariffed_events_stripped_table=tariffed_events_stripped_table,
                    enable_consistency_checks=True,
                    transaction_id=transaction.transaction_id,
                    lock_wait_seconds=1,
                ).run()


def get_result_meta(yt_client, path):
    meta_attr = yt_client.get(ypath_join(path, '@' + LOG_TARIFF_META_ATTR))
    run_id = meta_attr.pop('run_id')
    assert run_id
    return meta_attr


def get_result_rows(yt_client, path, key):
    return sorted(yt_client.read_table(path), key=key)


def get_result(yt_client, result):
    meta = result.commands_meta
    unprocessed_commands = result.unprocessed_commands
    res_act_headers_table = result.res_act_headers_table
    res_daily_act_headers_table = result.res_daily_act_headers_table
    res_act_rows_table = result.res_act_rows_table
    res_daily_act_rows_table = result.res_daily_act_rows_table
    res_interim_act_rows_table = result.res_interim_act_rows_table
    res_acted_events_table = result.res_acted_events_table
    res_unprocessed_events_full_table = result.res_unprocessed_events_full_table
    res_unprocessed_events_stripped_table = result.res_unprocessed_events_stripped_table
    res_unprocessed_interim_rows_table = result.res_unprocessed_interim_rows_table

    meta = meta.copy()
    assert meta.pop('run_id')

    return {
        'meta': meta,
        'unprocessed_commands': unprocessed_commands,
        'act_headers': {
            'rows': get_result_rows(yt_client, res_act_headers_table, lambda row: row['act_id']),
            'meta': get_result_meta(yt_client, res_act_headers_table),
        },
        'daily_act_headers': {
            'rows': get_result_rows(yt_client, res_daily_act_headers_table, lambda row: row['act_id']),
            'meta': get_result_meta(yt_client, res_daily_act_headers_table),
        },
        'act_rows': {
            'rows': get_result_rows(yt_client, res_act_rows_table, lambda row: (row['act_id'], row['UID'])),
            'meta': get_result_meta(yt_client, res_act_rows_table),
        },
        'daily_act_rows': {
            'rows': get_result_rows(yt_client, res_daily_act_rows_table, lambda row: (row['act_id'], row['UID'])),
            'meta': get_result_meta(yt_client, res_daily_act_rows_table),
        },
        'interim_act_rows': {
            'rows': get_result_rows(yt_client, res_interim_act_rows_table, lambda row: row['UID']),
            'meta': get_result_meta(yt_client, res_interim_act_rows_table),
        },
        'acted_events': {
            'rows': get_result_rows(yt_client, res_acted_events_table, lambda row: (row['UID'], row['row_UID'])),
            'meta': get_result_meta(yt_client, res_acted_events_table),
        },
        'unprocessed_events_full': {
            'rows': get_result_rows(yt_client, res_unprocessed_events_full_table, lambda row: row['UID']),
            'meta': get_result_meta(yt_client, res_unprocessed_events_full_table),
        },
        'unprocessed_events_stripped': {
            'rows': get_result_rows(yt_client, res_unprocessed_events_stripped_table, lambda row: row['UID']),
            'meta': get_result_meta(yt_client, res_unprocessed_events_stripped_table),
        },
        'unprocessed_interim_rows': {
            'rows': get_result_rows(yt_client, res_unprocessed_interim_rows_table, lambda row: row['UID']),
            'meta': get_result_meta(yt_client, res_unprocessed_interim_rows_table),
        },
    }


@pytest.mark.parametrize('is_daily', [False, True])
def test_create(
    yt_client,
    job,
    commands_dir,
    tariffed_events_dir,
    external_act_headers_dir,
    external_act_rows_dir,
    external_interim_act_rows_dir,
    external_acted_events_dir,
    external_unprocessed_events_full_dir,
    act_headers_dir,
    daily_act_headers_dir,
    act_rows_dir,
    daily_act_rows_dir,
    interim_act_rows_dir,
    acted_events_dir,
    monthly_acted_events_dir,
    daily_acted_events_dir,
    unprocessed_events_full_dir,
    unprocessed_events_stripped_dir,
    unprocessed_interim_rows_dir,
    is_daily,
):
    commands_meta = COMMANDS_META1.copy()
    commands_meta[PREV_COMMANDS_TABLE] = '0'
    commands_meta[LAST_COMMANDS_TABLE] = '2'
    acts_meta = ACTS_META1
    daily_acts_meta = DAILY_ACTS_META2

    tariffed_events_stripped_table = yt_client.create_temp_table(
        prefix='tariffed_events_stripped_table', attributes={'schema': TARIFFED_EVENTS_TABLE_SCHEMA}
    )
    utils.meta.set_log_tariff_meta(yt_client, tariffed_events_stripped_table, commands_meta)

    commands1 = [
        {'id': 0, 'command': 'create',
         'params': json.dumps({'new_act_id': 'YB-1-1', 'ticket_id': 'TEST-1'})},
    ]
    create_table(
        yt_client, commands_dir, '2',
        schema=COMMANDS_TABLE_SCHEMA, rows=commands1,
    )

    prepare_internal_tables(**locals())

    if is_daily:
        act_dt = DAILY_ACT_DT
    else:
        act_dt = ACT_DT

    external_act_headers = [
        {'act_id': 'YB-1-1', 'act_dt': act_dt.int_timestamp, 'act_sum': 0.1, 'hidden': None, 'ticket_id': None},
    ]
    external_act_rows = [
        {'UID': 'r1', 'act_id': 'YB-1-1', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r2', 'act_id': 'YB-1-1', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r3', 'act_id': 'YB-1-1', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    external_interim_act_rows = [
        {'UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    external_acted_events = [
        {'UID': '1', 'row_UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': '2', 'row_UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': '3', 'row_UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    external_unprocessed_events_full = [
        {'command_id': 0, 'UID': 'YB-1-1_ue1'},
        {'command_id': 0, 'UID': 'YB-1-1_ue2'},
        {'command_id': 1, 'UID': 'YB-2_ue1'},
    ]
    prepare_external_tables('2', **locals())

    with yt_client.Transaction() as transaction:
        job_ = job(
            yt_client=yt_client,
            commands_meta=commands_meta,
            acts_meta=acts_meta,
            daily_acts_meta=daily_acts_meta,
            tariffed_events_stripped_table=tariffed_events_stripped_table,
            enable_consistency_checks=True,
            transaction_id=transaction.transaction_id,
        )
        result = job_.run()

    job_.set_all_meta()

    return get_result(yt_client, result)


@pytest.mark.parametrize('is_daily', [False, True])
def test_recreate(
    yt_client,
    job,
    commands_dir,
    tariffed_events_dir,
    external_act_headers_dir,
    external_act_rows_dir,
    external_interim_act_rows_dir,
    external_acted_events_dir,
    external_unprocessed_events_full_dir,
    act_headers_dir,
    daily_act_headers_dir,
    act_rows_dir,
    daily_act_rows_dir,
    interim_act_rows_dir,
    acted_events_dir,
    monthly_acted_events_dir,
    daily_acted_events_dir,
    unprocessed_events_full_dir,
    unprocessed_events_stripped_dir,
    unprocessed_interim_rows_dir,
    is_daily,
):
    commands_meta = COMMANDS_META1.copy()
    commands_meta[PREV_COMMANDS_TABLE] = '0'
    commands_meta[LAST_COMMANDS_TABLE] = '2'
    acts_meta = ACTS_META1
    daily_acts_meta = DAILY_ACTS_META2

    tariffed_events_stripped_table = yt_client.create_temp_table(
        prefix='tariffed_events_stripped_table', attributes={'schema': TARIFFED_EVENTS_TABLE_SCHEMA}
    )
    utils.meta.set_log_tariff_meta(yt_client, tariffed_events_stripped_table, commands_meta)

    commands1 = [
        {'id': 0, 'command': 'create',
         'params': json.dumps({'act_id': 'YB-1', 'new_act_id': 'YB-1-1', 'ticket_id': 'TEST-1'})},
    ]
    create_table(
        yt_client, commands_dir, '2',
        schema=COMMANDS_TABLE_SCHEMA, rows=commands1,
    )

    if is_daily:
        act_dt = DAILY_ACT_DT
    else:
        act_dt = ACT_DT

    if is_daily:
        daily_act_headers1 = [
            {'act_id': 'YB-1', 'act_dt': act_dt.int_timestamp, 'act_sum': 0.1, 'hidden': None, 'ticket_id': None},
        ]
        daily_act_rows1 = [
            {'UID': 'r1', 'act_id': 'YB-1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'UID': 'r2', 'act_id': 'YB-1', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'UID': 'r3', 'act_id': 'YB-1', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
        interim_act_rows1 = [
            {'UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
        daily_acted_events1 = [
            {'act_id': 'YB-1', 'UID': '1', 'row_UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'act_id': 'YB-1', 'UID': '2', 'row_UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'act_id': 'YB-1', 'UID': '3', 'row_UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
    else:
        act_headers1 = [
            {'act_id': 'YB-1', 'act_dt': act_dt.int_timestamp, 'act_sum': 0.1, 'hidden': None, 'ticket_id': None},
        ]
        act_rows1 = [
            {'UID': 'r1', 'act_id': 'YB-1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'UID': 'r2', 'act_id': 'YB-1', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'UID': 'r3', 'act_id': 'YB-1', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
        interim_act_rows1 = [
            {'UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
        monthly_acted_events1 = [
            {'act_id': 'YB-1', 'UID': '1', 'row_UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'act_id': 'YB-1', 'UID': '2', 'row_UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'act_id': 'YB-1', 'UID': '3', 'row_UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
    prepare_internal_tables(**locals())

    external_act_headers = [{'act_id': 'YB-1-1', 'act_dt': act_dt.int_timestamp, 'act_sum': 0.1}]
    external_act_rows = [
        {'UID': 'r1', 'act_id': 'YB-1-1', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r2', 'act_id': 'YB-1-1', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r3', 'act_id': 'YB-1-1', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    external_interim_act_rows = [
        {'UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    external_acted_events = [
        {'UID': '1', 'row_UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': '2', 'row_UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': '3', 'row_UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    prepare_external_tables('2', **locals())

    with yt_client.Transaction() as transaction:
        job_ = job(
            yt_client=yt_client,
            commands_meta=commands_meta,
            acts_meta=acts_meta,
            daily_acts_meta=daily_acts_meta,
            tariffed_events_stripped_table=tariffed_events_stripped_table,
            enable_consistency_checks=True,
            transaction_id=transaction.transaction_id,
        )
        result = job_.run()

    job_.set_all_meta()

    return get_result(yt_client, result)


@pytest.mark.parametrize('is_daily', [False, True])
def test_update(
    yt_client,
    job,
    commands_dir,
    tariffed_events_dir,
    external_act_headers_dir,
    external_act_rows_dir,
    external_interim_act_rows_dir,
    external_acted_events_dir,
    external_unprocessed_events_full_dir,
    act_headers_dir,
    daily_act_headers_dir,
    act_rows_dir,
    daily_act_rows_dir,
    interim_act_rows_dir,
    acted_events_dir,
    monthly_acted_events_dir,
    daily_acted_events_dir,
    unprocessed_events_full_dir,
    unprocessed_events_stripped_dir,
    unprocessed_interim_rows_dir,
    is_daily,
):
    commands_meta = COMMANDS_META1.copy()
    commands_meta[PREV_COMMANDS_TABLE] = '0'
    commands_meta[LAST_COMMANDS_TABLE] = '2'
    acts_meta = ACTS_META1
    daily_acts_meta = DAILY_ACTS_META2

    tariffed_events_stripped_table = yt_client.create_temp_table(
        prefix='tariffed_events_stripped_table', attributes={'schema': TARIFFED_EVENTS_TABLE_SCHEMA}
    )
    utils.meta.set_log_tariff_meta(yt_client, tariffed_events_stripped_table, commands_meta)

    commands1 = [
        {'id': 0, 'command': 'update', 'params': json.dumps({'act_id': 'YB-1', 'new_act_id': 'YB-1-1', 'ticket_id': 'TEST-1'})},
    ]
    create_table(
        yt_client, commands_dir, '2',
        schema=COMMANDS_TABLE_SCHEMA, rows=commands1,
    )

    if is_daily:
        act_dt = DAILY_ACT_DT
    else:
        act_dt = ACT_DT

    if is_daily:
        daily_act_headers1 = [{'act_id': 'YB-1', 'act_dt': act_dt.int_timestamp, 'act_sum': 0.1, 'hidden': None, 'ticket_id': None}]
        daily_act_rows1 = [
            {'UID': 'r1', 'act_id': 'YB-1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'UID': 'r2', 'act_id': 'YB-1', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'UID': 'r3', 'act_id': 'YB-1', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
        interim_act_rows1 = [
            {'UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
        daily_acted_events1 = [
            {'act_id': 'YB-1', 'UID': '1', 'row_UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'act_id': 'YB-1', 'UID': '2', 'row_UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'act_id': 'YB-1', 'UID': '3', 'row_UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
    else:
        act_headers1 = [{'act_id': 'YB-1', 'act_dt': act_dt.int_timestamp, 'act_sum': 0.1, 'hidden': None, 'ticket_id': None}]
        act_rows1 = [
            {'UID': 'r1', 'act_id': 'YB-1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'UID': 'r2', 'act_id': 'YB-1', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'UID': 'r3', 'act_id': 'YB-1', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
        interim_act_rows1 = [
            {'UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
        monthly_acted_events1 = [
            {'act_id': 'YB-1', 'UID': '1', 'row_UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'act_id': 'YB-1', 'UID': '2', 'row_UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'act_id': 'YB-1', 'UID': '3', 'row_UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
    prepare_internal_tables(**locals())

    prepare_external_tables('2', **locals())

    with yt_client.Transaction() as transaction:
        job_ = job(
            yt_client=yt_client,
            commands_meta=commands_meta,
            acts_meta=acts_meta,
            daily_acts_meta=daily_acts_meta,
            tariffed_events_stripped_table=tariffed_events_stripped_table,
            enable_consistency_checks=True,
            transaction_id=transaction.transaction_id,
        )
        result = job_.run()

    job_.set_all_meta()

    return get_result(yt_client, result)


@pytest.mark.parametrize('is_daily', [False, True])
def test_delete(
    yt_client,
    job,
    commands_dir,
    tariffed_events_dir,
    external_act_headers_dir,
    external_act_rows_dir,
    external_interim_act_rows_dir,
    external_acted_events_dir,
    external_unprocessed_events_full_dir,
    act_headers_dir,
    daily_act_headers_dir,
    act_rows_dir,
    daily_act_rows_dir,
    interim_act_rows_dir,
    acted_events_dir,
    monthly_acted_events_dir,
    daily_acted_events_dir,
    unprocessed_events_full_dir,
    unprocessed_events_stripped_dir,
    unprocessed_interim_rows_dir,
    is_daily,
):
    commands_meta = COMMANDS_META1.copy()
    commands_meta[PREV_COMMANDS_TABLE] = '0'
    commands_meta[LAST_COMMANDS_TABLE] = '2'
    acts_meta = ACTS_META1
    daily_acts_meta = DAILY_ACTS_META2

    tariffed_events_stripped_table = yt_client.create_temp_table(
        prefix='tariffed_events_stripped_table', attributes={'schema': TARIFFED_EVENTS_TABLE_SCHEMA}
    )
    utils.meta.set_log_tariff_meta(yt_client, tariffed_events_stripped_table, commands_meta)

    commands1 = [
        {'id': 0, 'command': 'delete', 'params': json.dumps({'act_id': 'YB-1', 'ticket_id': 'TEST-1'})},
    ]
    create_table(
        yt_client, commands_dir, '2',
        schema=COMMANDS_TABLE_SCHEMA, rows=commands1,
    )

    if is_daily:
        act_dt = DAILY_ACT_DT
    else:
        act_dt = ACT_DT

    if is_daily:
        daily_act_headers1 = [
            {'act_id': 'YB-1', 'act_dt': act_dt.int_timestamp, 'act_sum': 0.1, 'hidden': None, 'ticket_id': None}]
        daily_act_rows1 = [
            {'UID': 'r1', 'act_id': 'YB-1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'UID': 'r2', 'act_id': 'YB-1', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'UID': 'r3', 'act_id': 'YB-1', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
        interim_act_rows1 = [
            {'UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
        daily_acted_events1 = [
            {'act_id': 'YB-1', 'UID': '1', 'row_UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'act_id': 'YB-1', 'UID': '2', 'row_UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'act_id': 'YB-1', 'UID': '3', 'row_UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
    else:
        act_headers1 = [{'act_id': 'YB-1', 'act_dt': act_dt.int_timestamp, 'act_sum': 0.1, 'hidden': None, 'ticket_id': None}]
        act_rows1 = [
            {'UID': 'r1', 'act_id': 'YB-1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'UID': 'r2', 'act_id': 'YB-1', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'UID': 'r3', 'act_id': 'YB-1', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
        interim_act_rows1 = [
            {'UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
        monthly_acted_events1 = [
            {'act_id': 'YB-1', 'UID': '1', 'row_UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
            {'act_id': 'YB-1', 'UID': '2', 'row_UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
            {'act_id': 'YB-1', 'UID': '3', 'row_UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
        ]
    prepare_internal_tables(**locals())

    prepare_external_tables('2', **locals())

    with yt_client.Transaction() as transaction:
        job_ = job(
            yt_client=yt_client,
            commands_meta=commands_meta,
            acts_meta=acts_meta,
            daily_acts_meta=daily_acts_meta,
            tariffed_events_stripped_table=tariffed_events_stripped_table,
            enable_consistency_checks=True,
            transaction_id=transaction.transaction_id,
        )
        result = job_.run()

    job_.set_all_meta()

    return get_result(yt_client, result)


def test_unprocessed(
    yt_client,
    job,
    commands_dir,
    tariffed_events_dir,
    external_act_headers_dir,
    external_act_rows_dir,
    external_interim_act_rows_dir,
    external_acted_events_dir,
    external_unprocessed_events_full_dir,
    act_headers_dir,
    daily_act_headers_dir,
    act_rows_dir,
    daily_act_rows_dir,
    interim_act_rows_dir,
    acted_events_dir,
    monthly_acted_events_dir,
    daily_acted_events_dir,
    unprocessed_events_full_dir,
    unprocessed_events_stripped_dir,
    unprocessed_interim_rows_dir,
):
    commands_meta = COMMANDS_META1.copy()
    commands_meta[PREV_COMMANDS_TABLE] = '0'
    commands_meta[LAST_COMMANDS_TABLE] = '2'
    acts_meta = ACTS_META1
    daily_acts_meta = DAILY_ACTS_META2

    tariffed_events1 = [
        {'UID': 'te11'},
        {'UID': 'te12'},
    ]
    tariffed_events2 = [
        {'UID': 'te21'},
        {'UID': 'te22'},
    ]
    tariffed_events3 = [
        {'UID': 'te31'},
        {'UID': 'te32'},
    ]
    tariffed_events4 = [
        {'UID': 'te41'},
        {'UID': 'te42'},
    ]
    tariffed_events5 = [
        {'UID': 'te51'},
        {'UID': 'te52'},
    ]
    tariffed_events6 = [
        {'UID': 'te61'},
        {'UID': 'te62'},
    ]
    tariffed_events7 = [
        {'UID': 'te71'},
        {'UID': 'te72'},
    ]
    tariffed_events_stripped_table = yt_client.create_temp_table(
        prefix='tariffed_events_stripped_table', attributes={'schema': TARIFFED_EVENTS_TABLE_SCHEMA}
    )
    yt_client.write_table(
        tariffed_events_stripped_table,
        tariffed_events7,
    )
    utils.meta.set_log_tariff_meta(yt_client, tariffed_events_stripped_table, commands_meta)

    commands = []
    create_table(
        yt_client, commands_dir, '2',
        schema=COMMANDS_TABLE_SCHEMA, rows=commands,
    )

    daily_act_headers1 = [
        {'act_id': 'YB-1', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 0.1, 'hidden': None, 'ticket_id': None}]
    daily_act_rows1 = [
        {'UID': 'r1', 'act_id': 'YB-1', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r2', 'act_id': 'YB-1', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r3', 'act_id': 'YB-1', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    interim_act_rows1 = [
        {'UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    daily_acted_events1 = [
        {'act_id': 'YB-1', 'UID': '1', 'row_UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'act_id': 'YB-1', 'UID': '2', 'row_UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'act_id': 'YB-1', 'UID': '3', 'row_UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    daily_act_headers2 = [
        {'act_id': 'YB-2', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 0.1, 'hidden': None, 'ticket_id': None}]
    daily_act_rows2 = [
        {'UID': 'r4', 'act_id': 'YB-2', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r5', 'act_id': 'YB-2', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r6', 'act_id': 'YB-2', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    interim_act_rows2 = [
        {'UID': 'r4', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r5', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r6', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    daily_acted_events2 = [
        {'act_id': 'YB-2', 'UID': '4', 'row_UID': 'r4', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'act_id': 'YB-2', 'UID': '5', 'row_UID': 'r5', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'act_id': 'YB-2', 'UID': '6', 'row_UID': 'r6', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    unprocessed_events_full = [
        {'UID': 'ue1'},
        {'UID': 'ue2'},
        {'UID': 'ue3'},
    ]
    unprocessed_events_stripped = [
        {'UID': 'ue1'},
        {'UID': 'ue2'},
        {'UID': 'ue3'},
    ]
    unprocessed_interim_rows = [
        {'UID': 'r-1', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r-2', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r-3', 'acted_qty': 1.5, 'acted_sum': 1.5},
        {'UID': 'r4', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r5', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r6', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]

    prepare_internal_tables(**locals())

    prepare_external_tables('2', **locals())

    with yt_client.Transaction() as transaction:
        job_ = job(
            yt_client=yt_client,
            commands_meta=commands_meta,
            acts_meta=acts_meta,
            daily_acts_meta=daily_acts_meta,
            tariffed_events_stripped_table=tariffed_events_stripped_table,
            enable_consistency_checks=True,
            transaction_id=transaction.transaction_id,
        )
        result = job_.run()

    job_.set_all_meta()

    return get_result(yt_client, result)


def test_unprocessed_wo_interim(
    yt_client,
    job,
    commands_dir,
    tariffed_events_dir,
    external_act_headers_dir,
    external_act_rows_dir,
    external_interim_act_rows_dir,
    external_acted_events_dir,
    external_unprocessed_events_full_dir,
    act_headers_dir,
    daily_act_headers_dir,
    act_rows_dir,
    daily_act_rows_dir,
    interim_act_rows_dir,
    acted_events_dir,
    monthly_acted_events_dir,
    daily_acted_events_dir,
    unprocessed_events_full_dir,
    unprocessed_events_stripped_dir,
    unprocessed_interim_rows_dir,
):
    commands_meta = COMMANDS_META1_WIDE.copy()
    commands_meta[PREV_COMMANDS_TABLE] = '0'
    commands_meta[LAST_COMMANDS_TABLE] = '2'
    acts_meta = ACTS_META1
    daily_acts_meta = DAILY_ACTS_META2

    tariffed_events1 = [
        {'UID': 'te11'},
        {'UID': 'te12'},
    ]
    tariffed_events2 = [
        {'UID': 'te21'},
        {'UID': 'te22'},
    ]
    tariffed_events3 = [
        {'UID': 'te31'},
        {'UID': 'te32'},
    ]
    tariffed_events4 = [
        {'UID': 'te41'},
        {'UID': 'te42'},
    ]
    tariffed_events5 = [
        {'UID': 'te51'},
        {'UID': 'te52'},
    ]
    tariffed_events6 = [
        {'UID': 'te61'},
        {'UID': 'te62'},
    ]
    tariffed_events7 = [
        {'UID': 'te71'},
        {'UID': 'te72'},
    ]
    tariffed_events_stripped_table = yt_client.create_temp_table(
        prefix='tariffed_events_stripped_table', attributes={'schema': TARIFFED_EVENTS_TABLE_SCHEMA}
    )
    yt_client.write_table(
        tariffed_events_stripped_table,
        tariffed_events7,
    )
    utils.meta.set_log_tariff_meta(yt_client, tariffed_events_stripped_table, commands_meta)

    commands = []
    create_table(
        yt_client, commands_dir, '2',
        schema=COMMANDS_TABLE_SCHEMA, rows=commands,
    )

    daily_act_headers1 = [
        {'act_id': 'YB-1', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 0.1, 'hidden': None, 'ticket_id': None}]
    daily_act_rows1 = [
        {'UID': 'r1', 'act_id': 'YB-1', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r2', 'act_id': 'YB-1', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r3', 'act_id': 'YB-1', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    interim_act_rows1 = [
        {'UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    daily_acted_events1 = [
        {'act_id': 'YB-1', 'UID': '1', 'row_UID': 'r1', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'act_id': 'YB-1', 'UID': '2', 'row_UID': 'r2', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'act_id': 'YB-1', 'UID': '3', 'row_UID': 'r3', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    daily_act_headers2 = [
        {'act_id': 'YB-2', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 0.1, 'hidden': None, 'ticket_id': None}]
    daily_act_rows2 = [
        {'UID': 'r4', 'act_id': 'YB-2', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r5', 'act_id': 'YB-2', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r6', 'act_id': 'YB-2', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    daily_acted_events2 = [
        {'act_id': 'YB-2', 'UID': '4', 'row_UID': 'r4', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'act_id': 'YB-2', 'UID': '5', 'row_UID': 'r5', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'act_id': 'YB-2', 'UID': '6', 'row_UID': 'r6', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]
    unprocessed_events_full = [
        {'UID': 'ue1'},
        {'UID': 'ue2'},
        {'UID': 'ue3'},
    ]
    unprocessed_events_stripped = [
        {'UID': 'ue1'},
        {'UID': 'ue2'},
        {'UID': 'ue3'},
    ]
    unprocessed_interim_rows = [
        {'UID': 'r-1', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r-2', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r-3', 'acted_qty': 1.5, 'acted_sum': 1.5},
        {'UID': 'r4', 'acted_qty': 0.5, 'acted_sum': 0.5},
        {'UID': 'r5', 'acted_qty': -1.9, 'acted_sum': -1.9},
        {'UID': 'r6', 'acted_qty': 1.5, 'acted_sum': 1.5},
    ]

    prepare_internal_tables(**locals())

    prepare_external_tables('2', **locals())

    for meta in (INTERIM_META2, INTERIM_META3):
        yt_client.remove(ypath_join(interim_act_rows_dir, meta[RUN_ID_KEY]))
        yt_client.remove(ypath_join(acted_events_dir, meta[RUN_ID_KEY]))

    yt_client.remove(ypath_join(unprocessed_events_full_dir, INTERIM_META3[RUN_ID_KEY]))
    yt_client.remove(ypath_join(unprocessed_events_stripped_dir, INTERIM_META3[RUN_ID_KEY]))
    create_table(
        yt_client, unprocessed_events_full_dir, INTERIM_META1[RUN_ID_KEY],
        schema=UNPROCESSED_EVENTS_FULL_TABLE_SCHEMA, meta=INTERIM_META1, rows=unprocessed_events_full,
    )
    create_table(
        yt_client, unprocessed_events_stripped_dir, INTERIM_META1[RUN_ID_KEY],
        schema=UNPROCESSED_EVENTS_STRIPPED_TABLE_SCHEMA, meta=INTERIM_META1, rows=unprocessed_events_stripped,
    )

    with yt_client.Transaction() as transaction:
        job_ = job(
            yt_client=yt_client,
            commands_meta=commands_meta,
            acts_meta=acts_meta,
            daily_acts_meta=daily_acts_meta,
            tariffed_events_stripped_table=tariffed_events_stripped_table,
            enable_consistency_checks=True,
            transaction_id=transaction.transaction_id,
        )
        result = job_.run()

    job_.set_all_meta()

    return get_result(yt_client, result)


def test_run(
    yt_client,
    job,
    commands_dir,
    tariffed_events_dir,
    external_act_headers_dir,
    external_act_rows_dir,
    external_interim_act_rows_dir,
    external_acted_events_dir,
    external_unprocessed_events_full_dir,
    act_headers_dir,
    daily_act_headers_dir,
    act_rows_dir,
    daily_act_rows_dir,
    interim_act_rows_dir,
    acted_events_dir,
    monthly_acted_events_dir,
    daily_acted_events_dir,
    unprocessed_events_full_dir,
    unprocessed_events_stripped_dir,
    unprocessed_interim_rows_dir,
):
    commands_meta = COMMANDS_META1.copy()
    commands_meta[PREV_COMMANDS_TABLE] = '0'
    commands_meta[LAST_COMMANDS_TABLE] = '2'
    acts_meta = ACTS_META1
    daily_acts_meta = DAILY_ACTS_META2

    tariffed_events1 = [
        {'UID': 'te11'},
        {'UID': 'te12'},
    ]
    tariffed_events2 = [
        {'UID': 'te21'},
        {'UID': 'te22'},
    ]
    tariffed_events3 = [
        {'UID': 'te31'},
        {'UID': 'te32'},
    ]
    tariffed_events4 = [
        {'UID': 'te41'},
        {'UID': 'te42'},
    ]
    tariffed_events5 = [
        {'UID': 'te51'},
        {'UID': 'te52'},
    ]
    tariffed_events6 = [
        {'UID': 'te61'},
        {'UID': 'te62'},
    ]
    tariffed_events7 = [
        {'UID': 'te71'},
        {'UID': 'te72'},
    ]
    tariffed_events_stripped_table = yt_client.create_temp_table(
        prefix='tariffed_events_stripped_table', attributes={'schema': TARIFFED_EVENTS_TABLE_SCHEMA}
    )
    yt_client.write_table(
        tariffed_events_stripped_table,
        tariffed_events7,
    )
    utils.meta.set_log_tariff_meta(yt_client, tariffed_events_stripped_table, commands_meta)

    commands = [
        {'id': 0, 'command': 'delete', 'params': json.dumps({'act_id': 'YB-1', 'ticket_id': 'TEST-1'})},
        {'id': 1, 'command': 'update', 'params': json.dumps({'act_id': 'YB-2', 'new_act_id': 'YB-2-1', 'ticket_id': 'TEST-2'})},
    ]
    create_table(
        yt_client, commands_dir, '0',
        schema=COMMANDS_TABLE_SCHEMA, rows=commands,
    )

    commands = [
        # delete non-existent
        {'id': 0, 'command': 'delete', 'params': json.dumps({'act_id': 'YB+1', 'ticket_id': 'TEST-0'})},
        # delete daily existent
        {'id': 1, 'command': 'delete', 'params': json.dumps({'act_id': 'YBD-1', 'ticket_id': 'TEST-1'})},
        # delete monthly existent
        {'id': 2, 'command': 'delete', 'params': json.dumps({'act_id': 'YBM-1', 'ticket_id': 'TEST-2'})},
        # create daily
        {'id': 3, 'command': 'create', 'params': json.dumps({'new_act_id': 'YBD-2', 'ticket_id': 'TEST-3'})},
        # create monthly
        {'id': 4, 'command': 'create', 'params': json.dumps({'new_act_id': 'YBM-2', 'ticket_id': 'TEST-4'})},
        # delete daily created
        {'id': 5, 'command': 'delete', 'params': json.dumps({'act_id': 'YBD-2', 'ticket_id': 'TEST-5'})},
        # delete monthly created
        {'id': 6, 'command': 'delete', 'params': json.dumps({'act_id': 'YBM-2', 'ticket_id': 'TEST-6'})},
        # update non-existent
        {'id': 7, 'command': 'update', 'params': json.dumps({'act_id': 'YB+2', 'new_act_id': 'YB+2-1', 'ticket_id': 'TEST-7'})},
        # update daily
        {'id': 8, 'command': 'update', 'params': json.dumps({'act_id': 'YBD-3', 'new_act_id': 'YBD-3-1', 'ticket_id': 'TEST-8'})},
        # update monthly
        {'id': 9, 'command': 'update', 'params': json.dumps({'act_id': 'YBM-3', 'new_act_id': 'YBM-3-1', 'ticket_id': 'TEST-9'})},
        # create daily
        {'id': 10, 'command': 'create', 'params': json.dumps({'new_act_id': 'YBD-4', 'ticket_id': 'TEST-10'})},
        # create monthly
        {'id': 11, 'command': 'create', 'params': json.dumps({'new_act_id': 'YBM-4', 'ticket_id': 'TEST-11'})},
    ]
    create_table(
        yt_client, commands_dir, '1',
        schema=COMMANDS_TABLE_SCHEMA, rows=commands,
    )

    commands = [
        # update daily created
        {'id': 12, 'command': 'update', 'params': json.dumps({'act_id': 'YBD-4', 'new_act_id': 'YBD-4-1', 'ticket_id': 'TEST-12'})},
        # update monthly created
        {'id': 13, 'command': 'update', 'params': json.dumps({'act_id': 'YBM-4', 'new_act_id': 'YBM-4-1', 'ticket_id': 'TEST-13'})},
        # create daily existent
        {'id': 14, 'command': 'create', 'params': json.dumps({'new_act_id': 'YB-1', 'ticket_id': 'TEST-14'})},
        # create monthly existent
        {'id': 15, 'command': 'create', 'params': json.dumps({'new_act_id': 'YB-2', 'ticket_id': 'TEST-15'})},
        # recreate daily
        {'id': 16, 'command': 'create', 'params': json.dumps({'act_id': 'YBD-5', 'new_act_id': 'YBD-5-1', 'ticket_id': 'TEST-16'})},
        # recreate monthly
        {'id': 17, 'command': 'create', 'params': json.dumps({'act_id': 'YBM-5', 'new_act_id': 'YBM-5-1', 'ticket_id': 'TEST-17'})},
        # recreate any existent as any existent
        {'id': 18, 'command': 'create', 'params': json.dumps({'act_id': 'YBD-6', 'new_act_id': 'YB-3', 'ticket_id': 'TEST-18'})},
        {'id': 19, 'command': 'create', 'params': json.dumps({'act_id': 'YBM-6', 'new_act_id': 'YB-4', 'ticket_id': 'TEST-19'})},
        {'id': 20, 'command': 'create', 'params': json.dumps({'act_id': 'YBD-7', 'new_act_id': 'YB-5', 'ticket_id': 'TEST-20'})},
        {'id': 21, 'command': 'create', 'params': json.dumps({'act_id': 'YBM-7', 'new_act_id': 'YB-6', 'ticket_id': 'TEST-21'})},
        # delete daily created and deleted
        {'id': 22, 'command': 'delete', 'params': json.dumps({'act_id': 'YBD-2', 'ticket_id': 'TEST-5'})},
        # delete monthly created and deleted
        {'id': 23, 'command': 'delete', 'params': json.dumps({'act_id': 'YBM-2', 'ticket_id': 'TEST-6'})},
    ]
    create_table(
        yt_client, commands_dir, '2',
        schema=COMMANDS_TABLE_SCHEMA, rows=commands,
    )

    bad_commands_ids = {0, 7, 14, 15, 18, 19, 20, 21, 22, 23}

    act_headers1 = [
        {'act_id': 'YBM-1', 'act_dt': ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YBM-3', 'act_dt': ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YB-2', 'act_dt': ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YBM-5', 'act_dt': ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YB-4', 'act_dt': ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YB-5', 'act_dt': ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YBM-6', 'act_dt': ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YBM-7', 'act_dt': ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
    ]
    daily_act_headers2 = [
        {'act_id': 'YBD-1', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YBD-3', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YB-1', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YBD-5', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YB-3', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YB-6', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YBD-6', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YBD-7', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
    ]
    act_rows1 = [
        {'UID': 'YBM-1_r1', 'act_id': 'YBM-1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-1_r2', 'act_id': 'YBM-1', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBM-3_r1', 'act_id': 'YBM-3', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-3_r2', 'act_id': 'YBM-3', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBM-5_r1', 'act_id': 'YBM-5', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-5_r2', 'act_id': 'YBM-5', 'acted_qty': -2.0, 'acted_sum': -2.0},
    ]
    daily_act_rows2 = [
        {'UID': 'YBD-1_r1', 'act_id': 'YBD-1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-1_r2', 'act_id': 'YBD-1', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBD-3_r1', 'act_id': 'YBD-3', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-3_r2', 'act_id': 'YBD-3', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBD-5_r1', 'act_id': 'YBD-5', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-5_r2', 'act_id': 'YBD-5', 'acted_qty': -2.0, 'acted_sum': -2.0},
    ]
    daily_acted_events2 = [
        {'act_id': 'YBD-1', 'UID': 'YBD-1_r1_e1', 'row_UID': 'YBD-1_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'act_id': 'YBD-1', 'UID': 'YBD-1_r2_e1', 'row_UID': 'YBD-1_r2', 'acted_qty': 1.0, 'acted_sum': 1.0},
        {'act_id': 'YBD-1', 'UID': 'YBD-1_r2_e2', 'row_UID': 'YBD-1_r2', 'acted_qty': -3.0, 'acted_sum': -3.0},
        {'act_id': 'YBD-3', 'UID': 'YBD-3_r1_e1', 'row_UID': 'YBD-3_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'act_id': 'YBD-3', 'UID': 'YBD-3_r2_e1', 'row_UID': 'YBD-3_r2', 'acted_qty': 1.0, 'acted_sum': 1.0},
        {'act_id': 'YBD-3', 'UID': 'YBD-3_r2_e2', 'row_UID': 'YBD-3_r2', 'acted_qty': -3.0, 'acted_sum': -3.0},
        {'act_id': 'YBD-5', 'UID': 'YBD-5_r1_e1', 'row_UID': 'YBD-5_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'act_id': 'YBD-5', 'UID': 'YBD-5_r2_e1', 'row_UID': 'YBD-5_r2', 'acted_qty': 1.0, 'acted_sum': 1.0},
        {'act_id': 'YBD-5', 'UID': 'YBD-5_r2_e2', 'row_UID': 'YBD-5_r2', 'acted_qty': -3.0, 'acted_sum': -3.0},
    ]
    interim_act_rows1 = [
        {'UID': 'YBM-1_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-1_r2', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBM-3_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-3_r2', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBM-5_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-5_r2', 'acted_qty': -2.0, 'acted_sum': -2.0},
    ]
    interim_act_rows2 = [
        {'UID': 'YBD-1_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-1_r2', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBD-3_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-3_r2', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBD-5_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-5_r2', 'acted_qty': -2.0, 'acted_sum': -2.0},
    ]
    monthly_acted_events1 = [
        {'act_id': 'YBM-1', 'UID': 'YBM-1_r1_e1', 'row_UID': 'YBM-1_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'act_id': 'YBM-1', 'UID': 'YBM-1_r2_e1', 'row_UID': 'YBM-1_r2', 'acted_qty': 1.0, 'acted_sum': 1.0},
        {'act_id': 'YBM-1', 'UID': 'YBM-1_r2_e2', 'row_UID': 'YBM-1_r2', 'acted_qty': -3.0, 'acted_sum': -3.0},
        {'act_id': 'YBM-3', 'UID': 'YBM-3_r1_e1', 'row_UID': 'YBM-3_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'act_id': 'YBM-3', 'UID': 'YBM-3_r2_e1', 'row_UID': 'YBM-3_r2', 'acted_qty': 1.0, 'acted_sum': 1.0},
        {'act_id': 'YBM-3', 'UID': 'YBM-3_r2_e2', 'row_UID': 'YBM-3_r2', 'acted_qty': -3.0, 'acted_sum': -3.0},
        {'act_id': 'YBM-5', 'UID': 'YBM-5_r1_e1', 'row_UID': 'YBM-5_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'act_id': 'YBM-5', 'UID': 'YBM-5_r2_e1', 'row_UID': 'YBM-5_r2', 'acted_qty': 1.0, 'acted_sum': 1.0},
        {'act_id': 'YBM-5', 'UID': 'YBM-5_r2_e2', 'row_UID': 'YBM-5_r2', 'acted_qty': -3.0, 'acted_sum': -3.0},
    ]

    prepare_internal_tables(**locals())

    external_act_headers = [
        {'act_id': 'YBM-2', 'act_dt': ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YBD-2', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YBM-4', 'act_dt': ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YBD-4', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YBM-5-1', 'act_dt': ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
        {'act_id': 'YBD-5-1', 'act_dt': DAILY_ACT_DT.int_timestamp, 'act_sum': 10.0, 'hidden': None, 'ticket_id': None},
    ]
    external_act_rows = [
        {'UID': 'YBM-2_r1', 'act_id': 'YBM-2', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-2_r2', 'act_id': 'YBM-2', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBD-2_r1', 'act_id': 'YBD-2', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-2_r2', 'act_id': 'YBD-2', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBM-4_r1', 'act_id': 'YBM-4', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-4_r2', 'act_id': 'YBM-4', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBD-4_r1', 'act_id': 'YBD-4', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-4_r2', 'act_id': 'YBD-4', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBM-5-1_r1', 'act_id': 'YBM-5-1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-5-1_r2', 'act_id': 'YBM-5-1', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBD-5-1_r1', 'act_id': 'YBD-5-1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-5-1_r2', 'act_id': 'YBD-5-1', 'acted_qty': -2.0, 'acted_sum': -2.0},
    ]
    external_interim_act_rows = [
        {'UID': 'YBM-2_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-2_r2', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBD-2_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-2_r2', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBM-4_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-4_r2', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBD-4_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-4_r2', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBM-5-1_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-5-1_r2', 'acted_qty': -2.0, 'acted_sum': -2.0},
        {'UID': 'YBD-5-1_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-5-1_r2', 'acted_qty': -2.0, 'acted_sum': -2.0},
    ]
    external_acted_events = [
        {'UID': 'YBM-2_r1_e1', 'row_UID': 'YBM-2_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-2_r2_e1', 'row_UID': 'YBM-2_r2', 'acted_qty': 1.0, 'acted_sum': 1.0},
        {'UID': 'YBM-2_r2_e2', 'row_UID': 'YBM-2_r2', 'acted_qty': -3.0, 'acted_sum': -3.0},
        {'UID': 'YBD-2_r1_e1', 'row_UID': 'YBD-2_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-2_r2_e1', 'row_UID': 'YBD-2_r2', 'acted_qty': 1.0, 'acted_sum': 1.0},
        {'UID': 'YBD-2_r2_e2', 'row_UID': 'YBD-2_r2', 'acted_qty': -3.0, 'acted_sum': -3.0},
        {'UID': 'YBM-4_r1_e1', 'row_UID': 'YBM-4_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-4_r2_e1', 'row_UID': 'YBM-4_r2', 'acted_qty': 1.0, 'acted_sum': 1.0},
        {'UID': 'YBM-4_r2_e2', 'row_UID': 'YBM-4_r2', 'acted_qty': -3.0, 'acted_sum': -3.0},
        {'UID': 'YBD-4_r1_e1', 'row_UID': 'YBD-4_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-4_r2_e1', 'row_UID': 'YBD-4_r2', 'acted_qty': 1.0, 'acted_sum': 1.0},
        {'UID': 'YBD-4_r2_e2', 'row_UID': 'YBD-4_r2', 'acted_qty': -3.0, 'acted_sum': -3.0},
        {'UID': 'YBM-5-1_r1_e1', 'row_UID': 'YBM-5-1_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBM-5-1_r2_e1', 'row_UID': 'YBM-5-1_r2', 'acted_qty': 1.0, 'acted_sum': 1.0},
        {'UID': 'YBM-5-1_r2_e2', 'row_UID': 'YBM-5-1_r2', 'acted_qty': -3.0, 'acted_sum': -3.0},
        {'UID': 'YBD-5-1_r1_e1', 'row_UID': 'YBD-5-1_r1', 'acted_qty': 12.0, 'acted_sum': 12.0},
        {'UID': 'YBD-5-1_r2_e1', 'row_UID': 'YBD-5-1_r2', 'acted_qty': 1.0, 'acted_sum': 1.0},
        {'UID': 'YBD-5-1_r2_e2', 'row_UID': 'YBD-5-1_r2', 'acted_qty': -3.0, 'acted_sum': -3.0},
    ]
    external_unprocessed_events_full = [
        {'command_id': 3, 'UID': 'YBD-2_ue1'},
        {'command_id': 3, 'UID': 'YBD-2_ue2'},
        {'command_id': 4, 'UID': 'YBM-2_ue1'},
        {'command_id': 4, 'UID': 'YBM-2_ue2'},
        {'command_id': 4, 'UID': 'YBM-2_ue3'},
        {'command_id': 18, 'UID': 'YB-3_ue1'},
    ]
    prepare_external_tables('1', **locals())

    # context is reused
    prepare_external_tables('2', **locals())

    with yt_client.Transaction() as transaction:
        job_ = job(
            yt_client=yt_client,
            commands_meta=commands_meta,
            acts_meta=acts_meta,
            daily_acts_meta=daily_acts_meta,
            tariffed_events_stripped_table=tariffed_events_stripped_table,
            enable_consistency_checks=True,
            transaction_id=transaction.transaction_id,
        )
        result = job_.run()

    job_.set_all_meta()

    return get_result(yt_client, result)
