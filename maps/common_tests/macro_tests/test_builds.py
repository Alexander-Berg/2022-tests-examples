import http.client
import itertools

from maps.garden.common_tests.test_utils.tester import (
    SRC_RESOURCES, RESOURCES, RASP_RESOURCES, DEFAULT_SYSTEM_CONTOUR, wait_until, RESOURCES_OLD
)


def test_builds_from_resources(builds_helper):
    for src_resource in SRC_RESOURCES:
        # first ymapsdf_src build: ok
        builds_helper.build_src("ymapsdf_src", src_resource)

        # build for non source module: fail
        response = builds_helper.garden_client.post(
            "/modules/ymapsdf/builds/",
            json={
                "contour": DEFAULT_SYSTEM_CONTOUR,
                "resources": [src_resource],
                "foreign_key": {"id": "1"},
            })
        assert response.status_code == http.client.BAD_REQUEST

        # build for second ymapsdf_src with the very same properties: fail
        response = builds_helper.garden_client.post(
            "/modules/ymapsdf_src/builds/",
            json={
                "contour": DEFAULT_SYSTEM_CONTOUR,
                "resources": [src_resource],
                "foreign_key": {"id": "1"},
            })
        assert response.status_code == http.client.CONFLICT


def test_release_name_uniquness(builds_helper):
    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in RESOURCES
    ]
    ymapsdf_builds = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds
    ]

    builds_helper.build_module(
        "world_creator", ymapsdf_builds, release_name="1.2.0")
    builds_helper.start_module(
        "world_creator", ymapsdf_builds, release_name="1.2.0", expected_code=http.client.CONFLICT)


def test_deployment(builds_helper):
    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in RESOURCES
    ]
    ymapsdf_builds = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds
    ]

    world_build = builds_helper.build_module(
        "world_creator", ymapsdf_builds)
    world_deployment_build = builds_helper.build_module(
        "world_creator_deployment", [world_build])

    builds_helper.delete_build(world_build, expected_code=http.client.CONFLICT)
    builds_helper.build_exists(world_build)

    for build in [world_deployment_build, world_build]:
        builds_helper.delete_build(build)


def test_tracked_ancestor_builds(builds_helper):
    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in RESOURCES
    ]
    old_ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in RESOURCES_OLD
    ]

    ymapsdf_src_builds_full_info = [
        builds_helper.get_build_info(build["module_name"], build["build_id"])
        for build in ymapsdf_src_builds
    ]

    # check that tracked_ancestor_builds exists, for the source module with tracked_ancestor=true
    for build in ymapsdf_src_builds_full_info:
        assert len(build["tracked_ancestor_builds"]) == 1
        for value in build["tracked_ancestor_builds"]:
            assert value["name"] == "ymapsdf_src"

    # check that tracked_ancestor_builds not exists, for the source module with tracked_ancestor=false
    rasp_export_src = [
        builds_helper.get_build_info(build["module_name"], build["build_id"])
        for build in [
            builds_helper.build_src("rasp_export_src", resource, DEFAULT_SYSTEM_CONTOUR) for resource in RASP_RESOURCES
        ]
    ]
    for build in rasp_export_src:
        assert len(build["tracked_ancestor_builds"]) == 0

    # check that tracked_ancestor_builds list from module traits is processed correctly
    ymapsdf_builds = [builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds]
    old_ymapsdf_builds = [builds_helper.build_ymapsdf(source) for source in old_ymapsdf_src_builds]
    ymapsdf_builds_full_info = [
        builds_helper.get_build_info(build["module_name"], build["build_id"])
        for build in ymapsdf_builds
    ]
    for build in ymapsdf_builds_full_info:
        assert len(build["tracked_ancestor_builds"]) == 1
        for value in build["tracked_ancestor_builds"]:
            assert value["name"] == "ymapsdf_src"

    world_build = builds_helper.build_module(
        "world_creator",
        sources=old_ymapsdf_builds,
        contour_name=DEFAULT_SYSTEM_CONTOUR,
        release_name="world_build",
        ignore_warnings=True
    )
    geocoder_indexer = builds_helper.build_module("geocoder_indexer", ymapsdf_builds)

    offline_cache = builds_helper.start_module("offline_cache", [world_build, geocoder_indexer], ignore_warnings=True)
    builds_helper.wait_for_build(offline_cache)
    offline_cache = builds_helper.get_build_info(offline_cache["module_name"], offline_cache["build_id"])

    ymapsdf_ancestors = list(
        itertools.chain.from_iterable(build["tracked_ancestor_builds"] for build in ymapsdf_src_builds_full_info)
    )
    assert len(offline_cache["tracked_ancestor_builds"]) == len(ymapsdf_ancestors)
    assert all(ancestor in offline_cache["tracked_ancestor_builds"] for ancestor in ymapsdf_ancestors)

    # check the correctness of the information in /module_statistics/
    module_statistics = builds_helper.garden_client.get(
        f"/module_statistics/?module=offline_cache&contour={DEFAULT_SYSTEM_CONTOUR}"
    ).get_json()

    assert len(module_statistics) == 1
    assert len(module_statistics[0]["tracked_ancestor_builds"]) == len(ymapsdf_ancestors)
    assert all(ancestor in module_statistics[0]["tracked_ancestor_builds"] for ancestor in ymapsdf_ancestors)


def test_failing_module(builds_helper, smtp_server):
    ymapsdf_src = builds_helper.build_ymapsdf_src(RESOURCES[0])

    emails_count = len(smtp_server["emails"])

    build = builds_helper.build_module("failing_module", [ymapsdf_src], expected_status="failed")

    assert wait_until(lambda: len(smtp_server["emails"]) == emails_count + 1)

    response = builds_helper.garden_client.put(
        build["url"],
        json={"progress": {"status": "in_progress"}}
    )
    assert response.status_code == http.client.OK

    assert wait_until(lambda: len(smtp_server["emails"]) == emails_count + 2)

    failing_module_builds = builds_helper.module_builds("failing_module")
    assert len(failing_module_builds) == 1
    build_id = failing_module_builds[0]["id"]

    response = builds_helper.garden_client.get(f"modules/failing_module/builds/{build_id}/errors/")
    assert response.status_code == http.client.OK
    error_log = response.get_json()
    assert error_log
    assert error_log["buildException"]
    assert error_log["buildException"]["exceptionType"] == "RuntimeError"
    assert not error_log["failedTasks"]
