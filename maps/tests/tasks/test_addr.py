import logging
import pytest

from maps.garden.modules.ymapsdf_osm.lib import addr
from maps.garden.modules.ymapsdf_osm.lib.constants import TmpTable
from maps.garden.modules.ymapsdf_osm.lib.schemas import TMP_TABLES_SCHEMAS

from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from .utils import get_task_executor

logger = logging.getLogger("ymapsdf_osm.test_addr_tables")


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(environment_settings, source_folder="test_addr_create_tables")


@pytest.mark.use_local_yt("hahn")
def test_addr_filter_tables(task_executor):
    input_resources = {
        TmpTable.OBJECT_DETAILS_WITH_RELATIONS: task_executor.create_custom_input_yt_table_resource(
            TmpTable.OBJECT_DETAILS_WITH_RELATIONS,
            schema=TMP_TABLES_SCHEMAS[TmpTable.OBJECT_DETAILS]
        ),
    }

    output_resources = {
        TmpTable.ADDR_OBJECT_DETAILS: task_executor.create_yt_table_resource(TmpTable.ADDR_OBJECT_DETAILS),
        TmpTable.LOG_BAD_ADDR_FILTER: task_executor.create_yt_table_resource(TmpTable.LOG_BAD_ADDR_FILTER),
    }

    task_executor.execute_task(
        task=addr.FilterAddrTables(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )


@pytest.mark.use_local_yt("hahn")
def test_addr_create_tables(task_executor):
    input_resources = {
        TmpTable.ADDR_OBJECT_DETAILS: task_executor.create_custom_input_yt_table_resource(
            TmpTable.ADDR_OBJECT_DETAILS,
            schema=TMP_TABLES_SCHEMAS[TmpTable.OBJECT_DETAILS]
        ),
    }

    output_resources = {
        TmpTable.ADDR_WITH_COORD_AND_RD_NAME: task_executor.create_yt_table_resource(TmpTable.ADDR_WITH_COORD_AND_RD_NAME),
        TmpTable.ADDR_TAGS: task_executor.create_yt_table_resource(TmpTable.ADDR_TAGS),
        TmpTable.ADDR_NODE: task_executor.create_yt_table_resource(TmpTable.ADDR_NODE),
        TmpTable.LOG_BAD_ADDR: task_executor.create_yt_table_resource(TmpTable.LOG_BAD_ADDR),
    }

    task_executor.execute_task(
        task=addr.MakeAddrTables(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )
