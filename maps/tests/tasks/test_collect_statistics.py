import pytest
from unittest import mock

from yt.wrapper.schema import TableSchema
from maps.garden.sdk.test_utils.canonization import canonize_yt_tables

from maps.garden.libs.ymapsdf.lib.collect_statistics.collect_statistics import StatisticsRow

from maps.garden.modules.ymapsdf_osm.defs import YT_CLUSTER
from maps.garden.modules.ymapsdf_osm.lib.constants import YmapsdfTable
from maps.garden.modules.ymapsdf_osm.lib.collect_statistics import CollectStatistics, UpdateAllStatisticsTable
from maps.garden.modules.ymapsdf_osm.lib.schemas import LOG_TABLE_SCHEMA_WITH_ISOCODES

from .utils import get_task_executor


TEST_PROPERTIES = {
    "shipping_date": "20220624",
    "region": "saa",
    "vendor": "osm",
}


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(
        environment_settings, source_folder="test_collect_statistics", test_properties=TEST_PROPERTIES
    )


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_collect_statistics(task_executor):
    input_resources = {
        YmapsdfTable.AD: task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.AD),
        YmapsdfTable.ADDR: task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.ADDR),
        YmapsdfTable.BLD: task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.BLD),
        YmapsdfTable.RD: task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.RD),
        YmapsdfTable.FT: task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.FT),
        YmapsdfTable.RD_EL: task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.RD_EL),
        YmapsdfTable.COND: task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.COND),
        YmapsdfTable.COND_RD_SEQ: task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.COND_RD_SEQ),
        YmapsdfTable.LOG_BAD_OSM: task_executor.create_custom_input_yt_table_resource(
            table_name=YmapsdfTable.LOG_BAD_OSM,
            schema=LOG_TABLE_SCHEMA_WITH_ISOCODES
        ),
    }

    # This resource is not creating by the task.
    # But table `all_statistics` is appended in the end by task or created if not.
    # So this fake resource is helpful for check the result
    helper_resource = {
        "all_statistics": task_executor.create_custom_input_yt_table_resource(
            table_name="all_statistics",
            schema=list(TableSchema.from_row_type(StatisticsRow).to_yson_type())
        )
    }

    build_statistic_resources = {
        YmapsdfTable.STATISTICS: task_executor.create_yt_table_resource(YmapsdfTable.STATISTICS)
    }

    with (
        mock.patch(
            "maps.garden.libs.ymapsdf.lib.collect_statistics.collect_statistics.CollectStatistics._get_timestamp",
            return_value=1655769600
        ),
        mock.patch(
            "maps.garden.libs.ymapsdf.lib.collect_statistics.collect_statistics.UpdateAllStatisticsTable._get_all_statistics_table_path",
            return_value=helper_resource["all_statistics"].path
        )
    ):
        task_executor.execute_task(
            task=CollectStatistics(),
            input_resources=input_resources,
            output_resources=build_statistic_resources,
        )
        task_executor.execute_task(
            task=UpdateAllStatisticsTable(),
            input_resources=build_statistic_resources,
            output_resources=helper_resource,
        )

    return canonize_yt_tables(
        yt_table_resources=build_statistic_resources | helper_resource
    )
