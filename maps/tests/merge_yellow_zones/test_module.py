import pytest
import yatest.common

from yt.wrapper.ypath import ypath_dirname

from maps.garden.sdk.core import Version

from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import data as data_utils
from maps.garden.sdk.test_utils import ymapsdf_schema
from maps.garden.sdk.test_utils import ymapsdf

from maps.garden.modules.yellow_zones_bundle.lib import graph as yellow_zones_bundle
from maps.garden.modules.ymapsdf.lib.merge_poi import graph as merge_poi
from maps.garden.modules.ymapsdf.lib.merge_yellow_zones import merge_yellow_zones

from maps.garden.modules.ymapsdf.lib.merge_yellow_zones import constants

from . import data

YT_SERVER = "hahn"


def _prepare_yellow_zones(cook, yt_client):
    YELLOW_ZONES_PATH = "//home/maps/poi/yellow_zones/export_data/19.08.29-0/shapes"

    yellow_zones_resource = cook.create_input_resource("yellow_zones_shapes")
    yellow_zones_resource.version = Version(properties={
        "release_name": "2019.08.29",
        "yt_path": YELLOW_ZONES_PATH
    })

    yt_client.create("map_node", ypath_dirname(YELLOW_ZONES_PATH), recursive=True)
    yt_client.write_table(YELLOW_ZONES_PATH, data.YELLOW_ZONES_YT_DATA)


@pytest.mark.use_local_yt(YT_SERVER)
def test_success(environment_settings, yt_client):
    cook = test_utils.GraphCook(environment_settings)

    yellow_zones_bundle.fill_graph(cook.input_builder())
    merge_poi.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)

    merge_yellow_zones.fill_graph(cook.target_builder(), ymapsdf.TEST_REGIONS)

    ymapsdf.create_resources(
        cook, constants.INPUT_STAGE,
        data_dir=yatest.common.test_source_path("input"),
        table_names=constants.INPUT_TABLES)

    _prepare_yellow_zones(cook, yt_client)

    test_utils.execute(cook)

    data_utils.validate_data(
        environment_settings,
        YT_SERVER,
        constants.YT_OUTPUT_PHASE,
        ymapsdf.TEST_PROPERTIES,
        yatest.common.test_source_path("output"),
        constants.OUTPUT_TABLES
    )

    ymapsdf_schema.validate(
        environment_settings,
        YT_SERVER,
        constants.YT_OUTPUT_PHASE,
        ymapsdf.TEST_PROPERTIES,
        constants.OUTPUT_TABLES
    )
