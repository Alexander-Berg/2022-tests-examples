import http.client

from tests.api.iam_permissions import mocks  # noqa NOTE(rocco66): fixture


def test_agent_report(iam, walle_test):
    host_name = "does-not-matter"
    request_data = {"version": "0.0.0"}
    response = walle_test.api_client.put(f"/v1/hosts/{host_name}/agent-report", data=request_data)
    assert response.status_code == http.client.OK
