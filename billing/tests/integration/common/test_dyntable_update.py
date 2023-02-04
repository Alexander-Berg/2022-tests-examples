# -*- coding: utf-8 -*-

from unittest import mock

import pytest

from yt.wrapper import (
    ypath_join,
    common as yt_common,
    YtClient,
)

from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)
from billing.library.python import yt_utils

from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
    LOG_INTERVAL_KEY,
    DYN_TABLE_IS_UPDATING_KEY,
)
from billing.log_tariffication.py.jobs.common import dyntable_update

from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
    create_yt_client,
    create_dyntable,
)
from billing.log_tariffication.py.tests.constants import (
    CURR_RUN_ID,
    PREV_LOG_INTERVAL,
    CURR_LOG_INTERVAL,
    NEXT_LOG_INTERVAL,
)


TABLE_SCHEMA = [
    {'name': 'OrderID', 'type': 'int64', 'sort_order': 'ascending'},
    {'name': 'CurrencyID', 'type': 'uint64', 'sort_order': 'ascending'},
    {'name': 'Amount', 'type': 'int64'},
]

TABLE_WITH_PREV_SCHEMA = TABLE_SCHEMA + [{'name': '__prev_Amount', 'type': 'int64'}]


@pytest.fixture(name='main_table_path')
def main_table_path_fixture(yt_root):
    return ypath_join(yt_root, 'main_table')


@pytest.fixture(name='update_table_dir')
def update_table_dir_fixture(yt_client, yt_root):
    return create_subdirectory(yt_client, yt_root, 'update_table')


@pytest.fixture(name='update_table_path')
def update_table_path_fixture(update_table_dir):
    return ypath_join(update_table_dir, CURR_RUN_ID)


def list2table_data(data, schema=None):
    if schema is None:
        schema = TABLE_SCHEMA

    table_data = []
    for values in data:
        table_data.append({column['name']: values[i] for i, column in enumerate(schema)})

    return table_data


def init_dyntable(yt_client, path, log_interval, data, is_updating=False):
    create_dyntable(
        yt_client,
        path,
        TABLE_SCHEMA,
        list2table_data(data),
        {
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: log_interval.to_meta(),
                DYN_TABLE_IS_UPDATING_KEY: is_updating,
            },
        }
    )


def create_table(yt_client, path, log_interval, data=None, schema=None):
    if schema is None:
        schema = TABLE_SCHEMA

    yt_client.create(
        'table',
        path,
        attributes={
            LOG_TARIFF_META_ATTR: {LOG_INTERVAL_KEY: log_interval.to_meta()},
            'schema': schema
        }
    )
    if data:
        yt_client.write_table(path, list2table_data(data, schema))


def get_result(yt_client, path):
    return {
        'meta': yt_client.get(ypath_join(path, '@' + LOG_TARIFF_META_ATTR)),
        'rows': list(yt_client.read_table(path)),
    }


@pytest.mark.parametrize(
    ['enable_dynamic_store_read'],
    [
        pytest.param(True, id='enable_dynamic_store_read=True'),
        pytest.param(False, id='enable_dynamic_store_read=False'),
    ]
)
def test_new(yt_client, main_table_path, update_table_path, enable_dynamic_store_read):
    init_dyntable(
        yt_client,
        main_table_path,
        CURR_LOG_INTERVAL,
        [
            (1, 1, 10),
            (1, 2, 66),
            (2, 1, 42),
        ]
    )
    create_table(
        yt_client,
        update_table_path,
        NEXT_LOG_INTERVAL,
        [
            (1, 1, 11),
            (2, 2, 7),
            (3, 1, 97),
        ]
    )

    with yt_client.Transaction():
        dyntable_update.run_job(
            yt_client,
            main_table_path,
            update_table_path,
            batch_size=2,
            enable_dynamic_store_read=enable_dynamic_store_read,
        )

    tablets_state = yt_utils.yt.get_node_attr(yt_client, main_table_path, 'tablet_state')
    enable_dynamic_store_read_value = yt_utils.yt.dynamic_store_read_enabled(yt_client, main_table_path)
    if enable_dynamic_store_read:
        assert tablets_state == 'mounted'
        assert enable_dynamic_store_read_value
    else:
        assert tablets_state == 'frozen'
        assert not enable_dynamic_store_read_value
    return get_result(yt_client, main_table_path)


@pytest.mark.parametrize(
    'update_interval, is_updating',
    [
        pytest.param(CURR_LOG_INTERVAL, True, id='in_progress'),
        pytest.param(CURR_LOG_INTERVAL, False, id='completed'),
        pytest.param(PREV_LOG_INTERVAL, False, id='previous'),
    ]
)
def test_procession_retries(yt_client, main_table_path, update_table_path, update_interval, is_updating):
    init_dyntable(yt_client, main_table_path, CURR_LOG_INTERVAL, [(1, 1, 10)], is_updating=is_updating)
    create_table(yt_client, update_table_path, update_interval, [(1, 1, 11)])

    with yt_client.Transaction():
        dyntable_update.run_job(
            yt_client,
            main_table_path,
            update_table_path,
        )

    return get_result(yt_client, main_table_path)


def test_force_update(yt_client, main_table_path, update_table_path):
    init_dyntable(yt_client, main_table_path, CURR_LOG_INTERVAL, [(1, 1, 10)])
    create_table(yt_client, update_table_path, CURR_LOG_INTERVAL, [(1, 1, 11)])

    with yt_client.Transaction():
        dyntable_update.run_job(
            yt_client,
            main_table_path,
            update_table_path,
            force=True
        )

    return get_result(yt_client, main_table_path)


@pytest.mark.parametrize(
    'table_interval, update_interval, is_updating',
    [
        pytest.param(
            LogInterval([Subinterval('c', 't', 0, 10, 20)]),
            LogInterval([Subinterval('c', 't', 0, 21, 30)]),
            False,
            id='new_ahead'
        ),
        pytest.param(
            LogInterval([Subinterval('c', 't', 0, 10, 20)]),
            LogInterval([Subinterval('c', 't', 0, 19, 30)]),
            False,
            id='new_intersecting'
        ),
        pytest.param(
            LogInterval([Subinterval('c', 't', 0, 10, 20)]),
            LogInterval([Subinterval('c', 't', 0, 20, 30)]),
            True,
            id='in_progress_following'
        ),
        pytest.param(
            LogInterval([Subinterval('c', 't', 0, 10, 20)]),
            LogInterval([Subinterval('c', 't', 0, 9, 20)]),
            True,
            id='in_progress_intersecting'
        ),
    ]
)
def test_interval_mismatch(yt_client, main_table_path, update_table_path,
                           table_interval, update_interval, is_updating):
    init_dyntable(yt_client, main_table_path, table_interval, [(1, 1, 10)], is_updating=is_updating)
    create_table(yt_client, update_table_path, update_interval, [(1, 1, 11)])

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction():
            dyntable_update.run_job(
                yt_client,
                main_table_path,
                update_table_path,
            )

    assert exc_info.value.args[0] == "New log interval mismatch"

    return get_result(yt_client, main_table_path)


def test_update_fail(yt_client, main_table_path, update_table_path):
    init_dyntable(yt_client, main_table_path, CURR_LOG_INTERVAL, [(1, 1, 10)])
    create_table(yt_client, update_table_path, NEXT_LOG_INTERVAL, [(1, 1, 666)])

    mock_path = 'billing.log_tariffication.py.jobs.common.dyntable_update.update_table'
    with mock.patch(mock_path, side_effect=AssertionError('kthulhu fhtagn')):
        with pytest.raises(AssertionError) as exc_info:
            with yt_client.Transaction():
                dyntable_update.run_job(
                    yt_client,
                    main_table_path,
                    update_table_path,
                )

    assert exc_info.value.args[0] == 'kthulhu fhtagn'

    return get_result(yt_client, main_table_path)


def test_main_lock(yt_client, yt_root, main_table_path, update_table_path):
    init_dyntable(yt_client, main_table_path, CURR_LOG_INTERVAL, [(1, 1, 10)])
    create_table(yt_client, update_table_path, NEXT_LOG_INTERVAL, [(1, 1, 666)])

    alt_yt_client = create_yt_client()
    with alt_yt_client.Transaction():
        alt_yt_client.lock(
            yt_root,
            mode='shared',
            child_key='main_table'
        )

        with pytest.raises(yt_common.YtError) as exc_info:
            with yt_client.Transaction():
                dyntable_update.run_job(
                    yt_client,
                    main_table_path,
                    update_table_path,
                    lock_wait_seconds=1
                )

        assert 'Timed out while waiting' in exc_info.value.message

    return get_result(yt_client, main_table_path)


def test_meta_lock(yt_client, main_table_path, update_table_path):
    init_dyntable(yt_client, main_table_path, CURR_LOG_INTERVAL, [(1, 1, 10)])
    create_table(yt_client, update_table_path, NEXT_LOG_INTERVAL, [(1, 1, 666)])

    alt_yt_client = create_yt_client()
    with alt_yt_client.Transaction():
        alt_yt_client.lock(main_table_path)

        with pytest.raises(yt_common.YtError) as exc_info:
            with yt_client.Transaction():
                dyntable_update.run_job(
                    yt_client,
                    main_table_path,
                    update_table_path,
                    lock_wait_seconds=1
                )

        assert 'Timed out while waiting' in exc_info.value.message

    return get_result(yt_client, main_table_path)


def test_empty_interval(yt_client, main_table_path, update_table_path):
    init_dyntable(yt_client, main_table_path, CURR_LOG_INTERVAL, [(1, 1, 10)])
    empty_interval = LogInterval.from_slices(CURR_LOG_INTERVAL.end, CURR_LOG_INTERVAL.end)
    create_table(yt_client, update_table_path, empty_interval)

    with yt_client.Transaction():
        dyntable_update.run_job(
            yt_client,
            main_table_path,
            update_table_path,
        )

    return get_result(yt_client, main_table_path)


def test_ensure_dynamic_store_read_enabled(yt_client: YtClient, main_table_path):
    assert not yt_utils.yt.dynamic_store_read_enabled(yt_client, main_table_path)
    init_dyntable(yt_client, main_table_path, CURR_LOG_INTERVAL, [])
    yt_client.unmount_table(main_table_path)
    # Зачем здесь транзакция - см. комментарий внутри ensure_dynamic_store_read_enabled
    with yt_client.Transaction():
        dyntable_update.ensure_dynamic_store_read_enabled(yt_client, main_table_path)
    assert yt_utils.yt.dynamic_store_read_enabled(yt_client, main_table_path)


def test_tablet_states(yt_client, main_table_path):
    """
    Possible (known/valid) states:
        unmounting, unmounted, mounting, mounted, freezing, frozen

    Forbidden actions:
        * cannot freeze (directly) or unfreeze unmounted table
        * cannot mount frozen table
        * (sometimes) cannot mount(freeze=True) a mounted table
        * yql cannot read from unmounted table

    As far as I can see, all commands are idempotent.
    """

    def get_state():
        return yt_client.get(main_table_path + '/@tablet_state')

    init_dyntable(yt_client, main_table_path, CURR_LOG_INTERVAL, [(1, 1, 10)])
    assert get_state() == 'frozen'

    # remount and freeze already frozen table
    yt_client.mount_table(main_table_path, sync=True, freeze=True)
    assert get_state() == 'frozen'

    # freeze already frozen table
    yt_client.freeze_table(main_table_path, sync=True)
    assert get_state() == 'frozen'

    # double unfreeze
    yt_client.unfreeze_table(main_table_path, sync=True)
    yt_client.unfreeze_table(main_table_path, sync=True)
    assert get_state() == 'mounted'

    # double mount unfrozen
    yt_client.mount_table(main_table_path, sync=True)
    yt_client.mount_table(main_table_path, sync=True)
    assert get_state() == 'mounted'

    # freeze mounted table, double freeze
    yt_client.freeze_table(main_table_path, sync=True)
    yt_client.freeze_table(main_table_path, sync=True)
    assert get_state() == 'frozen'

    # double unmount
    yt_client.unmount_table(main_table_path, sync=True)
    yt_client.unmount_table(main_table_path, sync=True)
    assert get_state() == 'unmounted'


def test_ignore_columns(yt_client, main_table_path, update_table_path):
    init_dyntable(
        yt_client,
        main_table_path,
        CURR_LOG_INTERVAL,
        [
            (1, 1, 10),
            (1, 2, 66),
            (2, 1, 42),
        ]
    )
    create_table(
        yt_client,
        update_table_path,
        NEXT_LOG_INTERVAL,
        data=[
            (1, 1, 11, 10),
            (2, 2, 7, 66),
            (3, 1, 97, 42),
        ],
        schema=TABLE_WITH_PREV_SCHEMA
    )

    with yt_client.Transaction():
        dyntable_update.run_job(
            yt_client,
            main_table_path,
            update_table_path,
            batch_size=2,
            read_columns=['ID', 'Version', 'Amount']
        )

    result = list(yt_client.read_table(main_table_path))

    assert '__prev_Amount' not in result[0]
