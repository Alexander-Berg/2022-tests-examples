import pytest

from maps.garden.libs.ymapsdf.lib.parent_finder import propagate_ft_isocodes
from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from maps.garden.modules.ymapsdf_osm.lib.constants import YmapsdfTable
from .utils import get_task_executor


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(environment_settings, source_folder="test_propagate_ft_isocodes")


@pytest.mark.use_local_yt_yql
def test_build_ad_geom_for_coverage_task(mocker, task_executor):
    mocker.patch("maps.garden.modules.ymapsdf_osm.lib.utils.YT_CLUSTER", "plato")
    ft_out = task_executor.create_yt_table_resource(YmapsdfTable.FT)
    task_executor.execute_task(
        task=propagate_ft_isocodes.PropagateFtIsocodes(),
        input_resources={
            "ft_search_class_in": task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.FT),
            "ft_ad_tmp_in": task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.FT_AD),
            "ad_in": task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.AD),
        },
        output_resources={
            "ft_out": ft_out,
        },
    )

    return canonize_yt_tables(
        yt_table_resources=[ft_out]
    )
