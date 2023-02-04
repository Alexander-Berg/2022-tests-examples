import http.client as httplib

import pytest

from infra.walle.server.tests.lib.util import TestCase, monkeypatch_function
from tests.api.maintenance_plot_api.mocks import MOCK_MAINTENANCE_PLOT_OBJ, COMMON_SETTINGS
from walle.clients import abc, staff


@pytest.fixture
def test(request):
    return TestCase.create(request)


@pytest.mark.usefixtures("unauthenticated")
@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_create_maintenance_plot_unauthenticated(test):
    data = MOCK_MAINTENANCE_PLOT_OBJ.to_dict()
    result = test.api_client.post("/v1/maintenance-plots", data=data)

    assert result.status_code == httplib.UNAUTHORIZED
    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("monkeypatches_for_noop_scenario")
def test_create_maintenance_plot_not_authorized(test, mp):
    monkeypatch_function(
        mp,
        func=abc.get_service_members,
        module=abc,
        return_value=[
            {
                "person": {"login": "some-dummy"},
            }
        ],
    )
    data = MOCK_MAINTENANCE_PLOT_OBJ.to_dict()
    result = test.api_client.post("/v1/maintenance-plots", data=data)

    assert result.status_code == httplib.FORBIDDEN
    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.usefixtures("monkeypatch_locks")
def test_create_maintenance_plot_successfully(test, mp):
    monkeypatch_function(
        mp,
        func=abc.get_service_members,
        module=abc,
        return_value=[
            {
                "person": {"login": "some-dummy"},
            }
        ],
    )
    monkeypatch_function(
        mp, func=staff.check_logins, module=staff, return_value=COMMON_SETTINGS.maintenance_approvers.logins
    )

    data = MOCK_MAINTENANCE_PLOT_OBJ.to_dict()

    result = test.api_client.post("/v1/maintenance-plots", data=data)

    assert result.status_code == httplib.CREATED

    test.maintenance_plots.mock(data, save=False)
    test.maintenance_plots.assert_equal()


@pytest.mark.usefixtures("authorized_admin")
@pytest.mark.usefixtures("monkeypatch_locks")
def test_vaildation_error_from_outer_system(test, mp):
    monkeypatch_function(
        mp,
        func=abc.get_service_members,
        module=abc,
        return_value=[
            {
                "person": {"login": "some-dummy"},
            }
        ],
    )
    monkeypatch_function(mp, func=staff.check_logins, module=staff, return_value=[])

    data = MOCK_MAINTENANCE_PLOT_OBJ.to_dict()

    result = test.api_client.post("/v1/maintenance-plots", data=data)

    assert result.status_code == httplib.BAD_REQUEST

    test.maintenance_plots.assert_equal()
