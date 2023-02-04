import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, hosts_api_url, monkeypatch_hw_client_for_host


@pytest.fixture
def test(monkeypatch_timestamp, request, monkeypatch_audit_log):
    return TestCase.create(request)


@pytest.mark.parametrize("power_on", [True, False])
@pytest.mark.parametrize("host_id_field", ["inv", "name"])
def test_power_status(test, mp, host_id_field, power_on):
    host = test.mock_host()
    hw_client = monkeypatch_hw_client_for_host(mp, host, power_on=power_on)
    hw_client.power_status.return_value = power_on

    response = test.api_client.get(hosts_api_url(host, host_id_field, "/power-status"))
    assert response.status_code == http.client.OK
    assert response.json["is_powered_on"] == power_on
