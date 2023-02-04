from maps.infra.ratelimiter2.tools.pyhelpers.config import timings, wait_for
from maps.infra.ratelimiter2.tools.pyhelpers.mock_utils import MockDiscoveryServer
from maps.infra.ratelimiter2.tools.pyhelpers.mocks import mock_backend, CallableHandler


RPS = 100


def check_counters(instance, timer, initial_counters: int, rps: int, extra: int = 0) -> None:
    seconds_passed = timer.seconds
    final_counters = instance.current_counters()
    assert final_counters >= initial_counters
    assert final_counters - initial_counters >= seconds_passed * rps - rps
    # 5xRPS to account for slow execution
    assert final_counters - initial_counters <= seconds_passed * rps + 5 * rps + extra


def check_counters_roughly_equal(instance, initial_counters: int, value: int) -> None:
    final_counters = instance.current_counters()
    assert final_counters >= initial_counters
    # -RPS margin for lower bound if server had a counters tick on request.
    assert final_counters - initial_counters >= value - RPS
    # 5xRPS margin to account for slow execution.
    assert final_counters - initial_counters <= value + 5 * RPS


def test_peer_sync(run_servers, eliminate_sync_gap) -> None:
    """
    Both server replicas are equal,
    even when proxy sends syncs just to one of them.
    """
    with run_servers(n=2) as servers:
        server1, server2 = servers
        server1.set_limits(rps=0, burst=0)  # 0 RPS, no leaky bucket
        wait_for(timings.resource_update_interval)

        eliminate_sync_gap(server1)
        server1.sync_counters(n=100)
        assert server1.current_counters() == 100

        wait_for(timings.peer_synchronization_interval)

        assert server1.current_counters() == 100
        assert server2.current_counters() == 100

        # Now restart servers one by one
        server1.restart_backend()
        wait_for(timings.peer_synchronization_interval)
        server2.restart_backend()
        wait_for(timings.peer_synchronization_interval)

        # Expect counters state intact
        assert server1.current_counters() == 100
        assert server2.current_counters() == 100


def test_server_restart(run_servers, run_proxy, eliminate_sync_gap) -> None:
    """Restart all servers one by one - counters must stay intact."""
    with run_servers(n=2) as servers:
        server1, server2 = servers
        server1.set_limits(rps=RPS, burst=0)
        wait_for(timings.resource_update_interval)

        with run_proxy(servers) as proxy:
            eliminate_sync_gap(proxy)
            proxy.sync_counters(n=0)  # Kick leaky bucket.
            wait_for(timings.counter_synchronization_interval)

            # Wait to tick start on both servers.
            wait_for(timings.counter_tick_interval)

            counters = []
            for instance in (server1, server2):
                counters.append(instance.current_counters())

            # Big increment, so it not eaten by timed lower bound
            # NB: replicated state is above lower bound
            proxy.sync_counters(100 * RPS)
            wait_for(timings.counter_synchronization_interval)

            for server, server_counters in zip((server1, server2), counters):
                check_counters_roughly_equal(server, server_counters, 100 * RPS)

            server1.restart_backend()
            wait_for(timings.peer_synchronization_interval)

            for server, server_counters in zip((server1, server2), counters):
                check_counters_roughly_equal(server, server_counters, 100 * RPS)

            server2.restart_backend()
            wait_for(timings.peer_synchronization_interval)

            for server, server_counters in zip((server1, server2), counters):
                check_counters_roughly_equal(server, server_counters, 100 * RPS)


def test_adjustment(run_server, timer) -> None:
    """Leaky bucket must leak with specified rate."""
    with run_server() as server:
        server.set_limits(rps=RPS, burst=0)
        wait_for(timings.resource_update_interval)

        server.sync_counters(n=0)
        # Make sure bucket leaked.
        wait_for(timings.counter_tick_interval)

        timer = timer()
        initial_counters = server.current_counters()
        wait_for(timings.second)

        check_counters(server, timer, initial_counters, RPS)


def test_new_server(run_servers, run_proxy, eliminate_sync_gap, block_peer_sync) -> None:
    """
        Proxy starts with one server in its topology. After some
        big counters increment, another server is being added into
        topology. Counters should be synced.
    """
    with run_servers(n=2) as servers:
        server1, server2 = servers
        server1.set_limits(rps=RPS, burst=0)
        wait_for(timings.resource_update_interval)

        block_peer_sync(servers)  # Disable peer sync.
        with MockDiscoveryServer([server1]) as discovery_server:
            with run_proxy(discovery_server=discovery_server) as proxy:
                eliminate_sync_gap(proxy)
                proxy.sync_counters(n=0)  # Kick leaky bucket
                wait_for(timings.counter_synchronization_interval)
                wait_for(timings.counter_tick_interval)

                initial_counters = proxy.current_counters()
                REQUESTS = 100 * RPS
                proxy.sync_counters(REQUESTS)
                wait_for(timings.counter_synchronization_interval)

                # First server should has counters update.
                check_counters_roughly_equal(server1, initial_counters, REQUESTS)
                # Second one should have uninitialized counters.
                assert server2.current_counters() == 0

                # Add second server into topology.
                discovery_server.change_topology(servers)
                # Enable peer sync.
                server1.unblock_all()
                server2.unblock_all()
                wait_for(timings.peer_synchronization_interval)

                # First server should still have roughly the same counters.
                check_counters_roughly_equal(server1, initial_counters, REQUESTS)
                # We expect new server to have a counters update.
                check_counters_roughly_equal(server2, initial_counters, REQUESTS)


def test_session_id(run_proxy) -> None:
    """
        Session id is passed in shard url parameter.
        It should be randomly generated at agent start.
    """
    shards = set()

    def save_shard(hnd) -> tuple[bytes, int]:
        shards.add(hnd.path.split('shard=')[-1])
        return b'', 200

    with mock_backend() as backend, MockDiscoveryServer([backend]) as discovery:
        backend.replace([('/counters/sync', CallableHandler(save_shard))])
        with run_proxy(discovery_server=discovery):
            wait_for(timings.counter_synchronization_interval)
        with run_proxy(discovery_server=discovery):
            wait_for(timings.counter_synchronization_interval)

    # Two agent starts should create two unique session ids.
    assert len(shards) == 2
