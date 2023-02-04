import json

from maps.garden.sdk.core import Resource
from maps.garden.sdk.module_rpc.proto import resource_pb2 as resource_proto

from maps.garden.sdk.yt.yt_file import YtFileResource
from maps.garden.sdk.yt.yt_directory import YtDirectoryResource
from maps.garden.sdk.yt.yt_source_path import YtSourcePathResource
from maps.garden.sdk.yt.yt_table import YtTableResource
from maps.garden.sdk.yt.proto import resource_pb2 as yt_resource_proto

YT_TABLE_SCHEMA = [
    {"name": "id", "type": "int64", "required": True},
    {"name": "data", "type": "string", "required": True},
]


def test_there_and_back_again_yt_table():
    yt_table = YtTableResource(
        name="test_yt_table_resource",
        path_template="//some/cypress/folder"
    )

    proto = yt_table.to_proto()
    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(yt_resource_proto.ytTable)

    assert proto.name == yt_table.name
    assert proto.state == resource_proto.Resource.State.NOT_CREATED
    assert proto.Extensions[yt_resource_proto.ytTable].ypathTemplate == yt_table._path_template

    decoded_yt_table = Resource.from_proto(proto)
    assert isinstance(decoded_yt_table, YtTableResource)
    assert decoded_yt_table._path_template == yt_table._path_template
    assert decoded_yt_table.name == yt_table.name
    assert decoded_yt_table._key_columns is None
    assert decoded_yt_table._unique_keys is None
    assert decoded_yt_table._attributes == yt_table._attributes


def test_there_and_back_again_yt_table_with_all_arguments():
    yt_table = YtTableResource(
        name="test_yt_table_resource",
        path_template="//some/cypress/folder",
        key_columns=["id"],
        unique_keys=True,
        schema=YT_TABLE_SCHEMA
    )

    proto = yt_table.to_proto()
    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(yt_resource_proto.ytTable)

    assert proto.name == yt_table.name
    assert proto.state == resource_proto.Resource.State.NOT_CREATED
    assert proto.Extensions[yt_resource_proto.ytTable].ypathTemplate == yt_table._path_template
    assert proto.Extensions[yt_resource_proto.ytTable].keyColumns == ["id"]
    assert proto.Extensions[yt_resource_proto.ytTable].uniqueKeys
    assert proto.Extensions[yt_resource_proto.ytTable].ytSchema == json.dumps(YT_TABLE_SCHEMA)

    decoded_yt_table = Resource.from_proto(proto)
    assert isinstance(decoded_yt_table, YtTableResource)
    assert decoded_yt_table._path_template == yt_table._path_template
    assert decoded_yt_table.name == yt_table.name
    assert decoded_yt_table._key_columns == yt_table._key_columns
    assert isinstance(decoded_yt_table._key_columns, list)
    assert decoded_yt_table._unique_keys == yt_table._unique_keys
    assert decoded_yt_table._attributes == yt_table._attributes


def test_there_and_back_again_yt_file():
    yt_file = YtFileResource(
        name="test_yt_file_resource",
        filename_template="//other/cypress/folder"
    )

    proto = yt_file.to_proto()
    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(yt_resource_proto.ytFile)

    assert proto.name == yt_file.name
    assert proto.state == resource_proto.Resource.State.NOT_CREATED
    assert proto.Extensions[yt_resource_proto.ytFile].ypathTemplate == yt_file._filename_template

    decoded_yt_file = Resource.from_proto(proto)
    assert isinstance(decoded_yt_file, YtFileResource)
    assert decoded_yt_file._filename_template == yt_file._filename_template
    assert decoded_yt_file.name == yt_file.name


def test_there_and_back_again_yt_directory():
    yt_dir = YtDirectoryResource(
        name="test_yt_dir_resource",
        filename_template="//another/cypress/folder"
    )

    proto = yt_dir.to_proto()
    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(yt_resource_proto.ytDirectory)

    assert proto.name == yt_dir.name
    assert proto.state == resource_proto.Resource.State.NOT_CREATED
    assert proto.Extensions[yt_resource_proto.ytDirectory].ypathTemplate == yt_dir._filename_template

    decoded_yt_dir = Resource.from_proto(proto)
    assert isinstance(decoded_yt_dir, YtDirectoryResource)
    assert decoded_yt_dir._filename_template == yt_dir._filename_template
    assert decoded_yt_dir.name == yt_dir.name


def test_there_and_back_again_yt_source_path():
    yt_path = YtSourcePathResource(
        name="test_yt_dir_resource",
        server="any_yt_cluster_will_be_accepted"
    )

    proto = yt_path.to_proto()
    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(yt_resource_proto.ytSourcePath)

    assert proto.name == yt_path.name
    assert proto.Extensions[yt_resource_proto.ytSourcePath].cluster == yt_path.server

    decoded_yt_path = Resource.from_proto(proto)
    assert isinstance(decoded_yt_path, YtSourcePathResource)
    assert decoded_yt_path.name == yt_path.name
