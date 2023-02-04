from datetime import datetime, timezone
from maps.b2bgeo.ya_courier.analytics_backend.lib.plan_fact.analytics_storage import Version


def test_when_version_is_used_as_prev_you_filter_out_initial_version():
    initial_dt = datetime(2021, 11, 18, 19, 36, 7, 167455, tzinfo=timezone.utc)
    str_version = str(Version.from_dt(initial_dt))
    assert initial_dt <= Version.from_str(str_version).value

    next_version = datetime(2021, 11, 18, 19, 36, 7, 168455, tzinfo=timezone.utc)
    assert next_version > Version.from_str(str_version).value
