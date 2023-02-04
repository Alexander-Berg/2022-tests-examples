import http.client
import pytest


def _prepare_ymapsdf(builds_helper, **properties):
    resource_info = {
        "name": "{region}_src".format(**properties),
        "version": {
            "properties": properties,
        },
    }
    ymapsdf_src = builds_helper.build_ymapsdf_src(resource_info)
    return builds_helper.build_ymapsdf(ymapsdf_src)


def _check_build(builds_helper, build, expected_properties):
    response = builds_helper.garden_client.get(build["url"])
    assert response.status_code == http.client.OK
    properties = response.get_json()["properties"]
    print(properties)
    for property_name, expected_value in expected_properties.items():
        assert properties.get(property_name) == expected_value


@pytest.mark.parametrize("is_reverse_src_order", [True, False])
def test_build_properties(builds_helper, is_reverse_src_order):
    """
    Test how source build properties propagate to target build properties.
    """
    europe = _prepare_ymapsdf(
        builds_helper,
        region="europe",
        shipping_date="20201011",
        vendor="navteq",
        europe_property="unique_value",
        same_common_property="same_value",
        different_common_property="unique_value")

    australia = _prepare_ymapsdf(
        builds_helper,
        region="australia",
        shipping_date="20201011",
        vendor="navteq",
        australia_property="unique_value",
        same_common_property="same_value",
        different_common_property="other_unique_value")

    expected_properties = dict(
        shipping_date="20201011",
        region=None,
        europe_property="unique_value",
        australia_property="unique_value",
        same_common_property="same_value",
        different_common_property=None)

    src_list = [europe, australia]
    if is_reverse_src_order:
        src_list.reverse()

    build = builds_helper.build_module("world_creator", src_list)

    _check_build(builds_helper, build, expected_properties)

    expected_properties = dict(
        shipping_date="20201011",
        region=None,
        europe_property=None,
        australia_property="unique_value",
        same_common_property=None,
        different_common_property=None)

    build = builds_helper.build_module("build_with_propagated_properties", src_list)

    _check_build(builds_helper, build, expected_properties)

    builds_helper.start_module(
        "build_with_different_common_property",
        src_list,
        expected_code=http.client.BAD_REQUEST)
