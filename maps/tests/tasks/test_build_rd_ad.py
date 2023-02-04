import pytest

from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from maps.garden.modules.ymapsdf_osm.defs import YT_CLUSTER
from maps.garden.modules.ymapsdf_osm.lib.build_rd_ad import(
    BuildRdAd, PrepareCityCenterGeom, BuildCityRoadsGeom, BuildCityBordersByRoads
)
from maps.garden.modules.ymapsdf_osm.lib.schemas import TMP_TABLES_SCHEMAS
from maps.garden.modules.ymapsdf_osm.lib.constants import TmpFile, TmpTable, YmapsdfTable
from .utils import get_task_executor, get_full_ad_coverage


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(environment_settings, source_folder="test_build_rd_ad")


@pytest.mark.use_local_yt_yql
def test_prepare_city_center_geom(mocker, task_executor):
    mocker.patch("maps.garden.modules.ymapsdf_osm.lib.utils.YT_CLUSTER", "plato")
    input_resources = {
        TmpTable.AD_WITHOUT_PARENTS: task_executor.create_ymapsdf_input_yt_table_resource(
            table_name=YmapsdfTable.AD,
        ),
        TmpTable.AD_GEOM_WITHOUT_EXTRA_CITY_BORDERS: task_executor.create_ymapsdf_input_yt_table_resource(
            table_name=YmapsdfTable.AD_GEOM,
        ),
    }
    output_resources = {
        TmpTable.AD_CITY_CENTER_GEOM: task_executor.create_yt_table_resource(TmpTable.AD_CITY_CENTER_GEOM),
    }
    task_executor.execute_task(
        task=PrepareCityCenterGeom(),
        input_resources=input_resources,
        output_resources=output_resources,
    )
    return canonize_yt_tables(
        yt_table_resources=output_resources
    )


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_build_city_roads_geom(task_executor):
    city_roads_geom = task_executor.create_yt_table_resource(TmpTable.AD_CITY_ROADS_GEOM)
    task_executor.execute_task(
        task=BuildCityRoadsGeom(),
        input_resources={
            TmpFile.FULL_AD_COVERAGE: get_full_ad_coverage(task_executor),
            YmapsdfTable.RD_GEOM: task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.RD_GEOM),
            TmpTable.AD_CITY_CENTER_GEOM: task_executor.create_custom_input_yt_table_resource(
                table_name=TmpTable.AD_CITY_CENTER_GEOM,
                schema=TMP_TABLES_SCHEMAS[TmpTable.AD_CITY_CENTER_GEOM],
            ),
            TmpTable.RD_EL_WITHOUT_NAMES: task_executor.create_custom_input_yt_table_resource(
                table_name=TmpTable.RD_EL_WITHOUT_NAMES,
                schema=TMP_TABLES_SCHEMAS[TmpTable.RD_EL_WITHOUT_NAMES],
            ),
        },
        output_resources={
            TmpTable.AD_CITY_ROADS_GEOM: city_roads_geom,
        },
    )
    return canonize_yt_tables(
        yt_table_resources=[city_roads_geom]
    )


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_build_city_borders_by_roads(task_executor):
    ad_geom_out = task_executor.create_yt_table_resource(TmpTable.AD_CITY_BORDERS_FROM_ROADS)
    task_executor.execute_task(
        task=BuildCityBordersByRoads(),
        input_resources={
            TmpFile.FULL_AD_COVERAGE: get_full_ad_coverage(task_executor),
            TmpTable.AD_CITY_ROADS_GEOM: task_executor.create_custom_input_yt_table_resource(
                table_name=TmpTable.AD_CITY_ROADS_GEOM,
                schema=TMP_TABLES_SCHEMAS[TmpTable.AD_CITY_ROADS_GEOM],
            ),
        },
        output_resources={
            TmpTable.AD_CITY_BORDERS_FROM_ROADS: ad_geom_out,
        },
    )
    return canonize_yt_tables(
        yt_table_resources=[ad_geom_out]
    )


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_build_rd_ad_by_coverage(task_executor):
    rd_ad = task_executor.create_yt_table_resource(YmapsdfTable.RD_AD)
    task_executor.execute_task(
        task=BuildRdAd(),
        input_resources={
            YmapsdfTable.RD_GEOM: task_executor.create_ymapsdf_input_yt_table_resource(YmapsdfTable.RD_GEOM),
            TmpFile.FULL_AD_COVERAGE: get_full_ad_coverage(task_executor),
        },
        output_resources={
            YmapsdfTable.RD_AD: rd_ad,
        },
    )
    return canonize_yt_tables(
        yt_table_resources=[rd_ad]
    )
