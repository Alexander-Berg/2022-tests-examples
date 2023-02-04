import os
import pytest

from maps.garden.sdk.core import Version
from maps.garden.sdk.yt.yql_tools import YqlTask, YqlError
from maps.garden.sdk.yt.yt_table import YtTableResource


YT_CLUSTER = "plato"


def _create_dummy_table(environment_settings):
    table = YtTableResource(
        name="test_dummy_table",
        path_template="/test_dummy_table",
        server=YT_CLUSTER)
    table.version = Version()
    table.load_environment_settings(environment_settings)
    return table


@pytest.mark.use_local_yt_yql
def test_yql_task(environment_settings):
    task = YqlTask("SELECT 1;")
    task.load_environment_settings(environment_settings)
    table = _create_dummy_table(environment_settings)
    task(table=table)


@pytest.mark.use_local_yt_yql
def test_yql_lib_attachment(environment_settings):
    lib_text = "$test = 1; export $test;"
    query_text = "PRAGMA library('lib.sql'); IMPORT lib SYMBOLS $test; SELECT 1 + $test"
    task = YqlTask(query_text, attach_files_data={"lib.sql": lib_text})
    task.load_environment_settings(environment_settings)
    table = _create_dummy_table(environment_settings)
    task(table=table)


@pytest.mark.use_local_yt_yql
def test_yql_task_incorrect_query(environment_settings):
    task = YqlTask("absolutely incorrect query")
    task.load_environment_settings(environment_settings)
    table = _create_dummy_table(environment_settings)
    with pytest.raises(YqlError):
        task(table=table)


@pytest.mark.use_local_yt_yql
def test_yt_tables(environment_settings):
    YT_TABLE_SCHEMA = [
        {"name": "id", "type": "int64", "required": True},
        {"name": "data", "type": "string", "required": True},
    ]

    TABLE_DATA = [
        {"id": 1, "data": "foo"},
        {"id": 2, "data": "bar"}
    ]

    src_table = YtTableResource(
        name="test_src_table",
        path_template="/test_src_table",
        server=YT_CLUSTER,
        schema=YT_TABLE_SCHEMA)
    src_table.version = Version(properties={"region": "test_region"})
    src_table.load_environment_settings(environment_settings)
    src_table.write_table(TABLE_DATA, raw=False)
    src_table.logged_commit()
    src_table.calculate_size()

    dst_table = YtTableResource(
        name="test_dst_table",
        path_template="/test_dst_table",
        schema=YT_TABLE_SCHEMA)
    dst_table.version = Version(properties={"region": "test_region"})
    dst_table.load_environment_settings(environment_settings)

    os.environ["YT_SPEC"] = '{"annotations":{"key":"value"}}'

    task = YqlTask("""
INSERT INTO `{dst_table}` (id, data)
SELECT id * 2, data || data
FROM `{src_table}`
    """)
    task.load_environment_settings(environment_settings)
    task(src_table=src_table, dst_table=dst_table)

    assert src_table.server == dst_table.server

    dst_table_data = list(dst_table.read_table())

    assert dst_table_data == [
        {"id": 2, "data": "foofoo"},
        {"id": 4, "data": "barbar"}
    ]


@pytest.mark.use_local_yt_yql
def test_filtering_resources(environment_settings):
    task = YqlTask("DROP TABLE `{yt_table}`;")
    task.load_environment_settings(environment_settings)
    table = _create_dummy_table(environment_settings)
    task(yt_table=table, other="some_other")
