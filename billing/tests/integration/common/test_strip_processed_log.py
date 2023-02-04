
import pytest
from hamcrest import assert_that, contains_inanyorder

from yt.wrapper import ypath_join

from billing.library.python.logmeta_utils.meta import (
    get_log_tariff_meta,
    set_log_tariff_meta,
)
from billing.log_tariffication.py.jobs.common import strip_processed_log

from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
)
from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    NEXT_RUN_ID,
    PREV_LOG_TARIFF_META,
    CURR_LOG_TARIFF_META,
)


@pytest.mark.parametrize(
    ['sort_columns'],
    [
        pytest.param(['id'], id='sorted'),
        pytest.param([], id='unsorted'),
    ]
)
def test_strip(yt_client, yql_client, yt_root, sort_columns):
    log_dir = create_subdirectory(yt_client, yt_root, 'log_dir')
    stripped_log_dir = create_subdirectory(yt_client, yt_root, 'stripped_log_dir')

    log_table_attrs = {
        'schema': [
            {'name': 'id', 'type': 'uint64'},
            {'name': 'val', 'type': 'string'},
        ]
    }

    rows = [
        {'id': 3, 'val': '3'},
        {'id': 2, 'val': '2'},
        {'id': 1, 'val': '1'},
    ]

    log_path = ypath_join(log_dir, NEXT_RUN_ID)
    yt_client.create('table', log_path, attributes=log_table_attrs)
    yt_client.write_table(log_path, rows)
    set_log_tariff_meta(yt_client, log_path, CURR_LOG_TARIFF_META)

    # Эта таблица не должна попасть в выборку.
    other_log_path = ypath_join(log_dir, PREV_RUN_ID)
    yt_client.create('table', other_log_path, attributes=log_table_attrs)
    yt_client.write_table(other_log_path, [
        {'id': 4, 'val': '4'},
    ])
    set_log_tariff_meta(yt_client, other_log_path, PREV_LOG_TARIFF_META)

    with yt_client.Transaction() as transaction:
        stripped_log_path = strip_processed_log.run_job(
            yt_client, yql_client, CURR_LOG_TARIFF_META,
            log_dir, stripped_log_dir,
            select_columns=['id', 'val as value'],
            sort_columns=sort_columns,
            transaction_id=transaction.transaction_id,
        )

    assert get_log_tariff_meta(yt_client, stripped_log_path) == CURR_LOG_TARIFF_META

    if sort_columns:
        assert yt_client.get(ypath_join(stripped_log_path, '@sorted_by')) == sort_columns
    else:
        assert not yt_client.exists(ypath_join(stripped_log_path, '@sorted_by'))

    expected_rows = [
        {'id': 3, 'value': '3'},
        {'id': 2, 'value': '2'},
        {'id': 1, 'value': '1'},
    ]

    assert_that(list(yt_client.read_table(stripped_log_path)),
                contains_inanyorder(*expected_rows))
