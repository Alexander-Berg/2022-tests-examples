import pytest

from maps.garden.modules.ymapsdf.lib.parent_finder import process_ad_excl


@pytest.mark.use_local_yt_yql
def test_process_ad_excl(test_task_executor):
    test_task_executor.execute_final_task(
        task=process_ad_excl.AddAutoExcludesTask(),
        input_resources={
            "ad_excl_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ad_excl"),
            "ad": test_task_executor.create_ymapsdf_input_yt_table_resource("ad"),
            "ad_nm": test_task_executor.create_ymapsdf_input_yt_table_resource("ad_nm"),
            "locality": test_task_executor.create_ymapsdf_input_yt_table_resource("locality"),
        },
        output_resources={
            "ad_excl_out": test_task_executor.create_yt_table_resource("ad_excl")
        },
    )
