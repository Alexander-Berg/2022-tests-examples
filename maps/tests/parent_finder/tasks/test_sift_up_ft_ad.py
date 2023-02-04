import pytest

from maps.garden.modules.ymapsdf.lib.parent_finder import parent_finder
from maps.garden.modules.ymapsdf.lib.parent_finder import process_ft


@pytest.mark.use_local_yt("hahn")
def test_sift_up_ft_ad(test_task_executor):
    create_ymapsdf = test_task_executor.create_ymapsdf_input_yt_table_resource
    create_custom = test_task_executor.create_custom_input_yt_table_resource

    test_task_executor.execute_final_task(
        task=process_ft.SiftUpFtAdTask(),
        input_resources={
            "ad": create_ymapsdf("ad"),
            "ft": create_ymapsdf("ft"),
            "ft_ad_tmp": create_ymapsdf("ft_ad"),
            parent_finder.FT_AD_GEOCODER_COLLISIONS: create_custom(parent_finder.FT_AD_GEOCODER_COLLISIONS),
        },
        output_resources={
            "ft_ad": test_task_executor.create_yt_table_resource("ft_ad"),
        },
    )
