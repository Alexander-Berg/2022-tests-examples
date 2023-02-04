import pytest

from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from maps.garden.modules.osm_borders_src.defs import COUNTRIES_GEOM_TABLE, COUNTRIES_GEOM_SCHEMA
from maps.garden.modules.ymapsdf_osm.defs import YT_CLUSTER
from maps.garden.modules.ymapsdf_osm.lib.build_ad import (
    FilterAd, SelectLabelNodes, MoveTagsToCityRelations, FilterCityNodeDuplicates, BuildAd
)
from maps.garden.modules.ymapsdf_osm.lib.constants import TmpTable, YmapsdfTable, OsmTable
from maps.garden.modules.ymapsdf_osm.lib.schemas import TMP_TABLES_SCHEMAS
from .utils import get_task_executor, make_sorted


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(environment_settings, source_folder="test_build_ad")


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_filter_ad(task_executor):
    input_resources = {
        TmpTable.OBJECT_DETAILS_WITH_ISOCODES: task_executor.create_custom_input_yt_table_resource(
            TmpTable.OBJECT_DETAILS_WITH_ISOCODES,
            schema=TMP_TABLES_SCHEMAS[TmpTable.OBJECT_DETAILS_WITH_ISOCODES]
        ),
    }

    output_resources = {
        TmpTable.AD_OBJECT_DETAILS: task_executor.create_yt_table_resource(
            TmpTable.AD_OBJECT_DETAILS,
        ),
    }

    task_executor.execute_task(
        task=FilterAd(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )


@pytest.mark.use_local_yt_yql
def test_select_label_nodes(task_executor):
    input_resources = {
        TmpTable.AD_OBJECT_DETAILS: task_executor.create_custom_input_yt_table_resource(
            TmpTable.AD_OBJECT_DETAILS,
            schema=TMP_TABLES_SCHEMAS[TmpTable.OBJECT_DETAILS_WITH_ISOCODES]
        ),
        OsmTable.RELATIONS_MEMBERS: task_executor.create_custom_input_yt_table_resource(OsmTable.RELATIONS_MEMBERS),
    }

    output_resources = {
        TmpTable.AD_LABEL_DUPLICATES: task_executor.create_yt_table_resource(
            TmpTable.AD_LABEL_DUPLICATES,
        ),
    }

    task_executor.execute_task(
        task=SelectLabelNodes(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_move_tags_to_city_relations(task_executor):
    input_resources = {
        TmpTable.AD_OBJECT_DETAILS: task_executor.create_custom_input_yt_table_resource(
            TmpTable.AD_OBJECT_DETAILS,
            schema=TMP_TABLES_SCHEMAS[TmpTable.OBJECT_DETAILS_WITH_ISOCODES]
        ),
        TmpTable.AD_LABEL_DUPLICATES: task_executor.create_custom_input_yt_table_resource(
            TmpTable.AD_LABEL_DUPLICATES,
            schema=TMP_TABLES_SCHEMAS[TmpTable.AD_LABEL_DUPLICATES]
        ),
    }

    output_resources = {
        TmpTable.AD_OBJECT_DETAILS_ADDED_TAGS: task_executor.create_yt_table_resource(
            TmpTable.AD_OBJECT_DETAILS_ADDED_TAGS,
        ),
        TmpTable.AD_DUPLICATES_ID: task_executor.create_yt_table_resource(
            TmpTable.AD_DUPLICATES_ID,
        ),
    }

    task_executor.execute_task(
        task=MoveTagsToCityRelations(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_filter_city_node_duplicates(task_executor):
    input_resources = {
        TmpTable.AD_OBJECT_DETAILS_ADDED_TAGS: task_executor.create_custom_input_yt_table_resource(
            TmpTable.AD_OBJECT_DETAILS_ADDED_TAGS,
            schema=TMP_TABLES_SCHEMAS[TmpTable.OBJECT_DETAILS_WITH_ISOCODES]
        ),
        TmpTable.AD_DUPLICATES_ID: task_executor.create_custom_input_yt_table_resource(
            TmpTable.AD_DUPLICATES_ID,
            schema=make_sorted(TMP_TABLES_SCHEMAS[TmpTable.AD_DUPLICATES_ID])
        ),
    }

    output_resources = {
        TmpTable.AD_OBJECT_DETAILS_WO_DUPLICATES: task_executor.create_yt_table_resource(
            TmpTable.AD_OBJECT_DETAILS_WO_DUPLICATES,
        ),
    }

    task_executor.execute_task(
        task=FilterCityNodeDuplicates(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_build_ad(task_executor):
    input_resources = {
        COUNTRIES_GEOM_TABLE: task_executor.create_custom_input_yt_table_resource(
            table_name=COUNTRIES_GEOM_TABLE,
            schema=COUNTRIES_GEOM_SCHEMA,
        ),
        TmpTable.AD_OBJECT_DETAILS_WO_DUPLICATES: task_executor.create_custom_input_yt_table_resource(
            TmpTable.AD_OBJECT_DETAILS_WO_DUPLICATES,
            schema=TMP_TABLES_SCHEMAS[TmpTable.OBJECT_DETAILS_WITH_ISOCODES]
        ),
    }

    output_resources = {
        TmpTable.AD_WITHOUT_PARENTS: task_executor.create_yt_table_resource(YmapsdfTable.AD),
        TmpTable.AD_GEOM_WITHOUT_EXTRA_CITY_BORDERS: task_executor.create_yt_table_resource(
            TmpTable.AD_GEOM_WITHOUT_EXTRA_CITY_BORDERS,
        ),
        YmapsdfTable.AD_NM: task_executor.create_yt_table_resource(YmapsdfTable.AD_NM),
        YmapsdfTable.LOCALITY: task_executor.create_yt_table_resource(YmapsdfTable.LOCALITY),
        TmpTable.LOG_BAD_AD: task_executor.create_yt_table_resource(TmpTable.LOG_BAD_AD),
    }

    task_executor.execute_task(
        task=BuildAd(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )
