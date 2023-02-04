from maps.infra.ratelimiter2.tools.pyhelpers.config import timings, wait_for


REQUESTS_SERVED = 1


def test_proxies_sync_through_server(run_server, run_proxies, eliminate_sync_gap, hosts) -> None:
    """
        Sends increments to each proxy
        Expect all of them synchronized through server
    """
    with run_server() as server:
        server.set_limits(rps=0, burst=0)
        wait_for(timings.resource_update_interval)

        with run_proxies(server, shard_ids=hosts) as proxies:
            for i, proxy in enumerate(proxies):
                eliminate_sync_gap(proxy)
                proxy.sync_counters(n=REQUESTS_SERVED)

            wait_for(timings.counter_synchronization_interval)

            # Check total
            total_counters = server.current_counters()
            assert total_counters == REQUESTS_SERVED * len(proxies)

            # Check each proxy
            for i, proxy in enumerate(proxies):
                other_total = server.sync_counters(
                    n=REQUESTS_SERVED,
                    shard=proxy.shard_id
                )
                assert other_total == REQUESTS_SERVED * (len(proxies) - 1)


def test_outdated_proxy(run_server, eliminate_sync_gap) -> None:
    """
        Server should drop delta from proxy that hasn't been synced for
        more than max_sync_gap interval.
    """
    with run_server() as server:
        server.set_limits(rps=0, burst=0)
        wait_for(timings.resource_update_interval)

        eliminate_sync_gap(server)
        server.sync_counters(n=REQUESTS_SERVED)
        wait_for(timings.max_sync_gap)

        server.sync_counters(n=REQUESTS_SERVED * 2)
        assert server.current_counters() == REQUESTS_SERVED
