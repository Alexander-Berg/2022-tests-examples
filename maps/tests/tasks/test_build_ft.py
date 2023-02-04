import pytest
import logging
from shapely.geometry import Point

from maps.libs.ymapsdf.py.ft_type import FtType
from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from maps.garden.libs.osm.osm_object.osm_object_id import OsmObjectId, OsmObjectType
from maps.garden.modules.ymapsdf_osm.lib.common.errors import InvalidOsmDataError
from maps.garden.modules.ymapsdf_osm.defs import YT_CLUSTER
from maps.garden.modules.ymapsdf_osm.lib.build_ft import BuildFt, FtGeomRow, _get_ft_type
from maps.garden.modules.ymapsdf_osm.lib.constants import TmpTable, YmapsdfTable
from maps.garden.modules.ymapsdf_osm.lib.schemas import TMP_TABLES_SCHEMAS
from maps.garden.modules.ymapsdf_osm.lib.bad_objects_log import LogQueue
from .utils import get_task_executor

logger = logging.getLogger("test_build_ft")


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(environment_settings, source_folder="test_build_ft")


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_build_ft(task_executor):
    input_resources = {
        TmpTable.FT_OBJECT_DETAILS: task_executor.create_custom_input_yt_table_resource(
            TmpTable.FT_OBJECT_DETAILS,
            schema=TMP_TABLES_SCHEMAS[TmpTable.OBJECT_DETAILS]
        ),
        TmpTable.FT_TYPE_TO_RUBRIC: task_executor.create_custom_input_yt_table_resource(
            TmpTable.FT_TYPE_TO_RUBRIC,
            schema=TMP_TABLES_SCHEMAS[TmpTable.FT_TYPE_TO_RUBRIC]
        )
    }

    output_resources = {
        TmpTable.FT_WITHOUT_ISOCODES: task_executor.create_yt_table_resource(YmapsdfTable.FT),
        TmpTable.FT_TAGS: task_executor.create_yt_table_resource(TmpTable.FT_TAGS),
        YmapsdfTable.FT_GEOM: task_executor.create_yt_table_resource(YmapsdfTable.FT_GEOM),
        TmpTable.LOG_BAD_FT: task_executor.create_yt_table_resource(TmpTable.LOG_BAD_FT),
    }

    task_executor.execute_task(
        task=BuildFt(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )


def test_ft_geom_row_create():
    good_point = Point(10, 20)
    bad_point_1 = Point(1, -89.9)
    bad_point_2 = Point(100, 89.9)

    FtGeomRow.create(0, good_point)
    with pytest.raises(InvalidOsmDataError):
        FtGeomRow.create(0, bad_point_1)
    with pytest.raises(InvalidOsmDataError):
        FtGeomRow.create(0, bad_point_2)


@pytest.mark.parametrize(
    ("tags", "expected"),
    [
        (
            {
                "aeroway" : "aerodrome"
            },
            FtType.TRANSPORT_AIRPORT_DOMESTIC
        ),
        (
            {
                "aeroway" : "aerodrome",
                "aerodrome" : "international"
            },
            FtType.TRANSPORT_AIRPORT
        ),
        (
            {
                "aeroway" : "aerodrome",
                "aerodrome:type" : "international",
                "aerodrome" : "public"
            },
            FtType.TRANSPORT_AIRPORT
        ),
    ]
)
def test_aeroway_conversion(tags, expected):
    log_queue = LogQueue("test_names", logger)
    object_id = OsmObjectId(type=OsmObjectType.WAY, id=1)
    result = _get_ft_type(
        object_id.to_ymapsdf_id(index=0),
        tags,
        log_queue=log_queue)
    assert result == expected
