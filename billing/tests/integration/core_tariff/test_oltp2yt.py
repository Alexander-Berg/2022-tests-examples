
import pytest
from hamcrest import contains_inanyorder, contains, assert_that

from yt.wrapper import ypath_join, TablePath

from billing.log_tariffication.py.jobs.core_tariff import oltp2yt
from billing.library.python.logmeta_utils.meta import (
    get_log_tariff_meta,
    set_log_tariff_meta,
)

from billing.log_tariffication.py.tests.constants import (
    CURR_RUN_ID, PREV_RUN_ID, STREAM_LOG_TABLE_SCHEMA,
    CURR_LOG_TARIFF_META, PREV_LOG_TARIFF_META, NEXT_RUN_ID,
    NEXT_LOG_TARIFF_META
)
from billing.log_tariffication.py.tests.utils import (
    check_node_is_locked
)

SCHEMA = [
    {'name': 'id', 'type': 'uint64'},
    {'name': 'ts', 'type': 'uint64'},
]


@pytest.mark.parametrize(
    ['with_checks', 'sort_by'],
    [
        pytest.param(True, ['id', 'ts'], id='with checks and sort_by'),
        pytest.param(False, None, id='without checks and sort_by'),
    ]
)
def test_write(yt_client, yt_root, with_checks, sort_by):
    rows = [
        {'id': 2, 'ts': 4},
        {'id': 2, 'ts': 1},
        {'id': 1, 'ts': 5},
    ]

    if sort_by:
        rows_matcher = contains(*reversed(rows))
    else:
        rows_matcher = contains_inanyorder(*rows)

    prev_table_path = ypath_join(yt_root, PREV_RUN_ID)
    yt_client.create('table', prev_table_path)
    set_log_tariff_meta(yt_client, prev_table_path, PREV_LOG_TARIFF_META)

    next_table_path = ypath_join(yt_root, NEXT_RUN_ID)
    yt_client.create('table', next_table_path)
    set_log_tariff_meta(yt_client, next_table_path, NEXT_LOG_TARIFF_META)

    table_path = ypath_join(yt_root, CURR_RUN_ID)

    if not with_checks:
        # Must be ignored and deleted
        yt_client.create('table', table_path, attributes={
            'schema': STREAM_LOG_TABLE_SCHEMA
        })
        set_log_tariff_meta(yt_client, table_path, PREV_LOG_TARIFF_META)

    with yt_client.Transaction(ping=False):
        assert oltp2yt.run_job(
            yt_client, yt_root, rows, SCHEMA, CURR_LOG_TARIFF_META,
            sort_by=sort_by,
            with_checks=with_checks
        ) == table_path
        check_node_is_locked(yt_root)

    assert get_log_tariff_meta(yt_client, table_path) == CURR_LOG_TARIFF_META

    actual_schema = [
        {'name': col['name'], 'type': col['type']}
        for col in yt_client.get(ypath_join(table_path, '@schema'))
    ]
    assert actual_schema == SCHEMA

    if sort_by:
        assert yt_client.get(ypath_join(table_path, '@sorted_by')) == sort_by

    assert_that(list(yt_client.read_table(table_path)), rows_matcher)


@pytest.mark.usefixtures('yt_transaction')
def test_already_done(yt_client, yt_root, caplog):
    rows = [
        {'id': 1, 'ts': 1},
        {'id': 2, 'ts': 2},
    ]

    table_path = TablePath(
        ypath_join(yt_root, CURR_RUN_ID),
        schema=SCHEMA
    )

    yt_client.create('table', table_path,
                     attributes={'schema': SCHEMA})
    set_log_tariff_meta(yt_client, table_path, CURR_LOG_TARIFF_META)
    yt_client.write_table(table_path, rows)

    assert oltp2yt.run_job(
        yt_client, yt_root, [{'id': 3, 'ts': 3}],
        SCHEMA, CURR_LOG_TARIFF_META,
    ) == table_path

    assert_that(
        list(yt_client.read_table(table_path)),
        contains_inanyorder(*rows)
    )

    assert "Already done" in caplog.text


@pytest.mark.parametrize(
    ['table_name', 'table_meta'],
    [
        (CURR_RUN_ID, PREV_LOG_TARIFF_META),
        (PREV_RUN_ID, CURR_LOG_TARIFF_META),
        (NEXT_RUN_ID, PREV_LOG_TARIFF_META),
    ]
)
@pytest.mark.usefixtures('yt_transaction')
def test_checks(yt_client, yt_root, table_name, table_meta):
    table_path = ypath_join(yt_root, table_name)
    yt_client.create('table', table_path)
    set_log_tariff_meta(yt_client, table_path, table_meta)
    with pytest.raises(AssertionError):
        oltp2yt.run_job(
            yt_client, yt_root, [],
            SCHEMA, CURR_LOG_TARIFF_META,
        )
