import functools
import json
import http.client
import operator

from maps.garden.libs_server.build.build_defs import BuildOperationString, BuildStatusString

from maps.garden.common_tests.test_utils.tester import DEFAULT_SYSTEM_CONTOUR
from maps.garden.common_tests.test_utils.tester import expected_error_msg
from maps.garden.common_tests.test_utils.constants import RESOURCE_TEMPLATE

RESOURCES = [
    json.loads(RESOURCE_TEMPLATE % (
        "europe_src", "europe", "20201011", "navteq")),
    json.loads(RESOURCE_TEMPLATE % (
        "australia_src", "australia", "20201011", "navteq"))
]


def test_release_name_uniquness(builds_helper):
    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in RESOURCES
    ]
    ymapsdf_builds = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds
    ]

    data = {
        "contour": DEFAULT_SYSTEM_CONTOUR,
        "properties": {
            "release_name": "1.2.0",
        },
        "sources": [
            {
                "name": ymf["module_name"],
                "version": "build_id:" + ymf["build_id"]
            }
            for ymf in ymapsdf_builds
        ]
    }

    print(data)

    expected_value = [
        expected_error_msg(
            "A release with the name '1.2.0' already exists in contour 'unittest'.", "error")
    ]

    response = builds_helper.garden_client.post("modules/world_creator/builds/", json=data)
    assert response.status_code == http.client.CREATED

    build = {"module_name": "world_creator", "build_id": response.get_json()["id"]}
    builds_helper.wait_for_build(build)

    response = builds_helper.garden_client.post("modules/world_creator/builds/", json=data)
    assert response.status_code == http.client.CONFLICT
    assert response.get_json() == expected_value


def test_wrong_module_name(builds_helper):
    MODULE_NAME = "FakeModuleName"
    BUILD_ID = 1

    expected_missing_module_value = [
        expected_error_msg(
            f"Module '{MODULE_NAME}' (version 'None') not found in contour '{DEFAULT_SYSTEM_CONTOUR}'", "error")
    ]
    expected_missing_build_value = [
        expected_error_msg(
            f"{MODULE_NAME} build with ID {BUILD_ID} not found.", "error")
    ]

    # ModuleView
    response = builds_helper.garden_client.get(f"modules/{MODULE_NAME}/")
    assert response.status_code == http.client.NOT_FOUND
    assert response.get_json() == expected_missing_module_value

    # ModuleBuildsView
    module_builds_address = f"modules/{MODULE_NAME}/builds/"

    ymapsdf_src = builds_helper.build_ymapsdf_src(RESOURCES[0])
    response = builds_helper.garden_client.post(
        module_builds_address, json={
            "contour": DEFAULT_SYSTEM_CONTOUR,
            "sources": [
                {
                    "version": ymapsdf_src["version"],
                    "name": "ymapsdf_src"
                }
            ]
        }
    )
    assert response.status_code == http.client.NOT_FOUND
    assert response.get_json() == expected_missing_module_value

    response = builds_helper.garden_client.get(module_builds_address)
    assert response.status_code == http.client.NOT_FOUND
    assert response.get_json() == expected_missing_module_value

    # ModuleBuildView
    module_build_address = f"modules/{MODULE_NAME}/builds/1/"

    response = builds_helper.garden_client.get(module_build_address)
    assert response.status_code == http.client.NOT_FOUND
    assert response.get_json() == expected_missing_build_value

    response = builds_helper.garden_client.put(
        module_build_address, json={'progress': {'status': 'cancelled'}})
    assert response.status_code == http.client.NOT_FOUND
    assert response.get_json() == expected_missing_build_value

    response = builds_helper.garden_client.delete(module_build_address, json={})
    assert response.status_code == http.client.NOT_FOUND
    assert response.get_json() == expected_missing_build_value

    # ErrorLogView
    response = builds_helper.garden_client.get(f"{module_build_address}errors/")
    assert response.status_code == http.client.NOT_FOUND
    response_json = response.get_json()[0]
    assert response_json["type"] == "error"
    assert f"{MODULE_NAME} build with ID {BUILD_ID} not found" in response_json["message"]


