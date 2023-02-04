import pytest

import yatest

from maps.garden.modules.ymapsdf.lib.merge_ext import graph as merge_ext
from maps.garden.modules.ymapsdf.lib.geometry_collector import geometry_collector
from maps.garden.modules.ymapsdf.lib.geometry_collector import constants
from maps.garden.sdk import test_utils
from maps.garden.sdk.extensions import mutagen

YT_SERVER = "plato"

INPUT_TABLES = [
    "node",
    "edge",
    "ad",
    "ad_center",
    "ad_face",
    "ad_face_patch",
    "ad_geom",
    "bld",
    "bld_face",
    "bld_geom",
    "face_edge",
    "ft",
    "ft_center",
    "ft_face",
    "ft_edge",
    "ft_geom",
    "ft_rd_el",
    "rd",
    "rd_center",
    "rd_el",
    "rd_rd_el",
    "rd_geom"
]


@pytest.mark.use_local_yt_yql
def test_execution(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    # Input module
    merge_ext.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)

    geometry_collector.fill_processing_graph_for_region(
        mutagen.create_region_vendor_mutagen(
            cook.target_builder(),
            test_utils.ymapsdf.TEST_REGION,
            test_utils.ymapsdf.TEST_VENDOR))

    test_utils.ymapsdf.create_resources(
        cook,
        stage=constants.INPUT_STAGE,
        table_names=INPUT_TABLES,
        data_dir=yatest.common.test_source_path("data/input"))

    test_utils.execute(cook)

    test_utils.data.validate_data(
        environment_settings,
        YT_SERVER,
        constants.OUTPUT_STAGE,
        test_utils.ymapsdf.TEST_PROPERTIES,
        yatest.common.test_source_path("data/output"),
        constants.OUTPUT_TABLES
    )

    test_utils.ymapsdf_schema.validate(
        environment_settings,
        YT_SERVER,
        constants.OUTPUT_STAGE,
        test_utils.ymapsdf.TEST_PROPERTIES,
        constants.OUTPUT_TABLES
    )
