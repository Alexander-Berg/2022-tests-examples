import pytest
import typing as tp

from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from maps.garden.modules.ymapsdf_osm.defs import YT_CLUSTER
from maps.garden.modules.ymapsdf_osm.lib.log_bad_osm import INPUT_LOGS_TABLES, MergeBadOsmLogs
from maps.garden.modules.ymapsdf_osm.lib.constants import TmpTable, YmapsdfTable
from maps.garden.modules.ymapsdf_osm.lib.schemas import TMP_TABLES_SCHEMAS
from .utils import get_task_executor


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(
        environment_settings=environment_settings,
        source_folder="test_log_bad_osm",
    )


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_log_bad_osm(task_executor):
    def get_schema(table_name: str) -> list[dict[str, tp.Any]]:
        schema = TMP_TABLES_SCHEMAS[table_name]
        schema[0]["sort_order"] = "ascending"
        return schema

    input_resources = {
        table_name: task_executor.create_custom_input_yt_table_resource(table_name, schema=get_schema(table_name))
        for table_name in (INPUT_LOGS_TABLES + [TmpTable.OBJECT_DETAILS_WITH_ISOCODES])
    }

    output_resources = {
        YmapsdfTable.LOG_BAD_OSM: task_executor.create_yt_table_resource(YmapsdfTable.LOG_BAD_OSM),
    }

    task_executor.execute_task(
        task=MergeBadOsmLogs(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )
