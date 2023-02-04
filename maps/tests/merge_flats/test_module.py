import pytest
import yatest.common

from yt.wrapper.ypath import ypath_dirname

from maps.garden.sdk.core import Version
from maps.garden.sdk.yt import utils as yt_utils

from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import data as data_utils
from maps.garden.sdk.test_utils import ymapsdf_schema
from maps.garden.sdk.test_utils import ymapsdf

from maps.garden.modules.flats_bundle.lib import graph as flats_bundle
from maps.garden.modules.ymapsdf.lib.merge_poi import graph as merge_poi
from maps.garden.modules.ymapsdf.lib.merge_flats import merge_flats

from maps.garden.modules.ymapsdf.lib.merge_flats import constants

from . import data

YT_SERVER = "hahn"


def _prepare_flats(cook):
    FLATS_PATH_TEMPLATE = "//home/maps/poi/flats/export_data/2019-08-29/{}"
    FLATS_INFO = {
        "entrance_flat_range": data.FLATS_YT_DATA,
        "entrance_level_flat_range": data.FLATS_LEVEL_YT_DATA,
    }

    for table, table_data in FLATS_INFO.items():
        flats_path = FLATS_PATH_TEMPLATE.format(table)

        flats_resource = cook.create_input_resource(table)
        flats_resource.version = Version(properties={
            "release_name": "2019-08-29",
            "yt_path": flats_path
        })

        yt_client = yt_utils.get_yt_client(
            yt_utils.get_server_settings(
                yt_utils.get_yt_settings(cook.environment_settings),
                server=YT_SERVER))
        yt_client.create("map_node", ypath_dirname(flats_path), recursive=True, ignore_existing=True)
        yt_client.write_table(flats_path, table_data)


@pytest.mark.use_local_yt("hahn")
def test_success(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    flats_bundle.fill_graph(cook.input_builder())
    merge_poi.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)

    merge_flats.fill_graph(cook.target_builder(), ymapsdf.TEST_REGIONS)

    ymapsdf.create_resources(
        cook, constants.INPUT_STAGE,
        data_dir=yatest.common.test_source_path("input"),
        table_names=constants.INPUT_TABLES)

    _prepare_flats(cook)

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
