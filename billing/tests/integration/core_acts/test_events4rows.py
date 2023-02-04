
from hamcrest import assert_that, contains_inanyorder, has_items, has_entries
import pytest

from yt.wrapper import (
    ypath_join,
    common as yt_common,
)

from billing.log_tariffication.py.jobs.core_acts.events4rows import Job
from billing.log_tariffication.py.lib.constants import (
    RUN_ID_KEY,
    LOG_INTERVAL_KEY,
)
from billing.log_tariffication.py.lib import utils
from billing.log_tariffication.py.tests.constants import (
    OLD_LOG_INTERVAL,
    PREV_LOG_INTERVAL,
    CURR_LOG_INTERVAL,
    OLD_LOG_TARIFF_META,
    PREV_LOG_TARIFF_META,
    CURR_LOG_TARIFF_META,
    NEXT_LOG_TARIFF_META,
)
from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
)
from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
    create_yt_client,
)


ROWS_TABLE_SCHEMA = [
    {'name': 'UID', 'type': 'string'},
    {'name': 'act_id', 'type': 'string'},
]
EVENTS_PARTS_TABLE_SCHEMA = [
    {'name': 'row_UID', 'type': 'string'},
    {'name': 'UID', 'type': 'string'},
]


@pytest.fixture(name='rows_dir')
def rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'rows_dir')


@pytest.fixture(name='events_dir')
def events_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'events_dir')


@pytest.fixture(name='unprocessed_events_dir')
def unprocessed_events_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'unprocessed_events_dir')


@pytest.fixture(name='res_rows_dir')
def res_rows_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'res_rows_dir')


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


def test_locked(
    yt_client,
    yql_client,
    rows_dir,
    events_dir,
    unprocessed_events_dir,
    res_rows_dir,
):
    current_meta = CURR_LOG_TARIFF_META

    alt_yt_client = create_yt_client()
    with alt_yt_client.Transaction():
        alt_yt_client.lock(res_rows_dir)

        with pytest.raises(yt_common.YtError) as exc_info:
            with yt_client.Transaction() as transaction:
                Job(
                    yt_client,
                    yql_client,
                    current_meta,
                    rows_dir,
                    events_dir,
                    unprocessed_events_dir,
                    res_rows_dir,
                    rows_columns=[],
                    sort_columns=[],
                    transaction_id=transaction.transaction_id,
                    lock_wait_seconds=1,
                ).run()

    assert 'Timed out while waiting' in exc_info.value.message


def test_invalid_rows_columns(
    yt_client,
    yql_client,
    rows_dir,
    events_dir,
    unprocessed_events_dir,
    res_rows_dir,
):
    current_meta = CURR_LOG_TARIFF_META

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            Job(
                yt_client,
                yql_client,
                current_meta,
                rows_dir,
                events_dir,
                unprocessed_events_dir,
                res_rows_dir,
                rows_columns=['UID'],
                sort_columns=[],
                transaction_id=transaction.transaction_id,
            ).run()

    assert exc_info.value.args[0] == 'row UID is available by default as row_UID, UID from events if present'


def test_already_processed(
    yt_client,
    yql_client,
    rows_dir,
    events_dir,
    unprocessed_events_dir,
    res_rows_dir,
):
    current_meta = CURR_LOG_TARIFF_META

    create_table(yt_client, res_rows_dir, current_meta[RUN_ID_KEY], meta=current_meta)

    with yt_client.Transaction() as transaction:
        assert not Job(
            yt_client,
            yql_client,
            current_meta,
            rows_dir,
            events_dir,
            unprocessed_events_dir,
            res_rows_dir,
            rows_columns=[],
            sort_columns=[],
            transaction_id=transaction.transaction_id,
        ).run()


def test_no_unprocessed_events(
    yt_client,
    yql_client,
    rows_dir,
    events_dir,
    unprocessed_events_dir,
    res_rows_dir,
):
    current_meta = CURR_LOG_TARIFF_META

    create_table(yt_client, events_dir, current_meta[RUN_ID_KEY], meta=current_meta)

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            Job(
                yt_client,
                yql_client,
                current_meta,
                rows_dir,
                events_dir,
                unprocessed_events_dir,
                res_rows_dir,
                rows_columns=[],
                sort_columns=[],
                transaction_id=transaction.transaction_id,
            ).run()

    assert exc_info.value.args[0] == 'unprocessed_events directory is empty'


@pytest.mark.parametrize('is_processed', [False, True])
def test_future_unprocessed_events(
    yt_client,
    yql_client,
    rows_dir,
    events_dir,
    unprocessed_events_dir,
    res_rows_dir,
    is_processed,
):
    current_meta = CURR_LOG_TARIFF_META
    next_meta = NEXT_LOG_TARIFF_META

    if is_processed:
        create_table(yt_client, unprocessed_events_dir, current_meta[RUN_ID_KEY], meta=current_meta)
    create_table(yt_client, unprocessed_events_dir, next_meta[RUN_ID_KEY], meta=next_meta)
    create_table(yt_client, events_dir, current_meta[RUN_ID_KEY], meta=current_meta)

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            Job(
                yt_client,
                yql_client,
                current_meta,
                rows_dir,
                events_dir,
                unprocessed_events_dir,
                res_rows_dir,
                rows_columns=[],
                sort_columns=[],
                transaction_id=transaction.transaction_id,
            ).run()

    assert exc_info.value.args[0] == 'current run is out of order'


