import yt.wrapper as yt

from maps.garden.libs.geocoder.export.defs import resource_names as geocoder_rn
from maps.garden.sdk.core import Version
from maps.garden.sdk.test_utils import data


def create_tables(geocoder_dir_resource, data_path):
    yt_client = geocoder_dir_resource.get_yt_client()

    def _create_table(path, schema):
        yt_table_path = yt.ypath_join(geocoder_dir_resource.path, path)
        yt_client.create("table", yt_table_path, attributes={"schema": schema}, recursive=True)
        yt_client.write_table(yt_table_path, data.read_table_from_file(data_path + "/input", path))

    toponyms_schema = data.read_table_from_file(data_path + "/schema", "toponyms")
    recognition_schema = data.read_table_from_file(data_path + "/schema", "recognition_map")
    uri_schema = data.read_table_from_file(data_path + "/schema", "uri")

    _create_table("toponyms", toponyms_schema)
    _create_table("pov/recognition_map", recognition_schema)
    _create_table("pov/001/uri", uri_schema)
    _create_table("pov/BY/uri", uri_schema)
    _create_table("pov/UA/uri", uri_schema)


def prepare_data(cook, yt_server, data_dir):
    geocoder_dir_resource = cook.create_input_resource(geocoder_rn.GEOSRC_YT)
    geocoder_dir_resource.server = yt_server
    geocoder_dir_resource.version = Version(
        properties={
            "release": "0-0-0"
        }
    )
    geocoder_dir_resource.load_environment_settings(cook.environment_settings)
    create_tables(geocoder_dir_resource, data_dir + "/geocoder_export")
