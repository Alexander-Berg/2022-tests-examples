from yandex.maps.test_utils.common import wait_until
from maps.garden.common_tests.test_utils.tester import (
    RESOURCES, update_shipping_date)


def test_map_autostart(builds_helper):
    """
    Test autostarter for map modules
    """
    assert len(builds_helper.module_builds("ymapsdf")) == 0

    ymapsdf_src = builds_helper.build_ymapsdf_src(RESOURCES[0])
    ymapsdf = builds_helper.build_ymapsdf(ymapsdf_src)
    assert len(builds_helper.module_builds("ymapsdf")) == 1

    builds_helper.build_module("denormalization", sources=[ymapsdf])
    assert len(builds_helper.module_builds("denormalization")) == 1

    builds_helper.enable_module_autostart("denormalization")

    ymapsdf_src = builds_helper.build_ymapsdf_src(update_shipping_date(RESOURCES[1]))
    ymapsdf = builds_helper.build_ymapsdf(ymapsdf_src)
    assert wait_until(lambda: len(builds_helper.module_builds("denormalization")) == 2)


def test_deployment_autostart(builds_helper):
    """
    Test autostarter for deployment modules
    """
    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in RESOURCES
    ]
    ymapsdf_builds = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds
    ]

    builds_helper.enable_module_autostart('world_creator_deployment')

    assert len(builds_helper.module_builds('world_creator_deployment')) == 0

    builds_helper.build_module("world_creator", ymapsdf_builds)

    assert wait_until(lambda: len(builds_helper.module_builds("world_creator_deployment")) == 1)
