"""Tests HostNetwork API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import mock_host_health_status
from walle.hosts import HostStatus


class TestGetNetworkInfo:
    @pytest.fixture
    def sample_hosts_net(self, walle_test):
        hosts = []
        networks = []
        for inv in range(3):

            host = walle_test.mock_host(
                {
                    "inv": inv,
                    "name": "host-{}".format(inv),
                    "config": "base",
                    "status": HostStatus.READY,
                    "status_reason": "reason-mock",
                    "health": mock_host_health_status(),
                }
            )
            network = walle_test.mock_host_network(
                dict(active_mac="MOCKMAC", active_mac_time=1, network_switch="MOCKSWITCH", network_port="MOCKPORT"),
                host=host,
            )
            hosts.append(host)
            networks.append(network)

        return hosts, networks

    @pytest.mark.parametrize("field_list", ["active_mac", ["active_mac"]])
    def test_get_network_info(self, walle_test, sample_hosts_net, field_list):
        hosts, networks = sample_hosts_net
        result = walle_test.api_client.get(
            "/v1/hosts/{}/network".format(hosts[0].uuid), query_string={"fields": field_list}
        )
        assert http.client.OK == result.status_code
        assert result.content_type == "application/json"
        assert {"active_mac": networks[0].active_mac} == result.json

    @pytest.mark.parametrize("field_list", ["active_mac,uuid", ["active_mac", "uuid"]])
    def test_query_network_single(self, walle_test, sample_hosts_net, field_list):
        hosts, networks = sample_hosts_net
        result = walle_test.api_client.post(
            "/v1/get-hosts/network", data={"uuids": [hosts[0].uuid]}, query_string={"fields": field_list}
        )
        assert result.status_code == http.client.OK
        assert result.content_type == "application/json"
        assert {"result": [{"uuid": hosts[0].uuid, "active_mac": networks[0].active_mac}], "total": 1} == result.json

    @pytest.mark.parametrize("field_list", ["active_mac,uuid", ["active_mac", "uuid"]])
    def test_query_network_all(self, walle_test, sample_hosts_net, field_list):
        hosts, networks = sample_hosts_net
        result = walle_test.api_client.post(
            "/v1/get-hosts/network",
            data={"uuids": [hosts[0].uuid, hosts[1].uuid, hosts[2].uuid]},
            query_string={"fields": field_list},
        )
        assert result.status_code == http.client.OK
        assert result.content_type == "application/json"
        assert {
            "result": [
                {"uuid": hosts[0].uuid, "active_mac": networks[0].active_mac},
                {"uuid": hosts[1].uuid, "active_mac": networks[1].active_mac},
                {"uuid": hosts[2].uuid, "active_mac": networks[2].active_mac},
            ],
            "total": 3,
        } == result.json

    def test_query_network_with_limit(self, walle_test, sample_hosts_net):
        hosts, networks = sample_hosts_net
        result = walle_test.api_client.post(
            "/v1/get-hosts/network",
            data={"uuids": [hosts[0].uuid, hosts[1].uuid, hosts[2].uuid]},
            query_string={"fields": "uuid,active_mac", "limit": 1},
        )

        assert result.status_code == http.client.OK
        assert result.content_type == "application/json"
        assert {
            "result": [{"uuid": hosts[0].uuid, "active_mac": networks[0].active_mac}],
            "next_cursor": hosts[1].uuid,
            "total": 3,
        } == result.json

    def test_query_network_with_cursor(self, walle_test, sample_hosts_net):
        hosts, networks = sample_hosts_net
        result = walle_test.api_client.post(
            "/v1/get-hosts/network",
            data={"uuids": [hosts[0].uuid, hosts[1].uuid, hosts[2].uuid]},
            query_string={"fields": "uuid,active_mac", "limit": 1, "cursor": hosts[1].uuid},
        )

        assert result.status_code == http.client.OK
        assert result.content_type == "application/json"
        assert {
            "next_cursor": hosts[2].uuid,
            "result": [{"uuid": hosts[1].uuid, "active_mac": networks[1].active_mac}],
        } == result.json
