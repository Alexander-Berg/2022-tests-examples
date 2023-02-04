import threading

from infra.rtc_sla_tentacles.backend.lib.harvesters import ticker
from infra.rtc_sla_tentacles.backend.lib.funccall_stats_server import server as stat_server


class FakeTicker(ticker.Ticker):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.calls_counter = 0
        self.locks_counter = 0
        self.end = threading.Event()

    def _init_locks(self):
        pass

    def _try_to_lock(self, harvester, iteration_ts) -> bool:
        self.locks_counter += 1
        return True

    def _get_actual_harvesters(self):
        return list(self.harvesters_manager.get_harvesters())

    def _run_with_stats(self, harvester, started_time):
        self.calls_counter += 1
        self.end.wait(5000)


def test_ticker_job_limit(harvesters_manager, config_interface, mongomock_client):
    thread_count = 5
    stats_server = stat_server.FunccallStatsServer("name", 8080, thread_count)
    stat_server.init_harvester_unistat(g.harvester_type for g in harvesters_manager.get_harvester_groups())

    ticker_instance = FakeTicker(
        harvesters_manager,
        config_interface,
        mongo_client=mongomock_client,
        stats_server=stats_server,
        worker_count=thread_count,
    )
    assert len(ticker_instance._get_actual_harvesters()) > thread_count
    try:
        ticker_instance._tick()
        assert ticker_instance.calls_counter == thread_count
        assert ticker_instance.locks_counter == thread_count
    finally:
        ticker_instance.end.set()
