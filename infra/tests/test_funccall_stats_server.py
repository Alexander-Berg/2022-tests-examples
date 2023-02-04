import json

from infra.rtc_sla_tentacles.backend.lib.funccall_stats_server import server as stat_server


def test_funccall_stats_server():
    server = stat_server.FunccallStatsServer("test", port=80, thread_count=1)
    stat_server.init_harvester_unistat(["juggler"])
    # noinspection PyProtectedMember
    app = server._get_flask_app()
    client = app.test_client()

    stat_server.add_ok("juggler", 10)
    stat_server.add_ok("juggler", 10)
    stat_server.add_error("juggler", 1)

    response = client.get("/unistat")
    stats_actual = {k: v for [k, v] in json.loads(response.data)}

    assert sum(b[1] for b in stats_actual["task_juggler_time_dhhh"]) == 3  # NOTE(rocco66): buckets count
    assert stats_actual["task_juggler_count_dmmm"] == 3.0
    assert stats_actual["task_juggler_errors_dmmm"] == 1.0
