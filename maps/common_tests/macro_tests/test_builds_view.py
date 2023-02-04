import http.client

from yandex.maps.test_utils.common import wait_until

from maps.garden.common_tests.test_utils import tester


def test_get_all_builds(builds_helper):
    response = builds_helper.garden_client.get("builds/")
    assert response.status_code == http.client.OK
    assert response.get_json()["total"] == 0
    assert len(response.get_json()["builds"]) == 0

    # 2 build for each resource: ymapsdf_src & ymapsdf
    input_counts = len(tester.RESOURCES) * 2

    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in tester.RESOURCES
    ]
    ymapsdf_builds = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds
    ]

    response = builds_helper.garden_client.get("builds/")
    assert response.status_code == http.client.OK
    assert response.get_json()["total"] == input_counts
    assert len(response.get_json()["builds"]) == input_counts

    builds_helper.build_module("failing_module", ymapsdf_builds, expected_status="failed")

    response = builds_helper.garden_client.get("builds/")
    assert response.status_code == http.client.OK
    assert response.get_json()["total"] == input_counts + 1
    assert len(response.get_json()["builds"]) == input_counts + 1


def test_get_filtered_builds(builds_helper):
    input_counts = len(tester.RESOURCES)

    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in tester.RESOURCES
    ]
    ymapsdf_builds = [
        builds_helper.build_ymapsdf(source) for source in ymapsdf_src_builds
    ]
    builds_helper.build_module("failing_module", ymapsdf_builds, expected_status="failed")

    response = builds_helper.garden_client.get("builds/?status=failed")
    assert response.status_code == http.client.OK
    assert response.get_json()["total"] == 1
    assert len(response.get_json()["builds"]) == 1

    response = builds_helper.garden_client.get("builds/?status=completed&module=ymapsdf")
    assert response.status_code == http.client.OK
    assert response.get_json()["total"] == input_counts
    assert len(response.get_json()["builds"]) == input_counts


def test_get_abandoned_build(builds_helper, db):
    """
    Abandoned module was removed from Garden but there are builds left.
    """
    ymapsdf_src = builds_helper.build_ymapsdf_src(tester.RESOURCES[0])
    builds_helper.build_module("failing_module", [ymapsdf_src], expected_status="failed")

    db.builds.update_many({"name": "failing_module"}, {"$set": {"name": "abandoned"}})

    response = builds_helper.garden_client.get("builds/")
    assert response.status_code == http.client.OK
    assert response.get_json()["total"] == 2
    assert len(response.get_json()["builds"]) == 2
    assert response.get_json()["builds"][1].get("name") == "abandoned"
    url = response.get_json()['builds'][1].get("url")

    response = builds_helper.garden_client.get(url)
    assert response.status_code == http.client.OK
    assert response.get_json()["name"] == "abandoned"


def test_delete_abandoned_build(builds_helper, db):
    ymapsdf_src = builds_helper.build_ymapsdf_src(tester.RESOURCES[0])
    builds_helper.build_module("failing_module", [ymapsdf_src], expected_status="failed")

    db.builds.update_many({"name": "failing_module"}, {"$set": {"name": "abandoned"}})

    response = builds_helper.garden_client.get("builds/")
    assert response.status_code == http.client.OK
    assert response.get_json()["total"] == 2  # ymapsdf_src and abandoned
    assert len(response.get_json()["builds"]) == 2

    for build in response.get_json()['builds']:
        if build["name"] == "abandoned":
            url = build["url"]

    response = builds_helper.garden_client.delete(url, json={})
    assert response.status_code == http.client.OK

    def wait_cond():
        response = builds_helper.garden_client.get("builds/")
        assert response.status_code == http.client.OK
        return len(response.get_json()['builds']) == 1

    assert wait_until(wait_cond)


def test_resource_url(builds_helper):
    ymapsdf_src = builds_helper.build_ymapsdf_src(tester.RESOURCES[0])
    builds_helper.build_ymapsdf(ymapsdf_src)

    response = builds_helper.garden_client.get("builds/")
    assert response.status_code == http.client.OK
    assert response.get_json()["total"] == 2
    assert len(response.get_json()["builds"]) == 2

    build = response.get_json()["builds"][0]
    assert "resources_url" in build

    response = builds_helper.garden_client.get(build["resources_url"])
    assert response.status_code == http.client.OK
    assert len(response.get_json()) == 1
