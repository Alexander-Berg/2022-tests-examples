from copy import deepcopy

from maps.garden.common_tests.test_utils import tester


def test_dependent_resources_removal(builds_helper):
    """
    Check resources removal upon all parent builds removal
    """

    traits = builds_helper.garden_client.get("modules/ymapsdf/traits/").json
    build_limit = traits["build_limits"][0]["max"]

    # The following dict matches input resource names from ymapsdf_src
    # to the ymapsdf output ones (see `test_modules/ymapsdf.py`).
    # TODO: move it to a common place
    resource_names = {
        "europe_src": "europe_expanded",
        "australia_src": "australia_shrinked",
    }
    additional_builds_per_source_count = 5
    completed_builds = []
    for i in range(additional_builds_per_source_count + build_limit):
        for resource in tester.RESOURCES:
            resource = deepcopy(resource)
            properties = resource["version"]["properties"]
            # Ensure unique sources for each ymapsdf build
            properties["shipping_date"] = str(int(properties["shipping_date"]) + i)
            source = builds_helper.build_ymapsdf_src(resource)
            build = builds_helper.build_ymapsdf(source)
            completed_builds.append((build, resource_names[resource["name"]]))

    additional_builds_count = additional_builds_per_source_count * len(resource_names)
    autoremoved_builds = completed_builds[:additional_builds_count]
    builds_to_remove_manually = completed_builds[additional_builds_count:]

    assert tester.wait_until(
        lambda: not any(
            builds_helper.build_exists(build)
            for build, _ in autoremoved_builds
        ),
        timeout=10,  # seconds
    )

    shallow_deleted_builds = builds_to_remove_manually[:-len(resource_names)]
    deep_deleted_builds = builds_to_remove_manually[-len(resource_names):]
    for builds, resource_count in [
        (shallow_deleted_builds, 1),
        (deep_deleted_builds, 0),
    ]:
        for build, resource_name in builds:
            builds_helper.delete_build(build)
            builds_helper.check_resources_count(resource_name, resource_count)
