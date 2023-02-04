import pytest
import hashlib
import json

from yatest.common import test_source_path

from maps.garden.sdk import test_utils
from maps.garden.sdk.core import Version
from maps.garden.sdk.extensions import get_full_resource_name
from maps.garden.modules.osm_src import defs as osm_src_defs
from maps.garden.modules.osm_src.lib import graph as osm_src_graph
from maps.garden.modules.osm_to_yt import defs
from maps.garden.modules.osm_to_yt.lib import graph
from maps.garden.modules.osm_borders_src import defs as osm_borders_src_defs
from maps.garden.modules.osm_borders_src.lib import graph as osm_borders_src
from maps.garden.libs.osm.test_utils.osm_to_pbf import convert_osm_to_pbf
from .utils import get_countries_coverage, get_regions_coverage, get_water_regions_coverage, TEST_COVERAGE_PROPERTIES

TEST_REGION1 = "cis1"
TEST_REGION2 = "cis2"


def _create_yt_file_resource(cook, yt_client, resource_name: str, properties: dict, data: bytearray):
    resource = cook.create_input_resource(resource_name)
    resource.version = Version(properties=properties)
    resource.server = "hahn"
    resource.load_environment_settings(cook.environment_settings)
    yt_client.create("file", path=resource.path, recursive=True, ignore_existing=True)
    yt_client.write_file(resource.path, data)


def _with_region_vendor(resource_name: str, region: str):
    return get_full_resource_name(resource_name, region, osm_src_defs.VENDOR)


def _get_water_regions() -> dict:
    return json.load(open(test_source_path("data/water_regions.json"), "r"))


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(graph.fill_graph)


@pytest.mark.parametrize("region", [TEST_REGION1, TEST_REGION2])
@pytest.mark.use_local_yt("hahn")
def test_osm_to_yt_task(mocker, requests_mock, environment_settings, yt_client, region):
    mocker.patch("maps.garden.modules.osm_src.defs.REGIONS", [region])
    mocker.patch("maps.garden.modules.osm_borders_src.lib.water_regions_coverage.get_water_regions", _get_water_regions)

    test_pbf = f"test_{region}.pbf"
    test_pbf_url = "https://proxy.sandbox.yandex-team.ru/2490779955"
    convert_osm_to_pbf(test_source_path("data/osm_to_yt.osm"), test_pbf)

    cook = test_utils.GraphCook(environment_settings)

    osm_src_graph.fill_graph(cook.input_builder())
    osm_borders_src.fill_graph(cook.input_builder())
    graph.fill_graph(cook.target_builder())

    _create_yt_file_resource(
        cook,
        yt_client,
        resource_name=osm_borders_src_defs.OSM_BORDERS_SRC.resource_name("countries_coverage"),
        properties=TEST_COVERAGE_PROPERTIES,
        data=get_countries_coverage(cook.environment_settings),
    )

    _create_yt_file_resource(
        cook,
        yt_client,
        resource_name=osm_borders_src_defs.OSM_BORDERS_SRC.resource_name("regions_coverage"),
        properties=TEST_COVERAGE_PROPERTIES,
        data=get_regions_coverage(cook.environment_settings),
    )

    _create_yt_file_resource(
        cook,
        yt_client,
        resource_name=osm_borders_src_defs.OSM_BORDERS_SRC.resource_name("water_regions_coverage"),
        properties=TEST_COVERAGE_PROPERTIES,
        data=get_water_regions_coverage(cook.environment_settings),
    )

    input_resource = cook.create_input_resource(_with_region_vendor(osm_src_defs.OSM_PBF_URL, region))
    input_resource.version = Version(
        properties={
            "shipping_date": "20211015",
            "region": region,
            "vendor": "osm",
            "file_list": [
                {
                    "name": test_pbf,
                    "url": test_pbf_url,
                    "md5": hashlib.md5(open(test_pbf, "rb").read()).hexdigest()
                }
            ]
        }
    )

    input_resource = cook.create_input_resource(_with_region_vendor(osm_src_defs.OSM_FLAG, region))
    input_resource.version = Version(
        properties={
            "shipping_date": "20211015",
            "region": region,
            "vendor": "osm",
        }
    )

    input_resource = cook.create_input_resource("osm_borders_src_countries_geom")
    input_resource.write_table([{"osm_id": 1, "isocode": "RU", "shape": "POLYGON ((6 1, 8 1, 8 4, 6 4, 6 1))"}])
    input_resource.version = Version(
        properties={
            "shipping_date": "20211014",
            "vendor": "osm"
        }
    )

    input_resource = cook.create_input_resource("osm_borders_src_regions_geom")
    input_resource.write_table([{"region": "cis1", "shape": "POLYGON ((6.1 1.1, 8.1 1.1, 8.1 4.1, 6.1 1.1))"}])
    input_resource.version = Version(
        properties={
            "shipping_date": "20211014",
            "vendor": "osm"
        }
    )

    with open(test_pbf, "rb") as f:
        mocker.patch("maps.garden.sdk.resources.remote_dir.urlopen", lambda url: f)
        out_resources = test_utils.execute(cook)

    expecting_tables_names = [
        "countries_geom",
        "regions_geom",
        "nodes",
        "nodes_tags",
        "ways",
        "ways_nodes",
        "ways_tags",
        "relations",
        "relations_tags",
        "relations_members",
    ]
    expecting_files_names = [
        "countries_coverage"
    ]
    result = []
    for name in expecting_tables_names:
        resource_name = _with_region_vendor(defs.OSM_TO_YT.resource_name(name), region)
        assert resource_name in out_resources

        yt_table = out_resources[resource_name]
        yt_table_data = list(yt_table.read_table())
        result.append({name: yt_table_data})

    for name in expecting_files_names:
        resource_name = _with_region_vendor(defs.OSM_TO_YT.resource_name(name), region)
        assert resource_name in out_resources

        yt_file = out_resources[resource_name]
        yt_file_data = yt_file.read_file().read()
        assert yt_file_data

    return result