def test_no_rows_table(
    yt_client,
    yql_client,
    rows_dir,
    events_dir,
    unprocessed_events_dir,
    res_rows_dir,
):
    prev_meta = PREV_LOG_TARIFF_META
    current_meta = CURR_LOG_TARIFF_META

    create_table(yt_client, unprocessed_events_dir, prev_meta[RUN_ID_KEY], meta=prev_meta)
    create_table(yt_client, events_dir, current_meta[RUN_ID_KEY], meta=current_meta)

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            Job(
                yt_client,
                yql_client,
                current_meta,
                rows_dir,
                events_dir,
                unprocessed_events_dir,
                res_rows_dir,
                rows_columns=[],
                sort_columns=[],
                transaction_id=transaction.transaction_id,
            ).run()

    assert exc_info.value.args[0] == 'rows table does not exist'


@pytest.mark.parametrize('is_processed', [False, True])
def test_future_events(
    yt_client,
    yql_client,
    rows_dir,
    events_dir,
    unprocessed_events_dir,
    res_rows_dir,
    is_processed,
):
    prev_meta = PREV_LOG_TARIFF_META
    current_meta = CURR_LOG_TARIFF_META
    next_meta = NEXT_LOG_TARIFF_META

    create_table(yt_client, rows_dir, current_meta[RUN_ID_KEY], meta=current_meta)
    create_table(yt_client, unprocessed_events_dir, prev_meta[RUN_ID_KEY], meta=prev_meta)
    if is_processed:
        create_table(yt_client, events_dir, next_meta[RUN_ID_KEY], meta=next_meta)

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction() as transaction:
            Job(
                yt_client,
                yql_client,
                current_meta,
                rows_dir,
                events_dir,
                unprocessed_events_dir,
                res_rows_dir,
                rows_columns=[],
                sort_columns=[],
                transaction_id=transaction.transaction_id,
            ).run()

    assert exc_info.value.args[0] == 'Not found any intersecting intervals'


EVENTS = [
    # events_first
    [
        {'row_UID': '1', 'UID': '10'},
        {'row_UID': '1', 'UID': '11'},
        {'row_UID': '2', 'UID': '20'},
        {'row_UID': '2', 'UID': '21'},
        {'row_UID': '2', 'UID': '22'},
        {'row_UID': '7', 'UID': '70'},
        {'row_UID': '7', 'UID': '71'},
    ],
    # events_last
    [
        {'row_UID': '3', 'UID': '30'},
        {'row_UID': '3', 'UID': '31'},
        {'row_UID': '4', 'UID': '40'},
        {'row_UID': '4', 'UID': '41'},
        {'row_UID': '4', 'UID': '42'},
        {'row_UID': '8', 'UID': '80'},
        {'row_UID': '8', 'UID': '81'},
    ],
    # unprocessed_events
    [
        {'row_UID': '5', 'UID': '50'},
        {'row_UID': '5', 'UID': '51'},
        {'row_UID': '6', 'UID': '60'},
        {'row_UID': '6', 'UID': '61'},
        {'row_UID': '6', 'UID': '62'},
        {'row_UID': '9', 'UID': '90'},
        {'row_UID': '9', 'UID': '91'},
    ],
]


