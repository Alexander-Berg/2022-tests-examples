import pytest
from smb.common.sensors import MetricGroup, RateBuilder

from maps_adv.warden.client.lib import PeriodicalTask
from maps_adv.warden.client.lib.client import ClientFactory


@pytest.fixture
def metric_group():
    return MetricGroup(RateBuilder())


@pytest.fixture
def periodical_task(mocker, metric_group):
    mocker.patch.object(PeriodicalTask, "_relaunch_interval_after_exception", new=0)

    def make_periodical_task(func, name="task_1", **kwargs):
        return PeriodicalTask(name, func, sensors=metric_group, **kwargs)

    return make_periodical_task


@pytest.fixture
def client_factory(mock_client):
    mock_client.create_task.coro.return_value = {
        "task_id": 1,
        "status": "accepted",
        "time_limit": 0.2,
    }

    class MockClientFactory(ClientFactory):
        _client_class = lambda *args, **kwargs: mock_client  # noqa: E731

    return MockClientFactory("")
