from library.python import resource
import yt.yson as yson

from maps.analyzer.services.eta_comparison.lib.get_route_data import GisRouteDataParser


def test_router_parser():
    parser = GisRouteDataParser()
    response = yson.loads(resource.find('2gis_response'))
    geometry, dist, time = parser.parse(response)
    geometry = [[*point] for point in geometry]  # format to array

    assert geometry == yson.loads(resource.find('2gis_expected_geometry'))
    assert dist == 10267
    assert time == 892
