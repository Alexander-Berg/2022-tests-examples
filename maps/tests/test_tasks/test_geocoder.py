import pytest
import yatest.common

from maps.garden.sdk.core import Version
from maps.garden.sdk.yt import YtTableResource, YtDirectoryResource

from maps.garden.sdk.test_utils import ymapsdf, data
from maps.garden.modules.renderer_denormalization.defs import resource_names as rn
from maps.garden.modules.renderer_denormalization.lib import const, tasks
from maps.garden.modules.renderer_denormalization.tests.lib import geocoder


GEOCODER_DATA_PATH = yatest.common.source_path("maps/renderer/denormalization/test_data/geocoder_export")


@pytest.mark.use_local_yt("hahn")
def test_geocoder_merge_task(environment_settings):
    geocoder_dir_resource = YtDirectoryResource(
        name="geocoder_dir",
        filename_template="//tmp/geocoder_export",
        server="hahn")
    geocoder_dir_resource.version = Version()
    geocoder_dir_resource.load_environment_settings(environment_settings)
    geocoder.create_tables(geocoder_dir_resource, GEOCODER_DATA_PATH)

    output_resource = YtTableResource(
        rn.DENORMALIZATION_STAGE.resource_name(const.GEOCODER_URI_TABLE_NAME),
        rn.DENORMALIZATION_STAGE.make_yt_path(const.GEOCODER_URI_TABLE_NAME),
        schema=const.GEOCODER_URI_SCHEMA,
        key_columns=const.GEOCODER_URI_KEY_COLUMNS
    )
    output_resource.version = Version(properties=ymapsdf.TEST_PROPERTIES)
    output_resource.load_environment_settings(environment_settings)

    task = tasks.MergeGeocoderUrisTask()
    task.load_environment_settings(environment_settings)
    task(ymapsdf_resource=geocoder_dir_resource, source_dir=geocoder_dir_resource, sink_table=output_resource)
    output_resource.logged_commit()

    assert list(output_resource.read_table()) == data.read_table_from_file(GEOCODER_DATA_PATH + "/output", const.GEOCODER_URI_TABLE_NAME)
