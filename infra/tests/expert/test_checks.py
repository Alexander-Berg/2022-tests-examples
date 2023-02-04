"""Test automation plot and checks"""

import pytest

from infra.walle.server.tests.lib.util import TestCase
from walle.expert import automation_plot
from walle.models import timestamp


@pytest.fixture
def test(request):
    return TestCase.create(request)


def mock_automation_plot(test, check_names=None, enabled=True, start_time=None):
    if check_names is None:
        check_names = ["walle_certificate"]

    if start_time is None:
        start_time = timestamp()

    return test.automation_plot.mock(
        {
            "checks": [
                {"name": check, "enabled": enabled, "reboot": True, "redeploy": True, "start_time": start_time}
                for check in check_names
            ],
        }
    )


@pytest.mark.usefixtures("test")
def test_save_plot():
    plot = automation_plot.AutomationPlot(
        id="plot-mock",
        name="Plot mock",
        owners=["wall-e", "@ya_group"],
        checks=[
            automation_plot.Check(
                name="walle_certificate", enabled=True, reboot=True, redeploy=True, start_time=timestamp()
            )
        ],
    )

    plot.save()


def test_disable_check(test):
    check_names = ["walle_certificate", "UNREACHABLE", "ssh"]
    plot = mock_automation_plot(test, check_names=check_names, enabled=True, start_time=timestamp() - 10)
    plot.save()

    for name in check_names:
        plot.copy().disable_check(name)
        _find_check(name, plot.checks).enabled = False

        # assert only one check was updated
        test.automation_plot.assert_equal()


@pytest.mark.usefixtures("monkeypatch_timestamp")
def test_enable_check(test):
    check_names = ["walle_certificate", "UNREACHABLE", "ssh"]
    plot = mock_automation_plot(test, check_names=check_names, enabled=False, start_time=timestamp() - 10)
    plot.save()

    for name in check_names:
        plot.copy().enable_check(name)

        check = _find_check(name, plot.checks)
        check.enabled = True
        check.start_time = timestamp()

        # assert only one check was updated
        test.automation_plot.assert_equal()


def _find_check(name, check_list):
    return next(iter(check for check in check_list if check.name == name), None)
