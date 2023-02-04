import pytest
from os import environ
import yt.wrapper as yt_wrapper

from datacloud.dev_utils.yt import yt_utils
from mapreduce.yt.python.yt_stuff import YtConfig


environ['YT_STUFF_MAX_START_RETRIES'] = '2'


# Required to test DynTables
# TODO: Later move DynTables and tests to separate module
@pytest.fixture(scope='module')
def yt_config(request):
    return YtConfig(
        wait_tablet_cell_initialization=True,
    )


def test_create_folders(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    folders = ['//test/first', '//test/second', '//and/another/one']
    for folder in folders:
        assert not yt_client.exists(folder), 'Foulder should not exist now'
    yt_utils.create_folders(folders, yt_client)
    for folder in folders:
        assert yt_client.exists(folder), 'Folder should exist'


def test_create_single_folder(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    folder = '//test/single-folder'
    assert not yt_client.exists(folder), 'Foulder shoud not exist now'
    yt_utils.create_folders(folder, yt_client)
    assert yt_client.exists(folder), 'Folder should exist now'


def test_check_table_exists(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    table_path = '//tmp/check-esist-table'
    assert not yt_utils.check_table_exists(table_path, yt_client),  \
        'Table should not exist now'
    yt_client.create('table', table_path)
    assert not yt_utils.check_table_exists(table_path, yt_client),  \
        'Empty table should be marked as unexisting'
    yt_client.write_table(table_path, [{'x': 1}, {'x': 2}, {'x': 3}])
    assert yt_utils.check_table_exists(table_path, yt_client),  \
        'Table should exist'


def test_get_k_last_tables(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    folder = '//test-get-k-last'
    yt_utils.create_folders([folder, ], yt_client)
    assert yt_utils.get_k_last_tables(folder, 1, yt_client) == [], \
        'Should be empty'
    dates = ['2018-01-02', '2018-01-03', '2018-01-04']
    for date in dates:
        yt_client.create('table', yt_wrapper.ypath_join(folder, date))
    assert yt_utils.get_k_last_tables(folder, 1, yt_client) == [
        yt_wrapper.ypath_join(folder, '2018-01-04')], 'Should return latest'
    assert yt_utils.get_k_last_tables(folder, 2, yt_client) == [
        yt_wrapper.ypath_join(folder, '2018-01-03'),
        yt_wrapper.ypath_join(folder, '2018-01-04')], 'Should return 2 latest'


def test_remove_table(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    table = '//table-remove-table'
    yt_client.create('table', table)
    assert yt_client.exists(table), 'Table should exist'
    yt_utils.remove_table(table, yt_client)
    assert not yt_client.exists(table), 'Table should be removed'


def test_create_dyn_table(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    table_path = '//test-dyn-table'
    assert not yt_client.exists(table_path), 'DynTable should not exist'

    schema = [
        {'name': 'key', 'type': 'string', 'sort_order': 'ascending'},
        {'name': 'value', 'type': 'string'}
    ]
    yt_utils.DynTable.create_table(table_path, schema, yt_client)
    assert yt_client.exists(table_path), 'DynTable should exist'


def test_dyn_table_usage(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    table_path = '//test-dyn-table-usage'
    assert not yt_client.exists(table_path), 'DynTable should not exist'
    schema = [
        {'name': 'key', 'type': 'string', 'sort_order': 'ascending'},
        {'name': 'value', 'type': 'string'}
    ]
    yt_utils.DynTable.create_table(table_path, schema, yt_client)
    rows = list(yt_utils.DynTable.get_rows_from_table(table_path, {'key': 'first-key'}, yt_client))
    assert rows == [], 'Request to empty table should return empty response'

    yt_utils.DynTable.insert_row(table_path, yt_client, [
        {'key': 'first-key', 'value': 'first-value'},
        {'key': 'second-key', 'value': 'second-value'}
    ])
    rows = list(yt_utils.DynTable.get_rows_from_table(table_path, {'key': 'first-key'}, yt_client))
    assert rows == [{'key': 'first-key', 'value': 'first-value'}], 'Inserted record shoud exist'

    yt_utils.DynTable.remove_row(table_path, yt_client, [
        {'key': 'first-key'},
        {'key': 'non-existing-key'}
    ])
    rows = list(yt_utils.DynTable.get_rows_from_table(table_path, {'key': 'first-key'}, yt_client))
    assert rows == [], 'After record was remove it shoud not exist'

    yt_utils.DynTable.remove_table(table_path, yt_client)
    assert not yt_client.exists(table_path), 'Removed DynTable should not exist'
