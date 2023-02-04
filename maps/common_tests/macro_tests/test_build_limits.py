import http.client
import pytest

from maps.garden.libs_server.common.contour_manager import ContourManager

from maps.garden.common_tests.test_utils import tester


@pytest.fixture
def prepare_data(db):
    contour_manager = ContourManager(db)
    contour_manager.create("apollo_test_contour", "apollo")


def test_build_limits_not_remove_on_exceeded(builds_helper):
    """
    Module `unique_world_creator` has `remove_on_exceed=false` in its traits.
    Expect that new builds are not created when the max limit is reached.
    Expect that builds are prohibited from deletion with the min limit is reached.
    """
    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in tester.RESOURCES
    ]
    ymapsdf_builds = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds
    ]

    # max build limit is 2 => expect both builds are successful
    build1 = builds_helper.build_module("unique_world_creator", sources=ymapsdf_builds, release_name="1")
    build2 = builds_helper.build_module("unique_world_creator", sources=ymapsdf_builds, release_name="2")

    # max build limit is reached => build can't start
    builds_helper.start_module(
        "unique_world_creator",
        sources=ymapsdf_builds,
        expected_code=http.client.CONFLICT,
        release_name="3")

    # the previously created builds are not autoremoved
    assert builds_helper.build_exists(build1)
    assert builds_helper.build_exists(build2)

    # min build limit is 1 => deletion is successful
    builds_helper.delete_build(build1)

    # min build limit is reached => build can't be removed
    builds_helper.delete_build(build2, expected_code=http.client.FORBIDDEN)

    assert not builds_helper.build_exists(build1)
    assert builds_helper.build_exists(build2)


def test_build_limits_remove_on_exceed(builds_helper):
    """Test automatic removal of builds on completion.

    Check that after the second build of world_creator is completed, the
    first build is removed automatically, since world_creater has in its
    configuration `remove_on_exceed` set to true, and `max` to 1.
    """
    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in tester.RESOURCES
    ]
    ymapsdf_builds = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds
    ]

    # remove_on_exceed is true => expect both builds are successfull
    build1 = builds_helper.build_module("world_creator", sources=ymapsdf_builds, release_name="1")
    build2 = builds_helper.build_module("world_creator", sources=ymapsdf_builds, release_name="2")

    # max build limit is 1 => expect that the old build is deleted
    # Garden deletes the first build right after the second one is
    # completed so we have to wait for it a little
    assert tester.wait_until(
        lambda: not builds_helper.build_exists(build1))
    assert builds_helper.build_exists(build2)


def test_build_limits_not_remove_on_exceed_in_contours(builds_helper, prepare_data):
    """
    If remove_on_exceed is false then build limits are checked
    when a new build is starting.
    Build limits are checked per contour.
    """
    # Build ymapsdf in default system_contour
    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in tester.RESOURCES
    ]
    ymapsdf_builds = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds
    ]

    # max build limit is 2 => expect both builds are successful
    system_build1 = builds_helper.build_module(
        "unique_world_creator",
        sources=ymapsdf_builds,
        contour_name=tester.DEFAULT_SYSTEM_CONTOUR,
        release_name="1")
    system_build2 = builds_helper.build_module(
        "unique_world_creator",
        sources=ymapsdf_builds,
        contour_name=tester.DEFAULT_SYSTEM_CONTOUR,
        release_name="2")

    # max build limit is 2 for each contour => expect build is successful in other contour
    user_build1 = builds_helper.build_module(
        "unique_world_creator",
        sources=ymapsdf_builds,
        contour_name="apollo_test_contour",
        release_name="1")
    user_build2 = builds_helper.build_module(
        "unique_world_creator",
        sources=ymapsdf_builds,
        contour_name="apollo_test_contour",
        release_name="2")

    # max build limit is reached => build can't start
    builds_helper.start_module(
        "unique_world_creator",
        sources=ymapsdf_builds,
        contour_name="apollo_test_contour",
        expected_code=http.client.CONFLICT,
        release_name="3")

    assert builds_helper.build_exists(system_build1)
    assert builds_helper.build_exists(system_build2)
    assert builds_helper.build_exists(user_build1)
    assert builds_helper.build_exists(user_build2)


def test_build_limits_remove_on_exceed_in_contours(builds_helper, prepare_data):
    """
    If remove_on_exceed is true then build limits are not checked
    when a new build is starting.
    But there is an autoremover that removes old builds per contour.
    """
    # Build ymapsdf in default system_contour
    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in tester.RESOURCES
    ]
    ymapsdf_builds = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds
    ]

    system_build = builds_helper.build_module(
        "world_creator",
        sources=ymapsdf_builds,
        contour_name=tester.DEFAULT_SYSTEM_CONTOUR,
        release_name="1")

    # max build limit is 1 for each contour => expect build is successful in other contour
    user_build1 = builds_helper.build_module(
        "world_creator",
        sources=ymapsdf_builds,
        contour_name="apollo_test_contour",
        release_name="2")

    assert builds_helper.build_exists(system_build)
    assert builds_helper.build_exists(user_build1)

    # remove_on_exceed is true => expect a new build is successful and old build is deleted
    user_build2 = builds_helper.build_module(
        "world_creator",
        sources=ymapsdf_builds,
        contour_name="apollo_test_contour",
        release_name="3")

    assert builds_helper.build_exists(system_build)
    # Build removal takes time. So we should wait a little
    assert tester.wait_until(
        lambda: not builds_helper.build_exists(user_build1))
    assert builds_helper.build_exists(user_build2)
