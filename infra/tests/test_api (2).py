import collections
import datetime
import time

from infra.rtc_sla_tentacles.backend.lib.api.api_class import DeployEngine
from infra.rtc_sla_tentacles.backend.lib.api.handlers import pipeline, features
from infra.rtc_sla_tentacles.backend.lib.clickhouse import database, client as clickhouse_client
from infra.rtc_sla_tentacles.backend.lib.metrics import metrics_provider


def _prepare_slo_calculation(monkeypatch, api, target_allocation_zone_id):
    zone_metrics = metrics_provider.AllZoneMetrics(
        metrics_provider.MetricsTentaclesAvailability(10, 0, 0, 10),
        metrics_provider.MetricsTentaclesTimestampResourceFreshness(10, 0, 0, 90),
        metrics_provider.MetricsTentaclesReallocation(),
        metrics_provider.NonSloMetrics()
    )
    allocation_zones_metrics = features.AllocationZonesMetrics(
        metrics={target_allocation_zone_id: zone_metrics},
        ts=int(time.time()),
    )
    monkeypatch.setattr(api.metric_storage, "fetch_all_zones_metrics", lambda *args, **kwargs: allocation_zones_metrics)


def test_overview(client, monkeypatch, api):
    target_allocation_zone_id = "rtc_sla_tentacles_testing_gencfg"
    _prepare_slo_calculation(monkeypatch, api, target_allocation_zone_id)
    response = client.get("/api/data/overview/")
    assert response.status_code == 200, response.data
    zones = response.get_json()["result"]["allocation_zones"]

    # NOTE(rocco66): see config_file_example1 for details
    assert len(zones) == 3

    assert zones[0]["allocation_zone"] == "rtc_sla_tentacles_testing_gencfg"
    assert zones[0]["tags"]["deploy_engine"] == DeployEngine.gencfg
    assert zones[0]["tags"]["location"] == "sas"

    assert zones[1]["allocation_zone"] == "rtc_sla_tentacles_testing_yp_lite"
    assert zones[1]["tags"]["deploy_engine"] == DeployEngine.yp_lite
    assert zones[1]["tags"]["location"] == "man"

    assert zones[2]["allocation_zone"] == "rtc_sla_tentacles_testing_yp_lite_daemonset"
    assert zones[2]["tags"]["deploy_engine"] == DeployEngine.yp_lite
    assert zones[2]["tags"]["location"] == "vla"


def _prepare_clickhouse_pipeline_data(monkeypatch):
    Pod = collections.namedtuple("Pod", pipeline.POD_COLUMNS + ["count"])
    Iteration = collections.namedtuple("Iteration", pipeline.ITERATION_COLUMNS)
    operation = database.Tentacle.walle_operation_state.inner_field.enum_cls.operation
    mocked_pods_data = [
        Pod("probation", operation, False, False, "", "", True, True, True, True, True, True, True, True, True, 1000)
    ]
    now = datetime.datetime.now()
    mocked_iteration = Iteration(now, now, now)
    monkeypatch.setattr(pipeline, "_get_pods_data", lambda *args, **kwargs: mocked_pods_data)
    monkeypatch.setattr(pipeline, "_get_iteration", lambda *args, **kwargs: mocked_iteration)


def test_allocation_zone_details(client, api, monkeypatch):
    target_allocation_zone_id = "rtc_sla_tentacles_testing_yp_lite"

    _prepare_clickhouse_pipeline_data(monkeypatch)
    _prepare_slo_calculation(monkeypatch, api, target_allocation_zone_id)

    response = client.post(
        "/api/data/allocation_zone_details/",
        json={"allocation_zone_id": target_allocation_zone_id},
    )
    assert response.status_code == 200
    result = response.get_json()["result"]
    assert result["features"]["data_freshness"]["status"] == "OK"
    assert result["features"]["availability"]
    assert result["features"]["redeploy"]


def test_pods_list(client, monkeypatch):
    target_allocation_zone_id = "rtc_sla_tentacles_testing_yp_lite"
    requested_columns = ["fqdn", "walle_operation_state", "excluded_from_slo_by_walle"]

    RequestedTentacle = collections.namedtuple("RequestedTentacle", requested_columns)
    operation = database.Tentacle.walle_operation_state.inner_field.enum_cls.operation
    mocked_tentacles_data = [
        RequestedTentacle("foo.yandex-team.ru", operation, 1),
        RequestedTentacle("bar.yandex-team.ru", operation, 1),
    ]
    monkeypatch.setattr(clickhouse_client.ClickhouseClient, "select", lambda *args, **kwargs: mocked_tentacles_data)
    monkeypatch.setattr(clickhouse_client.ClickhouseClient, "count", lambda *args, **kwargs: 2)

    response = client.post(
        "/api/data/pods_list/",
        json={
            "allocation_zone_id": target_allocation_zone_id,
            "columns": requested_columns,
        },
    )
    assert response.status_code == 200
    tentacles = response.get_json()["result"]["tentacles"]
    assert {t["fqdn"] for t in tentacles} == {"foo.yandex-team.ru", "bar.yandex-team.ru"}
    assert all(t["excluded_from_slo_by_walle"] for t in tentacles)
