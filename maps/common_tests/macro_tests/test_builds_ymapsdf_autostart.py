from yandex.maps.test_utils.common import wait_until

from maps.garden.common_tests.test_utils.tester import RASP_RESOURCES, SRC_RESOURCES


def test_followers(builds_helper):
    # autostart first ymapsdf build
    builds_helper.enable_module_autostart("ymapsdf")
    builds_helper.build_src("rasp_export_src", RASP_RESOURCES[0])
    builds_helper.build_src("ymapsdf_src", SRC_RESOURCES[0])

    assert wait_until(lambda: len(builds_helper.module_builds("ymapsdf")) == 1)

    ymapsdf_builds = builds_helper.module_builds("ymapsdf")
    ymapsdf_build1 = ymapsdf_builds[0]
    assert len(ymapsdf_build1["sources"]) == 2

    # autostart second ymapsdf build
    rasp2_build = builds_helper.build_src("rasp_export_src", RASP_RESOURCES[1])
    src2_build = builds_helper.build_src("ymapsdf_src", SRC_RESOURCES[1])

    assert wait_until(lambda: len(builds_helper.module_builds("ymapsdf")) == 2)

    ymapsdf_builds = builds_helper.module_builds("ymapsdf")

    # check ymapsdf sources
    ymapsdf2_build = next(
        x for x in ymapsdf_builds
        if x["version"] != ymapsdf_build1["version"])
    ymapsdf2_sources = ymapsdf2_build["sources"]
    assert len(ymapsdf2_sources) == 2

    rasp_source = next(x for x in ymapsdf2_sources if x["name"] != "ymapsdf_src")
    assert rasp_source['version'] == rasp2_build["version"]
    src_source = next(
        x for x in ymapsdf2_sources if x["name"] != "rasp_export_src")
    assert src_source['version'] == src2_build["version"]


def test_src_sort_order(builds_helper):
    builds_helper.enable_module_autostart("ymapsdf")

    # First, create a build with a later shipping_date: 2017
    builds_helper.build_src("rasp_export_src", RASP_RESOURCES[1])

    # Second, create a build with an earlier shipping_date: 2016
    rasp2_build = builds_helper.build_src("rasp_export_src", RASP_RESOURCES[0])

    builds_helper.build_src("ymapsdf_src", SRC_RESOURCES[0])

    assert wait_until(lambda: len(builds_helper.module_builds("ymapsdf")) == 1)
    ymapsdf_builds = builds_helper.module_builds("ymapsdf")
    ymapsdf_build1 = ymapsdf_builds[0]

    ymapsdf_sources = ymapsdf_build1["sources"]
    assert len(ymapsdf_sources) == 2

    # Take the source with the max id
    rasp_source = next(x for x in ymapsdf_sources if x["name"] != "ymapsdf_src")
    assert rasp_source['version'] == rasp2_build["version"]
