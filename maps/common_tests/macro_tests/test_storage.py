import hashlib
import http.client
import json

from maps.garden.common_tests.test_utils.tester import RESOURCES
from maps.garden.common_tests.test_utils.constants import RESOURCE_TEMPLATE, ROAD_GRAPH_DATA


def _build_road_graph(builds_helper):
    ymapsdf_src = builds_helper.build_ymapsdf_src(RESOURCES[0])
    return builds_helper.build_module("road_graph", [ymapsdf_src])


def test_path_like_reqource_name(builds_helper):
    path_like_name = 'path/like/name'
    resource = json.loads(RESOURCE_TEMPLATE % (path_like_name, "europe_src", "europe", "navteq"))

    builds_helper.build_src("extra_resources", resource)
    builds_helper.check_resources_count(path_like_name, 1)


def test_uri_and_handler(builds_helper):
    _build_road_graph(builds_helper)

    response = builds_helper.garden_client.get("storage/?name=road_graph_dir")
    assert response.status_code == http.client.OK

    for resource in response.get_json():
        if resource['properties'].get('is_empty', False):
            continue
        assert 'uri' in resource
        assert resource['uri'].startswith("http"), "URI: " + resource['uri']

        dir_response = builds_helper.garden_client.get(resource['uri'])
        assert dir_response.status_code == http.client.OK

        files = dir_response.get_json()['content']['files']
        assert len(files) == 1
        assert files[0]["uri"].startswith("http")
        assert files[0]["sha1"] == hashlib.sha1(ROAD_GRAPH_DATA).hexdigest()
        break
    else:
        assert False


def test_limit(builds_helper):
    _build_road_graph(builds_helper)

    response = builds_helper.garden_client.get("storage/?limit=400")
    assert response.status_code == http.client.OK
    assert len(response.get_json()) == 3

    response = builds_helper.garden_client.get("storage/?limit=1")
    assert response.status_code == http.client.OK
    assert len(response.get_json()) == 1

    response = builds_helper.garden_client.get("storage/?limit=wrong_value")
    assert response.status_code == http.client.OK
    assert len(response.get_json()) == 3


def test_offset(builds_helper):
    _build_road_graph(builds_helper)

    response = builds_helper.garden_client.get("storage/?offset=1")
    assert response.status_code == http.client.OK
    assert len(response.get_json()) == 2

    response = builds_helper.garden_client.get("storage/?offset=wrong_value")
    assert response.status_code == http.client.OK
    assert len(response.get_json()) == 3


def test_properties(builds_helper):
    _build_road_graph(builds_helper)

    response = builds_helper.garden_client.get("storage/?non_existent_property=1")
    assert response.status_code == http.client.OK
    assert len(response.get_json()) == 0

    response = builds_helper.garden_client.get("storage/?vendor=yandex")
    assert response.status_code == http.client.OK
    assert len(response.get_json()) == 0

    response = builds_helper.garden_client.get("storage/?vendor=navteq")
    assert response.status_code == http.client.OK
    assert len(response.get_json()) == 1


def test_uri(builds_helper):
    _build_road_graph(builds_helper)

    response = builds_helper.garden_client.get("storage/?name=road_graph")
    assert response.status_code == http.client.OK

    for resource in response.get_json():
        if resource['properties'].get('is_empty', False):
            continue
        assert 'uri' in resource
        break
    else:
        # should never arrive here
        assert False


def test_build_resources(builds_helper):
    build = _build_road_graph(builds_helper)

    response = builds_helper.garden_client.get(f"storage/?build={build['version']}")
    assert response.status_code == http.client.OK
    assert len(response.get_json()) == 2

    response = builds_helper.garden_client.get("storage/")
    assert response.status_code == http.client.OK
    assert len(response.get_json()) > 2
