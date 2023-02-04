# coding=utf-8

import datetime
from typing import List, Any, Iterable, Optional  # noqa

from yt.yson import to_yson_type

from billing.library.python.logfeller_utils.log_interval import LogInterval, Subinterval
from billing.library.python.logfeller_utils.log_interval import (
    LB_META_ATTR, get_next_stream_table_name, STREAM_TABLE_NAME_FORMATS, STREAM_5MIN
)


def generate_tables(intervals, first_table_name=None):
    # type: (Iterable[LogInterval], Optional[str]) -> List[Any]
    tables = []

    if first_table_name is None:
        first_table_name = generate_stream_log_table_name()
    table_name = first_table_name
    for interval in intervals:
        tables.append(to_yson_type(
            table_name,
            attributes={
                LB_META_ATTR: interval.to_meta()
            }
        ))
        table_name = get_next_stream_table_name(table_name)
    return tables


def generate_stream_log_table_name(dt=None):
    dt = dt or datetime.datetime.now()
    return dt.replace(second=0, minute=0).strftime(STREAM_TABLE_NAME_FORMATS[STREAM_5MIN])


def mk_interval(start_idx, end_idx):
    return LogInterval([Subinterval('a', 'a', 0, start_idx, end_idx)])
