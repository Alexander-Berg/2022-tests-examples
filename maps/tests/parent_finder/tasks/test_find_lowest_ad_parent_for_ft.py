import pytest

from maps.garden.modules.ymapsdf.lib.parent_finder import build_coverage
from maps.garden.modules.ymapsdf.lib.parent_finder import parent_finder
from maps.garden.modules.ymapsdf.lib.parent_finder import process_ft


@pytest.mark.use_local_yt("hahn")
def test_find_lowest_ad_parent_for_ft(test_task_executor):
    ad_geom_resource = test_task_executor.create_custom_input_yt_table_resource(parent_finder.AD_GEOM_FOR_COVERAGE)

    result = test_task_executor.execute_task(
        task=build_coverage.BuildFullCoverageTask(),
        input_resources={
            parent_finder.AD_GEOM_FOR_COVERAGE + "_in": ad_geom_resource,
        },
        output_resources={
            "coverage_yt_file_out": test_task_executor.create_coverage_resource(
                parent_finder.FULL_AD_COVERAGE_RESOURCE_NAME,
                parent_finder.FULL_AD_COVERAGE_FILE_NAME,
            ),
        },
    )

    test_task_executor.execute_final_task(
        task=process_ft.FindLowestAdParentForFtTask(),
        input_resources={
            "ft": test_task_executor.create_ymapsdf_input_yt_table_resource("ft"),
            "ft_geom": test_task_executor.create_ymapsdf_input_yt_table_resource("ft_geom"),
            "ft_ad_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ft_ad"),
            "full_coverage": result["coverage_yt_file_out"],
        },
        output_resources={
            "ft_ad_tmp": test_task_executor.create_yt_table_resource("ft_ad_tmp"),
        },
    )
