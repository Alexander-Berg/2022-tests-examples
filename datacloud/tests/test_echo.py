import yatest.common as yatest_common

from mapreduce.yt.python.yt_stuff import YtConfig, YtStuff


def mapper(rec):
    yield rec


def test_local_yt():
    config = YtConfig(
        local_cypress_dir=yatest_common.source_path("datacloud/dev_utils/yt/tests/cypress_dir")
    )

    stuff = YtStuff(config)
    stuff.start_local_yt()
    yt_client = stuff.get_yt_client()
    yt_client.write_table('//new-table', [{'a': '1'}, {'b': '2'}])
    yt_client.run_map(
        mapper,
        '//new-table',
        '//other-table'
    )
    assert yt_client.exists('//other-table'), 'Table not found :('
    assert yt_client.row_count('//other-table') == 2, 'Wrong number of lines'
    stuff.stop_local_yt()


def test_local_cypress():
    config = YtConfig(
        local_cypress_dir=yatest_common.source_path("datacloud/dev_utils/yt/tests/cypress_dir")
    )
    stuff = YtStuff(config)
    stuff.start_local_yt()
    yt_client = stuff.get_yt_client()
    assert yt_client.exists('//sample/table'), 'Table should exist'
    stuff.stop_local_yt()
