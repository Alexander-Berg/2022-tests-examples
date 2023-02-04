from typing import (
    NewType,
    Dict,
    Union,
    List,
    Type,
)
import string
from datetime import (
    datetime as dt,
    date,
    timedelta as td,
)
import itertools as it
from collections import deque
import random as rnd

from hypothesis import (
    strategies as st,
    assume,
)


def random_merge(a, b):
    a = deque(a)
    b = deque(b)
    res = []
    while a or b:
        if a and b:
            source: deque = rnd.choice([a, b])
            res.append(source.popleft())
        elif a:
            res.append(a.popleft())
        else:
            res.append(b.popleft())
    return res


YTRecord = NewType(
    "YTRecord",
    Dict[str, Union[str, int, float]]
)
YTSchema = NewType(
    "YTSchema",
    List[Dict[str, str]]
)
PYSchema = NewType(
    "PYSchema",
    Dict[str, Type]
)


st.register_type_strategy(float, st.floats(allow_infinity=False, allow_nan=False))
st.register_type_strategy(int, st.integers(min_value=-(2**32), max_value=2**32))

rows = st.integers(min_value=0, max_value=10)
columns = st.integers(min_value=1, max_value=10)
shape = st.tuples(
    rows,
    columns,
)

ascii_lower = st.characters(
    whitelist_categories=(),
    whitelist_characters=string.ascii_lowercase
)

column_names = st.text(
    ascii_lower,
    min_size=1,
    max_size=20,
)

table_name = st.tuples(
    st.text(
        ascii_lower,
        min_size=1,
        max_size=4,
    ),
    st.text(
        ascii_lower,
        min_size=1,
        max_size=20
    )
).map(lambda r: "{0}.{1}".format(*r))


def fixed_size_schema(size):
    return st.dictionaries(
        keys=column_names,
        values=st.sampled_from([int, float, str]),
        min_size=size,
        max_size=size,
    )


def rows_from_schema(rows, schema: PYSchema):
    strategy = {
        k: st.from_type(v)
        for k, v in schema.items()
    }
    return st.lists(
        st.fixed_dictionaries(strategy),
        min_size=rows,
        max_size=rows
    )


ttable = {
    int: 'int64',
    float: 'double',
    str: 'utf8'
}


def yt_schema_from_py_schema(py_schema):
    return [{'name': k, 'type': ttable[v]} for k, v in py_schema.items()]


@st.composite
def rows_with_yt_schema(draw, shape=shape):
    rows, columns = draw(shape)
    schema = draw(fixed_size_schema(columns))
    concrete_rows = rows_from_schema(rows, schema)
    return draw(concrete_rows), yt_schema_from_py_schema(schema), schema

"""
simple log records
время события (датавремя),
н записей об удалении строк в каких-то месяцах. Затем м записей о вставках в каких-то месяцах.
'1487479 rows deleted between 01.02.18 and 01.03.18'
'1808525 rows inserted between 01.02.18 and 01.03.18'
время события всегда позже вставок
но мы зачем то проверяем это в коде. зачем? спросить у жень
Всё может перемежаться ошибками
"""

#
simple_log_date_formats = [
    '%Y-%m-%d %H:%M:%S',
    '%d.%m.%y %H:%M:%S',
    '%d-%m-%Y %H:%M:%S',
    '%d %b %Y %H:%M',
]

simple_log_partition_format = [
    '%Y-%m-%d',
    '%d.%m.%y',
    '%d-%b-%y',
]


@st.composite
def partition_message(draw, partition: date, keyword='inserted', is_valid=True):
    if is_valid:
        n = draw(st.integers(min_value=1, max_value=10_000_000))
    else:
        n = 0
    lpf = draw(st.sampled_from(simple_log_partition_format))
    rpf = draw(st.sampled_from(simple_log_partition_format))
    lpart = partition.strftime(lpf)
    rpart = (partition.replace(day=15) + td(days=20)).replace(day=1).strftime(rpf)
    return f"{n} rows {keyword} between {lpart} and {rpart}"


