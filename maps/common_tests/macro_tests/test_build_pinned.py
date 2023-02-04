import http.client

from maps.garden.common_tests.test_utils.tester import RESOURCES


def test_pin_unpin(builds_helper):
    ymapsdf_src = builds_helper.build_ymapsdf_src(RESOURCES[0])
    ymapsdf = builds_helper.build_ymapsdf(ymapsdf_src)

    # pin build
    response = builds_helper.garden_client.put(
        ymapsdf["url"],
        json={"pin": True}
    )
    assert response.status_code == http.client.OK

    response = builds_helper.garden_client.get(ymapsdf["url"])
    assert response.status_code == http.client.OK
    assert response.get_json()["pinned"]

    # unpin build
    response = builds_helper.garden_client.put(
        ymapsdf["url"],
        json={"pin": False}
    )
    assert response.status_code == http.client.OK

    response = builds_helper.garden_client.get(ymapsdf["url"])
    assert response.status_code == http.client.OK
    assert not response.get_json()["pinned"]


def test_remove_pinned(builds_helper):
    ymapsdf_src = builds_helper.build_ymapsdf_src(RESOURCES[0])
    ymapsdf = builds_helper.build_ymapsdf(ymapsdf_src)

    response = builds_helper.garden_client.put(
        ymapsdf["url"],
        json={"pin": True}
    )
    assert response.status_code == http.client.OK

    builds_helper.delete_build(ymapsdf, expected_code=http.client.FORBIDDEN)