def test_wrong_build_id(builds_helper):
    # ModuleBuildView
    expected_value = [expected_error_msg(
        "ymapsdf_src build with ID 1000 not found.", "error")]
    module_build_address = "modules/ymapsdf_src/builds/1000/"

    response = builds_helper.garden_client.get(module_build_address)
    assert response.status_code == http.client.NOT_FOUND
    assert response.get_json() == expected_value

    response = builds_helper.garden_client.put(
        module_build_address, json={'progress': {'status': 'cancelled'}})
    assert response.status_code == http.client.NOT_FOUND
    assert response.get_json() == expected_value

    response = builds_helper.garden_client.delete(module_build_address, json={})
    assert response.status_code == http.client.NOT_FOUND
    assert response.get_json() == expected_value

    # ErrorLogView
    response = builds_helper.garden_client.get(f"{module_build_address}errors/")
    assert response.status_code == http.client.NOT_FOUND
    response_json = response.get_json()[0]
    assert response_json["type"] == "error"
    assert "ymapsdf_src build with ID 1000 not found" in response_json["message"]


def test_wrong_dir_key(builds_helper):
    ymapsdf_src = builds_helper.build_ymapsdf_src(RESOURCES[0])
    builds_helper.build_module("road_graph", [ymapsdf_src])

    response = builds_helper.garden_client.get("storage/?name=road_graph_dir")
    assert response.status_code == http.client.OK
    as_json = response.get_json()[0]
    wrong_key = as_json["key"] + "0000"
    wrong_key_url = as_json['uri'] + "0000"

    expected_value = [expected_error_msg(
        f"Resource with key '{wrong_key}' is not found in the storage", "error")
    ]
    response = builds_helper.garden_client.get(wrong_key_url)
    assert response.status_code == http.client.NOT_FOUND
    assert response.get_json() == expected_value


def test_wrong_method(builds_helper):
    expected_value = [expected_error_msg(
        "The method is not allowed for the requested URL.", "error")]
    response = builds_helper.garden_client.post(
        "module_event_types/", json={})
    assert response.status_code == http.client.METHOD_NOT_ALLOWED
    assert response.get_json() == expected_value

    response = builds_helper.garden_client.put(
        "module_event_types/", json={})
    assert response.status_code == http.client.METHOD_NOT_ALLOWED
    assert response.get_json() == expected_value

    response = builds_helper.garden_client.delete("module_event_types/")
    assert response.status_code == http.client.METHOD_NOT_ALLOWED
    assert response.get_json() == expected_value


def test_exceeded_build_limit(builds_helper):
    ymapdf_builds = [
        builds_helper.build_ymapsdf(builds_helper.build_ymapsdf_src(resource))
        for resource in RESOURCES
    ]

    builds = []
    for i in [0, 1]:
        builds.append(builds_helper.build_module("unique_world_creator", ymapdf_builds, release_name=str(i)))

    response = builds_helper.start_module(
        "unique_world_creator", ymapdf_builds, expected_code=http.client.CONFLICT, release_name="3")
    assert "The builds limit of unique_world_creator exceeded maximum 2" in response[0]["message"]

    builds_helper.delete_build(builds[0])
    response = builds_helper.delete_build(builds[1], expected_code=http.client.FORBIDDEN)
    assert "Builds limit of unique_world_creator exceeded minimum 1" in response[0]["message"]


def test_conflicting_offspring_deployment_modules(builds_helper):
    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in RESOURCES
    ]
    ymapsdf_builds = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds
    ]

    world_build = builds_helper.build_module("world_creator", ymapsdf_builds)
    build_url = world_build["url"]
    builds_helper.build_module("world_creator_deployment", [world_build])

    expected_value = [
        expected_error_msg(
            message="There are builds requiring build you requested to change: unittest:world_creator_deployment:1",
            type="error",
            conflicts=[{
                "url": "http://localhost/modules/world_creator_deployment/builds/1/",
                "contour_name": "unittest",
                "name": "world_creator_deployment",
                "id": 1,
                "status": "completed",
            }])
    ]

    response = builds_helper.garden_client.delete(build_url, json={})
    assert response.status_code == http.client.CONFLICT
    assert response.get_json() == expected_value

    response = builds_helper.garden_client.put(
        build_url, json={'progress': {'status': 'in_progress'}})
    assert response.status_code == http.client.CONFLICT
    assert response.get_json() == expected_value

    # try to suppress errors, should still fail anyway
    response = builds_helper.garden_client.delete(
        build_url,
        json={'ignore_warnings': True})
    assert response.status_code == http.client.CONFLICT
    assert response.get_json() == expected_value

    response = builds_helper.garden_client.put(
        build_url, json={
            'ignore_warnings': True,
            'progress': {'status': 'in_progress'}})
    assert response.status_code == http.client.CONFLICT
    assert response.get_json() == expected_value


