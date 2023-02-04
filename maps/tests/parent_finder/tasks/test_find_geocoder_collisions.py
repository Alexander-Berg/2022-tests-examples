import pytest

from maps.garden.modules.ymapsdf.lib.parent_finder import parent_finder
from maps.garden.modules.ymapsdf.lib.parent_finder import process_ft


@pytest.mark.use_local_yt_yql
def test_find_geocoder_collisions(test_task_executor):
    test_task_executor.execute_final_task(
        task=process_ft.FindGeocoderCollisionsTask(),
        input_resources={
            "ft_nm": test_task_executor.create_ymapsdf_input_yt_table_resource("ft_nm"),
            "ft_type": test_task_executor.create_ymapsdf_input_yt_table_resource("ft_type"),
            parent_finder.FT_SEARCH_CLASS: test_task_executor.create_ymapsdf_input_yt_table_resource("ft"),
            "ft_ad_tmp": test_task_executor.create_ymapsdf_input_yt_table_resource("ft_ad"),
        },
        output_resources={
            parent_finder.FT_AD_GEOCODER_COLLISIONS:
                test_task_executor.create_yt_table_resource(parent_finder.FT_AD_GEOCODER_COLLISIONS)
        },
    )
