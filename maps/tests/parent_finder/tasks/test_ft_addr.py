import os.path
import pytest

from maps.garden.sdk import test_utils
from maps.garden.libs.ymapsdf.lib.parent_finder import ft_addr
from maps.garden.modules.ymapsdf.lib.parent_finder.ft_addr import FtAddrTask


@pytest.mark.use_local_yt_yql
def test_ft_points(test_task_executor):
    result = test_task_executor.execute_task(
        task=ft_addr.FtPoints(),
        input_resources={
            "ft_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ft"),
            "ft_center_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ft_center"),
            "ft_edge_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ft_edge"),
            "ft_face_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ft_face"),
            "node_in": test_task_executor.create_ymapsdf_input_yt_table_resource("node"),
        },
        output_resources={
            "ft_points_out": test_task_executor.create_yt_table_resource("ft_points"),
        },
    )
    test_task_executor.validate(result)

    test_task_executor.execute_final_task(
        task=ft_addr.FtPointTiles(),
        input_resources={
            "ft_points_in": result["ft_points_out"],
        },
        output_resources={
            "ft_point_tiles_out": test_task_executor.create_yt_table_resource("ft_point_tiles"),
        },
    )


@pytest.mark.use_local_yt("hahn")
def test_ft_near_bld(test_task_executor):
    test_task_executor.execute_final_task(
        task=ft_addr.FtNearBld(),
        input_resources={
            "ft_point_tiles_in": test_task_executor.create_custom_input_yt_table_resource("ft_point_tiles"),
            "bld_tiles_in": test_task_executor.create_custom_input_yt_table_resource("bld_tiles")
        },
        output_resources={
            "ft_near_bld_out": test_task_executor.create_yt_table_resource("ft_near_bld"),
        },
    )


@pytest.mark.use_local_yt_yql
def test_ft_nearest_bld_addr(test_task_executor):
    result = test_task_executor.execute_task(
        task=ft_addr.FtNearestBldAddr(),
        input_resources={
            "bld_in": test_task_executor.create_ymapsdf_input_yt_table_resource("bld"),
            "bld_addr_in": test_task_executor.create_ymapsdf_input_yt_table_resource("bld_addr"),
            "ft_near_bld_in": test_task_executor.create_custom_input_yt_table_resource("ft_near_bld"),
            "addr_geom_tmp_in": test_task_executor.create_custom_input_yt_table_resource("addr_geom_tmp"),
        },
        output_resources={
            "ft_nearest_bld_addr_out": test_task_executor.create_yt_table_resource("ft_nearest_bld_addr"),
        },
    )

    # Convert geometry fields in the expected output table from WKT to WKB to allow for comparison
    filepath = os.path.join(test_task_executor.data_path_output, "ft_nearest_bld_addr.jsonl")
    expected = list(test_utils.geometry.convert_wkt_to_wkb_in_jsonl_file(
        filepath, field_names=["ft_shape", "addr_shape"]))
    assert list(result["ft_nearest_bld_addr_out"].read_table()) == expected

    test_task_executor.execute_final_task(
        task=ft_addr.FtBldAddrDistance(),
        input_resources={
            "ft_nearest_bld_addr_in": result["ft_nearest_bld_addr_out"],
        },
        output_resources={
            "ft_bld_addr_distance_out": test_task_executor.create_yt_table_resource("ft_bld_addr_distance"),
        },
    )


@pytest.mark.use_local_yt_yql
def test_update_ft_addr(test_task_executor):
    test_task_executor.execute_final_task(
        task=FtAddrTask(),
        input_resources={
            "ft_addr_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ft_addr"),
            "ft_bld_addr_distance_in": test_task_executor.create_custom_input_yt_table_resource("ft_bld_addr_distance"),
        },
        output_resources={
            "ft_addr_out": test_task_executor.create_yt_table_resource("ft_addr")
        },
    )
