import pytest

from infra.walle.server.tests.lib.util import monkeypatch_automation_plot_id
from sepelib.core.constants import HOUR_SECONDS
from walle.expert.automation_plot import AUTOMATION_PLOT_BASIC_ID, AUTOMATION_PLOT_FULL_FEATURED_ID
from walle.expert.types import CheckType, CheckStatus
from walle.models import timestamp


@pytest.fixture()
def enable_hw_checks(mp):
    monkeypatch_automation_plot_id(mp, AUTOMATION_PLOT_FULL_FEATURED_ID)


@pytest.fixture()
def disable_hw_checks(mp):
    monkeypatch_automation_plot_id(mp, AUTOMATION_PLOT_BASIC_ID)


@pytest.fixture
def reasons():
    return {
        check_type: {
            "status": CheckStatus.PASSED,
            "effective_timestamp": timestamp() - HOUR_SECONDS,
            "status_mtime": timestamp() - HOUR_SECONDS,
            "timestamp": timestamp(),
        }
        for check_type in CheckType.ALL
    }


@pytest.fixture(params=(False, True))
def fast(request):
    return request.param


@pytest.fixture(params=[True, False])
def hw_checks_enabled(mp, request):
    if request.param:
        monkeypatch_automation_plot_id(mp, AUTOMATION_PLOT_FULL_FEATURED_ID)
    else:
        monkeypatch_automation_plot_id(mp, AUTOMATION_PLOT_BASIC_ID)
    return request.param
