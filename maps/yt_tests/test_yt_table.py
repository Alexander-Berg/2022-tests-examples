import copy
import datetime
import pytest

import yt.yson
import yt.wrapper as yt
from yt.wrapper.ypath import ypath_join

from maps.garden.sdk.core import Version

from maps.garden.sdk.yt import YtTableResource

YT_SERVER = "hahn"

TABLE_DATA = [
    {"id": 3, "data": "aaa"},
    {"id": 1, "data": "bbb"},
    {"id": 4, "data": "ccc"},
    {"id": 2, "data": "ddd"}
]

YT_TABLE_SCHEMA = [
    {"name": "id", "type": "int64", "required": True},
    {"name": "data", "type": "string", "required": True},
]


@pytest.mark.use_local_yt(YT_SERVER)
def test_table_resource(environment_settings):
    resource = YtTableResource(
        "test_table",
        path_template="/{prop}/table",
        server=YT_SERVER)
    resource.version = Version(properties={'prop': "123"})
    resource.load_environment_settings(environment_settings)

    tmp_dir = environment_settings["yt_servers"]["hahn"]["tmp_dir"]

    assert resource.path.startswith(tmp_dir)

    resource.write_table(TABLE_DATA)

    assert resource.get_yt_client().exists(ypath_join(resource.path, "@expiration_time"))

    resource.logged_commit()
    resource.calculate_size()
    resource.clean()

    prefix = environment_settings["yt_servers"]["hahn"]["prefix"]

    assert resource.path == prefix + "/123/table"
    assert resource.physically_exists

    assert list(resource.read_table()) == TABLE_DATA

    assert not resource.get_yt_client().exists(ypath_join(resource.path, "@expiration_time"))

    data_size = resource.get_yt_client().get(
        ypath_join(resource.path, "@uncompressed_data_size"))
    assert data_size > 0
    assert resource.size["bytes"] == data_size

    # Test resource copying

    another_prefix = "//home/another_prefix"
    another_environment_settings = copy.deepcopy(environment_settings)
    another_environment_settings["yt_servers"]["hahn"]["prefix"] = another_prefix
    another_environment_settings["yt_servers"]["hahn"]["tmp_dir"] = another_prefix + "/tmp"

    # Test resource removal

    resource.remove()
    assert not resource.physically_exists


@pytest.mark.use_local_yt(YT_SERVER)
def test_table_special_read_write(environment_settings):
    resource = YtTableResource(
        name="table_read_write",
        path_template="/table_read_write",
        server=YT_SERVER)
    resource.version = Version()
    resource.load_environment_settings(environment_settings)

    resource.write_table(TABLE_DATA, append=True)

    resource.logged_commit()
    resource.calculate_size()

    expected_data = [{"id": 3}, {"id": 1}, {"id": 4}, {"id": 2}]
    assert list(resource.read_table(columns=["id"])) == expected_data


@pytest.mark.use_local_yt(YT_SERVER)
def test_sort_table(environment_settings):
    table = YtTableResource(
        name="test_table",
        path_template="/test_table",
        server="hahn",
        key_columns=["id"],
        schema=YT_TABLE_SCHEMA)
    table.version = Version()
    table.load_environment_settings(environment_settings)
    table.write_table(TABLE_DATA)

    yt_client = table.get_yt_client()

    assert not yt_client.is_sorted(table.path)

    table.logged_commit()

    assert yt_client.is_sorted(table.path)

    expected_data = copy.copy(TABLE_DATA)
    expected_data.sort(key=lambda row: row["id"])
    assert list(yt_client.read_table(table.path)) == expected_data

    key_columns = yt_client.get(ypath_join(table.path, "@key_columns"))
    assert key_columns == table.key_columns

    optimize_for = yt_client.get(ypath_join(table.path, "@optimize_for"))
    assert optimize_for == "scan"

    assert table.last_id() == 4

    result_schema = table.schema()
    assert result_schema[0]["sort_order"] == "ascending"
    assert not result_schema.attributes["unique_keys"]

    unsorted_schema = table.schema_without_key_columns()
    assert "sort_order" not in unsorted_schema[0]
    assert not unsorted_schema.attributes["unique_keys"]


@pytest.mark.use_local_yt(YT_SERVER)
def test_table_schema(environment_settings):
    table = YtTableResource(
        name="test_table_schema",
        path_template="/test_table_schema",
        server="hahn")
    table.version = Version()
    table.load_environment_settings(environment_settings)

    table.set_schema(YT_TABLE_SCHEMA)
    table.write_table(TABLE_DATA)

    table.logged_commit()

    # We cann't compare these two schemas directly
    # because YT adds some special attributes to columns

    result_schema_columns = {column["name"] for column in table.schema()}
    unsorted_schema_columns = {column["name"] for column in table.schema_without_key_columns()}
    expected_schema_columns = {column["name"] for column in YT_TABLE_SCHEMA}

    assert result_schema_columns == expected_schema_columns
    assert unsorted_schema_columns == expected_schema_columns


