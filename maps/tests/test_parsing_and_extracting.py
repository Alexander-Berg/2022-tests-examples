import json
import os
import pytest
import datetime as dt

import yatest.common

from yt.wrapper.ypath import ypath_join
from yt.wrapper.client import YtClient
from yql.api.v1.client import YqlClient

from maps.garden.sandbox.yt_stats_collector.lib import yt_log_parser
from maps.garden.sandbox.yt_stats_collector.lib import stats_generator

YT_SERVER = "plato"
LOGS_TABLE = "//home/logs"
PARSED_LOGS_PATH = "//home/result"
NOW = dt.datetime(year=2021, month=8, day=20, hour=19)


def _prepare_logs(yt_client: YtClient):
    data_dir = yatest.common.source_path("maps/garden/sandbox/yt_stats_collector/tests/data")
    yt_client.create("map_node", yt_log_parser._YT_SCHEDULER_LOG_FOLDER, recursive=True, ignore_existing=True)
    with open(os.path.join(data_dir, "yt_scheduler_log.json")) as f:
        yt_client.write_table(ypath_join(yt_log_parser._YT_SCHEDULER_LOG_FOLDER, (NOW-dt.timedelta(days=2)).strftime("%Y-%m-%d")), json.load(f))


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_yt_yql
def test_scheduler_log_parsing(yt_client, environment_settings):
    _prepare_logs(yt_client)
    log_parser = yt_log_parser.YtLogParser(yt_client, PARSED_LOGS_PATH, period_in_days=14)
    parsed_dates = log_parser.parse_yt_scheduler_logs()
    assert parsed_dates

    yql_client = YqlClient(db=YT_SERVER, **environment_settings["yql"])
    stats_generator.generate_stats(yql_client, PARSED_LOGS_PATH)

    all_tables = yt_client.search(root=PARSED_LOGS_PATH, node_type=["table"])
    return {
        table: [row for row in yt_client.read_table(table)]
        for table in all_tables
    }
