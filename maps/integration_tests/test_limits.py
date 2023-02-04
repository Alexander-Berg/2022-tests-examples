import requests
from unittest import mock

from maps.infra.ratelimiter2.tools.pyhelpers.config import Timings, timings, wait_for, Time
from maps.infra.ratelimiter2.proto import counters_pb2
from maps.infra.ratelimiter2.tools.pyhelpers.make_counters import make_limits_proto, make_counters_proto
from maps.infra.ratelimiter2.tools.pyhelpers.mock_utils import MockServerResponse, MockDiscoveryServer


def test_limits_set(run_server, resources) -> None:
    """Makes post request to set limits on server that uses mongodb."""
    with run_server() as server:
        initial_limits = server.query_limits()
        expected_limits = make_limits_proto(resources, rps=100, burst=0)
        server.set_limits_proto(expected_limits)
        wait_for(timings.resource_update_interval)

        server_limits = server.query_limits()
        # server increments last limits version from mongodb
        expected_limits.version = server_limits.version
        assert server_limits == expected_limits
        assert initial_limits.version < server_limits.version


def test_limits_sync(run_servers, resources) -> None:
    """
        Set limits to server1
        Expect server2 to sync new limits from mongodb
    """
    with run_servers(n=2) as servers:
        server1, server2 = servers
        expected_limits = make_limits_proto(resources, rps=200, burst=10)
        server1.set_limits_proto(expected_limits)
        wait_for(timings.resource_update_interval)

        server_limits = server2.query_limits()
        # server increments last limits version from mongodb
        expected_limits.version = server_limits.version

        assert server_limits == expected_limits


def test_limits_update(run_servers) -> None:
    """Setting limits for different projects should not interfere."""
    def check_limits(server, expected_limits, *, project: str) -> None:
        server_limits = server.query_limits(project=project)
        # Server increments last limits version from mongodb.
        expected_limits.version = server_limits.version
        assert server_limits == expected_limits

    n = 5
    project_a = "maps_core_project_a"
    project_b = "maps_core_project_b"
    resources_a = [f"resource_a_{i}" for i in range(20)]
    resources_b = [f"resource_b_{i}" for i in range(20)]
    with run_servers(n=2) as servers:
        server_a, server_b = servers
        for i in range(n):
            expected_limits_a = make_limits_proto(resources_a, rps=100*(i+1), burst=10*(i+1))
            expected_limits_b = make_limits_proto(resources_b, rps=200*(i+1), burst=20*(i+1))
            server_a.set_limits_proto(expected_limits_a, project=project_a)
            server_b.set_limits_proto(expected_limits_b, project=project_b)
            wait_for(timings.resource_update_interval)

            check_limits(server_a, expected_limits_a, project=project_a)
            check_limits(server_b, expected_limits_b, project=project_b)


def test_limits_conflict_proxy(run_proxy, resources) -> None:
    """Proxy should not request new limits during conflict having newer version."""
    no_background_limits_update = Timings()
    no_background_limits_update.resource_update_interval = Time(seconds=60*60)

    with mock.patch("maps.infra.ratelimiter2.tools.pyhelpers.config.timings", no_background_limits_update):
        with MockServerResponse() as server, MockDiscoveryServer([server]) as discovery:
            # Expect first request from ratelimiter proxy for getting initial limits.
            initial_limits = make_limits_proto(resources, rps=100, burst=0, version=42)
            server.request_on(url="/resources/limits/get").will_respond(body=initial_limits.SerializeToString())

            with run_proxy(discovery_server=discovery):
                # Emulate server limits update. Agent should request new ones.
                newer_limits = make_limits_proto(resources, rps=100, burst=0, version=120)
                counters = make_counters_proto([], [], [], limits_version=newer_limits.version)
                server.request_on(url="/counters/sync").will_respond(status=409, body=counters.SerializeToString())
                server.request_on(url="/resources/limits/get").will_respond(body=newer_limits.SerializeToString())
                # Wait for client to make a request.
                wait_for(timings.counter_synchronization_interval)

                # Emulate server with older limits. Agent should NOT request new ones.
                older_limits = make_limits_proto(resources, rps=100, burst=0, version=12)
                counters = make_counters_proto([], [], [], limits_version=older_limits.version)
                server.request_on(url="/counters/sync").will_respond(status=409, body=counters.SerializeToString())
                # Make sure client won't request new limits.
                # Agent will get 409 with lower version and should not go for new limits.
                wait_for(timings.counter_synchronization_interval*2)

                # Assert that client requested new limits only on start and with older limits.
                assert server.actual_requests["/counters/sync"] >= 2
                assert server.actual_requests["/resources/limits/get"] == 2


def test_limits_conflict_server(run_server, resources) -> None:
    """Checks server response on limits conflict"""
    with run_server() as server:
        server.set_limits(rps=100, burst=0)
        wait_for(timings.resource_update_interval)

        server_limits_version = server.query_limits().version

        counters = make_counters_proto([], [], [], limits_version=server_limits_version - 1)
        response = requests.post(f'{server.url}/counters/sync?shard=shard0',
                                 data=counters.SerializeToString())
        response_proto = counters_pb2.Counters()
        response_proto.ParseFromString(response.content)

        assert response.status_code == 409
        assert response_proto.limits_version == server_limits_version
