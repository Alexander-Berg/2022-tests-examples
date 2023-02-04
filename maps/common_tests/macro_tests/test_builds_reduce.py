import http.client

from maps.garden.common_tests.test_utils.tester import RESOURCES, update_shipping_date


def test_mutual_exclusion(builds_helper):
    ymapsdf_src_builds_v1 = [
        builds_helper.build_ymapsdf_src(resource) for resource in RESOURCES
    ]
    ymapsdf_builds_v1 = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds_v1
    ]

    ymapsdf_src_builds_v2 = [
        builds_helper.build_ymapsdf_src(update_shipping_date(resource)) for resource in RESOURCES
    ]
    ymapsdf_builds_v2 = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds_v2
    ]

    gi_v1 = builds_helper.build_module("geocoder_indexer", ymapsdf_builds_v1, ignore_warnings=True)
    wc = builds_helper.build_module("world_creator", ymapsdf_builds_v1, ignore_warnings=True)
    gi_v2 = builds_helper.build_module("geocoder_indexer", ymapsdf_builds_v2, ignore_warnings=True)

    assert len(builds_helper.module_builds("geocoder_indexer")) == 2

    builds_helper.start_module("offline_cache", [wc, gi_v1], ignore_warnings=True)

    builds_helper.start_module(
        "offline_cache",
        [wc, gi_v2],
        expected_code=http.client.CONFLICT,
        ignore_warnings=True,
    )

    assert len(builds_helper.module_builds("offline_cache")) == 1
