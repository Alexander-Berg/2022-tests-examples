import pytest

from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from maps.garden.modules.ymapsdf_osm.defs import YT_CLUSTER
from maps.garden.modules.ymapsdf_osm.lib.build_bld import MakeBld
from maps.garden.modules.ymapsdf_osm.lib.constants import TmpTable, YmapsdfTable
from maps.garden.modules.ymapsdf_osm.lib.schemas import TMP_TABLES_SCHEMAS
from .utils import get_task_executor


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(environment_settings, source_folder="test_build_bld")


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_build_bld(task_executor):
    output_resources = {
        YmapsdfTable.BLD_GEOM: task_executor.create_yt_table_resource(YmapsdfTable.BLD_GEOM),
        TmpTable.BLD_WITHOUT_ISOCODES: task_executor.create_yt_table_resource(TmpTable.BLD_WITHOUT_ISOCODES),
        TmpTable.LOG_BAD_BLD: task_executor.create_yt_table_resource(TmpTable.LOG_BAD_BLD),
    }
    task_executor.execute_task(
        task=MakeBld(),
        input_resources={
            TmpTable.OBJECT_DETAILS_WITH_RELATIONS: task_executor.create_custom_input_yt_table_resource(
                TmpTable.OBJECT_DETAILS_WITH_RELATIONS,
                schema=TMP_TABLES_SCHEMAS[TmpTable.OBJECT_DETAILS]
            ),
        },
        output_resources=output_resources
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )
