from maps.pylibs.utils.lib import common

import common as cm
import pytest


@pytest.mark.use_local_mongo
def _scan_resources(garden_client_helper, contour_name):
    def _get_ids(builds):
        return set(map(lambda build: build["id"], builds))

    existing_builds = _get_ids(garden_client_helper.get_builds(
        module_name=cm.TEST_SRC_MODULE,
        contour_name=contour_name,
    ))

    garden_client_helper.scan_resources(module_name=cm.TEST_SRC_MODULE, contour_name=contour_name)

    def check_builds():
        builds = garden_client_helper.get_builds(
            module_name=cm.TEST_SRC_MODULE,
            contour_name=contour_name,
        )
        return len(builds) and all(map(lambda build: build["progress"]["status"] == "completed", builds))

    assert common.wait_until(check_builds, timeout=120, check_interval=1)

    builds_after_scan = _get_ids(garden_client_helper.get_builds(
        module_name=cm.TEST_SRC_MODULE,
        contour_name=contour_name,
    ))

    created_builds = builds_after_scan - existing_builds
    assert len(created_builds) == 1, created_builds  # at least one build must be created

    for build_id in created_builds:
        garden_client_helper.wait_build(cm.TEST_SRC_MODULE, build_id)
        garden_client_helper.wait_build(cm.TEST_SRC_MODULE, build_id)

    return created_builds


@pytest.mark.use_local_mongo
def test_scan_resources(garden_client_helper):
    garden_client_helper.init_module_version(cm.CURRENT_BINARY_MODULE_VERSION)
    assert len(_scan_resources(garden_client_helper, cm.USER_CONTOUR_NAME)) > 0
    assert len(_scan_resources(garden_client_helper, cm.SYSTEM_CONTOUR_NAME)) > 0
