import pytest

import yatest

from maps.garden.modules.ymapsdf.lib.merge_ext import graph as merge_ext
from maps.garden.modules.ymapsdf.lib.geometry_collector import rd_el
from maps.garden.modules.ymapsdf.lib.geometry_collector import constants
from maps.garden.sdk import test_utils
from maps.garden.sdk.extensions import mutagen


YT_SERVER = "hahn"
INPUT_TABLES = ["rd_el"]
OUTPUT_TABLES = ["rd_el"]


@pytest.mark.use_local_yt(YT_SERVER)
def test_execution(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    merge_ext.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)

    target_builder = mutagen.create_region_vendor_mutagen(
        cook.target_builder(),
        test_utils.ymapsdf.TEST_REGION,
        test_utils.ymapsdf.TEST_VENDOR
    )
    rd_el.fill_graph(target_builder)

    test_utils.ymapsdf.create_resources(
        cook,
        stage=constants.INPUT_STAGE,
        table_names=INPUT_TABLES,
        data_dir=yatest.common.test_source_path("data/rd_el/input")
    )

    test_utils.execute(cook)

    test_utils.data.validate_data(
        environment_settings,
        YT_SERVER,
        constants.OUTPUT_STAGE,
        test_utils.ymapsdf.TEST_PROPERTIES,
        yatest.common.test_source_path("data/rd_el/output"),
        OUTPUT_TABLES
    )

    test_utils.ymapsdf_schema.validate(
        environment_settings,
        YT_SERVER,
        constants.OUTPUT_STAGE,
        test_utils.ymapsdf.TEST_PROPERTIES,
        OUTPUT_TABLES
    )
