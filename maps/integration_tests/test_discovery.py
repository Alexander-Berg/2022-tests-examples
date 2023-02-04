from maps.infra.ratelimiter2.tools.pyhelpers.config import timings, wait_for
from maps.infra.ratelimiter2.tools.pyhelpers.mock_utils import MockDiscoveryServer


REQUESTS = 100


def test_discovery(run_servers, run_proxy, block_peer_sync, eliminate_sync_gap) -> None:
    """
        Proxy is initialized with one of the servers and should get
        the rest from discovery. Thus counters should be synced on all
        servers.
    """
    with run_servers(n=4) as servers:
        servers[0].set_limits(rps=0, burst=0)
        wait_for(timings.resource_update_interval)
        # We want counters sync only from proxy, so we disable peer sync.
        block_peer_sync(servers)

        with run_proxy(servers[0]) as proxy:
            eliminate_sync_gap(proxy)
            proxy.sync_counters(n=REQUESTS)
            wait_for(timings.counter_synchronization_interval)

            for server in servers:
                assert server.current_counters() == REQUESTS


def test_topology_change(run_servers, run_proxy, block_peer_sync, eliminate_sync_gap) -> None:
    """
        Checking discovery on dynamic topology. Only servers from
        current topology should get updates by proxy.
    """
    def add_requests(proxy):
        proxy_total = proxy.current_counters()
        new_total = proxy_total + REQUESTS
        proxy.sync_counters(n=new_total)
        wait_for(timings.counter_synchronization_interval)
        return new_total

    with run_servers(n=4) as servers:
        servers[0].set_limits(rps=0, burst=0)
        wait_for(timings.resource_update_interval)
        # We want counters sync only from proxy, so we disable peer sync.
        block_peer_sync(servers)

        with MockDiscoveryServer(servers) as discovery_server:
            with run_proxy(discovery_server=discovery_server) as proxy:
                eliminate_sync_gap(proxy)

                # Initial topology has all servers.
                add_requests(proxy)
                for server in servers:
                    assert server.current_counters() == REQUESTS

                # Let's change topology and 'remove' half servers from it.
                current_topology = servers[:len(servers) // 2]
                discovery_server.change_topology(current_topology)
                wait_for(timings.discovery_update_interval)

                # Check that only servers from current topology received new requests.
                current_total = add_requests(proxy)
                for server in servers:
                    if server in current_topology:
                        assert server.current_counters() == current_total
                    else:
                        assert server.current_counters() < current_total

                # Remove all servers from topology and expect less than total.
                discovery_server.change_topology([])
                wait_for(timings.discovery_update_interval)

                current_total = add_requests(proxy)
                for server in servers:
                    assert server.current_counters() < current_total

                # Restore initial topology.
                discovery_server.change_topology(servers)
                wait_for(timings.discovery_update_interval)

                current_total = add_requests(proxy)
                for server in servers:
                    assert server.current_counters() == current_total
