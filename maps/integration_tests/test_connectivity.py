from maps.infra.ratelimiter2.tools.pyhelpers.config import timings, wait_for


RPS = 100


def check_counters(instance, timer, initial_counters: int, rps: int, extra: int = 0) -> None:
    seconds_passed = timer.seconds
    final_counters = instance.current_counters()
    assert final_counters >= initial_counters
    assert final_counters - initial_counters >= seconds_passed * rps - rps
    # 5xRPS to account for slow execution
    assert final_counters - initial_counters <= seconds_passed * rps + 5 * rps + extra


def test_unavailability(run_servers, run_proxy, block_peer_sync) -> None:
    """
        Server is unreachable for proxy, counters must not be synced.
        (Test for correctness of proxy utilities).
    """
    with run_servers(n=2) as servers:
        server1, server2 = servers
        server1.set_limits(rps=0, burst=0)
        wait_for(timings.resource_update_interval)

        with run_proxy(servers) as proxy:
            proxy.sync_counters(n=0)  # Init counters.
            wait_for(timings.counter_synchronization_interval)

            server1.block_sync_for(shard_ids=[proxy.shard_id])
            block_peer_sync(servers)

            unavailable_server_initial_counters = server1.current_counters()
            available_server_initial_counters = server2.current_counters()
            proxy_initial_counters = proxy.current_counters()

            REQUESTS = 100  # Add requests on proxy.
            proxy.sync_counters(n=REQUESTS)
            wait_for(timings.counter_synchronization_interval)
            wait_for(timings.peer_synchronization_interval)

            unavailable_server_final_counters = server1.current_counters()
            available_server_final_counters = server2.current_counters()
            proxy_final_counters = proxy.current_counters()

            assert unavailable_server_initial_counters == unavailable_server_final_counters
            assert available_server_initial_counters + REQUESTS == available_server_final_counters
            assert proxy_initial_counters + REQUESTS == proxy_final_counters


def test_long_disconnect(run_servers, run_proxy, eliminate_sync_gap, block_peer_sync, timer) -> None:
    """One server can't sync counters for more than max sync gap."""
    with run_servers(n=2) as servers:
        server1, server2 = servers
        server1.set_limits(rps=RPS, burst=0)
        wait_for(timings.resource_update_interval)

        with run_proxy(servers) as proxy:
            eliminate_sync_gap(proxy)
            proxy.sync_counters(n=0)  # Kick leaky bucket
            # Wait for counters to synchronize on servers and
            # tick to increment counters.
            wait_for(timings.counter_synchronization_interval)
            wait_for(timings.counter_tick_interval)

            timers, initial_counters = [], []
            for instance in (server1, server2, proxy):
                timers.append(timer())
                initial_counters.append(instance.current_counters())

            # Block counter synchronization for server.
            server1.block_sync_for(shard_ids=[proxy.shard_id])
            block_peer_sync(servers)

            # Add plus 2 extra RPS every second.
            for i in range(1, 5):
                proxy.sync_counters(i * RPS + 2 * RPS)
                wait_for(timings.second)

            # Restore counters sync for servers.
            wait_for(timings.max_sync_gap)
            server1.unblock_all()
            server2.unblock_all()
            wait_for(timings.counter_synchronization_interval)

            for instance, timer, counters in zip((server1, server2, proxy), timers, initial_counters):
                check_counters(instance, timer, counters, RPS)


def test_split(run_servers, run_proxies, eliminate_sync_gap, timer, hosts) -> None:
    """We have two datacenters that function independently for some time."""
    with run_servers(n=2) as servers:
        server1, server2 = servers
        server1.set_limits(rps=RPS, burst=0)
        wait_for(timings.resource_update_interval)

        with run_proxies(servers, shard_ids=hosts[:2]) as proxies:
            proxy1, proxy2 = proxies
            eliminate_sync_gap(proxy1, proxy2)

            proxy1.sync_counters(n=0)  # kick leaky bucket
            proxy2.sync_counters(n=0)
            wait_for(timings.counter_synchronization_interval)
            wait_for(timings.counter_tick_interval)

            timers, counters = [], []
            for instance in (server1, server2):
                timers.append(timer())
                counters.append(instance.current_counters())

            # Split starts here.
            # Proxies will still be able to talk to one of their servers.
            shard1 = [proxy1.shard_id, server1.shard_id]
            shard2 = [proxy2.shard_id, server2.shard_id]
            server1.block_sync_for(shard_ids=shard2)
            server2.block_sync_for(shard_ids=shard1)

            # Add plus 2 extra RPS every second.
            for i in range(1, 5):
                proxy1.sync_counters(timers[0].seconds * RPS + 2 * RPS)
                proxy2.sync_counters(timers[1].seconds * RPS + 2 * RPS)
                wait_for(timings.second)

            # Every master knows only about own worker in its shard,
            # so we expect 1xRPS plus 1xRPS extra each.
            for server, timer, server_counters in zip((server1, server2), timers, counters):
                check_counters(server, timer, server_counters, RPS, extra=RPS)

            # Wait max sync gap to drop counters from different shard
            # during reconnect.
            wait_for(timings.max_sync_gap)
            server1.unblock_all()
            server2.unblock_all()

            # Now servers can sync with both proxies. But due to big lag in
            # communication with reconnected proxy, server in each shard
            # will ignore requests from other shard. Still 1xRPS.
            for server, timer, server_counters in zip((server1, server2), timers, counters):
                check_counters(server, timer, server_counters, RPS)

            # Let's check that servers will get counters from both proxies.
            proxy1.sync_counters(n=0)  # First sync after max_sync_gap
            proxy2.sync_counters(n=0)  # that will drop counters.
            wait_for(timings.counter_synchronization_interval)

            counters = [server.current_counters() for server in (server1, server2)]
            proxy1.sync_counters(n=100000)
            proxy2.sync_counters(n=100000)
            wait_for(timings.counter_synchronization_interval)

            # Expect proxy1 + proxy2 updates sum on both servers.
            for server, server_counters in zip(servers, counters):
                current = server.current_counters()
                # 1 RPS overlap due to async
                assert current - server_counters >= 200000 - RPS
                # 20sec margin for slow execution
                assert current - server_counters <= 200000 + RPS*20
