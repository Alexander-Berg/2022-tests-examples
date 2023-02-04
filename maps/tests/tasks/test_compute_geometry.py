import pytest

from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from maps.garden.modules.ymapsdf_osm.defs import YT_CLUSTER
from maps.garden.modules.ymapsdf_osm.lib.compute_geometry import (
    PrepareRelationsWays, PrepareRelationWaysForColonies, ComputeGeometry, AddIsocodesToObjectDetails
)
from maps.garden.modules.ymapsdf_osm.lib.constants import TmpTable, OsmTable
from maps.garden.modules.ymapsdf_osm.lib.schemas import TMP_TABLES_SCHEMAS
from .utils import get_task_executor, get_countries_coverage


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(environment_settings, source_folder="test_compute_geometry")


@pytest.mark.use_local_yt_yql
def test_prepare_relations_ways(task_executor):
    input_resources = {
        TmpTable.OBJECT_DETAILS: task_executor.create_custom_input_yt_table_resource(
            TmpTable.OBJECT_DETAILS,
            schema=TMP_TABLES_SCHEMAS[TmpTable.OBJECT_DETAILS]
        ),
        OsmTable.RELATIONS_MEMBERS: task_executor.create_custom_input_yt_table_resource(OsmTable.RELATIONS_MEMBERS),
    }

    output_resources = {
        TmpTable.RELATIONS_WAYS: task_executor.create_yt_table_resource(TmpTable.RELATIONS_WAYS),
    }

    task_executor.execute_task(
        task=PrepareRelationsWays(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )


@pytest.mark.use_local_yt_yql
def test_prepare_relations_ways_for_colonies(task_executor):
    input_resources = {
        TmpTable.RELATIONS_WAYS: task_executor.create_custom_input_yt_table_resource(TmpTable.RELATIONS_WAYS),
    }

    output_resources = {
        TmpTable.RELATIONS_WAYS_FOR_COLONIES: task_executor.create_yt_table_resource(
            TmpTable.RELATIONS_WAYS_FOR_COLONIES,
        ),
    }

    task_executor.execute_task(
        task=PrepareRelationWaysForColonies(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_compute_geometry(task_executor):
    input_resources = {
        TmpTable.OBJECT_DETAILS: task_executor.create_custom_input_yt_table_resource(
            TmpTable.OBJECT_DETAILS,
            schema=TMP_TABLES_SCHEMAS[TmpTable.OBJECT_DETAILS]
        ),
        TmpTable.RELATIONS_WAYS: task_executor.create_custom_input_yt_table_resource(TmpTable.RELATIONS_WAYS),
        TmpTable.RELATIONS_WAYS_FOR_COLONIES: task_executor.create_custom_input_yt_table_resource(
            TmpTable.RELATIONS_WAYS_FOR_COLONIES,
            schema=TMP_TABLES_SCHEMAS[TmpTable.RELATIONS_WAYS_FOR_COLONIES],
        ),
    }

    output_resources = {
        TmpTable.OBJECT_DETAILS_WITH_RELATIONS: task_executor.create_yt_table_resource(TmpTable.OBJECT_DETAILS_WITH_RELATIONS),
        TmpTable.LOG_BAD_COMPUTE_GEOMETRY: task_executor.create_yt_table_resource(TmpTable.LOG_BAD_COMPUTE_GEOMETRY),
    }

    task_executor.execute_task(
        task=ComputeGeometry(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_add_isocodes_to_object_details(task_executor):
    input_resources = {
        TmpTable.OBJECT_DETAILS_WITH_RELATIONS: task_executor.create_custom_input_yt_table_resource(
            TmpTable.OBJECT_DETAILS_WITH_RELATIONS,
            schema=TMP_TABLES_SCHEMAS[TmpTable.OBJECT_DETAILS]
        ),
        "countries_coverage": get_countries_coverage(task_executor)
    }

    output_resources = {
        TmpTable.OBJECT_DETAILS_WITH_ISOCODES: task_executor.create_yt_table_resource(TmpTable.OBJECT_DETAILS_WITH_ISOCODES),
        TmpTable.LOG_BAD_ISOCODES: task_executor.create_yt_table_resource(TmpTable.LOG_BAD_ISOCODES),
    }

    task_executor.execute_task(
        task=AddIsocodesToObjectDetails(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )
