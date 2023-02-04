import pytest

import yatest
from maps.garden.sdk.core import AutotestsFailedError

from maps.garden.modules.ymapsdf.lib.merge_ext import graph as merge_ext
from maps.garden.modules.ymapsdf.lib.geometry_collector import ft
from maps.garden.modules.ymapsdf.lib.geometry_collector import constants
from maps.garden.sdk import test_utils
from maps.garden.sdk.extensions import mutagen

from . import common


INPUT_TABLES = [
    "edge",
    "ft",
    "ft_center",
    "ft_edge",
    "ft_face",
    "ft_geom",
    "ft_rd_el",
    "face_edge",
    "node",
    "rd_el",
]
OUTPUT_TABLES = [
    "ft_geom"
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
    ft.fill_graph(target_builder)

    test_utils.ymapsdf.create_resources(
        cook,
        stage=constants.INPUT_STAGE,
        table_names=INPUT_TABLES,
        data_dir=yatest.common.test_source_path("data/ft/input")
    )

    test_utils.execute(cook)

    test_utils.data.validate_data(
        environment_settings,
        common.YT_SERVER,
        constants.OUTPUT_STAGE,
        test_utils.ymapsdf.TEST_PROPERTIES,
        yatest.common.test_source_path("data/ft/output"),
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
def test_compute_ft_geom_task_error(test_task_executor):
    create_ymapsdf = test_task_executor.create_ymapsdf_input_yt_table_resource
    create_custom = test_task_executor.create_custom_input_yt_table_resource
    with pytest.raises(AutotestsFailedError, match=r'https://npro.maps.yandex.ru/#!/objects/3 '):
        test_task_executor.execute_final_task(
            task=ft.ComputeFtGeomTask(),
            input_resources={
                "ft_in": create_ymapsdf("ft"),
                "stacked_ft_node_tmp_in": create_custom("stacked_ft_node_tmp"),
                "stacked_ft_edge_tmp_in": create_custom("stacked_ft_edge_tmp"),
                "stacked_ft_face_tmp_in": create_custom("stacked_ft_face_tmp"),
                "stacked_ft_rd_el_tmp_in": create_custom("stacked_ft_rd_el_tmp"),
                "ft_geom_in": create_ymapsdf("ft_geom")
            },
            output_resources={
                "ft_geom_out": test_task_executor.create_yt_table_resource("ft_geom"),
            },
        )
