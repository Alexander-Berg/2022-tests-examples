import yatest.common
import json

from maps.infra.monitoring.notification_counter.lib import notification_counter

NOTIFICATIONS_PATH = yatest.common.source_path(
    "maps/infra/monitoring/notification_counter/tests/notifications.json")


# Canonical aggregations for "maps_core_driving_router".
canonical_aggregations = dict(
    daily=[2.0, 3.0, 4.0, 4.0, 3.0, 3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 2.0, 3.0, 3.0,
           3.0, 2.0, 2.0, 13.0, 1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 1.0,
           2.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 7.0, 0.0, 0.0, 2.0, 1.0, 0.0, 11.0, 0.0,
           0.0, 0.0, 1.0, 9.0, 14.0, 4.0, 0.0, 0.0, 0.0, 4.0, 1.0, 0.0, 0.0, 0.0, 0.0],
    weekly=[19.0, 17.0, 15.0, 11.0, 7.0, 4.0, 3.0, 6.0, 9.0, 11.0, 13.0, 15.0, 28.0,
            27.0, 24.0, 21.0, 20.0, 18.0, 16.0, 3.0, 2.0, 5.0, 5.0, 4.0, 6.0, 6.0,
            7.0, 7.0, 4.0, 5.0, 4.0, 9.0, 9.0, 8.0, 10.0, 11.0, 10.0, 21.0, 14.0,
            14.0, 14.0, 13.0, 21.0, 35.0, 28.0, 28.0, 28.0, 28.0, 31.0, 23.0, 9.0,
            5.0, 5.0, 5.0],
    monthly=[55.0, 55.0, 52.0, 49.0, 45.0, 42.0, 40.0, 40.0, 47.0, 46.0, 46.0, 48.0,
             49.0, 47.0, 55.0, 52.0, 49.0, 47.0, 46.0, 42.0, 55.0, 59.0, 59.0, 57.0,
             57.0, 61.0, 62.0, 62.0, 59.0, 59.0, 58.0]
)


def test_aggregate():
    with open(NOTIFICATIONS_PATH) as f:
        notifications = json.load(f)
    df = notification_counter.aggregate(notifications)

    driving_router = df[df.sla_service == "maps_core_driving_router"]
    aggregate_for = lambda period: driving_router.notifications.rolling(period).sum().dropna().tolist()

    assert canonical_aggregations["daily"] == aggregate_for(period=1)
    assert canonical_aggregations["weekly"] == aggregate_for(period=7)
    assert canonical_aggregations["monthly"] == aggregate_for(period=30)


def test_apply_mapping():
    with open(NOTIFICATIONS_PATH) as f:
        notifications = json.load(f)

    sla_service_mapping = {
        # (Nanny prefix, alert name) -> list of sla services.
        ("maps_core_driving_router", "router_stopwatch_jams_age"): ["maps_core_jams", "maps_core_driving_router"]
    }

    df = notification_counter.aggregate(notifications, sla_service_mapping=sla_service_mapping)

    # We expect only 15 notifications of 'router_stopwatch_jams_age' alert.
    assert df[df.sla_service == "maps_core_jams"].notifications.sum() == 15
    # And total number of 'maps_core_driving_router' notifications shouldn't change.
    assert df[df.sla_service == "maps_core_driving_router"].notifications.sum() == 113
    assert df[df.sla_service == "maps_core_mobile_proxy"].notifications.sum() == 2
