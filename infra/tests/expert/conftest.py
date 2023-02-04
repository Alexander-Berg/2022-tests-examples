import pytest

from infra.walle.server.tests.lib.util import get_mock_health_data
from sepelib.core import config
from sepelib.core.constants import HOUR_SECONDS, MINUTE_SECONDS
from walle.expert import screening, juggler, decisionmakers
from walle.expert.types import CheckStatus
from walle.expert.automation_plot import AUTOMATION_PLOT_BASIC_ID, AUTOMATION_PLOT_FULL_FEATURED_ID
from walle.hosts import HostState, HostStatus
from walle.models import monkeypatch_timestamp, timestamp
from infra.walle.server.tests.lib import util

CUSTOM_CHECK_NAME_1 = "custom_check_mock"
CUSTOM_CHECK_NAME_2 = "custom_check_mock_2"


@pytest.yield_fixture(autouse=True)
def tier_2():
    yield from util.tier_2()


@pytest.fixture
def custom_checks():
    custom_checks.CUSTOM_CHECK_NAME_1 = "custom_check_mock"
    custom_checks.CUSTOM_CHECK_NAME_2 = "custom_check_mock_2"
    return custom_checks


@pytest.fixture
def enable_basic_automation_plot(mp, walle_test):
    return mock_automation_plot(AUTOMATION_PLOT_BASIC_ID, mp, walle_test)


@pytest.fixture
def fake_automation_plot(mp, walle_test):
    return mock_automation_plot("automation-plot-mock-id", mp, walle_test)


def mock_automation_plot(automation_plot_id, mp, walle_test):
    if automation_plot_id not in {AUTOMATION_PLOT_BASIC_ID, AUTOMATION_PLOT_FULL_FEATURED_ID}:
        walle_test.automation_plot.mock(
            {
                "id": automation_plot_id,
                "checks": [
                    {
                        "name": check_name,
                        "enabled": True,
                        "reboot": True,
                        "redeploy": True,
                        "start_time": timestamp() - 100,
                    }
                    for check_name in (CUSTOM_CHECK_NAME_1, CUSTOM_CHECK_NAME_2)
                ],
            }
        )

    mocked_decision_maker_cache_key = decisionmakers._DecisionMakerCacheKey(
        automation_plot_id,
        has_infiniband=False,
    )
    mp.method(
        decisionmakers._DecisionMakerCacheKey.from_project,
        return_value=mocked_decision_maker_cache_key,
        obj=decisionmakers._DecisionMakerCacheKey,
    )


@pytest.fixture
def enable_modern_automation_plot(mp, walle_test):
    return mock_automation_plot(AUTOMATION_PLOT_FULL_FEATURED_ID, mp, walle_test)


def _monkeypatch_host_processing(mp, inject=None):
    """Count function calls and pass them through."""
    original_process_func = screening._process_host

    def mock_process(*args, **kwargs):
        if inject is not None:
            inject(*args, **kwargs)
        original_process_func(*args, **kwargs)

    return mp.function(screening._process_host, side_effect=mock_process)


@pytest.fixture
def mock_health_data():
    return get_mock_health_data()


@pytest.fixture
def monkeypatch_screening(mp, walle_test, mock_health_data):
    monkeypatch_timestamp(mp)

    project = walle_test.mock_project({"id": "some-id", "tags": [config.get_value("infiniband.involvement_tag")]})
    host = walle_test.mock_host(
        {
            "inv": 0,
            "state": HostState.ASSIGNED,
            "status": HostStatus.READY,
            "project": project.id,
        }
    )
    got_checks = {}

    def catch_checks(host_updater, host, host_health, decision_maker):
        got_checks.update(host_health.current_reasons)

    _monkeypatch_host_processing(mp, catch_checks)

    mp.function(
        juggler._fetch_health_data,
        return_value=[
            juggler.HostHealthStatus(
                host.name,
                [
                    {
                        "type": check["type"],
                        "metadata": check["metadata"],
                        "status": CheckStatus.FAILED,
                        "status_mtime": timestamp() - HOUR_SECONDS,
                        "timestamp": timestamp() - MINUTE_SECONDS,
                    }
                    for check in mock_health_data
                ],
            )
        ],
    )

    return got_checks
