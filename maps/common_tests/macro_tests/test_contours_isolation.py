import http.client

from maps.garden.libs_server.common.mongo import mongodb_by_config

from maps.garden.libs_server.common.contour_manager import ContourManager

from maps.garden.common_tests.test_utils import tester


def test_contours_isolation(builds_helper):
    db = mongodb_by_config({})
    contour_manager = ContourManager(db)
    contour_manager.create("vasya_test_contour", "vasya")

    system_src_build = builds_helper.build_ymapsdf_src(
        tester.RESOURCES[0],
        contour_name=tester.DEFAULT_SYSTEM_CONTOUR)
    user_src_build = builds_helper.build_ymapsdf_src(
        tester.RESOURCES[1],
        contour_name="vasya_test_contour")

    # system contour can use source build from itself
    builds_helper.start_ymapsdf(
        system_src_build,
        contour_name=tester.DEFAULT_SYSTEM_CONTOUR,
        expected_code=http.client.CREATED)

    # system contour can't use builds from user contours
    builds_helper.start_ymapsdf(
        user_src_build,
        contour_name=tester.DEFAULT_SYSTEM_CONTOUR,
        expected_code=http.client.BAD_REQUEST)

    # user contour can use builds from system contours
    builds_helper.start_ymapsdf(
        system_src_build,
        contour_name="vasya_test_contour",
        expected_code=http.client.CREATED)

    # user contour can use source build from itself
    builds_helper.start_ymapsdf(
        user_src_build,
        contour_name="vasya_test_contour",
        expected_code=http.client.CREATED)
