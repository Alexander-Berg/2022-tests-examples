import pytest

from maps.garden.sdk.test_utils.canonization import canonize_json
from maps.garden.sdk.yt import YqlTask
from maps.garden.modules.ymapsdf_osm.lib.validation import ad
from maps.garden.modules.ymapsdf_osm.lib.constants import TmpTable, YmapsdfTable
from .utils import get_task_executor


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(environment_settings, source_folder="test_ad_filters")


@pytest.mark.use_local_yt_yql
def test_find_ad_without_geometry_task(mocker, task_executor):
    mocker.patch("maps.garden.modules.ymapsdf_osm.lib.utils.YT_CLUSTER", "plato")
    val_ad_without_geometry = task_executor.create_yt_table_resource(TmpTable.VALIDATION_AD_WITHOUT_GEOMETRY)

    task_executor.execute_task(
        task=YqlTask(
            query=ad.FIND_AD_WITHOUT_GEOMETRY_QUERY,
            displayed_name="FindAdWithoutGeometry",
        ),
        input_resources={
            YmapsdfTable.AD: task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.AD),
            YmapsdfTable.AD_GEOM: task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.AD_GEOM),
        },
        output_resources={
            TmpTable.VALIDATION_AD_WITHOUT_GEOMETRY: val_ad_without_geometry,
        },
    )
    return canonize_json(
        data=list(val_ad_without_geometry.read_table()),
        file_name="data"
    )


@pytest.mark.use_local_yt_yql
def test_find_parent_ad_cycles_task(mocker, task_executor):
    mocker.patch("maps.garden.modules.ymapsdf_osm.lib.utils.YT_CLUSTER", "plato")
    val_ad_parent_cycles = task_executor.create_yt_table_resource(TmpTable.VALIDATION_AD_PARENT_CYCLES)

    task_executor.execute_task(
        task=YqlTask(
            query=ad.FIND_PARENT_AD_CYCLES_QUERY,
            displayed_name="FindParentAdCycles",
        ),
        input_resources={
            YmapsdfTable.AD: task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.AD),
        },
        output_resources={
            TmpTable.VALIDATION_AD_PARENT_CYCLES: val_ad_parent_cycles,
        },
    )
    return canonize_json(
        data=list(val_ad_parent_cycles.read_table()),
        file_name="data"
    )
