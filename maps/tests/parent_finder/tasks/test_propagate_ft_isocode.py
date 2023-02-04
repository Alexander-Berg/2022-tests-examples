import pytest

from maps.garden.libs.ymapsdf.lib.parent_finder import propagate_ft_isocodes


@pytest.mark.use_local_yt_yql
def test_propagate_ft_isocode(test_task_executor):
    test_task_executor.execute_final_task(
        task=propagate_ft_isocodes.PropagateFtIsocodes(),
        input_resources={
            "ad_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ad"),
            "ft_ad_tmp_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ft_ad"),
            "ft_search_class_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ft")
        },
        output_resources={
            "ft_out": test_task_executor.create_yt_table_resource("ft")
        },
    )
