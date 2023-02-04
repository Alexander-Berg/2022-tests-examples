import pytest

from maps.garden.modules.ymapsdf.lib.common import filter_region_data_task
from maps.garden.modules.ymapsdf.lib.common.filter_region_data import FormatType


_INPUT_DATA_TABLE_NAME = "input_table"
_OUTPUT_DATA_TABLE_NAME = "output_table"


@pytest.fixture
def input_resources(test_task_executor):
    return {
        "meta": test_task_executor.create_ymapsdf_input_yt_table_resource("meta"),
        "meta_param": test_task_executor.create_ymapsdf_input_yt_table_resource("meta_param"),
        "input_table": test_task_executor.create_custom_input_yt_table_resource(_INPUT_DATA_TABLE_NAME)
    }


@pytest.fixture
def output_resources(test_task_executor):
    return {
        "output_table": test_task_executor.create_yt_table_resource(_OUTPUT_DATA_TABLE_NAME),
    }


@pytest.mark.use_local_yt("hahn")
def test_filter_wkb_data_by_polygon_region(test_task_executor, input_resources, output_resources):
    result = test_task_executor.execute_task(
        task=filter_region_data_task.FilterRegionDataTask(
            geometry_column="wkb_shape",
            geometry_format=int(FormatType.WKB),
            input_table_name=_INPUT_DATA_TABLE_NAME,
            output_table_name=_OUTPUT_DATA_TABLE_NAME),
        input_resources=input_resources,
        output_resources=output_resources
    )
    test_task_executor.validate(result)
    # Ensure that the original table ordering is preserved
    assert result[_OUTPUT_DATA_TABLE_NAME].key_columns == ["some_field"]


@pytest.mark.use_local_yt("hahn")
def test_filter_ewkb_data_by_polygon_region(test_task_executor, input_resources, output_resources):
    result = test_task_executor.execute_task(
        task=filter_region_data_task.FilterRegionDataTask(
            geometry_column="shape",
            geometry_format=int(FormatType.EWKB),
            input_table_name=_INPUT_DATA_TABLE_NAME,
            output_table_name=_OUTPUT_DATA_TABLE_NAME),
        input_resources=input_resources,
        output_resources=output_resources
    )
    test_task_executor.validate(result)
    # Ensure that the original table ordering is preserved
    assert not result[_OUTPUT_DATA_TABLE_NAME].key_columns


@pytest.mark.use_local_yt("hahn")
def test_filter_yson_point_data_by_polygon_region(test_task_executor, input_resources, output_resources):
    test_task_executor.execute_final_task(
        task=filter_region_data_task.FilterRegionDataTask(
            geometry_column="point",
            geometry_format=int(FormatType.YSON_POINT),
            input_table_name=_INPUT_DATA_TABLE_NAME,
            output_table_name=_OUTPUT_DATA_TABLE_NAME),
        input_resources=input_resources,
        output_resources=output_resources
    )


@pytest.mark.use_local_yt("hahn")
def test_filter_wkb_data_by_multipolygon_region(test_task_executor, input_resources, output_resources):
    test_task_executor.execute_final_task(
        task=filter_region_data_task.FilterRegionDataTask(
            geometry_column="wkb_shape",
            geometry_format=int(FormatType.WKB),
            input_table_name=_INPUT_DATA_TABLE_NAME,
            output_table_name=_OUTPUT_DATA_TABLE_NAME),
        input_resources=input_resources,
        output_resources=output_resources
    )


@pytest.mark.use_local_yt("hahn")
def test_filter_ewkb_data_by_multipolygon_region(test_task_executor, input_resources, output_resources):
    test_task_executor.execute_final_task(
        task=filter_region_data_task.FilterRegionDataTask(
            geometry_column="shape",
            geometry_format=int(FormatType.EWKB),
            input_table_name=_INPUT_DATA_TABLE_NAME,
            output_table_name=_OUTPUT_DATA_TABLE_NAME),
        input_resources=input_resources,
        output_resources=output_resources
    )


@pytest.mark.use_local_yt("hahn")
def test_filter_yson_point_data_by_multipolygon_region(test_task_executor, input_resources, output_resources):
    test_task_executor.execute_final_task(
        task=filter_region_data_task.FilterRegionDataTask(
            geometry_column="point",
            geometry_format=int(FormatType.YSON_POINT),
            input_table_name=_INPUT_DATA_TABLE_NAME,
            output_table_name=_OUTPUT_DATA_TABLE_NAME),
        input_resources=input_resources,
        output_resources=output_resources
    )
