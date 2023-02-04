import os.path
from unittest import mock
import pytest
import shutil
import yatest.common

from maps.masstransit.configs.feeds.config import FeedsConfig

from maps.garden.sdk.core import Version
from maps.garden.sdk import test_utils

from maps.garden.sdk.test_utils import ymapsdf_schema
from maps.garden.sdk.test_utils import ymapsdf

from maps.garden.modules.ymapsdf.lib.merge_stops import constants

from maps.garden.modules.merge_masstransit.lib import merge_masstransit
from maps.garden.modules.ymapsdf.lib.merge_poi import graph as merge_poi
from maps.garden.modules.ymapsdf.lib.merge_stops import merge_stops


YT_SERVER = "plato"


GTFS_FEEDS_CONFIG = {
    "regions": ["canberra"],
}


def _prepare_merged_masstransit_data(cook):
    resource = cook.create_input_resource("merged_masstransit_data")
    resource.version = Version(properties={"release": "0.0.0-0"})
    resource.load_environment_settings(cook.environment_settings)

    merged_masstransit_data_path = yatest.common.source_path(
        "maps/garden/modules/ymapsdf/tests/merge_stops/data/merged_masstransit_data_0.0.0-0")

    # We can't use `shutil.copytree` here
    # because it requires the destination dir not to exist
    for filename in os.listdir(merged_masstransit_data_path):
        shutil.copyfile(
            os.path.join(merged_masstransit_data_path, filename),
            os.path.join(resource.path(), filename))

    resource.logged_commit()
    resource.calculate_size()


@pytest.mark.use_local_yt_yql
@mock.patch("maps.garden.sdk.yt.geobase.get_geobase5", return_value="")  # use standard location for unittests
@mock.patch("maps.masstransit.configs.feeds.config.FeedsConfig", return_value=FeedsConfig(GTFS_FEEDS_CONFIG))
def test_success(gtfs_mock, geobase_mock, environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    merge_poi.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)
    merge_masstransit.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)

    merge_stops.fill_graph(cook.target_builder(), ymapsdf.TEST_REGIONS)

    ymapsdf.create_resources(
        cook,
        stage=constants.INPUT_STAGE,
        data_dir=yatest.common.test_source_path("data/ymapsdf_tables_json"),
        table_names=constants.INPUT_TABLES)

    _prepare_merged_masstransit_data(cook)

    test_utils.execute(cook)

    ymapsdf.validate_data(
        environment_settings,
        constants.YT_OUTPUT_PHASE,
        yatest.common.test_source_path("data/ymapsdf_tables_expected"),
        constants.OUTPUT_TABLES
    )

    ymapsdf_schema.validate(
        environment_settings,
        YT_SERVER,
        constants.YT_OUTPUT_PHASE,
        ymapsdf.TEST_PROPERTIES,
        constants.OUTPUT_TABLES
    )
