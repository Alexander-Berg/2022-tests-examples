import pytest
import threading
import time

from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import DummyConfig
from balancer.test.util.process import BalancerStartError

from configs import UnistatSimpleConfig, UnistatConfigWithConflictingAddresses


def test_connection_count_to_unistat(ctx):
    ctx.start_backend(DummyConfig())
    ctx.start_balancer(UnistatSimpleConfig(workers=4))

    prev = None
    for _ in xrange(100):
        stats = ctx.get_unistat()

        if prev is None:
            prev = stats["worker-connections_count_summ"]
            continue

        assert stats["worker-connections_count_summ"] - prev == 1
        prev = stats["worker-connections_count_summ"]


def test_invalid_unistat_request(ctx):
    ctx.start_backend(DummyConfig())
    ctx.start_balancer(UnistatSimpleConfig(workers=4))

    req = ctx.perform_request(http.request.get("/definitely-not-a-unistat"), ctx.balancer.config.stats_port)
    assert req.status == 400

    req = ctx.perform_request(http.request.post("/unistat"), ctx.balancer.config.stats_port)
    assert req.status == 400


def test_reload_config(ctx):
    ctx.start_backend(DummyConfig())
    ctx.start_balancer(UnistatSimpleConfig())
    ctx.balancer.reload_config(UnistatSimpleConfig(), keep_ports=True)


def test_conflicting_addresses(ctx):
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(UnistatConfigWithConflictingAddresses(conflicting_with_admin=False))


def test_conflicting_addresses_with_admin(ctx):
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(UnistatConfigWithConflictingAddresses(conflicting_with_admin=True))


def test_unistat_reload_race_condition(ctx):
    def ask_unistat(test_failed, do_exit):
        last_value = None
        while not do_exit.is_set():
            try:
                stats = ctx.get_unistat()
                count = stats['worker-connections_count_summ']
            except:
                count = None
            if last_value is not None and count is not None:
                if count != 1 and count < last_value:
                    test_failed.set()
                    do_exit.set()
            last_value = count
            time.sleep(0.02)
    ctx.start_backend(DummyConfig())
    ctx.start_balancer(UnistatSimpleConfig(workers=2))
    test_failed = threading.Event()
    do_exit = threading.Event()
    unistat_thread = threading.Thread(target=ask_unistat, args=(test_failed, do_exit))
    unistat_thread.start()
    for _ in range(10):
        ctx.balancer.reload_config(UnistatSimpleConfig(workers=2), keep_ports=True)
        time.sleep(1)
        if test_failed.is_set():
            break
    do_exit.set()
    assert not test_failed.is_set(), 'detected race condition in unistat while reloading config'
