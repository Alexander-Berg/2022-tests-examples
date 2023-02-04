import pytest

from maps.garden.modules.ymapsdf.lib.parent_finder import parent_finder


@pytest.mark.use_local_yt_yql
def test_filter_ad_geom_for_coverage(test_task_executor):
    ad_geom_for_coverage = parent_finder.AD_GEOM_FOR_COVERAGE
    test_task_executor.execute_final_task(
        task=parent_finder.BuildAdGeomForCoverageTask(),
        input_resources={
            table_name: test_task_executor.create_ymapsdf_input_yt_table_resource(table_name)
            for table_name in ["ad", "ad_nm", "ad_geom"]
        },
        output_resources={
            ad_geom_for_coverage: test_task_executor.create_yt_table_resource(ad_geom_for_coverage),
        },
    )
