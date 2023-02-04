import pytest

from maps.garden.modules.ymapsdf.lib.parent_finder import parent_finder
from maps.garden.modules.ymapsdf.lib.parent_finder import process_ft


@pytest.mark.use_local_yt("hahn")
def test_update_search_class(test_task_executor):
    test_task_executor.execute_final_task(
        task=process_ft.UpdateSearchClassTask(),
        input_resources={
            "ft_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ft")
        },
        output_resources={
            parent_finder.FT_SEARCH_CLASS + "_out":
                test_task_executor.create_yt_table_resource(parent_finder.FT_SEARCH_CLASS)
        },
    )
