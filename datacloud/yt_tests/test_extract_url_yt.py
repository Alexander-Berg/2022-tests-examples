from yt.wrapper import TablePath
from datacloud.features.cluster import extract_url
from datacloud.features.cluster.path_config import PathConfig
from datacloud.dev_utils.yt import yt_utils
from datacloud.dev_utils.yt.yt_utils import ypath_join


def test_input_log_tables_prod(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    date = '2020-01-01'
    path_config = PathConfig()
    tables = extract_url.get_input_log_tables(path_config, yt_client, date=date)
    expected = [
        ypath_join(path_config.external_logs_dir, 'watch_log_tskv/2020-01-01'),
        ypath_join(path_config.external_logs_dir, 'spy_log/2020-01-01'),
    ]
    assert tables == expected


def test_input_log_tables_retro(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    folder = '//test/cluster/test-input-log-tables-retro'
    watch_log = ypath_join(folder, 'watch_log_tskv')
    spy_log = ypath_join(folder, 'spy_log')
    yt_utils.create_folders([folder, watch_log, spy_log], yt_client)
    tables_to_create = [
        ypath_join(watch_log, '2020-01-01'),
        ypath_join(watch_log, '2020-01-02'),
        ypath_join(spy_log, '2020-01-01'),
        ypath_join(spy_log, 'table-that-will-be-included'),
    ]
    for table in tables_to_create:
        yt_client.create(type='table', path=table)
    path_config = PathConfig(is_retro=True)
    path_config.external_logs_dir = folder
    tables = extract_url.get_input_log_tables(path_config, yt_client)
    assert set(tables) == set(tables_to_create)


def test_reduce_counter(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    folder = '//test/cluster/reduce-counter'
    yt_utils.create_folders([folder], yt_client)
    input_table = TablePath(ypath_join(folder, 'input_table'), sorted_by=['key'])
    output_table = ypath_join(folder, 'output_table')
    yt_client.write_table(
        input_table,
        [
            {'key': 'key1', 'counter': 1},
            {'key': 'key1', 'counter': 2},
            {'key': 'key1', 'counter': 3},
            {'key': 'key2', 'counter': 42},
            {'key': 'key2', 'counter': 56},
        ],
    )
    yt_client.run_reduce(extract_url.reduce_counter, input_table, output_table,
                         reduce_by='key')
    actual = list(yt_client.read_table(output_table))
    expected = [
        {'key': 'key1', 'counter': 6},
        {'key': 'key2', 'counter': 98},
    ]
    actual = set(tuple(sorted(it.items())) for it in actual)
    expected = set(tuple(sorted(it.items())) for it in expected)
    assert actual == expected


def test_date_filter(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    folder = '//test/cluster/date-filter'
    input_dates_table = TablePath(ypath_join(folder, 'input_dates'),
                                  sorted_by=['key'])
    input_data_table = TablePath(ypath_join(folder, 'input_data'),
                                  sorted_by=['key'])
    output_table = ypath_join(folder, 'output')
    yt_utils.create_folders([folder], yt_client)
    yt_client.write_table(
        input_dates_table,
        [{'key': 'key1', 'timestamp': 1577836800}]  # 2020-01-01 00:00:00
    )
    good_recs = [
        {'key': 'key1', 'timestamp': 1577836100, 'val': 1},
        {'key': 'key1', 'timestamp': 1577780000, 'val': 3},
    ]
    bad_recs = [
        {'key': 'key1', 'timestamp': 1577600000, 'val': 2},  # too old
        {'key': 'key1', 'timestamp': 1577900000, 'val': 4},  # too new
    ]
    yt_client.write_table(
        input_data_table,
        good_recs + bad_recs
    )

    yt_client.run_reduce(
        extract_url.DateFilter(days_to_take=1),
        [input_dates_table, input_data_table],
        output_table,
        reduce_by='key',
    )
    actual = list(yt_client.read_table(output_table))
    actual = set(tuple(sorted(it.items())) for it in actual)
    expected = set(tuple(sorted(it.items())) for it in good_recs)
    assert actual == expected
