import pytest

import yatest

from maps.garden.modules.ymapsdf.lib.geometry_collector import geometry_collector
from maps.garden.modules.ymapsdf.lib.translation import translate as translation
from maps.garden.modules.ymapsdf.lib.parent_finder import parent_finder
from maps.garden.modules.ymapsdf.lib.parent_finder import constants
from maps.garden.sdk import test_utils
from maps.garden.sdk.extensions import mutagen


YT_SERVER = "plato"


def create_region_builder(builder):
    return mutagen.create_region_vendor_mutagen(
        builder,
        test_utils.ymapsdf.TEST_REGION,
        test_utils.ymapsdf.TEST_VENDOR,
    )


@pytest.mark.use_local_yt_yql
def test_execution(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    # Input modules
    geometry_collector.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)
    translation.fill_processing_graph_for_region(create_region_builder(cook.input_builder()))

    parent_finder.fill_processing_graph_for_region(create_region_builder(cook.target_builder()))

    tables_settings = [
        {
            "stage": constants.INPUT_STAGE,
            "table_names": [
                "ad", "ad_nm", "ad_geom", "ad_excl",
                "addr", "addr_range",
                "bld", "bld_geom", "bld_addr",
                "ft", "ft_ad", "ft_geom", "ft_type",
                "ft_center", "ft_face", "ft_edge", "ft_addr",
                "locality",
                "node",
                "rd", "rd_el", "rd_rd_el"
            ],
        },
        {
            "stage": constants.OUTPUT_STAGE,
            "table_names": ["ft_nm"],
        },
    ]

    for settings in tables_settings:
        test_utils.ymapsdf.create_resources(
            cook,
            data_dir=yatest.common.test_source_path("data/input"),
            **settings
        )

    test_utils.execute(cook)

    expected_data_path = yatest.common.test_source_path("data/output")

    test_utils.data.validate_data(
        environment_settings,
        YT_SERVER,
        constants.OUTPUT_STAGE,
        test_utils.ymapsdf.TEST_PROPERTIES,
        expected_data_path,
        constants.YT_OUTPUT_TABLES
    )

    test_utils.ymapsdf_schema.validate(
        environment_settings,
        YT_SERVER,
        constants.OUTPUT_STAGE,
        test_utils.ymapsdf.TEST_PROPERTIES,
        constants.YT_OUTPUT_TABLES
    )
