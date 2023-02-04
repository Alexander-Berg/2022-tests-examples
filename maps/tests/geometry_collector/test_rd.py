import pytest

import yatest
from maps.garden.sdk.core import AutotestsFailedError

from maps.garden.modules.ymapsdf.lib.merge_ext import graph as merge_ext
from maps.garden.modules.ymapsdf.lib.geometry_collector import rd
from maps.garden.modules.ymapsdf.lib.geometry_collector import constants
from maps.garden.sdk import test_utils
from maps.garden.sdk.extensions import mutagen

from . import common

INPUT_TABLES = [
    "rd",
    "rd_el",
    "rd_rd_el",
    "rd_geom",
]
OUTPUT_TABLES = [
    "rd_geom"
]


@pytest.mark.use_local_yt_yql
def test_execution(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    merge_ext.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)

    target_builder = mutagen.create_region_vendor_mutagen(
        cook.target_builder(),
        test_utils.ymapsdf.TEST_REGION,
        test_utils.ymapsdf.TEST_VENDOR
    )
    rd.fill_graph(target_builder)

    test_utils.ymapsdf.create_resources(
        cook,
        stage=constants.INPUT_STAGE,
        table_names=INPUT_TABLES,
        data_dir=yatest.common.test_source_path("data/rd/input")
    )

    test_utils.execute(cook)

    test_utils.data.validate_data(
        environment_settings,
        common.YT_SERVER,
        constants.OUTPUT_STAGE,
        test_utils.ymapsdf.TEST_PROPERTIES,
        yatest.common.test_source_path("data/rd/output"),
        OUTPUT_TABLES
    )

    test_utils.ymapsdf_schema.validate(
        environment_settings,
        common.YT_SERVER,
        constants.OUTPUT_STAGE,
        test_utils.ymapsdf.TEST_PROPERTIES,
        OUTPUT_TABLES
    )


@pytest.mark.use_local_yt_yql
def test_compute_rd_geom_task_error(test_task_executor):
    create_ymapsdf = test_task_executor.create_ymapsdf_input_yt_table_resource
    create_custom = test_task_executor.create_custom_input_yt_table_resource
    with pytest.raises(AutotestsFailedError, match=r'https://npro.maps.yandex.ru/#!/objects/3 '):
        test_task_executor.execute_final_task(
            task=rd.ComputeRdGeomTask(),
            input_resources={
                "rd_in": create_ymapsdf("rd"),
                "stacked_rd_el_tmp_in": create_custom("stacked_rd_el_tmp"),
                "rd_geom_in": create_ymapsdf("rd_geom"),
            },
            output_resources={
                "rd_geom_out": test_task_executor.create_yt_table_resource("rd_geom"),
            },
        )
