from maps.infra.ratelimiter2.tools.pyhelpers.config import timings, wait_for


def test_async(run_server, run_proxies, eliminate_sync_gap, hosts) -> None:
    """
        Multiple proxies receive increments and sync through single server
        Expect everyone to have sum of all increments in the end
    """
    with run_server() as server:
        server.set_limits(rps=0, burst=0)  # Disable leaky bucket
        wait_for(timings.resource_update_interval)

        with run_proxies(server, shard_ids=hosts) as proxies:
            eliminate_sync_gap(*proxies)
            # Wait proxies sync with server to eliminate sync gap
            wait_for(timings.counter_synchronization_interval)

            REQUEST_COUNT = 5
            for n in range(REQUEST_COUNT + 1):
                wait_for(timings.min_sync_gap)
                for proxy in proxies:
                    proxy.sync_counters(n=n)
            wait_for(timings.counter_synchronization_interval)
            # NB: Need extra sync to guarantee that
            # all proxies received final update from all others
            wait_for(timings.counter_synchronization_interval)

            totals = {
                instance.shard_id: instance.current_counters()
                for instance in proxies + [server]
            }
            expected = {
                instance.shard_id: REQUEST_COUNT * len(proxies)
                for instance in proxies + [server]
            }
            assert totals == expected