@pytest.mark.use_local_yt(YT_SERVER)
def test_table_user_attributes(environment_settings):
    user_attributes = {"user_attribute1": "value1"}

    table = YtTableResource(
        name="test_table_user_attributes",
        path_template="/test_table_user_attributes",
        server="hahn")
    table.version = Version()
    table.load_environment_settings(environment_settings)
    table.update_attributes(user_attributes)
    table.write_table([])
    table.logged_commit()

    yt_client = table.get_yt_client()

    new_table = YtTableResource(
        name="test_table_user_attributes_2",
        path_template="/test_table_user_attributes_2",
        server="hahn")
    new_table.version = Version()
    new_table.load_environment_settings(environment_settings)
    # copy user attributes from another table
    new_table.transfer_attributes_from(table, ["user_attribute1"])
    # set a completely new user attribute
    yt_client.set(ypath_join(new_table.path, "@user_attribute2"), "value2")
    new_table.write_table([])
    new_table.logged_commit()

    assert yt_client.get(ypath_join(new_table.path, "@user_attribute1")) == "value1"
    assert yt_client.get(ypath_join(new_table.path, "@user_attribute2")) == "value2"


@pytest.mark.use_local_yt(YT_SERVER)
def test_table_chunk_merge(environment_settings):
    table = YtTableResource(
        name="test_table_chunk_merge",
        path_template="/test_table_chunk_merge",
        server="hahn")
    table.version = Version()
    table.load_environment_settings(environment_settings)

    table.write_table([{"id": 1}])  # First chunk
    table.write_table([{"id": 2}], append=True)  # Second chunk

    chunk_count = table.get_yt_client().get(ypath_join(table.path, "@chunk_count"))
    assert chunk_count == 2

    table.logged_commit()

    chunk_count = table.get_yt_client().get(ypath_join(table.path, "@chunk_count"))
    assert chunk_count == 1


@pytest.mark.use_local_yt(YT_SERVER)
def test_table_with_initial_unique_keys(environment_settings):
    table = YtTableResource(
        name="test_table_with_initial_unique_keys",
        path_template="/test_table_with_initial_unique_keys",
        server="hahn")
    table.version = Version()
    table.load_environment_settings(environment_settings)

    schema = yt.yson.YsonList([
        {"name": "id", "type": "int64", "required": True, "sort_order": "ascending"},
        {"name": "data", "type": "string", "required": True},
    ])
    schema.attributes["unique_keys"] = True

    table.set_schema(schema)

    with pytest.raises(yt.errors.YtHttpResponseError):
        table.write_table([
            {"id": 1, "data": "foo"},
            {"id": 1, "data": "foo"}
        ])

    table.write_table([
        {"id": 1, "data": "foo"},
        {"id": 2, "data": "bar"}
    ])

    table.logged_commit()

    result_schema = table.schema()
    assert result_schema.attributes["unique_keys"]


@pytest.mark.use_local_yt(YT_SERVER)
def test_table_make_unique_keys_good_data(environment_settings):
    table = YtTableResource(
        name="test_table_make_unique_keys_good_data",
        path_template="/test_table_make_unique_keys_good_data",
        server="hahn")
    table.version = Version()
    table.load_environment_settings(environment_settings)

    table.set_schema([
        {"name": "id", "type": "int64", "required": True},
        {"name": "data", "type": "string", "required": True},
    ])
    table.key_columns = ["id"]
    table.unique_keys = True

    table.write_table([
        {"id": 1, "data": "foo"},
        {"id": 2, "data": "bar"}
    ])

    table.logged_commit()

    result_schema = table.schema()
    assert result_schema.attributes["unique_keys"]


@pytest.mark.use_local_yt(YT_SERVER)
def test_table_make_unique_keys_bad_data(environment_settings):
    table = YtTableResource(
        name="test_table_make_unique_keys_bad_data",
        path_template="/test_table_make_unique_keys_bad_data",
        server="hahn")
    table.version = Version()
    table.load_environment_settings(environment_settings)

    table.set_schema([
        {"name": "id", "type": "int64", "required": True},
        {"name": "data", "type": "string", "required": True},
    ])
    table.key_columns = ["id"]
    table.unique_keys = True

    table.write_table([
        {"id": 1, "data": "foo"},
        {"id": 1, "data": "foo"}
    ])

    with pytest.raises(yt.errors.YtOperationFailedError):
        table.logged_commit()


@pytest.mark.use_local_yt(YT_SERVER)
def test_expiration_time(environment_settings):
    expiration_time = datetime.datetime.utcnow() + datetime.timedelta(hours=1)

    table_resource = YtTableResource(
        name="test_expiration_table",
        path_template="/test_expiration_table",
        server=YT_SERVER)
    table_resource.version = Version()
    table_resource.load_environment_settings(environment_settings)
    table_resource.expiration_time = expiration_time
    # Accessing 'path' attribute actually creates a temporary table if it does
    # not yet exist.
    table_resource.path
    table_resource.logged_commit()

    prefix = environment_settings["yt_servers"]["hahn"]["prefix"]
    assert table_resource.get_yt_client().exists(
        prefix + "/test_expiration_table/@expiration_time")
