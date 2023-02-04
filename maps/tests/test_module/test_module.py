from collections import namedtuple
import math
from unittest import mock
import pytest
import xml.etree.ElementTree as xml

from yatest.common import test_source_path

from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import ymapsdf
from maps.garden.sdk.test_utils.ymapsdf_schema import YmapsdfSchemaManager
from maps.garden.sdk.extensions import resource_namer
from maps.garden.sdk.yt import YtTableResource
from maps.garden.sdk.resources import FileResource

from maps.garden.modules.ymapsdf import defs as ymapsdf_defs
from maps.garden.modules.export_cams.lib import (
    graph, extract as export_cams_extract)


TEST_REGIONS = [
    ("cis1", "yandex"),
    ("tr", "yandex")
]

# local cluster name used in yql recipe
YT_CLUSTER = "plato"

RELEASE_NAME = "20.04.25-0"

SOME_TIMESTAMP = 1586854629

XML_NAMESPACES = {
    "ym": "http://maps.yandex.ru/ymaps/1.x",
    "gml": "http://www.opengis.net/gml",
    "jm": "http://maps.yandex.ru/jams/1.x"
}

RUSSIA_REGION_ID = 225

KINDA_TURKEY_REGION_BORDER_LONGITUDE = 40

SCHEMA_MANAGER = YmapsdfSchemaManager()


RoadEvent = namedtuple("RoadEvent", [
    "event_id",
    "tags",
    "description",
    "begin",
    "end",
    "pos_x",
    "pos_y",
    "course"
])


def populate_input_ymapsdf_tables(cook):
    resources = dict((r.name, r) for r in cook.input_resources())

    for region, vendor in TEST_REGIONS:
        data_dir = test_source_path(f"data/input/{region}_{vendor}/")
        for table_name in graph.INPUT_YMAPSDF_TABLES:
            resource_name = ymapsdf_defs.FINAL_STAGE.resource_name(
                table_name, region, vendor)
            # Schema is provided only to automatically populate 'x', 'xmin',
            # etc. columns. While not used in tested module, they are
            # required by ymapsdf yt table schema.
            test_utils.data.populate_resource_with_data(
                resources[resource_name],
                data_dir,
                table_name,
                schema=SCHEMA_MANAGER.yt_schema_for_sorted_table(table_name))


def read_table_from_file(filepath, wkt_fields):
    return list(test_utils.geometry.convert_wkt_to_wkb_in_jsonl_file(
        filepath, ["pos", "poly"]))


def validate_road_events_tables(yt_client, resources):
    for region, vendor in TEST_REGIONS:
        resource_name = resource_namer.get_full_resource_name(
            graph.ROAD_EVENTS_RESOURCE_NAME, region, vendor)
        resource = resources[resource_name]
        assert isinstance(resource.resource, YtTableResource)
        resource_data = list(yt_client.read_table(resource.path))
        reference_data_path = test_source_path(
            f"data/output/{region}_{vendor}/road_events.jsonl")
        reference_data = read_table_from_file(
            reference_data_path, ["pos", "poly"])
        assert resource_data == reference_data


def read_road_events_from_xml(filepath):
    road_events = {}

    root = xml.parse(filepath).getroot()

    event_xpath = "./ym:GeoObjectCollection/gml:featureMember/ym:GeoObject"
    for event_element in root.iterfind(event_xpath, XML_NAMESPACES):
        event_id = event_element.get(f"{{{XML_NAMESPACES['gml']}}}id")

        metadata_element = event_element.find(
            "./gml:metaDataProperty/jm:JamMetaData",
            XML_NAMESPACES)
        begin = int(metadata_element.find(
            "./jm:time/jm:begin", XML_NAMESPACES).text)
        end = int(metadata_element.find(
            "./jm:time/jm:end", XML_NAMESPACES).text)
        course = float(metadata_element.find(
            "./jm:course", XML_NAMESPACES).text)
        tags = frozenset((
            tag_element.text for tag_element in metadata_element.iterfind(
                "./jm:eventTags/jm:tag", XML_NAMESPACES)))

        description = event_element.find(
            "./gml:description", XML_NAMESPACES).text
        pos_x, pos_y = [
            float(s) for s in event_element.find(
                "./gml:Point/gml:pos", XML_NAMESPACES).text.split()]

        road_events[event_id] = RoadEvent(
            event_id=event_id,
            tags=tags,
            description=description,
            begin=begin,
            end=end,
            pos_x=pos_x,
            pos_y=pos_y,
            course=course)

    return road_events


def validate_road_event(generated, reference):
    assert generated.event_id == reference.event_id
    assert generated.tags == reference.tags
    assert generated.description == reference.description
    assert generated.begin == reference.begin
    assert generated.end == reference.end
    assert math.isclose(generated.pos_x, reference.pos_x, abs_tol=1e-6)
    assert math.isclose(generated.pos_y, reference.pos_y, abs_tol=1e-6)
    assert math.isclose(generated.course, reference.course, abs_tol=1e-6)


def validate_result_xml(resources):
    resource = resources[graph.ROAD_EVENTS_RESOURCE_NAME]
    assert isinstance(resource, FileResource)
    resource.ensure_available()

    reference_road_events = read_road_events_from_xml(
        test_source_path("data/output/result.xml"))
    generated_road_events = read_road_events_from_xml(
        resource.path())

    assert len(reference_road_events) == len(generated_road_events)
    for road_event_id, road_event in reference_road_events.items():
        assert road_event_id in generated_road_events
        validate_road_event(generated_road_events[road_event_id], road_event)


class GeobaseLookupMock:
    def get_region_id_by_location(self, lon, lat):
        return RUSSIA_REGION_ID \
            if lon > KINDA_TURKEY_REGION_BORDER_LONGITUDE \
            else export_cams_extract.TURKEY_REGION_ID

    def is_id_in(self, inner_region_id, outer_region_id):
        return inner_region_id == outer_region_id


@mock.patch.object(
    graph.ConvertTask,
    "_get_geobase_lookup",
    return_value=GeobaseLookupMock())
@mock.patch(
    "maps.garden.modules.export_cams.lib.xml._get_start_time",
    return_value=SOME_TIMESTAMP)
@mock.patch.object(graph.UploadTask, "load_environment_settings")
@mock.patch.object(graph.UploadTask, "__call__")
@pytest.mark.use_local_yt_yql
def test_module(
        mocked_upload_call,
        mocked_upload_load_environment,
        mocked_get_start_time,
        mocked_get_geobase_lookup,
        environment_settings,
        yt_client):

    cook = test_utils.GraphCook(environment_settings)

    ymapsdf.fill_graph(cook.input_builder(), TEST_REGIONS)
    graph.fill_graph(cook.target_builder(), TEST_REGIONS)

    cook.create_build_params_resource(properties={"release": RELEASE_NAME, "release_name": RELEASE_NAME})

    ymapsdf.create_final_resources_for_many_regions(
        cook, TEST_REGIONS, graph.INPUT_YMAPSDF_TABLES)

    populate_input_ymapsdf_tables(cook)

    resources = test_utils.execute(cook)

    assert graph.ROAD_EVENTS_UPLOADED_MARKER_RESOURCE_NAME in resources

    validate_road_events_tables(yt_client, resources)

    validate_result_xml(resources)
