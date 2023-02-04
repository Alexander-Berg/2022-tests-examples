import pytest

import ads.emily.storage.libs.monitor.alerts.solo.registry.v3.alert as registry_v3


@pytest.mark.parametrize("cls,load_dict", [
    (registry_v3.Alert, dict(
        id="id",
        project_id="project_id",
        name="name",
        expression={"program": "1"},
        group_by_labels=[],
        description="desc",
        annotations={"key": "value"},
        window="10s",
        delay="1s",
        channels=[],
    )),
    (registry_v3.NotificationConfig, dict(channel_id="id", notify_about_statuses=[1, 2, 3, 4, 5], repeat_period="10s")),
])
def test_proto_consistency(cls, load_dict):
    loaded = cls.load(load_dict)
    loaded_proto = loaded.to_proto()
    dumped_dict = cls.from_proto(loaded_proto).to_dict()
    assert load_dict == dumped_dict, f"{cls} is not equal to proto dump"
