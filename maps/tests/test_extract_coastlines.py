import pytest

from maps.garden.sdk import test_utils
from maps.garden.sdk.core import Version
from maps.garden.sdk.test_utils.geometry import convert_wkb_to_wkt
from maps.garden.modules.osm_coastlines_src import defs
from maps.garden.modules.osm_coastlines_src.lib import graph
from maps.garden.modules.osm_coastlines_src.tests.geojson_to_shp import convert_geojson_to_shp

from yatest.common import test_source_path


@pytest.mark.use_local_yt("hahn")
def test_osm_to_yt_task(mocker, environment_settings):
    input_geojson_file_path = test_source_path("data/water_polygons.json")
    input_shp_file_path = "water_polygons.shp"
    input_spx_file_path = "water_polygons.shx"

    convert_geojson_to_shp(input_geojson_file_path, input_shp_file_path)

    cook = test_utils.GraphCook(environment_settings)

    graph.fill_graph(cook.target_builder())

    urlmap = {
        "https://proxy.sandbox.yandex-team.ru/111" : open(input_shp_file_path, "rb"),
        "https://proxy.sandbox.yandex-team.ru/222" : open(input_spx_file_path, "rb")
    }

    input_resource = cook.create_input_resource(defs.OSM_COASTLINES_URL)
    input_resource.version = Version(
        properties={
            "file_list": [
                {
                    "name": "water_polygons.shp",
                    "url": "https://proxy.sandbox.yandex-team.ru/111"
                },
                {
                    "name": "water_polygons.shx",
                    "url": "https://proxy.sandbox.yandex-team.ru/222"
                }
            ],
            "shipping_date": "20211014",
            "vendor": "osm",
        }
    )

    mocker.patch("maps.garden.sdk.resources.remote_dir.urlopen", lambda url: urlmap[url])
    out_resources = test_utils.execute(cook)

    expecting_names = [
        "coastlines_geom",
    ]
    result = []
    for name in expecting_names:
        resource_name = defs.OSM_COASTLINES_SRC.resource_name(name)
        assert resource_name in out_resources

        yt_table = out_resources[resource_name]
        yt_table_data = list(yt_table.read_table())
        convert_wkb_to_wkt(yt_table_data)
        result.append({name: yt_table_data})

    return result
