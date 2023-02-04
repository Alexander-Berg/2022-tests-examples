import pytest
from maps.garden.libs.ymapsdf.lib.parent_finder import propagate_addr_isocodes
from maps.garden.modules.ymapsdf.lib.parent_finder import process_addr


@pytest.mark.use_local_yt_yql
def test_propagate_addr_isocode(test_task_executor):
    test_task_executor.execute_final_task(
        task=propagate_addr_isocodes.PropagateAddrIsocodes(),
        input_resources={
            "addr_in": test_task_executor.create_ymapsdf_input_yt_table_resource("addr"),
            "ft_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ft"),
            "rd_in": test_task_executor.create_ymapsdf_input_yt_table_resource("rd"),
            "ad_in": test_task_executor.create_ymapsdf_input_yt_table_resource("ad"),
        },
        output_resources={
            "addr_with_isocodes": test_task_executor.create_yt_table_resource("addr")
        },
    )


@pytest.mark.use_local_yt_yql
def test_recompute_addr_search_class(test_task_executor):
    test_task_executor.execute_final_task(
        task=process_addr.RecomputeAddrSearchClass(),
        input_resources={
            "addr_with_isocodes": test_task_executor.create_ymapsdf_input_yt_table_resource("addr"),
            "bld_addr": test_task_executor.create_ymapsdf_input_yt_table_resource("bld_addr"),
        },
        output_resources={
            "addr_out": test_task_executor.create_yt_table_resource("addr")
        },
    )
