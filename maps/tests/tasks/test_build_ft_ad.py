import pytest

from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from maps.garden.modules.ymapsdf_osm.defs import YT_CLUSTER
from maps.garden.modules.ymapsdf_osm.lib.build_ft_ad import FindLowestAdParentForFt
from maps.garden.modules.ymapsdf_osm.lib.constants import TmpFile, TmpTable, YmapsdfTable
from .utils import get_task_executor, get_full_ad_coverage


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(environment_settings, source_folder="test_build_ft_ad")


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_build_ft_ad(task_executor):
    ft_ad_out = task_executor.create_yt_table_resource(TmpTable.FT_AD_LOWEST)
    task_executor.execute_task(
        task=FindLowestAdParentForFt(),
        input_resources={
            YmapsdfTable.FT_GEOM: task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.FT_GEOM),
            TmpFile.FULL_AD_COVERAGE: get_full_ad_coverage(task_executor),
        },
        output_resources={
            TmpTable.FT_AD_LOWEST: ft_ad_out,
        },
    )

    return canonize_yt_tables(
        yt_table_resources=[ft_ad_out]
    )
