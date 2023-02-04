import http.client
import json

from maps.garden.common_tests.test_utils.tester import RESOURCES, wait_until
from maps.garden.common_tests.test_utils.constants import RESOURCE_TEMPLATE, INVALID_SHIPPING_DATE


def test_builds_list(builds_helper, smtp_server):
    emails_count = len(smtp_server["emails"])

    ymapsdf_src_builds = [
        builds_helper.build_ymapsdf_src(resource) for resource in RESOURCES
    ]

    invalid_resource = json.loads(
        RESOURCE_TEMPLATE % ("australia_src", "australia", INVALID_SHIPPING_DATE, "navteq")
    )
    ymapsdf_src_builds.append(
        builds_helper.build_ymapsdf_src(invalid_resource)
    )

    num_iterations = len(RESOURCES) + 1
    for i in range(num_iterations):
        status = "completed" if i != num_iterations - 1 else "failed"
        builds_helper.build_ymapsdf(ymapsdf_src_builds[i], expected_status=status)

    response = builds_helper.garden_client.get("modules/ymapsdf/builds/")
    assert response.status_code == http.client.OK

    response = builds_helper.garden_client.get("modules/ymapsdf/builds/?status=failed")
    assert response.status_code == http.client.OK
    assert len(response.get_json()) == 1

    response = builds_helper.garden_client.get("modules/ymapsdf/builds/?limit=2")
    assert response.status_code == http.client.OK
    assert len(response.get_json()) == 2

    response = builds_helper.garden_client.get("modules/ymapsdf/builds/?limit=1&status=completed")
    assert response.status_code == http.client.OK
    assert len(response.get_json()) == 1

    # Check if we've got email notifications
    # Note, that we can have more notification, because of builds autostart
    assert wait_until(lambda: len(smtp_server["emails"]) >= num_iterations + emails_count)
    sender, receivers, data = smtp_server["emails"][0]
    assert sender == "noreply@yandex-team.ru"
