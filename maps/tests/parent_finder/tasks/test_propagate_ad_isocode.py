import pytest

from maps.garden.modules.ymapsdf.lib.parent_finder import propagate_ad_isocodes


@pytest.mark.use_local_yt("hahn")
def test_propagate_ad_isocode(test_task_executor):
    test_task_executor.execute_final_task(
        task=propagate_ad_isocodes.PropagateAdIsocodesTask(),
        input_resources={
            "ad_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ad")
        },
        output_resources={
            "ad_out": test_task_executor.create_yt_table_resource("ad")
        },
    )