@pytest.mark.parametrize(
    'rows, events_first, events_last, unprocessed_events, res_rows, rows_columns, sort_columns',
    [
        pytest.param(
            # rows
            [
                {'UID': '0'},
                {'UID': '6'},
                {'UID': '1'},
                {'UID': '5'},
                {'UID': '2'},
                {'UID': '4'},
                {'UID': '3'},
            ],
            *EVENTS,
            # res_rows
            [
                {'row_UID': '1', 'UID': '10'},
                {'row_UID': '1', 'UID': '11'},
                {'row_UID': '2', 'UID': '20'},
                {'row_UID': '2', 'UID': '21'},
                {'row_UID': '2', 'UID': '22'},
                {'row_UID': '3', 'UID': '30'},
                {'row_UID': '3', 'UID': '31'},
                {'row_UID': '4', 'UID': '40'},
                {'row_UID': '4', 'UID': '41'},
                {'row_UID': '4', 'UID': '42'},
                {'row_UID': '5', 'UID': '50'},
                {'row_UID': '5', 'UID': '51'},
                {'row_UID': '6', 'UID': '60'},
                {'row_UID': '6', 'UID': '61'},
                {'row_UID': '6', 'UID': '62'},
            ],
            [],
            [],
            id='simple',
        ),
        pytest.param(
            # rows
            [
                {'UID': '0'},
                {'UID': '6'},
                {'UID': '1'},
                {'UID': '5'},
                {'UID': '2'},
                {'UID': '4'},
                {'UID': '3'},
            ],
            *EVENTS,
            # res_rows
            [
                {'row_UID': '1', 'UID': '10'},
                {'row_UID': '1', 'UID': '11'},
                {'row_UID': '2', 'UID': '20'},
                {'row_UID': '2', 'UID': '21'},
                {'row_UID': '2', 'UID': '22'},
                {'row_UID': '3', 'UID': '30'},
                {'row_UID': '3', 'UID': '31'},
                {'row_UID': '4', 'UID': '40'},
                {'row_UID': '4', 'UID': '41'},
                {'row_UID': '4', 'UID': '42'},
                {'row_UID': '5', 'UID': '50'},
                {'row_UID': '5', 'UID': '51'},
                {'row_UID': '6', 'UID': '60'},
                {'row_UID': '6', 'UID': '61'},
                {'row_UID': '6', 'UID': '62'},
            ],
            [],
            ['row_UID'],
            id='sort_columns',
        ),
        pytest.param(
            # rows
            [
                {'UID': '0', 'act_id': 'YB-0'},
                {'UID': '6', 'act_id': 'YB-3'},
                {'UID': '1', 'act_id': 'YB-1'},
                {'UID': '5', 'act_id': 'YB-2'},
                {'UID': '2', 'act_id': 'YB-1'},
                {'UID': '4', 'act_id': 'YB-2'},
                {'UID': '3', 'act_id': 'YB-2'},
            ],
            *EVENTS,
            # res_rows
            [
                {'act_id': 'YB-1', 'row_UID': '1', 'UID': '10'},
                {'act_id': 'YB-1', 'row_UID': '1', 'UID': '11'},
                {'act_id': 'YB-1', 'row_UID': '2', 'UID': '20'},
                {'act_id': 'YB-1', 'row_UID': '2', 'UID': '21'},
                {'act_id': 'YB-1', 'row_UID': '2', 'UID': '22'},
                {'act_id': 'YB-2', 'row_UID': '3', 'UID': '30'},
                {'act_id': 'YB-2', 'row_UID': '3', 'UID': '31'},
                {'act_id': 'YB-2', 'row_UID': '4', 'UID': '40'},
                {'act_id': 'YB-2', 'row_UID': '4', 'UID': '41'},
                {'act_id': 'YB-2', 'row_UID': '4', 'UID': '42'},
                {'act_id': 'YB-2', 'row_UID': '5', 'UID': '50'},
                {'act_id': 'YB-2', 'row_UID': '5', 'UID': '51'},
                {'act_id': 'YB-3', 'row_UID': '6', 'UID': '60'},
                {'act_id': 'YB-3', 'row_UID': '6', 'UID': '61'},
                {'act_id': 'YB-3', 'row_UID': '6', 'UID': '62'},
            ],
            ['act_id'],
            ['act_id', 'row_UID', 'UID'],
            id='rows_columns + sort_columns',
        ),
    ]
)
def test_run(
    yt_client,
    yql_client,
    rows_dir,
    events_dir,
    unprocessed_events_dir,
    res_rows_dir,
    rows,
    events_first,
    events_last,
    unprocessed_events,
    res_rows,
    rows_columns,
    sort_columns,
):
    old_meta = OLD_LOG_TARIFF_META.copy()
    current_meta = CURR_LOG_TARIFF_META.copy()
    interval = LogInterval.from_slices(PREV_LOG_INTERVAL.beginning, CURR_LOG_INTERVAL.end)
    current_meta[LOG_INTERVAL_KEY] = interval.to_meta()
    prev_interval = LogInterval.from_slices(OLD_LOG_INTERVAL.beginning, PREV_LOG_INTERVAL.beginning)
    old_meta[LOG_INTERVAL_KEY] = prev_interval.to_meta()

    create_table(
        yt_client, rows_dir, current_meta[RUN_ID_KEY],
        schema=ROWS_TABLE_SCHEMA, meta=current_meta, rows=rows
    )
    create_table(
        yt_client, unprocessed_events_dir, old_meta[RUN_ID_KEY],
        schema=EVENTS_PARTS_TABLE_SCHEMA, meta=old_meta, rows=unprocessed_events
    )
    create_table(
        yt_client, events_dir, PREV_LOG_TARIFF_META[RUN_ID_KEY],
        schema=EVENTS_PARTS_TABLE_SCHEMA, meta=PREV_LOG_TARIFF_META, rows=events_first
    )
    create_table(
        yt_client, events_dir, CURR_LOG_TARIFF_META[RUN_ID_KEY],
        schema=EVENTS_PARTS_TABLE_SCHEMA, meta=CURR_LOG_TARIFF_META, rows=events_last
    )

    with yt_client.Transaction() as transaction:
        res_rows_table = Job(
            yt_client,
            yql_client,
            current_meta,
            rows_dir,
            events_dir,
            unprocessed_events_dir,
            res_rows_dir,
            rows_columns=rows_columns,
            sort_columns=sort_columns,
            transaction_id=transaction.transaction_id,
        ).run()

    if sort_columns:
        assert_that(
            list(yt_client.read_table(res_rows_table)),
            has_items(*[has_entries(row) for row in res_rows]),
        )
    else:
        assert_that(
            list(yt_client.read_table(res_rows_table)),
            contains_inanyorder(*res_rows),
        )
