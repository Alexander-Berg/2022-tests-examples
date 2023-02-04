import pytest

from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from maps.garden.sdk.yt import YqlTask
from maps.garden.modules.ymapsdf_osm.defs import YT_CLUSTER
from maps.garden.modules.ymapsdf_osm.lib.build_coverage import (
    FindParentForAd, MergeAdGeomForCoverageWithExtraCities, BUILD_AD_GEOM_FOR_COVERAGE_QUERY
)
from maps.garden.modules.ymapsdf_osm.lib.schemas import TMP_TABLES_SCHEMAS
from maps.garden.modules.ymapsdf_osm.lib.constants import TmpTable, TmpFile, YmapsdfTable
from .utils import get_task_executor, get_full_ad_coverage, make_sorted


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(environment_settings, source_folder="test_build_coverage")


@pytest.mark.use_local_yt_yql
def test_build_ad_geom_for_coverage_task(mocker, task_executor):
    mocker.patch("maps.garden.modules.ymapsdf_osm.lib.utils.YT_CLUSTER", "plato")
    ad_geom_for_coverage = task_executor.create_yt_table_resource(TmpTable.AD_GEOM_FOR_COVERAGE)
    task_executor.execute_task(
        task=YqlTask(
            query=BUILD_AD_GEOM_FOR_COVERAGE_QUERY,
            displayed_name="BuildAdGeomForCoverage",
        ),
        input_resources={
            TmpTable.AD_WITHOUT_PARENTS: task_executor.create_ymapsdf_input_yt_table_resource(
                table_name=TmpTable.AD_WITHOUT_PARENTS,
                schema_name=YmapsdfTable.AD,
            ),
            TmpTable.AD_GEOM_WITHOUT_EXTRA_CITY_BORDERS: task_executor.create_ymapsdf_input_yt_table_resource(
                table_name=YmapsdfTable.AD_GEOM,
            ),
        },
        output_resources={
            TmpTable.AD_GEOM_FOR_COVERAGE: ad_geom_for_coverage,
        },
    )

    return canonize_yt_tables(
        yt_table_resources=[ad_geom_for_coverage]
    )


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_merge_ad_geom_for_coverage_with_extra_cities_task(mocker, task_executor):

    geom_for_coverage_out = task_executor.create_yt_table_resource(TmpTable.AD_GEOM_FOR_COVERAGE_WITH_EXTRA_CITIES)
    task_executor.execute_task(
        task=MergeAdGeomForCoverageWithExtraCities(),
        input_resources={
            TmpTable.AD_GEOM_FOR_COVERAGE: task_executor.create_custom_input_yt_table_resource(
                TmpTable.AD_GEOM_FOR_COVERAGE,
                schema=make_sorted(TMP_TABLES_SCHEMAS[TmpTable.AD_GEOM_FOR_COVERAGE]),
            ),
            TmpTable.AD_CITY_BORDERS_FROM_ROADS: task_executor.create_ymapsdf_input_yt_table_resource(
                TmpTable.AD_CITY_BORDERS_FROM_ROADS,
                schema_name=YmapsdfTable.AD_GEOM,
            ),
        },
        output_resources={
            TmpTable.AD_GEOM_FOR_COVERAGE_WITH_EXTRA_CITIES: geom_for_coverage_out,
        },
    )

    return canonize_yt_tables(
        yt_table_resources=[geom_for_coverage_out]
    )


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_build_full_coverage_task(task_executor):
    coverage_data = get_full_ad_coverage(task_executor).read_file().read()
    # todo: check coverage content somehow
    assert coverage_data


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_find_parent_for_ad_task(task_executor):
    full_ad_coverage = get_full_ad_coverage(task_executor)
    ad_out = task_executor.create_yt_table_resource(YmapsdfTable.AD)
    task_executor.execute_task(
        task=FindParentForAd(),
        input_resources={
            TmpTable.AD_WITHOUT_PARENTS: task_executor.create_ymapsdf_input_yt_table_resource(
                table_name=TmpTable.AD_WITHOUT_PARENTS,
                schema_name=YmapsdfTable.AD,
            ),
            YmapsdfTable.AD_GEOM: task_executor.create_ymapsdf_input_yt_table_resource(
                table_name=YmapsdfTable.AD_GEOM,
            ),
            TmpFile.FULL_AD_COVERAGE: full_ad_coverage,
        },
        output_resources={
            YmapsdfTable.AD: ad_out,
        },
    )

    return canonize_yt_tables(
        yt_table_resources=[ad_out]
    )
