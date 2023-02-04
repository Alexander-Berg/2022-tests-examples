import pytest
import yatest.common

from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import data as data_utils
from maps.garden.sdk.test_utils import ymapsdf_schema
from maps.garden.sdk.test_utils import ymapsdf

from maps.garden.modules.ymapsdf.lib.geometry_collector import geometry_collector
from maps.garden.modules.ymapsdf.lib.filter_flats import filter_flats
from maps.garden.modules.ymapsdf.lib.parent_finder import parent_finder

from maps.garden.modules.ymapsdf.lib.filter_flats import constants

YT_SERVER = "hahn"


@pytest.mark.use_local_yt(YT_SERVER)
def test_success(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    geometry_collector.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)
    parent_finder.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)

    filter_flats.fill_graph(cook.target_builder(), ymapsdf.TEST_REGIONS)

    ymapsdf.create_resources(
        cook, constants.INPUT_STAGE,
        data_dir=yatest.common.test_source_path("data/input"),
        table_names=["entrance_flat_range", "entrance_level_flat_range"])
    ymapsdf.create_resources(
        cook, constants.OUTPUT_STAGE,
        data_dir=yatest.common.test_source_path("data/input"),
        table_names=["ft_addr"])

    test_utils.execute(cook)

    data_utils.validate_data(
        environment_settings,
        YT_SERVER,
        constants.OUTPUT_STAGE,
        ymapsdf.TEST_PROPERTIES,
        yatest.common.test_source_path("data/output"),
        constants.OUTPUT_TABLES
    )

    ymapsdf_schema.validate(
        environment_settings,
        YT_SERVER,
        constants.OUTPUT_STAGE,
        ymapsdf.TEST_PROPERTIES,
        constants.OUTPUT_TABLES
    )
