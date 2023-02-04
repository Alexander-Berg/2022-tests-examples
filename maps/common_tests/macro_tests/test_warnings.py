import json
import http.client

from maps.garden.common_tests.test_utils.constants import RESOURCE_TEMPLATE
from maps.garden.common_tests.test_utils.tester import expected_error_msg

resources = {
    "europe_old": json.loads(RESOURCE_TEMPLATE % (
        "europe_src", "europe", "20201011", "navteq")),
    "europe_new": json.loads(RESOURCE_TEMPLATE % (
        "europe_src", "europe", "20201012", "navteq")),
    "europe_east": json.loads(RESOURCE_TEMPLATE % (
        "europe_east_src", "europe_east", "20201012", "navteq")),
    "europe_west": json.loads(RESOURCE_TEMPLATE % (
        "europe_west_src", "europe_west", "20201012", "navteq")),
    "australia": json.loads(RESOURCE_TEMPLATE % (
        "australia_src", "australia", "20201011", "navteq"))
}


def _build_ymapsdf(builds_helper, name):
    ymapsdf_src = builds_helper.build_ymapsdf_src(resources[name])
    return builds_helper.build_ymapsdf(ymapsdf_src)


def _check_warning_response(response, pattern, value):
    assert len(response) == 1
    item = response[0]

    message = item.pop("message")
    assert message.startswith(pattern)
    result = json.loads(message[len(pattern):].strip())
    if isinstance(result, list):
        result.sort()
    assert result == value


def test_freshness(builds_helper):
    europe_old = _build_ymapsdf(builds_helper, "europe_old")
    europe_new = _build_ymapsdf(builds_helper, "europe_new")
    australia = _build_ymapsdf(builds_helper, "australia")

    # test failed build request (newer version available)
    response = builds_helper.start_module(
        "world_creator",
        [europe_old, australia],
        expected_code=http.client.BAD_REQUEST,
    )

    _check_warning_response(
        response=response,
        pattern="Newer version is available for Ymapsdf with properties",
        value={
            "shipping_date": "20201012",
            "region": "europe",
            "vendor": "navteq",
            "autostarted": False,
        },
    )
    # test succeed build request
    builds_helper.start_module("world_creator", [europe_new, australia])


def test_conflicting_offspring_non_deployment_modules(builds_helper):
    europe_old = _build_ymapsdf(builds_helper, "europe_old")
    australia = _build_ymapsdf(builds_helper, "australia")
    world_build = builds_helper.build_module(
        "world_creator", [europe_old, australia])
    builds_helper.build_module("world_creator_deployment", [world_build])

    expected_value = [
        expected_error_msg(
            "There are builds using the build you requested to change: unittest:world_creator:1",
            "warning",
            conflicts=[{
                "url": "http://localhost/modules/world_creator/builds/1/",
                "contour_name": "unittest",
                "name": "world_creator",
                "id": 1,
                "status": "completed",
            }])
    ]
    ymapdf_build_url = [
        "modules/ymapsdf/builds/{0}/".format(build_nr)
        for build_nr in [1, 2]]

    response = builds_helper.garden_client.delete(ymapdf_build_url[1], json={})
    assert response.status_code == http.client.CONFLICT
    assert response.get_json() == expected_value

    response = builds_helper.garden_client.put(
        ymapdf_build_url[1],
        json={'progress': {'status': 'in_progress'}})
    assert response.status_code == http.client.CONFLICT
    assert response.get_json() == expected_value

    # try to suppress warnings, should succeed now
    response = builds_helper.garden_client.delete(
        ymapdf_build_url[0],
        json={'ignore_warnings': True})
    assert response.status_code == http.client.OK

    response = builds_helper.garden_client.put(
        ymapdf_build_url[1],
        json={
            'ignore_warnings': True,
            'progress': {'status': 'in_progress'}
        }
    )
    assert response.status_code == http.client.OK
