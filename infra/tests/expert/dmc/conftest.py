import pytest

from infra.walle.server.tests.lib.util import TestCase
from walle.expert import decisionmakers
from walle.expert.automation_plot import AUTOMATION_PLOT_BASIC_ID, AUTOMATION_PLOT_FULL_FEATURED_ID
from walle.models import timestamp


@pytest.fixture
def test(request, monkeypatch_timestamp, mp, monkeypatch_locks, mp_juggler_source):
    return TestCase.create(request)


@pytest.fixture(params=[AUTOMATION_PLOT_BASIC_ID, AUTOMATION_PLOT_FULL_FEATURED_ID, "automation-plot-mock-id"])
def mock_automation_plot(request, mp, test):
    pytest.CUSTOM_CHECK_NAME = "custom-check-mock"
    if request.param not in {AUTOMATION_PLOT_BASIC_ID, AUTOMATION_PLOT_FULL_FEATURED_ID}:
        test.automation_plot.mock(
            {
                "id": request.param,
                "checks": [
                    {
                        "name": pytest.CUSTOM_CHECK_NAME,
                        "enabled": True,
                        "reboot": True,
                        "redeploy": True,
                        "start_time": timestamp() - 100,
                    }
                ],
            }
        )

    mocked_decision_maker_cache_key = decisionmakers._DecisionMakerCacheKey(
        request.param,
        has_infiniband=False,
    )
    mp.method(
        decisionmakers._DecisionMakerCacheKey.from_project,
        return_value=mocked_decision_maker_cache_key,
        obj=decisionmakers._DecisionMakerCacheKey,
    )
