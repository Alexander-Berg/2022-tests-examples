import pytest
from os import environ

from mapreduce.yt.python.yt_stuff import YtConfig
from datacloud.dev_utils.yt.yt_config_table import ConfigTable


environ['YT_STUFF_MAX_START_RETRIES'] = '2'


schema = [
    {'name': 'key', 'type': 'string', 'sort_order': 'ascending'},
    {'name': 'value', 'type': 'string'}
]


# Required to test DynTables
# TODO: Later move DynTables and tests to separate module
@pytest.fixture(scope='module')
def yt_config(request):
    return YtConfig(
        wait_tablet_cell_initialization=True,
    )


def test_create_config_table(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    table_path = '//test-create-config-table'
    config_table = ConfigTable(table_path, schema, yt_client)
    assert not config_table.exists(), 'ConfigTable should not exist'
    config_table.create_table()
    assert config_table.exists(), 'After creation ConfigTable should exist'


def test_insert_and_remove_records(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    table_path = '//test-config-table-insert'
    config_table = ConfigTable(table_path, schema, yt_client)
    assert not config_table.exists(), 'ConfigTable should not exist'
    config_table.create_table()
    records = list(config_table.list_records())
    assert records == [], 'New ConfigTable should be empty'

    config_table.insert_records([{'key': 'first-key', 'value': 'first-value'}]), 'Config table should not exist'
    records = list(config_table.list_records())
    assert records == [{'key': 'first-key', 'value': 'first-value'}], 'Inserted record shoud exist'

    config_table.remove_records([{'key': 'first-key'}])
    records = list(config_table.list_records())
    assert records == [], 'Record should be removed'

    config_table.insert_records_with_retry([{'key': 'second-key', 'value': 'second-value'}]), 'Config table should not exist'
    records = list(config_table.list_records())
    assert records == [{'key': 'second-key', 'value': 'second-value'}], 'Inserted record shoud exist'


def test_config_table_requests(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    table_path = '//test-config-table-requests'
    config_table = ConfigTable(table_path, schema, yt_client)
    assert not config_table.exists(), 'Config table should not exist'
    config_table.create_table()
    input_records = [
        {'key': 'first-key', 'value': 'first-value'},
        {'key': 'second-key', 'value': 'second-value'},
        {'key': 'third-key', 'value': 'third-value'},
    ]
    config_table.insert_records(input_records)

    records = list(config_table.list_records())
    assert records == input_records

    records = list(config_table.request_records('* FROM [{}]'.format(table_path)))
    assert records == input_records

    record = config_table.get_record('* FROM [{}]'.format(table_path))
    assert record == input_records[0]

    record = config_table.get_record_by_params({'key': 'second-key'})
    assert record == input_records[1]

    record = list(config_table.get_records_by_params({'key': 'second-key'}))
    assert record == [input_records[1]]


def test_update_record(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    table_path = '//test-config-table-update'
    upd_schema = [
        {'name': 'key', 'type': 'string', 'sort_order': 'ascending'},
        {'name': 'field1', 'type': 'string'},
        {'name': 'field2', 'type': 'string'},
    ]
    config_table = ConfigTable(table_path, upd_schema, yt_client)
    assert not config_table.exists(), 'ConfigTable should not exist'
    config_table.create_table()
    input_records = [
        {'key': 'first-key', 'field1': 'old-value-1', 'field2': 'old-value-2'}
    ]
    config_table.insert_records(input_records)
    record = config_table.get_record_by_params({'key': 'first-key'})
    assert record == input_records[0]

    expected_record = input_records[0]
    expected_record['field2'] = 'new-value-2'
    config_table.update_record('* FROM [{}] WHERE key="first-key"'.format(table_path), {'field2': 'new-value-2'})
    record = config_table.get_record_by_params({'key': 'first-key'})
    assert record == input_records[0]