def test_malformed_requests(builds_helper):
    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in RESOURCES
    ]
    ymapsdf_builds = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds
    ]

    world_build = builds_helper.build_module("world_creator", ymapsdf_builds)
    build_url = world_build["url"]

    def check_bad_delete(data, error_msg):
        response = builds_helper.garden_client.put(
            build_url,
            json=data,
        )
        assert response.status_code == http.client.BAD_REQUEST
        msg = functools.reduce(
            operator.getitem,
            ["validation_error", "body_params", 0, "msg"],
            response.get_json(),
        )
        assert msg == error_msg

    data = {"progress": {"status": "WRONG_STATUS"}}
    statuses_str = ", ".join(f"'{status}'" for status in BuildStatusString)
    msg = f"value is not a valid enumeration member; permitted: {statuses_str}"
    check_bad_delete(data, msg)

    data = {"progress": "SHOULD_BE_DICT"}
    msg = "value is not a valid dict"
    check_bad_delete(data, msg)


def test_build_with_nonexistent_resource(builds_helper):
    sources = {
        "contour": DEFAULT_SYSTEM_CONTOUR,
        "sources": [
            {
                "version": "key:FAKE_RESOURCE_KEY",
                "name": "europe_src"
            }
        ]
    }
    expected_value = [expected_error_msg(
        "There is no resource 'europe_src' with"
        " key 'FAKE_RESOURCE_KEY'", "error"
        )]
    response = builds_helper.garden_client.post(
        "modules/ymapsdf/builds/", json=sources)
    assert response.status_code == http.client.BAD_REQUEST
    assert response.get_json() == expected_value


def test_build_with_nonexistent_build_id(builds_helper):
    sources = {
        "contour": DEFAULT_SYSTEM_CONTOUR,
        "sources": [
            {
                "version": "build_id:12345",
                "name": "europe_src"
            }
        ]
    }
    expected_value = [expected_error_msg(
        "There is no build '12345' of 'europe_src'", "error"
        )]

    response = builds_helper.garden_client.post(
        "modules/ymapsdf/builds/", json=sources)
    assert response.status_code == http.client.BAD_REQUEST
    assert response.get_json() == expected_value


def test_src_build_with_nonexistent_contour(builds_helper):
    data = {
        "contour": "nonexistent",
        "resources": [RESOURCES[0]],
        "foreign_key": {"id": "1"},
    }
    response = builds_helper.garden_client.post(
        "modules/ymapsdf_src/builds/",
        json=data,
    )
    assert response.status_code == http.client.NOT_FOUND


def test_build_with_nonexistent_contour(builds_helper):
    ymapsdf_src = builds_helper.build_ymapsdf_src(RESOURCES[0])
    data = {
        "contour": "nonexistent",
        "sources": [
            {
                "version": ymapsdf_src["version"],
                "name": "ymapsdf_src"
            }
        ]
    }
    response = builds_helper.garden_client.post(
        "modules/ymapsdf/builds/",
        json=data,
    )
    assert response.status_code == http.client.NOT_FOUND


def test_build_with_nonexistent_contour_modern_ui(builds_helper):
    ymapsdf_src = builds_helper.build_ymapsdf_src(RESOURCES[0])

    data = {
        "action": {
            "name":  BuildOperationString.CREATE,
            "params": [
                {"name": "buildName", "value": "ymapsdf"},
                {"name": "contourName", "value": "nonexistent"},
                {"name": "sources", "value": [
                    "{}:{}".format(ymapsdf_src["module_name"], ymapsdf_src["build_id"])
                ]}
            ]
        }
    }

    response = builds_helper.garden_client.post("build/", json=data)
    assert response.status_code == http.client.NOT_FOUND
