from yandex.maps.test_utils.common import wait_until
from maps.garden.common_tests.test_utils.tester import RESOURCES, update_shipping_date


def test_reduce_autostart(builds_helper):
    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in RESOURCES
    ]
    ymapsdf_builds = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds
    ]
    assert len(builds_helper.module_builds("ymapsdf")) == 2

    name = "world_with_params_creator"

    build = builds_helper.build_module(name, ymapsdf_builds, release_name="0.0.0")

    assert len(builds_helper.module_builds(name)) == 1

    builds_helper.enable_module_autostart(name)

    ymapsdf_src = builds_helper.build_ymapsdf_src(update_shipping_date(RESOURCES[0]))
    builds_helper.build_ymapsdf(ymapsdf_src)
    assert len(builds_helper.module_builds("ymapsdf")) == 3

    assert wait_until(lambda: len(builds_helper.module_builds(name)) == 2)

    builds = builds_helper.module_builds(name)

    if builds[0]["version"] == build["version"]:
        old_build, new_build = builds[0], builds[1]
    else:
        old_build, new_build = builds[1], builds[0]

    assert old_build["properties"]["release_name"] == "0.0.0"
    assert new_build["properties"]["release_name"]
    assert old_build["properties"]["release_name"] != new_build["properties"]["release_name"]

    assert not old_build["properties"]["autostarted"]
    assert new_build["properties"]["autostarted"]

    _, build_id = new_build["version"].split(":")
    builds_helper.wait_for_build({"module_name": name, "build_id": build_id})

    # unique_world_creator has build limit 2, new build should not autostart.
    ymapsdf_src = builds_helper.build_ymapsdf_src(update_shipping_date(RESOURCES[1]))
    builds_helper.build_ymapsdf(ymapsdf_src)
    assert len(builds_helper.module_builds("ymapsdf")) == 4
    assert len(builds_helper.module_builds(name)) == 2