def partitions_dates(num, commit_date):
    return st.lists(
        st.dates(min_value=date(1993, 1, 1), max_value=commit_date).map(lambda x: x.replace(day=1)),
        # unique_by=lambda x: (x.year, x.month),
        unique=True,
        min_size=num,
        max_size=num,
    )


def make_message(draw, keyword='inserted', is_valid=True):
    def s(partition):
        return draw(partition_message(partition, keyword=keyword, is_valid=is_valid))
    return s


@st.composite
def simple_log_activity_block(draw):
    table = draw(table_name)
    table_r = it.repeat(table)

    insert_producer = make_message(draw)
    invalid_producer = make_message(draw, is_valid=False)
    delete_producer = make_message(draw, keyword="deleted")

    num_of_out_of_time_insertions = draw(st.integers(min_value=0, max_value=3))
    num_of_intime_insertions = draw(st.integers(min_value=0, max_value=10))
    num_of_invalid_insertions = draw(st.integers(min_value=0, max_value=5))

    assume(num_of_intime_insertions + num_of_invalid_insertions > 0)

    total_insertions = num_of_out_of_time_insertions + num_of_intime_insertions + num_of_invalid_insertions

    commit_dt: dt = draw(st.datetimes(min_value=dt(1993, 1, 1), max_value=dt(3000, 1, 1)))

    partitions: List[date] = draw(partitions_dates(total_insertions, commit_dt.date()))
    out_of_time_part = partitions[:num_of_out_of_time_insertions:]
    intime_part = partitions[num_of_out_of_time_insertions:-num_of_invalid_insertions:]
    invalid_part = partitions[-num_of_invalid_insertions::]

    num_of_deleted = draw(st.integers(min_value=0, max_value=5))

    deleted_partitions = draw(partitions_dates(
        num_of_deleted,
        commit_dt.date()
    ))
    deleted_event_dt = draw(st.lists(
        st.datetimes(max_value=commit_dt).filter(lambda x: x != commit_dt),
        min_size=num_of_deleted,
        max_size=num_of_deleted,
        unique=True,
    ).map(sorted))

    out_of_time_dt = draw(st.lists(
        st.datetimes(max_value=commit_dt).filter(lambda x: x != commit_dt),
        min_size=num_of_out_of_time_insertions,
        max_size=num_of_out_of_time_insertions,
        unique=True,
    ).map(sorted))

    intime_dt = draw(st.lists(
        st.datetimes(min_value=commit_dt),
        min_size=num_of_intime_insertions,
        max_size=num_of_intime_insertions,
        unique=True,
    ).map(sorted))

    invalid_dt = draw(st.lists(
        st.datetimes(min_value=commit_dt),
        min_size=num_of_invalid_insertions,
        max_size=num_of_invalid_insertions,
        unique=True,
    ).map(sorted))

    deleted_messages = [delete_producer(p) for p in deleted_partitions]
    deleted_records = zip(deleted_messages, deleted_event_dt, table_r)

    out_of_time_messages = [insert_producer(p) for p in out_of_time_part]
    out_of_time_records = zip(out_of_time_messages, out_of_time_dt, table_r)

    intime_messages = [insert_producer(p) for p in intime_part]
    intime_records = zip(intime_messages, intime_dt, table_r)

    invalid_messages = [invalid_producer(p) for p in invalid_part]
    invalid_records = zip(invalid_messages, invalid_dt, table_r)

    insertions_records = random_merge(intime_records, invalid_records)

    commit_event_dt = max(invalid_dt + intime_dt)
    commit_message = f'START_PREV_MON_REFR {commit_dt.strftime(rnd.choice(simple_log_date_formats))}'
    commit_records = [(commit_message, commit_event_dt, table)]

    all_records = list(it.chain(
        deleted_records,
        out_of_time_records,
        insertions_records,
        commit_records,
    ))

    return all_records, (list(zip(intime_part, intime_dt)), table)
