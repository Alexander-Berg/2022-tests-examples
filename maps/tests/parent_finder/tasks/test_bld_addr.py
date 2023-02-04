import json
import os.path
import pytest

from maps.garden.libs.ymapsdf.lib.parent_finder import bld_addr
from maps.garden.libs.ymapsdf.lib.parent_finder import build_grid
from maps.garden.modules.ymapsdf.lib.parent_finder import bld_addr_final
from maps.garden.modules.ymapsdf.lib.parent_finder import parent_finder

from maps.garden.modules.ymapsdf.lib.parent_finder.bld_addr import (
    PrepareBldComplexPartsTask, CollectBldComplexGeometryTask, DistributeBldByTilesTask
)


@pytest.mark.use_local_yt_yql
def test_collect_bld_complex_geom(test_task_executor):
    result = test_task_executor.execute_task(
        task=PrepareBldComplexPartsTask(),
        input_resources={
            "bld": test_task_executor.create_ymapsdf_input_yt_table_resource("bld"),
            "bld_geom": test_task_executor.create_ymapsdf_input_yt_table_resource("bld_geom"),
        },
        output_resources={
            "bld_complex_parts": test_task_executor.create_yt_table_resource("bld_complex_parts"),
        },
    )

    test_task_executor.execute_final_task(
        task=CollectBldComplexGeometryTask(),
        input_resources={
            "bld_complex_parts": result["bld_complex_parts"],
        },
        output_resources={
            "bld_complex_geom": test_task_executor.create_yt_table_resource("bld_complex_geom"),
        },
    )


@pytest.mark.use_local_yt("hahn")
def test_distribute_bld_by_tiles(test_task_executor):
    result = test_task_executor.execute_task(
        task=DistributeBldByTilesTask(),
        input_resources={
            "bld": test_task_executor.create_ymapsdf_input_yt_table_resource("bld"),
            "bld_geom": test_task_executor.create_ymapsdf_input_yt_table_resource("bld_geom"),
            "bld_complex_geom": test_task_executor.create_custom_input_yt_table_resource("bld_complex_geom"),
        },
        output_resources={
            "bld_tiles": test_task_executor.create_yt_table_resource("bld_tiles"),
        },
    )

    # After a geometry is converted from Geodetic spatial reference to Mercator projection
    # numbers start looking ugly.
    # There is no sense to test geo-to-mercator conversion here.
    # Therefore `shape` and `buffered_shape` fields are dropped from the test.

    with open(os.path.join(test_task_executor.data_path_output, "bld_tiles.jsonl")) as f:
        expected_data = [json.loads(row) for row in f]

    result_data = []
    for row in result["bld_tiles"].read_table():
        del row["shape"]
        result_data.append(row)
    result_data.sort(key=lambda row: (row["tile_x"], row["tile_y"], row["bld_id"]))

    assert result_data == expected_data


@pytest.mark.use_local_yt("hahn")
def test_find_near_bld_and_addr(test_task_executor):
    result = test_task_executor.execute_task(
        task=build_grid.BuildAddrGrid(),
        input_resources={
            "addr_geom_tmp_in": test_task_executor.create_custom_input_yt_table_resource("addr_geom_tmp"),
        },
        output_resources={
            "addr_grid_yt_file_out": test_task_executor.create_coverage_resource(
                parent_finder.ADDR_GRID_RESOURCE_NAME,
                "doesnt_matter"),
        },
    )

    test_task_executor.execute_final_task(
        task=bld_addr.FindNearBldAndAddr(),
        input_resources={
            "bld_tiles": test_task_executor.create_custom_input_yt_table_resource("bld_tiles"),
            "addr_grid": result["addr_grid_yt_file_out"]
        },
        output_resources={
            "addr_nearest_bld": test_task_executor.create_yt_table_resource("addr_nearest_bld"),
            "noaddr_bld_touching": test_task_executor.create_yt_table_resource("noaddr_bld_touching"),
        },
    )


@pytest.mark.use_local_yt("hahn")
def test_compute_bld_addr(test_task_executor):
    test_task_executor.execute_final_task(
        task=bld_addr_final.ComputeBldAddrTask(),
        input_resources={
            "bld_addr_in": test_task_executor.create_ymapsdf_input_yt_table_resource("bld_addr"),
            "addr_nearest_bld": test_task_executor.create_custom_input_yt_table_resource("addr_nearest_bld"),
            "noaddr_bld_touching": test_task_executor.create_custom_input_yt_table_resource("noaddr_bld_touching"),
        },
        output_resources={
            "bld_addr_out": test_task_executor.create_yt_table_resource("bld_addr"),
        },
    )
