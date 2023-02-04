import pytest

from maps.garden.libs.ymapsdf.lib.parent_finder import compute_bld_isocodes

from maps.garden.modules.ymapsdf.lib.parent_finder import build_coverage
from maps.garden.modules.ymapsdf.lib.parent_finder import parent_finder


@pytest.mark.use_local_yt("hahn")
def test_bld_isocodes(test_task_executor):
    result = test_task_executor.execute_task(
        task=build_coverage.BuildBriefCoverageTask(),
        input_resources={
            "ad_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ad"),
            "ad_geom_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ad_geom"),
        },
        output_resources={
            "coverage_yt_file_out": test_task_executor.create_coverage_resource(
                parent_finder.BRIEF_AD_COVERAGE_RESOURCE_NAME,
                parent_finder.BRIEF_AD_COVERAGE_RESOURCE_NAME,
            ),
        },
    )

    test_task_executor.execute_final_task(
        task=compute_bld_isocodes.ComputeBldIsocodes(),
        input_resources={
            "bld_in": test_task_executor.create_ymapsdf_input_yt_table_resource("bld"),
            "bld_geom_in": test_task_executor.create_ymapsdf_input_yt_table_resource("bld_geom"),
            "brief_ad_coverage": result["coverage_yt_file_out"],
        },
        output_resources={
            "bld_out": test_task_executor.create_yt_table_resource("bld"),
        },
    )
