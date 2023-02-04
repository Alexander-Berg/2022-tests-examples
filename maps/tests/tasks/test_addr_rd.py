import pytest

from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from maps.garden.modules.ymapsdf_osm.lib import addr_rd
from maps.garden.modules.ymapsdf_osm.lib.constants import TmpTable

from .utils import get_task_executor


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(environment_settings, source_folder="test_addr_rd")


@pytest.mark.use_local_yt("hahn")
def test_link_addr_and_rd(task_executor):
    input_resources = {
        TmpTable.ADDR_WITH_COORD_AND_RD_NAME: task_executor.create_custom_input_yt_table_resource(
            TmpTable.ADDR_WITH_COORD_AND_RD_NAME
        ),
        TmpTable.RD_NM_GEOM: task_executor.create_custom_input_yt_table_resource(TmpTable.RD_NM_GEOM)
    }

    output_resources = {
        TmpTable.ADDR_WITHOUT_ISOCODES: task_executor.create_yt_table_resource(TmpTable.ADDR_WITHOUT_ISOCODES),
        TmpTable.LOG_BAD_ADDR_RD: task_executor.create_yt_table_resource(TmpTable.LOG_BAD_ADDR_RD),
    }

    task_executor.execute_task(
        task=addr_rd.LinkAddrAndRd(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )
