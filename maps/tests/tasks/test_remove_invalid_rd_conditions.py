import pytest

from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from maps.garden.modules.ymapsdf_osm.defs import YT_CLUSTER
from maps.garden.modules.ymapsdf_osm.lib.constants import TmpTable, YmapsdfTable
from maps.garden.modules.ymapsdf_osm.lib.remove_invalid_rd_conditions import RemoveInvalidRdConditions

from .utils import get_task_executor


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(
        environment_settings,
        source_folder="test_remove_invalid_rd_conditions"
    )


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_remove_invalid_rd_conditions(task_executor):
    create_input = task_executor.create_ymapsdf_input_yt_table_resource

    input_resources = {
        YmapsdfTable.RD_EL: create_input(
            YmapsdfTable.RD_EL
        ),
        TmpTable.COND_WITH_TOLL_POSTS: create_input(
            TmpTable.COND_WITH_TOLL_POSTS,
            "cond"
        ),
        TmpTable.COND_RD_SEQ_WITH_TOLL_POSTS: create_input(
            TmpTable.COND_RD_SEQ_WITH_TOLL_POSTS,
            "cond_rd_seq"
        ),
        TmpTable.VEHICLE_RESTRICTION_TMP: create_input(
            TmpTable.VEHICLE_RESTRICTION_TMP,
            "vehicle_restriction"
        ),
        TmpTable.RD_EL_VEHICLE_RESTRICTION_TMP: create_input(
            TmpTable.RD_EL_VEHICLE_RESTRICTION_TMP,
            "rd_el_vehicle_restriction"
        ),
        TmpTable.COND_VEHICLE_RESTRICTION_TMP: create_input(
            TmpTable.COND_VEHICLE_RESTRICTION_TMP,
            "cond_vehicle_restriction"
        )
    }

    output_resources = dict(
        (table, task_executor.create_yt_table_resource(table))
        for table in [
            YmapsdfTable.COND,
            YmapsdfTable.COND_RD_SEQ,
            YmapsdfTable.VEHICLE_RESTRICTION,
            YmapsdfTable.RD_EL_VEHICLE_RESTRICTION,
            YmapsdfTable.COND_VEHICLE_RESTRICTION
        ]
    )

    task_executor.execute_task(
        task=RemoveInvalidRdConditions(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )
